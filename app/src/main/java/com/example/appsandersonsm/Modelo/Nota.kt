package com.example.appsandersonsm.Modelo

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


/**
 *
 * Anotaciones:
 *  @Entity define una tabla, y @PrimaryKey identifica la clave primaria.
 *  En Nota, libroId está relacionado con la tabla libros.
 *  Generación Automática de IDs:
 *  En Nota, id se autogenera.
 *  Relaciones:
 *  La relación de clave foránea se establece para que al eliminar un Libro,
 *  sus Notas asociadas también se eliminen (onDelete = ForeignKey.CASCADE).
 *
 */

@Entity(
    tableName = "notas",
    foreignKeys = [
        ForeignKey(
            entity = Libro::class,
            parentColumns = ["id"], // Referencia a la clave primaria "id"
            childColumns = ["idLibroN"], // Usa "libroId" como clave foránea
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["idLibroN"])]
)
data class Nota(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val userId: String = "", // Relación con "usuarios"
    var idLibroN: Int = 0, // Relación con "libros"
    var titulo: String = "",
    var contenido: String = "",
    val fechaCreacion: String = obtenerFechaActual(),
    var fechaModificacion: String = ""
){
    companion object {
        fun obtenerFechaActual(): String {
            // Implementa la lógica para obtener la fecha actual en el formato deseado
            // Por ejemplo:
            val formatter = java.text.SimpleDateFormat(
                "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
                java.util.Locale.getDefault()
            )
            return formatter.format(java.util.Date())
        }
    }
}
