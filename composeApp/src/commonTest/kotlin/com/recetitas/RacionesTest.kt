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
    @Test fun igualSinCambios() = assertEquals("200 g", escalarIngrediente("200 g", 4, 4))
}
