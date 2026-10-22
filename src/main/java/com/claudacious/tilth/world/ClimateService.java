package com.claudacious.tilth.world;

import com.claudacious.tilth.TilthConfig;
import com.claudacious.tilth.sim.climate.BiomeClimate;
import com.claudacious.tilth.sim.climate.BiomeClimateTable;
import com.claudacious.tilth.sim.climate.DailyWeather;
import com.claudacious.tilth.sim.climate.WeatherGenerator;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.biome.Biome;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Climate normals and daily weather for a position in the world. */
public final class ClimateService {
    private static final Map<Biome, BiomeClimate> CACHE = new ConcurrentHashMap<>();

    private ClimateService() {
    }

    public static BiomeClimate climateOf(Holder<Biome> biome) {
        return CACHE.computeIfAbsent(biome.value(), b -> {
            String id = biome.unwrapKey().map(k -> k.location().toString()).orElse("");
            return BiomeClimateTable.lookup(id).orElseGet(() -> {
                Biome.ClimateSettings cs = b.getModifiedClimateSettings();
                return BiomeClimateTable.fallback(cs.temperature(), cs.downfall(), cs.hasPrecipitation());
            });
        });
    }

    public static BiomeClimate climateAt(ServerLevel level, BlockPos pos) {
        return climateOf(level.getBiome(pos));
    }

    /** Degrees C of cooling from elevation above sea level and (optionally) latitude. */
    public static double altitudeCooling(ServerLevel level, BlockPos pos) {
        double lapse = TilthConfig.LAPSE_RATE.getAsDouble() * Math.max(0, pos.getY() - level.getSeaLevel());
        double latitude = TilthConfig.LATITUDE_GRADIENT.getAsDouble() * Math.abs(pos.getZ()) / 1000.0;
        return lapse + latitude;
    }

    public static DailyWeather weather(ServerLevel level, BlockPos pos, BiomeClimate climate, long day) {
        boolean observed = TilthCalendar.rainedOn(level, day) && level.getBiome(pos).value().hasPrecipitation();
        return WeatherGenerator.generate(level.getSeed(), climate, day, pos.getX(), pos.getZ(), altitudeCooling(level, pos), observed);
    }

    public static DailyWeather today(ServerLevel level, BlockPos pos) {
        return weather(level, pos, climateAt(level, pos), TilthCalendar.today(level));
    }

    public static void clearCache() {
        CACHE.clear();
    }
}
