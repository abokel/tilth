package com.claudacious.tilth.genetics;

import com.claudacious.tilth.registry.TilthItems;
import com.claudacious.tilth.registry.TilthMisc;
import com.claudacious.tilth.sim.crop.CropSpecies;
import com.claudacious.tilth.sim.crop.Crops;
import com.claudacious.tilth.sim.genetics.CropGenome;
import com.claudacious.tilth.sim.genetics.GeneticsModel;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapelessRecipe;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/** Shapeless recipe that recombines two seed genomes into one incubatable embryo. */
public class GeneSplicingRecipe extends ShapelessRecipe {
    public GeneSplicingRecipe(ShapelessRecipe base) {
        super(base.getGroup(), base.category(), base.getResultItem(RegistryAccess.EMPTY), base.getIngredients());
    }

    @Override
    public boolean matches(CraftingInput input, net.minecraft.world.level.Level level) {
        if (!super.matches(input, level)) {
            return false;
        }
        List<ItemStack> seeds = new ArrayList<>();
        for (ItemStack stack : input.items()) {
            if (!stack.isEmpty() && !stack.is(TilthItems.GENE_SPLICER.get())) {
                seeds.add(stack);
            }
        }
        return seeds.size() >= 2 && speciesOf(seeds.get(0)) != null && speciesOf(seeds.get(0)) == speciesOf(seeds.get(1));
    }

    @Override
    public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
        ItemStack result = super.assemble(input, registries);
        if (result.isEmpty()) {
            return result;
        }
        List<ItemStack> seeds = new ArrayList<>();
        for (ItemStack stack : input.items()) {
            if (!stack.isEmpty() && !stack.is(TilthItems.GENE_SPLICER.get())) {
                seeds.add(stack);
            }
        }
        if (seeds.size() < 2) {
            return ItemStack.EMPTY;
        }
        CropSpecies a = speciesOf(seeds.get(0));
        CropSpecies b = speciesOf(seeds.get(1));
        if (a == null || b == null || !a.id.equals(b.id)) {
            return ItemStack.EMPTY;
        }
        CropGenome ga = SeedGenetics.genomeOrLandrace(seeds.get(0), a);
        CropGenome gb = SeedGenetics.genomeOrLandrace(seeds.get(1), b);
        CropGenome child = GeneticsModel.inheritCrop(ga, gb, new Random(input.hashCode() * 31L + seeds.hashCode()), "splice");
        SeedGenetics.setGenome(result, child);
        return result;
    }

    private static CropSpecies speciesOf(ItemStack stack) {
        CropSpecies species = SeedGenetics.speciesOf(stack);
        if (species != null) {
            return species;
        }
        if (stack.is(Items.WHEAT_SEEDS)) {
            return Crops.COMMON_WHEAT;
        }
        if (stack.is(Items.POTATO)) {
            return Crops.POTATO;
        }
        if (stack.is(Items.CARROT)) {
            return Crops.CARROT;
        }
        if (stack.is(Items.BEETROOT_SEEDS)) {
            return Crops.BEETROOT;
        }
        return null;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return TilthMisc.GENE_SPLICING.get();
    }

    public static class Serializer implements RecipeSerializer<GeneSplicingRecipe> {
        private static final MapCodec<GeneSplicingRecipe> CODEC =
                RecipeSerializer.SHAPELESS_RECIPE.codec().xmap(GeneSplicingRecipe::new, r -> r);
        private static final StreamCodec<RegistryFriendlyByteBuf, GeneSplicingRecipe> STREAM_CODEC =
                RecipeSerializer.SHAPELESS_RECIPE.streamCodec().map(GeneSplicingRecipe::new, r -> r);

        @Override
        public MapCodec<GeneSplicingRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, GeneSplicingRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }
}
