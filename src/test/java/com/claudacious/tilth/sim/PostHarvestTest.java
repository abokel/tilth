package com.claudacious.tilth.sim;

import com.claudacious.tilth.sim.postharvest.GrainDryingModel;
import com.claudacious.tilth.sim.postharvest.GrainSpoilageModel;
import com.claudacious.tilth.sim.postharvest.GrainStorageState;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PostHarvestTest {
    @Test
    void dryingMovesTowardAmbientEquilibriumAndRespectsBounds() {
        double start = 0.28;
        double dryAir = GrainDryingModel.nextMoisture(start, 0.25, false, false, 1.0, 1.2);
        assertTrue(dryAir < start);
        double rainy = GrainDryingModel.nextMoisture(0.10, 0.95, true, false, 1.0, 1.2);
        assertTrue(rainy > 0.10);
        assertTrue(rainy <= GrainDryingModel.MAX_MOISTURE);
        assertTrue(dryAir >= GrainDryingModel.MIN_MOISTURE);
    }

    @Test
    void spoilageIncreasesWithMoistureAndHeat() {
        double safe = GrainDryingModel.SAFE_STORAGE_MOISTURE;
        assertEquals(0.0, GrainSpoilageModel.dailySpoilageFraction(safe, 22, safe), 1.0e-9);
        double coolWet = GrainSpoilageModel.dailySpoilageFraction(0.22, 10, safe);
        double hotWet = GrainSpoilageModel.dailySpoilageFraction(0.22, 30, safe);
        assertTrue(hotWet > coolWet);
        assertTrue(GrainSpoilageModel.expectedSpoiled(500, hotWet, 2.0) > 0);
    }

    @Test
    void storageStateUsesCompatibilityDefaults() {
        GrainStorageState state = GrainStorageState.fromSaved(null, null, null, null);
        assertEquals(0, state.grainCount());
        assertEquals(0, state.spoiledCount());
        assertEquals(GrainDryingModel.DEFAULT_HARVEST_MOISTURE, state.moisture(), 1.0e-9);
        assertEquals(0.0, state.spoilageCarry(), 1.0e-9);
    }

    @Test
    void siloAerationDriesToJustBelowSafeAndNoFurther() {
        double safe = 0.14;
        double after = GrainSpoilageModel.aerate(0.22, safe, 0.01, 3.0);
        assertEquals(0.19, after, 1.0e-9);
        assertEquals(safe - 0.01, GrainSpoilageModel.aerate(0.22, safe, 0.01, 50.0), 1.0e-9);
        assertEquals(0.10, GrainSpoilageModel.aerate(0.10, safe, 0.01, 5.0), 1.0e-9, "dry grain is not dried further");
    }

    @Test
    void siloKeepsGrainBetterThanOpenStorage() {
        double safe = 0.14;
        double wet = GrainSpoilageModel.dailySpoilageFraction(0.20, 22, safe);
        double days = 10;
        double pests = 0.004;
        double chest = GrainSpoilageModel.survival(wet, days) * GrainSpoilageModel.survival(pests, days);
        // In a silo: a quarter of the mould and no pests, while aeration also takes the grain below safe moisture.
        double silo = GrainSpoilageModel.survival(wet * 0.25, days);
        assertTrue(silo > chest, "silo " + silo + " vs chest " + chest);
        assertEquals(1.0, GrainSpoilageModel.survival(0.0, days), 1.0e-12);
        assertTrue(GrainSpoilageModel.survival(pests, 100) < 0.7, "open storage loses a third of its grain over 100 days");
    }
}
