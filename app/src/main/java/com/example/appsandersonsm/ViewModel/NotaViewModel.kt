package com.example.appsandersonsm.ViewModel

import androidx.lifecycle.*
import com.example.appsandersonsm.Modelo.Nota
import com.example.appsandersonsm.Repository.NotaRepository
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class NotaViewModel(private val repository: NotaRepository) : ViewModel() {

    fun eliminarNotaPorId(idNota: Int) {
        viewModelScope.launch {
            repository.eliminarNotaPorId(idNota)
        }
    }

    fun insertarNotasEstaticasSiVacia(notasEstaticas: List<Nota>, idLibro: Int) {
        viewModelScope.launch {
            repository.insertarNotasEstaticasSiTablaVacia(notasEstaticas, idLibro)
        }
    }

    fun insertarNota(nota: Nota) {
        viewModelScope.launch {
            repository.insertOrUpdateNota(nota)
        }
    }

    private fun obtenerFechaActual(): String {
        val formatoFecha = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        return formatoFecha.format(Date())
    }

    fun getNotaById(id: Int): LiveData<Nota> {
        return repository.getNotaById(id)
    }

    fun getNotasByLibroId(libroId: Int): LiveData<List<Nota>> {
        return repository.getNotasByLibroId(libroId).asLiveData()
    }

    fun updateNota(nota: Nota) = viewModelScope.launch {
        repository.updateNota(nota)
    }


    fun contarNotasPorLibro(libroId: Int): LiveData<Int> {
        return repository.contarNotasPorLibro(libroId)
    }


    // Factory para el ViewModel
    class NotaViewModelFactory(private val repository: NotaRepository) :
        ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(NotaViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return NotaViewModel(repository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }

}


