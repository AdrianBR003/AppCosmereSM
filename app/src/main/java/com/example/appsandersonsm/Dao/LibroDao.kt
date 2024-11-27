package com.example.appsandersonsm.Dao

import androidx.room.*
import com.example.appsandersonsm.Modelo.Libro
import kotlinx.coroutines.flow.Flow

@Dao
interface LibroDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLibros(libros: List<Libro>)

    @Update
    suspend fun updateLibro(libro: Libro)

    @Delete
    suspend fun deleteLibro(libro: Libro)

    @Query("SELECT * FROM libros WHERE id = :id")
    suspend fun getLibroById(id: Int): Libro?

    @Query("SELECT * FROM libros")
    fun getAllLibros(): Flow<List<Libro>>

    @Query("SELECT DISTINCT nombreSaga FROM libros")
    fun getAllSagas(): Flow<List<String>>

    @Query("SELECT * FROM libros WHERE nombreSaga = :nombreSaga")
    fun getLibrosBySaga(nombreSaga: String): Flow<List<Libro>>

    @Query("SELECT COUNT(*) FROM libros")
    suspend fun getCount(): Int

}




