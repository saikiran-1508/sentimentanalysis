package com.example.sentimentanalysis.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// --- COLORS MATCHED CLOSELY TO IMAGE ---

// Top (teal glow area)
val GradientTop = Color(0xFF1FA59A)

// Bottom (deep blue teal dark)
val GradientBottom = Color(0xFF052B38)

@Composable
fun AppBackground(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        GradientTop,
                        GradientBottom
                    )
                )
            )
    ) {
        content()
    }
}
