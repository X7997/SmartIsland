package com.agupta07505.smartisland.model

data class WorkoutExercise(
    val name: String,
    val weight: String = "",
    val sets: Int = 4,
    val reps: String = "12次",
    val videoUrl: String? = null,
    val targetMuscle: String? = null,
    val equipment: String? = null,
    val setupTips: String? = null
)

data class WorkoutCategory(
    val categoryName: String,
    val cue: String? = null,
    val exercises: List<WorkoutExercise> = emptyList()
)

data class WorkoutPlan(
    val categories: List<WorkoutCategory> = emptyList()
)

data class FitnessSessionState(
    val isActive: Boolean = false,
    val categoryName: String = "",
    val cue: String? = null,
    val currentExerciseIndex: Int = 0,
    val currentExercise: WorkoutExercise? = null,
    val currentSet: Int = 1,
    val totalSets: Int = 4,
    val isResting: Boolean = false,
    val restSecondsRemaining: Int = 60,
    val totalRestSeconds: Int = 60,
    val totalExercisesCount: Int = 0,
    val exerciseSecondsRemaining: Int = 0,
    val totalExerciseSeconds: Int = 0,
    val setElapsedSeconds: Int = 0
)
