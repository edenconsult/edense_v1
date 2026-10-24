package com.example.ui.screens.tasks

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ListAlt
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
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.entity.TaskEntity
import com.example.ui.theme.EdenseAmber
import com.example.ui.theme.EdenseAmberContainer
import com.example.ui.theme.EdenseGreen
import com.example.ui.theme.EdenseGreenContainer
import com.example.ui.theme.EdenseNavy
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TasksScreen(
    viewModel: TasksViewModel,
    modifier: Modifier = Modifier
) {
    val tasks by viewModel.filteredTasks.collectAsStateWithLifecycle()
    val filter by viewModel.filter.collectAsStateWithLifecycle()
    val typeFilter by viewModel.typeFilter.collectAsStateWithLifecycle()
    val selectedSubjects by viewModel.selectedSubjects.collectAsStateWithLifecycle()

    var showAddDialog by remember { mutableStateOf(false) }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = EdenseNavy,
                contentColor = Color.White,
                modifier = Modifier.testTag("add_task_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Task")
            }
        },
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .testTag("tasks_screen")
        ) {
            // Status Tabs
            TabRow(selectedTabIndex = filter.ordinal) {
                Tab(
                    selected = filter == TaskFilter.ALL,
                    onClick = { viewModel.setFilter(TaskFilter.ALL) },
                    text = { Text("All") }
                )
                Tab(
                    selected = filter == TaskFilter.PENDING,
                    onClick = { viewModel.setFilter(TaskFilter.PENDING) },
                    text = { Text("Pending") }
                )
                Tab(
                    selected = filter == TaskFilter.COMPLETED,
                    onClick = { viewModel.setFilter(TaskFilter.COMPLETED) },
                    text = { Text("Completed") }
                )
            }

            // Type Filter Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = typeFilter == "ALL",
                    onClick = { viewModel.setTypeFilter("ALL") },
                    label = { Text("All Types") }
                )
                FilterChip(
                    selected = typeFilter == "ASSIGNMENT",
                    onClick = { viewModel.setTypeFilter("ASSIGNMENT") },
                    label = { Text("Assignments") }
                )
                FilterChip(
                    selected = typeFilter == "TEST",
                    onClick = { viewModel.setTypeFilter("TEST") },
                    label = { Text("Tests/Exams") }
                )
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (tasks.isEmpty()) {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth().padding(top = 24.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    Icons.Default.ListAlt,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(36.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "No tasks in this view",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Tap + to add an assignment, class test, or project deadline.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                } else {
                    items(tasks, key = { it.id }) { task ->
                        val subject = selectedSubjects.firstOrNull { it.id == task.subjectId }
                        TaskItemCard(
                            task = task,
                            subjectName = subject?.name ?: "",
                            onToggle = { isChecked -> viewModel.toggleTask(task.id, isChecked) },
                            onDelete = { viewModel.deleteTask(task.id) }
                        )
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddTaskDialog(
            selectedSubjects = selectedSubjects,
            onDismiss = { showAddDialog = false },
            onConfirm = { title, subjectId, type, dueDate, reminderTime, notes ->
                viewModel.addTask(title, subjectId, type, dueDate, reminderTime, notes)
                showAddDialog = false
            }
        )
    }
}

@Composable
fun TaskItemCard(
    task: TaskEntity,
    subjectName: String,
    onToggle: (Boolean) -> Unit,
    onDelete: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (task.isCompleted) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
            else MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = if (task.isCompleted) 0.dp else 2.dp),
        modifier = Modifier.fillMaxWidth().testTag("task_card_${task.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = task.isCompleted,
                onCheckedChange = onToggle,
                modifier = Modifier.testTag("task_checkbox_inner_${task.id}")
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    textDecoration = if (task.isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                    color = if (task.isCompleted) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val (typeColor, typeBg) = when (task.type) {
                        "TEST", "EXAM" -> EdenseAmber to EdenseAmberContainer
                        "PROJECT" -> EdenseGreen to EdenseGreenContainer
                        else -> EdenseNavy to MaterialTheme.colorScheme.surfaceVariant
                    }
                    Box(
                        modifier = Modifier
                            .background(typeBg, RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = task.type,
                            color = typeColor,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    if (subjectName.isNotBlank()) {
                        Text(
                            text = "• $subjectName",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = "• Due ${task.dueDate}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Composable
fun AddTaskDialog(
    selectedSubjects: List<com.example.data.local.entity.SubjectEntity>,
    onDismiss: () -> Unit,
    onConfirm: (title: String, subjectId: String, type: String, dueDate: String, reminderTime: String, notes: String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var subjectId by remember { mutableStateOf(selectedSubjects.firstOrNull()?.id ?: "") }
    var type by remember { mutableStateOf("ASSIGNMENT") }
    var dueDate by remember {
        mutableStateOf(SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()))
    }
    var reminderTime by remember { mutableStateOf("06:00") }
    var notes by remember { mutableStateOf("") }

    val types = listOf("ASSIGNMENT", "TEST", "EXAM", "PROJECT")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Add Academic Task / Deadline",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Task Title") },
                    placeholder = { Text("e.g. Collect Exercise 4B Homework") },
                    modifier = Modifier.fillMaxWidth().testTag("add_task_title")
                )

                if (selectedSubjects.isNotEmpty()) {
                    Text(text = "Subject:", style = MaterialTheme.typography.labelMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        selectedSubjects.forEach { sub ->
                            FilterChip(
                                selected = subjectId == sub.id,
                                onClick = { subjectId = sub.id },
                                label = { Text(sub.name) }
                            )
                        }
                    }
                }

                Text(text = "Type:", style = MaterialTheme.typography.labelMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    types.forEach { t ->
                        FilterChip(
                            selected = type == t,
                            onClick = { type = t },
                            label = { Text(t) }
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    com.example.ui.components.EdenseDatePickerField(
                        label = "Due Date",
                        dateValue = dueDate,
                        onDateSelected = { dueDate = it },
                        modifier = Modifier.weight(1f).testTag("add_task_date")
                    )
                    com.example.ui.components.EdenseTimePickerField(
                        label = "Reminder Time",
                        timeValue = reminderTime,
                        onTimeSelected = { reminderTime = it },
                        modifier = Modifier.weight(1f).testTag("add_task_time")
                    )
                }

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes (Optional)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank()) {
                        onConfirm(title, subjectId, type, dueDate, reminderTime, notes)
                    }
                },
                enabled = title.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = EdenseNavy),
                modifier = Modifier.testTag("confirm_add_task")
            ) {
                Text("Add Task")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
