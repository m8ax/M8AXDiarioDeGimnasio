package com.mviiiax.m8ax_diariogimnasio

import android.content.Intent
import android.graphics.SurfaceTexture
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Surface
import android.view.TextureView
import android.view.View
import android.view.animation.AccelerateInterpolator
import android.view.animation.BounceInterpolator
import android.view.animation.DecelerateInterpolator
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.mviiiax.m8ax_diariogimnasio.ui.login.LoginActivity
import java.util.Calendar

class SplashActivity : AppCompatActivity() {
    private var mediaPlayer: MediaPlayer? = null
    private var videoPlayer: MediaPlayer? = null
    private val splashHandler = Handler(Looper.getMainLooper())
    private var runnableFinal: Runnable? = null
    private lateinit var logoVideo: TextureView
    private var isFinishingSplash = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)
        val rootLayout = findViewById<RelativeLayout>(R.id.rootLayout)
        val backgroundImage = findViewById<ImageView>(R.id.backgroundImage)
        val logoImage = findViewById<ImageView>(R.id.logoImage)
        val txtMensajeSplash = findViewById<TextView>(R.id.txtMensajeSplash)
        logoVideo = findViewById(R.id.logoVideo)
        val calendar = Calendar.getInstance()
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)
        val isSpecialDates =
            (month == Calendar.DECEMBER && day >= 20) || (month == Calendar.JANUARY && day <= 6)
        if (isSpecialDates) {
            backgroundImage.setImageResource(
                arrayOf(
                    R.drawable.m8axnavidad1,
                    R.drawable.m8axnavidad2,
                    R.drawable.m8axnavidad3,
                    R.drawable.m8axnavidad4
                ).random()
            )
            val mensajesNavideños = arrayOf(
                "Entrena Hoy Para Quemar\nEl Turrón De Mañana",
                "Papá Noel No Regala Músculos\nSe Ganan En El Gym",
                "Cambiando Los Polvorones\nPor Repeticiones De Hierro",
                "Que Tus Ganas De Entrenar\nSean Más Grandes Que El Roscón",
                "En Esta Navidad, Mi Regalo\nFavorito Es Un Nuevo Récord",
                "Ni El Grinch Podrá Con\nMis Ganas De Entrenar",
                "Forjando Un Cuerpo De Acero\nBajo Las Luces De Navidad",
                "Los Renos Corren\nYo Levanto Hierro Puro",
                "Esta Navidad El Único Gordo\nSerá Mi Bíceps",
                "Que Tu Disciplina Brille Más\nQue El Árbol De Navidad",
                "Navidad Ganadora\nSudor, Esfuerzo Y Victoria",
                "El Turrón Es Temporal\nEl Músculo Es Para Siempre",
                "¡ Dale Más Fuerte ! Los Excesos\nSe Sudan En El Rack",
                "Cenando Como Rey\nEntrenando Como Bestia Navideña",
                "Papá Noel Te Vigila:\n¡ No Te Saltes El Día De Piernas !",
                "Que Los Reyes Magos Te Traigan\nFuerza Inquebrantable",
                "Entrenando En Navidad\nPara Lucir Como Un Guerrero",
                "Menos Campanas Y Más\nDiscos De Veinte Kilos",
                "Mi Árbol De Navidad Tiene\nForma De Mi Jaula De Potencia",
                "Brindamos Por Los Logros\nDe Hoy Y Los De Mañana",
                "Paz, Amor Y Muchos\nKilogramos En La Barra",
                "Cero Excusas Navideñas\nCien Por Cien Algoritmo M8AX",
                "Ni La Nieve Detiene A Un\nCorazón Guerrero",
                "Regálate Salud, Regálate\nUn Entrenamiento Épico",
                "¡ Feliz Navidad ! Que El Hierro\nTe Acompañe Siempre",
                "Cambiando El Carbón Por\nPura Energía En El Gym",
                "¿ Turrón ? Solo Si Después\nHay Sentadillas Pesadas",
                "Navidades Brutales, Son Donde\nSe Crean Los Campeones",
                "Que El Año Nuevo Te Encuentre\nSiendo Tu Mejor Versión",
                "¡ Fuerza Y Honor !\nTambién En Estas Fiestas",
                "¡ Feliz Navidad ! Que Tu Fuerza\nCrezca Como El Espíritu Navideño",
                "Brilla En El Gym Tanto Como\nLas Luces De Tu Árbol",
                "Levanta Hierro Con La Alegría\nDe Una Mañana De Reyes",
                "El Mejor Adorno De Navidad\nEs Tu Propia Constancia",
                "Que La Magia De La Navidad\nTe Dé Energía Explosiva",
                "Campanas Y Pesas\nEl Ritmo De Tu Navidad",
                "¡ Vamos ! Convierte Esas Cenas\nEn Entrenamientos Legendarios",
                "La Navidad Es Dulce\nPero Tu Voluntad Es Más Fuerte",
                "Risas, Abrazos Y Series\nAl Límite En El Rack",
                "Que Tu Motivación Sea El Faro\nDe Estas Fiestas",
                "Regala Sonrisas Y Comparte\nTu Pasión Por El Hierro",
                "Navidades Llenas De Luz\nSudor Y Sueños Cumplidos",
                "Siente La Magia Del Gimnasio\nEn Cada Repetición",
                "Entrena Con El Corazón Contento\nY La Mente En La Victoria",
                "Que Santa Te Traiga Mucha\nFuerza Y Felicidad",
                "Brindis Por La Salud\nY Por Un Cuerpo Imbatible",
                "Cada Ejercicio Es Un Regalo\nQue Te Haces A Ti Mismo",
                "Navidad Es Compartir Esfuerzo\nCon Tu Tribu Del Gym",
                "Que El Espíritu Navideño\nGuíe Tus Pasos Al Éxito",
                "Disfruta, Entrena Y Sé\nFeliz En Esta Navidad"
            )
            txtMensajeSplash.text = "\n${mensajesNavideños.random()}"
        } else {
            if (hour in 7..19) {
                backgroundImage.setImageResource(
                    arrayOf(
                        R.drawable.m8axdia, R.drawable.m8axdia2, R.drawable.m8axdia3
                    ).random()
                )
            } else {
                backgroundImage.setImageResource(
                    arrayOf(
                        R.drawable.m8axnoche, R.drawable.m8axnoche2, R.drawable.m8axnoche3
                    ).random()
                )
            }
            val mensajesMotivadores = arrayOf(
                "Tu Cuerpo Es Tu Templo, Cuídalo",
                "Entrena Como Una Bestia",
                "El Éxito Se Gana Con Sudor",
                "Sin Excusas Solo Resultados Reales",
                "Hoy Es El Día De Superarte",
                "Forjando Músculos De Hierro Puro",
                "La Disciplina Vence Al Talento",
                "No Pares Hasta Estar Orgulloso",
                "Tu Único Límite Eres Tú Mismo",
                "Levántate Y Domina El Gimnasio",
                "Cada Repetición Cuenta Para Ganar",
                "Transforma El Dolor En Fuerza",
                "Mente De Acero Cuerpo De Guerrero",
                "El Sudor Es Grasa Llorando",
                "Enfocado En Tu Mejor Versión",
                "Construye Tu Legado Paso A Paso",
                "La Constancia Crea Campeones Hoy",
                "Rompe Tus Barreras Mentales Ahora",
                "Fuerza Bruta En Cada Entrenamiento",
                "Superando Límites Con Energía",
                "M8AX - The Algorithm Man - M8AX",
                "Siente El Poder En Tus Manos",
                "Mañana Agradecerás Haber Empezado Hoy",
                "Tu Voluntad Es Tu Mayor Activo",
                "Conviértete En Una Máquina Imparable",
                "La Victoria Ama La Preparación",
                "Cero Excusas Cien Por Cien Actitud",
                "Esculpe Tu Destino Futuro",
                "Persigue Tus Metas Sin Descanso",
                "Bienvenido Al Equipo De Los Fuertes",
                "Domina Tus Pesos Domina Tu Vida",
                "La Motivación Te Arranca\nEl Hábito Te Sigue",
                "Sangre Sudor Y Gloria",
                "Haz Que Tu Cuerpo Sea Tu Obra Maestra",
                "Ningún Entrenamiento Es Mal Entrenamiento",
                "Hoy Vas A Ser Mejor Que Ayer",
                "El Hierro Nunca Te Miente",
                "Crea Una Mentalidad Inquebrantable",
                "Sé Fuerte Cuando Seas Débil",
                "Tu Progreso Es Tu Mayor Recompensa",
                "Entrena Duro En Silencio\nDeja Que Tu Éxito Grite",
                "Lucha Por Tus Sueños Cada Día",
                "El Futuro No Está Establecido\nSolo Existe El Que Nosotros Hacemos...",
                "No Es Un Pasatiempo Es Un Estilo De Vida",
                "Vence A Tu Miedo Supera Tu Record",
                "El Dolor Es Temporal La Gloria Es Eterna",
                "Cuerpo Fuerte Mente Poderosa",
                "Tu Esfuerzo De Hoy Es Tu Fuerza De Mañana",
                "En La Constancia Está El Secreto",
                "Ríndete O Sigue Adelante Tú Eliges",
                "Por Muchas Vueltas Que Demos\nSiempre Tendremos El Culo Atrás",
                "El Miedo Es La Gasolina\nDe Los Valientes",
                "Tus Músculos No Saben Contar\nSolo Sienten El Esfuerzo",
                "Si Fuera Fácil\nTodo El Mundo Lo Haría",
                "La Diferencia Entre Querer\nY Poder Es La Disciplina",
                "Tu Única Competencia\nEs La Persona En El Espejo",
                "No Desees Un Buen Cuerpo\nTrabaja Para Construirlo",
                "El Dolor De Hoy\nEs La Victoria De Mañana",
                "Para Ser El Número Uno\nEntrena Como El Número Dos",
                "Un Entrenamiento De Una Hora\nEs Solo El 4% De Tu Día",
                "No Busques Tiempo\nCrea El Tiempo Para Entrenar",
                "La Meta Es Ser Mejor\nDe Lo Que Fuiste Ayer",
                "Donde Termina El Esfuerzo\nComienza El Fracaso",
                "Tu Cuerpo Puede Aguantar Casi Todo\nEs A Tu Mente A Quien Debes Convencer",
                "¡ Fuerza Y Honor !",
                "https://youtube.com/m8ax",
                "Que La Fuerza\nTe Acompañe Siempre",
                "Entrenar O No Entrenar\nNo Existe El Intentar",
                "Yo Soy\nTu Entrenador",
                "Este Es El Camino\nDel Guerrero",
                "Un Caballero Jedi\nNunca Se Rinde",
                "Si Mi Mente Puede Concebirlo\nPuedo Lograrlo",
                "¿ Por Qué Nos Caemos ?\nPara Aprender A Levantarnos",
                "No Es Quien Soy\nSino Lo Que Hago",
                "Un Gran Poder Conlleva\nUna Gran Responsabilidad",
                "Puedo Hacer Esto\nTodo El Día",
                "Hasta El Infinito\nY Más Allá",
                "Digo Lo Que Digo\nY Hago Lo Que Digo",
                "La Primera Regla Del Gym\nEs Hablar Del Gym",
                "Soy El Dueño De Mi Destino\nY Capitán De Mi Alma",
                "Mi Nombre Es\nMáximo Décimo Meridio",
                "Lo Que Hacemos En La Vida Tiene\nSu Eco En La Eternidad",
                "No Importa Cuánto Golpeas\nSino Lo Que Aguantas",
                "Un Hombre Que No Entrena\nNo Es Un Hombre",
                "Mantén A Tus Amigos Cerca\nY A Tus Pesas Más Cerca",
                "Tú Eres\nEl Elegido",
                "Libera Tu Mente\nEntrena Tu Cuerpo",
                "No Hay Cuchara\nSolo Hay Esfuerzo",
                "--- https://youtube.com/m8ax ---",
                "Hoy Cenaremos\nEn El Infierno",
                "Preferiría Morir De Pie\nQue Vivir Arrodillado",
                "No Dejes Que Nadie Te Diga\nQue No Puedes",
                "Sigue Nadando\nSigue Entrenando",
                "La Vida Se Abre Camino\nCon Cada Repetición",
                "He Visto Cosas Que\nNo Creeríais En El Rack",
                "Solo Tú Decides\nQué Hacer Con Tu Tiempo",
                "Incluso El Más Pequeño\nPuede Cambiar Su Destino",
                "No Conocéis El Poder\nDel Lado Oscuro",
                "Si Quema\nEstá Funcionando",
                "Volveré...\nY Más Fuerte"
            )
            txtMensajeSplash.text = "\n${mensajesMotivadores.random()}"
        }
        backgroundImage.alpha = 0f
        backgroundImage.animate().alpha(1f).setDuration(1000).start()
        val imageLogos = arrayOf(
            R.drawable.logom8ax,
            R.drawable.logoapp,
            R.drawable.logom8ax2,
            R.drawable.logom8ax3,
            R.drawable.logom8ax4,
            R.drawable.logom8ax5,
            R.drawable.logom8ax6,
            R.drawable.logom8ax7,
            R.drawable.logom8ax8,
            R.drawable.logom8ax9,
            R.drawable.logom8ax10,
            R.drawable.logom8ax11,
            R.drawable.logom8ax12,
            R.drawable.logom8ax13
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
            logoImage.visibility = View.VISIBLE
            logoImage.setImageResource(imageLogos.random())
            animarEntradaRandom(logoImage)
        } else {
            logoVideo.visibility = View.VISIBLE
            logoVideo.alpha = 0f
            logoVideo.surfaceTextureListener = object : TextureView.SurfaceTextureListener {
                override fun onSurfaceTextureAvailable(st: SurfaceTexture, w: Int, h: Int) {
                    val surface = Surface(st)
                    try {
                        videoPlayer = MediaPlayer()
                        videoPlayer?.setDataSource(
                            this@SplashActivity,
                            Uri.parse("android.resource://$packageName/${videoLogos.random()}")
                        )
                        videoPlayer?.setSurface(surface)
                        videoPlayer?.isLooping = true
                        videoPlayer?.prepareAsync()
                        videoPlayer?.setOnPreparedListener { mp ->
                            mp.start()
                            animarEntradaRandom(logoVideo)
                        }
                    } catch (e: Exception) {
                    }
                }

                override fun onSurfaceTextureSizeChanged(st: SurfaceTexture, w: Int, h: Int) {}
                override fun onSurfaceTextureDestroyed(st: SurfaceTexture): Boolean = true
                override fun onSurfaceTextureUpdated(st: SurfaceTexture) {}
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
        runnableFinal = Runnable {
            if (!isFinishingSplash) {
                val viewParaMover =
                    if (logoImage.visibility == View.VISIBLE) logoImage else logoVideo
                vibrarVisualmente(viewParaMover)
                viewParaMover.postDelayed({
                    val metrics = resources.displayMetrics
                    val screenWidth = metrics.widthPixels.toFloat() * 1.5f
                    val screenHeight = metrics.heightPixels.toFloat() * 1.5f
                    val direcciones = arrayOf("ARRIBA", "ABAJO", "IZQUIERDA", "DERECHA")
                    val direccionFinal = direcciones.random()
                    val randomRotation = (720..1440).random().toFloat()
                    val randomDuration = (700..1100).random().toLong()
                    var moveX = 0f
                    var moveY = 0f
                    when (direccionFinal) {
                        "ARRIBA" -> moveY = -screenHeight
                        "ABAJO" -> moveY = screenHeight
                        "IZQUIERDA" -> moveX = -screenWidth
                        "DERECHA" -> moveX = screenWidth
                    }
                    viewParaMover.animate().translationX(moveX).translationY(moveY)
                        .rotation(randomRotation).alpha(0f).setDuration(randomDuration)
                        .withEndAction {
                            rootLayout.animate().alpha(0f).setDuration(400).withEndAction {
                                if (!isFinishingSplash) {
                                    liberarRecursos()
                                    startActivity(Intent(this, LoginActivity::class.java))
                                    overridePendingTransition(
                                        android.R.anim.fade_in, android.R.anim.fade_out
                                    )
                                    finish()
                                }
                            }.start()
                        }.start()
                }, 300)
            }
        }
        runnableFinal?.let { splashHandler.postDelayed(it, 5000) }
    }

    private fun animarEntradaRandom(view: View) {
        view.alpha = 1f
        view.scaleX = 1f
        view.scaleY = 1f
        view.rotation = 0f
        view.translationY = 0f
        val opcion = (1..4).random()
        when (opcion) {
            1 -> {
                view.translationY = -1500f
                view.animate().translationY(0f).setDuration(1000)
                    .setInterpolator(AccelerateInterpolator()).withEndAction {
                        vibrarImpacto()
                        view.animate().translationY(-200f).setDuration(250)
                            .setInterpolator(DecelerateInterpolator()).withEndAction {
                                view.animate().translationY(0f).setDuration(250)
                                    .setInterpolator(AccelerateInterpolator()).withEndAction {
                                        vibrarImpacto()
                                        view.animate().translationY(-80f).setDuration(150)
                                            .setInterpolator(DecelerateInterpolator())
                                            .withEndAction {
                                                view.animate().translationY(0f).setDuration(150)
                                                    .setInterpolator(AccelerateInterpolator())
                                                    .withEndAction {
                                                        vibrarImpacto()
                                                    }.start()
                                            }.start()
                                    }.start()
                            }.start()
                    }.start()
            }

            2 -> {
                view.alpha = 0f
                view.animate().alpha(1f).setDuration(1000).start()
            }

            3 -> {
                view.rotation = -720f
                view.scaleX = 0f
                view.scaleY = 0f
                view.animate().rotation(0f).scaleX(1f).scaleY(1f).setDuration(1200)
                    .setInterpolator(DecelerateInterpolator()).start()
            }

            4 -> {
                view.translationY = -1500f
                view.animate().translationY(0f).setDuration(600)
                    .setInterpolator(AccelerateInterpolator()).withEndAction {
                        vibrarImpacto()
                        view.animate().scaleY(0.7f).scaleX(1.3f).setDuration(100).withEndAction {
                            view.animate().scaleY(1f).scaleX(1f).setDuration(300)
                                .setInterpolator(BounceInterpolator()).start()
                        }.start()
                    }.start()
            }
        }
    }

    private fun vibrarVisualmente(view: View) {
        val d = 25L
        val a = 25f
        view.animate().translationXBy(a).setDuration(d).withEndAction {
            view.animate().translationXBy(-a * 2).setDuration(d).withEndAction {
                view.animate().translationXBy(a * 2).setDuration(d).withEndAction {
                    view.animate().translationXBy(-a * 2).setDuration(d).withEndAction {
                        view.animate().translationXBy(a * 2).setDuration(d).withEndAction {
                            view.animate().translationXBy(-a * 2).setDuration(d).withEndAction {
                                view.animate().translationXBy(a).setDuration(d).start()
                            }.start()
                        }.start()
                    }.start()
                }.start()
            }.start()
        }.start()
    }

    private fun vibrarImpacto() {
        val vibrator = getSystemService(VIBRATOR_SERVICE) as android.os.Vibrator
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            vibrator.vibrate(
                android.os.VibrationEffect.createOneShot(
                    50, android.os.VibrationEffect.DEFAULT_AMPLITUDE
                )
            )
        } else {
            @Suppress("DEPRECATION") vibrator.vibrate(50)
        }
    }

    private fun liberarRecursos() {
        try {
            mediaPlayer?.let { if (it.isPlaying) it.stop(); it.release() }
            mediaPlayer = null
            videoPlayer?.let { if (it.isPlaying) it.stop(); it.release() }
            videoPlayer = null
        } catch (e: Exception) {
        }
    }

    override fun onBackPressed() {
        isFinishingSplash = true
        splashHandler.removeCallbacksAndMessages(null)
        liberarRecursos()
        finishAndRemoveTask()
        super.onBackPressed()
    }

    override fun onPause() {
        super.onPause()
        mediaPlayer?.pause()
        videoPlayer?.pause()
    }

    override fun onResume() {
        super.onResume()
        if (!isFinishingSplash) {
            mediaPlayer?.start()
            videoPlayer?.start()
        }
    }

    override fun onDestroy() {
        isFinishingSplash = true
        splashHandler.removeCallbacksAndMessages(null)
        liberarRecursos()
        super.onDestroy()
    }
}