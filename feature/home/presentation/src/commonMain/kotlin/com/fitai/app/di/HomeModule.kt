package com.fitai.app.di

import com.fitai.app.viewmodel.HomeViewModel
import com.fitai.app.viewmodel.ProgressViewModel
import com.fitai.app.viewmodel.StarterChallengeViewModel
import com.fitai.app.viewmodel.StreakViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val homePresentationModule = module {
    includes(homeDomainModule)
    viewModelOf(::HomeViewModel)
    viewModelOf(::ProgressViewModel)
    viewModelOf(::StarterChallengeViewModel)
    viewModelOf(::StreakViewModel)
}
