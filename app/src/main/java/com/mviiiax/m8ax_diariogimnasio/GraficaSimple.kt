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
    private var mediaExterna: Double = 0.0
    private val paintLinea = Paint().apply {
        color = Color.DKGRAY
        strokeWidth = 4f
        style = Paint.Style.STROKE
        isAntiAlias = true
        strokeJoin = Paint.Join.ROUND
    }
    private val paintMedia = Paint().apply {
        color = Color.BLUE
        strokeWidth = 3f
        style = Paint.Style.STROKE
        pathEffect = DashPathEffect(floatArrayOf(15f, 10f), 0f)
        isAntiAlias = true
    }
    private val paintGrid = Paint().apply {
        color = Color.GRAY
        strokeWidth = 1.5f
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

    fun setData(nuevaData: List<Int>, mediaReal: Double) {
        puntos = nuevaData.takeLast(30)
        mediaExterna = mediaReal
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
        val margenL = 85f
        val margenR = 45f
        val margenT = 100f
        val margenB = 70f
        val anchoUsable = width - margenL - margenR
        val altoUsable = height - margenT - margenB
        val maxVal = ((puntos.maxOrNull() ?: 100) * 1.15f).coerceAtLeast(100f)
        val pasoX = anchoUsable / (puntos.size - 1)
        val mediaRedondeada = Math.round(mediaExterna).toInt()
        paintTexto.textAlign = Paint.Align.LEFT
        val labels = listOf(
            "Bajo" to "#660000",
            "Normal" to "#006600",
            "Intenso" to "#666600",
            "Top" to "#660066",
            "Media ➜ ${mediaRedondeada}m" to "#0000FF"
        )
        var xLeyenda = margenL
        val separacion = anchoUsable / 5f
        labels.forEach { (txt, colorStr) ->
            paintPunto.color = Color.parseColor(colorStr)
            canvas.drawRect(xLeyenda, 10f, xLeyenda + 20f, 30f, paintPunto)
            paintTexto.color = Color.DKGRAY
            paintTexto.textSize = 22f
            canvas.drawText(txt, xLeyenda + 25f, 28f, paintTexto)
            xLeyenda += separacion
        }
        paintEjes.textAlign = Paint.Align.RIGHT
        for (i in 0..4) {
            val yGrid = height - margenB - (i * (altoUsable / 4))
            canvas.drawLine(margenL, yGrid, width - margenR, yGrid, paintGrid)
            canvas.drawText("${(maxVal / 4 * i).toInt()}", margenL - 10f, yGrid + 8f, paintEjes)
        }
        val yMedia = height - margenB - (mediaExterna.toFloat() / maxVal * altoUsable)
        canvas.drawLine(margenL, yMedia, width - margenR, yMedia, paintMedia)
        paintEjes.textAlign = Paint.Align.CENTER
        paintEjes.textSize = 26f
        canvas.drawText(
            "GRÁFICA DE GIMNASIO ➜ ( TIEMPO EN MINUTOS )",
            margenL + (anchoUsable / 2f),
            height - 15f,
            paintEjes
        )
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
            val colorMedicion = when {
                valor < 45 -> Color.parseColor("#660000")
                valor < 61 -> Color.parseColor("#006600")
                valor < 91 -> Color.parseColor("#666600")
                else -> Color.parseColor("#660066")
            }
            paintPunto.color = colorMedicion
            paintTexto.color = colorMedicion
            paintTexto.textAlign = Paint.Align.CENTER
            canvas.drawCircle(x, y, 8f, paintPunto)
            val yOffset = if (i % 2 == 0) -12f else -30f
            canvas.drawText("$valor", x, y + yOffset, paintTexto)
        }
    }
}