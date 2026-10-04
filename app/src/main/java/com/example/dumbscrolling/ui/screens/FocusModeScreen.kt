package com.example.dumbscrolling.ui.screens

import android.app.Application
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FocusModeScreen() {
    val context = LocalContext.current
    val application = context.applicationContext as Application
    val viewModel: FocusModeViewModel = viewModel(
        factory = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return FocusModeViewModel(application) as T
            }
        }
    )

    val isFocusModeActive by viewModel.isFocusModeActive.collectAsState()
    val remainingTimeMs by viewModel.remainingTimeMs.collectAsState()
    val completedSessions by viewModel.completedSessions.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Focus Mode") })
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Completed Sessions: $completedSessions",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 32.dp)
            )

            if (isFocusModeActive) {
                Text(
                    text = "Focus Mode Active",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = formatTime(remainingTimeMs),
                    style = MaterialTheme.typography.displayLarge,
                    fontWeight = FontWeight.Light
                )
                
                Spacer(modifier = Modifier.height(32.dp))
                
                Button(
                    onClick = { viewModel.stopFocusMode() },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Stop Early")
                }
            } else {
                var durationStr by remember { mutableStateOf("25") }
                
                Text(
                    text = "Set Timer (minutes)",
                    style = MaterialTheme.typography.bodyLarge
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                OutlinedTextField(
                    value = durationStr,
                    onValueChange = { if (it.all { char -> char.isDigit() }) durationStr = it },
                    singleLine = true,
                    modifier = Modifier.width(100.dp)
                )
                
                Spacer(modifier = Modifier.height(32.dp))
                
                Button(
                    onClick = {
                        val duration = durationStr.toIntOrNull() ?: 25
                        if (duration > 0) {
                            viewModel.startFocusMode(duration)
                        }
                    }
                ) {
                    Text("Start Focus Session")
                }
            }
        }
    }
}

private fun formatTime(ms: Long): String {
    val minutes = TimeUnit.MILLISECONDS.toMinutes(ms)
    val seconds = TimeUnit.MILLISECONDS.toSeconds(ms) % 60
    return String.format("%02d:%02d", minutes, seconds)
}
