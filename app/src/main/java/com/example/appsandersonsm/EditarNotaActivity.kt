package com.example.appsandersonsm

import android.media.Image
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.example.appsandersonsm.Modelo.Nota
import com.example.appsandersonsm.ViewModel.NotaViewModel
import com.google.firebase.Firebase
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.firestore
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class EditarNotaActivity : AppCompatActivity() {

    private lateinit var editTextTitulo: EditText
    private lateinit var editTextContenido: EditText
    private lateinit var imageViewDelete: ImageView
    private lateinit var botonGuardar: Button
    private lateinit var notaViewModel: NotaViewModel
    private var nota: Nota? = null
    private var userId = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("EditarNotaActivity", "onCreate iniciado")
        setContentView(R.layout.activity_editar_nota)
        Log.d("EditarNotaActivity", "Layout inflado")


        // Obtener el USER_ID del Intent
        userId = intent.getStringExtra("USER_ID") ?: ""
        Log.d("EditarNotaActivity", "USER_ID recibido: $userId")

        // Obtén el ID de la nota del Intent
        var notaId = intent.getIntExtra("NOTA_ID", -1)
        Log.d("EditarNotaActivity", "Nota ID recibido: $notaId")

        if (notaId == -1) {
            Log.e("EditarNotaActivity", "Nota ID no válido")
            finish() // Cierra la actividad si el ID no es válido
            return
        }


        // Inicializar vistas
        editTextTitulo = findViewById(R.id.editTextTitulo)
        editTextContenido = findViewById(R.id.editTextContenido)
        botonGuardar = findViewById(R.id.botonGuardar)

        // Inicializar ViewModel con la fábrica personalizada
        notaViewModel = ViewModelProvider(this,
            NotaViewModel.NotaViewModelFactory((application as InitApplication).notaRepository)
        ).get(NotaViewModel::class.java)

        // Obtener el ID de la nota pasada en el Intent
        notaId = intent.getIntExtra("NOTA_ID", 0)

        // Cargar la nota desde la base de datos
        notaViewModel.getNotaById(notaId, userId).observe(this) { nota ->
            if (nota != null) {
                this.nota = nota
                editTextTitulo.setText(nota.titulo)
                editTextContenido.setText(nota.contenido)
            } else {
                Toast.makeText(this, "Nota no encontrada", Toast.LENGTH_SHORT).show()
                finish()
            }
        }

        // Manejar el evento de clic del botón Guardar
        botonGuardar.setOnClickListener {
            guardarCambios()
        }

        // Eliminar nota
        imageViewDelete = findViewById(R.id.imv_eliminarnota)
        imageViewDelete.setOnClickListener {
            eliminarNotaPorId(notaId)
        }


    }

    private fun guardarCambios() {
        // Validar que la nota no sea nula
        val notaActual = nota ?: run {
            Toast.makeText(this, "No se encontró la nota", Toast.LENGTH_SHORT).show()
            Log.e("EditarNotaActivity", "Intento de guardar cambios sin una nota válida")
            return
        }

        // Validar que los campos no estén vacíos
        val nuevoTitulo = editTextTitulo.text.toString().trim()
        val nuevoContenido = editTextContenido.text.toString().trim()

        if (nuevoTitulo.isEmpty()) {
            editTextTitulo.error = "El título no puede estar vacío"
            Log.e("EditarNotaActivity", "Título vacío")
            return
        }

        if (nuevoContenido.isEmpty()) {
            editTextContenido.error = "El contenido no puede estar vacío"
            Log.e("EditarNotaActivity", "Contenido vacío")
            return
        }

        // Actualizar los datos de la nota
        notaActual.titulo = nuevoTitulo
        notaActual.contenido = nuevoContenido
        notaActual.fechaModificacion = obtenerFechaActual() // Actualiza la fecha de modificación
        Log.d("EditarNotaActivity", "Actualizando nota: $notaActual")

        // Actualizar la nota en la base de datos
        notaViewModel.updateNota(notaActual)

        // Opcional: Mostrar un mensaje de confirmación
        Toast.makeText(this, "Nota actualizada correctamente", Toast.LENGTH_SHORT).show()
        Log.d("EditarNotaActivity", "Nota actualizada correctamente")

        // Finalizar la actividad
        finish()
    }

    private fun eliminarNotaPorId(idNota: Int) {
        notaViewModel.eliminarNotaPorId(idNota, userId)

        // Opcional: Mostrar un mensaje al usuario
        Toast.makeText(this, "Nota eliminada", Toast.LENGTH_SHORT).show()
        finish()
    }

    private fun obtenerFechaActual(): String {
        val formatoFecha = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        return formatoFecha.format(Date())
    }
}
