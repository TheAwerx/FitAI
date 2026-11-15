package com.fitai.app.di

import com.fitai.app.coach.CoachHistoryRepository
import com.fitai.app.coach.CoachHistoryRepositoryImpl
import com.fitai.app.coach.WeeklyReportCacheRepository
import com.fitai.app.coach.WeeklyReportCacheRepositoryImpl
import org.koin.dsl.module

val coachDataModule = module {
    single<CoachHistoryRepository> { CoachHistoryRepositoryImpl(get()) }
    single<WeeklyReportCacheRepository> { WeeklyReportCacheRepositoryImpl(get()) }
}
