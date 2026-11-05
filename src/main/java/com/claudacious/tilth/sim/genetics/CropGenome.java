package com.claudacious.tilth.sim.genetics;

import org.jetbrains.annotations.Nullable;

/**
 * A crop plant's heredity: its cultivar, its diploid genotype ({@link CropGenotype}) and how many generations it is
 * from the cross that made it (1 = F1). The trait values used by the growth model (growth, yield, drought, frost,
 * colour) are worked out from the genotype by {@link #of}: additive genes, plus hybrid vigour (or inbreeding
 * depression) in cross-pollinated crops and the semi-dwarfing gene's extra grain in small grains.
 *
 * <p>Genomes saved before genotypes existed have an empty {@code alleles}; a genotype matching their traits is
 * reconstructed on demand.
 */
public record CropGenome(String speciesId, String cultivar, double growth, double yield, double drought, double frost, int phenotype,
                         String alleles, int generation) {

    public CropGenome(String speciesId, String cultivar, double growth, double yield, double drought, double frost, int phenotype) {
        this(speciesId, cultivar, growth, yield, drought, frost, phenotype, "", 0);
    }

    public static CropGenome landrace(String speciesId) {
        return new CropGenome(speciesId, "landrace", 0, 0, 0, 0, 0);
    }

    /** The genome of a plant with this genotype, its trait values worked out from its genes. */
    public static CropGenome of(String speciesId, String cultivar, CropGenotype g, int generation) {
        double yield = g.trait(1);
        if (MatingSystem.of(speciesId) == MatingSystem.OUTCROSS) {
            // Hybrid vigour: a wide cross outyields its parents; a line inbred to uniformity loses vigour.
            yield += 0.3 * (g.heterozygosity() - 0.35);
        }
        if (g.semiDwarf() && MatingSystem.dwarfingHelps(speciesId)) {
            // Short straw: more of the plant goes into grain, and it stands up to wind and rain.
            yield += 0.08;
        }
        return new CropGenome(speciesId, cultivar, g.trait(0), yield, g.trait(2), g.trait(3), g.colour(), g.alleles(), generation).normalized();
    }

    public CropGenotype genotype() {
        if (CropGenotype.valid(alleles)) {
            return new CropGenotype(alleles);
        }
        return CropGenotype.fromTraits(growth, yield, drought, frost, phenotype);
    }

    public boolean resistant() {
        return CropGenotype.valid(alleles) && genotype().resistant();
    }

    public boolean semiDwarf() {
        return CropGenotype.valid(alleles) && genotype().semiDwarf();
    }

    /** "F1 hybrid", "F3, segregating", "true-breeding", "clone", or "landrace". */
    public String breedingStatus() {
        if (MatingSystem.of(speciesId) == MatingSystem.CLONAL) {
            return "clone";
        }
        if (!CropGenotype.valid(alleles)) {
            return "landrace".equals(cultivar) ? "landrace" : "saved seed";
        }
        CropGenotype g = genotype();
        if (g.trueBreeding()) {
            return "true-breeding";
        }
        if (generation == 1) {
            return "F1 hybrid";
        }
        if (generation <= 0) {
            return "landrace".equals(cultivar) ? "landrace" : "mixed";
        }
        return "F" + generation + ", segregating";
    }

    public CropGenome normalized() {
        return new CropGenome(speciesId, cultivar == null || cultivar.isBlank() ? "landrace" : cultivar,
                clamp(growth), clamp(yield), clamp(drought), clamp(frost), Math.floorMod(phenotype, 12),
                CropGenotype.valid(alleles) ? alleles : "", Math.max(0, generation));
    }

    public double growthMultiplier() {
        return 1.0 + growth * 0.45;
    }

    public double yieldMultiplier() {
        return 1.0 + yield * 0.55;
    }

    public double droughtBonusDays() {
        return drought * 6.0;
    }

    public double frostToleranceC() {
        return frost * 2.5;
    }

    public String encode() {
        CropGenome g = normalized();
        return g.speciesId + "|" + g.cultivar + "|" + g.growth + "|" + g.yield + "|" + g.drought + "|" + g.frost + "|" + g.phenotype
                + "|" + g.alleles + "|" + g.generation;
    }

    @Nullable
    public static CropGenome tryDecode(String encoded) {
        if (encoded == null || encoded.isBlank()) {
            return null;
        }
        String[] p = encoded.split("\\|", -1);
        if ((p.length != 7 && p.length != 9) || p[0].isBlank()) {
            return null;
        }
        try {
            double growth = Double.parseDouble(p[2]);
            double yield = Double.parseDouble(p[3]);
            double drought = Double.parseDouble(p[4]);
            double frost = Double.parseDouble(p[5]);
            if (!finite(growth) || !finite(yield) || !finite(drought) || !finite(frost)) {
                return null;
            }
            String alleles = p.length == 9 ? p[7] : "";
            int generation = p.length == 9 ? Integer.parseInt(p[8]) : 0;
            if (!alleles.isEmpty() && !CropGenotype.valid(alleles)) {
                return null;
            }
            return new CropGenome(p[0], p[1], growth, yield, drought, frost, Integer.parseInt(p[6]), alleles, generation).normalized();
        } catch (RuntimeException e) {
            return null;
        }
    }

    public static CropGenome decode(String encoded) {
        CropGenome decoded = tryDecode(encoded);
        return decoded == null ? landrace("unknown") : decoded;
    }

    private static double clamp(double v) {
        if (!finite(v)) {
            return 0.0;
        }
        return Math.max(-0.45, Math.min(0.45, v));
    }

    private static boolean finite(double v) {
        return Double.isFinite(v);
    }
}
