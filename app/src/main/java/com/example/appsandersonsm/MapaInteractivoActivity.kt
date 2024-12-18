package com.example.appsandersonsm

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PointF
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.view.ViewTreeObserver
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.example.appsandersonsm.Adapter.TutorialDialogFragment
import com.example.appsandersonsm.Dao.LibroDao
import com.example.appsandersonsm.Dao.NotaDao
import com.example.appsandersonsm.DataBase.AppDatabase
import com.example.appsandersonsm.Modelo.Libro
import com.example.appsandersonsm.ViewModel.LibroViewModel
import com.google.android.material.bottomnavigation.BottomNavigationView
import io.getstream.photoview.PhotoView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlin.math.roundToInt

/**
 * MapaInteractivoActivity muestra un mapa interactivo con marcadores para cada libro del usuario,
 * maneja la navegación entre actividades, y gestiona la visualización de leyendas y tutoriales.
 */
class MapaInteractivoActivity : AppCompatActivity() {

    companion object {
        private const val PREFS_NAME = "app_prefs"
        private const val KEY_IS_FIRST_TIME = "isFirstTime"
    }

    private var isReceiverRegistered = false

    private val languageChangeReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == "LANGUAGE_CHANGED") {
                isReceiverRegistered = true
                recreate()
            }
        }
    }

    private lateinit var photoView: PhotoView
    private lateinit var markerContainer: FrameLayout
    private lateinit var bottomNavigationView: BottomNavigationView
    private var listaLibros: List<Libro> = emptyList()
    private val markers = mutableListOf<ImageView>()
    private lateinit var arrowOverlayView: ArrowOverlayView
    private lateinit var libroDao: LibroDao
    private lateinit var notaDao: NotaDao
    private var isLeyendaVisible = false
    private var userId = ""
    var photoViewReady = false
    var listaLibrosReady = false

    private val animators = mutableListOf<ValueAnimator>()

    private val libroCoordenadasNormalizadas = mapOf(
        1 to PointF(0.38f, 0.80f),
        2 to PointF(0.45f, 0.86f),
        3 to PointF(0.55f, 0.88f),
        4 to PointF(0.64f, 0.85f),
        5 to PointF(0.06f, 0.76f),
        6 to PointF(0.105f, 0.63f),
        7 to PointF(0.07f, 0.50f),
        8 to PointF(0.12f, 0.40f)
    )

    private val relacionesCronologicas = listOf(
        Pair(1, 2),
        Pair(2, 3),
        Pair(3, 4),
        Pair(5, 1),
        Pair(8, 7),
        Pair(7, 6),
        Pair(6, 5)
    )

    private val libroViewModel: LibroViewModel by viewModels {
        LibroViewModel.LibroViewModelFactory((application as InitApplication).libroRepository)
    }

    private val applicationScope = CoroutineScope(SupervisorJob())

    /**
     * Inicializa la actividad, configura las vistas, maneja la primera vez del usuario,
     * y configura los marcadores en el mapa.
     *
     * @param savedInstanceState El estado previamente guardado de la actividad.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_mapa_interactivo)
        supportActionBar?.hide()

        if (isFirstTimeUser()) {
            showTutorial()
            setFirstTimeUser(false)
        }

        val database = AppDatabase.getDatabase(applicationContext, applicationScope)
        libroDao = database.libroDao()
        notaDao = database.notaDao()

        userId = intent.getStringExtra("USER_ID") ?: ""

        if (userId.isEmpty()) {
            Log.e("MapaInteractivo", "userId está vacío. Finalizando actividad.")
            Toast.makeText(this, "Error: ID de usuario no proporcionado.", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        Log.d("MapaInteractivo", "Observando libros del usuario: $userId")
        libroViewModel.getAllLibrosByUsuario(userId).observe(this, Observer { libros ->
            if (libros.isNullOrEmpty()) {
                Log.e("MapaInteractivo", "No se encontraron libros.")
            } else {
                Log.d("MapaInteractivo", "Libros cargados: ${libros.size}")
                listaLibros = libros
                listaLibrosReady = true
                Log.d("MapaInteractivo", "Actualizando Inicial Saga")
            }
        })

        photoView = findViewById(R.id.photoView)
        markerContainer = findViewById(R.id.mapContainerMarkers)
        bottomNavigationView = findViewById(R.id.bottomNavigationView)
        arrowOverlayView = findViewById(R.id.arrowOverlayView)

        photoView.minimumScale = 1.0f
        photoView.mediumScale = 1.5f
        photoView.maximumScale = 3.0f

        photoView.viewTreeObserver.addOnGlobalLayoutListener(object :
            ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                if (listaLibros.isNotEmpty()) {
                    photoViewReady = true
                    photoView.viewTreeObserver.removeOnGlobalLayoutListener(this)
                    intentarInicializarMarcadores()
                } else {
                    Log.d("MapaInteractivo", "PhotoView listo, pero listaLibros aún no está inicializada.")
                }
            }
        })

        photoView.setOnMatrixChangeListener { actualizarMarcadoresYFlechas() }

        bottomNavigationView.selectedItemId = R.id.nav_map

        bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_settings -> {
                    val intent = Intent(this, AjustesActivity::class.java)
                    intent.putExtra("USER_ID", userId)
                    Log.d("Id", "MapaInteractivo a nav_settings=${userId}")
                    startActivity(intent)
                    true
                }

                R.id.nav_map -> {
                    true
                }

                R.id.nav_book -> {
                    val intent = Intent(this, LibroActivity::class.java)
                    intent.putExtra("USER_ID", userId)
                    Log.d("Id", "MapaInteractivo a nav_book=${userId}")
                    startActivity(intent)
                    true
                }

                else -> false
            }
        }

        arrowOverlayView.relaciones = relacionesCronologicas

        val followButton: ImageButton = findViewById(R.id.followButton)
        val leyendaView: View = findViewById(R.id.leyenda)
        val closeButton: ImageButton = findViewById(R.id.closeButton)

        leyendaView.visibility = View.INVISIBLE
        leyendaView.post {
            leyendaView.translationY = -leyendaView.height.toFloat()
            leyendaView.visibility = View.GONE
        }

        followButton.setOnClickListener {
            leyendaView.post {
                if (!isLeyendaVisible) {
                    leyendaView.visibility = View.VISIBLE
                    followButton.isEnabled = false

                    val animator = ObjectAnimator.ofFloat(
                        leyendaView,
                        "translationY",
                        -leyendaView.height.toFloat(),
                        0f
                    )
                    animator.duration = 300
                    animator.interpolator = AccelerateDecelerateInterpolator()
                    animator.start()

                    isLeyendaVisible = true
                }
            }
        }

        closeButton.setOnClickListener {
            if (isLeyendaVisible) {
                val animator = ObjectAnimator.ofFloat(
                    leyendaView,
                    "translationY",
                    0f,
                    -leyendaView.height.toFloat()
                )
                animator.duration = 300
                animator.interpolator = AccelerateDecelerateInterpolator()
                animator.start()

                animator.addListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        leyendaView.visibility = View.GONE
                        followButton.isEnabled = true
                        leyendaView.translationY = 0f
                    }
                })

                isLeyendaVisible = false
            }
        }
    }

    /**
     * Pausa todas las animaciones cuando la actividad está en pausa.
     */
    override fun onPause() {
        super.onPause()
        animators.forEach { it.cancel() }
    }

    /**
     * Reanuda todas las animaciones cuando la actividad se reanuda.
     */
    override fun onResume() {
        super.onResume()
        animators.forEach { it.start() }
    }

    /**
     * Limpia los receptores registrados cuando la actividad se destruye.
     */
    override fun onDestroy() {
        super.onDestroy()
        if (isReceiverRegistered) {
            unregisterReceiver(languageChangeReceiver)
            isReceiverRegistered = false
        }
    }

    /**
     * Verifica si es la primera vez que el usuario accede a esta actividad.
     *
     * @return `true` si es la primera vez, `false` en caso contrario.
     */
    private fun isFirstTimeUser(): Boolean {
        val sharedPref = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return sharedPref.getBoolean(KEY_IS_FIRST_TIME, true)
    }

    /**
     * Actualiza el estado de si es la primera vez que el usuario accede.
     *
     * @param isFirstTime `false` después de que el usuario haya visto el tutorial.
     */
    private fun setFirstTimeUser(isFirstTime: Boolean) {
        val sharedPref = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        with(sharedPref.edit()) {
            putBoolean(KEY_IS_FIRST_TIME, isFirstTime)
            apply()
        }
    }

    /**
     * Intenta inicializar los marcadores si las vistas están listas.
     */
    private fun intentarInicializarMarcadores() {
        if (photoViewReady && listaLibrosReady) {
            Log.d("MapaInteractivo", "Intentando inicializar marcadores.")
            Log.d("MapaInteractivo", "User Id intentarInicializarmArcadores: ${userId}")
            inicializarMarcadores()
        }
    }

    /**
     * Inicializa los marcadores en el mapa basándose en la lista de libros.
     */
    private fun inicializarMarcadores() {
        if (listaLibros.isEmpty()) {
            Log.e("MapaInteractivo", "Intentando inicializar marcadores sin datos.")
            return
        }
        markerContainer.removeAllViews()
        markers.clear()
        animators.clear()

        listaLibros.forEach { libro ->
            var sizeInDpID = 60
            var red = 255
            var green = 255
            var blue = 255

            if (libro.inicialSaga) {
                sizeInDpID = 90
                red = 214
                green = 168
                blue = 0
            }

            if (libro.nombreSaga.equals("Libro Independiente")) {
                red = 106
                green = 26
                blue = 128
            }

            if (libro.empezarLeer || libro.nombreLibro.equals("Nacidos de la Bruma (El imperio final)")) {
                red = 26
                green = 128
                blue = 58
            }

            val marker = ImageView(this).apply {
                val scale = resources.displayMetrics.density
                val sizeInPx = (sizeInDpID * scale + 0.5f).roundToInt()
                layoutParams = FrameLayout.LayoutParams(sizeInPx, sizeInPx)

                val backgroundDrawable = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(Color.TRANSPARENT)
                    val strokeWidth = (4 * scale + 0.5f).roundToInt()
                    val strokeColor = Color.BLACK
                    setStroke(strokeWidth, strokeColor)
                }
                background = backgroundDrawable
                val padding = (4 * scale + 0.5f).roundToInt()
                setPadding(padding, padding, padding, padding)
                scaleType = ImageView.ScaleType.CENTER_CROP
                contentDescription = "Libro ${libro.id}"

                val resID = resources.getIdentifier(libro.nombrePortada, "drawable", packageName)
                Glide.with(this@MapaInteractivoActivity)
                    .load(resID)
                    .apply(RequestOptions.circleCropTransform())
                    .into(this)

                setOnClickListener {
                    abrirDetallesLibro(libro.id)
                }

                val animator = ValueAnimator.ofInt(255, 50, 255).apply {
                    duration = 2500L
                    repeatMode = ValueAnimator.RESTART
                    repeatCount = ValueAnimator.INFINITE
                    addUpdateListener { animation ->
                        val alphaValue = animation.animatedValue as Int
                        val strokeColorWithAlpha = Color.argb(alphaValue, red, green, blue)
                        val strokeWidth = (2 * scale + 0.5f).roundToInt()
                        backgroundDrawable.setStroke(strokeWidth, strokeColorWithAlpha)
                        backgroundDrawable.invalidateSelf()
                        invalidate()
                    }
                    start()
                }
                animators.add(animator)
            }

            markers.add(marker)
            markerContainer.addView(marker)
        }

        Handler(Looper.getMainLooper()).postDelayed({
            actualizarMarcadoresYFlechas()
        }, 100)
    }

    /**
     * Actualiza las posiciones de los marcadores y las flechas en el mapa.
     */
    private fun actualizarMarcadoresYFlechas() {
        val matrix = FloatArray(9)
        photoView.imageMatrix.getValues(matrix)

        val scaleX = matrix[android.graphics.Matrix.MSCALE_X]
        val scaleY = matrix[android.graphics.Matrix.MSCALE_Y]
        val transX = matrix[android.graphics.Matrix.MTRANS_X]
        val transY = matrix[android.graphics.Matrix.MTRANS_Y]

        val imageWidth = photoView.drawable.intrinsicWidth.toFloat()
        val imageHeight = photoView.drawable.intrinsicHeight.toFloat()

        val scaledWidth = imageWidth * scaleX
        val scaledHeight = imageHeight * scaleY

        val coordenadasPantalla = mutableMapOf<Int, Pair<Float, Float>>()

        markers.forEachIndexed { index, marker ->
            if (index >= listaLibros.size) return@forEachIndexed
            val libro = listaLibros[index]
            val coordNormalizada = libroCoordenadasNormalizadas[libro.id] ?: PointF(0.5f, 0.5f)
            val absX = (coordNormalizada.x * scaledWidth) + transX
            val absY = (coordNormalizada.y * scaledHeight) + transY

            marker.x = absX - (marker.width / 2)
            marker.y = absY - (marker.height / 2)

            val centerX = marker.x + (marker.width / 2)
            val centerY = marker.y + (marker.height / 2)

            coordenadasPantalla[libro.id] = Pair(centerX, centerY)
        }

        arrowOverlayView.libroCoordenadasPantalla = coordenadasPantalla
        arrowOverlayView.invalidate()
    }

    /**
     * Abre la actividad de detalles del libro seleccionado.
     *
     * @param libroId El ID del libro a mostrar.
     */
    private fun abrirDetallesLibro(libroId: Int) {
        val intent = Intent(this, DetallesLibroActivity::class.java)
        intent.putExtra("LIBRO_ID", libroId)
        intent.putExtra("USER_ID", userId)
        Log.d("MapaInteractivo", "Extras: ${intent?.extras}")
        startActivity(intent)
    }

    /**
     * Muestra el tutorial para la primera vez que el usuario accede a la actividad.
     */
    private fun showTutorial() {
        val tutorialDialog = TutorialDialogFragment()
        tutorialDialog.show(supportFragmentManager, "tutorial_dialog")
    }
}
