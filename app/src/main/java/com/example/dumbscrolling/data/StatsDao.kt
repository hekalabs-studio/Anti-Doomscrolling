package com.example.dumbscrolling.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface StatsDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(stat: DailyStat)

    @Query("SELECT * FROM daily_stats ORDER BY dateStr DESC LIMIT 7")
    suspend fun getStatsForLast7Days(): List<DailyStat>

    @Query("SELECT * FROM daily_stats WHERE dateStr = :dateStr")
    suspend fun getStatForDate(dateStr: String): DailyStat?

    @Query("DELETE FROM daily_stats")
    suspend fun deleteAllStats()
}