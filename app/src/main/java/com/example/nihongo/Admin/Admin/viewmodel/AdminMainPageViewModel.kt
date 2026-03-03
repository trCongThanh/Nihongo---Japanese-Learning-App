package com.example.nihongo.Admin.viewmodel

import android.util.Log
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nihongo.User.data.models.Course
import com.example.nihongo.User.data.models.User
import com.example.nihongo.User.data.models.UserProgress
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.coroutineScope

class AdminMainPageViewModel : ViewModel() {
    // Firestore instance
    private val firestore = FirebaseFirestore.getInstance()

    // StateFlows for UI state
    private val _monthlyUserCount = MutableStateFlow<Map<String, Int>>(emptyMap())
    val monthlyUserCount: StateFlow<Map<String, Int>> = _monthlyUserCount

    private val _topUsers = MutableStateFlow<List<User>>(emptyList())
    val topUsers: StateFlow<List<User>> = _topUsers

    private val _topCourses = MutableStateFlow<List<Course>>(emptyList())
    val topCourses: StateFlow<List<Course>> = _topCourses

    private val _totalCourseCount = MutableStateFlow(0)
    val totalCourseCount: StateFlow<Int> = _totalCourseCount

    private val _vipMemberCount = MutableStateFlow(0)
    val vipMemberCount: StateFlow<Int> = _vipMemberCount

    private val _mostEnrolledCourses = MutableStateFlow<List<CourseEnrollment>>(emptyList())
    val mostEnrolledCourses: StateFlow<List<CourseEnrollment>> = _mostEnrolledCourses

    // Loading state
    val isLoading = mutableStateOf(false)

    // Data class to hold course enrollment information
    data class CourseEnrollment(
        val courseId: String = "",
        val title: String = "",
        val enrollmentCount: Int = 0
    )

    // Initialize and load all data
    init {
        loadAllData()
    }

    fun loadAllData() {
        viewModelScope.launch {
            isLoading.value = true
            try {
                // Load all statistics in parallel for better performance
                coroutineScope {
                    launch { getMonthlyUserCounts() }
                    launch { getTopUsersByActivityPoints() }
                    launch { getMostLikedCourses() }
                    launch { getTotalCourseCount() }
                    launch { getVipMemberCount() }
                    launch { getMostEnrolledCourses() }
                }
            } catch (e: Exception) {
                Log.e("AdminViewModel", "Error loading data", e)
            } finally {
                isLoading.value = false
            }
        }
    }

    private suspend fun getMonthlyUserCounts() {
        try {
            // Get current date and calculate date 6 months ago
            val calendar = Calendar.getInstance()
            val endDate = calendar.time
            calendar.add(Calendar.MONTH, -6)
            val startDate = calendar.time

            // Get all users
            val users = firestore.collection("users")
                .get()
                .await()
                .documents
                .mapNotNull { it.toObject(User::class.java) }

            // Format for month names
            val monthFormat = SimpleDateFormat("MMM", Locale.getDefault())

            // Initialize result map with all months
            val result = mutableMapOf<String, Int>()

            // Fill all months with 0 initially
            calendar.time = startDate
            while (calendar.time.before(endDate) || calendar.time == endDate) {
                val monthName = monthFormat.format(calendar.time)
                result[monthName] = 0
                calendar.add(Calendar.MONTH, 1)
            }

            // Count users per month based on creation date
            val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault())

            for (user in users) {
                try {
                    val creationDate = dateFormat.parse(user.createdAt)
                    if (creationDate != null && !creationDate.before(startDate) && !creationDate.after(endDate)) {
                        val month = monthFormat.format(creationDate)
                        result[month] = (result[month] ?: 0) + 1
                    }
                } catch (e: Exception) {
                    Log.e("AdminViewModel", "Error parsing date: ${user.createdAt}", e)
                }
            }

            _monthlyUserCount.value = result
        } catch (e: Exception) {
            Log.e("AdminViewModel", "Error getting monthly user counts", e)
        }
    }

    private suspend fun getTopUsersByActivityPoints(limit: Int = 5) {
        try {
            val users = firestore.collection("users")
                .orderBy("activityPoints", Query.Direction.DESCENDING) //activityPoints
                .limit(limit.toLong())
                .get()
                .await()
                .documents
                .mapNotNull { it.toObject(User::class.java) }

            _topUsers.value = users
        } catch (e: Exception) {
            Log.e("AdminViewModel", "Error getting top users", e)
        }
    }

    private suspend fun getMostLikedCourses(limit: Int = 5) {
        try {
            val courses = firestore.collection("courses")
                .orderBy("likes", Query.Direction.DESCENDING)
                .limit(limit.toLong())
                .get()
                .await()
                .documents
                .mapNotNull { it.toObject(Course::class.java) }

            _topCourses.value = courses
        } catch (e: Exception) {
            Log.e("AdminViewModel", "Error getting most liked courses", e)
        }
    }

    private suspend fun getTotalCourseCount() {
        try {
            val snapshot = firestore.collection("courses")
                .get()
                .await()

            _totalCourseCount.value = snapshot.size()
        } catch (e: Exception) {
            Log.e("AdminViewModel", "Error getting total course count", e)
        }
    }

    private suspend fun getVipMemberCount() {
        try {
            val snapshot = firestore.collection("users")
                .whereEqualTo("vip", true)
                .get()
                .await()

            _vipMemberCount.value = snapshot.size()
        } catch (e: Exception) {
            Log.e("AdminViewModel", "Error getting VIP member count", e)
        }
    }

    private suspend fun getMostEnrolledCourses(limit: Int = 5) {
        try {
            // Lấy tất cả khóa học
            val courses = firestore.collection("courses")
                .get()
                .await()
                .documents
                .mapNotNull {
                    val course = it.toObject(Course::class.java)
                    course?.copy(id = it.id)
                }

            val enrollmentMap = mutableMapOf<String, Int>() // courseId -> số lượng user

            // Lấy danh sách tất cả userId từ collection "users"
            val userIds = getAllUserIds()

            for (userId in userIds) {
                val courseDocs = firestore.collection("user_progress")
                    .document(userId)
                    .collection("courses")
                    .get()
                    .await()
                    .documents

                for (courseDoc in courseDocs) {
                    val courseId = courseDoc.id
                    enrollmentMap[courseId] = (enrollmentMap[courseId] ?: 0) + 1
                }
            }

            val courseEnrollments = courses.map {
                CourseEnrollment(
                    courseId = it.id,
                    title = it.title,
                    enrollmentCount = enrollmentMap[it.id] ?: 0
                )
            }

            _mostEnrolledCourses.value = courseEnrollments
                .sortedByDescending { it.enrollmentCount }
                .take(limit)

        } catch (e: Exception) {
            Log.e("AdminViewModel", "Error getting most enrolled courses", e)
        }
    }

    private suspend fun getAllUserIds(): List<String> {
        return try {
            firestore.collection("users")
                .get()
                .await()
                .documents
                .map { it.id }
        } catch (e: Exception) {
            Log.e("AdminViewModel", "Error getting user IDs", e)
            emptyList()
        }
    }


    // Original methods
    suspend fun getAllUserProgress(userId: String): List<UserProgress> {
        return try {
            val snapshot = firestore.collection("user_progress")
                .document(userId)
                .collection("courses")
                .get()
                .await()

            snapshot.documents.mapNotNull { it.toObject(UserProgress::class.java) }
        } catch (e: Exception) {
            Log.e("UserProgress", "Error getting all user progress", e)
            emptyList()
        }
    }

    suspend fun getUserProgressForCourse(userId: String, courseId: String): UserProgress? {
        return try {
            val snapshot = firestore.collection("user_progress")
                .document(userId)
                .collection("courses")
                .document(courseId)
                .get()
                .await()

            snapshot.toObject(UserProgress::class.java)
        } catch (e: Exception) {
            Log.e("UserProgress", "Error getting user progress for course", e)
            null
        }
    }
}