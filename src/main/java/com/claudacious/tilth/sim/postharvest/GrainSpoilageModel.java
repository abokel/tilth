package com.claudacious.tilth.sim.postharvest;

/** Pure model for moisture- and temperature-driven grain spoilage in storage. */
public final class GrainSpoilageModel {
    private GrainSpoilageModel() {
    }

    public static double dailySpoilageFraction(double moisture, double temperatureC, double safeStorageMoisture) {
        if (moisture <= safeStorageMoisture) {
            return 0.0;
        }
        double wet = clamp((moisture - safeStorageMoisture) / Math.max(0.02, 0.30 - safeStorageMoisture), 0.0, 1.3);
        double heat = clamp((temperatureC - 8.0) / 24.0, 0.0, 1.5);
        double fraction = 0.001 + 0.030 * wet * wet * (0.45 + heat);
        return clamp(fraction, 0.0, 0.08);
    }

    public static double expectedSpoiled(double amount, double dailyFraction, double elapsedDays) {
        if (amount <= 0 || dailyFraction <= 0 || elapsedDays <= 0) {
            return 0.0;
        }
        double survival = Math.exp(-dailyFraction * elapsedDays);
        return amount * (1.0 - survival);
    }

    /**
     * Aeration: fans in a built silo push air through the grain, drying it by {@code ratePerDay} each day down to
     * one point below the safe storage moisture, and no further.
     */
    public static double aerate(double moisture, double safeStorageMoisture, double ratePerDay, double elapsedDays) {
        double target = safeStorageMoisture - 0.01;
        if (moisture <= target || ratePerDay <= 0 || elapsedDays <= 0) {
            return moisture;
        }
        return Math.max(target, moisture - ratePerDay * elapsedDays);
    }

    /** Fraction of grain left after {@code elapsedDays} of losses at {@code dailyFraction}, compounded. */
    public static double survival(double dailyFraction, double elapsedDays) {
        if (dailyFraction <= 0 || elapsedDays <= 0) {
            return 1.0;
        }
        return Math.exp(-dailyFraction * elapsedDays);
    }

    public static double moistureRisk(double moisture, double safeStorageMoisture) {
        if (moisture <= safeStorageMoisture) {
            return 0.0;
        }
        return clamp((moisture - safeStorageMoisture) / Math.max(0.02, 0.30 - safeStorageMoisture), 0.0, 1.0);
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}
