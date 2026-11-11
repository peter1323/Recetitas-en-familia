package com.recetitas.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.recetitas.AtrasSistema
import com.recetitas.MantenerPantallaEncendida
import com.recetitas.archivoRecibido
import com.recetitas.carpetaDatos
import com.recetitas.carpetaTemporal
import com.recetitas.data.Repositorio
import com.recetitas.model.Categoria
import com.recetitas.model.Paquete
import io.github.vinceglb.filekit.dialogs.FileKitType
import io.github.vinceglb.filekit.dialogs.compose.rememberFilePickerLauncher
import io.github.vinceglb.filekit.readBytes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.io.write

sealed interface Pantalla {
    data object Inicio : Pantalla
    /** categoria == null: todas, o solo favoritas si [favoritas]. */
    data class Lista(val categoria: Categoria?, val favoritas: Boolean = false) : Pantalla
    data class VerReceta(val id: String) : Pantalla
    data class Editor(val id: String?, val categoria: Categoria? = null) : Pantalla
    data object Ajustes : Pantalla
    data object Pdf : Pantalla
}

@Composable
fun App() {
    val repo = remember { Repositorio(carpetaDatos()) }
    val ajustes by repo.ajustes.collectAsState()
    val pila = remember { mutableStateListOf<Pantalla>(Pantalla.Inicio) }
    val scope = rememberCoroutineScope()

    val ir: (Pantalla) -> Unit = { pila.add(it) }
    val volver: () -> Unit = { if (pila.size > 1) pila.removeAt(pila.lastIndex) }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.Default) {
            runCatching { repo.limpiarFotos() }
            runCatching { repo.copiaDiariaSiToca() }
        }
    }

    // ---------- Recibir recetas (WhatsApp, doble clic, botón Importar) ----------
    val recibido by archivoRecibido.collectAsState()
    var paquete by remember { mutableStateOf<Paquete?>(null) }
    var errorImportar by remember { mutableStateOf(false) }
    val tmpImportar = remember { Path(carpetaTemporal(), "importar").toString() }
    LaunchedEffect(recibido) {
        val ruta = recibido ?: return@LaunchedEffect
        val p = withContext(Dispatchers.Default) { repo.leerPaquete(ruta, tmpImportar) }
        if (p == null || p.recetas.isEmpty()) errorImportar = true else paquete = p
        archivoRecibido.value = null
    }
    val selectorImportar = rememberFilePickerLauncher(type = FileKitType.File()) { f ->
        if (f != null) scope.launch {
            val destino = Path(carpetaTemporal(), "elegida.receta")
            val bytes = f.readBytes()
            withContext(Dispatchers.Default) { SystemFileSystem.sink(destino).buffered().use { it.write(bytes) } }
            archivoRecibido.value = destino.toString()
        }
    }

    TemaRecetitas(ajustes) {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            AtrasSistema(pila.size > 1, volver)
            val actual = pila.last()
            MantenerPantallaEncendida(ajustes.pantallaEncendida && actual is Pantalla.VerReceta)
            Box(Modifier.fillMaxSize()) {
                when (actual) {
                    Pantalla.Inicio -> PantallaInicio(repo, ir)
                    is Pantalla.Lista -> PantallaLista(repo, actual, ir, volver)
                    // key: al saltar de una receta a otra vinculada, la nueva empieza desde arriba
                    is Pantalla.VerReceta -> key(actual.id) { PantallaReceta(repo, actual.id, ir, volver) }
                    is Pantalla.Editor -> PantallaEditor(repo, actual.id, actual.categoria, volver = volver, alGuardar = { id ->
                        // Al crear una receta nueva se pasa directamente a verla
                        volver()
                        if (actual.id == null) ir(Pantalla.VerReceta(id))
                    })
                    Pantalla.Ajustes -> PantallaAjustes(repo, volver, importar = { selectorImportar.launch() }, ir = ir)
                    Pantalla.Pdf -> PantallaPdf(repo, volver)
                }
            }

            paquete?.let { p ->
                val existentes = repo.recetas.value.map { it.id }.toSet()
                val nuevas = p.recetas.count { it.id !in existentes }
                val texto = when {
                    p.recetas.size == 1 && nuevas == 0 -> "Ya tienes \"${p.recetas[0].nombre}\". ¿La actualizas con esta versión?"
                    p.recetas.size == 1 -> "¿Guardar \"${p.recetas[0].nombre}\" en tus recetas?"
                    else -> "¿Guardar ${p.recetas.size} recetas? ($nuevas nuevas, ${p.recetas.size - nuevas} se actualizan)"
                }
                Aviso(
                    onDismissRequest = { paquete = null },
                    title = { Text("Receta recibida") },
                    text = { Text(texto) },
                    confirmButton = {
                        Button({
                            paquete = null
                            scope.launch {
                                withContext(Dispatchers.Default) { repo.importar(p, tmpImportar) }
                                if (p.recetas.size == 1) ir(Pantalla.VerReceta(p.recetas[0].id))
                            }
                        }) { Text("Sí, guardar") }
                    },
                    dismissButton = { TextButton({ paquete = null }) { Text("No") } },
                )
            }
            if (errorImportar) Aviso(
                onDismissRequest = { errorImportar = false },
                title = { Text("No se ha podido abrir") },
                text = { Text("Ese archivo no es una receta de Recetitas en familia.") },
                confirmButton = { Button({ errorImportar = false }) { Text("Vale") } },
            )
        }
    }
}
