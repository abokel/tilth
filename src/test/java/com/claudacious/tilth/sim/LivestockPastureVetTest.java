package com.claudacious.tilth.sim;

import com.claudacious.tilth.sim.livestock.LivestockDiseaseModel;
import com.claudacious.tilth.sim.livestock.PastureModel;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LivestockPastureVetTest {
    @Test
    void pastureOvergrazesThenRecoversWithRotation() {
        double quality = 1.0;
        quality = PastureModel.graze(quality, 0.22, 3.0);
        assertTrue(quality < 0.4, "expected severe overgrazing");
        quality = PastureModel.recover(quality, 0.12, 4.0);
        assertTrue(quality > 0.7, "expected recovery after resting paddock");
        assertEquals(1.0, PastureModel.recover(quality, 1.0, 100), 1.0e-9, "must stay bounded");
    }

    @Test
    void diseaseRiskAndProgressionStayBounded() {
        double susceptibility = LivestockDiseaseModel.susceptibility(0.3, 0.2, false);
        double pressure = LivestockDiseaseModel.transmissionPressure(0.9, true, true, false);
        double chance = LivestockDiseaseModel.infectionChance(0.7, 0.8, susceptibility, pressure, 1.5);
        assertTrue(chance > 0.0 && chance <= 1.0);

        double severity = 0.5;
        for (int i = 0; i < 20; i++) {
            severity = LivestockDiseaseModel.nextSeverity(severity, 0.7, 0.9, 0.8, 0.0, 0.0);
            assertTrue(severity >= 0.0 && severity <= 1.0);
        }
        double treated = LivestockDiseaseModel.nextSeverity(severity, 0.7, 0.4, 0.2, 1.0, 0.5);
        assertTrue(treated < severity, "treatment should reduce severity");
    }

    @Test
    void quarantineLowersRiskAndImpactIsCapped() {
        double openRisk = LivestockDiseaseModel.susceptibility(0.4, 0.3, false);
        double quarantineRisk = LivestockDiseaseModel.susceptibility(0.4, 0.3, true);
        assertTrue(quarantineRisk < openRisk);
        assertEquals(0.4, LivestockDiseaseModel.impactFactor(1.0, 0.6), 1.0e-9);
        assertEquals(1.0, LivestockDiseaseModel.impactFactor(0.0, 0.6), 1.0e-9);
    }

    @Test
    void environmentalIntroductionIsLowBoundedAndQuarantineAware() {
        double stress = LivestockDiseaseModel.hostStress(0.25, 0.3);
        double none = LivestockDiseaseModel.environmentalIntroductionPressure(0.9, stress, false, false);
        double open = LivestockDiseaseModel.environmentalIntroductionPressure(0.9, stress, true, false);
        double quarantined = LivestockDiseaseModel.environmentalIntroductionPressure(0.9, stress, true, true);
        assertEquals(0.0, none, 1.0e-12);
        assertTrue(open > 0.0 && open <= 0.0006);
        assertTrue(quarantined > 0.0 && quarantined < open);

        double chance = LivestockDiseaseModel.infectionChance(0.8, 0.9, 0.85, open, 1.5);
        assertTrue(chance > 0.0 && chance < 0.001, "environment-only spontaneous infection must stay uncommon");
    }

    @Test
    void diagnosisWindowHandlesMissingStaleAndCurrentTimestamps() {
        long now = 120_000L;
        long window = 48_000L;
        assertFalse(LivestockDiseaseModel.isRecentDiagnosis(null, now, window));
        assertFalse(LivestockDiseaseModel.isRecentDiagnosis(now - window - 1, now, window));
        assertTrue(LivestockDiseaseModel.isRecentDiagnosis(now - window, now, window));
        assertTrue(LivestockDiseaseModel.isRecentDiagnosis(now - 10, now, window));
        assertFalse(LivestockDiseaseModel.isRecentDiagnosis(now + 1, now, window));
    }
}
