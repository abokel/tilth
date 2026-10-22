package com.claudacious.tilth.water;

import com.claudacious.tilth.TilthConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.neoforged.neoforge.event.level.block.CreateFluidSourceEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * Finite water. Vanilla turns any gap between two water sources into a new source, which makes a
 * 2x1 hole an infinite well. Here that only happens where configured (oceans and rivers by default,
 * which are effectively infinite water bodies anyway).
 */
public final class WaterSourceRules {
    private static List<? extends String> parsedFrom;
    private static List<TagKey<Biome>> tags = List.of();
    private static List<ResourceKey<Biome>> ids = List.of();

    private WaterSourceRules() {
    }

    public static void onCreateSource(CreateFluidSourceEvent event) {
        if (!event.getFluidState().is(FluidTags.WATER)) {
            return;
        }
        Level level = event.getLevel();
        BlockPos pos = event.getPos();
        switch (TilthConfig.SOURCE_MODE.get()) {
            case VANILLA -> {
            }
            case NEVER -> event.setCanConvert(false);
            case INFINITE_BIOMES_ONLY -> {
                if (event.canConvert() && !isInfinite(level, pos)) {
                    event.setCanConvert(false);
                }
            }
            case INFINITE_BIOMES_BELOW_SEA_LEVEL -> {
                if (event.canConvert() && (!isInfinite(level, pos) || pos.getY() > level.getSeaLevel())) {
                    event.setCanConvert(false);
                }
            }
        }
    }

    /** True if the position lies in a biome configured as an effectively infinite water body. */
    public static boolean isInfinite(Level level, BlockPos pos) {
        refresh();
        Holder<Biome> biome = level.getBiome(pos);
        for (TagKey<Biome> tag : tags) {
            if (biome.is(tag)) {
                return true;
            }
        }
        for (ResourceKey<Biome> id : ids) {
            if (biome.is(id)) {
                return true;
            }
        }
        return false;
    }

    private static synchronized void refresh() {
        List<? extends String> raw = TilthConfig.INFINITE_BIOMES.get();
        if (raw == parsedFrom) {
            return;
        }
        List<TagKey<Biome>> newTags = new ArrayList<>();
        List<ResourceKey<Biome>> newIds = new ArrayList<>();
        for (String s : raw) {
            String trimmed = s.trim();
            if (trimmed.startsWith("#")) {
                ResourceLocation rl = ResourceLocation.tryParse(trimmed.substring(1));
                if (rl != null) {
                    newTags.add(TagKey.create(Registries.BIOME, rl));
                }
            } else {
                ResourceLocation rl = ResourceLocation.tryParse(trimmed);
                if (rl != null) {
                    newIds.add(ResourceKey.create(Registries.BIOME, rl));
                }
            }
        }
        tags = newTags;
        ids = newIds;
        parsedFrom = raw;
    }
}
