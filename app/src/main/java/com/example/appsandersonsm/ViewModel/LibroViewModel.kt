// LibroViewModel.kt
package com.example.appsandersonsm.ViewModel

import android.util.Log
import androidx.lifecycle.*
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewmodel.CreationExtras
import com.example.appsandersonsm.DataBase.JsonHandler
import com.example.appsandersonsm.Modelo.Libro
import com.example.appsandersonsm.Repository.LibroRepository
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch

class LibroViewModel(private val repository: LibroRepository) : ViewModel() {

    fun getAllLibrosByUsuario(userId: String): LiveData<List<Libro>> {
        return repository.getAllLibrosByUsuario(userId).asLiveData()
    }

    fun getAllSagasByUsuario(userId: String): LiveData<List<String>> {
        return repository.getAllSagasByUsuario(userId).asLiveData()
    }

    fun getLibrosBySagaAndUsuario(nombreSaga: String, userId: String): LiveData<List<Libro>> {
        return repository.getLibrosBySagaAndUsuario(nombreSaga, userId).asLiveData()
    }

    fun getLibroByIdAndUsuario(id: Int, userId: String): LiveData<Libro?> {
        val libro = MutableLiveData<Libro?>()
        viewModelScope.launch {
            libro.postValue(repository.getLibroByIdAndUsuario(id, userId))
        }
        return libro
    }

    fun updateLocalizacion(languageCode: String, jsonHandler: JsonHandler, userId: String) =
        viewModelScope.launch {
            repository.updateLocalization(languageCode, jsonHandler, userId)
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

    fun actualizarSinopsis(libroId: Int, sinopsis: String, userId: String) = viewModelScope.launch {
        repository.actualizarSinopsis(libroId, sinopsis, userId)
    }

    fun actualizarValoracion(libroId: Int, valoracion: Float, userId: String) = viewModelScope.launch {
        require(valoracion in 0.0..10.0) { "La valoración debe estar entre 0 y 10." }
        repository.actualizarValoracion(libroId, valoracion, userId)
    }

    suspend fun guardarLibroEnLaNube(libro: Libro) {
        val libroData = libro.toFirestore() // Convierte el libro a un mapa para Firestore
        Log.d("LibroViewModel", "Guardando libro en la nube: $libroData")
        FirebaseFirestore.getInstance().collection("libros")
            .document(libro.id.toString())
            .set(libroData)
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
