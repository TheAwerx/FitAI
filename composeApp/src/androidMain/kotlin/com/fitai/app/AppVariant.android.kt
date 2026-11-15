package com.fitai.app

actual val appEnvironment: AppEnvironment =
    if (BuildConfig.USE_REMOTE_PLAN_GENERATION) AppEnvironment.Prod else AppEnvironment.Dev
