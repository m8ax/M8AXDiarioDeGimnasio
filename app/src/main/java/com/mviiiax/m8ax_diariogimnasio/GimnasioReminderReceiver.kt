package com.mviiiax.m8ax_diariogimnasio

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import androidx.core.app.NotificationCompat
import java.util.Locale

class GimnasioReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val channelId = "gim_reminder_channel"
        val channelName = "Recordatorio Gimnasio"
        var tts: TextToSpeech? = null
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId, channelName, NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager.createNotificationChannel(channel)
        }
        val title = "¡ Hora De Entrenar !"
        val text =
            "No Te Olvides De Hacer Tu Sesión De Gimnasio Hoy, Ni Te Olvides De Apuntarla En El Diario... 💪"
        val frases = listOf(
            "Hora De Entrenar! No Te Olvides De Hacer Tu Sesión De Gimnasio Hoy; Ni Te Olvides De Apuntarla En El Diario...",
            "¡Vamos! Tu Gimnasio Te Está Esperando Y Tu Diario También… ¡No Hagas Que Se Enfaden!",
            "Levántate, Mueve Esos Músculos Y Registra Cada Gota De Sudor En Tu Diario.",
            "Hoy Toca Romper Límites Y Apuntarlo Todo: ¡El Diario Quiere Detalles!",
            "¡Al Ataque! Tu Sesión De Hoy No Se Va A Entrenar Sola, Y Tu Diario Merece Enterarse.",
            "No Dejes Que Tus Abdominales Se Aburran, ¡Y Tu Diario Tampoco!",
            "Cada Repetición Cuenta Y Cada Línea En Tu Diario También. ¡A Sudar Se Ha Dicho!",
            "El Gimnasio Llama Y Tu Diario Grita: ¡Anota Todo, Campeón!",
            "Hora De Transformar Esfuerzo En Resultados Y Resultados En Historias Para Tu Diario.",
            "Ponle Ritmo A Tus Músculos Y Chispa A Tu Diario. ¡Hoy Se Entrena Con Estilo!"
        )
        val text2 = frases.random()
        val launchIntent =
            context.packageManager.getLaunchIntentForPackage(context.packageName) ?: return
        launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            launchIntent!!,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val notification = NotificationCompat.Builder(context, channelId).setContentTitle(title)
            .setContentText(text).setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent).setAutoCancel(true).build()
        notificationManager.notify(1001, notification)
        val config = context.getSharedPreferences("M8AX-Config_TTS", Context.MODE_PRIVATE)
        val ttsEnabled = config.getBoolean("tts_enabled", true)
        if (ttsEnabled) {
            tts = TextToSpeech(context.applicationContext) { status ->
                if (status == TextToSpeech.SUCCESS) {
                    tts?.setLanguage(tts?.defaultLanguage ?: Locale.getDefault())
                    tts?.setSpeechRate(0.9f)
                    tts?.speak(text2, TextToSpeech.QUEUE_FLUSH, null, "notifTTS")
                    Handler(Looper.getMainLooper()).postDelayed({
                        tts?.stop()
                        tts?.shutdown()
                    }, 12000)
                }
            }
        }
    }
}