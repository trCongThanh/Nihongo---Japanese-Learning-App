package com.example.nihongo.User

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.core.content.ContextCompat
import androidx.navigation.compose.rememberNavController
import com.example.nihongo.User.data.repository.AIRepository
import com.example.nihongo.User.data.repository.CourseRepository
import com.example.nihongo.User.data.repository.ExerciseRepository
import com.example.nihongo.User.data.repository.LessonRepository
import com.example.nihongo.User.data.repository.UserRepository
import com.example.nihongo.User.ui.components.BottomNavItem
import com.example.nihongo.User.utils.AppNavGraph
import com.example.nihongo.User.utils.NavigationRoutes
import com.example.nihongo.User.utils.SessionManager
import com.example.nihongo.ui.theme.NihongoTheme
import com.google.firebase.messaging.FirebaseMessaging
import com.onesignal.OneSignal
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private var backPressedTime = 0L

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val sessionManager = SessionManager(this)
        val userRepo = UserRepository(sessionManager = sessionManager)
        val courseRepo = CourseRepository()
        val lessonRepo = LessonRepository()
        val exerciseRepo = ExerciseRepository()
        val aiRepo = AIRepository()

        // Check if user is already logged in
        val loggedInUser = sessionManager.getUserDetails()
        val startDestination = if (loggedInUser != null) {
            // Set the current user in repository
            CoroutineScope(Dispatchers.IO).launch {
                // Cập nhật trạng thái online của người dùng
                userRepo.updateUserOnlineStatus(loggedInUser.id, true)

                // Thiết lập tag user_email cho OneSignal
                OneSignal.User.addTag(loggedInUser.email, "true")
                Log.d("OneSignal", "Set user_email tag in MainActivity: ${loggedInUser.email}")
            }
            "home/${loggedInUser.email}"
        } else {
            NavigationRoutes.LOGIN
        }

        // Rest of your code...

        setContent {
            NihongoTheme {
                Surface(color = MaterialTheme.colorScheme.background) {
                    val navController = rememberNavController()

                    // Handle back press for the entire app
                    BackHandler {
                        val currentRoute = navController.currentBackStackEntry?.destination?.route

                        // If we're on a main screen (home, courses, profile, community)
                        if (currentRoute?.contains(BottomNavItem.Home.route) == true ||
                            currentRoute?.contains(BottomNavItem.Courses.route) == true ||
                            currentRoute?.contains(BottomNavItem.Profile.route) == true ||
                            currentRoute?.contains(BottomNavItem.Community.route) == true) {

                            // Implement double back press to exit
                            if (backPressedTime + 2000 > System.currentTimeMillis()) {
                                finish()
                            } else {
                                Toast.makeText(this@MainActivity, "Press back again to exit", Toast.LENGTH_SHORT).show()
                                backPressedTime = System.currentTimeMillis()
                            }
                        } else {
                            // For other screens, allow normal back navigation
                            navController.popBackStack()
                        }
                    }

                    AppNavGraph(
                        navController = navController,
                        userRepo = userRepo,
                        courseRepo = courseRepo,
                        lessonRepo = lessonRepo,
                        exerciseRepo = exerciseRepo,
                        aiRepo = aiRepo,
                        startDestination = startDestination
                    )
                }
            }
        }

        requestNotificationPermissionIfNeeded()
        FirebaseMessaging.getInstance().subscribeToTopic("all")
            .addOnCompleteListener { task ->
                var msg = "Subscribed to topic"
                if (!task.isSuccessful) {
                    msg = "Subscription failed"
                }
                Log.d("FCM", msg)
            }
        OneSignal.initWithContext(this,"47f96538-4b2b-4933-999a-a9012080d4e9")
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    // Optional: handle result if needed
    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                Log.d("MainActivity", "Notification permission granted")
            } else {
                Log.w("MainActivity", "Notification permission denied")
            }
        }
}