package com.example.dumbscrolling.ui.screens

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = viewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()

    if (uiState.showResetDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.hideResetDialog() },
            title = { Text("Reset Data") },
            text = { Text("Are you sure you want to reset all usage statistics and focus sessions? This action cannot be undone.") },
            confirmButton = {
                TextButton(onClick = { viewModel.resetData() }) {
                    Text("Reset")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.hideResetDialog() }) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            item {
                Text(
                    text = "Settings",
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.padding(bottom = 24.dp)
                )
            }

            item {
                Button(
                    onClick = { context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) },
                    modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)
                ) {
                    Text("Enable Tracking Service")
                }
            }

            item {
                Text(
                    text = "Session Limit: ${uiState.sessionLimitMinutes} minutes",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Slider(
                    value = uiState.sessionLimitMinutes.toFloat(),
                    onValueChange = { viewModel.updateSessionLimit(it.toInt()) },
                    valueRange = 1f..60f,
                    steps = 58,
                    modifier = Modifier.padding(bottom = 24.dp)
                )
            }

            item {
                Text(
                    text = "Monitored Apps",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            items(uiState.monitoredApps) { app ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = app.name, style = MaterialTheme.typography.bodyLarge)
                    Switch(
                        checked = app.isMonitored,
                        onCheckedChange = { viewModel.toggleAppMonitoring(app.packageName, it) }
                    )
                }
            }
            
            item {
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = { viewModel.showResetDialog() },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Reset Data")
                }
            }
        }
    }
}
