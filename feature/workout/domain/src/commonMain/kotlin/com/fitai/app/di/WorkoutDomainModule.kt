package com.fitai.app.di

import com.fitai.app.workout.FinishWorkoutSessionUseCase
import com.fitai.app.workout.GetExerciseAlternativesUseCase
import com.fitai.app.workout.GetExerciseByIdUseCase
import com.fitai.app.workout.GetExercisesForDayUseCase
import com.fitai.app.workout.GetSessionSummaryUseCase
import com.fitai.app.workout.LoadActiveSessionUseCase
import com.fitai.app.workout.ObserveWorkoutOverviewUseCase
import com.fitai.app.workout.RefreshSessionSetsUseCase
import com.fitai.app.workout.ReplaceExerciseUseCase
import com.fitai.app.workout.StartTodayModeSessionUseCase
import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.module

val workoutDomainModule = module {
    factoryOf(::ObserveWorkoutOverviewUseCase)
    factoryOf(::GetExercisesForDayUseCase)
    factoryOf(::GetExerciseByIdUseCase)
    factoryOf(::LoadActiveSessionUseCase)
    factoryOf(::RefreshSessionSetsUseCase)
    factoryOf(::FinishWorkoutSessionUseCase)
    factoryOf(::GetExerciseAlternativesUseCase)
    factoryOf(::ReplaceExerciseUseCase)
    factoryOf(::StartTodayModeSessionUseCase)
    factoryOf(::GetSessionSummaryUseCase)
}
