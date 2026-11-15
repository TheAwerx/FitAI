package com.fitai.app.util

import platform.Foundation.NSLocale
import platform.Foundation.preferredLanguages

actual fun deviceLanguageCode(): String {
    val tag = NSLocale.preferredLanguages.firstOrNull() as? String ?: return "tr"
    return tag.substringBefore('-').lowercase()
}
