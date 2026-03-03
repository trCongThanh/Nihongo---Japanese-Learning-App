package com.example.nihongo.User.ui.screens.login

import android.util.Log
import android.util.Patterns
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.nihongo.User.data.models.User
import com.example.nihongo.User.data.repository.UserRepository
import com.example.nihongo.utils.EmailSender
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

@Composable
fun RegisterScreen(navController: NavController, userRepo: UserRepository) {
    val scope = rememberCoroutineScope()
    var username by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("") }
    var isSendingOtp by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, Color(0xFFB0EACD), RoundedCornerShape(16.dp))
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("Tạo tài khoản", style = MaterialTheme.typography.headlineMedium)

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = username,
                onValueChange = { username = it; message = "" },
                label = { Text("Tên người dùng") },
                singleLine = true
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = email,
                onValueChange = { email = it; message = "" },
                label = { Text("Email") },
                singleLine = true,
                isError = email.isNotBlank() && !Patterns.EMAIL_ADDRESS.matcher(email).matches()
            )

            if (email.isNotBlank() && !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                Text("Email không hợp lệ!", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = password,
                onValueChange = { password = it; message = "" },
                label = { Text("Mật khẩu") },
                singleLine = true,
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    val icon = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff
                    val description = if (passwordVisible) "Ẩn mật khẩu" else "Hiện mật khẩu"
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(imageVector = icon, contentDescription = description)
                    }
                }
            )

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = {
                    scope.launch {
                        if (username.isBlank()) {
                            message = "Tên người dùng không được để trống!"
                            return@launch
                        }

                        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                            message = "Email không hợp lệ!"
                            return@launch
                        }

                        if (password.length < 6) {
                            message = "Mật khẩu phải có ít nhất 6 ký tự!"
                            return@launch
                        }

                        val createdAt = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()).format(Date())
                        val user = User(
                            id = UUID.randomUUID().toString(),
                            username = username,
                            email = email,
                            password = password,
                            createdAt = createdAt,
                            vip = false
                        )

                        Log.d("RegisterScreen", "Creating user: id=${user.id}, username=${user.username}, email=${user.email}")

                        val success = userRepo.registerUser(user)
                        Log.d("RegisterScreen", "Registration result: $success")

                        message = if (success) {
                            isSendingOtp = true
                            val otp = EmailSender.generateOTP()
                            withContext(Dispatchers.IO) {
                                EmailSender.sendOTP(
                                    recipientEmail = email,
                                    otp = otp,
                                    onSuccess = {
                                        scope.launch {
                                            navController.currentBackStackEntry?.savedStateHandle?.apply {
                                                set("expectedOtp", otp)
                                                set("user_email", email)
                                            }
                                            navController.navigate("otp_screen")
                                            isSendingOtp = false
                                        }
                                    },
                                    onFailure = {
                                        it.printStackTrace()
                                        Log.e("EmailSender", "Lỗi gửi OTP: ${it.message}", it)
                                        scope.launch {
                                            message = "Không thể gửi OTP: ${it.localizedMessage ?: "Lỗi không xác định"}"
                                            isSendingOtp = false
                                        }
                                    }
                                )
                            }
                            "Gửi OTP thành công!"
                        } else {
                            "Tài khoản đã tồn tại!"
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF388E3C))
            ) {
                Text(if (isSendingOtp) "Đang gửi OTP..." else "Đăng ký")
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (message.isNotEmpty()) {
                Text(
                    text = message,
                    color = if (message.contains("thành công")) Color(0xFF4CAF50) else Color.Red,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            TextButton(onClick = {
                navController.navigate("login")
            }) {
                Text("← Quay lại đăng nhập")
            }
        }
    }
}
