package com.mviiiax.m8ax_diariogimnasio

import android.graphics.Color
import android.graphics.Paint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class ListaCompraAdapter(
    private val items: MutableList<CompraItem>,
    private val onItemChanged: () -> Unit,
    private val hablar: (String) -> Unit
) : RecyclerView.Adapter<ListaCompraAdapter.ViewHolder>() {
    private var usarRomano = true

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val textItem: TextView = itemView.findViewById(R.id.textItem)
    }

    fun setUsarRomano(valor: Boolean) {
        usarRomano = valor
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view =
            LayoutInflater.from(parent.context).inflate(R.layout.item_lista_compra, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        val numero = if (usarRomano) toRoman(position + 1) else "${position + 1}"
        holder.textItem.text = "• $numero •   \u2794   ${item.nombre}"
        if (item.comprado) {
            holder.textItem.paintFlags = holder.textItem.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
            holder.textItem.setTextColor(Color.parseColor("#7CFC00"))
        } else {
            holder.textItem.paintFlags =
                holder.textItem.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
            holder.textItem.setTextColor(Color.WHITE)
        }
        holder.itemView.setOnClickListener {
            item.comprado = !item.comprado
            notifyItemChanged(position)
            onItemChanged()
            val mensajes = listOf(
                Pair(" Comprado!", " Aún Por Comprar"),
                Pair(" Hecho!", " Pendiente"),
                Pair(" Marcado.", " Desmarcado."),
            )
            val parElegido = mensajes.random()
            hablar("${item.nombre}; ${if (item.comprado) parElegido.first else parElegido.second}")
        }
    }

    override fun getItemCount(): Int = items.size

    private fun toRoman(num: Int): String {
        if (num > 1_000_000) return num.toString()
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

    fun eliminarItem(position: Int) {
        if (position >= 0 && position < items.size) {
            items.removeAt(position)
            notifyItemRemoved(position)
            notifyItemRangeChanged(0, items.size)
            onItemChanged()
        }
    }
}