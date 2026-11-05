package com.claudacious.tilth.market;

import com.claudacious.tilth.postharvest.GrainStacks;
import com.claudacious.tilth.registry.TilthBlocks;
import com.claudacious.tilth.registry.TilthItems;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.Nullable;

/**
 * What the market buys, in groups whose prices move together. Each item has a base value in emeralds; the day's price
 * is that times the group's index (season and events) times the item's quality.
 */
public enum Commodity {
    GRAIN("Grain"),
    VEGETABLES("Vegetables"),
    MEAT("Meat"),
    DAIRY("Dairy"),
    EGGS("Eggs"),
    WOOL("Wool"),
    HAY("Hay"),
    FIBRE("Fibre & oil");

    public final String displayName;

    Commodity(String displayName) {
        this.displayName = displayName;
    }

    /** One item's commodity and base value (emeralds), or null if the market does not deal in it. */
    public record Quote(Commodity commodity, double base) {
    }

    @Nullable
    public static Quote quote(ItemStack stack) {
        Item item = stack.getItem();
        if (GrainStacks.isStorableGrain(stack)) {
            return new Quote(GRAIN, 0.04);
        }
        if (item == Items.POTATO || item == Items.CARROT || item == Items.BEETROOT || item == Items.PUMPKIN || item == Items.MELON_SLICE
                || isTilth(item, "cabbage", "onion", "garlic", "lettuce", "tomato", "sugar_beet", "radish", "chickpea", "lentil", "common_bean",
                "pea", "peanut")) {
            return new Quote(VEGETABLES, 0.05);
        }
        if (item == Items.BEEF || item == Items.PORKCHOP || item == Items.MUTTON || item == Items.CHICKEN || item == Items.RABBIT) {
            return new Quote(MEAT, 0.14);
        }
        if (item == Items.MILK_BUCKET) {
            return new Quote(DAIRY, 0.5);
        }
        if (item == TilthItems.BUTTER.get()) {
            return new Quote(DAIRY, 0.3);
        }
        if (item == TilthItems.CHEESE.get()) {
            return new Quote(DAIRY, 0.45);
        }
        if (item == Items.EGG) {
            return new Quote(EGGS, 0.06);
        }
        if (item == TilthItems.FLEECE.get()) {
            return new Quote(WOOL, 0.15);
        }
        if (item == TilthItems.YARN.get()) {
            return new Quote(WOOL, 0.12);
        }
        if (item == TilthItems.WOOLLEN_CLOTH.get()) {
            return new Quote(WOOL, 0.9);
        }
        if (item == TilthItems.DRIED_HAY.get()) {
            return new Quote(HAY, 0.03);
        }
        if (item == Items.HAY_BLOCK) {
            return new Quote(HAY, 0.3);
        }
        if (item == TilthBlocks.ALFALFA_BALE.get().asItem()) {
            return new Quote(HAY, 0.45);
        }
        if (isTilth(item, "cotton", "flax", "sunflower") || item == TilthItems.VEGETABLE_OIL.get()) {
            return new Quote(FIBRE, 0.07);
        }
        return null;
    }

    private static boolean isTilth(Item item, String... names) {
        String path = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(item).getPath();
        String ns = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(item).getNamespace();
        if (!"tilth".equals(ns)) {
            return false;
        }
        for (String n : names) {
            if (path.equals(n)) {
                return true;
            }
        }
        return false;
    }
}
