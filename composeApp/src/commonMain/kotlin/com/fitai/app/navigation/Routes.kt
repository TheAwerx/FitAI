package com.fitai.app.navigation

import kotlinx.serialization.Serializable

@Serializable
data object SplashRoute

@Serializable
data object WelcomeRoute

@Serializable
data object OnboardingRoute

@Serializable
data object MainTabRoute

@Serializable
data object HomeRoute

@Serializable
data object WorkoutRoute

@Serializable
data object NutritionRoute

@Serializable
data object ProgressRoute

@Serializable
data object ProfileRoute

@Serializable
data class ExerciseDetailRoute(val exerciseId: String)

@Serializable
data class ActiveSessionRoute(val sessionId: String)

@Serializable
data object MealLogRoute

@Serializable
data class PaywallRoute(
    val fromOnboarding: Boolean = false,
)

/** [funnel] is the mandatory sign-up after the paywall; [signIn] opens the sign-in form first. */
@Serializable
data class AuthRoute(
    val signIn: Boolean = false,
    val funnel: Boolean = false,
)

@Serializable
data object PlanSetupRoute

enum class MainTab(val route: Any) {
    Home(HomeRoute),
    Workout(WorkoutRoute),
    Nutrition(NutritionRoute),
    Progress(ProgressRoute),
    Profile(ProfileRoute),
}
