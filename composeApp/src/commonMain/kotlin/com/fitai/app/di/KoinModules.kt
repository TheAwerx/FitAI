package com.fitai.app.di

fun appModules() = listOf(
    platformDriverModule(),
    databaseModule(),
    appModule,
)
