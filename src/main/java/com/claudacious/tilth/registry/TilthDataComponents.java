package com.claudacious.tilth.registry;

import com.claudacious.tilth.Tilth;
import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.List;

public final class TilthDataComponents {
    public static final DeferredRegister<DataComponentType<?>> COMPONENTS = DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, Tilth.MOD_ID);

    /** Harvest quality grade 1 (poor) to 5 (premium); carried through recipes into food. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> QUALITY = COMPONENTS.register("quality",
            () -> DataComponentType.<Integer>builder().persistent(Codec.intRange(1, 5)).networkSynchronized(ByteBufCodecs.VAR_INT).build());

    /** Corner stakes placed so far with a surveyor's chain. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<List<BlockPos>>> SURVEY_POINTS = COMPONENTS.register("survey_points",
            () -> DataComponentType.<List<BlockPos>>builder().persistent(BlockPos.CODEC.listOf())
                    .networkSynchronized(BlockPos.STREAM_CODEC.apply(ByteBufCodecs.list())).build());

    private TilthDataComponents() {
    }
}
