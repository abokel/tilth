package com.claudacious.tilth.world;

import com.claudacious.tilth.sim.calendar.CalendarMath;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import sereneseasons.api.season.ISeasonState;
import sereneseasons.api.season.SeasonHelper;

/**
 * The world calendar, read from Serene Seasons. Its year of twelve sub-seasons maps onto the twelve months, early
 * spring being March and late winter February, so the crop climate (daily temperature, rain, day length) always
 * matches the season Serene Seasons shows. Use {@code /season set} to change the date.
 *
 * <p>Serene Seasons counts time within the current year only, so the number of years completed is kept here: the
 * calendar day never runs backwards while the seasons move forward.
 */
public final class TilthCalendar {
    private static volatile CalendarMath math = new CalendarMath(8, 3);
    private static CalendarData cached;
    private static MinecraftServer cachedServer;

    private TilthCalendar() {
    }

    public static CalendarMath math() {
        return math;
    }

    public static CalendarData data(MinecraftServer server) {
        if (cachedServer != server || cached == null) {
            cached = server.overworld().getDataStorage().computeIfAbsent(CalendarData.FACTORY, CalendarData.NAME);
            cachedServer = server;
        }
        return cached;
    }

    /** How far through the Serene Seasons year the world is, 0 at the start of early spring. */
    static double yearFraction(Level level) {
        ISeasonState s = SeasonHelper.getSeasonState(level);
        int cycle = Math.max(1, s.getCycleDuration());
        return Math.floorMod(s.getSeasonCycleTicks(), cycle) / (double) cycle;
    }

    /** Real calendar days since 1 January of year 1 (fractional). */
    public static double realDay(Level level) {
        MinecraftServer server = level.getServer();
        Level clock = server != null ? server.overworld() : level;
        long years = server != null ? data(server).years() : 0;
        return years * (double) CalendarMath.DAYS_PER_YEAR + CalendarMath.dayOfSeasonYear(yearFraction(clock));
    }

    public static long today(Level level) {
        return (long) Math.floor(realDay(level));
    }

    public static String describeToday(Level level) {
        double d = realDay(level);
        int doy = CalendarMath.dayOfYear(d);
        return CalendarMath.describe(d) + " (" + CalendarMath.seasonPhase(doy) + " " + CalendarMath.season(doy).displayName().toLowerCase() + ")";
    }

    public static boolean rainedOn(Level level, long day) {
        MinecraftServer server = level.getServer();
        return server != null && data(server).rained(day);
    }

    public static void onServerTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        ServerLevel overworld = server.overworld();
        if (overworld.getGameTime() % 20 != 0) {
            return;
        }
        CalendarData data = data(server);
        // A new Serene Seasons year: its clock wrapped from late winter back to early spring.
        double fraction = yearFraction(overworld);
        data.observe(fraction);
        // Each sub-season is a month, so a month lasts as many game days as a sub-season does.
        ISeasonState s = SeasonHelper.getSeasonState(overworld);
        int daysPerMonth = Math.max(1, s.getSubSeasonDuration() / Math.max(1, s.getDayDuration()));
        if (math.daysPerMonth() != daysPerMonth) {
            math = new CalendarMath(daysPerMonth, 3);
        }
        if (overworld.getGameTime() % 100 == 0 && overworld.isRaining()) {
            data.markRain(today(overworld));
        }
    }

    public static void onServerStopped(ServerStoppedEvent event) {
        cached = null;
        cachedServer = null;
    }
}
