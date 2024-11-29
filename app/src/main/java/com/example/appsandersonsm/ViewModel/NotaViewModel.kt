// NotaViewModel.kt
package com.example.appsandersonsm.ViewModel

import android.database.sqlite.SQLiteConstraintException
import android.util.Log
import androidx.lifecycle.*
import com.example.appsandersonsm.Modelo.Nota
import com.example.appsandersonsm.Repository.LibroRepository
import com.example.appsandersonsm.Repository.NotaRepository
import kotlinx.coroutines.launch

class NotaViewModel(private val repository: NotaRepository) : ViewModel() {



    private val _notas = MutableLiveData<List<Nota>>()
    val notas: LiveData<List<Nota>> = repository.notas

    private val _numeroNotas = MutableLiveData<Int>()


    fun verificarEInsertarNotasEstaticas(notasEstaticas: List<Nota>, idLibro: Int) {
        viewModelScope.launch {
            repository.insertarNotasEstaticasSiTablaVacia(notasEstaticas, idLibro)
        }
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

    fun addNota(nota: Nota) {
        val currentNotas = _notas.value?.toMutableList() ?: mutableListOf()
        currentNotas.add(nota)
        _notas.value = currentNotas

        Log.d("NotaViewModel", "Nueva nota añadida: $nota")
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


