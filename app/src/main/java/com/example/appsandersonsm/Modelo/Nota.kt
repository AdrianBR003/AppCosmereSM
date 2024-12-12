package com.example.appsandersonsm.Modelo

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.appsandersonsm.Firestore.NotaFirestore
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
            parentColumns = ["id"], // Clave primaria de "libros"
            childColumns = ["libroId"], // Clave foránea en "notas"
            onDelete = ForeignKey.CASCADE // Elimina las notas si el libro se elimina
        ),
        ForeignKey(
            entity = Usuario::class,
            parentColumns = ["id"], // Clave primaria de "usuarios"
            childColumns = ["userId"], // Clave foránea en "notas"
            onDelete = ForeignKey.CASCADE // Elimina las notas si el usuario se elimina
        )
    ],
    indices = [Index(value = ["libroId"]), Index(value = ["userId"])] // Índices para optimizar consultas
)
data class Nota(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val libroId: Int = 0, // Relación con "libros"
    val userId: String = "", // Relación con "usuarios"
    var titulo: String = "",
    var contenido: String = "",
    val fechaCreacion: String = obtenerFechaActual(),
    var fechaModificacion: String = ""
) {
    fun toFirestore(): NotaFirestore {
        return NotaFirestore(
            titulo = titulo,
            contenido = contenido,
            fechaCreacion = fechaCreacion,
            fechaModificacion = fechaModificacion,
            libroId = libroId
        )
    }

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
