/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 */

package com.agupta07505.smartisland.data.nutrition

import java.util.regex.Pattern
import kotlin.math.roundToInt

/**
 * 饮料/食品包装文字智能解析器 (图转文 -> 文析营核心引擎 3.0)
 * 能够从真实手机相机 OCR 提取的各种无序、错位或半结构化文本中，高精度提取产品名、容量与营养成分
 * 彻底杜绝由于 NRV% 误判、换行错位、无糖被覆盖、容量默认值跳变等导致的“同一瓶饮料含糖量不同”问题
 */
object NutritionTextParser {

    fun parseOcrText(rawText: String, defaultCapacity: Int = 500): ScannedProductInfo {
        val lines = rawText.lines().map { it.trim() }.filter { it.isNotBlank() }

        var brand = ""
        var productName = ""
        var productType = ""
        var ingredients = ""
        var detectedCapacity: Int? = null

        var energyKj = 0.0
        var energyKcal = 0.0
        var protein = 0.0
        var fat = 0.0
        var carbs = 0.0
        var sugar = 0.0
        var sodium = 0.0

        var hasExplicitSugar = false
        var isSugarFreeDeclared = false
        var isPerServingOrBottle = false
        var servingAmountMl = 100

        val candidateTitles = mutableListOf<String>()

        // 预扫描：是否有全包装醒目的“0糖 / 无糖 / 0卡 / 0能量”
        for (line in lines) {
            if (line.matches(Regex(".*(?:0糖|零糖|无糖|0蔗糖|零蔗糖|无蔗糖|0卡|零卡|0能量).*")) && !line.contains("非0糖")) {
                isSugarFreeDeclared = true
            }
        }

        for (line in lines) {
            // 1. 净含量提取（支持“净含量：500mL”、“规格 330ml”、“净含量 1.25L”以及包装正面大字如“500mL”、“330mL”）
            if (detectedCapacity == null) {
                val cap = extractCapacity(line)
                if (cap != null) {
                    detectedCapacity = cap
                }
            }

            // 2. 配料表提取
            if (line.startsWith("配料") || line.contains("配料表") || line.contains("配料：") || line.contains("配料:")) {
                ingredients = if (ingredients.isBlank()) line else "$ingredients；$line"
            }

            // 3. 营养成分表基准提取（每100mL、每份 250mL、每瓶 500mL 等）
            if (line.contains("营养成分") || line.contains("每100") || line.contains("每份") || line.contains("每瓶") || line.contains("每罐")) {
                if (line.contains("每份") || line.contains("每瓶") || line.contains("每罐") || line.contains("整瓶")) {
                    isPerServingOrBottle = true
                    // 尝试提取每份的具体毫升数，如“每份(250mL)”
                    val sMatcher = Pattern.compile("每份\\s*[(（]?\\s*(\\d+)\\s*(?:ml|mL|毫升|g|克)", Pattern.CASE_INSENSITIVE).matcher(line)
                    if (sMatcher.find()) {
                        servingAmountMl = sMatcher.group(1)?.toIntOrNull() ?: 100
                    }
                }
            }

            // 4. 营养成分表各行精确提取
            if (isLicenseOrStandardLine(line)) {
                continue // 绝不把许可证号/标准号送入营养解析
            }

            if (line.contains("能量")) {
                extractEnergy(line)?.let { (kj, kcal) ->
                    energyKj = kj
                    energyKcal = kcal
                }
            } else if (line.contains("蛋白质")) {
                extractGramValue(line, 100.0)?.let { protein = it }
            } else if (line.contains("脂肪") && !line.contains("反式")) {
                extractGramValue(line, 100.0)?.let { fat = it }
            } else if (line.contains("碳水化合物") || (line.contains("碳水") && !line.contains("碳酸"))) {
                extractGramValue(line, 100.0)?.let { carbs = it }
            } else if (line.contains("糖") && !line.contains("配料") && !line.contains("白砂糖") && !line.contains("冰糖") && !line.contains("果葡糖浆")) {
                // 糖分精准匹配
                if (isExplicitZeroSugar(line)) {
                    sugar = 0.0
                    hasExplicitSugar = true
                } else {
                    val sVal = extractGramValue(line, 100.0)
                    if (sVal != null) {
                        sugar = sVal
                        hasExplicitSugar = true
                    }
                }
            } else if (line.contains("钠") && !line.contains("碳酸氢钠") && !line.contains("柠檬酸钠") && !line.contains("聚磷酸钠")) {
                extractSodiumMg(line)?.let { sodium = it }
            } else {
                // 品名候选过滤
                val cleanedTitle = cleanTitleCandidate(line)
                if (cleanedTitle.isNotBlank() && !isTechnicalMetaLine(cleanedTitle) && cleanedTitle.length in 2..25) {
                    candidateTitles.add(cleanedTitle)
                }
            }
        }

        // 最终容量判定：若拍到了净含量用拍到的，若未拍到使用传入的 defaultCapacity
        val finalCapacity = detectedCapacity ?: defaultCapacity

        // 糖分安全校验：
        // 1. 如果包装明确印有“0糖/无糖”，糖分强力校准为 0.0g，绝不允许被碳水化合物或其他干扰项污染
        if (isSugarFreeDeclared) {
            sugar = 0.0
            hasExplicitSugar = true
        }
        // 2. 只有在整张表没有标“糖”（hasExplicitSugar == false），且产品不是“0糖茶”时，才将糖分以碳水作为估算
        if (!hasExplicitSugar) {
            if (carbs > 0.0 && !isSugarFreeDeclared) {
                sugar = carbs
            } else {
                sugar = 0.0
            }
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
            productName.contains("可乐") || productName.contains("汽水") || productName.contains("气泡") -> "碳酸/气泡水"
            productName.contains("奶") || productName.contains("蛋白") -> "蛋白饮品"
            else -> "包装饮品"
        }

        // 规格与整瓶数值换算（严格区分“每100mL”与“每瓶/每份”）
        val totalSugar: Double
        val totalKcal: Double
        val sugarPer100Out: Double
        val energyKcalPer100Out: Double
        val servingSizeText: String

        if (isPerServingOrBottle && servingAmountMl > 100) {
            // 表格已经是每份（整瓶）数据
            totalSugar = (sugar * 10).roundToInt() / 10.0
            totalKcal = (energyKcal * 10).roundToInt() / 10.0
            sugarPer100Out = ((sugar / (servingAmountMl / 100.0)) * 10).roundToInt() / 10.0
            energyKcalPer100Out = ((energyKcal / (servingAmountMl / 100.0)) * 10).roundToInt() / 10.0
            servingSizeText = "每份(${servingAmountMl}mL)"
        } else {
            // 表格是标准每 100mL 数据
            val factor = finalCapacity / 100.0
            sugarPer100Out = (sugar * 10).roundToInt() / 10.0
            energyKcalPer100Out = (energyKcal * 10).roundToInt() / 10.0
            totalSugar = (sugarPer100Out * factor * 10).roundToInt() / 10.0
            totalKcal = (energyKcalPer100Out * factor * 10).roundToInt() / 10.0
            servingSizeText = "每100mL"
        }

        val isSugarExceeded = totalSugar > 25.0

        val healthEvaluation = when {
            isSugarFreeDeclared || (totalSugar == 0.0 && totalKcal <= 15.0) -> {
                "0糖低卡配方，升糖负荷几乎为零，运动减脂与控糖期可放心补充水分。"
            }
            isSugarExceeded -> {
                "整瓶含糖量达 ${totalSugar}g（约相当于 ${(totalSugar / 4.5).roundToInt()} 块方糖），已超过《中国居民膳食指南》推荐的每日添加糖限量（25g）。健身控糖期建议适量或分次饮用。"
            }
            totalSugar in 0.1..15.0 -> {
                "低糖轻负担（整瓶约 ${totalSugar}g 糖），热量约 ${totalKcal} kcal，适合运动前后快速恢复肌糖原与适度供能。"
            }
            else -> {
                "热量（约 ${totalKcal} kcal）与含糖（约 ${totalSugar}g）处于中等范围，建议结合当日总热量与运动消耗平衡饮用。"
            }
        }

        val micronutrientAdvice = when {
            productType.contains("电解质") -> "富含钠离子、钾离子与微量矿物盐，适合大强度深蹲、硬拉等暴汗训练后快速补水与防止肌痉挛抽筋。"
            productType.contains("茶") -> "含有天然茶多酚与微量钾元素；运动出汗后饮用利于水合，日常可搭配绿叶蔬菜与坚果补充膳食镁元素。"
            productType.contains("蛋白") -> "富含乳清或植物蛋白与必需氨基酸，建议在力量训练后 30-45 分钟窗口期摄入以促进骨骼肌超量恢复。"
            productType.contains("果汁") -> "富含维生素C与天然果糖，但膳食纤维较少；若当日出汗较多，建议多吃深色绿叶菜以补足钾和镁。"
            else -> "大强度出汗训练后注意随餐饮水补充钠与钾离子，日常饮食建议多摄入深色蔬菜、全谷物与新鲜水果以平衡微量元素。"
        }

        return ScannedProductInfo(
            brand = brand.ifBlank { "优质品牌" },
            productName = productName.ifBlank { "包装食品/饮料" },
            productType = productType,
            ingredients = ingredients,
            servingSize = servingSizeText,
            bottleCapacityMl = finalCapacity,
            energyKjPer100 = energyKj,
            energyKcalPer100 = energyKcalPer100Out,
            proteinPer100 = protein,
            fatPer100 = fat,
            carbsPer100 = carbs,
            sugarPer100 = sugarPer100Out,
            sodiumMgPer100 = sodium,
            totalEnergyKcal = totalKcal,
            totalSugarG = totalSugar,
            isSugarExceeded = isSugarExceeded,
            healthEvaluation = healthEvaluation,
            micronutrientAdvice = micronutrientAdvice,
            timestamp = System.currentTimeMillis()
        )
    }

    /**
     * 提取容量/净含量数值（单位：mL）
     */
    fun extractCapacity(line: String): Int? {
        // 1. 标准前缀：净含量 500mL、规格 330ml、Net 450mL
        val stdMatcher = Pattern.compile(
            "(?:净含量|容量|规格|Net)\\s*[：:\\s]*(\\d+(?:\\.\\d+)?)\\s*(?:ml|mL|毫升|L|升|克|g|G)",
            Pattern.CASE_INSENSITIVE
        ).matcher(line)
        if (stdMatcher.find()) {
            val numStr = stdMatcher.group(1) ?: return null
            val num = numStr.toDoubleOrNull() ?: return null
            val isLiter = line.contains("L", ignoreCase = false) || line.contains("升")
            val ml = if (isLiter && num < 10.0) (num * 1000).toInt() else num.toInt()
            if (ml in 100..5000) return ml
        }

        // 2. 独立容量标记（如在非营养表格区域单独出现的“330mL”、“500ml”、“450mL”）
        if (!isTechnicalMetaLine(line)) {
            val soloMatcher = Pattern.compile("(\\d{3,4})\\s*(?:ml|mL|毫升)", Pattern.CASE_INSENSITIVE).matcher(line)
            if (soloMatcher.find()) {
                val ml = soloMatcher.group(1)?.toIntOrNull()
                if (ml != null && ml in 150..2500) return ml
            }
        }
        return null
    }

    /**
     * 从行内精准提取以克(g)为单位的数值，彻底排除 NRV% 干扰
     * 例如："其中：糖 9.8g 11%" -> 提取 9.8，绝不误提取 11.0！
     * 例如："糖 11% 9.8g" -> 提取 9.8，绝不误提取 11.0！
     * 例如："0g" -> 提取 0.0
     * 例如："< 0.5g" -> 提取 0.0
     */
    fun extractGramValue(line: String, maxLimit: Double = 100.0): Double? {
        if (line.contains("<0.5") || line.contains("< 0.5") || line.contains("未检出") || line.contains("无")) {
            return 0.0
        }

        // 优先匹配显式带 g 或 克 单位的数值，且排除紧跟 %
        // 匹配例如: 9.8g, 9.8 克, 0g, 0.0g
        val unitMatcher = Pattern.compile("(\\d+(?:\\.\\d+)?)\\s*(?:g|克|G)(?!%)", Pattern.CASE_INSENSITIVE).matcher(line)
        if (unitMatcher.find()) {
            val v = unitMatcher.group(1)?.toDoubleOrNull()
            if (v != null && v in 0.0..maxLimit) {
                return (v * 10).roundToInt() / 10.0
            }
        }

        // 次选：匹配行内不带 % 的独立浮点数或整数（排除后面的 %）
        val noPercentMatcher = Pattern.compile("(\\d+(?:\\.\\d+)?)(?!\\s*%)").matcher(line)
        while (noPercentMatcher.find()) {
            val start = noPercentMatcher.start()
            // 确保不是年份、电话、GB、SC
            val sub = line.substring(0, start)
            if (sub.contains("SC") || sub.contains("GB") || sub.contains("69")) continue
            val v = noPercentMatcher.group(1)?.toDoubleOrNull()
            if (v != null && v in 0.0..maxLimit) {
                return (v * 10).roundToInt() / 10.0
            }
        }

        return null
    }

    /**
     * 能量精准提取 (kJ 与 kcal 双向折算)
     */
    fun extractEnergy(line: String): Pair<Double, Double>? {
        if (line.contains("0kJ") || line.contains("0 kJ") || line.contains("0千焦") || line.contains("0kcal") || line.contains("0千卡")) {
            return Pair(0.0, 0.0)
        }
        val kjMatcher = Pattern.compile("(\\d+(?:\\.\\d+)?)\\s*(?:千焦|kJ|kj)(?!%)", Pattern.CASE_INSENSITIVE).matcher(line)
        if (kjMatcher.find()) {
            val kj = kjMatcher.group(1)?.toDoubleOrNull() ?: 0.0
            val kcal = (kj / 4.184 * 10).roundToInt() / 10.0
            return Pair((kj * 10).roundToInt() / 10.0, kcal)
        }
        val kcalMatcher = Pattern.compile("(\\d+(?:\\.\\d+)?)\\s*(?:千卡|kcal|大卡)(?!%)", Pattern.CASE_INSENSITIVE).matcher(line)
        if (kcalMatcher.find()) {
            val kcal = kcalMatcher.group(1)?.toDoubleOrNull() ?: 0.0
            val kj = (kcal * 4.184 * 10).roundToInt() / 10.0
            return Pair(kj, (kcal * 10).roundToInt() / 10.0)
        }
        // 兜底：纯数字（排除 %）
        val noPercentMatcher = Pattern.compile("(\\d+(?:\\.\\d+)?)(?!\\s*%)").matcher(line)
        while (noPercentMatcher.find()) {
            val v = noPercentMatcher.group(1)?.toDoubleOrNull() ?: continue
            if (v in 1.0..8000.0) {
                return if (v > 60.0) {
                    Pair((v * 10).roundToInt() / 10.0, (v / 4.184 * 10).roundToInt() / 10.0)
                } else {
                    Pair((v * 4.184 * 10).roundToInt() / 10.0, (v * 10).roundToInt() / 10.0)
                }
            }
        }
        return null
    }

    /**
     * 钠提取 (mg)
     */
    fun extractSodiumMg(line: String): Double? {
        if (line.contains("0mg") || line.contains("0 mg") || line.contains("0毫克")) return 0.0
        val mgMatcher = Pattern.compile("(\\d+(?:\\.\\d+)?)\\s*(?:mg|毫克)(?!%)", Pattern.CASE_INSENSITIVE).matcher(line)
        if (mgMatcher.find()) {
            val v = mgMatcher.group(1)?.toDoubleOrNull()
            if (v != null && v in 0.0..10000.0) return (v * 10).roundToInt() / 10.0
        }
        val noPercentMatcher = Pattern.compile("(\\d+(?:\\.\\d+)?)(?!\\s*%)").matcher(line)
        while (noPercentMatcher.find()) {
            val v = noPercentMatcher.group(1)?.toDoubleOrNull() ?: continue
            if (v in 0.0..10000.0) return (v * 10).roundToInt() / 10.0
        }
        return null
    }

    private fun isExplicitZeroSugar(line: String): Boolean {
        val zeroKeywords = listOf("0g", "0克", "0.0g", "0.0克", "<0.5", "< 0.5", "无糖", "0糖", "未检出", "无")
        return zeroKeywords.any { line.contains(it, ignoreCase = true) }
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
