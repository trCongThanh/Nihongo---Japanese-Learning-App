package com.example.nihongo.User.data.models

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class CourseReview(
    val id: String = "",
    val userId: String = "",
    val courseId: String = "",
    val text: String = "",
    val rating: Int = 5,
    val timestamp: Long = 0,
    val userName: String = "",
    val userAvatar: String = ""
) {
    val formattedTimestamp: String
        get() = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(timestamp))
}