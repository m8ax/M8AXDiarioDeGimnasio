package com.mviiiax.m8ax_diariogimnasio

import android.graphics.Color
import android.graphics.Paint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView

data class PasswordItem(
    val servicio: String, val password: String
)

class PasswordsAdapter(
    private val items: MutableList<PasswordItem>,
    private val onListChanged: () -> Unit,
    private val hablar: (String) -> Unit
) : RecyclerView.Adapter<PasswordsAdapter.ViewHolder>() {
    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val textItem: TextView = itemView.findViewById(R.id.textItem)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view =
            LayoutInflater.from(parent.context).inflate(R.layout.item_lista_compra, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.textItem.text = "${position + 1} - ${item.servicio} → ${item.password}"
        holder.textItem.paintFlags =
            holder.textItem.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
        holder.textItem.setTextColor(Color.WHITE)
        holder.itemView.setOnClickListener {
            val clipboard =
                holder.itemView.context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
            val clip = android.content.ClipData.newPlainText("Contraseña", item.password)
            clipboard.setPrimaryClip(clip)
            hablar("Contraseña De ${item.servicio} Copiada.")
            Toast.makeText(
                holder.itemView.context,
                "Contraseña De ${item.servicio} Copiada.",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    override fun getItemCount(): Int = items.size
    fun eliminarItem(position: Int) {
        if (position >= 0 && position < items.size) {
            items.removeAt(position)
            notifyItemRemoved(position)
            notifyItemRangeChanged(0, items.size)
            onListChanged()
        }
    }
}