package com.mviiiax.m8ax_diariogimnasio

import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.telephony.PhoneStateListener
import android.telephony.TelephonyManager
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ImageView
import android.widget.ListView
import android.widget.ProgressBar
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.documentfile.provider.DocumentFile
import java.util.Locale
import kotlin.random.Random

class ReproductorActivity : ComponentActivity() {
    private lateinit var btnSeleccionarCarpeta: Button
    private lateinit var btnPlay: Button
    private lateinit var btnPausa: Button
    private lateinit var btnStop: Button
    private lateinit var btnSiguiente: Button
    private lateinit var btnAnterior: Button
    private lateinit var btnShuffle: Button
    private lateinit var btnAleatoria: Button
    private lateinit var tvCancionActual: TextView
    private lateinit var tvTiempo: TextView
    private lateinit var seekBar: SeekBar
    private lateinit var listViewCanciones: ListView
    private lateinit var ivCaratula: ImageView
    private lateinit var ondaView: OndaView
    private var listaCanciones: MutableList<Uri> = mutableListOf()
    private var indiceActual = 0
    private var mediaPlayer: MediaPlayer? = null
    private var shuffleEnabled = false
    private lateinit var tts: TextToSpeech
    private var ttsEnabled = false
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var progressCargando: ProgressBar
    private lateinit var tvDuracionTotal: TextView
    private var duracionTotalMs: Long = 0
    private var handlerApagado: Handler? = null
    private var runnableApagado: Runnable? = null
    private lateinit var btnProgramarApagado: Button
    private lateinit var adapterCanciones: ArrayAdapter<String>
    private var ttsReady = false
    private var wasPlayingBeforeCall = false
    var isPrepared = false
    private lateinit var telephonyManager: TelephonyManager
    private lateinit var phoneStateListener: PhoneStateListener

    companion object {
        private const val REQUEST_CODE_CARPETA = 101
        private const val PREFS_NAME = "M8AX-Config_TTS"
        private const val KEY_TTS = "tts_enabled"
    }

    private val extensionesAudio = listOf(
        "mp3",
        "wav",
        "ogg",
        "aac",
        "opus",
        "m4a",
        "flac",
        "mid",
        "xmf",
        "mxmf",
        "rtx",
        "ota",
        "imy",
        "3gp",
        "amr",
        "mp4",
        "avi",
        "mov",
        "mpg",
        "mkv",
        "webm",
        "m4v",
        "ogv",
        "wmv"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reproductor)
        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        ttsEnabled = prefs.getBoolean(KEY_TTS, false)
        tts = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.setLanguage(tts?.defaultLanguage ?: Locale.getDefault())
                tts?.setSpeechRate(0.9f)
                ttsReady = true
            }
        }
        telephonyManager = getSystemService(TELEPHONY_SERVICE) as TelephonyManager
        phoneStateListener = object : PhoneStateListener() {
            override fun onCallStateChanged(state: Int, phoneNumber: String?) {
                when (state) {
                    TelephonyManager.CALL_STATE_RINGING, TelephonyManager.CALL_STATE_OFFHOOK -> {
                        if (mediaPlayer?.isPlaying == true) {
                            pausar()
                            wasPlayingBeforeCall = true
                        } else {
                            wasPlayingBeforeCall = false
                        }
                    }

                    TelephonyManager.CALL_STATE_IDLE -> {
                        if (wasPlayingBeforeCall) {
                            continuar()
                            wasPlayingBeforeCall = false
                        }
                    }
                }
            }
        }
        if (checkSelfPermission(android.Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            if (shouldShowRequestPermissionRationale(android.Manifest.permission.READ_PHONE_STATE)) {
                if (ttsEnabled) reproducirTTS("Permiso Necesario")
                AlertDialog.Builder(this).setTitle("Permiso Necesario")
                    .setMessage("Necesitas Este Permiso Para Pausar La Música Automáticamente Cuando Recibas Una Llamada Y Reanudarla Cuando Termines.")
                    .setPositiveButton("Aceptar") { _, _ ->
                        requestPermissions(
                            arrayOf(android.Manifest.permission.READ_PHONE_STATE), 123
                        )
                    }.setNegativeButton("Cancelar") { dialog, _ ->
                        dialog.dismiss()
                    }.show()
            } else {
                requestPermissions(arrayOf(android.Manifest.permission.READ_PHONE_STATE), 123)
            }
        } else {
            telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_CALL_STATE)
        }
        btnSeleccionarCarpeta = findViewById(R.id.btnSeleccionarCarpeta)
        btnPlay = findViewById(R.id.btnPlay)
        btnPausa = findViewById(R.id.btnPausa)
        btnStop = findViewById(R.id.btnStop)
        btnSiguiente = findViewById(R.id.btnSiguiente)
        btnAnterior = findViewById(R.id.btnAnterior)
        btnShuffle = findViewById(R.id.btnShuffle)
        btnAleatoria = findViewById(R.id.btnAleatoria)
        tvCancionActual = findViewById(R.id.tvCancionActual)
        tvTiempo = findViewById(R.id.tvTiempo)
        seekBar = findViewById(R.id.seekBar)
        listViewCanciones = findViewById(R.id.listViewCanciones)
        ivCaratula = findViewById(R.id.ivCaratula)
        ondaView = findViewById(R.id.ondaView)
        progressCargando = findViewById(R.id.progressCargando)
        progressCargando.visibility = View.GONE
        tvDuracionTotal = findViewById(R.id.tvDuracionTotal)
        tvDuracionTotal.text = "Canciones - 0 | Duración Total - 0d 0h 0m 0s"
        btnProgramarApagado = findViewById(R.id.btnProgramarApagado)
        btnProgramarApagado.setOnClickListener { mostrarDialogoApagado() }
        adapterCanciones = object : ArrayAdapter<String>(
            this, android.R.layout.simple_list_item_1, mutableListOf()
        ) {
            override fun getView(
                position: Int, convertView: android.view.View?, parent: android.view.ViewGroup
            ): android.view.View {
                val view = super.getView(position, convertView, parent) as TextView
                view.setTextColor(android.graphics.Color.WHITE)
                return view
            }
        }
        listViewCanciones.adapter = adapterCanciones
        btnSeleccionarCarpeta.setOnClickListener {
            detener2()
            seleccionarCarpeta()
            if (ttsEnabled) reproducirTTS("Selecciona Carpeta De Música.")
        }
        btnPlay.setOnClickListener {
            if (mediaPlayer == null) {
                reproducirNuevaCancion()
                if (ttsEnabled) reproducirTTS("Play")
            } else {
                continuar()
                if (ttsEnabled) reproducirTTS("Continuar")
            }
        }
        btnPausa.setOnClickListener {
            pausar()
            if (ttsEnabled) reproducirTTS("Pausado")
        }
        btnStop.setOnClickListener {
            detener()
            if (ttsEnabled) reproducirTTS("Detenido")
        }
        btnSiguiente.setOnClickListener {
            if (listaCanciones.isNotEmpty()) {
                siguiente()
                if (ttsEnabled) reproducirTTS(
                    "Siguiente Canción: ${
                        obtenerNombreCancion(
                            listaCanciones[indiceActual]
                        )
                    }"
                )
            } else {
                Toast.makeText(this, "No Hay Canciones Para Reproducir", Toast.LENGTH_SHORT).show()
                if (ttsEnabled) reproducirTTS("No Hay Canciones Para Reproducir")
            }
        }
        btnAnterior.setOnClickListener {
            if (listaCanciones.isNotEmpty()) {
                anterior()
                if (ttsEnabled) reproducirTTS(
                    "Canción anterior: ${
                        obtenerNombreCancion(
                            listaCanciones[indiceActual]
                        )
                    }"
                )
            } else {
                Toast.makeText(this, "No Hay Canciones Para Reproducir", Toast.LENGTH_SHORT).show()
                if (ttsEnabled) reproducirTTS("No Hay Canciones Para Reproducir")
            }
        }
        btnShuffle.setOnClickListener {
            shuffleEnabled = !shuffleEnabled
            Toast.makeText(
                this, if (shuffleEnabled) "Shuffle ON" else "Shuffle OFF", Toast.LENGTH_SHORT
            ).show()
            if (ttsEnabled) reproducirTTS(if (shuffleEnabled) "Shuffle Activado" else "Shuffle Desactivado")
        }
        btnAleatoria.setOnClickListener {
            if (listaCanciones.isNotEmpty()) {
                reproducirAleatoria()
                if (ttsEnabled) reproducirTTS(
                    "Reproducción Aleatoria: ${
                        obtenerNombreCancion(
                            listaCanciones[indiceActual]
                        )
                    }"
                )
            } else {
                Toast.makeText(this, "No Hay Canciones Para Reproducir", Toast.LENGTH_SHORT).show()
                if (ttsEnabled) reproducirTTS("No Hay Canciones Para Reproducir")
            }
        }
        listViewCanciones.setOnItemClickListener { _, _, position, _ ->
            if (listaCanciones.isEmpty()) return@setOnItemClickListener
            indiceActual = position
            reproducirNuevaCancion()
        }
        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser && mediaPlayer != null && isPrepared) {
                    try {
                        mediaPlayer!!.seekTo(progress)
                    } catch (e: IllegalStateException) {
                        e.printStackTrace()
                    }
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
    }

    private fun continuar() {
        mediaPlayer?.let {
            try {
                if (!it.isPlaying) {
                    it.start()
                    actualizarSeekBar()
                    ondaView.iniciarAnimacion(it)
                }
            } catch (e: IllegalStateException) {
                e.printStackTrace()
            }
        }
    }

    private fun reproducirNuevaCancion() {
        if (listaCanciones.isEmpty()) return
        detener()
        isPrepared = false
        val archivoUri = listaCanciones[indiceActual]
        try {
            mediaPlayer = MediaPlayer().apply {
                setDataSource(this@ReproductorActivity, archivoUri)
                setOnPreparedListener {
                    isPrepared = true
                    start()
                    seekBar.max = it.duration
                    actualizarSeekBar()
                    mediaPlayer?.let { mp -> ondaView.iniciarAnimacion(mp) }
                }
                setOnCompletionListener { siguienteAutomatico() }
                prepareAsync()
            }
            val nombreCancion = obtenerNombreCancion(archivoUri)
            tvCancionActual.text = nombreCancion
            actualizarCaratula()
            if (ttsEnabled) reproducirTTS(nombreCancion)
        } catch (e: Exception) {
            e.printStackTrace()
            if (ttsEnabled) reproducirTTS("Error Al Reproducir La Canción")
            Toast.makeText(this, "Error Al Reproducir La Canción", Toast.LENGTH_SHORT).show()
        }
    }

    private fun mostrarDialogoApagado() {
        val opciones = (5..720 step 5).map { min ->
            val horas = min.toDouble() / 60
            String.format("%d Minutos | %.2f Horas", min, horas)
        }.toTypedArray()
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Programar Apagado")
        builder.setItems(opciones) { _, which ->
            val minutos = (which + 1) * 5
            programarApagadoEn(minutos)
        }
        builder.setNegativeButton("Cancelar") { dialog, _ ->
            dialog.dismiss()
            if (ttsEnabled) reproducirTTS("Cancelado")
        }
        builder.show()
    }

    private fun programarApagadoEn(minutos: Int) {
        handlerApagado?.removeCallbacksAndMessages(null)
        val millis = minutos * 60 * 1000L
        runnableApagado = Runnable {
            detener()
            Toast.makeText(this, "Apagado Completado", Toast.LENGTH_SHORT).show()
            if (ttsEnabled) reproducirTTS("Apagado Completado")
            finish()
        }
        handlerApagado = Handler(Looper.getMainLooper())
        handlerApagado?.postDelayed(runnableApagado!!, millis)
        Toast.makeText(this, "Apagado Programado En $minutos Minutos", Toast.LENGTH_SHORT).show()
        if (ttsEnabled) reproducirTTS("Apagado Programado En $minutos Minutos")
    }

    private fun reproducirAleatoria() {
        if (listaCanciones.isEmpty()) {
            Toast.makeText(this, "No Hay Canciones En La Lista", Toast.LENGTH_SHORT).show()
            return
        }
        if (listaCanciones.size <= 1) {
            indiceActual = 0
        } else {
            var i: Int
            do {
                i = Random.nextInt(listaCanciones.size)
            } while (i == indiceActual)
            indiceActual = i
        }
        reproducirNuevaCancion()
        Toast.makeText(
            this,
            "Reproducción Aleatoria: ${obtenerNombreCancion(listaCanciones[indiceActual])}",
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun seleccionarCarpeta() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
        startActivityForResult(intent, REQUEST_CODE_CARPETA)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_CARPETA && resultCode == RESULT_OK) {
            data?.data?.let { uri ->
                progressCargando.visibility = View.VISIBLE
                btnSeleccionarCarpeta.isEnabled = false
                Thread {
                    listaCanciones.clear()
                    adapterCanciones.clear()
                    duracionTotalMs = 0
                    recorrerCarpeta(uri)
                    runOnUiThread {
                        progressCargando.visibility = View.GONE
                        btnSeleccionarCarpeta.isEnabled = true
                    }
                }.start()
            }
        }
    }

    private fun recorrerCarpeta(uri: Uri) {
        val carpeta = DocumentFile.fromTreeUri(this, uri) ?: return
        recorrerDocumentFile(carpeta)
    }

    private fun recorrerDocumentFile(carpeta: DocumentFile) {
        val archivos = carpeta.listFiles()
        archivos.forEach { f ->
            if (f.isDirectory) {
                recorrerDocumentFile(f)
            } else if (f.name?.substringAfterLast(".")?.lowercase() in extensionesAudio) {
                var durMs = 0L
                var nombre = obtenerNombreCancion(f.uri)
                var añadir = true
                val mmr = MediaMetadataRetriever()
                try {
                    mmr.setDataSource(this, f.uri)
                    durMs = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                        ?.toLongOrNull() ?: 0L
                    if (durMs == 0L) añadir = false
                } catch (e: Exception) {
                    añadir = false
                } finally {
                    mmr.release()
                }
                if (añadir) {
                    listaCanciones.add(f.uri)
                    duracionTotalMs += durMs
                    runOnUiThread {
                        adapterCanciones.add(
                            "${listaCanciones.size} - $nombre - ${
                                formatearDuracion(
                                    durMs
                                )
                            }"
                        )
                        adapterCanciones.notifyDataSetChanged()
                        val totalSegundos = duracionTotalMs / 1000
                        val dias = totalSegundos / 86400
                        val horas = (totalSegundos % 86400) / 3600
                        val minutos = (totalSegundos % 3600) / 60
                        val segundos = totalSegundos % 60
                        tvDuracionTotal.text =
                            "Total Canciones - ${listaCanciones.size} | Duración Total - ${dias}d ${horas}h ${minutos}m ${segundos}s"
                    }
                }
            }
        }
    }

    private fun obtenerNombreCancion(uri: Uri): String {
        return DocumentFile.fromSingleUri(this, uri)?.name ?: uri.lastPathSegment ?: "Canción; "
    }

    private fun actualizarCaratula() {
        val archivoUri = listaCanciones[indiceActual]
        val mmr = MediaMetadataRetriever()
        try {
            mmr.setDataSource(this, archivoUri)
            val img = mmr.embeddedPicture
            if (img != null) ivCaratula.setImageBitmap(
                BitmapFactory.decodeByteArray(img, 0, img.size)
            )
            else ivCaratula.setImageResource(R.drawable.logom8ax)
        } catch (e: Exception) {
            ivCaratula.setImageResource(R.drawable.logom8ax)
        } finally {
            mmr.release()
        }
    }

    private fun actualizarSeekBar() {
        mediaPlayer?.let {
            try {
                seekBar.progress = it.currentPosition
                tvTiempo.text = "${formatTime(it.currentPosition)} / ${formatTime(it.duration)}"
                if (it.isPlaying) handler.postDelayed({ actualizarSeekBar() }, 100)
            } catch (e: Exception) {
            }
        } ?: run {
            seekBar.progress = 0
            tvTiempo.text = "00:00 / 00:00"
        }
    }

    private fun formatearDuracion(ms: Long): String {
        var totalSegundos = ms / 1000
        val dias = totalSegundos / 86400
        totalSegundos %= 86400
        val horas = totalSegundos / 3600
        totalSegundos %= 3600
        val minutos = totalSegundos / 60
        val segundos = totalSegundos % 60
        return "${dias}d ${horas}h ${minutos}m ${segundos}s"
    }

    private fun formatTime(ms: Int): String {
        val totalSeconds = ms / 1000
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        return if (hours > 0) String.format("%02d:%02d:%02d", hours, minutes, seconds)
        else String.format("%02d:%02d", minutes, seconds)
    }

    private fun siguienteAutomatico() {
        if (listaCanciones.isEmpty()) return
        indiceActual = if (shuffleEnabled && listaCanciones.size > 1) {
            var i: Int
            do {
                i = Random.nextInt(listaCanciones.size)
            } while (i == indiceActual)
            i
        } else {
            (indiceActual + 1) % listaCanciones.size
        }
        reproducirNuevaCancion()
    }

    private fun pausar() {
        try {
            mediaPlayer?.pause()
        } catch (e: IllegalStateException) {
            e.printStackTrace()
        }
        ondaView.detenerAnimacion()
    }

    private fun detener() {
        ondaView.detenerAnimacion()
        handler.removeCallbacksAndMessages(null)
        mediaPlayer?.let {
            try {
                if (it.isPlaying) it.stop()
            } catch (e: IllegalStateException) {
                e.printStackTrace()
            }
            it.release()
        }
        mediaPlayer = null
        seekBar.progress = 0
        tvTiempo.text = "00:00 / 00:00"
    }

    private fun detener2() {
        ondaView.detenerAnimacion()
        handler.removeCallbacksAndMessages(null)
        mediaPlayer?.let {
            try {
                if (it.isPlaying) it.stop()
            } catch (e: IllegalStateException) {
                e.printStackTrace()
            }
            try {
                it.release()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        mediaPlayer = null
        seekBar.progress = 0
        tvTiempo.text = "00:00 / 00:00"
    }

    private fun siguiente() {
        if (listaCanciones.isEmpty()) return
        siguienteAutomatico()
        if (ttsEnabled) reproducirTTS("Siguiente Canción: ${obtenerNombreCancion(listaCanciones[indiceActual])}")
    }

    private fun anterior() {
        if (listaCanciones.isEmpty()) return
        indiceActual = if (listaCanciones.size == 1) 0
        else if (indiceActual - 1 < 0) listaCanciones.size - 1
        else indiceActual - 1
        reproducirNuevaCancion()
        if (ttsEnabled) reproducirTTS("Canción Anterior: ${obtenerNombreCancion(listaCanciones[indiceActual])}")
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 123) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_CALL_STATE)
                if (ttsEnabled && ttsReady) reproducirTTS("Permiso Concedido.")
                Toast.makeText(this, "Permiso Concedido", Toast.LENGTH_SHORT).show()
            } else {
                if (ttsEnabled && ttsReady) reproducirTTS("Permiso Denegado; La Música No Se Pausará En Llamadas Entrantes.")
                Toast.makeText(
                    this,
                    "Permiso Denegado: La Música No Se Pausará En Llamadas Entrantes",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun reproducirTTS(texto: String) {
        if (ttsEnabled && ttsReady) {
            try {
                tts?.speak(texto, TextToSpeech.QUEUE_FLUSH, null, null)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer?.release()
        handlerApagado?.removeCallbacksAndMessages(null)
        tts?.shutdown()
        telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_NONE)
    }
}