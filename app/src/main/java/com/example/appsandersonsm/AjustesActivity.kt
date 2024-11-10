package com.example.appsandersonsm

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.appsandersonsm.Repositorio.DatabaseHelper
import com.google.android.material.bottomnavigation.BottomNavigationView

class AjustesActivity : AppCompatActivity() {

    private lateinit var textViewPaginasLeidas: TextView
    private lateinit var textViewLibrosLeidos: TextView
    private lateinit var textViewSagasLeidas: TextView
    private lateinit var dbHelper: DatabaseHelper
    private lateinit var bottomNavigationView: BottomNavigationView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ajustes)

        supportActionBar?.hide() // Hide default topbar with app name

        // Inicializar vistas
        textViewPaginasLeidas = findViewById(R.id.textViewPaginasLeidas)
        textViewLibrosLeidos = findViewById(R.id.textViewLibrosLeidos)
        textViewSagasLeidas = findViewById(R.id.textViewSagasLeidas)
        bottomNavigationView = findViewById(R.id.bottomNavigationView)

        // Inicializar DatabaseHelper
        dbHelper = DatabaseHelper(this)

        // Cargar y mostrar las estadísticas
        cargarEstadisticas()

        // Configurar el listener del BottomNavigationView
        bottomNavigationView.selectedItemId = R.id.nav_settings

        bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_settings -> {
                    // Ya estamos en AjustesActivity, no hacemos nada
                    true
                }
                R.id.nav_map -> {
                    startActivity(Intent(this, MapaInteractivoActivity::class.java))
                    true
                }
                R.id.nav_book -> {
                    startActivity(Intent(this, LibroActivity::class.java))
                    true
                }
                else -> false
            }
        }
    }

    private fun cargarEstadisticas() {
        // Obtener todos los libros
        val libros = dbHelper.getAllLibros()

        // Calcular el número total de páginas leídas y libros leídos
        var totalPaginasLeidas = 0
        var totalLibrosLeidos = 0

        libros.forEach { libro ->
            totalPaginasLeidas += libro.progreso
            if (libro.progreso >= libro.totalPaginas && libro.totalPaginas > 0) {
                totalLibrosLeidos += 1
            }
        }

        // Calcular los nombres de las sagas leídas
        val sagas = dbHelper.getAllSagas()
        val sagasLeidas = mutableListOf<String>()

        sagas.forEach { saga ->
            val librosDeLaSaga = dbHelper.getLibrosBySagaId(saga)
            val todosLibrosLeidos = librosDeLaSaga.all { libro ->
                libro.progreso >= libro.totalPaginas && libro.totalPaginas > 0
            }
            if (todosLibrosLeidos && librosDeLaSaga.isNotEmpty()) {
                sagasLeidas.add(saga) // Agregar el nombre de la saga leída
            }
        }

        // Formatear la lista de sagas leídas en el formato solicitado
        val sagasLeidasTexto = if (sagasLeidas.isNotEmpty()) {
            "Sagas leídas:\n\n" + sagasLeidas.joinToString(separator = "\n") { "    - $it" }
        } else {
            "Sagas leídas: Ninguna saga completada"
        }

        // Actualizar los TextViews con las estadísticas
        textViewPaginasLeidas.text = "Páginas leídas: $totalPaginasLeidas"
        textViewLibrosLeidos.text = "Libros leídos: $totalLibrosLeidos"
        textViewSagasLeidas.text = sagasLeidasTexto
    }
}
