package com.recetitas.pdf

import com.recetitas.AUTOR
import com.recetitas.model.Categoria
import com.recetitas.model.Receta
import com.recetitas.model.Utensilio
import com.recetitas.pdf.Pdf.Fuente

private const val MARGEN = 50f
private const val NARANJA = 0xBA7517
private const val TEJA = 0x993C1D
private const val GRIS = 0x5F5E5A
private const val CREMA = 0xFAEEDA

/**
 * Crea el PDF de una o varias recetas, ordenadas por categoría.
 * Con más de una receta añade portada e índice con números de página.
 */
fun crearPdf(
    recetas: List<Receta>,
    utensilios: List<Utensilio>,
    fotosPasos: Boolean,
    leerFoto: (String) -> ByteArray?,
): ByteArray {
    val ordenadas = recetas.sortedWith(compareBy({ it.categoria.ordinal }, { it.nombre.lowercase() }))
    val libro = ordenadas.size > 1

    // Se reservan en blanco la portada y el índice, se maquetan las recetas (así se sabe en qué página
    // empieza cada una) y al final se vuelve atrás a rellenar portada e índice.
    val categorias = ordenadas.map { it.categoria }.distinct().size
    val paginasIndice = if (libro) (ordenadas.size + 2 * categorias + 29) / 30 else 0

    val pdf = Pdf(if (libro) "Recetitas en familia" else ordenadas.firstOrNull()?.nombre ?: "Receta")
    if (libro) repeat(1 + paginasIndice) { pdf.nuevaPagina() }
    val inicio = mutableMapOf<String, Int>()
    for (r in ordenadas) {
        pdf.nuevaPagina()
        inicio[r.id] = pdf.numPaginas
        Maquetador(pdf, leerFoto).receta(r, utensilios, fotosPasos)
    }
    if (libro) {
        pdf.irAPagina(1); portada(pdf, ordenadas.size)
        indice(pdf, ordenadas, inicio, paginasIndice)
    }
    return pdf.bytes()
}

private fun portada(pdf: Pdf, total: Int) {
    pdf.rect(0f, 0f, pdf.ancho, pdf.alto, CREMA)
    val titulo = "Recetitas en familia"
    pdf.texto(titulo, (pdf.ancho - pdf.anchoTexto(titulo, 40f, Fuente.NEGRITA)) / 2, 380f, 40f, Fuente.NEGRITA, TEJA)
    val sub = "$total recetas"
    pdf.texto(sub, (pdf.ancho - pdf.anchoTexto(sub, 18f)) / 2, 420f, 18f, Fuente.NORMAL, GRIS)
    val pie = "Hecho con Recetitas en familia, app creada por $AUTOR"
    pdf.texto(pie, (pdf.ancho - pdf.anchoTexto(pie, 10f)) / 2, pdf.alto - 40, 10f, Fuente.NORMAL, GRIS)
}

private fun indice(pdf: Pdf, recetas: List<Receta>, inicio: Map<String, Int>, paginas: Int) {
    var y = 0f
    var hechas = 0
    fun pagina() {
        // Las páginas del índice ya existen (2, 3...); si no caben, se escribe apretado en la última
        if (hechas < paginas) { hechas++; pdf.irAPagina(1 + hechas) }
        y = MARGEN + 30; pdf.texto("Índice", MARGEN, y, 24f, Fuente.NEGRITA, TEJA); y += 30
    }
    pagina()
    var cat: Categoria? = null
    for (r in recetas) {
        if (r.categoria != cat) {
            cat = r.categoria
            if (y > pdf.alto - MARGEN - 40) pagina()
            y += 8
            pdf.texto(cat.nombre, MARGEN, y, 15f, Fuente.NEGRITA, NARANJA); y += 22
        }
        if (y > pdf.alto - MARGEN) pagina()
        val num = inicio.getValue(r.id).toString()
        val nombre = r.nombre.ifBlank { "Sin nombre" }
        pdf.texto(nombre, MARGEN + 12, y, 12f)
        pdf.texto(num, pdf.ancho - MARGEN - pdf.anchoTexto(num, 12f), y, 12f, Fuente.NORMAL, GRIS)
        y += 20
    }
}

private class Maquetador(val pdf: Pdf, val leerFoto: (String) -> ByteArray?) {
    var y = MARGEN
    val anchoUtil = pdf.ancho - 2 * MARGEN

    fun pie() {
        val n = pdf.numPaginas.toString()
        pdf.texto(n, (pdf.ancho - pdf.anchoTexto(n, 10f)) / 2, pdf.alto - 25, 10f, Fuente.NORMAL, GRIS)
    }

    fun sitio(h: Float) {
        if (y + h > pdf.alto - MARGEN) { pie(); pdf.nuevaPagina(); y = MARGEN }
    }

    fun parrafo(t: String, tam: Float, f: Fuente = Fuente.NORMAL, color: Int = 0x2C2C2A, sangria: Float = 0f) {
        for (l in pdf.partir(t, tam, anchoUtil - sangria, f)) {
            sitio(tam * 1.45f)
            y += tam
            pdf.texto(l, MARGEN + sangria, y, tam, f, color)
            y += tam * 0.45f
        }
    }

    fun titulo(t: String) { y += 14; sitio(40f); parrafo(t, 16f, Fuente.NEGRITA, TEJA); y += 4 }

    fun receta(r: Receta, utensilios: List<Utensilio>, fotosPasos: Boolean) {
        parrafo(r.categoria.nombre.uppercase(), 10f, Fuente.NEGRITA, NARANJA)
        y += 4
        val foto = r.foto?.let(leerFoto)
        if (foto != null && Pdf.tamanoJpeg(foto) != null) {
            val h = 230f
            pdf.imagen(foto, MARGEN, y, anchoUtil, h)
            // Nombre encima de la foto, sobre una franja oscura
            val lineas = pdf.partir(r.nombre, 22f, anchoUtil - 24, Fuente.NEGRITA)
            val franja = 16f + lineas.size * 27f
            pdf.rect(MARGEN, y + h - franja, anchoUtil, franja, 0x2C2C2A, opaco = false)
            var ty = y + h - franja + 8
            lineas.forEach { ty += 22f; pdf.texto(it, MARGEN + 12, ty, 22f, Fuente.NEGRITA, 0xFFFFFF); ty += 5f }
            y += h + 14
        } else {
            parrafo(r.nombre.ifBlank { "Sin nombre" }, 24f, Fuente.NEGRITA, TEJA)
            y += 6
        }
        val datos = listOfNotNull(
            r.autor.takeIf { it.isNotBlank() }?.let { "Receta de $it" },
            r.tiempo.takeIf { it.isNotBlank() },
            "Para ${r.raciones} personas",
        ).joinToString("  ·  ")
        parrafo(datos, 11f, Fuente.NORMAL, GRIS)

        val us = r.utensilios.mapNotNull { id -> utensilios.find { it.id == id } }
        if (us.isNotEmpty()) {
            titulo("Utensilios")
            if (fotosPasos && us.any { it.foto != null }) {
                val lado = 70f
                var x = MARGEN
                sitio(lado + 20)
                for (u in us) {
                    if (x + lado > pdf.ancho - MARGEN) { x = MARGEN; y += lado + 22; sitio(lado + 20) }
                    u.foto?.let(leerFoto)?.let { pdf.imagen(it, x, y, lado, lado) } ?: pdf.rect(x, y, lado, lado, CREMA)
                    pdf.texto(pdf.partir(u.nombre, 9f, lado).first(), x, y + lado + 11, 9f)
                    x += lado + 10
                }
                y += lado + 22
            } else parrafo(us.joinToString(", ") { it.nombre }, 12f)
        }

        if (r.ingredientes.isNotEmpty()) {
            titulo("Ingredientes")
            r.ingredientes.filter { it.isNotBlank() }.forEach { parrafo("•  $it", 12f, sangria = 6f) }
        }

        if (r.pasos.isNotEmpty()) {
            titulo("Pasos")
            r.pasos.forEachIndexed { i, p ->
                y += 4
                parrafo("${i + 1}.  ${p.texto}", 12f)
                p.etiqueta()?.let { parrafo(it, 11f, Fuente.NEGRITA, TEJA, sangria = 18f) }
                if (p.nota.isNotBlank()) parrafo("Nota: ${p.nota}", 11f, Fuente.CURSIVA, NARANJA, sangria = 18f)
                if (fotosPasos) p.foto?.let(leerFoto)?.let {
                    sitio(150f); y += 4
                    pdf.imagen(it, MARGEN + 18, y, 200f, 140f); y += 146
                }
            }
        }
        pie()
    }
}
