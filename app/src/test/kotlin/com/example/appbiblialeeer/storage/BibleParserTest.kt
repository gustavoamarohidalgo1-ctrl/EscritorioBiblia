package com.example.appbiblialeeer.storage

import com.example.appbiblialeeer.data.biblePlan
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.File
import java.io.FilterInputStream
import java.io.InputStream

class BibleParserTest {

    // Los tests locales se ejecutan desde la carpeta app/
    private fun passage(reference: String, wrap: (InputStream) -> InputStream = { it }): List<String> {
        val ref = BibleParser.parseReading(reference)
        val book = BibleParser.resolveBook(ref.bookName)
        return wrap(File("src/main/resources/origen/${book.fileName}").inputStream()).use {
            BibleParser.passageLines(it, book.id, ref)
        }
    }

    // Entrega el libro en trozos pequeños y variables: los cortes de bloque caen en cualquier byte
    private class PorTrozos(input: InputStream) : FilterInputStream(input) {
        private var lecturas = 0
        override fun read(b: ByteArray, off: Int, len: Int) = super.read(b, off, minOf(len, 1 + lecturas++ % 53))
    }

    // Si al preparar el plan de otro mes hay una errata en una referencia, este test lo avisa
    @Test
    fun todasLasLecturasDelPlanTienenTexto() {
        for (dia in biblePlan) {
            for (referencia in dia.referencias) {
                assertTrue("Día ${dia.dia}: \"$referencia\" no tiene texto", passage(referencia).isNotEmpty())
            }
        }
    }

    @Test
    fun capituloCompletoSinTitulos() {
        val lineas = passage("2 Crónicas 6")
        assertEquals(42, lineas.size)
        assertTrue(lineas.first().startsWith("1 "))
    }

    @Test
    fun rangoEntreCapitulosConTitulos() {
        val lineas = passage("Malaquías 2:17-3:18")
        assertEquals("Capítulo 2", lineas[0])
        assertTrue(lineas[1].startsWith("17 "))
        assertEquals("Capítulo 3", lineas[2])
        assertTrue(lineas.last().startsWith("18 "))
        assertEquals(19 + 2, lineas.size)
    }

    @Test
    fun unSoloVersiculo() {
        assertEquals(listOf("1 En el principio creó Dios los cielos y la tierra."), passage("Génesis 1:1"))
    }

    @Test
    fun leerPorTrozosDaLoMismo() {
        val referencias = listOf("Salmos 119", "Salmos 78:21-37", "2 Crónicas 20:1-21:1", "Judas 1", "Apocalipsis 22")
        for (referencia in referencias) {
            assertEquals(referencia, passage(referencia), passage(referencia) { PorTrozos(it) })
        }
    }

    // El texto se limpia en bytes igual que el cleanText original (reemplazos + regex sobre el String)
    @Test
    fun limpiezaIgualQueAntes() {
        val textos = listOf(
            "Hola /nmundo", "  dos  espacios  ", "\\n Cantaré\\tyo\\r", "a/b\\c", "tab\tfinal\t", "/n", "", "fin /n",
            "ñandú /n/n ¿Quién?"
        )
        for (texto in textos) {
            val linea = "(1, 1, 7, '$texto'),"
            val obtenido = BibleParser.passageLines(
                ByteArrayInputStream(linea.toByteArray()), 1, BibleParser.parseReading("Génesis 1")
            )
            assertEquals(texto, listOf("7 " + cleanTextOriginal(texto)), obtenido)
        }
    }

    private fun cleanTextOriginal(raw: String) = raw
        .replace("\\n", "\n")
        .replace("/n", "\n")
        .replace("\\r", " ")
        .replace("\r", " ")
        .replace("\\t", " ")
        .replace("\t", " ")
        .replace(Regex("\\s+"), " ")
        .trim()

    @Test
    fun referenciaSinVersiculosDevuelveListaVacia() {
        assertTrue(passage("Juan 99").isEmpty())
    }
}
