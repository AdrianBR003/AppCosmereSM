package com.example.appsandersonsm.Adapter


import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.appsandersonsm.Modelo.Nota
import com.example.appsandersonsm.R

class NotasAdapter(private val listener: OnNotaClickListener) : RecyclerView.Adapter<NotasAdapter.NotaViewHolder>() {

    private var notas: List<Nota> = emptyList()

    interface OnNotaClickListener {
        fun onNotaClick(nota: Nota)
    }

    fun setNotas(nuevasNotas: List<Nota>) {
        this.notas = nuevasNotas
        Log.d("NotasAdapter", "Notas asignadas al adaptador: $notas")
        notifyDataSetChanged() // Refresca el RecyclerView
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotaViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_nota, parent, false)
        return NotaViewHolder(view)
    }

    override fun onBindViewHolder(holder: NotaViewHolder, position: Int) {
        val nota = notas[position]
        Log.d("LogNotasAdapter", "Enlazando nota: $nota")
        holder.bind(nota)
    }

    override fun getItemCount() = notas.size

    inner class NotaViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val textViewTitulo: TextView = itemView.findViewById(R.id.tvTituloitemNota)
        private val textViewFecha: TextView = itemView.findViewById(R.id.tvFechaNota)

        fun bind(nota: Nota) {
            textViewTitulo.text = nota.titulo
            textViewFecha.text = "Fecha: ${nota.fechaCreacion}"

            itemView.setOnClickListener {
                listener.onNotaClick(nota)
            }
        }
    }
}
