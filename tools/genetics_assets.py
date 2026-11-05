"""Genetics assets: the Genetic Incubator block, the Gene Splicer and Spliced Embryo, the escaped homunculus, and the
Veterinary Draught and Spoiled Grain icons (which used to borrow vanilla icons)."""
import math

from pixelart import Img, rng_for, scale
from breeds import faces

MOD = "tilth"
STEEL = (150, 156, 164)


def rgba(c, f=1.0, a=255):
    out = scale((*c[:3], 255), f)
    return (out[0], out[1], out[2], a)


# ---------------------------------------------------------------- incubator

def tex_steel(seed, vents=False, rivets=True):
    img = Img(16, 16)
    rng = rng_for("incubator", seed)
    for y in range(16):
        for x in range(16):
            img.set(x, y, rgba(STEEL, 0.9 + 0.08 * rng.random() + (0.06 if y < 2 else 0)))
    for i in range(16):
        img.set(i, 0, rgba(STEEL, 0.7))
        img.set(i, 15, rgba(STEEL, 0.65))
        img.set(0, i, rgba(STEEL, 0.75))
        img.set(15, i, rgba(STEEL, 0.7))
    if rivets:
        for x, y in ((2, 2), (13, 2), (2, 13), (13, 13)):
            img.set(x, y, rgba(STEEL, 1.3))
    if vents:
        for y in range(5, 12, 2):
            img.line(4, y, 11, y, rgba((40, 42, 48)))
    return img


def tex_front(working):
    img = tex_steel("front")
    # A window into the warm chamber; a culture dish glows green while it works.
    for y in range(3, 11):
        for x in range(3, 13):
            img.set(x, y, rgba((60, 80, 86) if not working else (70, 110, 70), 0.9 + 0.05 * ((x + y) % 2)))
    for x in range(3, 13):
        img.set(x, 3, rgba((180, 210, 220), 1.0))
    if working:
        for x in range(5, 11):
            img.set(x, 9, rgba((120, 230, 110)))
        img.set(7, 8, rgba((180, 255, 160)))
        img.set(8, 8, rgba((180, 255, 160)))
        img.set(8, 7, rgba((150, 240, 130)))
    else:
        img.line(5, 9, 10, 9, rgba((150, 160, 160)))
    # Control panel: a dial and a status lamp.
    img.set(4, 12, rgba((30, 30, 34)))
    img.set(5, 12, rgba((200, 170, 70)))
    img.set(11, 12, rgba((60, 220, 80) if working else (180, 40, 36)))
    img.set(11, 13, rgba((40, 40, 44)))
    return img


def incubator_model(working):
    t = {"particle": f"{MOD}:block/incubator_side", "side": f"{MOD}:block/incubator_side", "top": f"{MOD}:block/incubator_top",
         "front": f"{MOD}:block/incubator_front" + ("_on" if working else ""), "base": "minecraft:block/smooth_stone"}

    def box(frm, to, faces_):
        return {"from": frm, "to": to, "faces": {f: {"texture": "#" + t_} for f, t_ in faces_.items()}}
    return {"parent": "minecraft:block/block", "textures": t, "elements": [
        box([0, 0, 0], [16, 2, 16], {f: "base" for f in ("north", "south", "east", "west", "up", "down")}),
        box([1, 2, 1], [15, 14, 15], {"north": "front", "south": "side", "east": "side", "west": "side", "up": "top", "down": "side"}),
        box([6, 14, 6], [10, 16, 10], {f: "side" for f in ("north", "south", "east", "west", "up")})]}


# ---------------------------------------------------------------- items

def icon(draw):
    img = Img()
    draw(img)
    img.outline(0.45)
    return img


def draw_splicer(img):
    """A precision splicing tool: steel barrel, brass dial, fine needle, a twist of helix beside it."""
    for k in range(9):
        x, y = 4 + k, 11 - k
        img.set(x, y, rgba(STEEL, 1.1))
        img.set(x, y + 1, rgba(STEEL, 0.75))
    img.line(12, 3, 14, 1, rgba((220, 224, 230)))
    img.set(6, 9, rgba((206, 168, 60)))
    img.set(7, 9, rgba((236, 196, 84)))
    img.set(6, 8, rgba((236, 196, 84)))
    for y in range(3, 12):
        dx = round(1.5 * math.sin(y * 0.9))
        img.set(3 + dx, y, rgba((80, 170, 220)))
        img.set(3 - dx, y, rgba((220, 80, 120)))


def draw_embryo(img):
    """A stoppered glass vial holding a sprouting seed in green gel."""
    for y in range(4, 14):
        for x in range(5, 11):
            img.set(x, y, rgba((200, 230, 236), 1.0, 150 if 6 <= x <= 9 else 220))
    for y in range(8, 14):
        for x in range(6, 10):
            img.set(x, y, rgba((110, 200, 90)))
    img.set(7, 10, rgba((220, 190, 120)))
    img.set(8, 10, rgba((220, 190, 120)))
    img.set(8, 9, rgba((90, 170, 60)))
    img.set(8, 8, rgba((140, 220, 100)))
    img.rect(6, 2, 9, 3, rgba((150, 108, 62)))


def draw_draught(img):
    """An amber medicine bottle with a cork and a white label bearing a red cross."""
    for y in range(6, 15):
        half = 3 if y > 7 else 2
        for x in range(8 - half, 8 + half):
            img.set(x, y, rgba((150, 82, 30), 0.85 + 0.15 * (x == 8 - half + 1)))
    img.rect(7, 3, 8, 5, rgba((200, 210, 214), 1.0, 200))
    img.rect(7, 2, 8, 2, rgba((160, 120, 70)))
    img.rect(5, 9, 10, 12, rgba((236, 232, 220)))
    img.line(7, 9, 7, 12, rgba((200, 40, 40)))
    img.line(8, 9, 8, 12, rgba((200, 40, 40)))
    img.line(5, 10, 10, 10, rgba((200, 40, 40)))
    img.line(5, 11, 10, 11, rgba((200, 40, 40)))


def draw_spoiled(img):
    """Mouldy grain: dull kernels furred with grey-green mould."""
    rng = rng_for("spoiled_grain_icon")
    for _ in range(14):
        x, y = 3 + rng.randrange(10), 4 + rng.randrange(9)
        c = (150, 140, 100) if rng.random() < 0.6 else (110, 120, 90)
        img.set(x, y, rgba(c))
        img.set(x + 1, y, rgba(c, 0.85))
    for _ in range(10):
        img.set(3 + rng.randrange(11), 4 + rng.randrange(10), rgba((180, 196, 170)))
    for _ in range(5):
        img.set(3 + rng.randrange(11), 4 + rng.randrange(10), rgba((60, 70, 50)))


# ---------------------------------------------------------------- the homunculus (slime layout, 64x32)

def homunculus_texture():
    img = Img(64, 32)
    rng = rng_for("homunculus")
    skin = (226, 170, 160)

    def paint(u, v, w, h, d, colour):
        for name, (rect, _) in faces(u, v, w, h, d).items():
            x0, y0, rw, rh = rect
            for j in range(rh):
                for i in range(rw):
                    img.px[y0 + j][x0 + i] = colour(name, i, j, rw, rh)
    # Outer jelly: pale flesh, mostly see-through.
    paint(0, 0, 8, 8, 8, lambda f, i, j, rw, rh: rgba(skin, 1.05 + 0.05 * rng.random(), 110))
    # Inner body: pink, veined.
    paint(0, 16, 6, 6, 6, lambda f, i, j, rw, rh: rgba((200, 110, 110) if (i * 3 + j) % 7 == 0 else skin, 0.9 + 0.08 * rng.random()))
    # Eyes and mouth.
    paint(32, 0, 2, 2, 2, lambda f, i, j, rw, rh: rgba((20, 16, 18)) if not (i == 0 and j == 0 and f == "north") else rgba((240, 240, 240)))
    paint(32, 4, 2, 2, 2, lambda f, i, j, rw, rh: rgba((20, 16, 18)) if not (i == 0 and j == 0 and f == "north") else rgba((240, 240, 240)))
    paint(32, 8, 1, 1, 1, lambda f, i, j, rw, rh: rgba((120, 30, 40)))
    return img


def generate(lang, h):
    save, model, blockstate = h["save"], h["model"], h["blockstate"]
    save(tex_steel("side", vents=True), "block", "incubator_side.png")
    save(tex_steel("top", vents=True, rivets=False), "block", "incubator_top.png")
    save(tex_front(False), "block", "incubator_front.png")
    save(tex_front(True), "block", "incubator_front_on.png")
    model("block/incubator", incubator_model(False))
    model("block/incubator_working", incubator_model(True))
    variants = {}
    for d, r in (("north", 0), ("east", 90), ("south", 180), ("west", 270)):
        for working in ("false", "true"):
            variants[f"facing={d},working={working}"] = {"model": f"{MOD}:block/incubator" + ("_working" if working == "true" else ""), "y": r}
    blockstate("incubator", {"variants": variants})
    model("item/incubator", {"parent": f"{MOD}:block/incubator"})
    for name, draw in (("gene_splicer", draw_splicer), ("spliced_embryo", draw_embryo), ("veterinary_draught", draw_draught),
                       ("spoiled_grain", draw_spoiled)):
        save(icon(draw), "item", name + ".png")
        model(f"item/{name}", {"parent": "minecraft:item/generated", "textures": {"layer0": f"{MOD}:item/{name}"}})
    save(homunculus_texture(), "entity", "homunculus.png")
