import BibliaCore
import SwiftUI

/// Barra lateral con los 30 días y, a la derecha, las lecturas del día elegido
struct VistaPrincipal: View {
    @EnvironmentObject private var progreso: Progreso
    @State private var diaElegido: Int?

    var body: some View {
        NavigationSplitView {
            List(planSeptiembre, selection: $diaElegido) { lectura in
                FilaDia(dia: lectura.dia, completado: progreso.diaCompletado(lectura.dia))
                    .tag(lectura.dia)
            }
            .navigationTitle("Septiembre")
            .navigationSplitViewColumnWidth(min: 170, ideal: 200, max: 280)
        } detail: {
            if let dia = diaElegido, let lectura = planSeptiembre.first(where: { $0.dia == dia }) {
                DetalleDia(lectura: lectura)
                    .id(dia)
            } else {
                ContentUnavailableView(
                    tituloDelPlan,
                    systemImage: "book.closed",
                    description: Text("Elige un día para ver sus lecturas.")
                )
            }
        }
        .task {
            // Al abrir, el día que toca leer
            let pendiente = progreso.diaPendiente
            if diaElegido == nil { diaElegido = pendiente?.dia ?? 1 }
            // ✅ Sus textos se cargan ya, en segundo plano: al abrir una lectura aparece al instante
            let referencias = pendiente?.referencias ?? []
            await Task.detached(priority: .utility) { Biblia.precargar(referencias) }.value
        }
    }
}

private struct FilaDia: View {
    let dia: Int
    let completado: Bool

    var body: some View {
        Label {
            Text("Día \(dia)")
                .font(Lora.negrita(15))
        } icon: {
            Image(systemName: completado ? "checkmark.circle.fill" : "circle")
                .foregroundStyle(completado ? Color.acento : Color.secondary)
        }
        .padding(.vertical, 3)
    }
}
