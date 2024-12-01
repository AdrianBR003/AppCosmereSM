package com.example.appsandersonsm.Adapter

import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.appsandersonsm.Modelo.Noticia
import com.example.appsandersonsm.R

class NoticiasAdapter(private val noticias: List<Noticia>) :
    RecyclerView.Adapter<NoticiasAdapter.NoticiaViewHolder>() {

    class NoticiaViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val titulo: TextView = view.findViewById(R.id.textViewTituloNoticia)
        val descripcion: TextView = view.findViewById(R.id.textViewDescripcionNoticia)
        val imagen: ImageView = view.findViewById(R.id.imageViewNoticia)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NoticiaViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_noticia, parent, false)
        return NoticiaViewHolder(view)
    }

    override fun onBindViewHolder(holder: NoticiaViewHolder, position: Int) {
        val noticia = noticias[position]
        holder.titulo.text = noticia.titulo
        holder.descripcion.text = noticia.descripcion
        // Si tienes una URL de imagen, usa Glide o Picasso para cargarla
        Glide.with(holder.itemView.context)
            .load(noticia.imagenUrl)
            .into(holder.imagen)

        // Configurar el click listener para abrir el enlace
        holder.itemView.setOnClickListener {
            val context = holder.itemView.context
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(noticia.enlace))
            context.startActivity(intent)
        }
    }

    override fun getItemCount(): Int = noticias.size
}
