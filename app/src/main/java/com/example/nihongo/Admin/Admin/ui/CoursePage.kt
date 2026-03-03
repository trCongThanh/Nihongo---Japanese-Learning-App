package com.example.nihongo.Admin.ui

import android.app.Application
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.with
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.RemoveRedEye
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.filled.Title
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.compose.rememberAsyncImagePainter
import coil.compose.rememberImagePainter
import com.example.nihongo.Admin.viewmodel.AdminCourseViewModel
import com.example.nihongo.User.data.models.Course
import coil.compose.rememberImagePainter
import coil.request.ImageRequest
import com.example.nihongo.Admin.utils.AiCourseGenerate
import com.example.nihongo.Admin.utils.AiCourseGenerate.generateImageUrl
import com.example.nihongo.Admin.utils.CatboxUploader
import com.example.nihongo.Admin.utils.ImgurUploader
import com.example.nihongo.Admin.viewmodel.AdminExerciseViewModel
import com.example.nihongo.Admin.viewmodel.AdminLessonViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID


@OptIn(ExperimentalAnimationApi::class)
@Composable
fun CoursePage(viewModel: AdminCourseViewModel = viewModel()) {

    val adminLessonViewModel: AdminLessonViewModel = viewModel()
    val adminExerciseViewModel: AdminExerciseViewModel = viewModel()

    val courses by viewModel.courses.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    var selectedCourse by remember { mutableStateOf<Course?>(null) }
    var isEditing by remember { mutableStateOf(false) }
    var showAddDialog by remember { mutableStateOf(false) }

    // Bubble wizard
    var showBubble by remember { mutableStateOf(true) }

    // Success/Error states
    var showSuccessOverlay by remember { mutableStateOf(false) }
    var showErrorOverlay by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    // Wizard dialog states
    var showWizardDialog by remember { mutableStateOf(false) }
    var description by remember { mutableStateOf("") }
    var lessonCount by remember { mutableStateOf("") }
    var unitsPerLesson by remember { mutableStateOf("") }
    var exercisesPerLesson by remember { mutableStateOf("") }
    var isGenerating by remember { mutableStateOf(false) }

    // Define green theme colors for the app
    val greenTheme = lightColorScheme(
        primary = Color(0xFF4CAF50),
        secondary = Color(0xFF8BC34A),
        tertiary = Color(0xFFCDDC39),
        background = Color(0xFFF5F5F5),
        surface = Color.White,
        onPrimary = Color.White,
        onSecondary = Color.Black,
        onTertiary = Color.Black,
        onBackground = Color.Black,
        onSurface = Color.Black
    )

    // ───────────────────────────────────────────────
    // Callback Functions
    // ───────────────────────────────────────────────
    val onSuccess: (String) -> Unit = { generatedJson ->
        Log.d("WizardAI", "Generated JSON: $generatedJson")

        showSuccessOverlay = true

        // Parse and save the generated course
        try {
            // TODO: Parse generatedJson and save to database
            // viewModel.saveCourseFromJson(generatedJson)

            // Auto hide after 2 seconds
            CoroutineScope(Dispatchers.Main).launch {
                delay(2000)
                showSuccessOverlay = false
                viewModel.fetchCourses() // Refresh course list
            }
        } catch (e: Exception) {
            Log.e("WizardAI", "Error parsing JSON", e)
            showSuccessOverlay = false
            errorMessage = "Không thể phân tích dữ liệu: ${e.message}"
            showErrorOverlay = true
        }
    }

    val onError: (String) -> Unit = { error ->
        Log.e("WizardAI", "Generation failed: $error")

        errorMessage = error
        showErrorOverlay = true
        isGenerating = false
        showWizardDialog = false

        // Auto hide after 3 seconds
        CoroutineScope(Dispatchers.Main).launch {
            delay(3000)
            showErrorOverlay = false
        }
    }

    MaterialTheme(colorScheme = greenTheme) {
        LaunchedEffect(Unit) {
            viewModel.fetchCourses()
        }

        Box(modifier = Modifier.fillMaxSize()) {
            AnimatedContent(
                targetState = selectedCourse?.takeIf { !isEditing },
                transitionSpec = {
                    if (targetState != null) {
                        slideInHorizontally { it } + fadeIn() with
                                slideOutHorizontally { -it } + fadeOut()
                    } else {
                        slideInHorizontally { -it } + fadeIn() with
                                slideOutHorizontally { it } + fadeOut()
                    }
                },
                label = "CourseContentTransition"
            ) { targetCourse ->
                if (targetCourse != null) {
                    LessonPage(
                        courseId = targetCourse.id,
                        onBack = {
                            showBubble = true
                            selectedCourse = null
                            UIVisibilityController.enableDisplayTopBarAndBottom()
                        }
                    )
                    UIVisibilityController.disableDisplayTopBarAndBottom()
                } else {
                    CourseListContent(
                        courses = courses,
                        isLoading = isLoading,
                        onAddCourse = {
                            selectedCourse = null
                            showAddDialog = true
                        },
                        onViewCourse = {
                            selectedCourse = it
                            isEditing = false
                            showBubble = false
                        },
                        onEditCourse = {
                            selectedCourse = it
                            isEditing = true
                            showAddDialog = true
                        },
                        onDeleteCourse = {
                            Log.d("CoursePage", "Deleting course with ID: ${it.id}")
                            viewModel.deleteCourse(it)
                        },
                        showAddDialog = showAddDialog,
                        courseToEdit = selectedCourse,
                        onDismissDialog = { showAddDialog = false },
                        onSaveDB = { course ->
                            if (selectedCourse == null) {
                                viewModel.addCourse(course)
                            } else {
                                viewModel.updateCourse(course)
                            }
                            viewModel.fetchCourses()
                        },
                        onSaveUI = {
                            showAddDialog = false
                            selectedCourse = null
                            isEditing = false
                        }
                    )
                }
            }

            // ───────────────────────────────────────────────
            // Success Overlay
            // ───────────────────────────────────────────────
            if (showSuccessOverlay) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.5f))
                        .clickable(enabled = false) { },
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .background(Color.White, RoundedCornerShape(16.dp))
                            .padding(32.dp)
                    ) {
                        Icon(
                            Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = Color(0xFFFFD700),
                            modifier = Modifier.size(64.dp)
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            "Wizard đã tạo thành công!",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF4CAF50)
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            "Khóa học đã được thêm vào danh sách",
                            fontSize = 14.sp,
                            color = Color.Gray
                        )
                    }
                }
            }

            // ───────────────────────────────────────────────
            // Error Overlay
            // ───────────────────────────────────────────────
            if (showErrorOverlay) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.5f))
                        .clickable { showErrorOverlay = false },
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .background(Color.White, RoundedCornerShape(16.dp))
                            .padding(32.dp)
                            .widthIn(max = 320.dp)
                    ) {
                        Icon(
                            Icons.Default.Error,
                            contentDescription = null,
                            tint = Color(0xFFD32F2F),
                            modifier = Modifier.size(64.dp)
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            "Có lỗi xảy ra!",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFD32F2F)
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            errorMessage,
                            fontSize = 14.sp,
                            color = Color.Gray,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = { showErrorOverlay = false },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFD32F2F)
                            )
                        ) {
                            Text("Đóng")
                        }
                    }
                }
            }

            // ───────────────────────────────────────────────
            // Wizard Bubble + Dialog
            // ───────────────────────────────────────────────
            if (showBubble) {
                // Floating Wizard Button
                FloatingActionButton(
                    onClick = { showWizardDialog = true },
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(start = 24.dp, bottom = 100.dp),
                    containerColor = Color(0xFF6A5AE0), // tím indigo đẹp
                    contentColor = Color.White,
                    shape = CircleShape,
                    elevation = FloatingActionButtonDefaults.elevation(8.dp)
                ) {
                    Icon(
                        Icons.Default.AutoAwesome,
                        contentDescription = "Wizard",
                        tint = Color.White
                    )
                }

                // Wizard Dialog
                if (showWizardDialog) {
                    AlertDialog(
                        onDismissRequest = { if (!isGenerating) showWizardDialog = false },
                        confirmButton = {},
                        containerColor = Color(0xFFF8F6FF), // tím nhạt pastel
                        title = null,
                        text = {
                            Column(
                                verticalArrangement = Arrangement.spacedBy(18.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    "Nihongo no Wizard",
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF5A4FD8) // tím đậm high-end
                                )

                                Text(
                                    "Generate a full Japanese study course with structured lessons, units, and exercises.",
                                    fontSize = 14.sp,
                                    color = Color(0xFF6E6E6E)
                                )

                                // TextField theme chỉnh border tím xanh
                                val outlineColor = Color(0xFF6A5AE0)

                                OutlinedTextField(
                                    value = description,
                                    onValueChange = { description = it },
                                    label = { Text("Course description") },
                                    maxLines = 3,
                                    modifier = Modifier.fillMaxWidth(),
                                    enabled = !isGenerating,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = outlineColor,
                                        focusedLabelColor = outlineColor
                                    )
                                )

                                OutlinedTextField(
                                    value = lessonCount,
                                    onValueChange = { lessonCount = it.filter { c -> c.isDigit() } },
                                    label = { Text("Lessons") },
                                    modifier = Modifier.fillMaxWidth(),
                                    enabled = !isGenerating,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = outlineColor,
                                        focusedLabelColor = outlineColor
                                    )
                                )

                                OutlinedTextField(
                                    value = unitsPerLesson,
                                    onValueChange = { unitsPerLesson = it.filter { c -> c.isDigit() } },
                                    label = { Text("Units per lesson") },
                                    modifier = Modifier.fillMaxWidth(),
                                    enabled = !isGenerating,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = outlineColor,
                                        focusedLabelColor = outlineColor
                                    )
                                )

                                OutlinedTextField(
                                    value = exercisesPerLesson,
                                    onValueChange = { exercisesPerLesson = it.filter { c -> c.isDigit() } },
                                    label = { Text("Exercises per unit") },
                                    modifier = Modifier.fillMaxWidth(),
                                    enabled = !isGenerating,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = outlineColor,
                                        focusedLabelColor = outlineColor
                                    )
                                )

                                if (isGenerating) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(top = 12.dp)
                                    ) {
                                        CircularProgressIndicator(color = Color(0xFF6A5AE0))
                                        Spacer(Modifier.width(12.dp))
                                        Text("Generating course…", color = Color(0xFF5A4FD8))
                                    }
                                }

                                Row(
                                    horizontalArrangement = Arrangement.End,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    if (!isGenerating) {
                                        TextButton(onClick = { showWizardDialog = false }) {
                                            Text("Cancel", color = Color(0xFF6A5AE0))
                                        }
                                    }

                                    val scope = rememberCoroutineScope()
                                    Button(
                                        onClick = {
                                            // validate...
                                            if (description.isBlank() ||
                                                lessonCount.isBlank() ||
                                                unitsPerLesson.isBlank() ||
                                                exercisesPerLesson.isBlank()
                                            ) {
                                                onError("Vui lòng điền đầy đủ thông tin")
                                                return@Button
                                            }

                                            isGenerating = true

                                            scope.launch {
                                                try {
                                                    val result = AiCourseGenerate.generateCourseContent(
                                                        description = description,
                                                        lessons = lessonCount.toInt(),
                                                        units = unitsPerLesson.toInt(),
                                                        exercises = exercisesPerLesson.toInt(),
                                                        viewModel,
                                                        adminLessonViewModel,
                                                        adminExerciseViewModel,
                                                        onSuccess = onSuccess,
                                                        onError = onError
                                                    )
                                                    Log.d("WizardAI", "AI OUTPUT: $result")
                                                    isGenerating = false
                                                    showWizardDialog = false
                                                } catch (e: Exception) {
                                                    onError("Lỗi không xác định: ${e.message}")
                                                    isGenerating = false
                                                    showWizardDialog = false
                                                }
                                            }
                                        },
                                        enabled = !isGenerating,
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Color(0xFF6A5AE0),
                                            contentColor = Color.White
                                        )
                                    ) {
                                        Text("Generate")
                                    }
                                }
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun CourseListContent(
    courses: List<Course>,
    isLoading: Boolean,
    onAddCourse: () -> Unit,
    onViewCourse: (Course) -> Unit,
    onEditCourse: (Course) -> Unit,
    onDeleteCourse: (Course) -> Unit,
    showAddDialog: Boolean,
    courseToEdit: Course?,
    onDismissDialog: () -> Unit,
    onSaveDB: (Course) -> Unit,
    onSaveUI: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Spacer(modifier = Modifier.height(40.dp))
        Text(
            "📚 Course Manager",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = onAddCourse,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            ),
            shape = RoundedCornerShape(12.dp),
            elevation = ButtonDefaults.buttonElevation(4.dp)
        ) {
            Icon(
                Icons.Default.Add,
                contentDescription = "Add",
                modifier = Modifier.padding(end = 8.dp)
            )
            Text("Add Course", fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            "Danh sách khóa học:",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(8.dp))

        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.CenterHorizontally),
                color = MaterialTheme.colorScheme.primary
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                itemsIndexed(courses) { _, course ->
                    CourseCard(
                        course = course,
                        onView = { onViewCourse(course) },
                        onEdit = { onEditCourse(course) },
                        onDelete = { onDeleteCourse(course) }
                    )
                }

                item { Spacer(modifier = Modifier.height(60.dp)) }
            }
        }

        if (showAddDialog) {
            AddCourseDialog(
                showDialog = true,
                courseToEdit = courseToEdit,
                onDismiss = onDismissDialog,
                onSaveDB = onSaveDB,
                onSaveUI = onSaveUI
            )
        }
    }
}

@Composable
fun CourseCard(
    course: Course,
    onView: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val defaultPainter = rememberAsyncImagePainter("https://drive.google.com/uc?export=view&id=1uyNSW54w4stVjixb9ke_rOFhiGaeekEN")

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp),
            elevation = CardDefaults.cardElevation(6.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                // Background image
                AsyncImage(
                    model = course.imageRes,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(16.dp)),
                    placeholder = defaultPainter,
                    error = defaultPainter,
                    fallback = defaultPainter
                )

                // Overlay with green gradient
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.7f)
                                )
                            )
                        )
                )

                // Main content area
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    Row(modifier = Modifier.weight(1f)) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = course.title,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = course.description,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                color = Color.White.copy(alpha = 0.9f)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Row {
                                Icon(
                                    imageVector = Icons.Default.Star,
                                    contentDescription = "Rating",
                                    tint = Color(0xFFFFD700)
                                )
                                Text(
                                    "${course.rating}",
                                    color = Color.White,
                                    modifier = Modifier.padding(start = 4.dp, end = 12.dp)
                                )

                                Icon(
                                    imageVector = Icons.Default.ThumbUp,
                                    contentDescription = "Likes",
                                    tint = Color(0xFF4CAF50)
                                )
                                Text(
                                    "${course.likes}",
                                    color = Color.White,
                                    modifier = Modifier.padding(start = 4.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        // Action buttons
                        Column(
                            verticalArrangement = Arrangement.Bottom,
                            horizontalAlignment = Alignment.End,
                            modifier = Modifier
                                .fillMaxHeight()
                                .offset(y = (10).dp)
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(15.dp)) {
                                IconButton(
                                    onClick = onView,
                                    modifier = Modifier
                                        .size(36.dp)
                                        .background(
                                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                                            shape = CircleShape
                                        )
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.RemoveRedEye,
                                        contentDescription = "View",
                                        tint = Color.White,
                                        modifier = Modifier.size(15.dp) // 🔽 nhỏ hơn
                                    )
                                }

                                IconButton(
                                    onClick = onEdit,
                                    modifier = Modifier
                                        .size(36.dp)
                                        .background(
                                            color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.8f),
                                            shape = CircleShape
                                        )
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = "Edit",
                                        tint = Color.White,
                                        modifier = Modifier.size(15.dp)
                                    )
                                }

                                IconButton(
                                    onClick = onDelete,
                                    modifier = Modifier
                                        .size(36.dp)
                                        .background(
                                            color = Color.Red.copy(alpha = 0.8f),
                                            shape = CircleShape
                                        )
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Delete",
                                        tint = Color.White,
                                        modifier = Modifier.size(15.dp)
                                    )
                                }
                            }

                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                }

                // VIP badge
                if (course.vip) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Box(
                            modifier = Modifier
                                .background(
                                    Brush.linearGradient(
                                        colors = listOf(Color(0xFFFFD700), Color(0xFFFFA500)),
                                        start = Offset(0f, 0f),
                                        end = Offset(100f, 100f)
                                    ),
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .padding(horizontal = 10.dp, vertical = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "VIP",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Black
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AddCourseDialog(
    showDialog: Boolean,
    courseToEdit: Course?,
    onDismiss: () -> Unit,
    onSaveDB: (Course) -> Unit,
    onSaveUI: () -> Unit
) {
    val context = LocalContext.current
    if (!showDialog) return

    val id = remember { mutableStateOf(courseToEdit?.id ?: "") }
    val title = remember { mutableStateOf(courseToEdit?.title ?: "") }
    val description = remember { mutableStateOf(courseToEdit?.description ?: "") }
    val rating = remember { mutableStateOf(courseToEdit?.rating ?: 0.0) }
    val reviews = remember { mutableStateOf(courseToEdit?.reviews ?: 0) }
    val likes = remember { mutableStateOf(courseToEdit?.likes ?: 0) }
    val defaultImage = "https://drive.google.com/uc?export=view&id=1uyNSW54w4stVjixb9ke_rOFhiGaeekEN"
    val imageRes = remember { mutableStateOf(courseToEdit?.imageRes ?: defaultImage) }
    val isVip = remember { mutableStateOf(courseToEdit?.vip ?: false) }

    // State for image source selection
    val imageSource = remember { mutableStateOf(if (courseToEdit?.imageRes == defaultImage) "default" else "url") }
    val selectedImageUri = remember { mutableStateOf<Uri?>(null) }
    val coroutineScope = rememberCoroutineScope()

    // State for AI image generation
    val isGeneratingImage = remember { mutableStateOf(false) }

    // Image picker launcher
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedImageUri.value = uri
            imageSource.value = "device"
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnClickOutside = false,
            dismissOnBackPress = true
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight(),
            shape = RoundedCornerShape(24.dp),
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header
                Text(
                    text = if (courseToEdit == null) "Add New Course" else "Edit Course",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // Title
                OutlinedTextField(
                    value = title.value,
                    onValueChange = { title.value = it },
                    label = { Text("Title") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                    ),
                    leadingIcon = {
                        Icon(
                            Icons.Default.Title,
                            contentDescription = "Title",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                )

                // Description
                OutlinedTextField(
                    value = description.value,
                    onValueChange = { description.value = it },
                    label = { Text("Description") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                        .heightIn(min = 100.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                    ),
                    leadingIcon = {
                        Icon(
                            Icons.Default.Description,
                            contentDescription = "Description",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    maxLines = 5
                )

                // Image Source Selection
                Text(
                    "Image Source",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SelectableButton(
                        text = "Default",
                        isSelected = imageSource.value == "default",
                        onClick = {
                            imageSource.value = "default"
                            imageRes.value = defaultImage
                        },
                        modifier = Modifier.weight(1f),
                        enabled = !isGeneratingImage.value
                    )

                    SelectableButton(
                        text = "URL",
                        isSelected = imageSource.value == "url",
                        onClick = { imageSource.value = "url" },
                        modifier = Modifier.weight(1f),
                        enabled = !isGeneratingImage.value
                    )

                    SelectableButton(
                        text = "Device",
                        isSelected = imageSource.value == "device",
                        onClick = {
                            imageSource.value = "device"
                            imagePickerLauncher.launch("image/*")
                        },
                        modifier = Modifier.weight(1f),
                        enabled = !isGeneratingImage.value
                    )
                }

                // Generate Image Button (only show when Default is selected)
                AnimatedVisibility(visible = imageSource.value == "default") {
                    Button(
                        onClick = {
                            if (title.value.isNotBlank()) {
                                isGeneratingImage.value = true
                                coroutineScope.launch {
                                    isGeneratingImage.value = true
                                    try {
                                        if (title.value.isBlank()) {
                                            Toast.makeText(context, "Please enter a title first", Toast.LENGTH_SHORT).show()
                                            return@launch
                                        }

                                        // Gọi hàm generateImageUrl (Bison001)
                                        val uploadedUrl = generateImageUrl(title.value)

                                        if (!uploadedUrl.isNullOrBlank()) {
                                            imageRes.value = uploadedUrl
                                        } else {
                                            Toast.makeText(context, "Failed to generate image", Toast.LENGTH_SHORT).show()
                                        }
                                    } catch (e: Exception) {
                                        Log.e("ImageGeneration", "Error generating image: ${e.message}", e)
                                        Toast.makeText(context, "Error generating image: ${e.message}", Toast.LENGTH_SHORT).show()
                                    } finally {
                                        isGeneratingImage.value = false
                                    }
                                }
                            } else {
                                Toast.makeText(
                                    context,
                                    "Please enter a title first",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        shape = RoundedCornerShape(12.dp),
                        enabled = !isGeneratingImage.value && title.value.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondary
                        )
                    ) {
                        if (isGeneratingImage.value) {
                            CircularProgressIndicator(
                                modifier = Modifier
                                    .size(20.dp)
                                    .padding(end = 8.dp),
                                color = MaterialTheme.colorScheme.onSecondary,
                                strokeWidth = 2.dp
                            )
                            Text("Generating...")
                        } else {
                            Icon(
                                Icons.Default.AutoAwesome,
                                contentDescription = "Generate",
                                modifier = Modifier.padding(end = 8.dp)
                            )
                            Text("Generate AI Image", fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // URL input field (only show if URL source is selected)
                AnimatedVisibility(visible = imageSource.value == "url") {
                    OutlinedTextField(
                        value = if (imageSource.value == "url") imageRes.value else "",
                        onValueChange = { imageRes.value = it },
                        label = { Text("Image URL") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                        ),
                        leadingIcon = {
                            Icon(
                                Icons.Default.Link,
                                contentDescription = "URL",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    )
                }

                // Image Preview
                Text(
                    "Image Preview",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(12.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    when (imageSource.value) {
                        "default", "url" -> {
                            AsyncImage(
                                model = imageRes.value,
                                contentDescription = "Course Image",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )

                            // Show loading overlay when generating
                            if (isGeneratingImage.value) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(Color.Black.copy(alpha = 0.5f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                        "device" -> {
                            if (selectedImageUri.value != null) {
                                Image(
                                    painter = rememberAsyncImagePainter(selectedImageUri.value),
                                    contentDescription = "Selected Image",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Icon(
                                    Icons.Default.Image,
                                    contentDescription = "No Image",
                                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                                    modifier = Modifier.size(64.dp)
                                )
                            }
                        }
                    }
                }

                // VIP status toggle with switch
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = "VIP",
                        tint = if (isVip.value) Color(0xFFFFD700) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        modifier = Modifier.padding(end = 8.dp)
                    )

                    Text(
                        "VIP Course",
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.weight(1f)
                    )

                    Switch(
                        checked = isVip.value,
                        onCheckedChange = { isVip.value = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = MaterialTheme.colorScheme.primary,
                            checkedTrackColor = MaterialTheme.colorScheme.primaryContainer,
                            uncheckedThumbColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    )
                }

                // Action buttons
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                        enabled = !isGeneratingImage.value
                    ) {
                        Text("Cancel")
                    }

                    Button(
                        onClick = {
                            coroutineScope.launch {
                                // Handle image upload if from device
                                var finalImageUrl = when (imageSource.value) {
                                    "default" -> imageRes.value // Use current imageRes (could be generated or default)
                                    "url" -> imageRes.value
                                    "device" -> {
                                        // Only upload if there's a selected image
                                        if (selectedImageUri.value != null) {
                                            try {
                                                // Convert Uri to File
                                                val inputStream = context.contentResolver.openInputStream(selectedImageUri.value!!)
                                                val file = File(context.cacheDir, "temp_image.jpg")
                                                file.outputStream().use { outputStream ->
                                                    inputStream?.copyTo(outputStream)
                                                }

                                                // Upload to Catbox
                                                val link = CatboxUploader.uploadVideo(file!!)
                                                link ?: defaultImage
                                            } catch (e: Exception) {
                                                Log.e("CatboxUpload", "Error: ${e.message}")
                                                defaultImage
                                            }
                                        } else {
                                            defaultImage
                                        }
                                    }
                                    else -> defaultImage
                                }

                                // Create course object with the image URL
                                val course = Course(
                                    id = id.value,
                                    title = title.value,
                                    description = description.value,
                                    rating = rating.value,
                                    reviews = reviews.value,
                                    likes = likes.value,
                                    imageRes = finalImageUrl,
                                    vip = isVip.value
                                )

                                // Save to database
                                onSaveDB(course)
                                onSaveUI()
                            }
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        ),
                        enabled = !isGeneratingImage.value
                    ) {
                        Icon(
                            Icons.Default.Save,
                            contentDescription = "Save",
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Text("Save", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}


@Composable
fun SelectableButton(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            },
            contentColor = if (isSelected) {
                MaterialTheme.colorScheme.onPrimary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Text(text)
    }
}

@Composable
fun SelectableButton(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
            contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary
        ),
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.primary
        ),
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = if (isSelected) 4.dp else 0.dp
        )
    ) {
        Text(text, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal, fontSize = 10.sp)
    }
}






