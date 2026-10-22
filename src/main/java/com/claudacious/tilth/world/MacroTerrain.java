package com.claudacious.tilth.world;

import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Landscape-scale terrain: the noise generator's surface height sampled on a 32-block grid and
 * cached, giving the Gaussian-weighted mean elevation within ~80 blocks. A column's height above
 * that mean tells hilltops from valley floors far beyond what one chunk can see.
 */
public final class MacroTerrain {
    private static final int GRID = 32;
    private static final int RADIUS_CELLS = 3;
    private static final double SIGMA = 48.0;
    private static final Map<ResourceKey<Level>, ConcurrentHashMap<Long, Integer>> CACHE = new ConcurrentHashMap<>();

    private MacroTerrain() {
    }

    public static double mean(ServerLevel level, int x, int z) {
        return mean(level, level.getChunkSource().getGenerator(), level.getChunkSource().randomState(), level, x, z);
    }

    public static double mean(ServerLevel level, ChunkGenerator generator, RandomState randomState, LevelHeightAccessor heights, int x, int z) {
        ConcurrentHashMap<Long, Integer> cache = CACHE.computeIfAbsent(level.dimension(), k -> new ConcurrentHashMap<>());
        if (cache.size() > 250_000) {
            cache.clear();
        }
        int seaLevel = level.getSeaLevel();
        int gx0 = Math.floorDiv(x, GRID);
        int gz0 = Math.floorDiv(z, GRID);
        double sum = 0;
        double weights = 0;
        for (int dx = -RADIUS_CELLS; dx <= RADIUS_CELLS; dx++) {
            for (int dz = -RADIUS_CELLS; dz <= RADIUS_CELLS; dz++) {
                final int gx = gx0 + dx;
                final int gz = gz0 + dz;
                long key = ((long) gx << 32) ^ (gz & 0xFFFFFFFFL);
                int h = cache.computeIfAbsent(key, k -> generator.getBaseHeight(gx * GRID + GRID / 2, gz * GRID + GRID / 2,
                        Heightmap.Types.OCEAN_FLOOR_WG, heights, randomState));
                double cx = gx * GRID + GRID / 2.0 - x;
                double cz = gz * GRID + GRID / 2.0 - z;
                double w = Math.exp(-(cx * cx + cz * cz) / (2 * SIGMA * SIGMA));
                sum += w * Math.max(h, seaLevel);
                weights += w;
            }
        }
        return sum / weights;
    }

    public static void clear() {
        CACHE.clear();
    }
}
