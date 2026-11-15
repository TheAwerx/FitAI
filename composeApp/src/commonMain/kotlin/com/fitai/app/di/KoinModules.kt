package com.fitai.app.di

import com.fitai.app.useRemotePlanGeneration

fun appModules() = listOf(
    platformDriverModule(),
    databaseModule(),
    dataRepositoryModule(useRemotePlanGeneration),
    dataUseCaseModule,
    pushNotificationModule,
    appModule,
    authDataModule,
    authPresentationModule,
    onboardingDataModule,
    onboardingPresentationModule,
    homeDataModule,
    homePresentationModule,
    workoutDataModule,
    workoutPresentationModule,
    nutritionDataModule,
    nutritionPresentationModule,
    profileDataModule,
    profilePresentationModule,
    coachDataModule,
    coachPresentationModule,
)
