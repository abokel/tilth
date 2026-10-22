package com.claudacious.tilth.sim.soil;

/** USDA natural drainage classes, with a typical seasonal-high water table depth. */
public enum DrainageClass {
    EXCESSIVE("Excessively drained", 8.0),
    WELL("Well drained", 3.0),
    MODERATELY_WELL("Moderately well drained", 1.2),
    SOMEWHAT_POOR("Somewhat poorly drained", 0.6),
    POOR("Poorly drained", 0.25),
    VERY_POOR("Very poorly drained", 0.0);

    private final String displayName;
    private final double waterTableDepth;

    DrainageClass(String displayName, double waterTableDepth) {
        this.displayName = displayName;
        this.waterTableDepth = waterTableDepth;
    }

    public String displayName() {
        return displayName;
    }

    /** Typical seasonal-high water table depth (m). */
    public double waterTableDepth() {
        return waterTableDepth;
    }

    public boolean isWet() {
        return ordinal() >= POOR.ordinal();
    }

    public static DrainageClass ofIndex(int index) {
        DrainageClass[] v = values();
        return v[Math.max(0, Math.min(v.length - 1, index))];
    }
}
