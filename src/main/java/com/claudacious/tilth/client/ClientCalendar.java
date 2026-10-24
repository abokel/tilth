package com.claudacious.tilth.client;

import com.claudacious.tilth.sim.calendar.CalendarMath;
import net.minecraft.client.Minecraft;

/** The Tilth calendar as the client knows it, from {@link com.claudacious.tilth.network.CalendarSync}. */
public final class ClientCalendar {
    private static long offset;
    private static CalendarMath math = new CalendarMath(6, 3);
    private static boolean known;

    private ClientCalendar() {
    }

    public static void set(long offsetTicks, int daysPerMonth, int startMonth) {
        offset = offsetTicks;
        math = new CalendarMath(daysPerMonth, startMonth);
        known = true;
    }

    public static boolean known() {
        return known && Minecraft.getInstance().level != null;
    }

    /** Day of the year (0-364) on the client, or -1 before the server has said. */
    public static int dayOfYear() {
        if (!known()) {
            return -1;
        }
        return CalendarMath.dayOfYear(math.realDay(Minecraft.getInstance().level.getGameTime() + offset));
    }
}
