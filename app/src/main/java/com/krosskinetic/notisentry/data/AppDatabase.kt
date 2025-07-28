package com.krosskinetic.notisentry.data

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters


@Database(entities = [AppNotifications::class, AppBlacklist::class, AppNotificationSummary::class], version = 3, exportSchema = false)
@TypeConverters(Converters::class) // Used to convert List<> inside NotificationDao to gson and back
abstract class AppDatabase : RoomDatabase() {
    abstract fun notificationDao(): NotificationDao
    abstract fun whitelistDao(): AppWhitelistDao
    abstract fun savedSummariesDao(): AppSummariesDao
}