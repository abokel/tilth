package com.claudacious.tilth.sim.genetics;

import com.claudacious.tilth.sim.SimSettings;

import java.util.Random;

/**
 * Inheritance for crops and livestock.
 *
 * <p>Crops are Mendelian ({@link CropGenotype}): every plant has two alleles at each gene and passes one of each, at
 * random, to each seed. What saved seed inherits depends on how the crop sets seed ({@link MatingSystem}): selfed in
 * wheat and beans (lines grow uniform and breed true), crossed with the neighbours in maize and rye (hybrid vigour
 * from wide crosses, depression from inbreeding), copied in potatoes and garlic. A controlled cross of two plants
 * (the gene splicer) gives an F1; each generation after it is F2, F3 and so on.
 *
 * <p>Livestock traits are polygenic and follow the infinitesimal model: young take the average of their parents
 * plus a random Mendelian deviation, so selection shifts a herd over generations.
 */
public final class GeneticsModel {
    private GeneticsModel() {
    }

    /** A landrace plant of a species: a mixed population with traits spread around the species average. */
    public static CropGenome randomLandrace(String speciesId, Random random) {
        return CropGenome.of(speciesId, "landrace", CropGenotype.landrace(random), 0);
    }

    /** A controlled cross of two plants (the gene splicer): an F1 named for both parents. */
    public static CropGenome cross(CropGenome mother, CropGenome father, Random random) {
        CropGenotype m = mother.genotype();
        CropGenotype f = father.genotype();
        double mu = SimSettings.cropMutationChance / CropGenotype.LOCI;
        CropGenotype child = CropGenotype.zygote(m.gamete(random, mu), f.gamete(random, mu));
        String name = mother.cultivar().equals(father.cultivar()) ? mother.cultivar() : shortName(mother.cultivar()) + " x " + shortName(father.cultivar());
        int generation = mother.cultivar().equals(father.cultivar()) ? Math.max(mother.generation(), father.generation()) + 1 : 1;
        return CropGenome.of(mother.speciesId(), name, child, generation);
    }

    /** Kept for callers of the old API: a cross of two plants. */
    public static CropGenome inheritCrop(CropGenome a, CropGenome b, Random random, String cultivarPrefix) {
        return cross(a, b, random);
    }

    /** One plant selfed: each seed gets two random gametes of the same parent, so heterozygosity halves each generation. */
    public static CropGenome self(CropGenome parent, Random random) {
        CropGenotype g = parent.genotype();
        double mu = SimSettings.cropMutationChance / CropGenotype.LOCI;
        return CropGenome.of(parent.speciesId(), parent.cultivar(), CropGenotype.zygote(g.gamete(random, mu), g.gamete(random, mu)),
                parent.generation() + 1);
    }

    /**
     * Seed saved from a plant at harvest. Self-pollinating crops self; cross-pollinating crops take pollen from a
     * neighbouring plant of the same crop ({@code pollen}, or themselves if none grows nearby: inbreeding); clonal
     * crops are copies.
     */
    public static CropGenome saveSeed(CropGenome mother, CropGenome pollen, Random random) {
        return switch (MatingSystem.of(mother.speciesId())) {
            case CLONAL -> mother;
            case SELF -> self(mother, random);
            case OUTCROSS -> pollen == null ? self(mother, random) : cross(mother, pollen, random);
        };
    }

    /** A rare change in the genes of a plant grown on in the incubator. */
    public static CropGenome mutateCrop(CropGenome g, Random random) {
        CropGenotype genes = g.genotype();
        double mu = SimSettings.cropMutationChance / CropGenotype.LOCI;
        // Each allele copy has a small chance to change.
        String a = genes.alleles();
        StringBuilder out = new StringBuilder(a.length());
        for (int i = 0; i < a.length(); i++) {
            char c = a.charAt(i);
            out.append(random.nextDouble() < mu ? (c == '1' ? '0' : '1') : c);
        }
        return CropGenome.of(g.speciesId(), g.cultivar(), new CropGenotype(out.toString()), g.generation());
    }

    private static String shortName(String cultivar) {
        String first = cultivar.split(" x ")[0];
        return first.length() > 18 ? first.substring(0, 18) : first;
    }

    public static LivestockGenome randomLivestock(Random random) {
        return new LivestockGenome(jitter(random, 0.16), jitter(random, 0.16), jitter(random, 0.16), jitter(random, 0.12), random.nextInt(12))
                .normalized();
    }

    public static LivestockGenome inheritLivestock(LivestockGenome a, LivestockGenome b, Random random) {
        LivestockGenome pa = a.normalized();
        LivestockGenome pb = b.normalized();
        LivestockGenome child = new LivestockGenome(
                midparent(pa.appetite(), pb.appetite(), random),
                midparent(pa.feedEfficiency(), pb.feedEfficiency(), random),
                midparent(pa.produce(), pb.produce(), random),
                midparent(pa.bodySize(), pb.bodySize(), random),
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

    /** Infinitesimal model: the parents' average plus a Mendelian sampling deviation (half the additive variance). */
    private static double midparent(double a, double b, Random random) {
        return (a + b) / 2.0 + random.nextGaussian() * 0.06;
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
