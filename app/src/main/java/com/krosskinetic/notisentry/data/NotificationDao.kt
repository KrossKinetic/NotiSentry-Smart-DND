package com.krosskinetic.notisentry.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface NotificationDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(notification: AppNotifications)

    @Query("SELECT * FROM blocked_notifications ORDER BY timestamp DESC")
    fun getAllNotifications(): Flow<List<AppNotifications>>

    @Query("DELETE FROM blocked_notifications WHERE id = :notificationId")
    suspend fun deleteById(notificationId: Int)

    @Query("DELETE FROM blocked_notifications")
    suspend fun clearAll()

    @Query("DELETE FROM blocked_notifications WHERE timestamp <= :timestamp")
    suspend fun deleteSavedNotifications(timestamp: Long)

    @Query("DELETE FROM blocked_notifications WHERE timestamp >= :startTimestamp AND timestamp <= :endTimestamp")
    suspend fun deleteSavedNotifications(startTimestamp: Long, endTimestamp: Long)
}