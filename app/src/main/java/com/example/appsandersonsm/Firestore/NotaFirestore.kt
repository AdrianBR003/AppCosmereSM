package com.example.appsandersonsm.Firestore

import com.example.appsandersonsm.Modelo.Nota

data class NotaFirestore(
    val titulo: String = "",
    val contenido: String = "",
    val fechaCreacion: String = "",
    val fechaModificacion: String = "",
    val libroId: Int = 0 // Añade este campo
) {
    fun toNota(id: Int, libroId: Int): Nota {
        return Nota(
            id = id,
            libroId = libroId,
            titulo = titulo,
            contenido = contenido,
            fechaCreacion = fechaCreacion,
            fechaModificacion = fechaModificacion
        )
    }

    fun toMap(): Map<String, Any> {
        return mapOf(
            "titulo" to titulo,
            "contenido" to contenido,
            "fechaCreacion" to fechaCreacion,
            "fechaModificacion" to fechaModificacion,
            "libroId" to libroId
        )
    }
}
