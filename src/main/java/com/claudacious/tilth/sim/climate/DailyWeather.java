package com.claudacious.tilth.sim.climate;

/**
 * Weather for one real calendar day at one location.
 *
 * @param day               real day index since 1 January, year 1
 * @param tMin              minimum air temperature (degrees C)
 * @param tMax              maximum air temperature (degrees C)
 * @param rain              liquid precipitation (mm)
 * @param snow              precipitation falling as snow, water equivalent (mm)
 * @param et0               reference evapotranspiration (mm)
 * @param relativeHumidity  mean relative humidity (%)
 * @param leafWetnessHours  hours of leaf wetness (rain or dew), important for fungal infection
 */
public record DailyWeather(long day, double tMin, double tMax, double rain, double snow, double et0,
                           double relativeHumidity, double leafWetnessHours) {

    public double tMean() {
        return (tMin + tMax) / 2.0;
    }

    public double precipitation() {
        return rain + snow;
    }

    public boolean frost() {
        return tMin < 0.0;
    }
}
