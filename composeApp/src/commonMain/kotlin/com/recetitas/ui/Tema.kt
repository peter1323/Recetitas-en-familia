package com.recetitas.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import com.recetitas.ColorBarras
import com.recetitas.model.Ajustes
import com.recetitas.model.ESCALAS_LETRA
import com.recetitas.model.Tema

// Colores cálidos de cocina: naranja, teja y crema (los mismos del icono)
val Naranja = Color(0xFFEF9F27)
val Teja = Color(0xFF993C1D)
val Crema = Color(0xFFFAEEDA)
val Fuego = Color(0xFFA32D2D)

private val claro = lightColorScheme(
    primary = Color(0xFFBA7517), onPrimary = Color.White,
    primaryContainer = Naranja, onPrimaryContainer = Color(0xFF412402),
    secondary = Teja, onSecondary = Color.White,
    secondaryContainer = Crema, onSecondaryContainer = Color(0xFF633806),
    tertiaryContainer = Color(0xFFFCEBEB), onTertiaryContainer = Color(0xFF791F1F),
    background = Color(0xFFFFFBF4), onBackground = Color(0xFF2C2C2A),
    surface = Color(0xFFFFFBF4), onSurface = Color(0xFF2C2C2A),
    surfaceVariant = Crema, onSurfaceVariant = Color(0xFF5F5E5A),
    surfaceContainer = Color(0xFFFFF4E4), surfaceContainerHigh = Color(0xFFFBEEDC),
    surfaceContainerHighest = Color(0xFFF6E6CF), surfaceContainerLow = Color(0xFFFFF8EE),
    outline = Color(0xFFB4B2A9), outlineVariant = Color(0xFFD3D1C7),
)

private val oscuro = darkColorScheme(
    primary = Naranja, onPrimary = Color(0xFF412402),
    primaryContainer = Naranja, onPrimaryContainer = Color(0xFF412402),
    secondary = Color(0xFFF0997B), onSecondary = Color(0xFF4A1B0C),
    secondaryContainer = Color(0xFF3A3530), onSecondaryContainer = Color(0xFFFAC775),
    tertiaryContainer = Color(0xFF501313), onTertiaryContainer = Color(0xFFF7C1C1),
    background = Color(0xFF1E1E1C), onBackground = Color(0xFFF1EFE8),
    surface = Color(0xFF1E1E1C), onSurface = Color(0xFFF1EFE8),
    surfaceVariant = Color(0xFF2C2C2A), onSurfaceVariant = Color(0xFFB4B2A9),
    surfaceContainer = Color(0xFF2A2927), surfaceContainerHigh = Color(0xFF32302D),
    surfaceContainerHighest = Color(0xFF3A3834), surfaceContainerLow = Color(0xFF242321),
    outline = Color(0xFF888780), outlineVariant = Color(0xFF444441),
)

@Composable
fun TemaRecetitas(ajustes: Ajustes, contenido: @Composable () -> Unit) {
    val oscuroActivo = when (ajustes.tema) {
        Tema.CLARO -> false
        Tema.OSCURO -> true
        Tema.AUTO -> isSystemInDarkTheme()
    }
    // El tamaño de letra elegido en Ajustes multiplica el del sistema
    val d = LocalDensity.current
    val escala = ESCALAS_LETRA[ajustes.tamanoLetra.coerceIn(ESCALAS_LETRA.indices)]
    CompositionLocalProvider(LocalDensity provides Density(d.density, d.fontScale * escala)) {
        ColorBarras(oscuroActivo)
        MaterialTheme(colorScheme = if (oscuroActivo) oscuro else claro, content = contenido)
    }
}
