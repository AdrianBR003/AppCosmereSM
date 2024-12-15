package com.example.appsandersonsm

import android.app.Application
import android.util.Log
import androidx.room.Room
import com.example.appsandersonsm.Dao.LibroDao
import com.example.appsandersonsm.Dao.NotaDao
import com.example.appsandersonsm.Dao.UsuarioDao
import com.example.appsandersonsm.DataBase.AppDatabase
import com.example.appsandersonsm.Repository.LibroRepository
import com.example.appsandersonsm.Repository.NotaRepository
import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob

// InitApplication.kt
class InitApplication : Application() {

    // Instancia de la base de datos
    val appDatabase: AppDatabase by lazy {
        Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "app_database"
        )
            .fallbackToDestructiveMigration()
            .build()
    }

    // DAOs
    val libroDao: LibroDao by lazy { appDatabase.libroDao() }
    val notaDao: NotaDao by lazy { appDatabase.notaDao() }
    val usuarioDao: UsuarioDao by lazy { appDatabase.usuarioDao() }

    // Repositorios
    val libroRepository: LibroRepository by lazy { LibroRepository(libroDao, notaDao) }
    val notaRepository: NotaRepository by lazy { NotaRepository(notaDao) }

    // userId inicializado desde SharedPreferences
    val userId: String by lazy {
        val sharedPreferences = getSharedPreferences("AppPreferences", MODE_PRIVATE)
        sharedPreferences.getString("USER_ID", "") ?: ""
    }

    override fun onCreate() {
        super.onCreate()

        FirebaseApp.initializeApp(this)
        Log.d("InitApplication", "InitApplication se inicializó correctamente")
    }

    fun setUserId(id: String) {
        // Método para actualizar el userId si es necesario
        // Este método puede ser llamado desde actividades donde se obtiene el userId
        // Considera sincronizar si accedes desde múltiples hilos
    }
}
