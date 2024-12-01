package com.example.appsandersonsm

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.appsandersonsm.API.NewsApiService
import com.example.appsandersonsm.Adapter.NoticiasAdapter
import com.example.appsandersonsm.Modelo.Noticia
import com.example.appsandersonsm.Repository.LibroRepository
import com.example.appsandersonsm.ViewModel.LibroViewModel
import com.example.appsandersonsm.ViewModel.LibroViewModelFactory
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.crashlytics.buildtools.reloc.org.apache.commons.logging.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class AjustesActivity : AppCompatActivity() {

    private lateinit var textViewPaginasLeidas: TextView
    private lateinit var textViewLibrosLeidos: TextView
    private lateinit var textViewSagasLeidas: TextView
    private lateinit var bottomNavigationView: BottomNavigationView
    private lateinit var recyclerViewNoticias: RecyclerView
    private lateinit var progressBar: ProgressBar


    private lateinit var libroRepository: LibroRepository


    // Usar LibroViewModel existente
    private val libroViewModel: LibroViewModel by viewModels {
        val app = application as? InitApplication
            ?: throw IllegalStateException("Application is not InitApplication")
        LibroViewModelFactory(app.libroRepository)
    }

    // API
    val retrofit = Retrofit.Builder()
        .baseUrl("https://newsapi.org/v2/") // Base URL de News API
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val newsApi = retrofit.create(NewsApiService::class.java)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ajustes)

        libroRepository = (application as InitApplication).libroRepository

        supportActionBar?.hide() // Ocultar la barra superior

        // Inicializar vistas
        textViewPaginasLeidas = findViewById(R.id.textViewPaginasLeidas)
        textViewLibrosLeidos = findViewById(R.id.textViewLibrosLeidos)
        textViewSagasLeidas = findViewById(R.id.textViewSagasLeidas)
        bottomNavigationView = findViewById(R.id.bottomNavigationView)
        recyclerViewNoticias = findViewById(R.id.recyclerViewNoticias)
        progressBar = findViewById(R.id.progressBarN)


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

        // Configurar RecyclerView
        recyclerViewNoticias.layoutManager =
            LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)

        // Observar datos del ViewModel
        observarDatos()

        // Fetch de noticias reales

        fetchNoticias()
    }

    private fun observarDatos() {
        // Observa los libros
        libroViewModel.allLibros.observe(this) { libros ->
            // Filtra los libros terminados (progreso == totalPaginas y totalPaginas > 0)
            val librosTerminados =
                libros.filter { it.progreso == it.totalPaginas && it.totalPaginas > 0 }
            val totalPaginasLeidas = librosTerminados.sumOf { it.progreso }
            val totalLibrosLeidos = librosTerminados.size

            // Actualiza los TextView con las métricas
            textViewPaginasLeidas.text = "Páginas leídas: $totalPaginasLeidas"
            textViewLibrosLeidos.text = "Libros leídos: $totalLibrosLeidos"

            // Agrupa libros terminados por saga
            val librosPorSaga = libros.groupBy { it.nombreSaga }

            // Identifica las sagas leídas
            val sagasLeidas = librosPorSaga.filter { (_, librosDeLaSaga) ->
                librosDeLaSaga.isNotEmpty() && librosDeLaSaga.all { libro ->
                    libro.progreso == libro.totalPaginas && libro.totalPaginas > 0
                }
            }.keys // Obtén los nombres de las sagas leídas

            // Crea el texto para las sagas leídas
            val sagasLeidasTexto = if (sagasLeidas.isNotEmpty()) {
                "Sagas leídas:\n\n" + sagasLeidas.joinToString(separator = "\n") { "    - $it" }
            } else {
                "Sagas leídas: Ninguna saga completada"
            }
            textViewSagasLeidas.text = sagasLeidasTexto
        }
    }

    private fun fetchNoticias() {
        val apiKey = "6c1e1e7d1bdf4283867fd5d85fd2744e"
        val query = "Brandon Sanderson OR Cosmere"
        val languages = listOf("en", "es") // Idiomas: inglés y español

        // Mostrar la barra de carga y ocultar el RecyclerView
        progressBar.visibility = View.VISIBLE
        recyclerViewNoticias.visibility = View.GONE

        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Realizar llamadas concurrentes a la API para cada idioma
                val responses = languages.map { language ->
                    async {
                        newsApi.getNews(query, apiKey, language)
                    }
                }

                // Esperar todas las respuestas
                val articles = responses.awaitAll().flatMap { response ->
                    if (response.status == "ok") response.articles else emptyList()
                }

                // Filtrar las noticias estrictamente por "Brandon Sanderson" o "Cosmere"
                val noticiasFiltradas = articles.filter { article ->
                    val lowerCaseTitle = article.title.lowercase()
                    val lowerCaseDescription = article.description?.lowercase() ?: ""

                    lowerCaseTitle.contains("brandon sanderson") ||
                            lowerCaseTitle.contains("cosmere") ||
                            lowerCaseDescription.contains("brandon sanderson") ||
                            lowerCaseDescription.contains("cosmere")
                }.map { article ->
                    // Mapear a la clase Noticia
                    Noticia(
                        titulo = article.title,
                        descripcion = article.description ?: "Sin descripción",
                        enlace = article.url,
                        imagenUrl = article.urlToImage ?: ""
                    )
                }

                withContext(Dispatchers.Main) {
                    if (noticiasFiltradas.isNotEmpty()) {
                        recyclerViewNoticias.adapter = NoticiasAdapter(noticiasFiltradas)
                        recyclerViewNoticias.visibility = View.VISIBLE
                    } else {
                        Toast.makeText(
                            this@AjustesActivity,
                            "No se encontraron noticias relacionadas con Brandon Sanderson o Cosmere.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    progressBar.visibility = View.GONE
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    progressBar.visibility = View.GONE
                    Toast.makeText(
                        this@AjustesActivity,
                        "Error al cargar noticias: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }
}
