package com.example.appbiblialeeer.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * El "atrás" del teléfono en el escritorio: la ventana llama a [volver] al pulsar Esc o ⌘[.
 * Como en Android, gana el último BackHandler activo (el de la pantalla más interna).
 */
class DespachadorAtras {
    internal class Manejador(var activo: Boolean, var accion: () -> Unit)

    private val manejadores = mutableListOf<Manejador>()

    internal fun agregar(manejador: Manejador) = manejadores.add(manejador)

    internal fun quitar(manejador: Manejador) = manejadores.remove(manejador)

    /** true si alguna pantalla volvió atrás. */
    fun volver(): Boolean {
        val manejador = manejadores.lastOrNull { it.activo } ?: return false
        manejador.accion()
        return true
    }
}

val LocalDespachadorAtras = staticCompositionLocalOf { DespachadorAtras() }

/** Mismo uso que BackHandler de androidx.activity. */
@Composable
fun BackHandler(enabled: Boolean = true, onBack: () -> Unit) {
    val despachador = LocalDespachadorAtras.current
    val manejador = remember { DespachadorAtras.Manejador(enabled, onBack) }
    SideEffect {
        manejador.activo = enabled
        manejador.accion = onBack
    }
    DisposableEffect(despachador) {
        despachador.agregar(manejador)
        onDispose { despachador.quitar(manejador) }
    }
}
