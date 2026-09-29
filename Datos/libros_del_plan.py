#!/usr/bin/env python3
"""
Los libros que lleva la app, con solo los versículos que usa el plan de lectura
(Sources/BibliaCore/Plan.swift):

    python3 Datos/libros_del_plan.py <carpeta>

Escribe en <carpeta> un <libro>.z por cada libro del plan, en el mismo formato que
Resources/Libros, pero solo con los versículos de sus lecturas: la app solo abre esas lecturas.
Por ejemplo, de Salmos solo quedan los salmos 73 a 95. Lo usa crear-app.sh.

Antes de terminar comprueba que cada lectura del plan da exactamente las mismas líneas con los
libros recortados que con los completos. Reconoce las referencias igual que Referencia.swift y
Libros.swift, y falla si alguna no es válida.
"""
import re
import sys
import unicodedata
import zlib
from pathlib import Path

from compactar import CLAVES, DESTINO as LIBROS_COMPLETOS, RAIZ

ALIAS = {"cantar_de_los_cantares": "cantares", "cantar_de_cantares": "cantares"}
ESPACIOS = " \t\n\x0b\x0c\r"


class ReferenciaInvalida(Exception):
    pass


# --- Igual que Referencia.swift ---

def separar(s: str):
    """"2 Crónicas 20:1-21:1" → ("2 Crónicas", "20:1-21:1")"""
    i = 1
    while i < len(s):
        if s[i] not in ESPACIOS:
            i += 1
            continue
        j = i
        while j < len(s) and s[j] in ESPACIOS:
            j += 1
        if j < len(s) and "0" <= s[j] <= "9":
            return s[:i], s[j:]
        i = j
    return None


def entero(texto: str) -> int:
    if not re.fullmatch(r"[+-]?[0-9]+", texto):
        raise ReferenciaInvalida(texto)
    return int(texto)


def referencia(texto: str):
    """→ (libro, capítulo inicial, versículo inicial, capítulo final, versículo final)"""
    separado = separar(texto.strip(ESPACIOS))
    if separado is None:
        raise ReferenciaInvalida(texto)
    libro, resto = separado
    if ":" in resto:
        partes = resto.split("-", 1)
        inicio = [entero(n) for n in partes[0].split(":")]
        if len(inicio) < 2:
            raise ReferenciaInvalida(texto)
        if len(partes) == 1:
            return libro, inicio[0], inicio[1], None, None
        fin = [entero(n) for n in partes[1].split(":")]
        if ":" in partes[1]:
            if len(fin) < 2:
                raise ReferenciaInvalida(texto)
            return libro, inicio[0], inicio[1], fin[0], fin[1]
        return libro, inicio[0], inicio[1], inicio[0], fin[0]
    if "-" in resto:
        capitulos = [entero(n) for n in resto.split("-")]
        if len(capitulos) < 2:
            raise ReferenciaInvalida(texto)
        return libro, capitulos[0], None, capitulos[1], None
    return libro, entero(resto), None, None, None


def contiene(ref, capitulo: int, versiculo: int) -> bool:
    """Igual que Rango.contiene de Biblia.swift"""
    _, primer_cap, primer_ver, cap_fin, ver_fin = ref
    primer_ver = primer_ver if primer_ver is not None else -2**63
    ultimo_cap = cap_fin if cap_fin is not None else primer_cap
    ultimo_ver = ver_fin if ver_fin is not None else (ref[2] if cap_fin is None else None)
    ultimo_ver = ultimo_ver if ultimo_ver is not None else 2**63
    anterior = capitulo < primer_cap or (capitulo == primer_cap and versiculo < primer_ver)
    return not anterior and (capitulo < ultimo_cap or (capitulo == ultimo_cap and versiculo <= ultimo_ver))


# --- Igual que Libros.swift ---

def clave(nombre: str) -> str:
    """"2 Crónicas" → "2_cronicas\""""
    s = nombre.lower().strip()
    s = "".join(c for c in unicodedata.normalize("NFD", s) if not unicodedata.combining(c))
    s = s.replace(".", "").replace("-", "_")
    s = re.sub(r"\s+", "_", s)
    s = re.sub(r"__+", "_", s).strip("_")
    return ALIAS.get(s, s)


# --- Igual que Biblia.lineas ---

def lineas(libro: bytes, ref) -> list:
    salida, capitulo_actual = [], -1
    ultimo_cap = ref[3] if ref[3] is not None else ref[1]
    for linea in libro.decode("utf-8").split("\n"):
        if not linea:
            continue
        capitulo, versiculo, texto = linea.split("\t", 2)
        capitulo, versiculo = int(capitulo), int(versiculo)
        if capitulo > ultimo_cap:
            break
        if contiene(ref, capitulo, versiculo):
            if ref[3] is not None and capitulo != capitulo_actual:
                capitulo_actual = capitulo
                salida.append(f"Capítulo {capitulo}")
            salida.append(f"{versiculo} {texto}")
    return salida


def comprimir(datos: bytes) -> bytes:
    compresor = zlib.compressobj(9, zlib.DEFLATED, -15)  # DEFLATE sin cabecera, como .zlib de Apple
    return compresor.compress(datos) + compresor.flush()


def main() -> int:
    if len(sys.argv) != 2:
        sys.exit("Uso: python3 Datos/libros_del_plan.py <carpeta>")
    destino = Path(sys.argv[1])
    destino.mkdir(parents=True, exist_ok=True)

    plan = (RAIZ / "Sources/BibliaCore/Plan.swift").read_text(encoding="utf-8")
    textos = re.findall(r'"([^"]+)"', plan.split("public let planSeptiembre")[1])
    if not textos:
        sys.exit("No se encontraron lecturas en Plan.swift")
    lecturas = {}  # libro -> [(texto, referencia)]
    for texto in textos:
        try:
            ref = referencia(texto)
        except ReferenciaInvalida:
            sys.exit(f"Referencia inválida en el plan: {texto}")
        k = clave(ref[0])
        if k not in CLAVES:
            sys.exit(f"Libro desconocido en el plan: {texto}")
        lecturas.setdefault(k, []).append((texto, ref))

    antes = despues = 0
    for k, refs in sorted(lecturas.items()):
        completo = zlib.decompress((LIBROS_COMPLETOS / f"{k}.z").read_bytes(), -15)
        usadas = [linea for linea in completo.decode("utf-8").split("\n") if linea and any(
            contiene(ref, int(linea.split("\t")[0]), int(linea.split("\t")[1])) for _, ref in refs)]
        recortado = "".join(f"{linea}\n" for linea in usadas).encode("utf-8")
        comprimido = comprimir(recortado)
        (destino / f"{k}.z").write_bytes(comprimido)

        # Cada lectura debe dar lo mismo con el libro recortado que con el completo
        releido = zlib.decompress((destino / f"{k}.z").read_bytes(), -15)
        for texto, ref in refs:
            esperado = lineas(completo, ref)
            if not esperado or lineas(releido, ref) != esperado:
                sys.exit(f"{texto}: el libro recortado no da el mismo texto")
        antes += len((LIBROS_COMPLETOS / f"{k}.z").read_bytes())
        despues += len(comprimido)

    print(f"{len(lecturas)} libros con los versículos de {len(textos)} lecturas: "
          f"{antes / 1024:.0f} KB → {despues / 1024:.0f} KB")
    return 0


if __name__ == "__main__":
    sys.exit(main())
