package com.claudacious.tilth.sim;

/**
 * Tunables read by the pure simulation code. The mod copies its TOML config into these fields
 * whenever the config loads or reloads, which keeps the simulation free of Minecraft and NeoForge
 * types and therefore unit-testable.
 */
public final class SimSettings {
    private SimSettings() {
    }

    // Crops
    public static volatile double growthRateMultiplier = 1.0;
    public static volatile double yieldMultiplier = 1.0;
    public static volatile boolean frostKillsCrops = true;
    public static volatile boolean droughtKillsCrops = true;
    public static volatile boolean waterloggingKillsCrops = true;
    public static volatile double overripeDaysBeforeDeath = 45.0;

    // Soil
    public static volatile double mineralizationMultiplier = 1.0;
    public static volatile double leachingMultiplier = 1.0;
    public static volatile double nutrientDemandMultiplier = 1.0;
    public static volatile double phDriftMultiplier = 1.0;
    public static volatile double rootZoneDepthMeters = 0.6;

    // Climate
    public static volatile double temperatureOffset = 0.0;
    public static volatile double precipitationMultiplier = 1.0;
    public static volatile double evapotranspirationMultiplier = 1.0;
    public static volatile double latitudeDegrees = 40.0;

    // Afflictions (diseases, pests, fungi) are modelled but inactive by default.
    public static volatile boolean afflictionsEnabled = false;
    public static volatile double afflictionPressureMultiplier = 1.0;
}
