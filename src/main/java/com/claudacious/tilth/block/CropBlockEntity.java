package com.claudacious.tilth.block;

import com.claudacious.tilth.TilthConfig;
import com.claudacious.tilth.quality.Quality;
import com.claudacious.tilth.registry.TilthBlockEntities;
import com.claudacious.tilth.registry.TilthBlocks;
import com.claudacious.tilth.registry.TilthItems;
import com.claudacious.tilth.sim.crop.CropGrowthModel;
import com.claudacious.tilth.sim.crop.CropSpecies;
import com.claudacious.tilth.sim.crop.CropState;
import com.claudacious.tilth.sim.crop.GrowthForm;
import com.claudacious.tilth.sim.crop.HarvestResult;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/** Growth state of a planted crop. Advanced by the farmland beneath it. */
public class CropBlockEntity extends BlockEntity {
    private CropState crop = new CropState();
    private boolean initialized;
    @Nullable
    private List<ItemStack> pendingDrops;

    public CropBlockEntity(BlockPos pos, BlockState state) {
        super(TilthBlockEntities.CROP.get(), pos, state);
    }

    public CropSpecies species() {
        return ((TilthCropBlock) getBlockState().getBlock()).species();
    }

    public CropState cropState() {
        return crop;
    }

    public void markInitialized() {
        if (!initialized) {
            initialized = true;
            setChanged();
        }
    }

    public double lightFactor(ServerLevel level) {
        if (!TilthConfig.CROPS_NEED_LIGHT.getAsBoolean()) {
            return 1.0;
        }
        int sky = level.getBrightness(LightLayer.SKY, worldPosition);
        int block = level.getBrightness(LightLayer.BLOCK, worldPosition);
        return Math.max(Math.min(1.0, sky / 12.0), Math.min(0.6, block / 14.0 * 0.6));
    }

    /** Syncs the visual stage with the simulated state; a dead crop becomes a withered plant. */
    public void afterSimulation(ServerLevel level) {
        BlockState state = getBlockState();
        if (crop.dead) {
            if (level.getBlockEntity(worldPosition.below()) instanceof FarmlandBlockEntity farmland) {
                farmland.soil().addResidue(species().residueKgM2 * Math.min(1.0, crop.biomass), species().residueNPercent);
                farmland.setChanged();
            }
            level.setBlock(worldPosition, TilthBlocks.DEAD_CROP.get().defaultBlockState(), Block.UPDATE_ALL);
            return;
        }
        int stage = species().visualStage(crop);
        CropCondition condition = CropCondition.of(crop);
        if (state.getValue(TilthCropBlock.STAGE) != stage || state.getValue(TilthCropBlock.CONDITION) != condition) {
            level.setBlock(worldPosition, state.setValue(TilthCropBlock.STAGE, stage).setValue(TilthCropBlock.CONDITION, condition),
                    Block.UPDATE_CLIENTS);
        }
        setChanged();
    }

    /** Debug/bone meal: add a fraction of the crop's thermal-time requirement. */
    public void boost(ServerLevel level, double fraction) {
        CropSpecies sp = species();
        if (!crop.germinated) {
            crop.germinated = true;
            crop.biomass = 0.01;
        }
        crop.gdd += sp.gddToMaturity * fraction;
        crop.dev = crop.gdd / sp.gddToMaturity;
        crop.biomass += fraction * 0.8;
        afterSimulation(level);
    }

    /** Debug: jump straight to a visual stage. */
    public void setStage(ServerLevel level, int stage) {
        CropSpecies sp = species();
        crop.germinated = stage > 0;
        crop.dev = sp.stageStart[Math.max(0, Math.min(CropSpecies.STAGES - 1, stage))];
        crop.gdd = crop.dev * sp.gddToMaturity;
        crop.biomass = Math.max(crop.biomass, crop.dev * 0.9);
        afterSimulation(level);
    }

    public void harvest(ServerLevel level, @Nullable Player player) {
        CropSpecies sp = species();
        HarvestResult result = CropGrowthModel.harvest(sp, crop, new Random(level.random.nextLong()));
        List<ItemStack> drops = harvestStacks(level, sp, result);
        for (ItemStack stack : drops) {
            Block.popResource(level, worldPosition, stack);
        }
        boolean straw = sp.form == GrowthForm.CEREAL || sp.form == GrowthForm.TALL_GRASS;
        if (level.getBlockEntity(worldPosition.below()) instanceof FarmlandBlockEntity farmland) {
            farmland.recordHarvest(level, sp, result.residueKgM2() * (straw ? 0.5 : 1.0), result.residueNPercent());
        }
        level.playSound(null, worldPosition, SoundEvents.CROP_BREAK, SoundSource.BLOCKS, 1.0f, 1.0f);
        if (player != null) {
            player.displayClientMessage(Component.literal(String.format("Harvested %s: %s, yield %.1f %s", sp.name,
                    Quality.name(result.grade()), result.perAcre(), sp.yieldUnit.label())).withStyle(ChatFormatting.GREEN), true);
        }
        if (sp.perennial && crop.harvests + 1 < sp.maxHarvests) {
            CropGrowthModel.regrow(sp, crop);
            afterSimulation(level);
        } else {
            level.setBlock(worldPosition, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        }
    }

    /** Called before a player breaks the crop, while the soil below can still be updated. */
    public void prepareBreak(ServerLevel level) {
        CropSpecies sp = species();
        if (sp.isHarvestable(crop)) {
            HarvestResult result = CropGrowthModel.harvest(sp, crop, new Random(level.random.nextLong()));
            pendingDrops = harvestStacks(level, sp, result);
            if (level.getBlockEntity(worldPosition.below()) instanceof FarmlandBlockEntity farmland) {
                boolean straw = sp.form == GrowthForm.CEREAL || sp.form == GrowthForm.TALL_GRASS;
                farmland.recordHarvest(level, sp, result.residueKgM2() * (straw ? 0.5 : 1.0), result.residueNPercent());
            }
        } else if (crop.germinated && level.getBlockEntity(worldPosition.below()) instanceof FarmlandBlockEntity farmland) {
            // Pulling up a young crop returns its biomass to the soil as green residue.
            farmland.soil().addResidue(sp.residueKgM2 * Math.min(1.0, crop.biomass), Math.max(sp.residueNPercent, 1.5));
            farmland.setChanged();
        }
    }

    public List<ItemStack> breakDrops(ServerLevel level) {
        if (pendingDrops != null) {
            List<ItemStack> drops = pendingDrops;
            pendingDrops = null;
            return drops;
        }
        CropSpecies sp = species();
        if (sp.isHarvestable(crop)) {
            return harvestStacks(level, sp, CropGrowthModel.harvest(sp, crop, new Random(level.random.nextLong())));
        }
        if (!crop.germinated) {
            return List.of(new ItemStack(item(sp.seedItem)));
        }
        return List.of();
    }

    private static List<ItemStack> harvestStacks(ServerLevel level, CropSpecies sp, HarvestResult r) {
        List<ItemStack> out = new ArrayList<>();
        if (r.produce() > 0) {
            ItemStack produce = new ItemStack(item(sp.produceItem), r.produce());
            Quality.set(produce, r.grade());
            out.add(produce);
        }
        if (r.seeds() > 0) {
            out.add(new ItemStack(item(sp.seedItem), r.seeds()));
        }
        if ((sp.form == GrowthForm.CEREAL || sp.form == GrowthForm.TALL_GRASS) && r.residueKgM2() > 0.1) {
            out.add(new ItemStack(TilthItems.STRAW.get(), r.residueKgM2() > 0.4 ? 2 : 1));
        }
        if (sp.produceItem.equals("minecraft:potato") && level.random.nextFloat() < 0.02f) {
            out.add(new ItemStack(Items.POISONOUS_POTATO));
        }
        return out;
    }

    private static Item item(String id) {
        return BuiltInRegistries.ITEM.get(ResourceLocation.parse(id));
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("crop", CropNbt.write(crop));
        tag.putBoolean("initialized", initialized);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("crop")) {
            crop = CropNbt.read(tag.getCompound("crop"));
        }
        initialized = tag.getBoolean("initialized");
    }
}
