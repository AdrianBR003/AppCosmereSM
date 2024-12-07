package com.example.appsandersonsm.Repository

import androidx.lifecycle.LiveData
import com.example.appsandersonsm.Dao.LibroDao
import com.example.appsandersonsm.DataBase.JsonHandler
import com.example.appsandersonsm.Modelo.Libro
import kotlinx.coroutines.flow.Flow

class LibroRepository(private val libroDao: LibroDao) {

    val allLibros: Flow<List<Libro>> = libroDao.getAllLibros()
    val allSagas: Flow<List<String>> = libroDao.getAllSagas()

    fun getLibrosBySaga(nombreSaga: String): Flow<List<Libro>> {
        return libroDao.getLibrosBySaga(nombreSaga)
    }

    suspend fun getLibroById(id: Int): Libro? {
        return libroDao.getLibroById(id)
    }

    class LibroRepository(private val libroDao: LibroDao) {
        val allLibros: Flow<List<Libro>> = libroDao.getAllLibros()
    }

    suspend fun updateLocalization(languageCode: String, jsonHandler: JsonHandler) {
        val librosLocalizados = jsonHandler.cargarLibrosDesdeJson(languageCode)
        librosLocalizados.forEach { libro ->
            libro.sinopsis?.let {
                libroDao.updateLibroLocalization(libro.id, libro.nombreLibro, libro.nombreSaga,
                    it
                )
            }
        }
    }


    suspend fun insertLibros(libros: List<Libro>) {
        libroDao.insertLibros(libros)
    }

    suspend fun updateLibro(libro: Libro) {
        libroDao.updateLibro(libro)
    }

    suspend fun deleteLibro(libro: Libro) {
        libroDao.deleteLibro(libro)
    }

    suspend fun actualizarSinopsis(libroId: Int, sinopsis: String) {
        libroDao.actualizarSinopsis(libroId, sinopsis)
    }


    suspend fun actualizarValoracion(libroId: Int, valoracion: Float) {
        libroDao.actualizarValoracion(libroId, valoracion)
    }
}