import Foundation

/// Un libro de la Biblia: su posición en el orden canónico (Génesis = 1 … Apocalipsis = 66),
/// que es el primer número de cada línea de su archivo, y el archivo en resources/origen.
public struct Libro: Equatable, Sendable {
    public let id: Int
    public let archivo: String
}

public enum Libros {

    // Orden canónico: el id de cada libro es su posición
    static let claves = [
        "genesis", "exodo", "levitico", "numeros", "deuteronomio", "josue", "jueces", "rut",
        "1_samuel", "2_samuel", "1_reyes", "2_reyes", "1_cronicas", "2_cronicas", "esdras",
        "nehemias", "ester", "job", "salmos", "proverbios", "eclesiastes", "cantares", "isaias",
        "jeremias", "lamentaciones", "ezequiel", "daniel", "oseas", "joel", "amos", "abdias",
        "jonas", "miqueas", "nahum", "habacuc", "sofonias", "hageo", "zacarias", "malaquias",
        "mateo", "marcos", "lucas", "juan", "hechos", "romanos", "1_corintios", "2_corintios",
        "galatas", "efesios", "filipenses", "colosenses", "1_tesalonicenses", "2_tesalonicenses",
        "1_timoteo", "2_timoteo", "tito", "filemon", "hebreos", "santiago", "1_pedro", "2_pedro",
        "1_juan", "2_juan", "3_juan", "judas", "apocalipsis"
    ]

    private static let idPorClave = Dictionary(uniqueKeysWithValues: claves.enumerated().map { ($0.element, $0.offset + 1) })

    /// "2 Crónicas", "Cantar de los Cantares", "1 juan"… → su libro
    public static func buscar(_ nombre: String) throws -> Libro {
        let clave = alias(normalizar(nombre))
        guard let id = idPorClave[clave] else { throw ErrorBiblia.libroDesconocido(nombre) }
        return Libro(id: id, archivo: "\(clave).txt")
    }

    /// Minúsculas, sin tildes ni puntos y con "_" en vez de espacios: "2 Crónicas" → "2_cronicas"
    static func normalizar(_ nombre: String) -> String {
        nombre.lowercased()
            .trimmingCharacters(in: .whitespacesAndNewlines)
            .folding(options: .diacriticInsensitive, locale: nil)
            .replacingOccurrences(of: ".", with: "")
            .replacingOccurrences(of: "-", with: "_")
            .replacingOccurrences(of: "\\s+", with: "_", options: .regularExpression)
            .replacingOccurrences(of: "__+", with: "_", options: .regularExpression)
            .trimmingCharacters(in: CharacterSet(charactersIn: "_"))
    }

    private static func alias(_ clave: String) -> String {
        switch clave {
        case "cantar_de_los_cantares", "cantar_de_cantares": return "cantares"
        default: return clave
        }
    }
}
