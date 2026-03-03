package com.example.nihongo.Admin.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nihongo.User.data.models.Course
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.toObject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class AdminCourseViewModel : ViewModel() {
    private val db = FirebaseFirestore.getInstance()
    private val coursesCollection = db.collection("courses")

    private val _courses = MutableStateFlow<List<Course>>(emptyList())
    val courses: StateFlow<List<Course>> get() = _courses

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> get() = _isLoading

    init {
        fetchCourses()
    }

    fun fetchCourses() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val snapshot = coursesCollection.get().await()
                Log.d("CoursePage", "Documents size: ${snapshot.documents.size}")

                val courseList = snapshot.documents.mapNotNull { document ->
                    val data = document.data ?: return@mapNotNull null
                    Log.d("CoursePage", "Raw data: $data")
                    try {
                        Course(
                            id = document.id,
                            title = data["title"] as? String ?: "",
                            description = data["description"] as? String ?: "",
                            rating = (data["rating"] as? Number)?.toDouble() ?: 0.0,
                            reviews = (data["reviews"] as? Number)?.toInt() ?: 0,
                            likes = (data["likes"] as? Number)?.toInt() ?: 0,
                            imageRes = data["imageRes"]?.toString() ?: "https://drive.google.com/uc?export=view&id=1uyNSW54w4stVjixb9ke_rOFhiGaeekEN",
                            vip = data["vip"] as? Boolean ?: false
                        )
                    } catch (e: Exception) {
                        e.printStackTrace()
                        null
                    }
                }
                _courses.value = courseList
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }


    fun addCourse(course: Course) {
        viewModelScope.launch {
            try {
                val newDoc = coursesCollection.document()
                val newCourse = course.copy(id = newDoc.id)
                newDoc.set(newCourse).await()
                fetchCourses()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun updateCourse(course: Course) {
        viewModelScope.launch {
            try {
                Log.d("CoursePage", "Updating course with ID: ${course.id}")

                val docRef = coursesCollection.document(course.id)
                docRef.set(course).await()

                fetchCourses()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun deleteCourse(course: Course) {
        viewModelScope.launch {
            try {
                Log.d("CoursePage", "Deleting course with ID: ${course.id}")
                val docRef = coursesCollection.document(course.id)
                docRef.delete().await()

                fetchCourses()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
