"""Assets for the workshop blocks, implements and instruments: soil lab, samples, spreader, microscope."""
from pixelart import Img, mix, rng_for, scale

MOD = "tilth"


def _planks(name, base, dark):
    img = Img(16, 16)
    rng = rng_for(name)
    for y in range(16):
        board = y // 4
        tone = 0.92 + 0.06 * (board % 2)
        for x in range(16):
            grain = 0.95 + 0.08 * rng.random()
            img.set(x, y, scale(base, tone * grain))
        if y % 4 == 3:
            img.line(0, y, 15, y, dark)
    for _ in range(5):
        x, y = rng.randrange(16), rng.randrange(16)
        img.set(x, y, scale(dark, 1.1))
    return img


def _bench_top(name):
    img = _planks(name, (118, 86, 58), (78, 54, 34))
    for y in range(16):
        img.set(0, y, (70, 48, 30, 255))
        img.set(15, y, (70, 48, 30, 255))
    return img


def _glass():
    img = Img(16, 16, (205, 230, 238, 90))
    for y in range(16):
        img.set(0, y, (230, 245, 250, 170))
        img.set(15, y, (160, 190, 200, 150))
    for y in range(2, 9):
        img.set(3, y, (255, 255, 255, 190))
    return img


def _liquid(color):
    img = Img(16, 16)
    rng = rng_for("liquid", *color)
    for y in range(16):
        for x in range(16):
            img.set(x, y, scale((*color, 230), 0.9 + 0.15 * rng.random() + (0.1 if y < 2 else 0)))
    return img


def _paper():
    img = Img(16, 16, (236, 230, 212, 255))
    for y in range(3, 15, 3):
        img.line(2, y, 13, y, (150, 150, 170, 255))
    img.line(2, 2, 8, 2, (60, 60, 80, 255))
    return img


def _box(frm, to, tex, faces=("north", "south", "east", "west", "up", "down"), uv_scale=1.0):
    out = {}
    for f in faces:
        out[f] = {"texture": "#" + tex}
    return {"from": frm, "to": to, "faces": out}


def lab_model(liquid_tex):
    legs = [_box([x, 0, z], [x + 2, 11, z + 2], "wood") for x, z in ((1, 1), (13, 1), (1, 13), (13, 13))]
    tubes = [_box([x, 14, 10], [x + 1, 17.5, 11], "glass") for x in (9.5, 11.5, 13)]
    tube_liquid = [_box([x + 0.2, 14, 10.2], [x + 0.8, 16, 10.8], "liquid") for x in (9.5, 11.5, 13)]
    return {
        "parent": "minecraft:block/block",
        "render_type": "minecraft:translucent",
        "textures": {"particle": f"{MOD}:block/soil_lab_top", "wood": f"{MOD}:block/soil_lab_wood", "top": f"{MOD}:block/soil_lab_top",
                     "glass": f"{MOD}:block/soil_lab_glass", "liquid": f"{MOD}:block/{liquid_tex}", "paper": f"{MOD}:block/soil_lab_paper"},
        "elements": [
            {"from": [0, 11, 0], "to": [16, 13, 16], "faces": {
                "up": {"texture": "#top"}, "down": {"texture": "#wood"}, "north": {"texture": "#wood", "uv": [0, 0, 16, 2]},
                "south": {"texture": "#wood", "uv": [0, 0, 16, 2]}, "east": {"texture": "#wood", "uv": [0, 0, 16, 2]},
                "west": {"texture": "#wood", "uv": [0, 0, 16, 2]}}},
            *legs,
            _box([1, 3, 1], [15, 4, 15], "wood"),
            _box([3, 13, 4], [7, 17, 8], "glass"),
            _box([3.5, 13, 4.5], [6.5, 15.5, 7.5], "liquid"),
            _box([4.5, 17, 5.5], [5.5, 19, 6.5], "glass"),
            _box([9, 13, 9.5], [14.5, 14, 11.5], "wood"),
            *tubes, *tube_liquid,
            {"from": [8, 13, 2], "to": [14, 13.2, 7], "faces": {"up": {"texture": "#paper"}}},
        ],
    }


def sample_icon():
    img = Img()
    burlap = (176, 146, 98, 255)
    for y in range(5, 15):
        half = 3 + min(y - 5, 3)
        for x in range(8 - half, 8 + half):
            shade = 0.85 + 0.1 * ((x + y) % 2) - 0.12 * (x > 8)
            img.set(x, y, scale(burlap, shade))
    img.ellipse(7.5, 5.5, 3.2, 1.6, (96, 70, 46, 255))
    for x in (6, 8, 10):
        img.set(x, 5, (120, 90, 60, 255))
    img.line(5, 7, 10, 7, (120, 92, 60, 255))
    img.outline(0.45)
    return img


def generate(lang, h):
    save, model, blockstate, loot, self_drop = h["save"], h["model"], h["blockstate"], h["loot"], h["self_drop"]
    save(_planks("lab_wood", (150, 112, 74), (98, 70, 44)), "block", "soil_lab_wood.png")
    save(_bench_top("lab_top"), "block", "soil_lab_top.png")
    save(_glass(), "block", "soil_lab_glass.png")
    save(_liquid((122, 92, 58)), "block", "soil_lab_liquid.png")
    save(_liquid((70, 170, 150)), "block", "soil_lab_liquid_working.png")
    save(_paper(), "block", "soil_lab_paper.png")
    model("block/soil_lab", lab_model("soil_lab_liquid"))
    model("block/soil_lab_working", lab_model("soil_lab_liquid_working"))
    variants = {}
    for facing, rot in (("north", 0), ("east", 90), ("south", 180), ("west", 270)):
        for working in ("false", "true"):
            m = {"model": f"{MOD}:block/soil_lab" + ("_working" if working == "true" else "")}
            if rot:
                m["y"] = rot
            variants[f"facing={facing},working={working}"] = m
    blockstate("soil_lab", {"variants": variants})
    model("item/soil_lab", {"parent": f"{MOD}:block/soil_lab"})
    loot("soil_lab", self_drop("soil_lab"))
    lang[f"block.{MOD}.soil_lab"] = "Soil Lab"

    save(sample_icon(), "item", "soil_sample.png")
    model("item/soil_sample", {"parent": "minecraft:item/generated", "textures": {"layer0": f"{MOD}:item/soil_sample"}})
    lang[f"item.{MOD}.soil_sample"] = "Soil Sample"
    generate_spreader(lang, h)
    generate_microscope(lang, h)
    generate_instruments(lang, h)
    generate_farmyard(lang, h)


# ---------------------------------------------------------------- fertiliser spreader

RED = (168, 52, 40, 255)
WOOD = (150, 108, 62, 255)
IRON = (150, 152, 158, 255)


def spreader_texture():
    img = Img(64, 64)
    rng = rng_for("spreader")
    for y in range(64):
        for x in range(64):
            if y < 22 and x < 60:
                base, f = RED, 0.9 + 0.1 * ((x // 4 + y) % 3) / 2 + rng.uniform(-0.04, 0.04)
                if y in (0, 13, 21) or x in (0, 14, 29, 45, 59):
                    base, f = (110, 34, 28, 255), 1.0
            elif y < 40 and x < 36:
                base, f = WOOD, 0.9 + rng.uniform(-0.05, 0.06)
            elif y < 40:
                base, f = IRON, 0.85 + rng.uniform(-0.05, 0.08)
            elif x < 24 and y < 60:
                cx, cy = (x % 12) - 5.5 if x >= 4 else 0, (y - 40) % 20
                rim = x in (0, 1, 2, 3) or (4 <= x < 24 and ((x - 4) % 10 in (0, 9) or (y - 50) in (0, 9) or y in (40, 49, 59)))
                base, f = ((70, 70, 74, 255), 1.0) if rim else (WOOD, 0.8 + rng.uniform(-0.05, 0.05))
                if not rim and (x - 4) % 10 in (4, 5):
                    base, f = WOOD, 0.65
            else:
                base, f = IRON, 0.8 + rng.uniform(-0.05, 0.1)
            img.px[y][x] = scale(base, f)
    return img


def spreader_icon():
    img = Img()
    img.rect(2, 4, 12, 9, RED)
    for x in range(2, 13):
        img.set(x, 4, scale(RED, 1.25))
        img.set(x, 9, scale(RED, 0.7))
    img.rect(3, 5, 11, 5, (90, 66, 40, 255))
    img.line(12, 8, 15, 6, scale(WOOD, 0.9))
    img.ellipse(5.5, 11.5, 2.6, 2.6, (60, 60, 64, 255), shade=0.2)
    img.ellipse(5.5, 11.5, 1.4, 1.4, scale(WOOD, 0.9), shade=0.2)
    img.rect(1, 10, 3, 10, IRON)
    img.outline(0.45)
    return img


def generate_spreader(lang, h):
    h["save"](spreader_texture(), "entity", "spreader.png")
    h["save"](spreader_icon(), "item", "spreader.png")
    h["model"]("item/spreader", {"parent": "minecraft:item/generated", "textures": {"layer0": f"{MOD}:item/spreader"}})
    lang[f"item.{MOD}.spreader"] = "Fertilizer Spreader"
    lang[f"entity.{MOD}.spreader"] = "Fertilizer Spreader"


# ---------------------------------------------------------------- microscope and plant samples

BRASS = (196, 160, 72, 255)


def microscope_icon():
    img = Img()
    img.rect(3, 13, 12, 14, (60, 60, 66, 255))
    img.rect(4, 12, 11, 12, (90, 90, 98, 255))
    img.line(10, 12, 10, 6, (70, 70, 76, 255))
    img.line(11, 12, 11, 6, (90, 90, 98, 255))
    img.rect(4, 9, 9, 9, (120, 120, 128, 255))
    for i in range(7):
        img.set(4 + i // 2, 7 - i, scale(BRASS, 1.0 + 0.04 * (i % 2)))
        img.set(5 + i // 2, 7 - i, scale(BRASS, 0.8))
    img.rect(6, 0, 8, 1, scale(BRASS, 1.2))
    img.set(4, 8, (210, 230, 240, 255))
    img.outline(0.45)
    return img


def plant_sample_icon():
    img = Img()
    img.rect(2, 4, 13, 11, (210, 230, 238, 160))
    img.ellipse(7.5, 7.5, 4.2, 2.4, (80, 150, 60, 255), shade=0.35)
    img.line(4, 8, 11, 7, (60, 120, 45, 255))
    img.set(9, 6, (150, 110, 50, 255))
    img.set(6, 9, (150, 110, 50, 255))
    img.outline(0.45)
    return img


def generate_microscope(lang, h):
    import microscope
    microscope.generate(h["afflictions"], h["save"])
    for name, icon, label in (("microscope", microscope_icon, "Microscope"), ("plant_sample", plant_sample_icon, "Plant Sample")):
        h["save"](icon(), "item", name + ".png")
        h["model"](f"item/{name}", {"parent": "minecraft:item/generated", "textures": {"layer0": f"{MOD}:item/{name}"}})
        lang[f"item.{MOD}.{name}"] = label


# ---------------------------------------------------------------- survey ledger and field instruments

def survey_icon():
    img = Img()
    cover = (52, 96, 60, 255)
    img.rect(3, 2, 12, 14, cover)
    img.rect(4, 3, 4, 13, scale(cover, 0.7))
    img.rect(12, 3, 12, 13, (232, 224, 200, 255))
    img.rect(6, 6, 10, 8, (150, 110, 70, 255))
    img.line(6, 6, 10, 6, (120, 86, 52, 255))
    img.rect(6, 10, 10, 10, (200, 180, 120, 255))
    img.outline(0.45)
    return img


def thermometer_icon():
    img = Img()
    glass = (226, 236, 240, 255)
    for y in range(1, 12):
        img.set(7, y, glass)
        img.set(8, y, scale(glass, 0.85))
    for y in range(6, 12):
        img.set(7, y, (210, 40, 40, 255))
    img.ellipse(7.5, 12.5, 2.2, 2.2, (200, 30, 30, 255))
    for y in (3, 5, 7, 9):
        img.set(9, y, (90, 90, 96, 255))
    img.outline(0.45)
    return img


def moisture_meter_icon():
    img = Img()
    img.ellipse(7.5, 5.5, 4.5, 4.5, (60, 64, 72, 255), shade=0.2)
    img.ellipse(7.5, 5.5, 3.3, 3.3, (232, 230, 220, 255), shade=0.1)
    img.line(5, 7, 7, 5, (40, 110, 200, 255))
    img.line(7, 5, 9, 4, (40, 110, 200, 255))
    img.set(5, 3, (200, 60, 50, 255))
    img.set(10, 3, (60, 150, 60, 255))
    img.rect(5, 10, 5, 15, (170, 172, 178, 255))
    img.rect(10, 10, 10, 15, (170, 172, 178, 255))
    img.outline(0.45)
    return img


def generate_instruments(lang, h):
    for name, icon, label in (("soil_survey", survey_icon, "Soil Survey"), ("thermometer", thermometer_icon, "Thermometer"),
                              ("moisture_meter", moisture_meter_icon, "Soil Moisture Meter")):
        h["save"](icon(), "item", name + ".png")
        h["model"](f"item/{name}", {"parent": "minecraft:item/generated", "textures": {"layer0": f"{MOD}:item/{name}"}})
        lang[f"item.{MOD}.{name}"] = label


# ---------------------------------------------------------------- peat fuel, alfalfa bales, manure and slurry

DATA_ROOT = None  # set by generate_farmyard


def _write(path_parts, obj):
    import json
    import os
    path = os.path.join(DATA_ROOT, *path_parts)
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8", newline="\n") as f:
        json.dump(obj, f, indent=2, ensure_ascii=False)
        f.write("\n")


def cut_peat_icon():
    img = Img()
    rng = rng_for("cut_peat")
    img.rect(3, 5, 12, 11, (66, 46, 30, 255))
    for _ in range(26):
        x, y = rng.randrange(3, 13), rng.randrange(5, 12)
        img.set(x, y, (92, 68, 42, 255) if rng.random() < 0.6 else (48, 34, 22, 255))
    for x in range(3, 13, 3):
        img.line(x, 5, x + 1, 11, (110, 92, 48, 255))
    img.outline(0.45)
    return img


def peat_brick_icon():
    img = Img()
    img.rect(2, 6, 13, 10, (58, 40, 30, 255))
    img.line(2, 6, 13, 6, (84, 62, 44, 255))
    img.line(2, 10, 13, 10, (40, 28, 20, 255))
    for x in (5, 9):
        img.set(x, 8, (40, 28, 20, 255))
    img.outline(0.45)
    return img


def slurry_bucket_icon():
    img = Img()
    iron = (190, 192, 198, 255)
    for y in range(4, 14):
        half = 5 - (y - 4) // 4
        img.line(8 - half, y, 7 + half, y, scale(iron, 0.85 + 0.03 * (y % 2)))
    img.rect(4, 4, 11, 6, (96, 72, 34, 255))
    img.set(6, 5, (128, 100, 52, 255))
    img.set(9, 4, (70, 52, 24, 255))
    img.line(3, 3, 12, 3, iron)
    img.outline(0.45)
    return img


ALFALFA_TONES = [(84, 116, 46), (98, 132, 52), (108, 144, 58), (118, 154, 63), (130, 165, 70)]
TWINE = [(140, 58, 40), (150, 64, 44)]


def bale_side():
    """Pressed alfalfa seen from the side: stalks run along the bale, two twine bands wrap round it."""
    img = Img(16, 16)
    rng = rng_for("alfalfa_bale_side")
    for x in range(16):
        tone = rng.randrange(1, 5)
        for y in range(16):
            if rng.random() < 0.18:
                tone = max(1, min(4, tone + rng.choice((-1, 1))))
            img.set(x, y, (*ALFALFA_TONES[tone], 255))
        # The odd darker gap between stalks.
        if rng.random() < 0.35:
            y0 = rng.randrange(16)
            img.line(x, y0, x, min(15, y0 + rng.randrange(1, 4)), (*ALFALFA_TONES[0], 255))
    for y in (3, 12):
        for x in range(16):
            img.set(x, y, (*TWINE[(x // 2) % 2], 255))
    return img


def bale_top():
    """The cut end of the bale, like a vanilla hay bale end: a mottle of chopped stalk ends, no twine."""
    img = Img(16, 16)
    rng = rng_for("alfalfa_bale_top")
    for y in range(16):
        for x in range(16):
            img.set(x, y, (*ALFALFA_TONES[2], 255))
    for _ in range(70):
        x, y = rng.randrange(16), rng.randrange(16)
        tone = rng.choice((1, 1, 3, 3, 3, 4))
        w, h = rng.choice(((1, 1), (2, 1), (1, 2), (2, 2)))
        for dy in range(h):
            for dx in range(w):
                img.set((x + dx) % 16, (y + dy) % 16, (*ALFALFA_TONES[tone], 255))
    for _ in range(14):
        img.set(rng.randrange(16), rng.randrange(16), (*ALFALFA_TONES[0], 255))
    return img


def manure_texture():
    img = Img(16, 16)
    rng = rng_for("manure_layer")
    for y in range(16):
        for x in range(16):
            img.set(x, y, scale((86, 62, 34, 255), 0.75 + 0.35 * rng.random()))
    for _ in range(14):
        x, y = rng.randrange(16), rng.randrange(16)
        img.line(x, y, min(15, x + rng.randrange(1, 4)), y, (176, 152, 82, 255))
    for _ in range(8):
        img.set(rng.randrange(16), rng.randrange(16), (60, 72, 34, 255))
    return img


def male_features_texture():
    """One sheet for every male feature model (see MaleFeaturesLayer): bands of colour, each part's UVs in its band."""
    img = Img(32, 64)
    bands = [((206, 168, 60), 0, 4), ((222, 214, 190), 4, 12), ((244, 240, 230), 12, 16), ((200, 40, 36), 16, 24),
             ((40, 70, 60), 24, 64)]
    rng = rng_for("male_features")
    for color, top, bottom in bands:
        for y in range(top, bottom):
            for x in range(32):
                c = scale((*color, 255), 0.85 + 0.25 * rng.random())
                if top == 24 and (x + y) % 5 == 0:
                    c = (60, 110, 150, 255)
                img.set(x, y, c)
    return img


def layer_model(n):
    h = 2 * n
    tex = f"{MOD}:block/manure_layer"
    side = {"uv": [0, 16 - h, 16, 16], "texture": "#texture"}
    up = {"uv": [0, 0, 16, 16], "texture": "#texture"}
    if n == 8:
        up["cullface"] = "up"
    return {"parent": "minecraft:block/block", "textures": {"particle": tex, "texture": tex},
            "elements": [{"from": [0, 0, 0], "to": [16, h, 16], "faces": {
                "down": {"uv": [0, 0, 16, 16], "texture": "#texture", "cullface": "down"}, "up": up,
                "north": dict(side, cullface="north"), "south": dict(side, cullface="south"),
                "west": dict(side, cullface="west"), "east": dict(side, cullface="east")}}]}


def generate_farmyard(lang, h):
    global DATA_ROOT
    DATA_ROOT = h["data"]
    save, model, blockstate, loot = h["save"], h["model"], h["blockstate"], h["loot"]
    for name, icon, label in (("cut_peat", cut_peat_icon, "Cut Peat"), ("peat_brick", peat_brick_icon, "Peat Brick"),
                              ("slurry_bucket", slurry_bucket_icon, "Bucket of Slurry")):
        save(icon(), "item", name + ".png")
        model(f"item/{name}", {"parent": "minecraft:item/generated", "textures": {"layer0": f"{MOD}:item/{name}"}})
        lang[f"item.{MOD}.{name}"] = label

    # Peat breaks into four cut sods (the block itself only with silk touch), tilled peat likewise.
    silk = {"condition": "minecraft:match_tool", "predicate": {
        "predicates": {"minecraft:enchantments": [{"enchantments": "minecraft:silk_touch", "levels": {"min": 1}}]}}}
    for block in ("peat", "peat_farmland"):
        loot(block, {"type": "minecraft:block", "pools": [{"rolls": 1, "bonus_rolls": 0, "entries": [{"type": "minecraft:alternatives", "children": [
            {"type": "minecraft:item", "name": f"{MOD}:peat", "conditions": [silk]},
            {"type": "minecraft:item", "name": f"{MOD}:cut_peat", "functions": [
                {"function": "minecraft:set_count", "count": 4}, {"function": "minecraft:explosion_decay"}]}]}]}]})
    _write(["tilth", "recipe", "peat_brick.json"], {"type": "minecraft:smelting", "category": "misc",
                                                   "ingredient": {"item": f"{MOD}:cut_peat"}, "result": {"id": f"{MOD}:peat_brick"},
                                                   "experience": 0.1, "cookingtime": 200})
    _write(["neoforge", "data_maps", "item", "furnace_fuels.json"], {"values": {
        f"{MOD}:cut_peat": {"burn_time": 800}, f"{MOD}:peat_brick": {"burn_time": 1600}}})

    # Alfalfa hay bale.
    save(bale_side(), "block", "alfalfa_bale_side.png")
    save(bale_top(), "block", "alfalfa_bale_top.png")
    textures = {"end": f"{MOD}:block/alfalfa_bale_top", "side": f"{MOD}:block/alfalfa_bale_side"}
    model("block/alfalfa_bale", {"parent": "minecraft:block/cube_column", "textures": textures})
    model("block/alfalfa_bale_horizontal", {"parent": "minecraft:block/cube_column_horizontal", "textures": textures})
    blockstate("alfalfa_bale", {"variants": {
        "axis=y": {"model": f"{MOD}:block/alfalfa_bale"},
        "axis=z": {"model": f"{MOD}:block/alfalfa_bale_horizontal", "x": 90},
        "axis=x": {"model": f"{MOD}:block/alfalfa_bale_horizontal", "x": 90, "y": 90}}})
    model("item/alfalfa_bale", {"parent": f"{MOD}:block/alfalfa_bale"})
    loot("alfalfa_bale", h["self_drop"]("alfalfa_bale"))
    lang[f"block.{MOD}.alfalfa_bale"] = "Alfalfa Hay Bale"
    _write(["tilth", "recipe", "alfalfa_bale.json"], {"type": "minecraft:crafting_shaped", "category": "building",
                                                     "pattern": ["###", "###", "###"], "key": {"#": {"item": f"{MOD}:alfalfa"}},
                                                     "result": {"id": f"{MOD}:alfalfa_bale", "count": 1}})
    _write(["tilth", "recipe", "alfalfa_from_bale.json"], {"type": "minecraft:crafting_shapeless", "category": "misc",
                                                          "ingredients": [{"item": f"{MOD}:alfalfa_bale"}],
                                                          "result": {"id": f"{MOD}:alfalfa", "count": 9}})

    # Manure lying on the ground, in layers like snow; only a shovel gathers it.
    save(manure_texture(), "block", "manure_layer.png")
    variants = {}
    for n in range(1, 9):
        model(f"block/manure_layer_{n}", layer_model(n))
        variants[f"layers={n}"] = {"model": f"{MOD}:block/manure_layer_{n}"}
    blockstate("manure_layer", {"variants": variants})
    lang[f"block.{MOD}.manure_layer"] = "Manure"
    loot("manure_layer", {"type": "minecraft:block", "pools": [{"rolls": 1, "bonus_rolls": 0,
        "conditions": [{"condition": "minecraft:match_tool", "predicate": {"items": "#minecraft:shovels"}}],
        "entries": [{"type": "minecraft:alternatives", "children": [
            {"type": "minecraft:item", "name": f"{MOD}:manure", "functions": [{"function": "minecraft:set_count", "count": n}],
             "conditions": [{"condition": "minecraft:block_state_property", "block": f"{MOD}:manure_layer", "properties": {"layers": str(n)}}]}
            for n in range(1, 9)]}]}]})
    _write(["tilth", "recipe", "slurry_bucket.json"], {"type": f"{MOD}:consuming_shapeless", "category": "misc",
                                                      "ingredients": [{"item": "minecraft:water_bucket"}] + [{"item": f"{MOD}:manure"}] * 4,
                                                      "result": {"id": f"{MOD}:slurry_bucket", "count": 1}})
    save(male_features_texture(), "entity", "male_features.png")
