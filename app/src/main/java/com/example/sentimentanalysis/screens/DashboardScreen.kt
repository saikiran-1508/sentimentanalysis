package com.example.sentimentanalysis.screens

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.sentimentanalysis.data.EmotionProfile
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
    val currentProfile by sentimentViewModel.currentEmotionProfile.collectAsState()

    var inputText by remember { mutableStateOf("") }
    var isListening by remember { mutableStateOf(false) } // Track if mic is active
    val context = LocalContext.current

    // Initialize SpeechRecognizer
    val speechRecognizer = remember { SpeechRecognizer.createSpeechRecognizer(context) }
    val speechIntent = remember {
        Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        }
    }

    // Permission Launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            Toast.makeText(context, "Permission Granted. Tap Mic to speak.", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "Microphone permission is required.", Toast.LENGTH_SHORT).show()
        }
    }

    // Speech Listener Logic
    DisposableEffect(Unit) {
        val listener = object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {}
            override fun onBeginningOfSpeech() { isListening = true }
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() { isListening = false }
            override fun onError(error: Int) {
                isListening = false
                // Don't show toast for "No match" to keep it clean
            }
            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    inputText = matches[0]
                    isListening = false
                }
            }
            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        }
        speechRecognizer.setRecognitionListener(listener)
        onDispose {
            speechRecognizer.destroy()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("AI Sentiment Analyzer", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = onSignOut) {
                        Icon(Icons.Default.Logout, contentDescription = "Sign Out", tint = MaterialTheme.colorScheme.error)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "Hello, ${userData ?: "User"}",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = if (isListening) "Listening..." else "Tap to speak in any language",
                style = MaterialTheme.typography.bodyMedium,
                color = if (isListening) Color.Red else MaterialTheme.colorScheme.secondary,
                fontWeight = if (isListening) FontWeight.Bold else FontWeight.Normal
            )

            Spacer(modifier = Modifier.height(30.dp))

            // 1. SEAMLESS MIC BUTTON (No Popup)
            val micColor by animateColorAsState(if (isListening) Color(0xFFFF1744) else MaterialTheme.colorScheme.primaryContainer, label = "micColor")
            val infiniteTransition = rememberInfiniteTransition(label = "pulse")
            val scale by if (isListening) {
                infiniteTransition.animateFloat(
                    initialValue = 1f,
                    targetValue = 1.2f,
                    animationSpec = infiniteRepeatable(tween(500), RepeatMode.Reverse),
                    label = "scale"
                )
            } else {
                remember { mutableStateOf(1f) }
            }

            Box(
                modifier = Modifier
                    .size(120.dp)
                    .scale(scale)
                    .clip(CircleShape)
                    .background(micColor)
                    .clickable {
                        // Check Permission first
                        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                            if (isListening) {
                                speechRecognizer.stopListening()
                                isListening = false
                            } else {
                                speechRecognizer.startListening(speechIntent)
                                isListening = true
                            }
                        } else {
                            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isListening) Icons.Default.GraphicEq else Icons.Default.Mic,
                    contentDescription = "Speak",
                    modifier = Modifier.size(60.dp),
                    tint = if (isListening) Color.White else MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Show text ONLY if spoken
            if (inputText.isNotBlank()) {
                Text(
                    text = "\"$inputText\"",
                    style = MaterialTheme.typography.bodyLarge,
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(16.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                // 2. ANALYZE BUTTON (Triggers Charts)
                Button(
                    onClick = { sentimentViewModel.analyzeSentiment(inputText) },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    enabled = uiState !is SentimentState.Loading
                ) {
                    if (uiState is SentimentState.Loading) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Analyzing...")
                    } else {
                        Text("Analyze Emotions")
                    }
                }
            }

            Spacer(modifier = Modifier.height(30.dp))

            // 3. RESULTS (Hidden until Analyze is clicked)
            AnimatedVisibility(visible = uiState is SentimentState.Success, enter = fadeIn()) {
                Column {
                    if (uiState is SentimentState.Success) {
                        val state = uiState as SentimentState.Success

                        // A. Main Result Card
                        ResultCard(sentiment = state.sentiment)

                        Spacer(modifier = Modifier.height(20.dp))

                        // B. The Chart
                        SentimentChart(history = history)

                        Spacer(modifier = Modifier.height(20.dp))

                        // C. Compact Side-by-Side Emotion Bars
                        CompactEmotionGrid(profile = currentProfile)
                    }
                }
            }

            if (uiState is SentimentState.Error) {
                Text(
                    text = (uiState as SentimentState.Error).message,
                    color = MaterialTheme.colorScheme.error
                )
            }

            Spacer(modifier = Modifier.height(50.dp))
        }
    }
}

// --- HELPER: Compact Grid for Emotions (Side by Side) ---
@Composable
fun CompactEmotionGrid(profile: EmotionProfile) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha=0.5f), RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Text("Emotion Breakdown", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(10.dp))

        Row(Modifier.fillMaxWidth()) {
            CompactEmotionItem("Joy", profile.joy, Color(0xFF00E676), Modifier.weight(1f))
            Spacer(modifier = Modifier.width(10.dp))
            CompactEmotionItem("Calm", profile.calm, Color(0xFF00B0FF), Modifier.weight(1f))
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth()) {
            CompactEmotionItem("Surprise", profile.surprise, Color(0xFFFFEA00), Modifier.weight(1f))
            Spacer(modifier = Modifier.width(10.dp))
            CompactEmotionItem("Sadness", profile.sadness, Color(0xFF90A4AE), Modifier.weight(1f))
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth()) {
            CompactEmotionItem("Fear", profile.fear, Color(0xFFAA00FF), Modifier.weight(1f))
            Spacer(modifier = Modifier.width(10.dp))
            CompactEmotionItem("Anger", profile.anger, Color(0xFFFF1744), Modifier.weight(1f))
        }
    }
}

@Composable
fun CompactEmotionItem(label: String, percentage: Int, color: Color, modifier: Modifier = Modifier) {
    val animatedProgress by animateFloatAsState(targetValue = percentage / 100f, label = "progress")

    Column(modifier = modifier) {
        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            Text(label, style = MaterialTheme.typography.bodySmall)
            Text("$percentage%", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
        }
        LinearProgressIndicator(
            progress = { animatedProgress },
            modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
            color = color,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )
    }
}

// --- HELPER: Result Card ---
@Composable
fun ResultCard(sentiment: String) {
    val (bgColor, icon, text) = when (sentiment.lowercase()) {
        "positive", "happy", "joy" -> Triple(Color(0xFF00C853), Icons.Default.Mood, "Positive Vibes!")
        "negative", "sad", "anger", "fear" -> Triple(Color(0xFFD50000), Icons.Default.MoodBad, "Negative Emotion")
        "calm", "relaxed" -> Triple(Color(0xFF00B0FF), Icons.Default.SelfImprovement, "Calm & Relaxed")
        else -> Triple(Color(0xFF455A64), Icons.Default.SentimentNeutral, "Neutral Tone")
    }

    Card(
        modifier = Modifier.fillMaxWidth().height(100.dp),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(8.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxSize().background(Brush.horizontalGradient(listOf(bgColor, bgColor.copy(alpha = 0.7f))))
        ) {
            Row(
                modifier = Modifier.fillMaxSize().padding(20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Dominant Emotion", color = Color.White.copy(alpha = 0.8f), style = MaterialTheme.typography.labelMedium)
                    Text(text, color = Color.White, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                }
                Icon(imageVector = icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(40.dp))
            }
        }
    }
}