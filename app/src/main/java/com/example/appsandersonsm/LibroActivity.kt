package com.example.appsandersonsm

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.RatingBar
import android.widget.Spinner
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.appsandersonsm.Adapter.LibroAdapter
import com.example.appsandersonsm.DataBase.JsonHandler
import com.example.appsandersonsm.Modelo.Libro
import com.example.appsandersonsm.ViewModel.LibroViewModel
import com.example.appsandersonsm.ViewModel.LibroViewModelFactory
import com.google.android.material.bottomnavigation.BottomNavigationView
import org.json.JSONArray
import java.util.Locale

class LibroActivity : AppCompatActivity() {

    private lateinit var spinnerSagas: Spinner
    private lateinit var spinnerLeido: Spinner
    private lateinit var recyclerViewLibros: RecyclerView
    private lateinit var libroAdapter: LibroAdapter
    private lateinit var bottomNavigationView: BottomNavigationView
    private lateinit var ratingBar: RatingBar
    private var idLibro: Int = 0
    private var userId = ""

    // ViewModel para gestionar los datos de libros
    private val libroViewModel: LibroViewModel by viewModels {
        LibroViewModelFactory((application as InitApplication).libroRepository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_libros)

        supportActionBar?.hide() // Ocultar la barra superior

        // Coger el ID del Intent del Login
        userId = intent.getStringExtra("USER_ID") ?: ""


        // Intent
        idLibro = intent.getIntExtra("LIBRO_ID", 0)
        Log.d("LibroActivity", "Libro ID: $idLibro")

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

        // Actualizar localización de los datos según el idioma seleccionado
        aplicarLocalizacionDatos()

        // Observar los datos del ViewModel
        observarDatos()
    }

    private fun aplicarLocalizacionDatos() {
        // Leer el idioma guardado en SharedPreferences
        val prefs = getSharedPreferences("AppPreferences", Context.MODE_PRIVATE)
        val languageCode = prefs.getString("language", Locale.getDefault().language) ?: "es"

        // Crear JsonHandler con el dao, sin modificar la clase
        val jsonHandler = JsonHandler(this, (application as InitApplication).database.libroDao())

        // Llamar a la función del ViewModel para actualizar la localización
        libroViewModel.updateLocalizacion(languageCode, jsonHandler, userId)
    }

    private fun observarDatos() {
        // Observar la lista de libros
        libroViewModel.getAllLibrosByUsuario(userId).observe(this) { libros ->
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
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                libroViewModel.getAllLibrosByUsuario(userId).value?.let { filtrarLibros(it) }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        spinnerLeido.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                libroViewModel.getAllLibrosByUsuario(userId).value?.let { filtrarLibros(it) }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun configurarSpinnerLeido() {
        // Obtener las opciones desde los recursos de strings
        val opcionesLeido = listOf(
            getString(R.string.spinner_leido_all),
            getString(R.string.spinner_leido_read),
            getString(R.string.spinner_leido_started),
            getString(R.string.spinner_leido_not_started)
        )

        val spinnerAdapterLeido = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            opcionesLeido
        )
        spinnerAdapterLeido.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerLeido.adapter = spinnerAdapterLeido
    }

    private fun configurarSpinnerSagas(sagas: List<String>) {
        // Obtener el texto "Todas las sagas" desde los recursos de strings
        val todasLasSagas = getString(R.string.spinner_sagas_all_sagas)

        // Crear una lista única eliminando duplicados y agregando la opción "Todas las sagas"
        val opcionesSagas = mutableListOf(todasLasSagas).apply {
            addAll(sagas.distinct())
        }

        val spinnerAdapterSagas = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            opcionesSagas
        )
        spinnerAdapterSagas.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerSagas.adapter = spinnerAdapterSagas
    }

    private fun filtrarLibros(libros: List<Libro>) {
        // Obtener las selecciones actuales de los spinners
        val sagaSeleccionada = spinnerSagas.selectedItem?.toString() ?: getString(R.string.spinner_sagas_all_sagas)
        val estadoSeleccionado = spinnerLeido.selectedItem?.toString() ?: getString(R.string.spinner_leido_all)

        // Filtrar los libros según los criterios seleccionados
        val librosFiltrados = libros.filter { libro ->
            val coincideSaga =
                sagaSeleccionada == getString(R.string.spinner_sagas_all_sagas) || libro.nombreSaga == sagaSeleccionada
            val coincideEstado = when (estadoSeleccionado) {
                getString(R.string.spinner_leido_read) -> libro.progreso >= libro.totalPaginas
                getString(R.string.spinner_leido_started) -> libro.progreso in 1 until libro.totalPaginas
                getString(R.string.spinner_leido_not_started) -> libro.progreso == 0
                else -> true
            }
            coincideSaga && coincideEstado
        }

        // Actualizar la lista en el adaptador
        libroAdapter.submitList(librosFiltrados)
    }
}
