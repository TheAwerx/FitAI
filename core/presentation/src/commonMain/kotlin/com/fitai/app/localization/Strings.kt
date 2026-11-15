package com.fitai.app.localization

import androidx.compose.runtime.staticCompositionLocalOf
import com.fitai.app.model.ActivityLevel
import com.fitai.app.model.DietRestriction
import com.fitai.app.model.EquipmentOption
import com.fitai.app.model.FitnessGoal
import com.fitai.app.model.FocusArea
import com.fitai.app.model.Gender
import com.fitai.app.model.TrainingExperience
import com.fitai.app.model.TrainingObstacle

interface Strings {
    val appName: String
    val splashLoading: String

    val tabHome: String
    val tabWorkout: String
    val tabNutrition: String
    val tabProgress: String
    val tabProfile: String
    val homeTitle: String
    val homeSubtitle: String
    val homeTodayWorkout: String
    val homeNoWorkout: String
    val homeMacros: String
    val homeQuickLogMeal: String
    fun homeTrialRemaining(days: Int): String
    val homeTrialExpired: String

    val welcomeTitle: String
    val welcomeHeadline: String
    val welcomeSubtitle: String
    val welcomeCta: String
    val welcomeHaveAccount: String

    fun onboardingProgress(step: Int, total: Int): String
    val onboardingLabelName: String
    val onboardingNameTitle: String
    val onboardingNameSubtitle: String
    val onboardingNamePlaceholder: String
    val onboardingLabelEmail: String
    val onboardingLabelAge: String
    val onboardingAgeTitle: String
    val onboardingAgeSubtitle: String
    val onboardingAgeUnit: String
    val onboardingLabelHeight: String
    val onboardingHeightTitle: String
    val onboardingHeightSubtitle: String
    val onboardingHeightUnitCm: String
    val onboardingHeightUnitFt: String
    val onboardingLabelWeight: String
    val onboardingWeightTitle: String
    val onboardingWeightSubtitle: String
    val onboardingWeightUnit: String
    val onboardingGoalTitle: String
    val onboardingGoalSubtitle: String
    val onboardingActivityTitle: String
    val onboardingActivitySubtitle: String
    val onboardingEquipmentTitle: String
    val onboardingEquipmentSubtitle: String
    val onboardingDietTitle: String
    val onboardingDietSubtitle: String
    val onboardingCreatePlan: String
    val onboardingDietNotesLabel: String
    val onboardingDietNotesPlaceholder: String
    val onboardingGeneratingTitle: String
    val onboardingGeneratingSubtitle: String
    val onboardingGeneratingWorkout: String
    val onboardingGeneratingNutrition: String
    val onboardingGeneratingOptimize: String
    val onboardingTrainerTip: String
    val onboardingTrainerTipBody: String
    val onboardingErrorTitle: String
    val onboardingErrorSubtitle: String
    fun goalLabel(code: String): String
    fun goalDescription(code: String): String
    fun activityLabel(code: String): String
    fun activityDescription(code: String): String
    fun equipmentLabel(code: String): String
    val equipmentFullGymSubtitle: String
    fun dietLabel(code: String): String

    val workoutTitle: String
    val workoutActivePlan: String
    val workoutHistory: String
    val workoutRegenerate: String
    val workoutStartSession: String
    fun workoutExercisesCount(count: Int): String
    fun workoutSetsReps(sets: Int, reps: String): String
    fun workoutRest(seconds: Int): String
    val workoutSessionActive: String
    val workoutCompleteSession: String
    fun workoutSetNumber(number: Int): String
    val workoutWeightKg: String
    val workoutActualReps: String
    val workoutNoPlan: String

    val nutritionTitle: String
    val nutritionTargets: String
    val nutritionCalories: String
    val nutritionProtein: String
    val nutritionCarbs: String
    val nutritionFat: String
    val nutritionTodayMeals: String
    val nutritionLogMeal: String
    val nutritionPhotoMeal: String
    val nutritionMealName: String
    val nutritionRegenerate: String
    val nutritionNoPlan: String
    val nutritionAnalyzing: String

    val paywallTitle: String
    val paywallSubtitle: String
    val paywallFeatureWorkouts: String
    val paywallFeatureNutrition: String
    val paywallFeatureProgress: String
    val paywallComparePlans: String
    val paywallMonthly: String
    val paywallYearly: String
    val paywallSubscribe: String
    val paywallSignIn: String
    val paywallStartTrial: String
    val paywallContinueGuest: String
    val paywallComingSoon: String

    val profileTitle: String
    val profileCloudHint: String
    fun profileLocalSummary(name: String, goal: String, activity: String): String
    fun profileDietRestrictions(value: String): String
    val profileGoogle: String
    val profileApple: String
    val profileSignOut: String
    val profileAccount: String
    val profileSettings: String
    val profileEdit: String
    val profileRegeneratePlans: String
    val profileRegenerating: String
    val profileSaved: String
    val profilePlansRegenerated: String
    val profileAge: String
    val profileHeight: String
    val profileWeight: String
    val profileGoal: String
    val profileActivity: String
    val profileEquipment: String
    val profileDiet: String
    val profileEmailAuth: String

    val authTitleSignIn: String
    val authTitleRegister: String
    val authPassword: String

    val actionContinue: String
    val actionBack: String
    val actionFinish: String
    val actionSubmit: String
    val actionSave: String
    val actionRetry: String
    val actionCancel: String

    val firebaseUnavailable: String
    val validationNameRequired: String
    val validationNumbersRequired: String
    val validationFieldsRequired: String
    val validationEmailPasswordRequired: String
    val errorUnknown: String
    val trialExpired: String
    val syncFailed: String
    val appleUnsupportedAndroid: String

    val sampleEmailLabel: String
    val sampleEmailPlaceholder: String
    val actionPrimaryExample: String

    // region:foundation:interface
    val proBadge: String
    val upgradeToPro: String
    val featureLockedTitle: String
    val featureLockedBody: String
    fun quotaRemaining(remaining: Int, total: Int): String
    val quotaExhausted: String
    // region:workout:interface
    fun workoutSuggestedWeight(weight: String, reps: Int): String
    fun workoutSuggestedReps(reps: Int): String
    val workoutReasonIncreaseWeight: String
    val workoutReasonAddRep: String
    val workoutReasonDeload: String
    val workoutReasonBodyweight: String
    val workoutCopyPreviousSet: String
    val workoutRestTitle: String
    val workoutRestSkip: String
    val workoutRestAdd15: String
    val workoutReplace: String
    val workoutReplaceTitle: String
    val workoutReplaceSubtitle: String
    val workoutReplaceEmpty: String
    val workoutGym: String
    val workoutBodyweight: String
    fun workoutKg(value: String): String
    fun workoutMinutes(minutes: Int): String
    val workoutSummaryTitle: String
    val workoutSummarySubtitle: String
    val workoutSummaryDuration: String
    val workoutSummaryVolume: String
    val workoutSummarySets: String
    fun workoutSummarySetsValue(done: Int, total: Int): String
    val workoutSummaryPrTitle: String
    val workoutSummaryNoPr: String
    fun workoutPrSet(weight: String, reps: Int): String
    fun workoutPrE1rm(value: String): String
    val workoutSummaryDone: String
    val workoutTodayModeTitle: String
    val workoutTodayModeEntryBody: String
    val workoutTodayModeSubtitle: String
    val workoutTodayMinutesLabel: String
    val workoutTodayEnergyLabel: String
    val workoutTodayEnergyLow: String
    val workoutTodayEnergyHigh: String
    val workoutTodayPainLabel: String
    val workoutPainKnee: String
    val workoutPainLowerBack: String
    val workoutPainShoulder: String
    val workoutPainWrist: String
    val workoutPainNone: String
    val workoutTodayLocationLabel: String
    val workoutLocationHome: String
    val workoutLocationHotel: String
    val workoutLocationOutdoor: String
    val workoutTodaySubmit: String
    val workoutTodayAdapting: String
    val workoutTodayNoteTitle: String
    // region:planquota:interface
    fun planQuotaRemaining(remaining: Int, total: Int): String
    val planQuotaHint: String
    val planQuotaExhausted: String
    fun planQuotaResetsInDays(days: Int): String
    val planQuotaConfirmTitle: String
    fun planQuotaConfirmBody(remainingAfter: Int, total: Int): String
    val planQuotaConfirmAction: String
    val planQuotaErrorExhausted: String
    val planQuotaErrorInProgress: String
    val planQuotaErrorInitialLimit: String
    val planGenTitle: String
    val planGenSubtitle: String
    val planGenSteps: List<String>
    val planGenMuscleGroups: List<String>
    val planGenDoneTitle: String
    val planGenDoneAction: String
    val planGenFailedTitle: String
    val planGenFailedBody: String
    val planGenFailedKeptRight: String
    // region:trial:interface
    val trialUsedOnDeviceTitle: String
    val trialUsedOnDeviceBody: String
    val trialUsedByAccountTitle: String
    val trialUsedByAccountBody: String
    fun trialActiveTitle(days: Int): String
    val trialActiveBody: String
    fun trialStartedMessage(days: Int): String
    val trialRequiresAccount: String
    val trialStartFailed: String
    val planSetupTrialUsedOnDevice: String
    val planSetupTrialUsedByAccount: String
    val planSetupTrialPending: String
    fun settingsPlanTrial(days: Int): String
    val settingsPlanTrialEnded: String
    fun trialDebugLine(state: String, days: Int, keyPrefix: String, source: String, backend: String): String
    // region:onboarding:interface
    val onboardingSkip: String
    val onboardingAttributionTitle: String
    val onboardingAttributionSubtitle: String
    fun attributionLabel(code: String): String
    fun onboardingPreviewHeadline(days: Int, minutes: Int, equipment: String): String
    val onboardingPreviewEquipmentNone: String
    val onboardingPreviewEquipmentHome: String
    val onboardingPreviewEquipmentGym: String
    val onboardingPreviewFallbackTitle: String
    val onboardingPreviewSubtitle: String
    val onboardingPreviewWeekTitle: String
    val onboardingPreviewMacroTitle: String
    fun onboardingPreviewKcal(kcal: Int): String
    fun onboardingPreviewGrams(grams: Int): String
    fun weekdayShort(dayOfWeek: Int): String
    val onboardingCommitmentTitle: String
    val onboardingCommitmentSubtitle: String
    fun onboardingCommitmentStatement(days: Int): String
    val onboardingCommitmentDaysLabel: String
    val onboardingCommitmentHold: String
    val onboardingCommitmentHoldA11y: String
    fun paywallTrialCta(days: Int): String
    val paywallTrustLine: String
    val paywallRestore: String
    val paywallRestoreSuccess: String
    val paywallRestoreNotFound: String
    fun paywallSavingsBadge(percent: Int): String
    fun paywallMonthlyEquivalent(price: String): String
    fun paywallTrialDays(days: Int): String
    val paywallLoadError: String
    // region:nutrition:interface
    val nutritionPlateTitle: String
    val nutritionPlateBody: String
    val nutritionPlateCta: String
    val nutritionPlateGallery: String
    val nutritionPlateLockedTitle: String
    val nutritionAnalysisFailed: String
    val nutritionReviewTitle: String
    val nutritionReviewHint: String
    val nutritionRemainingTitle: String
    val nutritionTrainingDayTarget: String
    val nutritionRestDayTarget: String
    fun nutritionKcalLeft(kcal: Int): String
    fun nutritionKcalProgress(consumed: Int, target: Int): String
    fun nutritionGramsProgress(consumed: Int, target: Int): String
    fun nutritionMealMacros(proteinG: Int, carbsG: Int, fatG: Int): String
    val nutritionNextMealTitle: String
    val nutritionNextMealLoading: String
    val nutritionNextMealError: String
    val nutritionNextMealLockedBody: String
    fun nutritionNextMealMacros(calories: Int, proteinG: Int, carbsG: Int, fatG: Int): String
    // region:coach:interface
    val coachTitle: String
    val coachEntryTeaser: String
    val coachWeekTitle: String
    val coachStatSessions: String
    val coachStatVolume: String
    val coachStatProtein: String
    val coachStatWeight: String
    val coachStatPrs: String
    fun coachStatSessionsValue(done: Int, planned: Int): String
    fun coachStatVolumeValue(kg: Int, changePercent: Int?): String
    fun coachStatProteinValue(avg: Int, target: Int): String
    fun coachStatWeightValue(change: String?): String
    val coachHeadlineNoActivity: String
    fun coachHeadlinePlanCompleted(sessions: Int): String
    fun coachHeadlineVolumeUp(percent: Int): String
    fun coachHeadlineProteinLow(avg: Int, target: Int): String
    fun coachHeadlineInProgress(done: Int, planned: Int): String
    val coachReportHighlights: String
    val coachReportNextWeek: String
    val coachReportLockedTitle: String
    val coachReportLockedBody: String
    val coachReportFailed: String
    val coachReportLoadError: String
    val coachChatTitle: String
    val coachChatEmpty: String
    val coachPromptKnee: String
    val coachPromptProtein: String
    val coachPromptHarder: String
    val coachInputPlaceholder: String
    val coachSendA11y: String
    val coachTyping: String
    val coachDisclaimer: String
    val coachDailyLimit: String
    fun coachMessageTooLong(max: Int): String
    val coachSendFailed: String
    val coachChatLockedTitle: String
    val coachChatLockedBody: String
    val coachClearHistory: String
    fun coachCharCount(count: Int, max: Int): String
    // region:engagement:interface
    fun engagementStreakDays(days: Int): String
    val engagementStreakLabel: String
    val engagementFreezeAvailable: String
    val engagementFreezeUsed: String
    val engagementTodayActive: String
    fun engagementLevel(level: Int): String
    fun engagementWeeklyXp(xp: Int): String
    fun engagementXpToNext(xp: Int): String
    val engagementChecklistTitle: String
    fun engagementChecklistProgress(done: Int, total: Int): String
    val engagementChecklistSession: String
    val engagementChecklistMealPhoto: String
    val engagementChecklistWeighIn: String
    fun homeTodayExerciseCount(count: Int): String
    fun homeMacrosLeft(proteinG: Int, carbsG: Int, fatG: Int): String
    val homeNoDietPlan: String
    val homeWeekTitle: String
    fun homeVolumeTonnes(tonnes: String): String
    fun homeVolumeChange(percent: Int): String
    val progressWeightTitle: String
    val progressWeightEmpty: String
    fun progressWeightLatest(weight: String): String
    fun progressWeightChange(change: String): String
    val progressAddWeighIn: String
    val progressWeightLabel: String
    val progressWaistLabel: String
    val progressWeightInvalid: String
    val progressWaistInvalid: String
    val progressWeighInSaved: String
    val progressSaveFailed: String
    val progressLoadError: String
    val progressVolumeTitle: String
    val progressVolumeEmpty: String
    fun progressWeekShort(day: Int, month: Int): String
    val progressPrTitle: String
    val progressPrEmpty: String
    fun progressPrValue(weight: String, reps: Int): String
    fun progressE1rm(value: String): String
    val progressBadgesTitle: String
    fun progressBadgesCount(unlocked: Int, total: Int): String
    val badgeFirstSession: String
    val badgeFirstMealPhoto: String
    val badgeFirstWeighIn: String
    val badgeFirstPr: String
    val badgeStreak7: String
    val badgeStreak28: String
    val badgeSessions10: String
    val badgeMeals100: String
    val badgeVolume10t: String
    // region:notifications:interface
    val notificationsPermissionTitle: String
    val notificationsPermissionSubtitle: String
    val notificationsHourUnit: String
    val notificationsMinuteUnit: String
    fun notificationsTimeSummary(time: String): String
    val notificationsRemind: String
    val notificationsNotNow: String
    val notificationsProfileSection: String
    val notificationsProfileToggle: String
    val notificationsProfileTime: String
    val notificationsPermissionDenied: String
    val notificationsUpdateFailed: String
    val notificationsChannelWorkout: String
    val notificationsChannelMeals: String
    val notificationsChannelProgram: String
    val notificationsWorkoutTitle: String
    val notificationsWorkoutBody: String
    val notificationsMealTitle: String
    val notificationsMealBody: String
    val notificationsDay0Title: String
    val notificationsDay0Body: String
    val notificationsDay1Title: String
    val notificationsDay1Body: String
    val notificationsDay2Title: String
    val notificationsDay2Body: String
    val notificationsDay3Title: String
    val notificationsDay3Body: String
    val notificationsDay5Title: String
    val notificationsDay5Body: String
    val notificationsDay7Title: String
    val notificationsDay7Body: String
    val notificationsTrialEndTitle: String
    val notificationsTrialEndBody: String
    // region:funnel:interface
    val languageCode: String
    fun languageName(code: String): String
    val actionClose: String
    val actionYes: String
    val actionNo: String
    fun percentLabel(value: Int): String
    fun longDate(day: Int, month: Int, year: Int): String
    fun shortDate(day: Int, month: Int): String
    fun dateRange(start: String, end: String): String
    val welcomeProofPlanTitle: String
    val welcomeProofPlanBody: String
    val welcomeProofCoachTitle: String
    val welcomeProofCoachBody: String
    val welcomeProofProgressTitle: String
    val welcomeProofProgressBody: String
    val welcomeHaveAccountPrefix: String
    val welcomeSignInLink: String
    val onboardingIntroTrackTitle: String
    val onboardingIntroTrackBody: String
    val onboardingIntroProgressTitle: String
    val onboardingIntroProgressBody: String
    val onboardingIntroPlateTitle: String
    val onboardingIntroPlateBody: String
    val onboardingMockSet: String
    val onboardingMockPrevious: String
    val onboardingMockKg: String
    val onboardingMockReps: String
    val onboardingMockExercise: String
    val onboardingMockMaxWeight: String
    val onboardingMockLastMonth: String
    fun onboardingMockWeek(week: Int): String
    val onboardingMockMealName: String
    val onboardingObstacleTitle: String
    fun obstacleLabel(code: String): String
    val onboardingFocusTitle: String
    val onboardingFocusSubtitle: String
    fun focusLabel(code: String): String
    fun focusDescription(code: String): String
    val onboardingExperienceTitle: String
    fun experienceLabel(code: String): String
    val onboardingGenderTitle: String
    val onboardingGenderSubtitle: String
    fun genderLabel(code: String): String
    val onboardingWeightPrivacy: String
    val onboardingWeightUsage: String
    val onboardingStatementTitle: String
    val onboardingStatementConsistency: String
    val onboardingStatementNutrition: String
    val onboardingStoryDataTitle: String
    val onboardingStoryDataBody: String
    val onboardingStorySystemTitle: String
    val onboardingStorySystemBody: String
    val onboardingStoryPlanTitle: String
    val onboardingStoryPlanBody: String
    val onboardingMockPlanTitle: String
    val onboardingMockPlanExercises: List<String>
    fun onboardingMockPlanMore(count: Int): String
    val onboardingMockToday: String
    val onboardingDaysTitle: String
    fun onboardingDaysLabel(days: Int): String
    fun onboardingDaysTag(days: Int): String
    val onboardingDurationTitle: String
    fun onboardingDurationLabel(minutes: Int): String
    fun onboardingDurationTag(minutes: Int): String
    val onboardingNotifyTitle: String
    val onboardingNotifyBody: String
    val onboardingNotifyMockClock: String
    val onboardingNotifyMockTitle: String
    val onboardingNotifyMockBody: String
    val onboardingNotifyMockTime: String
    val onboardingAnalyzingTitle: String
    val onboardingAnalyzingSteps: List<String>
    fun onboardingResultTitle(name: String): String
    val onboardingResultSubtitle: String
    fun onboardingResultBenefits(goal: String): List<String>
    val onboardingResultChartTitle: String
    val onboardingResultWithFitAI: String
    val onboardingResultWithoutFitAI: String
    val onboardingResultPlateau: String
    val onboardingQuoteLines: List<String>
    val onboardingPledgeLead: String
    fun onboardingPledgeDays(days: Int): String
    val onboardingPledgeTagline: String
    val onboardingPledgeHold: String
    val onboardingPledgeSigned: String
    val paywallChooseTitle: String
    fun paywallPlanReadyFor(name: String): String
    fun paywallSummaryDays(days: Int): String
    val paywallIncludesAi: String
    val paywallIncludesOverload: String
    val paywallIncludesCoach: String
    val paywallNoCommitment: String
    val paywallExitWarning: String
    val paywallExitOfferTitle: String
    fun paywallExitOfferBody(weeklyPrice: String): String
    val paywallExitDecline: String
    val paywallExitUnlock: String
    fun paywallPerWeek(price: String): String
    val authSignupTitle: String
    val authSignupSubtitle: String
    val authContinueEmail: String
    val authNoAccount: String
    val authSignUpLink: String
    val authLegal: String
    val authSignInHeading: String
    val authEmailHeading: String
    val authOr: String
    val authPasswordPlaceholder: String
    val authForgotPassword: String
    val authResetSent: String
    val authShowPassword: String
    val authHidePassword: String
    val authCreateAccount: String
    val authSignInCta: String
    val authOfflineContinue: String
    val authNoProfileFound: String
    val validationPasswordShort: String
    val validationEmailRequired: String
    val planSetupPersonalTitle: String
    val planSetupStarterTitle: String
    val planSetupStepWorkout: String
    val planSetupStepNutrition: String
    val planSetupStepReminders: String
    val planSetupErrorTitle: String
    val planSetupErrorBody: String
    val planSetupUseStarter: String
    val challengeTitle: String
    val challengeTaskNotifications: String
    val challengeGift: String
    val challengeGiftLocked: String
    val challengeAllDone: String
    val challengeHide: String
    val challengeOpenA11y: String
    val streakTitle: String
    fun streakQuote(days: Int): String
    val streakHowItWorks: String
    val streakHowItWorksBody: String
    fun streakLongest(days: Int): String
    fun streakA11y(days: Int): String
    val profileStatWorkouts: String
    val profileStatStreak: String
    val profileStatRecords: String
    val profileMeasurements: String
    val profileTabWorkouts: String
    val profileTabCharts: String
    val profileNoWorkouts: String
    val profileNoWorkoutsBody: String
    val profileChartEmptyTitle: String
    val profileChartEmptyBody: String
    val profileLogWorkout: String
    val profileGuestName: String
    val profilePeriod: String
    fun trendMetricLabel(code: String): String
    fun trendPeriodLabel(weeks: Int): String
    fun historyMeta(minutes: Int, volume: String, sets: Int): String
    val settingsProfileSection: String
    val settingsSubscription: String
    val settingsPlanPro: String
    val settingsPlanFree: String
    val settingsLanguage: String
    fun settingsSignedInAs(email: String): String
    val settingsGuestAccount: String
    val measurementWaist: String
    val measurementSameAsStart: String
    fun measurementChange(signed: String, unit: String): String
    val measurementEntries: String
    val measurementEmpty: String
    fun measurementPeriodLabel(months: Int): String
    fun cmValue(value: String): String
}

val LocalStrings = staticCompositionLocalOf<Strings> { StringsTr }

private fun StringsTrGoal(code: String) = when (code) {
    FitnessGoal.LoseFat.code -> "Yağ yak"
    FitnessGoal.BuildMuscle.code -> "Kas yap"
    FitnessGoal.GetStronger.code -> "Güçlen"
    FitnessGoal.ImproveEndurance.code -> "Dayanıklılık"
    FitnessGoal.StayHealthy.code -> "Sağlıklı kal"
    else -> code
}

private fun StringsTrGoalDesc(code: String) = when (code) {
    FitnessGoal.LoseFat.code -> "Kilo verme ve sıkılaşma odaklı"
    FitnessGoal.BuildMuscle.code -> "Kas kütlesi ve hacim"
    FitnessGoal.GetStronger.code -> "Kuvvet ve performans"
    FitnessGoal.ImproveEndurance.code -> "Kardiyo ve dayanıklılık"
    FitnessGoal.StayHealthy.code -> "Genel sağlık ve denge"
    else -> ""
}

private fun StringsTrActivity(code: String) = when (code) {
    ActivityLevel.Sedentary.code -> "Hareketsiz"
    ActivityLevel.Light.code -> "Hafif aktif"
    ActivityLevel.Moderate.code -> "Orta aktif"
    ActivityLevel.Very.code -> "Çok aktif"
    ActivityLevel.Athlete.code -> "Sporcu"
    else -> code
}

private fun StringsTrActivityDesc(code: String) = when (code) {
    ActivityLevel.Sedentary.code -> "Çoğunlukla oturarak, ofis işi, az egzersiz."
    ActivityLevel.Light.code -> "Haftada 1–3 gün hafif egzersiz, ayakta iş."
    ActivityLevel.Moderate.code -> "Haftada 3–5 gün orta yoğunluk."
    ActivityLevel.Very.code -> "Haftada 6–7 gün yoğun antrenman."
    ActivityLevel.Athlete.code -> "Çok yoğun antrenman, fiziksel iş veya günde iki antrenman."
    else -> ""
}

private fun StringsTrEquipment(code: String) = when (code) {
    EquipmentOption.Bodyweight.code -> "Yalnızca vücut ağırlığı"
    EquipmentOption.Dumbbells.code -> "Dambıl"
    EquipmentOption.Barbell.code -> "Barbell"
    EquipmentOption.ResistanceBands.code -> "Direnç lastikleri"
    EquipmentOption.PullUpBar.code -> "Barfiks barı"
    EquipmentOption.FullGym.code -> "Tam spor salonu"
    else -> code
}

private fun StringsTrDiet(code: String) = when (code) {
    DietRestriction.None.code -> "Yok"
    DietRestriction.Vegetarian.code -> "Vejetaryen"
    DietRestriction.Vegan.code -> "Vegan"
    DietRestriction.Pescatarian.code -> "Pesketaryen"
    DietRestriction.Halal.code -> "Helal"
    DietRestriction.Kosher.code -> "Koşer"
    DietRestriction.GlutenFree.code -> "Glutensiz"
    DietRestriction.DairyFree.code -> "Sütsüz"
    else -> code
}

object StringsTr : Strings {
    override val appName = "FitAI"
    override val splashLoading = "Yükleniyor…"
    override val tabHome = "Ana Sayfa"
    override val tabWorkout = "Antrenman"
    override val tabNutrition = "Beslenme"
    override val tabProgress = "İlerleme"
    override val tabProfile = "Profil"
    override val homeTitle = "Ana Sayfa"
    override val homeSubtitle = "Bugünkü özetiniz"
    override val homeTodayWorkout = "Bugünkü antrenman"
    override val homeNoWorkout = "Bugün için egzersiz yok"
    override val homeMacros = "Günlük makrolar"
    override val homeQuickLogMeal = "Öğün ekle"
    override fun homeTrialRemaining(days: Int) = "Deneme: $days gün kaldı"
    override val homeTrialExpired = "Deneme süreniz doldu"
    override val welcomeTitle = "FitAI"
    override val welcomeHeadline = "Hayalindeki fiziğe\nAI ile ulaş"
    override val welcomeSubtitle =
        "Zirve performans için tasarlanmış kişiselleştirilmiş AI antrenman ve beslenme."
    override val welcomeCta = "Başla"
    override val welcomeHaveAccount = "Zaten hesabım var"
    override fun onboardingProgress(step: Int, total: Int) = "Adım $step / $total"
    override val onboardingLabelName = "Ad Soyad"
    override val onboardingNameTitle = "Size nasıl hitap edelim?"
    override val onboardingNameSubtitle = "Deneyiminizi kişiselleştirmek için temel bilgilerle başlayalım."
    override val onboardingNamePlaceholder = "örn. Ali Yılmaz"
    override val onboardingLabelEmail = "E-posta adresi"
    override val onboardingLabelAge = "Yaş"
    override val onboardingAgeTitle = "Kaç yaşındasınız?"
    override val onboardingAgeSubtitle = "Fitness planınızı kişiselleştirmemize yardımcı olur."
    override val onboardingAgeUnit = "Yaş"
    override val onboardingLabelHeight = "Boy (cm)"
    override val onboardingHeightTitle = "Boyunuz kaç?"
    override val onboardingHeightSubtitle = "Metabolizma hızınızı hesaplamamıza yardımcı olur."
    override val onboardingHeightUnitCm = "cm"
    override val onboardingHeightUnitFt = "ft/in"
    override val onboardingLabelWeight = "Kilo (kg)"
    override val onboardingWeightTitle = "Kilonuz nedir?"
    override val onboardingWeightSubtitle = "Günlük kalori ihtiyacınızı hesaplamamıza yardımcı olur."
    override val onboardingWeightUnit = "kg"
    override val onboardingGoalTitle = "Ana hedefiniz nedir?"
    override val onboardingGoalSubtitle = "Fitness hedefinizi en iyi tanımlayan seçeneği seçin."
    override val onboardingActivityTitle = "Günlük aktivite düzeyiniz?"
    override val onboardingActivitySubtitle = "Fitness planınızı doğru uyarlamamıza yardımcı olur."
    override val onboardingEquipmentTitle = "Hangi ekipmanları kullanabilirsiniz?"
    override val onboardingEquipmentSubtitle = "Antrenmanları kişiselleştirmek için uygun olanları seçin."
    override val onboardingDietTitle = "Diyet tercihiniz veya kısıtlamanız var mı?"
    override val onboardingDietSubtitle = "Beslenme planını uyarlamak için uygun olanları seçin."
    override val onboardingCreatePlan = "Planımı oluştur"
    override val onboardingDietNotesLabel = "Başka bir şey?"
    override val onboardingDietNotesPlaceholder = "Örn. Fıstık alerjisi, aralıklı oruç..."
    override val onboardingGeneratingTitle = "Planınız hazırlanıyor…"
    override val onboardingGeneratingSubtitle = "AI koçunuz antrenman ve beslenme planınızı oluşturuyor."
    override val onboardingGeneratingWorkout = "Antrenman planınız oluşturuluyor…"
    override val onboardingGeneratingNutrition = "Beslenme planınız hazırlanıyor…"
    override val onboardingGeneratingOptimize = "Hedefinize göre optimize ediliyor…"
    override val onboardingTrainerTip = "Koç ipucu"
    override val onboardingTrainerTipBody =
        "Planınız ekipmanınıza ve diyet tercihlerinize özel. Tutarlılık, sonuç görmenin anahtarıdır."
    override val onboardingErrorTitle = "Bir şeyler ters gitti"
    override val onboardingErrorSubtitle = "Plan oluşturulamadı. Tekrar deneyin veya geri dönüp bilgilerinizi kontrol edin."
    override fun goalLabel(code: String) = StringsTrGoal(code)
    override fun goalDescription(code: String) = when (code) {
        FitnessGoal.LoseFat.code -> "Kilo verme ve sıkılaşma odaklı"
        FitnessGoal.BuildMuscle.code -> "Hipertrofi ve kütle artışı"
        FitnessGoal.GetStronger.code -> "Ham güç ve kaldırma kapasitesi"
        FitnessGoal.ImproveEndurance.code -> "Kardiyo sağlığı ve dayanıklılık"
        FitnessGoal.StayHealthy.code -> "Genel wellness ve idame"
        else -> ""
    }
    override fun activityLabel(code: String) = StringsTrActivity(code)
    override fun activityDescription(code: String) = StringsTrActivityDesc(code)
    override fun equipmentLabel(code: String) = StringsTrEquipment(code)
    override val equipmentFullGymSubtitle = "Ticari salon ekipmanı"
    override fun dietLabel(code: String) = StringsTrDiet(code)
    override val workoutTitle = "Antrenman"
    override val workoutActivePlan = "Aktif plan"
    override val workoutHistory = "Geçmiş planlar"
    override val workoutRegenerate = "Planı yenile"
    override val workoutStartSession = "Antrenmana başla"
    override fun workoutExercisesCount(count: Int) = "$count egzersiz"
    override fun workoutSetsReps(sets: Int, reps: String) = "$sets set × $reps"
    override fun workoutRest(seconds: Int) = "Dinlenme: $seconds sn"
    override val workoutSessionActive = "Aktif antrenman"
    override val workoutCompleteSession = "Antrenmanı bitir"
    override fun workoutSetNumber(number: Int) = "Set $number"
    override val workoutWeightKg = "Ağırlık (kg)"
    override val workoutActualReps = "Tekrar"
    override val workoutNoPlan = "Henüz antrenman planınız yok"
    override val nutritionTitle = "Beslenme"
    override val nutritionTargets = "Günlük hedefler"
    override val nutritionCalories = "Kalori"
    override val nutritionProtein = "Protein"
    override val nutritionCarbs = "Karbonhidrat"
    override val nutritionFat = "Yağ"
    override val nutritionTodayMeals = "Bugünkü öğünler"
    override val nutritionLogMeal = "Öğün kaydet"
    override val nutritionPhotoMeal = "Fotoğraftan analiz"
    override val nutritionMealName = "Öğün adı"
    override val nutritionRegenerate = "Diyet planını yenile"
    override val nutritionNoPlan = "Henüz diyet planınız yok"
    override val nutritionAnalyzing = "Fotoğraf analiz ediliyor…"
    override val paywallTitle = "FitAI Premium"
    override val paywallSubtitle = "Sınırsız AI planları ve ilerleme takibi."
    override val paywallFeatureWorkouts = "Sınırsız AI antrenmanları"
    override val paywallFeatureNutrition = "Detaylı beslenme planları"
    override val paywallFeatureProgress = "İlerleme analitikleri"
    override val paywallComparePlans = "Planları karşılaştır"
    override val paywallMonthly = "Aylık"
    override val paywallYearly = "Yıllık"
    override val paywallSubscribe = "Abone ol"
    override val paywallSignIn = "Giriş yap"
    override val paywallStartTrial = "Ücretsiz denemeyi başlat"
    override val paywallContinueGuest = "Misafir olarak devam et"
    override val paywallComingSoon = "Abonelik yakında"
    override val profileTitle = "Profil"
    override val profileCloudHint =
        "Verileriniz önce bu cihazda saklanır. Diğer cihazlarla senkron veya yedek için giriş yapabilirsiniz."
    override fun profileLocalSummary(name: String, goal: String, activity: String) =
        "$name · $goal · $activity"
    override fun profileDietRestrictions(value: String) = "Diyet: $value"
    override val profileGoogle = "Google ile devam et"
    override val profileApple = "Apple ile devam et"
    override val profileSignOut = "Çıkış yap"
    override val profileAccount = "Hesap"
    override val profileSettings = "Ayarlar"
    override val profileEdit = "Profili düzenle"
    override val profileRegeneratePlans = "Planı yeniden üret"
    override val profileRegenerating = "Planlar yenileniyor…"
    override val profileSaved = "Profil kaydedildi"
    override val profilePlansRegenerated = "Planlar yenilendi"
    override val profileAge = "Yaş"
    override val profileHeight = "Boy"
    override val profileWeight = "Kilo"
    override val profileGoal = "Hedef"
    override val profileActivity = "Aktivite"
    override val profileEquipment = "Ekipman"
    override val profileDiet = "Diyet"
    override val profileEmailAuth = "E-posta ile giriş"
    override val authTitleSignIn = "E-posta ile giriş"
    override val authTitleRegister = "Hesap oluştur"
    override val authPassword = "Şifre"
    override val actionContinue = "Devam"
    override val actionBack = "Geri"
    override val actionFinish = "Bitir"
    override val actionSubmit = "Gönder"
    override val actionSave = "Kaydet"
    override val actionRetry = "Tekrar dene"
    override val actionCancel = "İptal"
    override val firebaseUnavailable = "Bulut girişi şu an kullanılamıyor (Firebase yapılandırması eksik)."
    override val validationNameRequired = "Lütfen adınızı girin."
    override val validationNumbersRequired = "Yaş, boy ve kilo için geçerli sayılar girin."
    override val validationFieldsRequired = "Lütfen bir seçim yapın."
    override val validationEmailPasswordRequired = "E-posta ve şifre gerekli."
    override val errorUnknown = "Bir hata oluştu."
    override val trialExpired = "Deneme süreniz doldu."
    override val syncFailed = "Profil buluta gönderilemedi."
    override val appleUnsupportedAndroid = "Apple ile giriş bu platformda desteklenmiyor."
    override val sampleEmailLabel = "E-posta"
    override val sampleEmailPlaceholder = "ornek@fitai.app"
    override val actionPrimaryExample = "Örnek birincil buton"
    // region:foundation:tr
    override val proBadge = "PRO"
    override val upgradeToPro = "Pro'ya geç"
    override val featureLockedTitle = "Bu özellik Pro'ya özel"
    override val featureLockedBody = "Sınırsız AI koçluk, öğün önerileri ve tam haftalık rapor için Pro'ya geç."
    override fun quotaRemaining(remaining: Int, total: Int) = "Bu dönem $remaining/$total hak kaldı"
    override val quotaExhausted = "Ücretsiz hakkın doldu. Sınırsız kullanım için Pro'ya geç."
    // region:workout:tr
    override fun workoutSuggestedWeight(weight: String, reps: Int) = "Önerilen: ${weight.replace('.', ',')} kg × $reps"
    override fun workoutSuggestedReps(reps: Int) = "Önerilen: $reps tekrar"
    override val workoutReasonIncreaseWeight = "Geçen sefer tüm setlerde üst sınıra ulaştın"
    override val workoutReasonAddRep = "Aynı ağırlık, bir tekrar daha"
    override val workoutReasonDeload = "Son iki seans zorladı, %5 hafiflettik"
    override val workoutReasonBodyweight = "Geçen seferden bir tekrar fazla"
    override val workoutCopyPreviousSet = "Önceki seti kopyala"
    override val workoutRestTitle = "Dinlenme"
    override val workoutRestSkip = "Atla"
    override val workoutRestAdd15 = "+15 sn"
    override val workoutReplace = "Değiştir"
    override val workoutReplaceTitle = "Egzersizi değiştir"
    override val workoutReplaceSubtitle = "Aynı kas grubunu çalıştıran alternatifler"
    override val workoutReplaceEmpty = "Bu kas grubu için alternatif bulunamadı."
    override val workoutGym = "Salon"
    override val workoutBodyweight = "Vücut ağırlığı"
    override fun workoutKg(value: String) = "${value.replace('.', ',')} kg"
    override fun workoutMinutes(minutes: Int) = "$minutes dk"
    override val workoutSummaryTitle = "Harika iş!"
    override val workoutSummarySubtitle = "Antrenman tamamlandı, emeğin kayıtta."
    override val workoutSummaryDuration = "Süre"
    override val workoutSummaryVolume = "Toplam hacim"
    override val workoutSummarySets = "Set"
    override fun workoutSummarySetsValue(done: Int, total: Int) = "$done/$total"
    override val workoutSummaryPrTitle = "Yeni kişisel rekorlar"
    override val workoutSummaryNoPr = "Bu sefer rekor yok. Düzenli devam et, rekorlar gelecek."
    override fun workoutPrSet(weight: String, reps: Int) = "${weight.replace('.', ',')} kg × $reps"
    override fun workoutPrE1rm(value: String) = "Tahmini 1TM ${value.replace('.', ',')} kg"
    override val workoutSummaryDone = "Tamam"
    override val workoutTodayModeTitle = "Bugün Modu"
    override val workoutTodayModeEntryBody = "Vaktin az, enerjin düşük ya da bir yerin mi ağrıyor? Seansı bugüne uyarlayalım."
    override val workoutTodayModeSubtitle = "Bugünkü durumunu seç, seansını ona göre kuralım."
    override val workoutTodayMinutesLabel = "Ne kadar vaktin var?"
    override val workoutTodayEnergyLabel = "Enerjin nasıl?"
    override val workoutTodayEnergyLow = "Bitkin"
    override val workoutTodayEnergyHigh = "Zinde"
    override val workoutTodayPainLabel = "Ağrıyan bir yer var mı?"
    override val workoutPainKnee = "Diz"
    override val workoutPainLowerBack = "Bel"
    override val workoutPainShoulder = "Omuz"
    override val workoutPainWrist = "Bilek"
    override val workoutPainNone = "Yok"
    override val workoutTodayLocationLabel = "Nerede çalışacaksın?"
    override val workoutLocationHome = "Ev"
    override val workoutLocationHotel = "Otel"
    override val workoutLocationOutdoor = "Dışarıda"
    override val workoutTodaySubmit = "Seansımı uyarla"
    override val workoutTodayAdapting = "Seansın uyarlanıyor…"
    override val workoutTodayNoteTitle = "Koçun notu"
    // region:planquota:tr
    override fun planQuotaRemaining(remaining: Int, total: Int) = "$remaining/$total hak kaldı"
    override val planQuotaHint = "Her yenileme 1 hak kullanır. İlk planın haktan düşmez."
    override val planQuotaExhausted = "Yenileme hakkın bitti. Mevcut planınla devam edebilirsin."
    override fun planQuotaResetsInDays(days: Int) = "Hakların $days gün sonra yenilenir."
    override val planQuotaConfirmTitle = "Planın yenilensin mi?"
    override fun planQuotaConfirmBody(remainingAfter: Int, total: Int) =
        "Bu işlem 1 yenileme hakkı kullanır; sonrasında $remainingAfter/$total hakkın kalır. " +
            "Şimdiki planın geçmiş planlara taşınır. Plan üretilemezse hakkın düşmez."
    override val planQuotaConfirmAction = "Yenile"
    override val planQuotaErrorExhausted = "Yenileme hakkın kalmadı."
    override val planQuotaErrorInProgress = "Planın zaten hazırlanıyor. Birkaç saniye sonra tekrar dene."
    override val planQuotaErrorInitialLimit = "Bu hesabın ücretsiz ilk plan hakkı doldu."
    override val planGenTitle = "Yeni planın hazırlanıyor"
    override val planGenSubtitle = "Hedefine, ekipmanına ve haftana göre"
    override val planGenSteps = listOf(
        "Hedefin analiz ediliyor",
        "Ekipmanına göre egzersizler seçiliyor",
        "Haftan kuruluyor",
    )
    override val planGenMuscleGroups = listOf("Göğüs", "Sırt", "Bacak", "Omuz", "Kol", "Core", "Kardiyo")
    override val planGenDoneTitle = "Planın hazır"
    override val planGenDoneAction = "Planı gör"
    override val planGenFailedTitle = "Plan yenilenemedi"
    override val planGenFailedBody = "Bağlantını kontrol edip tekrar dene."
    override val planGenFailedKeptRight = "Hakkın kullanılmadı."
    // region:trial:tr
    override val trialUsedOnDeviceTitle = "Ücretsiz deneme bu cihazda kullanıldı"
    override val trialUsedOnDeviceBody =
        "7 günlük deneme her cihazda bir kez kullanılabilir. Pro'ya geçerek tüm özelliklere hemen erişebilirsin."
    override val trialUsedByAccountTitle = "Bu hesabın denemesi bitti"
    override val trialUsedByAccountBody =
        "7 günlük deneme her hesapta bir kez kullanılabilir. Pro ile kaldığın yerden devam et."
    override fun trialActiveTitle(days: Int) = "Denemen sürüyor: $days gün kaldı"
    override val trialActiveBody = "Şimdi Pro'ya geçersen deneme bitince kesintisiz devam edersin."
    override fun trialStartedMessage(days: Int) = "$days günlük denemen başladı"
    override val trialRequiresAccount = "Denemeyi başlatmak için hesabınla giriş yap."
    override val trialStartFailed = "Deneme şu an başlatılamadı. Bağlantını kontrol edip tekrar dene."
    override val planSetupTrialUsedOnDevice =
        "Bu cihazda ücretsiz deneme daha önce kullanıldığı için başlangıç planınla devam ediyorsun."
    override val planSetupTrialUsedByAccount =
        "Bu hesabın deneme süresi daha önce kullanıldığı için başlangıç planınla devam ediyorsun."
    override val planSetupTrialPending = "Deneme şu an başlatılamadı; bağlantın gelince otomatik başlayacak."
    override fun settingsPlanTrial(days: Int) = "Pro deneme · $days gün kaldı"
    override val settingsPlanTrialEnded = "Ücretsiz plan · deneme bitti"
    override fun trialDebugLine(state: String, days: Int, keyPrefix: String, source: String, backend: String) =
        "Deneme (debug): $state · $days gün · cihaz $keyPrefix ($source, $backend)"
    // region:onboarding:tr
    override val onboardingSkip = "Atla"
    override val onboardingAttributionTitle = "Bizi nereden duydun?"
    override val onboardingAttributionSubtitle = "Bir seçenek seç ya da bu adımı atla."
    override fun attributionLabel(code: String) = when (code) {
        com.fitai.app.preferences.AttributionSources.TIKTOK -> "TikTok"
        com.fitai.app.preferences.AttributionSources.INSTAGRAM -> "Instagram"
        com.fitai.app.preferences.AttributionSources.YOUTUBE -> "YouTube"
        com.fitai.app.preferences.AttributionSources.FRIEND -> "Arkadaş tavsiyesi"
        com.fitai.app.preferences.AttributionSources.GYM -> "Spor salonu"
        com.fitai.app.preferences.AttributionSources.SEARCH -> "Google veya mağaza araması"
        else -> "Diğer"
    }
    override fun onboardingPreviewHeadline(days: Int, minutes: Int, equipment: String) =
        "$days gün, $minutes dk, $equipment planın hazır"
    override val onboardingPreviewEquipmentNone = "ekipmansız"
    override val onboardingPreviewEquipmentHome = "ev ekipmanıyla"
    override val onboardingPreviewEquipmentGym = "salon ekipmanıyla"
    override val onboardingPreviewFallbackTitle = "Planın hazır"
    override val onboardingPreviewSubtitle = "İlk haftanın programı ve günlük beslenme hedefin."
    override val onboardingPreviewWeekTitle = "İlk haftan"
    override val onboardingPreviewMacroTitle = "Günlük hedefin"
    override fun onboardingPreviewKcal(kcal: Int) = "$kcal kcal"
    override fun onboardingPreviewGrams(grams: Int) = "$grams g"
    override fun weekdayShort(dayOfWeek: Int) = when (dayOfWeek) {
        1 -> "Pzt"
        2 -> "Sal"
        3 -> "Çar"
        4 -> "Per"
        5 -> "Cum"
        6 -> "Cmt"
        7 -> "Paz"
        else -> "$dayOfWeek"
    }
    override val onboardingCommitmentTitle = "Kendine söz ver"
    override val onboardingCommitmentSubtitle = "Haftalık hedefini seç. Söz verenler planına daha sık uyar."
    override fun onboardingCommitmentStatement(days: Int) = "Haftada $days gün antrenman yapacağım."
    override val onboardingCommitmentDaysLabel = "Haftada kaç gün?"
    override val onboardingCommitmentHold = "Onaylamak için basılı tut"
    override val onboardingCommitmentHoldA11y = "Taahhüdü onayla"
    override fun paywallTrialCta(days: Int) = "$days gün ücretsiz başla"
    override val paywallTrustLine = "Deneme bitmeden 24 saat önce hatırlatırız. İstediğin an iptal et."
    override val paywallRestore = "Satın alımları geri yükle"
    override val paywallRestoreSuccess = "Aboneliğin geri yüklendi."
    override val paywallRestoreNotFound = "Aktif abonelik bulunamadı."
    override fun paywallSavingsBadge(percent: Int) = "%$percent tasarruf"
    override fun paywallMonthlyEquivalent(price: String) = "Yalnızca $price"
    override fun paywallTrialDays(days: Int) = "$days gün ücretsiz deneme"
    override val paywallLoadError = "Planlar yüklenemedi."
    // region:nutrition:tr
    override val nutritionPlateTitle = "Tabak → Plan"
    override val nutritionPlateBody = "Tabağını çek; AI makroları hesaplasın, kalan hedefini güncellesin."
    override val nutritionPlateCta = "Tabağını çek"
    override val nutritionPlateGallery = "Galeriden seç"
    override val nutritionPlateLockedTitle = "Haftalık fotoğraf hakkın doldu"
    override val nutritionAnalysisFailed = "Fotoğraf analiz edilemedi."
    override val nutritionReviewTitle = "Analiz sonucu"
    override val nutritionReviewHint = "Kaydetmeden önce değerleri düzeltebilirsin."
    override val nutritionRemainingTitle = "Kalan makro"
    override val nutritionTrainingDayTarget = "Antrenman günü hedefi"
    override val nutritionRestDayTarget = "Dinlenme günü hedefi"
    override fun nutritionKcalLeft(kcal: Int) = "$kcal kcal kaldı"
    override fun nutritionKcalProgress(consumed: Int, target: Int) = "$consumed / $target kcal"
    override fun nutritionGramsProgress(consumed: Int, target: Int) = "$consumed / $target g"
    override fun nutritionMealMacros(proteinG: Int, carbsG: Int, fatG: Int) =
        "P $proteinG g · K $carbsG g · Y $fatG g"
    override val nutritionNextMealTitle = "Sonraki öğün"
    override val nutritionNextMealLoading = "Sonraki öğünün hazırlanıyor…"
    override val nutritionNextMealError = "Öneri alınamadı."
    override val nutritionNextMealLockedBody = "Kalan makrona göre sonraki öğününü AI planlasın. Pro'ya özel."
    override fun nutritionNextMealMacros(calories: Int, proteinG: Int, carbsG: Int, fatG: Int) =
        "$calories kcal · P $proteinG g · K $carbsG g · Y $fatG g"
    // region:coach:tr
    override val coachTitle = "AI Koç"
    override val coachEntryTeaser = "Kayıtlarını bilen koçuna sor, haftanı gör."
    override val coachWeekTitle = "Bu hafta"
    override val coachStatSessions = "Seans"
    override val coachStatVolume = "Hacim"
    override val coachStatProtein = "Protein"
    override val coachStatWeight = "Kilo"
    override val coachStatPrs = "Rekor"
    override fun coachStatSessionsValue(done: Int, planned: Int) = if (planned > 0) "$done/$planned" else "$done"
    override fun coachStatVolumeValue(kg: Int, changePercent: Int?): String {
        val change = changePercent?.let { if (it >= 0) " · +%$it" else " · -%${-it}" }.orEmpty()
        return "$kg kg$change"
    }
    override fun coachStatProteinValue(avg: Int, target: Int) = if (target > 0) "$avg/$target g" else "$avg g"
    override fun coachStatWeightValue(change: String?) = change?.let { "${it.replace('.', ',')} kg" } ?: "—"
    override val coachHeadlineNoActivity = "Yeni bir hafta. İlk seansınla başla."
    override fun coachHeadlinePlanCompleted(sessions: Int) = "Harika! Planlanan $sessions seansın hepsi tamam."
    override fun coachHeadlineVolumeUp(percent: Int) = "Hacmin geçen haftaya göre %$percent arttı."
    override fun coachHeadlineProteinLow(avg: Int, target: Int) = "Protein ortalaman $avg g, hedefin $target g."
    override fun coachHeadlineInProgress(done: Int, planned: Int) =
        if (planned > 0) "Bu hafta $done/$planned seans tamam. Devam!" else "Bu hafta $done seans tamamladın."
    override val coachReportHighlights = "Öne çıkanlar"
    override val coachReportNextWeek = "Gelecek hafta"
    override val coachReportLockedTitle = "Tam haftalık rapor Pro'da"
    override val coachReportLockedBody = "Koçun haftanı yorumlasın, gelecek hafta için net adımlar versin."
    override val coachReportFailed = "AI raporu şu an hazırlanamadı."
    override val coachReportLoadError = "Haftalık özet yüklenemedi."
    override val coachChatTitle = "Koçuna sor"
    override val coachChatEmpty = "Antrenman ve öğün kayıtlarını biliyorum. Aklındakini sor."
    override val coachPromptKnee = "Dizim ağrıyor, squat yerine ne yapayım?"
    override val coachPromptProtein = "Bu hafta proteinimi nasıl artırırım?"
    override val coachPromptHarder = "Planımı nasıl zorlaştırırım?"
    override val coachInputPlaceholder = "Koçuna yaz…"
    override val coachSendA11y = "Mesajı gönder"
    override val coachTyping = "Koç yazıyor…"
    override val coachDisclaimer = "AI koç tıbbi tavsiye vermez. Ağrın sürerse bir uzmana danış."
    override val coachDailyLimit = "Bugünkü mesaj sınırına ulaştın. Yarın tekrar dene."
    override fun coachMessageTooLong(max: Int) = "Mesaj en fazla $max karakter olabilir."
    override val coachSendFailed = "Mesaj gönderilemedi."
    override val coachChatLockedTitle = "AI Koç sohbeti Pro'da"
    override val coachChatLockedBody = "Kayıtlarını bilen koçuna istediğin an soru sor."
    override val coachClearHistory = "Sohbeti temizle"
    override fun coachCharCount(count: Int, max: Int) = "$count/$max"
    // region:engagement:tr
    override fun engagementStreakDays(days: Int) = "$days gün"
    override val engagementStreakLabel = "Seri"
    override val engagementFreezeAvailable = "Haftalık dondurma hakkın hazır"
    override val engagementFreezeUsed = "Bu haftanın dondurması kullanıldı"
    override val engagementTodayActive = "Bugün tamam"
    override fun engagementLevel(level: Int) = "Seviye $level"
    override fun engagementWeeklyXp(xp: Int) = "Bu hafta $xp XP"
    override fun engagementXpToNext(xp: Int) = "Sonraki seviyeye $xp XP"
    override val engagementChecklistTitle = "İlk adımların"
    override fun engagementChecklistProgress(done: Int, total: Int) = "$done/$total"
    override val engagementChecklistSession = "İlk antrenmanını tamamla"
    override val engagementChecklistMealPhoto = "İlk öğün fotoğrafını çek"
    override val engagementChecklistWeighIn = "İlk tartını gir"
    override fun homeTodayExerciseCount(count: Int) = "$count egzersiz"
    override fun homeMacrosLeft(proteinG: Int, carbsG: Int, fatG: Int) =
        "P $proteinG g · K $carbsG g · Y $fatG g kaldı"
    override val homeNoDietPlan = "Diyet planın hazır olunca hedeflerin burada"
    override val homeWeekTitle = "Bu hafta"
    override fun homeVolumeTonnes(tonnes: String) = "$tonnes t"
    override fun homeVolumeChange(percent: Int) =
        if (percent >= 0) "+%$percent geçen haftaya göre" else "−%${-percent} geçen haftaya göre"
    override val progressWeightTitle = "Kilo trendi"
    override val progressWeightEmpty = "Trendini görmek için tartı ekle"
    override fun progressWeightLatest(weight: String) = "$weight kg"
    override fun progressWeightChange(change: String) = "90 günde $change kg"
    override val progressAddWeighIn = "Tartı ekle"
    override val progressWeightLabel = "Kilo (kg)"
    override val progressWaistLabel = "Bel (cm, isteğe bağlı)"
    override val progressWeightInvalid = "20–400 kg arası bir değer gir"
    override val progressWaistInvalid = "30–300 cm arası bir değer gir"
    override val progressWeighInSaved = "Tartı kaydedildi"
    override val progressSaveFailed = "Kaydedilemedi, tekrar dene"
    override val progressLoadError = "İlerleme yüklenemedi"
    override val progressVolumeTitle = "Haftalık hacim"
    override val progressVolumeEmpty = "Seans tamamladıkça hacmin burada"
    override fun progressWeekShort(day: Int, month: Int) = "$day.$month"
    override val progressPrTitle = "Kişisel rekorlar"
    override val progressPrEmpty = "Seans tamamladıkça rekorların burada"
    override fun progressPrValue(weight: String, reps: Int) = "$weight kg × $reps"
    override fun progressE1rm(value: String) = "e1RM $value kg"
    override val progressBadgesTitle = "Rozetler"
    override fun progressBadgesCount(unlocked: Int, total: Int) = "$unlocked/$total"
    override val badgeFirstSession = "İlk seans"
    override val badgeFirstMealPhoto = "İlk tabak"
    override val badgeFirstWeighIn = "İlk tartı"
    override val badgeFirstPr = "İlk rekor"
    override val badgeStreak7 = "7 gün seri"
    override val badgeStreak28 = "4 hafta seri"
    override val badgeSessions10 = "10 seans"
    override val badgeMeals100 = "100 öğün"
    override val badgeVolume10t = "10 ton"
    // region:notifications:tr
    override val notificationsPermissionTitle = "Antrenman saatini hatırlatayım mı?"
    override val notificationsPermissionSubtitle = "Antrenman günlerinde seçtiğin saatte kısa bir hatırlatma gönderirim."
    override val notificationsHourUnit = "sa"
    override val notificationsMinuteUnit = "dk"
    override fun notificationsTimeSummary(time: String) = "Antrenman günlerinde saat $time"
    override val notificationsRemind = "Hatırlat"
    override val notificationsNotNow = "Şimdi değil"
    override val notificationsProfileSection = "Hatırlatmalar"
    override val notificationsProfileToggle = "Antrenman ve öğün hatırlatmaları"
    override val notificationsProfileTime = "Hatırlatma saati"
    override val notificationsPermissionDenied = "Bildirim izni kapalı. Cihaz ayarlarından açabilirsin."
    override val notificationsUpdateFailed = "Hatırlatmalar güncellenemedi"
    override val notificationsChannelWorkout = "Antrenman hatırlatmaları"
    override val notificationsChannelMeals = "Öğün hatırlatmaları"
    override val notificationsChannelProgram = "Program ve abonelik"
    override val notificationsWorkoutTitle = "Antrenman zamanı"
    override val notificationsWorkoutBody = "Bugünkü seansın hazır. Hadi başlayalım."
    override val notificationsMealTitle = "Öğle tabağın"
    override val notificationsMealBody = "Tabağının fotoğrafını çek, makrolarını hesaplayayım."
    override val notificationsDay0Title = "İlk seansın hazır"
    override val notificationsDay0Body = "Planın hazır. Başlamaya hazır mısın?"
    override val notificationsDay1Title = "Öğle tabağını çek"
    override val notificationsDay1Body = "Öğle tabağının fotoğrafını çek. Akşam için öneri vereyim."
    override val notificationsDay2Title = "Dinlenmek de plana dahil"
    override val notificationsDay2Body = "Planlı dinlenme günleri serini bozmaz. Protein hedefini unutma."
    override val notificationsDay3Title = "Ağırlıkların güncellendi"
    override val notificationsDay3Body = "Önerilen ağırlıkların son seansına göre hazır. Biraz daha yüklen."
    override val notificationsDay5Title = "Zamanın az mı?"
    override val notificationsDay5Body = "20 dakikalık Bugün Modu seansını dene."
    override val notificationsDay7Title = "Haftalık raporun hazır"
    override val notificationsDay7Body = "İlk haftan nasıl geçti? Raporuna göz at."
    override val notificationsTrialEndTitle = "Denemen yarın bitiyor"
    override val notificationsTrialEndBody = "Pro özellikler 24 saat sonra kapanır. Devam etmek için planını seç."
    // region:funnel:tr
    override val languageCode = "tr"
    override fun languageName(code: String) = funnelLanguageName(code)
    override val actionClose = "Kapat"
    override val actionYes = "Evet"
    override val actionNo = "Hayır"
    override fun percentLabel(value: Int) = "%$value"
    override fun longDate(day: Int, month: Int, year: Int) = "$day ${TR_MONTHS[month - 1]} $year"
    override fun shortDate(day: Int, month: Int) = "$day ${TR_MONTHS[month - 1].take(3)}"
    override fun dateRange(start: String, end: String) = "$start – $end"
    override val welcomeProofPlanTitle = "60 saniyede"
    override val welcomeProofPlanBody = "sana özel antrenman planı"
    override val welcomeProofCoachTitle = "Tek AI koç"
    override val welcomeProofCoachBody = "antrenman ve beslenme birlikte"
    override val welcomeProofProgressTitle = "Her seansta"
    override val welcomeProofProgressBody = "ilerlemene göre güncellenen ağırlıklar"
    override val welcomeHaveAccountPrefix = "Zaten hesabın var mı?"
    override val welcomeSignInLink = "Giriş yap"
    override val onboardingIntroTrackTitle = "Antrenmanlarını takip et"
    override val onboardingIntroTrackBody = "Her setini, tekrarını ve ağırlığını kaydet; FitAI bir sonraki seansı buna göre ayarlasın."
    override val onboardingIntroProgressTitle = "İlerlemeni analiz et"
    override val onboardingIntroProgressBody = "Kas grubu ve egzersiz bazında hacim, rekor ve kilo trendlerini gör."
    override val onboardingIntroPlateTitle = "Tabağını çek, gerisini AI yapsın"
    override val onboardingIntroPlateBody = "Fotoğraftan kalori ve makroları hesaplar, günün kalan hedefini anında günceller."
    override val onboardingMockSet = "Set"
    override val onboardingMockPrevious = "Önceki"
    override val onboardingMockKg = "KG"
    override val onboardingMockReps = "TEKRAR"
    override val onboardingMockExercise = "Bench press"
    override val onboardingMockMaxWeight = "Maks. ağırlık"
    override val onboardingMockLastMonth = "Son 1 ay"
    override fun onboardingMockWeek(week: Int) = "$week. hf"
    override val onboardingMockMealName = "Tavuklu kinoa kasesi"
    override val onboardingObstacleTitle = "Bu hedefe ulaşmanı engelleyen ne?"
    override fun obstacleLabel(code: String) = when (code) {
        TrainingObstacle.Time.code -> "Zaman yetersizliği"
        TrainingObstacle.Knowledge.code -> "Ne yapacağımı bilmiyorum"
        TrainingObstacle.Motivation.code -> "Düşük motivasyon"
        TrainingObstacle.Injury.code -> "Sakatlık veya ağrı"
        TrainingObstacle.Equipment.code -> "Ekipman eksikliği"
        else -> "Daha önce hiç denemedim"
    }
    override val onboardingFocusTitle = "FitAI'ı ne için kullanmak istiyorsun?"
    override val onboardingFocusSubtitle = "En fazla 3 seçenek seç."
    override fun focusLabel(code: String) = when (code) {
        FocusArea.PersonalPlan.code -> "Kişisel antrenman planı al"
        FocusArea.SmartOverload.code -> "Akıllı ağırlık önerileri"
        FocusArea.PlatePhoto.code -> "Tabağımı çekip makro takip et"
        FocusArea.LogLifts.code -> "Set ve ağırlıklarımı kaydet"
        FocusArea.Streak.code -> "Serimi koruyarak tutarlı kal"
        else -> "AI koça soru sor"
    }
    override fun focusDescription(code: String) = when (code) {
        FocusArea.PersonalPlan.code -> "Ekipmanına, seviyene ve hedefine göre AI ile hazırlanır."
        FocusArea.SmartOverload.code -> "Her egzersizde ne kadar ekleyeceğini hesaplar, platoya girmezsin."
        FocusArea.PlatePhoto.code -> "Tek fotoğraftan kalori ve makro hesabı."
        FocusArea.LogLifts.code -> "Geçmiş seanslar ve rekorların tek yerde."
        FocusArea.Streak.code -> "Planlı dinlenme günleri seriyi bozmaz."
        else -> "Kayıtlarını bilen koçtan anında cevap."
    }
    override val onboardingExperienceTitle = "Antrenman konusunda ne kadar deneyimlisin?"
    override fun experienceLabel(code: String) = when (code) {
        TrainingExperience.None.code -> "Hiç antrenman yapmadım"
        TrainingExperience.Beginner.code -> "Acemi · Daha önce denedim"
        TrainingExperience.Intermediate.code -> "Orta · Düzenli antrenman"
        else -> "İleri · Yıllardır antrenman"
    }
    override val onboardingGenderTitle = "Cinsiyetin nedir?"
    override val onboardingGenderSubtitle = "Kalori ve makro hedefini hesaplamak için kullanılır."
    override fun genderLabel(code: String) = if (code == Gender.Female.code) "Kadın" else "Erkek"
    override val onboardingWeightPrivacy = "Bilgilerin gizli kalır, kimseyle paylaşılmaz."
    override val onboardingWeightUsage = "Kilon, kalori hedefini ve önerilen ağırlıkları hesaplamak için kullanılır."
    override val onboardingStatementTitle = "Bu ifadeye katılıyor musun?"
    override val onboardingStatementConsistency =
        "“Şimdiye kadar hiçbir seansı kaçırmadan, tutarlı şekilde antrenman yapamadım.”"
    override val onboardingStatementNutrition = "“Ne yiyeceğimi bilmediğim için diyetlerim hep yarıda kaldı.”"
    override val onboardingStoryDataTitle = "Sıkı çalıştığını düşünüyorsun ama bunu kanıtlayacak verin yok"
    override val onboardingStoryDataBody =
        "Geçen aya göre daha fazla kaldırıp kaldırmadığını bilmiyorsun. Hislerine göre çalışıyorsun ve hisler yanıltır."
    override val onboardingStorySystemTitle = "Bu senin hatan değil. İşe yarayan bir sisteme ihtiyacın var."
    override val onboardingStorySystemBody =
        "Sonuç alanlar senden farklı değil. Sadece her gün ne yapacaklarını söyleyen bir planları ve beslenmelerini takip eden bir koçları var."
    override val onboardingStoryPlanTitle = "Hazır programları kopyalama. Kendine ait olanı al."
    override val onboardingStoryPlanBody =
        "FitAI profilini analiz eder; vücuduna, ekipmanına ve hedefine göre antrenman ve beslenme planı kurar."
    override val onboardingMockPlanTitle = "Sırt & Biceps"
    override val onboardingMockPlanExercises = listOf("Lat pulldown", "Barbell row", "Oturarak kablo row", "Hammer curl")
    override fun onboardingMockPlanMore(count: Int) = "+$count egzersiz daha"
    override val onboardingMockToday = "Bugün"
    override val onboardingDaysTitle = "Haftada kaç gün antrenman yapmak istiyorsun?"
    override fun onboardingDaysLabel(days: Int) = "Haftada $days gün"
    override fun onboardingDaysTag(days: Int) = when (days) {
        2 -> "Sağlam başlangıç"
        3 -> "Tutarlı"
        4 -> "Kararlı"
        5 -> "İnanılmaz"
        else -> "Bahane yok"
    }
    override val onboardingDurationTitle = "Antrenmanların ne kadar sürsün?"
    override fun onboardingDurationLabel(minutes: Int) = if (minutes >= 90) "90+ dakika" else "$minutes dakika"
    override fun onboardingDurationTag(minutes: Int) = when {
        minutes <= 15 -> "Ekspres"
        minutes <= 30 -> "Verimli"
        minutes <= 45 -> "Dengeli"
        minutes <= 60 -> "Yoğun"
        else -> "Tam gaz"
    }
    override val onboardingNotifyTitle = "Bildirimleri aç, hedeflerine odaklan"
    override val onboardingNotifyBody = "Antrenman günlerinde kısa bir hatırlatma; serini korumanın en kolay yolu."
    override val onboardingNotifyMockClock = "9:41"
    override val onboardingNotifyMockTitle = "Bugün antrenman günü!"
    override val onboardingNotifyMockBody = "Sırt & Biceps · 45 dk"
    override val onboardingNotifyMockTime = "şimdi"
    override val onboardingAnalyzingTitle = "Planın oluşturuluyor…"
    override val onboardingAnalyzingSteps = listOf(
        "Profilin analiz ediliyor…",
        "Antrenman yoğunluğu ayarlanıyor…",
        "Beslenme hedeflerin hesaplanıyor…",
        "Planın son haline getiriliyor…",
    )
    override fun onboardingResultTitle(name: String) =
        if (name.isBlank()) "Tebrikler!\nKişisel planın hazır." else "Tebrikler $name!\nKişisel planın hazır."
    override val onboardingResultSubtitle = "Bu planla hedefine ulaşabileceğin tarih:"
    override fun onboardingResultBenefits(goal: String) = when (goal) {
        FitnessGoal.LoseFat.code -> listOf(
            "Yağ yakarken kasını koruyacaksın",
            "Kalori hedefin her gün hazır olacak",
            "Serinle motivasyonun yüksek kalacak",
        )
        FitnessGoal.BuildMuscle.code -> listOf(
            "Kas kazanımını en üst düzeye çıkaracaksın",
            "Her seansta ne kadar ekleyeceğini bileceksin",
            "Kendine çok daha fazla güveneceksin",
        )
        FitnessGoal.GetStronger.code -> listOf(
            "Ana hareketlerde rekorlarını kıracaksın",
            "Ağırlık artışını sistemli yapacaksın",
            "Kendine çok daha fazla güveneceksin",
        )
        FitnessGoal.ImproveEndurance.code -> listOf(
            "Daha uzun süre, daha güçlü çalışacaksın",
            "Yoğunluğu dengeli artıracaksın",
            "Her hafta ilerlemeni göreceksin",
        )
        else -> listOf(
            "Düzenli hareket alışkanlığı kazanacaksın",
            "Dengeli beslenmeyi kolayca takip edeceksin",
            "Kendini her gün daha enerjik hissedeceksin",
        )
    }
    override val onboardingResultChartTitle = "Neler yapabileceğini keşfet"
    override val onboardingResultWithFitAI = "FitAI ile"
    override val onboardingResultWithoutFitAI = "FitAI olmadan"
    override val onboardingResultPlateau = "Plato"
    override val onboardingQuoteLines = listOf("Unutma", "Hiçbir şey değişmez", "sen değişmedikçe")
    override val onboardingPledgeLead = "FitAI ile antrenman yapacağıma söz veriyorum"
    override fun onboardingPledgeDays(days: Int) = "haftada $days kez"
    override val onboardingPledgeTagline = "Bahane yok, bırakmak yok."
    override val onboardingPledgeHold = "İmzalamak için basılı tut"
    override val onboardingPledgeSigned = "İmzalandı"
    override val paywallChooseTitle = "Planını seç"
    override fun paywallPlanReadyFor(name: String) =
        if (name.isBlank()) "Kişisel planın hazır" else "$name, kişisel planın hazır"
    override fun paywallSummaryDays(days: Int) = "Haftada $days gün"
    override val paywallIncludesAi = "AI ile kişisel antrenman ve beslenme planı"
    override val paywallIncludesOverload = "Her seansta güncellenen ağırlık önerileri"
    override val paywallIncludesCoach = "Sınırsız AI koç ve tabak analizi"
    override val paywallNoCommitment = "Taahhüt yok, istediğin zaman iptal et"
    override val paywallExitWarning = "Bu ekranı kapatırsan kişisel planını kaybedersin"
    override val paywallExitOfferTitle = "Sınırlı süreli teklif"
    override fun paywallExitOfferBody(weeklyPrice: String) = "Bugün yalnızca $weeklyPrice / hafta ile başla"
    override val paywallExitDecline = "Kişisel plan olmadan devam et"
    override val paywallExitUnlock = "Kişisel planımın kilidini aç"
    override fun paywallPerWeek(price: String) = "$price / hafta"
    override val authSignupTitle = "Planını kaydetmek için hesabını oluştur"
    override val authSignupSubtitle = "Verilerin güvende kalsın, her cihazda kaldığın yerden devam et."
    override val authContinueEmail = "E-posta ile devam et"
    override val authNoAccount = "Hesabın yok mu?"
    override val authSignUpLink = "Kaydol"
    override val authLegal = "Devam ederek Gizlilik Politikası ve Kullanım Şartları'nı kabul etmiş olursun."
    override val authSignInHeading = "Giriş yap"
    override val authEmailHeading = "E-posta ile kaydol"
    override val authOr = "veya"
    override val authPasswordPlaceholder = "En az 6 karakter"
    override val authForgotPassword = "Şifreni mi unuttun?"
    override val authResetSent = "Şifre sıfırlama bağlantısı e-postana gönderildi."
    override val authShowPassword = "Şifreyi göster"
    override val authHidePassword = "Şifreyi gizle"
    override val authCreateAccount = "Hesabımı oluştur"
    override val authSignInCta = "Giriş yap"
    override val authOfflineContinue = "Hesapsız devam et (çevrimdışı mod)"
    override val authNoProfileFound = "Bu hesapta kayıtlı profil yok. Planını oluşturmak için birkaç soruyu yanıtla."
    override val validationPasswordShort = "Şifre en az 6 karakter olmalı."
    override val validationEmailRequired = "E-posta adresini gir."
    override val planSetupPersonalTitle = "Kişisel AI planın hazırlanıyor…"
    override val planSetupStarterTitle = "Başlangıç planın hazırlanıyor…"
    override val planSetupStepWorkout = "Antrenman programın kuruluyor"
    override val planSetupStepNutrition = "Beslenme hedeflerin hesaplanıyor"
    override val planSetupStepReminders = "Hatırlatmaların ayarlanıyor"
    override val planSetupErrorTitle = "Kişisel plan şu an oluşturulamadı"
    override val planSetupErrorBody = "Tekrar deneyebilir ya da şimdilik başlangıç planıyla devam edebilirsin."
    override val planSetupUseStarter = "Başlangıç planıyla devam et"
    override val challengeTitle = "Başlangıç Görevleri"
    override val challengeTaskNotifications = "Bildirimleri aç"
    override val challengeGift = "Hediyeni aç"
    override val challengeGiftLocked = "Tüm görevleri tamamla, hediyen açılsın"
    override val challengeAllDone = "Tüm görevler tamam! Hediyen hazır"
    override val challengeHide = "Tekrar gösterme"
    override val challengeOpenA11y = "Başlangıç görevlerini aç"
    override val streakTitle = "Antrenman serisi"
    override fun streakQuote(days: Int) = when {
        days <= 0 -> "Bugün ilk adımı at.\nSerin seninle başlar."
        days == 1 -> "“Bir seri başlatmıyorsun.”\nYeni birine dönüşüyorsun."
        days < 7 -> "Her seans bir sonrakini kolaylaştırır.\nBöyle devam!"
        days < 28 -> "Bir haftayı devirdin.\nArtık bu senin rutinin."
        else -> "Bir aydır vazgeçmiyorsun.\nSonuçlar kendini gösterecek."
    }
    override val streakHowItWorks = "Seri nasıl çalışır?"
    override val streakHowItWorksBody =
        "Planlı antrenman gününde seans tamamladığında serin büyür. Planlı dinlenme günleri seriyi bozmaz. " +
            "Bir antrenman gününü kaçırırsan haftada bir dondurma hakkın seriyi korur."
    override fun streakLongest(days: Int) = "En uzun seri: $days gün"
    override fun streakA11y(days: Int) = "Antrenman serisi: $days gün"
    override val profileStatWorkouts = "Antrenman"
    override val profileStatStreak = "Seri"
    override val profileStatRecords = "Rekor"
    override val profileMeasurements = "Ölçümler"
    override val profileTabWorkouts = "Antrenmanlar"
    override val profileTabCharts = "Grafik"
    override val profileNoWorkouts = "Henüz antrenman yok"
    override val profileNoWorkoutsBody = "İlk seansını tamamladığında burada görünecek."
    override val profileChartEmptyTitle = "Henüz veri yok"
    override val profileChartEmptyBody = "Zaman içindeki ilerlemeni burada göreceksin"
    override val profileLogWorkout = "Antrenman kaydet"
    override val profileGuestName = "Sporcu"
    override val profilePeriod = "Dönem"
    override fun trendMetricLabel(code: String) = when (code) {
        "Duration" -> "Süre"
        "Sessions" -> "Seans"
        else -> "Hacim"
    }
    override fun trendPeriodLabel(weeks: Int) = if (weeks >= 26) "6 ay" else "$weeks hafta"
    override fun historyMeta(minutes: Int, volume: String, sets: Int) = "$minutes dk · $volume · $sets set"
    override val settingsProfileSection = "Profil bilgileri"
    override val settingsSubscription = "Abonelik"
    override val settingsPlanPro = "FitAI Pro aktif"
    override val settingsPlanFree = "Ücretsiz plan"
    override val settingsLanguage = "Dil"
    override fun settingsSignedInAs(email: String) = "$email ile giriş yapıldı"
    override val settingsGuestAccount = "Henüz hesaba bağlı değilsin"
    override val measurementWaist = "Bel"
    override val measurementSameAsStart = "Başlangıçtakiyle aynı"
    override fun measurementChange(signed: String, unit: String) = "Başlangıca göre ${signed.replace('.', ',')} $unit"
    override val measurementEntries = "Kayıtlar"
    override val measurementEmpty = "Ölçüm ekledikçe grafiğin burada oluşur"
    override fun measurementPeriodLabel(months: Int) = if (months >= 12) "1 yıl" else "$months ay"
    override fun cmValue(value: String) = "${value.replace('.', ',')} cm"
}

private val TR_MONTHS = listOf(
    "Ocak", "Şubat", "Mart", "Nisan", "Mayıs", "Haziran",
    "Temmuz", "Ağustos", "Eylül", "Ekim", "Kasım", "Aralık",
)

private val EN_MONTHS = listOf(
    "January", "February", "March", "April", "May", "June",
    "July", "August", "September", "October", "November", "December",
)

private fun funnelLanguageName(code: String) = when (code) {
    "en" -> "English"
    else -> "Türkçe"
}

object StringsEn : Strings {
    override val appName = "FitAI"
    override val splashLoading = "Loading…"
    override val tabHome = "Home"
    override val tabWorkout = "Workout"
    override val tabNutrition = "Nutrition"
    override val tabProgress = "Progress"
    override val tabProfile = "Profile"
    override val homeTitle = "Home"
    override val homeSubtitle = "Your daily summary"
    override val homeTodayWorkout = "Today's workout"
    override val homeNoWorkout = "No exercises scheduled for today"
    override val homeMacros = "Daily macros"
    override val homeQuickLogMeal = "Log a meal"
    override fun homeTrialRemaining(days: Int) = "Trial: $days days left"
    override val homeTrialExpired = "Your trial has expired"
    override val welcomeTitle = "FitAI"
    override val welcomeHeadline = "Reach the body\nyou want with AI"
    override val welcomeSubtitle =
        "Personalized AI workouts and nutrition designed for peak performance."
    override val welcomeCta = "Get started"
    override val welcomeHaveAccount = "I already have an account"
    override fun onboardingProgress(step: Int, total: Int) = "Step $step of $total"
    override val onboardingLabelName = "Full Name"
    override val onboardingNameTitle = "What should we call you?"
    override val onboardingNameSubtitle = "Let's start with the basics to personalize your experience."
    override val onboardingNamePlaceholder = "e.g. Alex Jensen"
    override val onboardingLabelEmail = "Email Address"
    override val onboardingLabelAge = "Age"
    override val onboardingAgeTitle = "How old are you?"
    override val onboardingAgeSubtitle = "This helps us personalize your fitness plan."
    override val onboardingAgeUnit = "Yrs"
    override val onboardingLabelHeight = "Height (cm)"
    override val onboardingHeightTitle = "What's your height?"
    override val onboardingHeightSubtitle = "This helps us calculate your metabolic rate."
    override val onboardingHeightUnitCm = "cm"
    override val onboardingHeightUnitFt = "ft/in"
    override val onboardingLabelWeight = "Weight (kg)"
    override val onboardingWeightTitle = "What's your current weight?"
    override val onboardingWeightSubtitle = "This helps us calculate your daily caloric needs."
    override val onboardingWeightUnit = "kg"
    override val onboardingGoalTitle = "What's your main goal?"
    override val onboardingGoalSubtitle = "Select the option that best describes your fitness objective."
    override val onboardingActivityTitle = "How active are you day to day?"
    override val onboardingActivitySubtitle = "This helps us tailor your fitness plan accurately."
    override val onboardingEquipmentTitle = "What equipment can you use?"
    override val onboardingEquipmentSubtitle = "Select all that apply to help us tailor your workouts."
    override val onboardingDietTitle = "Any diet preferences or restrictions?"
    override val onboardingDietSubtitle = "Select all that apply to help us tailor your nutrition plan."
    override val onboardingCreatePlan = "Create my plan"
    override val onboardingDietNotesLabel = "Anything else?"
    override val onboardingDietNotesPlaceholder = "E.g. Peanut allergy, intermittent fasting..."
    override val onboardingGeneratingTitle = "Finalizing your plan..."
    override val onboardingGeneratingSubtitle = "Your AI coach is building workout and nutrition plans."
    override val onboardingGeneratingWorkout = "Building your workout plan..."
    override val onboardingGeneratingNutrition = "Crafting your nutrition plan..."
    override val onboardingGeneratingOptimize = "Optimizing for your goal..."
    override val onboardingTrainerTip = "Trainer Tip"
    override val onboardingTrainerTipBody =
        "Your plan is designed specifically for your equipment and diet preferences. Consistency is the key to seeing results."
    override val onboardingErrorTitle = "Something went wrong"
    override val onboardingErrorSubtitle = "We couldn't generate your plan. Retry or go back to review your answers."
    override fun goalLabel(code: String) = when (code) {
        FitnessGoal.LoseFat.code -> "Lose fat"
        FitnessGoal.BuildMuscle.code -> "Build muscle"
        FitnessGoal.GetStronger.code -> "Get stronger"
        FitnessGoal.ImproveEndurance.code -> "Improve endurance"
        FitnessGoal.StayHealthy.code -> "Stay healthy"
        else -> code
    }
    override fun goalDescription(code: String) = when (code) {
        FitnessGoal.LoseFat.code -> "Focus on weight loss and toning"
        FitnessGoal.BuildMuscle.code -> "Hypertrophy and mass building"
        FitnessGoal.GetStronger.code -> "Increase raw power and lifting capacity"
        FitnessGoal.ImproveEndurance.code -> "Cardio health and stamina"
        FitnessGoal.StayHealthy.code -> "General wellness and maintenance"
        else -> ""
    }
    override fun activityLabel(code: String) = when (code) {
        ActivityLevel.Sedentary.code -> "Sedentary"
        ActivityLevel.Light.code -> "Lightly active"
        ActivityLevel.Moderate.code -> "Moderately active"
        ActivityLevel.Very.code -> "Very active"
        ActivityLevel.Athlete.code -> "Athlete"
        else -> code
    }
    override fun activityDescription(code: String) = when (code) {
        ActivityLevel.Sedentary.code -> "Mostly sitting, office work, little to no exercise."
        ActivityLevel.Light.code -> "Light exercise 1-3 days a week, standing work."
        ActivityLevel.Moderate.code -> "Moderate exercise 3-5 days a week."
        ActivityLevel.Very.code -> "Hard exercise 6-7 days a week."
        ActivityLevel.Athlete.code -> "Very hard exercise, physical job or training twice a day."
        else -> ""
    }
    override fun equipmentLabel(code: String) = when (code) {
        EquipmentOption.Bodyweight.code -> "None (Bodyweight only)"
        EquipmentOption.Dumbbells.code -> "Dumbbells"
        EquipmentOption.Barbell.code -> "Barbell"
        EquipmentOption.ResistanceBands.code -> "Resistance Bands"
        EquipmentOption.PullUpBar.code -> "Pull-up Bar"
        EquipmentOption.FullGym.code -> "Full Gym Access"
        else -> code
    }
    override val equipmentFullGymSubtitle = "Commercial gym equipment"
    override fun dietLabel(code: String) = when (code) {
        DietRestriction.None.code -> "None"
        DietRestriction.Vegetarian.code -> "Vegetarian"
        DietRestriction.Vegan.code -> "Vegan"
        DietRestriction.Pescatarian.code -> "Pescatarian"
        DietRestriction.Halal.code -> "Halal"
        DietRestriction.Kosher.code -> "Kosher"
        DietRestriction.GlutenFree.code -> "Gluten-free"
        DietRestriction.DairyFree.code -> "Dairy-free"
        else -> code
    }
    override val workoutTitle = "Workout"
    override val workoutActivePlan = "Active plan"
    override val workoutHistory = "Plan history"
    override val workoutRegenerate = "Regenerate plan"
    override val workoutStartSession = "Start workout"
    override fun workoutExercisesCount(count: Int) = "$count exercises"
    override fun workoutSetsReps(sets: Int, reps: String) = "$sets sets × $reps"
    override fun workoutRest(seconds: Int) = "Rest: ${seconds}s"
    override val workoutSessionActive = "Active workout"
    override val workoutCompleteSession = "Finish workout"
    override fun workoutSetNumber(number: Int) = "Set $number"
    override val workoutWeightKg = "Weight (kg)"
    override val workoutActualReps = "Reps"
    override val workoutNoPlan = "No workout plan yet"
    override val nutritionTitle = "Nutrition"
    override val nutritionTargets = "Daily targets"
    override val nutritionCalories = "Calories"
    override val nutritionProtein = "Protein"
    override val nutritionCarbs = "Carbs"
    override val nutritionFat = "Fat"
    override val nutritionTodayMeals = "Today's meals"
    override val nutritionLogMeal = "Log meal"
    override val nutritionPhotoMeal = "Analyze from photo"
    override val nutritionMealName = "Meal name"
    override val nutritionRegenerate = "Regenerate diet plan"
    override val nutritionNoPlan = "No diet plan yet"
    override val nutritionAnalyzing = "Analyzing photo…"
    override val paywallTitle = "FitAI Premium"
    override val paywallSubtitle = "Unlimited AI plans and progress tracking."
    override val paywallFeatureWorkouts = "Unlimited AI Workouts"
    override val paywallFeatureNutrition = "Detailed Nutrition Plans"
    override val paywallFeatureProgress = "Progress Analytics"
    override val paywallComparePlans = "Compare Plans"
    override val paywallMonthly = "Monthly"
    override val paywallYearly = "Yearly"
    override val paywallSubscribe = "Subscribe"
    override val paywallSignIn = "Sign in"
    override val paywallStartTrial = "Start Free Trial"
    override val paywallContinueGuest = "Continue as Guest"
    override val paywallComingSoon = "Subscription coming soon"
    override val profileTitle = "Profile"
    override val profileCloudHint =
        "Your data is stored on this device first. Sign in to sync or back up across devices."
    override fun profileLocalSummary(name: String, goal: String, activity: String) =
        "$name · $goal · $activity"
    override fun profileDietRestrictions(value: String) = "Diet: $value"
    override val profileGoogle = "Continue with Google"
    override val profileApple = "Continue with Apple"
    override val profileSignOut = "Sign out"
    override val profileAccount = "Account"
    override val profileSettings = "Settings"
    override val profileEdit = "Edit profile"
    override val profileRegeneratePlans = "Regenerate plans"
    override val profileRegenerating = "Regenerating plans…"
    override val profileSaved = "Profile saved"
    override val profilePlansRegenerated = "Plans regenerated"
    override val profileAge = "Age"
    override val profileHeight = "Height"
    override val profileWeight = "Weight"
    override val profileGoal = "Goal"
    override val profileActivity = "Activity"
    override val profileEquipment = "Equipment"
    override val profileDiet = "Diet"
    override val profileEmailAuth = "Sign in with email"
    override val authTitleSignIn = "Sign in with email"
    override val authTitleRegister = "Create account"
    override val authPassword = "Password"
    override val actionContinue = "Continue"
    override val actionBack = "Back"
    override val actionFinish = "Finish"
    override val actionSubmit = "Submit"
    override val actionSave = "Save"
    override val actionRetry = "Retry"
    override val actionCancel = "Cancel"
    override val firebaseUnavailable = "Cloud sign-in is unavailable (Firebase is not configured)."
    override val validationNameRequired = "Please enter your name."
    override val validationNumbersRequired = "Enter valid numbers for age, height, and weight."
    override val validationFieldsRequired = "Please make a selection."
    override val validationEmailPasswordRequired = "Email and password are required."
    override val errorUnknown = "Something went wrong."
    override val trialExpired = "Your trial has expired."
    override val syncFailed = "Could not sync profile to the cloud."
    override val appleUnsupportedAndroid = "Apple Sign-In is not supported on this platform."
    override val sampleEmailLabel = "Email"
    override val sampleEmailPlaceholder = "you@fitai.app"
    override val actionPrimaryExample = "Sample primary button"
    // region:foundation:en
    override val proBadge = "PRO"
    override val upgradeToPro = "Upgrade to Pro"
    override val featureLockedTitle = "This is a Pro feature"
    override val featureLockedBody = "Upgrade to Pro for unlimited AI coaching, meal suggestions and the full weekly report."
    override fun quotaRemaining(remaining: Int, total: Int) = "$remaining/$total left this period"
    override val quotaExhausted = "You've used your free uses. Upgrade to Pro for unlimited access."
    // region:workout:en
    override fun workoutSuggestedWeight(weight: String, reps: Int) = "Suggested: $weight kg × $reps"
    override fun workoutSuggestedReps(reps: Int) = "Suggested: $reps reps"
    override val workoutReasonIncreaseWeight = "You hit the top of the range on every set last time"
    override val workoutReasonAddRep = "Same weight, one more rep"
    override val workoutReasonDeload = "Last two sessions were tough, 5% lighter"
    override val workoutReasonBodyweight = "One more rep than last time"
    override val workoutCopyPreviousSet = "Copy previous set"
    override val workoutRestTitle = "Rest"
    override val workoutRestSkip = "Skip"
    override val workoutRestAdd15 = "+15s"
    override val workoutReplace = "Replace"
    override val workoutReplaceTitle = "Replace exercise"
    override val workoutReplaceSubtitle = "Alternatives that train the same muscles"
    override val workoutReplaceEmpty = "No alternatives found for this muscle group."
    override val workoutGym = "Gym"
    override val workoutBodyweight = "Bodyweight"
    override fun workoutKg(value: String) = "$value kg"
    override fun workoutMinutes(minutes: Int) = "$minutes min"
    override val workoutSummaryTitle = "Great work!"
    override val workoutSummarySubtitle = "Workout complete, your effort is logged."
    override val workoutSummaryDuration = "Duration"
    override val workoutSummaryVolume = "Total volume"
    override val workoutSummarySets = "Sets"
    override fun workoutSummarySetsValue(done: Int, total: Int) = "$done/$total"
    override val workoutSummaryPrTitle = "New personal records"
    override val workoutSummaryNoPr = "No records this time. Stay consistent and they will come."
    override fun workoutPrSet(weight: String, reps: Int) = "$weight kg × $reps"
    override fun workoutPrE1rm(value: String) = "Est. 1RM $value kg"
    override val workoutSummaryDone = "Done"
    override val workoutTodayModeTitle = "Today Mode"
    override val workoutTodayModeEntryBody = "Short on time, low on energy or something hurts? Let's fit the session to today."
    override val workoutTodayModeSubtitle = "Tell us how today looks and we'll build your session around it."
    override val workoutTodayMinutesLabel = "How much time do you have?"
    override val workoutTodayEnergyLabel = "How's your energy?"
    override val workoutTodayEnergyLow = "Drained"
    override val workoutTodayEnergyHigh = "Fresh"
    override val workoutTodayPainLabel = "Anything hurting?"
    override val workoutPainKnee = "Knee"
    override val workoutPainLowerBack = "Lower back"
    override val workoutPainShoulder = "Shoulder"
    override val workoutPainWrist = "Wrist"
    override val workoutPainNone = "None"
    override val workoutTodayLocationLabel = "Where are you training?"
    override val workoutLocationHome = "Home"
    override val workoutLocationHotel = "Hotel"
    override val workoutLocationOutdoor = "Outdoor"
    override val workoutTodaySubmit = "Adapt my session"
    override val workoutTodayAdapting = "Adapting your session…"
    override val workoutTodayNoteTitle = "Coach's note"
    // region:planquota:en
    override fun planQuotaRemaining(remaining: Int, total: Int) = "$remaining/$total left"
    override val planQuotaHint = "Each regeneration uses 1 credit. Your first plan is free."
    override val planQuotaExhausted = "You've used all your regenerations. Keep going with your current plan."
    override fun planQuotaResetsInDays(days: Int) = "Your regenerations come back in $days days."
    override val planQuotaConfirmTitle = "Regenerate your plan?"
    override fun planQuotaConfirmBody(remainingAfter: Int, total: Int) =
        "This uses 1 regeneration; you'll have $remainingAfter/$total left. " +
            "Your current plan moves to plan history. If the new plan fails, you keep the credit."
    override val planQuotaConfirmAction = "Regenerate"
    override val planQuotaErrorExhausted = "You have no regenerations left."
    override val planQuotaErrorInProgress = "Your plan is already being built. Try again in a few seconds."
    override val planQuotaErrorInitialLimit = "This account has used its free first plans."
    override val planGenTitle = "Building your new plan"
    override val planGenSubtitle = "Fitted to your goal, equipment and week"
    override val planGenSteps = listOf(
        "Analyzing your goal",
        "Picking exercises for your equipment",
        "Building your week",
    )
    override val planGenMuscleGroups = listOf("Chest", "Back", "Legs", "Shoulders", "Arms", "Core", "Cardio")
    override val planGenDoneTitle = "Your plan is ready"
    override val planGenDoneAction = "See plan"
    override val planGenFailedTitle = "Couldn't regenerate your plan"
    override val planGenFailedBody = "Check your connection and try again."
    override val planGenFailedKeptRight = "No regeneration was used."
    // region:trial:en
    override val trialUsedOnDeviceTitle = "Free trial already used on this device"
    override val trialUsedOnDeviceBody =
        "The 7-day trial can be used once per device. Go Pro to unlock every feature right away."
    override val trialUsedByAccountTitle = "This account's trial has ended"
    override val trialUsedByAccountBody =
        "The 7-day trial can be used once per account. Go Pro to pick up where you left off."
    override fun trialActiveTitle(days: Int) = if (days == 1) "Your trial is on: 1 day left" else "Your trial is on: $days days left"
    override val trialActiveBody = "Subscribe now and Pro carries on without a break when the trial ends."
    override fun trialStartedMessage(days: Int) = "Your $days-day trial has started"
    override val trialRequiresAccount = "Sign in to your account to start the trial."
    override val trialStartFailed = "The trial couldn't start right now. Check your connection and try again."
    override val planSetupTrialUsedOnDevice =
        "This device already used the free trial, so you're starting with the starter plan."
    override val planSetupTrialUsedByAccount =
        "This account already used its free trial, so you're starting with the starter plan."
    override val planSetupTrialPending = "The trial couldn't start right now; it will start once you're back online."
    override fun settingsPlanTrial(days: Int) = if (days == 1) "Pro trial · 1 day left" else "Pro trial · $days days left"
    override val settingsPlanTrialEnded = "Free plan · trial ended"
    override fun trialDebugLine(state: String, days: Int, keyPrefix: String, source: String, backend: String) =
        "Trial (debug): $state · $days d · device $keyPrefix ($source, $backend)"
    // region:onboarding:en
    override val onboardingSkip = "Skip"
    override val onboardingAttributionTitle = "How did you hear about us?"
    override val onboardingAttributionSubtitle = "Pick one option or skip this step."
    override fun attributionLabel(code: String) = when (code) {
        com.fitai.app.preferences.AttributionSources.TIKTOK -> "TikTok"
        com.fitai.app.preferences.AttributionSources.INSTAGRAM -> "Instagram"
        com.fitai.app.preferences.AttributionSources.YOUTUBE -> "YouTube"
        com.fitai.app.preferences.AttributionSources.FRIEND -> "A friend"
        com.fitai.app.preferences.AttributionSources.GYM -> "My gym"
        com.fitai.app.preferences.AttributionSources.SEARCH -> "Google or app store search"
        else -> "Other"
    }
    override fun onboardingPreviewHeadline(days: Int, minutes: Int, equipment: String) =
        "Your plan is ready: $days days, $minutes min, $equipment"
    override val onboardingPreviewEquipmentNone = "no equipment"
    override val onboardingPreviewEquipmentHome = "home equipment"
    override val onboardingPreviewEquipmentGym = "gym equipment"
    override val onboardingPreviewFallbackTitle = "Your plan is ready"
    override val onboardingPreviewSubtitle = "Your first week of training and your daily nutrition target."
    override val onboardingPreviewWeekTitle = "Your first week"
    override val onboardingPreviewMacroTitle = "Daily target"
    override fun onboardingPreviewKcal(kcal: Int) = "$kcal kcal"
    override fun onboardingPreviewGrams(grams: Int) = "$grams g"
    override fun weekdayShort(dayOfWeek: Int) = when (dayOfWeek) {
        1 -> "Mon"
        2 -> "Tue"
        3 -> "Wed"
        4 -> "Thu"
        5 -> "Fri"
        6 -> "Sat"
        7 -> "Sun"
        else -> "$dayOfWeek"
    }
    override val onboardingCommitmentTitle = "Make a promise"
    override val onboardingCommitmentSubtitle = "Pick your weekly goal. People who commit stick to their plan more often."
    override fun onboardingCommitmentStatement(days: Int) = "I will train $days days a week."
    override val onboardingCommitmentDaysLabel = "Days per week"
    override val onboardingCommitmentHold = "Press and hold to commit"
    override val onboardingCommitmentHoldA11y = "Confirm commitment"
    override fun paywallTrialCta(days: Int) = "Start $days-day free trial"
    override val paywallTrustLine = "We'll remind you 24 hours before your trial ends. Cancel anytime."
    override val paywallRestore = "Restore purchases"
    override val paywallRestoreSuccess = "Your subscription was restored."
    override val paywallRestoreNotFound = "No active subscription found."
    override fun paywallSavingsBadge(percent: Int) = "Save $percent%"
    override fun paywallMonthlyEquivalent(price: String) = "Only $price"
    override fun paywallTrialDays(days: Int) = "$days-day free trial"
    override val paywallLoadError = "Couldn't load plans."
    // region:nutrition:en
    override val nutritionPlateTitle = "Plate → Plan"
    override val nutritionPlateBody = "Snap your plate. AI counts the macros and updates what's left for today."
    override val nutritionPlateCta = "Snap your plate"
    override val nutritionPlateGallery = "Choose from gallery"
    override val nutritionPlateLockedTitle = "Weekly photo analyses used up"
    override val nutritionAnalysisFailed = "Couldn't analyze the photo."
    override val nutritionReviewTitle = "Analysis result"
    override val nutritionReviewHint = "You can adjust the values before saving."
    override val nutritionRemainingTitle = "Remaining macros"
    override val nutritionTrainingDayTarget = "Training day target"
    override val nutritionRestDayTarget = "Rest day target"
    override fun nutritionKcalLeft(kcal: Int) = "$kcal kcal left"
    override fun nutritionKcalProgress(consumed: Int, target: Int) = "$consumed / $target kcal"
    override fun nutritionGramsProgress(consumed: Int, target: Int) = "$consumed / $target g"
    override fun nutritionMealMacros(proteinG: Int, carbsG: Int, fatG: Int) =
        "P $proteinG g · C $carbsG g · F $fatG g"
    override val nutritionNextMealTitle = "Next meal"
    override val nutritionNextMealLoading = "Planning your next meal…"
    override val nutritionNextMealError = "Couldn't get a suggestion."
    override val nutritionNextMealLockedBody = "Let AI plan your next meal around your remaining macros. Pro only."
    override fun nutritionNextMealMacros(calories: Int, proteinG: Int, carbsG: Int, fatG: Int) =
        "$calories kcal · P $proteinG g · C $carbsG g · F $fatG g"
    // region:coach:en
    override val coachTitle = "AI Coach"
    override val coachEntryTeaser = "Ask a coach who knows your logs. See your week."
    override val coachWeekTitle = "This week"
    override val coachStatSessions = "Sessions"
    override val coachStatVolume = "Volume"
    override val coachStatProtein = "Protein"
    override val coachStatWeight = "Weight"
    override val coachStatPrs = "PRs"
    override fun coachStatSessionsValue(done: Int, planned: Int) = if (planned > 0) "$done/$planned" else "$done"
    override fun coachStatVolumeValue(kg: Int, changePercent: Int?): String {
        val change = changePercent?.let { if (it >= 0) " · +$it%" else " · $it%" }.orEmpty()
        return "$kg kg$change"
    }
    override fun coachStatProteinValue(avg: Int, target: Int) = if (target > 0) "$avg/$target g" else "$avg g"
    override fun coachStatWeightValue(change: String?) = change?.let { "$it kg" } ?: "—"
    override val coachHeadlineNoActivity = "A fresh week. Start with your first session."
    override fun coachHeadlinePlanCompleted(sessions: Int) = "Great! All $sessions planned sessions done."
    override fun coachHeadlineVolumeUp(percent: Int) = "Your volume is up $percent% from last week."
    override fun coachHeadlineProteinLow(avg: Int, target: Int) = "Protein averages $avg g against a $target g target."
    override fun coachHeadlineInProgress(done: Int, planned: Int) =
        if (planned > 0) "$done/$planned sessions done this week. Keep going!" else "$done sessions done this week."
    override val coachReportHighlights = "Highlights"
    override val coachReportNextWeek = "Next week"
    override val coachReportLockedTitle = "Full weekly report is Pro"
    override val coachReportLockedBody = "Let your coach review your week and set clear steps for the next one."
    override val coachReportFailed = "The AI report isn't available right now."
    override val coachReportLoadError = "Couldn't load your weekly summary."
    override val coachChatTitle = "Ask your coach"
    override val coachChatEmpty = "I know your workouts and meals. Ask me anything."
    override val coachPromptKnee = "My knee hurts. What can I do instead of squats?"
    override val coachPromptProtein = "How can I get more protein this week?"
    override val coachPromptHarder = "How do I make my plan harder?"
    override val coachInputPlaceholder = "Message your coach…"
    override val coachSendA11y = "Send message"
    override val coachTyping = "Coach is typing…"
    override val coachDisclaimer = "The AI coach is not medical advice. If pain persists, see a professional."
    override val coachDailyLimit = "You've reached today's message limit. Try again tomorrow."
    override fun coachMessageTooLong(max: Int) = "Messages can be up to $max characters."
    override val coachSendFailed = "Message couldn't be sent."
    override val coachChatLockedTitle = "AI coach chat is Pro"
    override val coachChatLockedBody = "Ask a coach who knows your logs, any time."
    override val coachClearHistory = "Clear chat"
    override fun coachCharCount(count: Int, max: Int) = "$count/$max"
    // region:engagement:en
    override fun engagementStreakDays(days: Int) = if (days == 1) "1 day" else "$days days"
    override val engagementStreakLabel = "Streak"
    override val engagementFreezeAvailable = "Weekly freeze ready"
    override val engagementFreezeUsed = "This week's freeze is used"
    override val engagementTodayActive = "Today done"
    override fun engagementLevel(level: Int) = "Level $level"
    override fun engagementWeeklyXp(xp: Int) = "$xp XP this week"
    override fun engagementXpToNext(xp: Int) = "$xp XP to next level"
    override val engagementChecklistTitle = "Your first steps"
    override fun engagementChecklistProgress(done: Int, total: Int) = "$done/$total"
    override val engagementChecklistSession = "Finish your first workout"
    override val engagementChecklistMealPhoto = "Snap your first meal photo"
    override val engagementChecklistWeighIn = "Log your first weigh-in"
    override fun homeTodayExerciseCount(count: Int) = if (count == 1) "1 exercise" else "$count exercises"
    override fun homeMacrosLeft(proteinG: Int, carbsG: Int, fatG: Int) =
        "P $proteinG g · C $carbsG g · F $fatG g left"
    override val homeNoDietPlan = "Your targets appear here once your diet plan is ready"
    override val homeWeekTitle = "This week"
    override fun homeVolumeTonnes(tonnes: String) = "$tonnes t"
    override fun homeVolumeChange(percent: Int) =
        if (percent >= 0) "+$percent% vs last week" else "−${-percent}% vs last week"
    override val progressWeightTitle = "Weight trend"
    override val progressWeightEmpty = "Add a weigh-in to see your trend"
    override fun progressWeightLatest(weight: String) = "$weight kg"
    override fun progressWeightChange(change: String) = "$change kg in 90 days"
    override val progressAddWeighIn = "Add weigh-in"
    override val progressWeightLabel = "Weight (kg)"
    override val progressWaistLabel = "Waist (cm, optional)"
    override val progressWeightInvalid = "Enter a value between 20 and 400 kg"
    override val progressWaistInvalid = "Enter a value between 30 and 300 cm"
    override val progressWeighInSaved = "Weigh-in saved"
    override val progressSaveFailed = "Couldn't save, try again"
    override val progressLoadError = "Couldn't load progress"
    override val progressVolumeTitle = "Weekly volume"
    override val progressVolumeEmpty = "Your volume shows up as you finish sessions"
    override fun progressWeekShort(day: Int, month: Int) = "$month/$day"
    override val progressPrTitle = "Personal records"
    override val progressPrEmpty = "Your records show up as you finish sessions"
    override fun progressPrValue(weight: String, reps: Int) = "$weight kg × $reps"
    override fun progressE1rm(value: String) = "e1RM $value kg"
    override val progressBadgesTitle = "Badges"
    override fun progressBadgesCount(unlocked: Int, total: Int) = "$unlocked/$total"
    override val badgeFirstSession = "First session"
    override val badgeFirstMealPhoto = "First plate"
    override val badgeFirstWeighIn = "First weigh-in"
    override val badgeFirstPr = "First PR"
    override val badgeStreak7 = "7-day streak"
    override val badgeStreak28 = "4-week streak"
    override val badgeSessions10 = "10 sessions"
    override val badgeMeals100 = "100 meals"
    override val badgeVolume10t = "10 tonnes"
    // region:notifications:en
    override val notificationsPermissionTitle = "Want a reminder at workout time?"
    override val notificationsPermissionSubtitle = "On training days I'll send a short nudge at the time you pick."
    override val notificationsHourUnit = "h"
    override val notificationsMinuteUnit = "min"
    override fun notificationsTimeSummary(time: String) = "Training days at $time"
    override val notificationsRemind = "Remind me"
    override val notificationsNotNow = "Not now"
    override val notificationsProfileSection = "Reminders"
    override val notificationsProfileToggle = "Workout and meal reminders"
    override val notificationsProfileTime = "Reminder time"
    override val notificationsPermissionDenied = "Notifications are off. You can turn them on in device settings."
    override val notificationsUpdateFailed = "Couldn't update reminders"
    override val notificationsChannelWorkout = "Workout reminders"
    override val notificationsChannelMeals = "Meal reminders"
    override val notificationsChannelProgram = "Program and subscription"
    override val notificationsWorkoutTitle = "Workout time"
    override val notificationsWorkoutBody = "Today's session is ready. Let's go."
    override val notificationsMealTitle = "Lunch check-in"
    override val notificationsMealBody = "Snap your plate and I'll count the macros."
    override val notificationsDay0Title = "Your first session is ready"
    override val notificationsDay0Body = "Your plan is ready. Ready to start?"
    override val notificationsDay1Title = "Snap your lunch"
    override val notificationsDay1Body = "Take a photo of your lunch and I'll suggest dinner."
    override val notificationsDay2Title = "Rest is part of the plan"
    override val notificationsDay2Body = "Planned rest days keep your streak. Keep an eye on protein."
    override val notificationsDay3Title = "Your weights are updated"
    override val notificationsDay3Body = "Suggested weights now follow your last session. Push a little more."
    override val notificationsDay5Title = "Short on time?"
    override val notificationsDay5Body = "Try a 20-minute Today Mode session."
    override val notificationsDay7Title = "Your weekly report is ready"
    override val notificationsDay7Body = "See how your first week went."
    override val notificationsTrialEndTitle = "Your trial ends tomorrow"
    override val notificationsTrialEndBody = "Pro features end in 24 hours. Pick a plan to keep them."
    // region:funnel:en
    override val languageCode = "en"
    override fun languageName(code: String) = funnelLanguageName(code)
    override val actionClose = "Close"
    override val actionYes = "Yes"
    override val actionNo = "No"
    override fun percentLabel(value: Int) = "$value%"
    override fun longDate(day: Int, month: Int, year: Int) = "${EN_MONTHS[month - 1]} $day, $year"
    override fun shortDate(day: Int, month: Int) = "${EN_MONTHS[month - 1].take(3)} $day"
    override fun dateRange(start: String, end: String) = "$start – $end"
    override val welcomeProofPlanTitle = "In 60 seconds"
    override val welcomeProofPlanBody = "a workout plan built for you"
    override val welcomeProofCoachTitle = "One AI coach"
    override val welcomeProofCoachBody = "for training and nutrition"
    override val welcomeProofProgressTitle = "Every session"
    override val welcomeProofProgressBody = "weights that follow your progress"
    override val welcomeHaveAccountPrefix = "Already have an account?"
    override val welcomeSignInLink = "Sign in"
    override val onboardingIntroTrackTitle = "Track your workouts"
    override val onboardingIntroTrackBody = "Log every set, rep and weight, and FitAI tunes your next session from it."
    override val onboardingIntroProgressTitle = "Analyze your progress"
    override val onboardingIntroProgressBody = "See volume, records and weight trends per muscle and exercise."
    override val onboardingIntroPlateTitle = "Snap your plate, AI does the rest"
    override val onboardingIntroPlateBody = "Calories and macros from a photo, with today's remaining target updated instantly."
    override val onboardingMockSet = "Set"
    override val onboardingMockPrevious = "Previous"
    override val onboardingMockKg = "KG"
    override val onboardingMockReps = "REPS"
    override val onboardingMockExercise = "Bench press"
    override val onboardingMockMaxWeight = "Max weight"
    override val onboardingMockLastMonth = "Last month"
    override fun onboardingMockWeek(week: Int) = "Wk $week"
    override val onboardingMockMealName = "Chicken quinoa bowl"
    override val onboardingObstacleTitle = "What's keeping you from that goal?"
    override fun obstacleLabel(code: String) = when (code) {
        TrainingObstacle.Time.code -> "Not enough time"
        TrainingObstacle.Knowledge.code -> "Not knowing what to do"
        TrainingObstacle.Motivation.code -> "Low motivation"
        TrainingObstacle.Injury.code -> "Injury or pain"
        TrainingObstacle.Equipment.code -> "Lack of equipment"
        else -> "I've never really tried"
    }
    override val onboardingFocusTitle = "What do you want FitAI for?"
    override val onboardingFocusSubtitle = "Pick up to 3."
    override fun focusLabel(code: String) = when (code) {
        FocusArea.PersonalPlan.code -> "Get a personal workout plan"
        FocusArea.SmartOverload.code -> "Smart weight suggestions"
        FocusArea.PlatePhoto.code -> "Track macros from plate photos"
        FocusArea.LogLifts.code -> "Log my sets and weights"
        FocusArea.Streak.code -> "Stay consistent with streaks"
        else -> "Ask an AI coach"
    }
    override fun focusDescription(code: String) = when (code) {
        FocusArea.PersonalPlan.code -> "Built by AI around your equipment, level and goal."
        FocusArea.SmartOverload.code -> "Tells you how much to add on every lift so you never plateau."
        FocusArea.PlatePhoto.code -> "Calories and macros from a single photo."
        FocusArea.LogLifts.code -> "Past sessions and records in one place."
        FocusArea.Streak.code -> "Planned rest days never break your streak."
        else -> "Instant answers from a coach that knows your logs."
    }
    override val onboardingExperienceTitle = "How experienced are you with training?"
    override fun experienceLabel(code: String) = when (code) {
        TrainingExperience.None.code -> "I've never trained"
        TrainingExperience.Beginner.code -> "Beginner · Tried it before"
        TrainingExperience.Intermediate.code -> "Intermediate · Train regularly"
        else -> "Advanced · Years of training"
    }
    override val onboardingGenderTitle = "What's your sex?"
    override val onboardingGenderSubtitle = "Used to calculate your calorie and macro targets."
    override fun genderLabel(code: String) = if (code == Gender.Female.code) "Female" else "Male"
    override val onboardingWeightPrivacy = "Your data stays private and is never shared."
    override val onboardingWeightUsage = "Your weight sets your calorie target and suggested loads."
    override val onboardingStatementTitle = "Do you agree with this?"
    override val onboardingStatementConsistency = "“I've never managed to train consistently without skipping sessions.”"
    override val onboardingStatementNutrition = "“My diets always fell apart because I didn't know what to eat.”"
    override val onboardingStoryDataTitle = "You think you train hard, but you have no data to prove it"
    override val onboardingStoryDataBody =
        "You don't know whether you lift more than a month ago. You train on feel, and feelings lie."
    override val onboardingStorySystemTitle = "It's not your fault. You need a system that works."
    override val onboardingStorySystemBody =
        "People who get results aren't different from you. They just have a plan that tells them what to do and a coach that tracks what they eat."
    override val onboardingStoryPlanTitle = "Stop copying programs. Get one that's yours."
    override val onboardingStoryPlanBody =
        "FitAI analyzes your profile and builds training and nutrition around your body, equipment and goal."
    override val onboardingMockPlanTitle = "Back & Biceps"
    override val onboardingMockPlanExercises = listOf("Lat pulldown", "Barbell row", "Seated cable row", "Hammer curl")
    override fun onboardingMockPlanMore(count: Int) = "+$count more"
    override val onboardingMockToday = "Today"
    override val onboardingDaysTitle = "How many days a week do you want to train?"
    override fun onboardingDaysLabel(days: Int) = "$days days a week"
    override fun onboardingDaysTag(days: Int) = when (days) {
        2 -> "Solid start"
        3 -> "Consistent"
        4 -> "Committed"
        5 -> "Incredible"
        else -> "No excuses"
    }
    override val onboardingDurationTitle = "How long should your workouts be?"
    override fun onboardingDurationLabel(minutes: Int) = if (minutes >= 90) "90+ minutes" else "$minutes minutes"
    override fun onboardingDurationTag(minutes: Int) = when {
        minutes <= 15 -> "Express"
        minutes <= 30 -> "Efficient"
        minutes <= 45 -> "Balanced"
        minutes <= 60 -> "Intense"
        else -> "All in"
    }
    override val onboardingNotifyTitle = "Turn on notifications and stay on track"
    override val onboardingNotifyBody = "A short nudge on training days, the easiest way to keep your streak."
    override val onboardingNotifyMockClock = "9:41"
    override val onboardingNotifyMockTitle = "It's training day!"
    override val onboardingNotifyMockBody = "Back & Biceps · 45 min"
    override val onboardingNotifyMockTime = "now"
    override val onboardingAnalyzingTitle = "Building your plan…"
    override val onboardingAnalyzingSteps = listOf(
        "Analyzing your profile…",
        "Tuning training intensity…",
        "Calculating nutrition targets…",
        "Finalizing your plan…",
    )
    override fun onboardingResultTitle(name: String) =
        if (name.isBlank()) "Congrats!\nYour personal plan is ready." else "Congrats $name!\nYour personal plan is ready."
    override val onboardingResultSubtitle = "With this plan you can reach your goal by:"
    override fun onboardingResultBenefits(goal: String) = when (goal) {
        FitnessGoal.LoseFat.code -> listOf(
            "You'll burn fat while keeping your muscle",
            "Your calorie target will be ready every day",
            "Your streak will keep motivation high",
        )
        FitnessGoal.BuildMuscle.code -> listOf(
            "You'll maximize muscle gain",
            "You'll know exactly how much to add each session",
            "You'll feel far more confident",
        )
        FitnessGoal.GetStronger.code -> listOf(
            "You'll break records on the main lifts",
            "You'll add weight systematically",
            "You'll feel far more confident",
        )
        FitnessGoal.ImproveEndurance.code -> listOf(
            "You'll go longer and stronger",
            "You'll raise intensity at a steady pace",
            "You'll see progress every week",
        )
        else -> listOf(
            "You'll build a steady habit of moving",
            "You'll follow balanced eating with ease",
            "You'll feel more energetic every day",
        )
    }
    override val onboardingResultChartTitle = "See what you can do"
    override val onboardingResultWithFitAI = "With FitAI"
    override val onboardingResultWithoutFitAI = "Without FitAI"
    override val onboardingResultPlateau = "Plateau"
    override val onboardingQuoteLines = listOf("Remember", "Nothing changes", "if you don't change")
    override val onboardingPledgeLead = "I promise to train with FitAI"
    override fun onboardingPledgeDays(days: Int) = "$days times a week"
    override val onboardingPledgeTagline = "No excuses, no quitting."
    override val onboardingPledgeHold = "Press and hold to sign"
    override val onboardingPledgeSigned = "Signed"
    override val paywallChooseTitle = "Choose your plan"
    override fun paywallPlanReadyFor(name: String) =
        if (name.isBlank()) "Your personal plan is ready" else "$name, your personal plan is ready"
    override fun paywallSummaryDays(days: Int) = "$days days a week"
    override val paywallIncludesAi = "Personal AI training and nutrition plan"
    override val paywallIncludesOverload = "Weight suggestions updated every session"
    override val paywallIncludesCoach = "Unlimited AI coach and plate analysis"
    override val paywallNoCommitment = "No commitment, cancel anytime"
    override val paywallExitWarning = "Close this screen and you'll lose your personal plan"
    override val paywallExitOfferTitle = "Limited-time offer"
    override fun paywallExitOfferBody(weeklyPrice: String) = "Start today for just $weeklyPrice / week"
    override val paywallExitDecline = "Continue without a personal plan"
    override val paywallExitUnlock = "Unlock my personal plan"
    override fun paywallPerWeek(price: String) = "$price / week"
    override val authSignupTitle = "Create your account to save your plan"
    override val authSignupSubtitle = "Keep your data safe and pick up where you left off on any device."
    override val authContinueEmail = "Continue with email"
    override val authNoAccount = "Don't have an account?"
    override val authSignUpLink = "Sign up"
    override val authLegal = "By continuing you agree to the Privacy Policy and Terms of Use."
    override val authSignInHeading = "Sign in"
    override val authEmailHeading = "Sign up with email"
    override val authOr = "or"
    override val authPasswordPlaceholder = "At least 6 characters"
    override val authForgotPassword = "Forgot your password?"
    override val authResetSent = "We sent a password reset link to your email."
    override val authShowPassword = "Show password"
    override val authHidePassword = "Hide password"
    override val authCreateAccount = "Create my account"
    override val authSignInCta = "Sign in"
    override val authOfflineContinue = "Continue without an account (offline mode)"
    override val authNoProfileFound = "There's no profile on this account yet. Answer a few questions to build your plan."
    override val validationPasswordShort = "Password must be at least 6 characters."
    override val validationEmailRequired = "Enter your email."
    override val planSetupPersonalTitle = "Building your personal AI plan…"
    override val planSetupStarterTitle = "Setting up your starter plan…"
    override val planSetupStepWorkout = "Building your training program"
    override val planSetupStepNutrition = "Calculating your nutrition targets"
    override val planSetupStepReminders = "Scheduling your reminders"
    override val planSetupErrorTitle = "We couldn't build your personal plan right now"
    override val planSetupErrorBody = "Retry, or continue with a starter plan for now."
    override val planSetupUseStarter = "Continue with a starter plan"
    override val challengeTitle = "Starter Goals"
    override val challengeTaskNotifications = "Turn on notifications"
    override val challengeGift = "Open your gift"
    override val challengeGiftLocked = "Finish every goal to unlock your gift"
    override val challengeAllDone = "All goals done! Your gift is ready"
    override val challengeHide = "Don't show again"
    override val challengeOpenA11y = "Open starter goals"
    override val streakTitle = "Workout streak"
    override fun streakQuote(days: Int) = when {
        days <= 0 -> "Take the first step today.\nYour streak starts with you."
        days == 1 -> "“You're not starting a streak.”\nYou're becoming someone new."
        days < 7 -> "Every session makes the next one easier.\nKeep going!"
        days < 28 -> "A full week down.\nThis is your routine now."
        else -> "A month without quitting.\nThe results will show."
    }
    override val streakHowItWorks = "How do streaks work?"
    override val streakHowItWorksBody =
        "Your streak grows when you finish a session on a planned training day. Planned rest days never break it. " +
            "Miss a training day and your weekly freeze keeps the streak alive."
    override fun streakLongest(days: Int) = "Longest streak: $days days"
    override fun streakA11y(days: Int) = "Workout streak: $days days"
    override val profileStatWorkouts = "Workouts"
    override val profileStatStreak = "Streak"
    override val profileStatRecords = "Records"
    override val profileMeasurements = "Measurements"
    override val profileTabWorkouts = "Workouts"
    override val profileTabCharts = "Charts"
    override val profileNoWorkouts = "No workouts yet"
    override val profileNoWorkoutsBody = "Your first finished session will show up here."
    override val profileChartEmptyTitle = "No data yet"
    override val profileChartEmptyBody = "You'll see your progress over time here"
    override val profileLogWorkout = "Log a workout"
    override val profileGuestName = "Athlete"
    override val profilePeriod = "Period"
    override fun trendMetricLabel(code: String) = when (code) {
        "Duration" -> "Duration"
        "Sessions" -> "Sessions"
        else -> "Volume"
    }
    override fun trendPeriodLabel(weeks: Int) = if (weeks >= 26) "6 months" else "$weeks weeks"
    override fun historyMeta(minutes: Int, volume: String, sets: Int) = "$minutes min · $volume · $sets sets"
    override val settingsProfileSection = "Profile details"
    override val settingsSubscription = "Subscription"
    override val settingsPlanPro = "FitAI Pro is active"
    override val settingsPlanFree = "Free plan"
    override val settingsLanguage = "Language"
    override fun settingsSignedInAs(email: String) = "Signed in as $email"
    override val settingsGuestAccount = "You're not connected to an account yet"
    override val measurementWaist = "Waist"
    override val measurementSameAsStart = "Same as when you started"
    override fun measurementChange(signed: String, unit: String) = "$signed $unit since you started"
    override val measurementEntries = "Entries"
    override val measurementEmpty = "Your chart builds up as you add measurements"
    override fun measurementPeriodLabel(months: Int) = if (months >= 12) "1 year" else "$months months"
    override fun cmValue(value: String) = "$value cm"
}
