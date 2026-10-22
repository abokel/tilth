package com.claudacious.tilth.sim.crop;

import com.claudacious.tilth.sim.Noise;
import com.claudacious.tilth.sim.soil.Nutrient;

/** Agronomic parameters of a crop species. Built with {@link #builder}. */
public final class CropSpecies {
    public static final int STAGES = 10;

    public final String id;
    public final String name;
    public final String scientificName;
    public final CropFamily family;
    public final GrowthForm form;

    /** Cardinal temperatures (degrees C) for development and growth. */
    public final double tBase, tOpt, tMax, tCutoff;
    /** Thermal time from sowing to emergence and from emergence to maturity (degree-days above tBase). */
    public final double germinationGdd, gddToMaturity;
    public final double[] stageStart;
    public final int[] bbch;

    public final double frostKillVegetative, frostKillReproductive, heatStress;
    public final double phLow, phHigh, phMinTolerated, phMaxTolerated;
    /** FAO-56 crop coefficients, maximum rooting depth (m), depletion fraction p, waterlogging tolerance (0-1). */
    public final double kcIni, kcMid, kcEnd, rootDepthMax, depletionFraction, waterloggingTolerance;
    /** Total nutrient uptake at the reference yield (kg/ha), indexed by {@link Nutrient#ordinal()}. */
    private final double[] uptake;
    /** Share of nitrogen demand that rhizobia can supply (legumes). */
    public final double fixationShare;

    /** Reference yield of the harvested product (t/ha) for unstressed growth. */
    public final double referenceYield;
    public final YieldUnit yieldUnit;
    public final double unitWeightLb;

    public final String produceItem;
    public final String seedItem;
    public final int baseProduce;
    public final int baseSeeds;
    public final int harvestStage;
    public final boolean perennial;
    public final int regrowStage;
    public final int maxHarvests;
    public final int tallFromStage;
    public final double residueKgM2;
    public final double residueNPercent;
    public final boolean needsFlooding;

    private CropSpecies(Builder b) {
        this.id = b.id;
        this.name = b.name;
        this.scientificName = b.scientificName;
        this.family = b.family;
        this.form = b.form;
        this.tBase = b.tBase;
        this.tOpt = b.tOpt;
        this.tMax = b.tMax;
        this.tCutoff = b.tCutoff;
        this.germinationGdd = b.germinationGdd;
        this.gddToMaturity = b.gddToMaturity;
        this.stageStart = b.form.stageStart();
        this.bbch = b.form.bbch();
        this.frostKillVegetative = b.frostVeg;
        this.frostKillReproductive = b.frostRepro;
        this.heatStress = b.heatStress;
        this.phLow = b.phLow;
        this.phHigh = b.phHigh;
        this.phMinTolerated = b.phMinTol;
        this.phMaxTolerated = b.phMaxTol;
        this.kcIni = b.kcIni;
        this.kcMid = b.kcMid;
        this.kcEnd = b.kcEnd;
        this.rootDepthMax = b.rootDepth;
        this.depletionFraction = b.depletion;
        this.waterloggingTolerance = b.waterlogTolerance;
        this.uptake = b.uptake.clone();
        this.fixationShare = b.fixation;
        this.referenceYield = b.referenceYield;
        this.yieldUnit = b.yieldUnit;
        this.unitWeightLb = b.unitWeightLb;
        this.produceItem = b.produceItem;
        this.seedItem = b.seedItem;
        this.baseProduce = b.baseProduce;
        this.baseSeeds = b.baseSeeds;
        this.harvestStage = b.harvestStage;
        this.perennial = b.perennial;
        this.regrowStage = b.regrowStage;
        this.maxHarvests = b.maxHarvests;
        this.tallFromStage = b.tallFromStage;
        this.residueKgM2 = b.residueKg;
        this.residueNPercent = b.residueN;
        this.needsFlooding = b.flooded;
    }

    public static Builder builder(String id, String name, String scientificName, CropFamily family, GrowthForm form) {
        return new Builder(id, name, scientificName, family, form);
    }

    public double uptake(Nutrient n) {
        return uptake[n.ordinal()];
    }

    public boolean isLegume() {
        return fixationShare > 0;
    }

    public boolean seedIsProduce() {
        return seedItem.equals(produceItem);
    }

    public boolean isTall(int stage) {
        return tallFromStage >= 0 && stage >= tallFromStage;
    }

    /** FAO-56 crop coefficient curve over development. */
    public double kc(double dev) {
        if (dev < 0.15) {
            return kcIni;
        }
        if (dev < 0.40) {
            return Noise.lerp(kcIni, kcMid, (dev - 0.15) / 0.25);
        }
        if (dev < 0.75) {
            return kcMid;
        }
        if (dev < 1.0) {
            return Noise.lerp(kcMid, kcEnd, (dev - 0.75) / 0.25);
        }
        return kcEnd;
    }

    public double rootDepth(double dev) {
        return 0.1 + (rootDepthMax - 0.1) * Math.min(1.0, Math.max(0, dev) / 0.6);
    }

    /** 1 inside the optimum pH band, falling to 0.2 at the tolerance limits and 0.1 beyond. */
    public double phSuitability(double ph) {
        if (ph >= phLow && ph <= phHigh) {
            return 1.0;
        }
        if (ph < phLow) {
            if (ph <= phMinTolerated) {
                return 0.1;
            }
            return 0.2 + 0.8 * (ph - phMinTolerated) / (phLow - phMinTolerated);
        }
        if (ph >= phMaxTolerated) {
            return 0.1;
        }
        return 0.2 + 0.8 * (phMaxTolerated - ph) / (phMaxTolerated - phHigh);
    }

    /** Visual block stage 0-9 for the crop's state. */
    public int visualStage(CropState cs) {
        if (!cs.germinated) {
            return 0;
        }
        int stage = 1;
        for (int i = 2; i < STAGES; i++) {
            if (cs.dev >= stageStart[i]) {
                stage = i;
            }
        }
        return stage;
    }

    public boolean isHarvestable(CropState cs) {
        return !cs.dead && cs.germinated && visualStage(cs) >= harvestStage;
    }

    public int bbchFor(int stage) {
        return bbch[Math.max(0, Math.min(STAGES - 1, stage))];
    }

    public static final class Builder {
        private final String id;
        private final String name;
        private final String scientificName;
        private final CropFamily family;
        private final GrowthForm form;
        private double tBase, tOpt = 25, tMax = 35, tCutoff = 30;
        private double germinationGdd = 80, gddToMaturity = 1200;
        private double frostVeg = -2, frostRepro = -1, heatStress = 32;
        private double phLow = 6.0, phHigh = 7.0, phMinTol = 5.0, phMaxTol = 8.0;
        private double kcIni = 0.4, kcMid = 1.1, kcEnd = 0.5, rootDepth = 1.0, depletion = 0.5, waterlogTolerance = 0.2;
        private final double[] uptake = new double[Nutrient.values().length];
        private double fixation;
        private double referenceYield = 3.0;
        private YieldUnit yieldUnit = YieldUnit.BUSHEL;
        private double unitWeightLb = 60;
        private String produceItem = "minecraft:air";
        private String seedItem = "minecraft:air";
        private int baseProduce = 2, baseSeeds = 1;
        private int harvestStage = 9;
        private boolean perennial;
        private int regrowStage = 3;
        private int maxHarvests = 1;
        private int tallFromStage = -1;
        private double residueKg = 0.2, residueN = 1.0;
        private boolean flooded;

        private Builder(String id, String name, String scientificName, CropFamily family, GrowthForm form) {
            this.id = id;
            this.name = name;
            this.scientificName = scientificName;
            this.family = family;
            this.form = form;
        }

        public Builder temps(double base, double opt, double max, double cutoff) {
            this.tBase = base;
            this.tOpt = opt;
            this.tMax = max;
            this.tCutoff = cutoff;
            return this;
        }

        public Builder gdd(double germination, double maturity) {
            this.germinationGdd = germination;
            this.gddToMaturity = maturity;
            return this;
        }

        public Builder frost(double vegetative, double reproductive) {
            this.frostVeg = vegetative;
            this.frostRepro = reproductive;
            return this;
        }

        public Builder heat(double tMaxAtFlowering) {
            this.heatStress = tMaxAtFlowering;
            return this;
        }

        public Builder ph(double low, double high, double minTolerated, double maxTolerated) {
            this.phLow = low;
            this.phHigh = high;
            this.phMinTol = minTolerated;
            this.phMaxTol = maxTolerated;
            return this;
        }

        public Builder water(double kcIni, double kcMid, double kcEnd, double rootDepth, double depletion, double waterlogTolerance) {
            this.kcIni = kcIni;
            this.kcMid = kcMid;
            this.kcEnd = kcEnd;
            this.rootDepth = rootDepth;
            this.depletion = depletion;
            this.waterlogTolerance = waterlogTolerance;
            return this;
        }

        public Builder uptake(double n, double p, double k, double s, double ca, double mg) {
            uptake[Nutrient.N.ordinal()] = n;
            uptake[Nutrient.P.ordinal()] = p;
            uptake[Nutrient.K.ordinal()] = k;
            uptake[Nutrient.S.ordinal()] = s;
            uptake[Nutrient.CA.ordinal()] = ca;
            uptake[Nutrient.MG.ordinal()] = mg;
            return this;
        }

        public Builder fixation(double share) {
            this.fixation = share;
            return this;
        }

        public Builder yield(double tonnesPerHa, YieldUnit unit, double unitWeightLb) {
            this.referenceYield = tonnesPerHa;
            this.yieldUnit = unit;
            this.unitWeightLb = unitWeightLb;
            return this;
        }

        public Builder items(String produce, int produceCount, String seeds, int seedCount) {
            this.produceItem = produce;
            this.baseProduce = produceCount;
            this.seedItem = seeds;
            this.baseSeeds = seedCount;
            return this;
        }

        public Builder residue(double kgPerM2, double nitrogenPercent) {
            this.residueKg = kgPerM2;
            this.residueN = nitrogenPercent;
            return this;
        }

        public Builder perennial(int regrowStage, int maxHarvests) {
            this.perennial = true;
            this.regrowStage = regrowStage;
            this.maxHarvests = maxHarvests;
            return this;
        }

        public Builder harvestAt(int stage) {
            this.harvestStage = stage;
            return this;
        }

        public Builder tall(int fromStage) {
            this.tallFromStage = fromStage;
            return this;
        }

        public Builder flooded() {
            this.flooded = true;
            return this;
        }

        public CropSpecies build() {
            return new CropSpecies(this);
        }
    }
}
