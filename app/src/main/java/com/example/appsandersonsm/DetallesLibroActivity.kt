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
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.appsandersonsm.Adapter.NotasAdapter
import com.example.appsandersonsm.Modelo.Libro
import com.example.appsandersonsm.Modelo.Nota
import com.example.appsandersonsm.ViewModel.LibroViewModel
import com.example.appsandersonsm.ViewModel.LibroViewModelFactory
import com.example.appsandersonsm.ViewModel.NotaViewModel
import com.google.android.material.bottomnavigation.BottomNavigationView

class DetallesLibroActivity : AppCompatActivity(),NotasAdapter.OnNotaClickListener {


    private lateinit var notaViewModel: NotaViewModel
    private lateinit var progressBar: ProgressBar
    private lateinit var bottomNavigationView: BottomNavigationView
    private lateinit var imagenPortada: ImageView
    private lateinit var editTextProgressCurrent: EditText
    private lateinit var editTextProgressTotal: EditText
    private lateinit var textViewSinopsis: TextView
    private lateinit var textViewNumeroNotas: TextView
    private lateinit var ratingBarValoracion: RatingBar
    private var libro: Libro? = null
    private val libroViewModel: LibroViewModel by viewModels {
        LibroViewModelFactory((application as InitApplication).libroRepository)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        setContentView(R.layout.activity_detalles_libro)

        // Inicializacion RecyclerViewNotas
        val recyclerViewNotas = findViewById<RecyclerView>(R.id.recyclerViewNotas)
        val notasAdapter = NotasAdapter(this)
        recyclerViewNotas.layoutManager = LinearLayoutManager(this)
        recyclerViewNotas.adapter = notasAdapter

        // Carga de datos estáticos
        val notasEstaticas = listOf(
            Nota(1, 1, "Contenido de la nota estática 1", "2024-01-01"),
            Nota(2, 1, "Contenido de la nota estática 2", "2024-01-02"),
            Nota(3, 2, "Contenido de la nota estática 3", "2024-01-03")
        )

        // Asignar las notas estáticas al adaptador
        notasAdapter.setNotas(notasEstaticas)

        supportActionBar?.hide() // Ocultar la barra de acción predeterminada

        // Inicializar vistas
        editTextProgressCurrent = findViewById(R.id.editTextProgressCurrent)
        editTextProgressTotal = findViewById(R.id.editTextProgressTotal)
        progressBar = findViewById(R.id.progressBar)
        bottomNavigationView = findViewById(R.id.bottomNavigationView)
        imagenPortada = findViewById(R.id.portadaImageView)
        textViewSinopsis = findViewById(R.id.tvSinopsis)
        textViewNumeroNotas = findViewById(R.id.tvNumeroNotas)
        ratingBarValoracion = findViewById(R.id.ratingBar)
        val btnExpandirSinopsis = findViewById<ImageView>(R.id.btnExpandirSinopsis)
        val scrollViewSinopsis = findViewById<NestedScrollView>(R.id.scrollViewSinopsis)
        val detailConstraintLayout = findViewById<ConstraintLayout>(R.id.detailConstraintLayout)
        notaViewModel = ViewModelProvider(
            this,
            NotaViewModel.NotaViewModelFactory((application as InitApplication).notaRepository)
        )[NotaViewModel::class.java]

        var isExpanded = false

        btnExpandirSinopsis.setOnClickListener {
            isExpanded = !isExpanded

            // Iniciar una transición para animar los cambios en el layout
            val transition = AutoTransition()
            transition.duration = 200
            TransitionManager.beginDelayedTransition(detailConstraintLayout, transition)

            if (isExpanded) {
                // Mostrar el NestedScrollView y cambiar icono
                scrollViewSinopsis.visibility = View.VISIBLE
                btnExpandirSinopsis.setImageResource(R.drawable.ic_updesc) // Cambiar al icono "arriba"
            } else {
                // Ocultar el NestedScrollView y restaurar icono
                scrollViewSinopsis.visibility = View.GONE
                btnExpandirSinopsis.setImageResource(R.drawable.ic_downdesc) // Cambiar al icono "abajo"
            }
        }

        // Obtener el ID del libro del Intent
        val libroId = intent.getIntExtra("LIBRO_ID", 0)

        // Cargar el libro desde la base de datos
        cargarDatosLibro(libroId)

        // Configuracion RecyclerView Notas


        // Inicializamos el notaViewModel para las notas estaticas y demas
        notaViewModel.notas.observe(this) { notas ->
            if (!notas.isNullOrEmpty()) {
                Log.d("DetallesLibroActivity", "Actualizando adaptador con notas: $notas")
                notasAdapter.setNotas(notas)
            } else {
                Log.w("DetallesLibroActivity", "Notas observadas están vacías o nulas.")
            }
        }

        notaViewModel.getNotasByLibroId(libroId).observe(this) { notas ->
            if (notas.isNotEmpty()) {
                Log.d("DetallesLibroActivity", "Notas observadas para libro $libroId: $notas")
                notasAdapter.setNotas(notas)
            } else {
                Log.w("DetallesLibroActivity", "No se encontraron notas para libro $libroId.")
            }
        }

        recyclerViewNotas.isNestedScrollingEnabled = false

        recyclerViewNotas.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    // Si el RecyclerView puede desplazarse, deshabilita la intercepción del padre
                    if (recyclerViewNotas.canScrollVertically(-1) || recyclerViewNotas.canScrollVertically(1)) {
                        recyclerViewNotas.parent.requestDisallowInterceptTouchEvent(true)
                    }
                }
                MotionEvent.ACTION_MOVE -> {
                    // Sigue deshabilitando la intercepción si se está desplazando
                    recyclerViewNotas.parent.requestDisallowInterceptTouchEvent(
                        recyclerViewNotas.canScrollVertically(-1) || recyclerViewNotas.canScrollVertically(1)
                    )
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    // Cuando se suelta el gesto, permite que el padre recupere el control
                    recyclerViewNotas.parent.requestDisallowInterceptTouchEvent(false)
                }
            }
            false
        }



        // Inicializar vistas
        editTextProgressCurrent = findViewById(R.id.editTextProgressCurrent)
        editTextProgressTotal = findViewById(R.id.editTextProgressTotal)
        progressBar = findViewById(R.id.progressBar)
        bottomNavigationView = findViewById(R.id.bottomNavigationView)
        imagenPortada = findViewById(R.id.portadaImageView)


        // Configuración del BottomNavigationView
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
                    startActivity(Intent(this, LibroActivity::class.java))
                    true
                }

                else -> false
            }
        }

        // Listeners para actualizar el progreso cuando cambian los valores
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

        // Listener para actualizar la valoración
        ratingBarValoracion.setOnRatingBarChangeListener { _, rating, _ ->
            libro?.let {
                libroViewModel.actualizarValoracion(it.id, rating)
            }
        }
    }

    private fun cargarDatosLibro(libroId: Int) {
        libroViewModel.getLibroById(libroId).observe(this) { libroCargado ->
            if (libroCargado == null) {
                Log.e("DetallesLibroActivity", "Libro no encontrado en la base de datos para ID: $libroId")
                return@observe
            }

            libro = libroCargado
            libro?.let {
                // Rellenar campos con datos del libro
                val resID = resources.getIdentifier(it.nombrePortada, "drawable", packageName)
                imagenPortada.setImageResource(resID)
                editTextProgressCurrent.setText(it.progreso.toString())
                Log.d("DetallesLibroActivity", "Sinopsis cargada: ${it.sinopsis}")
                if (it.totalPaginas > 0) {
                    editTextProgressTotal.setText(it.totalPaginas.toString())
                } else {
                    Log.w("DetallesLibroActivity", "El valor de totalPaginas es 0 o inválido.")
                }

                // Rellenar las nuevas propiedades
                textViewSinopsis.text = it.sinopsis ?: "Sinopsis no disponible"
                textViewNumeroNotas.text = ""
                ratingBarValoracion.rating = it.valoracion

                // Actualizar la barra de progreso
                updateProgressBar()
            }
        }
    }

    private fun guardarProgresoEnBaseDeDatos() {
        val current = editTextProgressCurrent.text.toString().toIntOrNull() ?: 0
        val total = editTextProgressTotal.text.toString().toIntOrNull() ?: 0

        if (total > 0) {
            libro?.let {
                it.progreso = current
                it.totalPaginas = total
                libroViewModel.updateLibro(it)

                Log.d("DetallesLibroActivity", "Progreso actualizado: $current / $total")
            }
        } else {
            Log.w("DetallesLibroActivity", "Total de páginas inválido, no se actualiza el progreso.")
        }
    }

    private fun calcularProgreso(current: Int, total: Int): Int {
        return if (total > 0) (current * 100) / total else 0
    }

    private fun updateProgressBar() {
        val current = editTextProgressCurrent.text.toString().toIntOrNull() ?: 0
        val total =
            editTextProgressTotal.text.toString().toIntOrNull() ?: libro?.totalPaginas ?: 100

        val progresoPorcentaje = calcularProgreso(current, total)
        progressBar.max = 100
        progressBar.progress = progresoPorcentaje.coerceIn(0, 100)
    }

    override fun onNotaClick(nota: Nota) {
        Toast.makeText(this, "Nota seleccionada: ${nota.titulo}", Toast.LENGTH_SHORT).show()

        // Navegar a otra actividad si es necesario
        val intent = Intent(this, EditarNotaActivity::class.java)
        intent.putExtra("NOTA_ID", nota.id)
        startActivity(intent)
    }
}