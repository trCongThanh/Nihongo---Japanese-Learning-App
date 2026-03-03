package com.example.nihongo.Admin.viewmodel

import android.util.Log
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nihongo.User.data.models.Lesson
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import androidx.lifecycle.viewModelScope
import com.example.nihongo.User.data.models.Course
import com.example.nihongo.User.data.models.SubLesson
import com.example.nihongo.User.data.models.UnitItem
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class AdminLessonViewModel : ViewModel() {
    private val firestore = FirebaseFirestore.getInstance()

    // State flows for course and lessons
    private val _course = MutableStateFlow<Course?>(null)
    val course: StateFlow<Course?> = _course.asStateFlow()

    private val _lessons = MutableStateFlow<List<Lesson>>(emptyList())
    val lessons: StateFlow<List<Lesson>> = _lessons.asStateFlow()

    // State for lesson being edited
    private val _currentEditingLesson = MutableStateFlow<Lesson?>(null)
    val currentEditingLesson: StateFlow<Lesson?> = _currentEditingLesson.asStateFlow()

    // Loading and error states
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    // Fetch course by ID
    fun fetchCourse(courseId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val courseDoc = firestore.collection("courses").document(courseId).get().await()
                _course.value = courseDoc.toObject(Course::class.java)
            } catch (e: Exception) {
                _errorMessage.value = "Failed to load course: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    // Fetch lessons for a course
    fun fetchLessons(courseId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val lessonsSnapshot = firestore.collection("lessons")
                    .whereEqualTo("courseId", courseId)
                    .get()
                    .await()

                val lessonsList = lessonsSnapshot.documents.mapNotNull { doc ->
                    doc.toObject(Lesson::class.java)
                }

                _lessons.value = lessonsList.sortedBy { it.step }
            } catch (e: Exception) {
                _errorMessage.value = "Failed to load lessons: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    // Set current lesson for editing
    fun setCurrentEditingLesson(lesson: Lesson?) {
        _currentEditingLesson.value = lesson
    }

    // Create a new lesson
    fun createLesson(courseId: String, lesson: Lesson) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                addLessonToCourse(courseId, lesson)
                // Refresh lessons after creating
                fetchLessons(courseId)
                _errorMessage.value = null
            } catch (e: Exception) {
                _errorMessage.value = "Failed to create lesson: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    // Update an existing lesson
    fun updateLesson(lesson: Lesson) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val courseId = lesson.courseId
                val lessonWithUpdatedSubLessonIds = generateSubLessonIds(lesson)

                firestore.collection("lessons")
                    .document(lesson.id)
                    .set(lessonWithUpdatedSubLessonIds)
                    .await()

                // Refresh lessons after updating
                fetchLessons(courseId)
                _errorMessage.value = null
            } catch (e: Exception) {
                _errorMessage.value = "Failed to update lesson: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    // Delete a lesson
    fun deleteLesson(lesson: Lesson) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val courseId = lesson.courseId

                firestore.collection("lessons")
                    .document(lesson.id)
                    .delete()
                    .await()

                // Refresh lessons after deleting
                fetchLessons(courseId)
                _errorMessage.value = null
            } catch (e: Exception) {
                _errorMessage.value = "Failed to delete lesson: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    // Create a new unit in a lesson
    fun addUnitToLesson(lessonId: String, unit: UnitItem) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val lesson = _lessons.value.find { it.id == lessonId }

                if (lesson != null) {
                    val updatedUnits = lesson.units.toMutableList()
                    updatedUnits.add(unit)

                    val updatedLesson = lesson.copy(
                        units = updatedUnits,
                        totalUnits = updatedUnits.size
                    )

                    updateLesson(updatedLesson)
                } else {
                    _errorMessage.value = "Lesson not found"
                }
            } catch (e: Exception) {
                _errorMessage.value = "Failed to add unit: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    // Delete a unit from a lesson
    fun deleteUnitFromLesson(lessonId: String, unitIndex: Int) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val lesson = _lessons.value.find { it.id == lessonId }

                if (lesson != null && unitIndex < lesson.units.size) {
                    val updatedUnits = lesson.units.toMutableList()
                    updatedUnits.removeAt(unitIndex)

                    val updatedLesson = lesson.copy(
                        units = updatedUnits,
                        totalUnits = updatedUnits.size
                    )

                    updateLesson(updatedLesson)
                } else {
                    _errorMessage.value = "Lesson or unit not found"
                }
            } catch (e: Exception) {
                _errorMessage.value = "Failed to delete unit: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    // Add a sub-lesson to a unit
    fun addSubLessonToUnit(lessonId: String, unitIndex: Int, subLesson: SubLesson) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val lesson = _lessons.value.find { it.id == lessonId }

                if (lesson != null && unitIndex < lesson.units.size) {
                    val updatedUnits = lesson.units.toMutableList()
                    val unit = updatedUnits[unitIndex]

                    val updatedSubLessons = unit.subLessons.toMutableList()
                    updatedSubLessons.add(subLesson)

                    updatedUnits[unitIndex] = unit.copy(
                        subLessons = updatedSubLessons,
                        progress = "0/${updatedSubLessons.size}"
                    )

                    val updatedLesson = lesson.copy(units = updatedUnits)
                    updateLesson(updatedLesson)
                } else {
                    _errorMessage.value = "Lesson or unit not found"
                }
            } catch (e: Exception) {
                _errorMessage.value = "Failed to add sub-lesson: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    // Delete a sub-lesson from a unit
    fun deleteSubLesson(lessonId: String, unitIndex: Int, subLessonIndex: Int) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val lesson = _lessons.value.find { it.id == lessonId }

                if (lesson != null && unitIndex < lesson.units.size) {
                    val updatedUnits = lesson.units.toMutableList()
                    val unit = updatedUnits[unitIndex]

                    if (subLessonIndex < unit.subLessons.size) {
                        val updatedSubLessons = unit.subLessons.toMutableList()
                        updatedSubLessons.removeAt(subLessonIndex)

                        updatedUnits[unitIndex] = unit.copy(
                            subLessons = updatedSubLessons,
                            progress = "0/${updatedSubLessons.size}"
                        )

                        val updatedLesson = lesson.copy(units = updatedUnits)
                        updateLesson(updatedLesson)
                    } else {
                        _errorMessage.value = "Sub-lesson not found"
                    }
                } else {
                    _errorMessage.value = "Lesson or unit not found"
                }
            } catch (e: Exception) {
                _errorMessage.value = "Failed to delete sub-lesson: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    // Create new empty lesson template
    fun createEmptyLesson(courseId: String): Lesson {
        return Lesson(
            id = "",
            courseId = courseId,
            step = (_lessons.value.maxOfOrNull { it.step } ?: 0) + 1,
            stepTitle = "New Lesson",
            overview = "Lesson overview...",
            totalUnits = 0,
            completedUnits = 0,
            units = emptyList()
        )
    }

    // Create empty unit template
    fun createEmptyUnit(): UnitItem {
        return UnitItem(
            unitTitle = "New Unit",
            progress = "0/0",
            subLessons = emptyList()
        )
    }

    // Create empty sub-lesson template
    fun createEmptySubLesson(): SubLesson {
        return SubLesson(
            id = "",
            title = "New Sub-Lesson",
            type = "Practice",
            isCompleted = false
        )
    }

    // Reset error message
    fun clearError() {
        _errorMessage.value = null
    }
    // Hàm push Firestore
    suspend fun addLessonToCourse(courseId: String, lesson: Lesson) {
        val firestore = FirebaseFirestore.getInstance()
        val lessonsCollection = firestore.collection("lessons")

        val generatedId = lesson.id.ifEmpty { lessonsCollection.document().id }
        val lessonWithId = lesson.copy(id = generatedId, courseId = courseId)

        // Sinh ID cho SubLesson
        val finalLesson = generateSubLessonIds(lessonWithId)

        // Push lesson lên Firestore
        lessonsCollection.document(generatedId).set(finalLesson).await()
    }


    fun generateSubLessonIds(lesson: Lesson): Lesson {
        val firestore = FirebaseFirestore.getInstance()

        val updatedUnits = lesson.units.map { unit ->
            val updatedSubLessons = unit.subLessons.map { subLesson ->
                val generatedId = subLesson.id.ifEmpty { firestore.collection("sublessons").document().id }
                subLesson.copy(id = generatedId)
            }
            unit.copy(subLessons = updatedSubLessons)
        }

        return lesson.copy(units = updatedUnits)
    }
}

