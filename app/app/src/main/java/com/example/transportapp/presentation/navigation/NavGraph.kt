package com.example.transportapp.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.transportapp.data.session.SessionManager
import com.example.transportapp.presentation.history.RideHistoryScreen
import com.example.transportapp.presentation.home.HomeScreen
import com.example.transportapp.presentation.login.LoginScreen
import com.example.transportapp.presentation.policy.DriverPolicyScreen
import com.example.transportapp.presentation.profile.ProfileScreen
import com.example.transportapp.presentation.driver.DriverScreen

sealed class Screen(val route: String) {
    data object Login          : Screen("login")
    data object Home           : Screen("home")
    data object RideHistory    : Screen("history")
    data object Profile        : Screen("profile")
    data object DriverDashboard: Screen("driver_dashboard")
    data object DriverPolicy   : Screen("driver_policy")
}

@Composable
fun SetupNavGraph(
    navController: NavHostController,
    startRoute: String,
    sessionManager: SessionManager
) {
    NavHost(navController = navController, startDestination = startRoute) {

        composable(Screen.Login.route) {
            LoginScreen(
                onLoginSuccess = {
                    sessionManager.refreshActivity()
                    sessionManager.saveDriverMode(false)
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                },
                onDriverLoginSuccess = {
                    sessionManager.refreshActivity()
                    sessionManager.saveDriverMode(true)
                    navController.navigate(Screen.DriverDashboard.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Home.route) {
            HomeScreen(
                onNavigateToHistory = { navController.navigate(Screen.RideHistory.route) },
                onNavigateToProfile = { navController.navigate(Screen.Profile.route) },
                onNavigateToDriverDashboard = {
                    // Show policy screen first time driver mode is enabled from Home
                    sessionManager.saveDriverMode(true)
                    if (sessionManager.isPolicyPending()) {
                        navController.navigate(Screen.DriverPolicy.route)
                    } else {
                        navController.navigate(Screen.DriverDashboard.route) {
                            popUpTo(Screen.Home.route) { inclusive = true }
                        }
                    }
                },
                sessionManager = sessionManager
            )
        }

        composable(Screen.RideHistory.route) {
            RideHistoryScreen(onBack = { navController.popBackStack() })
        }

        composable(Screen.Profile.route) {
            ProfileScreen(
                onBack = { navController.popBackStack() },
                onLogout = {
                    sessionManager.clearSession()
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                onNavigateToDriverDashboard = {
                    sessionManager.saveDriverMode(true)
                    sessionManager.setPolicyPending(true)
                    navController.navigate(Screen.DriverPolicy.route) {
                        popUpTo(Screen.Home.route) { inclusive = false }
                    }
                }
            )
        }

        composable(Screen.DriverPolicy.route) {
            DriverPolicyScreen(
                sessionManager = sessionManager,
                daysRemaining = sessionManager.driverLockDaysRemaining(),
                onAcceptAndContinue = {
                    navController.navigate(Screen.DriverDashboard.route) {
                        popUpTo(Screen.Home.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.DriverDashboard.route) {
            DriverScreen(
                onSwitchToPassenger = {
                    // Explicit switch — clears driver mode, lets user go to passenger
                    sessionManager.saveDriverMode(false)
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.DriverDashboard.route) { inclusive = true }
                    }
                },
                // Fix #2: Logout from driver — goes to Login WITHOUT clearing isDriverMode in Firestore
                onLogout = {
                    sessionManager.clearSession()
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                sessionManager = sessionManager
            )
        }
    }
}
