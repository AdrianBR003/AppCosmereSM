package com.example.appsandersonsm

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.appsandersonsm.Modelo.Libro
import com.example.appsandersonsm.Repositorio.DatabaseHelper
import com.google.android.material.bottomnavigation.BottomNavigationView

class LibroActivity : AppCompatActivity() {

    private lateinit var spinnerSagas: Spinner
    private lateinit var spinnerLeido: Spinner
    private lateinit var recyclerViewLibros: RecyclerView
    private lateinit var libroAdapter: LibroAdapter
    private lateinit var listaLibros: List<Libro>
    private lateinit var listaSagas: List<String>
    private lateinit var dbHelper: DatabaseHelper
    private lateinit var bottomNavigationView: BottomNavigationView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_libros)

        supportActionBar?.hide() // Hide default topbar with app name

        // Inicializar el BottomNavigationView
        bottomNavigationView = findViewById(R.id.bottomNavigationView)

        // Configuración de BottomNavigationView
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

                R.id.nav_book -> {
                    // Ya estamos en LibroActivity, no hacemos nada
                    true
                }

                else -> false
            }
        }

        // Inicializar el Spinner
        spinnerSagas = findViewById(R.id.spinnerSagas)
        spinnerLeido = findViewById(R.id.spinnerLeido)

        // Inicializar el RecyclerView
        recyclerViewLibros = findViewById(R.id.recyclerViewLibros)
        recyclerViewLibros.layoutManager = LinearLayoutManager(this)

        // Obtener los datos de la base de datos
        dbHelper = DatabaseHelper(this)
        listaLibros = dbHelper.getAllLibros()
        listaSagas = dbHelper.getAllSagas()

        // Configurar el Spinner y el RecyclerView
        configurarSpinner()
        configurarRecyclerView()
    }

    override fun onResume() {
        super.onResume()
        // Actualizar la lista de libros desde la base de datos
        listaLibros = dbHelper.getAllLibros()
        // Filtrar los libros según los filtros actuales
        filtrarLibros()
    }

    private fun configurarSpinner() {
        // Opciones para el Spinner de Estado de Lectura
        val opcionesEstadoLectura = listOf("Todos", "Leídos", "Empezados", "No empezados")

        // Configurar el Adapter del Spinner de Estado de Lectura
        val estadoLecturaAdapter =
            ArrayAdapter(this, android.R.layout.simple_spinner_item, opcionesEstadoLectura)
        estadoLecturaAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerLeido.adapter = estadoLecturaAdapter

        // Opciones para el Spinner de Sagas
        val opcionesSagas = mutableListOf("Todas las sagas")
        opcionesSagas.addAll(listaSagas)

        // Configurar el Adapter del Spinner de Sagas
        val spinnerAdapterSagas =
            ArrayAdapter(this, android.R.layout.simple_spinner_item, opcionesSagas)
        spinnerAdapterSagas.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerSagas.adapter = spinnerAdapterSagas

        // Manejar la selección en el Spinner de Sagas
        spinnerSagas.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                filtrarLibros() // Llamar al método de filtrado cada vez que cambia la saga seleccionada
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                // Opcional: manejar cuando no se selecciona nada
            }
        }

        // Manejar la selección en el Spinner de Estado de Lectura
        spinnerLeido.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                filtrarLibros() // Llamar al método de filtrado cada vez que cambia el estado de lectura seleccionado
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                // Opcional: manejar cuando no se selecciona nada
            }
        }
    }

    private fun filtrarLibros() {
        val sagaSeleccionada = spinnerSagas.selectedItem.toString()
        val estadoSeleccionado = spinnerLeido.selectedItem.toString()

        val librosFiltrados = listaLibros.filter { libro ->
            // Filtrar por saga seleccionada
            val coincideSaga =
                sagaSeleccionada == "Todas las sagas" || libro.nombreSaga == sagaSeleccionada

            // Filtrar por estado de lectura seleccionado
            val coincideEstado = when (estadoSeleccionado) {
                "Leídos" -> libro.progreso >= libro.totalPaginas
                "Empezados" -> libro.progreso in 1 until libro.totalPaginas
                "No empezados" -> libro.progreso == 0
                else -> true // "Todos" selecciona cualquier progreso
            }

            coincideSaga && coincideEstado
        }

        // Actualizar el RecyclerView con la lista filtrada
        libroAdapter.actualizarLista(librosFiltrados)
    }

    private fun configurarRecyclerView() {
        libroAdapter = LibroAdapter(this, listaLibros) { libro ->
            val intent = Intent(this, DetallesLibroActivity::class.java)
            intent.putExtra("LIBRO_ID", libro.id)
            startActivity(intent)
        }
        recyclerViewLibros.adapter = libroAdapter
    }
}
