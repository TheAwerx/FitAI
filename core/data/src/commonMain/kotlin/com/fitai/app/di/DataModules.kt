package com.fitai.app.di

import com.fitai.app.access.CheckFeatureAccessUseCase
import com.fitai.app.access.EntitlementRepository
import com.fitai.app.access.EntitlementRepositoryImpl
import com.fitai.app.access.FirestorePlanQuotaRepository
import com.fitai.app.access.GetPlanQuotaUseCase
import com.fitai.app.access.LocalPlanQuotaRepository
import com.fitai.app.access.PlanQuotaRepository
import com.fitai.app.access.RegenerateWorkoutPlanUseCase
import com.fitai.app.access.SpendPlanRegenerationUseCase
import com.fitai.app.access.GetAccessTierUseCase
import com.fitai.app.access.RecordFeatureUseUseCase
import com.fitai.app.access.UsageLimitRepository
import com.fitai.app.access.UsageLimitRepositoryImpl
import com.fitai.app.ai.AIRepository
import com.fitai.app.analytics.AnalyticsTracker
import com.fitai.app.analytics.LogAnalyticsTracker
import com.fitai.app.analytics.TrackEventUseCase
import com.fitai.app.billing.BillingRepository
import com.fitai.app.billing.GetSubscriptionProductsUseCase
import com.fitai.app.billing.MockBillingRepository
import com.fitai.app.billing.PurchaseSubscriptionUseCase
import com.fitai.app.billing.RestorePurchasesUseCase
import com.fitai.app.preferences.UserPreferencesRepository
import com.fitai.app.preferences.UserPreferencesRepositoryImpl
import com.fitai.app.repository.ProgressRepository
import com.fitai.app.repository.ProgressRepositoryImpl
import com.fitai.app.stats.ComputeWeeklyStatsUseCase
import com.fitai.app.ai.AiRemoteDataSource
import com.fitai.app.ai.DevPlanAIRepository
import com.fitai.app.ai.FirebaseAIRepository
import com.fitai.app.ai.MockAIRepository
import com.fitai.app.auth.FirestoreProfileSyncRepository
import com.fitai.app.auth.ProfileSyncRepository
import com.fitai.app.data.DatabaseDriverFactory
import com.fitai.app.db.FitAiDatabase
import com.fitai.app.domain.AnalyzeMealPhotoUseCase
import com.fitai.app.domain.CheckTrialAccessUseCase
import com.fitai.app.domain.CompleteSetUseCase
import com.fitai.app.domain.GenerateDietPlanUseCase
import com.fitai.app.domain.GenerateWorkoutPlanUseCase
import com.fitai.app.domain.InitGuestTrialUseCase
import com.fitai.app.domain.LogMealUseCase
import com.fitai.app.domain.StartWorkoutSessionUseCase
import com.fitai.app.engagement.AddWeighInUseCase
import com.fitai.app.engagement.GetBadgesUseCase
import com.fitai.app.engagement.GetEngagementSummaryUseCase
import com.fitai.app.engagement.GetMeasurementHistoryUseCase
import com.fitai.app.engagement.GetProgressOverviewUseCase
import com.fitai.app.engagement.GetStarterChallengeUseCase
import com.fitai.app.engagement.GetWorkoutHistoryUseCase
import com.fitai.app.engagement.GetWorkoutStreakUseCase
import com.fitai.app.engagement.HideStarterChallengeUseCase
import com.fitai.app.firebase.FirebaseBootstrap
import com.fitai.app.food.BundledFoodDataSource
import com.fitai.app.food.FoodRepositoryImpl
import com.fitai.app.network.HttpClientFactory
import com.fitai.app.notifications.BuildAndScheduleRemindersUseCase
import com.fitai.app.notifications.CancelRemindersUseCase
import com.fitai.app.notifications.GetReminderSettingsUseCase
import com.fitai.app.notifications.ReminderStateRepository
import com.fitai.app.notifications.ReminderStateRepositoryImpl
import com.fitai.app.notifications.SetRemindersEnabledUseCase
import com.fitai.app.notifications.UpdateReminderTimeUseCase
import com.fitai.app.nutrition.GetTodayNutritionOverviewUseCase
import com.fitai.app.repository.FoodRepository
import com.fitai.app.repository.LocalUserRepository
import com.fitai.app.repository.LocalUserRepositoryImpl
import com.fitai.app.session.LocalSessionRepository
import com.fitai.app.session.LocalSessionRepositoryImpl
import com.fitai.app.repository.NutritionRepository
import com.fitai.app.repository.NutritionRepositoryImpl
import com.fitai.app.repository.UserLocalDataSource
import com.fitai.app.repository.WorkoutRepository
import com.fitai.app.repository.WorkoutRepositoryImpl
import com.fitai.app.sync.CloudSyncLocalDataSource
import com.fitai.app.sync.CloudSyncScheduler
import com.fitai.app.sync.FirestoreSyncRemoteDataSource
import com.fitai.app.sync.FirestoreUserDataSyncRepository
import com.fitai.app.sync.NoOpUserDataSyncRepository
import com.fitai.app.sync.SyncingFoodRepository
import com.fitai.app.sync.SyncingLocalUserRepository
import com.fitai.app.sync.SyncingNutritionRepository
import com.fitai.app.sync.SyncingProgressRepository
import com.fitai.app.sync.SyncingUserPreferencesRepository
import com.fitai.app.sync.SyncingWorkoutRepository
import com.fitai.app.sync.UserDataSyncRepository
import com.fitai.app.trial.FirestoreTrialRepository
import com.fitai.app.trial.LocalTrialRepository
import com.fitai.app.trial.TrialRepository
import com.fitai.app.training.DetectSessionPRsUseCase
import com.fitai.app.training.GetPersonalRecordsUseCase
import com.fitai.app.training.GetSetSuggestionsUseCase
import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.module

fun databaseModule() = module {
    single { FitAiDatabase(get<DatabaseDriverFactory>().createDriver()) }
}

fun dataRepositoryModule(useRemotePlanGeneration: Boolean) = module {
    single { HttpClientFactory.create() }
    single { UserLocalDataSource(get()) }
    // Cloud copy (firebase/SCHEMA.md): device writes go to SQLDelight first, then the scheduler mirrors them.
    single { CloudSyncLocalDataSource(get()) }
    single { FirestoreSyncRemoteDataSource() }
    single { UserPreferencesRepositoryImpl(get()) }
    single { CloudSyncScheduler(get(), get(), get<UserPreferencesRepositoryImpl>()) }
    single<WorkoutRepository> { SyncingWorkoutRepository(WorkoutRepositoryImpl(get()), get()) }
    single<NutritionRepository> { SyncingNutritionRepository(NutritionRepositoryImpl(get()), get()) }
    single { BundledFoodDataSource() }
    single<FoodRepository> { SyncingFoodRepository(FoodRepositoryImpl(get(), get()), get()) }
    single<LocalUserRepository> { SyncingLocalUserRepository(LocalUserRepositoryImpl(get()), get()) }
    single { AiRemoteDataSource() }
    single<AIRepository> {
        val localPlans = MockAIRepository(get())
        when {
            !FirebaseBootstrap.ensureInitialized() -> localPlans
            useRemotePlanGeneration -> FirebaseAIRepository(get())
            else -> DevPlanAIRepository(remote = FirebaseAIRepository(get()), localPlans = localPlans)
        }
    }
    single<TrialRepository> {
        if (FirebaseBootstrap.ensureInitialized()) FirestoreTrialRepository() else LocalTrialRepository()
    }
    single<UserDataSyncRepository> {
        if (FirebaseBootstrap.ensureInitialized()) {
            FirestoreUserDataSyncRepository(get(), get())
        } else {
            NoOpUserDataSyncRepository()
        }
    }
    single<ProfileSyncRepository> { FirestoreProfileSyncRepository(get()) }
    single<ProgressRepository> { SyncingProgressRepository(ProgressRepositoryImpl(get()), get()) }
    single<EntitlementRepository> { EntitlementRepositoryImpl(get(), get()) }
    single<UsageLimitRepository> { UsageLimitRepositoryImpl(get()) }
    single { LocalPlanQuotaRepository(get()) }
    single<PlanQuotaRepository> {
        // Dev never calls generateWorkoutPlan, so the server count would stay at zero there.
        if (useRemotePlanGeneration && FirebaseBootstrap.ensureInitialized()) {
            FirestorePlanQuotaRepository(get())
        } else {
            get<LocalPlanQuotaRepository>()
        }
    }
    single<AnalyticsTracker> { LogAnalyticsTracker() }
    single<UserPreferencesRepository> { SyncingUserPreferencesRepository(get<UserPreferencesRepositoryImpl>(), get()) }
    single<LocalSessionRepository> { LocalSessionRepositoryImpl(get(), get()) }
    single<BillingRepository> { MockBillingRepository(get()) }
    single<ReminderStateRepository> { ReminderStateRepositoryImpl(get()) }
}

val dataUseCaseModule = module {
    factoryOf(::GenerateWorkoutPlanUseCase)
    factoryOf(::GenerateDietPlanUseCase)
    factoryOf(::AnalyzeMealPhotoUseCase)
    factoryOf(::LogMealUseCase)
    factoryOf(::StartWorkoutSessionUseCase)
    factoryOf(::CompleteSetUseCase)
    factoryOf(::CheckTrialAccessUseCase)
    factoryOf(::InitGuestTrialUseCase)
    factoryOf(::GetAccessTierUseCase)
    factoryOf(::CheckFeatureAccessUseCase)
    factoryOf(::RecordFeatureUseUseCase)
    factoryOf(::GetPlanQuotaUseCase)
    factoryOf(::SpendPlanRegenerationUseCase)
    factoryOf(::RegenerateWorkoutPlanUseCase)
    factoryOf(::TrackEventUseCase)
    factoryOf(::GetSubscriptionProductsUseCase)
    factoryOf(::PurchaseSubscriptionUseCase)
    factoryOf(::RestorePurchasesUseCase)
    factoryOf(::ComputeWeeklyStatsUseCase)
    factoryOf(::GetSetSuggestionsUseCase)
    factoryOf(::DetectSessionPRsUseCase)
    factoryOf(::GetPersonalRecordsUseCase)
    factoryOf(::GetTodayNutritionOverviewUseCase)
    factoryOf(::GetEngagementSummaryUseCase)
    factoryOf(::GetBadgesUseCase)
    factoryOf(::AddWeighInUseCase)
    factoryOf(::GetProgressOverviewUseCase)
    factoryOf(::GetWorkoutStreakUseCase)
    factoryOf(::GetStarterChallengeUseCase)
    factoryOf(::HideStarterChallengeUseCase)
    factoryOf(::GetMeasurementHistoryUseCase)
    factoryOf(::GetWorkoutHistoryUseCase)
    factoryOf(::BuildAndScheduleRemindersUseCase)
    factoryOf(::CancelRemindersUseCase)
    factoryOf(::SetRemindersEnabledUseCase)
    factoryOf(::UpdateReminderTimeUseCase)
    factoryOf(::GetReminderSettingsUseCase)
}
