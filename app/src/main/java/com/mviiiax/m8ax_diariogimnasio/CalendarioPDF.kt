package com.mviiiax.m8ax_diariogimnasio

import android.content.ContentValues
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.speech.tts.TextToSpeech
import android.util.TypedValue
import android.view.WindowManager
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import org.shredzone.commons.suncalc.MoonIllumination
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.Year
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Calendar
import java.util.Locale
import kotlin.math.roundToInt

class ActivityCalendarioAnual : AppCompatActivity(), TextToSpeech.OnInitListener {
    private lateinit var etYear: EditText
    private lateinit var btnGenerar: Button
    private lateinit var cbDiasEntrenados: CheckBox
    private lateinit var mediaPlayer: MediaPlayer
    private var tts: TextToSpeech? = null
    private var ttsEnabled = false
    private lateinit var etYearA: EditText
    private lateinit var etYearB: EditText
    private lateinit var btnGenerarIntervalo: Button
    private lateinit var tvProgreso: TextView
    private lateinit var cbPorcentajeLuna: CheckBox

    @Volatile
    private var cancelPdfGeneration = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_calendario_pdf)
        etYear = findViewById(R.id.etYear)
        btnGenerar = findViewById(R.id.btnGenerar)
        cbDiasEntrenados = findViewById(R.id.cbDiasEntrenados)
        etYearA = findViewById(R.id.etYearA)
        etYearB = findViewById(R.id.etYearB)
        tvProgreso = findViewById(R.id.tvProgreso)
        cbPorcentajeLuna = findViewById<CheckBox>(R.id.cbLunaVisible)
        btnGenerarIntervalo = findViewById(R.id.btnGenerarIntervalo)
        mediaPlayer = MediaPlayer.create(this, R.raw.m8axsonidofondo)
        mediaPlayer.isLooping = true
        mediaPlayer.start()
        val prefs = getSharedPreferences("M8AX-Config_TTS", MODE_PRIVATE)
        ttsEnabled = prefs.getBoolean("tts_enabled", false)
        tts = TextToSpeech(this, this)
        btnGenerar.setOnClickListener {
            var year = etYear.text.toString().toIntOrNull() ?: 1
            if (year < 1) year = 1
            if (year > 1_000_000) year = 1_000_000
            etYear.setText(year.toString())
            cancelPdfGeneration = false
            if (cbPorcentajeLuna.isChecked) {
                mostrarDialogoLuna { seleccion ->
                    if (seleccion == null) return@mostrarDialogoLuna
                    exportCalendarioPdf(year, seleccion)
                }
            } else {
                exportCalendarioPdf(year, 0)
            }
        }
        btnGenerarIntervalo.setOnClickListener {
            var yearA = etYearA.text.toString().toIntOrNull() ?: 1
            var yearB = etYearB.text.toString().toIntOrNull() ?: 1
            if (yearA == 0) yearA = 1
            if (yearB == 0) yearB = 1
            if (yearA > yearB) {
                val temp = yearA
                yearA = yearB
                yearB = temp
            }
            cancelPdfGeneration = false
            if (cbPorcentajeLuna.isChecked) {
                mostrarDialogoLuna { seleccion ->
                    if (seleccion == null) return@mostrarDialogoLuna
                    val maxIntervalo = if (seleccion == 2) 10000 else 20000
                    if (yearB - yearA + 1 > maxIntervalo) yearB = yearA + maxIntervalo - 1
                    etYearA.setText(yearA.toString())
                    etYearB.setText(yearB.toString())
                    btnGenerarIntervalo.isEnabled = false
                    btnGenerar.isEnabled = false
                    cbDiasEntrenados.isEnabled = false
                    cbPorcentajeLuna.isEnabled = false
                    window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                    Thread {
                        exportCalendarioPdfIntervalo(yearA, yearB, seleccion)
                        runOnUiThread {
                            btnGenerarIntervalo.isEnabled = true
                            btnGenerar.isEnabled = true
                            cbDiasEntrenados.isEnabled = true
                            cbPorcentajeLuna.isEnabled = true
                            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                            tvProgreso.text =
                                "Nota 1 - Si Eliges Los Porcentajes De Luna Visible O Dibujos De Luna, En Las Celdas De Los Días De Cada Mes, Los Porcentajes O Dibujos Serán Calculados El Día En Concreto A Las 12:00:00. Intervalos Grandes Entre Años Puede Tardar Un Poco En Grabarlos En Fichero. Restricciones De Android Al Grabar Desde App Externa Al Sistema. Ser Paciente Es Tu Virtud.\n\nNota 2 - El Intervalo Máximo De Años Que Se Puede Generar Depende Del Tipo De Luna Seleccionado; Si Se Eligen Dibujos De Luna Al Pulsar El Botón De Crear, El Intervalo Se Limita A 10000 Años. Para Todas Las Demás Opciones, Incluyendo Porcentaje De Luna Visible Y Días Entrenados, El Intervalo Máximo Permitido Es De 20000 Años. Si Pones Un Rango Mayor Del Permitido El Sistema Ajustará Automáticamente El Año Final Para Que No Se Supere El Límite Según La Opción Elegida, Garantizando Que La Generación Del PDF No Cause Fallos Ni Bloqueos.\n\nNota 3 - 10000 Años Con Dibujos De Luna Ocupan Aproximadamente 750 MB. 20000 Años Con Cualquier Otra Opción, Ocupan Aproximadamente 70 MB.\n\n... By M8AX ..."
                        }
                    }.start()
                }
            } else {
                val maxIntervalo = 20000
                if (yearB - yearA + 1 > maxIntervalo) yearB = yearA + maxIntervalo - 1
                etYearA.setText(yearA.toString())
                etYearB.setText(yearB.toString())
                btnGenerarIntervalo.isEnabled = false
                btnGenerar.isEnabled = false
                cbDiasEntrenados.isEnabled = false
                cbPorcentajeLuna.isEnabled = false
                Thread {
                    exportCalendarioPdfIntervalo(yearA, yearB, 0)
                    runOnUiThread {
                        btnGenerarIntervalo.isEnabled = true
                        btnGenerar.isEnabled = true
                        cbDiasEntrenados.isEnabled = true
                        cbPorcentajeLuna.isEnabled = true
                        tvProgreso.text =
                            "Nota 1 - Si Eliges Los Porcentajes De Luna Visible O Dibujos De Luna, En Las Celdas De Los Días De Cada Mes, Los Porcentajes O Dibujos Serán Calculados El Día En Concreto A Las 12:00:00. Intervalos Grandes Entre Años Puede Tardar Un Poco En Grabarlos En Fichero. Restricciones De Android Al Grabar Desde App Externa Al Sistema. Ser Paciente Es Tu Virtud.\n\nNota 2 - El Intervalo Máximo De Años Que Se Puede Generar Depende Del Tipo De Luna Seleccionado; Si Se Eligen Dibujos De Luna Al Pulsar El Botón De Crear, El Intervalo Se Limita A 10000 Años. Para Todas Las Demás Opciones, Incluyendo Porcentaje De Luna Visible Y Días Entrenados, El Intervalo Máximo Permitido Es De 20000 Años. Si Pones Un Rango Mayor Del Permitido El Sistema Ajustará Automáticamente El Año Final Para Que No Se Supere El Límite Según La Opción Elegida, Garantizando Que La Generación Del PDF No Cause Fallos Ni Bloqueos.\n\nNota 3 - 10000 Años Con Dibujos De Luna Ocupan Aproximadamente 750 MB. 20000 Años Con Cualquier Otra Opción, Ocupan Aproximadamente 70 MB.\n\n... By M8AX ..."
                    }
                }.start()
            }
        }
    }

    private fun mostrarDialogoLuna(callback: (Int?) -> Unit) {
        val opciones = arrayOf(
            "% De Luna Visible En Lugar De Los Días", "Dibujo De Luna Con Los Días Incluídos"
        )
        Toast.makeText(
            this, "Selecciona Modo En El Que La Luna Saldrá En El Calendario", Toast.LENGTH_LONG
        ).show()
        if (ttsEnabled) {
            tts?.speak(
                "Selecciona Modo En El Que La Luna Saldrá En El Calendario.",
                TextToSpeech.QUEUE_FLUSH,
                null,
                "ttsPdfId"
            )
        }
        val builder = androidx.appcompat.app.AlertDialog.Builder(this)
        builder.setTitle("Selecciona Modo De Luna")
        builder.setItems(opciones) { _, which ->
            callback(if (which == 0) 1 else 2)
        }
        builder.setCancelable(true)
        builder.setOnCancelListener {
            if (ttsEnabled) {
                tts?.speak(
                    "Vale, Pues Nada.", TextToSpeech.QUEUE_FLUSH, null, "ttsCancelLuna"
                )
            }
            callback(null)
        }
        builder.show()
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts?.setLanguage(tts?.defaultLanguage ?: Locale.getDefault())
            tts?.setSpeechRate(0.9f)
        }
    }

    fun formatEta(seconds: Long): String {
        val h = seconds / 3600
        val m = (seconds % 3600) / 60
        val s = seconds % 60
        return "%02d:%02d:%02d".format(h, m, s)
    }

    private fun exportCalendarioPdf(year: Int, tipoLuna: Int? = 0) {
        try {
            val pdf = PdfDocument()
            val paint = Paint().apply { textSize = 10f }
            val paintRed = Paint().apply { textSize = 10f; color = Color.RED }
            val paintVerde = Paint().apply {
                textSize = 10f; color = Color.rgb(0, 150, 0); isFakeBoldText = true
            }
            val paintBold = Paint().apply { isFakeBoldText = true; textSize = 14f }
            val pageInfo = PdfDocument.PageInfo.Builder(842, 595, 1).create()
            var page = pdf.startPage(pageInfo)
            var canvas = page.canvas
            var y = 40
            val anioRoman = if (year in 1..1_000_000) intToRoman(year) else ""
            val extras = mutableListOf<String>()
            if (cbDiasEntrenados.isChecked) extras.add("GIMNASIO")
            if (cbPorcentajeLuna.isChecked) {
                if (tipoLuna == 1) extras.add("% LUNA")
                if (tipoLuna == 2) extras.add("LUNA DIBUJO")
            }
            val extrasTexto = if (extras.isNotEmpty()) {
                "     |     " + extras.joinToString(" Y ")
            } else {
                ""
            }
            val baseTitulo =
                if (anioRoman.isNotEmpty()) "CALENDARIO ANUAL DEL AÑO ➤ ( $year - $anioRoman )" else "CALENDARIO ANUAL DEL AÑO ( $year )"
            val titulo = baseTitulo + extrasTexto
            val anchoTitulo = paintBold.measureText(titulo)
            canvas.drawText(titulo, (pageInfo.pageWidth - anchoTitulo) / 2f, y.toFloat(), paintBold)
            y += 30
            val (entrenados, minutosTotalesAno) = if (cbDiasEntrenados.isChecked) obtenerDiasEntrenados(
                year
            ) else emptyMap<LocalDate, Int>() to 0
            val meses = (1..12).map { YearMonth.of(year, it) }
            var filaMes = 0
            val anchoMes = 160f
            val espacioEntreMeses = 20f
            val totalAnchoFila = 4 * anchoMes + 3 * espacioEntreMeses
            val xStartBase = (pageInfo.pageWidth - totalAnchoFila) / 2f
            val paintLuna = Paint().apply { isAntiAlias = true; style = Paint.Style.FILL }
            while (filaMes < 12) {
                val yStartFila = y
                var maxAlturaMesFila = 0
                for (i in 0..3) {
                    if (filaMes + i >= 12) break
                    val ym = meses[filaMes + i]
                    var yMes = yStartFila
                    val xPos = xStartBase + i * (anchoMes + espacioEntreMeses)
                    val mesNombre =
                        ym.month.getDisplayName(TextStyle.FULL, Locale("es", "ES")).uppercase()
                    val textoMes = "$mesNombre - $year"
                    canvas.drawText(textoMes, xPos, yMes.toFloat(), paintBold)
                    yMes += 15
                    val diasSemana = listOf("Lun", "Mar", "Mié", "Jue", "Vie", "Sáb", "Dom")
                    var xDia = xPos
                    diasSemana.forEach { dia ->
                        val p = if (dia == "Sáb" || dia == "Dom") paintRed else paint
                        canvas.drawText(dia, xDia, yMes.toFloat(), p)
                        xDia += 20
                    }
                    yMes += 15
                    var xDiaNum = xPos
                    val start = ym.atDay(1)
                    for (j in 1 until start.dayOfWeek.value) xDiaNum += 20
                    for (day in 1..ym.lengthOfMonth()) {
                        val date = LocalDate.of(year, ym.month.value, day)
                        val p = when {
                            cbDiasEntrenados.isChecked && entrenados.contains(date) -> paintVerde
                            date.dayOfWeek == DayOfWeek.SATURDAY || date.dayOfWeek == DayOfWeek.SUNDAY -> paintRed
                            else -> paint
                        }
                        if (cbPorcentajeLuna.isChecked && tipoLuna == 1) {
                            val porcentaje = getPorcentajeLuna(Calendar.getInstance().apply {
                                set(year, ym.month.value - 1, day)
                            })
                            canvas.drawText("$porcentaje", xDiaNum, yMes.toFloat(), p)
                        } else if (tipoLuna == 2) {
                            val r = 6f
                            val cx = xDiaNum + r
                            val cy = yMes - 6f
                            val moon = MoonIllumination.compute()
                                .on(date.atTime(12, 0).atZone(ZoneId.systemDefault())).execute()
                            val f = moon.fraction.toFloat()
                            paintLuna.color = Color.LTGRAY
                            canvas.drawCircle(cx, cy, r, paintLuna)
                            paintLuna.color = Color.BLACK
                            val clip = if (moon.phase < 0.5f) r * (1f - f) else r * f
                            canvas.save()
                            if (moon.phase < 0.5f) canvas.clipRect(
                                cx - r, cy - r, cx - r + clip * 2f, cy + r
                            )
                            else canvas.clipRect(cx - r + clip * 2f, cy - r, cx + r, cy + r)
                            canvas.drawCircle(cx, cy, r, paintLuna)
                            canvas.restore()
                            if (cbDiasEntrenados.isChecked && entrenados.contains(date)) {
                                val colorDia = entrenados[date]
                                if (colorDia != null) {
                                    paintLuna.color = colorDia
                                    canvas.drawCircle(cx, cy, 2f, paintLuna)
                                }
                            }
                            val dayPaint = Paint().apply {
                                color =
                                    if (date.dayOfWeek == DayOfWeek.SATURDAY || date.dayOfWeek == DayOfWeek.SUNDAY) Color.RED
                                    else Color.BLACK
                                textSize = 3f
                                textAlign = Paint.Align.CENTER
                                isFakeBoldText = true
                            }
                            canvas.drawText(day.toString(), cx, cy + r + 2.6f, dayPaint)
                        } else {
                            canvas.drawText(day.toString(), xDiaNum, yMes.toFloat(), p)
                        }
                        xDiaNum += 20
                        if (date.dayOfWeek == DayOfWeek.SUNDAY) {
                            xDiaNum = xPos; yMes += 15
                        }
                    }
                    if (yMes > maxAlturaMesFila) maxAlturaMesFila = yMes
                }
                y = maxAlturaMesFila + 30
                if (y > 540) {
                    pdf.finishPage(page)
                    page = pdf.startPage(pageInfo)
                    canvas = page.canvas
                    y = 40
                }
                filaMes += 4
            }
            if (cbDiasEntrenados.isChecked && entrenados.isNotEmpty()) {
                val totalDiasEntrenados = entrenados.size
                val mediaEntrenadosMin =
                    (minutosTotalesAno.toDouble() / totalDiasEntrenados).roundToInt()
                val totalDiasAno = if (Year.of(year).isLeap) 366 else 365
                val mediaRealMin = (minutosTotalesAno.toDouble() / totalDiasAno).roundToInt()
                val eh = mediaEntrenadosMin / 60
                val em = mediaEntrenadosMin % 60
                val rh = mediaRealMin / 60
                val rm = mediaRealMin % 60
                val detalleReal = if (rh == 0 && rm == 0 && minutosTotalesAno > 0) {
                    val segundosDia = (minutosTotalesAno * 60) / totalDiasAno
                    " | Detalle ⇾ ${segundosDia}s / Día"
                } else {
                    ""
                }
                val textoDias =
                    "Días Que Has Hecho Ejercicio Este Año $year ⇾ $totalDiasEntrenados Días | " + "Media De Sesiones ⇾ $mediaEntrenadosMin Min - ( ${eh}h ${em}m ) | " + "Media Real ⇾ $mediaRealMin Min - ( ${rh}h ${rm}m )$detalleReal"
                val paintDias = Paint().apply {
                    color = Color.rgb(0, 0, 0)
                    textSize = 11f
                    isFakeBoldText = true
                }
                val xDias = (pageInfo.pageWidth - paintDias.measureText(textoDias)) / 2f
                val yDias = pageInfo.pageHeight - 35f
                canvas.drawText(textoDias, xDias, yDias, paintDias)
            }
            val paintAzul =
                Paint().apply { color = Color.BLUE; textSize = 10f; isFakeBoldText = true }
            val paintRojo =
                Paint().apply { color = Color.RED; textSize = 10f; isFakeBoldText = true }
            val texto1 = "Calendario Creado Por M8AX - "
            val texto2 = "https://youtube.com/m8ax"
            val x =
                (pageInfo.pageWidth - (paintAzul.measureText(texto1) + paintRojo.measureText(texto2))) / 2f
            val yFooter = pageInfo.pageHeight - 20f
            canvas.drawText(texto1, x, yFooter, paintAzul)
            canvas.drawText(texto2, x + paintAzul.measureText(texto1), yFooter, paintRojo)
            val logos = listOf("logom8ax", "logom8ax3", "logom8ax4", "logom8ax5")
            val logoName = logos.random()
            val logoId = resources.getIdentifier(logoName, "drawable", packageName)
            if (logoId != 0) {
                val bitmap = android.graphics.BitmapFactory.decodeResource(resources, logoId)
                val logoWidth = 100
                val logoHeight = 100
                val xLogo = (pageInfo.pageWidth - logoWidth) / 2f
                val yLogo = pageInfo.pageHeight - logoHeight - 47f
                val matrix = android.graphics.Matrix()
                matrix.postScale(
                    logoWidth.toFloat() / bitmap.width, logoHeight.toFloat() / bitmap.height
                )
                matrix.postTranslate(xLogo, yLogo)
                val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
                canvas.drawBitmap(bitmap, matrix, paint)
                bitmap.recycle()
            }
            pdf.finishPage(page)
            val partes = mutableListOf<String>()
            if (cbDiasEntrenados.isChecked) partes.add("Gimnasio")
            if (cbPorcentajeLuna.isChecked) {
                partes.add(if (tipoLuna == 1) "Pctje_Luna" else "Luna_Dibujo")
            }
            val sufijo = if (partes.isEmpty()) "Normal" else partes.joinToString("-")
            val fileName = "M8AX - Calendario-$year-$sufijo.PdF"
            val outputStream: OutputStream
            val uriToOpen: android.net.Uri
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                val contentValues = android.content.ContentValues().apply {
                    put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(android.provider.MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
                    put(
                        android.provider.MediaStore.MediaColumns.RELATIVE_PATH,
                        android.os.Environment.DIRECTORY_DOWNLOADS
                    )
                }
                val resolver = contentResolver
                uriToOpen = resolver.insert(
                    android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues
                )!!
                outputStream = resolver.openOutputStream(uriToOpen)!!
            } else {
                val downloadsDir =
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                val file = File(downloadsDir, fileName)
                outputStream = FileOutputStream(file)
                uriToOpen = android.net.Uri.fromFile(file)
            }
            pdf.writeTo(outputStream)
            pdf.close()
            outputStream.close()
            Toast.makeText(this, "PDF Generado En Descargas", Toast.LENGTH_LONG).show()
            if (ttsEnabled) tts?.speak(
                "PDF Generado Correctamente En Descargas",
                TextToSpeech.QUEUE_FLUSH,
                null,
                "ttsPdfId"
            )
            val intent = Intent(Intent.ACTION_VIEW)
            intent.setDataAndType(uriToOpen, "application/pdf")
            intent.flags = Intent.FLAG_ACTIVITY_NO_HISTORY or Intent.FLAG_GRANT_READ_URI_PERMISSION
            startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
            val year = etYear.text.toString().toIntOrNull() ?: 1
            if (year == 0) {
                val mensaje = "Error: Año 0 No Válido Para Cálculo Lunar"
                Toast.makeText(this, mensaje, Toast.LENGTH_LONG).show()
                if (ttsEnabled) tts?.speak(mensaje, TextToSpeech.QUEUE_FLUSH, null, "ttsPdfId")
            } else {
                val mensaje = "Error Al Generar PDF"
                Toast.makeText(this, mensaje, Toast.LENGTH_SHORT).show()
                if (ttsEnabled) tts?.speak(
                    "Error Al Generar El Fichero PDF", TextToSpeech.QUEUE_FLUSH, null, "ttsPdfId"
                )
            }
        }
    }

    private fun exportCalendarioPdfIntervalo(yearA: Int, yearB: Int, tipoLuna: Int?) {
        val startYear = minOf(yearA, yearB)
        val endYear = maxOf(yearA, yearB)
        val blockSize = 20
        val startMillis = System.currentTimeMillis()
        var calendariosGenerados = 0
        val totalCalendarios = (endYear - startYear) + 1
        runOnUiThread { tvProgreso.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20f) }
        try {
            val pdf = PdfDocument()
            val paint = Paint().apply { textSize = 10f }
            val paintRed =
                Paint().apply { textSize = 10f; color = Color.RED; isFakeBoldText = true }
            val paintVerde = Paint().apply {
                textSize = 10f; color = Color.rgb(0, 150, 0); isFakeBoldText = true
            }
            val paintBold = Paint().apply { isFakeBoldText = true; textSize = 14f }
            val paintLuna = Paint().apply { isAntiAlias = true; style = Paint.Style.FILL }
            val pageInfo = PdfDocument.PageInfo.Builder(842, 595, 1).create()
            var year = startYear
            while (year <= endYear) {
                if (cancelPdfGeneration) return
                val blockEnd = minOf(year + blockSize - 1, endYear)
                for (y in year..blockEnd) {
                    if (cancelPdfGeneration) return
                    if ((y - startYear + 1) % 50 == 0) Thread.yield()
                    val elapsedSeconds = (System.currentTimeMillis() - startMillis) / 1000.0
                    val cps = if (elapsedSeconds > 0) calendariosGenerados / elapsedSeconds else 0.0
                    val porcentaje = (calendariosGenerados * 100.0) / totalCalendarios
                    val restantes = totalCalendarios - calendariosGenerados
                    val etaSegundos = if (cps > 0) (restantes / cps).toLong() else 0L
                    if (elapsedSeconds > 0) {
                        runOnUiThread {
                            tvProgreso.text =
                                "\n\n\nGenerando Calendarios\nAño En Curso - $y\nProgreso - ${
                                    "%.2f".format(
                                        porcentaje
                                    )
                                }%\nVelocidad - ${"%.2f".format(cps)} Cal/s\nCalendarios Restantes - $restantes\nETA - ${
                                    formatEta(
                                        etaSegundos
                                    )
                                }\nTiempo Transcurrido - ${formatEta(elapsedSeconds.toLong())}"
                            if (ttsEnabled && elapsedSeconds.toInt() % 25 == 0 && elapsedSeconds > 10) {
                                val MinutosRestantes = (etaSegundos / 60).toInt()
                                val TextoMinutos =
                                    if (MinutosRestantes < 1) "Queda Menos De Un Minuto." else "Quedan Unos $MinutosRestantes Minutos."
                                tts?.speak(
                                    "Progreso Al ${"%.2f".format(porcentaje)} Por Ciento; Velocidad ${
                                        "%.2f".format(
                                            cps
                                        )
                                    } Calendarios Por Segundo; $TextoMinutos",
                                    TextToSpeech.QUEUE_FLUSH,
                                    null,
                                    "ttsPdfId"
                                )
                            }
                        }
                    }
                    var page = pdf.startPage(pageInfo)
                    var canvas = page.canvas
                    var yPos = 40
                    val anioRoman = if (y in 1..1_000_000) intToRoman(y) else ""
                    val extras = mutableListOf<String>()
                    if (cbDiasEntrenados.isChecked) extras.add("GIMNASIO")
                    if (cbPorcentajeLuna.isChecked) {
                        if (tipoLuna == 1) extras.add("% LUNA")
                        if (tipoLuna == 2) extras.add("LUNA DIBUJO")
                    }
                    val extrasTexto =
                        if (extras.isNotEmpty()) "     |     " + extras.joinToString(" Y ") else ""
                    val baseTitulo =
                        if (anioRoman.isNotEmpty()) "CALENDARIO ANUAL DEL AÑO ➤ ( $y - $anioRoman )"
                        else "CALENDARIO ANUAL DEL AÑO ( $y )"
                    val titulo = baseTitulo + extrasTexto
                    val anchoTitulo = paintBold.measureText(titulo)
                    canvas.drawText(
                        titulo, (pageInfo.pageWidth - anchoTitulo) / 2f, yPos.toFloat(), paintBold
                    )
                    yPos += 30
                    val (entrenados, minutosTotalesAno) = if (cbDiasEntrenados.isChecked) obtenerDiasEntrenados(
                        y
                    ) else emptyMap<LocalDate, Int>() to 0
                    val meses = (1..12).map { YearMonth.of(y, it) }
                    var filaMes = 0
                    val anchoMes = 160f
                    val espacioEntreMeses = 20f
                    val totalAnchoFila = 4 * anchoMes + 3 * espacioEntreMeses
                    val xStartBase = (pageInfo.pageWidth - totalAnchoFila) / 2f
                    while (filaMes < 12) {
                        val yStartFila = yPos
                        var maxAlturaMesFila = 0
                        for (i in 0..3) {
                            if (filaMes + i >= 12) break
                            val ym = meses[filaMes + i]
                            var yMes = yStartFila
                            val xPos = xStartBase + i * (anchoMes + espacioEntreMeses)
                            val mesNombre =
                                ym.month.getDisplayName(TextStyle.FULL, Locale("es", "ES"))
                                    .uppercase()
                            canvas.drawText("$mesNombre - $y", xPos, yMes.toFloat(), paintBold)
                            yMes += 15
                            val diasSemana = listOf("Lun", "Mar", "Mié", "Jue", "Vie", "Sáb", "Dom")
                            var xDia = xPos
                            diasSemana.forEach {
                                canvas.drawText(
                                    it,
                                    xDia,
                                    yMes.toFloat(),
                                    if (it == "Sáb" || it == "Dom") paintRed else paint
                                )
                                xDia += 20
                            }
                            yMes += 15
                            var xDiaNum = xPos
                            val start = ym.atDay(1)
                            for (j in 1 until start.dayOfWeek.value) xDiaNum += 20
                            for (day in 1..ym.lengthOfMonth()) {
                                if (cancelPdfGeneration) return
                                val date = LocalDate.of(y, ym.month.value, day)
                                val p = when {
                                    cbDiasEntrenados.isChecked && entrenados.contains(date) -> paintVerde
                                    date.dayOfWeek == DayOfWeek.SATURDAY || date.dayOfWeek == DayOfWeek.SUNDAY -> paintRed
                                    else -> paint
                                }
                                if (cbPorcentajeLuna.isChecked && tipoLuna == 1) {
                                    val pct = getPorcentajeLuna(Calendar.getInstance().apply {
                                        set(y, ym.month.value - 1, day)
                                    })
                                    canvas.drawText("$pct", xDiaNum, yMes.toFloat(), p)
                                } else if (tipoLuna == 2) {
                                    val r = 6f
                                    val cx = xDiaNum + r
                                    val cy = yMes - 6f
                                    val moon = MoonIllumination.compute()
                                        .on(date.atTime(12, 0).atZone(ZoneId.systemDefault()))
                                        .execute()
                                    val f = moon.fraction.toFloat()
                                    paintLuna.color = Color.LTGRAY
                                    canvas.drawCircle(cx, cy, r, paintLuna)
                                    paintLuna.color = Color.BLACK
                                    val clip = if (moon.phase < 0.5f) r * (1f - f) else r * f
                                    canvas.save()
                                    if (moon.phase < 0.5f) canvas.clipRect(
                                        cx - r, cy - r, cx - r + clip * 2f, cy + r
                                    )
                                    else canvas.clipRect(cx - r + clip * 2f, cy - r, cx + r, cy + r)
                                    canvas.drawCircle(cx, cy, r, paintLuna)
                                    canvas.restore()
                                    if (cbDiasEntrenados.isChecked && entrenados.contains(date)) {
                                        val colorDia = entrenados[date]
                                        if (colorDia != null) {
                                            paintLuna.color = colorDia
                                            canvas.drawCircle(cx, cy, 2f, paintLuna)
                                        }
                                    }
                                    val dayPaint = Paint().apply {
                                        color =
                                            if (date.dayOfWeek == DayOfWeek.SATURDAY || date.dayOfWeek == DayOfWeek.SUNDAY) Color.RED
                                            else Color.BLACK
                                        textSize = 3f
                                        textAlign = Paint.Align.CENTER
                                        isFakeBoldText = true
                                    }
                                    canvas.drawText(day.toString(), cx, cy + r + 2.6f, dayPaint)
                                } else {
                                    canvas.drawText(day.toString(), xDiaNum, yMes.toFloat(), p)
                                }
                                xDiaNum += 20
                                if (date.dayOfWeek == DayOfWeek.SUNDAY) {
                                    xDiaNum = xPos; yMes += 15
                                }
                            }
                            if (yMes > maxAlturaMesFila) maxAlturaMesFila = yMes
                        }
                        yPos = maxAlturaMesFila + 30
                        if (yPos > 540) {
                            pdf.finishPage(page)
                            page = pdf.startPage(pageInfo)
                            canvas = page.canvas
                            yPos = 40
                        }
                        filaMes += 4
                    }
                    if (cbDiasEntrenados.isChecked && entrenados.isNotEmpty()) {
                        val totalDiasEntrenados = entrenados.size
                        val mediaEntrenadosMin =
                            (minutosTotalesAno.toDouble() / totalDiasEntrenados).roundToInt()
                        val totalDiasAno = if (Year.of(y).isLeap) 366 else 365
                        val mediaRealMin =
                            (minutosTotalesAno.toDouble() / totalDiasAno).roundToInt()
                        val eh = mediaEntrenadosMin / 60
                        val em = mediaEntrenadosMin % 60
                        val rh = mediaRealMin / 60
                        val rm = mediaRealMin % 60
                        val detalleReal = if (rh == 0 && rm == 0 && minutosTotalesAno > 0) {
                            val segundosDia = (minutosTotalesAno * 60) / totalDiasAno
                            " | Detalle ⇾ ${segundosDia}s / Día"
                        } else {
                            ""
                        }
                        val textoDias =
                            "Días Que Has Hecho Ejercicio Este Año $y ⇾ $totalDiasEntrenados Días | " + "Media De Sesiones ⇾ $mediaEntrenadosMin Min - ( ${eh}h ${em}m ) | " + "Media Real ⇾ $mediaRealMin Min - ( ${rh}h ${rm}m )$detalleReal"
                        val paintDias = Paint().apply {
                            color = Color.rgb(0, 0, 0)
                            textSize = 11f
                            isFakeBoldText = true
                        }
                        val xDias = (pageInfo.pageWidth - paintDias.measureText(textoDias)) / 2f
                        val yDias = pageInfo.pageHeight - 35f
                        canvas.drawText(textoDias, xDias, yDias, paintDias)
                    }
                    val paintAzul =
                        Paint().apply { color = Color.BLUE; textSize = 10f; isFakeBoldText = true }
                    val paintRojo =
                        Paint().apply { color = Color.RED; textSize = 10f; isFakeBoldText = true }
                    val texto1 = "Calendario Creado Por M8AX - "
                    val texto2 = "https://youtube.com/m8ax"
                    val xFooter =
                        (pageInfo.pageWidth - (paintAzul.measureText(texto1) + paintRojo.measureText(
                            texto2
                        ))) / 2f
                    val yFooter = pageInfo.pageHeight - 20f
                    canvas.drawText(texto1, xFooter, yFooter, paintAzul)
                    canvas.drawText(
                        texto2, xFooter + paintAzul.measureText(texto1), yFooter, paintRojo
                    )
                    pdf.finishPage(page)
                    calendariosGenerados++
                }
                year = blockEnd + 1
                System.gc()
            }
            val pageLogo = pdf.startPage(pageInfo)
            val canvasLogo = pageLogo.canvas
            val logos = listOf("logom8ax", "logom8ax3", "logom8ax4", "logom8ax5")
            val logoId = resources.getIdentifier(logos.random(), "drawable", packageName)
            if (logoId != 0) {
                val bmp = BitmapFactory.decodeResource(resources, logoId)
                val targetSize = 500
                val x = (pageInfo.pageWidth - targetSize) / 2f
                val y = pageInfo.pageHeight - targetSize - 10f
                val matrix = Matrix()
                matrix.postScale(
                    targetSize.toFloat() / bmp.width, targetSize.toFloat() / bmp.height
                )
                matrix.postTranslate(x, y)
                val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
                canvasLogo.drawBitmap(bmp, matrix, paint)
                bmp.recycle()
            }
            pdf.finishPage(pageLogo)
            val partes = mutableListOf<String>()
            if (cbDiasEntrenados.isChecked) partes.add("Gimnasio")
            if (cbPorcentajeLuna.isChecked) partes.add(if (tipoLuna == 1) "Pctje_Luna" else "Luna_Dibujo")
            val sufijo = if (partes.isEmpty()) "Normal" else partes.joinToString("-")
            val rango = if (startYear == endYear) "$startYear" else "$startYear-$endYear"
            val fileName = "M8AX - Calendario-$rango-$sufijo.PdF"
            val outputStream: OutputStream
            val uriToOpen: Uri
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val cv = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                }
                uriToOpen = contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, cv)!!
                outputStream = contentResolver.openOutputStream(uriToOpen)!!
            } else {
                val file = File(
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                    fileName
                )
                outputStream = FileOutputStream(file)
                uriToOpen = Uri.fromFile(file)
            }
            if (cancelPdfGeneration) {
                try {
                    pdf.close()
                    outputStream.close()
                } catch (_: Exception) {
                }
                return
            }
            runOnUiThread { tvProgreso.text = "\n\n\n\n\n\nGrabando PDF, Puede Tardar Un Poco..." }
            if (ttsEnabled) tts?.speak(
                "Grabando P D F; Puede Tardar Un Poco.", TextToSpeech.QUEUE_FLUSH, null, "ttsPdfId"
            )
            pdf.writeTo(outputStream)
            pdf.close()
            outputStream.close()
            runOnUiThread {
                tvProgreso.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
                tvProgreso.text =
                    "Nota 1 - Si Eliges Los Porcentajes De Luna Visible O Dibujos De Luna, En Las Celdas De Los Días De Cada Mes, Los Porcentajes O Dibujos Serán Calculados El Día En Concreto A Las 12:00:00. Intervalos Grandes Entre Años Puede Tardar Un Poco En Grabarlos En Fichero. Restricciones De Android Al Grabar Desde App Externa Al Sistema. Ser Paciente Es Tu Virtud.\n\nNota 2 - El Intervalo Máximo De Años Que Se Puede Generar Depende Del Tipo De Luna Seleccionado; Si Se Eligen Dibujos De Luna Al Pulsar El Botón De Crear, El Intervalo Se Limita A 10000 Años. Para Todas Las Demás Opciones, Incluyendo Porcentaje De Luna Visible Y Días Entrenados, El Intervalo Máximo Permitido Es De 20000 Años. Si Pones Un Rango Mayor Del Permitido El Sistema Ajustará Automáticamente El Año Final Para Que No Se Supere El Límite Según La Opción Elegida, Garantizando Que La Generación Del PDF No Cause Fallos Ni Bloqueos.\n\nNota 3 - 10000 Años Con Dibujos De Luna Ocupan Aproximadamente 750 MB. 20000 Años Con Cualquier Otra Opción, Ocupan Aproximadamente 70 MB.\n\n... By M8AX ..."
                Toast.makeText(this, "PDF Generado En Descargas", Toast.LENGTH_LONG).show()
            }
            if (ttsEnabled) tts?.speak(
                "P D F; Generado Correctamente En Descargas",
                TextToSpeech.QUEUE_FLUSH,
                null,
                "ttsPdfId"
            )
            startActivity(Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uriToOpen, "application/pdf")
                flags = Intent.FLAG_ACTIVITY_NO_HISTORY or Intent.FLAG_GRANT_READ_URI_PERMISSION
            })
        } catch (e: Exception) {
            e.printStackTrace()
            val yearAInput = etYearA.text.toString().toIntOrNull() ?: 1
            val yearBInput = etYearB.text.toString().toIntOrNull() ?: 1
            runOnUiThread {
                val mensaje =
                    if (yearAInput <= 0 || yearBInput <= 0) "Error: Año 0 No Válido Para Cálculo Lunar" else "Error Al Generar PDF"
                Toast.makeText(
                    this,
                    mensaje,
                    if (yearAInput <= 0 || yearBInput <= 0) Toast.LENGTH_LONG else Toast.LENGTH_SHORT
                ).show()
            }
            if (ttsEnabled) {
                if (yearAInput <= 0 || yearBInput <= 0) tts?.speak(
                    "Error: Año 0 No Válido Para Cálculo Lunar",
                    TextToSpeech.QUEUE_FLUSH,
                    null,
                    "ttsPdfId"
                ) else tts?.speak(
                    "Error Al Generar El Fichero PDF", TextToSpeech.QUEUE_FLUSH, null, "ttsPdfId"
                )
            }
        }
    }

    private fun getPorcentajeLuna(fecha: Calendar): Int {
        val zoned = java.time.ZonedDateTime.of(
            fecha.get(Calendar.YEAR),
            fecha.get(Calendar.MONTH) + 1,
            fecha.get(Calendar.DAY_OF_MONTH),
            12,
            0,
            0,
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

    private fun obtenerDiasEntrenados(year: Int): Pair<Map<LocalDate, Int>, Int> {
        val sdf = DateTimeFormatter.ofPattern("dd/MM/yyyy")
        val lista = AppDatabase.getDatabase(this).gimnasioDao().getEntrenadosPorAno(year.toString())
        var totalMinutosAno = 0
        val mapa = lista.mapNotNull {
            val fecha = LocalDate.parse(it.fechaHora.substring(0, 10), sdf)
            totalMinutosAno += it.valor
            val color = when {
                it.valor < 45 -> Color.RED
                it.valor < 61 -> Color.rgb(34, 139, 34)
                it.valor < 91 -> Color.rgb(255, 140, 0)
                else -> Color.BLUE
            }
            fecha to color
        }.toMap()
        return mapa to totalMinutosAno
    }

    override fun onBackPressed() {
        cancelPdfGeneration = true
        super.onBackPressed()
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer.stop()
        mediaPlayer.release()
        tts?.shutdown()
    }

    override fun onPause() {
        super.onPause()
        mediaPlayer?.pause()
    }

    override fun onResume() {
        super.onResume()
        mediaPlayer?.start()
    }
}