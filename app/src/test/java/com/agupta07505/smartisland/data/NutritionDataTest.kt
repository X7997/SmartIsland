/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 */

package com.agupta07505.smartisland.data

import com.agupta07505.smartisland.data.nutrition.NutritionTextParser
import com.agupta07505.smartisland.data.nutrition.ScannedProductInfo
import com.agupta07505.smartisland.data.nutrition.WorkoutArchiveRecord
import com.agupta07505.smartisland.ui.components.MuscleCategory
import com.agupta07505.smartisland.ui.components.MuscleMatcher
import com.agupta07505.smartisland.ui.components.TargetMuscle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NutritionDataTest {

    @Test
    fun testScannedProductInfoJsonSerialization() {
        val original = ScannedProductInfo(
            id = "test_item_001",
            brand = "果子茶探",
            productName = "苹果茉莉果汁茶饮料",
            productType = "果汁茶饮料",
            ingredients = "水、果葡糖浆、浓缩苹果汁、茉莉花茶",
            servingSize = "每100mL",
            bottleCapacityMl = 500,
            energyKjPer100 = 131.0,
            energyKcalPer100 = 31.3,
            proteinPer100 = 0.0,
            fatPer100 = 0.0,
            saturatedFatPer100 = 0.0,
            carbsPer100 = 7.5,
            sugarPer100 = 7.5,
            sodiumMgPer100 = 19.0,
            totalEnergyKcal = 156.5,
            totalSugarG = 37.5,
            isSugarExceeded = true,
            healthEvaluation = "整瓶含糖37.5g已超标",
            micronutrientAdvice = "注意出汗与钾镁补充"
        )

        val jsonStr = original.toJson()
        val restored = ScannedProductInfo.fromJson(jsonStr)

        assertEquals(original.id, restored.id)
        assertEquals(original.brand, restored.brand)
        assertEquals(original.productName, restored.productName)
        assertEquals(original.bottleCapacityMl, restored.bottleCapacityMl)
        assertEquals(original.sugarPer100, restored.sugarPer100, 0.001)
        assertEquals(original.totalSugarG, restored.totalSugarG, 0.001)
        assertTrue(restored.isSugarExceeded)
        assertEquals(original.micronutrientAdvice, restored.micronutrientAdvice)
    }

    @Test
    fun testWorkoutArchiveRecordJsonSerialization() {
        val record = WorkoutArchiveRecord(
            id = "archive_123",
            categoryName = "臀腿",
            completedAt = 1761720000000L,
            durationSeconds = 2700,
            totalSets = 16,
            summary = "完成 臀腿 训练 · 共 4 个动作，16 组"
        )

        val json = record.toJson()
        val restored = WorkoutArchiveRecord.fromJson(json)

        assertEquals("archive_123", restored.id)
        assertEquals("臀腿", restored.categoryName)
        assertEquals(2700, restored.durationSeconds)
        assertEquals(16, restored.totalSets)
        assertEquals("完成 臀腿 训练 · 共 4 个动作，16 组", restored.summary)
    }

    @Test
    fun testMuscleCategoryMatchingForWeeklyMap() {
        // 验证周训练图中各部位映射与包含逻辑
        assertTrue(MuscleMatcher.isMuscleInCategory(TargetMuscle.QUADS, "臀腿"))
        assertTrue(MuscleMatcher.isMuscleInCategory(TargetMuscle.HAMSTRINGS, "臀腿"))
        assertTrue(MuscleMatcher.isMuscleInCategory(TargetMuscle.GLUTE_MAXIMUS, "臀腿"))
        assertTrue(MuscleMatcher.isMuscleInCategory(TargetMuscle.CALVES, "臀腿"))

        assertTrue(MuscleMatcher.isMuscleInCategory(TargetMuscle.ABS_RECTUS, "腹"))
        assertTrue(MuscleMatcher.isMuscleInCategory(TargetMuscle.OBLIQUES, "腹"))

        assertTrue(MuscleMatcher.isMuscleInCategory(TargetMuscle.LAT_DORSI, "背"))
        assertTrue(MuscleMatcher.isMuscleInCategory(TargetMuscle.MID_BACK, "背"))
        assertTrue(MuscleMatcher.isMuscleInCategory(TargetMuscle.TRAPEZIUS, "背"))

        assertTrue(MuscleMatcher.isMuscleInCategory(TargetMuscle.CHEST_MAJOR, "胸"))
        assertTrue(MuscleMatcher.isMuscleInCategory(TargetMuscle.DELTOIDS, "肩"))

        // 反向互斥校验
        assertFalse(MuscleMatcher.isMuscleInCategory(TargetMuscle.CHEST_MAJOR, "臀腿"))
        assertFalse(MuscleMatcher.isMuscleInCategory(TargetMuscle.QUADS, "胸"))
    }

    @Test
    fun testNutritionTextParserFromOcrText() {
        val ocrSample = """
            果子茶探
            苹果茉莉
            果汁茶饮料
            净含量：500ml
            配料：水、苹果浓缩汁、茉莉花茶、白砂糖、柠檬酸、维生素C
            营养成分表
            项目 每100mL 营养素参考值%
            能量 131kJ 2%
            蛋白质 0g 0%
            脂肪 0g 0%
            碳水化合物 7.5g 3%
            - 糖 7.5g
            钠 15mg 1%
        """.trimIndent()

        val parsed = NutritionTextParser.parseOcrText(ocrSample)

        assertEquals(500, parsed.bottleCapacityMl)
        assertEquals(131.0, parsed.energyKjPer100, 0.1)
        assertEquals(31.3, parsed.energyKcalPer100, 0.2)
        assertEquals(7.5, parsed.sugarPer100, 0.1)
        assertEquals(37.5, parsed.totalSugarG, 0.2)
        assertEquals(156.5, parsed.totalEnergyKcal, 0.5)
        assertTrue(parsed.isSugarExceeded)
        assertEquals(15.0, parsed.sodiumMgPer100, 0.1)
        assertTrue(parsed.healthEvaluation.contains("已超过"))
        assertTrue(parsed.productName.contains("苹果茉莉") || parsed.productName.contains("果子茶探"))
    }

    @Test
    fun testNutritionTextParserSugarFreeDrink() {
        val ocrSample = """
            东方树叶
            乌龙茶
            茶饮料
            净含量：500mL
            营养成分表
            项目 每100mL
            能量 0kJ
            蛋白质 0g
            脂肪 0g
            碳水化合物 0g
            钠 8mg
        """.trimIndent()

        val parsed = NutritionTextParser.parseOcrText(ocrSample)

        assertEquals(500, parsed.bottleCapacityMl)
        assertEquals(0.0, parsed.energyKcalPer100, 0.01)
        assertEquals(0.0, parsed.totalSugarG, 0.01)
        assertFalse(parsed.isSugarExceeded)
        assertEquals(8.0, parsed.sodiumMgPer100, 0.1)
        assertTrue(parsed.healthEvaluation.contains("0糖") || parsed.healthEvaluation.contains("适度"))
    }

    @Test
    fun testNutritionTextParserRejectsLicenseNumberAsSugar() {
        // 模拟真实用户手机拍摄的康师傅绿茶 OCR 原始半结构文本
        val ocrSample = """
            品名,康师傅 绿茶(调味茶饮品)
            配料表:水白砂糖、绿茶茶叶,茉莉花茶茶叶,食用香精
            食品生产许可证编号：SC10645011000117
            产品标准号：GB/T 21733
            净含量：500mL
            营养成分表
            项目 每100mL 营养素参考值%
            能量 78kJ 1%
            蛋白质 0g 0%
            脂肪 0g 0%
            碳水化合物 4.6g 2%
            - 糖 4.6g
            钠 12mg 1%
        """.trimIndent()

        val parsed = NutritionTextParser.parseOcrText(ocrSample)

        // 验证：许可证编号 SC10645011000117 绝对不能被误解析为含糖量！
        assertEquals(4.6, parsed.sugarPer100, 0.01)
        assertEquals(23.0, parsed.totalSugarG, 0.01)
        assertFalse(parsed.isSugarExceeded)
        assertEquals(500, parsed.bottleCapacityMl)
        assertTrue(parsed.productName.contains("康师傅") || parsed.productName.contains("绿茶"))
        assertFalse(parsed.productName.startsWith("品名,"))
    }
}
