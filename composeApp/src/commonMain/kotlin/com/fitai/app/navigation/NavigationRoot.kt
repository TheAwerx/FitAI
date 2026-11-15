package com.fitai.app.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.fitai.app.auth.SignInOutcome
import com.fitai.app.data.AppPreferences
import com.fitai.app.data.FunnelStage
import com.fitai.app.ui.navigation.MainTabShell
import com.fitai.app.ui.screens.auth.AuthScreen
import com.fitai.app.ui.screens.notifications.RemindersLaunchSync
import com.fitai.app.ui.screens.onboarding.OnboardingScreen
import com.fitai.app.ui.screens.paywall.PaywallScreen
import com.fitai.app.ui.screens.plansetup.PlanSetupScreen
import com.fitai.app.ui.screens.splash.SplashScreen
import com.fitai.app.ui.screens.welcome.WelcomeScreen
import org.koin.compose.koinInject

/**
 * First run: Welcome → Onboarding → Paywall → mandatory sign-up → plan setup → Main.
 * [AppPreferences.funnelStage] lets a killed app resume at the same funnel step.
 */
@Composable
fun NavigationRoot(
    onLanguageChange: (String) -> Unit,
    preferences: AppPreferences = koinInject(),
    navController: NavHostController = rememberNavController(),
) {
    val startDestination: Any = when {
        preferences.requiresLogin -> AuthRoute(signIn = true)
        preferences.onboardingCompleted -> MainTabRoute
        else -> SplashRoute
    }

    RemindersLaunchSync(enabled = preferences.onboardingCompleted && !preferences.requiresLogin)

    NavHost(
        navController = navController,
        startDestination = startDestination,
    ) {
        composable<SplashRoute> {
            SplashScreen(
                onContinue = {
                    val next: Any = when {
                        preferences.requiresLogin -> AuthRoute(signIn = true)
                        preferences.onboardingCompleted -> MainTabRoute
                        preferences.funnelStage == FunnelStage.Paywall -> PaywallRoute(fromOnboarding = true)
                        preferences.funnelStage == FunnelStage.Signup -> AuthRoute(funnel = true)
                        preferences.funnelStage == FunnelStage.PlanSetup -> PlanSetupRoute
                        preferences.welcomeSeen -> OnboardingRoute
                        else -> WelcomeRoute
                    }
                    navController.replaceAll(next)
                },
            )
        }
        composable<WelcomeRoute> {
            WelcomeScreen(
                onContinue = {
                    preferences.welcomeSeen = true
                    navController.replaceAll(OnboardingRoute)
                },
                onHaveAccount = {
                    navController.navigate(AuthRoute(signIn = true)) { launchSingleTop = true }
                },
                onLanguageChange = onLanguageChange,
            )
        }
        composable<OnboardingRoute> {
            OnboardingScreen(
                onNavigatePaywall = {
                    preferences.funnelStage = FunnelStage.Paywall
                    navController.replaceAll(PaywallRoute(fromOnboarding = true))
                },
                onBackToWelcome = { navController.replaceAll(WelcomeRoute) },
            )
        }
        composable<PaywallRoute> { entry ->
            val route = entry.toRoute<PaywallRoute>()
            val toSignup = {
                preferences.funnelStage = FunnelStage.Signup
                navController.replaceAll(AuthRoute(funnel = true))
            }
            PaywallScreen(
                fromOnboarding = route.fromOnboarding,
                onUnlocked = { if (route.fromOnboarding) toSignup() else navController.leaveOrHome() },
                onDeclined = toSignup,
                onClose = { navController.leaveOrHome() },
            )
        }
        composable<AuthRoute> { entry ->
            val route = entry.toRoute<AuthRoute>()
            AuthScreen(
                signIn = route.signIn,
                funnel = route.funnel,
                onBack = { navController.navigateUp() },
                onCompleted = { outcome ->
                    preferences.requiresLogin = false
                    when {
                        outcome == SignInOutcome.NoProfile && !route.funnel -> {
                            preferences.welcomeSeen = true
                            preferences.onboardingCompleted = false
                            navController.replaceAll(OnboardingRoute)
                        }
                        outcome == SignInOutcome.RestoredFromCloud || !preferences.onboardingCompleted -> {
                            preferences.funnelStage = FunnelStage.PlanSetup
                            navController.replaceAll(PlanSetupRoute)
                        }
                        navController.previousBackStackEntry != null -> navController.navigateUp()
                        else -> navController.replaceAll(MainTabRoute)
                    }
                },
                onContinueOffline = {
                    preferences.funnelStage = FunnelStage.PlanSetup
                    navController.replaceAll(PlanSetupRoute)
                },
            )
        }
        composable<PlanSetupRoute> {
            PlanSetupScreen(
                onDone = {
                    preferences.onboardingCompleted = true
                    preferences.funnelStage = null
                    navController.replaceAll(MainTabRoute)
                },
            )
        }
        composable<MainTabRoute> {
            MainTabShell(
                onNavigateAuth = { signIn ->
                    navController.navigate(AuthRoute(signIn = signIn)) { launchSingleTop = true }
                },
                onNavigatePaywall = {
                    navController.navigate(PaywallRoute()) { launchSingleTop = true }
                },
                onSignedOut = { navController.replaceAll(AuthRoute(signIn = true)) },
                onLanguageChange = onLanguageChange,
            )
        }
    }
}

/** Clears the back stack so funnel screens can't be revisited with system back. */
private fun NavHostController.replaceAll(route: Any) {
    navigate(route) {
        launchSingleTop = true
        popUpTo(graph.id) { inclusive = true }
    }
}

private fun NavHostController.leaveOrHome() {
    if (previousBackStackEntry != null) popBackStack() else replaceAll(MainTabRoute)
}
