package com.claudacious.tilth.sim.soil;

import java.util.Arrays;
import java.util.List;

/**
 * The twelve USDA soil texture classes plus peat (an organic soil). Percentages are the
 * approximate centroid of each class on the texture triangle.
 */
public enum SoilTexture {
    SAND("sand", "Sand", 92, 3, 'A'),
    LOAMY_SAND("loamy_sand", "Loamy Sand", 82, 6, 'A'),
    SANDY_LOAM("sandy_loam", "Sandy Loam", 65, 10, 'B'),
    LOAM("loam", "Loam", 40, 20, 'B'),
    SILT_LOAM("silt_loam", "Silt Loam", 20, 15, 'B'),
    SILT("silt", "Silt", 7, 6, 'C'),
    SANDY_CLAY_LOAM("sandy_clay_loam", "Sandy Clay Loam", 60, 27, 'C'),
    CLAY_LOAM("clay_loam", "Clay Loam", 32, 34, 'C'),
    SILTY_CLAY_LOAM("silty_clay_loam", "Silty Clay Loam", 10, 34, 'C'),
    SANDY_CLAY("sandy_clay", "Sandy Clay", 52, 42, 'D'),
    SILTY_CLAY("silty_clay", "Silty Clay", 6, 47, 'D'),
    CLAY("clay", "Clay", 20, 60, 'D'),
    PEAT("peat", "Peat", 30, 20, 'D');

    public static final List<SoilTexture> MINERAL = Arrays.stream(values()).filter(t -> !t.isOrganic()).toList();

    private final String id;
    private final String displayName;
    private final int sand;
    private final int clay;
    private final char hydrologicGroup;

    SoilTexture(String id, String displayName, int sand, int clay, char hydrologicGroup) {
        this.id = id;
        this.displayName = displayName;
        this.sand = sand;
        this.clay = clay;
        this.hydrologicGroup = hydrologicGroup;
    }

    public String id() {
        return id;
    }

    public String displayName() {
        return displayName;
    }

    public int sand() {
        return sand;
    }

    public int clay() {
        return clay;
    }

    public int silt() {
        return 100 - sand - clay;
    }

    public boolean isOrganic() {
        return this == PEAT;
    }

    public char hydrologicGroup() {
        return hydrologicGroup;
    }

    public double defaultOrganicMatter() {
        return isOrganic() ? 45.0 : 2.5;
    }

    public Hydraulics hydraulics(double organicMatter) {
        if (isOrganic()) {
            return Hydraulics.PEAT;
        }
        return Pedotransfer.saxtonRawls(sand / 100.0, clay / 100.0, organicMatter);
    }

    /** Cation exchange capacity (cmol+/kg): clay and humus hold nutrient cations and buffer pH. */
    public double cec(double organicMatter) {
        return Math.min(150.0, 0.5 * clay + 0.05 * silt() + 2.0 * Math.min(organicMatter, 60.0));
    }

    /** SCS curve-number retention parameter (mm) by hydrologic soil group. */
    public double retentionMm() {
        return switch (hydrologicGroup) {
            case 'A' -> 250;
            case 'B' -> 120;
            case 'C' -> 75;
            default -> 45;
        };
    }

    /** Horizontal distance (blocks) that water seeps by capillarity from a ditch or pond. */
    public int irrigationReach() {
        if (isOrganic()) {
            return 6;
        }
        return Math.max(2, Math.min(6, 2 + (silt() + clay) / 30));
    }

    /** Relative ease of tillage and root penetration, 0 (heavy clay) to 1 (sand). */
    public double workability() {
        return isOrganic() ? 0.9 : Math.max(0.15, 1.0 - clay / 70.0);
    }

    /** Texture after fines are washed out by erosion. */
    public SoilTexture coarser() {
        return isOrganic() ? this : TextureTriangle.classify(sand + 14, clay - 6);
    }

    /** Texture after fines are deposited (colluvium or alluvium). */
    public SoilTexture finer() {
        return isOrganic() ? this : TextureTriangle.classify(sand - 12, clay + 8);
    }

    public static SoilTexture byId(String id) {
        for (SoilTexture t : values()) {
            if (t.id.equals(id)) {
                return t;
            }
        }
        return LOAM;
    }
}
