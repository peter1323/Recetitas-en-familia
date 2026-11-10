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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.recetitas.carpetaTemporal
import com.recetitas.data.Repositorio
import com.recetitas.model.Categoria
import com.recetitas.model.Tema
import com.recetitas.pdf.crearPdf
import com.recetitas.rememberCompartir
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.io.files.Path

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun Marco(titulo: String, volver: () -> Unit, contenido: @Composable () -> Unit) {
    Scaffold(topBar = {
        TopAppBar(title = { Text(titulo) }, navigationIcon = { IconButton(volver) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Volver") } })
    }) { relleno ->
        Box(Modifier.fillMaxSize().padding(relleno), contentAlignment = Alignment.TopCenter) {
            Column(Modifier.widthIn(max = 700.dp).fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) { contenido() }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaAjustes(repo: Repositorio, volver: () -> Unit, importar: () -> Unit, ir: (Pantalla) -> Unit) {
    val a by repo.ajustes.collectAsState()
    val compartir = rememberCompartir()
    val scope = rememberCoroutineScope()
    var trabajando by remember { mutableStateOf(false) }

    Marco("Ajustes", volver) {
        Seccion("Tamaño de letra")
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            listOf(14, 17, 20, 23).forEachIndexed { i, tam ->
                SegmentedButton(a.tamanoLetra == i, { repo.guardarAjustes(a.copy(tamanoLetra = i)) }, SegmentedButtonDefaults.itemShape(i, 4)) {
                    Text("A", fontSize = tam.sp)
                }
            }
        }
        Seccion("Tema")
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            listOf(Tema.CLARO to "Claro", Tema.OSCURO to "Oscuro", Tema.AUTO to "Auto").forEachIndexed { i, (t, nombre) ->
                SegmentedButton(a.tema == t, { repo.guardarAjustes(a.copy(tema = t)) }, SegmentedButtonDefaults.itemShape(i, 3)) { Text(nombre) }
            }
        }
        Espacio(16)
        Interruptor("Pantalla siempre encendida", "Mientras ves una receta, la pantalla no se apaga.", a.pantallaEncendida) {
            repo.guardarAjustes(a.copy(pantallaEncendida = it))
        }
        Interruptor("Copia de seguridad diaria", "Guarda cada día una copia de todas las recetas dentro de la app (las 7 últimas).", a.copiaDiaria) {
            repo.guardarAjustes(a.copy(copiaDiaria = it))
        }
        Fila("Exportar todas las recetas", "Un archivo con todo, para guardarlo fuera o pasarlo a otro móvil u ordenador.") {
            if (trabajando) return@Fila
            trabajando = true
            scope.launch {
                val ruta = withContext(Dispatchers.Default) {
                    Path(carpetaTemporal(), "todas.receta").toString().also { repo.exportar(repo.recetas.value, it, todosLosUtensilios = true) }
                }
                trabajando = false
                compartir(ruta, "Recetitas en familia - copia.receta")
            }
        }
        Fila("Importar recetas o una copia", "Abre un archivo .receta.", importar)
        Fila("Libro de recetas en PDF", "Todas las recetas ordenadas por tipo, con índice.") { ir(Pantalla.Pdf) }
        if (trabajando) CircularProgressIndicator(Modifier.padding(16.dp))
    }
}

@Composable
private fun Interruptor(titulo: String, texto: String, valor: Boolean, alCambiar: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().clickable { alCambiar(!valor) }.padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(titulo, style = MaterialTheme.typography.titleMedium)
            Text(texto, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(valor, alCambiar)
    }
    HorizontalDivider()
}

@Composable
private fun Fila(titulo: String, texto: String, alTocar: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(onClick = alTocar).padding(vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(titulo, style = MaterialTheme.typography.titleMedium)
            Text(texto, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, null)
    }
    HorizontalDivider()
}

@Composable
fun PantallaPdf(repo: Repositorio, volver: () -> Unit) {
    val recetas by repo.recetas.collectAsState()
    var categorias by remember { mutableStateOf(Categoria.entries.toSet()) }
    var soloFavoritas by remember { mutableStateOf(false) }
    var fotosPasos by remember { mutableStateOf(true) }
    var trabajando by remember { mutableStateOf(false) }
    val compartir = rememberCompartir()
    val scope = rememberCoroutineScope()
    val elegidas = recetas.filter { it.categoria in categorias && (!soloFavoritas || it.favorita) }

    Marco("Libro de recetas en PDF", volver) {
        Seccion("Qué incluir")
        Categoria.entries.forEach { c ->
            val n = recetas.count { it.categoria == c }
            Casilla("${c.nombre} ($n)", c in categorias) { categorias = if (it) categorias + c else categorias - c }
        }
        HorizontalDivider(Modifier.padding(vertical = 8.dp))
        Casilla("Solo favoritas", soloFavoritas) { soloFavoritas = it }
        Casilla("Incluir fotos de los pasos y utensilios", fotosPasos) { fotosPasos = it }
        Text("Con fotos queda más completo, pero ocupa más páginas.", style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)

        Button(
            {
                if (trabajando || elegidas.isEmpty()) return@Button
                trabajando = true
                scope.launch {
                    val ruta = withContext(Dispatchers.Default) {
                        escribirTemporal("libro.pdf", crearPdf(elegidas, repo.utensilios.value, fotosPasos, repo::leerFoto))
                    }
                    trabajando = false
                    compartir(ruta, "Recetitas en familia.pdf")
                }
            },
            Modifier.fillMaxWidth().padding(vertical = 20.dp).height(56.dp),
        ) {
            if (trabajando) CircularProgressIndicator(Modifier.height(24.dp), color = MaterialTheme.colorScheme.onPrimary)
            else Text("Crear PDF (${elegidas.size} recetas)", style = MaterialTheme.typography.titleMedium)
        }
        if (elegidas.isEmpty()) Text("No hay recetas con lo que has marcado.", color = MaterialTheme.colorScheme.error)
    }
}

@Composable
private fun Casilla(texto: String, valor: Boolean, alCambiar: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().clickable { alCambiar(!valor) }, verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Checkbox(valor, alCambiar)
        Text(texto, style = MaterialTheme.typography.bodyLarge)
    }
}
