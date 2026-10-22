package com.claudacious.tilth.sim.calendar;

/**
 * Maps Minecraft ticks onto a real 365-day calendar.
 *
 * <p>All agronomic processes (thermal time, rainfall, evapotranspiration, mineralisation) are
 * modelled per <em>real</em> day. A Minecraft day is compressed to {@code 365 / (12 * daysPerMonth)}
 * real days, so with the default of 6 days per month one game day covers about five calendar days
 * and a full year passes in 72 game days.
 */
public final class CalendarMath {
    public static final int TICKS_PER_DAY = 24000;
    public static final int DAYS_PER_YEAR = 365;

    private static final int[] MONTH_LENGTH = {31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31};
    private static final int[] MONTH_START = new int[13];
    private static final String[] MONTH_NAMES = {"January", "February", "March", "April", "May", "June",
            "July", "August", "September", "October", "November", "December"};

    static {
        int acc = 0;
        for (int m = 0; m < 12; m++) {
            MONTH_START[m] = acc;
            acc += MONTH_LENGTH[m];
        }
        MONTH_START[12] = acc;
    }

    private final int daysPerMonth;
    private final int startMonth;

    public CalendarMath(int daysPerMonth, int startMonth) {
        this.daysPerMonth = Math.max(1, daysPerMonth);
        this.startMonth = Math.min(12, Math.max(1, startMonth));
    }

    public int daysPerMonth() {
        return daysPerMonth;
    }

    public int gameDaysPerYear() {
        return daysPerMonth * 12;
    }

    /** How many real calendar days one Minecraft day represents. */
    public double realDaysPerGameDay() {
        return DAYS_PER_YEAR / (double) gameDaysPerYear();
    }

    /** Real days elapsed since 1 January, year 1, for a calendar tick count. */
    public double realDay(long calendarTicks) {
        return MONTH_START[startMonth - 1] + (calendarTicks / (double) TICKS_PER_DAY) * realDaysPerGameDay();
    }

    /** Number of game ticks that spans the given number of real days. */
    public long ticksForRealDays(double realDays) {
        return Math.round(realDays / realDaysPerGameDay() * TICKS_PER_DAY);
    }

    public static int dayOfYear(double realDay) {
        return Math.floorMod((long) Math.floor(realDay), DAYS_PER_YEAR);
    }

    public static int year(double realDay) {
        return (int) Math.floorDiv((long) Math.floor(realDay), DAYS_PER_YEAR) + 1;
    }

    /** Month number 1-12 for a day of the year (0-364). */
    public static int month(int dayOfYear) {
        for (int m = 0; m < 12; m++) {
            if (dayOfYear < MONTH_START[m + 1]) {
                return m + 1;
            }
        }
        return 12;
    }

    public static int dayOfMonth(int dayOfYear) {
        return dayOfYear - MONTH_START[month(dayOfYear) - 1] + 1;
    }

    public static int firstDayOfMonth(int month) {
        return MONTH_START[Math.min(12, Math.max(1, month)) - 1];
    }

    public static String monthName(int month) {
        return MONTH_NAMES[Math.min(12, Math.max(1, month)) - 1];
    }

    public static Season season(int dayOfYear) {
        return Season.ofMonth(month(dayOfYear));
    }

    /** "early", "mid" or "late" within the current season. */
    public static String seasonPhase(int dayOfYear) {
        int m = month(dayOfYear);
        int idx = switch (m) {
            case 12, 3, 6, 9 -> 0;
            case 1, 4, 7, 10 -> 1;
            default -> 2;
        };
        return idx == 0 ? "early" : (idx == 1 ? "mid" : "late");
    }

    public static String describe(double realDay) {
        int doy = dayOfYear(realDay);
        int m = month(doy);
        return dayOfMonth(doy) + " " + monthName(m) + ", Year " + year(realDay);
    }
}
