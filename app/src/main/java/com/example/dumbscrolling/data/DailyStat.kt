package com.example.dumbscrolling.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "daily_stats")
data class DailyStat(
    @PrimaryKey val dateStr: String, // Format "yyyy-MM-dd"
    val totalDurationMs: Long = 0L
)