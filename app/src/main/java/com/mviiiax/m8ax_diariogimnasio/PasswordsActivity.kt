package com.mviiiax.m8ax_diariogimnasio

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Color
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.text.method.PasswordTransformationMethod
import android.util.Base64
import android.view.MotionEvent
import android.view.WindowManager
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import com.itextpdf.text.Document
import com.itextpdf.text.Element
import com.itextpdf.text.Font
import com.itextpdf.text.Paragraph
import com.itextpdf.text.pdf.BaseFont
import com.itextpdf.text.pdf.PdfWriter
import org.json.JSONArray
import org.json.JSONObject
import org.spongycastle.jce.provider.BouncyCastleProvider
import java.io.File
import java.io.FileOutputStream
import java.security.SecureRandom
import java.security.Security
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.Executor
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

class PasswordsActivity : AppCompatActivity(), TextToSpeech.OnInitListener {
    private lateinit var etGenerated: EditText
    private lateinit var etServicio: EditText
    private lateinit var etPassword: EditText
    private lateinit var cb64: CheckBox
    private lateinit var cb128: CheckBox
    private lateinit var cb256: CheckBox
    private lateinit var cb512: CheckBox
    private lateinit var btnGenerate: Button
    private lateinit var btnCopy: Button
    private lateinit var btnAdd: Button
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: PasswordsAdapter
    private val items = mutableListOf<PasswordItem>()
    private lateinit var masterPassword: String
    private lateinit var secretKey: SecretKeySpec
    private lateinit var checkBoxes: Array<CheckBox>
    private lateinit var executor: Executor
    private lateinit var passwordManager: PasswordManager
    private lateinit var tts: TextToSpeech
    private var ttsEnabled: Boolean = true
    private var mediaPlayer: MediaPlayer? = null
    private val prefsName = "M8AX-Passwords"
    private val prefsKey = "M8AX-Servicios-Claves_Encriptados"
    private val configPrefs = "M8AX-Config_TTS"
    private lateinit var btnAdvanced: Button
    private lateinit var btnExportPdf: Button
    private var passwordVisible = false
    private val SESSION_TIMEOUT_MS = 120_000L
    private val handler = Handler(Looper.getMainLooper())
    private var cerrarRunnable: Runnable? = null

    companion object {
        private const val CHARSET =
            "!\"#$%&'()*+,-./0123456789:;<=>?@" + "ABCDEFGHIJKLMNOPQRSTUVWXYZ[\\]^_`" + "abcdefghijklmnopqrstuvwxyz{|}~"
        private val BITS_PER_CHAR = kotlin.math.log2(CHARSET.length.toDouble())
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE
        )
        setContentView(R.layout.activity_passwords)
        executor = ContextCompat.getMainExecutor(this)
        passwordManager = PasswordManager(this)
        val prefs = getSharedPreferences(configPrefs, MODE_PRIVATE)
        ttsEnabled = prefs.getBoolean("tts_enabled", true)
        tts = TextToSpeech(this, this)
        mediaPlayer = MediaPlayer.create(this, R.raw.m8axsonidofondo)
        mediaPlayer?.isLooping = true
        mediaPlayer?.start()
        etGenerated = findViewById(R.id.etGenerated)
        etServicio = findViewById(R.id.etServicio)
        etPassword = findViewById(R.id.etPassword)
        cb64 = findViewById(R.id.cb64)
        cb128 = findViewById(R.id.cb128)
        cb256 = findViewById(R.id.cb256)
        cb512 = findViewById(R.id.cb512)
        btnGenerate = findViewById(R.id.btnGenerate)
        btnCopy = findViewById(R.id.btnCopy)
        btnAdd = findViewById(R.id.btnAdd)
        btnExportPdf = findViewById(R.id.btnExportPdf)
        btnExportPdf.setOnClickListener {
            exportarAPdf()
        }
        btnAdvanced = findViewById(R.id.btnAdvanced)
        btnAdvanced.setOnClickListener {
            val opciones = arrayOf("Generar Por Caracteres", "Generar Por Entropía")
            speak("Elige Que Quieres...")
            AlertDialog.Builder(this).setTitle("Modo Avanzado").setItems(opciones) { _, which ->
                when (which) {
                    0 -> {
                        speak("Vale")
                        val input = EditText(this)
                        input.inputType = android.text.InputType.TYPE_CLASS_NUMBER
                        AlertDialog.Builder(this)
                            .setTitle("Número De Caracteres Para La Contraseña")
                            .setMessage("Introduce Cuántos Caracteres Quieres").setView(input)
                            .setPositiveButton("OK") { _, _ ->
                                val num = input.text.toString().toIntOrNull() ?: 0
                                if (num > 0) {
                                    val password =
                                        (1..num).map { PasswordsActivity.CHARSET.random() }
                                            .joinToString("")
                                    etGenerated.setText(password)
                                    val charsetSize = PasswordsActivity.CHARSET.length
                                    val entropy = num * kotlin.math.log2(charsetSize.toDouble())
                                    val mensaje = "Contraseña De $num Caracteres Generada Con ${
                                        "%.2f".format(entropy)
                                    } Bits De Entropía"
                                    Toast.makeText(this, mensaje, Toast.LENGTH_SHORT).show()
                                    speak(mensaje)
                                } else {
                                    val mensaje = "Número Inválido"
                                    Toast.makeText(this, mensaje, Toast.LENGTH_SHORT).show()
                                    speak(mensaje)
                                }
                            }.setNegativeButton("Cancelar", null).show()
                    }

                    1 -> {
                        speak("Okey")
                        val input = EditText(this)
                        input.inputType = android.text.InputType.TYPE_CLASS_NUMBER
                        AlertDialog.Builder(this)
                            .setTitle("Bits De Entropía Para Generar Contraseña")
                            .setMessage("Introduce La Entropía Deseada").setView(input)
                            .setPositiveButton("OK") { _, _ ->
                                val bits = input.text.toString().toIntOrNull() ?: 0
                                if (bits > 0) {
                                    val length =
                                        kotlin.math.ceil(bits / PasswordsActivity.BITS_PER_CHAR)
                                            .toInt()
                                    val password =
                                        (1..length).map { PasswordsActivity.CHARSET.random() }
                                            .joinToString("")
                                    etGenerated.setText(password)
                                    val charsetSize = PasswordsActivity.CHARSET.length
                                    val entropy = length * kotlin.math.log2(charsetSize.toDouble())
                                    val mensaje = "Contraseña Generada Con ${length} Caracteres Y ${
                                        "%.2f".format(entropy)
                                    } Bits De Entropía"
                                    Toast.makeText(this, mensaje, Toast.LENGTH_SHORT).show()
                                    speak(mensaje)
                                } else {
                                    val mensaje = "Valor De Entropía Inválido"
                                    Toast.makeText(this, mensaje, Toast.LENGTH_SHORT).show()
                                    speak(mensaje)
                                }
                            }.setNegativeButton("Cancelar", null).show()
                    }
                }
            }.show()
        }
        recyclerView = findViewById(R.id.recyclerViewPasswords)
        cb128.isChecked = true
        setInputsEnabled(false)
        checkBoxes = arrayOf(cb64, cb128, cb256, cb512)
        checkBoxes.forEach { cb ->
            cb.setOnCheckedChangeListener { buttonView, isChecked ->
                if (isChecked) checkBoxes.filter { it != buttonView }
                    .forEach { it.isChecked = false }
            }
        }
        adapter = PasswordsAdapter(items, { savePasswords() }) { mensaje ->
            Toast.makeText(this, mensaje, Toast.LENGTH_SHORT).show()
            speak(mensaje)
        }
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter
        val swipeHandler = object :
            ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {
            override fun onMove(
                rv: RecyclerView, vh: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder
            ) = false

            override fun onSwiped(vh: RecyclerView.ViewHolder, direction: Int) {
                val pos = vh.adapterPosition
                val item = items[pos]
                adapter.eliminarItem(pos)
                val mensajeBorrado = "Borrado ${item.servicio}"
                Toast.makeText(this@PasswordsActivity, mensajeBorrado, Toast.LENGTH_SHORT).show()
                speak(mensajeBorrado)
                Snackbar.make(
                    findViewById(R.id.recyclerViewPasswords),
                    "${item.servicio} borrado",
                    Snackbar.LENGTH_LONG
                ).setAction("Deshacer") {
                    items.add(pos, item)
                    adapter.notifyDataSetChanged()
                    val mensajeRestaurado = "Restaurado ${item.servicio}"
                    Toast.makeText(
                        this@PasswordsActivity, mensajeRestaurado, Toast.LENGTH_SHORT
                    ).show()
                    speak(mensajeRestaurado)
                }.show()
            }
        }
        ItemTouchHelper(swipeHandler).attachToRecyclerView(recyclerView)
        checkMasterPassword()
        btnGenerate.setOnClickListener {
            val bits = when {
                cb64.isChecked -> 64
                cb128.isChecked -> 128
                cb256.isChecked -> 256
                cb512.isChecked -> 512
                else -> 128
            }
            etGenerated.setText(generatePassword(bits))
            Toast.makeText(this, "Contraseña Generada", Toast.LENGTH_SHORT).show()
            speak("Contraseña Generada")
        }
        btnCopy.setOnClickListener {
            val text = etGenerated.text.toString()
            if (text.isNotEmpty()) {
                copyToClipboard(text)
                speak("Contraseña Copiada")
            } else {
                Toast.makeText(this, "Nada Que Copiar", Toast.LENGTH_SHORT).show()
                speak("Nada Que Copiar")
            }
        }
        btnAdd.setOnClickListener {
            val servicio = etServicio.text.toString().trim()
            val password = etPassword.text.toString().trim()
            if (servicio.isNotEmpty() && password.isNotEmpty()) {
                val item = PasswordItem(servicio, password)
                items.add(item)
                adapter.notifyItemInserted(items.size - 1)
                savePasswords()
                etServicio.text.clear()
                etPassword.text.clear()
                val mensaje = "Contraseña Añadida"
                Toast.makeText(this, mensaje, Toast.LENGTH_SHORT).show()
                speak(mensaje)
            } else {
                val mensaje = "Debes Completar Ambos Campos: Servicio Y Contraseña"
                Toast.makeText(this, mensaje, Toast.LENGTH_SHORT).show()
                speak(mensaje)
            }
        }
        passwordVisible = false
        etPassword.transformationMethod = PasswordTransformationMethod.getInstance()
        etPassword.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_UP) {
                val drawableEndIndex = 2
                val drawable = etPassword.compoundDrawables[drawableEndIndex]
                if (drawable != null && event.rawX >= (etPassword.right - drawable.bounds.width() - etPassword.paddingEnd)) {
                    val cursorPos = etPassword.selectionStart
                    passwordVisible = !passwordVisible
                    val eyeDrawable = if (passwordVisible) ContextCompat.getDrawable(
                        this, R.drawable.ic_eye_open_24
                    )
                    else ContextCompat.getDrawable(this, R.drawable.ic_eye_closed_24)
                    eyeDrawable?.setTint(Color.WHITE)
                    etPassword.setCompoundDrawablesWithIntrinsicBounds(
                        null, null, eyeDrawable, null
                    )
                    etPassword.transformationMethod = if (passwordVisible) null
                    else PasswordTransformationMethod.getInstance()
                    etPassword.setSelection(cursorPos)
                    etPassword.setHorizontallyScrolling(false)
                    etPassword.maxLines = 4
                    etPassword.minLines = 2
                    return@setOnTouchListener true
                }
            }
            false
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1001) {
            if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                exportarAPdf()
            } else {
                Toast.makeText(
                    this, "Permiso Denegado. No Se Puede Guardar El PDF...", Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun exportarAPdf() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            if (ContextCompat.checkSelfPermission(
                    this, Manifest.permission.WRITE_EXTERNAL_STORAGE
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), 1001
                )
                return
            }
        }
        try {
            if (items.isEmpty()) {
                Toast.makeText(this, "No Hay Contraseñas Para Exportar", Toast.LENGTH_SHORT).show()
                speak("No Hay Contraseñas Para Exportar.")
                return
            }
            if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
                Security.addProvider(BouncyCastleProvider())
            }
            val fechaHora =
                SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(java.util.Date())
            val fileName = "M8AX-Claves_$fechaHora.PdF"
            val document = Document()
            var outputStream: java.io.OutputStream? = null
            var uriToOpen: Uri? = null
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val contentValues = ContentValues().apply {
                    put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(android.provider.MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
                    put(
                        android.provider.MediaStore.MediaColumns.RELATIVE_PATH,
                        Environment.DIRECTORY_DOWNLOADS
                    )
                }
                uriToOpen = contentResolver.insert(
                    android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues
                )
                outputStream = uriToOpen?.let { contentResolver.openOutputStream(it) }
            } else {
                val downloadsDir =
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                val pdfFile = File(downloadsDir, fileName)
                outputStream = FileOutputStream(pdfFile)
                uriToOpen = Uri.fromFile(pdfFile)
            }
            if (outputStream == null) {
                Toast.makeText(this, "Error Al Crear El PDF", Toast.LENGTH_SHORT).show()
                speak("Errorr Al Crear El P D F.")
                return
            }
            val inputStream = assets.open("fonts/mviiiax.ttf")
            val fontFile = File(cacheDir, "mviiiax.ttf")
            inputStream.copyTo(FileOutputStream(fontFile))
            inputStream.close()
            val baseFont =
                BaseFont.createFont(fontFile.absolutePath, BaseFont.IDENTITY_H, BaseFont.EMBEDDED)
            val fuenteTitulo = Font(baseFont, 18f, Font.BOLD)
            val fuenteNormal = Font(baseFont, 14f, Font.NORMAL)
            val fuenteFooter = Font(baseFont, 22f, Font.BOLD, com.itextpdf.text.BaseColor.BLUE)
            val writer = PdfWriter.getInstance(document, outputStream)
            writer.setEncryption(
                masterPassword.toByteArray(Charsets.UTF_8),
                masterPassword.toByteArray(Charsets.UTF_8),
                PdfWriter.ALLOW_PRINTING,
                PdfWriter.ENCRYPTION_AES_256
            )
            document.open()
            val fechaActual = SimpleDateFormat(
                "dd/MM/yyyy - HH:mm:ss", Locale.getDefault()
            ).format(java.util.Date())
            document.add(Paragraph("--- Gestor De Contraseñas ---\n\n", fuenteTitulo))
            document.add(Paragraph("Fecha → $fechaActual", fuenteNormal))
            document.add(Paragraph("Total De Servicios → ${items.size}", fuenteNormal))
            document.add(Paragraph("\n\n"))
            items.forEachIndexed { index, item ->
                val linea = "${index + 1} - ${item.servicio}  →  ${item.password}"
                document.add(Paragraph(linea, fuenteNormal))
            }
            document.add(Paragraph("\n\n\n"))
            val footer = Paragraph(
                "By M8AX Corp. ${
                    SimpleDateFormat(
                        "yyyy", Locale.getDefault()
                    ).format(java.util.Date())
                }", fuenteFooter
            )
            footer.alignment = Element.ALIGN_CENTER
            document.add(footer)
            document.close()
            outputStream.close()
            Toast.makeText(this, "PDF Cifrado Generado En Descargas", Toast.LENGTH_LONG).show()
            speak("P D F Cifrado; Generado En Descargas.")
            uriToOpen?.let {
                val intent = android.content.Intent(android.content.Intent.ACTION_VIEW)
                intent.setDataAndType(it, "application/pdf")
                intent.flags =
                    android.content.Intent.FLAG_ACTIVITY_NO_HISTORY or android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                startActivity(android.content.Intent.createChooser(intent, "Abrir PDF Con…"))
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Error Al Generar El PDF", Toast.LENGTH_SHORT).show()
            speak("Error Al Generar El P D F.")
        }
    }

    private fun setInputsEnabled(enabled: Boolean) {
        etGenerated.isEnabled = enabled
        etServicio.isEnabled = enabled
        etPassword.isEnabled = enabled
        cb64.isEnabled = enabled
        cb128.isEnabled = enabled
        cb256.isEnabled = enabled
        cb512.isEnabled = enabled
        btnGenerate.isEnabled = enabled
        btnCopy.isEnabled = enabled
        btnAdd.isEnabled = enabled
        btnAdvanced.isEnabled = enabled
        btnExportPdf.isEnabled = enabled
        recyclerView.isEnabled = enabled
    }

    private fun checkMasterPassword() {
        if (!passwordManager.hasPassword()) {
            pedirMasterPassword()
        } else {
            masterPassword = passwordManager.getPassword()!!
            val prefs = getSharedPreferences(prefsName, Context.MODE_PRIVATE)
            val saltBase64 = prefs.getString("salt", null)
            val salt = saltBase64?.let { Base64.decode(it, Base64.NO_WRAP) }
            secretKey = generateKey(masterPassword, salt)
            setupBiometricLogin()
        }
    }

    private fun pedirMasterPassword() {
        val editText = EditText(this)
        editText.hint = "Introduce Contraseña Maestra"
        val dialog = AlertDialog.Builder(this).setTitle("Crear Contraseña Maestra")
            .setMessage("Debes Crear Una Contraseña Maestra Para Cifrar Las Contraseñas.")
            .setView(editText).setCancelable(false).setPositiveButton("OK") { _, _ ->
                val input = editText.text?.toString()?.trim()
                if (input.isNullOrEmpty()) {
                    val msg = "No Puedes Dejar La Contraseña Vacía"
                    Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
                    speak(msg)
                    pedirMasterPassword()
                } else {
                    masterPassword = input
                    secretKey = generateKey(masterPassword)
                    passwordManager.savePassword(masterPassword)
                    val msg = "Contraseña Maestra Creada Correctamente"
                    Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
                    speak(msg)
                    setupBiometricLogin()
                    loadPasswords()
                }
            }.setNegativeButton("Cancelar") { _, _ ->
                val msg = "Se Canceló La Creación De La Contraseña. Cerrando App..."
                Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
                speak(msg)
                finish()
            }.create()
        dialog.show()
    }

    private fun generateKey(password: String, salt: ByteArray? = null): SecretKeySpec {
        val actualSalt = salt ?: ByteArray(16).also { SecureRandom().nextBytes(it) }
        if (salt == null) {
            val prefs = getSharedPreferences(prefsName, Context.MODE_PRIVATE)
            prefs.edit().putString("salt", Base64.encodeToString(actualSalt, Base64.NO_WRAP))
                .apply()
        }
        val spec = javax.crypto.spec.PBEKeySpec(password.toCharArray(), actualSalt, 200_000, 256)
        val factory = javax.crypto.SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val keyBytes = factory.generateSecret(spec).encoded
        return SecretKeySpec(keyBytes, "AES")
    }

    private fun setupBiometricLogin() {
        val biometricManager = BiometricManager.from(this)
        if (biometricManager.canAuthenticate() == BiometricManager.BIOMETRIC_SUCCESS) {
            val promptInfo = BiometricPrompt.PromptInfo.Builder().setTitle("Login Con Huella")
                .setSubtitle("Usa Tu Huella Para Entrar Más Rápido")
                .setNegativeButtonText("Usar Contraseña").build()
            val biometricPrompt = BiometricPrompt(
                this, executor, object : BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                        super.onAuthenticationSucceeded(result)
                        speak("Huella Aceptada")
                        loadPasswords()
                        setInputsEnabled(true)
                    }

                    override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                        super.onAuthenticationError(errorCode, errString)
                        speak("Error De Huella: $errString")
                        if (errorCode != BiometricPrompt.ERROR_CANCELED) {
                            pedirMasterPasswordLogin()
                        }
                    }
                })
            biometricPrompt.authenticate(promptInfo)
        } else {
            pedirMasterPasswordLogin()
        }
    }

    private fun pedirMasterPasswordLogin() {
        val editText = EditText(this)
        editText.hint = "Contraseña Maestra"
        val dialog = AlertDialog.Builder(this).setTitle("Login")
            .setMessage("Introduce Tu Contraseña Maestra").setView(editText).setCancelable(true)
            .setPositiveButton("OK") { _, _ ->
                val input = editText.text?.toString()?.trim()
                when {
                    input.isNullOrEmpty() -> {
                        val msg = "Contraseña Requerida"
                        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
                        speak(msg)
                        pedirMasterPasswordLogin()
                    }

                    input != masterPassword -> {
                        val msg = "Contraseña Incorrecta"
                        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
                        speak(msg)
                        Thread.sleep(2000)
                        pedirMasterPasswordLogin()
                    }

                    else -> {
                        val msg = "Contraseña Correcta"
                        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
                        speak(msg)
                        loadPasswords()
                        setInputsEnabled(true)
                    }
                }
            }.setNegativeButton("Cancelar") { _, _ ->
                val msg = "Login Cancelado"
                Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
                speak("Login Cancelado")
                Thread.sleep(2000)
                finish()
            }.create()
        dialog.show()
        speak("Introduce Tu Contraseña Maestra")
    }

    private fun generatePassword(bits: Int): String {
        val length = kotlin.math.ceil(bits / BITS_PER_CHAR).toInt()
        val random = SecureRandom()
        val result = StringBuilder(length)
        repeat(length) {
            result.append(CHARSET[random.nextInt(CHARSET.length)])
        }
        return result.toString()
    }

    private fun copyToClipboard(text: String) {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Password", text)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(this, "Copiado Al Portapapeles", Toast.LENGTH_SHORT).show()
    }

    private fun savePasswords() {
        try {
            val jsonArray = JSONArray()
            items.forEach {
                val obj = JSONObject()
                obj.put("servicio", it.servicio)
                obj.put("password", it.password)
                jsonArray.put(obj)
            }
            val jsonStr = jsonArray.toString()
            val encrypted = encrypt(jsonStr)
            val prefs = getSharedPreferences(prefsName, Context.MODE_PRIVATE)
            prefs.edit().putString(prefsKey, encrypted).apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun loadPasswords() {
        try {
            val prefs = getSharedPreferences(prefsName, Context.MODE_PRIVATE)
            val encrypted = prefs.getString(prefsKey, null)
            if (!encrypted.isNullOrEmpty()) {
                val decrypted = decrypt(encrypted)
                val jsonArray = JSONArray(decrypted)
                items.clear()
                for (i in 0 until jsonArray.length()) {
                    val obj = jsonArray.getJSONObject(i)
                    val item = PasswordItem(obj.getString("servicio"), obj.getString("password"))
                    items.add(item)
                }
                adapter.notifyDataSetChanged()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun encrypt(str: String): String {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val iv = ByteArray(12)
        SecureRandom().nextBytes(iv)
        val spec = GCMParameterSpec(128, iv)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, spec)
        val encrypted = cipher.doFinal(str.toByteArray(Charsets.UTF_8))
        val combined = iv + encrypted
        return Base64.encodeToString(combined, Base64.NO_WRAP)
    }

    private fun decrypt(str: String): String {
        val combined = Base64.decode(str, Base64.NO_WRAP)
        val iv = combined.copyOfRange(0, 12)
        val encrypted = combined.copyOfRange(12, combined.size)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val spec = GCMParameterSpec(128, iv)
        cipher.init(Cipher.DECRYPT_MODE, secretKey, spec)
        return String(cipher.doFinal(encrypted), Charsets.UTF_8)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts?.setLanguage(tts?.defaultLanguage ?: Locale.getDefault())
            tts?.setSpeechRate(0.9f)
        }
    }

    override fun onStop() {
        super.onStop()
        cerrarRunnable = Runnable {
            masterPassword = ""
            secretKey = SecretKeySpec(ByteArray(32), "AES")
            speak("Cierro El Gestor De Contraseñas Por Seguridad.")
            Thread.sleep(5000)
            finish()
        }
        handler.postDelayed(cerrarRunnable!!, SESSION_TIMEOUT_MS)
    }

    override fun onStart() {
        super.onStart()
        cerrarRunnable?.let {
            handler.removeCallbacks(it)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        tts?.stop()
        tts?.shutdown()
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
        handler.removeCallbacksAndMessages(null)
        masterPassword = ""
        secretKey = SecretKeySpec(ByteArray(32), "AES")
        items.clear()
    }

    override fun onPause() {
        super.onPause()
        mediaPlayer?.pause()
    }

    override fun onResume() {
        super.onResume()
        mediaPlayer?.start()
    }

    fun speak(text: String) {
        if (ttsEnabled) tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "passwords_tts")
    }
}