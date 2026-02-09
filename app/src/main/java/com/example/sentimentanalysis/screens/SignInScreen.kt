package com.example.sentimentanalysis.screens

import android.app.Activity
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sentimentanalysis.data.AuthViewModel
import com.example.sentimentanalysis.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignInScreen(
    viewModel: AuthViewModel,
    onAuthSuccess: () -> Unit,
    onNavigateToSignUp: () -> Unit
) {
    val context = LocalContext.current
    val isLoading by viewModel.isLoading.collectAsState()
    val isOtpSent by viewModel.isOtpSent.collectAsState()
    val authError by viewModel.authError.collectAsState()
    val signInSuccess by viewModel.signInSuccess.collectAsState()
    val resetEmailSent by viewModel.passwordResetEmailSent.collectAsState()

    var isPhoneMode by remember { mutableStateOf(false) }

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var phoneNumber by remember { mutableStateOf("+91") }
    var otpCode by remember { mutableStateOf("") }

    var showForgotPassword by remember { mutableStateOf(false) }
    var resetEmailText by remember { mutableStateOf("") }

    BackHandler(enabled = isPhoneMode) {
        isPhoneMode = false
        viewModel.resetState()
    }

    LaunchedEffect(signInSuccess) {
        if (signInSuccess) {
            onAuthSuccess()
            viewModel.resetState()
        }
    }

    LaunchedEffect(resetEmailSent) {
        if (resetEmailSent) {
            Toast.makeText(context, "Reset link sent!", Toast.LENGTH_LONG).show()
            showForgotPassword = false
            viewModel.resetState()
        }
    }

    // Wrap content in the new AppBackground
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
                // --- HEADER TITLE ---
                Text(
                    text = if (isPhoneMode) (if (isOtpSent) "Verify OTP" else "Phone Login") else "Welcome Back",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.ExtraBold, // Extra Bold for visibility
                    color = Color.White,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Text(
                    text = "Sign in to continue",
                    fontSize = 16.sp,
                    color = Color.LightGray.copy(alpha = 0.8f),
                    modifier = Modifier.padding(bottom = 40.dp)
                )

                if (!isPhoneMode) {
                    // ================= EMAIL LOGIN MODE =================

                    // Email Field
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Email Address", color = Color.White.copy(alpha = 0.7f)) },
                        leadingIcon = { Icon(Icons.Default.Email, null, tint = Color.White) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = transparentFieldColors(), // Custom transparent look
                        shape = RoundedCornerShape(16.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    // Password Field
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

                    // Forgot Password Link
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Forgot Password?",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.align(Alignment.End).clickable { showForgotPassword = true }
                    )

                    Spacer(modifier = Modifier.height(32.dp))

                    // Login Button (Green/Teal theme to match background, or sticking to Neon)
                    Button(
                        onClick = { viewModel.signInWithEmail(email, password) },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        // Using a bright Green/Cyan for high contrast on the dark teal
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2ECC71))
                    ) {
                        if (isLoading) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                        else Text("Log In", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Sign Up Link
                    Row {
                        Text("Don't have an account? ", color = Color.White.copy(alpha = 0.8f), fontSize = 15.sp)
                        Text(
                            "Sign Up",
                            color = Color(0xFF2ECC71), // Match button color
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            modifier = Modifier.clickable { onNavigateToSignUp() }
                        )
                    }

                    Spacer(modifier = Modifier.height(30.dp))
                    HorizontalDivider(color = Color.White.copy(alpha = 0.3f), thickness = 1.dp)
                    Spacer(modifier = Modifier.height(30.dp))

                    // "Continue with Phone" Button
                    OutlinedButton(
                        onClick = { isPhoneMode = true },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                        border = androidx.compose.foundation.BorderStroke(1.5.dp, Color.White.copy(alpha = 0.5f))
                    ) {
                        Icon(Icons.Default.Phone, null, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Continue with Phone", fontSize = 16.sp, fontWeight = FontWeight.Medium)
                    }

                } else {
                    // ================= PHONE LOGIN MODE =================
                    if (!isOtpSent) {
                        OutlinedTextField(
                            value = phoneNumber,
                            onValueChange = { phoneNumber = it },
                            label = { Text("Phone Number", color = Color.White.copy(alpha = 0.7f)) },
                            leadingIcon = { Icon(Icons.Default.Phone, null, tint = Color.White) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            colors = transparentFieldColors(),
                            shape = RoundedCornerShape(16.dp)
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(
                            onClick = {
                                val activity = context as? Activity
                                if (activity != null) viewModel.sendOtp(phoneNumber, activity)
                            },
                            modifier = Modifier.fillMaxWidth().height(56.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2ECC71))
                        ) {
                            if (isLoading) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                            else Text("Get OTP", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        }
                    } else {
                        Text("Code sent to $phoneNumber", color = Color.White.copy(alpha = 0.8f), fontSize = 16.sp)
                        Spacer(modifier = Modifier.height(24.dp))
                        OutlinedTextField(
                            value = otpCode,
                            onValueChange = { if (it.length <= 6) otpCode = it },
                            label = { Text("Enter OTP", color = Color.White.copy(alpha = 0.7f)) },
                            leadingIcon = { Icon(Icons.Default.Lock, null, tint = Color.White) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                            colors = transparentFieldColors(),
                            shape = RoundedCornerShape(16.dp)
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(
                            onClick = { viewModel.verifyOtp(otpCode) },
                            modifier = Modifier.fillMaxWidth().height(56.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2ECC71))
                        ) {
                            if (isLoading) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                            else Text("Verify", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    TextButton(onClick = { isPhoneMode = false; viewModel.resetState() }) {
                        Text("Cancel", color = Color.White.copy(alpha = 0.7f))
                    }
                }

                if (authError != null) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = authError!!,
                        color = Color(0xFFFF6B6B), // Soft red for error on teal background
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        if (showForgotPassword) {
            AlertDialog(
                onDismissRequest = { showForgotPassword = false },
                containerColor = Color(0xFF031820), // Dark background for dialog
                title = { Text("Reset Password", color = Color.White) },
                text = {
                    OutlinedTextField(
                        value = resetEmailText,
                        onValueChange = { resetEmailText = it },
                        label = { Text("Email", color = Color.Gray) },
                        colors = transparentFieldColors()
                    )
                },
                confirmButton = {
                    Button(onClick = { viewModel.resetPassword(resetEmailText) }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2ECC71))) { Text("Send") }
                },
                dismissButton = {
                    TextButton(onClick = { showForgotPassword = false }) { Text("Cancel", color = Color.Gray) }
                }
            )
        }
    }
}

// Custom Colors for the "Transparent/Glass" look on input fields
@Composable
fun transparentFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = Color.White,
    unfocusedTextColor = Color.White,
    focusedBorderColor = Color(0xFF2ECC71), // Green border when active
    unfocusedBorderColor = Color.White.copy(alpha = 0.5f), // Semi-transparent white when inactive
    cursorColor = Color.White,
    focusedLabelColor = Color(0xFF2ECC71),
    unfocusedLabelColor = Color.White.copy(alpha = 0.7f),
    focusedContainerColor = Color.White.copy(alpha = 0.05f), // Very subtle fill
    unfocusedContainerColor = Color.White.copy(alpha = 0.05f)
)