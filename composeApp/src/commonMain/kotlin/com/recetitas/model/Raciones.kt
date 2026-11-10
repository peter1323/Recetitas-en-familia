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
