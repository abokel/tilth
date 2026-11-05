package com.claudacious.tilth.sim.livestock;

import com.claudacious.tilth.livestock.LivestockState;
import com.claudacious.tilth.sim.genetics.GeneticsModel;
import com.claudacious.tilth.sim.genetics.LivestockGenome;

import java.util.Random;

/** Pure offspring inheritance helpers used by immediate and gestational livestock births. */
public final class LivestockBreedingModel {
    private LivestockBreedingModel() {
    }

    public static OffspringInheritance inheritOffspring(LivestockGenome motherGenome, LivestockGenome fatherGenome, Random random) {
        byte sex = random.nextBoolean() ? LivestockState.FEMALE : LivestockState.MALE;
        LivestockGenome genome = GeneticsModel.inheritLivestock(motherGenome, fatherGenome, random);
        return new OffspringInheritance(sex, genome);
    }

    public record OffspringInheritance(byte sex, LivestockGenome genome) {
    }
}
