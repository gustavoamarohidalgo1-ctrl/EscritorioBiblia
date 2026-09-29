package com.example.appbiblialeeer.storage

import java.util.concurrent.Executors
import java.util.prefs.BackingStoreException
import java.util.prefs.Preferences

/**
 * Progreso guardado con las preferencias de Java (en macOS, un .plist en ~/Library/Preferences).
 * Mismo nodo para toda la app, igual que el antiguo SharedPreferences "PlanLectura".
 */
fun planPrefs(): Preferences = Preferences.userRoot().node("com/example/appbiblialeeer/PlanLectura")

// ✅ Un solo hilo en segundo plano para escribir en disco: marcar una lectura no frena la interfaz
private val escritor = Executors.newSingleThreadExecutor { tarea ->
    Thread(tarea, "guardar-progreso").apply { isDaemon = true }
}

// Escribe en disco enseguida (como apply de SharedPreferences); si falla o la app se cierra antes,
// Java lo guarda igualmente al salir
private fun Preferences.guardar() {
    escritor.execute {
        try {
            flush()
        } catch (_: BackingStoreException) {
        }
    }
}

fun saveDayProgress(preferences: Preferences, day: Int, progress: Map<String, Boolean>) {
    progress.forEach { (reference, isCompleted) ->
        preferences.putBoolean("Day_${day}_$reference", isCompleted)
    }
    preferences.guardar()
}

fun loadDayProgress(preferences: Preferences, day: Int, references: List<String>): Map<String, Boolean> {
    return references.associateWith { reference ->
        preferences.getBoolean("Day_${day}_$reference", false)
    }
}

fun saveDayAsCompleted(preferences: Preferences, dia: Int) {
    preferences.putBoolean("Day_$dia", true)
    preferences.guardar()
}

fun loadCompletedDays(preferences: Preferences): Map<Int, Boolean> {
    return (1..31).associateWith { preferences.getBoolean("Day_$it", false) }
}

// --- posición de lectura (mismas claves que en el teléfono) ---
private fun scrollKey(reference: String) = "ref_${reference.hashCode()}"

/** Índice y desplazamiento del primer elemento visible la última vez que se leyó. */
fun loadScrollPosition(preferences: Preferences, reference: String): Pair<Int, Int> {
    val key = scrollKey(reference)
    return preferences.getInt("scroll_index_$key", 0) to preferences.getInt("scroll_offset_$key", 0)
}

fun saveScrollPosition(preferences: Preferences, reference: String, index: Int, offset: Int) {
    val key = scrollKey(reference)
    preferences.putInt("scroll_index_$key", index)
    preferences.putInt("scroll_offset_$key", offset)
    // ✅ Sin guardar(): al arrastrar la barra de desplazamiento esto se llama en cada fotograma.
    // Java lo escribe en disco por su cuenta cada pocos segundos y al cerrar la app
}
