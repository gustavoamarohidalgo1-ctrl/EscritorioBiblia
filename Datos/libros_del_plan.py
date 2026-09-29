#!/usr/bin/env python3
"""
Los libros que necesita el plan de lectura (Sources/BibliaCore/Plan.swift), uno por línea:

    python3 Datos/libros_del_plan.py    →  2_cronicas.z, juan.z, salmos.z…

crear-app.sh lo usa para meter en la app solo esos libros: la app solo abre las lecturas del plan.
Reconoce los nombres igual que Libros.swift ("2 Crónicas", "Cantar de los Cantares"…) y falla si
alguna lectura no es de un libro conocido.
"""
import re
import sys
import unicodedata

from compactar import CLAVES, RAIZ

ALIAS = {"cantar_de_los_cantares": "cantares", "cantar_de_cantares": "cantares"}
ESPACIOS = " \t\n\x0b\x0c\r"


def libro(referencia: str) -> str:
    """"2 Crónicas 20:1-21:1" → "2 Crónicas" (como Referencia.separar)"""
    s = referencia.strip(ESPACIOS)
    i = 1
    while i < len(s):
        if s[i] not in ESPACIOS:
            i += 1
            continue
        j = i
        while j < len(s) and s[j] in ESPACIOS:
            j += 1
        if j < len(s) and "0" <= s[j] <= "9":
            return s[:i]
        i = j
    sys.exit(f"Referencia inválida en el plan: {referencia}")


def clave(nombre: str) -> str:
    """"2 Crónicas" → "2_cronicas" (como Libros.normalizar)"""
    s = nombre.lower().strip()
    s = "".join(c for c in unicodedata.normalize("NFD", s) if not unicodedata.combining(c))
    s = s.replace(".", "").replace("-", "_")
    s = re.sub(r"\s+", "_", s)
    s = re.sub(r"__+", "_", s).strip("_")
    return ALIAS.get(s, s)


def main() -> int:
    plan = (RAIZ / "Sources/BibliaCore/Plan.swift").read_text(encoding="utf-8")
    referencias = re.findall(r'"([^"]+)"', plan.split("public let planSeptiembre")[1])
    if not referencias:
        sys.exit("No se encontraron lecturas en Plan.swift")
    libros = set()
    for referencia in referencias:
        k = clave(libro(referencia))
        if k not in CLAVES:
            sys.exit(f"Libro desconocido en el plan: {referencia}")
        libros.add(k)
    for k in sorted(libros):
        print(f"{k}.z")
    return 0


if __name__ == "__main__":
    sys.exit(main())
