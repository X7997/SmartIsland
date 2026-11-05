/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 */

package com.agupta07505.smartisland.data.nutrition

import org.json.JSONObject

/**
 * 扫描识别出的食品/饮料包装信息与营养成分
 */
data class ScannedProductInfo(
    val id: String = System.currentTimeMillis().toString(),
    val brand: String = "",
    val productName: String = "未知饮品/食品",
    val productType: String = "",
    val ingredients: String = "",
    val servingSize: String = "每100mL",
    val bottleCapacityMl: Int = 500,
    val energyKjPer100: Double = 0.0,
    val energyKcalPer100: Double = 0.0,
    val proteinPer100: Double = 0.0,
    val fatPer100: Double = 0.0,
    val saturatedFatPer100: Double = 0.0,
    val carbsPer100: Double = 0.0,
    val sugarPer100: Double = 0.0,
    val sodiumMgPer100: Double = 0.0,
    val totalEnergyKcal: Double = 0.0,
    val totalSugarG: Double = 0.0,
    val isSugarExceeded: Boolean = false,
    val healthEvaluation: String = "",
    val micronutrientAdvice: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val imageUri: String? = null
) {
    fun toJson(): String {
        val obj = JSONObject()
        obj.put("id", id)
        obj.put("brand", brand)
        obj.put("productName", productName)
        obj.put("productType", productType)
        obj.put("ingredients", ingredients)
        obj.put("servingSize", servingSize)
        obj.put("bottleCapacityMl", bottleCapacityMl)
        obj.put("energyKjPer100", energyKjPer100)
        obj.put("energyKcalPer100", energyKcalPer100)
        obj.put("proteinPer100", proteinPer100)
        obj.put("fatPer100", fatPer100)
        obj.put("saturatedFatPer100", saturatedFatPer100)
        obj.put("carbsPer100", carbsPer100)
        obj.put("sugarPer100", sugarPer100)
        obj.put("sodiumMgPer100", sodiumMgPer100)
        obj.put("totalEnergyKcal", totalEnergyKcal)
        obj.put("totalSugarG", totalSugarG)
        obj.put("isSugarExceeded", isSugarExceeded)
        obj.put("healthEvaluation", healthEvaluation)
        obj.put("micronutrientAdvice", micronutrientAdvice)
        obj.put("timestamp", timestamp)
        obj.put("imageUri", imageUri ?: "")
        return obj.toString()
    }

    companion object {
        fun fromJson(jsonStr: String): ScannedProductInfo {
            val obj = JSONObject(jsonStr)
            return ScannedProductInfo(
                id = obj.optString("id", System.currentTimeMillis().toString()),
                brand = obj.optString("brand", ""),
                productName = obj.optString("productName", "未知产品"),
                productType = obj.optString("productType", ""),
                ingredients = obj.optString("ingredients", ""),
                servingSize = obj.optString("servingSize", "每100mL"),
                bottleCapacityMl = obj.optInt("bottleCapacityMl", 500),
                energyKjPer100 = obj.optDouble("energyKjPer100", 0.0),
                energyKcalPer100 = obj.optDouble("energyKcalPer100", 0.0),
                proteinPer100 = obj.optDouble("proteinPer100", 0.0),
                fatPer100 = obj.optDouble("fatPer100", 0.0),
                saturatedFatPer100 = obj.optDouble("saturatedFatPer100", 0.0),
                carbsPer100 = obj.optDouble("carbsPer100", 0.0),
                sugarPer100 = obj.optDouble("sugarPer100", 0.0),
                sodiumMgPer100 = obj.optDouble("sodiumMgPer100", 0.0),
                totalEnergyKcal = obj.optDouble("totalEnergyKcal", 0.0),
                totalSugarG = obj.optDouble("totalSugarG", 0.0),
                isSugarExceeded = obj.optBoolean("isSugarExceeded", false),
                healthEvaluation = obj.optString("healthEvaluation", ""),
                micronutrientAdvice = obj.optString("micronutrientAdvice", ""),
                timestamp = obj.optLong("timestamp", System.currentTimeMillis()),
                imageUri = obj.optString("imageUri", "").takeIf { it.isNotBlank() }
            )
        }
    }
}

/**
 * 训练留档记录模型
 */
data class WorkoutArchiveRecord(
    val id: String = System.currentTimeMillis().toString(),
    val categoryName: String,
    val completedAt: Long = System.currentTimeMillis(),
    val durationSeconds: Int = 0,
    val totalSets: Int = 0,
    val summary: String = ""
) {
    fun toJson(): String {
        val obj = JSONObject()
        obj.put("id", id)
        obj.put("categoryName", categoryName)
        obj.put("completedAt", completedAt)
        obj.put("durationSeconds", durationSeconds)
        obj.put("totalSets", totalSets)
        obj.put("summary", summary)
        return obj.toString()
    }

    companion object {
        fun fromJson(jsonStr: String): WorkoutArchiveRecord {
            val obj = JSONObject(jsonStr)
            return WorkoutArchiveRecord(
                id = obj.optString("id", System.currentTimeMillis().toString()),
                categoryName = obj.optString("categoryName", ""),
                completedAt = obj.optLong("completedAt", System.currentTimeMillis()),
                durationSeconds = obj.optInt("durationSeconds", 0),
                totalSets = obj.optInt("totalSets", 0),
                summary = obj.optString("summary", "")
            )
        }
    }
}
