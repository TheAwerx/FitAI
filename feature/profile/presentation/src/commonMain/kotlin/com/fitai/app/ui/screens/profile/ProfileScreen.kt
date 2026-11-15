package com.fitai.app.ui.screens.profile

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.fitai.app.engagement.WorkoutHistory
import com.fitai.app.engagement.WorkoutHistoryEntry
import com.fitai.app.engagement.WorkoutTrendMetric
import com.fitai.app.engagement.weeklyTrend
import com.fitai.app.localization.LocalStrings
import com.fitai.app.ui.components.ChartSeries
import com.fitai.app.ui.components.FitAIButton
import com.fitai.app.ui.components.FitAILineChart
import com.fitai.app.ui.theme.FitAIColors
import com.fitai.app.ui.theme.FitAIIcons
import com.fitai.app.ui.theme.FitAIShapes
import com.fitai.app.ui.theme.FitAISpacing
import com.fitai.app.viewmodel.ProfileViewModel
import com.fitai.app.viewmodel.profile.ProfileIntent
import com.fitai.app.viewmodel.profile.ProfileState
import com.fitai.app.viewmodel.profile.ProfileTab
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.number
import kotlinx.datetime.plus
import org.koin.compose.viewmodel.koinViewModel
import kotlin.math.roundToInt

/** Profile hub: identity and stats, workout history and trend charts; settings live in [SettingsScreen]. */
@Composable
fun ProfileScreen(
    onOpenSettings: () -> Unit,
    onOpenMeasurements: () -> Unit,
    onLogWorkout: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ProfileViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsState()
    LaunchedEffect(Unit) { viewModel.onIntent(ProfileIntent.Refresh) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(0f to FitAIColors.Primary.copy(alpha = 0.08f), 0.3f to FitAIColors.Background))
            .verticalScroll(rememberScrollState())
            .padding(horizontal = FitAISpacing.lg)
            .padding(top = FitAISpacing.md, bottom = FitAISpacing.lg),
        verticalArrangement = Arrangement.spacedBy(FitAISpacing.lg),
    ) {
        ProfileHeader(state, onEdit = onOpenSettings, onSettings = onOpenSettings)
        Row(horizontalArrangement = Arrangement.spacedBy(FitAISpacing.md)) {
            HubButton(FitAIIcons.Straighten, LocalStrings.current.profileMeasurements, onOpenMeasurements, Modifier.weight(1f))
            HubButton(FitAIIcons.Edit, LocalStrings.current.profileEdit, onOpenSettings, Modifier.weight(1f))
        }
        ProfileTabs(selected = state.tab, onSelect = { viewModel.onIntent(ProfileIntent.SelectTab(it)) })
        when (state.tab) {
            ProfileTab.Workouts -> WorkoutHistoryList(state.history, onLogWorkout)
            ProfileTab.Charts -> TrendCard(state, onIntent = viewModel::onIntent, onLogWorkout = onLogWorkout)
        }
    }
}

@Composable
private fun ProfileHeader(state: ProfileState, onEdit: () -> Unit, onSettings: () -> Unit) {
    val strings = LocalStrings.current
    val name = state.localUser?.name?.trim().orEmpty().ifBlank { strings.profileGuestName }
    Column(verticalArrangement = Arrangement.spacedBy(FitAISpacing.md)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = state.authUser?.email?.takeIf { state.hasAccount } ?: strings.profileTitle,
                style = MaterialTheme.typography.titleMedium,
                color = FitAIColors.OnSurfaceMuted,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            IconButton(onClick = onEdit) {
                Icon(FitAIIcons.Edit, contentDescription = strings.profileEdit, tint = FitAIColors.OnSurface)
            }
            IconButton(onClick = onSettings) {
                Icon(FitAIIcons.Settings, contentDescription = strings.profileSettings, tint = FitAIColors.OnSurface)
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(FitAISpacing.lg)) {
            Box(
                modifier = Modifier
                    .size(88.dp)
                    .background(
                        Brush.linearGradient(listOf(FitAIColors.Primary.copy(alpha = 0.35f), FitAIColors.SurfaceVariant)),
                        CircleShape,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = name.take(1).uppercase(),
                    style = MaterialTheme.typography.displayMedium,
                    color = FitAIColors.OnSurface,
                )
                if (state.isPro) {
                    Text(
                        text = strings.proBadge,
                        style = MaterialTheme.typography.labelSmall,
                        color = FitAIColors.OnPrimary,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .offset(y = 6.dp)
                            .background(FitAIColors.Primary, CircleShape)
                            .padding(horizontal = FitAISpacing.sm, vertical = 2.dp),
                    )
                }
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(FitAISpacing.sm)) {
                Text(name, style = MaterialTheme.typography.headlineLarge, color = FitAIColors.OnSurface, maxLines = 1)
                Row(horizontalArrangement = Arrangement.spacedBy(FitAISpacing.lg)) {
                    StatColumn(state.history?.entries?.size ?: 0, strings.profileStatWorkouts)
                    StatColumn(state.streak?.currentDays ?: 0, strings.profileStatStreak, accent = (state.streak?.currentDays ?: 0) > 0)
                    StatColumn(state.history?.personalRecordCount ?: 0, strings.profileStatRecords)
                }
            }
        }
    }
}

@Composable
private fun StatColumn(value: Int, label: String, accent: Boolean = false) {
    Column {
        Text(
            value.toString(),
            style = MaterialTheme.typography.headlineMedium,
            color = if (accent) FitAIColors.Accent else FitAIColors.OnSurface,
        )
        Text(label, style = MaterialTheme.typography.labelSmall, color = FitAIColors.OnSurfaceMuted, maxLines = 1)
    }
}

@Composable
private fun HubButton(icon: ImageVector, text: String, onClick: () -> Unit, modifier: Modifier) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = FitAIShapes.button,
        color = FitAIColors.Surface,
        border = BorderStroke(1.dp, FitAIColors.SurfaceVariant),
    ) {
        Row(
            modifier = Modifier.padding(vertical = FitAISpacing.md, horizontal = FitAISpacing.sm),
            horizontalArrangement = Arrangement.spacedBy(FitAISpacing.sm, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(icon, contentDescription = null, tint = FitAIColors.Primary, modifier = Modifier.size(20.dp))
            Text(text, style = MaterialTheme.typography.titleMedium, color = FitAIColors.OnSurface, maxLines = 1)
        }
    }
}

@Composable
private fun ProfileTabs(selected: ProfileTab, onSelect: (ProfileTab) -> Unit) {
    val strings = LocalStrings.current
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val tabWidth = maxWidth / ProfileTab.entries.size
        val indicatorOffset by animateDpAsState(tabWidth * selected.ordinal)
        Column {
            Row {
                ProfileTab.entries.forEach { tab ->
                    val active = tab == selected
                    Surface(
                        onClick = { onSelect(tab) },
                        color = FitAIColors.Background.copy(alpha = 0f),
                        modifier = Modifier.width(tabWidth),
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = FitAISpacing.md),
                            horizontalArrangement = Arrangement.spacedBy(FitAISpacing.sm, Alignment.CenterHorizontally),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                imageVector = if (tab == ProfileTab.Workouts) FitAIIcons.FitnessCenter else FitAIIcons.ShowChart,
                                contentDescription = null,
                                tint = if (active) FitAIColors.Primary else FitAIColors.OnSurfaceMuted,
                                modifier = Modifier.size(20.dp),
                            )
                            Text(
                                text = if (tab == ProfileTab.Workouts) strings.profileTabWorkouts else strings.profileTabCharts,
                                style = MaterialTheme.typography.titleMedium,
                                color = if (active) FitAIColors.OnSurface else FitAIColors.OnSurfaceMuted,
                            )
                        }
                    }
                }
            }
            Box(modifier = Modifier.fillMaxWidth().height(2.dp).background(FitAIColors.SurfaceVariant)) {
                Box(
                    modifier = Modifier
                        .offset(x = indicatorOffset)
                        .width(tabWidth)
                        .height(2.dp)
                        .background(FitAIColors.Primary),
                )
            }
        }
    }
}

@Composable
private fun WorkoutHistoryList(history: WorkoutHistory?, onLogWorkout: () -> Unit) {
    val strings = LocalStrings.current
    val entries = history?.entries.orEmpty()
    if (entries.isEmpty()) {
        EmptyState(
            icon = FitAIIcons.FitnessCenter,
            title = strings.profileNoWorkouts,
            body = strings.profileNoWorkoutsBody,
            action = strings.profileLogWorkout,
            onAction = onLogWorkout,
        )
        return
    }
    Column(verticalArrangement = Arrangement.spacedBy(FitAISpacing.md)) {
        entries.forEach { entry -> HistoryCard(entry) }
    }
}

@Composable
private fun HistoryCard(entry: WorkoutHistoryEntry) {
    val strings = LocalStrings.current
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = FitAIShapes.card,
        color = FitAIColors.Surface,
        border = BorderStroke(1.dp, FitAIColors.SurfaceVariant),
    ) {
        Row(
            modifier = Modifier.padding(FitAISpacing.md),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(FitAISpacing.md),
        ) {
            Column(
                modifier = Modifier
                    .size(52.dp)
                    .background(FitAIColors.Primary.copy(alpha = 0.15f), FitAIShapes.button),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(entry.date.day.toString(), style = MaterialTheme.typography.headlineMedium, color = FitAIColors.Primary)
                Text(
                    strings.weekdayShort(entry.date.dayOfWeek.isoDayNumber),
                    style = MaterialTheme.typography.labelSmall,
                    color = FitAIColors.OnSurfaceMuted,
                )
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(FitAISpacing.xs)) {
                Text(
                    text = entry.muscleGroups.take(3).joinToString(" · ").ifBlank { entry.planTitle.orEmpty() },
                    style = MaterialTheme.typography.titleMedium,
                    color = FitAIColors.OnSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    strings.historyMeta(entry.durationMinutes, strings.workoutKg(entry.volumeKg.roundToInt().toString()), entry.completedSets),
                    style = MaterialTheme.typography.bodySmall,
                    color = FitAIColors.OnSurfaceMuted,
                )
                Text(
                    strings.shortDate(entry.date.day, entry.date.month.number),
                    style = MaterialTheme.typography.labelSmall,
                    color = FitAIColors.OnSurfaceMuted,
                )
            }
        }
    }
}

@Composable
private fun TrendCard(state: ProfileState, onIntent: (ProfileIntent) -> Unit, onLogWorkout: () -> Unit) {
    val strings = LocalStrings.current
    val history = state.history
    val buckets = history?.weeklyTrend(state.trendMetric, state.trendWeeks).orEmpty()
    val hasData = buckets.any { it.value > 0.0 }
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = FitAIShapes.card,
        color = FitAIColors.Surface,
        border = BorderStroke(1.dp, FitAIColors.SurfaceVariant),
    ) {
        Column(modifier = Modifier.padding(FitAISpacing.md), verticalArrangement = Arrangement.spacedBy(FitAISpacing.md)) {
            Row(verticalAlignment = Alignment.Top) {
                Column(modifier = Modifier.weight(1f)) {
                    Dropdown(
                        label = strings.trendMetricLabel(state.trendMetric.name),
                        options = WorkoutTrendMetric.entries.map { it to strings.trendMetricLabel(it.name) },
                        onSelect = { onIntent(ProfileIntent.SelectTrendMetric(it)) },
                    )
                    if (buckets.isNotEmpty()) {
                        val first = buckets.first().weekStart
                        val last = buckets.last().weekStart.plus(DatePeriod(days = 6))
                        Text(
                            strings.dateRange(
                                strings.shortDate(first.day, first.month.number),
                                strings.shortDate(last.day, last.month.number),
                            ),
                            style = MaterialTheme.typography.labelSmall,
                            color = FitAIColors.OnSurfaceMuted,
                        )
                    }
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(strings.profilePeriod, style = MaterialTheme.typography.labelSmall, color = FitAIColors.OnSurfaceMuted)
                    Dropdown(
                        label = strings.trendPeriodLabel(state.trendWeeks),
                        options = ProfileState.TREND_PERIODS.map { it to strings.trendPeriodLabel(it) },
                        onSelect = { onIntent(ProfileIntent.SelectTrendWeeks(it)) },
                    )
                }
            }
            if (hasData) {
                val total = buckets.sumOf { it.value }
                Text(
                    text = trendTotal(state.trendMetric, total, strings),
                    style = MaterialTheme.typography.headlineLarge,
                    color = FitAIColors.OnSurface,
                )
                FitAILineChart(
                    series = listOf(ChartSeries(buckets.map { it.value.toFloat() }, showEndDot = true)),
                    height = 180.dp,
                    minValue = 0f,
                    gridLines = 4,
                )
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    listOf(buckets.first(), buckets[buckets.size / 2], buckets.last()).forEach {
                        Text(
                            strings.shortDate(it.weekStart.day, it.weekStart.month.number),
                            style = MaterialTheme.typography.labelSmall,
                            color = FitAIColors.OnSurfaceMuted,
                        )
                    }
                }
            } else {
                EmptyState(
                    icon = FitAIIcons.ShowChart,
                    title = strings.profileChartEmptyTitle,
                    body = strings.profileChartEmptyBody,
                    action = strings.profileLogWorkout,
                    onAction = onLogWorkout,
                    framed = false,
                )
            }
        }
    }
}

private fun trendTotal(metric: WorkoutTrendMetric, total: Double, strings: com.fitai.app.localization.Strings): String =
    when (metric) {
        WorkoutTrendMetric.Volume -> strings.workoutKg(total.roundToInt().toString())
        WorkoutTrendMetric.Duration -> strings.workoutMinutes(total.roundToInt())
        WorkoutTrendMetric.Sessions -> strings.coachStatSessionsValue(total.roundToInt(), 0)
    }

@Composable
internal fun <T> Dropdown(label: String, options: List<Pair<T, String>>, onSelect: (T) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        Surface(onClick = { open = true }, color = FitAIColors.Surface, shape = FitAIShapes.chip) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = FitAISpacing.xs)) {
                Text(label, style = MaterialTheme.typography.titleMedium, color = FitAIColors.OnSurface)
                Icon(FitAIIcons.ExpandMore, contentDescription = null, tint = FitAIColors.OnSurfaceMuted, modifier = Modifier.size(20.dp))
            }
        }
        DropdownMenu(
            expanded = open,
            onDismissRequest = { open = false },
            shape = FitAIShapes.card,
            containerColor = FitAIColors.Surface,
        ) {
            options.forEach { (value, text) ->
                DropdownMenuItem(
                    text = { Text(text, color = FitAIColors.OnSurface) },
                    onClick = {
                        open = false
                        onSelect(value)
                    },
                )
            }
        }
    }
}

@Composable
internal fun EmptyState(
    icon: ImageVector,
    title: String,
    body: String,
    action: String?,
    onAction: () -> Unit,
    framed: Boolean = true,
) {
    val content: @Composable () -> Unit = {
        Column(
            modifier = Modifier.fillMaxWidth().padding(vertical = FitAISpacing.xl, horizontal = FitAISpacing.md),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(FitAISpacing.sm),
        ) {
            Box(
                modifier = Modifier.size(56.dp).background(FitAIColors.SurfaceVariant, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = null, tint = FitAIColors.OnSurface)
            }
            Spacer(Modifier.height(FitAISpacing.xs))
            Text(title, style = MaterialTheme.typography.headlineMedium, color = FitAIColors.OnSurface, textAlign = TextAlign.Center)
            Text(body, style = MaterialTheme.typography.bodyMedium, color = FitAIColors.OnSurfaceMuted, textAlign = TextAlign.Center)
            if (action != null) {
                Spacer(Modifier.height(FitAISpacing.sm))
                FitAIButton(text = action, onClick = onAction, modifier = Modifier.fillMaxWidth())
            }
        }
    }
    if (framed) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = FitAIShapes.card,
            color = FitAIColors.Surface,
            border = BorderStroke(1.dp, FitAIColors.SurfaceVariant),
        ) { content() }
    } else {
        content()
    }
}
