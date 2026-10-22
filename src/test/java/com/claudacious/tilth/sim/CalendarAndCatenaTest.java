package com.claudacious.tilth.sim;

import com.claudacious.tilth.sim.calendar.CalendarMath;
import com.claudacious.tilth.sim.calendar.Season;
import com.claudacious.tilth.sim.catena.CatenaAnalyzer;
import com.claudacious.tilth.sim.catena.SlopePosition;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CalendarAndCatenaTest {
    @Test
    void calendarStartsOnFirstOfStartMonth() {
        CalendarMath cal = new CalendarMath(6, 3);
        double d = cal.realDay(0);
        int doy = CalendarMath.dayOfYear(d);
        assertEquals(3, CalendarMath.month(doy));
        assertEquals(1, CalendarMath.dayOfMonth(doy));
        assertEquals(Season.SPRING, CalendarMath.season(doy));
        assertEquals(365.0 / 72.0, cal.realDaysPerGameDay(), 1e-9);
    }

    @Test
    void oneGameYearIsOneCalendarYear() {
        CalendarMath cal = new CalendarMath(6, 1);
        long ticks = 72L * CalendarMath.TICKS_PER_DAY;
        assertEquals(2, CalendarMath.year(cal.realDay(ticks)));
    }

    /** A cone-shaped hill rising 20 blocks above a flat plain at y=64. */
    private static int hill(int x, int z) {
        double r = Math.hypot(x, z);
        return 64 + (int) Math.round(Math.max(0, 20 - r * 0.5));
    }

    @Test
    void hilltopIsSummitAndFlankIsBackslope() {
        CatenaAnalyzer.Result top = CatenaAnalyzer.analyze(CalendarAndCatenaTest::hill, 0, 0, 15, false, 63);
        assertTrue(top.position() == SlopePosition.SUMMIT || top.position() == SlopePosition.SHOULDER, top.toString());

        CatenaAnalyzer.Result flank = CatenaAnalyzer.analyze((x, z) -> 64 + x, 0, 0, 0, false, 63);
        assertEquals(SlopePosition.BACKSLOPE, flank.position());
    }

    @Test
    void bowlIsDepression() {
        CatenaAnalyzer.HeightField bowl = (x, z) -> 70 + (int) Math.round(Math.min(6, (x * x + z * z) / 8.0));
        CatenaAnalyzer.Result r = CatenaAnalyzer.analyze(bowl, 0, 0, -6, false, 63);
        assertEquals(SlopePosition.DEPRESSION, r.position());
    }
}
