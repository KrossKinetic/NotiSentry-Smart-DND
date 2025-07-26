package com.krosskinetic.notisentry.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")
@Singleton
class SettingsRepository @Inject constructor(@ApplicationContext private val context: Context) {
    private val START_SERVICE_KEY = booleanPreferencesKey("start_service")
    private val START_SERVICE_TIME_KEY = longPreferencesKey("start_service_time")
    private val END_SERVICE_TIME_KEY = longPreferencesKey("end_service_time")
    private val INTRO_DONE_KEY = booleanPreferencesKey("intro_done")

    private val USE_SMART_CATEGORIZATION_KEY = booleanPreferencesKey("smart_categorization")

    private val SMART_CATEGORIZATION_STRING_KEY = stringPreferencesKey("smart_categorization_string")

    val startUseSmartCategorizationFlow: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[USE_SMART_CATEGORIZATION_KEY] ?: false
        }

    val startSmartCategorizationStringFlow: Flow<String> = context.dataStore.data
        .map { preferences ->
            preferences[SMART_CATEGORIZATION_STRING_KEY] ?: ""
        }
    val startServiceFlow: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[START_SERVICE_KEY] ?: false
        }

    val startServiceTimeFlow: Flow<Long> = context.dataStore.data
        .map { preferences ->
            preferences[START_SERVICE_TIME_KEY] ?: 0L
        }

    val endServiceTimeFlow: Flow<Long> = context.dataStore.data
        .map { preferences ->
            preferences[END_SERVICE_TIME_KEY] ?: 0L
        }

    val introDoneFlow: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[INTRO_DONE_KEY] ?: false
        }

    suspend fun saveIsStarted(isStarted: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[START_SERVICE_KEY] = isStarted
        }
    }

    suspend fun saveStartTime(startTime: Long) {
        context.dataStore.edit { preferences ->
            preferences[START_SERVICE_TIME_KEY] = startTime
        }
    }

    suspend fun saveEndTime(endTime: Long) {
        context.dataStore.edit { preferences ->
            preferences[END_SERVICE_TIME_KEY] = endTime
        }
    }

    suspend fun saveIntroDone(isDone: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[INTRO_DONE_KEY] = isDone
        }
    }

    suspend fun saveSmartCategorization(isSmart: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[USE_SMART_CATEGORIZATION_KEY] = isSmart
        }
    }

    suspend fun saveSmartCategorizationStringTime(string: String) {
        context.dataStore.edit { preferences ->
            preferences[SMART_CATEGORIZATION_STRING_KEY] = string
        }
    }
}