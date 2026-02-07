package com.yourname.sentimentanalysis.ui

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.yourname.sentimentanalysis.data.AuthViewModel
import com.yourname.sentimentanalysis.screens.SignInScreen
import com.yourname.sentimentanalysis.screens.SignUpScreen

@Composable
fun NavGraph(authViewModel: AuthViewModel = viewModel()) {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "signin") {
        composable("signin") {
            SignInScreen(
                viewModel = authViewModel,
                onNavigateToSignUp = { navController.navigate("signup") },
                onAuthSuccess = { /* Navigate to Dashboard */ }
            )
        }
        composable("signup") {
            SignUpScreen(
                viewModel = authViewModel,
                onNavigateToSignIn = { navController.navigate("signin") },
                onAuthSuccess = { /* Navigate to Dashboard */ }
            )
        }
    }
}