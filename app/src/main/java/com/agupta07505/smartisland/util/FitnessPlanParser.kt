package com.agupta07505.smartisland.util

import android.content.Context
import android.net.Uri
import android.util.Log
import android.util.Xml
import com.agupta07505.smartisland.model.WorkoutCategory
import com.agupta07505.smartisland.model.WorkoutExercise
import com.agupta07505.smartisland.model.WorkoutPlan
import org.json.JSONObject
import org.xmlpull.v1.XmlPullParser
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.InputStream
import java.util.regex.Pattern
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

object FitnessPlanParser {
    private const val TAG = "FitnessPlanParser"
    private const val CUSTOM_PLAN_FILENAME = "custom_fitness_plan.json"

    private val VIDEO_URL_PATTERN = Pattern.compile("https?://[^\\s]+")
    private val SETS_REPS_PATTERN = Pattern.compile("(\\d+)\\s*[×x*X]\\s*(\\d+)")
    private val TIME_SETS_PATTERN = Pattern.compile("(\\d+s)\\s*[×x*X]\\s*(\\d+)")

    fun getSavedCustomPlanFile(context: Context): File {
        return File(context.filesDir, CUSTOM_PLAN_FILENAME)
    }

    fun hasCustomPlan(context: Context): Boolean {
        val file = getSavedCustomPlanFile(context)
        return file.exists() && file.length() > 20
    }

    fun deleteCustomPlan(context: Context): Boolean {
        val file = getSavedCustomPlanFile(context)
        return if (file.exists()) file.delete() else false
    }

    fun savePlan(context: Context, plan: WorkoutPlan) {
        val root = JSONObject()
        val catArray = org.json.JSONArray()
        for (cat in plan.categories) {
            val catObj = JSONObject()
            catObj.put("categoryName", cat.categoryName)
            if (cat.cue != null) catObj.put("cue", cat.cue) else catObj.put("cue", JSONObject.NULL)
            val exArray = org.json.JSONArray()
            for (ex in cat.exercises) {
                val exObj = JSONObject()
                exObj.put("name", ex.name)
                exObj.put("weight", ex.weight)
                exObj.put("sets", ex.sets)
                exObj.put("reps", ex.reps)
                if (ex.videoUrl != null) exObj.put("videoUrl", ex.videoUrl) else exObj.put("videoUrl", JSONObject.NULL)
                if (ex.targetMuscle != null) exObj.put("targetMuscle", ex.targetMuscle) else exObj.put("targetMuscle", JSONObject.NULL)
                if (ex.equipment != null) exObj.put("equipment", ex.equipment) else exObj.put("equipment", JSONObject.NULL)
                if (ex.setupTips != null) exObj.put("setupTips", ex.setupTips) else exObj.put("setupTips", JSONObject.NULL)
                exArray.put(exObj)
            }
            catObj.put("exercises", exArray)
            catArray.put(catObj)
        }
        root.put("categories", catArray)

        val file = getSavedCustomPlanFile(context)
        file.writeText(root.toString(2), Charsets.UTF_8)
    }

    fun loadCurrentPlan(context: Context): Pair<WorkoutPlan, String> {
        val defaultPlan = try {
            val jsonStr = context.assets.open("default_fitness_plan.json").bufferedReader().use { it.readText() }
            parseJson(jsonStr)
        } catch (e: Exception) {
            WorkoutPlan()
        }

        val defaultTipsMap = mutableMapOf<String, WorkoutExercise>()
        for (cat in defaultPlan.categories) {
            for (ex in cat.exercises) {
                defaultTipsMap[ex.name.trim()] = ex
            }
        }

        fun enrichPlan(plan: WorkoutPlan): WorkoutPlan {
            val enrichedCategories = plan.categories.map { cat ->
                val enrichedExercises = cat.exercises.map { ex ->
                    val defaultEx = defaultTipsMap[ex.name.trim()]
                    ex.copy(
                        targetMuscle = ex.targetMuscle ?: defaultEx?.targetMuscle ?: inferTargetMuscle(ex.name, cat.categoryName),
                        equipment = ex.equipment ?: defaultEx?.equipment ?: inferEquipment(ex.name),
                        setupTips = ex.setupTips?.takeIf { it.isNotBlank() } ?: defaultEx?.setupTips,
                        videoUrl = ex.videoUrl ?: defaultEx?.videoUrl
                    )
                }
                cat.copy(exercises = enrichedExercises)
            }
            return WorkoutPlan(enrichedCategories)
        }

        val customFile = getSavedCustomPlanFile(context)
        if (customFile.exists() && customFile.length() > 20) {
            try {
                val jsonStr = customFile.readText(Charsets.UTF_8)
                val plan = parseJson(jsonStr)
                if (plan.categories.isNotEmpty()) {
                    val lastModified = java.text.SimpleDateFormat("MM-dd HH:mm", java.util.Locale.getDefault())
                        .format(java.util.Date(customFile.lastModified()))
                    return enrichPlan(plan) to "自定义已导入健身计划 ($lastModified)"
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load custom plan file, falling back to assets", e)
            }
        }

        return enrichPlan(defaultPlan) to "系统内置标准健身计划"
    }

    fun parseFromUri(context: Context, uri: Uri): Result<WorkoutPlan> {
        return try {
            val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                ?: return Result.failure(IllegalStateException("无法读取文件内容"))

            val plan = parseFromBytes(bytes)
            if (plan.categories.isEmpty()) {
                return Result.failure(IllegalStateException("未能从文件中解析出有效的健身部位与动作"))
            }

            // Persist to internal storage
            savePlan(context, plan)
            Result.success(plan)
        } catch (e: Exception) {
            Log.e(TAG, "Error importing plan from uri: $uri", e)
            Result.failure(e)
        }
    }

    fun parseFromBytes(bytes: ByteArray): WorkoutPlan {
        if (bytes.size >= 4 && bytes[0] == 0x50.toByte() && bytes[1] == 0x4B.toByte()) {
            // ZIP header (PK\x03\x04), parse as Excel .xlsx
            return parseXlsx(ByteArrayInputStream(bytes))
        }

        // Otherwise parse as UTF-8 JSON
        val jsonStr = String(bytes, Charsets.UTF_8)
        return parseJson(jsonStr)
    }

    fun parseJson(jsonStr: String): WorkoutPlan {
        val root = JSONObject(jsonStr)
        val categoriesArray = root.optJSONArray("categories") ?: return WorkoutPlan()
        val categories = mutableListOf<WorkoutCategory>()

        for (i in 0 until categoriesArray.length()) {
            val catObj = categoriesArray.getJSONObject(i)
            val catName = catObj.optString("categoryName", "")
            val cue = if (catObj.has("cue") && !catObj.isNull("cue")) catObj.optString("cue") else null
            val exercisesArray = catObj.optJSONArray("exercises")
            val exercises = mutableListOf<WorkoutExercise>()

            if (exercisesArray != null) {
                for (j in 0 until exercisesArray.length()) {
                    val exObj = exercisesArray.getJSONObject(j)
                    val name = exObj.optString("name", "")
                    val weight = exObj.optString("weight", "")
                    val sets = exObj.optInt("sets", 4)
                    val reps = exObj.optString("reps", "12次")
                    val videoUrl = if (exObj.has("videoUrl") && !exObj.isNull("videoUrl")) exObj.optString("videoUrl") else null
                    val targetMuscle = if (exObj.has("targetMuscle") && !exObj.isNull("targetMuscle")) exObj.optString("targetMuscle") else null
                    val equipment = if (exObj.has("equipment") && !exObj.isNull("equipment")) exObj.optString("equipment") else null
                    val setupTips = if (exObj.has("setupTips") && !exObj.isNull("setupTips")) exObj.optString("setupTips") else null

                    if (name.isNotBlank()) {
                        exercises.add(
                            WorkoutExercise(
                                name = name,
                                weight = weight,
                                sets = sets,
                                reps = reps,
                                videoUrl = videoUrl,
                                targetMuscle = targetMuscle,
                                equipment = equipment,
                                setupTips = setupTips
                            )
                        )
                    }
                }
            }
            if (catName.isNotBlank()) {
                categories.add(WorkoutCategory(categoryName = catName, cue = cue, exercises = exercises))
            }
        }
        return WorkoutPlan(categories)
    }

    /**
     * Parse .xlsx using native ZipInputStream and XmlPullParser without third-party libraries.
     */
    fun parseXlsx(inputStream: InputStream): WorkoutPlan {
        val zipFiles = mutableMapOf<String, ByteArray>()
        ZipInputStream(inputStream).use { zis ->
            var entry: ZipEntry? = zis.nextEntry
            while (entry != null) {
                if (entry.name.startsWith("xl/")) {
                    val baos = ByteArrayOutputStream()
                    val buffer = ByteArray(4096)
                    var len: Int
                    while (zis.read(buffer).also { len = it } > 0) {
                        baos.write(buffer, 0, len)
                    }
                    zipFiles[entry.name] = baos.toByteArray()
                }
                entry = zis.nextEntry
            }
        }

        // 1. Parse sharedStrings.xml
        val sharedStrings = mutableListOf<String>()
        val sstBytes = zipFiles["xl/sharedStrings.xml"]
        if (sstBytes != null) {
            val parser = Xml.newPullParser()
            parser.setInput(ByteArrayInputStream(sstBytes), "UTF-8")
            var eventType = parser.eventType
            var inSi = false
            val currentText = StringBuilder()

            while (eventType != XmlPullParser.END_DOCUMENT) {
                val tag = parser.name
                when (eventType) {
                    XmlPullParser.START_TAG -> {
                        if (tag == "si") {
                            inSi = true
                            currentText.clear()
                        }
                    }
                    XmlPullParser.TEXT -> {
                        if (inSi) {
                            currentText.append(parser.text)
                        }
                    }
                    XmlPullParser.END_TAG -> {
                        if (tag == "si") {
                            inSi = false
                            sharedStrings.add(currentText.toString())
                        }
                    }
                }
                eventType = parser.next()
            }
        }

        // 2. Parse workbook.xml for sheet names
        val sheetNames = mutableListOf<String>()
        val wbBytes = zipFiles["xl/workbook.xml"]
        if (wbBytes != null) {
            val parser = Xml.newPullParser()
            parser.setInput(ByteArrayInputStream(wbBytes), "UTF-8")
            var eventType = parser.eventType
            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_TAG && parser.name == "sheet") {
                    val name = parser.getAttributeValue(null, "name")
                    if (!name.isNullOrBlank()) {
                        sheetNames.add(name)
                    }
                }
                eventType = parser.next()
            }
        }

        // 3. Parse each sheet
        val categories = mutableListOf<WorkoutCategory>()
        for (i in sheetNames.indices) {
            val sheetName = sheetNames[i]
            val sheetFileName = "xl/worksheets/sheet${i + 1}.xml"
            val sheetBytes = zipFiles[sheetFileName] ?: continue

            val rows = parseSheetRows(sheetBytes, sharedStrings)
            if (rows.isEmpty()) continue

            var cue: String? = null
            var startIndex = 0
            val firstRowA = rows.firstOrNull()?.get("A")?.trim().orEmpty()
            if (firstRowA.length > 15) {
                cue = firstRowA
                startIndex = 1
            }

            val exercises = mutableListOf<WorkoutExercise>()
            for (r in startIndex until rows.size) {
                val rowCells = rows[r]
                val name = rowCells["A"]?.trim().orEmpty()
                if (name.isBlank() || name == "None") continue

                val c2 = rowCells["B"]?.trim().orEmpty()
                val c3 = rowCells["C"]?.trim().orEmpty()
                val c4 = rowCells["D"]?.trim().orEmpty()
                val c5 = rowCells["E"]?.trim().orEmpty()

                val videoUrl = extractVideoUrl(c5) ?: extractVideoUrl(c4) ?: extractVideoUrl(c3) ?: extractVideoUrl(c2)

                var weight = ""
                var sets = 4
                var reps = "12次"
                var setupTips: String? = null

                // Determine setupTips from the 3rd column (C) or 4th column (D)
                if (c3.isNotBlank() && extractVideoUrl(c3) == null) {
                    if (isPureSetsReps(c3)) {
                        val parsed = parseSetsReps(c3)
                        sets = parsed.first
                        reps = parsed.second
                        if (c4.isNotBlank() && extractVideoUrl(c4) == null && !isPureSetsReps(c4)) {
                            setupTips = c4
                        }
                    } else {
                        setupTips = c3
                    }
                } else if (c4.isNotBlank() && extractVideoUrl(c4) == null && !isPureSetsReps(c4)) {
                    setupTips = c4
                }

                if (sheetName.contains("腹")) {
                    if (name.contains("平板支撑")) {
                        sets = 2
                        reps = "40s"
                        weight = ""
                    } else if (isPureSetsReps(c2)) {
                        val parsed = parseSetsReps(c2)
                        sets = parsed.first
                        reps = parsed.second
                        weight = ""
                    } else {
                        weight = c2
                        if (isPureSetsReps(c3)) {
                            val parsed = parseSetsReps(c3)
                            sets = parsed.first
                            reps = parsed.second
                        }
                    }
                } else if (sheetName.contains("有氧")) {
                    weight = c2
                    sets = 4
                    reps = if (name.contains("爬坡")) "爬坡" else "有氧"
                } else {
                    if (isPureSetsReps(c2)) {
                        val parsed = parseSetsReps(c2)
                        sets = parsed.first
                        reps = parsed.second
                        weight = ""
                    } else {
                        weight = c2
                        if (isPureSetsReps(c3)) {
                            val parsed = parseSetsReps(c3)
                            sets = parsed.first
                            reps = parsed.second
                        }
                    }
                }

                if (weight == "None") weight = ""

                val targetMuscle = inferTargetMuscle(name, sheetName)
                val equipment = inferEquipment(name)

                exercises.add(
                    WorkoutExercise(
                        name = name,
                        weight = weight,
                        sets = sets,
                        reps = reps,
                        videoUrl = videoUrl,
                        targetMuscle = targetMuscle,
                        equipment = equipment,
                        setupTips = setupTips
                    )
                )
            }

            categories.add(WorkoutCategory(categoryName = sheetName, cue = cue, exercises = exercises))
        }

        return WorkoutPlan(categories)
    }

    private fun parseSheetRows(sheetBytes: ByteArray, sharedStrings: List<String>): List<Map<String, String>> {
        val rows = mutableListOf<Map<String, String>>()
        val parser = Xml.newPullParser()
        parser.setInput(ByteArrayInputStream(sheetBytes), "UTF-8")
        var eventType = parser.eventType

        var currentRow = mutableMapOf<String, String>()
        var currentCellRef: String? = null
        var currentCellType: String? = null
        var inValue = false
        val currentValText = StringBuilder()

        while (eventType != XmlPullParser.END_DOCUMENT) {
            val tag = parser.name
            when (eventType) {
                XmlPullParser.START_TAG -> {
                    if (tag == "row") {
                        currentRow = mutableMapOf()
                    } else if (tag == "c") {
                        currentCellRef = parser.getAttributeValue(null, "r")
                        currentCellType = parser.getAttributeValue(null, "t")
                        currentValText.clear()
                    } else if (tag == "v") {
                        inValue = true
                        currentValText.clear()
                    }
                }
                XmlPullParser.TEXT -> {
                    if (inValue) {
                        currentValText.append(parser.text)
                    }
                }
                XmlPullParser.END_TAG -> {
                    if (tag == "v") {
                        inValue = false
                    } else if (tag == "c") {
                        val colLetter = currentCellRef?.replace(Regex("[0-9]"), "") ?: ""
                        var valStr = currentValText.toString().trim()
                        if (currentCellType == "s" && valStr.isNotEmpty()) {
                            val idx = valStr.toIntOrNull()
                            if (idx != null && idx in sharedStrings.indices) {
                                valStr = sharedStrings[idx]
                            }
                        }
                        if (colLetter.isNotEmpty()) {
                            currentRow[colLetter] = valStr
                        }
                    } else if (tag == "row") {
                        if (currentRow.isNotEmpty()) {
                            rows.add(currentRow)
                        }
                    }
                }
            }
            eventType = parser.next()
        }
        return rows
    }

    private fun extractVideoUrl(text: String?): String? {
        if (text.isNullOrBlank()) return null
        val matcher = VIDEO_URL_PATTERN.matcher(text)
        return if (matcher.find()) matcher.group() else null
    }

    private fun parseSetsReps(text: String?): Pair<Int, String> {
        if (text.isNullOrBlank()) return 4 to "12次"

        val mTime = TIME_SETS_PATTERN.matcher(text)
        if (mTime.find()) {
            val sets = mTime.group(2)?.toIntOrNull() ?: 2
            val reps = mTime.group(1) ?: "40s"
            return sets to reps
        }

        val m = SETS_REPS_PATTERN.matcher(text)
        if (m.find()) {
            val reps = (m.group(1) ?: "12") + "次"
            val sets = m.group(2)?.toIntOrNull() ?: 4
            return sets to reps
        }

        if (text.contains("后三中三")) {
            return 4 to "后三中三"
        }

        return 4 to text
    }

    private fun isPureSetsReps(text: String?): Boolean {
        if (text.isNullOrBlank()) return false
        val t = text.trim()
        return t.matches(Regex("^[0-9]+[×x*X][0-9]+$")) ||
               t.matches(Regex("^[0-9]+s[×x*X][0-9]+$")) ||
               t.matches(Regex("^[0-9]+[次组]$")) ||
               t == "后三中三"
    }

    fun inferEquipment(name: String): String {
        return when {
            name.contains("高位下拉") -> "高位下拉机"
            name.contains("划船") -> "坐姿划船机"
            name.contains("面拉") -> "龙门架绳索"
            name.contains("引体") -> "辅助引体向上机"
            name.contains("腿弯举") -> "俯卧腿弯举机"
            name.contains("髋外展") -> "坐姿髋外展机"
            name.contains("臀推") -> "史密斯机 + 卧推凳"
            name.contains("倒蹬") -> "45度倒蹬机"
            name.contains("髋内收") -> "坐姿髋内收机"
            name.contains("小腿") -> "阶梯/拉伸板"
            name.contains("平板支撑") -> "瑜伽垫"
            name.contains("卷腹机") -> "卷腹机"
            name.contains("屈腿") -> "平行双杠/垫上"
            name.contains("卷腹凳") -> "下斜卷腹凳"
            name.contains("推胸") && name.contains("上斜") -> "史密斯机 + 30度上斜凳"
            name.contains("推胸") -> "坐姿推胸机"
            name.contains("夹胸") -> "蝴蝶机"
            name.contains("推举") || name.contains("肩推") -> "坐姿肩推机"
            name.contains("侧平举") -> "哑铃"
            name.contains("飞鸟") -> "蝴蝶机反向"
            name.contains("爬坡") -> "跑步机"
            else -> "专业器械"
        }
    }

    fun inferTargetMuscle(name: String, categoryName: String): String {
        return when {
            name.contains("高位下拉") || name.contains("引体") -> "背阔肌"
            name.contains("划船") -> "中背、菱形肌、中下斜方肌"
            name.contains("面拉") || name.contains("飞鸟") -> "三角肌后束、肩胛稳定肌群"
            name.contains("腿弯举") -> "腘绳肌/大腿后侧"
            name.contains("髋外展") -> "臀中肌"
            name.contains("臀推") -> "臀大肌"
            name.contains("倒蹬") -> "臀大肌 + 大腿"
            name.contains("髋内收") -> "大腿内收肌群"
            name.contains("小腿") -> "小腿/足踝"
            name.contains("平板支撑") -> "核心/腹直肌"
            name.contains("卷腹") -> "腹直肌"
            name.contains("屈腿") -> "下腹/核心"
            name.contains("推胸") && name.contains("上斜") -> "胸大肌上部"
            name.contains("推胸") || name.contains("夹胸") -> "胸大肌"
            name.contains("推举") -> "三角肌"
            name.contains("侧平举") -> "三角肌中束"
            categoryName.contains("背") -> "背部肌群"
            categoryName.contains("臀") || categoryName.contains("腿") -> "臀腿肌群"
            categoryName.contains("胸") -> "胸大肌"
            categoryName.contains("肩") -> "三角肌"
            categoryName.contains("腹") -> "腹部核心"
            else -> categoryName
        }
    }
}
