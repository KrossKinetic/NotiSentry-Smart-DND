package com.krosskinetic.notisentry.data

import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationRepository @Inject constructor(private val notificationDao: NotificationDao, private val blacklistDao: AppWhitelistDao, private val summaryDao: AppSummariesDao) {
    // List of blocked notifications that acts as a bridge between service and view model using hilt

    val blockedNotificationsFlow: Flow<List<AppNotifications>> = notificationDao.getAllNotifications()
    val blackListedAppsFlow: Flow<List<AppBlacklist>> = blacklistDao.getAllBlacklistedApps()
    val savedSummariesFlow: Flow<List<AppNotificationSummary>> = summaryDao.getAllSavedSummaries()

    suspend fun isAppBlacklisted(packageName: String): Boolean {
        // The DAO function returns the number of rows that match.
        // If the count is greater than 0, then the app is on the list.
        return blacklistDao.isBlacklisted(packageName) > 0
    }
    suspend fun addBlockedNotification(notification: AppNotifications) {
        notificationDao.insert(notification)
    }

    suspend fun addToBlacklist(packageName: String) {
        blacklistDao.addToBlacklist(AppBlacklist(packageName))
    }

    // Function to remove an app from the whitelist
    suspend fun removeFromBlacklist(packageName: String) {
        blacklistDao.removeFromBlacklist(packageName)
    }

    suspend fun addToSavedSummary(summaryText: String, startTimestamp: Long, endTimestamp: Long) {
        summaryDao.addToSummaries(AppNotificationSummary(summaryText = summaryText, startTimestamp = startTimestamp, endTimestamp = endTimestamp))
    }

    suspend fun deleteOldSummaries(timestamp: Long) {
        summaryDao.deleteSavedSummaries(timestamp)
    }

    suspend fun deleteOldNotifications(timestamp: Long) {
        notificationDao.deleteSavedNotifications(timestamp)
    }

    suspend fun deleteSummaryWithNotification(summaryId: Int) {
        val summary = summaryDao.getFromSavedSummaries(summaryId)
        notificationDao.deleteSavedNotifications(summary.startTimestamp, summary.endTimestamp)
        summaryDao.removeFromSavedSummaries(summaryId)
    }

}