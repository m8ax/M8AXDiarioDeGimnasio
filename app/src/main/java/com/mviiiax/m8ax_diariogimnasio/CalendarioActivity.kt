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
import android.view.ViewTreeObserver
import android.widget.Button
import android.widget.GridLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.shredzone.commons.suncalc.MoonIllumination
import java.text.SimpleDateFormat
import java.time.ZoneId
import java.util.Calendar
import java.util.Locale
import kotlin.random.Random

class CalendarioActivity : AppCompatActivity() {
    private lateinit var tvMesAnio: TextView
    private lateinit var gridDias: GridLayout
    private lateinit var dao: GimnasioDao
    private var currentYear = 0
    private var currentMonth = 0
    private var scaleFactor = 1.0f
    private lateinit var scaleGestureDetector: ScaleGestureDetector
    var tts: TextToSpeech? = null
    var ttsEnabled = true
    private var mediaPlayer: MediaPlayer? = null
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val config = getSharedPreferences("M8AX-Config_TTS", MODE_PRIVATE)
        ttsEnabled = config.getBoolean("tts_enabled", false)
        if (ttsEnabled) {
            tts = TextToSpeech(this) { status ->
                if (status == TextToSpeech.SUCCESS) {
                    tts?.language = Locale.getDefault()
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
            textSize = 22f
            gravity = Gravity.CENTER
            setPadding(16, 0, 16, 0)
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        val btnSiguienteMes = Button(this).apply { text = ">" }
        navLayout.addView(btnAnteriorMes)
        navLayout.addView(tvMesAnio)
        navLayout.addView(btnSiguienteMes)
        gridDias = GridLayout(this).apply {
            columnCount = 7
            rowCount = 7
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
            )
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
        val logos = listOf(R.drawable.logoapp, R.drawable.logom8ax)
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
            if (ttsEnabled) {
                val nombreMes = meses[currentMonth]
                tts?.stop()
                tts?.speak(
                    "$nombreMes De $currentYear", TextToSpeech.QUEUE_FLUSH, null, "ttsFlexionesId"
                )
            }
        }
        btnSiguienteMes.setOnClickListener {
            if (currentMonth == 11) {
                currentMonth = 0; currentYear++
            } else currentMonth++
            mostrarMes(currentYear, currentMonth, layout.width)
            if (ttsEnabled) {
                val nombreMes = meses[currentMonth]
                tts?.stop()
                tts?.speak(
                    "$nombreMes De $currentYear", TextToSpeech.QUEUE_FLUSH, null, "ttsFlexionesId"
                )
            }
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

    private fun crearCelda(
        text: String, ancho: Int, colorTexto: Int, fondoColor: Int? = null, negrita: Boolean = false
    ): TextView {
        val tv = TextView(this)
        tv.text = text
        tv.gravity = Gravity.CENTER
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
        tv.setTextColor(colorTexto)
        if (negrita) tv.setTypeface(tv.typeface, Typeface.BOLD)
        val param = GridLayout.LayoutParams().apply { width = ancho; height = ancho }
        tv.layoutParams = param
        val gd = GradientDrawable()
        gd.setStroke(2, Color.WHITE)
        gd.setColor(fondoColor ?: Color.BLACK)
        tv.background = gd
        return tv
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

    private fun mostrarMes(year: Int, month: Int, anchoTotal: Int) {
        gridDias.removeAllViews()
        val anchoCelda = anchoTotal / 7
        val cal = Calendar.getInstance()
        cal.set(year, month, 1)
        val sdf = SimpleDateFormat("MMMM yyyy", Locale("es"))
        tvMesAnio.text = sdf.format(cal.time).replaceFirstChar { it.uppercase() }
        val diasSemana = listOf("LUN", "MAR", "MIÉ", "JUE", "VIE", "SÁB", "DOM")
        for (d in diasSemana) {
            val colorTexto = if (d == "SÁB" || d == "DOM") Color.RED else Color.WHITE
            gridDias.addView(crearCelda(d, anchoCelda, colorTexto, Color.BLACK, true))
        }
        lifecycleScope.launch(Dispatchers.IO) {
            val registros = dao.getAll()
            val diasConDatos = registros.filter {
                val sdfDia = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                val fecha = sdfDia.parse(it.fechaHora.substringBefore(" "))
                fecha?.let { f ->
                    val c = Calendar.getInstance()
                    c.time = f
                    c.get(Calendar.YEAR) == year && c.get(Calendar.MONTH) == month && (it.valor
                        ?: 0) > 0
                } ?: false
            }.map {
                val sdfDia = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                val fecha = sdfDia.parse(it.fechaHora.substringBefore(" "))!!
                val c = Calendar.getInstance()
                c.time = fecha
                c.get(Calendar.DAY_OF_MONTH)
            }.toSet()
            withContext(Dispatchers.Main) {
                var primerDiaSemana = cal.get(Calendar.DAY_OF_WEEK) - 1
                if (primerDiaSemana == 0) primerDiaSemana = 7
                val diasMes = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
                val hoy = Calendar.getInstance()
                val esHoyMesActual =
                    (hoy.get(Calendar.YEAR) == year && hoy.get(Calendar.MONTH) == month)
                for (i in 1 until primerDiaSemana) {
                    gridDias.addView(crearCelda("", anchoCelda, Color.WHITE))
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
                    val porcentajeLuna = getPorcentajeLuna(cal)
                    val texto = "$dia\n$porcentajeLuna%"
                    val diaTextView = TextView(this@CalendarioActivity).apply {
                        textSize = 16f
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
                        layoutParams = LinearLayout.LayoutParams(anchoCelda, anchoCelda)
                        background = GradientDrawable().apply {
                            setColor(fondo)
                            setStroke(2, Color.GRAY)
                            cornerRadius = 8f
                        }
                        addView(
                            diaTextView, LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.MATCH_PARENT, 0, 2f
                            )
                        )
                        addView(
                            lunaView, LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f
                            )
                        )
                    }
                    gridDias.addView(container)
                }
            }
        }
    }
}