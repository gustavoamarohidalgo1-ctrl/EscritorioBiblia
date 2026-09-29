package com.example.appbiblialeeer.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.appbiblialeeer.R

// ----------------------------
// Fuente Lora (local)
// ----------------------------

val LoraFamily = FontFamily(
    Font(R.font.lora_regular, FontWeight.Normal),
    Font(R.font.lora_bold, FontWeight.Bold)
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