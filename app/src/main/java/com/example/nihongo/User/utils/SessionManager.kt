package com.example.nihongo.User.utils

import android.content.Context
import android.content.SharedPreferences
import com.example.nihongo.User.data.models.User
import com.google.gson.Gson

class SessionManager(context: Context) {
    private val sharedPreferences: SharedPreferences = 
        context.getSharedPreferences("NihongoSession", Context.MODE_PRIVATE)
    private val editor: SharedPreferences.Editor = sharedPreferences.edit()
    private val gson = Gson()

    companion object {
        private const val KEY_IS_LOGGED_IN = "isLoggedIn"
        private const val KEY_USER_DATA = "userData"
    }

    fun createLoginSession(user: User) {
        editor.putBoolean(KEY_IS_LOGGED_IN, true)
        editor.putString(KEY_USER_DATA, gson.toJson(user))
        editor.apply()
    }

    fun logoutUser() {
        editor.clear()
        editor.apply()
    }

    fun isLoggedIn(): Boolean {
        return sharedPreferences.getBoolean(KEY_IS_LOGGED_IN, false)
    }

    fun getUserDetails(): User? {
        val userJson = sharedPreferences.getString(KEY_USER_DATA, null) ?: return null
        return gson.fromJson(userJson, User::class.java)
    }
}