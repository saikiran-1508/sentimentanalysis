package com.example.sentimentanalysis.ui

import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.sentimentanalysis.data.AuthViewModel
import com.example.sentimentanalysis.data.GoogleAuthUiClient
import com.example.sentimentanalysis.data.SentimentViewModel
import com.example.sentimentanalysis.screens.DashboardScreen
import com.example.sentimentanalysis.screens.ProfileScreen
import com.example.sentimentanalysis.screens.SignInScreen
import kotlinx.coroutines.launch

@Composable
fun NavGraph(
    authViewModel: AuthViewModel = viewModel(),
    sentimentViewModel: SentimentViewModel = viewModel()
) {
    val navController = rememberNavController()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Initialize Google Auth Client to check for logged-in user
    val googleAuthUiClient = remember { GoogleAuthUiClient(context) }

    // Determine where to start (Dashboard if logged in, Sign In if not)
    val startDestination = if (googleAuthUiClient.getSignedInUser() != null) "dashboard" else "signin"

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        // 1. SIGN IN SCREEN
        composable("signin") {
            SignInScreen(
                viewModel = authViewModel,
                onAuthSuccess = {
                    navController.navigate("dashboard") {
                        popUpTo("signin") { inclusive = true } // Clear back stack
                    }
                },
                onNavigateToSignUp = {
                    Toast.makeText(context, "Sign Up coming soon", Toast.LENGTH_SHORT).show()
                }
            )
        }

        // 2. DASHBOARD SCREEN
        composable("dashboard") {
            DashboardScreen(
                sentimentViewModel = sentimentViewModel,
                onNavigateToProfile = {
                    navController.navigate("profile")
                }
            )
        }

        // 3. PROFILE SCREEN
        composable("profile") {
            ProfileScreen(
                viewModel = sentimentViewModel,
                onBack = {
                    navController.popBackStack()
                },
                onLogout = {
                    scope.launch {
                        googleAuthUiClient.signOut()
                        Toast.makeText(context, "Logged Out", Toast.LENGTH_SHORT).show()

                        // Go back to Sign In and clear history
                        navController.navigate("signin") {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                }
            )
        }
    }
}