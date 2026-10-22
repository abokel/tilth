package com.claudacious.tilth.sim;

import com.claudacious.tilth.sim.climate.BiomeClimate;
import com.claudacious.tilth.sim.climate.DailyWeather;
import com.claudacious.tilth.sim.crop.CropGrowthModel;
import com.claudacious.tilth.sim.crop.CropSpecies;
import com.claudacious.tilth.sim.crop.CropState;
import com.claudacious.tilth.sim.soil.Hydraulics;
import com.claudacious.tilth.sim.soil.SoilChemistry;
import com.claudacious.tilth.sim.soil.SoilState;
import com.claudacious.tilth.sim.soil.SoilTexture;
import com.claudacious.tilth.sim.soil.SoilWater;

/**
 * Advances one block of field by one real day: snow, water balance, soil chemistry and (if
 * present) the crop, in that order, so the crop sees today's soil moisture and nutrient pools.
 */
public final class FieldSimulator {
    private FieldSimulator() {
    }

    /**
     * Site conditions that do not change from day to day.
     *
     * @param irrigation      0-1 strength of lateral seepage from nearby water (distance-weighted)
     * @param flooded         standing water directly beside the block (paddy conditions)
     * @param slopeFactor     1 on flat ground, up to ~3 on steep slopes
     * @param capillaryFactor how readily the soil wicks water up from a shallow water table
     */
    public record Inputs(double irrigation, boolean flooded, double slopeFactor, double capillaryFactor) {
        public static final Inputs NONE = new Inputs(0, false, 1, 1);
    }

    /** Called once per day before growth when diseases, pests and fungi are enabled. */
    @FunctionalInterface
    public interface AfflictionHook {
        void apply(CropSpecies species, CropState crop, SoilState soil, DailyWeather weather);
    }

    public record Report(SoilWater.Step water, double snowmelt) {
    }

    public static Report step(SoilState s, SoilTexture tex, BiomeClimate climate, DailyWeather w, Inputs in,
                              CropSpecies species, CropState crop, AfflictionHook hook) {
        Hydraulics h = tex.hydraulics(s.organicMatter);
        double layer = SoilWater.layerMm();

        s.snowpack += w.snow();
        double melt = 0;
        if (s.snowpack > 0 && w.tMean() > 0) {
            melt = Math.min(s.snowpack, 3.5 * w.tMean());
            s.snowpack -= melt;
        }

        boolean cropActive = species != null && crop != null && !crop.dead && crop.germinated;
        double kc = cropActive ? species.kc(crop.dev) : 0;
        double rootDepth = cropActive ? species.rootDepth(crop.dev) : 0.15;
        double depletion = species != null ? species.depletionFraction : 0.5;

        double irrigation;
        if (in.flooded()) {
            irrigation = Math.max(0, (h.saturation() - s.moisture) * layer);
        } else {
            irrigation = Math.min(in.irrigation() * 8.0, Math.max(0, (h.fieldCapacity() - s.moisture) * layer));
        }
        double capillary = 0;
        if (s.waterTableDepth < 1.5) {
            capillary = (1.5 - s.waterTableDepth) / 1.5 * 3.0 * in.capillaryFactor();
            capillary = Math.min(capillary, Math.max(0, (h.fieldCapacity() - s.moisture) * layer));
        }

        SoilWater.Step water = SoilWater.step(s, tex, h, w.rain() + melt, irrigation, capillary, w.et0(), kc, rootDepth,
                depletion, in.slopeFactor());
        SoilChemistry.step(s, tex, h, w, water.drainage(), water.waterlogged(), climate);

        if (species != null && crop != null && !crop.dead) {
            if (hook != null && crop.germinated) {
                hook.apply(species, crop, s, w);
            }
            CropGrowthModel.step(species, crop, s, h, w, water);
        }
        return new Report(water, melt);
    }
}
