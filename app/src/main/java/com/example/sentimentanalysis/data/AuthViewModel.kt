package com.example.sentimentanalysis.data

import android.app.Activity
import androidx.lifecycle.ViewModel
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.TimeUnit

class AuthViewModel : ViewModel() {

    private val auth = FirebaseAuth.getInstance()

    // --- UI STATES ---
    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _isOtpSent = MutableStateFlow(false)
    val isOtpSent = _isOtpSent.asStateFlow()

    private val _authError = MutableStateFlow<String?>(null)
    val authError = _authError.asStateFlow()

    private val _signInSuccess = MutableStateFlow(false)
    val signInSuccess = _signInSuccess.asStateFlow()

    private val _passwordResetEmailSent = MutableStateFlow(false)
    val passwordResetEmailSent = _passwordResetEmailSent.asStateFlow()

    private var verificationId: String? = null
    private var forceResendingToken: PhoneAuthProvider.ForceResendingToken? = null

    // --- 1. SIGN UP (Fixes SignUpScreen) ---
    fun signUpWithEmail(email: String, pass: String) {
        if (email.isBlank() || pass.isBlank()) {
            _authError.value = "Email and Password required"
            return
        }
        _isLoading.value = true
        _authError.value = null

        auth.createUserWithEmailAndPassword(email, pass)
            .addOnCompleteListener { task ->
                _isLoading.value = false
                if (task.isSuccessful) {
                    _signInSuccess.value = true
                } else {
                    _authError.value = task.exception?.message ?: "Sign Up Failed"
                }
            }
    }

    // --- 2. SIGN IN ---
    fun signInWithEmail(email: String, pass: String) {
        if (email.isBlank() || pass.isBlank()) {
            _authError.value = "Email and Password required"
            return
        }
        _isLoading.value = true
        _authError.value = null

        auth.signInWithEmailAndPassword(email, pass)
            .addOnCompleteListener { task ->
                _isLoading.value = false
                if (task.isSuccessful) {
                    _signInSuccess.value = true
                } else {
                    _authError.value = task.exception?.message ?: "Login Failed"
                }
            }
    }

    // --- 3. FORGOT PASSWORD ---
    fun resetPassword(email: String) {
        if (email.isBlank()) {
            _authError.value = "Enter email"
            return
        }
        _isLoading.value = true
        auth.sendPasswordResetEmail(email)
            .addOnCompleteListener { task ->
                _isLoading.value = false
                if (task.isSuccessful) {
                    _passwordResetEmailSent.value = true
                } else {
                    _authError.value = task.exception?.message ?: "Failed to send email"
                }
            }
    }

    // --- 4. RESET STATE (Fixes Red Error) ---
    fun resetState() {
        _passwordResetEmailSent.value = false
        _authError.value = null
        _signInSuccess.value = false
    }

    // --- 5. PHONE AUTH ---
    fun sendOtp(phoneNumber: String, activity: Activity) {
        if (phoneNumber.isBlank()) {
            _authError.value = "Enter phone number"
            return
        }
        _isLoading.value = true
        _authError.value = null

        val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                signInWithPhoneCredential(credential)
            }
            override fun onVerificationFailed(e: FirebaseException) {
                _isLoading.value = false
                _authError.value = e.message ?: "Verification failed"
            }
            override fun onCodeSent(verId: String, token: PhoneAuthProvider.ForceResendingToken) {
                _isLoading.value = false
                _isOtpSent.value = true
                verificationId = verId
                forceResendingToken = token
            }
        }

        val options = PhoneAuthOptions.newBuilder(auth)
            .setPhoneNumber(phoneNumber)
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(activity)
            .setCallbacks(callbacks)
            .build()
        PhoneAuthProvider.verifyPhoneNumber(options)
    }

    fun verifyOtp(code: String) {
        if (code.isBlank() || verificationId == null) {
            _authError.value = "Enter OTP"
            return
        }
        _isLoading.value = true
        val credential = PhoneAuthProvider.getCredential(verificationId!!, code)
        signInWithPhoneCredential(credential)
    }

    private fun signInWithPhoneCredential(credential: PhoneAuthCredential) {
        auth.signInWithCredential(credential)
            .addOnCompleteListener { task ->
                _isLoading.value = false
                if (task.isSuccessful) {
                    _signInSuccess.value = true
                } else {
                    _authError.value = task.exception?.message ?: "Sign In failed"
                }
            }
    }
}