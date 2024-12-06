package com.example.appsandersonsm

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Intent
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewTreeObserver
import android.view.animation.DecelerateInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.appsandersonsm.DataBase.JsonHandler
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import java.util.Random

class LoginActivity : AppCompatActivity() {

    private lateinit var bookContainer: FrameLayout
    private lateinit var googleSignInClient: GoogleSignInClient

    companion object {
        private const val RC_SIGN_IN = 9001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val sharedPreferences = getSharedPreferences("AppPreferences", MODE_PRIVATE)
        val isLoginSkipped = sharedPreferences.getBoolean("isLoginSkipped", false)

        if (isLoginSkipped) {
            Toast.makeText(this, "Has iniciado sesión como Invitado", Toast.LENGTH_SHORT).show()
            val intent = Intent(this, MapaInteractivoActivity::class.java) // Cambia esta actividad si es necesario
            startActivity(intent)
            finish() // Finaliza LoginActivity
            return
        }

        setContentView(R.layout.activity_login)

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

    private fun skipLogin() {
        // Guardar en SharedPreferences que el usuario ha omitido el inicio de sesión
        val sharedPreferences = getSharedPreferences("AppPreferences", MODE_PRIVATE)
        sharedPreferences.edit().putBoolean("isLoginSkipped", true).apply()

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
                            ?:null

                        if (drawableId != 0) {
                            if (drawableId != null) {
                                bookCover.setImageResource(drawableId)
                            }
                            val params = FrameLayout.LayoutParams(200, 300)
                            bookCover.layoutParams = params
                            bookCover.visibility = View.INVISIBLE
                            bookCover.setBackgroundResource(R.drawable.book_init_border)
                            bookCover.setPadding(2, 2, 2,2) // Ajusta los valores para dar espacio al marco
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

        val valorA = (Random().nextFloat() * 0.5f) + 0.2f
        val startY = containerHeight
        val endY = containerHeight * valorA
        val bound = (containerWidth - 200).toInt()
        val startX = if (bound > 0) Random().nextInt(bound).toFloat() else 0f

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
        animatorSet.startDelay = (Random().nextInt(3500) + 2000).toLong() // Inicio de las animaciones - Retraso aleatorio entre 1 y 3 segundos
        animatorSet.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationStart(animation: Animator) {
                bookCover.visibility = View.VISIBLE
            }

            override fun onAnimationEnd(animation: Animator) {
                // Reinicia la posición y la alpha
                val validBound = (containerWidth - 200).coerceAtLeast(0f).toInt()
                val startX = if (validBound > 0) Random().nextInt(validBound).toFloat() else 0f
                bookCover.x = startX
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
                Toast.makeText(this, "Error al iniciar sesión: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun handleSignInResult(account: GoogleSignInAccount?) {
        if (account != null) {
            val welcomeMessage = "Bienvenido, ${account.displayName}"
            Toast.makeText(this, welcomeMessage, Toast.LENGTH_SHORT).show()
            val intent = Intent(this, MapaInteractivoActivity::class.java)
            startActivity(intent)
            finish()
        }
    }
}
