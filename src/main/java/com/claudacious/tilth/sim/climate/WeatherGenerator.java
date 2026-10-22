package com.claudacious.tilth.sim.climate;

import com.claudacious.tilth.sim.Noise;
import com.claudacious.tilth.sim.SimSettings;
import com.claudacious.tilth.sim.calendar.CalendarMath;

/**
 * Deterministic stochastic weather generator.
 *
 * <p>Temperature follows the seasonal cycle of the local climate plus a synoptic anomaly (cold snaps,
 * heat waves) that varies smoothly across ~1 km weather regions. Rain occurrence is a per-region
 * Bernoulli draw whose probability tracks the climate's monthly rainfall, and wet-day amounts are
 * exponentially distributed so monthly totals match the climate normals. Because everything is a
 * pure function of (seed, day, position) the same day can be regenerated when a chunk catches up.
 */
public final class WeatherGenerator {
    private static final double REGION_SIZE = 1024.0;

    private WeatherGenerator() {
    }

    /**
     * @param altitudeCooling degrees C to subtract for elevation (lapse rate) and latitude
     * @param observedRain    true if Minecraft weather actually rained here on this day
     */
    public static DailyWeather generate(long seed, BiomeClimate climate, long day, double x, double z,
                                        double altitudeCooling, boolean observedRain) {
        int doy = CalendarMath.dayOfYear(day);
        double gx = x / REGION_SIZE;
        double gz = z / REGION_SIZE;

        double anomaly = regionalAnomaly(seed, day, gx, gz);
        double seasonal = ClimateMath.seasonalMean(climate, doy);
        double tMean = seasonal + anomaly - altitudeCooling + SimSettings.temperatureOffset;

        long cellX = Math.round(gx);
        long cellZ = Math.round(gz);
        double meanPrecip = ClimateMath.meanDailyPrecip(climate, doy) * SimSettings.precipitationMultiplier;
        double wetProbability = Noise.clamp(0.12 + 0.09 * meanPrecip, 0.04, 0.70);
        if (meanPrecip <= 0.0) {
            wetProbability = 0;
        }
        double u = Noise.unit(seed ^ 0x5241494E2D444159L, day, cellX, cellZ);
        boolean wet = u < wetProbability;
        double precip = 0;
        if (wet) {
            double u2 = Noise.unit(seed ^ 0x414D4F554E54L, day, cellX, cellZ);
            precip = Math.min(90.0, meanPrecip / wetProbability * -Math.log(1.0 - u2 * 0.999));
        }
        if (observedRain && climate.annualPrecip() > 0) {
            wet = true;
            precip = Math.max(precip, 0.6 * meanPrecip / Math.max(0.05, wetProbability) + 3.0);
        }

        double u3 = Noise.unit(seed ^ 0x44545221L, day, cellX, cellZ);
        double dtr = climate.diurnalRange() * (wet ? 0.55 : 1.0) * (0.85 + 0.3 * u3);
        double tMax = tMean + dtr / 2.0;
        double tMin = tMean - dtr / 2.0;

        double ra = ClimateMath.extraterrestrialRadiationMm(doy, SimSettings.latitudeDegrees);
        double et0 = ClimateMath.hargreavesEt0(tMin, tMax, ra) * (wet ? 0.7 : 1.0) * SimSettings.evapotranspirationMultiplier;

        double rh;
        double wetHours;
        if (wet) {
            rh = 88 + 9 * u3;
            wetHours = Math.min(24, 8 + precip * 0.6 + 6 * u3);
        } else {
            rh = Noise.clamp(30 + 55 * climate.humidity() + 15 * (u3 - 0.5), 10, 95);
            wetHours = rh > 80 ? 6 + 4 * u3 : (rh > 65 ? 2 + 2 * u3 : 0);
        }

        double rain = precip;
        double snow = 0;
        if (tMean < 0.5) {
            snow = precip;
            rain = 0;
        }
        return new DailyWeather(day, tMin, tMax, rain, snow, et0, rh, wetHours);
    }

    /** Synoptic temperature anomaly blended bilinearly between weather regions. */
    private static double regionalAnomaly(long seed, long day, double gx, double gz) {
        long ix = (long) Math.floor(gx);
        long iz = (long) Math.floor(gz);
        double fx = gx - ix;
        double fz = gz - iz;
        double a = cellAnomaly(seed, day, ix, iz);
        double b = cellAnomaly(seed, day, ix + 1, iz);
        double c = cellAnomaly(seed, day, ix, iz + 1);
        double d = cellAnomaly(seed, day, ix + 1, iz + 1);
        return Noise.lerp(Noise.lerp(a, b, fx), Noise.lerp(c, d, fx), fz);
    }

    private static double cellAnomaly(long seed, long day, long ix, long iz) {
        double shiftSlow = Noise.unit(seed ^ 0x534C4F57L, ix, iz) * 997.0;
        double shiftFast = Noise.unit(seed ^ 0x46415354L, ix, iz) * 991.0;
        return 4.0 * Noise.value(seed ^ 0x51EDL, day / 5.0 + shiftSlow)
                + 1.5 * Noise.value(seed ^ 0xA11CEL, day / 1.5 + shiftFast);
    }
}
