package com.example.appbiblialeeer

import com.example.appbiblialeeer.data.biblePlan
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.ByteBuffer

/**
 * Las fuentes Lora de res/font solo traen los caracteres que la app usa (así pesan menos).
 * Si algún texto nuevo (un título, una referencia del plan de otro mes) usa un carácter que no
 * tienen, se vería con la fuente del sistema: este test lo avisa para regenerarlas.
 */
class FuentesTest {

    // Los tests locales se ejecutan desde la carpeta app/
    private val fuentes = listOf("src/main/res/font/lora_regular.ttf", "src/main/res/font/lora_bold.ttf")

    // La fuente original tampoco tenía el guion suave (invisible): siempre usó la del sistema
    private val sinGlifoDesdeSiempre = setOf(0x00AD)

    @Test
    fun lasFuentesCubrenTodosLosTextos() {
        val textos = mutableMapOf<Int, String>() // carácter -> dónde aparece
        biblePlan.forEach { dia -> dia.referencias.forEach { ref -> ref.forEach { textos.putIfAbsent(it.code, "BiblePlan: $ref") } } }
        File("src/main/java").walk().filter { it.extension == "kt" }.forEach { archivo ->
            literales(archivo.readText()).forEach { textos.putIfAbsent(it.code, "${archivo.name}: literal de texto") }
        }
        File("src/main/assets/origen").listFiles()!!.forEach { libro ->
            libro.readText().forEach { if (it != '\n' && it != '\r') textos.putIfAbsent(it.code, "origen/${libro.name}") }
        }

        for (ruta in fuentes) {
            val cubiertos = codigosCubiertos(File(ruta).readBytes())
            val faltan = textos.filterKeys { it !in cubiertos && it !in sinGlifoDesdeSiempre }
            assertTrue(
                "$ruta no tiene: " + faltan.entries.joinToString { (c, donde) -> "U+%04X '%c' (%s)".format(c, c, donde) },
                faltan.isEmpty()
            )
        }
    }

    /** Caracteres dentro de los literales "..." y """...""" de un archivo Kotlin (sin comentarios). */
    private fun literales(codigo: String): List<Char> {
        val out = mutableListOf<Char>()
        var i = 0
        while (i < codigo.length) {
            when {
                codigo.startsWith("//", i) -> i = codigo.indexOf('\n', i).let { if (it < 0) codigo.length else it }
                codigo.startsWith("/*", i) -> i = codigo.indexOf("*/", i + 2).let { if (it < 0) codigo.length else it + 2 }
                codigo.startsWith("\"\"\"", i) -> {
                    val fin = codigo.indexOf("\"\"\"", i + 3)
                    out.addAll(codigo.substring(i + 3, fin).toList()); i = fin + 3
                }
                codigo[i] == '"' || codigo[i] == '\'' -> {
                    val comilla = codigo[i]
                    i++
                    while (i < codigo.length && codigo[i] != comilla) {
                        if (codigo[i] == '\\') i++ else out.add(codigo[i])
                        i++
                    }
                    i++
                }
                else -> i++
            }
        }
        return out
    }

    /** Códigos de la tabla cmap (formato 4, Unicode BMP) de un archivo TrueType. */
    private fun codigosCubiertos(ttf: ByteArray): Set<Int> {
        val b = ByteBuffer.wrap(ttf)
        val numTablas = b.getShort(4).toInt() and 0xFFFF
        val cmap = (0 until numTablas).map { 12 + 16 * it }
            .first { String(ttf, it, 4, Charsets.US_ASCII) == "cmap" }
            .let { b.getInt(it + 8) }
        val subtablas = b.getShort(cmap + 2).toInt() and 0xFFFF
        val sub = (0 until subtablas).map { cmap + 4 + 8 * it }
            .first { b.getShort(it).toInt() == 3 && b.getShort(it + 2).toInt() == 1 }
            .let { cmap + b.getInt(it + 4) }
        check(b.getShort(sub).toInt() == 4) { "se esperaba cmap formato 4" }
        val segmentos = (b.getShort(sub + 6).toInt() and 0xFFFF) / 2
        val fines = sub + 14
        val inicios = fines + 2 * segmentos + 2
        val deltas = inicios + 2 * segmentos
        val rangos = deltas + 2 * segmentos
        val cubiertos = mutableSetOf<Int>()
        for (s in 0 until segmentos) {
            val inicio = b.getShort(inicios + 2 * s).toInt() and 0xFFFF
            val fin = b.getShort(fines + 2 * s).toInt() and 0xFFFF
            val delta = b.getShort(deltas + 2 * s).toInt()
            val rango = b.getShort(rangos + 2 * s).toInt() and 0xFFFF
            for (c in inicio..fin) {
                if (c == 0xFFFF) continue
                val glifo = if (rango == 0) (c + delta) and 0xFFFF else {
                    val g = b.getShort(rangos + 2 * s + rango + 2 * (c - inicio)).toInt() and 0xFFFF
                    if (g == 0) 0 else (g + delta) and 0xFFFF
                }
                if (glifo != 0) cubiertos.add(c)
            }
        }
        return cubiertos
    }
}
