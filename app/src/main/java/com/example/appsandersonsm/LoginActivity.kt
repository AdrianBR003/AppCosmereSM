package com.example.appsandersonsm

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
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
import com.example.appsandersonsm.DataBase.JsonHandler
import com.example.appsandersonsm.MapaInteractivoActivity
import com.example.appsandersonsm.R
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import java.util.Locale
import kotlin.random.Random

class LoginActivity : AppCompatActivity() {

    private lateinit var bookContainer: FrameLayout
    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var rlToggleLanguage: RelativeLayout
    private lateinit var tvLanguageState: TextView
    private var isEnglish: Boolean = false // Estado inicial del idioma

    companion object {
        private const val RC_SIGN_IN = 9001
        private const val PREFS_NAME = "AppPreferences"
        private const val KEY_LANGUAGE = "language"
        private const val KEY_IS_LOGIN_SKIPPED = "isLoginSkipped"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Recuperar SharedPreferences
        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        var language = prefs.getString(KEY_LANGUAGE, null)

        if (language == null) {
            // Obtener el idioma del sistema si no está guardado
            language = Locale.getDefault().language
            // Guardar el idioma del sistema en SharedPreferences
            prefs.edit().putString(KEY_LANGUAGE, language).apply()
        }

        // Configurar el idioma
        if (language != null) {
            setLocale(language)
        }

        // Establecer el contenido de la vista después de configurar el idioma
        setContentView(R.layout.activity_login)

        // Inicializar vistas relacionadas con el cambio de idioma
        rlToggleLanguage = findViewById(R.id.rlToggleLanguage)
        tvLanguageState = findViewById(R.id.tvLanguageState)

        // Configurar el fondo según el idioma
        setBackgroundBasedOnLanguage(language)

        // Actualizar el estado del TextView
        updateLanguageState(language)

        // Configurar el listener para cambiar el idioma
        rlToggleLanguage.setOnClickListener {
            toggleLanguage()
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
        setupBookAnimations()

        findViewById<View>(R.id.btn_skip_login).setOnClickListener {
            skipLogin()
        }

        // Configuración de inicio de sesión con Google
        configureGoogleSignIn()

        // Configurar el botón de inicio de sesión de Google
        findViewById<View>(R.id.btn_google_sign_in).setOnClickListener {
            signInWithGoogle()
        }
    }

    /**
     * Configura el fondo del RelativeLayout según el idioma.
     */
    private fun setBackgroundBasedOnLanguage(language: String?) {
        when (language?.lowercase()) {
            "es", "spanish" -> {
                rlToggleLanguage.setBackgroundResource(R.drawable.rounded_es) // Reemplaza con tu drawable para español
                isEnglish = false
            }
            "en", "english" -> {
                rlToggleLanguage.setBackgroundResource(R.drawable.rounded_en) // Reemplaza con tu drawable para inglés
                isEnglish = true
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
            else -> getString(R.string.idioma_desconocido)
        }
    }

    /**
     * Cambia el idioma entre inglés y español.
     */
    private fun toggleLanguage() {
        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        val currentLanguage = prefs.getString(KEY_LANGUAGE, "en") ?: "en"
        val newLanguage = if (currentLanguage.startsWith("es")) "en" else "es"

        // Guardar el nuevo idioma en SharedPreferences
        prefs.edit().putString(KEY_LANGUAGE, newLanguage).apply()

        // Reiniciar la actividad para aplicar los cambios
        recreate()
    }

    /**
     * Reinicia la actividad con la nueva configuración de idioma.
     */
    override fun recreate() {
        super.recreate()
        // Opcional: Puedes añadir animaciones de transición si lo deseas
    }

    private fun skipLogin() {
        // Guardar en SharedPreferences que el usuario ha omitido el inicio de sesión
        val sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        sharedPreferences.edit().putBoolean(KEY_IS_LOGIN_SKIPPED, true).apply()

        // Navegar a la siguiente actividad
        val intent = Intent(this, MapaInteractivoActivity::class.java) // Cambia esta actividad si es necesario
        startActivity(intent)
        finish() // Finaliza la actividad actual
    }

    private fun configureGoogleSignIn() {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)
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
        return resources.getIdentifier(nombre, "drawable", packageName)
    }

    private fun setupBookAnimations() {
        val jsonHandler = JsonHandler(this)
        val libros = jsonHandler.cargarLibrosDesdeJson() // Carga los datos del JSON

        bookContainer.viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                // Remueve el listener para evitar múltiples llamadas
                bookContainer.viewTreeObserver.removeOnGlobalLayoutListener(this)

                val containerHeight = bookContainer.height.toFloat()
                val containerWidth = bookContainer.width.toFloat()

                if (containerHeight > 0 && containerWidth > 0) {
                    for ((index, libro) in libros.withIndex()) {
                        val bookCover = ImageView(this@LoginActivity)
                        val drawableId = libro.nombrePortada?.let { obtenerDrawablePorNombre(it) }
                            ?: 0

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
        })
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

    private fun signInWithGoogle() {
        val signInIntent = googleSignInClient.signInIntent
        startActivityForResult(signInIntent, RC_SIGN_IN)
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
            val welcomeMessage = getString(R.string.bienvenido_inicio, account.displayName)
            Toast.makeText(this, welcomeMessage, Toast.LENGTH_SHORT).show()
            val intent = Intent(this, MapaInteractivoActivity::class.java)
            startActivity(intent)
            finish()
        }
    }

    /**
     * Configura la localización de la aplicación.
     */
    private fun setLocale(language: String) {
        val locale = Locale(language)
        Locale.setDefault(locale)
        val config = resources.configuration
        config.setLocale(locale)
        resources.updateConfiguration(config, resources.displayMetrics)
    }
}
