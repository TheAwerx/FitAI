package com.fitai.app.platform

import android.content.Context
import com.russhwolf.settings.Settings
import com.russhwolf.settings.SharedPreferencesSettings

private lateinit var prefsContext: Context

fun initAndroidPreferencesContext(context: Context) {
    prefsContext = context.applicationContext
}

actual fun createAppSettings(): Settings {
    check(::prefsContext.isInitialized) { "initAndroidPreferencesContext must be called first" }
    return SharedPreferencesSettings.Factory(prefsContext).create("fitai_prefs")
}
