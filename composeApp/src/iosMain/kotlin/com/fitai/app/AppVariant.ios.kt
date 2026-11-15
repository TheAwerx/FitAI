package com.fitai.app

import platform.Foundation.NSBundle

actual val appEnvironment: AppEnvironment
    get() {
        val raw = NSBundle.mainBundle.objectForInfoDictionaryKey("FITAI_ENVIRONMENT")?.toString()
        return if (raw.equals("prod", ignoreCase = true)) AppEnvironment.Prod else AppEnvironment.Dev
    }
