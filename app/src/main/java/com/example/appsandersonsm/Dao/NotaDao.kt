package com.example.appsandersonsm.Dao

import android.util.Log
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

    @Query("DELETE FROM notas WHERE id = :idNota AND userId = :userId")
    suspend fun eliminarNotaPorId(idNota: Int, userId: String)

    @Query("UPDATE notas SET fechaModificacion = :nuevaFecha WHERE id = :notaId AND userId = :userId")
    suspend fun actualizarFechaModificacion(notaId: Int, nuevaFecha: String, userId: String)

    @Query("SELECT COUNT(*) FROM notas WHERE libroId = :libroId AND userId = :userId")
    fun contarNotasPorLibro(libroId: Int, userId: String): LiveData<Int>

    @Query("SELECT COUNT(*) FROM notas WHERE libroId = :libroId AND userId = :userId")
    suspend fun contarNotasPorLibroSync(libroId: Int, userId: String): Int

    @Transaction
    suspend fun insertarNotasSiTablaVaciaTransaccion(notasEstaticas: List<Nota>, libroId: Int, userId: String) {
        val numeroNotas = contarNotasPorLibroSync(libroId, userId)
        if (numeroNotas == 0) {
            insertarNotas(notasEstaticas)
            Log.d("NotaDao", "Notas estáticas insertadas: $notasEstaticas")
        } else {
            Log.d(
                "NotaDao",
                "La tabla ya contiene $numeroNotas notas para el usuario $userId. No se insertaron notas estáticas."
            )
        }
    }

    @Query("SELECT * FROM notas WHERE userId = :userId")
    fun getAllNotasByUsuario(userId: String): LiveData<List<Nota>>

    @Update
    suspend fun updateNota(nota: Nota)

    @Delete
    suspend fun deleteNota(nota: Nota)

    @Query("SELECT * FROM notas WHERE id = :id AND userId = :userId")
    fun getNotaById(id: Int, userId: String): LiveData<Nota>

    @Query("SELECT * FROM notas WHERE libroId = :libroId AND userId = :userId")
    fun getNotasByLibroId(libroId: Int, userId: String): Flow<List<Nota>>

    @Query("SELECT * FROM notas WHERE libroId = :libroId AND userId = :userId ORDER BY fechaCreacion ASC")
    fun getNotasPorLibro(libroId: Int, userId: String): Flow<List<Nota>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateNota(nota: Nota)
    @Query("DELETE FROM notas")
    suspend fun deleteAllNotas()
}
