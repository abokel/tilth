package com.claudacious.tilth.sim;

import com.claudacious.tilth.sim.calendar.CalendarMath;
import com.claudacious.tilth.sim.catena.SlopePosition;
import com.claudacious.tilth.sim.climate.BiomeClimate;
import com.claudacious.tilth.sim.climate.BiomeClimateTable;
import com.claudacious.tilth.sim.climate.DailyWeather;
import com.claudacious.tilth.sim.climate.WeatherGenerator;
import com.claudacious.tilth.sim.crop.CropGrowthModel;
import com.claudacious.tilth.sim.crop.CropSpecies;
import com.claudacious.tilth.sim.crop.CropState;
import com.claudacious.tilth.sim.crop.Crops;
import com.claudacious.tilth.sim.crop.HarvestResult;
import com.claudacious.tilth.sim.soil.DrainageClass;
import com.claudacious.tilth.sim.soil.SoilBaseline;
import com.claudacious.tilth.sim.soil.SoilHorizon;
import com.claudacious.tilth.sim.soil.SoilState;
import com.claudacious.tilth.sim.soil.SoilTexture;
import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

/** Whole-season simulations: the model should reproduce realistic timing and relative yields. */
class CropSeasonTest {
    private static final BiomeClimate PLAINS = BiomeClimateTable.lookup("minecraft:plains").orElseThrow();
    private static final long SEED = 12345L;

    record Outcome(CropState crop, SoilState soil, int daysToMaturity, HarvestResult harvest) {
    }

    private static Outcome grow(CropSpecies sp, BiomeClimate climate, SoilState soil, SoilTexture tex, int sowMonth, int sowDay,
                                double irrigation) {
        CropState cs = new CropState();
        long day = CalendarMath.firstDayOfMonth(sowMonth) + sowDay - 1 + 365L * 10;
        int matured = -1;
        for (int i = 0; i < 300 && !cs.dead; i++) {
            DailyWeather w = WeatherGenerator.generate(SEED, climate, day + i, 100, 100, 0, false);
            FieldSimulator.step(soil, tex, climate, w, new FieldSimulator.Inputs(irrigation, false, 1, 1), sp, cs, null);
            if (matured < 0 && cs.dev >= 1.0) {
                matured = i;
                break;
            }
        }
        HarvestResult h = cs.dead ? null : CropGrowthModel.harvest(sp, cs, new Random(1));
        return new Outcome(cs, soil, matured, h);
    }

    private static SoilState loam(BiomeClimate c) {
        return SoilBaseline.create(SoilTexture.LOAM, SoilHorizon.TOPSOIL, SlopePosition.LEVEL, DrainageClass.WELL, c, 0);
    }

    @Test
    void springWheatMaturesInSummer() {
        Outcome o = grow(Crops.COMMON_WHEAT, PLAINS, loam(PLAINS), SoilTexture.LOAM, 4, 1, 0.3);
        assertFalse(o.crop().dead, o.crop().deathCause);
        assertTrue(o.daysToMaturity() > 80 && o.daysToMaturity() < 170, "days to maturity " + o.daysToMaturity());
        assertTrue(o.harvest().produce() >= 1, "produce " + o.harvest());
        System.out.printf("Wheat: %d days, %.1f %s, grade %d, limiting=%s%n", o.daysToMaturity(), o.harvest().perAcre(),
                Crops.COMMON_WHEAT.yieldUnit.label(), o.harvest().grade(), o.crop().limitingFactor);
    }

    @Test
    void maizeSownInMayMaturesBeforeWinter() {
        Outcome o = grow(Crops.MAIZE, PLAINS, loam(PLAINS), SoilTexture.LOAM, 5, 10, 0.3);
        assertFalse(o.crop().dead, o.crop().deathCause);
        assertTrue(o.daysToMaturity() > 90 && o.daysToMaturity() < 175, "days to maturity " + o.daysToMaturity());
        System.out.printf("Maize: %d days, %.0f %s, grade %d%n", o.daysToMaturity(), o.harvest().perAcre(),
                Crops.MAIZE.yieldUnit.label(), o.harvest().grade());
    }

    @Test
    void hardFrostKillsTomatoButNotRye() {
        CropState tomato = new CropState();
        tomato.germinated = true;
        tomato.dev = 0.3;
        CropState rye = new CropState();
        rye.germinated = true;
        rye.dev = 0.3;
        SoilState soil = loam(PLAINS);
        DailyWeather frost = new DailyWeather(100, -4.0, 6.0, 0, 0, 1.0, 70, 0);
        FieldSimulator.step(soil, SoilTexture.LOAM, PLAINS, frost, FieldSimulator.Inputs.NONE, Crops.TOMATO, tomato, null);
        FieldSimulator.step(soil, SoilTexture.LOAM, PLAINS, frost, FieldSimulator.Inputs.NONE, Crops.RYE, rye, null);
        assertTrue(tomato.dead, "tomato survived -4 C");
        assertFalse(rye.dead, "rye died at -4 C");
    }

    @Test
    void warmSeasonSeedSownInWinterWaitsForSpringOrRots() {
        Outcome o = grow(Crops.MAIZE, PLAINS, loam(PLAINS), SoilTexture.LOAM, 1, 15, 0.3);
        assertTrue(o.crop().dead || o.crop().daysSinceSowing > 60,
                "maize sown in January must not emerge in winter");
    }

    @Test
    void manureRaisesMaizeYield() {
        SoilState bare = loam(PLAINS);
        SoilState manured = loam(PLAINS);
        for (int i = 0; i < 3; i++) {
            com.claudacious.tilth.sim.soil.Amendment.MANURE.applyTo(manured, SoilTexture.LOAM);
        }
        Outcome b = grow(Crops.MAIZE, PLAINS, bare, SoilTexture.LOAM, 5, 10, 0.3);
        Outcome m = grow(Crops.MAIZE, PLAINS, manured, SoilTexture.LOAM, 5, 10, 0.3);
        assertTrue(CropGrowthModel.yieldFactor(m.crop()) > CropGrowthModel.yieldFactor(b.crop()),
                "manured " + CropGrowthModel.yieldFactor(m.crop()) + " bare " + CropGrowthModel.yieldFactor(b.crop()));
        System.out.printf("Maize unfertilised %.0f bu/ac, manured %.0f bu/ac%n", b.harvest().perAcre(), m.harvest().perAcre());
    }

    @Test
    void acidSoilCutsYield() {
        SoilState good = loam(PLAINS);
        SoilState acid = loam(PLAINS);
        acid.ph = 4.6;
        Outcome g = grow(Crops.BARLEY, PLAINS, good, SoilTexture.LOAM, 4, 1, 0.3);
        Outcome a = grow(Crops.BARLEY, PLAINS, acid, SoilTexture.LOAM, 4, 1, 0.3);
        assertTrue(CropGrowthModel.yieldFactor(a.crop()) < CropGrowthModel.yieldFactor(g.crop()) * 0.75,
                "acid " + CropGrowthModel.yieldFactor(a.crop()) + " vs good " + CropGrowthModel.yieldFactor(g.crop()));
    }

    @Test
    void nitrogenStarvedSoilCutsYieldUnlessLegume() {
        SoilState poor = loam(PLAINS);
        poor.nitrogen = 1;
        poor.organicMatter = 0.4;
        SoilState rich = loam(PLAINS);
        rich.nitrogen = 60;
        Outcome p = grow(Crops.MAIZE, PLAINS, poor, SoilTexture.LOAM, 5, 10, 0.3);
        Outcome r = grow(Crops.MAIZE, PLAINS, rich, SoilTexture.LOAM, 5, 10, 0.3);
        assertTrue(CropGrowthModel.yieldFactor(p.crop()) < CropGrowthModel.yieldFactor(r.crop()) * 0.8,
                "poor " + CropGrowthModel.yieldFactor(p.crop()) + " rich " + CropGrowthModel.yieldFactor(r.crop()));

        SoilState poorForBeans = loam(PLAINS);
        poorForBeans.nitrogen = 1;
        poorForBeans.organicMatter = 0.4;
        Outcome soy = grow(Crops.SOYBEAN, PLAINS, poorForBeans, SoilTexture.LOAM, 5, 15, 0.3);
        assertTrue(soy.crop().fixedNitrogen > 50, "soy fixed " + soy.crop().fixedNitrogen + " kg N/ha");
    }

    @Test
    void desertCropWithoutIrrigationDies() {
        BiomeClimate desert = BiomeClimateTable.lookup("minecraft:desert").orElseThrow();
        SoilState sand = SoilBaseline.create(SoilTexture.SAND, SoilHorizon.TOPSOIL, SlopePosition.LEVEL,
                DrainageClass.EXCESSIVE, desert, 0);
        Outcome o = grow(Crops.LETTUCE, desert, sand, SoilTexture.SAND, 3, 1, 0.0);
        assertTrue(o.crop().dead || CropGrowthModel.yieldFactor(o.crop()) < 0.3,
                "unirrigated lettuce in desert sand should fail: " + o.crop().deathCause + " yf="
                        + CropGrowthModel.yieldFactor(o.crop()));
    }
}
