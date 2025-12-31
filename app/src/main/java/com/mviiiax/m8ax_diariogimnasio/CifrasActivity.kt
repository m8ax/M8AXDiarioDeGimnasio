package com.mviiiax.m8ax_diariogimnasio

import android.app.AlertDialog
import android.graphics.Color
import android.media.MediaPlayer
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.util.Locale
import kotlin.concurrent.thread
import kotlin.random.Random

class CifrasActivity : AppCompatActivity(), TextToSpeech.OnInitListener {
    private lateinit var numerosTextView: TextView
    private lateinit var statsTextView: TextView
    private var paused = false
    private val handler = Handler(Looper.getMainLooper())
    private val allowedNumbers = listOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 25, 50, 75, 100)
    private var totalAttempts = 0L
    private var totalFailures = 0L
    private var totalSuccess = 0L
    private var totalOperations = 0L
    private var nosuccess = 0L
    private var startTime = android.os.SystemClock.elapsedRealtime()
    private lateinit var pauseButton: Button
    private lateinit var manualButton: Button
    private var mediaPlayer: MediaPlayer? = null
    private var tts: TextToSpeech? = null
    private var ttsEnabled = false
    private val ttsHandler = Handler(Looper.getMainLooper())
    private var ttsRunnable: Runnable? = null
    private var distanciaTotalKm = 0.0
    private var useClassic: Boolean = true
    private var running = true
    private fun resetStats() {
        totalAttempts = 0L
        totalFailures = 0L
        totalSuccess = 0L
        nosuccess = 0L
        totalOperations = 0L
        distanciaTotalKm = 0.0
        startTime = android.os.SystemClock.elapsedRealtime()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val prefs = getSharedPreferences("M8AX-Config_TTS", MODE_PRIVATE)
        ttsEnabled = prefs.getBoolean("tts_enabled", false)
        if (ttsEnabled) tts = TextToSpeech(this, this)
        mediaPlayer = MediaPlayer.create(this, R.raw.m8axsonidofondo)
        mediaPlayer?.isLooping = true
        mediaPlayer?.setVolume(1.0f, 1.0f)
        mediaPlayer?.start()
        window.decorView.systemUiVisibility =
            View.SYSTEM_UI_FLAG_IMMERSIVE or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_FULLSCREEN
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        val rootLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.BLACK)
        }
        val numerosScroll = ScrollView(this).apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f)
        }
        numerosTextView = TextView(this).apply { setTextColor(Color.WHITE) }
        numerosScroll.addView(numerosTextView)
        rootLayout.addView(numerosScroll)
        val statsScroll = ScrollView(this).apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f)
        }
        statsTextView = TextView(this).apply { setTextColor(Color.WHITE) }
        statsScroll.addView(statsTextView)
        rootLayout.addView(statsScroll)
        val buttonLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.END
        }
        pauseButton = Button(this).apply {
            text = "Pausar"
            setOnClickListener {
                paused = !paused
                val nuevoTexto = if (paused) "Continuar" else "Pausar"
                val nuevoTexto2 = if (paused) "Pausar" else "Continuar"
                text = nuevoTexto
                if (paused) {
                    mediaPlayer?.pause()
                } else {
                    mediaPlayer?.start()
                    resetStats()
                }
                if (ttsEnabled) {
                    tts?.stop()
                    tts?.speak(
                        nuevoTexto2, TextToSpeech.QUEUE_FLUSH, null, "ttsPauseResume"
                    )
                }
            }
        }
        buttonLayout.addView(pauseButton)
        manualButton = Button(this).apply {
            text = "Ingresar Números"
            setOnClickListener { showManualInputDialog() }
        }
        val modoButton = Button(this).apply {
            text = if (useClassic) "Modo Clásico" else "Modo Brutal"
            setOnClickListener {
                useClassic = !useClassic
                val nuevoTexto = if (useClassic) "Modo Clásico" else "Modo Brutal"
                text = nuevoTexto
                resetStats()
                if (ttsEnabled) {
                    tts?.stop()
                    tts?.speak(nuevoTexto, TextToSpeech.QUEUE_FLUSH, null, "ttsModo")
                }
            }
        }
        val params = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            gravity = Gravity.START or Gravity.BOTTOM
            setMargins(0, 0, 20, 20)
        }
        val decorView = window.decorView as FrameLayout
        decorView.addView(modoButton, params)
        buttonLayout.addView(manualButton)
        rootLayout.addView(buttonLayout)
        setContentView(rootLayout)
        startCalculationLoop()
        startStatsUpdater()
        startTTSLoop()
        mostrarFechaInferiorOverlay()
    }

    private fun mostrarFechaInferiorOverlay() {
        val formatter = java.time.format.DateTimeFormatter.ofPattern("EEEE, dd/MM/yyyy - HH:mm:ss")
        val overlay = object : View(this) {
            var fechaTexto = java.time.LocalDateTime.now().format(formatter)
                .replaceFirstChar { it.uppercaseChar() }

            override fun onDraw(canvas: android.graphics.Canvas) {
                super.onDraw(canvas)
                val paint = android.graphics.Paint().apply {
                    color = android.graphics.Color.parseColor("#66CCFF")
                    textSize = 50f
                    textAlign = android.graphics.Paint.Align.CENTER
                    isAntiAlias = true
                }
                paint.textAlign = android.graphics.Paint.Align.CENTER
                canvas.drawText("By M8AX - $fechaTexto", (width / 2f) - 160f, height - 50f, paint)
            }
        }
        val rootView = window.decorView as ViewGroup
        rootView.addView(
            overlay, ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT
            )
        )
        val handler = Handler(Looper.getMainLooper())
        val runnable = object : Runnable {
            override fun run() {
                overlay.fechaTexto = java.time.LocalDateTime.now().format(formatter)
                    .replaceFirstChar { it.uppercaseChar() }
                overlay.invalidate()
                handler.postDelayed(this, 1000)
            }
        }
        handler.post(runnable)
    }

    private fun startTTSLoop() {
        if (!ttsEnabled) return
        ttsRunnable = object : Runnable {
            override fun run() {
                if (!paused) {
                    val total = totalSuccess + nosuccess
                    val porcentaje = if (total > 0) totalSuccess * 100 / total else 0
                    val elapsedSec =
                        ((android.os.SystemClock.elapsedRealtime() - startTime) / 1000).toInt()
                    val horas = elapsedSec / 3600
                    val mins = (elapsedSec % 3600) / 60
                    val segundos = elapsedSec % 60
                    val texto =
                        "Números Objetivo Resueltos Exactamente; $totalSuccess De $total; Aciertos; $porcentaje Por Ciento; Tiempo transcurrido; $horas Horas, $mins Minutos Y $segundos Segundos."
                    tts?.stop()
                    tts?.speak(texto, TextToSpeech.QUEUE_FLUSH, null, "tts_m8ax")
                }
                ttsHandler.postDelayed(this, 60_000)
            }
        }
        ttsHandler.postDelayed(ttsRunnable!!, 60_000)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts?.language = Locale.getDefault()
            tts?.setSpeechRate(0.9f)
        }
    }

    override fun onPause() {
        super.onPause(); mediaPlayer?.pause()
        running = false
    }

    override fun onResume() {
        super.onResume(); if (!paused) mediaPlayer?.start()
        running = true
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer?.release()
        mediaPlayer = null
        tts?.shutdown()
        running = true
    }

    private fun calcularDistanciaEnKm(cadena: String, cmPorCaracter: Double = 0.25): Double {
        val longitudCm = cadena.length * cmPorCaracter
        return longitudCm / 100_000.0
    }

    private fun startCalculationLoop() {
        thread {
            var counter = 0
            while (running) {
                if (!paused) {
                    val numbers: List<Int>
                    val target: Int
                    if (useClassic) {
                        numbers = allowedNumbers.shuffled().take(6)
                        target = Random.nextInt(100, 1000)
                    } else {
                        numbers = List(6) {
                            val chance = Random.nextInt(100)
                            if (chance < 70) {
                                Random.nextInt(1, 71)
                            } else {
                                listOf(75, 100, 150, 200, 250, 300, 350, 400, 450, 500).random()
                            }
                        }
                        target = Random.nextInt(100, 10_000)
                    }
                    val (expr, res, attempts, failures, exact) = bfsExact(numbers, target)
                    totalAttempts += attempts
                    totalFailures += failures
                    totalOperations += (attempts + failures)
                    if (exact) totalSuccess++ else nosuccess++
                    distanciaTotalKm += calcularDistanciaEnKm(expr)
                    val emoji = if (exact) "😃" else "😡"
                    val displayText = buildString {
                        append("---------------------------------------------------------------------------------\n")
                        append("M8AX - Números Seleccionados: $numbers\n")
                        append("M8AX - Número Objetivo: $target\n")
                        append("M8AX - Solución: $expr = $emoji $res $emoji\n")
                    }
                    handler.post {
                        numerosTextView.append(displayText)
                        (numerosTextView.parent as ScrollView).post {
                            (numerosTextView.parent as ScrollView).fullScroll(View.FOCUS_DOWN)
                        }
                        counter++
                        if (counter >= 1000) {
                            numerosTextView.text = ""
                            counter = 0
                        }
                    }
                }
                Thread.yield()
            }
        }
    }

    private fun startStatsUpdater() {
        val statsHandler = Handler(Looper.getMainLooper())
        val runnable = object : Runnable {
            override fun run() {
                val elapsed = (android.os.SystemClock.elapsedRealtime() - startTime) / 1000.0
                val opsPerSec = if (elapsed > 0) totalOperations / elapsed else 0.0
                val successRate =
                    if (totalSuccess + nosuccess > 0) totalSuccess * 100.0 / (totalSuccess + nosuccess) else 0.0
                val metrosPorSegundo = if (elapsed > 0) distanciaTotalKm * 1000 / elapsed else 0.0
                val statsText = buildString {
                    append("M8AX - Intentos Fallidos: $totalFailures\n")
                    append("M8AX - Intentos Factibles: $totalAttempts\n")
                    append("M8AX - Operaciones Totales: $totalOperations\n")
                    append("M8AX - Operaciones Totales Por Segundo: %.2f\n".format(opsPerSec))
                    append("M8AX - Nº Objetivo Exacto: $totalSuccess De ${totalSuccess + nosuccess}\n")
                    append(
                        "M8AX - Total De Nºs Objetivo Exactos Por Segundo: %.2f\n".format(
                            totalSuccess / elapsed
                        )
                    )
                    append(
                        "M8AX - Total De Nºs Objetivo Casi Exactos Por Segundo: %.2f\n".format(
                            nosuccess / elapsed
                        )
                    )
                    append("M8AX - Porcentaje De Aciertos: %.2f %%\n".format(successRate))
                    append(
                        "M8AX - Soluciones Y No Soluciones Escritas Ocupan En Linea Recta: %.5f Kilómetros\n".format(
                            distanciaTotalKm
                        )
                    )
                    append(
                        "M8AX - Metros Por Segundo De Soluciones: ( %.5f m/s ) - ( %.2f Km/h )\n".format(
                            metrosPorSegundo, metrosPorSegundo * 3.6
                        )
                    )
                    val sec = elapsed.toInt()
                    val dias = sec / (24 * 3600)
                    val horas = (sec % (24 * 3600)) / 3600
                    val minutos = (sec % 3600) / 60
                    val segundos = sec % 60
                    append("M8AX - Tiempo Transcurrido: $dias D, $horas H, $minutos M, $segundos S.\n")
                }
                statsTextView.text = statsText
                statsHandler.postDelayed(this, 500)
            }
        }
        statsHandler.post(runnable)
    }

    private fun hablarSolo(mensaje: String) {
        if (ttsEnabled) {
            tts?.stop()
            tts?.speak(mensaje, TextToSpeech.QUEUE_FLUSH, null, "ttsError")
        }
    }

    private fun showManualInputDialog() {
        val builder = AlertDialog.Builder(this)
        if (ttsEnabled) tts?.speak(
            "Introduce 6 Números Separados Por Coma; Y Objetivo Separado Por Punto Y Coma.",
            TextToSpeech.QUEUE_FLUSH,
            null,
            "ttsFlexionesId"
        )
        builder.setTitle("Introduce 6 Números Separados Por Coma Y Objetivo Separado Por ;")
        val input = EditText(this).apply {
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.BLACK)
            setPadding(20, 20, 20, 20)
            hint = "Ej: 75,50,8,6,3,1;527"
        }
        builder.setView(input)
        builder.setPositiveButton("Calcular") { _, _ ->
            if (ttsEnabled) tts?.speak(
                "Adelante, Calculemos", TextToSpeech.QUEUE_FLUSH, null, "ttsCalcular"
            )
            val text = input.text.toString().trim()
            if (text.isEmpty()) {
                hablarSolo("No Has Escrito Nada"); return@setPositiveButton
            }
            val separador = text.indexOf(';')
            if (separador == -1 || separador == text.lastIndex) {
                hablarSolo("Falta El Punto Y Coma O El Objetivo"); return@setPositiveButton
            }
            val numerosTexto = text.substring(0, separador).trim()
            val objetivoTexto = text.substring(separador + 1).trim()
            val target = objetivoTexto.toIntOrNull()
                ?: run { hablarSolo("El Objetivo $objetivoTexto No Es Válido"); return@setPositiveButton }
            val numsStr = numerosTexto.split(",").map { it.trim() }
            if (numsStr.size != 6) {
                hablarSolo("Tienes ${numsStr.size} Números. Debes Poner Exactamente Seis"); return@setPositiveButton
            }
            val nums = numsStr.mapNotNull { it.toIntOrNull() }
            if (nums.size != 6) {
                hablarSolo("No Entiendo Estos Valores: ${numsStr.filter { it.toIntOrNull() == null }}"); return@setPositiveButton
            }
            thread {
                val (expr, res, _, _, exact) = bfsExact(nums, target)
                val emoji = if (exact) "😃 $res 😃" else "😡 $res 😡"
                handler.post {
                    val mensaje =
                        "Números: ${nums.joinToString(", ")}\nObjetivo: $target\n\n" + if (exact) "¡ Solución Exacta !\n\n$expr = $res\n\n$emoji"
                        else "Mejor Posible:\n\n$expr = $res\n\n( Diferencia ${
                            kotlin.math.abs(
                                target - res
                            )
                        } ) - $emoji"
                    AlertDialog.Builder(this).setTitle("Resultado").setMessage(mensaje)
                        .setPositiveButton("Cerrar", null).show()
                    if (ttsEnabled) {
                        if (exact) {
                            tts?.stop()
                            tts?.speak(
                                "Has Llegado Al Número $res Exacto.",
                                TextToSpeech.QUEUE_FLUSH,
                                null,
                                "ttsExact"
                            )
                        } else {
                            val diff = kotlin.math.abs(target - res)
                            tts?.stop()
                            tts?.speak(
                                "Has Llegado Al Número $res; Diferencia Con El Número Objetivo; $diff.",
                                TextToSpeech.QUEUE_FLUSH,
                                null,
                                "ttsNoExact"
                            )
                        }
                    }
                }
            }
        }
        builder.setNegativeButton("Cancelar") { _, _ ->
            if (ttsEnabled) tts?.speak(
                "Cancelando", TextToSpeech.QUEUE_FLUSH, null, "ttsCancelar"
            )
        }
        builder.setOnCancelListener {
            if (ttsEnabled) tts?.speak(
                "Cancelando", TextToSpeech.QUEUE_FLUSH, null, "ttsCancelarBack"
            )
        }
        builder.show()
    }

    private object MotorM8AX {
        fun resolver(nums: List<Int>, target: Int): Quintuple<String, Int, Long, Long, Boolean> {
            data class Estado(val valores: List<Int>, val pasos: String, val usados: Int)

            val queue = ArrayDeque<Estado>()
            queue.add(Estado(nums, "", 0))
            var mejorExpr = ""
            var mejorRes = 0
            var mejorDiff = Int.MAX_VALUE
            var exacto = false
            var attempts = 0L
            var failures = 0L
            val visitados = mutableSetOf<Pair<List<Int>, Int>>()
            while (queue.isNotEmpty()) {
                val actual = queue.removeFirst()
                attempts++
                if (actual.valores.size == 1) {
                    val valor = actual.valores[0]
                    val diff = kotlin.math.abs(valor - target)
                    if (diff == 0) {
                        exacto = true
                        mejorExpr = actual.pasos.ifEmpty { valor.toString() }
                        mejorRes = valor
                        break
                    }
                    if (diff < mejorDiff) {
                        mejorDiff = diff
                        mejorRes = valor
                        mejorExpr = actual.pasos.ifEmpty { valor.toString() }
                    }
                    continue
                }
                for (i in actual.valores.indices) {
                    if (actual.valores[i] == target) {
                        exacto = true
                        mejorExpr = actual.valores[i].toString()
                        mejorRes = target
                        return Quintuple(mejorExpr, mejorRes, attempts + 10, failures, exacto)
                    }
                }
                val clave = actual.valores.sorted() to actual.usados
                if (clave in visitados) continue
                visitados.add(clave)
                for (i in actual.valores.indices) {
                    for (j in i + 1 until actual.valores.size) {
                        val a = actual.valores[i]
                        val b = actual.valores[j]
                        val resto = actual.valores.filterIndexed { k, _ -> k != i && k != j }
                        val ops = mutableListOf<Triple<Int, String, String>>()
                        ops += Triple(a + b, "+", "($a + $b) = ${a + b}")
                        ops += Triple(a * b, "×", "($a × $b) = ${a * b}")
                        if (a >= b) ops += Triple(a - b, "-", "($a - $b) = ${a - b}")
                        if (b != 0 && a % b == 0) ops += Triple(a / b, "÷", "($a ÷ $b) = ${a / b}")
                        else failures++
                        for ((res, _, paso) in ops) {
                            if (res == target) {
                                val exprFinal =
                                    if (actual.pasos.isEmpty()) paso else actual.pasos + " | " + paso
                                return Quintuple(exprFinal, target, attempts + 20, failures, true)
                            }
                            val nuevoPaso =
                                if (actual.pasos.isEmpty()) paso else actual.pasos + " | " + paso
                            queue.add(Estado(resto + res, nuevoPaso, actual.usados + 2))
                        }
                    }
                }
            }
            return Quintuple(mejorExpr, mejorRes, attempts, failures, exacto)
        }
    }

    data class Quintuple<A, B, C, D, E>(val a: A, val b: B, val c: C, val d: D, val e: E)

    private fun bfsExact(
        numbers: List<Int>, target: Int
    ): Quintuple<String, Int, Long, Long, Boolean> {
        return MotorM8AX.resolver(numbers, target)
    }
}