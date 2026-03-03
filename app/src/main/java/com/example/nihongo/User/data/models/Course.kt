package com.example.nihongo.User.data.models

data class Course(
    val id: String = "",        // Firestore Document ID
    val title: String = "",
    val description: String = "",
    val rating: Double = 0.0,
    val reviews: Int = 0,
    val likes: Int = 0,
    val dislikes: Int = 0,
    val imageRes: String = "",
    val vip: Boolean = false
)
