package com.fitai.app.ui.plangeneration

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.zIndex
import com.fitai.app.access.PlanQuotaErrors
import com.fitai.app.localization.LocalStrings
import com.fitai.app.ui.components.FitAIButton
import com.fitai.app.ui.components.FitAIButtonVariant
import com.fitai.app.ui.components.FitAIQuotaChip
import com.fitai.app.ui.platform.rememberReducedMotion
import com.fitai.app.ui.theme.FitAIColors
import com.fitai.app.ui.theme.FitAIIcons
import com.fitai.app.ui.theme.FitAISpacing
import kotlin.math.abs
import kotlin.math.exp
import kotlin.random.Random
import kotlinx.coroutines.delay

/**
 * Full-screen moment while a workout plan is generated. Seven muscle-group cards shuffle,
 * get picked, then deal into a Monday–Sunday grid that lights up day by day. The loop runs
 * for as long as the request does; the ViewModel keeps it up for [PlanGenerationTiming.MIN_VISIBLE_MS].
 * With reduced motion the cards stay in the grid and only fade.
 */
@Composable
fun PlanGenerationOverlay(
    state: PlanGenerationUiState,
    onRetry: () -> Unit,
    onClose: () -> Unit,
) {
    val running = state.phase == PlanGenerationPhase.Running
    Dialog(
        onDismissRequest = { if (!running) onClose() },
        properties = DialogProperties(
            dismissOnBackPress = !running,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false,
        ),
    ) {
        PlanGenerationContent(state = state, onRetry = onRetry, onClose = onClose)
    }
}

private val OverlayGlow = Brush.verticalGradient(
    0f to FitAIColors.Primary.copy(alpha = 0.14f),
    0.55f to FitAIColors.Background,
)

private const val TICK_MS = 350L
private const val CARD_COUNT = 7

/** One loop of the stage, in ticks: shuffle, pick, deal, light the week, hold, gather. */
private object Cycle {
    const val PICK = 4
    const val DEAL = 8
    const val LIGHT = 9
    const val GATHER = 19
    const val LENGTH = 20
}

private val PlaceholderDays = setOf(1, 2, 4, 5)

@Composable
private fun PlanGenerationContent(
    state: PlanGenerationUiState,
    onRetry: () -> Unit,
    onClose: () -> Unit,
) {
    val strings = LocalStrings.current
    val reducedMotion = rememberReducedMotion()
    val running = state.phase == PlanGenerationPhase.Running
    var tick by remember { mutableIntStateOf(0) }
    LaunchedEffect(running) {
        if (!running) return@LaunchedEffect
        tick = 0
        while (true) {
            delay(TICK_MS)
            tick++
        }
    }
    val elapsedMs = tick * TICK_MS
    val activeStep = when {
        elapsedMs < Cycle.PICK * TICK_MS -> 0
        elapsedMs < Cycle.DEAL * TICK_MS -> 1
        else -> 2
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(FitAIColors.Background)
            .background(OverlayGlow),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = FitAISpacing.lg, vertical = FitAISpacing.xl),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            StatusBadge(phase = state.phase, reducedMotion = reducedMotion)
            Spacer(Modifier.height(FitAISpacing.md))
            Text(
                text = when (state.phase) {
                    PlanGenerationPhase.Running -> strings.planGenTitle
                    PlanGenerationPhase.Success -> strings.planGenDoneTitle
                    PlanGenerationPhase.Failed -> strings.planGenFailedTitle
                },
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                style = MaterialTheme.typography.headlineLarge,
                color = FitAIColors.OnSurface,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(FitAISpacing.sm))
            Text(
                text = when (state.phase) {
                    PlanGenerationPhase.Running -> strings.planGenSubtitle
                    PlanGenerationPhase.Success -> state.planTitle.orEmpty()
                    PlanGenerationPhase.Failed -> planGenerationErrorText(state.errorCode, strings)
                },
                style = MaterialTheme.typography.bodyLarge,
                color = FitAIColors.OnSurfaceMuted,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(FitAISpacing.xl))
            WeekStage(
                phase = state.phase,
                tick = tick,
                trainingDays = state.trainingDays.ifEmpty { PlaceholderDays },
                reducedMotion = reducedMotion,
                activeStep = activeStep,
            )
            Spacer(Modifier.height(FitAISpacing.xl))
            when (state.phase) {
                PlanGenerationPhase.Running -> {
                    StepList(activeStep = activeStep)
                    Spacer(Modifier.height(FitAISpacing.lg))
                    GenerationProgressBar(elapsedMs = elapsedMs)
                }
                PlanGenerationPhase.Success -> {
                    state.quota?.let { quota ->
                        FitAIQuotaChip(
                            label = strings.planQuotaRemaining(quota.remaining, quota.limit),
                            exhausted = quota.exhausted,
                        )
                        Spacer(Modifier.height(FitAISpacing.lg))
                    }
                    FitAIButton(
                        text = strings.planGenDoneAction,
                        onClick = onClose,
                        modifier = Modifier.fillMaxWidth(),
                        trailingIcon = FitAIIcons.ArrowForward,
                    )
                }
                PlanGenerationPhase.Failed -> {
                    if (state.spendsRight && state.errorCode != PlanQuotaErrors.EXHAUSTED) {
                        Text(
                            text = strings.planGenFailedKeptRight,
                            style = MaterialTheme.typography.bodyMedium,
                            color = FitAIColors.Primary,
                            textAlign = TextAlign.Center,
                        )
                        Spacer(Modifier.height(FitAISpacing.lg))
                    }
                    if (state.canRetry) {
                        FitAIButton(
                            text = strings.actionRetry,
                            onClick = onRetry,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Spacer(Modifier.height(FitAISpacing.sm))
                    }
                    FitAIButton(
                        text = strings.actionClose,
                        onClick = onClose,
                        variant = FitAIButtonVariant.Secondary,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}

@Composable
private fun StatusBadge(phase: PlanGenerationPhase, reducedMotion: Boolean) {
    val pulse = if (phase == PlanGenerationPhase.Running && !reducedMotion) {
        val transition = rememberInfiniteTransition()
        transition.animateFloat(
            initialValue = 1f,
            targetValue = 1.35f,
            animationSpec = infiniteRepeatable(tween(900, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        ).value
    } else {
        1f
    }
    val popIn by animateFloatAsState(
        targetValue = if (phase == PlanGenerationPhase.Running) 1f else 1.08f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
    )
    val (icon, tint) = when (phase) {
        PlanGenerationPhase.Running -> FitAIIcons.AutoAwesome to FitAIColors.Primary
        PlanGenerationPhase.Success -> FitAIIcons.Check to FitAIColors.OnPrimary
        PlanGenerationPhase.Failed -> FitAIIcons.Warning to FitAIColors.Warning
    }
    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(84.dp)) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .graphicsLayer {
                    scaleX = pulse
                    scaleY = pulse
                    alpha = (1.6f - pulse).coerceIn(0f, 1f)
                }
                .background(FitAIColors.Primary.copy(alpha = 0.18f), CircleShape),
        )
        Box(
            modifier = Modifier
                .size(56.dp)
                .graphicsLayer {
                    scaleX = popIn
                    scaleY = popIn
                }
                .background(
                    when (phase) {
                        PlanGenerationPhase.Success -> FitAIColors.Primary
                        PlanGenerationPhase.Failed -> FitAIColors.Warning.copy(alpha = 0.18f)
                        PlanGenerationPhase.Running -> FitAIColors.Primary.copy(alpha = 0.2f)
                    },
                    CircleShape,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(28.dp))
        }
    }
}

private enum class StageMode { Shuffle, Pick, Grid, Gather }

private fun stageMode(cycleTick: Int): StageMode = when {
    cycleTick < Cycle.PICK -> StageMode.Shuffle
    cycleTick < Cycle.DEAL -> StageMode.Pick
    cycleTick < Cycle.GATHER -> StageMode.Grid
    else -> StageMode.Gather
}

private val CardWidth = 72.dp
private val CardHeight = 96.dp
private val StageHeight = 200.dp
private val GridGap = 6.dp
private val CardShape = RoundedCornerShape(12.dp)

@Composable
private fun WeekStage(
    phase: PlanGenerationPhase,
    tick: Int,
    trainingDays: Set<Int>,
    reducedMotion: Boolean,
    activeStep: Int,
) {
    val strings = LocalStrings.current
    val labels = strings.planGenMuscleGroups
    val cycleTick = tick % Cycle.LENGTH
    val mode = when {
        phase == PlanGenerationPhase.Success || reducedMotion -> StageMode.Grid
        phase == PlanGenerationPhase.Failed -> StageMode.Gather
        else -> stageMode(cycleTick)
    }
    // Cards from bottom to top of the deck; each shuffle tick pulls one out and puts it on top.
    var deck by remember { mutableStateOf(List(CARD_COUNT) { it }) }
    var lifted by remember { mutableStateOf<Int?>(null) }
    LaunchedEffect(tick, mode) {
        if (mode == StageMode.Shuffle && phase == PlanGenerationPhase.Running) {
            val card = deck[Random.nextInt(0, CARD_COUNT - 1)]
            deck = deck - card + card
            lifted = card
        } else {
            lifted = null
        }
    }
    val pickedCount = if (mode == StageMode.Pick) (cycleTick - Cycle.PICK + 1) * 2 else 0
    val litCount = when {
        phase == PlanGenerationPhase.Success -> CARD_COUNT
        phase == PlanGenerationPhase.Failed -> 0
        reducedMotion -> (activeStep + 1) * CARD_COUNT / 3
        mode == StageMode.Grid -> (cycleTick - Cycle.LIGHT + 1).coerceIn(0, CARD_COUNT)
        else -> 0
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .height(StageHeight),
    ) {
        val tileWidth = ((maxWidth - GridGap * (CARD_COUNT - 1)) / CARD_COUNT).coerceAtMost(CardWidth)
        val gridScale = tileWidth / CardWidth
        val gridStart = (maxWidth - (tileWidth * CARD_COUNT + GridGap * (CARD_COUNT - 1))) / 2
        val centerX = maxWidth / 2
        val centerY = StageHeight / 2
        val gridY = centerY - 10.dp
        val labelY = gridY + CardHeight * gridScale / 2 + FitAISpacing.sm

        repeat(CARD_COUNT) { index ->
            val day = index + 1
            val slot = deck.indexOf(index)
            val fromCenter = slot - CARD_COUNT / 2
            val inGrid = mode == StageMode.Grid
            val picked = mode == StageMode.Pick && slot >= CARD_COUNT - pickedCount
            val isLifted = lifted == index
            val lit = inGrid && day in trainingDays && index < litCount

            val targetX: Dp
            val targetY: Dp
            val targetRotation: Float
            val targetScale: Float
            if (inGrid) {
                targetX = gridStart + tileWidth / 2 + (tileWidth + GridGap) * index
                targetY = gridY
                targetRotation = 0f
                targetScale = gridScale
            } else {
                targetX = centerX + 16.dp * fromCenter
                targetY = centerY + 3.dp * abs(fromCenter) - (if (isLifted) 46.dp else 0.dp) - (if (picked) 12.dp else 0.dp)
                targetRotation = 5f * fromCenter + if (isLifted) (if (index % 2 == 0) 14f else -14f) else 0f
                targetScale = if (isLifted) 1.06f else 1f
            }
            val moveSpec: AnimationSpec<Dp> = when (mode) {
                StageMode.Grid -> tween(520, delayMillis = index * 55, easing = FastOutSlowInEasing)
                StageMode.Gather -> tween(420, delayMillis = (CARD_COUNT - 1 - index) * 35, easing = FastOutSlowInEasing)
                else -> spring(dampingRatio = 0.72f, stiffness = 320f)
            }
            val floatSpec: AnimationSpec<Float> = when (mode) {
                StageMode.Grid -> tween(520, delayMillis = index * 55, easing = FastOutSlowInEasing)
                StageMode.Gather -> tween(420, delayMillis = (CARD_COUNT - 1 - index) * 35, easing = FastOutSlowInEasing)
                else -> spring(dampingRatio = 0.72f, stiffness = 320f)
            }
            val x by animateDpAsState(targetX, if (reducedMotion) tween(0) else moveSpec)
            val y by animateDpAsState(targetY, if (reducedMotion) tween(0) else moveSpec)
            val rotation by animateFloatAsState(targetRotation, if (reducedMotion) tween(0) else floatSpec)
            val scale by animateFloatAsState(targetScale, if (reducedMotion) tween(0) else floatSpec)
            val detailAlpha by animateFloatAsState(if (inGrid) 0f else 1f, tween(260))
            val cardAlpha by animateFloatAsState(
                targetValue = when {
                    phase == PlanGenerationPhase.Failed -> 0.45f
                    inGrid && day !in trainingDays && (litCount >= CARD_COUNT || phase == PlanGenerationPhase.Success) -> 0.45f
                    else -> 1f
                },
                animationSpec = tween(300),
            )
            val border by animateColorAsState(
                when {
                    lit || picked || isLifted -> FitAIColors.Primary
                    else -> FitAIColors.SurfaceVariant
                },
                tween(240),
            )
            val fill by animateColorAsState(
                if (lit) FitAIColors.Primary.copy(alpha = 0.22f) else FitAIColors.Surface,
                tween(240),
            )
            MuscleCard(
                label = labels.getOrElse(index) { "" },
                icon = if (lit && phase == PlanGenerationPhase.Success) FitAIIcons.Check else FitAIIcons.FitnessCenter,
                highlighted = lit || picked || isLifted,
                fill = fill,
                border = border,
                detailAlpha = detailAlpha,
                modifier = Modifier
                    .zIndex(if (inGrid) index.toFloat() else slot.toFloat() + if (isLifted) CARD_COUNT else 0)
                    .offset(x = x - CardWidth / 2, y = y - CardHeight / 2)
                    .graphicsLayer {
                        rotationZ = rotation
                        scaleX = scale
                        scaleY = scale
                        alpha = cardAlpha
                    },
            )
        }
        val dayLabelAlpha by animateFloatAsState(if (mode == StageMode.Grid) 1f else 0f, tween(300, delayMillis = 200))
        repeat(CARD_COUNT) { index ->
            val day = index + 1
            Text(
                text = strings.weekdayShort(day),
                modifier = Modifier
                    .width(tileWidth + GridGap)
                    .offset(x = gridStart + (tileWidth + GridGap) * index - GridGap / 2, y = labelY)
                    .graphicsLayer { alpha = dayLabelAlpha },
                style = MaterialTheme.typography.labelSmall,
                color = if (day in trainingDays && index < litCount) FitAIColors.OnSurface else FitAIColors.OnSurfaceMuted,
                textAlign = TextAlign.Center,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun MuscleCard(
    label: String,
    icon: ImageVector,
    highlighted: Boolean,
    fill: Color,
    border: Color,
    detailAlpha: Float,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .size(CardWidth, CardHeight)
            .clip(CardShape)
            .background(fill)
            .border(1.5.dp, border, CardShape)
            .padding(FitAISpacing.sm),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Box(
            modifier = Modifier
                .size(26.dp)
                .background(
                    if (highlighted) FitAIColors.Primary.copy(alpha = 0.25f) else FitAIColors.SurfaceVariant,
                    CircleShape,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (highlighted) FitAIColors.Primary else FitAIColors.OnSurfaceMuted,
                modifier = Modifier.size(16.dp),
            )
        }
        Column(
            modifier = Modifier.graphicsLayer { alpha = detailAlpha },
            verticalArrangement = Arrangement.spacedBy(FitAISpacing.xs),
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = FitAIColors.OnSurface,
                maxLines = 1,
            )
            SkeletonLine(widthFraction = 0.8f)
            SkeletonLine(widthFraction = 0.5f)
        }
    }
}

@Composable
private fun SkeletonLine(widthFraction: Float) {
    Box(
        modifier = Modifier
            .fillMaxWidth(widthFraction)
            .height(4.dp)
            .background(FitAIColors.SurfaceVariant, CircleShape),
    )
}

@Composable
private fun StepList(activeStep: Int) {
    val strings = LocalStrings.current
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(FitAISpacing.md),
    ) {
        strings.planGenSteps.forEachIndexed { index, text ->
            val done = index < activeStep
            val active = index == activeStep
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(FitAISpacing.sm)) {
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .background(
                            when {
                                done -> FitAIColors.Primary
                                active -> FitAIColors.Primary.copy(alpha = 0.2f)
                                else -> FitAIColors.SurfaceVariant
                            },
                            CircleShape,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    if (done) {
                        Icon(FitAIIcons.Check, contentDescription = null, tint = FitAIColors.OnPrimary, modifier = Modifier.size(14.dp))
                    } else if (active) {
                        Box(Modifier.size(8.dp).background(FitAIColors.Primary, CircleShape))
                    }
                }
                Text(
                    text = text,
                    modifier = if (active) Modifier.semantics { liveRegion = LiveRegionMode.Polite } else Modifier,
                    style = MaterialTheme.typography.bodyLarge,
                    color = when {
                        active -> FitAIColors.OnSurface
                        done -> FitAIColors.OnSurfaceMuted
                        else -> FitAIColors.OnSurfaceMuted.copy(alpha = 0.5f)
                    },
                )
            }
        }
    }
}

/** Creeps toward 90% while the request runs; the overlay switches state when it answers. */
@Composable
private fun GenerationProgressBar(elapsedMs: Long) {
    val target = 0.9f * (1f - exp(-elapsedMs / 6_000f))
    val progress by animateFloatAsState(target, tween(TICK_MS.toInt(), easing = LinearEasing))
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(6.dp)
            .background(FitAIColors.SurfaceVariant, CircleShape),
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(progress.coerceIn(0.02f, 1f))
                .background(FitAIColors.Primary, CircleShape),
        )
    }
}
