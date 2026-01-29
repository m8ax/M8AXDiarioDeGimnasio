package com.mviiiax.m8ax_diariogimnasio

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.drawable.GradientDrawable
import android.media.MediaPlayer
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import com.mviiiax.m8ax_diariogimnasio.databinding.ActivityAstronomiaBinding
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import org.shredzone.commons.suncalc.MoonIllumination
import org.shredzone.commons.suncalc.MoonPhase
import org.shredzone.commons.suncalc.MoonPosition
import org.shredzone.commons.suncalc.MoonTimes
import org.shredzone.commons.suncalc.SunPosition
import org.shredzone.commons.suncalc.SunTimes
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.Random
import kotlin.math.roundToInt

class AstronomiaActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAstronomiaBinding
    private val handler = Handler(Looper.getMainLooper())
    private var musicaFondo: MediaPlayer? = null
    private var lat = 42.46
    private var lng = -2.44
    private var ciudadActual = "Logroño ( Defecto )"
    private var tts: TextToSpeech? = null
    private var ttsEnabled: Boolean = false
    private var fraccionActual: Double = 0.0
    private var climaInfo: String = "Sincronizando..."
    private val random = Random()
    private var ultimoRefrescoClima: Long = 0
    private var estrellasEnPantalla = 0
    private val randomEstrellas = Random()
    private var estrellasFijas = 0
    private val maxEstrellasFondo = 200
    private var astronautaActivo: ImageView? = null
    private var overlayAstronauta: FrameLayout? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        activarModoInmersivo()
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        binding = ActivityAstronomiaBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val config = getSharedPreferences("M8AX-Config_TTS", MODE_PRIVATE)
        ttsEnabled = config.getBoolean("tts_enabled", false)
        if (ttsEnabled) {
            tts = TextToSpeech(this) { status ->
                if (status == TextToSpeech.SUCCESS) {
                    tts?.setLanguage(tts?.defaultLanguage ?: Locale.getDefault())
                    tts?.setSpeechRate(0.9f)
                }
            }
        }
        binding.imgLunaFondo.setOnClickListener {
            if (ttsEnabled) {
                val porcentaje = (fraccionActual * 100).format(2)
                val temperatura = if (climaInfo.contains("➔") && !climaInfo.contains(
                        "Error", true
                    )
                ) climaInfo.split("➔").last().trim() else ""
                val fase = calcularFaseDetallada(
                    fraccionActual,
                    MoonIllumination.compute().on(ZonedDateTime.now()).execute().phase
                )
                val mensaje = if (temperatura.isEmpty() || ciudadActual.contains(
                        "Error", true
                    ) || ciudadActual.contains("Defecto")
                ) {
                    "La Luna Está Iluminada Al $porcentaje Por Ciento, En Fase $fase."
                } else {
                    "En $ciudadActual Hay $temperatura Y La Luna Está Iluminada Al $porcentaje Por Ciento, En Fase $fase."
                }
                tts?.speak(mensaje, TextToSpeech.QUEUE_FLUSH, null, "lunaId")
            }
        }
        musicaFondo = MediaPlayer.create(this, R.raw.m8axsonidofondo).apply {
            setVolume(0.4f, 0.4f)
            isLooping = true
        }
        overlayAstronauta = FrameLayout(this)
        overlayAstronauta!!.layoutParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT
        )
        val root = findViewById<FrameLayout>(android.R.id.content)
        root.addView(overlayAstronauta)
        obtenerDatosUbicacionYClima()
    }

    private fun lanzarAstronauta() {
        if (astronautaActivo != null) return
        val root = findViewById<FrameLayout>(android.R.id.content)
        val astronauta = ImageView(this)
        astronauta.setImageResource(R.drawable.m8axastronauta)
        val tam = (30..900).random()
        astronauta.layoutParams = FrameLayout.LayoutParams(tam, tam)
        val w = root.width.toFloat()
        val h = root.height.toFloat()
        val lado = (0..3).random()
        var startX = 0f
        var startY = 0f
        var endX = 0f
        var endY = 0f
        when (lado) {
            0 -> {
                startX = -tam.toFloat(); startY = (0..h.toInt()).random().toFloat(); endX =
                    w + tam; endY = (0..h.toInt()).random().toFloat()
            }

            1 -> {
                startX = w + tam; startY = (0..h.toInt()).random().toFloat(); endX =
                    -tam.toFloat(); endY = (0..h.toInt()).random().toFloat()
            }

            2 -> {
                startX = (0..w.toInt()).random().toFloat(); startY = -tam.toFloat(); endX =
                    (0..w.toInt()).random().toFloat(); endY = h + tam
            }

            3 -> {
                startX = (0..w.toInt()).random().toFloat(); startY = h + tam; endX =
                    (0..w.toInt()).random().toFloat(); endY = -tam.toFloat()
            }
        }
        astronauta.x = startX
        astronauta.y = startY
        overlayAstronauta?.addView(astronauta)
        astronautaActivo = astronauta
        val duracion = (5000..12000).random().toLong()
        astronauta.animate().x(endX).y(endY).setDuration(duracion)
            .setInterpolator(android.view.animation.LinearInterpolator()).withEndAction {
                overlayAstronauta?.removeView(astronauta)
                astronautaActivo = null
                val retraso = (10000..25000).random().toLong()
                handler.postDelayed({ lanzarAstronauta() }, retraso)
            }.start()
        astronauta.animate().rotationBy(360f).setDuration(20000)
            .setInterpolator(android.view.animation.LinearInterpolator()).start()
    }

    private fun lanzarAstronautaRotacion(astronauta: ImageView) {
        astronauta.animate().rotationBy(360f).setDuration(20000)
            .setInterpolator(android.view.animation.LinearInterpolator()).withEndAction {
                if (astronautaActivo != null) lanzarAstronautaRotacion(astronauta)
            }.start()
    }

    private fun generarCieloEstrellado() {
        val root = findViewById<FrameLayout>(android.R.id.content)
        binding.imgLunaFondo.post {
            val lunaCentroX = binding.imgLunaFondo.x + binding.imgLunaFondo.width / 2f
            val lunaCentroY = binding.imgLunaFondo.y + binding.imgLunaFondo.height / 2f
            val lunaRadio =
                (binding.imgLunaFondo.width.coerceAtMost(binding.imgLunaFondo.height) / 2f) * 1.05f
            while (estrellasFijas < maxEstrellasFondo) {
                val estrella = View(this)
                estrella.layoutParams = FrameLayout.LayoutParams(1, 1)
                estrella.setBackgroundColor(Color.WHITE)
                val ancho = root.width
                val alto = root.height
                var x: Float
                var y: Float
                do {
                    x = randomEstrellas.nextFloat() * ancho
                    y = randomEstrellas.nextFloat() * alto
                } while ((x - lunaCentroX) * (x - lunaCentroX) + (y - lunaCentroY) * (y - lunaCentroY) < lunaRadio * lunaRadio)
                estrella.x = x
                estrella.y = y
                estrella.alpha = randomEstrellas.nextFloat().coerceIn(0.2f, 1f)
                root.addView(estrella)
                estrellasFijas++
                generarParpadeo(estrella)
            }
        }
    }

    private fun generarParpadeo(estrella: View) {
        val duracion = (100..3000).random().toLong()
        estrella.animate().alpha(if (estrella.alpha < 0.5f) 1f else 0.1f).setDuration(duracion)
            .setStartDelay((0..2000).random().toLong())
            .setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    generarParpadeo(estrella)
                }
            }).start()
    }

    private val motorVozAutomatico = object : Runnable {
        override fun run() {
            if (ttsEnabled) {
                val porcentaje = (fraccionActual * 100).format(2)
                val temperatura = if (climaInfo.contains("➔") && !climaInfo.contains(
                        "Error", true
                    )
                ) climaInfo.split("➔").last().trim() else ""
                val fase = calcularFaseDetallada(
                    fraccionActual,
                    MoonIllumination.compute().on(ZonedDateTime.now()).execute().phase
                )
                val mensaje = if (temperatura.isEmpty() || ciudadActual.contains(
                        "Error", true
                    ) || ciudadActual.contains("Defecto")
                ) {
                    "La Luna Está Iluminada Al $porcentaje Por Ciento, En Fase $fase."
                } else {
                    "En $ciudadActual Hay $temperatura Y La Luna Está Iluminada Al $porcentaje Por Ciento, En Fase $fase."
                }
                tts?.speak(mensaje, TextToSpeech.QUEUE_FLUSH, null, "autoVozId")
            }
            handler.postDelayed(this, 5 * 60 * 1000)
        }
    }

    private fun activarModoInmersivo() {
        window.decorView.systemUiVisibility =
            (View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_FULLSCREEN)
    }

    private val tick = object : Runnable {
        override fun run() {
            render()
            handler.postDelayed(this, 1000)
        }
    }

    private fun explotarEnLuna(x: Float, y: Float, impactoEnLuna: Boolean) {
        val root = findViewById<FrameLayout>(android.R.id.content)
        val esCritico = impactoEnLuna && random.nextInt(10) > 7
        val cant = if (esCritico) 100 else if (impactoEnLuna) 40 else 15
        if (impactoEnLuna) {
            val f = if (esCritico) 1.2f else 1.08f
            binding.imgLunaFondo.animate().scaleX(f).scaleY(f).setDuration(80).withEndAction {
                binding.imgLunaFondo.animate().scaleX(1f).scaleY(1f).setDuration(120).start()
            }.start()
            val originalX = binding.imgLunaFondo.translationX
            binding.imgLunaFondo.animate().translationX(originalX + 20f).setDuration(40)
                .withEndAction {
                    binding.imgLunaFondo.animate().translationX(originalX - 20f).setDuration(40)
                        .withEndAction {
                            binding.imgLunaFondo.animate().translationX(originalX).setDuration(40)
                                .start()
                        }.start()
                }.start()
        }
        val mp = MediaPlayer.create(this, R.raw.m8axinicio8)
        mp.start()
        mp.setOnCompletionListener { it.release() }
        repeat(cant) {
            val p = View(this)
            val s = if (esCritico) (12..30).random() else (8..20).random()
            p.layoutParams = FrameLayout.LayoutParams(s, s)
            p.background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.rgb(random.nextInt(256), random.nextInt(256), random.nextInt(256)))
            }
            p.x = x - (s / 2)
            p.y = y - (s / 2)
            p.z = 100f
            root.addView(p)
            val disp = if (esCritico) 700f else 350f
            p.animate().x(x + (random.nextFloat() * 2 - 1) * disp)
                .y(y + (random.nextFloat() * 2 - 1) * disp).alpha(0f).scaleX(0.2f).scaleY(0.2f)
                .setDuration((400..1000).random().toLong())
                .setListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(a: Animator) {
                        root.removeView(p)
                    }
                }).start()
        }
    }

    private fun lanzarCometa() {
        val root = findViewById<FrameLayout>(android.R.id.content)
        val cometa = View(this)
        val tam = (10..15).random()
        cometa.layoutParams = FrameLayout.LayoutParams(tam * 10, tam)
        val color = Color.rgb(random.nextInt(256), random.nextInt(256), random.nextInt(256))
        cometa.background = GradientDrawable(
            GradientDrawable.Orientation.LEFT_RIGHT,
            intArrayOf(Color.TRANSPARENT, color, Color.WHITE)
        )
        val location = IntArray(2)
        binding.imgLunaFondo.getLocationOnScreen(location)
        val lunaX = location[0].toFloat() + (binding.imgLunaFondo.width / 2)
        val lunaY = location[1].toFloat() + (binding.imgLunaFondo.height / 2)
        val vaALuna = random.nextInt(10) < 7
        val tx = if (vaALuna) lunaX + (-120..120).random() else (0..root.width).random().toFloat()
        val ty = if (vaALuna) lunaY + (-120..120).random() else (0..root.height).random().toFloat()
        val startX = if (random.nextBoolean()) -200f else root.width.toFloat() + 200f
        val startY = (0..root.height).random().toFloat()
        cometa.x = startX
        cometa.y = startY
        cometa.rotation =
            Math.toDegrees(Math.atan2((ty - startY).toDouble(), (tx - startX).toDouble())).toFloat()
        cometa.elevation = 99f
        root.addView(cometa)
        cometa.animate().x(tx).y(ty).setDuration((600..1400).random().toLong())
            .setInterpolator(android.view.animation.LinearInterpolator())
            .setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(a: Animator) {
                    explotarEnLuna(tx, ty, vaALuna)
                    root.removeView(cometa)
                }
            }).start()
    }

    private val motorEstrellas = object : Runnable {
        override fun run() {
            if (estrellasEnPantalla < 15) {
                val aCrear = random.nextInt(7)
                repeat(aCrear) { if (estrellasEnPantalla < 15) lanzarEstrella() }
            }
            if (random.nextInt(100) > 70) {
                lanzarCometa()
                if (random.nextInt(10) > 6) {
                    handler.postDelayed({ lanzarCometa() }, (500..1500).random().toLong())
                }
            }
            val proximoAtaque = (2000..15000).random().toLong()
            handler.postDelayed(this, proximoAtaque)
        }
    }

    private fun lanzarEstrella() {
        val estrella = View(this)
        val ancho = (80..300).random()
        val alto = (2..4).random()
        estrella.layoutParams = FrameLayout.LayoutParams(ancho, alto)
        val gd = GradientDrawable(
            GradientDrawable.Orientation.LEFT_RIGHT, intArrayOf(Color.TRANSPARENT, Color.WHITE)
        )
        estrella.background = gd
        val root = findViewById<FrameLayout>(android.R.id.content)
        val w = root.width.toFloat()
        val h = root.height.toFloat()
        val ladoSalida = random.nextInt(4)
        var startX = 0f
        var startY = 0f
        var endX = 0f
        var endY = 0f
        var angulo = 0f
        when (ladoSalida) {
            0 -> {
                startX = -ancho.toFloat(); startY = random.nextFloat() * h; endX = w + ancho; endY =
                    startY + (h * 0.2f); angulo = 15f
            }

            1 -> {
                startX = w + ancho; startY = random.nextFloat() * h; endX = -ancho.toFloat(); endY =
                    startY + (h * 0.2f); angulo = 165f
            }

            2 -> {
                startX = random.nextFloat() * w; startY = -ancho.toFloat(); endX =
                    startX + (w * 0.2f); endY = h + ancho; angulo = 75f
            }

            3 -> {
                startX = random.nextFloat() * w; startY = h + ancho; endX =
                    startX - (w * 0.2f); endY = -ancho.toFloat(); angulo = 255f
            }
        }
        estrella.x = startX
        estrella.y = startY
        estrella.rotation = angulo
        val indexLuna = root.indexOfChild(binding.imgLunaFondo)
        if (indexLuna >= 0) root.addView(estrella, indexLuna) else root.addView(estrella)
        estrellasEnPantalla++
        val velocidadAleatoria = (300..1500).random().toLong()
        estrella.animate().x(endX).y(endY).setDuration(velocidadAleatoria)
            .setInterpolator(android.view.animation.AccelerateInterpolator())
            .setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    root.removeView(estrella)
                    estrellasEnPantalla--
                }
            }).start()
    }

    private fun obtenerDatosUbicacionYClima() {
        Thread {
            try {
                val client = OkHttpClient()
                val ipRequest = Request.Builder().url("http://ip-api.com/json/").build()
                val ipResponse = client.newCall(ipRequest).execute()
                if (ipResponse.isSuccessful) {
                    val json = JSONObject(ipResponse.body?.string() ?: "")
                    lat = json.optDouble("lat", 42.46)
                    lng = json.optDouble("lon", -2.44)
                    ciudadActual = json.optString("city", "Logroño")
                }
                val weatherRequest =
                    Request.Builder().url("https://wttr.in/$lat,$lng?format=%t").build()
                val weatherResponse = client.newCall(weatherRequest).execute()
                if (weatherResponse.isSuccessful) {
                    val temp = weatherResponse.body?.string()?.trim()?.replace("+", "") ?: ""
                    climaInfo = "$ciudadActual ➔ $temp"
                }
            } catch (e: Exception) {
                climaInfo = "Error De Red"
            }
        }.start()
    }

    fun intToRoman(num: Int): String {
        if (num < 0) return ""
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

    private fun render() {
        val ahoraMillis = System.currentTimeMillis()
        val tiempoEspera =
            if (climaInfo.contains("Error", true) || ciudadActual.isBlank()) 30000 else 900000
        if (ahoraMillis - ultimoRefrescoClima > tiempoEspera) {
            ultimoRefrescoClima = ahoraMillis
            obtenerDatosUbicacionYClima()
        }
        val now = ZonedDateTime.now()
        val f = DateTimeFormatter.ofPattern("HH:mm:ss")
        val fFecha = DateTimeFormatter.ofPattern("dd/MM/yyyy")
        val fHora = DateTimeFormatter.ofPattern("HH:mm:ss")
        val sb = StringBuilder()
        val s = SunTimes.compute().at(lat, lng).on(now.toLocalDate()).fullCycle().execute()
        val c = SunTimes.compute().at(lat, lng).on(now)
        val sp = SunPosition.compute().at(lat, lng).on(now).execute()
        val duracionDia = if (s.rise != null && s.set != null) {
            val segundos = java.time.Duration.between(s.rise, s.set).seconds
            val horas = segundos / 3600
            val minutos = (segundos % 3600) / 60
            val segundosRestantes = segundos % 60
            "${horas}h ${minutos}m ${segundosRestantes}s"
        } else {
            "N / D"
        }
        sb.append("══ SOL ➔ HORARIOS DEL DÍA ══\n")
        sb.append("SALIDA DEL SOL ➔     ${s.rise?.format(f)}\n")
        sb.append("PUESTA DEL SOL ➔     ${s.set?.format(f)}\n")
        sb.append("TIEMPO DE LUZ ➔      $duracionDia\n")
        sb.append("PUNTO MÁS ALTO ➔     ${s.noon?.format(f)}\n")
        sb.append("PUNTO MÁS BAJO ➔     ${s.nadir?.format(f)}\n")
        sb.append(
            "LUZ NATURAL ➔        ${
                c.twilight(SunTimes.Twilight.CIVIL).execute().rise?.format(f)
            }\n"
        )
        sb.append(
            "CLARIDAD NÁUTICA ➔   ${
                c.twilight(SunTimes.Twilight.NAUTICAL).execute().rise?.format(f)
            }\n"
        )
        sb.append(
            "OSCURIDAD TOTAL ➔    ${
                c.twilight(SunTimes.Twilight.ASTRONOMICAL).execute().rise?.format(f)
            }\n"
        )
        sb.append(
            "HORA DORADA ➔        ${
                c.twilight(SunTimes.Twilight.GOLDEN_HOUR).execute().rise?.format(f)
            }\n"
        )
        sb.append(
            "HORA AZUL ➔          ${
                c.twilight(SunTimes.Twilight.BLUE_HOUR).execute().rise?.format(f)
            }\n"
        )
        sb.append("\n══ SOL ➔ DATOS DE POSICIÓN ══\n")
        val azimut = sp.azimuth
        val orientacion = when {
            azimut >= 337.5 || azimut < 22.5 -> "NORTE"
            azimut >= 22.5 && azimut < 67.5 -> "NORESTE"
            azimut >= 67.5 && azimut < 112.5 -> "ESTE"
            azimut >= 112.5 && azimut < 157.5 -> "SURESTE"
            azimut >= 157.5 && azimut < 202.5 -> "SUR"
            azimut >= 202.5 && azimut < 247.5 -> "SUROESTE"
            azimut >= 247.5 && azimut < 292.5 -> "OESTE"
            else -> "NOROESTE"
        }
        sb.append("DÓNDE MIRAR ➔        $orientacion (${azimut.format(2)}°)\n")
        sb.append("ELEVACIÓN ➔          ${sp.altitude.format(2)}°\n")
        sb.append(
            "DIST. A TIERRA ➔     ${
                String.format(
                    Locale("es", "ES"), "%,d", sp.distance.toLong()
                )
            } KM\n"
        )
        val m = MoonTimes.compute().at(lat, lng).on(now.toLocalDate()).fullCycle().execute()
        val mi = MoonIllumination.compute().on(now).execute()
        val mp = MoonPosition.compute().at(lat, lng).on(now).execute()
        fraccionActual = mi.fraction
        sb.append("\n══ LUNA ➔ HORARIOS Y ESTADO ══\n")
        sb.append("SALIDA DE LUNA ➔     ${m.rise?.format(f)}\n")
        sb.append("PUESTA DE LUNA ➔     ${m.set?.format(f)}\n")
        val ahora = ZonedDateTime.now()
        var ultimaNueva = MoonPhase.compute().phase(MoonPhase.Phase.NEW_MOON).execute().time
        if (ultimaNueva.isAfter(ahora)) ultimaNueva =
            ultimaNueva.minusSeconds((29.530588853 * 86400).toLong())
        val edadSegundos = java.time.Duration.between(ultimaNueva, ahora).seconds
        val dias = edadSegundos / 86400
        val horas = (edadSegundos % 86400) / 3600
        val minutos = (edadSegundos % 3600) / 60
        val segundos = edadSegundos % 60
        sb.append("EDAD DE LA LUNA ➔    ${dias}d ${horas}h ${minutos}m ${segundos}s\n")
        val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")
        val proximaLunaLlena = MoonPhase.compute().phase(MoonPhase.Phase.FULL_MOON).execute().time
        val proximaLunaNueva = MoonPhase.compute().phase(MoonPhase.Phase.NEW_MOON).execute().time
        sb.append("PRÓX. LUNA LLENA ➔   ${proximaLunaLlena.format(formatter)}\n")
        sb.append("PRÓX. LUNA NUEVA ➔   ${proximaLunaNueva.format(formatter)}\n")
        sb.append("24H VISIBLE ➔        ${if (m.isAlwaysUp) "SÍ" else "NO"}\n")
        sb.append("24H OCULTA ➔         ${if (m.isAlwaysDown) "SÍ" else "NO"}\n")
        sb.append("\n══ LUNA ➔ APARIENCIA ACTUAL ══\n")
        sb.append("PORCIÓN ILUMINADA ➔  ${mi.fraction.format(4)}\n")
        sb.append("BRILLO EN CIELO ➔    ${(mi.fraction * 100).format(7)}%\n")
        sb.append("FASE ACTUAL ➔        ${calcularFaseDetallada(mi.fraction, mi.phase)}\n")
        sb.append("GRADOS DE LA FASE ➔  ${mi.phase.format(2)}°\n")
        sb.append("ÁNGULO DE LUZ ➔      ${mi.angle.format(2)}°\n")
        val azimutLuna = mp.azimuth
        val orientacionLuna = when {
            azimutLuna >= 337.5 || azimutLuna < 22.5 -> "NORTE"
            azimutLuna >= 22.5 && azimutLuna < 67.5 -> "NORESTE"
            azimutLuna >= 67.5 && azimutLuna < 112.5 -> "ESTE"
            azimutLuna >= 112.5 && azimutLuna < 157.5 -> "SURESTE"
            azimutLuna >= 157.5 && azimutLuna < 202.5 -> "SUR"
            azimutLuna >= 202.5 && azimutLuna < 247.5 -> "SUROESTE"
            azimutLuna >= 247.5 && azimutLuna < 292.5 -> "OESTE"
            else -> "NOROESTE"
        }
        sb.append("\n══ LUNA ➔ POSICIÓN EXACTA ══\n")
        sb.append("DÓNDE MIRAR ➔        $orientacionLuna (${azimutLuna.format(2)}°)\n")
        sb.append("ALTURA SOBRE SUELO ➔ ${mp.altitude.format(2)}°\n")
        sb.append(
            "DISTANCIA A TIERRA ➔ ${
                String.format(
                    Locale("es", "ES"), "%,d", mp.distance.roundToInt()
                )
            } KM\n"
        )
        sb.append("INCLINACIÓN VISUAL ➔ ${(mi.angle - mp.parallacticAngle).format(2)}°\n")
        sb.append("\n══ INFORMACIÓN LOCAL ══\n")
        sb.append("FECHA ➔              ${now.format(fFecha)}\n")
        val hh = intToRoman(now.hour)
        val mm = intToRoman(now.minute)
        val ss = intToRoman(now.second)
        sb.append("HORA LOCAL ➔         ${now.format(fHora)}\n")
        sb.append("HORA ROMANA ➔        $hh:$mm:$ss\n")
        sb.append("CIUDAD Y CLIMA ➔     $climaInfo\n")
        sb.append("UBICACIÓN GPS ➔      ${lat.format(4)}, ${lng.format(4)}\n")
        val spannable = android.text.SpannableString(sb.toString())
        val lines = sb.toString().split("\n")
        var pos = 0
        for (line in lines) {
            if (line.startsWith("══")) {
                spannable.setSpan(
                    android.text.style.ForegroundColorSpan(Color.YELLOW),
                    pos,
                    pos + line.length,
                    android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }
            pos += line.length + 1
        }
        binding.txtDatosBrutos.text = spannable
        val matrix = ColorMatrix(
            floatArrayOf(
                3.5f,
                0f,
                0f,
                0f,
                60f,
                0f,
                3.5f,
                0f,
                0f,
                60f,
                0f,
                0f,
                3.5f,
                0f,
                60f,
                0f,
                0f,
                0f,
                1f,
                0f
            )
        )
        binding.imgLunaIluminada.colorFilter = ColorMatrixColorFilter(matrix)
        binding.imgLunaFondo.alpha = 0.55f
        binding.vistaSombra.actualizar(mi.fraction, mi.phase)
        val porcentajeTexto = "${(mi.fraction * 100).format(2)}%"
        binding.txtPorcentajeLuna.text = porcentajeTexto
    }

    private fun calcularFaseDetallada(frac: Double, phase: Double): String {
        val pct = (frac * 100)
        val waxing = phase < 0.5
        return when {
            pct < 5.0 -> "LUNA NUEVA"
            pct >= 5.0 && pct < 50.0 -> if (waxing) "CUARTO CRECIENTE" else "CUARTO MENGUANTE"
            pct >= 50.0 && pct < 95.0 -> if (waxing) "GIBOSA CRECIENTE" else "GIBOSA MENGUANTE"
            pct >= 95.0 -> "LUNA LLENA"
            else -> "LUNA NUEVA"
        }
    }

    private fun Double.format(digits: Int) = "%.${digits}f".format(this)
    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) activarModoInmersivo()
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(tick)
        handler.removeCallbacks(motorEstrellas)
        handler.removeCallbacks(motorVozAutomatico)
        musicaFondo?.stop()
        musicaFondo?.release()
        tts?.shutdown()
        astronautaActivo?.let {
            it.animate().cancel()
            (it.parent as? FrameLayout)?.removeView(it)
            astronautaActivo = null
        }
    }

    override fun onResume() {
        super.onResume()
        activarModoInmersivo()
        if (musicaFondo?.isPlaying == false) musicaFondo?.start()
        handler.post(tick)
        handler.post(motorEstrellas)
        handler.post(motorVozAutomatico)
        binding.root.post {
            generarCieloEstrellado()
            if (overlayAstronauta == null) {
                overlayAstronauta = FrameLayout(this)
                overlayAstronauta!!.layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT
                )
                val root = findViewById<FrameLayout>(android.R.id.content)
                root.addView(overlayAstronauta)
            }
            lanzarAstronauta()
        }
    }

    override fun onPause() {
        super.onPause()
        if (musicaFondo?.isPlaying == true) musicaFondo?.pause()
        handler.removeCallbacksAndMessages(null)
        astronautaActivo?.let {
            it.animate().cancel()
            (it.parent as? FrameLayout)?.removeView(it)
            astronautaActivo = null
        }
        tts?.stop()
    }
}