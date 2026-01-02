package com.mviiiax.m8ax_diariogimnasio

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Matrix
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.common.BitMatrix
import com.google.zxing.integration.android.IntentIntegrator
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class VerticalCaptureActivity : com.journeyapps.barcodescanner.CaptureActivity()
class CrearQrActivity : AppCompatActivity() {
    private lateinit var etTexto: EditText
    private lateinit var btnGenerarLogon: Button
    private lateinit var btnGenerarLogo: Button
    private lateinit var btnGenerar: Button
    private lateinit var btnGrabar: Button
    private lateinit var btnEscanearQR: Button
    private lateinit var ivQR: ImageView
    private var bitmapQR: Bitmap? = null
    private var bitmapGuardar: Bitmap? = null
    private var logoUsuario: Bitmap? = null
    private lateinit var tvContador: TextView
    private val QR_MAX_CHAR = 1250
    private var mediaPlayer: MediaPlayer? = null
    private var tts: TextToSpeech? = null
    private var ttsEnabled = false
    private var cualLogo: Int = 1
    private fun startQRScanner() {
        val integrator = IntentIntegrator(this)
        if (ttsEnabled) {
            tts?.speak(
                "Apunta Al Código Con La Cámara.", TextToSpeech.QUEUE_FLUSH, null, "ttsFlexionesId"
            )
        }
        integrator.setCaptureActivity(VerticalCaptureActivity::class.java)
        integrator.setDesiredBarcodeFormats(IntentIntegrator.QR_CODE)
        integrator.setPrompt("--- Apunta Al Código QR, Para Escanearlo ---")
        integrator.setCameraId(0)
        integrator.setBeepEnabled(true)
        integrator.setBarcodeImageEnabled(false)
        integrator.initiateScan()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_qr)
        etTexto = findViewById(R.id.etTexto)
        btnGenerarLogon = findViewById(R.id.btnGenerarLogon)
        btnGenerarLogo = findViewById(R.id.btnGenerarLogo)
        btnGenerar = findViewById(R.id.btnGenerar)
        btnGrabar = findViewById(R.id.btnGrabar)
        ivQR = findViewById(R.id.ivQR)
        val prefs = getSharedPreferences("M8AX-Config_TTS", Context.MODE_PRIVATE)
        ttsEnabled = prefs.getBoolean("tts_enabled", false)
        etTexto.filters = arrayOf(android.text.InputFilter.LengthFilter(QR_MAX_CHAR))
        btnGenerarLogon.setOnClickListener {
            ivQR.setImageBitmap(null)
            val texto = etTexto.text.toString()
            if (texto.isEmpty()) {
                if (ttsEnabled) {
                    tts?.speak(
                        "Habrá Que Escribir Algo Primero No Crees? Digo Yo Vamos...",
                        TextToSpeech.QUEUE_FLUSH,
                        null,
                        "ttsFlexionesId"
                    )
                }
                Toast.makeText(
                    this, "Escribe Primero Un Texto Para Generar El QR", Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }
            cualLogo = 1
            generarQR(texto, false)
        }
        btnGenerarLogo.setOnClickListener {
            ivQR.setImageBitmap(null)
            val texto = etTexto.text.toString()
            if (texto.isEmpty()) {
                if (ttsEnabled) {
                    tts?.speak(
                        "Habrá Que Escribir Algo Primero No Crees? Digo Yo Vamos...",
                        TextToSpeech.QUEUE_FLUSH,
                        null,
                        "ttsFlexionesId"
                    )
                }
                Toast.makeText(
                    this, "Escribe Primero Un Texto Para Generar El QR", Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }
            cualLogo = 2
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
            intent.addCategory(Intent.CATEGORY_OPENABLE)
            intent.type = "image/*"
            startActivityForResult(intent, 100)
        }
        btnGenerar.setOnClickListener {
            ivQR.setImageBitmap(null)
            val texto = etTexto.text.toString()
            if (texto.isEmpty()) {
                if (ttsEnabled) {
                    tts?.speak(
                        "Habrá Que Escribir Algo Primero No Crees? Digo Yo Vamos...",
                        TextToSpeech.QUEUE_FLUSH,
                        null,
                        "ttsFlexionesId"
                    )
                }
                Toast.makeText(
                    this, "Escribe Primero Un Texto Para Generar El QR", Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }
            cualLogo = 3
            val logo = BitmapFactory.decodeResource(resources, R.drawable.logom8ax)
            generarQR(texto, false, logo)
        }
        btnEscanearQR = findViewById(R.id.btnEscanearQR)
        btnEscanearQR.setOnClickListener {
            startQRScanner()
        }
        btnGrabar.setOnClickListener {
            val texto = etTexto.text.toString()
            if (texto.isEmpty()) {
                if (ttsEnabled) {
                    tts?.speak(
                        "Habrá Que Escribir Algo Primero Para Guardarlo No Crees? Digo Yo Vamos...",
                        TextToSpeech.QUEUE_FLUSH,
                        null,
                        "ttsFlexionesId"
                    )
                }
                Toast.makeText(
                    this, "Escribe Primero Un Texto Para Guardar El QR", Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }
            when (cualLogo) {
                1 -> generarQR(texto, true)
                2 -> {
                    if (logoUsuario != null) {
                        generarQR(texto, true, logoUsuario)
                    } else {
                        if (ttsEnabled) {
                            tts?.speak(
                                "Selecciona Un Logo Primero...",
                                TextToSpeech.QUEUE_FLUSH,
                                null,
                                "ttsFlexionesId"
                            )
                        }
                        Toast.makeText(
                            this, "Selecciona Un Logo Primero", Toast.LENGTH_SHORT
                        ).show()
                    }
                }

                3 -> {
                    val logo = BitmapFactory.decodeResource(resources, R.drawable.logom8ax)
                    generarQR(texto, true, logo)
                }
            }
        }
        mediaPlayer = MediaPlayer.create(this, R.raw.m8axsonidofondo)
        mediaPlayer?.isLooping = true
        tts = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.setLanguage(tts?.defaultLanguage ?: Locale.getDefault())
                tts?.setSpeechRate(0.9f)
            }
        }
        tvContador = findViewById(R.id.tvContador)
        etTexto.filters = arrayOf(android.text.InputFilter.LengthFilter(QR_MAX_CHAR))
        tvContador.text = "0/$QR_MAX_CHAR"
        etTexto.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val longitud = s?.length ?: 0
                tvContador.text = "$longitud/$QR_MAX_CHAR"
            }

            override fun afterTextChanged(s: android.text.Editable?) {}
        })
        val rootLayout = findViewById<LinearLayout>(R.id.rootLayout)
        rootLayout.setOnClickListener {
            if (ttsEnabled) {
                tts?.stop()
            }
        }
    }

    private fun generarQR(texto: String, grabar: Boolean, logo: Bitmap? = null) {
        if (ttsEnabled) {
            tts?.speak(
                "Código Q R; Generado.", TextToSpeech.QUEUE_ADD, null, "ttsId"
            )
        }
        try {
            val hints = HashMap<EncodeHintType, Any>().apply {
                put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.H)
                put(EncodeHintType.CHARACTER_SET, "UTF-8")
                put(EncodeHintType.MARGIN, 1)
            }
            val writer = QRCodeWriter()
            val matrixPantalla: BitMatrix =
                writer.encode(texto, BarcodeFormat.QR_CODE, 512, 512, hints)
            var qrBitmapPantalla = bitMatrixToBitmap(matrixPantalla)
            if (logo != null) qrBitmapPantalla = overlayLogo(qrBitmapPantalla, logo)
            bitmapQR = qrBitmapPantalla
            ivQR.setImageBitmap(bitmapQR)
            if (grabar) {
                val matrixAlta: BitMatrix =
                    writer.encode(texto, BarcodeFormat.QR_CODE, 2048, 2048, hints)
                var highResBitmap = bitMatrixToBitmap(matrixAlta)
                if (logo != null) highResBitmap = overlayLogo(highResBitmap, logo)
                bitmapGuardar = highResBitmap
                guardarQR(highResBitmap)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun bitMatrixToBitmap(matrix: BitMatrix): Bitmap {
        val width = matrix.width
        val height = matrix.height
        val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val pixels = IntArray(width * height)
        val black = -0x1000000
        val white = -0x1
        var offset = 0
        for (y in 0 until height) {
            for (x in 0 until width) {
                pixels[offset++] = if (matrix.get(x, y)) black else white
            }
        }
        bmp.setPixels(pixels, 0, width, 0, 0, width, height)
        return bmp
    }

    private fun overlayLogo(qrBitmap: Bitmap, logoBitmap: Bitmap): Bitmap {
        val scaleFactor = qrBitmap.width * 0.2f / logoBitmap.width
        val matrix = Matrix()
        matrix.postScale(scaleFactor, scaleFactor)
        matrix.postTranslate(
            (qrBitmap.width - logoBitmap.width * scaleFactor) / 2f,
            (qrBitmap.height - logoBitmap.height * scaleFactor) / 2f
        )
        val combined = Bitmap.createBitmap(qrBitmap.width, qrBitmap.height, qrBitmap.config)
        val canvas = Canvas(combined)
        canvas.drawBitmap(qrBitmap, 0f, 0f, null)
        canvas.drawBitmap(logoBitmap, matrix, null)
        return combined
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 100 && resultCode == Activity.RESULT_OK) {
            data?.data?.let { uri ->
                contentResolver.openInputStream(uri)?.use { input ->
                    logoUsuario = BitmapFactory.decodeStream(input)
                    Toast.makeText(this, "Logo Seleccionado Correctamente", Toast.LENGTH_SHORT)
                        .show()
                    if (ttsEnabled) {
                        tts?.speak(
                            "Logo Seleccionado.", TextToSpeech.QUEUE_FLUSH, null, "ttsFlexionesId"
                        )
                    }
                }
                val texto = etTexto.text.toString()
                if (texto.isNotEmpty() && logoUsuario != null) {
                    generarQR(texto, false, logoUsuario)
                }
            }
        }
        if (requestCode == 1 && resultCode == Activity.RESULT_OK) {
            data?.data?.let { uri: Uri ->
                bitmapGuardar?.let { bmp ->
                    contentResolver.openOutputStream(uri)?.use { out: OutputStream ->
                        bmp.compress(Bitmap.CompressFormat.PNG, 100, out)
                    }
                    if (ttsEnabled) {
                        tts?.speak(
                            "Código Q R; Guardado.", TextToSpeech.QUEUE_FLUSH, null, "ttsId"
                        )
                    }
                }
            }
        }
        val result = com.google.zxing.integration.android.IntentIntegrator.parseActivityResult(
            requestCode, resultCode, data
        )
        if (result != null) {
            val contenido = result.contents
            if (contenido != null) {
                etTexto.setText(contenido)
                generarQR(contenido, false, null)
                if (ttsEnabled) {
                    tts?.speak(
                        "Texto Detectado; $contenido", TextToSpeech.QUEUE_FLUSH, null, "ttsQRId"
                    )
                }
            } else {
                if (ttsEnabled) {
                    tts?.speak(
                        "No Veo Ninggún Codigo Q R.", TextToSpeech.QUEUE_FLUSH, null, "ttsQRId"
                    )
                }
                Toast.makeText(this, "No Se Detectó Ningún Q R", Toast.LENGTH_SHORT).show()
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    override fun onResume() {
        super.onResume(); mediaPlayer?.start()
    }

    override fun onPause() {
        super.onPause(); mediaPlayer?.pause()
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer?.stop()
        tts?.stop()
        tts?.shutdown()
        mediaPlayer?.release()
        mediaPlayer = null
    }

    private fun guardarQR(bitmap: Bitmap) {
        val sdf = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
        val fechaHora = sdf.format(Date())
        val nombreArchivo = "M8AX-QR_$fechaHora.png"
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT)
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        intent.type = "image/png"
        intent.putExtra(Intent.EXTRA_TITLE, nombreArchivo)
        startActivityForResult(intent, 1)
        if (ttsEnabled) tts?.speak(
            "Elige Donde Guardar Tu Obra Maestra.", TextToSpeech.QUEUE_FLUSH, null, "ttsId"
        )
    }
}