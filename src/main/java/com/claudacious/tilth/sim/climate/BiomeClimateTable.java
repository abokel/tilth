package com.claudacious.tilth.sim.climate;

import com.claudacious.tilth.sim.Noise;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Real-world climate analogues for vanilla biomes. Plains behave like the US Corn Belt, deserts like
 * the Sonoran, jungles like the Amazon basin, taiga like interior Canada, and so on. Modded biomes
 * fall back to an estimate from their Minecraft temperature and downfall values.
 */
public final class BiomeClimateTable {
    private static final Map<String, BiomeClimate> TABLE = new HashMap<>();

    static {
        //                                   MAT   amp  DTR  precip seas  humid
        put("plains",                      10.0, 13.0, 11, 850, 0.35, 0.60);
        put("sunflower_plains",            11.0, 13.0, 12, 780, 0.35, 0.55);
        put("snowy_plains",                -6.0, 17.0,  9, 420, 0.30, 0.70);
        put("ice_spikes",                 -12.0, 18.0,  8, 300, 0.20, 0.70);
        put("desert",                      23.0, 10.0, 18, 120, 0.00, 0.20);
        put("swamp",                       16.0,  9.0,  8, 1300, 0.20, 0.90);
        put("mangrove_swamp",              26.0,  3.0,  7, 1900, 0.40, 0.92);
        put("forest",                       9.0, 11.0, 10, 900, 0.20, 0.70);
        put("flower_forest",               11.0, 11.0, 10, 950, 0.20, 0.70);
        put("birch_forest",                 7.0, 12.0, 10, 800, 0.20, 0.68);
        put("old_growth_birch_forest",      7.0, 12.0, 10, 850, 0.20, 0.70);
        put("dark_forest",                  9.0, 10.0,  8, 1150, 0.10, 0.80);
        put("old_growth_pine_taiga",        4.0, 13.0,  9, 900, 0.10, 0.75);
        put("old_growth_spruce_taiga",      3.0, 13.0,  9, 950, 0.10, 0.78);
        put("taiga",                        1.0, 16.0, 11, 550, 0.40, 0.70);
        put("snowy_taiga",                 -5.0, 18.0, 10, 450, 0.40, 0.72);
        put("savanna",                     24.0,  4.0, 13, 750, 0.80, 0.45);
        put("savanna_plateau",             22.0,  4.0, 14, 700, 0.80, 0.42);
        put("windswept_hills",              5.0, 10.0,  9, 1000, 0.00, 0.72);
        put("windswept_gravelly_hills",     4.0, 10.0,  9, 950, 0.00, 0.70);
        put("windswept_forest",             5.0, 10.0,  9, 1050, 0.00, 0.74);
        put("windswept_savanna",           22.0,  5.0, 15, 600, 0.70, 0.40);
        put("jungle",                      26.0,  2.0,  8, 2400, 0.20, 0.92);
        put("sparse_jungle",               25.0,  3.0,  9, 1600, 0.40, 0.85);
        put("bamboo_jungle",               26.0,  2.0,  8, 2600, 0.30, 0.93);
        put("badlands",                    18.0, 12.0, 18, 300, 0.40, 0.25);
        put("eroded_badlands",             18.0, 12.0, 18, 280, 0.40, 0.25);
        put("wooded_badlands",             16.0, 12.0, 16, 450, 0.40, 0.30);
        put("meadow",                       5.0, 10.0, 11, 1000, 0.30, 0.70);
        put("cherry_grove",                12.0, 11.0, 10, 1300, 0.50, 0.75);
        put("grove",                        0.0, 12.0,  9, 800, 0.10, 0.70);
        put("snowy_slopes",                -6.0, 12.0,  9, 700, 0.10, 0.70);
        put("frozen_peaks",               -12.0, 10.0,  8, 600, 0.00, 0.65);
        put("jagged_peaks",               -10.0, 10.0,  8, 650, 0.00, 0.65);
        put("stony_peaks",                  2.0, 11.0, 12, 600, 0.00, 0.50);
        put("river",                       10.0, 12.0, 10, 800, 0.30, 0.75);
        put("frozen_river",                -5.0, 16.0,  9, 420, 0.30, 0.75);
        put("beach",                       15.0,  7.0,  7, 800, 0.00, 0.80);
        put("snowy_beach",                 -3.0, 12.0,  7, 500, 0.00, 0.80);
        put("stony_shore",                  8.0,  8.0,  7, 1000, 0.00, 0.80);
        put("warm_ocean",                  26.0,  3.0,  5, 1500, 0.20, 0.90);
        put("lukewarm_ocean",              20.0,  5.0,  5, 1100, 0.10, 0.85);
        put("deep_lukewarm_ocean",         20.0,  5.0,  5, 1100, 0.10, 0.85);
        put("ocean",                       12.0,  6.0,  5, 1000, 0.00, 0.85);
        put("deep_ocean",                  12.0,  6.0,  5, 1000, 0.00, 0.85);
        put("cold_ocean",                   6.0,  8.0,  5, 900, 0.00, 0.85);
        put("deep_cold_ocean",              6.0,  8.0,  5, 900, 0.00, 0.85);
        put("frozen_ocean",                -2.0, 10.0,  5, 500, 0.00, 0.85);
        put("deep_frozen_ocean",           -2.0, 10.0,  5, 500, 0.00, 0.85);
        put("mushroom_fields",             12.0,  5.0,  6, 1100, 0.00, 0.85);
        // Caves have almost no annual or daily temperature swing and no rain: underground farms must be irrigated.
        put("dripstone_caves",             11.0,  0.5, 0.5, 0, 0.00, 0.90);
        put("lush_caves",                  14.0,  0.5, 0.5, 0, 0.00, 0.95);
        put("deep_dark",                   10.0,  0.5, 0.5, 0, 0.00, 0.90);
        // Other dimensions: nothing temperate grows here.
        put("nether_wastes",               45.0,  0.0, 2, 0, 0.00, 0.05);
        put("soul_sand_valley",            40.0,  0.0, 2, 0, 0.00, 0.05);
        put("crimson_forest",              40.0,  0.0, 2, 0, 0.00, 0.10);
        put("warped_forest",               38.0,  0.0, 2, 0, 0.00, 0.10);
        put("basalt_deltas",               50.0,  0.0, 2, 0, 0.00, 0.05);
        put("the_end",                    -15.0,  0.0, 1, 0, 0.00, 0.10);
        put("end_highlands",              -15.0,  0.0, 1, 0, 0.00, 0.10);
        put("end_midlands",               -15.0,  0.0, 1, 0, 0.00, 0.10);
        put("small_end_islands",          -15.0,  0.0, 1, 0, 0.00, 0.10);
        put("end_barrens",                -15.0,  0.0, 1, 0, 0.00, 0.10);
    }

    private BiomeClimateTable() {
    }

    private static void put(String id, double mat, double amp, double dtr, double precip, double seasonality, double humidity) {
        TABLE.put("minecraft:" + id, new BiomeClimate(mat, amp, dtr, precip, seasonality, humidity));
    }

    public static Optional<BiomeClimate> lookup(String biomeId) {
        return Optional.ofNullable(TABLE.get(biomeId));
    }

    /** Estimate for biomes without a table entry (usually modded biomes). */
    public static BiomeClimate fallback(float baseTemperature, float downfall, boolean hasPrecipitation) {
        double t = baseTemperature;
        double d = Noise.clamp(downfall, 0, 1);
        double mat;
        if (t < 0) {
            mat = -8 + 10 * t;
        } else if (t < 0.3) {
            mat = -8 + 30 * t;
        } else if (t < 1.0) {
            mat = 1 + 14 * (t - 0.3) / 0.7;
        } else {
            mat = 15 + 8 * Math.min(1.0, t - 1.0);
        }
        if (t >= 0.9 && d >= 0.8) {
            mat = Math.max(mat, 25);
        }
        double precip = hasPrecipitation ? 150 + 1500 * d : 120;
        double amp = Math.max(2, 14 - 9 * d) * (mat > 22 ? 0.4 : 1.0);
        double dtr = 7 + 10 * (1 - d);
        double humidity = Noise.clamp(0.25 + 0.7 * d, 0.1, 0.95);
        return new BiomeClimate(mat, amp, dtr, precip, 0.2, humidity);
    }
}
