package com.example.appsandersonsm

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.MotionEvent
import android.widget.EditText
import android.widget.ImageView
import android.widget.ProgressBar
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.appsandersonsm.Modelo.Libro
import com.example.appsandersonsm.Repositorio.DatabaseHelper
import com.google.android.material.bottomnavigation.BottomNavigationView

class DetallesLibroActivity : AppCompatActivity() {

    private lateinit var progressBar: ProgressBar
    private lateinit var bottomNavigationView: BottomNavigationView
    private lateinit var imagenPortada: ImageView
    private lateinit var editTextProgressCurrent: EditText
    private lateinit var editTextProgressTotal: EditText
    private lateinit var dbHelper: DatabaseHelper
    private var libro: Libro? = null

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        setContentView(R.layout.activity_detalles_libro)

        supportActionBar?.hide() // Hide default topbar with app name

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

        // Inicializar el DatabaseHelper
        dbHelper = DatabaseHelper(this)

        // Cargar datos iniciales en la base de datos si es la primera vez
        dbHelper.cargarDatosInicialesDesdeJson()

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
        libro = dbHelper.getLibroById(libroId)

        libro?.let {
            val resID = resources.getIdentifier(it.nombrePortada, "drawable", packageName)
            imagenPortada.setImageResource(resID)
            updateProgressBar()
            editTextProgressCurrent.setText(it.progreso.toString())
            editTextProgressTotal.setText(it.totalPaginas.toString())
        }
    }

    private fun calcularProgreso(current: Int, total: Int): Int {
        return if (total > 0) (current * 100) / total else 0
    }

    private fun updateProgressBar() {
        val current = editTextProgressCurrent.text.toString().toIntOrNull() ?: 0
        val total = editTextProgressTotal.text.toString().toIntOrNull() ?: libro?.totalPaginas ?: 100

        val progresoPorcentaje = calcularProgreso(current, total)
        progressBar.max = 100
        progressBar.progress = progresoPorcentaje.coerceIn(0, 100)
    }

    private fun guardarProgresoEnBaseDeDatos() {
        val current = editTextProgressCurrent.text.toString().toIntOrNull() ?: 0
        val total = editTextProgressTotal.text.toString().toIntOrNull() ?: 0

        // Actualizar las propiedades del objeto 'libro'
        libro?.progreso = current
        libro?.totalPaginas = total

        // Guardar el libro actualizado en la base de datos
        libro?.let { dbHelper.actualizarProgresoLibro(it) }

        // Log para depuración
        Log.d(
            "DetallesLibroActivity",
            "Progreso guardado: ${libro?.progreso}, Total páginas guardado: ${libro?.totalPaginas}"
        )
    }
}
