package com.krosskinetic.notisentry.data

import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationRepository @Inject constructor(private val notificationDao: NotificationDao, private val whitelistDao: AppWhitelistDao, private val summaryDao: AppSummariesDao) {
    // List of blocked notifications that acts as a bridge between service and view model using hilt

    val blockedNotificationsFlow: Flow<List<AppNotifications>> = notificationDao.getAllNotifications()
    val whiteListedAppsFlow: Flow<List<AppWhitelist>> = whitelistDao.getAllWhitelistedApps()
    val savedSummariesFlow: Flow<List<AppNotificationSummary>> = summaryDao.getAllSavedSummaries()

    suspend fun isAppWhitelisted(packageName: String): Boolean {
        // The DAO function returns the number of rows that match.
        // If the count is greater than 0, then the app is on the list.
        return whitelistDao.isWhitelisted(packageName) > 0
    }
    suspend fun addBlockedNotification(notification: AppNotifications) {
        notificationDao.insert(notification)
    }

    suspend fun addToWhitelist(packageName: String) {
        whitelistDao.addToWhitelist(AppWhitelist(packageName))
    }

    // Function to remove an app from the whitelist
    suspend fun removeFromWhitelist(packageName: String) {
        whitelistDao.removeFromWhitelist(packageName)
    }

    suspend fun addToSavedSummary(summaryText: String, startTimestamp: Long, endTimestamp: Long) {
        summaryDao.addToSummaries(AppNotificationSummary(summaryText = summaryText, startTimestamp = startTimestamp, endTimestamp = endTimestamp))
    }
}