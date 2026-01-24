package com.mviiiax.m8ax_diariogimnasio

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.random.Random

class CustomAnalogClock @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {
    private val paintCircle = Paint().apply {
        color = Color.WHITE; style = Paint.Style.STROKE; strokeWidth = 8f; isAntiAlias = true
    }
    private val paintMark =
        Paint().apply { color = Color.WHITE; strokeWidth = 4f; isAntiAlias = true }
    private val pintarAgujaHora = Paint().apply {
        color = Color.WHITE; strokeWidth = 14f; strokeCap = Paint.Cap.ROUND; isAntiAlias = true
    }
    private val pintarAgujaMinuto = Paint().apply {
        color = Color.WHITE; strokeWidth = 10f; strokeCap = Paint.Cap.ROUND; isAntiAlias = true
    }
    private val pintarAgujaSegundos =
        Paint().apply { strokeWidth = 4f; strokeCap = Paint.Cap.ROUND; isAntiAlias = true }
    private val paintText = Paint().apply {
        color = Color.WHITE; textAlign = Paint.Align.CENTER; isFakeBoldText = true; isAntiAlias =
        true
    }
    private val paintDate = Paint().apply {
        color = Color.WHITE; textSize = 68f; textAlign = Paint.Align.CENTER; isAntiAlias = true
    }
    private val paintFirma = Paint().apply {
        color = Color.WHITE; textSize = 58f; textAlign = Paint.Align.CENTER; isAntiAlias = true
    }
    private val paintFirma1 = Paint().apply {
        color = Color.WHITE; textSize = 100f; textAlign = Paint.Align.CENTER; isAntiAlias = true
    }
    private val paintCheck = Paint().apply { strokeWidth = 4f; isAntiAlias = true }
    private val paintLabel =
        Paint().apply { color = Color.WHITE; textSize = 40f; isAntiAlias = true }
    private var scaleFactor = 1.0f
    var modoSegundero = 1
    private var ultimoSegundo = -1
    private val rectCheck = RectF()
    private val scaleDetector =
        ScaleGestureDetector(context, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
            override fun onScale(d: ScaleGestureDetector): Boolean {
                scaleFactor *= d.scaleFactor; scaleFactor =
                    scaleFactor.coerceIn(0.5f, 3.0f); invalidate(); return true
            }
        })

    override fun onDraw(canvas: Canvas) {
        canvas.save()
        val cx = width / 2f;
        val cy = height / 2f + 80f
        canvas.scale(scaleFactor, scaleFactor, cx, cy)
        val radius = (Math.min(width, height) / 2f) - 40f
        canvas.drawCircle(cx, cy, radius, paintCircle)
        pintarMarcas(canvas, cx, cy, radius)
        paintText.textSize = radius * 0.15f
        for (i in 1..12) {
            val angle = Math.toRadians((i * 30 - 90).toDouble())
            val x = cx + (radius * 0.82f * Math.cos(angle)).toFloat()
            val y = cy + (radius * 0.82f * Math.sin(angle)).toFloat() + (paintText.textSize / 3)
            canvas.drawText(i.toString(), x, y, paintText)
        }
        val cal = Calendar.getInstance()
        val h = cal.get(Calendar.HOUR);
        val m = cal.get(Calendar.MINUTE);
        val s = cal.get(Calendar.SECOND);
        val ms = cal.get(Calendar.MILLISECOND)
        if (s != ultimoSegundo) {
            paintFirma.color =
                Color.rgb(Random.nextInt(256), Random.nextInt(256), Random.nextInt(256))
            ultimoSegundo = s
        }
        val hA = (h + m / 60f) * 30f;
        val mA = (m + s / 60f) * 6f;
        val sA: Float
        if (modoSegundero == 1) {
            pintarAgujaSegundos.color = Color.GREEN; sA = (s + ms / 1000f) * 6f
        } else {
            pintarAgujaSegundos.color = Color.RED; sA = s * 6f
        }
        dibujarAguja(canvas, cx, cy, radius * 0.50f, hA, pintarAgujaHora)
        dibujarAguja(canvas, cx, cy, radius * 0.70f, mA, pintarAgujaMinuto)
        dibujarAguja(canvas, cx, cy, radius * 0.85f, sA, pintarAgujaSegundos)
        val f = SimpleDateFormat("EEEE dd/MM/yyyy", Locale("es", "ES")).format(Date())
            .replaceFirstChar { it.uppercase() }
        canvas.drawText(f, cx, cy - radius - 320f, paintDate)
        val cX = cx - 200f;
        val cY = cy - radius - 250f
        rectCheck.set(cX, cY - 35f, cX + 45f, cY + 10f)
        paintCheck.color = if (modoSegundero == 1) Color.GREEN else Color.RED
        paintCheck.style = if (modoSegundero == 1) Paint.Style.FILL else Paint.Style.STROKE
        canvas.drawRect(rectCheck, paintCheck)
        canvas.drawText("MODO CONTINUO", cX + 65f, cY - 10f, paintLabel)
        canvas.drawText(
            "PULSA PARA CAMBIAR", cX + 65f, cY + 30f, paintLabel.apply { textSize = 25f })
        canvas.drawText("By M8AX", cx, cy + radius - 140f, paintFirma)
        canvas.drawText("--- CRONÓMETRO ---", cx, cy + radius + 230f, paintFirma1)
        canvas.restore()
        if (modoSegundero == 1) postInvalidateOnAnimation() else postInvalidateDelayed(1000)
    }

    override fun onTouchEvent(e: MotionEvent): Boolean {
        scaleDetector.onTouchEvent(e)
        if (e.action == MotionEvent.ACTION_DOWN) {
            val m = android.graphics.Matrix();
            val cx = width / 2f;
            val cy = height / 2f + 80f
            m.postScale(1 / scaleFactor, 1 / scaleFactor, cx, cy)
            val pts = floatArrayOf(e.x, e.y); m.mapPoints(pts)
            if (rectCheck.contains(pts[0], pts[1])) {
                modoSegundero = if (modoSegundero == 1) 0 else 1; invalidate(); return true
            }
        }
        return true
    }

    private fun pintarMarcas(c: Canvas, cx: Float, cy: Float, r: Float) {
        for (i in 0 until 60) {
            val a = Math.toRadians((i * 6).toDouble())
            val st = if (i % 5 == 0) r * 0.88f else r * 0.94f
            val sX = cx + (st * Math.cos(a)).toFloat();
            val sY = cy + (st * Math.sin(a)).toFloat()
            val eX = cx + (r * Math.cos(a)).toFloat();
            val eY = cy + (r * Math.sin(a)).toFloat()
            paintMark.strokeWidth = if (i % 5 == 0) 6f else 3f
            c.drawLine(sX, sY, eX, eY, paintMark)
        }
    }

    private fun dibujarAguja(c: Canvas, cx: Float, cy: Float, l: Float, aD: Float, p: Paint) {
        val a = Math.toRadians(aD - 90.0)
        c.drawLine(cx, cy, cx + l * Math.cos(a).toFloat(), cy + l * Math.sin(a).toFloat(), p)
    }
}