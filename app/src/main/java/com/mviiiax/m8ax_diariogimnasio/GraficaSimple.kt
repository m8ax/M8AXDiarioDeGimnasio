package com.mviiiax.m8ax_diariogimnasio

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.View

class GraficaSimple(context: Context, attrs: AttributeSet) : View(context, attrs) {
    private var puntos: List<Int> = emptyList()
    private val paintLinea = Paint().apply {
        color = Color.parseColor("#BDBDBD")
        strokeWidth = 4f
        style = Paint.Style.STROKE
        isAntiAlias = true
        strokeJoin = Paint.Join.ROUND
    }
    private val paintPunto = Paint().apply {
        style = Paint.Style.FILL
        isAntiAlias = true
    }
    private val paintGrid = Paint().apply {
        color = Color.parseColor("#E0E0E0")
        strokeWidth = 1.5f
        style = Paint.Style.STROKE
    }
    private val paintTexto = Paint().apply {
        textSize = 22f
        isFakeBoldText = true
        isAntiAlias = true
    }
    private val paintEjes = Paint().apply {
        color = Color.BLACK
        textSize = 18f
        isAntiAlias = true
    }

    fun setData(nuevaData: List<Int>) {
        puntos = nuevaData.takeLast(30)
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (puntos.size < 2) {
            paintTexto.color = Color.GRAY
            paintTexto.textAlign = Paint.Align.CENTER
            canvas.drawText(
                "... Se Requieren Al Menos 2 Registros ...", width / 2f, height / 2f, paintTexto
            )
            return
        }
        val margenL = 80f
        val margenR = 40f
        val margenT = 40f
        val margenB = 60f
        val anchoUsable = width - margenL - margenR
        val altoUsable = height - margenT - margenB
        val maxVal = (puntos.maxOrNull() ?: 100).coerceAtLeast(100).toFloat()
        val pasoX = anchoUsable / (puntos.size - 1)
        for (i in 0..4) {
            val yGrid = height - margenB - (i * (altoUsable / 4))
            val valorEjeY = (maxVal / 4 * i).toInt()
            canvas.drawLine(margenL, yGrid, width - margenR, yGrid, paintGrid)
            canvas.drawText("$valorEjeY", 10f, yGrid + 10f, paintEjes)
        }
        paintEjes.textAlign = Paint.Align.CENTER
        canvas.drawText(
            "GRÁFICA DE GIMNASIO - TIEMPO ( EN MINUTOS )",
            margenL + (anchoUsable / 2f),
            height - 10f,
            paintEjes
        )
        paintEjes.textAlign = Paint.Align.LEFT
        val path = Path()
        val coordenadasPuntos = mutableListOf<Pair<Float, Float>>()
        puntos.forEachIndexed { i, valor ->
            val x = margenL + (i * pasoX)
            val y = height - margenB - (valor.toFloat() / maxVal * altoUsable)
            coordenadasPuntos.add(Pair(x, y))
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        canvas.drawPath(path, paintLinea)
        puntos.forEachIndexed { i, valor ->
            val (x, y) = coordenadasPuntos[i]
            val v = valor.toDouble()
            val colorMedicion = when {
                v == 0.0 -> Color.BLACK
                v < 45.0 -> Color.parseColor("#660000")
                v < 61.0 -> Color.parseColor("#006600")
                v < 91.0 -> Color.parseColor("#666600")
                else -> Color.parseColor("#660066")
            }
            paintPunto.color = colorMedicion
            paintTexto.color = colorMedicion
            canvas.drawCircle(x, y, 9f, paintPunto)
            if (puntos.size <= 15 || i % 2 == 0) {
                canvas.drawText("$valor", x - 15, y - 25, paintTexto)
            }
        }
    }
}