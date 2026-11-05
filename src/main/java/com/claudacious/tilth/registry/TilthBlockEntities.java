package com.claudacious.tilth.registry;

import com.claudacious.tilth.Tilth;
import com.claudacious.tilth.block.CropBlockEntity;
import com.claudacious.tilth.block.DryingRackBlockEntity;
import com.claudacious.tilth.block.FarmlandBlockEntity;
import com.claudacious.tilth.block.GrainSiloBlockEntity;
import com.claudacious.tilth.block.IncubatorBlockEntity;
import com.claudacious.tilth.block.SoilLabBlockEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class TilthBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Tilth.MOD_ID);

    @SuppressWarnings("DataFlowIssue")
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<FarmlandBlockEntity>> FARMLAND = BLOCK_ENTITIES.register("farmland",
            () -> BlockEntityType.Builder.of(FarmlandBlockEntity::new,
                    TilthBlocks.FARMLAND.values().stream().map(DeferredBlock::get).toArray(Block[]::new)).build(null));

    @SuppressWarnings("DataFlowIssue")
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CropBlockEntity>> CROP = BLOCK_ENTITIES.register("crop",
            () -> BlockEntityType.Builder.of(CropBlockEntity::new,
                    TilthBlocks.CROPS.values().stream().map(DeferredBlock::get).toArray(Block[]::new)).build(null));

    @SuppressWarnings("DataFlowIssue")
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SoilLabBlockEntity>> SOIL_LAB = BLOCK_ENTITIES.register("soil_lab",
            () -> BlockEntityType.Builder.of(SoilLabBlockEntity::new, TilthBlocks.SOIL_LAB.get()).build(null));

    @SuppressWarnings("DataFlowIssue")
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<IncubatorBlockEntity>> INCUBATOR = BLOCK_ENTITIES.register("incubator",
            () -> BlockEntityType.Builder.of(IncubatorBlockEntity::new, TilthBlocks.INCUBATOR.get()).build(null));

    @SuppressWarnings("DataFlowIssue")
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<DryingRackBlockEntity>> DRYING_RACK = BLOCK_ENTITIES.register("drying_rack",
            () -> BlockEntityType.Builder.of(DryingRackBlockEntity::new, TilthBlocks.DRYING_RACK.get()).build(null));

    @SuppressWarnings("DataFlowIssue")
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<com.claudacious.tilth.block.CompostBinBlockEntity>> COMPOST_BIN =
            BLOCK_ENTITIES.register("compost_bin", () -> BlockEntityType.Builder.of(com.claudacious.tilth.block.CompostBinBlockEntity::new,
                    TilthBlocks.COMPOST_BIN.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<com.claudacious.tilth.block.CheesePressBlockEntity>> CHEESE_PRESS =
            BLOCK_ENTITIES.register("cheese_press", () -> BlockEntityType.Builder.of(com.claudacious.tilth.block.CheesePressBlockEntity::new,
                    TilthBlocks.CHEESE_PRESS.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<GrainSiloBlockEntity>> GRAIN_SILO = BLOCK_ENTITIES.register("grain_silo",
            () -> BlockEntityType.Builder.of(GrainSiloBlockEntity::new, TilthBlocks.GRAIN_SILO.get()).build(null));

    private TilthBlockEntities() {
    }
}
