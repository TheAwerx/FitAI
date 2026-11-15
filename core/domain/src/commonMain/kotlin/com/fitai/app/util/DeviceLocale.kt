package com.fitai.app.util

/** Lowercase ISO-639 language of the device, e.g. "tr" or "en". */
expect fun deviceLanguageCode(): String

/** Language picked inside the app ("tr" / "en"); set by the app root, null follows the device. */
object AppLanguage {
    var override: String? = null
}

/** AI request and generated-plan locale; mirrors `ProvideFitAIStrings` (English only for "en", Turkish otherwise). */
fun appLocale(): String = if ((AppLanguage.override ?: deviceLanguageCode()) == "en") "en" else "tr"
