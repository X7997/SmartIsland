/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.ui

import com.agupta07505.smartisland.ui.expanded.IslandExpandedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDp
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.input.pointer.positionChange
import kotlin.math.abs
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import com.agupta07505.smartisland.data.SmartIslandSettings
import com.agupta07505.smartisland.di.SmartIslandRepositories
import com.agupta07505.smartisland.model.IslandMode
import com.agupta07505.smartisland.model.IslandNotification
import com.agupta07505.smartisland.data.LaunchableApp
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.FlashlightOn
import androidx.compose.ui.res.painterResource
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

@Composable
fun IslandOverlayView(
    settings: SmartIslandSettings,
    expanded: Boolean,
    notifications: List<IslandNotification>,
    selectedIndex: Int,
    launcherApps: List<LaunchableApp>?,
    onPageSelected: (Int) -> Unit,
    onOpenNotification: (IslandNotification) -> Unit,
    onLaunchApp: (String) -> Unit,
    onToggleExpanded: () -> Unit,
    onDismissNotification: () -> Unit,
    onOpenFloatingWindow: () -> Unit,
    statusBarHeight: Float,
    modifier: Modifier = Modifier,
    isInputActive: Boolean = false,
    onReplyStateChanged: (Boolean) -> Unit = {},
    onDismissAllNotifications: () -> Unit = {},
    isFullWidth: Boolean = true
) {
    // Fix #1: rememberUpdatedState ensures the lambda is always fresh
    // even though pointerInput(Unit) never restarts its coroutine
    val currentOnToggle by rememberUpdatedState(onToggleExpanded)
    val currentOnDismiss by rememberUpdatedState(onDismissNotification)
    val currentOnDismissAll by rememberUpdatedState(onDismissAllNotifications)
    val currentOnOpenFloatingWindow by rememberUpdatedState(onOpenFloatingWindow)
    val currentOnOpenNotification by rememberUpdatedState(onOpenNotification)
    val currentExpanded by rememberUpdatedState(expanded)
    val haptic = LocalHapticFeedback.current

    val scope = rememberCoroutineScope()
    var dragOffset by remember { mutableStateOf(0f) }

    val context = LocalContext.current
    val displayMetrics = context.resources.displayMetrics
    val density = LocalDensity.current
    val screenWidth = with(density) { displayMetrics.widthPixels.toDp() }
    val screenCenter = screenWidth / 2f
    val expandedWidth = ((displayMetrics.widthPixels / displayMetrics.density) * EXPANDED_WIDTH_RATIO).dp
    val transition = updateTransition(targetState = expanded, label = "islandTransition")

    val sizeSpec = spring<androidx.compose.ui.unit.Dp>(
        dampingRatio = 0.65f,
        stiffness = 450f
    )
    val sizeSpecFloat = spring<Float>(
        dampingRatio = 0.65f,
        stiffness = 450f
    )
    val heightSpec = spring<androidx.compose.ui.unit.Dp>(
        dampingRatio = 0.65f,
        stiffness = 450f
    )
    val alphaSpec = tween<Float>(
        durationMillis = 190,
        easing = FastOutSlowInEasing
    )

    val safeIndex = selectedIndex.coerceIn(0, (notifications.size - 1).coerceAtLeast(0))
    val activeNotification = notifications.getOrNull(safeIndex)
    val activeMode = activeNotification?.mode ?: IslandMode.Empty

    val initialEstimatedHeight = remember(activeMode, notifications.isEmpty()) {
        if (notifications.isEmpty()) 135.dp else defaultEstimatedHeightForMode(activeMode)
    }
    var expandedHeight by remember { mutableStateOf(initialEstimatedHeight) }

    LaunchedEffect(activeMode, notifications.isEmpty()) {
        if (!expanded) {
            expandedHeight = if (notifications.isEmpty()) 135.dp else defaultEstimatedHeightForMode(activeMode)
        }
    }

    val compactGap = COMPACT_INDICATOR_GAP_DP.dp
    val miniPillWidth = remember(activeMode, settings.width) {
        when (activeMode) {
            IslandMode.Fitness -> settings.width.dp
            else -> settings.width.dp
        }
    }
    val circleSize = settings.height.dp
    val compactShapes = compactNotificationShapes(notifications.size, expanded)
    val hasCompanion = notifications.size >= 2
    val minMainLeft = compactGap
    val maxMainLeft = (screenWidth - miniPillWidth - compactGap).coerceAtLeast(minMainLeft)
    val collapsedMainLeft = (screenCenter + settings.xOffset.dp - miniPillWidth / 2f)
        .coerceIn(minMainLeft, maxMainLeft)
    // Primary island horizontal position is always fixed regardless of whether secondary island exists
    val collapsedMainOffset = if (isFullWidth) {
        settings.xOffset.dp
    } else {
        if (hasCompanion) (compactGap + circleSize) / 2f else 0.dp
    }
    val expandedTopOffset = if (hasCompanion) {
        statusBarHeight.dp.coerceAtLeast(circleSize + compactGap)
    } else {
        statusBarHeight.dp
    }
    val isIdleHiding = settings.hideWhenIdle && notifications.isEmpty()

    var isAutoHidden by remember { mutableStateOf(false) }
    var userInteractionTimestamp by remember { mutableStateOf(System.currentTimeMillis()) }

    // Reset auto-hide whenever active notifications change or selection changes
    LaunchedEffect(notifications.map { it.key }, selectedIndex) {
        isAutoHidden = false
        userInteractionTimestamp = System.currentTimeMillis()
    }

    // Auto-hide countdown timer when pill is collapsed and autoHidePill is enabled
    LaunchedEffect(expanded, settings.autoHidePill, settings.autoHideTimeoutSeconds, userInteractionTimestamp) {
        if (expanded || !settings.autoHidePill) {
            isAutoHidden = false
            return@LaunchedEffect
        }
        val timeoutMs = (settings.autoHideTimeoutSeconds.coerceAtLeast(1) * 1000L)
        kotlinx.coroutines.delay(timeoutMs)
        isAutoHidden = true
    }

    val isHiding = isIdleHiding || (settings.autoHidePill && isAutoHidden)

    val width by transition.animateDp(transitionSpec = { sizeSpec }, label = "islandWidth") {
        if (it) expandedWidth else if (isHiding) 0.dp else miniPillWidth
    }
    val height by transition.animateDp(transitionSpec = { heightSpec }, label = "islandHeight") {
        if (it) expandedHeight else if (isHiding) 0.dp else settings.height.dp
    }
    val yOffset by transition.animateDp(transitionSpec = { sizeSpec }, label = "islandYOffset") {
        if (it) expandedTopOffset else settings.yOffset.dp
    }
    val radius by transition.animateDp(transitionSpec = { sizeSpec }, label = "islandRadius") {
        if (it) 34.dp else if (isHiding) 0.dp else settings.cornerRadius.dp
    }
    val animatedXOffset by transition.animateDp(transitionSpec = { sizeSpec }, label = "islandXOffset") {
        if (it) 0.dp else collapsedMainOffset
    }

    val collapsedAlpha by transition.animateFloat(
        transitionSpec = { alphaSpec },
        label = "collapsedAlpha"
    ) {
        if (it || isHiding) 0f else 1f
    }

    val expandedAlpha by transition.animateFloat(
        transitionSpec = { alphaSpec },
        label = "expandedAlpha"
    ) {
        if (it) 1f else 0f
    }

    val contentScale by transition.animateFloat(
        transitionSpec = { sizeSpecFloat },
        label = "contentScale"
    ) {
        if (it) 1f else 0.95f
    }

    val contentSlideY by transition.animateDp(
        transitionSpec = { sizeSpec },
        label = "contentSlideY"
    ) {
        if (it) 0.dp else (-6).dp
    }

    val safeWidth = width.coerceAtLeast(0.dp)
    val safeHeight = height.coerceAtLeast(0.dp)
    val safeRadius = radius.coerceAtLeast(0.dp)

    // Tactile spring scale bounce animation only when user switches between active notifications
    val switchScaleAnim = remember { androidx.compose.animation.core.Animatable(1f) }
    var isInitialComposition by remember { mutableStateOf(true) }
    var lastSelectedIndex by remember { mutableStateOf(selectedIndex) }
    LaunchedEffect(selectedIndex) {
        if (isInitialComposition) {
            isInitialComposition = false
            lastSelectedIndex = selectedIndex
            return@LaunchedEffect
        }
        if (lastSelectedIndex != selectedIndex) {
            lastSelectedIndex = selectedIndex
            switchScaleAnim.animateTo(
                targetValue = 0.92f,
                animationSpec = tween(40, easing = FastOutSlowInEasing)
            )
            switchScaleAnim.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = 650f
                )
            )
        }
    }

    // Dual Pill (Multi-Tasking Split Island) Detection:
    // When 2 or more notifications exist (e.g. Music + Notification/Timer/Call/Fitness), split into Main Pill + Secondary Bubble
    val secondaryNotification = if (notifications.size >= 2) {
        notifications.firstOrNull { it.key != activeNotification?.key }
    } else null
    val secondaryIndex = if (secondaryNotification != null) {
        notifications.indexOfFirst { it.key == secondaryNotification.key }
    } else -1
    val tertiaryNotification = if (notifications.size >= 3) {
        notifications.firstOrNull {
            it.key != activeNotification?.key && it.key != secondaryNotification?.key
        }
    } else null
    val tertiaryIndex = if (tertiaryNotification != null) {
        notifications.indexOfFirst { it.key == tertiaryNotification.key }
    } else -1
    val isSplitMode = secondaryNotification != null
    val secondaryIsPill = compactShapes.singleOrNull() == CompactNotificationShape.MiniPill
    val showTertiaryPill = compactShapes.size == 2 && tertiaryNotification != null

    val secondaryAlpha by animateFloatAsState(
        targetValue = if (isSplitMode && !isHiding) 1f else 0f,
        animationSpec = tween(durationMillis = 180, easing = FastOutSlowInEasing),
        label = "secondaryAlpha"
    )
    val secondaryScale by animateFloatAsState(
        targetValue = if (isSplitMode && !isHiding) 1f else 0.3f,
        animationSpec = spring(dampingRatio = 0.65f, stiffness = 450f),
        label = "secondaryScale"
    )
    val secondaryBubbleWidth by animateDpAsState(
        targetValue = if (secondaryIsPill) miniPillWidth else circleSize,
        animationSpec = spring(dampingRatio = 0.65f, stiffness = 450f),
        label = "secondaryBubbleWidth"
    )
    val secondaryPillProgress = (miniPillWidth - circleSize).value.let { widthDelta ->
        if (widthDelta == 0f) {
            if (secondaryIsPill) 1f else 0f
        } else {
            ((secondaryBubbleWidth - circleSize).value / widthDelta).coerceIn(0f, 1f)
        }
    }
    val secondaryBubbleCorner by animateDpAsState(
        targetValue = if (secondaryIsPill) settings.cornerRadius.dp else circleSize / 2f,
        animationSpec = spring(dampingRatio = 0.65f, stiffness = 450f),
        label = "secondaryBubbleCorner"
    )
    val tertiaryAlpha by animateFloatAsState(
        targetValue = if (showTertiaryPill && !isHiding) 1f else 0f,
        animationSpec = tween(durationMillis = 180, easing = FastOutSlowInEasing),
        label = "tertiaryAlpha"
    )
    val tertiaryScale by animateFloatAsState(
        targetValue = if (showTertiaryPill && !isHiding) 1f else 0.3f,
        animationSpec = spring(dampingRatio = 0.65f, stiffness = 450f),
        label = "tertiaryScale"
    )

    val expandedCompactX = collapsedMainLeft
    val secondaryOffset by animateDpAsState(
        targetValue = when {
            secondaryIsPill -> if (isFullWidth) settings.xOffset.dp else 0.dp
            else -> if (isFullWidth) {
                settings.xOffset.dp - (miniPillWidth / 2f + compactGap + circleSize / 2f)
            } else {
                -((miniPillWidth + compactGap) / 2f)
            }
        },
        animationSpec = spring(dampingRatio = 0.75f, stiffness = 520f),
        label = "secondaryOffset"
    )

    // Outer Box: Fills the entire WindowManager window bounds (which are padded for easy touch)
    val outerModifier = if (currentExpanded) {
        modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures {
                    if (isInputActive) {
                        onReplyStateChanged(false)
                    }
                    currentOnToggle()
                }
            }

    } else {
        modifier.fillMaxSize()
    }

    Box(
        modifier = outerModifier,
        contentAlignment = Alignment.TopCenter
    ) {

        // Invisible touch target over the pill location when hiding, so tapping the area reveals the pill or opens shortcuts
        if (isHiding && !currentExpanded) {
            Box(
                modifier = Modifier
                    .width(miniPillWidth)
                    .height(settings.height.dp)
                    .graphicsLayer {
                        translationX = collapsedMainOffset.toPx()
                        translationY = settings.yOffset.dp.toPx()
                    }
                    .pointerInput(Unit) {
                        detectTapGestures {
                            if (settings.autoHidePill && isAutoHidden) {
                                // First tap on auto-hidden pill: awaken and reveal the pill
                                isAutoHidden = false
                                userInteractionTimestamp = System.currentTimeMillis()
                            } else {
                                // Empty notifications idle hiding: expand favorite shortcuts
                                currentOnToggle()
                            }
                        }
                    }
            )
        }

        // Match the narrow WindowManager touch region so nearby controls remain clickable.
        if (!currentExpanded && !isHiding) {
            val touchWidth = miniPillWidth + 8.dp
            val touchHeight = settings.yOffset.dp + settings.height.dp + 4.dp
            Box(
                modifier = Modifier
                    .width(touchWidth)
                    .height(touchHeight)
                    .graphicsLayer {
                        translationX = collapsedMainOffset.toPx()
                    }
                    .pointerInput(displayMetrics.density, isInputActive) {
                        if (isInputActive) return@pointerInput
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            userInteractionTimestamp = System.currentTimeMillis()
                            val startPos = down.position
                            val pointerId = down.id
                            var lastPos = startPos
                            var isHoldRegistered = false

                            val holdJob = scope.launch {
                                kotlinx.coroutines.delay(HOLD_GESTURE_THRESHOLD_MS)
                                isHoldRegistered = true
                                triggerHapticVibration(context)
                            }

                            while (true) {
                                val event = awaitPointerEvent(androidx.compose.ui.input.pointer.PointerEventPass.Initial)
                                val change = event.changes.firstOrNull { it.id == pointerId } ?: break
                                lastPos = change.position

                                val curDeltaX = lastPos.x - startPos.x
                                val curDeltaY = lastPos.y - startPos.y

                                if (abs(curDeltaX) > 8f || abs(curDeltaY) > 8f) {
                                    holdJob.cancel()
                                }

                                if (change.changedToUp() || !change.pressed) {
                                    change.consume()
                                    holdJob.cancel()

                                    val density = displayMetrics.density
                                    val swipeThreshold = 12f * density
                                    val tapThreshold = 10f * density

                                    val deltaX = lastPos.x - startPos.x
                                    val deltaY = lastPos.y - startPos.y
                                    val absDeltaX = abs(deltaX)
                                    val absDeltaY = abs(deltaY)
                                    val isHorizontalSwipe = absDeltaX > absDeltaY && absDeltaX > swipeThreshold
                                    val isTap = absDeltaX < tapThreshold && absDeltaY < tapThreshold

                                    val isFitnessMode = activeMode == IslandMode.Fitness
                                    val fitnessRepo = SmartIslandRepositories.fitnessRepository(context)

                                    if (isFitnessMode && isHorizontalSwipe) {
                                        triggerHapticVibration(context)
                                        if (deltaX < 0) {
                                            fitnessRepo.nextSet()
                                        } else {
                                            fitnessRepo.undoOrPreviousSet()
                                        }
                                    } else if (activeMode == IslandMode.Music && isHorizontalSwipe) {
                                        triggerHapticVibration(context)
                                        val currentNotif = notifications.getOrNull(safeIndex)
                                        val audioManager = context.getSystemService(android.content.Context.AUDIO_SERVICE) as? android.media.AudioManager
                                        if (deltaX < 0) {
                                            val nextAction = currentNotif?.actionIntents?.find {
                                                val t = it.title.lowercase()
                                                t.contains("下") || t.contains("next") || t.contains("skip")
                                            }
                                            if (nextAction?.pendingIntent != null) {
                                                try { nextAction.pendingIntent.send() } catch (e: Exception) {}
                                            } else {
                                                audioManager?.dispatchMediaKeyEvent(android.view.KeyEvent(android.view.KeyEvent.ACTION_DOWN, android.view.KeyEvent.KEYCODE_MEDIA_NEXT))
                                                audioManager?.dispatchMediaKeyEvent(android.view.KeyEvent(android.view.KeyEvent.ACTION_UP, android.view.KeyEvent.KEYCODE_MEDIA_NEXT))
                                            }
                                        } else {
                                            val prevAction = currentNotif?.actionIntents?.find {
                                                val t = it.title.lowercase()
                                                t.contains("上") || t.contains("prev") || t.contains("back")
                                            }
                                            if (prevAction?.pendingIntent != null) {
                                                try { prevAction.pendingIntent.send() } catch (e: Exception) {}
                                            } else {
                                                audioManager?.dispatchMediaKeyEvent(android.view.KeyEvent(android.view.KeyEvent.ACTION_DOWN, android.view.KeyEvent.KEYCODE_MEDIA_PREVIOUS))
                                                audioManager?.dispatchMediaKeyEvent(android.view.KeyEvent(android.view.KeyEvent.ACTION_UP, android.view.KeyEvent.KEYCODE_MEDIA_PREVIOUS))
                                            }
                                        }
                                    } else if (notifications.size > 1 && isHorizontalSwipe) {
                                        triggerHapticVibration(context)
                                        if (deltaX < 0) {
                                            onPageSelected((selectedIndex + 1) % notifications.size)
                                        } else {
                                            onPageSelected((selectedIndex - 1 + notifications.size) % notifications.size)
                                        }
                                    } else if (isHorizontalSwipe) {
                                        triggerHapticVibration(context)
                                        currentOnDismiss()
                                    } else if (isTap) {
                                        if (activeMode == IslandMode.Empty) {
                                            triggerHapticVibration(context)
                                            try {
                                                val launchIntent = android.content.Intent(context, com.agupta07505.smartisland.MainActivity::class.java).apply {
                                                    flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP
                                                }
                                                context.startActivity(launchIntent)
                                            } catch (e: Exception) {
                                                android.util.Log.e("IslandOverlayView", "Failed to launch MainActivity", e)
                                            }
                                        } else {
                                            currentOnToggle()
                                        }
                                    } else if (deltaY > swipeThreshold) {
                                        currentOnToggle()
                                    } else if (deltaY < -swipeThreshold) {
                                        if (isFitnessMode) {
                                            fitnessRepo.cancelRestTimer()
                                        } else {
                                            currentOnDismiss()
                                        }
                                    }
                                    break
                                } else if (!change.pressed) {
                                    holdJob.cancel()
                                    break
                                }
                            }
                            holdJob.cancel()
                        }
                    }
            )
        }

        // Inner Box: The actual visible pill container, managing the black background shape and size animations
        Box(
            modifier = Modifier
                .width(safeWidth)
                .height(safeHeight)
                .graphicsLayer {
                    translationX = animatedXOffset.toPx()
                    translationY = yOffset.toPx() + dragOffset
                    scaleX = switchScaleAnim.value
                    scaleY = switchScaleAnim.value
                }
                .then(
                    if (settings.enableShadow && currentExpanded && !isHiding) {
                        Modifier.shadow(
                            elevation = 16.dp,
                            shape = RoundedCornerShape(safeRadius),
                            clip = false,
                            ambientColor = Color.Black,
                            spotColor = Color.Black
                        )
                    } else Modifier
                )
                .then(
                    if (currentExpanded) {
                        Modifier.pointerInput(displayMetrics.density, isInputActive) {
                            if (isInputActive) return@pointerInput
                            awaitEachGesture {
                                val down = awaitFirstDown(requireUnconsumed = false)
                                userInteractionTimestamp = System.currentTimeMillis()
                                val startPos = down.position
                                val pointerId = down.id
                                var lastPos = startPos
                                var isHoldRegistered = false
                                var isDragging = false

                                val holdJob = scope.launch {
                                    kotlinx.coroutines.delay(HOLD_GESTURE_THRESHOLD_MS)
                                    isHoldRegistered = true
                                    triggerHapticVibration(context)
                                }

                                while (true) {
                                    val event = awaitPointerEvent(androidx.compose.ui.input.pointer.PointerEventPass.Initial)
                                    val change = event.changes.firstOrNull { it.id == pointerId } ?: break
                                    lastPos = change.position

                                    val curDeltaX = lastPos.x - startPos.x
                                    val curDeltaY = lastPos.y - startPos.y

                                    if (abs(curDeltaX) > 8f || abs(curDeltaY) > 8f) {
                                        holdJob.cancel()
                                        isDragging = true
                                    }

                                    if (isDragging) {
                                        dragOffset = curDeltaY.coerceIn(
                                            -DRAG_MAX_OFFSET_DP * displayMetrics.density,
                                            DRAG_MAX_OFFSET_DP * displayMetrics.density
                                        )
                                    }

                                    if (change.changedToUp() || !change.pressed) {
                                        val wasConsumedByChild = change.isConsumed
                                        change.consume()
                                        holdJob.cancel()

                                        val density = displayMetrics.density
                                        val swipeThreshold = 12f * density
                                        val tapThreshold = 10f * density

                                        val deltaX = lastPos.x - startPos.x
                                        val deltaY = lastPos.y - startPos.y
                                        val absDeltaX = abs(deltaX)
                                        val absDeltaY = abs(deltaY)

                                        if (deltaY < -swipeThreshold) {
                                            // Swipe Up when expanded -> Collapse
                                            currentOnToggle()
                                        } else if (absDeltaX < tapThreshold && absDeltaY < tapThreshold && !wasConsumedByChild) {
                                            // Tap when expanded
                                            if (!isHoldRegistered) {
                                                if (activeMode == IslandMode.Fitness) {
                                                    val fitnessRepo = SmartIslandRepositories.fitnessRepository(context)
                                                    val fState = fitnessRepo.sessionState.value
                                                    val videoUrl = fState.currentExercise?.videoUrl?.trim()
                                                    if (!videoUrl.isNullOrBlank()) {
                                                        try {
                                                            val intent = android.content.Intent(
                                                                android.content.Intent.ACTION_VIEW,
                                                                android.net.Uri.parse(videoUrl)
                                                            ).apply {
                                                                flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK
                                                            }
                                                            context.startActivity(intent)
                                                        } catch (e: Exception) {
                                                            android.widget.Toast.makeText(context, "无法唤醒视频 App", android.widget.Toast.LENGTH_SHORT).show()
                                                        }
                                                    } else {
                                                        android.widget.Toast.makeText(context, "当前动作暂无视频链接", android.widget.Toast.LENGTH_SHORT).show()
                                                    }
                                                } else {
                                                    val currentNotification = notifications.getOrNull(safeIndex)
                                                    if (currentNotification != null &&
                                                        currentNotification.mode != IslandMode.Battery &&
                                                        currentNotification.mode != IslandMode.Timer
                                                    ) {
                                                        currentOnOpenNotification(currentNotification)
                                                    }
                                                }
                                            }
                                        }
                                        break
                                    } else if (!change.pressed) {
                                        holdJob.cancel()
                                        break
                                    }
                                }

                                holdJob.cancel()
                                if (dragOffset != 0f) {
                                    scope.launch {
                                        androidx.compose.animation.core.Animatable(dragOffset).animateTo(
                                            targetValue = 0f,
                                            animationSpec = spring(
                                                dampingRatio = Spring.DampingRatioNoBouncy,
                                                stiffness = Spring.StiffnessMedium
                                            )
                                        ) {
                                            dragOffset = value
                                        }
                                    }
                                }
                            }
                        }
                    } else Modifier
                )
                .clip(RoundedCornerShape(safeRadius))
                .background(Color.Black.copy(alpha = settings.opacity)),
            contentAlignment = Alignment.TopCenter
        ) {
            // Collapsed content layer (pinned to fixed pill bounds at top-center, cancelling yOffset)
            if (collapsedAlpha > 0f) {
                Box(
                    modifier = Modifier
                        .width(miniPillWidth)
                        .height(settings.height.dp)
                        .align(Alignment.TopCenter)
                        .graphicsLayer {
                            alpha = collapsedAlpha
                        }
                ) {
                    IslandCollapsedContent(
                        mode = activeMode,
                        notification = activeNotification,
                        collapsedAlpha = collapsedAlpha,
                        settings = settings
                    )
                }
            }

            // Expanded content layer — smoothly fade out while collapsing
            if (expanded || expandedAlpha > 0.01f) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight()
                        .graphicsLayer {
                            alpha = expandedAlpha
                            scaleX = contentScale
                            scaleY = contentScale
                            translationY = contentSlideY.toPx()
                        }
                ) {
                    IslandExpandedContent(
                        notifications = notifications,
                        launcherApps = launcherApps,
                        selectedIndex = selectedIndex,
                        onPageSelected = onPageSelected,
                        onOpenNotification = onOpenNotification,
                        onLaunchApp = onLaunchApp,
                        onCollapse = onToggleExpanded,
                        statusBarHeight = statusBarHeight.dp,
                        // Each mode owns its natural height. The launcher already
                        // supplies its own loading height and must not impose that
                        // minimum on compact call or battery content.
                        onHeightMeasured = { expandedHeight = it },
                        settings = settings,
                        onReplyStateChanged = onReplyStateChanged
                    )
                }
            }
        }

        // Collapsed: secondary circle. Expanded with 2: the same item morphs
        // into a full-size pill. Expanded with 3+: it stays the circle on the right.
        if (secondaryAlpha > 0f && secondaryNotification != null) {
            Box(
                modifier = Modifier
                    .absoluteOffset {
                        IntOffset(
                            secondaryOffset.roundToPx(),
                            settings.yOffset.dp.roundToPx()
                        )
                    }
                    .width(secondaryBubbleWidth)
                    .height(circleSize)
                    .graphicsLayer {
                        alpha = secondaryAlpha
                        scaleX = secondaryScale * switchScaleAnim.value
                        scaleY = secondaryScale * switchScaleAnim.value
                    }
                    .then(
                        if (settings.enableShadow && currentExpanded) {
                            Modifier.shadow(
                                elevation = 8.dp,
                                shape = RoundedCornerShape(secondaryBubbleCorner),
                                clip = false,
                                ambientColor = Color.Black,
                                spotColor = Color.Black
                            )
                        } else Modifier
                    )
                    .clip(RoundedCornerShape(secondaryBubbleCorner))
                    .background(Color.Black.copy(alpha = settings.opacity))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        if (secondaryIndex >= 0) {
                            onPageSelected(secondaryIndex)
                        }
                        if (!currentExpanded) {
                            currentOnToggle()
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer { alpha = 1f - secondaryPillProgress },
                    contentAlignment = Alignment.Center
                ) {
                    SecondaryBubbleContent(
                        notification = secondaryNotification,
                        settings = settings
                    )
                }
                Box(
                    modifier = Modifier
                        .requiredWidth(miniPillWidth)
                        .height(circleSize)
                        .graphicsLayer { alpha = secondaryPillProgress },
                    contentAlignment = Alignment.Center
                ) {
                    IslandCollapsedContent(
                        mode = secondaryNotification.mode,
                        notification = secondaryNotification,
                        collapsedAlpha = 1f,
                        settings = settings
                    )
                }
            }
        }

        if (tertiaryAlpha > 0f && tertiaryNotification != null) {
            Box(
                modifier = Modifier
                    .absoluteOffset {
                        IntOffset(
                            (expandedCompactX - screenCenter + miniPillWidth / 2f).roundToPx(),
                            settings.yOffset.dp.roundToPx()
                        )
                    }
                    .width(miniPillWidth)
                    .height(circleSize)
                    .graphicsLayer {
                        alpha = tertiaryAlpha
                        scaleX = tertiaryScale * switchScaleAnim.value
                        scaleY = tertiaryScale * switchScaleAnim.value
                    }
                    .clip(RoundedCornerShape(settings.cornerRadius.dp))
                    .background(Color.Black.copy(alpha = settings.opacity))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        if (tertiaryIndex >= 0) {
                            onPageSelected(tertiaryIndex)
                        }
                        if (!currentExpanded) {
                            currentOnToggle()
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                IslandCollapsedContent(
                    mode = tertiaryNotification.mode,
                    notification = tertiaryNotification,
                    collapsedAlpha = 1f,
                    settings = settings
                )
            }
        }
    }
}

@Composable
private fun SecondaryBubbleContent(
    notification: IslandNotification,
    settings: SmartIslandSettings
) {
    when (notification.mode) {
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
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFF59E0B).copy(alpha = 0.25f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Rounded.FlashlightOn,
                    contentDescription = "Flashlight",
                    tint = Color(0xFFFACC15),
                    modifier = Modifier.size(12.dp)
                )
            }
        }
        IslandMode.Hotspot -> {
            HotspotCollapsedGlyph(notification = notification, settings = settings)
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
        IslandMode.IncomingCall -> {
            val icon = notification.largeIcon ?: notification.icon
            if (icon != null) {
                Image(
                    bitmap = icon.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier
                        .size(20.dp)
                        .clip(CircleShape)
                )
            } else {
                Icon(
                    Icons.Rounded.Call,
                    contentDescription = null,
                    tint = Color(settings.callColor),
                    modifier = Modifier.size(14.dp)
                )
            }
        }
        IslandMode.Music -> {
            val artwork = notification.largeIcon ?: notification.icon
            if (artwork != null) {
                Image(
                    bitmap = artwork.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier
                        .size(20.dp)
                        .clip(CircleShape)
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(Color(settings.musicVisualizerColor)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Rounded.MusicNote,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(12.dp)
                    )
                }
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
        IslandMode.Notification, IslandMode.DownloadUpload, IslandMode.Empty -> {
            NotificationGlyph(notification = notification, settings = settings)
        }
    }
}

// Animation specs
private const val EXPANDED_WIDTH_RATIO = 0.95f
private const val SWIPE_THRESHOLD_DP = 35f
private const val DRAG_MAX_OFFSET_DP = 100f
private const val COMPACT_INDICATOR_GAP_DP = 8f
private const val HOLD_GESTURE_THRESHOLD_MS = 300L

private fun triggerHapticVibration(context: android.content.Context) {
    runCatching {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            val vm = context.getSystemService(android.content.Context.VIBRATOR_MANAGER_SERVICE) as? android.os.VibratorManager
            val vibrator = vm?.defaultVibrator
            if (vibrator?.hasVibrator() == true) {
                vibrator.vibrate(android.os.VibrationEffect.createOneShot(60L, android.os.VibrationEffect.DEFAULT_AMPLITUDE))
                return
            }
        }
        @Suppress("DEPRECATION")
        val vibrator = context.getSystemService(android.content.Context.VIBRATOR_SERVICE) as? android.os.Vibrator
        if (vibrator?.hasVibrator() == true) {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                vibrator.vibrate(android.os.VibrationEffect.createOneShot(60L, android.os.VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(60L)
            }
        }
    }
}

fun playNeteaseDailyRecommend(context: android.content.Context) {
    try {
        val uri = android.net.Uri.parse("orpheus://songrcmd?autoplay=1")
        val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, uri).apply {
            flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP
            `package` = "com.netease.cloudmusic"
        }
        context.startActivity(intent)
    } catch (e: Exception) {
        try {
            val widgetUri = android.net.Uri.parse("orpheuswidget://songrcmd?autoplay=1")
            val widgetIntent = android.content.Intent(android.content.Intent.ACTION_VIEW, widgetUri).apply {
                flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK
                `package` = "com.netease.cloudmusic"
            }
            context.startActivity(widgetIntent)
        } catch (e2: Exception) {
            try {
                val launchIntent = context.packageManager.getLaunchIntentForPackage("com.netease.cloudmusic")
                if (launchIntent != null) {
                    launchIntent.flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK
                    context.startActivity(launchIntent)
                } else {
                    android.widget.Toast.makeText(context, "请先安装网易云音乐 App", android.widget.Toast.LENGTH_SHORT).show()
                }
            } catch (e3: Exception) {
                android.widget.Toast.makeText(context, "启动网易云音乐失败: ${e3.message}", android.widget.Toast.LENGTH_SHORT).show()
            }
        }
    }
}

internal enum class CompactNotificationShape { MiniPill, Circle }

internal fun defaultEstimatedHeightForMode(mode: IslandMode?): Dp {
    return when (mode) {
        IslandMode.Music -> 175.dp
        IslandMode.Notification -> 135.dp
        IslandMode.IncomingCall, IslandMode.Battery -> 115.dp
        IslandMode.LiveActivity, IslandMode.Navigation -> 180.dp
        IslandMode.DownloadUpload, IslandMode.Hotspot -> 160.dp
        IslandMode.Bluetooth, IslandMode.Flashlight, IslandMode.ScreenRecording,
        IslandMode.Timer, IslandMode.Stopwatch -> 115.dp
        IslandMode.Fitness -> 130.dp
        IslandMode.Empty, null -> 135.dp
    }
}

internal fun compactNotificationShapes(
    notificationCount: Int,
    expanded: Boolean
): List<CompactNotificationShape> = when {
    notificationCount < 2 -> emptyList()
    !expanded -> listOf(CompactNotificationShape.Circle)
    notificationCount == 2 -> listOf(CompactNotificationShape.MiniPill)
    else -> listOf(CompactNotificationShape.MiniPill, CompactNotificationShape.Circle)
}
