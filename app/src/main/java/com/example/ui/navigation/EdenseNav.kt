package com.example.ui.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.EventNote
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Assignment
import androidx.compose.material.icons.outlined.EventNote
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import com.example.data.remote.SyncState
import kotlinx.coroutines.launch
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.screens.home.HomeScreen
import com.example.ui.screens.home.HomeViewModel
import com.example.ui.screens.sessions.SessionsScreen
import com.example.ui.screens.sessions.SessionsViewModel
import com.example.ui.screens.setup.SetupScreen
import com.example.ui.screens.setup.SetupViewModel
import com.example.ui.screens.tasks.TasksScreen
import com.example.ui.screens.tasks.TasksViewModel
import com.example.ui.screens.timetable.TimetableScreen
import com.example.ui.screens.timetable.TimetableViewModel

enum class NavDestination(
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val tag: String
) {
    HOME("Now", Icons.Filled.Home, Icons.Outlined.Home, "nav_home"),
    TIMETABLE("Timetable", Icons.Filled.EventNote, Icons.Outlined.EventNote, "nav_timetable"),
    SESSIONS("Sessions", Icons.Filled.MenuBook, Icons.Outlined.MenuBook, "nav_sessions"),
    TASKS("Tasks", Icons.Filled.Assignment, Icons.Outlined.Assignment, "nav_tasks")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EdenseNav(
    homeViewModel: HomeViewModel,
    timetableViewModel: TimetableViewModel,
    sessionsViewModel: SessionsViewModel,
    tasksViewModel: TasksViewModel,
    setupViewModel: SetupViewModel,
    modifier: Modifier = Modifier
) {
    val profile by setupViewModel.profile.collectAsStateWithLifecycle()
    val calendar by setupViewModel.calendar.collectAsStateWithLifecycle()
    val selectedCount by setupViewModel.selectedCount.collectAsStateWithLifecycle()
    val pendingTaskCount = tasksViewModel.allTasks.collectAsStateWithLifecycle().value.count { !it.isCompleted }
    val syncManager = setupViewModel.syncManager
    val syncState by syncManager.syncState.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()

    // 1. Open the app -> check internet connection -> sync if available, else fall back to offline
    LaunchedEffect(Unit) {
        syncManager.syncOnAppLaunch()
    }

    // 2. React to network changes when connectivity returns
    LaunchedEffect(Unit) {
        syncManager.networkMonitor.isOnlineFlow.collect { isOnline ->
            if (isOnline && syncManager.isUserSignedIn) {
                syncManager.syncOnAppLaunch()
            }
        }
    }

    var currentScreen by rememberSaveable { mutableIntStateOf(NavDestination.HOME.ordinal) }
    var inSetupScreen by rememberSaveable { mutableStateOf(false) }

    // If onboarding is not completed or 0 subjects selected, gate to setup
    val isSetupRequired = profile?.isOnboardingCompleted != true || selectedCount == 0

    if (isSetupRequired || inSetupScreen) {
        BackHandler(enabled = !isSetupRequired && inSetupScreen) {
            inSetupScreen = false
        }
        SetupScreen(
            viewModel = setupViewModel,
            onFinish = {
                inSetupScreen = false
            },
            modifier = modifier
        )
        return
    }

    BackHandler(enabled = currentScreen != NavDestination.HOME.ordinal) {
        currentScreen = NavDestination.HOME.ordinal
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "E",
                                color = MaterialTheme.colorScheme.tertiary,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Edense",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "${calendar?.activeTermName ?: "Semester 1"} • ${calendar?.academicYear ?: "2026/27"}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.secondary,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                },
                actions = {
                    // Network & Cloud Sync Status Badge
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .padding(end = 4.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                when (syncState) {
                                    is SyncState.Syncing, is SyncState.Checking -> MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f)
                                    is SyncState.Synced -> MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f)
                                    else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                                }
                            )
                            .clickable {
                                scope.launch {
                                    syncManager.syncOnAppLaunch()
                                }
                            }
                            .padding(horizontal = 8.dp, vertical = 5.dp)
                            .testTag("sync_status_badge")
                    ) {
                        when (syncState) {
                            is SyncState.Syncing, is SyncState.Checking -> {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(12.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.tertiary
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Syncing",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.tertiary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            is SyncState.Synced -> {
                                Icon(
                                    Icons.Default.CloudDone,
                                    contentDescription = "Synced with Cloud",
                                    modifier = Modifier.size(15.dp),
                                    tint = MaterialTheme.colorScheme.secondary
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Synced",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.secondary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            is SyncState.OfflineFallback -> {
                                Icon(
                                    Icons.Default.CloudOff,
                                    contentDescription = "Offline Mode Active",
                                    modifier = Modifier.size(15.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Offline",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            else -> {
                                val online = syncManager.isOnline
                                Icon(
                                    if (online) Icons.Default.CloudQueue else Icons.Default.CloudOff,
                                    contentDescription = if (online) "Online" else "Offline",
                                    modifier = Modifier.size(15.dp),
                                    tint = if (online) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (online) "Online" else "Offline",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (online) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }

                    IconButton(
                        onClick = { inSetupScreen = true },
                        modifier = Modifier.testTag("top_settings_button")
                    ) {
                        Icon(
                            Icons.Default.Settings,
                            contentDescription = "School & Subject Settings",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                modifier = Modifier
                    .navigationBarsPadding()
                    .testTag("bottom_nav_bar")
            ) {
                NavDestination.entries.forEach { destination ->
                    val selected = currentScreen == destination.ordinal
                    NavigationBarItem(
                        selected = selected,
                        onClick = { currentScreen = destination.ordinal },
                        icon = {
                            if (destination == NavDestination.TASKS && pendingTaskCount > 0) {
                                BadgedBox(
                                    badge = {
                                        Badge {
                                            Text(pendingTaskCount.coerceAtMost(99).toString())
                                        }
                                    }
                                ) {
                                    Icon(
                                        imageVector = if (selected) destination.selectedIcon else destination.unselectedIcon,
                                        contentDescription = destination.title
                                    )
                                }
                            } else {
                                Icon(
                                    imageVector = if (selected) destination.selectedIcon else destination.unselectedIcon,
                                    contentDescription = destination.title
                                )
                            }
                        },
                        label = { Text(destination.title) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.secondaryContainer,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        modifier = Modifier.testTag(destination.tag)
                    )
                }
            }
        },
        modifier = modifier
    ) { innerPadding ->
        when (currentScreen) {
            NavDestination.HOME.ordinal -> {
                HomeScreen(
                    viewModel = homeViewModel,
                    onNavigateToTimetable = { currentScreen = NavDestination.TIMETABLE.ordinal },
                    onNavigateToSessions = { currentScreen = NavDestination.SESSIONS.ordinal },
                    modifier = Modifier.padding(innerPadding)
                )
            }
            NavDestination.TIMETABLE.ordinal -> {
                TimetableScreen(
                    viewModel = timetableViewModel,
                    modifier = Modifier.padding(innerPadding)
                )
            }
            NavDestination.SESSIONS.ordinal -> {
                SessionsScreen(
                    viewModel = sessionsViewModel,
                    modifier = Modifier.padding(innerPadding)
                )
            }
            NavDestination.TASKS.ordinal -> {
                TasksScreen(
                    viewModel = tasksViewModel,
                    modifier = Modifier.padding(innerPadding)
                )
            }
        }
    }
}
