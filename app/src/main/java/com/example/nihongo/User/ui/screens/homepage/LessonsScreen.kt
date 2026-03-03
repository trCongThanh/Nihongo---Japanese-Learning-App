package com.example.nihongo.ui.screens.homepage

import android.util.Log
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.QuestionAnswer
import androidx.compose.material.icons.filled.Star
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
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.nihongo.User.data.models.Course
import com.example.nihongo.User.data.models.CourseReview
import com.example.nihongo.User.data.models.Exercise
import com.example.nihongo.User.data.models.ExerciseType
import com.example.nihongo.User.data.models.Lesson
import com.example.nihongo.User.data.models.SubLesson
import com.example.nihongo.User.data.models.UnitItem
import com.example.nihongo.User.data.models.User
import com.example.nihongo.User.data.models.UserProgress
import com.example.nihongo.User.data.repository.AIRepository
import com.example.nihongo.User.data.repository.CourseRepository
import com.example.nihongo.User.data.repository.LessonRepository
import com.example.nihongo.User.data.repository.UserRepository
import com.example.nihongo.User.ui.components.BottomNavItem
import com.example.nihongo.User.ui.components.FloatingAISensei
import com.example.nihongo.User.ui.screens.homepage.CourseLikesTab
import com.example.nihongo.User.ui.screens.homepage.CourseReviewsTab
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LessonsScreen(
    courseId: String,
    userEmail: String,
    navController: NavController,
    courseRepository: CourseRepository,
    lessonRepository: LessonRepository,
    userRepository: UserRepository
) {
    val course = remember { mutableStateOf<Course?>(null) }
    val lessons = remember { mutableStateOf<List<Lesson>>(emptyList()) }
    val userProgress = remember { mutableStateOf<UserProgress?>(null) }
    val expandedLessons = remember { mutableStateMapOf<String, Boolean>() }
    val expandedUnits = remember { mutableStateMapOf<String, Boolean>() }
    val selectedItem = "courses"
    val coroutineScope = rememberCoroutineScope()
    
    // Add states for reviews and likes
    var reviews by remember { mutableStateOf<List<CourseReview>>(emptyList()) }
    var userReview by remember { mutableStateOf<CourseReview?>(null) }
    var isLiked by remember { mutableStateOf(false) }
    var isDisliked by remember { mutableStateOf(false) }
    var currentUser by remember { mutableStateOf<User?>(null) }
    val aiRepository = remember { AIRepository() }

    // Add refresh trigger
    var refreshTrigger by remember { mutableStateOf(0) }
    
    // Function to refresh data
    fun refreshData() {
        refreshTrigger += 1
    }
    
    // Thêm tabs và selectedTab
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("Bài học", "Tiến độ", "Tài liệu", "Đánh giá", "Thích/Không thích")
    
    // Save scroll position when navigating away
    val savedScrollPosition = navController.currentBackStackEntry?.savedStateHandle?.get<Int>("scrollPosition")
    
    // Check if returning from QuizScreen and refresh data
    LaunchedEffect(navController.currentBackStackEntry) {
        val shouldRefresh = navController.previousBackStackEntry?.savedStateHandle?.get<Boolean>("shouldRefreshProgress")
        if (shouldRefresh == true) {
            refreshData()
            navController.previousBackStackEntry?.savedStateHandle?.remove<Boolean>("shouldRefreshProgress")
        }
    }
    
    // Fetch data
    LaunchedEffect(courseId, refreshTrigger) {
        try {
            // Lấy thông tin khóa học và bài học
            course.value = courseRepository.getCourseById(courseId)
            
            // Thêm dữ liệu mẫu cho khóa học
//            val sampleLessons = createSampleLessons()
//            sampleLessons.forEach { sampleLesson ->
//                // Chỉ thêm bài học mẫu nếu nó thuộc về khóa học hiện tại
//                if (sampleLesson.courseId == courseId) {
////                    addLessonToCourse(courseId, sampleLesson)
//                    addExercisesToLesson(sampleLesson)
//                }
//            }
            
            // Lấy lại danh sách bài học sau khi đã thêm
            lessons.value = lessonRepository.getLessonsByCourseId(courseId)
            
            // Lấy thông tin người dùng
            val user = userRepository.getUserByEmail(userEmail)
            currentUser = user
            
            user?.let {
                // Lấy tiến độ của người dùng
                userProgress.value = userRepository.getUserProgressForCourse(it.id, courseId)
                
                // Kiểm tra xem user đã thích/không thích khóa học chưa
                isLiked = courseRepository.isCourseLikedByUser(courseId, it.id)
                isDisliked = courseRepository.isCoursedislikedByUser(courseId, it.id)
                
                // Lấy đánh giá của người dùng hiện tại
                userReview = courseRepository.getUserReviewForCourse(courseId, it.id)
            }
            
            // Lấy tất cả đánh giá của khóa học
            reviews = courseRepository.getCourseReviews(courseId)
        } catch (e: Exception) {
            Log.e("LessonsScreen", "Error loading data: ${e.message}")
        }
    }
    
    Scaffold(
        containerColor = Color(0xFFF5F5F5),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        course.value?.title ?: "Đang tải...",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold
                        )
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { 
                        // Navigate to HomeScreen instead of just popping back
                        navController.navigate("${BottomNavItem.Home.route}/$userEmail") {
                            popUpTo(navController.graph.startDestinationId) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Quay lại")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF4CAF50),
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Course header
            CourseHeader(course.value)
            
            // Replace TabRow with ScrollableTabRow
            ScrollableTabRow(
                selectedTabIndex = selectedTab,
                edgePadding = 16.dp,
                indicator = { tabPositions ->
                    TabRowDefaults.Indicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        height = 3.dp,
                        color = Color(0xFF4CAF50)
                    )
                },
                containerColor = Color.White,
                contentColor = Color(0xFF4CAF50)
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
                0 -> LessonsTab(
                    lessons = lessons.value,
                    userProgress = userProgress.value,
                    expandedLessons = expandedLessons,
                    expandedUnits = expandedUnits,
                    savedScrollPosition = savedScrollPosition,
                    onSubLessonClick = { sub, lesson ->
                        navController.navigate("exercise/${course.value?.id}/${lesson.id}/${sub.id}/$userEmail")
                    },
                    navController = navController
                )
                1 -> {
                    // Chuyển đổi UserProgress? thành List<UserProgress>
                    val progressList = userProgress.value?.let { listOf(it) } ?: emptyList()
                    ProgressTab(progressList, lessons.value)
                }
                2 -> MaterialsTab(course.value, navController, userEmail)
                3 -> {
                    CourseReviewsTab(
                        reviews = reviews,
                        userReview = userReview,
                        currentUser = currentUser,
                        courseId = courseId,
                        courseRepository = courseRepository,
                        context = LocalContext.current
                    ) { refreshData() }
                }
                4 -> {
                    CourseLikesTab(
                        course = course.value,
                        isLiked = isLiked,
                        currentUser = currentUser,
                        courseId = courseId,
                        courseRepository = courseRepository,
                        context = LocalContext.current
                    ) { 
                        refreshData()
                        // Also refresh course data
                        coroutineScope.launch {
                            course.value = courseRepository.getCourseById(courseId)
                        }
                    }
                }
            }
        }
        FloatingAISensei(
            currentUser = currentUser,
            aiRepository = aiRepository,
        )
    }
}

@Composable
fun CourseHeader(course: Course?) {
    if (course == null) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .background(Color.LightGray)
        )
        return
    }
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
    ) {
        // Background image
        AsyncImage(
            model = course.imageRes,
            contentDescription = "Course Image",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
        
        // Gradient overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.7f)
                        )
                    )
                )
        )
        
        // Course info
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(16.dp)
        ) {
            // VIP badge if applicable
            if (course.vip) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .background(
                            color = Color(0xFFFFD700),
                            shape = RoundedCornerShape(4.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = "VIP",
                        tint = Color.Black,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "VIP",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
            
            Text(
                text = course.title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = null,
                    tint = Color(0xFFFFC107),
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "${course.rating}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White
                )
                
                Spacer(modifier = Modifier.width(16.dp))
                
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
            }
        }
    }
}

@Composable
fun LessonsTab(
    lessons: List<Lesson>,
    userProgress: UserProgress?,
    expandedLessons: SnapshotStateMap<String, Boolean>,
    expandedUnits: SnapshotStateMap<String, Boolean>,
    savedScrollPosition: Int? = null,
    onSubLessonClick: (SubLesson, Lesson) -> Unit,
    navController: NavController? = null
) {
    val listState = rememberLazyListState(
        initialFirstVisibleItemIndex = savedScrollPosition ?: 0
    )
    
    // Save scroll position periodically and when navigating
    LaunchedEffect(listState.firstVisibleItemIndex) {
        navController?.currentBackStackEntry?.savedStateHandle?.set(
            "scrollPosition", 
            listState.firstVisibleItemIndex
        )
    }
    
    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        items(lessons) { lesson ->
            val isExpanded = expandedLessons[lesson.id] ?: false
            val isCompleted = userProgress?.completedLessons?.contains(lesson.id) ?: false

            ModernLessonCard(
                lesson = lesson,
                isExpanded = isExpanded,
                isLessonCompleted = isCompleted,
                expandedUnits = expandedUnits,
                onToggleExpand = { expandedLessons[lesson.id] = !isExpanded },
                onSubLessonClick = { subLesson -> onSubLessonClick(subLesson, lesson) },
                userProgress = userProgress
            )
            
            Spacer(modifier = Modifier.height(8.dp))
        }
        
        // Add some space at the bottom
        item {
            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

@Composable
fun ModernLessonCard(
    lesson: Lesson,
    isLessonCompleted: Boolean,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    expandedUnits: MutableMap<String, Boolean>,
    onSubLessonClick: (SubLesson) -> Unit,
    userProgress: UserProgress? = null
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                )
            ),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(4.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            // Lesson header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggleExpand() }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Lesson number circle
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(
                            color = if (isLessonCompleted) Color(0xFF4CAF50) else Color(0xFFE0E0E0),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (isLessonCompleted) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Completed",
                            tint = Color.White
                        )
                    } else {
                        Text(
                            text = "${lesson.step}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.DarkGray
                        )
                    }
                }
                
                Spacer(modifier = Modifier.width(16.dp))
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = lesson.stepTitle,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    
                    // Thay đổi ở đây: Sử dụng completedSubLessons
                    val completedText = if (userProgress != null) {
                        // Lấy tất cả ID của sub-lessons trong lesson này
                        val subLessonIds = lesson.units.flatMap { unit -> 
                            unit.subLessons.map { it.id } 
                        }
                        
                        // Đếm số sub-lessons đã hoàn thành bằng cách kiểm tra completedSubLessons
                        val completedSubLessons = subLessonIds.count { subLessonId ->
                            userProgress.completedSubLessons.contains(subLessonId)
                        }
                        
                        val totalSubLessons = subLessonIds.size
                        "$completedSubLessons/$totalSubLessons đã hoàn thành"
                    } else {
                        // Fallback nếu không có userProgress
                        "${lesson.completedUnits}/${lesson.totalUnits} đã hoàn thành"
                    }
                    
                    Text(
                        text = completedText,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }
                
                Icon(
                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (isExpanded) "Collapse" else "Expand",
                    tint = Color(0xFF4CAF50)
                )
            }
            
            // Progress indicator
            LinearProgressIndicator(
                progress = { 
                    if (userProgress != null) {
                        // Tính toán tiến độ dựa trên số sub-lessons đã hoàn thành trong lesson này
                        val subLessonIds = lesson.units.flatMap { unit -> 
                            unit.subLessons.map { it.id } 
                        }
                        
                        val completedSubLessons = subLessonIds.count { subLessonId ->
                            userProgress.completedSubLessons.contains(subLessonId)
                        }
                        
                        val totalSubLessons = subLessonIds.size
                        if (totalSubLessons > 0) completedSubLessons.toFloat() / totalSubLessons.toFloat() else 0f
                    } else {
                        if (lesson.totalUnits > 0) 
                            lesson.completedUnits.toFloat() / lesson.totalUnits.toFloat() 
                        else 0f 
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp),
                color = Color(0xFF4CAF50),
                trackColor = Color(0xFFE0E0E0)
            )
            
            // Expanded content
            if (isExpanded) {
                Divider(color = Color(0xFFEEEEEE), thickness = 1.dp)
                
                // Lesson overview
                if (lesson.overview.isNotBlank()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "Tổng quan",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Text(
                            text = lesson.overview,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.DarkGray
                        )
                    }
                    
                    Divider(color = Color(0xFFEEEEEE), thickness = 1.dp)
                }
                
                // Units
                lesson.units.forEach { unit ->
                    val unitKey = "${lesson.id}_${unit.unitTitle}"
                    ModernUnitCard(
                        unit = unit,
                        isExpanded = expandedUnits[unitKey] == true,
                        onClick = { expandedUnits[unitKey] = !(expandedUnits[unitKey] ?: false) },
                        onSubLessonClick = onSubLessonClick,
                        userProgress = userProgress
                    )
                }
            }
        }
    }
}

@Composable
fun ModernUnitCard(
    unit: UnitItem,
    isExpanded: Boolean,
    onClick: () -> Unit,
    onSubLessonClick: (SubLesson) -> Unit,
    userProgress: UserProgress? = null
) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        // Unit header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onClick() }
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Folder,
                contentDescription = null,
                tint = Color(0xFF4CAF50),
                modifier = Modifier.size(24.dp)
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = unit.unitTitle,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                
                // Cập nhật thông tin tiến độ của unit dựa trên userProgress
                val progressText = if (userProgress != null) {
                    // Đếm số sub-lessons đã hoàn thành trong unit này
                    val completedSubLessons = unit.subLessons.count { subLesson ->
                        userProgress.completedSubLessons.contains(subLesson.id)
                    }
                    val totalSubLessons = unit.subLessons.size
                    "$completedSubLessons/$totalSubLessons"
                } else {
                    unit.progress
                }
                
                Text(
                    text = progressText,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }
            
            Icon(
                imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = if (isExpanded) "Collapse" else "Expand",
                tint = Color(0xFF4CAF50)
            )
        }
        
        // Sub-lessons
        if (isExpanded) {
            unit.subLessons.forEach { subLesson ->
                // Kiểm tra xem subLesson đã hoàn thành chưa dựa trên userProgress
                val isCompleted = userProgress?.completedSubLessons?.contains(subLesson.id) 
                    ?: subLesson.isCompleted
                
                // Cập nhật trạng thái hoàn thành của subLesson
                val updatedSubLesson = subLesson.copy(isCompleted = isCompleted)
                
                ModernSubLessonItem(
                    subLesson = updatedSubLesson,
                    onClick = { onSubLessonClick(subLesson) }
                )
            }
        }
    }
}

@Composable
fun ModernSubLessonItem(
    subLesson: SubLesson,
    onClick: () -> Unit,
    userProgress: UserProgress? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(start = 56.dp, end = 16.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Icon based on type
        Icon(
            imageVector = when (subLesson.type) {
                "Video" -> Icons.Default.PlayCircle
                "Practice" -> Icons.Default.Edit
                "Quiz" -> Icons.Default.QuestionAnswer
                else -> Icons.Default.Description
            },
            contentDescription = null,
            tint = if (subLesson.isCompleted) Color(0xFF4CAF50) else Color.Gray,
            modifier = Modifier.size(20.dp)
        )
        
        Spacer(modifier = Modifier.width(12.dp))
        
        Text(
            text = subLesson.title,
            style = MaterialTheme.typography.bodyMedium,
            color = if (subLesson.isCompleted) Color(0xFF4CAF50) else Color.DarkGray,
            modifier = Modifier.weight(1f)
        )
        
        if (subLesson.isCompleted) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = "Completed",
                tint = Color(0xFF4CAF50),
                modifier = Modifier.size(20.dp)
            )
        } else {
            Icon(
                imageVector = Icons.Default.ArrowForward,
                contentDescription = "Start",
                tint = Color(0xFF4CAF50),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
fun ProgressTab(userProgress: List<UserProgress>, lessons: List<Lesson>) {
    // Calculate overall progress
    val totalLessons = lessons.size
    val completedLessons = userProgress.flatMap { it.completedLessons }.distinct().size
    val progressPercentage = if (totalLessons > 0) {
        (completedLessons.toFloat() / totalLessons.toFloat()) * 100
    } else 0f

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Progress overview
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        "Tổng quan tiến độ",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    LinearProgressIndicator(
                        progress = { progressPercentage / 100 },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp),
                        color = Color(0xFF4CAF50),
                        trackColor = Color(0xFFE0E0E0)
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "Hoàn thành: $completedLessons/$totalLessons bài học",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        
                        Text(
                            "${progressPercentage.toInt()}%",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF4CAF50)
                        )
                    }
                }
            }
        }
        
        // Section title
        item {
            Text(
                "Chi tiết tiến độ bài học",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }
        
        // Lesson progress items
        items(lessons) { lesson ->
            val isCompleted = userProgress.any { progress ->
                progress.completedLessons.contains(lesson.id)
            }
            
            LessonProgressItem(
                lesson = lesson,
                isCompleted = isCompleted
            )
        }
        
        // Add some space at the bottom
        item {
            Spacer(modifier = Modifier.height(80.dp))
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
fun LessonProgressItem(
    lesson: Lesson,
    isCompleted: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .background(
                    color = if (isCompleted) Color(0xFF4CAF50) else Color(0xFFE0E0E0),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            if (isCompleted) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Completed",
                    tint = Color.White
                )
            }
        }
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Text(
            text = lesson.stepTitle,
            style = MaterialTheme.typography.bodyMedium,
            color = if (isCompleted) Color(0xFF4CAF50) else Color.DarkGray
        )
    }
}

@Composable
fun MaterialsTab(course: Course?, navController: NavController, userEmail: String) {
    if (course == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.LightGray)
        )
        return
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Tài liệu khóa học",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        // Danh sách tài liệu với liên kết đến FlashcardScreen
        val materials = listOf(
            Pair("Bảng chữ Hiragana", "hiragana"),
            Pair("Bảng chữ Katakana", "katakana"),
            Pair("Kanji cơ bản N5", "kanji"),
            Pair("Từ vựng N5", "vocabulary")
        )
        
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(materials) { (title, type) ->
                MaterialCard(
                    title = title,
                    onClick = {
                        // Điều hướng đến FlashcardScreen với tab được chọn
                        navController.navigate("vocabulary/$userEmail?tab=$type") {
                            launchSingleTop = true
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun MaterialCard(title: String, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.MenuBook,
                contentDescription = null,
                tint = Color(0xFF4CAF50),
                modifier = Modifier.size(24.dp)
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            
            Spacer(modifier = Modifier.weight(1f))
            
            Icon(
                imageVector = Icons.Default.ArrowForward,
                contentDescription = "Mở",
                tint = Color(0xFF4CAF50),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
// Hàm push Firestore
suspend fun addLessonToCourse(courseId: String, lesson: Lesson) {
    val firestore = FirebaseFirestore.getInstance()
    val lessonsCollection = firestore.collection("lessons")

    val generatedId = lesson.id.ifEmpty { lessonsCollection.document().id }
    val lessonWithId = lesson.copy(id = generatedId, courseId = courseId)

    // Sinh ID cho SubLesson
    val finalLesson = generateSubLessonIds(lessonWithId)

    // Push lesson lên Firestore
    lessonsCollection.document(generatedId).set(finalLesson).await()
}

fun generateSubLessonIds(lesson: Lesson): Lesson {
    val firestore = FirebaseFirestore.getInstance()

    val updatedUnits = lesson.units.map { unit ->
        val updatedSubLessons = unit.subLessons.map { subLesson ->
            val generatedId = subLesson.id.ifEmpty { firestore.collection("sublessons").document().id }
            subLesson.copy(id = generatedId)
        }
        unit.copy(subLessons = updatedSubLessons)
    }

    return lesson.copy(units = updatedUnits)
}

fun createSampleLessons(): List<Lesson> {
    return listOf(
        Lesson(
            id = "1",
            courseId = "1",
            step = 1,
            stepTitle = "Bước 1: Giới thiệu về Hiragana",
            overview = """
            Tổng quan bảng chữ Hiragana.

            2/2 | 3:20 minutes

            Cùng tìm hiểu nguồn gốc và vai trò của Hiragana trong tiếng Nhật.
        """.trimIndent(),
            totalUnits = 1,
            completedUnits = 0,
            units = listOf(
                UnitItem(
                    unitTitle = "Giới thiệu Hiragana",
                    progress = "0/2",
                    subLessons = listOf(
                        SubLesson(id = "1-1", title = "Hiragana là gì?", type = "Video", isCompleted = false),
                        SubLesson(id = "1-2", title = "Tại sao nên học Hiragana?", type = "Practice", isCompleted = false)
                    )
                )
            )
        ),
        Lesson(
            id = "2",
            courseId = "1",
            step = 2,
            stepTitle = "Bước 2: Học hàng あ",
            overview = """
            Học các chữ cái: あ, い, う, え, お

            3/3 | 5:40 minutes

            Luyện cách viết và đọc.
        """.trimIndent(),
            totalUnits = 1,
            completedUnits = 0,
            units = listOf(
                UnitItem(
                    unitTitle = "Hàng あ",
                    progress = "0/3",
                    subLessons = listOf(
                        SubLesson(id = "2-1", title = "Phát âm và cách viết あ-い-う-え-お", type = "Video", isCompleted = false),
                        SubLesson(id = "2-2", title = "Từ vựng với あ hàng", type = "Practice", isCompleted = false),
                        SubLesson(id = "2-3", title = "Luyện viết hàng あ", type = "Practice", isCompleted = false)
                    )
                )
            )
        ),
        Lesson(
            id = "3",
            courseId = "1",
            step = 3,
            stepTitle = "Bước 3: Học hàng か",
            overview = """
            Học các chữ cái: か, き, く, け, こ

            3/3 | 5:50 minutes

            Phát âm và luyện viết với ví dụ từ vựng.
        """.trimIndent(),
            totalUnits = 1,
            completedUnits = 0,
            units = listOf(
                UnitItem(
                    unitTitle = "Hàng か",
                    progress = "0/3",
                    subLessons = listOf(
                        SubLesson(id = "3-1", title = "Phát âm và cách viết か-き-く-け-こ", type = "Video", isCompleted = false),
                        SubLesson(id = "3-2", title = "Từ vựng với か hàng", type = "Practice", isCompleted = false),
                        SubLesson(id = "3-3", title = "Luyện viết hàng か", type = "Practice", isCompleted = false)
                    )
                )
            )
        ),
        Lesson(
            id = "4",
            courseId = "1",
            step = 4,
            stepTitle = "Bước 4: Học hàng さ",
            overview = """
            Học các chữ cái: さ, し, す, せ, そ

            3/3 | 6:00 minutes

            Tập phát âm và luyện viết chữ rõ ràng.
        """.trimIndent(),
            totalUnits = 1,
            completedUnits = 0,
            units = listOf(
                UnitItem(
                    unitTitle = "Hàng さ",
                    progress = "0/3",
                    subLessons = listOf(
                        SubLesson(id = "4-1", title = "Phát âm và cách viết さ-し-す-せ-そ", type = "Video", isCompleted = false),
                        SubLesson(id = "4-2", title = "Từ vựng với さ hàng", type = "Practice", isCompleted = false),
                        SubLesson(id = "4-3", title = "Luyện viết hàng さ", type = "Practice", isCompleted = false)
                    )
                )
            )
        ),
        Lesson(
            id = "5",
            courseId = "1",
            step = 5,
            stepTitle = "Bước 5: Học hàng た",
            overview = """
            Học các chữ cái: た, ち, つ, て, と

            3/3 | 6:05 minutes

            Hướng dẫn đọc và luyện từ vựng ứng dụng.
        """.trimIndent(),
            totalUnits = 1,
            completedUnits = 0,
            units = listOf(
                UnitItem(
                    unitTitle = "Hàng た",
                    progress = "0/3",
                    subLessons = listOf(
                        SubLesson(id = "5-1", title = "Phát âm và cách viết た-ち-つ-て-と", type = "Video", isCompleted = false),
                        SubLesson(id = "5-2", title = "Từ vựng với た hàng", type = "Practice", isCompleted = false),
                        SubLesson(id = "5-3", title = "Luyện viết hàng た", type = "Practice", isCompleted = false)
                    )
                )
            )
        ),
        Lesson(
            id = "6",
            courseId = "1",
            step = 6,
            stepTitle = "Bước 6: Học hàng な",
            overview = """
            Học hàng な: な, に, ぬ, ね, の

            3/3 | 6:10 minutes

            Tập phát âm và từ vựng ví dụ.
        """.trimIndent(),
            totalUnits = 1,
            completedUnits = 0,
            units = listOf(
                UnitItem(
                    unitTitle = "Hàng な",
                    progress = "0/3",
                    subLessons = listOf(
                        SubLesson(id = "6-1", title = "Phát âm và cách viết な-に-ぬ-ね-の", type = "Video", isCompleted = false),
                        SubLesson(id = "6-2", title = "Từ vựng với な hàng", type = "Practice", isCompleted = false),
                        SubLesson(id = "6-3", title = "Luyện viết hàng な", type = "Practice", isCompleted = false)
                    )
                )
            )
        ),
        Lesson(
            id = "7",
            courseId = "1",
            step = 7,
            stepTitle = "Bước 7: Học hàng は",
            overview = """
            Học hàng は: は, ひ, ふ, へ, ほ

            3/3 | 6:45 minutes

            Luyện phát âm, ví dụ từ vựng.
        """.trimIndent(),
            totalUnits = 1,
            completedUnits = 0,
            units = listOf(
                UnitItem(
                    unitTitle = "Hàng は",
                    progress = "0/3",
                    subLessons = listOf(
                        SubLesson(id = "7-1", title = "Phát âm và cách viết は-ひ-ふ-へ-ほ", type = "Video", isCompleted = false),
                        SubLesson(id = "7-2", title = "Từ vựng với は hàng", type = "Practice", isCompleted = false),
                        SubLesson(id = "7-3", title = "Luyện viết hàng は", type = "Practice", isCompleted = false)
                    )
                )
            )
        ),
        Lesson(
            id = "8",
            courseId = "1",
            step = 8,
            stepTitle = "Bước 8: Học hàng ま",
            overview = """
            Học hàng ま: ま, み, む, め, も

            3/3 | 6:20 minutes

            Phát âm rõ ràng và luyện từ vựng thực hành.
        """.trimIndent(),
            totalUnits = 1,
            completedUnits = 0,
            units = listOf(
                UnitItem(
                    unitTitle = "Hàng ま",
                    progress = "0/3",
                    subLessons = listOf(
                        SubLesson(id = "8-1", title = "Phát âm và cách viết ま-み-む-め-も", type = "Video", isCompleted = false),
                        SubLesson(id = "8-2", title = "Từ vựng với ま hàng", type = "Practice", isCompleted = false),
                        SubLesson(id = "8-3", title = "Luyện viết hàng ま", type = "Practice", isCompleted = false)
                    )
                )
            )
        ),
        Lesson(
            id = "9",
            courseId = "1",
            step = 9,
            stepTitle = "Bước 9: Học hàng や",
            overview = """
            Học hàng や: や, ゆ, よ

            3/3 | 5:00 minutes

            Ít ký tự, dễ học nhanh và luyện viết.
        """.trimIndent(),
            totalUnits = 1,
            completedUnits = 0,
            units = listOf(
                UnitItem(
                    unitTitle = "Hàng や",
                    progress = "0/3",
                    subLessons = listOf(
                        SubLesson(id = "9-1", title = "Phát âm và cách viết や-ゆ-よ", type = "Video", isCompleted = false),
                        SubLesson(id = "9-2", title = "Từ vựng với や hàng", type = "Practice", isCompleted = false),
                        SubLesson(id = "9-3", title = "Luyện viết hàng や", type = "Practice", isCompleted = false)
                    )
                )
            )
        ),
        Lesson(
            id = "10",
            courseId = "1",
            step = 10,
            stepTitle = "Bước 10: Học hàng ら",
            overview = """
            Học hàng ら: ら, り, る, れ, ろ

            3/3 | 6:15 minutes

            Thực hành phát âm chuẩn và ví dụ cơ bản.
        """.trimIndent(),
            totalUnits = 1,
            completedUnits = 0,
            units = listOf(
                UnitItem(
                    unitTitle = "Hàng ら",
                    progress = "0/3",
                    subLessons = listOf(
                        SubLesson(id = "10-1", title = "Phát âm và cách viết ら-り-る-れ-ろ", type = "Video", isCompleted = false),
                        SubLesson(id = "10-2", title = "Từ vựng với ら hàng", type = "Practice", isCompleted = false),
                        SubLesson(id = "10-3", title = "Luyện viết hàng ら", type = "Practice", isCompleted = false)
                    )
                )
            )
        ),
        Lesson(
            id = "11",
            courseId = "1",
            step = 11,
            stepTitle = "Bước 11: Học hàng わ",
            overview = """
            Học hàng わ: わ, を, ん

            3/3 | 5:40 minutes

            Ký tự đặc biệt và âm kết thúc câu.
        """.trimIndent(),
            totalUnits = 1,
            completedUnits = 0,
            units = listOf(
                UnitItem(
                    unitTitle = "Hàng わ",
                    progress = "0/3",
                    subLessons = listOf(
                        SubLesson(id = "11-1", title = "Phát âm và cách viết わ-を-ん", type = "Video", isCompleted = false),
                        SubLesson(id = "11-2", title = "Từ vựng với わ hàng", type = "Practice", isCompleted = false),
                        SubLesson(id = "11-3", title = "Luyện viết hàng わ", type = "Practice", isCompleted = false)
                    )
                )
            )
        ),
        Lesson(
            id = "12",
            courseId = "1",
            step = 12,
            stepTitle = "Bước 12: Luyện tập tổng hợp",
            overview = """
            Tổng hợp toàn bộ Hiragana đã học.

            2/2 | 8:00 minutes

            Nghe - đọc - viết ứng dụng thực tế.
        """.trimIndent(),
            totalUnits = 1,
            completedUnits = 0,
            units = listOf(
                UnitItem(
                    unitTitle = "Tổng ôn tập",
                    progress = "0/2",
                    subLessons = listOf(
                        SubLesson(id = "12-2", title = "Kiểm tra từ vựng & chữ viết", type = "Practice", isCompleted = false)
                    )
                )
            )
        ),
        Lesson(
            id = "13",
            courseId = "1",
            step = 13,
            stepTitle = "Bước 13: Thử thách mini game",
            overview = """
            Game hóa kiểm tra kiến thức Hiragana.

            1/1 | 5:30 minutes

            Kết hợp âm thanh, hình ảnh và phản xạ.
        """.trimIndent(),
            totalUnits = 1,
            completedUnits = 0,
            units = listOf(
                UnitItem(
                    unitTitle = "Mini game luyện phản xạ",
                    progress = "0/1",
                    subLessons = listOf(
                        SubLesson(id = "13-2", title = "Game phản xạ chữ Hiragana", type = "Practice", isCompleted = false)
                    )
                )
            )
        ),
        Lesson(
            id = "14",
            courseId = "1",
            step = 14,
            stepTitle = "Bước 14: Bài kiểm tra tổng kết",
            overview = """
            Kiểm tra đánh giá tổng hợp Hiragana.

            1/1 | 10:00 minutes

            Làm bài test và nhận kết quả.
        """.trimIndent(),
            totalUnits = 1,
            completedUnits = 0,
            units = listOf(
                UnitItem(
                    unitTitle = "Bài kiểm tra tổng hợp",
                    progress = "0/1",
                    subLessons = listOf(
                        SubLesson(id = "14-2", title = "Làm bài kiểm tra tổng kết", type = "Practice", isCompleted = false)
                    )
                )
            )
        ),
        // Katakana course
//                Lesson(
//                id = "k1",
//        courseId = "2",
//        step = 1,
//        stepTitle = "Bước 1: Giới thiệu về Katakana",
//        overview = """
//            Tổng quan bảng chữ Katakana.
//
//            2/2 | 3:20 minutes
//
//            Cùng tìm hiểu nguồn gốc và vai trò của Katakana trong tiếng Nhật.
//        """.trimIndent(),
//        totalUnits = 1,
//        completedUnits = 0,
//        units = listOf(
//            UnitItem(
//                unitTitle = "Giới thiệu Katakana",
//                progress = "0/2",
//                subLessons = listOf(
//                    SubLesson(id = "k1-1", title = "Katakana là gì?", type = "Video", isCompleted = false),
//                    SubLesson(id = "k1-2", title = "Tại sao nên học Katakana?", type = "Practice", isCompleted = false)
//                )
//            )
//        )
//    ),
//    Lesson(
//        id = "k2",
//        courseId = "2",
//        step = 2,
//        stepTitle = "Bước 2: Học hàng ア",
//        overview = """
//            Học các chữ cái: ア, イ, ウ, エ, オ
//
//            3/3 | 5:40 minutes
//
//            Luyện cách viết và đọc.
//        """.trimIndent(),
//        totalUnits = 1,
//        completedUnits = 0,
//        units = listOf(
//            UnitItem(
//                unitTitle = "Hàng ア",
//                progress = "0/3",
//                subLessons = listOf(
//                    SubLesson(id = "k2-1", title = "Phát âm và cách viết ア-イ-ウ-エ-オ", type = "Video", isCompleted = false),
//                    SubLesson(id = "k2-2", title = "Từ vựng với ア hàng", type = "Practice", isCompleted = false),
//                    SubLesson(id = "k2-3", title = "Luyện viết hàng ア", type = "Practice", isCompleted = false)
//                )
//            )
//        )
//    ),
//    Lesson(
//        id = "k3",
//        courseId = "2",
//        step = 3,
//        stepTitle = "Bước 3: Học hàng カ",
//        overview = """
//            Học các chữ cái: カ, キ, ク, ケ, コ
//
//            3/3 | 5:50 minutes
//
//            Phát âm và luyện viết với ví dụ từ vựng.
//        """.trimIndent(),
//        totalUnits = 1,
//        completedUnits = 0,
//        units = listOf(
//            UnitItem(
//                unitTitle = "Hàng カ",
//                progress = "0/3",
//                subLessons = listOf(
//                    SubLesson(id = "k3-1", title = "Phát âm và cách viết カ-キ-ク-ケ-コ", type = "Video", isCompleted = false),
//                    SubLesson(id = "k3-2", title = "Từ vựng với カ hàng", type = "Practice", isCompleted = false),
//                    SubLesson(id = "k3-3", title = "Luyện viết hàng カ", type = "Practice", isCompleted = false)
//                )
//            )
//        )
//    ),
//    Lesson(
//        id = "k4",
//        courseId = "2",
//        step = 4,
//        stepTitle = "Bước 4: Học hàng サ",
//        overview = """
//            Học các chữ cái: サ, シ, ス, セ, ソ
//
//            3/3 | 6:00 minutes
//
//            Tập phát âm và luyện viết chữ rõ ràng.
//        """.trimIndent(),
//        totalUnits = 1,
//        completedUnits = 0,
//        units = listOf(
//            UnitItem(
//                unitTitle = "Hàng サ",
//                progress = "0/3",
//                subLessons = listOf(
//                    SubLesson(id = "k4-1", title = "Phát âm và cách viết サ-シ-ス-セ-ソ", type = "Video", isCompleted = false),
//                    SubLesson(id = "k4-2", title = "Từ vựng với サ hàng", type = "Practice", isCompleted = false),
//                    SubLesson(id = "k4-3", title = "Luyện viết hàng サ", type = "Practice", isCompleted = false)
//                )
//            )
//        )
//    ),
//    Lesson(
//        id = "k5",
//        courseId = "2",
//        step = 5,
//        stepTitle = "Bước 5: Học hàng タ",
//        overview = """
//            Học các chữ cái: タ, チ, ツ, テ, ト
//
//            3/3 | 6:05 minutes
//
//            Hướng dẫn đọc và luyện từ vựng ứng dụng.
//        """.trimIndent(),
//        totalUnits = 1,
//        completedUnits = 0,
//        units = listOf(
//            UnitItem(
//                unitTitle = "Hàng タ",
//                progress = "0/3",
//                subLessons = listOf(
//                    SubLesson(id = "k5-1", title = "Phát âm và cách viết タ-チ-ツ-テ-ト", type = "Video", isCompleted = false),
//                    SubLesson(id = "k5-2", title = "Từ vựng với タ hàng", type = "Practice", isCompleted = false),
//                    SubLesson(id = "k5-3", title = "Luyện viết hàng タ", type = "Practice", isCompleted = false)
//                )
//            )
//        )
//    ),
//    Lesson(
//        id = "k6",
//        courseId = "2",
//        step = 6,
//        stepTitle = "Bước 6: Học hàng ナ",
//        overview = """
//            Học các chữ cái: ナ, ニ, ヌ, ネ, ノ
//
//            3/3 | 6:10 minutes
//
//            Phát âm và luyện viết với ví dụ từ vựng.
//        """.trimIndent(),
//        totalUnits = 1,
//        completedUnits = 0,
//        units = listOf(
//            UnitItem(
//                unitTitle = "Hàng ナ",
//                progress = "0/3",
//                subLessons = listOf(
//                    SubLesson(id = "k6-1", title = "Phát âm và cách viết ナ-ニ-ヌ-ネ-ノ", type = "Video", isCompleted = false),
//                    SubLesson(id = "k6-2", title = "Từ vựng với ナ hàng", type = "Practice", isCompleted = false),
//                    SubLesson(id = "k6-3", title = "Luyện viết hàng ナ", type = "Practice", isCompleted = false)
//                )
//            )
//        )
//    ),
//    Lesson(
//        id = "k7",
//        courseId = "2",
//        step = 7,
//        stepTitle = "Bước 7: Học hàng ハ",
//        overview = """
//            Học các chữ cái: ハ, ヒ, フ, ヘ, ホ
//
//            3/3 | 6:15 minutes
//
//            Phát âm và luyện viết với ví dụ từ vựng.
//        """.trimIndent(),
//        totalUnits = 1,
//        completedUnits = 0,
//        units = listOf(
//            UnitItem(
//                unitTitle = "Hàng ハ",
//                progress = "0/3",
//                subLessons = listOf(
//                    SubLesson(id = "k7-1", title = "Phát âm và cách viết ハ-ヒ-フ-ヘ-ホ", type = "Video", isCompleted = false),
//                    SubLesson(id = "k7-2", title = "Từ vựng với ハ hàng", type = "Practice", isCompleted = false),
//                    SubLesson(id = "k7-3", title = "Luyện viết hàng ハ", type = "Practice", isCompleted = false)
//                )
//            )
//        )
//    ),
//    Lesson(
//        id = "k8",
//        courseId = "2",
//        step = 8,
//        stepTitle = "Bước 8: Học hàng マ",
//        overview = """
//            Học hàng マ: マ, ミ, ム, メ, モ
//
//            3/3 | 6:20 minutes
//
//            Phát âm rõ ràng và luyện từ vựng thực hành.
//        """.trimIndent(),
//        totalUnits = 1,
//        completedUnits = 0,
//        units = listOf(
//            UnitItem(
//                unitTitle = "Hàng マ",
//                progress = "0/3",
//                subLessons = listOf(
//                    SubLesson(id = "k8-1", title = "Phát âm và cách viết マ-ミ-ム-メ-モ", type = "Video", isCompleted = false),
//                    SubLesson(id = "k8-2", title = "Từ vựng với マ hàng", type = "Practice", isCompleted = false),
//                    SubLesson(id = "k8-3", title = "Luyện viết hàng マ", type = "Practice", isCompleted = false)
//                )
//            )
//        )
//    ),
//    Lesson(
//        id = "k9",
//        courseId = "2",
//        step = 9,
//        stepTitle = "Bước 9: Học hàng ヤ",
//        overview = """
//            Học hàng ヤ: ヤ, ユ, ヨ
//
//            3/3 | 5:30 minutes
//
//            Phát âm và luyện viết với ví dụ từ vựng.
//        """.trimIndent(),
//        totalUnits = 1,
//        completedUnits = 0,
//        units = listOf(
//            UnitItem(
//                unitTitle = "Hàng ヤ",
//                progress = "0/3",
//                subLessons = listOf(
//                    SubLesson(id = "k9-1", title = "Phát âm và cách viết ヤ-ユ-ヨ", type = "Video", isCompleted = false),
//                    SubLesson(id = "k9-2", title = "Từ vựng với ヤ hàng", type = "Practice", isCompleted = false),
//                    SubLesson(id = "k9-3", title = "Luyện viết hàng ヤ", type = "Practice", isCompleted = false)
//                )
//            )
//        )
//    ),
//    Lesson(
//        id = "k10",
//        courseId = "2",
//        step = 10,
//        stepTitle = "Bước 10: Học hàng ラ",
//        overview = """
//            Học hàng ラ: ラ, リ, ル, レ, ロ
//
//            3/3 | 6:15 minutes
//
//            Thực hành phát âm chuẩn và ví dụ cơ bản.
//        """.trimIndent(),
//        totalUnits = 1,
//        completedUnits = 0,
//        units = listOf(
//            UnitItem(
//                unitTitle = "Hàng ラ",
//                progress = "0/3",
//                subLessons = listOf(
//                    SubLesson(id = "k10-1", title = "Phát âm và cách viết ラ-リ-ル-レ-ロ", type = "Video", isCompleted = false),
//                    SubLesson(id = "k10-2", title = "Từ vựng với ラ hàng", type = "Practice", isCompleted = false),
//                    SubLesson(id = "k10-3", title = "Luyện viết hàng ラ", type = "Practice", isCompleted = false)
//                )
//            )
//        )
//    ),
//    Lesson(
//        id = "k11",
//        courseId = "2",
//        step = 11,
//        stepTitle = "Bước 11: Học hàng ワ",
//        overview = """
//            Học hàng ワ: ワ, ヲ, ン
//
//            3/3 | 5:45 minutes
//
//            Phát âm và luyện viết với ví dụ từ vựng.
//        """.trimIndent(),
//        totalUnits = 1,
//        completedUnits = 0,
//        units = listOf(
//            UnitItem(
//                unitTitle = "Hàng ワ",
//                progress = "0/3",
//                subLessons = listOf(
//                    SubLesson(id = "k11-1", title = "Phát âm và cách viết ワ-ヲ-ン", type = "Video", isCompleted = false),
//                    SubLesson(id = "k11-2", title = "Từ vựng với ワ hàng", type = "Practice", isCompleted = false),
//                    SubLesson(id = "k11-3", title = "Luyện viết hàng ワ", type = "Practice", isCompleted = false)
//                )
//            )
//        )
//    ),
//    Lesson(
//        id = "k12",
//        courseId = "2",
//        step = 12,
//        stepTitle = "Bước 12: Katakana đặc biệt",
//        overview = """
//            Học các ký tự đặc biệt và kết hợp.
//
//            3/3 | 7:00 minutes
//
//            Phát âm và luyện viết với ví dụ từ vựng.
//        """.trimIndent(),
//        totalUnits = 1,
//        completedUnits = 0,
//        units = listOf(
//            UnitItem(
//                unitTitle = "Katakana đặc biệt",
//                progress = "0/3",
//                subLessons = listOf(
//                    SubLesson(id = "k12-1", title = "Katakana với dấu dakuten và handakuten", type = "Video", isCompleted = false),
//                    SubLesson(id = "k12-2", title = "Katakana kết hợp", type = "Practice", isCompleted = false),
//                    SubLesson(id = "k12-3", title = "Luyện viết Katakana đặc biệt", type = "Practice", isCompleted = false)
//                )
//            )
//        )
//    ),
//    Lesson(
//        id = "k13",
//        courseId = "2",
//        step = 13,
//        stepTitle = "Bước 13: Từ vựng ngoại lai",
//        overview = """
//            Học cách viết từ vựng ngoại lai bằng Katakana.
//
//            3/3 | 7:30 minutes
//
//            Phát âm và luyện viết với ví dụ từ vựng.
//        """.trimIndent(),
//        totalUnits = 1,
//        completedUnits = 0,
//        units = listOf(
//            UnitItem(
//                unitTitle = "Từ vựng ngoại lai",
//                progress = "0/3",
//                subLessons = listOf(
//                    SubLesson(id = "k13-1", title = "Cách viết từ vựng ngoại lai", type = "Video", isCompleted = false),
//                    SubLesson(id = "k13-2", title = "Từ vựng ngoại lai thông dụng", type = "Practice", isCompleted = false),
//                    SubLesson(id = "k13-3", title = "Luyện viết từ vựng ngoại lai", type = "Practice", isCompleted = false)
//                )
//            )
//        )
//    ),
//    Lesson(
//        id = "k14",
//        courseId = "2",
//        step = 14,
//        stepTitle = "Bước 14: Bài kiểm tra tổng kết",
//        overview = """
//            Kiểm tra đánh giá tổng hợp Katakana.
//
//            1/1 | 10:00 minutes
//
//            Làm bài test và nhận kết quả.
//        """.trimIndent(),
//        totalUnits = 1,
//        completedUnits = 0,
//        units = listOf(
//            UnitItem(
//                unitTitle = "Bài kiểm tra tổng hợp",
//                progress = "0/1",
//                subLessons = listOf(
//                    SubLesson(id = "k14-2", title = "Làm bài kiểm tra tổng kết", type = "Practice", isCompleted = false)
//                )
//            )
//        )
//    )

    )
}


fun addExercisesToLesson(lesson: Lesson) {
    val db = FirebaseFirestore.getInstance()
    val exercisesRef = db.collection("lessons").document(lesson.id).collection("exercises")

    // Bước 1: Xóa toàn bộ exercise cũ
    exercisesRef.get().addOnSuccessListener { querySnapshot ->
        val batch = db.batch()

        // Duyệt qua tất cả các tài liệu trong collection để xóa chúng
        for (doc in querySnapshot.documents) {
            batch.delete(doc.reference)
        }

        // Sau khi batch delete xong, commit để thực thi xóa
        batch.commit().addOnSuccessListener {
            Log.d("LessonsScreen", "All old exercises in lesson '${lesson.id}' deleted.")

            // Bước 2: Thêm mới exercise sau khi đã xóa xong
            lesson.units.forEach { unitItem ->
                unitItem.subLessons.forEach { subLesson ->
                    // Lọc exercise theo type của subLesson (ví dụ: "Video" hoặc "Quiz")
                    val exercises = generateExercisesForSubLesson(subLesson, subLesson.id)

                    exercises.forEach { exercise ->
                        // Kiểm tra nếu exercise type phù hợp với subLesson type và subLessonId khớp
                        if (exercise.type == ExerciseType.from(subLesson.type) && exercise.subLessonId == subLesson.id) {
                            // Tạo exerciseData mà không cần id vì Firestore sẽ tự tạo ID
                            val exerciseData = hashMapOf(
                                "subLessonId" to subLesson.id,
                                "question" to exercise.question,
                                "answer" to exercise.answer,
                                "type" to exercise.type?.toString(),
                                "options" to exercise.options,
                                "videoUrl" to exercise.videoUrl,
                                "audioUrl" to exercise.audioUrl,
                                "imageUrl" to exercise.imageUrl,
                                "title" to exercise.title,
                                "explanation" to exercise.explanation,
                                "romanji" to exercise.romanji,
                                "kana" to exercise.kana
                            )

                            // Thêm exercise vào Firestore
                            exercisesRef.add(exerciseData)
                                .addOnSuccessListener { documentReference ->
                                    // Tài liệu đã được thêm vào Firestore, lấy document ID
                                    val exerciseId = documentReference.id
                                    Log.d("LessonsScreen", "Added exercise to subLesson: ${subLesson.id} with ID: $exerciseId")
                                }
                                .addOnFailureListener { e ->
                                    Log.e("LessonsScreen", "Failed to add exercise: ${e.message}")
                                }
                        }
                    }
                }
            }
        }.addOnFailureListener { e ->
            Log.e("LessonsScreen", "Failed to delete old exercises: ${e.message}")
        }
    }
}



fun generateExercisesForSubLesson(subLesson: SubLesson, subLessonId: String): List<Exercise> {
    return when (subLesson.type) {
        "Video" -> listOf(
            Exercise(
                subLessonId = "1-1",
                title = "Giới thiệu bảng chữ cái Hiragana",
                videoUrl = "https://drive.google.com/uc?id=1G2tiFAR1HRFKsNT8fLqri1Ucxim7GNo2&export=download",
                type = ExerciseType.VIDEO,
                explanation = "I. Giới thiệu các loại chữ trong tiếng Nhật➤Trong tiếng Nhật có 3 loại chữ:\n\na. Kanji (chữ Hán): 日本\n- Chữ Kanji du nhập từ Trung Quốc vào Nhật Bản từ thế kỷ thứ 4.\n\nb. Hiragana (chữ mềm): にほん\n- Hiragana được tạo từ Kanji, dùng viết trợ từ, từ thuần Nhật.\n- VD: 世 ⇒ せ.\n\nc. Katakana (chữ cứng): 二ホン\n- Katakana dùng cho từ ngoại lai, tên nước, tên riêng.\n- VD: Orange ⇒ オレンジ.➤II. Giới thiệu bảng chữ cái Hiragana➤- Bảng Hiragana gồm 46 chữ cái.\n- Hàng あ: あ(a), い(i), う(u), え(e), お(o).\n- Hàng か: か(ka), き(ki), く(ku), け(ke), こ(ko).\n- Hàng さ: さ(sa), し(shi), す(su), せ(se), そ(so).\n- Hàng た: た(ta), ち(chi), つ(tsu), て(te), と(to).\n- Hàng な: な(na), に(ni), ぬ(nu), ね(ne), の(no).\n- Hàng は: は(ha), ひ(hi), ふ(fu), へ(he), ほ(ho).\n- Hàng ま: ま(ma), み(mi), む(mu), め(me), も(mo).\n- Hàng や: や(ya), ゆ(yu), よ(yo).\n- Hàng ら: ら(ra), り(ri), る(ru), れ(re), ろ(ro).\n- Hàng わ: わ(wa), を(wo), ん(n)."
            ),
            Exercise(
                subLessonId = "2-1",
                title = "Phát âm và cách viết あ-い-う-え-お",
                videoUrl = "https://drive.google.com/uc?id=1Tkj5cMdfP2GtR60Usfbf0bLqNlRjzz6w&export=download",
                type = ExerciseType.VIDEO,
                explanation = "I. Hàng あ trong Hiragana➤Hàng あ gồm 5 chữ cái cơ bản:\n\n- あ (a): Phát âm như 'a' trong 'ba'\n- い (i): Phát âm như 'i' trong 'bi'\n- う (u): Phát âm như 'u' trong 'bu'\n- え (e): Phát âm như 'e' trong 'bet'\n- お (o): Phát âm như 'o' trong 'go'➤II. Cách viết hàng あ➤1. あ (a):\n- Bắt đầu từ trên, viết một đường cong sang trái\n- Tiếp tục với một đường cong sang phải\n- Kết thúc với một nét nhỏ ở dưới\n\n2. い (i):\n- Viết một nét ngang từ trái sang phải\n- Sau đó viết một đường cong dài xuống dưới\n\n3. う (u):\n- Bắt đầu từ trên, viết một đường cong xuống\n- Tiếp tục với một đường cong sang phải\n\n4. え (e):\n- Viết một nét ngang ngắn ở trên\n- Tiếp theo là một nét ngang dài hơn ở giữa\n- Kết thúc với một đường cong xuống dưới\n\n5. お (o):\n- Viết một đường cong từ trên xuống bên trái\n- Tiếp theo là một đường cong từ trên xuống bên phải\n- Kết thúc với một nét ngang ở giữa➤III. Từ vựng với hàng あ➤- あい (ai): Tình yêu\n- いえ (ie): Nhà\n- うえ (ue): Trên\n- えき (eki): Nhà ga\n- おと (oto): Âm thanh"
            ),
            Exercise(
                subLessonId = "3-1",
                title = "Phát âm và cách viết か-き-く-け-こ",
                videoUrl = "https://drive.google.com/uc?id=1Fjp7jImlYbvmKvEwxIrAqdbzygsqYI9Q&export=download",
                type = ExerciseType.VIDEO,
                explanation = "I. Hàng か trong Hiragana➤Hàng か gồm 5 chữ cái:\n\n- か (ka): Phát âm như 'ka' trong 'karate'\n- き (ki): Phát âm như 'ki' trong 'key'\n- く (ku): Phát âm như 'ku' trong 'cool'\n- け (ke): Phát âm như 'ke' trong 'kettle'\n- こ (ko): Phát âm như 'ko' trong 'code'➤II. Cách viết hàng か➤1. か (ka):\n- Viết một nét ngang từ trái sang phải\n- Tiếp theo là một đường cong xuống và sang phải\n\n2. き (ki):\n- Viết một nét ngang từ trái sang phải\n- Tiếp theo là một nét đứng xuống\n- Thêm một nét ngang cắt ngang nét đứng\n- Kết thúc với một đường cong ở dưới\n\n3. く (ku):\n- Viết một góc vuông, bắt đầu từ trên xuống rồi sang phải\n\n4. け (ke):\n- Viết một nét ngang từ trái sang phải\n- Tiếp theo là một nét đứng xuống\n- Thêm một nét ngang ngắn ở giữa\n- Kết thúc với một đường cong ở dưới\n\n5. こ (ko):\n- Viết hai nét ngang song song\n- Nối chúng bằng một nét đứng ở bên phải➤III. Từ vựng với hàng か➤- かさ (kasa): Ô, dù\n- きた (kita): Phía bắc\n- くに (kuni): Đất nước\n- けん (ken): Tỉnh\n- こと (koto): Việc, sự việc"
            ),
            Exercise(
                subLessonId = "4-1",
                title = "Phát âm và cách viết さ-し-す-せ-そ",
                videoUrl = "https://drive.google.com/uc?id=1Jq9LkHhOZjklfg1c5Q3LQ_yvQK_aP9_c&export=download",
                type = ExerciseType.VIDEO,
                explanation = "I. Hàng さ trong Hiragana➤Hàng さ gồm 5 chữ cái:\n\n- さ (sa): Phát âm như 'sa' trong 'saga'\n- し (shi): Phát âm như 'shi' trong 'sheet'\n- す (su): Phát âm như 'su' trong 'super'\n- せ (se): Phát âm như 'se' trong 'set'\n- そ (so): Phát âm như 'so' trong 'soda'➤II. Cách viết hàng さ➤1. さ (sa):\n- Viết một nét ngang từ trái sang phải\n- Tiếp theo là một đường cong xuống và sang trái\n- Kết thúc với một nét nhỏ ở dưới\n\n2. し (shi):\n- Bắt đầu từ trên, viết một đường thẳng chéo xuống\n- Tiếp tục với một đường cong sang phải\n\n3. す (su):\n- Viết một đường cong từ trên xuống\n- Thêm một nét nhỏ ở giữa\n\n4. せ (se):\n- Viết một đường cong từ trái sang phải\n- Tiếp theo là một đường cong xuống\n- Kết thúc với một nét ngang ở dưới\n\n5. そ (so):\n- Viết một đường cong từ trên xuống\n- Tiếp tục với một đường cong sang phải➤III. Từ vựng với hàng さ➤- さくら (sakura): Hoa anh đào\n- しま (shima): Hòn đảo\n- すし (sushi): Sushi\n- せんせい (sensei): Giáo viên\n- そら (sora): Bầu trời"
            ),
            Exercise(
                subLessonId = "5-1",
                title = "Phát âm và cách viết た-ち-つ-て-と",
                videoUrl = "https://drive.google.com/uc?id=1LqTzPS9HZc7Mptx1eFz7kC8kpghjgfV9&export=download",
                type = ExerciseType.VIDEO,
                explanation = "I. Hàng た trong Hiragana➤Hàng た gồm 5 chữ cái:\n\n- た (ta): Phát âm như 'ta' trong 'task'\n- ち (chi): Phát âm như 'chi' trong 'cheese'\n- つ (tsu): Phát âm như 'tsu' trong 'tsunami'\n- て (te): Phát âm như 'te' trong 'tell'\n- と (to): Phát âm như 'to' trong 'tone'➤II. Cách viết hàng た➤1. た (ta):\n- Viết một nét ngang từ trái sang phải\n- Tiếp theo là một nét đứng xuống\n- Kết thúc với một đường cong sang phải\n\n2. ち (chi):\n- Viết một nét ngang từ trái sang phải\n- Tiếp theo là một đường cong xuống\n- Thêm một nét nhỏ ở giữa\n\n3. つ (tsu):\n- Viết một đường cong từ trên xuống và sang phải\n- Kết thúc với một nét nhỏ ở dưới\n\n4. て (te):\n- Viết một nét ngang từ trái sang phải\n- Tiếp theo là một đường cong xuống và sang trái\n\n5. と (to):\n- Viết một đường cong từ trên xuống\n- Tiếp tục với một đường cong sang phải ở dưới➤III. Từ vựng với hàng た➤- たべる (taberu): Ăn\n- ちず (chizu): Bản đồ\n- つき (tsuki): Mặt trăng\n- てがみ (tegami): Thư\n- とり (tori): Chim"
            ),
            Exercise(
                subLessonId = "6-1",
                title = "Phát âm và cách viết な-に-ぬ-ね-の",
                videoUrl = "https://drive.google.com/uc?id=1jdtqF9PrO3hNl7Bzm5bl6yLv0AY0seXy&export=download",
                type = ExerciseType.VIDEO,
                explanation = "I. Hàng な trong Hiragana➤Hàng な gồm 5 chữ cái:\n\n- な (na): Phát âm như 'na' trong 'nap'\n- に (ni): Phát âm như 'ni' trong 'need'\n- ぬ (nu): Phát âm như 'nu' trong 'noodle'\n- ね (ne): Phát âm như 'ne' trong 'net'\n- の (no): Phát âm như 'no' trong 'note'➤II. Cách viết hàng な➤1. な (na):\n- Viết một nét ngang từ trái sang phải\n- Tiếp theo là một đường cong xuống\n- Kết thúc với một nét nhỏ ở dưới\n\n2. に (ni):\n- Viết hai nét ngang song song\n- Thêm một đường cong ở bên phải\n\n3. ぬ (nu):\n- Viết một đường cong từ trên xuống\n- Thêm một nét nhỏ ở giữa\n- Kết thúc với một đường cong ở dưới\n\n4. ね (ne):\n- Viết một đường cong từ trên xuống\n- Tiếp tục với một đường cong sang phải\n- Thêm một nét nhỏ ở giữa\n\n5. の (no):\n- Viết một đường cong từ trên xuống và sang phải➤III. Từ vựng với hàng な➤- なつ (natsu): Mùa hè\n- にわ (niwa): Vườn\n- ぬま (numa): Đầm lầy\n- ねこ (neko): Mèo\n- のみもの (nomimono): Đồ uống"
            ),
            Exercise(
                subLessonId = "7-1",
                title = "Phát âm và cách viết は-ひ-ふ-へ-ほ",
                videoUrl = "https://drive.google.com/uc?id=1Hp3ixuWiNY9-GxiFfGf0GR0yTlzi4uCK&export=download",
                type = ExerciseType.VIDEO,
                explanation = "I. Hàng は trong Hiragana➤Hàng は gồm 5 chữ cái:\n\n- は (ha): Phát âm như 'ha' trong 'happy'\n- ひ (hi): Phát âm như 'hi' trong 'heat'\n- ふ (fu): Phát âm như 'fu' trong 'food'\n- へ (he): Phát âm như 'he' trong 'help'\n- ほ (ho): Phát âm như 'ho' trong 'hope'➤II. Cách viết hàng は➤1. は (ha):\n- Viết hai nét ngang song song\n- Thêm một nét đứng ở giữa\n\n2. ひ (hi):\n- Viết một nét đứng từ trên xuống\n- Tiếp theo là một đường cong sang phải\n\n3. ふ (fu):\n- Viết một đường cong từ trên xuống\n- Tiếp tục với một đường cong sang phải\n- Thêm một nét nhỏ ở giữa\n\n4. へ (he):\n- Viết một đường cong từ trên xuống và sang trái\n\n5. ほ (ho):\n- Viết một nét đứng từ trên xuống\n- Thêm một nét ngang cắt ngang nét đứng\n- Tiếp theo là một đường cong ở bên phải➤III. Từ vựng với hàng は➤- はな (hana): Hoa\n- ひと (hito): Người\n- ふゆ (fuyu): Mùa đông\n- へや (heya): Phòng\n- ほん (hon): Sách"
            ),
            Exercise(
                subLessonId = "8-1",
                title = "Phát âm và cách viết ま-み-む-め-も",
                videoUrl = "https://drive.google.com/uc?id=1RtwjYelHX5tLHR2k6iVs8woAjQttN8Dh&export=download",
                type = ExerciseType.VIDEO,
                explanation = "I. Hàng ま trong Hiragana➤Hàng ま gồm 5 chữ cái:\n\n- ま (ma): Phát âm như 'ma' trong 'mama'\n- み (mi): Phát âm như 'mi' trong 'meet'\n- む (mu): Phát âm như 'mu' trong 'moon'\n- め (me): Phát âm như 'me' trong 'met'\n- も (mo): Phát âm như 'mo' trong 'more'➤II. Cách viết hàng ま➤1. ま (ma):\n- Viết một nét ngang từ trái sang phải\n- Tiếp theo là một đường cong xuống\n- Thêm một nét nhỏ ở giữa\n\n2. み (mi):\n- Viết một nét đứng từ trên xuống\n- Tiếp theo là hai đường cong song song ở bên phải\n\n3. む (mu):\n- Viết một đường cong từ trên xuống\n- Tiếp tục với một đường cong sang phải\n- Thêm một nét nhỏ ở giữa\n\n4. め (me):\n- Viết một đường cong từ trên xuống\n- Thêm một nét nhỏ ở giữa\n\n5. も (mo):\n- Viết một đường cong từ trên xuống\n- Tiếp tục với một đường cong sang phải\n- Thêm một nét nhỏ ở dưới➤III. Từ vựng với hàng ま➤- まど (mado): Cửa sổ\n- みず (mizu): Nước\n- むし (mushi): Côn trùng\n- め (me): Mắt\n- もの (mono): Đồ vật"
            ),

            Exercise(
                subLessonId = "9-1",
                title = "Phát âm và cách viết や-ゆ-よ",
                videoUrl = "https://drive.google.com/uc?id=1PnQyP2X5cHZSHJlNHXrkxEgt7bVqu7W9&export=download",
                type = ExerciseType.VIDEO,
                explanation = "I. Hàng や trong Hiragana➤Hàng や chỉ gồm 3 chữ cái:\n\n- や (ya): Phát âm như 'ya' trong 'yard'\n- ゆ (yu): Phát âm như 'yu' trong 'youth'\n- よ (yo): Phát âm như 'yo' trong 'yoga'➤II. Cách viết hàng や➤1. や (ya):\n- Viết một nét ngang từ trái sang phải\n- Tiếp theo là một đường cong xuống\n- Thêm một nét nhỏ ở dưới bên phải\n\n2. ゆ (yu):\n- Viết một đường cong từ trên xuống\n- Tiếp tục với một đường cong sang phải\n- Thêm một nét nhỏ ở dưới bên trái\n\n3. よ (yo):\n- Viết một đường cong từ trên xuống bên trái\n- Tiếp tục với một đường cong từ trên xuống bên phải\n- Thêm một nét ngang ở giữa➤III. Từ vựng với hàng や➤- やま (yama): Núi\n- ゆき (yuki): Tuyết\n- よる (yoru): Đêm\n\nHàng や rất quan trọng trong việc tạo ra các âm kết hợp trong tiếng Nhật, ví dụ: きゃ (kya), しゃ (sha), ちゃ (cha)."
            ),
            Exercise(
                subLessonId = "10-1",
                title = "Phát âm và cách viết ら-り-る-れ-ろ",
                videoUrl = "https://drive.google.com/uc?id=1jZa4zE1Fw91of6B5iFSPp-mMl58gftlx&export=download",
                type = ExerciseType.VIDEO,
                explanation = "I. Hàng ら trong Hiragana➤Hàng ら gồm 5 chữ cái:\n\n- ら (ra): Phát âm như 'ra' nhưng gần với 'la' trong tiếng Việt\n- り (ri): Phát âm như 'ri' trong 'ring'\n- る (ru): Phát âm như 'ru' trong 'rule'\n- れ (re): Phát âm như 're' trong 'red'\n- ろ (ro): Phát âm như 'ro' trong 'road'➤II. Cách viết hàng ら➤1. ら (ra):\n- Viết một đường cong từ trên xuống\n- Tiếp tục với một đường cong sang phải\n- Thêm một nét nhỏ ở giữa\n\n2. り (ri):\n- Viết một đường cong từ trên xuống bên trái\n- Tiếp tục với một đường cong từ trên xuống bên phải\n\n3. る (ru):\n- Viết một đường cong từ trên xuống\n- Tiếp tục với một đường cong sang phải\n- Thêm một nét nhỏ ở giữa\n\n4. れ (re):\n- Viết một đường cong từ trên xuống\n- Tiếp tục với một đường cong sang phải\n- Thêm một nét nhỏ ở dưới\n\n5. ろ (ro):\n- Viết một đường cong từ trên xuống và sang phải➤III. Từ vựng với hàng ら➤- らいねん (rainen): Năm sau\n- りんご (ringo): Táo\n- るす (rusu): Vắng nhà\n- れきし (rekishi): Lịch sử\n- ろく (roku): Số sáu"
            ),
            Exercise(
                subLessonId = "11-1",
                title = "Phát âm và cách viết わ-を-ん",
                videoUrl = "https://drive.google.com/uc?id=1NeRPwpcwHt1a7aeSmHf7zAdEY_HQ7dfV&export=download",
                type = ExerciseType.VIDEO,
                explanation = "I. Hàng わ và ん trong Hiragana➤Hàng わ chỉ gồm 2 chữ cái thường dùng và ん là chữ cái đặc biệt:\n\n- わ (wa): Phát âm như 'wa' trong 'water'\n- を (wo): Phát âm như 'o', dùng làm trợ từ đối tượng\n- ん (n): Phát âm như 'n' trong 'no', là âm cuối duy nhất trong tiếng Nhật➤II. Cách viết わ, を và ん➤1. わ (wa):\n- Viết một đường cong từ trên xuống\n- Tiếp tục với một đường cong sang phải\n- Thêm một nét nhỏ ở dưới\n\n2. を (wo):\n- Viết một đường cong từ trên xuống\n- Tiếp tục với một đường cong sang phải\n- Thêm một nét ngang ở giữa\n\n3. ん (n):\n- Viết một đường cong từ trên xuống\n- Tiếp tục với một đường cong sang phải ở dưới➤III. Từ vựng và cách dùng➤- わたし (watashi): Tôi\n- を (wo): Trợ từ đánh dấu đối tượng của động từ\n  Ví dụ: ほんをよむ (Hon wo yomu): Đọc sách\n- ん (n): Âm cuối trong nhiều từ\n  Ví dụ: ほん (hon): Sách\n\nLưu ý: ん là chữ cái duy nhất trong tiếng Nhật có thể đứng một mình và luôn ở cuối âm tiết. Nó không bao giờ bắt đầu một từ."
            )

            // Katakana course

//            Exercise(
//                subLessonId = "k1-1",
//                title = "Giới thiệu bảng chữ cái Katakana",
//                videoUrl = "https://drive.google.com/uc?id=1G2tiFAR1HRFKsNT8fLqri1Ucxim7GNo2&export=download",
//                type = ExerciseType.VIDEO,
//                explanation = "I. Nguồn gốc và vai trò của Katakana➤Katakana là một trong ba bảng chữ cái của tiếng Nhật, cùng với Hiragana và Kanji. Katakana được phát triển từ các phần của chữ Hán (Kanji) vào khoảng thế kỷ thứ 9. Ban đầu, Katakana được sử dụng bởi các nhà sư nam giới để ghi chú phát âm của các văn bản Phật giáo bằng tiếng Trung.➤II. Cách sử dụng Katakana trong tiếng Nhật hiện đại➤Katakana chủ yếu được sử dụng để viết: 1) Từ ngoại lai (gairaigo): Ví dụ như コンピューター (konpyūtā - computer), テレビ (terebi - television). 2) Tên nước ngoài: Ví dụ như アメリカ (Amerika - America), フランス (Furansu - France). 3) Thuật ngữ khoa học, kỹ thuật: Ví dụ như エネルギー (enerugī - energy). 4) Từ nhấn mạnh: Tương tự như việc sử dụng chữ in đậm hoặc in nghiêng trong tiếng Việt.➤III. Cấu trúc của bảng Katakana➤Katakana có 46 ký tự cơ bản, mỗi ký tự đại diện cho một âm tiết. Các ký tự được sắp xếp theo thứ tự: hàng あ (a, i, u, e, o), hàng か (ka, ki, ku, ke, ko), v.v. Katakana có hình dạng góc cạnh và thẳng hơn so với Hiragana."
//            ),
//            Exercise(
//                subLessonId = "k2-1",
//                title = "Phát âm và cách viết ア-イ-ウ-エ-オ",
//                videoUrl = "https://drive.google.com/uc?id=1Tkj5cMdfP2GtR60Usfbf0bLqNlRjzz6w&export=download",
//                type = ExerciseType.VIDEO,
//                explanation = "I. Giới thiệu hàng ア➤Hàng ア (a-gyō) là hàng đầu tiên trong bảng Katakana, bao gồm 5 chữ cái: ア (a), イ (i), ウ (u), エ (e), オ (o). Đây là những nguyên âm cơ bản trong tiếng Nhật.➤II. Cách viết và phát âm➤ア (a): Phát âm như 'a' trong 'ah'. Cách viết: Hai nét, một nét xiên từ trái sang phải và một nét ngắn ở giữa. イ (i): Phát âm như 'i' trong 'see'. Cách viết: Hai nét thẳng, một dài và một ngắn. ウ (u): Phát âm như 'u' trong 'blue'. Cách viết: Một nét cong và một nét ngắn ở bên phải. エ (e): Phát âm như 'e' trong 'bed'. Cách viết: Ba nét, hai nét ngang và một nét dọc ở giữa. オ (o): Phát âm như 'o' trong 'go'. Cách viết: Ba nét, một nét ngang trên cùng, một nét dọc và một nét ngang dưới cùng.➤III. Từ vựng ví dụ➤アイス (aisu - ice cream): Kem. イエロー (ierō - yellow): Màu vàng. ウール (ūru - wool): Len. エアコン (eakon - air conditioner): Máy điều hòa. オレンジ (orenji - orange): Quả cam, màu cam."
//            ),
//
//            Exercise(
//                subLessonId = "k3-1",
//                title = "Phát âm và cách viết カ-キ-ク-ケ-コ",
//                videoUrl = "https://drive.google.com/uc?id=1Fjp7jImlYbvmKvEwxIrAqdbzygsqYI9Q&export=download",
//                type = ExerciseType.VIDEO,
//                explanation = "I. Giới thiệu hàng カ➤Hàng カ (ka-gyō) là hàng thứ hai trong bảng Katakana, bao gồm 5 chữ cái: カ (ka), キ (ki), ク (ku), ケ (ke), コ (ko). Đây là những âm kết hợp giữa phụ âm 'k' và các nguyên âm.➤II. Cách viết và phát âm➤カ (ka): Phát âm như 'ka' trong 'car'. Cách viết: Hai nét, một nét xiên và một nét nhỏ ở bên phải. キ (ki): Phát âm như 'ki' trong 'key'. Cách viết: Ba nét, một nét dọc, một nét ngang và một nét xiên. ク (ku): Phát âm như 'ku' trong 'cool'. Cách viết: Hai nét, một nét góc và một nét ngắn. ケ (ke): Phát âm như 'ke' trong 'kettle'. Cách viết: Ba nét, một nét xiên, một nét ngang và một nét dọc. コ (ko): Phát âm như 'ko' trong 'code'. Cách viết: Hai nét, một nét ngang và một nét dọc.➤III. Từ vựng ví dụ➤カメラ (kamera - camera): Máy ảnh. キーボード (kībōdo - keyboard): Bàn phím. クッキー (kukkī - cookie): Bánh quy. ケーキ (kēki - cake): Bánh ngọt. コーヒー (kōhī - coffee): Cà phê."
//            ),
//            Exercise(
//                subLessonId = "k4-1",
//                title = "Phát âm và cách viết サ-シ-ス-セ-ソ",
//                videoUrl = "https://drive.google.com/uc?id=1Jq9LkHhOZjklfg1c5Q3LQ_yvQK_aP9_c&export=download",
//                type = ExerciseType.VIDEO,
//                explanation = "I. Giới thiệu hàng サ➤Hàng サ (sa-gyō) là hàng thứ ba trong bảng Katakana, bao gồm 5 chữ cái: サ (sa), シ (shi), ス (su), セ (se), ソ (so). Đây là những âm kết hợp giữa phụ âm 's' và các nguyên âm.➤II. Cách viết và phát âm➤サ (sa): Phát âm như 'sa' trong 'saga'. Cách viết: Ba nét, một nét ngang, một nét dọc và một nét xiên. シ (shi): Phát âm như 'shi' trong 'she'. Cách viết: Ba nét xiên ngắn. Lưu ý: Dễ nhầm lẫn với ツ (tsu), nhưng シ có các nét xiên hơn. ス (su): Phát âm như 'su' trong 'super'. Cách viết: Hai nét, một nét cong và một nét ngắn. セ (se): Phát âm như 'se' trong 'set'. Cách viết: Ba nét, một nét ngang, một nét dọc và một nét xiên. ソ (so): Phát âm như 'so' trong 'soda'. Cách viết: Hai nét xiên. Lưu ý: Dễ nhầm lẫn với ン (n), nhưng ソ có góc xiên hơn.➤III. Từ vựng ví dụ➤サラダ (sarada - salad): Salad. シャツ (shatsu - shirt): Áo sơ mi. スープ (sūpu - soup): Súp. セーター (sētā - sweater): Áo len. ソファ (sofa - sofa): Ghế sofa."
//            ),
//            Exercise(
//                subLessonId = "k5-1",
//                title = "Phát âm và cách viết タ-チ-ツ-テ-ト",
//                videoUrl = "https://drive.google.com/uc?id=1LqTzPS9HZc7Mptx1eFz7kC8kpghjgfV9&export=download",
//                type = ExerciseType.VIDEO,
//                explanation = "I. Giới thiệu hàng タ➤Hàng タ (ta-gyō) là hàng thứ tư trong bảng Katakana, bao gồm 5 chữ cái: タ (ta), チ (chi), ツ (tsu), テ (te), ト (to). Đây là những âm kết hợp giữa phụ âm 't' và các nguyên âm.➤II. Cách viết và phát âm➤タ (ta): Phát âm như 'ta' trong 'task'. Cách viết: Hai nét, một nét dọc và một nét xiên. チ (chi): Phát âm như 'chi' trong 'cheese'. Cách viết: Ba nét, một nét dọc, một nét ngang và một nét cong. ツ (tsu): Phát âm như 'tsu' trong 'tsunami'. Cách viết: Ba nét cong. Lưu ý: Dễ nhầm lẫn với シ (shi), nhưng ツ có các nét cong hơn. テ (te): Phát âm như 'te' trong 'tell'. Cách viết: Ba nét, một nét ngang, một nét dọc và một nét xiên. ト (to): Phát âm như 'to' trong 'tone'. Cách viết: Hai nét, một nét dọc và một nét xiên.➤III. Từ vựng ví dụ➤タクシー (takushī - taxi): Taxi. チーズ (chīzu - cheese): Phô mai. ツアー (tsuā - tour): Tour du lịch. テニス (tenisu - tennis): Quần vợt. トマト (tomato - tomato): Cà chua."
//            ),
//            Exercise(
//                subLessonId = "k6-1",
//                title = "Phát âm và cách viết ナ-ニ-ヌ-ネ-ノ",
//                videoUrl = "https://drive.google.com/uc?id=1jdtqF9PrO3hNl7Bzm5bl6yLv0AY0seXy&export=download",
//                type = ExerciseType.VIDEO,
//                explanation = "I. Giới thiệu hàng ナ➤Hàng ナ (na-gyō) là hàng thứ năm trong bảng Katakana, bao gồm 5 chữ cái: ナ (na), ニ (ni), ヌ (nu), ネ (ne), ノ (no). Đây là những âm kết hợp giữa phụ âm 'n' và các nguyên âm.➤II. Cách viết và phát âm➤ナ (na): Phát âm như 'na' trong 'not'. Cách viết: Hai nét, một nét xiên và một nét cong. ニ (ni): Phát âm như 'ni' trong 'need'. Cách viết: Hai nét ngang. ヌ (nu): Phát âm như 'nu' trong 'noodle'. Cách viết: Hai nét, một nét cong và một nét xiên. ネ (ne): Phát âm như 'ne' trong 'net'. Cách viết: Hai nét, một nét xiên và một nét cong. ノ (no): Phát âm như 'no' trong 'note'. Cách viết: Một nét xiên dài.➤III. Từ vựng ví dụ➤ナイフ (naifu - knife): Dao. ニュース (nyūsu - news): Tin tức. ヌードル (nūdoru - noodle): Mì. ネクタイ (nekutai - necktie): Cà vạt. ノート (nōto - note): Sổ ghi chép."
//            ),
//            Exercise(
//                subLessonId = "k7-1",
//                title = "Phát âm và cách viết ハ-ヒ-フ-ヘ-ホ",
//                videoUrl = "https://drive.google.com/uc?id=1Hp3ixuWiNY9-GxiFfGf0GR0yTlzi4uCK&export=download",
//                type = ExerciseType.VIDEO,
//                explanation = "I. Giới thiệu hàng ハ➤Hàng ハ (ha-gyō) là hàng thứ sáu trong bảng Katakana, bao gồm 5 chữ cái: ハ (ha), ヒ (hi), フ (fu), ヘ (he), ホ (ho). Đây là những âm kết hợp giữa phụ âm 'h' và các nguyên âm.➤II. Cách viết và phát âm➤ハ (ha): Phát âm như 'ha' trong 'hot'. Cách viết: Hai nét xiên. ヒ (hi): Phát âm như 'hi' trong 'he'. Cách viết: Hai nét, một nét dọc và một nét xiên. フ (fu): Phát âm như 'fu' trong 'food'. Cách viết: Hai nét, một nét dọc và một nét cong. ヘ (he): Phát âm như 'he' trong 'help'. Cách viết: Một nét ngang cong. ホ (ho): Phát âm như 'ho' trong 'hope'. Cách viết: Bốn nét, hai nét dọc và hai nét ngang.➤III. Từ vựng ví dụ➤ハンバーガー (hanbāgā - hamburger): Hamburger. ヒーター (hītā - heater): Máy sưởi. フォーク (fōku - fork): Nĩa. ヘリコプター (herikoputā - helicopter): Trực thăng. ホテル (hoteru - hotel): Khách sạn."
//            ),
//            Exercise(
//                subLessonId = "k8-1",
//                title = "Phát âm và cách viết マ-ミ-ム-メ-モ",
//                videoUrl = "https://drive.google.com/uc?id=1RtwjYelHX5tLHR2k6iVs8woAjQttN8Dh&export=download",
//                type = ExerciseType.VIDEO,
//                explanation = "I. Giới thiệu hàng マ➤Hàng マ (ma-gyō) là hàng thứ bảy trong bảng Katakana, bao gồm 5 chữ cái: マ (ma), ミ (mi), ム (mu), メ (me), モ (mo). Đây là những âm kết hợp giữa phụ âm 'm' và các nguyên âm.➤II. Cách viết và phát âm➤マ (ma): Phát âm như 'ma' trong 'mama'. Cách viết: Ba nét, hai nét xiên và một nét ngang. ミ (mi): Phát âm như 'mi' trong 'me'. Cách viết: Ba nét xiên. ム (mu): Phát âm như 'mu' trong 'moon'. Cách viết: Ba nét, hai nét xiên và một nét ngang. メ (me): Phát âm như 'me' trong 'met'. Cách viết: Hai nét, một nét xiên và một nét cong. モ (mo): Phát âm như 'mo' trong 'more'. Cách viết: Ba nét, một nét dọc, một nét ngang và một nét cong.➤III. Từ vựng ví dụ➤マンゴー (mangō - mango): Xoài. ミルク (miruku - milk): Sữa. ムービー (mūbī - movie): Phim. メニュー (menyū - menu): Thực đơn. モデル (moderu - model): Người mẫu."
//            ),
//            Exercise(
//                subLessonId = "k9-1",
//                title = "Phát âm và cách viết ヤ-ユ-ヨ",
//                videoUrl = "https://drive.google.com/uc?id=1PnQyP2X5cHZSHJlNHXrkxEgt7bVqu7W9&export=download",
//                type = ExerciseType.VIDEO,
//                explanation = "I. Giới thiệu hàng ヤ➤Hàng ヤ (ya-gyō) là hàng thứ tám trong bảng Katakana, bao gồm 3 chữ cái: ヤ (ya), ユ (yu), ヨ (yo). Đây là những âm kết hợp giữa phụ âm 'y' và các nguyên âm.➤II. Cách viết và phát âm➤ヤ (ya): Phát âm như 'ya' trong 'yard'. Cách viết: Hai nét, một nét xiên và một nét cong. ユ (yu): Phát âm như 'yu' trong 'you'. Cách viết: Hai nét, một nét cong và một nét xiên. ヨ (yo): Phát âm như 'yo' trong 'yoga'. Cách viết: Ba nét, một nét dọc và hai nét ngang.➤III. Từ vựng ví dụ➤ヤクルト (yakuruto - Yakult): Yakult (loại đồ uống). ユニフォーム (yunifōmu - uniform): Đồng phục. ヨーグルト (yōguruto - yogurt): Sữa chua."
//            ),
//            Exercise(
//                subLessonId = "k10-1",
//                title = "Phát âm và cách viết ラ-リ-ル-レ-ロ",
//                videoUrl = "https://drive.google.com/uc?id=1jZa4zE1Fw91of6B5iFSPp-mMl58gftlx&export=download",
//                type = ExerciseType.VIDEO,
//                explanation = "I. Giới thiệu hàng ラ➤Hàng ラ (ra-gyō) là hàng thứ chín trong bảng Katakana, bao gồm 5 chữ cái: ラ (ra), リ (ri), ル (ru), レ (re), ロ (ro). Đây là những âm kết hợp giữa phụ âm 'r' và các nguyên âm. Lưu ý rằng âm 'r' trong tiếng Nhật là một âm đặc biệt, gần với âm 'l' và 'r' trong tiếng Anh.➤II. Cách viết và phát âm➤ラ (ra): Phát âm như 'ra' trong 'ramen'. Cách viết: Hai nét, một nét xiên và một nét cong. リ (ri): Phát âm như 'ri' trong 'reach'. Cách viết: Hai nét, một nét xiên và một nét ngắn. ル (ru): Phát âm như 'ru' trong 'rule'. Cách viết: Hai nét, một nét cong và một nét xiên. レ (re): Phát âm như 're' trong 'red'. Cách viết: Hai nét xiên. ロ (ro): Phát âm như 'ro' trong 'road'. Cách viết: Một hình vuông.➤III. Từ vựng ví dụ➤ラジオ (rajio - radio): Radio. リンゴ (ringo - apple): Táo. ルーム (rūmu - room): Phòng. レストラン (resutoran - restaurant): Nhà hàng. ロボット (robotto - robot): Robot."
//            ),
//            Exercise(
//                subLessonId = "k11-1",
//                title = "Phát âm và cách viết ワ-ヲ-ン",
//                videoUrl = "https://drive.google.com/uc?id=1Tz_Yd-Yx-Yd-Yx-Yd-Yx-Yd-Yx-Yd-Y&export=download",
//                type = ExerciseType.VIDEO,
//                explanation = "I. Giới thiệu hàng ワ và ン➤Hàng ワ (wa-gyō) là hàng cuối cùng trong bảng Katakana, bao gồm 2 chữ cái: ワ (wa) và ヲ (wo). Ngoài ra còn có ン (n), là phụ âm duy nhất có thể đứng một mình trong tiếng Nhật.➤II. Cách viết và phát âm➤ワ (wa): Phát âm như 'wa' trong 'water'. Cách viết: Hai nét, một nét xiên và một nét cong. ヲ (wo): Phát âm như 'wo' trong 'won't'. Cách viết: Ba nét, một nét xiên, một nét ngang và một nét cong. Lưu ý: ヲ hiếm khi được sử dụng trong tiếng Nhật hiện đại, chủ yếu chỉ xuất hiện trong trợ từ を (wo). ン (n): Phát âm như 'n' trong 'no'. Cách viết: Hai nét xiên. Lưu ý: Dễ nhầm lẫn với ソ (so), nhưng ン có góc xiên ít hơn.➤III. Từ vựng ví dụ➤ワイン (wain - wine): Rượu vang. ワンピース (wanpīsu - one-piece dress): Váy liền. パン (pan - bread): Bánh mì (chứa ン). ペン (pen - pen): Bút (chứa ン)."
//            ),
//            Exercise(
//                subLessonId = "k12-1",
//                title = "Katakana với dấu dakuten và handakuten",
//                videoUrl = "https://drive.google.com/uc?id=1Tz_Yd-Yx-Yd-Yx-Yd-Yx-Yd-Yx-Yd-Y&export=download",
//                type = ExerciseType.VIDEO,
//                explanation = "I. Giới thiệu về dakuten và handakuten➤Dakuten (゛): Còn gọi là ten-ten, là hai dấu chấm nhỏ được thêm vào góc trên bên phải của một ký tự Katakana để biến âm vô thanh thành âm hữu thanh. Handakuten (゜): Còn gọi là maru, là một vòng tròn nhỏ được thêm vào góc trên bên phải của các ký tự trong hàng ハ (ha) để tạo ra âm 'p'.➤II. Dakuten - Biến đổi âm vô thanh thành âm hữu thanh➤カ (ka) → ガ (ga), キ (ki) → ギ (gi), ク (ku) → グ (gu), ケ (ke) → ゲ (ge), コ (ko) → ゴ (go). サ (sa) → ザ (za), シ (shi) → ジ (ji), ス (su) → ズ (zu), セ (se) → ゼ (ze), ソ (so) → ゾ (zo). タ (ta) → ダ (da), チ (chi) → ヂ (ji), ツ (tsu) → ヅ (zu), テ (te) → デ (de), ト (to) → ド (do). ハ (ha) → バ (ba), ヒ (hi) → ビ (bi), フ (fu) → ブ (bu), ヘ (he) → ベ (be), ホ (ho) → ボ (bo).➤III. Handakuten - Biến đổi âm 'h' thành âm 'p'➤ハ (ha) → パ (pa), ヒ (hi) → ピ (pi), フ (fu) → プ (pu), ヘ (he) → ペ (pe), ホ (ho) → ポ (po).➤IV. Từ vựng ví dụ➤ガム (gamu - gum): Kẹo cao su. ジュース (jūsu - juice): Nước ép. ドア (doa - door): Cửa. バス (basu - bus): Xe buýt. パン (pan - bread): Bánh mì."
//            ),
//            Exercise(
//                subLessonId = "k13-1",
//                title = "Cách viết từ vựng ngoại lai",
//                videoUrl = "https://drive.google.com/uc?id=1Tz_Yd-Yx-Yd-Yx-Yd-Yx-Yd-Yx-Yd-Y&export=download",
//                type = ExerciseType.VIDEO,
//                explanation = "I. Nguyên tắc cơ bản khi viết từ ngoại lai bằng Katakana➤Katakana được sử dụng chủ yếu để viết từ ngoại lai (gairaigo). Khi chuyển đổi từ tiếng nước ngoài sang Katakana, người Nhật dựa vào cách phát âm chứ không phải cách viết. Tiếng Nhật có cấu trúc âm tiết mở (kết thúc bằng nguyên âm), nên các phụ âm cuối thường được thêm nguyên âm.➤II. Quy tắc chuyển đổi phổ biến➤1. Âm dài được biểu thị bằng dấu gạch ngang (ー): コーヒー (kōhī - coffee). 2. Phụ âm kép được biểu thị bằng ッ nhỏ: ベッド (beddo - bed). 3. Âm tiết kết thúc bằng phụ âm thường được thêm ウ hoặc オ: ボックス (bokkusu - box). 4. Âm 'f' thường được chuyển thành フ (fu): フォーク (fōku - fork). 5. Âm 'v' thường được chuyển thành ヴ (vu): ヴァイオリン (vaiorin - violin). 6. Âm 'th' thường được chuyển thành ス (su) hoặc ズ (zu): スリー (surī - three).➤III. Ví dụ từ vựng ngoại lai➤コンピューター (konpyūtā - computer): Máy tính. スマートフォン (sumātofon - smartphone): Điện thoại thông minh. インターネット (intānetto - internet): Internet. テレビ (terebi - television): Tivi. ハンバーガー (hanbāgā - hamburger): Hamburger."
//            ),

        )

        "Practice" -> listOf(
            Exercise(
                subLessonId = "1-2",
                question = "Chữ 'あ' thuộc loại nào trong hệ thống chữ cái Nhật Bản?",
                answer = "Hiragana",
                imageUrl = "https://drive.google.com/uc?export=view&id=1TWpes3nKYbwSWyUj0uG0v5p-_a_zaVVh",
                type = ExerciseType.PRACTICE,
                options = listOf("Hiragana", "Katakana", "Kanji", "Romaji"),
                title = "Hiragana Knowledge",
                explanation = "Chữ 'あ' là một ký tự trong bảng chữ cái Hiragana.",
                romanji = "a",
                kana = "あ"
            ),
            Exercise(
                subLessonId = "1-2",
                question = "Chữ \"い\" được sử dụng trong từ nào dưới đây?",
                answer = "いえ",
                type = ExerciseType.PRACTICE,
                imageUrl = "https://drive.google.com/uc?export=view&id=1TWpes3nKYbwSWyUj0uG0v5p-_a_zaVVh",
                options = listOf("いえ", "いく", "いま", "いち"),
                title = "Hiragana Vocabulary",
                explanation = "Chữ 'い' có thể xuất hiện trong từ 'いえ' (nhà).",
                romanji = "i",
                kana = "い"
            ),
            Exercise(
                subLessonId = "2-2",
                question = "Từ vựng nào sau đây có chữ 'あ' và có nghĩa 'bạn'?",
                answer = "あなた",
                type = ExerciseType.PRACTICE,
                imageUrl = "https://drive.google.com/uc?export=view&id=1TWpes3nKYbwSWyUj0uG0v5p-_a_zaVVh",
                options = listOf("あなた", "あき", "あさ", "あら"),
                title = "Hiragana Vocabulary",
                explanation = "'あなた' có nghĩa là 'bạn' trong tiếng Nhật.",
                romanji = "anata",
                kana = "あなた"
            ),
            Exercise(
                subLessonId = "3-2",
                question = "Chữ \"か\" có thể được sử dụng trong từ nào sau đây?",
                answer = "かばん",
                type = ExerciseType.PRACTICE,
                imageUrl = "https://drive.google.com/uc?export=view&id=1TWpes3nKYbwSWyUj0uG0v5p-_a_zaVVh",
                options = listOf("かばん", "かさ", "きもの", "きょう"),
                title = "Hiragana Vocabulary",
                explanation = "'かばん' có nghĩa là 'cái cặp'.",
                romanji = "kaban",
                kana = "かばん"
            ),
            Exercise(
                subLessonId = "3-3",
                question = "Chữ 'き' có thể kết hợp với từ nào sau đây để có nghĩa 'bài học'?",
                answer = "きょうか",
                type = ExerciseType.PRACTICE,
                imageUrl = "https://drive.google.com/uc?export=view&id=1TWpes3nKYbwSWyUj0uG0v5p-_a_zaVVh",
                options = listOf("きょうか", "きんきょう", "きんこう", "きょうし"),
                title = "Hiragana Vocabulary",
                explanation = "'きょうか' có nghĩa là 'bài học'.",
                romanji = "kyouka",
                kana = "きょうか"
            ),
            Exercise(
                subLessonId = "4-2",
                question = "Chữ 'さ' thường gặp trong từ nào sau đây?",
                answer = "さくら",
                type = ExerciseType.PRACTICE,
                imageUrl = "https://drive.google.com/uc?export=view&id=1TWpes3nKYbwSWyUj0uG0v5p-_a_zaVVh",
                options = listOf("さくら", "さよなら", "すし", "せんせい"),
                title = "Hiragana Vocabulary",
                explanation = "'さくら' có nghĩa là 'hoa anh đào'.",
                romanji = "sakura",
                kana = "さくら"
            ),
            Exercise(
                subLessonId = "5-2",
                question = "Chữ 'た' có thể dùng trong từ nào dưới đây để có nghĩa 'một'?",
                answer = "たったいま",
                type = ExerciseType.PRACTICE,
                imageUrl = "https://drive.google.com/uc?export=view&id=1TWpes3nKYbwSWyUj0uG0v5p-_a_zaVVh",
                options = listOf("たったいま", "たべる", "たちまち", "たけやま"),
                title = "Hiragana Vocabulary",
                explanation = "'たったいま' có nghĩa là 'vừa mới'.",
                romanji = "tattaima",
                kana = "たったいま"
            ),
            Exercise(
                subLessonId = "5-3",
                question = "Khi viết chữ 'た', bạn cần phải chú ý điều gì để viết đúng?",
                answer = "Viết nét ngang trước rồi mới viết nét đứng.",
                type = ExerciseType.PRACTICE,
                imageUrl = "https://drive.google.com/uc?export=view&id=1TWpes3nKYbwSWyUj0uG0v5p-_a_zaVVh",
                options = listOf("Viết nét ngang trước rồi mới viết nét đứng", "Viết nét đứng trước rồi mới viết nét ngang", "Viết từ trên xuống dưới", "Viết từ trái qua phải"),
                title = "Writing Hiragana",
                explanation = "Để viết đúng chữ 'た', bạn cần viết nét ngang trước rồi viết nét đứng.",
                romanji = "ta",
                kana = "た"
            ),
            Exercise(
                subLessonId = "6-2",
                question = "Chữ 'な' trong từ 'なま' có nghĩa là gì?",
                answer = "Sống",
                type = ExerciseType.PRACTICE,
                imageUrl = "https://drive.google.com/uc?export=view&id=1TWpes3nKYbwSWyUj0uG0v5p-_a_zaVVh",
                options = listOf("Sống", "Người", "Nước", "Nay"),
                title = "Hiragana Vocabulary",
                explanation = "'なま' có nghĩa là 'sống', ví dụ như 'なまもの' (thực phẩm tươi).",
                romanji = "nama",
                kana = "なま"
            ),
            Exercise(
                subLessonId = "6-3",
                question = "Chữ 'ぬ' trong từ 'ぬいぐるみ' có nghĩa là gì?",
                answer = "Búp bê",
                type = ExerciseType.PRACTICE,
                imageUrl = "https://drive.google.com/uc?export=view&id=1TWpes3nKYbwSWyUj0uG0v5p-_a_zaVVh",
                options = listOf("Búp bê", "Cửa", "Điều", "Gối"),
                title = "Hiragana Vocabulary",
                explanation = "'ぬいぐるみ' có nghĩa là 'búp bê'.",
                romanji = "nuigurumi",
                kana = "ぬいぐるみ"
            ),
            Exercise(
                subLessonId = "7-2",
                question = "Chữ 'は' được sử dụng trong từ nào dưới đây?",
                answer = "はたけ (ruộng)",
                type = ExerciseType.PRACTICE,
                imageUrl = "https://drive.google.com/uc?export=view&id=1TWpes3nKYbwSWyUj0uG0v5p-_a_zaVVh",
                options = listOf("はな (hoa)", "はたけ (ruộng)", "ひと (người)", "ふる (cũ)"),
                title = "Flashcard Hiragana",
                explanation = "Chọn từ đúng chứa chữ 'は'.",
                romanji = "ha",
                kana = "は"
            ),
            Exercise(
                subLessonId = "7-3",
                question = "Chữ 'ひ' là chữ thuộc hàng nào trong bảng Hiragana?",
                answer = "Hàng ひ",
                type = ExerciseType.PRACTICE,
                imageUrl = "https://drive.google.com/uc?export=view&id=1TWpes3nKYbwSWyUj0uG0v5p-_a_zaVVh",
                options = listOf("Hàng ほ", "Hàng ひ", "Hàng ふ", "Hàng へ"),
                title = "Flashcard Hiragana",
                explanation = "Chọn hàng đúng của chữ 'ひ'.",
                romanji = "hi",
                kana = "ひ"
            ),
            Exercise(
                subLessonId = "8-2",
                question = "Từ nào sau đây chứa chữ 'ま'?",
                answer = "まど (cửa sổ)",
                type = ExerciseType.PRACTICE,
                imageUrl = "https://drive.google.com/uc?export=view&id=1TWpes3nKYbwSWyUj0uG0v5p-_a_zaVVh",
                options = listOf("みず (nước)", "まど (cửa sổ)", "むし (côn trùng)", "めがね (kính)"),
                title = "Flashcard Hiragana",
                explanation = "Chọn từ đúng chứa chữ 'ま'.",
                romanji = "ma",
                kana = "ま"
            ),
            Exercise(
                subLessonId = "8-3",
                question = "Chữ 'む' phát âm là gì trong từ 'むし' (côn trùng)?",
                answer = "mu",
                type = ExerciseType.PRACTICE,
                imageUrl = "https://drive.google.com/uc?export=view&id=1TWpes3nKYbwSWyUj0uG0v5p-_a_zaVVh",
                options = listOf("mi", "mu", "me", "mo"),
                title = "Flashcard Hiragana",
                explanation = "Chọn nghĩa đúng với chữ 'む' trong từ 'むし'.",
                romanji = "mu",
                kana = "む"
            ),
            Exercise(
                subLessonId = "9-2",
                question = "Chữ 'や' thường xuất hiện trong các từ vựng nào sau đây?",
                answer = "やさい (rau)",
                type = ExerciseType.PRACTICE,
                imageUrl = "https://drive.google.com/uc?export=view&id=1TWpes3nKYbwSWyUj0uG0v5p-_a_zaVVh",
                options = listOf("やすみ (nghỉ ngơi)", "やさい (rau)", "ゆめ (giấc mơ)", "よる (ban đêm)"),
                title = "Flashcard Hiragana",
                explanation = "Chọn từ đúng chứa chữ 'や'.",
                romanji = "ya",
                kana = "や"
            ),
            Exercise(
                subLessonId = "9-3",
                question = "Chữ 'ゆ' được phát âm là gì trong từ 'ゆめ' (giấc mơ)?",
                answer = "yu",
                type = ExerciseType.PRACTICE,
                imageUrl = "https://drive.google.com/uc?export=view&id=1TWpes3nKYbwSWyUj0uG0v5p-_a_zaVVh",
                options = listOf("ya", "yu", "yo", "yi"),
                title = "Flashcard Hiragana",
                explanation = "Chọn nghĩa đúng với chữ 'ゆ' trong từ 'ゆめ'.",
                romanji = "yu",
                kana = "ゆ"
            ),
            Exercise(
                subLessonId = "10-2",
                question = "Chữ 'ら' trong từ 'らく' (dễ chịu) được phát âm là gì?",
                answer = "ra",
                type = ExerciseType.PRACTICE,
                imageUrl = "https://drive.google.com/uc?export=view&id=1TWpes3nKYbwSWyUj0uG0v5p-_a_zaVVh",
                options = listOf("ra", "ri", "ru", "re"),
                title = "Flashcard Hiragana",
                explanation = "Chọn nghĩa đúng với chữ 'ら' trong từ 'らく'.",
                romanji = "ra",
                kana = "ら"
            ),
            Exercise(
                subLessonId = "10-3",
                question = "Chữ 'り' được sử dụng trong từ nào dưới đây?",
                answer = "りんご (táo)",
                type = ExerciseType.PRACTICE,
                imageUrl = "https://drive.google.com/uc?export=view&id=1TWpes3nKYbwSWyUj0uG0v5p-_a_zaVVh",
                options = listOf("りんご (táo)", "るす (vắng nhà)", "れんしゅう (luyện tập)", "れいぞうこ (tủ lạnh)"),
                title = "Flashcard Hiragana",
                explanation = "Chọn từ đúng chứa chữ 'り'.",
                romanji = "ri",
                kana = "り"
            ),
            Exercise(
                subLessonId = "11-2",
                question = "Chữ 'わ' xuất hiện trong từ nào dưới đây?",
                answer = "わたし (tôi)",
                type = ExerciseType.PRACTICE,
                imageUrl = "https://drive.google.com/uc?export=view&id=1TWpes3nKYbwSWyUj0uG0v5p-_a_zaVVh",
                options = listOf("わたし (tôi)", "を (biểu tượng chỉ đối tượng trong câu)", "ん (âm cuối của từ)", "よ (chỉ sự chú ý)"),
                title = "Flashcard Hiragana",
                explanation = "Chọn từ đúng chứa chữ 'わ'.",
                romanji = "wa",
                kana = "わ"
            ),
            Exercise(
                subLessonId = "11-3",
                question = "Chữ 'ん' trong từ 'きんぎょ' (cá vàng) phát âm là gì?",
                answer = "n",
                type = ExerciseType.PRACTICE,
                imageUrl = "https://drive.google.com/uc?export=view&id=1TWpes3nKYbwSWyUj0uG0v5p-_a_zaVVh",
                options = listOf("n", "m", "ng", "no"),
                title = "Flashcard Hiragana",
                explanation = "Chọn nghĩa đúng với chữ 'ん' trong từ 'きんぎょ'.",
                romanji = "n",
                kana = "ん"
            ),
            Exercise(
                subLessonId = "12-2",
                question = "Chữ nào thuộc hàng 'た' trong bảng Hiragana?",
                answer = "た",
                type = ExerciseType.PRACTICE,
                imageUrl = "https://drive.google.com/uc?export=view&id=1TWpes3nKYbwSWyUj0uG0v5p-_a_zaVVh",
                options = listOf("た", "ち", "つ", "て"),
                title = "Hiragana Review",
                explanation = "Chọn chữ đúng thuộc hàng 'た'.",
                romanji = "ta",
                kana = "た"
            ),
            Exercise(
                subLessonId = "12-2",
                question = "Chữ 'し' trong từ 'しごと' (công việc) phát âm là gì?",
                answer = "shi",
                type = ExerciseType.PRACTICE,
                imageUrl = "https://drive.google.com/uc?export=view&id=1TWpes3nKYbwSWyUj0uG0v5p-_a_zaVVh",
                options = listOf("shi", "sa", "su", "se"),
                title = "Flashcard Hiragana",
                explanation = "Chọn nghĩa đúng với chữ 'し' trong từ 'しごと'.",
                romanji = "shi",
                kana = "し"
            ),
            Exercise(
                subLessonId = "13-2",
                question = "Viết chữ cái 'ち' theo đúng thứ tự.",
                answer = "ち",
                type = ExerciseType.PRACTICE,
                imageUrl = "https://drive.google.com/uc?export=view&id=1TWpes3nKYbwSWyUj0uG0v5p-_a_zaVVh",
                options = listOf("ち", "つ", "て", "と"),
                title = "Practice Hiragana Writing",
                explanation = "Luyện viết chữ cái 'ち' theo đúng thứ tự.",
                romanji = "chi",
                kana = "ち"
            ),
            Exercise(
                subLessonId = "13-2",
                question = "Chữ 'し' trong từ 'しゅくだい' (bài tập) phát âm là gì?",
                answer = "shi",
                type = ExerciseType.PRACTICE,
                imageUrl = "https://drive.google.com/uc?export=view&id=1TWpes3nKYbwSWyUj0uG0v5p-_a_zaVVh",
                options = listOf("su", "shi", "sa", "se"),
                title = "Practice Hiragana Writing",
                explanation = "Chọn nghĩa đúng với chữ 'し' trong từ 'しゅくだい'.",
                romanji = "shi",
                kana = "し"
            ),
            Exercise(
                subLessonId = "14-2",
                question = "Chữ nào thuộc hàng 'は' trong từ 'はな' (hoa)?",
                answer = "は",
                options = listOf("は", "ひ", "ふ", "へ"),
                type = ExerciseType.PRACTICE,
                imageUrl = "https://drive.google.com/uc?export=view&id=1TWpes3nKYbwSWyUj0uG0v5p-_a_zaVVh",
                title = "Final Quiz Hiragana",
                explanation = "Chọn chữ cái đúng thuộc hàng 'は'.",
                romanji = "ha",
                kana = "は"
            ),
            Exercise(
                subLessonId = "14-2",
                question = "Chữ 'き' trong từ 'きもの' (kimono) phát âm là gì?",
                answer = "ki",
                type = ExerciseType.PRACTICE,
                imageUrl = "https://drive.google.com/uc?export=view&id=1TWpes3nKYbwSWyUj0uG0v5p-_a_zaVVh",
                options = listOf("ki", "ka", "ku", "ke"),
                title = "Final Quiz Hiragana",
                explanation = "Chọn nghĩa đúng với chữ 'き' trong từ 'きもの'.",
                romanji = "ki",
                kana = "き"
            ),

            // Katakana course
//            Exercise(
//                subLessonId = "k1-2",
//                question = "Katakana thường được sử dụng để viết gì?",
//                answer = "Từ ngoại lai",
//                options = listOf("Từ ngoại lai", "Tên người Nhật", "Động từ tiếng Nhật", "Tính từ tiếng Nhật"),
//                type = ExerciseType.PRACTICE
//            ),
//            Exercise(
//                subLessonId = "k1-2",
//                question = "Katakana có bao nhiêu ký tự cơ bản?",
//                answer = "46",
//                options = listOf("26", "36", "46", "56"),
//                type = ExerciseType.PRACTICE
//            ),
//            Exercise(
//                subLessonId = "k1-2",
//                question = "Katakana là một trong mấy bảng chữ cái của tiếng Nhật?",
//                answer = "3",
//                options = listOf("2", "3", "4", "5"),
//                type = ExerciseType.PRACTICE
//            ),
//            Exercise(
//                subLessonId = "k2-2",
//                question = "Chữ nào sau đây là 'a' trong Katakana?",
//                answer = "ア",
//                options = listOf("ア", "イ", "ウ", "エ"),
//                type = ExerciseType.PRACTICE
//            ),
//            Exercise(
//                subLessonId = "k2-2",
//                question = "Chữ 'エ' phát âm như thế nào?",
//                answer = "e",
//                options = listOf("a", "i", "u", "e"),
//                type = ExerciseType.PRACTICE
//            ),
//            Exercise(
//                subLessonId = "k2-2",
//                question = "Từ 'アイス' có nghĩa là gì?",
//                answer = "Kem",
//                options = listOf("Nước", "Kem", "Trà", "Cà phê"),
//                type = ExerciseType.PRACTICE
//            ),
//            Exercise(
//                subLessonId = "k2-3",
//                question = "Viết 'anime' bằng Katakana",
//                answer = "アニメ",
//                options = listOf("アニメ", "アネメ", "アニマ", "アネマ"),
//                type = ExerciseType.PRACTICE
//            ),
//            Exercise(
//                subLessonId = "k2-3",
//                question = "Viết 'auto' bằng Katakana",
//                answer = "アウト",
//                options = listOf("アウト", "アト", "アウトウ", "アトウ"),
//                type = ExerciseType.PRACTICE
//            ),
//            Exercise(
//                subLessonId = "k2-3",
//                question = "Viết 'area' bằng Katakana",
//                answer = "エリア",
//                options = listOf("アリア", "エリア", "アレア", "エレア"),
//                type = ExerciseType.PRACTICE
//            ),
//            Exercise(
//                subLessonId = "k3-2",
//                question = "Chữ nào sau đây là 'ka' trong Katakana?",
//                answer = "カ",
//                options = listOf("カ", "キ", "ク", "ケ"),
//                type = ExerciseType.PRACTICE
//            ),
//            Exercise(
//                subLessonId = "k3-2",
//                question = "Chữ 'コ' phát âm như thế nào?",
//                answer = "ko",
//                options = listOf("ka", "ki", "ku", "ko"),
//                type = ExerciseType.PRACTICE
//            ),
//            Exercise(
//                subLessonId = "k3-2",
//                question = "Từ 'カメラ' có nghĩa là gì?",
//                answer = "Máy ảnh",
//                options = listOf("Điện thoại", "Máy ảnh", "Máy tính", "Tivi"),
//                type = ExerciseType.PRACTICE
//            ),
//            Exercise(
//                subLessonId = "k3-3",
//                question = "Viết 'cake' bằng Katakana",
//                answer = "ケーキ",
//                options = listOf("ケーキ", "カケ", "ケキ", "カーキ"),
//                type = ExerciseType.PRACTICE
//            ),
//            Exercise(
//                subLessonId = "k3-3",
//                question = "Viết 'coffee' bằng Katakana",
//                answer = "コーヒー",
//                options = listOf("コーヒー", "カフィー", "コフィー", "カーヒー"),
//                type = ExerciseType.PRACTICE
//            ),
//            Exercise(
//                subLessonId = "k3-3",
//                question = "Viết 'key' bằng Katakana",
//                answer = "キー",
//                options = listOf("ケー", "キー", "カイ", "ケイ"),
//                type = ExerciseType.PRACTICE
//            ),
//            Exercise(
//                subLessonId = "k4-2",
//                question = "Chữ nào sau đây là 'sa' trong Katakana?",
//                answer = "サ",
//                options = listOf("サ", "シ", "ス", "セ"),
//                type = ExerciseType.PRACTICE
//            ),
//            Exercise(
//                subLessonId = "k4-2",
//                question = "Chữ 'ソ' phát âm như thế nào?",
//                answer = "so",
//                options = listOf("sa", "shi", "su", "so"),
//                type = ExerciseType.PRACTICE
//            ),
//            Exercise(
//                subLessonId = "k4-2",
//                question = "Từ 'サラダ' có nghĩa là gì?",
//                answer = "Salad",
//                options = listOf("Sandwich", "Salad", "Soup", "Sushi"),
//                type = ExerciseType.PRACTICE
//            ),
//            Exercise(
//                subLessonId = "k4-3",
//                question = "Viết 'service' bằng Katakana",
//                answer = "サービス",
//                options = listOf("サービス", "セルビス", "サビス", "セービス"),
//                type = ExerciseType.PRACTICE
//            ),
//            Exercise(
//                subLessonId = "k4-3",
//                question = "Viết 'system' bằng Katakana",
//                answer = "システム",
//                options = listOf("システム", "シスタム", "シテム", "スシテム"),
//                type = ExerciseType.PRACTICE
//            ),
//            Exercise(
//                subLessonId = "k4-3",
//                question = "Viết 'soup' bằng Katakana",
//                answer = "スープ",
//                options = listOf("ソープ", "スープ", "ソプ", "スプ"),
//                type = ExerciseType.PRACTICE
//            ),
//            Exercise(
//                subLessonId = "k5-2",
//                question = "Chữ nào sau đây là 'ta' trong Katakana?",
//                answer = "タ",
//                options = listOf("タ", "チ", "ツ", "テ"),
//                type = ExerciseType.PRACTICE
//            ),
//                    Exercise(
//            subLessonId = "k5-2",
//            question = "Chữ 'ト' phát âm như thế nào?",
//            answer = "to",
//            options = listOf("ta", "chi", "tsu", "to"),
//            type = ExerciseType.PRACTICE
//        ),
//
//           Exercise(
//            subLessonId = "k5-2",
//            question = "Từ 'タクシー' có nghĩa là gì?",
//            answer = "Taxi",
//            options = listOf("Tàu hỏa", "Taxi", "Xe buýt", "Xe đạp"),
//            type = ExerciseType.PRACTICE
//        ),
//
//            Exercise(
//            subLessonId = "k5-3",
//            question = "Viết 'table' bằng Katakana",
//            answer = "テーブル",
//            options = listOf("テーブル", "タブル", "テブル", "タブール"),
//            type = ExerciseType.PRACTICE
//        ),
//
//            Exercise(
//            subLessonId = "k5-3",
//            question = "Viết 'tomato' bằng Katakana",
//            answer = "トマト",
//            options = listOf("トマト", "タマト", "トメト", "タメト"),
//            type = ExerciseType.PRACTICE
//        ),
//
//            Exercise(
//            subLessonId = "k5-3",
//            question = "Viết 'ticket' bằng Katakana",
//            answer = "チケット",
//            options = listOf("チケット", "ティケット", "チケト", "ティケト"),
//            type = ExerciseType.PRACTICE
//        ),
//
//            Exercise(
//            subLessonId = "k6-2",
//            question = "Chữ nào sau đây là 'na' trong Katakana?",
//            answer = "ナ",
//            options = listOf("ナ", "ニ", "ヌ", "ネ"),
//            type = ExerciseType.PRACTICE
//        ),
//
//            Exercise(
//            subLessonId = "k6-2",
//            question = "Chữ 'ノ' phát âm như thế nào?",
//            answer = "no",
//            options = listOf("na", "ni", "nu", "no"),
//            type = ExerciseType.PRACTICE
//        ),
//
//            Exercise(
//            subLessonId = "k6-2",
//            question = "Từ 'ナイフ' có nghĩa là gì?",
//            answer = "Dao",
//            options = listOf("Nĩa", "Dao", "Thìa", "Đũa"),
//            type = ExerciseType.PRACTICE
//        ),
//
//            Exercise(
//            subLessonId = "k6-3",
//            question = "Viết 'note' bằng Katakana",
//            answer = "ノート",
//            options = listOf("ノート", "ナト", "ノト", "ナート"),
//            type = ExerciseType.PRACTICE
//        ),
//
//            Exercise(
//            subLessonId = "k6-3",
//            question = "Viết 'news' bằng Katakana",
//            answer = "ニュース",
//            options = listOf("ニュース", "ネウス", "ニウス", "ネース"),
//            type = ExerciseType.PRACTICE
//        ),
//
//            Exercise(
//            subLessonId = "k6-3",
//            question = "Viết 'necktie' bằng Katakana",
//            answer = "ネクタイ",
//            options = listOf("ネクタイ", "ナクタイ", "ネクティ", "ナクティ"),
//            type = ExerciseType.PRACTICE
//        ),
//
//            Exercise(
//            subLessonId = "k7-2",
//            question = "Chữ nào sau đây là 'ha' trong Katakana?",
//            answer = "ハ",
//            options = listOf("ハ", "ヒ", "フ", "ヘ"),
//            type = ExerciseType.PRACTICE
//        ),
//
//            Exercise(
//            subLessonId = "k7-2",
//            question = "Chữ 'ホ' phát âm như thế nào?",
//            answer = "ho",
//            options = listOf("ha", "hi", "fu", "ho"),
//            type = ExerciseType.PRACTICE
//        ),
//
//            Exercise(
//            subLessonId = "k7-2",
//            question = "Từ 'ホテル' có nghĩa là gì?",
//            answer = "Khách sạn",
//            options = listOf("Nhà hàng", "Khách sạn", "Bệnh viện", "Trường học"),
//            type = ExerciseType.PRACTICE
//        ),
//
//                Exercise(
//                    subLessonId = "k7-3",
//                    question = "Viết 'hamburger' bằng Katakana",
//                    answer = "ハンバーガー",
//                    options = listOf("ハンバーガー", "ハムバーガー", "ハンバガー", "ハムバガー"),
//                    type = ExerciseType.PRACTICE
//                ),
//        Exercise(
//            subLessonId = "k7-3",
//            question = "Viết 'helicopter' bằng Katakana",
//            answer = "ヘリコプター",
//            options = listOf("ヘリコプター", "ハリコプター", "ヘリコプタ", "ハリコプタ"),
//            type = ExerciseType.PRACTICE
//        ),
//        Exercise(
//            subLessonId = "k7-3",
//            question = "Viết 'fork' bằng Katakana",
//            answer = "フォーク",
//            options = listOf("フォーク", "ホーク", "フォク", "ホク"),
//            type = ExerciseType.PRACTICE
//        ),
//
//            // k8-2
//        Exercise(
//            subLessonId = "k8-2",
//            question = "Chữ nào sau đây là 'ma' trong Katakana?",
//            answer = "マ",
//            options = listOf("マ", "ミ", "ム", "メ"),
//            type = ExerciseType.PRACTICE
//        ),
//        Exercise(
//            subLessonId = "k8-2",
//            question = "Chữ 'モ' phát âm như thế nào?",
//            answer = "mo",
//            options = listOf("ma", "mi", "mu", "mo"),
//            type = ExerciseType.PRACTICE
//        ),
//        Exercise(
//            subLessonId = "k8-2",
//            question = "Từ 'ミルク' có nghĩa là gì?",
//            answer = "Sữa",
//            options = listOf("Nước", "Sữa", "Trà", "Cà phê"),
//            type = ExerciseType.PRACTICE
//        ),
//
//        Exercise(
//            subLessonId = "k8-3",
//            question = "Viết 'menu' bằng Katakana",
//            answer = "メニュー",
//            options = listOf("メニュー", "マニュー", "メヌー", "マヌー"),
//            type = ExerciseType.PRACTICE
//        ),
//        Exercise(
//            subLessonId = "k8-3",
//            question = "Viết 'model' bằng Katakana",
//            answer = "モデル",
//            options = listOf("モデル", "マデル", "モダル", "マダル"),
//            type = ExerciseType.PRACTICE
//        ),
//        Exercise(
//            subLessonId = "k8-3",
//            question = "Viết 'music' bằng Katakana",
//            answer = "ミュージック",
//            options = listOf("ミュージック", "ムジック", "ミュシック", "ムシック"),
//            type = ExerciseType.PRACTICE
//        ),
//
//        Exercise(
//            subLessonId = "k9-2",
//            question = "Chữ nào sau đây là 'ya' trong Katakana?",
//            answer = "ヤ",
//            options = listOf("ヤ", "ユ", "ヨ", "ワ"),
//            type = ExerciseType.PRACTICE
//        ),
//        Exercise(
//            subLessonId = "k9-2",
//            question = "Chữ 'ヨ' phát âm như thế nào?",
//            answer = "yo",
//            options = listOf("ya", "yu", "yo", "wa"),
//            type = ExerciseType.PRACTICE
//        ),
//        Exercise(
//            subLessonId = "k9-2",
//            question = "Từ 'ヨーグルト' có nghĩa là gì?",
//            answer = "Sữa chua",
//            options = listOf("Kem", "Sữa chua", "Bánh ngọt", "Bánh quy"),
//            type = ExerciseType.PRACTICE
//        ),
//
//        Exercise(
//            subLessonId = "k9-3",
//            question = "Viết 'yogurt' bằng Katakana",
//            answer = "ヨーグルト",
//            options = listOf("ヨーグルト", "ヤグルト", "ヨグルト", "ヤーグルト"),
//            type = ExerciseType.PRACTICE
//        ),
//        Exercise(
//            subLessonId = "k9-3",
//            question = "Viết 'yacht' bằng Katakana",
//            answer = "ヨット",
//            options = listOf("ヨット", "ヤット", "ヨト", "ヤト"),
//            type = ExerciseType.PRACTICE
//        ),
//        Exercise(
//            subLessonId = "k9-3",
//            question = "Viết 'uniform' bằng Katakana",
//            answer = "ユニフォーム",
//            options = listOf("ユニフォーム", "ヨニフォーム", "ユニホーム", "ヨニホーム"),
//            type = ExerciseType.PRACTICE
//        ),
//
//        Exercise(
//            subLessonId = "k10-2",
//            question = "Chữ nào sau đây là 'ra' trong Katakana?",
//            answer = "ラ",
//            options = listOf("ラ", "リ", "ル", "レ"),
//            type = ExerciseType.PRACTICE
//        ),
//        Exercise(
//            subLessonId = "k10-2",
//            question = "Chữ 'ロ' phát âm như thế nào?",
//            answer = "ro",
//            options = listOf("ra", "ri", "ru", "ro"),
//            type = ExerciseType.PRACTICE
//        ),
//        Exercise(
//            subLessonId = "k10-2",
//            question = "Từ 'レストラン' có nghĩa là gì?",
//            answer = "Nhà hàng",
//            options = listOf("Khách sạn", "Nhà hàng", "Cửa hàng", "Siêu thị"),
//            type = ExerciseType.PRACTICE
//        ),
//
//        Exercise(
//            subLessonId = "k10-3",
//            question = "Viết 'radio' bằng Katakana",
//            answer = "ラジオ",
//            options = listOf("ラジオ", "ラディオ", "ラジョ", "ラディョ"),
//            type = ExerciseType.PRACTICE
//        ),
//        Exercise(
//            subLessonId = "k10-3",
//            question = "Viết 'robot' bằng Katakana",
//            answer = "ロボット",
//            options = listOf("ロボット", "ラボット", "ロボト", "ラボト"),
//            type = ExerciseType.PRACTICE
//        ),
//        Exercise(
//            subLessonId = "k10-3",
//            question = "Viết 'lemon' bằng Katakana",
//            answer = "レモン",
//            options = listOf("レモン", "ラモン", "レマン", "ラマン"),
//            type = ExerciseType.PRACTICE
//        ),
//
//        Exercise(
//            subLessonId = "k11-2",
//            question = "Chữ nào sau đây là 'wa' trong Katakana?",
//            answer = "ワ",
//            options = listOf("ワ", "ヲ", "ン", "ラ"),
//            type = ExerciseType.PRACTICE
//        ),
//        Exercise(
//            subLessonId = "k11-2",
//            question = "Chữ 'ン' phát âm như thế nào?",
//            answer = "n",
//            options = listOf("wa", "wo", "n", "ra"),
//            type = ExerciseType.PRACTICE
//        ),
//        Exercise(
//            subLessonId = "k11-2",
//            question = "Từ 'ワイン' có nghĩa là gì?",
//            answer = "Rượu vang",
//            options = listOf("Nước", "Rượu vang", "Bia", "Nước ngọt"),
//            type = ExerciseType.PRACTICE
//        ),
//
//        Exercise(
//            subLessonId = "k11-3",
//            question = "Viết 'wine' bằng Katakana",
//            answer = "ワイン",
//            options = listOf("ワイン", "ウイン", "ワイヌ", "ウイヌ"),
//            type = ExerciseType.PRACTICE
//        ),
//        Exercise(
//            subLessonId = "k11-3",
//            question = "Viết 'one-piece' bằng Katakana",
//            answer = "ワンピース",
//            options = listOf("ワンピース", "ウンピース", "ワンピス", "ウンピス"),
//            type = ExerciseType.PRACTICE
//        ),
//        Exercise(
//            subLessonId = "k11-3",
//            question = "Viết 'pen' bằng Katakana",
//            answer = "ペン",
//            options = listOf("ペン", "パン", "ペヌ", "パヌ"),
//            type = ExerciseType.PRACTICE
//        ),
//
//        Exercise(
//            subLessonId = "k12-2",
//            question = "Dakuten (゛) biến đổi âm nào thành âm nào?",
//            answer = "Âm vô thanh thành âm hữu thanh",
//            options = listOf(
//                "Âm vô thanh thành âm hữu thanh",
//                "Âm hữu thanh thành âm vô thanh",
//                "Âm 'h' thành âm 'p'",
//                "Âm 'p' thành âm 'h'"
//            ),
//            type = ExerciseType.PRACTICE
//        ),
//        Exercise(
//            subLessonId = "k12-2",
//            question = "Handakuten (゜) biến đổi âm nào thành âm nào?",
//            answer = "Âm 'h' thành âm 'p'",
//            options = listOf(
//                "Âm vô thanh thành âm hữu thanh",
//                "Âm hữu thanh thành âm vô thanh",
//                "Âm 'h' thành âm 'p'",
//                "Âm 'p' thành âm 'h'"
//            ),
//            type = ExerciseType.PRACTICE
//        ),
//        Exercise(
//            subLessonId = "k12-2",
//            question = "Từ 'バス' có nghĩa là gì?",
//            answer = "Xe buýt",
//            options = listOf("Xe buýt", "Tàu hỏa", "Máy bay", "Xe đạp"),
//            type = ExerciseType.PRACTICE
//        ),
//
//            // k12-3
//        Exercise(
//            subLessonId = "k12-3",
//            question = "Viết 'bus' bằng Katakana",
//            answer = "バス",
//            options = listOf("バス", "ハス", "ブス", "フス"),
//            type = ExerciseType.PRACTICE
//        ),
//        Exercise(
//            subLessonId = "k12-3",
//            question = "Viết 'page' bằng Katakana",
//            answer = "ページ",
//            options = listOf("ページ", "ハージ", "ペジ", "ヘジ"),
//            type = ExerciseType.PRACTICE
//        ),
//        Exercise(
//            subLessonId = "k12-3",
//            question = "Viết 'juice' bằng Katakana",
//            answer = "ジュース",
//            options = listOf("ジュース", "シュース", "ジュス", "シュス"),
//            type = ExerciseType.PRACTICE
//        ),
//
//        Exercise(
//            subLessonId = "k13-2",
//            question = "Âm dài trong Katakana được biểu thị bằng ký hiệu nào?",
//            answer = "ー",
//            options = listOf("ー", "゛", "゜", "ッ"),
//            type = ExerciseType.PRACTICE
//        ),
//        Exercise(
//            subLessonId = "k13-2",
//            question = "Phụ âm kép trong Katakana được biểu thị bằng ký hiệu nào?",
//            answer = "ッ",
//            options = listOf("ー", "゛", "゜", "ッ"),
//            type = ExerciseType.PRACTICE
//        ),
//        Exercise(
//            subLessonId = "k13-2",
//            question = "Từ 'コンピューター' có nghĩa là gì?",
//            answer = "Máy tính",
//            options = listOf("Điện thoại", "Máy tính", "Máy ảnh", "Tivi"),
//            type = ExerciseType.PRACTICE
//        ),
//
//            // k13-3
//        Exercise(
//            subLessonId = "k13-3",
//            question = "Viết 'computer' bằng Katakana",
//            answer = "コンピューター",
//            options = listOf("コンピューター", "コンピュータ", "カンピューター", "カンピュータ"),
//            type = ExerciseType.PRACTICE
//        ),
//        Exercise(
//            subLessonId = "k13-3",
//            question = "Viết 'smartphone' bằng Katakana",
//            answer = "スマートフォン",
//            options = listOf("スマートフォン", "スマトフォン", "スマートホン", "スマトホン"),
//            type = ExerciseType.PRACTICE
//        ),
//        Exercise(
//            subLessonId = "k13-3",
//            question = "Viết 'television' bằng Katakana",
//            answer = "テレビ",
//            options = listOf("テレビ", "テレビジョン", "タレビ", "タレビジョン"),
//            type = ExerciseType.PRACTICE
//        ),
//                Exercise(
//                    subLessonId = "k14-2",
//                    question = "Âm 'yi' trong tiếng Nhật thường được viết bằng Katakana như thế nào?",
//                    answer = "イィ",
//                    options = listOf("イィ", "ヰ", "イエ", "ヱイ"),
//                    type = ExerciseType.PRACTICE
//                ),
//        Exercise(
//            subLessonId = "k14-2",
//            question = "Âm 'wi' trong tiếng Nhật thường được viết bằng Katakana như thế nào?",
//            answer = "ウィ",
//            options = listOf("ウィ", "ヰ", "ワイ", "ウイ"),
//            type = ExerciseType.PRACTICE
//        ),
//        Exercise(
//            subLessonId = "k14-2",
//            question = "Âm 'she' trong tiếng Nhật thường được viết bằng Katakana như thế nào?",
//            answer = "シェ",
//            options = listOf("シェ", "セ", "シエ", "セイ"),
//            type = ExerciseType.PRACTICE
//        ),
//        Exercise(
//            subLessonId = "k14-2",
//            question = "Âm 'je' trong tiếng Nhật thường được viết bằng Katakana như thế nào?",
//            answer = "ジェ",
//            options = listOf("ジェ", "ゼ", "ジエ", "ゼイ"),
//            type = ExerciseType.PRACTICE
//        ),
//        Exercise(
//            subLessonId = "k14-2",
//            question = "Âm 'ti' trong tiếng Nhật thường được viết bằng Katakana như thế nào?",
//            answer = "ティ",
//            options = listOf("ティ", "チ", "タイ", "テイ"),
//            type = ExerciseType.PRACTICE
//        ),
//        Exercise(
//            subLessonId = "k14-2",
//            question = "Âm 'fa' trong tiếng Nhật thường được viết bằng Katakana như thế nào?",
//            answer = "ファ",
//            options = listOf("ファ", "ハ", "フア", "ハー"),
//            type = ExerciseType.PRACTICE
//        ),
//        Exercise(
//            subLessonId = "k14-2",
//            question = "Âm 'va' trong tiếng Nhật thường được viết bằng Katakana như thế nào?",
//            answer = "ヴァ",
//            options = listOf("ヴァ", "バ", "ヴア", "ヴ"),
//            type = ExerciseType.PRACTICE
//        ),
//        Exercise(
//            subLessonId = "k14-2",
//            question = "Từ 'Twitter' được viết bằng Katakana như thế nào?",
//            answer = "ツイッター",
//            options = listOf("ツイッター", "ツウィター", "トゥイッター", "トゥウィター"),
//            type = ExerciseType.PRACTICE
//        ),
//        Exercise(
//            subLessonId = "k14-2",
//            question = "Từ 'Facebook' được viết bằng Katakana như thế nào?",
//            answer = "フェイスブック",
//            options = listOf("フェイスブック", "フェースブック", "フェイスボック", "フェースボック"),
//            type = ExerciseType.PRACTICE
//        ),
//        Exercise(
//            subLessonId = "k14-2",
//            question = "Từ 'Valentine' được viết bằng Katakana như thế nào?",
//            answer = "バレンタイン",
//            options = listOf("バレンタイン", "ヴァレンタイン", "バレンティン", "ヴァレンティン"),
//            type = ExerciseType.PRACTICE
//        )



            )


        else -> emptyList()
    }
}
