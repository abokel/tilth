"""
Content of the Tilth Field Guide. One source, three outputs (see guide.py): the in-game Patchouli
book, the documentation website in docs/, and GitHub-wiki pages.

Markup: paragraphs separated by blank lines; lines starting with "- " are list items; **term** is a
highlighted term; [[entry_id|text]] links to another entry in the same category.
Page types: text, spotlight (item), crafting / smelting (recipe id), entity.
"""

T = "text"


def text(s):
    return {"type": T, "text": s}


def spot(item, s, title=None):
    page = {"type": "spotlight", "item": item, "text": s}
    if title:
        page["title"] = title
    return page


def craft(recipe, s=""):
    return {"type": "crafting", "recipe": recipe, "text": s}


def smelt(recipe, s=""):
    return {"type": "smelting", "recipe": recipe, "text": s}


def entity(eid, s):
    return {"type": "entity", "entity": eid, "text": s}


BOOK = {
    "name": "Tilth Field Guide",
    "subtitle": "Realistic Agronomy",
    "landing": "A practical handbook for farming the way the land actually works: soils that belong to their hillside, "
               "water that runs out, crops that follow the seasons and a diet that needs variety. New here? Start with "
               "Getting Started, then Your First Field.",
}

CATEGORIES = [
    {
        "id": "getting_started", "name": "Getting Started", "icon": "tilth:soil_probe",
        "description": "What Tilth changes, your first field step by step, the calendar and the tools.",
        "entries": [
            {"id": "introduction", "name": "What Tilth Changes", "icon": "minecraft:wheat", "pages": [
                text("Farming in Tilth follows real agronomy. Every block of ground has a **soil texture** (sand, silt, "
                     "clay and their mixtures) that sets how much water it holds, how fast it drains and how well it keeps "
                     "nutrients.\n\nCrops grow on **thermal time**: warmth, not ticks. A cold spring slows them, a frost can "
                     "kill them, and each crop has its own season."),
                text("Tilled soil remembers its **nutrients**, **pH**, **organic matter**, moisture and compaction, and "
                     "every crop draws on them. Water is **finite**: two sources no longer make a third outside oceans and "
                     "rivers.\n\nYour own body needs a varied **diet**, and well-grown food is more nourishing."),
                text("Nothing here is meant to be a grind: every rate is in the config, and the commands under "
                     "**/tilth** let you inspect everything. If something surprises you, look at it with the Soil "
                     "Probe or **/tilth crop**: the reason is always shown."),
            ]},
            {"id": "first_field", "name": "Your First Field", "icon": "tilth:durum_wheat_seeds", "pages": [
                text("**1. Choose ground.** Gentle lower slopes (footslopes) and level ground usually have the deepest, "
                     "best-watered soil. Thin, stony soils sit on hilltops and steep slopes. Loam and silt loam are the "
                     "easiest soils to farm."),
                spot("tilth:soil_probe", "**2. Test the soil.** Right-click any soil with the Soil Probe for a full report: "
                                         "texture, pH, nutrients and water."),
                craft("tilth:soil_probe"),
                text("**3. Till.** Use a hoe for single blocks, the Ard for a three-block strip, or a Moldboard Plough "
                     "behind a horse for whole fields. Avoid working wet clay: it smears into clods.\n\n**4. Check the "
                     "date** with /tilth calendar. The year starts in early spring."),
                text("**5. Sow at the right time.** Cool-season crops (wheat, barley, peas, cabbage) go in as soon as the "
                     "ground thaws. Warm-season crops (maize, beans, tomato) wait until after the last frost. Vanilla wheat "
                     "seeds, potatoes, carrots and beetroot seeds plant the realistic crop on Tilth farmland."),
                text("**6. Watch it grow.** Right-click a crop with an empty hand to see its growth stage, how far it is "
                     "to maturity and what is holding it back: water, a nutrient, pH, light or the weather.\n\n**7. "
                     "Feed the soil.** Spread manure or compost, and lime acidic ground."),
                text("**8. Harvest when ripe** (the last stage, golden for grains). The harvest tells you the yield in "
                     "real units, bushels per acre for grain, and grades the produce from Poor to Premium. Leave the "
                     "straw or residue on the field to return organic matter, then rotate to a different crop."),
            ]},
            {"id": "calendar", "name": "Calendar & Seasons", "icon": "minecraft:clock", "pages": [
                text("Tilth keeps a real 365-day calendar. By default a month lasts 6 game days, so one game day covers "
                     "about five calendar days and a year passes in 72 game days. Sleeping moves the calendar forward "
                     "with the night."),
                text("Each biome has a real-world climate: plains are like the US Corn Belt, deserts like the Sonoran, "
                     "jungles like the Amazon, taiga like inland Canada. Weather varies from day to day, with cold "
                     "snaps, heat waves, rain and snow, and it is colder at altitude."),
                text("Use **/tilth calendar** for the date and **/tilth climate** for the local climate, today's weather "
                     "and degree days.\n\nThe days and month length are configurable. Shorter months make crops grow "
                     "faster in play time."),
            ]},
            {"id": "tools", "name": "Tools of the Trade", "icon": "tilth:quern", "pages": [
                spot("tilth:quern", "The **Quern** grinds grain into flour, oilseeds into oil and limestone into lime. "
                                    "It stays in the crafting grid and wears out slowly."),
                craft("tilth:quern"),
                spot("tilth:kitchen_knife", "The **Kitchen Knife** cuts dough into pasta, potatoes into fries and flax "
                                            "into fibre. Make it from flint or iron."),
                craft("tilth:kitchen_knife_flint"),
                spot("tilth:surveyors_chain", "The **Surveyor's Chain** stakes out field plots of any shape and measures "
                                              "them in acres. See Surveying Plots."),
                craft("tilth:surveyors_chain"),
            ]},
        ],
    },
    {
        "id": "soils", "name": "Soils", "icon": "tilth:loam_topsoil",
        "description": "Texture, horizons, the hillslope catena, and how soil changes over time.",
        "entries": [
            {"id": "texture", "name": "Soil Texture", "icon": "tilth:loam_topsoil", "pages": [
                text("Soil is a mix of **sand** (gritty, drains fast, holds little), **silt** (smooth, holds water well) "
                     "and **clay** (sticky, holds water and nutrients but drains slowly). The mix decides the texture "
                     "class, one of the twelve USDA classes, plus **peat**."),
                spot("tilth:sand_topsoil", "**Sand and loamy sand** warm up early and are easy to work, but dry out fast, "
                                           "lose nitrogen to leaching and need frequent manure."),
                spot("tilth:loam_topsoil", "**Loams** (loam, silt loam, sandy loam) are the best all-round farm soils: "
                                           "good water, good drainage and good fertility."),
                spot("tilth:clay_topsoil", "**Clays** hold the most water and nutrients but stay wet and cold in spring, "
                                           "compact easily and smear if worked wet."),
                spot("tilth:peat", "**Peat** is half organic matter. It forms in cold, wet hollows, holds huge amounts of "
                                   "water and is naturally very acidic."),
                text("Texture also sets how far water seeps sideways from a ditch or pond: about two blocks through sand "
                     "and up to five or six through silts, clays and peat. Fine soils draw water by capillarity."),
            ]},
            {"id": "horizons", "name": "Horizons & Profiles", "icon": "tilth:loam_subsoil", "pages": [
                text("Dig down and the soil changes. **Topsoil** (the A horizon) is dark with humus and full of roots. "
                     "**Subsoil** (the B horizon) is lighter, often redder or yellower, and holds clay washed down from "
                     "above. Below that lies weathered rock."),
                spot("tilth:gley", "Where the ground stays waterlogged, iron in the subsoil is chemically reduced and turns "
                                   "grey-blue with rusty mottles: **gley**. It marks poorly drained land."),
                text("Topsoil is thin on hilltops and steep slopes, where it washes away, and deep on footslopes and "
                     "valley floors, where it collects. Tilling subsoil gives poor farmland: little humus, little "
                     "nitrogen."),
            ]},
            {"id": "catena", "name": "The Catena", "icon": "minecraft:grass_block", "pages": [
                text("A **catena** is the chain of soils down a hillside. Water, dissolved minerals and fine particles all "
                     "move downhill, so position on the slope decides the soil."),
                text("- **Summit:** stable, moderately deep, well drained.\n- **Shoulder:** the most eroded: thin, sandy, "
                     "low in organic matter.\n- **Backslope:** steep and thin.\n- **Footslope:** deeper, receives soil "
                     "from above.\n- **Toeslope:** deep, fine-textured, rich in humus."),
                text("- **Depression:** a hollow with no outlet. Wet, clayey and gleyed; in cold climates it fills with "
                     "peat.\n- **Floodplain:** silty river sediment over a shallow water table.\n\nThe parent material "
                     "(wind-blown loess, glacial till, river alluvium, lake clay, dune sand) sets the starting texture."),
                text("Use **/tilth catena** anywhere to see the slope, curvature, relief, hillslope position and the "
                     "soil profile the land implies. Worldgen builds every new chunk this way."),
            ]},
            {"id": "living_soil", "name": "Living Soil", "icon": "tilth:silt_loam_grass", "pages": [
                text("Soil you move does not stay the same. Over time it adapts to its new place:\n- Subsoil kept wet "
                     "turns into gley; gley that drains turns back.\n- Subsoil exposed to sky and plants slowly becomes "
                     "topsoil.\n- Topsoil buried under rock becomes subsoil."),
                text("- Waterlogged topsoil in a cool, wet climate builds up peat.\n- Bare topsoil on a slope loses its "
                     "fine particles in heavy rain, coarsening by one class, and the fines settle in the hollow below.\n"
                     "\nGrass and crops protect soil from erosion. Plough across slopes, not down them."),
            ]},
            {"id": "soil_test", "name": "Reading a Soil Test", "icon": "tilth:soil_probe", "pages": [
                text("The probe reports nutrients in **ppm** in the top 20 cm, as a soil laboratory does. Rough guides:\n"
                     "- **N** (nitrate): under 10 low, 20-40 optimum.\n- **P**: under 10 low, 20-40 optimum.\n- **K**: under "
                     "90 low, 130-200 optimum."),
                text("- **pH**: 6.0-7.0 suits most crops.\n- **Organic matter**: 3% or more is healthy. It feeds nitrogen "
                     "and holds water.\n- **CEC**: higher means the soil holds nutrients and resists pH change."),
                text("**Water** is shown as the volume fraction **θ** next to field capacity (full after drainage) and "
                     "wilting point (empty to plants). **Compaction** above about 40% restricts roots and drainage; "
                     "ploughing breaks it up."),
            ]},
        ],
    },
    {
        "id": "fertility", "name": "Fertility", "icon": "tilth:compost",
        "description": "Nutrients, pH, organic matter, amendments and rotation.",
        "entries": [
            {"id": "nutrients", "name": "Nutrients", "icon": "tilth:manure", "pages": [
                text("Plants take six nutrients from the soil:\n- **Nitrogen (N):** leaves and protein. Mobile: washes "
                     "out with drainage, worst in sand.\n- **Phosphorus (P):** roots and seed. Locked up in very acid or "
                     "very alkaline soil.\n- **Potassium (K):** water use and quality. Held by clay."),
                text("- **Sulfur, Calcium, Magnesium:** needed in smaller amounts; calcium matters for peanuts and "
                     "tomatoes.\n\nGrowth is limited by whichever nutrient is shortest (Liebig's law of the minimum). "
                     "Adding more of the others does nothing."),
                text("Each crop needs a different amount: maize and cabbage are heavy feeders, while legumes make most "
                     "of their own nitrogen. Micronutrients (iron, zinc, manganese) become scarce above pH 7.5, and "
                     "sensitive crops then yellow."),
            ]},
            {"id": "ph", "name": "Soil pH", "icon": "tilth:agricultural_lime", "pages": [
                text("pH controls how much of each nutrient plants can actually reach. Rain slowly acidifies soil, "
                     "especially in humid climates, and every soil drifts back toward its climate's natural pH over the "
                     "years, so liming must be repeated."),
                spot("tilth:agricultural_lime", "**Agricultural Lime** raises pH. Grind calcite or dripstone in a "
                                                "quern. Clay and humus-rich soils need more lime per step."),
                craft("tilth:agricultural_lime_from_calcite"),
                spot("tilth:elemental_sulfur", "**Elemental Sulfur** lowers pH, for potatoes (it reduces scab) and "
                                               "acid-loving crops."),
                text("Crop preferences:\n- **Alfalfa, sugar beet:** 6.5-8.\n- **Most crops:** 6-7.\n- **Potato, oats, "
                     "rye:** 5-6.5.\n\nLiming to 7.2 also controls clubroot in cabbage-family crops."),
            ]},
            {"id": "organic_matter", "name": "Organic Matter & Residue", "icon": "tilth:straw", "pages": [
                text("Humus (soil organic matter) slowly breaks down and releases nitrogen. This happens fastest in warm, "
                     "moist soil and right after tillage, which brings a short flush of fertility.\n\nCrop residue left "
                     "on the field becomes new humus over time."),
                text("Residue type matters. **Straw** is low in nitrogen, so soil microbes borrow nitrogen from the soil "
                     "to digest it, and the next crop may go short. **Legume residue** (clover, alfalfa, beans) is rich "
                     "in nitrogen and gives it back."),
                text("Mulch left on the surface protects soil from drying out. Ploughed in, residue decomposes much "
                     "faster."),
            ]},
            {"id": "amendments", "name": "Amendments", "icon": "tilth:compost", "pages": [
                spot("tilth:manure", "**Manure** is dropped by cows, pigs, sheep, goats, chickens and horses. It supplies "
                                     "nitrogen, phosphorus, potassium and organic matter."),
                spot("tilth:compost", "**Compost** is slower and gentler. It builds organic matter and supplies "
                                      "balanced nutrients."),
                craft("tilth:compost_from_manure"),
                spot("minecraft:bone_meal", "**Bone meal** is a phosphorus and calcium fertiliser here. It no longer "
                                            "makes crops grow instantly (configurable)."),
                spot("tilth:wood_ash", "**Wood Ash**, from burning sticks, adds potassium and calcium and raises pH."),
                smelt("tilth:wood_ash_from_smelting"),
                text("Right-click farmland (or a crop on it) with an amendment to spread one dose per block. Alfalfa and "
                     "clover hay work as **green manure**: nitrogen-rich residue for the next crop."),
            ]},
            {"id": "rotation", "name": "Crop Rotation", "icon": "tilth:clover", "pages": [
                text("Farmland remembers its last six crops. Rotating crop families breaks the cycles of soil-borne "
                     "diseases and pests (take-all in wheat, rootworm in maize, clubroot in cabbage)."),
                text("A classic rotation is **grain → legume → root crop → grain**. Legumes leave nitrogen behind. Root "
                     "crops loosen the soil. Grasses after grasses invite disease.\n\nCover crops of clover over winter "
                     "protect and feed the soil."),
            ]},
        ],
    },
    {
        "id": "water", "name": "Water", "icon": "minecraft:water_bucket",
        "description": "Finite water, irrigation, the water table, ponds and evaporation.",
        "entries": [
            {"id": "finite_water", "name": "Finite Water", "icon": "minecraft:water_bucket", "pages": [
                text("Water is a resource. Outside oceans and rivers, two water sources no longer create a third, so a "
                     "bucket taken is a bucket gone. Small ponds slowly evaporate in dry, hot weather. Lakes, and ponds "
                     "below the water table, persist."),
                text("Rain refills the land. During rain, water runs to the lowest point of each hollow. A closed hollow "
                     "on slow-draining ground (clay, compacted soil, rock) fills block by block into a **pond**. Flat, "
                     "poorly drained ground collects **puddles**."),
                text("All of this is configurable: where sources may regenerate, how fast ponds form and evaporate, and "
                     "how large a hollow may fill. Pumps and wells are planned for a later release."),
            ]},
            {"id": "irrigation", "name": "Irrigation & Water Table", "icon": "minecraft:bucket", "pages": [
                text("Fields draw water three ways:\n- **Rain:** what doesn't run off a slope soaks in.\n- **Seepage** "
                     "from nearby water: two blocks through sand, four through loam, five or more through silt, clay "
                     "and peat.\n- **Capillary rise** from a shallow water table."),
                text("The water table is shallow in valleys and near rivers and deep on hilltops, so lower slopes stay "
                     "moist through dry spells.\n\nCrops use water fastest when the weather is hot, dry and windy, and "
                     "when they are fully grown."),
                text("**Rice** grows best flooded: plant it with water directly beside the block. Other crops drown if "
                     "the soil stays saturated for too long."),
            ]},
            {"id": "drought", "name": "Drought & Waterlogging", "icon": "minecraft:dead_bush", "pages": [
                text("When roots cannot draw enough water, growth slows. Prolonged drought (about two weeks with almost "
                     "no available water) kills a crop.\n\nStress at **flowering** costs the most yield: the same "
                     "drought at tillering costs far less."),
                text("Waterlogged soil suffocates roots, and nitrogen is lost to the air (denitrification). Clay "
                     "depressions and toeslopes are most at risk. Plant water-tolerant crops there, such as rice, "
                     "clover and sorghum, or drain them."),
            ]},
        ],
    },
    {
        "id": "crops", "name": "Crops", "icon": "tilth:durum_wheat",
        "description": "How crops grow, when to plant, harvest and quality, and every crop in detail.",
        "entries": [
            {"id": "how_crops_grow", "name": "How Crops Grow", "icon": "minecraft:wheat_seeds", "pages": [
                text("Crops develop on **growing degree days**: each day adds its average temperature above the crop's "
                     "base temperature. Wheat counts from 0 °C, maize from 10 °C. Warm weather speeds them, cold weather "
                     "stops them."),
                text("Seeds need warm, moist soil to sprout. Warm-season seed sitting in cold, wet soil for weeks will "
                     "**rot**.\n\nThe ten visible stages follow the **BBCH scale** used by agronomists: emergence, leaves, "
                     "tillering, stem elongation, heading, flowering, grain fill, ripening, ripe."),
                text("Each day the crop grows as fast as its scarcest resource allows: water, a nutrient, pH or light, "
                     "adjusted for temperature, compaction and disease. Right-click a crop to see what is limiting it."),
                text("**Frost** kills tender crops (tomato, beans, maize) just below 0 °C. Hardy crops such as rye and "
                     "garlic survive deep frost, though all crops are most sensitive while flowering. **Heat** above "
                     "about 32-38 °C at flowering sterilises grain."),
            ]},
            {"id": "planting_calendar", "name": "Planting Calendar", "icon": "minecraft:clock", "pages": "PLANTING"},
            {"id": "harvest", "name": "Harvest & Quality", "icon": "minecraft:wheat", "pages": [
                text("Harvest when the crop reaches its final stage: right-click it, or break it. Alfalfa and clover are "
                     "cut at early bloom and regrow, and tomatoes give several pickings. Crops left standing long after "
                     "ripening shatter and rot."),
                text("The harvest message shows the yield in real units: bushels per acre for grain, hundredweight or "
                     "tons for vegetables, bales for cotton. Produce is graded **Poor, Fair, Good, Excellent or "
                     "Premium** by how well the crop was fed, watered and kept healthy."),
                text("The grade carries through milling and cooking. Premium food gives 20% more nutrition, Poor food 20% "
                     "less. Cereals also give **straw**, useful for compost, hay or mulch."),
            ]},
        ],
        "auto": "CROPS",
    },
    {
        "id": "tillage", "name": "Tillage", "icon": "tilth:plow",
        "description": "Hoe, ard and moldboard plough, compaction and working wet soil.",
        "entries": [
            {"id": "hoe_and_ard", "name": "Hoe & Ard", "icon": "tilth:ard", "pages": [
                text("A **hoe** tills one block. Tilling vanilla dirt or grass gives farmland of whatever soil the "
                     "terrain implies there."),
                spot("tilth:ard", "The **Ard** is the oldest plough: a pointed beam dragged by hand. It stirs a "
                                  "three-block strip ahead of you without turning the soil over. That is quick, but it "
                                  "buries little residue and loosens compaction only partly."),
                craft("tilth:ard"),
            ]},
            {"id": "plow", "name": "The Moldboard Plough", "icon": "tilth:plow", "pages": [
                entity("tilth:plow", "A three-bottom moldboard plough. It turns a three-block swath, buries residue and "
                                     "breaks compaction."),
                craft("tilth:plow"),
                text("**To plough:** place it, mount a horse, donkey or mule, and right-click the plough to hitch it. "
                     "You can also hitch it to yourself on foot, but it is slow work.\n\n**Sneak and right-click** to "
                     "lower or raise the shares. Lowered, it tills everything it is dragged over."),
                text("Draft resistance slows whatever is pulling. Raise the shares to travel, and right-click again to "
                     "unhitch.\n\nPloughing destroys crops in its path and turns them into residue. Hit the plough to "
                     "pick it up."),
            ]},
            {"id": "compaction", "name": "Compaction & Wet Clay", "icon": "tilth:clay_farmland", "pages": [
                text("Feet, hooves and landing on farmland compact it, and wet soil compacts far more. Compacted soil "
                     "drains poorly, runs off and stunts roots. Freeze-thaw cycles slowly loosen it, and tillage breaks "
                     "it at once."),
                text("Never work **wet clay**: the plough smears it into hard clods and makes compaction worse. Wait "
                     "until the soil has dried below field capacity. The Soil Probe shows the moisture."),
            ]},
        ],
    },
    {
        "id": "food", "name": "Food & Diet", "icon": "tilth:protein_pasta",
        "description": "Nutrition, deficiencies, quality and the new kitchen recipes.",
        "entries": [
            {"id": "diet", "name": "Your Diet", "icon": "minecraft:bread", "pages": [
                text("Your body keeps stores of eight nutrients: carbohydrate, protein, fat, fibre, vitamin A, vitamin C, "
                     "iron and calcium. Energy runs out within a day or two, while vitamins A, iron and calcium last "
                     "many days. Check yours with **/tilth diet**."),
                text("Every food tooltip shows what it provides (hold Shift for the full list). Eat a variety: grains for "
                     "energy, legumes and meat for protein and iron, vegetables for vitamins, milk and tofu for "
                     "calcium.\n\nA balanced diet makes you **Well Nourished**: two extra hearts."),
            ]},
            {"id": "deficiencies", "name": "Deficiencies", "icon": "minecraft:rotten_flesh", "pages": [
                text("- **Scurvy** (vitamin C): wounds heal at half speed and you mine slower.\n- **Anemia** (iron): you "
                     "move slower.\n- **Night Blindness** (vitamin A): darkness closes in.\n- **Brittle Bones** "
                     "(calcium): falls hurt 50% more."),
                text("- **Protein Deficiency:** weaker attacks.\n- **Low Energy** (carbohydrate or fat): slower, and you "
                     "tire faster.\n- **Digestive Distress** (fibre): you get hungry faster.\n- **Malnourished:** an "
                     "overall poor diet costs two hearts."),
                text("Milk does not cure them: only eating the missing nutrient does."),
            ]},
            {"id": "kitchen", "name": "The Kitchen", "icon": "tilth:durum_semolina", "pages": [
                text("Grind grain into flour with the quern, mix doughs with a water bucket, cut pasta and fries with the "
                     "knife, then cook them in a furnace, smoker or campfire. Better ingredients make better food: the "
                     "grade carries through."),
                craft("tilth:durum_semolina", "Durum wheat is the pasta wheat."),
                craft("tilth:pasta_dough"),
                craft("tilth:protein_pasta_dough_chickpea", "Chickpea or lentil flour turns pasta into a protein meal."),
                smelt("tilth:protein_pasta_from_smelting"),
                craft("tilth:masa_dough", "Cornmeal cooked with wood ash (nixtamalisation) becomes masa for tortillas."),
                craft("tilth:burrito"),
                craft("tilth:hummus"),
                craft("tilth:lentil_soup"),
            ]},
        ],
    },
    {
        "id": "plots", "name": "Surveying Plots", "icon": "tilth:surveyors_chain",
        "description": "Stake out fields of any shape and measure them in acres.",
        "entries": [
            {"id": "staking", "name": "Staking a Field", "icon": "tilth:surveyors_chain", "pages": [
                text("Hold the **Surveyor's Chain** and right-click the corner blocks of your field in order, walking "
                     "around it. Any shape works: rectangles, L-shapes, a field following a river. Sneak and right-click "
                     "a block to remove the last stake."),
                text("**Sneak and use the chain in the air** to record the plot. Rename the chain in an anvil first to "
                     "name the plot.\n\nWhile you hold the chain, stakes and plot boundaries are shown in particles. "
                     "Use it in the air without sneaking to see which plot you are standing in."),
                craft("tilth:surveyors_chain"),
            ]},
            {"id": "acres", "name": "Acres & Areas", "icon": "minecraft:map", "pages": [
                text("One block is one square metre, so an acre is about 4,047 blocks. A 64 by 64 field (four by four "
                     "chunks) is almost exactly one acre.\n\nThe area counts every block inside or on the boundary, so "
                     "staking the corners of a 10 by 10 field gives exactly 100 m²."),
                text("Gunter's chain, 66 feet long, was the surveyor's tool that defined the acre: ten square chains.\n\n"
                     "**/tilth plot info** reports a plot's area, perimeter, average soil test and the crops growing in "
                     "it."),
            ]},
            {"id": "managing", "name": "Managing Plots", "icon": "minecraft:writable_book", "pages": [
                text("**/tilth plot list** shows every plot. **here** and **info <name>** describe one, **rename** and "
                     "**delete** manage them, and **addmember** / **removemember** share them."),
                text("Plot protection (only owners and members may build or harvest inside) is available but off by "
                     "default: set plots.protection in the config."),
            ]},
        ],
    },
    {
        "id": "afflictions", "name": "Diseases & Pests", "icon": "minecraft:spider_eye",
        "description": "Fungi, oomycetes, bacteria, viruses, insects, nematodes and helpful soil life.",
        "entries": [
            {"id": "overview", "name": "The Affliction Model", "icon": "minecraft:fermented_spider_eye", "pages": [
                text("Thirty-one organisms are modelled from real plant pathology: rusts, blights, scab, clubroot, "
                     "viruses, aphids, beetles, rootworm, nematodes, slugs, and helpful mycorrhizae, rhizobia and "
                     "Trichoderma."),
                text("Each one has its hosts, the weather it likes (temperature, humidity, hours of wet leaves), soil "
                     "conditions, how it spreads, how long it survives without a host, the damage it does and how "
                     "farmers control it.\n\nThey are **off by default** while their visuals are made."),
                text("Turn them on with afflictions.enabled in the config. **/tilth affliction list** and **info** "
                     "describe each organism, **risk** shows today's infection risk for the crop you are looking at, "
                     "and **infect** / **clear** let you test them."),
                text("Rotation is the main defence. Soil- and residue-borne organisms build up when the same host is "
                     "grown again before they die out. Ploughing buries residue-borne ones."),
            ]},
        ],
        "auto": "AFFLICTIONS",
    },
    {
        "id": "reference", "name": "Commands & Config", "icon": "minecraft:command_block",
        "description": "Every /tilth command and the most useful config options.",
        "entries": [
            {"id": "commands", "name": "Commands", "icon": "minecraft:command_block", "pages": [
                text("**Look things up:**\n- /tilth calendar\n- /tilth climate\n- /tilth soil\n- /tilth catena\n- "
                     "/tilth water\n- /tilth crop\n- /tilth diet\n- /tilth plot ...\n- /tilth affliction ...\n\nMost act "
                     "on the block you look at."),
                text("**Debugging** (operators):\n- /tilth time skip <days>\n- /tilth time set <month> <day>\n- /tilth "
                     "soil set <property> <value>\n- /tilth soil amend <type>\n- /tilth crop setstage|grow|kill\n- "
                     "/tilth fillfield <crop> <radius>\n- /tilth debug hud"),
            ]},
            {"id": "config", "name": "Configuration", "icon": "minecraft:comparator", "pages": [
                text("config/tilth-common.toml (also Mods > Tilth > Config):\n- **calendar:** days per month, start "
                     "month.\n- **climate:** temperature offset, rain, latitude bands.\n- **water:** source rules, ponds, "
                     "puddles, evaporation."),
                text("- **soil:** worldgen, morphing, erosion, leaching, trampling.\n- **crops:** growth speed, yield, "
                     "frost and drought deaths, bone meal.\n- **diet**, **plots**, **livestock** (manure), and "
                     "**afflictions**."),
            ]},
        ],
    },
]
