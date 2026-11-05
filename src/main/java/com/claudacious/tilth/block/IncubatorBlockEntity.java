package com.claudacious.tilth.block;

import com.claudacious.tilth.TilthConfig;
import com.claudacious.tilth.entity.HomunculusEntity;
import com.claudacious.tilth.genetics.SeedGenetics;
import com.claudacious.tilth.registry.TilthBlockEntities;
import com.claudacious.tilth.registry.TilthDataComponents;
import com.claudacious.tilth.sim.crop.CropSpecies;
import com.claudacious.tilth.sim.crop.Crops;
import com.claudacious.tilth.sim.genetics.CropGenome;
import com.claudacious.tilth.sim.genetics.GeneticsModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Random;

public class IncubatorBlockEntity extends BlockEntity {
    public enum InsertResult {
        OK,
        BUSY,
        INVALID
    }

    private ItemStack embryo = ItemStack.EMPTY;
    private ItemStack output = ItemStack.EMPTY;
    private int progress;
    private CropGenome genome;

    public IncubatorBlockEntity(BlockPos pos, BlockState state) {
        super(TilthBlockEntities.INCUBATOR.get(), pos, state);
    }

    public InsertResult insert(ItemStack stack) {
        if (!embryo.isEmpty() || !output.isEmpty()) {
            return InsertResult.BUSY;
        }
        CropGenome decoded = decodeEmbryoGenome(stack);
        if (decoded == null) {
            return InsertResult.INVALID;
        }
        embryo = stack.copyWithCount(1);
        progress = 0;
        genome = decoded;
        setWorking(true);
        setChanged();
        return InsertResult.OK;
    }

    public boolean isWorking() {
        return !embryo.isEmpty();
    }

    public int progressPercent() {
        int total = Math.max(1, TilthConfig.INCUBATOR_TICKS.getAsInt());
        return progress * 100 / total;
    }

    public ItemStack contents() {
        return embryo;
    }

    public ItemStack takeOutput() {
        ItemStack out = output;
        output = ItemStack.EMPTY;
        setChanged();
        return out;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, IncubatorBlockEntity incubator) {
        if (!(level instanceof ServerLevel server) || incubator.embryo.isEmpty()) {
            return;
        }
        if (incubator.genome == null) {
            incubator.output = incubator.embryo;
            incubator.embryo = ItemStack.EMPTY;
            incubator.progress = 0;
            incubator.setWorking(false);
            incubator.setChanged();
            return;
        }
        if (++incubator.progress < Math.max(1, TilthConfig.INCUBATOR_TICKS.getAsInt())) {
            incubator.setChanged();
            return;
        }
        Random random = new Random(server.getSeed() ^ pos.asLong() ^ server.getGameTime());
        CropGenome g = GeneticsModel.mutateCrop(incubator.genome, random);
        incubator.progress = 0;
        incubator.embryo = ItemStack.EMPTY;
        incubator.genome = null;
        incubator.setWorking(false);
        if (random.nextDouble() < TilthConfig.HOMUNCULUS_ESCAPE_CHANCE.getAsDouble()) {
            HomunculusEntity.spawn(server, pos.above());
            server.playSound(null, pos, SoundEvents.SLIME_SQUISH, SoundSource.BLOCKS, 1.0f, 0.7f);
        } else {
            CropSpecies species = Crops.byId(g.speciesId());
            if (species == null) {
                incubator.output = new ItemStack(com.claudacious.tilth.registry.TilthItems.SPLICED_EMBRYO.get());
                SeedGenetics.setGenome(incubator.output, g);
                incubator.setChanged();
                return;
            }
            ItemStack out = new ItemStack(net.minecraft.core.registries.BuiltInRegistries.ITEM.get(net.minecraft.resources.ResourceLocation.parse(species.seedItem)), 2);
            SeedGenetics.setGenome(out, g);
            incubator.output = out;
            server.playSound(null, pos, SoundEvents.BREWING_STAND_BREW, SoundSource.BLOCKS, 0.8f, 1.4f);
        }
        incubator.setChanged();
    }

    private void setWorking(boolean working) {
        if (level != null && getBlockState().getValue(IncubatorBlock.WORKING) != working) {
            level.setBlock(worldPosition, getBlockState().setValue(IncubatorBlock.WORKING, working), 3);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (!embryo.isEmpty()) {
            tag.put("Embryo", embryo.save(registries));
        }
        if (!output.isEmpty()) {
            tag.put("Output", output.save(registries));
        }
        if (genome != null) {
            tag.putString("Genome", genome.encode());
        }
        tag.putInt("Progress", progress);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        embryo = ItemStack.parseOptional(registries, tag.getCompound("Embryo"));
        output = ItemStack.parseOptional(registries, tag.getCompound("Output"));
        progress = tag.getInt("Progress");
        genome = tag.contains("Genome") ? CropGenome.tryDecode(tag.getString("Genome")) : null;
        if (!embryo.isEmpty() && genome == null) {
            genome = decodeEmbryoGenome(embryo);
        }
    }

    private static CropGenome decodeEmbryoGenome(ItemStack stack) {
        String encoded = stack.get(TilthDataComponents.CROP_GENOME.get());
        CropGenome decoded = CropGenome.tryDecode(encoded);
        if (decoded == null || Crops.byId(decoded.speciesId()) == null) {
            return null;
        }
        return decoded.normalized();
    }
}
