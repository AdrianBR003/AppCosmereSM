package com.example.appsandersonsm

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.transition.AutoTransition
import android.transition.TransitionManager
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.RatingBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.widget.NestedScrollView
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.appsandersonsm.Adapter.NotasAdapter
import com.example.appsandersonsm.Modelo.Libro
import com.example.appsandersonsm.Modelo.Nota
import com.example.appsandersonsm.Repository.NotaRepository
import com.example.appsandersonsm.ViewModel.LibroViewModel
import com.example.appsandersonsm.ViewModel.LibroViewModelFactory
import com.example.appsandersonsm.ViewModel.NotaViewModel
import com.google.android.material.bottomnavigation.BottomNavigationView

class DetallesLibroActivity : AppCompatActivity(), NotasAdapter.OnNotaClickListener {

    private lateinit var notaViewModel: NotaViewModel
    private lateinit var libroViewModel: LibroViewModel
    private lateinit var progressBar: ProgressBar
    private lateinit var bottomNavigationView: BottomNavigationView
    private lateinit var imagenPortada: ImageView
    private lateinit var editTextProgressCurrent: EditText
    private lateinit var editTextProgressTotal: EditText
    private lateinit var textViewSinopsis: TextView
    private lateinit var textViewNumeroNotas: TextView
    private lateinit var ratingBarValoracion: RatingBar
    private lateinit var notasAdapter: NotasAdapter

    private var libro: Libro? = null
    private var idLibro: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detalles_libro)
        supportActionBar?.hide() // Ocultar la barra de acción predeterminada

        // Obtener el ID del libro del Intent
        idLibro = intent.getIntExtra("LIBRO_ID", 0)
        Log.d("DetallesLibroActivity", "Libro ID: $idLibro")

        inicializarViewModels()
        inicializarVistas()
        configurarRecyclerView()
        configurarBottomNavigation()
        configurarListeners()
        observarNotas()
        inicializarDatosLibro()

    }

    private fun inicializarViewModels() {
        notaViewModel = ViewModelProvider(
            this,
            NotaViewModel.NotaViewModelFactory((application as InitApplication).notaRepository)
        )[NotaViewModel::class.java]

        libroViewModel = ViewModelProvider(
            this,
            LibroViewModelFactory((application as InitApplication).libroRepository)
        )[LibroViewModel::class.java]
    }

    private fun inicializarVistas() {
        progressBar = findViewById(R.id.progressBar)
        bottomNavigationView = findViewById(R.id.bottomNavigationView)
        imagenPortada = findViewById(R.id.portadaImageView)
        editTextProgressCurrent = findViewById(R.id.editTextProgressCurrent)
        editTextProgressTotal = findViewById(R.id.editTextProgressTotal)
        textViewSinopsis = findViewById(R.id.tvSinopsis)
        textViewNumeroNotas = findViewById(R.id.tvNumeroNotas)
        ratingBarValoracion = findViewById(R.id.ratingBar)
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun configurarRecyclerView() {
        val recyclerViewNotas = findViewById<RecyclerView>(R.id.recyclerViewNotas)
        notasAdapter = NotasAdapter(this)
        recyclerViewNotas.layoutManager = LinearLayoutManager(this)
        recyclerViewNotas.adapter = notasAdapter

        recyclerViewNotas.isNestedScrollingEnabled = false
        recyclerViewNotas.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                    recyclerViewNotas.parent.requestDisallowInterceptTouchEvent(
                        recyclerViewNotas.canScrollVertically(-1) || recyclerViewNotas.canScrollVertically(1)
                    )
                }
                else -> recyclerViewNotas.parent.requestDisallowInterceptTouchEvent(false)
            }
            false
        }
    }

    private fun configurarBottomNavigation() {
        bottomNavigationView.selectedItemId = R.id.nav_book
        bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_settings -> startActivity(Intent(this, AjustesActivity::class.java))
                R.id.nav_map -> startActivity(Intent(this, MapaInteractivoActivity::class.java))
                R.id.nav_book -> startActivity(Intent(this, LibroActivity::class.java))
                else -> false
            }
            true
        }
    }

    private fun configurarListeners() {
        editTextProgressCurrent.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                guardarProgresoEnBaseDeDatos()
                updateProgressBar()
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        editTextProgressTotal.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                guardarProgresoEnBaseDeDatos()
                updateProgressBar()
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        ratingBarValoracion.setOnRatingBarChangeListener { _, rating, _ ->
            libro?.let { libroViewModel.actualizarValoracion(it.id, rating) }
        }
    }

    private fun inicializarDatosLibro() {
        // Primero, contar las notas y actualizar el libro en memoria
        contarNotas(idLibro) { numeroNotas ->
            Log.d("DetallesLibroActivity", "Número de notas para libro $idLibro: $numeroNotas")

            // Inicializar notas estaticas
            insertarNotasEstaticasSiNecesario()

            // Una vez se obtienen las notas, cargar los datos del libro
            cargarDatosLibro(idLibro) { libroCargado ->
                libro = libroCargado
                libro?.numeroNotas = numeroNotas
                actualizarUIConDatosLibro(libroCargado)
            }
        }
    }

    private fun contarNotas(libroId: Int, callback: (Int) -> Unit) {
        notaViewModel.contarNotasPorLibro(libroId).observe(this) { numeroNotas ->
            callback(numeroNotas ?: 0)
        }
    }

    private fun cargarDatosLibro(libroId: Int, callback: (Libro?) -> Unit) {
        libroViewModel.getLibroById(libroId).observe(this) { libroCargado ->
            if (libroCargado == null) {
                Log.e("DetallesLibroActivity", "Libro no encontrado en la base de datos para ID: $libroId")
            }
            callback(libroCargado)
        }
    }

    private fun actualizarUIConDatosLibro(libro: Libro?) {
        libro?.let {
            // Actualizar vistas con los datos del libro
            val resID = resources.getIdentifier(it.nombrePortada, "drawable", packageName)
            imagenPortada.setImageResource(resID)
            editTextProgressCurrent.setText(it.progreso.toString())
            editTextProgressTotal.setText(it.totalPaginas.toString())
            textViewSinopsis.text = it.sinopsis ?: "Sinopsis no disponible"
            textViewNumeroNotas.text = it.numeroNotas.toString()
            ratingBarValoracion.rating = it.valoracion
            updateProgressBar()
        }
    }

    private fun guardarProgresoEnBaseDeDatos() {
        val current = editTextProgressCurrent.text.toString().toIntOrNull() ?: 0
        val total = editTextProgressTotal.text.toString().toIntOrNull() ?: 0

        if (total > 0) {
            libro?.let { libroActualizado ->
                libroActualizado.progreso = current
                libroActualizado.totalPaginas = total

                contarNotas(idLibro) { numeroNotas ->
                    libroActualizado.numeroNotas = numeroNotas
                    libroViewModel.updateLibro(libroActualizado)
                    Log.d("DetallesLibroActivity", "Progreso actualizado: $current / $total")
                }
            }
        } else {
            Log.w("DetallesLibroActivity", "Total de páginas inválido, no se actualiza el progreso.")
        }
    }

    private fun updateProgressBar() {
        val current = editTextProgressCurrent.text.toString().toIntOrNull() ?: 0
        val total = editTextProgressTotal.text.toString().toIntOrNull() ?: libro?.totalPaginas ?: 100
        progressBar.max = 100
        progressBar.progress = calcularProgreso(current, total).coerceIn(0, 100)
    }

    private fun calcularProgreso(current: Int, total: Int): Int {
        return if (total > 0) (current * 100) / total else 0
    }

    override fun onNotaClick(nota: Nota) {
        Toast.makeText(this, "Nota seleccionada: ${nota.titulo}", Toast.LENGTH_SHORT).show()
        startActivity(Intent(this, EditarNotaActivity::class.java).apply {
            putExtra("NOTA_ID", nota.id)
        })
    }

    private fun insertarNotasEstaticasSiNecesario() {
        val notasEstaticas = listOf(
            Nota(libroId = idLibro, titulo = "Nota 1", contenido = "Contenido de la nota 1"),
            Nota(libroId = idLibro, titulo = "Nota 2", contenido = "Contenido de la nota 2"),
            Nota(libroId = idLibro, titulo = "Nota 3", contenido = "Contenido de la nota 3")
        )

        notaViewModel.insertarNotasEstaticasSiVacia(notasEstaticas, idLibro)
    }

    private fun observarNotas() {
        notaViewModel.getNotasByLibroId(idLibro).observe(this) { notas ->
            Log.d("DetallesLibroActivity", "Notas cargadas desde ViewModel: $notas") // Verifica que las notas llegan
            notasAdapter.submitList(notas) // Actualiza el RecyclerView con las notas
        }
    }

}
