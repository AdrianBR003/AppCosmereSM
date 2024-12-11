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
import com.example.appsandersonsm.Modelo.Usuario
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
import java.util.UUID
import kotlin.random.Random

class LoginActivity : AppCompatActivity() {

    private lateinit var bookContainer: FrameLayout
    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var rlToggleLanguage: RelativeLayout
    private lateinit var tvLanguageState: TextView
    private var isChangingLanguage: Boolean = false
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
        private const val KEY_IS_LOGGED_IN = "IS_LOGGED_IN"
        private const val KEY_USER_ID = "USER_ID"
    }

    override fun attachBaseContext(newBase: Context?) {
        super.attachBaseContext(LocaleHelper.setLocale(newBase!!, ""))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        val sharedPreferences = getSharedPreferences("AppPreferences", MODE_PRIVATE)
        // Verificar si el usuario ya está autenticado
        val isLoggedIn = prefs.getBoolean(KEY_IS_LOGGED_IN, false)
        val userId = prefs.getString(KEY_USER_ID, null)
        sharedPreferences.edit().putString("USER_ID", userId).apply()

        if (isLoggedIn && userId != null) {
            // Redirigir a la siguiente actividad
            navigateToMainActivity(userId)
            return
        }

        val language = prefs.getString(KEY_LANGUAGE, "es") ?: "es"
        LocaleHelper.setLocale(this, language)

        setContentView(R.layout.activity_login)

        // Inicializar FirebaseAuth
        auth = FirebaseAuth.getInstance()
        configureGoogleSignIn()

        // Inicializar libroDao y notaDao
        val db = AppDatabase.getDatabase(applicationContext, lifecycleScope)
        libroDao = db.libroDao()
        notaDao = db.notaDao()

        // Inicializar Repository
        repository = DataRepository(libroDao, notaDao)

        // Inicializar vistas relacionadas con el cambio de idioma
        rlToggleLanguage = findViewById(R.id.rlToggleLanguage)
        tvLanguageState = findViewById(R.id.tvLanguageState)

        // Inicializar JsonHandler
        jsonHandler = JsonHandler(applicationContext, libroDao)

        // Configurar el fondo y el estado del idioma
        setBackgroundBasedOnLanguage(language)
        updateLanguageState(language)

        // Configurar el listener para cambiar el idioma
        rlToggleLanguage.setOnClickListener {
            if (!isChangingLanguage) {
                toggleLanguage()
            }
        }

        val isLoginSkipped = prefs.getBoolean(KEY_IS_LOGIN_SKIPPED, false)

        if (isLoginSkipped) {
            Toast.makeText(this, getString(R.string.mensajeInicioSesionInvitado), Toast.LENGTH_SHORT).show()
            navigateToMainActivity(UUID.randomUUID().toString())
            return
        }

        // Referencia al contenedor de libros
        bookContainer = findViewById(R.id.bookContainer)

        // Configurar el efecto de palpitar en el borde del botón
        addPulsatingEffectToBorder()

        // Configurar animaciones de portadas de libros
        cargarLibrosYConfigurarAnimaciones(language)

        findViewById<View>(R.id.btn_skip_login).setOnClickListener {
            skipLogin()
        }

        configureGoogleSignIn()

        val googleSignInButton = findViewById<View>(R.id.btn_google_sign_in)
        verificarTextoGoogleSignIn(googleSignInButton)

        findViewById<View>(R.id.btn_google_sign_in).setOnClickListener {
            signInWithGoogle()
        }
    }

    override fun onResume() {
        super.onResume()
        isChangingLanguage = false
        rlToggleLanguage.isEnabled = true
    }

    private fun toggleLanguage() {
        isChangingLanguage = true
        rlToggleLanguage.isEnabled = false

        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        val currentLanguage = prefs.getString(KEY_LANGUAGE, "es") ?: "es"
        val newLanguage = if (currentLanguage.startsWith("es")) "en" else "es"

        prefs.edit().putString(KEY_LANGUAGE, newLanguage).apply()

        LocaleHelper.setLocale(this, newLanguage)
        recreate()
    }

    private fun setBackgroundBasedOnLanguage(language: String?) {
        when (language?.lowercase()) {
            "es", "spanish" -> {
                rlToggleLanguage.setBackgroundResource(R.drawable.rounded_es)
            }
            "en", "english" -> {
                rlToggleLanguage.setBackgroundResource(R.drawable.rounded_en)
            }
            else -> {
                rlToggleLanguage.setBackgroundResource(R.drawable.rounded_es)
            }
        }
    }

    private fun updateLanguageState(language: String?) {
        tvLanguageState.text = when (language?.lowercase()) {
            "es", "spanish" -> getString(R.string.espanol)
            "en", "english" -> getString(R.string.ingles)
            else -> getString(R.string.espanol)
        }
    }

    private fun skipLogin() {
        val userId = UUID.randomUUID().toString()

        // Registrar al usuario invitado en Room
        registrarUsuarioEnRoom(userId, "Invitado", null, true)

        saveUserSession(userId, isLoginSkipped = true)
        navigateToMainActivity(userId)
    }

    private fun configureGoogleSignIn() {
        val idiomaActual = LocaleHelper.getLanguage(this)

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestIdToken(getString(R.string.default_web_client_id))
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)

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

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == RC_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                val account = task.getResult(ApiException::class.java)
                handleGoogleSignIn(account)
            } catch (e: ApiException) {
                Toast.makeText(this, getString(R.string.error_iniciar_sesion), Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun handleGoogleSignIn(account: GoogleSignInAccount?) {
        if (account != null) {
            val credential = GoogleAuthProvider.getCredential(account.idToken, null)
            auth.signInWithCredential(credential).addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val userId = account.id ?: UUID.randomUUID().toString()
                    val userName = account.displayName
                    val userEmail = account.email

                    // Registrar al usuario en Room
                    registrarUsuarioEnRoom(userId, userName, userEmail, false)

                    saveUserSession(userId)
                    navigateToMainActivity(userId)
                } else {
                    Toast.makeText(this, getString(R.string.error_iniciar_sesion), Toast.LENGTH_SHORT).show()
                }
            }
        }
    }


    private fun saveUserSession(userId: String, isLoginSkipped: Boolean = false) {
        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        prefs.edit().apply {
            putBoolean(KEY_IS_LOGGED_IN, true)
            putString(KEY_USER_ID, userId)
            putBoolean(KEY_IS_LOGIN_SKIPPED, isLoginSkipped)
            apply()
        }
    }


    private fun navigateToMainActivity(userId: String) {
        val intent = Intent(this, MapaInteractivoActivity::class.java).apply {
            putExtra("USER_ID", userId)
        }
        startActivity(intent)
        finish()
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

    private fun cargarLibrosYConfigurarAnimaciones(languageCode: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            val libros = jsonHandler.cargarLibrosDesdeJson(languageCode) // Daigual en este caso el userId
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
                        bookCover.setPadding(2, 2, 2, 2)
                        bookContainer.addView(bookCover)

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
        val valorA = (Random.nextFloat() * 0.5f) + 0.2f
        val startY = containerHeight
        val endY = containerHeight * valorA
        val bound = (containerWidth - 200).toInt()
        val startX = if (bound > 0) Random.nextInt(bound).toFloat() else 0f

        bookCover.x = startX
        bookCover.y = startY

        val translateY = ObjectAnimator.ofFloat(bookCover, "y", startY, endY).apply {
            duration = 6000
            interpolator = DecelerateInterpolator()
        }

        val fadeOut = ObjectAnimator.ofFloat(bookCover, "alpha", 1f, 0f).apply {
            duration = 500
            startDelay = 1500
        }

        val animatorSet = AnimatorSet()
        animatorSet.playTogether(translateY, fadeOut)
        animatorSet.startDelay = (Random.nextInt(3500) + 2000).toLong()
        animatorSet.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationStart(animation: Animator) {
                bookCover.visibility = View.VISIBLE
            }

            override fun onAnimationEnd(animation: Animator) {
                val validBound = (containerWidth - 200).coerceAtLeast(0f).toInt()
                val newStartX = if (validBound > 0) Random.nextInt(validBound).toFloat() else 0f
                bookCover.x = newStartX
                bookCover.y = startY
                bookCover.alpha = 1f
                animatorSet.start()
            }
        })

        animatorSet.start()
    }

    private fun obtenerDrawablePorNombre(nombre: String): Int {
        val drawableId = resources.getIdentifier(nombre, "drawable", packageName)
        if (drawableId == 0) {
            Log.e("DrawableVerification", "Drawable no encontrado para: $nombre. Usando default_cover.")
            return R.drawable.portada_elcamino
        }
        return drawableId
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

                    child.text = customText
                    Log.d("LoginActivity", "Texto forzado del botón Google Sign-In: $customText")
                }
            }
        } else {
            Log.e("LoginActivity", "El botón de Google Sign-In no es del tipo esperado.")
        }
    }

    private fun registrarUsuarioEnRoom(userId: String, nombre: String?, email: String?, esInvitado: Boolean) {
        val usuarioDao = AppDatabase.getDatabase(applicationContext, lifecycleScope).usuarioDao()
        val nuevoUsuario = Usuario(
            id = userId,
            nombre = nombre,
            email = email,
            esInvitado = esInvitado
        )

        lifecycleScope.launch {
            usuarioDao.insertarUsuario(nuevoUsuario)
        }
    }


}