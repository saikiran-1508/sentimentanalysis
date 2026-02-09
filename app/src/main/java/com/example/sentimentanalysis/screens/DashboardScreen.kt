package com.example.sentimentanalysis.screens

import android.app.Activity
import android.content.Intent
import android.speech.RecognizerIntent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.sentimentanalysis.data.EmotionProfile
import com.example.sentimentanalysis.data.SentimentState
import com.example.sentimentanalysis.data.SentimentViewModel
import java.util.Calendar

// Define Colors
val ColorHappiness = Color(0xFFF1C40F)
val ColorSadness = Color(0xFF3498DB)
val ColorAnger = Color(0xFFE74C3C)
val ColorFear = Color(0xFF9B59B6)
val ColorSurprise = Color(0xFFE67E22)
val ColorDisgust = Color(0xFF2ECC71)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: SentimentViewModel = viewModel(),
    onNavigateToProfile: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    // Greeting
    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    val greetingTitle = when (hour) {
        in 5..11 -> "Good Morning"
        in 12..16 -> "Good Afternoon"
        in 17..20 -> "Good Evening"
        else -> "Good Night"
    }

    val voiceLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data = result.data
            val results = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            val spokenText = results?.get(0) ?: ""
            if (spokenText.isNotBlank()) viewModel.analyzeSentiment(spokenText)
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f, targetValue = 1.1f,
        animationSpec = infiniteRepeatable(tween(1500), RepeatMode.Reverse), label = "scale"
    )

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = { Text("AI Sentiment Analyzer", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp) },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                    actions = {
                        IconButton(onClick = onNavigateToProfile) {
                            Icon(Icons.Default.Person, contentDescription = "Profile", tint = Color.Gray)
                        }
                    }
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier.padding(padding).fillMaxSize().padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // HIDE Greeting & Mic when showing results
                if (uiState !is SentimentState.Success) {
                    Spacer(modifier = Modifier.height(20.dp))
                    Text(text = greetingTitle, color = Color.Gray, fontSize = 32.sp, fontWeight = FontWeight.SemiBold)
                    Text(text = "Ready to listen?", color = Color.Gray, fontSize = 28.sp, fontWeight = FontWeight.SemiBold)

                    Spacer(modifier = Modifier.height(40.dp))

                    Box(modifier = Modifier.size(200.dp), contentAlignment = Alignment.Center) {
                        Box(modifier = Modifier.size(160.dp).scale(scale).border(1.dp, Color.White.copy(alpha = 0.2f), CircleShape))
                        FloatingActionButton(
                            onClick = {
                                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, java.util.Locale.getDefault())
                                    putExtra(RecognizerIntent.EXTRA_PROMPT, "How are you feeling?")
                                }
                                try { voiceLauncher.launch(intent) }
                                catch (e: Exception) { Toast.makeText(context, "Voice input not supported", Toast.LENGTH_SHORT).show() }
                            },
                            modifier = Modifier.size(100.dp).border(2.dp, Color.White, CircleShape),
                            containerColor = Color.Black,
                            shape = CircleShape,
                            elevation = FloatingActionButtonDefaults.elevation(0.dp)
                        ) {
                            Icon(Icons.Default.Mic, null, modifier = Modifier.size(40.dp), tint = Color.White)
                        }
                    }
                    Spacer(modifier = Modifier.height(20.dp))
                    Text("Tap mic to start", color = Color.Gray, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)

                    // Show error if any
                    if (uiState is SentimentState.Error) {
                        Spacer(modifier = Modifier.height(20.dp))
                        Text((uiState as SentimentState.Error).message, color = Color.Red, fontSize = 12.sp)
                    }
                }

                // --- RESULTS SECTION ---
                if (uiState is SentimentState.Success) {
                    val state = uiState as SentimentState.Success

                    Text(
                        text = state.sentiment.uppercase(),
                        color = Color.White,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // ROW: Pie Chart (Left) | List (Right)
                    Row(
                        modifier = Modifier.fillMaxWidth().height(300.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 1. Pie Chart
                        Box(modifier = Modifier.weight(1f).aspectRatio(1f)) {
                            EmotionPieChart(state.profile)
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        // 2. Breakdown List
                        Column(modifier = Modifier.weight(1.2f)) {
                            // FIXED: Using unique name "DashboardEmotionRow"
                            DashboardEmotionRow("Happiness", state.profile.happiness, ColorHappiness)
                            DashboardEmotionRow("Sadness", state.profile.sadness, ColorSadness)
                            DashboardEmotionRow("Anger", state.profile.anger, ColorAnger)
                            DashboardEmotionRow("Fear", state.profile.fear, ColorFear)
                            DashboardEmotionRow("Surprise", state.profile.surprise, ColorSurprise)
                            DashboardEmotionRow("Disgust", state.profile.disgust, ColorDisgust)
                        }
                    }

                    Spacer(modifier = Modifier.height(30.dp))

                    Button(
                        onClick = { viewModel.analyzeSentiment("") },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray)
                    ) {
                        Text("Analyze Again", color = Color.White)
                    }
                }

                if (uiState is SentimentState.Loading) CircularProgressIndicator(color = Color.White)
            }
        }
    }
}

// FIX: Switched Triple -> Pair to fix construction error
@Composable
fun EmotionPieChart(profile: EmotionProfile) {
    val total = (profile.happiness + profile.sadness + profile.anger + profile.fear + profile.surprise + profile.disgust).toFloat()

    val slices = listOf(
        Pair(profile.happiness, ColorHappiness),
        Pair(profile.sadness, ColorSadness),
        Pair(profile.anger, ColorAnger),
        Pair(profile.fear, ColorFear),
        Pair(profile.surprise, ColorSurprise),
        Pair(profile.disgust, ColorDisgust)
    ).filter { it.first > 0 }

    Canvas(modifier = Modifier.fillMaxSize().padding(8.dp)) {
        var startAngle = -90f
        val safeTotal = if (total == 0f) 1f else total

        slices.forEach { (value, color) ->
            val sweepAngle = (value / safeTotal) * 360f
            drawArc(
                color = color,
                startAngle = startAngle,
                sweepAngle = sweepAngle,
                useCenter = true,
                size = Size(size.width, size.height)
            )
            startAngle += sweepAngle
        }
    }
}

// FIX: Renamed to avoid conflict with EmotionComponents.kt
@Composable
fun DashboardEmotionRow(label: String, value: Int, color: Color) {
    if (value > 0) {
        Column(modifier = Modifier.padding(vertical = 4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(10.dp).background(color, CircleShape))
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "$label: $value%", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
            LinearProgressIndicator(
                progress = { value / 100f },
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp).height(4.dp).clip(RoundedCornerShape(2.dp)),
                color = color,
                trackColor = Color.DarkGray
            )
        }
    }
}