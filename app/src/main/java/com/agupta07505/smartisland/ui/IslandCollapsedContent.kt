/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.ui

import com.agupta07505.smartisland.util.formatNotificationTime
import com.agupta07505.smartisland.util.HotspotUtil
import com.agupta07505.smartisland.ui.components.DottedRing

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.LinearEasing
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Navigation
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.BatteryAlert
import androidx.compose.material.icons.rounded.BatterySaver
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.AvTimer
import androidx.compose.material.icons.rounded.BluetoothConnected
import androidx.compose.material.icons.rounded.FlashlightOn
import androidx.compose.material.icons.rounded.HourglassBottom
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.WifiTethering
import androidx.compose.material.icons.rounded.Rowing
import androidx.compose.material.icons.rounded.VerticalAlignBottom
import androidx.compose.material.icons.rounded.VerticalAlignTop
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material.icons.rounded.TrendingUp
import androidx.compose.material.icons.rounded.DirectionsRun
import androidx.compose.material.icons.rounded.AirlineSeatReclineExtra
import androidx.compose.material.icons.rounded.DirectionsWalk
import androidx.compose.material.icons.rounded.SelfImprovement
import androidx.compose.material.icons.rounded.SportsGymnastics
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.agupta07505.smartisland.di.SmartIslandRepositories
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.agupta07505.smartisland.model.IslandMode
import com.agupta07505.smartisland.model.IslandNotification
import com.agupta07505.smartisland.data.SmartIslandSettings
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun IslandCollapsedContent(
    mode: IslandMode,
    notification: IslandNotification?,
    collapsedAlpha: Float,
    settings: SmartIslandSettings,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val maxTranslationPx = with(density) { COLLAPSED_TRANSLATION_MAX_DP.toPx() }
    val translationProgress = 1f - collapsedAlpha
    val translationXLeft = translationProgress * maxTranslationPx
    val translationXRight = -translationProgress * maxTranslationPx

    Box(modifier = modifier.fillMaxSize()) {
        // Left Slot (Icon / Glyphs)
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = LEFT_SLOT_PADDING_START_DP.dp)
                .graphicsLayer {
                    translationX = translationXLeft
                },
            contentAlignment = Alignment.Center
        ) {
            when (mode) {
                IslandMode.Notification -> NotificationGlyph(notification = notification, settings = settings)
                IslandMode.IncomingCall -> {
                    val icon = notification?.largeIcon ?: notification?.icon
                    if (icon != null) {
                        Image(
                            bitmap = icon.asImageBitmap(),
                            contentDescription = null,
                            modifier = Modifier
                                .size(22.dp)
                                .clip(CircleShape)
                        )
                    } else {
                        Icon(
                            Icons.Rounded.Call,
                            contentDescription = null,
                            tint = Color(settings.callColor),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                IslandMode.Music -> {
                    val artwork = notification?.largeIcon ?: notification?.icon
                    if (artwork != null) {
                        Image(
                            bitmap = artwork.asImageBitmap(),
                            contentDescription = null,
                            modifier = Modifier
                                .size(24.dp)
                                .clip(RoundedCornerShape(6.dp))
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(22.dp)
                                .clip(CircleShape)
                                .background(Color(settings.musicVisualizerColor)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Rounded.MusicNote,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
                IslandMode.Battery -> {
                    BatteryCollapsedGlyph(notification = notification, settings = settings)
                }
                IslandMode.LiveActivity -> {
                    LiveActivityCollapsedGlyph(notification = notification, settings = settings)
                }
                IslandMode.Navigation -> {
                    NavigationCollapsedGlyph(notification = notification, settings = settings)
                }
                IslandMode.DownloadUpload -> {
                    NotificationGlyph(notification = notification, settings = settings)
                }
                IslandMode.Hotspot -> {
                    HotspotCollapsedGlyph(notification = notification, settings = settings)
                }
                IslandMode.Bluetooth -> {
                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .clip(CircleShape)
                            .background(Color(settings.bluetoothColor).copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Rounded.BluetoothConnected,
                            contentDescription = "Bluetooth",
                            tint = Color(settings.bluetoothColor),
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
                IslandMode.Flashlight -> {
                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .clip(CircleShape)
                            .background(Color(settings.flashlightColor).copy(alpha = 0.25f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Rounded.FlashlightOn,
                            contentDescription = "Flashlight",
                            tint = Color(settings.flashlightColor),
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
                IslandMode.ScreenRecording -> {
                    ScreenRecordingCollapsedGlyph(settings = settings)
                }
                IslandMode.Timer -> {
                    TimerCollapsedGlyph(notification = notification, settings = settings)
                }
                IslandMode.Stopwatch -> {
                    StopwatchCollapsedGlyph(notification = notification, settings = settings)
                }
                IslandMode.Fitness -> {
                    FitnessCollapsedLeft()
                }
                IslandMode.Empty -> Unit
            }
        }

        // Right Slot (Visualizer / Indicators)
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = RIGHT_SLOT_PADDING_END_DP.dp)
                .graphicsLayer {
                    translationX = translationXRight
                },
            contentAlignment = Alignment.Center
        ) {
            when (mode) {
                IslandMode.Notification -> {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(Color(settings.notificationDotColor))
                    )
                }
                IslandMode.IncomingCall -> {
                    if (notification?.isCallRinging == true) {
                        Text(
                            text = "Ringing...",
                            color = Color(settings.callColor),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    } else {
                        val time = notification?.timeMillis ?: System.currentTimeMillis()
                        CallTimer(postTimeMillis = time, color = Color(settings.callColor))
                    }
                }
                IslandMode.Music -> {
                    AudioVisualizer(
                        isPlaying = notification?.mediaIsPlaying == true,
                        color = Color(settings.musicVisualizerColor)
                    )
                }
                IslandMode.Battery -> {
                    val pctText = notification?.text ?: "49%"
                    val title = notification?.title?.lowercase() ?: ""
                    val isBatterySaver = title.contains("saver") || notification?.category == "battery_saver"
                    val isLowBattery = title.contains("low") || notification?.category == "battery_low" || (pctText.replace("%", "").toFloatOrNull() ?: 50f) <= 20f
                    val textColor = when {
                        isLowBattery -> Color(0xFFEF4444)
                        isBatterySaver -> Color(0xFFF59E0B)
                        else -> Color(settings.batteryColor)
                    }
                    Text(
                        text = pctText,
                        color = textColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                IslandMode.LiveActivity -> {
                    LiveActivityCollapsedRight(notification = notification, settings = settings)
                }
                IslandMode.Navigation -> {
                    NavigationCollapsedRight(notification = notification, settings = settings)
                }
                IslandMode.DownloadUpload -> {
                    DownloadUploadCollapsedRight(notification = notification, settings = settings)
                }
                IslandMode.Hotspot -> {
                    HotspotCollapsedRight(notification = notification, settings = settings)
                }
                IslandMode.Bluetooth -> {
                    Image(
                        painter = painterResource(id = com.agupta07505.smartisland.R.drawable.ic_bluetooth_device),
                        contentDescription = "Bluetooth Device",
                        modifier = Modifier
                            .size(20.dp)
                            .clip(CircleShape)
                    )
                }
                IslandMode.Flashlight -> {
                    Text(
                        text = "ON",
                        color = Color(0xFFFACC15),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                IslandMode.ScreenRecording -> {
                    val time = notification?.timeMillis ?: System.currentTimeMillis()
                    CallTimer(postTimeMillis = time, color = Color(0xFFEF4444))
                }
                IslandMode.Timer -> {
                    TimerCountdown(notification = notification, color = Color(settings.timerColor))
                }
                IslandMode.Stopwatch -> {
                    StopwatchTimer(notification = notification, color = Color(settings.stopwatchColor))
                }
                IslandMode.Fitness -> {
                    FitnessCollapsedRight()
                }
                IslandMode.Empty -> Unit
            }
        }
    }
}

val FitnessBlue = Color(0xFF38BDF8) // Electric Sky Blue - 做组发力、运动中、专属动作图案高亮
val FitnessOrange = Color(0xFFFF9800) // Energetic Vibrant Orange - 组间休息、倒计时指示

fun getShortExerciseName(fullName: String): String {
    val clean = fullName.trim()
    return when {
        clean.contains("面拉") -> "面拉"
        clean.contains("颈后高位下拉") -> "颈后高位下拉"
        clean.contains("坐姿划船") -> "坐姿划船"
        clean.contains("引体向上") -> "辅助引体"
        clean.contains("反手高位下拉") -> "反手下拉"
        clean.contains("坐姿器械推胸") -> "器械推胸"
        clean.contains("史密斯机上斜推胸") -> "上斜推胸"
        clean.contains("龙门架绳索下拉") -> "绳索下拉"
        clean.contains("坐姿器械推举") -> "器械推举"
        clean.contains("蝴蝶机反向飞鸟") -> "反向飞鸟"
        clean.contains("蝴蝶机夹胸") -> "蝴蝶夹胸"
        clean.contains("椭圆机") -> "椭圆机"
        clean.contains("腿弯举") -> "腿弯举"
        clean.contains("髋外展") -> "髋外展"
        clean.contains("髋内收") -> "髋内收"
        clean.contains("史密斯机臀推") -> "史密斯臀推"
        clean.contains("倒蹬") -> "倒蹬机"
        clean.contains("腿屈伸") -> "腿屈伸"
        clean.contains("卷腹") -> "卷腹"
        clean.contains("平板支撑") -> "平板支撑"
        clean.contains("爬坡") -> "爬坡"
        clean.contains("拉伸小腿") -> "拉伸小腿"
        else -> clean.removeSuffix("器械").removeSuffix("机").trim()
    }
}

fun getExerciseIcon(exerciseName: String, categoryName: String): ImageVector {
    val name = exerciseName.lowercase()
    val cat = categoryName.lowercase()
    return when {
        name.contains("划船") -> Icons.Rounded.Rowing
        name.contains("下拉") -> Icons.Rounded.VerticalAlignBottom
        name.contains("引体") -> Icons.Rounded.VerticalAlignTop
        name.contains("面拉") -> Icons.Rounded.SwapHoriz
        name.contains("爬坡") -> Icons.Rounded.TrendingUp
        name.contains("椭圆机") || name.contains("跑") -> Icons.Rounded.DirectionsRun
        name.contains("平板支撑") -> Icons.Rounded.HourglassBottom
        name.contains("卷腹") || name.contains("腿屈伸") -> Icons.Rounded.AirlineSeatReclineExtra
        name.contains("腿") || name.contains("髋") || name.contains("蹬") -> Icons.Rounded.DirectionsWalk
        name.contains("臀") -> Icons.Rounded.SelfImprovement
        name.contains("飞鸟") || name.contains("夹胸") -> Icons.Rounded.SportsGymnastics
        name.contains("推") || name.contains("哑铃") -> Icons.Rounded.FitnessCenter
        cat.contains("背") -> Icons.Rounded.VerticalAlignBottom
        cat.contains("胸") -> Icons.Rounded.FitnessCenter
        cat.contains("臀") || cat.contains("腿") -> Icons.Rounded.DirectionsWalk
        cat.contains("腹") -> Icons.Rounded.AirlineSeatReclineExtra
        cat.contains("肩") -> Icons.Rounded.FitnessCenter
        cat.contains("有氧") -> Icons.Rounded.DirectionsRun
        else -> Icons.Rounded.FitnessCenter
    }
}

fun formatShortWeight(raw: String?): String {
    if (raw.isNullOrBlank()) return ""
    val trimmed = raw.trim()
    if (trimmed == "空杆") return "空杆"
    if (trimmed.contains("心率") || trimmed.startsWith("=")) return ""
    val firstLine = trimmed.lines().firstOrNull()?.trim().orEmpty()
    if (firstLine.isBlank() || firstLine.length > 8) return ""

    val clean = if (firstLine.endsWith(".0")) {
        firstLine.removeSuffix(".0")
    } else firstLine

    return when {
        clean.endsWith("kg", ignoreCase = true) -> clean
        clean.any { it.isDigit() } -> "${clean}kg"
        else -> clean
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
internal fun FitnessCollapsedLeft() {
    val context = LocalContext.current
    val fitnessRepo = remember { SmartIslandRepositories.fitnessRepository(context) }
    val state by fitnessRepo.sessionState.collectAsState()
    val exercise = state.currentExercise

    val iconTint = if (state.isResting) FitnessOrange else FitnessBlue
    val actionIcon = getExerciseIcon(exercise?.name.orEmpty(), state.categoryName)
    val shortName = getShortExerciseName(exercise?.name ?: "举铁伴侣")

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        modifier = Modifier.widthIn(max = 54.dp)
    ) {
        Icon(
            imageVector = actionIcon,
            contentDescription = "Fitness Action",
            tint = iconTint,
            modifier = Modifier.size(13.dp)
        )
        Text(
            text = shortName,
            color = Color.White,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            modifier = Modifier.basicMarquee()
        )
    }
}

@Composable
internal fun FitnessCollapsedRight() {
    val context = LocalContext.current
    val fitnessRepo = remember { SmartIslandRepositories.fitnessRepository(context) }
    val state by fitnessRepo.sessionState.collectAsState()
    val exercise = state.currentExercise
    val shortWeight = formatShortWeight(exercise?.weight)

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.End),
        modifier = Modifier.widthIn(max = 50.dp)
    ) {
        if (shortWeight.isNotBlank()) {
            Text(
                text = shortWeight,
                color = Color.White,
                fontSize = 10.5.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
        }

        if (state.isResting) {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "休息",
                    color = FitnessOrange,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Medium,
                    lineHeight = 9.sp
                )
                Text(
                    text = "${state.restSecondsRemaining}s",
                    color = FitnessOrange,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    lineHeight = 12.sp
                )
            }
        } else if (state.exerciseSecondsRemaining > 0) {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "${state.currentSet}/${state.totalSets}",
                    color = Color.White,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    lineHeight = 11.sp
                )
                Text(
                    text = "${state.exerciseSecondsRemaining}s",
                    color = FitnessBlue,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    lineHeight = 12.sp
                )
            }
        } else {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "${state.currentSet}/${state.totalSets}",
                    color = Color.White,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    lineHeight = 11.sp
                )
                Text(
                    text = "${state.setElapsedSeconds}s",
                    color = FitnessBlue,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    lineHeight = 12.sp
                )
            }
        }
    }
}

@Composable
internal fun HotspotCollapsedGlyph(notification: IslandNotification?, settings: SmartIslandSettings = SmartIslandSettings.Default) {
    val hotspotColor = Color(settings.hotspotColor)
    Box(
        modifier = Modifier
            .size(22.dp)
            .clip(CircleShape)
            .background(hotspotColor.copy(alpha = 0.2f)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Rounded.WifiTethering,
            contentDescription = "Hotspot",
            tint = hotspotColor,
            modifier = Modifier.size(15.dp)
        )
    }
}

@Composable
private fun HotspotCollapsedRight(notification: IslandNotification?, settings: SmartIslandSettings) {
    val countText = remember(notification?.text, notification?.title) {
        HotspotUtil.parseDeviceCount(notification?.title, notification?.text).toString()
    }

    Text(
        text = countText,
        color = Color(settings.hotspotColor),
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold
    )
}

@Composable
internal fun BatteryCollapsedGlyph(notification: IslandNotification?, settings: SmartIslandSettings) {
    val pctText = notification?.text?.replace("%", "")?.trim() ?: "49"
    val pct = pctText.toFloatOrNull() ?: 49f
    val progress = (pct / 100f).coerceIn(0f, 1f)
    val title = notification?.title?.lowercase() ?: ""
    val isBatterySaver = title.contains("saver") || notification?.category == "battery_saver"
    val isLowBattery = title.contains("low") || notification?.category == "battery_low" || pct <= 20f

    val batteryColor = when {
        isLowBattery -> Color(0xFFEF4444)
        isBatterySaver -> Color(0xFFF59E0B)
        else -> Color(settings.batteryColor)
    }

    val infiniteTransition = rememberInfiniteTransition(label = "batteryPulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "batteryScale"
    )

    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 8000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "dottedRingRotation"
    )

    Box(
        modifier = Modifier.size(24.dp),
        contentAlignment = Alignment.Center
    ) {
        DottedRing(
            progress = progress,
            rotationAngle = rotationAngle,
            modifier = Modifier.size(22.dp),
            color = batteryColor
        )
        when {
            isBatterySaver -> {
                Icon(
                    Icons.Rounded.BatterySaver,
                    contentDescription = "Battery Saver",
                    tint = batteryColor,
                    modifier = Modifier.size(14.dp)
                )
            }
            isLowBattery -> {
                Icon(
                    Icons.Rounded.BatteryAlert,
                    contentDescription = "Low Battery",
                    tint = batteryColor,
                    modifier = Modifier
                        .size(14.dp)
                        .graphicsLayer {
                            scaleX = pulseScale
                            scaleY = pulseScale
                        }
                )
            }
            else -> {
                Icon(
                    Icons.Rounded.Bolt,
                    contentDescription = "Charging",
                    tint = batteryColor,
                    modifier = Modifier
                        .size(14.dp)
                        .graphicsLayer {
                            scaleX = pulseScale
                            scaleY = pulseScale
                        }
                )
            }
        }
    }
}

@Composable
internal fun NotificationGlyph(notification: IslandNotification?, settings: SmartIslandSettings = SmartIslandSettings.Default) {
    val largeIcon = notification?.largeIcon
    val icon = notification?.icon
    val mainIcon = largeIcon ?: icon
    if (mainIcon != null) {
        Box(modifier = Modifier.size(22.dp)) {
            Image(
                bitmap = mainIcon.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
            )
            if (largeIcon != null && icon != null) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .align(Alignment.BottomEnd)
                        .background(Color.Black, CircleShape)
                        .padding(1.dp)
                ) {
                    Image(
                        bitmap = icon.asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                    )
                }
            }
        }
    } else {
        Box(
            modifier = Modifier
                .size(22.dp)
                .clip(CircleShape)
                .background(Color(settings.notificationDotColor)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = notification?.appName?.firstOrNull()?.uppercase() ?: "S",
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun AudioVisualizer(
    isPlaying: Boolean,
    color: Color,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val infiniteTransition = rememberInfiniteTransition(label = "audio_visualizer")
        val heights = listOf(0.3f to 0.9f, 0.5f to 1.0f, 0.2f to 0.7f)

        heights.forEachIndexed { index, (min, max) ->
            val heightFraction by infiniteTransition.animateFloat(
                initialValue = min,
                targetValue = if (isPlaying) max else min,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 350 + index * 80, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "bar_$index"
            )

            Box(
                modifier = Modifier
                    .size(width = 3.dp, height = 14.dp)
                    .graphicsLayer {
                        scaleY = if (isPlaying) heightFraction else min
                        transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0.5f, 0.5f)
                    }
                    .clip(RoundedCornerShape(1.dp))
                    .background(color)
            )
        }
    }
}


@Composable
internal fun CallTimer(postTimeMillis: Long, color: Color) {
    var elapsedSeconds by remember(postTimeMillis) {
        mutableStateOf(((System.currentTimeMillis() - postTimeMillis) / 1000).coerceAtLeast(0L))
    }
    LaunchedEffect(postTimeMillis) {
        while (true) {
            elapsedSeconds = ((System.currentTimeMillis() - postTimeMillis) / 1000).coerceAtLeast(0L)
            kotlinx.coroutines.delay(1000)
        }
    }
    val minutes = elapsedSeconds / 60
    val seconds = elapsedSeconds % 60
    Text(
        text = "%02d:%02d".format(minutes, seconds),
        color = color,
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold
    )
}

@Composable
internal fun LiveActivityCollapsedGlyph(notification: IslandNotification?, settings: SmartIslandSettings = SmartIslandSettings.Default) {
    val brandColor = remember(notification?.packageName, settings.liveActivityColor) {
        if (notification != null) {
            Color(com.agupta07505.smartisland.util.LiveActivityParser.getBrandColor(notification.packageName))
        } else {
            Color(settings.liveActivityColor)
        }
    }
    val progress = remember(notification) {
        if (notification != null && notification.progressMax > 0) {
            (notification.progress.toFloat() / notification.progressMax.toFloat()).coerceIn(0.15f, 1f)
        } else {
            0.65f
        }
    }

    Box(
        modifier = Modifier.size(24.dp),
        contentAlignment = Alignment.Center
    ) {
        SmoothCircularRingProgress(
            progress = progress,
            color = brandColor,
            strokeWidthDp = 4.8f,
            modifier = Modifier.size(24.dp)
        )
        val mainIcon = notification?.largeIcon ?: notification?.icon
        if (mainIcon != null) {
            Image(
                bitmap = mainIcon.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier
                    .size(13.dp)
                    .clip(CircleShape)
            )
        } else {
            NotificationGlyph(notification = notification, settings = settings)
        }
    }
}

@Composable
private fun SmoothCircularRingProgress(
    progress: Float,
    color: Color,
    modifier: Modifier = Modifier,
    strokeWidthDp: Float = 4.5f
) {
    androidx.compose.foundation.Canvas(modifier = modifier) {
        val strokeWidth = strokeWidthDp.dp.toPx()
        val diameter = size.minDimension - strokeWidth
        val topLeftOffset = androidx.compose.ui.geometry.Offset(strokeWidth / 2f, strokeWidth / 2f)
        val arcSize = androidx.compose.ui.geometry.Size(diameter, diameter)

        // Track background ring
        drawArc(
            color = Color(0x4DFFFFFF),
            startAngle = -90f,
            sweepAngle = 360f,
            useCenter = false,
            topLeft = topLeftOffset,
            size = arcSize,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
        )

        // Filled progress arc (360 degrees for completed 1.0f)
        val sweepAngle = (360f * progress.coerceIn(0f, 1f))
        if (sweepAngle > 0f) {
            drawArc(
                color = color,
                startAngle = -90f,
                sweepAngle = sweepAngle,
                useCenter = false,
                topLeft = topLeftOffset,
                size = arcSize,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
        }
    }
}

@Composable
private fun LiveActivityCollapsedRight(notification: IslandNotification?, settings: SmartIslandSettings = SmartIslandSettings.Default) {
    val brandColor = remember(notification?.packageName, settings.liveActivityColor) {
        if (notification != null) {
            Color(com.agupta07505.smartisland.util.LiveActivityParser.getBrandColor(notification.packageName))
        } else {
            Color(settings.liveActivityColor)
        }
    }
    val etaText = remember(notification) {
        if (notification == null) return@remember "Active"
        val text = "${notification.title} ${notification.text}"
        val matcher = java.util.regex.Pattern.compile("(\\d+)\\s*(?:mins?|minutes?|min|m)\\b", java.util.regex.Pattern.CASE_INSENSITIVE).matcher(text)
        if (matcher.find()) {
            "${matcher.group(1)} min"
        } else if (text.lowercase().contains("arrived")) {
            "Arrived"
        } else {
            "Active"
        }
    }

    Text(
        text = etaText,
        color = brandColor,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold
    )
}

@Composable
internal fun NavigationCollapsedGlyph(notification: IslandNotification?, settings: SmartIslandSettings = SmartIslandSettings.Default) {
    val navColor = Color(settings.liveActivityColor)
    val turnDirection = remember(notification) {
        val title = notification?.title.orEmpty()
        val text = notification?.text.orEmpty()
        com.agupta07505.smartisland.util.NavigationParser.parseTurnDirection("$title $text".lowercase())
    }

    val angle = when (turnDirection) {
        com.agupta07505.smartisland.util.TurnDirection.LEFT -> -90f
        com.agupta07505.smartisland.util.TurnDirection.RIGHT -> 90f
        com.agupta07505.smartisland.util.TurnDirection.SLIGHT_LEFT -> -45f
        com.agupta07505.smartisland.util.TurnDirection.SLIGHT_RIGHT -> 45f
        com.agupta07505.smartisland.util.TurnDirection.U_TURN -> 180f
        else -> 0f
    }

    if (turnDirection == com.agupta07505.smartisland.util.TurnDirection.DESTINATION) {
        Icon(
            imageVector = Icons.Rounded.LocationOn,
            contentDescription = "Destination",
            tint = navColor,
            modifier = Modifier.size(18.dp)
        )
    } else {
        Icon(
            imageVector = Icons.Rounded.Navigation,
            contentDescription = "Turn direction",
            tint = navColor,
            modifier = Modifier
                .size(18.dp)
                .rotate(angle)
        )
    }
}

@Composable
private fun NavigationCollapsedRight(notification: IslandNotification?, settings: SmartIslandSettings = SmartIslandSettings.Default) {
    val distanceText = remember(notification) {
        if (notification == null) return@remember "200 m"
        val title = notification.title
        val text = notification.text
        val pattern = java.util.regex.Pattern.compile("(\\d+(?:\\.\\d+)?)\\s*(?:m|km|ft|mi|miles?|meters?)\\b", java.util.regex.Pattern.CASE_INSENSITIVE)
        val matcher = pattern.matcher("$title $text")
        if (matcher.find()) matcher.group(0) else "In 200 m"
    }

    Text(
        text = distanceText,
        color = Color(settings.liveActivityColor),
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold
    )
}

@Composable
private fun CustomPremiumTransferIcon(
    isUpload: Boolean,
    color: Color,
    modifier: Modifier = Modifier,
    motionProgress: Float = 0.5f,
    alphaFraction: Float = 1f
) {
    androidx.compose.foundation.Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val cx = w / 2f

        val strokeWidth = 1.8.dp.toPx()
        val arrowHeadWidth = 3.5.dp.toPx()
        val arrowHeadHeight = 3.5.dp.toPx()
        val shaftLength = 6.dp.toPx()

        val startY = if (isUpload) h * 0.95f else -h * 0.15f
        val endY = if (isUpload) -h * 0.15f else h * 0.95f
        val currentCenterY = startY + (endY - startY) * motionProgress

        val tipY = if (isUpload) currentCenterY - shaftLength / 2f else currentCenterY + shaftLength / 2f
        val tailY = if (isUpload) currentCenterY + shaftLength / 2f else currentCenterY - shaftLength / 2f

        val drawAlpha = alphaFraction.coerceIn(0f, 1f)
        val drawColor = color.copy(alpha = drawAlpha)

        // 1. Main Arrow Shaft
        drawLine(
            color = drawColor,
            start = androidx.compose.ui.geometry.Offset(cx, tailY),
            end = androidx.compose.ui.geometry.Offset(cx, tipY),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )

        // 2. Main Arrow Head Wings
        val wingY = if (isUpload) tipY + arrowHeadHeight else tipY - arrowHeadHeight
        val path = androidx.compose.ui.graphics.Path().apply {
            moveTo(cx - arrowHeadWidth, wingY)
            lineTo(cx, tipY)
            lineTo(cx + arrowHeadWidth, wingY)
        }
        drawPath(
            path = path,
            color = drawColor,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = androidx.compose.ui.graphics.StrokeJoin.Round)
        )

        // 3. Subtle Trailing Particle Dot
        val particleOffsetY = if (isUpload) 4.5.dp.toPx() else -4.5.dp.toPx()
        val particleY = tailY + particleOffsetY
        val particleAlpha = (drawAlpha * 0.5f).coerceIn(0f, 1f)
        drawCircle(
            color = color.copy(alpha = particleAlpha),
            radius = strokeWidth * 0.6f,
            center = androidx.compose.ui.geometry.Offset(cx, particleY)
        )
    }
}

@Composable
private fun DownloadUploadCollapsedRight(
    notification: IslandNotification?,
    settings: SmartIslandSettings = SmartIslandSettings.Default
) {
    val textCombined = remember(notification) {
        "${notification?.title} ${notification?.text}".lowercase()
    }
    val uploadKeywords = listOf("upload", "uploading", "sending", "posting", "exporting", "backing up", "backup")
    val isUpload = remember(textCombined) { uploadKeywords.any { textCombined.contains(it) } }
    val accentColor = Color(settings.transferColor)

    val progress = remember(notification) {
        if (notification != null && notification.progressMax > 0) {
            (notification.progress.toFloat() / notification.progressMax.toFloat()).coerceIn(0.1f, 1f)
        } else {
            0.55f
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "downloadUploadAnim")

    val motionFraction by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "arrowFlow"
    )

    val arrowAlpha = when {
        motionFraction < 0.2f -> motionFraction / 0.2f
        motionFraction > 0.8f -> (1f - motionFraction) / 0.2f
        else -> 1f
    }.coerceIn(0f, 1f)

    Box(
        modifier = Modifier.size(22.dp),
        contentAlignment = Alignment.Center
    ) {
        SmoothCircularRingProgress(
            progress = progress,
            color = accentColor,
            strokeWidthDp = 2.5f,
            modifier = Modifier.size(19.dp)
        )

        Box(
            modifier = Modifier
                .size(19.dp)
                .clip(CircleShape),
            contentAlignment = Alignment.Center
        ) {
            CustomPremiumTransferIcon(
                isUpload = isUpload,
                color = accentColor,
                motionProgress = motionFraction,
                alphaFraction = arrowAlpha,
                modifier = Modifier.size(14.dp)
            )
        }
    }
}

@Composable
internal fun ScreenRecordingCollapsedGlyph(settings: SmartIslandSettings = SmartIslandSettings.Default) {
    val infiniteTransition = rememberInfiniteTransition(label = "collapsedRecordingPulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    val recordingColor = Color(settings.screenRecordingColor)

    Box(
        modifier = Modifier
            .size(22.dp)
            .clip(CircleShape)
            .background(recordingColor.copy(alpha = 0.25f)),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .graphicsLayer { alpha = pulseAlpha }
                .clip(CircleShape)
                .background(recordingColor)
        )
    }
}

@Composable
internal fun TimerCollapsedGlyph(notification: IslandNotification?, settings: SmartIslandSettings = SmartIslandSettings.Default) {
    val timerColor = Color(settings.timerColor)
    val infiniteTransition = rememberInfiniteTransition(label = "timerPulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "timerPulseScale"
    )

    Box(
        modifier = Modifier
            .size(22.dp)
            .clip(CircleShape)
            .background(timerColor.copy(alpha = 0.2f)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Rounded.HourglassBottom,
            contentDescription = "Timer",
            tint = timerColor,
            modifier = Modifier
                .size(13.dp)
                .graphicsLayer {
                    scaleX = pulseScale
                    scaleY = pulseScale
                }
        )
    }
}

@Composable
internal fun StopwatchCollapsedGlyph(notification: IslandNotification?, settings: SmartIslandSettings = SmartIslandSettings.Default) {
    val stopwatchColor = Color(settings.stopwatchColor)
    val infiniteTransition = rememberInfiniteTransition(label = "stopwatchRotate")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "stopwatchRot"
    )

    Box(
        modifier = Modifier
            .size(22.dp)
            .clip(CircleShape)
            .background(stopwatchColor.copy(alpha = 0.2f)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Rounded.AvTimer,
            contentDescription = "Stopwatch",
            tint = stopwatchColor,
            modifier = Modifier
                .size(14.dp)
                .graphicsLayer { rotationZ = rotation }
        )
    }
}

@Composable
internal fun TimerCountdown(notification: IslandNotification?, color: Color) {
    val isPaused = remember(notification?.key, notification?.actionIntents, notification?.text, notification?.title) {
        val actions = notification?.actionIntents.orEmpty()
        actions.any {
            val t = it.title.lowercase()
            t.contains("resume") || t.contains("start") || t.contains("play") || t.contains("continue") || t.contains("unpause")
        } || notification?.text?.contains("pause", ignoreCase = true) == true ||
            notification?.title?.contains("pause", ignoreCase = true) == true
    }

    val targetTime = notification?.timeMillis ?: remember { System.currentTimeMillis() + 300000L }

    var remainingSec by remember(notification?.key, targetTime, isPaused) {
        val rem = if (targetTime > System.currentTimeMillis()) {
            ((targetTime - System.currentTimeMillis() + 500L) / 1000L).coerceAtLeast(0L)
        } else {
            0L
        }
        mutableStateOf(rem)
    }

    LaunchedEffect(notification?.key, targetTime, isPaused) {
        if (!isPaused) {
            while (true) {
                val now = System.currentTimeMillis()
                val rem = if (targetTime > now) {
                    ((targetTime - now + 500L) / 1000L).coerceAtLeast(0L)
                } else {
                    0L
                }
                remainingSec = rem
                if (rem <= 0L) break
                kotlinx.coroutines.delay(500L)
            }
        }
    }

    val text = remember(remainingSec) {
        com.agupta07505.smartisland.util.TimerStopwatchParser.formatTime(remainingSec)
    }

    Text(
        text = text,
        color = color,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold
    )
}

@Composable
internal fun StopwatchTimer(notification: IslandNotification?, color: Color) {
    val startTime = notification?.timeMillis ?: remember { System.currentTimeMillis() }
    val isPaused = remember(notification?.key, notification?.actionIntents) {
        notification?.actionIntents?.any {
            it.title.contains("resume", ignoreCase = true) || it.title.contains("start", ignoreCase = true)
        } == true
    }

    var elapsedSeconds by remember(notification?.key, startTime) {
        mutableStateOf(((System.currentTimeMillis() - startTime) / 1000L).coerceAtLeast(0L))
    }

    LaunchedEffect(notification?.key, startTime, isPaused) {
        if (!isPaused) {
            while (true) {
                elapsedSeconds = ((System.currentTimeMillis() - startTime) / 1000L).coerceAtLeast(0L)
                kotlinx.coroutines.delay(500L)
            }
        }
    }

    val text = remember(elapsedSeconds) {
        com.agupta07505.smartisland.util.TimerStopwatchParser.formatTime(elapsedSeconds)
    }

    Text(
        text = text,
        color = color,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold
    )
}

// Collapsed content animation
private val COLLAPSED_TRANSLATION_MAX_DP = 32.dp
private const val LEFT_SLOT_PADDING_START_DP = 8
private const val RIGHT_SLOT_PADDING_END_DP = 12
