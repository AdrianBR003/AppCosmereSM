package com.example.appsandersonsm.Dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.example.appsandersonsm.Modelo.Libro
import kotlinx.coroutines.flow.Flow
@Dao
interface LibroDao {

    @Query("""
        UPDATE libros 
        SET nombreLibro = :nombreLibro, 
            nombreSaga = :nombreSaga, 
            sinopsis = :sinopsis 
        WHERE id = :libroId AND userId = :userId
    """)
    suspend fun updateLibroLocalization(
        libroId: Int,
        nombreLibro: String,
        nombreSaga: String,
        sinopsis: String,
        userId: String
    )

    @Query("SELECT * FROM libros WHERE userId = :userId")
    suspend fun obtenerLibrosPorUsuario(userId: String): List<Libro>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLibros(libros: List<Libro>)

    @Update
    suspend fun updateLibro(libro: Libro)

    @Delete
    suspend fun deleteLibro(libro: Libro)

    @Query("SELECT * FROM libros WHERE id = :id AND userId = :userId")
    suspend fun getLibroById(id: Int, userId: String): Libro?

    @Query("SELECT * FROM libros WHERE userId = :userId")
    fun getAllLibrosByUsuario(userId: String): Flow<List<Libro>>

    @Query("SELECT DISTINCT nombreSaga FROM libros WHERE userId = :userId")
    fun getAllSagasPorUsuario(userId: String): Flow<List<String>>

    @Query("SELECT * FROM libros WHERE nombreSaga = :nombreSaga AND userId = :userId")
    fun getLibrosBySagaAndUsuario(nombreSaga: String, userId: String): Flow<List<Libro>>

    @Query("SELECT COUNT(*) FROM libros WHERE userId = :userId")
    suspend fun getCountByUsuario(userId: String): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLibro(libro: Libro)

    @Query("SELECT * FROM libros WHERE userId = :userId")
    fun obtenerTodosLosLibrosPorUsuario(userId: String): LiveData<List<Libro>>

    @Query("UPDATE libros SET sinopsis = :sinopsis WHERE id = :libroId AND userId = :userId")
    suspend fun actualizarSinopsis(libroId: Int, sinopsis: String, userId: String)

    @Query("UPDATE libros SET valoracion = :valoracion WHERE id = :libroId AND userId = :userId")
    suspend fun actualizarValoracion(libroId: Int, valoracion: Float, userId: String)
}
