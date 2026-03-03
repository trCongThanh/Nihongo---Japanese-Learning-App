package com.example.nihongo.User.ui.screens.homepage


import Campaign
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.nihongo.Admin.viewmodel.AdminNotifyPageViewModel
import com.example.nihongo.User.data.models.Course
import com.example.nihongo.User.data.models.User
import com.example.nihongo.User.data.models.UserProgress
import com.example.nihongo.User.data.repository.CourseRepository
import com.example.nihongo.User.data.repository.UserRepository
import com.example.nihongo.User.ui.components.BottomNavigationBar
import com.example.nihongo.User.utils.CloudinaryConfig
import com.example.nihongo.User.utils.NavigationRoutes
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.Locale
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    navController: NavController,
    userEmail: String,
    userRepository: UserRepository,
    courseRepository: CourseRepository,
    onSignOut: () -> Unit,
) {
    var currentUser by remember { mutableStateOf<User?>(null) }
    var userProgressList by remember { mutableStateOf<List<UserProgress>>(emptyList()) }
    var courseList by remember { mutableStateOf<List<Course>>(emptyList()) }
    val selectedItem = "profile"
    
    // States for edit mode
    var isEditMode by remember { mutableStateOf(false) }
    var editedUsername by remember { mutableStateOf("") }
    var editedImageUrl by remember { mutableStateOf("") }
    var editedJlptLevel by remember { mutableStateOf<Int?>(null) }
    var editedStudyMonths by remember { mutableStateOf<Int?>(null) }
    var editedPassword by remember { mutableStateOf("") }
    var showPasswordField by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    
    // State for image picker
    var showImagePickerOptions by remember { mutableStateOf(false) }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    
    // Thêm biến showSignOutDialog
    var showSignOutDialog by remember { mutableStateOf(false) }


    var showVipRegistrationDialog by remember { mutableStateOf(false) }
    var showPaymentMethodDialog by remember { mutableStateOf(false) }
    var showPaymentConfirmationDialog by remember { mutableStateOf(false) }
    var selectedPaymentMethod by remember { mutableStateOf("") }
    var isProcessingPayment by remember { mutableStateOf(false) }
    var paymentReference by remember { mutableStateOf("") }
    
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    // Image picker launcher
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            selectedImageUri = it
            // Upload image to Cloudinary when selected
            isLoading = true
            scope.launch {
                try {
                    val imageUrl = CloudinaryConfig.uploadImage(it, context)
                    editedImageUrl = imageUrl
                    
                    // Cập nhật URL ảnh ngay lập tức trong Firestore
                    currentUser?.let { user ->
                        val success = userRepository.updateUserImageUrl(user.id, imageUrl)
                        if (success) {
                            // Refresh user data
                            currentUser = userRepository.getUserByEmail(userEmail)
                            Toast.makeText(context, "Profile picture updated", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "Failed to update profile picture", Toast.LENGTH_SHORT).show()
                        }
                    }
                    
                    isLoading = false
                } catch (e: Exception) {
                    Log.e("ProfileScreen", "Error uploading image", e)
                    Toast.makeText(context, "Failed to upload image: ${e.message}", Toast.LENGTH_SHORT).show()
                    isLoading = false
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        val user = userRepository.getUserByEmail(userEmail)
        currentUser = user
        user?.let {
            userProgressList = userRepository.getAllUserProgress(it.id)
            // Initialize edit fields with current values
            editedUsername = it.username
            editedImageUrl = it.imageUrl
            editedJlptLevel = it.jlptLevel
            editedStudyMonths = it.studyMonths
        }
        courseList = courseRepository.getAllCourses()
    }

    // Image picker options dialog
    if (showImagePickerOptions) {
        AlertDialog(
            onDismissRequest = { showImagePickerOptions = false },
            title = { Text("Change Profile Picture") },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "Choose a new profile picture from your gallery",
                        color = Color.Black,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    
                    Button(
                        onClick = {
                            imagePickerLauncher.launch("image/*")
                            showImagePickerOptions = false
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Image, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Choose from Gallery",
                                color = Color.Black
                            )
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showImagePickerOptions = false }) {
                    Text(
                        "Cancel",
                        color = Color.Black
                    )
                }
            }
        )
    }
    
    // Sign Out Confirmation Dialog
    if (showSignOutDialog) {
        AlertDialog(
            onDismissRequest = { showSignOutDialog = false },
            title = { Text("Sign Out") },
            text = { Text(
                "Are you sure you want to sign out?",
                color = Color.Black
            ) },
            confirmButton = {
                Button(
                    onClick = {
                        showSignOutDialog = false
                        // Cập nhật trạng thái offline trước khi đăng xuất
                        currentUser?.let { user ->
                            CoroutineScope(Dispatchers.IO).launch {
                                userRepository.updateUserOnlineStatus(user.id, false)
                                // Đảm bảo cập nhật xong trước khi đăng xuất
                                withContext(Dispatchers.Main) {
                                    userRepository.logout() // This will clear both in-memory and SharedPreferences
                                    navController.navigate(NavigationRoutes.LOGIN) {
                                        popUpTo(0)
                                    }
                                }
                            }
                        } ?: run {
                            userRepository.logout()
                            navController.navigate(NavigationRoutes.LOGIN) {
                                popUpTo(0)
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) {
                    Text(
                        "Sign Out",
                        color = Color.Black
                    )
                }
            },
            dismissButton = {
                Button(
                    onClick = { showSignOutDialog = false }
                ) {
                    Text(
                        "Cancel",
                        color = Color.Black
                    )
                }
            }
        )
    }

    Scaffold(
        containerColor = Color(0xFFEEEEEE),
        topBar = {
            TopAppBar(
                title = {
                    currentUser?.let {
                        Column {
                            Text(
                                "👋 こんにちわ ${it.username} さん",
                                style = MaterialTheme.typography.bodyLarge,
                                color = Color.Black
                            )
                            if (it.vip) {
                                Text(
                                    "⭐ VIP です!",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Color(0xFFFFC107)
                                )
                            }
                        }
                    }
                },
                actions = {
                    if (isEditMode) {
                        IconButton(onClick = {
                            isEditMode = false
                            showPasswordField = false
                            editedPassword = ""
                            currentUser?.let { user ->
                                editedUsername = user.username
                                editedImageUrl = user.imageUrl
                                editedJlptLevel = user.jlptLevel
                                editedStudyMonths = user.studyMonths
                            }
                        }) {
                            Icon(Icons.Default.Close, contentDescription = "Cancel")
                        }
                        IconButton(onClick = {
                            // Validate input
                            if (editedUsername.isBlank()) {
                                Toast.makeText(context, "Username cannot be empty", Toast.LENGTH_SHORT).show()
                                return@IconButton
                            }
                            
                            if (editedStudyMonths != null && editedStudyMonths!! < 0) {
                                Toast.makeText(context, "Months of study cannot be negative", Toast.LENGTH_SHORT).show()
                                return@IconButton
                            }
                            
                            currentUser?.let { user ->
                                // Chỉ cập nhật các trường liên quan đến hồ sơ
                                val updatedUser = user.copy(
                                    username = editedUsername,
                                    imageUrl = editedImageUrl,
                                    jlptLevel = editedJlptLevel,
                                    studyMonths = editedStudyMonths
                                    // Các trường khác giữ nguyên
                                )
                                
                                CoroutineScope(Dispatchers.IO).launch {
                                    // Cập nhật mật khẩu nếu có thay đổi
                                    if (showPasswordField && editedPassword.isNotEmpty()) {
                                        if (editedPassword.length < 6) {
                                            withContext(Dispatchers.Main) {
                                                Toast.makeText(
                                                    context,
                                                    "Password must be at least 6 characters",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            }
                                            return@launch
                                        }
                                        
                                        val passwordUpdateSuccess = userRepository.updateUserPassword(user.id, editedPassword)
                                        if (!passwordUpdateSuccess) {
                                            withContext(Dispatchers.Main) {
                                                Toast.makeText(
                                                    context,
                                                    "Failed to update password",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            }
                                            return@launch
                                        }
                                    }
                                    
                                    val success = userRepository.updateUserProfile(updatedUser)
                                    
                                    withContext(Dispatchers.Main) {
                                        if (success) {
                                            // Refresh user data
                                            currentUser = userRepository.getUserByEmail(userEmail)
                                            isEditMode = false
                                            showPasswordField = false
                                            editedPassword = ""
                                            
                                            // Show success message
                                            Toast.makeText(
                                                context,
                                                "Profile updated successfully",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        } else {
                                            // Show error message
                                            Toast.makeText(
                                                context,
                                                "Failed to update profile",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    }
                                }
                            }
                        }) {
                            Icon(Icons.Default.Check, contentDescription = "Save")
                        }
                    } else {
                        IconButton(onClick = {
                            navController.navigate("community/$userEmail") {
                                popUpTo(navController.graph.startDestinationId) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }) {
                            Icon(Icons.Default.Group, contentDescription = "Community")
                        }
                        IconButton(onClick = { isEditMode = true }) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit Profile")
                        }
                    }
                }
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
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize()) {
            currentUser?.let { user ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .verticalScroll(rememberScrollState())
                        .background(Color.Transparent)
                        .padding(16.dp)
                ) {
                    // Profile Image Section
                    Box(
                        modifier = Modifier
                            .size(120.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFE0E0E0))
                            .align(Alignment.CenterHorizontally)
                            .clickable(enabled = isEditMode) {
                                if (isEditMode) {
                                    imagePickerLauncher.launch("image/*")
                                }
                            }
                    ) {
                        if (editedImageUrl.isNotEmpty()) {
                            AsyncImage(
                                model = editedImageUrl,
                                contentDescription = "User Image",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Filled.Person,
                                contentDescription = "User Icon",
                                tint = Color.Gray,
                                modifier = Modifier
                                    .size(100.dp)
                                    .padding(20.dp)
                                    .align(Alignment.Center)
                            )
                        }
                        
                        if (isEditMode) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .align(Alignment.BottomEnd)
                                    .clip(CircleShape)
                                    .background(Color(0xFF4CAF50))
                                    .padding(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Edit Image",
                                    tint = Color.White,
                                    modifier = Modifier.align(Alignment.Center)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // User Info Section
                    if (isEditMode) {
                        // Edit Mode UI
                        OutlinedTextField(
                            value = editedUsername,
                            onValueChange = { editedUsername = it },
                            label = { Text("Username") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        // Email field (read-only)
                        OutlinedTextField(
                            value = user.email,
                            onValueChange = { },
                            label = { Text("Email") },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = false,
                            singleLine = true
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        // JLPT Level dropdown
                        var isJlptDropdownExpanded by remember { mutableStateOf(false) }
                        ExposedDropdownMenuBox(
                            expanded = isJlptDropdownExpanded,
                            onExpandedChange = { isJlptDropdownExpanded = it },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedTextField(
                                value = editedJlptLevel?.let { "N$it" } ?: "Select JLPT Level",
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("JLPT Level") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isJlptDropdownExpanded) },
                                modifier = Modifier
                                    .menuAnchor()
                                    .fillMaxWidth(),
                                colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
                            )
                            
                            ExposedDropdownMenu(
                                expanded = isJlptDropdownExpanded,
                                onDismissRequest = { isJlptDropdownExpanded = false },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                // Option to clear selection
                                DropdownMenuItem(
                                    text = { Text("None") },
                                    onClick = {
                                        editedJlptLevel = null
                                        isJlptDropdownExpanded = false
                                    }
                                )
                                
                                // JLPT levels from N5 to N1
                                (5 downTo 1).forEach { level ->
                                    DropdownMenuItem(
                                        text = { Text("N$level") },
                                        onClick = {
                                            editedJlptLevel = level
                                            isJlptDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        // Study months field
                        OutlinedTextField(
                            value = editedStudyMonths?.toString() ?: "",
                            onValueChange = { 
                                val intValue = it.toIntOrNull()
                                if (it.isEmpty() || intValue != null) {
                                    editedStudyMonths = intValue
                                }
                            },
                            label = { Text("Months of Study") },
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            supportingText = { Text("Enter the number of months you've been studying Japanese") }
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        // Password change option
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Change Password")
                            Spacer(modifier = Modifier.width(8.dp))
                            Switch(
                                checked = showPasswordField,
                                onCheckedChange = { 
                                    showPasswordField = it
                                    if (!it) editedPassword = ""
                                }
                            )
                        }
                        
                        if (showPasswordField) {
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            OutlinedTextField(
                                value = editedPassword,
                                onValueChange = { editedPassword = it },
                                label = { Text("New Password") },
                                modifier = Modifier.fillMaxWidth(),
                                visualTransformation = PasswordVisualTransformation(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                singleLine = true,
                                supportingText = { Text("Password must be at least 6 characters") }
                            )
                        }
                    } else {
                        // View Mode UI
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(4.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp)
                            ) {
                                ProfileInfoItem(
                                    icon = Icons.Default.Person,
                                    label = "Username",
                                    value = user.username
                                )
                                
                                Spacer(modifier = Modifier.height(8.dp))
                                
                                ProfileInfoItem(
                                    icon = Icons.Default.Star,
                                    label = "JLPT Level",
                                    value = user.jlptLevel?.let { "N$it" } ?: "Not set"
                                )
                                
                                Spacer(modifier = Modifier.height(8.dp))
                                
                                ProfileInfoItem(
                                    icon = Icons.Default.CheckCircle,
                                    label = "Months of Study",
                                    value = user.studyMonths?.toString() ?: "Not set"
                                )
                                
                                Spacer(modifier = Modifier.height(8.dp))
                                
                                ProfileInfoItem(
                                    icon = Icons.Default.EmojiEvents,
                                    label = "Activity Points",
                                    value = user.activityPoints.toString()
                                )
                                
                                Spacer(modifier = Modifier.height(8.dp))
                                
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = "VIP Status",
                                        style = MaterialTheme.typography.bodyLarge,
                                        modifier = Modifier.weight(1f)
                                    )
                                    
                                    if (user.vip) {
                                        Text(
                                            text = "⭐ VIP",
                                            color = Color(0xFFFFC107),
                                            fontWeight = FontWeight.Bold
                                        )
                                    } else {
                                        Text(
                                            text = "Standard",
                                            color = Color.Gray
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Progress Section
                    Text(
                        text = "Your Progress",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    if (userProgressList.isEmpty()) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(4.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .padding(16.dp)
                                    .fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "You haven't joined any courses yet",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = Color.Gray
                                )
                                
                                Spacer(modifier = Modifier.height(8.dp))
                                
                                Button(
                                    onClick = {
                                        navController.navigate("courses/$userEmail") {
                                            popUpTo(navController.graph.startDestinationId) { saveState = true }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                                ) {
                                    Text(
                                        "Browse Courses",
                                        color = Color.Black
                                    )
                                }
                            }
                        }
                    } else {
                        userProgressList.forEach { progress ->
                            val course = courseList.find { it.id == progress.courseId }
                            
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                elevation = CardDefaults.cardElevation(2.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = course?.title ?: progress.courseTitle ?: "Unknown Course",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.weight(1f)
                                        )
                                        
                                        Text(
                                            text = "${(progress.progress * 100).toInt()}%",
                                            style = MaterialTheme.typography.bodyLarge,
                                            color = Color(0xFF4CAF50)
                                        )
                                    }
                                    
                                    Spacer(modifier = Modifier.height(8.dp))
                                    
                                    LinearProgressIndicator(
                                        progress = { progress.progress },
                                        modifier = Modifier.fillMaxWidth(),
                                        color = Color(0xFF4CAF50),
                                        trackColor = Color(0xFFE0E0E0)
                                    )
                                    
                                    Spacer(modifier = Modifier.height(8.dp))
                                    
                                    Row(
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = "${progress.passedExercises.size} exercises completed",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = Color.Gray
                                        )
                                        
                                        Text(
                                            text = "Last updated: ${formatDate(progress.lastUpdated)}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color.Gray
                                        )
                                    }
                                    
                                    Spacer(modifier = Modifier.height(8.dp))
                                    
                                    Button(
                                        onClick = {
                                            navController.navigate("lessons/${progress.courseId}/$userEmail")
                                        },
                                        modifier = Modifier.align(Alignment.End),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                                    ) {
                                        Text(
                                            "Continue",
                                            color = Color.Black
                                        )
                                    }
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Sign Out Button
                    Button(
                        onClick = { showSignOutDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            "Sign Out",
                            color = Color.Black
                        )
                    }
                    
                    // Add VIP Registration Button for standard users
                    if (currentUser != null && !currentUser!!.vip) {
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Button(
                            onClick = { showVipRegistrationDialog = true },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.Transparent
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    brush = Brush.horizontalGradient(
                                        listOf(
                                            Color(0xFFFFC107),
                                            Color(0xFFFF9800)
                                        )
                                    ),
                                    shape = RoundedCornerShape(30.dp)
                                )
                                .clip(RoundedCornerShape(30.dp)),
                            shape = RoundedCornerShape(30.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Star,
                                    contentDescription = null,
                                    tint = Color.White
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    "Nâng cấp lên VIP",
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                }
            } ?: run {
                // Loading or error state
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            
            // Loading overlay
            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.5f)),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Color.White)
                }
            }
        }
    }

    // Dialog đăng ký VIP
    if (showVipRegistrationDialog) {
        AlertDialog(
            onDismissRequest = { showVipRegistrationDialog = false },
            title = { 
                Text(
                    "Nâng cấp lên VIP", 
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleLarge
                ) 
            },
            text = { 
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        // Giới hạn chiều cao tối đa để đảm bảo dialog không vượt quá màn hình
                        .heightIn(max = 450.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                            .padding(vertical = 8.dp)
                    ) {
                        // Banner VIP
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(150.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    brush = Brush.linearGradient(
                                        colors = listOf(Color(0xFF1A237E), Color(0xFF3949AB)),
                                        start = Offset(0f, 0f),
                                        end = Offset(Float.POSITIVE_INFINITY, 0f)
                                    )
                                )
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Star,
                                    contentDescription = null,
                                    tint = Color(0xFFFFC107),
                                    modifier = Modifier.size(40.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    "Gói VIP Premium",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleLarge
                                )
                                Text(
                                    "100.000 VNĐ / tháng",
                                    color = Color(0xFFFFC107),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(20.dp))
                        
                        // Quyền lợi VIP
                        Text(
                            "Quyền lợi thành viên VIP:",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium
                        )
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        VipBenefitItem(
                            icon = Icons.Default.CheckCircle,
                            text = "Truy cập tất cả khóa học VIP"
                        )
                        
                        VipBenefitItem(
                            icon = Icons.Default.CheckCircle,
                            text = "Không giới hạn bài tập và flashcards"
                        )
                        
                        VipBenefitItem(
                            icon = Icons.Default.CheckCircle,
                            text = "Ưu tiên hỗ trợ từ đội ngũ giáo viên"
                        )
                        
                        VipBenefitItem(
                            icon = Icons.Default.CheckCircle,
                            text = "Huy hiệu VIP độc quyền trên hồ sơ"
                        )
                        
                        Spacer(modifier = Modifier.height(20.dp))
                        
                        // Phương thức thanh toán
                        Text(
                            "Chọn phương thức thanh toán:",
                            fontWeight = FontWeight.Medium,
                            style = MaterialTheme.typography.titleMedium
                        )
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(2.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(8.dp)
                            ) {
                                PaymentMethodItem(
                                    name = "VietcomBank",
                                    isSelected = selectedPaymentMethod == "VietcomBank",
                                    onSelect = { selectedPaymentMethod = "VietcomBank" }
                                )
                                
                                Divider(modifier = Modifier.padding(vertical = 4.dp))
                                
                                PaymentMethodItem(
                                    name = "BIDV",
                                    isSelected = selectedPaymentMethod == "BIDV",
                                    onSelect = { selectedPaymentMethod = "BIDV" }
                                )
                                
                                Divider(modifier = Modifier.padding(vertical = 4.dp))
                                
                                PaymentMethodItem(
                                    name = "Techcombank",
                                    isSelected = selectedPaymentMethod == "Techcombank",
                                    onSelect = { selectedPaymentMethod = "Techcombank" }
                                )
                                
                                Divider(modifier = Modifier.padding(vertical = 4.dp))
                                
                                PaymentMethodItem(
                                    name = "MB Bank",
                                    isSelected = selectedPaymentMethod == "MB Bank",
                                    onSelect = { selectedPaymentMethod = "MB Bank" }
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { 
                        if (selectedPaymentMethod.isNotEmpty()) {
                            // Tạo mã tham chiếu thanh toán
                            paymentReference = "VIP-${currentUser?.id}-${System.currentTimeMillis()}"
                            showVipRegistrationDialog = false
                            showPaymentMethodDialog = true
                        } else {
                            Toast.makeText(context, "Vui lòng chọn phương thức thanh toán", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF4CAF50)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Tiếp tục", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showVipRegistrationDialog = false }
                ) {
                    Text("Hủy")
                }
            },
            modifier = Modifier
                .padding(16.dp)
                .wrapContentHeight(),
            shape = RoundedCornerShape(16.dp)
        )
    }

    // Dialog chi tiết phương thức thanh toán
    if (showPaymentMethodDialog) {
        AlertDialog(
            onDismissRequest = { showPaymentMethodDialog = false },
            title = { 
                Text(
                    "Chi tiết thanh toán", 
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleLarge
                ) 
            },
            text = { 
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 450.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                            .padding(vertical = 8.dp)
                    ) {
                        // Thêm logo ngân hàng ở đầu
                        val bankLogoUrl = when(selectedPaymentMethod) {
                            "VietcomBank" -> "https://drive.google.com/uc?export=view&id=1I3P2A1hbVk414wNORPO-1AIRk0AFTG_V"
                            "BIDV" -> "https://drive.google.com/uc?export=view&id=11HTQfs00nfmme7ZsHH6plFUVK9eFa4pg"
                            "Techcombank" -> "https://drive.google.com/uc?export=view&id=1-dg2NuVCws0GTsu25I6w8k2Bsi1jhxFf"
                            "MB Bank" -> "https://drive.google.com/uc?export=view&id=1P9ljCLTucH7_LqrOAqioXcoVTbhZS7r1"
                            else -> ""
                        }
                        
                        // Logo ngân hàng
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(80.dp)
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            AsyncImage(
                                model = bankLogoUrl,
                                contentDescription = "Logo ngân hàng",
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .padding(8.dp),
                                contentScale = ContentScale.Fit
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // Thông tin chuyển khoản
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = Color(0xFFF5F5F5)
                            ),
                            elevation = CardDefaults.cardElevation(2.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp)
                            ) {
                                Text(
                                    text = "THÔNG TIN CHUYỂN KHOẢN",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF4CAF50),
                                    modifier = Modifier.padding(bottom = 12.dp)
                                )
                                
                                PaymentInfoItem("Ngân hàng", selectedPaymentMethod)
                                
                                Divider(
                                    modifier = Modifier.padding(vertical = 8.dp),
                                    color = Color.LightGray
                                )
                                
                                PaymentInfoItem("Số tài khoản", "970430102031")
                                
                                Divider(
                                    modifier = Modifier.padding(vertical = 8.dp),
                                    color = Color.LightGray
                                )
                                
                                PaymentInfoItem("Tên tài khoản", "TRAN THANH PHONG")
                                
                                Divider(
                                    modifier = Modifier.padding(vertical = 8.dp),
                                    color = Color.LightGray
                                )
                                
                                PaymentInfoItem("Số tiền", "100.000 VNĐ")
                                
                                Divider(
                                    modifier = Modifier.padding(vertical = 8.dp),
                                    color = Color.LightGray
                                )
                                
                                PaymentInfoItem("Nội dung chuyển khoản", paymentReference, isHighlighted = true)
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Text(
                            "Lưu ý: Vui lòng sao chép chính xác nội dung chuyển khoản để hệ thống có thể xác nhận thanh toán của bạn.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Text(
                            "Sau khi chuyển khoản, nhấn 'Xác nhận thanh toán' bên dưới.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.Gray,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { 
                        showPaymentMethodDialog = false
                        showPaymentConfirmationDialog = true
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Xác nhận thanh toán", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showPaymentMethodDialog = false }
                ) {
                    Text("Quay lại")
                }
            },
            modifier = Modifier
                .padding(16.dp)
                .wrapContentHeight(),
            shape = RoundedCornerShape(16.dp)
        )
    }

    // Dialog xác nhận thanh toán
    if (showPaymentConfirmationDialog) {
        AlertDialog(
            onDismissRequest = { showPaymentConfirmationDialog = false },
            title = { 
                Text(
                    "Xác nhận thanh toán", 
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleLarge
                ) 
            },
            text = { 
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (isProcessingPayment) {
                        CircularProgressIndicator(color = Color(0xFF4CAF50))
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Text(
                            "Đang xử lý thanh toán của bạn...",
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = Color(0xFF4CAF50),
                            modifier = Modifier.size(64.dp)
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Text(
                            "Bằng cách nhấn 'Xác nhận', bạn xác nhận rằng đã hoàn tất việc chuyển khoản.",
                            textAlign = TextAlign.Center
                        )
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        Text(
                            "Đội ngũ của chúng tôi sẽ xác minh thanh toán và kích hoạt trạng thái VIP của bạn trong vòng 24 giờ.",
                            color = Color.Gray,
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { 
                        isProcessingPayment = true
                        // Xử lý thanh toán và gửi thông báo cho admin
                        scope.launch {
                            delay(1500) // Giả lập thời gian xử lý
                            
                            // Gửi yêu cầu VIP lên Firestore
                            currentUser?.let { user ->
                                val vipRequest = hashMapOf(
                                    "userId" to user.id,
                                    "username" to user.username,
                                    "userEmail" to user.email,
                                    "paymentMethod" to selectedPaymentMethod,
                                    "amount" to "100000",
                                    "reference" to paymentReference,
                                    "requestDate" to FieldValue.serverTimestamp(),
                                    "status" to "pending",
                                    "notes" to ""
                                )
                                
                                try {
                                    // Lưu yêu cầu VIP vào Firestore
                                    val docRef = FirebaseFirestore.getInstance().collection("vipRequests")
                                        .add(vipRequest)
                                        .await()
                                    
                                    // Lấy danh sách tài khoản admin
                                    val adminSnapshot = FirebaseFirestore.getInstance().collection("users")
                                        .whereEqualTo("admin", true)
                                        .get()
                                        .await()
                                    
                                    // Gửi thông báo cho tất cả admin
                                    adminSnapshot.documents.forEach { adminDoc ->
                                        val adminEmail = adminDoc.getString("email")
                                        if (adminEmail != null) {
                                            // Tạo thông báo cho admin
                                            val notificationTitle = "Yêu cầu nâng cấp VIP mới"
                                            val notificationMessage = "Người dùng ${user.username} đã gửi yêu cầu nâng cấp VIP.\n" +
                                                    "Phương thức: $selectedPaymentMethod\n" +
                                                    "Số tiền: 100.000 VNĐ\n" +
                                                    "Mã tham chiếu: $paymentReference\n" +
                                                    "Vui lòng kiểm tra và xác nhận thanh toán."
                                            
                                            // Tạo đối tượng Campaign để gửi thông báo
                                            val campaign = Campaign(
                                                id = UUID.randomUUID().toString(),
                                                title = notificationTitle,
                                                message = notificationMessage,
                                                imageUrl = null,
                                                createdAt = Timestamp.now(),
                                                isScheduled = false,
                                                scheduledFor = null,
                                                isDaily = false,
                                                dailyHour = 0,
                                                dailyMinute = 0
                                            )
                                            
                                            // Gửi thông báo đến admin
                                            try {
                                                val notifyViewModel = AdminNotifyPageViewModel()
                                                
                                                // Sử dụng phương thức mới để gửi thông báo chỉ đến admin
                                                notifyViewModel.sendAdminOnlyNotification(campaign)
                                                
                                                // Lưu thông báo vào collection notifications
                                                val notification = hashMapOf(
                                                    "userId" to adminDoc.id,
                                                    "title" to notificationTitle,
                                                    "message" to notificationMessage,
                                                    "timestamp" to FieldValue.serverTimestamp(),
                                                    "read" to false,
                                                    "type" to "vip_request",
                                                    "referenceId" to docRef.id
                                                )
                                                
                                                FirebaseFirestore.getInstance().collection("notifications")
                                                    .add(notification)
                                                    .await()
                                                
                                                Log.d("ProfileScreen", "Notification sent to admin: $adminEmail")
                                                
                                            } catch (e: Exception) {
                                                Log.e("ProfileScreen", "Failed to send notification to admin: $adminEmail", e)
                                            }
                                        }
                                    }
                                    
                                    withContext(Dispatchers.Main) {
                                        isProcessingPayment = false
                                        showPaymentConfirmationDialog = false
                                        
                                        // Hiển thị thông báo thành công
                                        Toast.makeText(
                                            context,
                                            "Yêu cầu VIP đã được gửi thành công!",
                                            Toast.LENGTH_LONG
                                        ).show()
                                    }
                                } catch (e: Exception) {
                                    withContext(Dispatchers.Main) {
                                        isProcessingPayment = false
                                        
                                        // Hiển thị thông báo lỗi
                                        Toast.makeText(
                                            context,
                                            "Lỗi: ${e.message}",
                                            Toast.LENGTH_LONG
                                        ).show()
                                    }
                                }
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF4CAF50),
                        disabledContainerColor = Color(0xFF4CAF50).copy(alpha = 0.5f)
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isProcessingPayment
                ) {
                    Text(
                        if (isProcessingPayment) "Đang xử lý..." else "Xác nhận",
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showPaymentConfirmationDialog = false },
                    enabled = !isProcessingPayment
                ) {
                    Text("Hủy")
                }
            },
            modifier = Modifier
                .padding(16.dp)
                .wrapContentHeight(),
            shape = RoundedCornerShape(16.dp)
        )
    }
}

@Composable
fun ProfileInfoItem(
    icon: ImageVector,
    label: String,
    value: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color(0xFF4CAF50),
            modifier = Modifier.size(24.dp)
        )
        
        Spacer(modifier = Modifier.width(8.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray
            )
            
            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}

// Helper function to format date
private fun formatDate(timestamp: Long): String {
    val date = java.util.Date(timestamp)
    val format = java.text.SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
    return format.format(date)
}

@Composable
private fun VipBenefitItem(
    icon: ImageVector,
    text: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color(0xFF4CAF50),
            modifier = Modifier.size(20.dp)
        )
        
        Spacer(modifier = Modifier.width(12.dp))
        
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
fun PaymentInfoItem(label: String, value: String, isHighlighted: Boolean = false) {
    val context = LocalContext.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = Color.Gray,
            fontWeight = FontWeight.Medium
        )
        
        Spacer(modifier = Modifier.height(4.dp))
        
        if (isHighlighted) {
            // Hiển thị nội dung được highlight trong card riêng
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFFE8F5E9)  // Màu nền xanh nhạt
                ),
                border = BorderStroke(1.dp, Color(0xFF4CAF50)),
                shape = RoundedCornerShape(8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = value,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF4CAF50),
                        modifier = Modifier.weight(1f)
                    )
                    
                    IconButton(
                        onClick = {
                            // Copy to clipboard functionality
                            val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clipData = ClipData.newPlainText("Payment Reference", value)
                            clipboardManager.setPrimaryClip(clipData)
                            Toast.makeText(context, "Đã sao chép vào bộ nhớ tạm", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Copy to clipboard",
                            tint = Color(0xFF4CAF50)
                        )
                    }
                }
            }
        } else {
            // Hiển thị thông thường cho các thông tin khác
            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = Color.Black
            )
        }
    }
}

@Composable
fun PaymentMethodItem(name: String, isSelected: Boolean, onSelect: () -> Unit) {
    val bankLogoUrl = when(name) {
        "VietcomBank" -> "https://drive.google.com/uc?export=view&id=1I3P2A1hbVk414wNORPO-1AIRk0AFTG_V"
        "BIDV" -> "https://drive.google.com/uc?export=view&id=11HTQfs00nfmme7ZsHH6plFUVK9eFa4pg"
        "Techcombank" -> "https://drive.google.com/uc?export=view&id=1-dg2NuVCws0GTsu25I6w8k2Bsi1jhxFf"
        "MB Bank" -> "https://drive.google.com/uc?export=view&id=1P9ljCLTucH7_LqrOAqioXcoVTbhZS7r1"
        else -> ""
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect() }
            .padding(vertical = 8.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = isSelected,
            onClick = { onSelect() },
            colors = RadioButtonDefaults.colors(
                selectedColor = Color(0xFF4CAF50)
            )
        )
        
        Spacer(modifier = Modifier.width(2.dp))
        
        // Logo ngân hàng
        AsyncImage(
            model = bankLogoUrl,
            contentDescription = "Logo $name",
            modifier = Modifier
                .size(width = 45.dp, height = 32.dp)
                .padding(end = 4.dp),
            contentScale = ContentScale.Fit
        )
        
        Spacer(modifier = Modifier.width(4.dp))
        
        Text(
            text = name,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}
