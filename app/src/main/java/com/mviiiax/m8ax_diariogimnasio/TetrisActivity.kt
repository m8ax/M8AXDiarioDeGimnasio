package com.mviiiax.m8ax_diariogimnasio

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.media.MediaPlayer
import android.os.Bundle
import android.os.Handler
import android.speech.tts.TextToSpeech
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.util.Locale
import kotlin.random.Random

class TetrisActivity : AppCompatActivity() {
    private lateinit var tetrisView: TetrisView
    private lateinit var handler: Handler
    private var delay = 750L
    private var tts: TextToSpeech? = null
    private var ttsEnabled: Boolean = false
    private var mediaPlayer: MediaPlayer? = null
    lateinit var tvLineas: TextView
    lateinit var tvPuntos: TextView
    lateinit var tvNivel: TextView
    fun adjustSpeed(linesCleared: Int) {
        delay = 750L - (linesCleared * 5L)
        if (delay < 200L) delay = 200L
    }

    fun pauseGameFor2Seconds() {
        handler.removeCallbacksAndMessages(null)
        Handler(mainLooper).postDelayed({
            handler.postDelayed(object : Runnable {
                override fun run() {
                    if (!tetrisView.gameOver) {
                        if (!tetrisView.update()) tetrisView.setGameOver()
                    }
                    handler.postDelayed(this, delay)
                }
            }, delay)
        }, 2000)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tetris)
        tvLineas = findViewById(R.id.txtLineas)
        tvPuntos = findViewById(R.id.txtPuntos)
        tvNivel = findViewById(R.id.txtNivel)
        mediaPlayer = MediaPlayer.create(this, R.raw.m8axsonidofondo)
        mediaPlayer?.isLooping = true
        mediaPlayer?.setVolume(0.5f, 0.5f)
        mediaPlayer?.start()
        val container = findViewById<FrameLayout>(R.id.gameContainer)
        ttsEnabled = intent.getBooleanExtra("ttsEnabled", true)
        tts = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale.getDefault()
                tts?.setSpeechRate(0.9f)
            }
        }
        tetrisView = TetrisView(this)
        container.addView(tetrisView)
        tetrisView.initTts(this, ttsEnabled)
        val btnLeft = findViewById<ImageButton>(R.id.btnLeft)
        val btnRight = findViewById<ImageButton>(R.id.btnRight)
        val btnRotate = findViewById<ImageButton>(R.id.btnRotate)
        val btnDown = findViewById<ImageButton>(R.id.btnDown)
        btnLeft.setOnClickListener { if (!tetrisView.gameOver) tetrisView.moveLeft() else tetrisView.restart() }
        btnRight.setOnClickListener { if (!tetrisView.gameOver) tetrisView.moveRight() else tetrisView.restart() }
        btnRotate.setOnClickListener { if (!tetrisView.gameOver) tetrisView.rotate() else tetrisView.restart() }
        btnDown.setOnClickListener { if (!tetrisView.gameOver) tetrisView.drop() else tetrisView.restart() }
        handler = Handler(mainLooper)
        handler.postDelayed(object : Runnable {
            override fun run() {
                if (!tetrisView.gameOver) {
                    if (!tetrisView.update()) tetrisView.setGameOver()
                }
                handler.postDelayed(this, delay)
            }
        }, delay)
    }

    override fun onDestroy() {
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
        tts?.stop()
        tts?.shutdown()
        handler.removeCallbacksAndMessages(null)
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

class TetrisView(context: Context) : View(context) {
    private val cols = 10
    private val rows = 20
    private var blockWidth = 0
    private var blockHeight = 0
    private var offsetY = 0
    private val paint = Paint()
    private val grid = Array(rows) { IntArray(cols) { 0 } }
    private val colors: Array<Array<Int>> = Array(rows) { Array(cols) { Color.BLACK } }
    private var tts: TextToSpeech? = null
    private var ttsEnabled: Boolean = false
    var gameOver = false
        private set
    private var currentPiece = Tetromino.random()
    private var pieceX = cols / 2 - 1
    private var pieceY = 0
    private var currentColor = currentPiece.color
    private var cuenta = 0
    private var puntitos = 0
    private var level = 1
    private var showLevelText = false
    private val levelDisplayTime = 2000L
    private val ttsMessages = arrayOf(
        "Ahí Va Otra",
        "Cuidado Con Esa",
        "Perfecto O Casi",
        "Se Resbala Otra Vez",
        "Caída Libre Nivel Experto",
        "Ups Otra Vez",
        "No Era Esa Pieza",
        "Mira Cómo Cae",
        "A Punto De Encajar",
        "Eso No Entrará",
        "Sorpresa Total",
        "Lo Veo Llegar",
        "Ni Lo Intentes",
        "Ahí Se Queda",
        "Pieza Rebelde",
        "Casi Perfecto",
        "Uy Uy Uy",
        "Se Desliza Bien",
        "No Tan Rápido",
        "Eso Fue Cerca",
        "Volando Alto",
        "Caída Controlada",
        "Vaya Giro",
        "No Lo Esperaba",
        "Al Límite",
        "Ahí Está",
        "Eso Fue Inesperado",
        "Más Rápido",
        "A Toda Velocidad",
        "Se Escapa",
        "Queda Justo",
        "Eso Dolió",
        "Perfectamente Mal",
        "Casi Lo Haces",
        "Otra Intento",
        "Desastre Anunciado",
        "Bien Jugado",
        "Ahí Lo Tienes",
        "Todo Bajo Control",
        "Casi Se Va",
        "Otra Que No Entra",
        "Esto Es Tenso",
        "Ups Lo Tocó",
        "Se Escapó",
        "No Tan Mal",
        "Eso Se Queda",
        "Justo A Tiempo",
        "Demasiado Lento",
        "Ahí Lo Ves",
        "Se Resbaló",
        "Queda Perfecto",
        "Casi Cae",
        "Otra Que Desafía",
        "Giro Imprevisto",
        "Eso Fue Rápido",
        "Demasiado Perfecto",
        "Se Va Sin Control",
        "A Punto De Encajar",
        "Cae Con Estilo",
        "Muy Bien",
        "No Tan Bien",
        "Se Va De Largo",
        "Ahí Va",
        "Caída Peligrosa",
        "Otra Que Escapa",
        "Eso Fue Divertido",
        "No Lo Esperaba",
        "Se Desliza",
        "Perfectamente Mal",
        "Demasiado Cerca",
        "Giro Salvaje",
        "Ahí Lo Ves",
        "Se Escapa Otra Vez",
        "Casi Perfecto",
        "A Toda Velocidad",
        "Demasiado Lento",
        "Ups Lo Tocó",
        "Caída Libre",
        "Se Resbaló",
        "Ahí Está",
        "Todo Bajo Control",
        "Muy Bien Hecho",
        "Casi Lo Haces",
        "Giro Sorprendente",
        "Ahí Lo Tienes",
        "Eso Fue Divertido",
        "Se Va Sin Control",
        "Otro Intento",
        "Perfectamente Mal",
        "Casi Cae",
        "Se Escapa",
        "Giro Imprevisto",
        "Demasiado Perfecto",
        "No Tan Mal",
        "Ahí Lo Ves",
        "Otra Que Desafía",
        "Caída Peligrosa",
        "Se Resbala Otra Vez",
        "Casi Perfecto",
        "A Punto De Encajar",
        "Muy Bien",
        "Se Va De Largo",
        "Ahí Va",
        "Caída Libre Nivel Experto",
        "Ups Otra Vez",
        "No Era Esa Pieza",
        "Mira Cómo Cae",
        "Eso No Entrará",
        "Sorpresa Total",
        "Lo Veo Llegar",
        "Ni Lo Intentes",
        "Ahí Se Queda",
        "Pieza Rebelde",
        "Casi Perfecto",
        "Uy Uy Uy",
        "Se Desliza Bien",
        "No Tan Rápido",
        "Eso Fue Cerca",
        "Volando Alto",
        "Caída Controlada",
        "Vaya Giro",
        "No Lo Esperaba",
        "Al Límite",
        "Ahí Está",
        "Eso Fue Inesperado",
        "Más Rápido",
        "A Toda Velocidad",
        "Se Escapa",
        "Queda Justo",
        "Eso Dolió",
        "Perfectamente Mal",
        "Casi Lo Haces",
        "Otra Intento",
        "Desastre Anunciado",
        "Bien Jugado",
        "Ahí Lo Tienes",
        "Todo Bajo Control",
        "Casi Se Va",
        "Otra Que No Entra",
        "Esto Es Tenso",
        "Ups Lo Tocó",
        "Se Escapó",
        "No Tan Mal",
        "Eso Se Queda",
        "Justo A Tiempo",
        "Demasiado Lento",
        "Ahí Lo Ves",
        "Se Resbaló",
        "Queda Perfecto",
        "Casi Cae",
        "Otra Que Desafía",
        "Giro Imprevisto",
        "Eso Fue Rápido",
        "Demasiado Perfecto",
        "Se Va Sin Control",
        "A Punto De Encajar",
        "Cae Con Estilo",
        "Muy Bien",
        "No Tan Bien",
        "Se Va De Largo",
        "Ahí Va",
        "Caída Peligrosa",
        "Otra Que Escapa",
        "Eso Fue Divertido",
        "No Lo Esperaba",
        "Se Desliza",
        "Perfectamente Mal",
        "Demasiado Cerca",
        "Giro Salvaje",
        "Ahí Lo Ves",
        "Se Escapa Otra Vez",
        "Casi Perfecto",
        "A Toda Velocidad",
        "Demasiado Lento",
        "Ups Lo Tocó",
        "Caída Libre",
        "Se Resbaló",
        "Ahí Está",
        "Todo Bajo Control",
        "Muy Bien Hecho",
        "Casi Lo Haces",
        "Giro Sorprendente",
        "Ahí Lo Tienes",
        "Eso Fue Divertido",
        "Se Va Sin Control",
        "Otro Intento",
        "Perfectamente Mal",
        "Casi Cae",
        "Se Escapa",
        "Giro Imprevisto",
        "Demasiado Perfecto",
        "No Tan Mal",
        "Ahí Lo Ves"
    )

    init {
        paint.style = Paint.Style.FILL
    }

    fun initTts(context: Context, enabled: Boolean) {
        ttsEnabled = enabled
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale.getDefault()
                tts?.setSpeechRate(0.9f)
            }
        }
    }

    private fun showLevelMessage() {
        showLevelText = true
        invalidate()
        Handler(context.mainLooper).postDelayed({
            showLevelText = false
            invalidate()
        }, levelDisplayTime)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        blockHeight = height / rows
        blockWidth = width / cols
        offsetY = (height - blockHeight * rows) / 2
        canvas.drawColor(Color.BLACK)
        for (r in 0 until rows) {
            for (c in 0 until cols) {
                paint.color = colors[r][c]
                canvas.drawRect(
                    (c * blockWidth).toFloat(),
                    (r * blockHeight + offsetY).toFloat(),
                    ((c + 1) * blockWidth).toFloat(),
                    ((r + 1) * blockHeight + offsetY).toFloat(),
                    paint
                )
            }
        }
        if (!gameOver) {
            paint.color = currentColor
            for (r in currentPiece.shape.indices) {
                for (c in currentPiece.shape[r].indices) {
                    if (currentPiece.shape[r][c] != 0) {
                        val x = pieceX + c
                        val y = pieceY + r
                        if (y >= 0) canvas.drawRect(
                            (x * blockWidth).toFloat(),
                            (y * blockHeight + offsetY).toFloat(),
                            ((x + 1) * blockWidth).toFloat(),
                            ((y + 1) * blockHeight + offsetY).toFloat(),
                            paint
                        )
                    }
                }
            }
        }
        if (showLevelText) {
            paint.color = Color.WHITE
            paint.textSize = 80f
            paint.textAlign = Paint.Align.CENTER
            canvas.drawText("NIVEL $level", (width / 2).toFloat(), (height / 2).toFloat(), paint)
        }
        if (gameOver) {
            paint.color = Color.WHITE
            paint.textSize = 100f
            paint.textAlign = Paint.Align.CENTER
            if (ttsEnabled) tts?.speak(
                "FIN DEL JUEGO!", TextToSpeech.QUEUE_FLUSH, null, "ttsTetrisId"
            )
            canvas.drawText("FIN DEL JUEGO", (width / 2).toFloat(), (height / 2).toFloat(), paint)
        }
    }

    fun moveLeft() {
        if (canMove(pieceX - 1, pieceY, currentPiece.shape)) pieceX -= 1; invalidate()
    }

    fun moveRight() {
        if (canMove(pieceX + 1, pieceY, currentPiece.shape)) pieceX += 1; invalidate()
    }

    fun rotate() {
        val rotated = Array(currentPiece.shape[0].size) { r ->
            IntArray(currentPiece.shape.size) { c ->
                currentPiece.shape[currentPiece.shape.size - 1 - c][r]
            }
        }
        if (canMove(pieceX, pieceY, rotated)) currentPiece.shape = rotated
        invalidate()
    }

    fun drop() {
        while (canMove(
                pieceX, pieceY + 1, currentPiece.shape
            )
        ) pieceY += 1; mergePiece(); invalidate()
    }

    fun update(): Boolean {
        return if (canMove(pieceX, pieceY + 1, currentPiece.shape)) {
            pieceY += 1
            invalidate()
            true
        } else mergePiece()
    }

    private fun mergePiece(): Boolean {
        if (ttsEnabled) {
            val message = ttsMessages.random()
            tts?.speak(message, TextToSpeech.QUEUE_FLUSH, null, "ttsTetrisId")
        }
        for (r in currentPiece.shape.indices) {
            for (c in currentPiece.shape[r].indices) {
                if (currentPiece.shape[r][c] != 0) {
                    val x = pieceX + c
                    val y = pieceY + r
                    if (y < 0 || (y >= 0 && grid[y][x] != 0)) {
                        setGameOver()
                        return false
                    }
                    grid[y][x] = 1
                    colors[y][x] = currentColor
                }
            }
        }
        clearLines()
        currentPiece = Tetromino.random()
        pieceX = cols / 2 - 1
        pieceY = 0
        currentColor = currentPiece.color
        invalidate()
        return true
    }

    private fun clearLines() {
        var r = rows - 1
        var linesClearedThisTurn = 0
        while (r >= 0) {
            if (grid[r].all { it != 0 }) {
                linesClearedThisTurn++
                for (i in r downTo 1) {
                    grid[i] = grid[i - 1].clone()
                    colors[i] = colors[i - 1].copyOf()
                }
                grid[0] = IntArray(cols)
                colors[0] = Array(cols) { Color.BLACK }
            } else r--
        }
        if (linesClearedThisTurn > 0) {
            cuenta += linesClearedThisTurn
            puntitos = cuenta * 500
            (context as TetrisActivity).tvLineas.text = "Líneas: $cuenta"
            (context as TetrisActivity).tvPuntos.text = "Puntos: $puntitos"
            val previousLevel = level
            level = (cuenta / 10) + 1
            (context as TetrisActivity).tvNivel.text = "Nivel: $level"
            if (level > previousLevel) {
                for (lvl in (previousLevel + 1)..level) {
                    if (ttsEnabled) tts?.speak(
                        "Nivel $lvl", TextToSpeech.QUEUE_ADD, null, "ttsLevelId"
                    )
                    showLevelMessage()
                }
                (context as TetrisActivity).pauseGameFor2Seconds()
            }
            if (ttsEnabled) {
                tts?.speak(
                    "Has Completado $linesClearedThisTurn Línea${if (linesClearedThisTurn > 1) "s" else ""}. Total Líneas; $cuenta. Puntos; $puntitos.",
                    TextToSpeech.QUEUE_ADD,
                    null,
                    "ttsLinesId"
                )
            }
            (context as TetrisActivity).adjustSpeed(cuenta)
        }
    }

    private fun canMove(x: Int, y: Int, shape: Array<IntArray>): Boolean {
        for (r in shape.indices) {
            for (c in shape[r].indices) {
                if (shape[r][c] != 0) {
                    val newX = x + c
                    val newY = y + r
                    if (newX !in 0 until cols || newY >= rows) return false
                    if (newY >= 0 && grid[newY][newX] != 0) return false
                }
            }
        }
        return true
    }

    fun setGameOver() {
        gameOver = true; invalidate()
    }

    fun restart() {
        cuenta = 0
        puntitos = 0
        if (ttsEnabled) tts?.speak(
            "Comenzando Nueva Partida!", TextToSpeech.QUEUE_FLUSH, null, "ttsTetrisId"
        )
        for (r in 0 until rows) {
            grid[r] = IntArray(cols)
            colors[r] = Array(cols) { Color.BLACK }
        }
        gameOver = false
        currentPiece = Tetromino.random()
        pieceX = cols / 2 - 1
        pieceY = 0
        currentColor = currentPiece.color
        (context as TetrisActivity).tvLineas.text = "LÍNEAS - $cuenta"
        (context as TetrisActivity).tvPuntos.text = "PUNTOS - $puntitos"
        (context as TetrisActivity).tvNivel.text = "NIVEL - $level"
        invalidate()
    }
}

class Tetromino(var shape: Array<IntArray>, val color: Int) {
    companion object {
        private val SHAPES = listOf(
            arrayOf(intArrayOf(1, 1, 1, 1)),
            arrayOf(intArrayOf(1, 1), intArrayOf(1, 1)),
            arrayOf(intArrayOf(0, 1, 0), intArrayOf(1, 1, 1)),
            arrayOf(intArrayOf(1, 1, 0), intArrayOf(0, 1, 1)),
            arrayOf(intArrayOf(0, 1, 1), intArrayOf(1, 1, 0)),
            arrayOf(intArrayOf(1, 0, 0), intArrayOf(1, 1, 1)),
            arrayOf(intArrayOf(0, 0, 1), intArrayOf(1, 1, 1))
        )
        private val COLORS = listOf(
            Color.RED,
            Color.GREEN,
            Color.BLUE,
            Color.MAGENTA,
            Color.CYAN,
            Color.YELLOW,
            Color.LTGRAY
        )

        fun random(): Tetromino {
            val shape = SHAPES[Random.nextInt(SHAPES.size)]
            val color = COLORS[Random.nextInt(COLORS.size)]
            return Tetromino(shape.map { it.clone() }.toTypedArray(), color)
        }
    }
}