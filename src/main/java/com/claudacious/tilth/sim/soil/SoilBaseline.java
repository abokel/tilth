package com.claudacious.tilth.sim.soil;

import com.claudacious.tilth.sim.Noise;
import com.claudacious.tilth.sim.catena.SlopePosition;
import com.claudacious.tilth.sim.climate.BiomeClimate;

/**
 * The chemistry of undisturbed soil. Untilled soil blocks store nothing; when a block is first
 * tilled, its starting state is derived from its texture, horizon, hillslope position, climate and a
 * little deterministic noise.
 */
public final class SoilBaseline {
    private SoilBaseline() {
    }

    /**
     * @param noise deterministic variation in [-1, 1] for this block
     */
    public static SoilState create(SoilTexture tex, SoilHorizon horizon, SlopePosition pos, DrainageClass drainage,
                                   BiomeClimate climate, double noise) {
        SoilState s = new SoilState();
        double humid = climate.leachingIndex();

        // Organic matter: grasslands > forests > deserts; cool climates decompose humus slowly; hollows
        // accumulate it, eroded shoulders lose it; clay protects it.
        double coolFactor = Noise.clamp(1.3 - 0.03 * climate.meanTemp(), 0.5, 1.6);
        double om = (0.5 + 3.2 * humid * coolFactor) * pos.organicFactor() * (1.0 + 0.01 * (tex.clay() - 20));
        if (tex.sand() >= 80) {
            om *= 0.7;
        }
        if (drainage.isWet()) {
            om *= 1.3;
        }
        om *= 1.0 + 0.15 * noise;
        if (horizon != SoilHorizon.TOPSOIL) {
            om *= 0.3;
        }
        if (tex.isOrganic()) {
            om = 35 + 15 * (noise * 0.5 + 0.5);
        }
        s.organicMatter = Noise.clamp(om, 0.2, 80);

        // pH from weathering equilibrium; lower slopes collect leached bases.
        double ph = climate.equilibriumPh();
        if (pos.receivesSediment()) {
            ph += 0.2;
        } else if (pos == SlopePosition.SHOULDER || pos == SlopePosition.SUMMIT) {
            ph -= 0.1;
        }
        ph += 0.3 * noise;
        if (tex.isOrganic()) {
            ph = 4.2 + 1.0 * (noise * 0.5 + 0.5);
        }
        s.ph = Noise.clamp(ph, 4.0, 8.6);

        double om1 = Math.min(s.organicMatter, 8);
        s.nitrogen = Math.max(2, 4 + 3 * om1 + 3 * noise);
        s.phosphorus = Math.max(2, 8 + 2 * om1 + 6 * noise - (s.ph > 7.8 ? 4 : 0) - (s.ph < 5.3 ? 4 : 0));
        s.potassium = Math.max(30, 60 + 4.0 * tex.clay() + 20 * noise - 40 * humid * (tex.sand() / 100.0));
        s.sulfur = Math.max(2, 6 + 2 * om1 + 2 * noise);
        s.calcium = 300 + 25 * tex.clay() * Math.max(0.2, (s.ph - 4.0) / 4.0) + (s.ph > 7.5 ? 1500 : 0);
        s.magnesium = s.calcium / 6.5;
        if (horizon != SoilHorizon.TOPSOIL) {
            s.nitrogen *= 0.4;
            s.phosphorus *= 0.5;
            s.sulfur *= 0.6;
        }

        Hydraulics h = tex.hydraulics(s.organicMatter);
        double wet = drainage.isWet() ? 0.95 : 0.75;
        s.moisture = h.wiltingPoint() + (h.fieldCapacity() - h.wiltingPoint()) * wet;
        s.compaction = horizon == SoilHorizon.TOPSOIL ? 0.15 : 0.45;
        s.tilth = horizon == SoilHorizon.TOPSOIL ? Noise.clamp(0.4 + s.organicMatter / 10.0, 0.3, 0.8) : 0.25;
        s.waterTableDepth = drainage.waterTableDepth();
        s.salinity = climate.annualPrecip() < 250 ? 1.5 : 0.3;
        SoilChemistry.clamp(s);
        return s;
    }
}
