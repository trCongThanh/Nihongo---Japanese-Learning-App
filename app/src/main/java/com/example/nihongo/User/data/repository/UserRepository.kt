package com.example.nihongo.User.data.repository

import android.util.Log
import com.example.nihongo.User.data.models.User
import com.example.nihongo.User.data.models.UserProgress
import com.example.nihongo.User.utils.SessionManager
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.toObject
import kotlinx.coroutines.tasks.await
import java.security.MessageDigest

class UserRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val sessionManager: SessionManager? = null
) {
    private var currentUser: User? = null
    private val usersCollection = firestore.collection("users")

    // ===== USER AUTH =====
    suspend fun registerUser(user: User): Boolean {
        try {
            Log.d("UserRepository", "Starting registration for username: ${user.username}, email: ${user.email}")
            
            // Check for existing username
            val existingUsername = getUserByUsername(user.username)
            if (existingUsername != null) {
                Log.d("UserRepository", "Registration failed: Username already exists")
                return false
            }
            
            // Check for existing email
            val existingEmail = getUserByEmail(user.email)
            if (existingEmail != null) {
                Log.d("UserRepository", "Registration failed: Email already exists")
                return false
            }
            
            // Check if user ID is valid
            if (user.id.isBlank()) {
                Log.e("UserRepository", "Registration failed: User ID is blank")
                return false
            }
            
            // Proceed with registration
            val hashedUser = user.copy(password = hashPassword(user.password))
            Log.d("UserRepository", "Saving user with ID: ${hashedUser.id}")
            
            usersCollection.document(hashedUser.id).set(hashedUser).await()
            currentUser = hashedUser
            sessionManager?.createLoginSession(hashedUser)
            
            Log.d("UserRepository", "Registration successful for user: ${user.username}")
            return true
        } catch (e: Exception) {
            Log.e("UserRepository", "Registration error", e)
            return false
        }
    }


    suspend fun loginUserByEmail(email: String, password: String): User? {
        val hashedPassword = hashPassword(password)
        val querySnapshot = usersCollection
            .whereEqualTo("email", email)
            .whereEqualTo("password", hashedPassword)
            .get()
            .await()

        val user = querySnapshot.documents.firstOrNull()?.toObject<User>()
        currentUser = user
        user?.let { sessionManager?.createLoginSession(it) }
        return user
    }

    suspend fun getUserByEmail(email: String): User? {
        val querySnapshot = usersCollection
            .whereEqualTo("email", email)
            .get()
            .await()

        val document = querySnapshot.documents.firstOrNull()

        // Log raw Firestore document data
        Log.d("UserRepository", "Raw document data: ${document?.data}")

        val user = document?.toObject<User>()

        // Log mapped User object
        Log.d("UserRepository", "Mapped User object: $user")

        return user
    }

    suspend fun getAllUsers(): List<User> {
        return try {
            val querySnapshot = usersCollection.get().await()
            querySnapshot.documents.mapNotNull { it.toObject<User>() }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    fun isVip(): Boolean {
        return getCurrentUser()?.vip == true
    }

    fun getCurrentUser(): User? {
        return currentUser
    }

    private suspend fun getUserByUsername(username: String): User? {
        val querySnapshot = usersCollection
            .whereEqualTo("username", username)
            .get()
            .await()

        return querySnapshot.documents.firstOrNull()?.toObject<User>()
    }

    private fun hashPassword(password: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(password.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }

    fun logout() {
        currentUser = null
        sessionManager?.logoutUser()
    }

    // ===== USER PROGRESS =====
    suspend fun saveUserProgress(userId: String, userProgress: UserProgress) {
        try {
            val progressRef = firestore.collection("user_progress")
                .document(userId)
                .collection("courses")
                .document(userProgress.courseId)

            progressRef.set(userProgress).await()
            Log.d("UserProgress", "User progress saved successfully")
        } catch (e: Exception) {
            Log.e("UserProgress", "Error saving user progress", e)
        }
    }

    suspend fun updateUserProgress(
        userId: String,
        courseId: String,
        exerciseId: String,
        passed: Boolean,
        subLessonId: String? = null
    ) {
        try {
            val progressRef = firestore.collection("user_progress")
                .document(userId)
                .collection("courses")
                .document(courseId)

            val progressSnapshot = progressRef.get().await()

            if (progressSnapshot.exists()) {
                val userProgress = progressSnapshot.toObject(UserProgress::class.java) ?: return

                val updatedCompletedExercises = userProgress.completedExercises.toMutableList()
                val updatedPassedExercises = userProgress.passedExercises.toMutableList()
                val updatedCompletedSubLessons = userProgress.completedSubLessons?.toMutableList() ?: mutableListOf()

                if (!updatedCompletedExercises.contains(exerciseId)) {
                    updatedCompletedExercises.add(exerciseId)
                }

                if (passed && !updatedPassedExercises.contains(exerciseId)) {
                    updatedPassedExercises.add(exerciseId)
                }

                // Thêm subLessonId vào completedSubLessons nếu được cung cấp và chưa có trong danh sách
                if (subLessonId != null && !updatedCompletedSubLessons.contains(subLessonId)) {
                    updatedCompletedSubLessons.add(subLessonId)
                    Log.d("UserProgress", "Added subLessonId $subLessonId to completedSubLessons")
                }

                val updatedProgress = userProgress.copy(
                    completedExercises = updatedCompletedExercises,
                    passedExercises = updatedPassedExercises,
                    completedSubLessons = updatedCompletedSubLessons,
                    lastUpdated = System.currentTimeMillis()
                )

                progressRef.set(updatedProgress).await()
                Log.d("UserProgress", "User exercise progress updated with completedSubLessons: ${updatedCompletedSubLessons.size}")
            }
        } catch (e: Exception) {
            Log.e("UserProgress", "Error updating user progress", e)
        }
    }

    suspend fun markLessonAsCompleted(userId: String, courseId: String, lessonId: String, totalLessons: Int) {
        try {
            val progressRef = firestore.collection("user_progress")
                .document(userId)
                .collection("courses")
                .document(courseId)

            val snapshot = progressRef.get().await()
            if (!snapshot.exists()) return

            val userProgress = snapshot.toObject(UserProgress::class.java) ?: return
            val completedLessons = userProgress.completedLessons.toMutableList()

            if (!completedLessons.contains(lessonId)) {
                completedLessons.add(lessonId)
            }

            val updatedProgress = userProgress.copy(
                completedLessons = completedLessons,
                progress = (completedLessons.size.toFloat() / totalLessons.toFloat()),
                lastUpdated = System.currentTimeMillis()
            )
            Log.d("UserProgress", "completedLessons.size: ${completedLessons.size}")
            Log.d("UserProgress", "totalLessons: $totalLessons")
            progressRef.set(updatedProgress).await()
            Log.d("UserProgress", "Lesson marked as completed")

        } catch (e: Exception) {
            Log.e("UserProgress", "Error marking lesson as completed", e)
        }
    }

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
            // Lấy dữ liệu của khóa học cụ thể trong collection "courses"
            val snapshot = firestore.collection("user_progress")
                .document(userId)
                .collection("courses")
                .document(courseId) // Lấy document của khóa học cụ thể
                .get()
                .await()

            // Nếu tài liệu tồn tại, chuyển đổi nó thành đối tượng UserProgress
            snapshot.toObject(UserProgress::class.java)
        } catch (e: Exception) {
            Log.e("UserProgress", "Error getting user progress for course", e)
            null
        }
    }

    // Thêm hàm updateUser để cập nhật thông tin người dùng
    suspend fun updateUser(user: User) {
        try {
            // Cập nhật thông tin người dùng trong Firestore
            usersCollection.document(user.id).set(user).await()
            
            // Cập nhật currentUser trong repository
            currentUser = user
            
            // Cập nhật session nếu có SessionManager
            sessionManager?.createLoginSession(user)
            
            Log.d("UserRepository", "User updated successfully: ${user.id}")
        } catch (e: Exception) {
            Log.e("UserRepository", "Error updating user: ${e.message}")
            throw e
        }
    }

    // Lấy thông tin người dùng theo ID
    suspend fun getUserById(userId: String): User? {
        return try {
            Log.d("UserRepository", "Attempting to get user with ID: $userId")
            val documentSnapshot = usersCollection.document(userId).get().await()
            
            if (documentSnapshot.exists()) {
                val user = documentSnapshot.toObject(User::class.java)
                Log.d("UserRepository", "Successfully retrieved user: ${user?.username}")
                user
            } else {
                Log.e("UserRepository", "User document with ID $userId does not exist")
                null
            }
        } catch (e: Exception) {
            Log.e("UserRepository", "Error getting user by ID: $userId", e)
            null
        }
    }

    // Cập nhật trạng thái online của người dùng
    suspend fun updateUserOnlineStatus(userId: String, isOnline: Boolean) {
        try {
            val user = getUserById(userId) ?: return
            val updatedUser = user.updateOnlineStatus(isOnline)
            usersCollection.document(userId).update("online", isOnline).await()
            
            // Cập nhật currentUser nếu đó là người dùng hiện tại
            if (currentUser?.id == userId) {
                currentUser = updatedUser
            }
        } catch (e: Exception) {
            Log.e("UserRepository", "Error updating online status", e)
        }
    }



    // Thêm điểm năng động cho người dùng
    suspend fun addActivityPoints(userId: String, points: Int): Boolean {
        return try {
            Log.d("UserRepository", "Adding $points points to user $userId")
            
            // Lấy user từ Firestore
            val documentSnapshot = usersCollection.document(userId).get().await()
            
            if (!documentSnapshot.exists()) {
                Log.e("UserRepository", "Cannot add points: User document with ID $userId does not exist")
                return false
            }
            
            // Lấy dữ liệu user hiện tại
            val user = documentSnapshot.toObject(User::class.java)
            if (user == null) {
                Log.e("UserRepository", "Cannot add points: Failed to convert document to User object")
                return false
            }
            
            // Tính toán điểm mới và rank
            val newPoints = user.activityPoints + points
            val newRank = user.calculateRank()
            
            Log.d("UserRepository", "Updating user $userId: activityPoints from ${user.activityPoints} to $newPoints, rank to $newRank")
            
            // Cập nhật Firestore
            usersCollection.document(userId)
                .update(
                    "activityPoints", newPoints,
                    "rank", newRank
                ).await()
            
            // Cập nhật currentUser nếu đó là người dùng hiện tại
            if (currentUser?.id == userId) {
                currentUser = currentUser?.copy(
                    activityPoints = newPoints,
                    rank = newRank
                )
            }
            
            Log.d("UserRepository", "Successfully added $points points to user $userId")
            true
        } catch (e: Exception) {
            Log.e("UserRepository", "Error adding activity points to user $userId", e)
            false
        }
    }

    /**
     * Cập nhật thông tin hồ sơ người dùng
     * @param user Đối tượng User với thông tin đã được cập nhật
     * @return true nếu cập nhật thành công, false nếu có lỗi
     */
    suspend fun updateUserProfile(user: User): Boolean {
        return try {
            Log.d("UserRepository", "Updating user profile: ${user.id}")
            
            // Lấy thông tin người dùng hiện tại từ Firestore để đảm bảo dữ liệu mới nhất
            val currentUserDoc = usersCollection.document(user.id).get().await()
            val currentUserData = currentUserDoc.toObject(User::class.java)
            
            if (currentUserData == null) {
                Log.e("UserRepository", "Failed to update profile: User not found")
                return false
            }
            
            // Tạo đối tượng User mới với các trường được cập nhật
            // Chỉ cập nhật các trường liên quan đến hồ sơ, giữ nguyên các trường khác
            val updatedUser = currentUserData.copy(
                username = user.username,
                imageUrl = user.imageUrl,
                jlptLevel = user.jlptLevel,
                studyMonths = user.studyMonths
                // Các trường khác như email, password, vip, activityPoints, rank, online, partners, admin
                // sẽ được giữ nguyên từ currentUserData
            )
            
            // Cập nhật trong Firestore
            usersCollection.document(user.id).set(updatedUser).await()
            
            // Cập nhật currentUser trong repository
            currentUser = updatedUser
            
            // Cập nhật session nếu có SessionManager
            sessionManager?.createLoginSession(updatedUser)
            
            Log.d("UserRepository", "User profile updated successfully")
            true
        } catch (e: Exception) {
            Log.e("UserRepository", "Error updating user profile", e)
            false
        }
    }

    /**
     * Cập nhật URL ảnh đại diện của người dùng
     * @param userId ID của người dùng
     * @param imageUrl URL mới của ảnh đại diện
     * @return true nếu cập nhật thành công, false nếu có lỗi
     */
    suspend fun updateUserImageUrl(userId: String, imageUrl: String): Boolean {
        return try {
            Log.d("UserRepository", "Updating user image URL: $userId")
            
            // Cập nhật trong Firestore
            usersCollection.document(userId)
                .update("imageUrl", imageUrl)
                .await()
            
            // Cập nhật currentUser nếu có
            currentUser?.let {
                if (it.id == userId) {
                    currentUser = it.copy(imageUrl = imageUrl)
                    
                    // Cập nhật session nếu có SessionManager
                    sessionManager?.createLoginSession(currentUser!!)
                }
            }
            
            Log.d("UserRepository", "User image URL updated successfully")
            true
        } catch (e: Exception) {
            Log.e("UserRepository", "Error updating user image URL", e)
            false
        }
    }

    /**
     * Cập nhật mật khẩu người dùng
     * @param userId ID của người dùng
     * @param newPassword Mật khẩu mới (chưa được hash)
     * @return true nếu cập nhật thành công, false nếu có lỗi
     */
    suspend fun updateUserPassword(userId: String, newPassword: String): Boolean {
        return try {
            Log.d("UserRepository", "Updating password for user: $userId")
            
            // Hash mật khẩu mới
            val hashedPassword = hashPassword(newPassword)
            
            // Cập nhật mật khẩu trong Firestore
            usersCollection.document(userId)
                .update("password", hashedPassword)
                .await()
            
            // Cập nhật currentUser nếu đó là người dùng hiện tại
            currentUser?.let {
                if (it.id == userId) {
                    currentUser = it.copy(password = hashedPassword)
                }
            }
            
            Log.d("UserRepository", "Password updated successfully")
            true
        } catch (e: Exception) {
            Log.e("UserRepository", "Error updating password", e)
            false
        }
    }
}
