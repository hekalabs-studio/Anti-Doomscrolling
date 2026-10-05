package com.example.dumbscrolling.ui.navigation

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.example.dumbscrolling.ui.screens.MainScreen
import com.example.dumbscrolling.ui.screens.OnboardingScreen
import com.example.dumbscrolling.ui.screens.SettingsScreen
import kotlinx.serialization.Serializable

@Serializable
data object MainRoute : NavKey

@Serializable
data object SettingsRoute : NavKey

@Serializable
data object OnboardingRoute : NavKey

@Composable
fun AppNavigation() {
    val context = LocalContext.current
    val prefs = context.getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
    val isFirstTime = prefs.getBoolean("is_first_time", true)
    
    val backStack = rememberNavBackStack(if (isFirstTime) OnboardingRoute else MainRoute)
    

    NavDisplay(
        backStack = backStack,
        onBack = { backStack.removeLastOrNull() },
        entryProvider = entryProvider {
            entry<OnboardingRoute> {
                OnboardingScreen(onFinish = {
                    prefs.edit().putBoolean("is_first_time", false).apply()
                    backStack.clear()
                    backStack.add(MainRoute)
                })
            }
            entry<MainRoute> {
                MainScreen(
                    onNavigateToSettings = {
                        backStack.add(SettingsRoute)
                    }
                )
            }
            entry<SettingsRoute> {
                SettingsScreen()
            }
        }
    )
}
