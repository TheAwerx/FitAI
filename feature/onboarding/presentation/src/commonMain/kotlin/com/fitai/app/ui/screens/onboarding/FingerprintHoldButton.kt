package com.fitai.app.ui.screens.onboarding

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.unit.dp
import com.fitai.app.ui.theme.FitAIColors
import com.fitai.app.ui.theme.FitAIIcons
import com.fitai.app.ui.theme.FitAISpacing
import kotlinx.coroutines.launch

private const val HOLD_DURATION_MS = 1400

/**
 * Fingerprint drawn from arcs that fill with mint while pressed; confirms after [HOLD_DURATION_MS].
 * Screen readers get a plain click instead of the hold gesture.
 */
@Composable
internal fun FingerprintHoldButton(
    label: String,
    accessibilityLabel: String,
    signed: Boolean,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val progress = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val haptics = LocalHapticFeedback.current
    val currentOnConfirm by rememberUpdatedState(onConfirm)
    val currentSigned by rememberUpdatedState(signed)

    LaunchedEffect(signed) { if (signed) progress.snapTo(1f) }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(FitAISpacing.md),
    ) {
        Box(
            modifier = Modifier
                .size(132.dp)
                .clearAndSetSemantics {
                    role = Role.Button
                    contentDescription = accessibilityLabel
                    onClick(label = accessibilityLabel) {
                        if (!currentSigned) currentOnConfirm()
                        true
                    }
                }
                .pointerInput(Unit) {
                    detectTapGestures(
                        onPress = {
                            if (currentSigned) return@detectTapGestures
                            val fill = scope.launch {
                                val remaining = ((1f - progress.value) * HOLD_DURATION_MS).toInt()
                                progress.animateTo(1f, tween(remaining, easing = LinearEasing))
                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                currentOnConfirm()
                            }
                            tryAwaitRelease()
                            if (progress.value < 1f) {
                                fill.cancel()
                                scope.launch { progress.animateTo(0f, tween(250)) }
                            }
                        },
                    )
                },
            contentAlignment = Alignment.Center,
        ) {
            Canvas(modifier = Modifier.size(132.dp)) {
                val stroke = 3.dp.toPx()
                val rings = 6
                repeat(rings) { index ->
                    val radiusX = size.minDimension / 2 * (0.16f + index * 0.12f)
                    val radiusY = radiusX * 1.18f
                    val center = Offset(size.width / 2, size.height * 0.56f - index * 1.5.dp.toPx())
                    val lit = progress.value > index.toFloat() / rings
                    val start = if (index % 2 == 0) 150f else 175f
                    val sweep = if (index % 2 == 0) 250f else 205f
                    drawArc(
                        color = if (lit) FitAIColors.Primary else FitAIColors.OnSurface.copy(alpha = 0.85f),
                        startAngle = start - index * 4f,
                        sweepAngle = sweep + index * 6f,
                        useCenter = false,
                        topLeft = Offset(center.x - radiusX, center.y - radiusY),
                        size = Size(radiusX * 2, radiusY * 2),
                        style = Stroke(width = stroke, cap = StrokeCap.Round),
                    )
                }
                drawArc(
                    color = FitAIColors.Primary,
                    startAngle = -90f,
                    sweepAngle = 360f * progress.value,
                    useCenter = false,
                    topLeft = Offset(stroke, stroke),
                    size = Size(size.width - 2 * stroke, size.height - 2 * stroke),
                    style = Stroke(width = stroke * 1.5f, cap = StrokeCap.Round),
                )
            }
            if (signed) {
                Icon(FitAIIcons.CheckCircle, contentDescription = null, tint = FitAIColors.Primary, modifier = Modifier.size(48.dp))
            }
        }
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = if (signed) FitAIColors.Primary else FitAIColors.OnSurfaceMuted,
        )
    }
}
