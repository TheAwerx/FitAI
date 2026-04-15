package com.fitai.app

import androidx.compose.runtime.Composable
import com.fitai.app.navigation.AppNavHost
import com.fitai.app.ui.theme.FitAITheme

@Composable
fun FitAIApp() {
    FitAITheme {
        AppNavHost()
    }
}
