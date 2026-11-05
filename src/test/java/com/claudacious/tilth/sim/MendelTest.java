package com.claudacious.tilth.sim;

import com.claudacious.tilth.sim.genetics.CropGenome;
import com.claudacious.tilth.sim.genetics.CropGenotype;
import com.claudacious.tilth.sim.genetics.GeneticsModel;
import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class MendelTest {
    @org.junit.jupiter.api.BeforeEach
    void noMutation() {
        SimSettings.cropMutationChance = 0.0;
    }

    @org.junit.jupiter.api.AfterEach
    void restore() {
        SimSettings.cropMutationChance = 0.03;
    }

    private static CropGenotype uniform(char allele) {
        return new CropGenotype(String.valueOf(allele).repeat(2 * CropGenotype.LOCI));
    }

    @Test
    void f1OfTwoPureLinesIsUniformAndFullyHeterozygous() {
        CropGenome high = CropGenome.of("common_wheat", "High", uniform('1'), 8);
        CropGenome low = CropGenome.of("common_wheat", "Low", uniform('0'), 8);
        Random r = new Random(1);
        for (int i = 0; i < 20; i++) {
            CropGenome f1 = GeneticsModel.cross(high, low, r);
            assertEquals(1.0, f1.genotype().heterozygosity(), 1e-9);
            assertEquals(0.0, f1.genotype().trait(1), 1e-9, "additive genes: the F1 sits halfway between its parents");
            assertEquals(1, f1.generation());
            assertTrue(f1.resistant(), "resistance is dominant");
            assertFalse(f1.semiDwarf(), "semi-dwarfing is recessive");
        }
    }

    @Test
    void selfedF1SegregatesThreeToOne() {
        CropGenome f1 = GeneticsModel.cross(CropGenome.of("common_wheat", "A", uniform('1'), 8),
                CropGenome.of("common_wheat", "B", uniform('0'), 8), new Random(2));
        Random r = new Random(3);
        int susceptible = 0;
        int dwarf = 0;
        int n = 8000;
        for (int i = 0; i < n; i++) {
            CropGenome f2 = GeneticsModel.self(f1, r);
            if (!f2.resistant()) {
                susceptible++;
            }
            if (f2.semiDwarf()) {
                dwarf++;
            }
        }
        assertEquals(0.25, susceptible / (double) n, 0.02, "a quarter of the F2 lack the dominant resistance allele");
        assertEquals(0.25, dwarf / (double) n, 0.02, "a quarter of the F2 are recessive semi-dwarfs");
    }

    @Test
    void selfingHalvesHeterozygosityEachGeneration() {
        Random r = new Random(4);
        double total = 0;
        int runs = 400;
        for (int i = 0; i < runs; i++) {
            CropGenome g = CropGenome.of("barley", "X", CropGenotype.zygote("1".repeat(20), "0".repeat(20)), 1);
            for (int gen = 0; gen < 5; gen++) {
                g = GeneticsModel.self(g, r);
            }
            total += g.genotype().heterozygosity();
        }
        assertEquals(1.0 / 32, total / runs, 0.015);
    }

    @Test
    void hybridMaizeOutyieldsItsInbredParentsAndInbreedingSapsIt() {
        Random r = new Random(5);
        CropGenome a = CropGenome.of("maize", "A", CropGenotype.pureLine(r, 0.5, false, false), 8);
        CropGenome b = CropGenome.of("maize", "B", new CropGenotype(flip(a.genotype().alleles())), 8);
        CropGenome f1 = GeneticsModel.cross(a, b, r);
        assertTrue(f1.yield() > a.yield() && f1.yield() > b.yield(), "heterosis: " + f1.yield() + " vs " + a.yield() + ", " + b.yield());
        CropGenome inbred = f1;
        for (int i = 0; i < 6; i++) {
            inbred = GeneticsModel.saveSeed(inbred, null, r);
        }
        assertTrue(inbred.yield() < f1.yield(), "inbreeding depression");
        // The same cross in a self-pollinated crop gains nothing from heterozygosity.
        CropGenome wf1 = GeneticsModel.cross(CropGenome.of("common_wheat", "A", a.genotype(), 8), CropGenome.of("common_wheat", "B", b.genotype(), 8), r);
        assertEquals(wf1.genotype().trait(1), wf1.yield(), 1e-9);
    }

    @Test
    void clonesCopyTheirParentAndGenomesRoundTrip() {
        CropGenome potato = CropGenome.of("potato", "King Edward", CropGenotype.landrace(new Random(6)), 3);
        assertEquals(potato, GeneticsModel.saveSeed(potato, null, new Random(7)));
        assertEquals("clone", potato.breedingStatus());
        CropGenome back = CropGenome.decode(potato.encode());
        assertEquals(potato.genotype(), back.genotype());
        assertEquals(3, back.generation());
    }

    @Test
    void oldSavesKeepTheirTraits() {
        CropGenome legacy = new CropGenome("common_wheat", "old", 0.225, -0.1125, 0, 0.45, 5);
        CropGenotype g = legacy.genotype();
        assertEquals(0.225, g.trait(0), 1e-9);
        assertEquals(-0.1125, g.trait(1), 1e-9);
        assertEquals(0.45, g.trait(3), 1e-9);
    }

    private static String flip(String s) {
        StringBuilder b = new StringBuilder();
        for (char c : s.toCharArray()) {
            b.append(c == '1' ? '0' : '1');
        }
        return b.toString();
    }
}
