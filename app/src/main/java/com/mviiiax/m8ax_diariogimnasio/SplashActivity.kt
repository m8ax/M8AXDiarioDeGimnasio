package com.mviiiax.m8ax_diariogimnasio

import android.content.Intent
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.ImageView
import android.widget.VideoView
import androidx.appcompat.app.AppCompatActivity
import com.mviiiax.m8ax_diariogimnasio.ui.login.LoginActivity
import java.util.Calendar

class SplashActivity : AppCompatActivity() {
    private var mediaPlayer: MediaPlayer? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)
        val calendar = Calendar.getInstance()
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)
        val backgroundImage = findViewById<ImageView>(R.id.backgroundImage)
        val logoImage = findViewById<ImageView>(R.id.logoImage)
        val logoVideo = findViewById<VideoView>(R.id.logoVideo)
        val isSpecialDates =
            (month == Calendar.DECEMBER && day >= 20) || (month == Calendar.JANUARY && day <= 6)
        if (isSpecialDates) {
            val specialBackgrounds = arrayOf(R.drawable.m8axnavidad1, R.drawable.m8axnavidad2)
            backgroundImage.setImageResource(specialBackgrounds.random())
        } else if (hour in 7..19) {
            backgroundImage.setImageResource(R.drawable.m8axdia)
        } else {
            backgroundImage.setImageResource(R.drawable.m8axnoche)
        }
        backgroundImage.alpha = 0f
        backgroundImage.post { backgroundImage.animate().alpha(1f).setDuration(1000).start() }
        val imageLogos = arrayOf(
            R.drawable.logom8ax,
            R.drawable.logoapp,
            R.drawable.logom8ax2,
            R.drawable.logom8ax3,
            R.drawable.logom8ax4,
            R.drawable.logom8ax5
        )
        val videoLogos = arrayOf(
            R.raw.m8axvideo1,
            R.raw.m8axvideo2,
            R.raw.m8axvideo3,
            R.raw.m8axvideo4,
            R.raw.m8axvideo5,
            R.raw.m8axvideo6,
            R.raw.m8axvideo7,
            R.raw.m8axvideo8
        )
        if (Math.random() < 0.5) {
            val selectedImage = imageLogos.random()
            logoImage.visibility = View.VISIBLE
            logoVideo.visibility = View.GONE
            logoImage.setImageResource(selectedImage)
            logoImage.alpha = 0f
            logoImage.post { logoImage.animate().alpha(1f).setDuration(1000).start() }
        } else {
            val selectedVideo = videoLogos.random()
            logoImage.visibility = View.GONE
            logoVideo.visibility = View.VISIBLE
            val uri = Uri.parse("android.resource://$packageName/$selectedVideo")
            logoVideo.setVideoURI(uri)
            logoVideo.setOnPreparedListener { mp ->
                mp.isLooping = true
                logoVideo.alpha = 0f
                logoVideo.animate().alpha(1f).setDuration(1000).start()
                logoVideo.start()
            }
        }
        val sounds = arrayOf(
            R.raw.m8axinicio1,
            R.raw.m8axinicio2,
            R.raw.m8axinicio3,
            R.raw.m8axinicio4,
            R.raw.m8axinicio5,
            R.raw.m8axinicio6,
            R.raw.m8axinicio7,
            R.raw.m8axinicio8,
            R.raw.m8axinicio9,
            R.raw.m8axinicio10
        )
        mediaPlayer = MediaPlayer.create(this, sounds.random())
        mediaPlayer?.start()
        Handler(Looper.getMainLooper()).postDelayed({
            mediaPlayer?.release()
            if (logoVideo.isPlaying) logoVideo.stopPlayback()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }, 6000)
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer?.release()
        mediaPlayer = null
        val logoVideo = findViewById<VideoView>(R.id.logoVideo)
        if (logoVideo.isPlaying) logoVideo.stopPlayback()
    }
}