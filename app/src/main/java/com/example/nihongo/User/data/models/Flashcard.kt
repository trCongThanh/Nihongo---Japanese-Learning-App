package com.example.nihongo.User.data.models

data class Flashcard(
    val id: String = "",               // Firestore Document ID
    val exerciseId: String ="",              // Firestore lessonId là String
    val term: String ="",
    val definition: String="",
    val example: String? = null,       // Ví dụ sử dụng từ
    val pronunciation: String? = null, // Cách đọc từ
    val audioUrl: String? = null,      // Âm thanh phát âm
    val imageUrl: String? = null       // Hình minh họa từ
)
