/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.ui.sections

import android.widget.Toast
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.automirrored.rounded.AlignHorizontalLeft
import androidx.compose.material.icons.automirrored.rounded.AlignHorizontalRight
import androidx.compose.material.icons.rounded.CenterFocusStrong
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.FitScreen
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Smartphone
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.agupta07505.smartisland.R
import com.agupta07505.smartisland.data.SmartIslandSettings
import com.agupta07505.smartisland.data.SmartIslandSettingsRepository
import com.agupta07505.smartisland.ui.SliderSettingItem
import com.agupta07505.smartisland.util.CameraCutoutDetector
import kotlinx.coroutines.launch
import kotlin.math.abs

@Composable
fun PositionsSection(
    settings: SmartIslandSettings,
    repository: SmartIslandSettingsRepository
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val windowInfo = LocalWindowInfo.current
    val density = LocalDensity.current

    var localWidth by remember(settings.width) { mutableFloatStateOf(settings.width) }
    var localHeight by remember(settings.height) { mutableFloatStateOf(settings.height) }
    var localXOffset by remember(settings.xOffset) { mutableFloatStateOf(settings.xOffset) }
    var localYOffset by remember(settings.yOffset) { mutableFloatStateOf(settings.yOffset) }
    var localCornerRadius by remember(settings.cornerRadius) { mutableFloatStateOf(settings.cornerRadius) }
    var localOpacity by remember(settings.opacity) { mutableFloatStateOf(settings.opacity) }

    // Dynamically calculate responsive notch coordinates for the current device screen
    val screenWidthDp = with(density) { windowInfo.containerSize.width.toDp().value }
    val calculatedLeftX = (-(screenWidthDp / 2f - 40f)).coerceIn(
        SmartIslandSettings.MIN_X_OFFSET,
        -30f
    )
    val calculatedRightX = ((screenWidthDp / 2f - 40f)).coerceIn(
        30f,
        SmartIslandSettings.MAX_X_OFFSET
    )

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Card 1: Quick Responsive Layout Presets
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = stringResource(R.string.presets_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = stringResource(R.string.presets_desc, screenWidthDp.toInt()),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                // Auto Hardware Cutout Detection Button
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .clickable {
                            val detected = CameraCutoutDetector.detect(context)
                            localWidth = detected.widthDp
                            localHeight = detected.heightDp
                            localXOffset = detected.xOffsetDp
                            localYOffset = detected.yOffsetDp
                            scope.launch {
                                repository.setPosition(
                                    width = detected.widthDp,
                                    height = detected.heightDp,
                                    xOffset = detected.xOffsetDp,
                                    yOffset = detected.yOffsetDp
                                )
                            }
                            if (detected.hasHardwareCutout) {
                                Toast.makeText(
                                    context,
                                    "✅ 成功对齐物理挖孔：X偏移 ${detected.xOffsetDp.toInt()}dp, 宽度 ${detected.widthDp.toInt()}dp",
                                    Toast.LENGTH_SHORT
                                ).show()
                            } else {
                                Toast.makeText(
                                    context,
                                    "已应用标准居中适配，您可继续通过下方滑块微调",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.CenterFocusStrong,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "🔍 自动检测硬件挖孔位置",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "读取屏幕 DisplayCutout 硬件参数，一键对齐前摄",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Presets 2x2 Responsive Grid
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    // Row 1: Center Hole & Wide Island
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        val isCenterSelected = abs(settings.xOffset) < 5f && abs(settings.width - 112f) < 8f
                        PresetCardItem(
                            title = stringResource(R.string.preset_center_hole),
                            subtitle = stringResource(R.string.preset_center_hole_desc),
                            icon = Icons.Rounded.CenterFocusStrong,
                            isSelected = isCenterSelected,
                            onClick = {
                                localWidth = 112f
                                localHeight = 34f
                                localXOffset = 0f
                                localYOffset = 10f
                                localCornerRadius = 20f
                                scope.launch {
                                    repository.setPosition(
                                        width = 112f,
                                        height = 34f,
                                        xOffset = 0f,
                                        yOffset = 10f
                                    )
                                    repository.setCornerRadius(20f)
                                }
                                Toast.makeText(context, context.getString(R.string.toast_applied_center_preset), Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.weight(1f)
                        )

                        val isWideSelected = abs(settings.xOffset) < 5f && abs(settings.width - 150f) < 8f
                        PresetCardItem(
                            title = stringResource(R.string.preset_wide_island),
                            subtitle = stringResource(R.string.preset_wide_island_desc),
                            icon = Icons.Rounded.FitScreen,
                            isSelected = isWideSelected,
                            onClick = {
                                localWidth = 150f
                                localHeight = 38f
                                localXOffset = 0f
                                localYOffset = 12f
                                localCornerRadius = 22f
                                scope.launch {
                                    repository.setPosition(
                                        width = 150f,
                                        height = 38f,
                                        xOffset = 0f,
                                        yOffset = 12f
                                    )
                                    repository.setCornerRadius(22f)
                                }
                                Toast.makeText(context, context.getString(R.string.toast_applied_wide_preset), Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Row 2: Left Corner & Right Corner
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        val isLeftSelected = settings.xOffset < -30f
                        PresetCardItem(
                            title = stringResource(R.string.preset_left_corner),
                            subtitle = stringResource(R.string.preset_left_corner_desc),
                            icon = Icons.AutoMirrored.Rounded.AlignHorizontalLeft,
                            isSelected = isLeftSelected,
                            onClick = {
                                localWidth = 105f
                                localHeight = 34f
                                localXOffset = calculatedLeftX
                                localYOffset = 10f
                                localCornerRadius = 20f
                                scope.launch {
                                    repository.setPosition(
                                        width = 105f,
                                        height = 34f,
                                        xOffset = calculatedLeftX,
                                        yOffset = 10f
                                    )
                                    repository.setCornerRadius(20f)
                                }
                                Toast.makeText(context, context.getString(R.string.toast_applied_left_preset, calculatedLeftX.toInt()), Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.weight(1f)
                        )

                        val isRightSelected = settings.xOffset > 30f
                        PresetCardItem(
                            title = stringResource(R.string.preset_right_corner),
                            subtitle = stringResource(R.string.preset_right_corner_desc),
                            icon = Icons.AutoMirrored.Rounded.AlignHorizontalRight,
                            isSelected = isRightSelected,
                            onClick = {
                                localWidth = 105f
                                localHeight = 34f
                                localXOffset = calculatedRightX
                                localYOffset = 10f
                                localCornerRadius = 20f
                                scope.launch {
                                    repository.setPosition(
                                        width = 105f,
                                        height = 34f,
                                        xOffset = calculatedRightX,
                                        yOffset = 10f
                                    )
                                    repository.setCornerRadius(20f)
                                }
                                Toast.makeText(context, context.getString(R.string.toast_applied_right_preset, calculatedRightX.toInt()), Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Row 3: Compact Pill
                    val isCompactSelected = abs(settings.xOffset) < 5f && abs(settings.width - 92f) < 6f
                    PresetCardItem(
                        title = stringResource(R.string.preset_compact_pill),
                        subtitle = stringResource(R.string.preset_compact_pill_desc),
                        icon = Icons.Rounded.Smartphone,
                        isSelected = isCompactSelected,
                        onClick = {
                            localWidth = 92f
                            localHeight = 30f
                            localXOffset = 0f
                            localYOffset = 8f
                            localCornerRadius = 18f
                            scope.launch {
                                repository.setPosition(
                                    width = 92f,
                                    height = 30f,
                                    xOffset = 0f,
                                    yOffset = 8f
                                )
                                repository.setCornerRadius(18f)
                            }
                            Toast.makeText(context, context.getString(R.string.toast_applied_compact_preset), Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        // Card 2: Precision Dimensions & Offsets
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(Modifier.padding(20.dp)) {
                Text(
                    text = stringResource(R.string.precision_tuning_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = stringResource(R.string.precision_tuning_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp, bottom = 12.dp)
                )

                SliderSettingItem(
                    label = stringResource(R.string.slider_island_width),
                    value = localWidth,
                    range = SmartIslandSettings.MIN_WIDTH..SmartIslandSettings.MAX_WIDTH,
                    onValueChange = { localWidth = it },
                    onValueChangeFinished = { scope.launch { repository.setWidth(localWidth) } }
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))

                SliderSettingItem(
                    label = stringResource(R.string.slider_island_height),
                    value = localHeight,
                    range = SmartIslandSettings.MIN_HEIGHT..SmartIslandSettings.MAX_HEIGHT,
                    onValueChange = { localHeight = it },
                    onValueChangeFinished = { scope.launch { repository.setHeight(localHeight) } }
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))

                SliderSettingItem(
                    label = stringResource(R.string.slider_horizontal_offset),
                    value = localXOffset,
                    range = SmartIslandSettings.MIN_X_OFFSET..SmartIslandSettings.MAX_X_OFFSET,
                    onValueChange = { localXOffset = it },
                    onValueChangeFinished = { scope.launch { repository.setXOffset(localXOffset) } }
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))

                SliderSettingItem(
                    label = stringResource(R.string.slider_vertical_offset),
                    value = localYOffset,
                    range = SmartIslandSettings.MIN_Y_OFFSET..SmartIslandSettings.MAX_Y_OFFSET,
                    onValueChange = { localYOffset = it },
                    onValueChangeFinished = { scope.launch { repository.setYOffset(localYOffset) } }
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))

                SliderSettingItem(
                    label = stringResource(R.string.slider_corner_radius),
                    value = localCornerRadius,
                    range = SmartIslandSettings.MIN_CORNER_RADIUS..SmartIslandSettings.MAX_CORNER_RADIUS,
                    onValueChange = { localCornerRadius = it },
                    onValueChangeFinished = { scope.launch { repository.setCornerRadius(localCornerRadius) } }
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SliderSettingItem(
                        label = stringResource(R.string.slider_island_opacity),
                        value = (localOpacity * 100f),
                        range = (SmartIslandSettings.MIN_OPACITY * 100f)..(SmartIslandSettings.MAX_OPACITY * 100f),
                        suffix = "%",
                        step = 5f,
                        onValueChange = { localOpacity = (it / 100f).coerceIn(SmartIslandSettings.MIN_OPACITY, SmartIslandSettings.MAX_OPACITY) },
                        onValueChangeFinished = { scope.launch { repository.setOpacity(localOpacity) } }
                    )

                    // Quick Opacity Preset Chips
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            1.0f to "100%",
                            0.85f to "85%",
                            0.70f to "70%",
                            0.50f to "50%"
                        ).forEach { (targetVal, label) ->
                            val isSelected = abs(localOpacity - targetVal) < 0.04f
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                border = BorderStroke(1.dp, if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable {
                                        localOpacity = targetVal
                                        scope.launch { repository.setOpacity(targetVal) }
                                    }
                            ) {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier
                                        .padding(vertical = 6.dp)
                                        .fillMaxWidth(),
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        }
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.toggle_drop_shadow_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = stringResource(R.string.toggle_drop_shadow_desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Switch(
                        checked = settings.enableShadow,
                        onCheckedChange = { checked ->
                            scope.launch { repository.setEnableShadow(checked) }
                        }
                    )
                }

                Spacer(Modifier.height(14.dp))
                Button(
                    onClick = {
                        scope.launch {
                            repository.setPosition(
                                width = localWidth,
                                height = localHeight,
                                xOffset = localXOffset,
                                yOffset = localYOffset
                            )
                            repository.setCornerRadius(localCornerRadius)
                            repository.setOpacity(localOpacity)
                        }
                        Toast.makeText(
                            context,
                            "✅ 挖孔布局已保存！已永久生效（关闭后台、关机重启依然有效）",
                            Toast.LENGTH_LONG
                        ).show()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(Icons.Rounded.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("💾 保存并锁定当前布局设置", fontWeight = FontWeight.Bold)
                }

                Spacer(Modifier.height(10.dp))
                OutlinedButton(
                    onClick = {
                        scope.launch { repository.resetPosition() }
                        localWidth = SmartIslandSettings.Default.width
                        localHeight = SmartIslandSettings.Default.height
                        localXOffset = SmartIslandSettings.Default.xOffset
                        localYOffset = SmartIslandSettings.Default.yOffset
                        localCornerRadius = SmartIslandSettings.Default.cornerRadius
                        localOpacity = SmartIslandSettings.Default.opacity
                        Toast.makeText(context, context.getString(R.string.toast_reset_position), Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.btn_reset_position), fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun PresetCardItem(
    title: String,
    subtitle: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val borderColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
        label = "presetBorder"
    )
    val bgColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        label = "presetBg"
    )
    val iconTint by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        label = "presetIconTint"
    )

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(bgColor)
            .border(if (isSelected) 1.5.dp else 1.dp, borderColor, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(14.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(iconTint.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(18.dp)
                    )
                }

                if (isSelected) {
                    Icon(
                        imageVector = Icons.Rounded.CheckCircle,
                        contentDescription = "Active",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 14.sp
                )
            }
        }
    }
}
