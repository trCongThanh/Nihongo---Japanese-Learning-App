package com.example.nihongo.User.ui.screens.homepage

// Import các component AI mới
import android.net.Uri
import android.util.Log
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation.NavController
import com.example.nihongo.Admin.ui.parseExplanation
import com.example.nihongo.User.data.models.Exercise
import com.example.nihongo.User.data.models.ExerciseType
import com.example.nihongo.User.data.models.User
import com.example.nihongo.User.data.repository.AIRepository
import com.example.nihongo.User.data.repository.ExerciseRepository
import com.example.nihongo.User.data.repository.UserRepository
import com.example.nihongo.User.ui.components.FloatingAISensei
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.ui.PlayerView
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExerciseScreen(
    navController: NavController,
    sublessonId: String,
    exerciseRepository: ExerciseRepository,
    courseId: String,
    lessonId: String,
    userEmail: String
) {
        var exercises by remember { mutableStateOf<List<Exercise>>(emptyList()) }
        var quizExercises by remember { mutableStateOf<List<Exercise>>(emptyList()) }
        var isLoading by remember { mutableStateOf(true) }
        var shouldNavigateToQuiz by remember { mutableStateOf(false) }
        val userRepository = UserRepository()
        val coroutineScope = rememberCoroutineScope()
        val aiRepository = remember { AIRepository() }
        var currentUser by remember { mutableStateOf<User?>(null) }
        // Logic tải dữ liệu giữ nguyên
        LaunchedEffect(sublessonId) {
            currentUser = userRepository.getUserByEmail(userEmail)
            isLoading = true
            Log.d("ExerciseScreen", "Loading exercises for sublessonId: $sublessonId, lessonId: $lessonId")

            try {
                exercises = exerciseRepository.getExercisesBySubLessonId(sublessonId, lessonId)
                quizExercises = exerciseRepository.getPracticeExercisesExcludingFirstSubLesson(lessonId)

                // Nếu không có VIDEO thì trigger navigation
                val hasVideo = exercises.any { it.type == ExerciseType.VIDEO }

                if (!hasVideo) {
                    shouldNavigateToQuiz = true
                } else {
                    // Đánh dấu hoàn thành
                    coroutineScope.launch {
                        val user = userRepository.getUserByEmail(userEmail)
                        user?.let {
                            userRepository.updateUserProgress(
                                userId = it.id,
                                courseId = courseId,
                                exerciseId = exercises.first { ex -> ex.type == ExerciseType.VIDEO }.id ?: "",
                                passed = true,
                                subLessonId = sublessonId
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("ExerciseScreen", "Error loading data", e)
            } finally {
                isLoading = false
            }
        }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = exercises.firstOrNull()?.title ?: "Bài tập",
                            style = MaterialTheme.typography.titleLarge,
                            color = Color.DarkGray
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = {
                            navController.previousBackStackEntry?.savedStateHandle?.set("shouldRefreshProgress", true)
                            navController.popBackStack()
                        }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.White,
                        titleContentColor = Color.DarkGray,
                        actionIconContentColor = Color.Gray
                    )
                )
            },
            containerColor = Color(0xFFF5F5F5)
        ) { innerPadding ->
            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(color = Color(0xFF4CAF50))
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "Đang tải bài tập...",
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color.Gray
                        )
                    }
                }
            } else if (shouldNavigateToQuiz) {
                LaunchedEffect(Unit) {
                    navController.currentBackStackEntry?.savedStateHandle?.set("quizList", quizExercises)
                    navController.navigate("quiz_screen/${Uri.encode(userEmail)}/$courseId/$lessonId")
                }
            } else {
                val videoExercise = exercises.find { it.type == ExerciseType.VIDEO }

                if (videoExercise != null) {
                    val explanation = if (videoExercise.explanation.isNullOrBlank()) {
                        sampleExplanation
                    } else {
                        parseExplanation(videoExercise.explanation)
                    }

                    VideoExerciseView(
                        navController = navController,
                        userEmail = userEmail,
                        courseId = courseId,
                        lessonId = lessonId,
                        title = videoExercise.title ?: "",
                        videoPath = videoExercise.videoUrl ?: "",
                        explanation = explanation,
                        quiz = quizExercises,
                        innerPadding = innerPadding
                    )
                }
            }
        }
}

@Composable
fun VideoExerciseView(
    navController: NavController,
    userEmail: String,
    courseId: String,
    lessonId: String,
    title: String,
    videoPath: String,
    explanation: List<Pair<String, String>>,
    quiz: List<Exercise>,
    innerPadding: PaddingValues
) {
    var videoStarted by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val userRepository = UserRepository()
    val aiRepository = remember { AIRepository() }
    var currentUser by remember { mutableStateOf<User?>(null) }


    val player = remember(videoPath) {
        SimpleExoPlayer.Builder(context).build().apply {
            val mediaItem = MediaItem.fromUri(Uri.parse(videoPath))
            setMediaItem(mediaItem)
            prepare()
            playWhenReady = false

            addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(state: Int) {
                    if (state == Player.STATE_READY && !videoStarted) {
                        videoStarted = true
                    }
                }
            })
        }.also { }
    }

    DisposableEffect(player) {
        onDispose {
            player.stop()
            player.release()
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(vertical = 16.dp)
    ) {
        // Video Title
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(4.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(
                                    color = Color(0xFF4CAF50).copy(alpha = 0.1f),
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayCircle,
                                contentDescription = null,
                                tint = Color(0xFF4CAF50),
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF333333)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Xem video và học các khái niệm cơ bản. AI Sensei sẽ hỗ trợ bạn giải thích từ vựng khi bạn chạm vào văn bản bên dưới.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray,
                        lineHeight = 20.sp
                    )
                }
            }
        }

        // Video Player
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(4.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(16 / 9f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.Black)
                    ) {
                        AndroidView(
                            factory = { context ->
                                PlayerView(context).apply {
                                    this.player = player
                                    setShowBuffering(PlayerView.SHOW_BUFFERING_ALWAYS)
                                    useController = true
                                    controllerAutoShow = true
                                }
                            }
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = Color(0xFF4CAF50),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Nhấn vào video để phát/tạm dừng",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                    }
                }
            }
        }

        // Lesson Content với TÍCH HỢP AI
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(4.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(Color(0xFF4CAF50).copy(alpha = 0.1f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.MenuBook,
                                contentDescription = null,
                                tint = Color(0xFF4CAF50),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Nội dung bài học (Chạm để hỏi AI)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF333333)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    ExpandableExplanationCards(
                        explanationItems = explanation,
                    )
                }
            }
        }

        // Practice Button
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(4.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                    // Header... (giữ nguyên)

                    Button(
                        onClick = {
                            player.pause()
                            player.stop()
                            Log.d("QuizLog", "Quiz Exercises: ${quiz.map { it.question }}")
                            navController.currentBackStackEntry?.savedStateHandle?.set("quizList", quiz)
                            navController.navigate("quiz_screen/$userEmail/$courseId/$lessonId")
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF4CAF50),
                            contentColor = Color.White
                        ),
                        contentPadding = PaddingValues(vertical = 16.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                            Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("BẮT ĐẦU LUYỆN TẬP", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(24.dp)) }
    }
    FloatingAISensei(
        currentUser = currentUser,
        aiRepository = aiRepository
    )
}

@Composable
fun ExpandableExplanationCards(
    explanationItems: List<Pair<String, String>>,
) {
    val expandedCardIndex = remember { mutableStateOf(-1) }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        explanationItems.forEachIndexed { index, (title, content) ->
            val isExpanded = expandedCardIndex.value == index

            Card(
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .animateContentSize(spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessLow))
                    .clickable {
                        expandedCardIndex.value = if (isExpanded) -1 else index
                    },
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF9FAFB)),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Header row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            // Index number box
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .background(
                                        Color(0xFF4CAF50).copy(alpha = 0.1f),
                                        CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "${index + 1}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF4CAF50)
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Text(
                                text = title,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1E293B)
                            )
                        }

                        Icon(
                            imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = if (isExpanded) "Thu gọn" else "Mở rộng",
                            tint = Color(0xFF4CAF50)
                        )
                    }

                    // Expanded content
                    if (isExpanded) {
                        Spacer(modifier = Modifier.height(12.dp))

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    Color.White,
                                    RoundedCornerShape(8.dp)
                                )
                                .padding(12.dp)
                        ) {
                            Text(
                                text = content,
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFF475569),
                                lineHeight = 24.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

// Các hàm tiện ích parseExplanation và sampleExplanation giữ nguyên
fun parseExplanation(raw: String): List<Pair<String, String>> {
    val parts = raw.split("➤").filter { it.isNotBlank() }
    val result = mutableListOf<Pair<String, String>>()
    var i = 0
    while (i < parts.size - 1) {
        result.add(parts[i].trim() to parts[i + 1].trim())
        i += 2
    }
    return result
}

val sampleExplanation = listOf(
    "I. Giới thiệu các loại chữ trong tiếng Nhật" to """
        Trong tiếng Nhật có 3 loại chữ:
        a. Kanji (chữ Hán): 日本
        b. Hiragana (chữ mềm): にほん
        c. Katakana (chữ cứng): 二ホン
    """.trimIndent(),
    "II. Giới thiệu bảng chữ cái Hiragana" to """
        - Bảng Hiragana gồm 46 chữ cái.
        - Hàng あ: あ(a), い(i), う(u), え(e), お(o).
    """.trimIndent()
)