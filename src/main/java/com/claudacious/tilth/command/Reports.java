package com.claudacious.tilth.command;

import com.claudacious.tilth.TilthConfig;
import com.claudacious.tilth.block.CropBlockEntity;
import com.claudacious.tilth.block.FarmlandBlockEntity;
import com.claudacious.tilth.block.SoilLike;
import com.claudacious.tilth.block.TilthCropBlock;
import com.claudacious.tilth.quality.Quality;
import com.claudacious.tilth.sim.calendar.CalendarMath;
import com.claudacious.tilth.sim.catena.CatenaAnalyzer;
import com.claudacious.tilth.sim.catena.SoilProfile;
import com.claudacious.tilth.sim.climate.BiomeClimate;
import com.claudacious.tilth.sim.climate.ClimateMath;
import com.claudacious.tilth.sim.climate.DailyWeather;
import com.claudacious.tilth.sim.crop.Bbch;
import com.claudacious.tilth.sim.crop.CropGrowthModel;
import com.claudacious.tilth.sim.crop.CropSpecies;
import com.claudacious.tilth.sim.crop.CropState;
import com.claudacious.tilth.sim.soil.Hydraulics;
import com.claudacious.tilth.sim.soil.Nutrient;
import com.claudacious.tilth.sim.soil.SoilHorizon;
import com.claudacious.tilth.sim.soil.SoilState;
import com.claudacious.tilth.sim.soil.SoilTexture;
import com.claudacious.tilth.water.SurfaceWater;
import com.claudacious.tilth.water.WaterSourceRules;
import com.claudacious.tilth.world.ClimateService;
import com.claudacious.tilth.world.SoilService;
import com.claudacious.tilth.world.TerrainService;
import com.claudacious.tilth.world.TilthCalendar;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;

/** Human-readable reports shared by commands, the soil probe and the debug HUD. */
public final class Reports {
    private Reports() {
    }

    public static Component header(String title) {
        return Component.literal("=== " + title + " ===").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD);
    }

    public static Component line(String label, String value) {
        MutableComponent c = Component.literal(label + ": ").withStyle(ChatFormatting.GRAY);
        return c.append(Component.literal(value).withStyle(ChatFormatting.WHITE));
    }

    public static Component note(String text) {
        return Component.literal(text).withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC);
    }

    // ------------------------------------------------------------------ ratings (typical extension ranges)

    static String rateN(double ppm) {
        return ppm < 10 ? "low" : ppm < 20 ? "medium" : ppm < 40 ? "optimum" : "high";
    }

    static String rateP(double ppm) {
        return ppm < 10 ? "low" : ppm < 20 ? "medium" : ppm < 40 ? "optimum" : "high";
    }

    static String rateK(double ppm) {
        return ppm < 90 ? "low" : ppm < 130 ? "medium" : ppm < 200 ? "optimum" : "high";
    }

    static String ratePh(double ph) {
        if (ph < 5.0) {
            return "strongly acid";
        }
        if (ph < 5.5) {
            return "acid";
        }
        if (ph < 6.5) {
            return "slightly acid";
        }
        if (ph < 7.3) {
            return "neutral";
        }
        return ph < 8.0 ? "alkaline" : "strongly alkaline";
    }

    // ------------------------------------------------------------------ probe dispatch

    public static List<Component> probe(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        List<Component> out = new ArrayList<>();
        if (state.getBlock() instanceof TilthCropBlock) {
            out.addAll(crop(level, pos));
            out.addAll(soil(level, pos.below()));
            return out;
        }
        return soil(level, pos);
    }

    public static List<Component> soil(ServerLevel level, BlockPos pos) {
        List<Component> out = new ArrayList<>();
        BlockState state = level.getBlockState(pos);
        if (level.getBlockEntity(pos) instanceof FarmlandBlockEntity farmland) {
            farmland.advance(level);
            SoilTexture tex = farmland.texture();
            out.add(header("Soil test: " + tex.displayName() + " farmland"));
            describeState(out, tex, farmland.soil());
            out.add(line("Field", String.format("slope %.0f°, irrigation seepage %.0f%%, last ponding %.1f mm",
                    farmland.slopeDegrees(), farmland.lastIrrigation() * 100, farmland.lastPonding())));
            SoilState s = farmland.soil();
            if (!s.cropHistory.isEmpty()) {
                out.add(line("Crop history", String.join(", ", s.cropHistory)));
            }
            return out;
        }
        if (state.getBlock() instanceof SoilLike soil) {
            SoilTexture tex = soil.texture();
            out.add(header(state.getBlock().getName().getString() + " (undisturbed " + soil.horizon().id() + " estimate)"));
            describeState(out, tex, SoilService.baseline(level, pos, tex, soil.horizon()));
            out.add(note("Till it to start tracking this soil."));
            return out;
        }
        SoilProfile profile = SoilService.profile(level, pos);
        out.add(header("Not a Tilth soil: " + state.getBlock().getName().getString()));
        out.add(line("If tilled it would become", profile.topsoil().displayName() + " (" + profile.position().displayName() + ")"));
        return out;
    }

    private static void describeState(List<Component> out, SoilTexture tex, SoilState s) {
        Hydraulics h = tex.hydraulics(s.organicMatter);
        out.add(line("Texture", String.format("%d%% sand / %d%% silt / %d%% clay, hydrologic group %c",
                tex.sand(), tex.silt(), tex.clay(), tex.hydrologicGroup())));
        out.add(line("pH", String.format("%.1f (%s) | organic matter %.1f%% | CEC %.1f cmol/kg",
                s.ph, ratePh(s.ph), s.organicMatter, tex.cec(s.organicMatter))));
        out.add(line("N-P-K", String.format("N %.0f ppm (%s) | P %.0f ppm (%s) | K %.0f ppm (%s)",
                s.nitrogen, rateN(s.nitrogen), s.phosphorus, rateP(s.phosphorus), s.potassium, rateK(s.potassium))));
        out.add(line("Secondary", String.format("S %.0f | Ca %.0f | Mg %.0f ppm | micronutrient availability %.0f%%",
                s.sulfur, s.calcium, s.magnesium, 100 * Nutrient.micronutrientAvailability(s.ph))));
        double avail = (s.moisture - h.wiltingPoint()) / Math.max(0.01, h.fieldCapacity() - h.wiltingPoint());
        out.add(line("Water", String.format("θ %.2f (FC %.2f, WP %.2f) = %.0f%% of available | Ksat %.1f mm/h | water table %.1f m",
                s.moisture, h.fieldCapacity(), h.wiltingPoint(), 100 * avail, h.ksat(), s.waterTableDepth)));
        out.add(line("Structure", String.format("compaction %.0f%% | tilth %.0f%% | residue %.2f kg/m² (N %.1f%%) | snow %.0f mm",
                100 * s.compaction, 100 * s.tilth, s.residue, s.residueNitrogen, s.snowpack)));
    }

    public static List<Component> crop(ServerLevel level, BlockPos pos) {
        List<Component> out = new ArrayList<>();
        if (!(level.getBlockEntity(pos) instanceof CropBlockEntity crop)) {
            out.add(Component.literal("No crop here.").withStyle(ChatFormatting.RED));
            return out;
        }
        if (level.getBlockEntity(pos.below()) instanceof FarmlandBlockEntity farmland) {
            farmland.advance(level);
        }
        if (!(level.getBlockEntity(pos) instanceof CropBlockEntity current)) {
            out.add(Component.literal("The crop died and was removed.").withStyle(ChatFormatting.RED));
            return out;
        }
        CropSpecies sp = current.species();
        CropState cs = current.cropState();
        int stage = sp.visualStage(cs);
        out.add(header(sp.name + " (" + sp.scientificName + ")"));
        out.add(line("Stage", Bbch.describe(sp.bbchFor(stage)) + " [visual " + stage + "/9]"));
        if (!cs.germinated) {
            out.add(line("Germination", String.format("%.0f / %.0f °C·d above %.1f °C", cs.germinationGdd, sp.germinationGdd, sp.tBase)));
        } else {
            out.add(line("Thermal time", String.format("%.0f / %.0f °C·d (%.0f%%), %d days since sowing",
                    cs.gdd, sp.gddToMaturity, 100 * Math.min(1, cs.dev), cs.daysSinceSowing)));
        }
        out.add(line("Growth", String.format("%.0f%% of potential today | limiting: %s | light %.0f%%",
                100 * cs.lastGrowthFactor, cs.limitingFactor, 100 * cs.lightFactor)));
        StringBuilder suff = new StringBuilder();
        for (Nutrient n : Nutrient.values()) {
            suff.append(n.symbol()).append(String.format(" %.2f ", cs.sufficiency[n.ordinal()]));
        }
        out.add(line("Sufficiency", String.format("water %.2f | %s", cs.waterFactor, suff.toString().trim())));
        double yf = CropGrowthModel.yieldFactor(cs);
        double perAcre = sp.yieldUnit.perAcre(sp.referenceYield / 10.0 * yf, sp.unitWeightLb);
        out.add(line("Outlook", String.format("yield potential %.0f%% (~%.1f %s), quality %s",
                100 * yf, perAcre, sp.yieldUnit.label(), Quality.name(CropGrowthModel.grade(cs)))));
        if (sp.isLegume()) {
            out.add(line("Nitrogen fixed", String.format("%.0f kg/ha", cs.fixedNitrogen)));
        }
        if (cs.frostDamage > 0) {
            out.add(line("Frost damage", String.format("%.0f%%", 100 * cs.frostDamage)));
        }
        out.add(line("Afflictions", cs.afflictions.isEmpty() ? "none" : cs.afflictions.toString()));
        out.add(line("Needs", String.format("%.0f-%.0f °C (opt %.0f), pH %.1f-%.1f, frost-killed below %.0f °C",
                sp.tBase, sp.tMax, sp.tOpt, sp.phLow, sp.phHigh, sp.frostKillVegetative)));
        return out;
    }

    public static List<Component> calendar(ServerLevel level) {
        List<Component> out = new ArrayList<>();
        CalendarMath math = TilthCalendar.math();
        double day = TilthCalendar.realDay(level);
        int doy = CalendarMath.dayOfYear(day);
        out.add(header("Calendar"));
        out.add(line("Date", TilthCalendar.describeToday(level)));
        out.add(line("Scale", String.format("%d game days per month, 1 game day = %.2f calendar days, year = %d game days",
                math.daysPerMonth(), math.realDaysPerGameDay(), math.gameDaysPerYear())));
        out.add(line("Day of year", (doy + 1) + " / 365, " + CalendarMath.season(doy).displayName()));
        out.add(line("Rain recorded today", TilthCalendar.rainedOn(level, (long) Math.floor(day)) ? "yes" : "no"));
        return out;
    }

    public static List<Component> climate(ServerLevel level, BlockPos pos) {
        List<Component> out = new ArrayList<>();
        BiomeClimate c = ClimateService.climateAt(level, pos);
        DailyWeather w = ClimateService.today(level, pos);
        String biome = level.getBiome(pos).unwrapKey().map(k -> k.location().toString()).orElse("?");
        double zone = ClimateMath.hardinessZone(c);
        out.add(header("Climate at " + pos.toShortString()));
        out.add(line("Biome", biome + " (" + c.describe() + ")"));
        out.add(line("Normals", String.format("mean %.1f °C, annual swing ±%.1f °C, %.0f mm/yr, USDA zone %s",
                c.meanTemp(), c.amplitude(), c.annualPrecip(), ClimateMath.hardinessLabel(zone))));
        out.add(line("Elevation/latitude cooling", String.format("%.1f °C", ClimateService.altitudeCooling(level, pos))));
        out.add(line("Today", String.format("%s | %.1f to %.1f °C | rain %.1f mm, snow %.1f mm | ET0 %.1f mm | RH %.0f%% | leaf wetness %.0f h",
                TilthCalendar.describeToday(level), w.tMin(), w.tMax(), w.rain(), w.snow(), w.et0(), w.relativeHumidity(), w.leafWetnessHours())));
        out.add(line("Degree days today", String.format("base 0: %.1f | base 5: %.1f | base 10: %.1f",
                ClimateMath.growingDegreeDays(w.tMin(), w.tMax(), 0, 30), ClimateMath.growingDegreeDays(w.tMin(), w.tMax(), 5, 30),
                ClimateMath.growingDegreeDays(w.tMin(), w.tMax(), 10, 30))));
        out.add(line("Soil equilibrium pH", String.format("%.1f", c.equilibriumPh())));
        return out;
    }

    public static List<Component> catena(ServerLevel level, BlockPos pos) {
        List<Component> out = new ArrayList<>();
        CatenaAnalyzer.Result r = TerrainService.analyze(level, pos);
        SoilProfile p = SoilService.profile(level, pos, r);
        out.add(header("Catena analysis at " + pos.getX() + ", " + pos.getZ()));
        out.add(line("Hillslope position", r.position().displayName()));
        String shape = Math.abs(r.curvature()) < 0.15 ? "planar" : (r.curvature() > 0 ? "concave, collects water" : "convex, sheds water");
        out.add(line("Terrain", String.format("surface y=%d | slope %.1f° | local relief %+.1f m | curvature %+.2f (%s) | landscape relief %+.1f m",
                r.height(), r.slopeDegrees(), r.localRelief(), r.curvature(), shape, r.macroRelief())));
        out.add(line("Soil region", SoilService.region(level.getBiome(pos)).name().toLowerCase() + ", parent material " + p.parentMaterial().displayName()));
        out.add(line("Profile", String.format("%s topsoil %d m over %s%s subsoil %d m",
                p.topsoil().displayName(), p.topsoilDepth(), p.gleyed() ? "gleyed " : "", p.subsoil().displayName(), p.subsoilDepth())));
        out.add(line("Drainage", p.drainage().displayName() + String.format(", typical water table %.1f m", p.drainage().waterTableDepth())));
        return out;
    }

    public static List<Component> water(ServerLevel level, BlockPos pos) {
        List<Component> out = new ArrayList<>();
        out.add(header("Water at " + pos.toShortString()));
        out.add(line("Source creation", TilthConfig.SOURCE_MODE.get().name() + (WaterSourceRules.isInfinite(level, pos)
                ? " (this biome counts as an infinite water body)" : " (finite water here)")));
        out.add(line("Ground permeability", String.format("%.0f%%", 100 * SurfaceWater.permeability(level, pos))));
        if (level.getBlockState(pos).getBlock() instanceof SoilLike soil) {
            out.add(line("Irrigation reach", soil.texture().irrigationReach() + " blocks (capillary seepage through " + soil.texture().displayName() + ")"));
        }
        if (level.getBlockEntity(pos) instanceof FarmlandBlockEntity farmland) {
            out.add(line("Water table", String.format("%.1f m", farmland.soil().waterTableDepth)));
            out.add(line("Last ponding", String.format("%.1f mm", farmland.lastPonding())));
        }
        DailyWeather w = ClimateService.today(level, pos);
        out.add(line("Today", String.format("rain %.1f mm, ET0 %.1f mm, %s", w.rain(), w.et0(), level.isRainingAt(pos.above()) ? "raining now" : "dry now")));
        return out;
    }

    /** One-line summary for the debug HUD. */
    public static Component hudLine(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (state.getBlock() instanceof TilthCropBlock && level.getBlockEntity(pos) instanceof CropBlockEntity crop) {
            CropState cs = crop.cropState();
            CropSpecies sp = crop.species();
            return Component.literal(String.format("%s BBCH %02d %.0f%% | grow %.0f%% | lim %s | Y %.0f%%",
                    sp.name, sp.bbchFor(sp.visualStage(cs)), 100 * Math.min(1, cs.dev), 100 * cs.lastGrowthFactor, cs.limitingFactor,
                    100 * CropGrowthModel.yieldFactor(cs))).withStyle(ChatFormatting.GREEN);
        }
        if (level.getBlockEntity(pos) instanceof FarmlandBlockEntity farmland) {
            SoilState s = farmland.soil();
            Hydraulics h = farmland.hydraulics();
            return Component.literal(String.format("%s | pH %.1f OM %.1f%% | N %.0f P %.0f K %.0f | θ %.2f/%.2f | comp %.0f%%",
                    farmland.texture().displayName(), s.ph, s.organicMatter, s.nitrogen, s.phosphorus, s.potassium,
                    s.moisture, h.fieldCapacity(), 100 * s.compaction)).withStyle(ChatFormatting.AQUA);
        }
        if (state.getBlock() instanceof SoilLike soil) {
            SoilHorizon horizon = soil.horizon();
            return Component.literal(soil.texture().displayName() + " " + horizon.id()).withStyle(ChatFormatting.YELLOW);
        }
        DailyWeather w = ClimateService.today(level, pos);
        return Component.literal(String.format("%s | %.0f-%.0f °C | rain %.1f mm", TilthCalendar.describeToday(level), w.tMin(), w.tMax(), w.rain()))
                .withStyle(ChatFormatting.GRAY);
    }
}
