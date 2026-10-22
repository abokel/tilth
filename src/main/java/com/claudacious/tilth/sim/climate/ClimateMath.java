package com.claudacious.tilth.sim.climate;

import com.claudacious.tilth.sim.Noise;

/** Standard agro-meteorological formulas. */
public final class ClimateMath {
    private ClimateMath() {
    }

    /** Seasonal mean temperature: coldest around 15 January, warmest around 15 July. */
    public static double seasonalMean(BiomeClimate c, double dayOfYear) {
        return c.meanTemp() + c.amplitude() * Math.sin(2 * Math.PI * (dayOfYear - 105) / 365.0);
    }

    /** Mean daily precipitation (mm) for the day of the year, including monsoon/winter-rain seasonality. */
    public static double meanDailyPrecip(BiomeClimate c, double dayOfYear) {
        double base = c.annualPrecip() / 365.0;
        return Math.max(0, base * (1 + c.precipSeasonality() * Math.sin(2 * Math.PI * (dayOfYear - 105) / 365.0)));
    }

    /** Extraterrestrial radiation expressed as evaporation equivalent (mm/day), FAO-56 eq. 21. */
    public static double extraterrestrialRadiationMm(int dayOfYear, double latitudeDeg) {
        double phi = Math.toRadians(latitudeDeg);
        double j = dayOfYear + 1;
        double dr = 1 + 0.033 * Math.cos(2 * Math.PI * j / 365.0);
        double delta = 0.409 * Math.sin(2 * Math.PI * j / 365.0 - 1.39);
        double ws = Math.acos(Noise.clamp(-Math.tan(phi) * Math.tan(delta), -1, 1));
        double ra = (24 * 60 / Math.PI) * 0.0820 * dr
                * (ws * Math.sin(phi) * Math.sin(delta) + Math.cos(phi) * Math.cos(delta) * Math.sin(ws));
        return Math.max(0, ra * 0.408);
    }

    /** Hargreaves-Samani reference evapotranspiration (mm/day). */
    public static double hargreavesEt0(double tMin, double tMax, double raMm) {
        double tMean = (tMin + tMax) / 2;
        return Math.max(0, 0.0023 * (tMean + 17.8) * Math.sqrt(Math.max(0, tMax - tMin)) * raMm);
    }

    /** Growing degree days with horizontal cut-offs (the "modified" method used for maize). */
    public static double growingDegreeDays(double tMin, double tMax, double tBase, double tCutoff) {
        double hi = Noise.clamp(tMax, tBase, tCutoff);
        double lo = Noise.clamp(tMin, tBase, tCutoff);
        return Math.max(0, (hi + lo) / 2 - tBase);
    }

    /**
     * Yan & Hunt (1999) temperature response: 0 at the base and maximum temperatures, 1 at the
     * optimum, asymmetric in between.
     */
    public static double temperatureResponse(double t, double tBase, double tOpt, double tMax) {
        if (t <= tBase || t >= tMax) {
            return 0;
        }
        double exponent = (tOpt - tBase) / (tMax - tOpt);
        double r = ((tMax - t) / (tMax - tOpt)) * Math.pow((t - tBase) / (tOpt - tBase), exponent);
        return Noise.clamp(r, 0, 1);
    }

    /** USDA plant hardiness zone (e.g. 5.5 means "5b") estimated from climate normals. */
    public static double hardinessZone(BiomeClimate c) {
        double extremeMinC = c.meanTemp() - c.amplitude() - c.diurnalRange() / 2.0 - 9.0;
        double f = extremeMinC * 1.8 + 32;
        return Noise.clamp((f + 60) / 10.0 + 1.0, 1.0, 13.9);
    }

    public static String hardinessLabel(double zone) {
        int whole = (int) Math.floor(zone);
        return whole + ((zone - whole) < 0.5 ? "a" : "b");
    }
}
