package com.mviiiax.m8ax_diariogimnasio

import android.content.ContentValues
import android.content.pm.PackageManager
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.hardware.camera2.CameraManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.provider.MediaStore
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

class AudioEventosActivity : AppCompatActivity() {
    private lateinit var contadorCentral: TextView
    private lateinit var statsIzq: List<TextView>
    private lateinit var statsDer: List<TextView>
    private lateinit var oscilloscopeView: OscilloscopeView
    private lateinit var barsView: BarsView
    private lateinit var waveformView: WaveformView
    private lateinit var vuActual: VUMeter
    private lateinit var vuRMS: VUMeter
    private lateinit var vuPico: VUMeter
    private lateinit var scrollEventos: LinearLayout
    private lateinit var audioRecord: AudioRecord
    private val sampleRate = 44100
    private var isDetectando = false
    private var contadorEventos = 0
    private var dbPicoActual = 0.0
    private var lastEventTime = System.currentTimeMillis()
    private var startTime = System.currentTimeMillis()
    private var dentroEvento = false
    private val eventosUltimoMinuto = mutableListOf<Long>()
    private var dbMax = -90.0
    private var dbMin = Double.MAX_VALUE
    private var rmsPromedio = 0.0
    private var picoUltimo = 0.0
    private val umbralAlto = 2000
    private val umbralBajo = 1000
    private var picoEventoActual = 0
    private val calibrationOffset = 80.0
    private var ultimoEventoTime = 0L
    private var cbMotor: CheckBox? = null
    private var cbFlash: CheckBox? = null
    private var rmsFast = 0.0
    private var rmsSlow = 0.0
    private val ATTACK_FAST = 0.9
    private val DECAY_FAST = 0.4
    private val ATTACK_SLOW = 0.2
    private val DECAY_SLOW = 0.05
    private var ultimoDisparo = 0L
    private val COOLDOWN_MS = 50L
    private val SENSIBILIDAD = 1.0
    private val RMS_MINIMO = 10.0
    private val PERMISSION_REQUEST_CODE = 1001
    private var mediaRecorder: MediaRecorder? = null
    private var grabandoAudio = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        window.decorView.systemUiVisibility =
            (View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN)
        comprobarYPedirPermisos()
    }

    private fun crearCheckBox(texto: String): CheckBox {
        return CheckBox(this).apply {
            text = texto
            isChecked = false
            setTextColor(Color.WHITE)
            buttonTintList = android.content.res.ColorStateList.valueOf(Color.WHITE)
            textSize = 14f
            setPadding(0, 10, 0, 0)
        }
    }

    private fun crearArchivoGrabacion(extPorDefecto: String = "3gp"): Pair<Uri?, File?> {
        val nombreArchivo = "M8AX-${
            SimpleDateFormat(
                "dd-MM-yyyy_HHmmss", Locale.getDefault()
            ).format(Date())
        }.$extPorDefecto"
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, nombreArchivo)
                put(
                    MediaStore.MediaColumns.MIME_TYPE,
                    if (extPorDefecto == "opus") "audio/ogg" else "audio/3gpp"
                )
                put(
                    MediaStore.MediaColumns.RELATIVE_PATH,
                    Environment.DIRECTORY_MUSIC + "/M8AX - Diario De Gimnasio"
                )
            }
            val uri =
                contentResolver.insert(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, contentValues)
            Pair(uri, null)
        } else {
            val dir =
                File(Environment.getExternalStorageDirectory(), "Music/M8AX - Diario De Gimnasio")
            if (!dir.exists()) dir.mkdirs()
            Pair(null, File(dir, nombreArchivo))
        }
    }

    private fun iniciarGrabacion() {
        if (grabandoAudio) return
        var extPorDefecto = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) "opus" else "3gp"
        var (uri, file) = crearArchivoGrabacion(extPorDefecto)
        try {
            mediaRecorder = MediaRecorder().apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    try {
                        setOutputFormat(MediaRecorder.OutputFormat.OGG)
                        setAudioEncoder(MediaRecorder.AudioEncoder.OPUS)
                        setAudioSamplingRate(16000)
                        setAudioEncodingBitRate(12000)
                        val fd = contentResolver.openFileDescriptor(uri!!, "rw")?.fileDescriptor
                            ?: throw IOException("No Se Pudo Abrir El Archivo: ${file?.name ?: "Desconocido"} Con Extensión $extPorDefecto")
                        setOutputFile(fd)
                    } catch (_: Exception) {
                        extPorDefecto = "3gp"
                        val fallback = crearArchivoGrabacion(extPorDefecto)
                        uri = fallback.first
                        file = fallback.second
                        setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
                        setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            val fd = contentResolver.openFileDescriptor(uri!!, "rw")?.fileDescriptor
                                ?: throw IOException("No Se Pudo Abrir El Archivo: ${file?.name ?: "Desconocido"} Con Extensión $extPorDefecto")
                            setOutputFile(fd)
                        } else {
                            setOutputFile(file!!.absolutePath)
                        }
                    }
                } else {
                    setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
                    setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
                    setOutputFile(file!!.absolutePath)
                }
                prepare()
                start()
            }
            grabandoAudio = true
        } catch (_: Exception) {
            try {
                mediaRecorder?.release()
            } catch (_: Exception) {
            }
            mediaRecorder = null
            grabandoAudio = false
            Toast.makeText(this, "--- No Se Pudo Iniciar La Grabación ---", Toast.LENGTH_LONG)
                .show()
        }
    }

    private fun detenerGrabacion() {
        try {
            mediaRecorder?.stop()
            mediaRecorder?.release()
        } catch (_: Exception) {
        }
        mediaRecorder = null
        grabandoAudio = false
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            window.decorView.systemUiVisibility =
                (View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN)
        }
    }

    private fun inicializarUI() {
        val mainLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.BLACK)
        }
        val statsLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            weightSum = 2f
        }
        statsIzq = listOf(
            "dB Actual", "dB Máx", "dB Mín", "dB Medio", "RMS", "Nivel Actual"
        ).map { crearStat(it) }
        statsDer = listOf(
            "Tmp. No Evts",
            "Eventos / Min",
            "Nº Evts Fuertes",
            "Fecha: --/--/----",
            "Hora: --:--:--",
            "Tmp. Online"
        ).map { crearStat(it) }
        val statsIzqLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            statsIzq.forEach { addView(it) }
            cbMotor = crearCheckBox("MOTOR RÍTMICO ON")
            addView(cbMotor)
            cbMotor?.setOnCheckedChangeListener { _, _ -> }
            setPadding(20, 20, 20, 20)
        }
        val statsDerLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            statsDer.forEach { addView(it) }
            cbFlash = crearCheckBox("FLASH RÍTMICO ON")
            addView(cbFlash)
            cbFlash?.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked && checkSelfPermission(android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                    cbFlash?.isChecked = false
                    Toast.makeText(
                        applicationContext,
                        "Permiso De Cámara Necesario Para El Flash Rítmico",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
            setPadding(20, 20, 20, 20)
        }
        statsLayout.addView(
            statsIzqLayout, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        )
        statsLayout.addView(
            statsDerLayout, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        )
        mainLayout.addView(statsLayout)
        contadorCentral = TextView(this).apply {
            text = "0"
            setTextColor(Color.RED)
            textSize = 56f
            gravity = Gravity.CENTER
            setOnClickListener {
                if (isDetectando) {
                    isDetectando = false
                    setTextColor(Color.GRAY)
                    Toast.makeText(context, "Medición Pausada", Toast.LENGTH_SHORT).show()
                } else {
                    isDetectando = true
                    if (grabandoAudio) {
                        setTextColor(Color.parseColor("#FF8A00"))
                    } else {
                        setTextColor(Color.RED)
                    }
                    Toast.makeText(context, "Medición Reanudada", Toast.LENGTH_SHORT).show()
                    startDetection()
                }
            }
            setOnLongClickListener {
                if (!grabandoAudio) {
                    iniciarGrabacion()
                    contadorCentral.setTextColor(Color.parseColor("#FF8A00"))
                    Toast.makeText(context, "Grabación Iniciada", Toast.LENGTH_SHORT).show()
                } else {
                    detenerGrabacion()
                    contadorCentral.setTextColor(Color.RED)
                    Toast.makeText(
                        context, "Grabación Guardada En Carpeta Music", Toast.LENGTH_SHORT
                    ).show()
                }
                true
            }
        }
        mainLayout.addView(
            contadorCentral, LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = 20
                bottomMargin = 20
            })
        val instruccionesOscillo = TextView(this).apply {
            text =
                "Toca El Texto Rojo / Naranja / Gris → Pausa / Reanuda Mediciones\nClick Largo → Inicia / Para Grabación En Carpeta Music - ( 1h-10 MB )"
            setTextColor(Color.LTGRAY)
            textSize = 12f
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 3)
        }
        mainLayout.addView(instruccionesOscillo)
        mainLayout.addView(crearGraficaTitulo("Osciloscopio - | Real - Time |"))
        oscilloscopeView = OscilloscopeView(this)
        mainLayout.addView(
            oscilloscopeView, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 300)
        )
        mainLayout.addView(crearGraficaTitulo("Bars EQ - | Real - Time |"))
        barsView = BarsView(this)
        mainLayout.addView(
            barsView, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 220)
        )
        mainLayout.addView(crearGraficaTitulo("Waveform - | Real - Time |"))
        waveformView = WaveformView(this)
        mainLayout.addView(
            waveformView, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 220)
        )
        val scrollView = ScrollView(this).apply {
            isVerticalScrollBarEnabled = true
            setScrollbarFadingEnabled(false)
            scrollBarStyle = View.SCROLLBARS_INSIDE_OVERLAY
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 300)
        }
        scrollEventos = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }
        scrollView.addView(scrollEventos)
        mainLayout.addView(scrollView)
        val vuLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(0, -22, 0, 0)
        }
        vuActual = VUMeter(this, Color.GREEN, "Actual")
        vuRMS = VUMeter(this, Color.CYAN, "RMS")
        vuPico = VUMeter(this, Color.MAGENTA, "Pico")
        vuLayout.addView(vuActual, LinearLayout.LayoutParams(0, 300, 1f))
        vuLayout.addView(vuRMS, LinearLayout.LayoutParams(0, 300, 1f))
        vuLayout.addView(vuPico, LinearLayout.LayoutParams(0, 300, 1f))
        mainLayout.addView(vuLayout)
        setContentView(mainLayout)
        startDetection()
    }

    private fun crearGraficaTitulo(titulo: String): TextView {
        return TextView(this).apply {
            text = titulo
            setTextColor(Color.WHITE)
            textSize = 16f
            setPadding(0, 10, 0, 10)
            gravity = Gravity.CENTER
        }
    }

    private fun crearStat(t: String) = TextView(this).apply {
        text = "$t: 0"
        setTextColor(Color.CYAN)
        textSize = 14f
    }

    override fun onResume() {
        super.onResume()
        if (::contadorCentral.isInitialized) {
            if (checkSelfPermission(android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                cbFlash?.isChecked = false
                cbFlash?.isEnabled = false
            }
        }
    }

    private fun comprobarYPedirPermisos() {
        val permisosQueFaltan = mutableListOf<String>()
        if (checkSelfPermission(android.Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            permisosQueFaltan.add(android.Manifest.permission.RECORD_AUDIO)
        }
        if (checkSelfPermission(android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            permisosQueFaltan.add(android.Manifest.permission.CAMERA)
        }
        if (permisosQueFaltan.isEmpty()) {
            inicializarUI()
        } else {
            requestPermissions(permisosQueFaltan.toTypedArray(), PERMISSION_REQUEST_CODE)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE) {
            var tieneAudio =
                checkSelfPermission(android.Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
            var tieneCamara =
                checkSelfPermission(android.Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
            permissions.forEachIndexed { index, permiso ->
                if (grantResults.getOrNull(index) == PackageManager.PERMISSION_GRANTED) {
                    when (permiso) {
                        android.Manifest.permission.RECORD_AUDIO -> tieneAudio = true
                        android.Manifest.permission.CAMERA -> tieneCamara = true
                    }
                }
            }
            if (!tieneAudio) {
                Toast.makeText(
                    this,
                    "Se Necesita Permiso De Micrófono Para Análizar El Sonido Ambiental",
                    Toast.LENGTH_LONG
                ).show()
                finish()
                return
            }
            inicializarUI()
            if (!tieneCamara) {
                cbFlash?.isChecked = false
                cbFlash?.isEnabled = false
            }
        }
    }

    private fun startDetection() {
        val bufferSize = AudioRecord.getMinBufferSize(
            sampleRate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT
        )
        audioRecord = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            bufferSize
        )
        audioRecord.startRecording()
        isDetectando = true
        Thread {
            val buffer = ShortArray(bufferSize)
            while (true) {
                if (!isDetectando) {
                    Thread.sleep(50)
                    continue
                }
                val read = audioRecord.read(buffer, 0, buffer.size)
                if (read <= 0) continue
                val boostFactor = 1.5f
                var sumSq = 0.0
                var pico = 0
                for (i in 0 until read) {
                    val v = (buffer[i] * boostFactor).toInt()
                        .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
                    sumSq += v * v
                    pico = max(pico, abs(v))
                }
                val rms = sqrt(sumSq / read)
                rmsPromedio = rms
                picoUltimo = pico.toDouble()
                val dbFS = 20 * log10(rms / 32768.0 + 1e-9)
                val dbSPL = dbFS + calibrationOffset
                val dbSPLClamped = dbSPL.coerceIn(0.0, 120.0)
                rmsFast = if (rms > rmsFast) rmsFast + (rms - rmsFast) * ATTACK_FAST
                else rmsFast + (rms - rmsFast) * DECAY_FAST
                rmsSlow = if (rms > rmsSlow) rmsSlow + (rms - rmsSlow) * ATTACK_SLOW
                else rmsSlow + (rms - rmsSlow) * DECAY_SLOW
                val ahora = System.currentTimeMillis()
                val hayGolpe =
                    rmsFast > rmsSlow * SENSIBILIDAD && rmsFast > RMS_MINIMO && ahora - ultimoDisparo > COOLDOWN_MS
                if (hayGolpe) {
                    ultimoDisparo = ahora
                    if (cbMotor?.isChecked == true) {
                        val fuerza = ((rmsFast / rmsSlow) * 220).toInt().coerceIn(80, 255)
                        val vibe = getSystemService(VIBRATOR_SERVICE) as Vibrator
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            vibe.vibrate(VibrationEffect.createOneShot(35, fuerza))
                        } else {
                            vibe.vibrate(35)
                        }
                    }
                    if (cbFlash?.isChecked == true) {
                        val cam = getSystemService(CAMERA_SERVICE) as CameraManager
                        val camId = cam.cameraIdList[0]
                        try {
                            cam.setTorchMode(camId, true)
                            Handler(Looper.getMainLooper()).postDelayed({
                                cam.setTorchMode(camId, false)
                            }, 35)
                        } catch (_: Exception) {
                        }
                    }
                }
                dbMax = max(dbMax, dbSPL)
                dbMin = min(dbMin, dbSPL)
                val now = System.currentTimeMillis()
                if (pico > umbralAlto) {
                    val tiempoDesdeUltimo = now - ultimoEventoTime
                    if (!dentroEvento && tiempoDesdeUltimo > 500) {
                        contadorEventos++
                        ultimoEventoTime = now
                        lastEventTime = now
                        eventosUltimoMinuto.add(now)
                        dentroEvento = true
                        picoEventoActual = pico
                        runOnUiThread {
                            agregarEventoEnUI(dbSPL, now)
                        }
                    } else if (dentroEvento && pico > picoEventoActual * 1.25 && tiempoDesdeUltimo > 500) {
                        contadorEventos++
                        ultimoEventoTime = now
                        lastEventTime = now
                        eventosUltimoMinuto.add(now)
                        picoEventoActual = pico
                        runOnUiThread {
                            agregarEventoEnUI(dbSPL, now)
                        }
                    }
                } else if (pico < umbralBajo) {
                    dentroEvento = false
                    picoEventoActual = 0
                }
                eventosUltimoMinuto.removeAll { now - it > 60000 }
                oscilloscopeView.updateWave(buffer, read)
                barsView.updateBars(buffer, read)
                waveformView.updateWaveform(buffer, read)
                val dbActual = dbSPLClamped
                val dbRMS = (20 * log10(rmsPromedio / 32768.0 + 1e-9) + calibrationOffset).coerceIn(
                    0.0, 120.0
                )
                val decay = 0.95
                dbPicoActual = max(dbPicoActual * decay, dbSPLClamped)
                val dbPico = dbPicoActual.coerceIn(0.0, 120.0)
                vuActual.setValue(dbActual)
                vuRMS.setValue(dbRMS)
                vuPico.setValue(dbPico)
                runOnUiThread {
                    contadorCentral.text = contadorEventos.toString()
                    statsIzq[0].text = "dB Actual → %.2f".format(dbSPL)
                    statsIzq[1].text = "dB Máx → %.2f".format(dbMax)
                    statsIzq[2].text = "dB Mín → %.2f".format(dbMin)
                    statsIzq[3].text = "dB Medio → %.2f".format((dbMax + dbMin) / 2)
                    statsIzq[4].text = "RMS → %.2f".format(rms)
                    statsIzq[5].text = "Nivel Actual → %.0f".format(picoUltimo)
                    val tiempoEventoMs = now - lastEventTime
                    val horas = tiempoEventoMs / 3600000
                    val minutos = (tiempoEventoMs % 3600000) / 60000
                    val segundos = (tiempoEventoMs % 60000) / 1000
                    statsDer[0].text =
                        "Tmp. No Evts → %02d:%02d:%02d".format(horas, minutos, segundos)
                    statsDer[1].text = "Eventos / Min → ${eventosUltimoMinuto.size}"
                    statsDer[2].text = "Nº Evts Fuertes → $contadorEventos"
                    val cal = Calendar.getInstance()
                    val sdfFecha = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                    val sdfHora = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
                    statsDer[3].text = "Fecha → ${sdfFecha.format(cal.time)}"
                    statsDer[4].text = "Hora → ${sdfHora.format(cal.time)}"
                    val tiempoEncendidoMs = now - startTime
                    val horasOn = tiempoEncendidoMs / 3600000
                    val minutosOn = (tiempoEncendidoMs % 3600000) / 60000
                    val segundosOn = (tiempoEncendidoMs % 60000) / 1000
                    statsDer[5].text =
                        "Tmp. Online → %02d:%02d:%02d".format(horasOn, minutosOn, segundosOn)
                }
                Thread.sleep(50)
            }
        }.start()
    }

    private fun agregarEventoEnUI(dbSPL: Double, now: Long) {
        val cal = Calendar.getInstance()
        cal.timeInMillis = now
        val diaSemana = cal.getDisplayName(Calendar.DAY_OF_WEEK, Calendar.LONG, Locale.getDefault())
            ?.replaceFirstChar { it.uppercase() } ?: ""
        val mes = cal.getDisplayName(Calendar.MONTH, Calendar.LONG, Locale.getDefault())
            ?.replaceFirstChar { it.uppercase() } ?: ""
        val diaMes = cal.get(Calendar.DAY_OF_MONTH)
        val año = cal.get(Calendar.YEAR)
        val horaStr = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
        val evento = TextView(this@AudioEventosActivity).apply {
            text = "$diaSemana, $diaMes De $mes De $año - $horaStr - dB → %.2f".format(dbSPL)
            setTextColor(
                when {
                    dbSPL >= 70.0 -> Color.RED
                    dbSPL >= 50.0 -> Color.YELLOW
                    else -> Color.CYAN
                }
            )
            textSize = 14f
        }
        scrollEventos.addView(evento)
        (scrollEventos.parent as ScrollView).post {
            (scrollEventos.parent as ScrollView).fullScroll(
                View.FOCUS_DOWN
            )
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        isDetectando = false
        if (grabandoAudio) detenerGrabacion()
        if (::audioRecord.isInitialized) {
            audioRecord.stop()
            audioRecord.release()
        }
    }

    class OscilloscopeView(context: android.content.Context) : View(context) {
        private val paint =
            Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.GREEN; strokeWidth = 3f }
        private val data = mutableListOf<Float>()
        fun updateWave(buf: ShortArray, read: Int) {
            if (width == 0) return
            synchronized(data) {
                for (i in 0 until read step max(1, read / width)) data.add(buf[i].toFloat())
                if (data.size > width) data.subList(0, data.size - width).clear()
            }
            postInvalidate()
        }

        override fun onDraw(c: Canvas) {
            c.drawColor(Color.BLACK)
            var px = 0f;
            var py = height / 2f
            synchronized(data) {
                for (i in data.indices) {
                    val x = i.toFloat()
                    val y = height / 2f - data[i] * height / 32768f
                    c.drawLine(px, py, x, y, paint)
                    px = x; py = y
                }
            }
        }
    }

    class BarsView(context: android.content.Context) : View(context) {
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        private var bars = FloatArray(0)
        fun updateBars(buf: ShortArray, read: Int) {
            if (width == 0) return
            val n = min(64, width / 6)
            if (bars.size != n) bars = FloatArray(n)
            val noiseFloor = 800f
            var maxVal = 0f
            for (i in 0 until n) {
                val idx = i * read / n
                val v = abs(buf[idx].toFloat())
                bars[i] = if (v < noiseFloor) 0f else v
                maxVal = max(maxVal, bars[i])
            }
            if (maxVal < noiseFloor * 1.2f) bars.fill(0f) else for (i in bars.indices) bars[i] /= maxVal
            postInvalidate()
        }

        override fun onDraw(c: Canvas) {
            c.drawColor(Color.BLACK)
            if (bars.isEmpty() || width == 0) return
            val barW = width.toFloat() / bars.size
            for (i in bars.indices) {
                val h = bars[i] * height
                paint.color = Color.rgb((h / height * 255).toInt().coerceIn(0, 255), 200, 50)
                c.drawRect(i * barW, height - h, (i + 1) * barW - 1, height.toFloat(), paint)
            }
        }
    }

    class WaveformView(context: android.content.Context) : View(context) {
        private val paint =
            Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.CYAN; strokeWidth = 2f }
        private var maxBuf = FloatArray(0)
        private var minBuf = FloatArray(0)
        private var writePos = 0
        fun updateWaveform(buf: ShortArray, read: Int) {
            val w = width
            if (w <= 0 || read <= 0) return
            if (maxBuf.size != w) {
                maxBuf = FloatArray(w); minBuf = FloatArray(w); writePos = 0
            }
            var max = Float.NEGATIVE_INFINITY
            var min = Float.POSITIVE_INFINITY
            for (i in 0 until read) {
                val v = buf[i].toFloat()
                if (v > max) max = v
                if (v < min) min = v
            }
            maxBuf[writePos] = max
            minBuf[writePos] = min
            writePos++; if (writePos >= maxBuf.size) writePos = 0
            postInvalidate()
        }

        override fun onDraw(c: Canvas) {
            c.drawColor(Color.BLACK)
            val w = width
            if (w == 0 || maxBuf.isEmpty()) return
            val mid = height / 2f
            for (x in 0 until min(w, maxBuf.size)) {
                val idx = writePos + x
                val safeIdx = if (idx >= maxBuf.size) idx - maxBuf.size else idx
                val yMax = mid - (maxBuf[safeIdx] / 32768f) * mid
                val yMin = mid - (minBuf[safeIdx] / 32768f) * mid
                c.drawLine(x.toFloat(), yMax, x.toFloat(), yMin, paint)
            }
        }
    }

    class VUMeter(
        context: android.content.Context, private val color: Int, private val label: String
    ) : View(context) {
        private val arcPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE; strokeWidth = 10f; this.color = color
        }
        private val tickPaint =
            Paint(Paint.ANTI_ALIAS_FLAG).apply { strokeWidth = 4f; color = Color.GRAY }
        private val needlePaint =
            Paint(Paint.ANTI_ALIAS_FLAG).apply { strokeWidth = 6f; color = Color.WHITE }
        private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE; textSize = 28f; textAlign = Paint.Align.CENTER
        }
        private var value = 0f
        fun setValue(v: Double) {
            value = (v / 120.0f).toFloat().coerceIn(0f, 1f); postInvalidate()
        }

        override fun onDraw(c: Canvas) {
            val cx = width / 2f
            val scale = context.resources.displayMetrics.density
            val offsetPx = 0.2f * 160 * scale / 2.54f
            val cy = height * 0.85f - offsetPx
            val r = min(width, height) * 0.4f
            c.drawArc(cx - r, cy - r, cx + r, cy + r, 180f, 180f, false, arcPaint)
            for (i in 0..12) {
                val a = Math.toRadians((180 + i * 15).toDouble())
                c.drawLine(
                    cx + r * 0.9f * cos(a).toFloat(),
                    cy + r * 0.9f * sin(a).toFloat(),
                    cx + r * cos(a).toFloat(),
                    cy + r * sin(a).toFloat(),
                    tickPaint
                )
            }
            val ang = Math.toRadians((180 + value * 180).toDouble())
            c.drawLine(
                cx,
                cy,
                cx + r * 0.88f * cos(ang).toFloat(),
                cy + r * 0.88f * sin(ang).toFloat(),
                needlePaint
            )
            c.drawText(label, cx, cy + r * 0.55f, textPaint)
        }
    }
}
