package com.example.dumbscrolling.ui.screens

import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.text.TextUtils
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.dumbscrolling.services.AppTrackingService
import kotlinx.coroutines.launch

@Composable
fun OnboardingScreen(onFinish: () -> Unit) {
    val pagerState = rememberPagerState(pageCount = { 4 })
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(modifier = Modifier.fillMaxSize()) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) { page ->
                when (page) {
                    0 -> OnboardingPage(
                        title = "Selamat Datang di Dumbscrolling",
                        description = "Aplikasi ini membantumu mengurangi kebiasaan doomscrolling di media sosial agar hidupmu lebih produktif."
                    )
                    1 -> OnboardingPage(
                        title = "Cara Kerja",
                        description = "Tetapkan batas waktu harian untuk aplikasi tertentu. Aktifkan Focus Mode saat kamu butuh konsentrasi tanpa gangguan."
                    )
                    2 -> AccessibilityPermissionPage()
                    3 -> FinalPage(onFinish = onFinish)
                }
            }

            // Pager Indicator & Navigation
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (pagerState.currentPage > 0) {
                    TextButton(onClick = {
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(pagerState.currentPage - 1)
                        }
                    }) {
                        Text("Kembali")
                    }
                } else {
                    Spacer(modifier = Modifier.width(8.dp))
                }

                Row(horizontalArrangement = Arrangement.Center) {
                    repeat(4) { iteration ->
                        val color = if (pagerState.currentPage == iteration) 
                            MaterialTheme.colorScheme.primary 
                        else 
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
                        
                        Box(
                            modifier = Modifier
                                .padding(4.dp)
                                .size(8.dp)
                                .background(color, shape = androidx.compose.foundation.shape.CircleShape)
                        )
                    }
                }

                if (pagerState.currentPage < 3) {
                    TextButton(onClick = {
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(pagerState.currentPage + 1)
                        }
                    }) {
                        Text("Lanjut")
                    }
                } else {
                    Spacer(modifier = Modifier.width(8.dp))
                }
            }
        }
    }
}

@Composable
fun OnboardingPage(title: String, description: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        Text(
            text = description,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun AccessibilityPermissionPage() {
    val context = LocalContext.current
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Izin Aksesibilitas",
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        Text(
            text = "Dumbscrolling memerlukan izin aksesibilitas (Accessibility) untuk mendeteksi kapan aplikasi media sosial sedang dibuka, sehingga kami bisa menghitung waktu dan memblokirnya.",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(bottom = 32.dp)
        )
        Button(onClick = {
            context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }) {
            Text("Buka Pengaturan Aksesibilitas")
        }
    }
}

@Composable
fun FinalPage(onFinish: () -> Unit) {
    val context = LocalContext.current
    
    // Lifecycle trick to check permission repeatedly when coming back from settings
    var hasPermission by remember { mutableStateOf(isAccessibilityServiceEnabled(context)) }
    
    DisposableEffect(Unit) {
        // We could use a more robust way to check when resuming, but for now we'll just check on compose
        val interval = 1000L
        val handler = android.os.Handler(android.os.Looper.getMainLooper())
        val runnable = object : Runnable {
            override fun run() {
                val currentPermission = isAccessibilityServiceEnabled(context)
                if (currentPermission != hasPermission) {
                    hasPermission = currentPermission
                }
                handler.postDelayed(this, interval)
            }
        }
        handler.post(runnable)
        
        onDispose {
            handler.removeCallbacks(runnable)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Siap Digunakan!",
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        if (hasPermission) {
            Text(
                text = "Hebat! Izin aksesibilitas sudah aktif.",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 32.dp)
            )
            Button(onClick = onFinish) {
                Text("Buka Aplikasi")
            }
        } else {
            Text(
                text = "Menunggu izin aksesibilitas. Harap berikan izin terlebih dahulu agar aplikasi dapat bekerja.",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(bottom = 32.dp)
            )
            Button(onClick = {
                context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
            }) {
                Text("Buka Pengaturan Aksesibilitas")
            }
        }
    }
}

// Helper to check if our Accessibility Service is enabled
fun isAccessibilityServiceEnabled(context: Context): Boolean {
    var accessibilityEnabled = 0
    val service = "${context.packageName}/${AppTrackingService::class.java.canonicalName}"
    try {
        accessibilityEnabled = Settings.Secure.getInt(
            context.applicationContext.contentResolver,
            Settings.Secure.ACCESSIBILITY_ENABLED
        )
    } catch (e: Settings.SettingNotFoundException) {
        // Ignored
    }

    if (accessibilityEnabled == 1) {
        val settingValue = Settings.Secure.getString(
            context.applicationContext.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        )
        if (settingValue != null) {
            val stringColonSplitter = TextUtils.SimpleStringSplitter(':')
            stringColonSplitter.setString(settingValue)
            while (stringColonSplitter.hasNext()) {
                val accessibilityService = stringColonSplitter.next()
                if (accessibilityService.equals(service, ignoreCase = true)) {
                    return true
                }
            }
        }
    }
    return false
}
