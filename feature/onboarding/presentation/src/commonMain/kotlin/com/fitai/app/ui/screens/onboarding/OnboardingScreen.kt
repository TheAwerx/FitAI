package com.fitai.app.ui.screens.onboarding

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import com.fitai.app.localization.LocalStrings
import com.fitai.app.localization.Strings
import com.fitai.app.ui.platform.rememberNotificationPermissionRequester
import com.fitai.app.ui.theme.FitAIColors
import com.fitai.app.ui.theme.FitAISpacing
import com.fitai.app.ui.theme.FitAITheme
import com.fitai.app.viewmodel.OnboardingViewModel
import com.fitai.app.viewmodel.onboarding.OnboardingEffect
import com.fitai.app.viewmodel.onboarding.OnboardingIntent
import com.fitai.app.viewmodel.onboarding.OnboardingPhase
import com.fitai.app.viewmodel.onboarding.OnboardingState
import com.fitai.app.viewmodel.onboarding.OnboardingStep
import com.fitai.app.viewmodel.onboarding.StepKind
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun OnboardingScreen(
    onNavigatePaywall: () -> Unit,
    onBackToWelcome: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: OnboardingViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                OnboardingEffect.NavigateToPaywall -> onNavigatePaywall()
                OnboardingEffect.NavigateBackToWelcome -> onBackToWelcome()
            }
        }
    }

    NavigationBackHandler(
        state = rememberNavigationEventState(currentInfo = NavigationEventInfo.None),
        isBackEnabled = state.phase != OnboardingPhase.Analyzing,
        onBackCompleted = { viewModel.onIntent(OnboardingIntent.PreviousStep) },
    )

    val requestNotifications = rememberNotificationPermissionRequester { granted ->
        viewModel.onIntent(OnboardingIntent.NotificationsResolved(granted))
    }

    Box(modifier = modifier.fillMaxSize().background(FitAIColors.Background)) {
        AnimatedContent(
            targetState = state.phase,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
        ) { phase ->
            when (phase) {
                OnboardingPhase.Profile -> ProfileSteps(
                    state = state,
                    onIntent = viewModel::onIntent,
                    onRequestNotifications = requestNotifications,
                )
                OnboardingPhase.Analyzing -> AnalyzingPhase(state)
                OnboardingPhase.SaveError -> SaveErrorPhase(
                    onRetry = { viewModel.onIntent(OnboardingIntent.RetrySave) },
                    onBack = { viewModel.onIntent(OnboardingIntent.PreviousStep) },
                )
                OnboardingPhase.Result -> ResultPhase(state, viewModel::onIntent)
                OnboardingPhase.Quote -> QuotePhase(onContinue = { viewModel.onIntent(OnboardingIntent.ContinueFromQuote) })
                OnboardingPhase.Pledge -> PledgePhase(state, viewModel::onIntent)
            }
        }
    }
}

@Composable
private fun ProfileSteps(
    state: OnboardingState,
    onIntent: (OnboardingIntent) -> Unit,
    onRequestNotifications: () -> Unit,
) {
    val strings = LocalStrings.current
    val step = state.step
    val kind = step.kind
    val glow = Brush.verticalGradient(0f to FitAIColors.Primary.copy(alpha = 0.07f), 0.35f to FitAIColors.Background)
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(glow)
            .imePadding(),
    ) {
        OnboardingProgressHeader(
            progress = if (kind == StepKind.Question || kind == StepKind.Statement) state.progress else null,
            onBack = if (step.ordinal == 0) null else ({ onIntent(OnboardingIntent.PreviousStep) }),
            backContentDescription = strings.actionBack,
        )
        AnimatedContent(
            targetState = step,
            transitionSpec = {
                val forward = targetState.ordinal > initialState.ordinal
                (slideInHorizontally { if (forward) it / 3 else -it / 3 } + fadeIn()) togetherWith
                    (slideOutHorizontally { if (forward) -it / 3 else it / 3 } + fadeOut())
            },
            modifier = Modifier.weight(1f),
        ) { current ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = FitAISpacing.lg)
                    .padding(top = FitAISpacing.md, bottom = FitAISpacing.md),
                verticalArrangement = if (current.kind == StepKind.Question) Arrangement.Top else Arrangement.Center,
            ) {
                StepBody(current, state, onIntent)
                state.errorMessage?.let { key ->
                    Spacer(Modifier.height(FitAISpacing.md))
                    ValidationText(mapOnboardingError(key, strings))
                }
            }
        }
        if (kind != StepKind.Statement) {
            OnboardingPrimaryBar(
                text = if (step == OnboardingStep.Notifications) strings.notificationsRemind else strings.actionContinue,
                enabled = state.canContinue,
                onClick = {
                    if (step == OnboardingStep.Notifications) onRequestNotifications() else onIntent(OnboardingIntent.NextStep)
                },
                above = if (kind == StepKind.Intro || kind == StepKind.Story) {
                    { OnboardingPageDots(count = OnboardingStep.PAGE_COUNT, selected = step.pageIndex) }
                } else {
                    null
                },
            )
        } else {
            Spacer(Modifier.height(FitAISpacing.lg))
        }
    }
}

@Composable
private fun StepBody(step: OnboardingStep, state: OnboardingState, onIntent: (OnboardingIntent) -> Unit) {
    val strings = LocalStrings.current
    when (step) {
        OnboardingStep.IntroTrack, OnboardingStep.IntroProgress, OnboardingStep.IntroPlate -> IntroSlide(step)
        OnboardingStep.Goal -> StepGoal(state, onIntent)
        OnboardingStep.Obstacle -> StepObstacle(state, onIntent)
        OnboardingStep.Focus -> StepFocus(state, onIntent)
        OnboardingStep.Attribution -> StepAttribution(state, onIntent)
        OnboardingStep.Experience -> StepExperience(state, onIntent)
        OnboardingStep.Gender -> StepGender(state, onIntent)
        OnboardingStep.Name -> StepName(state, onIntent)
        OnboardingStep.Age -> StepAge(state, onIntent)
        OnboardingStep.Height -> StepHeight(state, onIntent)
        OnboardingStep.Weight -> StepWeight(state, onIntent)
        OnboardingStep.StatementConsistency -> StatementStep(strings.onboardingStatementConsistency) {
            onIntent(OnboardingIntent.StatementAnswered(it))
        }
        OnboardingStep.StatementNutrition -> StatementStep(strings.onboardingStatementNutrition) {
            onIntent(OnboardingIntent.StatementAnswered(it))
        }
        OnboardingStep.StoryData, OnboardingStep.StorySystem, OnboardingStep.StoryPlan -> StorySlide(step)
        OnboardingStep.Activity -> StepActivity(state, onIntent)
        OnboardingStep.Equipment -> StepEquipment(state, onIntent)
        OnboardingStep.Days -> StepDays(state, onIntent)
        OnboardingStep.Duration -> StepDuration(state, onIntent)
        OnboardingStep.Diet -> StepDiet(state, onIntent)
        OnboardingStep.Notifications -> NotificationPrimingStep()
    }
}

private fun mapOnboardingError(key: String, strings: Strings): String = when (key) {
    "validation_name_required" -> strings.validationNameRequired
    "validation_numbers_required" -> strings.validationNumbersRequired
    "validation_fields_required" -> strings.validationFieldsRequired
    else -> strings.errorUnknown
}

@Preview
@Composable
private fun OnboardingPreview() {
    FitAITheme {
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            OnboardingScreen(onNavigatePaywall = {}, onBackToWelcome = {})
        }
    }
}
