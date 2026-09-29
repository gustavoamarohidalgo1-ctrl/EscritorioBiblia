// swift-tools-version: 5.9
import PackageDescription

let package = Package(
    name: "EscritorioBiblia",
    defaultLocalization: "es",
    platforms: [.macOS(.v14)],
    products: [
        .executable(name: "EscritorioBiblia", targets: ["EscritorioBiblia"])
    ],
    targets: [
        // Todo lo que no es interfaz: plan, lector de pasajes y los libros de la Biblia
        .target(
            name: "BibliaCore",
            resources: [
                .copy("Resources/origen"),
                .copy("Resources/Fuentes")
            ]
        ),
        // La app de SwiftUI
        .executableTarget(
            name: "EscritorioBiblia",
            dependencies: ["BibliaCore"]
        ),
        .testTarget(
            name: "BibliaCoreTests",
            dependencies: ["BibliaCore"],
            resources: [.copy("Fixtures")]
        )
    ]
)
