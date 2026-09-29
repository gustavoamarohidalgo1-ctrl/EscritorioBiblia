package com.example.appbiblialeeer.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.platform.Font
import androidx.compose.ui.unit.sp

// ----------------------------
// Fuente Lora (local, en resources/font)
// ----------------------------

private object Recursos

private fun fuente(archivo: String, weight: FontWeight) = Font(
    identity = archivo,
    data = checkNotNull(Recursos::class.java.getResourceAsStream("/font/$archivo")) {
        "Fuente no encontrada: $archivo"
    }.use { it.readBytes() },
    weight = weight
)

val LoraFamily = FontFamily(
    fuente("lora_regular.ttf", FontWeight.Normal),
    fuente("lora_bold.ttf", FontWeight.Bold)
)

// ----------------------------
// Tipografía personalizada
// ----------------------------

val AppTypography = Typography(

    // Títulos grandes (Ej: Plan de Lectura Bíblica - Marzo)
    titleLarge = TextStyle(
        fontFamily = LoraFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp
    ),

    // Títulos medianos (Ej: Día 1)
    titleMedium = TextStyle(
        fontFamily = LoraFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp
    ),

    // Texto principal (versículos)
    bodyLarge = TextStyle(
        fontFamily = LoraFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 18.sp,
        lineHeight = 32.sp
    ),

    // Texto secundario
    bodyMedium = TextStyle(
        fontFamily = LoraFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 26.sp
    ),

    // Texto pequeño (labels, ayudas)
    labelLarge = TextStyle(
        fontFamily = LoraFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp
    )
)