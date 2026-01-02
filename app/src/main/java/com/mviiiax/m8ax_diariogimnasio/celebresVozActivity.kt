package com.mviiiax.m8ax_diariogimnasio

import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.ActivityInfo
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.json.JSONObject
import java.io.IOException
import java.util.Locale
import kotlin.concurrent.thread

class CelebresVozActivity : AppCompatActivity(), TextToSpeech.OnInitListener {
    private lateinit var ivFondo: ImageView
    private lateinit var ivFondoAnterior: ImageView
    private lateinit var tvNoticia: TextView
    private lateinit var btnGuardarFoto: Button
    private lateinit var mediaPlayer: MediaPlayer
    private lateinit var tts: TextToSpeech
    private var ttsEnabled = true
    private var isTtsReady = false
    private val handler = Handler(Looper.getMainLooper())
    private var bitmapActual: Bitmap? = null
    private val REQ_GUARDAR_IMAGEN = 7777
    private val client = OkHttpClient()
    private val cambioAutomatico = object : Runnable {
        override fun run() {
            if (!isFinishing && !isDestroyed) {
                fetchRandomQuote()
            }
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        window.decorView.systemUiVisibility =
            (View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_LAYOUT_STABLE)
        setContentView(R.layout.activity_news_reader)
        ivFondo = findViewById(R.id.ivFondo)
        ivFondoAnterior = ImageView(this).apply {
            layoutParams = ivFondo.layoutParams
            scaleType = ImageView.ScaleType.CENTER_CROP
        }
        (ivFondo.parent as View).let { parent ->
            if (parent is ViewGroup) parent.addView(ivFondoAnterior, 0)
        }
        tvNoticia = findViewById(R.id.tvNoticia)
        btnGuardarFoto = findViewById(R.id.btnGuardarFoto)
        val prefs = getSharedPreferences("M8AX-Config_TTS", MODE_PRIVATE)
        ttsEnabled = prefs.getBoolean("tts_enabled", true)
        mediaPlayer = MediaPlayer.create(this, R.raw.m8axsonidofondo)
        mediaPlayer.isLooping = true
        tts = TextToSpeech(this, this)
        btnGuardarFoto.setOnClickListener { seleccionarDondeGuardar() }
        mostrarImagenConTransicion()
        fetchRandomQuote()
    }

    private fun capitalizarCadaPalabra(texto: String): String {
        return texto.split(" ").joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }
    }

    private fun fetchRandomQuote() {
        val request =
            Request.Builder().url("https://quotes-api-three.vercel.app/api/randomquote?language=es")
                .build()
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    mostrarNoticiaConTTS("¡Estoy Esperando A Ver Si Tengo Internet!", "")
                }
                handler.postDelayed({ fetchRandomQuote() }, 15_000)
            }

            override fun onResponse(call: Call, response: Response) {
                response.body?.string()?.let { bodyStr ->
                    try {
                        val json = JSONObject(bodyStr)
                        val quoteText = capitalizarCadaPalabra(json.getString("quote"))
                        val author = capitalizarCadaPalabra(json.getString("author"))
                        runOnUiThread {
                            mostrarNoticiaConTTS(quoteText, author)
                        }
                    } catch (_: Exception) {
                        runOnUiThread {
                            mostrarNoticiaConTTS("¡Estoy Esperando A Ver Si Tengo Internet!", "")
                        }
                        handler.postDelayed({ fetchRandomQuote() }, 15_000)
                    }
                }
            }
        })
    }

    private fun mostrarNoticiaConTTS(quote: String, author: String) {
        val displayText: String = if (author.isNotEmpty()) "$quote\n\n$author" else quote
        animarTexto(displayText)
        if (ttsEnabled && isTtsReady) {
            tts?.stop()
            tts?.speak(displayText, TextToSpeech.QUEUE_FLUSH, null, "ttsQuoteId")
            thread {
                while (tts.isSpeaking) Thread.sleep(300)
                Thread.sleep(5000)
                handler.post {
                    if (!isFinishing && !isDestroyed) {
                        mostrarImagenConTransicion()
                        handler.postDelayed(cambioAutomatico, 1_000)
                    }
                }
            }
        } else {
            handler.removeCallbacks(cambioAutomatico)
            handler.postDelayed(cambioAutomatico, 10_000)
        }
    }

    private fun animarTexto(texto: String) {
        val fadeOut = AlphaAnimation(1f, 0f).apply { duration = 300 }
        val fadeIn = AlphaAnimation(0f, 1f).apply { duration = 300 }
        fadeOut.setAnimationListener(object : Animation.AnimationListener {
            override fun onAnimationStart(animation: Animation?) {}
            override fun onAnimationEnd(animation: Animation?) {
                tvNoticia.text = texto
                tvNoticia.startAnimation(fadeIn)
            }

            override fun onAnimationRepeat(animation: Animation?) {}
        })
        tvNoticia.startAnimation(fadeOut)
    }

    private fun mostrarImagenConTransicion() {
        if (isFinishing || isDestroyed) return
        ivFondoAnterior.setImageDrawable(ivFondo.drawable)
        val imagenUrl = "https://picsum.photos/1080/1920?random=${System.currentTimeMillis()}"
        Glide.with(this).asBitmap().load(imagenUrl).centerCrop()
            .into(object : CustomTarget<Bitmap>() {
                override fun onResourceReady(
                    resource: Bitmap, transition: Transition<in Bitmap>?
                ) {
                    if (isFinishing || isDestroyed) return
                    bitmapActual = resource.copy(
                        resource.config ?: Bitmap.Config.ARGB_8888, true
                    )
                    ivFondo.setImageBitmap(resource)
                    val fade = AlphaAnimation(0f, 1f).apply { duration = 600 }
                    ivFondo.startAnimation(fade)
                }

                override fun onLoadCleared(placeholder: Drawable?) {}
            })
    }

    private fun seleccionarDondeGuardar() {
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "image/jpeg"
            putExtra(Intent.EXTRA_TITLE, "M8AX - Imagen - ${System.currentTimeMillis()}.JpG")
        }
        startActivityForResult(intent, REQ_GUARDAR_IMAGEN)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQ_GUARDAR_IMAGEN && resultCode == RESULT_OK) {
            val uri: Uri = data?.data ?: return
            guardarImagen(uri)
        }
    }

    private fun guardarImagen(uri: Uri) {
        thread {
            try {
                val bitmap = bitmapActual ?: throw Exception("No Hay Imagen Para Guardar")
                contentResolver.openOutputStream(uri)?.use { out ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out)
                }
                runOnUiThread {
                    Toast.makeText(this, "Imagen Guardada Correctamente.", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                runOnUiThread {
                    Toast.makeText(
                        this, "Error Al Guardar La Imagen - ${e.message}", Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts?.setLanguage(tts?.defaultLanguage ?: Locale.getDefault())
            tts?.setSpeechRate(0.9f)
            isTtsReady = true
            if (!ttsEnabled) {
                tts?.speak(
                    "Recuerda Tener El T T S Activado En La App De Gimnasio, Para Lectura De Frases Célebres Por Voz.",
                    TextToSpeech.QUEUE_FLUSH,
                    null,
                    "ttsWikiId"
                )
            } else {
                tts?.speak("Comencemos;", TextToSpeech.QUEUE_FLUSH, null, "ttsComencemosId")
            }
            Thread.sleep(2000)
        }
    }

    override fun onResume() {
        super.onResume()
        try {
            if (!mediaPlayer.isPlaying) mediaPlayer.start()
        } catch (_: Exception) {
        }
    }

    override fun onPause() {
        super.onPause()
        try {
            if (mediaPlayer.isPlaying) mediaPlayer.pause()
        } catch (_: Exception) {
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(cambioAutomatico)
        try {
            Glide.with(this).clear(ivFondo)
        } catch (_: Exception) {
        }
        try {
            mediaPlayer.stop(); mediaPlayer.release()
        } catch (_: Exception) {
        }
        try {
            tts?.stop(); tts?.shutdown()
        } catch (_: Exception) {
        }
    }
}