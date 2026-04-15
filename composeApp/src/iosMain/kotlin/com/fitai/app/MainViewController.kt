package com.fitai.app

import androidx.compose.ui.window.ComposeUIViewController
import com.fitai.app.di.appModules
import org.koin.core.context.startKoin

private var koinStarted = false

fun MainViewController() = ComposeUIViewController {
    if (!koinStarted) {
        startKoin {
            modules(appModules())
        }
        koinStarted = true
    }
    FitAIApp()
}
