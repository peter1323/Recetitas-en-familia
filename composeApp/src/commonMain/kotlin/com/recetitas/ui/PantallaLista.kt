package com.recetitas.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.PlaylistAdd
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.recetitas.data.Repositorio

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaLista(repo: Repositorio, filtro: Pantalla.Lista, ir: (Pantalla) -> Unit, volver: () -> Unit) {
    val recetas by repo.recetas.collectAsState()
    val colecciones by repo.colecciones.collectAsState()
    val coleccion = filtro.coleccion?.let { id -> colecciones.find { it.id == id } }
    // La colección se ha borrado: se vuelve atrás
    if (filtro.coleccion != null && coleccion == null) {
        LaunchedEffect(Unit) { volver() }
        return
    }
    var busqueda by rememberSaveable { mutableStateOf("") }
    var renombrar by remember { mutableStateOf(false) }
    var borrar by remember { mutableStateOf(false) }
    var elegir by remember { mutableStateOf(false) }
    val titulo = coleccion?.nombre ?: filtro.categoria?.nombre ?: if (filtro.favoritas) "Favoritas" else "Todas las recetas"
    val platos = agruparVersiones(
        recetas.filter {
            (filtro.categoria == null || it.categoria == filtro.categoria) && (!filtro.favoritas || it.favorita) &&
                (coleccion == null || it.id in coleccion.recetas)
        }.filter { busqueda.isBlank() || coincide(it, busqueda) },
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(titulo) },
                navigationIcon = { IconButton(volver) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Volver") } },
                actions = {
                    if (coleccion != null) {
                        IconButton({ renombrar = true }) { Icon(Icons.Outlined.Edit, "Cambiar nombre de la colección") }
                        IconButton({ borrar = true }) { Icon(Icons.Outlined.Delete, "Borrar colección") }
                    }
                },
            )
        },
        floatingActionButton = {
            when {
                coleccion != null -> ExtendedFloatingActionButton(
                    onClick = { elegir = true }, icon = { Icon(Icons.Outlined.PlaylistAdd, null) }, text = { Text("Elegir recetas") },
                )
                !filtro.favoritas -> ExtendedFloatingActionButton(
                    onClick = { ir(Pantalla.Editor(null, filtro.categoria)) },
                    icon = { Icon(Icons.Outlined.Add, null) }, text = { Text("Añadir") },
                )
            }
        },
    ) { relleno ->
        Column(Modifier.fillMaxSize().padding(relleno).padding(horizontal = 16.dp)) {
            CampoBusqueda(busqueda, { busqueda = it }, "Buscar en ${titulo.lowercase()}")
            if (platos.isEmpty()) Text(
                when {
                    busqueda.isNotBlank() -> "No hay ninguna receta que se llame así."
                    coleccion != null -> "Esta colección está vacía. Pulsa Elegir recetas para meter las que quieras."
                    filtro.favoritas -> "Toca el corazón de una receta para tenerla aquí."
                    else -> "Aún no hay recetas aquí. Pulsa Añadir para guardar la primera."
                },
                style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(vertical = 24.dp),
            )
            LazyVerticalGrid(
                GridCells.Adaptive(300.dp), Modifier.fillMaxSize().padding(top = 12.dp),
                contentPadding = PaddingValues(bottom = 90.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(platos, key = { it.first().id }) { versiones -> TarjetaPlato(repo, versiones, ir) }
            }
        }
    }

    if (coleccion != null) {
        if (elegir) ElegirRecetasColeccion(repo, coleccion) { elegir = false }
        if (renombrar) NombreColeccion("Cambiar nombre", coleccion.nombre, alCerrar = { renombrar = false }) {
            repo.guardarColeccion(coleccion.copy(nombre = it)); renombrar = false
        }
        if (borrar) Aviso(
            onDismissRequest = { borrar = false },
            title = { Text("¿Borrar la colección \"${coleccion.nombre}\"?") },
            text = { Text("Solo se borra la colección. Las recetas siguen en la app.") },
            confirmButton = { Button({ borrar = false; repo.borrarColeccion(coleccion.id) }) { Text("Borrar") } },
            dismissButton = { TextButton({ borrar = false }) { Text("No") } },
        )
    }
}
