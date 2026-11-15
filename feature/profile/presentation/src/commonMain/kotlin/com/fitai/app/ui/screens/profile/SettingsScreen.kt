package com.fitai.app.ui.screens.profile

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.fitai.app.localization.LocalStrings
import com.fitai.app.localization.Strings
import com.fitai.app.localization.SupportedLanguages
import com.fitai.app.model.ActivityLevel
import com.fitai.app.model.DietRestriction
import com.fitai.app.model.EquipmentOption
import com.fitai.app.model.FitnessGoal
import com.fitai.app.model.decodeProfileCodes
import com.fitai.app.notifications.ReminderSettings
import com.fitai.app.ui.components.FitAIButton
import com.fitai.app.ui.components.FitAIButtonVariant
import com.fitai.app.ui.components.FitAITextField
import com.fitai.app.ui.notifications.ReminderTimePicker
import com.fitai.app.ui.notifications.formatReminderTime
import com.fitai.app.ui.notifications.reminderTexts
import com.fitai.app.ui.platform.rememberAppleSignInLauncher
import com.fitai.app.ui.platform.rememberGoogleSignInLauncher
import com.fitai.app.ui.platform.rememberNotificationPermissionRequester
import com.fitai.app.ui.theme.FitAIColors
import com.fitai.app.ui.theme.FitAIIcons
import com.fitai.app.ui.theme.FitAIShapes
import com.fitai.app.ui.theme.FitAISpacing
import com.fitai.app.viewmodel.ProfileViewModel
import com.fitai.app.viewmodel.profile.ProfileEffect
import com.fitai.app.viewmodel.profile.ProfileIntent
import com.fitai.app.viewmodel.profile.ProfileState
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onNavigateSignIn: () -> Unit,
    onNavigatePaywall: () -> Unit,
    onSignedOut: () -> Unit,
    onLanguageChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ProfileViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val strings = LocalStrings.current

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is ProfileEffect.ShowMessage -> Unit
                ProfileEffect.NavigatePaywall -> onNavigatePaywall()
                ProfileEffect.SignedOut -> onSignedOut()
            }
        }
    }

    val launchGoogle = rememberGoogleSignInLauncher { result ->
        result.onSuccess { viewModel.onIntent(ProfileIntent.GoogleIdToken(it)) }
        result.onFailure { viewModel.onIntent(ProfileIntent.ProviderError(it.message)) }
    }
    val launchApple = rememberAppleSignInLauncher { result ->
        result.onSuccess { (id, nonce) -> viewModel.onIntent(ProfileIntent.AppleTokens(id, nonce)) }
        result.onFailure { viewModel.onIntent(ProfileIntent.ProviderError(it.message)) }
    }
    val reminderTexts = remember(strings) { strings.reminderTexts() }
    val requestNotificationPermission = rememberNotificationPermissionRequester { granted ->
        viewModel.onIntent(ProfileIntent.RemindersPermissionResolved(granted, reminderTexts))
    }

    Column(modifier = modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .height(64.dp)
                .padding(horizontal = FitAISpacing.lg),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(FitAISpacing.md),
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.size(40.dp),
                colors = IconButtonDefaults.iconButtonColors(containerColor = FitAIColors.Surface),
            ) {
                Icon(FitAIIcons.ChevronLeft, contentDescription = strings.actionBack, tint = FitAIColors.OnSurface)
            }
            Text(strings.profileSettings, style = MaterialTheme.typography.headlineMedium, color = FitAIColors.OnSurface)
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = FitAISpacing.lg)
                .padding(bottom = FitAISpacing.xl),
            verticalArrangement = Arrangement.spacedBy(FitAISpacing.lg),
        ) {
            state.message?.let { key ->
                Text(
                    text = mapProfileMessage(key, strings),
                    color = if (key == "profile_saved" || key == "plans_regenerated") FitAIColors.Success else MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            if (state.editing) {
                SettingsSection(strings.settingsProfileSection) { EditProfileForm(state, viewModel::onIntent) }
                return@Column
            }
            SettingsSection(strings.profileAccount) {
                AccountContent(
                    state = state,
                    onSignIn = onNavigateSignIn,
                    onGoogle = launchGoogle,
                    onApple = launchApple,
                    onSignOut = { viewModel.onIntent(ProfileIntent.SignOut) },
                )
            }
            SettingsSection(strings.settingsSubscription) {
                SettingsRow(
                    icon = FitAIIcons.AutoAwesome,
                    title = if (state.isPro) strings.settingsPlanPro else strings.settingsPlanFree,
                    trailing = if (state.isPro) null else strings.upgradeToPro,
                    onClick = if (state.isPro) null else onNavigatePaywall,
                )
            }
            SettingsSection(strings.settingsProfileSection) {
                ProfileSummary(state, strings)
                Row(horizontalArrangement = Arrangement.spacedBy(FitAISpacing.sm)) {
                    FitAIButton(
                        text = strings.profileEdit,
                        onClick = { viewModel.onIntent(ProfileIntent.StartEdit) },
                        variant = FitAIButtonVariant.Secondary,
                        leadingIcon = FitAIIcons.Edit,
                        enabled = state.localUser != null,
                        modifier = Modifier.weight(1f),
                    )
                    FitAIButton(
                        text = strings.profileRegeneratePlans,
                        onClick = { viewModel.onIntent(ProfileIntent.RegeneratePlans) },
                        enabled = !state.regenerating && state.localUser != null,
                        modifier = Modifier.weight(1f),
                    )
                }
                if (state.regenerating) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(FitAISpacing.sm)) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = FitAIColors.Primary, strokeWidth = 2.dp)
                        Text(strings.profileRegenerating, color = FitAIColors.OnSurfaceMuted)
                    }
                }
            }
            state.reminders?.let { reminders ->
                SettingsSection(strings.notificationsProfileSection) {
                    RemindersSection(
                        reminders = reminders,
                        busy = state.remindersBusy,
                        onEnable = requestNotificationPermission,
                        onDisable = { viewModel.onIntent(ProfileIntent.DisableReminders) },
                        onTimeChange = { hour, minute ->
                            viewModel.onIntent(ProfileIntent.ChangeReminderTime(hour, minute, reminderTexts))
                        },
                    )
                }
            }
            SettingsSection(strings.settingsLanguage) {
                SupportedLanguages.forEach { code ->
                    SettingsRow(
                        icon = null,
                        title = strings.languageName(code),
                        selected = code == strings.languageCode,
                        onClick = { onLanguageChange(code) },
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(FitAISpacing.sm)) {
        Text(title, style = MaterialTheme.typography.labelLarge, color = FitAIColors.OnSurfaceMuted)
        content()
    }
}

@Composable
private fun SettingsRow(
    icon: ImageVector?,
    title: String,
    trailing: String? = null,
    selected: Boolean = false,
    onClick: (() -> Unit)?,
) {
    val content: @Composable () -> Unit = {
        Row(
            modifier = Modifier.padding(FitAISpacing.md),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(FitAISpacing.md),
        ) {
            if (icon != null) Icon(icon, contentDescription = null, tint = FitAIColors.Primary, modifier = Modifier.size(22.dp))
            Text(title, style = MaterialTheme.typography.bodyLarge, color = FitAIColors.OnSurface, modifier = Modifier.weight(1f))
            trailing?.let { Text(it, style = MaterialTheme.typography.titleMedium, color = FitAIColors.Primary) }
            if (selected) Icon(FitAIIcons.Check, contentDescription = null, tint = FitAIColors.Primary)
            if (onClick != null && trailing == null && !selected) {
                Icon(FitAIIcons.ChevronRight, contentDescription = null, tint = FitAIColors.OnSurfaceMuted)
            }
        }
    }
    val border = BorderStroke(1.dp, if (selected) FitAIColors.Primary else FitAIColors.SurfaceVariant)
    if (onClick != null) {
        Surface(onClick = onClick, shape = FitAIShapes.card, color = FitAIColors.Surface, border = border, modifier = Modifier.fillMaxWidth()) {
            content()
        }
    } else {
        Surface(shape = FitAIShapes.card, color = FitAIColors.Surface, border = border, modifier = Modifier.fillMaxWidth()) {
            content()
        }
    }
}

@Composable
private fun AccountContent(
    state: ProfileState,
    onSignIn: () -> Unit,
    onGoogle: () -> Unit,
    onApple: () -> Unit,
    onSignOut: () -> Unit,
) {
    val strings = LocalStrings.current
    val email = state.authUser?.email
    if (state.hasAccount) {
        SettingsRow(
            icon = FitAIIcons.VerifiedUser,
            title = email?.let { strings.settingsSignedInAs(it) } ?: strings.profileAccount,
            onClick = null,
        )
        SettingsRow(
            icon = FitAIIcons.Logout,
            title = strings.profileSignOut,
            onClick = if (state.signingOut) null else onSignOut,
        )
        return
    }
    Text(strings.settingsGuestAccount, style = MaterialTheme.typography.bodyMedium, color = FitAIColors.OnSurfaceMuted)
    Text(strings.profileCloudHint, style = MaterialTheme.typography.bodySmall, color = FitAIColors.OnSurfaceMuted)
    if (!state.isFirebaseAvailable) {
        Text(strings.firebaseUnavailable, style = MaterialTheme.typography.bodySmall, color = FitAIColors.Warning)
    }
    FitAIButton(
        text = strings.profileEmailAuth,
        onClick = onSignIn,
        leadingIcon = FitAIIcons.Mail,
        enabled = state.isFirebaseAvailable,
        modifier = Modifier.fillMaxWidth(),
    )
    FitAIButton(
        text = strings.profileGoogle,
        onClick = onGoogle,
        variant = FitAIButtonVariant.Secondary,
        enabled = state.isFirebaseAvailable,
        modifier = Modifier.fillMaxWidth(),
    )
    FitAIButton(
        text = strings.profileApple,
        onClick = onApple,
        variant = FitAIButtonVariant.Secondary,
        enabled = state.isFirebaseAvailable,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun ProfileSummary(state: ProfileState, strings: Strings) {
    val user = state.localUser ?: return
    InfoRow(strings.profileAge, user.age.toString())
    InfoRow(strings.profileHeight, "${user.heightCm} ${strings.onboardingHeightUnitCm}")
    InfoRow(strings.profileWeight, strings.workoutKg(user.weightKg.toString()))
    InfoRow(strings.profileGoal, strings.goalLabel(user.goal))
    InfoRow(strings.profileActivity, strings.activityLabel(user.activityLevel))
    InfoRow(
        strings.profileEquipment,
        decodeProfileCodes(user.equipment).joinToString { strings.equipmentLabel(it) }.ifBlank { "—" },
    )
    InfoRow(
        strings.profileDiet,
        decodeProfileCodes(user.dietRestrictions).joinToString { strings.dietLabel(it) }
            .ifBlank { strings.dietLabel(DietRestriction.None.code) },
    )
}

@Composable
private fun RemindersSection(
    reminders: ReminderSettings,
    busy: Boolean,
    onEnable: () -> Unit,
    onDisable: () -> Unit,
    onTimeChange: (hour: Int, minute: Int) -> Unit,
) {
    val strings = LocalStrings.current
    var showTimeDialog by remember { mutableStateOf(false) }
    Surface(
        shape = FitAIShapes.card,
        color = FitAIColors.Surface,
        border = BorderStroke(1.dp, FitAIColors.SurfaceVariant),
        modifier = Modifier
            .fillMaxWidth()
            .toggleable(
                value = reminders.enabled,
                enabled = !busy,
                role = Role.Switch,
                onValueChange = { checked -> if (checked) onEnable() else onDisable() },
            ),
    ) {
        Row(
            modifier = Modifier.padding(FitAISpacing.md),
            horizontalArrangement = Arrangement.spacedBy(FitAISpacing.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(FitAIIcons.Notifications, contentDescription = null, tint = FitAIColors.Primary, modifier = Modifier.size(22.dp))
            Text(
                text = strings.notificationsProfileToggle,
                style = MaterialTheme.typography.bodyLarge,
                color = FitAIColors.OnSurface,
                modifier = Modifier.weight(1f),
            )
            Switch(
                checked = reminders.enabled,
                onCheckedChange = null,
                enabled = !busy,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = FitAIColors.OnPrimary,
                    checkedTrackColor = FitAIColors.Primary,
                ),
            )
        }
    }
    if (reminders.enabled) {
        Surface(
            onClick = { showTimeDialog = true },
            enabled = !busy,
            shape = FitAIShapes.card,
            color = FitAIColors.Surface,
            border = BorderStroke(1.dp, FitAIColors.SurfaceVariant),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                modifier = Modifier.padding(FitAISpacing.md),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(strings.notificationsProfileTime, color = FitAIColors.OnSurfaceMuted)
                Text(
                    text = formatReminderTime(reminders.hour, reminders.minute),
                    style = MaterialTheme.typography.titleMedium,
                    color = FitAIColors.Primary,
                )
            }
        }
        if (!reminders.permissionGranted) {
            Text(
                text = strings.notificationsPermissionDenied,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }
    }
    if (showTimeDialog) {
        ReminderTimeDialog(
            initialHour = reminders.hour,
            initialMinute = reminders.minute,
            onDismiss = { showTimeDialog = false },
            onConfirm = { hour, minute ->
                showTimeDialog = false
                onTimeChange(hour, minute)
            },
        )
    }
}

@Composable
private fun ReminderTimeDialog(
    initialHour: Int,
    initialMinute: Int,
    onDismiss: () -> Unit,
    onConfirm: (hour: Int, minute: Int) -> Unit,
) {
    val strings = LocalStrings.current
    var hour by remember { mutableIntStateOf(initialHour) }
    var minute by remember { mutableIntStateOf(initialMinute) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = FitAIColors.Background,
        title = { Text(strings.notificationsProfileTime) },
        text = {
            ReminderTimePicker(
                hour = hour,
                minute = minute,
                onHourChange = { hour = it },
                onMinuteChange = { minute = it },
                strings = strings,
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(hour, minute) }) {
                Text(strings.actionSave, color = FitAIColors.Primary)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(strings.actionCancel)
            }
        },
    )
}

@Composable
private fun InfoRow(label: String, value: String) {
    Surface(
        shape = FitAIShapes.card,
        color = FitAIColors.Surface,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(FitAISpacing.md),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(label, color = FitAIColors.OnSurfaceMuted)
            Text(value, style = MaterialTheme.typography.titleMedium, color = FitAIColors.OnSurface)
        }
    }
}

@Composable
private fun EditProfileForm(state: ProfileState, onIntent: (ProfileIntent) -> Unit) {
    val strings = LocalStrings.current
    FitAITextField(
        value = state.editName,
        onValueChange = { onIntent(ProfileIntent.EditName(it)) },
        label = strings.onboardingLabelName,
    )
    FitAITextField(
        value = state.editAge,
        onValueChange = { onIntent(ProfileIntent.EditAge(it)) },
        label = strings.onboardingLabelAge,
    )
    FitAITextField(
        value = state.editHeightCm,
        onValueChange = { onIntent(ProfileIntent.EditHeight(it)) },
        label = strings.onboardingLabelHeight,
    )
    FitAITextField(
        value = state.editWeightKg,
        onValueChange = { onIntent(ProfileIntent.EditWeight(it)) },
        label = strings.onboardingLabelWeight,
    )
    Text(strings.profileGoal, style = MaterialTheme.typography.titleMedium)
    FitnessGoal.entries.forEach { goal ->
        CompactSelect(
            title = strings.goalLabel(goal.code),
            selected = state.editGoal == goal.code,
            onClick = { onIntent(ProfileIntent.EditGoal(goal.code)) },
        )
    }
    Text(strings.profileActivity, style = MaterialTheme.typography.titleMedium)
    ActivityLevel.entries.forEach { level ->
        CompactSelect(
            title = strings.activityLabel(level.code),
            selected = state.editActivityLevel == level.code,
            onClick = { onIntent(ProfileIntent.EditActivity(level.code)) },
        )
    }
    Text(strings.profileEquipment, style = MaterialTheme.typography.titleMedium)
    EquipmentOption.entries.forEach { option ->
        CompactSelect(
            title = strings.equipmentLabel(option.code),
            selected = option.code in state.editEquipment,
            onClick = { onIntent(ProfileIntent.ToggleEquipment(option.code)) },
            multi = true,
        )
    }
    Text(strings.profileDiet, style = MaterialTheme.typography.titleMedium)
    DietRestriction.entries.forEach { option ->
        CompactSelect(
            title = strings.dietLabel(option.code),
            selected = option.code in state.editDietRestrictions,
            onClick = { onIntent(ProfileIntent.ToggleDiet(option.code)) },
            multi = true,
        )
    }
    Row(horizontalArrangement = Arrangement.spacedBy(FitAISpacing.sm)) {
        FitAIButton(
            text = strings.actionCancel,
            onClick = { onIntent(ProfileIntent.CancelEdit) },
            modifier = Modifier.weight(1f),
            variant = FitAIButtonVariant.Secondary,
            enabled = !state.saving,
        )
        FitAIButton(
            text = strings.actionSave,
            onClick = { onIntent(ProfileIntent.SaveProfile) },
            modifier = Modifier.weight(1f),
            enabled = !state.saving,
        )
    }
}

@Composable
private fun CompactSelect(
    title: String,
    selected: Boolean,
    onClick: () -> Unit,
    multi: Boolean = false,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .toggleable(
                value = selected,
                onValueChange = { onClick() },
                role = if (multi) Role.Checkbox else Role.RadioButton,
            ),
        shape = FitAIShapes.button,
        color = if (selected) FitAIColors.Primary.copy(alpha = 0.05f) else FitAIColors.Surface,
        border = BorderStroke(2.dp, if (selected) FitAIColors.Primary else FitAIColors.SurfaceVariant),
    ) {
        Text(
            text = title,
            modifier = Modifier.padding(FitAISpacing.md),
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}

private fun mapProfileMessage(key: String, strings: Strings): String = when (key) {
    "firebase_unavailable" -> strings.firebaseUnavailable
    "sync_failed" -> strings.syncFailed
    "apple_unsupported_android" -> strings.appleUnsupportedAndroid
    "validation_numbers_required" -> strings.validationNumbersRequired
    "validation_fields_required" -> strings.validationFieldsRequired
    "profile_saved" -> strings.profileSaved
    "plans_regenerated" -> strings.profilePlansRegenerated
    "trial_expired" -> strings.trialExpired
    "reminders_permission_denied" -> strings.notificationsPermissionDenied
    "reminders_update_failed" -> strings.notificationsUpdateFailed
    else -> key.ifBlank { strings.errorUnknown }
}
