# Changelog

All notable changes to Tilth are documented here. The format follows
[Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and versions follow
[Semantic Versioning](https://semver.org/).

## [Unreleased]

### Added
- **Radish**, a starter crop: ripens in two or three game days in almost any weather and soil, but yields little
  and never grades above Fair. New players get eight seeds, and grass often drops more.
- **Soil Lab** block. The probe's sneak-use takes a soil sample; the lab analyses it and returns a written
  Soil Test Report with every measurement plus lime and fertiliser advice.
- **Fertilizer Spreader**, a horse-drawn implement that spreads manure, compost, lime, ash, sulfur, bone meal or
  straw over a three-block swath, under growing crops too.
- **Soil Survey** ledger: file lab results from across a plot into one book. It shows plot averages and ranges,
  a whole-plot plan with lime and fertiliser totals for the plot's area (doses and spreader loads), spots that need
  separate treatment, how many samples the plot needs, and rotation advice from the crop history.
- **Thermometer** (hold it for the air temperature now; use it for today's range, frost risk and tomorrow's
  forecast) and **Soil Moisture Meter** (water content, plant-available water, waterlogging and how much to irrigate).
- **Microscope** and **Plant Samples**: cut a sample from a crop with the kitchen knife and look at the organisms
  on it, with a magnified view of each of the 31 diseases, pests and symbionts.
- **Peat fuel:** peat breaks into four Cut Peat (fuel, half a coal); dried in a furnace they become Peat Bricks
  (as good as charcoal). Silk touch still takes the block.
- **Alfalfa Hay Bale**: richer feed than grass hay (more condition per meal, more meals per bale).
- **Manure on the ground:** animals leave dung in layers like snow. Shovel it up, mix four with a water bucket into
  a Bucket of Slurry, then pour it on farmland (3x3) or load the spreader (nine doses a bucket). Dung left lying
  slowly rots into the soil.
- **Sexes and breeding:** every farm animal is male or female, and males look it (bull nose ring and horns, ram
  horns, boar tusks, rooster comb and tail). Only pairs breed; sheep and goats only in autumn; mammals carry
  their young for two to four days before giving birth, with litters for pigs and twins for sheep and goats.
- **Livestock husbandry:** animals have a body condition fed by grazing, hay bales and hand feeding. It decides
  breeding, milk (once a day), eggs, wool regrowth, manure, and meat yield and quality. Winter needs hay.
  `/tilth animal <target>` shows it.
- **Visible symptoms:** crops yellow when short of nitrogen, purple when short of phosphorus, wilt in drought and
  brown after frost. Farmland cracks when dry and shows standing water when saturated.
- **Seasons you can see:** oak and birch leaves colour through autumn and brown in winter, grass yellows, snow
  falls (drawn as snow, not rain, whenever the Tilth climate is freezing), settles, ponds freeze, and everything
  thaws in spring.
- **Jade and WTHIT** tooltips for crops (growth stage, ripeness, what is wrong), farmland (texture, moisture)
  and animals (sex, condition, and how long until a pregnant female gives birth).
- Option `plots.farmOnlyInPlots`: ground can only be tilled inside a surveyed plot by its owner or members.
- `/tilth bench plots <count>` for testing plot lookups at scale; `/tilth fillfield` now takes a radius up to 64.

### Changed
- **Water is infinite by default**, as in vanilla. Finite water is still available through `water.sourceCreation`
  and `water.evaporation`. Existing config files keep their old values.
- New players start with **full nutrition**.
- The **Soil Probe** has 63 uses. Its quick field test shows feel, moisture, a pH strip and compaction; nutrient
  levels now come from the Soil Lab.
- Ploughs (and the spreader) can only be pulled by a horse, donkey or mule, and are worked from the saddle:
  right-click to hitch, right-click again to lower or raise, and right-click on foot to unhitch.
- Textures have more depth: lit edges and shading on items, relief on soil, sunlit crop tops, fuller four-plane
  crop models, and randomly rotated soil blocks.

### Planned
- Pumps and wells for irrigation.
- Seed genetics and saved seed; disease-resistant cultivars.
- A multi-layer soil profile under each block; rainfall-intensity erosion.
- Seed drills, harrows and harvesters.

## [0.1.0] - 2026-10-22

First playable prototype for Minecraft 1.21.1 on NeoForge 21.1.

### Added
- **Soils.**
  - The twelve USDA texture classes plus peat, each as topsoil, subsoil, grass-covered soil and farmland, plus gley for waterlogged subsoil.
  - Water properties from the Saxton & Rawls (2006) pedotransfer functions.
- **Catena worldgen.**
  - Each new chunk is classified by hillslope position: summit, shoulder, backslope, footslope, toeslope, depression or floodplain.
  - Uses slope, curvature, local relief and landscape relief.
  - Regional parent materials: loess, glacial till, outwash, alluvium, lake clay, residual clay, dune sand, colluvium.
- **Living soil.** Gleying, oxidation, peat formation, pedogenesis of exposed subsoil, burial, and erosion with downslope deposition.
- **Soil chemistry.**
  - N, P, K, S, Ca and Mg in ppm, plus pH, organic matter and CEC.
  - Mineralisation, residue decomposition with nitrogen immobilisation, denitrification, leaching, phosphorus fixation, potassium buffering, and pH drift toward the climate's equilibrium.
- **Amendments:** manure (dropped by livestock), compost, bone meal as a P/Ca fertiliser, wood ash, agricultural lime, elemental sulfur, straw mulch, and green manure.
- **Water.**
  - Finite water: no new sources outside oceans and rivers, configurable.
  - Ponds that fill closed hollows during rain, and puddles on poorly drained ground.
  - Evaporation of small water bodies; groundwater-fed ponds persist.
  - Daily root-zone water balance: curve-number runoff, drainage, capillary rise from the water table, and irrigation seepage that reaches further through finer soils.
- **Calendar and climate.**
  - A real 365-day calendar with configurable compression.
  - A real-world climate for every vanilla biome, and a deterministic daily weather generator.
  - Lapse rate with altitude, optional latitude bands, Hargreaves ET0, and USDA hardiness zones.
- **28 crops:** cereals, legumes, roots and tubers, vegetables, oil and fibre crops.
  - Ten BBCH growth stages each; maize, sorghum and sunflower grow two blocks tall.
  - Development runs on growing degree days.
  - Daily growth follows Liebig's law of the minimum.
  - FAO-33 stage-wise yield response.
  - Frost, heat, drought, waterlogging and seed-rot deaths.
  - Legume nitrogen fixation.
  - Perennial forage regrowth.
- **Harvest** reports yield in bu/ac, cwt/ac, tons/ac, lb/ac or bales/ac.
- **Harvest quality grades** (Poor to Premium) that carry through quality-aware crafting, smelting, smoking and campfire recipes.
- **Vanilla seeds** plant the realistic crops on Tilth farmland.
- **Wild grass** drops seeds of crops suited to the local climate.
- **Tillage:**
  - Hoe.
  - Ard (hand scratch plough, three-block strip).
  - Animal-drawn three-bottom moldboard plough entity with draft resistance.
  - Effects on residue, compaction and mineralisation, and the wet-clay penalty.
- **Player diet.**
  - Eight nutrient stores that drain at realistic relative rates.
  - Nutrient profiles as a data map (`tilth:food_nutrition`).
  - Well Nourished, Malnourished, Scurvy, Anemia, Night Blindness (with fog), Brittle Bones, Protein Deficiency, Low Energy and Digestive Distress.
- **About 50 new foods and processing steps** with the quern and kitchen knife:
  - Pasta and protein pasta from durum semolina, tortillas from masa nixtamalised with wood ash, and burritos.
  - Tofu, soy milk, hummus, soups, porridge, rye bread, cornbread, fries, peanut butter and more.
- **Surveyor's Chain:**
  - Polygon field plots of any shape, measured in acres (Pick's theorem block counting).
  - Particle boundaries, plot reports, and optional plot protection.
- **Diseases, pests and fungi framework.**
  - 31 data-driven organisms with hosts, environmental envelopes, lifecycles, impacts and controls.
  - Risk engine using weather, field history and tillage.
  - Off by default.
- **Integrations:**
  - **Tilth Field Guide**, a Patchouli book (optional): crafted from a book and any seeds, and given to new players.
  - **JEI** (optional): Growing, Tillage and Soil Amendment recipe categories, plus information pages.
- **Documentation:** website (`docs/`) generated from the same source as the Field Guide.
- **Commands and config.**
  - `/tilth` inspection and debug commands, and a debug HUD.
  - `config/tilth-common.toml` with every simulation rate.
- **Development:** unit tests for the simulation core, a dependency-free asset generator, and a GitHub Actions build.

[Unreleased]: https://github.com/abokel/tilth/compare/v0.1.0...HEAD
[0.1.0]: https://github.com/abokel/tilth/releases/tag/v0.1.0
