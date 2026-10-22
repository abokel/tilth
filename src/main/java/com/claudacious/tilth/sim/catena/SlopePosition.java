package com.claudacious.tilth.sim.catena;

/**
 * Hillslope elements of a soil catena (Ruhe 1960; Conacher &amp; Dalrymple 1977). Upper slopes shed
 * water and fine particles and have thin, coarse, well-drained soils; lower slopes and hollows
 * receive them and have deep, fine, humus-rich and often waterlogged soils.
 */
public enum SlopePosition {
    //                         sand  clay  OM factor  topsoil  subsoil
    SUMMIT("Summit",              6,   -3,   0.85,       1,       2),
    SHOULDER("Shoulder",         10,   -4,   0.70,       1,       1),
    BACKSLOPE("Backslope",        8,   -2,   0.75,       1,       1),
    FOOTSLOPE("Footslope",       -4,    4,   1.20,       2,       3),
    TOESLOPE("Toeslope",         -8,    9,   1.35,       3,       3),
    DEPRESSION("Depression",    -10,   13,   1.60,       3,       3),
    FLOODPLAIN("Floodplain",     -6,    4,   1.20,       3,       3),
    LEVEL("Level upland",         0,    0,   1.00,       2,       2);

    private final String displayName;
    private final int sandDelta;
    private final int clayDelta;
    private final double organicFactor;
    private final int topsoilDepth;
    private final int subsoilDepth;

    SlopePosition(String displayName, int sandDelta, int clayDelta, double organicFactor, int topsoilDepth, int subsoilDepth) {
        this.displayName = displayName;
        this.sandDelta = sandDelta;
        this.clayDelta = clayDelta;
        this.organicFactor = organicFactor;
        this.topsoilDepth = topsoilDepth;
        this.subsoilDepth = subsoilDepth;
    }

    public String displayName() {
        return displayName;
    }

    public int sandDelta() {
        return sandDelta;
    }

    public int clayDelta() {
        return clayDelta;
    }

    public double organicFactor() {
        return organicFactor;
    }

    public int topsoilDepth() {
        return topsoilDepth;
    }

    public int subsoilDepth() {
        return subsoilDepth;
    }

    /**
     * Base drainage score: 0 = excessively drained ... 5 = very poorly drained. Upper slopes are well
     * drained; only coarse (sandy) soils there become excessively drained, via the texture adjustment
     * in {@link SoilProfile}.
     */
    public int drainageScore() {
        return switch (this) {
            case SUMMIT, SHOULDER, BACKSLOPE -> 1;
            case LEVEL, FOOTSLOPE -> 2;
            case TOESLOPE, FLOODPLAIN -> 3;
            case DEPRESSION -> 4;
        };
    }

    public boolean receivesSediment() {
        return this == FOOTSLOPE || this == TOESLOPE || this == DEPRESSION || this == FLOODPLAIN;
    }
}
