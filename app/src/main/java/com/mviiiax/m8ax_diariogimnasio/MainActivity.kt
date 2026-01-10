package com.mviiiax.m8ax_diariogimnasio

import android.app.AlarmManager
import android.app.AlertDialog
import android.app.PendingIntent
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Typeface
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.provider.Settings
import android.speech.tts.TextToSpeech
import android.text.Editable
import android.text.InputFilter
import android.text.InputType
import android.text.SpannableString
import android.text.Spanned
import android.text.TextWatcher
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.view.Gravity
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.DatePicker
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.MaterialToolbar
import com.google.gson.stream.JsonWriter
import com.itextpdf.text.Anchor
import com.itextpdf.text.BaseColor
import com.itextpdf.text.Chunk
import com.itextpdf.text.Document
import com.itextpdf.text.Element
import com.itextpdf.text.Font
import com.itextpdf.text.Image
import com.itextpdf.text.Paragraph
import com.itextpdf.text.pdf.BaseFont
import com.itextpdf.text.pdf.PdfPCell
import com.itextpdf.text.pdf.PdfPTable
import com.itextpdf.text.pdf.PdfWriter
import com.itextpdf.text.pdf.draw.LineSeparator
import me.zhanghai.android.fastscroll.FastScrollerBuilder
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import org.shredzone.commons.suncalc.MoonIllumination
import org.shredzone.commons.suncalc.MoonTimes
import org.shredzone.commons.suncalc.SunTimes
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.io.OutputStreamWriter
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.Period
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.Random
import java.util.TimeZone
import java.util.Timer
import java.util.TimerTask
import kotlin.math.roundToInt

data class MediaGimnasio(
    val totalRegistros: Int,
    val mediaNormal: Double,
    val diasEntreRegistros: Int,
    val mediaRealDia: Double,
    val añosEntreFechas: Int,
    val diasRestantesEntreFechas: Int
)

data class GimnasioRegistro(
    val fechaHora: String, val valor: Int
)

class MainActivity : AppCompatActivity() {
    var tts: android.speech.tts.TextToSpeech? = null
    var mediaparacolor: Double = 0.0
    var ttsEnabled: Boolean = true
    var contadorActualizarTemp = 0
    var ultimoPrecioBTC: String = "BTC ➜ ??? $ - ??? € - En 24H ???"
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: GimnasioAdapter
    private lateinit var db: AppDatabase
    private lateinit var toolbar: MaterialToolbar
    private var mediaPlayer: MediaPlayer? = null
    private var tipoOrdenExport = 1
    private lateinit var textEncima: TextView
    private val handler = Handler(Looper.getMainLooper())
    private var contadorespe = 0
    private var temperatura: String = ""
    private var diasTexto: String = ""
    private var lastClickTime = 0L
    private var clickCount = 0
    private val CLICK_WINDOW_MS = 600L
    private val clickHandler = android.os.Handler(android.os.Looper.getMainLooper())
    private var clickRunnable: Runnable? = null
    private val CIERRE_DELAY_MS = 3 * 60 * 60 * 1000L

    fun calcularMedia(lista: List<Gimnasio>): MediaGimnasio {
        if (lista.isEmpty()) return MediaGimnasio(0, 0.0, 0, 0.0, 0, 0)
        val sdfFecha = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val fechasSoloDia = lista.mapNotNull {
            runCatching {
                sdfFecha.parse(it.fechaHora.substringBefore(" -").trim())
            }.getOrNull()
        }
        val fechaInicio =
            fechasSoloDia.minOrNull()?.toInstant()?.atZone(ZoneId.systemDefault())?.toLocalDate()
                ?: LocalDate.now()
        val fechaFin =
            fechasSoloDia.maxOrNull()?.toInstant()?.atZone(ZoneId.systemDefault())?.toLocalDate()
                ?: LocalDate.now()
        val diasEntreRegistros = ChronoUnit.DAYS.between(fechaInicio, fechaFin).toInt() + 1
        val periodo = Period.between(fechaInicio, fechaFin)
        val años = periodo.years
        val diasRestantes =
            ChronoUnit.DAYS.between(fechaInicio.plusYears(años.toLong()), fechaFin).toInt() + 1
        val listaValidos = lista.filter { it.valor > 0 }
        val totalRegistros = listaValidos.size
        val totalMinutos = listaValidos.sumOf { it.valor.toInt() }
        val mediaNormal = if (totalRegistros > 0) listaValidos.map { it.valor }.average() else 0.0
        val mediaRealDia =
            if (diasEntreRegistros > 0) totalMinutos.toDouble() / diasEntreRegistros else totalMinutos.toDouble()
        return MediaGimnasio(
            totalRegistros = totalRegistros,
            mediaNormal = mediaNormal,
            diasEntreRegistros = diasEntreRegistros,
            mediaRealDia = mediaRealDia,
            añosEntreFechas = años,
            diasRestantesEntreFechas = diasRestantes
        )
    }

    private val updateRunnable = object : Runnable {
        var totalMinRedondeado = 0
        override fun run() {
            val ahora = Calendar.getInstance()
            val porcentajeLuna = getPorcentajeLuna(ahora)
            val fase = faseluna(
                Calendar.getInstance().toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
            )
            val diaSemanaRaw =
                ahora.getDisplayName(Calendar.DAY_OF_WEEK, Calendar.LONG, Locale.getDefault())
                    ?: "?"
            val diaSemana =
                diaSemanaRaw.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
            val fechaHora =
                SimpleDateFormat("dd/MM/yyyy · HH:mm:ss", Locale.getDefault()).format(ahora.time)
            val porcentajeFormateado = String.format("%03d", porcentajeLuna)
            if (contadorActualizarTemp % 600 == 0) {
                val fechaInicioStr = db.gimnasioDao().getFechaInicio()
                val fechaFinStr = db.gimnasioDao().getFechaFin()
                if (fechaInicioStr == null || fechaFinStr == null) {
                    diasTexto = "En Gim ➜ 0a 0d | Media Real ➜ 0h 0m / Día"
                } else {
                    val formato = DateTimeFormatter.ofPattern("dd/MM/yyyy")
                    val fechaInicio = LocalDate.parse(fechaInicioStr, formato)
                    val fechaFin = LocalDate.parse(fechaFinStr, formato)
                    val periodo = Period.between(fechaInicio, fechaFin)
                    val años = periodo.years
                    val dias =
                        ChronoUnit.DAYS.between(fechaInicio.plusYears(años.toLong()), fechaFin) + 1
                    val diasEntreRegistros =
                        ChronoUnit.DAYS.between(fechaInicio, fechaFin).toInt() + 1
                    val sumaValores = db.gimnasioDao().getSumaValoresPositivos() ?: 0.0
                    val mediaRealDia =
                        if (diasEntreRegistros > 0) sumaValores / diasEntreRegistros else 0.0
                    totalMinRedondeado = mediaRealDia.roundToInt()
                    val horas = totalMinRedondeado / 60
                    val minutos = totalMinRedondeado % 60
                    val totalRegistross = db.gimnasioDao().getTotalRegistros()
                    val totalEnRomanos = GimnasioAdapter.intToRoman(totalRegistross)
                    val totalRegistrosActivos = db.gimnasioDao().getTotalRegistrosActivos()
                    val diasEntrenados = if (diasEntreRegistros > 0) {
                        val porcentaje =
                            totalRegistrosActivos.toDouble() / diasEntreRegistros * 100.0
                        (kotlin.math.round(porcentaje * 100) / 100.0)
                    } else 0.0
                    diasTexto =
                        "En Gim ➜ ${años}a ${dias}d | Media Real ➜ ${horas}h ${minutos}m / Día\n" + "R.T. ➜ ( $totalRegistross – $totalEnRomanos ) | Act.Real ➜ ( $diasEntrenados% )"
                }
                Thread {
                    ultimoPrecioBTC = obtenerPrecioBTC()
                    val temp = obtenerTemperaturaPorIP()
                    if (!temp.isNullOrEmpty()) {
                        runOnUiThread {
                            temperatura = temp
                        }
                    }
                }.start()
            }
            contadorActualizarTemp++
            mediaparacolor = totalMinRedondeado.toDouble()
            val colorFondo = when {
                mediaparacolor == 0.0 -> Color.BLACK
                mediaparacolor < 45.0 -> Color.parseColor("#660000")
                mediaparacolor < 61.0 -> Color.parseColor("#006600")
                mediaparacolor < 91.0 -> Color.parseColor("#666600")
                else -> Color.parseColor("#660066")
            }
            textEncima.text = if (!temperatura.isNullOrEmpty()) {
                "$diaSemana, $fechaHora · Luna Al $porcentajeFormateado% $fase\n$diasTexto\n$temperatura"
            } else {
                "$diaSemana, $fechaHora · Luna Al $porcentajeFormateado% $fase\n$diasTexto"
            }
            textEncima.setBackgroundColor(colorFondo)
            textEncima.setTextColor(Color.WHITE)
            handler.postDelayed(this, 1000)
        }
    }

    fun obtenerTemperaturaPorIP(): String? {
        return try {
            val client = OkHttpClient()
            val ipRequest = Request.Builder().url("http://ip-api.com/json/").build()
            val ipResponse = client.newCall(ipRequest).execute()
            if (!ipResponse.isSuccessful) return null
            val ipBody = ipResponse.body?.string() ?: return null
            val json = JSONObject(ipBody)
            val lat = json.optDouble("lat", Double.NaN)
            val lon = json.optDouble("lon", Double.NaN)
            if (lat.isNaN() || lon.isNaN()) return null
            val ciudad = json.optString("city", "?")
            val timezone = json.optString("timezone", "UTC")
            val ispOriginal = json.optString("isp", "ISP desconocido")
            var isp = ispOriginal
            if (ispOriginal.contains("telefonica", ignoreCase = true)) isp =
                isp.replace(Regex("(?i)telefonica"), "Telefónica")
            if (ispOriginal.contains("espana", ignoreCase = true)) isp =
                isp.replace(Regex("(?i)espana"), "España")
            if (ispOriginal.contains("españa", ignoreCase = true)) isp =
                isp.replace(Regex("(?i)españa"), "España")
            isp =
                isp.split(" ").joinToString(" ") { it.replaceFirstChar { c -> c.uppercaseChar() } }
            val asRaw = json.optString("as", "")
            val asn = asRaw.split(" ").firstOrNull() ?: "AS?"
            val weatherRequest =
                Request.Builder().url("https://wttr.in/${lat},${lon}?format=%t").build()
            val weatherResponse = client.newCall(weatherRequest).execute()
            if (!weatherResponse.isSuccessful) return null
            val temp = weatherResponse.body?.string()?.trim() ?: return null
            val tzOffset = TimeZone.getTimeZone(timezone).rawOffset / 3600000
            val ahora = ZonedDateTime.now(ZoneId.of(timezone))
            val sunTimes = SunTimes.compute().at(lat, lon).on(ahora.toLocalDate()).execute()
            val moonTimes = MoonTimes.compute().at(lat, lon).on(ahora.toLocalDate()).execute()
            "$ciudad ➜ $temp · UTC ${if (tzOffset >= 0) "+$tzOffset" else "$tzOffset"}\nLat ➜ ${
                "%.4f".format(
                    lat
                )
            } · Lon ➜ ${"%.4f".format(lon)}\nSalida Del Sol ➜ ${
                sunTimes.rise?.format(
                    DateTimeFormatter.ofPattern(
                        "HH:mm"
                    )
                ) ?: "--"
            } · Puesta Del Sol ➜ ${sunTimes.set?.format(DateTimeFormatter.ofPattern("HH:mm")) ?: "--"}\nSalida De La Luna ➜ ${
                moonTimes.rise?.format(
                    DateTimeFormatter.ofPattern("HH:mm")
                ) ?: "--"
            } · Puesta De La Luna ➜ ${moonTimes.set?.format(DateTimeFormatter.ofPattern("HH:mm")) ?: "--"}\nRed ➜ $isp ($asn)\nPrecio De $ultimoPrecioBTC"
        } catch (e: Exception) {
            null
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

    private fun programarAlarmaDiaria(hora: Int, minuto: Int) {
        val intent = Intent(this, GimnasioReminderReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val calendar = Calendar.getInstance().apply {
            timeInMillis = System.currentTimeMillis()
            set(Calendar.HOUR_OF_DAY, hora)
            set(Calendar.MINUTE, minuto)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (before(Calendar.getInstance())) add(Calendar.DAY_OF_MONTH, 1)
        }
        val alarmManager = getSystemService(ALARM_SERVICE) as AlarmManager
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            if (alarmManager.canScheduleExactAlarms()) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pendingIntent
                )
            }
        } else {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pendingIntent
            )
        }
    }

    private fun mostrarDialogoDiaOlvidado() {
        val inflater = LayoutInflater.from(this)
        val dialogView = inflater.inflate(R.layout.dialog_dia_perdido, null)
        val etValor = dialogView.findViewById<EditText>(R.id.etValor)
        val etDiario = dialogView.findViewById<EditText>(R.id.etDiario)
        val tvContador = dialogView.findViewById<TextView>(R.id.tvContador)
        val watcherContador = object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val len = s?.length ?: 0
                tvContador.text = "$len / 1000"
                tvContador.setTypeface(null, Typeface.BOLD)
                when {
                    len <= 500 -> tvContador.setTextColor(Color.parseColor("#2E7D32"))
                    len <= 750 -> tvContador.setTextColor(Color.parseColor("#EF6C00"))
                    else -> tvContador.setTextColor(Color.parseColor("#C62828"))
                }
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        }
        etDiario.addTextChangedListener(watcherContador)
        tvContador.text = "${etDiario.text.length} / 1000"
        etValor.inputType = InputType.TYPE_CLASS_NUMBER
        etValor.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val nuevoValor = s.toString().toIntOrNull()
                if (nuevoValor != null) {
                    val valorFinal = if (nuevoValor > 960) 960 else nuevoValor
                    if (valorFinal != nuevoValor) {
                        etValor.setText(valorFinal.toString())
                        etValor.setSelection(etValor.text.length)
                        Toast.makeText(
                            this@MainActivity, "Máximo Permitido 960 Minutos", Toast.LENGTH_LONG
                        ).show()
                        if (ttsEnabled) {
                            tts?.speak(
                                "Máximo Permitido; 960 Minutos. O Piensas Quedarte A Vivir En El Gimnasio.",
                                TextToSpeech.QUEUE_FLUSH,
                                null,
                                "ttsMaxMinutos"
                            )
                        }
                    }
                }
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
        etDiario.filters = arrayOf(InputFilter.LengthFilter(1000))
        val datePicker = dialogView.findViewById<DatePicker>(R.id.datePicker)
        if (ttsEnabled) {
            tts?.speak(
                "Añade A La Base De Datos Un Día Que Se Te Olvidó Añadir; También Podemos Sustituir Uno Ya Existente; Pero No Podremos Añadir Días Futuros.",
                TextToSpeech.QUEUE_FLUSH,
                null,
                "ttsRestaurarId"
            )
        }
        AlertDialog.Builder(this).setTitle("Añadir Día Olvidado").setView(dialogView)
            .setPositiveButton("Añadir") { _, _ ->
                val dia = datePicker.dayOfMonth
                val mes = datePicker.month
                val anio = datePicker.year
                val calendario = Calendar.getInstance()
                calendario.set(anio, mes, dia)
                val sdfFecha = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                val fechaSeleccionada = sdfFecha.format(calendario.time)
                if (calendario.time.after(Date())) {
                    Toast.makeText(this, "No Puedes Añadir Días Futuros.", Toast.LENGTH_SHORT)
                        .show()
                    if (ttsEnabled) {
                        tts?.speak(
                            "No Puedes Añadir Días Futuros.",
                            TextToSpeech.QUEUE_FLUSH,
                            null,
                            "ttsRestaurarId"
                        )
                    }
                    return@setPositiveButton
                }
                val dao = AppDatabase.getDatabase(this).gimnasioDao()
                val registros = dao.getAll()
                val registroExistente =
                    registros.find { it.fechaHora.startsWith(fechaSeleccionada) }
                val ahora = Calendar.getInstance()
                calendario.set(Calendar.HOUR_OF_DAY, ahora.get(Calendar.HOUR_OF_DAY))
                calendario.set(Calendar.MINUTE, ahora.get(Calendar.MINUTE))
                calendario.set(Calendar.SECOND, ahora.get(Calendar.SECOND))
                val sdfFechaHora = SimpleDateFormat("dd/MM/yyyy - HH:mm:ss", Locale.getDefault())
                val fechaHoraCompleta = sdfFechaHora.format(calendario.time)
                val valor = etValor.text.toString().toIntOrNull() ?: 0
                val diarioTexto = etDiario.text.toString()
                if (registroExistente != null) {
                    if (ttsEnabled) {
                        tts?.speak(
                            "Ya Existe Un Registro De Gimnasio Para Este Día; Quieres Actualizarlo?",
                            TextToSpeech.QUEUE_FLUSH,
                            null,
                            "ttsRestaurarId"
                        )
                    }
                    AlertDialog.Builder(this).setTitle("Registro Existente")
                        .setMessage("Ya Existe Un Registro De Gimnasio Para Este Día. ¿ Quieres Actualizarlo ?")
                        .setPositiveButton("Sí") { _, _ ->
                            registroExistente.valor = valor
                            registroExistente.diario = diarioTexto
                            registroExistente.fechaHora = fechaHoraCompleta
                            dao.update(registroExistente)
                            val sdfFull = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                            val listaOrdenada = dao.getAll()
                                .sortedByDescending { sdfFull.parse(it.fechaHora.substring(0, 10)) }
                            adapter.updateData(listaOrdenada)
                            Toast.makeText(
                                this, "Registro Actualizado Correctamente.", Toast.LENGTH_SHORT
                            ).show()
                            if (ttsEnabled) {
                                tts?.speak(
                                    "Registro Actualizado Correctamente.",
                                    TextToSpeech.QUEUE_FLUSH,
                                    null,
                                    "ttsRestaurarId"
                                )
                            }
                        }.setNegativeButton("No") { _, _ ->
                            if (ttsEnabled) {
                                tts?.speak(
                                    "Pues Nada; No Actualizamos El Registro.",
                                    TextToSpeech.QUEUE_FLUSH,
                                    null,
                                    "ttsRestaurarId"
                                )
                            }
                        }.show()
                } else {
                    val nuevoRegistro = Gimnasio(
                        fechaHora = fechaHoraCompleta, valor = valor, diario = diarioTexto
                    )
                    dao.insert(nuevoRegistro)
                    contadorActualizarTemp = 595
                    val sdfFull = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                    val listaOrdenada = dao.getAll()
                        .sortedByDescending { sdfFull.parse(it.fechaHora.substring(0, 10)) }
                    adapter.updateData(listaOrdenada)
                    Toast.makeText(this, "Registro Añadido Correctamente.", Toast.LENGTH_SHORT)
                        .show()
                    if (ttsEnabled) {
                        tts?.speak(
                            "Registro Añadido Correctamente.",
                            TextToSpeech.QUEUE_FLUSH,
                            null,
                            "ttsRestaurarId"
                        )
                    }
                }
            }.setNegativeButton("Cancelar") { _, _ ->
                if (ttsEnabled) {
                    tts?.speak(
                        "Cancelando...", TextToSpeech.QUEUE_FLUSH, null, "ttsRestaurarId"
                    )
                }
            }.show()
    }

    fun getImageFromDrawable(resId: Int): Image {
        val bitmap = BitmapFactory.decodeResource(resources, resId)
        val stream = ByteArrayOutputStream()
        bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, stream)
        val imageBytes = stream.toByteArray()
        return Image.getInstance(imageBytes)
    }

    private fun formatearTiempo(totalMin: Int): String {
        var minutosRestantes = totalMin
        val años = minutosRestantes / (60 * 24 * 365)
        minutosRestantes %= (60 * 24 * 365)
        val dias = minutosRestantes / (60 * 24)
        minutosRestantes %= (60 * 24)
        val horas = minutosRestantes / 60
        minutosRestantes %= 60
        val minutos = minutosRestantes
        return "${años} Años, ${dias} Días, ${horas} Horas Y ${minutos} Minutos."
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        setContentView(R.layout.activity_main)
        val config = getSharedPreferences("M8AX-Config_TTS", MODE_PRIVATE)
        if (!config.contains("tts_enabled")) {
            config.edit().putBoolean("tts_enabled", true).apply()
        }
        ttsEnabled = config.getBoolean("tts_enabled", true)
        toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        toolbar.setTitleTextColor(Color.WHITE)
        toolbar.overflowIcon?.setTint(Color.WHITE)
        supportActionBar?.title = obtenerTituloSegunTemporada()
        db = AppDatabase.getDatabase(this)
        recyclerView = findViewById(R.id.rvGimnasio)
        recyclerView.layoutManager = LinearLayoutManager(this)
        ttsEnabled =
            getSharedPreferences("M8AX-Config_TTS", MODE_PRIVATE).getBoolean("tts_enabled", true)
        val sdfFull = SimpleDateFormat("dd/MM/yyyy - HH:mm:ss", Locale.getDefault())
        val fechaHoraActual = sdfFull.format(Date())
        val hoy = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())
        var lista = db.gimnasioDao().getAll()
        val existeHoy = lista.any { it.fechaHora.startsWith(hoy) }
        if (!existeHoy) {
            val registro = Gimnasio(fechaHora = fechaHoraActual, valor = 0)
            db.gimnasioDao().insert(registro)
        }
        lista = db.gimnasioDao().getAll().sortedByDescending { sdfFull.parse(it.fechaHora) }
        adapter = GimnasioAdapter(lista, db.gimnasioDao(), this, ::hablar, ::decir)
        recyclerView.adapter = adapter
        FastScrollerBuilder(recyclerView).setPopupTextProvider { _, _ ->
            val layoutManager = recyclerView.layoutManager as LinearLayoutManager
            val firstVisible = layoutManager.findFirstVisibleItemPosition()
            val lastVisible = layoutManager.findLastVisibleItemPosition()
            if (firstVisible == RecyclerView.NO_POSITION || lastVisible == RecyclerView.NO_POSITION) {
                return@setPopupTextProvider ""
            }
            val recyclerCenterY = recyclerView.height / 2
            var closestPosition = firstVisible
            var minDistance = Int.MAX_VALUE
            for (i in firstVisible..lastVisible) {
                val child = layoutManager.findViewByPosition(i) ?: continue
                val childCenterY = (child.top + child.bottom) / 2
                val distance = kotlin.math.abs(childCenterY - recyclerCenterY)
                if (distance < minDistance) {
                    minDistance = distance
                    closestPosition = i
                }
            }
            val registro = adapter.items[closestPosition]
            registro.fechaHora.substring(0, 10)
        }.build()
        recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                val currentFocus = recyclerView.findFocus()
                if (currentFocus !is EditText) {
                    recyclerView.clearFocus()
                }
            }

            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
                val currentFocus = recyclerView.findFocus()
                if (newState == RecyclerView.SCROLL_STATE_IDLE && currentFocus !is EditText) {
                    recyclerView.clearFocus()
                }
            }
        })
        mediaPlayer = MediaPlayer.create(this, R.raw.m8axgimnasio)
        mediaPlayer?.isLooping = true
        mediaPlayer?.start()
        if (savedInstanceState == null) {
            tts = TextToSpeech(this) { status ->
                if (status == TextToSpeech.SUCCESS) {
                    tts?.setLanguage(tts?.defaultLanguage ?: Locale.getDefault())
                    tts?.setSpeechRate(0.9f)
                    val calendar = Calendar.getInstance()
                    val hora = calendar.get(Calendar.HOUR_OF_DAY)
                    val minutos = calendar.get(Calendar.MINUTE)
                    val horaMinutos = String.format("%02d:%02d", hora, minutos)
                    val saludoBase = when (hora) {
                        in 6..11 -> "Buenos Días, Son Las ${horaMinutos}"
                        in 12..19 -> "Buenas Tardes, Son Las ${horaMinutos}"
                        else -> "Buenas Noches. Son Las ${horaMinutos}, No Es Hora De Descansar O Que..."
                    }
                    val listaRegistros = db.gimnasioDao().getAll().filter { it.valor > 0 }
                    val totalRegistros = listaRegistros.size
                    val mediaGimnasio =
                        if (totalRegistros > 0) listaRegistros.map { it.valor }.average() else 0.0
                    val cositanueva = mediaGimnasio.roundToInt()
                    val xxhoras = cositanueva / 60
                    val xxminutos = cositanueva % 60
                    val mediaFormateada = String.format("%.1f", mediaGimnasio)
                    val mensajeCompleto = buildString {
                        append("Hola M 8 A X, $saludoBase. ")
                        if (totalRegistros == 0) {
                            append("Aún No Tienes Ningún Registro De Gimnasio.")
                        } else {
                            append("En total Tienes $totalRegistros Registros Válidos De Gimnasio. Que Has Hecho Algo... Vaya... ")
                            append("La Media De Todos Ellos Es De $mediaFormateada Minutos Haciendo Ejercicio; Osea $xxhoras Horas Y $xxminutos Minutos. ")
                            when {
                                mediaGimnasio < 45 -> append("Tu Media Es Baja, Hay Que Hacer Más Ejercicio No Crees?.")
                                mediaGimnasio < 61 -> append("Tu Media De Ejercicio, Está Dentro Del Rango Recomendado, Bien Hecho Campeón!")
                                mediaGimnasio < 91 -> append("Tu Media Es Ligeramente Alta, Con Una Hora Al Día De Ejercicio, Es Suficiente.")
                                else -> append("Tu Media Es Alta, Tampoco Hace Falta Matarse Haciendo Ejercicio, No Te Parece?")
                            }
                        }
                        val listaTodos = db.gimnasioDao().getAll()
                        if (listaTodos.isNotEmpty()) {
                            val media = calcularMedia(listaTodos)
                            val mediaMinutos = String.format("%.1f", media.mediaRealDia)
                            val totalMinReal = media.mediaRealDia.roundToInt()
                            val horas = totalMinReal / 60
                            val minutosReal = totalMinReal % 60
                            val transcu =
                                "${media.añosEntreFechas} Años Y ${media.diasRestantesEntreFechas} Días"
                            val mensajeAppend = listOf(
                                "Que Todo Vaya Genial!. Media Real De Ejercicio Por Día En Los $transcu Desde Tu Primer Registro Al Último; $mediaMinutos Minutos; O Si Lo Prefieres; (${horas} Horas Y ${minutosReal} Minutos.)",
                                "Excelente Trabajo!. Durante Los Últimos $transcu, Tu Promedio Real Es De $mediaMinutos Minutos De Ejercicio Diario; O Si Lo Prefieres; (${horas} Horas Y ${minutosReal} Minutos.)",
                                "Genial Esfuerzo!. Tu Media Real Por Día En Estos $transcu; Fue De $mediaMinutos Minutos; O Si Lo Prefieres; (${horas} Horas Y ${minutosReal} Minutos.)",
                                "Perfecto!. En Los $transcu Desde Tu Primer Registro Al Último, Has Alcanzado Una Media Real De $mediaMinutos Minutos Por Día; O Si Lo Prefieres; (${horas} Horas Y ${minutosReal} Minutos.)",
                                "Sigue Así!. Promedio Real Diario De Ejercicio En Los $transcu: $mediaMinutos Minutos; O Alternativamente; (${horas} Horas Y ${minutosReal} Minutos.)",
                                "Fantástico!. Media Real De Ejercicio Por Día Durante Los $transcu: $mediaMinutos Minutos; O Si Lo Prefieres; (${horas} Horas Y ${minutosReal} Minutos.)"
                            )
                            val mensajeFinAppend = mensajeAppend.random()
                            append(" $mensajeFinAppend")
                            mediaparacolor = media?.mediaRealDia ?: 0.0
                            when {
                                media.mediaRealDia < 45 -> append(" Atención, Paciente Humano: Tus Minutos De Ejercicio Están Bajitos, Recomiendo Ejecutar Más Series Para Evitar Sobrecarga De Sedentarismo.")
                                media.mediaRealDia < 61 -> append(" Informe Optimizado: Tu Media De Ejercicio Está En Rango Ideal, Sistema Cardiovascular Funcionando Correctamente, Bien Hecho Marcos!")
                                media.mediaRealDia < 91 -> append(" Diagnóstico Ligeramente Extremo: Tu Media Es Alta, Ritmo Cardiaco Estable, Continúa Con Esta Precisión De Ingeniero, Pero No Te Sobrecalientes!.")
                                else -> append(" Alerta Máxima: Tu Media Es Muy Alta, Riesgo De Sobrecarga Detectado, Recomiendo Descanso Programado. Incluso Los Cyborgs Necesitan Recargar Baterías!")
                            }
                        } else {
                            append(" Que Tengas Un Buen Día.")
                        }
                    }
                    if (ttsEnabled) {
                        if (mediaGimnasio > 0.0) {
                            tts?.speak(mensajeCompleto, TextToSpeech.QUEUE_FLUSH, null, "saludoId")
                        } else {
                            val mensajesMotivadores = listOf(
                                "Ni Un Minuto De Ejercicio. Hoy Es Un Buen Día Para Empezar En El Gimnasio.",
                                "Ni Un Minuto De Ejercicio. Un Pequeño Entrenamiento Hoy Marca La Diferencia Mañana.",
                                "Aún No Me Has Hecho Ná De Ná. El Primer Paso Es Levantarse Y Moverse.",
                                "Tu Cuerpo Te Está Esperando, Vamos Al Gimnasio, Que Aún No Has Hecho Ni Un Minuto De Ejercicio.",
                                "Empieza Suave, Pero Empieza Hoy De Una Vez, Que No Tienes Nada Apuntado Aún."
                            )
                            tts?.speak(
                                mensajesMotivadores.random(),
                                TextToSpeech.QUEUE_FLUSH,
                                null,
                                "saludoId"
                            )
                        }
                    }
                }
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
            if (!alarmManager.canScheduleExactAlarms()) {
                if (ttsEnabled) {
                    tts?.speak(
                        "Permiso De Alarmas Exactas Requerido.",
                        TextToSpeech.QUEUE_FLUSH,
                        null,
                        "saludoId"
                    )
                }
                AlertDialog.Builder(this).setTitle("Permiso De Alarmas Exactas Requerido")
                    .setMessage("Para Que La Aplicación Pueda Recordarte Tus Sesiones De Gimnasio Con Precisión, Necesito Que Actives Las Alarmas Exactas...")
                    .setPositiveButton("Activar") { _, _ ->
                        val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                        if (ttsEnabled) {
                            tts?.speak(
                                "Permiso Concedido.", TextToSpeech.QUEUE_FLUSH, null, "saludoId"
                            )
                        }
                        startActivity(intent)
                    }.setNegativeButton("Cancelar") { _, _ ->
                        if (ttsEnabled) {
                            tts?.speak(
                                "Vale; De Acuerdo.", TextToSpeech.QUEUE_FLUSH, null, "saludoId"
                            )
                        }
                        Toast.makeText(this, "Vale... De Acuerdo...", Toast.LENGTH_LONG).show()
                    }.show()
            } else {
                programarAlarmaDiaria(16, 0)
            }
        } else {
            programarAlarmaDiaria(16, 0)
        }
        textEncima = findViewById(R.id.textEncima)
        textEncima.setOnClickListener {
            val now = System.currentTimeMillis()
            if (now - lastClickTime > CLICK_WINDOW_MS) clickCount = 0
            lastClickTime = now
            clickCount++
            clickRunnable?.let { clickHandler.removeCallbacks(it) }
            if (clickCount == 5) {
                clickCount = 0
                clickRunnable = null
                mailUltimoEntrenamientoGim()
                return@setOnClickListener
            }
            clickRunnable = Runnable {
                when (clickCount) {
                    1 -> {
                        var texto = textEncima.text.toString()
                        if (ttsEnabled && texto.isNotEmpty()) {
                            texto = texto.replace(
                                Regex("\\bLat\\b", RegexOption.IGNORE_CASE), "Latitud; "
                            ).replace(Regex("\\bLon\\b", RegexOption.IGNORE_CASE), "Longitud; ")
                                .replace(Regex("\\bGim\\b", RegexOption.IGNORE_CASE), "Gimnasio; ")
                                .replace("·", "\n").replace("→", "\n").replace("|", "\n")
                                .replace(Regex("(\\d+)a\\b"), "$1 Años")
                                .replace(Regex("(\\d+)d\\b"), " Y $1 Días")
                                .replace(Regex("(?<=% )CC(?=\\s|$)"), ", Cuarto Creciente. ")
                                .replace(Regex("(?<=% )GC(?=\\s|$)"), ", Gibosa Creciente. ")
                                .replace(Regex("(?<=% )LL(?=\\s|$)"), ", Luna Llena. ")
                                .replace(Regex("(?<=% )GM(?=\\s|$)"), ", Gibosa Menguante. ")
                                .replace(Regex("(?<=% )CM(?=\\s|$)"), ", Cuarto Menguante. ")
                                .replace(Regex("(?<=% )LN(?=\\s|$)"), ", Luna Nueva. ")
                                .replace(Regex("(\\(\\s*\\d+)\\s*–\\s*[^)]+(\\))"), "$1$2")
                                .replace(Regex("En 24h ↑ \\+([0-9]+\\.[0-9]+)%")) { match ->
                                    val numero = match.groupValues[1].replace(".", " Coma ")
                                    "En 24 Horas Ha Subido Un $numero%"
                                }.replace(Regex("En 24h ↓ -([0-9]+\\.[0-9]+)%")) { match ->
                                    val numero = match.groupValues[1].replace(".", " Coma ")
                                    "En 24 Horas Ha Bajado Un $numero%"
                                }.replace(
                                    Regex("En 24h → 0\\.000%"), "En 24 Horas Se Ha Mantenido Igual"
                                ).replace(
                                    Regex("BTC ➜ ([0-9]+) \\$ - ([0-9]+) €"),
                                    "BTC $1 Dólares; O $2 Euros."
                                )
                                .replace(Regex("(\\d+)%")) { "${it.groupValues[1].toInt()} Por Ciento" }
                                .replace("\n", ". ").replace(Regex(" (?<=\\s)/(?=\\s) "), " Por ")
                                .replace(
                                    Regex("\\bAct\\.Real\\b"),
                                    ". Actividad Real Desde Que Empezaste A Entrenar; "
                                ).replace(Regex("\\bR\\.T\\."), "Registros Totales; ")
                            tts?.stop()
                            tts?.speak(texto, TextToSpeech.QUEUE_FLUSH, null, "tts1")
                        }
                    }

                    2 -> {
                        Toast.makeText(this, "Voz Parada", Toast.LENGTH_SHORT).show()
                        decir("Voz Parada")
                    }
                }
                clickCount = 0
            }
            clickHandler.postDelayed(clickRunnable!!, CLICK_WINDOW_MS)
        }
        handler.post(updateRunnable)
    }

    fun hx(vararg b: Int): String =
        ByteArray(b.size) { i -> b[i].toByte() }.toString(Charsets.UTF_8)

    private fun mailUltimoEntrenamientoGim() {
        val listaRegistros = db.gimnasioDao().getAll().filter { it.valor > 0 }
        val ultimoRegistro = listaRegistros.lastOrNull() ?: return
        if (ultimoRegistro.valor <= 0 || ultimoRegistro.diario.isNullOrEmpty()) return
        val fechaStr = ultimoRegistro.fechaHora.take(10)
        val formato = DateTimeFormatter.ofPattern("dd/MM/yyyy")
        val fecha = LocalDate.parse(fechaStr, formato)
        if (fecha != LocalDate.now()) return
        val DiaAnio = fecha.dayOfYear.toString()
        // Toast.makeText(
        //     this, "Enviando Email, Con El Registro Del Entrenamiento De Hoy", Toast.LENGTH_LONG
        //) .show()
        // decir("Enviando Email; Con El Registro Del Entrenamiento De Hoy.")
        val tm = ultimoRegistro.valor.toInt()
        val hs = tm / 60
        val ms = tm % 60
        val mf = String.format("%02dh %02dm", hs, ms)
        val yearActual = java.util.Calendar.getInstance().get(Calendar.YEAR)
        val yearRomano = intToRoman(yearActual)
        val separador = "═════════════════════════════"
        Thread {
            try {
                val props = java.util.Properties().apply {
                    put("mail.smtp.auth", "true")
                    put("mail.smtp.starttls.enable", "true")
                    put("mail.smtp.host", "smtp.gmail.com")
                    put("mail.smtp.port", "587")
                }
                val session = javax.mail.Session.getInstance(
                    props, object : javax.mail.Authenticator() {
                        override fun getPasswordAuthentication() =
                            javax.mail.PasswordAuthentication(
                                "ejemplotucorreo@gmail.com",
                                "contraseña de app busca crear contraseña de app en google"
                            )
                    })
                val message = javax.mail.internet.MimeMessage(session).apply {
                    setFrom(javax.mail.internet.InternetAddress("ejemplotucorreo@gmail.com"))
                    setRecipients(
                        javax.mail.Message.RecipientType.TO,
                        javax.mail.internet.InternetAddress.parse("correodestino@gmail.com")
                    )
                    subject =
                        "----- Nueva Entrada De Gimnasio → ( ${ultimoRegistro.id} - ${ultimoRegistro.fechaHora} ) -----"
                    setText(
                        "${separador}\n\n${textEncima.text}\n\n${separador}\n\nRegistro - ${ultimoRegistro.id}-DA${DiaAnio} → ${ultimoRegistro.fechaHora}\nTiempo De Ejercicio → $mf\nDiario → ${ultimoRegistro.diario}\n\n${separador}\n\n||| By M8AX Corp. ( $yearActual - $yearRomano ) |||\n\n${separador}",
                        "UTF-8"
                    )
                }
                javax.mail.Transport.send(message)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }.start()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        menu?.findItem(R.id.action_toggle_tts)?.isChecked = ttsEnabled
        return true
    }

    private fun showTickerDialog() {
        val editText = EditText(this)
        if (ttsEnabled) {
            tts?.speak(
                "Escribe El Mensaje Que Quieres Que Salga A Modo De Ticker, De Izquiera A Derecha.",
                TextToSpeech.QUEUE_FLUSH,
                null,
                "ttsRestaurarId"
            )
        }
        editText.hint = "Escribe Tu Mensaje ( Máx 1000 Caracteres )"
        editText.filters = arrayOf(InputFilter.LengthFilter(1000))
        AlertDialog.Builder(this).setTitle("Mensaje Del Ticker").setView(editText)
            .setPositiveButton("OK") { _, _ ->
                val texto = editText.text.toString().trim()
                if (texto.isEmpty()) {
                    if (ttsEnabled) {
                        tts?.speak(
                            "El Texto Está Vacío, Así Que Nada.",
                            TextToSpeech.QUEUE_FLUSH,
                            null,
                            "ttsTextoVacioId"
                        )
                    }
                    return@setPositiveButton
                }
                val mensaje =
                    texto.split(" ").joinToString(" ") { it.capitalize(Locale.getDefault()) }
                if (ttsEnabled) {
                    tts?.speak(
                        "Vale, Adelante.", TextToSpeech.QUEUE_FLUSH, null, "ttsOkId"
                    )
                }
                val intent = Intent(this, TickerActivity::class.java)
                intent.putExtra("mensaje_ticker", mensaje)
                startActivity(intent)
            }.setNegativeButton("Cancelar") { _, _ ->
                if (ttsEnabled) {
                    tts?.speak(
                        "Vale, Pues No Hacemos Nada.",
                        TextToSpeech.QUEUE_FLUSH,
                        null,
                        "ttsCancelarId"
                    )
                }
            }.show()
    }

    private fun obtenerTituloSegunTemporada(): String {
        val calendar = Calendar.getInstance()
        val mes = calendar.get(Calendar.MONTH) + 1
        val dia = calendar.get(Calendar.DAY_OF_MONTH)
        val icono = if ((mes == 12 && dia >= 20) || (mes == 1 && dia <= 6)) {
            val iconosNavidad = listOf(
                "🎅",
                "🎄",
                "⛄",
                "🎁",
                "🕯️",
                "❄️",
                "✨",
                "🌟",
                "🛷",
                "🍪",
                "🦌",
                "🛎️",
                "🎶",
                "🍫",
                "☃️",
                "🧦",
                "🥶",
                "🕸️",
                "🎉",
                "🍷"
            )
            iconosNavidad.random()
        } else {
            val iconosGym = listOf(
                "💪",
                "🔥",
                "🏋️‍♂️",
                "⚡",
                "🥇",
                "🏆",
                "🦾",
                "🥵",
                "🏃‍♂️",
                "🩸",
                "🧘‍♂️",
                "🤸‍♂️",
                "🥊",
                "🛡️",
                "🏅",
                "🪢",
                "🧱",
                "🪵",
                "🪓",
                "🏐"
            )
            iconosGym.random()
        }
        return "$icono - Diario De Gimnasio De M8AX - $icono"
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_restaurar_db_downloads -> {
                val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                    type = "*/*"
                    addCategory(Intent.CATEGORY_OPENABLE)
                }
                startActivityForResult(intent, REQUEST_CODE_RESTAURAR_DB)
                if (ttsEnabled) {
                    tts?.speak(
                        "Selecciona El Backup, Que Deseas Restaurar.",
                        TextToSpeech.QUEUE_FLUSH,
                        null,
                        "ttsRestaurarId"
                    )
                }
                true
            }

            R.id.action_añadir_dia_olvidado -> {
                mostrarDialogoDiaOlvidado()
                return true
            }

            R.id.action_clear_0 -> {
                Thread {
                    val registrosInvalidos = db.gimnasioDao().getAll().filter { it.valor == 0 }
                    runOnUiThread {
                        if (registrosInvalidos.isEmpty()) {
                            Toast.makeText(
                                this, "No Hay Registros Inválidos Con 0 Min", Toast.LENGTH_SHORT
                            ).show()
                            if (ttsEnabled) {
                                tts?.speak(
                                    "No Hay Registros Inválidos Con 0 Minutos De Gimnasio.",
                                    TextToSpeech.QUEUE_FLUSH,
                                    null,
                                    "ttsNoRegistrosId"
                                )
                            }
                            return@runOnUiThread
                        }
                        Toast.makeText(
                            this,
                            "Hay ${registrosInvalidos.size} Registros Inválidos, Con 0 Min",
                            Toast.LENGTH_SHORT
                        ).show()
                        if (ttsEnabled) {
                            tts?.speak(
                                "Hay ${registrosInvalidos.size} Registros Inválidos Con 0 Minutos De Gimnasio. ¿Quieres Borrarlos?",
                                TextToSpeech.QUEUE_FLUSH,
                                null,
                                "ttsPreguntaId"
                            )
                        }
                        AlertDialog.Builder(this)
                            .setTitle("Borrar Registros Inválidos - ( 0 Min Gimnasio )")
                            .setMessage("Hay ${registrosInvalidos.size} Registros Inválidos Con 0 Minutos De Gimnasio. ¿ Quieres Borrarlos ?")
                            .setPositiveButton("Sí") { dialog, _ ->
                                Thread {
                                    db.gimnasioDao().borrarConValorCero()
                                    val nuevaLista = db.gimnasioDao().getAll()
                                    runOnUiThread {
                                        adapter.updateData(nuevaLista)
                                        contadorActualizarTemp = 595
                                        val cantidadBorrados = registrosInvalidos.size
                                        Toast.makeText(
                                            this,
                                            "$cantidadBorrados Registros Inválidos Borrados",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                        if (ttsEnabled) {
                                            tts?.speak(
                                                "$cantidadBorrados Registros Con 0 Minutos De Gimnasio Eliminados Correctamente.",
                                                TextToSpeech.QUEUE_FLUSH,
                                                null,
                                                "ttsConfirmId"
                                            )
                                        }
                                    }
                                }.start()
                                dialog.dismiss()
                            }.setNegativeButton("No") { dialog, _ ->
                                Toast.makeText(
                                    this, "Cancelando", Toast.LENGTH_SHORT
                                ).show()
                                if (ttsEnabled) {
                                    tts?.speak(
                                        "Cancelando Operación.",
                                        TextToSpeech.QUEUE_FLUSH,
                                        null,
                                        "ttsCancelId"
                                    )
                                }
                                dialog.dismiss()
                            }.show()
                    }
                }.start()
                true
            }

            R.id.menu_open_wiki -> {
                if (ttsEnabled) {
                    val textoWiki = "Preparando Un Artículo De WikiPedia; Para Lectura Automática."
                    tts?.speak(
                        textoWiki, TextToSpeech.QUEUE_FLUSH, null, "ttsWikiId"
                    )
                }
                Handler().postDelayed({
                    val intent = Intent(this, WikiInfinityActivity::class.java)
                    startActivity(intent)
                }, 5000)
            }

            R.id.menu_open_wh -> {
                if (ttsEnabled) {
                    val textoWiki = "Okey"
                    tts?.speak(
                        textoWiki, TextToSpeech.QUEUE_FLUSH, null, "ttsWikiId"
                    )
                }
                Handler().postDelayed({
                    val intent = Intent(this, WikiHowActivity::class.java)
                    startActivity(intent)
                }, 1000)
            }

            R.id.action_compra -> {
                val intent = Intent(this, ListaCompraActivity::class.java)
                if (ttsEnabled) {
                    val textoWiki = "Vámonos De Compras..."
                    tts?.speak(
                        textoWiki, TextToSpeech.QUEUE_FLUSH, null, "ttsWikiId"
                    )
                }
                startActivity(intent)
                true
            }

            R.id.menu_celebresvoz -> {
                if (ttsEnabled) {
                    val textoWiki = "Okey"
                    tts?.speak(
                        textoWiki, TextToSpeech.QUEUE_FLUSH, null, "ttsWikiId"
                    )
                }
                Handler().postDelayed({
                    val intent = Intent(this, CelebresVozActivity::class.java)
                    startActivity(intent)
                }, 1000)
            }

            R.id.action_export_pdf -> {
                if (ttsEnabled) {
                    tts?.speak(
                        "Elige El Método De Ordenación, Para Crear El P D F.",
                        TextToSpeech.QUEUE_FLUSH,
                        null,
                        "ttsRelojGrandeId"
                    )
                }
                AlertDialog.Builder(this).setTitle("Ordenar La Exportación Del PDF").setItems(
                    arrayOf(
                        "1. Fecha ---> ( Más Reciente Primero )",
                        "2. Fecha ---> ( Más Antigua Primero )",
                        "3. Minutos De Ejercicio ---> ( De + A - )",
                        "4. Longitud Del Diario ---> ( De + A - )"
                    )
                ) { _, which ->
                    tipoOrdenExport = which + 1
                    exportPdf()
                }.show()
                return true
            }

            R.id.action_ticker -> {
                showTickerDialog()
                true
            }

            R.id.action_export_txt -> {
                if (ttsEnabled) {
                    tts?.speak(
                        "Elige El Método De Ordenación, Para Crear El T X T.",
                        TextToSpeech.QUEUE_FLUSH,
                        null,
                        "ttsRelojGrandeId"
                    )
                }
                AlertDialog.Builder(this).setTitle("Ordenar La Exportación Del TXT").setItems(
                    arrayOf(
                        "1. Fecha ---> ( Más Reciente Primero )",
                        "2. Fecha ---> ( Más Antigua Primero )",
                        "3. Minutos De Ejercicio ---> ( De + A - )",
                        "4. Longitud Del Diario ---> ( De + A - )"
                    )
                ) { _, which ->
                    tipoOrdenExport = which + 1
                    exportTxt()
                }.show()
                return true
            }

            R.id.menu_cripto_precios -> {
                if (ttsEnabled) {
                    tts?.speak(
                        "Adelante Con Los Cripto Precios.",
                        TextToSpeech.QUEUE_FLUSH,
                        null,
                        "ttsRelojGrandeId"
                    )
                }
                startActivity(Intent(this, CriptoPrecios::class.java))
                true
            }

            R.id.action_export_json -> {
                if (ttsEnabled) {
                    tts?.speak(
                        "Elige El Método De Ordenación, Para Crear El Json.",
                        TextToSpeech.QUEUE_FLUSH,
                        null,
                        "ttsRelojGrandeId"
                    )
                }
                AlertDialog.Builder(this).setTitle("Ordenar La Exportación Del JSON").setItems(
                    arrayOf(
                        "1. Fecha ---> ( Más Reciente Primero )",
                        "2. Fecha ---> ( Más Antigua Primero )",
                        "3. Minutos De Ejercicio ---> ( De + A - )",
                        "4. Longitud Del Diario ---> ( De + A - )"
                    )
                ) { _, which ->
                    tipoOrdenExport = which + 1
                    exportJson()
                }.show()
                return true
            }

            R.id.menu_reloj_grande -> {
                val intent = Intent(this, RelojGrandeActivity::class.java)
                startActivity(intent)
                return true
            }

            R.id.action_calendario_mensual -> {
                val intent = Intent(this, CalendarioActivity::class.java)
                if (ttsEnabled) {
                    tts?.stop()
                }
                startActivity(intent)
                return true
            }

            R.id.action_tiempo -> {
                val intent = Intent(this, ElTiempoActivity::class.java)
                if (ttsEnabled) {
                    tts?.speak(
                        "A Ver; A Ver, Aver... Que Tiempo Hace?",
                        TextToSpeech.QUEUE_FLUSH,
                        null,
                        "ttsFlexionesId"
                    )
                }
                startActivity(intent)
                return true
            }

            R.id.action_tiempo2 -> {
                val intent = Intent(this, ElTiempoFullComunidad::class.java)
                if (ttsEnabled) {
                    tts?.speak(
                        "A Ver; Que Tiempo Hará En... Yo Que Se Donde...",
                        TextToSpeech.QUEUE_FLUSH,
                        null,
                        "ttsFlexionesId"
                    )
                }
                startActivity(intent)
                return true
            }

            R.id.action_pong -> {
                val intent = Intent(this, ThePong::class.java)
                if (ttsEnabled) {
                    tts?.speak(
                        "Venga!; A Echar Una Partida Al Pong.",
                        TextToSpeech.QUEUE_FLUSH,
                        null,
                        "ttsFlexionesId"
                    )
                }
                startActivity(intent)
                return true
            }

            R.id.action_rutinas -> {
                val intent = Intent(this, RutinasGim::class.java)
                if (ttsEnabled) {
                    tts?.speak(
                        "Venga!; Vamos A Planificar Rutinas Y A Sudar Un Poco.",
                        TextToSpeech.QUEUE_FLUSH,
                        null,
                        "ttsFlexionesId"
                    )
                }
                startActivity(intent)
                return true
            }

            R.id.action_navidad -> {
                val intent = Intent(this, NavidadActivity::class.java)
                if (ttsEnabled) {
                    tts?.speak(
                        "Cada Segundo Cuenta; No Los Desperdicies Tontamente.",
                        TextToSpeech.QUEUE_FLUSH,
                        null,
                        "ttsFlexionesId"
                    )
                }
                startActivity(intent)
                return true
            }

            R.id.menu_calendario_pdf -> {
                val intent = Intent(this, ActivityCalendarioAnual::class.java)
                if (ttsEnabled) {
                    tts?.speak(
                        "Genera Un Calendario En P D F; Con El Año O Intervalos De Años Que Elijas. Con Dibujitos Reales De La Luna Y Todo...",
                        TextToSpeech.QUEUE_FLUSH,
                        null,
                        "ttsFlexionesId"
                    )
                }
                startActivity(intent)
                return true
            }

            R.id.action_conecta4 -> {
                val intent = Intent(this, Conecta4Activity::class.java)
                if (ttsEnabled) {
                    tts?.speak(
                        "Venga! Vamos A Jugar Un Conecta 4.",
                        TextToSpeech.QUEUE_FLUSH,
                        null,
                        "ttsFlexionesId"
                    )
                }
                startActivity(intent)
                return true
            }

            R.id.action_damas -> {
                val intent = Intent(this, DamasActivity::class.java)
                if (ttsEnabled) {
                    tts?.speak(
                        "Venga! Vamos A Jugar Un Ratito A Las Damas.",
                        TextToSpeech.QUEUE_FLUSH,
                        null,
                        "ttsFlexionesId"
                    )
                }
                startActivity(intent)
                return true
            }

            R.id.action_clear_db -> {
                confirmClearDatabase()
                return true
            }

            R.id.action_crear_qr -> {
                if (ttsEnabled) {
                    tts?.speak(
                        "Crea Tu Própio Código Q R, Personalizado; Escribe Lo Que Quieras Y Genera El Código.",
                        TextToSpeech.QUEUE_FLUSH,
                        null,
                        "ttsFlexionesId"
                    )
                }
                val intent = Intent(this, CrearQrActivity::class.java)
                startActivity(intent)
                true
            }

            R.id.action_about -> {
                showAboutDialog()
                return true
            }

            R.id.action_ticker_cripto -> {
                val intent = Intent(this, TickerCriptoActivity::class.java)
                startActivity(intent)
                if (ttsEnabled) {
                    tts?.speak(
                        "Abriendo Ticker De Criptomonedas.",
                        TextToSpeech.QUEUE_FLUSH,
                        null,
                        "ttsFlexionesId"
                    )
                }
                true
            }

            R.id.menu_borrar_tabaco -> {
                borrarDatosTabaco()
                true
            }

            R.id.menu_cifras -> {
                if (ttsEnabled) {
                    tts?.speak(
                        "Haz Que Tu Móvil Se Ejercite Calculando; Con El Juego De Las Cifras. JiJiJi.",
                        TextToSpeech.QUEUE_FLUSH,
                        null,
                        "ttsFlexionesId"
                    )
                }
                startActivity(Intent(this, CifrasActivity::class.java))
                true
            }

            R.id.menu_salvapantallas -> {
                if (ttsEnabled) {
                    tts?.speak(
                        "Abriendo Salvapantallas... Disfrútalo!",
                        TextToSpeech.QUEUE_FLUSH,
                        null,
                        "ttsFlexionesId"
                    )
                }
                startActivity(Intent(this, SalvapantallasActivity::class.java))
                true
            }

            R.id.action_exit -> {
                finishAffinity()
                return true
            }

            R.id.action_factorizar -> {
                val intent = Intent(this, FactorizacionActivity::class.java)
                if (ttsEnabled) {
                    tts?.speak(
                        "Vamos A Factorizar Números.",
                        TextToSpeech.QUEUE_FLUSH,
                        null,
                        "ttsFlexionesId"
                    )
                }
                startActivity(intent)
                true
            }

            R.id.action_primos -> {
                val intent = Intent(this, PrimosActivity::class.java)
                startActivity(intent)
                if (ttsEnabled) {
                    tts?.speak(
                        "Vamos A Calcular Números Primos.",
                        TextToSpeech.QUEUE_FLUSH,
                        null,
                        "ttsFlexionesId"
                    )
                }
                true
            }

            R.id.menu_fumar -> {
                val intent = Intent(this, FumarActivity::class.java)
                startActivity(intent)
                if (ttsEnabled) {
                    tts?.speak(
                        "Estadísticas Del Fin Del Tabaco; En Tu Vida.",
                        TextToSpeech.QUEUE_FLUSH,
                        null,
                        "ttsFlexionesId"
                    )
                }
                true
            }

            R.id.action_reloj -> {
                val intent = Intent(this, RelojActivity::class.java)
                startActivity(intent)
                if (ttsEnabled) {
                    tts?.speak(
                        "Relojete Y Cronómetro.", TextToSpeech.QUEUE_FLUSH, null, "ttsFlexionesId"
                    )
                }
                return true
            }

            R.id.action_m8axchess -> {
                val intent = Intent(this, ChessActivity::class.java)
                if (ttsEnabled) {
                    tts?.speak(
                        "Guay!; Vamos A Jugar Al Ajedrez.",
                        TextToSpeech.QUEUE_FLUSH,
                        null,
                        "ttsFlexionesId"
                    )
                }
                startActivity(intent)
                true
            }

            R.id.rep_carpetas -> {
                val intent = Intent(this, ReproductorActivity::class.java)
                if (ttsEnabled) {
                    tts?.speak(
                        "Guay!; Vamos A Escuchar Música.",
                        TextToSpeech.QUEUE_FLUSH,
                        null,
                        "ttsFlexionesId"
                    )
                }
                startActivity(intent)
                true
            }

            R.id.action_gimweb -> {
                val intent = Intent(this, M8axGimActivity::class.java)
                if (ttsEnabled) {
                    tts?.speak(
                        "Okey; Vamos A Visitar Webs Útiles Para Tu Salud.",
                        TextToSpeech.QUEUE_FLUSH,
                        null,
                        "ttsFlexionesId"
                    )
                }
                startActivity(intent)
                true
            }

            R.id.menu_relojm -> {
                val intent = Intent(this, M8AXRelojes::class.java)
                if (ttsEnabled) {
                    tts?.speak(
                        "Okey; Que Hora Será; Será; En...",
                        TextToSpeech.QUEUE_FLUSH,
                        null,
                        "ttsFlexionesId"
                    )
                }
                startActivity(intent)
                true
            }

            R.id.rec_voz -> {
                val intent = Intent(this, AudioEventosActivity::class.java)
                if (ttsEnabled) {
                    tts?.speak(
                        "Contando Y Analizando Sonido Ambiental...",
                        TextToSpeech.QUEUE_FLUSH,
                        null,
                        "ttsFlexionesId"
                    )
                    Thread.sleep(3500)
                }
                startActivity(intent)
                true
            }

            R.id.action_mapas -> {
                val intent = Intent(this, M8AXMapas::class.java)
                if (ttsEnabled) {
                    tts?.speak(
                        "Okey; Vamos A Darle Caña Al; G P S...",
                        TextToSpeech.QUEUE_FLUSH,
                        null,
                        "ttsFlexionesId"
                    )
                }
                startActivity(intent)
                true
            }

            R.id.action_chatgpt -> {
                val intent = Intent(this, ChatGPTActivity::class.java)
                startActivity(intent)
                if (ttsEnabled) {
                    tts?.speak(
                        "Selecciona La Inteligencia Artificial, Con La Que Quieras Interactuar.",
                        TextToSpeech.QUEUE_FLUSH,
                        null,
                        "ttsFlexionesId"
                    )
                }
                true
            }

            R.id.action_toggle_tts -> {
                ttsEnabled = !ttsEnabled
                item.isChecked = ttsEnabled
                getSharedPreferences("M8AX-Config_TTS", MODE_PRIVATE).edit()
                    .putBoolean("tts_enabled", ttsEnabled).apply()
                val mensaje = if (ttsEnabled) "Voz Activada" else "Voz Desactivada"
                tts?.speak(mensaje, TextToSpeech.QUEUE_FLUSH, null, "ttsPdfId")
                Toast.makeText(this, mensaje, Toast.LENGTH_SHORT).show()
                return true
            }

            R.id.radios_online -> {
                val intent = Intent(this, RadiosOnlineActivity::class.java)
                startActivity(intent)
                if (ttsEnabled) {
                    tts?.speak(
                        "Abriendo Radios Online", TextToSpeech.QUEUE_FLUSH, null, "ttsRadiosId"
                    )
                }
                return true
            }

            R.id.passwords_gen -> {
                val intent = Intent(this, PasswordsActivity::class.java)
                startActivity(intent)
                if (ttsEnabled) {
                    tts?.speak(
                        "Abriendo Generador Y Gestor De Contraseñas.",
                        TextToSpeech.QUEUE_FLUSH,
                        null,
                        "ttsRadiosId"
                    )
                }
                return true
            }

            R.id.action_copiar_db_downloads -> {
                copiarDBADownloads(this, "M8AX-Gimnasio_DB", db)
                if (ttsEnabled) {
                    tts?.speak(
                        "Copia De Base De Datos De Gimnasio, A Carpeta Downloads; Correcta. Reiniciando.",
                        TextToSpeech.QUEUE_FLUSH,
                        null,
                        "ttsFlexionesId"
                    )
                }
                return true
            }

            R.id.action_tetris -> {
                val intent = Intent(this, TetrisActivity::class.java)
                intent.putExtra("ttsEnabled", ttsEnabled)
                startActivity(intent)
                if (ttsEnabled) {
                    tts?.speak(
                        "¡Vamos A Jugar Al Tetris!",
                        TextToSpeech.QUEUE_FLUSH,
                        null,
                        "ttsFlexionesId"
                    )
                }
                return true
            }

            R.id.action_flexiones -> {
                val prefs = getSharedPreferences("M8AX-Flexiones", Context.MODE_PRIVATE)
                val ultimasFlexiones = prefs.getInt("M8AX-Ultimas-Flexiones", -1)
                val fechaUltima = prefs.getString("M8AX-Fecha-UF", null)
                val horaUltima = prefs.getString("M8AX-Hora-UF", null)
                val fraseElegida =
                    if (ultimasFlexiones == -1 || fechaUltima == null || horaUltima == null) {
                        "Parece Que Es Tu Primera Vez En El Contador De Flexiones. ¡Vamos A Estrenarlo Con Fuerza!"
                    } else {
                        val mensajesUltimaSesion = listOf(
                            "El Último Día Que Hiciste Flexiones Fue El ${fechaUltima}, A Las ${horaUltima}, Y Hiciste ${ultimasFlexiones} Flexiones. ¡A Ver Si Te Superas!",
                            "Wow! El ${fechaUltima}, A Las ${horaUltima}, Lograste Hacer ${ultimasFlexiones} Flexiones. ¡Impresionante!",
                            "¡Eres Una Máquina! El Último Registro Fue El ${fechaUltima} A Las ${horaUltima} Con ${ultimasFlexiones} Flexiones.",
                            "Atención Campeón! El ${fechaUltima} A Las ${horaUltima}, Tuviste ${ultimasFlexiones} Flexiones. ¡Hoy Puedes Hacer Más!",
                            "¡Fuerza Bruta! El Último Día Que Sudaste Fue El ${fechaUltima}, A Las ${horaUltima} Con ${ultimasFlexiones} Flexiones.",
                            "¡No Pares! El ${fechaUltima} A Las ${horaUltima} Lograste ${ultimasFlexiones} Flexiones. ¡A Superarte Ahora!",
                            "Atención Guerrero! El Último Registro Fue El ${fechaUltima} A Las ${horaUltima} Con ${ultimasFlexiones} Flexiones. ¡A Por Más!",
                            "¡Mira Eso! El ${fechaUltima} A Las ${horaUltima} Hiciste ${ultimasFlexiones} Flexiones. ¡Imparable!",
                            "¡Qué Poder! El Último Día Que Trabajaste Fue El ${fechaUltima} A Las ${horaUltima}, Con ${ultimasFlexiones} Flexiones. ¡Sigue Así!",
                            "¡Cuidado! El ${fechaUltima}, A Las ${horaUltima}, Hiciste ${ultimasFlexiones} Flexiones. ¡El Récord Está En Juego!"
                        )
                        var frase = mensajesUltimaSesion.random()
                        frase += when {
                            ultimasFlexiones <= 5 -> " Menuda Mierda De Flexiones Me Hiciste..."
                            ultimasFlexiones in 6..10 -> " Hay Que Esforzarse Más, ¡Tú Puedes!"
                            ultimasFlexiones in 11..20 -> " Así Me Gusta, ¡Vas Subiendo!"
                            ultimasFlexiones in 21..30 -> " ¡Eres Una Máquina!"
                            ultimasFlexiones > 30 -> " ¡Eres El Dios De Las Flexiones!"
                            else -> ""
                        }
                        frase
                    }
                val intent = Intent(this, FlexionesActivity::class.java)
                intent.putExtra("ttsEnabled", ttsEnabled)
                intent.putExtra("fraseInicioFlexiones", fraseElegida)
                startActivity(intent)
                if (ttsEnabled) {
                    tts?.speak(
                        fraseElegida, TextToSpeech.QUEUE_FLUSH, null, "ttsEntradaFlexiones"
                    )
                }
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun reiniciarApp(context: Context) {
        Handler(Looper.getMainLooper()).postDelayed({
            val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)
            intent?.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            Runtime.getRuntime().exit(0)
        }, 7000L)
    }

    private fun toast(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
        if (ttsEnabled) {
            tts?.speak(msg, TextToSpeech.QUEUE_FLUSH, null, "ttsID")
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_RESTAURAR_DB && resultCode == RESULT_OK) {
            data?.data?.let { uri ->
                restaurarBaseDatosDesdeDownloads(uri)
            }
        }
    }

    private fun restaurarBaseDatosDesdeDownloads(uri: Uri) {
        try {
            val dbName = "M8AX-Gimnasio_DB"
            val dbFile = getDatabasePath(dbName)
            try {
                db.close()
            } catch (_: Exception) {
            }
            try {
                AppDatabase.closeInstance()
            } catch (_: Exception) {
            }
            File(dbFile.parent, "$dbName-wal").delete()
            File(dbFile.parent, "$dbName-shm").delete()
            contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(dbFile).use { output -> input.copyTo(output) }
            } ?: run {
                toast("No Se Pudo Abrir El Backup Seleccionado")
                if (ttsEnabled) {
                    tts?.speak(
                        "No Se Pudo Abrir El Backup Seleccionado.",
                        TextToSpeech.QUEUE_FLUSH,
                        null,
                        "ttsFlexionesId"
                    )
                }
                return
            }
            toast("Copia Restaurada Desde Backup Seleccionado\nReiniciando...")
            if (ttsEnabled) {
                tts?.speak(
                    "Copia Restaurada Desde Backup Seleccionado; Reiniciando...",
                    TextToSpeech.QUEUE_FLUSH,
                    null,
                    "ttsFlexionesId"
                )
            }
            reiniciarApp(this)
        } catch (e: Exception) {
            e.printStackTrace()
            toast("Error Al Restaurar: ${e.message}")
            if (ttsEnabled) {
                tts?.speak(
                    "Error, Al Restaurar.", TextToSpeech.QUEUE_FLUSH, null, "ttsFlexionesId"
                )
            }
        }
    }

    companion object {
        const val REQUEST_CODE_RESTAURAR_DB = 1234
        fun programarAlarmaDiaria(context: Context, hora: Int, minuto: Int) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                if (!alarmManager.canScheduleExactAlarms()) return
            }
            val intent = Intent(context, GimnasioReminderReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                12345,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val calendar = Calendar.getInstance().apply {
                timeInMillis = System.currentTimeMillis()
                set(Calendar.HOUR_OF_DAY, hora)
                set(Calendar.MINUTE, minuto)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
                if (before(Calendar.getInstance())) add(Calendar.DAY_OF_MONTH, 1)
            }
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pendingIntent
            )
        }
    }

    private fun showAboutDialog() {
        if (ttsEnabled) {
            tts?.stop()
        }
        val calendar = Calendar.getInstance()
        val mes = calendar.get(Calendar.MONTH)
        val dia = calendar.get(Calendar.DAY_OF_MONTH)
        val isNavidad =
            (mes == Calendar.DECEMBER && dia >= 20) || (mes == Calendar.JANUARY && dia <= 6)
        val soundRes = if (isNavidad) {
            listOf(R.raw.m8axdialogo3, R.raw.m8axdialogo4).random()
        } else {
            listOf(R.raw.m8axdialogo1, R.raw.m8axdialogo2).random()
        }
        val aboutPlayer: MediaPlayer = MediaPlayer.create(this, soundRes)
        aboutPlayer.start()
        aboutPlayer.setOnCompletionListener(object : MediaPlayer.OnCompletionListener {
            override fun onCompletion(mp: MediaPlayer) {
                mp.release()
            }
        })
        val currentYear = Calendar.getInstance().get(Calendar.YEAR)
        val formatoCompilacion = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
        val fechaCompilacion = LocalDateTime.parse("10/01/2026 17:45", formatoCompilacion)
        val ahora = LocalDateTime.now()
        val (años, dias, horas, minutos, segundos) = if (ahora.isBefore(fechaCompilacion)) {
            listOf(0L, 0L, 0L, 0L, 0L)
        } else {
            val a = ChronoUnit.YEARS.between(fechaCompilacion, ahora)
            val fechaMasAnios = fechaCompilacion.plusYears(a)
            val d = ChronoUnit.DAYS.between(fechaMasAnios, ahora)
            val h = ChronoUnit.HOURS.between(fechaMasAnios.plusDays(d), ahora)
            val m = ChronoUnit.MINUTES.between(fechaMasAnios.plusDays(d).plusHours(h), ahora)
            val s = ChronoUnit.SECONDS.between(
                fechaMasAnios.plusDays(d).plusHours(h).plusMinutes(m), ahora
            )
            listOf(a, d, h, m, s)
        }
        val tiempoTranscurrido =
            "... Fecha De Compilación - 10/01/2026 17:45 ...\n\n... Tmp. Desde Compilación - ${años}a${dias}d${horas}h${minutos}m${segundos}s ..."
        val prefs = getSharedPreferences("M8AX-Dejar_De_Fumar", Context.MODE_PRIVATE)
        val fechaDejarFumarMillis = prefs.getLong("fechaDejarFumar", -1L)
        var tiempoSinFumarTexto = ""
        if (fechaDejarFumarMillis != -1L) {
            var temp = Instant.ofEpochMilli(fechaDejarFumarMillis).atZone(ZoneId.systemDefault())
                .toLocalDateTime()
            val ahoraLocal = LocalDateTime.now()
            val años = ChronoUnit.YEARS.between(temp, ahoraLocal).also { temp = temp.plusYears(it) }
            val dias = ChronoUnit.DAYS.between(temp, ahoraLocal).also { temp = temp.plusDays(it) }
            val horas =
                ChronoUnit.HOURS.between(temp, ahoraLocal).also { temp = temp.plusHours(it) }
            val minutos =
                ChronoUnit.MINUTES.between(temp, ahoraLocal).also { temp = temp.plusMinutes(it) }
            val segundos = ChronoUnit.SECONDS.between(temp, ahoraLocal)
            tiempoSinFumarTexto =
                "... Tmp. Sin Fumar - ${años}a${dias}d${horas}h${minutos}m${segundos}s ..."
        }
        val textoIzquierda = SpannableString(
            "App Creada Por MarcoS OchoA DieZ - ( M8AX )\n\n" + "Mail - mviiiax.m8ax@gmail.com\n\n" + "Youtube - https://youtube.com/m8ax\n\n" + "Por Muchas Vueltas Que Demos, Siempre Tendremos El Culo Atrás...\n\n\n" + "... Creado En 103h De Programación ...\n\n" + "... Con +/- 21500 Líneas De Código ...\n\n" + "... +/- 880 KB En Texto Plano | TXT | ...\n\n" + "... +/- Libro Drácula De Bram Stoker En Código ...\n\n" + tiempoTranscurrido + "\n\n" + if (tiempoSinFumarTexto.isNotEmpty()) tiempoSinFumarTexto + "\n\n" else ""
        )
        val textoCentro = SpannableString(
            "| AND | OR | NOT | Ax = b | 0 - 1 |\n\n" + "M8AX CORP. $currentYear - ${
                intToRoman(
                    currentYear
                )
            }\n\n"
        )
        textoIzquierda.setSpan(
            android.text.style.StyleSpan(android.graphics.Typeface.BOLD),
            0,
            textoIzquierda.length,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        textoCentro.setSpan(
            android.text.style.StyleSpan(android.graphics.Typeface.BOLD),
            0,
            textoCentro.length,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        val emailStart = textoIzquierda.indexOf("mviiiax.m8ax@gmail.com")
        val emailEnd = emailStart + "mviiiax.m8ax@gmail.com".length
        textoIzquierda.setSpan(object : ClickableSpan() {
            override fun onClick(widget: View) {
                val intent = Intent(Intent.ACTION_SENDTO).apply {
                    data = Uri.parse("mailto:mviiiax.m8ax@gmail.com")
                }
                startActivity(intent)
            }
        }, emailStart, emailEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        val ytStart = textoIzquierda.indexOf("https://youtube.com/m8ax")
        val ytEnd = ytStart + "https://youtube.com/m8ax".length
        textoIzquierda.setSpan(object : ClickableSpan() {
            override fun onClick(widget: View) {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://youtube.com/m8ax"))
                startActivity(intent)
            }
        }, ytStart, ytEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        val mainLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(50, 40, 50, 40)
            gravity = Gravity.START
        }
        val tvLeft = TextView(this).apply {
            text = textoIzquierda
            movementMethod = LinkMovementMethod.getInstance()
            textAlignment = View.TEXT_ALIGNMENT_TEXT_START
        }
        val tvCenter = TextView(this).apply {
            text = textoCentro
            textAlignment = View.TEXT_ALIGNMENT_CENTER
        }
        val frame = FrameLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 400
            )
        }
        val logo = ImageView(this).apply {
            setImageResource(R.drawable.logom8ax)
            adjustViewBounds = true
            scaleType = ImageView.ScaleType.FIT_CENTER
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT
            )
        }
        var touchCount = 0
        val mensajes = listOf(
            "Modo Dios Activado. No Rompas Nada.",
            "No Deberías Estar Haciendo Esto... Pero Bienvenido Al Club.",
            "Has Desbloqueado El Snack Secreto De M 8 A X",
            "01001000 01101111 01101100 01100001... Eso Significa Hola, Por Si No Lo Pillas.",
            "El Futuro, No Está Establecido, No Hay Destino; Solo Existe El Que Nosotros Hacemos...",
            "Que La Fuerza Te Acompañe.",
            "M 8 A X Es El Mejor Programador Del Mundo.",
            "Nivel Oculto Activado. Prepárate Para Compilar Sin Piedad.",
            "Error 404: Huevo De Pascua No Encontrado... Oh Espera, Aquí Está...",
            "Jijijiji! Por Fin Alguien, Descubre Mi Tesoro Oculto...",
            "Cuidado Con Los Loops Infinitos, Pero Hoy Es Tu Día De Suerte.",
            "Has Encontrado La Ruta Secreta De Compilación.",
            "¡Atención! Se Ha Activado El Protocolo Oculto.",
            "Cada Click Cuenta, Pero Hoy Has Ganado.",
            "Los Bits No Mienten, Pero A Veces Se Divierten.",
            "Nivel Épico Alcanzado. Tu Código Brilla.",
            "Error 1337: Programador Legendario Detectado.",
            "Si Puedes Leer Esto, Eres Parte Del Club Secreto.",
            "Compila Sin Miedo, Que El Universo Te Respeta.",
            "Has Desatado La Magia Del Debug."
        )
        logo.setOnClickListener {
            touchCount++
            if (touchCount >= 10) {
                touchCount = 0
                contadorespe = 1
                val player = MediaPlayer.create(this@MainActivity, R.raw.m8axdialogo5)
                player.start()
                player.setOnCompletionListener { mp -> mp.release() }
                val mensaje = mensajes.random()
                tts?.stop()
                tts?.speak(
                    "Huevito De Pascua; " + mensaje, TextToSpeech.QUEUE_FLUSH, null, "easterEggId"
                )
                AlertDialog.Builder(this@MainActivity).setTitle("¡ Huevito De Pascua !")
                    .setMessage("$mensaje\n\nhttps://youtube.com/m8ax")
                    .setPositiveButton("OK", null).show()
            }
        }
        frame.addView(logo)
        mainLayout.addView(tvLeft)
        mainLayout.addView(tvCenter)
        mainLayout.addView(frame)
        val tvVer = TextView(this).apply {
            textAlignment = View.TEXT_ALIGNMENT_CENTER
            gravity = Gravity.CENTER
            textSize = 15f
            setTypeface(null, Typeface.BOLD)
        }
        val githubUrl = "https://github.com/m8ax/M8AXDiarioDeGimnasio"
        val spannableText = SpannableString("\nÚltima Release - v10.03.1977")
        spannableText.setSpan(object : android.text.style.ClickableSpan() {
            override fun onClick(widget: View) {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(githubUrl))
                widget.context.startActivity(intent)
            }

            override fun updateDrawState(ds: android.text.TextPaint) {
                val red = (100..255).random()
                val green = (100..255).random()
                val blue = (100..255).random()
                ds.color = android.graphics.Color.rgb(red, green, blue)
                ds.isUnderlineText = true
            }
        }, 0, spannableText.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        tvVer.text = spannableText
        tvVer.movementMethod = LinkMovementMethod.getInstance()
        val params = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            gravity = Gravity.CENTER
            topMargin = -55
        }
        tvVer.layoutParams = params
        mainLayout.addView(tvVer)
        val handler = Handler(mainLooper)
        val random = Random()
        fun addStar() {
            val star = ImageView(this).apply {
                setImageResource(android.R.drawable.star_big_on)
                alpha = 0.6f + random.nextFloat() * 0.4f
                val size = 10 + random.nextInt(15)
                layoutParams = FrameLayout.LayoutParams(size, size)
                x = random.nextFloat() * frame.width
                y = -size.toFloat()
            }
            frame.addView(star)
            val distance = frame.height + 20
            val duration = 3000L + random.nextInt(2000)
            star.animate().translationYBy(distance.toFloat()).alpha(0f).setDuration(duration)
                .withEndAction { frame.removeView(star) }.start()
        }

        val snowRunnable = object : Runnable {
            override fun run() {
                addStar()
                handler.postDelayed(this, 200)
            }
        }
        handler.post(snowRunnable)
        val handlerr = Handler(Looper.getMainLooper())
        var ttsRunnable: Runnable? = null
        val dialog = AlertDialog.Builder(this).setTitle("Acerca De...").setView(mainLayout)
            .setPositiveButton("Aceptar") { _, _ ->
                handlerr.removeCallbacks(snowRunnable)
                ttsRunnable?.let { handlerr.removeCallbacks(it) }
                tts?.stop()
            }.create()
        dialog.setOnDismissListener {
            contadorespe = 0
            handlerr.removeCallbacks(snowRunnable)
            ttsRunnable?.let { handlerr.removeCallbacks(it) }
            tts?.stop()
        }
        if (ttsEnabled) {
            ttsRunnable = Runnable {
                if (contadorespe == 0) {
                    tts?.stop()
                    tts?.speak(
                        "Espero Que Te Guste Mi Aplicación; Si Quieres Que Te Haga Una; No Dudes En Contactarme. Por Muchas Vueltas Que Demos, Siempre Tendremos El Culo Atrás...",
                        TextToSpeech.QUEUE_FLUSH,
                        null,
                        "ttsPdfId"
                    )
                }
            }
            handlerr.postDelayed(ttsRunnable!!, 5000)
        }
        dialog.show()
    }

    private fun confirmClearDatabase() {
        if (ttsEnabled) {
            tts?.speak(
                "Estás A Punto De Borrar Todos Los Registros De Gimnasio, ¿Estás Seguro?",
                TextToSpeech.QUEUE_FLUSH,
                null,
                "ttsPdfId"
            )
        }
        AlertDialog.Builder(this).setTitle("Borrar Base De Datos")
            .setMessage("¿ Estás Seguro De Que Quieres Borrar Todos Los Registros ?")
            .setPositiveButton("Sí") { _, _ ->
                Thread {
                    db.clearAllTables()
                    runOnUiThread {
                        if (ttsEnabled) {
                            tts?.stop()
                            tts?.speak(
                                "Base De Datos Borrada Correctamente. Reiniciando, Espera Un Momento...",
                                TextToSpeech.QUEUE_FLUSH,
                                null,
                                "ttsPdfId"
                            )
                        }
                        Handler(mainLooper).postDelayed({
                            reiniciarApp(this)
                        }, 100)
                    }
                }.start()
            }.setNegativeButton("No") { _, _ ->
                Toast.makeText(this, "Operación De Borrado Cancelada", Toast.LENGTH_SHORT).show()
                if (ttsEnabled) {
                    tts?.stop()
                    tts?.speak(
                        "Vale, No Voy A Borrar Nada, En Absoluto.",
                        TextToSpeech.QUEUE_FLUSH,
                        null,
                        "ttsNoId"
                    )
                }
            }.show()
    }

    private fun exportJson() {
        try {
            val lista = db.gimnasioDao().getAll()
            if (lista.isEmpty()) {
                if (ttsEnabled) {
                    tts?.speak(
                        "No Hay Datos Para Exportar A Fichero JSON.",
                        TextToSpeech.QUEUE_FLUSH,
                        null,
                        "ttsJsonId"
                    )
                }
                Toast.makeText(this, "No Hay Datos Para Exportar", Toast.LENGTH_SHORT).show()
                return
            }
            val sdfFecha = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            val fechasSoloDia = lista.mapNotNull {
                runCatching {
                    val fechaStr = it.fechaHora.substringBefore(" - ").trim()
                    sdfFecha.parse(fechaStr)
                }.getOrNull()
            }
            val fechaInicio = fechasSoloDia.minOrNull()?.toInstant()?.atZone(ZoneId.systemDefault())
                ?.toLocalDate() ?: LocalDate.now()
            val fechaFin = fechasSoloDia.maxOrNull()?.toInstant()?.atZone(ZoneId.systemDefault())
                ?.toLocalDate() ?: LocalDate.now()
            val diasEntreRegistros = ChronoUnit.DAYS.between(fechaInicio, fechaFin).toInt() + 1
            val listaValidos = lista.filter { it.valor > 0 }
            val totalMinutos = listaValidos.sumOf { it.valor }
            val mediaRealDia =
                if (diasEntreRegistros > 0) totalMinutos.toDouble() / diasEntreRegistros else totalMinutos.toDouble()
            val totalMinRedondeado = mediaRealDia.roundToInt()
            val horassReal = totalMinRedondeado / 60
            val minutossReal = totalMinRedondeado % 60
            val mediaRealFormateada = String.format("%02dh:%02dm", horassReal, minutossReal)
            val listaOrdenada = when (tipoOrdenExport) {
                1 -> lista.sortedByDescending {
                    runCatching { sdfFecha.parse(it.fechaHora.substring(0, 10)) }.getOrNull()
                        ?: Date(0)
                }

                2 -> lista.sortedBy {
                    runCatching { sdfFecha.parse(it.fechaHora.substring(0, 10)) }.getOrNull()
                        ?: Date(0)
                }

                3 -> lista.sortedByDescending { it.valor }
                4 -> lista.sortedByDescending { it.diario.length }
                else -> lista
            }
            val fileName = "M8AX-Diario-Gimnasio_${
                SimpleDateFormat(
                    "yyyyMMdd_HHmmss", Locale.getDefault()
                ).format(Date())
            }.json"
            var uriToOpen: Uri? = null
            val output: OutputStream
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, "application/json")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                }
                uriToOpen =
                    contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                output = contentResolver.openOutputStream(uriToOpen!!)!!
            } else {
                val downloads =
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                val file = File(downloads, fileName)
                output = FileOutputStream(file)
                uriToOpen = Uri.fromFile(file)
            }
            OutputStreamWriter(output, Charsets.UTF_8).use { writer ->
                JsonWriter(writer).use { jsonWriter ->
                    jsonWriter.setIndent("  ")
                    jsonWriter.beginObject()
                    jsonWriter.name("registros").beginArray()
                    listaOrdenada.forEachIndexed { index, item ->
                        val h = item.valor / 60
                        val m = item.valor % 60
                        val formato = String.format("%02dh:%02dm", h, m)
                        val calendar = Calendar.getInstance().apply {
                            time = sdfFecha.parse(item.fechaHora.substringBefore(" - ").trim())
                                ?: Date()
                        }
                        val diaDelAno = calendar.get(Calendar.DAY_OF_YEAR)
                        jsonWriter.beginObject()
                        jsonWriter.name("numero").value(index + 1)
                        jsonWriter.name("numero_romano").value(intToRoman(index + 1))
                        jsonWriter.name("dia_del_ano").value(diaDelAno)
                        jsonWriter.name("fecha_hora").value(item.fechaHora)
                        jsonWriter.name("valorm").value(item.valor)
                        jsonWriter.name("valorhm").value(formato)
                        if (item.diario.isNotEmpty()) {
                            jsonWriter.name("diario").value(item.diario)
                            jsonWriter.name("tiene_diario").value("•✔•")
                        } else {
                            jsonWriter.name("tiene_diario").value("")
                        }
                        jsonWriter.endObject()
                    }
                    jsonWriter.endArray()
                    val totalRegistros = listaValidos.size
                    val mediaGimnasio =
                        if (totalRegistros > 0) listaValidos.map { it.valor }.average() else 0.0
                    val totalMin = mediaGimnasio.roundToInt()
                    val h = totalMin / 60
                    val mm = totalMin % 60
                    val mediaFormateada = String.format("%02dh:%02dm", h, mm)
                    val tiempoFormateado = formatearTiempo(totalMinutos)
                    jsonWriter.name("resumen").beginObject()
                    jsonWriter.name("total_registros_validos").value(totalRegistros)
                    jsonWriter.name("total_registros_romano").value(intToRoman(totalRegistros))
                    jsonWriter.name("total_minutos_ejercicio").value(totalMinutos)
                    jsonWriter.name("total_tiempo_ejercicio_formateado").value(tiempoFormateado)
                    jsonWriter.name("media_ejercicio_minutos")
                        .value(String.format("%.1f", mediaGimnasio))
                    jsonWriter.name("media_formateada").value(mediaFormateada)
                    jsonWriter.name("mensaje_media").value(
                        when {
                            mediaGimnasio < 45 -> "Tu Media Es Baja, Hay Que Hacer Más Ejercicio ¿ No Crees ?."
                            mediaGimnasio < 61 -> "Tu Media De Ejercicio, Está Dentro Del Rango Recomendado, ¡ Bien Hecho Campeón !."
                            mediaGimnasio < 91 -> "Tu Media Es Ligeramente Alta, Con Una Hora Al Día De Ejercicio, Es Suficiente."
                            else -> "Tu Media Es Alta, Tampoco Hace Falta Matarse Haciendo Ejercicio, ¿ No Te Parece ?."
                        }
                    )
                    jsonWriter.name("dias_entre_primer_y_ultimo_registro").value(diasEntreRegistros)
                    jsonWriter.name("dias_entre_primer_y_ultimo_registro_romano")
                        .value(intToRoman(diasEntreRegistros))
                    jsonWriter.name("anos_dias_entre_primer_y_ultimo_registro").value(
                        "${ChronoUnit.YEARS.between(fechaInicio, fechaFin).toInt()} Años Y ${
                            ChronoUnit.DAYS.between(
                                fechaInicio.plusYears(
                                    ChronoUnit.YEARS.between(
                                        fechaInicio, fechaFin
                                    )
                                ), fechaFin
                            ).toInt() + 1
                        } Días."
                    )
                    jsonWriter.name("media_real_diaria").value(String.format("%.1f", mediaRealDia))
                    jsonWriter.name("media_real_formateada").value(mediaRealFormateada)
                    jsonWriter.name("mensaje_media_real").value(
                        when {
                            mediaRealDia < 45 -> "Tu Media Real Es Baja, Hay Que Hacer Más Ejercicio ¿ No Crees ?."
                            mediaRealDia < 61 -> "Tu Media Real De Ejercicio, Está Dentro Del Rango Recomendado, ¡ Bien Hecho Campeón !."
                            mediaRealDia < 91 -> "Tu Media Real Es Ligeramente Alta, Con Una Hora Al Día De Ejercicio, Es Suficiente."
                            else -> "Tu Media Real Es Alta, Tampoco Hace Falta Matarse Haciendo Ejercicio, ¿ No Te Parece ?."
                        }
                    )
                    jsonWriter.endObject()
                    jsonWriter.name("documento_generado").value(
                        SimpleDateFormat(
                            "yyyyMMdd_HHmmss", Locale.getDefault()
                        ).format(Date())
                    )
                    jsonWriter.name("by")
                        .value("M8AX Corp. ${Calendar.getInstance().get(Calendar.YEAR)}")
                    jsonWriter.endObject()
                }
            }
            if (ttsEnabled) {
                tts?.speak(
                    "Archivo JSON Generado Correctamente, En La Carpeta De Descargas.",
                    TextToSpeech.QUEUE_FLUSH,
                    null,
                    "ttsJsonId"
                )
            }
            Toast.makeText(this, "JSON Generado En Descargas", Toast.LENGTH_LONG).show()
            uriToOpen?.let {
                val intent = Intent(Intent.ACTION_VIEW)
                intent.setDataAndType(it, "text/plain")
                intent.flags =
                    Intent.FLAG_ACTIVITY_NO_HISTORY or Intent.FLAG_GRANT_READ_URI_PERMISSION
                startActivity(intent)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Error Al Generar JSON", Toast.LENGTH_SHORT).show()
            if (ttsEnabled) {
                tts?.speak(
                    "Error Al Generar El Fichero JSON.", TextToSpeech.QUEUE_FLUSH, null, "ttsJsonId"
                )
            }
        }
    }

    private fun exportPdf() {
        try {
            val lista = db.gimnasioDao().getAll()
            if (lista.isEmpty()) {
                if (ttsEnabled) {
                    tts?.speak(
                        "No Hay Datos Para Exportar A Fichero, P D F.",
                        TextToSpeech.QUEUE_FLUSH,
                        null,
                        "ttsPdfId"
                    )
                }
                Toast.makeText(this, "No Hay Datos Para Exportar", Toast.LENGTH_SHORT).show()
                return
            }
            val sdfFecha = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            val fechasSoloDia = lista.mapNotNull {
                runCatching {
                    val fechaStr = it.fechaHora.substringBefore(" - ").trim()
                    sdfFecha.parse(fechaStr)
                }.getOrNull()
            }
            val fechaInicio = fechasSoloDia.minOrNull()?.toInstant()?.atZone(ZoneId.systemDefault())
                ?.toLocalDate() ?: LocalDate.now()
            val fechaFin = fechasSoloDia.maxOrNull()?.toInstant()?.atZone(ZoneId.systemDefault())
                ?.toLocalDate() ?: LocalDate.now()
            val diasEntreRegistros = ChronoUnit.DAYS.between(fechaInicio, fechaFin).toInt() + 1
            val listaValidos = lista.filter { it.valor > 0 }
            val totalMinutos = listaValidos.sumOf { it.valor.toInt() }
            val mediaRealDia =
                if (diasEntreRegistros > 0) totalMinutos.toDouble() / diasEntreRegistros else totalMinutos.toDouble()
            val totalMinRedondeado = mediaRealDia.roundToInt()
            val horassReal = totalMinRedondeado / 60
            val minutossReal = totalMinRedondeado % 60
            val mediaRealFormateada = String.format("%02dh:%02dm", horassReal, minutossReal)
            val sdfSoloFecha = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            val listaOrdenada = when (tipoOrdenExport) {
                1 -> lista.sortedByDescending {
                    runCatching { sdfSoloFecha.parse(it.fechaHora.substring(0, 10)) }.getOrNull()
                        ?: Date(0)
                }

                2 -> lista.sortedBy {
                    runCatching { sdfSoloFecha.parse(it.fechaHora.substring(0, 10)) }.getOrNull()
                        ?: Date(0)
                }

                3 -> lista.sortedByDescending { it.valor }
                4 -> lista.sortedByDescending { it.diario.length }
                else -> lista
            }
            val sdfFileName = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
            val fechaParaArchivo = sdfFileName.format(Date())
            val fileName = "M8AX-Diario-Gimnasio_$fechaParaArchivo.PdF"
            val document = Document()
            var outputStream: OutputStream? = null
            var pdfFile: File? = null
            var uriToOpen: Uri? = null
            val sep = LineSeparator().apply {
                lineWidth = 1f
                percentage = 100f
                lineColor = BaseColor.GRAY
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                }
                uriToOpen =
                    contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                outputStream = uriToOpen?.let { contentResolver.openOutputStream(it) }
                if (outputStream == null) {
                    Toast.makeText(this, "Error Al Crear El Archivo PDF", Toast.LENGTH_SHORT).show()
                    return
                }
            } else {
                val downloadsDir =
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                pdfFile = File(downloadsDir, fileName)
                outputStream = FileOutputStream(pdfFile)
                uriToOpen = Uri.fromFile(pdfFile)
            }
            PdfWriter.getInstance(document, outputStream)
            document.open()
            val bfMviiiax = BaseFont.createFont(
                "assets/fonts/mviiiax.ttf", BaseFont.IDENTITY_H, BaseFont.EMBEDDED
            )
            val fontTituloo = Font(bfMviiiax, 14f, Font.BOLD, BaseColor(0, 0, 139))
            document.add(Paragraph("||| Diario De Gimnasio De M8AX |||\n\n", fontTituloo))
            var contando = 0
            listaOrdenada.forEach { itItem ->
                contando += 1
                val fontBold = Font(bfMviiiax, 12f, Font.BOLD)
                val fontValor = Font(
                    bfMviiiax, 12f, Font.BOLD, when {
                        itItem.valor < 45 -> BaseColor(255, 0, 0)
                        itItem.valor in 45..60 -> BaseColor(0, 100, 0)
                        itItem.valor in 61..90 -> BaseColor(255, 140, 0)
                        else -> BaseColor(255, 0, 0)
                    }
                )
                val fontDiario = Font(bfMviiiax, 11f, Font.NORMAL, BaseColor.BLACK)
                val p = Paragraph()
                p.spacingAfter = 6f
                val calendar = Calendar.getInstance().apply {
                    time = sdfFecha.parse(itItem.fechaHora.substringBefore(" - ").trim()) ?: Date()
                }
                val diaDelAno = calendar.get(Calendar.DAY_OF_YEAR)
                val iconoDiario = if (itItem.diario.isNotEmpty()) " - •✔•" else ""
                p.add(
                    Chunk(
                        "\n( $contando - ${intToRoman(contando)} ) - D.A-$diaDelAno - ${itItem.fechaHora}$iconoDiario",
                        fontBold
                    )
                )
                p.add(Chunk(" ----------> ", fontBold))
                val totaMin = itItem.valor
                val hhorass = totaMin / 60
                val mminutoss = totaMin % 60
                val mmediaFormateadaa = String.format("%02dh:%02dm", hhorass, mminutoss)
                p.add(Chunk("${itItem.valor} Minutos - $mmediaFormateadaa.\n\n", fontValor))
                if (itItem.diario.isNotEmpty()) {
                    val pDiario = Paragraph(itItem.diario, fontDiario)
                    pDiario.spacingBefore = 3f
                    pDiario.spacingAfter = 13f
                    p.add(pDiario)
                }
                document.add(p)
            }
            val totalRegistros = listaValidos.size
            val mediaGimnasio =
                if (totalRegistros > 0) listaValidos.map { it.valor }.average() else 0.0
            val totalMin = mediaGimnasio.roundToInt()
            val horass = totalMin / 60
            val minutoss = totalMin % 60
            val mediaFormateadaa = String.format("%02dh:%02dm", horass, minutoss)
            val colorMedia = when {
                mediaGimnasio < 45 -> BaseColor(255, 0, 0)
                mediaGimnasio < 61 -> BaseColor(0, 100, 0)
                mediaGimnasio < 91 -> BaseColor(255, 140, 0)
                else -> BaseColor(255, 0, 0)
            }
            val fontTitulo = Font(bfMviiiax, 14f, Font.BOLD)
            val fontResumen = Font(bfMviiiax, 12f, Font.BOLD, colorMedia)
            val fontMensaje = Font(bfMviiiax, 12f, Font.BOLD, colorMedia)
            val mensajeMedia = when {
                mediaGimnasio < 45 -> "Tu Media Es Baja, Hay Que Hacer Más Ejercicio ¿ No Crees ?.\n"
                mediaGimnasio < 61 -> "Tu Media De Ejercicio, Está Dentro Del Rango Recomendado, ¡ Bien Hecho Campeón !.\n"
                mediaGimnasio < 91 -> "Tu Media Es Ligeramente Alta, Con Una Hora Al Día De Ejercicio, Es Suficiente.\n"
                else -> "Tu Media Es Alta, Tampoco Hace Falta Matarse Haciendo Ejercicio, ¿ No Te Parece ?.\n"
            }
            val tiempoFormateado = formatearTiempo(totalMinutos)
            val resumen = Paragraph()
            resumen.add(Chunk("\n--- RESUMEN FINAL ---\n\n", fontTituloo))
            resumen.add(
                Chunk(
                    "Total De Registros Válidos, ( Más De 0 Minutos Haciendo Ejercicio ): $totalRegistros - ${
                        intToRoman(totalRegistros)
                    }.\n\n" + "Total De Minutos Ejercitándote: $totalMinutos - $tiempoFormateado\n",
                    fontResumen
                )
            )
            resumen.add(
                Chunk(
                    "\nMedia De Minutos Haciendo Ejercicio, ( Solo Apuntados Y Más De 0 Minutos Haciendo Ejercicio ): ${
                        String.format(
                            "%.1f", mediaGimnasio
                        )
                    } Minutos - $mediaFormateadaa.\n\n", fontResumen
                )
            )
            resumen.add(Chunk("$mensajeMedia", fontMensaje))
            val colorMedia2 = when {
                mediaRealDia < 45 -> BaseColor(255, 0, 0)
                mediaRealDia < 61 -> BaseColor(0, 100, 0)
                mediaRealDia < 91 -> BaseColor(255, 140, 0)
                else -> BaseColor(255, 0, 0)
            }
            val fontResumen2 = Font(bfMviiiax, 12f, Font.BOLD, colorMedia2)
            resumen.add(Chunk(sep))
            resumen.add(
                Chunk(
                    "\nDiferencia En Días Entre Primer Y Último Registro En La Base De Datos: $diasEntreRegistros - ${
                        intToRoman(diasEntreRegistros)
                    } Días - ${
                        ChronoUnit.YEARS.between(fechaInicio, fechaFin).toInt()
                    } Años Y ${
                        ChronoUnit.DAYS.between(
                            fechaInicio.plusYears(
                                ChronoUnit.YEARS.between(
                                    fechaInicio, fechaFin
                                )
                            ), fechaFin
                        ).toInt() + 1
                    } Días.\n\n", fontResumen2
                )
            )
            resumen.add(
                Chunk(
                    "Media Real Diaria En Minutos Desde Inicio De Estadísticas: ${
                        String.format(
                            "%.1f", mediaRealDia
                        )
                    } Minutos - $mediaRealFormateada.\n", fontResumen2
                )
            )
            val mensajeMedia2 = when {
                mediaRealDia < 45 -> "\nTu Media Real Es Baja, Hay Que Hacer Más Ejercicio ¿ No Crees ?.\n"
                mediaRealDia < 61 -> "\nTu Media Real De Ejercicio, Está Dentro Del Rango Recomendado, ¡ Bien Hecho Campeón !.\n"
                mediaRealDia < 91 -> "\nTu Media Real Es Ligeramente Alta, Con Una Hora Al Día De Ejercicio, Es Suficiente.\n"
                else -> "\nTu Media Real Es Alta, Tampoco Hace Falta Matarse Haciendo Ejercicio, ¿ No Te Parece ?.\n"
            }
            resumen.add(Chunk("$mensajeMedia2", fontResumen2))
            document.add(Chunk(sep))
            document.add(resumen)
            document.add(Chunk(sep))
            val tablaLogos = PdfPTable(2)
            tablaLogos.widthPercentage = 50f
            tablaLogos.horizontalAlignment = Element.ALIGN_CENTER
            val logo1 = getImageFromDrawable(R.drawable.logoapp)
            val logo2 = getImageFromDrawable(R.drawable.logom8ax)
            logo1.scaleToFit(100f, 100f)
            logo2.scaleToFit(100f, 100f)
            tablaLogos.addCell(PdfPCell(logo1).apply {
                border = 0; horizontalAlignment = Element.ALIGN_CENTER
            })
            tablaLogos.addCell(PdfPCell(logo2).apply {
                border = 0; horizontalAlignment = Element.ALIGN_CENTER
            })
            document.add(tablaLogos)
            document.add(Chunk(sep))
            val link = Anchor("https://youtube.com/m8ax", fontTituloo)
            link.reference = "https://youtube.com/m8ax"
            val piePagina = Paragraph().apply {
                add("\nDocumento Generado El - $fechaParaArchivo\n\n")
                add(link)
                add(
                    "\n\nBy M8AX Corp. ${Calendar.getInstance().get(Calendar.YEAR)} - ${
                        intToRoman(
                            Calendar.getInstance().get(Calendar.YEAR)
                        )
                    }.\n\n"
                )
            }
            piePagina.alignment = Element.ALIGN_CENTER
            document.add(piePagina)
            document.add(Chunk(sep))
            document.close()
            outputStream?.close()
            if (ttsEnabled) {
                tts?.speak(
                    "P D F Generado Correctamente, En La Carpeta De Descargas.",
                    TextToSpeech.QUEUE_FLUSH,
                    null,
                    "ttsPdfId"
                )
            }
            Toast.makeText(this, "PDF Generado En Descargas", Toast.LENGTH_LONG).show()
            uriToOpen?.let {
                val intent = Intent(Intent.ACTION_VIEW)
                intent.setDataAndType(it, "application/pdf")
                intent.flags =
                    Intent.FLAG_ACTIVITY_NO_HISTORY or Intent.FLAG_GRANT_READ_URI_PERMISSION
                startActivity(intent)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Error Al Generar PDF", Toast.LENGTH_SHORT).show()
            if (ttsEnabled) {
                tts?.speak(
                    "Error Al Generar El Fichero P D F.", TextToSpeech.QUEUE_FLUSH, null, "ttsPdfId"
                )
            }
        }
    }

    fun borrarDatosTabaco() {
        if (ttsEnabled) {
            tts?.speak(
                "¿Estás Seguro?", TextToSpeech.QUEUE_FLUSH, null, "ttsConfirmId"
            )
        }
        AlertDialog.Builder(this).setTitle("Confirmación").setMessage("¿ Estás Seguro ?")
            .setPositiveButton("Sí") { _, _ ->
                val prefs = getSharedPreferences("M8AX-Dejar_De_Fumar", Context.MODE_PRIVATE)
                prefs.edit().apply {
                    remove("fechaDejarFumar")
                    remove("precioPaquete")
                    remove("paquetesDia")
                    remove("alquitranMg")
                    remove("nicotinaMg")
                    remove("monoxidoMg")
                    apply()
                }
                Toast.makeText(this, "Configuración De Tabaco Reseteada...", Toast.LENGTH_SHORT)
                    .show()
                if (ttsEnabled) {
                    tts?.speak(
                        "Vale, Borrando Las Configuraciones De Estadísticas De Dejar De Fumar",
                        TextToSpeech.QUEUE_FLUSH,
                        null,
                        "ttsTxtId"
                    )
                }
            }.setNegativeButton("No") { _, _ ->
                if (ttsEnabled) {
                    tts?.speak(
                        "Vale, No Se Ha Borrado Nada", TextToSpeech.QUEUE_FLUSH, null, "ttsTxtId"
                    )
                }
                Toast.makeText(this, "No Se Ha Borrado Nada", Toast.LENGTH_SHORT).show()
            }.show()
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

    private fun exportTxt() {
        try {
            val lista = db.gimnasioDao().getAll()
            if (lista.isEmpty()) {
                if (ttsEnabled) {
                    tts?.speak(
                        "No Hay Datos Para Exportar A Fichero T X T.",
                        TextToSpeech.QUEUE_FLUSH,
                        null,
                        "ttsTxtId"
                    )
                }
                Toast.makeText(this, "No Hay Datos Para Exportar", Toast.LENGTH_SHORT).show()
                return
            }
            val sdfFecha = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            val fechasSoloDia = lista.mapNotNull {
                runCatching {
                    val fechaStr = it.fechaHora.substringBefore(" - ").trim()
                    sdfFecha.parse(fechaStr)
                }.getOrNull()
            }
            val fechaInicio = fechasSoloDia.minOrNull()?.toInstant()?.atZone(ZoneId.systemDefault())
                ?.toLocalDate() ?: LocalDate.now()
            val fechaFin = fechasSoloDia.maxOrNull()?.toInstant()?.atZone(ZoneId.systemDefault())
                ?.toLocalDate() ?: LocalDate.now()
            val diasEntreRegistros = ChronoUnit.DAYS.between(fechaInicio, fechaFin).toInt() + 1
            val listaValidos = lista.filter { it.valor > 0 }
            val totalMinutos = listaValidos.sumOf { it.valor }
            val mediaRealDia =
                if (diasEntreRegistros > 0) totalMinutos.toDouble() / diasEntreRegistros else totalMinutos.toDouble()
            val totalMinRedondeado = mediaRealDia.roundToInt()
            val horassReal = totalMinRedondeado / 60
            val minutossReal = totalMinRedondeado % 60
            val mediaRealFormateada = String.format("%02dh:%02dm", horassReal, minutossReal)
            val sdfSoloFecha = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            val listaOrdenada = when (tipoOrdenExport) {
                1 -> lista.sortedByDescending {
                    runCatching { sdfSoloFecha.parse(it.fechaHora.substring(0, 10)) }.getOrNull()
                        ?: Date(0)
                }

                2 -> lista.sortedBy {
                    runCatching { sdfSoloFecha.parse(it.fechaHora.substring(0, 10)) }.getOrNull()
                        ?: Date(0)
                }

                3 -> lista.sortedByDescending { it.valor }
                4 -> lista.sortedByDescending { it.diario.length }
                else -> lista
            }
            val sdfFileName = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
            val fechaParaArchivo = sdfFileName.format(Date())
            val fileName = "M8AX-Diario-Gimnasio_$fechaParaArchivo.TxT"
            var uriToOpen: Uri? = null
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, "text/plain")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                }
                uriToOpen =
                    contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                uriToOpen?.let { uri ->
                    contentResolver.openOutputStream(uri)?.bufferedWriter(Charsets.UTF_8)
                        ?.use { writer ->
                            writer.write("||| DIARIO DE GIMNASIO DE M8AX |||\n\n\n\n")
                            var contando = 0
                            listaOrdenada.forEach { item ->
                                contando += 1
                                val calendar = Calendar.getInstance().apply {
                                    time =
                                        sdfFecha.parse(item.fechaHora.substringBefore(" - ").trim())
                                            ?: Date()
                                }
                                val diaDelAno = calendar.get(Calendar.DAY_OF_YEAR)
                                writer.write(
                                    "--- REGISTRO ---\n\n( $contando - ${
                                        intToRoman(
                                            contando
                                        )
                                    } ) - D.A-$diaDelAno - ${item.fechaHora}"
                                )
                                if (item.diario.isNotEmpty()) writer.write(" - •✔•")
                                val total = item.valor
                                val h = total / 60
                                val m = total % 60
                                val formato = String.format("%02dh:%02dm", h, m)
                                writer.write(" ----------> ${item.valor} Minutos - $formato\n")
                                if (item.diario.isNotEmpty()) writer.write("\n--- DIARIO ---\n\n${item.diario}\n\n\n")
                                writer.write("\n")
                            }
                            val totalRegistros = listaValidos.size
                            val mediaGimnasio =
                                if (totalRegistros > 0) listaValidos.map { it.valor }
                                    .average() else 0.0
                            val totalMin = mediaGimnasio.roundToInt()
                            val h = totalMin / 60
                            val mm = totalMin % 60
                            val mediaFormateada = String.format("%02dh:%02dm", h, mm)
                            val tiempoFormateado = formatearTiempo(totalMinutos)
                            writer.write("--- RESUMEN FINAL ---\n\n\n\n")
                            writer.write(
                                "Total De Registros Válidos, ( Más De 0 Minutos Haciendo Ejercicio ): $totalRegistros - ${
                                    intToRoman(
                                        totalRegistros
                                    )
                                }.\n\n" + "Total Haciendo Ejercicio: $totalMinutos Minutos - $tiempoFormateado\n\n"
                            )
                            writer.write(
                                "Media De Minutos Haciendo Ejercicio, ( Solo Apuntados Y Más De 0 Minutos Haciendo Ejercicio ): ${
                                    String.format(
                                        "%.1f", mediaGimnasio
                                    )
                                } Minutos - $mediaFormateada.\n\n"
                            )
                            val mensajeMedia = when {
                                mediaGimnasio < 45 -> "Tu Media Es Baja, Hay Que Hacer Más Ejercicio ¿ No Crees ?.\n\n"
                                mediaGimnasio < 61 -> "Tu Media De Ejercicio, Está Dentro Del Rango Recomendado, ¡ Bien Hecho Campeón !.\n\n"
                                mediaGimnasio < 91 -> "Tu Media Es Ligeramente Alta, Con Una Hora Al Día De Ejercicio, Es Suficiente.\n\n"
                                else -> "Tu Media Es Alta, Tampoco Hace Falta Matarse Haciendo Ejercicio, ¿ No Te Parece ?.\n\n"
                            }
                            writer.write(mensajeMedia)
                            writer.write(
                                "\nDías Entre Primer Y Último Registro: $diasEntreRegistros - ${
                                    intToRoman(diasEntreRegistros)
                                } Días - ${
                                    ChronoUnit.YEARS.between(fechaInicio, fechaFin).toInt()
                                } Años Y ${
                                    ChronoUnit.DAYS.between(
                                        fechaInicio.plusYears(
                                            ChronoUnit.YEARS.between(
                                                fechaInicio, fechaFin
                                            )
                                        ), fechaFin
                                    ).toInt() + 1
                                } Días.\n\n"
                            )
                            writer.write(
                                "Media Real Diaria: ${
                                    String.format(
                                        "%.1f", mediaRealDia
                                    )
                                } Minutos - $mediaRealFormateada.\n\n"
                            )
                            val mensajeMedia2 = when {
                                mediaRealDia < 45 -> "Tu Media Real Es Baja, Hay Que Hacer Más Ejercicio ¿ No Crees ?.\n"
                                mediaRealDia < 61 -> "Tu Media Real De Ejercicio, Está Dentro Del Rango Recomendado, ¡ Bien Hecho Campeón !.\n"
                                mediaRealDia < 91 -> "Tu Media Real Es Ligeramente Alta, Con Una Hora Al Día De Ejercicio, Es Suficiente.\n"
                                else -> "Tu Media Real Es Alta, Tampoco Hace Falta Matarse Haciendo Ejercicio, ¿ No Te Parece ?.\n"
                            }
                            writer.write(mensajeMedia2)
                            writer.write("\n\nDocumento Generado El - $fechaParaArchivo\n")
                            writer.write(
                                "\nBy M8AX Corp. ${
                                    Calendar.getInstance().get(Calendar.YEAR)
                                }"
                            )
                        }
                }
            } else {
                val downloads =
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                val file = File(downloads, fileName)
                FileOutputStream(file).bufferedWriter(Charsets.UTF_8).use { writer ->
                    writer.write("||| DIARIO DE GIMNASIO DE M8AX |||\n\n\n")
                    var contando = 0
                    listaOrdenada.forEach { item ->
                        contando += 1
                        val calendar = Calendar.getInstance().apply {
                            time = sdfFecha.parse(item.fechaHora.substringBefore(" - ").trim())
                                ?: Date()
                        }
                        val diaDelAno = calendar.get(Calendar.DAY_OF_YEAR)
                        writer.write("( $contando - ${intToRoman(contando)} ) - D.A-$diaDelAno - ${item.fechaHora}")
                        if (item.diario.isNotEmpty()) writer.write(" - •✔•")
                        val total = item.valor
                        val h = total / 60
                        val m = total % 60
                        val formato = String.format("%02dh:%02dm", h, m)
                        writer.write(" ----------> ${item.valor} Minutos - $formato\n")
                        if (item.diario.isNotEmpty()) writer.write("\nDIARIO: ${item.diario}\n")
                        writer.write("\n")
                    }
                    val totalRegistros = listaValidos.size
                    val mediaGimnasio =
                        if (totalRegistros > 0) listaValidos.map { it.valor }.average() else 0.0
                    val totalMin = mediaGimnasio.roundToInt()
                    val h = totalMin / 60
                    val mm = totalMin % 60
                    val mediaFormateada = String.format("%02dh:%02dm", h, mm)
                    val tiempoFormateado = formatearTiempo(totalMinutos)
                    writer.write("\n--- RESUMEN FINAL ---\n\n\n")
                    writer.write(
                        "Total De Registros Válidos, ( Más De 0 Minutos Haciendo Ejercicio ): $totalRegistros - ${
                            intToRoman(
                                totalRegistros
                            )
                        }.\n\n" + "Total Haciendo Ejercicio: $totalMinutos Minutos - $tiempoFormateado\n\n"
                    )
                    writer.write(
                        "Media De Minutos Haciendo Ejercicio, ( Solo Apuntados Y Más De 0 Minutos Haciendo Ejercicio ): ${
                            String.format(
                                "%.1f", mediaGimnasio
                            )
                        } Minutos - $mediaFormateada.\n\n"
                    )
                    val mensajeMedia = when {
                        mediaGimnasio < 45 -> "Tu Media Es Baja, Hay Que Hacer Más Ejercicio ¿ No Crees ?.\n\n"
                        mediaGimnasio < 61 -> "Tu Media De Ejercicio, Está Dentro Del Rango Recomendado, ¡ Bien Hecho Campeón !.\n\n"
                        mediaGimnasio < 91 -> "Tu Media Es Ligeramente Alta, Con Una Hora Al Día De Ejercicio, Es Suficiente.\n\n"
                        else -> "Tu Media Es Alta, Tampoco Hace Falta Matarse Haciendo Ejercicio, ¿ No Te Parece ?.\n\n"
                    }
                    writer.write(mensajeMedia)
                    writer.write(
                        "\nDías Entre Primer Y Último Registro: $diasEntreRegistros - ${
                            intToRoman(diasEntreRegistros)
                        } Días - ${
                            ChronoUnit.YEARS.between(fechaInicio, fechaFin).toInt()
                        } Años Y ${
                            ChronoUnit.DAYS.between(
                                fechaInicio.plusYears(
                                    ChronoUnit.YEARS.between(
                                        fechaInicio, fechaFin
                                    )
                                ), fechaFin
                            ).toInt() + 1
                        } Días.\n\n"
                    )
                    writer.write(
                        "Media Real Diaria: ${
                            String.format(
                                "%.1f", mediaRealDia
                            )
                        } Minutos - $mediaRealFormateada.\n\n"
                    )
                    val mensajeMedia2 = when {
                        mediaRealDia < 45 -> "Tu Media Real Es Baja, Hay Que Hacer Más Ejercicio ¿ No Crees ?.\n"
                        mediaRealDia < 61 -> "Tu Media Real De Ejercicio, Está Dentro Del Rango Recomendado, ¡ Bien Hecho Campeón !.\n"
                        mediaRealDia < 91 -> "Tu Media Real Es Ligeramente Alta, Con Una Hora Al Día De Ejercicio, Es Suficiente.\n"
                        else -> "Tu Media Real Es Alta, Tampoco Hace Falta Matarse Haciendo Ejercicio, ¿ No Te Parece ?.\n"
                    }
                    writer.write(mensajeMedia2)
                    writer.write("\n\nDocumento Generado El - $fechaParaArchivo\n")
                    writer.write("\nBy M8AX Corp. ${Calendar.getInstance().get(Calendar.YEAR)}")
                }
                uriToOpen = Uri.fromFile(file)
            }
            if (ttsEnabled) {
                tts?.speak(
                    "Archivo T X T Generado Correctamente, En La Carpeta De Descargas.",
                    TextToSpeech.QUEUE_FLUSH,
                    null,
                    "ttsTxtId"
                )
            }
            Toast.makeText(this, "TXT Generado En Descargas", Toast.LENGTH_LONG).show()
            uriToOpen?.let {
                val intent = Intent(Intent.ACTION_VIEW)
                intent.setDataAndType(it, "text/plain")
                intent.flags =
                    Intent.FLAG_ACTIVITY_NO_HISTORY or Intent.FLAG_GRANT_READ_URI_PERMISSION
                startActivity(intent)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Error Al Generar TXT", Toast.LENGTH_SHORT).show()
            if (ttsEnabled) {
                tts?.speak(
                    "Error Al Generar El Fichero T X T.", TextToSpeech.QUEUE_FLUSH, null, "ttsTxtId"
                )
            }
        }
    }

    private var despedidaDicha = false
    override fun onDestroy() {
        if (!despedidaDicha) {
            despedidaDicha = true
            val calendar = Calendar.getInstance()
            val hora = calendar.get(Calendar.HOUR_OF_DAY)
            val minutos = calendar.get(Calendar.MINUTE)
            val horaMinutos = String.format("%02d:%02d", hora, minutos)
            val listaDespedidas = listOf(
                "Son Las ${horaMinutos}. Adiós M 8 A X, Cuídate.",
                "Son Las ${horaMinutos}, Hasta Luego, No Te Olvides De Beber Agua.",
                "Son Las ${horaMinutos}, Ya Nos Veremos Campeón, Hasta Luego.",
                "Son Las ${horaMinutos}, Adiós Campeón, No Te Olvides De Beber Agua.",
                "Son Las ${horaMinutos}, Hasta Luego, Que Tus Frutas Sean Siempre Dulces.",
                "Son Las ${horaMinutos}, Cuida Tus Espaldas Y No Olvides Estirarte.",
                "Son Las ${horaMinutos}, Hasta Luego, Que Tu Café Sea Ligero Y Saludable.",
                "Son Las ${horaMinutos}, Adiós, Recuerda Comer Verde Al Menos Una Vez.",
                "Son Las ${horaMinutos}, Que Tus Caminatas Sean Largas Y Felices.",
                "Son Las ${horaMinutos}, Hasta Luego, Que Tu Siesta Sea Reparadora.",
                "Son Las ${horaMinutos}, Cuídate Y No Olvides Respirar Profundo.",
                "Son Las ${horaMinutos}, Adiós, Que Tu Agua Siempre Esté Fría Y Fresca.",
                "Son Las ${horaMinutos}, Hasta Luego, Que Tus Frutos Secos No Se Quemen.",
                "Son Las ${horaMinutos}, Recuerda Sonreír Y Que Tu Corazón Se Sienta Ligero.",
                "Son Las ${horaMinutos}, Adiós, Que Tus Caminatas Sean Con Ritmo Alegre.",
                "Son Las ${horaMinutos}, Hasta Luego, Que Tus Verduras Nunca Falten.",
                "Son Las ${horaMinutos}, Cuídate, Que El Ejercicio Sea Divertido No Castigo.",
                "Son Las ${horaMinutos}, Adiós, Que Tu Día Empiece Con Buen Pie.",
                "Son Las ${horaMinutos}, Hasta Luego, Que La Fruta Sea Siempre Dulce.",
                "Son Las ${horaMinutos}, Recuerda Tomar Aire Fresco Cada Hora.",
                "Son Las ${horaMinutos}, Adiós, Que Tu Agua Sea Tan Clara Como Tu Sonrisa.",
                "Son Las ${horaMinutos}, Hasta Luego, Que Tu Postura Siempre Sea Correcta.",
                "Son Las ${horaMinutos}, Cuídate, Que Tus Snacks Sean Saludables Y Ricos.",
                "Son Las ${horaMinutos}, Adiós, Que La Energía No Te Falte Hoy.",
                "Son Las ${horaMinutos}, Hasta Luego, Que Tus Caminatas Sean Sin Dolor.",
                "Son Las ${horaMinutos}, Que Tu Desayuno Sea Nutritivo Y Sabroso.",
                "Son Las ${horaMinutos}, Adiós, Que Tus Ejercicios Sean Cortos Pero Efectivos.",
                "Son Las ${horaMinutos}, Hasta Luego, Que Tu Agua Nunca Se Acabe.",
                "Son Las ${horaMinutos}, Cuídate, Que Tus Ojos No Sufran De Pantallas.",
                "Son Las ${horaMinutos}, Adiós, Que Tu Respiración Sea Profunda Y Serena.",
                "Son Las ${horaMinutos}, Hasta Luego, Que Tus Estiramientos Sean Divertidos.",
                "Son Las ${horaMinutos}, Que Tus Comidas Siempre Sean Balanceadas.",
                "Son Las ${horaMinutos}, Adiós, Que Tus Pausas Sean Cortas Pero Reales.",
                "Son Las ${horaMinutos}, Hasta Luego, Que Tus Pasos Sean Alegres Y Firmes.",
                "Son Las ${horaMinutos}, Cuídate, Que Tu Postura Al Sentarte Sea Correcta.",
                "Son Las ${horaMinutos}, Adiós, Que Tus Snacks No Contengan Culpa.",
                "Son Las ${horaMinutos}, Hasta Luego, Que Tu Energía No Se Desgaste Rápido.",
                "Son Las ${horaMinutos}, Que Tus Verduras Siempre Sean Coloridas Y Frescas.",
                "Son Las ${horaMinutos}, Adiós, Que Tu Sonrisa Sea Tan Grande Como Tu Agua.",
                "Son Las ${horaMinutos}, Hasta Luego, Que Tus Ejercicios No Sean Aburridos.",
                "Son Las ${horaMinutos}, Cuídate, Que Tus Frutos Secos Sean Crocantes.",
                "Son Las ${horaMinutos}, Adiós, Que Tu Café Sea Saludable Pero Rico.",
                "Son Las ${horaMinutos}, Hasta Luego, Que Tus Caminatas No Tengan Prisa.",
                "Son Las ${horaMinutos}, Que Tu Postura Sea Siempre Recta Y Firme.",
                "Son Las ${horaMinutos}, Adiós, Que Tus Snacks Sean Más Saludables Que Dulces.",
                "Son Las ${horaMinutos}, Hasta Luego, Que Tus Pausas Te Recarguen.",
                "Son Las ${horaMinutos}, Cuídate, Que Tu Agua Siempre Sea Abundante.",
                "Son Las ${horaMinutos}, Adiós, Que Tus Comidas Sean Deliciosas Y Sanas.",
                "Son Las ${horaMinutos}, Hasta Luego, Que Tu Día Termine Con Energía Positiva.",
                "Son Las ${horaMinutos}, Que Tu Sonrisa Sea Tan Amplia Como Tu Hidratación.",
                "Son Las ${horaMinutos}, Adiós, Que Tus Caminatas Siempre Sean Placenteras.",
                "Son Las ${horaMinutos}, Hasta Luego, Que Tus Frutas Y Verduras Nunca Falten."
            )
            val listaRegistros = db.gimnasioDao().getAll().filter { it.valor > 0 }
            val ultimoRegistro = listaRegistros.lastOrNull()
            var mensaje = listaDespedidas.random()
            ultimoRegistro?.let {
                val tm = "${it.valor}".toInt()
                val hs = tm / 60
                val ms = tm % 60
                val mf = String.format("%02d Horas Y %02d Minutos.", hs, ms)
                mensaje =
                    "Tu Último Registro De Ejercicio Fué De ${it.valor} Minutos, O Si Lo Prefieres: $mf. $mensaje"
            }
            if (ttsEnabled) {
                tts?.speak(mensaje, TextToSpeech.QUEUE_FLUSH, null, "despedidaId")
                Thread.sleep(16000)
            }
        }
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
        tts?.stop()
        tts?.shutdown()
        tts = null
        handler.removeCallbacks(updateRunnable)
        clickRunnable?.let { clickHandler.removeCallbacks(it) }
        clickRunnable = null
        super.onDestroy()
    }

    private var cierreTimer: Timer? = null
    override fun onPause() {
        super.onPause()
        mediaPlayer?.pause()
        cierreTimer = Timer()
        cierreTimer?.schedule(object : TimerTask() {
            override fun run() {
                runOnUiThread {
                    finishAffinity()
                }
            }
        }, CIERRE_DELAY_MS)
    }

    override fun onResume() {
        super.onResume()
        mediaPlayer?.start()
        cierreTimer?.cancel()
        cierreTimer = null
    }

    fun hablar(registro: Gimnasio) {
        val config = getSharedPreferences("M8AX-Config_TTS", MODE_PRIVATE)
        val ttsEnabled = config.getBoolean("tts_enabled", false)
        val totalMinutosw = registro.valor
        val horasw = totalMinutosw / 60
        val minutosw = totalMinutosw % 60
        val textox =
            "Fecha Y Hora Del Registro: ${registro.fechaHora}.\n\n" + "Tiempo De Ejercicio: $horasw Horas Y $minutosw Minutos.\n\nDiario: ${registro.diario}"
        val clipboard =
            getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
        val clip = android.content.ClipData.newPlainText("Registro completo", textox)
        clipboard.setPrimaryClip(clip)
        android.widget.Toast.makeText(
            this, "Registro Copiado Al PortaPapeles", android.widget.Toast.LENGTH_SHORT
        ).show()
        if (ttsEnabled && registro.diario.isNotEmpty()) {
            val totalMinutos = registro.valor
            val horas = totalMinutos / 60
            val minutos = totalMinutos % 60
            val texto =
                "Fecha Y Hora Del Registro; ${registro.fechaHora}. " + "Tiempo De Ejercicio; $horas Horas Y $minutos Minutos. Diario; ${registro.diario}"
            tts?.apply {
                stop()
                speak("", TextToSpeech.QUEUE_FLUSH, null, "ttsFlush")
            }
            tts?.speak(texto, TextToSpeech.QUEUE_FLUSH, null, "ttsDiario")
        }
    }

    fun decir(texto: String) {
        if (!ttsEnabled) return
        tts?.apply {
            stop()
            speak(texto, TextToSpeech.QUEUE_FLUSH, null, "ttsSimple")
        }
    }

    fun obtenerPrecioBTC(): String {
        return try {
            val client = OkHttpClient()
            val precioUsd = client.newCall(
                Request.Builder().url("https://api.binance.com/api/v3/ticker/price?symbol=BTCUSDT")
                    .build()
            ).execute().use { resp ->
                if (!resp.isSuccessful) return "BTC ➜ ??? $ - ??? € - En 24h ???"
                val body = resp.body?.string() ?: return "BTC ➜ ??? $ - ??? € - En 24h ???"
                JSONObject(body).optString("price", "").toDoubleOrNull()
                    ?: return "BTC ➜ ??? $ - ??? € - En 24h ???"
            }
            val precioEur = client.newCall(
                Request.Builder().url("https://api.binance.com/api/v3/ticker/price?symbol=BTCEUR")
                    .build()
            ).execute().use { resp ->
                if (!resp.isSuccessful) return "BTC ➜ $precioUsd $ - ??? € - En 24h ???"
                val body = resp.body?.string() ?: return "BTC ➜ $precioUsd $ - ??? € - En 24h ???"
                JSONObject(body).optString("price", "").toDoubleOrNull()
                    ?: return "BTC ➜ $precioUsd $ - ??? € - En 24h ???"
            }
            val cambio24h = client.newCall(
                Request.Builder().url("https://api.binance.com/api/v3/ticker/24hr?symbol=BTCUSDT")
                    .build()
            ).execute().use { resp ->
                if (!resp.isSuccessful) 0.0 else {
                    val body = resp.body?.string()
                        ?: return "BTC ➜ ${precioUsd.toLong()} $ - ${precioEur.toLong()} € - En 24h → 0.000%"
                    val json = JSONObject(body)
                    json.optString("priceChangePercent", "0").toDoubleOrNull() ?: 0.0
                }
            }
            val porcentaje = String.format(java.util.Locale.US, "%.3f", kotlin.math.abs(cambio24h))
            val flechas = when {
                cambio24h > 0 -> "En 24h ↑ +$porcentaje%"
                cambio24h < 0 -> "En 24h ↓ -$porcentaje%"
                else -> "En 24h → 0.000%"
            }
            val usdStr = precioUsd.toLong().toString()
            val eurStr = precioEur.toLong().toString()
            "BTC ➜ $usdStr $ - $eurStr € - $flechas"
        } catch (_: Exception) {
            "BTC ➜ ??? $ - ??? € - En 24h ???"
        }
    }
}
