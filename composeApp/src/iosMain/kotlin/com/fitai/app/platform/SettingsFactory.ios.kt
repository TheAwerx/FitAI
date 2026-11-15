package com.fitai.app.platform

import com.russhwolf.settings.NSUserDefaultsSettings
import com.russhwolf.settings.Settings
import platform.Foundation.NSUserDefaults

actual fun createAppSettings(): Settings =
    NSUserDefaultsSettings(NSUserDefaults.standardUserDefaults)
