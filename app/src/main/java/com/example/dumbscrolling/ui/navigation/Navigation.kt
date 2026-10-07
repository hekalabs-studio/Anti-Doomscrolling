package com.example.dumbscrolling.ui.navigation

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.example.dumbscrolling.ui.screens.AboutScreen
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

@Serializable
data object AboutRoute : NavKey

@Serializable
data object SplashRoute : NavKey

@Composable
fun AppNavigation() {
    val context = LocalContext.current
    val prefs = context.getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
    
    val backStack = rememberNavBackStack(SplashRoute)
    

    NavDisplay(
        backStack = backStack,
        onBack = { backStack.removeLastOrNull() },
        entryProvider = entryProvider {
            entry<SplashRoute> {
                com.example.dumbscrolling.ui.screens.SplashScreen(
                    onSplashFinished = {
                        val isFirstTime = prefs.getBoolean("is_first_time", true)
                        backStack.clear()
                        if (isFirstTime) {
                            backStack.add(OnboardingRoute)
                        } else {
                            backStack.add(MainRoute)
                        }
                    }
                )
            }
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
                SettingsScreen(
                    onNavigateBack = { backStack.removeLastOrNull() },
                    onNavigateToOnboarding = {
                        backStack.add(OnboardingRoute)
                    },
                    onNavigateToAbout = {
                        backStack.add(AboutRoute)
                    }
                )
            }
            entry<AboutRoute> {
                AboutScreen(
                    onNavigateBack = { backStack.removeLastOrNull() }
                )
            }
        }
    )
}
