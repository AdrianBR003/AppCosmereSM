// LibroViewModel.kt
package com.example.appsandersonsm.ViewModel

import android.util.Log
import androidx.lifecycle.*
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewmodel.CreationExtras
import com.example.appsandersonsm.DataBase.JsonHandler
import com.example.appsandersonsm.Modelo.Libro
import com.example.appsandersonsm.Modelo.Nota
import com.example.appsandersonsm.Repository.LibroRepository
import com.google.firebase.Firebase
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext


class LibroViewModel(private val repository: LibroRepository) : ViewModel() {

    private val firestore = FirebaseFirestore.getInstance()
    private val _guardarEstado = MutableLiveData<Boolean>()
    val guardarEstado: LiveData<Boolean> get() = _guardarEstado

    suspend fun obtenerNotasDelLibro(idNotaL: Int, userId: String): List<Nota> {
        return repository.getNotasPorLibroYUsuario(idNotaL, userId)
    }

    fun getAllLibrosByUsuario(userId: String): LiveData<List<Libro>> {
        return repository.getAllLibrosByUsuario(userId)
    }

    fun clearAllBooks() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                repository.deleteAllLibros()
            } catch (e: Exception) {
                Log.e("LibroViewModel", "Error al borrar todos los libros: ${e.message}")
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
            if (valoracion !in 0.0..10.0) {
                Log.e("LibroViewModel", "Valoración inválida: $valoracion")
                return@launch
            }
            repository.actualizarValoracion(libroId, valoracion, userId)
        }

    fun guardarLibroEnLaNube(libro: Libro, notas: List<Nota>) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                if (libro.userId.isEmpty()) {
                    Log.e("LibroViewModel", "userId está vacío.")
                    return@launch
                }
                if (libro.id <= 0) {
                    Log.e("LibroViewModel", "libro.id es inválido: ${libro.id}")
                    return@launch
                }

                for (nota in notas) {
                    if (nota.id <= 0) {
                        Log.e("LibroViewModel", "Nota con id inválido: ${nota.id}")
                        return@launch
                    }
                }

                Log.d(
                    "LibroViewModel",
                    "Iniciando guardado en nube: userId=${libro.userId}, libroId=${libro.id}, idNotaL=${libro.idNotaL}"
                )
                Log.d("LibroViewModel", "Número de notas a guardar: ${notas.size}")

                val userDocRef = firestore.collection("users").document(libro.userId.toString())
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
                    "empezarLeer" to libro.empezarLeer,
                    "userId" to libro.userId,
                    "idNotaL" to libro.idNotaL
                )

                val batch = firestore.batch()

                batch.set(libroDocRef, data, SetOptions.merge())
                Log.d("LibroViewModel", "Añadido libro al batch: ${libro.id}")

                for (nota in notas) {
                    val notaDocRef = libroDocRef.collection("notas").document(nota.id.toString())
                    val notaData = mapOf(
                        "id" to nota.id,
                        "contenido" to nota.contenido,
                        "userId" to nota.userId,
                        "idLibroN" to nota.idLibroN
                    )
                    batch.set(notaDocRef, notaData, SetOptions.merge())
                    Log.d("LibroViewModel", "Añadida nota al batch: ${nota.id}")
                }

                batch.commit().await()
                Log.d("LibroViewModel", "Batch commit completado.")

                Log.d("LibroViewModel", "Libro y notas guardados correctamente en la nube.")

                withContext(Dispatchers.Main) {
                    _guardarEstado.value = true
                }
            } catch (e: Exception) {
                Log.e(
                    "LibroViewModel",
                    "Error al guardar libro y notas en la nube: ${e.localizedMessage}",
                    e
                )
                withContext(Dispatchers.Main) {
                    _guardarEstado.value = false
                }
            }
        }
    }

    /**
     * Factory para crear instancias de LibroViewModel.
     */
    class LibroViewModelFactory(private val repository: LibroRepository) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(LibroViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return LibroViewModel(repository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
