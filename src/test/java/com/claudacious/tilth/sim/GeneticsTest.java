package com.claudacious.tilth.sim;

import com.claudacious.tilth.livestock.LivestockState;
import com.claudacious.tilth.sim.crop.CropState;
import com.claudacious.tilth.sim.crop.CropGrowthModel;
import com.claudacious.tilth.sim.crop.Crops;
import com.claudacious.tilth.sim.genetics.CropGenome;
import com.claudacious.tilth.sim.genetics.GeneticsModel;
import com.claudacious.tilth.sim.genetics.LivestockGenome;
import com.claudacious.tilth.sim.livestock.LivestockBreedingModel;
import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class GeneticsTest {
    @Test
    void cropInheritanceStaysInBounds() {
        Random random = new Random(42);
        CropGenome a = new CropGenome("radish", "a", 0.3, -0.2, 0.1, 0.2, 1);
        CropGenome b = new CropGenome("radish", "b", -0.25, 0.25, -0.1, -0.2, 8);
        for (int i = 0; i < 200; i++) {
            CropGenome c = GeneticsModel.inheritCrop(a, b, random, "test");
            assertEquals("radish", c.speciesId());
            assertTrue(Math.abs(c.growth()) <= 0.45);
            assertTrue(Math.abs(c.yield()) <= 0.45);
            assertTrue(Math.abs(c.drought()) <= 0.45);
            assertTrue(Math.abs(c.frost()) <= 0.45);
        }
    }

    @Test
    void livestockMutationStaysInBounds() {
        Random random = new Random(7);
        LivestockGenome g = LivestockGenome.baseline();
        for (int i = 0; i < 300; i++) {
            g = GeneticsModel.mutateLivestock(g, random);
            assertTrue(Math.abs(g.appetite()) <= 0.45);
            assertTrue(Math.abs(g.feedEfficiency()) <= 0.45);
            assertTrue(Math.abs(g.produce()) <= 0.45);
            assertTrue(Math.abs(g.bodySize()) <= 0.45);
        }
    }

    @Test
    void oldCropSaveWithoutGenomeDefaultsSafely() {
        CropState state = new CropState();
        assertNull(state.genome);
        assertEquals("radish", state.genome(Crops.RADISH).speciesId());
    }

    @Test
    void yieldFactorUsesInheritedGenomeYieldTrait() {
        CropState plain = new CropState();
        plain.yieldPotential = 1.0;
        plain.afflictionYieldFactor = 1.0;
        plain.pollinationFactor = 1.0;
        CropState improved = new CropState();
        improved.yieldPotential = 1.0;
        improved.afflictionYieldFactor = 1.0;
        improved.pollinationFactor = 1.0;
        improved.genome = new CropGenome("radish", "selected", 0, 0.4, 0, 0, 1).normalized();
        assertTrue(CropGrowthModel.yieldFactor(improved) > CropGrowthModel.yieldFactor(plain));
    }

    @Test
    void legacyGenomePayloadsFallbackToDefaults() {
        CropGenome legacyCrop = CropGenome.decode("bad-data");
        LivestockGenome legacyAnimal = LivestockGenome.decode("1|2");
        assertEquals("unknown", legacyCrop.speciesId());
        assertEquals(1.0, legacyAnimal.feedMultiplier(), 1e-9);
    }

    @Test
    void strictCropGenomeDecodeRejectsMalformedData() {
        assertNull(CropGenome.tryDecode(null));
        assertNull(CropGenome.tryDecode(""));
        assertNull(CropGenome.tryDecode("radish|landrace|oops"));
        assertNull(CropGenome.tryDecode("|landrace|0|0|0|0|1"));
        assertNull(CropGenome.tryDecode("radish|landrace|NaN|0|0|0|1"));
        assertNull(CropGenome.tryDecode("radish|landrace|0|Infinity|0|0|1"));
        assertNull(CropGenome.tryDecode("radish|landrace|0|0|-Infinity|0|1"));
    }

    @Test
    void genomeNormalizationSanitizesNonFiniteTraits() {
        CropGenome crop = new CropGenome("radish", "landrace", Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY, 0.2, 1).normalized();
        assertEquals(0.0, crop.growth(), 1e-9);
        assertEquals(0.0, crop.yield(), 1e-9);
        assertEquals(0.0, crop.drought(), 1e-9);
        assertEquals(0.2, crop.frost(), 1e-9);

        LivestockGenome livestock = new LivestockGenome(Double.NaN, Double.POSITIVE_INFINITY, -0.2, Double.NEGATIVE_INFINITY, 1).normalized();
        assertEquals(0.0, livestock.appetite(), 1e-9);
        assertEquals(0.0, livestock.feedEfficiency(), 1e-9);
        assertEquals(-0.2, livestock.produce(), 1e-9);
        assertEquals(0.0, livestock.bodySize(), 1e-9);
    }

    @Test
    void livestockDecodeRejectsNonFiniteTraits() {
        LivestockGenome decoded = LivestockGenome.decode("NaN|0|0|0|1");
        assertEquals(1.0, decoded.feedMultiplier(), 1e-9);
        assertEquals(1.0, decoded.scaleMultiplier(), 1e-9);
    }

    @Test
    void livestockPhenotypeProducesVisibleScaleSpread() {
        LivestockGenome small = new LivestockGenome(0, 0, 0, 0, 0);
        LivestockGenome large = new LivestockGenome(0, 0, 0, 0, 11);
        assertTrue(small.phenotypeScaleMultiplier() < large.phenotypeScaleMultiplier());
        assertTrue(small.phenotypeScaleMultiplier() >= 0.92);
        assertTrue(large.phenotypeScaleMultiplier() <= 1.08);
    }

    @Test
    void offspringInheritanceHelperIsDeterministicAndSetsKnownSex() {
        LivestockGenome mother = new LivestockGenome(0.2, -0.1, 0.25, 0.1, 3);
        LivestockGenome father = new LivestockGenome(-0.2, 0.15, -0.1, -0.25, 8);

        Random expectedRandom = new Random(5);
        byte expectedSex = expectedRandom.nextBoolean() ? LivestockState.FEMALE : LivestockState.MALE;
        LivestockGenome expectedGenome = GeneticsModel.inheritLivestock(mother, father, expectedRandom);

        LivestockBreedingModel.OffspringInheritance inherited = LivestockBreedingModel.inheritOffspring(mother, father, new Random(5));
        assertEquals(expectedSex, inherited.sex());
        assertEquals(expectedGenome.encode(), inherited.genome().encode());
        assertTrue(inherited.sex() == LivestockState.FEMALE || inherited.sex() == LivestockState.MALE);
    }
}
