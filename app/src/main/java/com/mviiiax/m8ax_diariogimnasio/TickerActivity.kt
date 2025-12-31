package com.mviiiax.m8ax_diariogimnasio

import android.app.Activity
import android.content.pm.ActivityInfo
import android.media.MediaPlayer
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.view.MotionEvent
import android.view.WindowManager
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class TickerActivity : Activity() {
    private lateinit var tickerView: TickerView
    private var tts: TextToSpeech? = null
    private var ttsEnabled = true
    private val handler = Handler(Looper.getMainLooper())
    private var ttsRunnable: Runnable? = null
    private var mediaPlayer: MediaPlayer? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        val config = getSharedPreferences("M8AX-Config_TTS", MODE_PRIVATE)
        ttsEnabled = config.getBoolean("tts_enabled", true)
        tickerView = TickerView(this)
        setContentView(tickerView)
        mediaPlayer = MediaPlayer.create(this, R.raw.m8axsonidofondo)
        mediaPlayer?.isLooping = true
        mediaPlayer?.setVolume(0.7f, 0.7f)
        mediaPlayer?.start()
        val mensajeOriginal = intent.getStringExtra("mensaje_ticker")?.take(1000) ?: ""
        val mensajeCapitalizado = TickerView.capitalizeWords(mensajeOriginal)
        tickerView.setMensaje(mensajeCapitalizado)
        tickerView.start()
        inicializarTTS { startTTSLoop(mensajeCapitalizado) }
    }

    private fun inicializarTTS(onReady: () -> Unit) {
        tts = TextToSpeech(this) { estado ->
            if (estado == TextToSpeech.SUCCESS) {
                tts?.language = Locale("es", "ES")
                tts?.setSpeechRate(0.9f)
                onReady()
            }
        }
    }

    private fun startTTSLoop(text: String) {
        ttsRunnable?.let { handler.removeCallbacks(it) }
        val tiempoSeg = (text.length * 0.06).toLong()
        val intervalo = maxOf(tiempoSeg * 1000, 90_000)
        ttsRunnable = object : Runnable {
            override fun run() {
                if (ttsEnabled) {
                    tts?.stop()
                    tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "TTS_MSG")
                }
                handler.postDelayed(this, intervalo)
            }
        }
        handler.postDelayed(ttsRunnable!!, 1000)
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        if (event?.action == MotionEvent.ACTION_DOWN && ttsEnabled) {
            tts?.stop()
            val ahora = Calendar.getInstance()
            val formatoDia = SimpleDateFormat("EEEE d 'de' MMMM 'de' yyyy", Locale("es", "ES"))
            val fechaStr = formatoDia.format(ahora.time)
            val hora = ahora.get(Calendar.HOUR_OF_DAY)
            val min = ahora.get(Calendar.MINUTE)
            val texto = "ES $fechaStr Y SON LAS $hora HORAS Y $min MINUTOS."
            tts?.speak(texto, TextToSpeech.QUEUE_FLUSH, null, "TTS_FECHA")
        }
        return super.onTouchEvent(event)
    }

    override fun onPause() {
        super.onPause()
        mediaPlayer?.pause()
    }

    override fun onResume() {
        super.onResume()
        mediaPlayer?.start()
    }

    override fun onDestroy() {
        super.onDestroy()
        tickerView.stop()
        ttsRunnable?.let { handler.removeCallbacks(it) }
        mediaPlayer?.stop()
        mediaPlayer?.release()
        tts?.stop()
        tts?.shutdown()
    }
}