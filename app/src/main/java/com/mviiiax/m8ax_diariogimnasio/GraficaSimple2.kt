package com.mviiiax.m8ax_diariogimnasio

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.View
import java.text.SimpleDateFormat
import java.util.Locale

class GraficaSimple2(context: Context, attrs: AttributeSet) : View(context, attrs) {
    var puntos: List<Int> = emptyList()
    var mediaCalculada: Float = 0f
    private val paintLinea = Paint().apply {
        color = Color.parseColor("#BDBDBD")
        strokeWidth = 4f
        style = Paint.Style.STROKE
        isAntiAlias = true
        strokeJoin = Paint.Join.ROUND
    }
    private val paintMedia = Paint().apply {
        color = Color.BLUE
        strokeWidth = 3f
        style = Paint.Style.STROKE
        isAntiAlias = true
        pathEffect = DashPathEffect(floatArrayOf(10f, 10f), 0f)
    }
    private val paintPunto = Paint().apply {
        style = Paint.Style.FILL
        isAntiAlias = true
    }
    private val paintGrid = Paint().apply {
        color = Color.parseColor("#BDBDBD")
        strokeWidth = 1.5f
        style = Paint.Style.STROKE
    }
    private val paintTexto = Paint().apply {
        textSize = 22f
        isFakeBoldText = true
        isAntiAlias = true
    }

    fun setData(nuevaData: List<Glucosa>) {
        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.US)
        val listaOrdenada = nuevaData.filter { it.valor > 0 }.sortedBy {
            try {
                sdf.parse(it.fechaHora.substring(0, 10))
            } catch (e: Exception) {
                null
            }
        }.takeLast(30)
        puntos = listaOrdenada.map { it.valor }
        mediaCalculada = if (puntos.isNotEmpty()) puntos.average().toFloat() else 0f
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (puntos.size < 2) {
            paintTexto.color = Color.GRAY
            paintTexto.textAlign = Paint.Align.CENTER
            paintTexto.textSize = 45f
            canvas.drawText(
                "... Se Requieren Al Menos 2 Mediciones ...", width / 2f, height / 2f, paintTexto
            )
            return
        }
        val margen = 70f
        val anchoUsable = width - (margen * 2)
        val altoUsable = height - (margen * 3)
        val maxVal = (puntos.maxOrNull() ?: 100).coerceAtLeast(160).toFloat()
        val pasoX = anchoUsable / (puntos.size - 1)
        paintTexto.textAlign = Paint.Align.LEFT
        val labels = listOf(
            "Bajo" to "#FF0000",
            "Normal" to "#006400",
            "Pre-D" to "#FFA500",
            "Alto" to "#FF0000",
            "Media ➜ ${String.format("%.1f", mediaCalculada)}" to "#0000FF"
        )
        var xLeyenda = margen
        labels.forEach { (txt, colorStr) ->
            paintPunto.color = Color.parseColor(colorStr)
            canvas.drawRect(xLeyenda, 10f, xLeyenda + 20f, 30f, paintPunto)
            paintTexto.color = Color.DKGRAY
            paintTexto.textSize = 24f
            canvas.drawText(txt, xLeyenda + 25f, 28f, paintTexto)
            xLeyenda += (anchoUsable / 5) + 8f
        }
        paintTexto.textSize = 20f
        paintTexto.color = Color.GRAY
        paintTexto.textAlign = Paint.Align.RIGHT
        for (i in 0..4) {
            val yGrid = height - margen - (i * (altoUsable / 4))
            val valorEje = (maxVal / 4 * i).toInt()
            canvas.drawText("$valorEje", margen - 10f, yGrid + 8f, paintTexto)
            canvas.drawLine(margen, yGrid, width - margen, yGrid, paintGrid)
        }
        if (mediaCalculada > 0) {
            val yMedia = height - margen - (mediaCalculada / maxVal * altoUsable)
            canvas.drawLine(margen, yMedia, width - margen, yMedia, paintMedia)
        }
        val path = Path()
        val coordenadasPuntos = mutableListOf<Pair<Float, Float>>()
        puntos.forEachIndexed { i, valor ->
            val x = margen + (i * pasoX)
            val y = height - margen - (valor / maxVal * altoUsable)
            coordenadasPuntos.add(Pair(x, y))
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        canvas.drawPath(path, paintLinea)
        paintTexto.textAlign = Paint.Align.LEFT
        puntos.forEachIndexed { i, valor ->
            val (x, y) = coordenadasPuntos[i]
            val colorMedicion = when {
                valor < 70 -> Color.parseColor("#FF0000")
                valor in 70..99 -> Color.parseColor("#006400")
                valor in 100..125 -> Color.parseColor("#FFA500")
                else -> Color.parseColor("#FF0000")
            }
            paintPunto.color = colorMedicion
            paintTexto.color = colorMedicion
            paintTexto.textSize = 22f
            canvas.drawCircle(x, y, 9f, paintPunto)
            canvas.drawText("$valor", x - 15f, y - 25f, paintTexto)
        }
    }
}