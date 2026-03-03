package com.example.nihongo.User.data.models

data class UserProgress(
    val id: String? = null,
    val userId: String = "",
    val courseId: String = "",
    val courseTitle: String = "",
    val completedLessons: List<String> = emptyList(),
    val completedExercises: List<String> = emptyList(),
    val passedExercises: List<String> = emptyList(),
    val completedSubLessons: List<String> = emptyList(),
    val currentLessonId: String? = null,
    val totalLessons: Int = 0,
    val totalExercises: Int = 0,
    val progress: Float = 0f,
    val lastUpdated: Long = System.currentTimeMillis()
)
