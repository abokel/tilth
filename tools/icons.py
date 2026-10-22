"""Item icons, mob effect icons and the plough entity texture."""
from pixelart import Img, mix, rng_for, scale

WOOD = (150, 108, 62, 255)
IRON = (168, 170, 176, 255)
STONE = (140, 140, 136, 255)
GLASS = (200, 225, 235, 140)
BOWL = (136, 92, 52, 255)


def c4(c):
    return c if len(c) > 3 else (*c, 255)


def done(img, outline=True):
    if outline:
        img.outline(0.45)
    return img


# ------------------------------------------------------------------ crop produce and seeds

def seeds(color, shape="oval"):
    color = c4(color)
    img = Img()
    spots = [(5, 6), (9, 5), (7, 9), (11, 9), (4, 11), (8, 12), (12, 12)]
    for i, (x, y) in enumerate(spots):
        col = scale(color, 0.92 + 0.05 * (i % 3))
        if shape == "oval":
            img.ellipse(x + 0.5, y + 0.5, 1.6, 1.0, col)
        elif shape == "round":
            img.ellipse(x + 0.5, y + 0.5, 1.3, 1.3, col)
        elif shape == "long":
            img.ellipse(x + 0.5, y + 0.5, 1.9, 0.8, col)
        elif shape == "striped":
            img.ellipse(x + 0.5, y + 0.5, 1.7, 1.0, (64, 62, 60, 255))
            img.set(x, y, (225, 225, 220, 255))
        elif shape == "kernel":
            img.rect(x, y, x + 1, y + 1, col)
            img.set(x, y, scale(col, 1.2))
    return done(img)


def legume_pile(color, r=1.6, shape="round"):
    color = c4(color)
    img = Img()
    rows = [(4, 13, [3.5, 6.5, 9.5, 12.5]), (3, 10.5, [5, 8, 11]), (1, 8, [6.5, 9.5])]
    for _, y, xs in rows:
        for i, x in enumerate(xs):
            col = scale(color, 0.9 + 0.06 * (i % 3))
            if shape == "kidney":
                img.ellipse(x, y, r + 0.3, r - 0.2, col)
            elif shape == "peanut":
                img.ellipse(x - 0.8, y, r * 0.9, r * 0.9, col)
                img.ellipse(x + 0.8, y - 0.3, r * 0.9, r * 0.9, scale(col, 1.05))
            else:
                img.ellipse(x, y, r, r, col)
    return done(img)


def sheaf(stalk, head, awns=False):
    stalk, head = c4(stalk), c4(head)
    img = Img()
    for dx in (-2, 0, 2):
        img.line(4 + dx, 15, 10 + dx, 4, scale(stalk, 0.95 + 0.04 * dx))
        img.ellipse(10.5 + dx, 3.5, 1.2, 2.0, head)
        if awns:
            img.line(11 + dx, 1, 12 + dx, 0, scale(head, 1.15))
    img.line(5, 10, 9, 12, (190, 150, 90, 255))
    return done(img)


def grain_head(color, stalk=(160, 150, 90)):
    img = Img()
    img.line(5, 15, 8, 9, c4(stalk))
    img.ellipse(9.5, 6, 3.0, 4.0, c4(color))
    for x, y in ((8, 4), (10, 5), (9, 7), (11, 8), (8, 8)):
        img.set(x, y, scale(c4(color), 1.2))
    return done(img)


def cob():
    img = Img()
    img.ellipse(8.5, 8, 3.0, 6.5, (238, 200, 70, 255), shade=0.3)
    for y in range(3, 14, 2):
        for x in range(6, 12, 2):
            if img.opaque(x, y):
                img.set(x, y, (250, 222, 110, 255))
    img.line(4, 14, 6, 4, (120, 165, 80, 255))
    img.line(13, 14, 11, 4, (110, 150, 70, 255))
    return done(img)


def bulb(color, tip=(120, 160, 90), cloves=False):
    color = c4(color)
    img = Img()
    img.ellipse(8, 10, 5.0, 4.5, color)
    img.line(8, 5, 8, 1, c4(tip))
    img.line(9, 5, 10, 2, scale(c4(tip), 0.9))
    if cloves:
        img.line(8, 7, 8, 13, scale(color, 0.82))
        img.line(5, 8, 6, 12, scale(color, 0.86))
        img.line(11, 8, 10, 12, scale(color, 0.86))
    for x in (6, 8, 10):
        img.set(x, 15, (200, 190, 160, 255))
    return done(img)


def round_veg(color, top=(70, 130, 50)):
    img = Img()
    img.ellipse(8, 9, 5.5, 5.0, c4(color), shade=0.45)
    img.set(6, 6, scale(c4(color), 1.45))
    for x, y in ((8, 3), (6, 4), (10, 4), (8, 4)):
        img.set(x, y, c4(top))
    return done(img)


def leafy_head(color, vein, frilly=False):
    img = Img()
    img.ellipse(8, 8.5, 6.5, 6.0, c4(color), shade=0.35)
    for a in ((8, 3, 8, 14), (3, 8, 13, 8), (4, 4, 12, 12)):
        img.line(*a, c4(vein))
    if frilly:
        for x, y in ((2, 7), (14, 9), (5, 2), (11, 2), (3, 12), (13, 13)):
            img.set(x, y, scale(c4(color), 1.2))
    return done(img)


def root_veg(color, top=(80, 140, 60)):
    img = Img()
    img.ellipse(8, 8, 4.0, 4.5, c4(color), shade=0.35)
    img.line(8, 12, 8, 15, scale(c4(color), 0.9))
    for dx in (-2, 0, 2):
        img.line(8, 4, 8 + dx, 0, c4(top))
    return done(img)


def boll():
    img = Img()
    img.ellipse(6, 7, 3.0, 3.0, (250, 250, 248, 255), shade=0.2)
    img.ellipse(10, 6, 3.0, 3.0, (245, 245, 242, 255), shade=0.2)
    img.ellipse(8, 10, 3.2, 3.0, (252, 252, 250, 255), shade=0.2)
    for x, y in ((4, 12), (12, 11), (8, 14)):
        img.set(x, y, (110, 70, 40, 255))
    return done(img)


def bundle(color, accent=None, tie=(170, 120, 70)):
    color = c4(color)
    img = Img()
    for i, x in enumerate(range(5, 12)):
        img.line(x, 15, x + (i - 3) // 2, 1, scale(color, 0.9 + 0.04 * (i % 3)))
        if accent:
            img.set(x + (i - 3) // 2, 1 + (i % 2), c4(accent))
    img.line(4, 9, 12, 9, c4(tie))
    img.line(4, 10, 12, 10, scale(c4(tie), 0.85))
    return done(img)


def powder(color):
    color = c4(color)
    img = Img()
    img.ellipse(8, 11, 6.5, 4.0, color, shade=0.3)
    img.ellipse(8, 9, 4.0, 3.0, scale(color, 1.05), shade=0.3)
    rng = rng_for("powder", color)
    for _ in range(8):
        x, y = rng.randrange(3, 13), rng.randrange(7, 14)
        if img.opaque(x, y):
            img.set(x, y, scale(color, rng.choice((0.9, 1.12))))
    return done(img)


def lumps(color):
    color = c4(color)
    img = Img()
    for x, y, r in ((6, 10, 2.6), (10, 11, 2.3), (8, 7, 2.2), (11, 7, 1.6)):
        img.ellipse(x, y, r, r * 0.85, scale(color, 0.95 + 0.03 * x % 3), shade=0.4)
    return done(img)


def bottle(liquid):
    img = Img()
    img.rect(7, 1, 8, 2, (150, 110, 70, 255))
    img.rect(6, 3, 9, 4, GLASS)
    img.ellipse(7.5, 10, 4.5, 5.0, GLASS, shade=0)
    img.ellipse(7.5, 11, 4.0, 3.6, c4(liquid), shade=0.3)
    img.set(5, 8, (240, 250, 255, 220))
    return done(img)


def bowl(contents, bits=()):
    img = Img()
    img.ellipse(8, 9, 6.5, 2.4, c4(contents), shade=0.2)
    rng = rng_for("bowl", contents, tuple(bits))
    for b in bits:
        for _ in range(3):
            x, y = rng.randrange(4, 12), rng.randrange(8, 11)
            img.set(x, y, c4(b))
    for y in range(10, 15):
        w = 6.5 - (y - 10) * 0.9
        for x in range(int(8 - w), int(8 + w) + 1):
            img.set(x, y, scale(BOWL, 1.05 - (y - 10) * 0.06))
    return done(img)


def blob(color, flecks=None):
    img = Img()
    img.ellipse(8, 9, 5.5, 4.5, c4(color), shade=0.35)
    if flecks:
        for x, y in ((6, 8), (9, 10), (10, 7), (7, 11)):
            img.set(x, y, c4(flecks))
    return done(img)


def noodles(color, flecks=None):
    color = c4(color)
    img = Img()
    for i, y in enumerate(range(4, 14, 2)):
        for x in range(3, 14):
            dy = 1 if (x + i) % 4 < 2 else 0
            img.set(x, y + dy, scale(color, 0.92 + 0.04 * (i % 3)))
    if flecks:
        for x, y in ((5, 5), (9, 9), (12, 6), (7, 12)):
            img.set(x, y, c4(flecks))
    return done(img)


def loaf(color, crust):
    img = Img()
    img.ellipse(8, 9.5, 6.5, 4.0, c4(crust), shade=0.35)
    img.ellipse(8, 9, 5.5, 3.0, c4(color), shade=0.35)
    for x in (5, 8, 11):
        img.line(x, 7, x + 1, 9, scale(c4(crust), 0.8))
    return done(img)


def disc(color):
    img = Img()
    img.ellipse(8, 8.5, 6.5, 5.5, c4(color), shade=0.15)
    for x, y in ((6, 6), (10, 9), (7, 11), (11, 6)):
        img.set(x, y, scale(c4(color), 0.75))
    return done(img)


def wrap(color, filling):
    img = Img()
    for i in range(10):
        img.ellipse(4 + i * 0.8, 12 - i * 0.8, 2.8, 2.8, c4(color), shade=0.2)
    img.ellipse(12, 4, 2.2, 2.2, c4(filling), shade=0.2)
    return done(img)


def cube(color):
    img = Img()
    img.rect(4, 6, 11, 13, c4(color))
    img.rect(5, 3, 13, 5, scale(c4(color), 1.12))
    img.rect(12, 5, 13, 11, scale(c4(color), 0.82))
    return done(img)


def puffs(color):
    img = Img()
    for x, y in ((5, 6), (9, 5), (7, 9), (11, 9), (5, 12), (9, 12), (12, 13)):
        img.ellipse(x, y, 1.8, 1.6, c4(color), shade=0.2)
        img.set(x, y, (230, 190, 80, 255))
    return done(img)


def sticks(color):
    img = Img()
    for i, x in enumerate(range(4, 13, 2)):
        img.line(x, 3 + (i % 2), x + (1 if i % 2 else 0), 10, c4(color))
    img.rect(3, 10, 12, 14, (200, 40, 40, 255))
    return done(img)


def bar(color, flecks):
    img = Img()
    img.rect(2, 6, 13, 10, c4(color))
    img.rect(2, 5, 13, 5, scale(c4(color), 1.15))
    for x, y in ((4, 7), (7, 9), (10, 7), (12, 9), (5, 9)):
        img.set(x, y, c4(flecks))
    return done(img)


def sandwich(bread, filling):
    img = Img()
    img.rect(3, 4, 12, 7, c4(bread))
    img.rect(3, 8, 12, 8, c4(filling))
    img.rect(3, 9, 12, 12, scale(c4(bread), 0.95))
    return done(img)


# ------------------------------------------------------------------ tools

def knife():
    img = Img()
    img.line(4, 12, 11, 5, IRON)
    img.line(5, 12, 12, 5, scale(IRON, 1.15))
    img.line(2, 14, 4, 12, WOOD)
    img.line(3, 14, 4, 13, scale(WOOD, 0.85))
    return done(img)


def quern():
    img = Img()
    img.ellipse(8, 11, 6.0, 2.5, STONE, shade=0.3)
    img.ellipse(8, 8, 5.5, 2.3, scale(STONE, 1.08), shade=0.3)
    img.line(11, 7, 13, 2, WOOD)
    return done(img)


def probe():
    img = Img()
    img.line(3, 3, 11, 3, WOOD)
    img.line(7, 3, 7, 15, IRON)
    for y in range(9, 15, 2):
        img.set(8, y, scale(IRON, 0.8))
        img.set(6, y + 1, scale(IRON, 0.8))
    return done(img)


def chain_icon():
    img = Img()
    for i in range(8):
        x = 3 + i * 1.4
        y = 12 - i * 1.2
        img.ellipse(x, y, 1.3, 1.0, IRON if i % 2 else scale(IRON, 0.85), shade=0.2)
    img.line(13, 3, 15, 1, WOOD)
    return done(img)


def ard_icon():
    img = Img()
    img.line(2, 3, 12, 11, WOOD)
    img.line(3, 3, 13, 11, scale(WOOD, 0.85))
    img.line(11, 11, 14, 14, (60, 60, 64, 255))
    img.line(8, 8, 6, 13, WOOD)
    return done(img)


def plow_icon():
    img = Img()
    img.line(1, 6, 12, 6, WOOD)
    img.line(1, 7, 12, 7, scale(WOOD, 0.85))
    img.ellipse(11, 10, 3.0, 3.0, IRON, shade=0.4)
    img.line(9, 13, 14, 13, scale(IRON, 0.7))
    img.line(12, 6, 14, 2, WOOD)
    img.line(3, 7, 3, 12, WOOD)
    img.ellipse(3, 13, 1.6, 1.6, scale(WOOD, 0.9))
    return done(img)


# ------------------------------------------------------------------ registry of icons

WHITE_FLOUR = (240, 236, 226)
ICONS = {
    # seeds
    "durum_wheat_seeds": lambda: seeds((214, 180, 100)),
    "barley_seeds": lambda: seeds((200, 170, 105), "long"),
    "oat_seeds": lambda: seeds((214, 196, 140), "long"),
    "rye_seeds": lambda: seeds((150, 130, 100), "long"),
    "maize_seeds": lambda: seeds((240, 200, 70), "kernel"),
    "sorghum_seeds": lambda: seeds((170, 90, 60), "round"),
    "alfalfa_seeds": lambda: seeds((190, 160, 70), "round"),
    "clover_seeds": lambda: seeds((180, 140, 60), "round"),
    "sugar_beet_seeds": lambda: seeds((150, 120, 80), "round"),
    "onion_seeds": lambda: seeds((40, 38, 36)),
    "cabbage_seeds": lambda: seeds((90, 60, 50), "round"),
    "lettuce_seeds": lambda: seeds((210, 200, 180), "long"),
    "tomato_seeds": lambda: seeds((225, 210, 150)),
    "flax_seeds": lambda: seeds((150, 100, 60)),
    "cotton_seeds": lambda: seeds((70, 60, 50)),
    "sunflower_seeds": lambda: seeds((60, 60, 60), "striped"),
    # produce
    "durum_wheat": lambda: sheaf((210, 180, 100), (226, 190, 110), awns=True),
    "barley": lambda: sheaf((215, 190, 120), (222, 196, 120), awns=True),
    "oats": lambda: sheaf((215, 200, 150), (225, 210, 160)),
    "rye": lambda: sheaf((190, 170, 120), (180, 160, 115), awns=True),
    "paddy_rice": lambda: grain_head((215, 185, 100), (180, 170, 100)),
    "maize": cob,
    "sorghum": lambda: grain_head((150, 70, 45)),
    "soybeans": lambda: legume_pile((225, 205, 140), 1.5),
    "peas": lambda: legume_pile((140, 190, 80), 1.4),
    "chickpeas": lambda: legume_pile((220, 190, 130), 1.6),
    "lentils": lambda: legume_pile((190, 120, 60), 1.3),
    "beans": lambda: legume_pile((140, 50, 40), 1.6, "kidney"),
    "peanuts": lambda: legume_pile((200, 160, 110), 1.3, "peanut"),
    "alfalfa": lambda: bundle((100, 150, 70), (150, 110, 200)),
    "clover": lambda: bundle((90, 145, 70), (210, 80, 120)),
    "sugar_beet": lambda: root_veg((236, 228, 205)),
    "onion": lambda: bulb((200, 140, 70)),
    "garlic": lambda: bulb((240, 236, 226), (150, 170, 120), cloves=True),
    "cabbage": lambda: leafy_head((140, 185, 150), (200, 225, 200)),
    "lettuce": lambda: leafy_head((150, 210, 90), (200, 235, 150), frilly=True),
    "tomato": lambda: round_veg((215, 45, 35)),
    "flax": lambda: bundle((200, 180, 120), (110, 140, 230)),
    "cotton_boll": boll,
    # tools
    "quern": quern,
    "kitchen_knife": knife,
    "soil_probe": probe,
    "surveyors_chain": chain_icon,
    "ard": ard_icon,
    "plow": plow_icon,
    # amendments
    "manure": lambda: lumps((96, 70, 40)),
    "compost": lambda: powder((70, 50, 32)),
    "wood_ash": lambda: powder((150, 148, 144)),
    "agricultural_lime": lambda: powder((235, 232, 222)),
    "elemental_sulfur": lambda: powder((235, 215, 60)),
    "straw": lambda: bundle((220, 190, 110)),
    # processing
    "wheat_flour": lambda: powder(WHITE_FLOUR),
    "durum_semolina": lambda: powder((240, 220, 150)),
    "rye_flour": lambda: powder((200, 188, 165)),
    "cornmeal": lambda: powder((240, 205, 90)),
    "chickpea_flour": lambda: powder((235, 215, 160)),
    "lentil_flour": lambda: powder((225, 160, 100)),
    "milled_rice": lambda: seeds((245, 245, 238), "long"),
    "flax_fiber": lambda: bundle((215, 210, 195), tie=(150, 150, 150)),
    "vegetable_oil": lambda: bottle((230, 200, 60)),
    "peanut_butter": lambda: bottle((185, 130, 70)),
    "wheat_dough": lambda: blob((235, 220, 190)),
    "rye_dough": lambda: blob((190, 165, 130)),
    "pasta_dough": lambda: blob((240, 220, 140)),
    "protein_pasta_dough": lambda: blob((225, 200, 130), (190, 140, 80)),
    "masa_dough": lambda: blob((240, 215, 130)),
    "cornbread_batter": lambda: bowl((245, 210, 100)),
    "raw_pasta": lambda: noodles((240, 220, 140)),
    "raw_protein_pasta": lambda: noodles((222, 196, 128), (180, 130, 70)),
    "raw_fries": lambda: sticks((240, 230, 190)),
    "uncooked_rice": lambda: bowl((150, 190, 230), [(245, 245, 238)]),
    "soaked_beans": lambda: bowl((150, 190, 230), [(140, 50, 40)]),
    # foods
    "rolled_oats": lambda: seeds((225, 205, 160), "oval"),
    "soy_milk": lambda: bottle((245, 240, 225)),
    "cooked_pasta": lambda: noodles((248, 232, 170)),
    "protein_pasta": lambda: noodles((232, 210, 150), (170, 120, 60)),
    "pasta_marinara": lambda: bowl((240, 220, 150), [(200, 40, 30), (190, 50, 35)]),
    "rye_bread": lambda: loaf((150, 110, 70), (110, 75, 45)),
    "cornbread": lambda: loaf((245, 200, 80), (200, 150, 60)),
    "tortilla": lambda: disc((240, 220, 160)),
    "burrito": lambda: wrap((235, 215, 160), (200, 60, 40)),
    "porridge": lambda: bowl((230, 215, 180), [(200, 170, 120)]),
    "cooked_rice": lambda: bowl((248, 248, 242), [(230, 230, 222)]),
    "cooked_beans": lambda: bowl((150, 70, 50), [(120, 40, 30)]),
    "rice_and_beans": lambda: bowl((245, 245, 238), [(140, 50, 40), (120, 40, 30)]),
    "baked_beans": lambda: bowl((200, 90, 50), [(230, 150, 90)]),
    "lentil_soup": lambda: bowl((190, 120, 60), [(240, 140, 50), (220, 200, 150)]),
    "pea_soup": lambda: bowl((140, 180, 80), [(120, 160, 60)]),
    "tomato_soup": lambda: bowl((210, 60, 40), [(240, 120, 90)]),
    "onion_soup": lambda: bowl((190, 140, 70), [(230, 200, 140), (150, 100, 60)]),
    "tomato_sauce": lambda: bowl((200, 40, 30), [(230, 90, 60)]),
    "hummus": lambda: bowl((225, 200, 140), [(200, 170, 60), (160, 120, 60)]),
    "garden_salad": lambda: bowl((130, 190, 80), [(210, 45, 35), (240, 130, 40)]),
    "coleslaw": lambda: bowl((230, 235, 200), [(240, 130, 40), (180, 210, 170)]),
    "tofu": lambda: cube((245, 242, 230)),
    "popcorn": lambda: puffs((250, 245, 225)),
    "roasted_maize": cob,
    "fries": lambda: sticks((240, 200, 90)),
    "roasted_sunflower_seeds": lambda: seeds((90, 80, 70), "striped"),
    "roasted_peanuts": lambda: legume_pile((170, 120, 70), 1.3, "peanut"),
    "garlic_bread": lambda: loaf((235, 200, 130), (170, 120, 60)),
    "granola_bar": lambda: bar((200, 160, 90), (130, 80, 40)),
    "peanut_butter_sandwich": lambda: sandwich((230, 200, 140), (170, 110, 60)),
}


def effect_icon(color, symbol):
    img = Img(18, 18)
    col = c4(color)
    if symbol == "heart":
        img.ellipse(6, 7, 3.2, 3.2, col)
        img.ellipse(11, 7, 3.2, 3.2, col)
        for y in range(8, 15):
            w = 7 - (y - 8)
            img.line(8.5 - w, y, 8.5 + w, y, scale(col, 1.0 - (y - 8) * 0.03))
    elif symbol == "hollow_heart":
        img.ellipse(6, 7, 3.2, 3.2, col)
        img.ellipse(11, 7, 3.2, 3.2, col)
        for y in range(8, 15):
            w = 7 - (y - 8)
            img.line(8.5 - w, y, 8.5 + w, y, col)
        img.ellipse(8.5, 9, 2.2, 2.2, (40, 30, 25, 255))
    elif symbol == "meat":
        img.ellipse(10, 7, 4.5, 4.0, col)
        img.line(6, 10, 3, 14, (235, 225, 210, 255))
        img.ellipse(3, 14, 1.3, 1.3, (235, 225, 210, 255))
    elif symbol == "bolt":
        img.line(11, 2, 6, 9, col)
        img.line(12, 2, 7, 9, col)
        img.line(6, 9, 11, 9, col)
        img.line(11, 9, 6, 16, col)
        img.line(12, 9, 7, 16, col)
    elif symbol == "swirl":
        import math
        for i in range(40):
            a = i * 0.35
            r = 1 + i * 0.16
            img.set(9 + math.cos(a) * r, 9 + math.sin(a) * r, col)
    elif symbol == "citrus":
        img.ellipse(9, 10, 6.0, 6.0, col)
        img.ellipse(9, 10, 4.5, 4.5, (250, 240, 170, 255))
        for a in range(0, 360, 60):
            import math
            img.line(9, 10, 9 + math.cos(math.radians(a)) * 4, 10 + math.sin(math.radians(a)) * 4, col)
        img.line(3, 3, 15, 15, (200, 30, 30, 255))
    elif symbol == "drop":
        img.ellipse(9, 11, 4.0, 4.0, col)
        for y in range(3, 9):
            w = (y - 3) * 0.6
            img.line(9 - w, y, 9 + w, y, col)
    elif symbol == "eye":
        img.ellipse(9, 9, 7.0, 3.5, (235, 235, 230, 255), shade=0)
        img.ellipse(9, 9, 2.6, 2.6, col)
        img.ellipse(9, 9, 1.2, 1.2, (20, 20, 20, 255), shade=0)
        img.line(2, 15, 16, 3, (40, 40, 60, 255))
    elif symbol == "bone":
        img.line(4, 14, 14, 4, col)
        img.line(5, 14, 14, 5, col)
        for x, y in ((3, 13), (4, 15), (13, 3), (15, 4)):
            img.ellipse(x, y, 1.5, 1.5, col)
        img.line(8, 9, 10, 11, (90, 70, 60, 255))
    img.outline(0.4)
    return img


def plow_texture():
    img = Img(64, 64)
    rng = rng_for("plow")
    for y in range(64):
        for x in range(64):
            if y < 36:
                base = WOOD
                f = 0.92 + 0.08 * ((y * 7 + (x // 9) * 3) % 5) / 4.0 + rng.uniform(-0.04, 0.04)
                if (y + x // 16) % 6 == 0:
                    f *= 0.85
            else:
                base = IRON
                f = 0.85 + rng.uniform(-0.06, 0.08) + (0.12 if (x + y) % 11 == 0 else 0)
                if rng.random() < 0.03:
                    base = (150, 90, 50, 255)
            img.px[y][x] = scale(base, f)
    return img
