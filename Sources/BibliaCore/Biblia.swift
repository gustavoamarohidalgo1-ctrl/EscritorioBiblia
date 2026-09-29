import Foundation

/**
 Lee los pasajes de los libros en resources/origen.

 Cada línea tiene el formato `(libro, capítulo, versículo, 'texto'),` y los libros están en orden,
 así que solo se procesan las líneas hasta el final del pasaje. Se trabaja sobre los bytes UTF-8
 del archivo y solo se convierte a texto lo que se muestra.
 */
public enum Biblia {

    // ✅ Los 60 pasajes usados más recientemente (ej. "Hechos 2:1-13")
    private static let cache = CacheDePasajes(capacidad: 60)

    /// Líneas a mostrar: "N texto" por versículo, con "Capítulo N" antes de cada capítulo cuando
    /// la referencia es un rango. Lista vacía si el pasaje no tiene versículos.
    /// Lee del disco: llamar fuera del hilo principal.
    public static func pasaje(_ referencia: String) throws -> [String] {
        if let lineas = cache[referencia] { return lineas }

        let ref = try Referencia(referencia)
        let libro = try Libros.buscar(ref.libro)
        let datos = try Data(contentsOf: Recursos.libros.appendingPathComponent(libro.archivo),
                             options: .mappedIfSafe)
        let lineas = self.lineas(de: datos, libro: libro.id, referencia: ref)

        // Solo se guardan pasajes con texto: así la pantalla muestra "Texto no disponible" cada vez
        if !lineas.isEmpty { cache[referencia] = lineas }
        return lineas
    }

    /// Pasaje ya cargado, para mostrarlo al instante
    public static func enCache(_ referencia: String) -> [String]? { cache[referencia] }

    /// Carga de antemano las lecturas de un día. Llamar fuera del hilo principal.
    public static func precargar(_ referencias: [String]) {
        for referencia in referencias where cache[referencia] == nil {
            _ = try? pasaje(referencia)
        }
    }

    // MARK: - Lectura de las líneas

    static func lineas(de datos: Data, libro: Int, referencia: Referencia) -> [String] {
        let rango = Rango(referencia)
        let conTitulos = referencia.capituloFin != nil
        var salida: [String] = []
        var capituloActual = -1

        datos.withUnsafeBytes { (crudo: UnsafeRawBufferPointer) in
            let bytes = crudo.bindMemory(to: UInt8.self)
            let total = bytes.count
            var inicio = 0
            while inicio < total {
                var fin = inicio
                while fin < total && bytes[fin] != Byte.saltoDeLinea { fin += 1 }

                if let linea = numeros(bytes, desde: inicio, hasta: fin), linea.libro == libro {
                    // Ya pasamos el pasaje: no hace falta leer más
                    if linea.capitulo > rango.ultimoCapitulo { break }
                    if rango.contiene(linea.capitulo, linea.versiculo),
                       let cierre = comillaFinal(bytes, desde: linea.inicioTexto, hasta: fin) {
                        if conTitulos && linea.capitulo != capituloActual {
                            capituloActual = linea.capitulo
                            salida.append("Capítulo \(linea.capitulo)")
                        }
                        salida.append(versiculo(linea.versiculo, bytes, desde: linea.inicioTexto, hasta: cierre))
                    }
                }
                inicio = fin + 1
            }
        }
        return salida
    }

    private struct Linea {
        let libro: Int
        let capitulo: Int
        let versiculo: Int
        let inicioTexto: Int
    }

    /// Lee "(libro, capítulo, versículo, '" sin expresiones regulares. nil si la línea no tiene ese formato.
    /// Números de 9 cifras como mucho (así nunca se desbordan).
    private static func numeros(_ b: UnsafeBufferPointer<UInt8>, desde: Int, hasta: Int) -> Linea? {
        var i = desde
        while i < hasta && esEspacioDeRecorte(b[i]) { i += 1 }
        guard i < hasta, b[i] == Byte.parentesisAbre else { return nil }
        i += 1

        var valores = (0, 0, 0)
        for k in 0..<3 {
            while i < hasta && esEspacio(b[i]) { i += 1 }
            let inicioCifras = i
            var valor = 0
            while i < hasta && b[i] >= Byte.cero && b[i] <= Byte.nueve {
                if i - inicioCifras == 9 { return nil }
                valor = valor * 10 + Int(b[i] - Byte.cero)
                i += 1
            }
            if i == inicioCifras { return nil }
            switch k {
            case 0: valores.0 = valor
            case 1: valores.1 = valor
            default: valores.2 = valor
            }
            while i < hasta && esEspacio(b[i]) { i += 1 }
            guard i < hasta, b[i] == Byte.coma else { return nil }
            i += 1
        }
        while i < hasta && esEspacio(b[i]) { i += 1 }
        guard i < hasta, b[i] == Byte.comilla else { return nil }
        return Linea(libro: valores.0, capitulo: valores.1, versiculo: valores.2, inicioTexto: i + 1)
    }

    /// Comilla que cierra el texto (se busca desde el final); la línea debe terminar en "'),". nil si no.
    private static func comillaFinal(_ b: UnsafeBufferPointer<UInt8>, desde: Int, hasta: Int) -> Int? {
        var i = hasta - 1
        while i >= desde && esEspacioDeRecorte(b[i]) { i -= 1 }
        if i >= desde && b[i] == Byte.coma { i -= 1 }
        guard i >= desde, b[i] == Byte.parentesisCierra else { return nil }
        i -= 1
        while i >= desde && esEspacio(b[i]) { i -= 1 }
        return i >= desde && b[i] == Byte.comilla ? i : nil
    }

    /// "N texto" con el texto limpio: las marcas \n, /n, \r, \t y cualquier grupo de espacios quedan
    /// en un solo espacio, sin espacios en los extremos.
    private static func versiculo(_ numero: Int, _ b: UnsafeBufferPointer<UInt8>, desde: Int, hasta: Int) -> String {
        var texto = [UInt8]()
        texto.reserveCapacity(hasta - desde)
        var espacioPendiente = false // solo se escribe si después sigue texto
        var i = desde
        while i < hasta {
            let c = b[i]
            i += 1
            if c == Byte.barra || c == Byte.barraInvertida {
                // Marcas \n, /n, \r y \t: cuentan como espacio
                let siguiente: UInt8 = i < hasta ? b[i] : 0
                if siguiente == Byte.n || (c == Byte.barraInvertida && (siguiente == Byte.r || siguiente == Byte.t)) {
                    i += 1
                    espacioPendiente = !texto.isEmpty // los espacios del inicio se descartan
                    continue
                }
            } else if esEspacio(c) {
                espacioPendiente = !texto.isEmpty
                continue
            }
            if espacioPendiente {
                texto.append(Byte.espacio)
                espacioPendiente = false
            }
            texto.append(c)
        }
        var limpio = String(decoding: texto, as: UTF8.self)
        // Espacios Unicode en los extremos (no hay en los libros): mismo resultado que trim()
        if let primero = limpio.first, let ultimo = limpio.last, primero.isWhitespace || ultimo.isWhitespace {
            limpio = limpio.trimmingCharacters(in: .whitespacesAndNewlines)
        }
        return "\(numero) \(limpio)"
    }

    // Espacios de la expresión regular \s
    private static func esEspacio(_ c: UInt8) -> Bool { c == Byte.espacio || (c >= 9 && c <= 13) }

    // Espacios que quita trim() en los extremos de la línea (los ASCII, los únicos que hay en los libros)
    private static func esEspacioDeRecorte(_ c: UInt8) -> Bool { esEspacio(c) || (c >= 28 && c <= 31) }

    /// Bytes ASCII del formato: en UTF-8 nunca aparecen dentro de una letra con tilde o una ñ
    private enum Byte {
        static let saltoDeLinea: UInt8 = 10
        static let espacio: UInt8 = 32
        static let comilla: UInt8 = 39 // '
        static let parentesisAbre: UInt8 = 40 // (
        static let parentesisCierra: UInt8 = 41 // )
        static let coma: UInt8 = 44
        static let barra: UInt8 = 47 // /
        static let cero: UInt8 = 48
        static let nueve: UInt8 = 57
        static let barraInvertida: UInt8 = 92
        static let n: UInt8 = 110
        static let r: UInt8 = 114
        static let t: UInt8 = 116
    }
}

/// El pasaje como intervalo en el orden (capítulo, versículo) del libro
struct Rango {
    let primerCapitulo: Int
    let primerVersiculo: Int
    let ultimoCapitulo: Int
    let ultimoVersiculo: Int

    init(_ ref: Referencia) {
        primerCapitulo = ref.capituloInicio
        primerVersiculo = ref.versiculoInicio ?? Int.min
        ultimoCapitulo = ref.capituloFin ?? ref.capituloInicio
        ultimoVersiculo = ref.versiculoFin ?? (ref.capituloFin == nil ? ref.versiculoInicio : nil) ?? Int.max
    }

    func esAnterior(_ capitulo: Int, _ versiculo: Int) -> Bool {
        capitulo < primerCapitulo || (capitulo == primerCapitulo && versiculo < primerVersiculo)
    }

    func contiene(_ capitulo: Int, _ versiculo: Int) -> Bool {
        !esAnterior(capitulo, versiculo) &&
            (capitulo < ultimoCapitulo || (capitulo == ultimoCapitulo && versiculo <= ultimoVersiculo))
    }
}

/// Caché LRU segura entre hilos
final class CacheDePasajes: @unchecked Sendable {
    private let capacidad: Int
    private var pasajes: [String: [String]] = [:]
    private var orden: [String] = [] // del menos al más usado
    private let cerrojo = NSLock()

    init(capacidad: Int) { self.capacidad = capacidad }

    subscript(referencia: String) -> [String]? {
        get {
            cerrojo.lock()
            defer { cerrojo.unlock() }
            guard let lineas = pasajes[referencia] else { return nil }
            usar(referencia)
            return lineas
        }
        set {
            cerrojo.lock()
            defer { cerrojo.unlock() }
            pasajes[referencia] = newValue
            if newValue == nil {
                orden.removeAll { $0 == referencia }
                return
            }
            usar(referencia)
            if orden.count > capacidad {
                pasajes[orden.removeFirst()] = nil
            }
        }
    }

    private func usar(_ referencia: String) {
        if let posicion = orden.firstIndex(of: referencia) { orden.remove(at: posicion) }
        orden.append(referencia)
    }
}
