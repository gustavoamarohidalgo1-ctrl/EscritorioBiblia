package com.example.appbiblialeeer.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.unit.dp
import com.example.appbiblialeeer.data.DailyReading
import com.example.appbiblialeeer.storage.BibleTextStorage
import com.example.appbiblialeeer.ui.components.Iconos
import com.example.appbiblialeeer.storage.loadDayProgress
import com.example.appbiblialeeer.storage.planPrefs
import com.example.appbiblialeeer.storage.saveDayProgress
import com.example.appbiblialeeer.ui.BackHandler
import com.example.appbiblialeeer.ui.components.TarjetaItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private val VerdeCompletado = Color(0xFF388E3C)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetalleLecturaPantalla(
    lectura: DailyReading,
    dayIsCompleted: Boolean,
    iconoCheck: Painter,
    onVolver: () -> Unit,
    onMarkDayAsCompleted: (Int) -> Unit
) {
    val preferences = remember { planPrefs() }

    var completedReferences by remember(lectura.dia) {
        mutableStateOf(loadDayProgress(preferences, lectura.dia, lectura.referencias))
    }
    var selectedReference by rememberSaveable { mutableStateOf<String?>(null) }

    val total = completedReferences.size
    val completedCount = completedReferences.values.count { it }
    val progress = if (total == 0) 0f else completedCount.toFloat() / total.toFloat()
    val allCompleted = total > 0 && completedCount == total

    // Marcar el día como completado solo cuando todas las referencias lo estén
    LaunchedEffect(allCompleted) {
        if (allCompleted && !dayIsCompleted) {
            onMarkDayAsCompleted(lectura.dia)
        }
    }

    BackHandler(enabled = selectedReference != null) {
        selectedReference = null
    }

    // Prefetch lecturas del día
    LaunchedEffect(lectura.dia) {
        withContext(Dispatchers.IO) {
            BibleTextStorage.prefetchDay(lectura.referencias)
        }
    }

    val referenciaAbierta = selectedReference
    if (referenciaAbierta != null) {
        LecturaVersiculoPantalla(
            referencia = referenciaAbierta,
            onVolver = { selectedReference = null },
            onCompletarLectura = {
                if (completedReferences[referenciaAbierta] != true) {
                    completedReferences = completedReferences + (referenciaAbierta to true)
                    saveDayProgress(preferences, lectura.dia, completedReferences)
                }
            }
        )
    } else {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            "Lecturas del Día ${lectura.dia}",
                            style = MaterialTheme.typography.titleLarge
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onVolver) {
                            Icon(
                                Iconos.Volver,
                                contentDescription = "Volver"
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        titleContentColor = MaterialTheme.colorScheme.onSurface,
                        navigationIconContentColor = MaterialTheme.colorScheme.onSurface
                    )
                )
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .padding(paddingValues)
                    .fillMaxSize(),
                contentAlignment = Alignment.TopCenter
            ) {
                Column(
                    modifier = Modifier
                        .widthIn(max = AnchoMaximoLista)
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                        .fillMaxSize()
                ) {
                    Text(
                        text = "Progreso de lectura",
                        style = MaterialTheme.typography.titleMedium
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp) // ✅ más gordita
                            .clip(RoundedCornerShape(999.dp)), // ✅ redondeada elegante
                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                        color = if (allCompleted) VerdeCompletado else MaterialTheme.colorScheme.primary
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                        contentPadding = PaddingValues(vertical = 8.dp)
                    ) {
                        items(lectura.referencias) { reference ->
                            TarjetaItem(
                                texto = reference,
                                completado = completedReferences[reference] == true,
                                iconoCheck = iconoCheck,
                                onClick = { selectedReference = reference }
                            )
                        }
                    }
                }
            }
        }
    }
}
