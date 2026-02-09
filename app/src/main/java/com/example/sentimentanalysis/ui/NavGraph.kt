package com.example.sentimentanalysis.ui

import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.sentimentanalysis.data.AuthViewModel
import com.example.sentimentanalysis.data.SentimentViewModel
import com.example.sentimentanalysis.screens.*
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

@Composable
fun NavGraph(
    authViewModel: AuthViewModel = viewModel(),
    sentimentViewModel: SentimentViewModel = viewModel()
) {
    val navController = rememberNavController()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val auth = FirebaseAuth.getInstance()

    // IF logged in -> Dashboard. IF NOT -> Landing
    val startDestination = if (auth.currentUser != null) "dashboard" else "landing"

    NavHost(navController = navController, startDestination = startDestination) {

        // 1. LANDING SCREEN
        composable("landing") {
            LandingScreen(
                onGetStarted = {
                    navController.navigate("signin")
                }
            )
        }

        // 2. SIGN IN SCREEN
        composable("signin") {
            SignInScreen(
                viewModel = authViewModel,
                onAuthSuccess = {
                    navController.navigate("dashboard") {
                        popUpTo("landing") { inclusive = true } // Clear backstack so user can't go back to login
                    }
                },
                // This was missing in your code, causing the error:
                onNavigateToSignUp = {
                    navController.navigate("signup")
                }
            )
        }

        // 3. SIGN UP SCREEN (Restored)
        composable("signup") {
            SignUpScreen(
                viewModel = authViewModel,
                onAuthSuccess = {
                    navController.navigate("dashboard") {
                        popUpTo("landing") { inclusive = true }
                    }
                },
                onNavigateToSignIn = {
                    navController.popBackStack() // Go back to Sign In
                }
            )
        }

        // 4. DASHBOARD
        composable("dashboard") {
            DashboardScreen(
                onNavigateToProfile = { navController.navigate("profile") }
            )
        }

        // 5. PROFILE
        composable("profile") {
            ProfileScreen(
                viewModel = sentimentViewModel,
                onBack = { navController.popBackStack() },
                onLogout = {
                    scope.launch {
                        auth.signOut()
                        Toast.makeText(context, "Logged Out", Toast.LENGTH_SHORT).show()
                        navController.navigate("landing") {
                            popUpTo(0) { inclusive = true } // Clear everything
                        }
                    }
                }
            )
        }
    }
}