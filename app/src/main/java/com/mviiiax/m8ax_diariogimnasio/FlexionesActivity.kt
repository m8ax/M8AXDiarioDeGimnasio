package com.mviiiax.m8ax_diariogimnasio

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.media.MediaPlayer
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import java.util.Date
import java.util.Locale

class FlexionesActivity : AppCompatActivity(), SensorEventListener {
    private lateinit var txtContador: TextView
    private lateinit var txtMensaje: TextView
    private lateinit var sensorManager: SensorManager
    private var proximitySensor: Sensor? = null
    private var flexiones = 0
    private var ultimoEstadoCercania = false
    var tts: TextToSpeech? = null
    var ttsEnabled = true
    private var hablandoFraseAnimo = false
    private var mediaPlayer: MediaPlayer? = null
    private lateinit var txtUltimaSesion: TextView
    private val PREFS_NAME = "M8AX-Flexiones"
    private val KEY_ULTIMAS_FLEXIONES = "M8AX-Ultimas-Flexiones"
    private val KEY_FECHA = "M8AX-Fecha-UF"
    private val KEY_HORA = "M8AX-Hora-UF"
    private val frasesAnimo = listOf(
        "¡Vamos Campeón, Que Se Puede!",
        "¡Sigue Así, Fuerza Bruta!",
        "¡No Te Rindas, Vas Genial!",
        "¡Cada Flexión Cuenta, Sigue!",
        "¡Tú Puedes, Mente De Hierro!",
        "¡Fuerza Y Valor, Vamos Allá!",
        "¡Esto Se Nota, Músculos Al Poder!",
        "¡No Pares, Eres Una Máquina!",
        "¡Hasta El Infinito Y Más Allá!",
        "¡Que La Fuerza Te Acompañe!",
        "¡No Hay Atajos Para Los Fuertes!",
        "¡Demuestra De Qué Estás Hecho!",
        "¡Cada Gota De Sudor Cuenta!",
        "¡El Dolor Es Temporal, El Orgullo Eterno!",
        "¡Cree En Ti, Eres Imparable!",
        "¡Levántate Y Sigue!",
        "¡Hazlo Por Ti, Hazlo Por Los Tuyos!",
        "¡Eres Más Fuerte De Lo Que Crees!",
        "¡Suda Ahora, Sonríe Después!",
        "¡El Sacrificio Trae La Gloria!",
        "¡Sigue, Que Cada Rep Cuenta!",
        "¡No Pares Hasta Que Estés Orgulloso!",
        "¡El Poder Está En Tu Mente!",
        "¡Con Cada Flexión Te Superas!",
        "¡Rompe Tus Límites!",
        "¡Nunca Te Rindas!",
        "¡El Éxito Es De Los Valientes!",
        "¡Hoy Es El Día Para Ser Mejor!",
        "¡Tú Controlas Tu Destino!",
        "¡No Hay Excusas!",
        "¡Siente El Fuego, Sé La Máquina!",
        "¡Cada Repetición Te Hace Leyenda!",
        "¡Avanza, Aunque Duela!",
        "¡La Victoria Es Para Los Constantes!",
        "¡Eres Una Bestia En Acción!",
        "¡El Esfuerzo Nunca Traiciona!",
        "¡Cambia Tu Cuerpo, Cambia Tu Mente!",
        "¡Demuestra Tu Verdadero Poder!",
        "¡No Hay Límites, Solo Retos!",
        "¡Cada Caída Te Hace Más Fuerte!",
        "¡El Sudor Es Tu Medalla!",
        "¡Poder, Fuerza, Corazón!",
        "¡Siente Cada Músculo Trabajar!",
        "¡Sé Imparable, Sé Leyenda!",
        "¡Nada Te Detiene!",
        "¡El Dolor Es Debilidad Saliendo Del Cuerpo!",
        "¡Despierta Al Guerrero Que Llevas Dentro!",
        "¡Eres Invencible!",
        "¡Que Nada Te Detenga!",
        "¡Sigue Empujando Hasta El Final!"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_flexiones)
        mediaPlayer = MediaPlayer.create(this, R.raw.m8axsonidofondo)
        mediaPlayer?.isLooping = true
        mediaPlayer?.setVolume(0.5f, 0.5f)
        mediaPlayer?.start()
        window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        txtContador = findViewById(R.id.txtContadorFlexiones)
        txtMensaje = findViewById(R.id.txtMensajeFlexiones)
        txtUltimaSesion = findViewById(R.id.txtUltimaSesion)
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val ultimasFlexiones = prefs.getInt(KEY_ULTIMAS_FLEXIONES, 0)
        val fechaUltima = prefs.getString(KEY_FECHA, "--/--/----")
        val horaUltima = prefs.getString(KEY_HORA, "--:--:--")
        txtUltimaSesion.text =
            "Última Sesión - $ultimasFlexiones Flexiones\nFecha - $fechaUltima\nHora - $horaUltima"
        ttsEnabled = intent.getBooleanExtra("ttsEnabled", true)
        tts = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale.getDefault()
                tts?.setSpeechRate(0.9f)
            }
        }
        tts?.setOnUtteranceProgressListener(object :
            android.speech.tts.UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                if (utteranceId?.startsWith("animo_") == true) {
                    hablandoFraseAnimo = true
                }
            }

            override fun onDone(utteranceId: String?) {
                if (utteranceId?.startsWith("animo_") == true) {
                    hablandoFraseAnimo = false
                }
            }

            override fun onError(utteranceId: String?) {
                if (utteranceId?.startsWith("animo_") == true) {
                    hablandoFraseAnimo = false
                }
            }
        })
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        proximitySensor = sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY)
        proximitySensor?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
        }
    }

    override fun onSensorChanged(event: SensorEvent?) {
        event?.let {
            val cerca = it.values[0] < (proximitySensor?.maximumRange ?: 5f)
            if (cerca && !ultimoEstadoCercania) {
                flexiones++
                txtContador.text = "$flexiones"
                val layout = findViewById<ConstraintLayout>(R.id.layoutPrincipal)
                when {
                    flexiones <= 20 -> layout.setBackgroundColor(0xFFFFFFFF.toInt())
                    flexiones in 21..50 -> layout.setBackgroundColor(0xFFA8E6A1.toInt())
                    flexiones in 51..100 -> layout.setBackgroundColor(0xFFFFF59D.toInt())
                    else -> layout.setBackgroundColor(0xFFFFCDD2.toInt())
                }
                txtMensaje.text = when {
                    flexiones <= 20 -> "Aún No Has Hecho Suficientes Flexiones"
                    flexiones in 21..50 -> "Vas Muy Bien, Sigue Así"
                    flexiones in 51..100 -> "¡ Estás Pasándote Un Poco !"
                    else -> "¡Cuidado! Ya Has Hecho Muchas"
                }
                if (ttsEnabled && !hablandoFraseAnimo) {
                    tts?.speak("$flexiones", TextToSpeech.QUEUE_FLUSH, null, "flexion_$flexiones")
                }
                if (flexiones % 10 == 0 && ttsEnabled) {
                    val frase = frasesAnimo.random()
                    hablandoFraseAnimo = true
                    tts?.speak(frase, TextToSpeech.QUEUE_FLUSH, null, "animo_$flexiones")
                }
            }
            ultimoEstadoCercania = cerca
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
    }

    override fun onDestroy() {
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val editor = prefs.edit()
        editor.putInt(KEY_ULTIMAS_FLEXIONES, flexiones)
        val fecha = java.text.SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())
        val hora = java.text.SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
        editor.putString(KEY_FECHA, fecha)
        editor.putString(KEY_HORA, hora)
        editor.apply()
        sensorManager.unregisterListener(this)
        tts?.stop()
        tts?.shutdown()
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
        super.onDestroy()
    }

    override fun onPause() {
        super.onPause()
        mediaPlayer?.pause()
    }

    override fun onResume() {
        super.onResume()
        mediaPlayer?.start()
    }
}