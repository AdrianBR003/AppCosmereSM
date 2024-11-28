package com.example.appsandersonsm.Repository

import androidx.lifecycle.LiveData
import com.example.appsandersonsm.Dao.NotaDao
import com.example.appsandersonsm.Modelo.Nota
import kotlinx.coroutines.flow.Flow

class NotaRepository(private val notaDao: NotaDao) {

    suspend fun insertOrUpdateNota(nota: Nota) {
        notaDao.insertOrUpdateNota(nota)
    }

    fun getNotaById(id: Int): LiveData<Nota> {
        return notaDao.getNotaById(id)
    }

    fun getNotasByLibroId(libroId: Int): Flow<List<Nota>> {
        return notaDao.getNotasByLibroId(libroId)
    }

    fun getNotasPorLibro(libroId: Int): Flow<List<Nota>> {
        return notaDao.getNotasPorLibro(libroId)
    }

    suspend fun insertNota(nota: Nota) {
        notaDao.insertNota(nota)
    }

    suspend fun updateNota(nota: Nota) {
        notaDao.updateNota(nota)
    }

    suspend fun deleteNota(nota: Nota) {
        notaDao.deleteNota(nota)
    }
}
