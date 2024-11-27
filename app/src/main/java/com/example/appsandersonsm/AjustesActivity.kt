package com.example.appsandersonsm
import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.example.appsandersonsm.ViewModel.LibroViewModel
import com.example.appsandersonsm.ViewModel.LibroViewModelFactory
import com.google.android.material.bottomnavigation.BottomNavigationView

class AjustesActivity : AppCompatActivity() {

    private lateinit var textViewPaginasLeidas: TextView
    private lateinit var textViewLibrosLeidos: TextView
    private lateinit var textViewSagasLeidas: TextView
    private lateinit var bottomNavigationView: BottomNavigationView

    // Usar LibroViewModel existente
    private val libroViewModel: LibroViewModel by viewModels {
        LibroViewModelFactory((application as InitApplication).libroRepository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ajustes)

        supportActionBar?.hide() // Ocultar la barra superior

        // Inicializar vistas
        textViewPaginasLeidas = findViewById(R.id.textViewPaginasLeidas)
        textViewLibrosLeidos = findViewById(R.id.textViewLibrosLeidos)
        textViewSagasLeidas = findViewById(R.id.textViewSagasLeidas)
        bottomNavigationView = findViewById(R.id.bottomNavigationView)

        // Configurar navegación inferior
        bottomNavigationView.selectedItemId = R.id.nav_settings
        bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_settings -> true
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

        // Observar datos del ViewModel
        observarDatos()
    }

    private fun observarDatos() {
        libroViewModel.allLibros.observe(this) { libros ->
            val totalPaginasLeidas = libros.sumOf { it.progreso }
            val totalLibrosLeidos = libros.count { it.progreso >= it.totalPaginas && it.totalPaginas > 0 }
            textViewPaginasLeidas.text = "Páginas leídas: $totalPaginasLeidas"
            textViewLibrosLeidos.text = "Libros leídos: $totalLibrosLeidos"
        }

        libroViewModel.allSagas.observe(this) { sagas ->
            val sagasLeidas = sagas.filter { saga ->
                val librosDeLaSaga = libroViewModel.getLibrosBySaga(saga).value ?: emptyList()
                librosDeLaSaga.all { libro -> libro.progreso >= libro.totalPaginas && libro.totalPaginas > 0 }
            }

            val sagasLeidasTexto = if (sagasLeidas.isNotEmpty()) {
                "Sagas leídas:\n\n" + sagasLeidas.joinToString(separator = "\n") { "    - $it" }
            } else {
                "Sagas leídas: Ninguna saga completada"
            }
            textViewSagasLeidas.text = sagasLeidasTexto
        }
    }
}
