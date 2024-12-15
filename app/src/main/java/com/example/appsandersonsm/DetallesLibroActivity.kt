package com.example.appsandersonsm

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.RatingBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.NestedScrollView
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.appsandersonsm.Adapter.NotasAdapter
import com.example.appsandersonsm.Modelo.Libro
import com.example.appsandersonsm.Modelo.Nota
import com.example.appsandersonsm.ViewModel.LibroViewModel
import com.example.appsandersonsm.ViewModel.NotaViewModel
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.launch
import androidx.lifecycle.viewModelScope
import androidx.room.Room
import com.example.appsandersonsm.Dao.LibroDao
import com.example.appsandersonsm.DataBase.AppDatabase
import com.example.appsandersonsm.Repository.LibroRepository
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import com.example.appsandersonsm.ViewModel.NotaViewModel.NotaViewModelFactory
import com.google.firebase.firestore.FirebaseFirestore


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
    private lateinit var btnExpandirSinopsis: ImageView
    private lateinit var llNetScroll: LinearLayout
    private lateinit var nestedScrollViewSinopsis: NestedScrollView

    private val app: InitApplication by lazy {
        application as? InitApplication
            ?: throw IllegalStateException("InitApplication no está configurada correctamente")
    }

    private var libro: Libro? = null
    private var idLibro: Int = 0
    private var isExpanded = false
    private var contadorNotas: Int = 3
    private var userId = ""
    private val firestore = FirebaseFirestore.getInstance()

    private val languageChangeReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == "LANGUAGE_CHANGED") {
                recreate()
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("DetallesLibroActivity", "Application context: $applicationContext")
        setContentView(R.layout.activity_detalles_libro)

        // Coger el ID del Intent del Login
        userId = intent.getStringExtra("USER_ID") ?: ""

        supportActionBar?.hide()

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

        // Observar el estado de guardado
        libroViewModel.guardarEstado.observe(this, Observer { exito ->
            if (exito) {
                Toast.makeText(
                    this,
                    "Libro y notas guardados exitosamente en la nube.",
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                Toast.makeText(
                    this,
                    "Error al guardar libro y notas en la nube.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        })

        nestedScrollViewSinopsis = findViewById(R.id.scrollViewSinopsis)
        btnExpandirSinopsis = findViewById(R.id.btnExpandirSinopsis)
        nestedScrollViewSinopsis.visibility = View.GONE

        // Configurar el botón de expandir
        btnExpandirSinopsis.setOnClickListener {
            toggleSinopsisVisibility(nestedScrollViewSinopsis, btnExpandirSinopsis)
        }

        // Añadir la nota
        val addNotaImageView = findViewById<ImageView>(R.id.btn_anyadirnotas)
        addNotaImageView.setOnClickListener {
            agregarNuevaNota()
        }

        // Configurar el comportamiento táctil del NestedScrollView
        nestedScrollViewSinopsis.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                    nestedScrollViewSinopsis.parent.requestDisallowInterceptTouchEvent(
                        nestedScrollViewSinopsis.canScrollVertically(-1) || nestedScrollViewSinopsis.canScrollVertically(1)
                    )
                }

                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    nestedScrollViewSinopsis.parent.requestDisallowInterceptTouchEvent(false)
                }
            }
            false
        }
    }

    override fun onResume() {
        super.onResume()
        LocalBroadcastManager.getInstance(this).registerReceiver(
            languageChangeReceiver,
            IntentFilter("LANGUAGE_CHANGED")
        )
    }

    override fun onPause() {
        super.onPause()
        LocalBroadcastManager.getInstance(this).unregisterReceiver(languageChangeReceiver)
    }

    override fun onStop() {
        super.onStop()
        libro?.let { libroActualizado ->
            Log.d(
                "DetallesLibroActivity",
                "Guardando en nube el libro: ${libroActualizado.id} con progreso=${libroActualizado.progreso}, totalPaginas=${libroActualizado.totalPaginas}, userId=$userId"
            )

            lifecycleScope.launch {
                val notasDelLibro = obtenerNotasDelLibro(libroActualizado.id, userId)
                libroViewModel.guardarLibroEnLaNube(libroActualizado, notasDelLibro)
            }
        } ?: Log.e("DetallesLibroActivity", "El libro es nulo al intentar guardar en la nube.")
    }

    private fun inicializarViewModels() {
        // Crear una instancia de la Factory desde LibroViewModel
        val libroViewModelFactory = LibroViewModel.LibroViewModelFactory(app.libroRepository)
        libroViewModel = ViewModelProvider(
            this,
            libroViewModelFactory
        ).get(LibroViewModel::class.java)

        // Supongo que tienes una factory similar para NotaViewModel
        val notaViewModelFactory = NotaViewModelFactory(app.notaRepository)
        notaViewModel = ViewModelProvider(
            this,
            notaViewModelFactory
        ).get(NotaViewModel::class.java)
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
                R.id.nav_settings -> {
                    val intent = Intent(this, AjustesActivity::class.java)
                    intent.putExtra("USER_ID", userId)
                    startActivity(intent)
                    true
                }

                R.id.nav_map -> {
                    val intent = Intent(this, MapaInteractivoActivity::class.java)
                    intent.putExtra("USER_ID", userId)
                    startActivity(intent)
                    true
                }

                R.id.nav_book -> {
                    val intent = Intent(this, LibroActivity::class.java)
                    intent.putExtra("USER_ID", userId)
                    startActivity(intent)
                    true
                }

                else -> false
            }
        }
    }

    fun toggleSinopsisVisibility(llNetScroll: NestedScrollView, btnExpandirSinopsis: ImageView) {
        val duration = 300L
        val interpolator = AccelerateDecelerateInterpolator()

        if (isExpanded) {
            llNetScroll.animate()
                .translationY(-llNetScroll.height.toFloat())
                .alpha(0f)
                .setInterpolator(interpolator)
                .setDuration(duration)
                .withEndAction {
                    llNetScroll.visibility = View.GONE
                    llNetScroll.translationY = 0f
                    llNetScroll.alpha = 1f
                    btnExpandirSinopsis.setImageResource(R.drawable.ic_updesc)
                }
                .start()
        } else {
            llNetScroll.visibility = View.VISIBLE
            llNetScroll.alpha = 0f
            llNetScroll.translationY = -llNetScroll.height.toFloat()

            llNetScroll.post {
                llNetScroll.animate()
                    .translationY(0f)
                    .alpha(1f)
                    .setInterpolator(interpolator)
                    .setDuration(duration)
                    .withEndAction {
                        btnExpandirSinopsis.setImageResource(R.drawable.ic_downdesc)
                    }
                    .start()
            }
        }

        isExpanded = !isExpanded
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
            libro?.let { libroViewModel.actualizarValoracion(it.id, rating, userId) }
        }
    }

    private fun inicializarDatosLibro() {
        contarNotas(idLibro) { numeroNotas ->
            Log.d("DetallesLibroActivity", "Número de notas para libro $idLibro: $numeroNotas")

            cargarDatosLibro(idLibro) { libroCargado ->
                libro = libroCargado
                libro?.numeroNotas = numeroNotas
                actualizarUIConDatosLibro(libroCargado)
            }
        }
    }

    private fun contarNotas(libroId: Int, callback: (Int) -> Unit) {
        notaViewModel.contarNotasPorLibro(libroId, userId).observe(this) { numeroNotas ->
            callback(numeroNotas ?: 0)
        }
    }

    private fun cargarDatosLibro(libroId: Int, callback: (Libro?) -> Unit) {
        libroViewModel.getLibroByIdAndUsuario(libroId, userId).observe(this) { libroCargado ->
            if (libroCargado == null) {
                Log.e(
                    "DetallesLibroActivity",
                    "Libro no encontrado en la base de datos para ID: $libroId"
                )
            }
            callback(libroCargado)
        }
    }

    private fun actualizarUIConDatosLibro(libro: Libro?) {
        libro?.let {
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
            Log.w(
                "DetallesLibroActivity",
                "Total de páginas inválido, no se actualiza el progreso."
            )
        }
    }

    private fun updateProgressBar() {
        val current = editTextProgressCurrent.text.toString().toIntOrNull() ?: 0
        val total =
            editTextProgressTotal.text.toString().toIntOrNull() ?: libro?.totalPaginas ?: 100
        progressBar.max = 100
        progressBar.progress = calcularProgreso(current, total).coerceIn(0, 100)
    }

    private fun calcularProgreso(current: Int, total: Int): Int {
        return if (total > 0) (current * 100) / total else 0
    }

    override fun onNotaClick(nota: Nota) {
        Toast.makeText(this, "Nota seleccionada: ${nota.titulo}", Toast.LENGTH_SHORT).show()
        userId = intent.getStringExtra("USER_ID") ?: ""
        val intent = Intent(this, EditarNotaActivity::class.java)
        intent.putExtra("NOTA_ID", nota.id)
        intent.putExtra("USER_ID", userId)
        startActivity(intent)
    }

    private fun observarNotas() {
        notaViewModel.getNotasByLibroId(idLibro, userId = userId).observe(this) { notas ->
            Log.d(
                "DetallesLibroActivity",
                "Notas cargadas desde ViewModel: $notas"
            )
            notasAdapter.submitList(notas)
        }
    }

    private fun agregarNuevaNota() {
        contadorNotas++

        val nuevaNota = Nota(
            idLibroN = idLibro,
            userId = userId,
            titulo = "Nota $contadorNotas",
            contenido = "Contenido de la nota $contadorNotas"
        )

        notaViewModel.insertarNota(nuevaNota)

        Toast.makeText(this, "Nota $contadorNotas añadida", Toast.LENGTH_SHORT).show()
    }

    private suspend fun obtenerNotasDelLibro(idNotaL: Int, userId: String): List<Nota> {
        return withContext(Dispatchers.IO) {
            libroViewModel.obtenerNotasDelLibro(idNotaL, userId)
        }
    }
}
