package com.claudacious.tilth.quality;

import com.claudacious.tilth.registry.TilthMisc;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapelessRecipe;

/**
 * A shapeless recipe whose result inherits the average quality of its graded ingredients. Same JSON
 * format as {@code minecraft:crafting_shapeless} with type {@code tilth:quality_shapeless}.
 */
public class QualityShapelessRecipe extends ShapelessRecipe {
    public QualityShapelessRecipe(ShapelessRecipe base) {
        super(base.getGroup(), base.category(), base.getResultItem(RegistryAccess.EMPTY), base.getIngredients());
    }

    @Override
    public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
        ItemStack result = super.assemble(input, registries);
        Quality.set(result, Quality.average(input.items()));
        return result;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return TilthMisc.QUALITY_SHAPELESS.get();
    }

    public static class Serializer implements RecipeSerializer<QualityShapelessRecipe> {
        private static final MapCodec<QualityShapelessRecipe> CODEC =
                RecipeSerializer.SHAPELESS_RECIPE.codec().xmap(QualityShapelessRecipe::new, r -> r);
        private static final StreamCodec<RegistryFriendlyByteBuf, QualityShapelessRecipe> STREAM_CODEC =
                RecipeSerializer.SHAPELESS_RECIPE.streamCodec().map(QualityShapelessRecipe::new, r -> r);

        @Override
        public MapCodec<QualityShapelessRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, QualityShapelessRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }
}
