package com.claudacious.tilth.sim.postharvest;

/** Pure stored-grain state with compatibility defaults for older saves missing new fields. */
public record GrainStorageState(int grainCount, int spoiledCount, double moisture, double spoilageCarry) {
    public static GrainStorageState fromSaved(Integer grainCount, Integer spoiledCount, Float moisture, Double spoilageCarry) {
        int good = Math.max(0, grainCount == null ? 0 : grainCount);
        int spoiled = Math.max(0, spoiledCount == null ? 0 : spoiledCount);
        double m = moisture == null ? GrainDryingModel.DEFAULT_HARVEST_MOISTURE
                : clamp(moisture, GrainDryingModel.MIN_MOISTURE, GrainDryingModel.MAX_MOISTURE);
        double carry = spoilageCarry == null ? 0.0 : Math.max(0.0, spoilageCarry);
        return new GrainStorageState(good, spoiled, m, carry);
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}
