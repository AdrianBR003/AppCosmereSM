package com.example.appsandersonsm

import android.app.Application
import android.util.Log
import com.example.appsandersonsm.DataBase.AppDatabase
import com.example.appsandersonsm.Repository.LibroRepository
import com.example.appsandersonsm.Repository.NotaRepository
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob


/**
 *  La clase InitApplication en Android es una clase base que se crea antes de cualquier actividad, servicio u otro componente de la aplicación.
 *  Se utiliza para mantener el estado global de la aplicación y para inicializar componentes que necesitan estar disponibles en to_do el
 *  ciclo de vida de la aplicación, como bases de datos, repositorios, dependencias de inyección, entre otros.
 */

class InitApplication : Application() {

    lateinit var userId: String
        private set


    override fun onCreate() {
        super.onCreate()

        val sharedPreferences = getSharedPreferences("AppPreferences", MODE_PRIVATE)
        userId = sharedPreferences.getString("USER_ID", "") ?: ""

        Log.d("InitApplication", "User ID cargado: $userId")

        // FireStore
        FirebaseFirestore.getInstance().also {
            Log.d("InitApplication", "FirebaseFirestore inicializado.")
        }
        Log.d("InitApplication", "InitApplication se inicializó correctamente")
    }

    // Scope para operaciones asincrónicas
    private val applicationScope = CoroutineScope(SupervisorJob())


    // Instancia de la base de datos
    val database: AppDatabase by lazy { AppDatabase.getDatabase(this, applicationScope) }

    // Repositorios
    val libroRepository: LibroRepository by lazy { LibroRepository(database.libroDao()) }
    val notaRepository: NotaRepository by lazy { NotaRepository(database.notaDao()) }

    fun getUserId(id: String):String {
        userId = id
        Log.d("InitApplication", "User ID configurado: $userId")
        return userId;
    }


}
