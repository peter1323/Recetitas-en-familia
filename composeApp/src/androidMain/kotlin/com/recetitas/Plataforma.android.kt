package com.recetitas

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.speech.RecognizerIntent
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalView
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.dialogs.compose.rememberCameraPickerLauncher
import io.github.vinceglb.filekit.dialogs.compose.rememberShareFileLauncher
import io.github.vinceglb.filekit.path
import io.github.vinceglb.filekit.dialogs.init
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

private lateinit var carpetaAndroid: String
private lateinit var cacheAndroid: String
private var versionAndroid: String? = null

actual fun versionApp(): String? = versionAndroid

/** Lo llama MainActivity al arrancar y cada vez que llega un archivo de fuera. */
fun iniciarAndroid(activity: ComponentActivity) {
    carpetaAndroid = activity.filesDir.path
    cacheAndroid = activity.cacheDir.path
    versionAndroid = runCatching { activity.packageManager.getPackageInfo(activity.packageName, 0).versionName }.getOrNull()
    FileKit.init(activity)
}

/** Copia a la caché el archivo que llega de WhatsApp (o de donde sea) y avisa a la app. */
fun recibirIntent(activity: Activity, intent: Intent?) {
    val uri: Uri = when (intent?.action) {
        Intent.ACTION_VIEW -> intent.data
        Intent.ACTION_SEND -> @Suppress("DEPRECATION") intent.getParcelableExtra(Intent.EXTRA_STREAM)
        else -> null
    } ?: return
    runCatching {
        val destino = File(activity.cacheDir, "recibida.receta")
        activity.contentResolver.openInputStream(uri)!!.use { entrada -> destino.outputStream().use { entrada.copyTo(it) } }
        archivoRecibido.value = destino.path
    }
}

actual fun carpetaDatos(): String = carpetaAndroid
actual fun carpetaTemporal(): String = cacheAndroid

actual fun bytesAImagen(bytes: ByteArray): ImageBitmap? =
    BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()

actual fun prepararFoto(bytes: ByteArray): ByteArray? = runCatching {
    // Primero solo las medidas, para no cargar entera en memoria una foto de 50 megapíxeles
    val medidas = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeByteArray(bytes, 0, bytes.size, medidas)
    var reduccion = 1
    while (maxOf(medidas.outWidth, medidas.outHeight) / (reduccion * 2) >= 1600) reduccion *= 2
    var bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, BitmapFactory.Options().apply { inSampleSize = reduccion })
        ?: return null
    val escala = minOf(1f, 1600f / maxOf(bmp.width, bmp.height))
    // La cámara guarda la foto de lado y apunta el giro en el EXIF: aquí se aplica
    val giro = when (ExifInterface(bytes.inputStream()).getAttributeInt(ExifInterface.TAG_ORIENTATION, 1)) {
        ExifInterface.ORIENTATION_ROTATE_90 -> 90f
        ExifInterface.ORIENTATION_ROTATE_180 -> 180f
        ExifInterface.ORIENTATION_ROTATE_270 -> 270f
        else -> 0f
    }
    if (escala < 1f || giro != 0f) {
        val m = Matrix().apply { postScale(escala, escala); postRotate(giro) }
        bmp = Bitmap.createBitmap(bmp, 0, 0, bmp.width, bmp.height, m, true)
    }
    ByteArrayOutputStream().also { bmp.compress(Bitmap.CompressFormat.JPEG, 85, it) }.toByteArray()
}.getOrNull()

actual fun crearZip(destino: String, entradas: Map<String, String>) {
    ZipOutputStream(File(destino).outputStream().buffered()).use { zip ->
        entradas.forEach { (nombre, ruta) ->
            zip.putNextEntry(ZipEntry(nombre))
            File(ruta).inputStream().use { it.copyTo(zip) }
            zip.closeEntry()
        }
    }
}

actual fun extraerZip(origen: String, carpeta: String) {
    val raiz = File(carpeta).canonicalFile
    ZipInputStream(File(origen).inputStream().buffered()).use { zip ->
        generateSequence { zip.nextEntry }.filterNot { it.isDirectory }.forEach { e ->
            val f = File(raiz, e.name).canonicalFile
            if (!f.path.startsWith(raiz.path + File.separator)) return@forEach  // "../" fuera de la carpeta
            f.parentFile?.mkdirs()
            f.outputStream().use { zip.copyTo(it) }
        }
    }
}

@Composable
actual fun rememberCompartir(): (ruta: String, nombre: String) -> Unit {
    val compartir = rememberShareFileLauncher()
    return remember(compartir) {
        { ruta, nombre ->
            // Se copia con el nombre bonito ("Flan de huevo.receta"), que es el que verá quien lo reciba
            val dir = File(cacheAndroid, "compartir").apply { deleteRecursively(); mkdirs() }
            val f = File(ruta).copyTo(File(dir, nombre.replace(Regex("""[\\/:*?"<>|]"""), "")), overwrite = true)
            compartir.launch(PlatformFile(f))
        }
    }
}

@Composable
actual fun rememberDictado(alTexto: (String) -> Unit): (() -> Unit)? {
    val alTextoActual = rememberUpdatedState(alTexto)
    val lanzador = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { r ->
        r.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()?.let { alTextoActual.value(it) }
    }
    return {
        runCatching {
            lanzador.launch(Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "es-ES")
                putExtra(RecognizerIntent.EXTRA_PROMPT, "Habla ahora")
            })
        }
    }
}

@Composable
actual fun MantenerPantallaEncendida(activo: Boolean) {
    val vista = LocalView.current
    DisposableEffect(activo) {
        vista.keepScreenOn = activo
        onDispose { vista.keepScreenOn = false }
    }
}

@Composable
actual fun AtrasSistema(activo: Boolean, alVolver: () -> Unit) = BackHandler(activo, alVolver)

@Composable
actual fun rememberCamara(alFoto: (ByteArray) -> Unit): (() -> Unit)? {
    val alFotoActual = rememberUpdatedState(alFoto)
    val camara = rememberCameraPickerLauncher { archivo: PlatformFile? ->
        archivo?.let { runCatching { File(it.path).readBytes() }.getOrNull() }?.let { alFotoActual.value(it) }
    }
    return { camara.launch() }
}
