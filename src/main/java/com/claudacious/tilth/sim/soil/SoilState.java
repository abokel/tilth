package com.claudacious.tilth.sim.soil;

import java.util.ArrayList;
import java.util.List;

/**
 * Mutable chemical, physical and hydrological state of one block of farmed soil (one square metre of
 * field). Nutrient pools are plant-available concentrations in the 20 cm plough layer, the units a
 * soil-test lab reports.
 */
public final class SoilState {
    public static final int HISTORY_LENGTH = 6;

    /** Mineral nitrogen, nitrate + ammonium (ppm). */
    public double nitrogen = 15;
    /** Plant-available phosphorus, Olsen/Bray extractable (ppm). */
    public double phosphorus = 15;
    /** Exchangeable potassium (ppm). */
    public double potassium = 150;
    /** Sulfate sulfur (ppm). */
    public double sulfur = 10;
    /** Exchangeable calcium (ppm). */
    public double calcium = 1200;
    /** Exchangeable magnesium (ppm). */
    public double magnesium = 180;

    public double ph = 6.5;
    /** Soil organic matter (% by weight). */
    public double organicMatter = 2.5;
    /** Volumetric water content of the root zone (m3/m3). */
    public double moisture = 0.25;
    /** 0 = loose, freshly worked; 1 = severely compacted plough pan. */
    public double compaction = 0.15;
    /** Aggregate stability / seedbed quality, 0-1. */
    public double tilth = 0.6;
    /** Crop residue dry matter on or in the soil (kg/m2). */
    public double residue = 0;
    /** Nitrogen concentration of that residue (% of dry matter); below ~1.5% it locks up soil N. */
    public double residueNitrogen = 0.6;
    /** Share of the residue mixed into the soil by tillage (decomposes much faster than surface mulch). */
    public double incorporatedFraction = 0;
    /** Snow water equivalent sitting on the field (mm). */
    public double snowpack = 0;
    /** Days remaining of the mineralisation flush that follows tillage. */
    public double tillageFlushDays = 0;
    /** Electrical conductivity of the saturated paste (dS/m). */
    public double salinity = 0.3;
    /** Estimated depth to the water table (m). */
    public double waterTableDepth = 3.0;
    public int waterloggedDays = 0;
    /** Previous crops, most recent last, as "cropId@year". */
    public final List<String> cropHistory = new ArrayList<>();

    public double get(Nutrient n) {
        return switch (n) {
            case N -> nitrogen;
            case P -> phosphorus;
            case K -> potassium;
            case S -> sulfur;
            case CA -> calcium;
            case MG -> magnesium;
        };
    }

    public void set(Nutrient n, double value) {
        double v = Math.max(0, value);
        switch (n) {
            case N -> nitrogen = v;
            case P -> phosphorus = v;
            case K -> potassium = v;
            case S -> sulfur = v;
            case CA -> calcium = v;
            case MG -> magnesium = v;
        }
    }

    public void add(Nutrient n, double delta) {
        set(n, get(n) + delta);
    }

    public void addResidue(double kgPerM2, double nitrogenPercent) {
        if (kgPerM2 <= 0) {
            return;
        }
        double total = residue + kgPerM2;
        residueNitrogen = (residue * residueNitrogen + kgPerM2 * nitrogenPercent) / total;
        incorporatedFraction = (residue * incorporatedFraction) / total;
        residue = total;
    }

    public void recordCrop(String cropId, int year) {
        cropHistory.add(cropId + "@" + year);
        while (cropHistory.size() > HISTORY_LENGTH) {
            cropHistory.remove(0);
        }
    }

    /** Number of times the crop appears in the history within the last {@code years} years. */
    public int timesGrownWithin(String cropId, int currentYear, int years) {
        int count = 0;
        for (String entry : cropHistory) {
            int at = entry.lastIndexOf('@');
            if (at < 0) {
                continue;
            }
            String id = entry.substring(0, at);
            int year;
            try {
                year = Integer.parseInt(entry.substring(at + 1));
            } catch (NumberFormatException e) {
                continue;
            }
            if (id.equals(cropId) && currentYear - year <= years) {
                count++;
            }
        }
        return count;
    }

    public SoilState copy() {
        SoilState c = new SoilState();
        c.nitrogen = nitrogen;
        c.phosphorus = phosphorus;
        c.potassium = potassium;
        c.sulfur = sulfur;
        c.calcium = calcium;
        c.magnesium = magnesium;
        c.ph = ph;
        c.organicMatter = organicMatter;
        c.moisture = moisture;
        c.compaction = compaction;
        c.tilth = tilth;
        c.residue = residue;
        c.residueNitrogen = residueNitrogen;
        c.incorporatedFraction = incorporatedFraction;
        c.snowpack = snowpack;
        c.tillageFlushDays = tillageFlushDays;
        c.salinity = salinity;
        c.waterTableDepth = waterTableDepth;
        c.waterloggedDays = waterloggedDays;
        c.cropHistory.addAll(cropHistory);
        return c;
    }
}
