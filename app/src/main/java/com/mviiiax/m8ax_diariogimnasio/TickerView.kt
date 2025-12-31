package com.mviiiax.m8ax_diariogimnasio

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.view.View
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class TickerView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {
    private var mensajeOriginal: String = ""
    private var mensajeActual: String = ""
    private val baseTextSize = 50f
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = Typeface.createFromAsset(context.assets, "fonts/m8ax.ttf")
        textSize = baseTextSize
        color = Color.RED
    }
    private var posX = 0f
    private var textWidth = 0f
    private var velocidad = 7f
    private val handler = Handler(Looper.getMainLooper())
    private var runnable: Runnable? = null
    private val colores =
        arrayOf(Color.RED, Color.CYAN, Color.MAGENTA, Color.YELLOW, Color.GREEN, Color.WHITE)
    private var colorIndex = -1
    fun setMensaje(m: String) {
        mensajeOriginal = m.take(1000)
        generarMensajeActual()
        ajustarVelocidad()
        posX = width.toFloat()
        ajustarTamanyo()
        invalidate()
    }

    private fun generarMensajeActual() {
        val ahora = Calendar.getInstance()
        val formato = SimpleDateFormat("EEEE d/MM/yyyy - HH:mm:ss", Locale("es", "ES"))
        val fechaHora = formato.format(ahora.time).uppercase(Locale.getDefault())
        mensajeActual = "$fechaHora --- $mensajeOriginal"
        colorIndex = (colorIndex + 1) % colores.size
        paint.color = colores[colorIndex]
    }

    private fun ajustarVelocidad() {
        val len = mensajeActual.length
        velocidad = when {
            len <= 250 -> 15f
            len <= 500 -> 25f
            else -> 35f
        }
    }

    private fun ajustarTamanyo() {
        if (width == 0 || height == 0) return
        if (mensajeActual.isEmpty()) return
        val tmp = Paint(paint)
        tmp.textSize = baseTextSize
        val textHeightBase = tmp.descent() - tmp.ascent()
        val maxHeight = height.toFloat() * 1.70f
        val scale = maxHeight / textHeightBase
        paint.textSize = baseTextSize * scale
        textWidth = paint.measureText(mensajeActual)
    }

    fun start() {
        stop()
        runnable = object : Runnable {
            override fun run() {
                posX -= velocidad
                if (posX + textWidth < 0) {
                    generarMensajeActual()
                    ajustarVelocidad()
                    ajustarTamanyo()
                    posX = width.toFloat()
                }
                invalidate()
                handler.postDelayed(this, 16L)
            }
        }
        handler.post(runnable!!)
    }

    fun stop() {
        runnable?.let { handler.removeCallbacks(it) }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawColor(Color.BLACK)
        if (mensajeActual.isEmpty()) return
        val yPos = height / 2f - (paint.descent() + paint.ascent()) / 2f
        canvas.drawText(mensajeActual, posX, yPos, paint)
    }

    companion object {
        fun capitalizeWords(input: String): String {
            return input.split(" ").joinToString(" ") {
                it.replaceFirstChar { c -> c.uppercaseChar() }
            }
        }
    }
}