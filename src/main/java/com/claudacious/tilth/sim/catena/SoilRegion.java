package com.claudacious.tilth.sim.catena;

/** Broad soil-forming environments that vanilla and modded biomes are grouped into. */
public enum SoilRegion {
    DESERT,
    BADLANDS,
    GRASSLAND,
    SAVANNA,
    TEMPERATE_FOREST,
    BOREAL,
    TUNDRA,
    SWAMP,
    JUNGLE,
    MOUNTAIN,
    RIVER,
    BEACH,
    OCEAN,
    MUSHROOM;

    /** Wet, cool regions where hollows accumulate peat instead of mineral topsoil. */
    public boolean formsPeat() {
        return this == SWAMP || this == BOREAL || this == TUNDRA;
    }
}
