package com.fitai.app.di

import com.fitai.app.auth.PlatformAuthSession
import com.fitai.app.data.AndroidDatabaseDriverFactory
import com.fitai.app.data.DatabaseDriverFactory
import com.fitai.app.notifications.AndroidPushTokenProvider
import com.fitai.app.notifications.AndroidReminderScheduler
import com.fitai.app.notifications.PushTokenProvider
import com.fitai.app.notifications.ReminderScheduler
import com.fitai.app.ui.platform.AndroidPlatformAuthSession
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

actual fun platformDriverModule() = module {
    single<DatabaseDriverFactory> { AndroidDatabaseDriverFactory(androidContext()) }
    single<ReminderScheduler> { AndroidReminderScheduler(androidContext()) }
    single<PushTokenProvider> { AndroidPushTokenProvider(androidContext()) }
    single<PlatformAuthSession> { AndroidPlatformAuthSession(androidContext()) }
}
