package com.fitai.app

enum class AppEnvironment {
    Dev,
    Qa,
    Prod,
}

expect val appEnvironment: AppEnvironment

val useRemotePlanGeneration: Boolean
    get() = appEnvironment == AppEnvironment.Qa || appEnvironment == AppEnvironment.Prod
