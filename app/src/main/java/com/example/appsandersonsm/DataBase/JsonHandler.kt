package com.example.appsandersonsm.DataBase

import android.content.Context
import android.util.Log
import com.example.appsandersonsm.Dao.LibroDao
import com.example.appsandersonsm.Modelo.Libro
import org.json.JSONArray
import java.io.IOException

class JsonHandler(private val context: Context, private val libroDao: LibroDao) {

    private val fileNameEs = "libros.json"
    private val fileNameEn = "libros_en.json"

    // SobreCarga de Metodos
    fun cargarLibrosDesdeJson(languageCode: String): List<Libro> {
        val fileName = if (languageCode == "en") fileNameEn else fileNameEs
        val libros = mutableListOf<Libro>()
        val jsonString = leerJsonDesdeAssets(fileName)

        if (jsonString != null) {
            try {
                val jsonArray = JSONArray(jsonString)
                for (i in 0 until jsonArray.length()) {
                    val jsonObject = jsonArray.getJSONObject(i)
                    val libro = Libro(
                        id = 0, // Autogenerado por la base de datos
                        nombreLibro = jsonObject.getString("nombreLibro"),
                        nombreSaga = jsonObject.getString("nombreSaga"),
                        nombrePortada = jsonObject.getString("nombrePortada"),
                        progreso = jsonObject.optInt("progreso", 0),
                        totalPaginas = jsonObject.optInt("totalPaginas", 1500),
                        inicialSaga = jsonObject.optBoolean("inicialSaga", false),
                        sinopsis = jsonObject.optString("sinopsis", "Sinopsis no disponible"),
                        valoracion = jsonObject.optDouble("valoracion", 0.0).toFloat(),
                        numeroNotas = jsonObject.optInt("nNotas", 0),
                        empezarLeer = jsonObject.optBoolean("empezarLeer", false),
                        userId = "default", // Inicialmente vacío
                        leido = jsonObject.optBoolean("leido",false)
                    )
                    libros.add(libro)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Log.e("JsonHandler", "Error al parsear JSON: ${e.message}")
            }
        }
        return libros
    }


    fun cargarLibrosDesdeJson(languageCode: String, userId: String): List<Libro> {
        val fileName = if (languageCode == "en") fileNameEn else fileNameEs
        val libros = mutableListOf<Libro>()
        val jsonString = leerJsonDesdeAssets(fileName)

        if (jsonString != null) {
            try {
                val jsonArray = JSONArray(jsonString)
                for (i in 0 until jsonArray.length()) {
                    val jsonObject = jsonArray.getJSONObject(i)
                    val libro = Libro(
                        id = jsonObject.getInt("id"),
                        nombreLibro = jsonObject.getString("nombreLibro"),
                        nombreSaga = jsonObject.getString("nombreSaga"),
                        nombrePortada = jsonObject.getString("nombrePortada"),
                        progreso = jsonObject.optInt("progreso", 0),
                        totalPaginas = jsonObject.optInt("totalPaginas", 1500),
                        inicialSaga = jsonObject.optBoolean("inicialSaga", false),
                        sinopsis = jsonObject.optString("sinopsis", "Sinopsis no disponible"),
                        valoracion = jsonObject.optDouble("valoracion", 0.0).toFloat(),
                        numeroNotas = jsonObject.optInt("nNotas", 0),
                        empezarLeer = jsonObject.optBoolean("empezarLeer", false),
                        userId = userId, // Asociar con el usuario
                        leido = jsonObject.optBoolean("leido",false)
                    )
                    libros.add(libro)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Log.e("JsonHandler", "Error al parsear JSON: ${e.message}")
            }
        }
        return libros
    }

    /**
     * Carga los datos iniciales desde el archivo JSON en español para un usuario específico.
     */
    suspend fun cargarDatosIniciales(userId: String) {
        val jsonString = leerJsonDesdeAssets(fileNameEs)

        if (jsonString != null) {
            try {
                val jsonArray = JSONArray(jsonString)
                val libros = mutableListOf<Libro>()
                for (i in 0 until jsonArray.length()) {
                    val jsonObject = jsonArray.getJSONObject(i)
                    val libro = Libro(
                        id = jsonObject.getInt("id"),
                        nombreLibro = jsonObject.getString("nombreLibro"),
                        nombreSaga = jsonObject.getString("nombreSaga"),
                        nombrePortada = jsonObject.getString("nombrePortada"),
                        progreso = jsonObject.optInt("progreso", 0),
                        totalPaginas = jsonObject.optInt("totalPaginas", 1500),
                        inicialSaga = jsonObject.optBoolean("inicialSaga", false),
                        sinopsis = jsonObject.optString("sinopsis", "Sinopsis no disponible"),
                        valoracion = jsonObject.optDouble("valoracion", 0.0).toFloat(),
                        numeroNotas = jsonObject.optInt("nNotas", 0),
                        empezarLeer = jsonObject.optBoolean("empezarLeer", false),
                        userId = userId, // Asociar con el usuario
                        leido = jsonObject.optBoolean("leido",false)
                    )
                    libros.add(libro)
                }
                libroDao.insertLibros(libros)
                Log.d("JsonHandler", "Libros iniciales cargados para el usuario: $userId")
            } catch (e: Exception) {
                e.printStackTrace()
                Log.e("JsonHandler", "Error al parsear JSON inicial: ${e.message}")
            }
        } else {
            Log.e("JsonHandler", "JSON inicial no encontrado o vacío.")
        }
    }
    /**
     * Lee el contenido de un archivo JSON desde la carpeta assets.
     */
    private fun leerJsonDesdeAssets(fileName: String): String? {
        return try {
            val inputStream = context.assets.open(fileName)
            val size = inputStream.available()
            val buffer = ByteArray(size)
            inputStream.read(buffer)
            inputStream.close()
            String(buffer, Charsets.UTF_8)
        } catch (ex: IOException) {
            ex.printStackTrace()
            null
        }
    }
}
