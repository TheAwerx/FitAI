package com.fitai.app.firebase

expect object FirebaseBootstrap {
    /** Call once per process before using Firebase APIs; returns false if config is missing or init fails. */
    fun ensureInitialized(): Boolean
}
