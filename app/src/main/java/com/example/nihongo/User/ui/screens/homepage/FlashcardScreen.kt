package com.example.nihongo.User.ui.screens.homepage

import android.util.Log
import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.navigation.NavController
import com.example.nihongo.Admin.utils.AiCourseGenerate
import com.example.nihongo.User.data.models.User
import com.example.nihongo.User.data.repository.UserRepository
import com.example.nihongo.User.ui.components.BottomNavigationBar
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.*
import kotlinx.coroutines.tasks.await

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FlashcardScreen(navController: NavController, user_email: String, initialTab: String? = null) {
    val tabs = listOf("Hiragana", "Katakana", "Kanji", "Từ vựng")

    val initialTabIndex = when (initialTab) {
        "hiragana" -> 0
        "katakana" -> 1
        "kanji" -> 2
        "vocabulary" -> 3
        else -> 0
    }

    var selectedTab by remember { mutableStateOf(initialTabIndex) }
    val selectedItem = "vocabulary"
    var showStartSessionDialog by remember { mutableStateOf(false) }
    var studySessionConfig by remember { mutableStateOf<StudySessionConfig?>(null) }

    Scaffold(
        containerColor = Color(0xFFF5F5F5),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "Flashcard Nhật Ngữ",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold
                            )
                        )
                        Text(
                            "Học hiệu quả - Nhớ lâu hơn",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.9f)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF4CAF50),
                    titleContentColor = Color.White
                ),
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Quay lại",
                            tint = Color.White
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { showStartSessionDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Bắt đầu học",
                            tint = Color.White
                        )
                    }
                }
            )
        },
        bottomBar = {
            BottomNavigationBar(
                navController = navController,
                selectedItem = selectedItem,
                userEmail = user_email,
                onItemSelected = { selectedRoute ->
                    navController.navigate("$selectedRoute/$user_email") {
                        popUpTo(navController.graph.startDestinationId) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            ScrollableTabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.White,
                contentColor = Color(0xFF4CAF50),
                edgePadding = 16.dp,
                indicator = { tabPositions ->
                    TabRowDefaults.Indicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        height = 3.dp,
                        color = Color(0xFF4CAF50)
                    )
                }
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title) }
                    )
                }
            }

            AnimatedContent(
                targetState = selectedTab,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) { targetState ->
                when (targetState) {
                    0 -> HiraganaFlashcard()
                    1 -> KatakanaFlashcard()
                    2 -> KanjiFlashcard()
                    3 -> VocabularyFlashcard()
                    else -> Unit
                }
            }
        }

        if (showStartSessionDialog) {
            StudySetupDialog(
                onDismiss = { showStartSessionDialog = false },
                onStartSession = { config ->
                    studySessionConfig = config
                    showStartSessionDialog = false

                    if (!config.cardTypes.contains("mixed")) {
                        val firstType = config.cardTypes.firstOrNull()
                        when (firstType) {
                            "hiragana" -> selectedTab = 0
                            "katakana" -> selectedTab = 1
                            "kanji" -> selectedTab = 2
                            "vocabulary" -> selectedTab = 3
                        }
                    }
                }
            )
        }

        studySessionConfig?.let { config ->
            if (config.isAIChallenge) {
                AIChallengePracticeDialog(
                    config = config,
                    user_email = user_email,
                    onDismiss = { studySessionConfig = null }
                )
            } else {
                PracticeSessionDialog(
                    config = config,
                    user_email = user_email,
                    onDismiss = { studySessionConfig = null }
                )
            }
        }
    }
}

@Composable
fun StudySetupDialog(
    onDismiss: () -> Unit,
    onStartSession: (StudySessionConfig) -> Unit
) {
    var selectedMode by remember { mutableStateOf(0) } // 0: Flashcard, 1: AI Challenge
    var selectedCardTypes by remember { mutableStateOf(setOf<String>()) }
    var selectedCardCount by remember { mutableStateOf(10) }

    // AI Challenge settings
    var aiContent by remember { mutableStateOf("") }
    var aiQuestionCount by remember { mutableStateOf(5) }
    var aiLevel by remember { mutableStateOf("N5") }
    var aiDifficulty by remember { mutableStateOf("Dễ") }

    val isSelectionValid = if (selectedMode == 0) {
        selectedCardTypes.isNotEmpty()
    } else {
        true // AI Challenge không cần chọn loại thẻ
    }

    val cardTypeOptions = listOf(
        "hiragana" to "Hiragana",
        "katakana" to "Katakana",
        "kanji" to "Kanji",
        "vocabulary" to "Từ vựng",
        "mixed" to "Trộn tất cả"
    )

    val cardCountOptions = listOf(5, 10, 15, 20, 30, 50)
    val aiQuestionCountOptions = listOf(5, 10, 15, 20)
    val aiLevelOptions = listOf("N5", "N4", "N3", "N2", "N1")
    val aiDifficultyOptions = listOf("Dễ", "Trung bình", "Khó")

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                Text(
                    text = "Thiết lập học tập",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF388E3C),
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // Mode Selection Tabs
                TabRow(
                    selectedTabIndex = selectedMode,
                    containerColor = Color(0xFFF5F5F5),
                    contentColor = Color(0xFF4CAF50)
                ) {
                    Tab(
                        selected = selectedMode == 0,
                        onClick = { selectedMode = 0 },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = null,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Flashcard")
                            }
                        }
                    )
                    Tab(
                        selected = selectedMode == 1,
                        onClick = { selectedMode = 1 },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Star,
                                    contentDescription = null,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("AI Challenge")
                            }
                        }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Content based on selected mode
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                ) {
                    if (selectedMode == 0) {
                        // Flashcard Mode
                        Text(
                            text = "Chọn loại thẻ (có thể chọn nhiều)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        FlowRow(
                            maxItemsInEachRow = 3,
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            cardTypeOptions.forEach { (value, text) ->
                                val isSelected = selectedCardTypes.contains(value)

                                val onClick = {
                                    selectedCardTypes = when {
                                        value == "mixed" && !isSelected -> setOf("mixed")
                                        value != "mixed" && !isSelected -> {
                                            val newSelection = selectedCardTypes.toMutableSet()
                                            newSelection.add(value)
                                            newSelection.remove("mixed")
                                            newSelection
                                        }
                                        else -> {
                                            val newSelection = selectedCardTypes.toMutableSet()
                                            newSelection.remove(value)
                                            newSelection
                                        }
                                    }
                                }

                                FilterChip(
                                    selected = isSelected,
                                    onClick = onClick,
                                    label = { Text(text, fontSize = 14.sp) },
                                    leadingIcon = if (isSelected) {
                                        {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    } else null,
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = Color(0xFF81C784),
                                        selectedLabelColor = Color.White
                                    )
                                )
                            }
                        }

                        if (selectedCardTypes.isNotEmpty()) {
                            val selectedTypesText = if (selectedCardTypes.contains("mixed")) {
                                "Tất cả loại thẻ"
                            } else {
                                selectedCardTypes.joinToString(", ") { type ->
                                    cardTypeOptions.find { it.first == type }?.second ?: ""
                                }
                            }

                            Text(
                                text = "Đã chọn: $selectedTypesText",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF4CAF50),
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }

                        Text(
                            text = "Số lượng thẻ: $selectedCardCount",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(top = 24.dp, bottom = 8.dp)
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            cardCountOptions.forEach { count ->
                                OutlinedButton(
                                    onClick = { selectedCardCount = count },
                                    shape = CircleShape,
                                    border = BorderStroke(
                                        width = 1.dp,
                                        color = if (selectedCardCount == count) Color(0xFF4CAF50) else Color.LightGray
                                    ),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        containerColor = if (selectedCardCount == count) Color(0xFF4CAF50) else Color.Transparent,
                                        contentColor = if (selectedCardCount == count) Color.White else Color.DarkGray
                                    ),
                                    modifier = Modifier.size(46.dp),
                                    contentPadding = PaddingValues(0.dp)
                                ) {
                                    Text(
                                        text = count.toString(),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    } else {
                        // AI Challenge Mode
                        OutlinedTextField(
                            value = aiContent,
                            onValueChange = { aiContent = it },
                            label = { Text("Thêm Nội dung hỏi (tùy chọn) kết hợp cùng các course bạn đang học nhé !") },
                            placeholder = { Text("Để trống để tạo câu hỏi ngẫu nhiên") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp),
                            minLines = 2,
                            maxLines = 4,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF4CAF50),
                                focusedLabelColor = Color(0xFF4CAF50)
                            )
                        )

                        Text(
                            text = "Số câu hỏi: $aiQuestionCount",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            aiQuestionCountOptions.forEach { count ->
                                OutlinedButton(
                                    onClick = { aiQuestionCount = count },
                                    shape = CircleShape,
                                    border = BorderStroke(
                                        width = 1.dp,
                                        color = if (aiQuestionCount == count) Color(0xFF4CAF50) else Color.LightGray
                                    ),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        containerColor = if (aiQuestionCount == count) Color(0xFF4CAF50) else Color.Transparent,
                                        contentColor = if (aiQuestionCount == count) Color.White else Color.DarkGray
                                    ),
                                    modifier = Modifier.size(46.dp),
                                    contentPadding = PaddingValues(0.dp)
                                ) {
                                    Text(
                                        text = count.toString(),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "Trình độ JLPT: $aiLevel",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            aiLevelOptions.forEach { level ->
                                FilterChip(
                                    selected = aiLevel == level,
                                    onClick = { aiLevel = level },
                                    label = { Text(level) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = Color(0xFF81C784),
                                        selectedLabelColor = Color.White
                                    )
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "Độ khó: $aiDifficulty",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            aiDifficultyOptions.forEach { difficulty ->
                                FilterChip(
                                    selected = aiDifficulty == difficulty,
                                    onClick = { aiDifficulty = difficulty },
                                    label = { Text(difficulty) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = Color(0xFF81C784),
                                        selectedLabelColor = Color.White
                                    ),
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }

                // Buttons
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(
                        onClick = onDismiss,
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = Color.Gray
                        )
                    ) {
                        Text("Hủy")
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = {
                            if (isSelectionValid) {
                                if (selectedMode == 0) {
                                    onStartSession(
                                        StudySessionConfig(
                                            cardTypes = selectedCardTypes.toList(),
                                            cardCount = selectedCardCount,
                                            isAIChallenge = false
                                        )
                                    )
                                } else {
                                    onStartSession(
                                        StudySessionConfig(
                                            cardTypes = emptyList(),
                                            cardCount = 0,
                                            isAIChallenge = true,
                                            aiContent = aiContent.ifEmpty { "" },
                                            aiQuestionCount = aiQuestionCount,
                                            aiLevel = aiLevel,
                                            aiDifficulty = aiDifficulty
                                        )
                                    )
                                }
                            }
                        },
                        enabled = isSelectionValid,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF4CAF50)
                        ),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Bắt đầu học")
                    }
                }
            }
        }
    }
}

data class StudySessionConfig(
    val cardTypes: List<String>,
    val cardCount: Int,
    val isAIChallenge: Boolean = false,
    val aiContent: String = "",
    val aiQuestionCount: Int = 5,
    val aiLevel: String = "N5",
    val aiDifficulty: String = "Dễ"
)

data class AIQuestion(
    val question: String,
    val correctAnswer: String,
    val options: List<String>,
    val explanation: String
)

@Composable
fun AIChallengePracticeDialog(
    config: StudySessionConfig,
    user_email: String,
    onDismiss: () -> Unit
) {
    var questions by remember { mutableStateOf<List<AIQuestion>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var loadError by remember { mutableStateOf<String?>(null) }
    var currentQuestionIndex by remember { mutableStateOf(0) }
    var selectedAnswer by remember { mutableStateOf<String?>(null) }
    var showExplanation by remember { mutableStateOf(false) }
    var score by remember { mutableStateOf(0) }
    var correctAnswers by remember { mutableStateOf(0) }
    var isGameComplete by remember { mutableStateOf(false) }

    val userRepository = remember { UserRepository() }
    val context = LocalContext.current
    var currentUser by remember { mutableStateOf<User?>(null) }

    LaunchedEffect(user_email) {
        if (user_email.isNotEmpty()) {
            try {
                currentUser = userRepository.getUserByEmail(user_email)
            } catch (e: Exception) {
                Log.e("AIChallenge", "Error loading user", e)
            }
        }
    }

    LaunchedEffect(config) {
        isLoading = true
        try {
            val userRepository = UserRepository()
            Log.d("AIChallenge", "Getting user by email: $user_email")

            val current_user = userRepository.getUserByEmail(user_email)
            Log.d("AIChallenge", "Current user: ${current_user?.id}")

            val userProgressList = userRepository.getAllUserProgress(current_user?.id ?: "")
            Log.d("AIChallenge", "User progress count: ${userProgressList.size}")

            // Lấy tất cả courseTitle và nối thành chuỗi "title1,title2,..."
            val TitleCoursesString = userProgressList
                .map { it.courseTitle }
                .joinToString(separator = ",")

            Log.d("AIChallenge", "TitleCoursesString: $TitleCoursesString")

            val promptContent = """
            Các khóa học mà người học đã hoàn thành: $TitleCoursesString
            
            ${config.aiContent}
            """.trimIndent()

            Log.d("AIChallenge", "Prompt content: $promptContent")

            val response = AiCourseGenerate.generateAIChallenge(
                content = promptContent,
                number_of_question = config.aiQuestionCount,
                levelJLPT = config.aiLevel,
                mode = config.aiDifficulty
            )

            Log.d("AIChallenge", "AI response: $response")

            if (response != null) {
                questions = parseAIResponse(response)
                Log.d("AIChallenge", "Parsed questions count: ${questions.size}")

                if (questions.isEmpty()) {
                    loadError = "Không thể tạo câu hỏi. Vui lòng thử lại."
                }
            } else {
                loadError = "Lỗi kết nối. Vui lòng kiểm tra mạng và thử lại."
            }

        } catch (e: Exception) {
            loadError = "Đã xảy ra lỗi: ${e.message}"
            Log.e("AIChallenge", "Error generating questions", e)
        } finally {
            isLoading = false
            Log.d("AIChallenge", "Loading finished")
        }
    }

    fun checkAnswer() {
        if (selectedAnswer != null && currentQuestionIndex < questions.size) {
            val currentQuestion = questions[currentQuestionIndex]
            if (selectedAnswer == currentQuestion.correctAnswer) {
                score += 10
                correctAnswers++
            }
            showExplanation = true
        }
    }

    fun nextQuestion() {
        if (currentQuestionIndex < questions.size - 1) {
            currentQuestionIndex++
            selectedAnswer = null
            showExplanation = false
        } else {
            isGameComplete = true
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "AI Challenge",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF4CAF50)
                        )
                        Text(
                            text = "JLPT ${config.aiLevel} - ${config.aiDifficulty}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.Gray
                        )
                    }

                    Text(
                        text = "Điểm: $score",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF4CAF50)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                when {
                    isLoading -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator(color = Color(0xFF4CAF50))
                                Spacer(modifier = Modifier.height(16.dp))
                                Text("Đang tạo câu hỏi từ AI...")
                            }
                        }
                    }
                    loadError != null -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = null,
                                    tint = Color.Red,
                                    modifier = Modifier.size(48.dp)
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = loadError!!,
                                    textAlign = TextAlign.Center,
                                    color = Color.Red
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Button(
                                    onClick = onDismiss,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFF4CAF50)
                                    )
                                ) {
                                    Text("Đóng")
                                }
                            }
                        }
                    }
                    isGameComplete -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.padding(vertical = 32.dp)
                            ) {
                                Icon(
                                    imageVector = if (correctAnswers >= questions.size / 2)
                                        Icons.Default.EmojiEvents else Icons.Default.Favorite,
                                    contentDescription = null,
                                    tint = if (correctAnswers >= questions.size / 2)
                                        Color(0xFFFFD700) else Color(0xFF4CAF50),
                                    modifier = Modifier.size(72.dp)
                                )

                                Spacer(modifier = Modifier.height(16.dp))

                                Text(
                                    text = "Hoàn thành!",
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF4CAF50)
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                Text(
                                    text = "Điểm của bạn: $score",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Medium
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                Text(
                                    text = "Trả lời đúng: $correctAnswers/${questions.size}",
                                    style = MaterialTheme.typography.titleMedium
                                )

                                Spacer(modifier = Modifier.height(24.dp))

                                Button(
                                    onClick = {
                                        if (score > 0 && currentUser != null) {
                                            GlobalScope.launch(Dispatchers.IO) {
                                                try {
                                                    val repo = UserRepository()
                                                    val success = repo.addActivityPoints(currentUser!!.id, score)

                                                    if (success) {
                                                        withContext(Dispatchers.Main) {
                                                            Toast.makeText(
                                                                context,
                                                                "Đã thêm $score điểm năng động!",
                                                                Toast.LENGTH_SHORT
                                                            ).show()
                                                        }
                                                    }
                                                } catch (e: Exception) {
                                                    Log.e("AIChallenge", "Error adding points", e)
                                                }
                                            }
                                        }
                                        onDismiss()
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFF4CAF50)
                                    ),
                                    shape = RoundedCornerShape(24.dp),
                                    modifier = Modifier.width(200.dp)
                                ) {
                                    Text("Kết thúc")
                                }
                            }
                        }
                    }
                    else -> {
                        // Progress
                        LinearProgressIndicator(
                            progress = (currentQuestionIndex + 1).toFloat() / questions.size.toFloat(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = Color(0xFF4CAF50),
                            trackColor = Color(0xFFE0E0E0)
                        )

                        Text(
                            text = "Câu hỏi ${currentQuestionIndex + 1}/${questions.size}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.Gray,
                            modifier = Modifier.padding(top = 8.dp)
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .verticalScroll(rememberScrollState())
                        ) {
                            val currentQuestion = questions[currentQuestionIndex]

                            // Question
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 16.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = Color(0xFFF1F8E9)
                                ),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                            ) {
                                Text(
                                    text = currentQuestion.question,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.padding(16.dp)
                                )
                            }

                            // Options
                            currentQuestion.options.forEachIndexed { index, option ->
                                val isSelected = selectedAnswer == option
                                val isCorrect = option == currentQuestion.correctAnswer
                                val showResult = showExplanation

                                Card(
                                    onClick = {
                                        if (!showExplanation) {
                                            selectedAnswer = option
                                        }
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = when {
                                            showResult && isCorrect -> Color(0xFFC8E6C9)
                                            showResult && isSelected && !isCorrect -> Color(0xFFFFCDD2)
                                            isSelected -> Color(0xFFE3F2FD)
                                            else -> Color.White
                                        }
                                    ),
                                    border = BorderStroke(
                                        width = 2.dp,
                                        color = when {
                                            showResult && isCorrect -> Color(0xFF4CAF50)
                                            showResult && isSelected && !isCorrect -> Color(0xFFE57373)
                                            isSelected -> Color(0xFF2196F3)
                                            else -> Color(0xFFE0E0E0)
                                        }
                                    ),
                                    elevation = CardDefaults.cardElevation(
                                        defaultElevation = if (isSelected) 4.dp else 1.dp
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(32.dp)
                                                .background(
                                                    color = when {
                                                        showResult && isCorrect -> Color(0xFF4CAF50)
                                                        showResult && isSelected && !isCorrect -> Color(0xFFE57373)
                                                        isSelected -> Color(0xFF2196F3)
                                                        else -> Color(0xFFE0E0E0)
                                                    },
                                                    shape = CircleShape
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (showResult && isCorrect) {
                                                Icon(
                                                    imageVector = Icons.Default.Check,
                                                    contentDescription = null,
                                                    tint = Color.White,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            } else if (showResult && isSelected && !isCorrect) {
                                                Icon(
                                                    imageVector = Icons.Default.Close,
                                                    contentDescription = null,
                                                    tint = Color.White,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            } else {
                                                Text(
                                                    text = ('A' + index).toString(),
                                                    color = if (isSelected) Color.White else Color.Gray,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.width(12.dp))

                                        Text(
                                            text = option,
                                            style = MaterialTheme.typography.bodyLarge,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }
                            }

                            // Explanation
                            if (showExplanation) {
                                Spacer(modifier = Modifier.height(16.dp))

                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(
                                        containerColor = Color(0xFFFFF9C4)
                                    )
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.Info,
                                                contentDescription = null,
                                                tint = Color(0xFFF57C00),
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = "Giải thích:",
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFFF57C00)
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = currentQuestion.explanation,
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                    }
                                }
                            }
                        }

                        // Action Buttons
                        Spacer(modifier = Modifier.height(16.dp))

                        if (!showExplanation) {
                            Button(
                                onClick = { checkAnswer() },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp),
                                enabled = selectedAnswer != null,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF4CAF50)
                                ),
                                shape = RoundedCornerShape(24.dp)
                            ) {
                                Text("Kiểm tra")
                            }
                        } else {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = { isGameComplete = true },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp),
                                    border = BorderStroke(1.dp, Color(0xFFBDBDBD)),
                                    shape = RoundedCornerShape(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = null
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Dừng")
                                }

                                Button(
                                    onClick = { nextQuestion() },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFF4CAF50)
                                    ),
                                    shape = RoundedCornerShape(24.dp)
                                ) {
                                    Text(if (currentQuestionIndex < questions.size - 1) "Tiếp tục" else "Hoàn thành")
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(
                                        imageVector = Icons.Default.ArrowForward,
                                        contentDescription = null
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

fun parseAIResponse(response: String): List<AIQuestion> {
    val questions = mutableListOf<AIQuestion>()

    try {
        val textContent = extractTextFromGeminiResponse(response)
        Log.d("AIChallenge", "==== RAW RESPONSE START ====")
        Log.d("AIChallenge", textContent)
        Log.d("AIChallenge", "==== RAW RESPONSE END ====")

        if (textContent.isEmpty()) {
            Log.e("AIChallenge", "Empty response from AI")
            return questions
        }

        // Method 1: Try parsing with numbered markers
        var parsed = parseWithNumberedMarkers(textContent)

        // Method 2: If failed, try without numbers
        if (parsed.isEmpty()) {
            Log.d("AIChallenge", "Numbered markers failed, trying unnumbered...")
            parsed = parseWithUnnumberedMarkers(textContent)
        }

        // Method 3: If still failed, try simple split
        if (parsed.isEmpty()) {
            Log.d("AIChallenge", "Unnumbered failed, trying simple split...")
            parsed = parseWithSimpleSplit(textContent)
        }

        questions.addAll(parsed)
    } catch (e: Exception) {
        Log.e("AIChallenge", "Error parsing AI response: ${e.message}", e)
    }

    Log.d("AIChallenge", "Total questions parsed: ${questions.size}")
    return questions
}

// ===== PARSING METHODS =====

fun parseWithNumberedMarkers(text: String): List<AIQuestion> {
    val questions = mutableListOf<AIQuestion>()

    // Split by &Question pattern
    val blocks = text.split(Regex("&Question\\d+&")).filter { it.isNotBlank() }

    Log.d("AIChallenge", "Found ${blocks.size} question blocks with numbered markers")

    blocks.forEachIndexed { index, block ->
        try {
            val questionNum = index + 1

            // Extract question text (everything before &Answer marker)
            val answerRegex = Regex("&Answer\\d+&")
            val match = answerRegex.find(block)

            val questionText = if (match != null) {
                block.substring(0, match.range.first).trim()
            } else {
                block.trim()
            }


            // Extract correct answer
            val answerMatch = Regex("&Answer\\d+&\\s*(.+?)\\s*&Option").find(block)
            val correctAnswer = answerMatch?.groupValues?.get(1)?.trim() ?: ""

            // Extract all 4 options
            val options = mutableListOf<String>()
            for (choiceNum in 1..4) {
                val optionMatch = Regex("&Option\\d+Choice${choiceNum}&\\s*(.+?)\\s*(?=&|$)").find(block)
                val option = optionMatch?.groupValues?.get(1)?.trim()
                if (!option.isNullOrEmpty()) {
                    options.add(option)
                }
            }

            // Extract explanation
            val explanationMatch = Regex("&Explanation\\d+&\\s*(.+?)\\s*(?=&Question|$)", RegexOption.DOT_MATCHES_ALL).find(block)
            val explanation = explanationMatch?.groupValues?.get(1)?.trim()?.ifEmpty { "Không có giải thích" }
                ?: "Không có giải thích"

            Log.d("AIChallenge", "Q$questionNum: question='${questionText.take(30)}...', answer='$correctAnswer', options=${options.size}")

            if (questionText.isNotEmpty() && correctAnswer.isNotEmpty() && options.size >= 2) {
                // Ensure correct answer is in options list
                if (!options.contains(correctAnswer)) {
                    if (options.size >= 4) {
                        options[options.size - 1] = correctAnswer
                    } else {
                        options.add(correctAnswer)
                    }
                }

                // Shuffle to randomize position
                val shuffledOptions = options.shuffled()

                questions.add(
                    AIQuestion(
                        question = questionText,
                        correctAnswer = correctAnswer,
                        options = shuffledOptions.take(4),
                        explanation = explanation
                    )
                )

                Log.d("AIChallenge", "Successfully parsed question $questionNum")
            } else {
                Log.e("AIChallenge", "Invalid Q$questionNum: text empty=${questionText.isEmpty()}, answer empty=${correctAnswer.isEmpty()}, opts=${options.size}")
            }
        } catch (e: Exception) {
            Log.e("AIChallenge", "Error parsing question ${index + 1}: ${e.message}", e)
        }
    }

    return questions
}

fun parseWithUnnumberedMarkers(text: String): List<AIQuestion> {
    val questions = mutableListOf<AIQuestion>()
    val blocks = text.split("&Question&").filter { it.isNotBlank() }

    Log.d("AIChallenge", "Found ${blocks.size} blocks with unnumbered markers")

    for ((index, block) in blocks.withIndex()) {
        try {
            val questionText = block.substringBefore("&Answer&").trim()

            val answerBlock = block.substringAfter("&Answer&", "")
            val correctAnswer = answerBlock.substringBefore("&Option").trim()

            val options = mutableListOf<String>()

            // Try to extract options
            val optionPattern = Regex("&OptionChoice(\\d+)&")
            val optionMatches = optionPattern.findAll(block).toList()

            for (i in optionMatches.indices) {
                val startIdx = optionMatches[i].range.last + 1
                val endIdx = if (i < optionMatches.size - 1) {
                    optionMatches[i + 1].range.first
                } else {
                    block.indexOf("&Explanation&").takeIf { it > 0 } ?: block.length
                }

                val option = block.substring(startIdx, endIdx).trim()
                if (option.isNotEmpty()) {
                    options.add(option)
                }
            }

            val explanation = block.substringAfter("&Explanation&", "")
                .trim()
                .ifEmpty { "Không có giải thích" }

            if (questionText.isNotEmpty() && correctAnswer.isNotEmpty() && options.size >= 2) {
                if (!options.contains(correctAnswer)) {
                    options.add(correctAnswer)
                    options.shuffle()
                }

                questions.add(
                    AIQuestion(
                        question = questionText,
                        correctAnswer = correctAnswer,
                        options = options.take(4),
                        explanation = explanation
                    )
                )
            }
        } catch (e: Exception) {
            Log.e("AIChallenge", "Error in parseWithUnnumberedMarkers: ${e.message}", e)
        }
    }

    return questions
}

fun parseWithSimpleSplit(text: String): List<AIQuestion> {
    val questions = mutableListOf<AIQuestion>()

    // Try splitting by any Question marker
    val blocks = text.split(Regex("&Question\\d*&")).filter { it.isNotBlank() }

    Log.d("AIChallenge", "Found ${blocks.size} blocks with simple split")

    for (block in blocks) {
        try {
            // Find all content before first &Answer
            val answerRegex = Regex("&Answer\\d+&")
            val match = answerRegex.find(block)

            val questionText = if (match != null) {
                block.substring(0, match.range.first).trim()
            } else {
                ""
            }


            // Find answer
            val answerMatch = Regex("&Answer\\d*&\\s*([^&]+)").find(block)
            val correctAnswer = answerMatch?.groupValues?.get(1)?.trim() ?: ""

            // Find all options
            val optionMatches = Regex("&Option\\d*Choice\\d*&\\s*([^&]+)").findAll(block)
            val options = optionMatches.map { it.groupValues[1].trim() }.toList()

            // Find explanation
            val explanationMatch = Regex("&Explanation\\d*&\\s*(.+?)(?=&|$)", RegexOption.DOT_MATCHES_ALL).find(block)
            val explanation = explanationMatch?.groupValues?.get(1)?.trim()?.ifEmpty { "Không có giải thích" }
                ?: "Không có giải thích"

            if (questionText.isNotEmpty() && correctAnswer.isNotEmpty() && options.size >= 2) {
                val finalOptions = options.toMutableList()
                if (!finalOptions.contains(correctAnswer)) {
                    finalOptions.add(correctAnswer)
                    finalOptions.shuffle()
                }

                questions.add(
                    AIQuestion(
                        question = questionText,
                        correctAnswer = correctAnswer,
                        options = finalOptions.take(4),
                        explanation = explanation
                    )
                )
            }
        } catch (e: Exception) {
            Log.e("AIChallenge", "Error in parseWithSimpleSplit: ${e.message}", e)
        }
    }

    return questions
}


fun extractTextFromGeminiResponse(response: String): String {
    return try {
        val jsonResponse = org.json.JSONObject(response)
        val candidates = jsonResponse.getJSONArray("candidates")
        if (candidates.length() > 0) {
            val content = candidates.getJSONObject(0).getJSONObject("content")
            val parts = content.getJSONArray("parts")
            if (parts.length() > 0) {
                parts.getJSONObject(0).getString("text")
            } else ""
        } else ""
    } catch (e: Exception) {
        Log.e("AIChallenge", "Error extracting text from response", e)
        ""
    }
}

@Composable
fun PracticeSessionDialog(
    config: StudySessionConfig,
    user_email: String,
    onDismiss: () -> Unit
) {
    var flashcards by remember { mutableStateOf<List<Pair<String, String>>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var currentCardIndex by remember { mutableStateOf(0) }
    var isCardFlipped by remember { mutableStateOf(false) }
    var userAnswer by remember { mutableStateOf("") }
    var remainingAttempts by remember { mutableStateOf(2) }
    var score by remember { mutableStateOf(0) }
    var isGameComplete by remember { mutableStateOf(false) }
    var correctAnswers by remember { mutableStateOf(0) }
    val userRepository = remember { UserRepository() }
    val context = LocalContext.current

    var currentUser by remember { mutableStateOf<User?>(null) }

    LaunchedEffect(user_email) {
        if (user_email.isNotEmpty()) {
            try {
                currentUser = userRepository.getUserByEmail(user_email)
            } catch (e: Exception) {
                Log.e("FlashcardScreen", "Error loading user data", e)
            }
        }
    }

    val sessionTitle = when {
        config.cardTypes.contains("mixed") -> "Luyện tập trộn"
        config.cardTypes.size == 1 -> when(config.cardTypes.first()) {
            "hiragana" -> "Luyện tập Hiragana"
            "katakana" -> "Luyện tập Katakana"
            "kanji" -> "Luyện tập Kanji"
            "vocabulary" -> "Luyện tập Từ vựng"
            else -> "Luyện tập"
        }
        else -> "Flashcard Game"
    }

    LaunchedEffect(config) {
        if (config.cardTypes.contains("mixed")) {
            val hiragana = getFlashcardsByExerciseId("hiragana_basic")
            val katakana = getFlashcardsByExerciseId("katakana_basic")
            val kanji = getFlashcardsByExerciseId("kanji_n5")
            val vocabulary = getFlashcardsByExerciseId("vocabulary_n5")
            flashcards = (hiragana + katakana + kanji + vocabulary).shuffled()
        } else {
            val selectedFlashcards = mutableListOf<Pair<String, String>>()

            if (config.cardTypes.contains("hiragana")) {
                selectedFlashcards.addAll(getFlashcardsByExerciseId("hiragana_basic"))
            }
            if (config.cardTypes.contains("katakana")) {
                selectedFlashcards.addAll(getFlashcardsByExerciseId("katakana_basic"))
            }
            if (config.cardTypes.contains("kanji")) {
                selectedFlashcards.addAll(getFlashcardsByExerciseId("kanji_n5"))
            }
            if (config.cardTypes.contains("vocabulary")) {
                selectedFlashcards.addAll(getFlashcardsByExerciseId("vocabulary_n5"))
            }

            flashcards = selectedFlashcards.shuffled()
        }

        if (flashcards.size > config.cardCount) {
            flashcards = flashcards.take(config.cardCount).shuffled()
        }

        isLoading = false
    }

    fun checkAnswer() {
        if (currentCardIndex < flashcards.size) {
            val currentCard = flashcards[currentCardIndex]
            val correctAnswer = currentCard.second.trim().lowercase()
            val userInputAnswer = userAnswer.trim().lowercase()

            if (correctAnswer == userInputAnswer) {
                score += 10
                correctAnswers++

                if (currentCardIndex < flashcards.size - 1) {
                    currentCardIndex++
                    isCardFlipped = false
                    userAnswer = ""
                    remainingAttempts = 2
                } else {
                    isGameComplete = true
                }
            } else {
                remainingAttempts--

                if (remainingAttempts <= 0) {
                    isCardFlipped = true
                }
            }
        }
    }

    fun nextCard() {
        if (currentCardIndex < flashcards.size - 1) {
            currentCardIndex++
            isCardFlipped = false
            userAnswer = ""
            remainingAttempts = 2
        } else {
            isGameComplete = true
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White, RoundedCornerShape(16.dp))
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = sessionTitle,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = "Điểm: $score",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF4CAF50)
                    )
                }

                if (!isGameComplete) {
                    LinearProgressIndicator(
                        progress = (currentCardIndex + 1).toFloat() / flashcards.size.toFloat(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp)
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = Color(0xFF4CAF50),
                        trackColor = Color(0xFFE0E0E0)
                    )

                    Text(
                        text = "Thẻ ${currentCardIndex + 1}/${flashcards.size}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray
                    )
                }

                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.padding(32.dp),
                        color = Color(0xFF4CAF50)
                    )
                } else if (flashcards.isEmpty()) {
                    Text("Không tìm thấy thẻ flashcard cho loại này")
                } else if (isGameComplete) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = if (correctAnswers >= flashcards.size / 2)
                                Icons.Default.EmojiEvents else Icons.Default.Favorite,
                            contentDescription = null,
                            tint = if (correctAnswers >= flashcards.size / 2)
                                Color(0xFFFFD700) else Color(0xFF4CAF50),
                            modifier = Modifier.size(72.dp)
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "Hoàn thành!",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF4CAF50)
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Điểm của bạn: $score",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Medium
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Trả lời đúng: $correctAnswers/${flashcards.size}",
                            style = MaterialTheme.typography.titleMedium
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        Button(
                            onClick = {
                                if (score > 0 && currentUser != null) {
                                    GlobalScope.launch(Dispatchers.IO) {
                                        try {
                                            val repo = UserRepository()
                                            val success = repo.addActivityPoints(currentUser!!.id, score)

                                            if (success) {
                                                withContext(Dispatchers.Main) {
                                                    Toast.makeText(
                                                        context,
                                                        "Đã thêm $score điểm năng động!",
                                                        Toast.LENGTH_SHORT
                                                    ).show()
                                                }
                                            }
                                        } catch (e: Exception) {
                                            Log.e("FlashcardScreen", "Error adding points", e)
                                        }
                                    }
                                }
                                onDismiss()
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF4CAF50)
                            ),
                            shape = RoundedCornerShape(24.dp),
                            modifier = Modifier.width(200.dp)
                        ) {
                            Text("Kết thúc")
                        }
                    }
                } else {
                    val currentCard = flashcards[currentCardIndex]

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(250.dp)
                            .padding(vertical = 24.dp)
                    ) {
                        FlippableCard(
                            frontContent = {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(
                                            color = Color(0xFFE8F5E9),
                                            shape = RoundedCornerShape(16.dp)
                                        )
                                        .border(
                                            width = 1.dp,
                                            color = Color(0xFFBDBDBD),
                                            shape = RoundedCornerShape(16.dp)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = currentCard.first,
                                        style = MaterialTheme.typography.displaySmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            },
                            backContent = {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(
                                            color = Color(0xFFF1F8E9),
                                            shape = RoundedCornerShape(16.dp)
                                        )
                                        .border(
                                            width = 1.dp,
                                            color = Color(0xFFBDBDBD),
                                            shape = RoundedCornerShape(16.dp)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(
                                            text = "Đáp án:",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = Color.Gray
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = currentCard.second,
                                            style = MaterialTheme.typography.headlineMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            },
                            isFlipped = isCardFlipped
                        )
                    }

                    if (!isCardFlipped) {
                        Row(
                            modifier = Modifier.padding(bottom = 8.dp),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "Lượt còn lại: ",
                                style = MaterialTheme.typography.bodyMedium
                            )

                            repeat(remainingAttempts) {
                                Icon(
                                    imageVector = Icons.Default.FavoriteBorder,
                                    contentDescription = null,
                                    tint = Color(0xFFE57373),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }

                    if (!isCardFlipped) {
                        OutlinedTextField(
                            value = userAnswer,
                            onValueChange = { userAnswer = it },
                            label = { Text("Nhập đáp án") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions.Default.copy(
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(
                                onDone = { checkAnswer() }
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF4CAF50),
                                focusedLabelColor = Color(0xFF4CAF50)
                            )
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Button(
                            onClick = { checkAnswer() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF4CAF50)
                            ),
                            shape = RoundedCornerShape(24.dp)
                        ) {
                            Text("Kiểm tra")
                        }
                    } else {
                        Button(
                            onClick = { nextCard() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF4CAF50)
                            ),
                            shape = RoundedCornerShape(24.dp)
                        ) {
                            Text("Tiếp tục")
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        OutlinedButton(
                            onClick = {
                                if (!isCardFlipped) {
                                    isCardFlipped = true
                                } else {
                                    nextCard()
                                }
                            },
                            border = BorderStroke(1.dp, Color(0xFFBDBDBD)),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = Color(0xFF757575)
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.SkipNext,
                                contentDescription = "Skip"
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Bỏ qua")
                        }

                        OutlinedButton(
                            onClick = {
                                isGameComplete = true
                            },
                            border = BorderStroke(1.dp, Color(0xFFBDBDBD)),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = Color(0xFF757575)
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "End"
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Dừng")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FlippableCard(
    frontContent: @Composable () -> Unit,
    backContent: @Composable () -> Unit,
    isFlipped: Boolean
) {
    val rotation = animateFloatAsState(
        targetValue = if (isFlipped) 180f else 0f,
        animationSpec = tween(
            durationMillis = 400,
            easing = FastOutSlowInEasing
        ),
        label = "rotation"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer {
                rotationY = rotation.value
                cameraDistance = 12f * density
            }
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    alpha = if (rotation.value > 90f) 1f else 0f
                    rotationY = 180f
                }
        ) {
            backContent()
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    alpha = if (rotation.value < 90f) 1f else 0f
                }
        ) {
            frontContent()
        }
    }
}

@Composable
fun HiraganaFlashcard() {
    var flashcards by remember { mutableStateOf<List<Pair<String, String>>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        flashcards = getFlashcardsByExerciseId("hiragana_basic")
        isLoading = false
    }

    if (isLoading) {
        Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = Color(0xFF4CAF50))
        }
    } else if (flashcards.isEmpty()) {
        Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
            Text("Không tìm thấy thẻ Hiragana")
        }
    } else {
        ImprovedFlashcardGrid(flashcards)
    }
}

@Composable
fun KatakanaFlashcard() {
    var flashcards by remember { mutableStateOf<List<Pair<String, String>>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        flashcards = getFlashcardsByExerciseId("katakana_basic")
        isLoading = false
    }

    if (isLoading) {
        Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = Color(0xFF4CAF50))
        }
    } else if (flashcards.isEmpty()) {
        Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
            Text("Không tìm thấy thẻ Katakana")
        }
    } else {
        ImprovedFlashcardGrid(flashcards)
    }
}

@Composable
fun KanjiFlashcard() {
    var flashcards by remember { mutableStateOf<List<Pair<String, String>>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        flashcards = getFlashcardsByExerciseId("kanji_n5")
        isLoading = false
    }

    if (isLoading) {
        Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = Color(0xFF4CAF50))
        }
    } else if (flashcards.isEmpty()) {
        Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
            Text("Không tìm thấy thẻ Kanji")
        }
    } else {
        ImprovedFlashcardGrid(flashcards)
    }
}

@Composable
fun VocabularyFlashcard() {
    var flashcards by remember { mutableStateOf<List<Pair<String, String>>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        flashcards = getFlashcardsByExerciseId("vocabulary_n5")
        isLoading = false
    }

    if (isLoading) {
        Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = Color(0xFF4CAF50))
        }
    } else if (flashcards.isEmpty()) {
        Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
            Text("Không tìm thấy thẻ Từ vựng")
        }
    } else {
        ImprovedFlashcardGrid(flashcards)
    }
}

@Composable
fun ImprovedFlashcardGrid(flashcards: List<Pair<String, String>>) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        contentPadding = PaddingValues(8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        // Bỏ modifier height cố định
        modifier = Modifier.fillMaxSize()
    ) {
        items(flashcards.size) { index ->
            FlashcardGridItem(flashcard = flashcards[index])
        }
    }
}

@Composable
fun FlashcardGridItem(flashcard: Pair<String, String>) {
    var isFlipped by remember { mutableStateOf(false) }

    Card(
        onClick = { isFlipped = !isFlipped },
        modifier = Modifier
            .aspectRatio(1f)
            .shadow(elevation = 2.dp, shape = RoundedCornerShape(8.dp)),
        colors = CardDefaults.cardColors(
            containerColor = if (isFlipped) Color(0xFFF1F8E9) else Color(0xFFE8F5E9)
        )
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            if (isFlipped) {
                // Hiển thị định nghĩa
                Text(
                    text = flashcard.second,
                    textAlign = TextAlign.Center,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(8.dp)
                )
            } else {
                // Hiển thị thuật ngữ
                Text(
                    text = flashcard.first,
                    textAlign = TextAlign.Center,
                    fontSize = when {
                        flashcard.first.length <= 2 -> 24.sp
                        flashcard.first.length <= 4 -> 20.sp
                        else -> 16.sp
                    },
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(8.dp)
                )
            }
        }
    }
}

val hiraganaOrder = listOf(
    "あ", "い", "う", "え", "お",
    "か", "き", "く", "け", "こ",
    "さ", "し", "す", "せ", "そ",
    "た", "ち", "つ", "て", "と",
    "な", "に", "ぬ", "ね", "の",
    "は", "ひ", "ふ", "へ", "ほ",
    "ま", "み", "む", "め", "も",
    "や",       "ゆ",       "よ",
    "ら", "り", "る", "れ", "ろ",
    "わ",                   "を",
    "ん"
)

val katakanaOrder = listOf(
    "ア", "イ", "ウ", "エ", "オ",
    "カ", "キ", "ク", "ケ", "コ",
    "サ", "シ", "ス", "セ", "ソ",
    "タ", "チ", "ツ", "テ", "ト",
    "ナ", "ニ", "ヌ", "ネ", "ノ",
    "ハ", "ヒ", "フ", "ヘ", "ホ",
    "マ", "ミ", "ム", "メ", "モ",
    "ヤ",       "ユ",       "ヨ",
    "ラ", "リ", "ル", "レ", "ロ",
    "ワ",                   "ヲ",
    "ン"
)

suspend fun getFlashcardsByExerciseId(exerciseId: String): List<Pair<String, String>> {
    val firestore = FirebaseFirestore.getInstance()
    return try {
        val snapshot = firestore.collection("flashcards")
            .whereEqualTo("exerciseId", exerciseId)
            .get()
            .await()

        val rawList = snapshot.documents.mapNotNull { doc ->
            val term = doc.getString("term")
            val definition = doc.getString("definition")
            if (term != null && definition != null) term to definition else null
        }

        // Sắp xếp Hiragana và Katakana
        when (exerciseId) {
            "hiragana_basic" -> {
                hiraganaOrder.mapNotNull { orderChar -> rawList.find { it.first == orderChar } }
            }
            "katakana_basic" -> {
                katakanaOrder.mapNotNull { orderChar -> rawList.find { it.first == orderChar } }
            }
            else -> rawList // nếu không phải hiragana hay katakana, trả lại danh sách không sắp xếp
        }

    } catch (e: Exception) {
        e.printStackTrace()
        emptyList()
    }
}
