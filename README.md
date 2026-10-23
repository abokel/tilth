# Tilth: Realistic Agronomy

A NeoForge 1.21.1 mod that turns Minecraft farming into agronomy. In the spirit of TerraFirmaCraft, but the focus is the farm: soils, water, crops, diet.

> **Status: prototype (0.1.0).** All systems below are implemented and playable. Diseases, pests and fungi are modelled and can be inspected and triggered with commands, but stay switched off until their visuals are ready.

## What it does

### Soils that belong to the landscape
- **Thirteen soil types**: all twelve USDA texture classes (sand, loamy sand, sandy loam, loam, silt loam, silt, sandy clay loam, clay loam, silty clay loam, sandy clay, silty clay, clay) plus peat. Each comes in **topsoil** (A horizon), **subsoil** (B horizon) and **grass-covered** forms, with **gley** (waterlogged Bg) beneath poorly drained ground.
- **Catena worldgen.** Every new chunk is analysed column by column for slope, curvature, local relief and landscape relief (sampled from the noise generator up to ~80 blocks away), and classified into a hillslope position: summit, shoulder, backslope, footslope, toeslope, depression or floodplain. The parent material comes from the region (loess plains, glacial till, outwash, alluvium, lake clays, residual tropical clays, dune sand), and the position shifts the texture:
  - Shoulders get thin, sandy, eroded soils.
  - Toeslopes and hollows get deep, clay-rich, humus-rich, gleyed soils.
  - Cold, wet basins form peat.
- **Soil morphs with its surroundings.** Placed soil slowly takes on the character of its new spot:
  - Subsoil kept wet turns to gley, and gley that drains turns back.
  - Exposed subsoil becomes topsoil; buried topsoil becomes subsoil.
  - Waterlogged ground in cold climates accumulates peat.
  - Bare soil on slopes loses fines in rain, and they settle downslope.
- **Water physics from real pedotransfer functions.** Field capacity, wilting point, saturation, conductivity and bulk density come from Saxton & Rawls (2006).

### Soil chemistry
Tilled soil tracks:
- **Nutrients:** N, P, K, S, Ca and Mg in ppm, the units a soil lab reports.
- **Chemistry:** pH, organic matter and CEC.
- **Water and structure:** moisture, compaction, tilth, crop residue and snowpack.
- **History:** the last six crops grown.

Each day the field goes through:
- **Nutrient cycling:**
  - Humus mineralisation (Q10 temperature response).
  - Residue decomposition. Straw with a high C:N ratio locks up nitrogen; legume residue releases it.
  - Denitrification in waterlogged soil.
  - Leaching by drainage water: nitrate moves freely, cations are held by the CEC.
  - Phosphorus fixation at extreme pH and potassium buffering by clay.
- **Soil structure:** slow pH drift toward the climate's weathering equilibrium, so lime has to be reapplied; freeze-thaw loosening of compaction.
- **Availability:** pH controls how much of each nutrient crops can reach (the Truog chart), and micronutrient lock-up in alkaline soil.

Amendments: compost, manure (dropped by livestock), bone meal (P and Ca rather than instant growth), wood ash, agricultural lime, elemental sulfur, straw mulch, and green manure (alfalfa, clover).

### Water
- **Finite water.** Two sources no longer create a third except in oceans and rivers. The mode and the infinite biomes are configurable.
- **Ponds and puddles.** Rain runs downhill to the bottom of the local hollow. If the hollow is closed and the ground drains slowly, it fills layer by layer into a pond and stops at the spill point. Flat, poorly drained or compacted ground gets puddles.
- **Evaporation.** Small isolated water bodies shrink from their edges in dry weather, scaled by the day's evapotranspiration. Lakes, and ponds fed by groundwater, persist.
- **Soil hydrology.** Each field runs a daily root-zone water balance:
  - Inputs: rain and snowmelt (minus curve-number runoff), lateral seepage from nearby water (its reach depends on soil texture), and capillary rise from a shallow water table (which is shallower in valleys).
  - Outputs: crop transpiration under FAO-56 water stress, evaporation reduced by mulch, and drainage limited by conductivity.
- Every rate is in `config/tilth-common.toml`. Pumps are planned for a later release.

### Calendar and climate
- **Calendar.** A real 365-day calendar with configurable compression. By default 6 game days make a month, so a year is 72 game days.
- **Climate.** Every vanilla biome has a real-world analogue (plains are the Corn Belt, deserts the Sonoran, jungles the Amazon, taiga interior Canada). Modded biomes get an estimate.
- **Weather.** A deterministic weather generator produces daily temperature, rain, snow, humidity and leaf wetness. It includes synoptic anomalies (cold snaps, heat waves), lapse-rate cooling with altitude, optional TerraFirmaCraft-style latitude bands, Hargreaves reference evapotranspiration, and USDA hardiness zones.
- **Catch-up.** Fields catch up exactly when chunks reload, because the weather is a pure function of world seed, day and place.

### Crops
28 species:

| Group | Crops |
| --- | --- |
| Cereals | common wheat, durum wheat, barley, oats, rye, rice, maize, sorghum |
| Legumes | soybean, field pea, chickpea, lentil, common bean, peanut, alfalfa, red clover |
| Roots and tubers | potato, carrot, beetroot, sugar beet, onion, garlic |
| Vegetables | cabbage, lettuce, tomato |
| Oil and fibre | sunflower, flax, cotton |

- **Ten visual growth stages** mapped to the BBCH scale. Maize, sorghum and sunflower grow two blocks tall.
- **Phenology** runs on growing degree days, with each crop's own base, optimum and cut-off temperatures. Seeds need warmth and moisture to germinate, and rot in cold wet ground.
- **Daily growth** follows Liebig's law of the minimum across water, six nutrients, pH, micronutrients and light, with compaction and disease applied on top.
- **Yield** follows the FAO-33 stage-wise response (`1 - Ya/Ym = Ky x deficit`), so drought at flowering costs far more than drought at tillering.
- **Killers:** frost (each crop has its own vegetative and flowering thresholds), heat at anthesis, prolonged drought, waterlogging, and standing overripe in the field.
- **Legumes fix nitrogen.** Fixation drops in acid soil and when soil nitrate is high.
- **Harvests** report yield in the crop's customary unit: bu/ac, cwt/ac, tons/ac, lb/ac or bales/ac. Alfalfa and clover regrow after cutting; tomatoes give several pickings.
- **Vanilla seeds** (wheat, potato, carrot, beetroot) plant the realistic version on Tilth farmland. Wild grass drops seeds of crops suited to the local climate.

### Tillage
- **Hoe**: one block, shallow.
- **Ard** (scratch plough): a three-block strip in front of you, by hand.
- **Moldboard plough** (an entity). Hitch it to the horse, donkey or mule you are riding, or drag it yourself. Sneak and right-click to lower the shares, and it turns a three-block swath as it moves. Draft resistance slows whatever is pulling it.
- **Each implement buries residue, loosens compaction and triggers a mineralisation flush differently.** Ploughing wet clay smears it into clods.

### Diet and food
- **Eight nutrients:** carbohydrate, protein, fat, fiber, vitamin A, vitamin C, iron and calcium.
- **Body stores.** Each nutrient has a store that drains at a realistic relative rate: energy within a day, fat-soluble vitamins and minerals over many days.
- **Effects.** A balanced diet makes you Well Nourished (+2 hearts). Deficiencies cause:
  - **Scurvy:** wounds heal at half speed and you mine slower.
  - **Anemia:** slower movement.
  - **Night blindness:** fog closes in when it is dark.
  - **Brittle bones:** +50% fall damage.
  - **Protein deficiency:** weaker attacks.
  - **Low energy:** you tire faster.
  - **Digestive distress:** you get hungry faster.
- **Nutrient profiles** are a data map (`data/tilth/data_maps/item/food_nutrition.json`), so packs can rebalance any food, vanilla or modded. Tooltips show them; hold Shift for the full list.
- **Harvest quality.** Produce is graded Poor through Premium from how well the crop was grown. Recipes carry the grade through, and better food is more nourishing.
- **About 50 new foods and processing steps**, using a quern and a kitchen knife that wear down as you use them. For example:
  - Durum semolina → pasta dough → pasta. Protein pasta adds chickpea or lentil flour.
  - Masa nixtamalised with wood ash → tortillas → burritos.
  - Also tofu, hummus, lentil and pea soups, porridge, rye bread, cornbread, fries and peanut butter.

### Plots in acres
- **Survey any shape.** Stake out a field of any polygonal shape with a **Surveyor's Chain** (a nod to Gunter's chain: 66 ft long, 10 square chains to the acre).
- **Areas count real blocks.** They use Pick's theorem, so staking the corners of a 10×10 field gives exactly 100 m², and a 64×64 square is about 1.01 acres. Areas are shown in acres, hectares and m².
- **Plot reports** give perimeter, sampled soil averages and the crops present.
- **Boundaries** are drawn in particles while you hold the chain.
- **Protection** for owners and members is available but off by default.

### Diseases, pests and fungi: the framework
**31 organisms**, defined in datapacks (`data/tilth/tilth/affliction/*.json`):

| Type | Organisms |
| --- | --- |
| Fungi | stem rust, leaf rust, Fusarium head blight, powdery mildew, take-all, early blight, corn smut, northern corn leaf blight, white mold, gray mold |
| Oomycetes | late blight, damping-off, onion downy mildew |
| Bacteria | common scab, black rot |
| Viruses | barley yellow dwarf, tomato mosaic |
| Protist | clubroot |
| Insects | aphids, Colorado potato beetle, European corn borer, western corn rootworm, wireworms, cabbage white, desert locust |
| Nematodes | soybean cyst nematode, root-knot nematode |
| Molluscs | slugs |
| Beneficial | mycorrhizae, rhizobia, Trichoderma |

**Each definition specifies:**
- **Hosts and transmission:** which crops it attacks and how it spreads.
- **Environmental envelope:** temperature, humidity, leaf-wetness hours, soil moisture and pH.
- **Lifecycle and survival:** how long it persists without a host, so rotation length matters.
- **Impact:** yield, growth and quality losses.
- **Management:** rotation break, tillage reduction and other practices.

The risk engine combines today's weather with **field history** (inoculum builds up when the same host is grown again) and tillage. Set `afflictions.enabled = true` to let them spread, or use `/tilth affliction ...` to inspect and test them.

## Getting started (players)
1. Install NeoForge 21.1.x for Minecraft 1.21.1.
2. Put `tilth-1.21.1-0.1.0.jar` in your `mods` folder.
3. Create a **new world**. Catena soils only generate in newly generated chunks; in old chunks, tilling vanilla dirt converts it to the soil the terrain implies.

**Your first field:**
1. Find good ground, ideally a loam or silt loam footslope. Craft a **Soil Probe** and right-click soil to get a lab-style report.
2. Till with a hoe, the **Ard**, or a **Moldboard Plough** hitched to your horse.
3. Plant at the right time of year. Check the date with `/tilth calendar`; cool-season crops (wheat, peas, cabbage) go in early spring, warm-season crops (maize, beans, tomato) after the last frost.
4. Right-click a crop with an empty hand to see its BBCH stage, progress to maturity, and what is limiting its growth.
5. Fertilise based on soil tests and rotate crops. Legumes leave nitrogen behind; grasses after grasses invite disease.
6. Water matters. Fields within reach of water get seepage; sand reaches two blocks, clay and peat up to five or six.

### Optional companions
Neither is required, but both make the mod much easier to learn:
- **[Patchouli](https://modrinth.com/mod/patchouli)** adds the **Tilth Field Guide**, an in-game book with 91 entries: a walkthrough for your first season, every soil, every crop with its planting window, fertility, water, tillage, food and the disease and pest models. Each player gets one the first time they join (`integrations.giveFieldGuideOnFirstJoin`); craft another from a book and any seeds.
- **[JEI](https://modrinth.com/mod/jei)** shows every Tilth recipe, including the quality-aware ones, plus three Tilth tabs: **Growing** (seed, harvest, temperatures, degree-days, pH, nutrient demand), **Tillage** (which tool turns which soil into which farmland) and **Soil Amendments** (what each fertiliser does per dose). Soils, tools, manure and straw have information pages.

## Documentation
- **[Website](https://abokel.github.io/tilth/):** the Field Guide online, with the same content as the in-game book. GitHub Pages builds it from `docs/` (a [Just the Docs](https://just-the-docs.com/) site).
- **[Wiki](https://github.com/abokel/tilth/wiki):** the same guide as GitHub wiki pages.
- **[Changelog](CHANGELOG.md):** what changed in each release.
- **[Design notes](docs/DESIGN.md):** how the simulation works, for contributors.

## Commands
Inspection commands are open to everyone. World-changing ones need permission level 2 (configurable).

| Command | What it does |
| --- | --- |
| `/tilth calendar` | Date, season, time scale |
| `/tilth climate [pos]` | Climate normals, today's weather, degree days, hardiness zone |
| `/tilth soil [pos]` | Soil test (farmland) or undisturbed estimate (soil block) |
| `/tilth soil set <n\|p\|k\|s\|ca\|mg\|ph\|om\|moisture\|compaction\|residue> <value>` | Edit the farmland you look at |
| `/tilth soil amend <amendment>` | Apply an amendment |
| `/tilth catena [pos]` | Hillslope analysis and the soil profile it implies |
| `/tilth water [pos]` / `/tilth water pond` | Hydrology report / test whether a hollow would hold water |
| `/tilth crop [pos]` | Full crop report |
| `/tilth crop setstage <0-9>` / `grow <days>` / `kill` | Debug the crop you look at |
| `/tilth time skip <days>` / `time set <month> <day>` | Move the calendar forward (fields catch up) |
| `/tilth fillfield <species> <radius>` | Till and plant a test field around you |
| `/tilth debug hud` | Live action-bar readout of what you look at |
| `/tilth diet` / `diet set <nutrient> <0-1>` | Your nutrient stores |
| `/tilth plot list\|here\|info\|rename\|delete\|addmember\|removemember` | Manage plots |
| `/tilth affliction list\|info\|risk\|infect\|clear` | Inspect and test the disease/pest models |

## Configuration
All settings are in `config/tilth-common.toml`, and also in the in-game Mods → Tilth → Config screen:

| Section | Settings |
| --- | --- |
| Calendar | days per month, start month |
| Climate | temperature offset, precipitation and ET multipliers, lapse rate, latitude bands |
| Water | source-creation mode, infinite biomes, ponds, puddles, evaporation and their rates, sampling radius, maximum pond area |
| Soil | catena worldgen, morphing, erosion, vanilla conversion, mineralisation, leaching, pH drift, root-zone depth, trampling, catch-up limit |
| Crops | growth rate, yield, nutrient demand, frost/drought/waterlogging deaths, bone meal behaviour, seed redirection, light requirement |
| Diet | on/off, effects, decay |
| Plots | metres per block, protection, limits |
| Livestock | manure interval |
| Afflictions | on/off, pressure |
| Debug | command permission level |
| Integrations | give the Field Guide on first join |

## Building from source
Requirements: a JDK 17+ to run Gradle. The Java 21 toolchain is downloaded automatically.

```bash
./gradlew build
```

Other tasks:
- `./gradlew runClient` launches the game.
- `./gradlew test` runs the unit tests (soil physics, texture triangle, polygon areas, calendar, catena, whole-season crop simulations).
- `./gradlew runServer` starts a dev server in `run-server/`.
- `./gradlew runClientJoinLocal` starts a second client that joins that server.

The jar lands in `build/libs/`.

All textures, models, blockstates, loot tables, tags, recipes, the nutrition data map and the affliction definitions are generated by a dependency-free Python script. It also writes the Patchouli Field Guide and the `docs/` website from one source, `tools/guide_content.py`, with crop and affliction pages built from the same data the mod uses:

```bash
python tools/generate_assets.py
python tools/generate_assets.py --wiki ../tilth.wiki   # also write GitHub wiki pages into a wiki checkout
```

JEI and Patchouli are only needed at compile time (JEI's API) and in the dev runs; the jar does not bundle them.

| Path | Contents |
| --- | --- |
| `com.claudacious.tilth.sim` | The simulation core: soil, climate, crop, plot and diet math. Pure Java with no Minecraft types, so it is unit-tested in isolation. |
| `world`, `block`, `item`, `entity`, `water`, `worldgen`, `plot`, `diet`, `quality`, `affliction`, `command`, `client` | Connect the simulation to Minecraft. |
| `compat` | Optional integrations: the JEI plugin and the Field Guide. |
| `docs/` | The documentation website, plus `DESIGN.md`, which explains the models. |

## Science behind it
- **Soil water:** Saxton, K.E. & Rawls, W.J. (2006). *Soil water characteristic estimates by texture and organic matter.* SSSAJ 70:1569-1578.
- **Crop water use:** Allen, R.G. et al. (1998). *Crop evapotranspiration.* FAO Irrigation and Drainage Paper 56.
- **Yield response:** Doorenbos, J. & Kassam, A.H. (1979). *Yield response to water.* FAO Irrigation and Drainage Paper 33.
- **Evapotranspiration:** Hargreaves, G.H. & Samani, Z.A. (1985). *Reference crop evapotranspiration from temperature.*
- **Temperature response:** Yan, W. & Hunt, L.A. (1999). *An equation for modelling the temperature response of plants.* Annals of Botany 84.
- **Growth stages:** Meier, U. (ed.) (2001). *Growth stages of mono- and dicotyledonous plants.* BBCH Monograph.
- **Texture and runoff:** USDA NRCS soil texture triangle; SCS curve number method.
- **Hillslope positions:** Ruhe, R.V. (1960); Conacher, A.J. & Dalrymple, J.B. (1977), the nine-unit land-surface model.

## Roadmap
- **Water technology:** pumps and wells for irrigation.
- **Disease visuals:** symptoms on crops, so afflictions can be enabled by default; plus disease resistance through seed genetics and saved seed.
- **Deeper soil:** a multi-layer profile under each block, and erosion and deposition tied to rainfall intensity.
- **Machinery:** seed drills, harrows and harvesting.
- **Integrations:** Jade/WTHIT tooltips for soil and crops.

## License
All Rights Reserved (default; change `mod_license` in `gradle.properties` and add a LICENSE file if you want to open-source it).
