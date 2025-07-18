package com.krosskinetic.notisentry.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface AppWhitelistDao {

    @Query("SELECT COUNT(*) FROM whitelist_apps WHERE packageName = :appPackage")
    suspend fun isWhitelisted(appPackage: String): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addToWhitelist(app: AppWhitelist)

    @Query("DELETE FROM whitelist_apps WHERE packageName = :appPackage")
    suspend fun removeFromWhitelist(appPackage: String)

    @Query("SELECT * FROM whitelist_apps")
    fun getAllWhitelistedApps(): Flow<List<AppWhitelist>>

    @Query("DELETE FROM whitelist_apps")
    suspend fun clearAll()
}