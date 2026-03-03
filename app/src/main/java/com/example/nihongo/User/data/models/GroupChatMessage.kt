package com.example.nihongo.User.data.models

import com.google.firebase.Timestamp
import java.util.UUID

data class GroupChatMessage(
    var id: String = UUID.randomUUID().toString(),
    val groupId: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val senderImageUrl: String = "",
    val content: String = "",
    val timestamp: Timestamp? = null
)