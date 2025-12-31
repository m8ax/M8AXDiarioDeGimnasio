package com.mviiiax.m8ax_diariogimnasio

import android.content.ClipData
import android.content.ClipboardManager
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.MediaScannerConnection
import android.media.ToneGenerator
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.speech.tts.TextToSpeech
import android.view.View
import android.view.WindowManager
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.*
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.atomic.AtomicLong
import kotlin.math.sqrt

class PrimosActivity : AppCompatActivity() {
    private lateinit var numeroA: EditText
    private lateinit var numeroB: EditText
    private lateinit var botonCalcular: Button
    private lateinit var botonCalcularSimple: Button
    private lateinit var botonGuardar: Button
    private lateinit var primoActualText: TextView
    private lateinit var priSegText: TextView
    private lateinit var porcentajeText: TextView
    private lateinit var resultadosText: TextView
    private lateinit var progresoBar: ProgressBar
    private val ultimosPrimos = LinkedList<Long>()
    private val BUFFER_SIZE = 25000
    private val SEGMENT_SIZE = 10_000_000L
    private var mediaPlayer: MediaPlayer? = null
    private var tts: TextToSpeech? = null
    private var ttsEnabled = false
    private var brilloHandler: Handler? = null
    private var calculandoPrimos = false
    private var brilloEncendido = true
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_primos)
        numeroA = findViewById(R.id.numeroA)
        numeroB = findViewById(R.id.numeroB)
        botonCalcular = findViewById(R.id.botonCalcular)
        botonCalcularSimple = findViewById(R.id.botonCalcularSimple)
        botonGuardar = findViewById(R.id.botonGuardar)
        primoActualText = findViewById(R.id.primoActualText)
        priSegText = findViewById(R.id.priSegText)
        porcentajeText = findViewById(R.id.porcentajeText)
        resultadosText = findViewById(R.id.resultadosText)
        progresoBar = findViewById(R.id.progresoBar)
        val prefs = getSharedPreferences("M8AX-Config_TTS", MODE_PRIVATE)
        ttsEnabled = prefs.getBoolean("tts_enabled", false)
        if (ttsEnabled) {
            tts = TextToSpeech(this) { status ->
                if (status == TextToSpeech.SUCCESS) {
                    tts?.language = Locale.getDefault()
                    tts?.setSpeechRate(0.9f)
                }
            }
        }
        mediaPlayer = MediaPlayer.create(this, R.raw.m8axsonidofondo)
        mediaPlayer?.isLooping = true
        requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        window.decorView.systemUiVisibility =
            (View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION)
        botonCalcular.setOnClickListener {
            val aText = numeroA.text.toString().trim()
            val bText = numeroB.text.toString().trim()
            var a = aText.toLongOrNull()
            var b = bText.toLongOrNull()
            if (a == null || b == null) {
                Toast.makeText(
                    this@PrimosActivity, "Número Demasiado Grande O Inválido", Toast.LENGTH_LONG
                ).show()
                hablar("Número Demasiado Grande O Inválido")
                return@setOnClickListener
            }
            if (a > b) {
                val temp = a
                a = b
                b = temp
                numeroA.setText(a.toString())
                numeroB.setText(b.toString())
            }
            val desde = minOf(a, b)
            val hasta = maxOf(a, b)
            val MAX_SEGURO = Long.MAX_VALUE
            if (desde > MAX_SEGURO || hasta > MAX_SEGURO) {
                Toast.makeText(
                    this@PrimosActivity,
                    "Número Demasiado Grande, Máximo: $MAX_SEGURO",
                    Toast.LENGTH_LONG
                ).show()
                hablar("Número Demasiado Grande")
                return@setOnClickListener
            }
            iniciar(conIntervalos = true, guardarTXT = false)
        }
        botonCalcularSimple.setOnClickListener {
            val aText = numeroA.text.toString().trim()
            val bText = numeroB.text.toString().trim()
            var a = aText.toLongOrNull()
            var b = bText.toLongOrNull()
            if (a == null || b == null) {
                Toast.makeText(
                    this@PrimosActivity, "Número Demasiado Grande O Inválido", Toast.LENGTH_LONG
                ).show()
                hablar("Número Demasiado Grande O Inválido")
                return@setOnClickListener
            }
            if (a > b) {
                val temp = a
                a = b
                b = temp
                numeroA.setText(a.toString())
                numeroB.setText(b.toString())
            }
            val desde = minOf(a, b)
            val hasta = maxOf(a, b)
            val MAX_SEGURO = Long.MAX_VALUE
            if (desde > MAX_SEGURO || hasta > MAX_SEGURO) {
                Toast.makeText(
                    this, "Número Demasiado Grande, Máximo: $MAX_SEGURO", Toast.LENGTH_LONG
                ).show()
                hablar("Número Demasiado Grande")
                return@setOnClickListener
            }
            iniciar(false, false)
        }
        botonGuardar.setOnClickListener {
            val aText = numeroA.text.toString().trim()
            val bText = numeroB.text.toString().trim()
            var a = aText.toLongOrNull()
            var b = bText.toLongOrNull()
            if (a == null || b == null) {
                Toast.makeText(
                    this@PrimosActivity,
                    "Introduce Números Válidos Dentro De ±9,223,372,036,854,775,807",
                    Toast.LENGTH_LONG
                ).show()
                hablar("Número Inválido O Demasiado Grande")
                return@setOnClickListener
            }
            if (a > b) {
                val temp = a
                a = b
                b = temp
                numeroA.setText(a.toString())
                numeroB.setText(b.toString())
            }
            val desde = minOf(a, b)
            val hasta = maxOf(a, b)
            val MAX_LONG = Long.MAX_VALUE
            if (desde < 0 || hasta > MAX_LONG) {
                Toast.makeText(
                    this@PrimosActivity, "Introduce Números Entre 0 Y $MAX_LONG", Toast.LENGTH_LONG
                ).show()
                hablar("Número Fuera De Rango")
                return@setOnClickListener
            }
            iniciar(conIntervalos = true, guardarTXT = true)
        }
        progresoBar.max = 10000
        resultadosText.setOnLongClickListener {
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip =
                ClipData.newPlainText("--- Resultados De Números Primos ---\n", resultadosText.text)
            clipboard.setPrimaryClip(clip)
            Toast.makeText(this, "Texto Copiado Al Portapapeles", Toast.LENGTH_SHORT).show()
            hablar("Texto Copiado Al Portapapeles.")
            true
        }
    }

    fun beepCorto() {
        val tg = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 100)
        tg.startTone(ToneGenerator.TONE_PROP_BEEP, 250)
    }

    override fun onResume() {
        super.onResume()
        mediaPlayer?.start()
        if (calculandoPrimos) {
            startBrilloParpadeo()
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    override fun onPause() {
        super.onPause()
        mediaPlayer?.pause()
        stopBrilloParpadeo()
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer?.release()
        mediaPlayer = null
        tts?.shutdown()
        stopBrilloParpadeo()
    }

    private fun hablar(texto: String) {
        if (ttsEnabled) tts?.speak(texto, TextToSpeech.QUEUE_ADD, null, "tts1")
    }

    private fun iniciar(conIntervalos: Boolean, guardarTXT: Boolean) {
        val a = numeroA.text.toString().trim().toLongOrNull() ?: return
        val b = numeroB.text.toString().trim().toLongOrNull() ?: return
        val desde = minOf(a, b)
        val hasta = maxOf(a, b)
        calculandoPrimos = true
        startBrilloParpadeo()
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        botonCalcular.isEnabled = false
        botonCalcularSimple.isEnabled = false
        botonGuardar.isEnabled = false
        numeroA.isEnabled = false
        numeroB.isEnabled = false
        resultadosText.text = ""
        porcentajeText.text = "0%"
        progresoBar.progress = 0
        ultimosPrimos.clear()
        CoroutineScope(Dispatchers.Default).launch {
            hablar("Iniciando Cálculo De Primos")
            val resultado = calcular(desde, hasta, conIntervalos)
            if (guardarTXT) {
                hablar("Generando T X T; Con Todos Los Primos")
                calcularParaGuardar(desde, hasta)
            }
            withContext(Dispatchers.Main) {
                botonCalcular.isEnabled = true
                botonCalcularSimple.isEnabled = true
                botonGuardar.isEnabled = true
                numeroA.isEnabled = true
                numeroB.isEnabled = true
                calculandoPrimos = false
                beepCorto()
                stopBrilloParpadeo()
                window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                hablar("Cálculo De Primos Finalizado")
                if (guardarTXT) {
                    hablar("T X T; Generado En Descargas")
                    Toast.makeText(
                        this@PrimosActivity, "T X T Generado En Descargas", Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    fun setBrillo(value: Float) {
        val layout = window.attributes
        layout.screenBrightness = value
        window.attributes = layout
    }

    private fun startBrilloParpadeo() {
        var contadorSegundos = 0
        brilloHandler = Handler(Looper.getMainLooper())
        val runnable = object : Runnable {
            override fun run() {
                setBrillo(if (brilloEncendido) 0.3f else 0.5f)
                brilloEncendido = !brilloEncendido
                contadorSegundos++
                if (contadorSegundos % 60 == 0) {
                    val progText = porcentajeText.text.toString()
                    val progSolo = progText.split(" ")[0]
                    val primosPorSegundo =
                        priSegText.text.toString().filter { it.isDigit() || it == ',' }
                    hablar("Progreso Del Cálculo; $progSolo; Primos Por Segundo Promedio; $primosPorSegundo")
                }
                brilloHandler?.postDelayed(this, 1000)
            }
        }
        brilloHandler?.post(runnable)
    }

    private fun stopBrilloParpadeo() {
        brilloHandler?.removeCallbacksAndMessages(null)
        brilloHandler = null
        setBrillo(-1f)
    }

    private suspend fun calcular(min: Long, max: Long, conIntervalos: Boolean): String {
        val total = AtomicLong(0)
        val digitoFinal = LongArray(10)
        val capicua = AtomicLong(0)
        val sumaPrimo = AtomicLong(0)
        val sumaPrimodigpri = AtomicLong(0)
        val gemelos = AtomicLong(0)
        val reversibles = AtomicLong(0)
        val soloImpares = AtomicLong(0)
        val intervalos = if (conIntervalos) mutableMapOf<Long, Long>() else null
        val inicio = SystemClock.elapsedRealtime()
        val primosPequenos = primosHasta(sqrt(max.toDouble()).toLong().coerceAtMost(SEGMENT_SIZE))
        val ultimoPrimo = AtomicLong(0)
        if (2 in min..max) {
            total.incrementAndGet()
            digitoFinal[2]++
            intervalos?.let { mapa ->
                val clave = (2L / 10000L) * 10000L
                mapa[clave] = mapa.getOrDefault(clave, 0L) + 1L
            }
            if (esCapicua(2L)) capicua.incrementAndGet()
            if (sumaDigitosPrimo(2L)) sumaPrimo.incrementAndGet()
            if (sumaYDigitosPrimos(2L)) sumaPrimodigpri.incrementAndGet()
            if (soloCifrasImpares(2L)) soloImpares.incrementAndGet()
            if (esReversible(2L)) reversibles.incrementAndGet()
            synchronized(ultimosPrimos) { ultimosPrimos.addLast(2L) }
            ultimoPrimo.set(2L)
        }
        var segInicio = if (min <= 2) 3L else min
        var ultimaUI = inicio
        val sb = StringBuilder()
        while (segInicio <= max) {
            val segFin = minOf(segInicio + SEGMENT_SIZE - 1, max)
            val bits = java.util.BitSet((segFin - segInicio + 1).toInt())
            for (p in primosPequenos) {
                if (p * p > segFin) break
                var startMult = ((segInicio + p - 1) / p) * p
                if (startMult < p * p) startMult = p * p
                var marca = startMult
                while (marca <= segFin) {
                    bits.set((marca - segInicio).toInt())
                    marca += p
                }
            }
            var n = segInicio
            var localUltimo = ultimoPrimo.get()
            while (n <= segFin) {
                if (!bits.get((n - segInicio).toInt())) {
                    total.incrementAndGet()
                    digitoFinal[(n % 10).toInt()]++
                    if (esCapicua(n)) capicua.incrementAndGet()
                    if (sumaDigitosPrimo(n)) sumaPrimo.incrementAndGet()
                    if (sumaYDigitosPrimos(n)) sumaPrimodigpri.incrementAndGet()
                    if (n - localUltimo == 2L) gemelos.incrementAndGet()
                    if (soloCifrasImpares(n)) soloImpares.incrementAndGet()
                    if (esReversible(n)) reversibles.incrementAndGet()
                    intervalos?.let { mapa ->
                        val clave = (n / 10000) * 10000
                        synchronized(mapa) { mapa[clave] = mapa.getOrDefault(clave, 0) + 1 }
                    }
                    synchronized(ultimosPrimos) {
                        ultimosPrimos.addLast(n)
                        if (ultimosPrimos.size > BUFFER_SIZE) ultimosPrimos.removeFirst()
                    }
                    localUltimo = n
                }
                n += 1
            }
            if (localUltimo > ultimoPrimo.get()) ultimoPrimo.set(localUltimo)
            val ahora = SystemClock.elapsedRealtime()
            if (ahora - ultimaUI >= 1000) {
                val rangoTotal = (max - min + 1).toDouble()
                val prog = ((ultimoPrimo.get() - min + 1).toDouble() / rangoTotal * 100.0).coerceIn(
                    0.0, 100.0
                )
                val segs = (ahora - inicio) / 1000.0
                val vel = total.get() / segs.coerceAtLeast(0.001)
                val eta = calcularETA(inicio, prog.toInt())
                withContext(Dispatchers.Main) {
                    porcentajeText.text = "%.2f%%  •  ETA: %s".format(prog, eta)
                    progresoBar.progress = (prog * 100).toInt()
                    primoActualText.text = "Nº En Curso - : ${ultimoPrimo.get()}"
                    priSegText.text = "Primos / Seg - ${"%.2f".format(vel)}"
                }
                ultimaUI = ahora
            }
            segInicio = segFin + 1
        }
        val tiempo = (SystemClock.elapsedRealtime() - inicio) / 1000.0
        withContext(Dispatchers.Main) {
            sb.append(
                "--- Últimos Primos Disponibles ( Max. $BUFFER_SIZE ) ---\n\n${
                    ultimosPrimos.joinToString(
                        " "
                    )
                }\n\n"
            )
            intervalos?.let {
                sb.append("--- Intervalos De 10000 ---\n\n")
                it.toSortedMap().forEach { (k, v) ->
                    val pct = v * 100.0 / total.get()
                    sb.append(
                        "$k - ${k + 9999} → $v → ( ${
                            String.format(
                                "%.5f", pct
                            )
                        }% )\n"
                    )
                }
                sb.append("\n")
            }
            sb.append("--- Terminaciones De Números Primos ---\n\n")
            for (d in 0..9) if (digitoFinal[d] > 0) {
                val pct = digitoFinal[d] * 100.0 / total.get()
                sb.append(
                    "Acabados En $d ---> ${digitoFinal[d]} → ( ${
                        String.format(
                            "%.5f", pct
                        )
                    }% )\n"
                )
            }
            val totalIntervalo = (max - min + 1)
            val porcentajePrimos = (total.get() * 100.0 / totalIntervalo)
            sb.append(
                "\n--- Estadísticas Finales ---\n\n" + "Número Total De Primos En Intervalo ( ${numeroA.text} → ${numeroB.text} ) → ${total.get()} → " + "( ${
                    String.format(
                        "%.5f", porcentajePrimos
                    )
                }% )\n"
            )
            sb.append(
                "Tiempo De Cálculo → ${tiempo.toInt() / 3600}h ${(tiempo.toInt() % 3600) / 60}m ${
                    "%.2f".format(
                        tiempo % 60
                    )
                }s\n"
            )
            sb.append("Números Primos Por Segundo → ${"%.5f".format(total.get() / tiempo)} Primos / Seg\n")
            sb.append(
                "Números Primos Capicúa → ${capicua.get()} → ( ${
                    String.format(
                        "%.5f", capicua.get() * 100.0 / total.get()
                    )
                }% )\n"
            )
            sb.append(
                "Números Primos Cuya Suma De Dígitos Es Prima → ${sumaPrimo.get()} → ( ${
                    String.format(
                        "%.5f", sumaPrimo.get() * 100.0 / total.get()
                    )
                }% )\n"
            )
            sb.append(
                "Números Primos Cuya Suma De Dígitos Es Prima Y Todos Sus Dígitos Son Primos → ${sumaPrimodigpri.get()} → ( ${
                    String.format(
                        "%.5f", sumaPrimodigpri.get() * 100.0 / total.get()
                    )
                }% )\n"
            )
            sb.append(
                "Números Primos Gemelos → ${gemelos.get()} Parejas → ( ${
                    String.format(
                        "%.5f", gemelos.get() * 2 * 100.0 / total.get()
                    )
                }% )\n"
            )
            sb.append(
                "Números Que Leídos De D-I O De I-D Son Primos → ${reversibles.get()} → ( ${
                    String.format(
                        "%.5f", reversibles.get() * 100.0 / total.get()
                    )
                }% )\n"
            )
            sb.append(
                "Números Primos Que Todos Sus Dígitos Son Impares → ${soloImpares.get()} → ( ${
                    String.format(
                        "%.5f", soloImpares.get() * 100.0 / total.get()
                    )
                }% )\n\n"
            )
            val añoActual = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)
            sb.append("||| --- By M8AX Corp. $añoActual --- |||")
            resultadosText.text = sb.toString()
            porcentajeText.text = "100%"
            progresoBar.progress = 100
            if (resultadosText.parent is ScrollView) (resultadosText.parent as ScrollView).post {
                (resultadosText.parent as ScrollView).fullScroll(ScrollView.FOCUS_DOWN)
            }
        }
        return sb.toString()
    }

    private fun calcularETA(startTime: Long, progreso: Int): String {
        if (progreso <= 0.0) return "--"
        val ahora = SystemClock.elapsedRealtime()
        val transcurrido = ahora - startTime
        val restante = (transcurrido * (100.0 - progreso)) / progreso
        var segundos = (restante / 1000).toLong()
        val dias = segundos / 86400
        segundos %= 86400
        val horas = segundos / 3600
        segundos %= 3600
        val minutos = segundos / 60
        segundos %= 60
        return "${dias}D ${horas}H ${minutos}M ${segundos}S"
    }

    private fun calcularParaGuardar(min: Long, max: Long) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val sdfFileName = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
                val fechaParaArchivo = sdfFileName.format(Date())
                val a = numeroA.text.toString()
                val b = numeroB.text.toString()
                val fileName = "M8AX - PrimoS - ( ${a} - ${b} ) _ ${fechaParaArchivo}.TxT"
                var uriToOpen: Uri? = null
                val outputStream = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val contentValues = ContentValues().apply {
                        put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                        put(android.provider.MediaStore.MediaColumns.MIME_TYPE, "text/plain")
                        put(
                            android.provider.MediaStore.MediaColumns.RELATIVE_PATH,
                            Environment.DIRECTORY_DOWNLOADS
                        )
                    }
                    uriToOpen = contentResolver.insert(
                        android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues
                    )
                    BufferedOutputStream(contentResolver.openOutputStream(uriToOpen!!))
                } else {
                    val downloads =
                        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                    val file = File(downloads, fileName)
                    uriToOpen = Uri.fromFile(file)
                    BufferedOutputStream(FileOutputStream(file))
                }
                outputStream?.use { out ->
                    val total = AtomicLong(0)
                    val digitoFinal = LongArray(10)
                    val capicua = AtomicLong(0)
                    val sumaPrimo = AtomicLong(0)
                    val sumaPrimodigpri = AtomicLong(0)
                    val gemelos = AtomicLong(0)
                    val reversibles = AtomicLong(0)
                    val soloImpares = AtomicLong(0)
                    val intervalos = mutableMapOf<Long, Long>()
                    val primosPequenos =
                        primosHasta(sqrt(max.toDouble()).toLong().coerceAtMost(SEGMENT_SIZE))
                    val ultimoPrimo = AtomicLong(0)
                    val primosPorLinea = 15
                    val inicio = SystemClock.elapsedRealtime()
                    var lineaBuffer = ArrayList<Long>()
                    out.write("--- Listado De Números Primos ---\n\n".toByteArray())
                    var segInicio = if (min <= 2) 2L else min
                    while (segInicio <= max) {
                        val segFin = minOf(segInicio + SEGMENT_SIZE - 1, max)
                        val bits = java.util.BitSet((segFin - segInicio + 1).toInt())
                        for (p in primosPequenos) {
                            val p2 = p * p
                            if (p2 > segFin) break
                            var startMult = ((segInicio + p - 1) / p) * p
                            if (startMult < p2) startMult = p2
                            var marca = startMult
                            while (marca <= segFin) {
                                bits.set((marca - segInicio).toInt())
                                marca += p
                            }
                        }
                        var n = segInicio
                        var localUltimo = ultimoPrimo.get()
                        if (segInicio <= 2 && 2 in segInicio..segFin) {
                            total.incrementAndGet()
                            digitoFinal[2]++
                            if (esCapicua(2L)) capicua.incrementAndGet()
                            if (sumaDigitosPrimo(2L)) sumaPrimo.incrementAndGet()
                            if (sumaYDigitosPrimos(2L)) sumaPrimodigpri.incrementAndGet()
                            if (localUltimo != 0L && 2 - localUltimo == 2L) gemelos.incrementAndGet()
                            if (soloCifrasImpares(2L)) soloImpares.incrementAndGet()
                            if (esReversible(2L)) reversibles.incrementAndGet()
                            intervalos[(2 / 10000) * 10000] =
                                intervalos.getOrDefault((2 / 10000) * 10000, 0) + 1
                            lineaBuffer.add(2L)
                            localUltimo = 2L
                        }
                        if (segInicio % 2L == 0L) n = segInicio + 1
                        while (n <= segFin) {
                            if (!bits.get((n - segInicio).toInt())) {
                                total.incrementAndGet()
                                digitoFinal[(n % 10).toInt()]++
                                if (esCapicua(n)) capicua.incrementAndGet()
                                if (sumaDigitosPrimo(n)) sumaPrimo.incrementAndGet()
                                if (sumaYDigitosPrimos(n)) sumaPrimodigpri.incrementAndGet()
                                if (localUltimo != 0L && n - localUltimo == 2L) gemelos.incrementAndGet()
                                if (soloCifrasImpares(n)) soloImpares.incrementAndGet()
                                if (esReversible(n)) reversibles.incrementAndGet()
                                val clave = (n / 10000) * 10000
                                intervalos[clave] = intervalos.getOrDefault(clave, 0) + 1
                                lineaBuffer.add(n)
                                if (lineaBuffer.size >= primosPorLinea) {
                                    out.write((lineaBuffer.joinToString(" ") + "\n").toByteArray())
                                    out.flush()
                                    lineaBuffer.clear()
                                    yield()
                                }
                                localUltimo = n
                            }
                            n += 2
                        }
                        if (localUltimo > ultimoPrimo.get()) ultimoPrimo.set(localUltimo)
                        segInicio = segFin + 1
                    }
                    if (lineaBuffer.isNotEmpty()) {
                        out.write((lineaBuffer.joinToString(" ") + "\n").toByteArray())
                        out.flush()
                    }
                    out.write("\n--- Intervalos De 10000 ---\n\n".toByteArray())
                    intervalos.toSortedMap().forEach { (k, v) ->
                        val pct = v * 100.0 / total.get()
                        out.write(
                            "$k - ${k + 9999} → $v → ( ${
                                String.format(
                                    "%.5f", pct
                                )
                            }% )\n".toByteArray()
                        )
                    }
                    out.write("\n--- Terminaciones De Números Primos ---\n\n".toByteArray())
                    for (i in 0..9) {
                        val cantidad = digitoFinal[i]
                        if (cantidad > 0) {
                            val pct = if (total.get() > 0) cantidad * 100.0 / total.get() else 0.0
                            out.write(
                                "Acabados En $i ---> $cantidad → ( ${
                                    String.format(
                                        "%.5f", pct
                                    )
                                }% )\n".toByteArray()
                            )
                        }
                    }
                    val tiempoSegundos = (SystemClock.elapsedRealtime() - inicio) / 1000.0
                    val horas = tiempoSegundos.toInt() / 3600
                    val minutos = (tiempoSegundos.toInt() % 3600) / 60
                    val segundos = tiempoSegundos.toInt() % 60
                    val primosPorSegundo = total.get() / tiempoSegundos
                    out.write("\n--- Estadísticas Finales ---\n\n".toByteArray())
                    val totalIntervalo = (max - min + 1)
                    val porcentajePrimos = (total.get() * 100.0 / totalIntervalo)
                    out.write(
                        ("Número Total De Primos En Intervalo ( ${numeroA.text} → ${numeroB.text} ) → ${total.get()} → ( ${
                            String.format(
                                "%.5f", porcentajePrimos
                            )
                        }% )\n").toByteArray()
                    )
                    out.write("Tiempo De Cálculo → ${horas}h ${minutos}m ${segundos}s\n".toByteArray())
                    out.write("Números Primos Por Segundo → ${"%.5f".format(primosPorSegundo)} Primos / Seg\n".toByteArray())
                    out.write(
                        ("Números Primos Capicúa → ${capicua.get()} → ( ${
                            String.format(
                                "%.5f", capicua.get() * 100.0 / total.get()
                            )
                        }% )\n").toByteArray()
                    )
                    out.write(
                        ("Números Primos Cuya Suma De Dígitos Es Prima → ${sumaPrimo.get()} → ( ${
                            String.format(
                                "%.5f", sumaPrimo.get() * 100.0 / total.get()
                            )
                        }% )\n").toByteArray()
                    )
                    out.write(
                        ("Números Primos Cuya Suma De Dígitos Es Prima Y Todos Sus Dígitos Son Primos → ${sumaPrimodigpri.get()} → ( ${
                            String.format(
                                "%.5f", sumaPrimodigpri.get() * 100.0 / total.get()
                            )
                        }% )\n").toByteArray()
                    )
                    out.write(
                        ("Números Primos Gemelos → ${gemelos.get()} Parejas → ( ${
                            String.format(
                                "%.5f", gemelos.get() * 2 * 100.0 / total.get()
                            )
                        }% )\n").toByteArray()
                    )
                    out.write(
                        ("Números Que Leídos De D-I O De I-D Son Primos → ${reversibles.get()} → ( ${
                            String.format(
                                "%.5f", reversibles.get() * 100.0 / total.get()
                            )
                        }% )\n").toByteArray()
                    )
                    out.write(
                        ("Números Primos Que Todos Sus Dígitos Son Impares → ${soloImpares.get()} → ( ${
                            String.format(
                                "%.5f", soloImpares.get() * 100.0 / total.get()
                            )
                        }% )\n\n||| --- By M8AX Corp. ${
                            Calendar.getInstance().get(Calendar.YEAR)
                        } --- |||").toByteArray()
                    )
                    out.flush()
                    (outputStream as? FileOutputStream)?.fd?.sync()
                    out.close()
                }
                withContext(Dispatchers.Main) {
                    uriToOpen?.let {
                        MediaScannerConnection.scanFile(
                            this@PrimosActivity, arrayOf(it.path), arrayOf("text/plain"), null
                        )
                    }
                    Toast.makeText(
                        this@PrimosActivity, "TXT Generado En Descargas", Toast.LENGTH_LONG
                    ).show()
                    uriToOpen?.let {
                        val intent = Intent(Intent.ACTION_VIEW)
                        intent.setDataAndType(it, "text/plain")
                        intent.flags =
                            Intent.FLAG_ACTIVITY_NO_HISTORY or Intent.FLAG_GRANT_READ_URI_PERMISSION
                        startActivity(intent)
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    e.printStackTrace()
                    Toast.makeText(this@PrimosActivity, "Error Al Generar TXT", Toast.LENGTH_SHORT)
                        .show()
                }
            }
        }
    }

    private fun primosHasta(limite: Long): List<Long> {
        if (limite < 2) return emptyList()
        val tam = limite.toInt() + 1
        val criba = BooleanArray(tam) { true }
        criba[0] = false
        criba[1] = false
        val raiz = sqrt(limite.toDouble()).toInt()
        for (i in 2..raiz) if (criba[i]) for (j in i * i until tam step i) criba[j] = false
        val lista = ArrayList<Long>()
        for (i in 2 until tam) if (criba[i]) lista.add(i.toLong())
        return lista
    }

    private fun esCapicua(n: Long) = n.toString() == n.toString().reversed()
    private fun soloCifrasImpares(n: Long): Boolean {
        var x = n
        while (x > 0) {
            if ((x % 10) % 2L == 0L) return false
            x /= 10
        }
        return true
    }

    private fun sumaDigitosPrimo(n: Long): Boolean {
        var suma = 0
        var x = n
        while (x > 0) {
            suma += (x % 10).toInt()
            x /= 10
        }
        return esPrimo(suma.toLong())
    }

    private fun sumaYDigitosPrimos(n: Long): Boolean {
        var x = n
        var suma = 0
        while (x > 0) {
            val d = (x % 10).toInt()
            if (d != 2 && d != 3 && d != 5 && d != 7) return false
            suma += d
            x /= 10
        }
        return esPrimo(suma.toLong())
    }

    private fun esReversible(n: Long): Boolean {
        if (n % 10 == 0L) return false
        val rev = n.toString().reversed().toLong()
        if (n == 2L || n == 3L || n == 5L || n == 7L) return true
        return esPrimo(n) && esPrimo(rev)
    }

    private fun esPrimo(n: Long): Boolean {
        if (n < 2) return false
        if (n == 2L || n == 3L) return true
        if (n % 2L == 0L || n % 3L == 0L) return false
        var i = 5L
        while (i * i <= n) {
            if (n % i == 0L || n % (i + 2) == 0L) return false
            i += 6
        }
        return true
    }
}