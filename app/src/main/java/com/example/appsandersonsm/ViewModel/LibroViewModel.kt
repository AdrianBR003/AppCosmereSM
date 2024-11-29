// LibroViewModel.kt
package com.example.appsandersonsm.ViewModel

import androidx.lifecycle.*
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewmodel.CreationExtras
import com.example.appsandersonsm.Modelo.Libro
import com.example.appsandersonsm.Repository.LibroRepository
import kotlinx.coroutines.launch

class LibroViewModel(private val repository: LibroRepository) : ViewModel() {

    val allLibros: LiveData<List<Libro>> = repository.allLibros.asLiveData()
    val allSagas: LiveData<List<String>> = repository.allSagas.asLiveData()

    fun getLibrosBySaga(nombreSaga: String): LiveData<List<Libro>> {
        return repository.getLibrosBySaga(nombreSaga).asLiveData()
    }

    fun getLibroById(id: Int): LiveData<Libro?> {
        val libro = MutableLiveData<Libro?>()
        viewModelScope.launch {
            libro.postValue(repository.getLibroById(id))
        }
        return libro
    }

    fun insertLibros(libros: List<Libro>) = viewModelScope.launch {
        repository.insertLibros(libros)
    }

    fun updateLibro(libro: Libro) = viewModelScope.launch {
        repository.updateLibro(libro)
    }

    fun deleteLibro(libro: Libro) = viewModelScope.launch {
        repository.deleteLibro(libro)
    }

    // Actualizar la sinopsis de un libro
    fun actualizarSinopsis(libroId: Int, sinopsis: String) = viewModelScope.launch {
        repository.actualizarSinopsis(libroId, sinopsis)
    }

    // Actualizar la valoración de un libro
    fun actualizarValoracion(libroId: Int, valoracion: Float) = viewModelScope.launch {
        require(valoracion in 0.0..10.0) { "La valoración debe estar entre 0 y 10." }
        repository.actualizarValoracion(libroId, valoracion)
    }

}

class LibroViewModelFactory(private val repository: LibroRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
        if (modelClass.isAssignableFrom(LibroViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return LibroViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
