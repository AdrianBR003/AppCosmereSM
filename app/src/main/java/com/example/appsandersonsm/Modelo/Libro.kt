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
    var nNotas: Int = 0,
    var sinopsis: String? = "",
    var valoracion: Float = 0.0f
)

