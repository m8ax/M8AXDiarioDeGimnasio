package com.mviiiax.m8ax_diariogimnasio

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.audiofx.Visualizer
import android.util.AttributeSet
import android.view.View
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

class OndaView(context: Context, attrs: AttributeSet) : View(context, attrs) {
    private val paint = Paint().apply {
        color = Color.CYAN
        isAntiAlias = true
    }
    private val colores = arrayOf(
        Color.RED,
        Color.GREEN,
        Color.BLUE,
        Color.YELLOW,
        Color.MAGENTA,
        Color.CYAN,
        Color.WHITE,
        Color.LTGRAY,
        Color.parseColor("#00FFAA"),
        Color.parseColor("#FF8800"),
        Color.parseColor("#AA00FF"),
        Color.parseColor("#00FFA5"),
        Color.parseColor("#FF44AA"),
        Color.parseColor("#33DDFF"),
        Color.parseColor("#55FF00"),
        Color.parseColor("#FFDD00"),
        Color.parseColor("#FF0066"),
        Color.parseColor("#00FF66"),
        Color.parseColor("#3399FF"),
        Color.parseColor("#9933FF")
    )
    private var amplitudes = FloatArray(32) { 0f }
    private var targetAmplitudes = FloatArray(32) { 0f }
    private var coloresBarras = IntArray(32) { colores.random() }
    private var modoColor = 1
    private var colorUnico = Color.CYAN
    private var mediaPlayer: MediaPlayer? = null
    private var visualizer: Visualizer? = null
    private var animando = false
    private var visualizerDisponible = true
    private var mediaPlayerPreparado = false
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    fun iniciarAnimacion(mp: MediaPlayer) {
        if (mp.audioSessionId == 0 || !mp.isPlaying) return
        mediaPlayer = mp
        mediaPlayerPreparado = true
        animando = true
        modoColor = Random.nextInt(0, 2)
        if (modoColor == 0) colorUnico = colores.random()
        else for (i in coloresBarras.indices) coloresBarras[i] = colores.random()
        setupVisualizer()
        post(animRunnable)
    }

    private fun generarAnimacionFallback() {
        val alto = height.toFloat()
        for (i in targetAmplitudes.indices) {
            targetAmplitudes[i] = Random.nextFloat() * alto * 0.6f
        }
    }

    private fun setupVisualizer() {
        val sessionId = mediaPlayer?.audioSessionId ?: 0
        if (sessionId == 0) {
            visualizerDisponible = false
            return
        }
        try {
            visualizer?.release()
            visualizer = Visualizer(sessionId).apply {
                captureSize = Visualizer.getCaptureSizeRange()[1]
                setDataCaptureListener(
                    object : Visualizer.OnDataCaptureListener {
                        override fun onWaveFormDataCapture(
                            visualizer: Visualizer?, waveform: ByteArray?, samplingRate: Int
                        ) {
                        }

                        override fun onFftDataCapture(
                            visualizer: Visualizer?, fft: ByteArray?, samplingRate: Int
                        ) {
                            fft?.let { mapFFTtoBars(it) }
                        }
                    }, Visualizer.getMaxCaptureRate() / 2, false, true
                )
                enabled = true
            }
            visualizerDisponible = true
        } catch (e: Exception) {
            visualizerDisponible = false
            visualizer?.release()
            visualizer = null
        }
    }

    private fun mapFFTtoBars(fft: ByteArray) {
        if (fft.size < 2) return
        val barras = amplitudes.size
        val alto = height.toFloat()
        val n = fft.size / 2
        val binPorBarra = max(1, n / barras)
        val volumenActual = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        val volumenMax = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        val factorVolumen = volumenActual.toFloat() / volumenMax
        for (i in 0 until barras) {
            var sum = 0f
            val start = i * binPorBarra
            val end = min(start + binPorBarra, n)
            for (j in start until end) {
                val re = fft[2 * j].toInt()
                val im = fft[2 * j + 1].toInt()
                val mag = re * re + im * im
                sum += mag.toFloat()
            }
            val avg = sum / (end - start)
            val magnitude = log10(1 + avg)
            val boost = when {
                i <= 7 -> 1f
                i <= 23 -> 1.3f
                else -> 1.5f
            }
            targetAmplitudes[i] =
                min(alto, (magnitude / 5f * alto) * (1f + factorVolumen * 0.5f) * boost)
        }
    }

    fun detenerAnimacion() {
        animando = false
        mediaPlayerPreparado = false
        amplitudes.fill(0f)
        targetAmplitudes.fill(0f)
        invalidate()
        removeCallbacks(animRunnable)
        try {
            visualizer?.release()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        visualizer = null
    }

    private val animRunnable = object : Runnable {
        override fun run() {
            if (!animando || !mediaPlayerPreparado) return
            if (!visualizerDisponible) {
                generarAnimacionFallback()
            }
            for (i in amplitudes.indices) {
                val target = targetAmplitudes[i]
                amplitudes[i] += (target - amplitudes[i]) * 0.5f
                amplitudes[i] = max(0f, min(amplitudes[i], height.toFloat()))
            }
            invalidate()
            postDelayed(this, 16)
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        detenerAnimacion()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val ancho = width.toFloat()
        val alto = height.toFloat()
        val barraAncho = ancho / amplitudes.size
        for (i in amplitudes.indices) {
            val amp = amplitudes[i]
            val left = i * barraAncho
            val right = left + barraAncho * 0.7f
            val top = alto - amp
            paint.color = if (modoColor == 0) colorUnico else coloresBarras[i]
            canvas.drawRect(left, top, right, alto, paint)
        }
    }
}