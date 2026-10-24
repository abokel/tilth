#!/usr/bin/env python3
"""
Generates Tilth's resources: textures (procedural pixel art), block/item models, blockstates,
loot tables, tags, recipes, worldgen JSON, loot modifiers, the food nutrition data map, the
affliction (disease/pest/fungus) definitions and the English lang file.

Run from the repository root:  python tools/generate_assets.py
The Java code is the source of truth for ids; keep CROPS and the item lists below in sync with
com.claudacious.tilth.sim.crop.Crops and com.claudacious.tilth.registry.TilthItems.
"""
import json
import os
import shutil
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from pixelart import Img, mix, rng_for, scale, rgba  # noqa: E402
from plants import draw_plant, draw_dead_crop  # noqa: E402
from icons import ICONS, effect_icon, plow_texture  # noqa: E402
from data_tables import (CROPS, SOILS, FOOD_NUTRITION, AFFLICTIONS, RECIPES_SHAPED, RECIPES_SHAPELESS,  # noqa: E402
                         RECIPES_COOKING, NAMES, EFFECTS, TAGS)
import guide  # noqa: E402
import extras  # noqa: E402
import conditions  # noqa: E402
import lighting  # noqa: E402

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
RES = os.path.join(ROOT, "src", "main", "resources")
ASSETS = os.path.join(RES, "assets", "tilth")
DATA = os.path.join(RES, "data")
MOD = "tilth"


def write_json(path, obj):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8", newline="\n") as f:
        json.dump(obj, f, indent=2, ensure_ascii=False)
        f.write("\n")


def save(img, *parts):
    """Writes a texture, lit for depth: relief on block faces, rim light on items, sun and shade on crops."""
    name = parts[-1]
    if parts[0] == "item":
        img = lighting.sprite(img)
    elif parts[0] == "block" and len(parts) > 2 and parts[1] == "crop":
        img = lighting.plant(img)
    elif parts[0] == "block" and img.w == 16 and img.h == 16 and lighting.fully_opaque(img) and "lab" not in name:
        img = lighting.surface(img)
    path = os.path.join(ASSETS, "textures", *parts)
    os.makedirs(os.path.dirname(path), exist_ok=True)
    img.save(path)


def model(name, obj):
    write_json(os.path.join(ASSETS, "models", name + ".json"), obj)


def blockstate(name, obj):
    write_json(os.path.join(ASSETS, "blockstates", name + ".json"), obj)


def loot(name, obj):
    write_json(os.path.join(DATA, MOD, "loot_table", "blocks", name + ".json"), obj)


def rotations(model_id):
    """Four random rotations of a model, so a field of the same block does not show a repeating pattern."""
    return [{"model": model_id}] + [{"model": model_id, "y": r} for r in (90, 180, 270)]


def self_drop(block, item=None):
    return {"type": "minecraft:block", "pools": [{"rolls": 1, "bonus_rolls": 0,
            "entries": [{"type": "minecraft:item", "name": f"{MOD}:{item or block}"}],
            "conditions": [{"condition": "minecraft:survives_explosion"}]}]}


def silk_or(block, fallback):
    return {"type": "minecraft:block", "pools": [{"rolls": 1, "bonus_rolls": 0, "entries": [{"type": "minecraft:alternatives", "children": [
        {"type": "minecraft:item", "name": f"{MOD}:{block}", "conditions": [{"condition": "minecraft:match_tool", "predicate": {
            "predicates": {"minecraft:enchantments": [{"enchantments": "minecraft:silk_touch", "levels": {"min": 1}}]}}}]},
        {"type": "minecraft:item", "name": f"{MOD}:{fallback}", "conditions": [{"condition": "minecraft:survives_explosion"}]}]}]}]}


# ============================================================================ soils

def soil_texture(name, base, sand, clay, kind):
    rng = rng_for("soil", name, kind)
    if kind == "subsoil":
        base = scale(mix(base, (196, 140, 82), 0.35), 1.12)
    elif kind == "gley":
        base = (112, 120, 124, 255)
    img = Img(16, 16, rgba(base))
    for y in range(16):
        for x in range(16):
            img.px[y][x] = scale(base, rng.uniform(0.9, 1.08))
    for _ in range(max(2, clay // 6)):
        x, y, s = rng.randrange(16), rng.randrange(16), rng.choice((1, 2))
        f = rng.choice((0.82, 0.86, 1.1))
        for dy in range(s):
            for dx in range(s):
                img.set((x + dx) % 16, (y + dy) % 16, scale(base, f))
    for _ in range(sand // 3):
        f = rng.choice((1.28, 1.35, 0.78))
        img.set(rng.randrange(16), rng.randrange(16), scale(base, f))
    if kind == "topsoil" and name != "peat":
        for _ in range(6):
            img.set(rng.randrange(16), rng.randrange(16), scale(base, 0.65))
    if name == "peat":
        for _ in range(9):
            x, y = rng.randrange(16), rng.randrange(16)
            c = scale((120, 92, 56), rng.uniform(0.8, 1.1))
            img.line(x, y, x + rng.choice((2, 3)), y + rng.choice((-1, 0, 1)), c)
    if kind == "gley":
        for _ in range(5):
            cx, cy = rng.randrange(16), rng.randrange(16)
            img.ellipse(cx, cy, rng.uniform(0.8, 1.6), rng.uniform(0.6, 1.2), (168, 98, 48), shade=0)
    return img


def snow_side(img):
    out = Img(16, 16)
    out.px = [row[:] for row in img.px]
    rng = rng_for("snowside")
    for x in range(16):
        depth = 3 + rng.choice((0, 0, 1))
        for y in range(depth):
            out.set(x, y, (238 - y * 6, 246 - y * 4, 250, 255))
    return out


def farmland_top(img, moist):
    out = Img(16, 16)
    for y in range(16):
        for x in range(16):
            c = scale(img.px[y][x], 0.88)
            if y % 4 == 0:
                c = scale(c, 1.12)
            elif y % 4 == 2:
                c = scale(c, 0.74)
            if moist:
                c = scale(mix(c, (40, 46, 60), 0.18), 0.66)
            out.px[y][x] = c
    return out


def gen_soils(lang):
    for tex in SOILS:
        tid, base, sand, clay = tex["id"], tex["color"], tex["sand"], tex["clay"]
        label = tex["name"]
        organic = tid == "peat"
        top_name = "peat" if organic else f"{tid}_topsoil"
        top_img = soil_texture(tid, base, sand, clay, "topsoil")
        save(top_img, "block", top_name + ".png")
        model(f"block/{top_name}", {"parent": "minecraft:block/cube_all", "textures": {"all": f"{MOD}:block/{top_name}"}})
        blockstate(top_name, {"variants": {"": rotations(f"{MOD}:block/{top_name}")}})
        model(f"item/{top_name}", {"parent": f"{MOD}:block/{top_name}"})
        loot(top_name, self_drop(top_name))
        lang[f"block.{MOD}.{top_name}"] = "Peat" if organic else f"{label} Topsoil"

        if not organic:
            sub = f"{tid}_subsoil"
            save(soil_texture(tid, base, sand, clay, "subsoil"), "block", sub + ".png")
            model(f"block/{sub}", {"parent": "minecraft:block/cube_all", "textures": {"all": f"{MOD}:block/{sub}"}})
            blockstate(sub, {"variants": {"": rotations(f"{MOD}:block/{sub}")}})
            model(f"item/{sub}", {"parent": f"{MOD}:block/{sub}"})
            loot(sub, self_drop(sub))
            lang[f"block.{MOD}.{sub}"] = f"{label} Subsoil"

        grass = f"{tid}_grass"
        save(snow_side(top_img), "block", grass + "_snow.png")
        # The grass overlay layer has transparent pixels: without cutout rendering they draw as black.
        model(f"block/{grass}", {"parent": "minecraft:block/grass_block", "render_type": "minecraft:cutout_mipped", "textures": {
            "particle": f"{MOD}:block/{top_name}", "bottom": f"{MOD}:block/{top_name}", "side": f"{MOD}:block/{top_name}"}})
        model(f"block/{grass}_snow", {"parent": "minecraft:block/cube_bottom_top", "textures": {
            "particle": f"{MOD}:block/{top_name}", "bottom": f"{MOD}:block/{top_name}", "side": f"{MOD}:block/{grass}_snow",
            "top": "minecraft:block/grass_block_top"}})
        blockstate(grass, {"variants": {"snowy=false": {"model": f"{MOD}:block/{grass}"},
                                        "snowy=true": {"model": f"{MOD}:block/{grass}_snow"}}})
        model(f"item/{grass}", {"parent": f"{MOD}:block/{grass}"})
        loot(grass, silk_or(grass, top_name))
        lang[f"block.{MOD}.{grass}"] = f"{label} with Grass"

        farm = f"{tid}_farmland"
        save(farmland_top(top_img, False), "block", farm + ".png")
        save(farmland_top(top_img, True), "block", farm + "_moist.png")
        model(f"block/{farm}", {"parent": "minecraft:block/template_farmland", "textures": {
            "dirt": f"{MOD}:block/{top_name}", "top": f"{MOD}:block/{farm}"}})
        model(f"block/{farm}_moist", {"parent": "minecraft:block/template_farmland", "textures": {
            "dirt": f"{MOD}:block/{top_name}", "top": f"{MOD}:block/{farm}_moist"}})
        save(conditions.cracked(farmland_top(top_img, False)), "block", farm + "_cracked.png")
        save(conditions.soggy(farmland_top(top_img, True)), "block", farm + "_soggy.png")
        for variant in ("cracked", "soggy"):
            model(f"block/{farm}_{variant}", {"parent": "minecraft:block/template_farmland", "textures": {
                "dirt": f"{MOD}:block/{top_name}", "top": f"{MOD}:block/{farm}_{variant}"}})
        variants = {}
        for m in range(8):
            for wet in ("false", "true"):
                suffix = "_soggy" if wet == "true" else "_cracked" if m <= 1 else "_moist" if m >= 5 else ""
                variants[f"moisture={m},soggy={wet}"] = {"model": f"{MOD}:block/{farm}{suffix}"}
        blockstate(farm, {"variants": variants})
        model(f"item/{farm}", {"parent": f"{MOD}:block/{farm}"})
        loot(farm, self_drop(farm, top_name))
        lang[f"block.{MOD}.{farm}"] = f"{label} Farmland"

    gley = soil_texture("gley", (112, 120, 124), 6, 47, "gley")
    save(gley, "block", "gley.png")
    model("block/gley", {"parent": "minecraft:block/cube_all", "textures": {"all": f"{MOD}:block/gley"}})
    blockstate("gley", {"variants": {"": rotations(f"{MOD}:block/gley")}})
    model("item/gley", {"parent": f"{MOD}:block/gley"})
    loot("gley", self_drop("gley"))
    lang[f"block.{MOD}.gley"] = "Gley (Waterlogged Subsoil)"


# ============================================================================ crops

def gen_crops(lang):
    model("block/tall_cross", {
        "ambientocclusion": False,
        "render_type": "minecraft:cutout",
        "textures": {"particle": "#cross"},
        "elements": [
            {"from": [0.8, 0, 8], "to": [15.2, 32, 8], "rotation": {"origin": [8, 8, 8], "axis": "y", "angle": 45, "rescale": True},
             "shade": False, "faces": {"north": {"uv": [0, 0, 16, 16], "texture": "#cross"}, "south": {"uv": [0, 0, 16, 16], "texture": "#cross"}}},
            {"from": [8, 0, 0.8], "to": [8, 32, 15.2], "rotation": {"origin": [8, 8, 8], "axis": "y", "angle": 45, "rescale": True},
             "shade": False, "faces": {"west": {"uv": [0, 0, 16, 16], "texture": "#cross"}, "east": {"uv": [0, 0, 16, 16], "texture": "#cross"}}}
        ]})
    plane = lambda frm, to, rot, faces: {"from": frm, "to": to, "shade": False,
                                         **({"rotation": {"origin": [8, 8, 8], "axis": "y", "angle": 45, "rescale": True}} if rot else {}),
                                         "faces": {f: {"uv": [0, 0, 16, 16], "texture": "#cross"} for f in faces}}
    model("block/bushy_cross", {
        "ambientocclusion": False, "render_type": "minecraft:cutout", "textures": {"particle": "#cross"},
        "elements": [plane([0.8, 0, 8], [15.2, 16, 8], True, ("north", "south")),
                     plane([8, 0, 0.8], [8, 16, 15.2], True, ("east", "west")),
                     plane([1.5, 0, 8], [14.5, 15, 8], False, ("north", "south")),
                     plane([8, 0, 1.5], [8, 15, 14.5], False, ("east", "west"))]})
    for crop in CROPS:
        cid = crop["id"]
        variants = {}
        for stage in range(10):
            tall = crop.get("tall_from", -1) >= 0 and stage >= crop["tall_from"]
            plant = draw_plant(crop, stage, tall)
            for cond in conditions.CONDITIONS:
                own = cond == "healthy" or stage > 0
                tex = f"{cid}_stage{stage}" + ("" if not own or cond == "healthy" else "_" + cond)
                if own:
                    save(conditions.apply(plant, cond, f"{cid}{stage}"), "block", "crop", tex + ".png")
                    if tall:
                        model(f"block/crop/{tex}", {"parent": f"{MOD}:block/tall_cross", "textures": {"cross": f"{MOD}:block/crop/{tex}"}})
                    elif crop["form"] == "cereal":
                        model(f"block/crop/{tex}", {"parent": "minecraft:block/crop", "render_type": "minecraft:cutout",
                                                    "textures": {"crop": f"{MOD}:block/crop/{tex}"}})
                    else:
                        model(f"block/crop/{tex}", {"parent": f"{MOD}:block/bushy_cross",
                                                    "textures": {"cross": f"{MOD}:block/crop/{tex}"}})
                variants[f"condition={cond},stage={stage}"] = {"model": f"{MOD}:block/crop/{tex}"}
        blockstate(f"{cid}_crop", {"variants": variants})
        lang[f"block.{MOD}.{cid}_crop"] = crop["name"] + " Crop"

    save(draw_dead_crop(), "block", "dead_crop.png")
    model("block/dead_crop", {"parent": "minecraft:block/cross", "render_type": "minecraft:cutout",
                              "textures": {"cross": f"{MOD}:block/dead_crop"}})
    blockstate("dead_crop", {"variants": {"": {"model": f"{MOD}:block/dead_crop"}}})
    loot("dead_crop", {"type": "minecraft:block", "pools": []})
    lang[f"block.{MOD}.dead_crop"] = "Dead Crop"

    puddle = Img(16, 16)
    rng = rng_for("puddle")
    for y in range(16):
        for x in range(16):
            puddle.px[y][x] = (70 + rng.randrange(20), 110 + rng.randrange(20), 190 + rng.randrange(25), 150)
    for _ in range(6):
        x, y = rng.randrange(14), rng.randrange(16)
        puddle.line(x, y, x + 2, y, (170, 200, 240, 170))
    save(puddle, "block", "puddle.png")
    model("block/puddle", {
        "parent": "minecraft:block/block", "render_type": "minecraft:translucent",
        "textures": {"particle": f"{MOD}:block/puddle", "water": f"{MOD}:block/puddle"},
        "elements": [{"from": [0, 0, 0], "to": [16, 0.5, 16],
                      "faces": {"up": {"uv": [0, 0, 16, 16], "texture": "#water"},
                                "down": {"uv": [0, 0, 16, 16], "texture": "#water", "cullface": "down"}}}]})
    blockstate("puddle", {"variants": {"": {"model": f"{MOD}:block/puddle"}}})
    lang[f"block.{MOD}.puddle"] = "Puddle"


# ============================================================================ items

def crop_item_names():
    names = []
    for crop in CROPS:
        for key in ("seed", "produce"):
            item = crop[key]
            if item.startswith(MOD + ":"):
                n = item.split(":", 1)[1]
                if n not in names:
                    names.append(n)
    return names


def gen_items(lang):
    names = crop_item_names() + [n for n in ICONS if n not in crop_item_names()]
    missing = []
    for name in names:
        if name not in ICONS:
            missing.append(name)
            continue
        img = ICONS[name]()
        save(img, "item", name + ".png")
        model(f"item/{name}", {"parent": "minecraft:item/generated", "textures": {"layer0": f"{MOD}:item/{name}"}})
        lang[f"item.{MOD}.{name}"] = NAMES.get(name, name.replace("_", " ").title())
    if missing:
        raise SystemExit("No icon defined for: " + ", ".join(missing))


def gen_misc_textures(lang):
    save(plow_texture(), "entity", "plow.png")
    lang[f"entity.{MOD}.plow"] = "Moldboard Plough"
    for eid, (label, color, symbol) in EFFECTS.items():
        save(effect_icon(color, symbol), "mob_effect", eid + ".png")
        lang[f"effect.{MOD}.{eid}"] = label
    lang["itemGroup.tilth"] = "Tilth: Realistic Agronomy"
    lang["jei.tilth.crop"] = "Growing (Tilth)"
    lang["jei.tilth.tillage"] = "Tillage"
    lang["jei.tilth.amendment"] = "Soil Amendments"


# ============================================================================ data

def ing(s):
    if s.startswith("#"):
        return {"tag": s[1:]}
    return {"item": s if ":" in s else f"{MOD}:{s}"}


def res(s):
    return s if ":" in s else f"{MOD}:{s}"


def gen_recipes():
    rdir = os.path.join(DATA, MOD, "recipe")
    if os.path.isdir(rdir):
        shutil.rmtree(rdir)
    for name, pattern, key, result, count in RECIPES_SHAPED:
        write_json(os.path.join(rdir, name + ".json"), {
            "type": "minecraft:crafting_shaped", "category": "misc", "pattern": pattern,
            "key": {k: ing(v) for k, v in key.items()}, "result": {"id": res(result), "count": count}})
    for name, ingredients, result, count, quality in RECIPES_SHAPELESS:
        write_json(os.path.join(rdir, name + ".json"), {
            "type": f"{MOD}:quality_shapeless" if quality else "minecraft:crafting_shapeless", "category": "misc",
            "ingredients": [ing(i) for i in ingredients], "result": {"id": res(result), "count": count}})
    for name, ingredient, result, xp, quality in RECIPES_COOKING:
        for kind, time, vanilla in (("smelting", 200, "smelting"), ("smoking", 100, "smoking"), ("campfire", 600, "campfire_cooking")):
            rtype = f"{MOD}:quality_{vanilla}" if quality else f"minecraft:{vanilla}"
            write_json(os.path.join(rdir, f"{name}_from_{kind}.json"), {
                "type": rtype, "category": "food", "ingredient": ing(ingredient), "result": {"id": res(result)},
                "experience": xp, "cookingtime": time})
    # The Field Guide exists only when Patchouli is installed.
    write_json(os.path.join(rdir, "field_guide.json"), {
        "neoforge:conditions": [{"type": "neoforge:mod_loaded", "modid": "patchouli"}],
        "type": "minecraft:crafting_shapeless", "category": "misc",
        "ingredients": [{"item": "minecraft:book"}, {"tag": "c:seeds"}],
        "result": {"id": "patchouli:guide_book", "components": {"patchouli:book": f"{MOD}:field_guide"}}})


def gen_tags():
    blocks = {"topsoil": [], "subsoil": [], "grass": [], "farmland": [], "crops": []}
    for tex in SOILS:
        t = tex["id"]
        blocks["topsoil"].append(f"{MOD}:peat" if t == "peat" else f"{MOD}:{t}_topsoil")
        if t != "peat":
            blocks["subsoil"].append(f"{MOD}:{t}_subsoil")
        blocks["grass"].append(f"{MOD}:{t}_grass")
        blocks["farmland"].append(f"{MOD}:{t}_farmland")
    blocks["crops"] = [f"{MOD}:{c['id']}_crop" for c in CROPS]
    all_soil = blocks["topsoil"] + blocks["subsoil"] + blocks["grass"] + [f"{MOD}:gley"]

    def tag(ns, kind, name, values):
        write_json(os.path.join(DATA, ns, "tags", kind, name + ".json"), {"replace": False, "values": values})

    tag("minecraft", "block", "dirt", blocks["topsoil"] + blocks["subsoil"] + blocks["grass"])
    tag("minecraft", "block", "mineable/shovel", all_soil + blocks["farmland"] + [f"{MOD}:manure_layer"])
    tag("minecraft", "block", "mineable/hoe", [f"{MOD}:alfalfa_bale"])
    tag("minecraft", "block", "crops", blocks["crops"])
    for t in ("animals_spawnable_on", "valid_spawn", "wolves_spawnable_on", "rabbits_spawnable_on", "foxes_spawnable_on",
              "goats_spawnable_on", "parrots_spawnable_on"):
        tag("minecraft", "block", t, blocks["grass"])
    tag("minecraft", "block", "enderman_holdable", blocks["topsoil"] + blocks["grass"])
    tag(MOD, "block", "soils", all_soil)
    tag(MOD, "block", "farmland", blocks["farmland"])
    for (ns, name), values in TAGS.items():
        tag(ns, "item", name, [res(v) for v in values])


def gen_worldgen():
    write_json(os.path.join(DATA, MOD, "worldgen", "configured_feature", "catena_soils.json"),
               {"type": f"{MOD}:catena_soils", "config": {}})
    write_json(os.path.join(DATA, MOD, "worldgen", "placed_feature", "catena_soils.json"),
               {"feature": f"{MOD}:catena_soils", "placement": []})
    write_json(os.path.join(DATA, MOD, "neoforge", "biome_modifier", "catena_soils.json"),
               {"type": "neoforge:add_features", "biomes": "#minecraft:is_overworld", "features": f"{MOD}:catena_soils",
                "step": "top_layer_modification"})


def gen_loot_modifiers():
    grass_tables = ["minecraft:blocks/short_grass", "minecraft:blocks/tall_grass", "minecraft:blocks/fern", "minecraft:blocks/large_fern"]
    write_json(os.path.join(DATA, MOD, "loot_modifiers", "grass_seeds.json"), {
        "type": f"{MOD}:grass_seeds",
        "conditions": [{"condition": "minecraft:any_of", "terms": [
            {"condition": "neoforge:loot_table_id", "loot_table_id": t} for t in grass_tables]}],
        "chance": 0.06})
    write_json(os.path.join(DATA, "neoforge", "loot_modifiers", "global_loot_modifiers.json"),
               {"replace": False, "entries": [f"{MOD}:grass_seeds"]})


def gen_data_maps():
    values = {}
    for item, profile in FOOD_NUTRITION.items():
        values[res(item)] = {k: v for k, v in profile.items() if v}
    write_json(os.path.join(DATA, MOD, "data_maps", "item", "food_nutrition.json"), {"values": values})


def gen_afflictions():
    adir = os.path.join(DATA, MOD, MOD, "affliction")
    if os.path.isdir(adir):
        shutil.rmtree(adir)
    for aid, obj in AFFLICTIONS.items():
        write_json(os.path.join(adir, aid + ".json"), obj)


def main():
    lang = {}
    gen_soils(lang)
    gen_crops(lang)
    gen_items(lang)
    gen_misc_textures(lang)
    gen_recipes()
    gen_tags()
    extras.generate(lang, {"save": save, "model": model, "blockstate": blockstate, "loot": loot, "self_drop": self_drop,
                                  "afflictions": AFFLICTIONS, "data": DATA})
    gen_worldgen()
    gen_loot_modifiers()
    gen_data_maps()
    gen_afflictions()
    lang = dict(sorted(lang.items()))
    write_json(os.path.join(ASSETS, "lang", "en_us.json"), lang)
    wiki_dir = sys.argv[sys.argv.index("--wiki") + 1] if "--wiki" in sys.argv else None
    entries = guide.generate(ROOT, lang, wiki_dir)
    print(f"Generated {len(lang)} lang entries, {len(CROPS)} crops x 10 stages, {len(AFFLICTIONS)} afflictions, "
          f"{entries} guide entries (Patchouli book + docs site" + (" + wiki" if wiki_dir else "") + ").")


if __name__ == "__main__":
    main()
