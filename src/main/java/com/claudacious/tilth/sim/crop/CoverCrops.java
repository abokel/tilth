package com.claudacious.tilth.sim.crop;

import java.util.Set;

/**
 * Cover crops and green manure. Rye, oats, clover and alfalfa grown as cover hold the soil over winter and smother
 * weeds. Any crop cut down green with a hoe is green manure: its leaves and stems go back into the soil as fresh,
 * nitrogen-rich residue (a legume's most of all), instead of being harvested.
 */
public final class CoverCrops {
    private static final Set<String> COVER = Set.of("rye", "oats", "clover", "alfalfa");

    private CoverCrops() {
    }

    public static boolean isCover(CropSpecies species) {
        return COVER.contains(species.id);
    }

    /** Residue a green crop returns when worked in (kg/m2), from its growth so far. */
    public static double greenManureResidue(double biomass) {
        return Math.max(0.05, Math.min(1.0, biomass * 0.5));
    }

    /** Nitrogen content of that residue (%): green leaf is rich, a legume's richer, straw (0.6%) far poorer. */
    public static double greenManureNitrogen(boolean legume) {
        return legume ? 3.2 : 1.6;
    }
}
