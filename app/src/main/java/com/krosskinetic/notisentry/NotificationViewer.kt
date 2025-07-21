package com.krosskinetic.notisentry

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import com.krosskinetic.notisentry.data.AppNotifications
import com.krosskinetic.notisentry.data.NotiSentryEntryPoint
import com.krosskinetic.notisentry.data.NotificationRepository
import com.krosskinetic.notisentry.data.SettingsRepository
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class NotiSentryService : NotificationListenerService() {

    private lateinit var repository: NotificationRepository
    private lateinit var settingsRepository: SettingsRepository

    private val tag = "NotiSentryService"
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // This ensures service is initialized AFTER repository is initialized
    override fun onCreate() {
        super.onCreate()
        val entryPoint = EntryPointAccessors.fromApplication(
            applicationContext,
            NotiSentryEntryPoint::class.java
        )
        repository = entryPoint.notificationRepository()
        settingsRepository = entryPoint.settingsRepository()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        if (sbn != null) {
            val packageName = sbn.packageName

            if (packageName == "com.krosskinetic.notisentry" || sbn.isOngoing) {
                return
            }

            serviceScope.launch {
                if (!repository.isAppWhitelisted(packageName) && settingsRepository.startServiceFlow.first()) {
                    val notificationText = sbn.notification.extras.getString(Notification.EXTRA_TEXT)?: ""
                    val notificationTitle = sbn.notification.extras.getString(Notification.EXTRA_TITLE)?: ""

                    cancelNotification(sbn.key)


                    val allow = allowNotification(context = this@NotiSentryService,
                        "\n<Notification>\n Notification Package Name: $packageName\nNotification Title: $notificationTitle\nNotification Text: $notificationText \n</Notification>\n"
                    )

                    if (allow){
                        postNotification(this@NotiSentryService, AppNotifications(
                            title = notificationTitle,
                            text = notificationText,
                            packageName = packageName,
                            timestamp = System.currentTimeMillis()
                        ))
                        Log.d(tag, "Allowed Notification using Gemini-2.5-Flash from: $packageName")
                    } else {
                        val appNotification = AppNotifications(
                            title = notificationTitle,
                            text = notificationText,
                            packageName = packageName,
                            timestamp = System.currentTimeMillis()
                        )

                        repository.addBlockedNotification(appNotification)
                        Log.d(tag, "Blocked and saved notification from: $packageName")
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }
}

suspend fun allowNotification(context: Context, notificationText: String): Boolean {
    return withContext(Dispatchers.IO) {
        val userInstruction = "Can you allow Notifications that are from my Clock app please?"

        try {

            val prompt = "Return a simple 'Allow' or 'Don't Allow' based on the following user " +
                    "instruction about a notification if the user intent matches the notification: \n" +
                    "User Intent: " + userInstruction + "\n" +
                    "Notification: " + notificationText

            val taskOptions = LlmInference.LlmInferenceOptions.builder()
                .setModelPath("/data/data/com.krosskinetic.notisentry/files/model.task")
                .setMaxTopK(64)
                .build()

            // Create an instance of the LLM Inference task
            val llmInference = LlmInference.createFromOptions(context, taskOptions)

            val summary = llmInference.generateResponse(prompt)

            Log.d("NotiSentryAI", "Gemini Decided to : $summary")

            return@withContext summary.equals("Allow", ignoreCase = true)

        } catch (e: Exception) {
            Log.d("NotiSentryAI", "Error in allowNotification: $e")
            return@withContext false
        }
    }
}

fun postNotification(context: Context, appNotification: AppNotifications) {
    val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    val channelId = "general_notifications"
    val channelName = "General"

    val channel = NotificationChannel(
        channelId,
        channelName,
        NotificationManager.IMPORTANCE_DEFAULT
    ).apply {
        description = "General app notifications"
    }
    notificationManager.createNotificationChannel(channel)

    val notificationBuilder = NotificationCompat.Builder(context, channelId)
        .setSmallIcon(R.drawable.ic_launcher_foreground) // IMPORTANT: Replace with your icon resource
        .setContentTitle("Potential Notification Alert from: ${getAppNameFromPackageName(context, appNotification.packageName)}")
        .setContentText(if (appNotification.text == "") "NotiSentry detected Notification from an app based on your intent, but notification did not have text. Please check the app that sent the notification." else "Text: " + appNotification.text)
        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
        .setAutoCancel(true) // Dismiss the notification when the user taps it

    val notificationId = appNotification.timestamp.toInt()
    notificationManager.notify(notificationId, notificationBuilder.build())
}

fun getAppNameFromPackageName(context: Context, packageName: String): String {
    return try {
        val packageManager = context.packageManager
        val applicationInfo = packageManager.getApplicationInfo(packageName, 0)
        packageManager.getApplicationLabel(applicationInfo).toString()
    } catch(_: PackageManager.NameNotFoundException) {
        packageName
    }
}