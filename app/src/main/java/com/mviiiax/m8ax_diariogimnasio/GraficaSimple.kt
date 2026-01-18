package com.mviiiax.m8ax_diariogimnasio

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.View

class GraficaSimple(context: Context, attrs: AttributeSet) : View(context, attrs) {
    private var puntos: List<Int> = emptyList()
    private val paintLinea = Paint().apply {
        color = Color.DKGRAY
        strokeWidth = 5f
        style = Paint.Style.STROKE
        isAntiAlias = true
        strokeJoin = Paint.Join.ROUND
    }
    private val paintMedia = Paint().apply {
        color = Color.BLUE
        strokeWidth = 4f
        style = Paint.Style.STROKE
        pathEffect = DashPathEffect(floatArrayOf(15f, 10f), 0f)
        isAntiAlias = true
    }
    private val paintGrid = Paint().apply {
        color = Color.GRAY
        strokeWidth = 2f
        style = Paint.Style.STROKE
    }
    private val paintPunto = Paint().apply {
        style = Paint.Style.FILL
        isAntiAlias = true
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
            paintTexto.textSize = 40f
            canvas.drawText(
                "... ||| ▶ Se Requieren Al Menos 2 Registros ◀ ||| ...",
                width / 2f,
                height / 2f,
                paintTexto
            )
            return
        }
        val margenL = 80f
        val margenR = 40f
        val margenT = 60f
        val margenB = 60f
        val anchoUsable = width - margenL - margenR
        val altoUsable = height - margenT - margenB
        val maxVal = (puntos.maxOrNull() ?: 100).coerceAtLeast(100).toFloat()
        val pasoX = anchoUsable / (puntos.size - 1)
        val mediaRealVal = puntos.average()
        val mediaRedondeada = Math.round(mediaRealVal).toInt()
        paintTexto.textAlign = Paint.Align.LEFT
        val labels = listOf(
            "Bajo" to "#660000",
            "Normal" to "#006600",
            "Intenso" to "#666600",
            "Top" to "#660066",
            "Media ➜ ${mediaRedondeada}m" to "#0000FF"
        )
        val espacioPorElemento = anchoUsable / labels.size
        labels.forEachIndexed { index, (txt, colorStr) ->
            val xElemento = margenL + (index * espacioPorElemento)
            paintPunto.color = Color.parseColor(colorStr)
            canvas.drawRect(
                xElemento, 5f, xElemento + 25f, 30f, paintPunto
            )
            paintTexto.color = Color.BLACK
            paintTexto.textSize = 24f
            canvas.drawText(txt, xElemento + 30f, 28f, paintTexto)
        }
        paintTexto.textSize = 22f
        for (i in 0..4) {
            val yGrid = height - margenB - (i * (altoUsable / 4))
            val valorEjeY = (maxVal / 4 * i).toInt()
            canvas.drawLine(margenL, yGrid, width - margenR, yGrid, paintGrid)
            canvas.drawText("$valorEjeY", 10f, yGrid + 10f, paintEjes)
        }
        val yMedia = height - margenB - (mediaRealVal.toFloat() / maxVal * altoUsable)
        canvas.drawLine(margenL, yMedia, width - margenR, yMedia, paintMedia)
        paintEjes.textAlign = Paint.Align.CENTER
        paintEjes.textSize = 28f
        canvas.drawText(
            "GRÁFICA DE GIMNASIO ➜ ( TIEMPO EN MINUTOS )",
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
            if (i == 0) {
                path.moveTo(x, y)
            } else {
                path.lineTo(x, y)
            }
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
            canvas.drawText("$valor", x - 15, y - 25, paintTexto)
        }
    }
}
