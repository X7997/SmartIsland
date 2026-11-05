/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 */

package com.agupta07505.smartisland.ui.components

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GymMuscleAnatomyTest {

    @Test
    fun testExerciseMatchingToTargetMuscle() {
        // 1. 腿部动作匹配
        assertEquals(TargetMuscle.QUADS, MuscleMatcher.match("坐姿器械腿屈伸", "臀腿"))
        assertEquals(TargetMuscle.QUADS, MuscleMatcher.match("杠铃深蹲", "腿"))
        assertEquals(TargetMuscle.QUADS, MuscleMatcher.match("45度倒蹬", "臀腿"))
        assertEquals(TargetMuscle.HAMSTRINGS, MuscleMatcher.match("俯卧腿弯举", "臀腿"))
        assertEquals(TargetMuscle.GLUTE_MAXIMUS, MuscleMatcher.match("史密斯机臀推", "臀腿"))
        assertEquals(TargetMuscle.GLUTE_MEDIUS, MuscleMatcher.match("坐姿髋外展", "臀腿"))
        assertEquals(TargetMuscle.ADDUCTORS, MuscleMatcher.match("坐姿髋内收", "臀腿"))
        assertEquals(TargetMuscle.CALVES, MuscleMatcher.match("站姿提踵", "臀腿"))

        // 2. 胸部动作匹配
        assertEquals(TargetMuscle.CHEST_MAJOR, MuscleMatcher.match("坐姿器械推胸", "胸"))
        assertEquals(TargetMuscle.CHEST_MAJOR, MuscleMatcher.match("蝴蝶机夹胸", "胸"))
        assertEquals(TargetMuscle.UPPER_CHEST, MuscleMatcher.match("史密斯上斜推胸", "胸"))

        // 3. 背部动作匹配
        assertEquals(TargetMuscle.LAT_DORSI, MuscleMatcher.match("高位下拉", "背"))
        assertEquals(TargetMuscle.LAT_DORSI, MuscleMatcher.match("引体向上", "背"))
        assertEquals(TargetMuscle.MID_BACK, MuscleMatcher.match("坐姿划船", "背"))
        assertEquals(TargetMuscle.DELTOIDS_REAR, MuscleMatcher.match("绳索面拉", "背"))

        // 4. 肩部动作匹配
        assertEquals(TargetMuscle.DELTOIDS, MuscleMatcher.match("坐姿推举", "肩"))
        assertEquals(TargetMuscle.DELTOIDS_LATERAL, MuscleMatcher.match("哑铃侧平举", "肩"))
        assertEquals(TargetMuscle.DELTOIDS_REAR, MuscleMatcher.match("反向飞鸟", "肩"))

        // 5. 核心与手臂匹配
        assertEquals(TargetMuscle.ABS_RECTUS, MuscleMatcher.match("平板支撑", "腹"))
        assertEquals(TargetMuscle.LOWER_ABS, MuscleMatcher.match("悬垂屈腿", "腹"))
        assertEquals(TargetMuscle.BICEPS, MuscleMatcher.match("哑铃弯举", "手臂"))
        assertEquals(TargetMuscle.TRICEPS, MuscleMatcher.match("绳索下压", "手臂"))
    }

    @Test
    fun testIsMuscleInCategory() {
        // "臀腿" 分类应包含臀部、大腿、小腿
        assertTrue(MuscleMatcher.isMuscleInCategory(TargetMuscle.QUADS, "臀腿"))
        assertTrue(MuscleMatcher.isMuscleInCategory(TargetMuscle.HAMSTRINGS, "臀腿"))
        assertTrue(MuscleMatcher.isMuscleInCategory(TargetMuscle.ADDUCTORS, "臀腿"))
        assertTrue(MuscleMatcher.isMuscleInCategory(TargetMuscle.GLUTE_MAXIMUS, "臀腿"))
        assertTrue(MuscleMatcher.isMuscleInCategory(TargetMuscle.GLUTE_MEDIUS, "臀腿"))
        assertTrue(MuscleMatcher.isMuscleInCategory(TargetMuscle.CALVES, "臀腿"))
        // 但不包含胸、背、肩、腹
        assertFalse(MuscleMatcher.isMuscleInCategory(TargetMuscle.CHEST_MAJOR, "臀腿"))
        assertFalse(MuscleMatcher.isMuscleInCategory(TargetMuscle.LAT_DORSI, "臀腿"))
        assertFalse(MuscleMatcher.isMuscleInCategory(TargetMuscle.ABS_RECTUS, "臀腿"))
        assertFalse(MuscleMatcher.isMuscleInCategory(TargetMuscle.DELTOIDS, "臀腿"))

        // "胸" 分类仅包含胸部肌群
        assertTrue(MuscleMatcher.isMuscleInCategory(TargetMuscle.CHEST_MAJOR, "胸"))
        assertTrue(MuscleMatcher.isMuscleInCategory(TargetMuscle.UPPER_CHEST, "胸"))
        assertFalse(MuscleMatcher.isMuscleInCategory(TargetMuscle.QUADS, "胸"))
        assertFalse(MuscleMatcher.isMuscleInCategory(TargetMuscle.LAT_DORSI, "胸"))

        // "背" 分类包含背部各肌群
        assertTrue(MuscleMatcher.isMuscleInCategory(TargetMuscle.LAT_DORSI, "背"))
        assertTrue(MuscleMatcher.isMuscleInCategory(TargetMuscle.MID_BACK, "背"))
        assertTrue(MuscleMatcher.isMuscleInCategory(TargetMuscle.TRAPEZIUS, "背"))
        assertTrue(MuscleMatcher.isMuscleInCategory(TargetMuscle.LOWER_BACK, "背"))
        assertFalse(MuscleMatcher.isMuscleInCategory(TargetMuscle.CHEST_MAJOR, "背"))

        // 空分类或未开启训练应返回 false
        assertFalse(MuscleMatcher.isMuscleInCategory(TargetMuscle.QUADS, ""))
        assertFalse(MuscleMatcher.isMuscleInCategory(TargetMuscle.QUADS, "   "))
    }

    @Test
    fun testThreeVisualStatesLogic() {
        // 模拟做 "坐姿器械腿屈伸"（目标肌群: 股四头肌，所属大分类: 臀腿）
        val category = "臀腿"
        val activeTarget = TargetMuscle.QUADS

        fun getExpectedState(m: TargetMuscle): MuscleVisualState {
            return when {
                m == activeTarget -> MuscleVisualState.ACTIVE_FLASHING
                MuscleMatcher.isMuscleInCategory(m, category) -> MuscleVisualState.CATEGORY_FOCUSED
                else -> MuscleVisualState.DEFAULT
            }
        }

        // 1. 当前动作核心肌肉 -> 动作训练态（闪烁红）
        assertEquals(MuscleVisualState.ACTIVE_FLASHING, getExpectedState(TargetMuscle.QUADS))

        // 2. 所属部位的其他肌肉 -> 部位训练态（金属灰）
        assertEquals(MuscleVisualState.CATEGORY_FOCUSED, getExpectedState(TargetMuscle.HAMSTRINGS))
        assertEquals(MuscleVisualState.CATEGORY_FOCUSED, getExpectedState(TargetMuscle.GLUTE_MAXIMUS))
        assertEquals(MuscleVisualState.CATEGORY_FOCUSED, getExpectedState(TargetMuscle.ADDUCTORS))
        assertEquals(MuscleVisualState.CATEGORY_FOCUSED, getExpectedState(TargetMuscle.CALVES))

        // 3. 非今日训练部位的肌肉 -> 默认态（白色描边，透明底）
        assertEquals(MuscleVisualState.DEFAULT, getExpectedState(TargetMuscle.CHEST_MAJOR))
        assertEquals(MuscleVisualState.DEFAULT, getExpectedState(TargetMuscle.LAT_DORSI))
        assertEquals(MuscleVisualState.DEFAULT, getExpectedState(TargetMuscle.DELTOIDS))
        assertEquals(MuscleVisualState.DEFAULT, getExpectedState(TargetMuscle.ABS_RECTUS))
        assertEquals(MuscleVisualState.DEFAULT, getExpectedState(TargetMuscle.BICEPS))
    }

    @Test
    fun testDefaultIdleStateLogic() {
        // 未进行任何训练时（默认空闲状态）
        val category = ""
        val activeTarget: TargetMuscle? = null

        fun getExpectedState(m: TargetMuscle): MuscleVisualState {
            return when {
                activeTarget != null && m == activeTarget -> MuscleVisualState.ACTIVE_FLASHING
                MuscleMatcher.isMuscleInCategory(m, category) -> MuscleVisualState.CATEGORY_FOCUSED
                else -> MuscleVisualState.DEFAULT
            }
        }

        // 所有肌群必须全部是 DEFAULT 默认态（白色描边）
        TargetMuscle.values().forEach { muscle ->
            assertEquals(MuscleVisualState.DEFAULT, getExpectedState(muscle))
        }
    }
}
