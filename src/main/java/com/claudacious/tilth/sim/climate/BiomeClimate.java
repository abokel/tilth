package com.claudacious.tilth.sim.climate;

import com.claudacious.tilth.sim.Noise;

/**
 * Long-term climate normals for a location.
 *
 * @param meanTemp          mean annual air temperature (degrees C)
 * @param amplitude         half the difference between the warmest and coldest month (degrees C)
 * @param diurnalRange      typical daily max minus daily min (degrees C)
 * @param annualPrecip      mean annual precipitation (mm)
 * @param precipSeasonality -1 = winter rainfall maximum, 0 = even, +1 = summer (monsoon) maximum
 * @param humidity          0-1 general atmospheric moisture, drives relative humidity and dew
 */
public record BiomeClimate(double meanTemp, double amplitude, double diurnalRange, double annualPrecip,
                           double precipSeasonality, double humidity) {

    public BiomeClimate adjusted(double tempOffset, double precipMultiplier) {
        return new BiomeClimate(meanTemp + tempOffset, amplitude, diurnalRange, annualPrecip * precipMultiplier,
                precipSeasonality, humidity);
    }

    /** 0 (arid, base-rich soils) to 1 (humid, strongly leached soils). */
    public double leachingIndex() {
        return Noise.clamp((annualPrecip - 250.0) / 1200.0, 0.0, 1.0);
    }

    /** Soil pH that weathering trends toward under this climate. */
    public double equilibriumPh() {
        double ph = 8.0 - 2.6 * leachingIndex();
        if (annualPrecip > 400) {
            ph -= Noise.clamp((6.0 - meanTemp) / 10.0, 0.0, 1.0) * 1.2;
        }
        return Noise.clamp(ph, 4.2, 8.4);
    }

    /** Rough Koppen-style label for debug output. */
    public String describe() {
        if (annualPrecip < 250) {
            return meanTemp > 18 ? "hot desert" : "cold desert";
        }
        if (annualPrecip < 500 && meanTemp > 8) {
            return "semi-arid steppe";
        }
        if (meanTemp >= 22 && annualPrecip > 1500) {
            return "tropical rainforest";
        }
        if (meanTemp >= 20) {
            return precipSeasonality > 0.5 ? "tropical savanna" : "subtropical";
        }
        if (meanTemp - amplitude < -15) {
            return meanTemp < -5 ? "polar/tundra" : "boreal (subarctic)";
        }
        if (meanTemp - amplitude < -3) {
            return "humid continental";
        }
        return amplitude < 8 ? "temperate oceanic" : "humid temperate";
    }
}
