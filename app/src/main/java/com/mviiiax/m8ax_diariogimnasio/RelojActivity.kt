package com.mviiiax.m8ax_diariogimnasio

import android.content.pm.ActivityInfo
import android.media.MediaPlayer
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.speech.tts.TextToSpeech
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.widget.Button
import android.widget.Chronometer
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import java.util.Calendar
import java.util.Locale

class RelojActivity : AppCompatActivity() {
    private lateinit var cronometro: Chronometer
    private lateinit var btnStart: Button
    private lateinit var btnStop: Button
    private lateinit var btnReset: Button
    private var mediaPlayer: MediaPlayer? = null
    private lateinit var rootLayout: FrameLayout
    private lateinit var scaleDetector: ScaleGestureDetector
    private var scaleFactor = 1.0f
    private var lastX = 0f
    private var lastY = 0f
    private var translationX = 0f
    private var translationY = 0f
    private var tts: TextToSpeech? = null
    private var ttsEnabled: Boolean = false
    private var tiempoDetenido: Long = 0L
    private val handler = Handler(Looper.getMainLooper())
    private var wakeLock: android.os.PowerManager.WakeLock? = null
    private val cronometroRunnable = object : Runnable {
        override fun run() {
            if (!ttsEnabled) return
            val elapsedMillis = SystemClock.elapsedRealtime() - cronometro.base
            val totalMinutes = (elapsedMillis / 1000 / 60).toInt()
            if (totalMinutes > 0 && totalMinutes % 5 == 0) {
                val hours = totalMinutes / 60
                val minutes = totalMinutes % 60
                val parts = mutableListOf<String>()
                if (hours > 0) parts.add("$hours Hora${if (hours > 1) "s" else ""}")
                if (minutes > 0) parts.add("$minutes Minuto${if (minutes > 1) "s" else ""}")
                tts?.speak(
                    "El Cronómetro Lleva ${parts.joinToString(" Y ")}",
                    TextToSpeech.QUEUE_FLUSH,
                    null,
                    "cronometroId"
                )
            }
            handler.postDelayed(this, 60_000)
        }
    }
    private val horaRunnable = object : Runnable {
        override fun run() {
            if (!ttsEnabled || tts == null) {
                handler.postDelayed(this, 60_000)
                return
            }
            val calendar = Calendar.getInstance()
            val minute = calendar.get(Calendar.MINUTE)
            val hour = calendar.get(Calendar.HOUR_OF_DAY)
            if (minute == 0) {
                tts?.speak("Son Las $hour En Punto", TextToSpeech.QUEUE_FLUSH, null, "horaId")
            } else if (minute % 15 == 0) {
                tts?.speak(
                    "Son Las $hour Y $minute Minutos", TextToSpeech.QUEUE_FLUSH, null, "horaId"
                )
            }
            handler.postDelayed(this, 60_000)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        setContentView(R.layout.activity_reloj)
        window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        val config = getSharedPreferences("M8AX-Config_TTS", MODE_PRIVATE)
        ttsEnabled = config.getBoolean("tts_enabled", false)
        if (ttsEnabled) {
            tts = TextToSpeech(this) { status ->
                if (status == TextToSpeech.SUCCESS) {
                    tts?.setLanguage(tts?.defaultLanguage ?: Locale.getDefault())
                    tts?.setSpeechRate(0.9f)
                    val calendar = Calendar.getInstance()
                    val hour = calendar.get(Calendar.HOUR_OF_DAY)
                    val minute = calendar.get(Calendar.MINUTE)
                    tts?.speak(
                        "Son Las $hour Y $minute Minutos",
                        TextToSpeech.QUEUE_FLUSH,
                        null,
                        "horaInicial"
                    )
                    handler.post(horaRunnable)
                }
            }
        }
        rootLayout = findViewById(R.id.rootLayout)
        scaleDetector = ScaleGestureDetector(
            this, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
                override fun onScale(detector: ScaleGestureDetector): Boolean {
                    scaleFactor *= detector.scaleFactor
                    scaleFactor = scaleFactor.coerceIn(0.5f, 3.0f)
                    rootLayout.scaleX = scaleFactor
                    rootLayout.scaleY = scaleFactor
                    return true
                }
            })
        mediaPlayer = MediaPlayer.create(this, R.raw.m8axsonidofondo)
        mediaPlayer?.isLooping = true
        mediaPlayer?.setVolume(0.5f, 0.5f)
        mediaPlayer?.start()
        cronometro = findViewById(R.id.cronometro)
        btnStart = findViewById(R.id.btnStart)
        btnStop = findViewById(R.id.btnStop)
        btnReset = findViewById(R.id.btnReset)
        btnStart.setOnClickListener {
            handler.removeCallbacks(cronometroRunnable)
            val pm =
                getSystemService(android.content.Context.POWER_SERVICE) as android.os.PowerManager
            wakeLock = pm.newWakeLock(android.os.PowerManager.PARTIAL_WAKE_LOCK, "M8AX:RelojLock")
            wakeLock?.acquire(120 * 60 * 1000L)
            cronometro.base = SystemClock.elapsedRealtime() - tiempoDetenido
            cronometro.start()
            if (ttsEnabled) tts?.speak(
                "Cronómetro Iniciado", TextToSpeech.QUEUE_FLUSH, null, "btnStartId"
            )
            handler.postDelayed(cronometroRunnable, 60_000)
        }
        btnStop.setOnClickListener {
            handler.removeCallbacks(cronometroRunnable)
            if (wakeLock?.isHeld == true) wakeLock?.release()
            cronometro.stop()
            tiempoDetenido = SystemClock.elapsedRealtime() - cronometro.base
            if (ttsEnabled) tts?.speak(
                "Cronómetro Parado", TextToSpeech.QUEUE_FLUSH, null, "btnStopId"
            )
        }
        btnReset.setOnClickListener {
            handler.removeCallbacks(cronometroRunnable)
            if (wakeLock?.isHeld == true) wakeLock?.release()
            cronometro.base = SystemClock.elapsedRealtime()
            tiempoDetenido = 0L
            if (ttsEnabled) tts?.speak(
                "Cronómetro Reiniciado", TextToSpeech.QUEUE_FLUSH, null, "btnResetId"
            )
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        scaleDetector.onTouchEvent(event)
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                lastX = event.rawX - translationX
                lastY = event.rawY - translationY
            }

            MotionEvent.ACTION_MOVE -> {
                translationX = event.rawX - lastX
                translationY = event.rawY - lastY
                rootLayout.translationX = translationX
                rootLayout.translationY = translationY
            }
        }
        return true
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
        handler.removeCallbacks(cronometroRunnable)
        handler.removeCallbacks(horaRunnable)
        if (wakeLock?.isHeld == true) wakeLock?.release()
        mediaPlayer?.apply {
            if (isPlaying) stop()
            release()
        }
        mediaPlayer = null
        tts?.shutdown()
    }
}