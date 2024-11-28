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
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.widget.NestedScrollView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.appsandersonsm.Adapter.NotasAdapter
import com.example.appsandersonsm.Modelo.Libro
import com.example.appsandersonsm.ViewModel.LibroViewModel
import com.example.appsandersonsm.ViewModel.LibroViewModelFactory
import com.google.android.material.bottomnavigation.BottomNavigationView

class DetallesLibroActivity : AppCompatActivity() {

    private lateinit var progressBar: ProgressBar
    private lateinit var bottomNavigationView: BottomNavigationView
    private lateinit var imagenPortada: ImageView
    private lateinit var editTextProgressCurrent: EditText
    private lateinit var editTextProgressTotal: EditText
//    private lateinit var dbHelper: DatabaseHelper
    private var libro: Libro? = null
    private val libroViewModel: LibroViewModel by viewModels {
        LibroViewModelFactory((application as InitApplication).libroRepository)
    }


    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        setContentView(R.layout.activity_detalles_libro)

        supportActionBar?.hide() // Hide default topbar with app name

        // Descripcion
        // Referencias a las vistas

        val btnExpandirDescripcion = findViewById<ImageView>(R.id.btnExpandirDescripcion)
        val scrollViewDescripcion = findViewById<NestedScrollView>(R.id.scrollViewDescripcion)
        val tvDescripcion = findViewById<TextView>(R.id.tvDescripcion)
        val detailConstraintLayout = findViewById<ConstraintLayout>(R.id.detailConstraintLayout)
        var isExpanded = false

        btnExpandirDescripcion.setOnClickListener {
            isExpanded = !isExpanded

            // Iniciar una transición para animar los cambios en el layout
            val transition = AutoTransition()
            transition.duration = 200
            TransitionManager.beginDelayedTransition(detailConstraintLayout, transition)

            if (isExpanded) {
                // Mostrar el NestedScrollView y cambiar icono
                scrollViewDescripcion.visibility = View.VISIBLE
                btnExpandirDescripcion.setImageResource(R.drawable.ic_updesc) // Reemplaza con tu icono de 'up'
            } else {
                // Ocultar el NestedScrollView y restaurar icono
                scrollViewDescripcion.visibility = View.GONE
                btnExpandirDescripcion.setImageResource(R.drawable.ic_downdesc) // Reemplaza con tu icono de 'down'
            }
        }


        // Recycler View Notas
        val recyclerViewNotas = findViewById<RecyclerView>(R.id.recyclerViewNotas)
        recyclerViewNotas.layoutManager = LinearLayoutManager(this)
        recyclerViewNotas.adapter = NotasAdapter()

        recyclerViewNotas.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    // Deshabilita el scroll del padre cuando el usuario toca el RecyclerView
                    recyclerViewNotas.parent.requestDisallowInterceptTouchEvent(true)
                }

                MotionEvent.ACTION_UP -> {
                    // Habilita el scroll del padre cuando el usuario deja de tocar el RecyclerView
                    recyclerViewNotas.parent.requestDisallowInterceptTouchEvent(false)
                    // Llama a performClick para cumplir con las reglas de accesibilidad
                    recyclerViewNotas.performClick()
                }
            }
            false // Permitir que el RecyclerView maneje el evento táctil
        }


        // Inicializar vistas
        editTextProgressCurrent = findViewById(R.id.editTextProgressCurrent)
        editTextProgressTotal = findViewById(R.id.editTextProgressTotal)
        progressBar = findViewById(R.id.progressBar)
        bottomNavigationView = findViewById(R.id.bottomNavigationView)
        imagenPortada = findViewById(R.id.portadaImageView)


        // Obtener el ID del libro del Intent
        val libroId = intent.getIntExtra("LIBRO_ID", 0)

        // Cargar el libro desde la base de datos
        cargarDatosLibro(libroId)

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
    }

    private fun cargarDatosLibro(libroId: Int) {
        libroViewModel.getLibroById(libroId).observe(this) { libroCargado ->
            if (libroCargado == null) {
                Log.e("DetallesLibroActivity", "Libro no encontrado en la base de datos para ID: $libroId")
                return@observe
            }

            libro = libroCargado
            libro?.let {
                Log.d("DetallesLibroActivity", "Libro cargado: totalPaginas = ${it.totalPaginas}")
                val resID = resources.getIdentifier(it.nombrePortada, "drawable", packageName)
                imagenPortada.setImageResource(resID)
                editTextProgressCurrent.setText(it.progreso.toString())
                // Solo asigna el valor a editTextProgressTotal si totalPaginas es mayor que 0
                if (it.totalPaginas > 0) {
                    editTextProgressTotal.setText(it.totalPaginas.toString())
                } else {
                    Log.w("DetallesLibroActivity", "El valor de totalPaginas es 0 o inválido.")
                }
                updateProgressBar()
            }
        }
    }

    private fun guardarProgresoEnBaseDeDatos() {
        val current = editTextProgressCurrent.text.toString().toIntOrNull() ?: libro?.progreso ?: 0
        val total = editTextProgressTotal.text.toString().toIntOrNull()

        if (total != null && total > 0) {
            libro?.let {
                it.progreso = current
                it.totalPaginas = total
                libroViewModel.updateLibro(it)

                // Log para depuración
                Log.d(
                    "DetallesLibroActivity",
                    "Progreso guardado: ${it.progreso}, Total páginas guardado: ${it.totalPaginas}"
                )
            }
        } else {
            Log.w("DetallesLibroActivity", "El total de páginas no es válido. No se guardará.")
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



}
