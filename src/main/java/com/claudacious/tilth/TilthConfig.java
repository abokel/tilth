package com.claudacious.tilth;

import com.claudacious.tilth.sim.SimSettings;
import com.claudacious.tilth.world.TilthCalendar;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.List;

/** config/tilth-common.toml. Every rate in the simulation can be tuned here. */
public final class TilthConfig {
    public enum SourceMode {
        /** Vanilla behaviour: two adjacent sources create a third anywhere. */
        VANILLA,
        /** Water never creates new source blocks. Every bucket is finite. */
        NEVER,
        /** Only oceans, rivers and other biomes listed in infiniteWaterBiomes regenerate sources. */
        INFINITE_BIOMES_ONLY,
        /** Like INFINITE_BIOMES_ONLY, but additionally only at or below sea level. */
        INFINITE_BIOMES_BELOW_SEA_LEVEL
    }

    private static final ModConfigSpec.Builder B = new ModConfigSpec.Builder();

    // ------------------------------------------------------------------ calendar
    public static final ModConfigSpec.IntValue DAYS_PER_MONTH;
    public static final ModConfigSpec.IntValue START_MONTH;
    // ------------------------------------------------------------------ climate
    public static final ModConfigSpec.DoubleValue TEMPERATURE_OFFSET;
    public static final ModConfigSpec.DoubleValue PRECIPITATION_MULTIPLIER;
    public static final ModConfigSpec.DoubleValue ET_MULTIPLIER;
    public static final ModConfigSpec.DoubleValue LAPSE_RATE;
    public static final ModConfigSpec.DoubleValue LATITUDE_GRADIENT;
    public static final ModConfigSpec.DoubleValue LATITUDE;
    // ------------------------------------------------------------------ water
    public static final ModConfigSpec.EnumValue<SourceMode> SOURCE_MODE;
    public static final ModConfigSpec.ConfigValue<List<? extends String>> INFINITE_BIOMES;
    public static final ModConfigSpec.BooleanValue PONDS;
    public static final ModConfigSpec.BooleanValue PUDDLES;
    public static final ModConfigSpec.BooleanValue EVAPORATION;
    public static final ModConfigSpec.IntValue SURFACE_WATER_INTERVAL;
    public static final ModConfigSpec.IntValue SURFACE_WATER_SAMPLES;
    public static final ModConfigSpec.IntValue SURFACE_WATER_RADIUS;
    public static final ModConfigSpec.IntValue MAX_POND_AREA;
    public static final ModConfigSpec.IntValue MAX_EVAPORATING_BODY;
    public static final ModConfigSpec.DoubleValue POND_CHANCE;
    public static final ModConfigSpec.DoubleValue PUDDLE_CHANCE;
    public static final ModConfigSpec.DoubleValue EVAPORATION_RATE;
    public static final ModConfigSpec.BooleanValue GROUNDWATER_PONDS_PERSIST;
    // ------------------------------------------------------------------ soil
    public static final ModConfigSpec.BooleanValue CATENA_WORLDGEN;
    public static final ModConfigSpec.BooleanValue SOIL_MORPHING;
    public static final ModConfigSpec.DoubleValue MORPH_CHANCE;
    public static final ModConfigSpec.BooleanValue EROSION;
    public static final ModConfigSpec.BooleanValue CONVERT_VANILLA_ON_TILL;
    public static final ModConfigSpec.BooleanValue TILL_VANILLA_SAND;
    public static final ModConfigSpec.DoubleValue MINERALIZATION;
    public static final ModConfigSpec.DoubleValue LEACHING;
    public static final ModConfigSpec.DoubleValue PH_DRIFT;
    public static final ModConfigSpec.DoubleValue ROOT_ZONE_DEPTH;
    public static final ModConfigSpec.BooleanValue TRAMPLING_COMPACTS;
    public static final ModConfigSpec.IntValue FARMLAND_REVERT_DAYS;
    public static final ModConfigSpec.IntValue MAX_CATCH_UP_DAYS;
    // ------------------------------------------------------------------ crops
    public static final ModConfigSpec.DoubleValue GROWTH_RATE;
    public static final ModConfigSpec.DoubleValue YIELD;
    public static final ModConfigSpec.DoubleValue NUTRIENT_DEMAND;
    public static final ModConfigSpec.BooleanValue FROST_KILLS;
    public static final ModConfigSpec.BooleanValue DROUGHT_KILLS;
    public static final ModConfigSpec.BooleanValue WATERLOGGING_KILLS;
    public static final ModConfigSpec.BooleanValue BONE_MEAL_GROWS_CROPS;
    public static final ModConfigSpec.BooleanValue REDIRECT_VANILLA_SEEDS;
    public static final ModConfigSpec.BooleanValue GRASS_DROPS_SEEDS;
    public static final ModConfigSpec.IntValue STARTER_SEEDS;
    public static final ModConfigSpec.BooleanValue CROPS_NEED_LIGHT;
    // ------------------------------------------------------------------ diet
    public static final ModConfigSpec.BooleanValue DIET;
    public static final ModConfigSpec.BooleanValue DIET_EFFECTS;
    public static final ModConfigSpec.DoubleValue DIET_DECAY;
    // ------------------------------------------------------------------ plots
    public static final ModConfigSpec.DoubleValue METRES_PER_BLOCK;
    public static final ModConfigSpec.BooleanValue PLOT_PROTECTION;
    public static final ModConfigSpec.BooleanValue FARM_ONLY_IN_PLOTS;
    public static final ModConfigSpec.IntValue MAX_PLOT_VERTICES;
    public static final ModConfigSpec.IntValue MAX_PLOTS_PER_PLAYER;
    // ------------------------------------------------------------------ livestock
    public static final ModConfigSpec.IntValue MANURE_INTERVAL;
    public static final ModConfigSpec.BooleanValue LIVESTOCK;
    public static final ModConfigSpec.DoubleValue LIVESTOCK_APPETITE;
    // ------------------------------------------------------------------ afflictions
    public static final ModConfigSpec.BooleanValue AFFLICTIONS;
    public static final ModConfigSpec.DoubleValue AFFLICTION_PRESSURE;
    // ------------------------------------------------------------------ debug
    public static final ModConfigSpec.IntValue DEBUG_PERMISSION;
    public static final ModConfigSpec.IntValue HUD_INTERVAL;
    // ------------------------------------------------------------------ integrations
    public static final ModConfigSpec.BooleanValue GIVE_GUIDE;
    public static final ModConfigSpec.BooleanValue SEASONAL_COLORS;
    public static final ModConfigSpec.BooleanValue WINTER_SNOW;

    static {
        B.comment("Calendar. Agronomy runs on a real 365-day calendar; a Minecraft day covers 365/(12*daysPerMonth) real days.").push("calendar");
        SEASONAL_COLORS = B.comment("Leaves and grass change colour with the season (autumn colours, brown winter leaves). Client side.")
                .define("seasonalColors", true);
        WINTER_SNOW = B.comment("Snow settles and ponds freeze wherever the Tilth climate is below freezing, then thaw in spring.")
                .define("winterSnowAndIce", true);
        DAYS_PER_MONTH = B.comment("Minecraft days per calendar month (6 -> a 72-day year, each game day ~5 real days).")
                .defineInRange("daysPerMonth", 6, 1, 90);
        START_MONTH = B.comment("Month a new world starts in (1 = January). March gives time to prepare for spring sowing.")
                .defineInRange("startMonth", 3, 1, 12);
        B.pop();

        B.comment("Climate model.").push("climate");
        TEMPERATURE_OFFSET = B.comment("Degrees C added to every temperature (negative = colder world).")
                .defineInRange("temperatureOffset", 0.0, -30.0, 30.0);
        PRECIPITATION_MULTIPLIER = B.comment("Multiplier on all modelled rainfall and snowfall.")
                .defineInRange("precipitationMultiplier", 1.0, 0.0, 10.0);
        ET_MULTIPLIER = B.comment("Multiplier on reference evapotranspiration (how fast fields dry out).")
                .defineInRange("evapotranspirationMultiplier", 1.0, 0.0, 10.0);
        LAPSE_RATE = B.comment("Cooling in degrees C per block above sea level (real atmosphere: 0.0065/m; Minecraft hills are short so this is exaggerated).")
                .defineInRange("lapseRatePerBlock", 0.02, 0.0, 1.0);
        LATITUDE_GRADIENT = B.comment("TerraFirmaCraft-style latitude: degrees C colder per 1000 blocks of distance from z=0. 0 disables.")
                .defineInRange("latitudeGradientPer1000Blocks", 0.0, 0.0, 100.0);
        LATITUDE = B.comment("Latitude used for day length and solar radiation in the evapotranspiration formula.")
                .defineInRange("latitudeDegrees", 40.0, 0.0, 66.0);
        B.pop();

        B.comment("Water. Optional finite water, ponds forming in undrainable hollows, evaporation.").push("water");
        SOURCE_MODE = B.comment("When two water sources may create a third. VANILLA (infinite water, the default), NEVER, INFINITE_BIOMES_ONLY or "
                        + "INFINITE_BIOMES_BELOW_SEA_LEVEL. The last three make water finite outside the listed biomes.")
                .defineEnum("sourceCreation", SourceMode.VANILLA);
        INFINITE_BIOMES = B.comment("Biomes (ids) or biome tags (#ids) treated as effectively infinite water bodies.")
                .defineListAllowEmpty("infiniteWaterBiomes", List.of("#minecraft:is_ocean", "#minecraft:is_river"), () -> "#minecraft:is_ocean",
                        o -> o instanceof String s && !s.isBlank());
        PONDS = B.comment("Rain fills closed depressions (hollows with no outlet) on slowly draining ground, forming ponds.")
                .define("ponds", true);
        PUDDLES = B.comment("Heavy rain leaves shallow puddles on poorly drained or compacted ground.")
                .define("puddles", true);
        EVAPORATION = B.comment("Small, isolated water bodies outside infinite biomes slowly evaporate in dry weather. Pairs with finite water.")
                .define("evaporation", false);
        SURFACE_WATER_INTERVAL = B.comment("Ticks between surface-water passes.")
                .defineInRange("surfaceWaterInterval", 20, 1, 12000);
        SURFACE_WATER_SAMPLES = B.comment("Random columns examined around each player per pass.")
                .defineInRange("samplesPerPlayer", 3, 0, 256);
        SURFACE_WATER_RADIUS = B.comment("Radius (blocks) around players in which ponds form and water evaporates.")
                .defineInRange("radius", 64, 8, 256);
        MAX_POND_AREA = B.comment("Largest depression (in blocks of surface) that rain may fill. Bigger flat areas just get puddles.")
                .defineInRange("maxPondArea", 96, 1, 4096);
        MAX_EVAPORATING_BODY = B.comment("Water bodies with more surface blocks than this never evaporate (lakes are fed by groundwater).")
                .defineInRange("maxEvaporatingBodySize", 48, 1, 4096);
        POND_CHANCE = B.comment("Chance per rainy sample on impermeable ground that a depression gains one block of water.")
                .defineInRange("pondChance", 0.6, 0.0, 1.0);
        PUDDLE_CHANCE = B.comment("Chance per rainy sample that a puddle forms on poorly drained flat ground.")
                .defineInRange("puddleChance", 0.25, 0.0, 1.0);
        EVAPORATION_RATE = B.comment("Multiplier on evaporation chance (scaled by the day's evapotranspiration).")
                .defineInRange("evaporationRate", 1.0, 0.0, 100.0);
        GROUNDWATER_PONDS_PERSIST = B.comment("Ponds whose surface is below the local water table do not evaporate.")
                .define("groundwaterPondsPersist", true);
        B.pop();

        B.comment("Soil.").push("soil");
        CATENA_WORLDGEN = B.comment("Replace vanilla dirt with catena soils (texture, depth and drainage from hillslope position) in newly generated chunks.")
                .define("catenaWorldgen", true);
        SOIL_MORPHING = B.comment("Soil blocks slowly change with their surroundings: gleying when waterlogged, peat in cold wet hollows, subsoil becoming topsoil when exposed.")
                .define("morphing", true);
        MORPH_CHANCE = B.comment("Chance that a random tick on a soil block evaluates morphing.")
                .defineInRange("morphChance", 0.02, 0.0, 1.0);
        EROSION = B.comment("Bare soil on slopes loses fine particles to rain, which are deposited downslope.")
                .define("erosion", true);
        CONVERT_VANILLA_ON_TILL = B.comment("Tilling vanilla dirt or grass produces Tilth farmland with a soil texture inferred from the terrain.")
                .define("convertVanillaOnTill", true);
        TILL_VANILLA_SAND = B.comment("Vanilla sand can be tilled into sand farmland (desert agriculture with irrigation).")
                .define("tillVanillaSand", true);
        MINERALIZATION = B.comment("Multiplier on organic matter mineralisation (nitrogen release from humus).")
                .defineInRange("mineralizationMultiplier", 1.0, 0.0, 10.0);
        LEACHING = B.comment("Multiplier on nutrient leaching by drainage water.")
                .defineInRange("leachingMultiplier", 1.0, 0.0, 10.0);
        PH_DRIFT = B.comment("Multiplier on acidification and pH drift toward the climate equilibrium.")
                .defineInRange("phDriftMultiplier", 1.0, 0.0, 10.0);
        ROOT_ZONE_DEPTH = B.comment("Depth (m) of the soil water bucket simulated under each block.")
                .defineInRange("rootZoneDepth", 0.6, 0.1, 3.0);
        TRAMPLING_COMPACTS = B.comment("Landing on farmland compacts it instead of turning it back into dirt.")
                .define("tramplingCompacts", true);
        FARMLAND_REVERT_DAYS = B.comment("Real days farmland may lie bare before weeds reclaim it as untilled soil. 0 = never.")
                .defineInRange("farmlandRevertDays", 240, 0, 100000);
        MAX_CATCH_UP_DAYS = B.comment("Most real days a field simulates when its chunk is loaded again after a long absence.")
                .defineInRange("maxCatchUpDays", 730, 1, 100000);
        B.pop();

        B.comment("Crops.").push("crops");
        GROWTH_RATE = B.comment("Multiplier on thermal-time accumulation (2.0 = crops develop twice as fast).")
                .defineInRange("growthRateMultiplier", 1.0, 0.01, 100.0);
        YIELD = B.comment("Multiplier on harvested yield.")
                .defineInRange("yieldMultiplier", 1.0, 0.0, 100.0);
        NUTRIENT_DEMAND = B.comment("Multiplier on crop nutrient uptake (lower = soils deplete more slowly).")
                .defineInRange("nutrientDemandMultiplier", 1.0, 0.0, 10.0);
        FROST_KILLS = B.define("frostKillsCrops", true);
        DROUGHT_KILLS = B.define("droughtKillsCrops", true);
        WATERLOGGING_KILLS = B.define("waterloggingKillsCrops", true);
        BONE_MEAL_GROWS_CROPS = B.comment("If false, bone meal on Tilth crops or farmland acts as a phosphorus and calcium fertiliser instead of instant growth.")
                .define("boneMealGrowsCrops", false);
        REDIRECT_VANILLA_SEEDS = B.comment("Vanilla wheat seeds, potatoes, carrots and beetroot seeds plant Tilth crops on Tilth farmland.")
                .define("redirectVanillaSeeds", true);
        GRASS_DROPS_SEEDS = B.comment("Breaking grass can drop seeds of crops suited to the local climate.")
                .define("grassDropsSeeds", true);
        STARTER_SEEDS = B.comment("Radish seeds (the fast, low-yield starter crop) given to each player on first join. 0 disables.")
                .defineInRange("starterRadishSeeds", 8, 0, 64);
        CROPS_NEED_LIGHT = B.comment("Crops grow slowly without sunlight (strong artificial light gives partial growth).")
                .define("cropsNeedLight", true);
        B.pop();

        B.comment("Player diet and nutrition.").push("diet");
        DIET = B.define("enabled", true);
        DIET_EFFECTS = B.comment("Deficiencies and a balanced diet apply status effects.")
                .define("effects", true);
        DIET_DECAY = B.comment("Multiplier on how fast nutrient stores drain.")
                .defineInRange("decayMultiplier", 1.0, 0.0, 100.0);
        B.pop();

        B.comment("Field plots surveyed with the surveyor's chain.").push("plots");
        METRES_PER_BLOCK = B.comment("Edge length of a block in metres when reporting areas (acres, hectares).")
                .defineInRange("metresPerBlock", 1.0, 0.01, 1000.0);
        PLOT_PROTECTION = B.comment("Only a plot's owner and members may break, place or harvest inside it.")
                .define("protection", false);
        FARM_ONLY_IN_PLOTS = B.comment("Ground can only be tilled inside a surveyed plot, by its owner or members. Creative players are exempt.")
                .define("farmOnlyInPlots", false);
        MAX_PLOT_VERTICES = B.defineInRange("maxVertices", 64, 3, 1024);
        MAX_PLOTS_PER_PLAYER = B.defineInRange("maxPlotsPerPlayer", 64, 1, 10000);
        B.pop();

        B.comment("Livestock.").push("livestock");
        LIVESTOCK = B.comment("Farm animals have a body condition fed by grazing, hay bales and hand feeding. It decides breeding, milk, "
                        + "eggs, wool regrowth, manure and meat yield and quality.")
                .define("husbandry", true);
        LIVESTOCK_APPETITE = B.comment("Multiplier on how fast animals lose condition (1 = from top condition to starving in about two game days).")
                .defineInRange("appetiteMultiplier", 1.0, 0.0, 10.0);
        MANURE_INTERVAL = B.comment("Average ticks between manure drops from each adult farm animal. 0 disables.")
                .defineInRange("manureInterval", 9600, 0, 1000000);
        B.pop();

        B.comment("Diseases, pests and fungi. The models are defined (data/tilth/tilth/affliction) but inactive by default.").push("afflictions");
        AFFLICTIONS = B.define("enabled", false);
        AFFLICTION_PRESSURE = B.defineInRange("pressureMultiplier", 1.0, 0.0, 100.0);
        B.pop();

        B.comment("Debugging.").push("debug");
        DEBUG_PERMISSION = B.comment("Permission level for commands that change the world (/tilth time, soil set, crop grow ...).")
                .defineInRange("commandPermissionLevel", 2, 0, 4);
        HUD_INTERVAL = B.comment("Ticks between debug HUD refreshes.")
                .defineInRange("hudInterval", 10, 1, 200);
        B.pop();

        B.comment("Integrations with optional mods.").push("integrations");
        GIVE_GUIDE = B.comment("Give each player the Tilth Field Guide (a Patchouli book) the first time they join. Needs Patchouli.")
                .define("giveFieldGuideOnFirstJoin", true);
        B.pop();
    }

    public static final ModConfigSpec SPEC = B.build();

    private TilthConfig() {
    }

    public static void onLoad(ModConfigEvent event) {
        if (event.getConfig().getSpec() == SPEC) {
            apply();
        }
    }

    /** Copies config values into the pure simulation settings and the calendar. */
    public static void apply() {
        TilthCalendar.reconfigure(DAYS_PER_MONTH.getAsInt(), START_MONTH.getAsInt());
        SimSettings.growthRateMultiplier = GROWTH_RATE.getAsDouble();
        SimSettings.yieldMultiplier = YIELD.getAsDouble();
        SimSettings.frostKillsCrops = FROST_KILLS.getAsBoolean();
        SimSettings.droughtKillsCrops = DROUGHT_KILLS.getAsBoolean();
        SimSettings.waterloggingKillsCrops = WATERLOGGING_KILLS.getAsBoolean();
        SimSettings.mineralizationMultiplier = MINERALIZATION.getAsDouble();
        SimSettings.leachingMultiplier = LEACHING.getAsDouble();
        SimSettings.nutrientDemandMultiplier = NUTRIENT_DEMAND.getAsDouble();
        SimSettings.phDriftMultiplier = PH_DRIFT.getAsDouble();
        SimSettings.rootZoneDepthMeters = ROOT_ZONE_DEPTH.getAsDouble();
        SimSettings.temperatureOffset = TEMPERATURE_OFFSET.getAsDouble();
        SimSettings.precipitationMultiplier = PRECIPITATION_MULTIPLIER.getAsDouble();
        SimSettings.evapotranspirationMultiplier = ET_MULTIPLIER.getAsDouble();
        SimSettings.latitudeDegrees = LATITUDE.getAsDouble();
        SimSettings.afflictionsEnabled = AFFLICTIONS.getAsBoolean();
        SimSettings.afflictionPressureMultiplier = AFFLICTION_PRESSURE.getAsDouble();
    }
}
