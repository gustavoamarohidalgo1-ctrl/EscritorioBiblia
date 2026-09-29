import Foundation

public enum ErrorBiblia: Error, LocalizedError, Equatable {
    case referenciaInvalida(String)
    case libroDesconocido(String)

    public var errorDescription: String? {
        switch self {
        case .referenciaInvalida(let texto): return "Referencia inválida: \(texto)"
        case .libroDesconocido(let libro): return "Libro no encontrado: \(libro)"
        }
    }
}

/**
 Un pasaje del plan: capítulo completo ("Juan 17"), rango de capítulos ("Esdras 1-2"),
 rango de versículos en uno o varios capítulos ("Juan 9:1-23", "Malaquías 2:17-3:18")
 o un solo versículo ("Génesis 1:1").
 */
public struct Referencia: Equatable, Sendable {
    public let libro: String
    public let capituloInicio: Int
    public let versiculoInicio: Int?
    public let capituloFin: Int?
    public let versiculoFin: Int?

    public init(libro: String, capituloInicio: Int, versiculoInicio: Int? = nil,
                capituloFin: Int? = nil, versiculoFin: Int? = nil) {
        self.libro = libro
        self.capituloInicio = capituloInicio
        self.versiculoInicio = versiculoInicio
        self.capituloFin = capituloFin
        self.versiculoFin = versiculoFin
    }

    public init(_ texto: String) throws {
        let s = texto.trimmingCharacters(in: .whitespacesAndNewlines)
        guard let separado = Referencia.separar(s) else { throw ErrorBiblia.referenciaInvalida(texto) }
        let (libro, resto) = separado

        func numeros(_ parte: Substring, _ separador: Character) throws -> [Int] {
            try parte.split(separator: separador, omittingEmptySubsequences: false).map {
                guard let n = Int($0) else { throw ErrorBiblia.referenciaInvalida(texto) }
                return n
            }
        }

        if resto.contains(":") {
            let partes = resto.split(separator: "-", maxSplits: 1, omittingEmptySubsequences: false)
            let inicio = try numeros(partes[0], ":")
            guard inicio.count >= 2 else { throw ErrorBiblia.referenciaInvalida(texto) }
            if partes.count == 1 {
                self.init(libro: libro, capituloInicio: inicio[0], versiculoInicio: inicio[1])
            } else if partes[1].contains(":") {
                let fin = try numeros(partes[1], ":")
                guard fin.count >= 2 else { throw ErrorBiblia.referenciaInvalida(texto) }
                self.init(libro: libro, capituloInicio: inicio[0], versiculoInicio: inicio[1],
                          capituloFin: fin[0], versiculoFin: fin[1])
            } else {
                let fin = try numeros(partes[1], ":")
                self.init(libro: libro, capituloInicio: inicio[0], versiculoInicio: inicio[1],
                          capituloFin: inicio[0], versiculoFin: fin[0])
            }
        } else if resto.contains("-") {
            let capitulos = try numeros(Substring(resto), "-")
            guard capitulos.count >= 2 else { throw ErrorBiblia.referenciaInvalida(texto) }
            self.init(libro: libro, capituloInicio: capitulos[0], capituloFin: capitulos[1])
        } else {
            guard let capitulo = Int(resto) else { throw ErrorBiblia.referenciaInvalida(texto) }
            self.init(libro: libro, capituloInicio: capitulo)
        }
    }

    /// Separa "2 Crónicas 20:1-21:1" en ("2 Crónicas", "20:1-21:1"): el libro termina en el primer
    /// espacio seguido de un número (igual que la expresión ^(.+?)\s+(\d.*)$ de la versión anterior).
    static func separar(_ s: String) -> (String, String)? {
        let letras = Array(s)
        var i = 1
        while i < letras.count {
            guard esEspacio(letras[i]) else {
                i += 1
                continue
            }
            var j = i
            while j < letras.count && esEspacio(letras[j]) { j += 1 }
            if j < letras.count, let ascii = letras[j].asciiValue, ascii >= 48, ascii <= 57 {
                return (String(letras[..<i]), String(letras[j...]))
            }
            i = j
        }
        return nil
    }

    private static func esEspacio(_ c: Character) -> Bool {
        c == " " || c == "\t" || c == "\n" || c == "\u{0B}" || c == "\u{0C}" || c == "\r"
    }
}
