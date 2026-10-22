package com.claudacious.tilth.sim.crop;

/**
 * Outcome of harvesting one block.
 *
 * @param produce        number of produce items
 * @param seeds          number of separate seed items (0 when the produce is itself the seed)
 * @param grade          quality grade 1 (poor) to 5 (premium)
 * @param kgPerM2        yield of the harvested product per square metre
 * @param perAcre        the same yield in the crop's customary unit per acre
 * @param residueKgM2    crop residue left on the field
 * @param residueNPercent nitrogen concentration of that residue
 */
public record HarvestResult(int produce, int seeds, int grade, double kgPerM2, double perAcre, double residueKgM2,
                            double residueNPercent) {
}
