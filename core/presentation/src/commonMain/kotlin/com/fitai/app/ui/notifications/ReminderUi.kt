package com.fitai.app.ui.notifications

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.fitai.app.localization.Strings
import com.fitai.app.notifications.ReminderMessage
import com.fitai.app.notifications.ReminderTexts
import com.fitai.app.ui.components.FitAINumericPicker
import com.fitai.app.ui.theme.FitAISpacing

val ReminderMinuteOptions = listOf(0, 15, 30, 45)

private fun twoDigits(value: Int): String = value.toString().padStart(2, '0')

fun formatReminderTime(hour: Int, minute: Int): String = "${twoDigits(hour)}:${twoDigits(minute)}"

fun Strings.reminderTexts(): ReminderTexts = ReminderTexts(
    workout = ReminderMessage(notificationsWorkoutTitle, notificationsWorkoutBody),
    meal = ReminderMessage(notificationsMealTitle, notificationsMealBody),
    day0FirstSession = ReminderMessage(notificationsDay0Title, notificationsDay0Body),
    day1MealPhoto = ReminderMessage(notificationsDay1Title, notificationsDay1Body),
    day2RestDay = ReminderMessage(notificationsDay2Title, notificationsDay2Body),
    day3Overload = ReminderMessage(notificationsDay3Title, notificationsDay3Body),
    day5TodayMode = ReminderMessage(notificationsDay5Title, notificationsDay5Body),
    day7WeeklyReport = ReminderMessage(notificationsDay7Title, notificationsDay7Body),
    trialEnd = ReminderMessage(notificationsTrialEndTitle, notificationsTrialEndBody),
)

@Composable
fun ReminderTimePicker(
    hour: Int,
    minute: Int,
    onHourChange: (Int) -> Unit,
    onMinuteChange: (Int) -> Unit,
    strings: Strings,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(FitAISpacing.lg),
    ) {
        FitAINumericPicker(
            value = hour,
            onValueChange = onHourChange,
            range = 0..23,
            unit = strings.notificationsHourUnit,
            format = ::twoDigits,
        )
        FitAINumericPicker(
            value = minute,
            onValueChange = onMinuteChange,
            range = 0..59,
            unit = strings.notificationsMinuteUnit,
            format = ::twoDigits,
            values = ReminderMinuteOptions,
        )
    }
}
