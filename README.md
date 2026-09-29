# Escritorio Biblia — Plan de lectura de septiembre

App de escritorio para macOS con el plan de lectura bíblica
de septiembre: 30 días, 3 lecturas por día, texto Reina-Valera 1960.

Hecha con Kotlin y [Compose Multiplatform](https://www.jetbrains.com/compose-multiplatform/)
(Compose for Desktop), la misma tecnología de la versión anterior para Android.

## Requisitos

- **Java 17 o superior** (JDK). En un Mac, por ejemplo: `brew install --cask temurin`,
  o descárgalo de <https://adoptium.net>.
  Si abres el proyecto con IntelliJ IDEA, ya trae uno.

## Abrir la app sin instalarla

```bash
./gradlew run
```

## Crear la app para el Mac (.dmg)

```bash
./gradlew packageDmg
```

El instalador queda en `app/build/compose/binaries/main/dmg/`. Ábrelo y arrastra
**Mes de Septiembre** a Aplicaciones.

Como la app no está firmada con un certificado de Apple, la primera vez macOS no la deja abrir
con doble clic: haz clic derecho sobre la app → **Abrir** → **Abrir**
(o en Ajustes del Sistema → Privacidad y seguridad → **Abrir igualmente**).

## Uso

- Clic en un día para ver sus lecturas, y en una lectura para leerla.
- Una lectura se marca como completada al llegar al último versículo; el día, al completar sus tres lecturas.
- **Esc** o **⌘[** vuelve atrás (como el botón "atrás" del teléfono).
- La app recuerda por dónde ibas en cada lectura.
- El progreso se guarda en las preferencias de macOS de tu usuario (`~/Library/Preferences`).

## Tests

```bash
./gradlew test
```

## Estructura

```
app/
├── build.gradle.kts            Compose Desktop y empaquetado (.dmg)
├── icono.icns                  Icono de la app en macOS
└── src/
    ├── main/kotlin/com/example/appbiblialeeer/
    │   ├── Main.kt             Ventana de la app (antes MainActivity)
    │   ├── data/               Plan de lectura del mes
    │   ├── storage/            Lectura de los libros y progreso guardado
    │   └── ui/                 Pantallas, tarjetas, tema y navegación "atrás"
    ├── main/resources/
    │   ├── origen/             Los 66 libros de la Biblia (RVR 1960)
    │   ├── font/               Fuente Lora
    │   └── icono.png           Icono de la ventana y del Dock
    └── test/kotlin/            Tests del lector de pasajes y de las fuentes
```
