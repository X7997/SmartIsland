/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 */

package com.agupta07505.smartisland.ui.nutrition

import android.graphics.Bitmap
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import java.io.File
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddPhotoAlternate
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.LocalDining
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.agupta07505.smartisland.data.nutrition.NutritionRepository
import com.agupta07505.smartisland.data.nutrition.ScannedProductInfo
import com.agupta07505.smartisland.data.nutrition.WorkoutArchiveRecord
import com.agupta07505.smartisland.ui.components.GymMuscleAnatomyDualView
import com.agupta07505.smartisland.util.AiNutritionScanner
import kotlinx.coroutines.launch
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun NutritionScreen(
    nutritionRepository: NutritionRepository,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val weeklyTrainedCategories by nutritionRepository.weeklyTrainedCategories.collectAsState()
    val todayCalories by nutritionRepository.todayCalories.collectAsState()
    val todaySugar by nutritionRepository.todaySugar.collectAsState()
    val scannedProducts by nutritionRepository.scannedProducts.collectAsState()
    val workoutArchives by nutritionRepository.workoutArchives.collectAsState()

    // 连接 Repository 的后台保活扫描状态（切前后台绝不断流）
    val isAnalyzing by nutritionRepository.isAnalyzing.collectAsState()
    val scanProgress by nutritionRepository.scanProgress.collectAsState()
    val scanStageText by nutritionRepository.scanStageText.collectAsState()
    val pendingProductResult by nutritionRepository.pendingProductResult.collectAsState()
    val fallbackReason by nutritionRepository.fallbackReason.collectAsState()

    var selectedCapacityMl by remember { mutableIntStateOf(500) }
    var showResetWeekDialog by remember { mutableStateOf(false) }

    var tempCameraImageUri by remember { mutableStateOf<Uri?>(null) }

    fun prepareCameraImageUri(): Uri {
        val photoFile = File(context.cacheDir, "camera_nutrition_capture.jpg")
        if (photoFile.exists()) photoFile.delete()
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            photoFile
        )
        tempCameraImageUri = uri
        return uri
    }

    // 相册选图回调（调用 Repository 后台常驻异步任务，切后台永不断流）
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            nutritionRepository.startScan(context, uri, selectedCapacityMl)
        }
    }

    // 全像素高清拍照回调（调用 Repository 后台常驻异步任务，切后台永不断流）
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success: Boolean ->
        val uri = tempCameraImageUri
        if (success && uri != null) {
            nutritionRepository.startScan(context, uri, selectedCapacityMl)
        }
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // ==========================================
        // 模块 1：【每日训练】点亮（上面是每日训练的点亮，去除冗杂文字，蓝色高亮）
        // ==========================================
        DailyTrainingHighlightCard(
            weeklyTrainedCategories = weeklyTrainedCategories,
            onResetClick = { showResetWeekDialog = true }
        )

        // ==========================================
        // 模块 2：【拍照识别我的饮料】（下面是拍照识别我的饮料，简洁直观，带真实进度条）
        // ==========================================
        DrinkNutritionScanCard(
            todayCalories = todayCalories,
            todaySugar = todaySugar,
            selectedCapacityMl = selectedCapacityMl,
            onCapacityChanged = { selectedCapacityMl = it },
            isAnalyzing = isAnalyzing,
            scanProgress = scanProgress,
            scanStageText = scanStageText,
            onCameraClick = {
                val uri = prepareCameraImageUri()
                cameraLauncher.launch(uri)
            },
            onGalleryClick = { photoPickerLauncher.launch("image/*") }
        )

        // 今日已记录饮品流
        if (scannedProducts.isNotEmpty()) {
            Text(
                text = "今日已记录饮品 (${scannedProducts.size})",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            scannedProducts.forEach { product ->
                ScannedProductItemCard(
                    product = product,
                    onDelete = { nutritionRepository.removeProductRecord(product.id) }
                )
            }
        }

        // ==========================================
        // 模块 3：【推荐】（再下面是推荐：结合训练部位与微量元素精准食补）
        // ==========================================
        MicronutrientRecommendationCard(
            weeklyTrainedCategories = weeklyTrainedCategories,
            todaySugar = todaySugar
        )

        // 训练留档历史档案
        if (workoutArchives.isNotEmpty()) {
            Text(
                text = "训练历史留档 (${workoutArchives.size})",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            workoutArchives.take(8).forEach { archive ->
                WorkoutArchiveItemCard(archive = archive)
            }
        }
    }

    // AI 正常识别成功的详情弹窗
    if (pendingProductResult != null) {
        ScanResultDialog(
            product = pendingProductResult!!,
            onConfirm = {
                nutritionRepository.addProductRecord(pendingProductResult!!)
                nutritionRepository.clearPendingResult()
                Toast.makeText(context, "已成功存入今日饮食记录！", Toast.LENGTH_SHORT).show()
            },
            onDismiss = { nutritionRepository.clearPendingResult() }
        )
    }

    // 智能兜底弹窗（解决“识别失败直接退出什么都没有”的问题）
    if (fallbackReason != null) {
        IntelligentFallbackDialog(
            reason = fallbackReason!!,
            selectedCapacityMl = selectedCapacityMl,
            onSelectPreset = { presetInfo ->
                nutritionRepository.addProductRecord(presetInfo)
                nutritionRepository.clearFallbackReason()
                Toast.makeText(context, "已成功快速记录：${presetInfo.productName}", Toast.LENGTH_SHORT).show()
            },
            onRetryCamera = {
                nutritionRepository.clearFallbackReason()
                val uri = prepareCameraImageUri()
                cameraLauncher.launch(uri)
            },
            onDismiss = { nutritionRepository.clearFallbackReason() }
        )
    }

    // 重置本周点亮确认弹窗
    if (showResetWeekDialog) {
        AlertDialog(
            onDismissRequest = { showResetWeekDialog = false },
            title = { Text("重置训练点亮图？", fontWeight = FontWeight.Bold) },
            text = { Text("确定要重置已点亮的训练肌群并重新开始吗？") },
            confirmButton = {
                Button(
                    onClick = {
                        nutritionRepository.resetWeeklyTrainedParts()
                        showResetWeekDialog = false
                        Toast.makeText(context, "训练图已重置", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("重置")
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetWeekDialog = false }) {
                    Text("取消")
                }
            }
        )
    }
}

/**
 * 模块 1：每日训练点亮卡片（极简无废话，科技亮蓝高能点亮）
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DailyTrainingHighlightCard(
    weeklyTrainedCategories: Set<String>,
    onResetClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // 顶栏：标题与点亮标签
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(26.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF2563EB).copy(alpha = 0.18f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.FitnessCenter,
                            contentDescription = null,
                            tint = Color(0xFF2563EB),
                            modifier = Modifier.size(15.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "每日训练点亮图",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                IconButton(
                    onClick = onResetClick,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Refresh,
                        contentDescription = "重置周期",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            // 本周点亮部位标签栏（蓝色高亮徽标）
            if (weeklyTrainedCategories.isNotEmpty()) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    weeklyTrainedCategories.forEach { category ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF2563EB).copy(alpha = 0.18f))
                                .border(0.8.dp, Color(0xFF2563EB).copy(alpha = 0.65f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 9.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = "✨ 已练 $category",
                                color = Color(0xFF3B82F6),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            } else {
                Text(
                    text = "完成训练后，对应肌群将自动在此以科技亮蓝色点亮。",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp
                )
            }

            // 双面人体解剖矢量图（采用纯正科技亮蓝着色，高对比度极清晰）
            GymMuscleAnatomyDualView(
                activeMuscle = null,
                activeCategory = "",
                highlightedCategories = weeklyTrainedCategories,
                showLabels = true,
                showStatusBadge = false,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

/**
 * 模块 2：拍照识别我的饮料卡片（紧凑仪表盘 + 拍照大入口）
 */
@Composable
private fun DrinkNutritionScanCard(
    todayCalories: Double,
    todaySugar: Double,
    selectedCapacityMl: Int,
    onCapacityChanged: (Int) -> Unit,
    isAnalyzing: Boolean,
    scanProgress: Float,
    scanStageText: String,
    onCameraClick: () -> Unit,
    onGalleryClick: () -> Unit
) {
    val sugarLimit = 25.0
    val isSugarExceeded = todaySugar > sugarLimit
    val sugarProgress = (todaySugar / sugarLimit).coerceIn(0.0, 1.0).toFloat()

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(
            1.dp,
            if (isSugarExceeded) Color(0xFFEF4444).copy(alpha = 0.45f)
            else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 顶栏：标题与日期
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(26.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF10B981).copy(alpha = 0.18f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.LocalDining,
                            contentDescription = null,
                            tint = Color(0xFF10B981),
                            modifier = Modifier.size(15.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "拍照识别我的饮料",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Text(
                    text = SimpleDateFormat("M月d日", Locale.CHINA).format(Date()),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // 能量与糖分双指标
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // 热量
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFF59E0B).copy(alpha = 0.12f))
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column {
                            Text("今日热量", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                text = "${String.format(Locale.getDefault(), "%.1f", todayCalories)} kcal",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFD97706)
                            )
                        }
                    }
                }

                // 糖分
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (isSugarExceeded) Color(0xFFEF4444).copy(alpha = 0.14f)
                            else Color(0xFF10B981).copy(alpha = 0.12f)
                        )
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column {
                            Text("添加糖", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                text = "${String.format(Locale.getDefault(), "%.1f", todaySugar)} / 25g",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSugarExceeded) Color(0xFFDC2626) else Color(0xFF059669)
                            )
                        }
                    }
                }
            }

            // 糖分进度条
            LinearProgressIndicator(
                progress = { sugarProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = if (isSugarExceeded) Color(0xFFEF4444) else Color(0xFF10B981),
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            // 容量选择
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("净含量:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                listOf(330, 500, 1000).forEach { ml ->
                    val isSelected = selectedCapacityMl == ml
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            )
                            .clickable { onCapacityChanged(ml) }
                            .padding(horizontal = 9.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "${ml}mL",
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // 识别状态 / 按钮区
            if (isAnalyzing) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f))
                        .border(1.dp, Color(0xFF0284C7).copy(alpha = 0.35f), RoundedCornerShape(14.dp))
                        .padding(14.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = Color(0xFF0284C7)
                                )
                                Text(
                                    text = scanStageText.ifBlank { "正在全力解析成分表..." },
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Text(
                                text = "${(scanProgress * 100).toInt()}%",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF0284C7)
                            )
                        }

                        LinearProgressIndicator(
                            progress = { scanProgress.coerceIn(0.1f, 1f) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = Color(0xFF0284C7),
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )

                        Text(
                            text = "⚡ 后台常驻保障：切回桌面或切换应用不会中断识别",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = onCameraClick,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.PhotoCamera,
                            contentDescription = null,
                            modifier = Modifier.size(17.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("拍照识别", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }

                    OutlinedButton(
                        onClick = onGalleryClick,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.AddPhotoAlternate,
                            contentDescription = null,
                            modifier = Modifier.size(17.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("相册选图", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }
        }
    }
}

/**
 * 模块 3：微量元素精准食补推荐卡片（紧凑实用）
 */
@Composable
private fun MicronutrientRecommendationCard(
    weeklyTrainedCategories: Set<String>,
    todaySugar: Double
) {
    val hasLegOrGlute = weeklyTrainedCategories.any { it.contains("腿") || it.contains("臀") || it.contains("腹") }
    val hasUpperBody = weeklyTrainedCategories.any { it.contains("胸") || it.contains("背") || it.contains("肩") || it.contains("臂") }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF06B6D4).copy(alpha = 0.18f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Restaurant,
                        contentDescription = null,
                        tint = Color(0xFF06B6D4),
                        modifier = Modifier.size(15.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "结合训练与饮食 · 推荐补充",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            // 1. 臀腿/核心出汗与电解质
            if (hasLegOrGlute || weeklyTrainedCategories.isEmpty()) {
                CompactNutrientItem(
                    tag = "排汗与肌肉防抽搐",
                    nutrients = "钾 (K) · 镁 (Mg) · 钠 (Na)",
                    sources = "香蕉、天然椰子水、菠菜、原味坚果、淡盐水"
                )
            }

            // 2. 上肢肌力与肌蛋白超量恢复
            if (hasUpperBody || weeklyTrainedCategories.isEmpty()) {
                CompactNutrientItem(
                    tag = "肌蛋白合成与修复",
                    nutrients = "锌 (Zn) · 维生素 B族 (B6/B12)",
                    sources = "瘦牛肉、鸡蛋、三文鱼、燕麦、西兰花"
                )
            }

            // 3. 控糖代谢
            if (todaySugar > 25.0) {
                CompactNutrientItem(
                    tag = "糖原代谢加速辅酶",
                    nutrients = "维生素 B1 · 铬 (Chromium)",
                    sources = "全麦面包、糙米、绿豆汤、黑芝麻"
                )
            }
        }
    }
}

@Composable
private fun CompactNutrientItem(
    tag: String,
    nutrients: String,
    sources: String
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
            .padding(10.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "• $tag: ",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = nutrients,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Text(
                text = "🥗 食补优选: $sources",
                fontSize = 11.sp,
                color = Color(0xFF059669),
                fontWeight = FontWeight.Medium
            )
        }
    }
}

/**
 * 今日已记录饮品卡片
 */
@Composable
private fun ScannedProductItemCard(
    product: ScannedProductInfo,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (product.brand.isNotBlank()) "[${product.brand}] ${product.productName}" else product.productName,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${product.bottleCapacityMl}mL · ${product.totalEnergyKcal} kcal · 含糖 ${product.totalSugarG}g",
                    fontSize = 11.sp,
                    color = if (product.isSugarExceeded) Color(0xFFDC2626) else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            IconButton(onClick = onDelete, modifier = Modifier.size(24.dp)) {
                Icon(
                    imageVector = Icons.Rounded.Delete,
                    contentDescription = "删除",
                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.65f),
                    modifier = Modifier.size(17.dp)
                )
            }
        }
    }
}

/**
 * 训练留档单项卡片
 */
@Composable
private fun WorkoutArchiveItemCard(
    archive: WorkoutArchiveRecord
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF10B981).copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.CheckCircle,
                        contentDescription = null,
                        tint = Color(0xFF10B981),
                        modifier = Modifier.size(16.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "${archive.categoryName} 训练完成",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "用时 ${archive.durationSeconds / 60} 分钟 · 共 ${archive.totalSets} 组",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Text(
                text = SimpleDateFormat("MM-dd HH:mm", Locale.CHINA).format(Date(archive.completedAt)),
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * 智能兜底与离线补全弹窗（解决“识别失败直接退出什么都没有”的问题）
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun IntelligentFallbackDialog(
    reason: String,
    selectedCapacityMl: Int,
    onSelectPreset: (ScannedProductInfo) -> Unit,
    onRetryCamera: () -> Unit,
    onDismiss: () -> Unit
) {
    var customName by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Rounded.Warning,
                    contentDescription = null,
                    tint = Color(0xFFF59E0B),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("智能快速录入", fontWeight = FontWeight.Bold, fontSize = 17.sp)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "网络波动或包装成分表较模糊未能自动识别。别担心，点击下方预设或直接输入名称，即可一键记录：",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 16.sp
                )

                // 常用预设饮品标签
                Text("常见饮品一键补全：", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val presets = listOf(
                        Triple("无糖茶饮 (0卡0糖)", 0.0, 0.0),
                        Triple("果汁茶饮料 (苹果茉莉)", 156.5, 37.5),
                        Triple("运动电解质水", 50.0, 12.0),
                        Triple("零度可乐 (0卡0糖)", 0.0, 0.0),
                        Triple("乳清蛋白饮 (25g蛋白)", 125.0, 2.5),
                        Triple("美式黑咖啡 (0糖)", 5.0, 0.0)
                    )

                    presets.forEach { (name, kcal, sugar) ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
                                .clickable {
                                    val isExceeded = sugar > 25.0
                                    onSelectPreset(
                                        ScannedProductInfo(
                                            productName = name,
                                            bottleCapacityMl = selectedCapacityMl,
                                            totalEnergyKcal = kcal,
                                            totalSugarG = sugar,
                                            isSugarExceeded = isExceeded,
                                            healthEvaluation = if (isExceeded) "添加糖已超过25g推荐上限，建议适量饮用" else "热量适宜，可正常饮用",
                                            micronutrientAdvice = "运动后及时补水并关注电解质平衡"
                                        )
                                    )
                                }
                                .padding(horizontal = 9.dp, vertical = 6.dp)
                        ) {
                            Text(name, fontSize = 11.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }

                // 自定义名称快速保存
                OutlinedTextField(
                    value = customName,
                    onValueChange = { customName = it },
                    label = { Text("自定义饮品名称", fontSize = 11.sp) },
                    placeholder = { Text("如：茉莉花茶、椰子水", fontSize = 11.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            if (customName.isNotBlank()) {
                Button(
                    onClick = {
                        onSelectPreset(
                            ScannedProductInfo(
                                productName = customName.trim(),
                                bottleCapacityMl = selectedCapacityMl,
                                totalEnergyKcal = 80.0,
                                totalSugarG = 10.0,
                                healthEvaluation = "自定义饮品记录",
                                micronutrientAdvice = "运动后注意电解质补充"
                            )
                        )
                    }
                ) {
                    Text("保存记录")
                }
            } else {
                Button(onClick = onRetryCamera) {
                    Text("重新拍照")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

/**
 * AI 识图结果详情确认弹窗
 */
@Composable
private fun ScanResultDialog(
    product: ScannedProductInfo,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text("AI 营养成分解析结果", fontWeight = FontWeight.Bold, fontSize = 17.sp)
                if (product.brand.isNotBlank() || product.productName.isNotBlank()) {
                    Text(
                        text = if (product.brand.isNotBlank()) "${product.brand} · ${product.productName}" else product.productName,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFFF59E0B).copy(alpha = 0.15f))
                            .padding(8.dp)
                    ) {
                        Column {
                            Text("整瓶总热量", fontSize = 10.sp, color = Color(0xFFB45309))
                            Text("${product.totalEnergyKcal} kcal", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color(0xFFD97706))
                        }
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                if (product.isSugarExceeded) Color(0xFFEF4444).copy(alpha = 0.15f)
                                else Color(0xFF10B981).copy(alpha = 0.15f)
                            )
                            .padding(8.dp)
                    ) {
                        Column {
                            Text("整瓶含糖", fontSize = 10.sp, color = if (product.isSugarExceeded) Color(0xFFDC2626) else Color(0xFF047857))
                            Text("${product.totalSugarG} g", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = if (product.isSugarExceeded) Color(0xFFDC2626) else Color(0xFF059669))
                        }
                    }
                }

                if (product.isSugarExceeded) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFFFEF2F2))
                            .padding(horizontal = 8.dp, vertical = 5.dp)
                    ) {
                        Text(
                            text = "⚠️ 整瓶含糖已超标 (${product.totalSugarG}g > 25g 推荐线)",
                            color = Color(0xFFDC2626),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                if (product.healthEvaluation.isNotBlank()) {
                    Text(
                        text = "🔬 控糖评价: ${product.healthEvaluation}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        lineHeight = 15.sp
                    )
                }

                if (product.micronutrientAdvice.isNotBlank()) {
                    Text(
                        text = "💡 微量元素: ${product.micronutrientAdvice}",
                        fontSize = 11.sp,
                        color = Color(0xFF047857),
                        lineHeight = 15.sp
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = onConfirm, shape = RoundedCornerShape(10.dp)) {
                Text("存入记录")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}
