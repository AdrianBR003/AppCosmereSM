package com.example.appsandersonsm.DataBase

import android.content.Context
import android.util.Log
import androidx.room.*
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.appsandersonsm.Dao.LibroDao
import com.example.appsandersonsm.Dao.NotaDao
import com.example.appsandersonsm.Dao.UsuarioDao
import com.example.appsandersonsm.InitApplication
import com.example.appsandersonsm.Modelo.Libro
import com.example.appsandersonsm.Modelo.Nota
import com.example.appsandersonsm.Modelo.Usuario
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(entities = [Libro::class, Nota::class, Usuario::class], version = 3, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

    abstract fun usuarioDao(): UsuarioDao
    abstract fun libroDao(): LibroDao
    abstract fun notaDao(): NotaDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null



        fun getDatabase(context: Context, scope: CoroutineScope): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "app_database"
                ).build()
                Log.d("RoomDatabase", "Base de datos creada con éxito")
                INSTANCE = instance
                instance
            }
        }
    }

    private class AppDatabaseCallback(
        private val context: Context,
        private val scope: CoroutineScope,
        private val userId: String
    ) : RoomDatabase.Callback() {

        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            INSTANCE?.let { database ->
                scope.launch(Dispatchers.IO) {
                    populateDatabase(database.libroDao(), context, userId)
                }
            }
        }


        private suspend fun populateDatabase(libroDao: LibroDao, context: Context, userId: String) {
            // Carga datos desde un archivo JSON en el idioma predeterminado (español)
            val jsonHandler = JsonHandler(context, libroDao)
            jsonHandler.cargarDatosIniciales(userId) // Pasar userId al cargar los datos
            Log.d("AppDatabase", "Datos iniciales cargados para userId: $userId")
        }
    }
}
