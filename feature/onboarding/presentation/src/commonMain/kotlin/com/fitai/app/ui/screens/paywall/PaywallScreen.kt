package com.fitai.app.ui.screens.paywall

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import com.fitai.app.billing.SubscriptionPeriod
import com.fitai.app.billing.SubscriptionProduct
import com.fitai.app.localization.LocalStrings
import com.fitai.app.localization.Strings
import com.fitai.app.model.EquipmentOption
import com.fitai.app.model.TrialState
import com.fitai.app.model.TrialStatus
import com.fitai.app.onboarding.OnboardingSummary
import com.fitai.app.trial.TrialPolicy
import com.fitai.app.ui.components.FitAIButton
import com.fitai.app.ui.components.FitAIButtonVariant
import com.fitai.app.ui.theme.FitAIColors
import com.fitai.app.ui.theme.FitAIIcons
import com.fitai.app.ui.theme.FitAIShapes
import com.fitai.app.ui.theme.FitAISpacing
import com.fitai.app.ui.theme.FitAITheme
import com.fitai.app.viewmodel.PaywallViewModel
import com.fitai.app.viewmodel.paywall.PaywallEffect
import com.fitai.app.viewmodel.paywall.PaywallIntent
import com.fitai.app.viewmodel.paywall.PaywallMessage
import com.fitai.app.viewmodel.paywall.PaywallState
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel

/**
 * In the onboarding funnel ([fromOnboarding]) closing opens the exit offer and [onDeclined] continues
 * with the starter plan; elsewhere closing simply calls [onClose].
 */
@Composable
fun PaywallScreen(
    fromOnboarding: Boolean,
    onUnlocked: () -> Unit,
    onDeclined: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PaywallViewModel = koinViewModel(),
) {
    val strings = LocalStrings.current
    val state by viewModel.state.collectAsState()
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                PaywallEffect.Unlocked -> onUnlocked()
                PaywallEffect.Declined -> onDeclined()
                is PaywallEffect.ShowMessage -> scope.launch {
                    snackbar.showSnackbar(paywallMessage(effect.message, strings))
                }
            }
        }
    }

    val close = { if (fromOnboarding) viewModel.onIntent(PaywallIntent.Close) else onClose() }
    NavigationBackHandler(
        state = rememberNavigationEventState(currentInfo = NavigationEventInfo.None),
        isBackEnabled = !state.showExitOffer,
        onBackCompleted = close,
    )

    PaywallContent(
        state = state,
        snackbar = snackbar,
        onIntent = viewModel::onIntent,
        onClose = close,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PaywallContent(
    state: PaywallState,
    snackbar: SnackbarHostState,
    onIntent: (PaywallIntent) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val strings = LocalStrings.current
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(FitAIColors.Background)
            .background(
                Brush.verticalGradient(0f to FitAIColors.Primary.copy(alpha = 0.12f), 0.5f to FitAIColors.Background),
            ),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = FitAISpacing.sm),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onClose, enabled = !state.busy) {
                    Icon(FitAIIcons.Close, contentDescription = strings.actionClose, tint = FitAIColors.OnSurfaceMuted)
                }
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = FitAISpacing.lg)
                    .padding(bottom = FitAISpacing.md),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = strings.paywallChooseTitle,
                    style = MaterialTheme.typography.displayMedium,
                    color = FitAIColors.OnSurface,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(FitAISpacing.sm))
                Text(
                    text = strings.paywallPlanReadyFor(state.summary?.name?.trim().orEmpty()),
                    style = MaterialTheme.typography.bodyLarge,
                    color = FitAIColors.OnSurfaceMuted,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(FitAISpacing.xl))
                PlanSummaryCard(state.summary)
                Spacer(Modifier.height(FitAISpacing.xl))
                state.trialNotice?.takeUnless { state.loading }?.let { trial ->
                    TrialNoticeCard(trial)
                    Spacer(Modifier.height(FitAISpacing.md))
                }
                PlanOptions(state = state, onIntent = onIntent)
                Spacer(Modifier.height(FitAISpacing.md))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(FitAISpacing.xs),
                ) {
                    Icon(FitAIIcons.VerifiedUser, contentDescription = null, tint = FitAIColors.Primary, modifier = Modifier.size(16.dp))
                    Text(strings.paywallNoCommitment, style = MaterialTheme.typography.bodySmall, color = FitAIColors.OnSurfaceMuted)
                }
            }
            PaywallBottomBar(state = state, onIntent = onIntent)
        }
        SnackbarHost(
            hostState = snackbar,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding(),
        )
    }

    if (state.showExitOffer) {
        ModalBottomSheet(
            onDismissRequest = { onIntent(PaywallIntent.DismissExitOffer) },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = FitAIColors.Surface,
            shape = FitAIShapes.bottomSheet,
        ) {
            ExitOfferSheet(state = state, onIntent = onIntent)
        }
    }
}

@Composable
private fun PlanSummaryCard(summary: OnboardingSummary?) {
    val strings = LocalStrings.current
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = FitAIShapes.card,
        color = FitAIColors.Surface,
        border = BorderStroke(1.dp, FitAIColors.Primary.copy(alpha = 0.35f)),
    ) {
        Column(modifier = Modifier.padding(FitAISpacing.md), verticalArrangement = Arrangement.spacedBy(FitAISpacing.md)) {
            if (summary != null) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(FitAISpacing.sm),
                    verticalArrangement = Arrangement.spacedBy(FitAISpacing.sm),
                ) {
                    if (summary.goal.isNotBlank()) SummaryChip(FitAIIcons.Trophy, strings.goalLabel(summary.goal))
                    SummaryChip(FitAIIcons.CalendarToday, strings.paywallSummaryDays(summary.answers.daysPerWeek))
                    SummaryChip(FitAIIcons.Schedule, strings.workoutMinutes(summary.answers.sessionMinutes))
                    SummaryChip(FitAIIcons.FitnessCenter, equipmentLabel(summary.equipment, strings))
                }
            }
            IncludedRow(FitAIIcons.AutoAwesome, strings.paywallIncludesAi)
            IncludedRow(FitAIIcons.ShowChart, strings.paywallIncludesOverload)
            IncludedRow(FitAIIcons.TipsAndUpdates, strings.paywallIncludesCoach)
        }
    }
}

private fun equipmentLabel(codes: Set<String>, strings: Strings): String = when {
    EquipmentOption.FullGym.code in codes || EquipmentOption.Barbell.code in codes -> strings.workoutGym
    codes.isEmpty() || codes == setOf(EquipmentOption.Bodyweight.code) -> strings.workoutBodyweight
    else -> strings.workoutLocationHome
}

@Composable
private fun SummaryChip(icon: ImageVector, text: String) {
    Row(
        modifier = Modifier
            .background(FitAIColors.SurfaceVariant, FitAIShapes.chip)
            .padding(horizontal = FitAISpacing.md, vertical = FitAISpacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(FitAISpacing.xs),
    ) {
        Icon(icon, contentDescription = null, tint = FitAIColors.Primary, modifier = Modifier.size(16.dp))
        Text(text, style = MaterialTheme.typography.labelLarge, color = FitAIColors.OnSurface)
    }
}

@Composable
private fun IncludedRow(icon: ImageVector, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(FitAISpacing.md)) {
        Box(
            modifier = Modifier.size(36.dp).background(FitAIColors.Primary.copy(alpha = 0.2f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = FitAIColors.Primary, modifier = Modifier.size(20.dp))
        }
        Text(text, style = MaterialTheme.typography.titleMedium, color = FitAIColors.OnSurface, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun PlanOptions(
    state: PaywallState,
    onIntent: (PaywallIntent) -> Unit,
) {
    val strings = LocalStrings.current
    when {
        state.loading -> Box(
            modifier = Modifier.fillMaxWidth().height(160.dp),
            contentAlignment = Alignment.Center,
        ) {
            CircularProgressIndicator(color = FitAIColors.Primary)
        }
        state.loadFailed -> Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(FitAISpacing.md),
        ) {
            Text(
                text = strings.paywallLoadError,
                style = MaterialTheme.typography.bodyLarge,
                color = FitAIColors.OnSurfaceMuted,
                textAlign = TextAlign.Center,
            )
            FitAIButton(
                text = strings.actionRetry,
                onClick = { onIntent(PaywallIntent.RetryLoad) },
                variant = FitAIButtonVariant.Secondary,
            )
        }
        else -> Column(
            modifier = Modifier.fillMaxWidth().selectableGroup(),
            verticalArrangement = Arrangement.spacedBy(FitAISpacing.md),
        ) {
            state.products.forEach { product ->
                PlanOptionCard(
                    product = product,
                    trialDays = state.trialDaysFor(product),
                    selected = product.id == state.selectedProductId,
                    enabled = !state.busy,
                    onClick = { onIntent(PaywallIntent.ProductSelected(product.id)) },
                )
            }
        }
    }
}

/** Why the free trial isn't offered: used on this device, used by this account, or already running. */
@Composable
private fun TrialNoticeCard(trial: TrialStatus) {
    val strings = LocalStrings.current
    val (title, body) = when (trial.state) {
        TrialState.Active -> strings.trialActiveTitle(trial.daysRemaining) to strings.trialActiveBody
        TrialState.Expired -> strings.trialUsedByAccountTitle to strings.trialUsedByAccountBody
        else -> strings.trialUsedOnDeviceTitle to strings.trialUsedOnDeviceBody
    }
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = FitAIShapes.card,
        color = FitAIColors.Surface,
        border = BorderStroke(1.dp, FitAIColors.SurfaceVariant),
    ) {
        Row(
            modifier = Modifier.padding(FitAISpacing.md),
            horizontalArrangement = Arrangement.spacedBy(FitAISpacing.md),
        ) {
            Box(
                modifier = Modifier.size(36.dp).background(FitAIColors.SurfaceVariant, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = if (trial.state == TrialState.Active) FitAIIcons.Schedule else FitAIIcons.Lock,
                    contentDescription = null,
                    tint = FitAIColors.OnSurface,
                    modifier = Modifier.size(20.dp),
                )
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(FitAISpacing.xs)) {
                Text(title, style = MaterialTheme.typography.titleMedium, color = FitAIColors.OnSurface)
                Text(body, style = MaterialTheme.typography.bodySmall, color = FitAIColors.OnSurfaceMuted)
            }
        }
    }
}

@Composable
private fun PlanOptionCard(
    product: SubscriptionProduct,
    trialDays: Int,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val strings = LocalStrings.current
    val yearly = product.period == SubscriptionPeriod.Yearly
    val shape = FitAIShapes.card
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .toggleable(value = selected, enabled = enabled, onValueChange = { onClick() }, role = Role.RadioButton),
        shape = shape,
        color = if (selected) FitAIColors.Primary.copy(alpha = 0.08f) else FitAIColors.Surface,
        border = BorderStroke(2.dp, if (selected) FitAIColors.Primary else FitAIColors.SurfaceVariant),
    ) {
        Column {
            product.savingsPercent?.let { percent ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(if (selected) FitAIColors.Primary else FitAIColors.SurfaceVariant)
                        .padding(vertical = FitAISpacing.xs),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = strings.paywallSavingsBadge(percent),
                        style = MaterialTheme.typography.labelLarge,
                        color = if (selected) FitAIColors.OnPrimary else FitAIColors.OnSurface,
                    )
                }
            }
            Row(
                modifier = Modifier.padding(FitAISpacing.md),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(FitAISpacing.md),
            ) {
                RadioMark(selected)
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(FitAISpacing.xs)) {
                    Text(
                        text = if (yearly) strings.paywallYearly else strings.paywallMonthly,
                        style = MaterialTheme.typography.headlineMedium,
                        color = FitAIColors.OnSurface,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(FitAISpacing.xs)) {
                        product.originalPriceLabel?.let {
                            Text(
                                text = it,
                                style = MaterialTheme.typography.bodySmall,
                                color = FitAIColors.OnSurfaceMuted,
                                textDecoration = TextDecoration.LineThrough,
                            )
                        }
                        Text(product.priceLabel, style = MaterialTheme.typography.bodySmall, color = FitAIColors.OnSurfaceMuted)
                    }
                    if (trialDays > 0) {
                        Text(
                            text = strings.paywallTrialDays(trialDays),
                            style = MaterialTheme.typography.labelSmall,
                            color = FitAIColors.Primary,
                        )
                    }
                }
                Text(
                    text = product.pricePerMonthLabel,
                    style = MaterialTheme.typography.titleMedium,
                    color = FitAIColors.OnSurface,
                )
            }
        }
    }
}

@Composable
private fun RadioMark(selected: Boolean) {
    Surface(
        modifier = Modifier.size(24.dp),
        shape = CircleShape,
        color = if (selected) FitAIColors.Primary else Color.Transparent,
        border = if (selected) null else BorderStroke(2.dp, FitAIColors.OnSurfaceMuted.copy(alpha = 0.5f)),
    ) {
        if (selected) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                Icon(FitAIIcons.Check, contentDescription = null, tint = FitAIColors.OnPrimary, modifier = Modifier.size(16.dp))
            }
        }
    }
}

@Composable
private fun PaywallBottomBar(
    state: PaywallState,
    onIntent: (PaywallIntent) -> Unit,
) {
    val strings = LocalStrings.current
    val trialDays = state.selectedTrialDays
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(FitAIColors.Background.copy(alpha = 0.95f))
            .navigationBarsPadding()
            .padding(horizontal = FitAISpacing.lg, vertical = FitAISpacing.md),
        verticalArrangement = Arrangement.spacedBy(FitAISpacing.sm),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        FitAIButton(
            text = if (trialDays > 0) strings.paywallTrialCta(trialDays) else strings.paywallSubscribe,
            onClick = { onIntent(PaywallIntent.Purchase) },
            modifier = Modifier.fillMaxWidth(),
            enabled = state.selectedProduct != null && !state.busy,
            trailingIcon = FitAIIcons.ArrowForward,
        )
        if (trialDays > 0) {
            Text(
                text = strings.paywallTrustLine,
                style = MaterialTheme.typography.bodySmall,
                color = FitAIColors.OnSurfaceMuted,
                textAlign = TextAlign.Center,
            )
        }
        TextButton(onClick = { onIntent(PaywallIntent.Restore) }, enabled = !state.busy) {
            Text(strings.paywallRestore, style = MaterialTheme.typography.labelSmall, color = FitAIColors.OnSurfaceMuted)
        }
    }
}

@Composable
private fun ExitOfferSheet(state: PaywallState, onIntent: (PaywallIntent) -> Unit) {
    val strings = LocalStrings.current
    val weekly = state.yearlyProduct?.pricePerWeekLabel
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = FitAISpacing.lg)
            .padding(bottom = FitAISpacing.lg),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(FitAISpacing.md),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(FitAISpacing.sm)) {
            Icon(FitAIIcons.Warning, contentDescription = null, tint = FitAIColors.Warning, modifier = Modifier.size(20.dp))
            Text(strings.paywallExitWarning, style = MaterialTheme.typography.titleMedium, color = FitAIColors.OnSurface)
        }
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = FitAIShapes.card,
            color = FitAIColors.Primary.copy(alpha = 0.08f),
            border = BorderStroke(2.dp, FitAIColors.Primary),
        ) {
            Column(
                modifier = Modifier.padding(FitAISpacing.lg),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(FitAISpacing.xs),
            ) {
                Text(strings.paywallExitOfferTitle, style = MaterialTheme.typography.headlineMedium, color = FitAIColors.OnSurface)
                if (weekly != null) {
                    Text(
                        strings.paywallExitOfferBody(weekly),
                        style = MaterialTheme.typography.bodyLarge,
                        color = FitAIColors.OnSurface,
                        textAlign = TextAlign.Center,
                    )
                }
                state.yearlyProduct?.let(state::trialDaysFor)?.takeIf { it > 0 }?.let {
                    Text(strings.paywallTrialDays(it), style = MaterialTheme.typography.labelSmall, color = FitAIColors.Primary)
                }
            }
        }
        TextButton(
            onClick = { onIntent(PaywallIntent.Decline) },
            enabled = !state.busy,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(strings.paywallExitDecline, style = MaterialTheme.typography.bodyLarge, color = FitAIColors.OnSurfaceMuted)
        }
        FitAIButton(
            text = strings.paywallExitUnlock,
            onClick = { onIntent(PaywallIntent.PurchaseOffer) },
            enabled = state.yearlyProduct != null && !state.busy,
            trailingIcon = FitAIIcons.ArrowForward,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

private fun paywallMessage(message: PaywallMessage, strings: Strings): String = when (message) {
    PaywallMessage.RestoreSuccess -> strings.paywallRestoreSuccess
    PaywallMessage.RestoreNotFound -> strings.paywallRestoreNotFound
    PaywallMessage.GenericError -> strings.errorUnknown
    PaywallMessage.TrialUsedOnDevice -> strings.trialUsedOnDeviceTitle
    PaywallMessage.TrialUsedByAccount -> strings.trialUsedByAccountTitle
    PaywallMessage.TrialRequiresAccount -> strings.trialRequiresAccount
    PaywallMessage.TrialStartFailed -> strings.trialStartFailed
}

@Preview
@Composable
private fun PaywallPreview() {
    FitAITheme {
        PaywallContent(
            state = PaywallState(
                loading = false,
                products = listOf(
                    SubscriptionProduct(
                        "fitai_pro_yearly", SubscriptionPeriod.Yearly, "999,99 ₺/yıl", "83,33 ₺/ay", 7, 54,
                        originalPriceLabel = "2.159,88 ₺", pricePerWeekLabel = "19,23 ₺",
                    ),
                    SubscriptionProduct("fitai_pro_monthly", SubscriptionPeriod.Monthly, "179,99 ₺/ay", "179,99 ₺/ay", 0, null),
                ),
                selectedProductId = "fitai_pro_yearly",
                trialStatus = TrialStatus(TrialState.UsedOnDevice, trialDays = TrialPolicy.TRIAL_DAYS),
            ),
            snackbar = remember { SnackbarHostState() },
            onIntent = {},
            onClose = {},
        )
    }
}