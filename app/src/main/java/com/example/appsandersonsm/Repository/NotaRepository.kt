package com.example.appsandersonsm.Repository

import android.util.Log
import androidx.lifecycle.LiveData
import com.example.appsandersonsm.Dao.NotaDao
import com.example.appsandersonsm.Modelo.Nota
import kotlinx.coroutines.flow.Flow

class NotaRepository(private val notaDao: NotaDao) {

    val notas: LiveData<List<Nota>> = notaDao.getAllNotas()


    suspend fun insertarNotasEstaticasSiTablaVacia(notasEstaticas: List<Nota>) {
        val cantidad = notaDao.countNotas()
        if (cantidad == 0) {
            for (nota in notasEstaticas) {
                notaDao.insert(nota)
            }
            Log.d("NotaRepository", "Notas estáticas insertadas: $notasEstaticas")
        } else {
            Log.d("NotaRepository", "La tabla ya contiene $cantidad notas. No se insertaron notas estáticas.")
        }
    }

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
