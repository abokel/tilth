package com.claudacious.tilth.block;

import com.claudacious.tilth.sim.crop.CropState;
import com.claudacious.tilth.sim.genetics.CropGenome;
import net.minecraft.nbt.CompoundTag;

/** NBT (de)serialisation of {@link CropState}. */
public final class CropNbt {
    private CropNbt() {
    }

    public static CompoundTag write(CropState c) {
        CompoundTag t = new CompoundTag();
        t.putBoolean("germinated", c.germinated);
        t.putDouble("germination_gdd", c.germinationGdd);
        t.putDouble("gdd", c.gdd);
        t.putDouble("dev", c.dev);
        t.putDouble("biomass", c.biomass);
        t.putDouble("yield_potential", c.yieldPotential);
        putArray(t, "sufficiency", c.sufficiency);
        putArray(t, "stress_sum", c.stressSum);
        putArray(t, "stress_weight", c.stressWeight);
        t.putDouble("event_factor", c.eventFactor);
        t.putDouble("water_factor", c.waterFactor);
        t.putDouble("quality_sum", c.qualitySum);
        t.putDouble("quality_base_sum", c.qualityBaseSum);
        t.putDouble("pollination_quality_sum", c.pollinationQualitySum);
        t.putDouble("quality_weight", c.qualityWeight);
        t.putDouble("pollination_today", c.pollinationToday);
        t.putDouble("pollination_factor", c.pollinationFactor);
        t.putDouble("pollination_sum", c.pollinationSum);
        t.putDouble("pollination_weight", c.pollinationWeight);
        t.putInt("drought_days", c.droughtDays);
        t.putInt("waterlog_days", c.waterlogDays);
        t.putInt("cold_wet_days", c.coldWetDays);
        t.putInt("days_since_sowing", c.daysSinceSowing);
        t.putDouble("days_since_mature", c.daysSinceMature);
        t.putDouble("frost_damage", c.frostDamage);
        t.putInt("harvests", c.harvests);
        t.putBoolean("dead", c.dead);
        t.putString("death_cause", c.deathCause);
        t.putDouble("fixed_n", c.fixedNitrogen);
        t.putDouble("growth_factor", c.lastGrowthFactor);
        t.putString("limiting", c.limitingFactor);
        t.putDouble("aff_growth", c.afflictionGrowthFactor);
        t.putDouble("aff_yield", c.afflictionYieldFactor);
        t.putDouble("aff_quality", c.afflictionQualityFactor);
        CompoundTag aff = new CompoundTag();
        c.afflictions.forEach(aff::putDouble);
        t.put("afflictions", aff);
        if (c.genome != null) {
            t.putString("genome", c.genome.encode());
        }
        return t;
    }

    public static CropState read(CompoundTag t) {
        CropState c = new CropState();
        c.germinated = t.getBoolean("germinated");
        c.germinationGdd = t.getDouble("germination_gdd");
        c.gdd = t.getDouble("gdd");
        c.dev = t.getDouble("dev");
        c.biomass = t.getDouble("biomass");
        c.yieldPotential = t.contains("yield_potential") ? t.getDouble("yield_potential") : 1.0;
        getArray(t, "sufficiency", c.sufficiency, 1.0);
        getArray(t, "stress_sum", c.stressSum, 0.0);
        getArray(t, "stress_weight", c.stressWeight, 0.0);
        c.eventFactor = t.contains("event_factor") ? t.getDouble("event_factor") : 1.0;
        c.waterFactor = t.getDouble("water_factor");
        c.qualitySum = t.getDouble("quality_sum");
        c.qualityWeight = t.getDouble("quality_weight");
        c.pollinationToday = t.contains("pollination_today") ? t.getDouble("pollination_today") : 1.0;
        c.pollinationFactor = t.contains("pollination_factor") ? t.getDouble("pollination_factor") : 1.0;
        c.pollinationSum = t.getDouble("pollination_sum");
        c.pollinationWeight = t.getDouble("pollination_weight");
        if (t.contains("quality_base_sum")) {
            c.qualityBaseSum = t.getDouble("quality_base_sum");
            c.pollinationQualitySum = t.contains("pollination_quality_sum") ? t.getDouble("pollination_quality_sum")
                    : c.qualityWeight;
            c.qualitySum = c.qualityBaseSum + 0.1 * c.pollinationQualitySum;
        } else {
            double legacyPollination = c.qualityWeight <= 0 ? 1.0 : Math.max(0.0, Math.min(1.0, c.pollinationFactor));
            c.pollinationQualitySum = legacyPollination * c.qualityWeight;
            c.qualityBaseSum = c.qualitySum - 0.1 * c.pollinationQualitySum;
        }
        c.droughtDays = t.getInt("drought_days");
        c.waterlogDays = t.getInt("waterlog_days");
        c.coldWetDays = t.getInt("cold_wet_days");
        c.daysSinceSowing = t.getInt("days_since_sowing");
        c.daysSinceMature = t.getDouble("days_since_mature");
        c.frostDamage = t.getDouble("frost_damage");
        c.harvests = t.getInt("harvests");
        c.dead = t.getBoolean("dead");
        c.deathCause = t.getString("death_cause");
        c.fixedNitrogen = t.getDouble("fixed_n");
        c.lastGrowthFactor = t.getDouble("growth_factor");
        c.limitingFactor = t.getString("limiting");
        c.afflictionGrowthFactor = t.contains("aff_growth") ? t.getDouble("aff_growth") : 1.0;
        c.afflictionYieldFactor = t.contains("aff_yield") ? t.getDouble("aff_yield") : 1.0;
        c.afflictionQualityFactor = t.contains("aff_quality") ? t.getDouble("aff_quality") : 1.0;
        CompoundTag aff = t.getCompound("afflictions");
        for (String key : aff.getAllKeys()) {
            c.afflictions.put(key, aff.getDouble(key));
        }
        if (t.contains("genome")) {
            c.genome = CropGenome.decode(t.getString("genome"));
        }
        return c;
    }

    private static void putArray(CompoundTag t, String key, double[] values) {
        CompoundTag a = new CompoundTag();
        for (int i = 0; i < values.length; i++) {
            a.putDouble(Integer.toString(i), values[i]);
        }
        t.put(key, a);
    }

    private static void getArray(CompoundTag t, String key, double[] out, double fallback) {
        CompoundTag a = t.getCompound(key);
        for (int i = 0; i < out.length; i++) {
            String k = Integer.toString(i);
            out[i] = a.contains(k) ? a.getDouble(k) : fallback;
        }
    }
}
