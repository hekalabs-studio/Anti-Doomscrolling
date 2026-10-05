package com.example.dumbscrolling.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.dumbscrolling.R


import androidx.compose.ui.text.style.TextDecoration

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FocusModeScreen(
    viewModel: FocusModeViewModel,
    onNavigateToSettings: () -> Unit
) {
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
                Text(stringResource(R.string.start_focus))
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
        
        Spacer(modifier = Modifier.height(16.dp))
        TextButton(onClick = onNavigateToSettings) {
            Text(
                text = stringResource(R.string.change_in_settings),
                style = MaterialTheme.typography.bodySmall,
                textDecoration = TextDecoration.Underline
            )
        }
    }
}

private fun formatTime(ms: Long): String {
    val totalSeconds = maxOf(0L, (ms + 999) / 1000)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%02d:%02d", minutes, seconds)
}
