package com.example.appsandersonsm.Repositorio

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteException
import android.database.sqlite.SQLiteOpenHelper
import com.example.appsandersonsm.Modelo.Libro
import com.example.appsandersonsm.Repositorio.JsonHandler

class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "libros.db"
        private const val DATABASE_VERSION = 2 // Incrementado de 1 a 2

        // Tabla libros
        const val TABLE_LIBROS = "libros"
        const val COLUMN_ID = "id"
        const val COLUMN_NOMBRE_LIBRO = "nombreLibro"
        const val COLUMN_NOMBRE_SAGA = "nombreSaga"
        const val COLUMN_NOMBRE_PORTADA = "nombrePortada"
        const val COLUMN_PROGRESO = "progreso"
        const val COLUMN_TOTAL_PAGINAS = "totalPaginas"
        const val COLUMN_INICIAL_SAGA = "inicialSaga"

        // Tabla notas
        const val TABLE_NOTAS = "notas"
        const val COLUMN_NOTA_ID = "id"
        const val COLUMN_LIBRO_ID = "libro_id"
        const val COLUMN_CONTENIDO = "contenido"
        const val COLUMN_FECHA_CREACION = "fecha_creacion"
        const val COLUMN_FECHA_MODIFICACION = "fecha_modificacion"
    }

    private val jsonHandler = JsonHandler(context)

    override fun onCreate(db: SQLiteDatabase?) {
        val CREATE_LIBROS_TABLE = """
            CREATE TABLE $TABLE_LIBROS (
                $COLUMN_ID INTEGER PRIMARY KEY,
                $COLUMN_NOMBRE_LIBRO TEXT,
                $COLUMN_NOMBRE_SAGA TEXT,
                $COLUMN_NOMBRE_PORTADA TEXT,
                $COLUMN_PROGRESO INTEGER,
                $COLUMN_TOTAL_PAGINAS INTEGER,
                $COLUMN_INICIAL_SAGA INTEGER DEFAULT 0
            );
        """.trimIndent()

        val CREATE_NOTAS_TABLE = """
            CREATE TABLE $TABLE_NOTAS (
                $COLUMN_NOTA_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_LIBRO_ID INTEGER NOT NULL,
                $COLUMN_CONTENIDO TEXT NOT NULL,
                $COLUMN_FECHA_CREACION TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                $COLUMN_FECHA_MODIFICACION TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                FOREIGN KEY ($COLUMN_LIBRO_ID) REFERENCES $TABLE_LIBROS($COLUMN_ID) ON DELETE CASCADE
            );
        """.trimIndent()

        db?.execSQL(CREATE_LIBROS_TABLE)
        db?.execSQL(CREATE_NOTAS_TABLE)
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) {
            val CREATE_NOTAS_TABLE = """
                CREATE TABLE $TABLE_NOTAS (
                    $COLUMN_NOTA_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                    $COLUMN_LIBRO_ID INTEGER NOT NULL,
                    $COLUMN_CONTENIDO TEXT NOT NULL,
                    $COLUMN_FECHA_CREACION TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    $COLUMN_FECHA_MODIFICACION TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    FOREIGN KEY ($COLUMN_LIBRO_ID) REFERENCES $TABLE_LIBROS($COLUMN_ID) ON DELETE CASCADE
                );
            """.trimIndent()
            db?.execSQL(CREATE_NOTAS_TABLE)
        }
    }

    fun cargarDatosInicialesDesdeJson() {
        if (isDatabaseEmpty()) {
            jsonHandler.copiarJsonDesdeAssetsSiNoExiste()
            val libros = jsonHandler.cargarLibrosDesdeJson()
            insertarLibros(libros)
        }
    }

    private fun isDatabaseEmpty(): Boolean {
        val db = this.readableDatabase
        var cursor: Cursor? = null
        return try {
            cursor = db.rawQuery("SELECT COUNT(*) FROM $TABLE_LIBROS", null)
            cursor.moveToFirst() && cursor.getInt(0) == 0
        } finally {
            cursor?.close()
            db.close()
        }
    }

    fun agregarNota(libroId: Int, contenido: String): Long {
        if (contenido.isBlank()) throw IllegalArgumentException("El contenido no puede estar vacío.")

        val db = this.writableDatabase
        return try {
            val values = ContentValues().apply {
                put(COLUMN_LIBRO_ID, libroId)
                put(COLUMN_CONTENIDO, contenido)
            }
            db.insert(TABLE_NOTAS, null, values)
        } catch (e: SQLiteException) {
            e.printStackTrace()
            -1
        } finally {
            db.close()
        }
    }

    fun obtenerNotasPorLibro(libroId: Int): List<Map<String, Any>> {
        val notas = mutableListOf<Map<String, Any>>()
        val db = this.readableDatabase
        var cursor: Cursor? = null
        try {
            cursor = db.query(
                TABLE_NOTAS,
                null,
                "$COLUMN_LIBRO_ID = ?",
                arrayOf(libroId.toString()),
                null,
                null,
                "$COLUMN_FECHA_CREACION ASC"
            )

            if (cursor.moveToFirst()) {
                do {
                    val nota = mapOf(
                        COLUMN_NOTA_ID to cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_NOTA_ID)),
                        COLUMN_LIBRO_ID to cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_LIBRO_ID)),
                        COLUMN_CONTENIDO to cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CONTENIDO)),
                        COLUMN_FECHA_CREACION to cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_FECHA_CREACION)),
                        COLUMN_FECHA_MODIFICACION to cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_FECHA_MODIFICACION))
                    )
                    notas.add(nota)
                } while (cursor.moveToNext())
            }
        } finally {
            cursor?.close()
            db.close()
        }
        return notas
    }

    fun actualizarNota(notaId: Int, nuevoContenido: String): Int {
        if (nuevoContenido.isBlank()) throw IllegalArgumentException("El contenido no puede estar vacío.")

        val db = this.writableDatabase
        return try {
            val values = ContentValues().apply {
                put(COLUMN_CONTENIDO, nuevoContenido)
                put(COLUMN_FECHA_MODIFICACION, "CURRENT_TIMESTAMP")
            }
            db.update(TABLE_NOTAS, values, "$COLUMN_NOTA_ID = ?", arrayOf(notaId.toString()))
        } catch (e: SQLiteException) {
            e.printStackTrace()
            0
        } finally {
            db.close()
        }
    }

    fun eliminarNota(notaId: Int): Int {
        val db = this.writableDatabase
        return try {
            db.delete(TABLE_NOTAS, "$COLUMN_NOTA_ID = ?", arrayOf(notaId.toString()))
        } catch (e: SQLiteException) {
            e.printStackTrace()
            0
        } finally {
            db.close()
        }
    }

    private fun insertarLibros(libros: List<Libro>) {
        val db = this.writableDatabase
        try {
            libros.forEach { libro ->
                val values = ContentValues().apply {
                    put(COLUMN_ID, libro.id)
                    put(COLUMN_NOMBRE_LIBRO, libro.nombreLibro)
                    put(COLUMN_NOMBRE_SAGA, libro.nombreSaga)
                    put(COLUMN_NOMBRE_PORTADA, libro.nombrePortada)
                    put(COLUMN_PROGRESO, libro.progreso)
                    put(COLUMN_TOTAL_PAGINAS, libro.totalPaginas)
                    put(COLUMN_INICIAL_SAGA, if (libro.inicialSaga) 1 else 0)
                }
                db.insert(TABLE_LIBROS, null, values)
            }
        } finally {
            db.close()
        }
    }

    fun actualizarProgresoLibro(libro: Libro) {
        val db = this.writableDatabase
        try {
            val values = ContentValues().apply {
                put(COLUMN_PROGRESO, libro.progreso)
                put(COLUMN_TOTAL_PAGINAS, libro.totalPaginas)
            }
            db.update(TABLE_LIBROS, values, "$COLUMN_ID = ?", arrayOf(libro.id.toString()))
        } finally {
            db.close()
        }
    }

    fun getLibroById(id: Int): Libro? {
        val db = this.readableDatabase
        var cursor: Cursor? = null
        return try {
            cursor = db.query(
                TABLE_LIBROS,
                arrayOf(
                    COLUMN_ID, COLUMN_NOMBRE_LIBRO, COLUMN_NOMBRE_SAGA,
                    COLUMN_NOMBRE_PORTADA, COLUMN_PROGRESO, COLUMN_TOTAL_PAGINAS, COLUMN_INICIAL_SAGA
                ),
                "$COLUMN_ID = ?",
                arrayOf(id.toString()),
                null,
                null,
                null
            )

            if (cursor.moveToFirst()) {
                Libro(
                    id = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ID)),
                    nombreLibro = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_NOMBRE_LIBRO)),
                    nombreSaga = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_NOMBRE_SAGA)),
                    nombrePortada = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_NOMBRE_PORTADA)),
                    progreso = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_PROGRESO)),
                    totalPaginas = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_TOTAL_PAGINAS)),
                    inicialSaga = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_INICIAL_SAGA)) > 0
                )
            } else null
        } finally {
            cursor?.close()
            db.close()
        }
    }

    fun getAllSagas(): List<String> {
        val sagas = mutableListOf<String>()
        val db = this.readableDatabase
        var cursor: Cursor? = null
        try {
            cursor = db.rawQuery("SELECT DISTINCT $COLUMN_NOMBRE_SAGA FROM $TABLE_LIBROS", null)
            if (cursor.moveToFirst()) {
                do {
                    val saga = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_NOMBRE_SAGA))
                    sagas.add(saga)
                } while (cursor.moveToNext())
            }
        } finally {
            cursor?.close()
            db.close()
        }
        return sagas
    }

    fun getAllLibros(): List<Libro> {
        val libros = mutableListOf<Libro>()
        val db = this.readableDatabase
        var cursor: Cursor? = null
        try {
            cursor = db.rawQuery("SELECT * FROM $TABLE_LIBROS", null)
            if (cursor.moveToFirst()) {
                do {
                    val libro = Libro(
                        id = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ID)),
                        nombreLibro = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_NOMBRE_LIBRO)),
                        nombreSaga = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_NOMBRE_SAGA)),
                        nombrePortada = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_NOMBRE_PORTADA)),
                        progreso = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_PROGRESO)),
                        totalPaginas = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_TOTAL_PAGINAS)),
                        inicialSaga = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_INICIAL_SAGA)) > 0
                    )
                    libros.add(libro)
                } while (cursor.moveToNext())
            }
        } finally {
            cursor?.close()
            db.close()
        }
        return libros
    }

    fun getLibrosBySagaId(nombreSaga: String): List<Libro> {
        val libros = mutableListOf<Libro>()
        val db = this.readableDatabase
        var cursor: Cursor? = null
        try {
            cursor = db.query(
                TABLE_LIBROS,
                arrayOf(
                    COLUMN_ID, COLUMN_NOMBRE_LIBRO, COLUMN_NOMBRE_SAGA,
                    COLUMN_NOMBRE_PORTADA, COLUMN_PROGRESO, COLUMN_TOTAL_PAGINAS, COLUMN_INICIAL_SAGA
                ),
                "$COLUMN_NOMBRE_SAGA = ?",
                arrayOf(nombreSaga),
                null,
                null,
                null
            )

            if (cursor.moveToFirst()) {
                do {
                    val libro = Libro(
                        id = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ID)),
                        nombreLibro = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_NOMBRE_LIBRO)),
                        nombreSaga = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_NOMBRE_SAGA)),
                        nombrePortada = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_NOMBRE_PORTADA)),
                        progreso = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_PROGRESO)),
                        totalPaginas = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_TOTAL_PAGINAS)),
                        inicialSaga = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_INICIAL_SAGA)) > 0
                    )
                    libros.add(libro)
                } while (cursor.moveToNext())
            }
        } finally {
            cursor?.close()
            db.close()
        }
        return libros
    }
}
