package com.claudacious.tilth.sim.crop;

/** US customary units in which each crop's yield is traditionally reported per acre. */
public enum YieldUnit {
    BUSHEL("bu/ac"),
    HUNDREDWEIGHT("cwt/ac"),
    TON("tons/ac"),
    POUND("lb/ac"),
    BALE("bales/ac");

    public static final double SQ_M_PER_ACRE = 4046.8564224;
    public static final double LB_PER_KG = 2.20462262;

    private final String label;

    YieldUnit(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }

    /**
     * @param kgPerM2      harvested yield per square metre (one block)
     * @param unitWeightLb pounds per bushel (BUSHEL) or lint fraction (BALE); ignored otherwise
     */
    public double perAcre(double kgPerM2, double unitWeightLb) {
        double lbPerAcre = kgPerM2 * SQ_M_PER_ACRE * LB_PER_KG;
        return switch (this) {
            case BUSHEL -> lbPerAcre / Math.max(1, unitWeightLb);
            case HUNDREDWEIGHT -> lbPerAcre / 100.0;
            case TON -> lbPerAcre / 2000.0;
            case POUND -> lbPerAcre;
            case BALE -> lbPerAcre * unitWeightLb / 480.0;
        };
    }
}
