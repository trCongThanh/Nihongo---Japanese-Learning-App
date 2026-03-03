package com.example.nihongo.Admin.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SentimentDissatisfied
import androidx.compose.material.icons.filled.TextFormat
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.nihongo.Admin.viewmodel.AdminFlashcardViewModel
import com.example.nihongo.User.data.models.Flashcard
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FlashcardPage(
    viewModel: AdminFlashcardViewModel = viewModel(),
) {
    val flashcards by viewModel.flashcards.collectAsState()
    val selectedFlashcard by viewModel.selectedFlashcard.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val currentExerciseFilter by viewModel.currentExerciseFilter.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var flashcardToDelete by remember { mutableStateOf<String?>(null) }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // Load flashcards on initial composition
    LaunchedEffect(Unit) {
        viewModel.loadFlashcards()
    }

    // Show snackbar for error messages
    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            scope.launch {
                snackbarHostState.showSnackbar(
                    message = it,
                    duration = SnackbarDuration.Short
                )
                delay(2000)
                viewModel.clearError()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Flashcard Management",
                        style = MaterialTheme.typography.titleLarge,
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF4CAF50),
                    titleContentColor = Color.White
                ),
                actions = {
                    IconButton(
                        onClick = { showAddDialog = true }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add Flashcard",
                            tint = Color.White,
                        )
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            // Search and Filter Section
            SearchAndFilterSection(
                searchQuery = searchQuery,
                currentFilter = currentExerciseFilter,
                onSearchQueryChange = viewModel::setSearchQuery,
                onFilterChange = viewModel::setExerciseFilter,
                exerciseTypes = viewModel.exerciseTypes
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Flashcards List with Loading State
            Box(modifier = Modifier.fillMaxSize()) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = Color(0xFF4CAF50)
                    )
                } else if (flashcards.isEmpty()) {
                    EmptyFlashcardView(
                        onAddClick = { showAddDialog = true },
                        modifier = Modifier.align(Alignment.Center)
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(bottom = 80.dp)
                    ) {
                        items(flashcards) { flashcard ->
                            FlashcardItem(
                                flashcard = flashcard,
                                isSelected = selectedFlashcard?.id == flashcard.id,
                                onEdit = { viewModel.selectFlashcard(flashcard) },
                                onDelete = {
                                    flashcardToDelete = flashcard.id
                                    showDeleteConfirmDialog = true
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    // Add/Edit Dialog
    if (showAddDialog || selectedFlashcard != null) {
        FlashcardFormDialog(
            flashcard = selectedFlashcard,
            onDismiss = {
                showAddDialog = false
                viewModel.clearSelectedFlashcard()
            },
            onSave = { id, exerciseId, term, definition, example, pronunciation, audioUrl, imageUrl ->
                if (id.isNotEmpty()) {
                    // Update existing
                    viewModel.updateFlashcard(
                        id, exerciseId, term, definition, example, pronunciation, audioUrl, imageUrl
                    )
                } else {
                    // Create new
                    viewModel.createFlashcard(
                        exerciseId, term, definition, example, pronunciation, audioUrl, imageUrl
                    )
                }
                showAddDialog = false
                viewModel.clearSelectedFlashcard()
            }
        )
    }

    // Delete Confirmation Dialog
    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = { Text("Confirm Deletion") },
            text = { Text("Are you sure you want to delete this flashcard? This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        flashcardToDelete?.let { viewModel.deleteFlashcard(it) }
                        flashcardToDelete = null
                        showDeleteConfirmDialog = false
                    }
                ) {
                    Text("Delete", color = Color.Red)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchAndFilterSection(
    searchQuery: String,
    currentFilter: String?,
    onSearchQueryChange: (String) -> Unit,
    onFilterChange: (String?) -> Unit,
    exerciseTypes: List<String>,
) {
    var expanded by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current
    val focusRequester = remember { FocusRequester() }

    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        // Search bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester),
            placeholder = { Text("Search flashcards...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { onSearchQueryChange("") }) {
                        Icon(Icons.Default.Close, contentDescription = "Clear search")
                    }
                }
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                imeAction = ImeAction.Search
            ),
            keyboardActions = KeyboardActions(
                onSearch = { focusManager.clearFocus() }
            ),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFF4CAF50),
                cursorColor = Color(0xFF4CAF50)
            )
        )
        Spacer(modifier = Modifier.height(8.dp))

        // Filter dropdown
        Box(
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = currentFilter?.let { filterIdToName(it) } ?: "All Flashcards",
                onValueChange = {},
                modifier = Modifier.fillMaxWidth(),
                readOnly = true,
                leadingIcon = { Icon(Icons.Default.FilterList, contentDescription = "Filter") },
                trailingIcon = {
                    IconButton(onClick = { expanded = true }) {
                        Icon(Icons.Default.ArrowDropDown, contentDescription = "Show filters")
                    }
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF4CAF50)
                )
            )

            // Make the entire field clickable to open dropdown
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clickable { expanded = true }
            )

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.width(300.dp)
            ) {
                // All flashcards option
                DropdownMenuItem(
                    text = { Text("All Flashcards") },
                    onClick = {
                        onFilterChange(null)
                        expanded = false
                    },
                    leadingIcon = {
                        Icon(
                            Icons.Default.ViewList,
                            contentDescription = null
                        )
                    }
                )

                Divider()

                // Exercise type options
                exerciseTypes.forEach { exerciseId ->
                    DropdownMenuItem(
                        text = { Text(filterIdToName(exerciseId)) },
                        onClick = {
                            onFilterChange(exerciseId)
                            expanded = false
                        },
                        leadingIcon = {
                            val icon = when (exerciseId) {
                                "hiragana_basic" -> Icons.Default.TextFormat
                                "katakana_basic" -> Icons.Default.TextFormat
                                "kanji_n5" -> Icons.Default.Create
                                "vocabulary_n5" -> Icons.Default.Book
                                else -> Icons.Default.List
                            }
                            Icon(icon, contentDescription = null)
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun FlashcardItem(
    flashcard: Flashcard,
    isSelected: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    val backgroundColor = if (isSelected) {
        Color(0xFFECF9EC)
    } else {
        MaterialTheme.colorScheme.surface
    }

    val borderColor = if (isSelected) {
        Color(0xFF4CAF50)
    } else {
        Color(0xFFDDDDDD)
    }

    val elevation by animateFloatAsState(
        targetValue = if (isSelected) 4f else 1f
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(elevation.dp)
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(8.dp)
            )
            .clickable { onEdit() },
        colors = CardDefaults.cardColors(
            containerColor = backgroundColor
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Exercise Type Badge
                Badge(
                    containerColor = getExerciseColor(flashcard.exerciseId),
                    contentColor = Color.White
                ) {
                    Text(
                        text = filterIdToName(flashcard.exerciseId),
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelMedium
                    )
                }

                // Action buttons
                Row {
                    IconButton(onClick = onEdit) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit",
                            tint = Color(0xFF4CAF50)
                        )
                    }
                    IconButton(onClick = onDelete) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = Color.Red
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Term
            Text(
                text = flashcard.term,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Definition
            Text(
                text = flashcard.definition,
                style = MaterialTheme.typography.bodyLarge
            )

            // Optional fields
            flashcard.pronunciation?.let {
                if (it.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Pronunciation: $it",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray
                    )
                }
            }

            flashcard.example?.let {
                if (it.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Example: $it",
                        style = MaterialTheme.typography.bodyMedium,
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                    )
                }
            }

            // Media indicators
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.End
            ) {
                if (!flashcard.audioUrl.isNullOrEmpty()) {
                    Icon(
                        imageVector = Icons.Default.Audiotrack,
                        contentDescription = "Has audio",
                        tint = Color(0xFF2196F3),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }

                if (!flashcard.imageUrl.isNullOrEmpty()) {
                    Icon(
                        imageVector = Icons.Default.Image,
                        contentDescription = "Has image",
                        tint = Color(0xFFFF9800),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FlashcardFormDialog(
    flashcard: Flashcard?,
    onDismiss: () -> Unit,
    onSave: (
        id: String, exerciseId: String, term: String, definition: String,
        example: String?, pronunciation: String?, audioUrl: String?, imageUrl: String?,
    ) -> Unit,
) {
    val isEditing = flashcard != null
    val title = if (isEditing) "Edit Flashcard" else "Add New Flashcard"

    // Form state
    var term by remember { mutableStateOf(flashcard?.term ?: "") }
    var definition by remember { mutableStateOf(flashcard?.definition ?: "") }
    var exerciseId by remember { mutableStateOf(flashcard?.exerciseId ?: "vocabulary_n5") }
    var example by remember { mutableStateOf(flashcard?.example ?: "") }
    var pronunciation by remember { mutableStateOf(flashcard?.pronunciation ?: "") }
    var audioUrl by remember { mutableStateOf(flashcard?.audioUrl ?: "") }
    var imageUrl by remember { mutableStateOf(flashcard?.imageUrl ?: "") }

    // Validation state
    var termError by remember { mutableStateOf(false) }
    var definitionError by remember { mutableStateOf(false) }

    // Exercise type dropdown state
    var showExerciseDropdown by remember { mutableStateOf(false) }

    // Handle validation
    fun validate(): Boolean {
        termError = term.isBlank()
        definitionError = definition.isBlank()
        return !termError && !definitionError
    }

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
                // Title
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Form fields
                // Exercise Type Selection
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = filterIdToName(exerciseId),
                        onValueChange = {},
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Flashcard Type") },
                        trailingIcon = {
                            IconButton(onClick = { showExerciseDropdown = true }) {
                                Icon(Icons.Default.ArrowDropDown, contentDescription = "Select type")
                            }
                        },
                        readOnly = true
                    )

                    // Clickable overlay
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .clickable { showExerciseDropdown = true }
                    )

                    DropdownMenu(
                        expanded = showExerciseDropdown,
                        onDismissRequest = { showExerciseDropdown = false },
                        modifier = Modifier.width(300.dp)
                    ) {
                        listOf(
                            "hiragana_basic" to "Hiragana",
                            "katakana_basic" to "Katakana",
                            "kanji_n5" to "Kanji",
                            "vocabulary_n5" to "Vocabulary"
                        ).forEach { (id, name) ->
                            DropdownMenuItem(
                                text = { Text(name) },
                                onClick = {
                                    exerciseId = id
                                    showExerciseDropdown = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Term
                OutlinedTextField(
                    value = term,
                    onValueChange = {
                        term = it
                        if (it.isNotEmpty()) termError = false
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Term") },
                    isError = termError,
                    supportingText = {
                        if (termError) {
                            Text("Term is required", color = MaterialTheme.colorScheme.error)
                        }
                    }
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Definition
                OutlinedTextField(
                    value = definition,
                    onValueChange = {
                        definition = it
                        if (it.isNotEmpty()) definitionError = false
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Definition") },
                    isError = definitionError,
                    supportingText = {
                        if (definitionError) {
                            Text("Definition is required", color = MaterialTheme.colorScheme.error)
                        }
                    }
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Pronunciation (optional)
                OutlinedTextField(
                    value = pronunciation,
                    onValueChange = { pronunciation = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Pronunciation (optional)") },
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Example (optional)
                OutlinedTextField(
                    value = example,
                    onValueChange = { example = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Example (optional)") },
                    minLines = 2,
                    maxLines = 3
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Audio URL (optional)
                OutlinedTextField(
                    value = audioUrl,
                    onValueChange = { audioUrl = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Audio URL (optional)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri)
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Image URL (optional)
                OutlinedTextField(
                    value = imageUrl,
                    onValueChange = { imageUrl = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Image URL (optional)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri)
                )

                Spacer(modifier = Modifier.height(16.dp))

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
                            if (validate()) {
                                onSave(
                                    flashcard?.id ?: "",
                                    exerciseId,
                                    term,
                                    definition,
                                    example.ifBlank { null },
                                    pronunciation.ifBlank { null },
                                    audioUrl.ifBlank { null },
                                    imageUrl.ifBlank { null }
                                )
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF4CAF50)
                        )
                    ) {
                        Text(if (isEditing) "Update" else "Create")
                    }
                }
            }
        }
    }
}
// Add these functions to complete the FlashcardPage implementation

@Composable
fun EmptyFlashcardView(
    onAddClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.SentimentDissatisfied,
            contentDescription = "No flashcards",
            modifier = Modifier
                .size(80.dp)
                .padding(bottom = 16.dp),
            tint = Color.Gray
        )

        Text(
            text = "No flashcards found",
            style = MaterialTheme.typography.titleLarge,
            color = Color.Gray,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Try changing your search criteria or add a new flashcard",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.Gray,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onAddClick,
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF4CAF50)
            )
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Add Flashcard")
        }
    }
}

/**
 * Converts exercise ID to a human-readable name
 */
fun filterIdToName(exerciseId: String): String {
    return when (exerciseId) {
        "hiragana_basic" -> "Hiragana"
        "katakana_basic" -> "Katakana"
        "kanji_n5" -> "Kanji"
        "vocabulary_n5" -> "Vocabulary"
        else -> exerciseId.replace("_", " ").capitalizeWords()
    }
}

/**
 * Returns appropriate color for each exercise type
 */
fun getExerciseColor(exerciseId: String): Color {
    return when (exerciseId) {
        "hiragana_basic" -> Color(0xFF2196F3) // Blue
        "katakana_basic" -> Color(0xFF9C27B0) // Purple
        "kanji_n5" -> Color(0xFFC90000) // Deep Orange
        "vocabulary_n5" -> Color(0xFF4CAF50) // Green
        else -> Color(0xFF607D8B) // Blue Grey
    }
}

/**
 * Extension function to capitalize first letter of each word
 */
fun String.capitalizeWords(): String {
    return this.split(" ").joinToString(" ") { word ->
        word.replaceFirstChar { char ->
            if (char.isLowerCase()) char.titlecase() else char.toString()
        }
    }
}