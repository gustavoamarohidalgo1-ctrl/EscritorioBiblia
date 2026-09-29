#!/usr/bin/env python3
"""
Prepara los libros que lleva la app a partir de Datos/origen (el texto fuente).

    python3 Datos/compactar.py

Ejecutarlo después de cambiar cualquier .txt de Datos/origen. Escribe en
Sources/BibliaCore/Resources/Libros:

- <libro>.z: el libro con el texto ya limpio, una línea por versículo
  ("capítulo<TAB>versículo<TAB>texto"), comprimido con DEFLATE (el formato .zlib de Apple).
  Ocupa un 69 % menos que el original y la app ya no tiene que limpiar nada al leerlo.
- origen.sha256: la huella de cada .txt de origen; el test testDatosAlDia avisa si algún
  libro cambió y no se volvió a ejecutar este script.

Antes de escribir comprueba cada versículo: la limpieza da lo mismo que la de la versión original
y cada libro está completo y en orden.
"""
import hashlib
import re
import sys
import zlib
from pathlib import Path

RAIZ = Path(__file__).resolve().parent.parent
ORIGEN = RAIZ / "Datos" / "origen"
DESTINO = RAIZ / "Sources" / "BibliaCore" / "Resources" / "Libros"

# Orden canónico: el id de cada libro es su posición (igual que Libros.swift)
CLAVES = re.findall(r'"([^"]+)"', (RAIZ / "Sources/BibliaCore/Libros.swift").read_text(encoding="utf-8")
                    .split("static let claves = [")[1].split("]")[0])

LINEA = re.compile(rb"^\((\d+), (\d+), (\d+), '(.*)'\),?$")


def limpiar(texto: bytes) -> str:
    """Las marcas \\n, /n, \\r, \\t y cualquier grupo de espacios quedan en un solo espacio,
    sin espacios en los extremos (lo que antes hacía la app al mostrar cada versículo)."""
    salida = bytearray()
    espacio_pendiente = False
    i = 0
    while i < len(texto):
        c = texto[i]
        i += 1
        if c in (0x2F, 0x5C):  # / y \
            siguiente = texto[i] if i < len(texto) else 0
            if siguiente == 0x6E or (c == 0x5C and siguiente in (0x72, 0x74)):  # n, r, t
                i += 1
                espacio_pendiente = len(salida) > 0
                continue
        elif c == 0x20 or 9 <= c <= 13:
            espacio_pendiente = len(salida) > 0
            continue
        if espacio_pendiente:
            salida.append(0x20)
            espacio_pendiente = False
        salida.append(c)
    return salida.decode("utf-8").strip()


def limpieza_original(texto: str) -> str:
    """El cleanText de la primera versión (Android), para comprobar que el resultado es el mismo"""
    for marca, cambio in (("\\n", "\n"), ("/n", "\n"), ("\\r", " "), ("\r", " "), ("\\t", " "), ("\t", " ")):
        texto = texto.replace(marca, cambio)
    return re.sub(r"[ \t\n\x0b\f\r]+", " ", texto).strip()


def main() -> int:
    if len(CLAVES) != 66 or len(list(ORIGEN.glob("*.txt"))) != 66:
        sys.exit("Se esperaban 66 libros")
    DESTINO.mkdir(parents=True, exist_ok=True)
    huellas = []
    antes = despues = 0
    for id_libro, clave in enumerate(CLAVES, start=1):
        origen = (ORIGEN / f"{clave}.txt").read_bytes()
        lineas = []
        capitulo = versiculo = 0
        for numero, linea in enumerate(origen.split(b"\n"), start=1):
            if not linea.strip():
                continue
            donde = f"{clave}.txt:{numero}"
            partes = LINEA.match(linea)
            if not partes:
                sys.exit(f"{donde}: formato inválido")
            libro, cap, ver = (int(partes.group(k)) for k in (1, 2, 3))
            if libro != id_libro:
                sys.exit(f"{donde}: es del libro {libro}, se esperaba el {id_libro}")
            if cap != capitulo:
                if cap != capitulo + 1:
                    sys.exit(f"{donde}: falta el capítulo {capitulo + 1}")
                capitulo, versiculo = cap, 0
            if ver != versiculo + 1:
                sys.exit(f"{donde}: falta el versículo {capitulo}:{versiculo + 1}")
            versiculo = ver
            texto = limpiar(partes.group(4))
            if texto != limpieza_original(partes.group(4).decode("utf-8")):
                sys.exit(f"{donde}: la limpieza no coincide con la original")
            if "\t" in texto or "\n" in texto:
                sys.exit(f"{donde}: el texto limpio no puede tener tabuladores ni saltos de línea")
            lineas.append(f"{cap}\t{ver}\t{texto}\n")

        compacto = "".join(lineas).encode("utf-8")
        compresor = zlib.compressobj(9, zlib.DEFLATED, -15)  # DEFLATE sin cabecera, como .zlib de Apple
        comprimido = compresor.compress(compacto) + compresor.flush()
        (DESTINO / f"{clave}.z").write_bytes(comprimido)
        huellas.append(f"{hashlib.sha256(origen).hexdigest()}  {clave}.txt\n")
        antes += len(origen)
        despues += len(comprimido)

    (DESTINO / "origen.sha256").write_text("".join(huellas), encoding="utf-8")
    print(f"66 libros: {antes / 1e6:.2f} MB → {despues / 1e6:.2f} MB ({100 - 100 * despues / antes:.0f} % menos)")
    return 0


if __name__ == "__main__":
    sys.exit(main())
