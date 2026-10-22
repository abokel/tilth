package com.claudacious.tilth.diet;

import com.claudacious.tilth.sim.diet.DietNutrient;
import com.mojang.serialization.Codec;

import java.util.LinkedHashMap;
import java.util.Map;

/** Body nutrient stores of a player, each 0 (depleted) to 1 (full). Stored as a player attachment. */
public final class PlayerDiet {
    public static final double START_LEVEL = 0.75;
    public static final Codec<PlayerDiet> CODEC = Codec.unboundedMap(Codec.STRING, Codec.DOUBLE).xmap(PlayerDiet::fromMap, PlayerDiet::toMap);

    private final double[] levels = new double[DietNutrient.values().length];

    public PlayerDiet() {
        java.util.Arrays.fill(levels, START_LEVEL);
    }

    private static PlayerDiet fromMap(Map<String, Double> map) {
        PlayerDiet d = new PlayerDiet();
        map.forEach((k, v) -> {
            DietNutrient n = DietNutrient.byId(k);
            if (n != null) {
                d.levels[n.ordinal()] = clamp(v);
            }
        });
        return d;
    }

    private Map<String, Double> toMap() {
        Map<String, Double> m = new LinkedHashMap<>();
        for (DietNutrient n : DietNutrient.values()) {
            m.put(n.id(), levels[n.ordinal()]);
        }
        return m;
    }

    private static double clamp(double v) {
        return Math.max(0, Math.min(1, v));
    }

    public double get(DietNutrient n) {
        return levels[n.ordinal()];
    }

    public void set(DietNutrient n, double v) {
        levels[n.ordinal()] = clamp(v);
    }

    public void eat(NutrientProfile profile, double multiplier) {
        for (DietNutrient n : DietNutrient.values()) {
            levels[n.ordinal()] = clamp(levels[n.ordinal()] + n.gainFor(profile.get(n) * multiplier));
        }
    }

    public void decay(long ticks, double multiplier) {
        for (DietNutrient n : DietNutrient.values()) {
            levels[n.ordinal()] = clamp(levels[n.ordinal()] - n.decayPerTick() * ticks * multiplier);
        }
    }

    public double average() {
        double s = 0;
        for (double v : levels) {
            s += v;
        }
        return s / levels.length;
    }

    public double min() {
        double m = 1;
        for (double v : levels) {
            m = Math.min(m, v);
        }
        return m;
    }
}
