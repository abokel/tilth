package com.claudacious.tilth.sim.genetics;

import java.util.Random;

/**
 * A crop's diploid genotype: two alleles at each of {@link #LOCI} genes, written as a string of '0'/'1' pairs.
 *
 * <ul>
 *   <li>Loci 0-15: four genes each for growth rate, yield, drought tolerance and frost tolerance. Allele '1' adds and
 *       '0' takes away a fixed amount (additive, no dominance), so a trait can range from -0.45 (all '0') to +0.45.</li>
 *   <li>Locus 16, {@code R}: disease resistance, dominant: one '1' allele is enough.</li>
 *   <li>Locus 17, {@code dw}: semi-dwarfing, recessive: only '11' plants are short-strawed (more grain in cereals).</li>
 *   <li>Loci 18-19: leaf colour, which tints the plant (the phenotype index).</li>
 * </ul>
 * Gametes carry one allele per locus, chosen independently (Mendel's second law); a zygote is two gametes.
 */
public record CropGenotype(String alleles) {
    public static final int TRAIT_LOCI = 4;
    public static final int R = 16;
    public static final int DW = 17;
    public static final int COLOUR = 18;
    public static final int LOCI = 20;
    /** Effect of one allele dose at a trait locus: four loci at full dose sum to +/-0.45. */
    private static final double EFFECT = 0.45 / TRAIT_LOCI;

    public CropGenotype {
        if (alleles == null || alleles.length() != 2 * LOCI || !alleles.matches("[01]+")) {
            throw new IllegalArgumentException("bad genotype: " + alleles);
        }
    }

    public static boolean valid(String alleles) {
        return alleles != null && alleles.length() == 2 * LOCI && alleles.matches("[01]+");
    }

    /** Copies of allele '1' at a locus: 0, 1 or 2. */
    public int dose(int locus) {
        return (alleles.charAt(2 * locus) - '0') + (alleles.charAt(2 * locus + 1) - '0');
    }

    /** Value of trait 0 growth, 1 yield, 2 drought, 3 frost: -0.45 .. +0.45. */
    public double trait(int trait) {
        double v = 0;
        for (int i = 0; i < TRAIT_LOCI; i++) {
            v += (dose(trait * TRAIT_LOCI + i) - 1) * EFFECT;
        }
        return v;
    }

    /** Share of the trait loci that carry two different alleles: 0 for a pure line, 1 for a wide cross. */
    public double heterozygosity() {
        int het = 0;
        for (int l = 0; l < 4 * TRAIT_LOCI; l++) {
            if (dose(l) == 1) {
                het++;
            }
        }
        return het / (double) (4 * TRAIT_LOCI);
    }

    public boolean resistant() {
        return dose(R) >= 1;
    }

    public boolean semiDwarf() {
        return dose(DW) == 2;
    }

    /** Whether every locus is homozygous: the line breeds true when selfed. */
    public boolean trueBreeding() {
        for (int l = 0; l < LOCI; l++) {
            if (dose(l) == 1) {
                return false;
            }
        }
        return true;
    }

    /** Leaf-colour phenotype index (0..10). */
    public int colour() {
        return dose(COLOUR) * 4 + dose(COLOUR + 1);
    }

    /** A gamete: one allele from each locus, picked independently, each copy mutating with probability {@code mutation}. */
    public String gamete(Random random, double mutation) {
        StringBuilder g = new StringBuilder(LOCI);
        for (int l = 0; l < LOCI; l++) {
            char a = alleles.charAt(2 * l + (random.nextBoolean() ? 1 : 0));
            if (random.nextDouble() < mutation) {
                a = a == '1' ? '0' : '1';
            }
            g.append(a);
        }
        return g.toString();
    }

    public static CropGenotype zygote(String egg, String pollen) {
        StringBuilder z = new StringBuilder(2 * LOCI);
        for (int l = 0; l < LOCI; l++) {
            z.append(egg.charAt(l)).append(pollen.charAt(l));
        }
        return new CropGenotype(z.toString());
    }

    /** A landrace plant: a mixed, half-heterozygous population; resistance and dwarfing are rare. */
    public static CropGenotype landrace(Random random) {
        StringBuilder s = new StringBuilder(2 * LOCI);
        for (int l = 0; l < LOCI; l++) {
            double p = l == R ? 0.12 : l == DW ? 0.15 : 0.45;
            s.append(random.nextDouble() < p ? '1' : '0').append(random.nextDouble() < p ? '1' : '0');
        }
        return new CropGenotype(s.toString());
    }

    /** A bred pure line: homozygous everywhere, carrying the good allele at each trait locus with probability {@code good}. */
    public static CropGenotype pureLine(Random random, double good, boolean resistant, boolean dwarf) {
        StringBuilder s = new StringBuilder(2 * LOCI);
        for (int l = 0; l < LOCI; l++) {
            char a = l == R ? (resistant ? '1' : '0') : l == DW ? (dwarf ? '1' : '0') : random.nextDouble() < (l < 16 ? good : 0.5) ? '1' : '0';
            s.append(a).append(a);
        }
        return new CropGenotype(s.toString());
    }

    /**
     * A genotype giving these trait values, for seed saved before genotypes existed: each trait's loci are filled to
     * the right dose, half of them heterozygous as in a landrace.
     */
    public static CropGenotype fromTraits(double growth, double yield, double drought, double frost, int phenotype) {
        StringBuilder s = new StringBuilder(2 * LOCI);
        for (double v : new double[]{growth, yield, drought, frost}) {
            int target = (int) Math.round(v / EFFECT);
            int[] dose = {2, 0, 1, 1};
            int sum = 0;
            while (sum < target) {
                int i = smallest(dose);
                dose[i]++;
                sum++;
            }
            while (sum > target) {
                int i = largest(dose);
                dose[i]--;
                sum--;
            }
            for (int d : dose) {
                s.append(d == 2 ? "11" : d == 0 ? "00" : "01");
            }
        }
        s.append("00").append("00");
        int c = Math.floorMod(phenotype, 12);
        s.append(pair(Math.min(2, c / 4))).append(pair(Math.min(2, c % 4)));
        return new CropGenotype(s.toString());
    }

    private static String pair(int dose) {
        return dose == 2 ? "11" : dose == 1 ? "01" : "00";
    }

    private static int smallest(int[] d) {
        int best = 0;
        for (int i = 1; i < d.length; i++) {
            if (d[i] < d[best]) {
                best = i;
            }
        }
        return best;
    }

    private static int largest(int[] d) {
        int best = 0;
        for (int i = 1; i < d.length; i++) {
            if (d[i] > d[best]) {
                best = i;
            }
        }
        return best;
    }
}
