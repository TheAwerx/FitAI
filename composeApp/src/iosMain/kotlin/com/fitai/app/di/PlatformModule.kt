package com.fitai.app.di

import com.fitai.app.auth.PlatformAuthSession
import com.fitai.app.data.DatabaseDriverFactory
import com.fitai.app.data.IosDatabaseDriverFactory
import com.fitai.app.notifications.IosReminderScheduler
import com.fitai.app.notifications.ReminderScheduler
import com.fitai.app.trial.DeviceKeyProvider
import com.fitai.app.trial.IosDeviceKeyProvider
import com.fitai.app.ui.platform.IosPlatformAuthSession
import org.koin.dsl.module

actual fun platformDriverModule() = module {
    single<DatabaseDriverFactory> { IosDatabaseDriverFactory() }
    single<ReminderScheduler> { IosReminderScheduler() }
    single<PlatformAuthSession> { IosPlatformAuthSession() }
    single<DeviceKeyProvider> { IosDeviceKeyProvider() }
}
