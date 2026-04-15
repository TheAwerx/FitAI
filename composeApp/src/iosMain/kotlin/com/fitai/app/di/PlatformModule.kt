package com.fitai.app.di

import com.fitai.app.data.DatabaseDriverFactory
import com.fitai.app.data.IosDatabaseDriverFactory
import org.koin.dsl.module

actual fun platformDriverModule() = module {
    single<DatabaseDriverFactory> { IosDatabaseDriverFactory() }
}
