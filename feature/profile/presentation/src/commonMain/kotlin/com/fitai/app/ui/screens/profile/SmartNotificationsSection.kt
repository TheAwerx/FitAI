package com.fitai.app.ui.screens.profile

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.fitai.app.localization.LocalStrings
import com.fitai.app.localization.Strings
import com.fitai.app.notifications.PushNotificationType
import com.fitai.app.notifications.ReminderTexts
import com.fitai.app.notifications.isEnabled
import com.fitai.app.ui.theme.FitAIColors
import com.fitai.app.ui.theme.FitAIIcons
import com.fitai.app.ui.theme.FitAIShapes
import com.fitai.app.ui.theme.FitAISpacing
import com.fitai.app.viewmodel.PushNotificationSettingsViewModel
import com.fitai.app.viewmodel.notifications.PushNotificationSettingsIntent
import org.koin.compose.viewmodel.koinViewModel

/**
 * Per-type switches for the server's smart notifications. [refreshKey] changes when the reminder switch
 * or permission changed, which registers or removes this device.
 */
@Composable
internal fun SmartNotificationsSection(
    reminderTexts: ReminderTexts,
    refreshKey: Any?,
    viewModel: PushNotificationSettingsViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val strings = LocalStrings.current
    LaunchedEffect(refreshKey) { viewModel.onIntent(PushNotificationSettingsIntent.Refresh) }

    PushNotificationType.entries.forEach { type ->
        val (icon, title, body) = type.labels(strings)
        PushTypeRow(
            icon = icon,
            title = title,
            body = body,
            checked = state.types.isEnabled(type),
            enabled = !state.busy,
            onCheckedChange = { checked ->
                viewModel.onIntent(PushNotificationSettingsIntent.Toggle(type, checked, reminderTexts))
            },
        )
    }
    Text(
        text = if (state.pushActive == false) strings.pushSettingsNotRegistered else strings.pushSettingsHint,
        style = MaterialTheme.typography.bodySmall,
        color = FitAIColors.OnSurfaceMuted,
    )
}

private fun PushNotificationType.labels(strings: Strings): Triple<ImageVector, String, String> = when (this) {
    PushNotificationType.WorkoutLate ->
        Triple(FitAIIcons.FitnessCenter, strings.pushTypeWorkoutLateTitle, strings.pushTypeWorkoutLateBody)
    PushNotificationType.MealLogging ->
        Triple(FitAIIcons.Restaurant, strings.pushTypeMealTitle, strings.pushTypeMealBody)
    PushNotificationType.StreakRisk ->
        Triple(FitAIIcons.LocalFireDepartment, strings.pushTypeStreakTitle, strings.pushTypeStreakBody)
    PushNotificationType.Comeback ->
        Triple(FitAIIcons.Schedule, strings.pushTypeComebackTitle, strings.pushTypeComebackBody)
}

@Composable
private fun PushTypeRow(
    icon: ImageVector,
    title: String,
    body: String,
    checked: Boolean,
    enabled: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Surface(
        shape = FitAIShapes.card,
        color = FitAIColors.Surface,
        border = BorderStroke(1.dp, FitAIColors.SurfaceVariant),
        modifier = Modifier
            .fillMaxWidth()
            .toggleable(value = checked, enabled = enabled, role = Role.Switch, onValueChange = onCheckedChange),
    ) {
        Row(
            modifier = Modifier.padding(FitAISpacing.md),
            horizontalArrangement = Arrangement.spacedBy(FitAISpacing.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(icon, contentDescription = null, tint = FitAIColors.Primary, modifier = Modifier.size(22.dp))
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(FitAISpacing.xs)) {
                Text(title, style = MaterialTheme.typography.bodyLarge, color = FitAIColors.OnSurface)
                Text(body, style = MaterialTheme.typography.bodySmall, color = FitAIColors.OnSurfaceMuted)
            }
            Switch(
                checked = checked,
                onCheckedChange = null,
                enabled = enabled,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = FitAIColors.OnPrimary,
                    checkedTrackColor = FitAIColors.Primary,
                ),
            )
        }
    }
}
