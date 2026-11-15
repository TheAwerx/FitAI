package com.fitai.app.util

import java.util.Locale

actual fun deviceLanguageCode(): String = Locale.getDefault().language.lowercase()
