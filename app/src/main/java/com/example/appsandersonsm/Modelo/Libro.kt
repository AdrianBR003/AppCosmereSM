package com.example.appsandersonsm.Modelo

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.appsandersonsm.Firestore.LibroFirestore

@Entity(
    tableName = "libros",
    foreignKeys = [
        ForeignKey(
            entity = Usuario::class,
            parentColumns = ["id"], // Clave primaria de "usuarios"
            childColumns = ["userId"], // Clave foránea en "libros"
            onDelete = ForeignKey.CASCADE // Elimina los libros si el usuario se elimina
        )
    ],
    indices = [Index(value = ["userId"])] // Índice para mejorar la consulta por usuario
)
data class Libro(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val nombreLibro: String = "",
    val nombreSaga: String = "",
    val nombrePortada: String? = null,
    var progreso: Int = 0,
    var totalPaginas: Int = 0,
    var inicialSaga: Boolean = false,
    var sinopsis: String? = "",
    var valoracion: Float = 0.0f,
    var numeroNotas: Int = 0,
    var empezarLeer: Boolean = false,
    val userId: String = "" // Relación con la tabla "usuarios"
) {
    fun toFirestore(): LibroFirestore {
        return LibroFirestore(
            nombreLibro = nombreLibro,
            progreso = progreso,
            valoracion = valoracion
        )
    }
}
