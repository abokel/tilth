package com.claudacious.tilth.sim.soil;

/**
 * Soil water characteristics.
 *
 * @param wiltingPoint  volumetric water content at -1500 kPa (m3/m3)
 * @param fieldCapacity volumetric water content at -33 kPa (m3/m3)
 * @param saturation    volumetric water content at saturation (m3/m3)
 * @param ksat          saturated hydraulic conductivity (mm/h)
 * @param bulkDensity   dry bulk density (g/cm3)
 */
public record Hydraulics(double wiltingPoint, double fieldCapacity, double saturation, double ksat, double bulkDensity) {
    public static final Hydraulics PEAT = new Hydraulics(0.25, 0.55, 0.85, 8.0, 0.3);

    public double availableWater() {
        return fieldCapacity - wiltingPoint;
    }

    /** Converts kg/ha of a nutrient to ppm in the 20 cm plough layer. */
    public double ppmPerKgHa() {
        return 1.0 / (2.0 * bulkDensity);
    }

    /** Converts g/m2 of a nutrient to ppm in the 20 cm plough layer. */
    public double ppmPerGramM2() {
        return 5.0 / bulkDensity;
    }
}
