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
import android.view.animation.DecelerateInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.map
import com.example.appsandersonsm.Dao.LibroDao
import com.example.appsandersonsm.Dao.NotaDao
import com.example.appsandersonsm.Dao.UsuarioDao
import com.example.appsandersonsm.DataBase.AppDatabase
import com.example.appsandersonsm.DataBase.JsonHandler
import com.example.appsandersonsm.Firestore.DataRepository
import com.example.appsandersonsm.Locale.LocaleHelper
import com.example.appsandersonsm.MapaInteractivoActivity
import com.example.appsandersonsm.Modelo.Libro
import com.example.appsandersonsm.Modelo.Nota
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
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume

import kotlin.random.Random
import android.widget.*
import kotlinx.coroutines.*
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import com.example.appsandersonsm.ViewModel.LibroViewModel

import com.google.android.gms.tasks.Task

class LoginActivity : AppCompatActivity() {

    // Declaración de variables tardías (lateinit)
    private lateinit var bookContainer: FrameLayout
    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var rlToggleLanguage: RelativeLayout
    private lateinit var tvLanguageState: TextView
    private var isChangingLanguage: Boolean = false
    private lateinit var jsonHandler: JsonHandler
    private lateinit var auth: FirebaseAuth
    private lateinit var libroDao: LibroDao
    private lateinit var usuarioDao: UsuarioDao
    private lateinit var notaDao: NotaDao
    private lateinit var libroViewModel: LibroViewModel

    companion object {
        private const val RC_SIGN_IN = 9001
        private const val PREFS_NAME = "AppPreferences"
        private const val KEY_LANGUAGE = "language"
        private const val KEY_IS_LOGIN_SKIPPED = "isLoginSkipped"
        private const val KEY_IS_LOGGED_IN = "IS_LOGGED_IN"
        private const val KEY_USER_ID = "USER_ID"
    }

    private val firestore = FirebaseFirestore.getInstance()

    override fun attachBaseContext(newBase: Context?) {
        val baseContext = newBase ?: this
        val prefs = baseContext.getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        val language = prefs.getString(KEY_LANGUAGE, "es") ?: "es"
        super.attachBaseContext(LocaleHelper.setLocale(baseContext, language))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        val isLoginSkipped = prefs.getBoolean(KEY_IS_LOGIN_SKIPPED, false)
        val isLoggedIn = prefs.getBoolean(KEY_IS_LOGGED_IN, false)
        val userId = prefs.getString(KEY_USER_ID, null)

        // Si ya está logueado o ha hecho skip anteriormente
        if ((isLoggedIn && userId != null) || (isLoginSkipped && userId != null)) {
            // Evitas mostrar la pantalla de login:
            lifecycleScope.launch {
                // Haces la sincronización que necesites aquí
                if (!userId.equals("id_default")) {
                    sincronizarDatosConFirestore(userId)
                }
                navigateToMainActivity(userId)
            }
            return
        }

        val editor = prefs.edit()

        if (!prefs.contains(KEY_LANGUAGE)) {
            editor.putString(KEY_LANGUAGE, "es")
        }

        if (!prefs.contains(KEY_IS_LOGIN_SKIPPED)) {
            editor.putBoolean(KEY_IS_LOGIN_SKIPPED, false)
        }

        if (!prefs.contains(KEY_IS_LOGGED_IN)) {
            editor.putBoolean(KEY_IS_LOGGED_IN, false)
        }

        if (!prefs.contains(KEY_USER_ID)) {
            editor.putString(KEY_USER_ID, null)
        }

        editor.apply()

        val language = prefs.getString(KEY_LANGUAGE, "es") ?: "es"
        LocaleHelper.setLocale(this, language)

        setContentView(R.layout.activity_login)

        auth = FirebaseAuth.getInstance()
        configureGoogleSignIn()

        val db = AppDatabase.getDatabase(applicationContext, lifecycleScope)
        usuarioDao = db.usuarioDao()
        libroDao = db.libroDao()
        notaDao = db.notaDao()

        jsonHandler = JsonHandler(applicationContext, libroDao)

        rlToggleLanguage = findViewById(R.id.rlToggleLanguage)
        tvLanguageState = findViewById(R.id.tvLanguageState)
        setBackgroundBasedOnLanguage(language)
        updateLanguageState(language)

        rlToggleLanguage.setOnClickListener {
            if (!isChangingLanguage) {
                toggleLanguage()
            }
        }

        // Crear libros predeterminados para id_default si no existen
        lifecycleScope.launch(Dispatchers.IO) {
            crearLibrosPredeterminadosSiNoExisten("id_default", language)
        }

        if ((isLoggedIn && userId != null) || (isLoginSkipped && userId != null)) {
            if(!userId.equals("id_default")){
                navigateToMainActivitySkip(userId)
            }else {
                navigateToMainActivity(userId)
            }
            return
        }

        bookContainer = findViewById(R.id.bookContainer)
        addPulsatingEffectToBorder()
        cargarLibrosYConfigurarAnimaciones(language)

        findViewById<View>(R.id.btn_skip_login).setOnClickListener {
            skipLogin()
        }

        val googleSignInButton = findViewById<View>(R.id.btn_google_sign_in)
        verificarTextoGoogleSignIn(googleSignInButton)

        googleSignInButton.setOnClickListener {
            signInWithGoogle()
        }
    }

    override fun onResume() {
        super.onResume()
        isChangingLanguage = false
        rlToggleLanguage.isEnabled = true
    }

    private fun verificarTextoGoogleSignIn(googleSignInButton: View) {
        if (googleSignInButton is SignInButton) {
            for (i in 0 until googleSignInButton.childCount) {
                val child = googleSignInButton.getChildAt(i)
                if (child is TextView) {
                    val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                    val language = prefs.getString(KEY_LANGUAGE, "es") ?: "es"

                    val customText = when (language) {
                        "en" -> "Login Session"
                        "es" -> "Iniciar Sesion"
                        else -> "Iniciar Sesion"
                    }

                    child.text = customText
                    Log.d("LoginActivity", "Texto actualizado del botón Google Sign-In: $customText")
                }
            }
        }
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
        val userId = "id_default"

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
    }

    private fun signInWithGoogle() {
        if (!::googleSignInClient.isInitialized) {
            Log.e("LoginActivity", "GoogleSignInClient no está inicializado.")
            Toast.makeText(this, getString(R.string.iniciarS), Toast.LENGTH_SHORT).show()
            return
        }

        val signInIntent = googleSignInClient.signInIntent
        startActivityForResult(signInIntent, RC_SIGN_IN)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == RC_SIGN_IN) {
            val task: Task<GoogleSignInAccount> = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                val account = task.getResult(ApiException::class.java)
                handleGoogleSignIn(account)
            } catch (e: ApiException) {
                Toast.makeText(this, getString(R.string.error_iniciar_sesion), Toast.LENGTH_SHORT)
                    .show()
                Log.e("LoginActivity", "Error al iniciar sesión: ${e.message}", e)
            }
        }
    }

    private fun handleGoogleSignIn(account: GoogleSignInAccount?) {
        if (account != null) {
            val credential = GoogleAuthProvider.getCredential(account.idToken, null)
            auth.signInWithCredential(credential).addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val firebaseUser = auth.currentUser
                    if (firebaseUser == null) {
                        Toast.makeText(
                            this,
                            getString(R.string.error_iniciar_sesion),
                            Toast.LENGTH_SHORT
                        ).show()
                        return@addOnCompleteListener
                    }

                    val userId = firebaseUser.uid
                    Log.d("LoginActivity", "User ID: $userId")

                    lifecycleScope.launch(Dispatchers.IO) {
                        // 1. Crear el nuevo usuario con userId (si no existe)
                        val usuarioExistente = usuarioDao.obtenerUsuarioPorId(userId)
                        if (usuarioExistente == null) {
                            val nuevoUsuario = Usuario(
                                id = userId,
                                nombre = firebaseUser.displayName,
                                email = firebaseUser.email,
                                esInvitado = false
                            )
                            usuarioDao.insertarUsuario(nuevoUsuario)
                        }

                        // 2. Actualizar referencias de libros y notas desde 'id_default' al nuevo userId
                        libroDao.actualizarLibrosIdDefault(userId)
                        notaDao.actualizarNotasIdDefault(userId)

                        // 3. Borrar el usuario 'id_default'
                        usuarioDao.borrarUsuario("id_default")

                        // Guardar datos actuales del nuevo usuario
                        val librosActuales = libroDao.obtenerLibrosPorUsuario(userId)
                        val notasActuales = notaDao.getAllNotasByUsuario(userId).getOrAwaitValue() ?: emptyList()

                        // 4. Borrar todos los registros locales para reiniciar IDs
                        notaDao.borrarTodasLasNotas()
                        libroDao.borrarTodosLosLibros()

                        // 5. Resetear secuencias para que IDs empiecen desde 1
                        libroDao.resetearSecuenciaLibros()
                        notaDao.resetearSecuenciaNotas()

                        // 6. Reinsertar los datos con id=0, de esta manera Room generará IDs desde 1
                        val librosReiniciados = librosActuales.map { it.copy(id = 0) }
                        val notasReiniciadas = notasActuales.map { it.copy(id = 0) }

                        libroDao.insertLibros(librosReiniciados)
                        notaDao.insertarNotas(notasReiniciadas)

                        // Verificación: Contar los libros guardados
                        val countLibros = libroDao.getCountByUsuario(userId)
                        Log.d("LoginActivity", "Libros guardados para $userId: $countLibros")

                        // 7. Sincronizar con Firestore
                        sincronizarDatosConFirestore(userId)


                    }
                } else {
                    Log.e(
                        "LoginActivity",
                        "Error al iniciar sesión con Google: ${task.exception?.message}"
                    )
                    Toast.makeText(
                        this,
                        getString(R.string.error_iniciar_sesion),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        } else {
            Log.e("LoginActivity", "Cuenta de Google nula.")
            Toast.makeText(this, getString(R.string.error_iniciar_sesion), Toast.LENGTH_SHORT).show()
        }
    }


    private suspend fun sincronizarDatosConFirestore(userId: String) {
        try {
            Log.d("sincronizarDatosConFirestore", "Iniciando sincronización para el usuario: $userId")

            val localUsuario = usuarioDao.obtenerUsuarioPorId(userId)
            val localLibros = libroDao.obtenerLibrosPorUsuario(userId)
            val localNotas = notaDao.getAllNotasByUsuario(userId).getOrAwaitValue() ?: emptyList()

            Log.d(
                "sincronizarDatosConFirestore",
                "Datos locales obtenidos: usuario=${localUsuario?.nombre}, libros=${localLibros.size}, notas=${localNotas.size}"
            )

            if (localUsuario == null) {
                Log.e("sincronizarDatosConFirestore", "Usuario local es null. Abortando sincronización.")
                return
            }

            withContext(Dispatchers.IO) {
                val userDocRef = firestore.collection("users").document(userId)
                val userSnapshot = userDocRef.get().await()

                if (!userSnapshot.exists()) {
                    Log.d(
                        "sincronizarDatosConFirestore",
                        "No existen datos en Firestore. Subiendo datos locales a la nube..."
                    )

                    val usuarioMap = hashMapOf(
                        "id" to localUsuario.id,
                        "nombre" to localUsuario.nombre,
                        "email" to localUsuario.email,
                        "esInvitado" to localUsuario.esInvitado
                    )

                    userDocRef.set(usuarioMap, SetOptions.merge()).await()

                    localLibros.forEach { libro ->
                        val libroDocRef = userDocRef.collection("libros").document(libro.id.toString())
                        val libroMap = hashMapOf(
                            "id" to libro.id,
                            "nombreLibro" to libro.nombreLibro,
                            "nombreSaga" to libro.nombreSaga,
                            "nombrePortada" to libro.nombrePortada,
                            "progreso" to libro.progreso,
                            "totalPaginas" to libro.totalPaginas,
                            "inicialSaga" to libro.inicialSaga,
                            "sinopsis" to libro.sinopsis,
                            "valoracion" to libro.valoracion,
                            "numeroNotas" to libro.numeroNotas,
                            "empezarLeer" to libro.empezarLeer,
                            "userId" to libro.userId
                        )
                        libroDocRef.set(libroMap, SetOptions.merge()).await()
                    }

                    localNotas.forEach { nota ->
                        val notaDocRef = userDocRef.collection("notas").document(nota.id.toString())
                        val notaMap = hashMapOf(
                            "id" to nota.id,
                            "contenido" to nota.contenido,
                            "userId" to nota.userId
                        )
                        notaDocRef.set(notaMap, SetOptions.merge()).await()
                    }

                    Log.d(
                        "sincronizarDatosConFirestore",
                        "Datos locales subidos correctamente a Firestore."
                    )
                    saveUserSession(userId)
                    withContext(Dispatchers.Main) {
                        navigateToMainActivity(userId)
                    }
                } else {
                    Log.d(
                        "sincronizarDatosConFirestore",
                        "Existen datos en Firestore. Comparando y sincronizando..."
                    )

                    val usuarioFirestore = userSnapshot.getString("nombre")?.let {
                        Usuario(
                            id = userSnapshot.getString("id") ?: "",
                            nombre = it,
                            email = userSnapshot.getString("email"),
                            esInvitado = userSnapshot.getBoolean("esInvitado") ?: false
                        )
                    }

                    val librosFirestoreSnapshot = userDocRef.collection("libros").get().await()
                    val librosFirestore = librosFirestoreSnapshot.documents.mapNotNull { doc ->
                        doc.toObject(Libro::class.java)
                    }

                    val notasFirestoreSnapshot = userDocRef.collection("notas").get().await()
                    val notasFirestore = notasFirestoreSnapshot.documents.mapNotNull { doc ->
                        doc.toObject(Nota::class.java)
                    }

                    Log.d(
                        "sincronizarDatosConFirestore",
                        "Datos de Firestore: usuario=${usuarioFirestore?.nombre}, libros=${librosFirestore.size}, notas=${notasFirestore.size}"
                    )

                    val datosCoincidenLocal = datosCoinciden(localUsuario, usuarioFirestore)
                    val librosCoinciden = listasCoinciden(localLibros, librosFirestore)
                    val notasCoinciden = listasCoinciden(localNotas, notasFirestore)

                    Log.d(
                        "sincronizarDatosConFirestore",
                        "Coinciden datos: usuario=$datosCoincidenLocal, libros=$librosCoinciden, notas=$notasCoinciden"
                    )

                    if (!datosCoincidenLocal || !librosCoinciden || !notasCoinciden) {
                        Log.d(
                            "sincronizarDatosConFirestore",
                            "Los datos no coinciden. Sobrescribiendo datos locales con los de Firestore..."
                        )
                        sobrescribirDatosLocales(
                            userId,
                            usuarioFirestore,
                            librosFirestore,
                            notasFirestore
                        )
                    } else {
                        Log.d(
                            "sincronizarDatosConFirestore",
                            "Los datos coinciden. Guardando sesión..."
                        )
                        saveUserSession(userId)
                        withContext(Dispatchers.Main) {
                            navigateToMainActivity(userId)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(
                "sincronizarDatosConFirestore",
                "Error al sincronizar con Firestore: ${e.message}",
                e
            )
            withContext(Dispatchers.Main) {
                Toast.makeText(
                    this@LoginActivity,
                    getString(R.string.error_sincronizacion),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private suspend fun sobrescribirDatosLocales(
        userId: String,
        usuarioFirebase: Usuario?,
        librosFirebase: List<Libro>,
        notasFirebase: List<Nota>
    ) {
        try {
            if (usuarioFirebase != null) {
                usuarioDao.insertarUsuario(usuarioFirebase)
            }

            // Borramos datos locales, reseteamos secuencias y luego insertamos datos de Firestore
            libroDao.borrarTodosLosLibros()
            notaDao.borrarTodasLasNotas()

            libroDao.resetearSecuenciaLibros()
            notaDao.resetearSecuenciaNotas()

            libroDao.insertLibros(librosFirebase)
            notaDao.insertarNotas(notasFirebase)

            Log.d("LoginActivity", "Datos locales sobrescritos con los de Firestore.")
            saveUserSession(userId)
            withContext(Dispatchers.Main) {
                navigateToMainActivity(userId)
            }
        } catch (e: Exception) {
            Log.e("LoginActivity", "Error al sobrescribir datos locales: ${e.message}", e)
        }
    }

    private fun datosCoinciden(local: Usuario?, remoto: Usuario?): Boolean {
        if (local == null && remoto == null) return true
        if (local == null || remoto == null) return false
        return local.nombre == remoto.nombre &&
                local.email == remoto.email &&
                local.esInvitado == remoto.esInvitado
    }

    private fun <T> listasCoinciden(localList: List<T>, remoteList: List<T>): Boolean {
        if (localList.size != remoteList.size) return false
        return localList.toSet() == remoteList.toSet()
    }

    private fun navigateToMainActivity(userId: String) {
        val intent = Intent(this, MapaInteractivoActivity::class.java).apply {
            putExtra("USER_ID", userId)
        }
        startActivity(intent)
        finish()
    }

    private fun navigateToMainActivitySkip(userId: String) {
        lifecycleScope.launch {
            Log.d("LoginActivity", "SKIP con el usuario con id ${userId}")
            sincronizarDatosConFirestore(userId)
            // Una vez completada la sincronización, navegas a MainActivity
            val intent = Intent(this@LoginActivity, MapaInteractivoActivity::class.java).apply {
                putExtra("USER_ID", userId)
            }
            startActivity(intent)
            finish()
        }
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
            val libros = jsonHandler.cargarLibrosDesdeJson(languageCode)
            if (libros.isNotEmpty()) {
                withContext(Dispatchers.Main) {
                    setupBookAnimations(libros)
                }
            } else {
                withContext(Dispatchers.Main) {
                    Log.e(
                        "LoginActivity",
                        "No se cargaron libros desde el JSON para el idioma: $languageCode"
                    )
                    Toast.makeText(
                        this@LoginActivity,
                        getString(R.string.error_cargar_libros),
                        Toast.LENGTH_SHORT
                    ).show()
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
                    val drawableId =
                        libro.nombrePortada?.let { obtenerDrawablePorNombre(it) } ?: 0

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
                        Log.e(
                            "LoginActivity",
                            "No se encontró la portada: ${libro.nombrePortada}"
                        )
                        bookCover.setImageResource(R.drawable.portada_elcamino)
                        bookCover.visibility = View.VISIBLE
                    }
                }
            } else {
                Log.e("LoginActivity", "Dimensiones del contenedor no válidas")
                Toast.makeText(
                    this@LoginActivity,
                    getString(R.string.error_cargar_libros),
                    Toast.LENGTH_SHORT
                ).show()
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
            Log.e(
                "DrawableVerification",
                "Drawable no encontrado para: $nombre. Usando portada por defecto."
            )
            return R.drawable.portada_elcamino
        }
        return drawableId
    }

    private suspend fun crearLibrosPredeterminadosSiNoExisten(
        userId: String,
        languageCode: String = "es"
    ) {
        val usuarioExistente = usuarioDao.obtenerUsuarioPorId(userId)
        if (usuarioExistente == null) {
            val nuevoUsuario = Usuario(
                id = userId,
                nombre = if (userId == "id_default") "Invitado" else "Usuario",
                email = if (userId == "id_default") null else "email@example.com",
                esInvitado = userId == "id_default"
            )
            usuarioDao.insertarUsuario(nuevoUsuario)
            Log.d("LoginActivity", "Usuario creado: $userId")
        }

        val librosExistentes = libroDao.obtenerLibrosPorUsuario(userId)
        if (librosExistentes.isEmpty()) {
            if (userId == "id_default") {
                val librosDesdeJson = jsonHandler.cargarLibrosDesdeJson(languageCode, userId)
                libroDao.insertLibros(librosDesdeJson)
                Log.d("LoginActivity", "Libros predeterminados creados para id_default.")
            } else {
                val librosDefault = libroDao.obtenerLibrosPorUsuario("id_default")
                if (librosDefault.isNotEmpty()) {
                    val librosCopia = librosDefault.map { libro ->
                        libro.copy(id = 0, userId = userId) // Muy importante: id = 0
                    }
                    libroDao.insertLibros(librosCopia)

                    // Borramos todos los libros del usuario actual (si hubiera) y reiniciamos
                    libroDao.borrarTodosLosLibros()
                    notaDao.borrarTodasLasNotas()
                    libroDao.resetearSecuenciaLibros()
                    notaDao.resetearSecuenciaNotas()

                    libroDao.insertLibros(librosCopia)
                    Log.d(
                        "LoginActivity",
                        "Libros copiados desde id_default para el usuario: $userId"
                    )
                } else {
                    val librosDesdeJson = jsonHandler.cargarLibrosDesdeJson(languageCode, userId)
                    libroDao.insertLibros(librosDesdeJson)
                    Log.d(
                        "LoginActivity",
                        "Libros predeterminados creados desde JSON para el usuario: $userId"
                    )
                }
            }
        } else {
            Log.d("LoginActivity", "El usuario $userId ya tiene libros en la base de datos.")
        }
    }

    private suspend fun <T> LiveData<T>.getOrAwaitValue(): T {
        return withContext(Dispatchers.Main) {
            suspendCancellableCoroutine<T> { continuation ->
                val observer = object : Observer<T> {
                    override fun onChanged(value: T) {
                        removeObserver(this)
                        continuation.resume(value)
                    }
                }

                observeForever(observer)

                continuation.invokeOnCancellation {
                    removeObserver(observer)
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

}
