package com.mviiiax.m8ax_diariogimnasio

import android.app.Activity
import android.app.AlertDialog
import android.content.SharedPreferences
import android.media.MediaPlayer
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import java.math.BigInteger
import java.util.Calendar
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.math.max
import kotlin.random.Random

class FactorizacionActivity : Activity() {
    private var ttsEnabled = false
    private var tts: TextToSpeech? = null
    private var mediaPlayer: MediaPlayer? = null
    private lateinit var scrollView: ScrollView
    private lateinit var tvNumeros: TextView
    private lateinit var tvStats: TextView
    private lateinit var progressBar: ProgressBar
    private val handler = Handler(Looper.getMainLooper())
    private var totalFactores = 0
    private var operacionesTotales: Long = 0L
    private var inicioTiempo: Long = 0
    private var totalNumeros = 0
    private var digitosNumero = 0
    private val MAX_LINES_VISIBLE = 500
    private val numerosVisible = mutableListOf<String>()
    private val numerosPorActualizar = mutableListOf<Pair<String, String>>()
    private var pool: ExecutorService? = null
    private var ttsRunnable: Runnable? = null
    private lateinit var frameLayout: FrameLayout
    private lateinit var layoutFactorizacion: LinearLayout
    private var totalCaracteres: Long = 0L
    private var distanciaTotalKm: Double = 0.0
    private val CM_POR_CARACTER = 0.25
    private lateinit var botonFactorizar: Button
    private val MAX_DIGITOS: Int
        get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) 50 else 18

    private fun caracteresAKm(caracteres: Long): Double {
        val cm = caracteres * CM_POR_CARACTER
        return cm / 100_000.0
    }

    private fun cargarLayoutFactorizacion() {
        if (::layoutFactorizacion.isInitialized) {
            layoutFactorizacion.visibility = View.VISIBLE
            return
        }
        layoutFactorizacion = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(20, 20, 20, 20)
            visibility = View.VISIBLE
        }
        tvNumeros.apply {
            text = "||| ... Esperando Para Factorizar ... |||\n\n"
            textSize = 16f
            setTextColor(android.graphics.Color.WHITE)
            (parent as? ViewGroup)?.removeView(this)
        }
        layoutFactorizacion.addView(tvNumeros)
        progressBar.visibility = View.GONE
        val root = findViewById<FrameLayout>(android.R.id.content)
        root.addView(layoutFactorizacion)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.decorView.systemUiVisibility =
            (View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_FULLSCREEN)
        window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        frameLayout = FrameLayout(this)
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setBackgroundColor(android.graphics.Color.BLACK)
        }
        progressBar = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply {
            max = 100
            progressDrawable.setColorFilter(
                android.graphics.Color.GREEN, android.graphics.PorterDuff.Mode.SRC_IN
            )
        }
        layout.addView(progressBar)
        tvStats = TextView(this).apply {
            setTextColor(android.graphics.Color.GREEN)
            textSize = 16f
            gravity = Gravity.CENTER
            setPadding(10, 10, 10, 10)
        }
        layout.addView(tvStats)
        scrollView = ScrollView(this).apply { setBackgroundColor(android.graphics.Color.BLACK) }
        tvNumeros = TextView(this).apply { setTextColor(android.graphics.Color.WHITE) }
        scrollView.addView(tvNumeros)
        layout.addView(scrollView)
        frameLayout.addView(layout)
        agregarBotonFlotante(frameLayout)
        setContentView(frameLayout)
        val prefs: SharedPreferences = getSharedPreferences("M8AX-Config_TTS", MODE_PRIVATE)
        ttsEnabled = prefs.getBoolean("tts_enabled", false)
        if (ttsEnabled) {
            tts = TextToSpeech(this) { status ->
                if (status == TextToSpeech.SUCCESS) {
                    tts?.language = Locale.getDefault()
                    tts?.setSpeechRate(0.9f)
                }
            }
        }
        mediaPlayer = MediaPlayer.create(this, R.raw.m8axsonidofondo).apply {
            isLooping = true
            start()
        }
        pedirDatos()
    }

    private fun agregarBotonFlotante(frameLayout: FrameLayout) {
        botonFactorizar = Button(this).apply {
            text = "Factorizar Número"
            setTextColor(android.graphics.Color.WHITE)
            setBackgroundColor(android.graphics.Color.DKGRAY)
            textSize = 16f
            setPadding(20, 20, 20, 20)
            setOnClickListener {
                val editNumero = EditText(this@FactorizacionActivity).apply {
                    hint = "Número ( Máx $MAX_DIGITOS Dígitos )"
                    inputType = android.text.InputType.TYPE_CLASS_NUMBER
                }
                val layoutDialog = LinearLayout(this@FactorizacionActivity).apply {
                    orientation = LinearLayout.VERTICAL
                    setPadding(50, 40, 50, 10)
                    addView(editNumero)
                }
                AlertDialog.Builder(this@FactorizacionActivity)
                    .setTitle("Factorización Instantánea").setView(layoutDialog)
                    .setPositiveButton("Aceptar") { _, _ ->
                        val numText = editNumero.text.toString()
                        if (numText.isEmpty()) return@setPositiveButton
                        val numLimite =
                            if (numText.length > MAX_DIGITOS) numText.take(MAX_DIGITOS) else numText
                        Thread {
                            val (factores, numeroStr) = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                                val numero = BigInteger(numLimite)
                                val f = factorizarPollardRho(numero)
                                val factorString = if (numero.isProbablePrime(20)) "( ES PRIMO )"
                                else f.groupingBy { it }.eachCount()
                                    .map { (n, c) -> if (c > 1) "$n^$c" else n.toString() }
                                    .joinToString(" * ")
                                f to "$numero ▶ $factorString ◀"
                            } else {
                                val numero = numLimite.toLongOrNull() ?: return@Thread
                                val f = factorizarLong(numero)
                                val factorString = if (esPrimo(numero)) "( ES PRIMO )"
                                else f.groupingBy { it }.eachCount()
                                    .map { (n, c) -> if (c > 1) "$n^$c" else n.toString() }
                                    .joinToString(" * ")
                                f to "$numero ▶ $factorString ◀"
                            }
                            handler.post {
                                if (ttsEnabled) {
                                    tts?.stop()
                                    tts?.speak(
                                        "Aquí Tienes El Resultado;",
                                        TextToSpeech.QUEUE_FLUSH,
                                        null,
                                        "M8AX_RESULT"
                                    )
                                }
                                if (tvNumeros.lineCount >= 19) {
                                    tvNumeros.text =
                                        "||| ... Continuando Factorizaciones ... |||\n\n"
                                }
                                tvNumeros.append("$numeroStr\n")
                            }
                        }.start()
                    }.setNegativeButton("Cancelar") { dialog, _ ->
                        if (ttsEnabled) {
                            tts?.stop()
                            tts?.speak("Adiós", TextToSpeech.QUEUE_FLUSH, null, "M8AX_CANCEL")
                        }
                        dialog.dismiss()
                    }.setCancelable(false).create().show()
                if (ttsEnabled) {
                    tts?.stop()
                    tts?.speak(
                        "Pon Un Número De Máximo $MAX_DIGITOS Dígitos; Y Te Lo Factorizo",
                        TextToSpeech.QUEUE_FLUSH,
                        null,
                        "M8AX_PROMPT"
                    )
                }
            }
        }
        frameLayout.addView(
            botonFactorizar, FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.BOTTOM or Gravity.END
            ).apply { setMargins(0, 0, 30, 30) })
    }

    private fun pedirDatos() {
        val layoutDialog = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(50, 40, 50, 10)
        }
        val editDigitos = EditText(this).apply {
            hint = "Número De Dígitos ( Max $MAX_DIGITOS )..."
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
        }
        layoutDialog.addView(editDigitos)
        val editCantidad = EditText(this).apply {
            hint = "Cantidad De Números ( Max 1.000.000 )..."
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
        }
        layoutDialog.addView(editCantidad)
        val alertDialog =
            AlertDialog.Builder(this).setTitle("--- Factorización ---").setView(layoutDialog)
                .setPositiveButton("Iniciar") { _, _ ->
                    digitosNumero =
                        editDigitos.text.toString().toIntOrNull()?.coerceAtMost(MAX_DIGITOS) ?: 10
                    var cantidad = editCantidad.text.toString().toIntOrNull() ?: 5
                    if (cantidad > 1_000_000) {
                        if (ttsEnabled) {
                            tts?.stop()
                            tts?.speak(
                                "No Te Pases; Máximo Un Millón.",
                                TextToSpeech.QUEUE_FLUSH,
                                null,
                                "M8AX_CANCELAR"
                            )
                        }
                        cantidad = 1_000_000
                        Toast.makeText(
                            this,
                            "Se Ha Limitado La Cantidad Máxima A 1.000.000 De Números",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                    totalNumeros = cantidad
                    inicioTiempo = System.currentTimeMillis()
                    ejecutarFactorizacion(digitosNumero, totalNumeros)
                }.setNegativeButton("Factorizar Números Sueltos") { dialog, _ ->
                    if (ttsEnabled) {
                        tts?.stop()
                        tts?.speak("Allá Voy!", TextToSpeech.QUEUE_FLUSH, null, "M8AX_CANCELAR")
                    }
                    dialog.dismiss()
                    cargarLayoutFactorizacion()
                }.setCancelable(false).create()
        alertDialog.show()
    }

    private fun ejecutarFactorizacion(maxCifras: Int, totalNumeros: Int) {
        botonFactorizar.isEnabled = false
        botonFactorizar.visibility = View.INVISIBLE
        val cores: Int
        val usedCores: Int
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q) {
            if (maxCifras >= 16) {
                cores = Runtime.getRuntime().availableProcessors()
                usedCores = max(1, cores - 1)
            } else {
                cores = 1
                usedCores = cores
            }
        } else {
            cores = Runtime.getRuntime().availableProcessors()
            usedCores = max(1, cores - 1)
        }
        val numerosPorHilo = totalNumeros / usedCores
        val resto = totalNumeros % usedCores
        pool = Executors.newFixedThreadPool(usedCores)
        for (h in 1..usedCores) {
            val cantidad = if (h == usedCores) numerosPorHilo + resto else numerosPorHilo
            pool?.submit {
                for (i in 1..cantidad) {
                    val (numeroStr, factoresStr) = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        val numero = generarNumeroBig(maxCifras)
                        val f = factorizarPollardRho(numero)
                        numero.toString() to if (numero.isProbablePrime(20)) "( ES PRIMO )"
                        else f.groupingBy { it }.eachCount()
                            .map { (n, c) -> if (c > 1) "$n^$c" else n.toString() }
                            .joinToString(" * ")
                    } else {
                        val numero = generarNumeroLong(maxCifras)
                        val f = factorizarLong(numero)
                        numero.toString() to if (esPrimo(numero)) "( ES PRIMO )"
                        else f.groupingBy { it }.eachCount()
                            .map { (n, c) -> if (c > 1) "$n^$c" else n.toString() }
                            .joinToString(" * ")
                    }
                    synchronized(numerosPorActualizar) {
                        numerosPorActualizar.add(numeroStr to factoresStr)
                    }
                    if (i % 10 == 0) Thread.yield()
                }
            }
        }
        Thread {
            pool?.shutdown()
            if (ttsEnabled) iniciarTTSMinuto()
            while (pool?.isTerminated == false || numerosPorActualizar.isNotEmpty()) {
                Thread.sleep(500)
                val batch = mutableListOf<Pair<String, String>>()
                synchronized(numerosPorActualizar) {
                    if (numerosPorActualizar.isNotEmpty()) {
                        batch.addAll(numerosPorActualizar)
                        numerosPorActualizar.clear()
                    }
                }
                if (batch.isNotEmpty()) handler.post { actualizarUI(batch) }
            }
            handler.post {
                ttsRunnable?.let { handler.removeCallbacks(it) }
                actualizarUIFinal()
                mostrarEstadisticasFinales()
            }
        }.start()
    }

    private fun iniciarTTSMinuto() {
        ttsRunnable = object : Runnable {
            override fun run() {
                val now = Calendar.getInstance()
                val horaActual = String.format(
                    "%02d:%02d:%02d",
                    now.get(Calendar.HOUR_OF_DAY),
                    now.get(Calendar.MINUTE),
                    now.get(Calendar.SECOND)
                )
                val tiempoSegs = (System.currentTimeMillis() - inicioTiempo) / 1000.0
                val factPorSeg = if (tiempoSegs > 0) totalFactores / tiempoSegs else 0.0
                val opsPorSeg =
                    if (tiempoSegs > 0) operacionesTotales.toDouble() / tiempoSegs else 0.0
                val tiempoHHMM = String.format(
                    "%02d Horas Y %02d Minutos.",
                    (tiempoSegs.toInt() / 3600),
                    (tiempoSegs.toInt() % 3600) / 60,
                )
                val mensaje =
                    "Hora Actual; $horaActual; Llevamos $totalFactores Números Procesados; Números Factorizados Por Segundo; ${
                        "%.2f".format(
                            factPorSeg
                        )
                    }; Operacioness Por Segundo; ${"%.0f".format(opsPorSeg)}; Tiempo; $tiempoHHMM."
                tts?.speak(mensaje, TextToSpeech.QUEUE_FLUSH, null, "M8AX_PROGRESS")
                handler.postDelayed(this, 60000)
            }
        }
        handler.postDelayed(ttsRunnable!!, 60000)
    }

    private fun actualizarUI(batch: List<Pair<String, String>>) {
        for ((numero, factores) in batch) {
            val linea = "$numero ▶ $factores ◀"
            numerosVisible.add(linea)
            val caracteresLinea = linea.length + 1
            totalCaracteres += caracteresLinea
            distanciaTotalKm += caracteresAKm(caracteresLinea.toLong())
            if (numerosVisible.size > MAX_LINES_VISIBLE) numerosVisible.removeAt(0)
        }
        totalFactores += batch.size
        actualizarBarra()
        tvNumeros.text = numerosVisible.joinToString("\n")
        scrollView.post { scrollView.fullScroll(View.FOCUS_DOWN) }
    }

    private fun actualizarUIFinal() {
        actualizarBarra()
    }

    private fun actualizarBarra() {
        val progreso =
            if (totalNumeros > 0) ((totalFactores.toDouble() / totalNumeros.toDouble()) * 100).toInt()
            else 0
        progressBar.progress = progreso
        val color = when {
            progreso < 60 -> android.graphics.Color.GREEN
            progreso < 85 -> android.graphics.Color.YELLOW
            progreso < 95 -> android.graphics.Color.parseColor("#FFA500")
            else -> android.graphics.Color.RED
        }
        progressBar.progressDrawable.setColorFilter(color, android.graphics.PorterDuff.Mode.SRC_IN)
        val tiempoSegs = (System.currentTimeMillis() - inicioTiempo) / 1000.0
        val factPorSeg = if (tiempoSegs > 0) totalFactores / tiempoSegs else 0.0
        val opsPorSeg = if (tiempoSegs > 0) operacionesTotales.toDouble() / tiempoSegs else 0.0
        val tiempoHHMMSS = String.format(
            "%02d:%02d:%02d",
            tiempoSegs.toInt() / 3600,
            (tiempoSegs.toInt() % 3600) / 60,
            tiempoSegs.toInt() % 60
        )
        val now = Calendar.getInstance()
        val horaActual = String.format(
            "%02d:%02d:%02d",
            now.get(Calendar.HOUR_OF_DAY),
            now.get(Calendar.MINUTE),
            now.get(Calendar.SECOND)
        )
        tvStats.text = String.format(
            "En Curso: %d/%d | N. Fact/s: %.2f | Ops/s: %.0f | DT.Km: %.2f | Hora: %s | Tiempo: %s",
            totalFactores,
            totalNumeros,
            factPorSeg,
            opsPorSeg,
            distanciaTotalKm,
            horaActual,
            tiempoHHMMSS
        )
    }

    private fun mostrarEstadisticasFinales() {
        val tiempoSegs = (System.currentTimeMillis() - inicioTiempo) / 1000
        val kmPorSegundo = if (tiempoSegs > 0) distanciaTotalKm / tiempoSegs.toDouble() else 0.0
        val kmPorHora = kmPorSegundo * 3600.0
        val horas = tiempoSegs / 3600
        val minutos = (tiempoSegs % 3600) / 60
        val segs = tiempoSegs % 60
        val factPorSeg =
            if (tiempoSegs > 0) totalFactores.toDouble() / tiempoSegs else totalFactores.toDouble()
        val opsPorSeg =
            if (tiempoSegs > 0) operacionesTotales.toDouble() / tiempoSegs else operacionesTotales.toDouble()
        numerosVisible.add("\n============ Estadísticas Finales ============\n")
        numerosVisible.add("Total De Números Factorizados -------> $totalFactores / $totalNumeros")
        numerosVisible.add("Dígitos Por Cada Número -------> $digitosNumero")
        numerosVisible.add(
            String.format(
                "Números Factorizados Por Segundo -------> %.2f", factPorSeg
            )
        )
        numerosVisible.add(
            String.format(
                "Operaciones Totales De Cálculo Complejo -------> %d", operacionesTotales
            )
        )
        numerosVisible.add(
            String.format(
                "Operaciones Totales Por Segundo De Cálculo Complejo -------> %.2f", opsPorSeg
            )
        )
        numerosVisible.add(
            String.format(
                "Caracteres Totales Escritos -------> %d", totalCaracteres
            )
        )
        numerosVisible.add(
            String.format(
                "Esto Ocuparía En Línea Recta -------> %.6f Kilómetros", distanciaTotalKm
            )
        )
        numerosVisible.add(
            String.format(
                "Velocidad De Escritura Matemática -------> ( %.6f Km/s ) - ( %.6f Km/h )",
                kmPorSegundo,
                kmPorHora
            )
        )
        numerosVisible.add(
            String.format(
                "Tiempo Total -------> %02dh %02dm %02ds", horas, minutos, segs
            )
        )
        numerosVisible.add("\n==============By=====M8AX===============\n\n")
        tvNumeros.text = numerosVisible.joinToString("\n")
        scrollView.post { scrollView.fullScroll(View.FOCUS_DOWN) }
        if (ttsEnabled) {
            val statsFinales = """
        Factorización Completa; Estadísticas Finales;
        Total De Números Factorizados; $totalFactores De $totalNumeros
        ;Dígitos Por Cada Número; $digitosNumero
        ;Números Factorizados Por Segundo; ${"%.2f".format(factPorSeg)}
        ;Operaciones Totales De Cálculo Complejo; $operacionesTotales
        ;Operaciones Totales Por Segundo De Cálculo Complejo; ${"%.2f".format(opsPorSeg)}
        ;Caracteres Totales Escritos; $totalCaracteres Caracteres
        ;Esto Ocuparía En Línea Recta; ${"%.6f".format(distanciaTotalKm)} Kilómetros
        ;Velocidad De Escritura Matemática; ${"%.6f".format(kmPorSegundo)} Kilómetros Por Segundo
        ;O Lo Que Es Lo Mismo; ${"%.6f".format(kmPorHora)} Kilómetros Por Hora
        ;Tiempo Total; ${
                String.format(
                    "%02d Horas, %02d Minutos Y %02d Segundos.", horas, minutos, segs
                )
            }
    """.trimIndent()
            tts?.speak(statsFinales, TextToSpeech.QUEUE_FLUSH, null, "M8AX_STATS_FINAL")
        }
        pool?.shutdownNow()
        handler.removeCallbacksAndMessages(null)
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
        botonFactorizar.isEnabled = true
        botonFactorizar.visibility = View.VISIBLE
    }

    private fun generarNumeroBig(cifras: Int): BigInteger {
        val builder = StringBuilder()
        builder.append(Random.nextInt(1, 10))
        repeat(cifras - 1) { builder.append(Random.nextInt(0, 10)) }
        return BigInteger(builder.toString())
    }

    private fun generarNumeroLong(cifras: Int): Long {
        val builder = StringBuilder()
        builder.append(Random.nextInt(1, 10))
        repeat(cifras - 1) { builder.append(Random.nextInt(0, 10)) }
        return builder.toString().toLong()
    }

    private fun factorizarLong(n: Long): List<Long> {
        var num = n
        val factores = mutableListOf<Long>()
        var i = 2L
        while (i * i <= num) {
            while (num % i == 0L) {
                factores.add(i)
                num /= i
                operacionesTotales += 10L
            }
            i++
            operacionesTotales++
        }
        if (num > 1) factores.add(num)
        return factores
    }

    private fun esPrimo(n: Long): Boolean {
        if (n < 2) return false
        if (n == 2L || n == 3L) return true
        if (n % 2L == 0L) return false
        var i = 3L
        while (i * i <= n) {
            if (n % i == 0L) return false
            i += 2
        }
        return true
    }

    private fun factorizarPollardRho(n: BigInteger): List<BigInteger> {
        if (n <= BigInteger.ONE) return emptyList()
        val factores = mutableListOf<BigInteger>()
        var num = n
        val TWO = BigInteger.valueOf(2)
        while (num % TWO == BigInteger.ZERO) {
            factores.add(TWO)
            num = num.divide(TWO)
            operacionesTotales += 10L * num.bitLength()
        }
        var i = BigInteger.valueOf(3)
        val MILLON = BigInteger.valueOf(1_000_000)
        while (i.multiply(i) <= num && i < MILLON) {
            while (num % i == BigInteger.ZERO) {
                factores.add(i)
                num = num.divide(i)
                operacionesTotales += 10L * num.bitLength()
            }
            i = i.add(BigInteger.valueOf(2))
            operacionesTotales += 1L
        }
        if (num > BigInteger.ONE) {
            if (num.isProbablePrime(20)) {
                factores.add(num)
                operacionesTotales += 5L * num.bitLength()
            } else {
                val f = rho(num)
                operacionesTotales += 1L
                if (f == num || f == BigInteger.ONE) {
                    factores.add(num)
                    operacionesTotales += 1L
                } else {
                    factores.addAll(factorizarPollardRho(f))
                    factores.addAll(factorizarPollardRho(num.divide(f)))
                }
            }
        }
        return factores.sorted()
    }

    private fun rho(n: BigInteger): BigInteger {
        val one = BigInteger.valueOf(1)
        val two = BigInteger.valueOf(2)
        if (n.isProbablePrime(20)) return n
        if (n % two == BigInteger.ZERO) return two
        var x = two
        var y = two
        var d = one
        var c = BigInteger.valueOf(3)
        val rnd = Random.Default
        while (d == one) {
            x = (x.multiply(x).add(c)).mod(n)
            operacionesTotales += (5L + 1L + 10L) * n.bitLength()
            y = (y.multiply(y).add(c)).mod(n)
            operacionesTotales += (5L + 1L + 10L) * n.bitLength()
            y = (y.multiply(y).add(c)).mod(n)
            operacionesTotales += (5L + 1L + 10L) * n.bitLength()
            d = x.subtract(y).abs().gcd(n)
            operacionesTotales += 15L * n.bitLength()
        }
        if (d == n) {
            c = BigInteger.valueOf(rnd.nextLong(1, n.toLong()))
            x = two
            y = two
            d = one
            while (d == one) {
                x = (x.multiply(x).add(c)).mod(n)
                operacionesTotales += (5L + 1L + 10L) * n.bitLength()
                y = (y.multiply(y).add(c)).mod(n)
                operacionesTotales += (5L + 1L + 10L) * n.bitLength()
                y = (y.multiply(y).add(c)).mod(n)
                operacionesTotales += (5L + 1L + 10L) * n.bitLength()
                d = x.subtract(y).abs().gcd(n)
                operacionesTotales += 15L * n.bitLength()
            }
        }
        return d
    }

    override fun onDestroy() {
        super.onDestroy()
        pool?.shutdownNow()
        handler.removeCallbacksAndMessages(null)
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
        tts?.stop()
        tts?.shutdown()
        tts = null
    }
}