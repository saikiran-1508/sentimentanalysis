package com.example.sentimentanalysis.screens

import android.app.Activity
import android.content.Intent
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.sentimentanalysis.data.EmotionProfile
import com.example.sentimentanalysis.data.SentimentState
import com.example.sentimentanalysis.data.SentimentViewModel
import java.util.Locale

// Neon Colors
val NeonGreen = Color(0xFF00E676)
val NeonBlue = Color(0xFF2979FF)
val NeonRed = Color(0xFFFF1744)
val NeonPurple = Color(0xFFD500F9)
val NeonYellow = Color(0xFFFFEA00)
val NeonOrange = Color(0xFFFF9100)
val DarkBg = Color(0xFF121212)
val CardBg = Color(0xFF1E1E1E)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onNavigateToProfile: () -> Unit,
    sentimentViewModel: SentimentViewModel = viewModel()
) {
    val uiState by sentimentViewModel.uiState.collectAsState()
    var inputText by remember { mutableStateOf("") }

    // Feedback States
    var showFeedbackDialog by remember { mutableStateOf(false) }
    var lastAnalyzedText by remember { mutableStateOf("") }

    val context = LocalContext.current
    var tts: TextToSpeech? by remember { mutableStateOf(null) }

    LaunchedEffect(Unit) {
        tts = TextToSpeech(context) { if (it != TextToSpeech.ERROR) tts?.language = Locale.getDefault() }
    }

    val speechLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val res = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.get(0)
            if (res != null) inputText = res
        }
    }

    // --- FEEDBACK DIALOG (Self-Improvement) ---
    if (showFeedbackDialog) {
        AlertDialog(
            onDismissRequest = { showFeedbackDialog = false },
            containerColor = CardBg,
            title = { Text("Improve AI Accuracy", color = Color.White) },
            text = { Text("What was the correct emotion?", color = Color.Gray) },
            confirmButton = {
                Column {
                    val emotions = listOf("Happiness", "Sadness", "Anger", "Fear", "Surprise", "Disgust")
                    emotions.forEach { emotion ->
                        TextButton(
                            onClick = {
                                sentimentViewModel.teachAI(lastAnalyzedText, emotion)
                                showFeedbackDialog = false
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(emotion, color = NeonBlue)
                        }
                    }
                }
            }
        )
    }

    Scaffold(
        containerColor = DarkBg,
        topBar = {
            TopAppBar(
                title = { Text("AI Sentiment Analyzer", color = Color.White, fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = onNavigateToProfile) {
                        Box(Modifier.size(36.dp).clip(CircleShape).background(NeonPurple), Alignment.Center) {
                            Icon(Icons.Default.Person, null, tint = Color.White)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBg)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.padding(padding).fillMaxSize().padding(horizontal = 20.dp).verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(20.dp))

            // 1. Mic Button
            Box(
                modifier = Modifier
                    .size(140.dp)
                    .clip(CircleShape)
                    .background(Brush.radialGradient(listOf(NeonPurple.copy(alpha=0.4f), Color.Transparent)))
                    .clickable {
                        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                            putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak now...")
                        }
                        try { speechLauncher.launch(intent) } catch (e: Exception) { }
                    },
                contentAlignment = Alignment.Center
            ) {
                Box(Modifier.size(90.dp).clip(CircleShape).background(Brush.linearGradient(listOf(NeonPurple, NeonBlue))), Alignment.Center) {
                    Icon(Icons.Default.Mic, null, tint = Color.White, modifier = Modifier.size(40.dp))
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 2. Text & Controls
            if (inputText.isNotBlank()) {
                Text("\"$inputText\"", color = Color.White, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(bottom = 12.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Button(
                        onClick = { tts?.speak(inputText, TextToSpeech.QUEUE_FLUSH, null, null) },
                        colors = ButtonDefaults.buttonColors(containerColor = CardBg),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.VolumeUp, null, tint = NeonBlue)
                        Text(" Listen", color = Color.White)
                    }

                    Button(
                        onClick = {
                            lastAnalyzedText = inputText
                            sentimentViewModel.analyzeSentiment(inputText)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = NeonPurple),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.width(140.dp)
                    ) {
                        if (uiState is SentimentState.Loading) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp))
                        } else {
                            Text("Analyze", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(30.dp))

            // 3. RESULTS (Clean No-Emoji Layout)
            AnimatedVisibility(visible = uiState is SentimentState.Success) {
                if (uiState is SentimentState.Success) {
                    val state = uiState as SentimentState.Success

                    Column {
                        // The New Bar Chart
                        EmotionBarList(state.profile)

                        Spacer(modifier = Modifier.height(24.dp))

                        // 4. Feedback Question
                        Card(
                            colors = CardDefaults.cardColors(containerColor = CardBg),
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color.Gray.copy(alpha=0.3f))
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Was this correct?", color = Color.Gray)
                                Row {
                                    TextButton(onClick = { /* Correct */ }) { Text("Yes", color = NeonGreen) }
                                    TextButton(onClick = { showFeedbackDialog = true }) { Text("No", color = NeonRed) }
                                }
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(50.dp))
        }
    }
}

// --- NEW COMPONENT: Clean Layout (Label Top, Bar Bottom) ---
@Composable
fun EmotionBarList(profile: EmotionProfile) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CardBg),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Emotion Breakdown", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Spacer(modifier = Modifier.height(16.dp))

            // Clean Labels (No Emojis)
            EmotionRowItem("Happiness", profile.happiness, NeonGreen)
            EmotionRowItem("Sadness", profile.sadness, NeonBlue)
            EmotionRowItem("Anger", profile.anger, NeonRed)
            EmotionRowItem("Fear", profile.fear, NeonPurple)
            EmotionRowItem("Surprise", profile.surprise, NeonYellow)
            EmotionRowItem("Disgust", profile.disgust, NeonOrange)
        }
    }
}

@Composable
fun EmotionRowItem(label: String, percentage: Int, color: Color) {
    val animatedProgress by animateFloatAsState(targetValue = percentage / 100f, label = "progress")

    // Vertical Stack: Label on TOP, Bar BELOW
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
    ) {
        // 1. Label and Percentage Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label, // Clean Text
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = "$percentage%",
                color = Color.Gray,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        // 2. The Bar (Now below the text)
        LinearProgressIndicator(
            progress = { animatedProgress },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp)),
            color = color,
            trackColor = Color.DarkGray
        )
    }
}