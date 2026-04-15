package com.fitai.app.di

import com.fitai.app.data.DatabaseDriverFactory
import com.fitai.app.db.FitAiDatabase
import org.koin.dsl.module

fun databaseModule() = module {
    single { FitAiDatabase(get<DatabaseDriverFactory>().createDriver()) }
}
