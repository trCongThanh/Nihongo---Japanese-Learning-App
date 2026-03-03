package com.example.nihongo.Admin.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nihongo.User.data.models.User
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.security.MessageDigest

class AdminUserViewModel : ViewModel() {

    private val _users = MutableStateFlow<List<User>>(emptyList())
    val users: StateFlow<List<User>> = _users

    private val db = FirebaseFirestore.getInstance()

    init {
        fetchUsers()
    }

    private fun fetchUsers() {
        viewModelScope.launch {
            db.collection("users").get()
                .addOnSuccessListener { result ->
                    Log.d("UserCardDebug", result.documents.toString())
                    val userList = result.mapNotNull { it.toObject(User::class.java) }
                    userList.forEach { user ->
                        Log.d("UserCardDebug", "Fetched user: $user") // In ra dạng data class
                    }
                    _users.value = userList
                }
                .addOnFailureListener { exception ->
                    // handle error (e.g., log)
                }
        }
    }
    fun addUser(user: User) {
        val userWithId = user.copy(id = user.id.ifEmpty { db.collection("users").document().id })
        db.collection("users").document(userWithId.id)
            .set(userWithId)
            .addOnSuccessListener {
                fetchUsers()
            }
            .addOnFailureListener {
                // Handle error
            }
    }

    fun updateUser(user: User) {
        db.collection("users").document(user.id)
            .set(user)
            .addOnSuccessListener {
                fetchUsers()
            }
            .addOnFailureListener {
                // Handle error
            }
    }
    fun deleteUser(user: User) {
        db.collection("users").document(user.id)
            .delete()
            .addOnSuccessListener {
                fetchUsers()
            }
            .addOnFailureListener {
                // Handle error
            }
    }
    fun checkLogin(email: String, password: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                db.collection("users")
                    .whereEqualTo("email", email)
                    .whereEqualTo("admin", true)
                    .get()
                    .addOnSuccessListener { documents ->
                        if (documents.isEmpty) {
                            onResult(false)
                            return@addOnSuccessListener
                        }

                        val admin = documents.documents[0].toObject(User::class.java)
                        val hashedInput = sha256(password)

                        if (admin != null && admin.password == hashedInput) {
                            onResult(true)
                        } else {
                            onResult(false)
                        }
                    }
                    .addOnFailureListener {
                        onResult(false)
                    }
            } catch (e: Exception) {
                onResult(false)
            }
        }
    }
    fun sha256(input: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }

    fun getCurrentAdmin(onResult: (User?) -> Unit) {
        viewModelScope.launch {
            try {
                db.collection("users")
                    .whereEqualTo("admin", true)
                    .get()
                    .addOnSuccessListener { documents ->
                        if (!documents.isEmpty) {
                            val admin = documents.documents[0].toObject(User::class.java)
                            onResult(admin)
                        } else {
                            onResult(null)
                        }
                    }
                    .addOnFailureListener {
                        onResult(null)
                    }
            } catch (e: Exception) {
                onResult(null)
            }
        }
    }
}
