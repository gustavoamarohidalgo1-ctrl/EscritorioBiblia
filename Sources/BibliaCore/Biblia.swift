import Foundation

/**
 Lee los pasajes de los libros de Resources/Libros.

 Cada libro está ya limpio y comprimido (lo prepara Datos/compactar.py): una línea por versículo,
 "capítulo<TAB>versículo<TAB>texto", en orden. Solo se recorren las líneas hasta el final del pasaje
 y solo se convierte a texto lo que se muestra.
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
        let datos = try texto(de: libro)
        let lineas = self.lineas(de: datos, referencia: ref)

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

    /// El libro entero ya descomprimido (DEFLATE, el formato .zlib de Apple)
    static func texto(de libro: Libro) throws -> Data {
        let comprimido = try Data(contentsOf: Recursos.libros.appendingPathComponent(libro.archivo))
        return try (comprimido as NSData).decompressed(using: .zlib) as Data
    }

    // MARK: - Lectura de las líneas

    static func lineas(de libro: Data, referencia: Referencia) -> [String] {
        let rango = Rango(referencia)
        let conTitulos = referencia.capituloFin != nil
        var salida: [String] = []
        var capituloActual = -1

        libro.withUnsafeBytes { (crudo: UnsafeRawBufferPointer) in
            let bytes = crudo.bindMemory(to: UInt8.self)
            let total = bytes.count
            var i = 0
            while i < total {
                let capitulo = numero(bytes, &i)
                let versiculo = numero(bytes, &i)
                // Línea incompleta al final (no pasa con los libros generados): se ignora en vez de fallar
                if i > total { break }
                let inicioTexto = i
                while i < total && bytes[i] != saltoDeLinea { i += 1 }
                let finTexto = i
                i += 1

                // Ya pasamos el pasaje: no hace falta leer más
                if capitulo > rango.ultimoCapitulo { break }
                if rango.contiene(capitulo, versiculo) {
                    if conTitulos && capitulo != capituloActual {
                        capituloActual = capitulo
                        salida.append("Capítulo \(capitulo)")
                    }
                    salida.append("\(versiculo) " + String(decoding: bytes[inicioTexto..<finTexto], as: UTF8.self))
                }
            }
        }
        return salida
    }

    /// Lee las cifras desde [i] hasta el tabulador y deja [i] justo después de él
    private static func numero(_ bytes: UnsafeBufferPointer<UInt8>, _ i: inout Int) -> Int {
        var valor = 0
        while i < bytes.count && bytes[i] != tabulador {
            valor = valor * 10 + Int(bytes[i]) - 48
            i += 1
        }
        i += 1
        return valor
    }

    private static let tabulador: UInt8 = 9
    private static let saltoDeLinea: UInt8 = 10
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
