// NotaViewModel.kt
package com.example.appsandersonsm.ViewModel

import android.database.sqlite.SQLiteConstraintException
import android.util.Log
import androidx.lifecycle.*
import com.example.appsandersonsm.Modelo.Nota
import com.example.appsandersonsm.Repository.LibroRepository
import com.example.appsandersonsm.Repository.NotaRepository
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class NotaViewModel(private val repository: NotaRepository) : ViewModel() {

    fun getAllNotasByUsuario(userId: String): LiveData<List<Nota>> {
        return repository.getAllNotasByUsuario(userId)
    }

    fun eliminarNotaPorId(idNota: Int, userId: String) {
        viewModelScope.launch {
            repository.eliminarNotaPorId(idNota, userId)
        }
    }

    fun insertarNotasEstaticasSiVacia(notasEstaticas: List<Nota>, libroId: Int, userId: String) {
        viewModelScope.launch {
            repository.insertarNotasEstaticasSiTablaVacia(notasEstaticas, libroId, userId)
        }
    }

    fun insertarNota(nota: Nota) {
        viewModelScope.launch {
            repository.insertOrUpdateNota(nota)
        }
    }

    fun getNotaById(id: Int, userId: String): LiveData<Nota> {
        return repository.getNotaById(id, userId)
    }

    fun getNotasByLibroId(libroId: Int, userId: String): LiveData<List<Nota>> {
        return repository.getNotasByLibroId(libroId, userId).asLiveData()
    }

    fun updateNota(nota: Nota) = viewModelScope.launch {
        repository.updateNota(nota)
    }

    fun contarNotasPorLibro(libroId: Int, userId: String): LiveData<Int> {
        return repository.contarNotasPorLibro(libroId, userId)
    }
    

    // Factory para el ViewModel
    class NotaViewModelFactory(private val repository: NotaRepository) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(NotaViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return NotaViewModel(repository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
