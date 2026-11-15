package com.fitai.app.ui.screens.profile

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.fitai.app.engagement.MeasurementEntry
import com.fitai.app.engagement.MeasurementHistory
import com.fitai.app.localization.LocalStrings
import com.fitai.app.localization.Strings
import com.fitai.app.ui.components.ChartSeries
import com.fitai.app.ui.components.FitAIButton
import com.fitai.app.ui.components.FitAIButtonVariant
import com.fitai.app.ui.components.FitAILineChart
import com.fitai.app.ui.components.FitAISegmentedControl
import com.fitai.app.ui.components.FitAITextField
import com.fitai.app.ui.components.dashedOutline
import com.fitai.app.ui.theme.FitAIColors
import com.fitai.app.ui.theme.FitAIIcons
import com.fitai.app.ui.theme.FitAIShapes
import com.fitai.app.ui.theme.FitAISpacing
import com.fitai.app.util.todayLocalDate
import com.fitai.app.viewmodel.MeasurementsViewModel
import com.fitai.app.viewmodel.measurements.MeasurementForm
import com.fitai.app.viewmodel.measurements.MeasurementMetric
import com.fitai.app.viewmodel.measurements.MeasurementsIntent
import com.fitai.app.viewmodel.measurements.MeasurementsState
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.minus
import kotlinx.datetime.number
import org.koin.compose.viewmodel.koinViewModel
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
fun MeasurementsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MeasurementsViewModel = koinViewModel(),
) {
    val strings = LocalStrings.current
    val state by viewModel.state.collectAsState()
    LaunchedEffect(Unit) { viewModel.onIntent(MeasurementsIntent.Load) }

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
            Text(strings.profileMeasurements, style = MaterialTheme.typography.headlineMedium, color = FitAIColors.OnSurface)
        }
        val history = state.history
        if (state.loading || history == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = FitAIColors.Primary) }
            return@Column
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = FitAISpacing.lg)
                .padding(bottom = FitAISpacing.xl),
            verticalArrangement = Arrangement.spacedBy(FitAISpacing.lg),
        ) {
            FitAISegmentedControl(
                options = MeasurementMetric.entries.toList(),
                selected = state.metric,
                label = { if (it == MeasurementMetric.Weight) strings.profileWeight else strings.measurementWaist },
                onSelect = { viewModel.onIntent(MeasurementsIntent.SelectMetric(it)) },
                modifier = Modifier.fillMaxWidth(),
            )
            MeasurementChartCard(state, history, onSelectMonths = { viewModel.onIntent(MeasurementsIntent.SelectMonths(it)) })
            Surface(
                onClick = { viewModel.onIntent(MeasurementsIntent.OpenForm) },
                color = FitAIColors.Background,
                shape = FitAIShapes.card,
                modifier = Modifier.fillMaxWidth().dashedOutline(radius = 16.dp),
            ) {
                Row(
                    modifier = Modifier.padding(FitAISpacing.lg),
                    horizontalArrangement = Arrangement.spacedBy(FitAISpacing.sm, Alignment.CenterHorizontally),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(FitAIIcons.Add, contentDescription = null, tint = FitAIColors.Primary)
                    Text(strings.progressAddWeighIn, style = MaterialTheme.typography.titleMedium, color = FitAIColors.OnSurface)
                }
            }
            EntriesList(history.entries, state.metric, strings)
        }
    }

    state.form?.let { form ->
        MeasurementDialog(
            form = form,
            onWeightChange = { viewModel.onIntent(MeasurementsIntent.WeightChanged(it)) },
            onWaistChange = { viewModel.onIntent(MeasurementsIntent.WaistChanged(it)) },
            onSave = { viewModel.onIntent(MeasurementsIntent.Save) },
            onDismiss = { viewModel.onIntent(MeasurementsIntent.DismissForm) },
        )
    }
}

@Composable
private fun MeasurementChartCard(state: MeasurementsState, history: MeasurementHistory, onSelectMonths: (Int) -> Unit) {
    val strings = LocalStrings.current
    val since = todayLocalDate().minus(DatePeriod(months = state.months))
    val weight = state.metric == MeasurementMetric.Weight
    val inPeriod = history.entries.filter { it.date >= since }.mapNotNull { if (weight) it.weightKg else it.waistCm }
    val baseline = if (weight && (history.startDate ?: since) >= since) listOf(history.startWeightKg) else emptyList()
    val points = (baseline + inPeriod).ifEmpty { if (weight) listOf(history.startWeightKg) else emptyList() }
    val latest = points.lastOrNull()
    val start = if (weight) history.startWeightKg else history.entries.firstNotNullOfOrNull { it.waistCm }
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = FitAIShapes.card,
        color = FitAIColors.Surface,
        border = BorderStroke(1.dp, FitAIColors.SurfaceVariant),
    ) {
        Column(modifier = Modifier.padding(FitAISpacing.md), verticalArrangement = Arrangement.spacedBy(FitAISpacing.sm)) {
            Row(verticalAlignment = Alignment.Top) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = latest?.let { if (weight) strings.workoutKg(oneDecimal(it)) else strings.cmValue(oneDecimal(it)) } ?: "—",
                        style = MaterialTheme.typography.headlineLarge,
                        color = FitAIColors.OnSurface,
                    )
                    if (latest != null && start != null) {
                        val change = latest - start
                        Text(
                            text = if (abs(change) < 0.05) {
                                strings.measurementSameAsStart
                            } else {
                                strings.measurementChange(
                                    (if (change > 0) "+" else "") + oneDecimal(change),
                                    if (weight) strings.onboardingWeightUnit else strings.onboardingHeightUnitCm,
                                )
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = FitAIColors.Primary,
                        )
                    }
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(strings.profilePeriod, style = MaterialTheme.typography.labelSmall, color = FitAIColors.OnSurfaceMuted)
                    Dropdown(
                        label = strings.measurementPeriodLabel(state.months),
                        options = MeasurementsState.PERIODS.map { it to strings.measurementPeriodLabel(it) },
                        onSelect = onSelectMonths,
                    )
                }
            }
            if (points.isEmpty()) {
                Text(
                    strings.measurementEmpty,
                    style = MaterialTheme.typography.bodyMedium,
                    color = FitAIColors.OnSurfaceMuted,
                    modifier = Modifier.padding(vertical = FitAISpacing.xl),
                )
            } else {
                val low = points.min()
                val high = points.max()
                val pad = ((high - low) * 0.3).coerceAtLeast(2.0)
                FitAILineChart(
                    series = listOf(ChartSeries(points.map { it.toFloat() }, showPointDots = true, fill = points.size > 1)),
                    height = 180.dp,
                    minValue = (low - pad).toFloat(),
                    maxValue = (high + pad).toFloat(),
                    gridLines = 5,
                )
            }
        }
    }
}

@Composable
private fun EntriesList(entries: List<MeasurementEntry>, metric: MeasurementMetric, strings: Strings) {
    val rows = entries.reversed().mapNotNull { entry ->
        val value = if (metric == MeasurementMetric.Weight) entry.weightKg else entry.waistCm
        value?.let { entry to it }
    }
    if (rows.isEmpty()) return
    Column(verticalArrangement = Arrangement.spacedBy(FitAISpacing.sm)) {
        Text(strings.measurementEntries, style = MaterialTheme.typography.headlineMedium, color = FitAIColors.OnSurface)
        rows.forEachIndexed { index, (entry, value) ->
            Row(modifier = Modifier.fillMaxWidth().padding(vertical = FitAISpacing.sm), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    strings.longDate(entry.date.day, entry.date.month.number, entry.date.year),
                    style = MaterialTheme.typography.bodyLarge,
                    color = FitAIColors.OnSurface,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    if (metric == MeasurementMetric.Weight) strings.workoutKg(oneDecimal(value)) else strings.cmValue(oneDecimal(value)),
                    style = MaterialTheme.typography.titleMedium,
                    color = FitAIColors.OnSurface,
                )
            }
            if (index != rows.lastIndex) HorizontalDivider(color = FitAIColors.SurfaceVariant)
        }
    }
}

@Composable
private fun MeasurementDialog(
    form: MeasurementForm,
    onWeightChange: (String) -> Unit,
    onWaistChange: (String) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit,
) {
    val strings = LocalStrings.current
    val decimal = KeyboardOptions(keyboardType = KeyboardType.Decimal)
    AlertDialog(
        onDismissRequest = { if (!form.saving) onDismiss() },
        containerColor = FitAIColors.Surface,
        shape = FitAIShapes.card,
        title = { Text(strings.progressAddWeighIn, color = FitAIColors.OnSurface) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(FitAISpacing.sm)) {
                FitAITextField(
                    value = form.weightText,
                    onValueChange = onWeightChange,
                    label = strings.progressWeightLabel,
                    isError = form.weightInvalid,
                    supportingText = strings.progressWeightInvalid.takeIf { form.weightInvalid },
                    keyboardOptions = decimal,
                )
                FitAITextField(
                    value = form.waistText,
                    onValueChange = onWaistChange,
                    label = strings.progressWaistLabel,
                    isError = form.waistInvalid,
                    supportingText = strings.progressWaistInvalid.takeIf { form.waistInvalid },
                    keyboardOptions = decimal,
                )
            }
        },
        confirmButton = { FitAIButton(text = strings.actionSave, onClick = onSave, enabled = !form.saving) },
        dismissButton = {
            FitAIButton(text = strings.actionCancel, onClick = onDismiss, variant = FitAIButtonVariant.Ghost, enabled = !form.saving)
        },
    )
}

private fun oneDecimal(value: Double): String {
    val tenths = (value * 10).roundToInt()
    val sign = if (tenths < 0) "-" else ""
    return "$sign${abs(tenths) / 10}.${abs(tenths) % 10}"
}
