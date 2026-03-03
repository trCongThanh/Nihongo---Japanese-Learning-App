package com.example.nihongo.User.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Help
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

@Composable
fun TopBarIcon(selectedItem: String) {
    val icon = when (selectedItem) {
        "home" -> Icons.Filled.Home
        "courses" -> Icons.Filled.School
        "vocabulary" -> Icons.Filled.Book
        "community" -> Icons.Filled.Group
        "profile" -> Icons.Filled.Person
        else -> Icons.Filled.Help
    }

    IconButton(onClick = { /* optional: add some action if needed */ }) {
        Icon(
            imageVector = icon,
            contentDescription = selectedItem,
            tint = Color(0xFF4CAF50)
        )
    }
}
