package com.mviiiax.m8ax_diariogimnasio

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ArgbEvaluator
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Context
import android.content.pm.ActivityInfo
import android.graphics.Color
import android.graphics.RectF
import android.media.MediaPlayer
import android.os.Bundle
import android.os.Debug
import android.os.Handler
import android.os.Looper
import android.os.Process
import android.os.SystemClock
import android.speech.tts.TextToSpeech
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.shredzone.commons.suncalc.MoonIllumination
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt
import kotlin.random.Random

data class Barra(
    val view: View,
    var x: Float,
    var y: Float,
    var vx: Float,
    var vy: Float,
    var rotationSpeed: Float,
    val width: Float,
    val height: Float
)

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
    private var contenedorLuna: LinearLayout? = null
    private var ultimoMinutoHablado = -1
    private val BLOQUE_ANIMACION = 75
    private var registrosAnimacion: MutableList<Gimnasio> = mutableListOf()
    private val handlerAnimacion = Handler(Looper.getMainLooper())
    private lateinit var efectosFrame: FrameLayout
    private val tickerHandler = Handler(Looper.getMainLooper())
    private lateinit var tickerRapido: TextView
    private var tickerAnimatorSet: AnimatorSet? = null
    private var handlerFps: Handler? = null
    private var barrasRunnable: Runnable? = null
    private var updateLunaRunnable: Runnable? = null
    private val animParticulas = mutableListOf<AnimatorSet>()
    private var scrollListener: ViewTreeObserver.OnGlobalLayoutListener? = null
    private val animLineas = mutableListOf<AnimatorSet>()
    private var lastCpuTimeNs = 0L
    private var lastProcessCpuMs = 0L
    private var lastUptimeMs = 0L
    private var lastGcCount = 0L
    private var lastFrameTimeMs = SystemClock.elapsedRealtime()
    private var frameCount = 0
    private var fps = 0
    private var activityActiva = true
    private var juegoVidaRunnable: Runnable? = null
    private var matrizCidulas: Array<IntArray>? = null
    private val filasVida = 180
    private val columnasVida = 180
    private var vistasCelulas = mutableMapOf<Pair<Int, Int>, View>()
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
                val ahora = Calendar.getInstance()
                val porcentajeHoy = String.format(
                    Locale.US, "%.2f", getPorcentajeLuna(ahora)
                ).toDouble()
                ultimoMinutoHablado = minute
                tts?.speak(
                    "Hoy Es $diaSemana; Fecha; $fecha; Son Las $hora Y La Luna Esta Iluminada Al $porcentajeHoy%",
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

    private fun mostrarJuegoDeLaVida() {
        mainFrame.post {
            efectosFrame.removeAllViews()
            vistasCelulas.clear()
            val celdaW = mainFrame.width.toFloat() / columnasVida
            val celdaH = mainFrame.height.toFloat() / filasVida
            matrizCidulas =
                Array(filasVida) { IntArray(columnasVida) { if (Random.nextFloat() > 0.85f) 1 else 0 } }
            juegoVidaRunnable = object : Runnable {
                override fun run() {
                    val nuevaMatriz = Array(filasVida) { IntArray(columnasVida) }
                    for (f in 0 until filasVida) {
                        for (c in 0 until columnasVida) {
                            val vecinos = contarVecinos(f, c)
                            val viva = matrizCidulas!![f][c] == 1
                            if (viva && (vecinos == 2 || vecinos == 3)) nuevaMatriz[f][c] = 1
                            else if (!viva && vecinos == 3) nuevaMatriz[f][c] = 1
                            else if (!viva && Random.nextFloat() > 0.9995f) nuevaMatriz[f][c] = 1
                            else nuevaMatriz[f][c] = 0
                        }
                    }
                    matrizCidulas = nuevaMatriz
                    actualizarVistasVida(celdaW, celdaH)
                    handlerFps?.postDelayed(this, 10)
                }
            }
            handlerFps = Handler(Looper.getMainLooper())
            handlerFps?.post(juegoVidaRunnable!!)
        }
    }

    private fun contarVecinos(f: Int, c: Int): Int {
        var cuenta = 0
        for (i in -1..1) {
            for (j in -1..1) {
                if (i == 0 && j == 0) continue
                val nf = (f + i + filasVida) % filasVida
                val nc = (c + j + columnasVida) % columnasVida
                if (matrizCidulas!![nf][nc] == 1) cuenta++
            }
        }
        return cuenta
    }

    private fun actualizarVistasVida(w: Float, h: Float) {
        val pixelSize = 4
        val maxVistasVisibles = 8000
        var vistasEnPantalla = 0
        for (f in 0 until filasVida) {
            for (c in 0 until columnasVida) {
                val estaViva = matrizCidulas!![f][c] == 1
                val llave = Pair(f, c)
                val vista = vistasCelulas[llave]
                if (estaViva) {
                    if (vistasEnPantalla < maxVistasVisibles) {
                        vistasEnPantalla++
                        if (vista == null) {
                            val v = View(this).apply {
                                setBackgroundColor(
                                    Color.rgb(
                                        Random.nextInt(256),
                                        Random.nextInt(256),
                                        Random.nextInt(256)
                                    )
                                )
                                layoutParams = FrameLayout.LayoutParams(pixelSize, pixelSize)
                                x = c * w
                                y = f * h
                            }
                            efectosFrame.addView(v)
                            vistasCelulas[llave] = v
                        } else {
                            vista.visibility = View.VISIBLE
                        }
                    } else if (vista != null) {
                        vista.visibility = View.GONE
                    }
                } else {
                    vista?.visibility = View.GONE
                }
            }
        }
        if (vistasCelulas.size > 12000) {
            val iterator = vistasCelulas.entries.iterator()
            var eliminadas = 0
            while (iterator.hasNext() && eliminadas < 3000) {
                val entry = iterator.next()
                if (entry.value.visibility == View.GONE) {
                    efectosFrame.removeView(entry.value)
                    iterator.remove()
                    eliminadas++
                }
            }
        }
    }

    private fun iniciarScrollAutomatico(scrollView: ScrollView) {
        scrollAnim?.cancel()
        scrollAnim = null
        scrollListener?.let {
            scrollView.viewTreeObserver.removeOnGlobalLayoutListener(it)
            scrollListener = null
        }
        scrollListener = object : ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                scrollView.viewTreeObserver.removeOnGlobalLayoutListener(this)
                scrollListener = null
                val contentHeight = scrollView.getChildAt(0)?.height ?: return
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
        }
        scrollView.viewTreeObserver.addOnGlobalLayoutListener(scrollListener)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
            window.attributes.layoutInDisplayCutoutMode =
                android.view.WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }
        val prefs = getSharedPreferences("M8AX-Config_TTS", Context.MODE_PRIVATE)
        ttsEnabled = prefs.getBoolean("tts_enabled", false)
        if (ttsEnabled) tts = TextToSpeech(this, this)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        mainFrame = FrameLayout(this)
        setContentView(mainFrame)
        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(mainFrame) { v, insets ->
            val systemBars =
                insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.systemBars())
            v.setPadding(0, systemBars.top, 0, 0)
            insets
        }
        scrollView = ScrollView(this).apply {
            isFillViewport = true
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT
            )
        }
        container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.TOP
            setPadding(30, 0, 30, 0)
        }
        scrollView.addView(container)
        mainFrame.addView(scrollView)
        relojTextView = TextView(this).apply {
            textSize = 36f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = 20
            }
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
        tickerRapido = TextView(this).apply {
            textSize = 22f
            setTextColor(
                Color.rgb(
                    Random.nextInt(0, 256), Random.nextInt(0, 256), Random.nextInt(0, 256)
                )
            )
            setPadding(0, 10, 0, 10)
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }
        container.addView(tickerRapido)
        container.addView(tickerText)
        efectosFrame = FrameLayout(this)
        mainFrame.addView(efectosFrame)
        mediaPlayer = MediaPlayer.create(this, R.raw.m8axsonidofondo)
        mediaPlayer.isLooping = true
        mediaPlayer.start()
        activarModoInmersivo()
        handler.post(logEstadoRunnable)
        cargarRegistros()
    }

    fun iniciarTickerRapido(texto: String) {
        tickerAnimatorSet?.cancel()
        tickerAnimatorSet = null
        tickerRapido.text = texto
        tickerRapido.post {
            val screenWidth = mainFrame.width.toFloat()
            val screenHeight = mainFrame.height.toFloat()
            tickerRapido.maxWidth = screenWidth.toInt() - 40
            tickerRapido.isSingleLine = false
            tickerRapido.ellipsize = null
            tickerRapido.setHorizontallyScrolling(false)
            tickerRapido.measure(
                View.MeasureSpec.makeMeasureSpec(screenWidth.toInt(), View.MeasureSpec.AT_MOST),
                View.MeasureSpec.UNSPECIFIED
            )
            val textWidth = tickerRapido.measuredWidth.toFloat()
            val textHeight = tickerRapido.measuredHeight.toFloat()
            var startX = Random.nextFloat() * (screenWidth - textWidth)
            var startY = Random.nextFloat() * (screenHeight - textHeight)
            tickerRapido.translationX = startX
            tickerRapido.translationY = startY
            var velocidadMovimiento = Random.nextFloat() * (700f - 100f) + 100f
            var dx = if (Random.nextBoolean()) velocidadMovimiento else -velocidadMovimiento
            var dy = if (Random.nextBoolean()) velocidadMovimiento else -velocidadMovimiento
            var velocidadRotacion = Random.nextFloat() * (5f - 0.5f) + 0.5f
            var sentidoRotacion = if (Random.nextBoolean()) 1 else -1
            val anim = ValueAnimator.ofFloat(0f, 1f).apply {
                duration = 16L
                repeatCount = ValueAnimator.INFINITE
                addUpdateListener {
                    tickerRapido.translationX += dx * 0.016f
                    tickerRapido.translationY += dy * 0.016f
                    if (tickerRapido.translationX <= -textWidth / 2) {
                        tickerRapido.translationX = -textWidth / 2
                        dx = velocidadMovimiento
                    } else if (tickerRapido.translationX + textWidth >= screenWidth + textWidth / 2) {
                        tickerRapido.translationX = screenWidth - textWidth / 2
                        dx = -velocidadMovimiento
                    }
                    if (tickerRapido.translationY <= -textHeight / 2) {
                        tickerRapido.translationY = -textHeight / 2
                        dy = velocidadMovimiento
                    } else if (tickerRapido.translationY + textHeight >= screenHeight + textHeight / 2) {
                        tickerRapido.translationY = screenHeight - textHeight / 2
                        dy = -velocidadMovimiento
                    }
                    tickerRapido.rotation =
                        (tickerRapido.rotation + velocidadRotacion * sentidoRotacion) % 360f
                    if (Random.nextInt(0, 1000) < 2) {
                        sentidoRotacion *= -1
                    }
                    if (Random.nextFloat() < 0.01f) {
                        tickerRapido.setTextColor(
                            Color.rgb(
                                Random.nextInt(0, 256),
                                Random.nextInt(0, 256),
                                Random.nextInt(0, 256)
                            )
                        )
                        velocidadMovimiento = Random.nextFloat() * (700f - 100f) + 100f
                        val signoX = if (dx > 0) 1 else -1
                        val signoY = if (dy > 0) 1 else -1
                        dx = signoX * velocidadMovimiento
                        dy = signoY * velocidadMovimiento
                        velocidadRotacion = Random.nextFloat() * (5f - 0.5f) + 0.5f
                    }
                }
            }
            tickerAnimatorSet = AnimatorSet().apply {
                play(anim)
                start()
            }
        }
    }

    private fun activarModoInmersivo() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            window.setDecorFitsSystemWindows(false)
            val controller = window.insetsController
            if (controller != null) {
                controller.hide(androidx.core.view.WindowInsetsCompat.Type.systemBars())
                controller.systemBarsBehavior =
                    android.view.WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } else {
            window.decorView.systemUiVisibility =
                (View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_LAYOUT_STABLE)
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) activarModoInmersivo()
    }

    private fun cargarRegistros() {
        handlerAnimacion.post(rotarBloqueRunnable)
    }

    private val rotarBloqueRunnable: Runnable = Runnable {
        updateLunaRunnable?.let { handler.removeCallbacks(it) }
        contenedorLuna?.let { (window.decorView as FrameLayout).removeView(it) }
        juegoVidaRunnable = null
        handlerFps?.removeCallbacksAndMessages(null)
        animParticulas.forEach { it.cancel() }
        animParticulas.clear()
        animLineas.forEach { it.cancel() }
        animLineas.clear()
        handlerFps?.removeCallbacksAndMessages(null)
        handlerFps = null
        barrasRunnable = null
        efectosFrame.removeAllViews()
        tickerAnimatorSet?.cancel()
        tickerAnimatorSet = null
        scrollAnim?.cancel()
        scrollAnim = null
        tickerHandler.removeCallbacksAndMessages(null)
        efectosFrame.removeAllViews()
        lifecycleScope.launch {
            val nuevos: List<Gimnasio> = withContext(Dispatchers.IO) {
                AppDatabase.getDatabase(this@SalvapantallasActivity).gimnasioDao()
                    .getRegistrosAleatorios(BLOQUE_ANIMACION)
            }
            mainFrame.post {
                mainFrame.clearAnimation()
                if (Random.nextBoolean()) {
                    val anim = ObjectAnimator.ofObject(
                        mainFrame,
                        "backgroundColor",
                        ArgbEvaluator(),
                        Color.BLACK,
                        Color.HSVToColor(floatArrayOf(Random.nextFloat() * 360f, 0.8f, 0.15f)),
                        Color.BLACK
                    )
                    anim.duration = 6000
                    anim.repeatCount = ValueAnimator.INFINITE
                    anim.addListener(object : AnimatorListenerAdapter() {
                        override fun onAnimationRepeat(animation: Animator) {
                            val nuevoColor = Color.HSVToColor(
                                floatArrayOf(
                                    Random.nextFloat() * 360f, 0.8f, 0.15f
                                )
                            )
                            anim.setObjectValues(Color.BLACK, nuevoColor, Color.BLACK)
                        }
                    })
                    anim.start()
                } else {
                    mainFrame.setBackgroundColor(Color.BLACK)
                }
                container.removeAllViews()
                container.addView(relojTextView)
                val opcion = if (nuevos.size > 2) {
                    val r = Random.nextFloat()
                    when {
                        r < 0.70f -> 2
                        r < 0.80f -> 0
                        r < 0.90f -> 1
                        else -> 3
                    }
                } else {
                    val r = Random.nextFloat()
                    when {
                        r < 0.33f -> 0
                        r < 0.66f -> 1
                        else -> 3
                    }
                }
                when (opcion) {
                    0 -> mostrarLineasWindowsStyle1()
                    1 -> mostrarLineasWindowsStyle2()
                    2 -> {
                        registrosAnimacion.clear()
                        registrosAnimacion.addAll(nuevos)
                        val primerRegistro = registrosAnimacion.first()
                        if (!::tickerRapido.isInitialized) {
                            tickerRapido = TextView(this@SalvapantallasActivity).apply {
                                textSize = 22f
                                setPadding(0, 10, 0, 10)
                                layoutParams = LinearLayout.LayoutParams(
                                    ViewGroup.LayoutParams.WRAP_CONTENT,
                                    ViewGroup.LayoutParams.WRAP_CONTENT
                                )
                            }
                        }
                        tickerRapido.text =
                            "${primerRegistro.fechaHora} - ${primerRegistro.valor} Min - ( ${primerRegistro.valor / 60}h ${primerRegistro.valor % 60}m )\n${primerRegistro.diario}"
                        tickerRapido.setTextColor(
                            Color.rgb(
                                Random.nextInt(0, 256),
                                Random.nextInt(0, 256),
                                Random.nextInt(0, 256)
                            )
                        )
                        if (tickerRapido.parent == null) container.addView(tickerRapido, 1)
                        iniciarTickerRapido(tickerRapido.text.toString())
                        updateLunaRunnable?.let { handler.removeCallbacks(it) }
                        contenedorLuna?.let { (window.decorView as FrameLayout).removeView(it) }
                        contenedorLuna = LinearLayout(this@SalvapantallasActivity).apply {
                            orientation = LinearLayout.HORIZONTAL
                            gravity = Gravity.CENTER_VERTICAL
                        }
                        val dibujoLuna = LunaView(this@SalvapantallasActivity).apply {
                            layoutParams = LinearLayout.LayoutParams(120, 120)
                                .apply { setMargins(0, 0, 25, 0) }
                        }
                        val textoLuna = TextView(this@SalvapantallasActivity).apply {
                            textSize = 18f
                            setTextColor(Color.YELLOW)
                        }
                        contenedorLuna?.addView(dibujoLuna)
                        contenedorLuna?.addView(textoLuna)
                        (window.decorView as FrameLayout).addView(
                            contenedorLuna, FrameLayout.LayoutParams(-2, -2)
                        )
                        updateLunaRunnable = object : Runnable {
                            override fun run() {
                                val cal = Calendar.getInstance()
                                val zdt =
                                    java.time.ZonedDateTime.now(java.time.ZoneId.systemDefault())
                                val moon = org.shredzone.commons.suncalc.MoonIllumination.compute()
                                    .on(zdt.toInstant()).execute()
                                dibujoLuna.porcentaje = moon.fraction * 100.0
                                dibujoLuna.fase = moon.phase
                                if (cal.get(Calendar.SECOND) % 5 == 0) {
                                    val c = Color.rgb(
                                        Random.nextInt(128, 256),
                                        Random.nextInt(128, 256),
                                        Random.nextInt(128, 256)
                                    )
                                    dibujoLuna.colorLuna = c
                                    textoLuna.setTextColor(c)
                                }
                                dibujoLuna.invalidate()
                                textoLuna.text = "Luna Iluminada Al ➜ ${
                                    String.format(
                                        "%.6f", dibujoLuna.porcentaje
                                    )
                                }%"
                                handler.postDelayed(this, 1000)
                            }
                        }
                        handler.post(updateLunaRunnable!!)
                        contenedorLuna?.post {
                            val decor = window.decorView as FrameLayout
                            val sW =
                                (decor.width - contenedorLuna!!.width).toFloat().coerceAtLeast(1f)
                            val sH =
                                (decor.height - contenedorLuna!!.height).toFloat().coerceAtLeast(1f)
                            ObjectAnimator.ofFloat(contenedorLuna, "translationX", 0f, sW).apply {
                                duration = 20000L; repeatMode = ValueAnimator.REVERSE; repeatCount =
                                ValueAnimator.INFINITE; start()
                            }
                            ObjectAnimator.ofFloat(contenedorLuna, "translationY", 0f, sH).apply {
                                duration = 15000L; repeatMode = ValueAnimator.REVERSE; repeatCount =
                                ValueAnimator.INFINITE; start()
                            }
                        }
                        mostrarRegistrosConAnimaciones(registrosAnimacion)
                        scrollAnim?.cancel()
                        scrollAnim = null
                        iniciarScrollAutomatico(scrollView)
                    }

                    3 -> mostrarJuegoDeLaVida()
                }
            }
        }
        handlerAnimacion.postDelayed(rotarBloqueRunnable, 60_000)
    }

    private fun mostrarRegistrosConAnimaciones(registros: List<Gimnasio>) {
        tickerHandler.removeCallbacksAndMessages(null)
        fun actualizarTicker(index: Int) {
            val registro = registros[index % registros.size]
            tickerText.text = registro.diario
            tickerText.measure(0, 0)
            val width = tickerText.measuredWidth.toFloat()
            tickerText.translationX = mainFrame.width.toFloat()
            ObjectAnimator.ofFloat(
                tickerText, "translationX", mainFrame.width.toFloat(), -width
            ).setDuration(5000).start()
            tickerHandler.postDelayed({ actualizarTicker(index + 1) }, 5000)
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
            val tv = TextView(this@SalvapantallasActivity).apply {
                val textoHorasMin =
                    if (registro.valor >= 60) "${registro.valor / 60} h ${registro.valor % 60} m" else "${registro.valor} m"
                text =
                    "${registro.fechaHora} - ${registro.valor} Min - ( $textoHorasMin )\n${registro.diario}"
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
        val cantidadAleatoria = (50..300).random()
        crearParticulas(cantidadAleatoria)
    }

    private fun mostrarLineasWindowsStyle1() {
        mainFrame.post {
            val cantidadLineas = Random.nextInt(50, 501)
            repeat(cantidadLineas) {
                val colorAleatorio = Color.rgb(
                    Random.nextInt(50, 256), Random.nextInt(50, 256), Random.nextInt(50, 256)
                )
                val linea = View(this).apply {
                    setBackgroundColor(colorAleatorio)
                    layoutParams = FrameLayout.LayoutParams(5, 200)
                }
                efectosFrame.addView(linea)
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
                val rotacion = ObjectAnimator.ofFloat(linea, "rotation", 0f, 360f).apply {
                    duration = Random.nextLong(3000, 10000)
                    repeatCount = ValueAnimator.INFINITE
                    repeatMode = ValueAnimator.RESTART
                    interpolator = android.view.animation.LinearInterpolator()
                }
                val set = AnimatorSet().apply {
                    playTogether(animX, animY, rotacion)
                    start()
                }
                animLineas.add(set)
            }
        }
    }

    private fun mostrarLineasWindowsStyle2() {
        mainFrame.post {
            val cantidadLineas = Random.nextInt(50, 501)
            val barras = mutableListOf<Barra>()
            repeat(cantidadLineas) {
                val size = Random.nextInt(5, 20)
                val color = Color.rgb(
                    Random.nextInt(50, 256), Random.nextInt(50, 256), Random.nextInt(50, 256)
                )
                val barraView = View(this).apply {
                    setBackgroundColor(color)
                    layoutParams = FrameLayout.LayoutParams(size, size * 5)
                }
                efectosFrame.addView(barraView)
                val barra = Barra(
                    view = barraView,
                    x = Random.nextInt(mainFrame.width - size).toFloat(),
                    y = Random.nextInt(mainFrame.height - size * 5).toFloat(),
                    vx = Random.nextFloat() * 6 - 3,
                    vy = Random.nextFloat() * 6 - 3,
                    rotationSpeed = Random.nextFloat() * 4 - 2,
                    width = size.toFloat(),
                    height = (size * 5).toFloat()
                )
                barra.view.x = barra.x
                barra.view.y = barra.y
                barras.add(barra)
            }
            handlerFps?.removeCallbacksAndMessages(null)
            handlerFps = null
            barrasRunnable = null
            handlerFps = Handler(Looper.getMainLooper())
            barrasRunnable = object : Runnable {
                override fun run() {
                    for (i in barras.indices) {
                        val b = barras[i]
                        b.x += b.vx
                        b.y += b.vy
                        if (b.x < 0) {
                            b.x = 0f; b.vx *= -1
                        }
                        if (b.y < 0) {
                            b.y = 0f; b.vy *= -1
                        }
                        if (b.x + b.width > mainFrame.width) {
                            b.x = mainFrame.width - b.width; b.vx *= -1
                        }
                        if (b.y + b.height > mainFrame.height) {
                            b.y = mainFrame.height - b.height; b.vy *= -1
                        }
                        for (j in i + 1 until barras.size) {
                            val o = barras[j]
                            if (RectF(b.x, b.y, b.x + b.width, b.y + b.height).intersect(
                                    RectF(o.x, o.y, o.x + o.width, o.y + o.height)
                                )
                            ) {
                                val tempVx = b.vx
                                val tempVy = b.vy
                                b.vx = o.vx
                                b.vy = o.vy
                                o.vx = tempVx
                                o.vy = tempVy
                            }
                        }
                        b.view.x = b.x
                        b.view.y = b.y
                        b.view.rotation = (b.view.rotation + b.rotationSpeed) % 360
                    }
                    handlerFps?.postDelayed(this, 16)
                }
            }
            handlerFps?.post(barrasRunnable!!)
        }
    }

    private fun crearParticulas(cantidad: Int) {
        val modo = Random.nextInt(3)
        val colorFijoElegido =
            Color.rgb(Random.nextInt(256), Random.nextInt(256), Random.nextInt(256))
        repeat(cantidad) {
            val size = Random.nextInt(10, 30)
            val particula = View(this).apply {
                when (modo) {
                    0 -> setBackgroundColor(
                        Color.rgb(
                            Random.nextInt(256), Random.nextInt(256), Random.nextInt(256)
                        )
                    )

                    1 -> setBackgroundColor(colorFijoElegido)
                    else -> setBackgroundColor(Color.WHITE)
                }
                alpha = Random.nextFloat() * 0.6f + 0.2f
                layoutParams = FrameLayout.LayoutParams(size, size).apply {
                    leftMargin = Random.nextInt(mainFrame.width)
                    topMargin = Random.nextInt(mainFrame.height)
                }
            }
            efectosFrame.addView(particula)
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
            val set = AnimatorSet().apply { playTogether(animX, animY); start() }
            animParticulas.add(set)
        }
    }

    private fun getPorcentajeLuna(fecha: Calendar): Double {
        val zoned = java.time.ZonedDateTime.of(
            fecha.get(Calendar.YEAR),
            fecha.get(Calendar.MONTH) + 1,
            fecha.get(Calendar.DAY_OF_MONTH),
            fecha.get(Calendar.HOUR_OF_DAY),
            fecha.get(Calendar.MINUTE),
            fecha.get(Calendar.SECOND),
            0,
            java.time.ZoneId.systemDefault()
        )
        val frac = MoonIllumination.compute().on(zoned.toInstant()).execute().fraction
        return frac * 100
    }

    private fun leerRegistroAleatorio() {
        if (!ttsEnabled || !::tts.isInitialized || registrosAnimacion.isEmpty()) return
        val reg = registrosAnimacion.random()
        val porcentajeHoy = String.format(
            Locale.US, "%.2f", getPorcentajeLuna(Calendar.getInstance().apply {
                time = java.text.SimpleDateFormat(
                    "dd/MM/yyyy - HH:mm:ss", Locale.getDefault()
                ).parse(reg.fechaHora)
            })
        ).toDouble()
        val textoHorasMin =
            if (reg.valor >= 60) "${reg.valor / 60} Horas Y ${reg.valor % 60} Minutos" else "${reg.valor} Minutos"
        val mensajeTTS =
            "${reg.fechaHora}; Luna Iluminada Ese Día Al $porcentajeHoy%; Tiempo De Gimnasio; $textoHorasMin; Diario; ${reg.diario}"
        tts?.speak(mensajeTTS, TextToSpeech.QUEUE_FLUSH, null, "registroId")
    }

    override fun onInit(status: Int) {}

    override fun onPause() {
        super.onPause()
        mainFrame.clearAnimation()
        handler.removeCallbacksAndMessages(null)
        handlerAnimacion.removeCallbacksAndMessages(null)
        updateLunaRunnable?.let { handler.removeCallbacks(it) }
        juegoVidaRunnable = null
        handlerFps?.removeCallbacksAndMessages(null)
        tickerHandler.removeCallbacksAndMessages(null)
        handlerFps?.removeCallbacksAndMessages(null)
        handler.removeCallbacks(logEstadoRunnable)
        handlerFps = null
        barrasRunnable = null
        tickerAnimatorSet?.cancel()
        tickerAnimatorSet = null
        scrollAnim?.cancel()
        scrollAnim = null
        animParticulas.forEach { it.cancel() }
        animParticulas.clear()
        animLineas.forEach { it.cancel() }
        activityActiva = false
        animLineas.clear()
        efectosFrame.removeAllViews()
        if (::mediaPlayer.isInitialized && mediaPlayer.isPlaying) mediaPlayer.pause()
        if (ttsEnabled && ::tts.isInitialized) tts?.stop()
    }

    override fun onResume() {
        super.onResume()
        activarModoInmersivo()
        if (::mediaPlayer.isInitialized && !mediaPlayer.isPlaying) mediaPlayer.start()
        handler.post(updateRelojRunnable)
        activityActiva = true
        handler.post(logEstadoRunnable)
        handlerAnimacion.removeCallbacksAndMessages(null)
        handlerAnimacion.post(rotarBloqueRunnable)
    }

    private val logEstadoRunnable = object : Runnable {
        val handler = Handler(Looper.getMainLooper())
        override fun run() {
            if (!activityActiva) return
            val runtime = Runtime.getRuntime()
            val maxHeap = runtime.maxMemory() / 1024 / 1024
            val totalHeap = runtime.totalMemory() / 1024 / 1024
            val freeHeap = runtime.freeMemory() / 1024 / 1024
            val usedHeap = totalHeap - freeHeap
            val porcentajeHeap = (usedHeap.toDouble() / totalHeap * 100).roundToInt()
            val nativeUsed = Debug.getNativeHeapAllocatedSize() / 1024 / 1024
            val nativeFree = Debug.getNativeHeapFreeSize() / 1024 / 1024
            val nativeTotal = Debug.getNativeHeapSize() / 1024 / 1024
            val pss = Debug.getPss() / 1024
            val threads = Thread.activeCount()
            val uptimeMs = SystemClock.uptimeMillis()
            val uptime = uptimeMs / 1000
            val animPartCount = animParticulas.size
            val animLineCount = animLineas.size
            val gcCount = Debug.getRuntimeStat("art.gc.gc-count")?.toLongOrNull() ?: 0L
            val gcDelta = gcCount - lastGcCount
            lastGcCount = gcCount
            val cpuTimeNs = Debug.threadCpuTimeNanos()
            val deltaCpuNs = cpuTimeNs - lastCpuTimeNs
            val cpuPercentThread =
                if (lastUptimeMs > 0) (deltaCpuNs.toDouble() / ((uptimeMs - lastUptimeMs) * 1_000_000) * 100).roundToInt() else 0
            lastCpuTimeNs = cpuTimeNs
            val processCpuMs = Process.getElapsedCpuTime()
            val deltaProcessCpuMs = processCpuMs - lastProcessCpuMs
            val cpuPercentProcess =
                if (lastUptimeMs > 0) (deltaProcessCpuMs.toDouble() / (uptimeMs - lastUptimeMs) * 100).roundToInt() else 0
            lastProcessCpuMs = processCpuMs
            val now = SystemClock.elapsedRealtime()
            frameCount++
            if (now - lastFrameTimeMs >= 1000) {
                fps = frameCount
                frameCount = 0
                lastFrameTimeMs = now
            }
            lastUptimeMs = uptimeMs
            val horaActual = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
            val elementos = listOf(
                "Hora: $horaActual",
                "Uptime: ${uptime}s",
                "Heap Usada: ${usedHeap}MB (${porcentajeHeap}%)",
                "Heap Total: ${totalHeap}MB",
                "Heap Máx: ${maxHeap}MB",
                "Heap Libre: ${freeHeap}MB",
                "Nativa Usada: ${nativeUsed}MB",
                "Nativa Libre: ${nativeFree}MB",
                "Nativa Total: ${nativeTotal}MB",
                "PSS: ${pss}MB",
                "Hilos Activos: ${threads}",
                "AnimParticulas: ${animPartCount}",
                "AnimLineas: ${animLineCount}",
                "GC Delta: ${gcDelta}",
                "CPU Hilo Actual: ${deltaCpuNs / 1_000_000}ms",
                "CPU % Hilo: ${cpuPercentThread}%",
                "CPU % Proceso: ${cpuPercentProcess}%",
                "FPS Animaciones: ${fps}"
            )
            for (i in 0 until 6) {
                val fila = elementos.slice(i * 3 until (i + 1) * 3).joinToString(" | ")
                Log.d("DEBUG_RAM", fila)
            }
            Log.d("DEBUG_RAM", "\n")
            handler.postDelayed(this, 10000)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mainFrame.clearAnimation()
        handler.removeCallbacksAndMessages(null)
        handlerAnimacion.removeCallbacksAndMessages(null)
        tickerHandler.removeCallbacksAndMessages(null)
        juegoVidaRunnable = null
        handlerFps?.removeCallbacksAndMessages(null)
        activityActiva = false
        handler.removeCallbacks(logEstadoRunnable)
        handlerFps?.removeCallbacksAndMessages(null)
        updateLunaRunnable?.let { handler.removeCallbacks(it) }
        handlerFps = null
        barrasRunnable = null
        animParticulas.forEach { it.cancel() }
        animParticulas.clear()
        tickerAnimatorSet?.cancel()
        tickerAnimatorSet = null
        scrollAnim?.cancel()
        scrollAnim = null
        scrollListener?.let {
            scrollView.viewTreeObserver.removeOnGlobalLayoutListener(it)
            scrollListener = null
        }
        efectosFrame.removeAllViews()
        if (::tickerRapido.isInitialized) container.removeView(tickerRapido)
        if (::tickerText.isInitialized) container.removeView(tickerText)
        scrollView.removeAllViews()
        mediaPlayer.release()
        if (ttsEnabled) tts?.shutdown()
    }
}

class LunaView(context: Context) : View(context) {
    var porcentaje: Double = 0.0;
    var fase: Double = 0.0;
    var colorLuna: Int = Color.YELLOW
    private val paint = android.graphics.Paint()
        .apply { isAntiAlias = true; style = android.graphics.Paint.Style.FILL }

    override fun onDraw(canvas: android.graphics.Canvas) {
        val r = width.coerceAtMost(height) / 2f;
        val cx = width / 2f;
        val cy = height / 2f
        val f = (porcentaje / 100.0).toFloat()
        paint.color = colorLuna; canvas.drawCircle(cx, cy, r, paint)
        paint.color = Color.BLACK
        val clip = if (fase < 0.5) r * (1f - f) else r * f
        canvas.save()
        if (fase < 0.5) canvas.clipRect(cx - r, cy - r, cx - r + clip * 2f, cy + r)
        else canvas.clipRect(cx - r + clip * 2f, cy - r, cx + r, cy + r)
        canvas.drawCircle(cx, cy, r, paint); canvas.restore()
    }
}