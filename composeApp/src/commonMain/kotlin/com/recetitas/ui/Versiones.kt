package com.recetitas.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.recetitas.data.Repositorio
import com.recetitas.model.Receta

// Versiones: las recetas con el mismo nombre (sin mirar mayúsculas ni acentos) son versiones del mismo plato.

fun clavePlato(r: Receta) = sinAcentos(r.nombre.trim())

/** Agrupa las versiones de cada plato, ordenado por nombre. Dentro, la versión sin etiqueta va primero. */
fun agruparVersiones(recetas: List<Receta>): List<List<Receta>> =
    recetas.groupBy(::clavePlato).values
        .map { g -> g.sortedWith(compareBy({ it.version.isNotBlank() }, { it.version.lowercase() })) }
        .sortedBy { clavePlato(it.first()) }

/** Cómo se llama una versión en la lista para elegir: su etiqueta, o de quién es, o "Versión 2". */
fun nombreVersion(r: Receta, i: Int) = r.version.ifBlank { r.autor.takeIf { it.isNotBlank() }?.let { "Receta de $it" } ?: "Versión ${i + 1}" }

/** Tarjeta de un plato. Con una sola versión entra directo; con varias pregunta cuál vas a cocinar. */
@Composable
fun TarjetaPlato(repo: Repositorio, versiones: List<Receta>, ir: (Pantalla) -> Unit, modifier: Modifier = Modifier) {
    var elegir by remember { mutableStateOf(false) }
    val primera = versiones.first()
    val nombre = if (versiones.size == 1) primera.titulo() else "${primera.nombre} · ${versiones.size} versiones"
    FotoConNombre(repo, primera.foto, nombre, modifier.fillMaxWidth().height(150.dp)) {
        if (versiones.size == 1) ir(Pantalla.VerReceta(primera.id)) else elegir = true
    }
    if (elegir) ElegirVersion(repo, versiones, alCerrar = { elegir = false }) { elegir = false; ir(Pantalla.VerReceta(it)) }
}

@Composable
fun ElegirVersion(repo: Repositorio, versiones: List<Receta>, alCerrar: () -> Unit, alElegir: (String) -> Unit) {
    Dialogo(alCerrar) {
        Surface(Modifier.widthIn(max = 600.dp).fillMaxWidth().padding(12.dp), shape = RoundedCornerShape(20.dp)) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(versiones.first().nombre, style = MaterialTheme.typography.headlineSmall)
                Text("¿Qué versión vas a cocinar?", style = MaterialTheme.typography.bodyLarge)
                Column(Modifier.heightIn(max = 460.dp).verticalScroll(rememberScrollState())) {
                    versiones.forEachIndexed { i, r ->
                        Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable { alElegir(r.id) }.padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically) {
                            Foto(repo, r.foto, Modifier.size(64.dp))
                            Column(Modifier.padding(start = 12.dp)) {
                                Text(nombreVersion(r, i), style = MaterialTheme.typography.titleMedium)
                                val datos = listOfNotNull(r.autor.takeIf { it.isNotBlank() && r.version.isNotBlank() }?.let { "Receta de $it" }, r.tiempo.takeIf { it.isNotBlank() })
                                if (datos.isNotEmpty()) Text(datos.joinToString(" · "), style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
                TextButton(alCerrar, Modifier.fillMaxWidth().height(52.dp)) { Text("Cancelar") }
            }
        }
    }
}
