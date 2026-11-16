package com.recetitas

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.recetitas.ui.App
import kotlinx.coroutines.flow.MutableStateFlow
import java.awt.Frame
import java.io.File
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket

// Puerto local para que una segunda ventana le pase el archivo a la que ya está abierta
private const val PUERTO = 47321

/** Cada vez que otra ventana nos pasa algo, cambia y la ventana se trae al frente. */
private val traerAlFrente = MutableStateFlow(0)

fun main(args: Array<String>) {
    // Doble clic en un .receta: Windows abre la app con la ruta del archivo
    val archivo = args.firstOrNull { it.endsWith(".receta", ignoreCase = true) && File(it).exists() }

    // Solo una ventana: si ya hay una abierta, se le pasa el archivo (o una línea vacía) y esta se cierra.
    // Así no hay dos ventanas guardando a la vez en los mismos archivos.
    val servidor = runCatching { ServerSocket(PUERTO, 5, InetAddress.getLoopbackAddress()) }.getOrNull()
    if (servidor == null && avisarALaAbierta(archivo.orEmpty())) return

    archivo?.let { archivoRecibido.value = it }
    servidor?.let { escuchar(it) }
    application {
        Window(
            onCloseRequest = ::exitApplication,
            title = "Recetitas en familia",
            icon = remember { BitmapPainter(bytesAImagen(object {}.javaClass.getResourceAsStream("/icono.png")!!.readBytes())!!) },
            // Se abre ocupando toda la pantalla; al restaurarla queda en 1100x800
            state = rememberWindowState(placement = WindowPlacement.Maximized, width = 1100.dp, height = 800.dp),
            // Escape = volver atrás (si hay un menú abierto, Escape lo cierra antes y no llega aquí)
            onKeyEvent = { it.key == Key.Escape && it.type == KeyEventType.KeyDown && pulsarAtras() },
        ) {
            window.minimumSize = java.awt.Dimension(420, 600)
            val aviso by traerAlFrente.collectAsState()
            LaunchedEffect(aviso) {
                if (aviso == 0) return@LaunchedEffect
                if (window.extendedState and Frame.ICONIFIED != 0) window.extendedState = window.extendedState and Frame.ICONIFIED.inv()
                window.toFront()
                window.requestFocus()
            }
            App()
        }
    }
}

/** Devuelve true si había otra ventana de la app escuchando y ha recibido el aviso. */
private fun avisarALaAbierta(ruta: String): Boolean = runCatching {
    Socket(InetAddress.getLoopbackAddress(), PUERTO).use { s ->
        s.soTimeout = 2000
        s.getOutputStream().bufferedWriter(Charsets.UTF_8).apply { write(ruta); newLine(); flush() }
        s.getInputStream().bufferedReader().readLine() == "ok"  // confirma que es nuestra app y no otro programa
    }
}.getOrDefault(false)

private fun escuchar(servidor: ServerSocket) = Thread {
    while (true) {
        runCatching {
            servidor.accept().use { s ->
                val ruta = s.getInputStream().bufferedReader(Charsets.UTF_8).readLine().orEmpty()
                s.getOutputStream().bufferedWriter().apply { write("ok"); newLine(); flush() }
                // Solo se aceptan rutas de archivos .receta que existan
                if (ruta.endsWith(".receta", ignoreCase = true) && File(ruta).exists()) archivoRecibido.value = ruta
                traerAlFrente.value++
            }
        }
    }
}.apply { isDaemon = true; name = "una-sola-ventana"; start() }
