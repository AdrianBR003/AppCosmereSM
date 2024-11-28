package com.example.appsandersonsm

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.example.appsandersonsm.Modelo.Nota
import com.example.appsandersonsm.ViewModel.NotaViewModel

class EditarNotaActivity : AppCompatActivity() {

    private lateinit var editTextTitulo: EditText
    private lateinit var editTextContenido: EditText
    private lateinit var botonGuardar: Button
    private var notaId: Int = 0
    private lateinit var notaViewModel: NotaViewModel
    private var nota: Nota? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_editar_nota)

        // Inicializar vistas

        notaViewModel = ViewModelProvider(this,
            NotaViewModel.NotaViewModelFactory((application as InitApplication).notaRepository)
        ).get(NotaViewModel::class.java)
        editTextTitulo = findViewById(R.id.editTextTitulo)
        editTextContenido = findViewById(R.id.editTextContenido)
        botonGuardar = findViewById(R.id.botonGuardar)

        // Inicializar ViewModel
        notaViewModel = ViewModelProvider(this).get(NotaViewModel::class.java)

        // Obtener el ID de la nota pasada en el Intent
        notaId = intent.getIntExtra("NOTA_ID", 0)

        // Cargar la nota desde la base de datos
        notaViewModel.getNotaById(notaId).observe(this) { nota ->
            if (nota != null) {
                this.nota = nota
                editTextTitulo.setText(nota.titulo)
                editTextContenido.setText(nota.contenido)
            }
        }

        // Manejar el evento de clic del botón Guardar
        botonGuardar.setOnClickListener {
            guardarCambios()
        }
    }

    private fun guardarCambios() {
        // Validar que la nota no sea nula
        val notaActual = nota ?: return

        // Actualizar los datos de la nota
        notaActual.titulo = editTextTitulo.text.toString()
        notaActual.contenido = editTextContenido.text.toString()

        // Actualizar la nota en la base de datos
        notaViewModel.updateNota(notaActual)

        // Finalizar la actividad
        finish()
    }
}
