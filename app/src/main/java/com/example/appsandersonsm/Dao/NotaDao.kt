package com.example.appsandersonsm.Dao

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.room.*
import com.example.appsandersonsm.Modelo.Nota
import kotlinx.coroutines.flow.Flow

@Dao
interface NotaDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertarNota(nota: Nota): Long

    @Update
    suspend fun actualizarNota(nota: Nota)

    @Query("SELECT * FROM notas WHERE userId = :userId")
    fun getAllNotasByUsuario(userId: String): LiveData<List<Nota>>

    @Query("DELETE FROM notas")
    suspend fun borrarTodasLasNotas()

    @Query("DELETE FROM sqlite_sequence WHERE name='notas'")
    suspend fun resetearSecuenciaNotas()

    @Query("UPDATE notas SET userId = :newUserId WHERE userId = 'id_default'")
    suspend fun actualizarNotasIdDefault(newUserId: String)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(nota: Nota)

    @Query("SELECT * FROM notas WHERE idLibroN = :idLibroN AND userId = :userId")
    suspend fun obtenerNotasPorLibroYUsuario(idLibroN: Int, userId: String): List<Nota>


    @Query("DELETE FROM notas WHERE id = :idNota AND userId = :userId")
    suspend fun eliminarNotaPorId(idNota: Int, userId: String)


    @Query("UPDATE notas SET fechaModificacion = :nuevaFecha WHERE id = :notaId AND userId = :userId")
    suspend fun actualizarFechaModificacion(notaId: Int, nuevaFecha: String, userId: String)

    @Query("SELECT COUNT(*) FROM notas WHERE idLibroN = :libroId AND userId = :userId")
    suspend fun contarNotasPorLibroSync(libroId: Int, userId: String): Int

    @Update
    suspend fun updateNota(nota: Nota)

    @Delete
    suspend fun deleteNota(nota: Nota)


    @Query("SELECT COUNT(*) FROM notas WHERE idLibroN = :libroId AND userId = :userId")
    fun contarNotasPorLibro(libroId: Int, userId: String): LiveData<Int>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertarNotas(notas: List<Nota>)


    @Query("SELECT * FROM notas WHERE id = :id AND userId = :userId")
    fun getNotaById(id: Int, userId: String): LiveData<Nota>

    @Query("SELECT * FROM notas WHERE idLibroN = :libroId AND userId = :userId")
    fun getNotasByLibroId(libroId: Int, userId: String): Flow<List<Nota>>

    @Query("SELECT * FROM notas WHERE idLibroN = :libroId AND userId = :userId ORDER BY fechaCreacion ASC")
    fun getNotasPorLibro(libroId: Int, userId: String): Flow<List<Nota>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateNota(nota: Nota)
    @Query("DELETE FROM notas")
    suspend fun deleteAllNotas()
}
