 package com.example.nihongo.User.data.models

 import com.google.firebase.Timestamp

 data class StudyGroup(
     var id: String = "",
     val title: String = "",
     val description: String = "",
     val imageUrl: String = "",
     val creatorId: String = "",
     val createdBy: String = "",
     val createdAt: Long = 0,  // Đổi từ Timestamp? sang Long
     val lastActivity: Timestamp? = null,
     val members: List<String> = emptyList(),
     val memberCount: Int = 0
)


