package com.claudacious.tilth.diet;

import com.claudacious.tilth.sim.diet.DietNutrient;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;

import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Nutrients in one serving of a food, as percent of the daily value, e.g.
 * {@code {"protein": 55, "fat": 35, "iron": 30}}. Assigned to items through the
 * {@code tilth:food_nutrition} data map, so packs can add or rebalance any food, modded or vanilla.
 */
public record NutrientProfile(Map<DietNutrient, Float> values) {
    public static final Codec<NutrientProfile> CODEC = Codec.unboundedMap(Codec.STRING, Codec.FLOAT).comapFlatMap(map -> {
        EnumMap<DietNutrient, Float> out = new EnumMap<>(DietNutrient.class);
        for (Map.Entry<String, Float> e : map.entrySet()) {
            DietNutrient n = DietNutrient.byId(e.getKey());
            if (n == null) {
                return DataResult.error(() -> "Unknown diet nutrient: " + e.getKey());
            }
            out.put(n, e.getValue());
        }
        return DataResult.success(new NutrientProfile(out));
    }, profile -> {
        Map<String, Float> out = new LinkedHashMap<>();
        profile.values().forEach((k, v) -> out.put(k.id(), v));
        return out;
    });

    public float get(DietNutrient n) {
        return values.getOrDefault(n, 0f);
    }
}
