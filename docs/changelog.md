---
title: Changelog
nav_order: 98
---

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
- **Seasons from Serene Seasons** (now a required mod): its sub-seasons are Tilth's months, so the colours, snow
  and ice you see and the climate crops grow in agree. Tilth's own snow, ice, leaf colours and calendar settings are
  gone, as are `/tilth time skip` and `/tilth time set` (use `/season set`).
- **Jade and WTHIT** tooltips for crops (growth stage, ripeness, what is wrong), farmland (texture, moisture)
  and animals (sex, condition, and how long until a pregnant female gives birth).
- **Breeds** of cattle, pigs, sheep, chickens and horses, each with its own coat: Holstein, Jersey, Highland, Angus,
  Hereford and Brahman cattle; Large White, Berkshire, Tamworth, Old Spot and Mangalica pigs; Merino, Suffolk,
  Hebridean, Soay and Herdwick sheep (with their natural wool colours); Leghorn, Rhode Island Red, Australorp, Light
  Sussex and Barred Rock chickens; Shire, Arabian, Friesian, Fjord and Mustang horses. Animals take a breed that suits
  their biome's climate, young inherit one, and a breed shapes the genome (milk, eggs, size, thrift). Kept outside its
  climate an animal loses condition faster.
- **Mendelian crop genetics:** every plant has a diploid genotype: four additive genes each for growth, yield,
  drought and frost tolerance, a dominant disease-resistance gene (cuts infection by the 17 diseases with resistant
  varieties) and a recessive semi-dwarfing gene (more grain in wheat, barley, oats and rice). Seeds inherit one copy of
  each gene from each parent. Saved seed follows the crop's mating system: self-pollinated crops self and soon breed
  true; cross-pollinated crops (maize, rye, sunflowers, brassicas...) take pollen from neighbouring plants, with hybrid
  vigour from wide crosses and inbreeding depression from selfing; potatoes and garlic are clones. Tooltips show the
  generation (F1 hybrid, F3 segregating, true-breeding) and the resistance and semi-dwarf genes.
- **Livestock genetics** follow the infinitesimal model: young take their parents' average plus a random Mendelian
  deviation, so selection shifts a herd over generations.
- **Gene splicing:** a Gene Splicer makes a controlled cross of two seeds into a Spliced Embryo, grown into F1 seed in a Genetic
  Incubator. Rarely, something else grows in there, escapes and slimes about for a few blocks before falling apart.
- **Pollinators:** insect-pollinated crops set more seed with flowers and working bees nearby.
- **Post-harvest:** grain comes off the field with a moisture content (shown in its tooltip). Dry it on a Drying Rack.
  Grain above 14% moisture goes mouldy in store, and grain in chests, barrels and inventories also loses a little each
  day to rodents and weevils (worked out when the chest is next opened).
- **Grain Silo** multiblock: twenty segments built as a 2x2 tower five blocks high form one corrugated steel silo with
  a conical roof. It holds over 40,000 grain, dries it by aeration, cuts spoilage to a quarter and keeps rodents out.
  Fill and empty it by hand or with hoppers; comparators read how full it is; a broken controller keeps its grain on
  the dropped item.
- **Pasture and veterinary care:** grazing wears pasture down, so rotate paddocks. Cattle, pigs and poultry can catch
  diseases (bovine respiratory disease, swine influenza, coccidiosis). Diagnose with the microscope, treat with a
  Veterinary Draught, and quarantine with a sneak-right-click of a lead.
- **Weeds:** fields grow weeds that compete with crops for light, water and nitrogen, held back by canopy, mulch
  and cover crops. Hoe a crop to weed a 3x3 patch; tillage clears them too.
- **Cover crops and green manure:** rye, oats, clover and alfalfa smother weeds; sneak-hoe a green crop to work it in
  as nitrogen-rich residue.
- **Greenhouses:** a glass roof over a crop keeps off frost and warms it, but keeps out the rain.
- **Compost Bin:** greens and browns rot into compost, fastest at two browns to a green; turn it with a shovel.
- **Hay making:** hoe grass for cut grass, lay it in windrows to dry (rain can spoil it), gather dried hay for bales.
- **Wool and dairy:** shearing gives graded, coloured fleece (Merinos best); a Spinning Wheel makes yarn, four yarn
  make woollen cloth. Milk is graded by breed and condition; a Butter Churn makes butter and a Cheese Press ages cheese.
- **Market:** commodity prices move with the seasons and with announced events (droughts, gluts, fairs, hard
  winters). Market Stalls, built or found in villages, buy goods for coins, paying more for better quality.
- **Coins** in steps of nine: 9 copper pennies make a silver shilling, 9 shillings a gold crown, 9 crowns a gold
  sovereign. Nine craft into the next coin up and one breaks into nine. The market and the farm merchant deal in them.
- **Travelling Farm Merchant:** an occasional visitor in a straw hat who buys produce at market prices and sells
  named cultivar seed, veterinary draughts, compost and tack.
- **Breed shapes:** Highland cattle have long shaggy coats, forelocks and wide horns; Brahman a hump, dewlap and
  drooping ears; Mangalica pigs a curly fleece; Merinos a full fleece; Shire horses feathered hooves.
- **Farm Wagon:** a horse-drawn wagon that holds as much as a chest. Hitch it from the saddle like the plough.
- **Bench microscope:** sneak-use the microscope on a block to set it down, then right-click it with a plant sample.
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
- A multi-layer soil profile under each block; rainfall-intensity erosion.
- Seed drills, harrows and harvesters.

### Fixed
- The Gene Splicer, Spliced Embryo, Veterinary Draught and Spoiled Grain have their own icons instead of borrowed
  vanilla ones; the Genetic Incubator is a steel cabinet with a window that glows while it works; the escaped
  homunculus is pale and fleshy instead of a green slime.
- Chicken breed coats now keep vanilla's thin legs and toes, and their eyes sit above the beak where they show.

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
