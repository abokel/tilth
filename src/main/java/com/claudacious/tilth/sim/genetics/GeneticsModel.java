package com.claudacious.tilth.sim.genetics;

import com.claudacious.tilth.sim.SimSettings;

import java.util.Random;

/** Inheritance and mutation model for crops and livestock. */
public final class GeneticsModel {
    private GeneticsModel() {
    }

    public static CropGenome randomLandrace(String speciesId, Random random) {
        return new CropGenome(speciesId, "landrace",
                jitter(random, 0.18), jitter(random, 0.18), jitter(random, 0.18), jitter(random, 0.18), random.nextInt(12)).normalized();
    }

    public static CropGenome inheritCrop(CropGenome a, CropGenome b, Random random, String cultivarPrefix) {
        CropGenome pa = a.normalized();
        CropGenome pb = b.normalized();
        String species = pa.speciesId();
        CropGenome child = new CropGenome(species,
                cultivarPrefix + "-" + Integer.toHexString(random.nextInt(0x10000)),
                recombine(pa.growth(), pb.growth(), random),
                recombine(pa.yield(), pb.yield(), random),
                recombine(pa.drought(), pb.drought(), random),
                recombine(pa.frost(), pb.frost(), random),
                random.nextBoolean() ? pa.phenotype() : pb.phenotype()).normalized();
        return mutateCrop(child, random);
    }

    public static CropGenome mutateCrop(CropGenome g, Random random) {
        CropGenome n = g.normalized();
        return new CropGenome(n.speciesId(), n.cultivar(),
                mutate(n.growth(), random, SimSettings.cropMutationChance, SimSettings.cropMutationStep),
                mutate(n.yield(), random, SimSettings.cropMutationChance, SimSettings.cropMutationStep),
                mutate(n.drought(), random, SimSettings.cropMutationChance, SimSettings.cropMutationStep),
                mutate(n.frost(), random, SimSettings.cropMutationChance, SimSettings.cropMutationStep),
                random.nextDouble() < SimSettings.cropMutationChance * 0.5 ? random.nextInt(12) : n.phenotype()).normalized();
    }

    public static LivestockGenome randomLivestock(Random random) {
        return new LivestockGenome(jitter(random, 0.16), jitter(random, 0.16), jitter(random, 0.16), jitter(random, 0.12), random.nextInt(12))
                .normalized();
    }

    public static LivestockGenome inheritLivestock(LivestockGenome a, LivestockGenome b, Random random) {
        LivestockGenome pa = a.normalized();
        LivestockGenome pb = b.normalized();
        LivestockGenome child = new LivestockGenome(
                recombine(pa.appetite(), pb.appetite(), random),
                recombine(pa.feedEfficiency(), pb.feedEfficiency(), random),
                recombine(pa.produce(), pb.produce(), random),
                recombine(pa.bodySize(), pb.bodySize(), random),
                random.nextBoolean() ? pa.phenotype() : pb.phenotype()).normalized();
        return mutateLivestock(child, random);
    }

    public static LivestockGenome mutateLivestock(LivestockGenome g, Random random) {
        LivestockGenome n = g.normalized();
        return new LivestockGenome(
                mutate(n.appetite(), random, SimSettings.livestockMutationChance, SimSettings.livestockMutationStep),
                mutate(n.feedEfficiency(), random, SimSettings.livestockMutationChance, SimSettings.livestockMutationStep),
                mutate(n.produce(), random, SimSettings.livestockMutationChance, SimSettings.livestockMutationStep),
                mutate(n.bodySize(), random, SimSettings.livestockMutationChance, SimSettings.livestockMutationStep),
                random.nextDouble() < SimSettings.livestockMutationChance * 0.4 ? random.nextInt(12) : n.phenotype()).normalized();
    }

    private static double recombine(double a, double b, Random random) {
        return a + (b - a) * random.nextDouble();
    }

    private static double mutate(double value, Random random, double chance, double step) {
        if (random.nextDouble() >= chance) {
            return value;
        }
        return value + (random.nextDouble() * 2.0 - 1.0) * step;
    }

    private static double jitter(Random random, double range) {
        return (random.nextDouble() * 2.0 - 1.0) * range;
    }
}
