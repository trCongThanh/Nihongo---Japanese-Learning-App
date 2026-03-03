package com.example.nihongo.User.ui.screens.homepage

import android.util.Log
import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.with
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Comment
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Leaderboard
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.filled.ViewAgenda
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.nihongo.User.data.models.Course
import com.example.nihongo.User.data.models.User
import com.example.nihongo.User.data.models.UserProgress
import com.example.nihongo.User.data.repository.AIRepository
import com.example.nihongo.User.data.repository.CourseRepository
import com.example.nihongo.User.data.repository.UserRepository
import com.example.nihongo.User.ui.components.BottomNavigationBar
import com.example.nihongo.User.ui.components.FloatingAISensei
import com.example.nihongo.User.ui.components.TopBarIcon
import com.onesignal.OneSignal
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navController: NavController,
    user_email: String,
    userRepository: UserRepository,
    courseRepository: CourseRepository
) {
    var currentUser by remember { mutableStateOf<User?>(null) }
    var courseList by remember { mutableStateOf<List<Course>>(emptyList()) }
    var userProgressList by remember { mutableStateOf<List<UserProgress>>(emptyList()) }
    val selectedItem = "home"
    var isLoading by remember { mutableStateOf(true) }
    val context = LocalContext.current
    OneSignal.User.addTag(user_email, "true")
    val aiRepository = remember { AIRepository() }
// Calculate overall progress
    val totalLessons = userProgressList.sumOf { it.totalLessons }
    val completedLessons = userProgressList.flatMap { it.completedLessons }.distinct().size
    val progressPercentage = if (totalLessons > 0) {
        (completedLessons.toFloat() / totalLessons.toFloat()) * 100
    } else 0f

    LaunchedEffect(user_email) {
        try {
            currentUser = userRepository.getUserByEmail(user_email)
            courseList = courseRepository.getAllCourses()
            currentUser?.let { user ->
                userProgressList = userRepository.getAllUserProgress(user.id)
            }
            isLoading = false
        } catch (e: Exception) {
            Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            isLoading = false
        }
    }

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
                                    fontSize = 10.sp
                                )
                            )
                            if (it.vip) {
                                Text(
                                    "\u2B50 VIP です!",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontSize = 8.sp
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
                        navController.navigate("community/$user_email") {
                            popUpTo(navController.graph.startDestinationId) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }) {
                        Icon(Icons.Default.Group, contentDescription = "Community")
                    }
                    IconButton(onClick = {
                        navController.navigate("profile/$user_email") {
                            popUpTo(navController.graph.startDestinationId) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }) {
                        Icon(Icons.Default.Person, contentDescription = "Profile")
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
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Color(0xFF00C853))
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item{
                    Spacer(modifier = Modifier.height(16.dp))
                }
                // Welcome Card with Stats
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        elevation = CardDefaults.cardElevation(4.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // User avatar or icon
                                Box(
                                    modifier = Modifier
                                        .size(60.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFE0F2F1)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    currentUser?.let {
                                        if (it.imageUrl.isNotEmpty()) {
                                            AsyncImage(
                                                model = it.imageUrl,
                                                contentDescription = "User Avatar",
                                                modifier = Modifier.fillMaxSize(),
                                                contentScale = ContentScale.Crop
                                            )
                                        } else {
                                            Icon(
                                                imageVector = Icons.Default.Person,
                                                contentDescription = null,
                                                tint = Color(0xFF00C853),
                                                modifier = Modifier.size(36.dp)
                                            )
                                        }
                                    }
                                }
                                
                                Spacer(modifier = Modifier.width(16.dp))
                                
                                // User info
                                Column(modifier = Modifier.weight(1f)) {
                                    currentUser?.let {
                                        Text(
                                            text = it.username,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = it.rank,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = Color.Gray
                                            )
                                            
                                            if (it.vip) {
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Surface(
                                                    color = Color(0xFFFFC107),
                                                    shape = RoundedCornerShape(4.dp)
                                                ) {
                                                    Text(
                                                        text = "VIP",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        fontWeight = FontWeight.Bold,
                                                        color = Color.White,
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            // Stats section
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                // Activity Points
                                UserStatItem(
                                    icon = Icons.Default.EmojiEvents,
                                    value = currentUser?.activityPoints?.toString() ?: "0",
                                    label = "Điểm năng động",
                                    color = Color(0xFFFFA000)
                                )
                                
                                // Enrolled Courses
                                UserStatItem(
                                    icon = Icons.Default.School,
                                    value = userProgressList.size.toString(),
                                    label = "Khóa học",
                                    color = Color(0xFF2196F3)
                                )
                                
                                // JLPT Level
                                UserStatItem(
                                    icon = Icons.Default.Star,
                                    value = currentUser?.jlptLevel?.let { "N$it" } ?: "N/A",
                                    label = "JLPT Level",
                                    color = Color(0xFF4CAF50)
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            // Progress bar
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Tiến độ học tập",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium
                                    )

                                    Text(
                                        text = "${progressPercentage.toInt()}%",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Color(0xFF4CAF50),
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                
                                Spacer(modifier = Modifier.height(8.dp))
                                
                                LinearProgressIndicator(
                                    progress = { if (totalLessons > 0) completedLessons.toFloat() / totalLessons.toFloat() else 0f },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(8.dp)
                                        .clip(RoundedCornerShape(4.dp)),
                                    color = Color(0xFF4CAF50),
                                    trackColor = Color(0xFFE0E0E0)
                                )
                                
                                Spacer(modifier = Modifier.height(8.dp))
                                
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "$completedLessons/$totalLessons bài học",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.Gray
                                    )
                                    
                                    Text(
                                        text = "Cập nhật: ${SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.Gray
                                    )
                                }
                            }
                        }
                    }
                }

                // Your Progress Section
                if (userProgressList.isNotEmpty()) {
                    item {
                        Text(
                            "Tiến độ của bạn",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black, // Explicitly set to black
                            modifier = Modifier.padding(top = 8.dp, bottom = 8.dp)
                        )
                    }
                    
                    items(userProgressList.take(3)) { progress ->
                        val course = courseList.find { it.id == progress.courseId }
                        course?.let {
                            CourseProgressCard(
                                course = it,
                                userProgress = progress,
                                onContinueClick = {
                                    navController.navigate("lessons/${it.id}/$user_email")
                                }
                            )
                        }
                    }
                    
                    if (userProgressList.size > 3) {
                        item {
                            Button(
                                onClick = {
                                    navController.navigate("courses/$user_email") {
                                        popUpTo(navController.graph.startDestinationId) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color.White,
                                    contentColor = Color(0xFF00C853)
                                ),
                                border = BorderStroke(1.dp, Color(0xFF00C853)),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Xem tất cả khóa học của bạn")
                            }
                        }
                    }
                }
                // Learning Tools Section
                item {
                    Text(
                        "Công cụ học tập",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black,
                        modifier = Modifier.padding(top = 8.dp, bottom = 8.dp)
                    )
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        LearningToolCard(
                            title = "Flashcards",
                            icon = Icons.Default.ViewAgenda,
                            backgroundColor = Color(0xFFF44336),
                            modifier = Modifier.weight(1f),
                            onClick = {
                                navController.navigate("vocabulary/$user_email") {
                                    popUpTo(navController.graph.startDestinationId) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )

                        LearningToolCard(
                            title = "Bảng xếp hạng",
                            icon = Icons.Default.Leaderboard,
                            backgroundColor = Color(0xFF2196F3),
                            modifier = Modifier.weight(1f),
                            onClick = {
                                // Điều hướng đến community screen với tham số tab=2 (tab bảng xếp hạng)
                                navController.navigate("community/$user_email?tab=2") {
                                    popUpTo(navController.graph.startDestinationId) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        LearningToolCard(
                            title = "Cộng đồng",
                            icon = Icons.Default.Group,
                            backgroundColor = Color(0xFF9C27B0),
                            modifier = Modifier.weight(1f),
                            onClick = {
                                navController.navigate("community/$user_email") {
                                    popUpTo(navController.graph.startDestinationId) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )

                        LearningToolCard(
                            title = "Luyện tập",
                            icon = Icons.Default.FitnessCenter,
                            backgroundColor = Color(0xFFFF9800),
                            modifier = Modifier.weight(1f),
                            onClick = {
                                // Navigate to practice section
                                navController.navigate("courses/$user_email") {
                                    popUpTo(navController.graph.startDestinationId) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }
                }

                // Trending Courses Section
                item {
                    Text(
                        "Tất cả khóa học",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black, // Explicitly set to black
                        modifier = Modifier.padding(top = 8.dp, bottom = 8.dp)
                    )
                }
                
                item {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(vertical = 4.dp)
                    ) {
                        items(courseList) { course ->
                            CourseCard(
                                course = course,
                                onClick = {
                                    val progress = userProgressList.find { it.courseId == course.id }
                                    if (progress != null) {
                                        navController.navigate("lessons/${course.id}/$user_email")
                                    } else {
                                        navController.navigate("courses/${course.id}/$user_email")
                                    }
                                },
                                modifier = Modifier.width(220.dp)
                            )
                        }
                    }
                }

                // Recommended Courses Section
                item {
                    Text(
                        "Khóa học đề xuất",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black, // Explicitly set to black
                        modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                    )
                }
                
                items(courseList.filter { !userProgressList.any { progress -> progress.courseId == it.id } }.take(3)) { course ->
                    RecommendedCourseCard(
                        course = course,
                        onClick = {
                            navController.navigate("courses/${course.id}/$user_email")
                        }
                    )
                }

                // Bottom spacing
                item {
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
            FloatingAISensei(
                currentUser = currentUser,
                aiRepository = aiRepository,
            )
        }
    }
}


@OptIn(ExperimentalAnimationApi::class)
@Composable
fun FlipFlashcard(term: String, definition: String) {
    var isFlipped by rememberSaveable { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .width(65.dp)
            .height(60.dp)
            .clickable { isFlipped = !isFlipped },
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(8.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            AnimatedContent(
                targetState = isFlipped,
                transitionSpec = {
                    fadeIn() + scaleIn() with fadeOut() + scaleOut()
                },
                label = "Flip Flashcard"
            ) { flipped ->
                if (flipped) {
                    Text(
                        text = definition,
                        style = MaterialTheme.typography.headlineSmall,
                        textAlign = TextAlign.Center,
                        color = Color(0xFF333333)
                    )
                } else {
                    Text(
                        text = term,
                        style = MaterialTheme.typography.headlineLarge,
                        textAlign = TextAlign.Center,
                        color = Color(0xFF4CAF50)
                    )
                }
            }
        }
    }
}



@Composable
fun CourseCard(
    course: Course,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Log.d("CourseCard", "imageRes ID: ${course.imageRes}")
    Box(
        modifier = modifier
            .width(250.dp)
            .height(160.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(Color.LightGray)
            .clickable { onClick() }
    ) {
        AsyncImage(
            model = course.imageRes,
            contentDescription = "Course Image",
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Color.Gray),
            contentScale = ContentScale.Crop
        )

        if (course.vip) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(8.dp)
                    .background(Color(0xFFFFD700).copy(alpha = 0.8f), RoundedCornerShape(4.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "VIP",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
            }
        }

        Icon(
            imageVector = Icons.Default.OpenInNew,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .background(Color.Black.copy(alpha = 0.4f), shape = CircleShape)
                .padding(8.dp)
        )

        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.6f))
                    )
                )
                .padding(12.dp)
        ) {
            Text(
                text = course.title,
                color = Color.White,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = course.description,
                color = Color.White.copy(alpha = 0.7f),
                style = MaterialTheme.typography.bodySmall
            )

            Row(
                modifier = Modifier.padding(top = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.ThumbUp,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(14.dp)
                )
                Text("${course.likes}", color = Color.White, fontSize = 12.sp)

                Icon(
                    imageVector = Icons.Default.Comment,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(14.dp)
                )
                Text("${course.reviews}", color = Color.White, fontSize = 12.sp)
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(12.dp)
                .background(Color.White, RoundedCornerShape(8.dp))
                .padding(horizontal = 6.dp, vertical = 2.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "${course.rating}",
                    style = MaterialTheme.typography.bodySmall
                )
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = null,
                    tint = Color(0xFFFFC107),
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}
@Composable
fun CourseProgressCard(
    course: Course,
    userProgress: UserProgress,
    onContinueClick: () -> Unit
) {
    val progressPercent = (userProgress.progress * 100).toInt()
    val completed = userProgress.completedLessons.size
    val total = userProgress.totalLessons

    Card(
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AsyncImage(
                    model = course.imageRes,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(60.dp)
                        .clip(RoundedCornerShape(12.dp))
                )

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = course.title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Text(
                        text = "$completed/$total lesson",
                        fontSize = 13.sp,
                        color = Color.Gray
                    )
                }

                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(48.dp) // Đảm bảo Box có cùng kích thước với CircularProgressIndicator
                ) {
                    CircularProgressIndicator(
                        progress = (userProgress.progress),  // Chuyển đổi sang giá trị từ 0 đến 1
                        modifier = Modifier.size(48.dp),
                        color = Color(0xFF4CAF50),
                        strokeWidth = 4.dp
                    )

                    Text(
                        text = "$progressPercent%",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(start = 6.dp)
                    )
                }

            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onContinueClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Transparent // Dùng nền trong suốt
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        brush = Brush.horizontalGradient(
                            listOf(
                                Color(0xFF006FD1), // Xanh biển (turquoise)
                                Color(0xFF01A810)  // Xanh lá
                            )
                        )
                        ,
                        shape = RoundedCornerShape(30.dp)
                    )
                    .clip(RoundedCornerShape(30.dp)), // để không vượt ra khỏi góc bo
                shape = RoundedCornerShape(30.dp)
            ) {
                Text(
                    text = "Continue",
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}

@Composable
fun MiniFeatureCard(title: String, icon: ImageVector, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        color = Color(0xFFF5F5F5),
        shape = RoundedCornerShape(50),
        tonalElevation = 2.dp
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                modifier = Modifier.size(16.dp),
                tint = Color.Black.copy(alpha = 0.7f)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = title,
                fontSize = 12.sp,
                color = Color.Black.copy(alpha = 0.8f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun StatCard(
    icon: ImageVector,
    value: String,
    label: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .background(Color.White.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
            .padding(12.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(24.dp)
        )
        
        Spacer(modifier = Modifier.height(4.dp))
        
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.9f)
        )
    }
}

@Composable
fun LearningToolCard(
    title: String,
    icon: ImageVector,
    backgroundColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .height(100.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(4.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(32.dp)
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun RecommendedCourseCard(
    course: Course,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(4.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Course Image
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.LightGray)
            ) {
                AsyncImage(
                    model = course.imageRes,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                
                if (course.vip) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(4.dp)
                            .background(Color(0xFFFFC107), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "VIP",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.Black,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            // Course Info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = course.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = course.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = Color(0xFF00C853),
                        modifier = Modifier.size(16.dp)
                    )
                    
                    Spacer(modifier = Modifier.width(10.dp))

                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        tint = Color(0xFFFFC107),
                        modifier = Modifier.size(16.dp)
                    )
                    
                    Spacer(modifier = Modifier.width(4.dp))
                    
                    Text(
                        text = course.rating.toString(),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Gray
                    )
                }
            }
            
            // Enroll Button
            IconButton(
                onClick = onClick,
                modifier = Modifier
                    .size(40.dp)
                    .background(Color(0xFF00C853), CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowForward,
                    contentDescription = "Enroll",
                    tint = Color.White
                )
            }
        }
    }
}

@Composable
fun ProgressStat(
    value: Int,
    label: String,
    icon: ImageVector,
    color: Color
) {
    Column(
        modifier = Modifier
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(24.dp)
        )
        
        Spacer(modifier = Modifier.height(4.dp))
        
        Text(
            text = value.toString(),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = color
        )
        
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = Color.Gray
        )
    }
}

@Composable
fun UserStatItem(
    icon: ImageVector,
    value: String,
    label: String,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(horizontal = 8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(24.dp)
            )
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = Color.Gray
        )
    }
}
