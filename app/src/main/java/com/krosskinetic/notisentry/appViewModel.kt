package com.krosskinetic.notisentry

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.GenerativeBackend
import com.krosskinetic.notisentry.data.AppDetails
import com.krosskinetic.notisentry.data.AppNotificationSummary
import com.krosskinetic.notisentry.data.AppNotifications
import com.krosskinetic.notisentry.data.AppBlacklist
import com.krosskinetic.notisentry.data.NotificationRepository
import com.krosskinetic.notisentry.data.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject

data class AppUiState( // A Blueprint of everything UI needs to display at a certain moment
    // User app preference and data, need to be managed by DAO
    val blockedNotifications: List<AppNotifications> = emptyList(),
    val blacklistedApps: List<AppBlacklist> = emptyList(),
    val savedSummaries: List<AppNotificationSummary> = emptyList(),

    // Non-persistent data, should be recreated everytime app/screen is opened. No need to save
    val appDetailList: List<AppDetails> = emptyList(),
    val savedTodaySummaries: List<AppNotificationSummary> = emptyList(),
    val savedYesterdaySummaries: List<AppNotificationSummary> = emptyList(),
    val savedArchiveSummaries: List<AppNotificationSummary> = emptyList(),
    val filteredNotifs: List<AppNotifications> = emptyList(),

    // DataStore user preference
    val introDone: Boolean? = null,
    val startService: Boolean = false,
    val startServiceTime: Long = 0,
    val endServiceTime: Long = 0,
    val smartCategorizationString: String = "",
    val autoDeleteValue: Int = 0,
    val notificationCaptured: Int = 0,
)

const val NOTIFICATION_ID = 156
@HiltViewModel
class AppViewModel @Inject constructor(
    private val repository: NotificationRepository,
    private val settingsRepository: SettingsRepository
): ViewModel() {
    private val _uiState = MutableStateFlow(AppUiState()) // Created a live updating flow which can't
    // be read or written to
    val uiState: StateFlow<AppUiState> = _uiState.asStateFlow() // exposes it as a read-only flow

    init {
        _uiState.value = AppUiState()

        viewModelScope.launch {
            settingsRepository.notificationCapturedKeyFlow.collect { newNotificationCaptured ->
                _uiState.update { currentState ->
                    currentState.copy(
                        notificationCaptured = newNotificationCaptured
                    )
                }
            }
        }

        viewModelScope.launch {
            settingsRepository.startAutDeleteKeyFlow.collect { newAutoDeleteKey->
                _uiState.update { currentState ->
                    currentState.copy(
                        autoDeleteValue = newAutoDeleteKey
                    )
                }
            }
        }

        viewModelScope.launch {
            settingsRepository.startSmartCategorizationStringFlow.collect { newString ->
                _uiState.update { currentState ->
                    currentState.copy(
                        smartCategorizationString = newString
                    )
                }
            }
        }

        viewModelScope.launch {
            settingsRepository.startServiceFlow.collect { newStartService ->
                _uiState.update { currentState ->
                    currentState.copy(
                        startService = newStartService
                    )
                }
            }
        }

        viewModelScope.launch {
            settingsRepository.startServiceTimeFlow.collect { newStartServiceTime->
                _uiState.update { currentState ->
                    currentState.copy(
                        startServiceTime = newStartServiceTime
                    )
                }
            }
        }

        viewModelScope.launch {
            settingsRepository.endServiceTimeFlow.collect { newEndServiceTime ->
                _uiState.update { currentState ->
                    currentState.copy(
                       endServiceTime = newEndServiceTime
                    )
                }
            }
        }

        viewModelScope.launch {
            settingsRepository.introDoneFlow.collect { newIntroDone ->
                _uiState.update { currentState ->
                    currentState.copy(
                        introDone = newIntroDone
                    )
                }
            }
        }

        viewModelScope.launch {
            repository.blockedNotificationsFlow.collect { newBlockedList ->
                _uiState.update { currentState ->
                    currentState.copy(
                        blockedNotifications = newBlockedList
                    )
                }
            }
        }

        viewModelScope.launch {
            repository.blackListedAppsFlow.collect { blacklistedApps ->
                _uiState.update { currentState ->
                    currentState.copy(
                        blacklistedApps = blacklistedApps
                    )
                }
            }
        }
        viewModelScope.launch {
            repository.savedSummariesFlow.collect { savedSummaries ->
                _uiState.update { currentState ->
                    currentState.copy(
                        savedSummaries = savedSummaries
                    )
                }
            }
        }
    }

    fun updateAutoDeleteKey(key: Int){
        viewModelScope.launch {
            settingsRepository.saveAutoDeleteKey(key)
        }
    }

    fun formatTimestampToTime(timestamp: Long): String {
        val formatter = DateTimeFormatter.ofPattern("MM/dd/yy h:mm a", Locale.US)
        val instant = Instant.ofEpochMilli(timestamp)
        val localTime = instant.atZone(ZoneId.systemDefault())
        return localTime.format(formatter)
    }

    private fun getAppDetailsFromPackageName(context: Context, packageName: String): AppDetails? {
        val pm: PackageManager = context.packageManager
        val appInfo: ApplicationInfo?
        try {
            appInfo = pm.getApplicationInfo(packageName, 0)
        } catch (_: PackageManager.NameNotFoundException) {
            return null
        }
        val appName = pm.getApplicationLabel(appInfo).toString()
        val icon = pm.getApplicationIcon(appInfo)
        return AppDetails(appName, icon, packageName)
    }

    @SuppressLint("QueryPermissionsNeeded")
    fun updateListOfAppDetails(context: Context) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                val appDetailsList = mutableListOf<AppDetails>()
                val allInstalledApps =
                    context.packageManager.getInstalledApplications(PackageManager.GET_META_DATA).filter {
                        appInfo ->
                        context.packageManager.getLaunchIntentForPackage(appInfo.packageName) != null
                    }

                for (app in allInstalledApps) {
                    try {
                        val packageInfo = context.packageManager.getPackageInfo(
                            app.packageName,
                            PackageManager.GET_PERMISSIONS
                        )

                        val declaredPermissions = packageInfo.requestedPermissions

                        if (declaredPermissions != null && declaredPermissions.contains(Manifest.permission.POST_NOTIFICATIONS)) {
                            val details = getAppDetailsFromPackageName(context, app.packageName)
                            if (details != null) {
                                appDetailsList.add(details)
                            }
                        }
                    } catch(_: Exception){}
                }

                _uiState.update { currentState ->
                    currentState.copy(
                        appDetailList = appDetailsList.sortedBy { it.appName.lowercase() }
                    )
                }
            }
        }
    }

    fun updateBlacklistedApps(appPackage: String) {
        val currentBlacklist = _uiState.value.blacklistedApps
        val appIsInList = currentBlacklist.any { it.packageName == appPackage }

        viewModelScope.launch {
            if (appIsInList) {
                repository.removeFromBlacklist(appPackage)
            } else {
                repository.addToBlacklist(appPackage)
            }
        }
    }


    fun updateSummaries() {
        val savedSummaries = this._uiState.value.savedSummaries.toMutableList().sortedBy {it.startTimestamp}.reversed()

        // Get the date:
        val today = LocalDate.now()
        val yest = today.minusDays(1)
        val savedSummariesToday: MutableList<AppNotificationSummary> = mutableListOf()
        val savedSummariesYesterday: MutableList<AppNotificationSummary> = mutableListOf()
        val savedSummariesArchive: MutableList<AppNotificationSummary> = mutableListOf()

        for (summary in savedSummaries){
            when (Instant.ofEpochMilli(summary.endTimestamp).atZone(ZoneId.systemDefault()).toLocalDate()){
                today -> savedSummariesToday.add(summary)
                yest -> savedSummariesYesterday.add(summary)
                else -> savedSummariesArchive.add(summary)
            }
        }

        _uiState.update {
            currentState ->
            currentState.copy(
                savedTodaySummaries = savedSummariesToday,
                savedYesterdaySummaries = savedSummariesYesterday,
                savedArchiveSummaries = savedSummariesArchive
            )
        }
    }

    fun updateIntroDone(){
        viewModelScope.launch {
            settingsRepository.saveIntroDone(true)
        }
    }


    fun startStopFunc(context: Context) {
        viewModelScope.launch {
            toggleDND(context)
            if (uiState.value.startService){
                repository.clearProcessedMessages()
                settingsRepository.saveIsStarted(isStarted = false)
                settingsRepository.saveEndTime(endTime = System.currentTimeMillis())
                summarizeNotificationsWithGemma()
                resetNotificationCount()
                dismissPersistentNotification(context = context)
            } else {
                settingsRepository.saveIsStarted(isStarted = true)
                settingsRepository.saveStartTime(startTime = System.currentTimeMillis())
                postPersistentNotification(context = context)
            }
        }
    }

    fun isNotificationAccessGranted(context: Context): Boolean {
        return NotificationManagerCompat.getEnabledListenerPackages(context)
            .contains(context.packageName)
    }

    private fun summarizeNotificationsWithGemma() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                val timeStart = uiState.value.startServiceTime
                val timeEnd = uiState.value.endServiceTime

                val summaryText = _uiState.value.blockedNotifications
                    .filter { it.timestamp >= timeStart && it.timestamp <= timeEnd }
                    .joinToString(separator = "\n") { it.parsedText }

                Log.d("NotiSentryAI", "Summary text (Gemma): $summaryText")

                if (summaryText.isBlank()) {
                    return@withContext
                }

                try {
                    val prompt = """ 
                    You are an expert AI assistant designed to analyze and synthesize information from multiple notifications. Your goal is to identify distinct conversations and provide a high-level summary for each.

                    **Instructions:**
                    1.  Carefully read all the notifications listed below.
                    2.  Identify the main topics or conversations.
                    3.  Group the notifications that belong to the same conversation.
                    4.  For each group, create a concise, one to three sentence text that summarizes the core topic. Make sure you include all the major information present in the notification.
                    5.  If a notification is missing information, simply state that "<Group>: <AppName> sent a notification but further information was missing, please check the app for more details"
                    6.  Present the output as a clean numbered list of these headlines in the format: "1. <Group>: <Summary>" Don't use any asterisks for text.

                    **--- NOTIFICATIONS ---**
                    $summaryText
                    """

                    val model = Firebase.ai(backend = GenerativeBackend.googleAI())
                        .generativeModel("gemini-2.5-flash-lite")

                    val summary = model.generateContent(prompt)

                    Log.d("NotiSentryAI", "Summary: $summary")

                    repository.addToSavedSummary(summary.text?: "No summary found", timeStart, timeEnd)
                    settingsRepository.saveStartTime(0)
                    settingsRepository.saveEndTime(0)

                } catch (e: Exception) {
                    Log.d("NotiSentryAI", "Error summarizing notifications: $e")
                }
            }
        }
    }

    fun updateFilteredNotifs(startTime: Long, endTime: Long){
        viewModelScope.launch {
            val notifs = repository.blockedNotificationsFlow.map { notif -> notif.filter {it.timestamp >= startTime && it.timestamp <= endTime}}.first()
            _uiState.update {
                currentState ->
                currentState.copy(
                    filteredNotifs = notifs
                )
            }
        }
    }

    fun updateFilteredNotifsNoEnd(startTime: Long){
        viewModelScope.launch {
            val notifs = repository.blockedNotificationsFlow.map { notif -> notif.filter {it.timestamp >= startTime}}.first()
            _uiState.update {
                    currentState ->
                currentState.copy(
                    filteredNotifs = notifs
                )
            }
        }
    }

    fun getAppIconByPackageName(context: Context, packageName: String): Drawable? {
        return try {
            context.packageManager.getApplicationIcon(packageName)
        } catch (e: PackageManager.NameNotFoundException) {
            e.printStackTrace()
            null
        }
    }

    fun updateSmartCategorizationString(string: String){
        viewModelScope.launch {
            settingsRepository.saveSmartCategorizationStringTime(string)
            Log.d("NotiSentryAI", "Updated smart categorization string: ${settingsRepository.startSmartCategorizationStringFlow.first()}")
        }
    }

    fun deleteOldNotificationsSummaries(){
        val timestamp = System.currentTimeMillis() - (uiState.value.autoDeleteValue * 24 * 60 * 60 * 1000)
        viewModelScope.launch {
            repository.deleteOldSummaries(timestamp)
            repository.deleteOldNotifications(timestamp)
        }
    }

    fun deleteSummaryWithNotificationFromId(summaryId: Int){
        viewModelScope.launch {
            repository.deleteSummaryWithNotification(summaryId)
        }
    }

    fun resetNotificationCount(){
        viewModelScope.launch {
            settingsRepository.resetNotification()
        }
    }



    fun postPersistentNotification(
        context: Context,
        title: String = "NotiSentry Running.",
        content: String = "Notifications are being filtered. Enjoy a distraction free phone usage."
    ) {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val channelId = "general_notifications"

        val channel = NotificationChannel(
            channelId,
            "General",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "General Notification"
        }
        notificationManager.createNotificationChannel(channel)

        val notificationBuilder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentTitle(title)
            .setContentText(content)

        notificationManager.notify(NOTIFICATION_ID , notificationBuilder.build())
    }

    fun dismissPersistentNotification(context: Context) {
        with(NotificationManagerCompat.from(context)) {
            cancel(NOTIFICATION_ID)
        }
    }

    fun toggleDND(context: Context) {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (!uiState.value.startService){
            if (notificationManager.isNotificationPolicyAccessGranted) {
                notificationManager.setInterruptionFilter(
                    NotificationManager.INTERRUPTION_FILTER_PRIORITY
                )
            }
        } else {
            notificationManager.setInterruptionFilter(
                NotificationManager.INTERRUPTION_FILTER_ALL
            )
        }
    }

    fun sendAndCancelTestNotification(context: Context) {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val channelId = "general_notifications"

        val channel = NotificationChannel(
            channelId,
            "General",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "General app notifications"
        }
        notificationManager.createNotificationChannel(channel)

        val notificationBuilder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)

        notificationManager.notify(0, notificationBuilder.build())
        notificationManager.cancel(0)
    }
}