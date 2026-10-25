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
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.ListAlt
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.entity.TaskEntity
import com.example.data.local.entity.PeriodEntity
import com.example.domain.task.TaskPriorityFilter
import com.example.domain.task.PeriodReminderTiming
import com.example.domain.task.TaskSortOrder
import com.example.domain.task.TaskType
import com.example.domain.task.TaskUrgency
import com.example.domain.task.taskUrgency
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
    val filterState by viewModel.filterState.collectAsStateWithLifecycle()
    val selectedSubjects by viewModel.selectedSubjects.collectAsStateWithLifecycle()
    val periods by viewModel.allPeriods.collectAsStateWithLifecycle()

    var showAddDialog by remember { mutableStateOf(false) }
    var sortMenuExpanded by remember { mutableStateOf(false) }
    var priorityMenuExpanded by remember { mutableStateOf(false) }
    var subjectMenuExpanded by remember { mutableStateOf(false) }
    var typeMenuExpanded by remember { mutableStateOf(false) }

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
            TabRow(selectedTabIndex = filterState.status.ordinal) {
                Tab(
                    selected = filterState.status == TaskFilter.ALL,
                    onClick = { viewModel.setFilter(TaskFilter.ALL) },
                    text = { Text("All") },
                    modifier = Modifier.testTag("task_tab_all")
                )
                Tab(
                    selected = filterState.status == TaskFilter.PENDING,
                    onClick = { viewModel.setFilter(TaskFilter.PENDING) },
                    text = { Text("Pending") },
                    modifier = Modifier.testTag("task_tab_pending")
                )
                Tab(
                    selected = filterState.status == TaskFilter.COMPLETED,
                    onClick = { viewModel.setFilter(TaskFilter.COMPLETED) },
                    text = { Text("Completed") },
                    modifier = Modifier.testTag("task_tab_completed")
                )
            }

            // Multi-criteria Filter & Sort Control Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Sort Menu Chip
                Box {
                    FilterChip(
                        selected = filterState.sortOrder != TaskSortOrder.DUE_DATE_ASC,
                        onClick = { sortMenuExpanded = true },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Sort,
                                contentDescription = "Sort Tasks",
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        trailingIcon = {
                            Icon(
                                Icons.Default.ArrowDropDown,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        label = {
                            Text("Sort: ${filterState.sortOrder.label.substringBefore(" (")}")
                        },
                        modifier = Modifier.testTag("task_sort_chip")
                    )
                    DropdownMenu(
                        expanded = sortMenuExpanded,
                        onDismissRequest = { sortMenuExpanded = false },
                        modifier = Modifier.testTag("task_sort_menu")
                    ) {
                        TaskSortOrder.entries.forEach { order ->
                            DropdownMenuItem(
                                text = {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        if (filterState.sortOrder == order) {
                                            Icon(
                                                Icons.Default.Check,
                                                contentDescription = null,
                                                tint = EdenseNavy,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        } else {
                                            Spacer(modifier = Modifier.width(16.dp))
                                        }
                                        Text(
                                            order.label,
                                            fontWeight = if (filterState.sortOrder == order) FontWeight.Bold else FontWeight.Normal
                                        )
                                    }
                                },
                                onClick = {
                                    viewModel.setSortOrder(order)
                                    sortMenuExpanded = false
                                },
                                modifier = Modifier.testTag("task_sort_option_${order.name}")
                            )
                        }
                    }
                }

                // Priority Filter Chip
                Box {
                    FilterChip(
                        selected = filterState.priority != TaskPriorityFilter.ALL,
                        onClick = { priorityMenuExpanded = true },
                        leadingIcon = {
                            Icon(
                                Icons.Default.FilterList,
                                contentDescription = "Filter by Priority",
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        trailingIcon = {
                            Icon(
                                Icons.Default.ArrowDropDown,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        label = {
                            Text(
                                if (filterState.priority == TaskPriorityFilter.ALL) "Priority: All"
                                else "Priority: ${filterState.priority.label}"
                            )
                        },
                        modifier = Modifier.testTag("task_priority_filter_chip")
                    )
                    DropdownMenu(
                        expanded = priorityMenuExpanded,
                        onDismissRequest = { priorityMenuExpanded = false },
                        modifier = Modifier.testTag("task_priority_menu")
                    ) {
                        TaskPriorityFilter.entries.forEach { prio ->
                            DropdownMenuItem(
                                text = {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        if (filterState.priority == prio) {
                                            Icon(
                                                Icons.Default.Check,
                                                contentDescription = null,
                                                tint = EdenseNavy,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        } else {
                                            Spacer(modifier = Modifier.width(16.dp))
                                        }
                                        Text(
                                            prio.label,
                                            fontWeight = if (filterState.priority == prio) FontWeight.Bold else FontWeight.Normal
                                        )
                                    }
                                },
                                onClick = {
                                    viewModel.setPriorityFilter(prio)
                                    priorityMenuExpanded = false
                                },
                                modifier = Modifier.testTag("task_priority_option_${prio.name}")
                            )
                        }
                    }
                }

                // Subject Filter Chip
                if (selectedSubjects.isNotEmpty()) {
                    val currentSubjectName = selectedSubjects.firstOrNull { it.id == filterState.subjectId }?.name
                    Box {
                        FilterChip(
                            selected = filterState.subjectId != "ALL",
                            onClick = { subjectMenuExpanded = true },
                            trailingIcon = {
                                Icon(
                                    Icons.Default.ArrowDropDown,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                            },
                            label = {
                                Text(
                                    if (currentSubjectName == null) "Subject: All"
                                    else "Subject: $currentSubjectName"
                                )
                            },
                            modifier = Modifier.testTag("task_subject_filter_chip")
                        )
                        DropdownMenu(
                            expanded = subjectMenuExpanded,
                            onDismissRequest = { subjectMenuExpanded = false },
                            modifier = Modifier.testTag("task_subject_menu")
                        ) {
                            DropdownMenuItem(
                                text = {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        if (filterState.subjectId == "ALL") {
                                            Icon(
                                                Icons.Default.Check,
                                                contentDescription = null,
                                                tint = EdenseNavy,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        } else {
                                            Spacer(modifier = Modifier.width(16.dp))
                                        }
                                        Text("All Subjects", fontWeight = if (filterState.subjectId == "ALL") FontWeight.Bold else FontWeight.Normal)
                                    }
                                },
                                onClick = {
                                    viewModel.setSubjectFilter("ALL")
                                    subjectMenuExpanded = false
                                },
                                modifier = Modifier.testTag("task_subject_option_all")
                            )
                            selectedSubjects.forEach { sub ->
                                DropdownMenuItem(
                                    text = {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            if (filterState.subjectId == sub.id) {
                                                Icon(
                                                    Icons.Default.Check,
                                                    contentDescription = null,
                                                    tint = EdenseNavy,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            } else {
                                                Spacer(modifier = Modifier.width(16.dp))
                                            }
                                            Text(
                                                sub.name,
                                                fontWeight = if (filterState.subjectId == sub.id) FontWeight.Bold else FontWeight.Normal
                                            )
                                        }
                                    },
                                    onClick = {
                                        viewModel.setSubjectFilter(sub.id)
                                        subjectMenuExpanded = false
                                    },
                                    modifier = Modifier.testTag("task_subject_option_${sub.id}")
                                )
                            }
                        }
                    }
                }

                // Type Filter Chip
                Box {
                    FilterChip(
                        selected = filterState.type != "ALL",
                        onClick = { typeMenuExpanded = true },
                        trailingIcon = {
                            Icon(
                                Icons.Default.ArrowDropDown,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        label = {
                            Text(
                                if (filterState.type == "ALL") "Type: All"
                                else "Type: ${TaskType.displayLabel(filterState.type)}"
                            )
                        },
                        modifier = Modifier.testTag("task_type_filter_chip")
                    )
                    DropdownMenu(
                        expanded = typeMenuExpanded,
                        onDismissRequest = { typeMenuExpanded = false },
                        modifier = Modifier.testTag("task_type_menu")
                    ) {
                        DropdownMenuItem(
                            text = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    if (filterState.type == "ALL") {
                                        Icon(
                                            Icons.Default.Check,
                                            contentDescription = null,
                                            tint = EdenseNavy,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    } else {
                                        Spacer(modifier = Modifier.width(16.dp))
                                    }
                                    Text("All Types", fontWeight = if (filterState.type == "ALL") FontWeight.Bold else FontWeight.Normal)
                                }
                            },
                            onClick = {
                                viewModel.setTypeFilter("ALL")
                                typeMenuExpanded = false
                            },
                            modifier = Modifier.testTag("task_type_filter_option_all")
                        )
                        TaskType.entries.forEach { taskTypeItem ->
                            DropdownMenuItem(
                                text = {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        if (filterState.type == taskTypeItem.code) {
                                            Icon(
                                                Icons.Default.Check,
                                                contentDescription = null,
                                                tint = EdenseNavy,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        } else {
                                            Spacer(modifier = Modifier.width(16.dp))
                                        }
                                        Text(
                                            taskTypeItem.displayName,
                                            fontWeight = if (filterState.type == taskTypeItem.code) FontWeight.Bold else FontWeight.Normal
                                        )
                                    }
                                },
                                onClick = {
                                    viewModel.setTypeFilter(taskTypeItem.code)
                                    typeMenuExpanded = false
                                },
                                modifier = Modifier.testTag("task_type_filter_option_${taskTypeItem.code}")
                            )
                        }
                    }
                }

                // Clear Filters Button
                if (filterState.hasActiveSecondaryFilters) {
                    FilterChip(
                        selected = true,
                        onClick = { viewModel.clearFilters() },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Clear Filters",
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        label = { Text("Clear Filters") },
                        modifier = Modifier.testTag("task_clear_filters_chip")
                    )
                }
            }

            // Results count header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${tasks.size} ${if (tasks.size == 1) "task" else "tasks"}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (filterState.hasActiveSecondaryFilters) {
                    Text(
                        text = "Filtered view",
                        style = MaterialTheme.typography.labelSmall,
                        color = EdenseAmber,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            if (tasks.isEmpty()) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(16.dp)
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
            } else {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        TaskTableHeader()
                        HorizontalDivider()
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentPadding = PaddingValues(bottom = 8.dp)
                        ) {
                            items(tasks, key = { it.id }) { task ->
                                val subject = selectedSubjects.firstOrNull { it.id == task.subjectId }
                                TaskTableRow(
                                    task = task,
                                    subjectName = subject?.name.orEmpty(),
                                    onToggle = { isChecked -> viewModel.toggleTask(task.id, isChecked) },
                                    onDelete = { viewModel.deleteTask(task.id) }
                                )
                                HorizontalDivider()
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddTaskDialog(
            selectedSubjects = selectedSubjects,
            periods = periods,
            onDismiss = { showAddDialog = false },
            onConfirm = { title, subjectId, type, dueDate, reminderTime, notes, periodBlockId, timing ->
                viewModel.addTask(
                    title,
                    subjectId,
                    type,
                    dueDate,
                    reminderTime,
                    notes,
                    periodBlockId,
                    timing
                )
                showAddDialog = false
            }
        )
    }
}

@Composable
private fun TaskTableHeader() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 8.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Task",
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Due",
            modifier = Modifier.width(72.dp),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Text(
            text = "Done",
            modifier = Modifier.width(48.dp),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.width(48.dp))
    }
}

@Composable
private fun TaskTableRow(
    task: TaskEntity,
    subjectName: String,
    onToggle: (Boolean) -> Unit,
    onDelete: () -> Unit
) {
    val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    val urgency = taskUrgency(task.dueDate, today)
    val urgencyColor = when (urgency) {
        TaskUrgency.OVERDUE -> MaterialTheme.colorScheme.error
        TaskUrgency.DUE_TODAY, TaskUrgency.DUE_SOON -> EdenseAmber
        TaskUrgency.UPCOMING -> EdenseGreen
        TaskUrgency.UNKNOWN -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    val details = listOfNotNull(
        TaskType.displayLabel(task.type),
        subjectName.takeIf { it.isNotBlank() },
        task.className.takeIf { it.isNotBlank() }
    ).joinToString(" • ")

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                if (task.isCompleted) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                else MaterialTheme.colorScheme.surface
            )
            .padding(horizontal = 8.dp, vertical = 6.dp)
            .testTag("task_row_${task.id}"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(end = 8.dp)
        ) {
            Text(
                text = task.title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                textDecoration = if (task.isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                color = if (task.isCompleted) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = details,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = if (task.isCompleted) "Completed" else urgency.label,
                style = MaterialTheme.typography.labelSmall,
                color = if (task.isCompleted) EdenseGreen else urgencyColor,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1
            )
        }
        Text(
            text = task.dueDate,
            modifier = Modifier.width(72.dp),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        Checkbox(
            checked = task.isCompleted,
            onCheckedChange = onToggle,
            modifier = Modifier
                .size(48.dp)
                .testTag("task_checkbox_inner_${task.id}")
        )
        IconButton(onClick = onDelete) {
            Icon(
                Icons.Default.Delete,
                contentDescription = "Delete",
                tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTaskDialog(
    selectedSubjects: List<com.example.data.local.entity.SubjectEntity>,
    periods: List<PeriodEntity>,
    onDismiss: () -> Unit,
    onConfirm: (
        title: String,
        subjectId: String,
        type: String,
        dueDate: String,
        reminderTime: String,
        notes: String,
        periodBlockId: String,
        periodReminderTiming: String
    ) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var subjectId by remember { mutableStateOf(selectedSubjects.firstOrNull()?.id ?: "") }
    var type by remember { mutableStateOf(TaskType.ASSIGNMENT.code) }
    var typeDropdownExpanded by remember { mutableStateOf(false) }
    var dueDate by remember {
        mutableStateOf(SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()))
    }
    var reminderTime by remember { mutableStateOf("06:00") }
    var notes by remember { mutableStateOf("") }
    var selectedPeriodBlockId by remember { mutableStateOf("") }
    var periodDropdownExpanded by remember { mutableStateOf(false) }
    var periodReminderTiming by remember { mutableStateOf(PeriodReminderTiming.BEFORE_START.name) }
    val availablePeriods = remember(periods, subjectId) {
        periods.filter { it.subjectId == subjectId }
            .groupBy { it.blockId }
            .values
            .mapNotNull { block -> block.minByOrNull { it.startTime } }
    }
    val selectedPeriod = availablePeriods.firstOrNull { it.blockId == selectedPeriodBlockId }

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
            Column(
                modifier = Modifier
                    .heightIn(max = 480.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Task Title") },
                    placeholder = { Text("e.g. Collect Exercise 4B Homework") },
                    modifier = Modifier.fillMaxWidth().testTag("add_task_title")
                )

                if (selectedSubjects.isNotEmpty()) {
                    Text(text = "Subject:", style = MaterialTheme.typography.labelMedium)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        selectedSubjects.forEach { sub ->
                            FilterChip(
                                selected = subjectId == sub.id,
                                onClick = { subjectId = sub.id },
                                label = { Text(sub.name) },
                                modifier = Modifier.testTag("task_subject_chip_${sub.id}")
                            )
                        }
                    }
                }

                // Academic Task Type - Select Dropdown
                ExposedDropdownMenuBox(
                    expanded = typeDropdownExpanded,
                    onExpandedChange = { typeDropdownExpanded = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("add_task_type_dropdown_box")
                ) {
                    OutlinedTextField(
                        value = TaskType.displayLabel(type),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Task Type") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = typeDropdownExpanded) },
                        colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                            .testTag("add_task_type_dropdown")
                    )
                    ExposedDropdownMenu(
                        expanded = typeDropdownExpanded,
                        onDismissRequest = { typeDropdownExpanded = false },
                        modifier = Modifier.testTag("add_task_type_menu")
                    ) {
                        TaskType.entries.forEach { taskTypeItem ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = taskTypeItem.displayName,
                                        fontWeight = if (type == taskTypeItem.code) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                onClick = {
                                    type = taskTypeItem.code
                                    typeDropdownExpanded = false
                                },
                                contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
                                modifier = Modifier.testTag("task_type_option_${taskTypeItem.code}")
                            )
                        }
                    }
                }

                ExposedDropdownMenuBox(
                    expanded = periodDropdownExpanded,
                    onExpandedChange = { periodDropdownExpanded = it },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = selectedPeriod?.let {
                            "${it.className.ifBlank { "Class" }} • ${weekdayName(it.dayOfWeek)} • " +
                                "${it.startTime}–${availablePeriodsForBlock(periods, it.blockId).maxOf { p -> p.endTime }}"
                        } ?: "No linked period",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Link to a class period (optional)") },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = periodDropdownExpanded)
                        },
                        modifier = Modifier.menuAnchor().fillMaxWidth().testTag("task_period_dropdown")
                    )
                    ExposedDropdownMenu(
                        expanded = periodDropdownExpanded,
                        onDismissRequest = { periodDropdownExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("No linked period") },
                            onClick = {
                                selectedPeriodBlockId = ""
                                periodDropdownExpanded = false
                            }
                        )
                        availablePeriods.forEach { period ->
                            val blockEnd = availablePeriodsForBlock(periods, period.blockId)
                                .maxOf { it.endTime }
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        "${period.className.ifBlank { "Class" }} • ${weekdayName(period.dayOfWeek)} • " +
                                            "${period.startTime}–$blockEnd"
                                    )
                                },
                                onClick = {
                                    selectedPeriodBlockId = period.blockId
                                    periodDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                if (selectedPeriod != null) {
                    Text(
                        "Reminder timing (next occurrence)",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        PeriodReminderTiming.entries.forEach { timing ->
                            FilterChip(
                                selected = periodReminderTiming == timing.name,
                                onClick = { periodReminderTiming = timing.name },
                                label = { Text(timing.label) },
                                modifier = Modifier.testTag("task_period_timing_${timing.name}")
                            )
                        }
                    }
                    Text(
                        "Before-class reminders are 5 minutes before the selected boundary; " +
                            "after-class reminders are sent when the period ends. The task due date " +
                            "remains separate; this reminder is for the next scheduled class.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    com.example.ui.components.EdenseDatePickerField(
                        label = "Due Date",
                        dateValue = dueDate,
                        onDateSelected = { dueDate = it },
                        modifier = if (selectedPeriod == null) {
                            Modifier.weight(1f).testTag("add_task_date")
                        } else {
                            Modifier.fillMaxWidth().testTag("add_task_date")
                        }
                    )
                    if (selectedPeriod == null) {
                        com.example.ui.components.EdenseTimePickerField(
                            label = "Reminder Time",
                            timeValue = reminderTime,
                            onTimeSelected = { reminderTime = it },
                            modifier = Modifier.weight(1f).testTag("add_task_time")
                        )
                    }
                }

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes (Optional)") },
                    modifier = Modifier.fillMaxWidth().testTag("add_task_notes")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank()) {
                        onConfirm(
                            title,
                            subjectId,
                            type,
                            dueDate,
                            reminderTime,
                            notes,
                            selectedPeriodBlockId,
                            periodReminderTiming
                        )
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
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("cancel_add_task")
            ) {
                Text("Cancel")
            }
        }
    )
}

private fun availablePeriodsForBlock(periods: List<PeriodEntity>, blockId: String): List<PeriodEntity> =
    periods.filter { it.blockId == blockId }

private fun weekdayName(dayOfWeek: Int): String = when (dayOfWeek) {
    1 -> "Monday"
    2 -> "Tuesday"
    3 -> "Wednesday"
    4 -> "Thursday"
    5 -> "Friday"
    else -> "Unknown day"
}
