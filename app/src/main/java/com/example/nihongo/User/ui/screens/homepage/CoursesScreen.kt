package com.example.nihongo.User.ui.screens.homepage

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.ThumbDown
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.nihongo.User.data.models.Course
import com.example.nihongo.User.data.models.CourseReview
import com.example.nihongo.User.data.models.Lesson
import com.example.nihongo.User.data.models.User
import com.example.nihongo.User.data.models.UserProgress
import com.example.nihongo.User.data.repository.AIRepository
import com.example.nihongo.User.data.repository.CourseRepository
import com.example.nihongo.User.data.repository.LessonRepository
import com.example.nihongo.User.data.repository.UserRepository
import com.example.nihongo.User.ui.components.BottomNavItem
import com.example.nihongo.User.ui.components.BottomNavigationBar
import com.example.nihongo.User.ui.components.FloatingAISensei
import com.example.nihongo.User.ui.components.TopBarIcon
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CoursesScreen(
    navController: NavController,
    courseRepository: CourseRepository,
    userEmail: String
) {
    val allCourses = remember { mutableStateOf<List<Course>>(emptyList()) }
    var searchQuery by remember { mutableStateOf("") }
    val selectedItem = "courses"
    val filteredCourses = remember(searchQuery, allCourses.value) {
        if (searchQuery.isBlank()) allCourses.value
        else allCourses.value.filter { it.title.contains(searchQuery, ignoreCase = true) }
    }


    // Thêm tabs và selectedTab
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("Tất cả khóa học", "Khóa học của tôi", "Khóa học VIP")
    
    // Lấy danh sách khóa học của người dùng
    val userProgressList = remember { mutableStateOf<List<UserProgress>>(emptyList()) }
    val userRepository = UserRepository()
    val aiRepository = remember { AIRepository() }
    var currentUser by remember { mutableStateOf<User?>(null) }
    LaunchedEffect(Unit) {
        currentUser = userRepository.getUserByEmail(userEmail)

        allCourses.value = courseRepository.getAllCourses()
        val user = userRepository.getUserByEmail(userEmail)
        user?.let {
            userProgressList.value = userRepository.getAllUserProgress(it.id)
        }
    }
    
    // Lọc khóa học theo tab
    val displayedCourses = when (selectedTab) {
        0 -> filteredCourses // Tất cả khóa học
        1 -> filteredCourses.filter { course -> 
            userProgressList.value.any { it.courseId == course.id } 
        } // Khóa học của tôi
        2 -> filteredCourses.filter { it.vip } // Khóa học VIP
        else -> filteredCourses
    }

    Scaffold(
        containerColor = Color(0xFFF5F5F5),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        TextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("Tìm khóa học", color = Color.Gray) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 2.dp),
                            colors = TextFieldDefaults.colors(
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent,
                                focusedContainerColor = Color(0xFFF0F0F0),
                                unfocusedContainerColor = Color(0xFFF0F0F0)
                            ),
                            shape = RoundedCornerShape(8.dp),
                            singleLine = true,
                            trailingIcon = {
                                if (searchQuery.isNotBlank()) {
                                    IconButton(onClick = {
                                        val course = filteredCourses.firstOrNull()
                                        course?.let {
                                            navController.navigate("courses/${course.id}/$userEmail")
                                        }
                                    }) {
                                        Icon(
                                            Icons.Default.Search, 
                                            contentDescription = "Search",
                                            tint = Color(0xFF4CAF50)
                                        )
                                    }
                                }
                            },
                            textStyle = MaterialTheme.typography.bodyMedium.copy(color = Color.Black)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = {
                        // Navigate to HomeScreen when back button is pressed
                        navController.navigate("${BottomNavItem.Home.route}/$userEmail") {
                            popUpTo(navController.graph.startDestinationId) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }) {
                        TopBarIcon(selectedItem = selectedItem)
                    }
                },
                actions = {
                    IconButton(onClick = {
                        navController.navigate("community/$userEmail") {
                            popUpTo(navController.graph.startDestinationId) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }) {
                        Icon(Icons.Default.Group, contentDescription = "Community")
                    }
                    IconButton(onClick = {
                        navController.navigate("profile/$userEmail") {
                            popUpTo(navController.graph.startDestinationId) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }) {
                        Icon(Icons.Default.Person, contentDescription = "Profile")
                    }
                },
                modifier = Modifier.height(80.dp)
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White
                )
            )
        },
        bottomBar = {
            BottomNavigationBar(
                navController = navController,
                selectedItem = selectedItem,
                userEmail = userEmail,
                onItemSelected = { selectedRoute ->
                    navController.navigate("$selectedRoute/$userEmail") {
                        popUpTo(navController.graph.startDestinationId) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
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
                                color = if (selectedTab == index) Color.Black else Color.Gray,
                                fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    )
                }
            }
            
            // Content based on selected tab
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                // Section title
                Text(
                    text = tabs[selectedTab],
                    color = Color.Black,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
                
                // Alternating layout for courses
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (displayedCourses.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = when (selectedTab) {
                                        0 -> "Không tìm thấy khóa học nào"
                                        1 -> "Bạn chưa tham gia khóa học nào"
                                        2 -> "Không có khóa học VIP nào"
                                        else -> "Không tìm thấy khóa học nào"
                                    },
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = Color.Gray,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    } else {
                        items(displayedCourses.chunked(2)) { rowCourses ->
                            if (rowCourses.size == 2) {
                                // Two cards in a row
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    rowCourses.forEach { course ->
                                        Box(modifier = Modifier.weight(1f)) {
                                            CourseCard(
                                                course = course,
                                                onClick = {
                                                    val progress = userProgressList.value.find { it.courseId == course.id }
                                                    if (progress != null) {
                                                        navController.navigate("lessons/${course.id}/$userEmail")
                                                    } else {
                                                        navController.navigate("courses/${course.id}/$userEmail")
                                                    }
                                                }
                                            )
                                        }
                                    }
                                }
                            } else {
                                // Single card (full width)
                                rowCourses.forEach { course ->
                                    CourseCard(
                                        course = course,
                                        onClick = {
                                            val progress = userProgressList.value.find { it.courseId == course.id }
                                            if (progress != null) {
                                                navController.navigate("lessons/${course.id}/$userEmail")
                                            } else {
                                                navController.navigate("courses/${course.id}/$userEmail")
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
        FloatingAISensei(
            currentUser = currentUser,
            aiRepository = aiRepository
        )
    }
}



@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CourseDetailScreen(
    courseId: String,
    navController: NavController,
    courseRepository: CourseRepository,
    userRepository: UserRepository,
    lessonRepository: LessonRepository,
    userEmail: String
) {
    val course = remember { mutableStateOf<Course?>(null) }
    val isUserVip = remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val lessons = remember { mutableStateOf<List<Lesson>>(emptyList()) }
    val context = LocalContext.current
    val selectedItem = "courses"
    
    // Thêm tabs và selectedTab
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("Thông tin khóa học", "Đánh giá", "Thích/Không thích")
    
    // Thêm state cho đánh giá và thích/không thích
    var reviews by remember { mutableStateOf<List<CourseReview>>(emptyList()) }
    var userReview by remember { mutableStateOf<CourseReview?>(null) }
    var isLiked by remember { mutableStateOf(false) }
    var currentUser by remember { mutableStateOf<User?>(null) }
    var reviewText by remember { mutableStateOf("") }
    var rating by remember { mutableStateOf(5) }
    
    // Add a key for refreshing data
    var refreshTrigger by remember { mutableStateOf(0) }

    // Function to refresh data
    fun refreshData() {
        refreshTrigger += 1
    }

    // Fetch data
    LaunchedEffect(courseId, refreshTrigger) {
        course.value = courseRepository.getCourseById(courseId)
        lessons.value = lessonRepository.getLessonsByCourseId(courseId)
        
        // Lấy thông tin người dùng hiện tại
        val user = userRepository.getUserByEmail(userEmail)
        currentUser = user
        
        // Kiểm tra xem người dùng đã thích khóa học chưa
        user?.let { 
            isUserVip.value = user.vip
            isLiked = courseRepository.isCourseLikedByUser(courseId, user.id)
            
            // Lấy đánh giá của người dùng hiện tại
            userReview = courseRepository.getUserReviewForCourse(courseId, user.id)
            userReview?.let {
                reviewText = it.text
                rating = it.rating
            }
        }
        
        // Lấy tất cả đánh giá của khóa học
        reviews = courseRepository.getCourseReviews(courseId)
    }

    Scaffold(
        containerColor = Color(0xFFEEEEEE),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Chi tiết khóa học",
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.DarkGray
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
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
        bottomBar = {
            BottomNavigationBar(
                navController = navController,
                selectedItem = selectedItem,
                userEmail = userEmail,
                onItemSelected = { selectedRoute ->
                    navController.navigate("$selectedRoute/$userEmail") {
                        popUpTo(navController.graph.startDestinationId) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
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
                                color = if (selectedTab == index) Color.Black else Color.Gray,
                                fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    )
                }
            }
            
            // Content based on selected tab
            when (selectedTab) {
                0 -> CourseInfoTab(course.value, lessons.value, isUserVip.value, userEmail, navController, coroutineScope, userRepository, context)
                1 -> CourseReviewsTab(reviews, userReview, currentUser, courseId, courseRepository, context) { refreshData() }
                2 -> CourseLikesTab(course.value, isLiked, currentUser, courseId, courseRepository, context) { refreshData() }
            }
        }
    }
}

@Composable
fun CourseInfoTab(
    course: Course?,
    lessons: List<Lesson>,
    isUserVip: Boolean,
    userEmail: String,
    navController: NavController,
    coroutineScope: CoroutineScope,
    userRepository: UserRepository,
    context: Context
) {
    if (course == null) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = Color(0xFF4CAF50))
        }
        return
    }
    
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Course Image
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(4.dp)
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    AsyncImage(
                        model = course.imageRes,
                        contentDescription = "Course Image",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    
                    // VIP badge if applicable
                    if (course.vip) {
                        Surface(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(8.dp),
                            shape = RoundedCornerShape(4.dp),
                            color = Color(0xFFFFD700)
                        ) {
                            Text(
                                text = "VIP",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.Black,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        }
        
        // Course Title and Description
        item {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = course.title,
                color = Color.Black,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = course.description,
                color = Color.Gray,
                style = MaterialTheme.typography.bodySmall
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Course stats
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                StatItem(
                    icon = Icons.Default.Book,
                    value = "${lessons.size}",
                    label = "Bài học"
                )

                StatItem(
                    icon = Icons.Default.Star,
                    value = "${course.rating}",
                    label = "Đánh giá"
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Course content preview
            Text(
                text = "Nội dung khóa học",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(8.dp))
        }
        
        // Lesson list preview (first 3 lessons)
        items(lessons.take(3)) { lesson ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayCircle,
                        contentDescription = null,
                        tint = Color(0xFF4CAF50),
                        modifier = Modifier.size(24.dp)
                    )
                    
                    Spacer(modifier = Modifier.width(12.dp))
                    
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = lesson.stepTitle,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
        
        // Show more lessons button if there are more than 3
        if (lessons.size > 3) {
            item {
                TextButton(
                    onClick = { /* Do nothing, user needs to join course first */ },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Xem thêm ${lessons.size - 3} bài học khác")
                }
                
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
        
        // Join course button
        item {
            Button(
                onClick = {
                    if (course.vip && !isUserVip) {
                        Toast.makeText(context, "Bạn cần là thành viên VIP để tham gia khóa học này", Toast.LENGTH_SHORT).show()
                    } else {
                        coroutineScope.launch {
                            val user = userRepository.getUserByEmail(userEmail)
                            user?.let {
                                val newProgress = UserProgress(
                                    userId = it.id,
                                    courseId = course.id,
                                    completedLessons = emptyList(),
                                    completedExercises = emptyList(),
                                    passedExercises = emptyList(),
                                    completedSubLessons = emptyList(),
                                    totalLessons = lessons.size,
                                    progress = 0.0f,
                                    lastUpdated = System.currentTimeMillis(),
                                    courseTitle = course.title,
                                    currentLessonId = null
                                )
                                userRepository.saveUserProgress(it.id, newProgress)
                                navController.navigate("lessons/${course.id}/$userEmail")
                            }
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
            ) {
                Text("Tham gia khóa học", fontWeight = FontWeight.Bold)
            }
            
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun CourseReviewsTab(
    reviews: List<CourseReview>,
    userReview: CourseReview?,
    currentUser: User?,
    courseId: String,
    courseRepository: CourseRepository,
    context: Context,
    onDataChanged: () -> Unit
) {
    var reviewText by remember { mutableStateOf(userReview?.text ?: "") }
    var rating by remember { mutableStateOf(userReview?.rating ?: 5) }
    val coroutineScope = rememberCoroutineScope()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // User review input section
        if (currentUser != null) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "Đánh giá của bạn",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Rating stars
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Start
                        ) {
                            for (i in 1..5) {
                                IconButton(
                                    onClick = { rating = i },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Star,
                                        contentDescription = "Star $i",
                                        tint = if (i <= rating) Color(0xFFFFD700) else Color.LightGray
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Review text field
                        OutlinedTextField(
                            value = reviewText,
                            onValueChange = { reviewText = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Nhập đánh giá của bạn") },
                            minLines = 3
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Submit button
                        Button(
                            onClick = {
                                if (reviewText.isNotBlank()) {
                                    coroutineScope.launch {
                                        val newReview = CourseReview(
                                            id = userReview?.id ?: "",
                                            userId = currentUser.id,
                                            courseId = courseId,
                                            text = reviewText,
                                            rating = rating,
                                            timestamp = System.currentTimeMillis(),
                                            userName = currentUser.username,
                                            userAvatar = currentUser.imageUrl
                                        )

                                        val success = if (userReview == null) {
                                            courseRepository.addCourseReview(newReview)
                                        } else {
                                            courseRepository.updateCourseReview(newReview)
                                        }

                                        if (success) {
                                            Toast.makeText(context, "Đánh giá đã được gửi", Toast.LENGTH_SHORT).show()
                                            onDataChanged() // Refresh data after successful submission
                                        } else {
                                            Toast.makeText(context, "Có lỗi xảy ra, vui lòng thử lại", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                } else {
                                    Toast.makeText(context, "Vui lòng nhập đánh giá", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.align(Alignment.End),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                        ) {
                            Text("Gửi đánh giá")
                        }
                    }
                }
            }
        }

        // Reviews list
        item {
            Text(
                text = "Đánh giá từ học viên (${reviews.size})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }

        if (reviews.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Chưa có đánh giá nào cho khóa học này",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.Gray,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            items(reviews.sortedByDescending { it.timestamp }) { review ->
                ReviewItem(review)
                Divider(modifier = Modifier.padding(vertical = 8.dp))
            }
        }
    }
}

@Composable
fun ReviewItem(review: CourseReview) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        // User info and rating
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // User avatar
            AsyncImage(
                model = review.userAvatar,
                contentDescription = "User Avatar",
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )
            
            Spacer(modifier = Modifier.width(8.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = review.userName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
                
                Text(
                    text = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(review.timestamp)),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }
            
            // Rating stars
            Row {
                for (i in 1..5) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        tint = if (i <= review.rating) Color(0xFFFFD700) else Color.LightGray,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Review text
        Text(
            text = review.text,
            style = MaterialTheme.typography.bodyMedium,
            color = Color.DarkGray
        )
    }
}

@Composable
fun CourseLikesTab(
    course: Course?,
    isLiked: Boolean,
    currentUser: User?,
    courseId: String,
    courseRepository: CourseRepository,
    context: Context,
    onDataChanged: () -> Unit
) {
    // Thêm biến để theo dõi trạng thái dislike
    var isDisliked by remember { mutableStateOf(false) }
    var liked by remember { mutableStateOf(isLiked) }
    val coroutineScope = rememberCoroutineScope()

    // Add a key for refreshing data
    var refreshTrigger by remember { mutableStateOf(0) }

    // Function to refresh data
    fun refreshData() {
        refreshTrigger += 1
    }

    // Kiểm tra xem user đã dislike chưa
    LaunchedEffect(currentUser) {
        currentUser?.let { user ->
            val dislikeDoc = courseRepository.isCoursedislikedByUser(courseId, user.id)
            isDisliked = dislikeDoc
        }
    }

    // Update liked state when isLiked prop changes
    LaunchedEffect(isLiked) {
        liked = isLiked
    }

    // Thêm state để lưu danh sách đánh giá và course data
    var courseReviews by remember { mutableStateOf<List<CourseReview>>(emptyList()) }
    var currentCourse by remember { mutableStateOf(course) }

    // Lấy danh sách đánh giá và course data khi component được tạo hoặc refresh
    LaunchedEffect(courseId, refreshTrigger) {
        courseReviews = courseRepository.getCourseReviews(courseId)
        // Refresh course data to get updated likes/dislikes
        currentCourse = courseRepository.getCourseById(courseId)
    }

    // Khi onDataChanged được gọi, cập nhật refreshTrigger
    LaunchedEffect(Unit) {
        // Khi component được tạo, tải dữ liệu ban đầu
        refreshData()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (currentCourse == null) {
            CircularProgressIndicator(color = Color(0xFF4CAF50))
            return
        }
        
        val displayCourse = currentCourse ?: course

        // Course info
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(2.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Đánh giá khóa học",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    // Like button
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        IconButton(
                            onClick = {
                                if (currentUser != null) {
                                    coroutineScope.launch {
                                        val success = if (isDisliked) {
                                            // Nếu đang dislike, bỏ dislike trước rồi mới like
                                            courseRepository.removeDislike(courseId, currentUser.id) &&
                                                    courseRepository.likeCourse(courseId, currentUser.id)
                                        } else if (liked) {
                                            // Nếu đang like, bỏ like (trạng thái trung lập)
                                            courseRepository.unlikeCourse(courseId, currentUser.id)
                                        } else {
                                            // Nếu trung lập, thêm like
                                            courseRepository.likeCourse(courseId, currentUser.id)
                                        }

                                        if (success) {
                                            if (isDisliked) isDisliked = false
                                            liked = !liked
                                            Toast.makeText(
                                                context,
                                                if (liked) "Đã thích khóa học" else "Đã bỏ thích khóa học",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                            // Refresh course data and reviews
                                            refreshData()
                                            onDataChanged() // Also call the parent's callback
                                        }
                                    }
                                } else {
                                    Toast.makeText(context, "Vui lòng đăng nhập để thích khóa học", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier
                                .size(64.dp)
                                .background(
                                    color = if (liked) Color(0xFFE8F5E9) else Color.Transparent,
                                    shape = CircleShape
                                )
                        ) {
                            Icon(
                                imageVector = Icons.Default.ThumbUp,
                                contentDescription = "Like",
                                tint = if (liked) Color(0xFF4CAF50) else Color.Gray,
                                modifier = Modifier.size(32.dp)
                            )
                        }

                        Text(
                            text = "Thích",
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (liked) Color(0xFF4CAF50) else Color.Gray,
                            fontWeight = if (liked) FontWeight.Bold else FontWeight.Normal
                        )

                        Text(
                            text = "${displayCourse?.likes ?: 0} người thích",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                    }

                    // Dislike button
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        IconButton(
                            onClick = {
                                if (currentUser != null) {
                                    coroutineScope.launch {
                                        val success = if (liked) {
                                            // Nếu đang like, bỏ like trước rồi mới dislike
                                            courseRepository.unlikeCourse(courseId, currentUser.id) &&
                                                    courseRepository.dislikeCourse(courseId, currentUser.id)
                                        } else if (isDisliked) {
                                            // Nếu đang dislike, bỏ dislike (trạng thái trung lập)
                                            courseRepository.removeDislike(courseId, currentUser.id)
                                        } else {
                                            // Nếu trung lập, thêm dislike
                                            courseRepository.dislikeCourse(courseId, currentUser.id)
                                        }

                                        if (success) {
                                            if (liked) liked = false
                                            isDisliked = !isDisliked
                                            Toast.makeText(
                                                context,
                                                if (isDisliked) "Đã không thích khóa học" else "Đã bỏ không thích khóa học",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                            // Refresh course data and reviews
                                            refreshData()
                                            onDataChanged() // Also call the parent's callback
                                        }
                                    }
                                } else {
                                    Toast.makeText(context, "Vui lòng đăng nhập để đánh giá khóa học", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier
                                .size(64.dp)
                                .background(
                                    color = if (isDisliked) Color(0xFFFBE9E7) else Color.Transparent,
                                    shape = CircleShape
                                )
                        ) {
                            Icon(
                                imageVector = Icons.Default.ThumbDown,
                                contentDescription = "Dislike",
                                tint = if (isDisliked) Color(0xFFE64A19) else Color.Gray,
                                modifier = Modifier.size(32.dp)
                            )
                        }

                        Text(
                            text = "Không thích",
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (isDisliked) Color(0xFFE64A19) else Color.Gray,
                            fontWeight = if (isDisliked) FontWeight.Bold else FontWeight.Normal
                        )

                        Text(
                            text = "${displayCourse?.dislikes ?: 0} người không thích",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Phân bố đánh giá
                Text(
                    text = "Phân bố đánh giá",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.Start)
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Hiển thị phân bố đánh giá - refresh when courseReviews changes
                val ratingDistribution = remember(courseReviews) {
                    courseReviews.groupBy { it.rating }
                        .mapValues { it.value.size }
                        .toSortedMap(compareByDescending { it })
                }

                val totalReviews = courseReviews.size.toFloat().coerceAtLeast(1f)

                for (i in 5 downTo 1) {
                    val count = ratingDistribution[i] ?: 0
                    val percentage = (count / totalReviews) * 100

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "$i",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.width(16.dp)
                        )

                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = Color(0xFFFFD700),
                            modifier = Modifier
                                .size(16.dp)
                                .padding(end = 8.dp)
                        )

                        LinearProgressIndicator(
                            progress = percentage / 100,
                            modifier = Modifier
                                .weight(1f)
                                .height(8.dp),
                            color = Color(0xFF4CAF50),
                            trackColor = Color.LightGray
                        )

                        Text(
                            text = "$count",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.width(24.dp),
                            textAlign = TextAlign.End
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun StatItem(icon: ImageVector, value: String, label: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color(0xFF4CAF50),
            modifier = Modifier.size(24.dp)
        )
        
        Spacer(modifier = Modifier.height(4.dp))
        
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
