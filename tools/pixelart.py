"""Tiny dependency-free pixel-art toolkit used by generate_assets.py (PNG writer + drawing primitives)."""
import math
import random
import struct
import zlib


def clamp(v, lo=0, hi=255):
    return max(lo, min(hi, int(round(v))))


def scale(c, f):
    return (clamp(c[0] * f), clamp(c[1] * f), clamp(c[2] * f), c[3] if len(c) > 3 else 255)


def mix(a, b, t):
    a = a if len(a) > 3 else (*a, 255)
    b = b if len(b) > 3 else (*b, 255)
    return tuple(clamp(a[i] + (b[i] - a[i]) * t) for i in range(4))


def rgba(c, a=255):
    return (c[0], c[1], c[2], a)


def jitter(c, rng, amount):
    d = rng.uniform(-amount, amount)
    return (clamp(c[0] + d), clamp(c[1] + d), clamp(c[2] + d), c[3] if len(c) > 3 else 255)


class Img:
    def __init__(self, w=16, h=16, fill=(0, 0, 0, 0)):
        self.w, self.h = w, h
        self.px = [[tuple(fill) for _ in range(w)] for _ in range(h)]

    def get(self, x, y):
        if 0 <= x < self.w and 0 <= y < self.h:
            return self.px[y][x]
        return (0, 0, 0, 0)

    def set(self, x, y, c):
        x, y = int(x), int(y)
        if not (0 <= x < self.w and 0 <= y < self.h):
            return
        c = c if len(c) > 3 else (*c, 255)
        if c[3] >= 255:
            self.px[y][x] = c
            return
        if c[3] <= 0:
            return
        under = self.px[y][x]
        a = c[3] / 255.0
        ua = under[3] / 255.0
        out_a = a + ua * (1 - a)
        if out_a <= 0:
            return
        rgb = [(c[i] * a + under[i] * ua * (1 - a)) / out_a for i in range(3)]
        self.px[y][x] = (clamp(rgb[0]), clamp(rgb[1]), clamp(rgb[2]), clamp(out_a * 255))

    def opaque(self, x, y):
        return self.get(x, y)[3] > 0

    def line(self, x0, y0, x1, y1, c):
        x0, y0, x1, y1 = int(round(x0)), int(round(y0)), int(round(x1)), int(round(y1))
        dx, dy = abs(x1 - x0), -abs(y1 - y0)
        sx, sy = (1 if x0 < x1 else -1), (1 if y0 < y1 else -1)
        err = dx + dy
        while True:
            self.set(x0, y0, c)
            if x0 == x1 and y0 == y1:
                break
            e2 = 2 * err
            if e2 >= dy:
                err += dy
                x0 += sx
            if e2 <= dx:
                err += dx
                y0 += sy

    def rect(self, x0, y0, x1, y1, c):
        for y in range(int(y0), int(y1) + 1):
            for x in range(int(x0), int(x1) + 1):
                self.set(x, y, c)

    def ellipse(self, cx, cy, rx, ry, c, shade=0.3):
        for y in range(int(math.floor(cy - ry - 1)), int(math.ceil(cy + ry + 1)) + 1):
            for x in range(int(math.floor(cx - rx - 1)), int(math.ceil(cx + rx + 1)) + 1):
                nx = (x + 0.5 - cx) / max(rx, 0.01)
                ny = (y + 0.5 - cy) / max(ry, 0.01)
                if nx * nx + ny * ny <= 1.0:
                    f = 1.0 + shade * (-nx - ny) / 2.0
                    self.set(x, y, scale(c, f))

    def outline(self, factor=0.45):
        """Draw a dark rim around all opaque pixels (Minecraft item style)."""
        add = []
        for y in range(self.h):
            for x in range(self.w):
                if self.px[y][x][3] > 0:
                    continue
                for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                    n = self.get(x + dx, y + dy)
                    if n[3] > 200:
                        add.append((x, y, scale(n, factor)))
                        break
        for x, y, c in add:
            self.px[y][x] = (c[0], c[1], c[2], 255)

    def noise(self, rng, amount, only_opaque=True):
        for y in range(self.h):
            for x in range(self.w):
                c = self.px[y][x]
                if only_opaque and c[3] == 0:
                    continue
                self.px[y][x] = jitter(c, rng, amount)

    def save(self, path):
        raw = bytearray()
        for row in self.px:
            raw.append(0)
            for p in row:
                raw.extend(bytes(p))

        def chunk(tag, data):
            body = tag + data
            return struct.pack(">I", len(data)) + body + struct.pack(">I", zlib.crc32(body) & 0xFFFFFFFF)

        png = b"\x89PNG\r\n\x1a\n"
        png += chunk(b"IHDR", struct.pack(">IIBBBBB", self.w, self.h, 8, 6, 0, 0, 0))
        png += chunk(b"IDAT", zlib.compress(bytes(raw), 9))
        png += chunk(b"IEND", b"")
        with open(path, "wb") as f:
            f.write(png)


def rng_for(*parts):
    return random.Random("|".join(str(p) for p in parts))
