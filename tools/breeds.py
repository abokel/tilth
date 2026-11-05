"""
Coats for farm animal breeds, painted onto the vanilla model UV layouts (cow, pig, sheep body, chicken).

Every pixel of every box face is mapped to animal coordinates: across (0 right .. 1 left), up (0 belly/feet .. 1
spine/top) and along (0 front .. 1 rear). A breed's coat is then one function of those coordinates and the part it is
on, so patterns such as a Holstein's patches, a Hereford's white face and belly or a Berkshire's white points land in
the right place on every face. Eyes, muzzles, nostrils and hooves are drawn per face afterwards.

Face orientation follows ModelPart.Cube: the body boxes are turned 90 degrees about x, so their local top is the
chest, local bottom the rump, local north the belly and local south the spine.
"""
import math

from pixelart import Img, rng_for, scale, mix

# (part, u, v, w, h, d, turned): the vanilla model boxes and texture offsets.
LAYOUTS = {
    "cow": (64, 32, [("head", 0, 0, 8, 8, 6, False), ("horn", 22, 0, 1, 3, 1, False), ("body", 18, 4, 12, 18, 10, True),
                     ("udder", 52, 0, 4, 6, 1, True), ("leg", 0, 16, 4, 12, 4, False)]),
    "pig": (64, 32, [("head", 0, 0, 8, 8, 8, False), ("snout", 16, 16, 4, 3, 1, False), ("body", 28, 8, 10, 16, 8, True),
                     ("leg", 0, 16, 4, 6, 4, False)]),
    "sheep": (64, 32, [("head", 0, 0, 6, 6, 8, False), ("body", 28, 8, 8, 16, 6, True), ("leg", 0, 16, 4, 12, 4, False)]),
    "chicken": (64, 32, [("head", 0, 0, 4, 6, 3, False), ("beak", 14, 0, 4, 2, 2, False), ("wattle", 14, 4, 2, 2, 2, False),
                         ("body", 0, 9, 6, 8, 6, True), ("leg", 26, 0, 3, 5, 3, False), ("wing", 24, 13, 1, 4, 6, False)]),
}


def faces(u, v, w, h, d):
    """Texture rectangles of a box's faces, with a function giving local (x, y, z) in 0..1 for each pixel.
    Local y runs downward (model space); z runs from the front (-z, north) to the back."""
    f = lambda a, n: (a + 0.5) / n
    return {
        "top": ((u + d, v, w, d), lambda i, j: (f(i, w), 0.0, 1 - f(j, d))),
        "bottom": ((u + d + w, v, w, d), lambda i, j: (f(i, w), 1.0, 1 - f(j, d))),
        "west": ((u, v + d, d, h), lambda i, j: (0.0, f(j, h), 1 - f(i, d))),
        "north": ((u + d, v + d, w, h), lambda i, j: (f(i, w), f(j, h), 0.0)),
        "east": ((u + d + w, v + d, d, h), lambda i, j: (1.0, f(j, h), f(i, d))),
        "south": ((u + 2 * d + w, v + d, w, h), lambda i, j: (1 - f(i, w), f(j, h), 1.0)),
    }


def animal_coords(local, turned):
    """Local box coordinates to (across, up, along) on the animal."""
    x, y, z = local
    if turned:
        # Body boxes are turned +90 degrees about x: local z is height (z=1 the spine), local y the length.
        return x, z, y
    return x, 1 - y, z


# ---------------------------------------------------------------- value noise for patches

def _hash(ix, iy, iz, seed):
    n = (ix * 374761393 + iy * 668265263 + iz * 2147483647 + seed * 144269504) & 0xFFFFFFFF
    n = ((n ^ (n >> 13)) * 1274126177) & 0xFFFFFFFF
    return ((n ^ (n >> 16)) & 0xFFFF) / 65535.0


def noise(x, y, z, seed, freq):
    x, y, z = x * freq, y * freq, z * freq
    ix, iy, iz = math.floor(x), math.floor(y), math.floor(z)
    fx, fy, fz = x - ix, y - iy, z - iz
    s = lambda t: t * t * (3 - 2 * t)
    fx, fy, fz = s(fx), s(fy), s(fz)
    total = 0.0
    for dx in (0, 1):
        for dy in (0, 1):
            for dz in (0, 1):
                w = (fx if dx else 1 - fx) * (fy if dy else 1 - fy) * (fz if dz else 1 - fz)
                total += w * _hash(ix + dx, iy + dy, iz + dz, seed)
    return total


def patches(part, a, seed, freq=2.2, level=0.5):
    """True where a patch covers this point. Parts are offset so the head and body do not share one pattern."""
    across, up, along = a
    off = {"head": 7.3, "leg": 3.1, "body": 0.0}.get(part, 11.0)
    # Fold left and right sides together a little less than fully, so patches are not mirror images.
    return noise(across * 0.6 + off, up * 1.4 + off, along * 2.0 + off, seed, freq) > level


# ---------------------------------------------------------------- breed coats

def shade(c, a, rng, strength=0.10):
    """Light from above: spine and top a touch lighter, belly darker; a little grain."""
    across, up, along = a
    return scale((*c[:3], 255), (1 - strength) + 2 * strength * up * 0.6 + 0.06 * (rng.random() - 0.5))


CATTLE = {
    "holstein": dict(base=(240, 238, 232), patch=(30, 28, 30), face="patched"),
    "jersey": dict(base=(178, 128, 80), dark=(110, 74, 46), muzzle_ring=True),
    "highland": dict(base=(178, 94, 42), shaggy=True),
    "angus": dict(base=(34, 32, 34)),
    "hereford": dict(base=(146, 58, 30), white_face=True, white_under=True),
    "brahman": dict(base=(200, 196, 188), dark=(120, 118, 116), hump=True),
}
PIGS = {
    "large_white": dict(base=(236, 196, 186)),
    "berkshire": dict(base=(38, 34, 36), points=True),
    "tamworth": dict(base=(196, 104, 50)),
    "old_spot": dict(base=(236, 198, 186), spots=(38, 34, 36)),
    "mangalica": dict(base=(228, 204, 150), curly=True, snout=(60, 56, 56)),
}
SHEEP = {
    "merino": dict(face=(232, 222, 204), legs=(226, 214, 196), nose=(214, 150, 150)),
    "suffolk": dict(face=(34, 32, 32), legs=(30, 28, 28), nose=(50, 46, 46)),
    "hebridean": dict(face=(44, 40, 40), legs=(40, 36, 36), nose=(60, 54, 54)),
    "soay": dict(face=(118, 78, 48), legs=(104, 70, 44), nose=(150, 120, 96), pale_jaw=True),
    "herdwick": dict(face=(234, 234, 230), legs=(222, 222, 218), nose=(140, 140, 140), speckled=True),
}
CHICKENS = {
    "leghorn": dict(base=(244, 244, 238), legs=(232, 184, 64)),
    "rhode_island_red": dict(base=(128, 44, 24), tail=(28, 26, 26), legs=(226, 176, 60)),
    "australorp": dict(base=(30, 32, 34), sheen=(44, 82, 60), legs=(64, 62, 62)),
    "light_sussex": dict(base=(242, 240, 234), hackle=(34, 32, 32), tail=(34, 32, 32), legs=(232, 226, 210)),
    "barred_rock": dict(base=(206, 206, 210), bar=(58, 58, 64), legs=(226, 176, 60)),
}


def cattle_colour(breed, p, part, a, rng):
    across, up, along = a
    hoof = (46, 40, 38)
    if part == "horn":
        return (226, 214, 186) if breed != "highland" else (232, 220, 190)
    if part == "udder":
        return (226, 160, 160)
    if part == "leg" and up < 0.14:
        return hoof
    c = p["base"]
    if breed == "holstein":
        if patches(part, a, 17, 2.0, 0.5) and not (part == "leg" and up < 0.45):
            c = p["patch"]
        if part == "head" and abs(across - 0.5) < 0.16 and along < 0.2:
            c = p["base"]
    elif breed == "jersey":
        # Darker on the face, neck and legs, shading toward a pale belly.
        if part in ("head", "leg") or (part == "body" and along < 0.2):
            c = mix((*p["dark"], 255), (*c, 255), 0.35)[:3]
        if part == "body" and up < 0.3:
            c = mix((*c, 255), (226, 200, 160, 255), 0.5)[:3]
    elif breed == "highland":
        # Long hair: strands of light and dark running down the coat.
        strand = noise(across * 9, along * 9, 0.3, 41, 1.0)
        c = scale((*c, 255), 0.78 + 0.45 * strand)[:3]
    elif breed == "hereford":
        if part == "head" and (along < 0.5 or up > 0.75):
            c = (240, 236, 228)
        if part == "body" and (up < 0.22 or (along < 0.1 and up < 0.7)):
            c = (240, 236, 228)
        if part == "leg" and up < 0.45:
            c = (238, 234, 226)
    elif breed == "brahman":
        if (part == "body" and along < 0.3 and up > 0.45) or (part == "head" and up > 0.6):
            c = mix((*c, 255), (*p["dark"], 255), 0.5)[:3]
    return shade(c, a, rng)[:3]


def pig_colour(breed, p, part, a, rng):
    across, up, along = a
    c = p["base"]
    if part == "snout":
        c = p.get("snout", scale((*c, 255), 0.9)[:3] if breed != "berkshire" else (232, 206, 200))
        return c
    if breed == "berkshire":
        if part == "leg" and up < 0.5:
            c = (238, 232, 226)
        if part == "head" and abs(across - 0.5) < 0.12 and along < 0.1 and up < 0.7:
            c = (238, 232, 226)
    elif breed == "old_spot":
        if part != "leg" and patches(part, a, 23, 2.6, 0.72):
            c = p["spots"]
    elif breed == "mangalica":
        curl = ((int(across * 24) + int(along * 24) + int(up * 24)) % 3)
        c = scale((*c, 255), (0.86, 1.0, 1.1)[curl])[:3]
    if part == "leg" and up < 0.18:
        c = (70, 60, 56) if breed in ("berkshire", "mangalica") else (150, 110, 96)
    return shade(c, a, rng, 0.08)[:3]


def sheep_colour(breed, p, part, a, rng):
    across, up, along = a
    if part == "head":
        c = p["face"]
        if p.get("pale_jaw") and up < 0.3:
            c = mix((*c, 255), (220, 200, 170, 255), 0.6)[:3]
        if p.get("speckled") and rng.random() < 0.12:
            c = (120, 120, 120)
        return shade(c, a, rng, 0.06)[:3]
    if part == "leg":
        if up < 0.12:
            return (50, 44, 40)
        return shade(p["legs"], a, rng, 0.06)[:3]
    # The shorn body: pale skin with a hint of the breed's colour.
    return shade(mix((216, 190, 178, 255), (*p["face"], 255), 0.15)[:3], a, rng, 0.06)[:3]


def chicken_colour(breed, p, part, a, rng):
    across, up, along = a
    if part == "beak":
        return (230, 176, 60) if breed != "australorp" else (70, 66, 60)
    if part == "wattle":
        return (204, 38, 34)
    if part == "leg":
        return p["legs"]
    c = p["base"]
    if breed == "barred_rock":
        band = int((along * 7 + up * 5 + (0.5 if part == "wing" else 0)) * 2) % 2
        c = p["bar"] if band else c
    elif breed == "australorp" and part in ("body", "wing") and up > 0.6:
        c = mix((*c, 255), (*p["sheen"], 255), 0.5)[:3]
    elif breed == "light_sussex" and part == "head" and up < 0.45 and along > 0.3:
        c = p["hackle"] if int(across * 8) % 2 else c
    elif breed == "rhode_island_red" and part == "wing":
        c = scale((*c, 255), 0.82)[:3]
    if "tail" in p and part == "body" and along > 0.8 and up > 0.55:
        c = p["tail"]
    return shade(c, a, rng, 0.07)[:3]


SPECIES = {"cow": (CATTLE, cattle_colour), "pig": (PIGS, pig_colour), "sheep": (SHEEP, sheep_colour),
           "chicken": (CHICKENS, chicken_colour)}


# ---------------------------------------------------------------- per-face features

def put(img, rect, i, j, c):
    x0, y0, w, h = rect
    if 0 <= i < w and 0 <= j < h:
        img.set(x0 + i, y0 + j, (*c[:3], 255))


def features(species, breed, p, img, layout):
    rects = {}
    for part, u, v, w, h, d, turned in layout:
        rects[part] = {name: rect for name, (rect, _) in faces(u, v, w, h, d).items()}
    face = rects["head"]["north"]
    eye, white = (22, 20, 22), (236, 232, 222)
    if species == "cow":
        light_face = breed in ("hereford",) or (breed == "holstein")
        muzzle = {"angus": (60, 56, 56), "highland": (90, 60, 44), "brahman": (70, 68, 66)}.get(breed, (214, 164, 150))
        for i in range(2, 6):
            for j in range(5, 8):
                put(img, face, i, j, muzzle)
        if breed == "jersey":
            for i in range(1, 7):
                put(img, face, i, 4, (226, 210, 186))
        put(img, face, 2, 6, (40, 30, 30))
        put(img, face, 5, 6, (40, 30, 30))
        for i in (1, 6):
            put(img, face, i, 3, eye)
            put(img, face, i, 2, white if not light_face else (180, 170, 160))
        if breed == "highland":
            # A forelock hanging over the eyes.
            for i in range(0, 8):
                for j in range(0, 3 + (i % 2)):
                    put(img, face, i, j, scale((178, 94, 42, 255), 0.85 + 0.3 * ((i * 7 + j * 3) % 5) / 5))
    elif species == "pig":
        for i in (1, 6):
            put(img, face, i, 3, eye)
            put(img, face, i, 2, white if breed != "large_white" else (210, 170, 160))
        snout = rects["snout"]["north"]
        nostril = (60, 40, 40) if breed not in ("berkshire", "mangalica") else (20, 16, 16)
        put(img, snout, 1, 1, nostril)
        put(img, snout, 2, 1, nostril)
    elif species == "sheep":
        for i in (0, 5):
            put(img, face, i, 2, eye)
        for i in range(2, 4):
            put(img, face, i, 4, p["nose"])
            put(img, face, i, 5, scale((*p["nose"], 255), 0.8))
    elif species == "chicken":
        for i in (0, 3):
            put(img, face, i, 2, eye)
    # Hooves and feet: the soles too.
    if species in ("cow", "pig", "sheep"):
        sole = rects["leg"]["bottom"]
        for i in range(4):
            for j in range(4):
                put(img, sole, i, j, (40, 36, 34))


def coat(species, breed):
    w, h, layout = LAYOUTS[species]
    palette, colour = SPECIES[species]
    p = palette[breed]
    img = Img(w, h)
    rng = rng_for("breed", species, breed)
    for part, u, v, bw, bh, bd, turned in layout:
        for name, (rect, local) in faces(u, v, bw, bh, bd).items():
            x0, y0, rw, rh = rect
            for j in range(rh):
                for i in range(rw):
                    a = animal_coords(local(i, j), turned)
                    img.set(x0 + i, y0 + j, (*colour(breed, p, part, a, rng), 255))
    features(species, breed, p, img, layout)
    return img


def generate(lang, h):
    for species, (palette, _) in SPECIES.items():
        for breed in palette:
            h["save"](coat(species, breed), "entity", "breed", breed + ".png")
