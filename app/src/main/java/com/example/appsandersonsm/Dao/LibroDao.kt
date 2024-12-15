package com.example.appsandersonsm.Dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.example.appsandersonsm.DataBase.AppDatabase
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

    @Query("UPDATE libros SET leido = :leido WHERE id = :idLibro")
    suspend fun actualizarEstadoLeido(idLibro: Int, leido: Boolean)

    @Query("UPDATE libros SET inicialSaga = :inicialSaga WHERE id = :id AND userId = :userId")
    suspend fun updateInicialSaga(id: Int, inicialSaga: Boolean, userId: String)

    @Query("SELECT * FROM libros WHERE userId = :userId")
    suspend fun obtenerLibrosPorUsuario(userId: String): List<Libro>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLibros(libros: List<Libro>)

    @Query("UPDATE libros SET userId = :newUserId WHERE userId = 'id_default'")
    suspend fun actualizarLibrosIdDefault(newUserId: String)

    @Query("DELETE FROM libros")
    suspend fun borrarTodosLosLibros()

    @Query("DELETE FROM sqlite_sequence WHERE name='libros'")
    suspend fun resetearSecuenciaLibros()


    @Update
    suspend fun updateLibro(libro: Libro)

    @Delete
    suspend fun deleteLibro(libro: Libro)

    @Query("SELECT * FROM libros WHERE id = :id AND userId = :userId")
    suspend fun getLibroById(id: Int, userId: String): Libro?

    @Query("SELECT * FROM libros WHERE userId = :userId")
    fun getAllLibrosByUsuario(userId: String): LiveData<List<Libro>>

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

    @Query("DELETE FROM libros")
    suspend fun deleteAllLibros()
}
