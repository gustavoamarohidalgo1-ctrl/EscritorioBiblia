import CoreText
import CryptoKit
import XCTest
@testable import BibliaCore

final class BibliaTests: XCTestCase {

    // Si al preparar el plan de otro mes hay una errata en una referencia, este test lo avisa
    func testTodasLasLecturasDelPlanTienenTexto() throws {
        for dia in planSeptiembre {
            for referencia in dia.referencias {
                XCTAssertFalse(try Biblia.pasaje(referencia).isEmpty, "Día \(dia.dia): \"\(referencia)\" no tiene texto")
            }
        }
    }

    func testCapituloCompletoSinTitulos() throws {
        let lineas = try Biblia.pasaje("2 Crónicas 6")
        XCTAssertEqual(lineas.count, 42)
        XCTAssertTrue(lineas[0].hasPrefix("1 "))
    }

    func testRangoEntreCapitulosConTitulos() throws {
        let lineas = try Biblia.pasaje("Malaquías 2:17-3:18")
        XCTAssertEqual(lineas[0], "Capítulo 2")
        XCTAssertTrue(lineas[1].hasPrefix("17 "))
        XCTAssertEqual(lineas[2], "Capítulo 3")
        XCTAssertTrue(lineas.last!.hasPrefix("18 "))
        XCTAssertEqual(lineas.count, 19 + 2)
    }

    func testUnSoloVersiculo() throws {
        XCTAssertEqual(try Biblia.pasaje("Génesis 1:1"), ["1 En el principio creó Dios los cielos y la tierra."])
    }

    func testReferenciaSinVersiculosDevuelveListaVacia() throws {
        XCTAssertTrue(try Biblia.pasaje("Juan 99").isEmpty)
    }

    func testReferenciasInvalidas() {
        XCTAssertThrowsError(try Biblia.pasaje("Juan"))
        XCTAssertThrowsError(try Biblia.pasaje("Juan 3:x"))
        XCTAssertThrowsError(try Biblia.pasaje("Libro Inventado 1"))
    }

    func testReferencias() throws {
        XCTAssertEqual(try Referencia("2 Crónicas 20:1-21:1"),
                       Referencia(libro: "2 Crónicas", capituloInicio: 20, versiculoInicio: 1, capituloFin: 21, versiculoFin: 1))
        XCTAssertEqual(try Referencia("Juan 9:1-23"),
                       Referencia(libro: "Juan", capituloInicio: 9, versiculoInicio: 1, capituloFin: 9, versiculoFin: 23))
        XCTAssertEqual(try Referencia("Esdras 1-2"), Referencia(libro: "Esdras", capituloInicio: 1, capituloFin: 2))
        XCTAssertEqual(try Referencia(" 1 Juan 4 "), Referencia(libro: "1 Juan", capituloInicio: 4))
        XCTAssertEqual(try Libros.buscar("Cantar de los Cantares"), Libro(id: 22, archivo: "cantares.txt"))
        XCTAssertEqual(try Libros.buscar("2 Crónicas"), Libro(id: 14, archivo: "2_cronicas.txt"))
    }

    // El texto se limpia igual que el cleanText original (reemplazos + expresión regular sobre el texto)
    func testLimpiezaIgualQueAntes() throws {
        let textos = [
            "Hola /nmundo", "  dos  espacios  ", "\\n Cantaré\\tyo\\r", "a/b\\c", "tab\tfinal\t", "/n", "", "fin /n",
            "ñandú /n/n ¿Quién?"
        ]
        for texto in textos {
            let linea = Data("(1, 1, 7, '\(texto)'),".utf8)
            let obtenido = Biblia.lineas(de: linea, libro: 1, referencia: try Referencia("Génesis 1"))
            XCTAssertEqual(obtenido, ["7 " + limpiezaOriginal(texto)], texto)
        }
    }

    private func limpiezaOriginal(_ texto: String) -> String {
        texto
            .replacingOccurrences(of: "\\n", with: "\n")
            .replacingOccurrences(of: "/n", with: "\n")
            .replacingOccurrences(of: "\\r", with: " ")
            .replacingOccurrences(of: "\r", with: " ")
            .replacingOccurrences(of: "\\t", with: " ")
            .replacingOccurrences(of: "\t", with: " ")
            .replacingOccurrences(of: "[ \\t\\n\\x0B\\f\\r]+", with: " ", options: .regularExpression)
            .trimmingCharacters(in: .whitespacesAndNewlines)
    }

    // Los 66 libros completos y en orden: cada capítulo empieza en el versículo 1 y no se salta ninguno
    func testLibrosCompletosYEnOrden() throws {
        let formato = try NSRegularExpression(pattern: "^\\((\\d+), (\\d+), (\\d+), '.*'\\),?$")
        let archivos = try FileManager.default.contentsOfDirectory(at: Recursos.libros, includingPropertiesForKeys: nil)
            .filter { $0.pathExtension == "txt" }
        XCTAssertEqual(archivos.count, 66)
        for archivo in archivos {
            let nombre = archivo.deletingPathExtension().lastPathComponent.replacingOccurrences(of: "_", with: " ")
            let id = try Libros.buscar(nombre).id
            var capitulo = 0
            var versiculo = 0
            let texto = try String(contentsOf: archivo, encoding: .utf8)
            for (i, linea) in texto.components(separatedBy: "\n").enumerated() where !linea.isEmpty {
                let donde = "\(archivo.lastPathComponent):\(i + 1)"
                let rango = NSRange(linea.startIndex..., in: linea)
                guard let partes = formato.firstMatch(in: linea, range: rango) else {
                    XCTFail("\(donde): formato inválido")
                    continue
                }
                let numeros = (1...3).map { Int((linea as NSString).substring(with: partes.range(at: $0)))! }
                XCTAssertEqual(numeros[0], id, "\(donde): libro")
                if numeros[1] != capitulo {
                    XCTAssertEqual(numeros[1], capitulo + 1, "\(donde): capítulo")
                    capitulo = numeros[1]
                    versiculo = 0
                }
                XCTAssertEqual(numeros[2], versiculo + 1, "\(donde): versículo")
                versiculo = numeros[2]
            }
        }
    }

    /**
     Mismo texto que la versión anterior (Kotlin): Fixtures/referencias.tsv tiene, para 2.474 pasajes
     (todos los capítulos de la Biblia, las lecturas del plan y rangos al azar), cuántas líneas daba
     el lector de antes y la huella SHA-256 de su texto.
     */
    func testMismoTextoQueLaVersionAnterior() throws {
        let url = try XCTUnwrap(Bundle.module.url(forResource: "Fixtures", withExtension: nil))
            .appendingPathComponent("referencias.tsv")
        let filas = try String(contentsOf: url, encoding: .utf8).split(separator: "\n")
        XCTAssertEqual(filas.count, 2474)
        var fallos = 0
        for fila in filas {
            let campos = fila.split(separator: "\t", omittingEmptySubsequences: false)
            let referencia = String(campos[0])
            let lineas = try Biblia.pasaje(referencia)
            let huella = SHA256.hash(data: Data(lineas.joined(separator: "\n").utf8))
                .map { String(format: "%02x", $0) }.joined()
            if lineas.count != Int(campos[1]) || huella != String(campos[2]) {
                fallos += 1
                if fallos <= 5 { XCTFail("\"\(referencia)\": \(lineas.count) líneas, se esperaban \(campos[1])") }
            }
        }
        XCTAssertEqual(fallos, 0)
    }

    /**
     La fuente Lora solo trae los caracteres que la app usa (así pesa menos). Si algún texto nuevo
     usa un carácter que no tiene, se vería con otra fuente: este test lo avisa.
     */
    func testLasFuentesCubrenTodosLosTextos() throws {
        var caracteres: [Unicode.Scalar: String] = [:] // carácter -> dónde aparece
        for dia in planSeptiembre {
            for referencia in dia.referencias {
                for c in referencia.unicodeScalars where caracteres[c] == nil { caracteres[c] = "plan: \(referencia)" }
            }
        }
        for archivo in try FileManager.default.contentsOfDirectory(at: Recursos.libros, includingPropertiesForKeys: nil) {
            for c in try String(contentsOf: archivo, encoding: .utf8).unicodeScalars where caracteres[c] == nil {
                caracteres[c] = "origen/\(archivo.lastPathComponent)"
            }
        }
        // Textos de la app: el contenido de las comillas en Sources/
        let fuentesSwift = URL(fileURLWithPath: #filePath).deletingLastPathComponent()
            .deletingLastPathComponent().deletingLastPathComponent().appendingPathComponent("Sources")
        let enumerador = FileManager.default.enumerator(at: fuentesSwift, includingPropertiesForKeys: nil)
        while let archivo = enumerador?.nextObject() as? URL {
            guard archivo.pathExtension == "swift" else { continue }
            for c in literales(try String(contentsOf: archivo, encoding: .utf8)) where caracteres[c] == nil {
                caracteres[c] = "\(archivo.lastPathComponent): texto"
            }
        }
        // Saltos de línea, y el guion suave (invisible) que la fuente original tampoco tenía
        let ignorados: Set<Unicode.Scalar> = ["\n", "\r", "\u{00AD}"]

        for archivo in ["lora_regular.ttf", "lora_bold.ttf"] {
            let url = Recursos.fuentes.appendingPathComponent(archivo)
            let descriptores = try XCTUnwrap(CTFontManagerCreateFontDescriptorsFromURL(url as CFURL) as? [CTFontDescriptor])
            let fuente = CTFontCreateWithFontDescriptor(try XCTUnwrap(descriptores.first), 12, nil)
            let faltan = caracteres.filter { c, _ in !ignorados.contains(c) && !tieneGlifo(fuente, c) }
            XCTAssertTrue(faltan.isEmpty, "\(archivo) no tiene: " + faltan
                .map { String(format: "U+%04X '%@' (%@)", $0.key.value, String($0.key), $0.value) }
                .joined(separator: ", "))
        }
    }

    private func tieneGlifo(_ fuente: CTFont, _ c: Unicode.Scalar) -> Bool {
        var utf16 = Array(String(c).utf16)
        var glifos = [CGGlyph](repeating: 0, count: utf16.count)
        return CTFontGetGlyphsForCharacters(fuente, &utf16, &glifos, utf16.count)
    }

    /// Caracteres dentro de los textos "…" y """…""" de un archivo Swift (sin comentarios)
    private func literales(_ codigo: String) -> [Unicode.Scalar] {
        let c = Array(codigo.unicodeScalars)
        var salida: [Unicode.Scalar] = []
        var i = 0
        func empieza(_ texto: String, _ en: Int) -> Bool {
            let t = Array(texto.unicodeScalars)
            return en + t.count <= c.count && Array(c[en..<en + t.count]) == t
        }
        while i < c.count {
            if empieza("//", i) {
                while i < c.count && c[i] != "\n" { i += 1 }
            } else if empieza("/*", i) {
                i += 2
                while i < c.count && !empieza("*/", i) { i += 1 }
                i += 2
            } else if empieza("\"\"\"", i) {
                i += 3
                while i < c.count && !empieza("\"\"\"", i) { salida.append(c[i]); i += 1 }
                i += 3
            } else if c[i] == "\"" {
                i += 1
                while i < c.count && c[i] != "\"" && c[i] != "\n" {
                    if c[i] == "\\" { i += 1 } else { salida.append(c[i]) }
                    i += 1
                }
                i += 1
            } else {
                i += 1
            }
        }
        return salida
    }
}
