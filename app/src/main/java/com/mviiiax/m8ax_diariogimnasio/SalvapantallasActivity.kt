package com.mviiiax.m8ax_diariogimnasio

import android.animation.AnimatorSet
import android.animation.ArgbEvaluator
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Context
import android.content.pm.ActivityInfo
import android.graphics.Color
import android.media.MediaPlayer
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.setPadding
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.random.Random

class SalvapantallasActivity : AppCompatActivity(), TextToSpeech.OnInitListener {
    private lateinit var container: LinearLayout
    private lateinit var relojTextView: TextView
    private lateinit var tickerText: TextView
    private lateinit var mainFrame: FrameLayout
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var mediaPlayer: MediaPlayer
    private lateinit var tts: TextToSpeech
    private var ttsEnabled = false
    private lateinit var scrollView: ScrollView
    private var scrollAnim: ValueAnimator? = null
    private var ultimoMinutoHablado = -1
    private val updateRelojRunnable = object : Runnable {
        override fun run() {
            val now = Date()
            val cal = Calendar.getInstance().apply { time = now }
            val diaSemana = when (cal.get(Calendar.DAY_OF_WEEK)) {
                Calendar.MONDAY -> "LUNES"
                Calendar.TUESDAY -> "MARTES"
                Calendar.WEDNESDAY -> "MIÉRCOLES"
                Calendar.THURSDAY -> "JUEVES"
                Calendar.FRIDAY -> "VIERNES"
                Calendar.SATURDAY -> "SÁBADO"
                Calendar.SUNDAY -> "DOMINGO"
                else -> ""
            }
            val fecha = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(now)
            val hora = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(now)
            relojTextView.text = "$diaSemana $fecha - $hora"
            val minute = cal.get(Calendar.MINUTE)
            if (ttsEnabled && minute != ultimoMinutoHablado && ::tts.isInitialized && !tts.isSpeaking) {
                ultimoMinutoHablado = minute
                tts.speak(
                    "Hoy Es $diaSemana; Fecha; $fecha; Y Son Las $hora",
                    TextToSpeech.QUEUE_FLUSH,
                    null,
                    "fechaHoraId"
                )
                if (minute % 5 == 0) {
                    leerRegistroAleatorio()
                }
            }
            handler.postDelayed(this, 1000)
        }
    }

    private fun iniciarScrollAutomatico(scrollView: ScrollView) {
        scrollView.viewTreeObserver.addOnGlobalLayoutListener(object :
            ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                scrollView.viewTreeObserver.removeOnGlobalLayoutListener(this)
                val contentHeight = scrollView.getChildAt(0).height
                val scrollViewHeight = scrollView.height
                val distancia = contentHeight - scrollViewHeight
                if (distancia <= 0) return
                val duracion = (distancia / 150f * 1000).toLong()
                scrollAnim = ValueAnimator.ofFloat(0f, distancia.toFloat()).apply {
                    duration = duracion
                    repeatMode = ValueAnimator.REVERSE
                    repeatCount = ValueAnimator.INFINITE
                    interpolator = android.view.animation.LinearInterpolator()
                    addUpdateListener { valueAnimator ->
                        scrollView.scrollTo(0, (valueAnimator.animatedValue as Float).toInt())
                    }
                    start()
                }
                scrollView.setOnTouchListener { _, _ ->
                    scrollAnim?.cancel()
                    false
                }
            }
        })
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val prefs = getSharedPreferences("M8AX-Config_TTS", Context.MODE_PRIVATE)
        ttsEnabled = prefs.getBoolean("tts_enabled", false)
        if (ttsEnabled) tts = TextToSpeech(this, this)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        mainFrame = FrameLayout(this)
        setContentView(mainFrame)
        scrollView = ScrollView(this)
        container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(30)
        }
        scrollView.addView(container)
        mainFrame.addView(scrollView)
        val bgAnim = ObjectAnimator.ofObject(
            mainFrame, "backgroundColor", ArgbEvaluator(), Color.BLACK, Color.DKGRAY, Color.BLACK
        )
        bgAnim.duration = 4000
        bgAnim.repeatCount = ValueAnimator.INFINITE
        bgAnim.start()
        relojTextView = TextView(this).apply {
            textSize = 36f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
        }
        container.addView(relojTextView)
        handler.post(updateRelojRunnable)
        tickerText = TextView(this).apply {
            textSize = 20f
            setTextColor(Color.CYAN)
            setPadding(0, 20, 0, 20)
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }
        container.addView(tickerText)
        mediaPlayer = MediaPlayer.create(this, R.raw.m8axsonidofondo)
        mediaPlayer.isLooping = true
        mediaPlayer.start()
        activarModoInmersivo()
        cargarRegistros()
    }

    private fun activarModoInmersivo() {
        window.decorView.systemUiVisibility =
            (View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_LAYOUT_STABLE)
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) activarModoInmersivo()
    }

    private fun cargarRegistros() {
        lifecycleScope.launch {
            val registros: List<Gimnasio> = withContext(Dispatchers.IO) {
                AppDatabase.getDatabase(this@SalvapantallasActivity).gimnasioDao().getAll()
            }.sortedByDescending { it.fechaHora }
            mainFrame.post {
                if (registros.size <= 1) {
                    container.removeAllViews()
                    mainFrame.setBackgroundColor(Color.BLACK)
                    container.addView(relojTextView)
                    mostrarLineasWindowsStyle()
                } else {
                    mostrarRegistrosConAnimaciones(registros)
                    iniciarScrollAutomatico(scrollView)
                }
            }
        }
    }

    private fun mostrarRegistrosConAnimaciones(registros: List<Gimnasio>) {
        fun actualizarTicker(index: Int) {
            val registro = registros[index % registros.size]
            tickerText.text = registro.diario
            tickerText.measure(0, 0)
            val width = tickerText.measuredWidth.toFloat()
            tickerText.translationX = mainFrame.width.toFloat()
            ObjectAnimator.ofFloat(
                tickerText, "translationX", mainFrame.width.toFloat(), -width
            ).setDuration(5000).start()
            handler.postDelayed({ actualizarTicker(index + 1) }, 5000)
        }
        actualizarTicker(0)
        registros.forEachIndexed { index, registro ->
            val color = when {
                registro.valor < 45 -> Color.RED
                registro.valor < 61 -> Color.rgb(0, 100, 0)
                registro.valor < 91 -> Color.rgb(255, 140, 0)
                else -> Color.RED
            }
            val barra = View(this@SalvapantallasActivity).apply {
                setBackgroundColor(color)
                layoutParams = LinearLayout.LayoutParams(0, 40).apply { topMargin = 20 }
            }
            container.addView(barra)
            barra.post {
                val anim = ValueAnimator.ofInt(0, registro.valor * 10)
                anim.duration = 1500
                anim.addUpdateListener { value ->
                    barra.layoutParams.width = value.animatedValue as Int
                    barra.requestLayout()
                }
                anim.start()
            }
            val destinoX = Random.nextInt(mainFrame.width - 100).toFloat()
            val destinoY = Random.nextInt(mainFrame.height - 100).toFloat()
            ObjectAnimator.ofFloat(barra, "translationX", 0f, destinoX).apply {
                duration = 8000
                repeatMode = ValueAnimator.REVERSE
                repeatCount = ValueAnimator.INFINITE
                start()
            }
            ObjectAnimator.ofFloat(barra, "translationY", 0f, destinoY).apply {
                duration = 8000
                repeatMode = ValueAnimator.REVERSE
                repeatCount = ValueAnimator.INFINITE
                start()
            }
            val totalMin = registro.valor
            val horas = totalMin / 60
            val mins = totalMin % 60
            val tv = TextView(this@SalvapantallasActivity).apply {
                val textoHorasMin = if (registro.valor >= 60) {
                    val horas = registro.valor / 60
                    val minutos = registro.valor % 60
                    "$horas h $minutos m"
                } else {
                    ""
                }
                text = if (textoHorasMin.isNotEmpty()) {
                    "${registro.fechaHora} - ${registro.valor} Min - ( $textoHorasMin )\n${registro.diario}"
                } else {
                    "${registro.fechaHora} - ${registro.valor} Min\n${registro.diario}"
                }
                textSize = 20f
                setTextColor(Color.WHITE)
                setPadding(0, 10, 0, 20)
                alpha = 0f
            }
            container.addView(tv)
            tv.postDelayed({
                ObjectAnimator.ofFloat(tv, View.ALPHA, 0f, 1f).setDuration(1200).start()
            }, (index * 500 + 400).toLong())
            val destTextX = Random.nextInt(mainFrame.width - 200).toFloat()
            val destTextY = Random.nextInt(mainFrame.height - 200).toFloat()
            ObjectAnimator.ofFloat(tv, "translationX", 0f, destTextX).apply {
                duration = 12000
                repeatMode = ValueAnimator.REVERSE
                repeatCount = ValueAnimator.INFINITE
                start()
            }
            ObjectAnimator.ofFloat(tv, "translationY", 0f, destTextY).apply {
                duration = 12000
                repeatMode = ValueAnimator.REVERSE
                repeatCount = ValueAnimator.INFINITE
                start()
            }
        }
        crearParticulas(30)
    }

    private fun mostrarLineasWindowsStyle() {
        val cantidadLineas = 10
        repeat(cantidadLineas) {
            val linea = View(this).apply {
                setBackgroundColor(Color.CYAN)
                layoutParams = FrameLayout.LayoutParams(5, 200)
            }
            mainFrame.addView(linea)
            val startX = Random.nextInt(mainFrame.width - 5).toFloat()
            val startY = Random.nextInt(mainFrame.height - 200).toFloat()
            linea.x = startX
            linea.y = startY
            val endX = Random.nextInt(mainFrame.width - 5).toFloat()
            val endY = Random.nextInt(mainFrame.height - 200).toFloat()
            val animX = ObjectAnimator.ofFloat(linea, "translationX", startX, endX).apply {
                duration = Random.nextLong(2000, 5000)
                repeatCount = ValueAnimator.INFINITE
                repeatMode = ValueAnimator.REVERSE
            }
            val animY = ObjectAnimator.ofFloat(linea, "translationY", startY, endY).apply {
                duration = Random.nextLong(2000, 5000)
                repeatCount = ValueAnimator.INFINITE
                repeatMode = ValueAnimator.REVERSE
            }
            AnimatorSet().apply {
                playTogether(animX, animY)
                start()
            }
        }
    }

    private fun crearParticulas(cantidad: Int) {
        repeat(cantidad) {
            val size = Random.nextInt(10, 30)
            val particula = View(this).apply {
                setBackgroundColor(Color.WHITE)
                alpha = Random.nextFloat() * 0.6f + 0.2f
                layoutParams = FrameLayout.LayoutParams(size, size).apply {
                    leftMargin = Random.nextInt(mainFrame.width)
                    topMargin = Random.nextInt(mainFrame.height)
                }
            }
            mainFrame.addView(particula)
            val destinoX = Random.nextInt(mainFrame.width).toFloat()
            val destinoY = Random.nextInt(mainFrame.height).toFloat()
            val animX =
                ObjectAnimator.ofFloat(particula, "translationX", particula.x, destinoX).apply {
                    duration = Random.nextLong(3000, 6000)
                    interpolator = android.view.animation.LinearInterpolator()
                    repeatCount = ValueAnimator.INFINITE
                    repeatMode = ValueAnimator.REVERSE
                }
            val animY =
                ObjectAnimator.ofFloat(particula, "translationY", particula.y, destinoY).apply {
                    duration = Random.nextLong(3000, 6000)
                    interpolator = android.view.animation.LinearInterpolator()
                    repeatCount = ValueAnimator.INFINITE
                    repeatMode = ValueAnimator.REVERSE
                }
            AnimatorSet().apply {
                playTogether(animX, animY)
                start()
            }
        }
    }

    private fun leerRegistroAleatorio() {
        if (!ttsEnabled || !::tts.isInitialized) return
        lifecycleScope.launch {
            val registros = withContext(Dispatchers.IO) {
                AppDatabase.getDatabase(this@SalvapantallasActivity).gimnasioDao().getAll()
            }
            if (registros.isNotEmpty()) {
                val reg = registros.random()
                val totalMin = reg.valor
                val horas = totalMin / 60
                val mins = totalMin % 60
                val textoHorasMin = if (horas > 0) "${horas} Horas Y ${mins} Minutos" else ""
                val mensajeTTS = if (textoHorasMin.isNotEmpty()) {
                    "${reg.fechaHora}; Tiempo De Gimnasio; ${reg.valor} Minutos; $textoHorasMin; Diario; ${reg.diario}"
                } else {
                    "${reg.fechaHora}; Tiempo De Gimnasio; ${reg.valor} Minutos; Diario; ${reg.diario}"
                }
                tts.speak(
                    mensajeTTS, TextToSpeech.QUEUE_FLUSH, null, "registroId"
                )
            }
        }
    }

    override fun onInit(status: Int) {}
    override fun onResume() {
        super.onResume()
        activarModoInmersivo()
    }

    override fun onPause() {
        super.onPause()
        mediaPlayer.pause()
        if (ttsEnabled) tts.stop()
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(updateRelojRunnable)
        mediaPlayer.release()
        if (ttsEnabled) tts.shutdown()
    }
}