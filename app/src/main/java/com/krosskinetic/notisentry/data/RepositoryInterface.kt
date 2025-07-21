// NotiSentryEntryPoint.kt
package com.krosskinetic.notisentry.data

import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

// This interface tells Hilt how to give us dependencies
@EntryPoint
@InstallIn(SingletonComponent::class)
interface NotiSentryEntryPoint {
    fun notificationRepository(): NotificationRepository
    fun settingsRepository(): SettingsRepository
}