package com.claudacious.tilth.sim.soil;

import com.claudacious.tilth.sim.Noise;

/**
 * Saxton &amp; Rawls (2006) pedotransfer functions: water retention and conductivity from sand,
 * clay and organic matter. Soil Sci. Soc. Am. J. 70:1569-1578.
 */
public final class Pedotransfer {
    private Pedotransfer() {
    }

    /**
     * @param s  sand fraction (0-1)
     * @param c  clay fraction (0-1)
     * @param om organic matter (% by weight); the regression is fitted up to 8%
     */
    public static Hydraulics saxtonRawls(double s, double c, double om) {
        om = Noise.clamp(om, 0.0, 8.0);

        double t1500 = -0.024 * s + 0.487 * c + 0.006 * om + 0.005 * (s * om) - 0.013 * (c * om) + 0.068 * (s * c) + 0.031;
        double wp = t1500 + (0.14 * t1500 - 0.02);

        double t33 = -0.251 * s + 0.195 * c + 0.011 * om + 0.006 * (s * om) - 0.027 * (c * om) + 0.452 * (s * c) + 0.299;
        double fc = t33 + (1.283 * t33 * t33 - 0.374 * t33 - 0.015);

        double ts33 = 0.278 * s + 0.034 * c + 0.022 * om - 0.018 * (s * om) - 0.027 * (c * om) - 0.584 * (s * c) + 0.078;
        double s33 = ts33 + (0.636 * ts33 - 0.107);

        double sat = fc + s33 - 0.097 * s + 0.043;

        wp = Noise.clamp(wp, 0.01, 0.45);
        fc = Noise.clamp(fc, wp + 0.02, 0.6);
        sat = Noise.clamp(sat, fc + 0.03, 0.7);

        double bulkDensity = (1.0 - sat) * 2.65;
        double b = (Math.log(1500) - Math.log(33)) / (Math.log(fc) - Math.log(wp));
        double lambda = 1.0 / b;
        double ksat = 1930.0 * Math.pow(Math.max(0.001, sat - fc), 3.0 - lambda);

        return new Hydraulics(wp, fc, sat, Noise.clamp(ksat, 0.05, 300.0), Noise.clamp(bulkDensity, 0.9, 1.8));
    }
}
