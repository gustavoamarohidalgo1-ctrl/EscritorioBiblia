package com.example.appbiblialeeer.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalContext
import com.example.appbiblialeeer.data.biblePlan
import com.example.appbiblialeeer.storage.loadCompletedDays
import com.example.appbiblialeeer.storage.planPrefs
import com.example.appbiblialeeer.storage.saveDayAsCompleted
import com.example.appbiblialeeer.ui.screens.DetalleLecturaPantalla
import com.example.appbiblialeeer.ui.screens.ListaDiasPantalla

@Composable
fun AppBiblialectura() {
    val context = LocalContext.current
    val sharedPreferences = remember(context) { planPrefs(context) }
    var completedDays by remember { mutableStateOf(loadCompletedDays(sharedPreferences)) }
    // rememberSaveable: al girar el teléfono sigues en el mismo día
    var selectedDay by rememberSaveable { mutableStateOf<Int?>(null) }
    val listState = rememberLazyListState()
    // ✅ Un solo check para todas las tarjetas de la app (antes cada tarjeta creaba el suyo y lo
    // volvía a dibujar al entrar en pantalla); se ve exactamente igual
    val iconoCheck = rememberVectorPainter(Icons.Filled.CheckCircle)

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
                saveDayAsCompleted(sharedPreferences, completedDay)
            }
        )
    }
}
