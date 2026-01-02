package com.mviiiax.m8ax_diariogimnasio

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color
import android.media.MediaPlayer
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.text.InputType
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.GridLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.setPadding
import org.shredzone.commons.suncalc.MoonIllumination
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class FumarActivity : AppCompatActivity(), TextToSpeech.OnInitListener {
    private lateinit var prefs: SharedPreferences
    private lateinit var prefsFumar: SharedPreferences
    private lateinit var tts: TextToSpeech
    private lateinit var rootLayout: FrameLayout
    private val lluviaHandler = Handler(Looper.getMainLooper())
    private var ttsEnabled = false
    private lateinit var mediaPlayer: MediaPlayer
    private var fechaDejarFumar: Calendar? = null
    private var precioPaquete: Double = 0.0
    private var paquetesDia: Double = 0.0
    private var alquitranMg: Double = 0.0
    private var nicotinaMg: Double = 0.0
    private var monoxidoMg: Double = 0.0
    private lateinit var valueViews: List<TextView>
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var grid: GridLayout
    private val updateRunnable = object : Runnable {
        override fun run() {
            actualizarValores()
            handler.postDelayed(this, 1000)
        }
    }

    private fun lanzarPaquete() {
        val cantidad = (1..5).random()
        repeat(cantidad) {
            val paquete = ImageView(this)
            val numero = (1..3).random()
            val altura = (90..220).random()
            var ancho = altura
            when (numero) {
                1 -> {
                    paquete.setImageResource(R.drawable.m8axtabaco)
                    ancho = (altura * 0.65).toInt()
                    paquete.layoutParams = FrameLayout.LayoutParams(ancho, altura)
                }

                2 -> {
                    paquete.setImageResource(R.drawable.logom8ax)
                    paquete.layoutParams = FrameLayout.LayoutParams(altura, altura)
                }

                3 -> {
                    paquete.setImageResource(R.drawable.logoapp)
                    paquete.layoutParams = FrameLayout.LayoutParams(altura, altura)
                }
            }
            val pantallaWidth = resources.displayMetrics.widthPixels
            val posX = (0..(pantallaWidth - ancho)).random()
            paquete.x = posX.toFloat()
            paquete.y = -altura.toFloat()
            rootLayout.addView(paquete)
            val duracionCaida = (3000..6000).random().toLong()
            val rotacion = (180..720).random().toFloat()
            val alturaFinal = resources.displayMetrics.heightPixels.toFloat() - altura
            val reboteAltura = alturaFinal - (altura * 0.2f)
            val animatorY = ObjectAnimator.ofFloat(
                paquete, "translationY", -altura.toFloat(), alturaFinal, reboteAltura, alturaFinal
            )
            animatorY.duration = duracionCaida
            animatorY.interpolator = android.view.animation.AccelerateDecelerateInterpolator()
            val animatorRot = ObjectAnimator.ofFloat(paquete, "rotation", 0f, rotacion)
            animatorRot.duration = duracionCaida
            val set = AnimatorSet()
            set.playTogether(animatorY, animatorRot)
            set.start()
            set.addListener(object : android.animation.AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: android.animation.Animator) {
                    rootLayout.removeView(paquete)
                }
            })
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = getSharedPreferences("M8AX-Config_TTS", Context.MODE_PRIVATE)
        prefsFumar = getSharedPreferences("M8AX-Dejar_De_Fumar", MODE_PRIVATE)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        ttsEnabled = prefs.getBoolean("tts_enabled", false)
        tts = TextToSpeech(this, this)
        mediaPlayer = MediaPlayer.create(this, R.raw.m8axsonidofondo)
        mediaPlayer.isLooping = true
        mediaPlayer.start()
        if (!cargarDatosEsenciales()) {
            pedirDatosEsenciales()
        } else {
            inicializarUI()
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            window.decorView.systemUiVisibility =
                (View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_LAYOUT_STABLE)
        }
    }

    private fun cargarDatosEsenciales(): Boolean {
        return if (prefsFumar.contains("fechaDejarFumar") && prefsFumar.contains("precioPaquete") && prefsFumar.contains(
                "paquetesDia"
            ) && prefsFumar.contains("alquitranMg") && prefsFumar.contains("nicotinaMg") && prefsFumar.contains(
                "monoxidoMg"
            )
        ) {
            val millis = prefsFumar.getLong("fechaDejarFumar", 0L)
            fechaDejarFumar = Calendar.getInstance().apply { timeInMillis = millis }
            precioPaquete = prefsFumar.getFloat("precioPaquete", 0f).toDouble()
            paquetesDia = prefsFumar.getFloat("paquetesDia", 0f).toDouble()
            alquitranMg = prefsFumar.getFloat("alquitranMg", 0f).toDouble()
            nicotinaMg = prefsFumar.getFloat("nicotinaMg", 0f).toDouble()
            monoxidoMg = prefsFumar.getFloat("monoxidoMg", 0f).toDouble()
            true
        } else false
    }

    private fun pedirDatosEsenciales() {
        val scroll = ScrollView(this)
        val layout = LinearLayout(this)
        layout.orientation = LinearLayout.VERTICAL
        layout.setPadding(16)
        scroll.addView(layout)
        setContentView(scroll)
        activarModoInmersivo()
        val fechaBtn = Button(this)
        fechaBtn.text = "... Seleccionar Fecha / Hora En La Que Dejaste De Fumar ..."
        val fechaTexto = TextView(this)
        fechaTexto.text = ""
        layout.addView(fechaBtn)
        layout.addView(fechaTexto)
        fechaBtn.setOnClickListener {
            val now = Calendar.getInstance()
            DatePickerDialog(
                this, { _, y, m, d ->
                    TimePickerDialog(this, { _, h, min ->
                        fechaDejarFumar = Calendar.getInstance().apply { set(y, m, d, h, min, 0) }
                        fechaTexto.text =
                            SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(
                                fechaDejarFumar!!.time
                            )
                    }, now.get(Calendar.HOUR_OF_DAY), now.get(Calendar.MINUTE), true).show()
                }, now.get(Calendar.YEAR), now.get(Calendar.MONTH), now.get(Calendar.DAY_OF_MONTH)
            ).show()
        }
        fun createInput(hint: String): EditText {
            val e = EditText(this)
            e.hint = hint
            e.inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
            layout.addView(e)
            return e
        }

        val precioInput = createInput("Precio Del Paquete En ( € )...")
        val paquetesInput = createInput("Paquetes Que Te Fumabas Al Día...")
        val alquitranInput = createInput("Alquitrán En ( mg )...")
        val nicotinaInput = createInput("Nicotina En ( mg )...")
        val monoxidoInput = createInput("Monóxido De Carbono En ( mg )...")
        val guardarBtn = Button(this)
        guardarBtn.text = "Guardar Datos"
        if (ttsEnabled) {
            Handler(Looper.getMainLooper()).postDelayed({
                tts?.speak(
                    "Selecciona Fecha Y Hora En La Que Dejaste De Fumar Y Los Demas Datos. Para Decimales Usa El Punto Decimal.",
                    TextToSpeech.QUEUE_FLUSH,
                    null,
                    "ttsPdfId"
                )
            }, 150)
        }
        layout.addView(guardarBtn)
        guardarBtn.setOnClickListener {
            if (fechaDejarFumar != null && precioInput.text.isNotEmpty() && paquetesInput.text.isNotEmpty() && alquitranInput.text.isNotEmpty() && nicotinaInput.text.isNotEmpty() && monoxidoInput.text.isNotEmpty()) {
                precioPaquete = precioInput.text.toString().toDouble()
                paquetesDia = paquetesInput.text.toString().toDouble()
                alquitranMg = alquitranInput.text.toString().toDouble()
                nicotinaMg = nicotinaInput.text.toString().toDouble()
                monoxidoMg = monoxidoInput.text.toString().toDouble()
                prefsFumar.edit().apply {
                    putLong("fechaDejarFumar", fechaDejarFumar!!.timeInMillis)
                    putFloat("precioPaquete", precioPaquete.toFloat())
                    putFloat("paquetesDia", paquetesDia.toFloat())
                    putFloat("alquitranMg", alquitranMg.toFloat())
                    putFloat("nicotinaMg", nicotinaMg.toFloat())
                    putFloat("monoxidoMg", monoxidoMg.toFloat())
                    apply()
                }
                if (ttsEnabled) {
                    tts?.speak(
                        "Okey; Todo Está Dispuesto.", TextToSpeech.QUEUE_FLUSH, null, "ttsPdfId"
                    )
                }
                inicializarUI()
            } else {
                if (ttsEnabled) {
                    tts?.speak(
                        "No Te Dejes Campos En Blanco.", TextToSpeech.QUEUE_FLUSH, null, "ttsPdfId"
                    )
                }
                Toast.makeText(this, "Rellena Todos Los Campos", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private val lluviaRunnable = object : Runnable {
        override fun run() {
            lanzarPaquete()
            val delay = (10000L..25000L).random()
            lluviaHandler.postDelayed(this, delay)
        }
    }

    private fun getPorcentajeLuna(fecha: Calendar): Int {
        val zoned = java.time.ZonedDateTime.of(
            fecha.get(Calendar.YEAR),
            fecha.get(Calendar.MONTH) + 1,
            fecha.get(Calendar.DAY_OF_MONTH),
            Calendar.getInstance().get(Calendar.HOUR_OF_DAY),
            Calendar.getInstance().get(Calendar.MINUTE),
            Calendar.getInstance().get(Calendar.SECOND),
            0,
            java.time.ZoneId.systemDefault()
        )
        val frac = MoonIllumination.compute().on(zoned.toInstant()).execute().fraction
        return (frac * 100.0).toInt()
    }

    private fun activarModoInmersivo() {
        window.decorView.systemUiVisibility =
            (View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_LAYOUT_STABLE)
    }

    private fun inicializarUI() {
        val labels = arrayOf(
            "Fecha Actual Y % Luna",                  // 0
            "Fecha De Dejar De Fumar Y % Luna",       // 1
            "Tiempo Que Llevas Sin Fumar ",           // 2
            "Tiempo No Desperdiciado Fumando",        // 3
            "Tiempo Ganado En Salud",                 // 4
            "Cigarros No Fumados Por Día",            // 5
            "Paquetes No Fumados Por Día",            // 6
            "Precio Del Paquete De Tabaco ( € )",     // 7
            "Precio Por Cigarro ( € Y Ptas )",        // 8
            "Ahorro Por Día En ( € )",                // 9
            "Ahorro Por Semana En ( € )",             // 10
            "Ahorro Por Año En ( € )",                // 11
            "Ahorro Total En ( € )",                  // 12
            "Total Ahorrado Tabaco + Mecheros",       // 13
            "Cigarros Que No Te Has Fumado",          // 14
            "Paquetes Que No Te Has Fumado",          // 15
            "Cartones Que No Te Has Fumado",          // 16
            "Mecheros No Usados Y Precio Total ( € )",// 17
            "Longitud NF Total ( Km A Lo Ancho )",    // 18
            "Longitud NF Total ( Km A Lo Largo)",     // 19
            "Superficie Ocupada ( m² )",              // 20
            "Torre De Paquetes Uno Sobre Otro",       // 21
            "Cantidad Agua En Paquetes ( Litros )",   // 22
            "Peso Paquetes NF Vacios ( Kg )",         // 23
            "Peso Paquetes NF Llenos ( Kg )",         // 24
            "Tiradas Ceniza Y Peso Ceniza En ( Kg )", // 25
            "Caladas Totales Que No Has Dado",        // 26
            "Globos Inflados X Caladas No Dadas",     // 27
            "Peso De Tabaco No Consumido En ( Kg )",  // 28
            "Alquitrán ( g )",                        // 29
            "Nicotina ( g )",                         // 30
            "Monóxido De Carbono CO2 ( g )"           // 31
        )
        grid = GridLayout(this)
        grid.columnCount = 4
        grid.rowCount = (labels.size + 3) / 4
        rootLayout = FrameLayout(this)
        rootLayout.setBackgroundColor(0xFF000000.toInt())
        val gridParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT
        )
        grid.layoutParams = gridParams
        rootLayout.addView(grid)
        setContentView(rootLayout)
        activarModoInmersivo()
        val valueViews = mutableListOf<TextView>()
        for (i in labels.indices) {
            val cell = LinearLayout(this)
            cell.orientation = LinearLayout.VERTICAL
            val params = GridLayout.LayoutParams().apply {
                width = 0
                height = 0
                columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
                rowSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
                setMargins(2, 2, 2, 2)
            }
            cell.layoutParams = params
            cell.setBackgroundResource(R.drawable.celda_borde_gris)
            cell.setPadding(4)
            val labelTV = TextView(this)
            labelTV.text = labels[i]
            labelTV.setTextColor(0xFF00B4B4.toInt())
            labelTV.textSize = 12f
            labelTV.setSingleLine(true)
            labelTV.setHorizontallyScrolling(false)
            labelTV.ellipsize = null
            labelTV.setAutoSizeTextTypeWithDefaults(TextView.AUTO_SIZE_TEXT_TYPE_UNIFORM)
            labelTV.setAutoSizeTextTypeUniformWithConfiguration(
                8, 12, 1, TypedValue.COMPLEX_UNIT_SP
            )
            val valueTV = TextView(this)
            valueTV.text = ""
            valueTV.setTextColor(0xFFFFFFFF.toInt())
            valueTV.textSize = 14f
            valueTV.gravity = Gravity.END
            valueTV.setOnClickListener {
                if (ttsEnabled) {
                    tts?.stop()
                    val texto = "${labelTV.text}: ${valueTV.text}"
                    tts?.speak(texto, TextToSpeech.QUEUE_FLUSH, null, null)
                }
            }
            cell.addView(labelTV)
            cell.addView(valueTV)
            grid.addView(cell)
            valueViews.add(valueTV)
        }
        this.valueViews = valueViews
        handler.post(updateRunnable)
        lluviaHandler.post(lluviaRunnable)
    }

    fun formatoTiempo(segundosTotales: Double): String {
        var segundos = segundosTotales.toLong()
        val diasAños = 365L
        val anios = segundos / (diasAños * 24 * 60 * 60)
        segundos %= (diasAños * 24 * 60 * 60)
        val dias = segundos / (24 * 60 * 60)
        segundos %= (24 * 60 * 60)
        val horas = segundos / (60 * 60)
        segundos %= (60 * 60)
        val minutos = segundos / 60
        segundos %= 60
        return "${anios} a ${dias} d ${horas} h ${minutos} m ${segundos} s"
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS && ttsEnabled) {
            val result = tts?.setLanguage(tts.defaultLanguage ?: Locale.getDefault())
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                ttsEnabled = false
            } else {
                tts?.setSpeechRate(0.9f)
            }
        }
    }

    private fun actualizarValores() {
        val ahora = Calendar.getInstance()
        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())
        valueViews[0].text = "${sdf.format(ahora.time)} - Luna: ${getPorcentajeLuna(ahora)}%"
        valueViews[1].text = "${
            SimpleDateFormat(
                "dd/MM/yyyy HH:mm", Locale.getDefault()
            ).format(fechaDejarFumar!!.time)
        } - Luna: ${getPorcentajeLuna(fechaDejarFumar!!)}%"
        val diff = ahora.timeInMillis - fechaDejarFumar!!.timeInMillis
        val seg = ((diff / 1000) % 60).toInt()
        val min = ((diff / (1000 * 60)) % 60).toInt()
        val hrs = ((diff / (1000 * 60 * 60)) % 24).toInt()
        val diasTotales = diff.toDouble() / (1000 * 60 * 60 * 24)
        val anos = (diasTotales / 365).toInt()
        val dias = (diasTotales % 365).toInt()
        val añosSinFumar = anos.toFloat()
        val maxAños = 5f
        val colorInicio = intArrayOf(0, 0, 0)
        val colorFinal = intArrayOf(0, 55, 0)
        val factor = (añosSinFumar / maxAños).coerceIn(0f, 1f)
        val r = (colorInicio[0] + factor * (colorFinal[0] - colorInicio[0])).toInt()
        val g = (colorInicio[1] + factor * (colorFinal[1] - colorInicio[1])).toInt()
        val b = (colorInicio[2] + factor * (colorFinal[2] - colorInicio[2])).toInt()
        grid.setBackgroundColor(Color.rgb(r, g, b))
        valueViews[2].text = "${anos}a ${dias}d ${hrs}h ${min}m ${seg}s"
        val segundosTotales = (paquetesDia * 20 * diasTotales * 330)
        val segundosTotaless = (paquetesDia * 20 * diasTotales * 660)
        valueViews[3].text = formatoTiempo(segundosTotales)
        valueViews[4].text = formatoTiempo(segundosTotaless)
        val cigarrosDia = paquetesDia * 20
        valueViews[5].text = String.format(Locale.getDefault(), "%.5f Cig", cigarrosDia)
        valueViews[6].text = String.format(Locale.getDefault(), "%.5f Paq", paquetesDia)
        valueViews[7].text = String.format("%.5f €", precioPaquete)
        val eurporcig = precioPaquete / 20
        val ptasporcig = eurporcig * 166.386
        valueViews[8].text = String.format("%.5f € - %.5f Ptas", eurporcig, ptasporcig)
        val ahorroDia = precioPaquete * paquetesDia
        valueViews[9].text = String.format("%.5f €", ahorroDia)
        valueViews[10].text = String.format("%.5f €", ahorroDia * 7)
        valueViews[11].text = String.format("%.5f €", ahorroDia * 365)
        valueViews[12].text = String.format("%.5f €", ahorroDia * diasTotales)
        val totalCigarros = cigarrosDia * diasTotales
        val mecheros = totalCigarros / 1500
        val precioMecheros = mecheros * 1.25
        valueViews[13].text = String.format("%.5f €", (ahorroDia * diasTotales) + precioMecheros)
        val totalPaquetes = paquetesDia * diasTotales
        valueViews[14].text = String.format("%.5f Cig", totalCigarros)
        valueViews[15].text = String.format("%.5f Paq", totalPaquetes)
        valueViews[16].text = String.format("%.5f Car", totalPaquetes / 10)
        valueViews[17].text = String.format("%.5f Bics - %.5f €", mecheros, precioMecheros)
        valueViews[18].text = String.format("%.5f Km", ((8 * totalCigarros) / 1000.0) / 1000)
        valueViews[19].text = String.format("%.5f Km", (totalCigarros * 0.056) / 1000)
        valueViews[20].text = String.format("%.5f m²", ((8 * totalCigarros) / 1000.0) * 0.083)
        val torreMetros = totalPaquetes * 0.085
        val torreKm = torreMetros / 1000.0
        valueViews[21].text = String.format(
            "%.5f m  -  %.6f Km", torreMetros, torreKm
        )
        valueViews[22].text = String.format("%.5f l", totalPaquetes * 0.10285)
        valueViews[23].text = String.format("%.5f Kg", totalPaquetes * 0.008)
        valueViews[24].text = String.format("%.5f Kg", totalPaquetes * 0.027)
        val tiradasCeniza = totalCigarros * 9
        val pesoCenizaTotal = totalCigarros * 0.084 / 1000
        valueViews[25].text = String.format("%.5f - %.5f Kg", tiradasCeniza, pesoCenizaTotal)
        valueViews[26].text = String.format("%.5f Cal", totalCigarros * 15.0)
        valueViews[27].text = String.format("%.5f Globos", (totalCigarros * 15) / 16)
        val pesoTabaco = totalCigarros * 0.0008
        valueViews[28].text = String.format("%.5f Kg", pesoTabaco)
        val alquitranGr = alquitranMg / 1000 * totalCigarros
        val nicotinaGr = nicotinaMg / 1000 * totalCigarros
        val monoxidoGr = monoxidoMg / 1000 * totalCigarros
        valueViews[29].text = String.format("%.5f g", alquitranGr)
        valueViews[30].text = String.format("%.5f g", nicotinaGr)
        valueViews[31].text = String.format("%.5f g", monoxidoGr)
    }

    fun generarTextoTTS(): String {
        fun formatoLegible(segundosTotales: Double): String {
            var seg = segundosTotales.toLong()
            val anios = seg / (365 * 24 * 60 * 60)
            seg %= (365 * 24 * 60 * 60)
            val dias = seg / (24 * 60 * 60)
            seg %= (24 * 60 * 60)
            val horas = seg / (60 * 60)
            seg %= (60 * 60)
            val minutos = seg / 60
            seg %= 60
            return "${anios} Años, ${dias} Días, ${horas} Horas, ${minutos} Minutos y ${seg} Segundos"
        }

        fun capitalizarCadaPalabra(texto: String): String {
            return texto.split(" ")
                .joinToString(" ") { it.replaceFirstChar { c -> c.uppercaseChar() } }
        }

        val texto = StringBuilder()
        val ahora = Calendar.getInstance()
        val sdfHora = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        val sdfFecha = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val porcentajeActual = getPorcentajeLuna(ahora)
        texto.append("Son Las ${sdfHora.format(ahora.time)} Del Día ${sdfFecha.format(ahora.time)}; Luna Al $porcentajeActual%.\n\n")
        val diffSegundos =
            (Calendar.getInstance().timeInMillis - fechaDejarFumar!!.timeInMillis) / 1000.0
        val tiempoSinFumarLegible = formatoLegible(diffSegundos)
        texto.append(capitalizarCadaPalabra("Eso Significa Que Llevas Aproximadamente; $tiempoSinFumarLegible Sin Encender Un Cigarro.\n\n"))
        val diasTotales = diffSegundos / (24 * 60 * 60)
        val ahorroDia = precioPaquete * paquetesDia
        val ahorroTotal = ahorroDia * diasTotales
        val totalCigarross = paquetesDia * 20 * diasTotales
        val mecheros = totalCigarross / 1500
        val precioMecheros = mecheros * 1.25
        val preciofinal = ahorroTotal + precioMecheros
        texto.append(
            capitalizarCadaPalabra(
                "Has Ahorrado Un Total Aproximado De %.2f Euros Desde Que Dejaste De Fumar, Y Has Dejado De Gastar %.2f Mecheros, Que A 1.25 Euros Cada Uno; Hacen Un Total De %.2f Euros; Que Sumados A El Total De Cigarros No Fumados Nos Da; %.2f Euros Totales Ahorrados.\n\n".format(
                    ahorroTotal, mecheros, precioMecheros, preciofinal
                )
            )
        )
        val totalCigarros = paquetesDia * 20 * diasTotales
        texto.append(
            capitalizarCadaPalabra(
                "Eso Equivale A Haber Evitado Fumar Aproximadamente %.2f Cigarros.\n\n".format(
                    totalCigarros
                )
            )
        )
        val longitudTotalKm = (totalCigarros * 0.056) / 1000.0
        texto.append(
            capitalizarCadaPalabra(
                "Si Pusieras Todos Esos Cigarros, Uno Seguido Del Otro; Alcanzarías Aproximadamente %.2f Kilómetros De Largo.\n\n".format(
                    longitudTotalKm
                )
            )
        )
        val alquitranGr = alquitranMg / 1000 * totalCigarros
        val nicotinaGr = nicotinaMg / 1000 * totalCigarros
        val monoxidoGr = monoxidoMg / 1000 * totalCigarros
        texto.append(
            capitalizarCadaPalabra(
                "Has Evitado Consumir Aproximadamente %.2f Gramos De Alquitrán, %.2f Gramos De Nicotina Y %.2f Gramos De Monóxido De Carbono.".format(
                    alquitranGr, nicotinaGr, monoxidoGr
                )
            )
        )
        return texto.toString()
    }

    private val ttsRunnable = object : Runnable {
        override fun run() {
            if (!ttsEnabled) return
            val textoConversacional = generarTextoTTS()
            handler.postDelayed({
                tts?.stop()
                tts?.speak(textoConversacional, TextToSpeech.QUEUE_FLUSH, null, null)
            }, 1000)
            handler.postDelayed(this, 15 * 60 * 1000L)
        }
    }

    override fun onDestroy() {
        handler.removeCallbacks(updateRunnable)
        handler.removeCallbacks(ttsRunnable)
        lluviaHandler.removeCallbacks(lluviaRunnable)
        mediaPlayer.stop()
        mediaPlayer.release()
        tts?.shutdown()
        super.onDestroy()
    }

    override fun onPause() {
        super.onPause()
        mediaPlayer.pause()
        tts?.stop()
        handler.removeCallbacks(ttsRunnable)
    }

    override fun onResume() {
        super.onResume()
        mediaPlayer.start()
        if (ttsEnabled) {
            handler.postDelayed(ttsRunnable, 15 * 60 * 1000L)
        }
    }
}