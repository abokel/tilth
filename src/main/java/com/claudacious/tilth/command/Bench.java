package com.claudacious.tilth.command;

import com.claudacious.tilth.plot.Plot;
import com.claudacious.tilth.plot.PlotData;
import com.claudacious.tilth.sim.plot.PolygonMath;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/** Op-only micro-benchmarks for systems that scale with the world: plot lookups. */
final class Bench {
    private Bench() {
    }

    /**
     * Creates {@code count} irregular eight-sided plots of about two acres each on a grid around the player,
     * times the lookup used by tilling, protection and the plot HUD, then removes them again.
     */
    static int plots(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        ServerLevel level = player.serverLevel();
        int count = IntegerArgumentType.getInteger(ctx, "count");
        PlotData data = PlotData.get(level);
        Random rng = new Random(42);
        int side = (int) Math.ceil(Math.sqrt(count));
        int spacing = 100;
        int ox = player.getBlockX() - side * spacing / 2;
        int oz = player.getBlockZ() - side * spacing / 2;
        List<Plot> made = new ArrayList<>();
        long t0 = System.nanoTime();
        double blocks = 0;
        for (int i = 0; i < count; i++) {
            int cx = ox + (i % side) * spacing + spacing / 2;
            int cz = oz + (i / side) * spacing + spacing / 2;
            List<PolygonMath.Vertex> vs = new ArrayList<>();
            for (int k = 0; k < 8; k++) {
                double a = Math.PI * 2 * k / 8 + rng.nextDouble() * 0.3;
                double r = 38 + rng.nextDouble() * 10;
                vs.add(new PolygonMath.Vertex((int) Math.round(cx + Math.cos(a) * r), (int) Math.round(cz + Math.sin(a) * r)));
            }
            Plot p = data.create("bench", player, vs, level);
            blocks += p.blockCount();
            made.add(p);
        }
        long created = System.nanoTime() - t0;
        int lookups = 200_000;
        int hits = 0;
        long t1 = System.nanoTime();
        for (int i = 0; i < lookups; i++) {
            int x = ox + rng.nextInt(side * spacing);
            int z = oz + rng.nextInt(side * spacing);
            if (data.at(x, z) != null) {
                hits++;
            }
        }
        long looked = System.nanoTime() - t1;
        made.forEach(data::remove);
        double acres = blocks / 4046.86;
        int h = hits;
        ctx.getSource().sendSuccess(() -> Component.literal(String.format(
                "Plots: %d irregular plots (%.0f acres) created in %.1f ms; %d lookups at %.0f ns each (%.0f%% inside a plot). Removed.",
                count, acres, created / 1e6, lookups, looked / (double) lookups, 100.0 * h / lookups)), false);
        return 1;
    }
}
