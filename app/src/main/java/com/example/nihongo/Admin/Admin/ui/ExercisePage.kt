package com.example.nihongo.Admin.ui

import android.net.Uri
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.nihongo.Admin.viewmodel.AdminExerciseViewModel
import com.example.nihongo.User.data.models.Exercise
import com.example.nihongo.User.data.models.ExerciseType
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.ui.AspectRatioFrameLayout
import com.google.android.exoplayer2.ui.PlayerView
import kotlinx.coroutines.delay
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import com.example.nihongo.Admin.utils.CatboxUploader
import com.example.nihongo.Admin.utils.ImgurUploader
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExercisePage(
    lessonId: String,
    subLessonId: String,
    onBack: () -> Unit,
    viewModel: AdminExerciseViewModel = viewModel()
) {
    BackHandler {
        onBack()
    }

    val exercises by viewModel.exercises.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val currentExercise by viewModel.currentExercise.collectAsState()

    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    // Dialog states
    var showAddDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var selectedExerciseId by remember { mutableStateOf<String?>(null) }

    // Load exercises when component is first created
    LaunchedEffect(lessonId, subLessonId) {
        viewModel.loadExercises(lessonId, subLessonId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Exercises Management") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        viewModel.setCurrentExercise(viewModel.createEmptyExercise(subLessonId))
                        showAddDialog = true
                    }) {
                        Icon(Icons.Default.Add, contentDescription = "Add Exercise")
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else {
                if (exercises.isEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "No exercises found",
                            fontSize = 18.sp,
                            color = Color.Gray
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = {
                                viewModel.setCurrentExercise(viewModel.createEmptyExercise(subLessonId))
                                showAddDialog = true
                            }
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Add Exercise")
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                    ) {
                        items(exercises) { exercise ->
                            ExerciseItem(
                                exercise = exercise,
                                onEdit = {
                                    viewModel.setCurrentExercise(exercise)
                                    showEditDialog = true
                                },
                                onDelete = {
                                    viewModel.setCurrentExercise(exercise)
                                    selectedExerciseId = exercise.id
                                    showDeleteDialog = true
                                }
                            )
                        }
                    }
                }
            }

            // Error message
            errorMessage?.let { error ->
                Snackbar(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp),
                    action = {
                        TextButton(onClick = { viewModel.clearError() }) {
                            Text("Dismiss", color = Color.White)
                        }
                    }
                ) {
                    Text(error)
                }
            }
        }
    }

    // Add Exercise Dialog
    if (showAddDialog) {
        currentExercise?.let { exercise ->
            ExerciseDialog(
                exercise = exercise,
                isNew = true,
                onDismiss = { showAddDialog = false },
                onSave = { updatedExercise ->
                    viewModel.createExercise(
                        lessonId = lessonId,
                        exercise = updatedExercise,
                        onSuccess = { showAddDialog = false },
                        onError = { /* Error is handled by viewModel */ }
                    )
                }
            )
        }
    }

    // Edit Exercise Dialog
    if (showEditDialog) {
        currentExercise?.let { exercise ->
            ExerciseDialog(
                exercise = exercise,
                isNew = false,
                onDismiss = { showEditDialog = false },
                onSave = { updatedExercise ->
                    viewModel.updateExercise(
                        lessonId = lessonId,
                        exercise = updatedExercise,
                        onSuccess = { showEditDialog = false },
                        onError = { /* Error is handled by viewModel */ }
                    )
                }
            )
        }
    }

    // Delete Confirmation Dialog
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Exercise") },
            text = { Text("Are you sure you want to delete this exercise? This action cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        selectedExerciseId?.let { exerciseId ->
                            viewModel.deleteExercise(
                                lessonId = lessonId,
                                exerciseId = exerciseId,
                                subLessonId = subLessonId,
                                onSuccess = { showDeleteDialog = false },
                                onError = { /* Error is handled by viewModel */ }
                            )
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun ExerciseItem(
    exercise: Exercise,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val context = LocalContext.current
    var showVideo by remember { mutableStateOf(false) }
    var showAudio by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clickable(onClick = onEdit),
        shape = RoundedCornerShape(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Exercise type badge
            Box(
                modifier = Modifier
                    .background(
                        when (exercise.type) {
                            ExerciseType.VIDEO -> Color(0xFF2196F3)
                            ExerciseType.PRACTICE -> Color(0xFF4CAF50)
                            else -> Color.Gray
                        },
                        shape = RoundedCornerShape(4.dp)
                    )
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = exercise.type?.name ?: "UNKNOWN",
                    color = Color.White,
                    fontSize = 12.sp
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Title if available
            exercise.title?.let {
                if (it.isNotEmpty()) {
                    Text(
                        text = it,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            // Question
            Text(
                text = exercise.question ?: "No question",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Image if available
            exercise.imageUrl?.let {
                if (it.isNotEmpty()) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(it)
                            .crossfade(true)
                            .build(),
                        contentDescription = "Exercise Image",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            // Video preview and player
            exercise.videoUrl?.let {
                if (it.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showVideo = !showVideo },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.VideoLibrary,
                            contentDescription = "Video",
                            tint = Color(0xFF2196F3)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (showVideo) "Hide Video" else "Show Video",
                            color = Color(0xFF2196F3),
                            fontWeight = FontWeight.Medium
                        )
                    }

                    if (showVideo) {
                        Spacer(modifier = Modifier.height(8.dp))
                        VideoPlayer(url = it)
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }

            // Audio preview and player
            exercise.audioUrl?.let {
                if (it.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showAudio = !showAudio },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.AudioFile,
                            contentDescription = "Audio",
                            tint = Color(0xFF673AB7)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (showAudio) "Hide Audio" else "Show Audio",
                            color = Color(0xFF673AB7),
                            fontWeight = FontWeight.Medium
                        )
                    }

                    if (showAudio) {
                        Spacer(modifier = Modifier.height(8.dp))
                        AudioPlayer(url = it)
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }

            // Kana and Romanji if available
            Row(
                modifier = Modifier.fillMaxWidth()
            ) {
                exercise.kana?.let {
                    if (it.isNotEmpty()) {
                        Column(
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = "Kana:",
                                fontSize = 12.sp,
                                color = Color.Gray
                            )
                            Text(
                                text = it,
                                fontSize = 14.sp
                            )
                        }
                    }
                }

                exercise.romanji?.let {
                    if (it.isNotEmpty()) {
                        Column(
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = "Romanji:",
                                fontSize = 12.sp,
                                color = Color.Gray
                            )
                            Text(
                                text = it,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Answer if available
            exercise.answer?.let {
                if (it.isNotEmpty()) {
                    Text(
                        text = "Answer: $it",
                        fontSize = 14.sp,
                        color = Color.DarkGray
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }
            }

            // Options if available
            exercise.options?.let {
                if (it.isNotEmpty()) {
                    Text(
                        text = "Options:",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.DarkGray
                    )
                    it.forEachIndexed { index, option ->
                        Text(
                            text = "• $option",
                            fontSize = 14.sp,
                            color = Color.DarkGray,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                }
            }

            // Explanation if available
            if(exercise.type == ExerciseType.VIDEO) {
                exercise.explanation?.let { rawExplanation ->
                    if (rawExplanation.isNotEmpty()) {
                        val explanationItems = parseExplanation(rawExplanation)
                        val expandedCardIndex = remember { mutableStateOf(-1) }

                        Column {
                            Text(
                                text = "Explanation:",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color.DarkGray
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            explanationItems.forEachIndexed { index, (title, content) ->
                                val isExpanded = expandedCardIndex.value == index

                                Card(
                                    shape = RoundedCornerShape(16.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 6.dp)
                                        .animateContentSize { initialValue, targetValue -> }
                                        .clickable {
                                            expandedCardIndex.value = if (isExpanded) -1 else index
                                        },
                                    colors = CardDefaults.cardColors(
                                        containerColor = Color(0xFFF1EBF5)
                                    )
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Text(
                                            text = title,
                                            style = MaterialTheme.typography.titleMedium.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF1E293B)
                                            )
                                        )

                                        AnimatedVisibility(visible = isExpanded) {
                                            Column(modifier = Modifier.padding(top = 12.dp)) {
                                                Divider(color = Color(0xFFE2E8F0), thickness = 1.dp)
                                                Spacer(modifier = Modifier.height(12.dp))

                                                Text(
                                                    text = content,
                                                    style = MaterialTheme.typography.bodyMedium.copy(
                                                        lineHeight = 20.sp,
                                                        color = Color(0xFF334155)
                                                    )
                                                )

                                                Spacer(modifier = Modifier.height(12.dp))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            else {
                exercise.explanation?.let {
                    if (it.isNotEmpty()) {
                        Text(
                            text = "Explanation:",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.DarkGray
                        )
                        Text(
                            text = it,
                            fontSize = 14.sp,
                            color = Color.DarkGray
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                }
            }
            // Actions
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.End
            ) {
                IconButton(onClick = onEdit) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = "Edit",
                        tint = Color(0xFF2196F3)
                    )
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = Color(0xFFE53935)
                    )
                }
            }
        }
    }
}
fun parseExplanation(raw: String): List<Pair<String, String>> {
    val parts = raw.split("➤").filter { it.isNotBlank() }
    val result = mutableListOf<Pair<String, String>>()

    var i = 0
    while (i < parts.size - 1) {
        val title = parts[i].trim()
        val content = parts[i + 1].trim()
        result.add(title to content)
        i += 2
    }

    return result
}

// ==== THEME SELECTION ====
enum class DialogTheme(val primary: Color, val secondary: Color, val error: Color) {
    GREEN(Color(0xFF388E3C), Color(0xFF81C784), Color(0xFFD32F2F)),
    BLUE(Color(0xFF1976D2), Color(0xFF64B5F6), Color(0xFFD32F2F)),
    RED(Color(0xFFD32F2F), Color(0xFFEF9A9A), Color(0xFFD32F2F))
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExerciseDialog(
    exercise: Exercise,
    isNew: Boolean,
    onDismiss: () -> Unit,
    onSave: (Exercise) -> Unit
) {

    var selectedTheme by remember { mutableStateOf(DialogTheme.BLUE) }
    val colorScheme = lightColorScheme(
        primary = selectedTheme.primary,
        secondary = selectedTheme.secondary,
        error = selectedTheme.error
    )

    // ==== STATE ====
    var title by remember { mutableStateOf(exercise.title ?: "") }
    var videoUrl by remember { mutableStateOf(exercise.videoUrl ?: "") }
    var videoQuestion by remember { mutableStateOf(exercise.question ?: "") }
    var explanation by remember { mutableStateOf(exercise.explanation ?: "") }
    var practiceQuestion by remember { mutableStateOf(exercise.question ?: "") }
    var practiceAnswer by remember { mutableStateOf(exercise.answer ?: "") }
    var optionsList = remember { exercise.options?.toMutableList() ?: mutableListOf<String>() }
    var optionsState by remember { mutableStateOf(optionsList) }
    var romanji by remember { mutableStateOf(exercise.romanji ?: "") }
    var kana by remember { mutableStateOf(exercise.kana ?: "") }
    var audioUrl by remember { mutableStateOf(exercise.audioUrl ?: "") }
    var imageUrl by remember { mutableStateOf(exercise.imageUrl ?: "") }
    var type by remember { mutableStateOf(exercise.type ?: ExerciseType.PRACTICE) }
    var showVideoPreview by remember { mutableStateOf(false) }
    var showAudioPreview by remember { mutableStateOf(false) }
    val defaultImages = listOf(
        "https://img.freepik.com/premium-vector/man-wearing-hakama-with-crest-thinking-while-scratching-his-face_180401-12331.jpg",
        "https://thumb.ac-illust.com/95/95d92d1467ccf189f05af2b503a3e5a0_t.jpeg",
        "https://drive.google.com/uc?export=view&id=1TWpes3nKYbwSWyUj0uG0v5p-_a_zaVVh"
    )
    var imageInputType by remember { mutableStateOf(0) } // 0: Default, 1: URL, 2: Upload
    var selectedDefaultImageIndex by remember { mutableStateOf(defaultImages.indexOf(exercise.imageUrl).takeIf { it >= 0 } ?: 0) }
    var imageFile: File? by remember { mutableStateOf(null) }
    var videoInputType by remember { mutableStateOf(0) } // 0: URL, 1: Upload
    var videoFile: File? by remember { mutableStateOf(null) }
    var newOption by remember { mutableStateOf("") }
    var hasChanges by remember { mutableStateOf(isNew) }
    val isVideoType = type == ExerciseType.VIDEO
    val isPracticeType = type == ExerciseType.PRACTICE
    val isSaveEnabled = hasChanges && when (type) {
        ExerciseType.VIDEO -> (videoInputType == 0 && videoUrl.isNotBlank() || videoInputType == 1 && videoFile != null) && title.isNotBlank()
        ExerciseType.PRACTICE -> practiceQuestion.isNotBlank() && practiceAnswer.isNotBlank()
        else -> false
    }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isUploading by remember { mutableStateOf(false) }

    // Add these state variables in ExerciseDialog
    var topics by remember { mutableStateOf(listOf<Pair<String, String>>()) }
    var audioInputType by remember { mutableStateOf(0) } // 0: URL, 1: Upload
    var audioFile: File? by remember { mutableStateOf(null) }

    // Initialize topics from explanation if it exists
    LaunchedEffect(exercise.explanation) {
        if (exercise.type == ExerciseType.VIDEO && exercise.explanation != null) {
            topics = parseExplanation(exercise.explanation)
        }
    }

    Log.d("debugExplanation:", explanation)

    MaterialTheme(colorScheme = colorScheme) {
        Dialog(
            onDismissRequest = onDismiss,
            properties = DialogProperties(
                dismissOnBackPress = true,
                dismissOnClickOutside = true,
                usePlatformDefaultWidth = false
            )
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth(0.95f)
                    .fillMaxHeight(0.9f),
                shape = RoundedCornerShape(16.dp),
                tonalElevation = 6.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    // ==== THEME SELECTOR ====
//                    Row(
//                        modifier = Modifier.fillMaxWidth(),
//                        horizontalArrangement = Arrangement.Center
//                    ) {
//                        DialogTheme.values().forEach { theme ->
//                            Button(
//                                onClick = { selectedTheme = theme },
//                                colors = ButtonDefaults.buttonColors(
//                                    containerColor = theme.primary,
//                                    contentColor = Color.White
//                                ),
//                                modifier = Modifier.padding(horizontal = 4.dp)
//                            ) {
//                                Text(theme.name)
//                            }
//                        }
//                    }
                    Spacer(Modifier.height(8.dp))

                    // ==== HEADER ====
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isNew) "Add New Exercise" else "Edit Exercise",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        IconButton(onClick = onDismiss) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close"
                            )
                        }
                    }

                    // ==== TYPE SELECTOR ====
                    if (isNew) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            ExerciseType.values()
                                .filter { it == ExerciseType.PRACTICE || it == ExerciseType.VIDEO }
                                .forEach { exerciseType ->
                                    FilterChip(
                                        selected = type == exerciseType,
                                        onClick = {
                                            type = exerciseType
                                            hasChanges = true
                                        },
                                        label = {
                                            Text(
                                                text = when (exerciseType) {
                                                    ExerciseType.PRACTICE -> "Practice"
                                                    ExerciseType.VIDEO -> "Video"
                                                    else -> exerciseType.name
                                                }
                                            )
                                        }
                                    )
                                }
                        }
                        Spacer(Modifier.height(8.dp))
                    } else {
                        Text(
                            text = "Type: ${type.name}",
                            style = MaterialTheme.typography.titleMedium,
                            color = selectedTheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(8.dp))
                    }

                    // ==== MAIN CONTENT ====
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        if (isVideoType) {
                            item {
                                OutlinedTextField(
                                    value = title,
                                    onValueChange = { title = it; hasChanges = true },
                                    label = { Text("Title *") },
                                    modifier = Modifier.fillMaxWidth(),
                                    isError = title.isBlank(),
                                    supportingText = { if (title.isBlank()) Text("Title is required") }
                                )
                                Spacer(Modifier.height(8.dp))
                                OutlinedTextField(
                                    value = videoQuestion,
                                    onValueChange = { videoQuestion = it; hasChanges = true },
                                    label = { Text("Question (Optional)") },
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(Modifier.height(8.dp))
                                // ==== VIDEO INPUT ====
                                TabRow(selectedTabIndex = videoInputType) {
                                    Tab(selected = videoInputType == 0, onClick = { videoInputType = 0 }, text = { Text("URL") })
                                    Tab(selected = videoInputType == 1, onClick = { videoInputType = 1 }, text = { Text("Upload") })
                                }
                                if (videoInputType == 0) {
                                    OutlinedTextField(
                                        value = videoUrl,
                                        onValueChange = { videoUrl = it; hasChanges = true },
                                        label = { Text("Video URL *") },
                                        modifier = Modifier.fillMaxWidth(),
                                        isError = videoUrl.isBlank(),
                                        trailingIcon = {
                                            IconButton(onClick = { showVideoPreview = !showVideoPreview }) {
                                                Icon(
                                                    imageVector = if (showVideoPreview) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                                    contentDescription = if (showVideoPreview) "Hide preview" else "Show preview"
                                                )
                                            }
                                        }
                                    )
                                    AnimatedVisibility(
                                        visible = showVideoPreview && videoUrl.isNotBlank(),
                                        enter = expandVertically() + fadeIn(),
                                        exit = shrinkVertically() + fadeOut()
                                    ) {
                                        if (videoUrl.isNotBlank()) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(200.dp)
                                                    .padding(top = 8.dp)
                                                    .clip(RoundedCornerShape(8.dp))
                                            ) {
                                                VideoPlayer(videoUrl)
                                            }
                                        }
                                    }
                                } else {
                                    val context = LocalContext.current

                                    val launcher = rememberLauncherForActivityResult(
                                        ActivityResultContracts.GetContent()) { uri: Uri? ->
                                        if (uri != null) {
                                            try {
                                                // Convert Uri to File
                                                val inputStream = context.contentResolver.openInputStream(uri)
                                                val file = File(context.cacheDir, "video.mp4")
                                                file.outputStream().use { outputStream ->
                                                    inputStream?.copyTo(outputStream)
                                                }

                                                videoFile = file


                                            } catch (e: Exception) {
                                                Log.e("FileConversion", "Error: ${e.message}")
                                            }
                                        }
                                    }
                                    Button(
                                        onClick = {
                                            launcher.launch("video/*")
                                        },
                                        modifier = Modifier.fillMaxWidth()
                                    ) { Text(if (videoFile == null) "Upload video from device" else "Uploaded: ${videoFile?.name}") }
                                }
                                Spacer(Modifier.height(8.dp))
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    Text("Topics", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                    Spacer(Modifier.height(8.dp))
                                    
                                    topics.forEachIndexed { index, (title, content) ->
                                        var isExpanded by remember { mutableStateOf(false) }
                                        
                                        Card(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 4.dp),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Column(modifier = Modifier.padding(8.dp)) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    OutlinedTextField(
                                                        value = title,
                                                        onValueChange = { newTitle ->
                                                            topics = topics.toMutableList().apply {
                                                                this[index] = newTitle to content
                                                            }
                                                            hasChanges = true
                                                        },
                                                        label = { Text("Topic Title") },
                                                        modifier = Modifier.weight(1f)
                                                    )
                                                    IconButton(onClick = { isExpanded = !isExpanded }) {
                                                        Icon(
                                                            imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                                            contentDescription = if (isExpanded) "Collapse" else "Expand"
                                                        )
                                                    }
                                                    IconButton(
                                                        onClick = {
                                                            topics = topics.toMutableList().apply { removeAt(index) }
                                                            hasChanges = true
                                                        }
                                                    ) {
                                                        Icon(Icons.Default.Delete, contentDescription = "Delete topic")
                                                    }
                                                }
                                                
                                                AnimatedVisibility(visible = isExpanded) {
                                                    OutlinedTextField(
                                                        value = content,
                                                        onValueChange = { newContent ->
                                                            topics = topics.toMutableList().apply {
                                                                this[index] = title to newContent
                                                            }
                                                            hasChanges = true
                                                        },
                                                        label = { Text("Topic Content") },
                                                        modifier = Modifier.fillMaxWidth(),
                                                        minLines = 3
                                                    )
                                                }
                                            }
                                        }
                                    }
                                    
                                    Button(
                                        onClick = {
                                            topics = topics + ("" to "")
                                            hasChanges = true
                                        },
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = null)
                                        Spacer(Modifier.width(8.dp))
                                        Text("Add Topic")
                                    }
                                }
                            }
                        }
                        if (isPracticeType) {
                            item {
                                OutlinedTextField(
                                    value = practiceQuestion,
                                    onValueChange = { practiceQuestion = it; hasChanges = true },
                                    label = { Text("Question *") },
                                    modifier = Modifier.fillMaxWidth(),
                                    isError = practiceQuestion.isBlank(),
                                    supportingText = { if (practiceQuestion.isBlank()) Text("Question is required") }
                                )
                                Spacer(Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedTextField(
                                        value = romanji,
                                        onValueChange = { romanji = it; hasChanges = true },
                                        label = { Text("Romanji") },
                                        modifier = Modifier.weight(1f)
                                    )
                                    OutlinedTextField(
                                        value = kana,
                                        onValueChange = { kana = it; hasChanges = true },
                                        label = { Text("Kana") },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                Spacer(Modifier.height(8.dp))
                                // ==== OPTIONS ====
                                Text("Multiple Choice Options", fontWeight = FontWeight.Bold)
                                optionsList.forEachIndexed { index, option ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Checkbox(
                                            checked = option == practiceAnswer,
                                            onCheckedChange = { isChecked ->
                                                if (isChecked) {
                                                    practiceAnswer = option
                                                    hasChanges = true
                                                }
                                            }
                                        )
                                        Text(option, modifier = Modifier.weight(1f))
                                        IconButton(
                                            onClick = {
                                                optionsList.removeAt(index)
                                                optionsState = ArrayList(optionsList)
                                                hasChanges = true
                                            }
                                        ) {
                                            Icon(Icons.Default.Delete, contentDescription = "Remove", tint = colorScheme.error)
                                        }
                                    }
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    OutlinedTextField(
                                        value = newOption,
                                        onValueChange = { newOption = it },
                                        label = { Text("New Option") },
                                        modifier = Modifier.weight(1f),
                                        singleLine = true
                                    )
                                    IconButton(
                                        onClick = {
                                            if (newOption.isNotBlank()) {
                                                optionsList.add(newOption)
                                                optionsState = ArrayList(optionsList)
                                                newOption = ""
                                                hasChanges = true
                                            }
                                        }
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = "Add")
                                    }
                                }
                                Spacer(Modifier.height(8.dp))
                                // ==== IMAGE INPUT ====
                                Text("Exercise Image", fontWeight = FontWeight.Bold)
                                TabRow(selectedTabIndex = imageInputType) {
                                    Tab(selected = imageInputType == 0, onClick = { imageInputType = 0 }, text = { Text("Default") })
                                    Tab(selected = imageInputType == 1, onClick = { imageInputType = 1 }, text = { Text("URL") })
                                    Tab(selected = imageInputType == 2, onClick = { imageInputType = 2 }, text = { Text("Upload") })
                                }
                                when (imageInputType) {
                                    0 -> LazyRow(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(100.dp)
                                            .padding(vertical = 8.dp),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        itemsIndexed(defaultImages) { idx, url ->
                                            Box(
                                                modifier = Modifier
                                                    .size(90.dp)
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .border(
                                                        width = 2.dp,
                                                        color = if (selectedDefaultImageIndex == idx) colorScheme.primary else Color.Transparent,
                                                        shape = RoundedCornerShape(8.dp)
                                                    )
                                                    .clickable {
                                                        selectedDefaultImageIndex = idx
                                                        imageUrl = url
                                                        hasChanges = true
                                                    }
                                            ) {
                                                AsyncImage(
                                                    model = url,
                                                    contentDescription = "Default image ${idx + 1}",
                                                    contentScale = ContentScale.Crop,
                                                    modifier = Modifier.fillMaxSize()
                                                )
                                            }
                                        }
                                    }
                                    1 -> OutlinedTextField(
                                        value = imageUrl,
                                        onValueChange = { imageUrl = it; hasChanges = true },
                                        label = { Text("Image URL") },
                                        modifier = Modifier.fillMaxWidth(),
                                        singleLine = true
                                    )
                                    2 ->{
                                        val context = LocalContext.current

                                        // File picker launcher
                                        val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
                                            if (uri != null) {
                                                try {
                                                    // Convert Uri to File
                                                    val inputStream = context.contentResolver.openInputStream(uri)
                                                    val file = File(context.cacheDir, "temp_image.jpg")
                                                    file.outputStream().use { outputStream ->
                                                        inputStream?.copyTo(outputStream)
                                                    }

                                                    // Gán imageFile
                                                    imageFile = file


                                                } catch (e: Exception) {
                                                    Log.e("ImageUploader", "File conversion error: ${e.message}")
                                                }
                                            }
                                        }
                                            Button(
                                            onClick = {
                                                launcher.launch("image/*")
                                            },
                                            modifier = Modifier.fillMaxWidth()
                                        ) { Text(if (imageFile == null) "Upload image from device" else "Uploaded: ${imageFile?.name}") }
                                    }
                                }
                                Spacer(Modifier.height(8.dp))
                                // ==== AUDIO ====
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    Text("Audio", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                    Spacer(Modifier.height(8.dp))
                                    
                                    TabRow(selectedTabIndex = audioInputType) {
                                        Tab(selected = audioInputType == 0, onClick = { audioInputType = 0 }, text = { Text("URL") })
                                        Tab(selected = audioInputType == 1, onClick = { audioInputType = 1 }, text = { Text("Upload") })
                                    }
                                    
                                    when (audioInputType) {
                                        0 -> OutlinedTextField(
                                            value = audioUrl,
                                            onValueChange = { audioUrl = it; hasChanges = true },
                                            label = { Text("Audio URL") },
                                            modifier = Modifier.fillMaxWidth(),
                                            trailingIcon = {
                                                IconButton(onClick = { showAudioPreview = !showAudioPreview }) {
                                                    Icon(
                                                        imageVector = if (showAudioPreview) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                                        contentDescription = if (showAudioPreview) "Hide preview" else "Show preview"
                                                    )
                                                }
                                            }
                                        )
                                        1 -> {
                                            val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
                                                if (uri != null) {
                                                    try {
                                                        val inputStream = context.contentResolver.openInputStream(uri)
                                                        val file = File(context.cacheDir, "audio.mp3")
                                                        file.outputStream().use { outputStream ->
                                                            inputStream?.copyTo(outputStream)
                                                        }
                                                        audioFile = file
                                                    } catch (e: Exception) {
                                                        Log.e("AudioUploader", "File conversion error: ${e.message}")
                                                    }
                                                }
                                            }
                                            Button(
                                                onClick = { launcher.launch("audio/*") },
                                                modifier = Modifier.fillMaxWidth()
                                            ) { 
                                                Text(if (audioFile == null) "Upload audio from device" else "Uploaded: ${audioFile?.name}") 
                                            }
                                        }
                                    }
                                    
                                    AnimatedVisibility(
                                        visible = showAudioPreview && audioUrl.isNotBlank(),
                                        enter = expandVertically() + fadeIn(),
                                        exit = shrinkVertically() + fadeOut()
                                    ) {
                                        if (audioUrl.isNotBlank()) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(top = 8.dp)
                                            ) {
                                                AudioPlayer(audioUrl)
                                            }
                                        }
                                    }
                                }
                                Spacer(Modifier.height(8.dp))
                                // ==== EXPLANATION ====
                                OutlinedTextField(
                                    value = explanation,
                                    onValueChange = { explanation = it; hasChanges = true },
                                    label = { Text("Explanation") },
                                    modifier = Modifier.fillMaxWidth(),
                                    minLines = 3
                                )
                            }
                        }
                    }

                    // ==== ACTION BUTTONS ====
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f)
                        ) { Text("Cancel") }
                        Button(
                            onClick = {
                                scope.launch {
                                    isUploading = true
                                    var finalImageUrl = imageUrl
                                    var finalVideoUrl = videoUrl
                                    var finalAudioUrl = audioUrl
                                    
                                    // IMAGE UPLOAD
                                    if (isPracticeType && imageInputType == 2 && imageFile != null) {
                                        finalImageUrl = CatboxUploader.uploadVideo(imageFile!!) ?: ""
                                    }
                                    
                                    // VIDEO UPLOAD
                                    if (isVideoType && videoInputType == 1 && videoFile != null) {
                                        finalVideoUrl = CatboxUploader.uploadVideo(videoFile!!) ?: ""
                                    }
                                    
                                    // AUDIO UPLOAD
                                    if (isPracticeType && audioInputType == 1 && audioFile != null) {
                                        finalAudioUrl = CatboxUploader.uploadVideo(audioFile!!) ?: ""
                                    }
                                    
                                    // Format explanation based on type
                                    val finalExplanation = when (type) {
                                        ExerciseType.VIDEO -> topics.joinToString("➤") { (title, content) ->
                                            "$title➤$content"
                                        }
                                        ExerciseType.PRACTICE -> explanation
                                        else -> null
                                    }
                                    
                                    val updatedExercise = when (type) {
                                        ExerciseType.VIDEO -> exercise.copy(
                                            type = ExerciseType.VIDEO,
                                            title = title,
                                            videoUrl = finalVideoUrl,
                                            question = videoQuestion,
                                            explanation = finalExplanation,
                                            answer = null,
                                            options = null,
                                            romanji = null,
                                            kana = null,
                                            audioUrl = null,
                                            imageUrl = null
                                        )
                                        ExerciseType.PRACTICE -> exercise.copy(
                                            type = ExerciseType.PRACTICE,
                                            question = practiceQuestion,
                                            answer = practiceAnswer,
                                            options = optionsList,
                                            romanji = romanji.ifBlank { null },
                                            kana = kana.ifBlank { null },
                                            audioUrl = finalAudioUrl.ifBlank { null },
                                            imageUrl = finalImageUrl.ifBlank { null },
                                            title = null,
                                            videoUrl = null,
                                            explanation = finalExplanation
                                        )
                                        else -> exercise
                                    }
                                    isUploading = false
                                    onSave(updatedExercise)
                                }
                            },
                            enabled = isSaveEnabled && !isUploading,
                            modifier = Modifier.weight(1f)
                        ) {
                            if (isUploading) CircularProgressIndicator(modifier = Modifier.size(20.dp))
                            else Text("Save")
                        }
                    }
                }
            }
        }
    }
}


@Composable
fun VideoPlayer(url: String) {
    val context = LocalContext.current
    val exoPlayer = remember {
        SimpleExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(url))
            prepare()
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            exoPlayer.release()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .background(Color.Black)
    ) {
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = exoPlayer
                    useController = true
                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                }
            },
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Composable
fun AudioPlayer(url: String) {
    val context = LocalContext.current
    val exoPlayer = remember {
        SimpleExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(url))
            prepare()
        }
    }

    var isPlaying by remember { mutableStateOf(false) }
    var currentPosition by remember { mutableStateOf(0L) }
    var duration by remember { mutableStateOf(0L) }

    LaunchedEffect(exoPlayer) {
        while (true) {
            delay(1000)
            currentPosition = exoPlayer.currentPosition
            duration = exoPlayer.duration.coerceAtLeast(1L)
        }
    }

    DisposableEffect(Unit) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }

            override fun onPlaybackStateChanged(state: Int) {
                if (state == Player.STATE_READY) {
                    duration = exoPlayer.duration
                }
            }
        }

        exoPlayer.addListener(listener)

        onDispose {
            exoPlayer.removeListener(listener)
            exoPlayer.release()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
    ) {
        // Audio player controls
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            IconButton(onClick = {
                if (isPlaying) {
                    exoPlayer.pause()
                } else {
                    exoPlayer.play()
                }
            }) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (isPlaying) "Pause" else "Play",
                    tint = Color(0xFF673AB7)
                )
            }

            Text(
                text = formatDuration(currentPosition),
                color = Color.DarkGray,
                fontSize = 12.sp,
                modifier = Modifier.width(50.dp)
            )

            Slider(
                value = currentPosition.toFloat(),
                onValueChange = {
                    currentPosition = it.toLong()
                    exoPlayer.seekTo(it.toLong())
                },
                valueRange = 0f..duration.toFloat(),
                modifier = Modifier.weight(1f)
            )

            Text(
                text = formatDuration(duration),
                color = Color.DarkGray,
                fontSize = 12.sp,
                modifier = Modifier.width(50.dp)
            )
        }
    }
}

fun formatDuration(durationMs: Long): String {
    val totalSeconds = durationMs / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%02d:%02d", minutes, seconds)
}