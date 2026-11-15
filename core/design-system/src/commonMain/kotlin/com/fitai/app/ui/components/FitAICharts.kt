package com.fitai.app.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.fitai.app.ui.theme.FitAIColors
import com.fitai.app.ui.theme.FitAIShapes
import com.fitai.app.ui.theme.FitAISpacing

/** One plotted series of [FitAILineChart]; values share the chart's min/max scale. */
data class ChartSeries(
    val values: List<Float>,
    val color: Color = FitAIColors.Primary,
    val fill: Boolean = true,
    val showEndDot: Boolean = false,
    val showPointDots: Boolean = false,
)

/**
 * Smoothed area/line chart. With [animate] the lines draw in from the left once.
 * [dashedLevel] draws a dashed horizontal reference line at that value (e.g. a plateau).
 */
@Composable
fun FitAILineChart(
    series: List<ChartSeries>,
    modifier: Modifier = Modifier,
    height: Dp = 180.dp,
    minValue: Float? = null,
    maxValue: Float? = null,
    gridLines: Int = 0,
    dashedLevel: Float? = null,
    dashedColor: Color = FitAIColors.Error.copy(alpha = 0.7f),
    animate: Boolean = true,
) {
    val reveal = remember { Animatable(if (animate) 0f else 1f) }
    LaunchedEffect(series) {
        if (animate) {
            reveal.snapTo(0f)
            reveal.animateTo(1f, tween(durationMillis = 1200, easing = FastOutSlowInEasing))
        }
    }
    val all = series.flatMap { it.values }
    val low = minValue ?: all.minOrNull() ?: 0f
    val high = (maxValue ?: all.maxOrNull() ?: 1f).let { if (it - low < 0.001f) low + 1f else it }
    Canvas(modifier = modifier.fillMaxWidth().height(height)) {
        val pad = 8.dp.toPx()
        val chartHeight = size.height - 2 * pad
        fun yOf(value: Float) = pad + (1f - (value - low) / (high - low)) * chartHeight

        repeat(gridLines) { index ->
            val y = pad + chartHeight * index / (gridLines - 1).coerceAtLeast(1)
            drawLine(
                color = FitAIColors.SurfaceVariant,
                start = Offset(0f, y),
                end = Offset(size.width, y),
                strokeWidth = 1.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f)),
            )
        }
        dashedLevel?.let { level ->
            val y = yOf(level)
            drawLine(
                color = dashedColor,
                start = Offset(0f, y),
                end = Offset(size.width, y),
                strokeWidth = 1.5.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 10f)),
            )
        }
        clipRect(right = size.width * reveal.value) {
            series.forEach { s -> drawSeries(s, pad, ::yOf) }
        }
    }
}

private fun DrawScope.drawSeries(series: ChartSeries, pad: Float, yOf: (Float) -> Float) {
    val values = series.values
    if (values.isEmpty()) return
    val width = size.width - 2 * pad
    val points = if (values.size == 1) {
        listOf(Offset(size.width / 2, yOf(values.first())))
    } else {
        values.mapIndexed { index, v -> Offset(pad + width * index / (values.size - 1), yOf(v)) }
    }
    if (points.size > 1) {
        val line = smoothPath(points)
        if (series.fill) {
            val area = Path().apply {
                addPath(line)
                lineTo(points.last().x, size.height)
                lineTo(points.first().x, size.height)
                close()
            }
            drawPath(
                path = area,
                brush = Brush.verticalGradient(
                    colors = listOf(series.color.copy(alpha = 0.35f), series.color.copy(alpha = 0f)),
                ),
            )
        }
        drawPath(
            path = line,
            color = series.color,
            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round),
        )
    }
    if (series.showPointDots || points.size == 1) {
        points.forEach { drawCircle(series.color, radius = 4.dp.toPx(), center = it) }
    }
    if (series.showEndDot) {
        val end = points.last()
        drawCircle(series.color.copy(alpha = 0.3f), radius = 12.dp.toPx(), center = end)
        drawCircle(series.color, radius = 8.dp.toPx(), center = end)
    }
}

private fun smoothPath(points: List<Offset>): Path = Path().apply {
    moveTo(points.first().x, points.first().y)
    for (i in 1 until points.size) {
        val prev = points[i - 1]
        val curr = points[i]
        val midX = (prev.x + curr.x) / 2
        cubicTo(midX, prev.y, midX, curr.y, curr.x, curr.y)
    }
}

/** Circular progress with the percentage in the middle; used by goal pills and plan generation. */
@Composable
fun FitAIProgressRing(
    progress: Float,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    strokeWidth: Dp = 4.dp,
    label: String? = null,
    trackColor: Color = FitAIColors.SurfaceVariant,
    progressColor: Color = FitAIColors.Primary,
) {
    Box(modifier = modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.size(size)) {
            val stroke = strokeWidth.toPx()
            val diameter = this.size.minDimension - stroke
            val arcSize = Size(diameter, diameter)
            val topLeft = Offset(stroke / 2, stroke / 2)
            drawArc(trackColor, -90f, 360f, false, topLeft, arcSize, style = Stroke(stroke))
            drawArc(
                color = progressColor,
                startAngle = -90f,
                sweepAngle = 360f * progress.coerceIn(0f, 1f),
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(stroke, cap = StrokeCap.Round),
            )
        }
        if (label != null) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = FitAIColors.OnSurface,
                textAlign = TextAlign.Center,
            )
        }
    }
}

/** Segmented control: track `SurfaceVariant`, selected segment `Primary` / `OnPrimary`. */
@Composable
fun <T> FitAISegmentedControl(
    options: List<T>,
    selected: T,
    label: (T) -> String,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    highlightSelected: Boolean = true,
) {
    Row(
        modifier = modifier
            .clip(FitAIShapes.chip)
            .background(FitAIColors.SurfaceVariant)
            .padding(FitAISpacing.xs)
            .selectableGroup(),
    ) {
        options.forEach { option ->
            val isSelected = highlightSelected && option == selected
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(FitAIShapes.chip)
                    .background(if (isSelected) FitAIColors.Primary else Color.Transparent)
                    .selectable(
                        selected = isSelected,
                        enabled = enabled,
                        role = Role.Tab,
                        onClick = { onSelect(option) },
                    )
                    .padding(vertical = FitAISpacing.sm + FitAISpacing.xs),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = label(option),
                    style = MaterialTheme.typography.titleMedium,
                    color = if (isSelected) FitAIColors.OnPrimary else FitAIColors.OnSurfaceMuted,
                    maxLines = 1,
                )
            }
        }
    }
}

/** Dashed rounded outline drawn over the content, e.g. for "add entry" or empty-photo placeholders. */
fun Modifier.dashedOutline(color: Color = FitAIColors.OnSurfaceMuted.copy(alpha = 0.4f), radius: Dp = 12.dp): Modifier =
    drawWithContent {
        drawContent()
        val stroke = 1.5.dp.toPx()
        val r = radius.toPx()
        val rect = Rect(Offset(stroke / 2, stroke / 2), Size(size.width - stroke, size.height - stroke))
        drawRoundRect(
            color = color,
            topLeft = rect.topLeft,
            size = rect.size,
            cornerRadius = CornerRadius(r, r),
            style = Stroke(width = stroke, pathEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 10f))),
        )
    }
