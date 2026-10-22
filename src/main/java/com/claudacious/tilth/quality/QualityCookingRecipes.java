package com.claudacious.tilth.quality;

import com.claudacious.tilth.registry.TilthMisc;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CampfireCookingRecipe;
import net.minecraft.world.item.crafting.CookingBookCategory;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.item.crafting.SmeltingRecipe;
import net.minecraft.world.item.crafting.SmokingRecipe;

/** Furnace, smoker and campfire recipes that keep the ingredient's quality grade. */
public final class QualityCookingRecipes {
    private QualityCookingRecipes() {
    }

    private static ItemStack keepQuality(ItemStack result, SingleRecipeInput input) {
        Quality.set(result, Quality.get(input.item()));
        return result;
    }

    public static class Smelting extends SmeltingRecipe {
        public Smelting(String group, CookingBookCategory category, Ingredient ingredient, ItemStack result, float experience, int cookingTime) {
            super(group, category, ingredient, result, experience, cookingTime);
        }

        @Override
        public ItemStack assemble(SingleRecipeInput input, HolderLookup.Provider registries) {
            return keepQuality(super.assemble(input, registries), input);
        }

        @Override
        public RecipeSerializer<?> getSerializer() {
            return TilthMisc.QUALITY_SMELTING.get();
        }
    }

    public static class Smoking extends SmokingRecipe {
        public Smoking(String group, CookingBookCategory category, Ingredient ingredient, ItemStack result, float experience, int cookingTime) {
            super(group, category, ingredient, result, experience, cookingTime);
        }

        @Override
        public ItemStack assemble(SingleRecipeInput input, HolderLookup.Provider registries) {
            return keepQuality(super.assemble(input, registries), input);
        }

        @Override
        public RecipeSerializer<?> getSerializer() {
            return TilthMisc.QUALITY_SMOKING.get();
        }
    }

    public static class Campfire extends CampfireCookingRecipe {
        public Campfire(String group, CookingBookCategory category, Ingredient ingredient, ItemStack result, float experience, int cookingTime) {
            super(group, category, ingredient, result, experience, cookingTime);
        }

        @Override
        public ItemStack assemble(SingleRecipeInput input, HolderLookup.Provider registries) {
            return keepQuality(super.assemble(input, registries), input);
        }

        @Override
        public RecipeSerializer<?> getSerializer() {
            return TilthMisc.QUALITY_CAMPFIRE.get();
        }
    }
}
