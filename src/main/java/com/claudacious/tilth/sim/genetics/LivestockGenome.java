package com.claudacious.tilth.sim.genetics;

/** Stable heritable livestock traits. */
public record LivestockGenome(double appetite, double feedEfficiency, double produce, double bodySize, int phenotype) {
    public static LivestockGenome baseline() {
        return new LivestockGenome(0, 0, 0, 0, 0);
    }

    public LivestockGenome normalized() {
        return new LivestockGenome(clamp(appetite), clamp(feedEfficiency), clamp(produce), clamp(bodySize), Math.floorMod(phenotype, 12));
    }

    public double appetiteMultiplier() {
        return 1.0 + appetite * 0.55;
    }

    public double feedMultiplier() {
        return 1.0 + feedEfficiency * 0.55;
    }

    public double produceMultiplier() {
        return 1.0 + produce * 0.5;
    }

    public double scaleMultiplier() {
        return 1.0 + bodySize * 0.2;
    }

    public double phenotypeScaleMultiplier() {
        return 0.92 + Math.floorMod(phenotype, 12) * (0.16 / 11.0);
    }

    public String encode() {
        LivestockGenome g = normalized();
        return g.appetite + "|" + g.feedEfficiency + "|" + g.produce + "|" + g.bodySize + "|" + g.phenotype;
    }

    public static LivestockGenome decode(String encoded) {
        if (encoded == null || encoded.isBlank()) {
            return baseline();
        }
        String[] p = encoded.split("\\|", -1);
        if (p.length != 5) {
            return baseline();
        }
        try {
            double appetite = Double.parseDouble(p[0]);
            double feedEfficiency = Double.parseDouble(p[1]);
            double produce = Double.parseDouble(p[2]);
            double bodySize = Double.parseDouble(p[3]);
            if (!finite(appetite) || !finite(feedEfficiency) || !finite(produce) || !finite(bodySize)) {
                return baseline();
            }
            return new LivestockGenome(appetite, feedEfficiency, produce, bodySize, Integer.parseInt(p[4])).normalized();
        } catch (RuntimeException e) {
            return baseline();
        }
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
