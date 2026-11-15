package com.fitai.app.ui.screens.onboarding

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.fitai.app.localization.LocalStrings
import com.fitai.app.ui.components.ChartSeries
import com.fitai.app.ui.components.FitAILineChart
import com.fitai.app.ui.theme.FitAIColors
import com.fitai.app.ui.theme.FitAIIcons
import com.fitai.app.ui.theme.FitAIShapes
import com.fitai.app.ui.theme.FitAISpacing
import com.fitai.app.viewmodel.onboarding.OnboardingStep
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/** Title + body above (intro) or below (story) a full-width illustration. */
@Composable
internal fun SlideContent(
    title: String,
    body: String,
    visualOnTop: Boolean,
    visual: @Composable () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(FitAISpacing.lg),
    ) {
        if (visualOnTop) visual()
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(FitAISpacing.sm),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.displayMedium,
                color = FitAIColors.OnSurface,
                textAlign = TextAlign.Center,
            )
            Text(
                text = body,
                style = MaterialTheme.typography.bodyLarge,
                color = FitAIColors.OnSurfaceMuted,
                textAlign = TextAlign.Center,
            )
        }
        if (!visualOnTop) visual()
    }
}

@Composable
internal fun IntroSlide(step: OnboardingStep) {
    val strings = LocalStrings.current
    when (step) {
        OnboardingStep.IntroTrack -> SlideContent(strings.onboardingIntroTrackTitle, strings.onboardingIntroTrackBody, false) {
            SetTableMock()
        }
        OnboardingStep.IntroProgress -> SlideContent(
            strings.onboardingIntroProgressTitle,
            strings.onboardingIntroProgressBody,
            false,
        ) { ProgressChartMock() }
        else -> SlideContent(strings.onboardingIntroPlateTitle, strings.onboardingIntroPlateBody, false) { PlateMock() }
    }
}

@Composable
internal fun StorySlide(step: OnboardingStep) {
    val strings = LocalStrings.current
    when (step) {
        OnboardingStep.StoryData -> SlideContent(strings.onboardingStoryDataTitle, strings.onboardingStoryDataBody, true) {
            NoDataVisual()
        }
        OnboardingStep.StorySystem -> SlideContent(
            strings.onboardingStorySystemTitle,
            strings.onboardingStorySystemBody,
            true,
        ) { SystemOrbitVisual() }
        else -> SlideContent(strings.onboardingStoryPlanTitle, strings.onboardingStoryPlanBody, true) { PlanCardMock() }
    }
}

@Composable
private fun MockCard(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .shadow(24.dp, FitAIShapes.card, ambientColor = FitAIColors.Primary.copy(alpha = 0.15f), spotColor = FitAIColors.Primary.copy(alpha = 0.15f)),
        shape = FitAIShapes.card,
        color = FitAIColors.Surface,
        border = BorderStroke(1.dp, FitAIColors.SurfaceVariant),
    ) {
        Column(modifier = Modifier.padding(FitAISpacing.md)) { content() }
    }
}

@Composable
private fun SetTableMock() {
    val strings = LocalStrings.current
    MockCard {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(FitAISpacing.md)) {
            IconWell(icon = FitAIIcons.FitnessCenter, selected = true, size = 40.dp)
            Text(strings.onboardingMockExercise, style = MaterialTheme.typography.titleMedium, color = FitAIColors.OnSurface)
        }
        Spacer(Modifier.height(FitAISpacing.md))
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = FitAISpacing.sm)) {
            listOf(strings.onboardingMockSet, strings.onboardingMockPrevious, strings.onboardingMockKg, strings.onboardingMockReps)
                .forEachIndexed { index, label ->
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall,
                        color = FitAIColors.OnSurfaceMuted,
                        modifier = Modifier.weight(if (index == 1) 1.6f else 1f),
                        textAlign = TextAlign.Center,
                    )
                }
            Spacer(Modifier.width(32.dp))
        }
        Spacer(Modifier.height(FitAISpacing.sm))
        (1..3).forEach { set ->
            val active = set == 1
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(FitAIShapes.button)
                    .background(if (active) FitAIColors.Primary.copy(alpha = 0.12f) else Color.Transparent)
                    .padding(horizontal = FitAISpacing.sm, vertical = FitAISpacing.sm),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                MockCell("$set", Modifier.weight(1f))
                Text(
                    text = strings.workoutPrSet("80", 10),
                    style = MaterialTheme.typography.bodyMedium,
                    color = FitAIColors.OnSurfaceMuted,
                    modifier = Modifier.weight(1.6f),
                    textAlign = TextAlign.Center,
                )
                MockCell("85", Modifier.weight(1f))
                MockCell("10", Modifier.weight(1f))
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .background(if (active) FitAIColors.Primary else FitAIColors.SurfaceVariant, RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        FitAIIcons.Check,
                        contentDescription = null,
                        tint = if (active) FitAIColors.OnPrimary else FitAIColors.OnSurfaceMuted,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun MockCell(text: String, modifier: Modifier) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .background(FitAIColors.SurfaceVariant, RoundedCornerShape(8.dp))
                .padding(horizontal = FitAISpacing.md, vertical = FitAISpacing.xs),
        ) {
            Text(text, style = MaterialTheme.typography.titleMedium, color = FitAIColors.OnSurface)
        }
    }
}

@Composable
private fun ProgressChartMock() {
    val strings = LocalStrings.current
    MockCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(FitAISpacing.xs)) {
                Text(strings.onboardingMockMaxWeight, style = MaterialTheme.typography.labelSmall, color = FitAIColors.OnSurfaceMuted)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(FitAISpacing.sm)) {
                    Text(strings.workoutKg("90"), style = MaterialTheme.typography.headlineMedium, color = FitAIColors.OnSurface)
                    Row(
                        modifier = Modifier
                            .background(FitAIColors.Success.copy(alpha = 0.2f), FitAIShapes.chip)
                            .padding(horizontal = FitAISpacing.sm, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        Icon(FitAIIcons.ShowChart, contentDescription = null, tint = FitAIColors.Success, modifier = Modifier.size(14.dp))
                        Text(strings.percentLabel(11), style = MaterialTheme.typography.labelSmall, color = FitAIColors.Success)
                    }
                }
            }
            Text(strings.onboardingMockLastMonth, style = MaterialTheme.typography.labelSmall, color = FitAIColors.OnSurfaceMuted)
        }
        Text(
            strings.onboardingMockExercise,
            style = MaterialTheme.typography.bodySmall,
            color = FitAIColors.OnSurfaceMuted,
        )
        Spacer(Modifier.height(FitAISpacing.sm))
        FitAILineChart(
            series = listOf(ChartSeries(values = listOf(81f, 83f, 84.5f, 86f, 87f, 88.5f, 89.2f, 90f), showEndDot = true)),
            height = 150.dp,
            minValue = 78f,
            gridLines = 3,
        )
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            (1..4).forEach {
                Text(strings.onboardingMockWeek(it), style = MaterialTheme.typography.labelSmall, color = FitAIColors.OnSurfaceMuted)
            }
        }
    }
}

@Composable
private fun PlateMock() {
    val strings = LocalStrings.current
    MockCard {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp)
                .clip(FitAIShapes.button)
                .background(
                    Brush.radialGradient(
                        listOf(FitAIColors.Accent.copy(alpha = 0.35f), FitAIColors.Primary.copy(alpha = 0.15f), FitAIColors.SurfaceVariant),
                    ),
                ),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .background(FitAIColors.Background.copy(alpha = 0.6f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(FitAIIcons.Restaurant, contentDescription = null, tint = FitAIColors.OnSurface, modifier = Modifier.size(40.dp))
            }
            Icon(
                FitAIIcons.PhotoCamera,
                contentDescription = null,
                tint = FitAIColors.Primary,
                modifier = Modifier.align(Alignment.TopEnd).padding(FitAISpacing.sm).size(24.dp),
            )
        }
        Spacer(Modifier.height(FitAISpacing.md))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                strings.onboardingMockMealName,
                style = MaterialTheme.typography.titleMedium,
                color = FitAIColors.OnSurface,
                modifier = Modifier.weight(1f),
            )
            Text(strings.onboardingPreviewKcal(540), style = MaterialTheme.typography.titleMedium, color = FitAIColors.Primary)
        }
        Text(
            strings.nutritionMealMacros(42, 55, 14),
            style = MaterialTheme.typography.bodyMedium,
            color = FitAIColors.OnSurfaceMuted,
        )
        Spacer(Modifier.height(FitAISpacing.sm))
        Row(horizontalArrangement = Arrangement.spacedBy(FitAISpacing.xs)) {
            listOf(0.78f to FitAIColors.Primary, 0.55f to FitAIColors.Accent, 0.3f to FitAIColors.AccentSecondary).forEach { (fill, color) ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(6.dp)
                        .clip(CircleShape)
                        .background(FitAIColors.SurfaceVariant),
                ) {
                    Box(Modifier.fillMaxWidth(fill).height(6.dp).background(color))
                }
            }
        }
    }
}

@Composable
private fun NoDataVisual() {
    Box(
        modifier = Modifier.fillMaxWidth().height(260.dp),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            fun jagged(values: List<Float>, top: Float, span: Float): Path = Path().apply {
                values.forEachIndexed { index, v ->
                    val x = size.width * index / (values.size - 1)
                    val y = top + (1f - v) * span
                    if (index == 0) moveTo(x, y) else lineTo(x, y)
                }
            }
            val up = listOf(0.2f, 0.45f, 0.3f, 0.6f, 0.4f, 0.75f, 0.55f, 0.85f, 0.7f, 0.95f, 0.8f, 0.9f)
            val down = listOf(0.8f, 0.6f, 0.75f, 0.45f, 0.6f, 0.35f, 0.5f, 0.25f, 0.4f, 0.15f, 0.3f, 0.2f)
            drawPath(
                jagged(up, size.height * 0.05f, size.height * 0.4f),
                color = FitAIColors.Primary.copy(alpha = 0.8f),
                style = Stroke(width = 3.dp.toPx(), join = StrokeJoin.Round),
            )
            drawPath(
                jagged(down, size.height * 0.5f, size.height * 0.45f),
                color = FitAIColors.Error.copy(alpha = 0.8f),
                style = Stroke(width = 3.dp.toPx(), join = StrokeJoin.Round),
            )
        }
        Box(
            modifier = Modifier
                .size(120.dp)
                .shadow(32.dp, CircleShape, ambientColor = FitAIColors.Primary, spotColor = FitAIColors.Primary)
                .background(FitAIColors.Surface, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(FitAIIcons.ShowChart, contentDescription = null, tint = FitAIColors.OnSurfaceMuted, modifier = Modifier.size(56.dp))
        }
    }
}

@Composable
private fun SystemOrbitVisual() {
    val transition = rememberInfiniteTransition()
    val angle by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(durationMillis = 24_000, easing = LinearEasing), RepeatMode.Restart),
    )
    val orbiters: List<Triple<ImageVector, Color, Float>> = listOf(
        Triple(FitAIIcons.LocalFireDepartment, FitAIColors.Accent, 0f),
        Triple(FitAIIcons.Restaurant, FitAIColors.Primary, 72f),
        Triple(FitAIIcons.ShowChart, FitAIColors.AccentSecondary, 144f),
        Triple(FitAIIcons.Trophy, FitAIColors.Warning, 216f),
        Triple(FitAIIcons.AutoAwesome, FitAIColors.Primary, 288f),
    )
    BoxWithConstraints(
        modifier = Modifier.fillMaxWidth().aspectRatio(1.1f),
        contentAlignment = Alignment.Center,
    ) {
        val radius = maxWidth * 0.36f
        Canvas(modifier = Modifier.fillMaxSize()) {
            listOf(0.42f, 0.72f, 1f).forEach { factor ->
                drawCircle(
                    color = FitAIColors.SurfaceVariant,
                    radius = radius.toPx() * factor,
                    style = Stroke(width = 1.dp.toPx()),
                )
            }
        }
        Box(
            modifier = Modifier
                .size(112.dp)
                .shadow(40.dp, RoundedCornerShape(32.dp), ambientColor = FitAIColors.Primary, spotColor = FitAIColors.Primary)
                .background(FitAIColors.Surface, RoundedCornerShape(32.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(FitAIIcons.FitnessCenter, contentDescription = null, tint = FitAIColors.Primary, modifier = Modifier.size(64.dp))
        }
        orbiters.forEach { (icon, tint, start) ->
            val rad = ((angle + start) * kotlin.math.PI / 180.0)
            val dx = (cos(rad) * radius.value).roundToInt()
            val dy = (sin(rad) * radius.value).roundToInt()
            Box(
                modifier = Modifier
                    .offset { IntOffset(dx.dp.roundToPx(), dy.dp.roundToPx()) }
                    .size(48.dp)
                    .background(FitAIColors.Surface, RoundedCornerShape(14.dp))
                    .padding(1.dp),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier.fillMaxSize().background(tint.copy(alpha = 0.15f), RoundedCornerShape(14.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(24.dp))
                }
            }
        }
    }
}

@Composable
private fun PlanCardMock() {
    val strings = LocalStrings.current
    val exercises = strings.onboardingMockPlanExercises
    MockCard(modifier = Modifier.padding(horizontal = FitAISpacing.sm)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    strings.onboardingMockToday + " · " + strings.weekdayShort(3),
                    style = MaterialTheme.typography.labelSmall,
                    color = FitAIColors.OnSurfaceMuted,
                )
                Text(strings.onboardingMockPlanTitle, style = MaterialTheme.typography.headlineMedium, color = FitAIColors.OnSurface)
            }
            IconWell(icon = FitAIIcons.AutoAwesome, selected = true, size = 40.dp)
        }
        Spacer(Modifier.height(FitAISpacing.md))
        exercises.forEachIndexed { index, name ->
            Row(modifier = Modifier.fillMaxWidth().padding(vertical = FitAISpacing.xs)) {
                Text(name, style = MaterialTheme.typography.titleMedium, color = FitAIColors.OnSurface, modifier = Modifier.weight(1f))
                Text(
                    strings.workoutSetsReps(if (index == 2) 3 else 4, "10"),
                    style = MaterialTheme.typography.bodyMedium,
                    color = FitAIColors.OnSurfaceMuted,
                )
            }
        }
        Text(
            strings.onboardingMockPlanMore(2),
            style = MaterialTheme.typography.bodyMedium,
            color = FitAIColors.OnSurfaceMuted,
            modifier = Modifier.padding(top = FitAISpacing.xs),
        )
        Spacer(Modifier.height(FitAISpacing.md))
        Row(horizontalArrangement = Arrangement.spacedBy(FitAISpacing.lg)) {
            MetaChip(FitAIIcons.Schedule, strings.workoutMinutes(55))
            MetaChip(FitAIIcons.FitnessCenter, strings.workoutExercisesCount(6))
        }
    }
}

@Composable
private fun MetaChip(icon: ImageVector, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(FitAISpacing.xs)) {
        Icon(icon, contentDescription = null, tint = FitAIColors.OnSurfaceMuted, modifier = Modifier.size(16.dp))
        Text(text, style = MaterialTheme.typography.bodySmall, color = FitAIColors.OnSurfaceMuted)
    }
}

/** Speech-bubble quote with big No / Yes answers. */
@Composable
internal fun StatementStep(quote: String, onAnswer: (Boolean) -> Unit) {
    val strings = LocalStrings.current
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(FitAISpacing.xl)) {
        StepHeader(title = strings.onboardingStatementTitle)
        Box(modifier = Modifier.fillMaxWidth().padding(vertical = FitAISpacing.lg), contentAlignment = Alignment.Center) {
            Canvas(modifier = Modifier.matchParentSize()) {
                val tail = Path().apply {
                    moveTo(size.width * 0.3f, size.height - 2f)
                    lineTo(size.width * 0.24f, size.height + 36.dp.toPx())
                    lineTo(size.width * 0.44f, size.height - 2f)
                    close()
                }
                drawPath(tail, color = FitAIColors.Surface)
            }
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(32.dp),
                color = FitAIColors.Surface,
                border = BorderStroke(1.dp, FitAIColors.Primary.copy(alpha = 0.25f)),
            ) {
                Text(
                    text = quote,
                    style = MaterialTheme.typography.headlineMedium,
                    fontStyle = FontStyle.Italic,
                    color = FitAIColors.OnSurface,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = FitAISpacing.lg, vertical = FitAISpacing.xxl),
                )
            }
        }
        Spacer(Modifier.height(FitAISpacing.md))
        Row(horizontalArrangement = Arrangement.spacedBy(FitAISpacing.md)) {
            AnswerTile(strings.actionNo, FitAIIcons.Close, FitAIColors.Error, Modifier.weight(1f)) { onAnswer(false) }
            AnswerTile(strings.actionYes, FitAIIcons.Check, FitAIColors.Primary, Modifier.weight(1f)) { onAnswer(true) }
        }
    }
}

@Composable
private fun AnswerTile(label: String, icon: ImageVector, tint: Color, modifier: Modifier, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = modifier.height(132.dp),
        shape = FitAIShapes.card,
        color = FitAIColors.Surface,
        border = BorderStroke(2.dp, FitAIColors.SurfaceVariant),
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(FitAISpacing.md, Alignment.CenterVertically),
        ) {
            Box(
                modifier = Modifier.size(48.dp).background(tint.copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = null, tint = tint)
            }
            Text(label, style = MaterialTheme.typography.titleMedium, color = FitAIColors.OnSurface)
        }
    }
}

/** Lock-screen mock with a FitAI reminder, shown before the system permission prompt. */
@Composable
internal fun NotificationPrimingStep() {
    val strings = LocalStrings.current
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(FitAISpacing.xl),
    ) {
        Box(modifier = Modifier.fillMaxWidth().height(300.dp), contentAlignment = Alignment.TopCenter) {
            Box(
                modifier = Modifier
                    .width(220.dp)
                    .height(300.dp)
                    .clip(RoundedCornerShape(topStart = 40.dp, topEnd = 40.dp))
                    .background(
                        Brush.verticalGradient(listOf(FitAIColors.SurfaceVariant, FitAIColors.Background)),
                    ),
                contentAlignment = Alignment.TopCenter,
            ) {
                Text(
                    strings.onboardingNotifyMockClock,
                    style = MaterialTheme.typography.displayLarge,
                    color = FitAIColors.OnSurfaceMuted.copy(alpha = 0.5f),
                    modifier = Modifier.padding(top = FitAISpacing.xl),
                )
            }
            Surface(
                modifier = Modifier
                    .padding(top = 120.dp)
                    .fillMaxWidth()
                    .shadow(24.dp, FitAIShapes.card, ambientColor = FitAIColors.Primary.copy(alpha = 0.3f), spotColor = FitAIColors.Primary.copy(alpha = 0.3f)),
                shape = FitAIShapes.card,
                color = FitAIColors.Surface,
                border = BorderStroke(1.dp, FitAIColors.Primary.copy(alpha = 0.3f)),
            ) {
                Row(
                    modifier = Modifier.padding(FitAISpacing.md),
                    horizontalArrangement = Arrangement.spacedBy(FitAISpacing.md),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier.size(44.dp).background(FitAIColors.Background, RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(FitAIIcons.FitnessCenter, contentDescription = null, tint = FitAIColors.Primary)
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Row {
                            Text(
                                strings.appName,
                                style = MaterialTheme.typography.labelSmall,
                                color = FitAIColors.OnSurfaceMuted,
                                modifier = Modifier.weight(1f),
                            )
                            Text(strings.onboardingNotifyMockTime, style = MaterialTheme.typography.labelSmall, color = FitAIColors.OnSurfaceMuted)
                        }
                        Text(strings.onboardingNotifyMockTitle, style = MaterialTheme.typography.titleMedium, color = FitAIColors.OnSurface)
                        Text(strings.onboardingNotifyMockBody, style = MaterialTheme.typography.bodyMedium, color = FitAIColors.OnSurfaceMuted)
                    }
                }
            }
        }
        StepHeader(title = strings.onboardingNotifyTitle, subtitle = strings.onboardingNotifyBody, centered = true)
    }
}
