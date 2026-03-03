package com.example.nihongo.User.data.repository

import com.example.nihongo.User.data.models.Exercise
import com.example.nihongo.User.data.models.ExerciseType
import com.example.nihongo.User.data.models.Lesson
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import android.util.Log

class ExerciseRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    suspend fun getExercisesBySubLessonId(subLessonId: String, lessonId: String): List<Exercise> {
        val exercisesCollection = firestore.collection("lessons")
            .document(lessonId)
            .collection("exercises")
        return try {
            val snapshot = exercisesCollection
                .whereEqualTo("subLessonId", subLessonId)  // Truy vấn theo subLessonId
                .get()
                .await()

            snapshot.documents.mapNotNull { it.toObject(Exercise::class.java) }
        } catch (e: Exception) {
            emptyList() // Trả về list rỗng nếu lỗi
        }
    }
    suspend fun getPracticeExercisesExcludingFirstSubLesson(lessonId: String): List<Exercise> {
        return try {
            Log.d("ExerciseRepo", "Getting practice exercises for lessonId: $lessonId")
            
            // 1. Lấy tất cả các units cho lessonId
            val unitsSnapshot = firestore.collection("lessons")
                .document(lessonId)
                .get()
                .await()

            // 2. Lấy subLessons từ units
            val units = unitsSnapshot.toObject(Lesson::class.java)?.units ?: emptyList()
            Log.d("ExerciseRepo", "Found ${units.size} units for lessonId: $lessonId")
            
            // 3. Lọc ra các subLessonId không kết thúc bằng "-1"
            val filteredSubLessonIds = units
                .flatMap { it.subLessons }
                .filterNot { it.id.endsWith("-1") }
                .map { it.id }
            
            Log.d("ExerciseRepo", "Filtered subLessonIds: $filteredSubLessonIds")

            // 4. Lấy tất cả exercises thuộc các subLessonId đã lọc
            val exercisesSnapshot = firestore.collection("lessons")
                .document(lessonId)
                .collection("exercises")
                .get()
                .await()
            
            Log.d("ExerciseRepo", "Found ${exercisesSnapshot.documents.size} exercises in total")
            
            // 5. Lọc và chuyển đổi các exercises
            val exercises = exercisesSnapshot.documents.mapNotNull { doc ->
                try {
                    val exercise = doc.toObject(Exercise::class.java)?.copy(id = doc.id)
                    
                    // Log each exercise
                    Log.d("ExerciseRepo", "Exercise: id=${exercise?.id}, subLessonId=${exercise?.subLessonId}, " +
                            "type=${exercise?.type}")
                    
                    // Chỉ lấy các exercises có subLessonId nằm trong danh sách đã lọc
                    if (exercise != null && filteredSubLessonIds.contains(exercise.subLessonId)) {
                        exercise
                    } else {
                        Log.d("ExerciseRepo", "Skipping exercise with subLessonId: ${exercise?.subLessonId}")
                        null
                    }
                } catch (e: Exception) {
                    Log.e("ExerciseRepo", "Error converting exercise document: ${e.message}")
                    null
                }
            }
            
            Log.d("ExerciseRepo", "Returning ${exercises.size} filtered exercises")
            exercises
        } catch (e: Exception) {
            Log.e("ExerciseRepo", "Error getting practice exercises: ${e.message}")
            emptyList()
        }
    }
    fun getQuizExerciseIds(lessonId: String) {
        val db = FirebaseFirestore.getInstance()
        val exercisesRef = db.collection("lessons").document(lessonId).collection("exercises")

        // Truy vấn các bài tập từ Firestore
        exercisesRef.get().addOnSuccessListener { querySnapshot ->
            // Duyệt qua các tài liệu và lấy ID của từng tài liệu
            val exerciseIds = querySnapshot.documents.map { it.id }

            // In ra tất cả ID của các bài tập
            println("Quiz Exercise IDs: $exerciseIds")
        }.addOnFailureListener { exception ->
            println("Error getting exercises: $exception")
        }
    }


}
