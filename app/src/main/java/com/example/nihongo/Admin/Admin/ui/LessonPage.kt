package com.example.nihongo.Admin.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.filled.Visibility
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.nihongo.Admin.viewmodel.AdminLessonViewModel
import com.example.nihongo.User.data.models.Course
import com.example.nihongo.User.data.models.Lesson
import com.example.nihongo.User.data.models.SubLesson
import kotlinx.coroutines.launch


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LessonPage(
    courseId: String,
    onBack: () -> Unit = {},
    viewModel: AdminLessonViewModel = viewModel()
) {

    BackHandler {
        onBack()
    }

    UIVisibilityController.disableDisplayTopBarAndBottom()

    var showHelloB by remember { mutableStateOf(false) }
    var selectedLessonId by remember { mutableStateOf("") }
    var selectedSubLessonId by remember { mutableStateOf("") }

    AnimatedContent(
        targetState = showHelloB,
        transitionSpec = {
            fadeIn() togetherWith fadeOut()
        },
        label = "contentSwitcher"
    ) { showB ->
        if (showB) {
            ExercisePage(
                selectedLessonId,
                selectedSubLessonId,
                onBack = {
                    showHelloB = false
                    selectedSubLessonId = ""
                }
            )
        } else {
            ContentLessonPage(
                courseId = courseId,
                onBack = onBack,
                viewModel = viewModel,
                onNavigateToExercise = { lessonId, subLessonId ->
                    selectedLessonId = lessonId
                    selectedSubLessonId = subLessonId
                    showHelloB = true
                    UIVisibilityController.disableDisplayTopBarAndBottom()
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContentLessonPage(
    courseId: String,
    onBack: () -> Unit = {},
    viewModel: AdminLessonViewModel = viewModel(),
    onNavigateToExercise: (lessonId: String, subLessonId: String) -> Unit = { _, _ -> }
) {
    val course by viewModel.course.collectAsState()
    val lessons by viewModel.lessons.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val currentEditingLesson by viewModel.currentEditingLesson.collectAsState()

    val coroutineScope = rememberCoroutineScope()

    // States for dialogs
    var showLessonDialog by remember { mutableStateOf(false) }
    var showUnitDialog by remember { mutableStateOf(false) }
    var showSubLessonDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

    // States for editing
    var currentLessonId by remember { mutableStateOf("") }
    var currentUnitIndex by remember { mutableStateOf(-1) }
    var currentSubLessonIndex by remember { mutableStateOf(-1) }
    var isEditMode by remember { mutableStateOf(false) }

    // Expanded states
    val expandedLessons = remember { mutableStateMapOf<String, Boolean>() }
    val expandedUnits = remember { mutableStateMapOf<String, Boolean>() }

    // Temporary states for editing
    var tempLessonTitle by remember { mutableStateOf("") }
    var tempLessonOverview by remember { mutableStateOf("") }
    var tempLessonStep by remember { mutableStateOf(1) }
    var tempUnitTitle by remember { mutableStateOf("") }
    var tempSubLessonTitle by remember { mutableStateOf("") }
    var tempSubLessonType by remember { mutableStateOf("") }

    // Fetch data on initial composition
    LaunchedEffect(courseId) {
        viewModel.fetchCourse(courseId)
        viewModel.fetchLessons(courseId)
    }

    // Error handling
    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            // Handle error message display
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Course Details") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        val newLesson = viewModel.createEmptyLesson(courseId)
                        viewModel.setCurrentEditingLesson(newLesson)
                        tempLessonTitle = newLesson.stepTitle
                        tempLessonOverview = newLesson.overview
                        tempLessonStep = newLesson.step
                        isEditMode = false
                        showLessonDialog = true
                    }) {
                        Icon(Icons.Default.Add, "Add Lesson")
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
            // Loading indicator
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .size(50.dp)
                        .align(Alignment.Center)
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
            ) {
                // Course header
                course?.let { course ->
                    CourseHeader(course)
                }

                // Lessons list
                Text(
                    text = "Lessons",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(16.dp)
                )

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    itemsIndexed(lessons) { _, lesson ->
                        AdminLessonCard(
                            lesson = lesson,
                            isExpanded = expandedLessons[lesson.id] == true,
                            onToggleExpand = {
                                expandedLessons[lesson.id] = !(expandedLessons[lesson.id] ?: false)
                            },
                            expandedUnits = expandedUnits,
                            onEditLesson = {
                                viewModel.setCurrentEditingLesson(lesson)
                                tempLessonTitle = lesson.stepTitle
                                tempLessonOverview = lesson.overview
                                tempLessonStep = lesson.step
                                isEditMode = true
                                showLessonDialog = true
                            },
                            onDeleteLesson = {
                                viewModel.setCurrentEditingLesson(lesson)
                                showDeleteConfirmDialog = true
                            },
                            onAddUnit = {
                                currentLessonId = lesson.id
                                tempUnitTitle = ""
                                isEditMode = false
                                showUnitDialog = true
                            },
                            onEditUnit = { unitIndex ->
                                currentLessonId = lesson.id
                                currentUnitIndex = unitIndex
                                tempUnitTitle = lesson.units[unitIndex].unitTitle
                                isEditMode = true
                                showUnitDialog = true
                            },
                            onDeleteUnit = { unitIndex ->
                                coroutineScope.launch {
                                    viewModel.deleteUnitFromLesson(lesson.id, unitIndex)
                                }
                            },
                            onAddSubLesson = { unitIndex ->
                                currentLessonId = lesson.id
                                currentUnitIndex = unitIndex
                                tempSubLessonTitle = ""
                                tempSubLessonType = "Practice"
                                isEditMode = false
                                showSubLessonDialog = true
                            },
                            onEditSubLesson = { unitIndex, subLessonIndex, subLesson ->
                                currentLessonId = lesson.id
                                currentUnitIndex = unitIndex
                                currentSubLessonIndex = subLessonIndex
                                tempSubLessonTitle = subLesson.title
                                tempSubLessonType = subLesson.type
                                isEditMode = true
                                showSubLessonDialog = true
                            },
                            onDeleteSubLesson = { unitIndex, subLessonIndex ->
                                coroutineScope.launch {
                                    viewModel.deleteSubLesson(lesson.id, unitIndex, subLessonIndex)
                                }
                            },
                            onViewSubLesson = { lessonId, subLessonId ->
                                onNavigateToExercise(lessonId, subLessonId)
                            }
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }

            // Edit Lesson Dialog
            if (showLessonDialog) {
                LessonDialog(
                    title = if (isEditMode) "Edit Lesson" else "Add Lesson",
                    lessonTitle = tempLessonTitle,  // Use the temporary state variable
                    lessonOverview = tempLessonOverview,  // Use the temporary state variable
                    lessonStep = tempLessonStep,
                    onLessonTitleChange = { tempLessonTitle = it },
                    onLessonOverviewChange = { tempLessonOverview = it },
                    onLessonStepChange = { tempLessonStep = it },
                    onConfirm = {
                        coroutineScope.launch {
                            currentEditingLesson?.let { lesson ->
                                val updatedLesson = lesson.copy(
                                    stepTitle = tempLessonTitle,
                                    overview = tempLessonOverview,
                                    step = tempLessonStep
                                )

                                if (isEditMode) {
                                    viewModel.updateLesson(updatedLesson)
                                } else {
                                    viewModel.createLesson(courseId, updatedLesson)
                                }
                            }
                        }
                        showLessonDialog = false
                    },
                    onDismiss = { showLessonDialog = false }
                )
            }

            // Edit Unit Dialog
            if (showUnitDialog) {
                UnitDialog(
                    title = if (isEditMode) "Edit Unit" else "Add Unit",
                    unitTitle = tempUnitTitle,
                    onUnitTitleChange = { tempUnitTitle = it },
                    onConfirm = {
                        coroutineScope.launch {
                            if (isEditMode) {
                                val lesson = lessons.find { it.id == currentLessonId }
                                lesson?.let {
                                    val updatedUnits = it.units.toMutableList()
                                    updatedUnits[currentUnitIndex] = updatedUnits[currentUnitIndex].copy(
                                        unitTitle = tempUnitTitle
                                    )
                                    val updatedLesson = it.copy(units = updatedUnits)
                                    viewModel.updateLesson(updatedLesson)
                                }
                            } else {
                                val newUnit = viewModel.createEmptyUnit().copy(
                                    unitTitle = tempUnitTitle
                                )
                                viewModel.addUnitToLesson(currentLessonId, newUnit)
                            }
                        }
                        showUnitDialog = false
                    },
                    onDismiss = { showUnitDialog = false }
                )
            }

            // Edit SubLesson Dialog
            if (showSubLessonDialog) {
                SubLessonDialog(
                    title = if (isEditMode) "Edit Sub-Lesson" else "Add Sub-Lesson",
                    subLessonTitle = tempSubLessonTitle,
                    subLessonType = tempSubLessonType,
                    onSubLessonTitleChange = { tempSubLessonTitle = it },
                    onSubLessonTypeChange = { tempSubLessonType = it },
                    onConfirm = {
                        coroutineScope.launch {
                            if (isEditMode) {
                                val lesson = lessons.find { it.id == currentLessonId }
                                lesson?.let {
                                    val updatedUnits = it.units.toMutableList()
                                    val subLessons = updatedUnits[currentUnitIndex].subLessons.toMutableList()

                                    subLessons[currentSubLessonIndex] = subLessons[currentSubLessonIndex].copy(
                                        title = tempSubLessonTitle,
                                        type = tempSubLessonType
                                    )

                                    updatedUnits[currentUnitIndex] = updatedUnits[currentUnitIndex].copy(
                                        subLessons = subLessons,
                                        progress = "${subLessons.size}"
                                    )

                                    val updatedLesson = it.copy(units = updatedUnits)
                                    viewModel.updateLesson(updatedLesson)
                                }
                            } else {
                                val newSubLesson = viewModel.createEmptySubLesson().copy(
                                    title = tempSubLessonTitle,
                                    type = tempSubLessonType
                                )
                                viewModel.addSubLessonToUnit(currentLessonId, currentUnitIndex, newSubLesson)
                            }
                        }
                        showSubLessonDialog = false
                    },
                    onDismiss = { showSubLessonDialog = false }
                )
            }

            // Delete confirmation dialog
            if (showDeleteConfirmDialog) {
                DeleteConfirmationDialog(
                    itemType = "Lesson",
                    onConfirm = {
                        coroutineScope.launch {
                            currentEditingLesson?.let { lesson ->
                                viewModel.deleteLesson(lesson)
                            }
                        }
                        showDeleteConfirmDialog = false
                    },
                    onDismiss = { showDeleteConfirmDialog = false }
                )
            }
        }
    }
}

@Composable
fun CourseHeader(course: Course) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
    ) {
        // Course image
        AsyncImage(
            model = course.imageRes,
            contentDescription = "Course Image",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        // Semi-transparent overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.4f))
        )

        // Course info
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Bottom
        ) {
            Text(
                text = course.title,
                style = MaterialTheme.typography.headlineMedium,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.ThumbUp, contentDescription = null, tint = Color(0xFF3390C5))
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "${course.likes}",
                    color = Color.White
                )

                Spacer(modifier = Modifier.width(16.dp))

                Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFFFC107))
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "${course.rating}",
                    color = Color.White
                )

                Spacer(modifier = Modifier.width(16.dp))

                if (course.vip) {
                    AsyncImage(
                        model = "https://i.pinimg.com/originals/22/85/5c/22855c1151bc0b66608bfb21a77e01a0.gif",
                        contentDescription = "VIP",
                        modifier = Modifier
                            .size(24.dp) // chỉnh lại size nếu cần
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "VIP Course",
                        color = Color(0xFFFFD700)
                    )
                }
            }
        }
    }
}

@Composable
fun AdminLessonCard(
    lesson: Lesson,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    expandedUnits: MutableMap<String, Boolean>,
    onEditLesson: () -> Unit,
    onDeleteLesson: () -> Unit,
    onAddUnit: () -> Unit,
    onEditUnit: (Int) -> Unit,
    onDeleteUnit: (Int) -> Unit,
    onAddSubLesson: (Int) -> Unit,
    onEditSubLesson: (Int, Int, SubLesson) -> Unit,
    onDeleteSubLesson: (Int, Int) -> Unit,
    onViewSubLesson: (String, String) -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Lesson header with actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onToggleExpand() }
                ) {
                    Text(
                        text = "${lesson.step}. ${lesson.stepTitle}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "${lesson.totalUnits} units",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row {
                    // Edit button
                    IconButton(onClick = onEditLesson) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit Lesson",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    // Delete button
                    IconButton(onClick = onDeleteLesson) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete Lesson",
                            tint = Color.Red
                        )
                    }

                    // Expand/collapse button
                    IconButton(onClick = onToggleExpand) {
                        Icon(
                            imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = if (isExpanded) "Collapse" else "Expand"
                        )
                    }
                }
            }

            AnimatedVisibility(
                visible = isExpanded,
                enter = fadeIn(animationSpec = tween(200)) + expandVertically(animationSpec = tween(200)),
                exit = fadeOut(animationSpec = tween(200)) + shrinkVertically(animationSpec = tween(200))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                ) {
                    // Lesson overview
                    Text(
                        text = "Overview",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = lesson.overview,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )

                    Divider(modifier = Modifier.padding(vertical = 8.dp))

                    // Units section
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Units",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )

                        // Add unit button
                        Button(
                            onClick = onAddUnit,
                            modifier = Modifier.padding(4.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Add Unit",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Add Unit")
                        }
                        // Continuing from where the code left off...

                    }

                    // Units list
                    lesson.units.forEachIndexed { unitIndex, unit ->
                        val unitKey = "${lesson.id}_unit_$unitIndex"
                        val isUnitExpanded = expandedUnits[unitKey] == true

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp)
                            ) {
                                // Unit header with actions
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable {
                                                expandedUnits[unitKey] = !isUnitExpanded
                                            }
                                    ) {
                                        Text(
                                            text = unit.unitTitle,
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold
                                        )

                                        Spacer(modifier = Modifier.height(4.dp))

                                    }

                                    Row {
                                        // Edit unit button
                                        IconButton(
                                            onClick = { onEditUnit(unitIndex) },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Edit,
                                                contentDescription = "Edit Unit",
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }

                                        // Delete unit button
                                        IconButton(
                                            onClick = { onDeleteUnit(unitIndex) },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = "Delete Unit",
                                                tint = Color.Red,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }

                                        // Expand/collapse unit button
                                        IconButton(
                                            onClick = {
                                                expandedUnits[unitKey] = !isUnitExpanded
                                            },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(
                                                imageVector = if (isUnitExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                                contentDescription = if (isUnitExpanded) "Collapse" else "Expand",
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }

                                AnimatedVisibility(
                                    visible = isUnitExpanded,
                                    enter = fadeIn(animationSpec = tween(200)) + expandVertically(animationSpec = tween(200)),
                                    exit = fadeOut(animationSpec = tween(200)) + shrinkVertically(animationSpec = tween(200))
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(top = 8.dp)
                                    ) {
                                        // Sub-lessons section header
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
//                                            Text(
//                                                text = "Sub-Lessons",
//                                                style = MaterialTheme.typography.bodyMedium,
//                                                fontWeight = FontWeight.Bold
//                                            )

                                            // Add sub-lesson button
                                            Button(
                                                onClick = { onAddSubLesson(unitIndex) },
                                                modifier = Modifier.padding(4.dp),
                                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                                shape = RoundedCornerShape(4.dp),
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                                )
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Add,
                                                    contentDescription = "Add Sub-Lesson",
                                                    modifier = Modifier.size(14.dp)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Add Content", fontSize = 12.sp)
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(8.dp))

                                        // Sub-lessons list
                                        if (unit.subLessons.isEmpty()) {
                                            Text(
                                                text = "No content yet",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.padding(vertical = 8.dp)
                                            )
                                        } else {
                                            unit.subLessons.forEachIndexed { subLessonIndex, subLesson ->
                                                Card(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(vertical = 4.dp),
                                                    colors = CardDefaults.cardColors(
                                                        containerColor = MaterialTheme.colorScheme.surface
                                                    ),
                                                    shape = RoundedCornerShape(4.dp)
                                                ) {
                                                    Row(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .padding(8.dp),
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Column(
                                                            modifier = Modifier
                                                                .weight(1f)
                                                        ) {
                                                            Text(
                                                                text = subLesson.title,
                                                                style = MaterialTheme.typography.bodyMedium
                                                            )

                                                            Text(
                                                                text = "Type: ${subLesson.type}",
                                                                style = MaterialTheme.typography.bodySmall,
                                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                                            )
                                                        }
                                                        // View sub-lesson button
                                                        IconButton(
                                                            onClick = {
                                                                // Gọi callback khi nhấn con mắt
                                                                onViewSubLesson(lesson.id , subLesson.id)  // Thêm cái này vào parameters
                                                            },
                                                            modifier = Modifier.size(28.dp)
                                                        ) {
                                                            Icon(
                                                                imageVector = Icons.Default.Visibility,
                                                                contentDescription = "View Sub-Lesson",
                                                                tint = MaterialTheme.colorScheme.primary,
                                                                modifier = Modifier.size(16.dp)
                                                            )
                                                        }
                                                        // Edit sub-lesson button
                                                        IconButton(
                                                            onClick = { onEditSubLesson(unitIndex, subLessonIndex, subLesson) },
                                                            modifier = Modifier.size(28.dp)
                                                        ) {
                                                            Icon(
                                                                imageVector = Icons.Default.Edit,
                                                                contentDescription = "Edit Sub-Lesson",
                                                                tint = MaterialTheme.colorScheme.primary,
                                                                modifier = Modifier.size(16.dp)
                                                            )
                                                        }

                                                        // Delete sub-lesson button
                                                        IconButton(
                                                            onClick = { onDeleteSubLesson(unitIndex, subLessonIndex) },
                                                            modifier = Modifier.size(28.dp)
                                                        ) {
                                                            Icon(
                                                                imageVector = Icons.Default.Delete,
                                                                contentDescription = "Delete Sub-Lesson",
                                                                tint = Color.Red,
                                                                modifier = Modifier.size(16.dp)
                                                            )
                                                        }

                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LessonDialog(
    title: String,
    lessonTitle: String,
    lessonOverview: String,
    lessonStep: Int,
    onLessonTitleChange: (String) -> Unit,
    onLessonOverviewChange: (String) -> Unit,
    onLessonStepChange: (Int) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // Lesson step
                Text(
                    text = "Step Number",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(4.dp))

                OutlinedTextField(
                    value = lessonStep.toString(),
                    onValueChange = {
                        val step = it.toIntOrNull() ?: 1
                        onLessonStepChange(step)
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Lesson title
                Text(
                    text = "Title",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(4.dp))

                OutlinedTextField(
                    value = lessonTitle,
                    onValueChange = onLessonTitleChange,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    placeholder = { Text("New Lesson...") }
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Lesson overview
                Text(
                    text = "Overview",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(4.dp))

                OutlinedTextField(
                    value = lessonOverview,
                    onValueChange = onLessonOverviewChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    minLines = 3,
                    placeholder = { Text("Lesson overview...") }
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = {
                            // Set default values if fields are empty
                            val finalTitle = if (lessonTitle.isBlank()) "New Lesson" else lessonTitle
                            val finalOverview = if (lessonOverview.isBlank()) "Lesson overview..." else lessonOverview
                            onLessonTitleChange(finalTitle)
                            onLessonOverviewChange(finalOverview)
                            onConfirm()
                        }
                    ) {
                        Text("Save")
                    }
                }
            }
        }
    }
}

@Composable
fun UnitDialog(
    title: String,
    unitTitle: String,
    onUnitTitleChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // Unit title
                Text(
                    text = "Title",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(4.dp))

                OutlinedTextField(
                    value = unitTitle,
                    onValueChange = onUnitTitleChange,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(onClick = onConfirm) {
                        Text("Save")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubLessonDialog(
    title: String,
    subLessonTitle: String,
    subLessonType: String,
    onSubLessonTitleChange: (String) -> Unit,
    onSubLessonTypeChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val subLessonTypes = listOf("Video", "Practice", "Quiz", "Assignment", "Resource")
    var expanded by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // Sub-lesson title
                Text(
                    text = "Title",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(4.dp))

                OutlinedTextField(
                    value = subLessonTitle,
                    onValueChange = onSubLessonTitleChange,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Sub-lesson type dropdown
                Text(
                    text = "Type",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(4.dp))

                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = it },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = subLessonType,
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )

                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        subLessonTypes.forEach { type ->
                            DropdownMenuItem(
                                text = { Text(text = type) },
                                onClick = {
                                    onSubLessonTypeChange(type)
                                    expanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(onClick = onConfirm) {
                        Text("Save")
                    }
                }
            }
        }
    }
}

@Composable
fun DeleteConfirmationDialog(
    itemType: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Delete $itemType")
        },
        text = {
            Text("Are you sure you want to delete this $itemType? This action cannot be undone.")
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Red
                )
            ) {
                Text("Delete")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
