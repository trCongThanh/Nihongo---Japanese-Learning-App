package com.example.nihongo.User.data.models

data class Discussion(
    var id: String = "",
    val title: String = "",
    val content: String = "",
    val authorId: String = "",
    val authorName: String = "",
    val authorImageUrl: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val tags: List<String> = emptyList(),
    val commentCount: Int = 0  // Thêm trường này để theo dõi số lượng bình luận
)
