package com.example.nihongo.User.utils

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.nihongo.Admin.AdminLoginScreen
import com.example.nihongo.Admin.ui.CoursePage
import com.example.nihongo.Admin.ui.MainPage
import com.example.nihongo.Admin.ui.VipRequestPage
import com.example.nihongo.User.data.models.Exercise
import com.example.nihongo.User.data.repository.AIRepository
import com.example.nihongo.User.data.repository.CourseRepository
import com.example.nihongo.User.data.repository.ExerciseRepository
import com.example.nihongo.User.data.repository.LessonRepository
import com.example.nihongo.User.data.repository.UserRepository
import com.example.nihongo.User.ui.components.BottomNavItem
import com.example.nihongo.User.ui.screens.chat.CreateDiscussionScreen
import com.example.nihongo.User.ui.screens.chat.DiscussionChatScreen
import com.example.nihongo.User.ui.screens.chat.GroupChatScreen
import com.example.nihongo.User.ui.screens.chat.PrivateChatScreen
import com.example.nihongo.User.ui.screens.homepage.CommunityScreen
import com.example.nihongo.User.ui.screens.homepage.CourseDetailScreen
import com.example.nihongo.User.ui.screens.homepage.CoursesScreen
import com.example.nihongo.User.ui.screens.homepage.ExerciseScreen
import com.example.nihongo.User.ui.screens.homepage.FlashcardScreen
import com.example.nihongo.User.ui.screens.homepage.HomeScreen
import com.example.nihongo.User.ui.screens.homepage.ProfileScreen
import com.example.nihongo.User.ui.screens.homepage.QuizScreen
import com.example.nihongo.User.ui.screens.login.LoginScreen
import com.example.nihongo.User.ui.screens.login.OTPScreen
import com.example.nihongo.User.ui.screens.login.RegisterScreen
import com.example.nihongo.ui.screens.homepage.LessonsScreen
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun AppNavGraph(
    navController: NavHostController,
    userRepo: UserRepository,
    courseRepo: CourseRepository,
    lessonRepo: LessonRepository,
    exerciseRepo: ExerciseRepository,
    aiRepo : AIRepository,
    startDestination: String = NavigationRoutes.LOGIN
) {
    NavHost(navController = navController, startDestination = startDestination) {

        // Auth
        composable(NavigationRoutes.LOGIN) {
            LoginScreen(navController, userRepo)
        }

        composable(NavigationRoutes.REGISTER) {
            RegisterScreen(navController, userRepo)
        }

        composable("otp_screen") {
            val expectedOtp = navController.previousBackStackEntry?.savedStateHandle?.get<String>("expectedOtp") ?: ""
            val userEmail = navController.previousBackStackEntry?.savedStateHandle?.get<String>("user_email") ?: ""
            OTPScreen(navController, expectedOtp, userEmail)
        }

        // Bottom Nav Screens
        composable("${BottomNavItem.Home.route}/{user_email}",
            arguments = listOf(navArgument("user_email") { type = NavType.StringType })
        ) { backStackEntry ->
            val userEmail = backStackEntry.arguments?.getString("user_email") ?: ""
            HomeScreen(navController, userEmail, userRepo, courseRepo)
        }

        composable("${BottomNavItem.Courses.route}/{user_email}",
            arguments = listOf(navArgument("user_email") { type = NavType.StringType })
        ) { backStackEntry ->
            val userEmail = backStackEntry.arguments?.getString("user_email") ?: ""
            CoursesScreen(navController, courseRepo, userEmail)
        }

        composable("${BottomNavItem.Profile.route}/{user_email}",
            arguments = listOf(navArgument("user_email") { type = NavType.StringType })
        ) { backStackEntry ->
            val userEmail = backStackEntry.arguments?.getString("user_email") ?: ""
            ProfileScreen(
                navController = navController,
                userEmail = userEmail,
                userRepository = userRepo,
                courseRepository = courseRepo,
                onSignOut = {
                    navController.navigate(NavigationRoutes.LOGIN) {
                        popUpTo(0)
                    }
                    CoroutineScope(Dispatchers.IO).launch {
                        val currentUser = userRepo.getUserByEmail(userEmail)
                        val idUser = currentUser?.id
                        FirebaseMessaging.getInstance().unsubscribeFromTopic("$idUser")
                            .addOnCompleteListener { task ->
                                val msg = if (task.isSuccessful) {
                                    "Unsubscribed from topic"
                                } else {
                                    "Unsubscription failed"
                                }
                                Log.d("FCM", msg)
                            }
                    }
                }
            )
        }
        composable(
            "community/{user_email}?tab={tab}",
            arguments = listOf(
                navArgument("user_email") { type = NavType.StringType },
                navArgument("tab") { 
                    type = NavType.IntType 
                    defaultValue = 0
                }
            )
        ) { backStackEntry ->
            val userEmail = backStackEntry.arguments?.getString("user_email") ?: ""
            val initialTab = backStackEntry.arguments?.getInt("tab") ?: 0
            CommunityScreen(navController, userRepo, userEmail, initialTab)
        }




        composable("courses/{courseId}/{user_email}") { backStackEntry ->
            val courseId = backStackEntry.arguments?.getString("courseId")
            val userEmail = backStackEntry.arguments?.getString("user_email") ?: ""
            if (courseId != null) {
                CourseDetailScreen(courseId, navController, courseRepo, userRepo, lessonRepo, userEmail)
            } else {
                InvalidCourseScreen()
            }
        }

        composable("lessons/{courseId}/{user_email}",
            arguments = listOf(
                navArgument("courseId") { type = NavType.StringType },
                navArgument("user_email") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val courseId = backStackEntry.arguments?.getString("courseId") ?: ""
            val userEmail = backStackEntry.arguments?.getString("user_email") ?: ""
            LessonsScreen(courseId, userEmail, navController, courseRepo, lessonRepo, userRepo)
        }

        composable("exercise/{courseId}/{lessonId}/{sublessonId}/{user_email}",
            arguments = listOf(
                navArgument("courseId") { type = NavType.StringType },
                navArgument("lessonId") { type = NavType.StringType },
                navArgument("sublessonId") { type = NavType.StringType },
                navArgument("user_email") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val courseId = backStackEntry.arguments?.getString("courseId") ?: ""
            val lessonId = backStackEntry.arguments?.getString("lessonId")
            val sublessonId = backStackEntry.arguments?.getString("sublessonId")
            val userEmail = backStackEntry.arguments?.getString("user_email") ?: ""
            if (!lessonId.isNullOrBlank() && !sublessonId.isNullOrBlank()) {
                ExerciseScreen(navController, sublessonId, exerciseRepo, courseId,  lessonId, userEmail)
            } else {
                Text("Không tìm thấy bài tập, vui lòng thử lại.")
            }
        }

        composable("quiz_screen/{user_email}/{courseId}/{lessonId}",
            arguments = listOf(navArgument("user_email") { type = NavType.StringType })
        ) { backStackEntry ->
            val courseId = backStackEntry.arguments?.getString("courseId") ?: ""
            val lessonId = backStackEntry.arguments?.getString("lessonId") ?: ""
            val quizExercises = navController.previousBackStackEntry?.savedStateHandle?.get<List<Exercise>>("quizList") ?: emptyList()
            val userEmail = backStackEntry.arguments?.getString("user_email") ?: ""
            QuizScreen(quizExercises, userEmail,courseId,lessonId, navController)
        }
        composable(
            "vocabulary/{user_email}?tab={tab}",
            arguments = listOf(
                navArgument("user_email") { type = NavType.StringType },
                navArgument("tab") { 
                    type = NavType.StringType 
                    nullable = true
                    defaultValue = null
                }
            )
        ) { backStackEntry ->
            val userEmail = backStackEntry.arguments?.getString("user_email") ?: ""
            val tab = backStackEntry.arguments?.getString("tab")
            FlashcardScreen(navController, userEmail, tab)
        }

        // Thêm route cho màn hình chat nhóm
        composable(
            "group_chat/{groupId}/{user_email}",
            arguments = listOf(
                navArgument("groupId") { type = NavType.StringType },
                navArgument("user_email") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val groupId = backStackEntry.arguments?.getString("groupId") ?: ""
            val userEmail = backStackEntry.arguments?.getString("user_email") ?: ""
            GroupChatScreen(
                navController = navController,
                groupId = groupId,
                userEmail = userEmail,
                userRepository = userRepo,
                aiRepository = aiRepo
            )
        }

        // Thêm route cho màn hình chat cá nhân
        composable(
            "private_chat/{partnerId}/{user_email}",
            arguments = listOf(
                navArgument("partnerId") { type = NavType.StringType },
                navArgument("user_email") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val partnerId = backStackEntry.arguments?.getString("partnerId") ?: ""
            val userEmail = backStackEntry.arguments?.getString("user_email") ?: ""
            PrivateChatScreen(
                navController = navController,
                partnerUserId = partnerId,
                userEmail = userEmail,
                userRepository = userRepo
            )
        }

        // Thêm route cho màn hình chat thảo luận
        composable(
            "discussion_chat/{discussionId}/{user_email}",
            arguments = listOf(
                navArgument("discussionId") { type = NavType.StringType },
                navArgument("user_email") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val discussionId = backStackEntry.arguments?.getString("discussionId") ?: ""
            val userEmail = backStackEntry.arguments?.getString("user_email") ?: ""
            DiscussionChatScreen(
                navController = navController,
                discussionId = discussionId,
                userEmail = userEmail,
                userRepository = userRepo
            )
        }

        // Thêm route cho màn hình tạo thảo luận mới
        composable(
            "create_discussion/{user_email}",
            arguments = listOf(
                navArgument("user_email") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val userEmail = backStackEntry.arguments?.getString("user_email") ?: ""
            CreateDiscussionScreen(
                navController = navController,
                userEmail = userEmail,
                userRepository = userRepo
            )
        }
// Admin
        composable("course_page") {
            CoursePage()
        }
        composable("vipRequestPage") {
            VipRequestPage(navController = navController)
        }
        composable("admin_login") {
            AdminLoginScreen(navController)
        }
        composable("MainPage") {
            MainPage(navController)
        }

    }
}

@Composable
fun InvalidCourseScreen() {
    Text("Invalid course ID.")
}
