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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Lightbulb
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
import com.example.nihongo.User.data.models.GroupChatMessage
import com.example.nihongo.User.data.models.StudyGroup
import com.example.nihongo.User.data.models.User
import com.example.nihongo.User.data.repository.AIRepository
import com.example.nihongo.User.data.repository.AITip
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
fun GroupChatScreen(
    navController: NavController,
    groupId: String,
    userEmail: String,
    userRepository: UserRepository,
    aiRepository: AIRepository
) {
    val selectedItem = "community"
    var studyGroup by remember { mutableStateOf<StudyGroup?>(null) }
    var currentUser by remember { mutableStateOf<User?>(null) }
    var messages by remember { mutableStateOf<List<GroupChatMessage>>(emptyList()) }
    var messageText by remember { mutableStateOf("") }
    val firestore = FirebaseFirestore.getInstance()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val listenerState = remember { mutableStateOf<ListenerRegistration?>(null) }
    var dailyTip by remember { mutableStateOf<AITip?>(null) }

    // Thêm state để theo dõi lỗi
    var indexError by remember { mutableStateOf(false) }

    // Lấy thông tin nhóm và người dùng hiện tại
    LaunchedEffect(groupId, userEmail) {
        try {
            // Lấy thông tin nhóm
            val groupDoc = firestore.collection("studyGroups").document(groupId).get().await()
            studyGroup = groupDoc.toObject(StudyGroup::class.java)?.apply { id = groupDoc.id }
            Log.d("GroupChat", "Loaded study group: ${studyGroup?.title}")

            // Lấy thông tin người dùng hiện tại
            currentUser = userRepository.getUserByEmail(userEmail)
            Log.d("GroupChat", "Current user: ${currentUser?.username}")
            
            // Tải tin nhắn mà không sử dụng orderBy để tránh lỗi index
            val messagesSnapshot = firestore.collection("groupMessages")
                .whereEqualTo("groupId", groupId)
                .get()
                .await()
            
            val initialMessages = messagesSnapshot.documents.mapNotNull { doc ->
                doc.toObject(GroupChatMessage::class.java)?.apply {
                    id = doc.id
                }
            }.sortedBy { it.timestamp?.seconds ?: 0 } // Sắp xếp ở phía client
            
            Log.d("GroupChat", "Loaded ${initialMessages.size} initial messages")
            messages = initialMessages
            val desc = studyGroup?.description ?: ""
            // generateDailyTipForGroup là suspend nên có thể gọi trực tiếp trong LaunchedEffect
            val tip = aiRepository.generateDailyTipForGroup(groupId, "1", desc)
            dailyTip = tip
        } catch (e: Exception) {
            Log.e("GroupChat", "Error loading initial data", e)
        }
    }

    // Thiết lập real-time listener cho tin nhắn
    DisposableEffect(groupId) {
        val listener = try {
            firestore.collection("groupMessages")
                .whereEqualTo("groupId", groupId)
                // Không sử dụng orderBy để tránh lỗi index
                .addSnapshotListener { snapshot, e ->
                    if (e != null) {
                        Log.e("GroupChat", "Listen failed", e)
                        
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
                            doc.toObject(GroupChatMessage::class.java)?.apply {
                                id = doc.id
                            }
                        }.sortedBy { it.timestamp?.seconds ?: 0 } // Sắp xếp ở phía client
                        
                        Log.d("GroupChat", "Received ${newMessages.size} messages in real-time")
                        messages = newMessages
                    }
                }
        } catch (e: Exception) {
            Log.e("GroupChat", "Error setting up listener", e)
            null
        }

        listenerState.value = listener

        // Hủy listener khi component bị hủy
        onDispose {
            Log.d("GroupChat", "Removing Firestore listener")
            listener?.remove()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        studyGroup?.let {
                            Text(
                                text = it.title,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
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

            dailyTip?.let { tip ->
                Spacer(modifier = Modifier.height(12.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFFFF9C4)
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lightbulb,
                            contentDescription = null,
                            tint = Color(0xFFF57C00),
                            modifier = Modifier.size(20.dp)
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        Column {
                            Text(
                                text = "Mẹo hôm nay",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFE65100)
                            )

                            Text(
                                text = tip.tip,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF5D4037)
                            )
                        }
                    }
                }
            }

            val listState = rememberLazyListState()

            LaunchedEffect(messages.size) {
                if (messages.isNotEmpty()) {
                    listState.scrollToItem(messages.lastIndex)
                }
            }

            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                reverseLayout = false
            ) {
                item { Spacer(modifier = Modifier.height(12.dp)) }

                items(messages) { message ->
                    val isCurrentUser = message.senderId == currentUser?.id
                    MessageItem(message, isCurrentUser)
                    Spacer(modifier = Modifier.height(8.dp))
                }
                item { Spacer(modifier = Modifier.height(16.dp)) }

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
                            if (messageText.isNotBlank() && currentUser != null && studyGroup != null) {
                                val newMessage = GroupChatMessage(
                                    groupId = groupId,
                                    senderId = currentUser!!.id,
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
                                        val docRef = firestore.collection("groupMessages")
                                            .add(newMessage)
                                            .await()
                                        
                                        Log.d("GroupChat", "Message added with ID: ${docRef.id}")

                                        // Cập nhật lastActivity của nhóm
                                        firestore.collection("studyGroups")
                                            .document(groupId)
                                            .update("lastActivity", Timestamp.now())
                                            .await()

                                        // Cộng điểm năng động cho người dùng
                                        val updatedUser = currentUser!!.addActivityPoints(2)
                                        userRepository.updateUser(updatedUser)
                                        
                                        // Gửi thông báo đến tất cả người dùng trừ người gửi
                                        try {
                                            val notifyViewModel = AdminNotifyPageViewModel()
                                            val notificationTitle = "Tin nhắn mới trong ${studyGroup!!.title}"
                                            val notificationMessage = "${currentUser!!.username}: $sentText"

                                            // Sử dụng hàm mới để gửi thông báo nhóm đến tất cả trừ người gửi
                                            notifyViewModel.sendGroupChatNotification(
                                                notificationTitle,
                                                notificationMessage,
                                                currentUser!!.email
                                            )

                                            Log.d("GroupChat", "Notification sent to all users except sender")

                                            // Lưu thông báo vào collection notifications cho từng thành viên
                                            // Lấy tất cả người dùng từ Firestore
                                            val usersSnapshot = firestore.collection("users")
                                                .whereNotEqualTo("id", currentUser!!.id)
                                                .get()
                                                .await()
                                            
                                            val otherUsers = usersSnapshot.documents.mapNotNull { doc ->
                                                doc.id
                                            }
                                            
                                            otherUsers.forEach { userId ->
                                                val notification = hashMapOf(
                                                    "userId" to userId,
                                                    "title" to notificationTitle,
                                                    "message" to notificationMessage,
                                                    "timestamp" to FieldValue.serverTimestamp(),
                                                    "read" to false,
                                                    "type" to "group_message",
                                                    "referenceId" to groupId,
                                                    "senderId" to currentUser!!.id
                                                )

                                                FirebaseFirestore.getInstance().collection("notifications")
                                                    .add(notification)
                                                    .await()
                                            }
                                        } catch (e: Exception) {
                                            Log.e("GroupChat", "Failed to send notifications", e)
                                        }
                                    } catch (e: Exception) {
                                        Log.e("GroupChat", "Error sending message", e)
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
fun MessageItem(message: GroupChatMessage, isCurrentUser: Boolean) {
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





