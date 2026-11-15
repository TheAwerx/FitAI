package com.fitai.app

enum class AppEnvironment {
    Dev,
    Prod,
}

expect val appEnvironment: AppEnvironment

val useRemotePlanGeneration: Boolean
    get() = appEnvironment == AppEnvironment.Prod
