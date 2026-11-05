/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 */

package com.agupta07505.smartisland.data.nutrition

import java.util.regex.Pattern
import kotlin.math.roundToInt

/**
 * 饮料/食品包装文字智能解析器 (图转文 -> 文析营核心引擎)
 * 能够从 OCR 提取的无序或半结构化文本中，高精度提取产品名、配料表与营养成分表数值
 */
object NutritionTextParser {

    fun parseOcrText(rawText: String, defaultCapacity: Int = 500): ScannedProductInfo {
        val lines = rawText.lines().map { it.trim() }.filter { it.isNotBlank() }

        var brand = ""
        var productName = ""
        var productType = ""
        var ingredients = ""
        var capacity = defaultCapacity

        var energyKj = 0.0
        var energyKcal = 0.0
        var protein = 0.0
        var fat = 0.0
        var carbs = 0.0
        var sugar = 0.0
        var sodium = 0.0

        val candidateTitles = mutableListOf<String>()

        for (line in lines) {
            // 1. 净含量提取
            if (line.contains("净含量") || line.contains("容量") || line.contains("规格") || line.contains("Net", ignoreCase = true)) {
                val capMatcher = Pattern.compile("(\\d+)\\s*(?:ml|mL|毫升|克|g|G)", Pattern.CASE_INSENSITIVE).matcher(line)
                if (capMatcher.find()) {
                    capacity = capMatcher.group(1)?.toIntOrNull()?.coerceAtLeast(100) ?: defaultCapacity
                }
            }

            // 2. 配料表提取
            if (line.startsWith("配料") || line.contains("配料表") || line.contains("配料：")) {
                ingredients = if (ingredients.isBlank()) line else "$ingredients；$line"
            }

            // 3. 营养成分表各行提取
            if (line.contains("能量")) {
                val kjMatcher = Pattern.compile("(\\d+(?:\\.\\d+)?)\\s*(?:千焦|kJ|kj)", Pattern.CASE_INSENSITIVE).matcher(line)
                if (kjMatcher.find()) {
                    val v = kjMatcher.group(1)?.toDoubleOrNull() ?: 0.0
                    if (v in 0.0..8000.0) {
                        energyKj = v
                        energyKcal = (energyKj / 4.184 * 10).roundToInt() / 10.0
                    }
                } else {
                    val kcalMatcher = Pattern.compile("(\\d+(?:\\.\\d+)?)\\s*(?:千卡|kcal|大卡)", Pattern.CASE_INSENSITIVE).matcher(line)
                    if (kcalMatcher.find()) {
                        val v = kcalMatcher.group(1)?.toDoubleOrNull() ?: 0.0
                        if (v in 0.0..2000.0) {
                            energyKcal = v
                            energyKj = (energyKcal * 4.184 * 10).roundToInt() / 10.0
                        }
                    } else {
                        // 兜底提取行内第一个合理数值
                        extractSafeDouble(line, 8000.0)?.let {
                            if (it > 50) {
                                energyKj = it
                                energyKcal = (it / 4.184 * 10).roundToInt() / 10.0
                            } else {
                                energyKcal = it
                                energyKj = (it * 4.184 * 10).roundToInt() / 10.0
                            }
                        }
                    }
                }
            } else if (line.contains("蛋白质")) {
                extractSafeDouble(line, 100.0)?.let { protein = it }
            } else if (line.contains("脂肪") && !line.contains("反式")) {
                extractSafeDouble(line, 100.0)?.let { fat = it }
            } else if (line.contains("碳水化合物") || line.contains("碳水")) {
                extractSafeDouble(line, 100.0)?.let { carbs = it }
            } else if (line.contains("糖") && !isSugarExcludeLine(line)) {
                extractSafeDouble(line, 100.0)?.let { sugar = it }
            } else if (line.contains("钠") && !line.contains("碳酸氢钠")) {
                extractSafeDouble(line, 10000.0)?.let { sodium = it }
            } else {
                // 可能是品名/品牌候选
                val cleanedTitle = cleanTitleCandidate(line)
                if (cleanedTitle.isNotBlank() && !isTechnicalMetaLine(cleanedTitle) && cleanedTitle.length in 2..25) {
                    candidateTitles.add(cleanedTitle)
                }
            }
        }

        // 糖分兜底逻辑（很多饮料不单独标糖，但碳水几乎全部是糖）
        if (sugar <= 0.0 && carbs > 0.0) {
            sugar = carbs
        }

        // 产品品名提取决策
        if (candidateTitles.isNotEmpty()) {
            productName = candidateTitles.first()
            if (candidateTitles.size >= 2) {
                brand = candidateTitles[0]
                productName = "${candidateTitles[0]} ${candidateTitles[1]}"
            }
        } else {
            productName = "已拍照识别饮品"
        }

        // 推测产品类型
        productType = when {
            productName.contains("茶") || lines.any { it.contains("茶") } -> "茶饮料"
            productName.contains("果汁") || lines.any { it.contains("果汁") } -> "果汁饮料"
            productName.contains("电解质") || lines.any { it.contains("电解质") } -> "电解质饮料"
            productName.contains("咖啡") || lines.any { it.contains("咖啡") } -> "咖啡饮料"
            productName.contains("可乐") || productName.contains("汽水") -> "碳酸饮料"
            productName.contains("奶") || productName.contains("蛋白") -> "蛋白饮品"
            else -> "包装饮品"
        }

        val factor = capacity / 100.0
        val totalSugar = (sugar * factor * 10).roundToInt() / 10.0
        val totalKcal = (energyKcal * factor * 10).roundToInt() / 10.0
        val isSugarExceeded = totalSugar > 25.0

        val healthEvaluation = if (isSugarExceeded) {
            "整瓶含糖量达 ${totalSugar}g，已超过《中国居民膳食指南》推荐的每日添加糖限量（25g）。健身控糖期建议适量或分次饮用。"
        } else if (totalSugar == 0.0 && totalKcal <= 10.0) {
            "0糖极低热量饮品，运动训练及减脂控卡期可放心补充水分。"
        } else {
            "热量（约 ${totalKcal} kcal）与糖分处于适度范围，可作为运动前后的适量能量补充。"
        }

        val micronutrientAdvice = when {
            productType.contains("电解质") -> "富含钠、钾、镁等水合电解质，适合大强度深蹲、硬拉等暴汗训练后快速补水与肌肉防抽筋。"
            productType.contains("茶") -> "含有茶多酚与微量矿物质，运动出汗后饮用可补充水分；建议日常搭配绿叶蔬菜补充膳食镁与钾。"
            productType.contains("蛋白") -> "富含乳清/植物蛋白与支链氨基酸，建议在力量训练后 30 分钟内摄入以促进肌纤维修复超量恢复。"
            else -> "出汗较多时注意随餐饮水补纳与钾离子，日常饮食建议多摄入深色蔬菜、坚果与新鲜水果以平衡微量元素。"
        }

        return ScannedProductInfo(
            brand = brand.ifBlank { "优质品牌" },
            productName = productName.ifBlank { "包装食品/饮料" },
            productType = productType,
            ingredients = ingredients,
            servingSize = "每100mL",
            bottleCapacityMl = capacity,
            energyKjPer100 = energyKj,
            energyKcalPer100 = energyKcal,
            proteinPer100 = protein,
            fatPer100 = fat,
            carbsPer100 = carbs,
            sugarPer100 = sugar,
            sodiumMgPer100 = sodium,
            totalEnergyKcal = totalKcal,
            totalSugarG = totalSugar,
            isSugarExceeded = isSugarExceeded,
            healthEvaluation = healthEvaluation,
            micronutrientAdvice = micronutrientAdvice,
            timestamp = System.currentTimeMillis()
        )
    }

    private fun extractSafeDouble(line: String, maxLimit: Double): Double? {
        if (isLicenseOrStandardLine(line)) return null
        val matcher = Pattern.compile("(\\d+(?:\\.\\d+)?)").matcher(line)
        while (matcher.find()) {
            val v = matcher.group(1)?.toDoubleOrNull()
            if (v != null && v in 0.0..maxLimit) {
                return v
            }
        }
        return null
    }

    private fun isSugarExcludeLine(line: String): Boolean {
        val excludes = listOf(
            "每100", "参考值", "配料", "白砂糖", "冰糖", "果葡糖浆",
            "蔗糖", "代糖", "无糖", "0糖", "阿斯巴甜", "安赛蜜",
            "许可证", "标准", "SC", "GB", "电话", "热线", "条码", "69"
        )
        return excludes.any { line.contains(it, ignoreCase = true) }
    }

    private fun isLicenseOrStandardLine(line: String): Boolean {
        val blacklist = listOf("许可证", "SC", "GB", "ISO", "电话", "热线", "条码", "执行标准", "保质期", "生产日期", "地址", "产地", "QS")
        return blacklist.any { line.contains(it, ignoreCase = true) }
    }

    private fun cleanTitleCandidate(line: String): String {
        var cleaned = line.replace(Regex("^(?:品名|产品名称|名称|品牌|商标)[：:,，\\s]*"), "").trim()
        cleaned = cleaned.replace(Regex("^(?:量武于|含量|小于|大于)[^a-zA-Z\\u4e00-\\u9fa5]*"), "").trim()
        if (cleaned.contains("g/Kg", ignoreCase = true) || cleaned.contains("mg/kg", ignoreCase = true)) {
            return ""
        }
        return cleaned
    }

    private fun isTechnicalMetaLine(line: String): Boolean {
        val metaKeywords = listOf(
            "营养成分", "项目", "每100", "NRV", "参考值", "净含量", "配料", "保质期",
            "生产日期", "执行标准", "生产许可证", "SC", "GB", "电话", "地址", "储藏",
            "能量", "蛋白质", "脂肪", "碳水化合物", "钠", "千焦", "kJ", "kcal"
        )
        return metaKeywords.any { line.contains(it, ignoreCase = true) }
    }
}
