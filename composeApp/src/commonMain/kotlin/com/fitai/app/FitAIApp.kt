package com.fitai.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.fitai.app.data.AppPreferences
import com.fitai.app.localization.ProvideFitAIStrings
import com.fitai.app.navigation.NavigationRoot
import com.fitai.app.ui.theme.FitAITheme
import com.fitai.app.util.AppLanguage
import org.koin.compose.koinInject

@Composable
fun FitAIApp(preferences: AppPreferences = koinInject()) {
    var language by remember { mutableStateOf(preferences.languageOverride) }
    SideEffect { AppLanguage.override = language }
    FitAITheme {
        ProvideFitAIStrings(languageOverride = language) {
            NavigationRoot(
                onLanguageChange = { code ->
                    preferences.languageOverride = code
                    language = code
                },
            )
        }
    }
}
