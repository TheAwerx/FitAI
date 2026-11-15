package com.fitai.app.firebase

import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.app
import dev.gitlive.firebase.initialize

private var cached: Boolean? = null

actual object FirebaseBootstrap {
    actual fun ensureInitialized(): Boolean {
        cached?.let { return it }
        if (hasDefaultApp()) {
            cached = true
            return true
        }
        runCatching { Firebase.initialize() }
        val ok = hasDefaultApp()
        cached = ok
        return ok
    }
}

private fun hasDefaultApp(): Boolean = runCatching { Firebase.app }.isSuccess
