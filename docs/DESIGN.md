# Tilth design notes

How the simulation works and how it connects to the game.

## Architecture

```
sim/            pure Java, no Minecraft types, unit tested
  calendar/     CalendarMath: ticks -> real 365-day calendar
  climate/      BiomeClimate normals, WeatherGenerator (deterministic), ClimateMath (GDD, ET0, temperature response)
  soil/         SoilTexture (USDA classes), Pedotransfer (Saxton-Rawls), SoilState, SoilWater, SoilChemistry,
                Amendment, Tillage, SoilBaseline
  catena/       CatenaAnalyzer (slope, curvature, relief -> hillslope position), ParentMaterial, SoilProfile
  crop/         CropSpecies (+ Crops table), CropState, CropGrowthModel, BBCH
  plot/         PolygonMath (shoelace + Pick's theorem, point-in-polygon, self-intersection)
  diet/         DietNutrient
  FieldSimulator  one real day of one block: snow -> water -> chemistry -> crop

world/          adapters: TilthCalendar (SavedData offset), ClimateService, TerrainService, MacroTerrain, SoilService
block/          soil, grass, farmland (+ block entity running the simulation), crop (+ block entity), puddle
worldgen/       CatenaSoilFeature (TOP_LAYER_MODIFICATION, added to all overworld biomes by a biome modifier)
water/          WaterSourceRules (CreateFluidSourceEvent), SurfaceWater (ponds, puddles, evaporation)
```

The simulation core takes plain numbers and returns plain numbers. Config values are copied into
`SimSettings` when the TOML config loads, so the core never touches NeoForge.

## Time

The calendar runs on the overworld's total game time plus an offset. Sleeping adds the skipped
night, and `/tilth time skip` adds days. The calendar never runs backwards, unlike `dayTime`, which
`/time set` can rewind.

A Minecraft day covers `365 / (12 × daysPerMonth)` real days. The default of 6 days per month gives
about 5.07 real days per game day.

Every process is computed per **real** day. A farmland block entity stores `simDay`, the real day it
has simulated up to. Whenever it is touched (random tick, harvest, probe, amendment), it steps whole
days up to the current calendar day, capped at `maxCatchUpDays`.

The weather of any day at any place is a pure function of (seed, day, x, z), with one exception:
days on which Minecraft weather actually rained are recorded and count as wet. So a field unloaded
for a season catches up to the same state it would have reached if it had stayed loaded.

## Weather

- **Temperature.** The seasonal mean of the biome's climate (sinusoid, coldest mid-January), plus a
  synoptic anomaly (two octaves of value noise, interpolated between 1 km weather cells), minus
  lapse-rate cooling with elevation and optional latitude cooling.
- **Rain.** Wet days are a Bernoulli draw per weather cell, with a probability that rises with the
  month's mean rainfall. Wet-day amounts are exponentially distributed, which preserves the monthly
  totals. Precipitation below 0.5 °C falls as snow and builds a snowpack, which melts at
  3.5 mm per °C per day.
- **ET0.** Hargreaves-Samani, using extraterrestrial radiation for the configured latitude.

## Soil water (`SoilWater`)

A single root-zone bucket (0.6 m by default), updated in this order each day:

1. **Runoff.** The SCS curve number method. Retention depends on the hydrologic soil group and
   shrinks with compaction, slope and antecedent wetness.
2. **Inputs.** Infiltration, plus irrigation seepage, plus capillary rise from a water table
   shallower than 1.5 m.
3. **Crop water stress.** FAO-56 Ks: TAW and RAW are computed over the current rooting depth.
4. **Evapotranspiration.** `Kc·Ks·ET0`, plus bare-soil evaporation `Ke·(1 − cover)·ET0`, reduced
   by surface residue mulch.
5. **Drainage.** Water above field capacity drains, limited by saturated conductivity (reduced by
   compaction) and by a shallow water table.
6. **Ponding.** Water above saturation becomes ponding (reported, and used for puddles).

## Soil chemistry (`SoilChemistry`)

**Pools.** Plant-available pools in ppm of the 0–20 cm plough layer, where
1 ppm = 2 × bulk density kg/ha.

**Each day:**

| Process | Model |
| --- | --- |
| Mineralisation | Organic N = OM % × 500 ppm. Rate 3.5 %/yr at 25 °C × Q10 = 2 temperature factor × moisture factor × tillage flush. Releases P (N/9) and S (N/8). Peat mineralises at 15 % of the mineral-soil rate. |
| Residue | Decays faster once incorporated. Net N = decomposed DM × (N% − 1.5 %): straw immobilises nitrogen, legume residue releases it. Part of the decomposed residue becomes humus. |
| Denitrification | Waterlogged soil loses 4 % of nitrate per day (scaled by temperature). |
| Leaching | Fraction leached = drainage / (drainage + stored water). Nitrate and sulfate are mobile; cations are held by the CEC, and sand loses more potassium. Drainage also acidifies the soil. |
| pH | Drifts toward the climate's equilibrium pH: about 8 in arid climates, about 5.4 under tropical rain, lower in cold humid conifer zones. The time constant is about 9 years. |
| P fixation | Faster below pH 5.5 and above pH 7.5. |
| K buffering | K moves toward an equilibrium set by clay content. |
| Ca and Mg | Buffered toward pH-dependent levels. |
| Compaction | Relaxes slowly, faster with freeze-thaw. |

**Uptake.** Crop uptake follows a logistic curve over development, scaled to the species' total
uptake at its reference yield. Crops draw 55 % of each nutrient from the plough layer; the rest is
assumed to come from deeper roots. Availability is scaled by the pH availability curve, root access
and soil moisture.

## Crop growth (`CropGrowthModel`)

- **Germination.** Needs thermal time above `tBase` and soil moisture above 35 % of the available
  water. More than 30 days in cold, wet soil before emerging rots the seed.
- **Development.** `dev = GDD / gddToMaturity`, using the horizontal cut-off GDD method. Nutrient
  stress does not slow it.
- **Daily growth factor.** The Yan-Hunt temperature response × min(water, waterlogging, nutrient
  sufficiency, pH suitability, micronutrients, light) × compaction × disease.
- **Yield.** FAO-33 stage-wise response. Development-weighted mean stress is accumulated in four
  windows with Ky = 0.25 (vegetative), 0.6 (flowering), 0.45 (yield formation) and 0.1 (ripening).
  Yield factor = Π(1 − Ky·stress) × acute events (heat at anthesis, frost, standing overripe).
- **Quality grade.** A development-weighted average of nutrient sufficiency, water, pH and disease.
- **Legume fixation.** Covers up to `fixationShare` of the N demand. It is reduced by acid soil,
  high soil nitrate and drought.

The calibration tests (`CropSeasonTest`) check timing and yields:

| Crop and treatment | Result |
| --- | --- |
| Spring wheat on unfertilised loam, plains climate | about 100 days to maturity, about 30 bu/ac |
| Maize sown 10 May | about 140 days |
| Maize, unfertilised vs manured | about 70 bu/ac vs about 135 bu/ac |
| Barley on acid soil | much worse than on good soil |
| Soybean on nitrogen-starved soil | still fixes over 50 kg N/ha |
| Unirrigated lettuce in desert sand | fails |

## Catena (`CatenaAnalyzer`, `CatenaSoilFeature`)

**Measurements.** The feature reads terrain heights for the chunk plus a 16-block margin; the margin
lies in neighbouring chunks of the 3×3 world-generation region. Trees, plants, snow, ice and water
are skipped. For each column it computes:
- slope over a 4-block baseline;
- local relief against the 13×13 mean;
- curvature (mean height of the ring at distance 2, minus the centre);
- landscape relief against a Gaussian-weighted mean of noise-generator heights on a 32-block grid
  within about 80 blocks (`MacroTerrain`, cached).

**Classification.** These are classified into the Ruhe/Conacher hillslope elements.

**Profile.** `SoilProfile` then picks a parent material for the region (smooth noise, patches
hundreds of blocks wide) and builds the profile:
- The hillslope position shifts the topsoil texture.
- Humid climates weather more clay.
- Clay accumulates in the B horizon (illuviation).
- A drainage class is derived from position, texture and rainfall.
- Topsoil and subsoil depths are set.
- Poorly drained profiles get gley.
- Cold, wet hollows get peat.

## Surface water (`SurfaceWater`)

Every `surfaceWaterInterval` ticks, a few random columns near each player are sampled.

**In rain**, water follows the steepest descent to the bottom of the local hollow:
- **Closed depression** (a breadth-first search at the water line stays within `maxPondArea`):
  gains one water block, at the lowest unfilled layer, with probability `pondChance × (1 − permeability)`.
  Permeability comes from the soil's Ksat and compaction.
- **Open flat ground** that drains poorly gets a puddle block.

**In dry weather**, a small isolated water body (no more than `maxEvaporatingBodySize`, outside
infinite biomes and above sea level) loses an edge block with probability ∝ ET0. Puddles dry out on
random ticks.

## Diet

Each nutrient's store drains linearly over `storeDays`:

| Nutrient | Store lasts (game days) |
| --- | --- |
| Carbohydrate | 1 |
| Fiber | 1.5 |
| Protein | 2 |
| Fat | 3 |
| Vitamin C | 3 |
| Vitamin A | 8 |
| Iron | 8 |
| Calcium | 8 |

Eating adds `%DV / 100 / storeDays`, scaled by the food's quality grade (0.8–1.2). Effects refresh
every 2 seconds while their condition holds.

## Afflictions

Each day, for each organism that can infect the host at its current stage:

- **Infection chance** for an uninfected crop:
  `suitability(weather, soil) × inoculum(history, tillage) × spreadRate × 0.2 × pressure`.
- **Severity** of an infected crop grows logistically at a rate proportional to `suitability − 0.2`.
- **Effects** multiply growth, yield and quality, and an organism can kill the host.
- **Inoculum** rises by 0.3 per recent crop of the same host (within the organism's survival time)
  for soil-borne and residue-borne organisms, and falls with incorporation by tillage.
