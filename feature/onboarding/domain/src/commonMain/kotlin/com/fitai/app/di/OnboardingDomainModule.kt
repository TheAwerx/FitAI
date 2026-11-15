package com.fitai.app.di

import com.fitai.app.onboarding.ConfirmCommitmentUseCase
import com.fitai.app.onboarding.GetOnboardingSummaryUseCase
import com.fitai.app.onboarding.SaveAttributionUseCase
import com.fitai.app.onboarding.SaveGuestProfileUseCase
import com.fitai.app.onboarding.SaveOnboardingAnswersUseCase
import com.fitai.app.onboarding.SetupInitialPlansUseCase
import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.module

val onboardingDomainModule = module {
    factoryOf(::SaveGuestProfileUseCase)
    factoryOf(::SaveOnboardingAnswersUseCase)
    factoryOf(::SetupInitialPlansUseCase)
    factoryOf(::GetOnboardingSummaryUseCase)
    factoryOf(::SaveAttributionUseCase)
    factoryOf(::ConfirmCommitmentUseCase)
}
