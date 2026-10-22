package com.claudacious.tilth.water;

import com.claudacious.tilth.TilthConfig;
import com.claudacious.tilth.block.SoilLike;
import com.claudacious.tilth.block.TilthFarmlandBlock;
import com.claudacious.tilth.block.FarmlandBlockEntity;
import com.claudacious.tilth.registry.TilthBlocks;
import com.claudacious.tilth.sim.climate.DailyWeather;
import com.claudacious.tilth.sim.soil.Hydraulics;
import com.claudacious.tilth.world.ClimateService;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Surface water outside rivers and oceans.
 *
 * <p>During rain, water runs downhill to the lowest point of the local terrain. If that point sits in
 * a closed hollow (no outlet below the water line within {@code maxPondArea} blocks) and the ground
 * drains slowly, the hollow gains water block by block and becomes a pond. On flat, poorly drained or
 * compacted ground rain leaves puddles instead. In dry weather small isolated water bodies shrink
 * from their edges at a rate tied to the day's evapotranspiration; lakes, and ponds fed by the water
 * table, persist.
 */
public final class SurfaceWater {
    private static final int[][] NEIGHBOURS = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}, {1, 1}, {1, -1}, {-1, 1}, {-1, -1}};

    private SurfaceWater() {
    }

    public static void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level) || !level.dimensionType().hasSkyLight()) {
            return;
        }
        int interval = TilthConfig.SURFACE_WATER_INTERVAL.getAsInt();
        if (level.getGameTime() % interval != 0) {
            return;
        }
        boolean ponds = TilthConfig.PONDS.getAsBoolean();
        boolean puddles = TilthConfig.PUDDLES.getAsBoolean();
        boolean evaporation = TilthConfig.EVAPORATION.getAsBoolean();
        if (!ponds && !puddles && !evaporation) {
            return;
        }
        int samples = TilthConfig.SURFACE_WATER_SAMPLES.getAsInt();
        int radius = TilthConfig.SURFACE_WATER_RADIUS.getAsInt();
        RandomSource r = level.random;
        for (ServerPlayer player : level.players()) {
            for (int i = 0; i < samples; i++) {
                int x = player.getBlockX() + r.nextInt(2 * radius + 1) - radius;
                int z = player.getBlockZ() + r.nextInt(2 * radius + 1) - radius;
                if (!level.hasChunkAt(x, z)) {
                    continue;
                }
                BlockPos top = new BlockPos(x, level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z), z);
                if (level.isRainingAt(top)) {
                    rain(level, x, z, r, ponds, puddles);
                } else if (evaporation) {
                    evaporate(level, top.below(), r);
                }
            }
        }
    }

    // ------------------------------------------------------------------ rain

    private static int ground(ServerLevel level, int x, int z) {
        return level.getHeight(Heightmap.Types.OCEAN_FLOOR, x, z) - 1;
    }

    /** How freely the ground at a position lets water soak in: 0 impermeable, 1 free-draining. */
    public static double permeability(ServerLevel level, BlockPos ground) {
        BlockState s = level.getBlockState(ground);
        if (s.getBlock() instanceof SoilLike soil) {
            Hydraulics h = soil.texture().hydraulics(2.5);
            double p = Math.min(1.0, h.ksat() / 25.0);
            if (s.getBlock() instanceof TilthFarmlandBlock && level.getBlockEntity(ground) instanceof FarmlandBlockEntity be) {
                p *= 1.0 - 0.8 * be.soil().compaction;
            }
            return p;
        }
        if (s.is(BlockTags.SAND) || s.is(Blocks.GRAVEL)) {
            return 1.0;
        }
        if (s.is(BlockTags.DIRT) || s.is(Blocks.FARMLAND)) {
            return s.is(Blocks.MUD) ? 0.05 : 0.6;
        }
        if (s.is(Blocks.CLAY) || s.is(BlockTags.TERRACOTTA) || s.is(BlockTags.BASE_STONE_OVERWORLD)) {
            return 0.02;
        }
        return 0.3;
    }

    private static void rain(ServerLevel level, int x, int z, RandomSource r, boolean ponds, boolean puddles) {
        // Follow the steepest descent to the bottom of the local hollow.
        int cx = x;
        int cz = z;
        int h = ground(level, cx, cz);
        for (int step = 0; step < 24; step++) {
            int bestX = cx;
            int bestZ = cz;
            int bestH = h;
            for (int[] n : NEIGHBOURS) {
                int nx = cx + n[0];
                int nz = cz + n[1];
                if (!level.hasChunkAt(nx, nz)) {
                    continue;
                }
                int nh = ground(level, nx, nz);
                if (nh < bestH) {
                    bestH = nh;
                    bestX = nx;
                    bestZ = nz;
                }
            }
            if (bestH >= h) {
                break;
            }
            cx = bestX;
            cz = bestZ;
            h = bestH;
        }
        BlockPos bottomGround = new BlockPos(cx, h, cz);
        if (WaterSourceRules.isInfinite(level, bottomGround)) {
            return;
        }
        double permeability = permeability(level, bottomGround);

        // Fill the hollow from the lowest unfilled layer upward; stop where the water would spill out.
        if (ponds && r.nextDouble() < TilthConfig.POND_CHANCE.getAsDouble() * (1.0 - permeability)) {
            boolean closedAtAll = false;
            for (int waterY = h + 1; waterY <= h + 5; waterY++) {
                List<BlockPos> basin = closedBasin(level, cx, cz, waterY, TilthConfig.MAX_POND_AREA.getAsInt());
                if (basin == null) {
                    break;
                }
                closedAtAll = true;
                for (BlockPos cell : basin) {
                    BlockState at = level.getBlockState(cell);
                    FluidState fluid = at.getFluidState();
                    if (isSource(fluid)) {
                        continue;
                    }
                    boolean replaceable = at.isAir() || at.canBeReplaced() || at.is(TilthBlocks.PUDDLE.get()) || fluid.is(FluidTags.WATER);
                    BlockState below = level.getBlockState(cell.below());
                    boolean supported = below.isFaceSturdy(level, cell.below(), Direction.UP) || isSource(below.getFluidState());
                    if (replaceable && supported) {
                        level.setBlock(cell, Blocks.WATER.defaultBlockState(), Block.UPDATE_ALL);
                        return;
                    }
                }
            }
            if (closedAtAll) {
                return;
            }
        }
        if (puddles && permeability < 0.35) {
            BlockPos at = bottomGround.above();
            BlockState state = level.getBlockState(at);
            if (state.isAir() && TilthBlocks.PUDDLE.get().defaultBlockState().canSurvive(level, at)
                    && r.nextDouble() < TilthConfig.PUDDLE_CHANCE.getAsDouble() * (1.0 - permeability / 0.35)) {
                level.setBlock(at, TilthBlocks.PUDDLE.get().defaultBlockState(), Block.UPDATE_ALL);
            }
        }
    }

    /**
     * Columns that water at {@code level} would cover starting from the hollow bottom, in fill order
     * (nearest first), or null if the water would escape (no closed rim within the area limit).
     */
    public static List<BlockPos> closedBasin(ServerLevel level, int x, int z, int waterY, int maxArea) {
        ArrayDeque<long[]> queue = new ArrayDeque<>();
        Set<Long> seen = new HashSet<>();
        List<BlockPos> cells = new ArrayList<>();
        queue.add(new long[]{x, z});
        seen.add(key(x, z));
        while (!queue.isEmpty()) {
            long[] c = queue.poll();
            int px = (int) c[0];
            int pz = (int) c[1];
            if (!level.hasChunkAt(px, pz)) {
                return null;
            }
            cells.add(new BlockPos(px, waterY, pz));
            if (cells.size() > maxArea) {
                return null;
            }
            for (int i = 0; i < 4; i++) {
                int nx = px + NEIGHBOURS[i][0];
                int nz = pz + NEIGHBOURS[i][1];
                if (seen.add(key(nx, nz)) && level.hasChunkAt(nx, nz) && ground(level, nx, nz) < waterY) {
                    queue.add(new long[]{nx, nz});
                }
            }
        }
        return cells;
    }

    private static long key(int x, int z) {
        return ((long) x << 32) ^ (z & 0xFFFFFFFFL);
    }

    private static boolean isSource(FluidState f) {
        return f.is(FluidTags.WATER) && f.isSource();
    }

    // ------------------------------------------------------------------ evaporation

    private static void evaporate(ServerLevel level, BlockPos surface, RandomSource r) {
        if (!isSource(level.getFluidState(surface)) || !level.canSeeSky(surface.above()) || WaterSourceRules.isInfinite(level, surface)) {
            return;
        }
        if (TilthConfig.GROUNDWATER_PONDS_PERSIST.getAsBoolean() && surface.getY() <= level.getSeaLevel()) {
            return;
        }
        DailyWeather w = ClimateService.today(level, surface);
        double chance = TilthConfig.EVAPORATION_RATE.getAsDouble() * Math.max(0, w.et0()) / 20.0;
        if (r.nextDouble() >= chance) {
            return;
        }
        int maxBody = TilthConfig.MAX_EVAPORATING_BODY.getAsInt();
        List<BlockPos> body = waterSurface(level, surface, maxBody + 1);
        if (body == null || body.size() > maxBody) {
            return;
        }
        BlockPos edge = surface;
        int mostDry = -1;
        for (BlockPos p : body) {
            int dry = 0;
            for (Direction d : Direction.Plane.HORIZONTAL) {
                if (!level.getFluidState(p.relative(d)).is(FluidTags.WATER)) {
                    dry++;
                }
            }
            if (dry > mostDry) {
                mostDry = dry;
                edge = p;
            }
        }
        level.setBlock(edge, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
    }

    /** Connected water-source surface cells at one height, or null if it touches unloaded chunks. */
    private static List<BlockPos> waterSurface(ServerLevel level, BlockPos start, int limit) {
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        Set<BlockPos> seen = new HashSet<>();
        List<BlockPos> out = new ArrayList<>();
        queue.add(start);
        seen.add(start);
        while (!queue.isEmpty() && out.size() <= limit) {
            BlockPos p = queue.poll();
            if (!level.isLoaded(p)) {
                return null;
            }
            out.add(p);
            for (Direction d : Direction.Plane.HORIZONTAL) {
                BlockPos n = p.relative(d);
                if (seen.add(n) && level.isLoaded(n) && level.getFluidState(n).is(FluidTags.WATER)) {
                    queue.add(n);
                }
            }
        }
        return out;
    }

    /** Random tick of a puddle: dries out in dry weather. */
    public static void puddleTick(ServerLevel level, BlockPos pos, RandomSource r) {
        if (level.isRainingAt(pos)) {
            return;
        }
        DailyWeather w = ClimateService.today(level, pos);
        double chance = (0.15 + Math.max(0, w.et0()) / 8.0) * TilthConfig.EVAPORATION_RATE.getAsDouble();
        if (r.nextDouble() < chance) {
            level.setBlock(pos, Fluids.EMPTY.defaultFluidState().createLegacyBlock(), Block.UPDATE_ALL);
        }
    }
}
