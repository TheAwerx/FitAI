package com.fitai.app.di

import com.fitai.app.viewmodel.CoachViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val coachPresentationModule = module {
    includes(coachDomainModule)
    viewModelOf(::CoachViewModel)
}
