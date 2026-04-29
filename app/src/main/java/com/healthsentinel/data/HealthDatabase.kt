package com.healthsentinel.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [DailyHealthSummary::class], version = 1, exportSchema = false)
abstract class HealthDatabase : RoomDatabase() {
    abstract fun dailyHealthDao(): DailyHealthDao

    companion object {
        @Volatile private var instance: HealthDatabase? = null

        fun get(context: Context): HealthDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    HealthDatabase::class.java,
                    "health_sentinel.db"
                ).build().also { instance = it }
            }
    }
}
