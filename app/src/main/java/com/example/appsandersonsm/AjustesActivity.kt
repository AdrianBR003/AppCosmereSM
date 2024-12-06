package com.example.appsandersonsm

import android.animation.Animator
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.TransitionDrawable
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.Button
import android.widget.ProgressBar
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.appsandersonsm.API.NewsApiService
import com.example.appsandersonsm.Adapter.NoticiasAdapter
import com.example.appsandersonsm.Modelo.Noticia
import com.example.appsandersonsm.Repository.LibroRepository
import com.example.appsandersonsm.ViewModel.LibroViewModel
import com.example.appsandersonsm.ViewModel.LibroViewModelFactory
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.Locale

class AjustesActivity : AppCompatActivity() {

    private lateinit var textViewPaginasLeidas: TextView
    private lateinit var textViewLibrosLeidos: TextView
    private lateinit var textViewSagasLeidas: TextView
    private lateinit var bottomNavigationView: BottomNavigationView
    private lateinit var recyclerViewNoticias: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var textViewSagasEmpezadas: TextView

    private lateinit var libroRepository: LibroRepository
    private lateinit var textViewError: TextView

    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var textSCNombre: TextView

    private lateinit var toggleButton: RelativeLayout
    private lateinit var toggleButtonText: TextView
    private var isEN: Boolean = false // Estado inicial: ES

    val defaultLanguage =
        Locale.getDefault().language ?: "es" // Si el idioma es nulo, por defecto "es" (español)

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
        textViewError = findViewById(R.id.errorInternet)
        textViewSagasEmpezadas = findViewById(R.id.textViewSagasEmpezadas)
        textSCNombre = findViewById(R.id.textCSNombre)
        toggleButton = findViewById(R.id.toggleButton)
        toggleButtonText = findViewById(R.id.toggleButtonText)

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

        // Enlaces

        // Configurar los enlaces
        findViewById<TextView>(R.id.tvEnlace1).setOnClickListener { // Cosmere.es
            openWebPage("https://cosmere.es/")
        }

        findViewById<TextView>(R.id.tvEnlace2).setOnClickListener {
            openWebPage("https://es.coppermind.net/wiki/Coppermind:Bienvenidos")
        }

        findViewById<TextView>(R.id.tvEnlace3).setOnClickListener {
            openWebPage("https://www.brandonsanderson.com/")
        }

        findViewById<TextView>(R.id.tvEnlace4).setOnClickListener {
            openWebPage("https://x.com/brandsanderson")
        }

        // Referencia al botón de cerrar sesión
        val btnLogout = findViewById<Button>(R.id.btn_logout)
        btnLogout.setOnClickListener {
            signOut()
        }

        // Cerrar Sesion
        configureGoogleSignIn()

        // Boton Idiomas
        toggleButtonText.text = "ES"

        // Configurar el listener de clic
        toggleButton.setOnClickListener {
            toggleState()
        }

    }

    private fun configureGoogleSignIn() {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)
        val account = GoogleSignIn.getLastSignedInAccount(this)
        if (account != null) {
            textSCNombre.text = account.displayName
        } else {
            textSCNombre.text = if (defaultLanguage == "es") "Invitado" else "Guest"
        }
    }

    private fun signOut() {
        // Cerrar sesión en Google
        googleSignInClient.signOut().addOnCompleteListener(this) {
            // Restablecer el valor de isLoginSkipped en SharedPreferences
            val sharedPreferences = getSharedPreferences("AppPreferences", MODE_PRIVATE)
            sharedPreferences.edit().putBoolean("isLoginSkipped", false).apply()

            // Mostrar mensaje de confirmación
            Toast.makeText(this, "Has cerrado sesión", Toast.LENGTH_SHORT).show()

            // Redirigir al usuario a la pantalla de inicio de sesión
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(intent)
            finish()
        }
    }


    private fun observarDatos() {
        // Observa los libros
        libroViewModel.allLibros.observe(this) { libros ->
            // Filtra los libros terminados (progreso == totalPaginas y totalPaginas > 0)
            val librosTerminados =
                libros.filter { it.progreso == it.totalPaginas && it.totalPaginas > 0 }
            val totalPaginasLeidas = librosTerminados.sumOf { it.progreso }
            val totalLibrosLeidos = librosTerminados.size

            // Idioma por defecto del sistema
            val defaultLanguage = Locale.getDefault().language ?: "es"

            // Actualiza los TextView con las métricas
            textViewPaginasLeidas.text = if (defaultLanguage == "es") {
                "Páginas leídas: $totalPaginasLeidas"
            } else {
                "Pages Read: $totalPaginasLeidas"
            }

            textViewLibrosLeidos.text = if (defaultLanguage == "es") {
                "Libros leídos: $totalLibrosLeidos"
            } else {
                "Books Read: $totalLibrosLeidos"
            }

            // Agrupa libros por saga
            val librosPorSaga = libros.groupBy { it.nombreSaga }

            // Sagas empezadas
            val sagasEmpezadasTexto = if (librosPorSaga.isNotEmpty()) {
                val textoSagas = librosPorSaga.entries.mapNotNull { (saga, librosDeLaSaga) ->
                    val librosEmpezados =
                        librosDeLaSaga.filter { it.progreso > 0 && it.progreso < it.totalPaginas }
                    if (librosEmpezados.isNotEmpty()) {
                        if (saga == "Libro Independiente") {
                            librosEmpezados.joinToString(separator = "\n") { libro -> "    - ${libro.nombreLibro} [NI]" }
                        } else {
                            "    - $saga"
                        }
                    } else {
                        null
                    }
                }.joinToString(separator = "\n").trim()

                if (textoSagas.isNotEmpty()) {
                    if (defaultLanguage == "es") {
                        "Sagas empezadas:\n\n$textoSagas"
                    } else {
                        "Sagas Started:\n\n$textoSagas"
                    }
                } else {
                    if (defaultLanguage == "es") {
                        "Sagas empezadas: 0"
                    } else {
                        "Sagas Started: 0"
                    }
                }
            } else {
                if (defaultLanguage == "es") {
                    "Sagas empezadas: 0"
                } else {
                    "Sagas Started: 0"
                }
            }
            textViewSagasEmpezadas.text = sagasEmpezadasTexto

            // Sagas leídas
            val sagasLeidasTexto = if (librosPorSaga.isNotEmpty()) {
                val textoSagas = librosPorSaga.entries.mapNotNull { (saga, librosDeLaSaga) ->
                    if (saga == "Libro Independiente") {
                        val librosIndependientes =
                            librosDeLaSaga.filter { it.progreso == it.totalPaginas && it.totalPaginas > 0 }
                        if (librosIndependientes.isNotEmpty()) {
                            librosIndependientes.joinToString(separator = "\n") { libro -> "    - ${libro.nombreLibro} [NI]" }
                        } else {
                            null
                        }
                    } else {
                        if (librosDeLaSaga.all { it.progreso == it.totalPaginas && it.totalPaginas > 0 }) {
                            "    - $saga"
                        } else {
                            null
                        }
                    }
                }.joinToString(separator = "\n").trim()

                if (textoSagas.isNotEmpty()) {
                    if (defaultLanguage == "es") {
                        "Sagas leídas:\n\n$textoSagas"
                    } else {
                        "Sagas Read:\n\n$textoSagas"
                    }
                } else {
                    if (defaultLanguage == "es") {
                        "Sagas leídas: Ninguna saga completada"
                    } else {
                        "Sagas Read: No sagas completed"
                    }
                }
            } else {
                if (defaultLanguage == "es") {
                    "Sagas leídas: Ninguna saga completada"
                } else {
                    "Sagas Read: No sagas completed"
                }
            }
            textViewSagasLeidas.text = sagasLeidasTexto
        }
    }

    private fun fetchNoticias() {
        val apiKey = "6c1e1e7d1bdf4283867fd5d85fd2744e"
        val query = "Brandon Sanderson OR Cosmere"
        val languages = listOf("en", "es") // Idiomas: inglés y español

        // Verificar si hay conexión a Internet
        if (!isNetworkAvailable()) {
            runOnUiThread {
                progressBar.visibility = View.GONE
                textViewError.visibility = View.VISIBLE
            }
            return
        }
        // Mostrar la barra de carga y ocultar el RecyclerView
        progressBar.visibility = View.VISIBLE
        recyclerViewNoticias.visibility = View.GONE
        textViewError.visibility = View.GONE
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
                    textViewError.visibility = View.VISIBLE
                }
            }
        }
    }

    private fun isNetworkAvailable(): Boolean {
        val connectivityManager =
            getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork
        val capabilities = connectivityManager.getNetworkCapabilities(network)
        return capabilities != null &&
                capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }


    private fun openWebPage(url: String) {
        try {
            val customTabsIntent = CustomTabsIntent.Builder().build()
            customTabsIntent.launchUrl(this, Uri.parse(url))
        } catch (e: Exception) {
            Toast.makeText(this, "Error al abrir el enlace.", Toast.LENGTH_SHORT).show()
            Log.e("OpenWebPage", "Error: ${e.message}")
        }
    }

    private fun toggleState() {
        isEN = !isEN // Alternar el estado

        val newText = if (isEN) "EN" else "ES"
        val newDrawableRes = if (isEN) R.drawable.rounded_en else R.drawable.rounded_es
        val newTextColor = if (isEN) ContextCompat.getColor(this, R.color.white) else ContextCompat.getColor(this, R.color.gold_s)

        // Cambiar dinámicamente el fondo con una transición
        val currentBackground = toggleButton.background
        val newBackground = ContextCompat.getDrawable(this, newDrawableRes)

        // Configurar la transición entre drawables
        val transitionDrawable = TransitionDrawable(arrayOf(currentBackground, newBackground))
        toggleButton.background = transitionDrawable
        transitionDrawable.startTransition(300) // Duración de la transición

        // Calcular el desplazamiento vertical
        toggleButton.post {
            val maxTranslationY = toggleButton.height - toggleButtonText.height - toggleButton.paddingTop - toggleButton.paddingBottom
            val translationY = if (isEN) maxTranslationY.toFloat() else 0f

            // Animar el movimiento vertical del texto
            val animTextTranslation = ObjectAnimator.ofFloat(toggleButtonText, "translationY", translationY)

            // Animar el cambio de color del texto
            val currentTextColor = toggleButtonText.currentTextColor
            val animTextColor = ObjectAnimator.ofArgb(toggleButtonText, "textColor", currentTextColor, newTextColor)

            // Configurar las duraciones y el interpolador
            animTextTranslation.duration = 300
            animTextColor.duration = 300

            animTextTranslation.interpolator = AccelerateDecelerateInterpolator()
            animTextColor.interpolator = AccelerateDecelerateInterpolator()

            // Actualizar el texto al final de la animación
            animTextTranslation.addListener(object : android.animation.AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    toggleButtonText.text = newText
                }
            })

            // Ejecutar las animaciones juntas
            val set = AnimatorSet()
            set.playTogether(animTextTranslation, animTextColor)
            set.start()
        }
    }


}
