package com.agupta07505.smartisland.ui.expanded

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.agupta07505.smartisland.data.SmartIslandSettings
import com.agupta07505.smartisland.di.SmartIslandRepositories
import com.agupta07505.smartisland.model.IslandNotification
import com.agupta07505.smartisland.ui.components.MuscleMatcher

@Composable
fun FitnessExpanded(
    notification: IslandNotification?,
    bottomPadding: Dp,
    onCollapse: () -> Unit = {},
    settings: SmartIslandSettings = SmartIslandSettings.Default
) {
    val context = LocalContext.current
    val fitnessRepo = SmartIslandRepositories.fitnessRepository(context)
    val state by fitnessRepo.sessionState.collectAsState()
    val exercise = state.currentExercise

    val targetMuscle = remember(exercise?.name, state.categoryName, exercise?.targetMuscle) {
        MuscleMatcher.match(exercise?.name.orEmpty(), state.categoryName, exercise?.targetMuscle)
    }
    val themeColor = targetMuscle.category.color
    val restOrange = Color(0xFFFF9800)

    val launchVideo = {
        val cleanUrl = exercise?.videoUrl?.trim().orEmpty()
        if (cleanUrl.isNotBlank()) {
            try {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(cleanUrl)).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(context, "无法唤醒视频播放", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(context, "当前动作暂无视频链接", Toast.LENGTH_SHORT).show()
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { launchVideo() }
            .padding(horizontal = 18.dp, vertical = 12.dp)
            .padding(bottom = bottomPadding),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // 1. 左侧动作核心数据排版区 (权重 1f)
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(end = 10.dp),
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.Center
        ) {
            // 动作名称 + 匹配图标 (使用该部位统一颜色)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val actionIcon = com.agupta07505.smartisland.ui.getExerciseIcon(exercise?.name.orEmpty(), state.categoryName)
                Icon(
                    imageVector = actionIcon,
                    contentDescription = null,
                    tint = if (state.isResting) restOrange else themeColor,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = exercise?.name ?: "举铁伴侣",
                    color = Color.White,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Start,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // 重量与组数/倒计时信息
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val rawWeight = exercise?.weight?.trim().orEmpty()
                val cleanWeight = if (rawWeight.endsWith(".0")) rawWeight.removeSuffix(".0") else rawWeight
                val displayWeight = when {
                    cleanWeight.isBlank() -> "自重 / 徒手"
                    cleanWeight.endsWith("kg", ignoreCase = true) || cleanWeight == "空杆" -> cleanWeight
                    else -> "$cleanWeight kg"
                }
                Text(
                    text = displayWeight,
                    color = themeColor.copy(alpha = 0.95f),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "•",
                    color = Color.White.copy(alpha = 0.4f),
                    fontSize = 13.sp
                )

                val setInfo = if (state.isResting) {
                    "休息中 ${state.restSecondsRemaining}s"
                } else if (state.exerciseSecondsRemaining > 0) {
                    "第 ${state.currentSet}/${state.totalSets} 组 · ${state.exerciseSecondsRemaining}s"
                } else {
                    "第 ${state.currentSet}/${state.totalSets} 组 · 做组中 ${state.setElapsedSeconds}s"
                }
                Text(
                    text = setInfo,
                    color = if (state.isResting) restOrange else Color.White.copy(alpha = 0.9f),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            // 个人动作注意事项 / 器械设置 / 发力口诀
            val isPersonalTip = !exercise?.setupTips.isNullOrBlank()
            val cue = exercise?.setupTips?.takeIf { it.isNotBlank() } ?: state.cue?.trim()
            if (!cue.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.Start
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Lightbulb,
                        contentDescription = null,
                        tint = if (isPersonalTip) Color(0xFFFFB74D) else Color(0xFFFFD54F),
                        modifier = Modifier
                            .size(13.dp)
                            .padding(top = 2.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isPersonalTip) "要领: $cue" else cue,
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 11.sp,
                        lineHeight = 15.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Start
                    )
                }
            }
        }

        // 2. 右侧闲置区：绘制并点亮所锻炼肌肉的解剖微图 (统一颜色系统)
        com.agupta07505.smartisland.ui.components.MuscleAnatomyVisualizer(
            exerciseName = exercise?.name.orEmpty(),
            categoryName = state.categoryName,
            isResting = state.isResting
        )
    }
}
