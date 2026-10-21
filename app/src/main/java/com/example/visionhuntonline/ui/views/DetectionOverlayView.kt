package com.example.visionhuntonline.ui.views

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.View
import com.example.visionhuntonline.model.Detection

class DetectionOverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var detections: List<Detection> = emptyList()

    private val boxPaint = Paint().apply {
        color = Color.parseColor("#4ADE80")
        style = Paint.Style.STROKE
        strokeWidth = 8f
        isAntiAlias = true
    }

    private val textBgPaint = Paint().apply {
        color = Color.parseColor("#166534")
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    private val textPaint = Paint().apply {
        color = Color.WHITE
        textSize = 36f
        typeface = Typeface.DEFAULT_BOLD
        isAntiAlias = true
    }

    fun updateDetections(newDetections: List<Detection>) {
        detections = newDetections
        postInvalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val canvasWidth = width.toFloat()
        val canvasHeight = height.toFloat()

        for (detection in detections) {
            val rect = detection.boundingBox

            val left = rect.left * canvasWidth
            val top = rect.top * canvasHeight
            val right = rect.right * canvasWidth
            val bottom = rect.bottom * canvasHeight

            // Draw bounding box
            canvas.drawRect(left, top, right, bottom, boxPaint)

            // Draw label tag
            val labelText = "${detection.label} ${(detection.score * 100).toInt()}%"
            val textWidth = textPaint.measureText(labelText)
            val textHeight = textPaint.textSize

            val padding = 12f
            val tagLeft = left
            val tagTop = (top - textHeight - padding * 2).coerceAtLeast(0f)
            val tagRight = tagLeft + textWidth + padding * 2
            val tagBottom = tagTop + textHeight + padding * 2

            canvas.drawRect(tagLeft, tagTop, tagRight, tagBottom, textBgPaint)
            canvas.drawText(labelText, tagLeft + padding, tagBottom - padding - 6f, textPaint)
        }
    }
}
