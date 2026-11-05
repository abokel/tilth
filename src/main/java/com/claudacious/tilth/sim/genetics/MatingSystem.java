package com.claudacious.tilth.sim.genetics;

import java.util.Set;

/**
 * How a crop sets seed, which decides how its saved seed inherits.
 * <ul>
 *   <li>{@link #SELF}: self-pollinating (wheat, barley, oats, rice, peas, beans, lentils, tomatoes...). Saved seed is
 *       selfed, so each generation is more uniform and a line soon breeds true. Inbreeding does no harm.</li>
 *   <li>{@link #OUTCROSS}: wind- or insect-pollinated (maize, rye, sunflower, brassicas, onions, beets, carrots,
 *       clovers). Pollen comes from neighbouring plants of the same crop. Crossing distinct lines gives hybrid vigour;
 *       repeated inbreeding saps it.</li>
 *   <li>{@link #CLONAL}: grown from tubers or cloves (potatoes, garlic). Offspring are copies of the parent.</li>
 * </ul>
 */
public enum MatingSystem {
    SELF, OUTCROSS, CLONAL;

    private static final Set<String> OUTCROSSERS = Set.of("maize", "rye", "sunflower", "cabbage", "onion", "sugar_beet", "beetroot",
            "carrot", "clover", "alfalfa", "radish", "sorghum");
    private static final Set<String> CLONES = Set.of("potato", "garlic");
    private static final Set<String> SEMI_DWARF_CEREALS = Set.of("common_wheat", "durum_wheat", "barley", "oats", "rice");

    public static MatingSystem of(String speciesId) {
        return CLONES.contains(speciesId) ? CLONAL : OUTCROSSERS.contains(speciesId) ? OUTCROSS : SELF;
    }

    /** Whether the semi-dwarfing gene raises grain yield in this crop (the green-revolution small grains). */
    public static boolean dwarfingHelps(String speciesId) {
        return SEMI_DWARF_CEREALS.contains(speciesId);
    }
}
