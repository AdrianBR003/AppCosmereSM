package com.example.appsandersonsm.Modelo

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "libros")
data class Libro(
    @PrimaryKey(autoGenerate = true) val id: Int,
    val nombreLibro: String,
    val nombreSaga: String,
    val nombrePortada: String?,
    var progreso: Int,
    var totalPaginas: Int,
    var inicialSaga: Boolean,
    var nNotas: Int = 0, // Número de notas, predeterminado a 0
    var sinopsis: String? = "", // Descripción como texto
    var valoracion: Float = 0.0f // Valoración entre 0 y 10
) {
    init {
        require(valoracion in 0.0..10.0) { "La valoración debe estar entre 0 y 10." }
    }
}