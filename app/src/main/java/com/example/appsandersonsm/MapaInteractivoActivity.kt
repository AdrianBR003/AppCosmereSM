// src/main/java/com/example/appsandersonsm/MapaInteractivoActivity.kt
package com.example.appsandersonsm

import android.content.Intent
import android.graphics.Matrix
import android.graphics.PointF
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.ViewTreeObserver
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.example.appsandersonsm.Modelo.Libro
import com.example.appsandersonsm.Repositorio.DatabaseHelper
import com.google.android.material.bottomnavigation.BottomNavigationView
import io.getstream.photoview.PhotoView

class MapaInteractivoActivity : AppCompatActivity() {

    private lateinit var photoView: PhotoView
    private lateinit var markerContainer: FrameLayout
    private lateinit var bottomNavigationView: BottomNavigationView
    private lateinit var dbHelper: DatabaseHelper
    private lateinit var listaLibros: List<Libro>
    private val markers = mutableListOf<ImageView>()
    private lateinit var drawable: Drawable
    private lateinit var arrowOverlayView: ArrowOverlayView


    // Coordenadas normalizadas asociadas por ID de libro
    private val libroCoordenadasNormalizadas = mapOf(
        1 to PointF(0.38f, 0.80f),  // ID 1
        2 to PointF(0.45f, 0.86f),  // ID 2
        3 to PointF(0.55f, 0.88f),  // ID 3
        4 to PointF(0.64f, 0.85f),  // ID 4
        5 to PointF(0.06f,0.76f), // aliento
        6 to PointF(0.105f,0.63f),  // heroe
        7 to PointF(0.07f,0.50f),  // pozo
        8 to PointF(0.12f,0.40f),  // nacidos
        )

    // Definir relaciones cronológicas entre libros
    private val relacionesCronologicas = listOf(
        Pair(1, 2),
        Pair(2, 3),
        Pair(3, 4),
        Pair(5,1),
        Pair(8,7),
        Pair(7,6),
        Pair(6,5),
        // Agrega más relaciones según tus necesidades
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_mapa_interactivo)

        supportActionBar?.hide() // Hide default topbar with app name

        // Inicializar el DatabaseHelper y cargar los datos iniciales si la base de datos está vacía
        dbHelper = DatabaseHelper(this)
        dbHelper.cargarDatosInicialesDesdeJson()

        // Obtener la lista de libros desde la base de datos
        listaLibros = dbHelper.getAllLibros()

        // Inicializar vistas
        photoView = findViewById(R.id.photoView)
        markerContainer = findViewById(R.id.mapContainerMarkers)
        bottomNavigationView = findViewById(R.id.bottomNavigationView)
        arrowOverlayView = findViewById(R.id.arrowOverlayView)

        // Configuración de la imagen en el PhotoView
        drawable = photoView.drawable ?: throw IllegalStateException("PhotoView no tiene una imagen establecida.")
        photoView.minimumScale = 1.0f
        photoView.mediumScale = 1.5f
        photoView.maximumScale = 3.0f

        // Listener para inicializar los marcadores
        photoView.viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                inicializarMarcadores()
                photoView.viewTreeObserver.removeOnGlobalLayoutListener(this)
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
//        arrowOverlayView.libros = listaLibros
    }

    private fun inicializarMarcadores() {
        listaLibros.forEach { libro ->
            val marker = ImageView(this).apply {
                val sizeInDp = 60
                val scale = resources.displayMetrics.density
                val sizeInPx = (sizeInDp * scale + 0.5f).toInt()
                layoutParams = FrameLayout.LayoutParams(sizeInPx, sizeInPx)
                scaleType = ImageView.ScaleType.CENTER_CROP
                contentDescription = "Libro ${libro.id}"

                // Cargar la imagen de la portada
                val resID = resources.getIdentifier(libro.nombrePortada, "drawable", packageName)
                Glide.with(this@MapaInteractivoActivity)
                    .load(resID)
                    .apply(RequestOptions.circleCropTransform())
                    .into(this)

                setOnClickListener {
                    abrirDetallesLibro(libro.id)
                }
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

        val scaleX = matrix[Matrix.MSCALE_X]
        val scaleY = matrix[Matrix.MSCALE_Y]
        val transX = matrix[Matrix.MTRANS_X]
        val transY = matrix[Matrix.MTRANS_Y]

        val imageWidth = drawable.intrinsicWidth.toFloat()
        val imageHeight = drawable.intrinsicHeight.toFloat()

        val scaledWidth = imageWidth * scaleX
        val scaledHeight = imageHeight * scaleY

        // Mapa para almacenar las coordenadas en pantalla de cada libro
        val coordenadasPantalla = mutableMapOf<Int, Pair<Float, Float>>()

        markers.forEachIndexed { index, marker ->
            val libro = listaLibros[index]
            val coordNormalizada = libroCoordenadasNormalizadas[libro.id] ?: PointF(0.5f, 0.5f)
            val absX = (coordNormalizada.x * scaledWidth) + transX
            val absY = (coordNormalizada.y * scaledHeight) + transY
            marker.x = absX - (marker.width / 2)
            marker.y = absY - (marker.height / 2)

            coordenadasPantalla[libro.id] = Pair(absX, absY)
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
