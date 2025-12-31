package com.mviiiax.m8ax_diariogimnasio

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class CustomAnalogClock @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {
    private val paintCircle = Paint().apply {
        color = Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = 8f
        isAntiAlias = true
    }
    private val paintMark = Paint().apply {
        color = Color.WHITE
        strokeWidth = 4f
        isAntiAlias = true
    }
    private val paintHourHand = Paint().apply {
        color = Color.WHITE
        strokeWidth = 14f
        strokeCap = Paint.Cap.ROUND
        isAntiAlias = true
    }
    private val paintMinuteHand = Paint().apply {
        color = Color.WHITE
        strokeWidth = 10f
        strokeCap = Paint.Cap.ROUND
        isAntiAlias = true
    }
    private val paintSecondHand = Paint().apply {
        color = Color.RED
        strokeWidth = 4f
        strokeCap = Paint.Cap.ROUND
        isAntiAlias = true
    }
    private val paintText = Paint().apply {
        color = Color.WHITE
        textSize = 52f
        textAlign = Paint.Align.CENTER
        isAntiAlias = true
    }
    private val paintDate = Paint().apply {
        color = Color.WHITE
        textSize = 68f
        textAlign = Paint.Align.CENTER
        isAntiAlias = true
    }
    private val paintFirma = Paint().apply {
        color = Color.WHITE
        textSize = 58f
        textAlign = Paint.Align.CENTER
        isAntiAlias = true
    }
    private val paintFirma1 = Paint().apply {
        color = Color.WHITE
        textSize = 100f
        textAlign = Paint.Align.CENTER
        isAntiAlias = true
    }
    private var scaleFactor = 1.0f
    private val scaleDetector =
        ScaleGestureDetector(context, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
            override fun onScale(detector: ScaleGestureDetector): Boolean {
                scaleFactor *= detector.scaleFactor
                scaleFactor = scaleFactor.coerceIn(0.5f, 3.0f)
                invalidate()
                return true
            }
        })

    override fun onDraw(canvas: Canvas) {
        canvas.save()
        val cx = width / 2f
        val cy = height / 2f + 80f
        canvas.scale(scaleFactor, scaleFactor, cx, cy)
        val radius = (Math.min(width, height) / 2f) - 40f
        canvas.drawCircle(cx, cy, radius, paintCircle)
        drawMarks(canvas, cx, cy, radius)
        val calendar = Calendar.getInstance()
        val hour = calendar.get(Calendar.HOUR)
        val minute = calendar.get(Calendar.MINUTE)
        val second = calendar.get(Calendar.SECOND)
        val hourAngle = (hour + minute / 60f) * 30f
        val minuteAngle = (minute + second / 60f) * 6f
        val secondAngle = second * 6f
        drawHand(canvas, cx, cy, radius * 0.50f, hourAngle, paintHourHand)
        drawHand(canvas, cx, cy, radius * 0.70f, minuteAngle, paintMinuteHand)
        drawHand(canvas, cx, cy, radius * 0.85f, secondAngle, paintSecondHand)
        val fecha = SimpleDateFormat("EEEE dd/MM/yyyy", Locale("es", "ES")).format(Date())
            .replaceFirstChar { it.uppercase() }
        canvas.drawText(fecha, cx, cy - radius - 190f, paintDate)
        canvas.drawText("By M8AX", cx, cy + radius - 80f, paintFirma)
        canvas.drawText("--- CRONÓMETRO ---", cx, cy + radius + 230f, paintFirma1)
        canvas.restore()
        postInvalidateDelayed(1000)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        scaleDetector.onTouchEvent(event)
        return true
    }

    private fun drawMarks(canvas: Canvas, cx: Float, cy: Float, radius: Float) {
        for (i in 0 until 60) {
            val angle = Math.toRadians((i * 6).toDouble())
            val start = if (i % 5 == 0) radius * 0.88f else radius * 0.94f
            val stop = radius
            val startX = cx + (start * Math.cos(angle)).toFloat()
            val startY = cy + (start * Math.sin(angle)).toFloat()
            val stopX = cx + (stop * Math.cos(angle)).toFloat()
            val stopY = cy + (stop * Math.sin(angle)).toFloat()
            paintMark.strokeWidth = if (i % 5 == 0) 6f else 3f
            canvas.drawLine(startX, startY, stopX, stopY, paintMark)
        }
    }

    private fun drawHand(
        canvas: Canvas, cx: Float, cy: Float, length: Float, angleDeg: Float, paint: Paint
    ) {
        val angle = Math.toRadians(angleDeg - 90.0)
        val x = cx + length * Math.cos(angle).toFloat()
        val y = cy + length * Math.sin(angle).toFloat()
        canvas.drawLine(cx, cy, x, y, paint)
    }
}