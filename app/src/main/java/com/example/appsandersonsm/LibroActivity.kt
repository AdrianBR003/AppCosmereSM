package com.example.appsandersonsm

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.appsandersonsm.Adapter.LibroAdapter
import com.example.appsandersonsm.Modelo.Libro
import com.example.appsandersonsm.ViewModel.LibroViewModel
import com.example.appsandersonsm.ViewModel.LibroViewModelFactory
import com.google.android.material.bottomnavigation.BottomNavigationView
import org.json.JSONArray

class LibroActivity : AppCompatActivity() {

    private lateinit var spinnerSagas: Spinner
    private lateinit var spinnerLeido: Spinner
    private lateinit var recyclerViewLibros: RecyclerView
    private lateinit var libroAdapter: LibroAdapter
    private lateinit var bottomNavigationView: BottomNavigationView

    // ViewModel para gestionar los datos de libros
    private val libroViewModel: LibroViewModel by viewModels {
        LibroViewModelFactory((application as InitApplication).libroRepository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_libros)

        supportActionBar?.hide() // Ocultar la barra superior

        // Inicializar vistas
        bottomNavigationView = findViewById(R.id.bottomNavigationView)
        spinnerSagas = findViewById(R.id.spinnerSagas)
        spinnerLeido = findViewById(R.id.spinnerLeido)
        recyclerViewLibros = findViewById(R.id.recyclerViewLibros)
        recyclerViewLibros.layoutManager = LinearLayoutManager(this)

        // Configuración de navegación inferior
        bottomNavigationView.selectedItemId = R.id.nav_book
        bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_settings -> {
                    startActivity(Intent(this, AjustesActivity::class.java))
                    true
                }
                R.id.nav_map -> {
                    startActivity(Intent(this, MapaInteractivoActivity::class.java))
                    true
                }
                R.id.nav_book -> true // Ya estamos aquí
                else -> false
            }
        }

        // Inicializar adaptador del RecyclerView
        libroAdapter = LibroAdapter(this) { libro ->
            val intent = Intent(this, DetallesLibroActivity::class.java)
            intent.putExtra("LIBRO_ID", libro.id)
            startActivity(intent)
        }
        recyclerViewLibros.adapter = libroAdapter

        // Observar los datos del ViewModel
        observarDatos()
    }

    private fun observarDatos() {
        // Observar la lista de libros
        libroViewModel.allLibros.observe(this) { libros ->
            if (!libros.isNullOrEmpty()) {
                configurarSpinnerSagas(libros.map { it.nombreSaga })
                filtrarLibros(libros)
                configurarSpinnerLeido()
            } else {
                Log.d("LibroActivity", "No se encontraron libros.")
            }
        }

        // Observar cambios en los filtros del Spinner
        spinnerSagas.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                libroViewModel.allLibros.value?.let { filtrarLibros(it) }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        spinnerLeido.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                libroViewModel.allLibros.value?.let { filtrarLibros(it) }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun configurarSpinnerLeido() {
        val opcionesLeido = listOf("Todos", "Leídos", "Empezados", "No empezados")
        val spinnerAdapterLeido = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            opcionesLeido
        )
        spinnerAdapterLeido.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerLeido.adapter = spinnerAdapterLeido
    }

    private fun configurarSpinnerSagas(sagas: List<String>) {
        val opcionesSagas = mutableListOf("Todas las sagas").apply { addAll(sagas) }
        val spinnerAdapterSagas = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            opcionesSagas
        )
        spinnerAdapterSagas.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerSagas.adapter = spinnerAdapterSagas
    }

    private fun filtrarLibros(libros: List<Libro>) {
        val sagaSeleccionada = spinnerSagas.selectedItem?.toString() ?: "Todas las sagas"
        val estadoSeleccionado = spinnerLeido.selectedItem?.toString() ?: "Todos"

        // Filtrar los libros según los criterios seleccionados
        val librosFiltrados = libros.filter { libro ->
            val coincideSaga = sagaSeleccionada == "Todas las sagas" || libro.nombreSaga == sagaSeleccionada
            val coincideEstado = when (estadoSeleccionado) {
                "Leídos" -> libro.progreso >= libro.totalPaginas
                "Empezados" -> libro.progreso in 1 until libro.totalPaginas
                "No empezados" -> libro.progreso == 0
                else -> true
            }
            coincideSaga && coincideEstado
        }

        // Actualizar la lista en el adaptador
        libroAdapter.submitList(librosFiltrados)
    }
}
