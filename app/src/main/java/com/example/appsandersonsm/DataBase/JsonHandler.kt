// JsonHandler.kt
package com.example.appsandersonsm.Utils

import android.content.Context
import com.example.appsandersonsm.Modelo.Libro
import org.json.JSONArray
import java.io.IOException

class JsonHandler(private val context: Context) {

    private val fileName = "libros.json"

    // Lee y carga los datos desde el archivo JSON en assets
    fun cargarLibrosDesdeJson(): List<Libro> {
        val libros = mutableListOf<Libro>()
        val jsonString = leerJsonDesdeAssets()

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
                        numeroNotas = jsonObject.optInt("numeroNotas", 0),
                        empezarLeer = jsonObject.optBoolean("empezarLeer", false)
                    )

                    libros.add(libro)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return libros
    }

    // Lee el contenido del archivo JSON desde assets
    private fun leerJsonDesdeAssets(): String? {
        return try {
            val inputStream = context.assets.open("libros.json")
            val size = inputStream.available()
            val buffer = ByteArray(size)
            inputStream.read(buffer)
            inputStream.close()
            String(buffer, Charsets.UTF_8)
        } catch (ex: Exception) {
            ex.printStackTrace()
            null
        }
    }
}
