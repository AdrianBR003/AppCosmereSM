package com.example.appsandersonsm.Firestore

import com.example.appsandersonsm.Modelo.Libro

data class LibroFirestore(
    val nombreLibro: String = "",
    val progreso: Int = 0,
    val valoracion: Float = 0.0f
) {
    fun toLibro(id: Int): Libro {
        return Libro(
            id = id,
            nombreLibro = nombreLibro,
            nombreSaga = "", // Ajusta según tus necesidades
            nombrePortada = null,
            progreso = progreso,
            totalPaginas = 0, // Ajusta según tus necesidades
            inicialSaga = false, // Ajusta según tus necesidades
            sinopsis = "",
            valoracion = valoracion,
            numeroNotas = 0, // Ajusta según tus necesidades
            empezarLeer = false, // Ajusta según tus necesidades
            userId = ""
        )
    }

    fun toMap(): Map<String, Any> {
        return mapOf(
            "nombreLibro" to nombreLibro,
            "progreso" to progreso,
            "valoracion" to valoracion
        )
    }
}
