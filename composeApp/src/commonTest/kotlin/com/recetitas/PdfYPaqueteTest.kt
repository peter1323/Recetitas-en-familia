package com.recetitas

import com.recetitas.data.Repositorio
import com.recetitas.model.Categoria
import com.recetitas.model.Fuego
import com.recetitas.model.Paso
import com.recetitas.model.Receta
import com.recetitas.model.TipoFuego
import com.recetitas.model.Utensilio
import com.recetitas.pdf.Pdf
import com.recetitas.pdf.crearPdf
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PdfYPaqueteTest {
    private val flan = Receta(
        nombre = "Flan de huevo (de la abuela)", categoria = Categoria.POSTRES, autor = "Abuela", raciones = 6,
        ingredientes = listOf("6 huevos", "1 l de leche", "200 g de azúcar"),
        pasos = listOf(Paso("Hacer el caramelo", nota = "Color miel, ¡no oscuro!", fuego = Fuego(TipoFuego.VITRO, vitro = 6), tiempo = "3 min")),
    )

    @Test fun pdfValido() {
        val bytes = crearPdf(listOf(flan, flan.copy(id = "2", nombre = "Lentejas", categoria = Categoria.PRIMEROS)), emptyList(), true) { null }
        val texto = bytes.decodeToString()
        assertTrue(texto.startsWith("%PDF-1.4"))
        assertTrue(texto.trimEnd().endsWith("%%EOF"))
        // portada + índice + 2 recetas
        assertTrue(texto.contains("/Count 4"))
        // la tabla xref apunta de verdad al inicio de cada objeto
        val inicioXref = texto.substringAfterLast("startxref\n").trim().removeSuffix("%%EOF").trim().toInt()
        val entradas = texto.substring(inicioXref).lines().drop(3).takeWhile { it.endsWith(" n ") }
        entradas.forEachIndexed { i, e -> assertTrue(texto.substring(e.take(10).toInt()).startsWith("${i + 1} 0 obj")) }
    }

    @Test fun medidasJpeg() {
        // Cabecera mínima: SOI, APP0 vacío, SOF0 con alto 300 y ancho 400
        val jpeg = byteArrayOf(-1, -40, -1, -32, 0, 2, -1, -64, 0, 11, 8, 1, 44, 1, -112, 3, 0, 0, 0, 0)
        assertEquals(400 to 300, Pdf.tamanoJpeg(jpeg))
    }

    @Test fun compartirYRecibir() {
        val base = Path(carpetaTemporal(), "prueba-${flan.id}")
        val emisor = Repositorio(Path(base, "emisor").toString())
        val foto = emisor.guardarFoto(byteArrayOf(1, 2, 3))
        val olla = Utensilio(nombre = "Olla roja", foto = foto)
        emisor.guardarUtensilio(olla)
        val receta = flan.copy(foto = foto, utensilios = listOf(olla.id))
        emisor.guardarReceta(receta)
        val archivo = Path(base, "flan.receta").toString()
        emisor.exportar(listOf(receta), archivo)

        val receptor = Repositorio(Path(base, "receptor").toString())
        val tmp = Path(base, "tmp").toString()
        val paquete = receptor.leerPaquete(archivo, tmp)!!
        receptor.importar(paquete, tmp)
        assertEquals(listOf(receta), receptor.recetas.value)
        assertEquals(listOf(olla), receptor.utensilios.value)
        assertTrue(receptor.leerFoto(foto)!!.contentEquals(byteArrayOf(1, 2, 3)))
        assertEquals(null, receptor.leerPaquete(Path(base, "emisor", "recetas.json").toString(), tmp))
    }

    @Test fun limpiarFotosSoloBorraHuerfanas() {
        val repo = Repositorio(Path(carpetaTemporal(), "limpiar-${flan.id}").toString())
        val usada = repo.guardarFoto(byteArrayOf(1))
        val huerfana = repo.guardarFoto(byteArrayOf(2))
        repo.guardarReceta(flan.copy(foto = usada))
        repo.limpiarFotos()
        assertTrue(SystemFileSystem.exists(Path(repo.rutaFoto(usada))))
        assertTrue(!SystemFileSystem.exists(Path(repo.rutaFoto(huerfana))))
    }
}
