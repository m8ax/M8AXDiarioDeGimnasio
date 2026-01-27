package com.mviiiax.m8ax_diariogimnasio

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Shader
import android.media.MediaPlayer
import android.net.ConnectivityManager
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.view.MotionEvent
import android.view.TextureView
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.mviiiax.m8ax_diariogimnasio.databinding.ActivityRelojGrandeBinding
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
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
    private lateinit var bitmapLuna: Bitmap
    private lateinit var txtClima: TextView
    private var mediaPlayerFondo: MediaPlayer? = null
    private var textureViewFondo: TextureView? = null
    private var mediaPlayer: MediaPlayer? = null
    private var tts: TextToSpeech? = null
    private var ttsEnabled = false
    private var urlFeedActual: String = ""
    private lateinit var lunaView: ImageView
    private var ultimoVideo = -1
    private lateinit var imgAlarma: ImageView
    private var horaAlarma: String = ""
    private var alarmaMediaPlayer: MediaPlayer? = null
    private lateinit var txtHoraAlarmaProgramada: TextView
    private val urlsRadios = listOf(
        "https://25543.live.streamtheworld.com/CADENADIAL.mp3",
        "https://playerservices.streamtheworld.com/api/livestream-redirect/RADIOMARCA_NACIONAL.mp3",
        "https://25493.live.streamtheworld.com/CADENASER.mp3",
        "https://stream.live.vc.bbcmedia.co.uk/bbc_world_service",
        "https://atres-live.ondacero.es/live/delegaciones/oc/logrono/master.m3u8",
        "https://dispatcher.rndfnk.com/crtve/rne1/rio/mp3/high",
        "https://dispatcher.rndfnk.com/crtve/rneree/main/mp3/high",
        "https://stream.radioparadise.com/aac-320",
        "https://dispatcher.rndfnk.com/crtve/rne5/rio/mp3/high",
        "https://rtva-live-radio.flumotion.com/rtva/cfr.mp3",
        "https://playerservices.streamtheworld.com/api/livestream-redirect/Los40.mp3",
        "https://playerservices.streamtheworld.com/api/livestream-redirect/LOS40_URBAN.mp3",
        "https://atres-live.europafm.com/live/europafm/master.m3u8",
        "https://stream-156.zeno.fm/se76qau1hc9uv",
        "https://atres-live.europafm.com/live/delegaciones/efm/logrono/master.m3u8",
        "https://pureibizaradio.clubbingradios.com:9518/PureIbizaRadio",
        "https://ibizasonica.streaming-pro.com:8011/sonicaclub",
        "https://ibizasonica.streaming-pro.com:8000/ibizasonica",
        "https://stream.zeno.fm/lwv6zqgtv1dtv",
        "https://s2.we4stream.com/listen/loca_90s_/live",
        "https://s2.we4stream.com/listen/loca_tech_house/live",
        "https://s2.we4stream.com/listen/loca_techo/live",
        "https://icecast.walmradio.com:8443/classic",
        "https://megastar-cope.flumotion.com/playlist.m3u8",
        "https://atres-live.melodia-fm.com/live/melodiafm/master.m3u8",
        "https://az1.mediacp.eu/listen/100greatestclassicalmusic/radio.mp3",
        "https://rockfm-cope.flumotion.com/playlist.m3u8",
        "https://playerservices.streamtheworld.com/api/livestream-redirect/OWR_INTERNATIONAL_ADP.m3u8",
        "https://bbhitfm.kissfmradio.cires21.com/bbhitfm.mp3",
        "https://playerservices.streamtheworld.com/api/livestream-redirect/TOPRADIOAAC.aac",
        "https://icecast.walmradio.com:8443/jazz",
        "https://streamer.radio.co/sa77aa975e/listen",
        "https://mangoradio.stream.laut.fm/mangoradio",
        "https://icy.unitedradio.it/um058.mp3",
        "https://stream.zeno.fm/pxzwykxbluitv",
        "https://icecast.walmradio.com:8443/christmas",
        "https://funkyradio.streamingmedia.it/play.mp3",
        "https://streaming.exclusive.radio/er/rollingstones/icecast.audio",
        "https://cast1.torontocast.com:4610/stream",
        "https://cast2.asurahosting.com/proxy/1940sradio/stream",
        "https://wecast-bl03.flumotion.com/copesedes/caceres.mp3",
        "https://21223.live.streamtheworld.com/SER_CACERES.mp3"
    )
    private val coloresReloj = listOf(
        0xFF00FFAA,
        0xFFFF00FF,
        0xFF00FFFF,
        0xFFFFFF00,
        0xFFFF6600,
        0xFFFF1493,
        0xFF00FF00,
        0xFFAA00FF,
        0xFFFFFFFF
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
        bitmapLuna = BitmapFactory.decodeResource(resources, R.drawable.m8axluna)
        val config = getSharedPreferences("M8AX-Config_TTS", MODE_PRIVATE)
        ttsEnabled = config.getBoolean("tts_enabled", false)
        tts = TextToSpeech(this, this)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        window.decorView.systemUiVisibility =
            (View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)
        supportActionBar?.hide()
        binding = ActivityRelojGrandeBinding.inflate(layoutInflater)
        setContentView(binding.root)
        cambiarVideoFondo()
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
            textSize = 24f
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
        txtClima = TextView(this).apply {
            textSize = 24f
            setTextColor(coloresReloj.random())
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = android.view.Gravity.TOP or android.view.Gravity.START
                topMargin = (2 * resources.displayMetrics.density).toInt()
                leftMargin = (10 * resources.displayMetrics.density).toInt()
            }
        }
        binding.root.addView(txtClima)
        txtHoraAlarmaProgramada = TextView(this).apply {
            textSize = 18f
            setTextColor(0xFF00FF00.toInt())
            visibility = View.GONE
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = android.view.Gravity.TOP or android.view.Gravity.START
                topMargin = (42 * resources.displayMetrics.density).toInt()
                leftMargin = (55 * resources.displayMetrics.density).toInt()
            }
        }
        binding.root.addView(txtHoraAlarmaProgramada)
        imgAlarma = ImageView(this).apply {
            setImageResource(android.R.drawable.ic_lock_idle_alarm)
            setColorFilter(0xFFFF0000.toInt())
            layoutParams = FrameLayout.LayoutParams(
                (40 * resources.displayMetrics.density).toInt(),
                (40 * resources.displayMetrics.density).toInt()
            ).apply {
                gravity = android.view.Gravity.TOP or android.view.Gravity.START
                topMargin = (35 * resources.displayMetrics.density).toInt()
                leftMargin = (10 * resources.displayMetrics.density).toInt()
            }
            setOnClickListener {
                if (alarmaMediaPlayer != null || horaAlarma.isNotEmpty()) {
                    detenerAlarma()
                    if (ttsEnabled) {
                        tts?.speak(
                            "Alarma Cancelada", TextToSpeech.QUEUE_FLUSH, null, "ttsAlarmaCancelada"
                        )
                    }
                    horaAlarma = ""
                    txtHoraAlarmaProgramada.visibility = View.GONE
                    imgAlarma.setColorFilter(0xFFFF0000.toInt())
                } else {
                    val cal = Calendar.getInstance()
                    android.app.TimePickerDialog(this@RelojGrandeActivity, { _, hour, minute ->
                        horaAlarma = String.format("%02d:%02d", hour, minute)
                        imgAlarma.setColorFilter(0xFF00FF00.toInt())
                        txtHoraAlarmaProgramada.text = horaAlarma
                        txtHoraAlarmaProgramada.visibility = View.VISIBLE
                    }, cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), true).show()
                }
            }
        }
        binding.root.addView(imgAlarma)
    }

    fun obtenerTemperaturaPorIP(): String? {
        return try {
            val client = OkHttpClient()
            val ipRequest = Request.Builder().url("http://ip-api.com/json/").build()
            val ipResponse = client.newCall(ipRequest).execute()
            if (!ipResponse.isSuccessful) return null
            val json = JSONObject(ipResponse.body?.string() ?: return null)
            val lat = json.optDouble("lat")
            val lon = json.optDouble("lon")
            val ciudad = json.optString("city", "Desconocido")
            val weatherRequest =
                Request.Builder().url("https://wttr.in/${lat},${lon}?format=%t").build()
            val weatherResponse = client.newCall(weatherRequest).execute()
            if (!weatherResponse.isSuccessful) return null
            val temp = weatherResponse.body?.string()?.trim()?.replace("+", "") ?: return null
            "$ciudad ➔ $temp"
        } catch (e: Exception) {
            null
        }
    }

    private fun cambiarVideoFondo() {
        mediaPlayerFondo?.let {
            try {
                if (it.isPlaying) it.stop()
            } catch (_: IllegalStateException) {
            }
            it.release()
        }
        mediaPlayerFondo = null
        textureViewFondo?.let { binding.root.removeView(it) }
        textureViewFondo = null
        var videoSeleccionado: Int
        do {
            videoSeleccionado = (1..7).random()
        } while (videoSeleccionado == ultimoVideo)
        ultimoVideo = videoSeleccionado
        textureViewFondo = TextureView(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT
            )
            binding.root.addView(this, 0)
        }
        textureViewFondo!!.surfaceTextureListener = object : TextureView.SurfaceTextureListener {
            override fun onSurfaceTextureAvailable(
                surface: android.graphics.SurfaceTexture, width: Int, height: Int
            ) {
                mediaPlayerFondo = MediaPlayer()
                val uri =
                    Uri.parse("android.resource://${packageName}/raw/m8axfondovideo$videoSeleccionado")
                mediaPlayerFondo!!.apply {
                    setDataSource(this@RelojGrandeActivity, uri)
                    isLooping = true
                    setVolume(0f, 0f)
                    setSurface(android.view.Surface(surface))
                    setOnPreparedListener { mp -> mp.start() }
                    prepareAsync()
                }
            }

            override fun onSurfaceTextureDestroyed(surface: android.graphics.SurfaceTexture): Boolean {
                mediaPlayerFondo?.let {
                    try {
                        if (it.isPlaying) it.stop()
                    } catch (_: IllegalStateException) {
                    }
                    it.release()
                }
                mediaPlayerFondo = null
                return true
            }

            override fun onSurfaceTextureSizeChanged(
                surface: android.graphics.SurfaceTexture, width: Int, height: Int
            ) {
            }

            override fun onSurfaceTextureUpdated(surface: android.graphics.SurfaceTexture) {}
        }
    }

    private fun getPorcentajeLuna(fecha: Calendar): Double {
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
        return frac * 100.0
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
            val porcentajeLuna = String.format(
                Locale.US, "%.2f", getPorcentajeLuna(Calendar.getInstance().apply { time = ahora })
            )
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
            val shader = BitmapShader(bitmapLuna, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
            val m = Matrix()
            val scale = (2f * r) / bitmapLuna.width.toFloat()
            m.setScale(scale, scale)
            shader.setLocalMatrix(m)
            val moon = MoonIllumination.compute().on(
                java.time.ZonedDateTime.ofInstant(
                    java.util.Date().toInstant(), java.time.ZoneId.systemDefault()
                )
            ).execute()
            val f = moon.fraction.toFloat()
            val anchoVisible = 2f * r * f
            val desdeDerecha = moon.phase < 0.5
            paint.shader = shader
            canvas.drawCircle(r, r, r, paint)
            paint.shader = null
            canvas.save()
            if (desdeDerecha) {
                canvas.clipRect(0f, 0f, 2f * r - anchoVisible, 2f * r)
            } else {
                canvas.clipRect(anchoVisible, 0f, 2f * r, 2f * r)
            }
            paint.color = 0x13FFFFFF.toInt()
            canvas.drawCircle(r, r, r, paint)
            canvas.restore()
            canvas.save()
            if (desdeDerecha) {
                canvas.clipRect(2f * r - anchoVisible, 0f, 2f * r, 2f * r)
            } else {
                canvas.clipRect(0f, 0f, anchoVisible, 2f * r)
            }
            paint.color = (colorLuna and 0x00FFFFFF) or 0x66000000.toInt()
            paint.xfermode =
                android.graphics.PorterDuffXfermode(android.graphics.PorterDuff.Mode.SCREEN)
            canvas.drawCircle(r, r, r, paint)
            paint.xfermode = null
            canvas.restore()
            val horaMinutoActual = horaFormateada.take(5)
            if (horaAlarma.isNotEmpty() && horaAlarma == horaMinutoActual && alarmaMediaPlayer == null) {
                dispararAlarma()
                horaAlarma = ""
            }
            val margen = 140f
            lunaView.x = binding.root.width - size - margen
            lunaView.y = 25f
            lunaView.setImageBitmap(bitmap)
            if (ahora.seconds == 0) {
                val colorHora = coloresReloj.random()
                val nuevoColor = coloresReloj.random()
                txtClima.setTextColor(nuevoColor)
                binding.txtHoraGrande.setTextColor(colorHora)
                binding.txtFecha.setTextColor(colorHora)
                binding.txtTicker.setTextColor(coloresTicker.random())
                lineaDia.setBackgroundColor(coloresReloj.random())
                lineaSegundos.setBackgroundColor(coloresReloj.random())
                vibrarReloj(binding.txtHoraGrande, 2000L, 20f)
                if (ttsEnabled && ahora.minutes % 5 == 0) {
                    val ahora = Date()
                    val porcentajeLuna = String.format(
                        Locale.US,
                        "%.2f",
                        getPorcentajeLuna(Calendar.getInstance().apply { time = ahora })
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
                    val climaParaVoz = txtClima.text.toString().replace("➔", "")
                    tts?.speak(
                        "Son Las ${horaFormateada.take(5)}; Temperatura Actual En ${climaParaVoz}; Luna Iluminada Al $porcentajeLuna%; $faseLeible",
                        TextToSpeech.QUEUE_FLUSH,
                        null,
                        "ttsHoraId"
                    )
                    if (tickerNoticias.isNotEmpty()) {
                        handler.postDelayed({
                            val esIngles =
                                urlFeedActual.contains("nytimes") || urlFeedActual.contains("engadget")
                            if (esIngles) {
                                val resultado = tts?.setLanguage(Locale.US)
                                if (resultado == TextToSpeech.LANG_MISSING_DATA || resultado == TextToSpeech.LANG_NOT_SUPPORTED) {
                                    tts?.setLanguage(tts?.defaultLanguage ?: Locale.getDefault())
                                }
                            } else {
                                tts?.setLanguage(tts?.defaultLanguage ?: Locale.getDefault())
                            }
                            tickerNoticias.shuffled().take(5).forEachIndexed { index, noticia ->
                                tts?.speak(noticia, TextToSpeech.QUEUE_ADD, null, "noticia$index")
                            }
                            handler.postDelayed({
                                tts?.setLanguage(tts?.defaultLanguage ?: Locale.getDefault())
                            }, 15000)
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
            cambiarVideoFondo()
            handler.postDelayed(this, 300000)
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_DOWN) {
            if (alarmaMediaPlayer != null) {
                detenerAlarma()
                txtHoraAlarmaProgramada.visibility = View.GONE
                return true
            }
            val ahora = Date()
            val horaFormateada = formatoHora.format(ahora)
            val porcentajeLuna = String.format(
                Locale.US, "%.2f", getPorcentajeLuna(Calendar.getInstance().apply { time = ahora })
            )
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
                tts?.setLanguage(tts?.defaultLanguage ?: Locale.getDefault())
                val climaParaVoz = txtClima.text.toString().replace("➔", "")
                tts?.speak(
                    "Son Las ${horaFormateada.take(5)}; Temperatura Actual En ${climaParaVoz}; Luna Iluminada Al $porcentajeLuna%; $faseLeible",
                    TextToSpeech.QUEUE_FLUSH,
                    null,
                    "ttsHoraIdTouch"
                )
            }
            return true
        }
        return super.onTouchEvent(event)
    }

    private fun dispararAlarma() {
        val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val net = cm.activeNetworkInfo
        if (net != null && net.isConnected) {
            try {
                alarmaMediaPlayer = MediaPlayer().apply {
                    setDataSource(urlsRadios.random())
                    setAudioAttributes(
                        android.media.AudioAttributes.Builder()
                            .setUsage(android.media.AudioAttributes.USAGE_ALARM)
                            .setContentType(android.media.AudioAttributes.CONTENT_TYPE_MUSIC)
                            .build()
                    )
                    setOnPreparedListener { it.start() }
                    setOnErrorListener { _, _, _ ->
                        soltarSonidoLocal()
                        true
                    }
                    prepareAsync()
                }
                vibrarReloj(imgAlarma, 60000L, 15f)
            } catch (e: Exception) {
                soltarSonidoLocal()
            }
        } else {
            soltarSonidoLocal()
        }
    }

    private fun soltarSonidoLocal() {
        try {
            alarmaMediaPlayer?.reset()
            val sonidosLocales = listOf(R.raw.m8axgimweb, R.raw.m8axsw, R.raw.m8axalcyonmsx)
            alarmaMediaPlayer = MediaPlayer.create(this, sonidosLocales.random())
            alarmaMediaPlayer?.apply {
                isLooping = true
                start()
            }
            vibrarReloj(imgAlarma, 60000L, 15f)
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    }

    private fun detenerAlarma() {
        try {
            alarmaMediaPlayer?.let {
                try {
                    it.stop()
                } catch (_: Exception) {
                }
                it.reset()
                it.release()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            alarmaMediaPlayer = null
            imgAlarma.setColorFilter(0xFFFF0000.toInt())
            imgAlarma.clearAnimation()
            txtHoraAlarmaProgramada.visibility = View.GONE
        }
    }

    private fun cargarNoticias() {
        thread {
            try {
                val climaInfo = obtenerTemperaturaPorIP()
                val feedElegido = feeds.random()
                urlFeedActual = feedElegido
                val doc = Jsoup.connect(feedElegido).userAgent("Mozilla/5.0").timeout(15000).get()
                val noticias = doc.select("item title, entry title").mapNotNull { it.text().trim() }
                    .filter { it.length > 10 }.take(12)
                tickerNoticias = noticias
                val texto = noticias.map { it.capitalizeWords() }.joinToString("     •     ")
                    .ifBlank { "Cargando Noticias Del Mundo..." }
                runOnUiThread {
                    txtClima.text = climaInfo ?: ""
                    binding.txtTicker.text = texto
                    activarMarqueeRapido(binding.txtTicker, 450f)
                }
            } catch (e: Exception) {
                runOnUiThread {
                    binding.txtTicker.text = "M8AX Sin Conexión • Noticias Cada 10 Min"
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

    private fun vibrarReloj(v: View, duracionMs: Long, amplitudPx: Float) {
        val anim =
            android.animation.ObjectAnimator.ofFloat(v, "translationX", -amplitudPx, amplitudPx)
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
            tts?.stop()
            tts?.speak(
                "Abriendo Reloj Con Noticiario; En Pantalla Completa.",
                TextToSpeech.QUEUE_FLUSH,
                null,
                "ttsHoraIdTouch"
            )
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            mediaPlayer?.stop()
        } catch (_: Exception) {
        }
        mediaPlayer?.release()
        mediaPlayer = null
        mediaPlayerFondo?.let {
            try {
                if (it.isPlaying) it.stop()
            } catch (_: Exception) {
            }
            it.release()
        }
        mediaPlayerFondo = null
        textureViewFondo?.let { binding.root.removeView(it) }
        textureViewFondo = null
        tts?.stop()
        tts?.shutdown()
        detenerAlarma()
    }

    override fun onPause() {
        super.onPause()
        try {
            if (alarmaMediaPlayer?.isPlaying == true) alarmaMediaPlayer?.pause()
        } catch (_: Exception) {
        }
        try {
            mediaPlayer?.pause()
        } catch (_: Exception) {
        }
        try {
            mediaPlayerFondo?.pause()
        } catch (_: Exception) {
        }
    }

    override fun onResume() {
        super.onResume()
        try {
            if (alarmaMediaPlayer != null && alarmaMediaPlayer?.isPlaying == false) alarmaMediaPlayer?.start()
        } catch (_: Exception) {
        }
        try {
            mediaPlayer?.start()
        } catch (_: Exception) {
        }
        try {
            if (mediaPlayerFondo != null && !mediaPlayerFondo!!.isPlaying) mediaPlayerFondo?.start()
        } catch (_: Exception) {
        }
    }
}