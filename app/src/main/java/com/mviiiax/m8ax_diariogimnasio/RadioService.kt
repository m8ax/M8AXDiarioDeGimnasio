package com.mviiiax.m8ax_diariogimnasio

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Build
import android.os.IBinder
import android.widget.Toast
import androidx.core.app.NotificationCompat

class RadioService : Service() {
    companion object {
        const val ACTION_PLAY = "ACTION_PLAY"
        const val ACTION_PAUSE = "ACTION_PAUSE"
        const val ACTION_STOP = "ACTION_STOP"
        const val EXTRA_URL = "EXTRA_URL"
        const val CHANNEL_ID = "RadioChannel"
        const val NOTIF_ID = 1
    }

    private var mediaPlayer: MediaPlayer? = null
    private var currentUrl: String? = null
    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_PLAY -> {
                val url = intent.getStringExtra(EXTRA_URL)
                if (url != null) {
                    currentUrl = url
                    playRadio(url)
                } else {
                    resumeRadio()
                }
            }

            ACTION_PAUSE -> pauseRadio()
            ACTION_STOP -> stopRadio()
        }
        return START_STICKY
    }

    private fun playRadio(url: String) {
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
        mediaPlayer = MediaPlayer().apply {
            setAudioStreamType(AudioManager.STREAM_MUSIC)
            setDataSource(url)
            prepareAsync()
            setOnPreparedListener {
                start()
                startForeground(NOTIF_ID, buildNotification())
                Toast.makeText(this@RadioService, "Radio Iniciada", Toast.LENGTH_SHORT).show()
            }
            setOnErrorListener { _, _, _ ->
                Toast.makeText(this@RadioService, "Error Al Reproducir Radio", Toast.LENGTH_SHORT)
                    .show()
                true
            }
        }
    }

    private fun resumeRadio() {
        mediaPlayer?.let { player ->
            if (!player.isPlaying) {
                player.start()
                updateNotification()
                Toast.makeText(this, "Radio Reanudada", Toast.LENGTH_SHORT).show()
            }
        } ?: currentUrl?.let {
            playRadio(it)
        }
    }

    private fun pauseRadio() {
        mediaPlayer?.let { player ->
            if (player.isPlaying) {
                player.pause()
                updateNotification()
                Toast.makeText(this, "Radio En Pausa", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun stopRadio() {
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
        stopForeground(true)
        Toast.makeText(this, "Radio Detenida", Toast.LENGTH_SHORT).show()
    }

    private fun updateNotification() {
        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIF_ID, buildNotification())
    }

    private fun buildNotification(): Notification {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "No Hay Permiso Para Notificaciones", Toast.LENGTH_SHORT).show()
        }
        val playIntent = Intent(this, RadioService::class.java).apply { action = ACTION_PLAY }
        val pauseIntent = Intent(this, RadioService::class.java).apply { action = ACTION_PAUSE }
        val stopIntent = Intent(this, RadioService::class.java).apply { action = ACTION_STOP }
        val playPending = PendingIntent.getService(
            this, 0, playIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val pausePending = PendingIntent.getService(
            this, 1, pauseIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val stopPending = PendingIntent.getService(
            this, 2, stopIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val statusText = when {
            mediaPlayer?.isPlaying == true -> "... Reproduciendo ..."
            mediaPlayer != null -> "... En Pausa ..."
            else -> "... Detenido ..."
        }
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("M8AX \uD83C\uDFB5 RadioS OnlinE \uD83C\uDFB5 M8AX")
            .setContentText(statusText).setSmallIcon(android.R.drawable.ic_media_play)
            .setOngoing(mediaPlayer != null).addAction(0, "Play", playPending)
            .addAction(0, "Pausa", pausePending).addAction(0, "Stop", stopPending)
            .setStyle(NotificationCompat.DecoratedCustomViewStyle()).build()
    }

    override fun onDestroy() {
        super.onDestroy()
        stopRadio()
    }

    override fun onBind(intent: Intent?): IBinder? = null
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID, "Radio En Segundo Plano", NotificationManager.IMPORTANCE_LOW
            ).apply { description = "Reproducción De Radio" }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }
}