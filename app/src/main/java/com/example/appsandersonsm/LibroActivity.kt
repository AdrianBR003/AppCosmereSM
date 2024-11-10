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

        // No es necesario esta línea, ya que puede causar conflicto:
        // bottomNavigationView.menu.findItem(R.id.nav_map).isChecked = true

        // Inicializar el Spinner
        spinnerSagas = findViewById(R.id.spinnerSagas)

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
        // Obtener la saga seleccionada actualmente
        val sagaSeleccionada = spinnerSagas.selectedItem.toString()
        // Filtrar la lista según la saga seleccionada
        filtrarLibrosPorSaga(sagaSeleccionada)
    }

    private fun configurarSpinner() {
        // Añadir la opción "Todas las sagas" al inicio de la lista
        val opcionesSagas = mutableListOf("Todas las sagas")
        opcionesSagas.addAll(listaSagas)

        // Crear un ArrayAdapter para el Spinner
        val spinnerAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, opcionesSagas)
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerSagas.adapter = spinnerAdapter

        // Manejar la selección del Spinner
        spinnerSagas.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val sagaSeleccionada = opcionesSagas[position]
                filtrarLibrosPorSaga(sagaSeleccionada)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                // Opcional: manejar cuando no se selecciona nada
            }
        }
    }

    private fun configurarRecyclerView() {
        libroAdapter = LibroAdapter(this, listaLibros) { libro ->
            val intent = Intent(this, DetallesLibroActivity::class.java)
            intent.putExtra("LIBRO_ID", libro.id)
            startActivity(intent)
        }
        recyclerViewLibros.adapter = libroAdapter
    }

    private fun filtrarLibrosPorSaga(saga: String) {
        val listaFiltrada = if (saga == "Todas las sagas") {
            listaLibros
        } else {
            listaLibros.filter { it.nombreSaga == saga }
        }
        libroAdapter.actualizarLista(listaFiltrada)
    }
}
