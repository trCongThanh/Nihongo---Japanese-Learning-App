package com.example.nihongo.User.data.models

import com.google.firebase.Timestamp

data class PrivateChatMessage(
    var id: String = "",
    val chatId: String = "",
    val senderId: String = "",
    val receiverId: String = "",
    val senderName: String = "",
    val senderImageUrl: String = "",
    val content: String = "",
    val timestamp: Timestamp? = null,
    val read: Boolean = false
)