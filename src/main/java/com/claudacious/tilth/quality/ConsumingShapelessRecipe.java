package com.claudacious.tilth.quality;

import com.claudacious.tilth.registry.TilthMisc;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapelessRecipe;

/**
 * A shapeless recipe that uses its ingredients up completely, containers included: water bucket and manure
 * make a bucket of slurry, not slurry plus a spare bucket. Same JSON as {@code minecraft:crafting_shapeless}
 * with type {@code tilth:consuming_shapeless}.
 */
public class ConsumingShapelessRecipe extends ShapelessRecipe {
    public ConsumingShapelessRecipe(ShapelessRecipe base) {
        super(base.getGroup(), base.category(), base.getResultItem(RegistryAccess.EMPTY), base.getIngredients());
    }

    @Override
    public NonNullList<ItemStack> getRemainingItems(CraftingInput input) {
        return NonNullList.withSize(input.size(), ItemStack.EMPTY);
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return TilthMisc.CONSUMING_SHAPELESS.get();
    }

    public static class Serializer implements RecipeSerializer<ConsumingShapelessRecipe> {
        private static final MapCodec<ConsumingShapelessRecipe> CODEC =
                RecipeSerializer.SHAPELESS_RECIPE.codec().xmap(ConsumingShapelessRecipe::new, r -> r);
        private static final StreamCodec<RegistryFriendlyByteBuf, ConsumingShapelessRecipe> STREAM_CODEC =
                RecipeSerializer.SHAPELESS_RECIPE.streamCodec().map(ConsumingShapelessRecipe::new, r -> r);

        @Override
        public MapCodec<ConsumingShapelessRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, ConsumingShapelessRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }
}
