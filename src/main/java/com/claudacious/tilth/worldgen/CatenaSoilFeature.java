package com.claudacious.tilth.worldgen;

import com.claudacious.tilth.TilthConfig;
import com.claudacious.tilth.registry.TilthBlocks;
import com.claudacious.tilth.sim.Noise;
import com.claudacious.tilth.sim.catena.CatenaAnalyzer;
import com.claudacious.tilth.sim.catena.ParentMaterial;
import com.claudacious.tilth.sim.catena.SoilProfile;
import com.claudacious.tilth.sim.catena.SoilRegion;
import com.claudacious.tilth.sim.climate.BiomeClimate;
import com.claudacious.tilth.world.ClimateService;
import com.claudacious.tilth.world.MacroTerrain;
import com.claudacious.tilth.world.SoilService;
import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SnowyDirtBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * Replaces vanilla dirt and grass in each new chunk with a soil catena. For every column it measures
 * slope, curvature, local relief (13x13 window, read from the 3x3 chunk region) and landscape relief
 * (noise-generator heights within ~80 blocks), classifies the hillslope position, picks a parent
 * material for the region, and lays down the resulting profile: topsoil, subsoil or gley, with
 * thickness and texture set by position (thin and sandy on shoulders, deep, clayey and gleyed in
 * hollows, peat in cold wet basins).
 */
public class CatenaSoilFeature extends Feature<NoneFeatureConfiguration> {
    private static final int MARGIN = 16;
    private static final int SIZE = 16 + 2 * MARGIN;

    public CatenaSoilFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        if (!TilthConfig.CATENA_WORLDGEN.getAsBoolean()) {
            return false;
        }
        WorldGenLevel region = context.level();
        ServerLevel level = region.getLevel();
        ChunkPos chunk = new ChunkPos(context.origin());
        int x0 = chunk.getMinBlockX();
        int z0 = chunk.getMinBlockZ();
        int ox = x0 - MARGIN;
        int oz = z0 - MARGIN;

        int[] heights = new int[SIZE * SIZE];
        boolean[] wet = new boolean[SIZE * SIZE];
        for (int i = 0; i < SIZE; i++) {
            for (int j = 0; j < SIZE; j++) {
                heights[i * SIZE + j] = terrainHeight(region, ox + i, oz + j, wet, i * SIZE + j);
            }
        }
        CatenaAnalyzer.HeightField field = (x, z) -> {
            int i = Math.max(0, Math.min(SIZE - 1, x - ox));
            int j = Math.max(0, Math.min(SIZE - 1, z - oz));
            return heights[i * SIZE + j];
        };

        ChunkGenerator generator = context.chunkGenerator();
        RandomState randomState = level.getChunkSource().randomState();
        double m00 = MacroTerrain.mean(level, generator, randomState, region, x0, z0);
        double m10 = MacroTerrain.mean(level, generator, randomState, region, x0 + 16, z0);
        double m01 = MacroTerrain.mean(level, generator, randomState, region, x0, z0 + 16);
        double m11 = MacroTerrain.mean(level, generator, randomState, region, x0 + 16, z0 + 16);

        long seed = level.getSeed();
        int seaLevel = level.getSeaLevel();
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        for (int dx = 0; dx < 16; dx++) {
            for (int dz = 0; dz < 16; dz++) {
                int x = x0 + dx;
                int z = z0 + dz;
                int idx = (dx + MARGIN) * SIZE + (dz + MARGIN);
                int h = heights[idx];
                m.set(x, h, z);
                BlockState top = region.getBlockState(m);
                if (!isConvertible(top)) {
                    continue;
                }
                double mean = Noise.lerp(Noise.lerp(m00, m10, dx / 16.0), Noise.lerp(m01, m11, dx / 16.0), dz / 16.0);
                boolean nearWater = wet[idx] || waterNear(wet, dx + MARGIN, dz + MARGIN);
                CatenaAnalyzer.Result catena = CatenaAnalyzer.analyze(field, x, z, h - mean, nearWater, seaLevel);
                Holder<Biome> biome = region.getBiome(m);
                SoilRegion soilRegion = SoilService.region(biome);
                BiomeClimate climate = ClimateService.climateOf(biome);
                ParentMaterial pm = ParentMaterial.pick(soilRegion, SoilService.parentMaterialNoise(seed, x, z));
                SoilProfile profile = SoilProfile.build(pm, soilRegion, catena.position(), climate,
                        SoilService.sandJitter(seed, x, z), SoilService.clayJitter(seed, x, z), SoilService.depthNoise(seed, x, z));
                placeProfile(region, m.immutable(), top, profile);
            }
        }
        return true;
    }

    private static boolean isConvertible(BlockState s) {
        return s.is(Blocks.GRASS_BLOCK) || s.is(Blocks.DIRT);
    }

    private static boolean isReplaceableBelow(BlockState s) {
        return s.is(Blocks.DIRT) || s.is(Blocks.COARSE_DIRT) || s.is(Blocks.ROOTED_DIRT);
    }

    private static void placeProfile(WorldGenLevel region, BlockPos surface, BlockState top, SoilProfile p) {
        boolean grassTop = top.is(Blocks.GRASS_BLOCK);
        boolean snowy = grassTop && top.getValue(SnowyDirtBlock.SNOWY);
        BlockPos.MutableBlockPos m = surface.mutable();
        int topDepth = p.topsoilDepth();
        int maxDepth = topDepth + p.subsoilDepth() + 3;
        for (int d = 0; d < maxDepth; d++) {
            BlockState current = region.getBlockState(m);
            if (d == 0 ? !isConvertible(current) : !isReplaceableBelow(current)) {
                break;
            }
            BlockState place;
            if (d < topDepth) {
                place = (d == 0 && grassTop)
                        ? TilthBlocks.grass(p.topsoil()).setValue(SnowyDirtBlock.SNOWY, snowy)
                        : TilthBlocks.topsoil(p.topsoil());
            } else if (p.gleyed()) {
                place = TilthBlocks.GLEY.get().defaultBlockState();
            } else {
                place = TilthBlocks.subsoil(p.subsoil());
            }
            region.setBlock(m, place, 2);
            m.move(Direction.DOWN);
        }
    }

    /** Top of the ground, skipping trees, plants, snow, ice and water. Marks columns under water. */
    private static int terrainHeight(WorldGenLevel region, int x, int z, boolean[] wet, int idx) {
        int y = region.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z) - 1;
        int min = Math.max(region.getMinBuildHeight(), y - 64);
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos(x, y, z);
        while (y > min) {
            m.setY(y);
            BlockState s = region.getBlockState(m);
            if (!s.getFluidState().isEmpty()) {
                wet[idx] = true;
            } else if (s.blocksMotion() && !s.is(BlockTags.LOGS) && !s.is(BlockTags.LEAVES) && !s.is(BlockTags.ICE)
                    && !s.is(Blocks.CACTUS) && !s.is(Blocks.BAMBOO) && !s.is(Blocks.PUMPKIN) && !s.is(Blocks.MELON)
                    && !s.is(Blocks.BROWN_MUSHROOM_BLOCK) && !s.is(Blocks.RED_MUSHROOM_BLOCK) && !s.is(Blocks.MUSHROOM_STEM)
                    && !s.is(Blocks.BEE_NEST) && !s.is(Blocks.MOSS_BLOCK)) {
                return y;
            }
            y--;
        }
        return y;
    }

    private static boolean waterNear(boolean[] wet, int i, int j) {
        for (int di = -4; di <= 4; di++) {
            for (int dj = -4; dj <= 4; dj++) {
                int a = i + di;
                int b = j + dj;
                if (a >= 0 && b >= 0 && a < SIZE && b < SIZE && wet[a * SIZE + b]) {
                    return true;
                }
            }
        }
        return false;
    }
}
