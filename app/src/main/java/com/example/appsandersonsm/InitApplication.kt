package com.example.appsandersonsm

import android.app.Application
import android.util.Log
import com.example.appsandersonsm.DataBase.AppDatabase
import com.example.appsandersonsm.Repository.LibroRepository
import com.example.appsandersonsm.Repository.NotaRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob


class InitApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        Log.d("InitApplication", "InitApplication se inicializó correctamente")
    }

    // Scope para operaciones asincrónicas
    private val applicationScope = CoroutineScope(SupervisorJob())

    // Instancia de la base de datos
    val database: AppDatabase by lazy { AppDatabase.getDatabase(this, applicationScope) }

    // Repositorios
    val libroRepository: LibroRepository by lazy { LibroRepository(database.libroDao()) }
    val notaRepository: NotaRepository by lazy { NotaRepository(database.notaDao()) }
}
