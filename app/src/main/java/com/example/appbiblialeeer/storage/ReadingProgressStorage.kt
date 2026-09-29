package com.example.appbiblialeeer.storage

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

fun planPrefs(context: Context): SharedPreferences =
    context.getSharedPreferences("PlanLectura", Context.MODE_PRIVATE)

fun saveDayProgress(sharedPreferences: SharedPreferences, day: Int, progress: Map<String, Boolean>) {
    sharedPreferences.edit {
        progress.forEach { (reference, isCompleted) ->
            putBoolean("Day_${day}_$reference", isCompleted)
        }
    }
}

fun loadDayProgress(sharedPreferences: SharedPreferences, day: Int, references: List<String>): Map<String, Boolean> {
    return references.associateWith { reference ->
        sharedPreferences.getBoolean("Day_${day}_$reference", false)
    }
}

fun saveDayAsCompleted(sharedPreferences: SharedPreferences, dia: Int) {
    sharedPreferences.edit { putBoolean("Day_$dia", true) }
}

fun loadCompletedDays(sharedPreferences: SharedPreferences): Map<Int, Boolean> {
    return (1..31).associateWith { sharedPreferences.getBoolean("Day_$it", false) }
}

// --- posición de lectura (mismas claves de siempre, para no perder lo guardado) ---
private fun scrollKey(reference: String) = "ref_${reference.hashCode()}"

/** Índice y desplazamiento del primer elemento visible la última vez que se leyó. */
fun loadScrollPosition(sharedPreferences: SharedPreferences, reference: String): Pair<Int, Int> {
    val key = scrollKey(reference)
    return sharedPreferences.getInt("scroll_index_$key", 0) to sharedPreferences.getInt("scroll_offset_$key", 0)
}

fun saveScrollPosition(sharedPreferences: SharedPreferences, reference: String, index: Int, offset: Int) {
    val key = scrollKey(reference)
    sharedPreferences.edit {
        putInt("scroll_index_$key", index)
        putInt("scroll_offset_$key", offset)
    }
}
