package com.example.appbiblialeeer.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.appbiblialeeer.data.DailyReading
import com.example.appbiblialeeer.ui.components.TarjetaItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListaDiasPantalla(
    biblePlan: List<DailyReading>,
    completedDays: Map<Int, Boolean>,
    listState: LazyListState,
    iconoCheck: Painter,
    onDiaSeleccionado: (Int) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Plan de Lectura Bíblica - Septiembre",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontSize = 20.sp // Cambia este valor al tamaño que desees (ej: 18.sp, 20.sp)
                        )
                    )
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            state = listState,
            verticalArrangement = Arrangement.spacedBy(14.dp), // ← separación elegante
            modifier = Modifier
                .padding(paddingValues)
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            items(biblePlan, key = { it.dia }) { dayReading ->
                val isCompleted = completedDays[dayReading.dia] ?: false
                TarjetaItem(
                    texto = "Día ${dayReading.dia}",
                    completado = isCompleted,
                    iconoCheck = iconoCheck,
                    onClick = { onDiaSeleccionado(dayReading.dia) }
                )
            }
        }
    }
}
