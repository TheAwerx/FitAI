package com.fitai.app.di

import com.fitai.app.notifications.FirestorePushDeviceRepository
import com.fitai.app.notifications.GetPushNotificationTypesUseCase
import com.fitai.app.notifications.IsServerPushActiveUseCase
import com.fitai.app.notifications.ObservePushTokenRefreshesUseCase
import com.fitai.app.notifications.PushDeviceRepository
import com.fitai.app.notifications.SetPushNotificationTypeUseCase
import com.fitai.app.notifications.SyncPushRegistrationUseCase
import com.fitai.app.notifications.UnregisterPushDeviceUseCase
import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.module

/** Server push registration. `PushTokenProvider` comes from the platform module. */
val pushNotificationModule = module {
    single<PushDeviceRepository> { FirestorePushDeviceRepository(get()) }
    factoryOf(::SyncPushRegistrationUseCase)
    factoryOf(::UnregisterPushDeviceUseCase)
    factoryOf(::ObservePushTokenRefreshesUseCase)
    factoryOf(::IsServerPushActiveUseCase)
    factoryOf(::GetPushNotificationTypesUseCase)
    factoryOf(::SetPushNotificationTypeUseCase)
}
