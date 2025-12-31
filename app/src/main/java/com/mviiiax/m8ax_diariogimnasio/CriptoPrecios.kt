package com.mviiiax.m8ax_diariogimnasio

import android.content.pm.ActivityInfo
import android.graphics.Color
import android.media.MediaPlayer
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.GridLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.gson.Gson
import com.google.gson.JsonObject
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.IOException
import java.nio.charset.Charset
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.concurrent.fixedRateTimer
import kotlin.math.abs

class CriptoPrecios : AppCompatActivity(), TextToSpeech.OnInitListener {

    private val monedas = listOf(
        "BTCUSDT",
        "ETHUSDT",
        "BNBUSDT",
        "XRPUSDT",
        "SOLUSDT",
        "ADAUSDT",
        "DOGEUSDT",
        "DOTUSDT",
        "MATICUSDT",
        "LTCUSDT",
        "UNIUSDT",
        "LINKUSDT",
        "AVAXUSDT",
        "TRXUSDT",
        "SHIBUSDT",
        "NEARUSDT",
        "ATOMUSDT",
        "XLMUSDT",
        "ETCUSDT",
        "FILUSDT",
        "BCHUSDT",
        "HBARUSDT",
        "VETUSDT",
        "ICPUSDT",
        "AAVEUSDT",
        "ALGOUSDT",
        "APTUSDT",
        "EOSUSDT",
        "AXSUSDT",
        "MANAUSDT",
        "SANDUSDT",
        "THETAUSDT",
        "FTMUSDT",
        "GALAUSDT",
        "CHZUSDT",
        "XTZUSDT",
        "EGLDUSDT",
        "INJUSDT",
        "RUNEUSDT",
        "LDOUSDT",
        "MKRUSDT",
        "OPUSDT",
        "RNDRUSDT",
        "FETUSDT",
        "ARBUSDT",
        "SUIUSDT",
        "PEPEUSDT",
        "WLDUSDT",
        "SEIUSDT",
        "ROSEUSDT"
    )
    private val mensajesTranquilo = listOf(
        "Mercado Tranquilito En Los Últimos Cinco Minutos.",
        "Todo En Calma, Nada Se Movió En Los Últimos Cinco Minutos.",
        "Pura Paz, Mercado Dormidito Estos Cinco Minutos.",
        "Silencio Absoluto, Nadie Mueve Ficha En los Últimos Cinco Minutos.",
        "Mercado En Modo Siesta Total En Los Últimos Cinco Minutos.",
        "Ni Un Susurro, Todo Plano En Los Últimos Cinco Minutos.",
        "Paz Monástica En El Mercado Estos Cinco Minutos.",
        "Mercado En Modo Zen, Cero Movimiento En Cinco Minutos.",
        "Tranquilidad Absoluta, Nada Ha Pasado En Cinco Minutos.",
        "Mercado Congelado, Ni Un Tick En Los Últimos Cinco Minutos.",
        "Pura Calma Chicha, Mercado Inmóvil Los Últimos Cinco Minutos.",
        "Todo En Stand-by, Mercado Sin Acción En Los Últimos Cinco Minutos.",
        "Silencio Sepulcral En El Mercado, Estos Últimos Cinco Minutos.",
        "Mercado En Pausa, Cero Volatilidad En Los Últimos Cinco Minutos.",
        "Ni Un Solo Pump Ni Dump, Mercado Tranquilo En Los Últimos Cinco Minutillos."
    )
    private lateinit var tts: TextToSpeech
    private var ttsEnabled = true
    private lateinit var layout: GridLayout
    private lateinit var headerText: TextView
    private lateinit var topLineText: TextView
    private val handler = Handler(Looper.getMainLooper())
    private val client = OkHttpClient()
    private val gson = Gson()
    private val cambio24h = java.util.concurrent.ConcurrentHashMap<String, Double>()
    private val precioHace5Min = java.util.concurrent.ConcurrentHashMap<String, Double>()
    private val precioActual = java.util.concurrent.ConcurrentHashMap<String, Double>()
    private val cambio5Min = java.util.concurrent.ConcurrentHashMap<String, Double>()
    private var ultimaVez5Min = 0L
    private var mp: MediaPlayer? = null
    private var bytesDescargados: Long = 0
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        window.decorView.systemUiVisibility =
            (View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN)
        val config = getSharedPreferences("M8AX-Config_TTS", MODE_PRIVATE)
        ttsEnabled = config.getBoolean("tts_enabled", true)
        if (ttsEnabled) tts = TextToSpeech(this, this)
        try {
            mp = MediaPlayer.create(this, R.raw.m8axsonidofondo)
            mp?.isLooping = true
            mp?.setVolume(0.6f, 0.6f)
            mp?.start()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.BLACK)
            setPadding(10, 6, 10, 6)
            setOnClickListener { hablar24hConEstilo() }
        }
        headerText = TextView(this).apply {
            text = "M8AX Cripto Precios • Cargando... • 0.00 MB"
            textSize = 16f
            setTextColor(Color.CYAN)
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 8)
        }
        topLineText = TextView(this).apply {
            text = "Cargando Ranking..."
            textSize = 13f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            setPadding(0, 4, 0, 12)
            setBackgroundColor(Color.parseColor("#111111"))
        }
        layout = GridLayout(this).apply {
            columnCount = 10
            rowCount = 5
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f)
        }
        monedas.forEach { simbolo ->
            val tv = TextView(this).apply {
                text = simbolo.replace("USDT", "") + "\nCargando..."
                textSize = 15f
                setTextColor(Color.YELLOW)
                gravity = Gravity.CENTER
                setPadding(6, 10, 6, 10)
                setBackgroundColor(Color.parseColor("#0F0F0F"))
                layoutParams = GridLayout.LayoutParams().apply {
                    width = 0; height = 0
                    columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
                    rowSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
                    setMargins(3, 3, 3, 3)
                }
            }
            layout.addView(tv)
        }
        root.addView(headerText)
        root.addView(topLineText)
        root.addView(layout)
        setContentView(root)
        fixedRateTimer(period = 1000L) {
            handler.post {
                val sdf = SimpleDateFormat("EEEE, dd MMMM yyyy • HH:mm:ss", Locale.getDefault())
                val mb = String.format(Locale.US, "%.2f", bytesDescargados / (1024.0 * 1024.0))
                headerText.text = "M8AX Cripto Precios • ${
                    sdf.format(Date()).replaceFirstChar { it.uppercase() }
                } • $mb MB"
            }
        }
        fixedRateTimer(period = 8000L) {
            actualizarPrecios()
            val ahora = System.currentTimeMillis()
            if (ahora - ultimaVez5Min >= 300000L) {
                calcularCambio5Minutos()
                hablar5MinutosConEstilo()
                ultimaVez5Min = ahora
            }
        }
    }

    private fun actualizarPrecios() {
        monedas.forEach { simbolo ->
            val url = "https://api.binance.com/api/v3/ticker/24hr?symbol=$simbolo"
            val request = Request.Builder().url(url).build()
            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    e.printStackTrace()
                    handler.post {
                        val index = monedas.indexOf(simbolo)
                        if (index in 0 until layout.childCount) {
                            val tv = layout.getChildAt(index) as? TextView
                            tv?.text = "${simbolo.replace("USDT", "")}\nError"
                            tv?.setTextColor(Color.RED)
                        }
                    }
                }

                override fun onResponse(call: Call, response: Response) {
                    try {
                        response.body?.let { body ->
                            val bytes = body.bytes()
                            bytesDescargados += bytes.size
                            val json = gson.fromJson(
                                String(bytes, Charset.forName("UTF-8")), JsonObject::class.java
                            )
                            val precio = json.get("lastPrice")?.asString?.toDoubleOrNull() ?: 0.0
                            val cambio24 =
                                json.get("priceChangePercent")?.asString?.toDoubleOrNull() ?: 0.0
                            cambio24h[simbolo] = cambio24
                            precioActual[simbolo] = precio
                            if (!precioHace5Min.containsKey(simbolo)) precioHace5Min[simbolo] =
                                precio
                            handler.post {
                                val index = monedas.indexOf(simbolo)
                                if (index in 0 until layout.childCount) {
                                    val tv = layout.getChildAt(index) as? TextView
                                    tv?.let {
                                        val precioFmt = when {
                                            precio >= 10000 -> String.format(
                                                Locale.US, "%,.1f", precio
                                            )

                                            precio >= 1000 -> String.format(
                                                Locale.US, "%,.2f", precio
                                            )

                                            precio >= 100 -> String.format(
                                                Locale.US, "%.2f", precio
                                            )

                                            precio >= 10 -> String.format(Locale.US, "%.3f", precio)
                                            precio >= 1 -> String.format(Locale.US, "%.4f", precio)
                                            precio >= 0.1 -> String.format(
                                                Locale.US, "%.5f", precio
                                            )

                                            precio >= 0.001 -> String.format(
                                                Locale.US, "%.6f", precio
                                            )

                                            else -> String.format(Locale.US, "%.7f", precio)
                                        }
                                        it.text = "${
                                            simbolo.replace(
                                                "USDT", ""
                                            )
                                        }\n$$precioFmt\n${if (cambio24 >= 0) "+" else ""}${
                                            String.format(
                                                "%.2f", cambio24
                                            )
                                        }%"
                                        it.setTextColor(if (cambio24 >= 0) Color.GREEN else Color.RED)
                                        aplicarResaltadoDia()
                                    }
                                }
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        handler.post {
                            val index = monedas.indexOf(simbolo)
                            if (index in 0 until layout.childCount) {
                                val tv = layout.getChildAt(index) as? TextView
                                tv?.text = "${simbolo.replace("USDT", "")}\nError"
                                tv?.setTextColor(Color.RED)
                            }
                        }
                    }
                }
            })
        }
    }

    private fun aplicarResaltadoDia() {
        if (cambio24h.size < 10) return
        val mejor24h = cambio24h.maxByOrNull { it.value }?.key
        val peor24h = cambio24h.minByOrNull { it.value }?.key
        for (i in 0 until layout.childCount) {
            val tv = layout.getChildAt(i) as TextView
            val simbolo = monedas[i]
            val cambio = cambio24h[simbolo] ?: 0.0
            when (simbolo) {
                mejor24h -> {
                    tv.setBackgroundColor(Color.parseColor("#1B5E20"))
                    tv.setTextColor(Color.YELLOW)
                    tv.elevation = 16f
                }

                peor24h -> {
                    tv.setBackgroundColor(Color.parseColor("#B71C1C"))
                    tv.setTextColor(Color.WHITE)
                    tv.elevation = 16f
                }

                else -> {
                    tv.setBackgroundColor(Color.parseColor("#0F0F0F"))
                    tv.setTextColor(if (cambio >= 0) Color.GREEN else Color.RED)
                    tv.elevation = 4f
                }
            }
        }
        actualizarTop3()
    }

    private fun actualizarTop3() {
        if (cambio24h.size < 40) return
        val top3up = cambio24h.entries.sortedByDescending { it.value }.take(3).map {
            val s = it.key.replace("USDT", "")
            val p = if (it.value >= 0) "+${
                String.format(
                    "%.1f", it.value
                )
            }" else String.format("%.1f", it.value)
            "$s $p%"
        }.joinToString("   ")
        val top3down = cambio24h.entries.sortedBy { it.value }.take(3).map {
            val s = it.key.replace("USDT", "")
            "$s ${String.format("%.1f", it.value)}%"
        }.joinToString("   ")
        handler.post {
            topLineText.text = "↑ $top3up     ↓ $top3down"
        }
    }

    private fun calcularCambio5Minutos() {
        cambio5Min.clear()
        monedas.forEach { simbolo ->
            val viejo = precioHace5Min[simbolo] ?: return@forEach
            val ahora = precioActual[simbolo] ?: return@forEach
            if (viejo > 0.0) {
                val variacion = ((ahora - viejo) / viejo) * 100.0
                cambio5Min[simbolo] = variacion
            }
        }
        precioHace5Min.putAll(precioActual)
    }

    private fun hablar24hConEstilo() {
        if (!ttsEnabled || cambio24h.isEmpty()) return
        val hora = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
        val max = cambio24h.maxByOrNull { it.value } ?: return
        val min = cambio24h.minByOrNull { it.value } ?: return
        val mensaje = "Son Las $hora. En Las Últimas Veinticuatro Horas La Reina Absoluta Es ${
            max.key.replace(
                "USDT", ""
            )
        }; Con Un Brutal +${
            String.format(
                "%.2f", max.value
            )
        } Por Ciento; La Que Más Está Sufriendo Es ${min.key.replace("USDT", "")}; Con ${
            String.format(
                "%.2f", min.value
            )
        } Por Ciento."
        tts.speak(mensaje, TextToSpeech.QUEUE_FLUSH, null, null)
    }

    private fun hablar5MinutosConEstilo() {
        if (!ttsEnabled || cambio5Min.isEmpty()) return
        val max = cambio5Min.maxByOrNull { it.value } ?: return
        val min = cambio5Min.minByOrNull { it.value } ?: return
        val mensajeFinal = when {
            max.value > 0.8 || min.value < -0.8 -> {
                "¡Ojo! En Los Últimos Cinco Minutos ${
                    max.key.replace(
                        "USDT", ""
                    )
                }; Subió ${String.format("%.2f", max.value)} Por Ciento; Y ${
                    min.key.replace(
                        "USDT", ""
                    )
                }; Cayó ${String.format("%.2f", abs(min.value))} Por Ciento."
            }

            max.value > 0.3 || min.value < -0.3 -> {
                "En Los Últimos Cinco Minutos La Que Más Sube Es ${
                    max.key.replace(
                        "USDT", ""
                    )
                }; Con +${
                    String.format(
                        "%.2f", max.value
                    )
                } Por Ciento. La Que Más Baja ${
                    min.key.replace(
                        "USDT", ""
                    )
                }; Con ${String.format("%.2f", min.value)} Por Ciento."
            }

            else -> {
                mensajesTranquilo.random()
            }
        }
        handler.post {
            tts.speak(mensajeFinal, TextToSpeech.QUEUE_FLUSH, null, null)
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts.language = Locale.getDefault()
            tts.setSpeechRate(0.95f)
        }
    }

    override fun onDestroy() {
        mp?.stop(); mp?.release()
        if (ttsEnabled && ::tts.isInitialized) {
            tts.stop(); tts.shutdown()
        }
        super.onDestroy()
    }
}