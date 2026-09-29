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
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.agupta07505.smartisland.model.WorkoutExercise

/**
 * 训练部位枚举与统一颜色规范
 * 胸 = 粉红 | 腹 = 绿色 | 臀 = 紫色 | 腿 = 珊瑚红 | 背 = 蓝色 | 肩 = 橙色 | 小腿足踝 = 青蓝
 */
enum class MuscleCategory(val displayName: String, val color: Color) {
    CHEST("胸部", Color(0xFFF43F5E)),        // 粉红色 (Rose/Pink)
    ABS("腹部", Color(0xFF10B981)),          // 绿色 (Emerald Green)
    GLUTES("臀部", Color(0xFF8B5CF6)),       // 紫色 (Purple)
    LEGS("腿部", Color(0xFFFB7185)),         // 珊瑚红 (Coral Red)
    BACK("背部", Color(0xFF3B82F6)),         // 蓝色 (Blue)
    SHOULDERS("肩部", Color(0xFFF97316)),    // 橙色 (Orange)
    CALVES("小腿/足踝", Color(0xFF06B6D4)),   // 青蓝色 (Cyan/Teal)
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
    // 1. 胸
    CHEST_MAJOR("胸大肌", MuscleCategory.CHEST, isFront = true, isBack = false),
    UPPER_CHEST("胸大肌上部", MuscleCategory.CHEST, isFront = true, isBack = false),

    // 2. 腹
    ABS_RECTUS("腹直肌/核心", MuscleCategory.ABS, isFront = true, isBack = false),
    LOWER_ABS("下腹/核心", MuscleCategory.ABS, isFront = true, isBack = false),

    // 3. 肩
    LATERAL_DELTOID("三角肌中束", MuscleCategory.SHOULDERS, isFront = true, isBack = true),
    REAR_DELTOID("三角肌后束", MuscleCategory.SHOULDERS, isFront = false, isBack = true),
    DELTOIDS("三角肌", MuscleCategory.SHOULDERS, isFront = true, isBack = true),

    // 4. 背
    LAT_DORSI("背阔肌", MuscleCategory.BACK, isFront = false, isBack = true),
    MID_BACK("中背/肩胛稳定", MuscleCategory.BACK, isFront = false, isBack = true),
    TRAPEZIUS_LOWER("中下斜方肌", MuscleCategory.BACK, isFront = false, isBack = true),

    // 5. 臀
    GLUTE_MAXIMUS("臀大肌", MuscleCategory.GLUTES, isFront = false, isBack = true),
    GLUTE_MEDIUS("臀中肌", MuscleCategory.GLUTES, isFront = false, isBack = true),

    // 6. 腿
    QUADS("股四头肌", MuscleCategory.LEGS, isFront = true, isBack = false),
    HAMSTRINGS("腘绳肌/大腿后侧", MuscleCategory.LEGS, isFront = false, isBack = true),
    ADDUCTORS("大腿内收肌群", MuscleCategory.LEGS, isFront = true, isBack = false),

    // 7. 小腿/足踝
    CALVES_FRONT("小腿前侧", MuscleCategory.CALVES, isFront = true, isBack = false),
    CALVES_BACK("小腿后侧/足踝", MuscleCategory.CALVES, isFront = false, isBack = true)
}

/**
 * 智能肌肉匹配器
 */
object MuscleMatcher {
    fun match(exerciseName: String, categoryName: String, explicitTarget: String? = null): TargetMuscle {
        val target = explicitTarget?.lowercase().orEmpty()
        val name = exerciseName.lowercase()
        val cat = categoryName.lowercase()

        return when {
            // 优先检查 explicitTarget
            target.contains("胸大肌上") || target.contains("上胸") -> TargetMuscle.UPPER_CHEST
            target.contains("胸") -> TargetMuscle.CHEST_MAJOR
            target.contains("下腹") -> TargetMuscle.LOWER_ABS
            target.contains("腹") || target.contains("核心") -> TargetMuscle.ABS_RECTUS
            target.contains("后束") -> TargetMuscle.REAR_DELTOID
            target.contains("中束") -> TargetMuscle.LATERAL_DELTOID
            target.contains("三角") || target.contains("肩") -> TargetMuscle.DELTOIDS
            target.contains("中背") || target.contains("菱形") || target.contains("肩胛") -> TargetMuscle.MID_BACK
            target.contains("背阔") -> TargetMuscle.LAT_DORSI
            target.contains("臀中") -> TargetMuscle.GLUTE_MEDIUS
            target.contains("臀大") || target.contains("臀推") -> TargetMuscle.GLUTE_MAXIMUS
            target.contains("腘绳") || target.contains("腿弯举") || target.contains("后侧") -> TargetMuscle.HAMSTRINGS
            target.contains("内收") -> TargetMuscle.ADDUCTORS
            target.contains("四头") || target.contains("倒蹬") || target.contains("深蹲") -> TargetMuscle.QUADS
            target.contains("小腿") || target.contains("足踝") || target.contains("拉伸小腿") -> TargetMuscle.CALVES_BACK

            // 动作名称匹配
            name.contains("上斜") && (name.contains("推胸") || name.contains("卧推")) -> TargetMuscle.UPPER_CHEST
            name.contains("推胸") || name.contains("夹胸") || name.contains("卧推") -> TargetMuscle.CHEST_MAJOR
            name.contains("平板支撑") || name.contains("卷腹") -> TargetMuscle.ABS_RECTUS
            name.contains("下腹") || name.contains("屈腿") || name.contains("举腿") -> TargetMuscle.LOWER_ABS
            name.contains("反向飞鸟") || (name.contains("面拉") && (cat.contains("肩") || cat.contains("背"))) -> TargetMuscle.REAR_DELTOID
            name.contains("侧平举") -> TargetMuscle.LATERAL_DELTOID
            name.contains("推举") || name.contains("推肩") -> TargetMuscle.DELTOIDS
            name.contains("划船") -> TargetMuscle.MID_BACK
            name.contains("下拉") || name.contains("引体向上") -> TargetMuscle.LAT_DORSI
            name.contains("髋外展") -> TargetMuscle.GLUTE_MEDIUS
            name.contains("臀推") || name.contains("臀桥") -> TargetMuscle.GLUTE_MAXIMUS
            name.contains("腿弯举") || name.contains("直腿硬拉") -> TargetMuscle.HAMSTRINGS
            name.contains("髋内收") -> TargetMuscle.ADDUCTORS
            name.contains("倒蹬") || name.contains("腿屈伸") || name.contains("深蹲") -> TargetMuscle.QUADS
            name.contains("小腿") || name.contains("提踵") -> TargetMuscle.CALVES_BACK

            // 类别兜底
            cat.contains("胸") -> TargetMuscle.CHEST_MAJOR
            cat.contains("腹") -> TargetMuscle.ABS_RECTUS
            cat.contains("肩") -> TargetMuscle.DELTOIDS
            cat.contains("背") -> TargetMuscle.LAT_DORSI
            cat.contains("臀") -> TargetMuscle.GLUTE_MAXIMUS
            cat.contains("腿") -> TargetMuscle.QUADS
            else -> TargetMuscle.CHEST_MAJOR
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
            cat.contains("小腿") || cat.contains("足踝") -> MuscleCategory.CALVES.color
            else -> Color(0xFF64748B)
        }
    }
}

/**
 * 现代高精女性人体正反双面解剖肌肉点亮组件 (Apple Health 极简设计)
 */
@Composable
fun FemaleMuscleAnatomyDualView(
    activeMuscle: TargetMuscle?,
    activeCategory: String,
    modifier: Modifier = Modifier,
    isDarkTheme: Boolean = false,
    onMuscleSelected: ((TargetMuscle) -> Unit)? = null
) {
    val infiniteTransition = rememberInfiniteTransition(label = "muscleGlow")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.55f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 850, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    val baseBodyColor = if (isDarkTheme) Color(0xFF334155).copy(alpha = 0.45f) else Color(0xFFE2E8F0)
    val bodyStrokeColor = if (isDarkTheme) Color(0xFF64748B).copy(alpha = 0.5f) else Color(0xFFCBD5E1)
    val facialColor = if (isDarkTheme) Color(0xFF94A3B8) else Color(0xFF94A3B8)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 1. 左侧：女性人体 正面 (带极简面部特征识别)
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.weight(1f)
        ) {
            Canvas(
                modifier = Modifier
                    .size(width = 86.dp, height = 135.dp)
            ) {
                val cx = size.width / 2f
                val h = size.height

                // 绘制正面女性轮廓 (含人脸)
                drawFemaleSilhouette(
                    cx = cx,
                    h = h,
                    isFront = true,
                    baseColor = baseBodyColor,
                    strokeColor = bodyStrokeColor,
                    facialColor = facialColor
                )

                // 绘制正面目标肌肉
                drawFrontMuscles(
                    cx = cx,
                    h = h,
                    activeMuscle = activeMuscle,
                    activeCategory = activeCategory,
                    pulseAlpha = pulseAlpha
                )
            }
        }

        // 分隔微细线
        Box(
            modifier = Modifier
                .width(1.dp)
                .height(90.dp)
                .background(if (isDarkTheme) Color(0xFF334155).copy(alpha = 0.3f) else Color(0xFFE2E8F0))
        )

        // 2. 右侧：女性人体 背面 (无五官，显示发髻/脊柱/背臀轮廓)
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.weight(1f)
        ) {
            Canvas(
                modifier = Modifier
                    .size(width = 86.dp, height = 135.dp)
            ) {
                val cx = size.width / 2f
                val h = size.height

                // 绘制背面女性轮廓 (后脑发型/背部)
                drawFemaleSilhouette(
                    cx = cx,
                    h = h,
                    isFront = false,
                    baseColor = baseBodyColor,
                    strokeColor = bodyStrokeColor,
                    facialColor = facialColor
                )

                // 绘制背面目标肌肉
                drawBackMuscles(
                    cx = cx,
                    h = h,
                    activeMuscle = activeMuscle,
                    activeCategory = activeCategory,
                    pulseAlpha = pulseAlpha
                )
            }
        }
    }
}

/**
 * 绘制极简现代女性人体轮廓线
 */
private fun DrawScope.drawFemaleSilhouette(
    cx: Float,
    h: Float,
    isFront: Boolean,
    baseColor: Color,
    strokeColor: Color,
    facialColor: Color
) {
    val scale = h / 140f
    fun dp2px(v: Float) = v * scale

    // 头部
    val headRadius = dp2px(7.5f)
    val headCenter = Offset(cx, dp2px(11f))
    drawCircle(color = baseColor, radius = headRadius, center = headCenter)
    drawCircle(color = strokeColor, radius = headRadius, center = headCenter, style = Stroke(dp2px(1.1f)))

    if (isFront) {
        // 正面：绘制极简清爽女性五官线（一眼辨识正面）
        // 双眼/眉线
        drawLine(
            color = facialColor,
            start = Offset(cx - dp2px(3.8f), dp2px(10.5f)),
            end = Offset(cx - dp2px(1.5f), dp2px(10.5f)),
            strokeWidth = dp2px(1.1f)
        )
        drawLine(
            color = facialColor,
            start = Offset(cx + dp2px(1.5f), dp2px(10.5f)),
            end = Offset(cx + dp2px(3.8f), dp2px(10.5f)),
            strokeWidth = dp2px(1.1f)
        )
        // 极简唇线
        drawLine(
            color = facialColor,
            start = Offset(cx - dp2px(1.8f), dp2px(14.5f)),
            end = Offset(cx + dp2px(1.8f), dp2px(14.5f)),
            strokeWidth = dp2px(1.1f)
        )
    } else {
        // 背面：绘制优雅束发/后脑勺发线与颈后发丝（一眼辨识背面）
        val hairBunRadius = dp2px(3.2f)
        drawCircle(color = strokeColor.copy(alpha = 0.7f), radius = hairBunRadius, center = Offset(cx, dp2px(5.5f)))
        drawLine(
            color = strokeColor.copy(alpha = 0.5f),
            start = Offset(cx, dp2px(11f)),
            end = Offset(cx, dp2px(18f)),
            strokeWidth = dp2px(1f)
        )
    }

    // 女性优雅流线身体外轮廓 (纤细腰肢 + 饱满臀腿比例)
    val path = Path().apply {
        // 颈左
        moveTo(cx - dp2px(3.2f), dp2px(18f))
        // 锁骨/肩峰左
        cubicTo(cx - dp2px(8f), dp2px(20f), cx - dp2px(16f), dp2px(22f), cx - dp2px(20f), dp2px(25f))
        // 左大臂
        cubicTo(cx - dp2px(23f), dp2px(36f), cx - dp2px(24f), dp2px(48f), cx - dp2px(25f), dp2px(58f))
        // 左手折返
        lineTo(cx - dp2px(22f), dp2px(58f))
        // 左小臂内侧
        cubicTo(cx - dp2px(20f), dp2px(48f), cx - dp2px(18f), dp2px(38f), cx - dp2px(16f), dp2px(32f))
        // 细腰
        cubicTo(cx - dp2px(13f), dp2px(42f), cx - dp2px(11f), dp2px(48f), cx - dp2px(11.5f), dp2px(56f))
        // 胯骨与大腿外侧曲线
        cubicTo(cx - dp2px(16.5f), dp2px(64f), cx - dp2px(16f), dp2px(78f), cx - dp2px(14.5f), dp2px(94f))
        // 膝盖左
        lineTo(cx - dp2px(12.5f), dp2px(104f))
        // 左小腿外侧弧度
        cubicTo(cx - dp2px(13.5f), dp2px(114f), cx - dp2px(11.5f), dp2px(126f), cx - dp2px(9.5f), dp2px(136f))
        // 左脚踝
        lineTo(cx - dp2px(6.5f), dp2px(136f))
        // 左小腿内侧
        cubicTo(cx - dp2px(7f), dp2px(124f), cx - dp2px(8f), dp2px(112f), cx - dp2px(8f), dp2px(104f))
        // 膝盖内侧与大腿内侧
        cubicTo(cx - dp2px(8f), dp2px(92f), cx - dp2px(6f), dp2px(76f), cx - dp2px(1.5f), dp2px(67f))
        // 裆部中点
        lineTo(cx, dp2px(66f))
        // 右大腿内侧
        lineTo(cx + dp2px(1.5f), dp2px(67f))
        cubicTo(cx + dp2px(6f), dp2px(76f), cx + dp2px(8f), dp2px(92f), cx + dp2px(8f), dp2px(104f))
        // 右小腿内侧
        cubicTo(cx + dp2px(8f), dp2px(112f), cx + dp2px(7f), dp2px(124f), cx + dp2px(6.5f), dp2px(136f))
        // 右脚踝
        lineTo(cx + dp2px(9.5f), dp2px(136f))
        // 右小腿外侧
        cubicTo(cx + dp2px(11.5f), dp2px(126f), cx + dp2px(13.5f), dp2px(114f), cx + dp2px(12.5f), dp2px(104f))
        // 膝盖右
        lineTo(cx + dp2px(14.5f), dp2px(94f))
        // 右大腿外侧
        cubicTo(cx + dp2px(16f), dp2px(78f), cx + dp2px(16.5f), dp2px(64f), cx + dp2px(11.5f), dp2px(56f))
        // 细腰右
        cubicTo(cx + dp2px(11f), dp2px(48f), cx + dp2px(13f), dp2px(42f), cx + dp2px(16f), dp2px(32f))
        // 右小臂内侧
        cubicTo(cx + dp2px(18f), dp2px(38f), cx + dp2px(20f), dp2px(48f), cx + dp2px(22f), dp2px(58f))
        // 右手
        lineTo(cx + dp2px(25f), dp2px(58f))
        // 右大臂
        cubicTo(cx + dp2px(24f), dp2px(48f), cx + dp2px(23f), dp2px(36f), cx + dp2px(20f), dp2px(25f))
        // 肩峰右
        cubicTo(cx + dp2px(16f), dp2px(22f), cx + dp2px(8f), dp2px(20f), cx + dp2px(3.2f), dp2px(18f))
        close()
    }

    drawPath(path = path, color = baseColor, style = Fill)
    drawPath(path = path, color = strokeColor, style = Stroke(dp2px(1.1f)))

    if (!isFront) {
        // 背面：画脊柱微线
        drawLine(
            color = strokeColor.copy(alpha = 0.35f),
            start = Offset(cx, dp2px(24f)),
            end = Offset(cx, dp2px(56f)),
            strokeWidth = dp2px(1f)
        )
    }
}

/**
 * 绘制正面目标肌肉 (胸大肌、腹直肌、下腹、三角肌前中束、股四头、大腿内收、小腿前)
 */
private fun DrawScope.drawFrontMuscles(
    cx: Float,
    h: Float,
    activeMuscle: TargetMuscle?,
    activeCategory: String,
    pulseAlpha: Float
) {
    val scale = h / 140f
    fun dp2px(v: Float) = v * scale

    // 1. 胸大肌 (粉红色)
    val isChestActive = activeMuscle == TargetMuscle.CHEST_MAJOR || activeMuscle == TargetMuscle.UPPER_CHEST || activeCategory.contains("胸")
    if (isChestActive) {
        val color = MuscleCategory.CHEST.color
        val fillAlpha = if (activeMuscle == TargetMuscle.CHEST_MAJOR || activeMuscle == TargetMuscle.UPPER_CHEST) pulseAlpha else 0.45f
        val leftBreast = Path().apply {
            moveTo(cx - dp2px(1.8f), dp2px(24.5f))
            cubicTo(cx - dp2px(8f), dp2px(24f), cx - dp2px(14f), dp2px(27f), cx - dp2px(13.5f), dp2px(33f))
            cubicTo(cx - dp2px(13f), dp2px(38f), cx - dp2px(6f), dp2px(38.5f), cx - dp2px(1.8f), dp2px(35f))
            close()
        }
        val rightBreast = Path().apply {
            moveTo(cx + dp2px(1.8f), dp2px(24.5f))
            cubicTo(cx + dp2px(8f), dp2px(24f), cx + dp2px(14f), dp2px(27f), cx + dp2px(13.5f), dp2px(33f))
            cubicTo(cx + dp2px(13f), dp2px(38f), cx + dp2px(6f), dp2px(38.5f), cx + dp2px(1.8f), dp2px(35f))
            close()
        }
        drawPath(leftBreast, color.copy(alpha = fillAlpha))
        drawPath(leftBreast, color, style = Stroke(dp2px(1.2f)))
        drawPath(rightBreast, color.copy(alpha = fillAlpha))
        drawPath(rightBreast, color, style = Stroke(dp2px(1.2f)))
    }

    // 2. 腹直肌 / 核心 (绿色)
    val isAbsActive = activeMuscle == TargetMuscle.ABS_RECTUS || activeCategory.contains("腹")
    if (isAbsActive) {
        val color = MuscleCategory.ABS.color
        val fillAlpha = if (activeMuscle == TargetMuscle.ABS_RECTUS) pulseAlpha else 0.45f
        val abWidth = dp2px(4f)
        val abHeight = dp2px(3.2f)
        val gap = dp2px(1.2f)
        val startY = dp2px(36.5f)

        for (row in 0..2) {
            val y = startY + row * (abHeight + gap)
            drawRoundRect(
                color = color.copy(alpha = fillAlpha),
                topLeft = Offset(cx - abWidth - dp2px(0.8f), y),
                size = Size(abWidth, abHeight),
                cornerRadius = CornerRadius(dp2px(1.2f))
            )
            drawRoundRect(
                color = color,
                topLeft = Offset(cx - abWidth - dp2px(0.8f), y),
                size = Size(abWidth, abHeight),
                cornerRadius = CornerRadius(dp2px(1.2f)),
                style = Stroke(dp2px(1f))
            )
            drawRoundRect(
                color = color.copy(alpha = fillAlpha),
                topLeft = Offset(cx + dp2px(0.8f), y),
                size = Size(abWidth, abHeight),
                cornerRadius = CornerRadius(dp2px(1.2f))
            )
            drawRoundRect(
                color = color,
                topLeft = Offset(cx + dp2px(0.8f), y),
                size = Size(abWidth, abHeight),
                cornerRadius = CornerRadius(dp2px(1.2f)),
                style = Stroke(dp2px(1f))
            )
        }
    }

    // 3. 下腹 / 核心 (绿色)
    val isLowerAbsActive = activeMuscle == TargetMuscle.LOWER_ABS || activeCategory.contains("腹")
    if (isLowerAbsActive) {
        val color = MuscleCategory.ABS.color
        val fillAlpha = if (activeMuscle == TargetMuscle.LOWER_ABS) pulseAlpha else 0.45f
        val lowerAbs = Path().apply {
            moveTo(cx - dp2px(5f), dp2px(49f))
            lineTo(cx + dp2px(5f), dp2px(49f))
            lineTo(cx + dp2px(2.5f), dp2px(55f))
            lineTo(cx - dp2px(2.5f), dp2px(55f))
            close()
        }
        drawPath(lowerAbs, color.copy(alpha = fillAlpha))
        drawPath(lowerAbs, color, style = Stroke(dp2px(1.1f)))
    }

    // 4. 三角肌前中束 (橙色)
    val isShoulderActive = activeMuscle == TargetMuscle.DELTOIDS || activeMuscle == TargetMuscle.LATERAL_DELTOID || activeCategory.contains("肩")
    if (isShoulderActive) {
        val color = MuscleCategory.SHOULDERS.color
        val fillAlpha = if (activeMuscle == TargetMuscle.DELTOIDS || activeMuscle == TargetMuscle.LATERAL_DELTOID) pulseAlpha else 0.45f
        val leftDelt = Path().apply {
            moveTo(cx - dp2px(14f), dp2px(23.5f))
            lineTo(cx - dp2px(19.5f), dp2px(25f))
            cubicTo(cx - dp2px(22f), dp2px(30f), cx - dp2px(21f), dp2px(36f), cx - dp2px(17.5f), dp2px(37f))
            lineTo(cx - dp2px(15f), dp2px(30f))
            close()
        }
        val rightDelt = Path().apply {
            moveTo(cx + dp2px(14f), dp2px(23.5f))
            lineTo(cx + dp2px(19.5f), dp2px(25f))
            cubicTo(cx + dp2px(22f), dp2px(30f), cx + dp2px(21f), dp2px(36f), cx + dp2px(17.5f), dp2px(37f))
            lineTo(cx + dp2px(15f), dp2px(30f))
            close()
        }
        drawPath(leftDelt, color.copy(alpha = fillAlpha))
        drawPath(leftDelt, color, style = Stroke(dp2px(1.1f)))
        drawPath(rightDelt, color.copy(alpha = fillAlpha))
        drawPath(rightDelt, color, style = Stroke(dp2px(1.1f)))
    }

    // 5. 股四头肌 (珊瑚红)
    val isQuadsActive = activeMuscle == TargetMuscle.QUADS || (activeCategory.contains("腿") && activeMuscle == null)
    if (isQuadsActive) {
        val color = MuscleCategory.LEGS.color
        val fillAlpha = if (activeMuscle == TargetMuscle.QUADS) pulseAlpha else 0.45f
        val leftQuad = Path().apply {
            moveTo(cx - dp2px(13.5f), dp2px(64f))
            cubicTo(cx - dp2px(15.5f), dp2px(75f), cx - dp2px(14.5f), dp2px(88f), cx - dp2px(11f), dp2px(98f))
            lineTo(cx - dp2px(6.5f), dp2px(98f))
            cubicTo(cx - dp2px(9f), dp2px(85f), cx - dp2px(9f), dp2px(72f), cx - dp2px(5f), dp2px(65f))
            close()
        }
        val rightQuad = Path().apply {
            moveTo(cx + dp2px(13.5f), dp2px(64f))
            cubicTo(cx + dp2px(15.5f), dp2px(75f), cx + dp2px(14.5f), dp2px(88f), cx + dp2px(11f), dp2px(98f))
            lineTo(cx + dp2px(6.5f), dp2px(98f))
            cubicTo(cx + dp2px(9f), dp2px(85f), cx + dp2px(9f), dp2px(72f), cx + dp2px(5f), dp2px(65f))
            close()
        }
        drawPath(leftQuad, color.copy(alpha = fillAlpha))
        drawPath(leftQuad, color, style = Stroke(dp2px(1.1f)))
        drawPath(rightQuad, color.copy(alpha = fillAlpha))
        drawPath(rightQuad, color, style = Stroke(dp2px(1.1f)))
    }

    // 6. 大腿内收肌群 (珊瑚红)
    val isAdductorActive = activeMuscle == TargetMuscle.ADDUCTORS || activeCategory.contains("腿")
    if (isAdductorActive) {
        val color = MuscleCategory.LEGS.color
        val fillAlpha = if (activeMuscle == TargetMuscle.ADDUCTORS) pulseAlpha else 0.45f
        val leftAdductor = Path().apply {
            moveTo(cx - dp2px(4.5f), dp2px(66f))
            lineTo(cx - dp2px(1.5f), dp2px(67f))
            cubicTo(cx - dp2px(4f), dp2px(76f), cx - dp2px(5.5f), dp2px(86f), cx - dp2px(5.5f), dp2px(94f))
            lineTo(cx - dp2px(7f), dp2px(94f))
            close()
        }
        val rightAdductor = Path().apply {
            moveTo(cx + dp2px(4.5f), dp2px(66f))
            lineTo(cx + dp2px(1.5f), dp2px(67f))
            cubicTo(cx + dp2px(4f), dp2px(76f), cx + dp2px(5.5f), dp2px(86f), cx + dp2px(5.5f), dp2px(94f))
            lineTo(cx + dp2px(7f), dp2px(94f))
            close()
        }
        drawPath(leftAdductor, color.copy(alpha = fillAlpha))
        drawPath(leftAdductor, color, style = Stroke(dp2px(1.1f)))
        drawPath(rightAdductor, color.copy(alpha = fillAlpha))
        drawPath(rightAdductor, color, style = Stroke(dp2px(1.1f)))
    }

    // 7. 小腿前侧 / 胫骨前肌 (青蓝色)
    val isCalfFrontActive = activeMuscle == TargetMuscle.CALVES_FRONT || activeCategory.contains("小腿") || activeCategory.contains("足踝")
    if (isCalfFrontActive) {
        val color = MuscleCategory.CALVES.color
        val fillAlpha = if (activeMuscle == TargetMuscle.CALVES_FRONT) pulseAlpha else 0.45f
        drawRoundRect(
            color = color.copy(alpha = fillAlpha),
            topLeft = Offset(cx - dp2px(11.5f), dp2px(108f)),
            size = Size(dp2px(3.5f), dp2px(20f)),
            cornerRadius = CornerRadius(dp2px(1.5f))
        )
        drawRoundRect(
            color = color.copy(alpha = fillAlpha),
            topLeft = Offset(cx + dp2px(8f), dp2px(108f)),
            size = Size(dp2px(3.5f), dp2px(20f)),
            cornerRadius = CornerRadius(dp2px(1.5f))
        )
    }
}

/**
 * 绘制背面目标肌肉 (背阔肌、中背肩胛、中下斜方、三角肌后束、臀大肌、臀中肌、腘绳肌、小腿后侧)
 */
private fun DrawScope.drawBackMuscles(
    cx: Float,
    h: Float,
    activeMuscle: TargetMuscle?,
    activeCategory: String,
    pulseAlpha: Float
) {
    val scale = h / 140f
    fun dp2px(v: Float) = v * scale

    // 1. 中背 / 肩胛稳定区域 / 菱形肌 (蓝色)
    val isMidBackActive = activeMuscle == TargetMuscle.MID_BACK || activeMuscle == TargetMuscle.TRAPEZIUS_LOWER || activeCategory.contains("背")
    if (isMidBackActive) {
        val color = MuscleCategory.BACK.color
        val fillAlpha = if (activeMuscle == TargetMuscle.MID_BACK || activeMuscle == TargetMuscle.TRAPEZIUS_LOWER) pulseAlpha else 0.45f
        val midBackPath = Path().apply {
            moveTo(cx, dp2px(22f))
            lineTo(cx - dp2px(9f), dp2px(27f))
            lineTo(cx - dp2px(5f), dp2px(39f))
            lineTo(cx, dp2px(42f))
            lineTo(cx + dp2px(5f), dp2px(39f))
            lineTo(cx + dp2px(9f), dp2px(27f))
            close()
        }
        drawPath(midBackPath, color.copy(alpha = fillAlpha))
        drawPath(midBackPath, color, style = Stroke(dp2px(1.1f)))
    }

    // 2. 背阔肌 (蓝色)
    val isLatActive = activeMuscle == TargetMuscle.LAT_DORSI || activeCategory.contains("背")
    if (isLatActive) {
        val color = MuscleCategory.BACK.color
        val fillAlpha = if (activeMuscle == TargetMuscle.LAT_DORSI) pulseAlpha else 0.45f
        val leftLat = Path().apply {
            moveTo(cx - dp2px(3f), dp2px(32f))
            lineTo(cx - dp2px(15f), dp2px(30f))
            cubicTo(cx - dp2px(14f), dp2px(42f), cx - dp2px(11f), dp2px(48f), cx - dp2px(4f), dp2px(52f))
            close()
        }
        val rightLat = Path().apply {
            moveTo(cx + dp2px(3f), dp2px(32f))
            lineTo(cx + dp2px(15f), dp2px(30f))
            cubicTo(cx + dp2px(14f), dp2px(42f), cx + dp2px(11f), dp2px(48f), cx + dp2px(4f), dp2px(52f))
            close()
        }
        drawPath(leftLat, color.copy(alpha = fillAlpha))
        drawPath(leftLat, color, style = Stroke(dp2px(1.1f)))
        drawPath(rightLat, color.copy(alpha = fillAlpha))
        drawPath(rightLat, color, style = Stroke(dp2px(1.1f)))
    }

    // 3. 三角肌后束 (橙色)
    val isRearDeltActive = activeMuscle == TargetMuscle.REAR_DELTOID || activeCategory.contains("肩")
    if (isRearDeltActive) {
        val color = MuscleCategory.SHOULDERS.color
        val fillAlpha = if (activeMuscle == TargetMuscle.REAR_DELTOID) pulseAlpha else 0.45f
        val leftRearDelt = Path().apply {
            moveTo(cx - dp2px(11f), dp2px(24f))
            lineTo(cx - dp2px(19.5f), dp2px(25f))
            lineTo(cx - dp2px(18.5f), dp2px(33f))
            lineTo(cx - dp2px(12f), dp2px(29f))
            close()
        }
        val rightRearDelt = Path().apply {
            moveTo(cx + dp2px(11f), dp2px(24f))
            lineTo(cx + dp2px(19.5f), dp2px(25f))
            lineTo(cx + dp2px(18.5f), dp2px(33f))
            lineTo(cx + dp2px(12f), dp2px(29f))
            close()
        }
        drawPath(leftRearDelt, color.copy(alpha = fillAlpha))
        drawPath(leftRearDelt, color, style = Stroke(dp2px(1.1f)))
        drawPath(rightRearDelt, color.copy(alpha = fillAlpha))
        drawPath(rightRearDelt, color, style = Stroke(dp2px(1.1f)))
    }

    // 4. 臀中肌 (紫色)
    val isGluteMedActive = activeMuscle == TargetMuscle.GLUTE_MEDIUS || activeCategory.contains("臀")
    if (isGluteMedActive) {
        val color = MuscleCategory.GLUTES.color
        val fillAlpha = if (activeMuscle == TargetMuscle.GLUTE_MEDIUS) pulseAlpha else 0.45f
        val leftGluteMed = Path().apply {
            moveTo(cx - dp2px(5f), dp2px(55f))
            lineTo(cx - dp2px(13.5f), dp2px(56f))
            cubicTo(cx - dp2px(16f), dp2px(62f), cx - dp2px(14f), dp2px(68f), cx - dp2px(10f), dp2px(68f))
            lineTo(cx - dp2px(3f), dp2px(60f))
            close()
        }
        val rightGluteMed = Path().apply {
            moveTo(cx + dp2px(5f), dp2px(55f))
            lineTo(cx + dp2px(13.5f), dp2px(56f))
            cubicTo(cx + dp2px(16f), dp2px(62f), cx + dp2px(14f), dp2px(68f), cx + dp2px(10f), dp2px(68f))
            lineTo(cx + dp2px(3f), dp2px(60f))
            close()
        }
        drawPath(leftGluteMed, color.copy(alpha = fillAlpha))
        drawPath(leftGluteMed, color, style = Stroke(dp2px(1.1f)))
        drawPath(rightGluteMed, color.copy(alpha = fillAlpha))
        drawPath(rightGluteMed, color, style = Stroke(dp2px(1.1f)))
    }

    // 5. 臀大肌 (紫色)
    val isGluteMaxActive = activeMuscle == TargetMuscle.GLUTE_MAXIMUS || (activeCategory.contains("臀") && activeMuscle == null)
    if (isGluteMaxActive) {
        val color = MuscleCategory.GLUTES.color
        val fillAlpha = if (activeMuscle == TargetMuscle.GLUTE_MAXIMUS) pulseAlpha else 0.45f
        val leftGluteMax = Path().apply {
            moveTo(cx - dp2px(1.5f), dp2px(58f))
            lineTo(cx - dp2px(13f), dp2px(64f))
            cubicTo(cx - dp2px(15f), dp2px(72f), cx - dp2px(10f), dp2px(78f), cx - dp2px(2f), dp2px(76f))
            lineTo(cx - dp2px(0.8f), dp2px(68f))
            close()
        }
        val rightGluteMax = Path().apply {
            moveTo(cx + dp2px(1.5f), dp2px(58f))
            lineTo(cx + dp2px(13f), dp2px(64f))
            cubicTo(cx + dp2px(15f), dp2px(72f), cx + dp2px(10f), dp2px(78f), cx + dp2px(2f), dp2px(76f))
            lineTo(cx + dp2px(0.8f), dp2px(68f))
            close()
        }
        drawPath(leftGluteMax, color.copy(alpha = fillAlpha))
        drawPath(leftGluteMax, color, style = Stroke(dp2px(1.1f)))
        drawPath(rightGluteMax, color.copy(alpha = fillAlpha))
        drawPath(rightGluteMax, color, style = Stroke(dp2px(1.1f)))
    }

    // 6. 腘绳肌 / 大腿后侧 (珊瑚红)
    val isHamstringActive = activeMuscle == TargetMuscle.HAMSTRINGS || activeCategory.contains("腿")
    if (isHamstringActive) {
        val color = MuscleCategory.LEGS.color
        val fillAlpha = if (activeMuscle == TargetMuscle.HAMSTRINGS) pulseAlpha else 0.45f
        val leftHam = Path().apply {
            moveTo(cx - dp2px(11f), dp2px(78f))
            lineTo(cx - dp2px(2.5f), dp2px(78f))
            cubicTo(cx - dp2px(4f), dp2px(88f), cx - dp2px(5f), dp2px(96f), cx - dp2px(6.5f), dp2px(99f))
            lineTo(cx - dp2px(10.5f), dp2px(99f))
            close()
        }
        val rightHam = Path().apply {
            moveTo(cx + dp2px(11f), dp2px(78f))
            lineTo(cx + dp2px(2.5f), dp2px(78f))
            cubicTo(cx + dp2px(4f), dp2px(88f), cx + dp2px(5f), dp2px(96f), cx + dp2px(6.5f), dp2px(99f))
            lineTo(cx + dp2px(10.5f), dp2px(99f))
            close()
        }
        drawPath(leftHam, color.copy(alpha = fillAlpha))
        drawPath(leftHam, color, style = Stroke(dp2px(1.1f)))
        drawPath(rightHam, color.copy(alpha = fillAlpha))
        drawPath(rightHam, color, style = Stroke(dp2px(1.1f)))
    }

    // 7. 小腿后侧 / 腓肠肌与足踝 (青蓝色)
    val isCalfBackActive = activeMuscle == TargetMuscle.CALVES_BACK || activeCategory.contains("小腿") || activeCategory.contains("足踝")
    if (isCalfBackActive) {
        val color = MuscleCategory.CALVES.color
        val fillAlpha = if (activeMuscle == TargetMuscle.CALVES_BACK) pulseAlpha else 0.45f
        val leftCalf = Path().apply {
            moveTo(cx - dp2px(11f), dp2px(105f))
            cubicTo(cx - dp2px(13f), dp2px(114f), cx - dp2px(11f), dp2px(124f), cx - dp2px(8.5f), dp2px(132f))
            lineTo(cx - dp2px(7f), dp2px(132f))
            cubicTo(cx - dp2px(8f), dp2px(122f), cx - dp2px(8f), dp2px(112f), cx - dp2px(8f), dp2px(105f))
            close()
        }
        val rightCalf = Path().apply {
            moveTo(cx + dp2px(11f), dp2px(105f))
            cubicTo(cx + dp2px(13f), dp2px(114f), cx + dp2px(11f), dp2px(124f), cx + dp2px(8.5f), dp2px(132f))
            lineTo(cx + dp2px(7f), dp2px(132f))
            cubicTo(cx + dp2px(8f), dp2px(122f), cx + dp2px(8f), dp2px(112f), cx + dp2px(8f), dp2px(105f))
            close()
        }
        drawPath(leftCalf, color.copy(alpha = fillAlpha))
        drawPath(leftCalf, color, style = Stroke(dp2px(1.1f)))
        drawPath(rightCalf, color.copy(alpha = fillAlpha))
        drawPath(rightCalf, color, style = Stroke(dp2px(1.1f)))
    }
}

/**
 * 紧凑型灵动岛展开卡片右侧肌肉解剖 HUD
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
    val themeColor = muscle.category.color

    val infiniteTransition = rememberInfiniteTransition(label = "musclePulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.55f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 850, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    val baseBodyColor = Color(0xFF334155).copy(alpha = 0.45f)
    val bodyStrokeColor = Color(0xFF64748B).copy(alpha = 0.45f)
    val facialColor = Color(0xFF94A3B8)

    Box(
        modifier = modifier
            .width(82.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF0F172A).copy(alpha = 0.65f))
            .border(1.dp, themeColor.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
            .padding(vertical = 6.dp, horizontal = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // 根据肌肉主要位置决定显示正面还是背面，若双面均有则并排微缩
            val showFront = muscle.isFront
            val showBack = muscle.isBack && !muscle.isFront

            Canvas(
                modifier = Modifier.size(width = 66.dp, height = 74.dp)
            ) {
                val cx = size.width / 2f
                val h = size.height

                drawFemaleSilhouette(
                    cx = cx,
                    h = h,
                    isFront = showFront,
                    baseColor = baseBodyColor,
                    strokeColor = bodyStrokeColor,
                    facialColor = facialColor
                )

                if (showFront) {
                    drawFrontMuscles(
                        cx = cx,
                        h = h,
                        activeMuscle = muscle,
                        activeCategory = categoryName,
                        pulseAlpha = pulseAlpha
                    )
                } else {
                    drawBackMuscles(
                        cx = cx,
                        h = h,
                        activeMuscle = muscle,
                        activeCategory = categoryName,
                        pulseAlpha = pulseAlpha
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // 底部肌群标签 Chip (采用统一颜色)
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(themeColor.copy(alpha = 0.22f))
                    .padding(horizontal = 5.dp, vertical = 1.5.dp)
            ) {
                Text(
                    text = muscle.displayName,
                    color = themeColor,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    maxLines = 1
                )
            }
        }
    }
}
