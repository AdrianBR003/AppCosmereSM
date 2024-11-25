package com.example.appsandersonsm

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.appsandersonsm.Modelo.Nota

class NotasAdapter : RecyclerView.Adapter<NotasAdapter.NotaViewHolder>() {

    // Datos estáticos
    private val notas = listOf(
        Nota("Esta es la nota 1", "2024-01-01"),
        Nota("Esta es la nota 2", "2024-01-02"),
        Nota("Esta es la nota 3", "2024-01-03"),
        Nota("Esta es la nota 1", "2024-01-01"),
        Nota("Esta es la nota 2", "2024-01-02"),
        Nota("Esta es la nota 1", "2024-01-01"),
        Nota("Esta es la nota 2", "2024-01-02"),
        Nota("Esta es la nota 1", "2024-01-01"),
        Nota("Esta es la nota 2", "2024-01-02")
    )

    inner class NotaViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvContenidoNota: TextView = itemView.findViewById(R.id.tvContenidoNota)
        val tvFechaNota: TextView = itemView.findViewById(R.id.tvFechaNota)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotaViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_nota, parent, false)
        return NotaViewHolder(view)
    }

    override fun onBindViewHolder(holder: NotaViewHolder, position: Int) {
        val nota = notas[position]
        holder.tvContenidoNota.text = nota.contenido
        holder.tvFechaNota.text = "Fecha: ${nota.fecha}"
    }

    override fun getItemCount(): Int = notas.size
}
