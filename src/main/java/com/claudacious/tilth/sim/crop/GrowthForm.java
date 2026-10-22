package com.claudacious.tilth.sim.crop;

/**
 * Plant architecture. Determines the default mapping from the ten visual stages to BBCH codes and
 * to development fractions, and which model the client renders.
 */
public enum GrowthForm {
    CEREAL(new int[]{0, 9, 13, 21, 31, 51, 65, 75, 85, 89}),
    TALL_GRASS(new int[]{0, 9, 13, 16, 33, 55, 65, 75, 85, 89}),
    LEGUME(new int[]{0, 9, 12, 15, 25, 55, 65, 75, 85, 89}),
    FORAGE(new int[]{0, 9, 12, 15, 21, 31, 51, 60, 65, 69}),
    TUBER(new int[]{0, 9, 13, 21, 41, 51, 65, 45, 91, 97}),
    ROOT(new int[]{0, 9, 12, 15, 41, 43, 45, 47, 48, 49}),
    BULB(new int[]{0, 9, 12, 15, 41, 43, 45, 47, 48, 49}),
    HEAD_VEGETABLE(new int[]{0, 9, 12, 15, 19, 41, 43, 45, 47, 49}),
    FRUITING(new int[]{0, 9, 12, 15, 21, 51, 65, 71, 81, 89}),
    TALL_FLOWER(new int[]{0, 9, 12, 16, 31, 51, 65, 75, 85, 89}),
    FIBER(new int[]{0, 9, 12, 15, 31, 51, 65, 75, 85, 89});

    /** Development fraction (0 = emergence, 1 = maturity) at which visual stages 1..9 begin. */
    private static final double[] DEFAULT_STAGE_START = {0.0, 0.0, 0.08, 0.18, 0.30, 0.45, 0.58, 0.70, 0.84, 1.0};

    private final int[] bbch;

    GrowthForm(int[] bbch) {
        this.bbch = bbch;
    }

    public int[] bbch() {
        return bbch.clone();
    }

    public double[] stageStart() {
        return DEFAULT_STAGE_START.clone();
    }
}
