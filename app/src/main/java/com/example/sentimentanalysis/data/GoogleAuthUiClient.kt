package com.example.sentimentanalysis.data

import android.content.Context
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await
import java.util.concurrent.CancellationException

class GoogleAuthUiClient(private val context: Context) {
    private val auth = Firebase.auth
    private val credentialManager = CredentialManager.create(context)

    /**
     * Signs in using Google.
     * @return null if successful, or an Error Message String if it fails.
     */
    suspend fun signIn(): String? {
        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId("162310677260-2i0bstgooh50u6fbkn0e2oec6p627dtj.apps.googleusercontent.com") // TODO: CHECK THIS ID!
            .setAutoSelectEnabled(true)
            .setNonce(null)
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        return try {
            val result = credentialManager.getCredential(context, request)

            if (result.credential is CustomCredential) {
                val cred = GoogleIdTokenCredential.createFrom(result.credential.data)
                val firebaseCred = GoogleAuthProvider.getCredential(cred.idToken, null)
                auth.signInWithCredential(firebaseCred).await()
                null // Success! Returning null means "No Error"
            } else {
                "Unknown Credential Type"
            }
        } catch (e: GetCredentialException) {
            Log.e("GoogleAuth", "GetCredentialException: ${e.message}")
            // If the user manually closed the window, we return "Cancelled"
            if (e.message?.contains("User cancelled") == true || e.message?.contains("closed") == true) {
                return "Cancelled"
            }
            e.message ?: "Google Sign-In Failed"
        } catch (e: NoCredentialException) {
            Log.e("GoogleAuth", "No Credential: ${e.message}")
            "No Google Account found on device"
        } catch (e: CancellationException) {
            Log.d("GoogleAuth", "User Cancelled")
            "Cancelled"
        } catch (e: Exception) {
            Log.e("GoogleAuth", "Unknown Error: ${e.message}")
            "Error: ${e.message}"
        }
    }

    fun getSignedInUser(): String? = auth.currentUser?.displayName
    suspend fun signOut() = auth.signOut()
}