package com.recetitas

import com.recetitas.pdf.Pdf
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class FotoTest {
    private fun png(w: Int, h: Int): ByteArray =
        ByteArrayOutputStream().also { ImageIO.write(BufferedImage(w, h, BufferedImage.TYPE_INT_RGB), "png", it) }.toByteArray()

    @Test fun fotoGrandeSeReduceAJpeg() {
        val jpeg = prepararFoto(png(4000, 3000))!!
        // Empieza por FFD8 (JPEG) y el lado mayor queda en 1600 px, manteniendo la proporción
        assertEquals(0xFF.toByte(), jpeg[0]); assertEquals(0xD8.toByte(), jpeg[1])
        assertEquals(1600 to 1200, Pdf.tamanoJpeg(jpeg))
    }

    @Test fun fotoPequenaNoSeAgranda() = assertEquals(800 to 600, Pdf.tamanoJpeg(prepararFoto(png(800, 600))!!))

    @Test fun archivoQueNoEsImagen() = assertNull(prepararFoto("no soy una foto".encodeToByteArray()))
}
