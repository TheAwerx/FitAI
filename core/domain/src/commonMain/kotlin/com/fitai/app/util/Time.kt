package com.fitai.app.util

import kotlinx.datetime.Instant

expect fun epochMillis(): Long

fun nowIsoTimestamp(): String = Instant.fromEpochMilliseconds(epochMillis()).toString()
