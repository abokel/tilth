package com.claudacious.tilth.sim.livestock;

/** Bounded disease risk/progression helpers for livestock. */
public final class LivestockDiseaseModel {
    private static final double MAX_ENVIRONMENTAL_INTRODUCTION_PRESSURE = 0.0006;

    private LivestockDiseaseModel() {
    }

    public static double hostStress(double condition, double pastureQuality) {
        return clamp01((1.0 - clamp01(condition)) * 0.65 + (1.0 - clamp01(pastureQuality)) * 0.35);
    }

    public static double susceptibility(double condition, double pastureQuality, boolean quarantined) {
        double hostStress = hostStress(condition, pastureQuality);
        double quarantineFactor = quarantined ? 0.7 : 1.0;
        return clamp01((0.2 + hostStress) * quarantineFactor);
    }

    public static double transmissionPressure(double neighbourSeverity, boolean airborneOrMigratory, boolean closeContact, boolean quarantined) {
        double p = clamp01(neighbourSeverity);
        p *= airborneOrMigratory ? 1.0 : 0.75;
        p *= closeContact ? 1.0 : 0.5;
        if (quarantined) {
            p *= 0.35;
        }
        return clamp01(p);
    }

    public static double infectionChance(double spreadRate, double suitability, double susceptibility, double pressure, double pressureMultiplier) {
        double p = clamp01(spreadRate) * clamp01(suitability) * clamp01(susceptibility) * clamp01(pressure) * Math.max(0.0, pressureMultiplier) * 0.2;
        return clamp01(p);
    }

    public static double environmentalIntroductionPressure(double suitability, double hostStress, boolean supportsEnvironmentalIntroduction,
                                                           boolean quarantined) {
        if (!supportsEnvironmentalIntroduction) {
            return 0.0;
        }
        double pressure = 0.00005 + MAX_ENVIRONMENTAL_INTRODUCTION_PRESSURE
                * clamp01(suitability)
                * (0.2 + 0.8 * clamp01(hostStress));
        if (quarantined) {
            pressure *= 0.35;
        }
        return Math.min(MAX_ENVIRONMENTAL_INTRODUCTION_PRESSURE, Math.max(0.0, pressure));
    }

    public static double combinedPressure(double herdPressure, double environmentalPressure) {
        return clamp01(clamp01(herdPressure) + Math.max(0.0, environmentalPressure));
    }

    public static boolean isRecentDiagnosis(Long diagnosedAt, long now, long diagnosisWindow) {
        if (diagnosedAt == null || diagnosedAt < 0 || diagnosisWindow < 0 || now < diagnosedAt) {
            return false;
        }
        long cutoff = Math.max(0L, now - diagnosisWindow);
        return diagnosedAt >= cutoff;
    }

    public static double nextSeverity(double current, double spreadRate, double suitability, double pressure, double treatment, double recoveryBonus) {
        double s = clamp01(current);
        double growth = clamp01(spreadRate) * (clamp01(suitability) * 0.7 + clamp01(pressure) * 0.3) * s * (1.0 - s);
        double recovery = (0.01 + clamp01(treatment) * 0.25 + clamp01(recoveryBonus) * 0.04) * s;
        return clamp01(s + growth - recovery);
    }

    public static double impactFactor(double burden, double cap) {
        return 1.0 - clamp01(burden) * clamp01(cap);
    }

    private static double clamp01(double v) {
        return Math.max(0.0, Math.min(1.0, v));
    }
}
