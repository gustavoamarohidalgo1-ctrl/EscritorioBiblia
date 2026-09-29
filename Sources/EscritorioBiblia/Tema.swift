import BibliaCore
import CoreText
import SwiftUI

// Los mismos colores de la versión de Android (la app siempre usa modo oscuro)
extension Color {
    static let acento = Color(red: 79 / 255.0, green: 195 / 255.0, blue: 247 / 255.0) // #4FC3F7
    static let fondoTarjeta = Color(white: 30 / 255.0) // #1E1E1E
    static let fondoTarjetaResaltada = Color(white: 42 / 255.0)
    static let verdeCompletado = Color(red: 56 / 255.0, green: 142 / 255.0, blue: 60 / 255.0) // #388E3C
}

/// La fuente Lora que va dentro de la app
enum Lora {
    static func regular(_ tamano: CGFloat) -> Font { .custom("Lora-Regular", size: tamano) }
    static func negrita(_ tamano: CGFloat) -> Font { .custom("Lora-Bold", size: tamano) }

    /// Registra los .ttf para esta app (sin instalarlos en el sistema)
    static func registrar() {
        for archivo in ["lora_regular.ttf", "lora_bold.ttf"] {
            let url = Recursos.fuentes.appendingPathComponent(archivo)
            CTFontManagerRegisterFontsForURL(url as CFURL, .process, nil)
        }
    }
}
