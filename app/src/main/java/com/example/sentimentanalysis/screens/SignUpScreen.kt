package com.example.sentimentanalysis.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sentimentanalysis.data.AuthViewModel
import com.example.sentimentanalysis.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignUpScreen(
    viewModel: AuthViewModel,
    onAuthSuccess: () -> Unit,
    onNavigateToSignIn: () -> Unit
) {
    val context = LocalContext.current

    // Collecting states
    val isLoading by viewModel.isLoading.collectAsState()
    val authError by viewModel.authError.collectAsState()
    val signInSuccess by viewModel.signInSuccess.collectAsState()

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }

    // Handle Success
    LaunchedEffect(signInSuccess) {
        if (signInSuccess) {
            Toast.makeText(context, "Account Created!", Toast.LENGTH_SHORT).show()
            onAuthSuccess()
            viewModel.resetState()
        }
    }

    // Handle Error
    LaunchedEffect(authError) {
        if (authError != null) {
            Toast.makeText(context, authError, Toast.LENGTH_LONG).show()
            viewModel.resetState()
        }
    }

    // --- APPLY NEW GRADIENT BACKGROUND ---
    AppBackground {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // --- HEADER ---
                Text(
                    text = "Create Account",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Text(
                    text = "Join us to get started",
                    fontSize = 16.sp,
                    color = Color.LightGray.copy(alpha = 0.8f),
                    modifier = Modifier.padding(bottom = 40.dp)
                )

                // --- INPUTS (Transparent Style) ---

                // Email
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email", color = Color.White.copy(alpha = 0.7f)) },
                    leadingIcon = { Icon(Icons.Default.Email, null, tint = Color.White) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = transparentFieldColors(), // Same helper from SignInScreen
                    shape = RoundedCornerShape(16.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))

                // Password
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password", color = Color.White.copy(alpha = 0.7f)) },
                    leadingIcon = { Icon(Icons.Default.Lock, null, tint = Color.White) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    colors = transparentFieldColors(),
                    shape = RoundedCornerShape(16.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))

                // Confirm Password
                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = { confirmPassword = it },
                    label = { Text("Confirm Password", color = Color.White.copy(alpha = 0.7f)) },
                    leadingIcon = { Icon(Icons.Default.Lock, null, tint = Color.White) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    colors = transparentFieldColors(),
                    shape = RoundedCornerShape(16.dp)
                )

                Spacer(modifier = Modifier.height(32.dp))

                // --- SIGN UP BUTTON ---
                Button(
                    onClick = {
                        if (password == confirmPassword) {
                            viewModel.signUpWithEmail(email, password)
                        } else {
                            Toast.makeText(context, "Passwords do not match", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2ECC71)), // Matching Green
                    enabled = !isLoading
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                    } else {
                        Text("Sign Up", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // --- LOGIN LINK ---
                Row {
                    Text("Already have an account? ", color = Color.White.copy(alpha = 0.8f), fontSize = 15.sp)
                    Text(
                        "Log In",
                        color = Color(0xFF2ECC71),
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        modifier = Modifier.clickable { onNavigateToSignIn() }
                    )
                }
            }
        }
    }
}