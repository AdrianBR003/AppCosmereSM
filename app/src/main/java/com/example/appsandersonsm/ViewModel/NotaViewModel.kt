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
    val notas: LiveData<List<Nota>> get() = _notas

    init {
        viewModelScope.launch {
            inicializarNotasEstaticas() // Insertar las notas estáticas
        }
    }


    fun inicializarNotasEstaticas() {
        val notasEstaticas = listOf(
            Nota(
                1,
                1,
                "Nota predeterminada 1",
                "Contenido predeterminado 1",
                "2024-01-01",
                "2024-01-01"
            ),
            Nota(
                2,
                1,
                "Nota predeterminada 2",
                "Contenido predeterminado 2",
                "2024-01-01",
                "2024-01-01"
            ),
            Nota(
                3,
                1,
                "Nota predeterminada 3",
                "Contenido predeterminado 3",
                "2024-01-01",
                "2024-01-01"
            ),
            Nota(
                4,
                1,
                "Nota predeterminada 2",
                "Contenido predeterminado 2",
                "2024-01-01",
                "2024-01-01"
            ),
            Nota(
                5,
                2,
                "Nota predeterminada 3",
                "Contenido predeterminado 3",
                "2024-01-01",
                "2024-01-01"
            ),
            Nota(
                6,
                2,
                "Nota predeterminada 2",
                "Contenido predeterminado 2",
                "2024-01-01",
                "2024-01-01"
            ),
            Nota(
                7,
                2,
                "Nota predeterminada 3",
                "Contenido predeterminado 3",
                "2024-01-01",
                "2024-01-01"
            ),
            Nota(
                8,
                2,
                "Nota predeterminada 2",
                "Contenido predeterminado 2",
                "2024-01-01",
                "2024-01-01"
            ),
            Nota(
                9,
                2,
                "Nota predeterminada 3",
                "Contenido predeterminado 3",
                "2024-01-01",
                "2024-01-01"
            )
        )

        viewModelScope.launch {
            notasEstaticas.forEach { nota ->
                try {
                    repository.insertOrUpdateNota(nota) // Método que usa OnConflictStrategy.REPLACE
                } catch (e: SQLiteConstraintException) {
                    Log.e("NotaViewModel", "Error al insertar nota: ${e.message}")
                }
            }
        }
    }

        fun getNotaById(id: Int): LiveData<Nota> {
            return repository.getNotaById(id)
        }

        fun getNotasByLibroId(libroId: Int): LiveData<List<Nota>> {
            return repository.getNotasByLibroId(libroId).asLiveData()
        }

        fun getNotasPorLibro(libroId: Int): LiveData<List<Nota>> {
            return repository.getNotasByLibroId(libroId).asLiveData()
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


        fun addNota(nota: Nota) {
            val currentNotas = _notas.value?.toMutableList() ?: mutableListOf()
            currentNotas.add(nota)
            _notas.value = currentNotas

            Log.d("NotaViewModel", "Nueva nota añadida: $nota")
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

