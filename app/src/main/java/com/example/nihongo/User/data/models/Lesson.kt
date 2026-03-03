package com.example.nihongo.User.data.models

data class Lesson(
    val id: String = "",               // ID bài học
    val courseId: String = "",         // ID khóa học
    val step: Int = 0,                 // Bước số
    val stepTitle: String = "",        // Tên bước: "Bước 1: Cùng tìm hiểu về Bảng chữ cái trong tiếng Nhật"
    val overview: String = "",         // Tổng quan: "Tổng quan về chữ cái tiếng Nhật"
    val totalUnits: Int = 0,           // Tổng số Unit
    val completedUnits: Int = 0,       // Số Unit đã hoàn thành
    val units: List<UnitItem> = listOf()  // Danh sách các bài trong bài học
)

data class UnitItem(
    val unitTitle: String = "",          // Unit 1: Hiragana 1
    val progress: String = "",           // "6/9"
    val subLessons: List<SubLesson> = listOf() // Các bài học nhỏ trong unit
)

data class SubLesson(
    val id: String = "",
    val title: String = "",              // [B1][Video] Hàng あ và hàng か
    val type: String = "",               // Video / Quiz / Luyện tập
    val isCompleted: Boolean = false     // Trạng thái hoàn thành
)
