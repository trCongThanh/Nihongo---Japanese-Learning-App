package com.example.nihongo.User.data.repository

import com.example.nihongo.User.data.models.Flashcard
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.toObject
import kotlinx.coroutines.tasks.await

class FlashcardRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    private val flashcardsCollection = firestore.collection("flashcards")

    // Lấy tất cả flashcard thuộc bài học
    suspend fun getFlashcardsByLessonId(lessonId: String): List<Flashcard> {
        val querySnapshot = flashcardsCollection
            .whereEqualTo("lessonId", lessonId)  // Lọc theo lessonId
            .get()
            .await()

        return querySnapshot.documents.mapNotNull { it.toObject<Flashcard>() }
    }

    // Lấy flashcard theo ID
    suspend fun getFlashcardById(flashcardId: String): Flashcard? {
        val documentSnapshot = flashcardsCollection.document(flashcardId).get().await()
        return if (documentSnapshot.exists()) {
            documentSnapshot.toObject<Flashcard>()
        } else null
    }

    // Thêm một flashcard mới
    suspend fun addFlashcard(flashcard: Flashcard) {
        val flashcardRef = flashcardsCollection.document(flashcard.id.ifEmpty { flashcardsCollection.document().id })
        flashcardRef.set(flashcard).await()
    }
}
