package com.fitai.app.di

import com.fitai.app.home.CanAccessQuickActionsUseCase
import com.fitai.app.home.GetHomeDashboardUseCase
import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.module

val homeDomainModule = module {
    factoryOf(::GetHomeDashboardUseCase)
    factoryOf(::CanAccessQuickActionsUseCase)
}
