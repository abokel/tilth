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

    /** Encoded crop cultivar/genome for saved seed and splicing. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<String>> CROP_GENOME = COMPONENTS.register("crop_genome",
            () -> DataComponentType.<String>builder().persistent(Codec.STRING).networkSynchronized(ByteBufCodecs.STRING_UTF8).build());

    /** Grain moisture fraction (0.06 = very dry, 0.35 = very wet). */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Float>> GRAIN_MOISTURE = COMPONENTS.register("grain_moisture",
            () -> DataComponentType.<Float>builder().persistent(Codec.floatRange(0.06f, 0.35f)).networkSynchronized(ByteBufCodecs.FLOAT).build());

    /** Grain carried on a silo block item whose controller was broken while full. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<com.claudacious.tilth.postharvest.SiloContents>> SILO_CONTENTS =
            COMPONENTS.register("silo_contents", () -> DataComponentType.<com.claudacious.tilth.postharvest.SiloContents>builder()
                    .persistent(com.claudacious.tilth.postharvest.SiloContents.CODEC)
                    .networkSynchronized(com.claudacious.tilth.postharvest.SiloContents.STREAM_CODEC).build());

    private TilthDataComponents() {
    }
}
