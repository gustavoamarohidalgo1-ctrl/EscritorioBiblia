package com.example.appbiblialeeer.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathBuilder
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/**
 * ✅ Los dos únicos iconos de Material que usa la app, con los mismos trazos que
 * Icons.Filled.CheckCircle e Icons.AutoMirrored.Filled.ArrowBack: así la app no carga la librería
 * de iconos entera (≈0,9 MB) para dibujar dos.
 */
object Iconos {

    val Completado: ImageVector by lazy {
        icono("Filled.CheckCircle") {
            moveTo(12.0f, 2.0f)
            curveTo(6.48f, 2.0f, 2.0f, 6.48f, 2.0f, 12.0f)
            reflectiveCurveToRelative(4.48f, 10.0f, 10.0f, 10.0f)
            reflectiveCurveToRelative(10.0f, -4.48f, 10.0f, -10.0f)
            reflectiveCurveTo(17.52f, 2.0f, 12.0f, 2.0f)
            close()
            moveTo(10.0f, 17.0f)
            lineToRelative(-5.0f, -5.0f)
            lineToRelative(1.41f, -1.41f)
            lineTo(10.0f, 14.17f)
            lineToRelative(7.59f, -7.59f)
            lineTo(19.0f, 8.0f)
            lineToRelative(-9.0f, 9.0f)
            close()
        }
    }

    val Volver: ImageVector by lazy {
        icono("AutoMirrored.Filled.ArrowBack", autoMirror = true) {
            moveTo(20.0f, 11.0f)
            horizontalLineTo(7.83f)
            lineToRelative(5.59f, -5.59f)
            lineTo(12.0f, 4.0f)
            lineToRelative(-8.0f, 8.0f)
            lineToRelative(8.0f, 8.0f)
            lineToRelative(1.41f, -1.41f)
            lineTo(7.83f, 13.0f)
            horizontalLineTo(20.0f)
            verticalLineToRelative(-2.0f)
            close()
        }
    }

    // Igual que materialIcon + materialPath: 24×24 y relleno negro (Icon lo tiñe con su color)
    private inline fun icono(
        nombre: String,
        autoMirror: Boolean = false,
        trazo: PathBuilder.() -> Unit
    ): ImageVector = ImageVector.Builder(
        name = nombre,
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
        autoMirror = autoMirror
    ).path(fill = SolidColor(Color.Black), pathBuilder = trazo).build()
}
