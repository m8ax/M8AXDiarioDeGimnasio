package com.mviiiax.m8ax_diariogimnasio

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.text.Editable
import android.text.InputFilter
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class GimnasioAdapter(
    private var lista: List<Gimnasio>,
    private val dao: GimnasioDao,
    private val context: Context,
    private val callbackHablar: (Gimnasio) -> Unit,
    private val callbackDecir: (String) -> Unit
) : RecyclerView.Adapter<GimnasioAdapter.GimnasioViewHolder>() {
    inner class GimnasioViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val etValor: EditText = itemView.findViewById(R.id.etValor)
        val tvFechaHora: TextView = itemView.findViewById(R.id.tvFechaHora)
        val etDiario: EditText = itemView.findViewById(R.id.etDiario)
        val tvContador: TextView = itemView.findViewById(R.id.tvContador)
        var watcherValor: TextWatcher? = null
        var watcherDiario: TextWatcher? = null
    }

    val items: List<Gimnasio>
        get() = lista

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GimnasioViewHolder {
        val view =
            LayoutInflater.from(parent.context).inflate(R.layout.item_gimnasio, parent, false)
        return GimnasioViewHolder(view)
    }

    override fun getItemCount(): Int = lista.size
    override fun onBindViewHolder(holder: GimnasioViewHolder, position: Int) {
        holder.tvFechaHora.isSelected = true
        val pos = holder.bindingAdapterPosition
        if (pos == RecyclerView.NO_POSITION) return
        val registro = lista[pos]
        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val hoy = sdf.format(Date())
        val fechaRegistro = registro.fechaHora.substring(0, 10)
        val esHoy = fechaRegistro == hoy
        holder.watcherValor?.let { holder.etValor.removeTextChangedListener(it) }
        holder.etValor.setText("")
        holder.etValor.isEnabled = false
        holder.etValor.isFocusable = true
        holder.etValor.isCursorVisible = true
        holder.etValor.isLongClickable = true
        holder.etValor.setText(registro.valor.toString())
        holder.etValor.isEnabled = esHoy
        val watcherV = object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val nuevoValor = s.toString().toIntOrNull()
                if (nuevoValor != null) {
                    val valorFinal = if (nuevoValor > 960) 960 else nuevoValor
                    if (valorFinal != nuevoValor) {
                        holder.etValor.setText(valorFinal.toString())
                        holder.etValor.setSelection(holder.etValor.text.length)
                        callbackDecir("Máximo Permitido; 960 Minutos. O Piensas Quedarte A Vivir En El Gimnasio.")
                        Toast.makeText(
                            context, "Máximo Permitido 960 Minutos", Toast.LENGTH_LONG
                        ).show()
                    }
                    registro.valor = valorFinal
                    dao.update(registro)
                    (context as? MainActivity)?.contadorActualizarTemp = 595
                }
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        }
        holder.etValor.addTextChangedListener(watcherV)
        holder.watcherValor = watcherV
        holder.watcherDiario?.let { holder.etDiario.removeTextChangedListener(it) }
        holder.etDiario.setText(registro.diario)
        holder.tvContador.text = "${registro.diario.length} / 1000"
        holder.etDiario.filters = arrayOf(InputFilter.LengthFilter(1000))
        if (esHoy) {
            holder.etDiario.setOnClickListener(null)
            holder.etDiario.isEnabled = true
            holder.etDiario.isFocusable = true
            holder.etDiario.isFocusableInTouchMode = true
            holder.etDiario.isCursorVisible = true
            holder.etDiario.isLongClickable = false
            holder.etDiario.setTextColor(Color.BLACK)
            holder.etDiario.setTextIsSelectable(false)
            holder.etDiario.post {
                holder.etDiario.setTextIsSelectable(true)
            }
        } else {
            holder.etDiario.isEnabled = true
            holder.etDiario.isFocusable = false
            holder.etDiario.isFocusableInTouchMode = false
            holder.etDiario.isCursorVisible = false
            holder.etDiario.isLongClickable = false
            holder.etDiario.setTextColor(Color.parseColor("#0D47A1"))
            holder.etDiario.setOnClickListener {
                if (registro.diario.isNotEmpty()) {
                    callbackHablar(registro)
                }
            }
        }
        if (!esHoy) {
            holder.etDiario.isLongClickable = true
            holder.etDiario.setOnLongClickListener {
                val numeroRegistro = lista.size - pos
                callbackDecir("¿Estás Seguro De Que Quieres Borrar El Registro $numeroRegistro?")
                AlertDialog.Builder(context).setTitle("¿ Borrar Registro ?")
                    .setMessage("¿ Estás Seguro De Que Quieres Borrar El Registro $numeroRegistro ?")
                    .setPositiveButton("Sí") { dialog, _ ->
                        (context as? MainActivity)?.contadorActualizarTemp = 595
                        dao.deleteById(registro.id)
                        lista = lista.filter { it.id != registro.id }
                        notifyDataSetChanged()
                        Toast.makeText(
                            context,
                            "Registro $numeroRegistro, Borrado Correctamente",
                            Toast.LENGTH_SHORT
                        ).show()
                        callbackDecir("Registro $numeroRegistro; Borrado Correctamente.")
                        dialog.dismiss()
                    }.setNegativeButton("No") { dialog, _ ->
                        Toast.makeText(
                            context, "Okey, No Borramos Na De Na...", Toast.LENGTH_SHORT
                        ).show()
                        callbackDecir("Okey, No Borramos Na De Na...")
                        dialog.dismiss()
                    }.show()
                true
            }
        } else {
            holder.etDiario.isLongClickable = false
            holder.etDiario.setOnLongClickListener(null)
        }
        val watcherD = object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val textoActual = s?.toString() ?: ""
                val len = textoActual.length
                holder.tvContador.text = "$len / 1000"
                holder.tvContador.setTypeface(null, Typeface.BOLD)
                when {
                    len <= 500 -> holder.tvContador.setTextColor(Color.parseColor("#2E7D32"))
                    len <= 750 -> holder.tvContador.setTextColor(Color.parseColor("#EF6C00"))
                    else -> holder.tvContador.setTextColor(Color.parseColor("#C62828"))
                }
                registro.diario = textoActual
                dao.update(registro)
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        }
        holder.etDiario.addTextChangedListener(watcherD)
        holder.watcherDiario = watcherD
        val contando = lista.size - pos
        val romano = intToRoman(contando)
        val sdff = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val fecha =
            runCatching { sdff.parse(registro.fechaHora.substring(0, 10)) }.getOrNull() ?: Date()
        val calendar = Calendar.getInstance()
        calendar.time = fecha
        val diaDelAno = calendar.get(Calendar.DAY_OF_YEAR)
        holder.tvFechaHora.setTypeface(null, Typeface.BOLD)
        holder.tvFechaHora.text = "$contando-$romano-DA$diaDelAno → ${registro.fechaHora}"
    }

    fun updateData(nuevaLista: List<Gimnasio>) {
        val sdfFecha = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        lista = nuevaLista.sortedByDescending { sdfFecha.parse(it.fechaHora.substring(0, 10)) }
        notifyDataSetChanged()
    }

    companion object {
        fun intToRoman(num: Int): String {
            if (num <= 0) return ""
            val valores = intArrayOf(
                1_000_000,
                900_000,
                500_000,
                400_000,
                100_000,
                90_000,
                50_000,
                40_000,
                10_000,
                9_000,
                5_000,
                4_000,
                1000,
                900,
                500,
                400,
                100,
                90,
                50,
                40,
                10,
                9,
                5,
                4,
                1
            )
            val cadenas = arrayOf(
                "M",
                "CM",
                "D",
                "CD",
                "C",
                "XC",
                "L",
                "XL",
                "X",
                "IX",
                "V",
                "IV",
                "M",
                "CM",
                "D",
                "CD",
                "C",
                "XC",
                "L",
                "XL",
                "X",
                "IX",
                "V",
                "IV",
                "I"
            )
            var resultado = StringBuilder()
            var decimal = num
            while (decimal > 0) {
                for (i in valores.indices) {
                    if (decimal >= valores[i]) {
                        if (valores[i] > 1000) cadenas[i].forEach { c ->
                            resultado.append(c).append('\u0305')
                        } else resultado.append(cadenas[i])
                        decimal -= valores[i]
                        break
                    }
                }
            }
            return resultado.toString()
        }
    }
}
