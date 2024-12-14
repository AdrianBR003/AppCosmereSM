// LibroViewModel.kt
package com.example.appsandersonsm.ViewModel

import android.util.Log
import androidx.lifecycle.*
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewmodel.CreationExtras
import com.example.appsandersonsm.DataBase.JsonHandler
import com.example.appsandersonsm.Modelo.Libro
import com.example.appsandersonsm.Repository.LibroRepository
import com.google.firebase.Firebase
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class LibroViewModel(private val repository: LibroRepository) : ViewModel() {

    private val firestore = Firebase.firestore


    fun getAllLibrosByUsuario(userId: String): LiveData<List<Libro>> {
        return repository.getAllLibrosByUsuario(userId)
    }
    /**
     * Método para borrar todos los libros de la base de datos.
     */
    fun clearAllBooks() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                repository.deleteAllLibros()
                // Acciones post-eliminación si son necesarias
            } catch (e: Exception) {
                Log.e("YourViewModel", "Error al borrar todos los libros: ${e.message}")
            }
        }
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

    fun actualizarValoracion(libroId: Int, valoracion: Float, userId: String) =
        viewModelScope.launch {
            require(valoracion in 0.0..10.0) { "La valoración debe estar entre 0 y 10." }
            repository.actualizarValoracion(libroId, valoracion, userId)
        }

    fun guardarLibroEnLaNube(libro: Libro) {
        viewModelScope.launch {
            try {
                Log.d(
                    "LibroViewModel",
                    "Iniciando guardado en nube: userId=${libro.userId}, libroId=${libro.id}"
                )

                val userDocRef = firestore.collection("users").document(libro.userId)
                val libroDocRef = userDocRef.collection("libros").document(libro.id.toString())

                val data = mapOf(
                    "id" to libro.id,
                    "nombreLibro" to libro.nombreLibro,
                    "nombreSaga" to libro.nombreSaga,
                    "nombrePortada" to libro.nombrePortada,
                    "progreso" to libro.progreso,
                    "totalPaginas" to libro.totalPaginas,
                    "sinopsis" to libro.sinopsis,
                    "valoracion" to libro.valoracion,
                    "numeroNotas" to libro.numeroNotas,
                    "userId" to libro.userId
                )

                libroDocRef.set(data, SetOptions.merge()).await()
                Log.d("LibroViewModel", "Libro guardado correctamente en la nube.")
            } catch (e: Exception) {
                Log.e(
                    "LibroViewModel",
                    "Error al guardar libro en la nube: ${e.localizedMessage}",
                    e
                )
            }
        }
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
