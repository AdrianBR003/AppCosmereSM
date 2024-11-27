package com.example.appsandersonsm.Modelo

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey


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
    foreignKeys = [ForeignKey(
        entity = Libro::class,
        parentColumns = ["id"],
        childColumns = ["libroId"],
        onDelete = ForeignKey.CASCADE
    )]
)
data class Nota(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val libroId: Int,
    val contenido: String,
    val fechaCreacion: String = "",
    val fechaModificacion: String = ""
)