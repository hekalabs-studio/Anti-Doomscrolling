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

    val currentPhase by viewModel.currentPhase.collectAsState()
    val remainingTimeMs by viewModel.remainingTimeMs.collectAsState()
    val completedSessionsToday by viewModel.completedSessionsToday.collectAsState()
    val currentCycle by viewModel.currentCycle.collectAsState()
    val isPaused by viewModel.isPaused.collectAsState()
    val totalCycles = viewModel.totalCycles

    var showEndSessionDialog by remember { mutableStateOf(false) }

    if (showEndSessionDialog) {
        AlertDialog(
            onDismissRequest = { showEndSessionDialog = false },
            title = { Text("Akhiri Sesi?") },
            text = { Text("Sesi fokus akan dihentikan dan progres saat ini akan hilang.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.endSession()
                    showEndSessionDialog = false
                }) {
                    Text("Ya, Akhiri")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEndSessionDialog = false }) {
                    Text("Batal")
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Total Sesi Hari Ini: $completedSessionsToday",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Text(
            text = "Siklus $currentCycle dari $totalCycles",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 32.dp)
        )

        val phaseText = when (currentPhase) {
            PomodoroPhase.IDLE -> "Siap untuk Fokus"
            PomodoroPhase.FOKUS -> "Fokus"
            PomodoroPhase.ISTIRAHAT_PENDEK -> "Istirahat Pendek"
            PomodoroPhase.ISTIRAHAT_PANJANG -> "Istirahat Panjang"
        }

        Text(
            text = phaseText,
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
        
        if (currentPhase == PomodoroPhase.IDLE) {
            Button(onClick = { viewModel.startSession() }) {
                Text("Mulai")
            }
        } else {
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (isPaused) {
                    Button(onClick = { viewModel.startSession() }) {
                        Text("Lanjut")
                    }
                } else {
                    Button(onClick = { viewModel.pauseSession() }) {
                        Text("Jeda")
                    }
                }
                
                Button(onClick = { viewModel.skipPhase() }) {
                    Text("Lewati fase")
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = { showEndSessionDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Text("Akhiri sesi")
            }
        }
    }
}

private fun formatTime(ms: Long): String {
    val minutes = TimeUnit.MILLISECONDS.toMinutes(ms)
    val seconds = TimeUnit.MILLISECONDS.toSeconds(ms) % 60
    return String.format("%02d:%02d", minutes, seconds)
}
