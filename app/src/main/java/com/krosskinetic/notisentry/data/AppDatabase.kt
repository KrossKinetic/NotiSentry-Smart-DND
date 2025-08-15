package com.krosskinetic.notisentry.data

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters


@Database(entities = [ProcessedMessage::class, AppNotifications::class, AppBlacklist::class, AppNotificationSummary::class], version = 4, exportSchema = false)
@TypeConverters(Converters::class) // Used to convert List<> inside NotificationDao to gson and back
abstract class AppDatabase : RoomDatabase() {
    abstract fun notificationDao(): NotificationDao
    abstract fun blacklistDao(): AppBlacklistDao
    abstract fun savedSummariesDao(): AppSummariesDao

    abstract fun processedMessageDao(): ProcessedMessageDao
}