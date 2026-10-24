"""Visible symptoms applied to crop textures: chlorosis, phosphorus purpling, drought wilting, frost damage."""
from pixelart import Img, mix, rng_for, scale

CONDITIONS = ["healthy", "chlorotic", "purpled", "wilted", "frosted"]


def _is_foliage(c):
    return c[3] > 0 and c[1] > c[0] + 6 and c[1] >= c[2]


def _edge(img, x, y):
    for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
        if img.get(x + dx, y + dy)[3] == 0:
            return True
    return False


def _sag(img):
    """Shifts foliage in the upper two thirds down one pixel."""
    out = Img(img.w, img.h)
    out.px = [row[:] for row in img.px]
    top = int(img.h * 0.66)
    for y in range(top, 0, -1):
        for x in range(img.w):
            above = img.px[y - 1][x]
            if _is_foliage(above):
                out.px[y][x] = above
                if not _is_foliage(img.px[y - 2][x] if y >= 2 else (0, 0, 0, 0)):
                    out.px[y - 1][x] = (0, 0, 0, 0)
    return out


def apply(img, condition, key):
    if condition == "healthy":
        return img
    rng = rng_for("condition", condition, key)
    out = Img(img.w, img.h)
    out.px = [row[:] for row in img.px]
    if condition == "wilted":
        img = _sag(_sag(img))
        out.px = [row[:] for row in img.px]
    for y in range(out.h):
        for x in range(out.w):
            c = out.px[y][x]
            if not _is_foliage(c):
                continue
            if condition == "chlorotic":
                # Older, lower leaves yellow first.
                t = 0.35 + 0.35 * (y / out.h) + rng.uniform(-0.05, 0.05)
                out.px[y][x] = scale(mix(c, (214, 206, 92, 255), t), 1.08)
            elif condition == "purpled":
                if y > out.h * 0.35 or _edge(img, x, y) or rng.random() < 0.3:
                    out.px[y][x] = scale(mix(c, (150, 36, 140, 255), 0.62 + rng.uniform(0, 0.12)), 1.05)
            elif condition == "wilted":
                tip = _edge(out, x, y) and y < out.h * 0.6
                out.px[y][x] = mix(c, (176, 160, 96, 255), 0.7) if tip else scale(mix(c, (150, 140, 84, 255), 0.5), 0.86)
            elif condition == "frosted":
                if _edge(out, x, y) or rng.random() < 0.18:
                    out.px[y][x] = mix(c, (74, 48, 32, 255), 0.75)
                else:
                    out.px[y][x] = scale(mix(c, (110, 132, 140, 255), 0.35), 0.82)
    return out


def cracked(top):
    """Dry farmland: paler, with a network of shrinkage cracks."""
    out = Img(16, 16)
    rng = rng_for("cracks")
    for y in range(16):
        for x in range(16):
            out.px[y][x] = scale(mix(top.px[y][x], (196, 178, 140, 255), 0.22), 1.08)
    for _ in range(4):
        x, y = rng.randrange(16), rng.randrange(16)
        for _ in range(9):
            out.set(x, y, scale(top.px[y % 16][x % 16], 0.42))
            if rng.random() < 0.5:
                x = (x + rng.choice((-1, 1))) % 16
            else:
                y = (y + rng.choice((-1, 1))) % 16
    return out


def soggy(top):
    """Saturated farmland: dark and glossy, water standing in the furrows."""
    out = Img(16, 16)
    for y in range(16):
        for x in range(16):
            c = scale(mix(top.px[y][x], (52, 58, 70, 255), 0.18), 0.8)
            if y % 4 == 2:
                c = mix(c, (110, 140, 190, 255), 0.55)
                if (x + y) % 5 == 0:
                    c = (196, 214, 238, 255)
            out.px[y][x] = c
    return out
