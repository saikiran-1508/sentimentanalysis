package com.example.sentimentanalysis.data

import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class AuthViewModel : ViewModel() {
    // Firebase instance
    private val auth: FirebaseAuth = Firebase.auth

    // State management
    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState = _authState.asStateFlow()

    // 1. Get Current User (Used by Dashboard)
    val currentUser: FirebaseUser?
        get() = auth.currentUser

    // 2. Sign Out (Used by Dashboard)
    fun signOut() {
        auth.signOut()
        _authState.value = AuthState.Idle
    }

    // 3. Sign Up (Used by SignUpScreen)
    fun signUp(email: String, password: String) {
        _authState.value = AuthState.Loading
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    _authState.value = AuthState.Success("Account Created Successfully!")
                } else {
                    _authState.value = AuthState.Error(task.exception?.message ?: "Sign Up Failed")
                }
            }
    }

    // 4. Sign In (Used by SignInScreen)
    fun signIn(email: String, password: String) {
        _authState.value = AuthState.Loading
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    _authState.value = AuthState.Success("Welcome Back!")
                } else {
                    _authState.value = AuthState.Error(task.exception?.message ?: "Login Failed")
                }
            }
    }

    // 5. Reset State (Used after navigation)
    fun resetState() {
        _authState.value = AuthState.Idle
    }
}

// --- STATE CLASSES (Must be outside the ViewModel class) ---
sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    data class Success(val message: String) : AuthState()
    data class Error(val message: String) : AuthState()
}