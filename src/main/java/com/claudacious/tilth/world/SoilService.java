package com.claudacious.tilth.world;

import com.claudacious.tilth.block.SoilLike;
import com.claudacious.tilth.sim.Noise;
import com.claudacious.tilth.sim.catena.CatenaAnalyzer;
import com.claudacious.tilth.sim.catena.ParentMaterial;
import com.claudacious.tilth.sim.catena.SoilProfile;
import com.claudacious.tilth.sim.catena.SoilRegion;
import com.claudacious.tilth.sim.climate.BiomeClimate;
import com.claudacious.tilth.sim.soil.SoilBaseline;
import com.claudacious.tilth.sim.soil.SoilHorizon;
import com.claudacious.tilth.sim.soil.SoilState;
import com.claudacious.tilth.sim.soil.SoilTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.Tags;

import java.util.Optional;

/** Soil inference: which soil belongs at a place, and what its undisturbed chemistry is. */
public final class SoilService {
    private SoilService() {
    }

    public static SoilRegion region(Holder<Biome> biome) {
        if (biome.is(BiomeTags.IS_OCEAN) || biome.is(BiomeTags.IS_DEEP_OCEAN)) {
            return SoilRegion.OCEAN;
        }
        if (biome.is(BiomeTags.IS_RIVER)) {
            return SoilRegion.RIVER;
        }
        if (biome.is(BiomeTags.IS_BEACH) || biome.is(Tags.Biomes.IS_STONY_SHORES)) {
            return SoilRegion.BEACH;
        }
        if (biome.is(BiomeTags.IS_BADLANDS)) {
            return SoilRegion.BADLANDS;
        }
        if (biome.is(Tags.Biomes.IS_DESERT)) {
            return SoilRegion.DESERT;
        }
        if (biome.is(Tags.Biomes.IS_SWAMP) || biome.is(Biomes.MANGROVE_SWAMP)) {
            return SoilRegion.SWAMP;
        }
        if (biome.is(BiomeTags.IS_JUNGLE)) {
            return SoilRegion.JUNGLE;
        }
        if (biome.is(BiomeTags.IS_SAVANNA)) {
            return SoilRegion.SAVANNA;
        }
        if (biome.is(Tags.Biomes.IS_MUSHROOM)) {
            return SoilRegion.MUSHROOM;
        }
        if (biome.is(Tags.Biomes.IS_SNOWY_PLAINS) || biome.is(Biomes.ICE_SPIKES)) {
            return SoilRegion.TUNDRA;
        }
        if (biome.is(BiomeTags.IS_TAIGA)) {
            return SoilRegion.BOREAL;
        }
        if (biome.is(Tags.Biomes.IS_MOUNTAIN_PEAK) || biome.is(Tags.Biomes.IS_MOUNTAIN_SLOPE) || biome.is(Tags.Biomes.IS_WINDSWEPT)) {
            return SoilRegion.MOUNTAIN;
        }
        if (biome.is(BiomeTags.IS_FOREST)) {
            return SoilRegion.TEMPERATE_FOREST;
        }
        if (biome.is(Tags.Biomes.IS_PLAINS) || biome.is(Biomes.MEADOW) || biome.is(Biomes.CHERRY_GROVE)) {
            return SoilRegion.GRASSLAND;
        }
        BiomeClimate c = ClimateService.climateOf(biome);
        if (c.annualPrecip() < 300) {
            return SoilRegion.DESERT;
        }
        if (c.meanTemp() < -2) {
            return SoilRegion.TUNDRA;
        }
        if (c.meanTemp() < 4) {
            return SoilRegion.BOREAL;
        }
        if (c.meanTemp() > 22) {
            return c.annualPrecip() > 1500 ? SoilRegion.JUNGLE : SoilRegion.SAVANNA;
        }
        return c.annualPrecip() > 900 ? SoilRegion.TEMPERATE_FOREST : SoilRegion.GRASSLAND;
    }

    /** Smooth 0-1 noise that picks the parent material; patches are several hundred blocks across. */
    public static double parentMaterialNoise(long seed, int x, int z) {
        return Noise.clamp(0.5 + 0.9 * Noise.fbm(seed ^ 0x504152454E54L, x / 700.0, z / 700.0, 3), 0, 0.999);
    }

    public static double sandJitter(long seed, int x, int z) {
        return Noise.value(seed ^ 0x53414E44L, x / 9.0, z / 9.0) * 5.0;
    }

    public static double clayJitter(long seed, int x, int z) {
        return Noise.value(seed ^ 0x434C4159L, x / 11.0, z / 11.0) * 3.0;
    }

    public static int depthNoise(long seed, int x, int z) {
        double u = Noise.unit(seed ^ 0x44455054L, x, z);
        return u < 0.2 ? -1 : (u > 0.8 ? 1 : 0);
    }

    public static SoilProfile profile(ServerLevel level, BlockPos pos, CatenaAnalyzer.Result catena) {
        Holder<Biome> biome = level.getBiome(pos);
        long seed = level.getSeed();
        SoilRegion region = region(biome);
        ParentMaterial pm = ParentMaterial.pick(region, parentMaterialNoise(seed, pos.getX(), pos.getZ()));
        return SoilProfile.build(pm, region, catena.position(), ClimateService.climateOf(biome),
                sandJitter(seed, pos.getX(), pos.getZ()), clayJitter(seed, pos.getX(), pos.getZ()), depthNoise(seed, pos.getX(), pos.getZ()));
    }

    public static SoilProfile profile(ServerLevel level, BlockPos pos) {
        return profile(level, pos, TerrainService.analyze(level, pos));
    }

    /** Texture that would have generated here; used when tilling vanilla dirt in old chunks. */
    public static SoilTexture guessTexture(ServerLevel level, BlockPos pos) {
        return profile(level, pos).topsoil();
    }

    public static SoilState baseline(ServerLevel level, BlockPos pos, SoilTexture texture, SoilHorizon horizon) {
        CatenaAnalyzer.Result catena = TerrainService.analyze(level, pos);
        SoilProfile profile = profile(level, pos, catena);
        double noise = Noise.value(level.getSeed() ^ 0x534F494CL, pos.getX() / 7.0, pos.getZ() / 7.0);
        return SoilBaseline.create(texture, horizon, catena.position(), profile.drainage(), ClimateService.climateAt(level, pos), noise);
    }

    public static Optional<SoilLike> soilOf(BlockState state) {
        return state.getBlock() instanceof SoilLike s ? Optional.of(s) : Optional.empty();
    }
}
