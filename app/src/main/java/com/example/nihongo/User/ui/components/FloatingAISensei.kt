package com.example.nihongo.User.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import coil.compose.AsyncImage
import com.example.nihongo.User.data.models.User
import com.example.nihongo.User.data.repository.AIRepository
import kotlinx.coroutines.launch

@Composable
fun FloatingAISensei(
    currentUser: User?,
    aiRepository: AIRepository,
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(false) }
    var messages by remember { mutableStateOf<List<Pair<String, String>>>(emptyList()) }
    var inputText by remember { mutableStateOf("") }
    var isResponding by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val keyboardController = LocalSoftwareKeyboardController.current
    val context = LocalContext.current

    // Animated offset for drag
    var offsetY by remember { mutableStateOf(0f) }
    val dragThreshold = 200f // Threshold to close the chat

    val handleSendMessage: () -> Unit = {
        if (inputText.isNotBlank() && !isResponding && currentUser != null) {
            val userMessage = inputText
            messages = messages + ("user" to userMessage)
            inputText = ""
            isResponding = true
            keyboardController?.hide()

            coroutineScope.launch {
                try {
                    val response = aiRepository.chatWithAI(userMessage, userId = currentUser.id)
                    if (response != null) {
                        messages = messages + ("ai" to response.reply)
                    } else {
                        messages = messages + ("ai" to "Xin lỗi, tôi không thể trả lời lúc này.")
                    }
                } catch (e: Exception) {
                    messages = messages + ("ai" to "Đã xảy ra lỗi: ${e.message}")
                } finally {
                    isResponding = false
                }
            }
        }
    }

    // Auto scroll when new message
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty() && isExpanded) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    // Handle back button when chat is expanded
    BackHandler(enabled = isExpanded) {
        isExpanded = false
        offsetY = 0f
    }

    Box(modifier = modifier.fillMaxSize()) {
        // Floating AI Button
        if (!isExpanded) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 16.dp, bottom = 120.dp)
                    .zIndex(10f)
                    .size(64.dp)
                    .shadow(8.dp, CircleShape)
                    .clip(CircleShape)
                    .background(Color(0xFF4CAF50))
                    .clickable { isExpanded = true }
            ) {
                AsyncImage(
                    model = "https://drive.google.com/uc?export=view&id=1I_IBb8mpJwvv6LqbEe_Cgu060tqJmeN4",
                    contentDescription = "AI Sensei",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
        }

        // AI Chat Bottom Sheet
        AnimatedVisibility(
            visible = isExpanded,
            enter = slideInVertically(
                initialOffsetY = { it },
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                )
            ),
            exit = slideOutVertically(
                targetOffsetY = { it },
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                )
            ),
            modifier = Modifier.zIndex(100f)
        ) {
            // Overlay to dim background
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f))
                    .clickable(enabled = false) { } // Prevent clicks through
            ) {
                // Chat container
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(0.8f)
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 70.dp)
                        .offset(y = offsetY.dp)
                        .pointerInput(Unit) {
                            detectDragGestures(
                                onDragEnd = {
                                    if (offsetY > dragThreshold) {
                                        isExpanded = false
                                        offsetY = 0f
                                    } else {
                                        offsetY = 0f
                                    }
                                },
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    val newOffset = offsetY + dragAmount.y
                                    if (newOffset >= 0) {
                                        offsetY = newOffset
                                    }
                                }
                            )
                        }
                ) {
                    Card(
                        modifier = Modifier.fillMaxSize(),
                        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(8.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize()
                        ) {
                            // Header with drag handle
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        Color(0xFF4CAF50),
                                        RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
                                    )
                                    .padding(16.dp)
                            ) {
                                // Drag handle
                                Box(
                                    modifier = Modifier
                                        .width(40.dp)
                                        .height(4.dp)
                                        .background(Color.White.copy(alpha = 0.5f), RoundedCornerShape(2.dp))
                                        .align(Alignment.CenterHorizontally)
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                // Header content
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(40.dp)
                                                .clip(CircleShape)
                                                .background(Color.White)
                                        ) {
                                            AsyncImage(
                                                model = "https://drive.google.com/uc?export=view&id=1I_IBb8mpJwvv6LqbEe_Cgu060tqJmeN4",
                                                contentDescription = "AI Sensei",
                                                modifier = Modifier.fillMaxSize(),
                                                contentScale = ContentScale.Crop
                                            )
                                        }

                                        Spacer(modifier = Modifier.width(12.dp))

                                        Column {
                                            Text(
                                                text = "Nihongo AI Sensei",
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White
                                            )
                                            Text(
                                                text = "Trợ lý học tiếng Nhật",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = Color.White.copy(alpha = 0.8f)
                                            )
                                        }
                                    }

                                    IconButton(
                                        onClick = {
                                            isExpanded = false
                                            offsetY = 0f
                                        }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Close",
                                            tint = Color.White
                                        )
                                    }
                                }
                            }

                            // Messages area
                            LazyColumn(
                                state = listState,
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth()
                                    .background(Color(0xFFF5F5F5))
                                    .padding(horizontal = 16.dp),
                                contentPadding = PaddingValues(vertical = 16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                if (messages.isEmpty()) {
                                    item {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 32.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            AsyncImage(
                                                model = "https://drive.google.com/uc?export=view&id=1I_IBb8mpJwvv6LqbEe_Cgu060tqJmeN4",
                                                contentDescription = "AI Sensei",
                                                modifier = Modifier.size(58.dp),
                                                contentScale = ContentScale.Crop
                                            )
                                            Spacer(modifier = Modifier.height(10.dp))
                                            Text(
                                                text = "Xin chào! Tôi là AI Sensei\nHãy hỏi tôi về tiếng Nhật nhé!",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = Color.Gray,
                                                textAlign = TextAlign.Center
                                            )
                                        }
                                    }
                                } else {
                                    items(messages) { (sender, message) ->
                                        ChatBubble(
                                            message = message,
                                            isUser = sender == "user"
                                        )
                                    }
                                }

                                if (isResponding) {
                                    item {
                                        Row(
                                            modifier = Modifier.padding(8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(20.dp),
                                                color = Color(0xFF4CAF50),
                                                strokeWidth = 2.dp
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = "Đang trả lời...",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = Color.Gray
                                            )
                                        }
                                    }
                                }
                            }

                            // Input area
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color.White)
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = inputText,
                                    onValueChange = { inputText = it },
                                    modifier = Modifier.weight(1f),
                                    placeholder = { Text("Nhập câu hỏi...", fontSize = 14.sp) },
                                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                                    keyboardActions = KeyboardActions(onSend = { handleSendMessage() }),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = Color(0xFF4CAF50),
                                        unfocusedBorderColor = Color.LightGray
                                    ),
                                    shape = RoundedCornerShape(24.dp),
                                    enabled = !isResponding,
                                    maxLines = 3
                                )

                                Spacer(modifier = Modifier.width(8.dp))

                                IconButton(
                                    onClick = handleSendMessage,
                                    enabled = inputText.isNotBlank() && !isResponding,
                                    modifier = Modifier
                                        .size(48.dp)
                                        .background(
                                            color = if (inputText.isNotBlank() && !isResponding)
                                                Color(0xFF4CAF50) else Color.Gray,
                                            shape = CircleShape
                                        )
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Send,
                                        contentDescription = "Send",
                                        tint = Color.White
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ChatBubble(message: String, isUser: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        Card(
            colors = CardDefaults.cardColors(
                containerColor = if (isUser) Color(0xFF4CAF50) else Color.White
            ),
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (isUser) 16.dp else 4.dp,
                bottomEnd = if (isUser) 4.dp else 16.dp
            ),
            elevation = CardDefaults.cardElevation(2.dp),
            modifier = Modifier.widthIn(max = 280.dp)
        ) {
            Text(
                text = message,
                modifier = Modifier.padding(12.dp),
                color = if (isUser) Color.White else Color.Black,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}