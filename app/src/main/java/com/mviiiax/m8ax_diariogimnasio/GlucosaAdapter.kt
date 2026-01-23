package com.mviiiax.m8ax_diariogimnasio

import android.graphics.Color
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class GlucosaAdapter(
    private var lista: List<Glucosa>, private val dao: GlucosaDao
) : RecyclerView.Adapter<GlucosaAdapter.GlucosaViewHolder>() {
    inner class GlucosaViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvNumero: TextView = itemView.findViewById(R.id.tvNumero)
        val etValor: EditText = itemView.findViewById(R.id.etValor)
        val tvFechaHora: TextView = itemView.findViewById(R.id.tvFechaHora)
        var textWatcher: TextWatcher? = null
    }

    val items: List<Glucosa>
        get() = lista

    fun updateData(nuevaLista: List<Glucosa>) {
        lista = nuevaLista
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GlucosaViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_glucosa, parent, false)
        return GlucosaViewHolder(view)
    }

    override fun getItemCount(): Int = lista.size

    override fun onBindViewHolder(holder: GlucosaViewHolder, position: Int) {
        val registro = lista[position]
        holder.tvNumero.text = "${lista.size - position}   -"
        val scale = holder.itemView.context.resources.displayMetrics.density
        val paddingStartDp = (6 * scale + 0.5f).toInt()
        holder.tvFechaHora.setPadding(
            paddingStartDp,
            holder.tvFechaHora.paddingTop,
            holder.tvFechaHora.paddingRight,
            holder.tvFechaHora.paddingBottom
        )
        holder.tvFechaHora.text = registro.fechaHora
        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.US)
        val hoy = sdf.format(Date())
        val fechaRegistro = registro.fechaHora.substring(0, 10)
        val esHoy = fechaRegistro == hoy
        holder.etValor.isEnabled = false
        holder.etValor.isFocusable = true
        holder.etValor.isCursorVisible = true
        holder.etValor.isLongClickable = true
        holder.etValor.isEnabled = esHoy
        if (esHoy) {
            holder.etValor.isFocusableInTouchMode = true
            holder.etValor.setOnFocusChangeListener { v, hasFocus ->
                if (hasFocus) {
                    val rv =
                        (v.context as? androidx.appcompat.app.AppCompatActivity)?.findViewById<RecyclerView>(
                            R.id.rvGlucosa
                        )
                    rv?.postDelayed({
                        rv.scrollToPosition(0)
                        holder.etValor.setSelection(holder.etValor.text.length)
                    }, 100)
                }
            }
        }
        holder.textWatcher?.let { holder.etValor.removeTextChangedListener(it) }
        val valor = registro.valor
        val colorInicial = when {
            valor < 70 -> Color.parseColor("#FF0000")
            valor in 70..99 -> Color.parseColor("#006400")
            valor in 100..125 -> Color.parseColor("#FFA500")
            else -> Color.parseColor("#FF0000")
        }
        holder.etValor.setText(valor.toString())
        holder.etValor.setTextColor(colorInicial)
        val watcher = object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val nuevoValor = s.toString().toIntOrNull()
                if (nuevoValor != null) {
                    registro.valor = nuevoValor
                    dao.update(registro)
                    val colorEdit = when {
                        nuevoValor < 70 -> Color.parseColor("#FF0000")
                        nuevoValor in 70..99 -> Color.parseColor("#006400")
                        nuevoValor in 100..125 -> Color.parseColor("#FFA500")
                        else -> Color.parseColor("#FF0000")
                    }
                    holder.etValor.setTextColor(colorEdit)
                    if (s.toString().length >= 1) {
                        (holder.itemView.context as? AppGlucosa)?.refrescarGrafica()
                    }
                }
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        }
        holder.etValor.addTextChangedListener(watcher)
        holder.textWatcher = watcher
    }
}