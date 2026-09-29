/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland

import com.agupta07505.smartisland.ui.CompactNotificationShape
import com.agupta07505.smartisland.ui.compactNotificationShapes
import com.agupta07505.smartisland.service.collapsedTouchBounds
import org.junit.Assert.assertEquals
import org.junit.Test

class IslandOverlayLayoutTest {

    @Test
    fun collapsedTouchBoundsLeaveNearbyControlsAndLowerScreenTouchable() {
        val single = collapsedTouchBounds(
            screenWidthPx = 1080, density = 3f, mainWidthDp = 144f,
            pillHeightDp = 32f, xOffsetDp = 0f, yOffsetDp = 7f,
            hasSecondary = false
        )
        assertEquals(432 + 24, single.width) // visible pill plus 4dp on each side
        assertEquals(768, single.right)
        assertEquals(0, single.top) // keep the top-edge gesture corridor
        assertEquals(129, single.bottom) // 7dp offset + 32dp pill + 4dp margin
        org.junit.Assert.assertTrue(540 in single.left until single.right)
        org.junit.Assert.assertTrue(770 !in single.left until single.right)
        org.junit.Assert.assertTrue(145 >= single.bottom)

        val split = collapsedTouchBounds(
            screenWidthPx = 1080, density = 3f, mainWidthDp = 144f,
            pillHeightDp = 32f, xOffsetDp = 0f, yOffsetDp = 7f,
            hasSecondary = true
        )
        assertEquals(single.right, split.right)
        assertEquals(single.left - 120, split.left) // secondary circle plus 8dp gap, on the left
    }

    @Test
    fun compactIndicatorsMatchNotificationMatrix() {
        assertEquals(
            listOf(CompactNotificationShape.Circle),
            compactNotificationShapes(notificationCount = 2, expanded = false)
        )
        assertEquals(
            listOf(CompactNotificationShape.MiniPill),
            compactNotificationShapes(notificationCount = 2, expanded = true)
        )
        assertEquals(
            listOf(CompactNotificationShape.Circle),
            compactNotificationShapes(notificationCount = 3, expanded = false)
        )
        assertEquals(
            listOf(CompactNotificationShape.MiniPill, CompactNotificationShape.Circle),
            compactNotificationShapes(notificationCount = 3, expanded = true)
        )
    }

    @Test
    fun mainIslandPhysicalCenterRemainsStrictlyCentered() {
        val density = 2.75f
        val screenWidthPx = 1080f
        val widthDp = 180f
        val heightDp = 36f
        val compactGapDp = 8f

        listOf(-50f, 0f, 50f).forEach { xOffsetDp ->
            val mainWidthPx = widthDp * density
            val circleSizePx = heightDp * density
            val compactGapPx = compactGapDp * density

            // 1. Single island mode:
            val singleMainCenterPx = screenWidthPx / 2f + xOffsetDp * density

            // 2. Split mode with wrap_content window:
            val desiredMainLeftPx = screenWidthPx / 2f + xOffsetDp * density - mainWidthPx / 2f
            val secondaryWidthPx = compactGapPx + circleSizePx
            val groupLeftPx = desiredMainLeftPx - secondaryWidthPx
            val groupRightPx = desiredMainLeftPx + mainWidthPx
            val groupCenterPx = (groupLeftPx + groupRightPx) / 2f
            val windowXPx = (groupCenterPx - screenWidthPx / 2f).toInt()

            val windowCenterPx = screenWidthPx / 2f + windowXPx
            val composeCompensationPx = (compactGapPx + circleSizePx) / 2f
            val splitMainCenterPx = windowCenterPx + composeCompensationPx

            // Verify main center is unchanged (within 1px integer rounding)
            org.junit.Assert.assertEquals(
                "Main island center must stay identical in split mode",
                singleMainCenterPx,
                splitMainCenterPx,
                1.0f
            )
        }
    }

    @Test
    fun secondaryCircleIsStrictlyOnTheLeft() {
        val density = 2.75f
        val screenWidthPx = 1080f
        val widthDp = 180f
        val heightDp = 36f
        val compactGapDp = 8f

        listOf(-50f, 0f, 50f).forEach { xOffsetDp ->
            val mainWidthPx = widthDp * density
            val circleSizePx = heightDp * density
            val compactGapPx = compactGapDp * density

            val mainCenterPx = screenWidthPx / 2f + xOffsetDp * density
            val mainLeftPx = mainCenterPx - mainWidthPx / 2f

            // Secondary island is unconditionally on the left
            val secondaryCenterPx = mainCenterPx - (mainWidthPx / 2f + compactGapPx + circleSizePx / 2f)
            val secondaryRightPx = secondaryCenterPx + circleSizePx / 2f

            org.junit.Assert.assertTrue(
                "Secondary bubble right edge must be to the left of main island",
                secondaryRightPx < mainLeftPx
            )
            org.junit.Assert.assertEquals(
                "Gap between secondary right edge and main left edge must equal compactGap",
                compactGapPx,
                mainLeftPx - secondaryRightPx,
                0.01f
            )
        }
    }
}
