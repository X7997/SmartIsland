package com.agupta07505.smartisland.service

import kotlin.math.ceil
import kotlin.math.floor

/** Screen coordinates for the collapsed island and its narrow top-edge gesture corridor. */
internal data class CollapsedTouchBounds(
    val left: Int,
    val top: Int,
    val right: Int,
    val bottom: Int
) {
    val width: Int get() = right - left
}

internal fun collapsedTouchBounds(
    screenWidthPx: Int,
    density: Float,
    mainWidthDp: Float,
    pillHeightDp: Float,
    xOffsetDp: Float,
    yOffsetDp: Float,
    hasSecondary: Boolean
): CollapsedTouchBounds {
    val mainWidthPx = mainWidthDp * density
    val secondaryWidthPx = if (hasSecondary) (8f + pillHeightDp) * density else 0f
    val edgePaddingPx = 8f * density
    val desiredMainLeftPx = screenWidthPx / 2f + xOffsetDp * density - mainWidthPx / 2f
    val mainLeftPx = desiredMainLeftPx.coerceIn(
        edgePaddingPx,
        (screenWidthPx - mainWidthPx - edgePaddingPx).coerceAtLeast(edgePaddingPx)
    )
    val groupLeftPx = if (hasSecondary) (mainLeftPx - secondaryWidthPx).coerceAtLeast(0f) else mainLeftPx
    val touchPaddingPx = 4f * density
    return CollapsedTouchBounds(
        left = floor(groupLeftPx - touchPaddingPx).toInt().coerceAtLeast(0),
        top = 0,
        right = ceil(mainLeftPx + mainWidthPx + touchPaddingPx).toInt().coerceAtMost(screenWidthPx),
        bottom = ceil((yOffsetDp + pillHeightDp + 4f) * density).toInt().coerceAtLeast(1)
    )
}
