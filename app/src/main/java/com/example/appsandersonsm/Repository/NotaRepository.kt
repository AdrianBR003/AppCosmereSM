package com.example.appsandersonsm.Repository

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import com.example.appsandersonsm.Dao.NotaDao
import com.example.appsandersonsm.Modelo.Nota
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class NotaRepository(private val notaDao: NotaDao) {

    fun getAllNotasByUsuario(userId: String): LiveData<List<Nota>> {
        return notaDao.getAllNotasByUsuario(userId)
    }

    suspend fun actualizarFechaModificacion(notaId: Int, nuevaFecha: String, userId: String) {
        notaDao.actualizarFechaModificacion(notaId, nuevaFecha, userId)
    }

    suspend fun insertarNotasEstaticasSiTablaVacia(notasEstaticas: List<Nota>, libroId: Int, userId: String) {
        withContext(Dispatchers.IO) {
            notaDao.insertarNotasSiTablaVaciaTransaccion(notasEstaticas, libroId, userId)
        }
    }

    suspend fun eliminarNotaPorId(idNota: Int, userId: String) {
        notaDao.eliminarNotaPorId(idNota, userId)
    }

    fun contarNotasPorLibro(libroId: Int, userId: String): LiveData<Int> {
        return notaDao.contarNotasPorLibro(libroId, userId)
    }

    suspend fun insertOrUpdateNota(nota: Nota) {
        notaDao.insertOrUpdateNota(nota)
    }

    fun getNotaById(id: Int, userId: String): LiveData<Nota> {
        return notaDao.getNotaById(id, userId)
    }

    fun getNotasByLibroId(libroId: Int, userId: String): Flow<List<Nota>> {
        return notaDao.getNotasByLibroId(libroId, userId)
    }

    fun getNotasPorLibro(libroId: Int, userId: String): Flow<List<Nota>> {
        return notaDao.getNotasPorLibro(libroId, userId)
    }

    suspend fun updateNota(nota: Nota) {
        notaDao.updateNota(nota)
    }

    suspend fun deleteNota(nota: Nota) {
        notaDao.deleteNota(nota)
    }

}
