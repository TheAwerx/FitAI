package com.fitai.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.SnapPosition
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListLayoutInfo
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.fitai.app.ui.theme.FitAIColors
import com.fitai.app.ui.theme.FitAISpacing
import kotlin.math.abs
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull

@Composable
fun FitAINumericPicker(
    value: Int,
    onValueChange: (Int) -> Unit,
    range: IntRange,
    unit: String,
    modifier: Modifier = Modifier,
    itemWidth: Dp = 72.dp,
    format: (Int) -> String = { it.toString() },
    values: List<Int>? = null,
) {
    val items = remember(range, values) { values ?: range.toList() }
    val initialIndex = (items.indexOf(value)).coerceAtLeast(0)
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = initialIndex)
    val fling = rememberSnapFlingBehavior(
        lazyListState = listState,
        snapPosition = SnapPosition.Center,
    )

    LaunchedEffect(value, items) {
        val index = items.indexOf(value)
        if (index < 0 || listState.isScrollInProgress) return@LaunchedEffect
        if (listState.layoutInfo.centeredItemIndex() != index) {
            listState.animateScrollToItem(index)
        }
    }

    LaunchedEffect(listState, items, onValueChange) {
        snapshotFlow { listState.layoutInfo.centeredItemIndex() }
            .filterNotNull()
            .distinctUntilChanged()
            .collect { index ->
                items.getOrNull(index)?.let(onValueChange)
            }
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = format(value),
                style = MaterialTheme.typography.displayLarge,
                color = FitAIColors.Primary,
            )
            Spacer(Modifier.width(FitAISpacing.sm))
            Text(
                text = unit,
                style = MaterialTheme.typography.headlineMedium,
                color = FitAIColors.OnSurfaceMuted,
                modifier = Modifier.padding(bottom = FitAISpacing.xs),
            )
        }
        Spacer(Modifier.height(FitAISpacing.xl))
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp)
                .clipToBounds(),
        ) {
            val sidePad = (maxWidth - itemWidth) / 2
            LazyRow(
                state = listState,
                flingBehavior = fling,
                contentPadding = PaddingValues(horizontal = sidePad),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                itemsIndexed(items, key = { _, item -> item }) { _, item ->
                    val selected = item == value
                    Text(
                        text = format(item),
                        modifier = Modifier
                            .width(itemWidth)
                            .clickable { onValueChange(item) },
                        textAlign = TextAlign.Center,
                        style = if (selected) {
                            MaterialTheme.typography.headlineLarge
                        } else {
                            MaterialTheme.typography.headlineMedium
                        },
                        color = if (selected) {
                            FitAIColors.Primary
                        } else {
                            FitAIColors.OnSurfaceMuted.copy(alpha = 0.35f)
                        },
                    )
                }
            }
            FadeEdge(Alignment.CenterStart)
            FadeEdge(Alignment.CenterEnd)
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .width(2.dp)
                    .height(12.dp)
                    .background(FitAIColors.Primary),
            )
        }
    }
}

private fun LazyListLayoutInfo.centeredItemIndex(): Int? {
    val visible = visibleItemsInfo
    if (visible.isEmpty()) return null
    val center = (viewportStartOffset + viewportEndOffset) / 2
    return visible.minByOrNull { item ->
        abs((item.offset + item.size / 2) - center)
    }?.index
}

@Composable
private fun BoxScope.FadeEdge(alignment: Alignment) {
    Box(
        modifier = Modifier
            .fillMaxHeight()
            .width(48.dp)
            .align(alignment)
            .background(
                Brush.horizontalGradient(
                    colors = if (alignment == Alignment.CenterStart) {
                        listOf(FitAIColors.Background, Color.Transparent)
                    } else {
                        listOf(Color.Transparent, FitAIColors.Background)
                    },
                ),
            ),
    )
}
