package com.example.nihongo.User.data.repository

import android.util.Log
import com.example.nihongo.User.data.models.Course
import com.example.nihongo.User.data.models.CourseReview
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.toObject
import kotlinx.coroutines.tasks.await

class CourseRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    private val coursesCollection = firestore.collection("courses")

    suspend fun getAllCourses(): List<Course> {
        val snapshot = coursesCollection.get().await()
        return snapshot.documents.mapNotNull { it.toObject<Course>() }
    }

    suspend fun insertCourse(course: Course) {
        val courseId = if (course.id.isNotEmpty()) {
            course.id
        } else {
            coursesCollection.document().id  // Firestore tự sinh ID
        }

        val courseWithId = course.copy(id = courseId)
        coursesCollection.document(courseWithId.id).set(courseWithId).await()
    }

    suspend fun getCourseById(courseId: String): Course? {
        val documentSnapshot = coursesCollection.document(courseId).get().await()
        return if (documentSnapshot.exists()) {
            documentSnapshot.toObject<Course>()
        } else null
    }

    suspend fun getCourseReviews(courseId: String): List<CourseReview> {
        return try {
            firestore.collection("courseReviews")
                .whereEqualTo("courseId", courseId)
                .get()
                .await()
                .toObjects(CourseReview::class.java)
        } catch (e: Exception) {
            Log.e("CourseRepository", "Error getting course reviews", e)
            emptyList()
        }
    }

    suspend fun getUserReviewForCourse(courseId: String, userId: String): CourseReview? {
        return try {
            firestore.collection("courseReviews")
                .whereEqualTo("courseId", courseId)
                .whereEqualTo("userId", userId)
                .get()
                .await()
                .toObjects(CourseReview::class.java)
                .firstOrNull()
        } catch (e: Exception) {
            Log.e("CourseRepository", "Error getting user review", e)
            null
        }
    }

    suspend fun addCourseReview(review: CourseReview): Boolean {
        return try {
            // Tạo ID mới cho đánh giá
            val reviewRef = firestore.collection("courseReviews").document()
            val reviewWithId = review.copy(id = reviewRef.id)
            
            // Thêm đánh giá mới
            reviewRef.set(reviewWithId).await()
            
            // Cập nhật rating trung bình của khóa học
            updateCourseRating(review.courseId)
            
            // Cập nhật số lượng reviews trong Course
            firestore.collection("courses")
                .document(review.courseId)
                .update("reviews", FieldValue.increment(1))
                .await()
            
            true
        } catch (e: Exception) {
            Log.e("CourseRepository", "Error adding course review", e)
            false
        }
    }

    suspend fun updateCourseReview(review: CourseReview): Boolean {
        return try {
            // Cập nhật đánh giá
            firestore.collection("courseReviews")
                .document(review.id)
                .set(review)
                .await()
            
            // Cập nhật rating trung bình của khóa học
            updateCourseRating(review.courseId)
            
            true
        } catch (e: Exception) {
            Log.e("CourseRepository", "Error updating course review", e)
            false
        }
    }

    private suspend fun updateCourseRating(courseId: String) {
        try {
            // Lấy tất cả đánh giá của khóa học
            val reviews = firestore.collection("courseReviews")
                .whereEqualTo("courseId", courseId)
                .get()
                .await()
                .toObjects(CourseReview::class.java)
            
            // Tính rating trung bình
            val averageRating = if (reviews.isNotEmpty()) {
                reviews.sumOf { it.rating } / reviews.size.toDouble()
            } else {
                0.0
            }
            
            // Cập nhật rating của khóa học
            firestore.collection("courses")
                .document(courseId)
                .update("rating", averageRating)
                .await()
        } catch (e: Exception) {
            Log.e("CourseRepository", "Error updating course rating", e)
        }
    }

    suspend fun isCourseLikedByUser(courseId: String, userId: String): Boolean {
        return try {
            val doc = firestore.collection("courseLikes")
                .whereEqualTo("courseId", courseId)
                .whereEqualTo("userId", userId)
                .get()
                .await()
            
            !doc.isEmpty
        } catch (e: Exception) {
            Log.e("CourseRepository", "Error checking if course is liked", e)
            false
        }
    }

    suspend fun likeCourse(courseId: String, userId: String): Boolean {
        return try {
            // Thêm like mới
            val likeData = hashMapOf(
                "courseId" to courseId,
                "userId" to userId,
                "timestamp" to FieldValue.serverTimestamp()
            )
            firestore.collection("courseLikes").add(likeData).await()
            
            // Cập nhật số lượng like của khóa học
            firestore.collection("courses")
                .document(courseId)
                .update("likes", FieldValue.increment(1))
                .await()
            
            true
        } catch (e: Exception) {
            Log.e("CourseRepository", "Error liking course", e)
            false
        }
    }
    
    suspend fun unlikeCourse(courseId: String, userId: String): Boolean {
        return try {
            // Tìm và xóa like
            val likeDoc = firestore.collection("courseLikes")
                .whereEqualTo("courseId", courseId)
                .whereEqualTo("userId", userId)
                .get()
                .await()
            
            if (!likeDoc.isEmpty) {
                // Xóa document like
                firestore.collection("courseLikes")
                    .document(likeDoc.documents[0].id)
                    .delete()
                    .await()
                
                // Giảm số lượng like của khóa học
                firestore.collection("courses")
                    .document(courseId)
                    .update("likes", FieldValue.increment(-1))
                    .await()
                
                true
            } else {
                false
            }
        } catch (e: Exception) {
            Log.e("CourseRepository", "Error unliking course", e)
            false
        }
    }
    
    suspend fun dislikeCourse(courseId: String, userId: String): Boolean {
        return try {
            // Thêm dislike mới
            val dislikeData = hashMapOf(
                "courseId" to courseId,
                "userId" to userId,
                "timestamp" to FieldValue.serverTimestamp()
            )
            firestore.collection("courseDislikes").add(dislikeData).await()
            
            // Cập nhật số lượng dislike của khóa học
            firestore.collection("courses")
                .document(courseId)
                .update("dislikes", FieldValue.increment(1))
                .await()
            
            true
        } catch (e: Exception) {
            Log.e("CourseRepository", "Error disliking course", e)
            false
        }
    }

    suspend fun removeDislike(courseId: String, userId: String): Boolean {
        return try {
            // Tìm và xóa dislike
            val dislikeDoc = firestore.collection("courseDislikes")
                .whereEqualTo("courseId", courseId)
                .whereEqualTo("userId", userId)
                .get()
                .await()
            
            if (!dislikeDoc.isEmpty) {
                // Xóa document dislike
                firestore.collection("courseDislikes")
                    .document(dislikeDoc.documents[0].id)
                    .delete()
                    .await()
                
                // Giảm số lượng dislike của khóa học
                firestore.collection("courses")
                    .document(courseId)
                    .update("dislikes", FieldValue.increment(-1))
                    .await()
                
                true
            } else {
                true // Không có dislike để xóa, vẫn trả về true
            }
        } catch (e: Exception) {
            Log.e("CourseRepository", "Error removing dislike", e)
            false
        }
    }

    suspend fun isCoursedislikedByUser(courseId: String, userId: String): Boolean {
        return try {
            val dislikeDoc = firestore.collection("courseDislikes")
                .whereEqualTo("courseId", courseId)
                .whereEqualTo("userId", userId)
                .get()
                .await()
            
            !dislikeDoc.isEmpty
        } catch (e: Exception) {
            Log.e("CourseRepository", "Error checking if course is disliked", e)
            false
        }
    }
}
