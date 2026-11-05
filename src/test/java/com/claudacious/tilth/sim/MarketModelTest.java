package com.claudacious.tilth.sim;

import com.claudacious.tilth.sim.market.MarketModel;
import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class MarketModelTest {
    @Test
    void grainIsCheapAfterHarvestAndDearBeforeIt() {
        assertTrue(MarketModel.seasonal(MarketModel.GRAIN, 9) < MarketModel.seasonal(MarketModel.GRAIN, 5));
        assertTrue(MarketModel.seasonal(MarketModel.HAY, 2) > MarketModel.seasonal(MarketModel.HAY, 7));
        assertTrue(MarketModel.seasonal(MarketModel.DAIRY, 5) < MarketModel.seasonal(MarketModel.DAIRY, 11));
    }

    @Test
    void indexDriftsToSeasonAndStaysBounded() {
        Random r = new Random(4);
        double index = 2.4;
        for (int d = 0; d < 60; d++) {
            index = MarketModel.step(index, MarketModel.GRAIN, 9, r);
            assertTrue(index >= 0.4 && index <= 2.5);
        }
        assertEquals(MarketModel.seasonal(MarketModel.GRAIN, 9), index, 0.15);
    }

    @Test
    void eventsMatchTheirSeasons() {
        Random r = new Random(1);
        for (int i = 0; i < 400; i++) {
            MarketModel.Event e = MarketModel.roll(1, r);
            if (e != null) {
                assertTrue(e.possibleIn(1), e.id());
                assertNotEquals("bumper_harvest", e.id());
            }
        }
    }

    @Test
    void qualityPaysMore() {
        assertTrue(MarketModel.price(0.1, 1, 1, 5) > MarketModel.price(0.1, 1, 1, 3));
        assertTrue(MarketModel.price(0.1, 1, 1, 1) < MarketModel.price(0.1, 1, 1, 0));
        assertEquals(0.1, MarketModel.price(0.1, 1, 1, 0), 1e-12);
    }
}
