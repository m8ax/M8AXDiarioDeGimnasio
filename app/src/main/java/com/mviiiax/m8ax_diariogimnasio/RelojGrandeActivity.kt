package com.mviiiax.m8ax_diariogimnasio

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.media.MediaPlayer
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.mviiiax.m8ax_diariogimnasio.databinding.ActivityRelojGrandeBinding
import org.jsoup.Jsoup
import org.shredzone.commons.suncalc.MoonIllumination
import java.lang.reflect.Field
import java.lang.reflect.Method
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.concurrent.thread

class RelojGrandeActivity : AppCompatActivity(), TextToSpeech.OnInitListener {
    private lateinit var binding: ActivityRelojGrandeBinding
    private val handler = Handler(Looper.getMainLooper())
    private val formatoHora = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
    private val formatoFecha = SimpleDateFormat("EEEE, dd/MM/yyyy", Locale.getDefault())
    private var mediaPlayer: MediaPlayer? = null
    private var tts: TextToSpeech? = null
    private var ttsEnabled = false
    private lateinit var lunaView: ImageView
    private val coloresReloj = listOf(
        0xFF00FFAA,
        0xFFFF00FF,
        0xFF00FFFF,
        0xFFFFFF00,
        0xFFFF6600,
        0xFFFF1493,
        0xFF00FF00,
        0xFFAA00FF
    ).map { it.toInt() }
    private val coloresTicker = listOf(
        0xFFFF0066,
        0xFFFF6600,
        0xFFFFFF33,
        0xFF00FFCC,
        0xFFFF3399,
        0xFF66FF00,
        0xFFFF9900,
        0xFFCC00FF,
        0xFF00FFFF
    ).map { it.toInt() }
    private val feeds = listOf(
        "https://feeds.elpais.com/mrss-s/pages/ep/site/elpais.com/portada",
        "https://feeds.elpais.com/mrss-s/pages/ep/site/elpais.com/section/internacional/portada",
        "https://e00-elmundo.uecdn.es/elmundo/rss/portada.xml",
        "https://e00-elmundo.uecdn.es/elmundo/rss/internacional.xml",
        "https://rss.nytimes.com/services/xml/rss/nyt/World.xml",
        "https://es.cointelegraph.com/rss",
        "https://elchapuzasinformatico.com/feed",
        "https://www.boe.es/rss/boe.php",
        "https://www.eldiadelarioja.es/rss/DLRPortada.xml",
        "https://engadget.com/rss.xml",
        "https://mviiiaxm8ax.blogspot.com/rss.xml"
    )
    private var tickerNoticias: List<String> = emptyList()
    private lateinit var lineaDia: View
    private lateinit var lineaSegundos: View
    private var colorLuna = coloresReloj.random()
    private var minutoAnterior = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val config = getSharedPreferences("M8AX-Config_TTS", MODE_PRIVATE)
        ttsEnabled = config.getBoolean("tts_enabled", false)
        tts = TextToSpeech(this, this)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        window.decorView.systemUiVisibility =
            (View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)
        supportActionBar?.hide()
        binding = ActivityRelojGrandeBinding.inflate(layoutInflater)
        setContentView(binding.root)
        mediaPlayer = MediaPlayer.create(this, R.raw.m8axsonidofondo)
        mediaPlayer?.isLooping = true
        mediaPlayer?.setVolume(0.5f, 0.5f)
        mediaPlayer?.start()
        binding.txtTicker.isSelected = true
        val alturaLinea = (3 * resources.displayMetrics.density).toInt()
        lineaDia = View(this).apply {
            setBackgroundColor(coloresReloj.random())
            layoutParams = FrameLayout.LayoutParams(0, alturaLinea)
        }
        binding.root.addView(lineaDia)
        lineaSegundos = View(this).apply {
            setBackgroundColor(coloresReloj.random())
            layoutParams = FrameLayout.LayoutParams(0, alturaLinea)
        }
        binding.root.addView(lineaSegundos)
        binding.txtFecha.apply {
            textSize = 26f
            text = horaEnRomano(Date()) + " - " + formatoFecha.format(Date())
            setTextColor(coloresReloj.random())
            gravity = android.view.Gravity.CENTER
            post {
                val offset = 3 * resources.displayMetrics.density
                val extraEspacio = 18 * resources.displayMetrics.density
                y = binding.txtHoraGrande.bottom + offset - extraEspacio
            }
        }
        binding.txtHoraGrande.post {
            val offset = 3 * resources.displayMetrics.density
            lineaDia.y = binding.txtHoraGrande.bottom + offset + binding.txtFecha.height
            lineaDia.x = 0f
            lineaDia.layoutParams.width = 0
            lineaDia.requestLayout()
        }
        handler.post(relojYcoloresRunnable)
        cargarNoticias()
        handler.postDelayed(cargarNoticiasRunnable, 600000)
        lunaView = ImageView(this)
        val size = (50 * resources.displayMetrics.density).toInt()
        val params = FrameLayout.LayoutParams(size, size)
        params.topMargin = binding.txtFecha.bottom + 10
        params.leftMargin = binding.txtFecha.right + 10
        binding.root.addView(lunaView, params)
    }

    private fun getPorcentajeLuna(fecha: Calendar): Int {
        val zoned = java.time.ZonedDateTime.of(
            fecha.get(Calendar.YEAR),
            fecha.get(Calendar.MONTH) + 1,
            fecha.get(Calendar.DAY_OF_MONTH),
            Calendar.getInstance().get(Calendar.HOUR_OF_DAY),
            Calendar.getInstance().get(Calendar.MINUTE),
            Calendar.getInstance().get(Calendar.SECOND),
            0,
            java.time.ZoneId.systemDefault()
        )
        val frac = MoonIllumination.compute().on(zoned.toInstant()).execute().fraction
        return (frac * 100.0).toInt()
    }

    private fun horaEnRomano(date: Date): String {
        val cal = Calendar.getInstance()
        cal.time = date
        val h = cal.get(Calendar.HOUR_OF_DAY)
        val m = cal.get(Calendar.MINUTE)
        val s = cal.get(Calendar.SECOND)
        return "${aRomano(h)}:${aRomano(m)}:${aRomano(s)}"
    }

    fun String.capitalizeWords(): String = split(" ").joinToString(" ") { word ->
        word.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
    }

    private fun aRomano(num: Int): String {
        if (num == 0) return "N"
        val valores = intArrayOf(
            1_000_000,
            900_000,
            500_000,
            400_000,
            100_000,
            90_000,
            50_000,
            40_000,
            10_000,
            9_000,
            5_000,
            4_000,
            1000,
            900,
            500,
            400,
            100,
            90,
            50,
            40,
            10,
            9,
            5,
            4,
            1
        )
        val cadenas = arrayOf(
            "M",
            "CM",
            "D",
            "CD",
            "C",
            "XC",
            "L",
            "XL",
            "X",
            "IX",
            "V",
            "IV",
            "M",
            "CM",
            "D",
            "CD",
            "C",
            "XC",
            "L",
            "XL",
            "X",
            "IX",
            "V",
            "IV",
            "I"
        )
        var resultado = StringBuilder()
        var decimal = num
        while (decimal > 0) {
            for (i in valores.indices) {
                if (decimal >= valores[i]) {
                    if (valores[i] > 1000) cadenas[i].forEach { c ->
                        resultado.append(c).append('\u0305')
                    } else resultado.append(cadenas[i])
                    decimal -= valores[i]
                    break
                }
            }
        }
        return resultado.toString()
    }

    private val relojYcoloresRunnable = object : Runnable {
        override fun run() {
            val ahora = Date()
            val fase = faseluna(
                Calendar.getInstance().toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
            )
            val horaFormateada = formatoHora.format(ahora)
            val fechaFormateada = formatoFecha.format(ahora)
            val fechaConDiaCapitalizado = fechaFormateada.replaceFirstChar {
                if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
            }
            binding.txtHoraGrande.text = horaFormateada
            val porcentajeLuna = getPorcentajeLuna(Calendar.getInstance().apply { time = ahora })
            binding.txtFecha.text =
                horaEnRomano(ahora) + " - " + fechaConDiaCapitalizado + ", Luna $porcentajeLuna% $fase"
            val size = (15 * resources.displayMetrics.density).toInt()
            val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            val paint = Paint().apply { isAntiAlias = true; style = Paint.Style.FILL }
            val r = size / 2f
            val ahoraa = Calendar.getInstance()
            val minutoActual = ahoraa.get(Calendar.MINUTE)
            if (minutoActual != minutoAnterior) {
                colorLuna = coloresReloj.random()
                minutoAnterior = minutoActual
            }
            paint.color = colorLuna
            canvas.drawCircle(r, r, r, paint)
            val moon = MoonIllumination.compute().on(
                java.time.ZonedDateTime.ofInstant(
                    Date().toInstant(), java.time.ZoneId.systemDefault()
                )
            ).execute()
            val f = moon.fraction.toFloat()
            paint.color = Color.DKGRAY
            for (x in 0 until size) {
                val xRel = x - r
                val yMax = Math.sqrt((r * r - xRel * xRel).toDouble()).toFloat()
                val drawGray = if (moon.phase < 0.5) x < size * (1f - f) else x > size * f
                if (drawGray) canvas.drawLine(x.toFloat(), r - yMax, x.toFloat(), r + yMax, paint)
            }
            val margen = 130f
            lunaView.x = binding.root.width - size - margen
            lunaView.y = 25f
            lunaView.setImageBitmap(bitmap)
            if (ahora.seconds == 0) {
                val colorHora = coloresReloj.random()
                binding.txtHoraGrande.setTextColor(colorHora)
                binding.txtFecha.setTextColor(colorHora)
                binding.txtTicker.setTextColor(coloresTicker.random())
                lineaDia.setBackgroundColor(coloresReloj.random())
                lineaSegundos.setBackgroundColor(coloresReloj.random())
                vibrarReloj(binding.txtHoraGrande, 2000L, 20f)
                if (ttsEnabled && ahora.minutes % 5 == 0) {
                    val ahora = Date()
                    val porcentajeLuna =
                        getPorcentajeLuna(Calendar.getInstance().apply { time = ahora })
                    val faseLeible = when (fase) {
                        "LN" -> "Luna Nueva"
                        "CC" -> "Cuarto Creciente"
                        "GC" -> "Gibosa Creciente"
                        "LL" -> "Luna Llena"
                        "GM" -> "Gibosa Menguante"
                        "CM" -> "Cuarto Menguante"
                        else -> fase
                    }
                    tts?.speak(
                        "Son Las ${horaFormateada.take(5)}; Luna Iluminada Al $porcentajeLuna%; $faseLeible",
                        TextToSpeech.QUEUE_FLUSH,
                        null,
                        "ttsHoraId"
                    )
                    if (tickerNoticias.isNotEmpty()) {
                        handler.postDelayed({
                            tickerNoticias.shuffled().take(5).forEachIndexed { index, noticia ->
                                tts?.speak(
                                    noticia, TextToSpeech.QUEUE_ADD, null, "ttsNoticia$index"
                                )
                            }
                        }, 3000)
                    }
                }
            }
            val anchoTotal = binding.root.width
            if (anchoTotal > 0) {
                val segundosDia = ahora.hours * 3600 + ahora.minutes * 60 + ahora.seconds
                val anchoDia = (anchoTotal * (segundosDia / 86400f)).toInt()
                lineaDia.layoutParams.width = anchoDia
                lineaDia.requestLayout()
                val anchoSegundos = (anchoTotal * (ahora.seconds / 60f)).toInt()
                lineaSegundos.layoutParams.width = anchoSegundos
                lineaSegundos.y = 0f
                lineaSegundos.x = 0f
                lineaSegundos.requestLayout()
            }
            handler.postDelayed(this, 1000)
        }
    }

    private val cargarNoticiasRunnable = object : Runnable {
        override fun run() {
            cargarNoticias()
            handler.postDelayed(this, 300000)
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_DOWN) {
            val ahora = Date()
            val horaFormateada = formatoHora.format(ahora)
            val porcentajeLuna = getPorcentajeLuna(Calendar.getInstance().apply { time = ahora })
            val fase = faseluna(
                Calendar.getInstance().toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
            )
            val faseLeible = when (fase) {
                "LN" -> "Luna Nueva"
                "CC" -> "Cuarto Creciente"
                "GC" -> "Gibosa Creciente"
                "LL" -> "Luna Llena"
                "GM" -> "Gibosa Menguante"
                "CM" -> "Cuarto Menguante"
                else -> fase
            }
            if (ttsEnabled) {
                tts?.speak(
                    "Son Las ${horaFormateada.take(5)}; Luna Iluminada Al $porcentajeLuna%; $faseLeible",
                    TextToSpeech.QUEUE_FLUSH,
                    null,
                    "ttsHoraIdTouch"
                )
            }
            return true
        }
        return super.onTouchEvent(event)
    }

    private fun cargarNoticias() {
        thread {
            try {
                val doc =
                    Jsoup.connect(feeds.random()).userAgent("Mozilla/5.0").timeout(15000).get()
                val noticias = doc.select("item title, entry title").mapNotNull { it.text().trim() }
                    .filter { it.length > 10 }.take(12)
                tickerNoticias = noticias
                val texto = noticias.map { it.capitalizeWords() }.joinToString("     •     ")
                    .ifBlank { "Cargando Noticias Del Mundo..." }
                runOnUiThread {
                    binding.txtTicker.text = texto
                    binding.txtTicker.isSelected = true
                    activarMarqueeRapido(binding.txtTicker, 450f)
                }
            } catch (e: Exception) {
                runOnUiThread {
                    binding.txtTicker.text = "M8AX Sin Conexión • Noticias Cada 10 Min"
                    binding.txtTicker.isSelected = true
                    activarMarqueeRapido(binding.txtTicker, 450f)
                }
            }
        }
    }

    private fun activarMarqueeRapido(tv: TextView, velocidad: Float) {
        tv.isSelected = true
        tv.post {
            try {
                val field: Field = TextView::class.java.getDeclaredField("mMarquee")
                field.isAccessible = true
                val marquee = field.get(tv) ?: return@post
                val method: Method =
                    marquee.javaClass.getDeclaredMethod("setSpeed", Float::class.java)
                method.isAccessible = true
                method.invoke(marquee, velocidad)
            } catch (e: Exception) {
            }
        }
    }

    fun faseluna(date: LocalDate): String {
        val now = ZonedDateTime.now()
        val zoned = ZonedDateTime.of(
            date.year,
            date.monthValue,
            date.dayOfMonth,
            now.hour,
            now.minute,
            now.second,
            0,
            ZoneId.systemDefault()
        )
        val illumination = MoonIllumination.compute().on(zoned.toInstant()).execute()
        val frac = illumination.fraction
        val waxing = illumination.phase < 0.5
        val pct = (frac * 100).toInt()
        return when {
            pct < 5 -> "LN"
            pct in 5..49 -> if (waxing) "CC" else "CM"
            pct in 50..94 -> if (waxing) "GC" else "GM"
            pct >= 95 -> "LL"
            else -> "LN"
        }
    }

    private fun vibrarReloj(tv: TextView, duracionMs: Long, amplitudPx: Float) {
        val anim =
            android.animation.ObjectAnimator.ofFloat(tv, "translationX", -amplitudPx, amplitudPx)
        anim.duration = 50L
        anim.repeatMode = android.animation.ValueAnimator.REVERSE
        anim.repeatCount = (duracionMs / 50 / 2).toInt()
        anim.start()
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts?.setLanguage(tts?.defaultLanguage ?: Locale.getDefault())
            tts?.setSpeechRate(0.9f)
        }
        if (ttsEnabled) {
            tts?.speak(
                "Abriendo Reloj Con Noticiario; En Pantalla Completa",
                TextToSpeech.QUEUE_FLUSH,
                null,
                "ttsHoraIdTouch"
            )
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
        tts?.shutdown()
    }

    override fun onPause() {
        super.onPause()
        mediaPlayer?.pause()
    }

    override fun onResume() {
        super.onResume()
        mediaPlayer?.start()
    }
}