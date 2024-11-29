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
            parentColumns = ["id"], // Clave primaria de "libros"
            childColumns = ["libroId"], // Clave foránea en "notas"
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["libroId"])]
)
data class Nota(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val libroId: Int, // Debe coincidir con el tipo de "id" en "libros"
    var titulo: String,
    var contenido: String,
    val fechaCreacion: String = obtenerFechaActual(),
    var fechaModificacion: String = ""
)

fun obtenerFechaActual(): String {
    val formatoFecha = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    return formatoFecha.format(Date())
}

