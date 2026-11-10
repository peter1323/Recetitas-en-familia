package com.recetitas.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.recetitas.data.Repositorio

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaLista(repo: Repositorio, filtro: Pantalla.Lista, ir: (Pantalla) -> Unit, volver: () -> Unit) {
    val recetas by repo.recetas.collectAsState()
    var busqueda by rememberSaveable { mutableStateOf("") }
    val titulo = filtro.categoria?.nombre ?: if (filtro.favoritas) "Favoritas" else "Todas las recetas"
    val lista = recetas
        .filter { (filtro.categoria == null || it.categoria == filtro.categoria) && (!filtro.favoritas || it.favorita) }
        .filter { busqueda.isBlank() || coincide(it, busqueda) }
        .sortedBy { it.nombre.lowercase() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(titulo) },
                navigationIcon = { IconButton(volver) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Volver") } },
            )
        },
        floatingActionButton = {
            if (!filtro.favoritas) ExtendedFloatingActionButton(
                onClick = { ir(Pantalla.Editor(null, filtro.categoria)) },
                icon = { Icon(Icons.Outlined.Add, null) }, text = { Text("Añadir") },
            )
        },
    ) { relleno ->
        Column(Modifier.fillMaxSize().padding(relleno).padding(horizontal = 16.dp)) {
            CampoBusqueda(busqueda, { busqueda = it }, "Buscar en ${titulo.lowercase()}")
            if (lista.isEmpty()) Text(
                when {
                    busqueda.isNotBlank() -> "No hay ninguna receta que se llame así."
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
                items(lista, key = { it.id }) { r ->
                    FotoConNombre(repo, r.foto, r.nombre, Modifier.fillMaxWidth().height(150.dp)) { ir(Pantalla.VerReceta(r.id)) }
                }
            }
        }
    }
}
