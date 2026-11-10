package com.recetitas.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.AddCircleOutline
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material.icons.outlined.RemoveCircleOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.recetitas.AtrasSistema
import com.recetitas.data.Repositorio
import com.recetitas.model.Categoria
import com.recetitas.model.Fuego
import com.recetitas.model.ModoHorno
import com.recetitas.model.NivelGas
import com.recetitas.model.Paso
import com.recetitas.model.Receta
import com.recetitas.model.TipoFuego
import com.recetitas.model.Utensilio

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun PantallaEditor(repo: Repositorio, id: String?, categoria: Categoria?, volver: () -> Unit, alGuardar: (String) -> Unit) {
    val original = remember(id) { repo.recetas.value.find { it.id == id } ?: Receta(categoria = categoria ?: Categoria.PRIMEROS, ingredientes = listOf("")) }
    var r by remember(id) { mutableStateOf(original) }
    val utensilios by repo.utensilios.collectAsState()
    var errorNombre by remember { mutableStateOf(false) }
    var salir by remember { mutableStateOf(false) }
    var paso by remember { mutableStateOf<Int?>(null) }  // índice; r.pasos.size = paso nuevo
    var elegirUtensilios by remember { mutableStateOf(false) }

    val intentarVolver = { if (r != original) salir = true else volver() }
    AtrasSistema(true, intentarVolver)

    fun guardar() {
        if (r.nombre.isBlank()) { errorNombre = true; return }
        val limpia = r.copy(nombre = r.nombre.trim(), ingredientes = r.ingredientes.map { it.trim() }.filter { it.isNotEmpty() })
        repo.guardarReceta(limpia)
        alGuardar(limpia.id)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (id == null) "Nueva receta" else "Editar receta") },
                navigationIcon = { IconButton(intentarVolver) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Volver") } },
                actions = { TextButton(::guardar) { Text("Guardar", style = MaterialTheme.typography.titleMedium) } },
            )
        },
    ) { relleno ->
        Box(Modifier.fillMaxSize().padding(relleno).imePadding(), contentAlignment = Alignment.TopCenter) {
            Column(Modifier.widthIn(max = 800.dp).fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
                // ----- Foto principal -----
                if (r.foto != null) FotoConNombre(repo, r.foto, r.nombre, Modifier.fillMaxWidth().height(200.dp))
                else HuecoFoto("Foto del plato", Modifier.fillMaxWidth().height(120.dp))
                Row(Modifier.padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    BotonesFoto(repo, { r = r.copy(foto = it) }, Modifier.weight(1f))
                    if (r.foto != null) IconButton({ r = r.copy(foto = null) }) { Icon(Icons.Outlined.Delete, "Quitar foto") }
                }

                CampoTexto(r.nombre, { r = r.copy(nombre = it); errorNombre = false }, "Nombre de la receta")
                if (errorNombre) Text("Ponle un nombre a la receta.", color = MaterialTheme.colorScheme.error)

                Seccion("Tipo")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Categoria.entries.forEach { c ->
                        FilterChip(r.categoria == c, { r = r.copy(categoria = c) }, label = { Text(c.nombre) },
                            leadingIcon = { Icon(c.icono(), null, Modifier.size(18.dp)) })
                    }
                }

                Espacio(12)
                CampoTexto(r.autor, { r = r.copy(autor = it) }, "De quién es la receta (abuela, mamá...)")
                Espacio(8)
                CampoTexto(r.tiempo, { r = r.copy(tiempo = it) }, "Tiempo total (por ejemplo 50 min)")
                SelectorRaciones(r.raciones) { r = r.copy(raciones = it) }

                // ----- Utensilios -----
                Seccion("Utensilios")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    r.utensilios.mapNotNull { u -> utensilios.find { it.id == u } }.forEach { u ->
                        InputChip(true, { r = r.copy(utensilios = r.utensilios - u.id) }, label = { Text(u.nombre) },
                            avatar = { Foto(repo, u.foto, Modifier.size(24.dp)) },
                            trailingIcon = { Icon(Icons.Outlined.Close, "Quitar ${u.nombre}", Modifier.size(18.dp)) })
                    }
                }
                OutlinedButton({ elegirUtensilios = true }) { Icon(Icons.Outlined.Add, null); Text("  Añadir utensilio") }

                // ----- Ingredientes -----
                Seccion("Ingredientes")
                Text("Una línea por ingrediente, con la cantidad delante (200 g de azúcar). Así se recalcula al cambiar las personas.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                r.ingredientes.forEachIndexed { i, ing ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CampoTexto(ing, { nuevo -> r = r.copy(ingredientes = r.ingredientes.toMutableList().also { it[i] = nuevo }) },
                            "Ingrediente ${i + 1}", Modifier.weight(1f), dictar = true)
                        IconButton({ r = r.copy(ingredientes = r.ingredientes.filterIndexed { j, _ -> j != i }) }) {
                            Icon(Icons.Outlined.Delete, "Quitar ingrediente ${i + 1}")
                        }
                    }
                }
                OutlinedButton({ r = r.copy(ingredientes = r.ingredientes + "") }, Modifier.padding(top = 6.dp)) {
                    Icon(Icons.Outlined.Add, null); Text("  Añadir ingrediente")
                }

                // ----- Pasos -----
                Seccion("Pasos")
                r.pasos.forEachIndexed { i, p ->
                    Card(Modifier.fillMaxWidth().padding(bottom = 8.dp).clickable { paso = i }, shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
                        Row(Modifier.padding(start = 12.dp, top = 4.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text("${i + 1}. ${p.texto}", style = MaterialTheme.typography.bodyLarge)
                                p.etiqueta()?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onTertiaryContainer) }
                            }
                            Column {
                                IconButton({ r = r.copy(pasos = mover(r.pasos, i, -1)) }, Modifier.size(36.dp)) { Icon(Icons.Outlined.KeyboardArrowUp, "Subir paso") }
                                IconButton({ r = r.copy(pasos = mover(r.pasos, i, 1)) }, Modifier.size(36.dp)) { Icon(Icons.Outlined.KeyboardArrowDown, "Bajar paso") }
                            }
                            IconButton({ paso = i }) { Icon(Icons.Outlined.Edit, "Editar paso ${i + 1}") }
                            IconButton({ r = r.copy(pasos = r.pasos.filterIndexed { j, _ -> j != i }) }) { Icon(Icons.Outlined.Delete, "Borrar paso ${i + 1}") }
                        }
                    }
                }
                OutlinedButton({ paso = r.pasos.size }) { Icon(Icons.Outlined.Add, null); Text("  Añadir paso") }

                // ----- Original -----
                Seccion("Foto del cuaderno original")
                Text("Haz una foto a la página escrita a mano: así se guarda también la letra original.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (r.fotoOriginal != null) Foto(repo, r.fotoOriginal, Modifier.fillMaxWidth().height(180.dp).padding(top = 8.dp))
                Row(Modifier.padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    BotonesFoto(repo, { r = r.copy(fotoOriginal = it) }, Modifier.weight(1f))
                    if (r.fotoOriginal != null) IconButton({ r = r.copy(fotoOriginal = null) }) { Icon(Icons.Outlined.Delete, "Quitar foto del cuaderno") }
                }

                Button(::guardar, Modifier.fillMaxWidth().padding(vertical = 20.dp).height(56.dp)) {
                    Text("Guardar receta", style = MaterialTheme.typography.titleMedium)
                }
            }
        }
    }

    paso?.let { i ->
        val actual = r.pasos.getOrNull(i) ?: Paso()
        EditorPaso(repo, actual, if (i < r.pasos.size) "Editar paso ${i + 1}" else "Nuevo paso", alCerrar = { paso = null }) { nuevo ->
            r = r.copy(pasos = if (i < r.pasos.size) r.pasos.toMutableList().also { it[i] = nuevo } else r.pasos + nuevo)
            paso = null
        }
    }

    if (elegirUtensilios) SelectorUtensilios(repo, r.utensilios, alCerrar = { elegirUtensilios = false }) { r = r.copy(utensilios = it) }

    if (salir) AlertDialog(
        onDismissRequest = { salir = false },
        title = { Text("¿Salir sin guardar?") },
        text = { Text("Se perderán los cambios que has hecho.") },
        confirmButton = { Button({ salir = false; guardar() }) { Text("Guardar") } },
        dismissButton = { TextButton({ salir = false; volver() }) { Text("Salir sin guardar") } },
    )
}

private fun <T> mover(lista: List<T>, i: Int, d: Int): List<T> {
    val j = i + d
    if (j !in lista.indices) return lista
    return lista.toMutableList().also { val t = it[i]; it[i] = it[j]; it[j] = t }
}

/** Menú para editar un paso: texto, fuego, tiempo, foto y nota. */
@Composable
fun EditorPaso(repo: Repositorio, inicial: Paso, titulo: String, alCerrar: () -> Unit, alGuardar: (Paso) -> Unit) {
    var p by remember { mutableStateOf(inicial) }
    var error by remember { mutableStateOf(false) }
    Dialog(alCerrar, DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.widthIn(max = 700.dp).fillMaxWidth().padding(12.dp), shape = RoundedCornerShape(20.dp)) {
            Column(Modifier.verticalScroll(rememberScrollState()).padding(16.dp).imePadding(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(titulo, style = MaterialTheme.typography.headlineSmall)
                CampoTexto(p.texto, { p = p.copy(texto = it); error = false }, "Qué hay que hacer", lineas = 3, dictar = true)
                if (error) Text("Escribe qué hay que hacer en este paso.", color = MaterialTheme.colorScheme.error)

                Text("Fuego", style = MaterialTheme.typography.titleMedium)
                val f = p.fuego
                Segmentos(listOf("Nada", "Vitro", "Gas", "Horno"), f.tipo.ordinal) { p = p.copy(fuego = f.copy(tipo = TipoFuego.entries[it])) }
                when (f.tipo) {
                    TipoFuego.VITRO -> Contador("${f.vitro}", { p = p.copy(fuego = f.copy(vitro = (f.vitro - 1).coerceAtLeast(1))) }) {
                        p = p.copy(fuego = f.copy(vitro = (f.vitro + 1).coerceAtMost(12)))
                    }
                    TipoFuego.GAS -> Segmentos(NivelGas.entries.map { it.nombre.replaceFirstChar(Char::uppercase) }, f.gas.ordinal) {
                        p = p.copy(fuego = f.copy(gas = NivelGas.entries[it]))
                    }
                    TipoFuego.HORNO -> {
                        Contador("${f.hornoGrados} °C", { p = p.copy(fuego = f.copy(hornoGrados = (f.hornoGrados - 10).coerceAtLeast(50))) }) {
                            p = p.copy(fuego = f.copy(hornoGrados = (f.hornoGrados + 10).coerceAtMost(300)))
                        }
                        Segmentos(ModoHorno.entries.map { it.nombre.replaceFirstChar(Char::uppercase) }, f.hornoModo.ordinal) {
                            p = p.copy(fuego = f.copy(hornoModo = ModoHorno.entries[it]))
                        }
                    }
                    TipoFuego.NINGUNO -> {}
                }
                CampoTexto(p.tiempo, { p = p.copy(tiempo = it) }, "Tiempo (por ejemplo 3 min)")

                Text("Foto del paso", style = MaterialTheme.typography.titleMedium)
                if (p.foto != null) Foto(repo, p.foto, Modifier.fillMaxWidth().height(160.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    BotonesFoto(repo, { p = p.copy(foto = it) }, Modifier.weight(1f))
                    if (p.foto != null) IconButton({ p = p.copy(foto = null) }) { Icon(Icons.Outlined.Delete, "Quitar foto") }
                }
                CampoTexto(p.nota, { p = p.copy(nota = it) }, "Nota para aclarar (opcional)", lineas = 2, dictar = true)

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    TextButton(alCerrar, Modifier.weight(1f).height(52.dp)) { Text("Cancelar") }
                    Button({ if (p.texto.isBlank()) error = true else alGuardar(p.copy(texto = p.texto.trim(), nota = p.nota.trim())) },
                        Modifier.weight(1f).height(52.dp)) { Text("Guardar") }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun Segmentos(opciones: List<String>, elegida: Int, alElegir: (Int) -> Unit) {
    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
        opciones.forEachIndexed { i, o ->
            SegmentedButton(i == elegida, { alElegir(i) }, SegmentedButtonDefaults.itemShape(i, opciones.size)) { Text(o, maxLines = 1) }
        }
    }
}

@Composable
private fun Contador(valor: String, menos: () -> Unit, mas: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(MaterialTheme.colorScheme.tertiaryContainer),
        horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically,
    ) {
        val c = MaterialTheme.colorScheme.onTertiaryContainer
        IconButton(menos) { Icon(Icons.Outlined.RemoveCircleOutline, "Menos", Modifier.size(32.dp), tint = c) }
        Text(valor, style = MaterialTheme.typography.headlineMedium, color = c, modifier = Modifier.padding(horizontal = 20.dp))
        IconButton(mas) { Icon(Icons.Outlined.AddCircleOutline, "Más", Modifier.size(32.dp), tint = c) }
    }
}

/** Lista común de utensilios: se eligen los de esta receta o se crea uno nuevo con su foto. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SelectorUtensilios(repo: Repositorio, elegidos: List<String>, alCerrar: () -> Unit, alCambiar: (List<String>) -> Unit) {
    val todos by repo.utensilios.collectAsState()
    var sel by remember { mutableStateOf(elegidos) }
    var nombre by remember { mutableStateOf("") }
    var foto by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf(false) }
    Dialog(alCerrar, DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.widthIn(max = 700.dp).fillMaxWidth().padding(12.dp), shape = RoundedCornerShape(20.dp)) {
            Column(Modifier.verticalScroll(rememberScrollState()).padding(16.dp).imePadding(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Tus utensilios", style = MaterialTheme.typography.headlineSmall)
                if (todos.isEmpty()) Text("Aún no hay ninguno. Crea el primero abajo.")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    todos.forEach { u -> TarjetaUtensilio(repo, u, u.id in sel) { sel = if (u.id in sel) sel - u.id else sel + u.id } }
                }

                Text("Nuevo utensilio", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 8.dp))
                CampoTexto(nombre, { nombre = it; error = false }, "Nombre (sartén grande, olla roja...)")
                if (foto != null) Foto(repo, foto, Modifier.size(120.dp))
                BotonesFoto(repo, { foto = it })
                if (error) Text("Ponle un nombre al utensilio.", color = MaterialTheme.colorScheme.error)
                OutlinedButton({
                    if (nombre.isBlank()) { error = true; return@OutlinedButton }
                    val u = Utensilio(nombre = nombre.trim(), foto = foto)
                    repo.guardarUtensilio(u)
                    sel = sel + u.id; nombre = ""; foto = null
                }) { Icon(Icons.Outlined.Add, null); Text("  Crear y añadir") }

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    TextButton(alCerrar, Modifier.weight(1f).height(52.dp)) { Text("Cancelar") }
                    Button({ alCambiar(sel); alCerrar() }, Modifier.weight(1f).height(52.dp)) { Text("Listo") }
                }
            }
        }
    }
}

@Composable
private fun TarjetaUtensilio(repo: Repositorio, u: Utensilio, elegido: Boolean, alTocar: () -> Unit) {
    val borde = if (elegido) BorderStroke(3.dp, MaterialTheme.colorScheme.primary) else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    Column(Modifier.width(110.dp).clip(RoundedCornerShape(14.dp)).border(borde, RoundedCornerShape(14.dp)).clickable(onClick = alTocar).padding(6.dp)) {
        Box {
            Foto(repo, u.foto, Modifier.fillMaxWidth().heightIn(min = 90.dp, max = 90.dp))
            if (elegido) Icon(Icons.Outlined.CheckCircle, "Elegido", Modifier.align(Alignment.TopEnd).padding(4.dp)
                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(50)), tint = MaterialTheme.colorScheme.primary)
        }
        Text(u.nombre, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 4.dp))
    }
}
