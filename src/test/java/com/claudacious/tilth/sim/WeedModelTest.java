package com.claudacious.tilth.sim;

import com.claudacious.tilth.sim.climate.DailyWeather;
import com.claudacious.tilth.sim.crop.WeedModel;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class WeedModelTest {
    private static double season(double residue, double canopy, boolean cover) {
        double w = 0;
        for (int day = 0; day < 60; day++) {
            w = WeedModel.grow(w, 18, 0.8, 25, residue, canopy, cover);
        }
        return w;
    }

    @Test
    void bareWarmGroundGetsOverrun() {
        assertTrue(season(0, 0, false) > 0.6, "weeds after a bare summer: " + season(0, 0, false));
    }

    @Test
    void canopyMulchAndCoverHoldWeedsBack() {
        double bare = season(0, 0, false);
        assertTrue(season(0.6, 0, false) < bare, "mulch");
        assertTrue(season(0, 0.9, false) < bare * 0.5, "closed canopy");
        assertTrue(season(0, 0.9, true) < 0.05, "a dense cover crop all but stops them");
    }

    @Test
    void coldStopsWeeds() {
        assertEquals(0.0, WeedModel.grow(0.0, 2, 0.8, 25, 0, 0, false), 1e-9);
    }

    @Test
    void competitionAndGlass() {
        assertEquals(1.0, WeedModel.competition(0), 1e-9);
        assertEquals(0.55, WeedModel.competition(1), 1e-9);
        DailyWeather frost = new DailyWeather(1, -4, 6, 5, 0, 2, 0.7, 6);
        DailyWeather glass = WeedModel.underGlass(frost);
        assertTrue(glass.tMin() >= 2, "no frost under glass");
        assertEquals(0, glass.rain(), 1e-9, "no rain under glass");
        assertTrue(glass.tMax() > frost.tMax());
    }
}
