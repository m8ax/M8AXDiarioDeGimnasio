package com.mviiiax.m8ax_diariogimnasio

import android.content.Intent
import android.content.pm.ActivityInfo
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.media.MediaPlayer
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.util.TypedValue
import android.view.Gravity
import android.view.ScaleGestureDetector
import android.view.View
import android.view.ViewTreeObserver
import android.widget.Button
import android.widget.EditText
import android.widget.GridLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.shredzone.commons.suncalc.MoonIllumination
import java.time.ZoneId
import java.util.Calendar
import java.util.Locale
import kotlin.random.Random

class CalendarioActivity : AppCompatActivity() {
    private lateinit var tvMesAnio: TextView
    private lateinit var gridDias: GridLayout
    private lateinit var dao: GimnasioDao
    private lateinit var btnSiguienteMes: Button
    private lateinit var scaleGestureDetector: ScaleGestureDetector
    private var currentYear = 0
    private var currentMonth = 0
    private var scaleFactor = 1.0f
    private var mediaPlayer: MediaPlayer? = null
    private var jobMostrarMes: Job? = null
    private var nombreMesActual = ""
    var tts: TextToSpeech? = null
    var ttsEnabled = true
    val meses = arrayOf(
        "Enero",
        "Febrero",
        "Marzo",
        "Abril",
        "Mayo",
        "Junio",
        "Julio",
        "Agosto",
        "Septiembre",
        "Octubre",
        "Noviembre",
        "Diciembre"
    )
    val meses2 = arrayOf(
        "Enerito",
        "Febrerito",
        "Marzito",
        "Abrilito",
        "Mayito",
        "Junito",
        "Julito",
        "Agostito",
        "Septiembrito",
        "Octubrito",
        "Noviembrito",
        "Diciembrito"
    )
    private var mesesMostrar = meses

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val config = getSharedPreferences("M8AX-Config_TTS", MODE_PRIVATE)
        ttsEnabled = config.getBoolean("tts_enabled", false)
        if (ttsEnabled) {
            tts = TextToSpeech(this) { status ->
                if (status == TextToSpeech.SUCCESS) {
                    tts?.setLanguage(tts?.defaultLanguage ?: Locale.getDefault())
                    tts?.setSpeechRate(0.9f)
                    tts?.speak(
                        "Abriendo Calendario Mensual; Días Marcados En Verde, Es Que Tienes Registros De Gimnasio Efectivos, Ese Día En Concreto.",
                        TextToSpeech.QUEUE_FLUSH,
                        null,
                        "ttsFlexionesId"
                    )
                }
            }
        }
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        dao = AppDatabase.getDatabase(this).gimnasioDao()
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT
            )
            setPadding(16, 16, 16, 16)
            setBackgroundColor(Color.BLACK)
        }
        val navLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }
        val btnAnteriorMes = Button(this).apply { text = "<" }
        tvMesAnio = TextView(this).apply {
            textSize = 20f
            gravity = Gravity.CENTER
            setPadding(16, 0, 16, 0)
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        tvMesAnio.setOnClickListener {
            val input = EditText(this)
            input.inputType = android.text.InputType.TYPE_CLASS_NUMBER
            input.setText(currentYear.toString())
            input.setSelection(input.text.length)
            val dialog =
                AlertDialog.Builder(this).setTitle("Escribe Un Año Entre 1 Y 100000").setView(input)
                    .setPositiveButton("Aceptar") { _, _ ->
                        val año = input.text.toString().toIntOrNull()
                        if (año != null && año in 1..100000) {
                            currentYear = año
                            mostrarMes(currentYear, currentMonth, layout.width)
                            hablarMes()
                        } else if (ttsEnabled) {
                            tts?.speak(
                                "Año No Válido; Debe Estar Entre 1 Y 100000.",
                                TextToSpeech.QUEUE_FLUSH,
                                null,
                                "ttsSelector"
                            )
                        }
                    }.setNegativeButton("Cancelar") { _, _ ->
                        if (ttsEnabled) tts?.speak(
                            "Pues Nada...", TextToSpeech.QUEUE_FLUSH, null, "ttsSelector"
                        )
                    }.create()
            dialog.setOnCancelListener {
                if (ttsEnabled) tts?.speak(
                    "Cancelamos.", TextToSpeech.QUEUE_FLUSH, null, "ttsSelector"
                )
            }
            dialog.show()
            if (ttsEnabled) tts?.speak(
                "Escribe Un Año Entre 1 Y 100000.", TextToSpeech.QUEUE_FLUSH, null, "ttsSelector"
            )
        }
        btnSiguienteMes = Button(this).apply { text = ">" }
        navLayout.addView(btnAnteriorMes)
        navLayout.addView(tvMesAnio)
        navLayout.addView(btnSiguienteMes)
        gridDias = GridLayout(this).apply {
            columnCount = 7
            rowCount = GridLayout.UNDEFINED
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
            )
            setPadding(1, 0, 1, 0)
            clipToPadding = false
        }
        val containerGrid = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            addView(gridDias)
        }
        val tvLeyenda = TextView(this).apply {
            text =
                "Días En Verde = Tienes Registros En El Gimnasio Efectivos\nDebajo De Los Días, % De Luna Visible Y Dibujo De Luna En Hora Actual"
            setTextColor(Color.YELLOW)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = 20 }
        }
        val btnCalendarioAnual = Button(this).apply {
            text = "Calendarios Anuales Completos"
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = 16 }
        }
        val imageViewLogo = ImageView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0
            ).apply { weight = 1f; topMargin = 16; bottomMargin = 16; gravity = Gravity.CENTER }
            scaleType = ImageView.ScaleType.FIT_CENTER
        }
        val logos = arrayOf(
            R.drawable.logom8ax,
            R.drawable.logoapp,
            R.drawable.logom8ax2,
            R.drawable.logom8ax3,
            R.drawable.logom8ax4,
            R.drawable.logom8ax5
        )
        imageViewLogo.setImageResource(logos[Random.nextInt(logos.size)])
        layout.addView(navLayout)
        layout.addView(containerGrid)
        layout.addView(tvLeyenda)
        layout.addView(btnCalendarioAnual)
        layout.addView(imageViewLogo)
        scaleGestureDetector = ScaleGestureDetector(
            this, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
                override fun onScale(detector: ScaleGestureDetector): Boolean {
                    scaleFactor *= detector.scaleFactor
                    scaleFactor = scaleFactor.coerceIn(0.5f, 2.0f)
                    layout.scaleX = scaleFactor
                    layout.scaleY = scaleFactor
                    return true
                }
            })
        layout.setOnTouchListener { v, event ->
            scaleGestureDetector.onTouchEvent(event)
            true
        }
        setContentView(layout)
        mediaPlayer = MediaPlayer.create(this, R.raw.m8axsonidofondo)
        mediaPlayer?.isLooping = true
        mediaPlayer?.setVolume(0.5f, 0.5f)
        mediaPlayer?.start()
        val cal = Calendar.getInstance()
        currentYear = cal.get(Calendar.YEAR)
        currentMonth = cal.get(Calendar.MONTH)
        layout.viewTreeObserver.addOnGlobalLayoutListener(object :
            ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                layout.viewTreeObserver.removeOnGlobalLayoutListener(this)
                mostrarMes(currentYear, currentMonth, layout.width)
            }
        })
        btnAnteriorMes.setOnClickListener {
            if (currentMonth == 0) {
                currentMonth = 11; currentYear--
            } else currentMonth--
            mostrarMes(currentYear, currentMonth, layout.width)
            hablarMes()
        }
        btnSiguienteMes.setOnClickListener {
            if (currentMonth == 11) {
                currentMonth = 0; currentYear++
            } else currentMonth++
            mostrarMes(currentYear, currentMonth, layout.width)
            hablarMes()
        }
        btnCalendarioAnual.setOnClickListener {
            startActivity(Intent(this, CalendarioAnualActivity::class.java))
            if (ttsEnabled) {
                tts?.stop()
                tts?.speak(
                    "Calendarios Anuales Completos.",
                    TextToSpeech.QUEUE_FLUSH,
                    null,
                    "ttsFlexionesId"
                )
            }
        }
    }

    private fun esBisiesto(year: Int): Boolean {
        return (year % 4 == 0 && year % 100 != 0) || (year % 400 == 0)
    }

    private fun esXacobeo(year: Int): Boolean {
        val cal = Calendar.getInstance()
        cal.set(year, Calendar.JULY, 25)
        return cal.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY
    }

    private fun obtenerSufijoAnio(year: Int): String {
        val b = esBisiesto(year)
        val x = esXacobeo(year)
        return when {
            b && x -> " BX"
            b -> " B"
            x -> " X"
            else -> ""
        }
    }

    private fun hablarMes() {
        if (!ttsEnabled) return
        tts?.stop()
        val sufijo = when {
            nombreMesActual.endsWith("BX") -> "BX"
            nombreMesActual.endsWith("B") -> "B"
            nombreMesActual.endsWith("X") -> "X"
            else -> ""
        }
        val yearMatch = Regex("\\d{4}").find(nombreMesActual)
        val year = yearMatch?.value ?: currentYear.toString()
        val base = nombreMesActual.removeSuffix(sufijo).replace(year, "").trim()
        val textoTTS = when (sufijo) {
            "B" -> "$base De $year; Año Bisiesto."
            "X" -> "$base De $year; Año Xacobeo."
            "BX" -> "$base De $year; Año Bisiesto Y Xacobeo."
            else -> "$base De $year."
        }
        tts?.speak(textoTTS, TextToSpeech.QUEUE_FLUSH, null, "ttsMes")
    }

    private fun crearCelda(
        text: String, colorTexto: Int, fondoColor: Int? = null, negrita: Boolean = false
    ): TextView {
        val tv = TextView(this)
        tv.text = text
        tv.gravity = Gravity.CENTER
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
        tv.setTextColor(colorTexto)
        if (negrita) tv.setTypeface(tv.typeface, Typeface.BOLD)
        val param = GridLayout.LayoutParams(
            GridLayout.spec(GridLayout.UNDEFINED, 1f), GridLayout.spec(GridLayout.UNDEFINED, 1f)
        ).apply {
            width = 0
            height = GridLayout.LayoutParams.WRAP_CONTENT
        }
        tv.layoutParams = param
        val gd = GradientDrawable()
        gd.setStroke(2, Color.WHITE)
        gd.setColor(fondoColor ?: Color.BLACK)
        tv.background = gd
        return tv
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

    private fun mostrarMes(year: Int, month: Int, anchoTotal: Int) {
        if (jobMostrarMes?.isActive == true) {
            jobMostrarMes?.cancel()
        }
        val altoActual = gridDias.height
        gridDias.minimumHeight = altoActual
        gridDias.visibility = View.INVISIBLE
        gridDias.removeAllViews()
        val anchoCelda = anchoTotal / 7
        val cal = Calendar.getInstance()
        cal.set(year, month, 1)
        val usarDiminutivo = Random.nextBoolean()
        mesesMostrar = if (usarDiminutivo) meses2 else meses
        val sufijo = obtenerSufijoAnio(year)
        nombreMesActual = "${mesesMostrar[month]} $year$sufijo"
        tvMesAnio.text = nombreMesActual
        val diasSemana = listOf("LUN", "MAR", "MIÉ", "JUE", "VIE", "SÁB", "DOM")
        for (d in diasSemana) {
            val colorTexto = if (d == "SÁB" || d == "DOM") Color.RED else Color.WHITE
            gridDias.addView(crearCelda(d, colorTexto, Color.BLACK, true))
        }
        jobMostrarMes = lifecycleScope.launch(Dispatchers.IO) {
            val mesStr = String.format("%02d", month + 1)
            val anioStr = year.toString()
            val diasConDatos = dao.getDiasConDatos(mesStr, anioStr).map { it.toInt() }.toSet()
            withContext(Dispatchers.Main) {
                var primerDiaSemana = cal.get(Calendar.DAY_OF_WEEK) - 1
                if (primerDiaSemana == 0) primerDiaSemana = 7
                val diasMes = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
                val hoy = Calendar.getInstance()
                val esHoyMesActual =
                    (hoy.get(Calendar.YEAR) == year && hoy.get(Calendar.MONTH) == month)
                var celdaIndex = primerDiaSemana - 1
                for (i in 1 until primerDiaSemana) {
                    gridDias.addView(crearCelda("", Color.WHITE, Color.BLACK))
                    celdaIndex++
                }
                for (dia in 1..diasMes) {
                    cal.set(Calendar.DAY_OF_MONTH, dia)
                    var diaSemana = cal.get(Calendar.DAY_OF_WEEK) - 1
                    if (diaSemana == 0) diaSemana = 7
                    val colorTexto = when {
                        diasConDatos.contains(dia) -> Color.parseColor("#00FF00")
                        diaSemana == 6 || diaSemana == 7 -> Color.RED
                        else -> Color.WHITE
                    }
                    val fondo = if (esHoyMesActual && dia == hoy.get(Calendar.DAY_OF_MONTH)) {
                        Color.parseColor("#1E3A8A")
                    } else {
                        Color.BLACK
                    }
                    val diaTexto = if (diaSemana == 6 || diaSemana == 7) {
                        intToRoman(dia)
                    } else {
                        dia.toString()
                    }
                    val porcentajeLuna = getPorcentajeLuna(cal)
                    val texto = "$diaTexto\n$porcentajeLuna%"
                    val diaTextView = TextView(this@CalendarioActivity).apply {
                        setTextColor(colorTexto)
                        gravity = Gravity.CENTER
                        setTypeface(typeface, Typeface.BOLD)
                        val spannable = android.text.SpannableStringBuilder(texto)
                        val start = texto.indexOf("$porcentajeLuna%")
                        val end = start + "$porcentajeLuna%".length
                        spannable.setSpan(
                            android.text.style.ForegroundColorSpan(Color.YELLOW),
                            start,
                            end,
                            android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                        )
                        setText(spannable)
                    }
                    val lunaView = ImageView(this@CalendarioActivity)
                    val size = (15 * resources.displayMetrics.density).toInt()
                    val bitmap = android.graphics.Bitmap.createBitmap(
                        size, size, android.graphics.Bitmap.Config.ARGB_8888
                    )
                    val canvas = android.graphics.Canvas(bitmap)
                    val paint = android.graphics.Paint()
                        .apply { isAntiAlias = true; style = android.graphics.Paint.Style.FILL }
                    val r = size / 2f
                    paint.color = android.graphics.Color.rgb(40, 40, 40)
                    canvas.drawCircle(r, r, r, paint)
                    val moon = MoonIllumination.compute()
                        .on(cal.toInstant().atZone(ZoneId.systemDefault())).execute()
                    val f = moon.fraction.toFloat()
                    paint.color = android.graphics.Color.WHITE
                    for (x in 0 until size) {
                        val xRel = x - r
                        val yMax = Math.sqrt((r * r - xRel * xRel).toDouble()).toFloat()
                        val drawWhite =
                            if (moon.phase < 0.5) x >= size * (1f - f) else x <= size * f
                        if (drawWhite) canvas.drawLine(
                            x.toFloat(), r - yMax, x.toFloat(), r + yMax, paint
                        )
                    }
                    lunaView.setImageBitmap(bitmap)
                    val container = LinearLayout(this@CalendarioActivity).apply {
                        orientation = LinearLayout.VERTICAL
                        gravity = Gravity.CENTER
                        layoutParams = GridLayout.LayoutParams(
                            GridLayout.spec(GridLayout.UNDEFINED, 1f),
                            GridLayout.spec(GridLayout.UNDEFINED, 1f)
                        ).apply {
                            width = 0
                            height = anchoCelda
                        }
                        background = GradientDrawable().apply {
                            setColor(fondo)
                            setStroke(2, Color.GRAY)
                            cornerRadius = 8f
                        }
                        setPadding(0, (-9 * resources.displayMetrics.density).toInt(), 0, 0)
                        addView(
                            diaTextView, LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.MATCH_PARENT, 0, 2f
                            )
                        )
                        addView(
                            lunaView, LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f
                            ).apply {
                                topMargin = (-7.65 * resources.displayMetrics.density).toInt()
                            })
                    }
                    gridDias.addView(container)
                    celdaIndex++
                }
                gridDias.visibility = View.VISIBLE
                gridDias.minimumHeight = 0
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
    }

    override fun onResume() {
        super.onResume()
        mediaPlayer?.start()
    }

    override fun onPause() {
        super.onPause()
        mediaPlayer?.pause()
    }
}
