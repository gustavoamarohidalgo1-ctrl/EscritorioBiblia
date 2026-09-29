package com.example.appbiblialeeer

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.example.appbiblialeeer.storage.planPrefs
import com.example.appbiblialeeer.ui.AppBiblialectura
import com.example.appbiblialeeer.ui.DespachadorAtras
import com.example.appbiblialeeer.ui.LocalDespachadorAtras
import com.example.appbiblialeeer.ui.theme.AppBibliaTheme
import com.example.appbiblialeeer.ui.theme.LoraFamily
import java.awt.Taskbar
import javax.imageio.ImageIO
import kotlin.concurrent.thread

private const val NOMBRE_APP = "Mes de Septiembre"

fun main() {
    // Antes de crear la ventana: barra de título oscura en macOS (la app siempre usa modo oscuro)
    // y el nombre de la app en la barra de menús
    System.setProperty("apple.awt.application.appearance", "NSAppearanceNameDarkAqua")
    System.setProperty("apple.awt.application.name", NOMBRE_APP)

    precargarPrimerFotograma()

    val icono = ImageIO.read(checkNotNull(object {}.javaClass.getResource("/icono.png")))
    // Icono del Dock al ejecutar sin empaquetar (la app empaquetada usa icono.icns)
    runCatching {
        val taskbar = Taskbar.getTaskbar()
        if (taskbar.isSupported(Taskbar.Feature.ICON_IMAGE)) taskbar.iconImage = icono
    }

    application {
        val despachadorAtras = remember { DespachadorAtras() }
        Window(
            onCloseRequest = ::exitApplication,
            title = NOMBRE_APP,
            icon = remember { BitmapPainter(icono.toComposeImageBitmap()) },
            state = rememberWindowState(
                size = DpSize(900.dp, 820.dp),
                position = WindowPosition(Alignment.Center)
            ),
            // El botón "atrás" del teléfono: Esc o ⌘[ (como en Safari o el Finder)
            onPreviewKeyEvent = { evento ->
                evento.type == KeyEventType.KeyDown &&
                    (evento.key == Key.Escape || (evento.isMetaPressed && evento.key == Key.LeftBracket)) &&
                    despachadorAtras.volver()
            }
        ) {
            CompositionLocalProvider(LocalDespachadorAtras provides despachadorAtras) {
                AppBibliaTheme {
                    AppBiblialectura()
                }
            }
        }
    }
}

/**
 * ✅ Mientras se crea la ventana, un hilo aparte deja listo lo que el primer fotograma necesita:
 * el progreso guardado (se lee del disco) y la fuente Lora (se lee de los recursos de la app).
 */
private fun precargarPrimerFotograma() {
    thread(name = "precarga", isDaemon = true) {
        // Solo es una precarga: si algo falla, la pantalla lo carga como siempre
        runCatching {
            planPrefs().keys()
            LoraFamily
        }
    }
}
