package com.fitai.app.notifications

import com.russhwolf.settings.Settings
import com.russhwolf.settings.set

class ReminderStateRepositoryImpl(
    private val settings: Settings,
) : ReminderStateRepository {

    override var sequenceAnchorEpochMillis: Long?
        get() = settings.getLongOrNull(KEY_SEQUENCE_ANCHOR)
        set(value) = if (value == null) settings.remove(KEY_SEQUENCE_ANCHOR) else settings[KEY_SEQUENCE_ANCHOR] = value

    private companion object {
        const val KEY_SEQUENCE_ANCHOR = "reminder_sequence_anchor_ms"
    }
}
