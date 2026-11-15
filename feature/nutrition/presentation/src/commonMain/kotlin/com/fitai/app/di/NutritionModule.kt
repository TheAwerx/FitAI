package com.fitai.app.di

import com.fitai.app.viewmodel.CustomFoodViewModel
import com.fitai.app.viewmodel.FoodDetailViewModel
import com.fitai.app.viewmodel.FoodSearchViewModel
import com.fitai.app.viewmodel.MealLogViewModel
import com.fitai.app.viewmodel.NutritionViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val nutritionPresentationModule = module {
    includes(nutritionDomainModule)
    viewModelOf(::NutritionViewModel)
    viewModelOf(::MealLogViewModel)
    viewModelOf(::FoodSearchViewModel)
    viewModelOf(::FoodDetailViewModel)
    viewModelOf(::CustomFoodViewModel)
}
