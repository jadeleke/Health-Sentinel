package com.healthsentinel.data

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface DailyHealthDao {
    @Query("SELECT * FROM daily_health_summaries ORDER BY date DESC LIMIT 1")
    fun observeLatest(): Flow<DailyHealthSummary?>

    @Query("SELECT * FROM daily_health_summaries ORDER BY date DESC LIMIT :limit")
    fun observeRecent(limit: Int = 30): Flow<List<DailyHealthSummary>>

    @Query("SELECT * FROM daily_health_summaries ORDER BY date ASC")
    suspend fun getAllAscending(): List<DailyHealthSummary>

    @Query("SELECT * FROM daily_health_summaries ORDER BY date DESC LIMIT :limit")
    suspend fun getRecent(limit: Int = 30): List<DailyHealthSummary>

    @Query("SELECT * FROM daily_health_summaries WHERE date = :date LIMIT 1")
    suspend fun getByDate(date: String): DailyHealthSummary?

    @Upsert
    suspend fun upsert(summary: DailyHealthSummary)

    @Upsert
    suspend fun upsertAll(summaries: List<DailyHealthSummary>)
}
