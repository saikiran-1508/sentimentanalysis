package com.example.sentimentanalysis.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.sentimentanalysis.data.EmotionProfile
// Import our centralized colors
import com.example.sentimentanalysis.ui.theme.*

@Composable
fun EmotionBreakdownList(profile: EmotionProfile) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp)
            .background(CardBg, shape = RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Text(
            text = "Detailed Emotion Analysis",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(16.dp))

        // FIXED: Now uses the correct properties (happiness, disgust) from your Data Class
        EmotionRow("Happiness", profile.happiness, NeonGreen)
        EmotionRow("Sadness", profile.sadness, NeonBlue)
        EmotionRow("Anger", profile.anger, NeonRed)
        EmotionRow("Fear", profile.fear, NeonPurple)
        EmotionRow("Surprise", profile.surprise, NeonYellow)
        EmotionRow("Disgust", profile.disgust, NeonOrange)
    }
}

@Composable
fun EmotionRow(label: String, percentage: Int, color: Color) {
    val animatedProgress by animateFloatAsState(targetValue = percentage / 100f, label = "progress")

    Column(modifier = Modifier.padding(vertical = 6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = label, color = Color.White, style = MaterialTheme.typography.bodyMedium)
            Text(text = "$percentage%", color = Color.Gray, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(6.dp))

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