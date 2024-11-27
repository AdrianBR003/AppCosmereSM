package com.example.appsandersonsm.Adapter


import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.appsandersonsm.R

// Clase Nota
data class Nota(val contenido: String, val fecha: String)

// Adaptador de notas
class NotasAdapter : ListAdapter<Nota, NotasAdapter.NotaViewHolder>(NotaDiffCallback()) {

    // Datos estáticos para pruebas
    private val notasEstaticas = listOf(
        Nota("Esta es la nota 1", "2024-01-01"),
        Nota("Esta es la nota 2", "2024-01-02"),
        Nota("Esta es la nota 3", "2024-01-03"),
        Nota("Esta es la nota 4", "2024-01-04"),
        Nota("Esta es la nota 5", "2024-01-05"),
    )

    // ViewHolder para los elementos de la lista
    inner class NotaViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvContenidoNota: TextView = itemView.findViewById(R.id.tvContenidoNota)
        val tvFechaNota: TextView = itemView.findViewById(R.id.tvFechaNota)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotaViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_nota, parent, false)
        return NotaViewHolder(view)
    }

    override fun onBindViewHolder(holder: NotaViewHolder, position: Int) {
        // Obtener la nota de la lista estática o dinámica
        val nota = if (position < notasEstaticas.size) notasEstaticas[position] else getItem(position - notasEstaticas.size)

        // Configurar los valores en las vistas
        holder.tvContenidoNota.text = nota.contenido
        holder.tvFechaNota.text = "Fecha: ${nota.fecha}"
    }

    override fun getItemCount(): Int = notasEstaticas.size + currentList.size
}

// DiffCallback para optimización
class NotaDiffCallback : DiffUtil.ItemCallback<Nota>() {
    override fun areItemsTheSame(oldItem: Nota, newItem: Nota): Boolean {
        // Compara por contenido único
        return oldItem.contenido == newItem.contenido && oldItem.fecha == newItem.fecha
    }

    override fun areContentsTheSame(oldItem: Nota, newItem: Nota): Boolean {
        // Compara todos los datos
        return oldItem == newItem
    }
}
