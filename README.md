# Escritorio Biblia — Plan de lectura de septiembre

App **nativa de macOS** (Swift y SwiftUI) con el plan de lectura bíblica de septiembre:
30 días, 3 lecturas por día, texto Reina-Valera 1960.

## Requisitos

- **macOS 14 (Sonoma) o posterior**
- Las herramientas de desarrollo de Apple. Basta con las de línea de comandos (gratis):
  ```bash
  xcode-select --install
  ```
  Con **Xcode** instalado también sirve, y además permite abrir el proyecto: doble clic en `Package.swift`.

## Instalar la app

En la carpeta del proyecto:

```bash
./crear-app.sh --instalar
```

Compila la app, crea **Mes de Septiembre.app** y la copia a **Aplicaciones**. También deja el
instalador `build/Mes de Septiembre.dmg` por si quieres pasarla a otro Mac.

Como la app no está firmada con un certificado de Apple, la primera vez macOS no la deja abrir
con doble clic: haz clic derecho sobre la app → **Abrir** → **Abrir**
(o en Ajustes del Sistema → Privacidad y seguridad → **Abrir igualmente**).

## Probarla sin instalar

```bash
swift run
```

## Uso

- A la izquierda, los 30 días; al abrir la app se elige el que toca leer.
- Clic en una lectura para leerla. Se marca como completada al llegar al último versículo;
  el día, al completar sus tres lecturas.
- **Esc** o **⌘[** vuelve atrás.
- La app recuerda por dónde ibas en cada lectura.
- El progreso se guarda en las preferencias de tu usuario (`~/Library/Preferences`).

## Cambiar el texto de la Biblia

El texto fuente está en `Datos/origen/` (un `.txt` por libro). La app no lo lleva tal cual: lleva
una versión ya limpia y comprimida, un 69 % más pequeña (4,4 MB → 1,4 MB). Después de cambiar
cualquier `.txt`, vuelve a generarla:

```bash
python3 Datos/compactar.py
```

El script comprueba antes cada versículo (que la limpieza sea la de siempre y que no falte
ninguno). Si se te olvida ejecutarlo, el test `testDatosAlDia` te lo recuerda.

## Tests

```bash
swift test
```

Además de los tests del lector de pasajes, `Tests/BibliaCoreTests/Fixtures/referencias.tsv`
comprueba que se muestra exactamente el mismo texto que la versión anterior (Android/Kotlin) en
2.474 pasajes: todos los capítulos de la Biblia, las lecturas del plan y rangos al azar.

> Si `swift test` dice que no encuentra `XCTest`, instala Xcode (App Store) y ejecuta
> `sudo xcode-select -s /Applications/Xcode.app`.

## Estructura

```
Package.swift                  El proyecto (Swift Package Manager)
crear-app.sh                   Crea la .app, el .dmg y la instala
Recursos/icono.icns            Icono de la app
Datos/
├── origen/                    Los 66 libros de la Biblia (RVR 1960), texto fuente
└── compactar.py               Prepara los libros que lleva la app
Sources/
├── BibliaCore/                Todo lo que no es interfaz
│   ├── Plan.swift             Plan de lectura del mes
│   ├── Referencia.swift       "Juan 9:1-23" → libro, capítulos y versículos
│   ├── Libros.swift           Los 66 libros y sus archivos
│   ├── Biblia.swift           Lector de pasajes (con caché)
│   └── Resources/
│       ├── Libros/            Los libros ya limpios y comprimidos (generados)
│       └── Fuentes/           Fuente Lora
└── EscritorioBiblia/          La app (SwiftUI)
    ├── EscritorioBibliaApp.swift
    ├── Progreso.swift         Lecturas completadas y por dónde ibas
    ├── Tema.swift             Colores y fuente
    └── Vistas/                Lista de días, lecturas del día y texto
Tests/BibliaCoreTests/         Tests
```
