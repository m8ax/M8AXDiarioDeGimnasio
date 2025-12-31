package com.mviiiax.m8ax_diariogimnasio

import android.annotation.SuppressLint
import android.content.pm.ActivityInfo
import android.media.MediaPlayer
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.ArrayAdapter
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import java.util.Locale

class ChatGPTActivity : AppCompatActivity() {
    private lateinit var webView: WebView
    private lateinit var progressBar: ProgressBar
    private var mediaPlayer: MediaPlayer? = null

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val locale = Locale("es", "ES")
        Locale.setDefault(locale)
        val config = resources.configuration
        config.setLocale(locale)
        resources.updateConfiguration(config, resources.displayMetrics)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        setContentView(R.layout.activity_chat_gpt)
        progressBar = findViewById(R.id.progressBar)
        webView = findViewById(R.id.webView)
        mediaPlayer = MediaPlayer.create(this, R.raw.m8axsonidofondo)
        mediaPlayer?.isLooping = true
        mediaPlayer?.setVolume(0.5f, 0.5f)
        mediaPlayer?.start()
        val settings: WebSettings = webView.settings
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        settings.useWideViewPort = true
        settings.loadWithOverviewMode = true
        settings.builtInZoomControls = true
        settings.displayZoomControls = false
        webView.settings.userAgentString = webView.settings.userAgentString + " es-ES"
        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                progressBar.visibility = View.GONE
            }
        }
        webView.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                progressBar.progress = newProgress
                if (newProgress < 100) progressBar.visibility = View.VISIBLE
            }
        }
        mostrarMenuIA()
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK && webView.canGoBack()) {
            webView.goBack()
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    private fun mostrarMenuIA() {
        val nombres = arrayOf(
            "── CHATEA CON LA IA ──",
            "DuckDuckGo AI",
            "Microsoft Copilot",
            "ChatGPT (OpenAI)",
            "Grok (xAI)",
            "HuggingFace Chat",
            "DeepSeek",
            "You.com AI",
            "Perplexity AI",
            "Character.AI",
            "── CREA IMÁGENES CON LA IA ──",
            "DALL·E (OpenAI)",
            "MidJourney",
            "Stable Diffusion",
            "NightCafe",
            "Craiyon",
            "DeepAI Text to Image",
            "Runway ML",
            "Leonardo AI",
            "── CREA CANCIONES CON LA IA ──",
            "Suno AI",
            "SoundDraw",
            "AIVA AI"
        )
        val urls = arrayOf(
            "",
            "https://duckduckgo.com/?q=DuckDuckGo+AI+Chat&ia=chat&duckai=1",
            "https://copilot.microsoft.com/",
            "https://chatgpt.com/",
            "https://grok.com/",
            "https://huggingface.co/chat",
            "https://chat.deepseek.com/",
            "https://you.com/?chatMode=default",
            "https://www.perplexity.ai/",
            "https://character.ai/",
            "",
            "https://openai.com/dall-e",
            "https://www.midjourney.com/home",
            "https://stability.ai/stable-image",
            "https://creator.nightcafe.studio/",
            "https://www.craiyon.com/",
            "https://deepai.org/machine-learning-model/text2img",
            "https://runwayml.com/",
            "https://leonardo.ai/",
            "",
            "https://suno.com/",
            "https://soundraw.io/",
            "https://www.aiva.ai/"
        )
        val adapter =
            object : ArrayAdapter<String>(this, android.R.layout.select_dialog_item, nombres) {
                override fun isEnabled(position: Int): Boolean {
                    return urls[position].isNotEmpty()
                }

                override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                    val view = super.getView(position, convertView, parent)
                    val textView = view.findViewById<TextView>(android.R.id.text1)
                    textView.textSize = 16f
                    return view
                }
            }
        val dialog = AlertDialog.Builder(this).setTitle("Elige Asistente O Generador De IA")
            .setAdapter(adapter) { _, index ->
                cargarIA(urls[index])
            }.setCancelable(true).create()
        dialog.setOnCancelListener {
            if (webView.url.isNullOrEmpty()) finish()
        }
        dialog.show()
    }

    private fun cargarIA(url: String) {
        progressBar.visibility = View.VISIBLE
        webView.loadUrl(url)
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
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
        webView.destroy()
        super.onDestroy()
    }
}