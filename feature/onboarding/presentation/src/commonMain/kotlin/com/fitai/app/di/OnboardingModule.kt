package com.fitai.app.di

import com.fitai.app.viewmodel.OnboardingViewModel
import com.fitai.app.viewmodel.PaywallViewModel
import com.fitai.app.viewmodel.PlanSetupViewModel
import com.fitai.app.viewmodel.RemindersLaunchSyncViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val onboardingPresentationModule = module {
    includes(onboardingDomainModule)
    viewModelOf(::OnboardingViewModel)
    viewModelOf(::PaywallViewModel)
    viewModelOf(::PlanSetupViewModel)
    viewModelOf(::RemindersLaunchSyncViewModel)
}
