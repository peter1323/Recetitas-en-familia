package com.recetitas.model

import kotlinx.serialization.Serializable
import kotlin.uuid.Uuid

fun nuevoId(): String = Uuid.random().toString()

enum class Categoria(val nombre: String) {
    ENTRANTES("Entrantes"),
    PRIMEROS("Primeros"),
    SEGUNDOS("Segundos"),
    POSTRES("Postres"),
    SALSAS("Salsas"),
    MASAS("Masas y panes"),
    OTROS("Otros"),
}

enum class TipoFuego { NINGUNO, VITRO, GAS, HORNO }
enum class NivelGas(val nombre: String) { BAJO("bajo"), MEDIO("medio"), ALTO("alto") }
enum class ModoHorno(val nombre: String) {
    ARRIBA_ABAJO("arriba y abajo"), VENTILADOR("ventilador"), GRILL("grill")
}

@Serializable
data class Fuego(
    val tipo: TipoFuego = TipoFuego.NINGUNO,
    val vitro: Int = 5,
    val gas: NivelGas = NivelGas.MEDIO,
    val hornoGrados: Int = 180,
    val hornoModo: ModoHorno = ModoHorno.ARRIBA_ABAJO,
) {
    /** Texto corto para la etiqueta del paso, o null si el paso no lleva fuego. */
    fun texto(): String? = when (tipo) {
        TipoFuego.NINGUNO -> null
        TipoFuego.VITRO -> "Vitro $vitro"
        TipoFuego.GAS -> "Gas ${gas.nombre}"
        TipoFuego.HORNO -> "Horno $hornoGrados °C · ${hornoModo.nombre}"
    }
}

@Serializable
data class Paso(
    val texto: String = "",
    val nota: String = "",
    val foto: String? = null,
    val fuego: Fuego = Fuego(),
    val tiempo: String = "",
    /** id de otra receta que se usa en este paso (la mayonesa casera de la ensaladilla). */
    val receta: String? = null,
) {
    /** "Vitro 6 · 3 min", "45 min", o null si no hay nada que mostrar. */
    fun etiqueta(): String? = listOfNotNull(fuego.texto(), tiempo.trim().ifEmpty { null })
        .joinToString(" · ").ifEmpty { null }
}

@Serializable
data class Utensilio(
    val id: String = nuevoId(),
    val nombre: String = "",
    val foto: String? = null,
)

@Serializable
data class Receta(
    val id: String = nuevoId(),
    val nombre: String = "",
    val categoria: Categoria = Categoria.PRIMEROS,
    val autor: String = "",
    val tiempo: String = "",
    val raciones: Int = 4,
    val ingredientes: List<String> = emptyList(),
    val utensilios: List<String> = emptyList(),
    val pasos: List<Paso> = emptyList(),
    val foto: String? = null,
    val fotoOriginal: String? = null,
    val favorita: Boolean = false,
    /** Nombre de esta versión ("De la abuela", "Con huevo"). Las recetas con el mismo nombre son versiones del mismo plato. */
    val version: String = "",
) {
    /** "Empanada de atún (De la abuela)", o solo el nombre si no tiene versión. */
    fun titulo(): String = if (version.isBlank()) nombre else "$nombre ($version)"

    fun fotos(): List<String> = listOfNotNull(foto, fotoOriginal) + pasos.mapNotNull { it.foto }
}

enum class Tema { CLARO, OSCURO, AUTO }

@Serializable
data class Ajustes(
    /** 0..3. Empieza en 2 (grande) a propósito: es más fácil bajarla que encontrar cómo subirla. */
    val tamanoLetra: Int = 2,
    val tema: Tema = Tema.AUTO,
    val pantallaEncendida: Boolean = true,
    val copiaDiaria: Boolean = true,
    val ultimaCopia: Long = 0,
)

val ESCALAS_LETRA = listOf(1.0f, 1.2f, 1.4f, 1.65f)

@Serializable
data class Coleccion(
    val id: String = nuevoId(),
    val nombre: String = "",
    val recetas: List<String> = emptyList(),
)

/** Lo que va dentro de un archivo .receta y de las copias de seguridad. */
@Serializable
data class Paquete(
    val recetas: List<Receta> = emptyList(),
    val utensilios: List<Utensilio> = emptyList(),
    val colecciones: List<Coleccion> = emptyList(),
    val version: Int = 1,
    val app: String = "Recetitas en familia - creada por peter1323",
)
