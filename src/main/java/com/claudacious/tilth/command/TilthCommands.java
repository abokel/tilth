package com.claudacious.tilth.command;

import com.claudacious.tilth.TilthConfig;
import com.claudacious.tilth.affliction.Affliction;
import com.claudacious.tilth.affliction.AfflictionEngine;
import com.claudacious.tilth.block.CropBlockEntity;
import com.claudacious.tilth.block.FarmlandBlockEntity;
import com.claudacious.tilth.block.Tilling;
import com.claudacious.tilth.diet.PlayerDiet;
import com.claudacious.tilth.plot.Plot;
import com.claudacious.tilth.plot.PlotData;
import com.claudacious.tilth.registry.TilthAttachments;
import com.claudacious.tilth.registry.TilthBlocks;
import com.claudacious.tilth.sim.calendar.CalendarMath;
import com.claudacious.tilth.sim.crop.CropSpecies;
import com.claudacious.tilth.sim.crop.Crops;
import com.claudacious.tilth.sim.diet.DietNutrient;
import com.claudacious.tilth.sim.plot.PolygonMath;
import com.claudacious.tilth.sim.soil.Amendment;
import com.claudacious.tilth.sim.soil.SoilChemistry;
import com.claudacious.tilth.sim.soil.SoilState;
import com.claudacious.tilth.sim.soil.Tillage;
import com.claudacious.tilth.water.SurfaceWater;
import com.claudacious.tilth.world.TilthCalendar;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

/**
 * /tilth: inspection commands for everyone, plus world-editing debug commands (permission level from
 * the config) for testing the simulation without waiting for seasons.
 */
public final class TilthCommands {
    private static final List<String> SOIL_PROPERTIES = List.of("n", "p", "k", "s", "ca", "mg", "ph", "om", "moisture", "compaction", "residue");

    private TilthCommands() {
    }

    private static boolean op(CommandSourceStack s) {
        return s.hasPermission(TilthConfig.DEBUG_PERMISSION.getAsInt());
    }

    public static void register(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> d = event.getDispatcher();
        LiteralArgumentBuilder<CommandSourceStack> root = literal("tilth");

        root.then(literal("calendar").executes(ctx -> send(ctx, Reports.calendar(ctx.getSource().getLevel()))));
        root.then(report("climate", Reports::climate));
        root.then(report("catena", Reports::catena));
        root.then(report("water", Reports::water)
                .then(literal("pond").requires(TilthCommands::op).executes(TilthCommands::forcePond)));
        root.then(report("soil", Reports::soil)
                .then(literal("set").requires(TilthCommands::op)
                        .then(argument("property", StringArgumentType.word())
                                .suggests((c, b) -> SharedSuggestionProvider.suggest(SOIL_PROPERTIES, b))
                                .then(argument("value", DoubleArgumentType.doubleArg())
                                        .executes(TilthCommands::soilSet))))
                .then(literal("amend").requires(TilthCommands::op)
                        .then(argument("amendment", StringArgumentType.word())
                                .suggests((c, b) -> SharedSuggestionProvider.suggest(Arrays.stream(Amendment.values())
                                        .map(a -> a.name().toLowerCase(Locale.ROOT)), b))
                                .executes(TilthCommands::soilAmend))));
        root.then(report("crop", Reports::crop)
                .then(literal("setstage").requires(TilthCommands::op)
                        .then(argument("stage", IntegerArgumentType.integer(0, 9)).executes(TilthCommands::cropSetStage)))
                .then(literal("grow").requires(TilthCommands::op)
                        .then(argument("days", IntegerArgumentType.integer(1, 3650)).executes(TilthCommands::cropGrow)))
                .then(literal("kill").requires(TilthCommands::op).executes(TilthCommands::cropKill)));
        root.then(literal("time").requires(TilthCommands::op)
                .then(literal("skip").then(argument("days", DoubleArgumentType.doubleArg(0.01, 36500)).executes(TilthCommands::timeSkip)))
                .then(literal("set").then(argument("month", IntegerArgumentType.integer(1, 12))
                        .then(argument("day", IntegerArgumentType.integer(1, 31)).executes(TilthCommands::timeSet)))));
        root.then(literal("debug").then(literal("hud").executes(ctx -> {
            boolean on = DebugHud.toggle(ctx.getSource().getPlayerOrException());
            ctx.getSource().sendSuccess(() -> Component.literal("Tilth debug HUD " + (on ? "on" : "off")), false);
            return 1;
        })));
        root.then(literal("diet").executes(TilthCommands::dietShow)
                .then(literal("set").requires(TilthCommands::op)
                        .then(argument("nutrient", StringArgumentType.word())
                                .suggests((c, b) -> SharedSuggestionProvider.suggest(Arrays.stream(DietNutrient.values()).map(DietNutrient::id), b))
                                .then(argument("value", DoubleArgumentType.doubleArg(0, 1)).executes(TilthCommands::dietSet)))));
        root.then(literal("plot")
                .then(literal("list").executes(TilthCommands::plotList))
                .then(literal("here").executes(ctx -> plotInfo(ctx, null)))
                .then(literal("info").then(argument("name", StringArgumentType.greedyString()).suggests(TilthCommands::suggestPlots)
                        .executes(ctx -> plotInfo(ctx, StringArgumentType.getString(ctx, "name")))))
                .then(literal("rename").then(argument("old", StringArgumentType.string()).suggests(TilthCommands::suggestPlots)
                        .then(argument("new", StringArgumentType.greedyString()).executes(TilthCommands::plotRename))))
                .then(literal("delete").then(argument("name", StringArgumentType.greedyString()).suggests(TilthCommands::suggestPlots)
                        .executes(TilthCommands::plotDelete)))
                .then(literal("addmember").then(argument("name", StringArgumentType.string()).suggests(TilthCommands::suggestPlots)
                        .then(argument("player", EntityArgument.player()).executes(ctx -> plotMember(ctx, true)))))
                .then(literal("removemember").then(argument("name", StringArgumentType.string()).suggests(TilthCommands::suggestPlots)
                        .then(argument("player", EntityArgument.player()).executes(ctx -> plotMember(ctx, false))))));
        root.then(literal("affliction")
                .then(literal("list").executes(TilthCommands::afflictionList))
                .then(literal("info").then(argument("id", ResourceLocationArgument.id()).suggests(TilthCommands::suggestAfflictions)
                        .executes(TilthCommands::afflictionInfo)))
                .then(literal("risk").executes(TilthCommands::afflictionRisk))
                .then(literal("infect").requires(TilthCommands::op).then(argument("id", ResourceLocationArgument.id())
                        .suggests(TilthCommands::suggestAfflictions).executes(TilthCommands::afflictionInfect)))
                .then(literal("clear").requires(TilthCommands::op).executes(TilthCommands::afflictionClear)));
        root.then(literal("fillfield").requires(TilthCommands::op)
                .then(argument("species", StringArgumentType.word())
                        .suggests((c, b) -> SharedSuggestionProvider.suggest(Crops.map().keySet(), b))
                        .then(argument("radius", IntegerArgumentType.integer(1, 16)).executes(TilthCommands::fillField))));
        d.register(root);
    }

    private static LiteralArgumentBuilder<CommandSourceStack> report(String name, java.util.function.BiFunction<ServerLevel, BlockPos, List<Component>> fn) {
        return literal(name)
                .executes(ctx -> send(ctx, fn.apply(ctx.getSource().getLevel(), target(ctx))))
                .then(argument("pos", BlockPosArgument.blockPos())
                        .executes(ctx -> send(ctx, fn.apply(ctx.getSource().getLevel(), BlockPosArgument.getLoadedBlockPos(ctx, "pos")))));
    }

    private static int send(CommandContext<CommandSourceStack> ctx, List<Component> lines) {
        for (Component c : lines) {
            ctx.getSource().sendSuccess(() -> c, false);
        }
        return lines.size();
    }

    private static int fail(CommandContext<CommandSourceStack> ctx, String message) {
        ctx.getSource().sendFailure(Component.literal(message));
        return 0;
    }

    /** The block the executing player looks at (8 blocks), else the block under them. */
    private static BlockPos target(CommandContext<CommandSourceStack> ctx) {
        if (ctx.getSource().getEntity() instanceof ServerPlayer player) {
            HitResult hit = player.pick(8.0, 0f, false);
            if (hit instanceof BlockHitResult b && hit.getType() == HitResult.Type.BLOCK) {
                return b.getBlockPos();
            }
            return player.blockPosition().below();
        }
        return BlockPos.containing(ctx.getSource().getPosition());
    }

    private static FarmlandBlockEntity farmlandAt(ServerLevel level, BlockPos pos) {
        if (level.getBlockEntity(pos) instanceof FarmlandBlockEntity f) {
            return f;
        }
        if (level.getBlockEntity(pos.below()) instanceof FarmlandBlockEntity f) {
            return f;
        }
        return null;
    }

    private static CropBlockEntity cropAt(ServerLevel level, BlockPos pos) {
        if (level.getBlockEntity(pos) instanceof CropBlockEntity c) {
            return c;
        }
        if (level.getBlockEntity(pos.above()) instanceof CropBlockEntity c) {
            return c;
        }
        return null;
    }

    // ------------------------------------------------------------------ soil

    private static int soilSet(CommandContext<CommandSourceStack> ctx) {
        ServerLevel level = ctx.getSource().getLevel();
        FarmlandBlockEntity farmland = farmlandAt(level, target(ctx));
        if (farmland == null) {
            return fail(ctx, "Look at farmland (or a crop on farmland).");
        }
        farmland.advance(level);
        String property = StringArgumentType.getString(ctx, "property").toLowerCase(Locale.ROOT);
        double value = DoubleArgumentType.getDouble(ctx, "value");
        SoilState s = farmland.soil();
        switch (property) {
            case "n" -> s.nitrogen = value;
            case "p" -> s.phosphorus = value;
            case "k" -> s.potassium = value;
            case "s" -> s.sulfur = value;
            case "ca" -> s.calcium = value;
            case "mg" -> s.magnesium = value;
            case "ph" -> s.ph = value;
            case "om" -> s.organicMatter = value;
            case "moisture" -> s.moisture = value;
            case "compaction" -> s.compaction = value;
            case "residue" -> s.residue = value;
            default -> {
                return fail(ctx, "Unknown property. Use one of " + SOIL_PROPERTIES);
            }
        }
        SoilChemistry.clamp(s);
        farmland.setChanged();
        ctx.getSource().sendSuccess(() -> Component.literal("Set " + property + " = " + value), true);
        return 1;
    }

    private static int soilAmend(CommandContext<CommandSourceStack> ctx) {
        ServerLevel level = ctx.getSource().getLevel();
        FarmlandBlockEntity farmland = farmlandAt(level, target(ctx));
        if (farmland == null) {
            return fail(ctx, "Look at farmland (or a crop on farmland).");
        }
        Amendment a;
        try {
            a = Amendment.valueOf(StringArgumentType.getString(ctx, "amendment").toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return fail(ctx, "Unknown amendment.");
        }
        farmland.advance(level);
        a.applyTo(farmland.soil(), farmland.texture());
        farmland.setChanged();
        ctx.getSource().sendSuccess(() -> Component.literal("Applied " + a.displayName() + ": " + a.summary()), true);
        return 1;
    }

    // ------------------------------------------------------------------ crops

    private static int cropSetStage(CommandContext<CommandSourceStack> ctx) {
        ServerLevel level = ctx.getSource().getLevel();
        CropBlockEntity crop = cropAt(level, target(ctx));
        if (crop == null) {
            return fail(ctx, "Look at a Tilth crop.");
        }
        int stage = IntegerArgumentType.getInteger(ctx, "stage");
        crop.setStage(level, stage);
        ctx.getSource().sendSuccess(() -> Component.literal("Set " + crop.species().name + " to visual stage " + stage), true);
        return 1;
    }

    /**
     * Runs this one block ahead of the calendar. The block's own clock moves forward, so it will not
     * simulate again until the world calendar catches up; use /tilth time skip to move the whole world.
     */
    private static int cropGrow(CommandContext<CommandSourceStack> ctx) {
        ServerLevel level = ctx.getSource().getLevel();
        CropBlockEntity crop = cropAt(level, target(ctx));
        FarmlandBlockEntity farmland = crop == null ? null : farmlandAt(level, crop.getBlockPos().below());
        if (farmland == null) {
            return fail(ctx, "Look at a Tilth crop. To advance the whole world, use /tilth time skip <days>.");
        }
        int days = IntegerArgumentType.getInteger(ctx, "days");
        farmland.simulateExtraDays(level, days);
        ctx.getSource().sendSuccess(() -> Component.literal("Simulated " + days
                + " days ahead for this crop only, using the coming days' weather."), true);
        return days;
    }

    private static int cropKill(CommandContext<CommandSourceStack> ctx) {
        ServerLevel level = ctx.getSource().getLevel();
        CropBlockEntity crop = cropAt(level, target(ctx));
        if (crop == null) {
            return fail(ctx, "Look at a Tilth crop.");
        }
        crop.cropState().kill("killed by command");
        crop.afterSimulation(level);
        ctx.getSource().sendSuccess(() -> Component.literal("Crop killed."), true);
        return 1;
    }

    private static int fillField(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        ServerLevel level = player.serverLevel();
        CropSpecies sp = Crops.byId(StringArgumentType.getString(ctx, "species"));
        if (sp == null) {
            return fail(ctx, "Unknown crop. Try: " + String.join(", ", Crops.map().keySet()));
        }
        int r = IntegerArgumentType.getInteger(ctx, "radius");
        int planted = 0;
        for (int dx = -r; dx <= r; dx++) {
            for (int dz = -r; dz <= r; dz++) {
                int x = player.getBlockX() + dx;
                int z = player.getBlockZ() + dz;
                BlockPos ground = new BlockPos(x, level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z) - 1, z);
                if (Tilling.till(level, ground, Tillage.Implement.HOE, null) && level.getBlockState(ground.above()).isAir()) {
                    level.setBlockAndUpdate(ground.above(), TilthBlocks.crop(sp).get().defaultBlockState());
                    planted++;
                }
            }
        }
        int n = planted;
        ctx.getSource().sendSuccess(() -> Component.literal("Planted " + n + " blocks of " + sp.name), true);
        return planted;
    }

    // ------------------------------------------------------------------ time

    private static int timeSkip(CommandContext<CommandSourceStack> ctx) {
        double days = DoubleArgumentType.getDouble(ctx, "days");
        TilthCalendar.skipRealDays(ctx.getSource().getServer(), days);
        ServerLevel level = ctx.getSource().getLevel();
        ctx.getSource().sendSuccess(() -> Component.literal(String.format("Calendar advanced %.1f days to %s. Fields catch up as they tick.",
                days, TilthCalendar.describeToday(level))), true);
        return 1;
    }

    private static int timeSet(CommandContext<CommandSourceStack> ctx) {
        ServerLevel level = ctx.getSource().getLevel();
        int month = IntegerArgumentType.getInteger(ctx, "month");
        int day = IntegerArgumentType.getInteger(ctx, "day");
        double now = TilthCalendar.realDay(level);
        int year = CalendarMath.year(now);
        double target = (year - 1) * 365.0 + CalendarMath.firstDayOfMonth(month) + day - 1;
        while (target <= now) {
            target += 365;
        }
        TilthCalendar.skipRealDays(ctx.getSource().getServer(), target - now);
        ctx.getSource().sendSuccess(() -> Component.literal("Calendar moved forward to " + TilthCalendar.describeToday(level)), true);
        return 1;
    }

    // ------------------------------------------------------------------ diet

    private static int dietShow(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        PlayerDiet diet = player.getData(TilthAttachments.DIET);
        List<Component> lines = new ArrayList<>();
        lines.add(Reports.header("Diet of " + player.getGameProfile().getName()));
        for (DietNutrient n : DietNutrient.values()) {
            double v = diet.get(n);
            int bars = (int) Math.round(v * 20);
            ChatFormatting color = v < 0.2 ? ChatFormatting.RED : v < 0.5 ? ChatFormatting.YELLOW : ChatFormatting.GREEN;
            lines.add(Component.literal(String.format("%-14s ", n.displayName())).withStyle(ChatFormatting.GRAY)
                    .append(Component.literal("|".repeat(bars)).withStyle(color))
                    .append(Component.literal("|".repeat(20 - bars)).withStyle(ChatFormatting.DARK_GRAY))
                    .append(Component.literal(String.format(" %3.0f%%  (store lasts %.1f days)", v * 100, n.storeDays())).withStyle(ChatFormatting.GRAY)));
        }
        return send(ctx, lines);
    }

    private static int dietSet(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        DietNutrient n = DietNutrient.byId(StringArgumentType.getString(ctx, "nutrient"));
        if (n == null) {
            return fail(ctx, "Unknown nutrient.");
        }
        double v = DoubleArgumentType.getDouble(ctx, "value");
        player.getData(TilthAttachments.DIET).set(n, v);
        ctx.getSource().sendSuccess(() -> Component.literal(n.displayName() + " set to " + Math.round(v * 100) + "%"), true);
        return 1;
    }

    // ------------------------------------------------------------------ plots

    private static java.util.concurrent.CompletableFuture<com.mojang.brigadier.suggestion.Suggestions> suggestPlots(
            CommandContext<CommandSourceStack> ctx, com.mojang.brigadier.suggestion.SuggestionsBuilder b) {
        return SharedSuggestionProvider.suggest(PlotData.get(ctx.getSource().getLevel()).names().stream().map(PlotData::quoteIfNeeded), b);
    }

    private static int plotList(CommandContext<CommandSourceStack> ctx) {
        List<Plot> plots = PlotData.get(ctx.getSource().getLevel()).all();
        List<Component> lines = new ArrayList<>();
        lines.add(Reports.header(plots.size() + " plot(s) in this dimension"));
        double total = 0;
        for (Plot p : plots) {
            total += p.areaSquareMetres();
            lines.add(Reports.line(p.name(), PolygonMath.formatArea(p.areaSquareMetres()) + ", owner " + p.ownerName()
                    + ", " + p.vertices().size() + " corners"));
        }
        lines.add(Reports.line("Total", PolygonMath.formatArea(total)));
        return send(ctx, lines);
    }

    private static int plotInfo(CommandContext<CommandSourceStack> ctx, String name) {
        ServerLevel level = ctx.getSource().getLevel();
        PlotData data = PlotData.get(level);
        Plot p;
        if (name == null) {
            BlockPos pos = BlockPos.containing(ctx.getSource().getPosition());
            p = data.at(pos.getX(), pos.getZ());
        } else {
            p = data.byName(name.replace("\"", ""));
        }
        if (p == null) {
            return fail(ctx, name == null ? "You are not inside a plot." : "No plot named " + name);
        }
        List<Component> lines = new ArrayList<>();
        lines.add(Reports.header("Plot \"" + p.name() + "\""));
        lines.add(Reports.line("Area", PolygonMath.formatArea(p.areaSquareMetres()) + String.format(" = %,d blocks", p.blockCount())));
        lines.add(Reports.line("Perimeter", String.format("%.0f m (%.0f ft, %.1f Gunter's chains)", p.perimeterMetres(),
                p.perimeterMetres() * PolygonMath.FEET_PER_METRE, p.perimeterMetres() / 20.1168)));
        lines.add(Reports.line("Owner", p.ownerName() + (p.members().isEmpty() ? "" : " + " + p.members().size() + " member(s)")));
        lines.add(Reports.line("Bounds", String.format("x %d..%d, z %d..%d", p.minX(), p.maxX(), p.minZ(), p.maxZ())));
        lines.add(Reports.line("Surveyed", CalendarMath.describe(p.createdDay())));
        summarisePlot(level, p, lines);
        return send(ctx, lines);
    }

    /** Samples farmland and crops inside the plot (up to ~4000 blocks) for averages. */
    private static void summarisePlot(ServerLevel level, Plot p, List<Component> lines) {
        int step = Math.max(1, (int) Math.ceil(Math.sqrt(p.blockCount() / 4000.0)));
        int farmland = 0;
        int crops = 0;
        double ph = 0, n = 0, phos = 0, k = 0, om = 0;
        java.util.Map<String, Integer> cropCounts = new java.util.TreeMap<>();
        for (int x = p.minX(); x <= p.maxX(); x += step) {
            for (int z = p.minZ(); z <= p.maxZ(); z += step) {
                if (!p.contains(x, z) || !level.hasChunkAt(x, z)) {
                    continue;
                }
                BlockPos ground = new BlockPos(x, level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z) - 1, z);
                if (level.getBlockEntity(ground) instanceof FarmlandBlockEntity f && f.isInitialized()) {
                    farmland++;
                    ph += f.soil().ph;
                    n += f.soil().nitrogen;
                    phos += f.soil().phosphorus;
                    k += f.soil().potassium;
                    om += f.soil().organicMatter;
                }
                if (level.getBlockEntity(ground.above()) instanceof CropBlockEntity c) {
                    crops++;
                    cropCounts.merge(c.species().name, 1, Integer::sum);
                }
            }
        }
        if (farmland > 0) {
            lines.add(Reports.line("Soil (sampled)", String.format("pH %.1f, OM %.1f%%, N %.0f, P %.0f, K %.0f ppm over %d samples",
                    ph / farmland, om / farmland, n / farmland, phos / farmland, k / farmland, farmland)));
        } else {
            lines.add(Reports.line("Soil", "no tilled farmland found in the plot"));
        }
        if (crops > 0) {
            lines.add(Reports.line("Crops (sampled)", cropCounts.toString()));
        }
    }

    private static int plotRename(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        PlotData data = PlotData.get(ctx.getSource().getLevel());
        Plot p = data.byName(StringArgumentType.getString(ctx, "old"));
        if (p == null) {
            return fail(ctx, "No such plot.");
        }
        if (!canManage(ctx, p)) {
            return fail(ctx, "Only the owner can rename this plot.");
        }
        String newName = StringArgumentType.getString(ctx, "new").replace("\"", "");
        if (data.byName(newName) != null) {
            return fail(ctx, "A plot with that name already exists.");
        }
        p.rename(newName);
        data.changed();
        ctx.getSource().sendSuccess(() -> Component.literal("Renamed to " + newName), true);
        return 1;
    }

    private static int plotDelete(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        PlotData data = PlotData.get(ctx.getSource().getLevel());
        Plot p = data.byName(StringArgumentType.getString(ctx, "name").replace("\"", ""));
        if (p == null) {
            return fail(ctx, "No such plot.");
        }
        if (!canManage(ctx, p)) {
            return fail(ctx, "Only the owner can delete this plot.");
        }
        data.remove(p);
        ctx.getSource().sendSuccess(() -> Component.literal("Deleted plot " + p.name()), true);
        return 1;
    }

    private static int plotMember(CommandContext<CommandSourceStack> ctx, boolean add) throws CommandSyntaxException {
        PlotData data = PlotData.get(ctx.getSource().getLevel());
        Plot p = data.byName(StringArgumentType.getString(ctx, "name"));
        if (p == null) {
            return fail(ctx, "No such plot.");
        }
        if (!canManage(ctx, p)) {
            return fail(ctx, "Only the owner can change members.");
        }
        ServerPlayer member = EntityArgument.getPlayer(ctx, "player");
        if (add) {
            p.members().add(member.getUUID());
        } else {
            p.members().remove(member.getUUID());
        }
        data.changed();
        ctx.getSource().sendSuccess(() -> Component.literal((add ? "Added " : "Removed ") + member.getGameProfile().getName()), true);
        return 1;
    }

    private static boolean canManage(CommandContext<CommandSourceStack> ctx, Plot p) throws CommandSyntaxException {
        if (ctx.getSource().hasPermission(2) || ctx.getSource().getEntity() == null) {
            return true;
        }
        return ctx.getSource().getPlayerOrException().getUUID().equals(p.owner());
    }

    // ------------------------------------------------------------------ afflictions

    private static java.util.concurrent.CompletableFuture<com.mojang.brigadier.suggestion.Suggestions> suggestAfflictions(
            CommandContext<CommandSourceStack> ctx, com.mojang.brigadier.suggestion.SuggestionsBuilder b) {
        Registry<Affliction> reg = AfflictionEngine.registry(ctx.getSource().getLevel());
        return reg == null ? b.buildFuture() : SharedSuggestionProvider.suggestResource(reg.keySet(), b);
    }

    private static int afflictionList(CommandContext<CommandSourceStack> ctx) {
        Registry<Affliction> reg = AfflictionEngine.registry(ctx.getSource().getLevel());
        if (reg == null) {
            return fail(ctx, "Affliction registry not loaded.");
        }
        List<Component> lines = new ArrayList<>();
        lines.add(Reports.header(reg.size() + " afflictions defined" + (TilthConfig.AFFLICTIONS.getAsBoolean() ? "" : " (simulation disabled in config)")));
        reg.entrySet().stream().sorted((a, b) -> a.getKey().location().compareTo(b.getKey().location())).forEach(e -> {
            Affliction a = e.getValue();
            lines.add(Reports.line(AfflictionEngine.displayId(e.getKey().location()), a.name() + " [" + a.category().getSerializedName()
                    + (a.beneficial() ? ", beneficial" : "") + "] hosts: " + String.join(", ", a.hosts())));
        });
        return send(ctx, lines);
    }

    private static int afflictionInfo(CommandContext<CommandSourceStack> ctx) {
        Registry<Affliction> reg = AfflictionEngine.registry(ctx.getSource().getLevel());
        ResourceLocation id = ResourceLocationArgument.getId(ctx, "id");
        Affliction a = reg == null ? null : reg.get(id);
        if (a == null) {
            return fail(ctx, "Unknown affliction " + id);
        }
        List<Component> lines = new ArrayList<>();
        lines.add(Reports.header(a.name() + (a.scientificName().isEmpty() ? "" : " (" + a.scientificName() + ")")));
        lines.add(Reports.line("Type", a.category().getSerializedName() + (a.beneficial() ? " (beneficial)" : "")));
        lines.add(Reports.line("Hosts", String.join(", ", a.hosts())));
        lines.add(Reports.line("Spreads by", a.transmission().stream().map(Affliction.Transmission::getSerializedName).toList().toString()));
        Affliction.Envelope e = a.envelope();
        lines.add(Reports.line("Favoured by", String.format("%.0f-%.0f °C (opt %.0f), RH > %.0f%%, leaf wetness %.0f h, pH %.1f-%.1f",
                e.tempMin(), e.tempMax(), e.tempOpt(), e.minHumidity(), e.leafWetnessHours(), e.phMin(), e.phMax())));
        lines.add(Reports.line("Lifecycle", String.format("latent %d days, survives %.1f years via %s", a.lifecycle().latentDays(),
                a.lifecycle().survivalYears(), a.lifecycle().overwintering())));
        lines.add(Reports.line("Impact", String.format("up to %.0f%% yield loss%s", 100 * a.impact().maxYieldLoss(),
                a.impact().canKillHost() ? ", can kill the host" : "")));
        lines.add(Reports.line("Control", String.join("; ", a.control().methods())));
        if (!a.symptoms().isEmpty()) {
            lines.add(Reports.line("Symptoms", String.join("; ", a.symptoms())));
        }
        if (!a.notes().isEmpty()) {
            lines.add(Reports.note(a.notes()));
        }
        return send(ctx, lines);
    }

    private static int afflictionRisk(CommandContext<CommandSourceStack> ctx) {
        ServerLevel level = ctx.getSource().getLevel();
        CropBlockEntity crop = cropAt(level, target(ctx));
        FarmlandBlockEntity farmland = farmlandAt(level, target(ctx));
        Registry<Affliction> reg = AfflictionEngine.registry(level);
        if (crop == null || farmland == null || reg == null) {
            return fail(ctx, "Look at a Tilth crop.");
        }
        var w = com.claudacious.tilth.world.ClimateService.today(level, farmland.getBlockPos());
        int year = CalendarMath.year(TilthCalendar.realDay(level));
        List<Component> lines = new ArrayList<>();
        lines.add(Reports.header("Today's infection risk for " + crop.species().name));
        reg.entrySet().forEach(e -> {
            Affliction a = e.getValue();
            if (!a.infects(crop.species())) {
                return;
            }
            double suit = AfflictionEngine.suitability(a, w, farmland.soil(), farmland.hydraulics());
            double inoc = AfflictionEngine.inoculum(a, crop.species(), farmland.soil(), year);
            lines.add(Reports.line(a.name(), String.format("conditions %.0f%%, inoculum %.0f%%%s", 100 * suit, 100 * inoc,
                    a.beneficial() ? " (beneficial)" : "")));
        });
        return send(ctx, lines);
    }

    private static int afflictionInfect(CommandContext<CommandSourceStack> ctx) {
        ServerLevel level = ctx.getSource().getLevel();
        CropBlockEntity crop = cropAt(level, target(ctx));
        if (crop == null) {
            return fail(ctx, "Look at a Tilth crop.");
        }
        ResourceLocation id = ResourceLocationArgument.getId(ctx, "id");
        crop.cropState().afflictions.put(id.toString(), 0.1);
        crop.setChanged();
        ctx.getSource().sendSuccess(() -> Component.literal("Infected with " + id + (TilthConfig.AFFLICTIONS.getAsBoolean() ? ""
                : " (it will not progress until afflictions.enabled = true)")), true);
        return 1;
    }

    private static int afflictionClear(CommandContext<CommandSourceStack> ctx) {
        ServerLevel level = ctx.getSource().getLevel();
        CropBlockEntity crop = cropAt(level, target(ctx));
        if (crop == null) {
            return fail(ctx, "Look at a Tilth crop.");
        }
        crop.cropState().afflictions.clear();
        crop.cropState().afflictionGrowthFactor = 1;
        crop.cropState().afflictionYieldFactor = 1;
        crop.cropState().afflictionQualityFactor = 1;
        crop.setChanged();
        ctx.getSource().sendSuccess(() -> Component.literal("Cleared afflictions."), true);
        return 1;
    }

    // ------------------------------------------------------------------ water

    private static int forcePond(CommandContext<CommandSourceStack> ctx) {
        ServerLevel level = ctx.getSource().getLevel();
        BlockPos pos = target(ctx);
        List<BlockPos> basin = SurfaceWater.closedBasin(level, pos.getX(), pos.getZ(), pos.getY() + 1, TilthConfig.MAX_POND_AREA.getAsInt());
        if (basin == null) {
            return fail(ctx, "Water at this level would drain away (no closed rim within " + TilthConfig.MAX_POND_AREA.getAsInt() + " blocks).");
        }
        ctx.getSource().sendSuccess(() -> Component.literal("Closed depression: " + basin.size() + " blocks would hold water at y="
                + (pos.getY() + 1) + ". Rain fills it over time."), false);
        return basin.size();
    }
}
