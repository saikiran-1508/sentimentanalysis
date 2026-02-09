package com.example.sentimentanalysis

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.example.sentimentanalysis.ui.NavGraph
import com.example.sentimentanalysis.ui.theme.SentimentAnalysisTheme
import com.google.firebase.FirebaseApp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FirebaseApp.initializeApp(this)
        setContent {
            SentimentAnalysisTheme {
                NavGraph()
            }
        }
    }
}