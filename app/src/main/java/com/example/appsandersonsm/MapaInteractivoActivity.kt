package com.example.appsandersonsm

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.graphics.PointF
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.view.ViewTreeObserver
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.example.appsandersonsm.Dao.LibroDao
import com.example.appsandersonsm.Dao.NotaDao
import com.example.appsandersonsm.Dao.UsuarioDao
import com.example.appsandersonsm.Locale.LocaleHelper
import com.example.appsandersonsm.Modelo.Libro
import com.example.appsandersonsm.ViewModel.LibroViewModel
import com.example.appsandersonsm.ViewModel.LibroViewModelFactory
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.gson.Gson
import io.getstream.photoview.PhotoView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.IOException
import java.util.Locale
import kotlin.math.roundToInt

class MapaInteractivoActivity : AppCompatActivity() {

    private val languageChangeReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == "LANGUAGE_CHANGED") {
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
    private lateinit var usuarioDao: UsuarioDao
    private var isLeyendaVisible = false
    private var userId = ""
    // Lista para guardar las animaciones y poder gestionarlas
    private val animators = mutableListOf<ValueAnimator>()

    // Coordenadas normalizadas asociadas por ID de libro
    private val libroCoordenadasNormalizadas = mapOf(
        1 to PointF(0.38f, 0.80f),  // ID 1
        2 to PointF(0.45f, 0.86f),  // ID 2
        3 to PointF(0.55f, 0.88f),  // ID 3
        4 to PointF(0.64f, 0.85f),  // ID 4
        5 to PointF(0.06f, 0.76f),  // Aliento
        6 to PointF(0.105f, 0.63f), // Héroe
        7 to PointF(0.07f, 0.50f),  // Pozo
        8 to PointF(0.12f, 0.40f)   // Nacidos
    )

    // Definir relaciones cronológicas entre libros
    private val relacionesCronologicas = listOf(
        Pair(1, 2),
        Pair(2, 3),
        Pair(3, 4),
        Pair(5, 1),
        Pair(8, 7),
        Pair(7, 6),
        Pair(6, 5)
        // Agrega más relaciones según tus necesidades
    )

    // Inicializar el ViewModel usando el delegado by viewModels
    private val libroViewModel: LibroViewModel by viewModels {
        LibroViewModelFactory((application as InitApplication).libroRepository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_mapa_interactivo)
        supportActionBar?.hide() // Ocultar la barra de acción predeterminada

        // Obtener el ID del usuario desde el Intent
        userId = intent.getStringExtra("USER_ID") ?: ""

        if (userId.isEmpty()) {
            Log.e("MapaInteractivo", "userId está vacío. Finalizando actividad.")
            Toast.makeText(this, "Error: ID de usuario no proporcionado.", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Antes de observar los libros, cargar el JSON adecuado
        val idiomaActual = Locale.getDefault().language // Devuelve "es", "en", etc.
        val nombreArchivoJson = when (idiomaActual) {
            "en" -> "libros_en.json"
            else -> "libros.json" // Por defecto, español
        }

        cargarLibrosSiEsNecesario(nombreArchivoJson)

        // Inicializa el ViewModel y observa los datos
        libroViewModel.getAllLibrosByUsuario(userId).observe(this, Observer { libros ->
            if (libros != null) {
                listaLibros = libros
                inicializarMarcadores()
            } else {
                Log.e("MapaInteractivo", "No se encontraron libros en el ViewModel.")
            }
        })

        // Inicializar vistas
        photoView = findViewById(R.id.photoView)
        markerContainer = findViewById(R.id.mapContainerMarkers)
        bottomNavigationView = findViewById(R.id.bottomNavigationView)
        arrowOverlayView = findViewById(R.id.arrowOverlayView)


        // Configuración de la imagen en el PhotoView
        photoView.minimumScale = 1.0f
        photoView.mediumScale = 1.5f
        photoView.maximumScale = 3.0f

        // Listener para inicializar los marcadores una vez que la vista está lista
        photoView.viewTreeObserver.addOnGlobalLayoutListener(object :
            ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                if (listaLibros.isNotEmpty()) {
                    inicializarMarcadores()
                    photoView.viewTreeObserver.removeOnGlobalLayoutListener(this)
                } else {
                    Log.d(
                        "MapaInteractivo",
                        "PhotoView listo, pero listaLibros aún no está inicializada."
                    )
                }
            }
        })

        // Listener para actualizar la posición de los marcadores y las flechas
        photoView.setOnMatrixChangeListener { actualizarMarcadoresYFlechas() }

        // Configuración de BottomNavigationView
        bottomNavigationView.selectedItemId = R.id.nav_map

        bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_settings -> {
                    val intent = Intent(this, AjustesActivity::class.java)
                    intent.putExtra("USER_ID", userId)
                    startActivity(intent)
                    true
                }

                R.id.nav_map -> {
                    // Ya estamos en MapaInteractivoActivity, no hacemos nada
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

        // Configurar las relaciones cronológicas en el ArrowOverlayView
        arrowOverlayView.relaciones = relacionesCronologicas

        // Leyenda

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


        // Botón para ocultar la leyenda
        closeButton.setOnClickListener {
            if (isLeyendaVisible) {
                // Animar la leyenda hacia arriba
                val animator = ObjectAnimator.ofFloat(
                    leyendaView,
                    "translationY",
                    0f,
                    -leyendaView.height.toFloat()
                )
                animator.duration = 300
                animator.interpolator = AccelerateDecelerateInterpolator()
                animator.start()

                // Listener para ocultar la leyenda al final de la animación
                animator.addListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        leyendaView.visibility = View.GONE
                        followButton.isEnabled = true
                        leyendaView.translationY =
                            0f // Reinicia la posición para futuras animaciones
                    }
                })

                isLeyendaVisible = false
            }
        }
    }

    override fun onPause() {
        super.onPause()
        // Detener todas las animaciones cuando la actividad está en pausa
        animators.forEach { it.cancel() }
    }

    override fun onResume() {
        super.onResume()
        // Reanudar todas las animaciones cuando la actividad se reanuda
        animators.forEach { it.start() }
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(languageChangeReceiver)
    }

    private fun inicializarMarcadores() {
        if (listaLibros.isEmpty()) {
            Log.e("MapaInteractivo", "Intentando inicializar marcadores sin datos.")
            return
        }
        markerContainer.removeAllViews()
        markers.clear()
        animators.clear()

        listaLibros.forEach { libro ->
            // Definir el tamaño estándar
            var sizeInDpID = 60
            var red = 255
            var green = 255
            var blue = 255

            // Si inicialSaga es true, aumentar el tamaño y cambiar el color
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
                    val strokeWidth = (4 * scale + 0.5f).roundToInt() // 4dp
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

                // Configurar la animación del trazo
                val animator = ValueAnimator.ofInt(255, 50, 255).apply {
                    duration = 2500L
                    repeatMode = ValueAnimator.RESTART
                    repeatCount = ValueAnimator.INFINITE
                    addUpdateListener { animation ->
                        val alphaValue = animation.animatedValue as Int
                        // Actualizar el color del trazo con el nuevo valor de alpha
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

        // Llamar a actualizarMarcadoresYFlechas con un pequeño retraso para asegurar que PhotoView está listo
        Handler(Looper.getMainLooper()).postDelayed({
            actualizarMarcadoresYFlechas()
        }, 100) // Ajusta el tiempo de retraso si es necesario
    }

    private fun obtenerDrawablePorNombre(nombre: String): Int {
        val drawableId = resources.getIdentifier(nombre, "drawable", packageName)
        Log.d("DrawableVerification", "Nombre: $nombre, Drawable ID: $drawableId")
        if (drawableId == 0) {
            Log.e(
                "DrawableVerification",
                "Drawable no encontrado para: $nombre. Usando default_cover."
            )
            return R.drawable.portada_elcamino // Asegúrate de tener esta imagen en drawable/
        }
        return drawableId
    }


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

        // Mapa para almacenar las coordenadas en pantalla de cada libro
        val coordenadasPantalla = mutableMapOf<Int, Pair<Float, Float>>()

        markers.forEachIndexed { index, marker ->
            if (index >= listaLibros.size) return@forEachIndexed
            val libro = listaLibros[index]
            val coordNormalizada = libroCoordenadasNormalizadas[libro.id] ?: PointF(0.5f, 0.5f)
            val absX = (coordNormalizada.x * scaledWidth) + transX
            val absY = (coordNormalizada.y * scaledHeight) + transY

            // Centrar el marcador en las coordenadas calculadas
            marker.x = absX - (marker.width / 2)
            marker.y = absY - (marker.height / 2)

            // Calcular las coordenadas centrales del ImageView
            val centerX = marker.x + (marker.width / 2)
            val centerY = marker.y + (marker.height / 2)

            // Almacenar las coordenadas centrales en el mapa
            coordenadasPantalla[libro.id] = Pair(centerX, centerY)
        }

        // Actualizar las coordenadas en el ArrowOverlayView
        arrowOverlayView.libroCoordenadasPantalla = coordenadasPantalla

        // Redibujar las flechas
        arrowOverlayView.invalidate()
    }

    private fun abrirDetallesLibro(libroId: Int) {
        val intent = Intent(this, DetallesLibroActivity::class.java)
        intent.putExtra("LIBRO_ID", libroId)
        intent.putExtra("USER_ID", userId)
        Log.d("MapaInteractivo", "Intent: $intent")
        Log.d("MapaInteractivo", "Extras: ${intent?.extras}")
        startActivity(intent)
    }

    private fun cargarLibrosSiEsNecesario(nombreArchivoJson: String) {
        lifecycleScope.launch {
            try {
                // Verificar si la base de datos está vacía para el usuario actual
                val librosEnDb = libroViewModel.getAllLibrosByUsuario(userId).value
                if (librosEnDb.isNullOrEmpty()) {
                    // Leer el archivo JSON
                    val inputStream = assets.open(nombreArchivoJson)
                    val jsonString = inputStream.bufferedReader().use { it.readText() }

                    val gson = Gson()
                    val libros = gson.fromJson(jsonString, Array<Libro>::class.java).toList()

                    // Asignar el userId a cada libro cargado desde el JSON
                    val librosConUserId = libros.map { libro ->
                        libro.copy(userId = userId) // Asegura que userId no sea null
                    }

                    // Insertar los libros en la base de datos
                    libroViewModel.insertLibros(librosConUserId)

                    Log.d("MapaInteractivo", "Libros cargados desde JSON y asignados al userId: $userId")
                } else {
                    Log.d("MapaInteractivo", "Libros ya existen en la base de datos para el userId: $userId")
                }
            } catch (e: IOException) {
                Log.e("CargaLibros", "Error al cargar el archivo JSON: $nombreArchivoJson", e)
            } catch (e: Exception) {
                Log.e("CargaLibros", "Error inesperado al cargar libros: ${e.message}", e)
            }
        }
    }
}
