package com.example.appbiblialeeer.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.unit.dp

private val FormaTarjeta = RoundedCornerShape(20.dp)

// ✅ Creados una sola vez (antes se creaban para cada tarjeta que entraba en pantalla)
private val RellenoFila = Modifier.padding(horizontal = 22.dp, vertical = 20.dp)
private val TamanoIcono = Modifier.size(22.dp)
private val EspacioIcono = Modifier.width(12.dp)

/**
 * Tarjeta con check de completado: se usa para los días y para las lecturas de cada día.
 * [iconoCheck] es el mismo check para todas las tarjetas: se crea una sola vez
 * (rememberVectorPainter) y así el icono se dibuja una vez y no una por tarjeta.
 */
@Composable
fun TarjetaItem(
    texto: String,
    completado: Boolean,
    iconoCheck: Painter,
    onClick: () -> Unit
) {
    val iconTint =
        if (completado)
            MaterialTheme.colorScheme.primary
        else
            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .pointerHoverIcon(PointerIcon.Hand) // ✅ en el escritorio, la mano indica que se puede pulsar
            .clickable(onClick = onClick),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = FormaTarjeta,
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = RellenoFila,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = iconoCheck,
                contentDescription = if (completado) "Completado" else "Pendiente",
                tint = iconTint,
                modifier = TamanoIcono
            )

            Spacer(modifier = EspacioIcono)

            Text(
                text = texto,
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
}
