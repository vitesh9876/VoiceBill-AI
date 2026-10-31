package com.example.voicebillai.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.voicebillai.ui.theme.*

/**
 * Frosted Glass Card mimicking Apple Liquid Glass / Modern Glassmorphism
 */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(22.dp),
    backgroundColor: Color = GlassCardBg,
    borderColor: Color = GlassCardBorder,
    borderWidth: Dp = 1.dp,
    elevation: Dp = 2.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = modifier
            .shadow(elevation, shape, ambientColor = Color(0x10000000), spotColor = Color(0x15000000))
            .clip(shape)
            .border(BorderStroke(borderWidth, borderColor), shape),
        color = backgroundColor
    ) {
        Column(content = content)
    }
}

/**
 * Organic Sage & Mint blurred background gradient
 */
@Composable
fun GlassBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        GlassBackgroundTop,
                        GlassBackgroundMid,
                        GlassBackgroundBottom
                    )
                )
            )
    ) {
        content()
    }
}

