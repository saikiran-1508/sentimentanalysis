package com.example.sentimentanalysis.data

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.ai.client.generativeai.GenerativeModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// 1. Data for the Chart (Dominant Emotion Score)
data class SentimentDataPoint(
    val score: Int, // 1-10 Intensity
    val label: String,
    val timestamp: String
)

// 2. Data for the Detailed List (The Breakdown)
data class EmotionProfile(
    val anger: Int = 0,
    val joy: Int = 0,     // Laughing/Happy
    val calm: Int = 0,    // Relaxed
    val sadness: Int = 0,
    val fear: Int = 0,
    val surprise: Int = 0
)

class SentimentViewModel : ViewModel() {

    private val _uiState = MutableStateFlow<SentimentState>(SentimentState.Idle)
    val uiState = _uiState.asStateFlow()

    // Chart History
    private val _sentimentHistory = MutableStateFlow<List<SentimentDataPoint>>(emptyList())
    val sentimentHistory = _sentimentHistory.asStateFlow()

    // NEW: Detailed Breakdown State
    private val _currentEmotionProfile = MutableStateFlow(EmotionProfile())
    val currentEmotionProfile = _currentEmotionProfile.asStateFlow()

    private val generativeModel = GenerativeModel(
        modelName = "gemini-1.5-flash",
        apiKey = "YOUR_GEMINI_API_KEY_HERE"
    )

    fun analyzeSentiment(text: String) {
        if (text.isBlank()) return
        _uiState.value = SentimentState.Loading

        viewModelScope.launch {
            try {
                // 3. The "Pro" Prompt: Ask for specific numbers for EVERY emotion
                val prompt = """
                    Analyze the sentiment of this text spoken by a user: "$text"
                    
                    I need a breakdown of these 6 emotions: Anger, Joy, Calm, Sadness, Fear, Surprise.
                    Rate EACH one from 0 to 100 based on the text intensity.
                    
                    Also, give me a "Dominant Sentiment" label and a "Positivity Score" (1-10) for the chart.
                    
                    Respond in this EXACT format:
                    DOMINANT_LABEL|SCORE_1_TO_10|ANGER_PCT|JOY_PCT|CALM_PCT|SADNESS_PCT|FEAR_PCT|SURPRISE_PCT
                    
                    Example response:
                    Happy|8|5|80|10|0|0|5
                """.trimIndent()

                val response = generativeModel.generateContent(prompt)
                val rawText = response.text?.trim() ?: "Neutral|5|0|0|0|0|0|0"

                // 4. Parse the complex response
                val parts = rawText.split("|")
                val dominantLabel = parts.getOrElse(0) { "Neutral" }
                val chartScore = parts.getOrElse(1) { "5" }.toIntOrNull() ?: 5

                // Detailed Breakdown
                val anger = parts.getOrElse(2) { "0" }.toIntOrNull() ?: 0
                val joy = parts.getOrElse(3) { "0" }.toIntOrNull() ?: 0
                val calm = parts.getOrElse(4) { "0" }.toIntOrNull() ?: 0
                val sadness = parts.getOrElse(5) { "0" }.toIntOrNull() ?: 0
                val fear = parts.getOrElse(6) { "0" }.toIntOrNull() ?: 0
                val surprise = parts.getOrElse(7) { "0" }.toIntOrNull() ?: 0

                // Update Chart History
                val newPoint = SentimentDataPoint(
                    score = chartScore,
                    label = dominantLabel,
                    timestamp = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
                )
                val currentList = _sentimentHistory.value.toMutableList()
                currentList.add(newPoint)
                _sentimentHistory.value = currentList

                // Update Detailed Breakdown
                _currentEmotionProfile.value = EmotionProfile(anger, joy, calm, sadness, fear, surprise)

                _uiState.value = SentimentState.Success(dominantLabel, text)

            } catch (e: Exception) {
                _uiState.value = SentimentState.Error("AI Error: ${e.message}")
            }
        }
    }
}

// State Helper
sealed class SentimentState {
    object Idle : SentimentState()
    object Loading : SentimentState()
    data class Success(val sentiment: String, val input: String) : SentimentState()
    data class Error(val message: String) : SentimentState()
}