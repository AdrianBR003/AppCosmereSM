// NotaViewModel.kt
package com.example.appsandersonsm.ViewModel

import androidx.lifecycle.*
import com.example.appsandersonsm.Modelo.Nota
import com.example.appsandersonsm.Repository.NotaRepository
import kotlinx.coroutines.launch

class NotaViewModel(private val repository: NotaRepository) : ViewModel() {

    fun getNotasPorLibro(libroId: Int): LiveData<List<Nota>> {
        return repository.getNotasPorLibro(libroId).asLiveData()
    }

    fun insertNota(nota: Nota) = viewModelScope.launch {
        repository.insertNota(nota)
    }

    fun updateNota(nota: Nota) = viewModelScope.launch {
        repository.updateNota(nota)
    }

    fun deleteNota(nota: Nota) = viewModelScope.launch {
        repository.deleteNota(nota)
    }
}

class NotaViewModelFactory(private val repository: NotaRepository) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(NotaViewModel::class.java)) {
            return NotaViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

