package com.krosskinetic.notisentry.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface AppWhitelistDao {

    @Query("SELECT COUNT(*) FROM blacklist_apps WHERE packageName = :appPackage")
    suspend fun isBlacklisted(appPackage: String): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addToBlacklist(app: AppBlacklist)

    @Query("DELETE FROM blacklist_apps WHERE packageName = :appPackage")
    suspend fun removeFromBlacklist(appPackage: String)

    @Query("SELECT * FROM blacklist_apps")
    fun getAllBlacklistedApps(): Flow<List<AppBlacklist>>

    @Query("DELETE FROM blacklist_apps")
    suspend fun clearAll()
}