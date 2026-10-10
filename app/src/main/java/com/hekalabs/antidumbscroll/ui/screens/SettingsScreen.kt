package com.hekalabs.antidumbscroll.ui.screens

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.hekalabs.antidumbscroll.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit = {},
    onNavigateToOnboarding: () -> Unit = {},
    onNavigateToAbout: () -> Unit = {},
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()

    if (uiState.showResetDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.hideResetDialog() },
            title = { Text(stringResource(R.string.reset_data_btn)) },
            text = { Text(stringResource(R.string.reset_data_desc)) },
            confirmButton = {
                TextButton(onClick = { viewModel.resetData() }) {
                    Text(stringResource(R.string.reset_btn))
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.hideResetDialog() }) {
                    Text(stringResource(R.string.cancel_btn))
                }
            }
        )
    }

    if (uiState.showAppPicker) {
        AppPickerDialog(
            apps = uiState.monitoredApps,
            onDismiss = { viewModel.hideAppPicker() },
            onToggleApp = { pkg, isMonitored -> viewModel.toggleAppMonitoring(pkg, isMonitored) }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back_btn_desc)
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            item {
                Button(
                    onClick = { context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) },
                    modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)
                ) {
                    Text(stringResource(R.string.enable_tracking_service))
                }
            }

            // Language Settings Section
            item {
                var showLanguageMenu by remember { mutableStateOf(false) }
                val currentLanguageTag = AppCompatDelegate.getApplicationLocales().toLanguageTags()
                val currentLanguageName = when {
                    currentLanguageTag.contains("id") -> "Bahasa Indonesia"
                    currentLanguageTag.contains("ru") -> "Русский"
                    currentLanguageTag.contains("zh") -> "中文"
                    else -> "English"
                }

                fun changeLanguage(languageTag: String) {
                    AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(languageTag))
                    if (context is android.app.Activity) {
                        context.recreate()
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.language_setting),
                        style = MaterialTheme.typography.titleMedium
                    )
                    Box {
                        TextButton(onClick = { showLanguageMenu = true }) {
                            Text(currentLanguageName)
                        }
                        DropdownMenu(
                            expanded = showLanguageMenu,
                            onDismissRequest = { showLanguageMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("English") },
                                onClick = {
                                    showLanguageMenu = false
                                    changeLanguage("en")
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Bahasa Indonesia") },
                                onClick = {
                                    showLanguageMenu = false
                                    changeLanguage("id")
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Русский") },
                                onClick = {
                                    showLanguageMenu = false
                                    changeLanguage("ru")
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("中文") },
                                onClick = {
                                    showLanguageMenu = false
                                    changeLanguage("zh")
                                }
                            )
                        }
                    }
                }
            }

            // Pomodoro Settings Section
            item {
                Text(
                    text = stringResource(R.string.setting_pomodoro_title),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 16.dp),
                    color = MaterialTheme.colorScheme.primary
                )
            }
            
            item {
                Text(text = stringResource(R.string.setting_focus_duration, uiState.focusDuration))
                Slider(
                    value = uiState.focusDuration.toFloat(),
                    onValueChange = { viewModel.updateFocusDuration(it.toInt()) },
                    valueRange = 5f..120f,
                    steps = 114
                )
                
                Text(text = stringResource(R.string.setting_short_break, uiState.shortBreakDuration))
                Slider(
                    value = uiState.shortBreakDuration.toFloat(),
                    onValueChange = { viewModel.updateShortBreakDuration(it.toInt()) },
                    valueRange = 1f..30f,
                    steps = 28
                )

                Text(text = stringResource(R.string.setting_long_break, uiState.longBreakDuration))
                Slider(
                    value = uiState.longBreakDuration.toFloat(),
                    onValueChange = { viewModel.updateLongBreakDuration(it.toInt()) },
                    valueRange = 5f..60f,
                    steps = 54
                )

                Text(text = stringResource(R.string.setting_long_break_cycle, uiState.longBreakCycle))
                Slider(
                    value = uiState.longBreakCycle.toFloat(),
                    onValueChange = { viewModel.updateLongBreakCycle(it.toInt()) },
                    valueRange = 1f..10f,
                    steps = 8
                )

                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(stringResource(R.string.setting_auto_start_phase))
                    Switch(
                        checked = uiState.autoStartNextPhase,
                        onCheckedChange = { viewModel.updateAutoStartNextPhase(it) }
                    )
                }

                Button(
                    onClick = { viewModel.restoreDefaultPomodoroSettings() },
                    modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)
                ) {
                    Text(stringResource(R.string.setting_restore_default))
                }
                
                HorizontalDivider(modifier = Modifier.padding(bottom = 24.dp))
            }
            
            item {
                Text(
                    text = stringResource(R.string.setting_schedule_title),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 4.dp),
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = stringResource(R.string.setting_schedule_desc),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(stringResource(R.string.setting_schedule_title))
                    Switch(
                        checked = uiState.isScheduleEnabled,
                        onCheckedChange = { viewModel.updateScheduleEnabled(it) }
                    )
                }
                
                if (uiState.isScheduleEnabled) {
                    val context = LocalContext.current
                    
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(stringResource(R.string.setting_start_time))
                        TextButton(onClick = {
                            android.app.TimePickerDialog(
                                context,
                                { _, hour, minute -> viewModel.updateScheduleStartTime(hour, minute) },
                                uiState.scheduleStartHour,
                                uiState.scheduleStartMinute,
                                true
                            ).show()
                        }) {
                            Text(String.format(java.util.Locale.US, "%02d:%02d", uiState.scheduleStartHour, uiState.scheduleStartMinute))
                        }
                    }
                    
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(stringResource(R.string.setting_end_time))
                        TextButton(onClick = {
                            android.app.TimePickerDialog(
                                context,
                                { _, hour, minute -> viewModel.updateScheduleEndTime(hour, minute) },
                                uiState.scheduleEndHour,
                                uiState.scheduleEndMinute,
                                true
                            ).show()
                        }) {
                            Text(String.format(java.util.Locale.US, "%02d:%02d", uiState.scheduleEndHour, uiState.scheduleEndMinute))
                        }
                    }
                }
                
                HorizontalDivider(modifier = Modifier.padding(vertical = 24.dp))
            }

            item {
                Text(
                    text = stringResource(R.string.setting_session_limit, uiState.sessionLimitMinutes),
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
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.setting_monitored_apps),
                        style = MaterialTheme.typography.titleMedium
                    )
                    IconButton(onClick = { viewModel.showAppPicker() }) {
                        Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.add_app_btn))
                    }
                }
            }

            val activeMonitoredApps = uiState.monitoredApps.filter { it.isMonitored }
            
            if (activeMonitoredApps.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = stringResource(R.string.empty_monitored_apps_desc),
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                        Button(onClick = { viewModel.showAppPicker() }) {
                            Text(stringResource(R.string.add_app_btn))
                        }
                    }
                }
            } else {
                items(activeMonitoredApps) { app ->
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
            }
            
            item {
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = onNavigateToOnboarding,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.setting_view_tutorial_again))
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = { viewModel.showResetDialog() },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.reset_data_btn))
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                OutlinedButton(
                    onClick = onNavigateToAbout,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.about_title))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppPickerDialog(
    apps: List<AppItem>,
    onDismiss: () -> Unit,
    onToggleApp: (String, Boolean) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val filteredApps = apps.filter { it.name.contains(searchQuery, ignoreCase = true) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                Text(
                    text = stringResource(R.string.app_picker_title),
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text(stringResource(R.string.search_app_hint)) },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                    singleLine = true
                )

                LazyColumn(
                    modifier = Modifier.weight(1f)
                ) {
                    items(filteredApps) { app ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = app.name, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                            Switch(
                                checked = app.isMonitored,
                                onCheckedChange = { onToggleApp(app.packageName, it) }
                            )
                        }
                    }
                }
                
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.End).padding(top = 16.dp)
                ) {
                    Text(stringResource(R.string.done_btn))
                }
            }
        }
    }
}
