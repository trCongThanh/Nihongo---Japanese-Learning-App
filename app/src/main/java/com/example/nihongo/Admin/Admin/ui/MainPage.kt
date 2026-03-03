package com.example.nihongo.Admin.ui

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LibraryBooks
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import coil.compose.AsyncImage
import com.example.nihongo.Admin.viewmodel.AdminMainPageViewModel
import com.example.nihongo.User.MainActivity
import com.example.nihongo.User.data.models.Course
import com.example.nihongo.User.data.models.User
import com.onesignal.OneSignal

// Define custom theme colors
val primaryGreen = Color(0xFF4CAF50)
val lightGreen = Color(0xFFDCEDC8)
val darkGreen = Color(0xFF2E7D32)
val backgroundColor = Color.White
val surfaceColor = Color.White
val cardBackgroundColor = Color(0xFFF9FFF5)

@Composable
fun MainPage(navController: NavHostController) {
    val innerNavController = rememberNavController()
    val selectedItemIndex = remember { mutableStateOf(0) }
    val adminViewModel: AdminMainPageViewModel = viewModel()

    val showTopBar = remember { mutableStateOf(true) }
    val showBottomBar = remember { mutableStateOf(true) }

    // Assign global state
    UIVisibilityController.showTopBarState = showTopBar
    UIVisibilityController.showBottomBarState = showBottomBar

    Scaffold(
        containerColor = backgroundColor,
        topBar = { if (showTopBar.value) TopBar(innerNavController) },
        bottomBar = { if (showBottomBar.value) BottomNavigationBar(innerNavController, selectedItemIndex)}
    ) { padding ->
        NavHost(innerNavController, startDestination = "mainPage") {
            composable("mainPage") {
                LaunchedEffect(Unit) {
                    OneSignal.User.addTag("admin", "true")
                    Log.d("AdminMainPage", "Set OneSignal tag: admin=true")
                    adminViewModel.loadAllData()
                }
                
                AdminDashboard(
                    modifier = Modifier.padding(padding),
                    viewModel = adminViewModel,
                    navController = navController
                )
                UIVisibilityController.enableDisplayTopBarAndBottom()
            }
            composable("CoursePage") {
                UIVisibilityController.enableDisplayTopBarAndBottom()
                CoursePage()
            }
            composable("NotifyPage") {
                NotifyPage()
                UIVisibilityController.disableDisplayTopBar()
            }
            composable("FlashcardPage") {
                UIVisibilityController.disableDisplayTopBar()
                FlashcardPage()
            }
            composable("userPage") {
                UIVisibilityController.enableDisplayTopBarAndBottom()
                userPage()
            }
            composable("vipRequestPage") {
                VipRequestPage(navController = innerNavController)
            }
            composable("ProfilePage") {
                UIVisibilityController.disableDisplayTopBarAndBottom()
                ProfilePage(navController)
            }
        }
    }
}

@Composable
fun AdminDashboard(modifier: Modifier = Modifier, viewModel: AdminMainPageViewModel, navController: NavHostController) {
    val isLoading by viewModel.isLoading
    val monthlyUserCount by viewModel.monthlyUserCount.collectAsState()
    val topUsers by viewModel.topUsers.collectAsState()
    val topCourses by viewModel.topCourses.collectAsState()
    val totalCourseCount by viewModel.totalCourseCount.collectAsState()
    val vipMemberCount by viewModel.vipMemberCount.collectAsState()
    val mostEnrolledCourses by viewModel.mostEnrolledCourses.collectAsState()
    
    // Add search state
    var searchQuery by remember { mutableStateOf("") }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .background(backgroundColor),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            SearchBar(
                onSearchQueryChange = { query ->
                    searchQuery = query
                }
            )
        }

        // Only show content based on search query
        if (searchQuery.isEmpty()) {
            // Show all content when no search
            item {
                DashboardHeader(totalCourseCount, vipMemberCount)
            }

            item {
                Text(
                    "Monthly User Growth",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = darkGreen
                )
                Spacer(modifier = Modifier.height(8.dp))
                MonthlyUserChart(monthlyUserCount)
            }

            item {
                Text(
                    "Top 5 Active Users",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = darkGreen
                )
                Spacer(modifier = Modifier.height(8.dp))
                TopUsersSection(topUsers)
            }

            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            try {
                                Log.d("AdminDashboard", "Navigating to vipRequestPage")
                                navController.navigate("vipRequestPage")
                            } catch (e: Exception) {
                                Log.e("AdminDashboard", "Navigation error: ${e.message}", e)
                            }
                        },
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = Color(0xFFFFC107),
                            modifier = Modifier.size(24.dp)
                        )

                        Spacer(modifier = Modifier.width(16.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Quản lý yêu cầu VIP",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "Xem và phê duyệt các yêu cầu nâng cấp VIP",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray
                            )
                        }

                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = Color.Gray
                        )
                    }
                }
            }

            item {
                Text(
                    "Most Liked Courses",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = darkGreen
                )
                Spacer(modifier = Modifier.height(8.dp))
                TopCoursesSection(topCourses)
            }

            item {
                Text(
                    "Most Enrolled Courses",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = darkGreen
                )
                Spacer(modifier = Modifier.height(8.dp))
                MostEnrolledCoursesSection(mostEnrolledCourses)
            }
        } else {
            // Show relevant content based on search
            val query = searchQuery.lowercase()
            
            // Check for user-related content
            if (query.contains("user") || query.contains("member")) {
                // Show Monthly User Growth
                item {
                    Text(
                        "Monthly User Growth",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = darkGreen
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    MonthlyUserChart(monthlyUserCount)
                }

                // Show Active Users
                item {
                    Text(
                        "Top 5 Active Users",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = darkGreen
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    TopUsersSection(topUsers)
                }

                // Show VIP Members if query contains "vip"
                if (query.contains("vip")) {
                    item {
                        DashboardHeader(totalCourseCount, vipMemberCount)
                    }
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    try {
                                        navController.navigate("vipRequestPage")
                                    } catch (e: Exception) {
                                        Log.e("AdminDashboard", "Navigation error: ${e.message}", e)
                                    }
                                },
                            colors = CardDefaults.cardColors(containerColor = Color.White)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Star,
                                    contentDescription = null,
                                    tint = Color(0xFFFFC107),
                                    modifier = Modifier.size(24.dp)
                                )

                                Spacer(modifier = Modifier.width(16.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        "Quản lý yêu cầu VIP",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        "Xem và phê duyệt các yêu cầu nâng cấp VIP",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.Gray
                                    )
                                }

                                Icon(
                                    imageVector = Icons.Default.ChevronRight,
                                    contentDescription = null,
                                    tint = Color.Gray
                                )
                            }
                        }
                    }
                }
            }

            // Check for course-related content
            if (query.contains("course") || query.contains("enroll") || query.contains("like")) {
                // Show Most Liked Courses
                if (query.contains("like") || query.contains("course")) {
                    item {
                        Text(
                            "Most Liked Courses",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = darkGreen
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        TopCoursesSection(topCourses)
                    }
                }

                // Show Most Enrolled Courses
                if (query.contains("enroll") || query.contains("course")) {
                    item {
                        Text(
                            "Most Enrolled Courses",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = darkGreen
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        MostEnrolledCoursesSection(mostEnrolledCourses)
                    }
                }
            }

            // Show VIP-related content
            if (query.contains("vip")) {
                item {
                    DashboardHeader(totalCourseCount, vipMemberCount)
                }
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                try {
                                    navController.navigate("vipRequestPage")
                                } catch (e: Exception) {
                                    Log.e("AdminDashboard", "Navigation error: ${e.message}", e)
                                }
                            },
                        colors = CardDefaults.cardColors(containerColor = Color.White)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                tint = Color(0xFFFFC107),
                                modifier = Modifier.size(24.dp)
                            )

                            Spacer(modifier = Modifier.width(16.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "Quản lý yêu cầu VIP",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    "Xem và phê duyệt các yêu cầu nâng cấp VIP",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.Gray
                                )
                            }

                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = null,
                                tint = Color.Gray
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DashboardHeader(totalCourseCount: Int, vipMemberCount: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        StatCard(
            modifier = Modifier.weight(1f),
            icon = Icons.Default.MenuBook,
            title = "Total Courses",
            value = totalCourseCount.toString(),
            backgroundColor = lightGreen
        )

        Spacer(modifier = Modifier.width(16.dp))

        StatCard(
            modifier = Modifier.weight(1f),
            icon = Icons.Default.Star,
            title = "VIP Members",
            value = vipMemberCount.toString(),
            backgroundColor = lightGreen
        )
    }
}

@Composable
fun StatCard(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    title: String,
    value: String,
    backgroundColor: Color
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = primaryGreen,
                modifier = Modifier.size(32.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = darkGreen
            )

            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = darkGreen
            )
        }
    }
}

@Composable
fun MonthlyUserChart(monthlyData: Map<String, Int>) {
    if (monthlyData.isEmpty()) {
        EmptyStateCard("No monthly user data available")
        return
    }

    val sortedData = monthlyData.toList()
    val maxValue = sortedData.maxOfOrNull { it.second } ?: 1

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = cardBackgroundColor),
        shape = RoundedCornerShape(12.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Draw the chart
            Canvas(
                modifier = Modifier.fillMaxWidth().height(150.dp)
            ) {
                val barWidth = size.width / sortedData.size
                val heightRatio = size.height / maxValue.toFloat()

                // Draw horizontal grid lines
                val gridLineCount = 5
                val gridLineSpacing = size.height / gridLineCount

                for (i in 0..gridLineCount) {
                    val y = size.height - (i * gridLineSpacing)
                    drawLine(
                        color = Color.LightGray,
                        start = Offset(0f, y),
                        end = Offset(size.width, y),
                        strokeWidth = 1f
                    )
                }

                // Draw bars
                sortedData.forEachIndexed { index, (_, value) ->
                    val barHeight = value * heightRatio
                    val x = index * barWidth + barWidth / 4

                    drawLine(
                        color = primaryGreen,
                        start = Offset(x + barWidth / 2, size.height),
                        end = Offset(x + barWidth / 2, size.height - barHeight),
                        strokeWidth = barWidth / 2,
                        cap = StrokeCap.Round
                    )
                }
            }

            // Draw month labels
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .offset(x = (10).dp,y = (16).dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                sortedData.forEach { (month, _) ->
                    Text(
                        text = month,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        color = darkGreen
                    )
                }
            }
        }
    }
}

@Composable
fun TopUsersSection(users: List<User>) {
    if (users.isEmpty()) {
        EmptyStateCard("No user data available")
        return
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = cardBackgroundColor),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            users.forEachIndexed { index, user ->
                UserRankItem(
                    rank = index + 1,
                    user = user,
                    modifier = Modifier.fillMaxWidth()
                )

                if (index < users.size - 1) {
                    Divider(
                        modifier = Modifier.padding(vertical = 8.dp),
                        color = lightGreen
                    )
                }
            }
        }
    }
}

@Composable
fun UserRankItem(rank: Int, user: User, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "$rank",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.width(32.dp),
            color = darkGreen
        )

        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(lightGreen),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = user.username.take(1).uppercase(),
                style = MaterialTheme.typography.titleMedium,
                color = darkGreen
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = user.username,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = darkGreen
            )

            Text(
                text = if (user.vip) "VIP Member" else "Standard Member",
                style = MaterialTheme.typography.bodySmall,
                color = if (user.vip) Color(0xFF4CAF50) else Color.Gray
            )
        }

        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = "${user.activityPoints}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = darkGreen
            )

            Text(
                text = "Points",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )
        }
    }
}

@Composable
fun TopCoursesSection(courses: List<Course>) {
    if (courses.isEmpty()) {
        EmptyStateCard("No course data available")
        return
    }

    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(vertical = 8.dp)
    ) {
        items(courses) { course ->
            CourseCard(
                course = course,
                modifier = Modifier.width(240.dp)
            )
        }
    }
}

@Composable
fun CourseCard(course: Course, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = cardBackgroundColor),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(lightGreen),
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = course.imageRes,
                    contentDescription = "Course Image",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = course.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = darkGreen
            )

            Spacer(modifier = Modifier.height(4.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = "Rating",
                    modifier = Modifier.size(16.dp),
                    tint = primaryGreen
                )

                Spacer(modifier = Modifier.width(4.dp))

                Text(
                    text = course.rating.toString(),
                    style = MaterialTheme.typography.bodySmall,
                    color = darkGreen
                )

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = "(${course.reviews})",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Favorite,
                    contentDescription = "Likes",
                    modifier = Modifier.size(16.dp),
                    tint = primaryGreen
                )

                Spacer(modifier = Modifier.width(4.dp))

                Text(
                    text = "${course.likes} likes",
                    style = MaterialTheme.typography.bodySmall,
                    color = darkGreen
                )

                Spacer(modifier = Modifier.weight(1f))

                if (course.vip) {
                    Surface(
                        color = primaryGreen,
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier.offset(y = (-5).dp)
                    ) {
                        Text(
                            text = "VIP",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 0.dp),
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MostEnrolledCoursesSection(courseEnrollments: List<AdminMainPageViewModel.CourseEnrollment>) {
    if (courseEnrollments.isEmpty()) {
        EmptyStateCard("No enrollment data available")
        return
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = cardBackgroundColor),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            courseEnrollments.forEachIndexed { index, enrollment ->
                CourseEnrollmentItem(
                    rank = index + 1,
                    enrollment = enrollment,
                    modifier = Modifier.fillMaxWidth()
                )

                if (index < courseEnrollments.size - 1) {
                    Divider(
                        modifier = Modifier.padding(vertical = 8.dp),
                        color = lightGreen
                    )
                }
            }
        }
    }
    // ✅ Box màu đỏ cao 8dp ở cuối
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(16.dp)
    )
}

@Composable
fun CourseEnrollmentItem(
    rank: Int,
    enrollment: AdminMainPageViewModel.CourseEnrollment,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "$rank",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.width(32.dp),
            color = darkGreen
        )

        Icon(
            imageVector = Icons.Default.MenuBook,
            contentDescription = "Course",
            tint = primaryGreen,
            modifier = Modifier.size(28.dp)
        )

        Spacer(modifier = Modifier.width(12.dp))

        Text(
            text = enrollment.title,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
            color = darkGreen
        )

        Text(
            text = "${enrollment.enrollmentCount}",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = darkGreen
        )

        Spacer(modifier = Modifier.width(4.dp))

        Text(
            text = "users",
            style = MaterialTheme.typography.bodySmall,
            color = Color.Gray
        )
    }
}

@Composable
fun EmptyStateCard(message: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = cardBackgroundColor),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(24.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = "Info",
                modifier = Modifier.size(48.dp),
                tint = primaryGreen
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = darkGreen,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun TopBar(navController: NavHostController) {
    var expanded by remember { mutableStateOf(false) }
    val context = LocalContext.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            "NIHONGO",
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp,
            color = darkGreen
        )
        Row {
            Icon(
                Icons.Default.Notifications,
                contentDescription = "Notifications",
                modifier = Modifier.padding(end = 16.dp),
                tint = primaryGreen
            )

            Box {
                Icon(
                    Icons.Default.Settings,
                    contentDescription = "Settings",
                    modifier = Modifier
                        .clickable { expanded = true },
                    tint = primaryGreen
                )

                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Profile", color = darkGreen) },
                        onClick = {
                            expanded = false
                            navController.navigate("ProfilePage")
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Logout", color = darkGreen) },
                        onClick = {
                            val sharedPref = context.getSharedPreferences("admin_session", Context.MODE_PRIVATE)
                            sharedPref.edit().clear().apply()

                            expanded = false
                            val intent = Intent(context, MainActivity::class.java)
                            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            context.startActivity(intent)
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchBar(
    onSearchQueryChange: (String) -> Unit
) {
    var text by remember { mutableStateOf("") }
    var showResults by remember { mutableStateOf(false) }
    val context = LocalContext.current

    // Animation for TextField color
    val textFieldColor by animateColorAsState(
        targetValue = if (text.isEmpty()) Color(0xFFF5FFF0) else Color(0xFFE8F5E9),
        animationSpec = tween(durationMillis = 500)
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
    ) {
        TextField(
            value = text,
            onValueChange = { 
                text = it
                showResults = it.isNotEmpty()
                onSearchQueryChange(it)
            },
            placeholder = { Text("Search...", color = Color.Gray) },
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp),
            leadingIcon = {
                Icon(Icons.Default.Search, contentDescription = "Search Icon", tint = primaryGreen)
            },
            shape = RoundedCornerShape(12.dp),
            colors = TextFieldDefaults.colors(
                unfocusedIndicatorColor       = Color.Transparent
            ,     focusedIndicatorColor         = Color.Transparent
                , unfocusedContainerColor       = textFieldColor
                , focusedContainerColor         = textFieldColor
                , cursorColor                   = primaryGreen
            )
        )
    }
}

@Composable
fun BottomNavigationBar(navController: NavController, selectedItemIndex: MutableState<Int>) {
    NavigationBar(
        containerColor = Color.White
    ) {
        NavigationBarItem(
            icon = { Icon(Icons.Default.Home, contentDescription = "Home", tint = if (selectedItemIndex.value == 0) primaryGreen else Color.Gray) },
            label = { Text("Home", fontSize = 10.sp, color = if (selectedItemIndex.value == 0) primaryGreen else Color.Gray) },
            selected = selectedItemIndex.value == 0,
            onClick = {
                navController.navigate("mainPage") {
                    launchSingleTop = true
                }
                selectedItemIndex.value = 0
            }
        )
        NavigationBarItem(
            icon = { Icon(Icons.Default.Folder, contentDescription = "Courses", tint = if (selectedItemIndex.value == 1) primaryGreen else Color.Gray) },
            label = { Text("Courses" , fontSize = 10.sp, color = if (selectedItemIndex.value == 1) primaryGreen else Color.Gray) },
            selected = selectedItemIndex.value == 1,
            onClick = {
                navController.navigate("CoursePage") {
                    launchSingleTop = true
                }
                selectedItemIndex.value = 1
            }
        )
        NavigationBarItem(
            icon = { Icon(Icons.Default.LibraryBooks, contentDescription = "FlashCard", tint = if (selectedItemIndex.value == 2) primaryGreen else Color.Gray) },
            label = { Text("FlashCard" , fontSize = 10.sp, color = if (selectedItemIndex.value == 2) primaryGreen else Color.Gray) },
            selected = selectedItemIndex.value == 2,
            onClick = {
                navController.navigate("FlashcardPage") {
                    launchSingleTop = true
                }
                selectedItemIndex.value = 2
            }
        )
        NavigationBarItem(
            icon = { Icon(Icons.Default.Notifications, contentDescription = "Notify", tint = if (selectedItemIndex.value == 3) primaryGreen else Color.Gray) },
            label = { Text("Notify" , fontSize = 10.sp, color = if (selectedItemIndex.value == 3) primaryGreen else Color.Gray) },
            selected = selectedItemIndex.value == 3,
            onClick = {
                navController.navigate("NotifyPage") {
                    launchSingleTop = true
                }
                selectedItemIndex.value = 3
            }
        )
        NavigationBarItem(
            icon = { Icon(Icons.Default.Person, contentDescription = "Users", tint = if (selectedItemIndex.value == 4) primaryGreen else Color.Gray) },
            label = { Text("Users" , fontSize = 10.sp, color = if (selectedItemIndex.value == 4) primaryGreen else Color.Gray) },
            selected = selectedItemIndex.value == 4,
            onClick = {
                navController.navigate("userPage") {
                    launchSingleTop = true
                }
                selectedItemIndex.value = 4
            }
        )
    }
}

// Global controller
object UIVisibilityController {
    var showTopBarState: MutableState<Boolean>? = null
    var showBottomBarState: MutableState<Boolean>? = null

    fun disableDisplayTopBar() {
        showTopBarState?.value = false
    }

    fun disableDisplayBottomBar() {
        showBottomBarState?.value = false
    }

    fun enableDisplayTopBar() {
        showTopBarState?.value = true
    }

    fun enableDisplayBottomBar() {
        showBottomBarState?.value = true
    }

    fun enableDisplayTopBarAndBottom() {
        showTopBarState?.value = true
        showBottomBarState?.value = true
    }

    fun disableDisplayTopBarAndBottom() {
        showTopBarState?.value = false
        showBottomBarState?.value = false
    }
}