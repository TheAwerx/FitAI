package com.fitai.app

import androidx.compose.ui.window.ComposeUIViewController
import com.fitai.app.di.appModules
import com.fitai.app.firebase.FirebaseBootstrap
import org.koin.core.context.startKoin

private var koinStarted = false

fun MainViewController() = ComposeUIViewController {
    if (!koinStarted) {
        FirebaseBootstrap.ensureInitialized()
        startKoin {
            modules(appModules())
        }
        koinStarted = true
    }
    FitAIApp()
}
