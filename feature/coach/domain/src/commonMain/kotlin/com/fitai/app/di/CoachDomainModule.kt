package com.fitai.app.di

import com.fitai.app.coach.BuildCoachContextUseCase
import com.fitai.app.coach.ClearCoachHistoryUseCase
import com.fitai.app.coach.GetCoachHistoryUseCase
import com.fitai.app.coach.GetWeeklyReportUseCase
import com.fitai.app.coach.SendCoachMessageUseCase
import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.module

val coachDomainModule = module {
    factoryOf(::BuildCoachContextUseCase)
    factoryOf(::GetCoachHistoryUseCase)
    factoryOf(::ClearCoachHistoryUseCase)
    factoryOf(::SendCoachMessageUseCase)
    factoryOf(::GetWeeklyReportUseCase)
}
