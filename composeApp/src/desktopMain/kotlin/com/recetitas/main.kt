package com.recetitas

import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.recetitas.ui.App
import java.io.File

fun main(args: Array<String>) {
    // Doble clic en un .receta: Windows abre la app con la ruta del archivo
    args.firstOrNull { it.endsWith(".receta", ignoreCase = true) && File(it).exists() }?.let { archivoRecibido.value = it }
    application {
        Window(
            onCloseRequest = ::exitApplication,
            title = "Recetitas en familia",
            icon = remember { BitmapPainter(bytesAImagen(object {}.javaClass.getResourceAsStream("/icono.png")!!.readBytes())!!) },
            state = rememberWindowState(width = 1100.dp, height = 800.dp),
        ) {
            window.minimumSize = java.awt.Dimension(420, 600)
            App()
        }
    }
}
