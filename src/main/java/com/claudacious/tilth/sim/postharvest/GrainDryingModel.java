package com.claudacious.tilth.sim.postharvest;

/** Pure moisture-equilibrium model for post-harvest grain drying and rewetting. */
public final class GrainDryingModel {
    public static final double MIN_MOISTURE = 0.06;
    public static final double MAX_MOISTURE = 0.35;
    public static final double DEFAULT_HARVEST_MOISTURE = 0.20;
    public static final double SAFE_STORAGE_MOISTURE = 0.14;

    private GrainDryingModel() {
    }

    public static double nextMoisture(double current, double ambientHumidity, boolean raining, boolean sheltered,
                                      double elapsedDays, double dryingRatePerDay) {
        double humidity = clamp01(ambientHumidity);
        double equilibrium = 0.08 + 0.18 * humidity;
        if (raining) {
            equilibrium = Math.max(equilibrium, 0.28);
        }
        double exchange = Math.max(0.0, dryingRatePerDay) * Math.max(0.0, elapsedDays) * (sheltered ? 0.55 : 1.0);
        double next = current + (equilibrium - current) * Math.min(1.0, exchange);
        return clamp(next, MIN_MOISTURE, MAX_MOISTURE);
    }

    private static double clamp01(double value) {
        return clamp(value, 0.0, 1.0);
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}
