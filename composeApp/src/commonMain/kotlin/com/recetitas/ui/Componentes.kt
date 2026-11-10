package com.recetitas.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.recetitas.bytesAImagen
import com.recetitas.data.Repositorio
import com.recetitas.prepararFoto
import com.recetitas.rememberCamara
import com.recetitas.rememberDictado
import io.github.vinceglb.filekit.dialogs.FileKitType
import io.github.vinceglb.filekit.dialogs.compose.rememberFilePickerLauncher
import io.github.vinceglb.filekit.readBytes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Carga una foto guardada fuera del hilo de la interfaz. */
@Composable
fun rememberFoto(repo: Repositorio, nombre: String?): ImageBitmap? {
    // shortcut: decodifica la foto entera (máx. 1600 px) cada vez; si las listas van lentas, guardar miniaturas
    val img by produceState<ImageBitmap?>(null, nombre) {
        value = nombre?.let { withContext(Dispatchers.Default) { repo.leerFoto(it)?.let(::bytesAImagen) } }
    }
    return img
}

@Composable
fun Foto(repo: Repositorio, nombre: String?, modifier: Modifier = Modifier, alTocar: (() -> Unit)? = null) {
    val img = rememberFoto(repo, nombre)
    Box(
        modifier.clip(RoundedCornerShape(14.dp)).background(MaterialTheme.colorScheme.surfaceVariant)
            .then(if (alTocar != null) Modifier.clickable(onClick = alTocar) else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        if (img != null) Image(img, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        else Icon(Icons.Outlined.Restaurant, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(36.dp))
    }
}

/** Foto del plato con el nombre encima, sobre una franja oscura para que se lea siempre. */
@Composable
fun FotoConNombre(repo: Repositorio, foto: String?, nombre: String, modifier: Modifier, tam: Int = 18, alTocar: (() -> Unit)? = null) {
    Box(modifier) {
        Foto(repo, foto, Modifier.fillMaxSize(), alTocar)
        Text(
            nombre.ifBlank { "Sin nombre" },
            color = Color.White, fontSize = tam.sp, fontWeight = FontWeight.Medium,
            modifier = Modifier.align(Alignment.BottomStart).fillMaxWidth()
                .clip(RoundedCornerShape(bottomStart = 14.dp, bottomEnd = 14.dp))
                .background(Color(0xB32C2C2A)).padding(horizontal = 12.dp, vertical = 8.dp),
        )
    }
}

/** Botones "Hacer foto" (solo móvil) y "Galería". Llama a [alGuardar] con el nombre de la foto ya guardada. */
@Composable
fun BotonesFoto(repo: Repositorio, alGuardar: (String) -> Unit, modifier: Modifier = Modifier) {
    val scope = rememberCoroutineScope()
    var error by remember { mutableStateOf(false) }
    fun guardar(bytes: ByteArray) = scope.launch {
        val nombre = withContext(Dispatchers.Default) { prepararFoto(bytes)?.let(repo::guardarFoto) }
        if (nombre != null) alGuardar(nombre) else error = true
    }
    val camara = rememberCamara { guardar(it) }
    val galeria = rememberFilePickerLauncher(type = FileKitType.Image) { f ->
        if (f != null) scope.launch { guardar(f.readBytes()) }
    }
    Column(modifier) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (camara != null) OutlinedButton(onClick = camara, Modifier.weight(1f)) {
                Icon(Icons.Outlined.PhotoCamera, null); Text("  Cámara", maxLines = 1)
            }
            OutlinedButton(onClick = { galeria.launch() }, Modifier.weight(1f)) {
                Icon(Icons.Outlined.PhotoLibrary, null); Text("  Galería", maxLines = 1)
            }
        }
        if (error) Text("Esa imagen no se ha podido abrir. Prueba con otra.", color = MaterialTheme.colorScheme.error)
    }
}

/** Foto a pantalla completa. */
@Composable
fun VisorFoto(repo: Repositorio, foto: String, titulo: String, pie: String = "", alCerrar: () -> Unit) {
    Dialog(alCerrar, DialogProperties(usePlatformDefaultWidth = false)) {
        Column(Modifier.fillMaxSize().background(Color(0xFF1E1E1C)).padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(titulo, color = Color(0xFFF1EFE8), fontSize = 16.sp, modifier = Modifier.weight(1f))
                IconButton(alCerrar) { Icon(Icons.Outlined.Close, "Cerrar", tint = Color(0xFFF1EFE8)) }
            }
            val img = rememberFoto(repo, foto)
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                if (img != null) Image(img, null, Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
            }
            if (pie.isNotBlank()) Text(pie, color = Color(0xFFFAC775), fontSize = 15.sp, modifier = Modifier.padding(top = 8.dp))
        }
    }
}

/** Campo de texto con botón de micrófono para dictar (en el móvil). Lo dictado se añade al final. */
@Composable
fun CampoTexto(
    valor: String, alCambiar: (String) -> Unit, etiqueta: String,
    modifier: Modifier = Modifier, lineas: Int = 1, dictar: Boolean = false,
) {
    val dictado = if (dictar) rememberDictado { alCambiar(if (valor.isBlank()) it else "$valor $it") } else null
    OutlinedTextField(
        valor, alCambiar, modifier.fillMaxWidth(),
        label = { Text(etiqueta) },
        singleLine = lineas == 1, minLines = lineas,
        trailingIcon = dictado?.let { { IconButton(it) { Icon(Icons.Outlined.Mic, "Dictar") } } },
    )
}

@Composable
fun Seccion(titulo: String, modifier: Modifier = Modifier) {
    Text(titulo, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.secondary,
        modifier = modifier.padding(top = 20.dp, bottom = 8.dp))
}

/** Hueco reservado a la foto cuando aún no hay ninguna. */
@Composable
fun HuecoFoto(texto: String, modifier: Modifier) {
    Box(modifier.clip(RoundedCornerShape(14.dp)).background(MaterialTheme.colorScheme.surfaceVariant), contentAlignment = Alignment.Center) {
        Text(texto, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun Espacio(h: Int) = Box(Modifier.height(h.dp))
