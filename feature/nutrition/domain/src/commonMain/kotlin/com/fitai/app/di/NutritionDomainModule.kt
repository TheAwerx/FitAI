package com.fitai.app.di

import com.fitai.app.nutrition.AnalyzeMealPhotoWithQuotaUseCase
import com.fitai.app.nutrition.LogAnalyzedMealUseCase
import com.fitai.app.nutrition.ObserveNutritionOverviewUseCase
import com.fitai.app.nutrition.SuggestNextMealUseCase
import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.module

val nutritionDomainModule = module {
    factoryOf(::ObserveNutritionOverviewUseCase)
    factoryOf(::AnalyzeMealPhotoWithQuotaUseCase)
    factoryOf(::LogAnalyzedMealUseCase)
    factoryOf(::SuggestNextMealUseCase)
}
