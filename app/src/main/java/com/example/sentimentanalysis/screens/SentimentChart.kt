package com.example.sentimentanalysis.screens

// --- CORRECT IMPORTS (Material 3) ---
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card           // Must be material3
import androidx.compose.material3.CardDefaults   // Must be material3
import androidx.compose.material3.MaterialTheme  // Must be material3
import androidx.compose.material3.Text           // Must be material3
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

@Composable
fun SentimentChart(history: List<SentimentDataPoint>) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp),
        shape = RoundedCornerShape(16.dp),
        // This 'colorScheme' will now work because of the material3 imports above
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Emotional Trend (Last 10)",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))

            if (history.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = "Speak to track emotions...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
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
                        // Invert Y because 0 is at the top
                        val y = height - ((point.score / 10f) * height)

                        if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)

                        // Color logic: Green for positive (6+), Red for negative
                        val dotColor = if (point.score >= 6) Color(0xFF00C853) else Color(0xFFD50000)

                        drawCircle(
                            color = dotColor,
                            radius = 12f,
                            center = Offset(x, y)
                        )
                    }

                    drawPath(
                        path = path,
                        color = Color(0xFF1565C0),
                        style = Stroke(width = 8f, cap = StrokeCap.Round)
                    )

                    val fillPath = Path().apply {
                        addPath(path)
                        lineTo(width, height)
                        lineTo(0f, height)
                        close()
                    }

                    drawPath(
                        path = fillPath,
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFF1565C0).copy(alpha = 0.3f),
                                Color.Transparent
                            )
                        )
                    )
                }
            }
        }
    }
}