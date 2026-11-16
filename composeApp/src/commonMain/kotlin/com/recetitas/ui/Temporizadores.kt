package com.recetitas.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.recetitas.model.textoCuentaAtras
import com.recetitas.sonarAlarma
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlin.time.Clock

data class Temporizador(val id: String, val texto: String, val finMs: Long)

private fun ahoraMs() = Clock.System.now().toEpochMilliseconds()

/**
 * Temporizadores de los pasos. Siguen contando aunque cambies de pantalla.
 * shortcut: no es una alarma del sistema; si Android cierra la app del todo, se pierden. Para eso haría falta AlarmManager.
 */
object Temporizadores {
    val lista = MutableStateFlow<List<Temporizador>>(emptyList())

    /** Empieza (o vuelve a empezar) la cuenta atrás de un paso. */
    fun empezar(id: String, texto: String, segundos: Int) {
        lista.value = lista.value.filterNot { it.id == id } + Temporizador(id, texto, ahoraMs() + segundos * 1000L)
    }

    fun parar(id: String) { lista.value = lista.value.filterNot { it.id == id } }
}

/**
 * Tarjetas con la cuenta atrás y aviso al terminar. Se pone una vez en App, encima de todas las pantallas.
 */
@Composable
fun CapaTemporizadores(modifier: Modifier = Modifier) {
    val activos by Temporizadores.lista.collectAsState()
    var ahora by remember { mutableLongStateOf(ahoraMs()) }
    var terminado by remember { mutableStateOf<Temporizador?>(null) }
    var pararSonido by remember { mutableStateOf<(() -> Unit)?>(null) }

    LaunchedEffect(activos) {
        while (activos.isNotEmpty()) {
            ahora = ahoraMs()
            activos.firstOrNull { it.finMs <= ahora }?.let { t ->
                Temporizadores.parar(t.id)
                pararSonido?.invoke()
                pararSonido = sonarAlarma()
                terminado = t
                return@LaunchedEffect
            }
            delay(250)
        }
    }

    Column(modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        activos.forEach { t ->
            Row(
                Modifier.widthIn(max = 500.dp).clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.tertiaryContainer).padding(start = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Outlined.Timer, null, tint = MaterialTheme.colorScheme.onTertiaryContainer)
                Text("  ${textoCuentaAtras((t.finMs - ahora + 999) / 1000)}", style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onTertiaryContainer)
                Text("  ${t.texto}", style = MaterialTheme.typography.bodyMedium, maxLines = 1,
                    color = MaterialTheme.colorScheme.onTertiaryContainer, modifier = Modifier.weight(1f, fill = false))
                IconButton({ Temporizadores.parar(t.id) }) {
                    Icon(Icons.Outlined.Close, "Parar temporizador", tint = MaterialTheme.colorScheme.onTertiaryContainer, modifier = Modifier.size(22.dp))
                }
            }
        }
    }

    terminado?.let { t ->
        val cerrar = { pararSonido?.invoke(); pararSonido = null; terminado = null }
        Aviso(
            onDismissRequest = cerrar,
            title = { Text("¡Tiempo!") },
            text = { Text(t.texto, style = MaterialTheme.typography.titleMedium) },
            confirmButton = { Button(cerrar) { Text("Parar alarma") } },
        )
    }
}
