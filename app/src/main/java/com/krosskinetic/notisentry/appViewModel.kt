package com.krosskinetic.notisentry

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.GenerativeBackend
import com.krosskinetic.notisentry.data.AppDetails
import com.krosskinetic.notisentry.data.AppNotificationSummary
import com.krosskinetic.notisentry.data.AppNotifications
import com.krosskinetic.notisentry.data.AppWhitelist
import com.krosskinetic.notisentry.data.NotificationRepository
import com.krosskinetic.notisentry.data.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
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
    val whitelistedApps: List<AppWhitelist> = emptyList(),
    val savedSummaries: List<AppNotificationSummary> = emptyList(),

    // Non-persistent data, should be recreated everytime app/screen is opened. No need to save
    val appDetailList: List<AppDetails> = emptyList(),
    val savedTodaySummaries: List<AppNotificationSummary> = emptyList(),
    val savedYesterdaySummaries: List<AppNotificationSummary> = emptyList(),
    val savedArchiveSummaries: List<AppNotificationSummary> = emptyList(),

    // DataStore user preference
    val startService: Boolean = false,
    val startServiceTime: Long = 0,
    val endServiceTime: Long = 0,
    val introDone: Boolean = false,
    val isLoading: Boolean = true
)
@HiltViewModel
class AppViewModel @Inject constructor(
    private val repository: NotificationRepository,
    private val settingsRepository: SettingsRepository
): ViewModel() { // Holds UI data that survives configuration changes
    private val _uiState = MutableStateFlow(AppUiState()) // Tracks updates of GameUiState and transmits it
    val uiState: StateFlow<AppUiState> = _uiState.asStateFlow()

    val startService = settingsRepository.startServiceFlow
    val startServiceTime = settingsRepository.startServiceTimeFlow
    val endServiceTime = settingsRepository.endServiceTimeFlow
    val introDone = settingsRepository.introDoneFlow

    init {
        _uiState.value = AppUiState()

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
            repository.whiteListedAppsFlow.collect { whitelistedApps ->
                _uiState.update { currentState ->
                    currentState.copy(
                        whitelistedApps = whitelistedApps
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

    fun updateListOfAppDetails(context: Context) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                val appDetailsList = mutableListOf<AppDetails>()
                val allInstalledApps =
                    context.packageManager.getInstalledApplications(PackageManager.GET_META_DATA)

                for (app in allInstalledApps) {
                    val details = getAppDetailsFromPackageName(context, app.packageName)
                    if (details != null) {
                        appDetailsList.add(details)
                    }
                }

                _uiState.update { currentState ->
                    currentState.copy(
                        appDetailList = appDetailsList.sortedBy { it.appName.lowercase() }
                    )
                }
            }
        }
    }

    fun updateWhitelistedApps(appPackage: String) {
        val currentWhitelist = _uiState.value.whitelistedApps
        val appIsInList = currentWhitelist.any { it.packageName == appPackage }

        viewModelScope.launch {
            if (appIsInList) {
                repository.removeFromWhitelist(appPackage)
            } else {
                repository.addToWhitelist(appPackage)
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
        _uiState.update {
            currentState ->
            currentState.copy(
                introDone = true
            )
        }
    }


    fun startStopFunc(context: Context){
        if (_uiState.value.startService){
            Log.d("NotiSentryAI", "Stopping service")
            _uiState.update {
                currentState ->
                currentState.copy(
                    endServiceTime = System.currentTimeMillis(),
                    startService = !currentState.startService
                )
            }
            summarizeNotifications(context)
        } else {
            Log.d("NotiSentryAI", "Started service")
            _uiState.update {
                currentState ->
                currentState.copy(
                    startServiceTime = System.currentTimeMillis(),
                    startService = !currentState.startService
                )
            }
        }
    }

    fun isNotificationAccessGranted(context: Context): Boolean {
        return NotificationManagerCompat.getEnabledListenerPackages(context)
            .contains(context.packageName)
    }

    /** This is for Gemini Nano, not yet tested due to lack of Pixel 9 Pro
    fun summarizeNotificationsWithNano(context: Context){
        viewModelScope.launch {
            val timeStart = uiState.value.startServiceTime
            val timeEnd = uiState.value.endServiceTime
            // Using joinToString for cleaner code.

            val summaryText = _uiState.value.blockedNotifications
                .filter { it.timestamp >= timeStart && it.timestamp<= timeEnd }
                .joinToString(separator = "\n") {"App Package: " + it.packageName + ", Notification Text" +  it.text}

            Log.d("NotiSentryAI", "Summary text: $summaryText")

            if (summaryText.isBlank()) {
                return@launch
            }

            withContext(Dispatchers.IO) {
                try {
                    val summarizerOptions = SummarizerOptions.builder(context)
                        .setInputType(SummarizerOptions.InputType.ARTICLE)
                        .setOutputType(SummarizerOptions.OutputType.ONE_BULLET)
                        .setLanguage(SummarizerOptions.Language.ENGLISH)
                        .build()

                    val summarizer = Summarization.getClient(summarizerOptions)

                    // 1. CHECK THE STATUS FIRST
                    val featureStatus = summarizer.checkFeatureStatus().await()
                    Log.d("NotiSentryAI", "Summarizer status: $featureStatus")

                    withContext(Dispatchers.IO) {
                        val summarizationRequest = SummarizationRequest.builder(summaryText).build()
                        val result = summarizer.runInference(summarizationRequest).get().summary
                        Log.d("NotiSentryAI", "Summarized: $result")

                        repository.addToSavedSummary(result, timeStart, timeEnd)
                        _uiState.update { it.copy(startServiceTime = 0, endServiceTime = 0) }
                    }

                } catch (e: Exception) {
                    Log.e("NotiSentryAI", "An error occurred during summarization process. Trying Gemini Flash", e)
                    summarizeNotifications(summaryText, timeStart, timeEnd)
                }
            }
        }
    }*/


    // The unused Context parameter has been removed.
    fun summarizeNotifications(context: Context) {

        val timeStart = uiState.value.startServiceTime
        val timeEnd = uiState.value.endServiceTime
        // Using joinToString for cleaner code.

        val summaryText = _uiState.value.blockedNotifications
            .filter { it.timestamp >= timeStart && it.timestamp<= timeEnd }
            .joinToString(separator = "\n") {"App Package: " + it.packageName + ", Notification Text" +  it.text}

        Log.d("NotiSentryAI", "Summary text: $summaryText")

        if (summaryText.isBlank()) {
            return
        }

        viewModelScope.launch {
            try {
                val model = Firebase.ai(backend = GenerativeBackend.googleAI())
                    .generativeModel("gemini-2.5-flash")

                val prompt = "Summarize the following notifications into distinct sections " +
                        "that don't overwhelm the user but provide all the necessary " +
                        "information. Make sure to include the name of the app in the summary if it isn't clear from the summary text itself so people know where it is coming from. Follow the following format: \n" +
                        "Here is the summary: \n " +
                        "<Section>: <Summary> \n" +
                        " If text is missing, say what is written inside delimiters ```<Section>: Notification from <App Name> was received, but text was missing.``` \n" +
                        "Notifications to summarize: \n" +
                        summaryText

                val summary = model.generateContent(prompt).text ?: ""

                if (summary.isNotBlank()) {
                    repository.addToSavedSummary(summary, timeStart, timeEnd)
                    _uiState.update {
                        currentState ->
                        currentState.copy(
                            startServiceTime = 0,
                            endServiceTime = 0
                        )
                    }
                }

            } catch (e: Exception) {
                Log.d("NotiSentryAI", "Error summarizing notifications: $e")
            }
        }
    }


}