package com.recetitas.model

import kotlin.math.floor
import kotlin.math.max
import kotlin.math.roundToInt

private val MEDIDAS = setOf("g", "gr", "grs", "gramo", "gramos", "kg", "kilo", "kilos", "ml", "cl", "dl", "l", "litro", "litros")
private val MEDIAS = setOf("cucharada", "cucharadas", "cucharadita", "cucharaditas", "taza", "tazas", "vaso", "vasos", "sobre", "sobres")

// Cantidad al principio de la línea: "200", "1,5", "1.5", "1/2", "1 1/2", "½", "1½"
private val NUMERO = Regex("""^\s*(?:(\d+)\s*(?=\d+/\d+|½))?(?:(\d+)/(\d+)|(½)|(\d+(?:[.,]\d+)?))""")

/**
 * Recalcula la cantidad de una línea de ingrediente ("200 g de azúcar") para otras raciones.
 * Las líneas sin número ("sal al gusto") se devuelven igual.
 */
/** Cantidad del principio de la línea y lo que va detrás ("200 g de azúcar" -> 200.0, " g de azúcar"), o null si no empieza por número. */
private fun leerCantidad(linea: String): Pair<Double, String>? {
    val m = NUMERO.find(linea) ?: return null
    val (entero, num, den, medio, simple) = m.destructured
    var cantidad = (entero.ifEmpty { simple }).replace(',', '.').toDoubleOrNull() ?: 0.0
    if (num.isNotEmpty()) cantidad += num.toDouble() / den.toDouble().coerceAtLeast(1.0)
    if (medio.isNotEmpty()) cantidad += 0.5
    return cantidad to linea.substring(m.range.last + 1)
}

fun escalarIngrediente(linea: String, de: Int, a: Int): String {
    if (de <= 0 || de == a) return linea
    val (cantidad, resto) = leerCantidad(linea) ?: return linea
    val unidad = resto.trim().substringBefore(' ').lowercase().trimEnd('.')
    val nueva = cantidad * a / de
    val texto = when {
        unidad in MEDIDAS -> decimal(redondearMedida(nueva))
        unidad in MEDIAS -> mitades(max(0.5, redondearMitad(nueva)))
        // Cosas que se cuentan (huevos, pechugas): enteros, salvo que la receta ya usara medios
        cantidad < 1 || cantidad != floor(cantidad) -> mitades(max(0.5, redondearMitad(nueva)))
        else -> max(1, nueva.roundToInt()).toString()
    }
    return texto + resto
}

// Unidades que se pueden sumar: nombre en la receta -> (unidad base, cuántas unidades base es)
private val UNIDADES = buildMap {
    listOf("g", "gr", "grs", "gramo", "gramos").forEach { put(it, "g" to 1.0) }
    listOf("kg", "kilo", "kilos").forEach { put(it, "g" to 1000.0) }
    listOf("ml").forEach { put(it, "ml" to 1.0) }
    put("cl", "ml" to 10.0); put("dl", "ml" to 100.0)
    listOf("l", "litro", "litros").forEach { put(it, "ml" to 1000.0) }
    listOf("cucharada", "cucharadita", "taza", "vaso", "sobre", "diente", "lata", "pizca", "rama", "hoja", "bote", "paquete")
        .forEach { put(it, it to 1.0); put(it + "s", it to 1.0) }
}

private fun singular(s: String) = if (s.length > 3 && s.endsWith("s")) s.dropLast(1) else s

/**
 * Junta los ingredientes de varias recetas para la lista de la compra:
 * "200 g de azúcar" + "100 g de azúcar" = "300 g de azúcar"; "1,2 kg de harina" + "300 g de harina" = "1,5 kg de harina".
 * Las líneas sin cantidad ("sal al gusto") se dejan una sola vez.
 */
fun juntarIngredientes(lineas: List<String>): List<String> {
    data class Suma(var cantidad: Double, val unidad: String?, val nombre: String, var decimales: Boolean)
    val sumas = LinkedHashMap<String, Suma>()
    val sinCantidad = LinkedHashMap<String, String>()
    for (linea in lineas.map { it.trim() }.filter { it.isNotEmpty() }) {
        val leida = leerCantidad(linea)
        if (leida == null) { sinCantidad.getOrPut(singular(linea.lowercase())) { linea }; continue }
        val (cantidad, resto) = leida
        val palabra = resto.trim().substringBefore(' ').lowercase().trimEnd('.')
        val unidad = UNIDADES[palabra]
        val nombre = (if (unidad != null) resto.trim().substringAfter(' ', "") else resto.trim())
            .removePrefix("de ").removePrefix("del ").trim()
        val clave = "${unidad?.first}|${nombre.lowercase().split(' ').joinToString(" ") { singular(it) }}"
        val base = cantidad * (unidad?.second ?: 1.0)
        val s = sumas.getOrPut(clave) { Suma(0.0, unidad?.first, nombre, false) }
        s.cantidad += base
        if (cantidad != floor(cantidad)) s.decimales = true
    }
    return sumas.values.map { s ->
        val texto = when (s.unidad) {
            "g" -> if (s.cantidad >= 1000) "${decimal(redondearMedida(s.cantidad / 1000))} kg" else "${decimal(redondearMedida(s.cantidad))} g"
            "ml" -> if (s.cantidad >= 1000) "${decimal(redondearMedida(s.cantidad / 1000))} l" else "${decimal(redondearMedida(s.cantidad))} ml"
            null -> if (s.decimales) mitades(max(0.5, redondearMitad(s.cantidad))) else s.cantidad.roundToInt().toString()
            else -> mitades(max(0.5, redondearMitad(s.cantidad))) + " " + s.unidad + if (s.cantidad > 1) "s" else ""
        }
        val de = if (s.unidad != null && s.nombre.isNotEmpty()) " de " else " "
        (texto + de + s.nombre).trim()
    } + sinCantidad.values
}

private val HORAS = Regex("""(\d+(?:[.,]\d+)?)\s*(?:h|hora|horas)\b""", RegexOption.IGNORE_CASE)
private val MINUTOS = Regex("""(\d+(?:[.,]\d+)?)\s*(?:min|mins|minuto|minutos)\b""", RegexOption.IGNORE_CASE)
private val SEGUNDOS = Regex("""(\d+)\s*(?:s|seg|segs|segundo|segundos)\b""", RegexOption.IGNORE_CASE)

/** Segundos que indica el tiempo de un paso ("1 h 30 min", "4 min por lado"), o null si no hay ("toda la noche"). */
fun segundosDe(tiempo: String): Int? {
    fun suma(r: Regex, factor: Int) = r.findAll(tiempo).sumOf { (it.groupValues[1].replace(',', '.').toDouble() * factor).toInt() }
    val total = suma(HORAS, 3600) + suma(MINUTOS, 60) + suma(SEGUNDOS, 1)
    return total.takeIf { it > 0 }
}

/** 185 -> "3:05", 3725 -> "1:02:05" */
fun textoCuentaAtras(segundos: Long): String {
    val s = segundos.coerceAtLeast(0)
    val (h, m, seg) = Triple(s / 3600, s % 3600 / 60, s % 60)
    fun dos(n: Long) = n.toString().padStart(2, '0')
    return if (h > 0) "$h:${dos(m)}:${dos(seg)}" else "$m:${dos(seg)}"
}

/** Día contado desde el 1/1/1970 → (año, mes, día). Algoritmo de calendario civil de H. Hinnant. */
fun fechaDeDia(dia: Long): Triple<Int, Int, Int> {
    val z = dia + 719468
    val era = (if (z >= 0) z else z - 146096) / 146097
    val doe = z - era * 146097
    val yoe = (doe - doe / 1460 + doe / 36524 - doe / 146096) / 365
    val doy = doe - (365 * yoe + yoe / 4 - yoe / 100)
    val mp = (5 * doy + 2) / 153
    val d = (doy - (153 * mp + 2) / 5 + 1).toInt()
    val m = (if (mp < 10) mp + 3 else mp - 9).toInt()
    return Triple((yoe + era * 400 + if (m <= 2) 1 else 0).toInt(), m, d)
}

/** "11/11/2026" */
fun textoFecha(dia: Long) = fechaDeDia(dia).let { (a, m, d) -> "$d/$m/$a" }

/** "2026-11-11", para nombres de archivo que se ordenan solos. */
fun fechaIso(dia: Long) = fechaDeDia(dia).let { (a, m, d) -> "$a-${m.toString().padStart(2, '0')}-${d.toString().padStart(2, '0')}" }

private fun redondearMedida(v: Double): Double = when {
    v >= 100 -> (v / 5).roundToInt() * 5.0
    v >= 10 -> v.roundToInt().toDouble()
    else -> max(0.1, (v * 10).roundToInt() / 10.0)
}

private fun redondearMitad(v: Double) = (v * 2).roundToInt() / 2.0

private fun decimal(v: Double): String =
    if (v == floor(v)) v.toLong().toString() else v.toString().replace('.', ',')

private fun mitades(v: Double): String {
    val e = floor(v).toLong()
    return when {
        v == floor(v) -> e.toString()
        e == 0L -> "½"
        else -> "$e½"
    }
}
