package com.example.nihongo.User.data.models

import com.google.firebase.Timestamp

data class DiscussionMessage(
    var id: String = "",
    val discussionId: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val senderImageUrl: String = "",
    val content: String = "",
    val timestamp: Timestamp? = null,
    val attachmentUrl: String? = null
)