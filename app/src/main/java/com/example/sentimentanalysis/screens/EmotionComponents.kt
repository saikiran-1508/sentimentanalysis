package com.example.sentimentanalysis.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sentimentanalysis.data.EmotionProfile

@Composable
fun EmotionBreakdownList(profile: EmotionProfile) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp)
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Text(
            text = "Detailed Emotion Analysis",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(16.dp))

        // List of all emotions with their colors
        EmotionRow("Joy / Laughing", profile.joy, Color(0xFF00E676)) // Bright Green
        EmotionRow("Calm / Relaxed", profile.calm, Color(0xFF00B0FF)) // Light Blue
        EmotionRow("Surprise", profile.surprise, Color(0xFFFFEA00))   // Yellow
        EmotionRow("Sadness", profile.sadness, Color(0xFF90A4AE))     // Grey Blue
        EmotionRow("Fear", profile.fear, Color(0xFFAA00FF))           // Purple
        EmotionRow("Anger", profile.anger, Color(0xFFFF1744))         // Red
    }
}

@Composable
fun EmotionRow(label: String, percentage: Int, color: Color) {
    val animatedProgress by animateFloatAsState(targetValue = percentage / 100f)

    Column(modifier = Modifier.padding(vertical = 6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = label, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
            Text(text = "$percentage%", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(6.dp))

        // Custom Progress Bar
        LinearProgressIndicator(
            progress = { animatedProgress },
            modifier = Modifier
                .fillMaxWidth()
                .height(10.dp)
                .clip(RoundedCornerShape(5.dp)),
            color = color,
            trackColor = color.copy(alpha = 0.2f),
        )
    }
}