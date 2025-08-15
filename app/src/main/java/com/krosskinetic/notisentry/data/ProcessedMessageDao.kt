package com.krosskinetic.notisentry.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface ProcessedMessageDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(message: ProcessedMessage)

    @Query("SELECT COUNT(*) > 0 FROM processed_messages WHERE messageId = :messageId")
    suspend fun messageExists(messageId: String): Boolean

    @Query("DELETE FROM processed_messages")
    suspend fun clearAll()
}