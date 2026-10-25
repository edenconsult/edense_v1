package com.example.ui.screens.sessions

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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FormatListBulleted
import androidx.compose.material.icons.filled.HourglassBottom
import androidx.compose.material.icons.filled.RemoveCircleOutline
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.entity.TeachingSessionEntity
import com.example.data.local.entity.displayStatus
import com.example.ui.screens.home.SessionLogBottomSheetContent
import com.example.ui.theme.EdenseAmber
import com.example.ui.theme.EdenseAmberContainer
import com.example.ui.theme.EdenseGreen
import com.example.ui.theme.EdenseGreenContainer
import com.example.ui.theme.EdenseNavy
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionsScreen(
    viewModel: SessionsViewModel,
    modifier: Modifier = Modifier
) {
    val carryForwards by viewModel.carryForwardSummaries.collectAsStateWithLifecycle()
    val sessions by viewModel.filteredSessions.collectAsStateWithLifecycle()
    val selectedSubjects by viewModel.selectedSubjects.collectAsStateWithLifecycle()
    val filterSubjectId by viewModel.filterSubjectId.collectAsStateWithLifecycle()

    var showLogSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    Scaffold(
        floatingActionButton = {
            if (selectedSubjects.isNotEmpty()) {
                FloatingActionButton(
                    onClick = { showLogSheet = true },
                    containerColor = EdenseNavy,
                    contentColor = Color.White,
                    modifier = Modifier.testTag("log_session_fab")
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Log Session")
                }
            }
        },
        modifier = modifier
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .testTag("sessions_screen"),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header / Intro Card
            item {
                Text(
                    text = "Teaching State Tracker",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = EdenseNavy
                )
                Text(
                    text = "Know exactly where you left off, what was covered, and what to teach next.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Carry-Forward Cards for each assigned subject
            if (carryForwards.isNotEmpty()) {
                item {
                    Text(
                        text = "Next Lesson Carry-Forward",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = EdenseNavy
                    )
                }
                itemsIndexed(
                    items = carryForwards,
                    key = { index, item -> carryForwardStableKey(index, item) }
                ) { _, item ->
                    CarryForwardCard(item = item)
                }
            }

            // Subject Filter Chips
            if (selectedSubjects.size > 1) {
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = filterSubjectId == null,
                            onClick = { viewModel.setSubjectFilter(null) },
                            label = { Text("All Subjects") }
                        )
                        selectedSubjects.forEach { sub ->
                            FilterChip(
                                selected = filterSubjectId == sub.id,
                                onClick = { viewModel.setSubjectFilter(sub.id) },
                                label = { Text(sub.name) }
                            )
                        }
                    }
                }
            }

            // Session History Section
            item {
                Text(
                    text = "Session History (${sessions.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = EdenseNavy
                )
            }

            if (sessions.isEmpty()) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Default.FormatListBulleted,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "No teaching sessions recorded yet.",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Tap the + button to log your first lesson.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(sessions, key = { it.id }) { session ->
                    val subject = selectedSubjects.firstOrNull { it.id == session.subjectId }
                    SessionHistoryCard(
                        session = session,
                        subjectName = subject?.name ?: "Subject",
                        subjectColor = subject?.colorHex ?: "#1E6F5C"
                    )
                }
            }
        }
    }

    if (showLogSheet) {
        ModalBottomSheet(
            onDismissRequest = { showLogSheet = false },
            sheetState = sheetState
        ) {
            SessionLogBottomSheetContent(
                initialBlock = null,
                selectedSubjects = selectedSubjects,
                onSave = { subjectId, className, sessionDate, lastTopic, nextTopic, status, notes,
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
                        followUpDueDate
                    )
                    scope.launch { sheetState.hide() }.invokeOnCompletion {
                        showLogSheet = false
                    }
                },
                onCancel = {
                    scope.launch { sheetState.hide() }.invokeOnCompletion {
                        showLogSheet = false
                    }
                }
            )
        }
    }
}

internal fun carryForwardStableKey(index: Int, item: ClassCarryForward): String =
    "${item.subject.id}:${item.className}:$index"

@Composable
fun CarryForwardCard(item: ClassCarryForward) {
    val subject = item.subject
    val session = item.latestSession

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth().testTag("carry_forward_${subject.id}_${item.className}")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(Color(android.graphics.Color.parseColor(subject.colorHex)))
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = subject.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = EdenseNavy
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = item.className.ifBlank { "Class not specified" },
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (session != null) {
                // Last topic covered
                Text(
                    text = "LAST COVERED:",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = session.lastTopicCovered,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Next planned topic
                Text(
                    text = "PLANNED NEXT:",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = EdenseGreen
                )
                Text(
                    text = session.nextTopicPlanned.ifBlank { "To be determined" },
                    style = MaterialTheme.typography.bodyMedium,
                    color = EdenseGreen,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Logged on ${session.sessionDate}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    val (statusColor, statusBg) = when (session.status) {
                        "PARTIAL" -> EdenseAmber to EdenseAmberContainer
                        "SKIPPED" -> MaterialTheme.colorScheme.error to MaterialTheme.colorScheme.errorContainer
                        else -> EdenseGreen to EdenseGreenContainer
                    }

                    Box(
                        modifier = Modifier
                            .background(statusBg, RoundedCornerShape(4.dp))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = session.displayStatus(),
                            color = statusColor,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            } else {
                Text(
                    text = "No prior teaching sessions logged for this class yet.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun SessionHistoryCard(
    session: TeachingSessionEntity,
    subjectName: String,
    subjectColor: String
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth().testTag("session_card_${session.id}")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(Color(android.graphics.Color.parseColor(subjectColor)))
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = subjectName,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    val (statusColor, statusBg) = when (session.status) {
                        "PARTIAL" -> EdenseAmber to EdenseAmberContainer
                        "SKIPPED" -> MaterialTheme.colorScheme.error to MaterialTheme.colorScheme.errorContainer
                        else -> EdenseGreen to EdenseGreenContainer
                    }
                    Box(
                        modifier = Modifier
                            .background(statusBg, RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = session.displayStatus(),
                            color = statusColor,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        Icons.Default.CalendarToday,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${session.className.ifBlank { "Class not specified" }} • ${session.sessionDate}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Covered: ${session.lastTopicCovered}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )

            if (session.nextTopicPlanned.isNotBlank()) {
                Text(
                    text = "Next: ${session.nextTopicPlanned}",
                    style = MaterialTheme.typography.bodySmall,
                    color = EdenseGreen
                )
            }

            if (session.notes.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Notes: ${session.notes}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
