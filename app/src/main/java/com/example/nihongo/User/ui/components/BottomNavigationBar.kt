package com.example.nihongo.User.ui.components

import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.navigation.NavController


@Composable
fun BottomNavigationBar(
    navController: NavController,
    selectedItem: String,
    userEmail: String,
    onItemSelected: (String) -> Unit
) {
    val items = listOf(
        BottomNavItem.Home,
        BottomNavItem.Courses,
        BottomNavItem.Vocabulary,
        BottomNavItem.Community,
        BottomNavItem.Profile
    )

    NavigationBar(containerColor = Color.White) {
        items.forEach { item ->
            val currentRoute = item.createRoute(userEmail)
            val isSelected = selectedItem == item.route

            NavigationBarItem(
                icon = {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = item.label,
                        tint = if (isSelected) Color(0xFF4CAF50) else Color.Gray
                    )
                },
                label = {
                    Text(
                        text = item.label,
                        color = if (isSelected) Color(0xFF4CAF50) else Color.Gray
                    )
                },
                selected = isSelected,
                onClick = {
                    if (!isSelected) {
                        onItemSelected(item.route)
                        navController.navigate(currentRoute) {
                            popUpTo(navController.graph.startDestinationId) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                }
            )
        }
    }
}
