package com.fitai.app.di

import com.fitai.app.viewmodel.ActiveSessionViewModel
import com.fitai.app.viewmodel.ExerciseDetailViewModel
import com.fitai.app.viewmodel.SessionSummaryViewModel
import com.fitai.app.viewmodel.TodayModeViewModel
import com.fitai.app.viewmodel.WorkoutViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val workoutPresentationModule = module {
    includes(workoutDomainModule)
    viewModelOf(::WorkoutViewModel)
    viewModelOf(::ExerciseDetailViewModel)
    viewModelOf(::ActiveSessionViewModel)
    viewModelOf(::TodayModeViewModel)
    viewModelOf(::SessionSummaryViewModel)
}
