package com.krosskinetic.notisentry.data

import android.graphics.drawable.Drawable
import androidx.room.Entity
import androidx.room.PrimaryKey

data class AppDetails(
    val appName: String,
    val icon: Drawable,
    val packageName: String
)

@Entity(tableName = "whitelist_apps")
data class AppWhitelist(
    @PrimaryKey val packageName: String
)

@Entity(tableName = "blocked_notifications")
data class AppNotifications(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val title: String,
    val text: String,
    val timestamp: Long,
    val packageName: String,
    val appName: String,
    val messages: List<MessageInfo>,
    val conversationTitle: String,
    var parsedText: String = ""
) {
    // Using String? instead of CharSequence? for compatibility with Gson
    data class MessageInfo(val sender: String?, val text: String?, val timestamp: Long)
}

@Entity(tableName = "saved_summaries")
data class AppNotificationSummary(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val summaryText: String,
    val startTimestamp: Long,
    val endTimestamp: Long
)
