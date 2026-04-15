package com.fitai.app.di

import com.fitai.app.data.AndroidDatabaseDriverFactory
import com.fitai.app.data.DatabaseDriverFactory
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

actual fun platformDriverModule() = module {
    single<DatabaseDriverFactory> { AndroidDatabaseDriverFactory(androidContext()) }
}
