package com.claudacious.tilth.sim;

import com.claudacious.tilth.sim.calendar.CalendarMath;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SeasonCalendarTest {
    @Test
    void eachSubSeasonFillsItsMonth() {
        assertEquals(59, CalendarMath.dayOfSeasonYear(0.0), 1e-9, "early spring starts 1 March");
        assertEquals(181, CalendarMath.dayOfSeasonYear(4 / 12.0), 1e-9, "mid summer starts 1 July");
        assertEquals(334, CalendarMath.dayOfSeasonYear(9 / 12.0), 1e-9, "early winter starts 1 December");
        assertEquals(365, CalendarMath.dayOfSeasonYear(10 / 12.0), 1e-9, "mid winter is next January");
        assertEquals(424, CalendarMath.dayOfSeasonYear(1.0 - 1e-12), 1e-6, "late winter ends with February");
    }

    @Test
    void calendarNeverRunsBackwardThroughTheYear() {
        double last = -1;
        for (int i = 0; i < 1200; i++) {
            double day = CalendarMath.dayOfSeasonYear(i / 1200.0);
            assertTrue(day > last, "day " + day + " after " + last);
            last = day;
        }
    }
}
