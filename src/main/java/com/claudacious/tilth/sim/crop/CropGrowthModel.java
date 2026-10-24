package com.claudacious.tilth.sim.crop;

import com.claudacious.tilth.sim.Noise;
import com.claudacious.tilth.sim.SimSettings;
import com.claudacious.tilth.sim.climate.ClimateMath;
import com.claudacious.tilth.sim.climate.DailyWeather;
import com.claudacious.tilth.sim.soil.Hydraulics;
import com.claudacious.tilth.sim.soil.Nutrient;
import com.claudacious.tilth.sim.soil.SoilState;
import com.claudacious.tilth.sim.soil.SoilWater;

import java.util.Random;

/**
 * Daily crop growth.
 *
 * <ul>
 *   <li>Phenology runs on thermal time (growing degree days) and is not slowed by nutrient stress,
 *       as in real crops.</li>
 *   <li>Growth each day is the temperature response times the most limiting of water, nutrients,
 *       pH and micronutrients (Liebig's law of the minimum), times compaction and disease.</li>
 *   <li>Stress during flowering and grain fill also erodes the harvest index, so a drought at
 *       anthesis costs far more yield than the same drought at tillering.</li>
 *   <li>Frost, prolonged drought, waterlogging and cold wet seedbeds can kill the crop.</li>
 * </ul>
 */
public final class CropGrowthModel {
    /** FAO-33 yield response factors Ky for the vegetative, flowering, yield-formation and ripening windows. */
    private static final double[] KY = {0.25, 0.6, 0.45, 0.1};
    /** Share of a crop's nutrient uptake drawn from the 20 cm plough layer; the rest comes from deeper roots. */
    private static final double TOPSOIL_SHARE = 0.55;

    private CropGrowthModel() {
    }

    static int window(double dev) {
        if (dev < 0.45) {
            return 0;
        }
        if (dev < 0.72) {
            return 1;
        }
        if (dev < 0.95) {
            return 2;
        }
        return 3;
    }

    static double computeYieldPotential(CropState cs) {
        double yp = 1.0;
        for (int i = 0; i < KY.length; i++) {
            if (cs.stressWeight[i] > 0) {
                yp *= 1.0 - KY[i] * (cs.stressSum[i] / cs.stressWeight[i]);
            }
        }
        return Noise.clamp(yp * cs.eventFactor, 0.0, 1.0);
    }

    /** Cumulative share of total nutrient uptake reached at a development fraction (logistic). */
    static double uptakeCurve(double dev) {
        double f0 = logistic(0);
        double f1 = logistic(1);
        return (logistic(Noise.clamp(dev, 0, 1)) - f0) / (f1 - f0);
    }

    private static double logistic(double x) {
        return 1.0 / (1.0 + Math.exp(-10.0 * (x - 0.42)));
    }

    public static void step(CropSpecies sp, CropState cs, SoilState s, Hydraulics h, DailyWeather w, SoilWater.Step water) {
        if (cs.dead) {
            return;
        }
        cs.daysSinceSowing++;
        double tMean = w.tMean();
        double rw = (s.moisture - h.wiltingPoint()) / Math.max(0.01, h.fieldCapacity() - h.wiltingPoint());
        double gddToday = ClimateMath.growingDegreeDays(w.tMin(), w.tMax(), sp.tBase, sp.tCutoff)
                * SimSettings.growthRateMultiplier;

        if (!cs.germinated) {
            germinate(sp, cs, s, h, w, rw, gddToday);
            return;
        }

        double prevDev = cs.dev;
        cs.gdd += gddToday;
        cs.dev = cs.gdd / sp.gddToMaturity;
        double dDev = Math.max(0, Math.min(cs.dev, 1.0) - Math.min(prevDev, 1.0));

        // ---- Nutrient uptake (only while developing toward maturity)
        double uptakeFrac = Math.max(0, uptakeCurve(cs.dev) - uptakeCurve(prevDev));
        double ppmPerKgHa = h.ppmPerKgHa();
        double rootAccess = Noise.clamp(0.25 + 0.75 * Math.min(1.0, cs.dev / 0.5), 0.25, 1.0) * (1.0 - 0.5 * s.compaction);
        double moistAccess = Noise.clamp(rw * 1.2, 0.1, 1.0);
        double yieldScale = Math.min(1.2, 0.4 + 0.6 * cs.yieldPotential);
        for (Nutrient n : Nutrient.values()) {
            double demand = sp.uptake(n) * TOPSOIL_SHARE * uptakeFrac * ppmPerKgHa * SimSettings.nutrientDemandMultiplier * yieldScale;
            double fixed = 0;
            if (n == Nutrient.N && sp.isLegume()) {
                double fixFactor = Noise.clamp((s.ph - 5.0) / 0.8, 0, 1)
                        * Noise.clamp(1.2 - s.nitrogen / 40.0, 0.2, 1.0)
                        * Math.max(0.3, water.ks());
                fixed = demand * sp.fixationShare * fixFactor;
                cs.fixedNitrogen += fixed / ppmPerKgHa;
            }
            double soilDemand = Math.max(0, demand - fixed);
            double available = s.get(n) * n.availability(s.ph);
            double maxUptake = available * 0.12 * rootAccess * moistAccess;
            double uptake = Math.min(soilDemand, maxUptake);
            s.add(n, -uptake);
            double suff = demand <= 1e-6 ? 1.0 : (uptake + fixed) / demand;
            int i = n.ordinal();
            if (demand > 1e-6) {
                cs.sufficiency[i] = 0.85 * cs.sufficiency[i] + 0.15 * Noise.clamp(suff, 0, 1);
            } else {
                cs.sufficiency[i] = Math.min(1.0, cs.sufficiency[i] + 0.02);
            }
        }

        // ---- Stress factors
        double fT = ClimateMath.temperatureResponse(tMean, sp.tBase, sp.tOpt, sp.tMax);
        double fW = water.ks();
        if (sp.needsFlooding && s.moisture < h.fieldCapacity()) {
            fW *= 0.75;
        }
        double fWL = 1.0;
        if (water.waterlogged() && !sp.needsFlooding) {
            fWL = 1.0 - 0.8 * (1.0 - sp.waterloggingTolerance);
        }
        cs.waterFactor = 0.8 * cs.waterFactor + 0.2 * (fW * fWL);
        double fN = cs.minSufficiency();
        double fPh = sp.phSuitability(s.ph);
        double fMicro = Math.min(1.0, Nutrient.micronutrientAvailability(s.ph) / 0.8);
        double fC = 1.0 - 0.35 * s.compaction;
        double fD = cs.afflictionGrowthFactor;

        double limiting = fW * fWL;
        String limitingName = water.waterlogged() && fWL < fW ? "waterlogging" : "water";
        if (fN < limiting) {
            limiting = fN;
            limitingName = "nutrients (" + weakestNutrient(cs).displayName().toLowerCase() + ")";
        }
        if (fPh < limiting) {
            limiting = fPh;
            limitingName = "soil pH";
        }
        if (fMicro < limiting) {
            limiting = fMicro;
            limitingName = "micronutrients (high pH)";
        }
        double fL = Noise.clamp(cs.lightFactor, 0, 1);
        if (fL < limiting) {
            limiting = fL;
            limitingName = "light";
        }
        if (fT < limiting) {
            limitingName = tMean < sp.tOpt ? "temperature (cool)" : "temperature (hot)";
        }
        double growth = fT * limiting * fC * fD;
        cs.lastGrowthFactor = growth;
        cs.limitingFactor = growth > 0.85 ? "none" : limitingName;

        if (dDev > 0) {
            cs.biomass += dDev * growth;
            double stress = 1.0 - Math.min(limiting, fD) * fC;
            int win = window(Math.min(cs.dev, 1.0));
            cs.stressSum[win] += stress * dDev;
            cs.stressWeight[win] += dDev;
        }

        // Heat at anthesis sterilises florets.
        if (cs.dev >= 0.55 && cs.dev <= 0.75 && w.tMax() > sp.heatStress) {
            cs.eventFactor *= Math.max(0.5, 1.0 - 0.04 * (w.tMax() - sp.heatStress));
        }

        frost(sp, cs, w);
        survival(sp, cs, water);

        if (cs.dev >= 1.0) {
            cs.daysSinceMature += 1;
            if (cs.daysSinceMature > 10) {
                // Shattering, lodging, sprouting in the head, rot in the ground.
                cs.eventFactor *= 0.995;
            }
            if (!sp.perennial && cs.daysSinceMature > SimSettings.overripeDaysBeforeDeath) {
                cs.kill("left standing long after maturity and rotted");
            }
        }

        double weight = Math.max(0.05, Math.min(cs.dev, 1.0) * Math.min(cs.dev, 1.0));
        double q = 0.4 * fN + 0.3 * Math.min(1.0, fW * fWL) + 0.2 * fPh + 0.1 * cs.afflictionQualityFactor;
        cs.qualitySum += q * weight;
        cs.qualityWeight += weight;
        cs.yieldPotential = computeYieldPotential(cs);
    }

    private static void germinate(CropSpecies sp, CropState cs, SoilState s, Hydraulics h, DailyWeather w, double rw, double gddToday) {
        if (w.tMean() > sp.tBase + 1 && rw > 0.35) {
            cs.germinationGdd += gddToday;
            if (cs.germinationGdd >= sp.germinationGdd) {
                cs.germinated = true;
                cs.biomass = 0.01;
            }
        }
        if (w.tMean() < sp.tBase + 2 && s.moisture > 0.8 * h.fieldCapacity()) {
            cs.coldWetDays++;
        } else {
            cs.coldWetDays = Math.max(0, cs.coldWetDays - 1);
        }
        if (cs.coldWetDays > 30) {
            cs.kill("seed rotted in cold, wet soil");
        } else if (cs.daysSinceSowing > 150) {
            cs.kill("seed never germinated (too cold or too dry)");
        } else if (SimSettings.frostKillsCrops && w.tMin() < sp.frostKillVegetative - 6 && cs.germinationGdd > sp.germinationGdd * 0.5) {
            cs.kill("frost killed the sprouting seed");
        }
    }

    private static void frost(CropSpecies sp, CropState cs, DailyWeather w) {
        boolean flowering = cs.dev >= 0.5 && cs.dev <= 0.8;
        double threshold = flowering ? sp.frostKillReproductive : sp.frostKillVegetative;
        if (cs.dev < 0.1) {
            threshold += 2;
        }
        if (w.tMin() >= threshold) {
            return;
        }
        double severity = threshold - w.tMin();
        if (severity > 2.0 && SimSettings.frostKillsCrops && cs.dev < 1.0) {
            cs.kill(String.format("killed by frost (%.1f C)", w.tMin()));
            return;
        }
        cs.frostDamage = Math.min(1.0, cs.frostDamage + 0.1 * severity);
        cs.eventFactor *= Math.max(0.3, 1.0 - 0.1 * severity);
        cs.biomass *= 0.95;
    }

    private static void survival(CropSpecies sp, CropState cs, SoilWater.Step water) {
        if (water.ks() < 0.08) {
            cs.droughtDays++;
        } else {
            cs.droughtDays = Math.max(0, cs.droughtDays - 2);
        }
        if (SimSettings.droughtKillsCrops && cs.droughtDays > 14 && cs.dev < 1.0) {
            cs.kill("wilted beyond recovery in drought");
        }
        if (water.waterlogged() && !sp.needsFlooding) {
            cs.waterlogDays++;
        } else {
            cs.waterlogDays = Math.max(0, cs.waterlogDays - 1);
        }
        if (SimSettings.waterloggingKillsCrops && cs.waterlogDays > 10 + sp.waterloggingTolerance * 20 && cs.dev < 1.0) {
            cs.kill("roots suffocated in waterlogged soil");
        }
    }

    public static Nutrient weakestNutrient(CropState cs) {
        Nutrient worst = Nutrient.N;
        double v = Double.MAX_VALUE;
        for (Nutrient n : Nutrient.values()) {
            if (cs.sufficiency[n.ordinal()] < v) {
                v = cs.sufficiency[n.ordinal()];
                worst = n;
            }
        }
        return worst;
    }

    /** Relative yield (1 = reference yield) if harvested now. */
    public static double yieldFactor(CropState cs) {
        return cs.yieldPotential * cs.afflictionYieldFactor * (1.0 - 0.3 * cs.frostDamage) * SimSettings.yieldMultiplier;
    }

    public static int grade(CropState cs) {
        double q = cs.qualityScore() * (1.0 - 0.3 * cs.frostDamage) * (cs.daysSinceMature > 10 ? 0.9 : 1.0);
        if (q >= 0.92) {
            return 5;
        }
        if (q >= 0.82) {
            return 4;
        }
        if (q >= 0.65) {
            return 3;
        }
        if (q >= 0.45) {
            return 2;
        }
        return 1;
    }

    public static HarvestResult harvest(CropSpecies sp, CropState cs, Random random) {
        double yf = Math.max(0, yieldFactor(cs));
        double kgPerM2 = sp.referenceYield / 10.0 * yf;
        double expected = sp.baseProduce * yf * (0.8 + 0.4 * random.nextDouble());
        int produce = (int) Math.floor(expected);
        if (random.nextDouble() < expected - produce) {
            produce++;
        }
        int seeds = 0;
        if (!sp.seedIsProduce()) {
            double seedExpected = sp.baseSeeds * (0.6 + 0.4 * Math.min(1.0, yf)) + random.nextDouble() * 0.8;
            seeds = Math.max(cs.germinated ? 1 : 0, (int) Math.floor(seedExpected));
        } else if (produce == 0 && yf > 0.05) {
            produce = 1;
        }
        double residue = sp.residueKgM2 * (0.4 + 0.6 * Math.min(1.0, cs.biomass));
        return new HarvestResult(produce, seeds, Math.min(sp.maxGrade, grade(cs)), kgPerM2, sp.yieldUnit.perAcre(kgPerM2, sp.unitWeightLb), residue,
                sp.residueNPercent);
    }

    /** Resets a perennial (or multi-pick) crop to its regrowth stage after harvest. */
    public static void regrow(CropSpecies sp, CropState cs) {
        cs.harvests++;
        double startDev = sp.stageStart[Math.max(1, Math.min(CropSpecies.STAGES - 1, sp.regrowStage))];
        cs.dev = startDev;
        cs.gdd = startDev * sp.gddToMaturity;
        cs.biomass = Math.max(0.1, startDev * 0.8);
        java.util.Arrays.fill(cs.stressSum, 0);
        java.util.Arrays.fill(cs.stressWeight, 0);
        cs.eventFactor = 1.0;
        cs.yieldPotential = 1.0;
        cs.daysSinceMature = 0;
        cs.qualitySum = 0;
        cs.qualityWeight = 0;
    }
}
