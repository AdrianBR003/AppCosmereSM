package com.example.appsandersonsm.DataBase

import android.content.Context
import android.util.Log
import androidx.room.*
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.appsandersonsm.Dao.LibroDao
import com.example.appsandersonsm.Dao.NotaDao
import com.example.appsandersonsm.Modelo.Libro
import com.example.appsandersonsm.Modelo.Nota
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(entities = [Libro::class, Nota::class], version = 2, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

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
                INSTANCE = instance
                instance
            }
        }
    }

    private class AppDatabaseCallback(
        private val context: Context,
        private val scope: CoroutineScope
    ) : RoomDatabase.Callback() {

        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            INSTANCE?.let { database ->
                scope.launch(Dispatchers.IO) {
                    populateDatabase(database.libroDao(), context)
                }
            }
        }

        private suspend fun populateDatabase(libroDao: LibroDao, context: Context) {
            // Carga datos desde un archivo JSON en el idioma predeterminado (español)
            val jsonHandler = JsonHandler(context, libroDao)
            jsonHandler.cargarDatosIniciales()
        }
    }
}
