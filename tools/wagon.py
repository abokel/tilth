"""The horse-drawn farm wagon (texture, icon, recipe) and the bench microscope block (model, blockstate, loot)."""
import json
import os

from pixelart import Img, rng_for, scale

MOD = "tilth"
OAK = (150, 108, 62)
DARK = (92, 64, 38)
IRON = (70, 72, 78)
CANVAS = (226, 218, 196)


def wagon_texture():
    """128x128, laid out for WagonModel: plank wood everywhere, with spoked wheels, iron tyres and canvas hoops."""
    img = Img(128, 128)
    rng = rng_for("wagon")
    for y in range(128):
        for x in range(128):
            plank = (y // 4) % 2
            c = scale((*OAK, 255), (0.92 if plank else 1.0) * (0.94 + 0.1 * rng.random()))
            if y % 4 == 3:
                c = scale((*OAK, 255), 0.72)
            img.set(x, y, c)
    # Wheels: box at texOffs(100, 0), 2 wide, 10 high, 10 deep. Side faces are the wheel discs.
    for x0 in (100, 112):
        for j in range(10):
            for i in range(10):
                dx, dy = i - 4.5, j - 4.5
                r = (dx * dx + dy * dy) ** 0.5
                if r > 4.2:
                    c = IRON
                elif r < 1.3:
                    c = DARK
                elif abs(dx) < 0.7 or abs(dy) < 0.7 or abs(abs(dx) - abs(dy)) < 0.8:
                    c = scale((*OAK, 255), 0.85)[:3]
                elif r > 3.4:
                    c = scale((*OAK, 255), 0.8)[:3]
                else:
                    c = (40, 30, 22)
                img.set(x0 + i, 10 + j, (*c, 255))
    for x in range(110, 112):
        for y in range(0, 20):
            img.set(x, y, (*IRON, 255))
    for x in range(122, 124):
        for y in range(10, 20):
            img.set(x, y, (*IRON, 255))
    for x in range(112, 114):
        for y in range(0, 10):
            img.set(x, y, (*IRON, 255))
    # Axles and shaft darker; canvas hoops pale.
    for y in range(70, 74):
        for x in range(0, 48):
            img.set(x, y, scale((*DARK, 255), 0.9 + 0.1 * rng.random()))
    for y in range(80, 96):
        for x in range(0, 48):
            img.set(x, y, scale((*CANVAS, 255), 0.94 + 0.08 * rng.random()))
    return img


def wagon_icon():
    img = Img()
    for x in range(2, 14):
        for y in range(6, 10):
            img.set(x, y, scale((*OAK, 255), 0.85 if y == 9 else 1.0))
    img.line(1, 4, 14, 4, (*CANVAS, 255))
    img.line(1, 4, 1, 6, (*CANVAS, 255))
    img.line(14, 4, 14, 6, (*CANVAS, 255))
    for cx in (4, 11):
        for dy in range(-2, 3):
            for dx in range(-2, 3):
                if dx * dx + dy * dy <= 5:
                    img.set(cx + dx, 12 + dy, (*IRON, 255) if dx * dx + dy * dy >= 3 else (*DARK, 255))
    img.line(14, 8, 15, 7, (*DARK, 255))
    img.outline(0.45)
    return img


def microscope_model():
    """A brass and black bench microscope: foot, pillar, stage, arm and an angled eyepiece tube."""
    t = {"particle": "minecraft:block/black_concrete", "body": "minecraft:block/black_concrete",
         "brass": "minecraft:block/gold_block", "steel": "minecraft:block/iron_block", "glass": "minecraft:block/light_blue_stained_glass"}

    def box(frm, to, tex, rotation=None):
        e = {"from": frm, "to": to, "faces": {f: {"texture": "#" + tex} for f in ("north", "south", "east", "west", "up", "down")}}
        if rotation:
            e["rotation"] = rotation
        return e
    elements = [
        box([4, 0, 4], [12, 1, 12], "body"),
        box([9, 1, 9], [11, 9, 11], "body"),
        box([4, 5, 4], [11, 6, 11], "steel"),
        box([6.5, 6, 6.5], [8.5, 6.2, 8.5], "glass"),
        box([7, 9, 7], [11, 11, 11], "body"),
        box([6.5, 7, 6.5], [8.5, 13, 8.5], "brass", {"origin": [8, 10, 8], "axis": "x", "angle": -22.5}),
        box([6.25, 12.5, 6.25], [8.75, 14, 8.75], "body", {"origin": [8, 10, 8], "axis": "x", "angle": -22.5}),
        box([5, 2.5, 7], [6, 3.5, 8], "brass"),
    ]
    return {"parent": "minecraft:block/block", "textures": t, "elements": elements}


def generate(lang, h):
    save, model, blockstate, loot = h["save"], h["model"], h["blockstate"], h["loot"]
    save(wagon_texture(), "entity", "wagon.png")
    save(wagon_icon(), "item", "wagon.png")
    model("item/wagon", {"parent": "minecraft:item/generated", "textures": {"layer0": f"{MOD}:item/wagon"}})
    lang[f"item.{MOD}.wagon"] = "Farm Wagon"
    lang[f"entity.{MOD}.wagon"] = "Farm Wagon"
    path = os.path.join(h["data"], MOD, "recipe", "wagon.json")
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8", newline="\n") as f:
        json.dump({"type": "minecraft:crafting_shaped", "category": "transportation", "pattern": ["PCP", "IPI"],
                   "key": {"P": {"tag": "minecraft:planks"}, "C": {"item": "minecraft:chest"}, "I": {"item": "minecraft:iron_ingot"}},
                   "result": {"id": f"{MOD}:wagon", "count": 1}}, f, indent=2)
        f.write("\n")

    model("block/microscope", microscope_model())
    blockstate("microscope", {"variants": {f"facing={d}": {"model": f"{MOD}:block/microscope", "y": r}
                                           for d, r in (("north", 0), ("east", 90), ("south", 180), ("west", 270))}})
    loot("microscope", h["self_drop"]("microscope"))
    lang[f"block.{MOD}.microscope"] = "Microscope"
