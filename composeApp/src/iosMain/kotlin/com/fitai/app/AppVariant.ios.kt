package com.fitai.app

import platform.Foundation.NSBundle

actual val appEnvironment: AppEnvironment
    get() {
        val raw = NSBundle.mainBundle.objectForInfoDictionaryKey("FITAI_ENVIRONMENT")?.toString()
        return when (raw?.lowercase()) {
            "qa" -> AppEnvironment.Qa
            "prod" -> AppEnvironment.Prod
            else -> AppEnvironment.Dev
        }
    }
