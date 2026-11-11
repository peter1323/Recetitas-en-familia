package com.recetitas.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.BakeryDining
import androidx.compose.material.icons.outlined.Cake
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material.icons.outlined.DinnerDining
import androidx.compose.material.icons.outlined.Blender
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.CollectionsBookmark
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.SoupKitchen
import androidx.compose.material.icons.outlined.Tapas
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.recetitas.data.Repositorio
import com.recetitas.model.Categoria
import com.recetitas.model.Receta

fun Categoria.icono(): ImageVector = when (this) {
    Categoria.ENTRANTES -> Icons.Outlined.Tapas
    Categoria.PRIMEROS -> Icons.Outlined.SoupKitchen
    Categoria.SEGUNDOS -> Icons.Outlined.DinnerDining
    Categoria.POSTRES -> Icons.Outlined.Cake
    Categoria.SALSAS -> Icons.Outlined.WaterDrop
    Categoria.MASAS -> Icons.Outlined.BakeryDining
    Categoria.OTROS -> Icons.Outlined.Restaurant
}

/** Busca sin tener en cuenta mayúsculas ni acentos ("croqueta" encuentra "Croquetas"). */
fun coincide(r: Receta, texto: String): Boolean = sinAcentos(r.nombre).contains(sinAcentos(texto.trim()))

fun sinAcentos(s: String) = s.lowercase().map {
    when (it) { 'á', 'à' -> 'a'; 'é', 'è' -> 'e'; 'í', 'ì' -> 'i'; 'ó', 'ò' -> 'o'; 'ú', 'ü' -> 'u'; else -> it }
}.joinToString("")

@Composable
fun PantallaInicio(repo: Repositorio, ir: (Pantalla) -> Unit) {
    var utensilios by remember { mutableStateOf(false) }
    if (utensilios) SelectorUtensilios(repo, emptyList(), alCerrar = { utensilios = false }, elegir = false)
    val recetas by repo.recetas.collectAsState()
    var busqueda by rememberSaveable { mutableStateOf("") }

    Column(Modifier.fillMaxSize().safeDrawingPadding().padding(horizontal = 16.dp)) {
        Text("Recetitas en familia", style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.secondary, modifier = Modifier.padding(top = 16.dp, bottom = 12.dp))
        CampoBusqueda(busqueda, { busqueda = it }, "Buscar receta")

        LazyVerticalGrid(
            GridCells.Adaptive(if (busqueda.isBlank()) 150.dp else 300.dp),
            Modifier.weight(1f).padding(top = 12.dp),
            contentPadding = PaddingValues(bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (busqueda.isNotBlank()) {
                val encontradas = agruparVersiones(recetas.filter { coincide(it, busqueda) })
                if (encontradas.isEmpty()) item(span = { GridItemSpan(maxLineSpan) }) {
                    Text("No hay ninguna receta que se llame así.", Modifier.padding(16.dp))
                }
                items(encontradas, key = { it.first().id }) { versiones -> TarjetaPlato(repo, versiones, ir) }
            } else {
                // Se cuentan platos: las versiones de un mismo plato cuentan como uno
                item { Cuadro("Todas", Icons.Outlined.MenuBook, agruparVersiones(recetas).size) { ir(Pantalla.Lista(null)) } }
                items(Categoria.entries) { c ->
                    Cuadro(c.nombre, c.icono(), agruparVersiones(recetas.filter { it.categoria == c }).size) { ir(Pantalla.Lista(c)) }
                }
            }
        }

        Button({ ir(Pantalla.Editor(null)) }, Modifier.fillMaxWidth().height(56.dp)) {
            Icon(Icons.Outlined.Add, null); Text("  Añadir receta", style = MaterialTheme.typography.titleMedium)
        }
        Row(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
            BotonAbajo("Favoritas", Icons.Outlined.FavoriteBorder) { ir(Pantalla.Lista(null, favoritas = true)) }
            BotonAbajo("Colecciones", Icons.Outlined.CollectionsBookmark) { ir(Pantalla.Colecciones) }
            BotonAbajo("Utensilios", Icons.Outlined.Blender) { utensilios = true }
            BotonAbajo("Ajustes", Icons.Outlined.Settings) { ir(Pantalla.Ajustes) }
        }
    }
}

// Icono encima del texto: así caben los cuatro aunque la letra esté al máximo
@Composable
private fun RowScope.BotonAbajo(texto: String, icono: ImageVector, alTocar: () -> Unit) {
    Column(
        Modifier.weight(1f).clip(RoundedCornerShape(12.dp)).clickable(onClick = alTocar).padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(icono, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
        // Nunca más grande que la letra Normal: si no, "Colecciones" no cabe en el móvil
        val escala = LocalDensity.current.fontScale
        Text(texto, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge,
            fontSize = (14f / maxOf(escala, 1f)).sp, maxLines = 1)
    }
}

@Composable
private fun Cuadro(texto: String, icono: ImageVector, cuantas: Int, alTocar: () -> Unit) {
    Card(
        Modifier.fillMaxWidth().clickable(onClick = alTocar),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
    ) {
        Column(Modifier.fillMaxWidth().padding(vertical = 16.dp, horizontal = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icono, null, Modifier.size(36.dp), tint = MaterialTheme.colorScheme.onSecondaryContainer)
            Text(texto, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSecondaryContainer, modifier = Modifier.padding(top = 6.dp))
            Text(if (cuantas == 1) "1 receta" else "$cuantas recetas", style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun CampoBusqueda(valor: String, alCambiar: (String) -> Unit, texto: String) {
    OutlinedTextField(
        valor, alCambiar, Modifier.fillMaxWidth().widthIn(max = 700.dp),
        placeholder = { Text(texto) }, singleLine = true, shape = RoundedCornerShape(14.dp),
        leadingIcon = { Icon(Icons.Outlined.Search, null) },
        trailingIcon = if (valor.isEmpty()) null else ({ IconButton({ alCambiar("") }) { Icon(Icons.Outlined.Clear, "Borrar búsqueda") } }),
    )
}
