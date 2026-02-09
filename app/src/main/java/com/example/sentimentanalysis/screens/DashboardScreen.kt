package com.example.sentimentanalysis.screens

import android.app.Activity
import android.content.Intent
import android.speech.RecognizerIntent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.sentimentanalysis.data.SentimentState
import com.example.sentimentanalysis.data.SentimentViewModel
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    userData: String?,
    onSignOut: () -> Unit,
    sentimentViewModel: SentimentViewModel = viewModel()
) {
    val uiState by sentimentViewModel.uiState.collectAsState()
    val history by sentimentViewModel.sentimentHistory.collectAsState()
    var inputText by remember { mutableStateOf("") }
    val context = LocalContext.current

    // Voice Launcher
    val speechLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data = result.data
            val resultText = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.get(0)
            if (resultText != null) {
                inputText = resultText
                sentimentViewModel.analyzeSentiment(inputText)
            }
        } else {
            Toast.makeText(context, "Voice input failed", Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("AI Dashboard", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = onSignOut) {
                        Icon(Icons.Default.Logout, contentDescription = "Sign Out", tint = MaterialTheme.colorScheme.error)
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(20.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Hello, ${userData ?: "User"}",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.align(Alignment.Start)
            )

            Spacer(modifier = Modifier.height(20.dp))

            // 1. The Sentiment Chart
            SentimentChart(history = history)

            Spacer(modifier = Modifier.height(24.dp))

            // 2. Input Area with Mic
            OutlinedTextField(
                value = inputText,
                onValueChange = { inputText = it },
                label = { Text("Type or Speak...") },
                modifier = Modifier.fillMaxWidth().height(120.dp),
                shape = RoundedCornerShape(16.dp),
                trailingIcon = {
                    IconButton(onClick = {
                        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                            putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak now...")
                        }
                        try {
                            speechLauncher.launch(intent)
                        } catch (e: Exception) {
                            Toast.makeText(context, "Voice feature not supported", Toast.LENGTH_SHORT).show()
                        }
                    }) {
                        Icon(Icons.Default.Mic, contentDescription = "Voice Input", tint = MaterialTheme.colorScheme.primary)
                    }
                }
            )

            Spacer(modifier = Modifier.height(24.dp))

            // 3. Analyze Button
            Button(
                onClick = { sentimentViewModel.analyzeSentiment(inputText) },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(12.dp),
                enabled = inputText.isNotBlank() && uiState !is SentimentState.Loading
            ) {
                if (uiState is SentimentState.Loading) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Icon(Icons.Default.Analytics, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Analyze Text")
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 4. Result Animation
            AnimatedVisibility(visible = uiState is SentimentState.Success, enter = fadeIn()) {
                if (uiState is SentimentState.Success) {
                    val state = uiState as SentimentState.Success
                    // THIS IS THE FUNCTION THAT WAS MISSING
                    ResultCard(sentiment = state.sentiment)
                }
            }

            if (uiState is SentimentState.Error) {
                Text(
                    text = (uiState as SentimentState.Error).message,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 16.dp)
                )
            }
        }
    }
}

// --- MISSING FUNCTION ADDED BELOW ---
@Composable
fun ResultCard(sentiment: String) {
    val (bgColor, icon, text) = when (sentiment.lowercase()) {
        "positive" -> Triple(Color(0xFF00C853), Icons.Default.Mood, "Positive Vibes!")
        "negative" -> Triple(Color(0xFFD50000), Icons.Default.MoodBad, "Negative Emotion")
        else -> Triple(Color(0xFF455A64), Icons.Default.SentimentNeutral, "Neutral Tone")
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(140.dp),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(8.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.horizontalGradient(listOf(bgColor, bgColor.copy(alpha = 0.7f))))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Analysis Result",
                        color = Color.White.copy(alpha = 0.8f),
                        style = MaterialTheme.typography.labelLarge
                    )
                    Text(
                        text = text,
                        color = Color.White,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(64.dp)
                )
            }
        }
    }
}