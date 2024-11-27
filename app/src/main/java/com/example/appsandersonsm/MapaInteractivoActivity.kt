package com.example.appsandersonsm

import android.animation.ValueAnimator
import android.content.Intent
import android.graphics.Color
import android.graphics.PointF
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.ViewTreeObserver
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.example.appsandersonsm.Modelo.Libro
import com.example.appsandersonsm.ViewModel.LibroViewModel
import com.example.appsandersonsm.ViewModel.LibroViewModelFactory
import com.google.android.material.bottomnavigation.BottomNavigationView
import io.getstream.photoview.PhotoView
import kotlin.math.roundToInt

class MapaInteractivoActivity : AppCompatActivity() {

    private lateinit var photoView: PhotoView
    private lateinit var markerContainer: FrameLayout
    private lateinit var bottomNavigationView: BottomNavigationView
    private var listaLibros: List<Libro> = emptyList()
    private val markers = mutableListOf<ImageView>()
    private lateinit var arrowOverlayView: ArrowOverlayView


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
        photoView.viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                if (listaLibros.isNotEmpty()) {
                    inicializarMarcadores()
                    photoView.viewTreeObserver.removeOnGlobalLayoutListener(this)
                } else {
                    Log.d("MapaInteractivo", "PhotoView listo, pero listaLibros aún no está inicializada.")
                }
            }
        })

        libroViewModel.allLibros.observe(this, Observer { libros ->
            if (libros != null) {
                listaLibros = libros
                if (photoView.viewTreeObserver.isAlive) {
                    inicializarMarcadores()
                }
            } else {
                Log.e("MapaInteractivo", "No se encontraron libros en el ViewModel.")
            }
        })

        // Listener para actualizar la posición de los marcadores y las flechas
        photoView.setOnMatrixChangeListener { actualizarMarcadoresYFlechas() }

        // Configuración de BottomNavigationView
        bottomNavigationView.selectedItemId = R.id.nav_map

        bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_settings -> {
                    startActivity(Intent(this, AjustesActivity::class.java))
                    true
                }
                R.id.nav_map -> {
                    // Ya estamos en MapaInteractivoActivity, no hacemos nada
                    true
                }
                R.id.nav_book -> {
                    startActivity(Intent(this, LibroActivity::class.java))
                    true
                }
                else -> false
            }
        }

        // Configurar las relaciones cronológicas en el ArrowOverlayView
        arrowOverlayView.relaciones = relacionesCronologicas

        // Observa los datos de libros desde el ViewModel
        libroViewModel.allLibros.observe(this, Observer { libros ->
            if (libros != null) {
                listaLibros = libros
                // Solo inicializar los marcadores si el ViewTreeObserver ya se ejecutó
                if (photoView.viewTreeObserver.isAlive) {
                    inicializarMarcadores()
                } else {
                    Log.d("MapaInteractivo", "PhotoView aún no está listo. Esperando para inicializar marcadores.")
                }
            } else {
                Log.e("MapaInteractivo", "No se encontraron libros para inicializar marcadores.")
            }
        })
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
        }, 50) // Ajusta el tiempo de retraso si es necesario
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
        startActivity(intent)
    }
}
