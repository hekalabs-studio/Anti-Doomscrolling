package com.hekalabs.antidumbscroll.data

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [DailyStat::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun statsDao(): StatsDao
}