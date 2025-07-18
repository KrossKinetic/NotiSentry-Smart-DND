package com.krosskinetic.notisentry.data

import androidx.room.Database
import androidx.room.RoomDatabase


@Database(entities = [AppNotifications::class, AppWhitelist::class, AppNotificationSummary::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun notificationDao(): NotificationDao
    abstract fun whitelistDao(): AppWhitelistDao
    abstract fun savedSummariesDao(): AppSummariesDao
}