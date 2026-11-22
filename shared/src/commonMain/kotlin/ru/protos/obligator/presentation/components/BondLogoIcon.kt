package ru.protos.obligator.presentation.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

@Composable
fun BondLogoIcon(modifier: Modifier = Modifier.size(36.dp)) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val scaleX = w / 1024f
        val scaleY = h / 1024f

        drawRoundRect(
            brush = Brush.linearGradient(
                colors = listOf(Color(0xFF4C8DFF), Color(0xFF0A2A7A)),
                start = Offset(0f, 0f),
                end = Offset(w, h)
            ),
            cornerRadius = CornerRadius(160f * scaleX, 160f * scaleY),
            size = Size(w, h)
        )

        drawRoundRect(
            color = Color.White.copy(alpha = 0.35f),
            topLeft = Offset(282f * scaleX, 640f * scaleY),
            size = Size(120f * scaleX, 140f * scaleY),
            cornerRadius = CornerRadius(30f * scaleX, 30f * scaleY)
        )

        drawRoundRect(
            color = Color.White.copy(alpha = 0.55f),
            topLeft = Offset(452f * scaleX, 560f * scaleY),
            size = Size(120f * scaleX, 220f * scaleY),
            cornerRadius = CornerRadius(30f * scaleX, 30f * scaleY)
        )

        drawRoundRect(
            color = Color.White.copy(alpha = 0.85f),
            topLeft = Offset(622f * scaleX, 460f * scaleY),
            size = Size(120f * scaleX, 320f * scaleY),
            cornerRadius = CornerRadius(30f * scaleX, 30f * scaleY)
        )

        val path = Path().apply {
            moveTo(250f * scaleX, 560f * scaleY)
            cubicTo(
                380f * scaleX, 540f * scaleY,
                470f * scaleX, 430f * scaleY,
                560f * scaleX, 360f * scaleY
            )
            cubicTo(
                640f * scaleX, 300f * scaleY,
                710f * scaleX, 270f * scaleY,
                780f * scaleX, 230f * scaleY
            )
        }

        drawPath(
            path = path,
            color = Color.White,
            style = Stroke(width = 36f * scaleX)
        )

        drawCircle(
            color = Color.White,
            radius = 40f * scaleX,
            center = Offset(780f * scaleX, 230f * scaleY)
        )
    }
}
