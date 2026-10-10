package com.hekalabs.antidumbscroll.util

object TimeUtils {
    fun formatTime(ms: Long): String {
        val totalSeconds = maxOf(0L, (ms + 999) / 1000)
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return String.format(java.util.Locale.US, "%02d:%02d", minutes, seconds)
    }
}
