package com.example.appsandersonsm.Modelo

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.appsandersonsm.Firestore.LibroFirestore

@Entity(tableName = "libros")
data class Libro(
    @PrimaryKey(autoGenerate = true) val id: Int,
    val nombreLibro: String,
    val nombreSaga: String,
    val nombrePortada: String?,
    var progreso: Int,
    var totalPaginas: Int,
    var inicialSaga: Boolean,
    var sinopsis: String? = "",
    var valoracion: Float = 0.0f,
    var numeroNotas: Int = 0,
    var empezarLeer: Boolean
){
    fun toFirestore(): LibroFirestore {
        return LibroFirestore(
            nombreLibro = nombreLibro,
            progreso = progreso,
            valoracion = valoracion
        )
    }
}

