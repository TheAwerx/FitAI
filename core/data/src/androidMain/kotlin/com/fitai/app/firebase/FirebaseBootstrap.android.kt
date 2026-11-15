package com.fitai.app.firebase

import android.content.Context
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.app
import dev.gitlive.firebase.initialize

private lateinit var appContext: Context
private var cached: Boolean? = null

fun setFirebaseAndroidContext(context: Context) {
    appContext = context.applicationContext
}

actual object FirebaseBootstrap {
    actual fun ensureInitialized(): Boolean {
        cached?.let { return it }
        if (hasDefaultApp()) {
            cached = true
            return true
        }
        if (!::appContext.isInitialized) return false
        runCatching { Firebase.initialize(appContext) }
        val ok = hasDefaultApp()
        cached = ok
        return ok
    }
}

private fun hasDefaultApp(): Boolean = runCatching { Firebase.app }.isSuccess
