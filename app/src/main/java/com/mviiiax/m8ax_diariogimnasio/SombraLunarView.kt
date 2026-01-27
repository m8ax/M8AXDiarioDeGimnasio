package com.mviiiax.m8ax_diariogimnasio

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.util.AttributeSet
import android.view.View

class SombraLunarView(context: Context, attrs: AttributeSet?) : View(context, attrs) {
    private val paintLuz = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = 0x70FFFFFF.toInt()
        xfermode = PorterDuffXfermode(PorterDuff.Mode.SCREEN)
    }
    private val paintSombra = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = 0xAA000000.toInt()
    }
    private var fraccion: Double = 0.0
    private var faseReloj: Double = 0.0

    fun actualizar(frac: Double, phase: Double) {
        this.fraccion = frac
        this.faseReloj = phase
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0 || h <= 0) return
        val path = Path().apply { addCircle(w / 2, h / 2, w / 2, Path.Direction.CW) }
        canvas.clipPath(path)
        val anchoLuz = (w * fraccion).toFloat()
        val desdeDerecha = faseReloj < 0.5
        if (desdeDerecha) {
            canvas.drawRect(w - anchoLuz, 0f, w, h, paintLuz)
            canvas.drawRect(0f, 0f, w - anchoLuz, h, paintSombra)
        } else {
            canvas.drawRect(0f, 0f, anchoLuz, h, paintLuz)
            canvas.drawRect(anchoLuz, 0f, w, h, paintSombra)
        }
    }
}