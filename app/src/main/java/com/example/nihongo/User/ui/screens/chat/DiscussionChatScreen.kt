package com.example.nihongo.User.ui.screens.chat

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.nihongo.User.data.models.Discussion
import com.example.nihongo.User.data.models.DiscussionMessage
import com.example.nihongo.User.data.models.User
import com.example.nihongo.User.data.repository.UserRepository
import com.example.nihongo.User.ui.components.BottomNavigationBar
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiscussionChatScreen(
    navController: NavController,
    discussionId: String,
    userEmail: String,
    userRepository: UserRepository
) {
    val selectedItem = "community"
    var discussion by remember { mutableStateOf<Discussion?>(null) }
    var currentUser by remember { mutableStateOf<User?>(null) }
    var messages by remember { mutableStateOf<List<DiscussionMessage>>(emptyList()) }
    var messageText by remember { mutableStateOf("") }
    val firestore = FirebaseFirestore.getInstance()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val listenerState = remember { mutableStateOf<ListenerRegistration?>(null) }
    val listState = rememberLazyListState()
    
    // Thêm state để theo dõi lỗi
    var indexError by remember { mutableStateOf(false) }

    // Lấy thông tin thảo luận và người dùng hiện tại
    LaunchedEffect(discussionId, userEmail) {
        try {
            // Lấy thông tin thảo luận
            val discussionDoc = firestore.collection("discussions").document(discussionId).get().await()
            discussion = discussionDoc.toObject(Discussion::class.java)?.apply { id = discussionDoc.id }
            Log.d("DiscussionChat", "Loaded discussion: ${discussion?.title}")

            // Lấy thông tin người dùng hiện tại
            currentUser = userRepository.getUserByEmail(userEmail)
            Log.d("DiscussionChat", "Current user: ${currentUser?.username}")
            
            // Tải tin nhắn mà không sử dụng orderBy để tránh lỗi index
            val messagesSnapshot = firestore.collection("discussionMessages")
                .whereEqualTo("discussionId", discussionId)
                .get()
                .await()
            
            val loadedMessages = messagesSnapshot.documents.mapNotNull { doc ->
                doc.toObject(DiscussionMessage::class.java)?.apply {
                    id = doc.id
                }
            }.sortedBy { it.timestamp?.seconds ?: 0 }
            
            messages = loadedMessages
            Log.d("DiscussionChat", "Loaded ${messages.size} messages initially")
        } catch (e: Exception) {
            Log.e("DiscussionChat", "Error loading discussion or user", e)
            
            // Kiểm tra nếu lỗi là do thiếu index
            if (e.message?.contains("FAILED_PRECONDITION") == true && 
                e.message?.contains("index") == true) {
                indexError = true
            }
        }
    }

    // Thiết lập real-time listener cho tin nhắn
    DisposableEffect(discussionId) {
        val listener = try {
            firestore.collection("discussionMessages")
                .whereEqualTo("discussionId", discussionId)
                // Không sử dụng orderBy để tránh lỗi index
                .addSnapshotListener { snapshot, e ->
                    if (e != null) {
                        Log.e("DiscussionChat", "Listen failed", e)
                        
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
                            doc.toObject(DiscussionMessage::class.java)?.apply {
                                id = doc.id
                            }
                        }.sortedBy { it.timestamp?.seconds ?: 0 } // Sắp xếp ở phía client
                        
                        Log.d("DiscussionChat", "Received ${newMessages.size} messages in real-time")
                        messages = newMessages
                        
                        // Cuộn xuống tin nhắn mới nhất
                        if (newMessages.isNotEmpty()) {
                            coroutineScope.launch {
                                listState.animateScrollToItem(newMessages.size - 1)
                            }
                        }
                    }
                }
        } catch (e: Exception) {
            Log.e("DiscussionChat", "Error setting up listener", e)
            null
        }

        listenerState.value = listener

        // Hủy listener khi component bị hủy
        onDispose {
            Log.d("DiscussionChat", "Removing Firestore listener")
            listener?.remove()
        }
    }

    // Cuộn xuống tin nhắn mới nhất khi tin nhắn thay đổi
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = discussion?.title ?: "Thảo luận",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
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
        ) {
            // Hiển thị thông báo lỗi nếu có
            if (indexError) {
                Text(
                    text = "Cần tạo chỉ mục trong Firebase. Vui lòng liên hệ admin.",
                    color = Color.Red,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFFFEBEE))
                        .padding(8.dp)
                )
            }
            
            // Hiển thị nội dung thảo luận
            discussion?.let { disc ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFF5F5F5))
                        .padding(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Avatar của người tạo thảo luận
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color.LightGray),
                            contentAlignment = Alignment.Center
                        ) {
                            AsyncImage(
                                model = disc.authorImageUrl,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                        
                        Spacer(modifier = Modifier.width(12.dp))
                        
                        Column {
                            Text(
                                text = disc.authorName,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                            
                            // Thời gian tạo
                            val formattedDate = remember(disc.createdAt) {
                                val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                                sdf.format(Date(disc.createdAt))
                            }
                            
                            Text(
                                text = formattedDate,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Text(
                        text = disc.content,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    
                    // Tags
                    if (disc.tags.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            disc.tags.forEach { tag ->
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(Color(0xFFE0F7FA))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = tag,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFF00838F)
                                    )
                                }
                            }
                        }
                    }
                }
                
                Divider()
            }

            // Tin nhắn
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                state = listState,
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(messages) { message ->
                    val isCurrentUser = message.senderId == currentUser?.id
                    MessageItem(message, isCurrentUser)
                }
            }
            
            // Input box
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextField(
                    value = messageText,
                    onValueChange = { messageText = it },
                    placeholder = { Text("Nhập tin nhắn...") },
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 8.dp),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color(0xFFF5F5F5),
                        unfocusedContainerColor = Color(0xFFF5F5F5),
                        disabledContainerColor = Color(0xFFF5F5F5),
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    shape = RoundedCornerShape(24.dp),
                    maxLines = 3
                )
                
                IconButton(
                    onClick = {
                        if (messageText.isNotBlank() && currentUser != null && discussion != null) {
                            // Lưu tin nhắn hiện tại và xóa input
                            val sentText = messageText
                            messageText = ""
                            
                            coroutineScope.launch {
                                try {
                                    // Tạo tin nhắn mới
                                    val newMessage = DiscussionMessage(
                                        discussionId = discussionId,
                                        senderId = currentUser!!.id,
                                        senderName = currentUser!!.username,
                                        senderImageUrl = currentUser!!.imageUrl,
                                        content = sentText,
                                        timestamp = Timestamp.now()
                                    )
                                    
                                    // Thêm tin nhắn vào Firestore
                                    val docRef = firestore.collection("discussionMessages")
                                        .add(newMessage)
                                        .await()
                                    
                                    Log.d("DiscussionChat", "Message added with ID: ${docRef.id}")
                                    
                                    // Cập nhật số lượng bình luận trong thảo luận
                                    firestore.collection("discussions")
                                        .document(discussionId)
                                        .update("commentCount", (discussion?.commentCount ?: 0) + 1)
                                        .await()
                                    
                                    // Cộng điểm năng động cho người dùng
                                    val updatedUser = currentUser!!.addActivityPoints(2)
                                    userRepository.updateUser(updatedUser)
                                } catch (e: Exception) {
                                    Log.e("DiscussionChat", "Error sending message", e)
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
                    Icon(
                        Icons.Default.Send,
                        contentDescription = "Send",
                        tint = Color.White
                    )
                }
            }
        }
    }
}

@Composable
fun MessageItem(message: DiscussionMessage, isCurrentUser: Boolean) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isCurrentUser) Alignment.End else Alignment.Start
    ) {
        Row(
            verticalAlignment = Alignment.Top,
            horizontalArrangement = if (isCurrentUser) Arrangement.End else Arrangement.Start,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (!isCurrentUser) {
                // Avatar của người gửi (chỉ hiển thị cho tin nhắn của người khác)
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Color.LightGray),
                    contentAlignment = Alignment.Center
                ) {
                    AsyncImage(
                        model = message.senderImageUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
            }
            
            Column(
                horizontalAlignment = if (isCurrentUser) Alignment.End else Alignment.Start
            ) {
                if (!isCurrentUser) {
                    Text(
                        text = message.senderName,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                }
                
                // Nội dung tin nhắn
                Surface(
                    shape = RoundedCornerShape(
                        topStart = if (isCurrentUser) 16.dp else 0.dp,
                        topEnd = if (isCurrentUser) 0.dp else 16.dp,
                        bottomStart = 16.dp,
                        bottomEnd = 16.dp
                    ),
                    color = if (isCurrentUser) Color(0xFF00C853) else Color(0xFFE0E0E0),
                    modifier = Modifier.widthIn(max = 280.dp)
                ) {
                    Text(
                        text = message.content,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (isCurrentUser) Color.White else Color.Black,
                        modifier = Modifier.padding(12.dp)
                    )
                }
                
                // Thời gian gửi
                Spacer(modifier = Modifier.height(2.dp))
                val formattedTime = remember(message.timestamp) {
                    message.timestamp?.let {
                        val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
                        sdf.format(Date(it.seconds * 1000))
                    } ?: ""
                }
                Text(
                    text = formattedTime,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }
            
            if (isCurrentUser) {
                Spacer(modifier = Modifier.width(8.dp))
                // Avatar của người gửi (chỉ hiển thị cho tin nhắn của người khác)
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Color.LightGray),
                    contentAlignment = Alignment.Center
                ) {
                    AsyncImage(
                        model = message.senderImageUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}
