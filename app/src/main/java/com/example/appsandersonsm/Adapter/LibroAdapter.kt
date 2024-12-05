package com.example.appsandersonsm.Adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.RatingBar
import android.widget.TextView
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.example.appsandersonsm.Modelo.Libro
import com.example.appsandersonsm.R

class LibroAdapter(
    private val context: Context,
    private val onItemClick: (Libro) -> Unit
) : ListAdapter<Libro, LibroAdapter.LibroViewHolder>(LibroDiffCallback()) {

    // ViewHolder para representar un elemento de la lista
    inner class LibroViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imageViewPortada: ImageView = itemView.findViewById(R.id.imageViewPortada)
        val textViewNombreLibro: TextView = itemView.findViewById(R.id.textViewNombreLibro)
        val textViewNombreSaga: TextView = itemView.findViewById(R.id.textViewNombreSaga)
        val progressBar: ProgressBar = itemView.findViewById(R.id.progressBarLibro)
        val ratingBar: RatingBar = itemView.findViewById(R.id.ratingBarlibro)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LibroViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_libro, parent, false)
        return LibroViewHolder(view)
    }

    override fun onBindViewHolder(holder: LibroViewHolder, position: Int) {
        val libro = getItem(position)

        // Configuración de los valores en las vistas
        holder.textViewNombreLibro.text = libro.nombreLibro
        holder.textViewNombreSaga.text = libro.nombreSaga

        // Cálculo del progreso en porcentaje
        val progresoPorcentaje = if (libro.totalPaginas > 0) {
            (libro.progreso * 100) / libro.totalPaginas
        } else {
            0
        }

        // Cargar la imagen
        val resourceId = context.resources.getIdentifier(
            libro.nombrePortada,
            "drawable",
            context.packageName
        )
        if (resourceId != 0) {
            holder.imageViewPortada.setImageResource(resourceId)
        } else {
            holder.imageViewPortada.setImageResource(R.drawable.portada_elcamino)
        }

        holder.progressBar.max = 100
        holder.progressBar.progress = progresoPorcentaje.coerceIn(0, 100)
        holder.ratingBar.rating = libro.valoracion


        // Configuración del evento de clic
        holder.itemView.setOnClickListener {
            onItemClick(libro)
        }
    }
}

// Clase DiffCallback para optimizar cambios en la lista
class LibroDiffCallback : DiffUtil.ItemCallback<Libro>() {
    override fun areItemsTheSame(oldItem: Libro, newItem: Libro): Boolean {
        return oldItem.id == newItem.id // Compara por ID único
    }

    override fun areContentsTheSame(oldItem: Libro, newItem: Libro): Boolean {
        return oldItem == newItem // Compara por contenido completo
    }
}
