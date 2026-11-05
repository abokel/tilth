"""
The grain silo: one segment block that, built as a 2x2 tower five blocks high, draws as a single corrugated steel
silo with a concrete footing, an access hatch on each face, a rim, and a stepped conical roof.

Each formed segment knows its place (quad 0-3 in the footprint, tier 0-4 up the tower), so wall textures are drawn
32 pixels wide and split into left and right halves, and the roof is drawn as four quarters meeting at the peak.
"""
import json
import math
import os

from pixelart import Img, mix, rng_for, scale

MOD = "tilth"
STEEL = (170, 174, 178)
CONCRETE = (150, 147, 138)
RIM = (104, 108, 114)


def _rib(x):
    """Corrugation: a four-pixel wave of light and shade across the sheet."""
    return (1.06, 1.0, 0.88, 0.95)[x % 4]


def wall(tier):
    """A 32x16 run of wall for one tier: tier 0 has the footing and a hatch per face, tier 4 the top rim."""
    img = Img(32, 16)
    rng = rng_for("silo_wall", tier)
    for y in range(16):
        for x in range(32):
            c = scale((*STEEL, 255), _rib(x) * (0.97 + 0.06 * rng.random()))
            img.set(x, y, c)
    # Panel joints: the sheets of each ring overlap at the bottom, with a row of rivets.
    for x in range(32):
        img.set(x, 15, scale((*STEEL, 255), 0.7))
    for x in range(2, 32, 4):
        img.set(x, 14, scale((*STEEL, 255), 0.6))
    if tier == 0:
        for y in range(12, 16):
            for x in range(32):
                img.set(x, y, scale((*CONCRETE, 255), 0.92 + 0.12 * rng.random()))
        for x in range(32):
            img.set(x, 12, scale((*CONCRETE, 255), 1.12))
        # Access hatch, on the right-hand block of each face.
        hx = 20
        img.rect(hx, 2, hx + 7, 11, scale((*STEEL, 255), 0.62))
        img.rect(hx + 1, 3, hx + 6, 10, scale((*STEEL, 255), 0.88))
        for y in range(3, 11, 2):
            img.line(hx + 1, y, hx + 6, y, scale((*STEEL, 255), 0.8))
        img.set(hx + 5, 6, (60, 60, 64, 255))
        img.set(hx + 5, 7, (60, 60, 64, 255))
    if tier == 4:
        for y in range(0, 3):
            for x in range(32):
                img.set(x, y, scale((*RIM, 255), 1.0 if y == 1 else 0.85))
        for x in range(1, 32, 4):
            img.set(x, 1, (196, 198, 202, 255))
    # A ladder up the left-hand block of the north face is drawn on every tier (see ladder()).
    return img


def ladder(img):
    """Adds the ladder that runs up one side of the silo (drawn on the left half of one face only)."""
    out = Img(img.w, img.h)
    for y in range(img.h):
        for x in range(img.w):
            out.set(x, y, img.get(x, y))
    for y in range(16):
        out.set(4, y, (70, 72, 76, 255))
        out.set(9, y, (70, 72, 76, 255))
    for y in range(1, 16, 3):
        out.line(4, y, 9, y, (96, 98, 104, 255))
    return out


def half(img, right):
    out = Img(16, 16)
    for y in range(16):
        for x in range(16):
            out.set(x, y, img.get(x + (16 if right else 0), y))
    return out


def roof(quad):
    """A quarter of the conical roof seen from above; the peak is at this quarter's inner corner."""
    qx, qz = quad & 1, quad >> 1
    px, pz = (16 if qx == 0 else 0), (16 if qz == 0 else 0)
    img = Img(16, 16)
    rng = rng_for("silo_roof", quad)
    for y in range(16):
        for x in range(16):
            dx, dz = x + 0.5 - px, y + 0.5 - pz
            angle = math.atan2(dz, dx)
            r = math.hypot(dx, dz)
            rib = 1.1 if int((angle + math.pi) / (math.pi / 16)) % 2 else 0.9
            c = scale((*STEEL, 255), rib * (0.92 + 0.1 * min(1.0, r / 16)) * (0.97 + 0.05 * rng.random()))
            img.set(x, y, c)
    return img


def casing():
    """A loose segment: a bolted panel of corrugated sheet in a frame."""
    img = Img(16, 16)
    rng = rng_for("silo_casing")
    for y in range(16):
        for x in range(16):
            img.set(x, y, scale((*STEEL, 255), _rib(x) * (0.95 + 0.07 * rng.random())))
    frame = scale((*RIM, 255), 1.0)
    for i in range(16):
        img.set(i, 0, frame)
        img.set(i, 15, frame)
        img.set(0, i, frame)
        img.set(15, i, frame)
    for x, y in ((2, 2), (13, 2), (2, 13), (13, 13), (7, 2), (7, 13)):
        img.set(x, y, (210, 212, 216, 255))
    return img


def floor():
    img = Img(16, 16)
    rng = rng_for("silo_floor")
    for y in range(16):
        for x in range(16):
            img.set(x, y, scale((*CONCRETE, 255), 0.9 + 0.15 * rng.random()))
    return img


def _outer(quad):
    qx, qz = quad & 1, quad >> 1
    return {"west": qx == 0, "east": qx == 1, "north": qz == 0, "south": qz == 1}


def _right_half(face, quad):
    """Whether this block is on the viewer's right when the face is seen from outside."""
    qx, qz = quad & 1, quad >> 1
    return {"north": qx == 0, "south": qx == 1, "east": qz == 0, "west": qz == 1}[face]


def formed_model(quad, tier_name):
    tex = {"particle": f"{MOD}:block/silo_casing", "casing": f"{MOD}:block/silo_casing",
           "floor": f"{MOD}:block/silo_floor", "roof": f"{MOD}:block/silo_roof_{quad}", "rim": f"{MOD}:block/silo_rim"}
    faces = {}
    outer = _outer(quad)
    for face in ("north", "south", "east", "west"):
        if outer[face]:
            key = f"{face}_wall"
            side = "right" if _right_half(face, quad) else "left"
            ladder_tag = "_ladder" if face == "north" and side == "left" else ""
            tex[key] = f"{MOD}:block/silo_wall_{tier_name}_{side}{ladder_tag}"
            faces[face] = {"uv": [0, 0, 16, 16], "texture": "#" + key, "cullface": face}
        else:
            faces[face] = {"uv": [0, 0, 16, 16], "texture": "#casing", "cullface": face}
    faces["down"] = {"uv": [0, 0, 16, 16], "texture": "#floor" if tier_name == "bottom" else "#casing", "cullface": "down"}
    faces["up"] = {"uv": [0, 0, 16, 16], "texture": "#roof" if tier_name == "top" else "#casing", "cullface": "up"}
    elements = [{"from": [0, 0, 0], "to": [16, 16, 16], "faces": faces}]
    if tier_name == "top":
        # The roof: steps rising toward the centre of the silo, which is this block's inner corner.
        for k, inset in enumerate((1, 4, 7, 10, 13)):
            x0, x1 = (inset, 16) if outer["west"] else (0, 16 - inset)
            z0, z1 = (inset, 16) if outer["north"] else (0, 16 - inset)
            y0, y1 = 16 + 2 * k, 18 + 2 * k
            step = {"up": {"uv": [x0, z0, x1, z1], "texture": "#roof"}}
            for face in ("north", "south", "east", "west"):
                if outer[face]:
                    span = (x0, x1) if face in ("north", "south") else (z0, z1)
                    step[face] = {"uv": [span[0], 16 - (y1 - 16), span[1], 16 - (y0 - 16)], "texture": "#rim"}
            elements.append({"from": [x0, y0, z0], "to": [x1, y1, z1], "faces": step})
    return {"parent": "minecraft:block/block", "textures": tex, "elements": elements}


def generate(lang, h):
    save, model, blockstate, loot = h["save"], h["model"], h["blockstate"], h["loot"]
    save(casing(), "block", "silo_casing.png")
    save(floor(), "block", "silo_floor.png")
    rim = Img(16, 16)
    for y in range(16):
        for x in range(16):
            rim.set(x, y, scale((*RIM, 255), _rib(x)))
    save(rim, "block", "silo_rim.png")
    for quad in range(4):
        save(roof(quad), "block", f"silo_roof_{quad}.png")
    for tier, name in ((0, "bottom"), (2, "middle"), (4, "top")):
        w = wall(tier)
        for side, right in (("left", False), ("right", True)):
            save(half(w, right), "block", f"silo_wall_{name}_{side}.png")
            if side == "left":
                save(ladder(half(w, right)), "block", f"silo_wall_{name}_{side}_ladder.png")

    model("block/grain_silo", {"parent": "minecraft:block/cube_all", "textures": {"all": f"{MOD}:block/silo_casing"}})
    variants = {}
    for quad in range(4):
        for tier_name in ("bottom", "middle", "top"):
            model(f"block/grain_silo_{quad}_{tier_name}", formed_model(quad, tier_name))
        for tier in range(5):
            variants[f"formed=false,quad={quad},tier={tier}"] = {"model": f"{MOD}:block/grain_silo"}
            name = "bottom" if tier == 0 else "top" if tier == 4 else "middle"
            variants[f"formed=true,quad={quad},tier={tier}"] = {"model": f"{MOD}:block/grain_silo_{quad}_{name}"}
    blockstate("grain_silo", {"variants": variants})
    model("item/grain_silo", {"parent": f"{MOD}:block/grain_silo"})
    # The block keeps any grain it holds when broken, like a shulker box.
    loot("grain_silo", {"type": "minecraft:block", "pools": [{"rolls": 1, "bonus_rolls": 0,
        "conditions": [{"condition": "minecraft:survives_explosion"}],
        "entries": [{"type": "minecraft:item", "name": f"{MOD}:grain_silo", "functions": [
            {"function": "minecraft:copy_components", "source": "block_entity", "include": [f"{MOD}:silo_contents"]}]}]}]})
    lang[f"block.{MOD}.grain_silo"] = "Grain Silo Segment"
    path = os.path.join(h["data"], MOD, "recipe", "grain_silo.json")
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8", newline="\n") as f:
        json.dump({"type": "minecraft:crafting_shaped", "category": "building", "pattern": ["IPI", "P P", "IPI"],
                   "key": {"I": {"item": "minecraft:iron_ingot"}, "P": {"tag": "minecraft:planks"}},
                   "result": {"id": f"{MOD}:grain_silo", "count": 4}}, f, indent=2)
        f.write("\n")
