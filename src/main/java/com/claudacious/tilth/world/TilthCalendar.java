package com.claudacious.tilth.world;

import com.claudacious.tilth.sim.calendar.CalendarMath;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.event.level.SleepFinishedTimeEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * The world calendar. Calendar time is the overworld's total game time plus an offset that grows
 * when players sleep through the night and when an operator skips days, so it never runs backwards
 * (unlike the day-time clock, which /time set can rewind).
 */
public final class TilthCalendar {
    private static volatile CalendarMath math = new CalendarMath(6, 3);
    private static CalendarData cached;
    private static MinecraftServer cachedServer;

    private TilthCalendar() {
    }

    public static void reconfigure(int daysPerMonth, int startMonth) {
        math = new CalendarMath(daysPerMonth, startMonth);
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

    public static long ticks(Level level) {
        MinecraftServer server = level.getServer();
        if (server == null) {
            return level.getGameTime();
        }
        return server.overworld().getGameTime() + data(server).offsetTicks;
    }

    /** Real calendar days since 1 January of year 1 (fractional). */
    public static double realDay(Level level) {
        return math.realDay(ticks(level));
    }

    public static long today(Level level) {
        return (long) Math.floor(realDay(level));
    }

    public static String describeToday(Level level) {
        double d = realDay(level);
        int doy = CalendarMath.dayOfYear(d);
        return CalendarMath.describe(d) + " (" + CalendarMath.seasonPhase(doy) + " " + CalendarMath.season(doy).displayName().toLowerCase() + ")";
    }

    public static void skipRealDays(MinecraftServer server, double realDays) {
        data(server).addOffset(math.ticksForRealDays(realDays));
    }

    public static boolean rainedOn(Level level, long day) {
        MinecraftServer server = level.getServer();
        return server != null && data(server).rained(day);
    }

    public static void onSleepFinished(SleepFinishedTimeEvent event) {
        if (event.getLevel() instanceof ServerLevel level && level == level.getServer().overworld()) {
            long skipped = event.getNewTime() - level.getDayTime();
            if (skipped > 0) {
                data(level.getServer()).addOffset(skipped);
            }
        }
    }

    public static void onServerTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        ServerLevel overworld = server.overworld();
        if (overworld.getGameTime() % 100 == 0 && overworld.isRaining()) {
            data(server).markRain(today(overworld));
        }
    }

    public static void onServerStopped(ServerStoppedEvent event) {
        cached = null;
        cachedServer = null;
    }
}
