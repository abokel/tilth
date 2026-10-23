"""
Writes the Tilth Field Guide in three formats from guide_content.py:
  - the Patchouli book (data/tilth/patchouli_books, assets/tilth/patchouli_books),
  - the documentation website in docs/ (Jekyll + just-the-docs, GitHub Pages ready),
  - GitHub wiki pages (only when a wiki directory is given).
Crop and affliction pages are generated from the Java crop table and the affliction data.
"""
import json
import os
import re
import shutil

from data_tables import AFFLICTIONS, RECIPES_COOKING, RECIPES_SHAPED, RECIPES_SHAPELESS
from guide_content import BOOK, CATEGORIES, craft, spot, text

MOD = "tilth"
BOOK_ID = "field_guide"
REPO_URL = "https://github.com/abokel/tilth"

FAMILY_NAMES = {
    "POACEAE": "grass family", "FABACEAE": "legume family", "SOLANACEAE": "nightshade family",
    "BRASSICACEAE": "cabbage family", "AMARANTHACEAE": "beet family", "AMARYLLIDACEAE": "onion family",
    "APIACEAE": "carrot family", "ASTERACEAE": "daisy family", "LINACEAE": "flax family", "MALVACEAE": "mallow family",
}
UNIT_LABELS = {"BUSHEL": "bu/ac", "HUNDREDWEIGHT": "cwt/ac", "TON": "tons/ac", "POUND": "lb/ac", "BALE": "bales/ac"}


def write_json(path, obj):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8", newline="\n") as f:
        json.dump(obj, f, indent=2, ensure_ascii=False)
        f.write("\n")


def write_text(path, s):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8", newline="\n") as f:
        f.write(s)


# ============================================================================ crop data from Java

def parse_crops(java_path):
    src = open(java_path, encoding="utf-8").read()
    crops = []
    pattern = re.compile(r'CropSpecies\.builder\("(\w+)", "([^"]+)", "([^"]+)", (\w+), (\w+)\)(.*?)\.build\(\)\);', re.S)
    for m in pattern.finditer(src):
        cid, name, sci, family, form, chain = m.groups()
        calls = {}
        for method, args in re.findall(r"\.(\w+)\(([^()]*)\)", chain):
            parts = [a.strip().strip('"') for a in args.split(",")] if args.strip() else []
            vals = []
            for p in parts:
                try:
                    vals.append(float(p))
                except ValueError:
                    vals.append(p)
            calls[method] = vals
        crops.append({"id": cid, "name": name, "sci": sci, "family": family, "form": form, **calls})
    return crops


def per_acre(t_per_ha, unit, lb):
    kg_m2 = t_per_ha / 10.0
    lb_ac = kg_m2 * 4046.8564224 * 2.20462262
    return {"BUSHEL": lb_ac / max(1, lb), "HUNDREDWEIGHT": lb_ac / 100, "TON": lb_ac / 2000, "POUND": lb_ac,
            "BALE": lb_ac * lb / 480}[unit]


def fmt(v):
    return ("%d" % v) if float(v).is_integer() else ("%g" % v)


# ============================================================================ names

class Names:
    def __init__(self, lang):
        self.lang = lang

    def item(self, rid):
        if rid.startswith("#"):
            return "any " + rid.split(":", 1)[1].split("/")[-1].replace("_", " ")
        ns, path = rid.split(":", 1) if ":" in rid else (MOD, rid)
        for key in (f"item.{ns}.{path}", f"block.{ns}.{path}"):
            if key in self.lang:
                return self.lang[key]
        return path.replace("_", " ").title()


# ============================================================================ generated entries

def crop_entries(crops, names):
    entries = []
    for c in crops:
        tb, topt, tmax, _ = c["temps"]
        germ, gdd = c["gdd"]
        fv, fr = c["frost"]
        lo, hi, mn, mx = c["ph"]
        root = c["water"][3]
        wl = c["water"][5]
        n, p, k = c["uptake"][0], c["uptake"][1], c["uptake"][2]
        ref, unit, lb = c["yield"]
        produce, pn, seed, sn = c["items"]
        season = "Cool-season" if tb <= 5 else "Warm-season"
        sow = "sow as soon as the ground thaws" if tb <= 5 else "sow after the last frost, into warm soil"
        lines = [
            f"- **{season}:** {sow}. Sprouts after {fmt(germ)} °C·d above {fmt(tb)} °C.",
            f"- Ripens after **{fmt(gdd)} °C·d**. Best near {fmt(topt)} °C; stops above {fmt(tmax)} °C.",
            f"- **Frost** kills it below {fmt(fv)} °C ({fmt(fr)} °C in flower).",
            f"- **Soil pH** {fmt(lo)}-{fmt(hi)} (tolerates {fmt(mn)}-{fmt(mx)}). Roots {fmt(root)} m. "
            + ("Survives flooding." if wl >= 1 else ("Dislikes waterlogging." if wl < 0.2 else "Some tolerance of wet soil.")),
            f"- **Feeds on** N {fmt(n)}, P {fmt(p)}, K {fmt(k)} kg/ha per crop"
            + (f"; fixes about {int(c['fixation'][0] * 100)}% of its own nitrogen." if "fixation" in c else "."),
            f"- **Yield** when well grown: about {per_acre(ref, unit, lb):.0f} {UNIT_LABELS[unit]} ({fmt(ref)} t/ha).",
            f"- **Harvest:** {names.item(produce)}" + (f" and {names.item(seed)}" if seed != produce else "") + "."
            + (" Regrows after cutting." if "perennial" in c else "") + (" Grows two blocks tall." if "tall" in c else ""),
        ]
        intro = f"**{c['name']}** (_{c['sci']}_), {FAMILY_NAMES[c['family']]}."
        entries.append({"id": c["id"], "name": c["name"], "icon": seed, "pages": [
            spot(seed, intro, c["name"]),
            text("\n".join(lines)),
        ]})
    return entries


def planting_pages(crops):
    cool = sorted([c["name"] for c in crops if c["temps"][0] <= 5])
    warm = sorted([c["name"] for c in crops if c["temps"][0] > 5])
    hardy = sorted([c["name"] for c in crops if c["frost"][0] <= -10])
    return [
        text("**Cool-season crops** grow from 0-5 °C and tolerate light frost. Sow them in early spring, as soon as "
             "the ground thaws:\n\n" + "\n".join(f"- {n}" for n in cool)),
        text("**Warm-season crops** need 10 °C or more and die in frost. Sow them after the last frost, in late spring "
             "or early summer:\n\n" + "\n".join(f"- {n}" for n in warm)),
        text("**Very hardy crops** survive -10 °C or colder and can be sown in autumn to overwinter:\n\n"
             + "\n".join(f"- {n}" for n in hardy)
             + "\n\nCheck the date with /tilth calendar, and frosts with /tilth climate."),
    ]


# Icons by kind of organism; a lichen is itself a fungus-alga symbiosis.
AFFLICTION_ICONS = {
    "fungus": "minecraft:brown_mushroom",
    "oomycete": "minecraft:poisonous_potato",
    "bacterium": "minecraft:rotten_flesh",
    "virus": "minecraft:fermented_spider_eye",
    "insect": "minecraft:spider_eye",
    "nematode": "minecraft:string",
    "protist": "minecraft:clay_ball",
    "mollusc": "minecraft:slime_ball",
    "symbiont": "minecraft:glow_lichen",
}


def affliction_entries(names):
    entries = []
    for aid, a in sorted(AFFLICTIONS.items(), key=lambda kv: kv[1]["name"]):
        e = a["envelope"]
        cond = f"{fmt(e['temp_min'])}-{fmt(e['temp_max'])} °C (best {fmt(e['temp_opt'])} °C)"
        if e.get("min_humidity"):
            cond += f", humidity above {fmt(e['min_humidity'])}%"
        if e.get("leaf_wetness_hours"):
            cond += f", {fmt(e['leaf_wetness_hours'])} h of wet leaves"
        if e.get("ph_max", 10) < 9:
            cond += f", pH below {fmt(e['ph_max'])}"
        if e.get("ph_min", 3) > 3.5:
            cond += f", pH above {fmt(e['ph_min'])}"
        hosts = ", ".join(h.replace("family:", "").replace("_", " ") if h != "*" else "all crops" for h in a["hosts"])
        lines = [f"- **Hosts:** {hosts}.",
                 "- **Spreads:** " + ", ".join(t.replace("_", " ") for t in a["transmission"]) + ".",
                 f"- **Favoured by** {cond}.",
                 f"- **Survives** {fmt(a['lifecycle']['survival_years'])} years "
                 f"({a['lifecycle']['overwintering'].replace('_', ' ')})."]
        if a.get("beneficial"):
            lines.append(f"- **Benefit:** +{int(a['impact']['benefit'] * 100)}% to its host.")
        else:
            lines.append(f"- **Damage:** up to {int(a['impact']['max_yield_loss'] * 100)}% yield"
                         + (", can kill the plant." if a["impact"]["can_kill_host"] else "."))
        lines.append("- **Control:** " + "; ".join(a["control"]["methods"]) + ".")
        pages = [text(f"**{a['name']}** (_{a['scientific_name']}_), {a['category'].replace('_', ' ')}.\n\n"
                      + "\n".join(lines[:3])),
                 text("\n".join(lines[3:]) + (f"\n\n{a['notes']}" if a.get("notes") else ""))]
        entries.append({"id": aid, "name": a["name"], "icon": AFFLICTION_ICONS.get(a["category"], "minecraft:spider_eye"),
                        "pages": pages})
    return entries


def build_categories(crops, names):
    cats = []
    for cat in CATEGORIES:
        cat = dict(cat)
        entries = []
        for e in cat["entries"]:
            e = dict(e)
            if e["pages"] == "PLANTING":
                e["pages"] = planting_pages(crops)
            entries.append(e)
        if cat.get("auto") == "CROPS":
            entries += crop_entries(crops, names)
        elif cat.get("auto") == "AFFLICTIONS":
            entries += affliction_entries(names)
        cat["entries"] = entries
        cats.append(cat)
    return cats


# ============================================================================ Patchouli

CHARS_PER_LINE = 21
CAPACITY = {"text_first": 12, "text": 14, "spotlight": 7, "crafting": 3, "smelting": 5, "entity": 4}


def to_patchouli(s):
    s = re.sub(r"\*\*(.+?)\*\*", r"$(thing)\1$()", s)
    s = re.sub(r"_(.+?)_", r"$(o)\1$()", s)
    paras = s.split("\n\n")
    out = []
    for para in paras:
        lines = para.split("\n")
        parts = []
        for line in lines:
            if line.startswith("- "):
                parts.append("$(li)" + line[2:])
            else:
                parts.append(("$(br)" if parts else "") + line)
        out.append("".join(parts))
    return "$(br2)".join(out)


def visible_lines(s):
    """Rough count of rendered lines for a markup string."""
    plain = re.sub(r"\*\*|_", "", s)
    total = 0
    for para in plain.split("\n\n"):
        for line in para.split("\n"):
            width = CHARS_PER_LINE - (2 if line.startswith("- ") else 0)
            total += max(1, -(-len(line) // width))
        total += 1
    return total - 1


def split_markup(s, capacity):
    """Split markup into chunks that fit the line capacity, at paragraph or list-item boundaries."""
    units = []
    for para in s.split("\n\n"):
        items = para.split("\n")
        for i, it in enumerate(items):
            units.append((it, i == 0))
    chunks, current = [], []
    for unit, starts_para in units:
        candidate = current + [(unit, starts_para)]
        if current and visible_lines(join_units(candidate)) > capacity:
            chunks.append(join_units(current))
            current = [(unit, True)]
        else:
            current = candidate
    if current:
        chunks.append(join_units(current))
    return chunks


def join_units(units):
    s = ""
    for i, (u, starts_para) in enumerate(units):
        if i == 0:
            s = u
        else:
            s += ("\n\n" if starts_para else "\n") + u
    return s


def patchouli_pages(pages):
    out = []
    first = True
    for p in pages:
        kind = p["type"]
        body = p.get("text", "")
        if kind == "text":
            for chunk in split_markup(body, CAPACITY["text_first" if first else "text"]):
                out.append({"type": "patchouli:text", "text": to_patchouli(chunk)})
                first = False
            continue
        head, rest = body, ""
        if body and visible_lines(body) > CAPACITY[kind]:
            chunks = split_markup(body, CAPACITY[kind])
            head, rest = chunks[0], "\n\n".join(chunks[1:])
        page = {"type": "patchouli:" + kind}
        if kind == "spotlight":
            page["item"] = p["item"]
            if p.get("title"):
                page["title"] = p["title"]
        elif kind in ("crafting", "smelting"):
            page["recipe"] = p["recipe"]
        elif kind == "entity":
            page.update({"entity": p["entity"], "scale": 0.8, "offset": 0.3, "rotate": True})
        if head:
            page["text"] = to_patchouli(head)
        out.append(page)
        first = False
        if rest:
            for chunk in split_markup(rest, CAPACITY["text"]):
                out.append({"type": "patchouli:text", "text": to_patchouli(chunk)})
    return out


def write_patchouli(res_dir, cats):
    data_dir = os.path.join(res_dir, "data", MOD, "patchouli_books", BOOK_ID)
    asset_dir = os.path.join(res_dir, "assets", MOD, "patchouli_books", BOOK_ID)
    shutil.rmtree(asset_dir, ignore_errors=True)
    write_json(os.path.join(data_dir, "book.json"), {
        "name": BOOK["name"], "subtitle": BOOK["subtitle"], "landing_text": BOOK["landing"], "version": 1,
        "creative_tab": f"{MOD}:{MOD}", "book_texture": "patchouli:textures/gui/book_green.png",
        "model": "patchouli:book_green", "show_progress": False, "use_resource_pack": True, "i18n": False})
    for ci, cat in enumerate(cats):
        write_json(os.path.join(asset_dir, "en_us", "categories", cat["id"] + ".json"),
                   {"name": cat["name"], "description": cat["description"], "icon": cat["icon"], "sortnum": ci})
        for ei, e in enumerate(cat["entries"]):
            write_json(os.path.join(asset_dir, "en_us", "entries", cat["id"], e["id"] + ".json"), {
                "name": e["name"], "icon": e["icon"], "category": f"{MOD}:{cat['id']}", "sortnum": ei,
                "pages": patchouli_pages(e["pages"])})


# ============================================================================ Markdown (website + wiki)

def recipe_lookup():
    table = {}
    for name, pattern, key, result, count in RECIPES_SHAPED:
        counts = {}
        for row in pattern:
            for ch in row:
                if ch != " ":
                    counts[ch] = counts.get(ch, 0) + 1
        table[f"{MOD}:{name}"] = ("crafting table, shaped", [(key[k], c) for k, c in counts.items()], result, count)
    for name, ings, result, count, _ in RECIPES_SHAPELESS:
        counts = {}
        for i in ings:
            counts[i] = counts.get(i, 0) + 1
        table[f"{MOD}:{name}"] = ("crafting table", list(counts.items()), result, count)
    for name, ing, result, _, _ in RECIPES_COOKING:
        table[f"{MOD}:{name}_from_smelting"] = ("furnace, smoker or campfire", [(ing, 1)], result, 1)
    return table


def md_text(s):
    paras = []
    for para in s.split("\n\n"):
        lines = para.split("\n")
        rendered = []
        for i, line in enumerate(lines):
            if line.startswith("- "):
                rendered.append(line)
            else:
                nxt = lines[i + 1] if i + 1 < len(lines) else ""
                rendered.append(line + ("  " if nxt and not nxt.startswith("- ") else ""))
        paras.append("\n".join(rendered))
    return "\n\n".join(paras)


def md_pages(pages, names, recipes):
    out = []
    for p in pages:
        kind = p["type"]
        if kind == "text":
            out.append(md_text(p["text"]))
        elif kind == "spotlight":
            out.append(f"> **{p.get('title') or names.item(p['item'])}**  \n> " + md_text(p["text"]).replace("\n", "\n> "))
        elif kind in ("crafting", "smelting"):
            r = recipes.get(p["recipe"])
            if r:
                where, ings, result, count = r
                ing_s = " + ".join(f"{c} × {names.item(i)}" if c > 1 else names.item(i) for i, c in ings)
                out.append(f"**Recipe ({where}):** {ing_s} → " + (f"{count} × " if count > 1 else "") + names.item(result))
            if p.get("text"):
                out.append(md_text(p["text"]))
        elif kind == "entity":
            out.append(md_text(p["text"]))
    return "\n\n".join(out) + "\n"


def write_docs(docs_dir, cats, names, changelog_md):
    recipes = recipe_lookup()
    for cat in cats:
        shutil.rmtree(os.path.join(docs_dir, cat["id"]), ignore_errors=True)
    write_text(os.path.join(docs_dir, "_config.yml"),
               "title: Tilth\n"
               "description: Realistic agronomy for Minecraft (NeoForge 1.21.1)\n"
               "remote_theme: just-the-docs/just-the-docs@v0.10.1\n"
               "plugins:\n  - jekyll-remote-theme\n"
               "search_enabled: true\n"
               "heading_anchors: true\n"
               "aux_links:\n  \"Tilth on GitHub\": \"" + REPO_URL + "\"\n"
               "footer_content: \"Generated from tools/guide_content.py, the same source as the in-game Field Guide.\"\n"
               "exclude:\n  - wiki/\n")
    toc = "\n".join(f"- [{c['name']}]({c['id']}/) - {c['description']}" for c in cats)
    write_text(os.path.join(docs_dir, "index.md"),
               "---\ntitle: Home\nnav_order: 1\n---\n\n# Tilth: Realistic Agronomy\n\n" + BOOK["landing"] + "\n\n"
               "This site has the same content as the in-game **Tilth Field Guide**, which is available when Patchouli "
               "is installed.\n\n## Contents\n\n" + toc + "\n\n## More\n\n- [Changelog](changelog.html)\n"
               "- [Design notes: how the models work](DESIGN.html)\n- [Source code](" + REPO_URL + ")\n")
    write_text(os.path.join(docs_dir, "changelog.md"), "---\ntitle: Changelog\nnav_order: 98\n---\n\n" + changelog_md)
    for ci, cat in enumerate(cats):
        listing = "\n".join(f"- [{e['name']}]({e['id']}.html)" for e in cat["entries"])
        write_text(os.path.join(docs_dir, cat["id"], "index.md"),
                   f"---\ntitle: {cat['name']}\nnav_order: {ci + 2}\nhas_children: true\n---\n\n# {cat['name']}\n\n"
                   f"{cat['description']}\n\n{listing}\n")
        for ei, e in enumerate(cat["entries"]):
            write_text(os.path.join(docs_dir, cat["id"], e["id"] + ".md"),
                       f"---\ntitle: \"{e['name']}\"\nparent: {cat['name']}\nnav_order: {ei + 1}\n---\n\n# {e['name']}\n\n"
                       + md_pages(e["pages"], names, recipes))


def wiki_name(cat, e=None):
    s = cat["name"] if e is None else f"{cat['name']} - {e['name']}"
    return re.sub(r"[^A-Za-z0-9 \-]", "", s).replace(" ", "-")


def write_wiki(wiki_dir, cats, names, changelog_md):
    recipes = recipe_lookup()
    sidebar = ["**[Home](Home)**", ""]
    home = ["# Tilth: Realistic Agronomy", "", BOOK["landing"], "", "## Contents", ""]
    for cat in cats:
        sidebar.append(f"**[{cat['name']}]({wiki_name(cat)})**")
        home.append(f"- [{cat['name']}]({wiki_name(cat)}) - {cat['description']}")
        lines = [f"# {cat['name']}", "", cat["description"], ""]
        for e in cat["entries"]:
            sidebar.append(f"- [{e['name']}]({wiki_name(cat, e)})")
            lines.append(f"- [{e['name']}]({wiki_name(cat, e)})")
            write_text(os.path.join(wiki_dir, wiki_name(cat, e) + ".md"),
                       f"# {e['name']}\n\n" + md_pages(e["pages"], names, recipes))
        write_text(os.path.join(wiki_dir, wiki_name(cat) + ".md"), "\n".join(lines) + "\n")
        sidebar.append("")
    sidebar.append("**[Changelog](Changelog)**")
    home += ["", "See also the [Changelog](Changelog) and the [repository](" + REPO_URL + ")."]
    write_text(os.path.join(wiki_dir, "Home.md"), "\n".join(home) + "\n")
    write_text(os.path.join(wiki_dir, "_Sidebar.md"), "\n".join(sidebar) + "\n")
    write_text(os.path.join(wiki_dir, "Changelog.md"), changelog_md)


def generate(root, lang, wiki_dir=None):
    crops = parse_crops(os.path.join(root, "src", "main", "java", "com", "claudacious", "tilth", "sim", "crop", "Crops.java"))
    if len(crops) != 28:
        raise SystemExit(f"Expected 28 crops parsed from Crops.java, got {len(crops)}")
    names = Names(lang)
    cats = build_categories(crops, names)
    write_patchouli(os.path.join(root, "src", "main", "resources"), cats)
    changelog = open(os.path.join(root, "CHANGELOG.md"), encoding="utf-8").read()
    write_docs(os.path.join(root, "docs"), cats, names, changelog)
    if wiki_dir:
        write_wiki(wiki_dir, cats, names, changelog)
    return sum(len(c["entries"]) for c in cats)
