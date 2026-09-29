package com.agupta07505.smartisland.data

import android.content.Context
import android.media.AudioAttributes
import android.os.Build
import android.os.Environment
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import com.agupta07505.smartisland.model.FitnessSessionState
import com.agupta07505.smartisland.model.IslandMode
import com.agupta07505.smartisland.model.IslandNotification
import com.agupta07505.smartisland.model.WorkoutCategory
import com.agupta07505.smartisland.model.WorkoutExercise
import com.agupta07505.smartisland.model.WorkoutPlan
import com.agupta07505.smartisland.service.SmartIslandOverlayService
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FitnessRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val notificationRepository: INotificationRepository
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var restTimerJob: Job? = null
    private var exerciseTimerJob: Job? = null
    private var setTimerJob: Job? = null

    private val _plan = MutableStateFlow<WorkoutPlan>(WorkoutPlan())
    val plan: StateFlow<WorkoutPlan> = _plan.asStateFlow()

    private val _planSourceInfo = MutableStateFlow<String>("正在加载健身计划...")
    val planSourceInfo: StateFlow<String> = _planSourceInfo.asStateFlow()

    private val _sessionState = MutableStateFlow(FitnessSessionState())
    val sessionState: StateFlow<FitnessSessionState> = _sessionState.asStateFlow()

    init {
        loadPlan()
    }

    fun loadPlan(): String {
        return try {
            val (parsedPlan, source) = com.agupta07505.smartisland.util.FitnessPlanParser.loadCurrentPlan(context)
            _plan.value = parsedPlan

            val totalExercises = parsedPlan.categories.sumOf { it.exercises.size }
            val desc = "$source · 共 ${parsedPlan.categories.size} 个部位，${totalExercises} 个动作"
            _planSourceInfo.value = desc
            Log.d(TAG, "Loaded plan: $desc")
            desc
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load plan", e)
            val err = "健身计划加载异常: ${e.message}"
            _planSourceInfo.value = err
            err
        }
    }

    fun importPlanFromUri(uri: android.net.Uri): Result<String> {
        return com.agupta07505.smartisland.util.FitnessPlanParser.parseFromUri(context, uri).map { newPlan ->
            _plan.value = newPlan
            val totalExercises = newPlan.categories.sumOf { it.exercises.size }
            val desc = "已导入自定义健身计划 · 共 ${newPlan.categories.size} 个部位，${totalExercises} 个动作"
            _planSourceInfo.value = desc
            desc
        }
    }

    fun resetToDefaultPlan(): String {
        com.agupta07505.smartisland.util.FitnessPlanParser.deleteCustomPlan(context)
        return loadPlan()
    }

    private fun parsePlanJson(jsonStr: String): WorkoutPlan {
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

                    exercises.add(
                        WorkoutExercise(
                            name = name,
                            weight = weight,
                            sets = sets,
                            reps = reps,
                            videoUrl = videoUrl
                        )
                    )
                }
            }
            categories.add(WorkoutCategory(categoryName = catName, cue = cue, exercises = exercises))
        }
        return WorkoutPlan(categories)
    }

    private fun getExerciseDurationSeconds(exercise: WorkoutExercise?): Int {
        if (exercise == null) return 0
        if (exercise.name.contains("平板支撑")) return 40
        val match = Regex("(\\d+)\\s*s", RegexOption.IGNORE_CASE).find(exercise.reps)
        return match?.groupValues?.get(1)?.toIntOrNull() ?: 0
    }

    private fun startExerciseTimer(seconds: Int) {
        exerciseTimerJob?.cancel()
        if (seconds <= 0) {
            _sessionState.update { it.copy(exerciseSecondsRemaining = 0, totalExerciseSeconds = 0) }
            return
        }

        exerciseTimerJob = scope.launch {
            _sessionState.update {
                it.copy(
                    exerciseSecondsRemaining = seconds,
                    totalExerciseSeconds = seconds
                )
            }
            updateIslandNotification()

            for (sec in (seconds - 1) downTo 0) {
                delay(1000L)
                if (_sessionState.value.isResting || !_sessionState.value.isActive) break
                _sessionState.update { it.copy(exerciseSecondsRemaining = sec) }
                updateIslandNotification()
            }

            if (!_sessionState.value.isResting && _sessionState.value.isActive) {
                // 40s 平板支撑倒计时自然归零：触发完成震动并自动开启 60s (1 min) 组间休息
                _sessionState.update { it.copy(exerciseSecondsRemaining = 0) }
                triggerHoldCompletionVibration()
                startRestTimer(60)
            }
        }
    }

    private fun cancelExerciseTimer() {
        exerciseTimerJob?.cancel()
        exerciseTimerJob = null
        if (_sessionState.value.exerciseSecondsRemaining > 0) {
            _sessionState.update { it.copy(exerciseSecondsRemaining = 0) }
            updateIslandNotification()
        }
    }

    private fun startSetTimer() {
        setTimerJob?.cancel()
        _sessionState.update { it.copy(setElapsedSeconds = 0) }
        setTimerJob = scope.launch {
            while (_sessionState.value.isActive && !_sessionState.value.isResting && _sessionState.value.exerciseSecondsRemaining == 0) {
                delay(1000L)
                if (!_sessionState.value.isActive || _sessionState.value.isResting || _sessionState.value.exerciseSecondsRemaining > 0) break
                _sessionState.update { it.copy(setElapsedSeconds = it.setElapsedSeconds + 1) }
            }
        }
    }

    private fun cancelSetTimer() {
        setTimerJob?.cancel()
        setTimerJob = null
    }

    private fun triggerHoldCompletionVibration() {
        try {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }

            vibrator?.let {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val timings = longArrayOf(0, 200, 120, 250)
                    val amplitudes = intArrayOf(0, 255, 0, 255)
                    it.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
                } else {
                    @Suppress("DEPRECATION")
                    it.vibrate(longArrayOf(0, 200, 120, 250), -1)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Hold completion vibration failed", e)
        }
    }

    fun startWorkout(categoryName: String) {
        SmartIslandOverlayService.wakeUpOverlaySession(context)
        val currentPlan = _plan.value
        val category = currentPlan.categories.firstOrNull { it.categoryName == categoryName }
            ?: currentPlan.categories.firstOrNull()
            ?: return

        val initialExercise = category.exercises.firstOrNull()
        val duration = getExerciseDurationSeconds(initialExercise)

        _sessionState.update {
            FitnessSessionState(
                isActive = true,
                categoryName = category.categoryName,
                cue = category.cue,
                currentExerciseIndex = 0,
                currentExercise = initialExercise,
                currentSet = 1,
                totalSets = initialExercise?.sets ?: 4,
                isResting = false,
                restSecondsRemaining = 60,
                totalRestSeconds = 60,
                totalExercisesCount = category.exercises.size,
                exerciseSecondsRemaining = duration,
                totalExerciseSeconds = duration
            )
        }
        updateIslandNotification(autoExpand = true)
        if (duration > 0) {
            cancelSetTimer()
            startExerciseTimer(duration)
        } else {
            cancelExerciseTimer()
            startSetTimer()
        }
    }

    fun stopWorkout() {
        cancelRestTimer()
        cancelExerciseTimer()
        cancelSetTimer()
        _sessionState.update { it.copy(isActive = false, isResting = false, exerciseSecondsRemaining = 0, setElapsedSeconds = 0) }
        notificationRepository.removeNotification(FITNESS_NOTIFICATION_KEY)
    }

    fun nextSet() {
        val state = _sessionState.value
        if (!state.isActive) return
        val currentPlan = _plan.value
        val category = currentPlan.categories.firstOrNull { it.categoryName == state.categoryName } ?: return
        val currentExercise = state.currentExercise ?: return

        if (state.isResting) {
            // Already resting: skip rest and jump straight to the next set
            skipRestTimer()
            return
        }

        cancelExerciseTimer()
        cancelSetTimer()
        // Start 60s (1 min) rest countdown before moving to the next set or exercise
        startRestTimer(60)
    }

    fun skipRestTimer() {
        restTimerJob?.cancel()
        restTimerJob = null
        if (_sessionState.value.isResting) {
            advanceAfterRest(vibrateOnCompletion = false)
        }
    }

    private fun advanceAfterRest(vibrateOnCompletion: Boolean = true) {
        val state = _sessionState.value
        val currentPlan = _plan.value
        val category = currentPlan.categories.firstOrNull { it.categoryName == state.categoryName } ?: return
        val currentExercise = state.currentExercise ?: return

        if (state.currentSet < currentExercise.sets) {
            // Advance to next set
            val nextSetNum = state.currentSet + 1
            val duration = getExerciseDurationSeconds(currentExercise)
            _sessionState.update {
                it.copy(
                    currentSet = nextSetNum,
                    isResting = false,
                    restSecondsRemaining = 0,
                    exerciseSecondsRemaining = duration,
                    totalExerciseSeconds = duration
                )
            }
            updateIslandNotification()
            if (vibrateOnCompletion) triggerCompletionVibration()
            if (duration > 0) {
                cancelSetTimer()
                startExerciseTimer(duration)
            } else {
                cancelExerciseTimer()
                startSetTimer()
            }
        } else {
            // Finished all sets for current exercise -> advance to next exercise
            val nextIndex = state.currentExerciseIndex + 1
            if (nextIndex < category.exercises.size) {
                val nextExercise = category.exercises[nextIndex]
                val duration = getExerciseDurationSeconds(nextExercise)
                _sessionState.update {
                    it.copy(
                        currentExerciseIndex = nextIndex,
                        currentExercise = nextExercise,
                        currentSet = 1,
                        totalSets = nextExercise.sets,
                        isResting = false,
                        restSecondsRemaining = 0,
                        exerciseSecondsRemaining = duration,
                        totalExerciseSeconds = duration
                    )
                }
                updateIslandNotification()
                if (vibrateOnCompletion) triggerCompletionVibration()
                if (duration > 0) {
                    cancelSetTimer()
                    startExerciseTimer(duration)
                } else {
                    cancelExerciseTimer()
                    startSetTimer()
                }
            } else {
                // Completed all exercises for today!
                cancelExerciseTimer()
                cancelSetTimer()
                _sessionState.update {
                    it.copy(
                        isResting = false,
                        restSecondsRemaining = 0,
                        exerciseSecondsRemaining = 0,
                        setElapsedSeconds = 0
                    )
                }
                updateIslandNotification()
                if (vibrateOnCompletion) triggerCompletionVibration()
            }
        }
    }

    fun undoOrPreviousSet() {
        val state = _sessionState.value
        if (!state.isActive) return
        val currentPlan = _plan.value
        val category = currentPlan.categories.firstOrNull { it.categoryName == state.categoryName } ?: return

        if (state.isResting) {
            // 误触左滑进入休息：右滑立即取消休息，恢复至本组做组中
            cancelRestTimer()
            val duration = getExerciseDurationSeconds(state.currentExercise)
            _sessionState.update {
                it.copy(
                    isResting = false,
                    restSecondsRemaining = 0,
                    exerciseSecondsRemaining = duration,
                    totalExerciseSeconds = duration
                )
            }
            updateIslandNotification()
            if (duration > 0) {
                cancelSetTimer()
                startExerciseTimer(duration)
            } else {
                cancelExerciseTimer()
                startSetTimer()
            }
            return
        }

        // 已经进入下一组/下一动作：回退到上一组
        if (state.currentSet > 1) {
            val prevSetNum = state.currentSet - 1
            val duration = getExerciseDurationSeconds(state.currentExercise)
            _sessionState.update {
                it.copy(
                    currentSet = prevSetNum,
                    isResting = false,
                    restSecondsRemaining = 0,
                    exerciseSecondsRemaining = duration,
                    totalExerciseSeconds = duration
                )
            }
            updateIslandNotification()
            if (duration > 0) {
                cancelSetTimer()
                startExerciseTimer(duration)
            } else {
                cancelExerciseTimer()
                startSetTimer()
            }
        } else if (state.currentExerciseIndex > 0) {
            // 回退到上一个动作的最后一组
            val prevIndex = state.currentExerciseIndex - 1
            val prevExercise = category.exercises[prevIndex]
            val duration = getExerciseDurationSeconds(prevExercise)
            _sessionState.update {
                it.copy(
                    currentExerciseIndex = prevIndex,
                    currentExercise = prevExercise,
                    currentSet = prevExercise.sets,
                    totalSets = prevExercise.sets,
                    isResting = false,
                    restSecondsRemaining = 0,
                    exerciseSecondsRemaining = duration,
                    totalExerciseSeconds = duration
                )
            }
            updateIslandNotification()
            if (duration > 0) {
                cancelSetTimer()
                startExerciseTimer(duration)
            } else {
                cancelExerciseTimer()
                startSetTimer()
            }
        }
    }

    fun previousExercise() {
        val state = _sessionState.value
        if (!state.isActive) return
        val currentPlan = _plan.value
        val category = currentPlan.categories.firstOrNull { it.categoryName == state.categoryName } ?: return

        if (state.currentExerciseIndex > 0) {
            cancelRestTimer()
            val prevIndex = state.currentExerciseIndex - 1
            val prevExercise = category.exercises[prevIndex]
            val duration = getExerciseDurationSeconds(prevExercise)
            _sessionState.update {
                it.copy(
                    currentExerciseIndex = prevIndex,
                    currentExercise = prevExercise,
                    currentSet = 1,
                    totalSets = prevExercise.sets,
                    isResting = false,
                    exerciseSecondsRemaining = duration,
                    totalExerciseSeconds = duration
                )
            }
            updateIslandNotification()
            if (duration > 0) {
                cancelSetTimer()
                startExerciseTimer(duration)
            } else {
                cancelExerciseTimer()
                startSetTimer()
            }
        }
    }

    fun nextExercise() {
        val state = _sessionState.value
        if (!state.isActive) return
        val currentPlan = _plan.value
        val category = currentPlan.categories.firstOrNull { it.categoryName == state.categoryName } ?: return

        if (state.currentExerciseIndex + 1 < category.exercises.size) {
            cancelRestTimer()
            val nextIndex = state.currentExerciseIndex + 1
            val nextExercise = category.exercises[nextIndex]
            val duration = getExerciseDurationSeconds(nextExercise)
            _sessionState.update {
                it.copy(
                    currentExerciseIndex = nextIndex,
                    currentExercise = nextExercise,
                    currentSet = 1,
                    totalSets = nextExercise.sets,
                    isResting = false,
                    exerciseSecondsRemaining = duration,
                    totalExerciseSeconds = duration
                )
            }
            updateIslandNotification()
            if (duration > 0) {
                cancelSetTimer()
                startExerciseTimer(duration)
            } else {
                cancelExerciseTimer()
                startSetTimer()
            }
        }
    }

    fun startRestTimer(seconds: Int = 60) {
        restTimerJob?.cancel()
        restTimerJob = scope.launch {
            _sessionState.update {
                it.copy(
                    isResting = true,
                    restSecondsRemaining = seconds,
                    totalRestSeconds = seconds
                )
            }
            updateIslandNotification()

            for (sec in (seconds - 1) downTo 0) {
                delay(1000L)
                if (!_sessionState.value.isResting) break
                _sessionState.update { it.copy(restSecondsRemaining = sec) }
                updateIslandNotification()
            }

            if (_sessionState.value.isResting) {
                advanceAfterRest()
            }
        }
    }

    fun cancelRestTimer() {
        restTimerJob?.cancel()
        restTimerJob = null
        if (_sessionState.value.isResting) {
            _sessionState.update { it.copy(isResting = false) }
            updateIslandNotification()
            if (_sessionState.value.exerciseSecondsRemaining == 0) {
                startSetTimer()
            }
        }
    }

    private fun triggerCompletionVibration() {
        try {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }

            vibrator?.let {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    // 30s 休息结束，触发连续 5 次节奏分明、强感透彻的连震提醒开始下一组
                    val timings = longArrayOf(0, 200, 180, 200, 180, 200, 180, 200, 180, 350)
                    val amplitudes = intArrayOf(0, 255, 0, 255, 0, 255, 0, 255, 0, 255)
                    val effect = VibrationEffect.createWaveform(timings, amplitudes, -1)
                    val attrs = AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .build()
                    it.vibrate(effect, attrs)
                } else {
                    @Suppress("DEPRECATION")
                    it.vibrate(longArrayOf(0, 200, 180, 200, 180, 200, 180, 200, 180, 350), -1)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Vibration failed", e)
        }
    }

    private fun updateIslandNotification(autoExpand: Boolean = false) {
        val state = _sessionState.value
        if (!state.isActive) return
        val exercise = state.currentExercise ?: return

        val title = "${exercise.name} (第${state.currentSet}/${state.totalSets}组)"
        val text = if (state.isResting) {
            "休息: ${state.restSecondsRemaining}s"
        } else if (state.exerciseSecondsRemaining > 0) {
            "支撑中: ${state.exerciseSecondsRemaining}s / ${state.totalExerciseSeconds}s"
        } else {
            "目标: ${exercise.reps} ${exercise.weight}".trim()
        }

        val notif = IslandNotification(
            key = FITNESS_NOTIFICATION_KEY,
            packageName = context.packageName,
            appName = "健身伴侣",
            title = title,
            text = text,
            timeMillis = System.currentTimeMillis(),
            mode = IslandMode.Fitness,
            progress = if (state.isResting) state.restSecondsRemaining else if (state.exerciseSecondsRemaining > 0) state.exerciseSecondsRemaining else state.currentSet,
            progressMax = if (state.isResting) state.totalRestSeconds else if (state.exerciseSecondsRemaining > 0) state.totalExerciseSeconds else state.totalSets
        )
        notificationRepository.postNotification(notif, autoExpand = autoExpand)
    }

    companion object {
        private const val TAG = "FitnessRepository"
        const val FITNESS_NOTIFICATION_KEY = "gym_fitness_companion"
    }
}
