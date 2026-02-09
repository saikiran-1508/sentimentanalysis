package com.example.sentimentanalysis.screens

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.sentimentanalysis.data.AuthState
import com.example.sentimentanalysis.data.AuthViewModel
import com.example.sentimentanalysis.data.GoogleAuthUiClient
import kotlinx.coroutines.launch

@Composable
fun SignInScreen(
    viewModel: AuthViewModel,
    onAuthSuccess: () -> Unit,
    onNavigateToSignUp: () -> Unit
) {
    val authState by viewModel.authState.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val googleAuthUiClient = remember { GoogleAuthUiClient(context) }

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    LaunchedEffect(authState) {
        when (val state = authState) {
            is AuthState.Success -> {
                isLoading = false
                Toast.makeText(context, state.message, Toast.LENGTH_SHORT).show()
                onAuthSuccess()
                viewModel.resetState()
            }
            is AuthState.Error -> {
                isLoading = false
                Toast.makeText(context, state.message, Toast.LENGTH_LONG).show()
                viewModel.resetState()
            }
            is AuthState.Loading -> isLoading = true
            else -> isLoading = false
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Sentiment AI", style = MaterialTheme.typography.displayMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.ExtraBold)
        Text("Login to continue", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.secondary)

        Spacer(modifier = Modifier.height(48.dp))

        Card(elevation = CardDefaults.cardElevation(4.dp), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            Column(modifier = Modifier.padding(16.dp)) {
                OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Email") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(value = password, onValueChange = { password = it }, label = { Text("Password") }, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = { viewModel.signIn(email, password) },
            modifier = Modifier.fillMaxWidth().height(54.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
            enabled = !isLoading
        ) {
            if (isLoading) CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary) else Text("Sign In", fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(16.dp))

        // UPDATED GOOGLE BUTTON LOGIC
        OutlinedButton(
            onClick = {
                scope.launch {
                    isLoading = true
                    val error = googleAuthUiClient.signIn()

                    if (error == null) {
                        // SUCCESS: Navigate to Dashboard
                        Toast.makeText(context, "Google Sign-In Successful!", Toast.LENGTH_SHORT).show()
                        onAuthSuccess()
                    } else if (error.contains("Cancelled") || error.contains("closed")) {
                        // USER CLOSED IT: Do nothing, just stop loading
                        // Optional: Log it -> Log.d("SignIn", "User cancelled flow")
                    } else {
                        // REAL ERROR: Show it
                        Toast.makeText(context, "Sign In Failed: $error", Toast.LENGTH_LONG).show()
                    }
                    isLoading = false
                }
            },
            modifier = Modifier.fillMaxWidth().height(54.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("Continue with Google")
        }

        Spacer(modifier = Modifier.height(24.dp))
        TextButton(onClick = onNavigateToSignUp) { Text("New here? Create Account") }
    }
}