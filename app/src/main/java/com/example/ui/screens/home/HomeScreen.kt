package com.example.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EventNote
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.entity.SubjectEntity
import com.example.data.local.entity.PeriodEntity
import com.example.data.alarm.LessonLogPrompt
import com.example.domain.schedule.lessonEndTimeMillis
import com.example.domain.schedule.isLessonPeriodPast
import com.example.domain.schedule.sessionDateDayOfWeek
import com.example.domain.schedule.periodsForSessionDate
import com.example.domain.schedule.ClassBlock
import com.example.domain.schedule.ClassroomAssignments
import com.example.domain.schedule.DayScheduleStatus
import com.example.domain.home.timeOfDayGreeting
import com.example.domain.task.TaskType
import com.example.domain.task.taskUrgency
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onNavigateToTimetable: () -> Unit,
    onNavigateToSessions: () -> Unit,
    lessonPrompt: LessonLogPrompt? = null,
    onLessonPromptHandled: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val currentTime by viewModel.currentTimeStr.collectAsStateWithLifecycle()
    val dateDisplay by viewModel.currentDateFormatted.collectAsStateWithLifecycle()
    val currentDate by viewModel.currentDateStr.collectAsStateWithLifecycle()
    val profile by viewModel.teacherProfile.collectAsStateWithLifecycle()
    val calendar by viewModel.academicCalendar.collectAsStateWithLifecycle()
    val selectedSubjects by viewModel.selectedSubjects.collectAsStateWithLifecycle()
    val periods by viewModel.allPeriods.collectAsStateWithLifecycle()
    val pendingTasks by viewModel.pendingTasks.collectAsStateWithLifecycle()
    val scheduleStatus by viewModel.scheduleStatus.collectAsStateWithLifecycle()

    var showLogSheet by remember { mutableStateOf(false) }
    var loggingBlock by remember { mutableStateOf<ClassBlock?>(null) }
    var loggingBlockId by remember { mutableStateOf("") }
    var requestedSessionDate by remember { mutableStateOf("") }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    LaunchedEffect(lessonPrompt) {
        lessonPrompt?.let {
            loggingBlock = null
            loggingBlockId = it.blockId
            requestedSessionDate = it.sessionDate
            showLogSheet = true
        }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize().testTag("home_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Teacher Greeting & Time Header
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "${timeOfDayGreeting(currentTime)}, ${profile?.name ?: "Teacher"}",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = profile?.schoolName ?: "Senior High School",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Box(
                            modifier = Modifier
                                .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(10.dp))
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = currentTime,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "$dateDisplay • ${calendar?.activeTermName ?: "Semester 1"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        // Schedule Content based on Status
        when (val status = scheduleStatus) {
            is DayScheduleStatus.Vacation -> {
                item {
                    VacationBanner(
                        status = status,
                        onReviewLogs = onNavigateToSessions
                    )
                }
            }
            is DayScheduleStatus.Active -> {
                // Conflict Warning Banner if any
                if (status.conflictingBlocks.isNotEmpty()) {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Warning,
                                    contentDescription = "Conflict",
                                    tint = MaterialTheme.colorScheme.error
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "Timetable Period Conflict",
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                    Text(
                                        text = "Two scheduled classes have overlapping hours today. Please adjust in the Timetable tab.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                }
                            }
                        }
                    }
                }

                // NOW CARD
                if (status.currentBlock != null) {
                    item {
                        NowClassCard(
                            block = status.currentBlock,
                            countdownText = status.countdownText,
                            onLogSession = {
                                loggingBlock = status.currentBlock
                                loggingBlockId = status.currentBlock.blockId
                                requestedSessionDate = currentDate
                                showLogSheet = true
                            }
                        )
                    }
                }

                // NEXT CARD
                if (status.nextBlock != null) {
                    item {
                        NextClassCard(
                            block = status.nextBlock,
                            countdownText = status.countdownText
                        )
                    }
                } else if (status.currentBlock == null) {
                    item {
                        AllClassesCompleteCard(
                            message = status.countdownText ?: "All classes finished for today."
                        )
                    }
                }
            }
            is DayScheduleStatus.Empty -> {
                item {
                    EmptyTimetableCard(
                        selectedSubjects = selectedSubjects,
                        onSetupTimetable = onNavigateToTimetable
                    )
                }
            }
        }

        // Quick Action Bar
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = {
                        loggingBlock = null
                        loggingBlockId = ""
                        requestedSessionDate = currentDate
                        showLogSheet = true
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f).testTag("quick_log_session_button")
                ) {
                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Log Lesson")
                }
                OutlinedButton(
                    onClick = onNavigateToTimetable,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f).testTag("view_timetable_button")
                ) {
                    Icon(Icons.Default.EventNote, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Timetable")
                }
            }
        }

        // Urgent Pending Deadlines / Tasks
        if (pendingTasks.isNotEmpty()) {
            item {
                Text(
                    text = "Upcoming Deadlines & Tasks",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            items(pendingTasks.take(3).size) { index ->
                val task = pendingTasks[index]
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = task.isCompleted,
                            onCheckedChange = { checked ->
                                viewModel.toggleTask(task.id, checked)
                            },
                            modifier = Modifier.testTag("task_checkbox_${task.id}")
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = task.title,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "${TaskType.displayLabel(task.type)} • ${taskUrgency(task.dueDate, currentDate).label} • Due ${task.dueDate} • Reminder ${task.reminderTime}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }

    // Session Log Bottom Sheet
    if (showLogSheet) {
        ModalBottomSheet(
            onDismissRequest = { showLogSheet = false },
            sheetState = sheetState
        ) {
            SessionLogBottomSheetContent(
                initialBlock = loggingBlock,
                initialBlockId = loggingBlockId,
                initialDate = requestedSessionDate,
                selectedSubjects = selectedSubjects,
                periods = periods,
                onSave = { subjectId, className, sessionDate, periodBlockId, lastTopic, nextTopic, status, notes,
                           createFollowUpTask, followUpDueDate ->
                    viewModel.logSession(
                        subjectId,
                        className,
                        sessionDate,
                        lastTopic,
                        nextTopic,
                        status,
                        notes,
                        createFollowUpTask,
                        followUpDueDate,
                        periodBlockId
                    )
                    onLessonPromptHandled()
                    scope.launch { sheetState.hide() }.invokeOnCompletion {
                        showLogSheet = false
                    }
                },
                onCancel = {
                    onLessonPromptHandled()
                    scope.launch { sheetState.hide() }.invokeOnCompletion {
                        showLogSheet = false
                    }
                }
            )
        }
    }
}

@Composable
fun NowClassCard(
    block: ClassBlock,
    countdownText: String?,
    onLogSession: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        modifier = Modifier.fillMaxWidth().testTag("now_class_card")
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.secondaryContainer, RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "● CURRENT CLASS NOW",
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
                if (block.isDoublePeriod) {
                    Box(
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.tertiaryContainer, RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "Double Period",
                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = block.subjectName,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Text(
                text = "${block.className} • ${block.classroom}",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
            )

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.AccessTime,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "${block.startTime} – ${block.endTime} (${countdownText ?: ""})",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }

                Button(
                    onClick = onLogSession,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary,
                        contentColor = MaterialTheme.colorScheme.onSecondary
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.testTag("log_now_button")
                ) {
                    Text("Log Lesson")
                }
            }
        }
    }
}

@Composable
fun NextClassCard(
    block: ClassBlock,
    countdownText: String?
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth().testTag("next_class_card")
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "UPCOMING NEXT",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.secondary
                )
                if (countdownText != null) {
                    Box(
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.tertiaryContainer, RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = countdownText,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = block.subjectName,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "${block.className} • ${block.classroom} • Starts at ${block.startTime}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun VacationBanner(
    status: DayScheduleStatus.Vacation,
    onReviewLogs: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth().testTag("vacation_banner")
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.NotificationsOff,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "School Vacation / Recess",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "${status.reason} All school bells, class alerts, and notifications are automatically paused.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (status.resumesDate.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Classes resume: ${status.resumesDate}",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
            Spacer(modifier = Modifier.height(14.dp))
            Button(
                onClick = onReviewLogs,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondary,
                    contentColor = MaterialTheme.colorScheme.onSecondary
                ),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Review Teaching Notes")
            }
        }
    }
}

@Composable
fun AllClassesCompleteCard(message: String) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.CheckCircle,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = "Day Concluded",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun EmptyTimetableCard(
    selectedSubjects: List<SubjectEntity>,
    onSetupTimetable: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth().testTag("empty_timetable_card")
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "Welcome to Edense!",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = if (selectedSubjects.isEmpty()) {
                    "Please choose your 1 or 2 taught subjects in School Setup first."
                } else {
                    "Your weekly timetable is ready for your subjects: ${selectedSubjects.joinToString { it.name }}. Tap below to schedule your class periods."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(14.dp))
            Button(
                onClick = onSetupTimetable,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Build Weekly Timetable")
            }
        }
    }
}

@Composable
fun SessionLogBottomSheetContent(
    initialBlock: ClassBlock?,
    initialBlockId: String = initialBlock?.blockId.orEmpty(),
    initialDate: String = "",
    selectedSubjects: List<SubjectEntity>,
    periods: List<PeriodEntity>,
    onSave: (
        subjectId: String,
        className: String,
        sessionDate: String,
        periodBlockId: String,
        lastTopic: String,
        nextTopic: String,
        status: String,
        notes: String,
        createFollowUpTask: Boolean,
        followUpDueDate: String
    ) -> Unit,
    onCancel: () -> Unit
) {
    val initialPeriod = periods.firstOrNull { it.blockId == initialBlockId }
    val initialSubjectId = initialBlock?.subjectId ?: initialPeriod?.subjectId
        ?: selectedSubjects.firstOrNull()?.id.orEmpty()
    var selectedSubjectId by remember {
        mutableStateOf(initialSubjectId)
    }
    val assignedClasses = remember(selectedSubjectId, selectedSubjects) {
        selectedSubjects.firstOrNull { it.id == selectedSubjectId }
            ?.assignedClasses
            ?.let(ClassroomAssignments::parse)
            .orEmpty()
    }
    var selectedClassName by remember {
        mutableStateOf(
            initialBlock?.className?.takeIf(String::isNotBlank)
                ?: initialPeriod?.className?.takeIf(String::isNotBlank)
                ?: assignedClasses.firstOrNull().orEmpty()
        )
    }
    LaunchedEffect(selectedSubjects) {
        val subject = selectedSubjects.firstOrNull { it.id == selectedSubjectId }
            ?: selectedSubjects.firstOrNull()?.also { selectedSubjectId = it.id }
        if (selectedClassName.isBlank() && subject != null) {
            selectedClassName = ClassroomAssignments.parse(subject.assignedClasses)
                .firstOrNull().orEmpty()
        }
    }
    var sessionDate by remember {
        mutableStateOf(initialDate.ifBlank {
            java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.ROOT).format(java.util.Date())
        })
    }
    var selectedPeriodBlockId by remember { mutableStateOf(initialBlockId) }
    var lastTopic by remember { mutableStateOf("") }
    var nextTopic by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("COMPLETED") }
    var notes by remember { mutableStateOf("") }
    var createFollowUpTask by remember { mutableStateOf(false) }
    var followUpDueDate by remember {
        mutableStateOf(
            java.util.Calendar.getInstance().apply {
                add(java.util.Calendar.DAY_OF_YEAR, 7)
            }.time.let {
                java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(it)
            }
        )
    }
    var currentTimeMillis by remember { mutableStateOf(System.currentTimeMillis()) }
    val keyboardController = LocalSoftwareKeyboardController.current
    val keyboardVisible = WindowInsets.ime.getBottom(LocalDensity.current) > 0
    BackHandler(enabled = keyboardVisible) {
        keyboardController?.hide()
    }
    LaunchedEffect(Unit) {
        while (true) {
            currentTimeMillis = System.currentTimeMillis()
            kotlinx.coroutines.delay(15_000)
        }
    }
    val periodDayOfWeek = remember(sessionDate) { sessionDateDayOfWeek(sessionDate) }
    val availablePeriods = remember(periods, selectedSubjectId, sessionDate) {
        periodsForSessionDate(periods, selectedSubjectId, sessionDate)
            .groupBy { it.blockId }
            .values
            .mapNotNull { block -> block.minByOrNull { it.startTime } }
    }
    LaunchedEffect(selectedSubjectId, sessionDate, availablePeriods) {
        if (availablePeriods.none { it.blockId == selectedPeriodBlockId }) {
            val nextPeriod = availablePeriods.firstOrNull()
            selectedPeriodBlockId = nextPeriod?.blockId.orEmpty()
            nextPeriod?.className?.takeIf(String::isNotBlank)?.let {
                selectedClassName = it
            }
        }
    }
    val selectedPeriod = availablePeriods.firstOrNull { it.blockId == selectedPeriodBlockId }
    val blockPeriods = remember(periods, selectedPeriodBlockId) {
        periods.filter { it.blockId == selectedPeriodBlockId }
    }
    val selectedPeriodEnd = blockPeriods.maxByOrNull { it.endTime }?.endTime.orEmpty()
    val selectedLessonEnd = selectedPeriod?.let { lessonEndTimeMillis(sessionDate, selectedPeriodEnd) }
    val canLogSession = selectedSubjectId.isNotBlank() &&
        selectedPeriod != null &&
        periodDayOfWeek == selectedPeriod.dayOfWeek &&
        selectedLessonEnd != null &&
        isLessonPeriodPast(sessionDate, selectedPeriodEnd, currentTimeMillis) &&
        lastTopic.isNotBlank()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .imePadding()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            text = "Log Teaching Session",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        // Subject selector
        if (selectedSubjects.size > 1) {
            Text(
                text = "Subject:",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                selectedSubjects.forEach { sub ->
                    FilterChip(
                        selected = selectedSubjectId == sub.id,
                        onClick = {
                            selectedSubjectId = sub.id
                            selectedClassName = ClassroomAssignments.parse(sub.assignedClasses)
                                .firstOrNull().orEmpty()
                        },
                        label = { Text(sub.name) }
                    )
                }
            }
        }

        com.example.ui.components.EdenseDatePickerField(
            label = "Lesson Date",
            dateValue = sessionDate,
            onDateSelected = { sessionDate = it },
            modifier = Modifier.fillMaxWidth().testTag("session_date_input")
        )

        Text(
            text = "Class period:",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold
        )
        if (availablePeriods.isEmpty()) {
            Text(
                if (periodDayOfWeek == 0) {
                    "Choose a weekday to see this subject's scheduled periods."
                } else {
                    "No period is scheduled for this subject on ${weekdayLabel(periodDayOfWeek)}."
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                availablePeriods.forEach { period ->
                    val endTime = periods.filter { it.blockId == period.blockId }.maxOf { it.endTime }
                    FilterChip(
                        selected = selectedPeriodBlockId == period.blockId,
                        onClick = {
                            selectedPeriodBlockId = period.blockId
                            selectedClassName = period.className.ifBlank { selectedClassName }
                        },
                        label = {
                            Text(
                                "${period.className.ifBlank { selectedClassName.ifBlank { "Class" } }} • " +
                                    "${period.startTime}–$endTime"
                            )
                        },
                        modifier = Modifier.testTag("session_period_${period.blockId}")
                    )
                }

            }
        }
        if (selectedPeriod != null && selectedLessonEnd != null &&
            selectedLessonEnd >= currentTimeMillis
        ) {
            Text(
                "This lesson can be logged after ${selectedPeriodEnd}.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )
        }

        OutlinedTextField(
            value = lastTopic,
            onValueChange = { lastTopic = it },
            label = { Text("What did you cover today?") },
            placeholder = { Text("e.g. Factorisation of quadratic expressions") },
            modifier = Modifier.fillMaxWidth().testTag("last_topic_input")
        )

        // Status selection
        Text(
            text = "Session Outcome:",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = status == "COMPLETED",
                onClick = { status = "COMPLETED" },
                label = { Text("Completed") }
            )
            FilterChip(
                selected = status == "PARTIAL",
                onClick = { status = "PARTIAL" },
                label = { Text("Unfinished") }
            )
            FilterChip(
                selected = status == "SKIPPED",
                onClick = { status = "SKIPPED" },
                label = { Text("Skipped") }
            )
        }

        OutlinedTextField(
            value = nextTopic,
            onValueChange = { nextTopic = it },
            label = { Text("What is planned for next time?") },
            placeholder = { Text("e.g. Completing the square method") },
            modifier = Modifier.fillMaxWidth().testTag("next_topic_input")
        )

        OutlinedTextField(
            value = notes,
            onValueChange = { notes = it },
            label = { Text("Notes (Optional)") },
            placeholder = { Text("e.g. Exercise 4B questions 1-10 assigned") },
            modifier = Modifier.fillMaxWidth()
        )

        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(
                checked = createFollowUpTask,
                onCheckedChange = { createFollowUpTask = it },
                enabled = nextTopic.isNotBlank(),
                modifier = Modifier.testTag("create_follow_up_task_checkbox")
            )
            Text("Create a follow-up task for the next topic")
        }
        if (createFollowUpTask) {
            com.example.ui.components.EdenseDatePickerField(
                label = "Follow-up task due date",
                dateValue = followUpDueDate,
                onDateSelected = { followUpDueDate = it },
                modifier = Modifier.fillMaxWidth().testTag("follow_up_due_date_input")
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            OutlinedButton(onClick = onCancel) {
                Text("Cancel")
            }
            Spacer(modifier = Modifier.width(12.dp))
            Button(
                onClick = {
                    if (selectedSubjectId.isNotBlank() && lastTopic.isNotBlank()) {
                        onSave(
                            selectedSubjectId,
                            selectedClassName,
                            sessionDate,
                            selectedPeriodBlockId,
                            lastTopic,
                            nextTopic,
                            status,
                            notes,
                            createFollowUpTask,
                            followUpDueDate
                        )
                    }
                },
                enabled = canLogSession,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                modifier = Modifier.testTag("save_session_button")
            ) {
                Text("Save Session")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

private fun weekdayLabel(dayOfWeek: Int): String = when (dayOfWeek) {
    1 -> "Monday"
    2 -> "Tuesday"
    3 -> "Wednesday"
    4 -> "Thursday"
    5 -> "Friday"
    6 -> "Saturday"
    7 -> "Sunday"
    else -> "the selected day"
}
