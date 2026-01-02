package com.mviiiax.m8ax_diariogimnasio

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.media.MediaPlayer
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.view.View
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.json.JSONObject
import java.io.IOException
import java.util.Locale

class ChessActivity : AppCompatActivity() {
    private lateinit var webView: WebView
    private lateinit var tvStarWars: TextView
    private var mediaPlayer: MediaPlayer? = null
    private val handler = Handler(Looper.getMainLooper())
    private val client = OkHttpClient()
    private var ttsEnabled = false
    private var tts: TextToSpeech? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chess)
        webView = findViewById(R.id.webViewChess)
        tvStarWars = findViewById(R.id.tvStarWars)
        val btnNewGame = findViewById<Button>(R.id.btnNewGame)
        webView.settings.javaScriptEnabled = true
        webView.webViewClient = WebViewClient()
        webView.loadUrl("file:///android_asset/m8axchess/m8axchess.html")
        mediaPlayer = MediaPlayer.create(this, R.raw.m8axsonidofondo)
        mediaPlayer?.isLooping = true
        mediaPlayer?.start()
        val config = getSharedPreferences("M8AX-Config_TTS", MODE_PRIVATE)
        ttsEnabled = config.getBoolean("tts_enabled", false)
        tts = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.setLanguage(tts?.defaultLanguage ?: Locale.getDefault())
                tts?.setSpeechRate(0.9f)
            }
        }
        fetchRandomQuote()
        startFetchingFrases()
        btnNewGame.setOnClickListener {
            webView.reload()
            mediaPlayer?.stop()
            mediaPlayer?.release()
            mediaPlayer = MediaPlayer.create(this, R.raw.m8axsonidofondo)
            mediaPlayer?.isLooping = true
            mediaPlayer?.start()
            fetchRandomQuote()
        }
    }

    private fun capitalizarCadaPalabra(texto: String): String {
        return texto.lowercase().split(" ").joinToString(" ") { palabra ->
            if (palabra.isNotEmpty()) palabra.replaceFirstChar { it.uppercase() }
            else palabra
        }
    }

    private fun startFetchingFrases() {
        handler.post(object : Runnable {
            override fun run() {
                fetchRandomQuote()
                handler.postDelayed(this, 60_000)
            }
        })
    }

    private fun fetchRandomQuote() {
        val request =
            Request.Builder().url("https://quotes-api-three.vercel.app/api/randomquote?language=es")
                .build()
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    showStarWarsText("¡ Disfruta Tu Partida De Ajedrez !")
                }
            }

            override fun onResponse(call: Call, response: Response) {
                response.body?.string()?.let { bodyStr ->
                    try {
                        val json = JSONObject(bodyStr)
                        val quoteText = capitalizarCadaPalabra(json.getString("quote"))
                        val author = capitalizarCadaPalabra(json.getString("author"))
                        val displayText = "$quoteText\n\n$author"
                        runOnUiThread {
                            showStarWarsText(displayText)
                            if (ttsEnabled) {
                                tts?.speak(
                                    displayText, TextToSpeech.QUEUE_FLUSH, null, "ttsQuoteId"
                                )
                            }
                        }
                    } catch (e: Exception) {
                        runOnUiThread {
                            showStarWarsText("¡ Disfruta Tu Partida De Ajedrez !")
                        }
                    }
                }
            }
        })
    }

    private fun showStarWarsText(text: String) {
        tvStarWars.text = text
        tvStarWars.textSize = 24f
        tvStarWars.post {
            val parentHeight = (tvStarWars.parent as View).height.toFloat()
            val startY = parentHeight
            val endY = 0f
            tvStarWars.translationY = startY
            tvStarWars.scaleX = 1f
            tvStarWars.scaleY = 1f
            tvStarWars.rotationX = 0f
            val translateAnim = ObjectAnimator.ofFloat(tvStarWars, "translationY", startY, endY)
            val rotateXAnim = ObjectAnimator.ofFloat(tvStarWars, "rotationX", 0f, 20f)
            val animatorSet = AnimatorSet()
            animatorSet.playTogether(translateAnim, rotateXAnim)
            animatorSet.duration = 5000
            animatorSet.start()
        }
    }

    override fun onPause() {
        super.onPause()
        mediaPlayer?.pause()
    }

    override fun onResume() {
        super.onResume()
        mediaPlayer?.start()
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer?.release()
        mediaPlayer = null
        handler.removeCallbacksAndMessages(null)
        tts?.stop()
        tts?.shutdown()
        tts = null
    }
}