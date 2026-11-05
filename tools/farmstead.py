"""
Farmstead assets: breed body parts, the travelling farm merchant, compost bin, hay windrows, spinning wheel, butter
churn, cheese press, market stall (and its village structure), and the hay, wool and dairy items.
"""
import gzip
import io
import json
import math
import os
import struct

from pixelart import Img, mix, rng_for, scale
from breeds import faces

MOD = "tilth"
CLEAR = (0, 0, 0, 0)


def rgba(c, f=1.0):
    return scale((*c[:3], 255), f)


def paint_box(img, u, v, w, h, d, colour):
    """Paints every face of a box's UV layout with colour(face, i, j, rw, rh, rng) -> rgba (or CLEAR)."""
    rng = rng_for("box", u, v, w, h, d)
    for name, (rect, _) in faces(u, v, w, h, d).items():
        x0, y0, rw, rh = rect
        for j in range(rh):
            for i in range(rw):
                img.set(x0 + i, y0 + j, colour(name, i, j, rw, rh, rng))


SIDES = ("west", "north", "east", "south")


def hair(base, ragged_rows, bottom_clear=True, seed="hair", strand=0.25):
    """Long hair: strands in each column, a ragged bottom edge on the side faces and an open underside."""
    lengths = {}

    def colour(face, i, j, rw, rh, rng):
        if face == "bottom" and bottom_clear:
            return CLEAR
        if face in SIDES and ragged_rows:
            key = (face, i)
            if key not in lengths:
                lengths[key] = rh - rng.randrange(0, ragged_rows + 1)
            if j >= lengths[key]:
                return CLEAR
        shade = 0.82 + strand * (((i * 7 + (j // 3) * 3) % 5) / 4.0) + 0.06 * rng.random()
        return rgba(base, shade)
    return colour


def plain(base, var=0.08):
    return lambda face, i, j, rw, rh, rng: rgba(base, 1.0 - var / 2 + var * rng.random())


def curls(base, ragged_rows):
    lengths = {}

    def colour(face, i, j, rw, rh, rng):
        if face == "bottom":
            return CLEAR
        if face in SIDES and ragged_rows:
            key = (face, i)
            if key not in lengths:
                lengths[key] = rh - rng.randrange(0, ragged_rows + 1)
            if j >= lengths[key]:
                return CLEAR
        ring = ((i % 3) - 1) ** 2 + ((j % 3) - 1) ** 2
        return rgba(base, (1.12, 0.95, 0.82)[min(2, ring)] * (0.95 + 0.08 * rng.random()))
    return colour


def breed_feature_textures(save):
    hl = Img(64, 64)
    paint_box(hl, 0, 0, 12, 14, 18, hair((182, 96, 44), 6))
    paint_box(hl, 0, 32, 8, 6, 1, hair((176, 92, 42), 3, bottom_clear=False))
    horn = lambda face, i, j, rw, rh, rng: rgba((228, 216, 186) if i < rw - 1 else (90, 80, 70), 0.95 + 0.08 * rng.random())
    paint_box(hl, 0, 40, 7, 1, 1, horn)
    paint_box(hl, 0, 44, 1, 3, 1, lambda f, i, j, rw, rh, rng: rgba((228, 216, 186) if j > 0 else (90, 80, 70)))
    save(hl, "entity", "breed", "features", "highland.png")

    br = Img(32, 32)
    grey = (196, 192, 186)
    paint_box(br, 0, 0, 6, 3, 5, plain(scale((*grey, 255), 0.85)[:3]))
    paint_box(br, 0, 10, 2, 5, 3, lambda f, i, j, rw, rh, rng: rgba(grey, 0.8 + 0.1 * (j % 2) + 0.05 * rng.random()))
    paint_box(br, 0, 20, 2, 5, 1, lambda f, i, j, rw, rh, rng: rgba((214, 160, 150) if f == "south" else grey, 0.95 + 0.06 * rng.random()))
    save(br, "entity", "breed", "features", "brahman.png")

    mg = Img(64, 32)
    paint_box(mg, 0, 0, 10, 8, 16, curls((232, 206, 150), 2))
    save(mg, "entity", "breed", "features", "mangalica.png")

    me = Img(64, 32)
    paint_box(me, 0, 0, 8, 6, 16, curls((240, 236, 224), 1))
    save(me, "entity", "breed", "features", "merino.png")

    sh = Img(32, 16)
    paint_box(sh, 0, 0, 4, 4, 4, hair((236, 232, 222), 0, strand=0.18))
    save(sh, "entity", "breed", "features", "shire.png")


# ---------------------------------------------------------------- the farm merchant (villager model, 64x64)

def merchant_texture():
    img = Img(64, 64)
    rng = rng_for("farm_merchant")
    skin = (196, 140, 104)
    shirt_a, shirt_b = (176, 52, 44), (120, 30, 30)
    denim, denim_dark = (64, 92, 140), (44, 64, 104)
    boot = (86, 58, 36)
    straw, straw_dark = (226, 196, 112), (176, 146, 72)

    def skin_face(face, i, j, rw, rh, r):
        c = rgba(skin, 0.95 + 0.07 * r.random())
        if face == "north":
            if j == 3 and i in (1, 2, 5, 6):
                c = rgba((92, 70, 50))
            if j == 4 and i in (1, 6):
                c = rgba((250, 250, 250))
            if j == 4 and i in (2, 5):
                c = rgba((60, 96, 50))
            if j >= 7 and (i + j) % 2 == 0:
                c = rgba(skin, 0.75)
            if j == 6 and i in (1, 6):
                c = rgba((214, 120, 96))
        return c
    paint_box(img, 0, 0, 8, 10, 8, skin_face)
    paint_box(img, 24, 0, 2, 4, 2, plain(scale((*skin, 255), 0.9)[:3]))

    # Straw hat: the crown is the top of the hat layer; the 16x16 rim plate is a round brim.
    def crown(face, i, j, rw, rh, r):
        if face == "bottom":
            return CLEAR
        if face in SIDES and j > 3:
            return CLEAR
        if face in SIDES and j == 3:
            return rgba((150, 40, 32))
        return rgba(straw if (i + j) % 2 else straw_dark, 0.95 + 0.08 * r.random())
    paint_box(img, 32, 0, 8, 10, 8, crown)
    for x0 in (31, 48):
        for j in range(16):
            for i in range(16):
                d = math.hypot(i - 7.5, j - 7.5)
                img.set(x0 + i, 48 + j, rgba(straw if (i + j) % 2 else straw_dark, 0.95 + 0.08 * rng.random()) if d <= 7.8 else CLEAR)

    check = lambda face, i, j, rw, rh, r: rgba(shirt_a if ((i // 2) + (j // 2)) % 2 else shirt_b, 0.95 + 0.06 * r.random())
    paint_box(img, 16, 20, 8, 12, 6, check)

    # Overalls over shirt and legs: a bib with straps above the waist, denim below.
    def overalls(face, i, j, rw, rh, r):
        if face in ("top", "bottom"):
            return CLEAR
        if j < 7:
            front = face in ("north", "south")
            strap = front and (i in (1, rw - 2))
            bib = face == "north" and 2 <= i <= rw - 3 and j >= 3
            if not (strap or bib):
                return CLEAR
            if bib and j == 4 and i in (3, 4):
                return rgba((210, 200, 120))
        if j >= rh - 3:
            return rgba(boot, 0.9 + 0.1 * r.random())
        return rgba(denim if (i + j) % 3 else denim_dark, 0.95 + 0.06 * r.random())
    paint_box(img, 0, 38, 8, 20, 6, overalls)

    def legs(face, i, j, rw, rh, r):
        if j >= rh - 3:
            return rgba(boot, 0.9 + 0.1 * r.random())
        return rgba(denim, 0.92 + 0.08 * r.random())
    paint_box(img, 0, 22, 4, 12, 4, legs)

    def arms(face, i, j, rw, rh, r):
        if j >= rh - 2:
            return rgba(skin, 0.95)
        return check(face, i, j, rw, rh, r)
    paint_box(img, 44, 22, 4, 8, 4, arms)
    paint_box(img, 40, 38, 8, 4, 4, check)
    return img


# ---------------------------------------------------------------- block and item textures

def tex_slats():
    img = Img(16, 16)
    rng = rng_for("compost_slats")
    for y in range(16):
        for x in range(16):
            gap = y % 4 == 3
            img.set(x, y, rgba((40, 30, 20)) if gap else rgba((136, 98, 58), 0.9 + 0.12 * rng.random()))
    return img


def tex_speckle(base, bits, seed, amount=40):
    img = Img(16, 16)
    rng = rng_for(seed)
    for y in range(16):
        for x in range(16):
            img.set(x, y, rgba(base, 0.85 + 0.25 * rng.random()))
    for _ in range(amount):
        img.set(rng.randrange(16), rng.randrange(16), rgba(rng.choice(bits)))
    return img


def tex_hay(base, seed, spots=None):
    img = Img(16, 16)
    rng = rng_for(seed)
    for y in range(16):
        for x in range(16):
            img.set(x, y, rgba(base, 0.8 + 0.3 * rng.random()))
    for _ in range(22):
        x, y = rng.randrange(16), rng.randrange(16)
        img.line(x, y, min(15, x + rng.randrange(2, 6)), min(15, y + rng.randrange(0, 2)), rgba(base, 1.18))
    for _ in range(spots or 0):
        img.set(rng.randrange(16), rng.randrange(16), rgba((52, 60, 48)))
    return img


def tex_awning():
    img = Img(16, 16)
    for y in range(16):
        for x in range(16):
            img.set(x, y, rgba((196, 44, 40) if (x // 4) % 2 == 0 else (240, 236, 226)))
    return img


def tex_wheel():
    img = Img(16, 16)
    for y in range(16):
        for x in range(16):
            dx, dy = x - 7.5, y - 7.5
            r = math.hypot(dx, dy)
            if 6.0 <= r <= 7.6:
                img.set(x, y, rgba((116, 80, 46)))
            elif r < 1.5:
                img.set(x, y, rgba((86, 60, 36)))
            elif r < 6 and (abs(dx) < 0.6 or abs(dy) < 0.6 or abs(abs(dx) - abs(dy)) < 0.7):
                img.set(x, y, rgba((136, 98, 58)))
    return img


def icon(draw):
    img = Img()
    draw(img)
    img.outline(0.45)
    return img


def icon_cut_grass(img):
    rng = rng_for("cut_grass_icon")
    for k in range(12):
        x = 2 + k
        top = 3 + rng.randrange(5)
        img.line(x, 14, x + rng.choice((-1, 0, 1)), top, rgba((92, 150, 60), 0.85 + 0.3 * rng.random()))


def icon_hay(colour, spots=False):
    def draw(img):
        rng = rng_for("hay_icon", colour)
        for k in range(14):
            y = 4 + k % 9
            img.line(2 + rng.randrange(3), y, 12 + rng.randrange(3), y + rng.choice((-1, 0, 1)), rgba(colour, 0.85 + 0.3 * rng.random()))
        if spots:
            for _ in range(6):
                img.set(3 + rng.randrange(10), 4 + rng.randrange(9), rgba((50, 58, 46)))
    return draw


def icon_fleece(img):
    rng = rng_for("fleece_icon")
    for y in range(4, 13):
        for x in range(2, 14):
            if (x - 7.5) ** 2 / 36 + (y - 8.5) ** 2 / 20 <= 1.0:
                ring = ((x % 3) - 1) ** 2 + ((y % 3) - 1) ** 2
                img.set(x, y, rgba((236, 236, 236), (1.0, 0.9, 0.8)[min(2, ring)] * (0.95 + 0.05 * rng.random())))


def icon_yarn(img):
    for y in range(3, 14):
        for x in range(3, 14):
            if (x - 8) ** 2 + (y - 8) ** 2 <= 25:
                img.set(x, y, rgba((236, 236, 236), 0.8 + 0.2 * (((x + 2 * y) % 4) / 3)))
    img.line(12, 12, 15, 15, rgba((220, 220, 220)))


def icon_cloth(img):
    for y in range(4, 13):
        for x in range(2, 14):
            img.set(x, y, rgba((214, 196, 160), 0.88 + 0.12 * ((x + y) % 2)))
    img.line(2, 8, 13, 8, rgba((168, 150, 116)))


def icon_butter(img):
    img.rect(2, 9, 13, 12, rgba((236, 232, 220)))
    img.rect(4, 6, 11, 10, rgba((246, 222, 120)))
    img.line(4, 6, 11, 6, rgba((252, 236, 160)))


def icon_cheese(img):
    for y in range(5, 13):
        for x in range(2, 14):
            if x - 2 >= (12 - y) * 1.2 - 1:
                img.set(x, y, rgba((236, 196, 84)))
    img.line(2, 12, 13, 12, rgba((200, 150, 60)))
    for p in ((8, 9), (11, 10), (10, 7)):
        img.set(*p, rgba((206, 164, 66)))


def coin_icon(img, metal, mark, radius):
    """A struck coin: lit from the upper left, a raised rim, and a mark in the middle."""
    cx = cy = 7.5
    for y in range(16):
        for x in range(16):
            d = math.hypot(x - cx, y - cy)
            if d > radius:
                continue
            light = 1.0 + 0.18 * ((cx - x) + (cy - y)) / (2 * radius)
            c = scale((*metal, 255), light * (0.82 if d > radius - 1.1 else 1.0))
            img.set(x, y, c)
    dark = scale((*metal, 255), 0.62)
    bright = scale((*metal, 255), 1.25)
    if mark == "wheat":
        img.line(8, 4, 8, 11, dark)
        for y in (5, 7, 9):
            img.set(7, y, dark)
            img.set(9, y, dark)
    elif mark == "cross":
        img.line(8, 4, 8, 11, dark)
        img.line(5, 7, 11, 7, dark)
    elif mark == "crown":
        # A bold crown in bright metal, outlined, on a sunk field: three points over a jewelled band.
        crown = {(4, 5), (4, 6), (4, 7), (7, 4), (8, 4), (7, 5), (8, 5), (7, 6), (8, 6), (11, 5), (11, 6), (11, 7)}
        crown |= {(x, y) for x in range(4, 12) for y in (8, 9, 10)}
        crown |= {(5, 7), (6, 7), (9, 7), (10, 7)}
        for y in range(3, 12):
            for x in range(3, 13):
                if (x, y) not in crown and any((x + dx, y + dy) in crown for dx in (-1, 0, 1) for dy in (-1, 0, 1)):
                    img.set(x, y, scale((*metal, 255), 0.5))
        for x, y in crown:
            img.set(x, y, bright if y < 8 else scale((*metal, 255), 1.1))
        img.set(7, 9, (190, 30, 40, 255))
        img.set(8, 9, (190, 30, 40, 255))
    else:
        for x, y in ((8, 4), (8, 5), (8, 10), (8, 11), (5, 8), (6, 8), (10, 8), (11, 8), (7, 7), (9, 7), (7, 9), (9, 9), (8, 8)):
            img.set(x, y, dark)
        img.set(8, 8, bright)
    img.set(int(cx - radius * 0.5), int(cy - radius * 0.5), bright)


# ---------------------------------------------------------------- block models

def box(frm, to, tex, faces_=("north", "south", "east", "west", "up", "down"), uv=None, tint=None):
    e = {"from": frm, "to": to, "faces": {}}
    for f in faces_:
        face = {"texture": "#" + tex}
        if uv:
            face["uv"] = uv
        e["faces"][f] = face
    return e


def model_compost(fill, ready):
    els = [box([0, 0, 0], [16, 1, 16], "slats"),
           box([0, 1, 0], [16, 14, 1], "slats"), box([0, 1, 15], [16, 14, 16], "slats"),
           box([0, 1, 1], [1, 14, 15], "slats"), box([15, 1, 1], [16, 14, 15], "slats")]
    if fill > 0:
        h = 1 + fill * 3
        els.append(box([1, 1, 1], [15, h, 15], "ready" if ready else "heap", ("up",)))
    return {"parent": "minecraft:block/block", "textures": {"particle": f"{MOD}:block/compost_bin_slats", "slats": f"{MOD}:block/compost_bin_slats",
            "heap": f"{MOD}:block/compost_heap", "ready": f"{MOD}:block/compost_ready"}, "elements": els}


def model_windrow(layers, stage):
    h = 1 + layers
    tex = f"{MOD}:block/hay_windrow_{stage}"
    return {"parent": "minecraft:block/block", "textures": {"particle": tex, "hay": tex},
            "elements": [{"from": [0, 0, 0], "to": [16, h, 16], "faces": {
                "up": {"texture": "#hay"}, "down": {"texture": "#hay", "cullface": "down"},
                "north": {"uv": [0, 16 - h, 16, 16], "texture": "#hay"}, "south": {"uv": [0, 16 - h, 16, 16], "texture": "#hay"},
                "east": {"uv": [0, 16 - h, 16, 16], "texture": "#hay"}, "west": {"uv": [0, 16 - h, 16, 16], "texture": "#hay"}}}]}


def model_spinning_wheel():
    t = {"particle": "minecraft:block/spruce_planks", "wood": "minecraft:block/spruce_planks", "wheel": f"{MOD}:block/spinning_wheel_wheel",
         "wool": "minecraft:block/white_wool"}
    return {"parent": "minecraft:block/block", "render_type": "minecraft:cutout", "textures": t, "elements": [
        box([2, 4, 6], [14, 6, 10], "wood"), box([2, 0, 6], [3, 4, 7], "wood"), box([13, 0, 6], [14, 4, 7], "wood"),
        box([2, 0, 9], [3, 4, 10], "wood"), box([13, 0, 9], [14, 4, 10], "wood"),
        box([10, 6, 7.5], [11, 9, 8.5], "wood"),
        {"from": [6, 5, 7.75], "to": [16, 15, 8.25], "rotation": {"origin": [8, 8, 8], "axis": "y", "angle": 0},
         "faces": {"north": {"uv": [0, 0, 16, 16], "texture": "#wheel"}, "south": {"uv": [0, 0, 16, 16], "texture": "#wheel"}}},
        box([3, 6, 7], [4, 13, 8], "wood"), box([2, 11, 6], [5, 15, 9], "wool")]}


def model_churn():
    t = {"particle": "minecraft:block/spruce_planks", "wood": "minecraft:block/spruce_planks", "band": "minecraft:block/iron_block",
         "stick": "minecraft:block/stripped_oak_log"}
    return {"parent": "minecraft:block/block", "textures": t, "elements": [
        box([4, 0, 4], [12, 12, 12], "wood"), box([3.75, 2, 3.75], [12.25, 3, 12.25], "band"), box([3.75, 9, 3.75], [12.25, 10, 12.25], "band"),
        box([3.5, 12, 3.5], [12.5, 13, 12.5], "wood"), box([7.5, 13, 7.5], [8.5, 18, 8.5], "stick"), box([6, 17, 7.5], [10, 18, 8.5], "stick")]}


def model_press():
    t = {"particle": "minecraft:block/oak_planks", "wood": "minecraft:block/oak_planks", "iron": "minecraft:block/iron_block",
         "hoop": "minecraft:block/stripped_birch_log", "cheese": f"{MOD}:block/cheese_rind"}
    return {"parent": "minecraft:block/block", "textures": t, "elements": [
        box([1, 0, 1], [15, 3, 15], "wood"), box([1, 3, 7], [3, 15, 9], "wood"), box([13, 3, 7], [15, 15, 9], "wood"),
        box([1, 13, 6], [15, 15, 10], "wood"), box([7, 8, 7], [9, 13, 9], "iron"), box([4, 7, 4], [12, 8, 12], "wood"),
        box([4, 3, 4], [12, 7, 12], "hoop"), box([5, 3.5, 5], [11, 6.5, 11], "cheese", ("up",))]}


def model_stall():
    t = {"particle": "minecraft:block/oak_planks", "wood": "minecraft:block/oak_planks", "post": "minecraft:block/stripped_oak_log",
         "awning": f"{MOD}:block/market_awning", "pumpkin": "minecraft:block/pumpkin_side", "melon": "minecraft:block/melon_side",
         "hay": "minecraft:block/hay_block_top", "crate": "minecraft:block/barrel_side"}
    return {"parent": "minecraft:block/block", "textures": t, "elements": [
        box([0, 0, 4], [16, 9, 12], "wood"), box([0, 9, 3], [16, 10, 12], "wood"),
        box([0, 0, 13], [1, 16, 14], "post"), box([15, 0, 13], [16, 16, 14], "post"),
        {"from": [-1, 15, -1], "to": [17, 16, 15], "rotation": {"origin": [8, 16, 14], "axis": "x", "angle": -22.5},
         "faces": {f: {"texture": "#awning"} for f in ("north", "south", "east", "west", "up", "down")}},
        box([1, 10, 4], [5, 13, 8], "crate"), box([1.5, 13, 4.5], [4.5, 14, 7.5], "hay", ("up",)),
        box([6, 10, 4], [10, 14, 8], "pumpkin"), box([11, 10, 4], [15, 13, 8], "melon")]}


def facing_variants(name):
    return {f"facing={d}": {"model": f"{MOD}:block/{name}", "y": r} for d, r in (("north", 0), ("east", 90), ("south", 180), ("west", 270))}


# ---------------------------------------------------------------- the village stall structure (NBT)

def _nbt_string(s):
    b = s.encode("utf-8")
    return struct.pack(">H", len(b)) + b


def _nbt_payload(tag_type, value):
    if tag_type == 3:
        return struct.pack(">i", value)
    if tag_type == 8:
        return _nbt_string(value)
    if tag_type == 10:
        out = b""
        for k, (t, v) in value.items():
            out += bytes([t]) + _nbt_string(k) + _nbt_payload(t, v)
        return out + b"\x00"
    if tag_type == 9:
        elem_type, items = value
        out = bytes([elem_type]) + struct.pack(">i", len(items))
        for it in items:
            out += _nbt_payload(elem_type, it)
        return out
    raise ValueError(tag_type)


def structure_nbt(size, blocks):
    """blocks: list of (x, y, z, name, props, nbt_or_None). Writes a gzipped structure template."""
    palette = []
    keys = {}
    entries = []
    for x, y, z, name, props, nbt in blocks:
        key = (name, tuple(sorted(props.items())))
        if key not in keys:
            keys[key] = len(palette)
            st = {"Name": (8, name)}
            if props:
                st["Properties"] = (10, {k: (8, v) for k, v in props.items()})
            palette.append(st)
        e = {"pos": (9, (3, [x, y, z])), "state": (3, keys[key])}
        if nbt:
            e["nbt"] = (10, nbt)
        entries.append(e)
    root = {"DataVersion": (3, 3955), "size": (9, (3, list(size))), "palette": (9, (10, palette)),
            "blocks": (9, (10, entries)), "entities": (9, (10, []))}
    raw = b"\x0a" + _nbt_string("") + _nbt_payload(10, root)
    buf = io.BytesIO()
    with gzip.GzipFile(fileobj=buf, mode="wb", mtime=0) as f:
        f.write(raw)
    return buf.getvalue()


def stall_structure():
    """A 5x4x5 market stall: a stall under a striped awning, produce, and corner posts, entered from the street (north)."""
    b = []
    for x in range(5):
        for z in range(5):
            if (x, z) == (2, 0):
                b.append((x, 0, z, "minecraft:jigsaw", {"orientation": "north_up"},
                          {"name": (8, "minecraft:building_entrance"), "target": (8, "minecraft:empty"), "pool": (8, "minecraft:empty"),
                           "final_state": (8, "minecraft:dirt_path"), "joint": (8, "rollable"), "id": (8, "minecraft:jigsaw"),
                           "placement_priority": (3, 0), "selection_priority": (3, 0)}))
            else:
                b.append((x, 0, z, "minecraft:dirt_path" if z < 2 else "minecraft:coarse_dirt", {}, None))
    b.append((2, 1, 2, f"{MOD}:market_stall", {"facing": "north"}, None))
    b.append((1, 1, 2, "minecraft:hay_block", {"axis": "x"}, None))
    b.append((3, 1, 2, "minecraft:barrel", {"facing": "up", "open": "false"}, None))
    b.append((1, 1, 4, f"{MOD}:compost_bin", {"fill": "2", "ready": "false"}, None))
    b.append((3, 1, 4, "minecraft:pumpkin", {}, None))
    for x, z in ((0, 1), (4, 1), (0, 4), (4, 4)):
        for y in (1, 2):
            b.append((x, y, z, "minecraft:oak_fence", {}, None))
    for x in range(5):
        for z in range(1, 5):
            b.append((x, 3, z, "minecraft:red_wool" if x % 2 == 0 else "minecraft:white_wool", {}, None))
    for x in range(1, 4):
        for z in range(1, 4):
            for y in (1, 2):
                if not any(e[0] == x and e[1] == y and e[2] == z for e in b):
                    b.append((x, y, z, "minecraft:air", {}, None))
    return structure_nbt((5, 4, 5), b)


# ---------------------------------------------------------------- everything

def _write_json(root, parts, obj):
    path = os.path.join(root, *parts)
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8", newline="\n") as f:
        json.dump(obj, f, indent=2)
        f.write("\n")


def generate(lang, h):
    save, model, blockstate, loot, data = h["save"], h["model"], h["blockstate"], h["loot"], h["data"]
    breed_feature_textures(save)
    save(merchant_texture(), "entity", "farm_merchant.png")
    lang[f"entity.{MOD}.farm_merchant"] = "Travelling Farm Merchant"

    save(tex_slats(), "block", "compost_bin_slats.png")
    save(tex_speckle((110, 84, 50), [(140, 170, 70), (180, 160, 90), (70, 50, 30)], "compost_heap"), "block", "compost_heap.png")
    save(tex_speckle((52, 38, 26), [(36, 26, 18), (74, 56, 36)], "compost_ready", 30), "block", "compost_ready.png")
    save(tex_hay((96, 150, 58), "hay_fresh"), "block", "hay_windrow_fresh.png")
    save(tex_hay((140, 160, 70), "hay_wilted"), "block", "hay_windrow_wilted.png")
    save(tex_hay((186, 170, 84), "hay_curing"), "block", "hay_windrow_curing.png")
    save(tex_hay((214, 190, 100), "hay_dry"), "block", "hay_windrow_dry.png")
    save(tex_hay((112, 116, 84), "hay_spoiled", 18), "block", "hay_windrow_spoiled.png")
    save(tex_awning(), "block", "market_awning.png")
    save(tex_wheel(), "block", "spinning_wheel_wheel.png")
    save(tex_speckle((236, 196, 84), [(206, 164, 66)], "cheese_rind", 12), "block", "cheese_rind.png")

    # Compost bin.
    variants = {}
    for fill in range(5):
        for ready in (False, True):
            name = f"compost_bin_{fill}" + ("_ready" if ready else "")
            model(f"block/{name}", model_compost(fill, ready))
            variants[f"fill={fill},ready={'true' if ready else 'false'}"] = {"model": f"{MOD}:block/{name}"}
    blockstate("compost_bin", {"variants": variants})
    model("item/compost_bin", {"parent": f"{MOD}:block/compost_bin_2"})
    lang[f"block.{MOD}.compost_bin"] = "Compost Bin"

    # Hay windrows.
    variants = {}
    stages = ["fresh", "wilted", "curing", "dry"]
    for layers in range(1, 5):
        for stage in stages + ["spoiled"]:
            model(f"block/hay_windrow_{layers}_{stage}", model_windrow(layers, stage))
        for dry in range(4):
            for spoiled in ("false", "true"):
                stage = "spoiled" if spoiled == "true" else stages[dry]
                variants[f"dryness={dry},layers={layers},spoiled={spoiled}"] = {"model": f"{MOD}:block/hay_windrow_{layers}_{stage}"}
    blockstate("hay_windrow", {"variants": variants})
    lang[f"block.{MOD}.hay_windrow"] = "Hay Windrow"

    def windrow_entry(item, cond, per_layer):
        return {"type": "minecraft:alternatives", "children": [
            {"type": "minecraft:item", "name": f"{MOD}:{item}", "functions": [{"function": "minecraft:set_count", "count": n * per_layer}],
             "conditions": [{"condition": "minecraft:block_state_property", "block": f"{MOD}:hay_windrow", "properties": {**cond, "layers": str(n)}}]}
            for n in range(1, 5)]}
    loot("hay_windrow", {"type": "minecraft:block", "pools": [
        {"rolls": 1, "bonus_rolls": 0, "entries": [windrow_entry("mouldy_hay", {"spoiled": "true"}, 1)]},
        {"rolls": 1, "bonus_rolls": 0, "entries": [windrow_entry("dried_hay", {"spoiled": "false", "dryness": "3"}, 2)]},
        {"rolls": 1, "bonus_rolls": 0, "entries": [{"type": "minecraft:alternatives", "children": [
            {"type": "minecraft:item", "name": f"{MOD}:cut_grass", "functions": [{"function": "minecraft:set_count", "count": n}],
             "conditions": [{"condition": "minecraft:block_state_property", "block": f"{MOD}:hay_windrow",
                             "properties": {"spoiled": "false", "dryness": str(dry), "layers": str(n)}}]}
            for n in range(1, 5) for dry in range(3)]}]}]})

    # Stations and the stall.
    for name, label, mdl in (("spinning_wheel", "Spinning Wheel", model_spinning_wheel()), ("butter_churn", "Butter Churn", model_churn()),
                             ("market_stall", "Market Stall", model_stall())):
        model(f"block/{name}", mdl)
        blockstate(name, {"variants": facing_variants(name)})
        model(f"item/{name}", {"parent": f"{MOD}:block/{name}"})
        loot(name, h["self_drop"](name))
        lang[f"block.{MOD}.{name}"] = label
    model("block/cheese_press", model_press())
    blockstate("cheese_press", {"variants": {"": {"model": f"{MOD}:block/cheese_press"}}})
    model("item/cheese_press", {"parent": f"{MOD}:block/cheese_press"})
    loot("cheese_press", h["self_drop"]("cheese_press"))
    lang[f"block.{MOD}.cheese_press"] = "Cheese Press"
    loot("compost_bin", h["self_drop"]("compost_bin"))

    # Items.
    items = [("cut_grass", "Cut Grass", icon(icon_cut_grass), False), ("dried_hay", "Dried Hay", icon(icon_hay((214, 190, 100))), False),
             ("mouldy_hay", "Mouldy Hay", icon(icon_hay((120, 124, 92), True)), False), ("fleece", "Fleece", icon(icon_fleece), True),
             ("yarn", "Yarn", icon(icon_yarn), True), ("woollen_cloth", "Woollen Cloth", icon(icon_cloth), False),
             ("butter", "Butter", icon(icon_butter), False), ("cheese", "Cheese", icon(icon_cheese), False)]
    for name, label, img, tinted in items:
        save(img, "item", name + ".png")
        model(f"item/{name}", {"parent": "minecraft:item/generated", "textures": {"layer0": f"{MOD}:item/{name}"}})
        lang[f"item.{MOD}.{name}"] = label

    # Recipes.
    def shaped(name, pattern, key, result, count=1):
        _write_json(data, [MOD, "recipe", name + ".json"], {"type": "minecraft:crafting_shaped", "category": "misc", "pattern": pattern,
                    "key": key, "result": {"id": result, "count": count}})
    planks = {"tag": "minecraft:planks"}
    stick = {"item": "minecraft:stick"}
    shaped("compost_bin", ["S S", "S S", "PSP"], {"S": stick, "P": planks}, f"{MOD}:compost_bin")
    shaped("spinning_wheel", ["S S", "SWS", "PPP"], {"S": stick, "W": {"item": "minecraft:string"}, "P": planks}, f"{MOD}:spinning_wheel")
    shaped("butter_churn", ["PSP", "P P", "PIP"], {"S": stick, "P": planks, "I": {"item": "minecraft:iron_nugget"}}, f"{MOD}:butter_churn")
    shaped("cheese_press", ["PIP", "P P", "SSS"], {"P": planks, "I": {"item": "minecraft:iron_ingot"}, "S": {"tag": "minecraft:wooden_slabs"}},
           f"{MOD}:cheese_press")
    shaped("market_stall", ["WWW", "PEP", "P P"], {"W": {"tag": "minecraft:wool"}, "E": {"item": "minecraft:emerald"}, "P": planks},
           f"{MOD}:market_stall")
    shaped("hay_from_dried_hay", ["HHH", "HHH", "HHH"], {"H": {"item": f"{MOD}:dried_hay"}}, "minecraft:hay_block")
    _write_json(data, [MOD, "recipe", "woollen_cloth.json"], {"type": "minecraft:crafting_shapeless", "category": "misc",
                "ingredients": [{"item": f"{MOD}:yarn"}] * 4, "result": {"id": f"{MOD}:woollen_cloth", "count": 1}})

    # Money, in steps of nine.
    coins = [("copper_penny", "Copper Penny", (196, 112, 64), "wheat", 5.0), ("silver_shilling", "Silver Shilling", (200, 204, 212), "cross", 5.5),
             ("gold_crown", "Gold Crown", (236, 192, 72), "crown", 6.0), ("gold_sovereign", "Gold Sovereign", (248, 208, 88), "star", 6.8)]
    for name, label, metal, mark, radius in coins:
        save(icon(lambda img, m=metal, k=mark, r=radius: coin_icon(img, m, k, r)), "item", name + ".png")
        model(f"item/{name}", {"parent": "minecraft:item/generated", "textures": {"layer0": f"{MOD}:item/{name}"}})
        lang[f"item.{MOD}.{name}"] = label
    for (small, _, _, _, _), (big, _, _, _, _) in zip(coins, coins[1:]):
        _write_json(data, [MOD, "recipe", f"{big}_from_{small}.json"], {"type": "minecraft:crafting_shapeless", "category": "misc",
                    "ingredients": [{"item": f"{MOD}:{small}"}] * 9, "result": {"id": f"{MOD}:{big}", "count": 1}})
        _write_json(data, [MOD, "recipe", f"{small}_from_{big}.json"], {"type": "minecraft:crafting_shapeless", "category": "misc",
                    "ingredients": [{"item": f"{MOD}:{big}"}], "result": {"id": f"{MOD}:{small}", "count": 9}})

    # The village stall.
    path = os.path.join(data, MOD, "structure", "village", "market_stall.nbt")
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "wb") as f:
        f.write(stall_structure())

    # Dairy nutrition (percent daily value per serving).
    from data_tables import FOOD_NUTRITION, p
    FOOD_NUTRITION[f"{MOD}:butter"] = p(0, 1, 40, 0, 14, 0, 0, 1)
    FOOD_NUTRITION[f"{MOD}:cheese"] = p(1, 25, 30, 0, 10, 0, 1, 40)
