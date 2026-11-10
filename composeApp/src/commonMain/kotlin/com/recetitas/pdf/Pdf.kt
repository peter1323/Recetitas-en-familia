package com.recetitas.pdf

import com.recetitas.AUTOR
import kotlinx.io.Buffer
import kotlinx.io.readByteArray
import kotlinx.io.write

/**
 * Escritor de PDF mínimo: texto con las fuentes estándar Helvetica (acentos y ñ incluidos),
 * rectángulos de color e imágenes JPEG. Es igual en Android y en Windows, sin librerías.
 * Medidas en puntos (A4 = 595 x 842), origen arriba a la izquierda (se da la vuelta al escribir).
 */
class Pdf(private val titulo: String = "Recetitas en familia") {
    val ancho = 595f
    val alto = 842f

    private val paginas = mutableListOf<StringBuilder>()
    private val imagenes = mutableListOf<Triple<ByteArray, Int, Int>>()
    private var indice = -1
    private val actual get() = paginas[indice]
    val numPaginas get() = paginas.size

    fun nuevaPagina() { paginas += StringBuilder(); indice = paginas.lastIndex }

    /** Vuelve a una página ya creada (1 = la primera) para dibujar en ella. */
    fun irAPagina(n: Int) { indice = n - 1 }

    enum class Fuente(val id: String, val anchos: IntArray) {
        NORMAL("F1", ANCHOS_NORMAL), NEGRITA("F2", ANCHOS_NEGRITA), CURSIVA("F3", ANCHOS_NORMAL)
    }

    fun anchoTexto(t: String, tam: Float, f: Fuente = Fuente.NORMAL): Float =
        t.sumOf { f.anchos[base(it).code.coerceIn(32, 126) - 32] } * tam / 1000f

    fun texto(t: String, x: Float, y: Float, tam: Float, f: Fuente = Fuente.NORMAL, color: Int = 0x2C2C2A) {
        actual.append("${rgb(color)} rg BT /${f.id} ${n(tam)} Tf ${n(x)} ${n(alto - y)} Td (${escapar(t)}) Tj ET\n")
    }

    fun rect(x: Float, y: Float, w: Float, h: Float, color: Int, opaco: Boolean = true) {
        val gs = if (opaco) "" else "/GS1 gs "
        actual.append("q $gs${rgb(color)} rg ${n(x)} ${n(alto - y - h)} ${n(w)} ${n(h)} re f Q\n")
    }

    /** Dibuja un JPEG ajustado al rectángulo, recortando lo que sobre (como "cover"). */
    fun imagen(jpeg: ByteArray, x: Float, y: Float, w: Float, h: Float) {
        val (iw, ih) = tamanoJpeg(jpeg) ?: return
        imagenes += Triple(jpeg, iw, ih)
        val escala = maxOf(w / iw, h / ih)
        val dw = iw * escala; val dh = ih * escala
        val dx = x + (w - dw) / 2; val dy = y + (h - dh) / 2
        actual.append("q ${n(x)} ${n(alto - y - h)} ${n(w)} ${n(h)} re W n ")
        actual.append("${n(dw)} 0 0 ${n(dh)} ${n(dx)} ${n(alto - dy - dh)} cm /Im${imagenes.size} Do Q\n")
    }

    /** Parte el texto en líneas que caben en [maxAncho]. */
    fun partir(t: String, tam: Float, maxAncho: Float, f: Fuente = Fuente.NORMAL): List<String> =
        t.split('\n').flatMap { parrafo ->
            val lineas = mutableListOf<String>()
            var linea = ""
            for (palabra in parrafo.split(' ')) {
                val prueba = if (linea.isEmpty()) palabra else "$linea $palabra"
                if (anchoTexto(prueba, tam, f) <= maxAncho || linea.isEmpty()) linea = prueba
                else { lineas += linea; linea = palabra }
            }
            lineas + linea
        }

    fun bytes(): ByteArray {
        val out = Buffer()
        val offsets = mutableListOf<Long>()
        fun obj(contenido: String) { offsets += out.size; out.write(latin1("${offsets.size} 0 obj\n$contenido\nendobj\n")) }

        out.write(latin1("%PDF-1.4\n%âãÏÓ\n"))
        // 1 catálogo, 2 páginas, 3-5 fuentes, 6 transparencia; luego imágenes; luego páginas y contenidos
        val primeraImagen = 7
        val primeraPagina = primeraImagen + imagenes.size
        val kids = paginas.indices.joinToString(" ") { "${primeraPagina + it * 2} 0 R" }
        obj("<< /Type /Catalog /Pages 2 0 R >>")
        obj("<< /Type /Pages /Kids [$kids] /Count ${paginas.size} >>")
        listOf("Helvetica", "Helvetica-Bold", "Helvetica-Oblique").forEach {
            obj("<< /Type /Font /Subtype /Type1 /BaseFont /$it /Encoding /WinAnsiEncoding >>")
        }
        obj("<< /Type /ExtGState /ca 0.65 >>")
        imagenes.forEach { (jpeg, w, h) ->
            offsets += out.size
            out.write(latin1("${offsets.size} 0 obj\n<< /Type /XObject /Subtype /Image /Width $w /Height $h /ColorSpace /DeviceRGB /BitsPerComponent 8 /Filter /DCTDecode /Length ${jpeg.size} >>\nstream\n"))
            out.write(jpeg)
            out.write(latin1("\nendstream\nendobj\n"))
        }
        val xobjs = imagenes.indices.joinToString(" ") { "/Im${it + 1} ${primeraImagen + it} 0 R" }
        paginas.forEachIndexed { i, contenido ->
            obj("<< /Type /Page /Parent 2 0 R /MediaBox [0 0 595 842] /Contents ${primeraPagina + i * 2 + 1} 0 R " +
                "/Resources << /Font << /F1 3 0 R /F2 4 0 R /F3 5 0 R >> /ExtGState << /GS1 6 0 R >> /XObject << $xobjs >> >> >>")
            val datos = latin1(contenido.toString())
            offsets += out.size
            out.write(latin1("${offsets.size} 0 obj\n<< /Length ${datos.size} >>\nstream\n"))
            out.write(datos)
            out.write(latin1("\nendstream\nendobj\n"))
        }
        // Metadatos del documento (Archivo > Propiedades en cualquier lector de PDF)
        obj("<< /Title (${escapar(titulo)}) /Author (${escapar(AUTOR)}) /Creator (Recetitas en familia - $AUTOR) /Producer (Recetitas en familia) >>")
        val inicioXref = out.size
        val xref = StringBuilder("xref\n0 ${offsets.size + 1}\n0000000000 65535 f \n")
        offsets.forEach { xref.append(it.toString().padStart(10, '0')).append(" 00000 n \n") }
        xref.append("trailer\n<< /Size ${offsets.size + 1} /Root 1 0 R /Info ${offsets.size} 0 R >>\nstartxref\n$inicioXref\n%%EOF\n")
        out.write(latin1(xref.toString()))
        return out.readByteArray()
    }

    private fun n(v: Float): String {
        val r = kotlin.math.round(v * 100) / 100
        return if (r == kotlin.math.floor(r)) r.toInt().toString() else r.toString()
    }

    private fun rgb(c: Int) = listOf(16, 8, 0).joinToString(" ") { n(((c shr it) and 0xFF) / 255f) }

    private fun escapar(t: String) = buildString {
        for (ch in t) {
            val c = winAnsi(ch)
            when (c) { '(', ')', '\\' -> append('\\').append(c); else -> append(c) }
        }
    }

    private fun latin1(s: String) = ByteArray(s.length) { s[it].code.toByte() }

    companion object {
        /** Ancho y alto de un JPEG leyendo su cabecera SOF. */
        fun tamanoJpeg(b: ByteArray): Pair<Int, Int>? {
            var i = 2
            while (i + 9 < b.size) {
                if (b[i].toInt() and 0xFF != 0xFF) return null
                val marca = b[i + 1].toInt() and 0xFF
                val largo = ((b[i + 2].toInt() and 0xFF) shl 8) or (b[i + 3].toInt() and 0xFF)
                if (marca in 0xC0..0xCF && marca != 0xC4 && marca != 0xC8 && marca != 0xCC) {
                    val h = ((b[i + 5].toInt() and 0xFF) shl 8) or (b[i + 6].toInt() and 0xFF)
                    val w = ((b[i + 7].toInt() and 0xFF) shl 8) or (b[i + 8].toInt() and 0xFF)
                    return w to h
                }
                i += 2 + largo
            }
            return null
        }
    }
}

/** Carácter tal como lo entiende WinAnsiEncoding (Latin-1 + algunos extra). Lo que no existe sale como "?". */
private fun winAnsi(c: Char): Char = when {
    c.code in 32..126 || c.code in 0xA0..0xFF -> c
    c == '€' -> '\u0080'; c == '…' -> '\u0085'; c == '•' -> '\u0095'
    c == '–' -> '\u0096'; c == '—' -> '\u0097'
    c == '‘' -> '\u0091'; c == '’' -> '\u0092'; c == '“' -> '\u0093'; c == '”' -> '\u0094'
    else -> '?'
}

/** Letra sin acento, para medir el ancho (á mide lo mismo que a). */
private fun base(c: Char): Char = when (c) {
    in "áàâäã" -> 'a'; in "éèêë" -> 'e'; in "íìîï" -> 'i'; in "óòôöõº°" -> 'o'; in "úùûü" -> 'u'; 'ñ' -> 'n'; 'ç' -> 'c'
    in "ÁÀÂÄÃ" -> 'A'; in "ÉÈÊË" -> 'E'; in "ÍÌÎÏ" -> 'I'; in "ÓÒÔÖÕ" -> 'O'; in "ÚÙÛÜ" -> 'U'; 'Ñ' -> 'N'; 'Ç' -> 'C'
    '¿' -> '?'; '¡' -> '!'; '½' -> 'W'; '·' -> '.'; '€' -> '0'; '—', '–' -> '-'; '“', '”' -> '"'; '‘', '’' -> '\''
    else -> c
}

// Anchos de Helvetica y Helvetica-Bold para los caracteres 32..126 (en milésimas del tamaño de letra)
private val ANCHOS_NORMAL = intArrayOf(
    278, 278, 355, 556, 556, 889, 667, 191, 333, 333, 389, 584, 278, 333, 278, 278,
    556, 556, 556, 556, 556, 556, 556, 556, 556, 556, 278, 278, 584, 584, 584, 556,
    1015, 667, 667, 722, 722, 667, 611, 778, 722, 278, 500, 667, 556, 833, 722, 778,
    667, 778, 722, 667, 611, 722, 667, 944, 667, 667, 611, 278, 278, 278, 469, 556,
    333, 556, 556, 500, 556, 556, 278, 556, 556, 222, 222, 500, 222, 833, 556, 556,
    556, 556, 333, 500, 278, 556, 500, 722, 500, 500, 500, 334, 260, 334, 584,
)
private val ANCHOS_NEGRITA = intArrayOf(
    278, 333, 474, 556, 556, 889, 722, 238, 333, 333, 389, 584, 278, 333, 278, 278,
    556, 556, 556, 556, 556, 556, 556, 556, 556, 556, 333, 333, 584, 584, 584, 611,
    975, 722, 722, 722, 722, 667, 611, 778, 722, 278, 556, 722, 611, 833, 722, 778,
    667, 778, 722, 667, 611, 722, 667, 944, 667, 667, 611, 333, 278, 333, 584, 556,
    333, 556, 611, 556, 611, 556, 333, 611, 611, 278, 278, 556, 278, 889, 611, 611,
    611, 611, 389, 556, 333, 611, 556, 778, 556, 556, 500, 389, 280, 389, 584,
)
