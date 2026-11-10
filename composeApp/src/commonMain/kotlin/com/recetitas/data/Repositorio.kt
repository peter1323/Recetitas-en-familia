package com.recetitas.data

import com.recetitas.crearZip
import com.recetitas.extraerZip
import com.recetitas.model.Ajustes
import com.recetitas.model.Paquete
import com.recetitas.model.Receta
import com.recetitas.model.Utensilio
import com.recetitas.model.nuevoId
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.io.readByteArray
import kotlinx.io.readString
import kotlinx.io.write
import kotlinx.io.writeString
import kotlinx.serialization.json.Json
import kotlin.time.Clock

/**
 * Guarda recetas, utensilios y ajustes en archivos JSON dentro de [carpeta], y las fotos en carpeta/fotos.
 * Las fotos se guardan por nombre de archivo, no con la ruta completa, para que las copias funcionen en otro dispositivo.
 */
class Repositorio(private val carpeta: String) {

    private val json = Json { prettyPrint = true; ignoreUnknownKeys = true; encodeDefaults = true }
    private val fs = SystemFileSystem
    private val archivoRecetas = Path(carpeta, "recetas.json")
    private val archivoUtensilios = Path(carpeta, "utensilios.json")
    private val archivoAjustes = Path(carpeta, "ajustes.json")
    val carpetaFotos = Path(carpeta, "fotos")
    private val carpetaCopias = Path(carpeta, "copias")

    init {
        fs.createDirectories(carpetaFotos)
        fs.createDirectories(carpetaCopias)
    }

    private val _recetas = MutableStateFlow(cargar<List<Receta>>(archivoRecetas) ?: emptyList())
    val recetas: StateFlow<List<Receta>> = _recetas.asStateFlow()

    private val _utensilios = MutableStateFlow(cargar<List<Utensilio>>(archivoUtensilios) ?: emptyList())
    val utensilios: StateFlow<List<Utensilio>> = _utensilios.asStateFlow()

    private val _ajustes = MutableStateFlow(cargar<Ajustes>(archivoAjustes) ?: Ajustes())
    val ajustes: StateFlow<Ajustes> = _ajustes.asStateFlow()

    private inline fun <reified T> cargar(ruta: Path): T? {
        if (!fs.exists(ruta)) return null
        val texto = fs.source(ruta).buffered().use { it.readString() }
        return try {
            json.decodeFromString<T>(texto)
        } catch (e: Exception) {
            // Archivo dañado: se aparta para no perderlo y se empieza vacío
            fs.atomicMove(ruta, Path("$ruta.dañado-${Clock.System.now().toEpochMilliseconds()}"))
            null
        }
    }

    /** Escribe en un temporal y lo renombra: si la app se cierra a mitad, el archivo anterior sigue intacto. */
    private fun escribir(destino: Path, texto: String) {
        val temporal = Path("$destino.tmp")
        fs.sink(temporal).buffered().use { it.writeString(texto) }
        fs.atomicMove(temporal, destino)
    }

    fun guardarReceta(receta: Receta) {
        val lista = _recetas.value
        _recetas.value = if (lista.any { it.id == receta.id }) lista.map { if (it.id == receta.id) receta else it } else lista + receta
        escribir(archivoRecetas, json.encodeToString(_recetas.value))
    }

    fun borrarReceta(id: String) {
        _recetas.value = _recetas.value.filterNot { it.id == id }
        escribir(archivoRecetas, json.encodeToString(_recetas.value))
    }

    fun guardarUtensilio(u: Utensilio) {
        val lista = _utensilios.value
        _utensilios.value = if (lista.any { it.id == u.id }) lista.map { if (it.id == u.id) u else it } else lista + u
        escribir(archivoUtensilios, json.encodeToString(_utensilios.value))
    }

    fun borrarUtensilio(id: String) {
        _utensilios.value = _utensilios.value.filterNot { it.id == id }
        escribir(archivoUtensilios, json.encodeToString(_utensilios.value))
        _recetas.value.filter { id in it.utensilios }.forEach { guardarReceta(it.copy(utensilios = it.utensilios - id)) }
    }

    fun guardarAjustes(a: Ajustes) {
        _ajustes.value = a
        escribir(archivoAjustes, json.encodeToString(a))
    }

    // ---------- FOTOS ----------

    fun rutaFoto(nombre: String): String = Path(carpetaFotos, nombre).toString()

    fun leerFoto(nombre: String): ByteArray? = runCatching {
        fs.source(Path(carpetaFotos, nombre)).buffered().use { it.readByteArray() }
    }.getOrNull()

    /** Guarda una foto ya preparada (JPEG) y devuelve su nombre. */
    fun guardarFoto(jpeg: ByteArray): String {
        val nombre = "${nuevoId()}.jpg"
        fs.sink(Path(carpetaFotos, nombre)).buffered().use { it.write(jpeg) }
        return nombre
    }

    /** Borra las fotos que ya no usa ninguna receta ni utensilio. Solo al arrancar, para no tocar una edición a medias. */
    fun limpiarFotos() {
        val usadas = _recetas.value.flatMap { it.fotos() }.toSet() + _utensilios.value.mapNotNull { it.foto }
        fs.list(carpetaFotos).filter { it.name !in usadas }.forEach { fs.delete(it) }
    }

    // ---------- COMPARTIR / COPIAS ----------

    /** Crea un .receta (zip con paquete.json y sus fotos) con estas recetas y los utensilios que usan. */
    fun exportar(recetas: List<Receta>, destino: String, todosLosUtensilios: Boolean = false) {
        val ids = recetas.flatMap { it.utensilios }.toSet()
        val utensilios = _utensilios.value.filter { todosLosUtensilios || it.id in ids }
        val paquete = Paquete(recetas, utensilios)
        val temporal = Path(carpeta, "paquete.json")
        escribir(temporal, json.encodeToString(paquete))
        val fotos = (recetas.flatMap { it.fotos() } + utensilios.mapNotNull { it.foto }).toSet()
            .filter { fs.exists(Path(carpetaFotos, it)) }
        crearZip(destino, mapOf("paquete.json" to temporal.toString()) + fotos.associate { "fotos/$it" to rutaFoto(it) })
        fs.delete(temporal)
    }

    /** Lee un .receta o una copia. Devuelve null si el archivo no es válido. Las fotos quedan en [temporal]/fotos. */
    fun leerPaquete(origen: String, temporal: String): Paquete? = runCatching {
        if (fs.exists(Path(temporal))) borrarCarpeta(Path(temporal))
        fs.createDirectories(Path(temporal))
        extraerZip(origen, temporal)
        val texto = fs.source(Path(temporal, "paquete.json")).buffered().use { it.readString() }
        json.decodeFromString<Paquete>(texto)
    }.getOrNull()

    /** Guarda lo leído con [leerPaquete]. Las recetas con el mismo id se actualizan; los utensilios que ya tienes se quedan como están. */
    fun importar(paquete: Paquete, temporal: String) {
        val fotosTmp = Path(temporal, "fotos")
        if (fs.exists(fotosTmp)) fs.list(fotosTmp).forEach { f ->
            val destino = Path(carpetaFotos, f.name)
            if (!fs.exists(destino)) fs.atomicMove(f, destino)
        }
        val misUtensilios = _utensilios.value.map { it.id }.toSet()
        paquete.utensilios.filter { it.id !in misUtensilios }.forEach { guardarUtensilio(it) }
        paquete.recetas.forEach { guardarReceta(it) }
        borrarCarpeta(Path(temporal))
    }

    /** Copia diaria automática en carpeta/copias. Guarda las 7 últimas. */
    fun copiaDiariaSiToca() {
        val a = _ajustes.value
        val hoy = Clock.System.now().toEpochMilliseconds() / 86_400_000
        if (!a.copiaDiaria || a.ultimaCopia == hoy || _recetas.value.isEmpty()) return
        exportar(_recetas.value, Path(carpetaCopias, "copia-$hoy.receta").toString(), todosLosUtensilios = true)
        guardarAjustes(a.copy(ultimaCopia = hoy))
        fs.list(carpetaCopias).sortedBy { it.name }.dropLast(7).forEach { fs.delete(it) }
    }

    private fun borrarCarpeta(p: Path) {
        if (fs.metadataOrNull(p)?.isDirectory == true) fs.list(p).forEach { borrarCarpeta(it) }
        fs.delete(p, mustExist = false)
    }
}
