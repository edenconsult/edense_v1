package com.example.ui.screens.setup

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.entity.SubjectEntity
import com.example.domain.schedule.ClassroomAssignments
import com.example.ui.theme.EdenseGreen
import com.example.ui.theme.EdenseNavy
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SetupScreen(
    viewModel: SetupViewModel,
    onFinish: () -> Unit,
    modifier: Modifier = Modifier
) {
    val profile by viewModel.profile.collectAsStateWithLifecycle()
    val calendar by viewModel.calendar.collectAsStateWithLifecycle()
    val academicTerms by viewModel.academicTerms.collectAsStateWithLifecycle()
    val allSubjects by viewModel.allSubjects.collectAsStateWithLifecycle()
    val selectedSubjects by viewModel.selectedSubjects.collectAsStateWithLifecycle()
    val selectedCount by viewModel.selectedCount.collectAsStateWithLifecycle()
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()
    val successMessage by viewModel.successMessage.collectAsStateWithLifecycle()

    var activeTab by remember { mutableIntStateOf(0) }
    var isSigningIn by remember { mutableStateOf(false) }
    var configuringSubject by remember { mutableStateOf<SubjectEntity?>(null) }
    val context = androidx.compose.ui.platform.LocalContext.current
    val coroutineScope = androidx.compose.runtime.rememberCoroutineScope()

    // Dialog configuration state
    var classInput by remember { mutableStateOf("SHS 2B") }
    var roomInput by remember { mutableStateOf("SHS 2B") }
    var roomInputCustomized by remember { mutableStateOf(false) }
    var periodsInput by remember { mutableStateOf("6") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "School & Subject Setup",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Eden Creative Systems • Edense",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    if (selectedCount > 0 && currentUser != null) {
                        Button(
                            onClick = {
                                viewModel.completeOnboarding()
                                onFinish()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = EdenseGreen),
                            modifier = Modifier
                                .padding(end = 8.dp)
                                .testTag("finish_setup_button")
                        ) {
                            Text("Save & Open")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (errorMessage != null) {
                SetupFeedbackBanner(
                    message = errorMessage.orEmpty(),
                    isError = true,
                    onDismiss = { viewModel.clearError() }
                )
            } else if (successMessage != null) {
                SetupFeedbackBanner(
                    message = successMessage.orEmpty(),
                    isError = false,
                    onDismiss = { viewModel.clearError() }
                )
            }

            if (currentUser == null) {
                SetupSignInPrompt(
                    isSigningIn = isSigningIn,
                    onSignIn = {
                        val activity = context as? android.app.Activity
                        if (activity == null) {
                            viewModel.showError(
                                "Google Sign-In could not start because there is no active Android activity."
                            )
                        } else {
                            isSigningIn = true
                            viewModel.authManager.signInWithGoogle(
                                activity = activity,
                                scope = coroutineScope,
                                onSuccess = {
                                    isSigningIn = false
                                    val email = viewModel.authManager.currentUser.value?.email
                                    viewModel.showSuccess(
                                        "Signed in as ${email ?: "your Google account"}."
                                    )
                                    viewModel.onGoogleSignInSuccess()
                                },
                                onError = { error ->
                                    isSigningIn = false
                                    viewModel.showError("Google sign-in failed: $error")
                                },
                                onCancelled = {
                                    isSigningIn = false
                                    viewModel.showError(
                                        "Google sign-in was cancelled. No account was connected."
                                    )
                                }
                            )
                        }
                    }
                )
            } else {
            TabRow(selectedTabIndex = activeTab) {
                Tab(
                    selected = activeTab == 0,
                    onClick = { activeTab = 0 },
                    text = { Text("1. Subjects ($selectedCount/2)") },
                    icon = { Icon(Icons.Default.School, contentDescription = "Subjects") }
                )
                Tab(
                    selected = activeTab == 1,
                    onClick = { activeTab = 1 },
                    text = { Text("2. Academic Year") },
                    icon = { Icon(Icons.Default.DateRange, contentDescription = "Calendar") }
                )
                Tab(
                    selected = activeTab == 2,
                    onClick = { activeTab = 2 },
                    text = { Text("3. Profile & Hours") },
                    icon = { Icon(Icons.Default.Settings, contentDescription = "Profile") }
                )
            }

            when (activeTab) {
                0 -> {
                    SubjectSelectionTab(
                        allSubjects = allSubjects,
                        selectedSubjects = selectedSubjects,
                        selectedCount = selectedCount,
                        onAddCustomSubject = { name, category ->
                            viewModel.addCustomSubject(name, category)
                        },
                        onSelectSubject = { subject ->
                            configuringSubject = subject
                            val classes = ClassroomAssignments.parse(
                                subject.assignedClasses.ifBlank { "SHS 2B" }
                            )
                            classInput = classes.joinToString(", ")
                            val savedRooms = ClassroomAssignments.parse(subject.assignedClassrooms)
                            val rooms = when {
                                savedRooms.size == classes.size -> savedRooms
                                classes.size == 1 && subject.defaultClassroom.isNotBlank() ->
                                    listOf(subject.defaultClassroom)
                                else -> ClassroomAssignments.defaultRooms(classes)
                            }
                            roomInput = rooms.joinToString(", ")
                            roomInputCustomized = rooms != classes
                            periodsInput = if (subject.targetPeriodsPerWeek > 0) subject.targetPeriodsPerWeek.toString() else "6"
                        },
                        onDeselectSubject = { subjectId ->
                            viewModel.deselectSubject(subjectId)
                        }
                    )
                }
                1 -> {
                    AcademicCalendarTab(
                        calendar = calendar,
                        terms = academicTerms,
                        errorMessage = errorMessage,
                        successMessage = successMessage,
                        onSave = { year, system, term, start, end, pauseOnVacation, manualVacation, savedTerms ->
                            viewModel.saveCalendar(
                                year,
                                system,
                                term,
                                start,
                                end,
                                pauseOnVacation,
                                manualVacation,
                                savedTerms
                            )
                        }
                    )
                }
                2 -> {
                    TeacherProfileTab(
                        profile = profile,
                        viewModel = viewModel,
                        errorMessage = errorMessage,
                        successMessage = successMessage,
                        onSave = { name, school, start, end ->
                            viewModel.saveProfile(name, school, start, end)
                        }
                    )
                }
            }
            }
        }
    }

    // Modal to configure chosen subject (Classes, Room, Target Periods)
    if (configuringSubject != null) {
        val subject = configuringSubject!!
        AlertDialog(
            onDismissRequest = { configuringSubject = null },
            title = {
                Text(
                    text = "Configure ${subject.name}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Enter classes separated by commas. Classrooms are paired in the same order and default to each class name. Edit the classroom list only if you use different rooms.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = classInput,
                        onValueChange = {
                            classInput = it
                            if (!roomInputCustomized) {
                                roomInput = ClassroomAssignments.defaultRooms(
                                    ClassroomAssignments.parse(it)
                                ).joinToString(", ")
                            }
                        },
                        label = { Text("Assigned Classes (e.g. 3A9, 3A11, 3HE2, 3HE3)") },
                        modifier = Modifier.fillMaxWidth().testTag("assigned_class_input")
                    )
                    OutlinedTextField(
                        value = roomInput,
                        onValueChange = {
                            roomInput = it
                            roomInputCustomized = true
                        },
                        label = { Text("Classrooms in matching order") },
                        modifier = Modifier.fillMaxWidth().testTag("default_room_input")
                    )
                    val classCount = ClassroomAssignments.parse(classInput).size
                    val roomCount = ClassroomAssignments.parse(roomInput).size
                    Text(
                        text = if (classCount == roomCount) {
                            "Each class has its matching classroom."
                        } else {
                            "Enter one classroom for each class ($classCount classes, $roomCount classrooms)."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = if (classCount == roomCount) {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        } else {
                            MaterialTheme.colorScheme.error
                        }
                    )
                    OutlinedTextField(
                        value = periodsInput,
                        onValueChange = { periodsInput = it.filter { char -> char.isDigit() } },
                        label = { Text("Target Periods Per Week (e.g. 6)") },
                        modifier = Modifier.fillMaxWidth().testTag("target_periods_input")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val quota = periodsInput.toIntOrNull() ?: 6
                        val classes = ClassroomAssignments.parse(classInput)
                        val rooms = ClassroomAssignments.parse(roomInput)
                        if (classes.isEmpty() || classes.size != rooms.size) return@Button
                        viewModel.selectSubject(
                            subjectId = subject.id,
                            assignedClasses = classes.joinToString(", "),
                            defaultClassroom = rooms.first(),
                            assignedClassrooms = rooms.joinToString(", "),
                            targetPeriods = quota
                        )
                        configuringSubject = null
                    },
                    enabled = ClassroomAssignments.parse(classInput).isNotEmpty() &&
                        ClassroomAssignments.parse(classInput).size ==
                        ClassroomAssignments.parse(roomInput).size,
                    colors = ButtonDefaults.buttonColors(containerColor = EdenseNavy),
                    modifier = Modifier.testTag("confirm_subject_button")
                ) {
                    Text("Add Subject")
                }
            },
            dismissButton = {
                TextButton(onClick = { configuringSubject = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun SetupFeedbackBanner(
    message: String,
    isError: Boolean,
    onDismiss: () -> Unit
) {
    val containerColor = if (isError) {
        MaterialTheme.colorScheme.errorContainer
    } else {
        MaterialTheme.colorScheme.secondaryContainer
    }
    val contentColor = if (isError) {
        MaterialTheme.colorScheme.onErrorContainer
    } else {
        MaterialTheme.colorScheme.onSecondaryContainer
    }
    Card(
        colors = CardDefaults.cardColors(containerColor = containerColor),
        modifier = Modifier.fillMaxWidth().padding(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Info,
                contentDescription = if (isError) "Error" else "Status",
                tint = contentColor
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = contentColor,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onDismiss) {
                Icon(Icons.Default.Close, contentDescription = "Dismiss")
            }
        }
    }
}

@Composable
private fun SetupSignInPrompt(
    isSigningIn: Boolean,
    onSignIn: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth().testTag("setup_sign_in_prompt")
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    Icons.Default.School,
                    contentDescription = null,
                    tint = EdenseNavy,
                    modifier = Modifier.size(40.dp)
                )
                Text(
                    text = "Connect your Google account",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = EdenseNavy
                )
                Text(
                    text = "Sign in with Google to access your school setup and keep your data backed up and synchronized when internet is available.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Button(
                    onClick = onSignIn,
                    enabled = !isSigningIn,
                    colors = ButtonDefaults.buttonColors(containerColor = EdenseGreen),
                    modifier = Modifier.fillMaxWidth().testTag("setup_google_sign_in_button")
                ) {
                    if (isSigningIn) {
                        androidx.compose.material3.CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Connecting...")
                    } else {
                        Text("Sign in with Google")
                    }
                }
            }
        }
    }
}

@Composable
fun SubjectSelectionTab(
    allSubjects: List<SubjectEntity>,
    selectedSubjects: List<SubjectEntity>,
    selectedCount: Int,
    onAddCustomSubject: (name: String, category: String) -> Unit,
    onSelectSubject: (SubjectEntity) -> Unit,
    onDeselectSubject: (String) -> Unit
) {
    var categoryFilter by remember { mutableStateOf("ALL") }
    var showAddSubjectDialog by remember { mutableStateOf(false) }

    val filteredList = when (categoryFilter) {
        "CORE" -> allSubjects.filter { it.category == "CORE" }
        "ELECTIVE" -> allSubjects.filter { it.category == "ELECTIVE" }
        else -> allSubjects
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Ghanaian SHS Subjects",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Box(
                            modifier = Modifier
                                .background(
                                    if (selectedCount == 2) EdenseGreen else MaterialTheme.colorScheme.primary,
                                    CircleShape
                                )
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "$selectedCount / 2 Selected",
                                color = Color.White,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "In Ghanaian Senior High Schools, teachers specialize in at most 2 subjects per academic year. Select your assigned subjects below.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                    )
                }
            }
        }

        item {
            OutlinedButton(
                onClick = { showAddSubjectDialog = true },
                modifier = Modifier.fillMaxWidth().testTag("add_custom_subject_button")
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Add a subject not listed")
            }
        }

        // Selected subjects summary chips
        if (selectedSubjects.isNotEmpty()) {
            item {
                Text(
                    text = "Your Assigned Subjects:",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    selectedSubjects.forEach { sub ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(14.dp)
                                        .clip(CircleShape)
                                        .background(Color(android.graphics.Color.parseColor(sub.colorHex)))
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = sub.name,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    val classRoomPairs = ClassroomAssignments.parse(sub.assignedClasses)
                                        .mapIndexed { index, className ->
                                            "$className → ${
                                                ClassroomAssignments.parse(sub.assignedClassrooms)
                                                    .getOrNull(index)
                                                    ?: sub.defaultClassroom.ifBlank { className }
                                            }"
                                        }
                                        .joinToString(", ")
                                    Text(
                                        text = "$classRoomPairs • ${sub.targetPeriodsPerWeek} periods/wk",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                IconButton(
                                    onClick = { onDeselectSubject(sub.id) },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Close,
                                        contentDescription = "Remove",
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Category filter chips
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = categoryFilter == "ALL",
                    onClick = { categoryFilter = "ALL" },
                    label = { Text("All Subjects") }
                )
                FilterChip(
                    selected = categoryFilter == "CORE",
                    onClick = { categoryFilter = "CORE" },
                    label = { Text("Core") }
                )
                FilterChip(
                    selected = categoryFilter == "ELECTIVE",
                    onClick = { categoryFilter = "ELECTIVE" },
                    label = { Text("Elective") }
                )
            }
        }

        items(filteredList, key = { it.id }) { subject ->
            val isSelected = subject.isSelected
            val canSelect = isSelected || selectedCount < 2

            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (isSelected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface
                ),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = canSelect) {
                        if (isSelected) {
                            onDeselectSubject(subject.id)
                        } else {
                            onSelectSubject(subject)
                        }
                    }
                    .testTag("subject_card_${subject.id}")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(Color(android.graphics.Color.parseColor(subject.colorHex)))
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = subject.name,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                        Text(
                            text = "${subject.category} SUBJECT",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (isSelected) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = "Selected",
                            tint = EdenseGreen
                        )
                    } else if (canSelect) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = "Add",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Text(
                            text = "Max 2",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }
        }
    }

    if (showAddSubjectDialog) {
        AddCustomSubjectDialog(
            onDismiss = { showAddSubjectDialog = false },
            onAdd = { name, category ->
                onAddCustomSubject(name, category)
                showAddSubjectDialog = false
            }
        )
    }
}

@Composable
private fun AddCustomSubjectDialog(
    onDismiss: () -> Unit,
    onAdd: (name: String, category: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("ELECTIVE") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add custom subject") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Subject name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("custom_subject_name_input")
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = category == "CORE",
                        onClick = { category = "CORE" },
                        label = { Text("Core") }
                    )
                    FilterChip(
                        selected = category == "ELECTIVE",
                        onClick = { category = "ELECTIVE" },
                        label = { Text("Elective") }
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onAdd(name, category) },
                enabled = name.isNotBlank(),
                modifier = Modifier.testTag("confirm_custom_subject_button")
            ) {
                Text("Add subject")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun AcademicCalendarTab(
    calendar: com.example.data.local.entity.AcademicCalendarEntity?,
    terms: List<com.example.data.local.entity.AcademicTermEntity>,
    errorMessage: String? = null,
    successMessage: String? = null,
    onSave: (year: String, system: String, term: String, start: String, end: String, pauseOnVacation: Boolean, manualVacation: Boolean, terms: List<com.example.data.local.entity.AcademicTermEntity>) -> Unit
) {
    val defaultTerm = remember(calendar) {
        com.example.data.local.entity.AcademicTermEntity(
            id = "active_calendar_term_1",
            name = calendar?.activeTermName ?: "Semester 1",
            startDate = calendar?.termStartDate ?: "2026-09-01",
            endDate = calendar?.termEndDate ?: "2026-12-18",
            orderIndex = 0
        )
    }
    var academicYear by remember(calendar) { mutableStateOf(calendar?.academicYear ?: "2026/2027") }
    var systemType by remember(calendar) { mutableStateOf(calendar?.systemType ?: "SEMESTER") }
    var termDrafts by remember(terms, calendar) {
        mutableStateOf(terms.ifEmpty { listOf(defaultTerm) })
    }
    val firstTerm = termDrafts.firstOrNull() ?: defaultTerm
    val termName = firstTerm.name
    val startDate = firstTerm.startDate
    val endDate = firstTerm.endDate
    var pauseOnVacation by remember(calendar) { mutableStateOf(calendar?.pauseAlarmsDuringVacation ?: true) }
    var manualVacation by remember(calendar) { mutableStateOf(calendar?.isManualVacationMode ?: false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Academic Calendar & Vacation Rules",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Configure term dates so Edense automatically pauses class notifications during holidays and school vacations.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        item {
            OutlinedTextField(
                value = academicYear,
                onValueChange = { academicYear = it },
                label = { Text("Academic Year (e.g. 2026/2027)") },
                modifier = Modifier.fillMaxWidth().testTag("academic_year_input")
            )
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = systemType == "SEMESTER",
                    onClick = {
                        systemType = "SEMESTER"
                        if (termName.startsWith("Term")) {
                            termDrafts = termDrafts.mapIndexed { index, term ->
                                if (index == 0) term.copy(name = "Semester 1") else term
                            }
                        }
                    },
                    label = { Text("Semester System (SHS)") }
                )
                FilterChip(
                    selected = systemType == "TERM",
                    onClick = {
                        systemType = "TERM"
                        if (termName.startsWith("Semester")) {
                            termDrafts = termDrafts.mapIndexed { index, term ->
                                if (index == 0) term.copy(name = "Term 1") else term
                            }
                        }
                    },
                    label = { Text("3-Term / Trimester") }
                )
            }
        }

        items(termDrafts.size) { index ->
            val term = termDrafts[index]
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Term ${index + 1}",
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f)
                        )
                        if (termDrafts.size > 1) {
                            IconButton(onClick = {
                                termDrafts = termDrafts.filterIndexed { termIndex, _ -> termIndex != index }
                                    .mapIndexed { order, item -> item.copy(orderIndex = order) }
                            }) {
                                Icon(Icons.Default.Close, contentDescription = "Remove term")
                            }
                        }
                    }
                    OutlinedTextField(
                        value = term.name,
                        onValueChange = { value ->
                            termDrafts = termDrafts.mapIndexed { termIndex, item ->
                                if (termIndex == index) item.copy(name = value) else item
                            }
                        },
                        label = { Text("Term / Semester Name") },
                        modifier = Modifier.fillMaxWidth().testTag("term_name_input_$index")
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        com.example.ui.components.EdenseDatePickerField(
                            label = "Term Start",
                            dateValue = term.startDate,
                            onDateSelected = { value ->
                                termDrafts = termDrafts.mapIndexed { termIndex, item ->
                                    if (termIndex == index) item.copy(startDate = value) else item
                                }
                            },
                            modifier = Modifier.weight(1f).testTag("term_start_input_$index")
                        )
                        com.example.ui.components.EdenseDatePickerField(
                            label = "Term End",
                            dateValue = term.endDate,
                            onDateSelected = { value ->
                                termDrafts = termDrafts.mapIndexed { termIndex, item ->
                                    if (termIndex == index) item.copy(endDate = value) else item
                                }
                            },
                            modifier = Modifier.weight(1f).testTag("term_end_input_$index")
                        )
                    }
                }
            }
        }

        item {
            OutlinedButton(
                onClick = {
                    termDrafts = termDrafts + com.example.data.local.entity.AcademicTermEntity(
                        id = java.util.UUID.randomUUID().toString(),
                        name = if (systemType == "TERM") "Term ${termDrafts.size + 1}" else "Semester ${termDrafts.size + 1}",
                        startDate = "",
                        endDate = "",
                        orderIndex = termDrafts.size
                    )
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Add Term / Semester")
            }
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Pause Alarms on Vacations",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Automatically stops all upcoming class reminders outside active term dates.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = pauseOnVacation,
                            onCheckedChange = { pauseOnVacation = it },
                            modifier = Modifier.testTag("pause_vacation_switch")
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Force Vacation / Mid-Term Break",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Instantly silences school alerts for impromptu holidays or mid-term breaks.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = manualVacation,
                            onCheckedChange = { manualVacation = it },
                            modifier = Modifier.testTag("manual_vacation_switch")
                        )
                    }
                }
            }
        }

        item {
            Button(
                onClick = {
                    val activeFirst = termDrafts.firstOrNull() ?: defaultTerm
                    onSave(
                        academicYear,
                        systemType,
                        activeFirst.name,
                        activeFirst.startDate,
                        activeFirst.endDate,
                        pauseOnVacation,
                        manualVacation,
                        termDrafts
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = EdenseNavy),
                modifier = Modifier.fillMaxWidth().testTag("save_calendar_button")
            ) {
                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Save Academic Calendar")
            }
        }

        if (successMessage != null || errorMessage != null) {
            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (errorMessage != null) MaterialTheme.colorScheme.errorContainer
                        else EdenseGreen.copy(alpha = 0.15f)
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth().testTag("calendar_feedback_banner")
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            if (errorMessage != null) Icons.Default.Close else Icons.Default.Check,
                            contentDescription = null,
                            tint = if (errorMessage != null) MaterialTheme.colorScheme.error else EdenseGreen,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = (errorMessage ?: successMessage).orEmpty(),
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (errorMessage != null) MaterialTheme.colorScheme.error else EdenseGreen,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TeacherProfileTab(
    profile: com.example.data.local.entity.TeacherProfileEntity?,
    viewModel: SetupViewModel,
    errorMessage: String? = null,
    successMessage: String? = null,
    onSave: (name: String, school: String, start: String, end: String) -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val coroutineScope = androidx.compose.runtime.rememberCoroutineScope()
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val isSyncing by viewModel.isSyncing.collectAsStateWithLifecycle()
    val syncMessage by viewModel.syncMessage.collectAsStateWithLifecycle()
    var isSigningIn by remember { mutableStateOf(false) }
    val exportFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            coroutineScope.launch {
                try {
                    val export = viewModel.createDataExport()
                    withContext(Dispatchers.IO) {
                        val output = context.contentResolver.openOutputStream(uri)
                            ?: throw IOException("The selected location could not be opened.")
                        output.use { it.write(export.toByteArray(Charsets.UTF_8)) }
                    }
                    viewModel.showSuccess("Your Edense data export was saved.")
                } catch (error: Exception) {
                    viewModel.showError(
                        "Couldn't export your data: ${error.localizedMessage ?: "Unknown error"}"
                    )
                }
            }
        }
    }

    var name by remember(profile) { mutableStateOf(profile?.name ?: "Mr. K. Mensah") }
    var schoolName by remember(profile) { mutableStateOf(profile?.schoolName ?: "Achimota Senior High School") }
    var startTime by remember(profile) { mutableStateOf(profile?.schoolStartTime ?: "07:00") }
    var endTime by remember(profile) { mutableStateOf(profile?.schoolEndTime ?: "15:00") }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Teacher Profile & Operating Hours",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Used for contextual greetings and default school bell hours.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        item {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Teacher Name") },
                modifier = Modifier.fillMaxWidth().testTag("teacher_name_input")
            )
        }

        item {
            OutlinedTextField(
                value = schoolName,
                onValueChange = { schoolName = it },
                label = { Text("School Name (e.g. Achimota SHS)") },
                modifier = Modifier.fillMaxWidth().testTag("school_name_input")
            )
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                com.example.ui.components.EdenseTimePickerField(
                    label = "School Starts",
                    timeValue = startTime,
                    onTimeSelected = { startTime = it },
                    modifier = Modifier.weight(1f).testTag("school_start_input")
                )
                com.example.ui.components.EdenseTimePickerField(
                    label = "School Ends",
                    timeValue = endTime,
                    onTimeSelected = { endTime = it },
                    modifier = Modifier.weight(1f).testTag("school_end_input")
                )
            }
        }

        item {
            Button(
                onClick = {
                    onSave(name, schoolName, startTime, endTime)
                },
                colors = ButtonDefaults.buttonColors(containerColor = EdenseNavy),
                modifier = Modifier.fillMaxWidth().testTag("save_profile_button")
            ) {
                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Save Profile")
            }
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth().testTag("data_export_card")
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Back up or export your data",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Your data stays in private app storage. Android device backup can include the Edense database when backup is enabled. Export a JSON copy to a location you trust; it contains your profile, timetable, teaching notes, and tasks.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedButton(
                        onClick = {
                            exportFileLauncher.launch("edense-data-export.json")
                        },
                        modifier = Modifier.fillMaxWidth().testTag("export_data_button")
                    ) {
                        Text("Export Data as JSON")
                    }
                }
            }
        }

        if (successMessage != null || errorMessage != null) {
            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (errorMessage != null) MaterialTheme.colorScheme.errorContainer
                        else EdenseGreen.copy(alpha = 0.15f)
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth().testTag("profile_feedback_banner")
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            if (errorMessage != null) Icons.Default.Close else Icons.Default.Check,
                            contentDescription = null,
                            tint = if (errorMessage != null) MaterialTheme.colorScheme.error else EdenseGreen,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = (errorMessage ?: successMessage).orEmpty(),
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (errorMessage != null) MaterialTheme.colorScheme.error else EdenseGreen,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }

        // Firebase Google Sign-In & Cloud Sync Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth().testTag("cloud_sync_card")
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Firebase Cloud Backup & Sync",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = EdenseNavy
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Edense is 100% offline-first. Connect your Google account to securely back up your timetable, carry-forward notes, and tasks to Cloud Firestore (project edense-99e8c).",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    if (currentUser == null) {
                        Button(
                            enabled = !isSigningIn,
                            onClick = {
                                val activity = context as? android.app.Activity
                                if (activity == null) {
                                    viewModel.showError("Google Sign-In could not start because there is no active Android activity.")
                                } else {
                                    isSigningIn = true
                                    viewModel.authManager.signInWithGoogle(
                                        activity = activity,
                                        scope = coroutineScope,
                                        onSuccess = {
                                            isSigningIn = false
                                            val email = viewModel.authManager.currentUser.value?.email
                                            viewModel.showSuccess(
                                                "Signed in as ${email ?: "your Google account"}."
                                            )
                                            viewModel.syncNow(preserveFeedback = true)
                                        },
                                        onError = { err ->
                                            isSigningIn = false
                                            viewModel.showError("Google sign-in failed: $err")
                                        },
                                        onCancelled = {
                                            isSigningIn = false
                                            viewModel.showError(
                                                "Google sign-in was cancelled. No account was connected."
                                            )
                                        }
                                    )
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = EdenseGreen),
                            modifier = Modifier.fillMaxWidth().testTag("google_sign_in_button")
                        ) {
                            if (isSigningIn) {
                                androidx.compose.material3.CircularProgressIndicator(
                                    color = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Connecting...")
                            } else {
                                Text("Sign in with Google")
                            }
                        }
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Connected Account",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = EdenseGreen
                                )
                                Text(
                                    text = currentUser?.email ?: "Google Account",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            OutlinedButton(
                                onClick = {
                                    viewModel.authManager.signOut(coroutineScope) { }
                                }
                            ) {
                                Text("Sign Out")
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Button(
                            onClick = { viewModel.syncNow() },
                            enabled = !isSyncing,
                            colors = ButtonDefaults.buttonColors(containerColor = EdenseNavy),
                            modifier = Modifier.fillMaxWidth().testTag("sync_now_button")
                        ) {
                            if (isSyncing) {
                                androidx.compose.material3.CircularProgressIndicator(
                                    color = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Syncing...")
                            } else {
                                Text("Sync to Cloud Now")
                            }
                        }

                        if (syncMessage != null) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = syncMessage ?: "",
                                style = MaterialTheme.typography.bodySmall,
                                color = EdenseGreen,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }
    }
}
