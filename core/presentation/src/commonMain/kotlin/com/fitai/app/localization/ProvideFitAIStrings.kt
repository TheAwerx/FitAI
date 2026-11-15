package com.fitai.app.localization

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.text.intl.Locale

/** [languageOverride] ("tr" / "en") wins over the device locale when the user picked a language in-app. */
@Composable
fun ProvideFitAIStrings(
    languageOverride: String? = null,
    content: @Composable () -> Unit,
) {
    val language = languageOverride ?: rememberFitAILanguageTag()
    val strings = remember(language) {
        if (language.startsWith("en", ignoreCase = true)) StringsEn else StringsTr
    }
    CompositionLocalProvider(LocalStrings provides strings, content = content)
}

/** Languages the app ships; codes match [Strings.languageCode]. */
val SupportedLanguages = listOf("tr", "en")

@Composable
private fun rememberFitAILanguageTag(): String {
    // Prefer Compose locale; fall back to Turkish as project default.
    return runCatching { Locale.current.language }.getOrDefault("tr")
}
