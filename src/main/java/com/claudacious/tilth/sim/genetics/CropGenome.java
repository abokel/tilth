package com.claudacious.tilth.sim.genetics;

import org.jetbrains.annotations.Nullable;

/** Stable heritable crop traits. */
public record CropGenome(String speciesId, String cultivar, double growth, double yield, double drought, double frost, int phenotype) {
    public static CropGenome landrace(String speciesId) {
        return new CropGenome(speciesId, "landrace", 0, 0, 0, 0, 0);
    }

    public CropGenome normalized() {
        return new CropGenome(speciesId, cultivar == null || cultivar.isBlank() ? "landrace" : cultivar,
                clamp(growth), clamp(yield), clamp(drought), clamp(frost), Math.floorMod(phenotype, 12));
    }

    public double growthMultiplier() {
        return 1.0 + growth * 0.45;
    }

    public double yieldMultiplier() {
        return 1.0 + yield * 0.55;
    }

    public double droughtBonusDays() {
        return drought * 6.0;
    }

    public double frostToleranceC() {
        return frost * 2.5;
    }

    public String encode() {
        CropGenome g = normalized();
        return g.speciesId + "|" + g.cultivar + "|" + g.growth + "|" + g.yield + "|" + g.drought + "|" + g.frost + "|" + g.phenotype;
    }

    @Nullable
    public static CropGenome tryDecode(String encoded) {
        if (encoded == null || encoded.isBlank()) {
            return null;
        }
        String[] p = encoded.split("\\|", -1);
        if (p.length != 7 || p[0].isBlank()) {
            return null;
        }
        try {
            double growth = Double.parseDouble(p[2]);
            double yield = Double.parseDouble(p[3]);
            double drought = Double.parseDouble(p[4]);
            double frost = Double.parseDouble(p[5]);
            if (!finite(growth) || !finite(yield) || !finite(drought) || !finite(frost)) {
                return null;
            }
            return new CropGenome(p[0], p[1], growth, yield, drought, frost, Integer.parseInt(p[6])).normalized();
        } catch (RuntimeException e) {
            return null;
        }
    }

    public static CropGenome decode(String encoded) {
        CropGenome decoded = tryDecode(encoded);
        return decoded == null ? landrace("unknown") : decoded;
    }

    private static double clamp(double v) {
        if (!finite(v)) {
            return 0.0;
        }
        return Math.max(-0.45, Math.min(0.45, v));
    }

    private static boolean finite(double v) {
        return Double.isFinite(v);
    }
}
