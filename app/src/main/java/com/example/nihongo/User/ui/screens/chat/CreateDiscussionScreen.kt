package com.example.nihongo.User.ui.screens.chat

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.nihongo.User.data.models.Discussion
import com.example.nihongo.User.data.models.User
import com.example.nihongo.User.data.repository.UserRepository
import com.example.nihongo.User.ui.components.BottomNavigationBar
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateDiscussionScreen(
    navController: NavController,
    userEmail: String,
    userRepository: UserRepository
) {
    val selectedItem = "community"
    var title by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }
    var tags by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var currentUser by remember { mutableStateOf<User?>(null) }
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val firestore = FirebaseFirestore.getInstance()
    
    // Lấy thông tin người dùng hiện tại
    LaunchedEffect(userEmail) {
        try {
            currentUser = userRepository.getUserByEmail(userEmail)
            Log.d("CreateDiscussion", "Loaded current user: ${currentUser?.username}")
        } catch (e: Exception) {
            Log.e("CreateDiscussion", "Error loading current user", e)
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Tạo thảo luận mới",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White
                )
            )
        },
        bottomBar = {
            BottomNavigationBar(
                navController = navController,
                selectedItem = selectedItem,
                userEmail = userEmail
            ) { selectedRoute ->
                navController.navigate("$selectedRoute/$userEmail") {
                    popUpTo(navController.graph.startDestinationId) { saveState = true }
                    launchSingleTop = true
                    restoreState = true
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Tiêu đề
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Tiêu đề") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            
            // Nội dung
            OutlinedTextField(
                value = content,
                onValueChange = { content = it },
                label = { Text("Nội dung") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                maxLines = 10
            )
            
            // Tags
            OutlinedTextField(
                value = tags,
                onValueChange = { tags = it },
                label = { Text("Tags (phân cách bằng dấu phẩy)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            
            // Nút tạo thảo luận
            Button(
                onClick = {
                    if (title.isBlank() || content.isBlank()) {
                        Toast.makeText(context, "Vui lòng nhập đầy đủ tiêu đề và nội dung", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    
                    if (currentUser == null) {
                        Toast.makeText(context, "Không thể xác định người dùng hiện tại", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    
                    isLoading = true
                    
                    coroutineScope.launch {
                        try {
                            // Tạo đối tượng Discussion mới
                            val tagList = tags.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                            val newDiscussion = Discussion(
                                id = "",
                                title = title,
                                content = content,
                                authorId = currentUser!!.id,
                                authorName = currentUser!!.username,
                                authorImageUrl = currentUser!!.imageUrl,
                                commentCount = 0,
                                createdAt = System.currentTimeMillis(),
                                tags = tagList
                            )
                            
                            // Lưu vào Firestore
                            val docRef = firestore.collection("discussions")
                                .add(newDiscussion)
                                .await()
                            
                            Log.d("CreateDiscussion", "Discussion created with ID: ${docRef.id}")
                            
                            // Cộng điểm năng động cho người dùng
                            val updatedUser = currentUser!!.addActivityPoints(5)
                            userRepository.updateUser(updatedUser)
                            
                            // Hiển thị thông báo thành công
                            Toast.makeText(context, "Đã tạo thảo luận mới", Toast.LENGTH_SHORT).show()
                            
                            // Điều hướng đến màn hình chat thảo luận
                            navController.navigate("discussion_chat/${docRef.id}/$userEmail") {
                                popUpTo("community/$userEmail") { inclusive = false }
                            }
                        } catch (e: Exception) {
                            Log.e("CreateDiscussion", "Error creating discussion", e)
                            Toast.makeText(context, "Lỗi: ${e.message}", Toast.LENGTH_SHORT).show()
                            isLoading = false
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00C853))
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = Color.White
                    )
                } else {
                    Icon(Icons.Default.Send, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Đăng thảo luận")
                }
            }
        }
    }
}