package com.example.appbiblialeeer.ui

import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import com.example.appbiblialeeer.data.biblePlan
import com.example.appbiblialeeer.storage.loadCompletedDays
import com.example.appbiblialeeer.storage.planPrefs
import com.example.appbiblialeeer.storage.saveDayAsCompleted
import com.example.appbiblialeeer.ui.components.Iconos
import com.example.appbiblialeeer.ui.screens.DetalleLecturaPantalla
import com.example.appbiblialeeer.ui.screens.ListaDiasPantalla

@Composable
fun AppBiblialectura() {
    val preferences = remember { planPrefs() }
    var completedDays by remember { mutableStateOf(loadCompletedDays(preferences)) }
    var selectedDay by rememberSaveable { mutableStateOf<Int?>(null) }
    val listState = rememberLazyListState()
    // ✅ Un solo check para todas las tarjetas de la app (antes cada tarjeta creaba el suyo y lo
    // volvía a dibujar al entrar en pantalla); se ve exactamente igual
    val iconoCheck = rememberVectorPainter(Iconos.Completado)

    BackHandler(enabled = (selectedDay != null)) {
        selectedDay = null
    }

    val day = selectedDay
    if (day == null) {
        ListaDiasPantalla(
            biblePlan,
            completedDays,
            listState = listState,
            iconoCheck = iconoCheck,
            onDiaSeleccionado = { selectedDay = it }
        )
    } else {
        DetalleLecturaPantalla(
            lectura = biblePlan[day - 1],
            dayIsCompleted = completedDays[day] == true,
            iconoCheck = iconoCheck,
            onVolver = { selectedDay = null },
            onMarkDayAsCompleted = { completedDay ->
                completedDays = completedDays + (completedDay to true)
                saveDayAsCompleted(preferences, completedDay)
            }
        )
    }
}
