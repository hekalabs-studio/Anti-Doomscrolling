package com.hekalabs.antidumbscroll.di

import android.content.Context
import android.content.SharedPreferences
import androidx.room.Room
import com.hekalabs.antidumbscroll.data.AppDatabase
import com.hekalabs.antidumbscroll.data.StatsDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import javax.inject.Named

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "anti_dumbscroll_db"
        ).build()
    }

    @Provides
    fun provideStatsDao(database: AppDatabase): StatsDao {
        return database.statsDao()
    }



    @Provides
    @Singleton
    @Named("FocusPrefs")
    fun provideFocusPreferences(@ApplicationContext context: Context): SharedPreferences {
        return context.getSharedPreferences("FocusModePrefs", Context.MODE_PRIVATE)
    }

    @Provides
    @Singleton
    @Named("UsagePrefs")
    fun provideUsagePreferences(@ApplicationContext context: Context): SharedPreferences {
        return context.getSharedPreferences("UsageStatsPrefs", Context.MODE_PRIVATE)
    }
}