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
        XCTAssertEqual(try Libros.buscar("Cantar de los Cantares"), Libro(id: 22, archivo: "cantares.z"))
        XCTAssertEqual(try Libros.buscar("2 Crónicas"), Libro(id: 14, archivo: "2_cronicas.z"))
    }

    // Formato compacto: "capítulo<TAB>versículo<TAB>texto", también sin salto final y con texto vacío
    func testFormatoCompacto() throws {
        let libro = Data("1\t1\tA\n1\t2\t\n2\t1\tC".utf8)
        XCTAssertEqual(Biblia.lineas(de: libro, referencia: try Referencia("X 1:2-2:1")),
                       ["Capítulo 1", "2 ", "Capítulo 2", "1 C"])
        XCTAssertEqual(Biblia.lineas(de: libro, referencia: try Referencia("X 1")), ["1 A", "2 "])
        XCTAssertEqual(Biblia.lineas(de: libro, referencia: try Referencia("X 3")), [])
    }

    // Los 66 libros que lleva la app, completos y en orden: cada capítulo empieza en el versículo 1
    // y no se salta ninguno (así apareció que faltaba Génesis 33:12)
    func testLibrosCompletosYEnOrden() throws {
        XCTAssertEqual(Libros.claves.count, 66)
        for clave in Libros.claves {
            let libro = try Libros.buscar(clave.replacingOccurrences(of: "_", with: " "))
            let texto = String(decoding: try Biblia.texto(de: libro), as: UTF8.self)
            var capitulo = 0
            var versiculo = 0
            for (i, linea) in texto.split(separator: "\n").enumerated() {
                let donde = "\(clave):\(i + 1)"
                let campos = linea.split(separator: "\t", maxSplits: 2, omittingEmptySubsequences: false)
                guard campos.count == 3, let c = Int(campos[0]), let v = Int(campos[1]) else {
                    XCTFail("\(donde): formato inválido")
                    continue
                }
                if c != capitulo {
                    XCTAssertEqual(c, capitulo + 1, "\(donde): capítulo")
                    capitulo = c
                    versiculo = 0
                }
                XCTAssertEqual(v, versiculo + 1, "\(donde): versículo")
                versiculo = v
            }
            XCTAssertGreaterThan(capitulo, 0, clave)
        }
    }

    // Si se cambia algún .txt de Datos/origen hay que volver a ejecutar Datos/compactar.py
    func testDatosAlDia() throws {
        let raiz = URL(fileURLWithPath: #filePath).deletingLastPathComponent()
            .deletingLastPathComponent().deletingLastPathComponent()
        let esperadas = try String(contentsOf: Recursos.libros.appendingPathComponent("origen.sha256"), encoding: .utf8)
        var huellas: [String: String] = [:] // archivo -> huella
        for fila in esperadas.split(separator: "\n") {
            let partes = fila.split(separator: " ", omittingEmptySubsequences: true)
            huellas[String(partes[1])] = String(partes[0])
        }
        XCTAssertEqual(huellas.count, 66)
        for (archivo, esperada) in huellas {
            let datos = try Data(contentsOf: raiz.appendingPathComponent("Datos/origen/\(archivo)"))
            let huella = SHA256.hash(data: datos).map { String(format: "%02x", $0) }.joined()
            XCTAssertEqual(huella, esperada, "\(archivo) cambió: ejecuta python3 Datos/compactar.py")
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
        for clave in Libros.claves {
            let libro = try Libros.buscar(clave.replacingOccurrences(of: "_", with: " "))
            for c in String(decoding: try Biblia.texto(de: libro), as: UTF8.self).unicodeScalars where caracteres[c] == nil {
                caracteres[c] = "libro \(clave)"
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
        // Separadores del formato, y el guion suave (invisible) que la fuente original tampoco tenía
        let ignorados: Set<Unicode.Scalar> = ["\n", "\r", "\t", "\u{00AD}"]

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
