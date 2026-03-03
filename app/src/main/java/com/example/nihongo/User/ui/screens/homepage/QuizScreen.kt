package com.example.nihongo.User.ui.screens.homepage


import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseOutBack
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.nihongo.User.data.models.Exercise
import com.example.nihongo.User.data.models.User
import com.example.nihongo.User.data.models.UserProgress
import com.example.nihongo.User.data.repository.AIRepository
import com.example.nihongo.User.data.repository.UserRepository
import com.example.nihongo.User.ui.components.FloatingAISensei
import com.google.accompanist.flowlayout.FlowRow
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuizScreen(quizExercises: List<Exercise>, userEmail: String, courseId: String, lessonId: String, navController: NavController) {
    // Add logging at the start of the function
    Log.d("QuizScreen", "Starting QuizScreen with lessonId: $lessonId, courseId: $courseId")
    Log.d("QuizScreen", "Received ${quizExercises.size} quiz exercises")
    
    // Log each exercise to check if exercises for lessons 13 and 14 are included
    quizExercises.forEachIndexed { index, exercise ->
        Log.d("QuizScreen", "Exercise $index: id=${exercise.id}, subLessonId=${exercise.subLessonId}, " +
                "type=${exercise.type}, question=${exercise.question}")
    }
    
    var currentIndex by remember { mutableStateOf(0) }
    val currentExercise = quizExercises.getOrNull(currentIndex)
    val correctAnswer = currentExercise?.answer ?: ""
    
    // Log the current exercise
    LaunchedEffect(currentIndex) {
        Log.d("QuizScreen", "Current exercise index: $currentIndex")
        Log.d("QuizScreen", "Current exercise: ${currentExercise?.id}, subLessonId: ${currentExercise?.subLessonId}")
        Log.d("QuizScreen", "Correct answer: $correctAnswer")
    }

    var selectedWords by remember { mutableStateOf(listOf<String>()) }
    var result by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    var shakeOffset by remember { mutableStateOf(0f) }

    // Confetti state
    var showConfetti by remember { mutableStateOf(false) }

    // Track if the answer has been checked
    var isAnswerChecked by remember { mutableStateOf(false) }
    val userProgressRepository = UserRepository()
    val aiRepository = remember { AIRepository() }
    val userRepository = remember { UserRepository() }
    var user by remember { mutableStateOf<User?>(null) }
    var userProgressList by remember { mutableStateOf<List<UserProgress>>(emptyList()) }

    // Progress tracking
    val progress = (currentIndex + 1).toFloat() / quizExercises.size.toFloat()

    var wrongAnswerCount by remember { mutableStateOf(0) }

    // Load user profile when composable is launched
    LaunchedEffect(userEmail) {
        scope.launch {
            Log.d("QuizScreen", "Loading user profile for email: $userEmail")
            user = userProgressRepository.getUserByEmail(userEmail)
            Log.d("QuizScreen", "User loaded: ${user?.id}, ${user?.username}")
            
            userProgressList = user?.let {
                val progress = userProgressRepository.getUserProgressForCourse(it.id, courseId)
                Log.d("QuizScreen", "User progress loaded for course $courseId: ${progress != null}")
                listOfNotNull(progress)
            } ?: emptyList()
            
            Log.d("QuizScreen", "User progress list size: ${userProgressList.size}")
        }
    }

    // Add button click logging
    val onCheckAnswerClick: () -> Unit = {
        val answer = selectedWords.joinToString(" ")
        Log.d("QuizScreen", "Checking answer: '$answer' against correct answer: '$correctAnswer'")
        
        if (answer == correctAnswer) {
            result = "Đúng rồi!"
            Log.d("QuizScreen", "Answer is correct!")
            
            scope.launch {
                // Update user progress
                user?.let {
                    Log.d("QuizScreen", "Updating user progress for userId: ${it.id}, exerciseId: ${currentExercise?.id}")
                    userProgressRepository.updateUserProgress(
                        userId = it.id,
                        courseId = courseId,
                        exerciseId = currentExercise?.id ?: "",
                        passed = true,
                        subLessonId = currentExercise?.subLessonId // Thêm subLessonId
                    )
                }

                // If it's the last question, mark the lesson as completed
                if (currentIndex == quizExercises.lastIndex) {
                    Log.d("QuizScreen", "This is the last question. Marking lesson as completed.")
                    user?.let {
                        val userProgressForCourse = userProgressList.firstOrNull { progress ->
                            progress.courseId == courseId
                        }

                        userProgressForCourse?.let { userProgress ->
                            val totalLessons = userProgress.totalLessons
                            Log.d("QuizScreen", "Marking lesson $lessonId as completed. Total lessons: $totalLessons")
                            
                            userProgressRepository.markLessonAsCompleted(
                                userId = it.id,
                                courseId = courseId,
                                lessonId = lessonId,
                                totalLessons = totalLessons
                            )
                        }
                    }
                }
            }
            showConfetti = true
        } else {
            result = "Sai rồi!"
            Log.d("QuizScreen", "Answer is incorrect!")
            wrongAnswerCount++
            Log.d("QuizScreen", "Wrong answer count for this question: $wrongAnswerCount")
            
            scope.launch {
                repeat(3) {
                    shakeOffset = 12f
                    delay(50)
                    shakeOffset = -12f
                    delay(50)
                }
                shakeOffset = 0f
            }
        }
        isAnswerChecked = true
    }
    
    // Reset wrong answer count when moving to next question
    LaunchedEffect(currentIndex) {
        wrongAnswerCount = 0
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Luyện tập",
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.DarkGray
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { 
                        Log.d("QuizScreen", "Back button clicked, navigating back")
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
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(16.dp)
            ) {
                // Progress indicator
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(4.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Câu hỏi ${currentIndex + 1}/${quizExercises.size}",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF2E7D32)
                            )
                            
                            Text(
                                text = "${(progress * 100).toInt()}%",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF2E7D32)
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = Color(0xFF4CAF50),
                            trackColor = Color(0xFFE8F5E9)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (!isAnswerChecked) {
                    currentExercise?.let {
                        val density = LocalDensity.current
                        val animatedOffset by animateFloatAsState(
                            targetValue = shakeOffset,
                            animationSpec = tween(100),
                            label = "shake"
                        )

                        Box(
                            modifier = Modifier
                                .offset(x = with(density) { animatedOffset.toDp() })
                                .fillMaxWidth()
                                .weight(1f)
                        ) {
                            QuestionCard(
                                question = it.question ?: "",
                                imageUrl = it.imageUrl ?: "",
                                romaji = it.romanji ?: "",
                                kana = it.kana ?: "",
                                options = it.options ?: emptyList(),
                                selectedWords = selectedWords,
                                onWordSelected = { word ->
                                    if (!selectedWords.contains(word)) {
                                        selectedWords = selectedWords + word
                                    }
                                },
                                onWordRemoved = { index ->
                                    selectedWords = selectedWords.toMutableList().apply { removeAt(index) }
                                }
                            )
                        }
                    }
                } else {
                    // Result view
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (result == "Đúng rồi!") Color(0xFFE8F5E9) else Color(0xFFFFEBEE)
                        ),
                        elevation = CardDefaults.cardElevation(4.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(80.dp)
                                    .background(
                                        color = if (result == "Đúng rồi!") Color(0xFF4CAF50) else Color.Red,
                                        shape = CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (result == "Đúng rồi!") "✓" else "✗",
                                    fontSize = 40.sp,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(24.dp))
                            
                            Text(
                                text = result ?: "",
                                color = if (result == "Đúng rồi!") Color(0xFF4CAF50) else if (result == "Sai rồi!") Color.Red else Color.Black,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp)
                            )
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            currentExercise?.let {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 8.dp),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Đáp án đúng: ",
                                        fontSize = 16.sp,
                                        color = Color.DarkGray
                                    )
                                    
                                    Text(
                                        text = correctAnswer,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF2E7D32)
                                    )
                                }
                                
                                if (result != "Đúng rồi!") {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 8.dp),
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Đáp án của bạn: ",
                                            fontSize = 16.sp,
                                            color = Color.DarkGray
                                        )
                                        
                                        Text(
                                            text = selectedWords.joinToString(" "),
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.Red
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Footer + Result
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(4.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        if (!isAnswerChecked) {
                            Button(
                                onClick = {
                                    Log.d("QuizScreen", "Check answer button clicked")
                                    onCheckAnswerClick()
                                },
                                enabled = selectedWords.isNotEmpty(),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF2E7D32),
                                    disabledContainerColor = Color(0xFFE0E0E0)
                                ),
                                modifier = Modifier.fillMaxWidth(),
                                contentPadding = PaddingValues(vertical = 16.dp)
                            ) {
                                Text(
                                    "KIỂM TRA", 
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                            }
                        } else {
                            Button(
                                onClick = {
                                    if (currentIndex < quizExercises.lastIndex) {
                                        Log.d("QuizScreen", "Moving to next question, index: ${currentIndex + 1}")
                                        currentIndex++
                                        selectedWords = listOf()
                                        result = null
                                        isAnswerChecked = false
                                    } else {
                                        Log.d("QuizScreen", "Quiz completed, navigating to lessons screen")
                                        // Set flag to refresh progress when returning to LessonsScreen
                                        navController.previousBackStackEntry?.savedStateHandle?.set("shouldRefreshProgress", true)
                                        navController.navigate("lessons/${courseId}/$userEmail") {
                                            popUpTo("lessons/${courseId}/$userEmail") { inclusive = false }
                                        }
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (currentIndex < quizExercises.lastIndex) 
                                        Color(0xFF2E7D32) else Color(0xFF1565C0)
                                ),
                                modifier = Modifier.fillMaxWidth(),
                                contentPadding = PaddingValues(vertical = 16.dp)
                            ) {
                                Text(
                                    if (currentIndex < quizExercises.lastIndex) "TIẾP TỤC" else "HOÀN THÀNH",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                            }
                        }
                    }
                }
            }

            // Confetti overlay
            if (showConfetti) {
                ConfettiOverlay(
                    onComplete = {
                        showConfetti = false
                    }
                )
            }
            FloatingAISensei(
                currentUser = user,
                aiRepository = aiRepository,
            )
        }
    }
}

@Composable
fun QuestionCard(
    question: String,
    imageUrl: String,
    romaji: String,
    kana: String,
    options: List<String>,
    selectedWords: List<String>,
    onWordSelected: (String) -> Unit,
    onWordRemoved: (Int) -> Unit
) {
    val scrollState = rememberScrollState()
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(scrollState)  // Thêm scroll cho toàn bộ nội dung
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = question,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1B5E20),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Image and Japanese text
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5)),
                elevation = CardDefaults.cardElevation(0.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Image with border
                    Card(
                        shape = RoundedCornerShape(8.dp),
                        elevation = CardDefaults.cardElevation(2.dp)
                    ) {
                        AsyncImage(
                            model = imageUrl,
                            contentDescription = "Question Image",
                            modifier = Modifier
                                .size(80.dp)
                                .clip(RoundedCornerShape(8.dp)),
                            contentScale = ContentScale.Crop
                        )
                    }
                    
                    Spacer(modifier = Modifier.width(16.dp))
                    
                    // Japanese text
                    Column {
                        if (kana.isNotEmpty()) {
                            Text(
                                text = kana,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1B5E20)
                            )
                        }
                        
                        if (romaji.isNotEmpty()) {
                            Text(
                                text = romaji,
                                fontSize = 16.sp,
                                fontStyle = FontStyle.Italic,
                                color = Color.Gray
                            )
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Selected words area
            Text(
                text = "Câu trả lời của bạn:",
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF1B5E20),
                modifier = Modifier.align(Alignment.Start)
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 60.dp),
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5)),
                border = BorderStroke(1.dp, Color(0xFFE0E0E0))
            ) {
                FlowRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    mainAxisSpacing = 8.dp,
                    crossAxisSpacing = 8.dp
                ) {
                    if (selectedWords.isEmpty()) {
                        Text(
                            text = "Chọn từ bên dưới để tạo câu trả lời",
                            fontSize = 14.sp,
                            color = Color.Gray,
                            fontStyle = FontStyle.Italic,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    } else {
                        selectedWords.forEachIndexed { index, word ->
                            OutlinedButton(
                                onClick = { onWordRemoved(index) },
                                border = BorderStroke(1.dp, Color(0xFF2E7D32)),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = Color(0xFFE8F5E9)
                                ),
                                shape = RoundedCornerShape(20.dp)
                            ) {
                                Text(
                                    text = word,
                                    color = Color.Black,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Remove",
                                    modifier = Modifier.size(16.dp),
                                    tint = Color(0xFF2E7D32)
                                )
                            }
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Available options
            Text(
                text = "Các từ có sẵn:",
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF1B5E20),
                modifier = Modifier.align(Alignment.Start)
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                mainAxisSpacing = 8.dp,
                crossAxisSpacing = 8.dp
            ) {
                options.forEach { option ->
                    val isSelected = selectedWords.contains(option)
                    OutlinedButton(
                        onClick = { onWordSelected(option) },
                        enabled = !isSelected,
                        border = BorderStroke(
                            width = 1.dp,
                            color = if (isSelected) Color.LightGray else Color(0xFF2E7D32)
                        ),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = if (isSelected) Color.LightGray else Color.White,
                            disabledContainerColor = Color.LightGray
                        ),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Text(
                            text = option,
                            color = if (isSelected) Color.Gray else Color(0xFF2E7D32)
                        )
                    }
                }
            }
            
            // Thêm khoảng trống ở cuối để đảm bảo nội dung không bị cắt
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
fun AnimatedResult(result: String?) {
    val isCorrect = result == "Đúng rồi!"
    val scale = remember { Animatable(0f) }

    LaunchedEffect(result) {
        if (result != null) {
            scale.snapTo(0f)
            scale.animateTo(1f, tween(1800, easing = EaseOutBack))
        }
    }

    AnimatedVisibility(
        visible = result != null,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .scale(scale.value)
                    .background(
                        color = if (isCorrect) Color(0xFF4CAF50) else Color.Red,
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (isCorrect) "✅" else "❌",
                    fontSize = 28.sp
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = result ?: "",
                fontSize = 18.sp,
                color = if (isCorrect) Color(0xFF2E7D32) else Color.Red,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun ConfettiOverlay(onComplete: () -> Unit) {
    val confettiParticles = remember { mutableStateListOf<ConfettiParticle>() }

    LaunchedEffect(Unit) {
        confettiParticles.clear()
        repeat(100) {
            confettiParticles.add(ConfettiParticle.random())
        }
        delay(2000)
        onComplete()
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(250.dp)
            .padding(top = 48.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        confettiParticles.forEach { particle ->
            ConfettiItem(particle)
        }
    }
}


@Composable
fun ConfettiItem(particle: ConfettiParticle) {
    val x = remember { Animatable(particle.startX) }
    val y = remember { Animatable(particle.startY) }
    val alpha = remember { Animatable(1f) }

    val shape = remember {
        listOf(CircleShape, RoundedCornerShape(3.dp), StarShape).random()
    }

    LaunchedEffect(Unit) {
        launch {
            x.animateTo(
                targetValue = particle.endX,
                animationSpec = tween(durationMillis = 1000, easing = LinearEasing)
            )
        }
        launch {
            y.animateTo(
                targetValue = particle.endY,
                animationSpec = tween(durationMillis = 1000, easing = LinearOutSlowInEasing)
            )
        }
        launch {
            alpha.animateTo(
                targetValue = 0f,
                animationSpec = tween(durationMillis = 1000)
            )
        }
    }

    Box(
        modifier = Modifier
            .offset { IntOffset(x.value.toInt(), y.value.toInt()) }
            .size(10.dp)
            .graphicsLayer { this.alpha = alpha.value }
            .background(
                color = particle.color,
                shape = StarShape
            )
    )
}

data class ConfettiParticle(
    val startX: Float,
    val startY: Float,
    val endX: Float,
    val endY: Float,
    val color: Color
) {
    companion object {
        fun random(): ConfettiParticle {
            val screenWidth = -100f
            val screenHeight = 1500f

            val centerX = screenWidth / 2
            val startX = centerX
            val startY = Random.nextFloat() * 50f // gần đỉnh

            // endX lệch ra trái hoặc phải từ tâm
            val maxOffsetX = 200f // có thể điều chỉnh để bay rộng hơn hoặc hẹp hơn
            val endX = centerX + (Random.nextFloat() * maxOffsetX * if (Random.nextBoolean()) 1 else -1)

            val endY = Random.nextFloat() * screenHeight

            return ConfettiParticle(
                startX = startX,
                startY = startY,
                endX = endX,
                endY = endY,
                color = Color(
                    red = Random.nextFloat(),
                    green = Random.nextFloat(),
                    blue = Random.nextFloat()
                )
            )
        }
    }

}

object StarShape : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val path = Path()
        val midX = size.width / 2
        val midY = size.height / 2
        val radius = size.minDimension / 2
        val spikes = 5
        val outerRadius = radius
        val innerRadius = radius / 2.5f
        val angle = Math.PI / spikes

        path.moveTo(midX, midY - outerRadius)
        for (i in 1 until spikes * 2) {
            val r = if (i % 2 == 0) outerRadius else innerRadius
            val x = midX + (r * cos(i * angle)).toFloat()
            val y = midY - (r * sin(i * angle)).toFloat()
            path.lineTo(x, y)
        }
        path.close()

        return Outline.Generic(path)
    }
}
