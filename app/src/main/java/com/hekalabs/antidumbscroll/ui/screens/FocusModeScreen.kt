package com.hekalabs.antidumbscroll.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.hekalabs.antidumbscroll.R
import com.hekalabs.antidumbscroll.util.TimeUtils


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
            title = { Text(stringResource(R.string.end_session_title)) },
            text = { Text(stringResource(R.string.end_session_desc)) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.endSession()
                    showEndSessionDialog = false
                }) {
                    Text(stringResource(R.string.end_session_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { showEndSessionDialog = false }) {
                    Text(stringResource(R.string.cancel_btn))
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
            text = stringResource(R.string.total_sessions_today, completedSessionsToday),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Text(
            text = stringResource(R.string.cycle_count, currentCycle, totalCycles),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 32.dp)
        )

        val phaseText = when (currentPhase) {
            PomodoroPhase.IDLE -> stringResource(R.string.phase_idle)
            PomodoroPhase.FOKUS -> stringResource(R.string.phase_fokus)
            PomodoroPhase.ISTIRAHAT_PENDEK -> stringResource(R.string.phase_istirahat_pendek)
            PomodoroPhase.ISTIRAHAT_PANJANG -> stringResource(R.string.phase_istirahat_panjang)
        }

        Text(
            text = phaseText,
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = TimeUtils.formatTime(remainingTimeMs),
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
                        Text(stringResource(R.string.continue_btn))
                    }
                } else {
                    Button(onClick = { viewModel.pauseSession() }) {
                        Text(stringResource(R.string.pause_btn))
                    }
                }
                
                Button(onClick = { viewModel.skipPhase() }) {
                    Text(stringResource(R.string.skip_phase_btn))
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = { showEndSessionDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Text(stringResource(R.string.end_btn))
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

