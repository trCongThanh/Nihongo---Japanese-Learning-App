package com.example.nihongo.User.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.School
import androidx.compose.ui.graphics.vector.ImageVector

sealed class BottomNavItem(
    val route: String,
    val label: String,
    val icon: ImageVector
) {
    object Home : BottomNavItem("home", "Home", Icons.Filled.Home)
    object Courses : BottomNavItem("courses", "Courses", Icons.Filled.School)
    object Vocabulary : BottomNavItem("vocabulary", "Vocabulary", Icons.Filled.Book)
    object Community : BottomNavItem("community", "Community", Icons.Filled.Group)
    object Profile : BottomNavItem("profile", "Profile", Icons.Filled.Person)

    fun createRoute(userEmail: String): String = "$route/$userEmail"
    val routePattern: String get() = "$route/{user_email}"
}


