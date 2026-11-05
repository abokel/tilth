package com.claudacious.tilth.sim.livestock;

/** Simple bounded pasture dynamics for rotational grazing. */
public final class PastureModel {
    private PastureModel() {
    }

    public static double graze(double quality, double dailyPressure, double days) {
        return clamp01(quality - Math.max(0.0, dailyPressure) * Math.max(0.0, days));
    }

    public static double recover(double quality, double dailyRecovery, double days) {
        return clamp01(quality + Math.max(0.0, dailyRecovery) * Math.max(0.0, days));
    }

    public static double forageFactor(double quality) {
        return 0.25 + 0.75 * clamp01(quality);
    }

    public static boolean overgrazed(double quality, double threshold) {
        return clamp01(quality) < clamp01(threshold);
    }

    private static double clamp01(double v) {
        return Math.max(0.0, Math.min(1.0, v));
    }
}
