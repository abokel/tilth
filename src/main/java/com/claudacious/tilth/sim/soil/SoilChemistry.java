package com.claudacious.tilth.sim.soil;

import com.claudacious.tilth.sim.Noise;
import com.claudacious.tilth.sim.SimSettings;
import com.claudacious.tilth.sim.climate.BiomeClimate;
import com.claudacious.tilth.sim.climate.DailyWeather;

/**
 * Daily soil nutrient processes: organic matter mineralisation, residue decomposition with N
 * immobilisation, denitrification, leaching, phosphorus fixation, potassium buffering by clay,
 * micronutrient weathering/recovery, acidification and slow pH drift toward the climate's weathering
 * equilibrium.
 */
public final class SoilChemistry {
    private SoilChemistry() {
    }

    public static void step(SoilState s, SoilTexture tex, Hydraulics h, DailyWeather w, double drainageMm,
                            boolean waterlogged, BiomeClimate climate) {
        double tSoil = w.tMean();
        double fT = tSoil <= 0 ? 0 : Noise.clamp(Math.pow(2.0, (tSoil - 25.0) / 10.0), 0, 1.4);
        double fc = h.fieldCapacity();
        double wp = h.wiltingPoint();
        double rw = (s.moisture - wp) / Math.max(0.01, fc - wp);
        double fW;
        if (rw <= 0) {
            fW = 0;
        } else if (rw <= 1) {
            fW = rw;
        } else {
            fW = Math.max(0.3, 1.0 - 0.7 * (s.moisture - fc) / Math.max(0.01, h.saturation() - fc));
        }
        double fTill = s.tillageFlushDays > 0 ? 1.6 : 1.0;
        s.tillageFlushDays = Math.max(0, s.tillageFlushDays - 1);
        double bd = h.bulkDensity();

        // Humus mineralisation: ~3.5% of organic N per year at 25 C in moist soil, which works out to the
        // familiar ~1.5-2% per year under field temperatures. Peat decomposes slowly because it is
        // usually waterlogged and acidic.
        double organicN = s.organicMatter * 500.0;
        double rate = 0.035 / 365.0 * (tex.isOrganic() ? 0.15 : 1.0) * (waterlogged ? 0.2 : 1.0);
        double nMin = organicN * rate * fT * fW * fTill * SimSettings.mineralizationMultiplier;
        s.nitrogen += nMin;
        s.phosphorus += nMin / 9.0;
        s.sulfur += nMin / 8.0;
        s.iron += nMin * 0.02;
        s.zinc += nMin * 0.0008;
        s.manganese += nMin * 0.006;
        s.copper += nMin * 0.0005;
        s.boron += nMin * 0.0004;
        s.molybdenum += nMin * 0.00003;
        s.organicMatter = Math.max(0.1, s.organicMatter - nMin / 500.0);

        // Residue decomposition. Straw (C:N ~80) ties up soil N; legume residue releases it.
        if (s.residue > 1e-4) {
            double k = (0.005 + 0.007 * s.incorporatedFraction) * fT * Math.max(0.2, fW);
            double decomposed = s.residue * k;
            s.residue -= decomposed;
            double netNGrams = decomposed * 1000.0 * (s.residueNitrogen / 100.0 - 0.015);
            double netNPpm = netNGrams * h.ppmPerGramM2();
            s.nitrogen = Math.max(0, s.nitrogen + netNPpm);
            s.potassium += decomposed * 1000.0 * 0.012 * h.ppmPerGramM2();
            s.phosphorus += decomposed * 1000.0 * 0.001 * h.ppmPerGramM2();
            s.iron += decomposed * 1000.0 * 0.00003 * h.ppmPerGramM2();
            s.zinc += decomposed * 1000.0 * 0.0000008 * h.ppmPerGramM2();
            s.manganese += decomposed * 1000.0 * 0.00001 * h.ppmPerGramM2();
            s.copper += decomposed * 1000.0 * 0.0000006 * h.ppmPerGramM2();
            s.boron += decomposed * 1000.0 * 0.0000007 * h.ppmPerGramM2();
            s.molybdenum += decomposed * 1000.0 * 0.00000003 * h.ppmPerGramM2();
            s.organicMatter += decomposed * 0.075 / bd;
            if (s.residue < 1e-4) {
                s.residue = 0;
                s.incorporatedFraction = 0;
            }
        }

        // Denitrification in anoxic soil.
        if (waterlogged) {
            s.nitrogen -= s.nitrogen * 0.04 * Math.max(0.2, fT);
        }

        // Leaching with drainage water. Nitrate and sulfate move freely; cations are held by the CEC.
        if (drainageMm > 0) {
            double stored = Math.max(1.0, s.moisture * SoilWater.layerMm());
            double fraction = drainageMm / (drainageMm + stored) * SimSettings.leachingMultiplier;
            double sandFrac = tex.sand() / 100.0;
            s.nitrogen -= s.nitrogen * fraction * 0.8;
            s.sulfur -= s.sulfur * fraction * 0.5;
            s.potassium -= s.potassium * fraction * (0.3 * sandFrac + 0.03);
            s.calcium -= s.calcium * fraction * 0.05;
            s.magnesium -= s.magnesium * fraction * 0.08;
            s.iron -= s.iron * fraction * 0.06;
            s.zinc -= s.zinc * fraction * 0.09;
            s.manganese -= s.manganese * fraction * 0.11;
            s.copper -= s.copper * fraction * 0.05;
            s.boron -= s.boron * fraction * 0.2;
            s.molybdenum -= s.molybdenum * fraction * 0.18;
            double buffer = Noise.clamp(10.0 / tex.cec(s.organicMatter), 0.3, 2.0);
            s.ph -= drainageMm * 0.00004 * buffer * SimSettings.phDriftMultiplier;
        }

        // pH drifts toward the climate equilibrium over years (liming has to be repeated).
        double eq = tex.isOrganic() ? 4.5 : climate.equilibriumPh();
        s.ph += (eq - s.ph) * 0.0003 * SimSettings.phDriftMultiplier;

        // Phosphate fixation by Fe/Al oxides (acid) or calcium (alkaline).
        double fixRate = 0.0015 * ((s.ph < 5.5 || s.ph > 7.5) ? 2.0 : 1.0);
        s.phosphorus -= Math.max(0, s.phosphorus - 4.0) * fixRate;

        // Potassium release from or fixation into clay interlayers.
        double kEq = 40 + 3.0 * tex.clay();
        s.potassium += (kEq - s.potassium) * 0.0008;

        // Calcium and magnesium buffered by mineral weathering, more of it in alkaline soils.
        double caEq = 300 + 400 * Math.max(0, s.ph - 5.0) + (s.ph > 7.6 ? 2000 : 0);
        s.calcium += (caEq - s.calcium) * 0.0005;
        s.magnesium += (caEq / 7.0 - s.magnesium) * 0.0005;
        double feEq = Math.max(3.0, 12 + 0.35 * tex.clay() + 2.0 * s.organicMatter - Math.max(0, s.ph - 6.8) * 7.0);
        double znEq = Math.max(0.15, 0.65 + 0.02 * tex.clay() + 0.07 * s.organicMatter - Math.max(0, s.ph - 7.0) * 0.35);
        double mnEq = Math.max(0.8, 4.5 + 0.1 * tex.clay() + 0.5 * s.organicMatter - Math.max(0, s.ph - 6.8) * 3.0);
        double cuEq = Math.max(0.1, 0.4 + 0.01 * tex.clay() + 0.03 * s.organicMatter - Math.max(0, s.ph - 7.1) * 0.07);
        double bEq = Math.max(0.08, 0.28 + 0.02 * s.organicMatter - Math.max(0, s.ph - 7.2) * 0.05);
        double moEq = Math.max(0.01, 0.025 + Math.max(0, s.ph - 5.5) * 0.02 + 0.003 * s.organicMatter);
        s.iron += (feEq - s.iron) * 0.0007;
        s.zinc += (znEq - s.zinc) * 0.0007;
        s.manganese += (mnEq - s.manganese) * 0.0007;
        s.copper += (cuEq - s.copper) * 0.0007;
        s.boron += (bEq - s.boron) * 0.0008;
        s.molybdenum += (moEq - s.molybdenum) * 0.0008;

        // Freeze-thaw and root activity slowly loosen compaction; humus builds structure.
        double relax = (w.tMin() < 0 && w.tMax() > 0) ? 0.0018 : 0.0006;
        s.compaction = Math.max(0.05, s.compaction - relax);
        s.tilth += 0.0005 * (Math.min(1.0, s.organicMatter / 4.0) - s.tilth);

        clamp(s);
    }

    public static void clamp(SoilState s) {
        s.nitrogen = Noise.clamp(s.nitrogen, 0, 400);
        s.phosphorus = Noise.clamp(s.phosphorus, 0, 300);
        s.potassium = Noise.clamp(s.potassium, 0, 1500);
        s.sulfur = Noise.clamp(s.sulfur, 0, 300);
        s.calcium = Noise.clamp(s.calcium, 0, 10000);
        s.magnesium = Noise.clamp(s.magnesium, 0, 2000);
        s.iron = Noise.clamp(s.iron, 0, 150);
        s.zinc = Noise.clamp(s.zinc, 0, 15);
        s.manganese = Noise.clamp(s.manganese, 0, 120);
        s.copper = Noise.clamp(s.copper, 0, 12);
        s.boron = Noise.clamp(s.boron, 0, 8);
        s.molybdenum = Noise.clamp(s.molybdenum, 0, 1.5);
        s.ph = Noise.clamp(s.ph, 3.5, 9.5);
        s.organicMatter = Noise.clamp(s.organicMatter, 0.1, 80);
        s.compaction = Noise.clamp(s.compaction, 0, 1);
        s.tilth = Noise.clamp(s.tilth, 0, 1);
        s.residue = Noise.clamp(s.residue, 0, 5);
    }
}
