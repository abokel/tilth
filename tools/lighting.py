"""
Post-processing that gives the flat procedural textures depth and life: light from the top left, shadow to
the bottom right, a relief emboss for surfaces, and a little more colour saturation.
"""
from pixelart import Img, clamp


def _lum(c):
    return 0.3 * c[0] + 0.59 * c[1] + 0.11 * c[2]


def _shade(c, f, sat=1.0):
    lum = _lum(c)
    rgb = [lum + (c[i] - lum) * sat for i in range(3)]
    return (clamp(rgb[0] * f), clamp(rgb[1] * f), clamp(rgb[2] * f), c[3])


def _copy(img):
    out = Img(img.w, img.h)
    out.px = [row[:] for row in img.px]
    return out


def surface(img, strength=0.95, sat=1.18):
    """Tileable relief for opaque block faces: brightness read as height, lit from the top left."""
    out = _copy(img)
    w, h = img.w, img.h
    for y in range(h):
        for x in range(w):
            c = img.px[y][x]
            d = _lum(c) - _lum(img.px[(y - 1) % h][(x - 1) % w])
            f = max(0.7, min(1.4, 1.0 + strength * d / 64.0))
            out.px[y][x] = _shade(c, f, sat)
    return out


def sprite(img, sat=1.22):
    """Item icons: a lit upper-left rim, a shaded lower-right rim and a soft top-to-bottom gradient."""
    out = _copy(img)
    w, h = img.w, img.h

    def opaque(x, y):
        return 0 <= x < w and 0 <= y < h and img.px[y][x][3] > 0

    edge = [[opaque(x, y) and not (opaque(x + 1, y) and opaque(x - 1, y) and opaque(x, y + 1) and opaque(x, y - 1))
             for x in range(w)] for y in range(h)]

    def is_edge(x, y):
        return 0 <= x < w and 0 <= y < h and edge[y][x]

    ys = [y for y in range(h) for x in range(w) if opaque(x, y)]
    top, bottom = (min(ys), max(ys)) if ys else (0, h - 1)
    for y in range(h):
        for x in range(w):
            c = img.px[y][x]
            if c[3] == 0 or edge[y][x]:
                continue
            f = 1.12 - 0.26 * (y - top) / max(1, bottom - top)
            lit = is_edge(x - 1, y) or is_edge(x, y - 1)
            if lit and (is_edge(x - 1, y - 1) or is_edge(x - 1, y) and is_edge(x, y - 1)):
                f *= 1.4
            elif lit:
                f *= 1.24
            elif is_edge(x + 1, y) or is_edge(x, y + 1):
                f *= 0.74
            d = _lum(c) - _lum(img.px[max(0, y - 1)][max(0, x - 1)])
            f *= max(0.82, min(1.2, 1.0 + 0.45 * d / 64.0))
            out.px[y][x] = _shade(c, f, sat)
    return out


def plant(img, sat=1.2):
    """Crops: sunlit tops and shaded lower leaves, with brighter upper leaf edges and darker undersides."""
    out = _copy(img)
    w, h = img.w, img.h
    for y in range(h):
        for x in range(w):
            c = img.px[y][x]
            if c[3] == 0:
                continue
            f = 1.18 - 0.4 * y / max(1, h - 1)
            above = img.px[y - 1][x] if y > 0 else (0, 0, 0, 0)
            below = img.px[y + 1][x] if y < h - 1 else (0, 0, 0, 0)
            if above[3] == 0:
                f *= 1.18
            elif below[3] == 0:
                f *= 0.78
            out.px[y][x] = _shade(c, f, sat)
    return out


def fully_opaque(img):
    return all(p[3] == 255 for row in img.px for p in row)
