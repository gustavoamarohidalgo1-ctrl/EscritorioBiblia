package com.example.appbiblialeeer.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.appbiblialeeer.storage.BibleTextStorage
import com.example.appbiblialeeer.storage.loadScrollPosition
import com.example.appbiblialeeer.storage.planPrefs
import com.example.appbiblialeeer.storage.saveScrollPosition
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

// ✅ Creados una sola vez (antes se creaban para cada versículo que entraba en pantalla)
private val EspacioVersiculo = Modifier.padding(bottom = 75.dp)
private val EspacioUltimoVersiculo = Modifier.padding(bottom = 0.dp)

// Ancho cómodo para leer: en una ventana ancha las líneas no se hacen interminables
private val AnchoMaximoTexto = 900.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LecturaVersiculoPantalla(
    referencia: String,
    onVolver: () -> Unit,
    onCompletarLectura: () -> Unit
) {
    val preferences = remember { planPrefs() }

    // ✅ Si el pasaje ya está en caché (se precarga al abrir el día) se muestra al instante,
    // directamente donde se dejó: sin loader y sin "brinco"
    val enCache = remember { BibleTextStorage.cachedPassage(referencia) }
    val posicionGuardada = remember { loadScrollPosition(preferences, referencia) }
    val listState = rememberLazyListState(posicionGuardada.first, posicionGuardada.second)

    var lineas by remember { mutableStateOf(enCache.orEmpty()) }
    var listo by remember { mutableStateOf(enCache != null) }
    // Solo se guarda la posición y se marca como leída si el texto cargó bien
    var textoOk by remember { mutableStateOf(!enCache.isNullOrEmpty()) }
    // Se marca una sola vez por apertura, como antes
    var finVisto by remember { mutableStateOf(false) }
    val completarLectura by rememberUpdatedState(onCompletarLectura)

    // Fade-in suave cuando hubo que cargar; se aplica al dibujar, sin recomponer la pantalla
    val contentAlpha = animateFloatAsState(
        targetValue = if (listo) 1f else 0f,
        label = "contentAlpha"
    )

    // 1) Cargar texto si no estaba en caché
    if (enCache == null) {
        LaunchedEffect(Unit) {
            try {
                val result = withContext(Dispatchers.IO) {
                    BibleTextStorage.getPassageLines(referencia)
                }
                if (result.isEmpty()) {
                    lineas = listOf("Texto no disponible para: $referencia")
                } else {
                    lineas = result
                    listState.scrollToItem(posicionGuardada.first, posicionGuardada.second)
                    textoOk = true
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                lineas = listOf(e.message ?: "Error cargando lectura")
            }
            listo = true
        }
    }

    if (textoOk) {
        // 2) Guardar la posición (mismas claves): al abrir y al terminar cada desplazamiento, al
        // minimizar la ventana y al salir. ✅ Antes se revisaba en cada fotograma del desplazamiento;
        // isScrollInProgress solo cambia al empezar y al terminar, así que desplazarse no cuesta nada
        fun guardarPosicion() = saveScrollPosition(
            preferences,
            referencia,
            listState.firstVisibleItemIndex,
            listState.firstVisibleItemScrollOffset
        )

        LaunchedEffect(Unit) {
            snapshotFlow { listState.isScrollInProgress }
                .collect { desplazando -> if (!desplazando) guardarPosicion() }
        }

        // Al minimizar la ventana y al volver atrás o cerrar la app
        val lifecycleOwner = LocalLifecycleOwner.current
        DisposableEffect(lifecycleOwner) {
            val observer = LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_STOP) guardarPosicion()
            }
            lifecycleOwner.lifecycle.addObserver(observer)
            onDispose {
                lifecycleOwner.lifecycle.removeObserver(observer)
                guardarPosicion()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = referencia, style = MaterialTheme.typography.titleLarge) },
                navigationIcon = {
                    IconButton(onClick = onVolver) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        }
    ) { paddingValues ->

        Box(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
        ) {
            val estiloVersiculo = MaterialTheme.typography.bodyLarge.copy(
                fontSize = 30.sp,
                lineHeight = 100.sp
            )

            // ✅ La lista SIEMPRE existe, solo que invisible hasta que esté lista
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .widthIn(max = AnchoMaximoTexto)
                    .padding(horizontal = 56.dp, vertical = 20.dp)
                    .fillMaxSize()
                    .graphicsLayer { alpha = contentAlpha.value }
            ) {
                val ultimo = lineas.lastIndex
                items(lineas.size) { index ->
                    Text(
                        text = lineas[index],
                        style = estiloVersiculo,
                        modifier = if (index == ultimo) EspacioUltimoVersiculo else EspacioVersiculo
                    )

                    // 3) Detectar fin: al ver el último versículo la lectura queda completada.
                    // ✅ Solo lo vigila el último versículo mientras existe (se crea al acercarse al final),
                    // en vez de revisar la lista en cada fotograma de toda la lectura
                    if (index == ultimo && textoOk && !finVisto) {
                        LaunchedEffect(Unit) {
                            snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index == index }
                                .first { it }
                            finVisto = true
                            completarLectura()
                        }
                    }
                }
            }

            VerticalScrollbar(
                adapter = rememberScrollbarAdapter(listState),
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .fillMaxHeight()
            )

            // ✅ Loader encima mientras se prepara (sin pantallazo negro)
            if (!listo) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
        }
    }
}
