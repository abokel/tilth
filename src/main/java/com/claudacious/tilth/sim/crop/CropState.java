package com.claudacious.tilth.sim.crop;

import com.claudacious.tilth.sim.SimSettings;
import com.claudacious.tilth.sim.genetics.CropGenome;
import com.claudacious.tilth.sim.soil.Nutrient;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

/** Growth state of one planted crop. */
public final class CropState {
    public boolean germinated;
    /** Thermal time accumulated toward emergence. */
    public double germinationGdd;
    /** Thermal time accumulated since emergence. */
    public double gdd;
    /** Development: 0 at emergence, 1 at maturity; keeps rising while the crop stands overripe. */
    public double dev;
    /** Accumulated biomass relative to an unstressed crop (0-1). */
    public double biomass;
    /**
     * Relative yield potential (0-1) from the FAO-33 stage-wise yield response: stress during
     * flowering and yield formation costs far more than stress during vegetative growth.
     */
    public double yieldPotential = 1.0;
    /** Development-weighted stress sums and weights for the vegetative, flowering, yield-formation and ripening windows. */
    public final double[] stressSum = new double[4];
    public final double[] stressWeight = new double[4];
    /** Multiplier from acute events: heat at anthesis, frost, standing overripe in the field. */
    public double eventFactor = 1.0;
    /** Smoothed nutrient sufficiency (supply/demand), indexed by {@link Nutrient#ordinal()}. */
    public final double[] sufficiency = new double[Nutrient.values().length];
    public double waterFactor = 1.0;
    /** Legacy aggregate quality; kept for save compatibility. */
    public double qualitySum;
    /** Cumulative non-pollination quality contribution, weighted by development. */
    public double qualityBaseSum;
    /** Cumulative pollination quality contribution, weighted by development. */
    public double pollinationQualitySum;
    public double qualityWeight;
    /** Pollination service sampled for the current simulated day (0-1). */
    public double pollinationToday = 1.0;
    /** Development-weighted pollination multiplier through flowering/fruit set. */
    public double pollinationFactor = 1.0;
    public double pollinationSum;
    public double pollinationWeight;
    public int droughtDays;
    public int waterlogDays;
    public int coldWetDays;
    public int daysSinceSowing;
    public double daysSinceMature;
    public double frostDamage;
    public int harvests;
    public boolean dead;
    public String deathCause = "";
    /** Nitrogen fixed by rhizobia so far (kg/ha). */
    public double fixedNitrogen;
    public double lastGrowthFactor = 1.0;
    public String limitingFactor = "none";
    /** 1 in full daylight; set by the world before each update from the block's sky and block light. */
    public double lightFactor = 1.0;

    /** Multipliers maintained by the affliction engine; all 1 when diseases and pests are disabled. */
    public double afflictionGrowthFactor = 1.0;
    public double afflictionYieldFactor = 1.0;
    public double afflictionQualityFactor = 1.0;
    /** Active afflictions: id to severity (0-1). */
    public final Map<String, Double> afflictions = new LinkedHashMap<>();
    /** Heritable cultivar traits. Null in old saves until first normalisation. */
    public CropGenome genome;

    public CropState() {
        Arrays.fill(sufficiency, 1.0);
    }

    public double minSufficiency() {
        double m = 1.0;
        for (double v : sufficiency) {
            m = Math.min(m, v);
        }
        return m;
    }

    public double qualityScore() {
        if (qualityWeight <= 0) {
            return 0.6;
        }
        double base = qualityBaseSum / qualityWeight;
        double pollination = SimSettings.pollinationEnabled && SimSettings.pollinationStrength > 0
                ? pollinationQualitySum / qualityWeight
                : 1.0;
        return base + 0.1 * pollination;
    }

    public void kill(String cause) {
        if (!dead) {
            dead = true;
            deathCause = cause;
        }
    }

    public CropGenome genome(CropSpecies species) {
        if (genome == null || !species.id.equals(genome.speciesId())) {
            genome = CropGenome.landrace(species.id);
        }
        return genome.normalized();
    }
}
