package com.example.nihongo.User.ui.screens.chat

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.nihongo.Admin.viewmodel.AdminNotifyPageViewModel
import com.example.nihongo.User.data.models.PrivateChatMessage
import com.example.nihongo.User.data.models.User
import com.example.nihongo.User.data.repository.UserRepository
import com.example.nihongo.User.ui.components.BottomNavigationBar
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivateChatScreen(
    navController: NavController,
    partnerUserId: String,
    userEmail: String,
    userRepository: UserRepository
) {
    val selectedItem = "community"
    var currentUser by remember { mutableStateOf<User?>(null) }
    var partnerUser by remember { mutableStateOf<User?>(null) }
    var messages by remember { mutableStateOf<List<PrivateChatMessage>>(emptyList()) }
    var messageText by remember { mutableStateOf("") }
    val firestore = FirebaseFirestore.getInstance()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val listenerState = remember { mutableStateOf<ListenerRegistration?>(null) }
    
    // Thêm state để theo dõi lỗi
    var indexError by remember { mutableStateOf(false) }
    
    // Tạo chatId từ userId của hai người dùng (sắp xếp để đảm bảo tính nhất quán)
    val chatId = remember(currentUser, partnerUserId) {
        val ids = listOfNotNull(currentUser?.id, partnerUserId).sorted()
        if (ids.size == 2) ids.joinToString("_") else ""
    }

    // Lấy thông tin người dùng hiện tại và đối tác
    LaunchedEffect(userEmail, partnerUserId) {
        try {
            // Lấy thông tin người dùng hiện tại
            currentUser = userRepository.getUserByEmail(userEmail)
            Log.d("PrivateChat", "Current user: ${currentUser?.username}")

            // Lấy thông tin đối tác
            partnerUser = userRepository.getUserById(partnerUserId)
            Log.d("PrivateChat", "Partner user: ${partnerUser?.username}")
            
            // Tải tin nhắn ban đầu
            if (chatId.isNotEmpty()) {
                try {
                    val messagesSnapshot = firestore.collection("privateMessages")
                        .whereEqualTo("chatId", chatId)
                        .get()
                        .await()
                    
                    val loadedMessages = messagesSnapshot.documents.mapNotNull { doc ->
                        doc.toObject(PrivateChatMessage::class.java)?.apply {
                            id = doc.id
                        }
                    }.sortedBy { it.timestamp?.seconds ?: 0 }
                    
                    messages = loadedMessages
                    Log.d("PrivateChat", "Loaded ${messages.size} messages initially")
                } catch (e: Exception) {
                    Log.e("PrivateChat", "Error loading initial messages", e)
                    
                    // Kiểm tra nếu lỗi là do thiếu index
                    if (e.message?.contains("FAILED_PRECONDITION") == true && 
                        e.message?.contains("index") == true) {
                        indexError = true
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("PrivateChat", "Error loading users", e)
        }
    }

    // Thiết lập real-time listener cho tin nhắn
    DisposableEffect(chatId) {
        if (chatId.isEmpty()) {
            return@DisposableEffect onDispose { listenerState.value?.remove() }
        }
        
        val listener = try {
            firestore.collection("privateMessages")
                .whereEqualTo("chatId", chatId)
                // Không sử dụng orderBy để tránh lỗi index
                .addSnapshotListener { snapshot, e ->
                    if (e != null) {
                        Log.e("PrivateChat", "Listen failed", e)
                        
                        // Kiểm tra nếu lỗi là do thiếu index
                        if (e.message?.contains("FAILED_PRECONDITION") == true && 
                            e.message?.contains("index") == true) {
                            indexError = true
                            Toast.makeText(
                                context, 
                                "Cần tạo chỉ mục trong Firebase. Vui lòng liên hệ admin.", 
                                Toast.LENGTH_LONG
                            ).show()
                        }
                        return@addSnapshotListener
                    }

                    if (snapshot != null) {
                        val newMessages = snapshot.documents.mapNotNull { doc ->
                            doc.toObject(PrivateChatMessage::class.java)?.apply {
                                id = doc.id
                            }
                        }.sortedBy { it.timestamp?.seconds ?: 0 } // Sắp xếp ở phía client
                        
                        Log.d("PrivateChat", "Received ${newMessages.size} messages in real-time")
                        messages = newMessages
                    }
                }
        } catch (e: Exception) {
            Log.e("PrivateChat", "Error setting up listener", e)
            null
        }

        listenerState.value = listener

        // Hủy listener khi component bị hủy
        onDispose {
            Log.d("PrivateChat", "Removing Firestore listener")
            listener?.remove()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        partnerUser?.let {
                            AsyncImage(
                                model = it.imageUrl,
                                contentDescription = null,
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = it.username,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = if (it.online) "Đang hoạt động" else "Không hoạt động",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (it.online) Color(0xFF00C853) else Color.Gray
                                )
                            }
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
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
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Hiển thị thông báo lỗi index nếu cần
            if (indexError) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "Cần cấu hình Firebase",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Ứng dụng cần tạo chỉ mục trong Firebase để hiển thị tin nhắn theo thời gian. " +
                            "Vui lòng liên hệ admin hoặc tạo chỉ mục trong Firebase Console.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            // Hiển thị tin nhắn
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                reverseLayout = false
            ) {
                items(messages) { message ->
                    val isCurrentUser = message.senderId == currentUser?.id
                    PrivateMessageItem(message, isCurrentUser)
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            // Thanh nhập tin nhắn
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextField(
                        value = messageText,
                        onValueChange = { messageText = it },
                        placeholder = { Text("Nhập tin nhắn...") },
                        modifier = Modifier.weight(1f),
                        colors = TextFieldDefaults.colors(
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            disabledIndicatorColor = Color.Transparent
                        ),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = {
                            if (messageText.isNotBlank() && currentUser != null && chatId.isNotEmpty()) {
                                val newMessage = PrivateChatMessage(
                                    chatId = chatId,
                                    senderId = currentUser!!.id,
                                    receiverId = partnerUserId,
                                    senderName = currentUser!!.username,
                                    senderImageUrl = currentUser!!.imageUrl,
                                    content = messageText,
                                    timestamp = Timestamp.now()
                                )

                                // Xóa tin nhắn đã gửi
                                val sentText = messageText
                                messageText = ""

                                coroutineScope.launch {
                                    try {
                                        // Thêm tin nhắn vào Firestore
                                        val docRef = firestore.collection("privateMessages")
                                            .add(newMessage)
                                            .await()
                                        
                                        Log.d("PrivateChat", "Message added with ID: ${docRef.id}")

                                        // Cộng điểm năng động cho người dùng
                                        val updatedUser = currentUser!!.addActivityPoints(1)
                                        userRepository.updateUser(updatedUser)
                                        
                                        // Gửi thông báo đến người nhận
                                        if (partnerUser != null) {
                                            try {
                                                val notifyViewModel = AdminNotifyPageViewModel()
                                                val notificationTitle = "Tin nhắn mới từ ${currentUser!!.username}"
                                                val notificationMessage = sentText
                                                
                                                // Sử dụng email của đối tác thay vì ID
                                                notifyViewModel.sendNotificationToSpecificUser(
                                                    notificationTitle,
                                                    notificationMessage,
                                                    partnerUser!!.email
                                                )
                                                
                                                Log.d("PrivateChat", "Notification sent to: ${partnerUser!!.username}")
                                                
                                                // Lưu thông báo vào collection notifications
                                                val notification = hashMapOf(
                                                    "userId" to partnerUserId,
                                                    "title" to notificationTitle,
                                                    "message" to notificationMessage,
                                                    "timestamp" to FieldValue.serverTimestamp(),
                                                    "read" to false,
                                                    "type" to "private_message",
                                                    "referenceId" to docRef.id,
                                                    "senderId" to currentUser!!.id
                                                )
                                                
                                                FirebaseFirestore.getInstance().collection("notifications")
                                                    .add(notification)
                                                    .await()
                                            } catch (e: Exception) {
                                                Log.e("PrivateChat", "Failed to send notification", e)
                                            }
                                        }
                                    } catch (e: Exception) {
                                        Log.e("PrivateChat", "Error sending message", e)
                                        // Hiển thị thông báo lỗi
                                        Toast.makeText(context, "Lỗi gửi tin nhắn: ${e.message}", Toast.LENGTH_SHORT).show()
                                        
                                        // Khôi phục tin nhắn nếu gửi thất bại
                                        messageText = sentText
                                    }
                                }
                            }
                        },
                        modifier = Modifier
                            .size(48.dp)
                            .background(Color(0xFF00C853), CircleShape),
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = Color(0xFF00C853),
                            contentColor = Color.White
                        )
                    ) {
                        Icon(Icons.Default.Send, contentDescription = "Send")
                    }
                }
            }
        }
    }
}

@Composable
fun PrivateMessageItem(message: PrivateChatMessage, isCurrentUser: Boolean) {
    val dateFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    val timeString = message.timestamp?.toDate()?.let { dateFormat.format(it) } ?: ""

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isCurrentUser) Alignment.End else Alignment.Start
    ) {
        if (!isCurrentUser) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AsyncImage(
                    model = message.senderImageUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = message.senderName,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
        }

        Row(verticalAlignment = Alignment.Bottom) {
            if (!isCurrentUser) {
                Spacer(modifier = Modifier.width(28.dp))
            }

            if (isCurrentUser) {
                Text(
                    text = timeString,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray,
                    modifier = Modifier.padding(end = 4.dp, bottom = 4.dp)
                )
            }

            Card(
                shape = RoundedCornerShape(
                    topStart = 16.dp,
                    topEnd = 16.dp,
                    bottomStart = if (isCurrentUser) 16.dp else 4.dp,
                    bottomEnd = if (isCurrentUser) 4.dp else 16.dp
                ),
                colors = CardDefaults.cardColors(
                    containerColor = if (isCurrentUser) Color(0xFF00C853) else Color(0xFFEEEEEE)
                )
            ) {
                Text(
                    text = message.content,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isCurrentUser) Color.White else Color.Black,
                    modifier = Modifier.padding(12.dp)
                )
            }

            if (!isCurrentUser) {
                Text(
                    text = timeString,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray,
                    modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
                )
            }
        }
    }
}




