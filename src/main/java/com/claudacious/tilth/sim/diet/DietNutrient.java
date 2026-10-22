package com.claudacious.tilth.sim.diet;

/**
 * Nutrients tracked in the player's diet. Each has a body store that drains over a number of game
 * days; food refills it by its percent daily value. Water-soluble vitamins and energy run out
 * quickly, while fat-soluble vitamins and minerals are stored for much longer, as in reality.
 */
public enum DietNutrient {
    CARBOHYDRATE("carbohydrate", "Carbohydrates", 1.0, 0xE3B448),
    PROTEIN("protein", "Protein", 2.0, 0xC2563B),
    FAT("fat", "Fat", 3.0, 0xF1D27A),
    FIBER("fiber", "Fiber", 1.5, 0x7A9A3B),
    VITAMIN_A("vitamin_a", "Vitamin A", 8.0, 0xF08A24),
    VITAMIN_C("vitamin_c", "Vitamin C", 3.0, 0xF5C518),
    IRON("iron", "Iron", 8.0, 0x8B3A3A),
    CALCIUM("calcium", "Calcium", 8.0, 0xE8E4D8);

    private final String id;
    private final String displayName;
    private final double storeDays;
    private final int color;

    DietNutrient(String id, String displayName, double storeDays, int color) {
        this.id = id;
        this.displayName = displayName;
        this.storeDays = storeDays;
        this.color = color;
    }

    public String id() {
        return id;
    }

    public String displayName() {
        return displayName;
    }

    /** Game days for a full store to drain with no intake. */
    public double storeDays() {
        return storeDays;
    }

    public int color() {
        return color;
    }

    /** Store fraction gained from eating {@code percentDailyValue}% of the daily requirement. */
    public double gainFor(double percentDailyValue) {
        return percentDailyValue / 100.0 / storeDays;
    }

    /** Store fraction lost per game tick. */
    public double decayPerTick() {
        return 1.0 / (storeDays * 24000.0);
    }

    public static DietNutrient byId(String id) {
        for (DietNutrient n : values()) {
            if (n.id.equals(id)) {
                return n;
            }
        }
        return null;
    }
}
