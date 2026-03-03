package com.example.nihongo.Admin.ui

import Campaign
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import com.example.nihongo.Admin.utils.AdminEmailSender
import com.example.nihongo.Admin.viewmodel.AdminUserViewModel
import com.example.nihongo.User.data.models.User
import com.example.nihongo.utils.EmailSender
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import com.example.nihongo.Admin.utils.ImgurUploader
import com.example.nihongo.Admin.viewmodel.AdminNotifyPageViewModel
import com.google.firebase.Timestamp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.security.MessageDigest
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.example.nihongo.Admin.utils.CatboxUploader

// Custom theme colors for Japanese language learning app
object NihongoTheme {
    val backgroundGray = Color(0xFFEEEEEE)   // Nền xám nhạt
    val primaryGreen = Color(0xFF4CAF50)     // Green for success/progress
    val accentRed = Color(0xFFE53935)        // Red for Japanese flag
    val backgroundWhite = Color(0xFFF5F5F5)  // White background
    val textDark = Color(0xFF212121)         // Dark text
    val secondaryLightGreen = Color(0xFFA5D6A7) // Light green for secondary elements
    val adminPurple = Color(0xFF6538CC)     // Green for success/progress
}

// Function to hash passwords with SHA-256
fun hashPassword(password: String): String {
    val bytes = MessageDigest.getInstance("SHA-256").digest(password.toByteArray())
    return bytes.joinToString("") { "%02x".format(it) }
}

@Composable
fun userPage(viewModel: AdminUserViewModel = androidx.lifecycle.viewmodel.compose.viewModel()) {
    val users by viewModel.users.collectAsState()
    var searchText by remember { mutableStateOf("") }
    var filterVipOnly by remember { mutableStateOf(false) }

    var showAddDialog by remember { mutableStateOf(false) }
    var selectedUser by remember { mutableStateOf<User?>(null) }

    var filterLoggedInOnly by remember { mutableStateOf(false) }

    val filteredUsers = users.filter {
        val matchesSearch = it.username.contains(searchText, ignoreCase = true) ||
                it.email.contains(searchText, ignoreCase = true)
        val matchesVip = !filterVipOnly || it.vip
        val matchesLoggedIn = !filterLoggedInOnly || it.online
        matchesSearch && matchesVip && matchesLoggedIn
    }

    // Outer Box to stack content + FAB
    Box(modifier = Modifier.fillMaxSize().background(NihongoTheme.backgroundWhite)) {
        // Wrap Column in LazyColumn for scrollability
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 64.dp, start = 16.dp, end = 16.dp, bottom = 16.dp)
        ) {
            item {
                OutlinedTextField(
                    value = searchText,
                    onValueChange = { searchText = it },
                    label = { Text("Search by name or email", color = NihongoTheme.textDark) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NihongoTheme.primaryGreen,
                        unfocusedBorderColor = NihongoTheme.primaryGreen.copy(alpha = 0.5f),
                        cursorColor = NihongoTheme.primaryGreen
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Filter options with styled checkboxes
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(NihongoTheme.secondaryLightGreen.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                        .padding(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Checkbox(
                            checked = filterVipOnly,
                            onCheckedChange = { filterVipOnly = it },
                            colors = CheckboxDefaults.colors(
                                checkedColor = NihongoTheme.primaryGreen,
                                uncheckedColor = NihongoTheme.textDark.copy(alpha = 0.6f)
                            )
                        )
                        Text("VIP Only", color = NihongoTheme.textDark)
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Checkbox(
                            checked = filterLoggedInOnly,
                            onCheckedChange = { filterLoggedInOnly = it },
                            colors = CheckboxDefaults.colors(
                                checkedColor = NihongoTheme.primaryGreen,
                                uncheckedColor = NihongoTheme.textDark.copy(alpha = 0.6f)
                            )
                        )
                        Text("Online Only", color = NihongoTheme.textDark)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }

            // User List
            items(filteredUsers) { user ->
                UserCard(user = user, onClick = { selectedUser = user })
                Spacer(modifier = Modifier.height(12.dp))
            }
            item {

                Spacer(modifier = Modifier.height(58.dp))

            }
        }

        // Floating Action Button overlaid
        FloatingActionButton(
            onClick = { selectedUser = null; showAddDialog = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 92.dp, end = 24.dp),
            containerColor = NihongoTheme.primaryGreen,
            contentColor = Color.White
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add User")
        }

        val context = LocalContext.current
        // Dialogs
        if (showAddDialog) {
            AddOrEditUserDialog(
                user = selectedUser,
                onDismiss = { showAddDialog = false },
                onSave = { user ->
                    // Hash password if not empty
                    val finalUser = if (user.password.isNotEmpty()) {
                        user.copy(password = hashPassword(user.password))
                    } else user

                    if (selectedUser == null) viewModel.addUser(finalUser)
                    else viewModel.updateUser(finalUser)
                    showAddDialog = false
                },
                onDelete = {
                    viewModel.deleteUser(it)
                    showAddDialog = false
                }
            )
        }

        selectedUser?.let { user ->
            AddOrEditUserDialog(
                user = user,
                onDismiss = { selectedUser = null },
                onSave = { updatedUser ->
                    // Hash password if it's been changed
                    val finalUser = if (updatedUser.password.isNotEmpty() && updatedUser.password != user.password) {
                        updatedUser.copy(password = hashPassword(updatedUser.password))
                    } else updatedUser

                    viewModel.updateUser(finalUser)
                    selectedUser = null
                },
                onDelete = {
                    viewModel.deleteUser(it)
                    selectedUser = null
                }
            )
        }
    }
}

@Composable
fun UserCard(user: User, onClick: () -> Unit) {
    val defaultImage =
        "https://ih1.redbubble.net/image.1629121092.8901/bg,f8f8f8-flat,750x,075,f-pad,750x1000,f8f8f8.jpg"
    val imageUrl = if (user.imageUrl.isNullOrBlank()) defaultImage else user.imageUrl

    val backgroundColor = if (user.vip)
        NihongoTheme.secondaryLightGreen.copy(alpha = 0.3f)
    else
        Color.White

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        elevation = CardDefaults.cardElevation(4.dp),
        shape = RoundedCornerShape(12.dp),
        border = if (user.vip)
            BorderStroke(2.dp, NihongoTheme.primaryGreen)
        else null
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = rememberAsyncImagePainter(
                    ImageRequest.Builder(LocalContext.current)
                        .data(imageUrl)
                        .crossfade(true)
                        .build()
                ),
                contentDescription = "User Image",
                modifier = Modifier
                    .size(60.dp)
                    .clip(CircleShape)
                    .border(2.dp, if (user.vip) NihongoTheme.primaryGreen else Color.Gray, CircleShape),
                contentScale = ContentScale.Crop
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = user.username,
                        style = MaterialTheme.typography.titleMedium,
                        color = if (user.vip) NihongoTheme.primaryGreen else NihongoTheme.textDark
                    )
                    if (user.vip) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = "VIP User",
                            tint = NihongoTheme.primaryGreen,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    if (user.admin) {
                        Spacer(modifier = Modifier.width(3.dp))
                        Icon(
                            Icons.Default.AdminPanelSettings,
                            contentDescription = "Admin User",
                            tint = NihongoTheme.adminPurple,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                Text(
                    text = user.email,
                    style = MaterialTheme.typography.bodyMedium,
                    color = NihongoTheme.textDark.copy(alpha = 0.7f)
                )
            }

            // Online status indicator
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .background(
                        color = if (user.online) NihongoTheme.primaryGreen else NihongoTheme.accentRed,
                        shape = CircleShape
                    )
            )
        }
    }
}

@Composable
fun AddOrEditUserDialog(
    user: User? = null,
    onDismiss: () -> Unit,
    onSave: (User) -> Unit,
    onDelete: (User) -> Unit,
) {
    val usernameState = remember { mutableStateOf(user?.username ?: "") }
    val emailState = remember { mutableStateOf(user?.email ?: "") }
    val imageUrlState = remember { mutableStateOf(user?.imageUrl ?: "") }
    val passwordState = remember { mutableStateOf("") } // Don't show existing password
    val vipState = remember { mutableStateOf(user?.vip ?: false) }
    val adminState = remember { mutableStateOf(user?.admin ?: false) }
    val selectedImageUri = remember { mutableStateOf<Uri?>(null) }
    val showUrlFieldState = remember { mutableStateOf(false) }

    // Track upload state
    var isUploading by remember { mutableStateOf(false) }
    var uploadError by remember { mutableStateOf<String?>(null) }

    // New state for password visibility switch
    val showPasswordField = remember { mutableStateOf(user == null) } // Show by default for new users

    var selectedTabIndex by remember { mutableStateOf(0) }
    var message by remember { mutableStateOf("") }
    var selectedSendType by remember { mutableStateOf("Notification") }
    var selectedContentType by remember { mutableStateOf("Thủ công") }
    var emailToSend by remember { mutableStateOf(user?.email ?: "") }
    var showDialog by remember { mutableStateOf(false) }
    var lastSendSuccess by remember { mutableStateOf(true) }

    // Context for file operations
    val context = LocalContext.current

    // Coroutine scope for async operations
    val coroutineScope = rememberCoroutineScope()

    val activityPointsState = remember { mutableStateOf(user?.activityPoints?.toString() ?: "0") }
    val jlptLevelState = remember { mutableStateOf(user?.jlptLevel) }
    val studyMonthsState = remember { mutableStateOf(user?.studyMonths) }
    val rankState = remember { mutableStateOf(user?.rank ?: "Tân binh") }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            if (user == null || selectedTabIndex == 0) {
                Button(
                    onClick = {
                        if (selectedImageUri.value != null) {
                            // Start upload process
                            isUploading = true
                            uploadError = null

                            coroutineScope.launch {
                                try {
                                    // Convert URI to file
                                    val inputStream = context.contentResolver.openInputStream(selectedImageUri.value!!)
                                    val file = File(context.cacheDir, "temp_upload_${System.currentTimeMillis()}.jpg")

                                    withContext(Dispatchers.IO) {
                                        file.outputStream().use { outputStream ->
                                            inputStream?.copyTo(outputStream)
                                        }
                                        inputStream?.close()
                                    }

                                    // Upload to Imgur
                                    val uploadedUrl = CatboxUploader.uploadVideo(file!!)

                                    withContext(Dispatchers.Main) {
                                        if (uploadedUrl != null) {
                                            // Update imageUrl with the uploaded link
                                            imageUrlState.value = uploadedUrl

                                            // Create user object with the new image URL
                                            saveUser(
                                                user,
                                                usernameState.value,
                                                emailState.value,
                                                uploadedUrl,
                                                passwordState.value,
                                                vipState.value,
                                                adminState.value,
                                                activityPointsState.value.toIntOrNull() ?: 0,
                                                jlptLevelState.value,
                                                studyMonthsState.value,
                                                rankState.value,
                                                onSave
                                            )

                                            // Clean up temp file
                                            file.delete()
                                        } else {
                                            uploadError = "Failed to upload image"
                                        }
                                        isUploading = false
                                    }
                                } catch (e: Exception) {
                                    withContext(Dispatchers.Main) {
                                        uploadError = "Error: ${e.localizedMessage}"
                                        isUploading = false
                                    }
                                }
                            }
                        } else {
                            // No new image to upload, just save with existing URL
                            saveUser(
                                user,
                                usernameState.value,
                                emailState.value,
                                imageUrlState.value,
                                passwordState.value,
                                vipState.value,
                                adminState.value,
                                activityPointsState.value.toIntOrNull() ?: 0,
                                jlptLevelState.value,
                                studyMonthsState.value,
                                rankState.value,
                                onSave
                            )
                        }
                    },
                    enabled = !isUploading,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NihongoTheme.primaryGreen,
                        contentColor = Color.White,
                        disabledContainerColor = NihongoTheme.primaryGreen.copy(alpha = 0.5f)
                    )
                ) {
                    if (isUploading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Uploading...")
                    } else {
                        Text("Save")
                    }
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = NihongoTheme.textDark
                )
            ) {
                Text("Cancel")
            }
        },
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    if (user == null) "Add User" else "Edit User",
                    color = NihongoTheme.primaryGreen
                )
                if (user != null && selectedTabIndex == 0) {
                    IconButton(onClick = { onDelete(user) }) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = NihongoTheme.accentRed
                        )
                    }
                }
            }
        },
        containerColor = NihongoTheme.backgroundWhite,
        text = {
            Column {
                // Only show tabs if editing an existing user
                if (user != null) {
                    TabRow(
                        selectedTabIndex = selectedTabIndex,
                        containerColor = NihongoTheme.backgroundWhite,
                        contentColor = NihongoTheme.primaryGreen,
                        indicator = { tabPositions ->
                            TabRowDefaults.Indicator(
                                modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                                color = NihongoTheme.primaryGreen
                            )
                        }
                    ) {
                        Tab(
                            selected = selectedTabIndex == 0,
                            onClick = { selectedTabIndex = 0 },
                            text = { Text("Edit") },
                            selectedContentColor = NihongoTheme.primaryGreen,
                            unselectedContentColor = NihongoTheme.textDark.copy(alpha = 0.7f)
                        )
                        Tab(
                            selected = selectedTabIndex == 1,
                            onClick = { selectedTabIndex = 1 },
                            text = { Text("Notify") },
                            selectedContentColor = NihongoTheme.primaryGreen,
                            unselectedContentColor = NihongoTheme.textDark.copy(alpha = 0.7f)
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Show content based on tab or directly if adding new user
                if (user == null || selectedTabIndex == 0) {
                    EditUserContent(
                        usernameState = usernameState,
                        emailState = emailState,
                        imageUrlState = imageUrlState,
                        passwordState = passwordState,
                        vipState = vipState,
                        adminState = adminState,
                        selectedImageUri = selectedImageUri,
                        showUrlFieldState = showUrlFieldState,
                        showPasswordField = showPasswordField,
                        activityPointsState = activityPointsState,
                        jlptLevelState = jlptLevelState,
                        studyMonthsState = studyMonthsState,
                        rankState = rankState
                    )

                    // Show upload error if any
                    uploadError?.let { error ->
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = error,
                            style = MaterialTheme.typography.bodySmall,
                            color = NihongoTheme.accentRed,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                } else {
                    NotifyContent(
                        user = user,
                        message = message,
                        onMessageChange = { message = it },
                        selectedSendType = selectedSendType,
                        onSendTypeChange = { selectedSendType = it },
                        selectedContentType = selectedContentType,
                        onContentTypeChange = { selectedContentType = it },
                        onAutoFillSelect = { message = it },
                        toEmail = emailToSend,
                        onSendResult = {
                            lastSendSuccess = it
                            showDialog = true
                        },
                        onDismiss = onDismiss
                    )
                }
            }
        }
    )
}

// Helper function to create and save user
private fun saveUser(
    existingUser: User?,
    username: String,
    email: String,
    imageUrl: String,
    password: String,
    isVip: Boolean,
    isAdmin: Boolean,
    activityPoints: Int,
    jlptLevel: Int?,
    studyMonths: Int?,
    rank: String,
    onSave: (User) -> Unit
) {
    val newUser = existingUser?.copy(
        username = username,
        email = email,
        imageUrl = imageUrl,
        password = if (password.isNotEmpty()) password else (existingUser.password ?: ""),
        vip = isVip,
        admin = isAdmin,
        activityPoints = activityPoints,
        jlptLevel = jlptLevel,
        studyMonths = studyMonths,
        rank = rank
    ) ?: User(
        username = username,
        email = email,
        imageUrl = imageUrl,
        password = password,
        vip = isVip,
        admin = isAdmin,
        activityPoints = activityPoints,
        jlptLevel = jlptLevel,
        studyMonths = studyMonths,
        rank = rank
    )
    onSave(newUser)
}

@Composable
private fun EditUserContent(
    usernameState: MutableState<String>,
    emailState: MutableState<String>,
    imageUrlState: MutableState<String>,
    passwordState: MutableState<String>,
    vipState: MutableState<Boolean>,
    adminState: MutableState<Boolean>,
    selectedImageUri: MutableState<Uri?>,
    showUrlFieldState: MutableState<Boolean>,
    showPasswordField: MutableState<Boolean>,
    activityPointsState: MutableState<String>,
    jlptLevelState: MutableState<Int?>,
    studyMonthsState: MutableState<Int?>,
    rankState: MutableState<String>,
) {
    val context = LocalContext.current
    val imagePickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            selectedImageUri.value = it
            // Don't update imageUrlState yet - we'll do that after upload
        }
    }

    val defaultImage = "https://ih1.redbubble.net/image.1629121092.8901/bg,f8f8f8-flat,750x,075,f-pad,750x1000,f8f8f8.jpg"
    
    // Password visibility toggle
    var passwordVisible by remember { mutableStateOf(false) }
    
    // JLPT Level options
    val jlptOptions = listOf("None", "N5", "N4", "N3", "N2", "N1")
    var jlptMenuExpanded by remember { mutableStateOf(false) }
    
    // Study months options
    val studyMonthsOptions = listOf("None", "1", "3", "6", "12", "18", "24", "36")
    var studyMonthsMenuExpanded by remember { mutableStateOf(false) }

    // Add scrolling support for the form
    val scrollState = rememberScrollState()

    Column(modifier = Modifier.verticalScroll(scrollState)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { imagePickerLauncher.launch("image/*") }
                .padding(bottom = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = rememberAsyncImagePainter(
                    model = selectedImageUri.value ?: (if (imageUrlState.value.isBlank()) defaultImage else imageUrlState.value)
                ),
                contentDescription = "User Image",
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .border(2.dp, NihongoTheme.primaryGreen, CircleShape),
                contentScale = ContentScale.Crop
            )

            // Show selected badge if an image is selected for upload
            if (selectedImageUri.value != null) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .offset((-8).dp, (-8).dp)
                        .size(24.dp)
                        .background(NihongoTheme.primaryGreen, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = "Image Selected",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        TextButton(
            onClick = { showUrlFieldState.value = !showUrlFieldState.value },
            colors = ButtonDefaults.textButtonColors(contentColor = NihongoTheme.primaryGreen)
        ) {
            Text(if (showUrlFieldState.value) "Pick Image" else "URL instead")
        }

        if (showUrlFieldState.value) {
            OutlinedTextField(
                value = imageUrlState.value,
                onValueChange = {
                    imageUrlState.value = it
                    selectedImageUri.value = null // Clear any selected image when using URL
                },
                label = { Text("Image URL") },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = NihongoTheme.primaryGreen,
                    unfocusedBorderColor = NihongoTheme.primaryGreen.copy(alpha = 0.5f),
                    cursorColor = NihongoTheme.primaryGreen
                )
            )
        }

        // Image selection info text
        if (selectedImageUri.value != null) {
            Text(
                text = "Image selected - will be uploaded when saved",
                style = MaterialTheme.typography.bodySmall,
                color = NihongoTheme.primaryGreen,
                modifier = Modifier.padding(top = 2.dp, bottom = 8.dp, start = 4.dp)
            )
        }

        OutlinedTextField(
            value = usernameState.value,
            onValueChange = { usernameState.value = it },
            label = { Text("Username") },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = NihongoTheme.primaryGreen,
                unfocusedBorderColor = NihongoTheme.primaryGreen.copy(alpha = 0.5f),
                cursorColor = NihongoTheme.primaryGreen
            )
        )

        OutlinedTextField(
            value = emailState.value,
            onValueChange = { emailState.value = it },
            label = { Text("Email") },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = NihongoTheme.primaryGreen,
                unfocusedBorderColor = NihongoTheme.primaryGreen.copy(alpha = 0.5f),
                cursorColor = NihongoTheme.primaryGreen
            )
        )

        // Password switch
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        ) {
            Text(
                text = "Change Password",
                style = MaterialTheme.typography.bodyMedium,
                color = NihongoTheme.textDark
            )
            Spacer(modifier = Modifier.weight(1f))
            Switch(
                checked = showPasswordField.value,
                onCheckedChange = { showPasswordField.value = it },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = NihongoTheme.primaryGreen,
                    checkedTrackColor = NihongoTheme.secondaryLightGreen,
                    uncheckedThumbColor = Color.Gray,
                    uncheckedTrackColor = Color.LightGray
                )
            )
        }

        // Password field only shows when switch is on - now with visibility toggle
        if (showPasswordField.value) {
            OutlinedTextField(
                value = passwordState.value,
                onValueChange = { passwordState.value = it },
                label = { Text("New Password") },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = NihongoTheme.primaryGreen,
                    unfocusedBorderColor = NihongoTheme.primaryGreen.copy(alpha = 0.5f),
                    cursorColor = NihongoTheme.primaryGreen
                ),
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            imageVector = if (passwordVisible) 
                                Icons.Default.Visibility 
                            else 
                                Icons.Default.VisibilityOff,
                            contentDescription = if (passwordVisible) "Hide password" else "Show password",
                            tint = NihongoTheme.primaryGreen
                        )
                    }
                }
            )

            Text(
                text = "Password will be hashed with SHA-256",
                style = MaterialTheme.typography.bodySmall,
                color = NihongoTheme.textDark.copy(alpha = 0.6f),
                modifier = Modifier.padding(top = 4.dp, start = 4.dp)
            )
        }
        
        // JLPT Level dropdown
        Text(
            "JLPT Level",
            style = MaterialTheme.typography.titleMedium,
            color = NihongoTheme.primaryGreen,
            modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
        )
        Box {
            OutlinedButton(
                onClick = { jlptMenuExpanded = true },
                modifier = Modifier.fillMaxWidth(),
                border = BorderStroke(1.dp, NihongoTheme.primaryGreen),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = NihongoTheme.primaryGreen
                )
            ) {
                Text(
                    if (jlptLevelState.value == null) "None" 
                    else "N${jlptLevelState.value}"
                )
            }

            DropdownMenu(
                expanded = jlptMenuExpanded,
                onDismissRequest = { jlptMenuExpanded = false }
            ) {
                jlptOptions.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option) },
                        onClick = {
                            jlptLevelState.value = when(option) {
                                "None" -> null
                                else -> option.substring(1).toInt()
                            }
                            jlptMenuExpanded = false
                        }
                    )
                }
            }
        }
        
        // Study Months dropdown
        Text(
            "Study Months",
            style = MaterialTheme.typography.titleMedium,
            color = NihongoTheme.primaryGreen,
            modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
        )
        Box {
            OutlinedButton(
                onClick = { studyMonthsMenuExpanded = true },
                modifier = Modifier.fillMaxWidth(),
                border = BorderStroke(1.dp, NihongoTheme.primaryGreen),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = NihongoTheme.primaryGreen
                )
            ) {
                Text(
                    if (studyMonthsState.value == null) "None" 
                    else "${studyMonthsState.value} months"
                )
            }

            DropdownMenu(
                expanded = studyMonthsMenuExpanded,
                onDismissRequest = { studyMonthsMenuExpanded = false }
            ) {
                studyMonthsOptions.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option) },
                        onClick = {
                            studyMonthsState.value = when(option) {
                                "None" -> null
                                else -> option.toInt()
                            }
                            studyMonthsMenuExpanded = false
                        }
                    )
                }
            }
        }
        
        // Activity Points
        OutlinedTextField(
            value = activityPointsState.value,
            onValueChange = { 
                val filtered = it.filter { char -> char.isDigit() }
                activityPointsState.value = filtered 
                // Auto update rank based on points
                rankState.value = when {
                    filtered.toIntOrNull() ?: 0 >= 1000 -> "Bậc thầy"
                    filtered.toIntOrNull() ?: 0 >= 750 -> "Chuyên gia"
                    filtered.toIntOrNull() ?: 0 >= 500 -> "Cao cấp"
                    filtered.toIntOrNull() ?: 0 >= 300 -> "Trung cấp"
                    filtered.toIntOrNull() ?: 0 >= 150 -> "Sơ cấp"
                    filtered.toIntOrNull() ?: 0 >= 50 -> "Người mới"
                    else -> "Tân binh"
                }
            },
            label = { Text("Activity Points") },
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = NihongoTheme.primaryGreen,
                unfocusedBorderColor = NihongoTheme.primaryGreen.copy(alpha = 0.5f),
                cursorColor = NihongoTheme.primaryGreen
            ),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
        )
        
        // Rank (read-only)
        OutlinedTextField(
            value = rankState.value,
            onValueChange = { },
            label = { Text("Rank") },
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = NihongoTheme.primaryGreen,
                unfocusedBorderColor = NihongoTheme.primaryGreen.copy(alpha = 0.5f),
                disabledBorderColor = NihongoTheme.primaryGreen.copy(alpha = 0.5f),
                disabledTextColor = NihongoTheme.textDark,
                disabledLabelColor = NihongoTheme.textDark
            ),
            enabled = false
        )

        // VIP user checkbox
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        ) {
            Checkbox(
                checked = vipState.value,
                onCheckedChange = { vipState.value = it },
                colors = CheckboxDefaults.colors(
                    checkedColor = NihongoTheme.primaryGreen,
                    uncheckedColor = NihongoTheme.textDark.copy(alpha = 0.6f)
                )
            )
            Text(
                "VIP User",
                color = NihongoTheme.textDark
            )
        }
        
        // Admin privilege checkbox
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        ) {
            Checkbox(
                checked = adminState.value,
                onCheckedChange = { adminState.value = it },
                colors = CheckboxDefaults.colors(
                    checkedColor = NihongoTheme.primaryGreen,
                    uncheckedColor = NihongoTheme.textDark.copy(alpha = 0.6f)
                )
            )
            Text(
                "Admin Privilege",
                color = NihongoTheme.textDark
            )
        }
    }
}

@Composable
fun NotifyContent(
    user: User,
    message: String,
    onMessageChange: (String) -> Unit,
    selectedSendType: String,
    onSendTypeChange: (String) -> Unit,
    selectedContentType: String,
    onContentTypeChange: (String) -> Unit,
    onAutoFillSelect: (String) -> Unit,
    toEmail: String,
    onSendResult: (Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    val sendOptions = listOf("Notification", "Email")
    val contentOptions = listOf("Thủ công", "Auto-fill")
    val autoFillTemplates = listOf(
        "Bạn chưa hoàn thành bài học hôm nay, hãy quay lại học nhé!",
        "Lớp học online đang bắt đầu, mời bạn tham gia.",
        "Bạn còn bài kiểm tra chưa làm, hãy hoàn thành sớm nhất."
    )

    var sendMenuExpanded by remember { mutableStateOf(false) }
    var contentMenuExpanded by remember { mutableStateOf(false) }
    var selectedTemplate by remember { mutableStateOf<String?>(null) }

    val scrollState = rememberScrollState()
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(scrollState)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(4.dp),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.White
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {

                // Send Type Dropdown
                Text(
                    "Send as",
                    style = MaterialTheme.typography.titleMedium,
                    color = NihongoTheme.primaryGreen
                )
                Box {
                    OutlinedButton(
                        onClick = { sendMenuExpanded = true },
                        modifier = Modifier.fillMaxWidth(),
                        border = BorderStroke(1.dp, NihongoTheme.primaryGreen),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = NihongoTheme.primaryGreen
                        )
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Start,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = if (selectedSendType == "Email") Icons.Default.Email else Icons.Default.Notifications,
                                contentDescription = null,
                                tint = NihongoTheme.primaryGreen
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(selectedSendType)
                        }
                    }

                    DropdownMenu(
                        expanded = sendMenuExpanded,
                        onDismissRequest = { sendMenuExpanded = false }
                    ) {
                        sendOptions.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option) },
                                onClick = {
                                    onSendTypeChange(option)
                                    sendMenuExpanded = false
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = if (option == "Email") Icons.Default.Email else Icons.Default.Notifications,
                                        contentDescription = null,
                                        tint = NihongoTheme.primaryGreen
                                    )
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Content Type Dropdown
                Text(
                    "Send Content",
                    style = MaterialTheme.typography.titleMedium,
                    color = NihongoTheme.primaryGreen
                )
                Box {
                    OutlinedButton(
                        onClick = { contentMenuExpanded = true },
                        modifier = Modifier.fillMaxWidth(),
                        border = BorderStroke(1.dp, NihongoTheme.primaryGreen),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = NihongoTheme.primaryGreen
                        )
                    ) {
                        Text(selectedContentType)
                    }

                    DropdownMenu(
                        expanded = contentMenuExpanded,
                        onDismissRequest = { contentMenuExpanded = false }
                    ) {
                        contentOptions.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option) },
                                onClick = {
                                    onContentTypeChange(option)
                                    contentMenuExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (selectedContentType == "Thủ công") {
                    Text(
                        "Nội dung",
                        style = MaterialTheme.typography.titleMedium,
                        color = NihongoTheme.primaryGreen
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = message,
                        onValueChange = onMessageChange,
                        placeholder = { Text("Nhập nội dung...") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp),
                        maxLines = 6,
                        textStyle = MaterialTheme.typography.bodyLarge,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NihongoTheme.primaryGreen,
                            unfocusedBorderColor = NihongoTheme.primaryGreen.copy(alpha = 0.5f),
                            cursorColor = NihongoTheme.primaryGreen
                        )
                    )
                } else {
                    Text(
                        "Select content sample",
                        style = MaterialTheme.typography.titleMedium,
                        color = NihongoTheme.primaryGreen
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        autoFillTemplates.forEach { template ->
                            TemplateOption(
                                template = template,
                                isSelected = template == selectedTemplate,
                                onClick = {
                                    selectedTemplate = template
                                    onAutoFillSelect(template)
                                    onMessageChange(template)
                                }
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Send button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            Button(
                onClick = {
                    if(selectedSendType == "Email"){
                        coroutineScope.launch {
                            val result = withContext(Dispatchers.IO) {
                                AdminEmailSender.sendEmail(
                                    toEmail = toEmail,
                                    subject = "Thông báo từ Nihongo",
                                    body = message
                                )
                            }
                            onSendResult(result)
                            if (result) {
                                Toast.makeText(context, "Đã gửi thành công!", Toast.LENGTH_SHORT).show()
                                onDismiss()
                            } else {
                                Toast.makeText(context, "Gửi thất bại, vui lòng thử lại", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                    else {
                        val campaign = Campaign(
                            title = "Thông báo từ Nihongo",
                            message = message,
                            imageUrl = "",
                            isScheduled = false,
                            isDaily = false,
                            scheduledFor = Timestamp.now(),
                            dailyHour = 0,
                            dailyMinute = 0
                        )
                        val viewModel = AdminNotifyPageViewModel()
                        viewModel.sendNotificationToUser(campaign, context, user.email)
                        onDismiss()
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = NihongoTheme.primaryGreen,
                    contentColor = Color.White
                )
            ) {
                Icon(
                    imageVector = if (selectedSendType == "Email") Icons.Default.Email else Icons.Default.Notifications,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Gửi")
            }
        }
    }
}

@Composable
fun TemplateOption(template: String, isSelected: Boolean, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(
            width = if (isSelected) 2.dp else 1.dp,
            color = if (isSelected) NihongoTheme.primaryGreen else Color.LightGray
        ),
        color = if (isSelected)
            NihongoTheme.secondaryLightGreen.copy(alpha = 0.2f)
        else
            Color.White,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(
                selected = isSelected,
                onClick = onClick,
                colors = RadioButtonDefaults.colors(
                    selectedColor = NihongoTheme.primaryGreen,
                    unselectedColor = Color.Gray
                )
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = template,
                style = MaterialTheme.typography.bodyMedium,
                color = NihongoTheme.textDark
            )
        }
    }
}