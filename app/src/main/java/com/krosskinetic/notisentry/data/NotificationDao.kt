package com.krosskinetic.notisentry.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface NotificationDao {
    /**
     * Inserts a new notification into the table. If a notification with the same
     * ID already exists, it will be replaced.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(notification: AppNotifications)

    /**
     * Fetches all blocked notifications from the table, ordered by the most recent first.
     * It returns a Flow, so your UI will automatically update when the data changes.
     */
    @Query("SELECT * FROM blocked_notifications ORDER BY timestamp DESC")
    fun getAllNotifications(): Flow<List<AppNotifications>>

    /**
     * Deletes a single notification from the table by its ID.
     */
    @Query("DELETE FROM blocked_notifications WHERE id = :notificationId")
    suspend fun deleteById(notificationId: Int)

    /**
     * Deletes all notifications from the table.
     */
    @Query("DELETE FROM blocked_notifications")
    suspend fun clearAll()
}