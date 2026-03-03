package com.example.nihongo.Admin

import android.content.Context
import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.nihongo.Admin.viewmodel.AdminUserViewModel
import com.onesignal.OneSignal

@Composable
fun AdminLoginScreen(navController: NavController) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    
    val viewModel = remember { AdminUserViewModel() }
    val context = LocalContext.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Admin Login", style = MaterialTheme.typography.headlineMedium, color = Color(0xFF2E7D32))

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = username,
            onValueChange = {
                username = it
                message = ""
            },
            label = { Text("Email Admin") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = password,
            onValueChange = {
                password = it
                message = ""
            },
            label = { Text("Mật khẩu") },
            visualTransformation = PasswordVisualTransformation(),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading
        )

        Spacer(modifier = Modifier.height(20.dp))

        Button(
            onClick = {
                if (username.isBlank() && password.isBlank()) {
                    // Cho phép vào nhưng không lưu session
                    navController.navigate("MainPage") {
                        popUpTo("admin_login") { inclusive = true }
                    }
                    return@Button
                }

                if (username.isBlank() || password.isBlank()) {
                    message = "Vui lòng nhập đầy đủ thông tin!"
                    return@Button
                }
                
                isLoading = true
                viewModel.checkLogin(username, password) { success ->
                    isLoading = false
                    if (success) {
                        val sharedPref = context.getSharedPreferences("admin_session", Context.MODE_PRIVATE)
                        sharedPref.edit().putBoolean("isLoggedIn", true).apply()
                    // Thiết lập tag "admin" = "true" cho OneSignal
                        OneSignal.User.addTag("admin", "true")
                        Log.d("AdminLogin", "Set OneSignal tag: admin=true")
                        navController.navigate("MainPage") {
                            popUpTo("admin_login") { inclusive = true }
                        }
                    } else {
                        message = "Sai email hoặc mật khẩu!"
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF388E3C)),
            enabled = !isLoading
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = Color.White
                )
            } else {
                Text("Đăng nhập")
            }
        }

        if (message.isNotEmpty()) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(message, color = Color.Red)
        }
    }
}
