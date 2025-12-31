package com.mviiiax.m8ax_diariogimnasio

import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.ActivityInfo
import android.graphics.Color
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.ToneGenerator
import android.net.TrafficStats
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.util.Calendar
import kotlin.concurrent.fixedRateTimer

class CalendarioAnualActivity : AppCompatActivity() {
    private lateinit var webView: WebView
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var mediaPlayer: MediaPlayer
    private val toneGenerator = ToneGenerator(AudioManager.STREAM_ALARM, 30)
    private lateinit var kbTextView: TextView
    private lateinit var donationTextView: TextView
    private lateinit var youtubeTextView: TextView
    private var previousTxBytes: Long = 0
    private var previousRxBytes: Long = 0
    private var totalKb: Long = 0
    private var toneGeneratorReleased = false
    private var beepTimer: java.util.Timer? = null

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        window.decorView.systemUiVisibility =
            (View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)
        actionBar?.hide()
        val layout = FrameLayout(this)
        setContentView(layout)
        webView = WebView(this)
        layout.addView(
            webView, FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT
            )
        )
        donationTextView = TextView(this)
        donationTextView.setTextColor(android.graphics.Color.YELLOW)
        donationTextView.textSize = 12f
        donationTextView.text = "Donación Voluntaria"
        donationTextView.setOnClickListener {
            val browserIntent = Intent(
                Intent.ACTION_VIEW, android.net.Uri.parse("https://www.paypal.com/paypalme/m8ax")
            )
            startActivity(browserIntent)
        }
        val donationParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT
        )
        donationParams.gravity = Gravity.TOP or Gravity.START
        donationParams.setMargins(16, 95, 0, 0)
        layout.addView(donationTextView, donationParams)
        youtubeTextView = TextView(this)
        youtubeTextView.setTextColor(android.graphics.Color.RED)
        youtubeTextView.textSize = 12f
        youtubeTextView.text = "M8AX Canal De Youtube"
        youtubeTextView.setOnClickListener {
            val browserIntent = Intent(
                Intent.ACTION_VIEW, android.net.Uri.parse("https://youtube.com/m8ax")
            )
            startActivity(browserIntent)
        }
        val youtubeParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT
        )
        youtubeParams.gravity = Gravity.TOP or Gravity.END
        youtubeParams.setMargins(0, 95, 16, 0)
        layout.addView(youtubeTextView, youtubeParams)
        kbTextView = TextView(this)
        kbTextView.setTextColor(Color.parseColor("#40E0D0"))
        kbTextView.textSize = 12f
        val kbParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT
        )
        kbParams.gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
        kbParams.setMargins(20, 95, 0, 0)
        layout.addView(kbTextView, kbParams)
        updateKbText(0)
        window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        activarModoInmersivo()
        val settings = webView.settings
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        settings.cacheMode = WebSettings.LOAD_NO_CACHE
        settings.setSupportZoom(true)
        settings.builtInZoomControls = true
        settings.displayZoomControls = false
        settings.useWideViewPort = true
        settings.loadWithOverviewMode = true
        settings.defaultZoom = WebSettings.ZoomDensity.FAR
        settings.textZoom = 100
        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(
                view: WebView?, request: WebResourceRequest?
            ): Boolean {
                request?.url?.let { url ->
                    if (!url.toString().startsWith("file:///android_asset/")) {
                        startActivity(Intent(Intent.ACTION_VIEW, url))
                        return true
                    }
                }
                return false
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                applyInitialZoom()
            }
        }
        webView.webChromeClient = WebChromeClient()
        webView.setOnTouchListener { _, event -> event.pointerCount > 1 }
        webView.loadUrl("file:///android_asset/m8ax1.html")
        handler.postDelayed(object : Runnable {
            override fun run() {
                applyInitialZoom()
                handler.postDelayed(this, 1500)
            }
        }, 1500)
        mediaPlayer = MediaPlayer.create(this, R.raw.m8axsonidofondo)
        mediaPlayer.isLooping = true
        mediaPlayer.setVolume(0.3f, 0.3f)
        mediaPlayer.start()
        setupHourlyAndHalfBeep()
        previousTxBytes = TrafficStats.getUidTxBytes(android.os.Process.myUid())
        previousRxBytes = TrafficStats.getUidRxBytes(android.os.Process.myUid())
        fixedRateTimer("dataUsageTimer", initialDelay = 1000, period = 1000) {
            val currentTx = TrafficStats.getUidTxBytes(android.os.Process.myUid())
            val currentRx = TrafficStats.getUidRxBytes(android.os.Process.myUid())
            val delta = (currentTx - previousTxBytes) + (currentRx - previousRxBytes)
            if (delta > 0) {
                totalKb += delta / 1024
                runOnUiThread { updateKbText(totalKb) }
            }
            previousTxBytes = currentTx
            previousRxBytes = currentRx
        }
    }

    private fun applyInitialZoom() {
        webView.post {
            while (webView.canZoomOut()) {
                webView.zoomOut()
            }
            webView.settings.setSupportZoom(false)
            webView.settings.builtInZoomControls = false
        }
    }

    private fun setupHourlyAndHalfBeep() {
        beepTimer = fixedRateTimer("timerBeep", initialDelay = getInitialDelay(), period = 60_000) {
            val calendar = Calendar.getInstance()
            val minutes = calendar.get(Calendar.MINUTE)
            runOnUiThread {
                when (minutes) {
                    0 -> {
                        playBeepSafe(100)
                        handler.postDelayed({ playBeepSafe(100) }, 125)
                    }

                    30 -> playBeepSafe(50)
                }
            }
        }
    }

    private fun playBeepSafe(durationMs: Int) {
        try {
            if (!toneGeneratorReleased) {
                toneGenerator.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, durationMs)
            }
        } catch (_: Exception) {
        }
    }

    private fun getInitialDelay(): Long {
        val now = Calendar.getInstance()
        return (60 - now.get(Calendar.SECOND)) * 1000L
    }

    private fun updateKbText(kb: Long) {
        val mb = kb.toDouble() / 1024
        val mbText = String.format("%.3f", mb)
        kbTextView.text = "   Tráfico De App - $mbText MB"
    }

    override fun onPause() {
        super.onPause()
        if (this::mediaPlayer.isInitialized && mediaPlayer.isPlaying) mediaPlayer.pause()
    }

    override fun onResume() {
        super.onResume()
        if (this::mediaPlayer.isInitialized) mediaPlayer.start()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (this::mediaPlayer.isInitialized) mediaPlayer.release()
        toneGenerator.release()
        toneGeneratorReleased = true
        beepTimer?.cancel()
    }

    private fun activarModoInmersivo() {
        window.decorView.systemUiVisibility =
            (View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_LAYOUT_STABLE)
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            activarModoInmersivo()
        }
    }
}