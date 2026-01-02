package com.mviiiax.m8ax_diariogimnasio

import android.content.SharedPreferences
import android.graphics.Color
import android.media.MediaPlayer
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity
import java.util.Locale

class M8axGimActivity : AppCompatActivity(), TextToSpeech.OnInitListener {
    private lateinit var mediaPlayer: MediaPlayer
    private lateinit var tts: TextToSpeech
    private var ttsEnabled: Boolean = false
    private lateinit var webView: WebView
    private lateinit var prefs: SharedPreferences
    override fun onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack()
        } else {
            super.onBackPressed()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_m8ax_gim)
        prefs = getSharedPreferences("M8AX-Config_TTS", MODE_PRIVATE)
        ttsEnabled = prefs.getBoolean("tts_enabled", false)
        tts = TextToSpeech(this, this)
        if (ttsEnabled) {
            mediaPlayer = MediaPlayer.create(this, R.raw.m8axgimweb)
        } else {
            mediaPlayer = MediaPlayer.create(this, R.raw.m8axsonidofondo)
        }
        mediaPlayer.isLooping = true
        mediaPlayer.start()
        webView = findViewById(R.id.webView)
        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true
        webView.settings.builtInZoomControls = true
        webView.settings.displayZoomControls = false
        webView.setBackgroundColor(Color.TRANSPARENT)
        webView.webViewClient = WebViewClient()
        webView.loadUrl("file:///android_asset/m8axgimweb/m8axgimweb.html")
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts?.setLanguage(tts?.defaultLanguage ?: Locale.getDefault())
            tts?.setSpeechRate(0.9f)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer.stop()
        mediaPlayer.release()
        tts?.stop()
        tts?.shutdown()
    }
}