package com.example.appsandersonsm.Repository

import android.util.Log
import androidx.lifecycle.LiveData
import com.example.appsandersonsm.Dao.NotaDao
import com.example.appsandersonsm.Modelo.Nota
import kotlinx.coroutines.flow.Flow

class NotaRepository(private val notaDao: NotaDao) {

    val notas: LiveData<List<Nota>> = notaDao.getAllNotas()


    suspend fun insertarNotasEstaticasSiTablaVacia(notasEstaticas: List<Nota>, libroId: Int) {
        val numeroNotas = notaDao.contarNotasPorLibroSync(libroId)
        if (numeroNotas == 0) {
            notaDao.insertarNotas(notasEstaticas)
            Log.d("NotaRepository", "Notas estáticas insertadas: $notasEstaticas")
        } else {
            Log.d(
                "NotaRepository",
                "La tabla ya contiene $numeroNotas notas. No se insertaron notas estáticas."
            )
        }
    }


    fun contarNotasPorLibro(libroId: Int): LiveData<Int> {
        return notaDao.contarNotasPorLibro(libroId)
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

    suspend fun updateNota(nota: Nota) {
        notaDao.updateNota(nota)
    }

    suspend fun deleteNota(nota: Nota) {
        notaDao.deleteNota(nota)
    }
}
