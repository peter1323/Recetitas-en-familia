package com.recetitas.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CollectionsBookmark
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.recetitas.data.Repositorio
import com.recetitas.model.Coleccion

/** Lista de colecciones ("Mis recetas", "Navidad"...). Al tocar una se ven sus recetas. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaColecciones(repo: Repositorio, ir: (Pantalla) -> Unit, volver: () -> Unit) {
    val colecciones by repo.colecciones.collectAsState()
    var nueva by remember { mutableStateOf(false) }
    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Colecciones") },
                navigationIcon = { IconButton(volver) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Volver") } })
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(onClick = { nueva = true }, icon = { Icon(Icons.Outlined.Add, null) }, text = { Text("Nueva colección") })
        },
    ) { relleno ->
        Column(Modifier.fillMaxSize().padding(relleno).padding(horizontal = 16.dp)) {
            if (colecciones.isEmpty()) Text(
                "Una colección junta las recetas que tú quieras, por ejemplo \"Mis recetas\" o \"Navidad\". Pulsa Nueva colección para crear la primera.",
                style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(vertical = 24.dp),
            )
            LazyVerticalGrid(
                GridCells.Adaptive(150.dp), Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 90.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(colecciones.sortedBy { sinAcentos(it.nombre) }, key = { it.id }) { c ->
                    Card(Modifier.fillMaxWidth().clickable { ir(Pantalla.Lista(null, coleccion = c.id)) }, shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
                        Column(Modifier.fillMaxWidth().padding(vertical = 16.dp, horizontal = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Outlined.CollectionsBookmark, null, Modifier.size(36.dp), tint = MaterialTheme.colorScheme.onSecondaryContainer)
                            Text(c.nombre, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.padding(top = 6.dp))
                            Text(if (c.recetas.size == 1) "1 receta" else "${c.recetas.size} recetas", style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
    if (nueva) NombreColeccion("Nueva colección", "", alCerrar = { nueva = false }) { nombre ->
        val c = Coleccion(nombre = nombre)
        repo.guardarColeccion(c)
        nueva = false
        ir(Pantalla.Lista(null, coleccion = c.id))
    }
}

/** Pide el nombre de una colección (nueva o para renombrarla). */
@Composable
fun NombreColeccion(titulo: String, inicial: String, alCerrar: () -> Unit, alGuardar: (String) -> Unit) {
    var nombre by remember { mutableStateOf(inicial) }
    var error by remember { mutableStateOf(false) }
    Aviso(
        onDismissRequest = alCerrar,
        title = { Text(titulo) },
        text = {
            Column {
                CampoTexto(nombre, { nombre = it; error = false }, "Nombre (Mis recetas, Navidad...)")
                if (error) Text("Ponle un nombre a la colección.", color = MaterialTheme.colorScheme.error)
            }
        },
        confirmButton = { Button({ if (nombre.isBlank()) error = true else alGuardar(nombre.trim()) }) { Text("Guardar") } },
        dismissButton = { TextButton(alCerrar) { Text("Cancelar") } },
    )
}

/** Elegir qué recetas van en una colección (casillas). */
@Composable
fun ElegirRecetasColeccion(repo: Repositorio, c: Coleccion, alCerrar: () -> Unit) {
    var sel by remember { mutableStateOf(c.recetas.toSet()) }
    var busqueda by remember { mutableStateOf("") }
    val todas = repo.recetas.value.filter { busqueda.isBlank() || coincide(it, busqueda) }.sortedBy { sinAcentos(it.titulo()) }
    Dialogo(alCerrar) {
        Surface(Modifier.widthIn(max = 700.dp).fillMaxWidth().padding(12.dp), shape = RoundedCornerShape(20.dp)) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Recetas de \"${c.nombre}\"", style = MaterialTheme.typography.headlineSmall)
                CampoBusqueda(busqueda, { busqueda = it }, "Buscar receta")
                Column(Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState())) {
                    todas.forEach { r ->
                        Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable { sel = if (r.id in sel) sel - r.id else sel + r.id },
                            verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(r.id in sel, { sel = if (it) sel + r.id else sel - r.id })
                            Foto(repo, r.foto, Modifier.size(48.dp))
                            Text(r.titulo(), style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(start = 12.dp))
                        }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    TextButton(alCerrar, Modifier.weight(1f).height(52.dp)) { Text("Cancelar") }
                    Button({ repo.guardarColeccion(c.copy(recetas = repo.recetas.value.map { it.id }.filter { it in sel })); alCerrar() },
                        Modifier.weight(1f).height(52.dp)) { Text("Guardar (${sel.size})") }
                }
            }
        }
    }
}
