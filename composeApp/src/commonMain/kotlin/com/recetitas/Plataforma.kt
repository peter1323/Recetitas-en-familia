package com.recetitas

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.ImageBitmap
import kotlinx.coroutines.flow.MutableStateFlow

// Lo que cada plataforma (Android / Windows) hace a su manera. Las versiones reales están en androidMain y desktopMain.

/** Carpeta donde la app guarda recetas, fotos y ajustes. */
expect fun carpetaDatos(): String

/** Carpeta para archivos temporales (lo que se comparte o se importa). */
expect fun carpetaTemporal(): String

expect fun bytesAImagen(bytes: ByteArray): ImageBitmap?

/** Gira la foto según la cámara, la reduce a 1600 px como mucho y la devuelve en JPEG. null si no es una imagen. */
expect fun prepararFoto(bytes: ByteArray): ByteArray?

/** Mete los archivos en un .zip. [entradas]: nombre dentro del zip -> ruta en disco. */
expect fun crearZip(destino: String, entradas: Map<String, String>)

/** Saca todo el .zip a [carpeta]. Ignora nombres peligrosos ("../"). */
expect fun extraerZip(origen: String, carpeta: String)

/** Abre el menú de compartir del móvil (WhatsApp...) o, en Windows, pregunta dónde guardar. */
@Composable
expect fun rememberCompartir(): (ruta: String, nombre: String) -> Unit

/** Dictado por voz. null si la plataforma no lo tiene. */
@Composable
expect fun rememberDictado(alTexto: (String) -> Unit): (() -> Unit)?

@Composable
expect fun MantenerPantallaEncendida(activo: Boolean)

/** Iconos de la barra de arriba del móvil (hora, batería) oscuros con tema claro y blancos con tema oscuro. */
@Composable
expect fun ColorBarras(oscuro: Boolean)

/** Botón "atrás" del móvil. En Windows no hace nada. */
@Composable
expect fun AtrasSistema(activo: Boolean, alVolver: () -> Unit)

/** Abre la cámara y devuelve la foto. null si no hay cámara (Windows). */
@Composable
expect fun rememberCamara(alFoto: (ByteArray) -> Unit): (() -> Unit)?

/** Versión instalada ("1.1.0"), o null si se ejecuta desde el código sin empaquetar. */
expect fun versionApp(): String?

const val AUTOR = "peter1323"
const val URL_GITHUB = "https://github.com/peter1323/Recetitas-en-familia"

/** Ruta de un archivo .receta que se ha abierto desde fuera (WhatsApp, doble clic). */
val archivoRecibido = MutableStateFlow<String?>(null)
