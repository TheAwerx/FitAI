package com.fitai.app.ui.screens.onboarding

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.fitai.app.ui.components.FitAIButton
import com.fitai.app.ui.components.FitAIButtonVariant
import com.fitai.app.ui.theme.FitAIColors
import com.fitai.app.ui.theme.FitAIIcons
import com.fitai.app.ui.theme.FitAISpacing

enum class OnboardingProgressVariant {
    ThinBar,
    ThickRounded,
}

/** Back button and the flow progress on one row; [progress] null hides the bar (intro and story slides). */
@Composable
fun OnboardingProgressHeader(
    progress: Float?,
    onBack: (() -> Unit)?,
    backContentDescription: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .height(64.dp)
            .padding(horizontal = FitAISpacing.lg),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(FitAISpacing.md),
    ) {
        if (onBack != null) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.size(40.dp),
                colors = IconButtonDefaults.iconButtonColors(containerColor = FitAIColors.Surface),
            ) {
                Icon(
                    imageVector = FitAIIcons.ChevronLeft,
                    contentDescription = backContentDescription,
                    tint = FitAIColors.OnSurface,
                )
            }
        } else {
            Spacer(Modifier.width(40.dp))
        }
        if (progress != null) {
            val animated by animateFloatAsState(progress)
            OnboardingProgressBar(
                progress = animated,
                variant = OnboardingProgressVariant.ThickRounded,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
fun OnboardingProgressBar(
    progress: Float,
    variant: OnboardingProgressVariant,
    modifier: Modifier = Modifier,
) {
    val trackHeight = if (variant == OnboardingProgressVariant.ThickRounded) 6.dp else 4.dp
    val shape = if (variant == OnboardingProgressVariant.ThickRounded) CircleShape else RectangleShape
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(trackHeight)
            .clip(shape)
            .background(FitAIColors.SurfaceVariant),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(progress.coerceIn(0f, 1f))
                .height(trackHeight)
                .clip(shape)
                .background(FitAIColors.Primary),
        )
    }
}

/** Pinned bar with the single primary action of a step. */
@Composable
fun OnboardingPrimaryBar(
    text: String,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    trailingIcon: ImageVector? = FitAIIcons.ChevronRight,
    containerColor: Color = FitAIColors.Background.copy(alpha = 0.95f),
    above: (@Composable () -> Unit)? = null,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(containerColor)
            .navigationBarsPadding()
            .padding(horizontal = FitAISpacing.lg, vertical = FitAISpacing.md),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(FitAISpacing.md),
    ) {
        above?.invoke()
        FitAIButton(
            text = text,
            onClick = onClick,
            modifier = Modifier.fillMaxWidth(),
            variant = FitAIButtonVariant.Primary,
            enabled = enabled,
            trailingIcon = trailingIcon,
        )
    }
}

/** Two-button bar kept for screens that offer a secondary way out. */
@Composable
fun OnboardingBottomBar(
    backText: String,
    continueText: String,
    backEnabled: Boolean,
    continueEnabled: Boolean,
    onBack: () -> Unit,
    onContinue: () -> Unit,
    modifier: Modifier = Modifier,
    containerColor: Color = FitAIColors.Background.copy(alpha = 0.95f),
    showBackChevron: Boolean = true,
    continueTrailingIcon: ImageVector? = FitAIIcons.ChevronRight,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(containerColor)
            .navigationBarsPadding()
            .padding(horizontal = FitAISpacing.lg, vertical = FitAISpacing.md),
        horizontalArrangement = Arrangement.spacedBy(FitAISpacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        FitAIButton(
            text = backText,
            onClick = onBack,
            modifier = Modifier.weight(1f),
            variant = FitAIButtonVariant.Secondary,
            enabled = backEnabled,
            leadingIcon = if (showBackChevron) FitAIIcons.ChevronLeft else null,
        )
        FitAIButton(
            text = continueText,
            onClick = onContinue,
            modifier = Modifier.weight(1f),
            variant = FitAIButtonVariant.Primary,
            enabled = continueEnabled,
            trailingIcon = continueTrailingIcon,
        )
    }
}

/** Active page is a wide mint capsule, others are small dots. */
@Composable
fun OnboardingPageDots(
    count: Int,
    selected: Int,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(FitAISpacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(count) { index ->
            val active = index == selected
            val width by animateDpAsState(if (active) 28.dp else 8.dp)
            val color by animateColorAsState(if (active) FitAIColors.Primary else FitAIColors.SurfaceVariant)
            Box(
                modifier = Modifier
                    .height(8.dp)
                    .width(width)
                    .clip(CircleShape)
                    .background(color),
            )
        }
    }
}

@Composable
internal fun StepHeader(
    title: String,
    subtitle: String? = null,
    centered: Boolean = false,
    largeTitle: Boolean = false,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (centered) Alignment.CenterHorizontally else Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(FitAISpacing.sm),
    ) {
        Text(
            text = title,
            style = if (largeTitle) MaterialTheme.typography.displayLarge else MaterialTheme.typography.displayMedium,
            color = FitAIColors.OnSurface,
            textAlign = if (centered) TextAlign.Center else TextAlign.Start,
            modifier = Modifier.fillMaxWidth(),
        )
        if (subtitle != null) {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyLarge,
                color = FitAIColors.OnSurfaceMuted,
                textAlign = if (centered) TextAlign.Center else TextAlign.Start,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
