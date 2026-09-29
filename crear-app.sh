#!/bin/bash
# Crea "Mes de Septiembre.app" y su instalador .dmg en la carpeta build/.
#   ./crear-app.sh                    → crea la app y el .dmg
#   ./crear-app.sh --instalar         → además la copia a /Applications (Aplicaciones)
#   ./crear-app.sh --biblia-completa  → mete los 66 libros, no solo los que usa el plan
set -euo pipefail
cd "$(dirname "$0")"

INSTALAR=0
BIBLIA_COMPLETA=0
for opcion in "$@"; do
    case "$opcion" in
        --instalar) INSTALAR=1 ;;
        --biblia-completa) BIBLIA_COMPLETA=1 ;;
        *) echo "Opción desconocida: $opcion" >&2; exit 1 ;;
    esac
done

NOMBRE="Mes de Septiembre"
EJECUTABLE="EscritorioBiblia"
IDENTIFICADOR="com.example.appbiblialeeer.septiembre"
VERSION="1.0.0"

echo "▸ Compilando (versión optimizada)…"
# ✅ -Osize: optimiza por tamaño; en una app de lectura la velocidad no cambia de forma apreciable
swift build -c release -Xswiftc -Osize --product "$EJECUTABLE"
BINARIOS="$(swift build -c release -Xswiftc -Osize --show-bin-path)"

APP="build/$NOMBRE.app"
echo "▸ Armando $APP…"
rm -rf "$APP"
mkdir -p "$APP/Contents/MacOS" "$APP/Contents/Resources"
cp "$BINARIOS/$EJECUTABLE" "$APP/Contents/MacOS/"
# ✅ Sin los símbolos de depuración: el programa ocupa bastante menos y funciona igual
strip -S -x "$APP/Contents/MacOS/$EJECUTABLE"
# Los libros y la fuente Lora (la app los busca en Contents/Resources)
mkdir -p "$APP/Contents/Resources/Libros"
if [[ $BIBLIA_COMPLETA == 1 ]]; then
    cp Sources/BibliaCore/Resources/Libros/*.z "$APP/Contents/Resources/Libros/"
else
    # ✅ Solo los libros que usa el plan (9 de 66): la app solo abre esas lecturas
    LIBROS="$(python3 Datos/libros_del_plan.py)"
    for libro in $LIBROS; do
        cp "Sources/BibliaCore/Resources/Libros/$libro" "$APP/Contents/Resources/Libros/"
    done
fi
cp -R Sources/BibliaCore/Resources/Fuentes "$APP/Contents/Resources/"
cp Recursos/icono.icns "$APP/Contents/Resources/"

cat > "$APP/Contents/Info.plist" <<PLIST
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
<plist version="1.0">
<dict>
    <key>CFBundleName</key>
    <string>$NOMBRE</string>
    <key>CFBundleDisplayName</key>
    <string>$NOMBRE</string>
    <key>CFBundleIdentifier</key>
    <string>$IDENTIFICADOR</string>
    <key>CFBundleExecutable</key>
    <string>$EJECUTABLE</string>
    <key>CFBundlePackageType</key>
    <string>APPL</string>
    <key>CFBundleShortVersionString</key>
    <string>$VERSION</string>
    <key>CFBundleVersion</key>
    <string>1</string>
    <key>CFBundleIconFile</key>
    <string>icono</string>
    <key>CFBundleDevelopmentRegion</key>
    <string>es</string>
    <key>LSMinimumSystemVersion</key>
    <string>14.0</string>
    <key>LSApplicationCategoryType</key>
    <string>public.app-category.reference</string>
    <key>NSHighResolutionCapable</key>
    <true/>
    <key>NSPrincipalClass</key>
    <string>NSApplication</string>
</dict>
</plist>
PLIST

# Firma local (sin cuenta de Apple): necesaria para que la app abra en los Mac con chip M
codesign --force --sign - "$APP"

echo "▸ Creando el instalador .dmg…"
TEMPORAL="$(mktemp -d)"
cp -R "$APP" "$TEMPORAL/"
ln -s /Applications "$TEMPORAL/Aplicaciones"
rm -f "build/$NOMBRE.dmg"
# ✅ ULMO (LZMA) comprime más que el UDZO por defecto; se abre en macOS 10.15 o posterior
hdiutil create -quiet -volname "$NOMBRE" -srcfolder "$TEMPORAL" -ov -format ULMO "build/$NOMBRE.dmg"
rm -rf "$TEMPORAL"

if [[ $INSTALAR == 1 ]]; then
    echo "▸ Instalando en /Applications…"
    rm -rf "/Applications/$NOMBRE.app"
    cp -R "$APP" /Applications/
    echo "✓ Instalada. Ábrela desde Aplicaciones o Launchpad."
fi

echo "✓ Listo:"
du -sh "$APP" "build/$NOMBRE.dmg"
