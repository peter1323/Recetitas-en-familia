package com.recetitas

import com.recetitas.model.escalarIngrediente
import kotlin.test.Test
import kotlin.test.assertEquals

class RacionesTest {
    @Test fun gramos() = assertEquals("135 g de azúcar", escalarIngrediente("200 g de azúcar", 6, 4))
    @Test fun litrosConDecimal() = assertEquals("0,7 l de leche", escalarIngrediente("1 l de leche", 6, 4))
    @Test fun huevosEnteros() = assertEquals("4 huevos", escalarIngrediente("6 huevos", 6, 4))
    @Test fun nuncaCeroHuevos() = assertEquals("1 huevo", escalarIngrediente("1 huevo", 6, 1))
    @Test fun fraccion() = assertEquals("1 cebolla", escalarIngrediente("1/2 cebolla", 2, 4))
    @Test fun medios() = assertEquals("1½ cucharadas de aceite", escalarIngrediente("1 cucharadas de aceite", 2, 3))
    @Test fun sinNumero() = assertEquals("Sal al gusto", escalarIngrediente("Sal al gusto", 4, 8))
    @Test fun comaDecimal() = assertEquals("3 kg de patatas", escalarIngrediente("1,5 kg de patatas", 2, 4))
    @Test fun fechas() {
        assertEquals("1/1/1970", com.recetitas.model.textoFecha(0))
        assertEquals("29/2/2024", com.recetitas.model.textoFecha(19782))
        assertEquals("2026-11-11", com.recetitas.model.fechaIso(20768))
    }
    @Test fun tiemposDePasos() {
        assertEquals(180, com.recetitas.model.segundosDe("3 min"))
        assertEquals(5400, com.recetitas.model.segundosDe("1 h 30 min"))
        assertEquals(240, com.recetitas.model.segundosDe("4 min por lado"))
        assertEquals(30, com.recetitas.model.segundosDe("30 seg"))
        assertEquals(5400, com.recetitas.model.segundosDe("1,5 horas"))
        assertEquals(null, com.recetitas.model.segundosDe("toda la noche"))
        assertEquals("3:05", com.recetitas.model.textoCuentaAtras(185))
        assertEquals("1:02:05", com.recetitas.model.textoCuentaAtras(3725))
    }
    @Test fun igualSinCambios() = assertEquals("200 g", escalarIngrediente("200 g", 4, 4))
}
