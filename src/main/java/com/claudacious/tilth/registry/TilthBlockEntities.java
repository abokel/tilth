package com.claudacious.tilth.registry;

import com.claudacious.tilth.Tilth;
import com.claudacious.tilth.block.CropBlockEntity;
import com.claudacious.tilth.block.FarmlandBlockEntity;
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

    private TilthBlockEntities() {
    }
}
