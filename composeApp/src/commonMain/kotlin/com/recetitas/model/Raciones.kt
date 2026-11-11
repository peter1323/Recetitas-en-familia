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
fun escalarIngrediente(linea: String, de: Int, a: Int): String {
    if (de <= 0 || de == a) return linea
    val m = NUMERO.find(linea) ?: return linea
    val (entero, num, den, medio, simple) = m.destructured
    var cantidad = (entero.ifEmpty { simple }).replace(',', '.').toDoubleOrNull() ?: 0.0
    if (num.isNotEmpty()) cantidad += num.toDouble() / den.toDouble().coerceAtLeast(1.0)
    if (medio.isNotEmpty()) cantidad += 0.5
    val resto = linea.substring(m.range.last + 1)
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
