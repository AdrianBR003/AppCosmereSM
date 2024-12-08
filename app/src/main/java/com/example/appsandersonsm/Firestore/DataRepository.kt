package com.example.appsandersonsm.Firestore

import android.util.Log
import androidx.lifecycle.asFlow
import com.example.appsandersonsm.Dao.LibroDao
import com.example.appsandersonsm.Dao.NotaDao
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class DataRepository(
    private val libroDao: LibroDao,
    private val notaDao: NotaDao
) {
    private val firestore = FirebaseFirestore.getInstance()

    /**
     * Sincroniza los datos descargados de Firestore a Room y luego sube los datos locales a Firestore.
     */
    suspend fun synchronizeData(userId: String) {
        val userDocRef = firestore.collection("usuarios").document(userId)

        try {
            // Descargar Libros desde Firestore
            val librosSnapshot = userDocRef.collection("libros").get().await()
            Log.d("DataRepository", "Libros descargados desde Firestore: ${librosSnapshot.size()} documentos.")
            val libros = librosSnapshot.documents.mapNotNull { doc ->
                val id = doc.id.toIntOrNull()
                val libroFirestore = doc.toObject(LibroFirestore::class.java)
                if (id != null && libroFirestore != null) {
                    libroFirestore.toLibro(id)
                } else {
                    null
                }
            }
            // Insertar o actualizar libros en Room
            libroDao.insertLibros(libros)
            Log.d("DataRepository", "Libros insertados/actualizados en Room: ${libros.size} libros.")

            // Descargar Notas desde Firestore
            val notasSnapshot = userDocRef.collection("notas").get().await()
            Log.d("DataRepository", "Notas descargadas desde Firestore: ${notasSnapshot.size()} documentos.")
            val notas = notasSnapshot.documents.mapNotNull { doc ->
                val id = doc.id.toIntOrNull()
                val libroId = doc.getLong("libroId")?.toInt() ?: 0
                val notaFirestore = doc.toObject(NotaFirestore::class.java)
                if (id != null && notaFirestore != null) {
                    notaFirestore.toNota(id, libroId)
                } else {
                    null
                }
            }
            // Insertar o actualizar notas en Room
            notaDao.insertarNotas(notas)
            Log.d("DataRepository", "Notas insertadas/actualizadas en Room: ${notas.size} notas.")

            // Subir datos locales a Firestore
            uploadLocalDataToFirestore(userId)
            Log.d("DataRepository", "Datos locales sincronizados y subidos a Firestore correctamente.")
        } catch (e: Exception) {
            Log.e("DataRepository", "Error en la sincronización: ${e.localizedMessage}")
        }
    }

    /**
     * Sube los datos locales de Room a Firestore utilizando operaciones por lotes (batch).
     */
    suspend fun uploadLocalDataToFirestore(userId: String) {
        val userDocRef = firestore.collection("usuarios").document(userId)

        try {
            Log.d("DataRepository", "Iniciando subida de libros a Firestore.")
            // Subir Libros
            val libros = libroDao.getAllLibros().first()
            val batch = firestore.batch()
            libros.forEach { libro ->
                val libroDocRef = userDocRef.collection("libros").document(libro.id.toString())
                batch.set(libroDocRef, libro.toFirestore())
                Log.d("DataRepository", "Libro subido a Firestore: ID=${libro.id}, Nombre=${libro.nombreLibro}")
            }

            Log.d("DataRepository", "Iniciando subida de notas a Firestore.")
            // Subir Notas
            val notas = notaDao.getAllNotas().asFlow().first()
            notas.forEach { nota ->
                val notaDocRef = userDocRef.collection("notas").document(nota.id.toString())
                val notaMap = nota.toFirestore().toMap().toMutableMap()
                notaMap["libroId"] = nota.libroId
                batch.set(notaDocRef, notaMap)
                Log.d("DataRepository", "Nota subida a Firestore: ID=${nota.id}, Título=${nota.titulo}, LibroID=${nota.libroId}")
            }

            // Ejecutar el Batch
            batch.commit().await()
            Log.d("DataRepository", "Batch de subida a Firestore ejecutado exitosamente.")
        } catch (e: Exception) {
            Log.e("DataRepository", "Error al subir datos locales a Firestore: ${e.localizedMessage}")
        }
    }


    /**
     * Establece listeners en Firestore para detectar cambios en tiempo real y actualizar Room.
     */
    fun listenForFirestoreUpdates(userId: String) {
        val userDocRef = firestore.collection("usuarios").document(userId)

        Log.d("DataRepository", "Activando listeners para Firestore.")

        // Listener para Libros
        userDocRef.collection("libros")
            .addSnapshotListener { snapshots, e ->
                if (e != null) {
                    Log.e("DataRepository", "Listener de libros falló: ${e.localizedMessage}")
                    return@addSnapshotListener
                }

                if (snapshots != null) {
                    Log.d("DataRepository", "Listener de libros activado. Cambios detectados.")
                    // Ejecutar en un scope de corrutina
                    kotlinx.coroutines.GlobalScope.launch {
                        val libros = snapshots.documents.mapNotNull { doc ->
                            val id = doc.id.toIntOrNull()
                            val libroFirestore = doc.toObject(LibroFirestore::class.java)
                            if (id != null && libroFirestore != null) {
                                libroFirestore.toLibro(id)
                            } else {
                                null
                            }
                        }
                        libroDao.insertLibros(libros)
                        Log.d("DataRepository", "Libros actualizados desde Firestore: ${libros.size} libros.")
                    }
                }
            }

        // Listener para Notas
        userDocRef.collection("notas")
            .addSnapshotListener { snapshots, e ->
                if (e != null) {
                    Log.e("DataRepository", "Listener de notas falló: ${e.localizedMessage}")
                    return@addSnapshotListener
                }

                if (snapshots != null) {
                    Log.d("DataRepository", "Listener de notas activado. Cambios detectados.")
                    // Ejecutar en un scope de corrutina
                    kotlinx.coroutines.GlobalScope.launch {
                        val notas = snapshots.documents.mapNotNull { doc ->
                            val id = doc.id.toIntOrNull()
                            val libroId = doc.getLong("libroId")?.toInt() ?: 0
                            val notaFirestore = doc.toObject(NotaFirestore::class.java)
                            if (id != null && notaFirestore != null) {
                                notaFirestore.toNota(id, libroId)
                            } else {
                                null
                            }
                        }
                        notaDao.insertarNotas(notas)
                        Log.d("DataRepository", "Notas actualizadas desde Firestore: ${notas.size} notas.")
                    }
                }
            }
    }
}