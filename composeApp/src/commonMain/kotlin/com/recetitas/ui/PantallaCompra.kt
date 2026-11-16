package com.recetitas.ui

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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.recetitas.data.Repositorio
import com.recetitas.model.escalarIngrediente
import com.recetitas.model.juntarIngredientes
import com.recetitas.rememberCompartirTexto

/** Lista de la compra: las recetas elegidas (con sus raciones) y sus ingredientes juntos, para ir tachando. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaCompra(repo: Repositorio, ir: (Pantalla) -> Unit, volver: () -> Unit) {
    val lista by repo.compra.collectAsState()
    val recetas by repo.recetas.collectAsState()
    var nueva by remember { mutableStateOf("") }
    var vaciar by remember { mutableStateOf(false) }
    var aviso by remember { mutableStateOf<String?>(null) }
    val compartir = rememberCompartirTexto()

    val elegidas = lista.recetas.mapNotNull { e -> recetas.find { it.id == e.receta }?.let { it to e.raciones } }
    val ingredientes = juntarIngredientes(elegidas.flatMap { (r, raciones) -> r.ingredientes.map { escalarIngrediente(it, r.raciones, raciones) } }) + lista.extra
    val pendientes = ingredientes.filterNot { it in lista.tachados }

    Scaffold(topBar = {
        TopAppBar(title = { Text("Lista de la compra") },
            navigationIcon = { IconButton(volver) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Volver") } })
    }) { relleno ->
        Box(Modifier.fillMaxSize().padding(relleno), contentAlignment = Alignment.TopCenter) {
            Column(Modifier.widthIn(max = 700.dp).fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
                if (elegidas.isEmpty() && lista.extra.isEmpty()) Text(
                    "La lista está vacía. Dentro de una receta, pulsa \"Añadir a la lista de la compra\" y aquí se juntan los ingredientes de todas.",
                    style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(vertical = 24.dp),
                )

                if (elegidas.isNotEmpty()) Seccion("Recetas")
                elegidas.forEach { (r, raciones) ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(r.titulo(), style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.weight(1f).clickable { ir(Pantalla.VerReceta(r.id)) })
                        Text("$raciones pers.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        IconButton({ repo.guardarCompra(lista.copy(recetas = lista.recetas.filterNot { it.receta == r.id })) }) {
                            Icon(Icons.Outlined.Close, "Quitar ${r.titulo()} de la lista")
                        }
                    }
                }

                if (ingredientes.isNotEmpty()) Seccion("Comprar (${pendientes.size})")
                ingredientes.forEach { item ->
                    val tachado = item in lista.tachados
                    Row(Modifier.fillMaxWidth().clickable {
                        repo.guardarCompra(lista.copy(tachados = if (tachado) lista.tachados - item else lista.tachados + item))
                    }, verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(tachado, { repo.guardarCompra(lista.copy(tachados = if (it) lista.tachados + item else lista.tachados - item)) })
                        Text(item, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f),
                            textDecoration = if (tachado) TextDecoration.LineThrough else null,
                            color = if (tachado) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface)
                        if (item in lista.extra) IconButton({ repo.guardarCompra(lista.copy(extra = lista.extra - item, tachados = lista.tachados - item)) }) {
                            Icon(Icons.Outlined.Close, "Quitar $item", Modifier.size(20.dp))
                        }
                    }
                    HorizontalDivider()
                }

                Seccion("Añadir otra cosa")
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CampoTexto(nueva, { nueva = it }, "Papel de cocina, pan...", Modifier.weight(1f), dictar = true)
                    IconButton({
                        if (nueva.isNotBlank()) { repo.guardarCompra(lista.copy(extra = lista.extra + nueva.trim())); nueva = "" }
                    }) { Icon(Icons.Outlined.Add, "Añadir a la lista") }
                }

                if (ingredientes.isNotEmpty()) {
                    Button({
                        val texto = "Lista de la compra\n" + pendientes.joinToString("\n") { "• $it" }
                        aviso = compartir(texto)
                    }, Modifier.fillMaxWidth().padding(top = 20.dp).height(52.dp)) {
                        Icon(Icons.Outlined.Share, null); Text("  Compartir lo que falta")
                    }
                    aviso?.let { Text(it, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 6.dp)) }
                    OutlinedButton({ vaciar = true }, Modifier.fillMaxWidth().padding(vertical = 10.dp).height(52.dp)) { Text("Vaciar lista") }
                }
            }
        }
    }

    if (vaciar) Aviso(
        onDismissRequest = { vaciar = false },
        title = { Text("¿Vaciar la lista de la compra?") },
        text = { Text("Se quitan todas las recetas y cosas de la lista. Las recetas siguen en la app.") },
        confirmButton = { Button({ vaciar = false; repo.guardarCompra(com.recetitas.model.ListaCompra()) }) { Text("Vaciar") } },
        dismissButton = { TextButton({ vaciar = false }) { Text("No") } },
    )
}
