package com.example.sentimentanalysis

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.google.firebase.FirebaseApp
// These must match your actual folder structure
import com.example.sentimentanalysis.ui.theme.SentimentAnalysisTheme
import com.example.sentimentanalysis.ui.auth.AuthScreen
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FirebaseApp.initializeApp(this)
        setContent {
            SentimentAnalysisTheme {
                // If you haven't made NavGraph yet, just call AuthScreen directly
                AuthScreen(onAuthSuccess = {
                    // Navigate later
                })
            }
        }
    }
}