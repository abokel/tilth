package com.claudacious.tilth.sim.catena;

/** Geological material the soil weathered from; sets the starting texture before catena effects. */
public enum ParentMaterial {
    AEOLIAN_SAND("Aeolian sand (dunes)", 88, 4),
    GLACIAL_OUTWASH("Glacial outwash", 75, 6),
    LOESS("Loess (wind-blown silt)", 12, 18),
    GLACIAL_TILL("Glacial till", 40, 20),
    ALLUVIUM("Alluvium", 25, 22),
    LACUSTRINE("Lacustrine/marine clay", 10, 45),
    RESIDUAL_CLAY("Weathered residual clay", 25, 50),
    COLLUVIUM("Colluvium", 50, 18),
    CALCAREOUS("Calcareous sediment", 35, 25);

    private final String displayName;
    private final int sand;
    private final int clay;

    ParentMaterial(String displayName, int sand, int clay) {
        this.displayName = displayName;
        this.sand = sand;
        this.clay = clay;
    }

    public String displayName() {
        return displayName;
    }

    public int sand() {
        return sand;
    }

    public int clay() {
        return clay;
    }

    /**
     * Picks the parent material for a region from a smooth noise value in [0, 1), so materials form
     * large contiguous patches (a loess plain, a till plain, an outwash fan).
     */
    public static ParentMaterial pick(SoilRegion region, double u) {
        return switch (region) {
            case DESERT -> u < 0.6 ? AEOLIAN_SAND : (u < 0.85 ? CALCAREOUS : ALLUVIUM);
            case BADLANDS -> u < 0.6 ? RESIDUAL_CLAY : COLLUVIUM;
            case GRASSLAND -> u < 0.55 ? LOESS : (u < 0.85 ? GLACIAL_TILL : ALLUVIUM);
            case SAVANNA -> u < 0.35 ? RESIDUAL_CLAY : (u < 0.7 ? COLLUVIUM : AEOLIAN_SAND);
            case TEMPERATE_FOREST -> u < 0.5 ? GLACIAL_TILL : (u < 0.75 ? LOESS : GLACIAL_OUTWASH);
            case BOREAL -> u < 0.4 ? GLACIAL_OUTWASH : (u < 0.8 ? GLACIAL_TILL : LACUSTRINE);
            case TUNDRA -> u < 0.6 ? GLACIAL_TILL : GLACIAL_OUTWASH;
            case SWAMP -> u < 0.6 ? LACUSTRINE : ALLUVIUM;
            case JUNGLE -> u < 0.7 ? RESIDUAL_CLAY : ALLUVIUM;
            case MOUNTAIN -> u < 0.7 ? COLLUVIUM : GLACIAL_TILL;
            case RIVER -> ALLUVIUM;
            case BEACH -> AEOLIAN_SAND;
            case OCEAN -> u < 0.5 ? LACUSTRINE : ALLUVIUM;
            case MUSHROOM -> u < 0.5 ? ALLUVIUM : LOESS;
        };
    }
}
