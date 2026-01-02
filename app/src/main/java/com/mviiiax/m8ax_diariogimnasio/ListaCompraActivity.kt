package com.mviiiax.m8ax_diariogimnasio

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import com.itextpdf.text.BaseColor
import com.itextpdf.text.Document
import com.itextpdf.text.Element
import com.itextpdf.text.Font
import com.itextpdf.text.Paragraph
import com.itextpdf.text.pdf.PdfWriter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ListaCompraActivity : AppCompatActivity(), TextToSpeech.OnInitListener {
    private lateinit var prefs: SharedPreferences
    private lateinit var prefsCompra: SharedPreferences
    private var ttsEnabled: Boolean = true
    private var tts: TextToSpeech? = null
    private var mediaPlayer: MediaPlayer? = null
    private lateinit var adapter: ListaCompraAdapter
    private val items = mutableListOf<CompraItem>()
    private val coroutineScope = CoroutineScope(Dispatchers.Main + Job())
    private lateinit var textInfo: TextView
    private var mensaje: String = ""
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_lista_compra)
        window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        prefs = getSharedPreferences("M8AX-Config_TTS", Context.MODE_PRIVATE)
        ttsEnabled = prefs.getBoolean("tts_enabled", true)
        prefsCompra = getSharedPreferences("M8AX-Lista_De_La_Compra", Context.MODE_PRIVATE)
        tts = TextToSpeech(this, this)
        mediaPlayer = MediaPlayer.create(this, R.raw.m8axsonidofondo)
        mediaPlayer?.isLooping = true
        mediaPlayer?.setVolume(0f, 0f)
        mediaPlayer?.start()
        fadeInMusic()
        val recyclerView = findViewById<RecyclerView>(R.id.recyclerViewLista)
        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = ListaCompraAdapter(items, {
            saveLista()
            actualizarTextInfo()
        }, ::mostrarMensaje)
        recyclerView.adapter = adapter
        val checkRomano = findViewById<CheckBox>(R.id.checkRomano)
        checkRomano.isChecked = true
        checkRomano.setOnCheckedChangeListener { _, isChecked ->
            adapter.setUsarRomano(isChecked)
            val mensaje = if (isChecked) "Notación Romana Activada" else "Notación Arábiga Activada"
            mostrarMensaje(mensaje)
        }
        textInfo = findViewById(R.id.textInfo)
        val editTextItem = findViewById<EditText>(R.id.editTextItem)
        val btnExportPdf = findViewById<Button>(R.id.btnExportPdf)
        btnExportPdf.setOnClickListener {
            exportarListaAPdf()
        }
        val btnAdd = findViewById<Button>(R.id.btnAdd)
        btnAdd.setOnClickListener {
            val text = editTextItem.text.toString().trim()
            if (text.isNotEmpty()) {
                items.add(CompraItem(text, false))
                adapter.notifyItemInserted(items.size - 1)
                saveLista()
                actualizarTextInfo()
                mostrarMensaje("Añadido $text; A La Lista.")
                editTextItem.text.clear()
            }
        }
        val swipeHandler = object :
            ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {
            override fun onMove(
                rv: RecyclerView, vh: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder
            ) = false

            override fun onSwiped(vh: RecyclerView.ViewHolder, direction: Int) {
                val position = vh.adapterPosition
                val itemBorrado = items[position]
                adapter.eliminarItem(position)
                saveLista()
                actualizarTextInfo()
                mostrarMensaje("Borrado ${itemBorrado.nombre}; De La Lista.")
                Snackbar.make(
                    findViewById(R.id.recyclerViewLista),
                    "${itemBorrado.nombre} borrado",
                    Snackbar.LENGTH_LONG
                ).setAction("Deshacer") {
                    items.add(position, itemBorrado)
                    adapter.notifyDataSetChanged()
                    saveLista()
                    actualizarTextInfo()
                    mostrarMensaje("Restaurado ${itemBorrado.nombre}; En La Lista.")
                }.show()
            }
        }
        ItemTouchHelper(swipeHandler).attachToRecyclerView(recyclerView)
        loadLista()
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts?.setLanguage(tts?.defaultLanguage ?: Locale.getDefault())
            tts?.setSpeechRate(0.9f)
        }
    }

    private fun mostrarMensaje(texto: String) {
        Toast.makeText(this, texto, Toast.LENGTH_SHORT).show()
        if (ttsEnabled) {
            tts?.stop()
            tts?.speak(texto, TextToSpeech.QUEUE_FLUSH, null, "tts_compra")
        }
    }

    private fun saveLista() {
        val jsonArray = JSONArray()
        items.forEach {
            val obj = JSONObject()
            obj.put("Nombre", it.nombre)
            obj.put("Comprado", it.comprado)
            jsonArray.put(obj)
        }
        prefsCompra.edit().putString("Lista_Compra_JsoN", jsonArray.toString()).apply()
    }

    private fun loadLista() {
        val jsonStr = prefsCompra.getString("Lista_Compra_JsoN", null)
        items.clear()
        jsonStr?.let {
            val jsonArray = JSONArray(it)
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val nombre = obj.getString("Nombre")
                val comprado = obj.getBoolean("Comprado")
                items.add(CompraItem(nombre, comprado))
            }
        }
        adapter.notifyDataSetChanged()
        actualizarTextInfo()
    }

    private fun actualizarTextInfo() {
        val abreviatura = if (items.size == 1) "Producto" else "Productos"
        textInfo.text = "Tu Lista De La Compra - [ ${items.size} $abreviatura ] -"
    }

    private fun fadeInMusic() {
        mediaPlayer?.let { mp ->
            coroutineScope.launch {
                for (i in 0..10) {
                    val vol = i / 10f
                    mp.setVolume(vol, vol)
                    delay(100)
                }
            }
        }
    }

    private fun fadeOutMusic(onComplete: (() -> Unit)? = null) {
        mediaPlayer?.let { mp ->
            coroutineScope.launch {
                for (i in 10 downTo 0) {
                    val vol = i / 10f
                    mp.setVolume(vol, vol)
                    delay(100)
                }
                mp.stop()
                mp.release()
                mediaPlayer = null
                onComplete?.invoke()
            }
        } ?: onComplete?.invoke()
    }

    override fun onPause() {
        super.onPause()
        fadeOutMusic()
    }

    override fun onResume() {
        super.onResume()
        if (mediaPlayer == null) {
            mediaPlayer = MediaPlayer.create(this, R.raw.m8axsonidofondo)
            mediaPlayer?.isLooping = true
            mediaPlayer?.setVolume(0f, 0f)
        }
        mediaPlayer?.start()
        fadeInMusic()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1001) {
            if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                exportarListaAPdf()
            } else {
                Toast.makeText(
                    this, "Permiso Denegado. No Se Puede Guardar El PDF...", Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun exportarListaAPdf() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            if (ContextCompat.checkSelfPermission(
                    this, Manifest.permission.WRITE_EXTERNAL_STORAGE
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), 1001
                )
                return
            }
        }
        try {
            if (items.isEmpty()) {
                mostrarMensaje("No Hay Productos En La Lista, Para Exportar.")
                return
            }
            val fechaHora = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val fileName = "M8AX-Lista-Compra_$fechaHora.PdF"
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
                outputStream = uriToOpen?.let { contentResolver.openOutputStream(it) }
                if (outputStream == null) {
                    mostrarMensaje("Error Al Crear El Fichero P D F.")
                    return
                }
            } else {
                val downloadsDir =
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                pdfFile = File(downloadsDir, fileName)
                outputStream = FileOutputStream(pdfFile)
                uriToOpen = Uri.fromFile(pdfFile)
            }
            val inputStream = assets.open("fonts/mviiiax.ttf")
            val outFile = File(cacheDir, "mviiiax.ttf")
            inputStream.copyTo(FileOutputStream(outFile))
            inputStream.close()
            val baseFont = com.itextpdf.text.pdf.BaseFont.createFont(
                outFile.absolutePath,
                com.itextpdf.text.pdf.BaseFont.IDENTITY_H,
                com.itextpdf.text.pdf.BaseFont.EMBEDDED
            )
            val fuenteEncabezado = Font(baseFont, 16f, Font.BOLD)
            val fuenteItems = Font(baseFont, 14f, Font.NORMAL)
            val fuenteItemsItalic = Font(baseFont, 14f, Font.ITALIC)
            PdfWriter.getInstance(document, outputStream)
            document.open()
            val fechaActual =
                SimpleDateFormat("dd/MM/yyyy - HH:mm:ss", Locale.getDefault()).format(Date())
            val pendientes = items.count { !it.comprado }
            val comprados = items.count { it.comprado }
            document.add(Paragraph("--- Lista De La Compra ---\n\n", fuenteEncabezado))
            document.add(Paragraph("Fecha - Hora   →   $fechaActual", fuenteEncabezado))
            document.add(
                Paragraph(
                    "Total Productos En La Lista   →   ${items.size}", fuenteEncabezado
                )
            )
            document.add(Paragraph("\n"))
            document.add(
                Paragraph(
                    "    • PENDIENTES / POR COMPRAR   →   $pendientes", fuenteEncabezado
                )
            )
            document.add(Paragraph("    • HECHOS / COMPRADOS   →   $comprados", fuenteEncabezado))
            document.add(Paragraph("\n"))
            document.add(Paragraph("PENDIENTES / POR COMPRAR", fuenteEncabezado))
            document.add(Paragraph("\n"))
            for ((index, item) in items.withIndex()) {
                if (!item.comprado) {
                    val numero =
                        if (findViewById<CheckBox>(R.id.checkRomano).isChecked) toRoman(index + 1)
                        else "${index + 1}"
                    document.add(
                        Paragraph(
                            "☐ $numero   →   ${item.nombre}", fuenteItems
                        )
                    )
                }
            }
            document.add(Paragraph("\n"))
            document.add(Paragraph("HECHOS / COMPRADOS", fuenteEncabezado))
            document.add(Paragraph("\n"))
            for ((index, item) in items.withIndex()) {
                if (item.comprado) {
                    val numero =
                        if (findViewById<CheckBox>(R.id.checkRomano).isChecked) toRoman(index + 1)
                        else "${index + 1}"
                    document.add(
                        Paragraph(
                            "✔ $numero   →   ${item.nombre}", fuenteItemsItalic
                        )
                    )
                }
            }
            document.add(Paragraph("\n\n\n\n\n"))
            val azul = BaseColor.BLUE
            val fuenteBy = Font(baseFont, 24f, Font.BOLD, azul)
            val para = Paragraph(
                "By M8AX Corp. ${SimpleDateFormat("yyyy", Locale.getDefault()).format(Date())}",
                fuenteBy
            )
            para.alignment = Element.ALIGN_CENTER
            document.add(para)
            document.close()
            outputStream?.close()
            mostrarMensaje("P D F; Generado Correctamente En La Carpeta De Descargas.")
            uriToOpen?.let {
                val intent = Intent(Intent.ACTION_VIEW)
                intent.setDataAndType(it, "application/pdf")
                intent.flags =
                    Intent.FLAG_ACTIVITY_NO_HISTORY or Intent.FLAG_GRANT_READ_URI_PERMISSION
                startActivity(Intent.createChooser(intent, "Abrir PDF Con..."))
            }
        } catch (e: Exception) {
            e.printStackTrace()
            mostrarMensaje("Error Al Crear El Fichero P D F.")
        }
    }

    fun toRoman(num: Int): String {
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

    override fun onDestroy() {
        super.onDestroy()
        val mensajes = listOf(
            "Hasta Luego, ¡Que No Se Te Olvide Ningún Ingrediente!",
            "Nos Vemos, ¡A Llenar La Lista Con Sabrosas Compras!",
            "Adiós, Que Tu Nevera Esté Siempre Bien Surtida.",
            "Chao, ¡Que La Lista No Se Vuelva Un Caos!",
            "Hasta Pronto, ¡A Comprar Con Cabeza!",
            "Nos Vemos, ¡Que No Falte Nada En Tu Despensa!",
            "Adiós, ¡Compra Inteligente Y Sin Olvidos!",
            "Hasta Luego, ¡Que Tu Lista Siempre Se Cumpla Al 100%!",
            "Chao, ¡A Tachar Unidades Y A Disfrutar De La Cocina!",
            "Nos Vemos, ¡Tu Lista De La Compra Te Estará Esperando!"
        )
        mensaje = mensajes.random()
        if (items.isNotEmpty()) {
        } else {
            val despedidas = listOf(
                "Adiós; Hasta Pronto!", "Nos Vemos!", "Bye! Bye!", "Hasta La Próxima", "Chao! Chao!"
            )
            mensaje = despedidas.random()
        }
        if (ttsEnabled && mensaje.isNotEmpty()) {
            tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {}
                override fun onError(utteranceId: String?) {
                    mediaPlayer?.stop()
                    mediaPlayer?.release()
                    mediaPlayer = null
                    tts?.shutdown()
                    coroutineScope.cancel()
                }

                override fun onDone(utteranceId: String?) {
                    mediaPlayer?.stop()
                    mediaPlayer?.release()
                    mediaPlayer = null
                    tts?.shutdown()
                    coroutineScope.cancel()
                }
            })
            tts?.speak(mensaje, TextToSpeech.QUEUE_FLUSH, null, "tts_onDestroy")
        } else {
            mediaPlayer?.stop()
            mediaPlayer?.release()
            mediaPlayer = null
            tts?.shutdown()
            coroutineScope.cancel()
        }
    }
}