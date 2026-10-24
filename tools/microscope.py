"""
Microscope views (128x128) of every affliction, drawn procedurally: host leaf cells as a jittered-grid
Voronoi, then the organism by kind (fungal hyphae and spores, oomycete sporangia, bacterial rods, viral
mosaic and inclusions, insects, nematodes, clubroot plasmodia, slug feeding, symbiotic structures),
all inside a round, lit eyepiece field.
"""
import math

from pixelart import Img, clamp, mix, rng_for, scale

N = 128
CENTER = (N - 1) / 2.0
RADIUS = 61.0

# Colour hints per affliction id (spores, bodies), otherwise by category.
COLORS = {
    "wheat_stem_rust": (196, 84, 30), "wheat_leaf_rust": (214, 120, 40), "powdery_mildew_of_cereals": (236, 236, 228),
    "late_blight": (220, 220, 210), "early_blight": (70, 52, 40), "northern_corn_leaf_blight": (90, 76, 60),
    "corn_smut": (40, 36, 40), "gray_mold": (150, 150, 146), "white_mold": (245, 245, 240), "fusarium_head_blight_scab": (226, 120, 150),
    "takeall": (40, 32, 28), "dampingoff": (220, 220, 210), "onion_downy_mildew": (170, 160, 190),
    "aphids": (110, 170, 70), "colorado_potato_beetle": (230, 180, 60), "european_corn_borer": (210, 190, 160),
    "western_corn_rootworm": (200, 200, 80), "wireworms": (200, 150, 70), "desert_locust": (200, 170, 60),
    "cabbage_white_butterfly": (150, 200, 90), "slugs": (130, 110, 90),
    "trichoderma": (60, 150, 70), "arbuscular_mycorrhizal_fungi": (230, 230, 200), "rhizobia_nodule_bacteria": (200, 120, 130),
}
CATEGORY_COLORS = {"fungus": (230, 230, 220), "oomycete": (220, 220, 210), "bacterium": (200, 180, 120),
                   "virus": (40, 70, 40), "insect": (120, 150, 70), "nematode": (225, 220, 200), "protist": (180, 140, 160),
                   "mollusc": (130, 110, 90), "symbiont": (220, 210, 180)}


class Canvas:
    def __init__(self):
        self.px = [[(0, 0, 0) for _ in range(N)] for _ in range(N)]

    def get(self, x, y):
        return self.px[y][x]

    def blend(self, x, y, c, a):
        if 0 <= x < N and 0 <= y < N and a > 0:
            o = self.px[y][x]
            self.px[y][x] = tuple(clamp(o[i] + (c[i] - o[i]) * min(1.0, a)) for i in range(3))

    def disc(self, cx, cy, r, c, a=1.0, shade=0.35):
        for y in range(int(cy - r - 1), int(cy + r + 2)):
            for x in range(int(cx - r - 1), int(cx + r + 2)):
                d = math.hypot(x - cx, y - cy)
                if d <= r:
                    light = 1.0 + shade * (0.6 - ((x - cx + r * 0.4) ** 2 + (y - cy + r * 0.4) ** 2) ** 0.5 / max(1, r))
                    self.blend(x, y, scale(c, light)[:3], a * min(1.0, r - d + 0.6))

    def ellipse(self, cx, cy, rx, ry, angle, c, a=1.0, shade=0.3):
        ca, sa = math.cos(angle), math.sin(angle)
        r = max(rx, ry)
        for y in range(int(cy - r - 1), int(cy + r + 2)):
            for x in range(int(cx - r - 1), int(cx + r + 2)):
                dx, dy = x - cx, y - cy
                u, v = (dx * ca + dy * sa) / rx, (-dx * sa + dy * ca) / ry
                d = u * u + v * v
                if d <= 1.0:
                    light = 1.0 + shade * (0.5 - (u + 0.4) ** 2 - (v + 0.4) ** 2)
                    self.blend(x, y, scale(c, light)[:3], a * min(1.0, (1.0 - d) * 4 + 0.3))

    def line(self, x0, y0, x1, y1, c, w=1.0, a=1.0):
        steps = int(max(abs(x1 - x0), abs(y1 - y0)) * 2) + 1
        for i in range(steps + 1):
            t = i / steps
            x, y = x0 + (x1 - x0) * t, y0 + (y1 - y0) * t
            if w <= 1.0:
                self.blend(int(round(x)), int(round(y)), c, a)
            else:
                self.disc(x, y, w / 2.0, c, a, shade=0.0)

    def to_img(self):
        img = Img(N, N)
        for y in range(N):
            for x in range(N):
                d = math.hypot(x - CENTER, y - CENTER)
                if d > RADIUS + 1:
                    img.px[y][x] = (8, 8, 10, 255)
                    continue
                c = self.px[y][x]
                vignette = 1.0 - 0.45 * (d / RADIUS) ** 3
                c = tuple(clamp(v * vignette) for v in c)
                if d > RADIUS - 1.5:
                    c = mix((*c, 255), (30, 30, 34, 255), min(1.0, (d - RADIUS + 1.5) / 2.5))[:3]
                img.px[y][x] = (*c, 255)
        return img


def leaf_cells(cv, rng, tint=(150, 200, 110), cell=14, chloroplasts=True, mosaic=None):
    """Host tissue: polygonal cells (jittered-grid Voronoi) with walls, nuclei and chloroplasts."""
    g = N // cell + 2
    pts = {}
    for gy in range(-1, g):
        for gx in range(-1, g):
            pts[(gx, gy)] = (gx * cell + rng.uniform(0.15, 0.85) * cell, gy * cell + rng.uniform(0.15, 0.85) * cell,
                             rng.uniform(-0.06, 0.06))
    owner = [[None] * N for _ in range(N)]
    for y in range(N):
        for x in range(N):
            gx, gy = x // cell, y // cell
            best = second = 1e9
            who = None
            for oy in (-1, 0, 1):
                for ox in (-1, 0, 1):
                    p = pts.get((gx + ox, gy + oy))
                    if p is None:
                        continue
                    d = (x - p[0]) ** 2 + (y - p[1]) ** 2
                    if d < best:
                        second, best, who = best, d, (gx + ox, gy + oy)
                    elif d < second:
                        second = d
            owner[y][x] = who
            edge = math.sqrt(second) - math.sqrt(best)
            base = tint
            if mosaic is not None and (hash(who) % 3 == 0):
                base = mosaic
            f = 1.0 + pts[who][2] + 0.05 * math.sin(x * 0.3 + y * 0.2)
            c = scale(base, f)[:3]
            if edge < 1.6:
                c = scale(base, 0.62)[:3]
            elif edge < 2.6:
                c = scale(base, 0.85)[:3]
            cv.px[y][x] = c
    for (gx, gy), (px, py, _) in pts.items():
        if 0 <= px < N and 0 <= py < N:
            cv.ellipse(px + rng.uniform(-3, 3), py + rng.uniform(-3, 3), 2.0, 1.6, rng.uniform(0, 3), scale(tint, 0.7)[:3], 0.8)
            if chloroplasts:
                for _ in range(5):
                    a = rng.uniform(0, 6.28)
                    r = rng.uniform(3, cell * 0.45)
                    cv.ellipse(px + math.cos(a) * r, py + math.sin(a) * r, 1.6, 1.1, a, (60, 140, 50), 0.85)
    return owner, pts


def wander(cv, rng, x, y, length, color, width=1.6, turn=0.35, branch=0.04, depth=0):
    a = rng.uniform(0, 6.28)
    for _ in range(length):
        nx, ny = x + math.cos(a) * 2, y + math.sin(a) * 2
        cv.line(x, y, nx, ny, color, width, 0.9)
        x, y = nx, ny
        a += rng.uniform(-turn, turn)
        if depth < 2 and rng.random() < branch:
            wander(cv, rng, x, y, length // 3, color, width * 0.8, turn, branch, depth + 1)
        if not (-10 < x < N + 10 and -10 < y < N + 10):
            break
    return x, y


def fungus(cv, rng, aid, color):
    hypha = mix((*color, 255), (240, 240, 235, 255), 0.5)[:3]
    for _ in range(7):
        wander(cv, rng, rng.uniform(0, N), rng.uniform(0, N), 70, hypha, 1.8)
    if "rust" in aid:
        for _ in range(4):
            cx, cy = rng.uniform(25, 100), rng.uniform(25, 100)
            for _ in range(28):
                a, r = rng.uniform(0, 6.28), rng.uniform(0, 13)
                cv.ellipse(cx + math.cos(a) * r, cy + math.sin(a) * r, 3.4, 2.8, a, color)
                cv.blend(int(cx + math.cos(a) * r - 1), int(cy + math.sin(a) * r - 1), (255, 220, 160), 0.6)
    elif "mildew" in aid:
        for _ in range(14):
            x, y = rng.uniform(10, 118), rng.uniform(10, 118)
            a = rng.uniform(-1.9, -1.2)
            for i in range(6):
                cv.ellipse(x + math.cos(a) * i * 4.4, y + math.sin(a) * i * 4.4, 2.6, 1.9, a, color)
    elif aid in ("early_blight", "northern_corn_leaf_blight"):
        for _ in range(16):
            x, y, a = rng.uniform(10, 118), rng.uniform(10, 118), rng.uniform(0, 6.28)
            for i in range(5):
                cv.ellipse(x + math.cos(a) * i * 2.6, y + math.sin(a) * i * 2.6, 2.4 - i * 0.25, 2.0 - i * 0.2, a, color)
                cv.line(x + math.cos(a) * i * 2.6 - 2 * math.sin(a), y + math.sin(a) * i * 2.6 + 2 * math.cos(a),
                        x + math.cos(a) * i * 2.6 + 2 * math.sin(a), y + math.sin(a) * i * 2.6 - 2 * math.cos(a), scale(color, 0.6)[:3])
    elif aid == "gray_mold":
        for _ in range(9):
            x, y = rng.uniform(15, 113), rng.uniform(15, 113)
            tx, ty = wander(cv, rng, x, y, 10, scale(color, 0.7)[:3], 1.6, 0.15)
            for _ in range(14):
                cv.ellipse(tx + rng.gauss(0, 4), ty + rng.gauss(0, 4), 1.8, 1.4, rng.uniform(0, 3), color)
    elif aid in ("corn_smut", "takeall"):
        for _ in range(240):
            cv.disc(rng.uniform(5, 123), rng.uniform(5, 123), rng.uniform(1.4, 2.4), color)
    else:
        for _ in range(40):
            cv.ellipse(rng.uniform(5, 123), rng.uniform(5, 123), rng.uniform(2.5, 4.5), rng.uniform(1.2, 2.2), rng.uniform(0, 3.1), color)


def oomycete(cv, rng, aid, color):
    hypha = (235, 235, 228)
    for _ in range(6):
        wander(cv, rng, rng.uniform(0, N), rng.uniform(0, N), 60, hypha, 1.6, 0.25)
    for _ in range(7):
        x, y = rng.uniform(20, 108), rng.uniform(40, 118)
        tx, ty = x, y - rng.uniform(25, 40)
        cv.line(x, y, tx, ty, hypha, 1.6)
        for side in (-1, 1):
            bx, by = tx + side * rng.uniform(6, 10), ty - rng.uniform(4, 10)
            cv.line(tx, ty, bx, by, hypha, 1.2)
            cv.ellipse(bx, by - 3, 2.6, 3.8, 0.3 * side, color)
            cv.disc(bx, by - 6.6, 0.9, scale(color, 0.7)[:3])
        cv.ellipse(tx, ty - 5, 2.6, 3.8, 0, color)


def bacterium(cv, rng, aid, color):
    for _ in range(9):
        cx, cy = rng.uniform(15, 113), rng.uniform(15, 113)
        for _ in range(40):
            a = rng.uniform(0, 3.14)
            cv.ellipse(cx + rng.gauss(0, 7), cy + rng.gauss(0, 7), 2.6, 1.0, a, color, 0.95, shade=0.5)
    cv.line(0, rng.uniform(40, 90), N, rng.uniform(40, 90), scale(color, 1.1)[:3], 5, 0.25)


def virus(cv, rng, aid, color):
    for _ in range(20):
        x, y = rng.uniform(10, 118), rng.uniform(10, 118)
        if "mosaic" in aid:
            for i in range(5):
                cv.line(x + i * 1.6, y, x + i * 1.6 + 4, y + 10, (30, 50, 30), 1.0, 0.8)
        else:
            cv.disc(x, y, rng.uniform(2.5, 4), (70, 90, 60), 0.85)
            for _ in range(6):
                cv.disc(x + rng.gauss(0, 2), y + rng.gauss(0, 2), 0.8, (20, 30, 20))


def insect(cv, rng, aid, color):
    cx, cy = 64 + rng.uniform(-6, 6), 64 + rng.uniform(-6, 6)
    a = rng.uniform(0, 6.28)
    fx, fy = math.cos(a), math.sin(a)
    larval = aid in ("european_corn_borer", "western_corn_rootworm", "wireworms", "cabbage_white_butterfly")
    if larval:
        for i in range(9):
            t = i - 4
            r = 7.5 - abs(t) * 0.5
            cv.disc(cx + fx * t * 6 + math.sin(i) * 1.5, cy + fy * t * 6, r, color)
            cv.disc(cx + fx * t * 6 - fy * r * 0.5, cy + fy * t * 6 + fx * r * 0.5, 1.0, scale(color, 0.5)[:3])
        cv.disc(cx + fx * 30, cy + fy * 30, 5.5, scale(color, 0.55)[:3])
        return
    body = [(-14, 13, 9), (0, 9, 7), (11, 6, 5.5)]
    for off, rx, ry in body:
        cv.ellipse(cx + fx * off, cy + fy * off, rx, ry, a, color, shade=0.45)
    if aid == "colorado_potato_beetle":
        for k in range(-2, 3):
            cv.line(cx + fx * -24 - fy * k * 3.5, cy + fy * -24 + fx * k * 3.5, cx + fx * -4 - fy * k * 3.5, cy + fy * -4 + fx * k * 3.5,
                    (30, 26, 20), 1.6)
    for side in (-1, 1):
        for k, off in enumerate((-6, 0, 6)):
            bx, by = cx + fx * off, cy + fy * off
            kx, ky = bx - fy * side * 16, by + fx * side * 16
            cv.line(bx, by, kx + fx * (k - 1) * 6, ky + fy * (k - 1) * 6, scale(color, 0.55)[:3], 2.0)
            cv.line(kx + fx * (k - 1) * 6, ky + fy * (k - 1) * 6, kx + fx * (k - 1) * 12 - fy * side * 6,
                    ky + fy * (k - 1) * 12 + fx * side * 6, scale(color, 0.45)[:3], 1.4)
        cv.line(cx + fx * 15, cy + fy * 15, cx + fx * 32 - fy * side * 10, cy + fy * 32 + fx * side * 10, scale(color, 0.4)[:3], 1.2)
        cv.disc(cx + fx * 14 - fy * side * 3.5, cy + fy * 14 + fx * side * 3.5, 1.6, (20, 20, 20))
    if aid == "desert_locust":
        cv.line(cx - fx * 4, cy - fy * 4, cx - fx * 34 + fy * 9, cy - fy * 34 - fx * 9, scale(color, 0.7)[:3], 3.0)
    if aid == "aphids":
        for _ in range(6):
            cv.ellipse(cx + rng.uniform(-50, 50), cy + rng.uniform(-50, 50), 4, 3, rng.uniform(0, 3), color)


def nematode(cv, rng, aid, color):
    for _ in range(5):
        x, y, a = rng.uniform(10, 118), rng.uniform(10, 118), rng.uniform(0, 6.28)
        for i in range(60):
            w = 2.6 * math.sin(math.pi * i / 60) + 0.6
            nx, ny = x + math.cos(a) * 1.6, y + math.sin(a) * 1.6
            cv.disc(nx, ny, w, color, 0.9, shade=0.4)
            x, y = nx, ny
            a += math.sin(i * 0.25) * 0.12
    for _ in range(30):
        cv.ellipse(rng.uniform(10, 118), rng.uniform(10, 118), 2.4, 1.5, rng.uniform(0, 3), scale(color, 0.9)[:3])


def protist(cv, rng, aid, color):
    for _ in range(5):
        cx, cy, r = rng.uniform(25, 103), rng.uniform(25, 103), rng.uniform(12, 18)
        cv.disc(cx, cy, r, scale(color, 0.85)[:3], 0.6)
        for _ in range(70):
            a, d = rng.uniform(0, 6.28), rng.uniform(0, r - 2)
            cv.disc(cx + math.cos(a) * d, cy + math.sin(a) * d, 1.2, scale(color, 0.6)[:3])


def mollusc(cv, rng, aid, color):
    for _ in range(4):
        cx, cy, r = rng.uniform(20, 108), rng.uniform(20, 108), rng.uniform(8, 16)
        for y in range(int(cy - r), int(cy + r)):
            for x in range(int(cx - r), int(cx + r)):
                if math.hypot(x - cx, (y - cy) * 1.3) < r + math.sin(x * 0.8) * 2:
                    cv.blend(x, y, (40, 48, 34), 0.9)
    y0 = rng.uniform(40, 90)
    for x in range(N):
        y = y0 + math.sin(x * 0.07) * 12
        for k in range(-5, 6):
            cv.blend(x, int(y + k), (230, 235, 240), 0.35 * (1 - abs(k) / 6))
    for row in range(5):
        for i in range(11):
            x, y = 22 + i * 8 + row * 2, 18 + row * 6
            cv.line(x, y, x + 2, y + 4, (190, 160, 120), 1.2)


def symbiont(cv, rng, aid, color):
    if "rhizobia" in aid:
        for _ in range(6):
            cx, cy, r = rng.uniform(25, 103), rng.uniform(25, 103), rng.uniform(10, 15)
            cv.disc(cx, cy, r, (210, 140, 150), 0.55)
            for _ in range(90):
                a, d = rng.uniform(0, 6.28), rng.uniform(0, r - 1.5)
                cv.ellipse(cx + math.cos(a) * d, cy + math.sin(a) * d, 1.6, 0.8, a, scale(color, 0.8)[:3])
    elif "mycorrhizal" in aid:
        for _ in range(4):
            wander(cv, rng, rng.uniform(0, N), rng.uniform(0, N), 70, (235, 235, 210), 1.8)
        for _ in range(6):
            cx, cy = rng.uniform(20, 108), rng.uniform(20, 108)
            for _ in range(5):
                wander(cv, rng, cx, cy, 6, (245, 240, 200), 1.0, 0.8, 0.3)
            cv.disc(rng.uniform(10, 118), rng.uniform(10, 118), 5, (230, 220, 160))
    else:
        for _ in range(6):
            wander(cv, rng, rng.uniform(0, N), rng.uniform(0, N), 60, (220, 235, 220), 1.6)
        for _ in range(12):
            cx, cy = rng.uniform(10, 118), rng.uniform(10, 118)
            for _ in range(10):
                cv.disc(cx + rng.gauss(0, 3), cy + rng.gauss(0, 3), 1.4, color)


DRAW = {"fungus": fungus, "oomycete": oomycete, "bacterium": bacterium, "virus": virus, "insect": insect,
        "nematode": nematode, "protist": protist, "mollusc": mollusc, "symbiont": symbiont}


def view(aid, category):
    rng = rng_for("microscope", aid)
    cv = Canvas()
    color = COLORS.get(aid, CATEGORY_COLORS.get(category, (220, 220, 210)))
    if category == "virus":
        leaf_cells(cv, rng, mosaic=(205, 220, 120) if "mosaic" in aid else (170, 200, 120))
    elif category in ("nematode", "protist") or aid in ("takeall", "rhizobia_nodule_bacteria", "arbuscular_mycorrhizal_fungi",
                                                         "western_corn_rootworm", "wireworms", "common_scab"):
        leaf_cells(cv, rng, tint=(214, 200, 168), cell=12, chloroplasts=False)
    else:
        leaf_cells(cv, rng)
    DRAW.get(category, fungus)(cv, rng, aid, color)
    return cv.to_img()


def healthy():
    rng = rng_for("microscope", "healthy")
    cv = Canvas()
    leaf_cells(cv, rng)
    for _ in range(3):
        x, y = rng.uniform(25, 103), rng.uniform(25, 103)
        cv.ellipse(x - 4, y, 3.4, 8, 0, (110, 170, 90))
        cv.ellipse(x + 4, y, 3.4, 8, 0, (110, 170, 90))
        cv.ellipse(x, y, 1.2, 6, 0, (40, 70, 40))
    return cv.to_img()


def generate(afflictions, save):
    for aid, a in afflictions.items():
        save(view(aid, a["category"]), "gui", "microscope", aid + ".png")
    for category in DRAW:
        save(view("generic_" + category, category), "gui", "microscope", category + ".png")
    save(healthy(), "gui", "microscope", "healthy.png")
