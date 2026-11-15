package com.fitai.app.ui.screens.progress

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import com.fitai.app.engagement.ProgressOverview
import com.fitai.app.localization.LocalStrings
import com.fitai.app.ui.components.FitAIButton
import com.fitai.app.ui.components.FitAIButtonVariant
import com.fitai.app.ui.components.FitAICard
import com.fitai.app.ui.components.FitAITextField
import com.fitai.app.ui.components.SkeletonLoader
import com.fitai.app.ui.theme.FitAIColors
import com.fitai.app.ui.theme.FitAIShapes
import com.fitai.app.ui.theme.FitAISpacing
import com.fitai.app.viewmodel.ProgressViewModel
import com.fitai.app.viewmodel.home.formatOneDecimal
import com.fitai.app.viewmodel.progress.ProgressEffect
import com.fitai.app.viewmodel.progress.ProgressIntent
import com.fitai.app.viewmodel.progress.WeighInForm
import org.koin.compose.viewmodel.koinViewModel

private const val MAX_RECORDS = 10

/** [openWeighIn] opens the weigh-in dialog once; [onWeighInOpened] lets the caller reset its flag. */
@Composable
fun ProgressScreen(
    modifier: Modifier = Modifier,
    openWeighIn: Boolean = false,
    onWeighInOpened: () -> Unit = {},
    viewModel: ProgressViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val strings = LocalStrings.current
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) { viewModel.onIntent(ProgressIntent.Load) }
    LaunchedEffect(openWeighIn) {
        if (openWeighIn) {
            viewModel.onIntent(ProgressIntent.OpenWeighIn)
            onWeighInOpened()
        }
    }
    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            val message = when (effect) {
                ProgressEffect.WeighInSaved -> strings.progressWeighInSaved
                ProgressEffect.SaveFailed -> strings.progressSaveFailed
            }
            snackbarHostState.showSnackbar(message)
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(FitAISpacing.lg),
            verticalArrangement = Arrangement.spacedBy(FitAISpacing.md),
        ) {
            Text(strings.tabProgress, style = MaterialTheme.typography.headlineMedium)
            val overview = state.overview
            when {
                state.loading -> SkeletonLoader()
                overview == null -> Column(verticalArrangement = Arrangement.spacedBy(FitAISpacing.sm)) {
                    Text(strings.progressLoadError, color = FitAIColors.OnSurfaceMuted)
                    FitAIButton(
                        text = strings.actionRetry,
                        onClick = { viewModel.onIntent(ProgressIntent.Load) },
                        variant = FitAIButtonVariant.Secondary,
                    )
                }
                else -> ProgressContent(overview, onAddWeighIn = { viewModel.onIntent(ProgressIntent.OpenWeighIn) })
            }
        }
        SnackbarHost(snackbarHostState, modifier = Modifier.align(Alignment.BottomCenter))
    }

    state.weighIn?.let { form ->
        WeighInDialog(
            form = form,
            onWeightChange = { viewModel.onIntent(ProgressIntent.WeightChanged(it)) },
            onWaistChange = { viewModel.onIntent(ProgressIntent.WaistChanged(it)) },
            onSave = { viewModel.onIntent(ProgressIntent.SaveWeighIn) },
            onDismiss = { viewModel.onIntent(ProgressIntent.DismissWeighIn) },
        )
    }
}

@Composable
private fun ProgressContent(overview: ProgressOverview, onAddWeighIn: () -> Unit) {
    val strings = LocalStrings.current
    WeightCard(overview, onAddWeighIn)

    FitAICard(modifier = Modifier.fillMaxWidth()) {
        SectionHeader(
            title = strings.progressVolumeTitle,
            trailing = overview.weeklyVolumes.lastOrNull()?.let { strings.homeVolumeTonnes(formatOneDecimal(it.volumeKg / 1000.0)) },
        )
        if (overview.weeklyVolumes.all { it.volumeKg <= 0.0 }) {
            EmptyHint(strings.progressVolumeEmpty)
        } else {
            WeeklyVolumeBars(overview.weeklyVolumes, Modifier.padding(top = FitAISpacing.md))
        }
    }

    FitAICard(modifier = Modifier.fillMaxWidth()) {
        SectionHeader(title = strings.progressPrTitle)
        val records = overview.personalRecords.take(MAX_RECORDS)
        if (records.isEmpty()) {
            EmptyHint(strings.progressPrEmpty)
        } else {
            Column(modifier = Modifier.padding(top = FitAISpacing.md)) {
                records.forEachIndexed { index, record -> PersonalRecordRow(record, showDivider = index != records.lastIndex) }
            }
        }
    }

    FitAICard(modifier = Modifier.fillMaxWidth()) {
        SectionHeader(
            title = strings.progressBadgesTitle,
            trailing = strings.progressBadgesCount(overview.badges.count { it.unlocked }, overview.badges.size),
        )
        Box(modifier = Modifier.padding(top = FitAISpacing.md)) { BadgeGrid(overview.badges) }
    }
}

@Composable
private fun WeightCard(overview: ProgressOverview, onAddWeighIn: () -> Unit) {
    val strings = LocalStrings.current
    FitAICard(modifier = Modifier.fillMaxWidth()) {
        SectionHeader(title = strings.progressWeightTitle)
        overview.latestWeightKg?.let { latest ->
            Text(
                text = strings.progressWeightLatest(formatOneDecimal(latest)),
                style = MaterialTheme.typography.headlineSmall,
                color = FitAIColors.OnSurface,
                modifier = Modifier.padding(top = FitAISpacing.sm),
            )
        }
        overview.weightChangeKg?.let { change ->
            val signed = if (change > 0) "+${formatOneDecimal(change)}" else formatOneDecimal(change)
            Text(
                text = strings.progressWeightChange(signed),
                style = MaterialTheme.typography.labelSmall,
                color = FitAIColors.OnSurfaceMuted,
            )
        }
        if (overview.weightPoints.isEmpty()) {
            EmptyHint(strings.progressWeightEmpty)
        } else {
            WeightTrendChart(overview.weightPoints, Modifier.padding(vertical = FitAISpacing.md))
        }
        FitAIButton(
            text = strings.progressAddWeighIn,
            onClick = onAddWeighIn,
            modifier = Modifier.fillMaxWidth().padding(top = FitAISpacing.sm),
        )
    }
}

@Composable
private fun SectionHeader(title: String, trailing: String? = null) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = FitAIColors.OnSurface,
            modifier = Modifier.weight(1f),
        )
        trailing?.let { Text(it, style = MaterialTheme.typography.labelMedium, color = FitAIColors.OnSurfaceMuted) }
    }
}

@Composable
private fun EmptyHint(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = FitAIColors.OnSurfaceMuted,
        modifier = Modifier.padding(top = FitAISpacing.sm),
    )
}

@Composable
private fun WeighInDialog(
    form: WeighInForm,
    onWeightChange: (String) -> Unit,
    onWaistChange: (String) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit,
) {
    val strings = LocalStrings.current
    val decimalKeyboard = KeyboardOptions(keyboardType = KeyboardType.Decimal)
    AlertDialog(
        onDismissRequest = { if (!form.saving) onDismiss() },
        containerColor = FitAIColors.Surface,
        shape = FitAIShapes.card,
        title = {
            Text(strings.progressAddWeighIn, style = MaterialTheme.typography.titleLarge, color = FitAIColors.OnSurface)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(FitAISpacing.sm)) {
                FitAITextField(
                    value = form.weightText,
                    onValueChange = onWeightChange,
                    label = strings.progressWeightLabel,
                    isError = form.weightInvalid,
                    supportingText = strings.progressWeightInvalid.takeIf { form.weightInvalid },
                    keyboardOptions = decimalKeyboard,
                )
                FitAITextField(
                    value = form.waistText,
                    onValueChange = onWaistChange,
                    label = strings.progressWaistLabel,
                    isError = form.waistInvalid,
                    supportingText = strings.progressWaistInvalid.takeIf { form.waistInvalid },
                    keyboardOptions = decimalKeyboard,
                )
            }
        },
        confirmButton = {
            FitAIButton(text = strings.actionSave, onClick = onSave, enabled = !form.saving)
        },
        dismissButton = {
            FitAIButton(
                text = strings.actionCancel,
                onClick = onDismiss,
                variant = FitAIButtonVariant.Ghost,
                enabled = !form.saving,
            )
        },
    )
}
