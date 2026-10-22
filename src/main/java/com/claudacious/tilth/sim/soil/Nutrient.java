package com.claudacious.tilth.sim.soil;

/** Plant nutrients tracked as plant-available pools (ppm in the plough layer). */
public enum Nutrient {
    N("Nitrogen", "N", new double[]{4.5, 5.5, 6.0, 8.0, 9.0}, new double[]{0.4, 0.8, 1.0, 1.0, 0.7}),
    P("Phosphorus", "P", new double[]{4.5, 5.5, 6.2, 7.2, 8.0, 9.0}, new double[]{0.3, 0.6, 1.0, 1.0, 0.6, 0.4}),
    K("Potassium", "K", new double[]{4.5, 5.5, 6.0, 9.0}, new double[]{0.5, 0.8, 1.0, 1.0}),
    S("Sulfur", "S", new double[]{4.5, 5.5, 6.0, 9.0}, new double[]{0.6, 0.9, 1.0, 1.0}),
    CA("Calcium", "Ca", new double[]{4.5, 5.5, 6.5, 9.0}, new double[]{0.3, 0.6, 1.0, 1.0}),
    MG("Magnesium", "Mg", new double[]{4.5, 5.5, 6.5, 9.0}, new double[]{0.3, 0.6, 1.0, 1.0});

    private static final double[] MICRO_PH = {4.5, 6.5, 7.5, 8.5};
    private static final double[] MICRO_AVAIL = {1.0, 1.0, 0.75, 0.45};

    private final String displayName;
    private final String symbol;
    private final double[] phPoints;
    private final double[] availability;

    Nutrient(String displayName, String symbol, double[] phPoints, double[] availability) {
        this.displayName = displayName;
        this.symbol = symbol;
        this.phPoints = phPoints;
        this.availability = availability;
    }

    public String displayName() {
        return displayName;
    }

    public String symbol() {
        return symbol;
    }

    /** Fraction of the pool plants can take up at this pH (the classic Truog availability chart). */
    public double availability(double ph) {
        return curve(ph, phPoints, availability);
    }

    /** Availability of Fe, Mn, Zn, Cu and B, which drop sharply in alkaline soils. */
    public static double micronutrientAvailability(double ph) {
        return curve(ph, MICRO_PH, MICRO_AVAIL);
    }

    static double curve(double x, double[] xs, double[] ys) {
        if (x <= xs[0]) {
            return ys[0];
        }
        for (int i = 1; i < xs.length; i++) {
            if (x <= xs[i]) {
                double t = (x - xs[i - 1]) / (xs[i] - xs[i - 1]);
                return ys[i - 1] + (ys[i] - ys[i - 1]) * t;
            }
        }
        return ys[ys.length - 1];
    }
}
