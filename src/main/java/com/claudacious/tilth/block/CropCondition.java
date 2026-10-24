package com.claudacious.tilth.block;

import com.claudacious.tilth.sim.crop.CropState;
import com.claudacious.tilth.sim.soil.Nutrient;
import net.minecraft.util.StringRepresentable;

/** What a crop visibly suffers from, shown by its texture. */
public enum CropCondition implements StringRepresentable {
    HEALTHY("healthy"),
    /** Yellowing leaves: short of nitrogen (or sulfur or magnesium). */
    CHLOROTIC("chlorotic"),
    /** Purple-tinged leaves: short of phosphorus. */
    PURPLED("purpled"),
    /** Dull, drooping leaves: short of water. */
    WILTED("wilted"),
    /** Browned, water-soaked leaf edges: frost damage. */
    FROSTED("frosted");

    private final String name;

    CropCondition(String name) {
        this.name = name;
    }

    @Override
    public String getSerializedName() {
        return name;
    }

    public static CropCondition of(CropState cs) {
        if (!cs.germinated) {
            return HEALTHY;
        }
        if (cs.frostDamage >= 0.2) {
            return FROSTED;
        }
        if (cs.waterFactor < 0.45 || cs.droughtDays >= 2) {
            return WILTED;
        }
        if (cs.sufficiency[Nutrient.N.ordinal()] < 0.6 || cs.sufficiency[Nutrient.S.ordinal()] < 0.5
                || cs.sufficiency[Nutrient.MG.ordinal()] < 0.5) {
            return CHLOROTIC;
        }
        if (cs.sufficiency[Nutrient.P.ordinal()] < 0.55) {
            return PURPLED;
        }
        return HEALTHY;
    }
}
