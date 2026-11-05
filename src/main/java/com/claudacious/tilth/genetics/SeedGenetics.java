package com.claudacious.tilth.genetics;

import com.claudacious.tilth.block.TilthCropBlock;
import com.claudacious.tilth.registry.TilthDataComponents;
import com.claudacious.tilth.sim.crop.CropSpecies;
import com.claudacious.tilth.sim.crop.Crops;
import com.claudacious.tilth.sim.genetics.CropGenome;
import com.claudacious.tilth.sim.genetics.GeneticsModel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemNameBlockItem;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerLevel;

import java.util.Random;

/** Helpers for reading and writing crop genomes on seed stacks. */
public final class SeedGenetics {
    private SeedGenetics() {
    }

    public static CropSpecies speciesOf(ItemStack stack) {
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
        if (stack.getItem() instanceof ItemNameBlockItem seed && seed.getBlock() instanceof TilthCropBlock crop) {
            return crop.species();
        }
        return null;
    }

    public static CropGenome genomeOrDefault(ItemStack stack, CropSpecies species, Level level, BlockPos pos) {
        String encoded = stack.get(TilthDataComponents.CROP_GENOME.get());
        CropGenome genome = encoded == null ? null : CropGenome.decode(encoded);
        if (genome == null || !species.id.equals(genome.speciesId()) || "unknown".equals(genome.speciesId())) {
            long worldSeed = level instanceof ServerLevel server ? server.getSeed() : 0L;
            long salt = worldSeed ^ pos.asLong() ^ stack.hashCode();
            genome = GeneticsModel.randomLandrace(species.id, new Random(salt));
        }
        return genome.normalized();
    }

    public static CropGenome genomeOrLandrace(ItemStack stack, CropSpecies species) {
        String encoded = stack.get(TilthDataComponents.CROP_GENOME.get());
        CropGenome genome = encoded == null ? CropGenome.landrace(species.id) : CropGenome.decode(encoded);
        if (!species.id.equals(genome.speciesId())) {
            return CropGenome.landrace(species.id);
        }
        return genome.normalized();
    }

    public static void setGenome(ItemStack stack, CropGenome genome) {
        stack.set(TilthDataComponents.CROP_GENOME.get(), genome.normalized().encode());
    }

    public static CropSpecies byGenome(CropGenome genome) {
        CropSpecies species = Crops.byId(genome.speciesId());
        return species == null ? Crops.RADISH : species;
    }
}
