package com.fitai.app.di

import com.fitai.app.data.AppPreferences
import com.fitai.app.platform.createAppSettings
import org.koin.dsl.module

val appModule = module {
    single { createAppSettings() }
    single { AppPreferences(get()) }
}
