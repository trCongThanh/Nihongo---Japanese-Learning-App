package com.example.nihongo.User.data.models

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class User(
    val id: String = "",
    val username: String = "",
    val email: String = "",
    val password: String = "",
    val imageUrl: String = "",
    val createdAt: String = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault()).format(Date()),
    val vip: Boolean = false,
    val isLoggedIn: Boolean = false,
    val activityPoints: Int = 0,
    val rank: String = "Tân binh",
    val jlptLevel: Int? = null,
    val studyMonths: Int? = null,
    val
    online: Boolean = false,
    val partners: List<String> = emptyList(),
    val admin: Boolean = false
) {
    // Hàm tính toán rank dựa trên điểm năng động
    fun calculateRank(): String {
        return when {
            activityPoints >= 1000 -> "Bậc thầy"
            activityPoints >= 750 -> "Chuyên gia"
            activityPoints >= 500 -> "Cao cấp"
            activityPoints >= 300 -> "Trung cấp"
            activityPoints >= 150 -> "Sơ cấp"
            activityPoints >= 50 -> "Người mới"
            else -> "Tân binh"
        }
    }
    
    // Hàm cập nhật trạng thái online
    fun updateOnlineStatus(isOnline: Boolean): User {
        return this.copy(online = isOnline)
    }
    
    // Hàm thêm đối tác học tập
    fun addPartner(partnerId: String): User {
        if (partners.contains(partnerId)) return this
        val newPartners = partners.toMutableList().apply { add(partnerId) }
        return this.copy(partners = newPartners)
    }
    
    // Hàm xóa đối tác học tập
    fun removePartner(partnerId: String): User {
        if (!partners.contains(partnerId)) return this
        val newPartners = partners.toMutableList().apply { remove(partnerId) }
        return this.copy(partners = newPartners)
    }
    
    // Hàm thêm điểm năng động
    fun addActivityPoints(points: Int): User {
        val newPoints = activityPoints + points
        val newRank = calculateRank()
        return this.copy(
            activityPoints = newPoints,
            rank = newRank
        )
    }
}
