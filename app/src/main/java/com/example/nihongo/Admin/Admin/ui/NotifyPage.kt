package com.example.nihongo.Admin.ui

import Campaign
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.net.Uri
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Title
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.Badge
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.nihongo.Admin.utils.ImgurUploader
import com.example.nihongo.Admin.viewmodel.AdminNotifyPageViewModel
import com.google.firebase.Timestamp
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotifyPage(viewModel: AdminNotifyPageViewModel = AdminNotifyPageViewModel()) {

    UIVisibilityController.disableDisplayTopBar()

    var title by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    var imageUrl by remember { mutableStateOf("") }
    var isScheduled by remember { mutableStateOf(false) }
    var isDaily by remember { mutableStateOf(false) }
    var scheduledDate by remember { mutableStateOf<Date?>(null) }
    var dailyHour by remember { mutableStateOf(9) } // Default 9 AM
    var dailyMinute by remember { mutableStateOf(0) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var showDailyTimePicker by remember { mutableStateOf(false) }
    var activeTab by remember { mutableStateOf(0) }

    // Edit mode states
    var isEditMode by remember { mutableStateOf(false) }
    var currentEditCampaignId by remember { mutableStateOf("") }

    // Thêm các biến trạng thái cho chọn ảnh
    var selectedImageType by remember { mutableStateOf("url") } // "url", "default", "upload"
    var selectedDefaultImage by remember { mutableStateOf("") }
    var uploadedImageUri by remember { mutableStateOf<Uri?>(null) }
    var showImagePreview by remember { mutableStateOf(false) }

    // Danh sách ảnh mặc định
    val defaultImages = listOf(
        "https://kosei.vn/uploads/hoc-kanji-moi-ngay-tu-muc.jpg",
        "https://japan.net.vn/images/uploads/2019/04/26/0-danh-ngon-tieng-nhat-hay.jpg",
        "https://storage.dekiru.vn/Data/2019/07/08/tai-xuong-636981962504233046.jpg"
    )

    // Add coroutine scope for image uploading
    val scope = rememberCoroutineScope()
    
    // Upload states
    var isUploading by remember { mutableStateOf(false) }
    var uploadError by remember { mutableStateOf<String?>(null) }

    // Initialize imgur uploader
    val imgurUploader = remember { ImgurUploader() }
    val catboxUploader =  com.example.nihongo.Admin.utils.CatboxUploader
    val context = LocalContext.current

    // Helper function to get file from Uri
    fun getFileFromUri(uri: Uri): File? {
        val contentResolver = context.contentResolver
        val tempFile = try {
            // Get the file name
            val fileName = contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                cursor.moveToFirst()
                cursor.getString(nameIndex)
            } ?: "temp_image_${System.currentTimeMillis()}.jpg"
            
            // Create temporary file
            File.createTempFile("upload_", fileName, context.cacheDir)
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
        
        try {
            // Copy URI content to temp file
            contentResolver.openInputStream(uri)?.use { inputStream ->
                tempFile.outputStream().use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
            return tempFile
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    // Launcher để chọn ảnh từ máy
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uploadedImageUri = uri
        if (uri != null) {
            showImagePreview = true
            // Start upload when image is selected
            isUploading = true
            uploadError = null
            
            scope.launch {
                try {
                    getFileFromUri(uri)?.let { file ->
//                        val imgurUrl = imgurUploader.uploadImage(file)
                        val catboxUrl = catboxUploader.uploadVideo(file)
                        if (catboxUrl != null) {
                            imageUrl = catboxUrl
                        } else {
                            uploadError = "Failed to upload image"
                        }
                    } ?: run {
                        uploadError = "Error processing image file"
                    }
                } catch (e: Exception) {
                    uploadError = "Error: ${e.message}"
                } finally {
                    isUploading = false
                }
            }
        }
    }

    val campaigns by viewModel.campaigns.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    val calendar = Calendar.getInstance()

    val dateFormatter = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
    val timeFormatter = SimpleDateFormat("HH:mm", Locale.getDefault())

    // Create a scroll state that we can programmatically control
    val scrollState = rememberScrollState()

    // Define green color scheme
    val greenColorScheme = darkColorScheme(
        primary = Color(0xFF4CAF50),        // Main green
        onPrimary = Color.White,
        primaryContainer = Color(0xFFB9F6CA), // Light green for containers
        onPrimaryContainer = Color(0xFF002200),
        secondary = Color(0xFF8BC34A),      // Lighter green for secondary elements
        onSecondary = Color.White,
        secondaryContainer = Color(0xFFDCEDC8),
        onSecondaryContainer = Color(0xFF1B5E20),
        tertiary = Color(0xFF009688),       // Teal for tertiary elements
        background = Color(0xFFF1F8E9),     // Light green background
        surface = Color.White,
        onSurface = Color(0xFF1B5E20),      // Dark green text on surface
        error = Color(0xFFF44336)           // Red for errors
    )

    // Function to reset form fields
    fun resetForm() {
        title = ""
        message = ""
        imageUrl = ""
        isScheduled = false
        isDaily = false
        scheduledDate = null
        dailyHour = 9
        dailyMinute = 0
        isEditMode = false
        currentEditCampaignId = ""
        // Reset image-related states
        selectedImageType = "url"
        selectedDefaultImage = ""
        uploadedImageUri = null
        showImagePreview = false
        uploadError = null
    }

    // Function to load campaign data for editing
    fun loadCampaignForEdit(campaign: Campaign) {
        title = campaign.title
        message = campaign.message
        imageUrl = campaign.imageUrl ?: ""
        isScheduled = campaign.isScheduled
        isDaily = campaign.isDaily
        scheduledDate = campaign.scheduledFor?.toDate()
        dailyHour = campaign.dailyHour
        dailyMinute = campaign.dailyMinute
        isEditMode = true
        currentEditCampaignId = campaign.id
        activeTab = 0 // Switch to create/edit tab
    }

    // Time pickers
    if (showDatePicker) {
        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                calendar.set(year, month, dayOfMonth)
                scheduledDate = calendar.time
                showDatePicker = false
                showTimePicker = true
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).apply {
            setOnCancelListener {
                showDatePicker = false
            }
        }.show()
    }

    if (showTimePicker) {
        TimePickerDialog(
            context,
            { _, hourOfDay, minute ->
                calendar.apply {
                    time = scheduledDate ?: Date()
                    set(Calendar.HOUR_OF_DAY, hourOfDay)
                    set(Calendar.MINUTE, minute)
                    set(Calendar.SECOND, 0)
                }
                scheduledDate = calendar.time
                showTimePicker = false
            },
            calendar.get(Calendar.HOUR_OF_DAY),
            calendar.get(Calendar.MINUTE),
            true
        ).apply {
            setOnCancelListener {
                showTimePicker = false
            }
        }.show()
    }

    if (showDailyTimePicker) {
        TimePickerDialog(
            context,
            { _, hourOfDay, minute ->
                dailyHour = hourOfDay
                dailyMinute = minute
                showDailyTimePicker = false
            },
            dailyHour,
            dailyMinute,
            true
        ).apply {
            setOnCancelListener {
                showDailyTimePicker = false
            }
        }.show()
    }

    MaterialTheme(
        colorScheme = greenColorScheme
    ) {
        Scaffold(
            topBar = {
                Surface(
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "Notification Manager",
                                color = MaterialTheme.colorScheme.onPrimary,
                                style = MaterialTheme.typography.headlineMedium
                            )
                        }

                        TabRow(
                            selectedTabIndex = activeTab,
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                            indicator = { tabPositions ->
                                TabRowDefaults.Indicator(
                                    modifier = Modifier.tabIndicatorOffset(tabPositions[activeTab]),
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            }
                        ) {
                            Tab(
                                selected = activeTab == 0,
                                onClick = { activeTab = 0 },
                                text = {
                                    Text(
                                        if (isEditMode) "Edit Notification" else "Create Notification",
                                        fontWeight = FontWeight.Bold
                                    )
                                },
                                icon = {
                                    Icon(
                                        imageVector = if (isEditMode) Icons.Default.Edit else Icons.Default.Create,
                                        contentDescription = if (isEditMode) "Edit" else "Create"
                                    )
                                }
                            )
                            Tab(
                                selected = activeTab == 1,
                                onClick = {
                                    activeTab = 1
                                    resetForm() // Reset form when switching to list view
                                },
                                text = {
                                    Text(
                                        "Scheduled Campaigns",
                                        fontWeight = FontWeight.Bold
                                    )
                                },
                                icon = {
                                    Icon(
                                        imageVector = Icons.Default.List,
                                        contentDescription = "List"
                                    )
                                }
                            )
                        }
                    }
                }
            }
        ) { paddingValues ->
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background
            ) {
                when (activeTab) {
                    0 -> {
                        // Create/Edit Notification Tab
                        Column(
                            modifier = Modifier
                                .padding(paddingValues)
                                .fillMaxSize()
                                .verticalScroll(scrollState)
                                .padding(16.dp)
                                // Add extra padding at the bottom to ensure content isn't covered by navigation bar
                                .padding(bottom = 80.dp)
                        ) {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 16.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surface
                                ),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp)
                                ) {
                                    Text(
                                        if (isEditMode) "Edit Notification Campaign" else "Create New Notification",
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )

                                    Spacer(modifier = Modifier.height(16.dp))

                                    OutlinedTextField(
                                        value = title,
                                        onValueChange = { title = it },
                                        label = { Text("Title") },
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                                            focusedLabelColor = MaterialTheme.colorScheme.primary
                                        ),
                                        leadingIcon = {
                                            Icon(
                                                imageVector = Icons.Default.Title,
                                                contentDescription = "Title",
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    )

                                    Spacer(modifier = Modifier.height(12.dp))

                                    OutlinedTextField(
                                        value = message,
                                        onValueChange = { message = it },
                                        label = { Text("Message") },
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                                            focusedLabelColor = MaterialTheme.colorScheme.primary
                                        ),
                                        minLines = 3,
                                        leadingIcon = {
                                            Icon(
                                                imageVector = Icons.Default.Message,
                                                contentDescription = "Message",
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    )

                                    Spacer(modifier = Modifier.height(12.dp))

                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(bottom = 16.dp),
                                        colors = CardDefaults.cardColors(
                                            containerColor = MaterialTheme.colorScheme.surface
                                        ),
                                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                                    ) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(16.dp)
                                        ) {
                                            Text(
                                                "Select notification image (Optional)",
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                            Spacer(modifier = Modifier.height(8.dp))

                                            // Lựa chọn kiểu ảnh
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                OutlinedButton(
                                                    onClick = {
                                                        selectedImageType = "url"
                                                        showImagePreview = false
                                                    },
                                                    modifier = Modifier.weight(1f),
                                                    colors = ButtonDefaults.outlinedButtonColors(
                                                        containerColor = if (selectedImageType == "url") 
                                                            MaterialTheme.colorScheme.primaryContainer 
                                                        else 
                                                            MaterialTheme.colorScheme.surface,
                                                        contentColor = if (selectedImageType == "url") 
                                                            MaterialTheme.colorScheme.onPrimaryContainer
                                                        else 
                                                            MaterialTheme.colorScheme.primary
                                                    )
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Link,
                                                        contentDescription = "URL",
                                                        modifier = Modifier.size(20.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                }

                                                OutlinedButton(
                                                    onClick = {
                                                        selectedImageType = "default"
                                                        showImagePreview = false
                                                    },
                                                    modifier = Modifier.weight(1f),
                                                    colors = ButtonDefaults.outlinedButtonColors(
                                                        containerColor = if (selectedImageType == "default") 
                                                            MaterialTheme.colorScheme.primaryContainer 
                                                        else 
                                                            MaterialTheme.colorScheme.surface,
                                                        contentColor = if (selectedImageType == "default") 
                                                            MaterialTheme.colorScheme.onPrimaryContainer
                                                        else 
                                                            MaterialTheme.colorScheme.primary
                                                    )
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Image,
                                                        contentDescription = "Default",
                                                        modifier = Modifier.size(20.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                }

                                                OutlinedButton(
                                                    onClick = {
                                                        selectedImageType = "upload"
                                                        showImagePreview = false
                                                    },
                                                    modifier = Modifier.weight(1f),
                                                    colors = ButtonDefaults.outlinedButtonColors(
                                                        containerColor = if (selectedImageType == "upload") 
                                                            MaterialTheme.colorScheme.primaryContainer 
                                                        else 
                                                            MaterialTheme.colorScheme.surface,
                                                        contentColor = if (selectedImageType == "upload") 
                                                            MaterialTheme.colorScheme.onPrimaryContainer
                                                        else 
                                                            MaterialTheme.colorScheme.primary
                                                    )
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Upload,
                                                        contentDescription = "Upload",
                                                        modifier = Modifier.size(20.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                }
                                            }

                                            Spacer(modifier = Modifier.height(16.dp))

                                            // Nhập URL
                                            if (selectedImageType == "url") {
                                                OutlinedTextField(
                                                    value = imageUrl,
                                                    onValueChange = {
                                                        imageUrl = it
                                                        showImagePreview = it.isNotBlank()
                                                    },
                                                    label = { Text("Image URL") },
                                                    modifier = Modifier.fillMaxWidth(),
                                                    leadingIcon = {
                                                        Icon(
                                                            imageVector = Icons.Default.Link,
                                                            contentDescription = "URL",
                                                            tint = MaterialTheme.colorScheme.primary
                                                        )
                                                    }
                                                )
                                            }

                                            // Chọn ảnh mặc định
                                            if (selectedImageType == "default") {
                                                LazyRow(
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                    contentPadding = PaddingValues(vertical = 8.dp)
                                                ) {
                                                    items(defaultImages) { img ->
                                                        Card(
                                                            modifier = Modifier
                                                                .size(100.dp)
                                                                .clickable {
                                                                    selectedDefaultImage = img
                                                                    showImagePreview = true
                                                                },
                                                            colors = CardDefaults.cardColors(
                                                                containerColor = if (selectedDefaultImage == img) 
                                                                    MaterialTheme.colorScheme.primaryContainer 
                                                                else 
                                                                    MaterialTheme.colorScheme.surface
                                                            ),
                                                            border = BorderStroke(
                                                                width = if (selectedDefaultImage == img) 2.dp else 1.dp,
                                                                color = if (selectedDefaultImage == img) 
                                                                    MaterialTheme.colorScheme.primary 
                                                                else 
                                                                    MaterialTheme.colorScheme.outline
                                                            )
                                                        ) {
                                                            AsyncImage(
                                                                model = img,
                                                                contentDescription = null,
                                                                modifier = Modifier.fillMaxSize(),
                                                                contentScale = ContentScale.Crop
                                                            )
                                                        }
                                                    }
                                                }
                                            }

                                            // Upload từ máy
                                            if (selectedImageType == "upload") {
                                                OutlinedButton(
                                                    onClick = { launcher.launch("image/*") },
                                                    modifier = Modifier.fillMaxWidth(),
                                                    colors = ButtonDefaults.outlinedButtonColors(
                                                        containerColor = MaterialTheme.colorScheme.surface,
                                                        contentColor = MaterialTheme.colorScheme.primary
                                                    ),
                                                    enabled = !isUploading
                                                ) {
                                                    if (isUploading) {
                                                        CircularProgressIndicator(
                                                            modifier = Modifier.size(20.dp),
                                                            color = MaterialTheme.colorScheme.primary,
                                                            strokeWidth = 2.dp
                                                        )
                                                        Spacer(modifier = Modifier.width(8.dp))
                                                        Text("Uploading...")
                                                    } else {
                                                        Icon(
                                                            imageVector = Icons.Default.Upload,
                                                            contentDescription = "Upload",
                                                            modifier = Modifier.size(20.dp)
                                                        )
                                                        Spacer(modifier = Modifier.width(8.dp))
                                                        Text("Chọn ảnh từ máy")
                                                    }
                                                }
                                                
                                                uploadError?.let { error ->
                                                    Text(
                                                        text = error,
                                                        color = MaterialTheme.colorScheme.error,
                                                        fontSize = 12.sp,
                                                        modifier = Modifier.padding(top = 4.dp)
                                                    )
                                                }
                                            }

                                            // Preview ảnh
                                            val previewUrl = when {
                                                selectedImageType == "url" && imageUrl.isNotBlank() -> imageUrl
                                                selectedImageType == "default" && selectedDefaultImage.isNotBlank() -> selectedDefaultImage
                                                selectedImageType == "upload" && uploadedImageUri != null -> uploadedImageUri.toString()
                                                else -> null
                                            }
                                            if (showImagePreview && previewUrl != null) {
                                                Spacer(modifier = Modifier.height(16.dp))
                                                Card(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    colors = CardDefaults.cardColors(
                                                        containerColor = MaterialTheme.colorScheme.surface
                                                    )
                                                ) {
                                                    AsyncImage(
                                                        model = previewUrl,
                                                        contentDescription = "Preview",
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .height(200.dp),
                                                        contentScale = ContentScale.Crop
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            Card(
                                modifier = Modifier
                                    .fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surface
                                ),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp)
                                ) {
                                    // Notification Type Selection
                                    Text(
                                        "Notification Type",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )

                                    Spacer(modifier = Modifier.height(8.dp))

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 8.dp)
                                            .clickable {
                                                isScheduled = false
                                                isDaily = false
                                            }
                                    ) {
                                        RadioButton(
                                            selected = !isScheduled && !isDaily,
                                            onClick = {
                                                isScheduled = false
                                                isDaily = false
                                            },
                                            colors = RadioButtonDefaults.colors(
                                                selectedColor = MaterialTheme.colorScheme.primary
                                            )
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Icon(
                                            imageVector = Icons.Default.Send,
                                            contentDescription = "Send immediately",
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Send immediately")
                                    }

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 8.dp)
                                    ) {
                                        RadioButton(
                                            selected = isScheduled,
                                            onClick = {
                                                isScheduled = true
                                                isDaily = false
                                                // Reset dialog states when switching to scheduled
                                                showDatePicker = false
                                                showTimePicker = false
                                                showDailyTimePicker = false
                                            },
                                            colors = RadioButtonDefaults.colors(
                                                selectedColor = MaterialTheme.colorScheme.primary
                                            )
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Icon(
                                            imageVector = Icons.Default.Schedule,
                                            contentDescription = "Schedule for later",
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Schedule for later")
                                        Spacer(modifier = Modifier.width(8.dp))
                                        if (isScheduled) {
                                            OutlinedButton(
                                                onClick = { showDatePicker = true },
                                                border = BorderStroke(
                                                    1.dp,
                                                    MaterialTheme.colorScheme.primary
                                                ),
                                                colors = ButtonDefaults.outlinedButtonColors(
                                                    contentColor = MaterialTheme.colorScheme.primary
                                                )
                                            ) {
                                                Text(scheduledDate?.let {
                                                    dateFormatter.format(it) + " " + timeFormatter.format(
                                                        it
                                                    )
                                                } ?: "date & time")
                                            }
                                        }
                                    }

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 8.dp)
                                    ) {
                                        RadioButton(
                                            selected = isDaily,
                                            onClick = {
                                                isDaily = true
                                                isScheduled = false
                                                // Reset dialog states when switching to daily
                                                showDatePicker = false
                                                showTimePicker = false
                                                showDailyTimePicker = false
                                            },
                                            colors = RadioButtonDefaults.colors(
                                                selectedColor = MaterialTheme.colorScheme.primary
                                            )
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Icon(
                                            imageVector = Icons.Default.Repeat,
                                            contentDescription = "Send daily",
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Send daily")
                                        Spacer(modifier = Modifier.width(8.dp))
                                        if (isDaily) {
                                            OutlinedButton(
                                                onClick = { showDailyTimePicker = true },
                                                border = BorderStroke(
                                                    1.dp,
                                                    MaterialTheme.colorScheme.primary
                                                ),
                                                colors = ButtonDefaults.outlinedButtonColors(
                                                    contentColor = MaterialTheme.colorScheme.primary
                                                )
                                            ) {
                                                Text(
                                                    "${String.format("%02d", dailyHour)}:${String.format("%02d", dailyMinute)}"
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(24.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        if (isEditMode) {
                                            Button(
                                                onClick = { resetForm() },
                                                modifier = Modifier.weight(1f),
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = MaterialTheme.colorScheme.secondary
                                                )
                                            ) {
                                                Text("Cancel")
                                            }
                                        }

                                        Button(
                                            onClick = {
                                                val finalImageUrl = when {
                                                    selectedImageType == "url" -> imageUrl
                                                    selectedImageType == "default" -> selectedDefaultImage
                                                    selectedImageType == "upload" && imageUrl.isNotBlank() -> imageUrl
                                                    else -> ""
                                                }

                                                val campaign = Campaign(
                                                    id = if (isEditMode) currentEditCampaignId else "",
                                                    title = title,
                                                    message = message,
                                                    imageUrl = if (finalImageUrl.isBlank()) null else finalImageUrl,
                                                    isScheduled = isScheduled,
                                                    isDaily = isDaily,
                                                    scheduledFor = scheduledDate?.let { Timestamp(it) },
                                                    dailyHour = dailyHour,
                                                    dailyMinute = dailyMinute
                                                )

                                                if (isEditMode) {
                                                    viewModel.updateCampaign(campaign, context)
                                                    Toast.makeText(
                                                        context,
                                                        "Campaign updated successfully",
                                                        Toast.LENGTH_SHORT
                                                    ).show()
                                                } else {
                                                    if (isScheduled && scheduledDate != null) {
                                                        viewModel.saveCampaign(context, campaign)
                                                        Toast.makeText(
                                                            context,
                                                            "Campaign scheduled successfully",
                                                            Toast.LENGTH_SHORT
                                                        ).show()
                                                    } else if (isDaily) {
                                                        viewModel.saveCampaign(context, campaign)
                                                        Toast.makeText(
                                                            context,
                                                            "Daily campaign created successfully",
                                                            Toast.LENGTH_SHORT
                                                        ).show()
                                                    } else {
                                                        viewModel.sendCampaign(campaign, context)
                                                        Toast.makeText(
                                                            context,
                                                            "Notification sent successfully",
                                                            Toast.LENGTH_SHORT
                                                        ).show()
                                                    }
                                                }

                                                // Clear fields after sending or updating
                                                resetForm()

                                                // Scroll back to top after submitting
                                                scope.launch {
                                                    scrollState.animateScrollTo(0)
                                                }
                                            },
                                            modifier = Modifier.weight(if (isEditMode) 1f else 1f),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = MaterialTheme.colorScheme.primary
                                            ),
                                            enabled = title.isNotBlank() && message.isNotBlank() &&
                                                    (!isScheduled || scheduledDate != null)
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.Center
                                            ) {
                                                Icon(
                                                    imageVector = when {
                                                        isEditMode -> Icons.Default.Save
                                                        !isScheduled && !isDaily -> Icons.Default.Send
                                                        isScheduled -> Icons.Default.Schedule
                                                        else -> Icons.Default.Repeat
                                                    },
                                                    contentDescription = "Action icon"
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    when {
                                                        isEditMode -> "Save Changes"
                                                        !isScheduled && !isDaily -> "Send Now"
                                                        isScheduled -> "Schedule Notification"
                                                        else -> "Set Up Daily Notification"
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    1 -> {
                        // Campaigns List Tab
                        if (isLoading) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                            }
                        } else {
                            Box(
                                modifier = Modifier
                                    .padding(paddingValues)
                                    .fillMaxSize()
                            ) {
                                LazyColumn(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(horizontal = 16.dp, vertical = 8.dp)
                                        // Add padding at the bottom to prevent FAB from covering content
                                        .padding(bottom = 80.dp)
                                ) {
                                    item {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 16.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Notifications,
                                                contentDescription = "Notifications",
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(28.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                "Scheduled Campaigns",
                                                style = MaterialTheme.typography.titleLarge,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                        Divider(color = MaterialTheme.colorScheme.primaryContainer)
                                    }

                                    if (campaigns.isEmpty()) {
                                        item {
                                            Card(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(vertical = 32.dp),
                                                colors = CardDefaults.cardColors(
                                                    containerColor = MaterialTheme.colorScheme.surface
                                                ),
                                                elevation = CardDefaults.cardElevation(
                                                    defaultElevation = 2.dp
                                                )
                                            ) {
                                                Column(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(32.dp),
                                                    horizontalAlignment = Alignment.CenterHorizontally
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.NotificationsOff,
                                                        contentDescription = "No notifications",
                                                        tint = MaterialTheme.colorScheme.primary,
                                                        modifier = Modifier.size(48.dp)
                                                    )
                                                    Spacer(modifier = Modifier.height(16.dp))
                                                    Text(
                                                        "No scheduled campaigns",
                                                        style = MaterialTheme.typography.titleMedium,
                                                        color = MaterialTheme.colorScheme.onSurface.copy(
                                                            alpha = 0.7f
                                                        )
                                                    )
                                                    Spacer(modifier = Modifier.height(16.dp))
                                                    Button(
                                                        onClick = { activeTab = 0 },
                                                        colors = ButtonDefaults.buttonColors(
                                                            containerColor = MaterialTheme.colorScheme.primary
                                                        )
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.Add,
                                                            contentDescription = "Create campaign"
                                                        )
                                                        Spacer(modifier = Modifier.width(8.dp))
                                                        Text("Create Campaign")
                                                    }
                                                }
                                            }
                                        }
                                    } else {
                                        items(campaigns) { campaign ->
                                            CampaignItem(
                                                campaign = campaign,
                                                onDelete = {
                                                    viewModel.deleteCampaign(campaign.id, context)
                                                    Toast.makeText(
                                                        context,
                                                        "Campaign deleted",
                                                        Toast.LENGTH_SHORT
                                                    ).show()
                                                },
                                                onSendNow = {
                                                    viewModel.sendCampaign(campaign, context)
                                                    Toast.makeText(
                                                        context,
                                                        "Notification sent",
                                                        Toast.LENGTH_SHORT
                                                    ).show()
                                                },
                                                onEdit = { loadCampaignForEdit(campaign) }
                                            )
                                            Spacer(modifier = Modifier.height(8.dp))
                                        }
                                    }
                                }

                                // FAB to add new campaign
                                FloatingActionButton(
                                    onClick = {
                                        resetForm()
                                        activeTab = 0
                                    },
                                    modifier = Modifier
                                        .align(Alignment.BottomEnd)
                                        .padding(16.dp),
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = "Add New Campaign")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CampaignItem(
    campaign: Campaign,
    onDelete: () -> Unit,
    onSendNow: () -> Unit,
    onEdit: () -> Unit
) {
    val dateFormatter = SimpleDateFormat("dd MMM yyyy HH:mm", Locale.getDefault())
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        onClick = { expanded = !expanded }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    when {
                        campaign.isDaily -> {
                            Icon(
                                imageVector = Icons.Default.Repeat,
                                contentDescription = "Daily",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .size(24.dp)
                                    .padding(end = 8.dp)
                            )
                        }
                        campaign.isScheduled -> {
                            Icon(
                                imageVector = Icons.Default.Schedule,
                                contentDescription = "Scheduled",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .size(24.dp)
                                    .padding(end = 8.dp)
                            )
                        }
                        else -> {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Sent",
                                tint = Color.Green,
                                modifier = Modifier
                                    .size(24.dp)
                                    .padding(end = 8.dp)
                            )
                        }
                    }

                    Text(
                        text = campaign.title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                IconButton(onClick = { expanded = !expanded }) {
                    Icon(
                        imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = if (expanded) "Show less" else "Show more",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = campaign.message,
                maxLines = if (expanded) Int.MAX_VALUE else 2,
                overflow = TextOverflow.Ellipsis,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Status indicator
            Row(verticalAlignment = Alignment.CenterVertically) {
                when {
                    campaign.isDaily -> {
                        Badge(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ) {
                            Text(
                                text = "Daily at ${String.format("%02d", campaign.dailyHour)}:${String.format("%02d", campaign.dailyMinute)}",
                                fontSize = 12.sp,
                                modifier = Modifier.padding(horizontal = 4.dp)
                            )
                        }
                    }
                    campaign.isScheduled -> {
                        Badge(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                        ) {
                            Text(
                                text = campaign.scheduledFor?.toDate()?.let { dateFormatter.format(it) } ?: "Scheduled",
                                fontSize = 12.sp,
                                modifier = Modifier.padding(horizontal = 4.dp)
                            )
                        }
                    }
                    else -> {
                        Badge(
                            containerColor = Color.Green.copy(alpha = 0.2f),
                            contentColor = Color.Green
                        ) {
                            Text(
                                text = "Sent",
                                fontSize = 12.sp,
                                modifier = Modifier.padding(horizontal = 4.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Last sent indicator if applicable
                campaign.lastSent?.toDate()?.let { lastSentDate ->
                    Text(
                        text = "Last sent: ${dateFormatter.format(lastSentDate)}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }

            if (expanded) {
                Spacer(modifier = Modifier.height(16.dp))
                Divider(color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
                Spacer(modifier = Modifier.height(8.dp))

                // Campaign image preview if available
                campaign.imageUrl?.let { url ->
                    if (url.isNotEmpty()) {
                        Text(
                            "Image URL:",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                        Text(
                            url,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onEdit,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit"
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                        }
                    }

                    OutlinedButton(
                        onClick = onSendNow,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.secondary
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Send,
                                contentDescription = "Send Now"
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                        }
                    }

                    OutlinedButton(
                        onClick = onDelete,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete"
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                        }
                    }
                }
            }
        }
    }
}