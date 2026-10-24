package com.example.ui.screens.timetable

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import com.example.domain.schedule.ClassBlock
import com.example.domain.schedule.ClassroomAssignments
import com.example.domain.schedule.SchedulingEngine
import com.example.ui.theme.EdenseAmber
import com.example.ui.theme.EdenseGreen
import com.example.ui.theme.EdenseNavy

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimetableScreen(
    viewModel: TimetableViewModel,
    modifier: Modifier = Modifier
) {
    val selectedDay by viewModel.selectedDay.collectAsStateWithLifecycle()
    val blocks by viewModel.blocksForSelectedDay.collectAsStateWithLifecycle()
    val quotas by viewModel.subjectQuotas.collectAsStateWithLifecycle()
    val selectedSubjects by viewModel.selectedSubjects.collectAsStateWithLifecycle()
    val conflictWarning by viewModel.conflictWarning.collectAsStateWithLifecycle()

    var showAddDialog by remember { mutableStateOf(false) }

    val daysOfWeek = listOf("Mon", "Tue", "Wed", "Thu", "Fri")

    Scaffold(
        floatingActionButton = {
            if (selectedSubjects.isNotEmpty()) {
                FloatingActionButton(
                    onClick = { showAddDialog = true },
                    containerColor = EdenseNavy,
                    contentColor = Color.White,
                    modifier = Modifier.testTag("add_period_fab")
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Period")
                }
            }
        },
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Day selector tabs
            TabRow(selectedTabIndex = selectedDay - 1) {
                daysOfWeek.forEachIndexed { index, label ->
                    val dayNum = index + 1
                    Tab(
                        selected = selectedDay == dayNum,
                        onClick = { viewModel.selectDay(dayNum) },
                        text = {
                            Text(
                                text = label,
                                fontWeight = if (selectedDay == dayNum) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        modifier = Modifier.testTag("day_tab_$dayNum")
                    )
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Conflict Warning banner if present
                if (conflictWarning != null) {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Warning,
                                    contentDescription = "Warning",
                                    tint = MaterialTheme.colorScheme.error
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = conflictWarning ?: "",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(onClick = { viewModel.clearWarning() }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Clear", tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }

                // Weekly Quota Progress Tracker
                if (quotas.isNotEmpty()) {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = RoundedCornerShape(12.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                            modifier = Modifier.fillMaxWidth().testTag("quota_tracker_card")
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(
                                    text = "Weekly Period Targets",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = EdenseNavy
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                quotas.forEach { quota ->
                                    val progress = if (quota.targetPeriods > 0) {
                                        (quota.scheduledPeriodsCount.toFloat() / quota.targetPeriods).coerceIn(0f, 1f)
                                    } else 0f

                                    Column(modifier = Modifier.padding(vertical = 4.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "${quota.subject.name} (${quota.subject.assignedClasses.ifBlank { "SHS" }})",
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                            Text(
                                                text = "${quota.scheduledPeriodsCount} / ${quota.targetPeriods} periods",
                                                style = MaterialTheme.typography.bodySmall,
                                                fontWeight = FontWeight.Bold,
                                                color = if (quota.scheduledPeriodsCount >= quota.targetPeriods) EdenseGreen else EdenseAmber
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(6.dp))
                                        LinearProgressIndicator(
                                            progress = { progress },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(6.dp)
                                                .clip(RoundedCornerShape(3.dp)),
                                            color = if (quota.scheduledPeriodsCount >= quota.targetPeriods) EdenseGreen else EdenseNavy,
                                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Periods list for selected day
                if (blocks.isEmpty()) {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    Icons.Default.AccessTime,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(36.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "No Classes on ${daysOfWeek[selectedDay - 1]}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Tap the + button below to add your class periods for this day.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                } else {
                    items(blocks, key = { it.blockId }) { block ->
                        TimetableBlockCard(
                            block = block,
                            onDelete = { viewModel.deleteBlock(block) }
                        )
                    }
                }
            }
        }
    }

    // Add Period Dialog - Strictly Gated to Teacher's Selected Subjects (Max 2)
    if (showAddDialog) {
        AddPeriodDialog(
            selectedSubjects = selectedSubjects,
            defaultDay = selectedDay,
            onDismiss = { showAddDialog = false },
            onConfirm = { subjectId, day, start, className, classroom, isDouble ->
                viewModel.addPeriod(subjectId, day, start, className, classroom, isDouble)
                showAddDialog = false
            }
        )
    }
}

@Composable
fun TimetableBlockCard(
    block: ClassBlock,
    onDelete: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("period_block_${block.blockId}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Subject color pill
            Box(
                modifier = Modifier
                    .width(6.dp)
                    .height(56.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(Color(android.graphics.Color.parseColor(block.colorHex)))
            )
            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = block.subjectName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = EdenseNavy
                    )
                    if (block.isDoublePeriod) {
                        Box(
                            modifier = Modifier
                                .background(EdenseAmber, RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "Double Period",
                                color = Color.White,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.AccessTime,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${block.startTime} – ${block.endTime} (${block.durationMinutes} mins)",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = EdenseGreen,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${block.className} • ${block.classroom}",
                        style = MaterialTheme.typography.bodySmall,
                        color = EdenseGreen,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Delete Period",
                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
                )
            }
        }
    }
}

@Composable
fun AddPeriodDialog(
    selectedSubjects: List<com.example.data.local.entity.SubjectEntity>,
    defaultDay: Int,
    onDismiss: () -> Unit,
    onConfirm: (subjectId: String, day: Int, start: String, className: String, classroom: String, isDouble: Boolean) -> Unit
) {
    var subjectId by remember { mutableStateOf(selectedSubjects.firstOrNull()?.id ?: "") }
    var selectedDay by remember { mutableStateOf(defaultDay) }
    var startTime by remember { mutableStateOf("07:00") }
    var isDoublePeriod by remember { mutableStateOf(false) }
    var className by remember {
        mutableStateOf(selectedSubjects.firstOrNull()?.assignedClasses?.split(",")?.firstOrNull()?.trim().orEmpty())
    }
    val selectedSubject = selectedSubjects.firstOrNull { it.id == subjectId }
    val classOptions = ClassroomAssignments.parse(selectedSubject?.assignedClasses.orEmpty())
    val classroomOptions = ClassroomAssignments.parse(selectedSubject?.assignedClassrooms.orEmpty())
    var classroom by remember {
        mutableStateOf(selectedSubjects.firstOrNull()?.let { subject ->
            val classes = ClassroomAssignments.parse(subject.assignedClasses)
            val rooms = ClassroomAssignments.parse(subject.assignedClassrooms)
            ClassroomAssignments.roomForClass(className, classes, rooms)
        } ?: "")
    }
    val selectedSlotIndex = SchedulingEngine.periodSlots.indexOfFirst { it.startTime == startTime }
    val selectedSlot = SchedulingEngine.periodSlots.getOrNull(selectedSlotIndex)
    val nextSlot = SchedulingEngine.periodSlots.getOrNull(selectedSlotIndex + 1)
    val canMakeDouble = selectedSlot != null && nextSlot?.startMinute == selectedSlot.endMinute
    val endTime = if (isDoublePeriod && canMakeDouble) nextSlot!!.endTime else selectedSlot?.endTime.orEmpty()

    val daysOfWeek = listOf("Mon", "Tue", "Wed", "Thu", "Fri")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Add Timetable Period",
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
                    text = "Select one of your assigned subjects:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )

                // Strictly gated to selected subjects!
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    selectedSubjects.forEach { sub ->
                        FilterChip(
                            selected = subjectId == sub.id,
                            onClick = {
                                subjectId = sub.id
                                val classes = ClassroomAssignments.parse(sub.assignedClasses)
                                val rooms = ClassroomAssignments.parse(sub.assignedClassrooms)
                                className = classes.firstOrNull().orEmpty()
                                classroom = ClassroomAssignments.roomForClass(className, classes, rooms)
                            },
                            label = { Text(sub.name) }
                        )
                    }
                }

                // Day Selection
                Text(
                    text = "Day:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    daysOfWeek.forEachIndexed { idx, label ->
                        val dayVal = idx + 1
                        FilterChip(
                            selected = selectedDay == dayVal,
                            onClick = { selectedDay = dayVal },
                            label = { Text(label) }
                        )
                    }
                }

                Text(
                    text = "One-hour periods (school breaks: 09:00–09:30 and 12:30–13:00)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(SchedulingEngine.periodSlots) { slot ->
                        FilterChip(
                            selected = startTime == slot.startTime,
                            onClick = {
                                startTime = slot.startTime
                                if (isDoublePeriod) {
                                    val idx = SchedulingEngine.periodSlots.indexOf(slot)
                                    val next = SchedulingEngine.periodSlots.getOrNull(idx + 1)
                                    if (next?.startMinute != slot.endMinute) isDoublePeriod = false
                                }
                            },
                            label = { Text("${slot.startTime}–${slot.endTime}") }
                        )
                    }
                }

                // Double Period Checkbox
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Checkbox(
                        checked = isDoublePeriod,
                        onCheckedChange = { isDoublePeriod = it },
                        enabled = canMakeDouble,
                        modifier = Modifier.testTag("double_period_checkbox")
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Column {
                        Text(
                            text = "Double Period (Continuous Block)",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = if (canMakeDouble) {
                                "Two adjacent one-hour periods: ${selectedSlot?.startTime}–$endTime"
                            } else {
                                "Unavailable here because a school break separates the next period."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (classOptions.isNotEmpty()) {
                    Text(
                        text = "Class and its assigned classroom:",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(classOptions) { option ->
                            val room = ClassroomAssignments.roomForClass(
                                option,
                                classOptions,
                                classroomOptions
                            )
                            FilterChip(
                                selected = className == option,
                                onClick = {
                                    className = option
                                    classroom = room
                                },
                                label = { Text("$option • $room") }
                            )
                        }
                    }
                } else {
                    OutlinedTextField(
                        value = className,
                        onValueChange = { className = it },
                        label = { Text("Class (e.g. SHS 2B)") },
                        modifier = Modifier.fillMaxWidth().testTag("dialog_class_name")
                    )
                }
                OutlinedTextField(
                    value = classroom,
                    onValueChange = { classroom = it },
                    label = { Text("Classroom for this class (e.g. Room 14)") },
                    modifier = Modifier.fillMaxWidth().testTag("dialog_classroom")
                )
                Text(
                    text = "Selected time: ${selectedSlot?.startTime ?: startTime}–$endTime",
                    style = MaterialTheme.typography.labelMedium
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (subjectId.isNotBlank() && className.isNotBlank() && classroom.isNotBlank() && endTime.isNotBlank()) {
                        onConfirm(subjectId, selectedDay, startTime, className, classroom, isDoublePeriod)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = EdenseNavy),
                modifier = Modifier.testTag("confirm_add_period")
            ) {
                Text("Add Period")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
