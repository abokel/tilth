package com.claudacious.tilth.sim.soil;

import com.claudacious.tilth.sim.Noise;
import com.claudacious.tilth.sim.SimSettings;

/**
 * Daily root-zone water balance (a single "bucket" in the style of FAO-56 chapter 8).
 *
 * <p>Inputs are rain and snowmelt (minus SCS curve-number runoff), irrigation seepage and capillary
 * rise from a shallow water table. Outputs are crop transpiration (reduced by the water stress
 * coefficient Ks), soil evaporation (reduced by residue mulch), and drainage below field capacity
 * limited by saturated conductivity and the water table. Water that cannot infiltrate past
 * saturation is returned as ponding, which the world uses to form puddles and ponds.
 */
public final class SoilWater {
    private SoilWater() {
    }

    /**
     * @param ks            water stress coefficient for the crop (1 = no stress)
     * @param drainage      deep percolation below the root zone (mm), carries leached nutrients
     * @param runoff        surface runoff (mm), drives erosion
     * @param ponding       water left standing on a saturated surface (mm)
     * @param evapotranspiration actual evapotranspiration (mm)
     * @param waterlogged   root zone near saturation (anoxic)
     */
    public record Step(double ks, double drainage, double runoff, double ponding, double evapotranspiration,
                       boolean waterlogged) {
    }

    public static double layerMm() {
        return SimSettings.rootZoneDepthMeters * 1000.0;
    }

    /**
     * @param input        rain + snowmelt reaching the surface (mm)
     * @param irrigation   water supplied laterally from ditches/ponds (mm)
     * @param capillary    upward flux from a shallow water table (mm)
     * @param et0          reference evapotranspiration (mm)
     * @param kc           crop coefficient (0 for bare soil)
     * @param rootDepth    current rooting depth of the crop (m)
     * @param depletion    FAO "p": fraction of available water the crop can use before stress
     * @param slopeFactor  1 on flat ground, larger on slopes (more runoff)
     */
    public static Step step(SoilState s, SoilTexture tex, Hydraulics h, double input, double irrigation, double capillary,
                            double et0, double kc, double rootDepth, double depletion, double slopeFactor) {
        double layer = layerMm();
        double fc = h.fieldCapacity();
        double wp = h.wiltingPoint();
        double sat = h.saturation();
        double ksat = h.ksat() * (1.0 - 0.8 * s.compaction) * (0.6 + 0.8 * s.tilth);

        // Runoff: SCS curve number. Compaction, slope and an already-wet profile all shrink retention.
        double wetness = Noise.clamp((s.moisture - wp) / Math.max(0.01, sat - wp), 0, 1);
        double retention = tex.retentionMm() * (1.0 - 0.5 * s.compaction) / Math.max(1.0, slopeFactor) * (1.3 - 0.8 * wetness);
        double ia = 0.2 * retention;
        double runoff = input > ia ? (input - ia) * (input - ia) / (input - ia + retention) : 0;
        double water = s.moisture * layer + (input - runoff) + irrigation + capillary;

        // Crop water stress (FAO-56 eq. 84) evaluated over the rooted depth.
        double theta = water / layer;
        double ks = 1.0;
        boolean cropActive = kc > 0;
        if (cropActive) {
            double taw = Math.max(1.0, (fc - wp) * 1000.0 * rootDepth);
            double raw = depletion * taw;
            double dr = Math.max(0, (fc - theta) * 1000.0 * rootDepth);
            ks = dr <= raw ? 1.0 : Noise.clamp((taw - dr) / ((1.0 - depletion) * taw), 0, 1);
        }

        // Evapotranspiration: transpiration plus evaporation from the uncovered, unmulched surface.
        double cover = cropActive ? Math.min(1.0, kc / 1.15) : 0;
        double ke = Noise.clamp((theta - 0.5 * wp) / Math.max(0.01, fc - 0.5 * wp), 0.05, 1.0) * 0.9;
        double mulch = 1.0 - Math.min(0.5, s.residue * (1.0 - s.incorporatedFraction) * 0.8);
        double et = (cropActive ? kc * ks * et0 : 0) + ke * (1 - cover) * et0 * mulch;
        double residual = 0.5 * wp * layer;
        et = Math.min(et, Math.max(0, water - residual));
        water -= et;

        // Drainage of water above field capacity, impeded by low conductivity and a high water table.
        double drainage = 0;
        theta = water / layer;
        if (theta > fc) {
            double excess = (theta - fc) * layer;
            double capacity = ksat * 24.0 * 0.5;
            double tableFactor = Noise.clamp(s.waterTableDepth / 2.0, 0.05, 1.0);
            drainage = Math.min(excess, capacity * tableFactor);
            water -= drainage;
        }

        double ponding = 0;
        if (water > sat * layer) {
            ponding = water - sat * layer;
            water = sat * layer;
        }
        s.moisture = water / layer;
        boolean waterlogged = s.moisture > fc + 0.6 * (sat - fc);
        s.waterloggedDays = waterlogged ? s.waterloggedDays + 1 : 0;
        return new Step(ks, drainage, runoff, ponding, et, waterlogged);
    }
}
