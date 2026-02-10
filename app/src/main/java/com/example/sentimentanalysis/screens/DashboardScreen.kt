package com.example.sentimentanalysis.screens

import android.Manifest
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.sentimentanalysis.data.EmotionProfile
import com.example.sentimentanalysis.data.SentimentState
import com.example.sentimentanalysis.data.SentimentViewModel
import java.util.Calendar
import java.util.Locale
import kotlin.math.cos
import kotlin.math.sin

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
    val audioPath by viewModel.lastAudioPath.collectAsState() // Accesses the actual recording path
    val context = LocalContext.current

    // --- Media Player for Voice Playback ---
    val mediaPlayer = remember { MediaPlayer() }

    // --- PERMISSION LAUNCHER ---
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.startRecording(context)
        } else {
            Toast.makeText(context, "Microphone permission is required", Toast.LENGTH_SHORT).show()
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            mediaPlayer.release()
        }
    }

    // Greeting logic
    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    val greetingTitle = when (hour) {
        in 5..11 -> "Good Morning"
        in 12..16 -> "Good Afternoon"
        in 17..20 -> "Good Evening"
        else -> "Good Night"
    }

    // Pulse Animation for Mic
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f, targetValue = 1.2f,
        animationSpec = infiniteRepeatable(tween(1000), RepeatMode.Reverse), label = "scale"
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
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // --- RECORDING UI ---
                if (uiState !is SentimentState.Success) {
                    Spacer(modifier = Modifier.height(20.dp))
                    Text(text = greetingTitle, color = Color.Gray, fontSize = 32.sp, fontWeight = FontWeight.SemiBold)

                    val statusText = if (uiState is SentimentState.Recording) "Listening... Tap to Stop" else "Tap mic to Record"
                    val statusColor = if (uiState is SentimentState.Recording) Color.Red else Color.Gray

                    Text(text = statusText, color = statusColor, fontSize = 24.sp, fontWeight = FontWeight.SemiBold)

                    Spacer(modifier = Modifier.height(40.dp))

                    // MIC BUTTON WITH ANIMATION
                    Box(modifier = Modifier.size(200.dp), contentAlignment = Alignment.Center) {
                        if (uiState is SentimentState.Recording) {
                            Box(modifier = Modifier.size(160.dp).scale(scale).border(1.dp, Color.Red.copy(alpha = 0.5f), CircleShape))
                        } else {
                            Box(modifier = Modifier.size(160.dp).border(1.dp, Color.White.copy(alpha = 0.2f), CircleShape))
                        }

                        FloatingActionButton(
                            onClick = {
                                if (uiState is SentimentState.Recording) {
                                    viewModel.stopAndAnalyze()
                                } else {
                                    val hasPermission = ContextCompat.checkSelfPermission(
                                        context,
                                        Manifest.permission.RECORD_AUDIO
                                    ) == PackageManager.PERMISSION_GRANTED

                                    if (hasPermission) {
                                        viewModel.startRecording(context)
                                    } else {
                                        permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                    }
                                }
                            },
                            modifier = Modifier.size(100.dp).border(2.dp, if(uiState is SentimentState.Recording) Color.Red else Color.White, CircleShape),
                            containerColor = Color.Black,
                            shape = CircleShape,
                            elevation = FloatingActionButtonDefaults.elevation(0.dp)
                        ) {
                            if (uiState is SentimentState.Recording) {
                                Icon(Icons.Default.Stop, null, modifier = Modifier.size(40.dp), tint = Color.Red)
                            } else {
                                Icon(Icons.Default.Mic, null, modifier = Modifier.size(40.dp), tint = Color.White)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    if (uiState is SentimentState.Error) {
                        Text((uiState as SentimentState.Error).message, color = Color.Red, fontSize = 12.sp)
                    }
                    if (uiState is SentimentState.Loading) {
                        CircularProgressIndicator(color = Color.White)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Analyzing Audio...", color = Color.White)
                    }
                }

                // --- RESULTS UI ---
                if (uiState is SentimentState.Success) {
                    val state = uiState as SentimentState.Success

                    Text(
                        text = state.sentiment.uppercase(),
                        color = Color.White,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Transcript Card with VOICE PLAYBACK
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A)),
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Transcript:", color = Color.Gray, fontSize = 12.sp)
                                Text(
                                    text = "\"${state.input}\"",
                                    color = Color.White,
                                    fontSize = 16.sp,
                                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                                )
                            }
                            IconButton(onClick = {
                                audioPath?.let { path ->
                                    try {
                                        mediaPlayer.reset()
                                        mediaPlayer.setDataSource(path)
                                        mediaPlayer.prepare()
                                        mediaPlayer.start()
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "Playback error", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }) {
                                Icon(Icons.Default.VolumeUp, "Listen to yourself", tint = ColorHappiness)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // PIE CHART (Restored and math-fixed)
                    Box(modifier = Modifier.size(300.dp)) {
                        EmotionPieChartWithLabels(state.profile)
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // EMOTION ROWS
                    Column(modifier = Modifier.fillMaxWidth()) {
                        DashboardEmotionRow("Happiness", state.profile.happiness, ColorHappiness)
                        DashboardEmotionRow("Sadness", state.profile.sadness, ColorSadness)
                        DashboardEmotionRow("Anger", state.profile.anger, ColorAnger)
                        DashboardEmotionRow("Fear", state.profile.fear, ColorFear)
                        DashboardEmotionRow("Surprise", state.profile.surprise, ColorSurprise)
                        DashboardEmotionRow("Disgust", state.profile.disgust, ColorDisgust)
                    }

                    Spacer(modifier = Modifier.height(30.dp))

                    Button(
                        onClick = { viewModel.resetState() },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray)
                    ) {
                        Text("Analyze New Audio", color = Color.White)
                    }
                    Spacer(modifier = Modifier.height(40.dp))
                }
            }
        }
    }
}

// --- PIE CHART COMPONENT ---

@Composable
fun EmotionPieChartWithLabels(profile: EmotionProfile) {
    val total = (profile.happiness + profile.sadness + profile.anger + profile.fear + profile.surprise + profile.disgust).toFloat()

    val slices = listOf(
        Triple(profile.happiness, ColorHappiness, "Happy"),
        Triple(profile.sadness, ColorSadness, "Sad"),
        Triple(profile.anger, ColorAnger, "Anger"),
        Triple(profile.fear, ColorFear, "Fear"),
        Triple(profile.surprise, ColorSurprise, "Surprise"),
        Triple(profile.disgust, ColorDisgust, "Disgust")
    ).filter { it.first > 0 }

    Canvas(modifier = Modifier.fillMaxSize()) {
        val centerX = size.width / 2
        val centerY = size.height / 2
        val radius = size.minDimension / 2
        var startAngle = -90f
        val safeTotal = if (total == 0f) 1f else total

        slices.forEach { (value, color, label) ->
            val sweepAngle = (value / safeTotal) * 360f
            drawArc(color = color, startAngle = startAngle, sweepAngle = sweepAngle, useCenter = true, size = Size(size.width, size.height))

            if (sweepAngle > 15f) {
                val midAngle = Math.toRadians((startAngle + sweepAngle / 2).toDouble())
                val textRadius = radius * 0.7f
                val x = (centerX + textRadius * cos(midAngle)).toFloat()
                val y = (centerY + textRadius * sin(midAngle)).toFloat()

                drawIntoCanvas { canvas ->
                    val paint = android.graphics.Paint().apply {
                        this.color = android.graphics.Color.WHITE
                        this.textSize = 32f
                        this.textAlign = android.graphics.Paint.Align.CENTER
                        this.isFakeBoldText = true
                    }
                    canvas.nativeCanvas.drawText(label, x, y, paint)
                    canvas.nativeCanvas.drawText("$value%", x, y + 35f, paint.apply { textSize = 24f })
                }
            }
            startAngle += sweepAngle
        }
    }
}

// --- EMOTION PROGRESS BAR COMPONENT ---

@Composable
fun DashboardEmotionRow(label: String, value: Int, color: Color) {
    if (value > 0) {
        Column(modifier = Modifier.padding(vertical = 4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(10.dp).background(color, CircleShape))
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "$label: $value%", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
            LinearProgressIndicator(
                progress = { value / 100f },
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp).height(6.dp).clip(RoundedCornerShape(3.dp)),
                color = color,
                trackColor = Color.DarkGray
            )
        }
    }
}