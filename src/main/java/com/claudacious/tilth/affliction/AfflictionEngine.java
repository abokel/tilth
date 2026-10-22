package com.claudacious.tilth.affliction;

import com.claudacious.tilth.registry.TilthMisc;
import com.claudacious.tilth.sim.FieldSimulator;
import com.claudacious.tilth.sim.Noise;
import com.claudacious.tilth.sim.SimSettings;
import com.claudacious.tilth.sim.calendar.CalendarMath;
import com.claudacious.tilth.sim.climate.ClimateMath;
import com.claudacious.tilth.sim.climate.DailyWeather;
import com.claudacious.tilth.sim.crop.CropSpecies;
import com.claudacious.tilth.sim.crop.CropState;
import com.claudacious.tilth.sim.soil.Hydraulics;
import com.claudacious.tilth.sim.soil.SoilState;
import com.claudacious.tilth.sim.soil.SoilTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

/**
 * Infection risk and severity progression. Risk = environmental suitability x inoculum pressure x
 * host susceptibility. Inoculum rises when the same host was grown recently in the field (residue
 * and soil-borne organisms) and falls with ploughing; severity grows logistically while conditions
 * stay favourable.
 */
public final class AfflictionEngine {
    private AfflictionEngine() {
    }

    @Nullable
    public static Registry<Affliction> registry(ServerLevel level) {
        return level.registryAccess().registry(TilthMisc.AFFLICTIONS).orElse(null);
    }

    /** Suitability of today's conditions for the organism, 0-1. */
    public static double suitability(Affliction a, DailyWeather w, SoilState soil, Hydraulics h) {
        Affliction.Envelope e = a.envelope();
        double fT = ClimateMath.temperatureResponse(w.tMean(), e.tempMin(), e.tempOpt(), e.tempMax());
        double fH = e.minHumidity() <= 0 ? 1.0 : Noise.clamp((w.relativeHumidity() - e.minHumidity()) / 15.0 + 0.5, 0, 1);
        double fWet = e.leafWetnessHours() <= 0 ? 1.0 : Noise.clamp(w.leafWetnessHours() / e.leafWetnessHours(), 0, 1);
        double rel = (soil.moisture - h.wiltingPoint()) / Math.max(0.01, h.fieldCapacity() - h.wiltingPoint());
        double fSoil = rel >= e.soilMoistureMin() && rel <= e.soilMoistureMax() ? 1.0 : 0.3;
        double fPh = soil.ph >= e.phMin() && soil.ph <= e.phMax() ? 1.0 : 0.25;
        return fT * fH * fWet * fSoil * fPh;
    }

    /** Inoculum pressure from field history and tillage, 0.02 (clean) to ~1. */
    public static double inoculum(Affliction a, CropSpecies host, SoilState soil, int year) {
        double pressure = 0.02;
        boolean persistsInField = a.transmission().contains(Affliction.Transmission.SOILBORNE)
                || a.transmission().contains(Affliction.Transmission.CROP_RESIDUE);
        if (persistsInField) {
            int years = (int) Math.ceil(Math.max(1, a.lifecycle().survivalYears()));
            int recent = soil.timesGrownWithin(host.id, year, years);
            pressure += 0.3 * recent;
            pressure *= 1.0 - a.control().tillageReduction() * soil.incorporatedFraction;
        }
        if (a.transmission().contains(Affliction.Transmission.AIRBORNE_SPORES) || a.transmission().contains(Affliction.Transmission.MIGRATION)) {
            pressure += 0.08;
        }
        return Noise.clamp(pressure, 0.0, 1.0);
    }

    /** Daily hook used by the field simulation when afflictions are enabled. */
    @Nullable
    public static FieldSimulator.AfflictionHook hook(ServerLevel level, BlockPos pos, SoilState soil) {
        if (!SimSettings.afflictionsEnabled) {
            return null;
        }
        Registry<Affliction> registry = registry(level);
        if (registry == null) {
            return null;
        }
        RandomSource random = level.random;
        return (species, crop, s, weather) -> step(registry, species, crop, s, weather, random);
    }

    private static void step(Registry<Affliction> registry, CropSpecies species, CropState crop, SoilState soil, DailyWeather w,
                             RandomSource random) {
        int stage = species.visualStage(crop);
        int year = CalendarMath.year(w.day());
        SoilTexture tex = SoilTexture.LOAM;
        Hydraulics h = tex.hydraulics(soil.organicMatter);
        double growth = 1.0;
        double yield = 1.0;
        double quality = 1.0;
        for (Map.Entry<net.minecraft.resources.ResourceKey<Affliction>, Affliction> entry : registry.entrySet()) {
            Affliction a = entry.getValue();
            if (a.beneficial() || !a.infects(species) || stage < a.impact().fromStage() || stage > a.impact().toStage()) {
                continue;
            }
            String id = entry.getKey().location().toString();
            double suit = suitability(a, w, soil, h);
            Double severity = crop.afflictions.get(id);
            if (severity == null) {
                double p = suit * inoculum(a, species, soil, year) * a.lifecycle().spreadRate() * 0.2 * SimSettings.afflictionPressureMultiplier;
                if (random.nextDouble() < p) {
                    crop.afflictions.put(id, 0.02);
                }
                continue;
            }
            double rate = a.lifecycle().spreadRate() * (suit - 0.2);
            double next = Noise.clamp(severity + severity * (1.0 - severity) * rate, 0.0, 1.0);
            crop.afflictions.put(id, next);
            growth *= 1.0 - next * a.impact().growthPenalty();
            yield *= 1.0 - next * a.impact().maxYieldLoss();
            quality *= 1.0 - next * a.impact().qualityPenalty();
            if (a.impact().canKillHost() && next > 0.95) {
                crop.kill("destroyed by " + a.name());
            }
        }
        crop.afflictionGrowthFactor = growth;
        crop.afflictionYieldFactor = Math.min(crop.afflictionYieldFactor, yield);
        crop.afflictionQualityFactor = quality;
    }

    public static String displayId(ResourceLocation id) {
        return id.getNamespace().equals("tilth") ? id.getPath() : id.toString();
    }
}
