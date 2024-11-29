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



    private val _notas = MutableLiveData<List<Nota>>()
    val notas: LiveData<List<Nota>> = repository.notas



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


