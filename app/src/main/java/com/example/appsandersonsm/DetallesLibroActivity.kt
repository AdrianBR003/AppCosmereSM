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
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.RatingBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
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
import kotlinx.coroutines.launch
import com.airbnb.lottie.LottieAnimationView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.example.appsandersonsm.ViewModel.NotaViewModel.NotaViewModelFactory



/**
 * DetallesLibroActivity muestra los detalles de un libro seleccionado, incluyendo su progreso, sinopsis,
 * valoraciones y notas asociadas. Permite al usuario editar el progreso, agregar nuevas notas y ver detalles
 * adicionales del libro. Además, maneja la sincronización de datos con la nube y la actualización de la interfaz
 * de usuario según los cambios de idioma.
 */
class DetallesLibroActivity : AppCompatActivity(), NotasAdapter.OnNotaClickListener {

    companion object {
        private const val PREFS_NAME = "app_prefs"
        private const val KEY_IS_FIRST_TIME = "isFirstTime"
    }

    private var isReceiverRegistered = false

    private val languageChangeReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == "LANGUAGE_CHANGED") {
                recreate()
            }
        }
    }

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
    private var contadorNotas: Int = 0
    private var userId = ""
    private var idNotaEliminada = -1
    private var isnotaEliminada = false

    /**
     * Inicializa la actividad, configura las vistas, los ViewModels, los listeners y observa los cambios en los datos.
     *
     * @param savedInstanceState El estado previamente guardado de la actividad.
     */
    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("DetallesLibroActivity", "Application context: $applicationContext")
        setContentView(R.layout.activity_detalles_libro)

        userId = intent.getStringExtra("USER_ID") ?: ""
        idNotaEliminada = intent.getIntExtra("notaId", -1)
        isnotaEliminada = intent.getBooleanExtra("IS_NOTA", false)
        idLibro = intent.getIntExtra("LIBRO_ID", -1)
        Log.d("DetallesLibroActivity", "Libro ID: $idLibro")

        supportActionBar?.hide()

        inicializarViewModels()
        inicializarVistas()
        configurarRecyclerView()
        configurarBottomNavigation()
        configurarListeners()
        observarNotas()
        inicializarDatosLibro()

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

        btnExpandirSinopsis.setOnClickListener {
            toggleSinopsisVisibility(nestedScrollViewSinopsis, btnExpandirSinopsis)
        }

        val addNotaImageView = findViewById<ImageView>(R.id.btn_anyadirnotas)
        addNotaImageView.setOnClickListener {
            agregarNuevaNota()
        }

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

        if (isnotaEliminada && idNotaEliminada != -1) {
            Log.d("DetallesLibroActivity", "Preparando para eliminar nota con ID: $idNotaEliminada")
            libro?.let { libroActualizado ->
                lifecycleScope.launch {
                    val notasDelLibro = obtenerNotasDelLibro(libroActualizado.id, userId)
                    libroViewModel.guardarLibroEnLaNube(libroActualizado, notasDelLibro, idNotaEliminada)
                }
            }
        }
    }

    /**
     * Registra el receptor de cambios de idioma cuando la actividad se reanuda.
     */
    override fun onResume() {
        super.onResume()
        LocalBroadcastManager.getInstance(this).registerReceiver(
            languageChangeReceiver,
            IntentFilter("LANGUAGE_CHANGED")
        )
    }

    /**
     * Pausa todas las animaciones y cancela el registro del receptor cuando la actividad está en pausa.
     */
    override fun onPause() {
        super.onPause()
        libro?.let { libroActualizado ->
            val idNotaEliminada = intent.getIntExtra("notaId", -1)

            if (idNotaEliminada != -1) {
                Log.d("DetallesLibroActivity", "Preparando para eliminar nota con ID: $idNotaEliminada y guardar cambios en el libro.")
                lifecycleScope.launch {
                    val notasDelLibro = obtenerNotasDelLibro(libroActualizado.id, userId)
                    val notasActualizadas = notasDelLibro.filter { it.id != idNotaEliminada }
                    libroViewModel.guardarLibroEnLaNube(libroActualizado, notasActualizadas, idNotaEliminada)
                }
            } else {
                Log.d("DetallesLibroActivity", "Guardando cambios en el libro sin eliminar ninguna nota.")
                lifecycleScope.launch {
                    val notasDelLibro = obtenerNotasDelLibro(libroActualizado.id, userId)
                    libroViewModel.guardarLibroEnLaNube(libroActualizado, notasDelLibro, -1)
                }
            }
        } ?: Log.e("DetallesLibroActivity", "El libro es nulo al intentar guardar en la nube.")
        LocalBroadcastManager.getInstance(this).unregisterReceiver(languageChangeReceiver)
    }

    /**
     * Inicializa los ViewModels para notas y libros utilizando las factories correspondientes.
     */
    private fun inicializarViewModels() {
        val libroViewModelFactory = LibroViewModel.LibroViewModelFactory(app.libroRepository)
        libroViewModel = ViewModelProvider(
            this,
            libroViewModelFactory
        ).get(LibroViewModel::class.java)

        val notaViewModelFactory = NotaViewModelFactory(app.notaRepository)
        notaViewModel = ViewModelProvider(
            this,
            notaViewModelFactory
        ).get(NotaViewModel::class.java)
    }

    /**
     * Inicializa las vistas de la interfaz de usuario.
     */
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

    /**
     * Configura el RecyclerView para mostrar las notas asociadas al libro.
     */
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

    /**
     * Configura la barra de navegación inferior para permitir la navegación entre actividades.
     */
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

    /**
     * Alterna la visibilidad de la sinopsis con una animación.
     *
     * @param llNetScroll La vista NestedScrollView de la sinopsis.
     * @param btnExpandirSinopsis El botón para expandir o contraer la sinopsis.
     */
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

    /**
     * Configura los listeners para los elementos interactivos de la interfaz de usuario.
     */
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

    /**
     * Inicializa los datos del libro seleccionado y actualiza la interfaz de usuario con estos datos.
     */
    private fun inicializarDatosLibro() {
        contarNotas(idLibro) { numeroNotas ->
            Log.d(
                "DetallesLibroActivity",
                "Número de notas para libro $idLibro: $numeroNotas"
            )

            cargarDatosLibro(idLibro) { libroCargado ->
                libro = libroCargado
                libro?.numeroNotas = numeroNotas
                actualizarUIConDatosLibro(libroCargado)
            }
        }
    }

    /**
     * Cuenta el número de notas asociadas a un libro específico.
     *
     * @param libroId El ID del libro.
     * @param callback La función a llamar con el número de notas.
     */
    private fun contarNotas(libroId: Int, callback: (Int) -> Unit) {
        notaViewModel.contarNotasPorLibro(libroId, userId).observe(this) { numeroNotas ->
            callback(numeroNotas ?: 0)
        }
    }

    /**
     * Carga los datos de un libro específico desde el ViewModel.
     *
     * @param libroId El ID del libro.
     * @param callback La función a llamar con el libro cargado.
     */
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

    /**
     * Actualiza la interfaz de usuario con los datos del libro cargado.
     *
     * @param libro El libro cargado.
     */
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

    /**
     * Guarda el progreso actual del libro en la base de datos y actualiza la interfaz de usuario.
     */
    private fun guardarProgresoEnBaseDeDatos() {
        val current = editTextProgressCurrent.text.toString().toIntOrNull() ?: 0
        val total = editTextProgressTotal.text.toString().toIntOrNull() ?: 0

        if (total > 0) {
            libro?.let { libroActualizado ->
                libroActualizado.progreso = current
                libroActualizado.totalPaginas = total
                contarNotas(idLibro) { numeroNotas ->
                    libroActualizado.numeroNotas = numeroNotas
                    Log.d("DetallesLibroActivity", "Progreso actualizado: $current / $total")
                    if (current == total && !libroActualizado.leido) {
                        mostrarPopupCelebracion()
                        libroActualizado.leido = true
                    }
                    libroViewModel.updateLibro(libroActualizado)
                }
            }
        } else {
            Log.w(
                "DetallesLibroActivity",
                "Total de páginas inválido, no se actualiza el progreso."
            )
        }
    }

    /**
     * Actualiza la ProgressBar con el porcentaje de progreso calculado.
     */
    private fun updateProgressBar() {
        val currentInput = editTextProgressCurrent.text.toString().toIntOrNull() ?: 0
        val totalInput = editTextProgressTotal.text.toString().toIntOrNull() ?: libro?.totalPaginas ?: 100

        if (totalInput <= 0) {
            editTextProgressTotal.error = "El total debe ser mayor que cero."
            progressBar.progress = 0
            return
        }

        val current = if (currentInput > totalInput) {
            editTextProgressCurrent.error = getString(R.string.error_progress)
            totalInput
        } else {
            editTextProgressCurrent.error = null
            currentInput
        }

        val progressPercent = calcularProgreso(current, totalInput).coerceIn(0, 100)

        progressBar.max = 100
        progressBar.progress = progressPercent
    }

    /**
     * Calcula el porcentaje de progreso basado en el progreso actual y el total.
     *
     * @param current El progreso actual.
     * @param total El progreso total.
     * @return El porcentaje de progreso.
     */
    private fun calcularProgreso(current: Int, total: Int): Int {
        return if (total > 0) (current * 100) / total else 0
    }

    /**
     * Maneja el clic en una nota, abriendo la actividad de edición de la nota seleccionada.
     *
     * @param nota La nota seleccionada.
     */
    override fun onNotaClick(nota: Nota) {
        Toast.makeText(this, "Nota seleccionada: ${nota.titulo}", Toast.LENGTH_SHORT).show()
        userId = intent.getStringExtra("USER_ID") ?: ""
        val intent = Intent(this, EditarNotaActivity::class.java)
        intent.putExtra("NOTA_ID", nota.id)
        intent.putExtra("USER_ID", userId)
        intent.putExtra("LIBRO_ID", idLibro)
        startActivity(intent)
    }

    /**
     * Observa los cambios en las notas asociadas al libro y actualiza el adaptador del RecyclerView.
     */
    private fun observarNotas() {
        notaViewModel.getNotasByLibroId(idLibro, userId = userId).observe(this) { notas ->
            Log.d(
                "DetallesLibroActivity",
                "Notas cargadas desde ViewModel: $notas"
            )
            notasAdapter.submitList(notas)
        }
    }

    /**
     * Agrega una nueva nota al libro y actualiza la interfaz de usuario.
     */
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

    /**
     * Obtiene todas las notas asociadas a un libro de forma asíncrona.
     *
     * @param idNotaL El ID del libro.
     * @param userId El ID del usuario.
     * @return La lista de notas asociadas al libro.
     */
    private suspend fun obtenerNotasDelLibro(idNotaL: Int, userId: String): List<Nota> {
        return withContext(Dispatchers.IO) {
            libroViewModel.obtenerNotasDelLibro(idNotaL, userId)
        }
    }

    /**
     * Muestra un popup de celebración cuando se completa un libro.
     */
    private fun mostrarPopupCelebracion() {
        val inflater = layoutInflater
        val view = inflater.inflate(R.layout.custom_popup, null)
        val buttonClose = view.findViewById<Button>(R.id.buttonClose)

        val textViewMessage = view.findViewById<TextView>(R.id.tv_libro)
        val fraseViewMessage = view.findViewById<TextView>(R.id.tv_frase)
        val lottieConfetti = view.findViewById<LottieAnimationView>(R.id.lottieConfetti)

        val tituloLibro = getTituloLibro(libro?.nombrePortada ?: "portada_elcamino")
        textViewMessage.text = tituloLibro

        val fraseResId = when (libro?.nombrePortada) {
            "portada_elcamino" -> R.string.frase_elcamino
            "portada_palabrasradiantes" -> R.string.frase_palabrasradiantes
            "portada_juramentada" -> R.string.frase_juramentada
            "portada_elritmoguerra" -> R.string.frase_elritmoguerra
            "portada_elaliento" -> R.string.frase_elaliento
            "portada_nacidos" -> R.string.frase_nacidos
            "portada_elheroe" -> R.string.frase_elheroe
            "portada_elpozo" -> R.string.frase_elpozo
            else -> R.string.frase_elcamino
        }

        fraseViewMessage.setText(fraseResId)

        val dialog = AlertDialog.Builder(this)
            .setView(view)
            .create()

        buttonClose.setOnClickListener {
            dialog.dismiss()
        }

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.show()

        dialog.window?.attributes?.windowAnimations = R.style.PopupAnimation

        dialog.window?.setLayout(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )

        lottieConfetti.visibility = View.VISIBLE
        lottieConfetti.playAnimation()
    }

    /**
     * Obtiene el título del libro basado en el nombre de la portada.
     *
     * @param nombrePortada El nombre de la portada del libro.
     * @return El título del libro.
     */
    fun Context.getTituloLibro(nombrePortada: String): String {
        return when (nombrePortada) {
            "portada_elimperiofinal" -> getString(R.string.titulo_el_imperio_final)
            "portada_elpozo" -> getString(R.string.titulo_el_pozo_de_la_ascension)
            "portada_elheroe" -> getString(R.string.titulo_el_heroe_de_las_eras)
            "portada_elaliento" -> getString(R.string.titulo_el_aliento_de_los_dioses)
            "portada_elcamino" -> getString(R.string.titulo_el_camino_de_los_reyes)
            "portada_palabrasradiantes" -> getString(R.string.titulo_palabras_radiantes)
            "portada_juramentada" -> getString(R.string.titulo_juramentada)
            "portada_elritmoguerra" -> getString(R.string.titulo_el_ritmo_de_la_guerra)
            else -> getString(R.string.titulo_el_camino_de_los_reyes)
        }
    }
}
