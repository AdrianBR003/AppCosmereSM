package com.example.appsandersonsm.Dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.example.appsandersonsm.Modelo.Nota
import kotlinx.coroutines.flow.Flow

@Dao
interface NotaDao {


    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertarNotas(notas: List<Nota>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(nota: Nota)

    @Query("DELETE FROM notas WHERE id = :idNota")
    suspend fun eliminarNotaPorId(idNota: Int)

    @Query("UPDATE notas SET fechaModificacion = :nuevaFecha WHERE id = :notaId")
    suspend fun actualizarFechaModificacion(notaId: Int, nuevaFecha: String)

    @Query("SELECT COUNT(*) FROM notas WHERE libroId = :libroId")
    fun contarNotasPorLibro(libroId: Int): LiveData<Int>

    @Query("SELECT COUNT(*) FROM notas WHERE libroId = :libroId")
    suspend fun contarNotasPorLibroSync(libroId: Int): Int

    @Transaction
    suspend fun insertarNotasSiTablaVaciaTransaccion(notasEstaticas: List<Nota>, libroId: Int) {
        val numeroNotas = contarNotasPorLibroSync(libroId)
        if (numeroNotas == 0) {
            insertarNotas(notasEstaticas)
        }
    }

    @Query("SELECT * FROM notas")
    fun getAllNotas(): LiveData<List<Nota>>

    @Update
    suspend fun updateNota(nota: Nota)

    @Delete
    suspend fun deleteNota(nota: Nota)

    @Query("SELECT * FROM notas WHERE id = :id")
    fun getNotaById(id: Int): LiveData<Nota>

    @Query("SELECT COUNT(*) FROM notas WHERE libroId = :libroId")
    suspend fun countNotasPorLibro(libroId: Int): Int

    @Query("SELECT * FROM notas WHERE libroId = :libroId")
    fun getNotasByLibroId(libroId: Int): Flow<List<Nota>>

    @Query("SELECT * FROM notas WHERE id = :libroId ORDER BY fechaCreacion ASC")
    fun getNotasPorLibro(libroId: Int): Flow<List<Nota>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateNota(nota: Nota)
}
