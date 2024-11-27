package com.example.appsandersonsm.Dao

import androidx.room.*
import com.example.appsandersonsm.Modelo.Nota
import kotlinx.coroutines.flow.Flow

@Dao
interface NotaDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNota(nota: Nota): Long

    @Update
    suspend fun updateNota(nota: Nota)

    @Delete
    suspend fun deleteNota(nota: Nota)

    @Query("SELECT * FROM notas WHERE libroId = :libroId ORDER BY fechaCreacion ASC")
    fun getNotasPorLibro(libroId: Int): Flow<List<Nota>>
}
