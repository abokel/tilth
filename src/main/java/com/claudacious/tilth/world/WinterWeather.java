package com.claudacious.tilth.world;

import com.claudacious.tilth.TilthConfig;
import com.claudacious.tilth.item.ThermometerItem;
import com.claudacious.tilth.network.WeatherSync;
import com.claudacious.tilth.sim.climate.DailyWeather;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Winter in temperate biomes, driven by the Tilth climate rather than the biome's fixed temperature:
 * precipitation falls as snow (clients are told, so they draw snow rather than rain) and settles, still water
 * freezes in hard frost, and both thaw when it warms. Biomes that are snowy in vanilla are left to vanilla.
 */
public final class WinterWeather {
    private static final int INTERVAL = 10;
    private static final int SYNC_INTERVAL = 40;
    private static final int SAMPLES = 20;
    private static final int RADIUS = 48;
    private static final int MAX_LAYERS = 3;

    private WinterWeather() {
    }

    /** Whether precipitation here falls as snow under the Tilth climate (and vanilla would show rain). */
    public static boolean snowingAt(ServerLevel level, BlockPos pos) {
        if (!level.isRaining() || !TilthConfig.WINTER_SNOW.getAsBoolean()) {
            return false;
        }
        Biome biome = level.getBiome(pos).value();
        if (!biome.hasPrecipitation() || biome.coldEnoughToSnow(pos)) {
            return false;
        }
        DailyWeather w = ClimateService.today(level, pos);
        return w.snow() > w.rain() || ThermometerItem.airNow(w, level.getDayTime()) <= 1.0;
    }

    public static void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level) || !level.dimensionType().hasSkyLight()) {
            return;
        }
        if (level.getGameTime() % SYNC_INTERVAL == 0) {
            for (ServerPlayer player : level.players()) {
                PacketDistributor.sendToPlayer(player, new WeatherSync(snowingAt(level, player.blockPosition())));
            }
        }
        if (level.getGameTime() % INTERVAL != 0 || !TilthConfig.WINTER_SNOW.getAsBoolean()) {
            return;
        }
        RandomSource r = level.random;
        for (ServerPlayer player : level.players()) {
            for (int i = 0; i < SAMPLES; i++) {
                int x = player.getBlockX() + r.nextInt(2 * RADIUS + 1) - RADIUS;
                int z = player.getBlockZ() + r.nextInt(2 * RADIUS + 1) - RADIUS;
                if (level.hasChunkAt(x, z)) {
                    sample(level, x, z, r);
                }
            }
        }
    }

    private static void sample(ServerLevel level, int x, int z, RandomSource r) {
        BlockPos top = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, new BlockPos(x, 0, z));
        Biome biome = level.getBiome(top).value();
        if (biome.coldEnoughToSnow(top) || !biome.hasPrecipitation()) {
            return;
        }
        DailyWeather w = ClimateService.today(level, top);
        BlockPos surface = top.below();
        BlockState below = level.getBlockState(surface);
        BlockState at = level.getBlockState(top);
        if (below.is(Blocks.WATER) && below.getFluidState().isSourceOfType(Fluids.WATER) && w.tMax() < 0.0) {
            if (r.nextFloat() < 0.35f) {
                level.setBlockAndUpdate(surface, Blocks.ICE.defaultBlockState());
            }
        } else if (snowingAt(level, top)) {
            if (r.nextFloat() < 0.6f) {
                if (at.is(Blocks.SNOW) && at.getValue(SnowLayerBlock.LAYERS) < MAX_LAYERS) {
                    level.setBlockAndUpdate(top, at.setValue(SnowLayerBlock.LAYERS, at.getValue(SnowLayerBlock.LAYERS) + 1));
                } else if ((at.isAir() || at.canBeReplaced() && !at.liquid()) && Blocks.SNOW.defaultBlockState().canSurvive(level, top)) {
                    level.setBlockAndUpdate(top, Blocks.SNOW.defaultBlockState());
                }
            }
        } else if (w.tMax() > 3.0) {
            if (at.is(Blocks.SNOW) && r.nextFloat() < 0.5f) {
                int layers = at.getValue(SnowLayerBlock.LAYERS);
                level.setBlockAndUpdate(top, layers > 1 ? at.setValue(SnowLayerBlock.LAYERS, layers - 1) : Blocks.AIR.defaultBlockState());
            } else if (below.is(Blocks.ICE) && level.getBlockState(surface.below()).getFluidState().is(Fluids.WATER) && r.nextFloat() < 0.3f) {
                level.setBlockAndUpdate(surface, Blocks.WATER.defaultBlockState());
            }
        }
    }
}
