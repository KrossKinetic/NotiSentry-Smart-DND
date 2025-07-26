package com.krosskinetic.notisentry

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.Person
import androidx.core.graphics.drawable.toBitmapOrNull
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.GenerativeBackend
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
                if (!repository.isAppWhitelisted(packageName) && settingsRepository.startServiceFlow.first() && !isNotificationLive(sbn)) {
                    cancelNotification(sbn.key)

                    var allow = false

                    val notification = extractNotificationData(sbn,this@NotiSentryService)
                    val notificationFinalText = formatNotificationToString(notification)
                    notification.parsedText = notificationFinalText

                    Log.d("NotiSentryAI", "Notification: $notificationFinalText")

                    if (settingsRepository.startUseSmartCategorizationFlow.first()){
                        allow = allowNotification(
                            notificationText = notificationFinalText,
                            intent = settingsRepository.startSmartCategorizationStringFlow.first()
                        )
                    }

                    if (allow){
                        postNotification(this@NotiSentryService, notification)
                        Log.d(tag, "Allowed Notification using Gemini-2.5-Flash from: $packageName")
                    } else {
                        repository.addBlockedNotification(notification)
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

suspend fun allowNotification(notificationText: String, intent: String): Boolean {
    return withContext(Dispatchers.IO) {
        val userInstruction = intent

        try {

            val prompt = "Return a simple 'Allow' or 'Don't Allow' based on the following user " +
                    "instruction about a notification if the user intent matches the notification. 'Allow' or 'Don't Allow' must be " +
                    "at the end of the response with 'A_2' (if Allowed) or 'A_1' (if Don't Allow) : \n" +
                    "User Intent: " + userInstruction + "\n" +
                    "Notification: " + notificationText

            val model = Firebase.ai(backend = GenerativeBackend.googleAI())
                .generativeModel("gemini-2.5-flash")

            val summary = model.generateContent(prompt).text?:""

            Log.d("NotiSentryAI", "Gemini Decided to : $summary")

            return@withContext summary.contains("A_2")

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

    // Create the channel (no changes needed here)
    val channel = NotificationChannel(
        channelId,
        channelName,
        NotificationManager.IMPORTANCE_DEFAULT
    ).apply {
        description = "General app notifications"
    }
    notificationManager.createNotificationChannel(channel)

    val launchIntent = context.packageManager.getLaunchIntentForPackage(appNotification.packageName)
    val pendingIntent = if (launchIntent != null) {
        PendingIntent.getActivity(context, 0, launchIntent, PendingIntent.FLAG_IMMUTABLE)
    } else {
        null
    }

    // Start building the notification
    val notificationBuilder = NotificationCompat.Builder(context, channelId)
        .setSmallIcon(R.drawable.ic_launcher_foreground)
        .setLargeIcon(getAppIconByPackageName(context, appNotification.packageName)?.toBitmapOrNull())
        .setWhen(appNotification.timestamp)
        .setContentIntent(pendingIntent)
        .setAutoCancel(true)
        .setPriority(NotificationCompat.PRIORITY_DEFAULT)


    if (appNotification.messages.isNotEmpty()) {
        val person = Person.Builder().setName("You").build()
        val messagingStyle = NotificationCompat.MessagingStyle(person)
            .setConversationTitle(appNotification.conversationTitle.ifBlank { appNotification.title })
            .setGroupConversation(appNotification.messages.distinctBy { it.sender }.count() > 1)

        appNotification.messages.forEach { msg ->
            val senderPerson = Person.Builder().setName(msg.sender).build()
            messagingStyle.addMessage(msg.text, msg.timestamp, senderPerson)
        }

        notificationBuilder.setStyle(messagingStyle)

    } else {
        // This is a standard notification
        notificationBuilder
            .setContentTitle(appNotification.title.ifBlank { appNotification.appName })
            .setContentText(appNotification.text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(appNotification.text)) // Allow text to expand
    }

    // Use a unique ID based on the timestamp for this notification
    val notificationId = appNotification.timestamp.toInt()
    notificationManager.notify(notificationId, notificationBuilder.build())
}

fun getAppIconByPackageName(context: Context, packageName: String): Drawable? {
    return try {
        context.packageManager.getApplicationIcon(packageName)
    } catch (e: PackageManager.NameNotFoundException) {
        e.printStackTrace()
        null
    }
}


fun formatNotificationToString(notification: AppNotifications): String {
    val sb = StringBuilder()

    val displayTitle = notification.conversationTitle.ifBlank {
        notification.title
    }
    sb.append("[${notification.appName}]")
    if (displayTitle.isNotBlank()) {
        sb.append(" $displayTitle")
    }
    sb.appendLine()
    if (notification.messages.isNotEmpty()) {
        notification.messages.forEach { message ->
            val sender = message.sender ?: "Unknown"
            sb.appendLine("$sender: ${message.text}")
        }
    } else if (notification.text.isNotBlank()) {
        sb.appendLine(notification.text)
    }

    return sb.toString().trim()
}

fun extractNotificationData(sbn: StatusBarNotification, context: Context): AppNotifications {
    val extras = sbn.notification.extras
    val packageName = sbn.packageName

    val appName = try {
        val pm = context.packageManager
        pm.getApplicationLabel(pm.getApplicationInfo(packageName, PackageManager.GET_META_DATA)).toString()
    } catch (_: Exception) {
        packageName
    }

    val messages = extras.getParcelableArray(Notification.EXTRA_MESSAGES, Bundle::class.java)?.mapNotNull {
        AppNotifications.MessageInfo(
            sender = it.getString("sender"),
            text = it.getString("text"),
            timestamp = it.getLong("time")
        )
    } ?: emptyList()

    return AppNotifications(
        title = extras.getString(Notification.EXTRA_TITLE) ?: "",
        text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: "",
        timestamp = sbn.postTime,
        packageName = packageName,
        appName = appName,
        messages = messages,
        conversationTitle = extras.getString(Notification.EXTRA_CONVERSATION_TITLE) ?: ""
    )
}

fun isNotificationLive(sbn: StatusBarNotification): Boolean {
    val notification = sbn.notification
    val extras = notification.extras
    val isOngoingFlag = (notification.flags and Notification.FLAG_ONGOING_EVENT) != 0
    val hasChronometer = extras.getBoolean(Notification.EXTRA_SHOW_CHRONOMETER)
    val hasProgressBar = extras.containsKey(Notification.EXTRA_PROGRESS)
    return isOngoingFlag || hasChronometer || hasProgressBar
}