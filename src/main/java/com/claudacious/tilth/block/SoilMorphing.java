package com.claudacious.tilth.block;

import com.claudacious.tilth.TilthConfig;
import com.claudacious.tilth.registry.TilthBlocks;
import com.claudacious.tilth.sim.climate.BiomeClimate;
import com.claudacious.tilth.sim.soil.SoilHorizon;
import com.claudacious.tilth.sim.soil.SoilTexture;
import com.claudacious.tilth.world.ClimateService;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.SnowyDirtBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * Pedogenesis in miniature. A soil block that ends up somewhere new slowly takes on the character of
 * that place:
 * <ul>
 *   <li>subsoil kept wet (by water beside it or a water table at its level) turns grey: gleying;</li>
 *   <li>gley that drains again oxidises back to subsoil;</li>
 *   <li>subsoil exposed to sky and plants accumulates humus and becomes topsoil;</li>
 *   <li>topsoil buried under rock or soil loses its humus and becomes subsoil;</li>
 *   <li>waterlogged topsoil in cold, wet climates accumulates peat;</li>
 *   <li>bare topsoil on a slope loses fines to rain (coarsening), which settle downslope (fining).</li>
 * </ul>
 */
public final class SoilMorphing {
    private SoilMorphing() {
    }

    public static void tick(ServerLevel level, BlockPos pos, BlockState state, SoilLike soil, RandomSource random) {
        if (!TilthConfig.SOIL_MORPHING.getAsBoolean() || random.nextDouble() > TilthConfig.MORPH_CHANCE.getAsDouble()) {
            return;
        }
        switch (soil.horizon()) {
            case SUBSOIL -> subsoil(level, pos, soil.texture(), random);
            case GLEY -> gley(level, pos);
            case TOPSOIL -> topsoil(level, pos, state, soil.texture(), random);
        }
    }

    private static void subsoil(ServerLevel level, BlockPos pos, SoilTexture texture, RandomSource random) {
        if (isWet(level, pos) && texture.clay() >= 15) {
            level.setBlockAndUpdate(pos, TilthBlocks.GLEY.get().defaultBlockState());
            return;
        }
        BlockPos above = pos.above();
        if (level.getBlockState(above).isAir() && level.canSeeSky(above) && random.nextInt(4) == 0) {
            level.setBlockAndUpdate(pos, TilthBlocks.topsoil(texture));
        }
    }

    private static void gley(ServerLevel level, BlockPos pos) {
        if (!isWet(level, pos) && !isWet(level, pos.below())) {
            level.setBlockAndUpdate(pos, TilthBlocks.subsoil(SoilTexture.SILTY_CLAY_LOAM));
        }
    }

    private static void topsoil(ServerLevel level, BlockPos pos, BlockState state, SoilTexture texture, RandomSource random) {
        BlockPos above = pos.above();
        BlockState aboveState = level.getBlockState(above);
        boolean isGrass = state.getBlock() instanceof SoilGrassBlock;

        if (aboveState.isSolidRender(level, above) && !(aboveState.getBlock() instanceof SoilLike) && random.nextInt(3) == 0) {
            if (!texture.isOrganic()) {
                level.setBlockAndUpdate(pos, TilthBlocks.subsoil(texture));
            }
            return;
        }

        if (!texture.isOrganic() && isWet(level, pos)) {
            BiomeClimate climate = ClimateService.climateAt(level, pos);
            if (climate.meanTemp() < 12 && climate.annualPrecip() > 500 && random.nextInt(8) == 0) {
                level.setBlockAndUpdate(pos, isGrass ? TilthBlocks.grass(SoilTexture.PEAT) : TilthBlocks.topsoil(SoilTexture.PEAT));
                return;
            }
        }

        if (TilthConfig.EROSION.getAsBoolean() && !isGrass && !texture.isOrganic() && aboveState.isAir()
                && level.canSeeSky(above) && level.isRainingAt(above)) {
            erode(level, pos, texture);
        }
    }

    /** Moves fines from a bare block to the lowest soil within a few blocks downslope. */
    private static void erode(ServerLevel level, BlockPos pos, SoilTexture texture) {
        BlockPos lowest = null;
        int lowestY = pos.getY() - 1;
        for (Direction d : Direction.Plane.HORIZONTAL) {
            for (int dist = 1; dist <= 3; dist++) {
                BlockPos p = pos.relative(d, dist);
                if (!level.isLoaded(p)) {
                    break;
                }
                int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, p.getX(), p.getZ()) - 1;
                if (y < lowestY) {
                    lowestY = y;
                    lowest = new BlockPos(p.getX(), y, p.getZ());
                }
            }
        }
        if (lowest == null || pos.getY() - lowestY < 2) {
            return;
        }
        SoilTexture coarser = texture.coarser();
        if (coarser != texture) {
            level.setBlockAndUpdate(pos, TilthBlocks.topsoil(coarser));
        }
        BlockState target = level.getBlockState(lowest);
        if (target.getBlock() instanceof SoilLike sink && sink.horizon() == SoilHorizon.TOPSOIL && !sink.texture().isOrganic()) {
            SoilTexture finer = sink.texture().finer();
            if (finer != sink.texture()) {
                if (target.getBlock() instanceof SoilGrassBlock) {
                    level.setBlockAndUpdate(lowest, TilthBlocks.grass(finer).setValue(SnowyDirtBlock.SNOWY, target.getValue(SnowyDirtBlock.SNOWY)));
                } else if (target.getBlock() instanceof SoilBlock) {
                    level.setBlockAndUpdate(lowest, TilthBlocks.topsoil(finer));
                }
            }
        }
    }

    /** Water beside or above the block, or standing in a puddle: the profile is saturated here. */
    static boolean isWet(ServerLevel level, BlockPos pos) {
        for (Direction d : Direction.values()) {
            if (d == Direction.DOWN) {
                continue;
            }
            BlockPos p = pos.relative(d);
            if (level.getFluidState(p).is(FluidTags.WATER) || level.getBlockState(p).is(TilthBlocks.PUDDLE.get())) {
                return true;
            }
        }
        return false;
    }
}
