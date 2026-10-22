package com.claudacious.tilth.world;

import com.claudacious.tilth.sim.catena.CatenaAnalyzer;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;

/** Runtime terrain analysis of loaded chunks (worldgen uses its own region-safe height grid). */
public final class TerrainService {
    private TerrainService() {
    }

    /**
     * Top of the ground at a column, or {@code fallback} when the chunk is not loaded. The lower of two
     * heightmaps skips both water surfaces (OCEAN_FLOOR ignores fluids) and tree canopies
     * (MOTION_BLOCKING_NO_LEAVES ignores leaves), so a pond reads as a hollow and a forest as ground.
     */
    public static int surfaceHeight(Level level, int x, int z, int fallback) {
        if (!level.hasChunkAt(x, z)) {
            return fallback;
        }
        int noLeaves = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
        int floor = level.getHeight(Heightmap.Types.OCEAN_FLOOR, x, z);
        return Math.min(noLeaves, floor) - 1;
    }

    public static CatenaAnalyzer.Result analyze(ServerLevel level, BlockPos pos) {
        final int y0 = surfaceHeight(level, pos.getX(), pos.getZ(), pos.getY());
        CatenaAnalyzer.HeightField field = (x, z) -> surfaceHeight(level, x, z, y0);
        double macro = y0 - MacroTerrain.mean(level, pos.getX(), pos.getZ());
        boolean nearWater = waterWithin(level, pos.atY(y0), 5, 2);
        return CatenaAnalyzer.analyze(field, pos.getX(), pos.getZ(), macro, nearWater, level.getSeaLevel());
    }

    /** True if water lies within {@code radius} blocks horizontally and {@code dy} vertically. */
    public static boolean waterWithin(Level level, BlockPos center, int radius, int dy) {
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        for (int y = -dy; y <= dy; y++) {
            for (int x = -radius; x <= radius; x++) {
                for (int z = -radius; z <= radius; z++) {
                    m.set(center.getX() + x, center.getY() + y, center.getZ() + z);
                    if (level.isLoaded(m) && level.getFluidState(m).is(FluidTags.WATER)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
}
