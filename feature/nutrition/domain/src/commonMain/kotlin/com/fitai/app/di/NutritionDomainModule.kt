package com.fitai.app.di

import com.fitai.app.nutrition.AddFoodToDiaryUseCase
import com.fitai.app.nutrition.AnalyzeMealPhotoWithQuotaUseCase
import com.fitai.app.nutrition.DeleteCustomFoodUseCase
import com.fitai.app.nutrition.DeleteMealEntryUseCase
import com.fitai.app.nutrition.EditMealEntryUseCase
import com.fitai.app.nutrition.GetFoodDetailUseCase
import com.fitai.app.nutrition.GetMealEntryUseCase
import com.fitai.app.nutrition.LogAnalyzedMealUseCase
import com.fitai.app.nutrition.LogPlannedMealUseCase
import com.fitai.app.nutrition.ObserveNutritionOverviewUseCase
import com.fitai.app.nutrition.SaveCustomFoodUseCase
import com.fitai.app.nutrition.SearchFoodsUseCase
import com.fitai.app.nutrition.SelectPlanMealOptionUseCase
import com.fitai.app.nutrition.SuggestNextMealUseCase
import com.fitai.app.nutrition.UpdateLoggedFoodUseCase
import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.module

val nutritionDomainModule = module {
    factoryOf(::ObserveNutritionOverviewUseCase)
    factoryOf(::AnalyzeMealPhotoWithQuotaUseCase)
    factoryOf(::LogAnalyzedMealUseCase)
    factoryOf(::SuggestNextMealUseCase)
    factoryOf(::SearchFoodsUseCase)
    factoryOf(::GetFoodDetailUseCase)
    factoryOf(::AddFoodToDiaryUseCase)
    factoryOf(::UpdateLoggedFoodUseCase)
    factoryOf(::EditMealEntryUseCase)
    factoryOf(::GetMealEntryUseCase)
    factoryOf(::DeleteMealEntryUseCase)
    factoryOf(::LogPlannedMealUseCase)
    factoryOf(::SelectPlanMealOptionUseCase)
    factoryOf(::SaveCustomFoodUseCase)
    factoryOf(::DeleteCustomFoodUseCase)
}
