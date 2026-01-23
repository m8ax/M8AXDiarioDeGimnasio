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
                sdf.parse(it.fechaHora.substring(0, 10))?.time ?: Long.MAX_VALUE
            } catch (e: Exception) {
                Long.MAX_VALUE
            }
        }
        puntos = listaOrdenada.map { it.valor }
        mediaCalculada = if (puntos.isNotEmpty()) puntos.average().toFloat() else 0f
        postInvalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (puntos.size < 2) {
            paintTexto.color = Color.GRAY
            paintTexto.textAlign = Paint.Align.CENTER
            paintTexto.textSize = 35f
            canvas.drawText(
                "... ||| ▶ Se Requieren Al Menos 2 Mediciones ◀ ||| ...",
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
        val maxVal = (puntos.maxOrNull() ?: 100).coerceAtLeast(160).toFloat() * 1.2f
        val pasoX = anchoUsable / (puntos.size - 1)
        paintTexto.textAlign = Paint.Align.LEFT
        val labels = listOf(
            "Bajo" to "#FF0000",
            "Normal" to "#006400",
            "Pre-D" to "#FFA500",
            "Alto" to "#FF0000",
            "Media ➜ ${String.format("%.1f", mediaCalculada)}" to "#0000FF"
        )
        var xLey = margenL
        val sep = (anchoUsable + margenR) / 5.5f
        labels.forEach { (txt, col) ->
            paintPunto.color = Color.parseColor(col)
            canvas.drawRect(xLey, 15f, xLey + 20f, 35f, paintPunto)
            paintTexto.color = Color.DKGRAY
            paintTexto.textSize = 18f
            canvas.drawText(txt, xLey + 25f, 33f, paintTexto)
            xLey += sep
        }
        paintTexto.textAlign = Paint.Align.RIGHT
        paintTexto.color = Color.GRAY
        for (i in 0..4) {
            val yG = height - margenB - (i * (altoUsable / 4))
            canvas.drawText("${(maxVal / 4 * i).toInt()}", margenL - 10f, yG + 8f, paintTexto)
            canvas.drawLine(margenL, yG, width - margenR, yG, paintGrid)
        }
        if (mediaCalculada > 0) {
            val yM = height - margenB - (mediaCalculada / maxVal * altoUsable)
            canvas.drawLine(margenL, yM, width - margenR, yM, paintMedia)
        }
        val path = Path()
        val coords = mutableListOf<Pair<Float, Float>>()
        puntos.forEachIndexed { i, v ->
            val x = margenL + (i * pasoX)
            val y = height - margenB - (v / maxVal * altoUsable)
            coords.add(Pair(x, y))
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        canvas.drawPath(path, paintLinea)
        puntos.forEachIndexed { i, valor ->
            val (x, y) = coords[i]
            paintPunto.color = when {
                valor < 70 -> Color.parseColor("#FF0000")
                valor in 70..99 -> Color.parseColor("#006400")
                valor in 100..125 -> Color.parseColor("#FFA500")
                else -> Color.parseColor("#FF0000")
            }
            paintTexto.color = paintPunto.color
            paintTexto.textAlign = Paint.Align.CENTER
            paintTexto.textSize = 22f
            canvas.drawCircle(x, y, 8f, paintPunto)
            val yOff = if (i % 2 == 0) -12f else -30f
            canvas.drawText("$valor", x, y + yOff, paintTexto)
        }
        paintTexto.color = Color.BLACK
        paintTexto.textSize = 24f
        paintTexto.textAlign = Paint.Align.CENTER
        canvas.drawText(
            "GRÁFICA DE GLUCOSA ➜ ( NIVELES DE GLUCOSA )", width / 2f, height - 15f, paintTexto
        )
    }
}