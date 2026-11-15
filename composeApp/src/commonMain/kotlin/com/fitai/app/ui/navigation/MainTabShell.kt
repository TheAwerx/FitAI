package com.fitai.app.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import com.fitai.app.localization.LocalStrings
import com.fitai.app.navigation.MainTab
import com.fitai.app.ui.screens.challenge.StarterChallengeHost
import com.fitai.app.ui.screens.coach.CoachEntryCard
import com.fitai.app.ui.screens.coach.CoachScreen
import com.fitai.app.ui.screens.home.HomeScreen
import com.fitai.app.ui.screens.nutrition.MealLogScreen
import com.fitai.app.ui.screens.nutrition.NutritionScreen
import com.fitai.app.ui.screens.profile.MeasurementsScreen
import com.fitai.app.ui.screens.profile.ProfileScreen
import com.fitai.app.ui.screens.profile.SettingsScreen
import com.fitai.app.ui.screens.progress.ProgressScreen
import com.fitai.app.ui.screens.streak.StreakScreen
import com.fitai.app.ui.screens.workout.ActiveSessionScreen
import com.fitai.app.ui.screens.workout.ExerciseDetailScreen
import com.fitai.app.ui.screens.workout.SessionSummaryScreen
import com.fitai.app.ui.screens.workout.TodayModeScreen
import com.fitai.app.ui.screens.workout.WorkoutScreen
import com.fitai.app.ui.theme.FitAIColors
import com.fitai.app.ui.theme.FitAIIcons

@Composable
fun MainTabShell(
    onNavigateAuth: (signIn: Boolean) -> Unit,
    onNavigatePaywall: () -> Unit,
    onSignedOut: () -> Unit,
    onLanguageChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var selectedTab by remember { mutableStateOf(MainTab.Home) }
    var workoutStack by remember { mutableStateOf<WorkoutStack>(WorkoutStack.List) }
    var profileStack by remember { mutableStateOf(ProfileStack.Hub) }
    var showMealLog by remember { mutableStateOf(false) }
    var showCoach by remember { mutableStateOf(false) }
    var showStreak by remember { mutableStateOf(false) }
    var openWeighIn by remember { mutableStateOf(false) }
    var challengeRefresh by remember { mutableIntStateOf(0) }
    val strings = LocalStrings.current

    val hasNestedScreen = showCoach || showMealLog || showStreak ||
        workoutStack != WorkoutStack.List || profileStack != ProfileStack.Hub
    NavigationBackHandler(
        state = rememberNavigationEventState(currentInfo = NavigationEventInfo.None),
        isBackEnabled = hasNestedScreen || selectedTab != MainTab.Home,
        onBackCompleted = {
            when {
                selectedTab == MainTab.Home && showStreak -> showStreak = false
                selectedTab == MainTab.Home && showCoach -> showCoach = false
                selectedTab == MainTab.Workout && workoutStack != WorkoutStack.List ->
                    workoutStack = WorkoutStack.List
                selectedTab == MainTab.Nutrition && showMealLog -> showMealLog = false
                selectedTab == MainTab.Profile && profileStack != ProfileStack.Hub -> profileStack = ProfileStack.Hub
                else -> selectedTab = MainTab.Home
            }
        },
    )

    fun openTab(tab: MainTab) {
        selectedTab = tab
        challengeRefresh++
    }

    val fullScreenFlow = showStreak || showCoach ||
        workoutStack is WorkoutStack.ActiveSession || workoutStack is WorkoutStack.Streak ||
        workoutStack is WorkoutStack.SessionSummary || workoutStack == WorkoutStack.TodayMode ||
        profileStack != ProfileStack.Hub || showMealLog

    Scaffold(
        modifier = modifier,
        containerColor = FitAIColors.Background,
        bottomBar = {
            Column {
                StarterChallengeHost(
                    refreshKey = challengeRefresh to selectedTab,
                    hidden = fullScreenFlow,
                    onOpenWorkout = {
                        workoutStack = WorkoutStack.List
                        openTab(MainTab.Workout)
                    },
                    onOpenMealPhoto = {
                        showMealLog = false
                        openTab(MainTab.Nutrition)
                    },
                    onOpenWeighIn = {
                        openWeighIn = true
                        openTab(MainTab.Progress)
                    },
                    onOpenGift = onNavigatePaywall,
                )
                NavigationBar(containerColor = FitAIColors.Surface) {
                    MainTab.entries.forEach { tab ->
                        val selected = selectedTab == tab
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                openTab(tab)
                                if (tab == MainTab.Home) {
                                    showCoach = false
                                    showStreak = false
                                }
                                if (tab == MainTab.Workout) workoutStack = WorkoutStack.List
                                if (tab == MainTab.Nutrition) showMealLog = false
                                if (tab == MainTab.Profile) profileStack = ProfileStack.Hub
                            },
                            icon = { Icon(tabIcon(tab), contentDescription = null) },
                            label = {
                                Text(
                                    text = when (tab) {
                                        MainTab.Home -> strings.tabHome
                                        MainTab.Workout -> strings.tabWorkout
                                        MainTab.Nutrition -> strings.tabNutrition
                                        MainTab.Progress -> strings.tabProgress
                                        MainTab.Profile -> strings.tabProfile
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = FitAIColors.Primary,
                                selectedTextColor = FitAIColors.OnSurface,
                                indicatorColor = FitAIColors.Primary.copy(alpha = 0.15f),
                                unselectedIconColor = FitAIColors.OnSurfaceMuted,
                                unselectedTextColor = FitAIColors.OnSurfaceMuted,
                            ),
                        )
                    }
                }
            }
        },
    ) { padding ->
        Box(modifier = Modifier.padding(padding).consumeWindowInsets(padding)) {
            when (selectedTab) {
                MainTab.Home -> when {
                    showStreak -> StreakScreen(onContinue = { showStreak = false })
                    showCoach -> CoachScreen(onBack = { showCoach = false }, onNavigatePaywall = onNavigatePaywall)
                    else -> HomeScreen(
                        onNavigateMealLog = {
                            selectedTab = MainTab.Nutrition
                            showMealLog = true
                        },
                        onNavigatePaywall = onNavigatePaywall,
                        onOpenStreak = { showStreak = true },
                        coachEntry = { CoachEntryCard(onClick = { showCoach = true }) },
                    )
                }
                MainTab.Workout -> when (val stack = workoutStack) {
                    WorkoutStack.List -> WorkoutScreen(
                        onNavigateExercise = { id ->
                            workoutStack = WorkoutStack.ExerciseDetail(id)
                        },
                        onNavigateActiveSession = { sessionId ->
                            workoutStack = WorkoutStack.ActiveSession(sessionId)
                        },
                        onNavigatePaywall = onNavigatePaywall,
                        onNavigateTodayMode = { workoutStack = WorkoutStack.TodayMode },
                    )
                    WorkoutStack.TodayMode -> TodayModeScreen(
                        onBack = { workoutStack = WorkoutStack.List },
                        onNavigateActiveSession = { sessionId ->
                            workoutStack = WorkoutStack.ActiveSession(sessionId)
                        },
                        onNavigatePaywall = onNavigatePaywall,
                    )
                    is WorkoutStack.ExerciseDetail -> ExerciseDetailScreen(
                        exerciseId = stack.exerciseId,
                        onBack = { workoutStack = WorkoutStack.List },
                        onStartSession = { sessionId ->
                            workoutStack = WorkoutStack.ActiveSession(sessionId)
                        },
                    )
                    is WorkoutStack.ActiveSession -> ActiveSessionScreen(
                        sessionId = stack.sessionId,
                        onBack = { workoutStack = WorkoutStack.List },
                        onCompleted = { sessionId -> workoutStack = WorkoutStack.Streak(sessionId) },
                    )
                    is WorkoutStack.Streak -> StreakScreen(
                        onContinue = { workoutStack = WorkoutStack.SessionSummary(stack.sessionId) },
                    )
                    is WorkoutStack.SessionSummary -> SessionSummaryScreen(
                        sessionId = stack.sessionId,
                        onDone = {
                            workoutStack = WorkoutStack.List
                            challengeRefresh++
                        },
                    )
                }
                MainTab.Nutrition -> if (showMealLog) {
                    MealLogScreen(onBack = { showMealLog = false }, onSaved = { showMealLog = false })
                } else {
                    NutritionScreen(
                        onNavigateMealLog = { showMealLog = true },
                        onNavigatePaywall = onNavigatePaywall,
                    )
                }
                MainTab.Progress -> ProgressScreen(
                    openWeighIn = openWeighIn,
                    onWeighInOpened = { openWeighIn = false },
                )
                MainTab.Profile -> when (profileStack) {
                    ProfileStack.Hub -> ProfileScreen(
                        onOpenSettings = { profileStack = ProfileStack.Settings },
                        onOpenMeasurements = { profileStack = ProfileStack.Measurements },
                        onLogWorkout = {
                            workoutStack = WorkoutStack.List
                            openTab(MainTab.Workout)
                        },
                    )
                    ProfileStack.Settings -> SettingsScreen(
                        onBack = { profileStack = ProfileStack.Hub },
                        onNavigateSignIn = { onNavigateAuth(true) },
                        onNavigatePaywall = onNavigatePaywall,
                        onSignedOut = onSignedOut,
                        onLanguageChange = onLanguageChange,
                    )
                    ProfileStack.Measurements -> MeasurementsScreen(onBack = { profileStack = ProfileStack.Hub })
                }
            }
        }
    }
}

private sealed interface WorkoutStack {
    data object List : WorkoutStack
    data class ExerciseDetail(val exerciseId: String) : WorkoutStack
    data class ActiveSession(val sessionId: String) : WorkoutStack
    data object TodayMode : WorkoutStack
    data class Streak(val sessionId: String) : WorkoutStack
    data class SessionSummary(val sessionId: String) : WorkoutStack
}

private enum class ProfileStack { Hub, Settings, Measurements }

private fun tabIcon(tab: MainTab): ImageVector = when (tab) {
    MainTab.Home -> FitAIIcons.Home
    MainTab.Workout -> FitAIIcons.FitnessCenter
    MainTab.Nutrition -> FitAIIcons.Restaurant
    MainTab.Progress -> FitAIIcons.ShowChart
    MainTab.Profile -> FitAIIcons.Person
}
