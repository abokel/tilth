package com.claudacious.tilth.sim.crop;

import com.claudacious.tilth.sim.climate.DailyWeather;

/**
 * Weeds and glass. Weed pressure on a field runs from 0 (clean) to 1 (overrun). Weeds come up fastest on warm, moist,
 * fertile ground; a closed crop canopy or a cover crop shades them out, and a mulch of residue holds them back.
 * Tillage and hoeing clear them. They compete with the crop for light, water and nitrogen.
 *
 * <p>A glass roof makes a greenhouse: nights stay above frost, days are warmer, and no rain falls inside.
 */
public final class WeedModel {
    private WeedModel() {
    }

    /**
     * One day of weed growth.
     *
     * @param weeds      pressure today (0..1)
     * @param tMean      mean air temperature (C)
     * @param moisture   relative soil moisture (0 dry .. 1 field capacity)
     * @param nitrogen   mineral nitrogen (ppm)
     * @param residue    surface residue (kg/m2), a mulch
     * @param canopy     crop ground cover (0 bare .. 1 closed)
     * @param coverCrop  whether the crop is grown as a cover (dense, weed-suppressing)
     */
    public static double grow(double weeds, double tMean, double moisture, double nitrogen, double residue, double canopy,
                              boolean coverCrop) {
        double warmth = clamp((tMean - 6.0) / 14.0, 0, 1);
        double wet = clamp(moisture, 0, 1.2);
        double fertile = 0.5 + 0.5 * clamp(nitrogen / 30.0, 0, 1);
        double mulch = 1.0 - 0.6 * clamp(residue / 0.6, 0, 1);
        double shade = 1.0 - 0.8 * clamp(canopy, 0, 1);
        // Logistic growth from a seed bank that never quite runs out.
        double rate = 0.16 * warmth * wet * fertile * mulch * shade;
        double next = weeds + rate * (weeds + 0.05) * (1.0 - weeds);
        if (coverCrop) {
            next -= 0.02 * clamp(canopy, 0, 1);
        }
        if (tMean < 0) {
            next -= 0.01;
        }
        return clamp(next, 0, 1);
    }

    /** How much of the light, water and nitrogen the crop keeps against this much weed: 1 clean, 0.55 overrun. */
    public static double competition(double weeds) {
        return 1.0 - 0.45 * clamp(weeds, 0, 1);
    }

    /** Daily weather under a glass roof: frost kept off, warmer days, still air, and no rain or snow. */
    public static DailyWeather underGlass(DailyWeather w) {
        double tMin = Math.max(w.tMin() + 6.0, 2.0);
        double tMax = Math.min(w.tMax() + 5.0, Math.max(w.tMax(), 38.0));
        return new DailyWeather(w.day(), tMin, Math.max(tMin + 2.0, tMax), 0.0, 0.0, w.et0() * 0.75,
                Math.min(1.0, w.relativeHumidity() + 0.1), w.leafWetnessHours() * 0.5);
    }

    private static double clamp(double v, double lo, double hi) {
        return Math.max(lo, Math.min(hi, v));
    }
}
