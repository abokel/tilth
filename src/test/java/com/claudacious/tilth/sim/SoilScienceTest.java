package com.claudacious.tilth.sim;

import com.claudacious.tilth.sim.soil.Hydraulics;
import com.claudacious.tilth.sim.soil.Nutrient;
import com.claudacious.tilth.sim.soil.Pedotransfer;
import com.claudacious.tilth.sim.soil.SoilChemistry;
import com.claudacious.tilth.sim.soil.SoilState;
import com.claudacious.tilth.sim.soil.SoilTexture;
import com.claudacious.tilth.sim.soil.TextureTriangle;
import com.claudacious.tilth.sim.climate.BiomeClimate;
import com.claudacious.tilth.sim.climate.BiomeClimateTable;
import com.claudacious.tilth.sim.climate.DailyWeather;
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

    @Test
    void micronutrientAvailabilityRespondsToPh() {
        assertTrue(Nutrient.FE.availability(5.8) > Nutrient.FE.availability(8.2));
        assertTrue(Nutrient.ZN.availability(5.8) > Nutrient.ZN.availability(8.2));
        assertTrue(Nutrient.MN.availability(5.8) > Nutrient.MN.availability(8.2));
        assertTrue(Nutrient.CU.availability(5.8) > Nutrient.CU.availability(8.2));
        assertTrue(Nutrient.B.availability(5.8) > Nutrient.B.availability(8.2));
        assertTrue(Nutrient.MO.availability(8.2) > Nutrient.MO.availability(5.8));
    }

    @Test
    void chemistryRecoversMicronutrientsTowardEquilibrium() {
        BiomeClimate climate = BiomeClimateTable.lookup("minecraft:plains").orElseThrow();
        SoilState s = new SoilState();
        s.ph = 6.6;
        s.organicMatter = 3.0;
        s.moisture = 0.28;
        s.iron = 0.1;
        s.zinc = 0.01;
        s.manganese = 0.1;
        s.copper = 0.01;
        s.boron = 0.01;
        s.molybdenum = 0.001;
        double startZn = s.zinc;
        for (int i = 0; i < 120; i++) {
            SoilChemistry.step(s, SoilTexture.LOAM, SoilTexture.LOAM.hydraulics(s.organicMatter), new DailyWeather(0, 12, 22, 1.5, 0, 3, 65, 0),
                    0.2, false, climate);
        }
        assertTrue(s.zinc > startZn);
        assertTrue(s.iron > 0.1);
        assertTrue(s.molybdenum > 0.001);
    }
}
