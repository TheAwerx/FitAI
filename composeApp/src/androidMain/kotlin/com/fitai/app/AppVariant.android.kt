package com.fitai.app

actual val appEnvironment: AppEnvironment
    get() = when (BuildConfig.ENVIRONMENT) {
        "qa" -> AppEnvironment.Qa
        "prod" -> AppEnvironment.Prod
        else -> AppEnvironment.Dev
    }
