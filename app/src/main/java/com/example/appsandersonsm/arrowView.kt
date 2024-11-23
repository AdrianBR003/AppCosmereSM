package com.example.appsandersonsm

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.View

class ArrowOverlayView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {

    // Lista de relaciones cronológicas (pares de IDs de libros)
    var relaciones: List<Pair<Int, Int>> = emptyList()

    // Mapa de ID de libro a sus coordenadas en pantalla (x, y)
    var libroCoordenadasPantalla: Map<Int, Pair<Float, Float>> = emptyMap()

    // Paint para dibujar las líneas de las flechas
    private val paint = Paint().apply {
        color = context.getColor(R.color.gold_s)
        strokeWidth = 5f
        style = Paint.Style.STROKE
        isAntiAlias = true
    }

    // Paint para dibujar la punta de las flechas
    private val arrowPaint = Paint().apply {
        color = context.getColor(R.color.gold_s)
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas ?: return

        relaciones.forEach { (idOrigen, idDestino) ->
            val origen = libroCoordenadasPantalla[idOrigen]
            val destino = libroCoordenadasPantalla[idDestino]

            if (origen != null && destino != null) {
                dibujarFlecha(canvas, origen.first, origen.second, destino.first, destino.second)
            }
        }
    }

    private fun dibujarFlecha(
        canvas: Canvas,
        startX: Float,
        startY: Float,
        endX: Float,
        endY: Float
    ) {
        // Dibujar la línea principal de la flecha
        canvas.drawLine(startX, startY, endX, endY, paint)

        // Dibujar la punta de la flecha al final
        dibujarPuntaFlecha(canvas, startX, startY, endX, endY)

        // Dibujar la punta de la flecha en la mitad
        dibujarPuntaFlechaEnMitad(canvas, startX, startY, endX, endY)
    }

    private fun dibujarPuntaFlecha(
        canvas: Canvas,
        startX: Float,
        startY: Float,
        endX: Float,
        endY: Float
    ) {
        val path = Path()
        val arrowSize = 45f

        val angle = Math.atan2((endY - startY).toDouble(), (endX - startX).toDouble())

        path.moveTo(endX, endY)
        path.lineTo(
            (endX - arrowSize * Math.cos(angle - Math.PI / 6)).toFloat(),
            (endY - arrowSize * Math.sin(angle - Math.PI / 6)).toFloat()
        )
        path.lineTo(
            (endX - arrowSize * Math.cos(angle + Math.PI / 6)).toFloat(),
            (endY - arrowSize * Math.sin(angle + Math.PI / 6)).toFloat()
        )
        path.close()

        canvas.drawPath(path, arrowPaint)
    }

    private fun dibujarPuntaFlechaEnMitad(
        canvas: Canvas,
        startX: Float,
        startY: Float,
        endX: Float,
        endY: Float
    ) {

        val t = 0.6f

        val posX = startX + t * (endX - startX)
        val posY = startY + t * (endY - startY)

        val path = Path()
        val arrowSize = 35f

        val angle = Math.atan2((endY - startY).toDouble(), (endX - startX).toDouble())

        path.moveTo(posX, posY)
        path.lineTo(
            (posX - arrowSize * Math.cos(angle - Math.PI / 6)).toFloat(),
            (posY - arrowSize * Math.sin(angle - Math.PI / 6)).toFloat()
        )
        path.lineTo(
            (posX - arrowSize * Math.cos(angle + Math.PI / 6)).toFloat(),
            (posY - arrowSize * Math.sin(angle + Math.PI / 6)).toFloat()
        )
        path.close()

        canvas.drawPath(path, arrowPaint)
    }
}
