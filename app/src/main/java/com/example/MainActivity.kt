package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
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

class MainActivity : ComponentActivity() {

    private val database by lazy { EdenseDatabase.getDatabase(this) }
    private val repository by lazy { EdenseRepository(database, applicationContext) }
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            EdenseTheme {
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
            }
        }
    }
}
