package com.example.appsandersonsm

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.view.ViewTreeObserver
import android.view.animation.DecelerateInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.appsandersonsm.Dao.LibroDao
import com.example.appsandersonsm.Dao.NotaDao
import com.example.appsandersonsm.DataBase.AppDatabase
import com.example.appsandersonsm.DataBase.JsonHandler
import com.example.appsandersonsm.Firestore.DataRepository
import com.example.appsandersonsm.Locale.LocaleHelper
import com.example.appsandersonsm.MapaInteractivoActivity
import com.example.appsandersonsm.Modelo.Libro
import com.example.appsandersonsm.R
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.SignInButton
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.random.Random

class LoginActivity : AppCompatActivity() {

    private lateinit var bookContainer: FrameLayout
    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var rlToggleLanguage: RelativeLayout
    private lateinit var tvLanguageState: TextView
    private var isChangingLanguage: Boolean = false // Nueva variable de control
    private lateinit var jsonHandler: JsonHandler
    private lateinit var auth: FirebaseAuth
    private lateinit var libroDao: LibroDao
    private lateinit var notaDao: NotaDao
    private lateinit var repository: DataRepository

    companion object {
        private const val RC_SIGN_IN = 9001
        private const val PREFS_NAME = "AppPreferences"
        private const val KEY_LANGUAGE = "language"
        private const val KEY_IS_LOGIN_SKIPPED = "isLoginSkipped"
    }

    override fun attachBaseContext(newBase: Context?) {
        super.attachBaseContext(LocaleHelper.setLocale(newBase!!,""))
    }

    override fun onCreate(savedInstanceState: Bundle?) {

        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)

        // Verificar si el usuario ya está autenticado
        val isLoggedIn = prefs.getBoolean("IS_LOGGED_IN", false)
        val userId = prefs.getString("USER_ID", null)

        if (isLoggedIn && userId != null) {
            // Redirigir a la siguiente actividad
            val intent = Intent(this, MapaInteractivoActivity::class.java)
            startActivity(intent)
            finish()
            return
        }
        
        val language = prefs.getString(KEY_LANGUAGE, "es") ?: "es" // Valor por defecto "es"
        LocaleHelper.setLocale(this, language)


        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        // Inicializar FirebaseAuth
        auth = FirebaseAuth.getInstance()
        configureGoogleSignIn()

        // Inicializar libroDao y notaDao primero
        val db = AppDatabase.getDatabase(applicationContext, lifecycleScope)
        libroDao = db.libroDao()
        Log.d("LoginActivity", "libroDao inicializado correctamente.")
        notaDao = db.notaDao()

        // Inicializar el Repository después
        repository = DataRepository(libroDao, notaDao)

        // Inicializar vistas relacionadas con el cambio de idioma
        rlToggleLanguage = findViewById(R.id.rlToggleLanguage)
        tvLanguageState = findViewById(R.id.tvLanguageState)

        // Inicializar JsonHandler
        jsonHandler = JsonHandler(applicationContext, libroDao)

        // Inicializar FirebaseAuth
        auth = FirebaseAuth.getInstance()

        // Inicializar Repository
        repository = DataRepository(libroDao, notaDao)

        // Configurar el fondo y el estado del idioma
        setBackgroundBasedOnLanguage(language)
        updateLanguageState(language)

        // Configurar el listener para cambiar el idioma
        rlToggleLanguage.setOnClickListener {
            if (!isChangingLanguage) {
                toggleLanguage()
            }
        }

        val sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        val isLoginSkipped = sharedPreferences.getBoolean(KEY_IS_LOGIN_SKIPPED, false)

        if (isLoginSkipped) {
            Toast.makeText(this, getString(R.string.mensajeInicioSesionInvitado), Toast.LENGTH_SHORT).show()
            val intent = Intent(this, MapaInteractivoActivity::class.java) // Cambia esta actividad si es necesario
            startActivity(intent)
            finish() // Finaliza LoginActivity
            return
        }

        // Referencia al contenedor de libros
        bookContainer = findViewById(R.id.bookContainer)

        // Configurar el efecto de palpitar en el borde del botón
        addPulsatingEffectToBorder()

        // Configurar animaciones de portadas de libros
        // Cargar libros desde JSON según el idioma
        cargarLibrosYConfigurarAnimaciones(language)

        findViewById<View>(R.id.btn_skip_login).setOnClickListener {
            skipLogin()
        }

        configureGoogleSignIn()

        val googleSignInButton = findViewById<View>(R.id.btn_google_sign_in)
        verificarTextoGoogleSignIn(googleSignInButton)

        // Configurar el botón de inicio de sesión de Google
        findViewById<View>(R.id.btn_google_sign_in).setOnClickListener {
            signInWithGoogle()
        }
    }



    override fun onResume() {
        super.onResume()
        // Rehabilitar el botón cuando la actividad se haya recreado
        isChangingLanguage = false
        rlToggleLanguage.isEnabled = true
    }

    /**
     * Cambia el idioma entre inglés y español.
     */
    private fun toggleLanguage() {
        isChangingLanguage = true
        rlToggleLanguage.isEnabled = false // Deshabilitar el botón para evitar múltiples clics

        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        val currentLanguage = prefs.getString(KEY_LANGUAGE, "es") ?: "es"
        val newLanguage = if (currentLanguage.startsWith("es")) "en" else "es"

        // Guardar el nuevo idioma en SharedPreferences
        prefs.edit().putString(KEY_LANGUAGE, newLanguage).apply()

        // Reiniciar la actividad para aplicar los cambios
        LocaleHelper.setLocale(this, newLanguage)
        recreate() // Reinicia la actividad
    }

    /**
     * Configura el fondo del RelativeLayout según el idioma.
     */
    private fun setBackgroundBasedOnLanguage(language: String?) {
        when (language?.lowercase()) {
            "es", "spanish" -> {
                rlToggleLanguage.setBackgroundResource(R.drawable.rounded_es) // Reemplaza con tu drawable para español
            }
            "en", "english" -> {
                rlToggleLanguage.setBackgroundResource(R.drawable.rounded_en) // Reemplaza con tu drawable para inglés
            }
            else -> {
                // Fondo por defecto si el idioma no es reconocido
                rlToggleLanguage.setBackgroundResource(R.drawable.rounded_es)
            }
        }
    }

    /**
     * Actualiza el estado del TextView que muestra el idioma actual.
     */
    private fun updateLanguageState(language: String?) {
        tvLanguageState.text = when (language?.lowercase()) {
            "es", "spanish" -> getString(R.string.espanol)
            "en", "english" -> getString(R.string.ingles)
            else -> getString(R.string.espanol)
        }
    }

    private fun skipLogin() {
        // Guardar en SharedPreferences que el usuario ha omitido el inicio de sesión
        val sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        sharedPreferences.edit().putBoolean(KEY_IS_LOGIN_SKIPPED, true).apply()

        // Navegar a la siguiente actividad
        val intent = Intent(this, MapaInteractivoActivity::class.java)
        startActivity(intent)
        finish()
    }

    private fun configureGoogleSignIn() {
        val idiomaActual = LocaleHelper.getLanguage(this) // Devuelve el idioma, e.g., "es" o "en"

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail() // Solicitar el correo electrónico
            .requestIdToken(getString(R.string.default_web_client_id)) // Solicitar el idToken
            .build()

        // Configurar el cliente de Google Sign-In
        googleSignInClient = GoogleSignIn.getClient(this, gso)

        // Configurar el idioma global para que afecte al botón
        LocaleHelper.setLocale(this, idiomaActual)
    }

    private fun signInWithGoogle() {
        if (!::googleSignInClient.isInitialized) {
            Log.e("LoginActivity", "GoogleSignInClient no está inicializado.")
            return
        }

        val signInIntent = googleSignInClient.signInIntent
        startActivityForResult(signInIntent, RC_SIGN_IN)
    }


    private fun addPulsatingEffectToBorder() {
        val container = findViewById<FrameLayout>(R.id.btn_google_sign_in_container)
        val background = container.background.mutate() as? GradientDrawable

        val animator = ValueAnimator.ofFloat(1f, 0.5f, 1f).apply {
            duration = 3500
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.RESTART

            addUpdateListener { animation ->
                val alphaValue = animation.animatedValue as Float
                background?.alpha = (alphaValue * 255).toInt()
            }
        }

        animator.start()
    }

    private fun obtenerDrawablePorNombre(nombre: String): Int {
        val drawableId = resources.getIdentifier(nombre, "drawable", packageName)
        if (drawableId == 0) {
            Log.e("DrawableVerification", "Drawable no encontrado para: $nombre. Usando default_cover.")
            return R.drawable.portada_elcamino
        }
        return drawableId
    }

    /**
     * Carga los libros desde el JSON correspondiente y configura las animaciones.
     * @param languageCode El código del idioma actual ("en" o "es").
     */
    private fun cargarLibrosYConfigurarAnimaciones(languageCode: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            val libros = jsonHandler.cargarLibrosDesdeJson(languageCode)
            if (libros.isNotEmpty()) {
                withContext(Dispatchers.Main) {
                    setupBookAnimations(libros)
                }
            } else {
                withContext(Dispatchers.Main) {
                    Log.e("LoginActivity", "No se cargaron libros desde el JSON para el idioma: $languageCode")
                }
            }
        }
    }




    /**
     * Configura las animaciones de las portadas de libros.
     * @param libros La lista de libros a animar.
     */
    private fun setupBookAnimations(libros: List<Libro>) {
        Log.d("LoginActivity", "setupBookAnimations llamado con ${libros.size} libros.")
        bookContainer.post {
            val containerHeight = bookContainer.height.toFloat()
            val containerWidth = bookContainer.width.toFloat()

            if (containerHeight > 0 && containerWidth > 0) {
                for ((index, libro) in libros.withIndex()) {
                    val bookCover = ImageView(this@LoginActivity)
                    val drawableId = libro.nombrePortada?.let { obtenerDrawablePorNombre(it) } ?: 0

                    if (drawableId != 0) {
                        bookCover.setImageResource(drawableId)
                        val params = FrameLayout.LayoutParams(200, 300)
                        bookCover.layoutParams = params
                        bookCover.visibility = View.INVISIBLE
                        bookCover.setBackgroundResource(R.drawable.book_init_border)
                        bookCover.setPadding(2, 2, 2, 2) // Ajusta los valores para dar espacio al marco
                        bookContainer.addView(bookCover)

                        // Configura la animación de cada libro
                        animateBookCover(bookCover, containerWidth, containerHeight, index)
                    } else {
                        Log.e("LoginActivity", "No se encontró la portada: ${libro.nombrePortada}")
                    }
                }
            } else {
                Log.e("LoginActivity", "Dimensiones del contenedor no válidas")
            }
        }
    }


    private fun animateBookCover(
        bookCover: ImageView,
        containerWidth: Float,
        containerHeight: Float,
        index: Int
    ) {
        // Posiciones iniciales y finales

        val valorA = (Random.nextFloat() * 0.5f) + 0.2f
        val startY = containerHeight
        val endY = containerHeight * valorA
        val bound = (containerWidth - 200).toInt()
        val startX = if (bound > 0) Random.nextInt(bound).toFloat() else 0f

        // Configuración inicial
        bookCover.x = startX
        bookCover.y = startY

        // Animación de movimiento hacia arriba
        val translateY = ObjectAnimator.ofFloat(bookCover, "y", startY, endY).apply {
            duration = 6000
            interpolator = DecelerateInterpolator()
        }

        // Animación de desvanecimiento
        val fadeOut = ObjectAnimator.ofFloat(bookCover, "alpha", 1f, 0f).apply {
            duration = 500
            startDelay = 1500
        }

        // Combinar animaciones
        val animatorSet = AnimatorSet()
        animatorSet.playTogether(translateY, fadeOut)
        animatorSet.startDelay = (Random.nextInt(3500) + 2000).toLong() // Inicio de las animaciones - Retraso aleatorio entre 2 y 5.5 segundos
        animatorSet.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationStart(animation: Animator) {
                bookCover.visibility = View.VISIBLE
            }

            override fun onAnimationEnd(animation: Animator) {
                // Reinicia la posición y la alpha
                val validBound = (containerWidth - 200).coerceAtLeast(0f).toInt()
                val newStartX = if (validBound > 0) Random.nextInt(validBound).toFloat() else 0f
                bookCover.x = newStartX
                bookCover.y = startY
                bookCover.alpha = 1f
                animatorSet.start() // Reinicia la animación
            }
        })

        animatorSet.start()
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == RC_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                val account = task.getResult(ApiException::class.java)
                handleSignInResult(account)
            } catch (e: ApiException) {
                Log.e("LoginActivity", "SignInResult: failed code=" + e.statusCode)
                Toast.makeText(this, getString(R.string.error_iniciar_sesion, e.localizedMessage), Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun handleSignInResult(account: GoogleSignInAccount?) {
        if (account != null) {
            val credential = GoogleAuthProvider.getCredential(account.idToken, null)
            auth.signInWithCredential(credential)
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        val user = auth.currentUser
                        if (user != null) {
                            val userId = user.uid
                            // Sincronizar datos con Firestore
                            lifecycleScope.launch {
                                repository.synchronizeData(userId)
                                Log.d("LoginActivity", "Sincronización de datos completada exitosamente.")
                            }
                            // Escuchar actualizaciones en tiempo real (opcional)
                            repository.listenForFirestoreUpdates(userId)
                            Log.d("LoginActivity", "Listeners para Firestore establecidos correctamente.")
                        }
                        val welcomeMessage = getString(R.string.bienvenido_inicio, account.displayName)
                        Toast.makeText(this, welcomeMessage, Toast.LENGTH_SHORT).show()
                        val intent = Intent(this, MapaInteractivoActivity::class.java)
                        startActivity(intent)
                        finish()
                    } else {
                        // Manejar fallos en el inicio de sesión
                        Log.e("LoginActivity", "signInWithCredential:failure", task.exception)
                        Toast.makeText(this, getString(R.string.error_iniciar_sesion), Toast.LENGTH_SHORT).show()
                    }
                }
        }
    }

    private fun verificarTextoGoogleSignIn(googleSignInButton: View) {
        if (googleSignInButton is SignInButton) {
            for (i in 0 until googleSignInButton.childCount) {
                val child = googleSignInButton.getChildAt(i)
                if (child is TextView) {
                    val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                    val language = prefs.getString(KEY_LANGUAGE, "es") ?: "es"

                    val customText = when (language) {
                        "en" -> "  Sign in          "
                        "es" -> "Iniciar sesión"
                        else -> "  Sign in          "
                    }

                    child.text = customText // Cambiar manualmente el texto
                    Log.d("LoginActivity", "Texto forzado del botón Google Sign-In: $customText")
                }
            }
        } else {
            Log.e("LoginActivity", "El botón de Google Sign-In no es del tipo esperado.")
        }
    }


}
