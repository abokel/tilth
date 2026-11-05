package com.claudacious.tilth.postharvest;

import com.claudacious.tilth.Tilth;
import com.claudacious.tilth.registry.TilthDataComponents;
import com.claudacious.tilth.sim.postharvest.GrainDryingModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public final class GrainStacks {
    public static final TagKey<Item> STORABLE_GRAINS = TagKey.create(Registries.ITEM, Tilth.id("storable_grains"));

    private GrainStacks() {
    }

    public static boolean isStorableGrain(ItemStack stack) {
        return stack.is(STORABLE_GRAINS);
    }

    public static float moisture(ItemStack stack) {
        Float stored = stack.get(TilthDataComponents.GRAIN_MOISTURE.get());
        if (stored == null || !Float.isFinite(stored)) {
            return (float) GrainDryingModel.DEFAULT_HARVEST_MOISTURE;
        }
        return clamp(stored, (float) GrainDryingModel.MIN_MOISTURE, (float) GrainDryingModel.MAX_MOISTURE);
    }

    public static void setMoisture(ItemStack stack, float moisture) {
        // Kept to the nearest half percentage point, so grain from the same store stacks instead of differing by a hair.
        float rounded = Math.round(moisture * 200f) / 200f;
        stack.set(TilthDataComponents.GRAIN_MOISTURE.get(),
                clamp(rounded, (float) GrainDryingModel.MIN_MOISTURE, (float) GrainDryingModel.MAX_MOISTURE));
    }

    public static ItemStack profileOf(ItemStack stack) {
        ItemStack out = stack.copyWithCount(1);
        out.remove(TilthDataComponents.GRAIN_MOISTURE.get());
        return out;
    }

    public static boolean sameProfile(ItemStack a, ItemStack b) {
        return ItemStack.isSameItemSameComponents(profileOf(a), profileOf(b));
    }

    public static float harvestMoisture(Level level, BlockPos pos) {
        float rain = level.isRainingAt(pos.above()) ? 1.0f : level.getRainLevel(1.0f);
        float downfall = level.getBiome(pos).value().getModifiedClimateSettings().downfall();
        float humidity = clamp(0.25f + 0.45f * downfall + 0.3f * rain, 0.0f, 1.0f);
        return clamp(0.14f + 0.10f * humidity, (float) GrainDryingModel.MIN_MOISTURE, (float) GrainDryingModel.MAX_MOISTURE);
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }
}
