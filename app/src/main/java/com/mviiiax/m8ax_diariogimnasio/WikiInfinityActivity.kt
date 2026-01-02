package com.mviiiax.m8ax_diariogimnasio

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.content.pm.ActivityInfo
import android.media.MediaPlayer
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.view.View
import android.view.WindowManager
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity
import java.util.Locale
import kotlin.random.Random

class WikiInfinityActivity : AppCompatActivity(), TextToSpeech.OnInitListener {
    private lateinit var webView: WebView
    private lateinit var mediaPlayer: MediaPlayer
    private lateinit var tts: TextToSpeech
    private var ttsEnabled: Boolean = true
    private var isTtsReady = false
    private val handler = Handler(Looper.getMainLooper())
    private val cambioAutomaticoRunnable = object : Runnable {
        override fun run() {
            loadRandomWiki()
            handler.postDelayed(this, 10_000)
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!hayInternet()) {
            AlertDialog.Builder(this).setTitle("Sin Conexión A internet")
                .setMessage("Esta Aplicación Necesita Conexión A Internet Para Funcionar.")
                .setCancelable(false).setPositiveButton("Cerrar") { _, _ -> finish() }.show()
            return
        }
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        window.decorView.systemUiVisibility =
            (View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_LAYOUT_STABLE)
        webView = WebView(this)
        setContentView(webView)
        webView.settings.javaScriptEnabled = true
        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                extractAndSpeak()
            }
        }
        val prefs = getSharedPreferences("M8AX-Config_TTS", MODE_PRIVATE)
        ttsEnabled = prefs.getBoolean("tts_enabled", true)
        tts = TextToSpeech(this, this)
        mediaPlayer = MediaPlayer.create(this, R.raw.m8axsonidofondo)
        mediaPlayer.isLooping = true
        loadRandomWiki()
    }

    private fun hayInternet(): Boolean {
        val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val nc = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(nc) ?: return false
        return caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) || caps.hasTransport(
            NetworkCapabilities.TRANSPORT_CELLULAR
        )
    }

    private fun loadRandomWiki() {
        val rnd = Random.nextInt(1, 101)
        val url = when {
            rnd <= 60 -> "https://es.wikipedia.org/wiki/Special:Random"
            rnd <= 80 -> "https://es.wikipedia.org/wiki/Categoría:Informática"
            else -> "https://es.wikipedia.org/wiki/Categoría:Tecnología"
        }
        webView.loadUrl(url)
    }

    private fun extractAndSpeak() {
        webView.evaluateJavascript(
            """
            (function() {
                let content = document.getElementById('mw-content-text');
                if (!content) return "";
                return content.innerText;
            })();
            """.trimIndent()
        ) { raw ->
            val clean = raw.replace("\\n", "\n").replace("\"", "").trim()
            if (clean.length < 20) {
                loadRandomWiki()
                return@evaluateJavascript
            }
            if (ttsEnabled && isTtsReady) {
                speakText(clean)
                handler.removeCallbacks(cambioAutomaticoRunnable)
            } else {
                handler.removeCallbacks(cambioAutomaticoRunnable)
                handler.postDelayed(cambioAutomaticoRunnable, 10_000)
            }
        }
    }

    private fun speakText(text: String) {
        tts?.stop()
        val params = Bundle()
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, params, "wiki_read")
        Thread {
            while (tts.isSpeaking) {
                Thread.sleep(300)
            }
            handler.post { loadRandomWiki() }
        }.start()
    }

    override fun onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack()
        } else {
            super.onBackPressed()
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts?.setLanguage(tts?.defaultLanguage ?: Locale.getDefault())
            tts?.setSpeechRate(0.9f)
            isTtsReady = true
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
        handler.removeCallbacks(cambioAutomaticoRunnable)
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