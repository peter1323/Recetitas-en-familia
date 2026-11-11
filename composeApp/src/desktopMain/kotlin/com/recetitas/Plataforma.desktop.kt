package com.recetitas

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.dialogs.FileKitDialogSettings
import io.github.vinceglb.filekit.dialogs.compose.rememberFileSaverLauncher
import io.github.vinceglb.filekit.path
import org.jetbrains.skia.EncodedImageFormat
import org.jetbrains.skia.Image
import org.jetbrains.skia.Rect
import org.jetbrains.skia.Surface
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

// En Windows los datos van a C:\Users\<usuario>\RecetitasEnFamilia
actual fun carpetaDatos(): String = File(System.getProperty("user.home"), "RecetitasEnFamilia").path

actual fun carpetaTemporal(): String =
    File(System.getProperty("java.io.tmpdir"), "recetitas").apply { mkdirs() }.path

actual fun bytesAImagen(bytes: ByteArray): ImageBitmap? =
    runCatching { Image.makeFromEncoded(bytes).toComposeImageBitmap() }.getOrNull()

actual fun prepararFoto(bytes: ByteArray): ByteArray? = runCatching {
    val img = Image.makeFromEncoded(bytes)
    val escala = minOf(1f, 1600f / maxOf(img.width, img.height))
    val w = (img.width * escala).toInt().coerceAtLeast(1)
    val h = (img.height * escala).toInt().coerceAtLeast(1)
    val surface = Surface.makeRasterN32Premul(w, h)
    surface.canvas.drawImageRect(img, Rect.makeWH(w.toFloat(), h.toFloat()))
    surface.makeImageSnapshot().encodeToData(EncodedImageFormat.JPEG, 85)!!.bytes
}.getOrNull()

// jpackage pone esta propiedad al empaquetar; con "gradlew run" no existe
actual fun versionApp(): String? = System.getProperty("jpackage.app-version")

// En Windows la carpeta de datos ya está a la vista (C:\Users\<usuario>\RecetitasEnFamilia\copias)
actual fun guardarCopiaVisible(ruta: String, nombre: String): String? = null

actual fun crearZip(destino: String, entradas: Map<String, String>) = zipear(destino, entradas)
actual fun extraerZip(origen: String, carpeta: String) = deszipear(origen, carpeta)

@Composable
actual fun rememberCompartir(): (ruta: String, nombre: String) -> Unit {
    // En Windows "compartir" es guardar el archivo donde se quiera (luego se manda por WhatsApp Web, correo...)
    val pendiente = remember { arrayOfNulls<String>(1) }
    val guardar = rememberFileSaverLauncher(FileKitDialogSettings.createDefault()) { destino: PlatformFile? ->
        val origen = pendiente[0]
        if (destino != null && origen != null) File(origen).copyTo(File(destino.path), overwrite = true)
    }
    return remember(guardar) {
        { ruta, nombre ->
            pendiente[0] = ruta
            guardar.launch(nombre.substringBeforeLast('.'), nombre.substringAfterLast('.'))
        }
    }
}

@Composable
actual fun rememberDictado(alTexto: (String) -> Unit): (() -> Unit)? = null

@Composable
actual fun MantenerPantallaEncendida(activo: Boolean) {}

@Composable
actual fun ColorBarras(oscuro: Boolean) {}

/**
 * En Windows "atrás" es la tecla Escape (la recoge la ventana en main.kt).
 * Cada pantalla se apunta al aparecer; la última apuntada que esté activa es la que vuelve atrás.
 */
private val atras = mutableListOf<State<Pair<Boolean, () -> Unit>>>()

/** Lo llama la ventana al pulsar Escape. Devuelve true si alguna pantalla ha vuelto atrás. */
fun pulsarAtras(): Boolean {
    // Con un menú abierto, Escape solo lo cierra (lo hace el propio menú)
    if (com.recetitas.ui.dialogosAbiertos > 0) return false
    val (_, alVolver) = atras.lastOrNull { it.value.first }?.value ?: return false
    alVolver()
    return true
}

@Composable
actual fun AtrasSistema(activo: Boolean, alVolver: () -> Unit) {
    val estado = rememberUpdatedState(activo to alVolver)
    DisposableEffect(Unit) {
        atras += estado
        onDispose { atras -= estado }
    }
}

@Composable
actual fun rememberCamara(alFoto: (ByteArray) -> Unit): (() -> Unit)? = null

internal fun zipear(destino: String, entradas: Map<String, String>) {
    ZipOutputStream(File(destino).outputStream().buffered()).use { zip ->
        entradas.forEach { (nombre, ruta) ->
            zip.putNextEntry(ZipEntry(nombre))
            File(ruta).inputStream().use { it.copyTo(zip) }
            zip.closeEntry()
        }
    }
}

internal fun deszipear(origen: String, carpeta: String) {
    val raiz = File(carpeta).canonicalFile
    ZipInputStream(File(origen).inputStream().buffered()).use { zip ->
        generateSequence { zip.nextEntry }.filterNot { it.isDirectory }.forEach { e ->
            val f = File(raiz, e.name).canonicalFile
            if (!f.path.startsWith(raiz.path + File.separator)) return@forEach  // "../" fuera de la carpeta
            f.parentFile.mkdirs()
            f.outputStream().use { zip.copyTo(it) }
        }
    }
}
