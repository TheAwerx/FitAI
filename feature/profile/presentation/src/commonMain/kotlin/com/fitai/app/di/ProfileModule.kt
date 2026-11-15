package com.fitai.app.di

import com.fitai.app.viewmodel.MeasurementsViewModel
import com.fitai.app.viewmodel.ProfileViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val profilePresentationModule = module {
    includes(profileDomainModule)
    viewModelOf(::ProfileViewModel)
    viewModelOf(::MeasurementsViewModel)
}
