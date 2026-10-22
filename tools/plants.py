"""Procedural crop sprites for the ten visual growth stages of every growth form."""
import math

from pixelart import Img, mix, rng_for, scale

STRAW = (206, 176, 104, 255)
DRY = (150, 118, 72, 255)


def _c(c):
    return c if len(c) > 3 else (*c, 255)


def _sen(stage, amounts):
    return amounts.get(stage, 0.0)


def sown(img, crop):
    H = img.h
    seed = _c(crop.get("seed_color", (170, 140, 90)))
    for x, y in ((4, H - 1), (8, H - 2), (12, H - 1), (6, H - 1)):
        img.set(x, y, seed)


def cereal(img, crop, stage, H):
    xs = [2, 5, 8, 11, 14, 4, 10, 7]
    n = [0, 3, 4, 6, 7, 7, 7, 7, 7, 7][stage]
    height = [0, 3, 5, 7, 10, 12, 13, 14, 14, 14][stage]
    leaf = mix(_c(crop["leaf"]), STRAW, _sen(stage, {7: 0.25, 8: 0.6, 9: 0.92}))
    for i in range(n):
        x = xs[i]
        h = max(2, height - (i % 3))
        lean = ((i % 3) - 1) if stage >= 4 else 0
        tx = x + lean
        img.line(x, H - 1, tx, H - 1 - h, leaf)
        for k in range(1 if stage <= 2 else 2):
            ly = H - 1 - max(1, int(h * (0.35 + 0.3 * k)))
            d = 1 if (i + k) % 2 == 0 else -1
            lx = x + int(round(lean * (H - 1 - ly) / max(h, 1)))
            img.line(lx, ly, lx + 2 * d, ly - 1, scale(leaf, 0.92))
            img.set(lx + 3 * d, ly, scale(leaf, 0.82))
        if stage >= 5:
            head = _c(crop["head"]) if stage <= 6 else mix(_c(crop["head"]), _c(crop["ripe"]), {7: 0.4, 8: 0.75, 9: 1.0}[stage])
            hy = H - 1 - h
            style = crop.get("head_style", "spike")
            if style == "spike":
                img.rect(tx, hy - 1, tx, hy + 2, head)
                img.rect(tx + 1, hy, tx + 1, hy + 1, scale(head, 0.85))
                if crop.get("awns"):
                    awn = scale(head, 1.12)
                    img.line(tx, hy - 2, tx - 1, hy - 4, awn)
                    img.line(tx + 1, hy - 1, tx + 2, hy - 3, awn)
            elif style == "panicle":
                img.line(tx, hy - 1, tx, hy + 2, scale(head, 0.8))
                for j in range(4):
                    img.set(tx - 1 + 2 * (j % 2), hy + j, head)
            elif style == "drooping":
                img.line(tx, hy, tx + 2, hy + 1, head)
                img.set(tx + 3, hy + 2, head)
                img.set(tx + 3, hy + 3, scale(head, 0.9))
            if stage == 6:
                img.set(tx - 1, hy, (240, 232, 160, 255))
                img.set(tx + 2, hy + 1, (240, 232, 160, 255))


def tall_grass(img, crop, stage, H):
    leaf = _c(crop["leaf"])
    t = _sen(stage, {7: 0.2, 8: 0.6, 9: 0.9})
    leafc = mix(leaf, STRAW, t)
    if H == 16:
        n = [0, 2, 3, 4, 5, 5][min(stage, 5)]
        hgt = [0, 4, 7, 10, 13, 15][min(stage, 5)]
        cx = 8
        img.line(cx, 15, cx, 15 - hgt // 2, leafc)
        for k in range(n):
            d = 1 if k % 2 == 0 else -1
            ly = 15 - int(hgt * (0.2 + 0.16 * k))
            ex = cx + d * (3 + k)
            img.line(cx, ly, ex, ly - 3, leafc)
            img.line(ex, ly - 3, ex + d, ly - 1, scale(leafc, 0.88))
            img.line(cx, ly + 1, ex, ly - 2, scale(leafc, 0.85))
        return
    stalk_h = min(H - 4, 16 + (stage - crop["tall_from"]) * 4)
    cx = 7
    stalk = mix(scale(leaf, 0.9), STRAW, t)
    img.rect(cx, H - 1 - stalk_h, cx + 1, H - 1, stalk)
    top = H - 1 - stalk_h
    k = 0
    for ny in range(H - 4, top + 3, -4):
        d = 1 if k % 2 == 0 else -1
        bx = cx + (1 if d > 0 else 0)
        img.line(bx, ny, bx + 5 * d, ny - 3, leafc)
        img.line(bx + 5 * d, ny - 3, bx + 7 * d, ny - 1, scale(leafc, 0.85))
        img.line(bx, ny + 1, bx + 4 * d, ny - 2, scale(leafc, 0.9))
        k += 1
    if crop["id"] == "maize":
        if stage >= 5:
            tc = mix((200, 190, 110, 255), (170, 140, 90, 255), t)
            img.line(cx, top, cx, top - 3, tc)
            img.line(cx, top - 1, cx - 2, top - 3, tc)
            img.line(cx + 1, top - 1, cx + 3, top - 3, tc)
        if stage >= 6:
            ey = H - 1 - int(stalk_h * 0.5)
            husk = mix((120, 170, 80, 255), (205, 185, 125, 255), t)
            img.ellipse(cx + 3, ey, 1.3, 2.7, husk, shade=0.25)
            silk = (230, 200, 160, 255) if stage == 6 else (140, 90, 50, 255)
            img.line(cx + 3, ey - 3, cx + 4, ey - 5, silk)
    else:
        if stage >= 5:
            hc = _c(crop["head"]) if stage < 7 else mix(_c(crop["head"]), _c(crop["ripe"]), (stage - 6) / 3)
            img.ellipse(cx + 0.5, top - 2, 2.2, 3.2, hc, shade=0.35)


def legume(img, crop, stage, H):
    rng = rng_for("legume", crop["id"])
    low = crop.get("low", False)
    climbing = crop.get("climbing", False)
    hmax = 8 if low else (15 if climbing else 13)
    h = max(2, int(hmax * [0, .15, .3, .5, .7, .85, .95, 1, 1, .95][stage]))
    leaf = mix(_c(crop["leaf"]), (176, 150, 78, 255), _sen(stage, {8: 0.5, 9: 0.85}))
    if stage == 1:
        img.line(8, 15, 8, 14, leaf)
        img.ellipse(6.5, 13.5, 1.6, 1.0, leaf)
        img.ellipse(9.5, 13.5, 1.6, 1.0, scale(leaf, 0.9))
        return
    stems = [3, 7, 11, 14] if climbing else [4, 8, 12]
    for sx in stems:
        top = 15 - h + rng.randrange(0, 2)
        img.line(sx, 15, sx + (1 if climbing else 0), top, scale(leaf, 0.78))
        for ly in range(13, top - 1, -3):
            for dx, dy in ((-1, 0), (1, 0), (0, -1)):
                img.set(sx + dx, ly + dy, scale(leaf, rng.choice((0.9, 1.0, 1.1))))
            if stage >= 3:
                img.set(sx - 2, ly + 1, scale(leaf, 0.88))
                img.set(sx + 2, ly + 1, scale(leaf, 1.05))
    if stage in (5, 6):
        for _ in range(5 if stage == 6 else 2):
            img.set(rng.randrange(2, 14), 15 - rng.randrange(3, max(4, h)), _c(crop["flower"]))
    if stage >= 7:
        pod = _c(crop["pod"]) if stage == 7 else mix(_c(crop["pod"]), _c(crop["ripe"]), 0.6 if stage == 8 else 1.0)
        for _ in range(5):
            px = rng.randrange(2, 14)
            py = 15 - rng.randrange(2, max(3, h - 1))
            img.line(px, py, px, py + 2, pod)


def forage(img, crop, stage, H):
    rng = rng_for("forage", crop["id"], stage)
    h = int(12 * [0, .15, .3, .45, .6, .75, .85, .95, 1, 1][stage])
    leaf = mix(_c(crop["leaf"]), STRAW, _sen(stage, {9: 0.45}))
    for _ in range(10 + stage * 7):
        x = rng.randrange(1, 15)
        top = 15 - int(h * (1 - abs(x - 7.5) / 9.0))
        y = rng.randrange(max(top, 0), 16)
        img.set(x, y, scale(leaf, rng.choice((0.82, 1.0, 1.12))))
    if stage >= 6:
        for _ in range(6 if stage < 9 else 4):
            x = rng.randrange(2, 14)
            y = 15 - h + rng.randrange(0, 3)
            fc = _c(crop["flower"]) if stage < 9 else (120, 90, 60, 255)
            if crop["id"] == "clover":
                img.ellipse(x + 0.5, y + 0.5, 1.1, 1.1, fc, shade=0.3)
            else:
                img.set(x, y, fc)
                img.set(x, y - 1, scale(fc, 1.12))


def tuber(img, crop, stage, H):
    rng = rng_for("tuber", crop["id"])
    h = max(2, int(12 * [0, .2, .4, .55, .7, .85, .95, 1, .8, .45][stage]))
    leaf = mix(_c(crop["leaf"]), (140, 118, 66, 255), _sen(stage, {8: 0.5, 9: 0.88}))
    for sx in (4, 8, 12):
        top = 15 - h + rng.randrange(0, 2)
        img.line(sx, 15, sx + rng.choice((-1, 0, 1)), top, scale(leaf, 0.8))
        for ly in range(13, top - 1, -3):
            img.ellipse(sx - 1 + 0.5, ly + 0.5, 1.3, 1.0, leaf, shade=0.3)
            img.ellipse(sx + 1.5, ly - 0.5, 1.2, 0.9, scale(leaf, 1.05), shade=0.3)
    if stage in (6, 7):
        for _ in range(4 if stage == 6 else 2):
            x, y = rng.randrange(2, 14), 15 - h + rng.randrange(0, 2)
            img.set(x, y, _c(crop["flower"]))
            img.set(x + 1, y, (240, 210, 60, 255))


def root(img, crop, stage, H):
    h = max(2, int(11 * [0, .2, .35, .5, .65, .8, .9, 1, 1, .95][stage]))
    leaf = mix(_c(crop["leaf"]), STRAW, _sen(stage, {9: 0.3}))
    if crop.get("feathery"):
        for k in range(4 + stage):
            dx = (k - (4 + stage) / 2) * 1.1
            ex, ey = 8 + dx, 15 - h + abs(dx) * 0.4
            img.line(8, 14, ex, ey, scale(leaf, 0.85))
            for j in range(1, 3):
                px = 8 + dx * j / 3
                py = 14 - (14 - ey) * j / 3
                img.set(px + 1, py, leaf)
                img.set(px - 1, py, scale(leaf, 1.1))
    else:
        stem = _c(crop.get("stem", scale(leaf, 0.8)))
        n = 2 + stage // 2
        for k in range(n):
            dx = (k - (n - 1) / 2) * 2.8
            tip_x, tip_y = 8 + dx * 1.15, 15 - h + abs(dx) * 0.35
            img.line(8, 14, 8 + dx * 0.5, 15 - h * 0.45, stem)
            img.ellipse(tip_x, tip_y + 1.5, 1.2, 2.1, scale(leaf, 0.95 + 0.05 * (k % 2)), shade=0.4)
            img.line(8 + dx * 0.5, 15 - h * 0.45, tip_x, tip_y, scale(stem, 1.1))
    if stage >= 4:
        r = 0.8 + (stage - 4) * 0.35
        img.ellipse(8, 15.2, r + 0.5, r * 0.75, _c(crop["root"]), shade=0.35)


def bulb(img, crop, stage, H):
    n = min(6, 2 + stage // 2)
    h = max(2, int(13 * [0, .2, .35, .5, .65, .8, .9, 1, .9, .6][stage]))
    leaf = mix(_c(crop["leaf"]), (200, 170, 100, 255), _sen(stage, {8: 0.4, 9: 0.9}))
    bulb_r = [0, 0, 0, 0.6, 1.0, 1.4, 1.8, 2.2, 2.5, 2.7][stage]
    for k in range(n):
        x = 8 + (k - n / 2) * 1.2
        if stage >= 8 and k % 2 == 0:
            mid = 15 - h // 2
            img.line(x, 15, x, mid, leaf)
            img.line(x, mid, x + (3 if k % 4 == 0 else -3), mid + 1, scale(leaf, 0.9))
        else:
            img.line(x, 14, x + (k - n / 2) * 0.35, 15 - h, leaf)
    if bulb_r > 0:
        img.ellipse(8, 14.6, bulb_r + 0.4, bulb_r * 0.8, _c(crop["root"]), shade=0.35)


def head(img, crop, stage, H):
    rng = rng_for("head", crop["id"], stage)
    ros = [0, 2, 3, 4, 5, 6, 6.5, 7, 7, 7][stage]
    head_r = [0, 0, 0, 0, 0, 1.5, 2.2, 3, 3.5, 4][stage]
    leaf = _c(crop["leaf"])
    for k in range(6):
        a = math.pi * k / 5.0
        x = 8 + math.cos(a) * ros * 0.7
        y = 15 - math.sin(a) * ros * 0.45 - 0.5
        img.ellipse(x, y, ros * 0.35 + 0.8, 1.1 + ros * 0.15, scale(leaf, 0.92 + 0.04 * (k % 3)), shade=0.3)
    if head_r:
        img.ellipse(8, 15 - head_r * 0.9, head_r, head_r * 0.9, _c(crop["head"]), shade=0.35)
    if crop.get("frilly"):
        for _ in range(4 + stage * 2):
            x, y = rng.randrange(1, 15), rng.randrange(16 - int(ros) - 2, 16)
            if img.opaque(x, y):
                img.set(x, y, scale(leaf, 1.2))


def fruiting(img, crop, stage, H):
    rng = rng_for("fruit", crop["id"])
    h = max(2, int(14 * [0, .15, .3, .45, .6, .75, .85, .95, 1, 1][stage]))
    leaf = _c(crop["leaf"])
    pts = [(8, 15)]
    for i in range(1, 6):
        y = 15 - int(h * i / 5)
        pts.append((8 + (2 if i % 2 else -2) + rng.choice((-1, 0, 1)), y))
    for a, b in zip(pts, pts[1:]):
        img.line(a[0], a[1], b[0], b[1], scale(leaf, 0.75))
    for x, y in pts[1:]:
        img.ellipse(x - 1.5, y + 0.5, 1.4, 1.0, leaf, shade=0.3)
        img.ellipse(x + 2, y, 1.3, 1.0, scale(leaf, 1.06), shade=0.3)
    if stage == 5:
        for x, y in pts[2:4]:
            img.set(x, y - 1, (200, 210, 90, 255))
    if stage == 6:
        for x, y in pts[2:5]:
            img.set(x + 1, y - 1, _c(crop["flower"]))
            img.set(x, y - 1, _c(crop["flower"]))
    if stage >= 7:
        fc = _c(crop["fruit"]) if stage == 7 else mix(_c(crop["fruit"]), _c(crop["ripe"]), 0.55 if stage == 8 else 1.0)
        for x, y in pts[1:5]:
            img.ellipse(x + 0.5, y + 2.5, 1.2, 1.2, fc, shade=0.4)


def tall_flower(img, crop, stage, H):
    leaf = mix(_c(crop["leaf"]), (130, 100, 55, 255), _sen(stage, {8: 0.4, 9: 0.85}))
    if H == 16:
        h = [0, 3, 6, 10, 13][min(stage, 4)]
        img.line(8, 15, 8, 15 - h, scale(leaf, 0.85))
        for k in range(min(4, stage + 1)):
            d = 1 if k % 2 == 0 else -1
            img.ellipse(8 + d * 2.5, 15 - h * (0.3 + 0.2 * k), 2.0, 1.2, leaf, shade=0.3)
        return
    stalk_h = min(H - 7, 14 + (stage - crop["tall_from"]) * 3)
    img.rect(7, H - 1 - stalk_h, 8, H - 1, scale(leaf, 0.8))
    k = 0
    for ny in range(H - 4, H - 1 - stalk_h + 4, -5):
        d = 1 if k % 2 == 0 else -1
        img.ellipse(7.5 + d * 3.2, ny, 2.6, 1.6, leaf, shade=0.3)
        k += 1
    top = H - 1 - stalk_h
    petals, centre = _c(crop["flower"]), _c(crop["center"])
    if stage == 5:
        img.ellipse(8, top - 1, 1.6, 1.6, scale(leaf, 1.1))
    elif stage in (6, 7):
        img.ellipse(8, top - 3, 4.2, 3.4, petals, shade=0.2)
        img.ellipse(8, top - 3, 2.2, 1.8, centre, shade=0.2)
    elif stage == 8:
        img.ellipse(9, top - 1, 3.4, 2.8, mix(petals, (120, 80, 40, 255), 0.5), shade=0.2)
        img.ellipse(9, top - 1, 2.2, 1.8, centre, shade=0.2)
    elif stage == 9:
        img.ellipse(9, top, 3.0, 2.2, (100, 70, 40, 255), shade=0.3)


def flax(img, crop, stage, H):
    h = max(2, int(14 * [0, .15, .3, .45, .6, .75, .85, .95, 1, 1][stage]))
    leaf = mix(_c(crop["leaf"]), STRAW, _sen(stage, {8: 0.6, 9: 0.95}))
    for i, x in enumerate((3, 5, 7, 9, 11, 13)):
        top = 15 - h + (i % 2)
        img.line(x, 15, x, top, leaf)
        for y in range(14, top, -2):
            img.set(x + (1 if (y + i) % 4 == 0 else -1), y, scale(leaf, 0.9))
        if stage == 6:
            img.set(x, top - 1, _c(crop["flower"]))
            img.set(x + 1, top, _c(crop["flower"]))
        elif stage >= 7:
            img.ellipse(x + 0.5, top, 0.9, 0.9, mix(_c(crop["pod"]), (180, 150, 90, 255), (stage - 7) / 2), shade=0.3)


def cotton(img, crop, stage, H):
    rng = rng_for("cotton", crop["id"])
    h = max(2, int(13 * [0, .15, .3, .45, .6, .75, .85, .95, 1, 1][stage]))
    leaf = mix(_c(crop["leaf"]), (150, 80, 50, 255), _sen(stage, {8: 0.4, 9: 0.8}))
    for sx in (4, 8, 12):
        top = 15 - h
        img.line(sx, 15, sx, top, scale(leaf, 0.75))
        for ly in range(13, top - 1, -3):
            img.ellipse(sx - 1.5, ly, 1.5, 1.1, leaf, shade=0.3)
            img.ellipse(sx + 1.5, ly - 1, 1.4, 1.0, scale(leaf, 1.05), shade=0.3)
    spots = [(rng.randrange(2, 14), 15 - rng.randrange(3, max(4, h))) for _ in range(5)]
    if stage == 6:
        for x, y in spots:
            img.set(x, y, _c(crop["flower"]))
    elif stage == 7:
        for x, y in spots:
            img.ellipse(x + 0.5, y + 0.5, 1.1, 1.1, _c(crop["pod"]), shade=0.3)
    elif stage >= 8:
        for x, y in spots:
            r = 1.0 if stage == 8 else 1.5
            img.ellipse(x + 0.5, y + 0.5, r, r, _c(crop["fiber"]), shade=0.15)
            if stage == 8:
                img.set(x, y + 1, (120, 80, 50, 255))


FORMS = {
    "cereal": cereal, "tall_grass": tall_grass, "legume": legume, "forage": forage, "tuber": tuber,
    "root": root, "bulb": bulb, "head": head, "fruiting": fruiting, "tall_flower": tall_flower,
    "flax": flax, "cotton": cotton,
}


def draw_plant(crop, stage, tall):
    H = 32 if tall else 16
    img = Img(16, H)
    if stage == 0:
        sown(img, crop)
        return img
    FORMS[crop["form"]](img, crop, stage, H)
    return img


def draw_dead_crop():
    img = Img(16, 16)
    c = (122, 94, 60, 255)
    for x, lean in ((3, 2), (6, -1), (9, 3), (12, -2)):
        img.line(x, 15, x + lean, 8, c)
        img.line(x + lean, 8, x + lean + (2 if lean > 0 else -2), 10, scale(c, 0.85))
    return img
