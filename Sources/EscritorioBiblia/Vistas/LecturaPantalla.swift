import BibliaCore
import SwiftUI

/// El texto de una lectura, con el mismo estilo que en el teléfono: letra grande y mucho espacio
struct LecturaPantalla: View {
    let referencia: String
    let lectura: LecturaDelDia

    @EnvironmentObject private var progreso: Progreso
    @Environment(\.dismiss) private var volver

    @State private var lineas: [String]
    @State private var mensaje: String?
    // Primera línea visible: sirve para volver al mismo sitio la próxima vez
    @State private var lineaVisible: Int?

    init(referencia: String, lectura: LecturaDelDia) {
        self.referencia = referencia
        self.lectura = lectura
        // ✅ Si el pasaje ya está en memoria (se precarga al abrir el día) se muestra al instante
        _lineas = State(initialValue: Biblia.enCache(referencia) ?? [])
    }

    var body: some View {
        ZStack {
            if let mensaje {
                Text(mensaje)
                    .font(Lora.regular(20))
                    .foregroundStyle(.secondary)
                    .padding(40)
            } else if lineas.isEmpty {
                ProgressView()
            } else {
                texto
            }
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .navigationTitle(referencia)
        .background(atajosParaVolver)
        .task(id: referencia) { await cargar() }
    }

    private var texto: some View {
        ScrollView {
            LazyVStack(alignment: .leading, spacing: 0) {
                ForEach(lineas.indices, id: \.self) { indice in
                    Text(lineas[indice])
                        .font(Lora.regular(30))
                        // Lora a 30 pt mide unos 38 pt de alto: así cada línea ocupa ~100 pt, como en Android
                        .lineSpacing(62)
                        .textSelection(.enabled)
                        .frame(maxWidth: .infinity, alignment: .leading)
                        .padding(.bottom, indice == lineas.count - 1 ? 0 : 75)
                        .onAppear {
                            // Al llegar al último versículo la lectura queda completada
                            if indice == lineas.count - 1 {
                                progreso.completarLectura(referencia, de: lectura)
                            }
                        }
                }
            }
            .scrollTargetLayout()
            .padding(.horizontal, 56)
            .padding(.vertical, 20)
            // Ancho cómodo para leer: en una ventana ancha las líneas no se hacen interminables
            .frame(maxWidth: 900)
            .frame(maxWidth: .infinity)
        }
        .scrollPosition(id: $lineaVisible, anchor: .top)
        .onChange(of: lineaVisible) { _, nueva in
            progreso.guardarPosicion(nueva, de: referencia)
        }
    }

    private func cargar() async {
        if lineas.isEmpty {
            let ref = referencia
            let resultado = await Task.detached(priority: .userInitiated) {
                Result { try Biblia.pasaje(ref) }
            }.value
            switch resultado {
            case .success(let cargadas) where !cargadas.isEmpty:
                lineas = cargadas
            case .success:
                mensaje = "Texto no disponible para: \(referencia)"
                return
            case .failure(let error):
                mensaje = error.localizedDescription
                return
            }
        }
        // Vuelve a donde lo dejaste
        if let guardada = progreso.posicion(de: referencia), guardada < lineas.count {
            lineaVisible = guardada
        }
    }

    // El "atrás" del teléfono: Esc o ⌘[ (como en Safari o el Finder)
    private var atajosParaVolver: some View {
        Group {
            Button("Volver") { volver() }
                .keyboardShortcut(.cancelAction)
            Button("Volver") { volver() }
                .keyboardShortcut("[", modifiers: .command)
        }
        .frame(width: 0, height: 0)
        .opacity(0)
        .accessibilityHidden(true)
    }
}
