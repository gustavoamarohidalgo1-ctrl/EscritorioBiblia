package com.example.appbiblialeeer.storage

import java.io.InputStream

object BibleTextStorage {

    // ✅ Cachea resultado por referencia (ej "Hechos 2:1-13"): los 60 pasajes usados más recientemente
    private val passageCache = object : LinkedHashMap<String, List<String>>(16, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, List<String>>) = size > 60
    }

    /** Pasaje ya cargado, para mostrarlo al instante sin esperar. */
    fun cachedPassage(reference: String): List<String>? = synchronized(passageCache) { passageCache[reference] }

    /**
     * Prefetch: precarga lecturas de un día (para que al tocar cargue instantáneo).
     * Llamar fuera del hilo principal.
     */
    fun prefetchDay(references: List<String>) {
        references.forEach { ref ->
            if (cachedPassage(ref) == null) {
                runCatching { getPassageLines(ref) }
            }
        }
    }

    /** Líneas del pasaje; lista vacía si la referencia no tiene versículos. Llamar fuera del hilo principal. */
    fun getPassageLines(reference: String): List<String> {
        cachedPassage(reference)?.let { return it }

        val ref = BibleParser.parseReading(reference)
        val book = BibleParser.resolveBook(ref.bookName)
        // ✅ Lee el libro en bytes, por bloques, y se detiene al terminar el pasaje (no carga el libro entero)
        val lines = openBook(book.fileName).use {
            BibleParser.passageLines(it, book.id, ref)
        }

        // Solo se cachean pasajes con texto: así la pantalla muestra "Texto no disponible" cada vez
        if (lines.isNotEmpty()) synchronized(passageCache) { passageCache[reference] = lines }
        return lines
    }

    // Los libros van dentro de la app, en resources/origen
    private fun openBook(fileName: String): InputStream =
        BibleTextStorage::class.java.getResourceAsStream("/origen/$fileName")
            ?: error("Libro no encontrado: $fileName")
}
