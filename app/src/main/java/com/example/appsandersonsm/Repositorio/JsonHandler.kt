package com.example.appsandersonsm.Repositorio

import android.content.Context
import com.example.appsandersonsm.Modelo.Libro
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

class JsonHandler(private val context: Context) {

    private val fileName = "libros.json"

    // Copia el archivo JSON desde assets a almacenamiento interno si no existe
    fun copiarJsonDesdeAssetsSiNoExiste() {
        val archivoDestino = File(context.filesDir, fileName)
        if (!archivoDestino.exists()) {
            context.assets.open(fileName).use { inputStream ->
                FileOutputStream(archivoDestino).use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
        }
    }

    // Lee y carga los datos desde el archivo JSON en almacenamiento interno
    fun cargarLibrosDesdeJson(): List<Libro> {
        val libros = mutableListOf<Libro>()
        val jsonString = leerJsonDesdeAlmacenamientoInterno()

        if (jsonString != null) {
            val jsonArray = JSONArray(jsonString)
            for (i in 0 until jsonArray.length()) {
                val jsonObject = jsonArray.getJSONObject(i)
                val libro = Libro(
                    id = jsonObject.getInt("id"),
                    nombreLibro = jsonObject.getString("nombreLibro"),
                    nombreSaga = jsonObject.getString("nombreSaga"),
                    nombrePortada = jsonObject.getString("nombrePortada"),
                    progreso = jsonObject.optInt("progreso", 0),
                    totalPaginas = jsonObject.optInt("totalPaginas", 100), // Usar optInt para totalPaginas con valor predeterminado
                    inicialSaga = jsonObject.optBoolean("inicialSaga", false)
                )
                libros.add(libro)
            }
        }
        return libros
    }



    // Guarda una lista de libros en el archivo JSON
    fun guardarLibrosEnJson(libros: List<Libro>) {
        val jsonArray = JSONArray()
        libros.forEach { libro ->
            val jsonObject = JSONObject().apply {
                put("id", libro.id)
                put("nombreLibro", libro.nombreLibro)
                put("nombreSaga", libro.nombreSaga)
                put("nombrePortada", libro.nombrePortada)
                put("progreso", libro.progreso)
                put("inicialSaga", libro.inicialSaga)
            }
            jsonArray.put(jsonObject)
        }
        escribirJsonEnAlmacenamientoInterno(jsonArray.toString())
    }

    // Lee el contenido del archivo JSON desde almacenamiento interno
    private fun leerJsonDesdeAlmacenamientoInterno(): String? {
        val file = File(context.filesDir, fileName)
        return if (file.exists()) {
            file.bufferedReader().use { it.readText() }
        } else {
            null
        }
    }

    // Escribe el JSON en el almacenamiento interno
    private fun escribirJsonEnAlmacenamientoInterno(jsonString: String) {
        val file = File(context.filesDir, fileName)
        file.writeText(jsonString)
    }
}
