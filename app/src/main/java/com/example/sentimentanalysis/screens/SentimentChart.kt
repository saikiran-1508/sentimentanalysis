package com.example.sentimentanalysis.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.sentimentanalysis.data.SentimentDataPoint
// IMPORT YOUR COLORS
import com.example.sentimentanalysis.ui.theme.*

@Composable
fun SentimentChart(history: List<SentimentDataPoint>) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = CardBg // Uses CardBg from Color.kt
        ),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Positivity Trend (Last 10)",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))

            if (history.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(text = "Speak to track emotions...", color = Color.Gray)
                }
            } else {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val width = size.width
                    val height = size.height
                    val points = history.takeLast(10)

                    if (points.size < 2) return@Canvas

                    val path = Path()
                    val spacePerPoint = width / (points.size - 1)

                    points.forEachIndexed { index, point ->
                        val x = index * spacePerPoint

                        // FIX: Calculate score manually (Happiness + Surprise)
                        val positiveSum = point.profile.happiness + point.profile.surprise
                        val score = (positiveSum / 10f).coerceIn(0f, 10f)

                        val y = height - ((score / 10f) * height)

                        if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)

                        val dotColor = if (score >= 5) NeonGreen else NeonRed

                        drawCircle(color = dotColor, radius = 12f, center = Offset(x, y))
                    }

                    drawPath(path = path, color = NeonBlue, style = Stroke(width = 8f, cap = StrokeCap.Round))

                    val fillPath = Path().apply {
                        addPath(path)
                        lineTo(width, height)
                        lineTo(0f, height)
                        close()
                    }
                    drawPath(path = fillPath, brush = Brush.verticalGradient(listOf(NeonBlue.copy(alpha = 0.3f), Color.Transparent)))
                }
            }
        }
    }
}