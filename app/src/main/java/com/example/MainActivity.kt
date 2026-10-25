package com.example

import android.Manifest
import android.app.AlarmManager
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.data.alarm.ReminderRescheduler
import com.example.data.local.EdenseDatabase
import com.example.data.repository.EdenseRepository
import com.example.ui.navigation.EdenseNav
import com.example.ui.screens.home.HomeViewModel
import com.example.ui.screens.home.HomeViewModelFactory
import com.example.ui.screens.sessions.SessionsViewModel
import com.example.ui.screens.sessions.SessionsViewModelFactory
import com.example.ui.screens.setup.SetupViewModel
import com.example.ui.screens.setup.SetupViewModelFactory
import com.example.ui.screens.tasks.TasksViewModel
import com.example.ui.screens.tasks.TasksViewModelFactory
import com.example.ui.theme.EdenseTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val exactAlarmAccessState = mutableStateOf(false)

    private val database by lazy { EdenseDatabase.getDatabase(this) }
    private val repository by lazy {
        EdenseRepository(database, applicationContext, syncManager::requestSync)
    }
    private val authManager by lazy { com.example.data.auth.AuthManager(applicationContext) }
    private val syncManager by lazy { com.example.data.remote.FirebaseSyncManager(applicationContext) }

    private val homeViewModel: HomeViewModel by viewModels { HomeViewModelFactory(repository) }
    private val timetableViewModel: com.example.ui.screens.timetable.TimetableViewModel by viewModels {
        com.example.ui.screens.timetable.TimetableViewModelFactory(repository)
    }
    private val sessionsViewModel: SessionsViewModel by viewModels { SessionsViewModelFactory(repository) }
    private val tasksViewModel: TasksViewModel by viewModels { TasksViewModelFactory(repository) }
    private val setupViewModel: SetupViewModel by viewModels {
        SetupViewModelFactory(repository, authManager, syncManager)
    }

    override fun onResume() {
        super.onResume()
        exactAlarmAccessState.value = Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
            (getSystemService(ALARM_SERVICE) as AlarmManager).canScheduleExactAlarms()
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                ReminderRescheduler.rescheduleAll(applicationContext)
            } catch (error: Exception) {
                android.util.Log.e("MainActivity", "Unable to restore scheduled reminders.", error)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        exactAlarmAccessState.value = Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
            (getSystemService(ALARM_SERVICE) as AlarmManager).canScheduleExactAlarms()
        setContent {
            EdenseTheme {
                var dismissedExactAlarmPrompt by remember { mutableStateOf(false) }

                // Request Notification Permission on Android 13+
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    val permissionLauncher = rememberLauncherForActivityResult(
                        contract = ActivityResultContracts.RequestPermission()
                    ) { /* Handled gracefully */ }

                    LaunchedEffect(Unit) {
                        val hasPermission = ContextCompat.checkSelfPermission(
                            this@MainActivity,
                            Manifest.permission.POST_NOTIFICATIONS
                        ) == PackageManager.PERMISSION_GRANTED
                        if (!hasPermission) {
                            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    }
                }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    EdenseNav(
                        homeViewModel = homeViewModel,
                        timetableViewModel = timetableViewModel,
                        sessionsViewModel = sessionsViewModel,
                        tasksViewModel = tasksViewModel,
                        setupViewModel = setupViewModel
                    )
                }

                if (!exactAlarmAccessState.value && !dismissedExactAlarmPrompt) {
                    AlertDialog(
                        onDismissRequest = { dismissedExactAlarmPrompt = true },
                        title = { Text("Enable on-time reminders") },
                        text = {
                            Text(
                                "Allow Edense to schedule exact alarms so class and task " +
                                    "reminders can appear at their selected times, even when the app is closed."
                            )
                        },
                        confirmButton = {
                            TextButton(
                                onClick = {
                                    dismissedExactAlarmPrompt = true
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                        startActivity(
                                            Intent(
                                                Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                                                Uri.parse("package:$packageName")
                                            )
                                        )
                                    }
                                }
                            ) {
                                Text("Enable")
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { dismissedExactAlarmPrompt = true }) {
                                Text("Not now")
                            }
                        }
                    )
                }

            }
        }
    }
}
