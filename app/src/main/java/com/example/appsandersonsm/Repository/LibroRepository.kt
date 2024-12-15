package com.example.appsandersonsm.Repository

import androidx.lifecycle.LiveData
import com.example.appsandersonsm.Dao.LibroDao
import com.example.appsandersonsm.Dao.NotaDao
import com.example.appsandersonsm.DataBase.JsonHandler
import com.example.appsandersonsm.Modelo.Libro
import com.example.appsandersonsm.Modelo.Nota
import kotlinx.coroutines.flow.Flow

class LibroRepository(private val libroDao: LibroDao, private val notaDao: NotaDao) {



    fun getAllLibrosByUsuario(userId: String): LiveData<List<Libro>> {
        return libroDao.getAllLibrosByUsuario(userId)
    }

    fun getAllSagasByUsuario(userId: String): Flow<List<String>> {
        return libroDao.getAllSagasPorUsuario(userId)
    }

    suspend fun deleteAllLibros(): Unit {
        return libroDao.deleteAllLibros()
    }


    fun getLibrosBySagaAndUsuario(nombreSaga: String, userId: String): Flow<List<Libro>> {
        return libroDao.getLibrosBySagaAndUsuario(nombreSaga, userId)
    }

    suspend fun getLibroByIdAndUsuario(id: Int, userId: String): Libro? {
        return libroDao.getLibroById(id, userId)
    }

    // Método que utiliza notaDao
    suspend fun getNotasPorLibroYUsuario(idNotaL: Int, userId: String): List<Nota> {
        // Asegúrate de que el método en NotaDao está correctamente definido
        return notaDao.obtenerNotasPorLibroYUsuario(idNotaL, userId)
    }

    suspend fun updateLocalization(languageCode: String, jsonHandler: JsonHandler, userId: String) {
        // Cargar los libros localizados desde el JSON correspondiente
        val librosLocalizados = jsonHandler.cargarLibrosDesdeJson(languageCode, userId)
        // Actualizar los datos en la base de datos con la información localizada
        librosLocalizados.forEach { libro ->
            libro.sinopsis?.let {
                libroDao.updateLibroLocalization(
                    libro.id,
                    libro.nombreLibro,
                    libro.nombreSaga,
                    it,
                    userId
                )
            }
        }
    }

    suspend fun actualizarEstadoLeido(idLibro: Int, leido: Boolean) {
        libroDao.actualizarEstadoLeido(idLibro, leido)
    }


    suspend fun insertLibros(libros: List<Libro>) {
        libroDao.insertLibros(libros)
    }

    suspend fun insertLibro(libro: Libro) {
        libroDao.insertLibro(libro)
    }

    suspend fun updateLibro(libro: Libro) {
        libroDao.updateLibro(libro)
    }

    suspend fun deleteLibro(libro: Libro) {
        libroDao.deleteLibro(libro)
    }

    suspend fun actualizarSinopsis(libroId: Int, sinopsis: String, userId: String) {
        libroDao.actualizarSinopsis(libroId, sinopsis, userId)
    }

    suspend fun actualizarValoracion(libroId: Int, valoracion: Float, userId: String) {
        libroDao.actualizarValoracion(libroId, valoracion, userId)
    }

    suspend fun getCountByUsuario(userId: String): Int {
        return libroDao.getCountByUsuario(userId)
    }
}
