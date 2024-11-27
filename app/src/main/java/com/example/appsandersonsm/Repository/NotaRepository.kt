package com.example.appsandersonsm.Repository

import com.example.appsandersonsm.Dao.NotaDao
import com.example.appsandersonsm.Modelo.Nota
import kotlinx.coroutines.flow.Flow

class NotaRepository(private val notaDao: NotaDao) {

    fun getNotasPorLibro(libroId: Int): Flow<List<Nota>> {
        return notaDao.getNotasPorLibro(libroId)
    }

    suspend fun insertNota(nota: Nota): Long {
        return notaDao.insertNota(nota)
    }

    suspend fun updateNota(nota: Nota) {
        notaDao.updateNota(nota)
    }

    suspend fun deleteNota(nota: Nota) {
        notaDao.deleteNota(nota)
    }
}
