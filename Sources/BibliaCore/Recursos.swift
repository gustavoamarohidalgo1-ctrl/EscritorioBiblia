import Foundation

/// Dónde están los libros y las fuentes: dentro de la app (Contents/Resources, los copia
/// crear-app.sh) o, con `swift run` y `swift test`, en el paquete de recursos que crea SwiftPM.
public enum Recursos {

    public static func carpeta(_ nombre: String) -> URL {
        if let url = Bundle.main.url(forResource: nombre, withExtension: nil) {
            return url
        }
        guard let url = Bundle.module.url(forResource: nombre, withExtension: nil) else {
            fatalError("Falta la carpeta de recursos \(nombre)")
        }
        return url
    }

    /// Los 66 libros, un .txt por libro
    public static let libros = carpeta("origen")

    /// La fuente Lora (regular y negrita)
    public static let fuentes = carpeta("Fuentes")
}
