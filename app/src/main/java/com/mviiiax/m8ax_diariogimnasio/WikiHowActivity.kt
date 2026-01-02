package com.mviiiax.m8ax_diariogimnasio

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.pm.ActivityInfo
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.view.View
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
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.net.URL
import java.util.Locale
import kotlin.concurrent.thread

class WikiHowActivity : AppCompatActivity(), TextToSpeech.OnInitListener {
    private lateinit var ivFondo: ImageView
    private lateinit var ivFondoAnterior: ImageView
    private lateinit var tvNoticia: TextView
    private lateinit var btnGuardarFoto: Button
    private lateinit var mediaPlayer: MediaPlayer
    private lateinit var tts: TextToSpeech
    private var ttsEnabled = true
    private var isTtsReady = false
    private val handler = Handler(Looper.getMainLooper())
    private val noticias = mutableListOf<String>()
    private var noticiasMezcladas = mutableListOf<String>()
    private var noticiaIndex = 0
    private var imagenActualUrl: String = ""
    private var bitmapActual: android.graphics.Bitmap? = null
    private val REQ_GUARDAR_IMAGEN = 7777
    private val cambioAutomatico = object : Runnable {
        override fun run() {
            if (!isFinishing && !isDestroyed) {
                cargarSiguienteNoticia()
                handler.postDelayed(this, 10_000)
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
            if (parent is android.view.ViewGroup) parent.addView(ivFondoAnterior, 0)
        }
        tvNoticia = findViewById(R.id.tvNoticia)
        btnGuardarFoto = findViewById(R.id.btnGuardarFoto)
        val prefs = getSharedPreferences("M8AX-Config_TTS", MODE_PRIVATE)
        ttsEnabled = prefs.getBoolean("tts_enabled", true)
        mediaPlayer = MediaPlayer.create(this, R.raw.m8axsonidofondo)
        mediaPlayer.isLooping = true
        tts = TextToSpeech(this, this)
        btnGuardarFoto.setOnClickListener {
            seleccionarDondeGuardar()
        }
        cargarNoticias()
    }

    private fun limpiarHtml(texto: String): String {
        return texto.replace(Regex("<.*?>"), "").replace("&lt;", "<").replace("&gt;", ">")
            .replace("&amp;", "&").replace("&quot;", "\"").replace("&apos;", "'").trim()
    }

    private fun cargarNoticias() {
        val rssUrls = listOf(
            "https://www.xataka.com/rss",
            "https://feeds.elpais.com/mrss-s/pages/ep/site/elpais.com/portada",
            "https://www.20minutos.es/rss/",
            "https://www.muyinteresante.es/rss",
            "https://www.marca.com/rss/",
            "https://www.lavanguardia.com/mvc/feed/rss/home",
            "https://www.abc.es/rss/feeds/abcPortada.xml",
            "https://www.elmundo.es/rss/espana.xml",
            "https://www.genbeta.com/rss.xml",
            "https://www.hipertextual.com/rss"
        )
        thread {
            var algunaCargada = false
            noticias.clear()
            rssUrls.forEach { url ->
                try {
                    val connection = URL(url).openConnection()
                    val input = connection.getInputStream()
                    val factory = XmlPullParserFactory.newInstance()
                    val parser = factory.newPullParser()
                    parser.setInput(input, null)
                    var event = parser.eventType
                    var insideItem = false
                    var title = ""
                    var description = ""
                    while (event != XmlPullParser.END_DOCUMENT) {
                        val name = parser.name
                        when (event) {
                            XmlPullParser.START_TAG -> {
                                if (name.equals("item", true)) insideItem = true
                                if (insideItem) {
                                    if (name.equals("title", true)) title = parser.nextText()
                                    if (name.equals("description", true)) description =
                                        parser.nextText()
                                }
                            }

                            XmlPullParser.END_TAG -> {
                                if (name.equals("item", true) && insideItem) {
                                    noticias.add(limpiarHtml("$title\n$description"))
                                    insideItem = false
                                }
                            }
                        }
                        event = parser.next()
                    }
                    input.close()
                    if (noticias.isNotEmpty()) algunaCargada = true
                } catch (_: Exception) {
                }
            }
            handler.post {
                if (algunaCargada) {
                    noticiasMezcladas = noticias.shuffled().toMutableList()
                    noticiaIndex = 0
                    cargarSiguienteNoticia()
                } else {
                    tvNoticia.text = "... No Se Pudieron Cargar Noticias. Reintentando ..."
                    handler.postDelayed({ cargarNoticias() }, 15_000)
                }
            }
        }
    }

    private fun cargarSiguienteNoticia() {
        if (isFinishing || isDestroyed) return
        if (noticiasMezcladas.isEmpty()) return
        val noticia = noticiasMezcladas[noticiaIndex]
        noticiaIndex++
        if (noticiaIndex >= noticiasMezcladas.size) {
            noticiasMezcladas = noticias.shuffled().toMutableList()
            noticiaIndex = 0
        }
        animarTexto(noticia)
        mostrarImagenConTransicion()
        if (ttsEnabled && isTtsReady) {
            tts?.stop()
            tts?.speak(noticia, TextToSpeech.QUEUE_FLUSH, null, "news")
            thread {
                while (tts.isSpeaking) Thread.sleep(300)
                handler.post {
                    if (!isFinishing && !isDestroyed) cargarSiguienteNoticia()
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
        ivFondoAnterior.setImageDrawable(ivFondo.drawable)
        imagenActualUrl = "https://picsum.photos/1080/1920?random=${System.currentTimeMillis()}"
        Glide.with(this).asBitmap().load(imagenActualUrl).centerCrop()
            .into(object : CustomTarget<android.graphics.Bitmap>() {
                override fun onResourceReady(
                    resource: android.graphics.Bitmap,
                    transition: Transition<in android.graphics.Bitmap>?
                ) {
                    bitmapActual = resource.copy(
                        resource.config ?: android.graphics.Bitmap.Config.ARGB_8888, true
                    )
                    ivFondo.setImageBitmap(resource)
                    val fade = AlphaAnimation(0f, 1f).apply { duration = 600 }
                    ivFondo.startAnimation(fade)
                }

                override fun onLoadCleared(placeholder: android.graphics.drawable.Drawable?) {}
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
        if (requestCode == REQ_GUARDAR_IMAGEN && resultCode == Activity.RESULT_OK) {
            val uri: Uri = data?.data ?: return
            guardarImagen(uri)
        }
    }

    private fun guardarImagen(uri: Uri) {
        thread {
            try {
                val bitmap = bitmapActual ?: throw Exception("No Hay Imagen Para Guardar")
                contentResolver.openOutputStream(uri)?.use { out ->
                    bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 100, out)
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
                val textoWiki =
                    "Recuerda Tener El T T S Activado En La App De Gimnasio, Para Lectura De Noticias Por Voz."
                tts?.speak(textoWiki, TextToSpeech.QUEUE_FLUSH, null, "ttsWikiId")
            } else {
                tts?.speak("Comencemos;", TextToSpeech.QUEUE_FLUSH, null, "ttsComencemosId")
            }
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