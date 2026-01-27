package com.mviiiax.m8ax_diariogimnasio

import android.app.AlertDialog
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Color
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.speech.tts.TextToSpeech
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.MaterialToolbar
import com.itextpdf.text.Anchor
import com.itextpdf.text.BaseColor
import com.itextpdf.text.Chunk
import com.itextpdf.text.Document
import com.itextpdf.text.Element
import com.itextpdf.text.Font
import com.itextpdf.text.Image
import com.itextpdf.text.Paragraph
import com.itextpdf.text.Rectangle
import com.itextpdf.text.pdf.BaseFont
import com.itextpdf.text.pdf.PdfPCell
import com.itextpdf.text.pdf.PdfPTable
import com.itextpdf.text.pdf.PdfWriter
import com.itextpdf.text.pdf.draw.LineSeparator
import me.zhanghai.android.fastscroll.FastScrollerBuilder
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class AppGlucosa : AppCompatActivity() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: GlucosaAdapter
    private lateinit var db: AppDatabase2
    private lateinit var toolbar: MaterialToolbar
    private var ttsEnabled: Boolean = true
    private var tts: android.speech.tts.TextToSpeech? = null
    private var mediaPlayer: MediaPlayer? = null

    companion object {
        const val REQUEST_CODE_RESTAURAR_DB = 1234
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main2)
        val prefs = getSharedPreferences("M8AX-Config_TTS", MODE_PRIVATE)
        ttsEnabled = prefs.getBoolean("tts_enabled", true)
        toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        toolbar.setTitleTextColor(Color.WHITE)
        toolbar.popupTheme = R.style.ToolbarPopupMenuBlack
        db = AppDatabase2.getDatabase(this)
        recyclerView = findViewById(R.id.rvGlucosa)
        recyclerView.layoutManager = LinearLayoutManager(this)
        val titulosAleatorios = listOf(
            "🩸 - Diario De Glucosa De M8AX - 🩸",
            "⚕️ - Diario De Glucosa De M8AX - ⚕️",
            "🧪 - Diario De Glucosa De M8AX - 🧪",
            "🩺 - Diario De Glucosa De M8AX - 🩺",
            "🛡️ - Diario De Glucosa De M8AX - 🛡️",
            "📈 - Diario De Glucosa De M8AX - 📈"
        )
        supportActionBar?.title = titulosAleatorios.random()
        var lista = db.glucosaDao().getAll()
        val sdfFull = SimpleDateFormat("dd/MM/yyyy - HH:mm:ss", Locale.US)
        val fechaHoraActual = sdfFull.format(Date())
        val hoy = SimpleDateFormat("dd/MM/yyyy", Locale.US).format(Date())
        val existeHoy = lista.any { it.fechaHora.startsWith(hoy) }
        if (!existeHoy) {
            val registro = Glucosa(fechaHora = fechaHoraActual, valor = 0)
            db.glucosaDao().insert(registro)
            lista = db.glucosaDao().getAll()
        }
        adapter = GlucosaAdapter(lista, db.glucosaDao())
        recyclerView.adapter = adapter
        recyclerView.descendantFocusability = android.view.ViewGroup.FOCUS_AFTER_DESCENDANTS
        recyclerView.isFocusable = false
        recyclerView.isFocusableInTouchMode = false
        FastScrollerBuilder(recyclerView).setPopupTextProvider { _, _ ->
            val layoutManager = recyclerView.layoutManager as LinearLayoutManager
            val firstVisible = layoutManager.findFirstVisibleItemPosition()
            val lastVisible = layoutManager.findLastVisibleItemPosition()
            if (firstVisible == RecyclerView.NO_POSITION || lastVisible == RecyclerView.NO_POSITION) ""
            else {
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
                adapter.items[closestPosition].fechaHora.substring(0, 10)
            }
        }.build()
        recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                val currentFocus = recyclerView.findFocus()
                if (currentFocus !is android.widget.EditText) {
                    recyclerView.clearFocus()
                }
            }

            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
                val currentFocus = recyclerView.findFocus()
                if (newState == RecyclerView.SCROLL_STATE_IDLE && currentFocus !is android.widget.EditText) {
                    recyclerView.clearFocus()
                }
            }
        })
        recyclerView.post { recyclerView.scrollToPosition(0) }
        mediaPlayer = MediaPlayer.create(this, R.raw.m8axhospital)
        mediaPlayer?.isLooping = true
        mediaPlayer?.start()
        tts = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.setLanguage(tts?.defaultLanguage ?: Locale.getDefault())
                tts?.setSpeechRate(0.9f)
                if (savedInstanceState == null && ttsEnabled) {
                    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
                    val saludo = when (hour) {
                        in 6..11 -> "Buenos Días"
                        in 12..19 -> "Buenas Tardes"
                        else -> "Buenas Noches"
                    }
                    val listaValidos = db.glucosaDao().getAll().filter { it.valor > 0 }
                    val media = if (listaValidos.isNotEmpty()) listaValidos.map { it.valor }
                        .average() else 0.0
                    val estado = when {
                        media < 1 -> "Sin Registros"
                        media < 70 -> "Baja"
                        media in 70.0..99.9 -> "Normal"
                        media in 100.0..125.9 -> "Ligeramente Alta"
                        else -> "Alta"
                    }
                    val msj = if (listaValidos.isEmpty()) {
                        "Hola, $saludo. Aún No Tienes Registros De Glucosa."
                    } else {
                        "Hola, $saludo. Tienes ${listaValidos.size} Registros Válidos De Glucosa. Tu Media Es ${
                            String.format(
                                "%.1f", media
                            )
                        } Miligramos Por Decilitro. Tu Media De Glucosa Es $estado."
                    }
                    tts?.speak(msj, TextToSpeech.QUEUE_FLUSH, null, "saludoId")
                }
            }
        }
        val grafica = findViewById<GraficaSimple2>(R.id.miGrafica2)
        grafica.setOnClickListener {
            if (ttsEnabled && tts != null) {
                val ultimos30 = db.glucosaDao().getUltimos30().filter { it.valor > 0 }
                if (ultimos30.isNotEmpty()) {
                    val media = ultimos30.map { it.valor }.average()
                    val estado = when {
                        media < 70 -> "Baja"
                        media < 100 -> "Normal"
                        media < 126 -> "Ligeramente Alta"
                        else -> "Alta"
                    }
                    val msj = "La Media De Los Últimos ${ultimos30.size} Registros Es De ${
                        String.format(
                            "%.1f", media
                        )
                    } Miligramos Por Decilitro. Tu Media De Glucosa Es $estado."
                    tts?.speak(msj, TextToSpeech.QUEUE_FLUSH, null, "mediaGraficaId")
                }
            }
        }
        refrescarGrafica()
    }

    private fun limpiarRegistrosCero() {
        if (ttsEnabled) {
            tts?.speak(
                "Voy A Limpiar Los Registros Vacíos. ¿Deseas Continuar?",
                TextToSpeech.QUEUE_FLUSH,
                null,
                "limpiarId"
            )
        }
        AlertDialog.Builder(this).setTitle("Limpiar Registros Con Glucosa A 0")
            .setMessage("Se Eliminarán Todos Los Registros Que Tengan El Valor De Glucosa A 0.\n\n¿ Deseas Continuar ?")
            .setPositiveButton("Sí") { _, _ ->
                val cuantosHabiaAntes = db.glucosaDao().getAll().size
                db.glucosaDao().borrarRegistrosVacios()
                var listaActual = db.glucosaDao().getAll()
                var seHaCreadoHoy = false
                if (listaActual.isEmpty()) {
                    val sdfFull = SimpleDateFormat("dd/MM/yyyy - HH:mm:ss", Locale.US)
                    val fechaHoraActual = sdfFull.format(Date())
                    val registroHoy = Glucosa(fechaHora = fechaHoraActual, valor = 0)
                    db.glucosaDao().insert(registroHoy)
                    listaActual = db.glucosaDao().getAll()
                    seHaCreadoHoy = true
                }
                val sdf = SimpleDateFormat("dd/MM/yyyy - HH:mm:ss", Locale.US)
                val listaOrdenada = listaActual.sortedByDescending { registro ->
                    try {
                        sdf.parse(registro.fechaHora)?.time
                    } catch (e: Exception) {
                        0L
                    }
                }
                val cuantosHayAhora = listaActual.size
                adapter.updateData(listaOrdenada)
                refrescarGrafica()
                val borrados = cuantosHabiaAntes - cuantosHayAhora
                when {
                    cuantosHabiaAntes == cuantosHayAhora && !seHaCreadoHoy -> {
                        Toast.makeText(
                            this, "No Había Ningún Registro Con Glucosa A 0", Toast.LENGTH_SHORT
                        ).show()
                        if (ttsEnabled) tts?.speak(
                            "No Había Ningún Registro Con Glucosa A 0 Para Limpiar.",
                            TextToSpeech.QUEUE_FLUSH,
                            null,
                            "nadaId"
                        )
                    }

                    seHaCreadoHoy -> {
                        refrescarGrafica()
                        Toast.makeText(
                            this, "Limpieza Completa. Se Creó Registro De Hoy", Toast.LENGTH_LONG
                        ).show()
                        if (ttsEnabled) tts?.speak(
                            "Limpieza Completa. He Mantenido El Registro De Hoy Para Que La Lista No Se Quede Vacía.",
                            TextToSpeech.QUEUE_FLUSH,
                            null,
                            "limpiarFinId"
                        )
                    }

                    else -> {
                        refrescarGrafica()
                        Toast.makeText(
                            this, "Se Han Eliminado $borrados Registros", Toast.LENGTH_SHORT
                        ).show()
                        if (ttsEnabled) tts?.speak(
                            "He Eliminado $borrados Registros Correctamente.",
                            TextToSpeech.QUEUE_FLUSH,
                            null,
                            "limpiarExitoId"
                        )
                    }
                }
            }.setNegativeButton("No") { _, _ ->
                Toast.makeText(this, "No Hacemos Ningún Cambio", Toast.LENGTH_LONG).show()
                if (ttsEnabled) {
                    tts?.speak(
                        "Vale, No Tocaré Nada.", TextToSpeech.QUEUE_FLUSH, null, "cancelarId"
                    )
                }
            }.show()
    }

    private fun ocultarTeclado() {
        try {
            val imm =
                getSystemService(Context.INPUT_METHOD_SERVICE) as? android.view.inputmethod.InputMethodManager
            val view = currentFocus ?: window.decorView.rootView ?: View(this)
            imm?.hideSoftInputFromWindow(view.windowToken, 0)
            view.clearFocus()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun refrescarGrafica() {
        val grafica = findViewById<GraficaSimple2>(R.id.miGrafica2)
        val ultimos30 = db.glucosaDao().getUltimos30()
        grafica.setData(ultimos30)
    }

    override fun onStart() {
        super.onStart()
        val sdf = SimpleDateFormat("dd/MM/yyyy - HH:mm:ss", Locale.US)
        val listaOrdenada = db.glucosaDao().getAll().sortedByDescending { registro ->
            sdf.parse(registro.fechaHora)?.time ?: 0L
        }
        adapter.updateData(listaOrdenada)
        refrescarGrafica()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_main2, menu)
        menu?.let {
            for (i in 0 until it.size()) {
                it.getItem(i).icon?.mutate()?.setTint(Color.WHITE)
            }
        }
        toolbar.overflowIcon?.mutate()?.setTint(Color.WHITE)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {

            R.id.action_export_pdf -> {
                ocultarTeclado()
                mostrarMenuExportacion()
                return true
            }

            R.id.action_limpiar_ceros -> {
                limpiarRegistrosCero()
                return true
            }

            R.id.action_clear_db -> {
                confirmClearDatabase()
                return true
            }

            R.id.action_health_tips -> {
                showHealthTipsDialog()
                return true
            }

            R.id.action_web_resources -> {
                showMedicalResources()
                return true
            }

            R.id.action_copiar_db_download -> {
                copiarDBADownload(this, "M8AX-Glucosa_DB", db)
                if (ttsEnabled) {
                    tts?.speak(
                        "Copia De Base De Datos De Glucosa, A Carpeta Downloads; Correcta. Reiniciando.",
                        TextToSpeech.QUEUE_FLUSH,
                        null,
                        "ttsFlexionesId"
                    )
                }
                return true
            }

            R.id.action_restaurar_db_download -> {
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
        }
        return super.onOptionsItemSelected(item)
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
            val dbName = "M8AX-Glucosa_DB"
            val dbFile = getDatabasePath(dbName)
            var nombreReal = ""
            contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                if (cursor.moveToFirst()) nombreReal = cursor.getString(nameIndex)
            }
            if (!nombreReal.uppercase().startsWith("M8AX-GLUCOSA_DB") || !nombreReal.lowercase()
                    .endsWith(".db")
            ) {
                toast("Error: El Fichero Seleccionado No Es Válido")
                if (ttsEnabled) tts?.speak(
                    "Error; El Fichero Seleccionado No Es Válido.",
                    TextToSpeech.QUEUE_FLUSH,
                    null,
                    "ttsId"
                )
                return
            }
            try {
                db.close()
            } catch (_: Exception) {
            }
            try {
                AppDatabase2.closeInstance()
            } catch (_: Exception) {
            }
            File(dbFile.parent, "$dbName-wal").delete()
            File(dbFile.parent, "$dbName-shm").delete()
            contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(dbFile).use { output -> input.copyTo(output) }
            } ?: throw Exception("Stream Nulo")
            toast("Copia Restaurada Desde Backup\nReiniciando...")
            if (ttsEnabled) tts?.speak(
                "Copia De Glucosa, Restaurada Desde Backup Seleccionado; Reiniciando...",
                TextToSpeech.QUEUE_FLUSH,
                null,
                "ttsId"
            )
            reiniciarApp(this)
        } catch (e: Exception) {
            e.printStackTrace()
            toast("Error Al Restaurar: Fichero No Válido")
            if (ttsEnabled) tts?.speak(
                "Error Al Restaurar; Fichero No Válido.", TextToSpeech.QUEUE_FLUSH, null, "ttsId"
            )
        }
    }

    private fun toast(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
        if (ttsEnabled) {
            tts?.speak(msg, TextToSpeech.QUEUE_FLUSH, null, "ttsID")
        }
    }

    private fun reiniciarApp(context: Context) {
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)
            intent?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            context.startActivity(intent)
            if (context is android.app.Activity) {
                context.finishAffinity()
            }
            android.os.Process.killProcess(android.os.Process.myPid())
            System.exit(0)
        }, 7000L)
    }

    private fun showMedicalResources() {
        val opciones =
            arrayOf("Fundación Para La Diabetes", "OMS - Diabetes", "Artículos De Interés")
        if (ttsEnabled) {
            tts?.speak(
                "Vale, Elige La Página Que Quieras Ver; Se Abrirá En Tu Navegador Predeterminado.",
                TextToSpeech.QUEUE_FLUSH,
                null,
                "ttsNoId"
            )
        }
        AlertDialog.Builder(this).setTitle("Recursos Médicos Oficiales")
            .setItems(opciones) { _, indice ->
                val url = when (indice) {
                    0 -> "https://www.fundaciondiabetes.org/"
                    1 -> "https://www.who.int/es/news-room/fact-sheets/detail/diabetes"
                    else -> "https://www.diabetes.org"
                }
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                startActivity(intent)
            }.setNegativeButton("Cerrar", null).show()
    }

    private fun showHealthTipsDialog() {
        val tips = """
        • Qué Es La Glucosa: Es El Azúcar Principal En La Sangre Y La Fuente De Energía Del Cuerpo.
        
        • Rango Normal: Generalmente Entre 70 Y 99 mg/dL En Ayunas.
        
        • Tip De Salud: Caminar 15 Minutos Después De Comer Ayuda A Estabilizar Los Picos De Glucosa.
        
        • Importancia: Un Control Diario Ayuda A Prevenir Complicaciones A Largo Plazo.
    """.trimIndent()
        AlertDialog.Builder(this).setTitle("Consejos Y Educación").setMessage(tips)
            .setPositiveButton("Entendido") { _, _ ->
                tts?.stop()
            }.show()
        if (ttsEnabled) {
            val textoParaLeer = tips.replace("•", "").replace("mg/dL", "Miligramos Por Decilitro")
            tts?.speak(
                "Aquí Tienes Una Nota Educativa Y Algunos Consejos De Salud; $textoParaLeer",
                TextToSpeech.QUEUE_FLUSH,
                null,
                "tipsId"
            )
        }
    }

    fun aRomano(num: Int): String {
        if (num > 1_000_000) return num.toString()
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

    private fun confirmClearDatabase() {
        if (ttsEnabled) {
            tts?.speak(
                "Estás A Punto De Borrar Todos Los Registros De Glucosa, ¿Estás Seguro?",
                TextToSpeech.QUEUE_FLUSH,
                null,
                "ttsPdfId"
            )
        }
        AlertDialog.Builder(this).setTitle("Borrar Base De Datos")
            .setMessage("¿ Estás Seguro De Que Quieres Borrar Todos Los Registros ?")
            .setPositiveButton("Sí") { _, _ ->
                db.clearAllTables()
                val sdfFull = SimpleDateFormat("dd/MM/yyyy - HH:mm:ss", Locale.US)
                val fechaHoraActual = sdfFull.format(Date())
                val registroHoy = Glucosa(fechaHora = fechaHoraActual, valor = 0)
                db.glucosaDao().insert(registroHoy)
                val nuevaLista = db.glucosaDao().getAll()
                adapter.updateData(nuevaLista)
                refrescarGrafica()
                Toast.makeText(this, "Base De Datos Reiniciada", Toast.LENGTH_SHORT).show()
                if (ttsEnabled) {
                    tts?.speak(
                        "Base De Datos Borrada Correctamente. He Creado El Registro De Hoy.",
                        TextToSpeech.QUEUE_FLUSH,
                        null,
                        "ttsPdfId"
                    )
                }
            }.setNegativeButton("No") { _, _ ->
                Toast.makeText(this, "Operación De Borrado Cancelada", Toast.LENGTH_SHORT).show()
                if (ttsEnabled) {
                    tts?.speak(
                        "Vale, No Voy A Borrar Nada, En Absoluto.",
                        TextToSpeech.QUEUE_FLUSH,
                        null,
                        "ttsNoId"
                    )
                }
            }.show()
    }

    private fun mostrarMenuExportacion() {
        if (ttsEnabled) {
            tts?.speak(
                "Elige Orden De Exportación.", TextToSpeech.QUEUE_FLUSH, null, "ttsNoId"
            )
        }
        val opciones = arrayOf(
            "Fecha: Más Antigua → Más Reciente",
            "Fecha: Más Reciente → Más Antigua",
            "Glucosa: Menor → Mayor",
            "Glucosa: Mayor → Menor"
        )
        AlertDialog.Builder(this).setTitle("Selecciona El Orden De Exportación")
            .setItems(opciones) { _, indice ->
                var lista = db.glucosaDao().getAll()
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
                    return@setItems
                }
                val formatoFecha = SimpleDateFormat("dd/MM/yyyy", Locale.US)
                lista = when (indice) {
                    0 -> lista.sortedBy { formatoFecha.parse(it.fechaHora.take(10)) ?: Date(0) }
                    1 -> lista.sortedByDescending {
                        formatoFecha.parse(it.fechaHora.take(10)) ?: Date(0)
                    }

                    2 -> lista.sortedBy { it.valor }
                    3 -> lista.sortedByDescending { it.valor }
                    else -> lista
                }
                exportPdf(lista)
            }.show()
    }

    private fun exportPdf(lista: List<Glucosa>) {
        if (lista.none { it.valor > 0 }) {
            Toast.makeText(this, "No Hay Datos Válidos Para Exportar", Toast.LENGTH_SHORT).show()
            if (ttsEnabled) tts?.speak(
                "No Hay Datos Válidos, Para Exportar A... P D F.",
                TextToSpeech.QUEUE_FLUSH,
                null,
                null
            )
            return
        }
        try {
            val sdfFileName = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)
            val fechaParaArchivo = sdfFileName.format(Date())
            val fileName = "M8AX-Diario-Glucosa_$fechaParaArchivo.PdF"
            val document = Document()
            var outputStream: OutputStream? = null
            var pdfFile: File? = null
            var uriToOpen: Uri? = null
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                }
                uriToOpen =
                    contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                if (uriToOpen != null) {
                    outputStream = contentResolver.openOutputStream(uriToOpen)
                } else {
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
            val baseFont = BaseFont.createFont(
                "assets/fonts/mviiiax.ttf", BaseFont.IDENTITY_H, BaseFont.EMBEDDED
            )
            val fontTituloo = Font(baseFont, 14f, Font.BOLD, BaseColor(0, 0, 139))
            document.add(Paragraph("--- DIARIO DE GLUCOSA DE M8AX ---\n\n", fontTituloo))
            var contador = 0
            lista.forEach { itItem ->
                val fontNormal = Font(baseFont, 12f, Font.NORMAL)
                val fontBold = Font(baseFont, 12f, Font.BOLD)
                val valor = itItem.valor
                val colorValor = when {
                    valor < 70 -> BaseColor(255, 0, 0)
                    valor in 70..99 -> BaseColor(0, 100, 0)
                    valor in 100..125 -> BaseColor(255, 140, 0)
                    else -> BaseColor(255, 0, 0)
                }
                val fontValor = Font(baseFont, 12f, Font.BOLD, colorValor)
                val p = Paragraph()
                contador++
                p.add(Chunk("$contador - ${itItem.fechaHora}", fontBold))
                p.add(Chunk(" -----▶ ", fontNormal))
                p.add(Chunk("${valor} mg/dL", fontValor))
                document.add(p)
            }
            val listaValidos = lista.filter { it.valor > 0 }
            val totalRegistros = listaValidos.size
            val mediaGlucosa =
                if (totalRegistros > 0) listaValidos.map { it.valor }.average() else 0.0
            val mediaFormateada = String.format("%.1f", mediaGlucosa)
            val colorMedia = when {
                mediaGlucosa < 70 -> BaseColor(255, 0, 0)
                mediaGlucosa < 100 -> BaseColor(0, 100, 0)
                mediaGlucosa < 126 -> BaseColor(255, 140, 0)
                else -> BaseColor(255, 0, 0)
            }
            val fontResumen = Font(baseFont, 12f, Font.BOLD, colorMedia)
            val fontMensaje = Font(baseFont, 12f, Font.BOLD, colorMedia)
            val mensajeMedia = when {
                mediaGlucosa < 70 -> "Tu Media Es Baja, Cuida Tu Alimentación."
                mediaGlucosa < 100 -> "Tu Media Está Dentro Del Rango Recomendado, Sigue Así..."
                mediaGlucosa < 126 -> "Tu Media Es Ligeramente Alta, Vigila Tu Glucosa. Camina, Que No Caminas Nada..."
                else -> "Tu Media Es Alta, Come Más Sano Y Haz Deporte. Vigila Tu Glucosa Y Consulta Con Tu Médico Si Es Necesario."
            }
            val resumen = Paragraph()
            resumen.add(Chunk("\n--- RESUMEN FINAL ---\n\n", fontTituloo))
            resumen.add(
                Chunk(
                    "Total De Registros Válidos ▶ $totalRegistros - ((( ${aRomano(totalRegistros)} )))\n",
                    fontResumen
                )
            )
            resumen.add(Chunk("Media De Glucosa ▶ $mediaFormateada mg/dL\n", fontResumen))
            resumen.add(Chunk("$mensajeMedia\n\n", fontMensaje))
            document.add(resumen)
            try {
                val graficaView = findViewById<GraficaSimple2>(R.id.miGrafica2)
                if (graficaView.width > 0 && graficaView.height > 0) {
                    val tablaGrafica = PdfPTable(1)
                    tablaGrafica.widthPercentage = 100f
                    tablaGrafica.setKeepTogether(true)
                    val bitmap = android.graphics.Bitmap.createBitmap(
                        graficaView.width,
                        graficaView.height,
                        android.graphics.Bitmap.Config.ARGB_8888
                    )
                    graficaView.draw(android.graphics.Canvas(bitmap))
                    val stream = java.io.ByteArrayOutputStream()
                    bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, stream)
                    val imagenPdf = com.itextpdf.text.Image.getInstance(stream.toByteArray())
                    imagenPdf.alignment = Element.ALIGN_CENTER
                    val anchoPagina =
                        document.pageSize.width - document.leftMargin() - document.rightMargin()
                    imagenPdf.scaleToFit(anchoPagina, 230f)
                    val font12Azul = Font(baseFont, 11f, Font.BOLD, BaseColor(0, 0, 139))
                    val celda = PdfPCell()
                    celda.border = Rectangle.NO_BORDER
                    celda.horizontalAlignment = Element.ALIGN_CENTER
                    val parrafo = Paragraph(
                        "⚫⚫⚫ ANÁLISIS DE TENDENCIA ▶ ( ÚLTIMOS 30 REGISTROS ) ⚫⚫⚫\n\n", font12Azul
                    )
                    parrafo.alignment = Element.ALIGN_CENTER
                    imagenPdf.alignment = Element.ALIGN_CENTER
                    celda.addElement(parrafo)
                    celda.addElement(imagenPdf)
                    tablaGrafica.addCell(celda)
                    document.add(tablaGrafica)
                }
            } catch (e: Exception) {
                document.add(Paragraph("\n( Gráfica No Disponible )\n"))
            }
            val sep = LineSeparator().apply {
                lineWidth = 1f
                percentage = 100f
                lineColor = BaseColor.GRAY
            }
            document.add(Chunk(sep))
            val tablaLogos = PdfPTable(2)
            tablaLogos.widthPercentage = 50f
            tablaLogos.horizontalAlignment = Element.ALIGN_CENTER
            val logo1 = getImageFromDrawable(R.drawable.logoapp)
            val logos = arrayOf(
                R.drawable.logom8ax,
                R.drawable.logom8ax3,
                R.drawable.logom8ax4,
                R.drawable.logom8ax5,
                R.drawable.logom8ax6,
                R.drawable.logom8ax7,
                R.drawable.logom8ax8,
                R.drawable.logom8ax9,
                R.drawable.logom8ax10
            )
            val logo2 = getImageFromDrawable(logos.random())
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
                    "\n\nBy M8AX Corp. ( ${
                        Calendar.getInstance().get(Calendar.YEAR)
                    } - ${
                        aRomano(Calendar.getInstance().get(Calendar.YEAR))
                    } )\n\n"
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

    fun getImageFromDrawable(resId: Int): Image {
        val bitmap = BitmapFactory.decodeResource(resources, resId)
        val stream = ByteArrayOutputStream()
        bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, stream)
        val imageBytes = stream.toByteArray()
        return Image.getInstance(imageBytes)
    }

    override fun onResume() {
        super.onResume()
        refrescarGrafica()
        if (mediaPlayer == null) {
            mediaPlayer = MediaPlayer.create(this, R.raw.m8axhospital)
            mediaPlayer?.isLooping = true
        }
        mediaPlayer?.start()
    }

    override fun onPause() {
        super.onPause()
        mediaPlayer?.pause()
    }

    override fun onDestroy() {
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
        tts?.stop()
        tts?.shutdown()
        tts = null
        super.onDestroy()
    }
}