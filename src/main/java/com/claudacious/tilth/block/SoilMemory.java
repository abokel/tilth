package com.claudacious.tilth.block;

import com.claudacious.tilth.registry.TilthAttachments;
import com.claudacious.tilth.sim.soil.SoilState;
import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;

import java.util.HashMap;
import java.util.Map;

/**
 * Per-chunk memory of farmed soil that reverted to untilled ground, so depletion, liming and crop
 * history survive a field being abandoned and later ploughed again.
 */
public final class SoilMemory {
    private static final int MAX_ENTRIES = 4096;
    public static final Codec<SoilMemory> CODEC = Codec.unboundedMap(Codec.STRING, CompoundTag.CODEC)
            .xmap(SoilMemory::new, m -> m.entries);

    private final Map<String, CompoundTag> entries;

    public SoilMemory() {
        this(new HashMap<>());
    }

    private SoilMemory(Map<String, CompoundTag> entries) {
        this.entries = new HashMap<>(entries);
    }

    public static void remember(Level level, BlockPos pos, SoilState state) {
        LevelChunk chunk = level.getChunkAt(pos);
        SoilMemory memory = chunk.getData(TilthAttachments.SOIL_MEMORY);
        if (memory.entries.size() >= MAX_ENTRIES) {
            memory.entries.remove(memory.entries.keySet().iterator().next());
        }
        memory.entries.put(Long.toString(pos.asLong()), SoilNbt.write(state));
        chunk.setUnsaved(true);
    }

    public static SoilState recall(Level level, BlockPos pos) {
        LevelChunk chunk = level.getChunkAt(pos);
        if (!chunk.hasData(TilthAttachments.SOIL_MEMORY)) {
            return null;
        }
        SoilMemory memory = chunk.getData(TilthAttachments.SOIL_MEMORY);
        CompoundTag tag = memory.entries.remove(Long.toString(pos.asLong()));
        if (tag == null) {
            return null;
        }
        chunk.setUnsaved(true);
        return SoilNbt.read(tag);
    }
}
