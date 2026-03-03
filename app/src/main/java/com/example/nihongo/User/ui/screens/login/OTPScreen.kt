package com.example.nihongo.User.ui.screens.login

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.nihongo.User.utils.NavigationRoutes

@Composable
fun OTPScreen(navController: NavController, expectedOtp: String, user_email: String)
 {
    val backgroundColor = Color(0xFFE8F5E9)
    val focusManager = LocalFocusManager.current
    val previousOtpValues = remember { mutableStateListOf("", "", "", "", "", "") }
    val otpValues = remember { mutableStateListOf("", "", "", "", "", "") }
    val focusRequesters = List(6) { FocusRequester() }
    var errorMessage by remember { mutableStateOf("") }

    val keyboardController = LocalSoftwareKeyboardController.current

    fun getOtp(): String = otpValues.joinToString("")

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, Color.LightGray, RoundedCornerShape(12.dp))
                .background(Color.White, RoundedCornerShape(12.dp))
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Nhập mã OTP", style = MaterialTheme.typography.headlineMedium)
            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.widthIn(max = 360.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                otpValues.forEachIndexed { index, value ->
                    OutlinedTextField(
                        value = value,
                        onValueChange = { newValue ->
                            if (newValue.length == 6 && newValue.all { it.isDigit() }) {
                                newValue.forEachIndexed { i, c ->
                                    otpValues[i] = c.toString()
                                }
                                focusRequesters[5].requestFocus()
                                keyboardController?.hide()
                            } else if (newValue.length <= 1) {
                                if (newValue.isNotEmpty() && newValue[0].isDigit()) {
                                    otpValues[index] = newValue
                                    if (index < 5) {
                                        focusRequesters[index + 1].requestFocus()
                                    } else {
                                        keyboardController?.hide()
                                    }
                                } else if (newValue.isEmpty()) {
                                    otpValues[index] = ""
                                }
                            }
                            previousOtpValues[index] = newValue
                        },
                        modifier = Modifier
                            .width(48.dp)
                            .height(64.dp)
                            .focusRequester(focusRequesters[index])
                            .onKeyEvent { keyEvent ->
                                if ( keyEvent.key == Key.Backspace) {
                                    if (otpValues[index].isEmpty()) {
                                        if (index > 0) {
                                            otpValues[index - 1] = ""
                                            focusRequesters[index - 1].requestFocus()
                                        }
                                        return@onKeyEvent true
                                    } else {
                                        otpValues[index] = ""
                                        return@onKeyEvent true
                                    }
                                }
                                false
                            }

                        ,
                        singleLine = true,
                        textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Center),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = {
                    if (getOtp() == expectedOtp) {
                        navController.navigate(NavigationRoutes.LOGIN)

                    } else {
                        errorMessage = "Mã OTP không đúng"
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
            ) {
                Text("Xác nhận", color = Color.White)
            }

            if (errorMessage.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(errorMessage, color = MaterialTheme.colorScheme.error)
            }
        }
    }
}
