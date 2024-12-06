package com.example.appsandersonsm.Adapter


import android.annotation.SuppressLint
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat.getString
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.appsandersonsm.Modelo.Nota
import com.example.appsandersonsm.R
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class NotasAdapter(private val onNotaClickListener: OnNotaClickListener) :
    ListAdapter<Nota, NotasAdapter.NotaViewHolder>(DIFF_CALLBACK) {

    interface OnNotaClickListener {
        fun onNotaClick(nota: Nota)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotaViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_nota, parent, false)
        return NotaViewHolder(view)
    }

    override fun onBindViewHolder(holder: NotaViewHolder, position: Int) {
        val nota = getItem(position)
        holder.bind(nota)
        holder.itemView.setOnClickListener { onNotaClickListener.onNotaClick(nota) }
    }

    // NOTAVIEWHOLDER
    class NotaViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tituloTextView: TextView = itemView.findViewById(R.id.tvTituloitemNota)
        private val fechaTextView: TextView = itemView.findViewById(R.id.tvFechaNota)

        @SuppressLint("SetTextI18n")
        fun bind(nota: Nota) {
            tituloTextView.text = nota.titulo

            // Usa la fecha de modificación si existe, si no, usa la fecha de creación
            val fecha = nota.fechaModificacion.takeIf { it.isNotBlank() } ?: nota.fechaCreacion

            // Formatear y mostrar la fecha
            val mensajeUltimaModificacion = if (Locale.getDefault().language == "es") {
                "Última Modificación:"
            } else {
                "Last Modified:"
            }

            fechaTextView.text = "$mensajeUltimaModificacion ${formatearFecha(fecha)}"        }

        private fun formatearFecha(fecha: String): String {
            return if (fecha.isBlank()) {
                "Sin fecha"
            } else {
                try {
                    // Primero, intenta con el formato completo (fecha y hora)
                    val formatoEntradaCompleto = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                    val formatoSalida = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                    val fechaParseada = formatoEntradaCompleto.parse(fecha)
                    formatoSalida.format(fechaParseada)
                } catch (e1: Exception) {
                    try {
                        // Si falla, intenta solo con la fecha (sin hora)
                        val formatoEntradaSimple = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                        val formatoSalida = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                        val fechaParseada = formatoEntradaSimple.parse(fecha)
                        formatoSalida.format(fechaParseada)
                    } catch (e2: Exception) {
                        Log.e("NotaViewHolder", "Error al formatear la fecha: ${e2.message}")
                        "Fecha inválida"
                    }
                }
            }
        }
    }


    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<Nota>() {
            override fun areItemsTheSame(oldItem: Nota, newItem: Nota): Boolean {
                return oldItem.id == newItem.id
            }

            override fun areContentsTheSame(oldItem: Nota, newItem: Nota): Boolean {
                return oldItem == newItem
            }
        }
    }
}
