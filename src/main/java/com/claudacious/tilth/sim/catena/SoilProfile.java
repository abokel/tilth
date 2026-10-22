package com.claudacious.tilth.sim.catena;

import com.claudacious.tilth.sim.climate.BiomeClimate;
import com.claudacious.tilth.sim.soil.DrainageClass;
import com.claudacious.tilth.sim.soil.SoilTexture;
import com.claudacious.tilth.sim.soil.TextureTriangle;

/**
 * A soil profile: what the worldgen places in a column, from the surface down.
 *
 * @param topsoil      A-horizon texture (PEAT for organic soils)
 * @param subsoil      B-horizon texture
 * @param topsoilDepth thickness of the A horizon in blocks
 * @param subsoilDepth thickness of the B horizon in blocks
 * @param gleyed       subsoil is a grey, reduced Bg horizon (waterlogged most of the year)
 * @param drainage     natural drainage class
 */
public record SoilProfile(SoilTexture topsoil, SoilTexture subsoil, int topsoilDepth, int subsoilDepth, boolean gleyed,
                          DrainageClass drainage, ParentMaterial parentMaterial, SlopePosition position) {

    /**
     * @param jitterSand small-scale sand variation (percentage points)
     * @param jitterClay small-scale clay variation (percentage points)
     * @param depthNoise -1, 0 or 1 extra blocks of topsoil
     */
    public static SoilProfile build(ParentMaterial pm, SoilRegion region, SlopePosition pos, BiomeClimate climate,
                                    double jitterSand, double jitterClay, int depthNoise) {
        double sand = pm.sand() + pos.sandDelta() + jitterSand;
        double clay = pm.clay() + pos.clayDelta() + jitterClay;
        // Arid soils keep their sand; humid ones weather feldspar into clay.
        clay += (climate.leachingIndex() - 0.4) * 4.0;
        SoilTexture top = TextureTriangle.classify(sand, clay);

        // Clay is washed down (illuviated) into the B horizon in humid climates: an argillic horizon.
        double illuviation = 3.0 + 6.0 * climate.leachingIndex();
        SoilTexture sub = TextureTriangle.classify(sand - 2.0, clay + illuviation);

        int drainScore = pos.drainageScore();
        if (sub.sand() >= 70) {
            drainScore--;
        }
        if (sub.clay() >= 40) {
            drainScore++;
        }
        if (climate.annualPrecip() < 400) {
            drainScore--;
        }
        if (climate.annualPrecip() > 1200) {
            drainScore++;
        }
        if (region == SoilRegion.SWAMP) {
            drainScore++;
        }
        DrainageClass drainage = DrainageClass.ofIndex(drainScore);

        boolean peat = region.formsPeat()
                && (pos == SlopePosition.DEPRESSION || (pos == SlopePosition.TOESLOPE && drainage.isWet()))
                && climate.annualPrecip() > 400 && climate.meanTemp() < 18;
        if (peat) {
            top = SoilTexture.PEAT;
        }

        int topDepth = Math.max(1, pos.topsoilDepth() + depthNoise + (peat ? 1 : 0));
        int subDepth = Math.max(1, pos.subsoilDepth() + (climate.leachingIndex() > 0.5 ? 1 : 0));
        boolean gleyed = drainage.isWet();
        return new SoilProfile(top, sub, topDepth, subDepth, gleyed, drainage, pm, pos);
    }
}
