package com.krosskinetic.notisentry

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import androidx.lifecycle.viewmodel.compose.viewModel
import com.krosskinetic.notisentry.data.AppNotifications
import com.krosskinetic.notisentry.data.NotificationRepository
import com.krosskinetic.notisentry.data.SettingsRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class NotiSentryService : NotificationListenerService() {

    @Inject
    lateinit var repository: NotificationRepository
    lateinit var settingsRepository: SettingsRepository
    private val tag = "NotiSentryService"

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        if (sbn != null) {
            val packageName = sbn.packageName

            serviceScope.launch {
                if (!repository.isAppWhitelisted(packageName) && settingsRepository.startServiceFlow.first()) {
                cancelNotification(sbn.key)
                    val notificationText = sbn.notification.extras.getString(Notification.EXTRA_TEXT)
                    val notificationTitle = sbn.notification.extras.getString(Notification.EXTRA_TITLE)

                    val appNotification = AppNotifications(
                        title = notificationTitle ?: "",
                        text = notificationText ?: "",
                        packageName = packageName,
                        timestamp = System.currentTimeMillis()
                    )

                    repository.addBlockedNotification(appNotification)
                    Log.d(tag, "Blocked and saved notification from: $packageName")
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }
}
