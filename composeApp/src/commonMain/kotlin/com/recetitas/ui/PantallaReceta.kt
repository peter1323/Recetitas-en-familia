package com.recetitas.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.AddCircleOutline
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.PictureAsPdf
import androidx.compose.material.icons.outlined.RemoveCircleOutline
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.recetitas.carpetaTemporal
import com.recetitas.data.Repositorio
import com.recetitas.model.Receta
import com.recetitas.model.escalarIngrediente
import com.recetitas.pdf.crearPdf
import com.recetitas.rememberCompartir
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.io.write

/** Escribe bytes en la carpeta temporal y devuelve la ruta. */
fun escribirTemporal(nombre: String, bytes: ByteArray): String {
    val ruta = Path(carpetaTemporal(), nombre)
    SystemFileSystem.sink(ruta).buffered().use { it.write(bytes) }
    return ruta.toString()
}

@Composable
fun PantallaReceta(repo: Repositorio, id: String, ir: (Pantalla) -> Unit, volver: () -> Unit) {
    val recetas by repo.recetas.collectAsState()
    val utensilios by repo.utensilios.collectAsState()
    val r = recetas.find { it.id == id } ?: return Text("Esta receta ya no existe.", Modifier.padding(24.dp))
    var raciones by remember(id) { mutableIntStateOf(r.raciones) }
    var visor by remember { mutableStateOf<Triple<String, String, String>?>(null) }
    var pasoEditando by remember { mutableStateOf<Int?>(null) }
    var menuCompartir by remember { mutableStateOf(false) }
    var confirmarBorrar by remember { mutableStateOf(false) }
    val compartir = rememberCompartir()
    val scope = rememberCoroutineScope()

    Box(Modifier.fillMaxSize().safeDrawingPadding(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(Modifier.widthIn(max = 800.dp).fillMaxSize().padding(horizontal = 16.dp)) {
            item {
                Box(Modifier.padding(top = 8.dp)) {
                    FotoConNombre(repo, r.foto, r.nombre, Modifier.fillMaxWidth().height(240.dp), tam = 24) {
                        r.foto?.let { visor = Triple(it, r.nombre, "") }
                    }
                    BotonRedondo(Icons.AutoMirrored.Outlined.ArrowBack, "Volver", volver, Modifier.align(Alignment.TopStart))
                    BotonRedondo(
                        if (r.favorita) Icons.Outlined.Favorite else Icons.Outlined.FavoriteBorder,
                        if (r.favorita) "Quitar de favoritas" else "Añadir a favoritas",
                        { repo.guardarReceta(r.copy(favorita = !r.favorita)) }, Modifier.align(Alignment.TopEnd),
                        color = if (r.favorita) Color(0xFFE24B4A) else Color.White,
                    )
                }
                val datos = listOfNotNull(r.categoria.nombre, r.autor.takeIf { it.isNotBlank() }?.let { "Receta de $it" }, r.tiempo.takeIf { it.isNotBlank() })
                Text(datos.joinToString("  ·  "), color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 10.dp))
                SelectorRaciones(raciones) { raciones = it }
            }

            val misUtensilios = r.utensilios.mapNotNull { u -> utensilios.find { it.id == u } }
            if (misUtensilios.isNotEmpty()) {
                item { Seccion("Utensilios") }
                item {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(misUtensilios) { u ->
                            Column(Modifier.width(100.dp)) {
                                Foto(repo, u.foto, Modifier.size(100.dp)) { u.foto?.let { visor = Triple(it, u.nombre, "") } }
                                Text(u.nombre, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 4.dp))
                            }
                        }
                    }
                }
            }

            if (r.ingredientes.isNotEmpty()) {
                item { Seccion("Ingredientes") }
                items(r.ingredientes.filter { it.isNotBlank() }) { linea ->
                    Text("•  " + escalarIngrediente(linea, r.raciones, raciones), style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(vertical = 3.dp))
                }
            }

            if (r.pasos.isNotEmpty()) item { Seccion("Pasos") }
            itemsIndexed(r.pasos) { i, paso ->
                Card(
                    Modifier.fillMaxWidth().padding(bottom = 10.dp), shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                ) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.Top) {
                            Box(Modifier.size(30.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer), contentAlignment = Alignment.Center) {
                                Text("${i + 1}", color = MaterialTheme.colorScheme.onPrimaryContainer, style = MaterialTheme.typography.titleSmall)
                            }
                            Text(paso.texto, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f).padding(horizontal = 10.dp))
                            IconButton({ pasoEditando = i }, Modifier.size(36.dp)) {
                                Icon(Icons.Outlined.Edit, "Editar paso ${i + 1}", tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                        paso.etiqueta()?.let { EtiquetaFuego(it) }
                        recetas.find { it.id == paso.receta }?.let { otra ->
                            OutlinedButton({ ir(Pantalla.VerReceta(otra.id)) }) {
                                Icon(Icons.Outlined.MenuBook, null); Text("  Ver receta: ${otra.nombre}")
                            }
                        }
                        if (paso.foto != null || paso.nota.isNotBlank()) Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            paso.foto?.let { f -> Foto(repo, f, Modifier.size(110.dp, 80.dp)) { visor = Triple(f, "Paso ${i + 1}", paso.nota.ifBlank { paso.texto }) } }
                            if (paso.nota.isNotBlank()) Nota(paso.nota, Modifier.weight(1f))
                        }
                    }
                }
            }

            item {
                Column(Modifier.padding(vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button({ menuCompartir = true }, Modifier.weight(1f).height(52.dp)) { Icon(Icons.Outlined.Share, null); Text("  Compartir") }
                        if (r.fotoOriginal != null) OutlinedButton({ visor = Triple(r.fotoOriginal, "Receta original", "") }, Modifier.weight(1f).height(52.dp)) {
                            Icon(Icons.Outlined.MenuBook, null); Text("  Original")
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedButton({ ir(Pantalla.Editor(r.id)) }, Modifier.weight(1f).height(52.dp)) { Icon(Icons.Outlined.Edit, null); Text("  Editar") }
                        TextButton({ confirmarBorrar = true }, Modifier.weight(1f).height(52.dp)) {
                            Icon(Icons.Outlined.Delete, null, tint = MaterialTheme.colorScheme.error)
                            Text("  Borrar", color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }
    }

    visor?.let { (f, t, pie) -> VisorFoto(repo, f, t, pie) { visor = null } }

    pasoEditando?.let { i ->
        EditorPaso(repo, r.id, r.pasos[i], "Editar paso ${i + 1}", alCerrar = { pasoEditando = null }) { nuevo ->
            repo.guardarReceta(r.copy(pasos = r.pasos.toMutableList().also { it[i] = nuevo }))
            pasoEditando = null
        }
    }

    if (menuCompartir) AlertDialog(
        onDismissRequest = { menuCompartir = false },
        title = { Text("Compartir \"${r.nombre}\"") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Button({
                    menuCompartir = false
                    scope.launch {
                        val ruta = withContext(Dispatchers.Default) {
                            Path(carpetaTemporal(), "compartida.receta").toString().also { repo.exportar(listOf(r), it) }
                        }
                        compartir(ruta, "${r.nombre.ifBlank { "receta" }}.receta")
                    }
                }, Modifier.fillMaxWidth().height(52.dp)) { Icon(Icons.Outlined.Share, null); Text("  Pasar a la familia") }
                Text("Se mete sola en su app al abrirla.", style = MaterialTheme.typography.bodySmall)
                OutlinedButton({
                    menuCompartir = false
                    scope.launch { compartir(pdfReceta(repo, r), "${r.nombre.ifBlank { "receta" }}.pdf") }
                }, Modifier.fillMaxWidth().height(52.dp)) { Icon(Icons.Outlined.PictureAsPdf, null); Text("  Enviar como PDF") }
                Text("Para leer o imprimir.", style = MaterialTheme.typography.bodySmall)
            }
        },
        confirmButton = { TextButton({ menuCompartir = false }) { Text("Cancelar") } },
    )

    if (confirmarBorrar) AlertDialog(
        onDismissRequest = { confirmarBorrar = false },
        title = { Text("¿Borrar \"${r.nombre}\"?") },
        text = { Text("Se borra de este dispositivo. Las copias de seguridad de días anteriores la siguen teniendo.") },
        confirmButton = {
            Button({ confirmarBorrar = false; volver(); repo.borrarReceta(r.id) }) { Text("Borrar") }
        },
        dismissButton = { TextButton({ confirmarBorrar = false }) { Text("No, dejarla") } },
    )
}

private suspend fun pdfReceta(repo: Repositorio, r: Receta): String = withContext(Dispatchers.Default) {
    escribirTemporal("receta.pdf", crearPdf(listOf(r), repo.utensilios.value, fotosPasos = true, leerFoto = repo::leerFoto, nombreReceta = repo::nombreReceta))
}

@Composable
fun SelectorRaciones(valor: Int, alCambiar: (Int) -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(top = 12.dp).clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.secondaryContainer).padding(horizontal = 14.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("Para", style = MaterialTheme.typography.titleMedium, maxLines = 1)
        Box(Modifier.weight(1f))
        IconButton({ if (valor > 1) alCambiar(valor - 1) }) { Icon(Icons.Outlined.RemoveCircleOutline, "Menos personas", Modifier.size(32.dp)) }
        Text("$valor", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(horizontal = 4.dp))
        IconButton({ if (valor < 99) alCambiar(valor + 1) }) { Icon(Icons.Outlined.AddCircleOutline, "Más personas", Modifier.size(32.dp)) }
        Box(Modifier.weight(1f))
        Text(if (valor == 1) "persona" else "personas", style = MaterialTheme.typography.titleMedium, maxLines = 1)
    }
}

@Composable
fun EtiquetaFuego(texto: String) {
    Row(
        Modifier.clip(RoundedCornerShape(50)).background(MaterialTheme.colorScheme.tertiaryContainer).padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Outlined.LocalFireDepartment, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onTertiaryContainer)
        Text(" $texto", color = MaterialTheme.colorScheme.onTertiaryContainer, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
fun Nota(texto: String, modifier: Modifier = Modifier) {
    Row(modifier.height(IntrinsicSize.Min)) {
        Box(Modifier.width(4.dp).fillMaxHeight().background(Naranja))
        Text(texto, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSecondaryContainer,
            modifier = Modifier.weight(1f).background(MaterialTheme.colorScheme.surface).padding(8.dp))
    }
}

@Composable
private fun BotonRedondo(icono: androidx.compose.ui.graphics.vector.ImageVector, descripcion: String, alTocar: () -> Unit, modifier: Modifier, color: Color = Color.White) {
    IconButton(alTocar, modifier.padding(8.dp).clip(CircleShape).background(Color(0x992C2C2A))) {
        Icon(icono, descripcion, tint = color)
    }
}
