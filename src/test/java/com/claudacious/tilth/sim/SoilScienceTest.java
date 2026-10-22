package com.claudacious.tilth.sim;

import com.claudacious.tilth.sim.soil.Hydraulics;
import com.claudacious.tilth.sim.soil.Pedotransfer;
import com.claudacious.tilth.sim.soil.SoilTexture;
import com.claudacious.tilth.sim.soil.TextureTriangle;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SoilScienceTest {
    @Test
    void textureClassCentroidsClassifyToThemselves() {
        for (SoilTexture t : SoilTexture.MINERAL) {
            assertEquals(t, TextureTriangle.classify(t.sand(), t.clay()), "centroid of " + t);
        }
    }

    @Test
    void knownTexturePoints() {
        assertEquals(SoilTexture.LOAM, TextureTriangle.classify(40, 20));
        assertEquals(SoilTexture.SILT_LOAM, TextureTriangle.classify(20, 15));
        assertEquals(SoilTexture.SAND, TextureTriangle.classify(95, 2));
        assertEquals(SoilTexture.CLAY, TextureTriangle.classify(20, 55));
        assertEquals(SoilTexture.SILTY_CLAY, TextureTriangle.classify(5, 45));
        assertEquals(SoilTexture.SANDY_CLAY_LOAM, TextureTriangle.classify(60, 25));
    }

    @Test
    void saxtonRawlsLoamMatchesPublishedValues() {
        Hydraulics loam = Pedotransfer.saxtonRawls(0.40, 0.20, 2.5);
        assertEquals(0.28, loam.fieldCapacity(), 0.03);
        assertEquals(0.14, loam.wiltingPoint(), 0.03);
        assertEquals(0.46, loam.saturation(), 0.04);
        assertTrue(loam.ksat() > 5 && loam.ksat() < 30, "loam Ksat " + loam.ksat());
    }

    @Test
    void sandDrainsFasterAndHoldsLessWaterThanClay() {
        Hydraulics sand = SoilTexture.SAND.hydraulics(1.0);
        Hydraulics clay = SoilTexture.CLAY.hydraulics(2.5);
        assertTrue(sand.ksat() > clay.ksat() * 10, "sand " + sand.ksat() + " clay " + clay.ksat());
        assertTrue(clay.fieldCapacity() > sand.fieldCapacity());
        assertTrue(clay.wiltingPoint() > sand.wiltingPoint());
    }

    @Test
    void erosionCoarsensAndDepositionFines() {
        assertEquals(SoilTexture.SANDY_LOAM, SoilTexture.LOAM.coarser());
        assertTrue(SoilTexture.LOAM.finer().clay() >= SoilTexture.LOAM.clay());
        assertEquals(SoilTexture.PEAT, SoilTexture.PEAT.coarser());
    }
}
