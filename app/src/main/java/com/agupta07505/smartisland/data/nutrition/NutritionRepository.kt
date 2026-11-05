/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 */

package com.agupta07505.smartisland.data.nutrition

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import com.agupta07505.smartisland.util.AiNutritionScanner
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.json.JSONArray
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NutritionRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs: SharedPreferences = context.getSharedPreferences("smartisland_nutrition_prefs", Context.MODE_PRIVATE)

    // 当日扫描记录列表
    private val _scannedProducts = MutableStateFlow<List<ScannedProductInfo>>(emptyList())
    val scannedProducts: StateFlow<List<ScannedProductInfo>> = _scannedProducts.asStateFlow()

    // 训练留档历史列表
    private val _workoutArchives = MutableStateFlow<List<WorkoutArchiveRecord>>(emptyList())
    val workoutArchives: StateFlow<List<WorkoutArchiveRecord>> = _workoutArchives.asStateFlow()

    // 本周已训练部位集合 (每周重置，驱动一周训练图点亮)
    private val _weeklyTrainedCategories = MutableStateFlow<Set<String>>(emptySet())
    val weeklyTrainedCategories: StateFlow<Set<String>> = _weeklyTrainedCategories.asStateFlow()

    // 当日累计摄入热量 (kcal) 与 糖分 (g)
    private val _todayCalories = MutableStateFlow(0.0)
    val todayCalories: StateFlow<Double> = _todayCalories.asStateFlow()

    private val _todaySugar = MutableStateFlow(0.0)
    val todaySugar: StateFlow<Double> = _todaySugar.asStateFlow()

    // 异步后台保活扫描状态（切应用/切后台绝不断流，带真实进度与阶段报告）
    private val _isAnalyzing = MutableStateFlow(false)
    val isAnalyzing: StateFlow<Boolean> = _isAnalyzing.asStateFlow()

    private val _scanProgress = MutableStateFlow(0f)
    val scanProgress: StateFlow<Float> = _scanProgress.asStateFlow()

    private val _scanStageText = MutableStateFlow("")
    val scanStageText: StateFlow<String> = _scanStageText.asStateFlow()

    private val _pendingProductResult = MutableStateFlow<ScannedProductInfo?>(null)
    val pendingProductResult: StateFlow<ScannedProductInfo?> = _pendingProductResult.asStateFlow()

    private val _fallbackReason = MutableStateFlow<String?>(null)
    val fallbackReason: StateFlow<String?> = _fallbackReason.asStateFlow()

    private val backgroundScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    init {
        loadData()
    }

    private fun getTodayDateString(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return sdf.format(Date())
    }

    private fun getCurrentWeekId(): String {
        val cal = Calendar.getInstance(Locale.getDefault())
        cal.firstDayOfWeek = Calendar.MONDAY
        val year = cal.get(Calendar.YEAR)
        val week = cal.get(Calendar.WEEK_OF_YEAR)
        return "${year}_W${week}"
    }

    private fun loadData() {
        val todayStr = getTodayDateString()
        val savedDate = prefs.getString("last_record_date", "")

        // 加载历史留档
        val archivesJson = prefs.getString("workout_archives_json", "[]") ?: "[]"
        try {
            val jsonArr = JSONArray(archivesJson)
            val list = mutableListOf<WorkoutArchiveRecord>()
            for (i in 0 until jsonArr.length()) {
                list.add(WorkoutArchiveRecord.fromJson(jsonArr.getString(i)))
            }
            _workoutArchives.value = list
        } catch (_: Exception) {
            _workoutArchives.value = emptyList()
        }

        // 加载一周已训练部位 (周一至周日周期重置)
        val currentWeek = getCurrentWeekId()
        val savedWeek = prefs.getString("current_week_id", "")
        if (savedWeek != currentWeek) {
            // 新的一周，重置点亮部位
            _weeklyTrainedCategories.value = emptySet()
            prefs.edit().putString("current_week_id", currentWeek).putStringSet("weekly_trained_parts", emptySet()).apply()
        } else {
            val savedParts = prefs.getStringSet("weekly_trained_parts", emptySet()) ?: emptySet()
            _weeklyTrainedCategories.value = savedParts
        }

        // 加载今日摄入明细
        if (savedDate == todayStr) {
            val productsJson = prefs.getString("today_products_json", "[]") ?: "[]"
            try {
                val jsonArr = JSONArray(productsJson)
                val list = mutableListOf<ScannedProductInfo>()
                var hasCorrupted = false
                for (i in 0 until jsonArr.length()) {
                    val item = ScannedProductInfo.fromJson(jsonArr.getString(i))
                    val sanitizedItem = if (item.totalSugarG > 500.0 || item.sugarPer100 > 100.0 || item.totalSugarG.isNaN()) {
                        hasCorrupted = true
                        val safeSugar100 = if (item.sugarPer100 in 0.0..100.0) item.sugarPer100 else 0.0
                        val safeTotalSugar = Math.round(safeSugar100 * item.bottleCapacityMl / 100.0 * 10.0) / 10.0
                        item.copy(sugarPer100 = safeSugar100, totalSugarG = safeTotalSugar, isSugarExceeded = safeTotalSugar > 25.0)
                    } else item
                    list.add(sanitizedItem)
                }
                _scannedProducts.value = list
                calculateTodayTotals(list)
                if (hasCorrupted) {
                    saveTodayProducts(todayStr, list)
                }
            } catch (_: Exception) {
                _scannedProducts.value = emptyList()
            }
        } else {
            // 新的一天，重置当日摄入
            _scannedProducts.value = emptyList()
            _todayCalories.value = 0.0
            _todaySugar.value = 0.0
            prefs.edit()
                .putString("last_record_date", todayStr)
                .putString("today_products_json", "[]")
                .apply()
        }
    }

    private fun calculateTodayTotals(list: List<ScannedProductInfo>) {
        var cal = 0.0
        var sugar = 0.0
        for (item in list) {
            val safeCal = if (item.totalEnergyKcal.isFinite() && item.totalEnergyKcal in 0.0..10000.0) item.totalEnergyKcal else 0.0
            val safeSugar = if (item.totalSugarG.isFinite() && item.totalSugarG in 0.0..500.0) item.totalSugarG else 0.0
            cal += safeCal
            sugar += safeSugar
        }
        _todayCalories.value = Math.round(cal * 10.0) / 10.0
        _todaySugar.value = Math.round(sugar * 10.0) / 10.0
    }

    /**
     * 启动异步识别任务（后台保活、切前后台绝不被杀死或中断，毫秒级快速反馈）
     */
    fun startScan(context: Context, imageUri: Uri, capacityMl: Int) {
        if (_isAnalyzing.value) return
        _isAnalyzing.value = true
        _scanProgress.value = 0.15f
        _scanStageText.value = "正在读取包装高清图片..."
        _fallbackReason.value = null

        backgroundScope.launch {
            try {
                val result = AiNutritionScanner.analyzeImageFromUri(
                    context = context.applicationContext,
                    imageUri = imageUri,
                    customCapacityMl = capacityMl,
                    onProgress = { progress, stage ->
                        _scanProgress.value = progress
                        _scanStageText.value = stage
                    }
                )
                result.onSuccess { info ->
                    _pendingProductResult.value = info
                }.onFailure { err ->
                    _fallbackReason.value = err.message ?: "未提取到有效包装字迹，请使用快捷预设或重新拍摄"
                }
            } catch (e: Exception) {
                _fallbackReason.value = e.message ?: "图片识别处理异常"
            } finally {
                _isAnalyzing.value = false
                _scanProgress.value = 1f
            }
        }
    }

    fun clearPendingResult() {
        _pendingProductResult.value = null
    }

    fun clearFallbackReason() {
        _fallbackReason.value = null
    }

    /**
     * 添加一项识图或手动录入的饮品/食品
     */
    fun addProductRecord(product: ScannedProductInfo) {
        val todayStr = getTodayDateString()
        _scannedProducts.update { current ->
            val updated = listOf(product) + current
            calculateTodayTotals(updated)
            saveTodayProducts(todayStr, updated)
            updated
        }
    }

    /**
     * 删除一项摄入记录
     */
    fun removeProductRecord(productId: String) {
        val todayStr = getTodayDateString()
        _scannedProducts.update { current ->
            val updated = current.filterNot { it.id == productId }
            calculateTodayTotals(updated)
            saveTodayProducts(todayStr, updated)
            updated
        }
    }

    private fun saveTodayProducts(dateStr: String, list: List<ScannedProductInfo>) {
        val arr = JSONArray()
        list.forEach { arr.put(it.toJson()) }
        prefs.edit()
            .putString("last_record_date", dateStr)
            .putString("today_products_json", arr.toString())
            .apply()
    }

    /**
     * 训练结束自动留档并点亮本周对应部位
     */
    fun archiveWorkoutSession(categoryName: String, durationSeconds: Int, totalSets: Int, summary: String = "") {
        val record = WorkoutArchiveRecord(
            categoryName = categoryName,
            completedAt = System.currentTimeMillis(),
            durationSeconds = durationSeconds,
            totalSets = totalSets,
            summary = summary.ifBlank { "完成 $categoryName 训练 · 共 ${totalSets} 组" }
        )

        // 1. 保存留档
        _workoutArchives.update { current ->
            val updated = listOf(record) + current
            val arr = JSONArray()
            updated.take(100).forEach { arr.put(it.toJson()) }
            prefs.edit().putString("workout_archives_json", arr.toString()).apply()
            updated
        }

        // 2. 点亮本周该部位
        val currentWeek = getCurrentWeekId()
        _weeklyTrainedCategories.update { current ->
            val updated = current + categoryName
            prefs.edit()
                .putString("current_week_id", currentWeek)
                .putStringSet("weekly_trained_parts", updated)
                .apply()
            updated
        }
    }

    /**
     * 手动重置本周点亮部位（用于调试或新周期）
     */
    fun resetWeeklyTrainedParts() {
        val currentWeek = getCurrentWeekId()
        _weeklyTrainedCategories.value = emptySet()
        prefs.edit()
            .putString("current_week_id", currentWeek)
            .putStringSet("weekly_trained_parts", emptySet())
            .apply()
    }
}
