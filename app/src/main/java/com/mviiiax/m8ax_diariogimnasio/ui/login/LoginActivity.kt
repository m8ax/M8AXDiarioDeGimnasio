package com.mviiiax.m8ax_diariogimnasio.ui.login

import android.content.Intent
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import com.mviiiax.m8ax_diariogimnasio.MainActivity
import com.mviiiax.m8ax_diariogimnasio.PasswordManager
import com.mviiiax.m8ax_diariogimnasio.R
import java.util.Calendar
import java.util.Locale
import java.util.concurrent.Executor

class LoginActivity : AppCompatActivity(), TextToSpeech.OnInitListener {
    private lateinit var passwordManager: PasswordManager
    private lateinit var executor: Executor
    private lateinit var tts: TextToSpeech
    private var ttsEnabled: Boolean = true
    fun Int.dpToPx(): Int = (this * resources.displayMetrics.density).toInt()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        val logos = listOf(R.drawable.logoapp, R.drawable.logom8ax)
        val chosenLogo = logos.random()
        val ivLogo: ImageView = findViewById(R.id.ivLogo)
        ivLogo.setImageResource(chosenLogo)
        val rootLayout = findViewById<LinearLayout>(R.id.rootLayout)
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val bgColor = if (hour in 7..19) {
            0xFF595959.toInt()
        } else {
            0xFF101010.toInt()
        }
        rootLayout.setBackgroundColor(bgColor)
        val prefs = getSharedPreferences("M8AX-Config_TTS", MODE_PRIVATE)
        ttsEnabled = prefs.getBoolean("tts_enabled", true)
        tts = TextToSpeech(this, this)
        passwordManager = PasswordManager(this)
        executor = ContextCompat.getMainExecutor(this)
        val tvMessage: TextView = findViewById(R.id.tvMessage)
        val etPassword: EditText = findViewById(R.id.etPassword)
        val btnSubmit: Button = findViewById(R.id.btnSubmit)
        btnSubmit.setBackgroundResource(R.drawable.btn_shadow)
        btnSubmit.elevation = 8f
        btnSubmit.stateListAnimator = null
        val creatingPassword = !passwordManager.hasPassword()
        btnSubmit.setOnClickListener {
            val input = etPassword.text.toString()
            if (input.isEmpty()) {
                Toast.makeText(this, "--- Introduce Una Contraseña ---", Toast.LENGTH_SHORT).show()
                if (ttsEnabled) speak("M 8 A X, Introduce Una Contraseña.")
                return@setOnClickListener
            }
            if (creatingPassword) {
                passwordManager.savePassword(input)
                if (ttsEnabled) {
                    val frasesTTS = listOf(
                        "Contraseña Creada. Protocolo Activado.",
                        "Clave Generada. Sistema Despierto.",
                        "Código Nuevo. Núcleo Estable.",
                        "Contraseña Lista. Puertas Abiertas.",
                        "Clave Aprobada. Acceso Futuro.",
                        "Código Registrado. IA Conforme.",
                        "Contraseña Sellada. Universo OK.",
                        "Clave Completa. Motor Cuántico En Línea.",
                        "Código Confirmado. Dimensión Segura.",
                        "Contraseña Cargada. Sector Protegido."
                    )
                    val frase = frasesTTS.random()
                    speak(frase)
                    Thread.sleep(3000)
                }
                goToMain()
            } else {
                val correctMessages = listOf(
                    "¡Contraseña Correcta! Entrando.",
                    "¡Bien Hecho! Acceso Concedido.",
                    "¡Éxito! Has Entrado.",
                    "¡Genial! Todo Listo.",
                    "¡Perfecto! Usuario Aprobado."
                )
                val incorrectMessages = listOf(
                    "¡Oops! Contraseña Incorrecta.",
                    "¡Nope! Intenta De Nuevo.",
                    "¡Error! Acceso Denegado.",
                    "¡Falló! Prueba Otra Vez.",
                    "¡Cuidado! Contraseña Inválida."
                )
                if (input == passwordManager.getPassword()) {
                    if (ttsEnabled) {
                        Toast.makeText(this, "--- Contraseña Correcta ---", Toast.LENGTH_SHORT)
                            .show()
                        val message = correctMessages.random()
                        speak(message)
                        Thread.sleep(3000)
                    }
                    goToMain()
                } else {
                    Toast.makeText(this, "--- Contraseña Incorrecta ---", Toast.LENGTH_SHORT).show()
                    if (ttsEnabled) {
                        val message = incorrectMessages.random()
                        speak(message)
                    }
                    etPassword.text.clear()
                }
            }
        }
        val btnFingerprint = Button(this).apply {
            text = "--- Entrar Con Huella ---"
            setBackgroundResource(R.drawable.btn_shadow)
            elevation = 8f
            stateListAnimator = null
        }
        val params = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT
        )
        params.topMargin = 32.dpToPx()
        btnFingerprint.layoutParams = params
        rootLayout.addView(btnFingerprint)
        btnFingerprint.setOnClickListener {
            setupBiometricLogin()
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts?.setLanguage(tts?.defaultLanguage ?: Locale.getDefault())
            tts?.setSpeechRate(0.9f)
            val tvMessage: TextView = findViewById(R.id.tvMessage)
            val btnSubmit: Button = findViewById(R.id.btnSubmit)
            val creatingPassword = !passwordManager.hasPassword()
            if (creatingPassword) {
                tvMessage.text = "--- Crea Tu Contraseña ---"
                btnSubmit.text = "--- Crear ---"
                val passwordPrompts = listOf(
                    "M 8 A X, Forja Tu Llave Secreta.",
                    "M 8 A X, Solo Tú Puedes Crear La Contraseña.",
                    "M 8 A X, Es Hora De Elegir Tu Código Legendario.",
                    "M 8 A X, Tu Destino Depende De Esta Contraseña.",
                    "M 8 A X, Crea El Secreto Que Nadie Descubrirá.",
                    "M 8 A X, Tu Contraseña Es La Llave Del Poder.",
                    "M 8 A X, Diseña El Código Que Protegerá Todo.",
                    "M 8 A X, Forja Tu Llave Maestra Con Cuidado.",
                    "M 8 A X, Solo Los Valientes Eligen Su Contraseña.",
                    "M 8 A X, Crea El Código Que Cambiará Tu Mundo."
                )
                if (ttsEnabled) {
                    speak(passwordPrompts.random())
                }
            } else {
                tvMessage.text = "--- Introduce Tu Contraseña ---"
                btnSubmit.text = "--- Aceptar ---"
                val sciFiPrompts = listOf(
                    "M 8 A X, Introduce Tu Código De Acceso Intergaláctico.",
                    "M 8 A X, Conecta Tu Mente Y Teclea La Contraseña.",
                    "M 8 A X, Inserta El Código Que Mantendrá La Nave Segura.",
                    "M 8 A X, Introduce Tu Llave Digital Del Futuro.",
                    "M 8 A X, Accede Al Núcleo Con Tu Contraseña Secreta.",
                    "M 8 A X, Teclea El Código Que Desbloqueará La Realidad.",
                    "M 8 A X, Introduce La Secuencia Para Activar El Portal.",
                    "M 8 A X, Solo Tú Puedes Abrir La Terminal Con Este Código.",
                    "M 8 A X, Teclea La Contraseña Para Entrar Al Ciberuniverso.",
                    "M 8 A X, Introduce Tu Clave Para Viajar Entre Dimensiones."
                )
                if (ttsEnabled) {
                    speak(sciFiPrompts.random())
                }
            }
        }
    }

    private fun speak(text: String) {
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "login_tts")
    }

    private fun setupBiometricLogin() {
        val biometricManager = BiometricManager.from(this)
        if (biometricManager.canAuthenticate() != BiometricManager.BIOMETRIC_SUCCESS) {
            if (ttsEnabled) {
                speak("Huella No Disponible.")
            }
            Toast.makeText(this, "--- Huella No Disponible ---", Toast.LENGTH_SHORT).show()
            return
        }
        val promptInfo = BiometricPrompt.PromptInfo.Builder().setTitle("--- Login Con Huella ---")
            .setSubtitle("--- Usa Tu Huella Para Entrar Más Rápido ---")
            .setNegativeButtonText("--- Cancelar ---").build()
        val fingerprintPromptsFunny = listOf(
            "Vamos Dedito, No Seas Tímido.",
            "Tócame Suave Que No Muero.",
            "Dedito Travieso, Haz Tu Magia.",
            "Un Toquecito Y Todo Se Arregla.",
            "No Falles Ahora, Soy Sensible.",
            "A Ver Ese Dedito; Tocame Suave Nene....",
            "Hazlo Bien O Te Castigo Con Un Beep.",
            "Suavecito, Que Me Haces Cosquillas.",
            "Vamos, Que Tu Huella Tiene Poder.",
            "Dame Ese Toque Como Un Maestro Jedi."
        )
        if (ttsEnabled) {
            speak(fingerprintPromptsFunny.random())
        }
        val biometricPrompt = BiometricPrompt(
            this, executor, object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    val fingerprintSuccessSciFiShort = listOf(
                        "Huella Confirmada. Acceso Activado.",
                        "Toque Detectado. Sistema Online.",
                        "Huella Aceptada. Seguridad Desbloqueada.",
                        "Acceso Autorizado. Zona Preparada.",
                        "Toque Correcto. IA En Línea.",
                        "Huella Verificada. Portal Abierto.",
                        "Verificación Exitosa. Red Desbloqueada.",
                        "Huella Registrada. Secuencia Iniciada.",
                        "Éxito Total. Usuario Autorizado.",
                        "Huella Confirmada. Todo Listo."
                    )
                    if (ttsEnabled) {
                        speak(fingerprintSuccessSciFiShort.random())
                        Thread.sleep(3000)
                    }
                    goToMain()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    Toast.makeText(
                        applicationContext, "--- Error Huella: $errString", Toast.LENGTH_SHORT
                    ).show()
                    if (ttsEnabled) {
                        speak("Error De Huella.")
                    }
                }
            })
        biometricPrompt.authenticate(promptInfo)
    }

    private fun goToMain() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        tts?.stop()
        tts?.shutdown()
    }
}