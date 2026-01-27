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
        isAntiAlias = true
        isFakeBoldText = true
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
        val margenR = 80f
        val margenT = 100f
        val margenB = 70f
        val anchoUsable = width - margenL - margenR
        val altoUsable = height - margenT - margenB
        val maxBruto = (puntos.maxOrNull() ?: 100).toFloat() * 1.15f
        val maxVal = (Math.ceil(maxBruto.toDouble() / 40.0) * 40.0).toFloat().coerceAtLeast(100f)
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
        paintTexto.textSize = 33f
        paintTexto.textAlign = Paint.Align.LEFT
        val anchoTotal =
            labels.sumOf { 20.0 + 5.0 + paintTexto.measureText(it.first).toDouble() + 70.0 }
                .toFloat() - 70f
        var xLeyenda = (width - anchoTotal) / 2f
        labels.forEach { (txt, colorStr) ->
            paintPunto.color = Color.parseColor(colorStr)
            canvas.drawRect(xLeyenda, 15f, xLeyenda + 20f, 35f, paintPunto)
            paintTexto.color = Color.DKGRAY
            canvas.drawText(txt, xLeyenda + 25f, 33f, paintTexto)
            xLeyenda += 20f + 5f + paintTexto.measureText(txt) + 70f
        }
        paintEjes.textAlign = Paint.Align.RIGHT
        paintEjes.color = Color.GRAY
        paintEjes.textSize = 26f
        for (i in 0..4) {
            val yGrid = height - margenB - (i * (altoUsable / 4))
            canvas.drawLine(margenL, yGrid, width - margenR, yGrid, paintGrid)
            canvas.drawText("${(maxVal / 4 * i).toInt()}", margenL - 10f, yGrid + 8f, paintEjes)
        }
        val yMedia = height - margenB - (mediaExterna.toFloat() / maxVal * altoUsable)
        canvas.drawLine(margenL, yMedia, width - margenR, yMedia, paintMedia)
        paintTexto.color = Color.rgb(0, 0, 139)
        paintTexto.textSize = 24f
        paintTexto.isFakeBoldText = true
        paintTexto.textAlign = Paint.Align.CENTER
        canvas.drawText(
            "GRÁFICA DE GIMNASIO ➜ ( TIEMPO EN MINUTOS )", width / 2f, height - 15f, paintTexto
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
            paintTexto.textSize = 22f
            canvas.drawCircle(x, y, 8f, paintPunto)
            val yOffset = if (i % 2 == 0) -12f else -30f
            canvas.drawText("$valor", x, y + yOffset, paintTexto)
        }
    }
}