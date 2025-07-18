package com.krosskinetic.notisentry.data

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class) // This makes the database a singleton
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "notiSentryDatabase"
        ).build()
    }

    @Provides
    fun provideNotificationDao(appDatabase: AppDatabase): NotificationDao {
        return appDatabase.notificationDao()
    }

    @Provides
    fun provideAppWhitelistDao(appDatabase: AppDatabase): AppWhitelistDao {
        return appDatabase.whitelistDao()
    }

    @Provides
    fun provideSavedSummariesDao(appDatabase: AppDatabase): AppSummariesDao {
        return appDatabase.savedSummariesDao()
    }
}