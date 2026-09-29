import BibliaCore
import Foundation

/**
 Lecturas y días completados, y por dónde ibas en cada lectura.

 Se guarda con UserDefaults (en ~/Library/Preferences), con las mismas claves que la versión de
 Android: "Day_5" para el día y "Day_5_Salmos 75" para cada lectura.
 */
final class Progreso: ObservableObject {
    @Published private(set) var diasCompletados: Set<Int>
    @Published private(set) var lecturasCompletadas: Set<String>

    private let defaults: UserDefaults

    // Mismo archivo de preferencias al abrir la app y al probarla con `swift run`
    init(defaults: UserDefaults = UserDefaults(suiteName: "com.example.appbiblialeeer.progreso") ?? .standard) {
        self.defaults = defaults
        diasCompletados = Set((1...31).filter { defaults.bool(forKey: "Day_\($0)") })
        var lecturas = Set<String>()
        for lectura in planSeptiembre {
            for referencia in lectura.referencias
            where defaults.bool(forKey: Progreso.clave(lectura.dia, referencia)) {
                lecturas.insert(Progreso.clave(lectura.dia, referencia))
            }
        }
        lecturasCompletadas = lecturas
    }

    func diaCompletado(_ dia: Int) -> Bool { diasCompletados.contains(dia) }

    func lecturaCompletada(_ referencia: String, dia: Int) -> Bool {
        lecturasCompletadas.contains(Progreso.clave(dia, referencia))
    }

    /// El primer día sin completar: el que toca leer
    var diaPendiente: LecturaDelDia? {
        planSeptiembre.first { !diaCompletado($0.dia) }
    }

    /// Marca la lectura como leída y, si era la última del día, también el día
    func completarLectura(_ referencia: String, de lectura: LecturaDelDia) {
        let clave = Progreso.clave(lectura.dia, referencia)
        guard !lecturasCompletadas.contains(clave) else { return }
        lecturasCompletadas.insert(clave)
        defaults.set(true, forKey: clave)

        let todas = lectura.referencias.allSatisfy { lecturaCompletada($0, dia: lectura.dia) }
        if todas && !diaCompletado(lectura.dia) {
            diasCompletados.insert(lectura.dia)
            defaults.set(true, forKey: "Day_\(lectura.dia)")
        }
    }

    // MARK: - Por dónde ibas

    /// Índice de la primera línea visible la última vez que se leyó
    func posicion(de referencia: String) -> Int? {
        defaults.object(forKey: "scroll_\(referencia)") as? Int
    }

    func guardarPosicion(_ linea: Int?, de referencia: String) {
        defaults.set(linea, forKey: "scroll_\(referencia)")
    }

    private static func clave(_ dia: Int, _ referencia: String) -> String { "Day_\(dia)_\(referencia)" }
}
