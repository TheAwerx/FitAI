package com.fitai.app.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.fitai.app.ui.screens.home.HomeScreen
import com.fitai.app.ui.screens.splash.SplashScreen

@Composable
fun AppNavHost() {
    val navController = rememberNavController()
    NavHost(
        navController = navController,
        startDestination = SplashRoute,
    ) {
        composable<SplashRoute> {
            SplashScreen(
                onContinue = {
                    navController.navigate(HomeRoute) {
                        launchSingleTop = true
                        popUpTo<SplashRoute> { inclusive = true }
                    }
                },
            )
        }
        composable<HomeRoute> {
            HomeScreen()
        }
    }
}
