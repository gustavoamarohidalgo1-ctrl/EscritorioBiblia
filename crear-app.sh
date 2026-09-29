#!/bin/bash
# Crea "Mes de Septiembre.app" y su instalador .dmg en la carpeta build/.
#   ./crear-app.sh             → crea la app y el .dmg
#   ./crear-app.sh --instalar  → además la copia a /Applications (Aplicaciones)
set -euo pipefail
cd "$(dirname "$0")"

NOMBRE="Mes de Septiembre"
EJECUTABLE="EscritorioBiblia"
IDENTIFICADOR="com.example.appbiblialeeer.septiembre"
VERSION="1.0.0"

echo "▸ Compilando (versión optimizada)…"
swift build -c release --product "$EJECUTABLE"
BINARIOS="$(swift build -c release --show-bin-path)"

APP="build/$NOMBRE.app"
echo "▸ Armando $APP…"
rm -rf "$APP"
mkdir -p "$APP/Contents/MacOS" "$APP/Contents/Resources"
cp "$BINARIOS/$EJECUTABLE" "$APP/Contents/MacOS/"
# Los libros y la fuente Lora (la app los busca en Contents/Resources)
cp -R Sources/BibliaCore/Resources/origen Sources/BibliaCore/Resources/Fuentes "$APP/Contents/Resources/"
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
hdiutil create -quiet -volname "$NOMBRE" -srcfolder "$TEMPORAL" -ov -format UDZO "build/$NOMBRE.dmg"
rm -rf "$TEMPORAL"

if [[ "${1:-}" == "--instalar" ]]; then
    echo "▸ Instalando en /Applications…"
    rm -rf "/Applications/$NOMBRE.app"
    cp -R "$APP" /Applications/
    echo "✓ Instalada. Ábrela desde Aplicaciones o Launchpad."
fi

echo "✓ Listo:"
du -sh "$APP" "build/$NOMBRE.dmg"
