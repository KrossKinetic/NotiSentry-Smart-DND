package com.krosskinetic.notisentry.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface AppSummariesDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addToSummaries(summary: AppNotificationSummary)

    @Query("DELETE FROM saved_summaries WHERE id = :summaryId")
    suspend fun removeFromSavedSummaries(summaryId: Int)

    @Query("DELETE FROM saved_summaries WHERE endTimestamp < :timestamp")
    suspend fun deleteSavedSummaries(timestamp: Long)

    @Query("SELECT * FROM saved_summaries ORDER BY startTimestamp DESC")
    fun getAllSavedSummaries(): Flow<List<AppNotificationSummary>>

    @Query("DELETE FROM saved_summaries")
    suspend fun clearAll()
}