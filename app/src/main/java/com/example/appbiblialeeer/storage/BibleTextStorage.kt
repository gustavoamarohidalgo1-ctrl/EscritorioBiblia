package com.example.appbiblialeeer.storage

import android.content.Context
import android.util.LruCache

object BibleTextStorage {

    // ✅ Cachea resultado por referencia (ej "Hechos 2:1-13")
    private val passageCache = LruCache<String, List<String>>(60)

    /** Pasaje ya cargado, para mostrarlo al instante sin esperar. */
    fun cachedPassage(reference: String): List<String>? = passageCache.get(reference)

    /**
     * Prefetch: precarga lecturas de un día (para que al tocar cargue instantáneo).
     * Llamar fuera del hilo principal.
     */
    fun prefetchDay(context: Context, references: List<String>) {
        references.forEach { ref ->
            if (passageCache.get(ref) == null) {
                runCatching { getPassageLines(context, ref) }
            }
        }
    }

    /** Líneas del pasaje; lista vacía si la referencia no tiene versículos. Llamar fuera del hilo principal. */
    fun getPassageLines(context: Context, reference: String): List<String> {
        passageCache.get(reference)?.let { return it }

        val ref = BibleParser.parseReading(reference)
        val book = BibleParser.resolveBook(ref.bookName)
        // ✅ Lee el libro en bytes, por bloques, y se detiene al terminar el pasaje (no carga el libro entero)
        val lines = context.assets.open("origen/${book.fileName}").use {
            BibleParser.passageLines(it, book.id, ref)
        }

        // Solo se cachean pasajes con texto: así la pantalla muestra "Texto no disponible" cada vez
        if (lines.isNotEmpty()) passageCache.put(reference, lines)
        return lines
    }
}
