package com.fitai.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import com.fitai.app.ui.theme.FitAIColors
import com.fitai.app.ui.theme.FitAIShapes
import com.fitai.app.ui.theme.FitAISpacing

@Composable
fun FitAICard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val cardModifier = modifier.clip(FitAIShapes.card)
    val inner: @Composable () -> Unit = {
        Column(
            modifier = Modifier.padding(FitAISpacing.md),
            content = content,
        )
    }
    if (onClick != null) {
        Card(
            modifier = cardModifier.clickable(onClick = onClick),
            shape = FitAIShapes.card,
            colors = CardDefaults.cardColors(containerColor = FitAIColors.Surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        ) {
            inner()
        }
    } else {
        Card(
            modifier = cardModifier,
            shape = FitAIShapes.card,
            colors = CardDefaults.cardColors(containerColor = FitAIColors.Surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        ) {
            inner()
        }
    }
}

@Composable
fun FitAIGradientCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val brush = Brush.linearGradient(FitAIColors.GradientPrimary)
    val base = modifier
        .clip(FitAIShapes.card)
        .background(brush)
        .padding(FitAISpacing.md)
    val boxModifier = if (onClick != null) {
        base.clickable(onClick = onClick)
    } else {
        base
    }
    Column(modifier = boxModifier, content = content)
}

@Composable
fun FitAIBorderedCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = modifier,
        shape = FitAIShapes.card,
        colors = CardDefaults.cardColors(containerColor = FitAIColors.Surface),
        border = BorderStroke(1.dp, FitAIColors.SurfaceVariant),
    ) {
        Column(
            modifier = Modifier.padding(FitAISpacing.md),
            content = content,
        )
    }
}
