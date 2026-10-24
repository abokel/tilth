package com.claudacious.tilth.sim.soil;

import com.claudacious.tilth.sim.Noise;

/**
 * Fertilisers and soil amendments. One item treats one block (1 m2); amounts are what a typical
 * field rate supplies to the plough layer, e.g. one compost item ~ 20 t/ha.
 */
public enum Amendment {
    //                       N    P    K    S   Ca   Mg   pH    OM%  residue  residueN%
    COMPOST("Compost",       8,   4,  10,   1,  15,   3, 0.05, 0.25, 0.00, 0.0),
    MANURE("Manure",        15,   6,  15,   2,  10,   3, 0.00, 0.20, 0.00, 0.0),
    BONE_MEAL("Bone Meal",   2,  25,   0,   0,  50,   0, 0.05, 0.00, 0.00, 0.0),
    WOOD_ASH("Wood Ash",     0,   2,  30,   1,  60,   8, 0.35, 0.00, 0.00, 0.0),
    AGRICULTURAL_LIME("Agricultural Lime", 0, 0, 0, 0, 200, 15, 0.60, 0.00, 0.00, 0.0),
    SULFUR("Elemental Sulfur", 0, 0,   0,  40,   0,   0, -0.40, 0.00, 0.00, 0.0),
    STRAW("Straw",           0,   0,   3,   0,   0,   0, 0.00, 0.00, 0.40, 0.6),
    SLURRY("Slurry",          7,   3,   7,   1,   4,   1, 0.00, 0.08, 0.00, 0.0),
    GREEN_MANURE("Green Manure", 0, 0, 0,   0,   0,   0, 0.00, 0.00, 0.30, 3.0);

    private final String displayName;
    private final double n;
    private final double p;
    private final double k;
    private final double s;
    private final double ca;
    private final double mg;
    private final double phDelta;
    private final double organicMatter;
    private final double residue;
    private final double residueN;

    Amendment(String displayName, double n, double p, double k, double s, double ca, double mg, double phDelta,
              double organicMatter, double residue, double residueN) {
        this.displayName = displayName;
        this.n = n;
        this.p = p;
        this.k = k;
        this.s = s;
        this.ca = ca;
        this.mg = mg;
        this.phDelta = phDelta;
        this.organicMatter = organicMatter;
        this.residue = residue;
        this.residueN = residueN;
    }

    public double n() {
        return n;
    }

    public double p() {
        return p;
    }

    public double k() {
        return k;
    }

    public double phDelta() {
        return phDelta;
    }

    public String displayName() {
        return displayName;
    }

    public void applyTo(SoilState state, SoilTexture texture) {
        state.nitrogen += n;
        state.phosphorus += p;
        state.potassium += k;
        state.sulfur += s;
        state.calcium += ca;
        state.magnesium += mg;
        // High-CEC soils (clay, humus) resist pH change and need more lime per unit change.
        double buffer = Noise.clamp(10.0 / texture.cec(state.organicMatter), 0.35, 2.0);
        state.ph += phDelta * buffer;
        state.organicMatter += organicMatter;
        state.addResidue(residue, residueN);
        SoilChemistry.clamp(state);
    }

    public String summary() {
        StringBuilder sb = new StringBuilder();
        append(sb, "N", n);
        append(sb, "P", p);
        append(sb, "K", k);
        append(sb, "S", s);
        append(sb, "Ca", ca);
        append(sb, "Mg", mg);
        if (phDelta != 0) {
            sb.append(sb.isEmpty() ? "" : ", ").append(phDelta > 0 ? "raises" : "lowers").append(" pH");
        }
        if (organicMatter > 0) {
            sb.append(sb.isEmpty() ? "" : ", ").append("+organic matter");
        }
        if (residue > 0) {
            sb.append(sb.isEmpty() ? "" : ", ").append("adds residue");
        }
        return sb.toString();
    }

    private static void append(StringBuilder sb, String symbol, double amount) {
        if (amount > 0) {
            sb.append(sb.isEmpty() ? "" : ", ").append("+").append((int) amount).append(" ppm ").append(symbol);
        }
    }
}
