package com.fitai.app.ui.screens.challenge

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.fitai.app.engagement.StarterChallenge
import com.fitai.app.engagement.StarterTask
import com.fitai.app.engagement.StarterTaskId
import com.fitai.app.localization.LocalStrings
import com.fitai.app.localization.Strings
import com.fitai.app.ui.components.FitAIProgressRing
import com.fitai.app.ui.components.dashedOutline
import com.fitai.app.ui.notifications.reminderTexts
import com.fitai.app.ui.platform.rememberNotificationPermissionRequester
import com.fitai.app.ui.theme.FitAIColors
import com.fitai.app.ui.theme.FitAIIcons
import com.fitai.app.ui.theme.FitAIShapes
import com.fitai.app.ui.theme.FitAISpacing
import com.fitai.app.viewmodel.StarterChallengeViewModel
import com.fitai.app.viewmodel.challenge.StarterChallengeEffect
import com.fitai.app.viewmodel.challenge.StarterChallengeIntent
import org.koin.compose.viewmodel.koinViewModel

/**
 * Floating goals pill pinned above the tab bar plus its bottom sheet. [refreshKey] reloads progress
 * (e.g. on tab change); [hidden] hides the pill on nested full-screen flows such as an active workout.
 */
@Composable
fun StarterChallengeHost(
    refreshKey: Any,
    hidden: Boolean,
    onOpenWorkout: () -> Unit,
    onOpenMealPhoto: () -> Unit,
    onOpenWeighIn: () -> Unit,
    onOpenGift: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: StarterChallengeViewModel = koinViewModel(),
) {
    val strings = LocalStrings.current
    val state by viewModel.state.collectAsState()
    val requestNotifications = rememberNotificationPermissionRequester { granted ->
        viewModel.onIntent(StarterChallengeIntent.NotificationsResolved(granted, strings.reminderTexts()))
    }

    LaunchedEffect(refreshKey) { viewModel.onIntent(StarterChallengeIntent.Refresh) }
    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                StarterChallengeEffect.RequestNotifications -> requestNotifications()
                StarterChallengeEffect.OpenWorkout -> onOpenWorkout()
                StarterChallengeEffect.OpenMealPhoto -> onOpenMealPhoto()
                StarterChallengeEffect.OpenWeighIn -> onOpenWeighIn()
                StarterChallengeEffect.OpenGift -> onOpenGift()
            }
        }
    }

    val challenge = state.challenge
    AnimatedVisibility(
        visible = state.visible && !hidden && challenge != null,
        enter = slideInVertically { it } + fadeIn(),
        exit = slideOutVertically { it } + fadeOut(),
        modifier = modifier,
    ) {
        if (challenge != null) {
            StarterChallengePill(challenge, onClick = { viewModel.onIntent(StarterChallengeIntent.OpenSheet) })
        }
    }

    if (state.sheetOpen && challenge != null) {
        StarterChallengeSheet(
            challenge = challenge,
            onDismiss = { viewModel.onIntent(StarterChallengeIntent.CloseSheet) },
            onTask = { viewModel.onIntent(StarterChallengeIntent.TaskClicked(it)) },
            onGift = { viewModel.onIntent(StarterChallengeIntent.OpenGift) },
            onHide = { viewModel.onIntent(StarterChallengeIntent.HideForever) },
        )
    }
}

@Composable
private fun StarterChallengePill(challenge: StarterChallenge, onClick: () -> Unit) {
    val strings = LocalStrings.current
    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = FitAISpacing.md, vertical = FitAISpacing.sm)
            .shadow(16.dp, FitAIShapes.chip, ambientColor = FitAIColors.Primary.copy(alpha = 0.3f), spotColor = FitAIColors.Primary.copy(alpha = 0.3f))
            .semantics { contentDescription = strings.challengeOpenA11y },
        shape = FitAIShapes.chip,
        color = FitAIColors.Surface,
        border = BorderStroke(1.dp, FitAIColors.Primary.copy(alpha = 0.35f)),
    ) {
        ChallengeHeader(challenge, trailing = {
            Icon(FitAIIcons.ChevronRight, contentDescription = null, tint = FitAIColors.OnSurfaceMuted)
        })
    }
}

@Composable
private fun ChallengeHeader(challenge: StarterChallenge, trailing: @Composable () -> Unit) {
    val strings = LocalStrings.current
    Row(
        modifier = Modifier.padding(start = FitAISpacing.sm, end = FitAISpacing.md, top = FitAISpacing.sm, bottom = FitAISpacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(FitAISpacing.md),
    ) {
        FitAIProgressRing(
            progress = challenge.progress,
            size = 52.dp,
            label = strings.percentLabel((challenge.progress * 100).toInt()),
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(strings.challengeTitle, style = MaterialTheme.typography.titleMedium, color = FitAIColors.OnSurface)
            Text(
                text = challenge.nextTask?.let { strings.starterTaskLabel(it.id) } ?: strings.challengeAllDone,
                style = MaterialTheme.typography.labelLarge,
                color = FitAIColors.Primary,
                maxLines = 1,
            )
        }
        trailing()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StarterChallengeSheet(
    challenge: StarterChallenge,
    onDismiss: () -> Unit,
    onTask: (StarterTaskId) -> Unit,
    onGift: () -> Unit,
    onHide: () -> Unit,
) {
    val strings = LocalStrings.current
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = FitAIColors.Surface,
        shape = FitAIShapes.bottomSheet,
    ) {
        Column(modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(bottom = FitAISpacing.md)) {
            Box(modifier = Modifier.padding(horizontal = FitAISpacing.sm)) {
                ChallengeHeader(challenge, trailing = {
                    IconButton(onClick = onDismiss) {
                        Icon(FitAIIcons.ExpandMore, contentDescription = strings.actionClose, tint = FitAIColors.OnSurfaceMuted)
                    }
                })
            }
            HorizontalDivider(color = FitAIColors.SurfaceVariant, modifier = Modifier.padding(top = FitAISpacing.sm))
            challenge.tasks.forEach { task -> TaskRow(task, strings, onClick = { onTask(task.id) }) }
            GiftRow(unlocked = challenge.allDone, onClick = onGift)
            TextButton(onClick = onHide, modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = FitAISpacing.md)) {
                Text(strings.challengeHide, style = MaterialTheme.typography.bodyMedium, color = FitAIColors.OnSurfaceMuted)
            }
        }
    }
}

@Composable
private fun TaskRow(task: StarterTask, strings: Strings, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .background(if (task.done) FitAIColors.Surface else FitAIColors.Primary.copy(alpha = 0.06f))
            .clickable(enabled = !task.done, onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .width(4.dp)
                .fillMaxHeight()
                .background(if (task.done) FitAIColors.Surface else FitAIColors.Primary),
        )
        Text(
            text = strings.starterTaskLabel(task.id),
            style = MaterialTheme.typography.titleMedium,
            color = if (task.done) FitAIColors.OnSurfaceMuted else FitAIColors.OnSurface,
            modifier = Modifier.weight(1f).padding(horizontal = FitAISpacing.lg, vertical = FitAISpacing.md + FitAISpacing.xs),
        )
        Box(modifier = Modifier.padding(end = FitAISpacing.lg)) {
            if (task.done) {
                Box(
                    modifier = Modifier.size(32.dp).background(FitAIColors.Primary, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(FitAIIcons.Check, contentDescription = null, tint = FitAIColors.OnPrimary, modifier = Modifier.size(18.dp))
                }
            } else {
                Box(
                    modifier = Modifier.size(32.dp).border(1.5.dp, FitAIColors.OnSurface, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(FitAIIcons.ChevronRight, contentDescription = null, tint = FitAIColors.OnSurface, modifier = Modifier.size(20.dp))
                }
            }
        }
    }
}

@Composable
private fun GiftRow(unlocked: Boolean, onClick: () -> Unit) {
    val strings = LocalStrings.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                if (unlocked) {
                    Brush.horizontalGradient(listOf(FitAIColors.Primary.copy(alpha = 0.25f), FitAIColors.Surface))
                } else {
                    Brush.horizontalGradient(listOf(FitAIColors.Surface, FitAIColors.Surface))
                },
            )
            .clickable(enabled = unlocked, onClick = onClick)
            .padding(horizontal = FitAISpacing.lg, vertical = FitAISpacing.md + FitAISpacing.xs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(FitAISpacing.md),
    ) {
        Icon(FitAIIcons.Gift, contentDescription = null, tint = if (unlocked) FitAIColors.Primary else FitAIColors.Accent)
        Column(modifier = Modifier.weight(1f)) {
            Text(strings.challengeGift, style = MaterialTheme.typography.titleMedium, color = FitAIColors.OnSurface)
            if (!unlocked) {
                Text(strings.challengeGiftLocked, style = MaterialTheme.typography.bodySmall, color = FitAIColors.OnSurfaceMuted)
            }
        }
        Box(
            modifier = Modifier
                .size(32.dp)
                .then(if (unlocked) Modifier.background(FitAIColors.Primary, CircleShape) else Modifier.dashedOutline(radius = 16.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = if (unlocked) FitAIIcons.ChevronRight else FitAIIcons.Lock,
                contentDescription = null,
                tint = if (unlocked) FitAIColors.OnPrimary else FitAIColors.OnSurfaceMuted,
                modifier = Modifier.size(16.dp),
            )
        }
    }
}

private fun Strings.starterTaskLabel(id: StarterTaskId): String = when (id) {
    StarterTaskId.Notifications -> challengeTaskNotifications
    StarterTaskId.FirstWorkout -> engagementChecklistSession
    StarterTaskId.FirstMealPhoto -> engagementChecklistMealPhoto
    StarterTaskId.FirstWeighIn -> engagementChecklistWeighIn
}
