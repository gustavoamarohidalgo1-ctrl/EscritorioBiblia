import AppKit
import BibliaCore
import SwiftUI

@main
struct EscritorioBibliaApp: App {
    @NSApplicationDelegateAdaptor(Delegado.self) private var delegado
    @StateObject private var progreso = Progreso()

    init() {
        Lora.registrar()
        // Con `swift run` el programa no está dentro de una .app: así aparece en el Dock y con menú
        NSApplication.shared.setActivationPolicy(.regular)
    }

    var body: some Scene {
        Window("Mes de Septiembre", id: "principal") {
            VistaPrincipal()
                .environmentObject(progreso)
                .preferredColorScheme(.dark)
                .tint(.acento)
                .frame(minWidth: 720, minHeight: 520)
        }
        .defaultSize(width: 1040, height: 780)
    }
}

final class Delegado: NSObject, NSApplicationDelegate {
    func applicationDidFinishLaunching(_ notification: Notification) {
        NSApp.activate()
    }

    // Al cerrar la ventana se cierra la app (solo tiene una)
    func applicationShouldTerminateAfterLastWindowClosed(_ sender: NSApplication) -> Bool { true }
}
