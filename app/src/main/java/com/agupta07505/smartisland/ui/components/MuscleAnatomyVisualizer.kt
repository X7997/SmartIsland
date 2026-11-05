/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 */

package com.agupta07505.smartisland.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * 训练部位枚举与统一颜色规范
 */
enum class MuscleCategory(val displayName: String, val color: Color) {
    CHEST("胸部", Color(0xFFF43F5E)),        // 玫红
    BACK("背部", Color(0xFF3B82F6)),         // 科技蓝
    SHOULDERS("肩部", Color(0xFFF97316)),    // 活力橙
    ARMS("手臂", Color(0xFF06B6D4)),         // 冰青色
    ABS("腹部", Color(0xFF10B981)),          // 翡翠绿
    GLUTES("臀部", Color(0xFF8B5CF6)),       // 梦幻紫
    LEGS("腿部", Color(0xFFFF3B30)),         // 健身房器械经典运动红
    CALVES("小腿/足踝", Color(0xFF14B8A6)),   // 青碧色
    GENERAL("综合", Color(0xFF64748B))
}

/**
 * 细分目标肌群定义
 */
enum class TargetMuscle(
    val displayName: String,
    val category: MuscleCategory,
    val isFront: Boolean = true,
    val isBack: Boolean = false
) {
    // 1. 胸部
    CHEST_MAJOR("胸大肌", MuscleCategory.CHEST, isFront = true, isBack = false),
    UPPER_CHEST("胸大肌上部", MuscleCategory.CHEST, isFront = true, isBack = false),

    // 2. 腹部/核心
    ABS_RECTUS("腹直肌/核心", MuscleCategory.ABS, isFront = true, isBack = false),
    LOWER_ABS("下腹/核心", MuscleCategory.ABS, isFront = true, isBack = false),
    OBLIQUES("腹外斜肌", MuscleCategory.ABS, isFront = true, isBack = false),

    // 3. 肩部
    DELTOIDS_FRONT("三角肌前束", MuscleCategory.SHOULDERS, isFront = true, isBack = false),
    DELTOIDS_LATERAL("三角肌中束", MuscleCategory.SHOULDERS, isFront = true, isBack = true),
    DELTOIDS_REAR("三角肌后束", MuscleCategory.SHOULDERS, isFront = false, isBack = true),
    DELTOIDS("三角肌", MuscleCategory.SHOULDERS, isFront = true, isBack = true),

    // 4. 手臂
    BICEPS("肱二头肌", MuscleCategory.ARMS, isFront = true, isBack = false),
    TRICEPS("肱三头肌", MuscleCategory.ARMS, isFront = false, isBack = true),
    FOREARMS("前臂肌群", MuscleCategory.ARMS, isFront = true, isBack = true),

    // 5. 背部
    LAT_DORSI("背阔肌", MuscleCategory.BACK, isFront = false, isBack = true),
    MID_BACK("中背/菱形肌", MuscleCategory.BACK, isFront = false, isBack = true),
    TRAPEZIUS("斜方肌", MuscleCategory.BACK, isFront = false, isBack = true),
    LOWER_BACK("下背/竖脊肌", MuscleCategory.BACK, isFront = false, isBack = true),

    // 6. 臀部
    GLUTE_MAXIMUS("臀大肌", MuscleCategory.GLUTES, isFront = false, isBack = true),
    GLUTE_MEDIUS("臀中肌", MuscleCategory.GLUTES, isFront = false, isBack = true),

    // 7. 腿部
    QUADS("股四头肌", MuscleCategory.LEGS, isFront = true, isBack = false),
    HAMSTRINGS("腘绳肌/大腿后侧", MuscleCategory.LEGS, isFront = false, isBack = true),
    ADDUCTORS("大腿内收肌群", MuscleCategory.LEGS, isFront = true, isBack = false),

    // 8. 小腿/足踝
    CALVES("小腿/腓肠肌", MuscleCategory.CALVES, isFront = true, isBack = true),
    TIBIALIS_ANTERIOR("胫骨前肌", MuscleCategory.CALVES, isFront = true, isBack = false)
}

/**
 * 肌肉显示状态：
 * 1. DEFAULT: 默认状态（纯白色描边，内部透明底）
 * 2. CATEGORY_FOCUSED: 部位训练（灰色填充，白色描边）
 * 3. ACTIVE_FLASHING: 动作训练（高饱和运动红呼吸闪烁，白色高光边界）
 */
enum class MuscleVisualState {
    DEFAULT,
    CATEGORY_FOCUSED,
    ACTIVE_FLASHING
}

/**
 * 智能肌肉与部位匹配器
 */
object MuscleMatcher {
    fun match(exerciseName: String, categoryName: String, explicitTarget: String? = null): TargetMuscle {
        val target = explicitTarget?.lowercase().orEmpty()
        val name = exerciseName.lowercase()
        val cat = categoryName.lowercase()

        return when {
            // 显式目标优先匹配
            target.contains("胸大肌上") || target.contains("上胸") -> TargetMuscle.UPPER_CHEST
            target.contains("胸") -> TargetMuscle.CHEST_MAJOR
            target.contains("下腹") -> TargetMuscle.LOWER_ABS
            target.contains("斜肌") -> TargetMuscle.OBLIQUES
            target.contains("腹") || target.contains("核心") -> TargetMuscle.ABS_RECTUS
            target.contains("后束") -> TargetMuscle.DELTOIDS_REAR
            target.contains("中束") -> TargetMuscle.DELTOIDS_LATERAL
            target.contains("前束") -> TargetMuscle.DELTOIDS_FRONT
            target.contains("三角") || target.contains("肩") -> TargetMuscle.DELTOIDS
            target.contains("二头") -> TargetMuscle.BICEPS
            target.contains("三头") -> TargetMuscle.TRICEPS
            target.contains("前臂") -> TargetMuscle.FOREARMS
            target.contains("斜方") -> TargetMuscle.TRAPEZIUS
            target.contains("下背") || target.contains("竖脊") -> TargetMuscle.LOWER_BACK
            target.contains("中背") || target.contains("菱形") || target.contains("肩胛") -> TargetMuscle.MID_BACK
            target.contains("背阔") -> TargetMuscle.LAT_DORSI
            target.contains("臀中") -> TargetMuscle.GLUTE_MEDIUS
            target.contains("臀大") || target.contains("臀推") -> TargetMuscle.GLUTE_MAXIMUS
            target.contains("腘绳") || target.contains("腿弯举") || target.contains("后侧") -> TargetMuscle.HAMSTRINGS
            target.contains("内收") -> TargetMuscle.ADDUCTORS
            target.contains("四头") || target.contains("倒蹬") || target.contains("深蹲") || target.contains("腿屈伸") -> TargetMuscle.QUADS
            target.contains("胫骨") -> TargetMuscle.TIBIALIS_ANTERIOR
            target.contains("小腿") || target.contains("提踵") || target.contains("足踝") -> TargetMuscle.CALVES

            // 动作名称匹配
            name.contains("上斜") && (name.contains("推胸") || name.contains("卧推")) -> TargetMuscle.UPPER_CHEST
            name.contains("推胸") || name.contains("夹胸") || name.contains("卧推") -> TargetMuscle.CHEST_MAJOR
            name.contains("平板支撑") || name.contains("卷腹") -> TargetMuscle.ABS_RECTUS
            name.contains("下腹") || name.contains("屈腿") || name.contains("举腿") -> TargetMuscle.LOWER_ABS
            name.contains("反向飞鸟") || (name.contains("面拉") && (cat.contains("肩") || cat.contains("背"))) -> TargetMuscle.DELTOIDS_REAR
            name.contains("侧平举") -> TargetMuscle.DELTOIDS_LATERAL
            name.contains("前平举") -> TargetMuscle.DELTOIDS_FRONT
            name.contains("腿弯举") || name.contains("直腿硬拉") -> TargetMuscle.HAMSTRINGS
            name.contains("倒蹬") || name.contains("腿屈伸") || name.contains("深蹲") -> TargetMuscle.QUADS
            name.contains("髋外展") -> TargetMuscle.GLUTE_MEDIUS
            name.contains("髋内收") -> TargetMuscle.ADDUCTORS
            name.contains("臀推") || name.contains("臀桥") -> TargetMuscle.GLUTE_MAXIMUS
            name.contains("提踵") || name.contains("小腿") -> TargetMuscle.CALVES
            name.contains("弯举") -> TargetMuscle.BICEPS
            name.contains("下压") || name.contains("臂屈伸") -> TargetMuscle.TRICEPS
            name.contains("划船") -> TargetMuscle.MID_BACK
            name.contains("下拉") || name.contains("引体向上") -> TargetMuscle.LAT_DORSI

            // 类别兜底
            cat.contains("胸") -> TargetMuscle.CHEST_MAJOR
            cat.contains("腹") -> TargetMuscle.ABS_RECTUS
            cat.contains("肩") -> TargetMuscle.DELTOIDS
            cat.contains("背") -> TargetMuscle.LAT_DORSI
            cat.contains("臀") -> TargetMuscle.GLUTE_MAXIMUS
            cat.contains("腿") -> TargetMuscle.QUADS
            cat.contains("臂") || cat.contains("手") -> TargetMuscle.BICEPS
            else -> TargetMuscle.QUADS
        }
    }

    /**
     * 判断某肌肉是否属于当前训练的整体部位分类（部位训练 -> 灰色显示）
     */
    fun isMuscleInCategory(muscle: TargetMuscle, categoryName: String): Boolean {
        val cat = categoryName.trim().lowercase()
        if (cat.isBlank()) return false

        return when {
            cat.contains("臀腿") -> {
                muscle.category == MuscleCategory.GLUTES ||
                muscle.category == MuscleCategory.LEGS ||
                muscle.category == MuscleCategory.CALVES
            }
            cat.contains("腿") -> {
                muscle.category == MuscleCategory.LEGS ||
                muscle.category == MuscleCategory.CALVES
            }
            cat.contains("臀") -> {
                muscle.category == MuscleCategory.GLUTES
            }
            cat.contains("胸") -> {
                muscle.category == MuscleCategory.CHEST
            }
            cat.contains("背") -> {
                muscle.category == MuscleCategory.BACK
            }
            cat.contains("肩") -> {
                muscle.category == MuscleCategory.SHOULDERS
            }
            cat.contains("腹") || cat.contains("核心") -> {
                muscle.category == MuscleCategory.ABS
            }
            cat.contains("臂") || cat.contains("手") -> {
                muscle.category == MuscleCategory.ARMS
            }
            else -> false
        }
    }

    fun getCategoryColor(categoryName: String): Color {
        val cat = categoryName.lowercase()
        return when {
            cat.contains("胸") -> MuscleCategory.CHEST.color
            cat.contains("腹") -> MuscleCategory.ABS.color
            cat.contains("臀") -> MuscleCategory.GLUTES.color
            cat.contains("腿") -> MuscleCategory.LEGS.color
            cat.contains("背") -> MuscleCategory.BACK.color
            cat.contains("肩") -> MuscleCategory.SHOULDERS.color
            cat.contains("臂") || cat.contains("手") -> MuscleCategory.ARMS.color
            cat.contains("小腿") || cat.contains("足踝") -> MuscleCategory.CALVES.color
            else -> Color(0xFF64748B)
        }
    }
}

/**
 * 健身房器械解剖人体正反双面高精矢量图组件 (Gym Equipment Anatomy Dual View)
 *
 * 遵循用户核心视觉规范：
 * 1. 默认状态：纯黑底色，白色描边描绘所有肌肉群（正反两体，涵盖全身）。
 * 2. 部位训练：当训练到具体部位（如臀腿、背部、胸部），该部位的所有肌肉全部呈现高级冷金属银灰底色。
 * 3. 动作训练：训练到具体器械或动作时，对应肌肉呈现高饱和经典运动红（与健身房实拍图一致）呼吸闪烁显示。
 */
@Composable
fun GymMuscleAnatomyDualView(
    activeMuscle: TargetMuscle?,
    activeCategory: String,
    modifier: Modifier = Modifier,
    highlightedCategories: Set<String> = emptySet(),
    isDarkTheme: Boolean = true,
    showLabels: Boolean = true,
    showStatusBadge: Boolean = true
) {
    val infiniteTransition = rememberInfiniteTransition(label = "gymMusclePulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.45f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    // 高级配色定义
    val cardBackground = Color(0xFF090D14)
    val cardBorder = Color(0xFF334155).copy(alpha = 0.45f)
    val labelColor = Color(0xFF94A3B8)
    val activeRed = Color(0xFFFF3B30) // 健身房实拍器械同款高能运动红

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(cardBackground)
            .border(1.dp, cardBorder, RoundedCornerShape(16.dp))
            .padding(vertical = 10.dp, horizontal = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            // 顶端标签栏（可选）
            if (showLabels) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "背面 · BACK",
                        color = labelColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 0.8.sp
                    )
                    Text(
                        text = "正面 · FRONT",
                        color = labelColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 0.8.sp
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
            }

            // 双面人体解剖矢量画布 (左: 背面, 右: 正面)
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
            ) {
                val totalWidth = size.width
                val totalHeight = size.height
                val gap = 16.dp.toPx()
                val figureWidth = (totalWidth - gap) / 2f
                val scale = minOf(figureWidth / 100f, totalHeight / 180f)

                val leftOriginX = (figureWidth - 100f * scale) / 2f
                val rightOriginX = figureWidth + gap + (figureWidth - 100f * scale) / 2f
                val originY = (totalHeight - 180f * scale) / 2f

                // 1. 绘制左侧人体：背面 (Back View)
                drawBackAnatomyFigure(
                    originX = leftOriginX,
                    originY = originY,
                    scale = scale,
                    activeMuscle = activeMuscle,
                    activeCategory = activeCategory,
                    highlightedCategories = highlightedCategories,
                    pulseAlpha = pulseAlpha,
                    activeColor = activeRed
                )

                // 2. 绘制右侧人体：正面 (Front View)
                drawFrontAnatomyFigure(
                    originX = rightOriginX,
                    originY = originY,
                    scale = scale,
                    activeMuscle = activeMuscle,
                    activeCategory = activeCategory,
                    highlightedCategories = highlightedCategories,
                    pulseAlpha = pulseAlpha,
                    activeColor = activeRed
                )
            }

            // 底端当前激活状态指示胶囊
            if (showStatusBadge) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 一周训练图点亮部位徽标
                    if (highlightedCategories.isNotEmpty()) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFF3B82F6).copy(alpha = 0.22f))
                                .border(0.8.dp, Color(0xFF3B82F6).copy(alpha = 0.65f), RoundedCornerShape(6.dp))
                                .padding(horizontal = 7.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "本周已点亮: ${highlightedCategories.joinToString("、")}",
                                color = Color(0xFF60A5FA),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // 部位训练灰标
                    if (activeCategory.isNotBlank()) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFF64748B).copy(alpha = 0.25f))
                                .border(0.8.dp, Color(0xFF64748B).copy(alpha = 0.6f), RoundedCornerShape(6.dp))
                                .padding(horizontal = 7.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "训练部位: $activeCategory",
                                color = Color(0xFFCBD5E1),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    // 动作聚焦红标 (带呼吸脉冲微点)
                    if (activeMuscle != null) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(activeRed.copy(alpha = 0.22f))
                                .border(0.8.dp, activeRed.copy(alpha = 0.65f), RoundedCornerShape(6.dp))
                                .padding(horizontal = 7.dp, vertical = 2.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(5.dp)
                                        .clip(CircleShape)
                                        .background(activeRed.copy(alpha = pulseAlpha))
                                )
                                Text(
                                    text = "动作聚焦: ${activeMuscle.displayName}",
                                    color = activeRed,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * 兼容旧版名称的委托调用
 */
@Composable
fun FemaleMuscleAnatomyDualView(
    activeMuscle: TargetMuscle?,
    activeCategory: String,
    modifier: Modifier = Modifier,
    isDarkTheme: Boolean = false,
    onMuscleSelected: ((TargetMuscle) -> Unit)? = null
) {
    GymMuscleAnatomyDualView(
        activeMuscle = activeMuscle,
        activeCategory = activeCategory,
        modifier = modifier,
        isDarkTheme = true,
        showLabels = true,
        showStatusBadge = false
    )
}

/**
 * 紧凑型灵动岛展开大卡片右侧肌肉解剖 HUD
 */
@Composable
fun MuscleAnatomyVisualizer(
    exerciseName: String,
    categoryName: String,
    isResting: Boolean = false,
    modifier: Modifier = Modifier
) {
    val muscle = remember(exerciseName, categoryName) {
        MuscleMatcher.match(exerciseName, categoryName)
    }

    val infiniteTransition = rememberInfiniteTransition(label = "islandMusclePulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.45f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    val activeRed = Color(0xFFFF3B30)

    Box(
        modifier = modifier
            .width(96.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF070B12).copy(alpha = 0.85f))
            .border(1.dp, Color(0xFF334155).copy(alpha = 0.5f), RoundedCornerShape(12.dp))
            .padding(vertical = 5.dp, horizontal = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // 双面矢量人体缩略图
            Canvas(
                modifier = Modifier.size(width = 86.dp, height = 72.dp)
            ) {
                val totalWidth = size.width
                val totalHeight = size.height
                val gap = 6.dp.toPx()
                val figureWidth = (totalWidth - gap) / 2f
                val scale = minOf(figureWidth / 100f, totalHeight / 180f)

                val leftOriginX = (figureWidth - 100f * scale) / 2f
                val rightOriginX = figureWidth + gap + (figureWidth - 100f * scale) / 2f
                val originY = (totalHeight - 180f * scale) / 2f

                drawBackAnatomyFigure(
                    originX = leftOriginX,
                    originY = originY,
                    scale = scale,
                    activeMuscle = muscle,
                    activeCategory = categoryName,
                    pulseAlpha = pulseAlpha,
                    activeColor = activeRed
                )

                drawFrontAnatomyFigure(
                    originX = rightOriginX,
                    originY = originY,
                    scale = scale,
                    activeMuscle = muscle,
                    activeCategory = categoryName,
                    pulseAlpha = pulseAlpha,
                    activeColor = activeRed
                )
            }

            Spacer(modifier = Modifier.height(3.dp))

            // 底部肌群标签 Chip
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(5.dp))
                    .background(activeRed.copy(alpha = 0.22f))
                    .border(0.6.dp, activeRed.copy(alpha = 0.6f), RoundedCornerShape(5.dp))
                    .padding(horizontal = 5.dp, vertical = 1.dp)
            ) {
                Text(
                    text = muscle.displayName,
                    color = activeRed,
                    fontSize = 9.5.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    maxLines = 1
                )
            }
        }
    }
}

// ======================================================================================
// 核心矢量绘制引擎 (Core Vector Drawing Engine)
// 100x180 归一化解剖坐标系，支持无损按比例任意缩放
// ======================================================================================

/**
 * 肌肉几何形状状态渲染
 */
private fun DrawScope.renderMusclePath(
    path: Path,
    state: MuscleVisualState,
    scale: Float,
    activeColor: Color,
    pulseAlpha: Float
) {
    val strokeWidth = 1.15f * scale
    val whiteStroke = Color.White.copy(alpha = 0.92f)
    // 用户指定视觉规范：点亮时采用高对比科技亮蓝，杜绝灰色在暗色背景下看不清的问题
    val focusedBlueFill = Color(0xFF2563EB).copy(alpha = 0.78f)
    val focusedBlueStroke = Color(0xFF93C5FD).copy(alpha = 0.95f)

    when (state) {
        MuscleVisualState.ACTIVE_FLASHING -> {
            // 1. 荧光发光层 (Glow Aura)
            drawPath(
                path = path,
                color = activeColor.copy(alpha = pulseAlpha * 0.38f),
                style = Stroke(width = 3.6f * scale)
            )
            // 2. 核心红能填充
            drawPath(
                path = path,
                color = activeColor.copy(alpha = pulseAlpha * 0.94f),
                style = Fill
            )
            // 3. 边缘高光白描边
            drawPath(
                path = path,
                color = Color.White.copy(alpha = 0.98f),
                style = Stroke(width = strokeWidth)
            )
        }
        MuscleVisualState.CATEGORY_FOCUSED -> {
            // 部位训练/周训练点亮：纯粹高能科技亮蓝底色填充，辅以浅天蓝高光轮廓
            drawPath(
                path = path,
                color = focusedBlueFill,
                style = Fill
            )
            drawPath(
                path = path,
                color = focusedBlueStroke,
                style = Stroke(width = strokeWidth)
            )
        }
        MuscleVisualState.DEFAULT -> {
            // 默认状态：纯黑底透明，仅白色精细描边
            drawPath(
                path = path,
                color = whiteStroke,
                style = Stroke(width = strokeWidth)
            )
        }
    }
}

/**
 * 计算特定肌肉的视觉呈现状态
 */
private fun resolveMuscleState(
    muscle: TargetMuscle,
    activeMuscle: TargetMuscle?,
    activeCategory: String,
    highlightedCategories: Set<String> = emptySet()
): MuscleVisualState {
    return when {
        // 当前动作核心目标肌肉 -> 闪烁显示
        activeMuscle == muscle -> MuscleVisualState.ACTIVE_FLASHING
        // 属于当前训练的整体大部位 -> 灰色显示
        activeCategory.isNotBlank() && MuscleMatcher.isMuscleInCategory(muscle, activeCategory) -> MuscleVisualState.CATEGORY_FOCUSED
        // 属于一周训练图中点亮的部位集合 -> 高级高亮/聚焦显示 (冷金属灰色)
        highlightedCategories.any { MuscleMatcher.isMuscleInCategory(muscle, it) } -> MuscleVisualState.CATEGORY_FOCUSED
        // 默认状态 -> 白色描边
        else -> MuscleVisualState.DEFAULT
    }
}

// --------------------------------------------------------------------------------------
// 1. 绘制正面人体 (Front View)
// 包含：头部、胸大肌、腹直肌、腹外斜肌、三角肌前中束、二头肌、前臂、股四头肌(3束精细解剖)、内收肌、胫骨前肌、足
// --------------------------------------------------------------------------------------
private fun DrawScope.drawFrontAnatomyFigure(
    originX: Float,
    originY: Float,
    scale: Float,
    activeMuscle: TargetMuscle?,
    activeCategory: String,
    highlightedCategories: Set<String> = emptySet(),
    pulseAlpha: Float,
    activeColor: Color
) {
    fun x(v: Float): Float = originX + v * scale
    fun y(v: Float): Float = originY + v * scale
    fun resolve(m: TargetMuscle) = resolveMuscleState(m, activeMuscle, activeCategory, highlightedCategories)

    val bodyStrokeColor = Color.White.copy(alpha = 0.90f)
    val bodyBaseFill = Color(0xFF1E293B).copy(alpha = 0.20f)
    val strokeWidth = 1.15f * scale

    // 头部轮廓
    val headPath = Path().apply {
        addOval(androidx.compose.ui.geometry.Rect(x(42.5f), y(4.5f), x(57.5f), y(23.5f)))
    }
    drawPath(headPath, color = bodyBaseFill, style = Fill)
    drawPath(headPath, color = bodyStrokeColor, style = Stroke(strokeWidth))

    // 颈部轮廓线
    drawLine(bodyStrokeColor, Offset(x(45.5f), y(22f)), Offset(x(45f), y(29f)), strokeWidth)
    drawLine(bodyStrokeColor, Offset(x(54.5f), y(22f)), Offset(x(55f), y(29f)), strokeWidth)

    // 1. 胸大肌 (Pectoralis Major)
    val chestState = resolve(TargetMuscle.CHEST_MAJOR)
    val leftChest = Path().apply {
        moveTo(x(49f), y(31f))
        lineTo(x(36f), y(31f))
        lineTo(x(34f), y(40f))
        cubicTo(x(34f), y(47f), x(40f), y(50f), x(48.5f), y(49f))
        close()
    }
    val rightChest = Path().apply {
        moveTo(x(51f), y(31f))
        lineTo(x(64f), y(31f))
        lineTo(x(66f), y(40f))
        cubicTo(x(66f), y(47f), x(60f), y(50f), x(51.5f), y(49f))
        close()
    }
    renderMusclePath(leftChest, chestState, scale, activeColor, pulseAlpha)
    renderMusclePath(rightChest, chestState, scale, activeColor, pulseAlpha)
    // 胸骨中缝线
    drawLine(bodyStrokeColor, Offset(x(50f), y(31f)), Offset(x(50f), y(49f)), strokeWidth)

    // 2. 三角肌前中束 (Deltoids)
    val shoulderState = resolve(TargetMuscle.DELTOIDS)
    val leftDelt = Path().apply {
        moveTo(x(36f), y(29f))
        lineTo(x(25f), y(32f))
        cubicTo(x(20f), y(37f), x(19f), y(44f), x(23f), y(49f))
        lineTo(x(31f), y(46f))
        lineTo(x(35f), y(37f))
        close()
    }
    val rightDelt = Path().apply {
        moveTo(x(64f), y(29f))
        lineTo(x(75f), y(32f))
        cubicTo(x(80f), y(37f), x(81f), y(44f), x(77f), y(49f))
        lineTo(x(69f), y(46f))
        lineTo(x(65f), y(37f))
        close()
    }
    renderMusclePath(leftDelt, shoulderState, scale, activeColor, pulseAlpha)
    renderMusclePath(rightDelt, shoulderState, scale, activeColor, pulseAlpha)

    // 3. 肱二头肌 (Biceps)
    val bicepState = resolve(TargetMuscle.BICEPS)
    val leftBicep = Path().apply {
        moveTo(x(23f), y(49f))
        cubicTo(x(19f), y(54f), x(18f), y(64f), x(21f), y(71f))
        cubicTo(x(26f), y(71f), x(28f), y(62f), x(28f), y(51f))
        close()
    }
    val rightBicep = Path().apply {
        moveTo(x(77f), y(49f))
        cubicTo(x(81f), y(54f), x(82f), y(64f), x(79f), y(71f))
        cubicTo(x(74f), y(71f), x(72f), y(62f), x(72f), y(51f))
        close()
    }
    renderMusclePath(leftBicep, bicepState, scale, activeColor, pulseAlpha)
    renderMusclePath(rightBicep, bicepState, scale, activeColor, pulseAlpha)

    // 4. 前臂屈肌群 (Forearms)
    val forearmState = resolve(TargetMuscle.FOREARMS)
    val leftForearm = Path().apply {
        moveTo(x(21f), y(71f))
        cubicTo(x(16f), y(76f), x(14f), y(88f), x(17f), y(102f))
        lineTo(x(22f), y(101f))
        cubicTo(x(25f), y(88f), x(27f), y(78f), x(26f), y(71f))
        close()
    }
    val rightForearm = Path().apply {
        moveTo(x(79f), y(71f))
        cubicTo(x(84f), y(76f), x(86f), y(88f), x(83f), y(102f))
        lineTo(x(78f), y(101f))
        cubicTo(x(75f), y(88f), x(73f), y(78f), x(74f), y(71f))
        close()
    }
    renderMusclePath(leftForearm, forearmState, scale, activeColor, pulseAlpha)
    renderMusclePath(rightForearm, forearmState, scale, activeColor, pulseAlpha)

    // 手掌轮廓
    val leftHand = Path().apply {
        moveTo(x(17f), y(102f))
        cubicTo(x(13f), y(108f), x(14f), y(116f), x(18f), y(117f))
        cubicTo(x(22f), y(116f), x(22f), y(108f), x(22f), y(101f))
        close()
    }
    val rightHand = Path().apply {
        moveTo(x(83f), y(102f))
        cubicTo(x(87f), y(108f), x(86f), y(116f), x(82f), y(117f))
        cubicTo(x(78f), y(116f), x(78f), y(108f), x(78f), y(101f))
        close()
    }
    drawPath(leftHand, color = bodyBaseFill, style = Fill)
    drawPath(leftHand, color = bodyStrokeColor, style = Stroke(strokeWidth))
    drawPath(rightHand, color = bodyBaseFill, style = Fill)
    drawPath(rightHand, color = bodyStrokeColor, style = Stroke(strokeWidth))

    // 5. 腹直肌 (Rectus Abdominis 6块排布)
    val absState = resolve(TargetMuscle.ABS_RECTUS)
    val absRects: List<Pair<Rect, Rect>> = listOf(
        Pair(Rect(x(41f), y(51f), x(48.5f), y(57f)), Rect(x(51.5f), y(51f), x(59f), y(57f))),
        Pair(Rect(x(41.5f), y(58.5f), x(48.5f), y(65.5f)), Rect(x(51.5f), y(58.5f), x(58.5f), y(65.5f))),
        Pair(Rect(x(42f), y(67f), x(48.5f), y(75f)), Rect(x(51.5f), y(67f), x(58f), y(75f)))
    )
    absRects.forEach { (lRect, rRect) ->
        val lPath = Path().apply { addRoundRect(RoundRect(lRect, CornerRadius(1.2f * scale))) }
        val rPath = Path().apply { addRoundRect(RoundRect(rRect, CornerRadius(1.2f * scale))) }
        renderMusclePath(lPath, absState, scale, activeColor, pulseAlpha)
        renderMusclePath(rPath, absState, scale, activeColor, pulseAlpha)
    }

    // 6. 腹外斜肌 (Obliques)
    val obliqueState = resolve(TargetMuscle.OBLIQUES)
    val leftOblique = Path().apply {
        moveTo(x(35f), y(51f))
        lineTo(x(40f), y(52f))
        cubicTo(x(40.5f), y(64f), x(41f), y(72f), x(36f), y(76f))
        lineTo(x(33f), y(64f))
        close()
    }
    val rightOblique = Path().apply {
        moveTo(x(65f), y(51f))
        lineTo(x(60f), y(52f))
        cubicTo(x(59.5f), y(64f), x(59f), y(72f), x(64f), y(76f))
        lineTo(x(67f), y(64f))
        close()
    }
    renderMusclePath(leftOblique, obliqueState, scale, activeColor, pulseAlpha)
    renderMusclePath(rightOblique, obliqueState, scale, activeColor, pulseAlpha)

    // 骨盆与腹股沟轮廓
    drawLine(bodyStrokeColor, Offset(x(36f), y(76f)), Offset(x(49f), y(87f)), strokeWidth)
    drawLine(bodyStrokeColor, Offset(x(64f), y(76f)), Offset(x(51f), y(87f)), strokeWidth)

    // 7. 股四头肌 (Quadriceps - 健身房照片核心红亮区域，3大肌束精细分割)
    val quadState = resolve(TargetMuscle.QUADS)

    // (A) 股直肌 (Rectus Femoris - 大腿中央水滴形核心)
    val leftRectusFemoris = Path().apply {
        moveTo(x(40.5f), y(87f))
        cubicTo(x(37.5f), y(96f), x(37.5f), y(112f), x(41f), y(126f))
        cubicTo(x(44.5f), y(112f), x(44.5f), y(96f), x(42.5f), y(87f))
        close()
    }
    val rightRectusFemoris = Path().apply {
        moveTo(x(59.5f), y(87f))
        cubicTo(x(62.5f), y(96f), x(62.5f), y(112f), x(59f), y(126f))
        cubicTo(x(55.5f), y(112f), x(55.5f), y(96f), x(57.5f), y(87f))
        close()
    }
    renderMusclePath(leftRectusFemoris, quadState, scale, activeColor, pulseAlpha)
    renderMusclePath(rightRectusFemoris, quadState, scale, activeColor, pulseAlpha)

    // (B) 股外侧肌 (Vastus Lateralis - 大腿外侧饱满外凸曲线)
    val leftVastusLat = Path().apply {
        moveTo(x(34f), y(84f))
        cubicTo(x(30f), y(96f), x(31f), y(114f), x(37f), y(126f))
        cubicTo(x(37f), y(112f), x(37.5f), y(96f), x(39.5f), y(87f))
        close()
    }
    val rightVastusLat = Path().apply {
        moveTo(x(66f), y(84f))
        cubicTo(x(70f), y(96f), x(69f), y(114f), x(63f), y(126f))
        cubicTo(x(63f), y(112f), x(62.5f), y(96f), x(60.5f), y(87f))
        close()
    }
    renderMusclePath(leftVastusLat, quadState, scale, activeColor, pulseAlpha)
    renderMusclePath(rightVastusLat, quadState, scale, activeColor, pulseAlpha)

    // (C) 股内侧肌 (Vastus Medialis - 膝盖内上方泪滴形水滴)
    val leftVastusMed = Path().apply {
        moveTo(x(43f), y(110f))
        cubicTo(x(46f), y(115f), x(46f), y(124f), x(41.5f), y(127f))
        cubicTo(x(41f), y(122f), x(42f), y(115f), x(43f), y(110f))
        close()
    }
    val rightVastusMed = Path().apply {
        moveTo(x(57f), y(110f))
        cubicTo(x(54f), y(115f), x(54f), y(124f), x(58.5f), y(127f))
        cubicTo(x(59f), y(122f), x(58f), y(115f), x(57f), y(110f))
        close()
    }
    renderMusclePath(leftVastusMed, quadState, scale, activeColor, pulseAlpha)
    renderMusclePath(rightVastusMed, quadState, scale, activeColor, pulseAlpha)

    // 8. 大腿内收肌群 (Adductors)
    val adductorState = resolve(TargetMuscle.ADDUCTORS)
    val leftAdductor = Path().apply {
        moveTo(x(44f), y(87f))
        lineTo(x(49f), y(87f))
        cubicTo(x(48f), y(97f), x(46f), y(107f), x(43.5f), y(110f))
        close()
    }
    val rightAdductor = Path().apply {
        moveTo(x(56f), y(87f))
        lineTo(x(51f), y(87f))
        cubicTo(x(52f), y(97f), x(54f), y(107f), x(56.5f), y(110f))
        close()
    }
    renderMusclePath(leftAdductor, adductorState, scale, activeColor, pulseAlpha)
    renderMusclePath(rightAdductor, adductorState, scale, activeColor, pulseAlpha)

    // 膝盖髌骨微圈
    drawCircle(bodyBaseFill, 3.2f * scale, Offset(x(41f), y(131f)))
    drawCircle(bodyStrokeColor, 3.2f * scale, Offset(x(41f), y(131f)), style = Stroke(strokeWidth))
    drawCircle(bodyBaseFill, 3.2f * scale, Offset(x(59f), y(131f)))
    drawCircle(bodyStrokeColor, 3.2f * scale, Offset(x(59f), y(131f)), style = Stroke(strokeWidth))

    // 9. 小腿胫骨前肌与腓肠肌前轮廓 (Tibialis Anterior / Calves)
    val calfFrontState = resolve(TargetMuscle.CALVES)
    val leftShin = Path().apply {
        moveTo(x(37f), y(136f))
        cubicTo(x(32f), y(144f), x(33f), y(156f), x(37f), y(168f))
        lineTo(x(42f), y(168f))
        cubicTo(x(43f), y(156f), x(41f), y(144f), x(40f), y(136f))
        close()
    }
    val rightShin = Path().apply {
        moveTo(x(63f), y(136f))
        cubicTo(x(68f), y(144f), x(67f), y(156f), x(63f), y(168f))
        lineTo(x(58f), y(168f))
        cubicTo(x(57f), y(156f), x(59f), y(144f), x(60f), y(136f))
        close()
    }
    renderMusclePath(leftShin, calfFrontState, scale, activeColor, pulseAlpha)
    renderMusclePath(rightShin, calfFrontState, scale, activeColor, pulseAlpha)

    // 胫骨正面分界脊线
    drawLine(bodyStrokeColor, Offset(x(39f), y(137f)), Offset(x(39.5f), y(167f)), strokeWidth * 0.8f)
    drawLine(bodyStrokeColor, Offset(x(61f), y(137f)), Offset(x(60.5f), y(167f)), strokeWidth * 0.8f)

    // 10. 双脚正面轮廓 (Feet)
    val leftFoot = Path().apply {
        moveTo(x(37f), y(168f))
        lineTo(x(31f), y(175f))
        lineTo(x(33f), y(178f))
        lineTo(x(43f), y(178f))
        lineTo(x(42f), y(172f))
        lineTo(x(42f), y(168f))
        close()
    }
    val rightFoot = Path().apply {
        moveTo(x(63f), y(168f))
        lineTo(x(69f), y(175f))
        lineTo(x(67f), y(178f))
        lineTo(x(57f), y(178f))
        lineTo(x(58f), y(172f))
        lineTo(x(58f), y(168f))
        close()
    }
    drawPath(leftFoot, color = bodyBaseFill, style = Fill)
    drawPath(leftFoot, color = bodyStrokeColor, style = Stroke(strokeWidth))
    drawPath(rightFoot, color = bodyBaseFill, style = Fill)
    drawPath(rightFoot, color = bodyStrokeColor, style = Stroke(strokeWidth))
}

// --------------------------------------------------------------------------------------
// 2. 绘制背面人体 (Back View)
// 包含：后脑轮廓、斜方肌、中背菱形肌/肩胛、三角肌后束、背阔肌、竖脊肌、三头肌、前臂伸肌、臀大肌/臀中肌、腘绳肌(双束)、小腿腓肠肌与跟腱、脚后跟
// --------------------------------------------------------------------------------------
private fun DrawScope.drawBackAnatomyFigure(
    originX: Float,
    originY: Float,
    scale: Float,
    activeMuscle: TargetMuscle?,
    activeCategory: String,
    highlightedCategories: Set<String> = emptySet(),
    pulseAlpha: Float,
    activeColor: Color
) {
    fun x(v: Float): Float = originX + v * scale
    fun y(v: Float): Float = originY + v * scale
    fun resolve(m: TargetMuscle) = resolveMuscleState(m, activeMuscle, activeCategory, highlightedCategories)

    val bodyStrokeColor = Color.White.copy(alpha = 0.90f)
    val bodyBaseFill = Color(0xFF1E293B).copy(alpha = 0.20f)
    val strokeWidth = 1.15f * scale

    // 头部后脑轮廓
    val headPath = Path().apply {
        addOval(androidx.compose.ui.geometry.Rect(x(42.5f), y(4.5f), x(57.5f), y(23.5f)))
    }
    drawPath(headPath, color = bodyBaseFill, style = Fill)
    drawPath(headPath, color = bodyStrokeColor, style = Stroke(strokeWidth))

    // 1. 斜方肌 (Trapezius: 上颈部与中下背风筝菱形)
    val trapsState = resolve(TargetMuscle.TRAPEZIUS)
    val upperTraps = Path().apply {
        moveTo(x(46f), y(22f))
        lineTo(x(35f), y(29f))
        lineTo(x(40f), y(35f))
        lineTo(x(50f), y(32f))
        lineTo(x(60f), y(35f))
        lineTo(x(65f), y(29f))
        lineTo(x(54f), y(22f))
        close()
    }
    val midLowerTraps = Path().apply {
        moveTo(x(50f), y(32f))
        lineTo(x(40f), y(35f))
        lineTo(x(42f), y(46f))
        lineTo(x(50f), y(56f))
        lineTo(x(58f), y(46f))
        lineTo(x(60f), y(35f))
        close()
    }
    renderMusclePath(upperTraps, trapsState, scale, activeColor, pulseAlpha)
    renderMusclePath(midLowerTraps, trapsState, scale, activeColor, pulseAlpha)

    // 2. 三角肌后束 (Posterior Deltoids)
    val rearDeltState = resolve(TargetMuscle.DELTOIDS_REAR)
    val leftRearDelt = Path().apply {
        moveTo(x(35f), y(29f))
        cubicTo(x(24f), y(32f), x(20f), y(38f), x(23f), y(47f))
        lineTo(x(29f), y(44f))
        lineTo(x(33f), y(37f))
        close()
    }
    val rightRearDelt = Path().apply {
        moveTo(x(65f), y(29f))
        cubicTo(x(76f), y(32f), x(80f), y(38f), x(77f), y(47f))
        lineTo(x(71f), y(44f))
        lineTo(x(67f), y(37f))
        close()
    }
    renderMusclePath(leftRearDelt, rearDeltState, scale, activeColor, pulseAlpha)
    renderMusclePath(rightRearDelt, rearDeltState, scale, activeColor, pulseAlpha)

    // 3. 中背/菱形肌/冈下肌 (Mid Back / Infraspinatus)
    val midBackState = resolve(TargetMuscle.MID_BACK)
    val leftInfraspinatus = Path().apply {
        moveTo(x(39f), y(36f))
        lineTo(x(34f), y(33f))
        cubicTo(x(30f), y(38f), x(31f), y(46f), x(36f), y(48f))
        lineTo(x(41f), y(45f))
        close()
    }
    val rightInfraspinatus = Path().apply {
        moveTo(x(61f), y(36f))
        lineTo(x(66f), y(33f))
        cubicTo(x(70f), y(38f), x(69f), y(46f), x(64f), y(48f))
        lineTo(x(59f), y(45f))
        close()
    }
    renderMusclePath(leftInfraspinatus, midBackState, scale, activeColor, pulseAlpha)
    renderMusclePath(rightInfraspinatus, midBackState, scale, activeColor, pulseAlpha)

    // 4. 背阔肌 (Latissimus Dorsi 两侧大羽翼)
    val latState = resolve(TargetMuscle.LAT_DORSI)
    val leftLat = Path().apply {
        moveTo(x(36f), y(48f))
        lineTo(x(42f), y(47f))
        lineTo(x(48f), y(56f))
        lineTo(x(47f), y(72f))
        cubicTo(x(40f), y(72f), x(34f), y(62f), x(32f), y(51f))
        close()
    }
    val rightLat = Path().apply {
        moveTo(x(64f), y(48f))
        lineTo(x(58f), y(47f))
        lineTo(x(52f), y(56f))
        lineTo(x(53f), y(72f))
        cubicTo(x(60f), y(72f), x(66f), y(62f), x(68f), y(51f))
        close()
    }
    renderMusclePath(leftLat, latState, scale, activeColor, pulseAlpha)
    renderMusclePath(rightLat, latState, scale, activeColor, pulseAlpha)

    // 5. 竖脊肌 / 下背 (Erector Spinae)
    val lowerBackState = resolve(TargetMuscle.LOWER_BACK)
    val lowerBackPath = Path().apply {
        moveTo(x(48f), y(57f))
        lineTo(x(52f), y(57f))
        lineTo(x(53f), y(75f))
        lineTo(x(47f), y(75f))
        close()
    }
    renderMusclePath(lowerBackPath, lowerBackState, scale, activeColor, pulseAlpha)
    // 脊柱中线
    drawLine(bodyStrokeColor, Offset(x(50f), y(24f)), Offset(x(50f), y(75f)), strokeWidth * 0.9f)

    // 6. 肱三头肌 (Triceps Brachii)
    val tricepState = resolve(TargetMuscle.TRICEPS)
    val leftTricep = Path().apply {
        moveTo(x(24f), y(47f))
        cubicTo(x(19f), y(53f), x(18f), y(64f), x(22f), y(71f))
        cubicTo(x(27f), y(70f), x(28f), y(61f), x(28f), y(49f))
        close()
    }
    val rightTricep = Path().apply {
        moveTo(x(76f), y(47f))
        cubicTo(x(81f), y(53f), x(82f), y(64f), x(78f), y(71f))
        cubicTo(x(73f), y(70f), x(72f), y(61f), x(72f), y(49f))
        close()
    }
    renderMusclePath(leftTricep, tricepState, scale, activeColor, pulseAlpha)
    renderMusclePath(rightTricep, tricepState, scale, activeColor, pulseAlpha)
    // 三头肌长头与外侧头分割线
    drawLine(bodyStrokeColor, Offset(x(23f), y(52f)), Offset(x(24f), y(68f)), strokeWidth * 0.8f)
    drawLine(bodyStrokeColor, Offset(x(77f), y(52f)), Offset(x(76f), y(68f)), strokeWidth * 0.8f)

    // 7. 前臂伸肌群 (Forearms Posterior)
    val forearmState = resolve(TargetMuscle.FOREARMS)
    val leftForearm = Path().apply {
        moveTo(x(22f), y(71f))
        cubicTo(x(16f), y(76f), x(15f), y(88f), x(18f), y(102f))
        lineTo(x(23f), y(101f))
        cubicTo(x(26f), y(88f), x(27f), y(78f), x(26f), y(71f))
        close()
    }
    val rightForearm = Path().apply {
        moveTo(x(78f), y(71f))
        cubicTo(x(84f), y(76f), x(85f), y(88f), x(82f), y(102f))
        lineTo(x(77f), y(101f))
        cubicTo(x(74f), y(88f), x(73f), y(78f), x(74f), y(71f))
        close()
    }
    renderMusclePath(leftForearm, forearmState, scale, activeColor, pulseAlpha)
    renderMusclePath(rightForearm, forearmState, scale, activeColor, pulseAlpha)

    // 手背轮廓
    val leftHand = Path().apply {
        moveTo(x(18f), y(102f))
        cubicTo(x(14f), y(108f), x(14f), y(116f), x(18f), y(117f))
        cubicTo(x(22f), y(116f), x(23f), y(108f), x(23f), y(101f))
        close()
    }
    val rightHand = Path().apply {
        moveTo(x(82f), y(102f))
        cubicTo(x(86f), y(108f), x(86f), y(116f), x(82f), y(117f))
        cubicTo(x(78f), y(116f), x(77f), y(108f), x(77f), y(101f))
        close()
    }
    drawPath(leftHand, color = bodyBaseFill, style = Fill)
    drawPath(leftHand, color = bodyStrokeColor, style = Stroke(strokeWidth))
    drawPath(rightHand, color = bodyBaseFill, style = Fill)
    drawPath(rightHand, color = bodyStrokeColor, style = Stroke(strokeWidth))

    // 8. 臀大肌与臀中肌 (Gluteus Maximus & Medius)
    val gluteState = resolve(TargetMuscle.GLUTE_MAXIMUS)
    val leftGlute = Path().apply {
        moveTo(x(35f), y(76f))
        lineTo(x(49f), y(76f))
        lineTo(x(49f), y(93f))
        cubicTo(x(44f), y(96f), x(36f), y(94f), x(32f), y(85f))
        lineTo(x(34f), y(78f))
        close()
    }
    val rightGlute = Path().apply {
        moveTo(x(65f), y(76f))
        lineTo(x(51f), y(76f))
        lineTo(x(51f), y(93f))
        cubicTo(x(56f), y(96f), x(64f), y(94f), x(68f), y(85f))
        lineTo(x(66f), y(78f))
        close()
    }
    renderMusclePath(leftGlute, gluteState, scale, activeColor, pulseAlpha)
    renderMusclePath(rightGlute, gluteState, scale, activeColor, pulseAlpha)
    // 臀沟中央线
    drawLine(bodyStrokeColor, Offset(x(50f), y(76f)), Offset(x(50f), y(93f)), strokeWidth)

    // 9. 腘绳肌群 / 大腿后侧 (Hamstrings: 双束解剖外侧股二头肌与内侧半腱肌)
    val hamState = resolve(TargetMuscle.HAMSTRINGS)
    // 左腿外侧束
    val leftHamOuter = Path().apply {
        moveTo(x(33f), y(92f))
        lineTo(x(40f), y(93f))
        cubicTo(x(40f), y(110f), x(39f), y(123f), x(35f), y(126f))
        cubicTo(x(33f), y(118f), x(32f), y(104f), x(33f), y(92f))
        close()
    }
    // 左腿内侧束
    val leftHamInner = Path().apply {
        moveTo(x(40f), y(93f))
        lineTo(x(47f), y(91f))
        cubicTo(x(46f), y(104f), x(45f), y(118f), x(41f), y(126f))
        cubicTo(x(40f), y(123f), x(40f), y(110f), x(40f), y(93f))
        close()
    }
    // 右腿内侧束
    val rightHamInner = Path().apply {
        moveTo(x(60f), y(93f))
        lineTo(x(53f), y(91f))
        cubicTo(x(54f), y(104f), x(55f), y(118f), x(59f), y(126f))
        cubicTo(x(60f), y(123f), x(60f), y(110f), x(60f), y(93f))
        close()
    }
    // 右腿外侧束
    val rightHamOuter = Path().apply {
        moveTo(x(67f), y(92f))
        lineTo(x(60f), y(93f))
        cubicTo(x(60f), y(110f), x(61f), y(123f), x(65f), y(126f))
        cubicTo(x(67f), y(118f), x(68f), y(104f), x(67f), y(92f))
        close()
    }
    renderMusclePath(leftHamOuter, hamState, scale, activeColor, pulseAlpha)
    renderMusclePath(leftHamInner, hamState, scale, activeColor, pulseAlpha)
    renderMusclePath(rightHamInner, hamState, scale, activeColor, pulseAlpha)
    renderMusclePath(rightHamOuter, hamState, scale, activeColor, pulseAlpha)

    // 腘窝水平横线
    drawLine(bodyStrokeColor, Offset(x(34f), y(128f)), Offset(x(44f), y(128f)), strokeWidth * 0.9f)
    drawLine(bodyStrokeColor, Offset(x(56f), y(128f)), Offset(x(66f), y(128f)), strokeWidth * 0.9f)

    // 10. 小腿腓肠肌与比目鱼肌/跟腱 (Calves: 双头爱心状外凸与纵向跟腱)
    val calfState = resolve(TargetMuscle.CALVES)
    val leftGastrocnemius = Path().apply {
        moveTo(x(35f), y(134f))
        cubicTo(x(31f), y(142f), x(32f), y(152f), x(36f), y(155f))
        lineTo(x(41f), y(155f))
        cubicTo(x(44f), y(150f), x(44f), y(142f), x(41f), y(134f))
        close()
    }
    val leftAchilles = Path().apply {
        moveTo(x(37f), y(155f))
        lineTo(x(40.5f), y(155f))
        lineTo(x(40f), y(168f))
        lineTo(x(38f), y(168f))
        close()
    }
    val rightGastrocnemius = Path().apply {
        moveTo(x(65f), y(134f))
        cubicTo(x(69f), y(142f), x(68f), y(152f), x(64f), y(155f))
        lineTo(x(59f), y(155f))
        cubicTo(x(56f), y(150f), x(56f), y(142f), x(59f), y(134f))
        close()
    }
    val rightAchilles = Path().apply {
        moveTo(x(63f), y(155f))
        lineTo(x(59.5f), y(155f))
        lineTo(x(60f), y(168f))
        lineTo(x(62f), y(168f))
        close()
    }
    renderMusclePath(leftGastrocnemius, calfState, scale, activeColor, pulseAlpha)
    renderMusclePath(leftAchilles, calfState, scale, activeColor, pulseAlpha)
    renderMusclePath(rightGastrocnemius, calfState, scale, activeColor, pulseAlpha)
    renderMusclePath(rightAchilles, calfState, scale, activeColor, pulseAlpha)

    // 11. 双脚后跟与足跟轮廓 (Feet Back)
    val leftFoot = Path().apply {
        moveTo(x(38f), y(168f))
        lineTo(x(33f), y(174f))
        lineTo(x(34f), y(178f))
        lineTo(x(43f), y(178f))
        lineTo(x(41f), y(172f))
        lineTo(x(40f), y(168f))
        close()
    }
    val rightFoot = Path().apply {
        moveTo(x(62f), y(168f))
        lineTo(x(67f), y(174f))
        lineTo(x(66f), y(178f))
        lineTo(x(57f), y(178f))
        lineTo(x(59f), y(172f))
        lineTo(x(60f), y(168f))
        close()
    }
    drawPath(leftFoot, color = bodyBaseFill, style = Fill)
    drawPath(leftFoot, color = bodyStrokeColor, style = Stroke(strokeWidth))
    drawPath(rightFoot, color = bodyBaseFill, style = Fill)
    drawPath(rightFoot, color = bodyStrokeColor, style = Stroke(strokeWidth))
}
