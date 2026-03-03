package com.example.nihongo.User.data.models

data class LearningGoal(
    var id: String = "",
    val title: String = "",
    val description: String = "",
    val target: Int = 0,
    val current: Int = 0,
    val type: String = "", // daily, weekly, vocabulary, etc.
    val startDate: Long = 0,
    val endDate: Long = 0
)