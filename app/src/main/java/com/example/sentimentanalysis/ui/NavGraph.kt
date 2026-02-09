package com.example.sentimentanalysis.ui

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.sentimentanalysis.data.AuthViewModel
import com.example.sentimentanalysis.screens.DashboardScreen
import com.example.sentimentanalysis.screens.SignInScreen
import com.example.sentimentanalysis.screens.SignUpScreen

@Composable
fun NavGraph(authViewModel: AuthViewModel = viewModel()) {
    val navController = rememberNavController()
    val currentUser = authViewModel.currentUser

    // Determine start destination based on if user is already logged in
    val startDest = if (currentUser != null) "dashboard" else "signin"

    NavHost(navController = navController, startDestination = startDest) {

        composable("signin") {
            SignInScreen(
                viewModel = authViewModel,
                onAuthSuccess = {
                    navController.navigate("dashboard") {
                        popUpTo("signin") { inclusive = true }
                    }
                },
                onNavigateToSignUp = { navController.navigate("signup") }
            )
        }

        composable("signup") {
            SignUpScreen(
                viewModel = authViewModel,
                onAuthSuccess = {
                    navController.navigate("dashboard") {
                        popUpTo("signup") { inclusive = true }
                    }
                },
                onNavigateToSignIn = { navController.popBackStack() }
            )
        }

        // In NavGraph.kt -> Dashboard composable
        composable("dashboard") {
            val user = authViewModel.currentUser?.displayName ?: "User"
            // We create the SentimentViewModel here
            DashboardScreen(
                userData = user,
                onSignOut = {
                    authViewModel.signOut()
                    navController.navigate("signin") {
                        popUpTo("dashboard") { inclusive = true }
                    }
                }
            )
        }
    }
}