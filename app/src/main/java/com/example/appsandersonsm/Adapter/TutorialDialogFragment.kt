package com.example.appsandersonsm.Adapter

import com.example.appsandersonsm.R

import android.os.Bundle
import android.view.*
import android.widget.Button
import androidx.fragment.app.DialogFragment
import androidx.viewpager2.widget.ViewPager2
import com.example.appsandersonsm.Modelo.TutorialPage
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator

class TutorialDialogFragment : DialogFragment() {

    private lateinit var viewPager: ViewPager2
    private lateinit var tabLayout: TabLayout
    private lateinit var btnFinish: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Aplica el estilo personalizado con animaciones de deslizamiento
        setStyle(STYLE_NORMAL, R.style.DialogSlideAnimation)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        return inflater.inflate(R.layout.fragment_tutorial_dialog, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        viewPager = view.findViewById(R.id.viewPagerTutorial)
        tabLayout = view.findViewById(R.id.tabLayout)
        btnFinish = view.findViewById(R.id.btnFinish)

        // Define las páginas del tutorial
        val tutorialPages = listOf(
            TutorialPage(
                title = "Guia de Lectura del Cosmere",
                imageRes = R.drawable.img_fondocosmere,
                description = "Esta aplicación te ayudará a registrar los libros que has leído del Cosmere"
            ),
            TutorialPage(
                title = "Mapa Interactivo",
                imageRes = R.drawable.img_mapainteractivo,
                description = "Busca tus libros desplazando la pantalla horizontalmente, y pulsa en los libros para ver las distintas opciones!"
            ),
            TutorialPage(
                title = "Registra tus avances",
                imageRes = R.drawable.img_detalleslibro,
                description = "Anota tu progreso, crea tus notas y valora los libros!"
            ),
            TutorialPage(
                title = "Filtra entre tus Libros",
                imageRes = R.drawable.img_libros,
                description = "Filtra según los libros que hayas leído, empezado, o que te queden por leer!"
            ),
            TutorialPage(
                title = "Más Opciones",
                imageRes = R.drawable.img_ajustes,
                description = "Enterate de las últimas noticias del Cosmere, mira tus estadísticas y traduce la aplicación a otro idioma"
            ),


            // Agrega más páginas según tus necesidades
        )

        // Configura el adaptador
        val adapter = TutorialAdapter(requireActivity(), tutorialPages)
        viewPager.adapter = adapter

        // Conecta el TabLayout con el ViewPager2
        TabLayoutMediator(tabLayout, viewPager) { _, _ -> }.attach()

        // Acción del botón "Empezar"
        btnFinish.setOnClickListener {
            dismiss() // Cierra el tutorial
        }
    }

    override fun onStart() {
        super.onStart()
        // Configurar el tamaño y la posición del diálogo
        dialog?.window?.setLayout(
            1000, // Ancho en píxeles, ajusta según tus necesidades
            2000
        )
        dialog?.window?.setGravity(Gravity.NO_GRAVITY) // Deslizar desde la derecha
        dialog?.window?.setWindowAnimations(R.style.DialogSlideAnimation) // Animación personalizada

        // Configurar el dimAmount para oscurecer el fondo
        dialog?.window?.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
        dialog?.window?.setDimAmount(0.5f) // Valor entre 0.0f (sin oscurecer) y 1.0f (totalmente oscurecido)
    }
}
