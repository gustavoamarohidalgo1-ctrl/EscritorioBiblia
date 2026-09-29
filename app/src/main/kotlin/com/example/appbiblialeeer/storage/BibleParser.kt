package com.example.appbiblialeeer.storage

import java.io.InputStream
import java.text.Normalizer

data class BookInfo(val id: Int, val fileName: String)

data class ReadingRef(
    val bookName: String,
    val startChapter: Int,
    val startVerse: Int? = null,
    val endChapter: Int? = null,
    val endVerse: Int? = null
)

/**
 * Lógica pura (sin interfaz) para leer los libros de resources/origen.
 * Cada línea tiene el formato (libro, capítulo, versículo, 'texto'), en orden,
 * así que solo se procesan las líneas del rango pedido y la lectura se corta al pasarlo.
 */
internal object BibleParser {

    // ✅ Regex compiladas una sola vez (antes se creaban en cada llamada)
    private val referenceRegex = Regex("""^(.+?)\s+(\d.*)$""")
    private val whitespaceRegex = Regex("\\s+")
    private val diacriticsRegex = Regex("\\p{InCombiningDiacriticalMarks}+")
    private val underscoresRegex = Regex("__+")

    // ✅ El libro se lee en bloques de bytes (sin BufferedReader ni un String por línea)
    private const val BLOCK_SIZE = 16 * 1024

    // Bytes ASCII del formato: en UTF-8 nunca aparecen dentro de una letra con tilde o una ñ
    private const val NEWLINE: Byte = 10 // '\n'
    private const val SPACE: Byte = 32 // ' '
    private const val QUOTE: Byte = 39 // '\''
    private const val OPEN_PAREN: Byte = 40 // '('
    private const val CLOSE_PAREN: Byte = 41 // ')'
    private const val COMMA: Byte = 44 // ','
    private const val SLASH: Byte = 47 // '/'
    private const val ZERO: Byte = 48 // '0'
    private const val BACKSLASH: Byte = 92 // '\\'
    private const val LETTER_N: Byte = 110 // 'n'
    private const val LETTER_R: Byte = 114 // 'r'
    private const val LETTER_T: Byte = 116 // 't'

    // Tipo de cada byte para isClean: 1 = espacio, 2 = hay que revisar ('/', '\\' y controles)
    private const val SPACE_KIND = 1
    private const val CHECK = 2
    private val BYTE_KIND = ByteArray(256).also { kinds ->
        for (b in 0 until 32) kinds[b] = CHECK.toByte()
        kinds[SPACE.toInt()] = SPACE_KIND.toByte()
        kinds[SLASH.toInt()] = CHECK.toByte()
        kinds[BACKSLASH.toInt()] = CHECK.toByte()
    }

    /**
     * Líneas a mostrar: "N texto" por versículo, con "Capítulo N" antes de cada capítulo
     * cuando la referencia es un rango. Lista vacía si el rango no tiene versículos.
     *
     * ✅ Trabaja sobre los bytes UTF-8 del libro: las líneas anteriores al pasaje no se recorren
     * ni se convierten a texto (ver skipLinesBeforeRange) y solo se decodifica el texto de los
     * versículos pedidos.
     */
    fun passageLines(input: InputStream, bookId: Int, ref: ReadingRef): List<String> {
        val range = VerseRange(ref)
        val showChapterTitles = ref.endChapter != null
        val numbers = IntArray(3)
        val out = ArrayList<String>()
        var currentChapter = -1

        var buf = ByteArray(BLOCK_SIZE)
        var verseBytes = ByteArray(512) // versículo ya limpio, antes de pasarlo a String
        var start = 0 // inicio de la línea actual en buf
        var end = 0 // bytes válidos en buf
        var eof = false

        while (true) {
            var lineEnd = indexOf(NEWLINE, buf, start, end)
            if (lineEnd < 0) {
                if (!eof) {
                    // Lleva el trozo de línea pendiente al inicio y lee el siguiente bloque
                    val pending = end - start
                    if (pending == buf.size) buf = buf.copyOf(buf.size * 2) // línea más larga que el bloque
                    else System.arraycopy(buf, start, buf, 0, pending)
                    val read = input.read(buf, pending, buf.size - pending)
                    eof = read < 0
                    end = if (eof) pending else pending + read
                    // Mientras no empiece el pasaje, las líneas anteriores se saltan de golpe
                    start = if (out.isEmpty()) skipLinesBeforeRange(buf, 0, end, bookId, range, numbers) else 0
                    continue
                }
                if (start >= end) break
                lineEnd = end // última línea del libro, sin salto final
            }

            val textStart = parseNumbers(buf, start, lineEnd, numbers)
            if (textStart >= 0 && numbers[0] == bookId) {
                val chapter = numbers[1]
                if (chapter > range.lastChapter) break // ya pasamos el rango: no hace falta leer más
                val verse = numbers[2]
                val close = if (range.contains(chapter, verse)) closingQuote(buf, textStart, lineEnd) else -1
                if (close >= 0) {
                    if (showChapterTitles && chapter != currentChapter) {
                        currentChapter = chapter
                        out.add("Capítulo $chapter")
                    }
                    val needed = close - textStart + 10 // número (máx. 9 cifras) + espacio + texto
                    if (verseBytes.size < needed) verseBytes = ByteArray(needed)
                    out.add(verseLine(verse, buf, textStart, close, verseBytes))
                }
            }
            start = lineEnd + 1
        }
        return out
    }

    /**
     * ✅ Salto rápido: el libro está en orden, así que las líneas del bloque anteriores al pasaje
     * se descartan con una búsqueda binaria, sin recorrerlas una a una. Se empieza por la última
     * línea completa: si aún va antes del pasaje (lo habitual), se salta el bloque entero.
     * Devuelve dónde seguir leyendo.
     */
    private fun skipLinesBeforeRange(
        buf: ByteArray, start: Int, end: Int, bookId: Int, range: VerseRange, numbers: IntArray
    ): Int {
        var from = start // todo lo anterior a 'from' va antes del pasaje
        var to = lastIndexOf(NEWLINE, buf, start, end) + 1 // fin de la última línea completa
        var probe = to - 1
        while (from < to) {
            // Línea que contiene 'probe'
            val lineStart = maxOf(from, lastIndexOf(NEWLINE, buf, from, probe) + 1)
            val lineEnd = indexOf(NEWLINE, buf, probe, to)
            val before = parseNumbers(buf, lineStart, lineEnd, numbers) >= 0 &&
                numbers[0] == bookId && range.isBefore(numbers[1], numbers[2])
            if (before) from = lineEnd + 1 else to = lineStart
            probe = (from + to) ushr 1
        }
        return from
    }

    // --- parse line ---

    /**
     * Lee "(libro, capítulo, versículo, '" sin regex. Devuelve dónde empieza el texto, o -1.
     * Números de 9 cifras como mucho (así nunca se desbordan).
     */
    private fun parseNumbers(buf: ByteArray, from: Int, to: Int, out: IntArray): Int {
        var i = from
        while (i < to && isTrimSpace(buf[i])) i++
        if (i >= to || buf[i] != OPEN_PAREN) return -1
        i++
        for (k in 0 until 3) {
            while (i < to && isSpace(buf[i])) i++
            val digitsStart = i
            var value = 0
            while (i < to) {
                val digit = buf[i] - ZERO
                if (digit < 0 || digit > 9) break
                value = value * 10 + digit
                i++
            }
            if (i == digitsStart || i - digitsStart > 9) return -1
            out[k] = value
            while (i < to && isSpace(buf[i])) i++
            if (i >= to || buf[i] != COMMA) return -1
            i++
        }
        while (i < to && isSpace(buf[i])) i++
        if (i >= to || buf[i] != QUOTE) return -1
        return i + 1
    }

    /** Comilla que cierra el texto (se busca desde el final); la línea debe terminar en "'),". -1 si no. */
    private fun closingQuote(buf: ByteArray, textStart: Int, lineEnd: Int): Int {
        var i = lineEnd - 1
        while (i >= textStart && isTrimSpace(buf[i])) i--
        if (i >= textStart && buf[i] == COMMA) i--
        if (i < textStart || buf[i] != CLOSE_PAREN) return -1
        i--
        while (i >= textStart && isSpace(buf[i])) i--
        return if (i >= textStart && buf[i] == QUOTE) i else -1
    }

    /**
     * "N texto" con el texto limpio, igual que el antiguo cleanText: las marcas \n, /n, \r, \t
     * y cualquier grupo de espacios quedan en un solo espacio, sin espacios en los extremos.
     * ✅ Se trabaja en bytes y se decodifica UTF-8 una sola vez, directo al String final.
     */
    private fun verseLine(verse: Int, src: ByteArray, from: Int, to: Int, dst: ByteArray): String {
        var n = writeNumber(verse, dst)
        dst[n++] = SPACE
        val textStart = n
        if (isClean(src, from, to)) {
            // ✅ Lo habitual: el texto ya viene limpio y se copia tal cual
            System.arraycopy(src, from, dst, n, to - from)
            n += to - from
        } else {
            n = cleanText(src, from, to, dst, n)
        }
        val line = String(dst, 0, n, Charsets.UTF_8)
        // Espacios Unicode en los extremos (no hay en los libros): mismo resultado que trim()
        return if (n > textStart && (line[textStart].isWhitespace() || line.last().isWhitespace())) {
            "$verse ${line.substring(textStart).trim()}"
        } else {
            line
        }
    }

    /**
     * true si no hay nada que limpiar: ni marcas, ni controles, ni espacios dobles o en los extremos.
     * Sin un "if" por byte: los espacios, cada pocas letras, harían fallar la predicción de saltos.
     */
    private fun isClean(src: ByteArray, from: Int, to: Int): Boolean {
        var dirty = 0
        var prevSpace = 1 // un espacio al inicio también cuenta como "doble"
        for (i in from until to) {
            val kind = BYTE_KIND[src[i].toInt() and 0xFF].toInt()
            dirty = dirty or (kind and (CHECK or prevSpace))
            prevSpace = kind and SPACE_KIND
        }
        return (dirty or prevSpace) == 0
    }

    /** Copia el texto a [dst] desde [start] quitando marcas y espacios sobrantes; devuelve el nuevo final. */
    private fun cleanText(src: ByteArray, from: Int, to: Int, dst: ByteArray, start: Int): Int {
        var n = start
        var pendingSpace = false // espacio por escribir: solo se escribe si después sigue texto
        var i = from
        while (i < to) {
            val b = src[i++]
            if (b == SLASH || b == BACKSLASH) {
                // Marcas \n, /n, \r y \t: cuentan como espacio
                val next: Byte = if (i < to) src[i] else 0
                if (next == LETTER_N || (b == BACKSLASH && (next == LETTER_R || next == LETTER_T))) {
                    i++
                    pendingSpace = n > start // los espacios del inicio se descartan
                    continue
                }
            } else if (isSpace(b)) {
                pendingSpace = n > start
                continue
            }
            if (pendingSpace) {
                dst[n++] = SPACE
                pendingSpace = false
            }
            dst[n++] = b
        }
        return n
    }

    /** Escribe [value] (≥ 0) en ASCII al inicio de [dst]; devuelve cuántos bytes ocupa. */
    private fun writeNumber(value: Int, dst: ByteArray): Int {
        var digits = 1
        var rest = value / 10
        while (rest > 0) {
            digits++
            rest /= 10
        }
        var v = value
        for (i in digits - 1 downTo 0) {
            dst[i] = (ZERO + v % 10).toByte()
            v /= 10
        }
        return digits
    }

    // Espacios de la regex \s
    private fun isSpace(b: Byte) = b == SPACE || (b >= 9 && b <= 13)

    // Espacios que quita trim() en los extremos de la línea (los ASCII, los únicos que hay en los libros)
    private fun isTrimSpace(b: Byte) = isSpace(b) || (b >= 28 && b <= 31)

    private fun indexOf(b: Byte, buf: ByteArray, from: Int, to: Int): Int {
        for (i in from until to) if (buf[i] == b) return i
        return -1
    }

    private fun lastIndexOf(b: Byte, buf: ByteArray, from: Int, to: Int): Int {
        for (i in to - 1 downTo from) if (buf[i] == b) return i
        return -1
    }

    // --- parse reading ---
    fun parseReading(input: String): ReadingRef {
        val s = input.trim()
        val m = referenceRegex.find(s) ?: error("Referencia inválida: $input")

        val bookName = m.groupValues[1].trim()
        val rest = m.groupValues[2].trim()

        return when {
            rest.contains(":") -> {
                val parts = rest.split("-", limit = 2)
                val (sc, sv) = parts[0].split(":").map { it.toInt() }
                if (parts.size == 1) ReadingRef(bookName, sc, sv)
                else {
                    val right = parts[1]
                    if (right.contains(":")) {
                        val (ec, ev) = right.split(":").map { it.toInt() }
                        ReadingRef(bookName, sc, sv, ec, ev)
                    } else {
                        ReadingRef(bookName, sc, sv, sc, right.toInt())
                    }
                }
            }
            rest.contains("-") -> {
                val (sc, ec) = rest.split("-").map { it.toInt() }
                ReadingRef(bookName, sc, null, ec, null)
            }
            else -> ReadingRef(bookName, rest.toInt())
        }
    }

    /**
     * Capítulo completo, rango de capítulos, rango de versos (en uno o varios capítulos) o un solo
     * versículo, como intervalo en el orden (capítulo, versículo) del libro.
     */
    private class VerseRange(ref: ReadingRef) {
        private val firstChapter = ref.startChapter
        private val firstVerse = ref.startVerse ?: Int.MIN_VALUE
        val lastChapter = ref.endChapter ?: ref.startChapter
        private val lastVerse =
            ref.endVerse ?: (if (ref.endChapter == null) ref.startVerse else null) ?: Int.MAX_VALUE

        fun isBefore(chapter: Int, verse: Int) =
            chapter < firstChapter || (chapter == firstChapter && verse < firstVerse)

        fun contains(chapter: Int, verse: Int) = !isBefore(chapter, verse) &&
            (chapter < lastChapter || (chapter == lastChapter && verse <= lastVerse))
    }

    // --- books ---
    fun resolveBook(bookName: String): BookInfo {
        val key = aliasKey(normalizeKey(bookName))
        val id = BOOK_ID_BY_KEY[key] ?: error("Libro no mapeado: $bookName (key=$key)")
        return BookInfo(id, "$key.txt")
    }

    private fun normalizeKey(s: String): String {
        val lower = s.lowercase().trim()
        val noAccents = Normalizer.normalize(lower, Normalizer.Form.NFD).replace(diacriticsRegex, "")
        return noAccents
            .replace(".", "")
            .replace("-", "_")
            .replace(whitespaceRegex, "_")
            .replace(underscoresRegex, "_")
            .trim('_')
    }

    private fun aliasKey(key: String): String = when (key) {
        "cantar_de_los_cantares" -> "cantares"
        "cantar_de_cantares" -> "cantares"
        else -> key
    }

    // Orden canónico: el id de cada libro es su posición (genesis = 1 … apocalipsis = 66)
    private val BOOK_ID_BY_KEY = listOf(
        "genesis", "exodo", "levitico", "numeros", "deuteronomio", "josue", "jueces", "rut",
        "1_samuel", "2_samuel", "1_reyes", "2_reyes", "1_cronicas", "2_cronicas", "esdras",
        "nehemias", "ester", "job", "salmos", "proverbios", "eclesiastes", "cantares", "isaias",
        "jeremias", "lamentaciones", "ezequiel", "daniel", "oseas", "joel", "amos", "abdias",
        "jonas", "miqueas", "nahum", "habacuc", "sofonias", "hageo", "zacarias", "malaquias",
        "mateo", "marcos", "lucas", "juan", "hechos", "romanos", "1_corintios", "2_corintios",
        "galatas", "efesios", "filipenses", "colosenses", "1_tesalonicenses", "2_tesalonicenses",
        "1_timoteo", "2_timoteo", "tito", "filemon", "hebreos", "santiago", "1_pedro", "2_pedro",
        "1_juan", "2_juan", "3_juan", "judas", "apocalipsis"
    ).withIndex().associate { (index, key) -> key to index + 1 }
}
