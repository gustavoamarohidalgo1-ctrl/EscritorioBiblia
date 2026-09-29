import BibliaCore
import SwiftUI

/// Las tres lecturas de un día, con su progreso. Al elegir una se abre su texto.
struct DetalleDia: View {
    let lectura: LecturaDelDia
    @EnvironmentObject private var progreso: Progreso
    @State private var ruta: [String] = []

    private var leidas: Int {
        lectura.referencias.filter { progreso.lecturaCompletada($0, dia: lectura.dia) }.count
    }

    var body: some View {
        NavigationStack(path: $ruta) {
            ScrollView {
                VStack(alignment: .leading, spacing: 14) {
                    Text("Progreso de lectura")
                        .font(Lora.negrita(20))

                    ProgressView(value: Double(leidas), total: Double(max(lectura.referencias.count, 1)))
                        .tint(leidas == lectura.referencias.count ? Color.verdeCompletado : Color.acento)
                        .padding(.bottom, 12)

                    ForEach(lectura.referencias, id: \.self) { referencia in
                        NavigationLink(value: referencia) {
                            TarjetaLectura(
                                texto: referencia,
                                completada: progreso.lecturaCompletada(referencia, dia: lectura.dia)
                            )
                        }
                        .buttonStyle(.plain)
                    }
                }
                .padding(24)
                .frame(maxWidth: 720)
                .frame(maxWidth: .infinity)
            }
            .navigationTitle("Lecturas del Día \(lectura.dia)")
            .navigationDestination(for: String.self) { referencia in
                LecturaPantalla(referencia: referencia, lectura: lectura)
            }
        }
        .task(id: lectura.dia) {
            // ✅ Precarga las lecturas del día: al abrir una, el texto ya está en memoria
            let referencias = lectura.referencias
            await Task.detached(priority: .userInitiated) { Biblia.precargar(referencias) }.value
        }
    }
}

/// Tarjeta de una lectura, con su check de completada
struct TarjetaLectura: View {
    let texto: String
    let completada: Bool
    @State private var encima = false

    var body: some View {
        HStack(spacing: 12) {
            Image(systemName: "checkmark.circle.fill")
                .font(.system(size: 22))
                .foregroundStyle(completada ? Color.acento : Color.secondary.opacity(0.35))
            Text(texto)
                .font(Lora.negrita(20))
            Spacer(minLength: 0)
        }
        .padding(.horizontal, 22)
        .padding(.vertical, 20)
        .background {
            RoundedRectangle(cornerRadius: 20, style: .continuous)
                .fill(encima ? Color.fondoTarjetaResaltada : Color.fondoTarjeta)
                .shadow(color: .black.opacity(0.35), radius: 4, y: 2)
        }
        .contentShape(RoundedRectangle(cornerRadius: 20, style: .continuous))
        .onHover { encima = $0 }
        .accessibilityLabel("\(texto), \(completada ? "completada" : "pendiente")")
    }
}
