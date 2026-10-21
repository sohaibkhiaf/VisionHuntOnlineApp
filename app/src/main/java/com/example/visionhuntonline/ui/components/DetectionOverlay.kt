package com.example.visionhuntonline.ui.components

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.visionhuntonline.model.Detection

@Composable
fun DetectionOverlay(
    detections: List<Detection>,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.fillMaxSize()) {
        val canvasWidth = size.width
        val canvasHeight = size.height

        val strokeWidthPx = 3.dp.toPx()
        val boxColor = Color(0xFF4ADE80)
        val textBgColor = Color(0xFF166534).toArgb()
        val textColor = android.graphics.Color.WHITE

        val textPaint = Paint().apply {
            color = textColor
            textSize = 14.sp.toPx()
            typeface = Typeface.DEFAULT_BOLD
            isAntiAlias = true
        }

        val bgPaint = Paint().apply {
            color = textBgColor
            style = Paint.Style.FILL
            isAntiAlias = true
        }

        for (detection in detections) {
            val rect = detection.boundingBox

            val left = rect.left * canvasWidth
            val top = rect.top * canvasHeight
            val right = rect.right * canvasWidth
            val bottom = rect.bottom * canvasHeight

            drawRect(
                color = boxColor,
                topLeft = Offset(left, top),
                size = Size(right - left, bottom - top),
                style = Stroke(width = strokeWidthPx)
            )

            val labelText = "${detection.label} ${(detection.score * 100).toInt()}%"
            val textWidth = textPaint.measureText(labelText)
            val textHeight = textPaint.textSize

            val padding = 8f
            val tagLeft = left
            val tagTop = (top - textHeight - padding * 2).coerceAtLeast(0f)
            val tagRight = tagLeft + textWidth + padding * 2
            val tagBottom = tagTop + textHeight + padding * 2

            drawContext.canvas.nativeCanvas.drawRect(
                tagLeft, tagTop, tagRight, tagBottom, bgPaint
            )

            drawContext.canvas.nativeCanvas.drawText(
                labelText,
                tagLeft + padding,
                tagBottom - padding - 4f,
                textPaint
            )
        }
    }
}
