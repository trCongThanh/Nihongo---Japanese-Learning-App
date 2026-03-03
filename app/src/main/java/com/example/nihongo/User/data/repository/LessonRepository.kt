package com.example.nihongo.User.data.repository

import com.example.nihongo.User.data.models.Lesson
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.toObject
import kotlinx.coroutines.tasks.await

class LessonRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    private val lessonsCollection = firestore.collection("lessons")

    // Lấy tất cả bài học thuộc khóa học
    suspend fun getLessonsByCourseId(courseId: String): List<Lesson> {
        val firestore = FirebaseFirestore.getInstance()
        val lessonsSnapshot = firestore.collection("lessons")
            .whereEqualTo("courseId", courseId)
            .orderBy("step") // Sắp xếp theo thứ tự bài học
            .get()
            .await()

        return lessonsSnapshot.toObjects(Lesson::class.java)
    }




    // Lấy bài học theo ID
    suspend fun getLessonById(lessonId: String): Lesson? {
        val documentSnapshot = lessonsCollection.document(lessonId).get().await()
        return if (documentSnapshot.exists()) {
            documentSnapshot.toObject<Lesson>()
        } else null
    }


}