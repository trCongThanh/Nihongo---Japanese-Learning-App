package com.example.nihongo.User.ui.screens.homepage

import android.content.Context
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Comment
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.nihongo.Admin.viewmodel.AdminNotifyPageViewModel
import com.example.nihongo.User.data.models.Discussion
import com.example.nihongo.User.data.models.DiscussionMessage
import com.example.nihongo.User.data.models.GroupChatMessage
import com.example.nihongo.User.data.models.LearningGoal
import com.example.nihongo.User.data.models.PrivateChatMessage
import com.example.nihongo.User.data.models.StudyGroup
import com.example.nihongo.User.data.models.User
import com.example.nihongo.User.data.repository.AIRepository
import com.example.nihongo.User.data.repository.AITip
import com.example.nihongo.User.data.repository.GroupChallenge
import com.example.nihongo.User.data.repository.UserRecommendation
import com.example.nihongo.User.data.repository.UserRepository
import com.example.nihongo.User.ui.components.BottomNavigationBar
import com.example.nihongo.User.ui.components.FloatingAISensei
import com.example.nihongo.User.ui.components.TopBarIcon
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommunityScreen(
    navController: NavController,
    userRepository: UserRepository,
    userEmail: String,
    initialTab: Int = 0,
    ) {
    var currentUser by remember { mutableStateOf<User?>(null) }
    var allUsers by remember { mutableStateOf<List<User>>(emptyList()) }
    var discussions by remember { mutableStateOf<List<Discussion>>(emptyList()) }
    var studyGroups by remember { mutableStateOf<List<StudyGroup>>(emptyList()) }
    var learningGoals by remember { mutableStateOf<List<LearningGoal>>(emptyList()) }
    val selectedItem = "community"

    // Định nghĩa tabs
    val tabs = listOf("Cộng đồng", "Thảo luận", "Bảng xếp hạng")

    // Sử dụng initialTab làm giá trị khởi tạo cho selectedTab
    var selectedTab by remember { mutableStateOf(initialTab) }

    val context = LocalContext.current

    val aiRepository = remember { AIRepository() }
    var smartRecommendations by remember { mutableStateOf<List<UserRecommendation>>(emptyList()) }
    var groupChallenges by remember { mutableStateOf<Map<String, List<GroupChallenge>>>(emptyMap()) }
    var dailyTips by remember { mutableStateOf<Map<String, AITip>>(emptyMap()) }
    var showRecommendationsDialog by remember { mutableStateOf(false) }
    var showChallengesDialog by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val filteredRecommendations = smartRecommendations.filter { rec ->
        currentUser?.partners?.contains(rec.userId) == false
    }
    LaunchedEffect(userEmail) {
        // Lấy dữ liệu người dùng
        currentUser = userRepository.getUserByEmail(userEmail)
        allUsers = userRepository.getAllUsers()

        // Lấy dữ liệu nhóm học tập từ Firestore
        studyGroups = getStudyGroupsFromFirestore()

        // Lấy dữ liệu thảo luận từ Firestore
        discussions = getDiscussionsFromFirestore()

        // Lấy dữ liệu mục tiêu học tập từ Firestore
        if (currentUser != null) {
            learningGoals = getLearningGoalsFromFirestore(currentUser!!.id)
        }

        // Cập nhật trạng thái online của người dùng hiện tại
        currentUser?.let { user ->
            userRepository.updateUserOnlineStatus(user.id, true)

            // === Load AI Data ===
            coroutineScope.launch {
                try {
                    // 1. Get smart recommendations
                    val rawRecs = aiRepository.getAIPartnerRecommendations(user, limit = 10)

                    smartRecommendations = rawRecs.filter { rec ->
                        user.partners.contains(rec.userId) == false
                    }

                    Log.d("CommunityScreen", "Filtered recommendations: ${smartRecommendations.size}/${rawRecs.size}")
                    Log.d("CommunityScreen", "Loaded ${smartRecommendations.size} AI recommendations")

                    // 2. Load challenges and tips for each group
                    studyGroups.forEach { group ->
                        // Load challenges
                        val challenges = aiRepository.getActiveChallengesForGroup(group.id)
                        groupChallenges = groupChallenges + (group.id to challenges)

                        // Generate daily tip if not exists
                        val userLevel = currentUser?.jlptLevel?.let { "N$it" } ?: "N5"
                        Log.d("CommunityScreen", "Calling generateDailyTipForGroup() for group ${group.id}, level = $userLevel")

                        val tip = aiRepository.generateDailyTipForGroup(
                            group.id,
                            userLevel,
                            group.description // <-- thêm description
                        )
                        Log.d("CommunityScreen", "Tip result for group ${group.id}: $tip")
                        if (tip != null) {
                            dailyTips = dailyTips + (group.id to tip)
                        }

                        Log.d("CommunityScreen", "Group ${group.id}: ${challenges.size} challenges, tip: ${tip != null}")
                    }
                } catch (e: Exception) {
                    Log.e("CommunityScreen", "Error loading AI data", e)
                }
            }
        }
    }

    // Dialog xác nhận upload dữ liệu mẫu
//    if (showUploadDialog) {
//        AlertDialog(
//            onDismissRequest = { showUploadDialog = false },
//            title = { Text("Tải dữ liệu mẫu") },
//            text = { Text("Bạn có chắc chắn muốn tải dữ liệu mẫu lên Firestore? Điều này sẽ thêm các nhóm học tập, thảo luận và mục tiêu học tập mẫu.") },
//            confirmButton = {
//                Button(
//                    onClick = {
//                        uploadSampleDataToFirestore(context)
//                        showUploadDialog = false
//                    }
//                ) {
//                    Text("Tải lên")
//                }
//            },
//            dismissButton = {
//                Button(
//                    onClick = { showUploadDialog = false }
//                ) {
//                    Text("Hủy")
//                }
//            }
//        )
//    }

    // Thêm DisposableEffect để cập nhật trạng thái offline khi rời khỏi màn hình
//    DisposableEffect(Unit) {
//        onDispose {
//            // Khi component bị hủy, cập nhật trạng thái offline
//            currentUser?.let { user ->
//                // Sử dụng CoroutineScope để thực hiện công việc bất đồng bộ
//                val scope = CoroutineScope(Dispatchers.IO)
//                scope.launch {
//                    userRepository.updateUserOnlineStatus(user.id, false)
//                }
//            }
//        }
//    }

    Scaffold(
        containerColor = Color(0xFFEEEEEE),
        topBar = {
            TopAppBar(
                title = {
                    currentUser?.let {
                        Column {
                            Text(
                                "\uD83D\uDC4B こんにちわ ${it.username} さん",
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontSize = 16.sp
                                )
                            )
                            if (it.vip) {
                                Text(
                                    "\u2B50 VIP です!",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontSize = 12.sp
                                    ),
                                    color = Color(0xFFFFC107)
                                )
                            }
                        }
                    }
                },
                navigationIcon = {
                    TopBarIcon(selectedItem = selectedItem)
                },
                actions = {
                    IconButton(onClick = {
                        navController.navigate("profile/$userEmail")
                    }) {
                        Icon(Icons.Default.Person, contentDescription = "Avatar")
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
        },
        //Thêm FloatingActionButton để hiển thị dialog xác nhận
//        floatingActionButton = {
//            if (currentUser?.vip == true) { // Chỉ hiển thị cho người dùng VIP
//                FloatingActionButton(
//                    onClick = { showUploadDialog = true },
//                    containerColor = Color(0xFF00C853)
//                ) {
//                    Icon(
//                        imageVector = Icons.Default.Add,
//                        contentDescription = "Upload Sample Data",
//                        tint = Color.White
//                    )
//                }
//            }
//        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Tab Row
            ScrollableTabRow(
                selectedTabIndex = selectedTab,
                edgePadding = 16.dp,
                indicator = { tabPositions ->
                    TabRowDefaults.Indicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        height = 3.dp,
                        color = Color(0xFF00C853)
                    )
                },
                containerColor = Color.White,
                contentColor = Color(0xFF00C853)
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = {
                            Text(
                                text = title,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    )
                }
            }

            // Content based on selected tab
            when (selectedTab) {
                0 -> StudyBuddiesTabEnhanced(
                    allUsers,
                    navController,
                    userEmail,
                    studyGroups,
                    userRepository,
                    smartRecommendations,
                    groupChallenges,
                    dailyTips,
                    onShowRecommendations = { showRecommendationsDialog = true },
                    onShowChallenges = { showChallengesDialog = true }
                )
                1 -> DiscussionTab(discussions, navController, userEmail)
                2 -> LeaderboardTab(allUsers, learningGoals)
            }
        }
        FloatingAISensei(
            currentUser = currentUser,
            aiRepository = aiRepository
        )
    }

    // === Smart Recommendations Dialog ===
    if (showRecommendationsDialog && filteredRecommendations.isNotEmpty()) {
        SmartRecommendationsDialog(
            recommendations = smartRecommendations,
            currentUser = currentUser,
            onConnect = { userId ->
                coroutineScope.launch {
                    currentUser?.let { user ->
                        val updatedUser = user.addPartner(userId)
                        userRepository.updateUser(updatedUser)

                        val partner = userRepository.getUserById(userId)
                        partner?.let { partnerUser ->
                            val updatedPartner = partnerUser.addPartner(user.id)
                            userRepository.updateUser(updatedPartner)
                            sendPartnerConnectionNotification(user, partnerUser)
                        }
                        currentUser = updatedUser

                        Toast.makeText(context, "Đã kết nối thành công!", Toast.LENGTH_SHORT).show()
                        smartRecommendations = smartRecommendations.filter { it.userId != userId }

                    }
                }
            },
            onDismiss = { showRecommendationsDialog = false }
        )
    }

    // === Group Challenges Dialog ===
    if (showChallengesDialog) {
        GroupChallengesDialog(
            challenges = groupChallenges,
            studyGroups = studyGroups,
            currentUser = currentUser,
            onJoinChallenge = { challengeId ->
                Toast.makeText(context, "Đã tham gia thử thách!", Toast.LENGTH_SHORT).show()
            },
            onDismiss = { showChallengesDialog = false }
        )
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun StudyBuddiesTabEnhanced(
    users: List<User>,
    navController: NavController,
    userEmail: String,
    studyGroups: List<StudyGroup>,
    userRepository: UserRepository,
    smartRecommendations: List<UserRecommendation>,
    groupChallenges: Map<String, List<GroupChallenge>>,
    dailyTips: Map<String, AITip>,
    onShowRecommendations: () -> Unit,
    onShowChallenges: () -> Unit
) {
    val latestGroupMessages = remember { mutableStateMapOf<String, String>() }
    val latestPrivateMessages = remember { mutableStateMapOf<String, String>() }
    val coroutineScope = rememberCoroutineScope()
    val firestore = FirebaseFirestore.getInstance()
    val context = LocalContext.current
    var currentUser by remember { mutableStateOf<User?>(null) }
    var showPartnerDialog by remember { mutableStateOf(false) }
    var selectedPartnerId by remember { mutableStateOf("") }
    var selectedPartnerName by remember { mutableStateOf("") }
    var messagesLoaded by remember { mutableStateOf(false) }

    LaunchedEffect(userEmail, studyGroups) {
        if (!messagesLoaded) {
            currentUser = userRepository.getUserByEmail(userEmail)
            coroutineScope.launch {
                try {
                    val allMessagesSnapshot = firestore.collection("groupMessages").get().await()
                    studyGroups.forEach { group ->
                        val groupMessages = allMessagesSnapshot.documents
                            .mapNotNull { doc -> doc.toObject(GroupChatMessage::class.java)?.takeIf { it.groupId == group.id } }
                            .sortedByDescending { it.timestamp?.seconds ?: 0 }
                        if (groupMessages.isNotEmpty()) {
                            latestGroupMessages[group.id] = "${groupMessages.first().senderName}: ${groupMessages.first().content}"
                        }
                    }
                    val privateMessagesSnapshot = firestore.collection("privateMessages").get().await()
                    currentUser?.partners?.forEach { partnerId ->
                        val chatId = if (currentUser!!.id < partnerId) "${currentUser!!.id}_$partnerId" else "${partnerId}_${currentUser!!.id}"
                        val chatMessages = privateMessagesSnapshot.documents
                            .mapNotNull { doc -> doc.toObject(PrivateChatMessage::class.java)?.takeIf { it.chatId == chatId } }
                            .sortedByDescending { it.timestamp?.seconds ?: 0 }
                        if (chatMessages.isNotEmpty()) {
                            latestPrivateMessages[partnerId] = chatMessages.first().content
                        }
                    }
                    messagesLoaded = true
                } catch (e: Exception) {
                    Log.e("CommunityScreen", "Error loading messages", e)
                }
            }
        }
    }

    if (showPartnerDialog) {
        AlertDialog(
            onDismissRequest = { showPartnerDialog = false },
            title = { Text("Kết nối đối tác học tập") },
            text = { Text("Bạn muốn kết nối với $selectedPartnerName làm đối tác học tập?") },
            confirmButton = {
                Button(
                    onClick = {
                        coroutineScope.launch {
                            try {
                                currentUser?.let { user ->
                                    val updatedUser = user.addPartner(selectedPartnerId)
                                    userRepository.updateUser(updatedUser)
                                    val partner = userRepository.getUserById(selectedPartnerId)
                                    partner?.let { partnerUser ->
                                        val updatedPartner = partnerUser.addPartner(user.id)
                                        userRepository.updateUser(updatedPartner)
                                        sendPartnerConnectionNotification(user, partnerUser)
                                    }
                                    currentUser = updatedUser
                                    Toast.makeText(context, "Đã kết nối với $selectedPartnerName", Toast.LENGTH_SHORT).show()
                                }
                            } catch (e: Exception) {
                                Toast.makeText(context, "Lỗi: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                            showPartnerDialog = false
                        }
                    }
                ) { Text("Kết nối") }
            },
            dismissButton = {
                Button(onClick = { showPartnerDialog = false }) { Text("Hủy") }
            }
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { Spacer(modifier = Modifier.height(12.dp)) }

        if (smartRecommendations.isNotEmpty()) {
            item {
                AIRecommendationCard(
                    recommendationCount = smartRecommendations.size,
                    topRecommendation = smartRecommendations.firstOrNull(),
                    onClick = onShowRecommendations
                )
            }
        }

        val totalChallenges = groupChallenges.values.flatten().size
        if (totalChallenges > 0) {
            item {
                ActiveChallengesCard(
                    challengeCount = totalChallenges,
                    onClick = onShowChallenges
                )
            }
        }
        item {
            Spacer(modifier = Modifier.height(12.dp))
            Text("Đang hoạt động", style = MaterialTheme.typography.labelMedium, color = Color.Gray)
            RealTimeClock()
            Spacer(modifier = Modifier.height(8.dp))
        }

        item {
            val onlineUsers = users.filter { it.online && it.id != currentUser?.id }
            if (onlineUsers.isEmpty()) {
                Text(
                    "Không có người dùng nào đang hoạt động",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            } else {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(onlineUsers) { user ->
                        val isPartner = currentUser?.partners?.contains(user.id) == true
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.clickable(enabled = isPartner) {
                                if (isPartner) {
                                    navController.navigate("private_chat/${user.id}/$userEmail")
                                }
                            }
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(60.dp)
                                    .clip(CircleShape)
                                    .background(Color.LightGray)
                                    .alpha(if (isPartner) 1f else 0.6f),
                                contentAlignment = Alignment.Center
                            ) {
                                AsyncImage(
                                    model = user.imageUrl,
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                                Box(
                                    modifier = Modifier
                                        .size(15.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF00C853))
                                        .align(Alignment.BottomEnd)
                                        .border(2.dp, Color.White, CircleShape)
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = user.username,
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                color = if (isPartner) Color.Unspecified else Color.Gray
                            )
                            if (!isPartner) {
                                Text(
                                    text = "Chưa kết nối",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontSize = 10.sp,
                                    color = Color.Gray
                                )
                            }
                        }
                    }
                }
            }
        }

        if (studyGroups.isNotEmpty()) {
            item {
                Text(
                    "Nhóm học tập",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                )
            }
            items(studyGroups) { group ->
                EnhancedStudyGroupCard(
                    group = group,
                    dailyTip = dailyTips[group.id],
                    challenges = groupChallenges[group.id] ?: emptyList(),
                    onClick = {
                        navController.navigate("group_chat/${group.id}/$userEmail")
                    },
                    latestMessage = latestGroupMessages[group.id]
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
            Text("Đối tác học tập", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
        }

        if (currentUser?.partners?.isNotEmpty() == true) {
            item {
                Text(
                    "Đối tác của bạn",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color(0xFF00C853),
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
            val partnerIds = currentUser?.partners ?: emptyList()
            val partners = users.filter { it.id in partnerIds }
            items(partners) { partner ->
                StudyPartnerCard(
                    user = partner,
                    currentUser = currentUser,
                    navController = navController,
                    userEmail = userEmail,
                    onPartnerRequest = { },
                    latestMessage = latestPrivateMessages[partner.id]
                )
            }
            item {
                Divider(modifier = Modifier.padding(vertical = 8.dp))
                Text(
                    "Người dùng khác",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.Gray
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        val nonPartners = users.filter {
            it.id != currentUser?.id && it.id !in (currentUser?.partners ?: emptyList())
        }
        items(nonPartners) { user ->
            StudyPartnerCard(
                user = user,
                currentUser = currentUser,
                navController = navController,
                userEmail = userEmail,
                onPartnerRequest = { partnerId ->
                    selectedPartnerId = partnerId
                    selectedPartnerName = user.username
                    showPartnerDialog = true
                },
                latestMessage = null
            )
        }
        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}
// Thêm hàm gửi thông báo kết nối đối tác
private suspend fun sendPartnerConnectionNotification(currentUser: User, partner: User) {
    try {
        val firestore = FirebaseFirestore.getInstance()

        // Tạo thông báo trong Firestore
        val notification = hashMapOf(
            "userId" to partner.id,
            "title" to "Kết nối đối tác học tập mới",
            "message" to "${currentUser.username} đã kết nối với bạn làm đối tác học tập",
            "timestamp" to FieldValue.serverTimestamp(),
            "read" to false,
            "type" to "partner_connection",
            "referenceId" to currentUser.id,
            "senderId" to currentUser.id
        )

        // Lưu thông báo vào Firestore
        firestore.collection("notifications")
            .add(notification)
            .await()

        // Gửi thông báo push qua OneSignal (nếu có)
        try {
            val notifyViewModel = AdminNotifyPageViewModel()
            notifyViewModel.sendNotificationToSpecificUser(
                "Kết nối đối tác học tập mới",
                "${currentUser.username} đã kết nối với bạn làm đối tác học tập",
                partner.email
            )

            Log.d("PartnerConnection", "Notification sent to: ${partner.username}")
        } catch (e: Exception) {
            Log.e("PartnerConnection", "Failed to send push notification", e)
        }
    } catch (e: Exception) {
        Log.e("PartnerConnection", "Error creating notification", e)
    }
}

@Composable
fun AIRecommendationCard(
    recommendationCount: Int,
    topRecommendation: UserRecommendation?,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFE8F5E9)
        ),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        color = Color(0xFF4CAF50),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Gợi ý kết nối thông minh",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF2E7D32)
                )

                topRecommendation?.let {
                    Text(
                        text = "${it.username} - ${it.matchScore}% phù hợp",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF558B2F)
                    )
                }

                Text(
                    text = "$recommendationCount người phù hợp với bạn",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }

            Icon(
                imageVector = Icons.Default.ArrowForward,
                contentDescription = null,
                tint = Color(0xFF4CAF50)
            )
        }
    }
}

// === Active Challenges Card ===
@Composable
fun ActiveChallengesCard(
    challengeCount: Int,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFFFF3E0)
        ),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        color = Color(0xFFFF9800),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.EmojiEvents,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Thử thách đang diễn ra",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFE65100)
                )

                Text(
                    text = "$challengeCount thử thách đang chờ bạn",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }

            Icon(
                imageVector = Icons.Default.ArrowForward,
                contentDescription = null,
                tint = Color(0xFFFF9800)
            )
        }
    }
}

@Composable
fun DiscussionTab(
    discussions: List<Discussion>,
    navController: NavController,  // Thêm NavController
    userEmail: String  // Thêm userEmail
) {
    var currentUser by remember { mutableStateOf<User?>(null) }
    val userRepository = remember { UserRepository() }
    val coroutineScope = rememberCoroutineScope()
    val firestore = FirebaseFirestore.getInstance()

    // Thêm state để lưu trữ tin nhắn mới nhất cho mỗi cuộc thảo luận
    val latestDiscussionMessages = remember { mutableStateMapOf<String, String>() }
    var messagesLoaded by remember { mutableStateOf(false) }

    // Lấy thông tin người dùng hiện tại
    LaunchedEffect(userEmail) {
        try {
            currentUser = userRepository.getUserByEmail(userEmail)
            Log.d("DiscussionTab", "Loaded current user: ${currentUser?.username}")
        } catch (e: Exception) {
            Log.e("DiscussionTab", "Error loading current user", e)
        }
    }

    // Tải tin nhắn mới nhất cho mỗi cuộc thảo luận
    LaunchedEffect(discussions) {
        if (!messagesLoaded && discussions.isNotEmpty()) {
            coroutineScope.launch {
                try {
                    // Lấy tất cả tin nhắn thảo luận
                    val allMessagesSnapshot = firestore.collection("discussionMessages")
                        .get()
                        .await()

                    Log.d("DiscussionTab", "Fetched ${allMessagesSnapshot.size()} total discussion messages")

                    // Lọc và xử lý tin nhắn cho từng cuộc thảo luận
                    discussions.forEach { discussion ->
                        val discussionMessages = allMessagesSnapshot.documents
                            .mapNotNull { doc ->
                                val message = doc.toObject(DiscussionMessage::class.java)
                                if (message?.discussionId == discussion.id) message else null
                            }
                            .sortedByDescending { it.timestamp?.seconds ?: 0 }

                        Log.d("DiscussionTab", "Found ${discussionMessages.size} messages for discussion ${discussion.id}")

                        if (discussionMessages.isNotEmpty()) {
                            val latestMessage = discussionMessages.first()
                            latestDiscussionMessages[discussion.id] = "${latestMessage.senderName}: ${latestMessage.content}"
                            Log.d("DiscussionTab", "Latest message for discussion ${discussion.id}: ${latestMessage.content}")
                        }
                    }

                    messagesLoaded = true
                } catch (e: Exception) {
                    Log.e("DiscussionTab", "Error loading messages", e)
                }
            }
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Cuộc thảo luận", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Button(
                    onClick = {
                        // Điều hướng đến màn hình tạo thảo luận mới
                        navController.navigate("create_discussion/$userEmail")
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00C853))
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Tạo mới")
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        // Discussions from Firestore
        if (discussions.isEmpty()) {
            item {
                Text(
                    "Không tìm thấy thảo luận nào. Hãy tạo một chủ đề mới.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp)
                )
            }
        } else {
            items(discussions) { discussion ->
                DiscussionCardWithChat(
                    discussion = discussion,
                    latestMessage = latestDiscussionMessages[discussion.id],
                    onClick = {
                        // Điều hướng đến màn hình chat thảo luận
                        navController.navigate("discussion_chat/${discussion.id}/$userEmail")
                    }
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun DiscussionCardWithChat(
    discussion: Discussion,
    latestMessage: String?,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
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
                        model = discussion.authorImageUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = discussion.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = discussion.authorName,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = discussion.content,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.DarkGray,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Tags
            if (discussion.tags.isNotEmpty()) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    discussion.tags.take(3).forEach { tag ->
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

            Spacer(modifier = Modifier.height(8.dp))

            // Thông tin bổ sung
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Thời gian tạo
                val formattedDate = remember(discussion.createdAt) {
                    val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                    sdf.format(Date(discussion.createdAt))
                }

                Text(
                    text = formattedDate,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )

                // Số lượng bình luận
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Comment,
                        contentDescription = null,
                        tint = Color(0xFF00C853),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${discussion.commentCount} bình luận",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF00C853)
                    )
                }
            }

            // Hiển thị tin nhắn mới nhất nếu có
            if (!latestMessage.isNullOrEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Divider(color = Color.LightGray, thickness = 0.5.dp)
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Comment,
                        contentDescription = null,
                        tint = Color.Gray,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = latestMessage,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF00897B),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontStyle = FontStyle.Italic
                    )
                }
            }
        }
    }
}

@Composable
fun LeaderboardTab(users: List<User>, learningGoals: List<LearningGoal>) {
    // Tính điểm học tập dựa trên dữ liệu thực
    val sortedUsers = remember(users) {
        // Sử dụng điểm năng động thực tế từ đối tượng User
        users.map { user ->
            // Tạo cặp User và điểm năng động của họ
            user to (user.activityPoints)
        }.sortedByDescending { it.second } // Sắp xếp theo điểm giảm dần
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(16.dp))
            Text("Bảng xếp hạng tuần này", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text("Dựa trên các điểm học tập", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Top 3 users with special styling
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                if (sortedUsers.size > 1) {
                    // 2nd place
                    TopUserCard(sortedUsers[1].first, sortedUsers[1].second, 2, Color(0xFFC0C0C0))
                }

                if (sortedUsers.isNotEmpty()) {
                    // 1st place
                    TopUserCard(sortedUsers[0].first, sortedUsers[0].second, 1, Color(0xFFFFD700))
                }

                if (sortedUsers.size > 2) {
                    // 3rd place
                    TopUserCard(sortedUsers[2].first, sortedUsers[2].second, 3, Color(0xFFCD7F32))
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }

        // Rest of the leaderboard - hiển thị tất cả người dùng còn lại
        val remainingUsers = sortedUsers.drop(3) // Bỏ top 3, lấy tất cả người dùng còn lại
        for (i in remainingUsers.indices) {
            item {
                LeaderboardRow(
                    user = remainingUsers[i].first,
                    points = remainingUsers[i].second,
                    rank = i + 4, // Thứ hạng bắt đầu từ 4
                    userRank = remainingUsers[i].first.rank
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
            Divider()
            Spacer(modifier = Modifier.height(16.dp))
            Text("Mục tiêu học tập của bạn", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))

            // Learning goals from Firestore
            if (learningGoals.isEmpty()) {
                Text(
                    "Chưa có mục tiêu nào được đặt ra. Đặt ra mục tiêu mới!",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                learningGoals.forEach { goal ->
                    val progress = if (goal.target > 0) goal.current.toFloat() / goal.target else 0f
                    LearningGoalCard(
                        title = goal.title,
                        progress = progress,
                        description = goal.description
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
// AI UI Components - Thêm vào file CommunityScreenFull.kt

// === Enhanced Study Group Card with AI ===
@Composable
fun EnhancedStudyGroupCard(
    group: StudyGroup,
    dailyTip: AITip?,
    challenges: List<GroupChallenge>,
    onClick: () -> Unit,
    latestMessage: String? = null
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.Group,
                    contentDescription = null,
                    tint = Color(0xFF00C853),
                    modifier = Modifier.size(32.dp)
                )

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = group.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = "${group.memberCount} thành viên",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }
            }
            if (!latestMessage.isNullOrEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            color = Color(0xFFF5F5F5),
                            shape = RoundedCornerShape(8.dp)
                        )
                        .padding(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Comment,
                        contentDescription = null,
                        tint = Color.Gray,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = latestMessage,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF00897B), // Đổi màu cho nổi bật
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontStyle = FontStyle.Italic,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

//            // Daily Tip
//            dailyTip?.let { tip ->
//                Spacer(modifier = Modifier.height(12.dp))
//                Card(
//                    modifier = Modifier.fillMaxWidth(),
//                    colors = CardDefaults.cardColors(
//                        containerColor = Color(0xFFFFF9C4)
//                    ),
//                    shape = RoundedCornerShape(8.dp)
//                ) {
//                    Row(
//                        modifier = Modifier.padding(12.dp),
//                        verticalAlignment = Alignment.Top
//                    ) {
//                        Icon(
//                            imageVector = Icons.Default.Lightbulb,
//                            contentDescription = null,
//                            tint = Color(0xFFF57C00),
//                            modifier = Modifier.size(20.dp)
//                        )
//
//                        Spacer(modifier = Modifier.width(8.dp))
//
//                        Column {
//                            Text(
//                                text = "Mẹo hôm nay",
//                                style = MaterialTheme.typography.labelSmall,
//                                fontWeight = FontWeight.Bold,
//                                color = Color(0xFFE65100)
//                            )
//
//                            Text(
//                                text = tip.tip,
//                                style = MaterialTheme.typography.bodySmall,
//                                color = Color(0xFF5D4037)
//                            )
//                        }
//                    }
//                }
//            }

            // Active Challenges
            if (challenges.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                challenges.take(2).forEach { challenge ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.EmojiEvents,
                            contentDescription = null,
                            tint = Color(0xFFFF9800),
                            modifier = Modifier.size(16.dp)
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        Text(
                            text = challenge.title,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFE65100),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                if (challenges.size > 2) {
                    Text(
                        text = "+${challenges.size - 2} thử thách khác",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Gray,
                        modifier = Modifier.padding(start = 24.dp)
                    )
                }
            }
        }
    }
}


@Composable
fun ChatBubble(message: String, isUser: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        Card(
            colors = CardDefaults.cardColors(
                containerColor = if (isUser) Color(0xFF4CAF50) else Color(0xFFF5F5F5)
            ),
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (isUser) 16.dp else 4.dp,
                bottomEnd = if (isUser) 4.dp else 16.dp
            )
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

// === Smart Recommendations Dialog ===
@Composable
fun SmartRecommendationsDialog(
    recommendations: List<UserRecommendation>,
    currentUser: User?,
    onConnect: (String) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = Color(0xFF4CAF50)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Gợi ý kết nối thông minh")
            }
        },
        text = {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 500.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(recommendations) { recommendation ->
                    RecommendationCard(
                        recommendation = recommendation,
                        onConnect = { onConnect(recommendation.userId) }
                    )
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Đóng")
            }
        }
    )
}

@Composable
fun RecommendationCard(
    recommendation: UserRecommendation,
    onConnect: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Avatar
                AsyncImage(
                    model = recommendation.imageUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = recommendation.username,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    recommendation.jlptLevel?.let {
                        Text(
                            text = "JLPT N$it",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF4CAF50)
                        )
                    }
                }

                // Match Score
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .background(
                            color = Color(0xFFE8F5E9),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "${recommendation.matchScore}%",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF4CAF50)
                        )
                        Text(
                            text = "phù hợp",
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 8.sp,
                            color = Color.Gray
                        )
                    }
                }
            }

            // Match Reasons
            if (recommendation.matchReasons.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))

                recommendation.matchReasons.take(3).forEach { reason ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(vertical = 2.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = Color(0xFF4CAF50),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = reason,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = onConnect,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF4CAF50)
                )
            ) {
                Text("Kết nối")
            }
        }
    }
}

// === Group Challenges Dialog ===
@Composable
fun GroupChallengesDialog(
    challenges: Map<String, List<GroupChallenge>>,
    studyGroups: List<StudyGroup>,
    currentUser: User?,
    onJoinChallenge: (String) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.EmojiEvents,
                    contentDescription = null,
                    tint = Color(0xFFFF9800)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Thử thách nhóm")
            }
        },
        text = {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 500.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                challenges.forEach { (groupId, groupChallenges) ->
                    val group = studyGroups.find { it.id == groupId }

                    item {
                        Text(
                            text = group?.title ?: "Group",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF4CAF50)
                        )
                    }

                    items(groupChallenges) { challenge ->
                        ChallengeCard(
                            challenge = challenge,
                            currentUserId = currentUser?.id,
                            onJoin = { onJoinChallenge(challenge.id) }
                        )
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Đóng")
            }
        }
    )
}

@Composable
fun ChallengeCard(
    challenge: GroupChallenge,
    currentUserId: String?,
    onJoin: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0)),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.EmojiEvents,
                    contentDescription = null,
                    tint = Color(0xFFFF9800),
                    modifier = Modifier.size(32.dp)
                )

                Spacer(modifier = Modifier.width(12.dp))

                Text(
                    text = challenge.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = challenge.description,
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Progress
            val userProgress = currentUserId?.let { challenge.participants[it] } ?: 0
            val progressPercent = if (challenge.targetValue > 0)
                (userProgress.toFloat() / challenge.targetValue) * 100 else 0f

            LinearProgressIndicator(
                progress = { progressPercent / 100 },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = Color(0xFFFF9800),
                trackColor = Color(0xFFFFE0B2)
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "$userProgress / ${challenge.targetValue}",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Rewards
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "🥇 ${challenge.rewards["top1"]}đ | 🥈 ${challenge.rewards["top2"]}đ | 🥉 ${challenge.rewards["top3"]}đ",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFE65100)
                )
            }
        }
    }
}
@Composable
fun StudyGroupCard(
    title: String,
    members: String,
    description: String,
    onClick: () -> Unit,
    latestMessage: String? = null
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Group,
                    contentDescription = null,
                    tint = Color(0xFF00C853),
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = members,
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Hiển thị mô tả
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.DarkGray,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            // Hiển thị tin nhắn mới nhất nếu có - THÊM PHẦN NÀY
            if (!latestMessage.isNullOrEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Divider(color = Color.LightGray, thickness = 0.5.dp)
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Comment,
                        contentDescription = null,
                        tint = Color.Gray,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = latestMessage,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF00897B),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontStyle = FontStyle.Italic
                    )
                }
            }
        }
    }
    Spacer(modifier = Modifier.height(8.dp))
}

@Composable
fun StudyPartnerCard(
    user: User,
    currentUser: User?,
    navController: NavController,
    userEmail: String,
    onPartnerRequest: (String) -> Unit,
    latestMessage: String?
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Avatar
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Color.LightGray),
                    contentAlignment = Alignment.Center
                ) {
                    AsyncImage(
                        model = user.imageUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )

                    // Online indicator
                    if (user.online) {
                        Box(
                            modifier = Modifier
                                .size(14.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF00C853))
                                .align(Alignment.BottomEnd)
                                .border(2.dp, Color.White, CircleShape)
                        )
                    }
                }

                // User info
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 12.dp)
                ) {
                    Text(
                        text = user.username,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = user.rank,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )

                    if (user.jlptLevel != null) {
                        Text(
                            text = "JLPT N${user.jlptLevel}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF00897B)
                        )
                    }
                }

                // Nút Kết nối hoặc Chat
                if (currentUser == null) {
                    // Không hiển thị nút nếu chưa đăng nhập
                }
                // Nút Kết nối - Chỉ hiển thị cho người dùng VIP và không phải đối tác hiện tại
                else if (currentUser.id != user.id && !currentUser.partners.contains(user.id)) {
                    Button(
                        onClick = { onPartnerRequest(user.id) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00C853)),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text("Kết nối")
                    }
                }
                // Nút Chat - Hiển thị cho người dùng VIP đã kết nối làm đối tác
                else if (currentUser.id != user.id && currentUser.partners.contains(user.id)) {
                    Button(
                        onClick = { navController.navigate("private_chat/${user.id}/$userEmail") },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00C853)),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text("Chat")
                    }
                }
            }

            // Hiển thị tin nhắn mới nhất nếu có
            if (!latestMessage.isNullOrEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Divider(color = Color.LightGray, thickness = 0.5.dp)
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Comment,
                        contentDescription = null,
                        tint = Color.Gray,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = latestMessage,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.DarkGray,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontStyle = FontStyle.Italic
                    )
                }
            }
        }
    }
    Spacer(modifier = Modifier.height(4.dp))
}

@Composable
fun DiscussionCard(title: String, preview: String, user: User?) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { /* Open discussion */ },
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = preview,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.DarkGray,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                user?.let {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(Color.LightGray)
                    ) {
                        AsyncImage(
                            model = it.imageUrl,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = it.username,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
                Icon(
                    imageVector = Icons.Default.Comment,
                    contentDescription = null,
                    tint = Color(0xFF00C853),
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "${(3..15).random()}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }
        }
    }
    Spacer(modifier = Modifier.height(8.dp))
}

@Composable
fun TopUserCard(user: User, points: Int, rank: Int, color: Color) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(8.dp)
    ) {
        // Huy chương với số thứ tự
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(color),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "#$rank",
                style = MaterialTheme.typography.titleLarge,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Ảnh đại diện người dùng
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(Color.LightGray),
            contentAlignment = Alignment.Center
        ) {
            AsyncImage(
                model = user.imageUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Tên người dùng
        Text(
            text = user.username,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        // Rank của người dùng
        Text(
            text = user.rank,
            style = MaterialTheme.typography.bodySmall,
            color = Color.Gray
        )

        // Điểm số
        Text(
            text = "$points điểm",
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFF4CAF50),
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun LeaderboardRow(user: User, points: Int, rank: Int, userRank: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Color.LightGray),
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = user.imageUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(user.username, style = MaterialTheme.typography.titleSmall)
                Text(
                    text = userRank, // Hiển thị rank của người dùng
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
                Text(
                    text = "${points} điểm", // Hiển thị điểm
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFE0E0E0)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "#$rank",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.Gray
                )
            }
        }
    }
}

@Composable
fun LearningGoalCard(title: String, progress: Float, description: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = progress,
                modifier = Modifier.fillMaxWidth(),
                color = Color(0xFF00C853),
                trackColor = Color(0xFFE0E0E0)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray
            )
        }
    }
}

@Composable
fun RealTimeClock() {
    var currentTime by remember { mutableStateOf(getCurrentFormattedTime()) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(1000) // Cập nhật mỗi giây
            currentTime = getCurrentFormattedTime()
        }
    }

    Text(
        text = currentTime,
        color = Color(0xFF00C853),
        style = MaterialTheme.typography.labelSmall
    )
}

fun getCurrentFormattedTime(): String {
    val sdf = SimpleDateFormat("hh:mm a", Locale.getDefault())
    return sdf.format(Date())
}

// Hàm để lấy dữ liệu từ Firestore
suspend fun getStudyGroupsFromFirestore(): List<StudyGroup> {
    val firestore = FirebaseFirestore.getInstance()
    return try {
        Log.d("Firestore", "Fetching study groups")
        val snapshot = firestore.collection("studyGroups")
            .get()
            .await()

        val groups = snapshot.documents.mapNotNull { doc ->
            doc.toObject(StudyGroup::class.java)?.apply {
                id = doc.id // Gán ID từ document
            }
        }

        Log.d("Firestore", "Fetched ${groups.size} study groups")
        groups
    } catch (e: Exception) {
        Log.e("Firestore", "Error getting study groups", e)
        emptyList()
    }
}

suspend fun getDiscussionsFromFirestore(): List<Discussion> {
    val firestore = FirebaseFirestore.getInstance()
    return try {
        Log.d("Firestore", "Fetching discussions")
        // Không sử dụng orderBy để tránh lỗi index
        val snapshot = firestore.collection("discussions")
            .get()
            .await()

        // Sắp xếp ở phía client
        val discussions = snapshot.documents.mapNotNull { doc ->
            doc.toObject(Discussion::class.java)?.apply {
                id = doc.id
            }
        }.sortedByDescending { it.createdAt } // Sử dụng createdAt thay vì timestamp

        Log.d("Firestore", "Fetched ${discussions.size} discussions")
        discussions
    } catch (e: Exception) {
        Log.e("Firestore", "Error getting discussions", e)
        emptyList()
    }
}

suspend fun getLearningGoalsFromFirestore(userId: String): List<LearningGoal> {
    val firestore = FirebaseFirestore.getInstance()
    return try {
        val snapshot = firestore.collection("users")
            .document(userId)
            .collection("learningGoals")
            .get()
            .await()

        snapshot.documents.mapNotNull { doc ->
            doc.toObject(LearningGoal::class.java)?.apply {
                id = doc.id
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
        emptyList()
    }
}

// Hàm để tạo dữ liệu mẫu và đẩy lên Firestore
fun uploadSampleDataToFirestore(context: Context) {
    val firestore = FirebaseFirestore.getInstance()

    // Xóa dữ liệu cũ trước khi thêm dữ liệu mới
    clearExistingData(firestore, context) { success ->
        if (success) {
            // Sau khi xóa thành công, thêm dữ liệu mới
            addSampleData(firestore, context)
        } else {
            Toast.makeText(context, "Không thể xóa dữ liệu cũ", Toast.LENGTH_SHORT).show()
        }
    }
}

// Hàm xóa dữ liệu cũ
private fun clearExistingData(firestore: FirebaseFirestore, context: Context, onComplete: (Boolean) -> Unit) {
    // Danh sách các collection cần xóa
    val collectionsToDelete = listOf("studyGroups", "discussions")
    var completedCount = 0
    var successCount = 0

    // Xóa dữ liệu từ các collection chính
    for (collectionName in collectionsToDelete) {
        firestore.collection(collectionName)
            .get()
            .addOnSuccessListener { snapshot ->
                val batch = firestore.batch()
                for (document in snapshot.documents) {
                    batch.delete(document.reference)
                }

                if (snapshot.documents.isNotEmpty()) {
                    batch.commit()
                        .addOnSuccessListener {
                            Log.d("Firestore", "Đã xóa thành công tất cả tài liệu trong $collectionName")
                            successCount++
                            completedCount++
                            if (completedCount == collectionsToDelete.size) {
                                onComplete(successCount == completedCount)
                            }
                        }
                        .addOnFailureListener { e ->
                            Log.e("Firestore", "Lỗi khi xóa tài liệu trong $collectionName", e)
                            completedCount++
                            if (completedCount == collectionsToDelete.size) {
                                onComplete(successCount == completedCount)
                            }
                        }
                } else {
                    // Collection trống, không cần xóa
                    Log.d("Firestore", "Không có tài liệu để xóa trong $collectionName")
                    successCount++
                    completedCount++
                    if (completedCount == collectionsToDelete.size) {
                        onComplete(successCount == completedCount)
                    }
                }
            }
            .addOnFailureListener { e ->
                Log.e("Firestore", "Lỗi khi lấy tài liệu từ $collectionName", e)
                completedCount++
                if (completedCount == collectionsToDelete.size) {
                    onComplete(successCount == completedCount)
                }
            }
    }

    // Xóa learning goals cho tất cả người dùng
    // Lưu ý: Đây là một thao tác phức tạp hơn vì cần duyệt qua từng người dùng
    firestore.collection("users")
        .get()
        .addOnSuccessListener { usersSnapshot ->
            var userProcessed = 0
            var userSuccess = 0

            if (usersSnapshot.documents.isEmpty()) {
                // Không có người dùng, coi như thành công
                completedCount++
                successCount++
                if (completedCount == collectionsToDelete.size + 1) {
                    onComplete(successCount == completedCount)
                }
                return@addOnSuccessListener
            }

            for (userDoc in usersSnapshot.documents) {
                val userId = userDoc.id
                firestore.collection("users")
                    .document(userId)
                    .collection("learningGoals")
                    .get()
                    .addOnSuccessListener { goalsSnapshot ->
                        val batch = firestore.batch()
                        for (goalDoc in goalsSnapshot.documents) {
                            batch.delete(goalDoc.reference)
                        }

                        if (goalsSnapshot.documents.isNotEmpty()) {
                            batch.commit()
                                .addOnSuccessListener {
                                    Log.d("Firestore", "Đã xóa thành công mục tiêu học tập cho người dùng $userId")
                                    userSuccess++
                                    userProcessed++
                                    if (userProcessed == usersSnapshot.size()) {
                                        completedCount++
                                        if (userSuccess == userProcessed) {
                                            successCount++
                                        }
                                        if (completedCount == collectionsToDelete.size + 1) {
                                            onComplete(successCount == completedCount)
                                        }
                                    }
                                }
                                .addOnFailureListener { e ->
                                    Log.e("Firestore", "Lỗi khi xóa mục tiêu học tập cho người dùng $userId", e)
                                    userProcessed++
                                    if (userProcessed == usersSnapshot.size()) {
                                        completedCount++
                                        if (completedCount == collectionsToDelete.size + 1) {
                                            onComplete(successCount == completedCount)
                                        }
                                    }
                                }
                        } else {
                            // Không có learning goals, coi như thành công
                            userSuccess++
                            userProcessed++
                            if (userProcessed == usersSnapshot.size()) {
                                completedCount++
                                if (userSuccess == userProcessed) {
                                    successCount++
                                }
                                if (completedCount == collectionsToDelete.size + 1) {
                                    onComplete(successCount == completedCount)
                                }
                            }
                        }
                    }
                    .addOnFailureListener { e ->
                        Log.e("Firestore", "Lỗi khi lấy mục tiêu học tập cho người dùng $userId", e)
                        userProcessed++
                        if (userProcessed == usersSnapshot.size()) {
                            completedCount++
                            if (completedCount == collectionsToDelete.size + 1) {
                                onComplete(successCount == completedCount)
                            }
                        }
                    }
            }
        }
        .addOnFailureListener { e ->
            Log.e("Firestore", "Lỗi khi lấy danh sách người dùng", e)
            completedCount++
            if (completedCount == collectionsToDelete.size + 1) {
                onComplete(successCount == completedCount)
            }
        }
}

// Hàm thêm dữ liệu mẫu mới
private fun addSampleData(firestore: FirebaseFirestore, context: Context) {
    // Lấy danh sách tất cả người dùng
    firestore.collection("users").get()
        .addOnSuccessListener { usersSnapshot ->
            val userIds = usersSnapshot.documents.map { it.id }
            // 1. Tạo dữ liệu mẫu cho Study Groups
            val sampleStudyGroups = listOf(
                StudyGroup(
                    id = "",
                    title = "N5 初心者",
                    description = "N5レベルの基本的な文法と語彙を学びましょう。",
                    memberCount = 15,
                    createdBy = "system",
                    createdAt = System.currentTimeMillis(),
                    imageUrl = "https://example.com/images/n5_group.jpg",
                    members = userIds  // Thêm tất cả người dùng vào nhóm
                ),
                StudyGroup(
                    id = "",
                    title = "会話練習",
                    description = "日常会話を練習するグループです。初心者歓迎！",
                    memberCount = 12,
                    createdBy = "system",
                    createdAt = System.currentTimeMillis(),
                    imageUrl = "https://example.com/images/conversation_group.jpg",
                    members = userIds  // Thêm tất cả người dùng vào nhóm
                ),
                StudyGroup(
                    id = "",
                    title = "漢字マスター",
                    description = "一緒に漢字を勉強しましょう。N4-N2レベル。",
                    memberCount = 8,
                    createdBy = "system",
                    createdAt = System.currentTimeMillis(),
                    imageUrl = "https://example.com/images/kanji_group.jpg",
                    members = userIds  // Thêm tất cả người dùng vào nhóm
                ),
                StudyGroup(
                    id = "",
                    title = "JLPT N3 対策",
                    description = "JLPT N3試験の準備をしているメンバーのグループです。",
                    memberCount = 10,
                    createdBy = "system",
                    createdAt = System.currentTimeMillis(),
                    imageUrl = "https://example.com/images/n3_group.jpg",
                    members = userIds  // Thêm tất cả người dùng vào nhóm
                ),
                StudyGroup(
                    id = "",
                    title = "アニメで日本語",
                    description = "アニメを通じて日本語を学ぶグループです。楽しく学びましょう！",
                    memberCount = 20,
                    createdBy = "system",
                    createdAt = System.currentTimeMillis(),
                    imageUrl = "https://example.com/images/anime_group.jpg",
                    members = userIds  // Thêm tất cả người dùng vào nhóm
                )
            )

            // 2. Tạo dữ liệu mẫu cho Discussions
            val sampleDiscussions = listOf(
                Discussion(
                    id = "",
                    title = "文法について質問",
                    content = "「て形」と「た形」の違いは何ですか？初心者ですが、この違いがよく分かりません。例文があれば助かります。",
                    authorId = "system",
                    authorName = "Tanaka",
                    authorImageUrl = "https://example.com/images/tanaka.jpg",
                    commentCount = 7,
                    createdAt = System.currentTimeMillis() - 86400000, // 1 day ago
                    tags = listOf("文法", "初心者", "質問")
                ),
                Discussion(
                    id = "",
                    title = "おすすめの勉強法",
                    content = "皆さんはどうやって単語を覚えていますか？アプリ、フラッシュカード、それとも他の方法？アドバイスをお願いします。",
                    authorId = "system",
                    authorName = "Yamada",
                    authorImageUrl = "https://example.com/images/yamada.jpg",
                    commentCount = 12,
                    createdAt = System.currentTimeMillis() - 172800000, // 2 days ago
                    tags = listOf("勉強法", "単語", "アドバイス")
                ),
                Discussion(
                    id = "",
                    title = "JLPT N5 試験",
                    content = "12月のJLPT試験に向けて一緒に勉強しませんか？オンラインで勉強会を開きたいと思います。興味のある方はコメントしてください。",
                    authorId = "system",
                    authorName = "Suzuki",
                    authorImageUrl = "https://example.com/images/suzuki.jpg",
                    commentCount = 15,
                    createdAt = System.currentTimeMillis() - 259200000, // 3 days ago
                    tags = listOf("JLPT", "N5", "勉強会")
                ),
                Discussion(
                    id = "",
                    title = "アニメで日本語",
                    content = "おすすめの初心者向けアニメはありますか？字幕付きで日本語を勉強したいです。簡単な日本語を使っているアニメが理想です。",
                    authorId = "system",
                    authorName = "Sato",
                    authorImageUrl = "https://example.com/images/sato.jpg",
                    commentCount = 9,
                    createdAt = System.currentTimeMillis() - 345600000, // 4 days ago
                    tags = listOf("アニメ", "初心者", "リスニング")
                ),
                Discussion(
                    id = "",
                    title = "発音の練習",
                    content = "「つ」と「す」の発音が難しいです。アドバイスください！特に「つ」の発音のコツを知りたいです。",
                    authorId = "system",
                    authorName = "Kato",
                    authorImageUrl = "https://example.com/images/kato.jpg",
                    commentCount = 6,
                    createdAt = System.currentTimeMillis() - 432000000, // 5 days ago
                    tags = listOf("発音", "練習", "アドバイス")
                )
            )

            // 3. Tạo dữ liệu mẫu cho Learning Goals (cần user ID)
            fun createSampleLearningGoals(userId: String) {
                val sampleLearningGoals = listOf(
                    LearningGoal(
                        id = "",
                        title = "毎日の学習",
                        description = "今週は5/7日達成しました",
                        target = 7,
                        current = 5,
                        type = "daily",
                        startDate = System.currentTimeMillis() - 604800000, // 1 week ago
                        endDate = System.currentTimeMillis() + 604800000 // 1 week from now
                    ),
                    LearningGoal(
                        id = "",
                        title = "新しい単語",
                        description = "今週は40/100語学びました",
                        target = 100,
                        current = 40,
                        type = "vocabulary",
                        startDate = System.currentTimeMillis() - 604800000,
                        endDate = System.currentTimeMillis() + 604800000
                    ),
                    LearningGoal(
                        id = "",
                        title = "文法のマスター",
                        description = "N5文法の3/10項目を完了しました",
                        target = 10,
                        current = 3,
                        type = "grammar",
                        startDate = System.currentTimeMillis() - 604800000,
                        endDate = System.currentTimeMillis() + 1209600000 // 2 weeks from now
                    )
                )

                // Upload learning goals
                val batch = firestore.batch()
                sampleLearningGoals.forEach { goal ->
                    val goalRef = firestore.collection("users")
                        .document(userId)
                        .collection("learningGoals")
                        .document()
                    batch.set(goalRef, goal)
                }

                batch.commit()
                    .addOnSuccessListener {
                        Log.d("Firestore", "Learning goals added for user $userId")
                        Toast.makeText(
                            context,
                            "Learning goals added successfully",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    .addOnFailureListener { e ->
                        Log.e("Firestore", "Error adding learning goals", e)
                        Toast.makeText(
                            context,
                            "Failed to add learning goals: ${e.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
            }

            // Upload study groups
            val studyGroupsBatch = firestore.batch()
            sampleStudyGroups.forEach { group ->
                val groupRef = firestore.collection("studyGroups").document()
                studyGroupsBatch.set(groupRef, group)
            }

            studyGroupsBatch.commit()
                .addOnSuccessListener {
                    Log.d("Firestore", "Study groups added successfully")
                    Toast.makeText(context, "Study groups added successfully", Toast.LENGTH_SHORT)
                        .show()
                }
                .addOnFailureListener { e ->
                    Log.e("Firestore", "Error adding study groups", e)
                    Toast.makeText(
                        context,
                        "Failed to add study groups: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }

            // Upload discussions
            val discussionsBatch = firestore.batch()
            sampleDiscussions.forEach { discussion ->
                val discussionRef = firestore.collection("discussions").document()
                discussionsBatch.set(discussionRef, discussion)
            }

            discussionsBatch.commit()
                .addOnSuccessListener {
                    Log.d("Firestore", "Discussions added successfully")
                    Toast.makeText(context, "Discussions added successfully", Toast.LENGTH_SHORT)
                        .show()
                }
                .addOnFailureListener { e ->
                    Log.e("Firestore", "Error adding discussions", e)
                    Toast.makeText(
                        context,
                        "Failed to add discussions: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }

            // Để thêm learning goals, cần có user ID
            // Ví dụ: Nếu có user hiện tại, thêm learning goals cho user đó
            val currentUser = FirebaseAuth.getInstance().currentUser
            currentUser?.uid?.let { userId ->
                createSampleLearningGoals(userId)
            }
        }

}