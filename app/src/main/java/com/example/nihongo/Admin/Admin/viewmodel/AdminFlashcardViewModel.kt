package com.example.nihongo.Admin.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nihongo.User.data.models.Flashcard
import com.example.nihongo.User.data.repository.FlashcardRepository
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.toObject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.UUID

class AdminFlashcardViewModel : ViewModel() {
    private val repository = FlashcardRepository(FirebaseFirestore.getInstance())

    // State for flashcard list
    private val _flashcards = MutableStateFlow<List<Flashcard>>(emptyList())
    val flashcards: StateFlow<List<Flashcard>> = _flashcards.asStateFlow()

    // Currently selected flashcard for editing
    private val _selectedFlashcard = MutableStateFlow<Flashcard?>(null)
    val selectedFlashcard: StateFlow<Flashcard?> = _selectedFlashcard.asStateFlow()

    // Loading state
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // Error handling
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    // Filter by exercise type
    private val _currentExerciseFilter = MutableStateFlow<String?>(null)
    val currentExerciseFilter: StateFlow<String?> = _currentExerciseFilter.asStateFlow()

    // Available exercise types
    val exerciseTypes = listOf(
        "hiragana_basic",
        "katakana_basic",
        "kanji_n5",
        "vocabulary_n5"
    )

    // Search query
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // CRUD Operations

    // Read - Load flashcards, optionally filtered by exerciseId
    fun loadFlashcards(exerciseId: String? = null) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _errorMessage.value = null
                _currentExerciseFilter.value = exerciseId

                val flashcardList = if (exerciseId != null) {
                    repository.getFlashcardsByExerciseId(exerciseId)
                } else {
                    repository.getAllFlashcards()
                }

                _flashcards.value = flashcardList
            } catch (e: Exception) {
                _errorMessage.value = "Failed to load flashcards: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    // Read - Get a single flashcard by ID
    fun getFlashcardById(id: String) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _errorMessage.value = null

                val flashcard = repository.getFlashcardById(id)
                _selectedFlashcard.value = flashcard
            } catch (e: Exception) {
                _errorMessage.value = "Failed to load flashcard: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }


    fun createFlashcard(
        exerciseId: String,
        term: String,
        definition: String,
        example: String? = null,
        pronunciation: String? = null,
        audioUrl: String? = null,
        imageUrl: String? = null
    ) {
        if (term.isBlank() || definition.isBlank()) {
            _errorMessage.value = "Term and definition cannot be empty"
            return
        }

        viewModelScope.launch {
            try {
                _isLoading.value = true
                _errorMessage.value = null

                val newFlashcard = Flashcard(
                    id = UUID.randomUUID().toString(),  // Tự tạo ID
                    exerciseId = exerciseId,
                    term = term,
                    definition = definition,
                    example = example,
                    pronunciation = pronunciation,
                    audioUrl = audioUrl,
                    imageUrl = imageUrl
                )

                repository.addFlashcard(newFlashcard) // Truyền kèm ID vào đây

                loadFlashcards(_currentExerciseFilter.value)
                _errorMessage.value = "Flashcard created successfully"
            } catch (e: Exception) {
                _errorMessage.value = "Failed to create flashcard: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }


    // Update - Edit existing flashcard
    fun updateFlashcard(
        id: String,
        exerciseId: String,
        term: String,
        definition: String,
        example: String? = null,
        pronunciation: String? = null,
        audioUrl: String? = null,
        imageUrl: String? = null
    ) {
        if (term.isBlank() || definition.isBlank()) {
            _errorMessage.value = "Term and definition cannot be empty"
            return
        }

        viewModelScope.launch {
            try {
                _isLoading.value = true
                _errorMessage.value = null

                val updatedFlashcard = Flashcard(
                    id = id,
                    exerciseId = exerciseId,
                    term = term,
                    definition = definition,
                    example = example,
                    pronunciation = pronunciation,
                    audioUrl = audioUrl,
                    imageUrl = imageUrl
                )

                repository.updateFlashcard(updatedFlashcard)

                // Refresh the list and clear selection
                _selectedFlashcard.value = null
                loadFlashcards(_currentExerciseFilter.value)
                _errorMessage.value = "Flashcard updated successfully"
            } catch (e: Exception) {
                _errorMessage.value = "Failed to update flashcard: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    // Delete - Remove flashcard
    fun deleteFlashcard(id: String) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _errorMessage.value = null

                repository.deleteFlashcard(id)

                // Refresh the list and clear selection if the deleted item was selected
                if (_selectedFlashcard.value?.id == id) {
                    _selectedFlashcard.value = null
                }
                loadFlashcards(_currentExerciseFilter.value)
                _errorMessage.value = "Flashcard deleted successfully"
            } catch (e: Exception) {
                _errorMessage.value = "Failed to delete flashcard: ${e.message}"
                Log.d("TestDebug", "Error loading flashcards: ${e}")
            } finally {
                _isLoading.value = false
            }
        }
    }

    // Select a flashcard for editing
    fun selectFlashcard(flashcard: Flashcard) {
        _selectedFlashcard.value = flashcard
    }

    // Clear selected flashcard
    fun clearSelectedFlashcard() {
        _selectedFlashcard.value = null
    }

    // Search functionality
    fun setSearchQuery(query: String) {
        _searchQuery.value = query
        searchFlashcards()
    }

    // Search flashcards based on term or definition
    private fun searchFlashcards() {
        viewModelScope.launch {
            try {
                if (_searchQuery.value.isBlank()) {
                    // If search is empty, just load normal filtered list
                    loadFlashcards(_currentExerciseFilter.value)
                    return@launch
                }

                _isLoading.value = true

                // First get all flashcards (or filtered by exercise)
                val allCards = if (_currentExerciseFilter.value != null) {
                    repository.getFlashcardsByExerciseId(_currentExerciseFilter.value!!)
                } else {
                    repository.getAllFlashcards()
                }

                // Then filter by search query
                val query = _searchQuery.value.lowercase()
                val filteredCards = allCards.filter { flashcard ->
                    flashcard.term.lowercase().contains(query) ||
                            flashcard.definition.lowercase().contains(query) ||
                            flashcard.example?.lowercase()?.contains(query) == true ||
                            flashcard.pronunciation?.lowercase()?.contains(query) == true
                }

                _flashcards.value = filteredCards
            } catch (e: Exception) {
                _errorMessage.value = "Search failed: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    // Clear error message
    fun clearError() {
        _errorMessage.value = null
    }

    // Switch exercise filter
    fun setExerciseFilter(exerciseId: String?) {
        loadFlashcards(exerciseId)
    }

}

// Extension function for FlashcardRepository to handle additional needed functions
suspend fun FlashcardRepository.getAllFlashcards(): List<Flashcard> {
    val firestore = FirebaseFirestore.getInstance()
    val querySnapshot = firestore.collection("flashcards").get().await()
    return querySnapshot.documents.mapNotNull { it.toObject(Flashcard::class.java) }
}

suspend fun FlashcardRepository.getFlashcardsByExerciseId(exerciseId: String): List<Flashcard> {
    val firestore = FirebaseFirestore.getInstance()
    val querySnapshot = firestore.collection("flashcards")
        .whereEqualTo("exerciseId", exerciseId)
        .get()
        .await()
    return querySnapshot.documents.mapNotNull { it.toObject(Flashcard::class.java) }
}

suspend fun FlashcardRepository.updateFlashcard(flashcard: Flashcard) {
    val firestore = FirebaseFirestore.getInstance()
    val documentRef = firestore.collection("flashcards").document(flashcard.id)
    documentRef.set(flashcard).await()
}

suspend fun FlashcardRepository.deleteFlashcard(id: String) {
    val firestore = FirebaseFirestore.getInstance()
    val documentRef = firestore.collection("flashcards").document(id)
    documentRef.delete().await()
}
